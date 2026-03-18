import java.sql.Connection;

public class CheckDB {
    public static void main(String[] args) {
        SchemaManager.initializeDatabase();
        Connection conn = Database.connectDB();
        if (conn != null) {
            System.out.println("SUCCESS! Java can now talk to the TrustVault database.");
        } else {
            System.out.println("FAILED! Check the error message above.");
        }
    }
}
