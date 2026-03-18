import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BankingRepository {

    public List<CustomerData> loadCustomers() {
        List<CustomerData> customers = new ArrayList<>();
        String sql = "SELECT customer_id, full_name, email, phone FROM customers ORDER BY customer_id DESC";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return customers;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(new CustomerData(
                            resultSet.getInt("customer_id"),
                            resultSet.getString("full_name"),
                            resultSet.getString("email"),
                            resultSet.getString("phone")
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return customers;
    }

    public String addCustomer(String fullName, String email, String phone, String address) {
        String sql = "INSERT INTO customers(full_name, email, phone, address) VALUES (?, ?, ?, ?)";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return "Database connection failed.";
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, fullName);
                statement.setString(2, email);
                statement.setString(3, phone.isBlank() ? null : phone);
                statement.setString(4, address.isBlank() ? null : address);
                statement.executeUpdate();
                return "Customer created successfully.";
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return "Unable to create customer. Email may already exist.";
        }
    }

    public List<Dashboard.CustomerOption> loadCustomerOptions() {
        List<Dashboard.CustomerOption> options = new ArrayList<>();
        String sql = "SELECT customer_id, full_name, email FROM customers ORDER BY full_name";

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return options;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String display = resultSet.getString("full_name") + " (" + resultSet.getString("email") + ")";
                    options.add(new Dashboard.CustomerOption(resultSet.getInt("customer_id"), display));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return options;
    }

    public String createAccount(int customerId, String accountType, BigDecimal initialDeposit) {
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

                if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    try (PreparedStatement transactionStatement = connection.prepareStatement(insertTransactionSql)) {
                        transactionStatement.setInt(1, accountId);
                        transactionStatement.setBigDecimal(2, initialDeposit);
                        transactionStatement.setBigDecimal(3, initialDeposit);
                        transactionStatement.setString(4, "Initial deposit");
                        transactionStatement.executeUpdate();
                    }
                }

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

    public List<AccountData> loadAccounts() {
        List<AccountData> accounts = new ArrayList<>();
        String sql = """
                SELECT a.account_number, c.full_name, a.account_type, a.balance, a.status
                FROM accounts a
                JOIN customers c ON c.customer_id = a.customer_id
                ORDER BY a.account_id DESC
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return accounts;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(new AccountData(
                            resultSet.getString("account_number"),
                            resultSet.getString("full_name"),
                            resultSet.getString("account_type"),
                            resultSet.getBigDecimal("balance"),
                            resultSet.getString("status")
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return accounts;
    }

    public AccountSnapshot findAccount(String accountNumber) {
        String sql = """
                SELECT a.account_id, a.account_number, c.full_name, a.balance
                FROM accounts a
                JOIN customers c ON c.customer_id = a.customer_id
                WHERE a.account_number = ? AND a.status = 'ACTIVE'
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return null;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, accountNumber);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new AccountSnapshot(
                                resultSet.getInt("account_id"),
                                resultSet.getString("account_number"),
                                resultSet.getString("full_name"),
                                resultSet.getBigDecimal("balance")
                        );
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public String deposit(String accountNumber, BigDecimal amount, String note) {
        return applyTransaction(accountNumber, amount, note, true);
    }

    public String withdraw(String accountNumber, BigDecimal amount, String note) {
        return applyTransaction(accountNumber, amount, note, false);
    }

    private String applyTransaction(String accountNumber, BigDecimal amount, String note, boolean deposit) {
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

                    if (!deposit && newBalance.compareTo(BigDecimal.ZERO) < 0) {
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

    public List<TransactionData> loadRecentTransactions() {
        List<TransactionData> transactions = new ArrayList<>();
        String sql = """
                SELECT t.transaction_id, a.account_number, c.full_name, t.transaction_type, t.amount, t.balance_after, t.created_at
                FROM transactions t
                JOIN accounts a ON a.account_id = t.account_id
                JOIN customers c ON c.customer_id = a.customer_id
                ORDER BY t.transaction_id DESC
                LIMIT 25
                """;

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return transactions;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(new TransactionData(
                            resultSet.getInt("transaction_id"),
                            resultSet.getString("account_number"),
                            resultSet.getString("full_name"),
                            resultSet.getString("transaction_type"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getBigDecimal("balance_after"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return transactions;
    }

    public OverviewStats loadOverviewStats() {
        int customerCount = count("SELECT COUNT(*) FROM customers");
        int accountCount = count("SELECT COUNT(*) FROM accounts");
        int transactionCount = count("SELECT COUNT(*) FROM transactions");
        BigDecimal totalBalance = sum("SELECT COALESCE(SUM(balance), 0) FROM accounts");

        return new OverviewStats(customerCount, accountCount, totalBalance, transactionCount);
    }

    private int count(String sql) {
        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return 0;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    private BigDecimal sum(String sql) {
        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return BigDecimal.ZERO;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    private String generateAccountNumber() {
        long base = System.currentTimeMillis() % 1_000_000_000L;
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return "TV" + base + random;
    }

    public record CustomerData(int customerId, String fullName, String email, String phone) {
    }

    public record AccountData(String accountNumber, String customerName, String accountType, BigDecimal balance, String status) {
    }

    public record TransactionData(int transactionId, String accountNumber, String customerName, String transactionType,
                                  BigDecimal amount, BigDecimal balanceAfter, LocalDateTime createdAt) {
    }

    public record AccountSnapshot(int accountId, String accountNumber, String customerName, BigDecimal balance) {
    }

    public record OverviewStats(int customerCount, int accountCount, BigDecimal totalBalance, int transactionCount) {
    }
}
