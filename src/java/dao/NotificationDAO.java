package dao;

import model.Notification;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> getNotificationsByUserId(int userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT id, user_id, title, message, type, is_read, "
                   + "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at "
                   + "FROM notifications WHERE user_id = ? ORDER BY id DESC LIMIT 20";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getBoolean("is_read"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (list.isEmpty()) {
            list.addAll(buildFakeNotifications(userId));
        }
        return list;
    }

    public int countUnreadByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = true WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createNotification(int userId, String title, String message, String type) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, is_read) VALUES (?, ?, ?, ?, false)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<Notification> buildFakeNotifications(int userId) {
        List<Notification> fake = new ArrayList<>();
        fake.add(new Notification(0, userId, "Đơn #9002 đang giao",
                "Shipper đang đến với bạn. Dự kiến giao trong 10 phút nữa.", "ORDER", false, "Vừa xong"));
        fake.add(new Notification(0, userId, "Đơn #8998 đã hoàn thành",
                "Đơn hàng đã giao thành công. Mời bạn đánh giá món ăn để nhận xu.", "ORDER", false, "2 phút trước"));
        fake.add(new Notification(0, userId, "Đơn #8997 đã hủy",
                "Đơn bị hủy do cửa hàng quá tải. Bạn có thể bấm Mua lại để đặt lại nhanh.", "SYSTEM", false, "5 phút trước"));
        return fake;
    }
}
