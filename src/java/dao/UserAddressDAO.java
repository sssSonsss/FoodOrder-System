package dao;

import model.UserAddress;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** CRUD địa chỉ giao hàng theo user. */
public class UserAddressDAO {

    public List<UserAddress> listByUserId(int userId) {
        List<UserAddress> list = new ArrayList<>();
        String sql = "SELECT id, user_id, label, recipient_name, phone, address_line, province, district, "
                   + "latitude, longitude, is_default FROM user_addresses WHERE user_id = ? ORDER BY is_default DESC, id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
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

    /** Địa chỉ thuộc đúng user (để tạo đơn). */
    public UserAddress findByIdForUser(int addressId, int userId) {
        String sql = "SELECT id, user_id, label, recipient_name, phone, address_line, province, district, "
                   + "latitude, longitude, is_default FROM user_addresses WHERE id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addressId);
            ps.setInt(2, userId);
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

    private UserAddress mapRow(ResultSet rs) throws Exception {
        UserAddress a = new UserAddress();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setLabel(rs.getString("label"));
        a.setRecipientName(rs.getString("recipient_name"));
        a.setPhone(rs.getString("phone"));
        a.setAddressLine(rs.getString("address_line"));
        a.setProvince(rs.getString("province"));
        a.setDistrict(rs.getString("district"));
        double lat = rs.getDouble("latitude");
        if (!rs.wasNull()) {
            a.setLatitude(lat);
        }
        double lng = rs.getDouble("longitude");
        if (!rs.wasNull()) {
            a.setLongitude(lng);
        }
        a.setDefaultAddress(rs.getBoolean("is_default"));
        return a;
    }
}
