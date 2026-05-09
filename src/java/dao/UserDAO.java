package dao;

import model.User;
import model.UserAddress;
import utils.DBConnection;
import utils.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Đăng ký và đăng nhập người dùng.
 */
public class UserDAO {

    public User findByLoginAccount(String account) {
        if (account == null || account.trim().isEmpty()) {
            return null;
        }
        String key = account.trim().toLowerCase();
        String sql = "SELECT id, username, email, password_hash, full_name, phone FROM users "
                   + "WHERE LOWER(username) = ? OR LOWER(email) = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean existsUsername(String username) {
        return existsField("username", username.trim().toLowerCase());
    }

    public boolean existsEmail(String email) {
        return existsField("email", email.trim().toLowerCase());
    }

    private boolean existsField(String column, String valueLower) {
        String sql = "SELECT 1 FROM users WHERE LOWER(" + column + ") = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, valueLower);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tạo tài khoản và một địa chỉ mặc định trong một transaction.
     *
     * @return user id hoặc -1 khi trùng email/username hoặc lỗi
     */
    public int register(User user, UserAddress address) {
        String insUser = "INSERT INTO users (username, email, password_hash, full_name, phone) "
                       + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        String insAddr = "INSERT INTO user_addresses (user_id, label, recipient_name, phone, address_line, "
                       + "province, district, latitude, longitude, is_default) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = conn.prepareStatement(insUser)) {
                    ps.setString(1, user.getUsername().trim());
                    ps.setString(2, user.getEmail().trim().toLowerCase());
                    ps.setString(3, user.getPasswordHash());
                    ps.setString(4, user.getFullName().trim());
                    ps.setString(5, user.getPhone() == null ? "" : user.getPhone().trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return -1;
                        }
                        userId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insAddr)) {
                    ps.setInt(1, userId);
                    ps.setString(2, address.getLabel() == null ? "Nhà" : address.getLabel().trim());
                    ps.setString(3, address.getRecipientName() == null ? user.getFullName() : address.getRecipientName().trim());
                    ps.setString(4, address.getPhone() == null || address.getPhone().isBlank()
                            ? user.getPhone() : address.getPhone().trim());
                    ps.setString(5, address.getAddressLine().trim());
                    ps.setString(6, address.getProvince() == null ? "" : address.getProvince().trim());
                    ps.setString(7, address.getDistrict() == null ? "" : address.getDistrict().trim());
                    if (address.getLatitude() != null) {
                        ps.setDouble(8, address.getLatitude());
                    } else {
                        ps.setNull(8, java.sql.Types.DOUBLE);
                    }
                    if (address.getLongitude() != null) {
                        ps.setDouble(9, address.getLongitude());
                    } else {
                        ps.setNull(9, java.sql.Types.DOUBLE);
                    }
                    ps.executeUpdate();
                }

                conn.commit();
                return userId;
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return -1;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private User mapUser(ResultSet rs) throws Exception {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("full_name"),
                rs.getString("phone")
        );
    }

    public boolean verifyCredentials(String account, String plainPassword) {
        User u = findByLoginAccount(account);
        return u != null && PasswordUtil.verify(plainPassword, u.getPasswordHash());
    }
}
