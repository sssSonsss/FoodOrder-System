package dao;

import model.Voucher;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Truy vấn voucher, ví người dùng và lịch sử sử dụng (demo PostgreSQL).
 */
public class VoucherDAO {

    /** Kết quả áp dụng voucher khi thanh toán (preview). */
    public static class ApplyResult {
        public boolean ok;
        public String message;
        public double discountAmount;
        public String discountType;

        public ApplyResult(boolean ok, String message, double discountAmount, String discountType) {
            this.ok = ok;
            this.message = message;
            this.discountAmount = discountAmount;
            this.discountType = discountType;
        }
    }

    private static final String BASE_SELECT = "SELECT v.id, v.code, v.title, v.description, v.discount_type, "
            + "v.discount_value::float8 AS discount_value, v.min_order_value::float8 AS min_order_value, "
            + "v.max_discount_amount::float8 AS max_discount_amount, v.usage_limit, v.used_count, "
            + "TO_CHAR(v.expiry_date, 'YYYY-MM-DD') AS expiry_date, v.is_active, v.promo_scope ";

    /**
     * Ưu đãi gợi ý: voucher đang hoạt động, chưa hết hạn (điều kiện fake đơn giản trong SQL).
     */
    public List<Voucher> listOffers(int userId, String keyword, String discountTypeFilter) {
        List<Voucher> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append(", CASE WHEN uv.user_id IS NOT NULL THEN TRUE ELSE FALSE END AS claimed ")
                .append("FROM vouchers v ")
                .append("LEFT JOIN user_vouchers uv ON uv.voucher_id = v.id AND uv.user_id = ? ")
                .append("WHERE v.is_active = TRUE AND v.expiry_date >= CURRENT_DATE ");
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(v.code) LIKE ? OR LOWER(v.title) LIKE ? OR LOWER(COALESCE(v.description,'')) LIKE ?) ");
        }
        if (discountTypeFilter != null && !discountTypeFilter.isEmpty() && !"ALL".equalsIgnoreCase(discountTypeFilter)) {
            sql.append("AND v.discount_type = ? ");
        }
        sql.append("ORDER BY v.id ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setInt(i++, userId);
            if (keyword != null && !keyword.trim().isEmpty()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.setString(i++, like);
                ps.setString(i++, like);
                ps.setString(i++, like);
            }
            if (discountTypeFilter != null && !discountTypeFilter.isEmpty() && !"ALL".equalsIgnoreCase(discountTypeFilter)) {
                ps.setString(i++, discountTypeFilter.toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapVoucher(rs, true));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (list.isEmpty()) {
            list.addAll(buildFakeOffers(userId));
        }
        return list;
    }

    /** Voucher đã nhận vào ví, chưa dùng trong đơn (mỗi mã một dòng — trùng DB do thiếu UNIQUE được gộp). */
    public List<Voucher> listMyWallet(int userId) {
        List<Voucher> list = new ArrayList<>();
        String inner = "SELECT DISTINCT ON (uv.voucher_id) "
                + "v.id, v.code, v.title, v.description, v.discount_type, "
                + "v.discount_value::float8 AS discount_value, v.min_order_value::float8 AS min_order_value, "
                + "v.max_discount_amount::float8 AS max_discount_amount, v.usage_limit, v.used_count, "
                + "TO_CHAR(v.expiry_date, 'YYYY-MM-DD') AS expiry_date, v.is_active, v.promo_scope, "
                + "TRUE AS claimed, TO_CHAR(uv.claimed_at, 'YYYY-MM-DD HH24:MI:SS') AS claimed_at "
                + "FROM user_vouchers uv "
                + "JOIN vouchers v ON v.id = uv.voucher_id "
                + "WHERE uv.user_id = ? AND v.is_active = TRUE "
                + "ORDER BY uv.voucher_id, uv.claimed_at DESC NULLS LAST";
        String sql = "SELECT * FROM (" + inner + ") wallet ORDER BY wallet.claimed_at DESC NULLS LAST";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Voucher v = mapVoucher(rs, true);
                    v.setClaimedAt(rs.getString("claimed_at"));
                    list.add(v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        list = dedupeWalletByVoucherId(list);
        return list;
    }

    private List<Voucher> dedupeWalletByVoucherId(List<Voucher> list) {
        Map<Integer, Voucher> map = new LinkedHashMap<>();
        for (Voucher v : list) {
            map.putIfAbsent(v.getId(), v);
        }
        return new ArrayList<>(map.values());
    }

    /** Mọi lần dùng voucher (mỗi dòng voucher_usage — gồm dùng lại cùng mã sau khi nhận lại). */
    public List<Voucher> listUsedHistory(int userId) {
        List<Voucher> list = new ArrayList<>();
        String sql = "SELECT h.id AS usage_id, " + BASE_SELECT.substring(7)
                + ", TO_CHAR(h.used_at, 'YYYY-MM-DD HH24:MI:SS') AS used_at, "
                + "h.discount_amount::float8 AS saved_discount, h.order_id AS related_order_id "
                + "FROM voucher_usage h "
                + "JOIN vouchers v ON v.id = h.voucher_id "
                + "WHERE h.user_id = ? "
                + "ORDER BY h.used_at DESC NULLS LAST, h.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Voucher v = mapVoucher(rs, false);
                    int usageId = rs.getInt("usage_id");
                    if (!rs.wasNull()) {
                        v.setUsageRecordId(usageId);
                    }
                    v.setUsedAt(rs.getString("used_at"));
                    v.setSavedDiscount(rs.getDouble("saved_discount"));
                    int oid = rs.getInt("related_order_id");
                    if (!rs.wasNull()) {
                        v.setRelatedOrderId(oid);
                    }
                    list.add(v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /** Nhận ưu đãi từ tab “Ưu đãi của tôi” vào ví. */
    public boolean claimVoucher(int userId, int voucherId) {
        String checkSql = "SELECT id FROM vouchers WHERE id = ? AND is_active = TRUE AND expiry_date >= CURRENT_DATE";
        String insertSql = "INSERT INTO user_vouchers (user_id, voucher_id) VALUES (?, ?) "
                + "ON CONFLICT (user_id, voucher_id) DO NOTHING";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setInt(1, voucherId);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
            }
            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                ins.setInt(1, userId);
                ins.setInt(2, voucherId);
                if (ins.executeUpdate() > 0) {
                    return true;
                }
            }
            try (PreparedStatement ex = conn.prepareStatement(
                    "SELECT 1 FROM user_vouchers WHERE user_id = ? AND voucher_id = ?")) {
                ex.setInt(1, userId);
                ex.setInt(2, voucherId);
                try (ResultSet rs = ex.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kiểm tra mã trong ví và tính giảm giá (preview checkout).
     */
    public ApplyResult previewApply(int userId, String rawCode, double subtotal, double shippingFee) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return new ApplyResult(false, "Chưa nhập mã giảm giá.", 0, "");
        }
        String code = rawCode.trim().toUpperCase();

        String sql = BASE_SELECT
                + "FROM vouchers v "
                + "JOIN user_vouchers uv ON uv.voucher_id = v.id AND uv.user_id = ? "
                + "WHERE UPPER(TRIM(v.code)) = ? AND v.is_active = TRUE AND v.expiry_date >= CURRENT_DATE ";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new ApplyResult(false,
                            "Mã không có trong ví hoặc đã hết hạn. Hãy nhận ưu đãi trước tại trang Voucher.",
                            0, "");
                }
                Voucher v = mapVoucher(rs, false);
                return calculateDiscount(v, subtotal, shippingFee);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApplyResult(false, "Không kiểm tra được voucher (lỗi hệ thống).", 0, "");
        }
    }

    private ApplyResult calculateDiscount(Voucher v, double subtotal, double shippingFee) {
        if (subtotal < v.getMinOrderValue()) {
            return new ApplyResult(false,
                    "Đơn chưa đạt giá trị tối thiểu " + formatMoney(v.getMinOrderValue()) + ".",
                    0, v.getDiscountType());
        }

        String type = v.getDiscountType() == null ? "" : v.getDiscountType().toUpperCase();
        double discount = 0;

        switch (type) {
            case "PERCENT":
                discount = subtotal * (v.getDiscountValue() / 100.0);
                if (v.getMaxDiscountAmount() != null && v.getMaxDiscountAmount() > 0) {
                    discount = Math.min(discount, v.getMaxDiscountAmount());
                }
                break;
            case "AMOUNT":
                discount = Math.min(v.getDiscountValue(), subtotal);
                break;
            case "FREESHIP":
                discount = Math.max(0, shippingFee);
                break;
            default:
                return new ApplyResult(false, "Loại voucher không hỗ trợ.", 0, type);
        }

        discount = Math.round(discount);
        if (discount <= 0) {
            return new ApplyResult(false, "Không thể áp dụng giảm giá với giỏ hàng hiện tại.", 0, type);
        }

        return new ApplyResult(true, "Áp dụng mã " + v.getCode() + " thành công.", discount, type);
    }

    /**
     * Sau khi đặt đơn thành công: ghi lịch sử, giải phóng khỏi ví, tăng used_count.
     */
    public void markVoucherUsed(int userId, String rawCode, int orderId, double discountAmount) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            return;
        }
        String code = rawCode.trim().toUpperCase();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer voucherId = null;
                String sel = "SELECT v.id FROM vouchers v "
                        + "JOIN user_vouchers uv ON uv.voucher_id = v.id AND uv.user_id = ? "
                        + "WHERE UPPER(TRIM(v.code)) = ?";
                try (PreparedStatement ps = conn.prepareStatement(sel)) {
                    ps.setInt(1, userId);
                    ps.setString(2, code);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            voucherId = rs.getInt(1);
                        }
                    }
                }
                if (voucherId == null) {
                    conn.rollback();
                    return;
                }

                String ins = "INSERT INTO voucher_usage (user_id, voucher_id, order_id, discount_amount) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, voucherId);
                    if (orderId > 0) {
                        ps.setInt(3, orderId);
                    } else {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    }
                    ps.setDouble(4, Math.max(0, discountAmount));
                    ps.executeUpdate();
                }

                String delUv = "DELETE FROM user_vouchers WHERE user_id = ? AND voucher_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(delUv)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, voucherId);
                    ps.executeUpdate();
                }

                String bump = "UPDATE vouchers SET used_count = used_count + 1 WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(bump)) {
                    ps.setInt(1, voucherId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Admin/demo: thêm voucher mới */
    public int createVoucher(Voucher v) {
        String sql = "INSERT INTO vouchers (code, title, description, discount_type, discount_value, min_order_value, "
                + "max_discount_amount, usage_limit, expiry_date, is_active, promo_scope) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::date, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getCode().trim().toUpperCase());
            ps.setString(2, v.getTitle());
            ps.setString(3, v.getDescription());
            ps.setString(4, v.getDiscountType().toUpperCase());
            ps.setDouble(5, v.getDiscountValue());
            ps.setDouble(6, v.getMinOrderValue());
            if (v.getMaxDiscountAmount() != null) {
                ps.setDouble(7, v.getMaxDiscountAmount());
            } else {
                ps.setNull(7, java.sql.Types.NUMERIC);
            }
            if (v.getUsageLimit() != null) {
                ps.setInt(8, v.getUsageLimit());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.setString(9, v.getExpiryDate());
            ps.setBoolean(10, v.isActive());
            ps.setString(11, v.getPromoScope() == null ? "ALL" : v.getPromoScope());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Voucher mapVoucher(ResultSet rs, boolean readClaimedFlag) throws Exception {
        Double maxD = rs.getObject("max_discount_amount") != null ? rs.getDouble("max_discount_amount") : null;
        Integer usageLim = rs.getObject("usage_limit") != null ? rs.getInt("usage_limit") : null;
        Voucher v = new Voucher(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("discount_type"),
                rs.getDouble("discount_value"),
                rs.getDouble("min_order_value"),
                maxD,
                usageLim,
                rs.getInt("used_count"),
                rs.getString("expiry_date"),
                rs.getBoolean("is_active"),
                rs.getString("promo_scope")
        );
        if (readClaimedFlag) {
            try {
                v.setClaimed(rs.getBoolean("claimed"));
            } catch (Exception ignored) {
                v.setClaimed(false);
            }
        }
        return v;
    }

    private String formatMoney(double v) {
        return String.format("%,.0fđ", v);
    }

    private List<Voucher> buildFakeOffers(int userId) {
        List<Voucher> list = new ArrayList<>();
        Voucher a = new Voucher(101, "NEWUSER", "Chào thành viên mới",
                "Giảm 20% cho đơn đầu tiên (demo).", "PERCENT", 20, 50000, 40000.0, null, 120,
                "2099-12-31", true, "FOOD");
        a.setClaimed(userId == 1);
        list.add(a);
        Voucher b = new Voucher(102, "FREESHIP", "Miễn phí ship",
                "Freeship tối đa một lần trong demo.", "FREESHIP", 0, 80000, null, null, 800,
                "2099-12-31", true, "SHIP");
        b.setClaimed(false);
        list.add(b);
        return list;
    }

}
