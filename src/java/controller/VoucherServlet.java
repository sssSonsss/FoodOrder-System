package controller;

import dao.VoucherDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Voucher;
import utils.AuthHelper;

import java.io.IOException;
import java.util.List;

/**
 * Trang voucher (JSP) + API JSON cho các tab và thao tác nhận ưu đãi / preview thanh toán.
 */
@WebServlet("/VoucherServlet")
public class VoucherServlet extends HttpServlet {

    private final VoucherDAO voucherDAO = new VoucherDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if ("api".equals(action)) {
            serveApi(request, response);
            return;
        }

        if (AuthHelper.getUserId(request) <= 0) {
            AuthHelper.redirectToLogin(request, response);
            return;
        }

        request.getRequestDispatcher("/vouchers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int userId = AuthHelper.getUserId(request);
        if (userId <= 0) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"ok\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập\"}");
            return;
        }

        if ("preview".equals(action)) {
            handlePreview(request, response, userId);
            return;
        }
        if ("claim".equals(action)) {
            handleClaim(request, response, userId);
            return;
        }

        response.setStatus(400);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print("{\"ok\":false,\"message\":\"Action không hợp lệ\"}");
    }

    private void serveApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        int userId = AuthHelper.getUserId(request);
        if (userId <= 0) {
            response.setStatus(401);
            response.getWriter().print("[]");
            return;
        }
        String tab = request.getParameter("tab");
        if (tab == null) tab = "offers";
        String keyword = request.getParameter("keyword");
        String filter = request.getParameter("filter");
        if (filter == null || filter.isBlank()) {
            filter = "ALL";
        }

        List<Voucher> list;
        switch (tab) {
            case "my":
                list = voucherDAO.listMyWallet(userId);
                break;
            case "history":
                list = voucherDAO.listUsedHistory(userId);
                break;
            case "offers":
            default:
                list = voucherDAO.listOffers(userId, keyword, filter);
                break;
        }

        response.getWriter().print(toJsonArray(list, tab));
    }

    private void handlePreview(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String code = request.getParameter("code");
        double subtotal = parseDouble(request.getParameter("subtotal"), 0);
        double shippingFee = parseDouble(request.getParameter("shippingFee"), 0);

        VoucherDAO.ApplyResult r = voucherDAO.previewApply(userId, code, subtotal, shippingFee);
        response.getWriter().print("{"
                + "\"ok\":" + r.ok + ","
                + "\"message\":\"" + escapeJson(r.message) + "\","
                + "\"discount\":" + r.discountAmount + ","
                + "\"discountType\":\"" + escapeJson(r.discountType) + "\""
                + "}");
    }

    private void handleClaim(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        int voucherId = parseInt(request.getParameter("voucherId"), 0);
        if (voucherId <= 0 || userId <= 0) {
            response.getWriter().print("{\"ok\":false,\"message\":\"Thông tin không hợp lệ\"}");
            return;
        }
        boolean ok = voucherDAO.claimVoucher(userId, voucherId);
        if (ok) {
            response.getWriter().print("{\"ok\":true,\"message\":\"Đã thêm voucher vào kho của bạn.\"}");
        } else {
            response.getWriter().print("{\"ok\":false,\"message\":\"Không nhận được voucher (đã có trong ví hoặc hết hạn).\"}");
        }
    }

    private String toJsonArray(List<Voucher> list, String tab) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Voucher v : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(v.getId()).append(",")
              .append("\"code\":\"").append(escapeJson(v.getCode())).append("\",")
              .append("\"title\":\"").append(escapeJson(v.getTitle())).append("\",")
              .append("\"description\":\"").append(escapeJson(v.getDescription())).append("\",")
              .append("\"discountType\":\"").append(escapeJson(v.getDiscountType())).append("\",")
              .append("\"discountValue\":").append(v.getDiscountValue()).append(",")
              .append("\"minOrderValue\":").append(v.getMinOrderValue()).append(",")
              .append("\"expiryDate\":\"").append(escapeJson(v.getExpiryDate())).append("\",")
              .append("\"promoScope\":\"").append(escapeJson(v.getPromoScope())).append("\",")
              .append("\"claimed\":").append(v.isClaimed());
            if ("my".equals(tab)) {
                sb.append(",\"claimedAt\":\"").append(escapeJson(v.getClaimedAt())).append("\"");
            }
            if ("history".equals(tab)) {
                sb.append(",\"usedAt\":\"").append(escapeJson(v.getUsedAt())).append("\",")
                  .append("\"savedDiscount\":").append(v.getSavedDiscount()).append(",")
                  .append("\"orderId\":").append(v.getRelatedOrderId() != null ? v.getRelatedOrderId() : "null");
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
