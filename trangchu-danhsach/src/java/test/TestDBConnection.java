package test;

import utils.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * ============================================================
 * TestDBConnection.java – Kiểm tra kết nối PostgreSQL
 * ============================================================
 * Cách chạy trong NetBeans:
 *   Chuột phải vào file → Run File (Shift+F6)
 *
 * Checklist trước khi chạy:
 *   1. PostgreSQL đang chạy ở cổng 5432
 *   2. Database "laptrinhweb_btl_foodorder" đã được tạo
 *   3. postgresql-xx.jar đã thêm vào Libraries của project
 *   4. User/password khớp với DBConnection.java
 * ============================================================
 */
public class TestDBConnection {

    public static void main(String[] args) {

        System.out.println("============================================================");
        System.out.println("  CHECK KẾT NỐI POSTGRESQL");
        System.out.println("============================================================");
        System.out.println("  URL  : jdbc:postgresql://localhost:5432/laptrinhweb_btl_foodorder");
        System.out.println("  User : postgres");
        System.out.println("============================================================\n");

        // ===== BƯỚC 1: Load driver =====
        System.out.println("[1/4] Load PostgreSQL JDBC Driver...");
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("      ✅ Driver loaded: org.postgresql.Driver\n");
        } catch (ClassNotFoundException e) {
            System.out.println("      ❌ KHÔNG TÌM THẤY DRIVER!");
            System.out.println("      → Giải pháp: Thêm postgresql-xx.jar vào project Libraries");
            System.out.println("        (Chuột phải project → Properties → Libraries → Add JAR)");
            return;
        }

        // ===== BƯỚC 2: Mở connection =====
        System.out.println("[2/4] Mở kết nối tới database...");
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null || conn.isClosed()) {
                System.out.println("      ❌ Connection null hoặc đã đóng ngay!\n");
                return;
            }
            System.out.println("      ✅ Kết nối thành công!\n");
        } catch (Exception e) {
            System.out.println("      ❌ LỖI KẾT NỐI: " + e.getMessage());
            printErrorHint(e.getMessage());
            return;
        }

        // ===== BƯỚC 3: Đọc metadata =====
        System.out.println("[3/4] Thông tin Database Server...");
        try {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("      ✅ DB Product  : " + meta.getDatabaseProductName());
            System.out.println("      ✅ DB Version  : " + meta.getDatabaseProductVersion());
            System.out.println("      ✅ JDBC Driver : " + meta.getDriverName());
            System.out.println("      ✅ Driver Ver  : " + meta.getDriverVersion());
            System.out.println();
        } catch (Exception e) {
            System.out.println("      ⚠️  Không đọc được metadata: " + e.getMessage() + "\n");
        }

        // ===== BƯỚC 4: Kiểm tra bảng =====
        System.out.println("[4/4] Kiểm tra bảng trong database...");
        try (Statement stmt = conn.createStatement()) {

            // Kiểm tra bảng categories
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        System.out.println("      ✅ Bảng categories : " + count + " dòng");
                    } else {
                        System.out.println("      ⚠️  Bảng categories : 0 dòng (chưa có data - chạy BTL_FoodMenu.sql)");
                    }
                }
            } catch (Exception e) {
                System.out.println("      ❌ Bảng categories không tồn tại → Chạy BTL_FoodMenu.sql trước!");
            }

            // Kiểm tra bảng foods
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM foods")) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    if (count > 0) {
                        System.out.println("      ✅ Bảng foods      : " + count + " dòng");
                    } else {
                        System.out.println("      ⚠️  Bảng foods      : 0 dòng (chưa có data - chạy BTL_FoodMenu.sql)");
                    }
                }
            } catch (Exception e) {
                System.out.println("      ❌ Bảng foods không tồn tại → Chạy BTL_FoodMenu.sql trước!");
            }

            // Kiểm tra bảng vouchers (module voucher)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vouchers")) {
                if (rs.next()) {
                    System.out.println("      ✅ Bảng vouchers   : " + rs.getInt(1) + " dòng");
                }
            } catch (Exception e) {
                System.out.println("      ℹ️  Bảng vouchers   : chưa tạo (bình thường nếu chưa chạy BTL.sql)");
            }

        } catch (Exception e) {
            System.out.println("      ❌ Lỗi query: " + e.getMessage());
        } finally {
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }

        System.out.println();
        System.out.println("============================================================");
        System.out.println("  ✅ KẾT NỐI THÀNH CÔNG – Database sẵn sàng!");
        System.out.println("============================================================");
    }

    // Gợi ý xử lý lỗi theo message
    private static void printErrorHint(String msg) {
        if (msg == null) return;
        System.out.println();
        if (msg.contains("Connection refused") || msg.contains("connect")) {
            System.out.println("      → NGUYÊN NHÂN: PostgreSQL chưa chạy hoặc sai cổng");
            System.out.println("      → Kiểm tra: pg_ctl status  hoặc  brew services list");
            System.out.println("      → Khởi động: pg_ctl start  hoặc  brew services start postgresql");
        } else if (msg.contains("password") || msg.contains("authentication")) {
            System.out.println("      → NGUYÊN NHÂN: Sai password");
            System.out.println("      → Sửa PASSWORD trong DBConnection.java cho đúng");
        } else if (msg.contains("database") && msg.contains("does not exist")) {
            System.out.println("      → NGUYÊN NHÂN: Database chưa được tạo");
            System.out.println("      → Chạy lệnh: CREATE DATABASE laptrinhweb_btl_foodorder;");
        } else if (msg.contains("role") && msg.contains("does not exist")) {
            System.out.println("      → NGUYÊN NHÂN: User 'postgres' không tồn tại");
            System.out.println("      → Sửa USER trong DBConnection.java cho đúng");
        }
        System.out.println();
    }
}
