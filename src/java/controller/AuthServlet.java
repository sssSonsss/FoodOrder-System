package controller;

import dao.UserAddressDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import model.UserAddress;
import utils.AuthHelper;
import utils.PasswordUtil;

import java.io.IOException;
import java.util.List;

/**
 * Đăng nhập, đăng ký, đăng xuất và API trạng thái / địa chỉ.
 */
@WebServlet("/AuthServlet")
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final UserAddressDAO userAddressDAO = new UserAddressDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        response.setCharacterEncoding("UTF-8");

        if ("status".equals(action)) {
            serveStatus(request, response);
            return;
        }
        if ("addresses".equals(action)) {
            serveAddresses(request, response);
            return;
        }
        if ("addressDetail".equals(action)) {
            serveAddressDetail(request, response);
            return;
        }
        if ("logout".equals(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/index.html");
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String ctx = request.getContextPath();

        if ("login".equals(action)) {
            handleLogin(request, response, ctx);
            return;
        }
        if ("register".equals(action)) {
            handleRegister(request, response, ctx);
            return;
        }

        response.sendRedirect(ctx + "/login.html?error=invalid");
    }

    private void serveStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        int uid = AuthHelper.getUserId(request);
        if (uid <= 0) {
            response.getWriter().print("{\"loggedIn\":false}");
            return;
        }
        HttpSession session = request.getSession(false);
        String username = session != null ? String.valueOf(session.getAttribute("username")) : "";
        String fullName = session != null ? String.valueOf(session.getAttribute("fullName")) : "";
        response.getWriter().print("{"
                + "\"loggedIn\":true,"
                + "\"userId\":" + uid + ","
                + "\"username\":\"" + escapeJson(username) + "\","
                + "\"fullName\":\"" + escapeJson(fullName) + "\""
                + "}");
    }

    private void serveAddresses(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        int uid = AuthHelper.getUserId(request);
        if (uid <= 0) {
            response.setStatus(401);
            response.getWriter().print("{\"needLogin\":true,\"items\":[]}");
            return;
        }
        List<UserAddress> list = userAddressDAO.listByUserId(uid);
        StringBuilder sb = new StringBuilder("{\"items\":[");
        boolean first = true;
        for (UserAddress a : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(addressToJson(a));
        }
        sb.append("]}");
        response.getWriter().print(sb.toString());
    }

    private void serveAddressDetail(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        int uid = AuthHelper.getUserId(request);
        int aid = parseInt(request.getParameter("id"), 0);
        if (uid <= 0 || aid <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Thiếu thông tin\"}");
            return;
        }
        UserAddress a = userAddressDAO.findByIdForUser(aid, uid);
        if (a == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Không tìm thấy địa chỉ\"}");
            return;
        }
        response.getWriter().print(addressToJsonObject(a));
    }

    private String addressToJson(UserAddress a) {
        return "{"
                + "\"id\":" + a.getId() + ","
                + "\"label\":\"" + escapeJson(nvl(a.getLabel())) + "\","
                + "\"recipientName\":\"" + escapeJson(nvl(a.getRecipientName())) + "\","
                + "\"phone\":\"" + escapeJson(nvl(a.getPhone())) + "\","
                + "\"addressLine\":\"" + escapeJson(nvl(a.getAddressLine())) + "\","
                + "\"province\":\"" + escapeJson(nvl(a.getProvince())) + "\","
                + "\"district\":\"" + escapeJson(nvl(a.getDistrict())) + "\","
                + "\"latitude\":" + (a.getLatitude() != null ? a.getLatitude() : "null") + ","
                + "\"longitude\":" + (a.getLongitude() != null ? a.getLongitude() : "null") + ","
                + "\"isDefault\":" + a.isDefaultAddress() + ","
                + "\"summary\":\"" + escapeJson(buildSummary(a)) + "\""
                + "}";
    }

    private String addressToJsonObject(UserAddress a) {
        return "{\"address\":" + addressToJson(a) + "}";
    }

    private String buildSummary(UserAddress a) {
        StringBuilder s = new StringBuilder();
        if (a.getRecipientName() != null && !a.getRecipientName().isBlank()) {
            s.append(a.getRecipientName().trim()).append(" · ");
        }
        if (a.getPhone() != null && !a.getPhone().isBlank()) {
            s.append(a.getPhone().trim()).append(" · ");
        }
        s.append(a.getAddressLine() == null ? "" : a.getAddressLine().trim());
        if (a.getDistrict() != null && !a.getDistrict().isBlank()) {
            s.append(", ").append(a.getDistrict().trim());
        }
        if (a.getProvince() != null && !a.getProvince().isBlank()) {
            s.append(", ").append(a.getProvince().trim());
        }
        return s.toString();
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response, String ctx)
            throws IOException {

        String account = request.getParameter("account");
        String password = request.getParameter("password");
        String redirect = safeRedirect(request.getParameter("redirect"));

        if (account == null || password == null || account.isBlank() || password.isBlank()) {
            response.sendRedirect(ctx + "/login.html?error=missing&redirect=" + enc(redirect));
            return;
        }

        User user = userDAO.findByLoginAccount(account.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            response.sendRedirect(ctx + "/login.html?error=credentials&redirect=" + enc(redirect));
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("fullName", user.getFullName());

        response.sendRedirect(ctx + redirect);
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response, String ctx)
            throws IOException {

        String username = trim(request.getParameter("username"));
        String email = trim(request.getParameter("email"));
        String password = request.getParameter("password");
        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String label = trim(request.getParameter("addressLabel"));
        String recipientName = trim(request.getParameter("recipientName"));
        String addressPhone = trim(request.getParameter("addressPhone"));
        String addressLine = trim(request.getParameter("addressLine"));
        String province = trim(request.getParameter("province"));
        String district = trim(request.getParameter("district"));
        String latStr = trim(request.getParameter("latitude"));
        String lngStr = trim(request.getParameter("longitude"));
        String redirect = safeRedirect(request.getParameter("redirect"));

        if (username.length() < 3 || username.length() > 40 || password == null || password.length() < 6
                || fullName.length() < 2 || email.length() < 5 || !email.contains("@")
                || addressLine.length() < 5) {
            response.sendRedirect(ctx + "/register.html?error=validation&redirect=" + enc(redirect));
            return;
        }

        if (userDAO.existsUsername(username)) {
            response.sendRedirect(ctx + "/register.html?error=user_exists&redirect=" + enc(redirect));
            return;
        }
        if (userDAO.existsEmail(email)) {
            response.sendRedirect(ctx + "/register.html?error=email_exists&redirect=" + enc(redirect));
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setFullName(fullName);
        user.setPhone(phone);

        UserAddress addr = new UserAddress();
        addr.setLabel(label.isEmpty() ? "Nhà" : label);
        addr.setRecipientName(recipientName.isEmpty() ? fullName : recipientName);
        addr.setPhone(addressPhone.isEmpty() ? phone : addressPhone);
        addr.setAddressLine(addressLine);
        addr.setProvince(province);
        addr.setDistrict(district);
        addr.setLatitude(parseDoubleOrNull(latStr));
        addr.setLongitude(parseDoubleOrNull(lngStr));

        int uid = userDAO.register(user, addr);
        if (uid <= 0) {
            response.sendRedirect(ctx + "/register.html?error=system&redirect=" + enc(redirect));
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("userId", uid);
        session.setAttribute("username", username);
        session.setAttribute("fullName", fullName);

        response.sendRedirect(ctx + redirect);
    }

    /** Chỉ cho phép đường dẫn nội bộ, tránh open redirect. */
    private String safeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return "/index.html";
        }
        String r = redirect.trim();
        if (r.contains("..") || r.startsWith("//") || r.startsWith("http") || r.startsWith("javascript")) {
            return "/index.html";
        }
        if (!r.startsWith("/")) {
            r = "/" + r;
        }
        return r;
    }

    private String enc(String redirect) {
        try {
            return java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String v, int def) {
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
