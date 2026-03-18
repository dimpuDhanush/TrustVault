import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BankingService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public String addCustomer(String fullName, String email, String phone, String address, String adminUsername) {
        String sql = "INSERT INTO customers(full_name, email, phone, address) VALUES (?, ?, ?, ?)";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, fullName);
                statement.setString(2, email);
                statement.setString(3, phone.isBlank() ? null : phone);
                statement.setString(4, address.isBlank() ? null : address);
                statement.executeUpdate();

                int customerId = 0;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        customerId = keys.getInt(1);
                    }
                }

                logAction(connection, adminUsername, "CREATE", "CUSTOMER", String.valueOf(customerId), fullName + " | " + email);
                connection.commit();
                return "Customer created successfully.";
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to create customer. Email may already exist.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to create customer.";
        }
    }

    public String createAccount(int customerId, String accountType, BigDecimal initialDeposit, String adminUsername) {
        String insertAccountSql = "INSERT INTO accounts(customer_id, account_number, account_type, balance, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        String insertTransactionSql = "INSERT INTO transactions(account_id, transaction_type, amount, balance_after, note) VALUES (?, 'DEPOSIT', ?, ?, ?)";
        String accountNumber = generateAccountNumber();

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement accountStatement = connection.prepareStatement(insertAccountSql, Statement.RETURN_GENERATED_KEYS)) {
                accountStatement.setInt(1, customerId);
                accountStatement.setString(2, accountNumber);
                accountStatement.setString(3, accountType);
                accountStatement.setBigDecimal(4, initialDeposit);
                accountStatement.executeUpdate();

                int accountId = 0;
                try (ResultSet keys = accountStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        accountId = keys.getInt(1);
                    }
                }

                if (initialDeposit.compareTo(ZERO) > 0) {
                    try (PreparedStatement transactionStatement = connection.prepareStatement(insertTransactionSql)) {
                        transactionStatement.setInt(1, accountId);
                        transactionStatement.setBigDecimal(2, initialDeposit);
                        transactionStatement.setBigDecimal(3, initialDeposit);
                        transactionStatement.setString(4, "Initial deposit");
                        transactionStatement.executeUpdate();
                    }
                }

                logAction(connection, adminUsername, "CREATE", "ACCOUNT", accountNumber, accountType + " | " + initialDeposit);
                connection.commit();
                return "Account created successfully. Account Number: " + accountNumber;
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to create account.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to create account.";
        }
    }

    public String deposit(String accountNumber, BigDecimal amount, String note, String adminUsername) {
        return applyCashTransaction(accountNumber, amount, note, true, adminUsername);
    }

    public String withdraw(String accountNumber, BigDecimal amount, String note, String adminUsername) {
        return applyCashTransaction(accountNumber, amount, note, false, adminUsername);
    }

    private String applyCashTransaction(String accountNumber, BigDecimal amount, String note, boolean deposit, String adminUsername) {
        String selectSql = "SELECT account_id, balance FROM accounts WHERE account_number = ? AND status = 'ACTIVE' FOR UPDATE";
        String updateSql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        String insertSql = "INSERT INTO transactions(account_id, transaction_type, amount, balance_after, note) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setString(1, accountNumber);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return "Account not found or inactive.";
                    }

                    int accountId = resultSet.getInt("account_id");
                    BigDecimal currentBalance = resultSet.getBigDecimal("balance");
                    BigDecimal newBalance = deposit ? currentBalance.add(amount) : currentBalance.subtract(amount);

                    if (!deposit && newBalance.compareTo(ZERO) < 0) {
                        connection.rollback();
                        return "Insufficient funds for withdrawal.";
                    }

                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                         PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                        updateStatement.setBigDecimal(1, newBalance);
                        updateStatement.setInt(2, accountId);
                        updateStatement.executeUpdate();

                        insertStatement.setInt(1, accountId);
                        insertStatement.setString(2, deposit ? "DEPOSIT" : "WITHDRAW");
                        insertStatement.setBigDecimal(3, amount);
                        insertStatement.setBigDecimal(4, newBalance);
                        insertStatement.setString(5, note == null || note.isBlank() ? null : note);
                        insertStatement.executeUpdate();
                    }

                    logAction(connection, adminUsername, deposit ? "DEPOSIT" : "WITHDRAW", "ACCOUNT", accountNumber,
                            (deposit ? "Deposit" : "Withdrawal") + " | " + amount);
                    connection.commit();
                    return "Transaction successful. New balance: " + newBalance;
                }
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to process transaction.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to process transaction.";
        }
    }

    public String updateAccountStatus(String accountNumber, String newStatus, String adminUsername) {
        String selectSql = "SELECT account_id, balance, status FROM accounts WHERE account_number = ? FOR UPDATE";
        String updateSql = "UPDATE accounts SET status = ? WHERE account_id = ?";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setString(1, accountNumber);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return "Account not found.";
                    }

                    int accountId = resultSet.getInt("account_id");
                    BigDecimal balance = resultSet.getBigDecimal("balance");
                    String currentStatus = resultSet.getString("status");

                    if (currentStatus.equalsIgnoreCase(newStatus)) {
                        connection.rollback();
                        return "Account is already " + newStatus + ".";
                    }

                    if ("CLOSED".equalsIgnoreCase(newStatus) && balance.compareTo(ZERO) != 0) {
                        connection.rollback();
                        return "Close the account only after balance reaches 0.00.";
                    }

                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
                        updateStatement.setString(1, newStatus);
                        updateStatement.setInt(2, accountId);
                        updateStatement.executeUpdate();
                    }

                    logAction(connection, adminUsername, "STATUS_CHANGE", "ACCOUNT", accountNumber, currentStatus + " -> " + newStatus);
                    connection.commit();
                    return "Account status updated to " + newStatus + ".";
                }
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to update account status.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to update account status.";
        }
    }

    public String addBeneficiary(int customerId, String nickname, String beneficiaryName, String beneficiaryAccountNumber,
                                 String bankName, String ifscCode, String adminUsername) {
        String sql = """
                INSERT INTO beneficiaries(customer_id, nickname, beneficiary_name, beneficiary_account_number, bank_name, ifsc_code)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, customerId);
                statement.setString(2, nickname);
                statement.setString(3, beneficiaryName);
                statement.setString(4, beneficiaryAccountNumber);
                statement.setString(5, bankName == null || bankName.isBlank() ? "TrustVault" : bankName);
                statement.setString(6, ifscCode == null || ifscCode.isBlank() ? null : ifscCode);
                statement.executeUpdate();

                int beneficiaryId = 0;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        beneficiaryId = keys.getInt(1);
                    }
                }

                logAction(connection, adminUsername, "CREATE", "BENEFICIARY", String.valueOf(beneficiaryId),
                        nickname + " | " + beneficiaryAccountNumber);
                connection.commit();
                return "Beneficiary added successfully.";
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to add beneficiary.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to add beneficiary.";
        }
    }

    public List<BeneficiaryData> loadBeneficiaries() {
        List<BeneficiaryData> beneficiaries = new ArrayList<>();
        String sql = """
                SELECT b.beneficiary_id, c.full_name AS owner_name, b.nickname, b.beneficiary_name,
                       b.beneficiary_account_number, b.bank_name, b.ifsc_code, b.created_at
                FROM beneficiaries b
                JOIN customers c ON c.customer_id = b.customer_id
                ORDER BY b.beneficiary_id DESC
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return beneficiaries;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    beneficiaries.add(new BeneficiaryData(
                            resultSet.getInt("beneficiary_id"),
                            resultSet.getString("owner_name"),
                            resultSet.getString("nickname"),
                            resultSet.getString("beneficiary_name"),
                            resultSet.getString("beneficiary_account_number"),
                            resultSet.getString("bank_name"),
                            resultSet.getString("ifsc_code"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return beneficiaries;
    }

    public String transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String note, String adminUsername) {
        if (fromAccountNumber.equalsIgnoreCase(toAccountNumber)) {
            return "Source and target account cannot be the same.";
        }

        String updateSql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        String transferSql = """
                INSERT INTO transfers(from_account_id, to_account_id, amount, note, created_by)
                VALUES (?, ?, ?, ?, ?)
                """;
        String transactionSql = """
                INSERT INTO transactions(account_id, transaction_type, amount, balance_after, note)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try {
                LockedAccount fromAccount = loadLockedAccount(connection, fromAccountNumber);
                LockedAccount toAccount = loadLockedAccount(connection, toAccountNumber);

                if (fromAccount == null || toAccount == null) {
                    connection.rollback();
                    return "One of the accounts was not found or is inactive.";
                }

                if (fromAccount.balance().compareTo(amount) < 0) {
                    connection.rollback();
                    return "Insufficient funds for transfer.";
                }

                BigDecimal fromBalance = fromAccount.balance().subtract(amount);
                BigDecimal toBalance = toAccount.balance().add(amount);

                try (PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                     PreparedStatement transferStatement = connection.prepareStatement(transferSql, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement transactionStatement = connection.prepareStatement(transactionSql)) {

                    updateStatement.setBigDecimal(1, fromBalance);
                    updateStatement.setInt(2, fromAccount.accountId());
                    updateStatement.executeUpdate();

                    updateStatement.setBigDecimal(1, toBalance);
                    updateStatement.setInt(2, toAccount.accountId());
                    updateStatement.executeUpdate();

                    transferStatement.setInt(1, fromAccount.accountId());
                    transferStatement.setInt(2, toAccount.accountId());
                    transferStatement.setBigDecimal(3, amount);
                    transferStatement.setString(4, note == null || note.isBlank() ? null : note);
                    transferStatement.setString(5, normalizeAdmin(adminUsername));
                    transferStatement.executeUpdate();

                    int transferId = 0;
                    try (ResultSet keys = transferStatement.getGeneratedKeys()) {
                        if (keys.next()) {
                            transferId = keys.getInt(1);
                        }
                    }

                    transactionStatement.setInt(1, fromAccount.accountId());
                    transactionStatement.setString(2, "TRANSFER_OUT");
                    transactionStatement.setBigDecimal(3, amount);
                    transactionStatement.setBigDecimal(4, fromBalance);
                    transactionStatement.setString(5, "To " + toAccount.accountNumber() + appendNote(note));
                    transactionStatement.executeUpdate();

                    transactionStatement.setInt(1, toAccount.accountId());
                    transactionStatement.setString(2, "TRANSFER_IN");
                    transactionStatement.setBigDecimal(3, amount);
                    transactionStatement.setBigDecimal(4, toBalance);
                    transactionStatement.setString(5, "From " + fromAccount.accountNumber() + appendNote(note));
                    transactionStatement.executeUpdate();

                    logAction(connection, adminUsername, "TRANSFER", "TRANSFER", String.valueOf(transferId),
                            fromAccount.accountNumber() + " -> " + toAccount.accountNumber() + " | " + amount);
                }

                connection.commit();
                return "Transfer successful.";
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to complete transfer.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to complete transfer.";
        }
    }

    public List<TransferData> loadRecentTransfers() {
        List<TransferData> transfers = new ArrayList<>();
        String sql = """
                SELECT t.transfer_id, fa.account_number AS from_account, ta.account_number AS to_account,
                       fc.full_name AS from_customer, tc.full_name AS to_customer, t.amount, t.note, t.created_by, t.created_at
                FROM transfers t
                JOIN accounts fa ON fa.account_id = t.from_account_id
                JOIN accounts ta ON ta.account_id = t.to_account_id
                JOIN customers fc ON fc.customer_id = fa.customer_id
                JOIN customers tc ON tc.customer_id = ta.customer_id
                ORDER BY t.transfer_id DESC
                LIMIT 25
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return transfers;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transfers.add(new TransferData(
                            resultSet.getInt("transfer_id"),
                            resultSet.getString("from_account"),
                            resultSet.getString("to_account"),
                            resultSet.getString("from_customer"),
                            resultSet.getString("to_customer"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getString("note"),
                            resultSet.getString("created_by"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return transfers;
    }

    public List<StatementEntryData> loadStatementEntries(String accountNumber, LocalDate fromDate, LocalDate toDate) {
        List<StatementEntryData> entries = new ArrayList<>();
        String sql = """
                SELECT t.transaction_id, a.account_number, c.full_name, t.transaction_type, t.amount,
                       t.balance_after, t.note, t.created_at
                FROM transactions t
                JOIN accounts a ON a.account_id = t.account_id
                JOIN customers c ON c.customer_id = a.customer_id
                WHERE a.account_number = ?
                  AND t.created_at >= ?
                  AND t.created_at < ?
                ORDER BY t.transaction_id DESC
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return entries;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, accountNumber);
                statement.setTimestamp(2, Timestamp.valueOf(fromDate.atStartOfDay()));
                statement.setTimestamp(3, Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        entries.add(new StatementEntryData(
                                resultSet.getInt("transaction_id"),
                                resultSet.getString("account_number"),
                                resultSet.getString("full_name"),
                                resultSet.getString("transaction_type"),
                                resultSet.getBigDecimal("amount"),
                                resultSet.getBigDecimal("balance_after"),
                                resultSet.getString("note"),
                                resultSet.getTimestamp("created_at").toLocalDateTime()
                        ));
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return entries;
    }

    public List<AuditLogData> loadAuditLogs(int limit) {
        List<AuditLogData> logs = new ArrayList<>();
        String sql = """
                SELECT audit_id, admin_username, action_type, entity_type, entity_id, details, created_at
                FROM audit_logs
                ORDER BY audit_id DESC
                LIMIT ?
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return logs;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        logs.add(new AuditLogData(
                                resultSet.getInt("audit_id"),
                                resultSet.getString("admin_username"),
                                resultSet.getString("action_type"),
                                resultSet.getString("entity_type"),
                                resultSet.getString("entity_id"),
                                resultSet.getString("details"),
                                resultSet.getTimestamp("created_at").toLocalDateTime()
                        ));
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return logs;
    }

    public String issueLoan(int customerId, String loanType, BigDecimal principal, BigDecimal annualRate, int tenureMonths, String adminUsername) {
        BigDecimal monthlyEmi = calculateLoanEmi(principal, annualRate, tenureMonths);
        BigDecimal totalPayable = monthlyEmi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(2, RoundingMode.HALF_UP);
        String sql = """
                INSERT INTO loans(customer_id, loan_type, principal_amount, interest_rate, tenure_months, monthly_emi, total_payable, paid_amount, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0.00, 'ACTIVE')
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, customerId);
                statement.setString(2, loanType);
                statement.setBigDecimal(3, principal);
                statement.setBigDecimal(4, annualRate);
                statement.setInt(5, tenureMonths);
                statement.setBigDecimal(6, monthlyEmi);
                statement.setBigDecimal(7, totalPayable);
                statement.executeUpdate();

                int loanId = 0;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        loanId = keys.getInt(1);
                    }
                }

                logAction(connection, adminUsername, "CREATE", "LOAN", String.valueOf(loanId),
                        loanType + " | " + principal + " | EMI " + monthlyEmi);
                connection.commit();
                return "Loan issued successfully. EMI: " + monthlyEmi;
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to issue loan.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to issue loan.";
        }
    }

    public String recordLoanPayment(int loanId, BigDecimal amount, String note, String adminUsername) {
        String selectSql = "SELECT total_payable, paid_amount, status FROM loans WHERE loan_id = ? FOR UPDATE";
        String updateSql = "UPDATE loans SET paid_amount = ?, status = ? WHERE loan_id = ?";
        String insertSql = "INSERT INTO loan_payments(loan_id, amount, note) VALUES (?, ?, ?)";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setInt(1, loanId);
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        connection.rollback();
                        return "Loan not found.";
                    }

                    String status = resultSet.getString("status");
                    if ("PAID".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                        connection.rollback();
                        return "Loan is already closed.";
                    }

                    BigDecimal totalPayable = resultSet.getBigDecimal("total_payable");
                    BigDecimal paidAmount = resultSet.getBigDecimal("paid_amount");
                    BigDecimal newPaidAmount = paidAmount.add(amount);

                    if (newPaidAmount.compareTo(totalPayable) > 0) {
                        connection.rollback();
                        return "Payment exceeds remaining loan balance.";
                    }

                    String newStatus = newPaidAmount.compareTo(totalPayable) == 0 ? "PAID" : "ACTIVE";

                    try (PreparedStatement updateStatement = connection.prepareStatement(updateSql);
                         PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                        updateStatement.setBigDecimal(1, newPaidAmount);
                        updateStatement.setString(2, newStatus);
                        updateStatement.setInt(3, loanId);
                        updateStatement.executeUpdate();

                        insertStatement.setInt(1, loanId);
                        insertStatement.setBigDecimal(2, amount);
                        insertStatement.setString(3, note == null || note.isBlank() ? null : note);
                        insertStatement.executeUpdate();
                    }

                    logAction(connection, adminUsername, "PAYMENT", "LOAN", String.valueOf(loanId), "Loan payment " + amount);
                    connection.commit();
                    return "Loan payment recorded successfully.";
                }
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to record loan payment.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to record loan payment.";
        }
    }

    public List<LoanData> loadLoans() {
        List<LoanData> loans = new ArrayList<>();
        String sql = """
                SELECT l.loan_id, c.full_name, l.loan_type, l.principal_amount, l.interest_rate, l.tenure_months,
                       l.monthly_emi, l.total_payable, l.paid_amount, l.status, l.created_at
                FROM loans l
                JOIN customers c ON c.customer_id = l.customer_id
                ORDER BY l.loan_id DESC
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return loans;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    loans.add(new LoanData(
                            resultSet.getInt("loan_id"),
                            resultSet.getString("full_name"),
                            resultSet.getString("loan_type"),
                            resultSet.getBigDecimal("principal_amount"),
                            resultSet.getBigDecimal("interest_rate"),
                            resultSet.getInt("tenure_months"),
                            resultSet.getBigDecimal("monthly_emi"),
                            resultSet.getBigDecimal("total_payable"),
                            resultSet.getBigDecimal("paid_amount"),
                            resultSet.getString("status"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return loans;
    }

    public String createFixedDeposit(int customerId, BigDecimal principal, BigDecimal interestRate, int tenureMonths, String adminUsername) {
        BigDecimal maturityAmount = calculateFdMaturity(principal, interestRate, tenureMonths);
        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(tenureMonths);
        String sql = """
                INSERT INTO fixed_deposits(customer_id, principal_amount, interest_rate, tenure_months, maturity_amount, start_date, maturity_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, customerId);
                statement.setBigDecimal(2, principal);
                statement.setBigDecimal(3, interestRate);
                statement.setInt(4, tenureMonths);
                statement.setBigDecimal(5, maturityAmount);
                statement.setDate(6, Date.valueOf(startDate));
                statement.setDate(7, Date.valueOf(maturityDate));
                statement.executeUpdate();

                int fdId = 0;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        fdId = keys.getInt(1);
                    }
                }

                logAction(connection, adminUsername, "CREATE", "FIXED_DEPOSIT", String.valueOf(fdId),
                        principal + " | maturity " + maturityAmount);
                connection.commit();
                return "Fixed deposit created successfully.";
            } catch (Exception exception) {
                connection.rollback();
                exception.printStackTrace();
                return "Unable to create fixed deposit.";
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to create fixed deposit.";
        }
    }

    public List<FixedDepositData> loadFixedDeposits() {
        List<FixedDepositData> deposits = new ArrayList<>();
        String sql = """
                SELECT fd.fd_id, c.full_name, fd.principal_amount, fd.interest_rate, fd.tenure_months,
                       fd.maturity_amount, fd.start_date, fd.maturity_date, fd.status, fd.created_at
                FROM fixed_deposits fd
                JOIN customers c ON c.customer_id = fd.customer_id
                ORDER BY fd.fd_id DESC
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return deposits;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    deposits.add(new FixedDepositData(
                            resultSet.getInt("fd_id"),
                            resultSet.getString("full_name"),
                            resultSet.getBigDecimal("principal_amount"),
                            resultSet.getBigDecimal("interest_rate"),
                            resultSet.getInt("tenure_months"),
                            resultSet.getBigDecimal("maturity_amount"),
                            resultSet.getDate("start_date").toLocalDate(),
                            resultSet.getDate("maturity_date").toLocalDate(),
                            resultSet.getString("status"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return deposits;
    }

    private void logAction(Connection connection, String adminUsername, String actionType, String entityType, String entityId, String details) throws Exception {
        String sql = "INSERT INTO audit_logs(admin_username, action_type, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeAdmin(adminUsername));
            statement.setString(2, actionType);
            statement.setString(3, entityType);
            statement.setString(4, entityId);
            statement.setString(5, details);
            statement.executeUpdate();
        }
    }

    private LockedAccount loadLockedAccount(Connection connection, String accountNumber) throws Exception {
        String sql = """
                SELECT a.account_id, a.account_number, a.balance, c.full_name
                FROM accounts a
                JOIN customers c ON c.customer_id = a.customer_id
                WHERE a.account_number = ? AND a.status = 'ACTIVE'
                FOR UPDATE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new LockedAccount(
                            resultSet.getInt("account_id"),
                            resultSet.getString("account_number"),
                            resultSet.getString("full_name"),
                            resultSet.getBigDecimal("balance")
                    );
                }
            }
        }
        return null;
    }

    private BigDecimal calculateLoanEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        double monthlyRate = annualRate.doubleValue() / 1200.0;
        double pow = Math.pow(1 + monthlyRate, tenureMonths);
        double emi = principal.doubleValue() * monthlyRate * pow / (pow - 1);
        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFdMaturity(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        double monthlyRate = annualRate.doubleValue() / 1200.0;
        double maturity = principal.doubleValue() * Math.pow(1 + monthlyRate, tenureMonths);
        return BigDecimal.valueOf(maturity).setScale(2, RoundingMode.HALF_UP);
    }

    private String appendNote(String note) {
        return note == null || note.isBlank() ? "" : " | " + note;
    }

    private String normalizeAdmin(String adminUsername) {
        return adminUsername == null || adminUsername.isBlank() ? "SYSTEM" : adminUsername;
    }

    private String generateAccountNumber() {
        long base = System.currentTimeMillis() % 1_000_000_000L;
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return "TV" + base + random;
    }

    private record LockedAccount(int accountId, String accountNumber, String customerName, BigDecimal balance) {
    }

    public record BeneficiaryData(int beneficiaryId, String ownerName, String nickname, String beneficiaryName,
                                  String beneficiaryAccountNumber, String bankName, String ifscCode, LocalDateTime createdAt) {
    }

    public record TransferData(int transferId, String fromAccountNumber, String toAccountNumber, String fromCustomerName,
                               String toCustomerName, BigDecimal amount, String note, String createdBy, LocalDateTime createdAt) {
    }

    public record StatementEntryData(int transactionId, String accountNumber, String customerName, String transactionType,
                                     BigDecimal amount, BigDecimal balanceAfter, String note, LocalDateTime createdAt) {
    }

    public record AuditLogData(int auditId, String adminUsername, String actionType, String entityType, String entityId,
                               String details, LocalDateTime createdAt) {
    }

    public record LoanData(int loanId, String customerName, String loanType, BigDecimal principalAmount,
                           BigDecimal interestRate, int tenureMonths, BigDecimal monthlyEmi, BigDecimal totalPayable,
                           BigDecimal paidAmount, String status, LocalDateTime createdAt) {
    }

    public record FixedDepositData(int fdId, String customerName, BigDecimal principalAmount, BigDecimal interestRate,
                                   int tenureMonths, BigDecimal maturityAmount, LocalDate startDate,
                                   LocalDate maturityDate, String status, LocalDateTime createdAt) {
    }
}
