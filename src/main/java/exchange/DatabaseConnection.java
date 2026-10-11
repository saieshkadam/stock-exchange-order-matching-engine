package exchange;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL="jdbc:mysql://localhost:3306/matching_engine";
    private static final String USER="root";
    private static final String PASSWORD="saiesh";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

