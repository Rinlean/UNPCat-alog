package main.stuff;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class dbconn {

    private static final String URL = "jdbc:mysql://192.168.100.12:3306/unpcat_alog";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // XAMPP default is empty

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Lookup account username by userid.
     *
     * @param userId the numeric userid (accounts.userid)
     * @return the username if found, or null if no account with that id exists
     * @throws SQLException if a DB error occurs
     */
    public static String getAccountNameById(int userId) throws SQLException {
        final String sql = "SELECT username FROM accounts WHERE account_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Lookup account username by userid string (convenience overload).
     *
     * @param userIdStr userid as String (will try to parse)
     * @return the username if found, or null if not found or parse error
     * @throws SQLException if a DB error occurs
     */
    public static String getAccountNameById(String userIdStr) throws SQLException {
        if (userIdStr == null) {
            return null;
        }
        try {
            int id = Integer.parseInt(userIdStr);
            return getAccountNameById(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
