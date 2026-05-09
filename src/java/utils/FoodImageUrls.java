package utils;

/**
 * Ảnh mặc định khi DB thiếu hoặc còn đường placeholder cũ — luôn là URL ảnh HTTPS trực tiếp.
 */
public final class FoodImageUrls {

    private FoodImageUrls() {}

    /** Phở bò (Wikimedia Commons) — ảnh mặc định chung. */
    public static final String DEFAULT =
            "https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg";

    /** Bánh mì — dùng làm fallback khi ảnh chính lỗi tải. */
    public static final String ALT =
            "https://upload.wikimedia.org/wikipedia/commons/5/5f/B%C3%A1nh_m%C3%AC_th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng_in_Saigon.jpg";

    public static String orDefault(String imageUrl) {
        if (imageUrl == null) {
            return DEFAULT;
        }
        String t = imageUrl.trim();
        if (t.isEmpty()) {
            return DEFAULT;
        }
        // Ảnh demo cũ trong DB (SVG local) — luôn thay bằng URL ảnh thật
        if (t.startsWith("images/") && t.endsWith("-placeholder.svg")) {
            return DEFAULT;
        }
        return t;
    }
}
