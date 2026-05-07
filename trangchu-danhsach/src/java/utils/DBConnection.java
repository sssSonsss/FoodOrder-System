package utils;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Tiện ích kết nối CSDL PostgreSQL.
 * Database: laptrinhweb_btl_foodorder
 * Chỉnh sửa host/user/password nếu cần.
 */
public class DBConnection {

    private static final String URL =
        "jdbc:postgresql://localhost:5432/laptrinhweb_btl_foodorder";

    private static final String USER     = "postgres";
    private static final String PASSWORD = "123";

    public static Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
