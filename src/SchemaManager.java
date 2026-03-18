import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SchemaManager {
    private static final Logger LOGGER = AppLogger.get(SchemaManager.class);

    public static void initializeDatabase() {
        String[] createStatements = {
                """
                CREATE TABLE IF NOT EXISTS admin (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id INT PRIMARY KEY AUTO_INCREMENT,
                    full_name VARCHAR(120) NOT NULL,
                    email VARCHAR(120) NOT NULL UNIQUE,
                    phone VARCHAR(20),
                    address VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS accounts (
                    account_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_id INT NOT NULL,
                    account_number VARCHAR(20) NOT NULL UNIQUE,
                    account_type VARCHAR(30) NOT NULL,
                    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
                    account_id INT NOT NULL,
                    transaction_type VARCHAR(20) NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    balance_after DECIMAL(15,2) NOT NULL,
                    note VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id)
                        REFERENCES accounts(account_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS beneficiaries (
                    beneficiary_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_id INT NOT NULL,
                    nickname VARCHAR(100) NOT NULL,
                    beneficiary_name VARCHAR(120) NOT NULL,
                    beneficiary_account_number VARCHAR(20) NOT NULL,
                    bank_name VARCHAR(100),
                    ifsc_code VARCHAR(20),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_beneficiaries_customer FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS transfers (
                    transfer_id INT PRIMARY KEY AUTO_INCREMENT,
                    from_account_id INT NOT NULL,
                    to_account_id INT NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    note VARCHAR(255),
                    created_by VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_transfers_from_account FOREIGN KEY (from_account_id)
                        REFERENCES accounts(account_id) ON DELETE CASCADE,
                    CONSTRAINT fk_transfers_to_account FOREIGN KEY (to_account_id)
                        REFERENCES accounts(account_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS audit_logs (
                    audit_id INT PRIMARY KEY AUTO_INCREMENT,
                    admin_username VARCHAR(100) NOT NULL,
                    action_type VARCHAR(50) NOT NULL,
                    entity_type VARCHAR(50) NOT NULL,
                    entity_id VARCHAR(100),
                    details VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS loans (
                    loan_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_id INT NOT NULL,
                    loan_type VARCHAR(40) NOT NULL,
                    principal_amount DECIMAL(15,2) NOT NULL,
                    interest_rate DECIMAL(5,2) NOT NULL,
                    tenure_months INT NOT NULL,
                    monthly_emi DECIMAL(15,2) NOT NULL,
                    total_payable DECIMAL(15,2) NOT NULL,
                    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_loans_customer FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS loan_payments (
                    payment_id INT PRIMARY KEY AUTO_INCREMENT,
                    loan_id INT NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    note VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_loan_payments_loan FOREIGN KEY (loan_id)
                        REFERENCES loans(loan_id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS fixed_deposits (
                    fd_id INT PRIMARY KEY AUTO_INCREMENT,
                    customer_id INT NOT NULL,
                    principal_amount DECIMAL(15,2) NOT NULL,
                    interest_rate DECIMAL(5,2) NOT NULL,
                    tenure_months INT NOT NULL,
                    maturity_amount DECIMAL(15,2) NOT NULL,
                    start_date DATE NOT NULL,
                    maturity_date DATE NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_fd_customer FOREIGN KEY (customer_id)
                        REFERENCES customers(customer_id) ON DELETE CASCADE
                )
                """
        };

        try (Connection connection = Database.connectDB()) {
            if (connection == null) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                for (String sql : createStatements) {
                    statement.execute(sql);
                }
            }

            ensureColumn(connection, "admin", "role", "ALTER TABLE admin ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN'");
            ensureColumn(connection, "customers", "phone", "ALTER TABLE customers ADD COLUMN phone VARCHAR(20)");
            ensureColumn(connection, "customers", "address", "ALTER TABLE customers ADD COLUMN address VARCHAR(255)");
            ensureColumn(connection, "customers", "created_at", "ALTER TABLE customers ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(connection, "accounts", "status", "ALTER TABLE accounts ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
            ensureColumn(connection, "accounts", "created_at", "ALTER TABLE accounts ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(connection, "transactions", "note", "ALTER TABLE transactions ADD COLUMN note VARCHAR(255)");
            ensureColumn(connection, "transactions", "balance_after", "ALTER TABLE transactions ADD COLUMN balance_after DECIMAL(15,2) NOT NULL DEFAULT 0.00");
            ensureColumn(connection, "transactions", "created_at", "ALTER TABLE transactions ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(connection, "loans", "total_payable", "ALTER TABLE loans ADD COLUMN total_payable DECIMAL(15,2) NOT NULL DEFAULT 0.00");
            ensureIndex(connection, "accounts", "idx_accounts_customer", "CREATE INDEX idx_accounts_customer ON accounts(customer_id)");
            ensureIndex(connection, "transactions", "idx_transactions_account_date", "CREATE INDEX idx_transactions_account_date ON transactions(account_id, created_at)");
            ensureIndex(connection, "transactions", "idx_transactions_account_txn", "CREATE INDEX idx_transactions_account_txn ON transactions(account_id, transaction_id)");
            ensureIndex(connection, "transfers", "idx_transfers_from_date", "CREATE INDEX idx_transfers_from_date ON transfers(from_account_id, created_at)");
            ensureIndex(connection, "transfers", "idx_transfers_to_date", "CREATE INDEX idx_transfers_to_date ON transfers(to_account_id, created_at)");
            ensureIndex(connection, "audit_logs", "idx_audit_logs_created", "CREATE INDEX idx_audit_logs_created ON audit_logs(created_at)");
            ensureIndex(connection, "audit_logs", "idx_audit_logs_admin_created", "CREATE INDEX idx_audit_logs_admin_created ON audit_logs(admin_username, created_at)");
            ensureIndex(connection, "loans", "idx_loans_customer_status", "CREATE INDEX idx_loans_customer_status ON loans(customer_id, status)");
            ensureIndex(connection, "loan_payments", "idx_loan_payments_loan_date", "CREATE INDEX idx_loan_payments_loan_date ON loan_payments(loan_id, created_at)");
            ensureIndex(connection, "fixed_deposits", "idx_fixed_deposits_customer_status", "CREATE INDEX idx_fixed_deposits_customer_status ON fixed_deposits(customer_id, status)");
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Database schema initialization failed.", exception);
        }
    }

    private static void ensureColumn(Connection connection, String tableName, String columnName, String alterSql) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    try (Statement alterStatement = connection.createStatement()) {
                        alterStatement.execute(alterSql);
                    }
                }
            }
        }
    }

    private static void ensureIndex(Connection connection, String tableName, String indexName, String createSql) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    try (Statement createStatement = connection.createStatement()) {
                        createStatement.execute(createSql);
                    }
                }
            }
        }
    }
}
