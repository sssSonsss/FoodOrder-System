package utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Đọc phiên đăng nhập và chuyển hướng tới trang đăng nhập khi cần.
 */
public final class AuthHelper {

    private AuthHelper() {
    }

    /** userId từ session; 0 nếu chưa đăng nhập (không dùng user giả). */
    public static int getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object[] keys = {
                    session.getAttribute("userId"),
                    session.getAttribute("user_id"),
                    session.getAttribute("uid")
            };
            for (Object key : keys) {
                int id = parseObjectToInt(key);
                if (id > 0) {
                    return id;
                }
            }
        }
        return 0;
    }

    public static void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri.substring(ctx.length());
        String q = request.getQueryString();
        String dest = q != null ? path + "?" + q : path;
        if (!dest.startsWith("/")) {
            dest = "/" + dest;
        }
        String target = ctx + "/login.html?redirect="
                + URLEncoder.encode(dest, StandardCharsets.UTF_8);
        response.sendRedirect(target);
    }

    private static int parseObjectToInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
