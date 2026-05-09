package dao;

import model.Category;
import utils.DBConnection;
import utils.FoodImageUrls;

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

        // Một dòng / danh mục; đếm món bằng subquery (tránh nhân bản hàng khi JOIN).
        // DISTINCT ON: nếu DB có trùng tên danh mục (id khác nhau), chỉ giữ id nhỏ nhất.
        String sql = "SELECT DISTINCT ON (LOWER(TRIM(c.name))) "
                   + "c.id, c.name, c.description, c.image_url, c.is_active, "
                   + "(SELECT COUNT(*)::int FROM foods f WHERE f.category_id = c.id AND f.is_active = true) AS item_count "
                   + "FROM categories c "
                   + "WHERE c.is_active = true "
                   + "ORDER BY LOWER(TRIM(c.name)), c.id";

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
                   + "(SELECT COUNT(*)::int FROM foods f WHERE f.category_id = c.id AND f.is_active = true) AS item_count "
                   + "FROM categories c "
                   + "WHERE c.id = ? AND c.is_active = true";

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
            FoodImageUrls.orDefault(rs.getString("image_url")),
            rs.getBoolean("is_active"),
            rs.getInt("item_count")
        );
    }
}
