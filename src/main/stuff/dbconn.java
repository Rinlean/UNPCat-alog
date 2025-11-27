package main.stuff;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class dbconn {

    private static final String URL = "jdbc:mysql://192.168.100.12:3306/unpcat_alog";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

}
