package dao;

import model.Food;
import utils.DBConnection;
import utils.FoodImageUrls;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO thao tác bảng foods.
 *
 * Các phương thức:
 *  - getFoods(search, categoryId, sort) : Tìm kiếm + lọc + sắp xếp
 *  - getFoodById(id)                    : Chi tiết một món
 *  - getFeaturedFoods(limit)            : Món nổi bật cho trang chủ
 *  - getFoodsByCategory(categoryId)     : Lọc theo danh mục
 */
public class FoodDAO {

    // ===== Tìm kiếm + lọc + sắp xếp =====
    public List<Food> getFoods(String search, int categoryId, String sort) {

        List<Food> list = new ArrayList<>();

        // Build câu SQL động
        StringBuilder sql = new StringBuilder(
            "SELECT f.id, f.category_id, c.name AS category_name, "
          + "f.name, f.description, f.price, f.image_url, "
          + "f.is_active, TO_CHAR(f.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
          + "f.rating, f.review_count "
          + "FROM foods f "
          + "LEFT JOIN categories c ON f.category_id = c.id "
          + "WHERE f.is_active = true "
        );

        List<Object> params = new ArrayList<>();

        // Filter: tìm kiếm theo tên hoặc mô tả (tiếng Việt qua ILIKE)
        if (search != null && !search.trim().isEmpty()) {
            String q = "%" + search.trim() + "%";
            sql.append("AND (f.name ILIKE ? OR f.description ILIKE ?) ");
            params.add(q);
            params.add(q);
        }

        // Filter: lọc theo danh mục
        if (categoryId > 0) {
            sql.append("AND f.category_id = ? ");
            params.add(categoryId);
        }

        // Sort
        switch (sort == null ? "" : sort) {
            case "price_asc":
                sql.append("ORDER BY f.price ASC");
                break;
            case "price_desc":
                sql.append("ORDER BY f.price DESC");
                break;
            case "rating":
                sql.append("ORDER BY f.rating DESC");
                break;
            case "newest":
                sql.append("ORDER BY f.created_at DESC");
                break;
            default:
                sql.append("ORDER BY f.id ASC");
                break;
        }
        // PostgreSQL: không cần thêm gì, LIMIT sẽ không dùng ở đây

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Gán tham số động
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===== Chi tiết một món =====
    public Food getFoodById(int id) {

        String sql = "SELECT f.id, f.category_id, c.name AS category_name, "
                   + "f.name, f.description, f.price, f.image_url, "
                   + "f.is_active, TO_CHAR(f.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
                   + "f.rating, f.review_count "
                   + "FROM foods f "
                   + "LEFT JOIN categories c ON f.category_id = c.id "
                   + "WHERE f.id = ? AND f.is_active = true";

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

    // ===== Món nổi bật (rating cao nhất) =====
    public List<Food> getFeaturedFoods(int limit) {

        List<Food> list = new ArrayList<>();

        String sql = "SELECT f.id, f.category_id, c.name AS category_name, "
                   + "f.name, f.description, f.price, f.image_url, "
                   + "f.is_active, TO_CHAR(f.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
                   + "f.rating, f.review_count "
                   + "FROM foods f "
                   + "LEFT JOIN categories c ON f.category_id = c.id "
                   + "WHERE f.is_active = true "
                   + "ORDER BY f.rating DESC, f.review_count DESC "
                   + "LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ===== Lọc theo danh mục =====
    public List<Food> getFoodsByCategory(int categoryId) {
        return getFoods(null, categoryId, null);
    }

    // ===== Helper: map ResultSet → Food =====
    private Food mapRow(ResultSet rs) throws SQLException {
        return new Food(
            rs.getInt("id"),
            rs.getInt("category_id"),
            rs.getString("category_name"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getDouble("price"),
            FoodImageUrls.orDefault(rs.getString("image_url")),
            rs.getBoolean("is_active"),
            rs.getString("created_at"),
            rs.getDouble("rating"),
            rs.getInt("review_count")
        );
    }
}
