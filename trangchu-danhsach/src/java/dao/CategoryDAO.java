package dao;

import model.Category;
import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO thao tác bảng categories.
 *
 * Các phương thức:
 *  - getAllCategories()   : Lấy tất cả danh mục đang active
 *  - getCategoryById(id) : Lấy danh mục theo ID
 */
public class CategoryDAO {

    // ===== Lấy tất cả danh mục =====
    public List<Category> getAllCategories() {

        List<Category> list = new ArrayList<>();

        String sql = "SELECT c.id, c.name, c.description, c.image_url, c.is_active, "
                   + "COUNT(f.id) as item_count "
                   + "FROM categories c "
                   + "LEFT JOIN foods f ON c.id = f.category_id AND f.is_active = true "
                   + "WHERE c.is_active = true "
                   + "GROUP BY c.id, c.name, c.description, c.image_url, c.is_active "
                   + "ORDER BY c.name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Category c = mapRow(rs);
                list.add(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===== Lấy danh mục theo ID =====
    public Category getCategoryById(int id) {

        String sql = "SELECT c.id, c.name, c.description, c.image_url, c.is_active, "
                   + "COUNT(f.id) as item_count "
                   + "FROM categories c "
                   + "LEFT JOIN foods f ON c.id = f.category_id AND f.is_active = true "
                   + "WHERE c.id = ? AND c.is_active = true "
                   + "GROUP BY c.id, c.name, c.description, c.image_url, c.is_active";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ===== Helper: map ResultSet → Category =====
    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("image_url"),
            rs.getBoolean("is_active"),
            rs.getInt("item_count")
        );
    }
}
