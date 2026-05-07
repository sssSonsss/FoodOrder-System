package dao;

import model.Order;
import model.OrderItem;
import model.OrderStatusLog;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public int createOrderFromCart(int userId, int addressId) throws Exception {
        String sqlCart = "SELECT c.food_id, c.quantity, f.price "
                       + "FROM cart_items c "
                       + "JOIN foods f ON c.food_id = f.id "
                       + "WHERE c.user_id = ?";

        String sqlInsertOrder = "INSERT INTO orders (user_id, total_price, status, address_id) "
                              + "VALUES (?, ?, ?, ?) RETURNING id";

        String sqlInsertItem = "INSERT INTO order_items (order_id, food_id, quantity, price) "
                             + "VALUES (?, ?, ?, ?)";

        String sqlDeleteCart = "DELETE FROM cart_items WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<OrderItem> items = new ArrayList<>();
                double total = 0;

                try (PreparedStatement psCart = conn.prepareStatement(sqlCart)) {
                    psCart.setInt(1, userId);
                    try (ResultSet rs = psCart.executeQuery()) {
                        while (rs.next()) {
                            int quantity = rs.getInt("quantity");
                            double price = rs.getDouble("price");
                            items.add(new OrderItem(0, rs.getInt("food_id"), "", quantity, price, ""));
                            total += quantity * price;
                        }
                    }
                }

                if (items.isEmpty()) {
                    conn.rollback();
                    return -1;
                }

                int orderId;
                try (PreparedStatement psOrder = conn.prepareStatement(sqlInsertOrder)) {
                    psOrder.setInt(1, userId);
                    psOrder.setDouble(2, total);
                    psOrder.setString(3, "CHO_XAC_NHAN");
                    psOrder.setInt(4, addressId);
                    try (ResultSet rs = psOrder.executeQuery()) {
                        if (!rs.next()) {
                            throw new Exception("Không thể tạo đơn hàng");
                        }
                        orderId = rs.getInt(1);
                    }
                }

                try (PreparedStatement psItem = conn.prepareStatement(sqlInsertItem)) {
                    for (OrderItem item : items) {
                        psItem.setInt(1, orderId);
                        psItem.setInt(2, item.getFoodId());
                        psItem.setInt(3, item.getQuantity());
                        psItem.setDouble(4, item.getPrice());
                        psItem.addBatch();
                    }
                    psItem.executeBatch();
                }

                insertStatusLog(conn, orderId, "CHO_XAC_NHAN", "Đơn hàng vừa được tạo");

                try (PreparedStatement psDelete = conn.prepareStatement(sqlDeleteCart)) {
                    psDelete.setInt(1, userId);
                    psDelete.executeUpdate();
                }

                conn.commit();
                return orderId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Order> getOrdersByUser(int userId, String status) {
        List<Order> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT id, user_id, total_price, status, address_id, "
          + "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at "
          + "FROM orders WHERE user_id = ? "
        );

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND status = ? ");
        }

        sql.append("ORDER BY id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setInt(1, userId);
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(2, status.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public Order getOrderById(int orderId, int userId) {
        String sql = "SELECT id, user_id, total_price, status, address_id, "
                   + "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at "
                   + "FROM orders WHERE id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapOrder(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> list = new ArrayList<>();

        String sql = "SELECT oi.order_id, oi.food_id, oi.quantity, oi.price, f.name, f.image_url "
                   + "FROM order_items oi "
                   + "JOIN foods f ON oi.food_id = f.id "
                   + "WHERE oi.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderItem(
                        rs.getInt("order_id"),
                        rs.getInt("food_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("image_url")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean cancelOrder(int orderId, int userId) {
        String sqlCheck = "SELECT status FROM orders WHERE id = ? AND user_id = ?";
        String sqlCancel = "UPDATE orders SET status = 'DA_HUY' WHERE id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String status = null;
                try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                    psCheck.setInt(1, orderId);
                    psCheck.setInt(2, userId);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            status = rs.getString("status");
                        }
                    }
                }

                if (status == null || (!"CHO_XAC_NHAN".equals(status) && !"DANG_CHUAN_BI".equals(status))) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement psCancel = conn.prepareStatement(sqlCancel)) {
                    psCancel.setInt(1, orderId);
                    psCancel.setInt(2, userId);
                    if (psCancel.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                insertStatusLog(conn, orderId, "DA_HUY", "Người dùng đã hủy đơn");

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean reorder(int orderId, int userId) {
        String sqlOrder = "SELECT id FROM orders WHERE id = ? AND user_id = ?";
        String sqlItems = "SELECT food_id, quantity FROM order_items WHERE order_id = ?";
        String sqlUpsert = "INSERT INTO cart_items (user_id, food_id, quantity) VALUES (?, ?, ?) "
                         + "ON CONFLICT (user_id, food_id) DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean hasOrder = false;
                try (PreparedStatement ps = conn.prepareStatement(sqlOrder)) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        hasOrder = rs.next();
                    }
                }

                if (!hasOrder) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement psItems = conn.prepareStatement(sqlItems);
                     PreparedStatement psUpsert = conn.prepareStatement(sqlUpsert)) {

                    psItems.setInt(1, orderId);
                    try (ResultSet rs = psItems.executeQuery()) {
                        while (rs.next()) {
                            psUpsert.setInt(1, userId);
                            psUpsert.setInt(2, rs.getInt("food_id"));
                            psUpsert.setInt(3, rs.getInt("quantity"));
                            psUpsert.addBatch();
                        }
                    }
                    psUpsert.executeBatch();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean addReview(int orderId, int userId, int foodId, int rating, String comment) {
        String sqlCheck = "SELECT o.status FROM orders o "
                        + "JOIN order_items oi ON o.id = oi.order_id "
                        + "WHERE o.id = ? AND o.user_id = ? AND oi.food_id = ?";

        String sqlInsert = "INSERT INTO reviews (order_id, user_id, food_id, rating, comment) "
                         + "VALUES (?, ?, ?, ?, ?)";

        String sqlUpdateFood = "UPDATE foods SET "
                             + "review_count = COALESCE(review_count, 0) + 1, "
                             + "rating = ((COALESCE(rating, 0) * COALESCE(review_count, 0)) + ?) / (COALESCE(review_count, 0) + 1) "
                             + "WHERE id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String status = null;
                try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                    psCheck.setInt(1, orderId);
                    psCheck.setInt(2, userId);
                    psCheck.setInt(3, foodId);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) {
                            status = rs.getString("status");
                        }
                    }
                }

                if (!"DA_GIAO_THANH_CONG".equals(status)) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                    psInsert.setInt(1, orderId);
                    psInsert.setInt(2, userId);
                    psInsert.setInt(3, foodId);
                    psInsert.setInt(4, rating);
                    psInsert.setString(5, comment);
                    psInsert.executeUpdate();
                }

                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateFood)) {
                    psUpdate.setInt(1, rating);
                    psUpdate.setInt(2, foodId);
                    psUpdate.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<OrderStatusLog> getStatusLogs(int orderId) {
        List<OrderStatusLog> list = new ArrayList<>();
        String sql = "SELECT id, order_id, status, note, TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at "
                   + "FROM order_status_logs WHERE order_id = ? ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderStatusLog(
                        rs.getInt("id"),
                        rs.getInt("order_id"),
                        rs.getString("status"),
                        rs.getString("note"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void appendStatusLog(int orderId, String status, String note) {
        String sql = "INSERT INTO order_status_logs (order_id, status, note) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, status);
            ps.setString(3, note);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertStatusLog(Connection conn, int orderId, String status, String note) throws Exception {
        String sql = "INSERT INTO order_status_logs (order_id, status, note) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.NO_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setString(2, status);
            ps.setString(3, note);
            ps.executeUpdate();
        }
    }

    private Order mapOrder(ResultSet rs) throws Exception {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getDouble("total_price"),
            rs.getString("status"),
            rs.getInt("address_id"),
            rs.getString("created_at")
        );
    }
}
