import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {
    private static final Logger LOGGER = AppLogger.get(Database.class);

    public static Connection connectDB() {
        try {
            return DriverManager.getConnection(AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Database connection failed.", exception);
            return null;
        }
    }
}
