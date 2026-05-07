package dao;

import model.CartItem;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    public List<CartItem> getCartByUserId(int userId) {
        List<CartItem> list = new ArrayList<>();
        String sql = "SELECT c.id, c.food_id, c.quantity, f.name, f.price, f.image_url "
                   + "FROM cart_items c "
                   + "JOIN foods f ON c.food_id = f.id "
                   + "WHERE c.user_id = ? "
                   + "ORDER BY c.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CartItem(
                        rs.getInt("id"),
                        rs.getInt("food_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity"),
                        rs.getString("image_url")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean addToCart(int userId, int foodId, int quantity) {
        String sqlCheck = "SELECT id, quantity FROM cart_items WHERE user_id = ? AND food_id = ?";
        String sqlInsert = "INSERT INTO cart_items (user_id, food_id, quantity) VALUES (?, ?, ?)";
        String sqlUpdate = "UPDATE cart_items SET quantity = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
                psCheck.setInt(1, userId);
                psCheck.setInt(2, foodId);

                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        int cartItemId = rs.getInt("id");
                        int oldQuantity = rs.getInt("quantity");
                        try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                            psUpdate.setInt(1, oldQuantity + quantity);
                            psUpdate.setInt(2, cartItemId);
                            return psUpdate.executeUpdate() > 0;
                        }
                    }
                }
            }

            try (PreparedStatement psInsert = conn.prepareStatement(sqlInsert)) {
                psInsert.setInt(1, userId);
                psInsert.setInt(2, foodId);
                psInsert.setInt(3, quantity);
                return psInsert.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateQuantity(int cartItemId, int quantity) {
        String sql = "UPDATE cart_items SET quantity = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, cartItemId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFromFile(int cartItemId) {
        String sql = "DELETE FROM cart_items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartItemId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkStock(int foodId, int quantity) {
        if (quantity <= 0) return false;
        if (quantity > 10) return false;
        String sql = "SELECT COUNT(*) FROM foods WHERE id = ? AND is_active = true";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, foodId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
