package controller;

import dao.CartDAO;
import dao.NotificationDAO;
import dao.OrderDAO;
import model.CartItem;
import model.Order;
import model.OrderItem;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.AuthHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/CartServlet")
public class CartServlet extends HttpServlet {

    private final CartDAO cartDAO = new CartDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int userId = AuthHelper.getUserId(request);

        String view = request.getParameter("view");
        if (view == null || view.trim().isEmpty()) {
            view = "cart";
        }

        if ("tracking-data".equals(view)) {
            disableBrowserCache(response);
            response.setContentType("application/json;charset=UTF-8");
            if (userId <= 0) {
                response.getWriter().print("[]");
                return;
            }
            /* Không tự đẩy trạng thái đơn ở đây (tránh mất đơn khỏi tab sau vài lần poll). */
            List<Order> delivering = getOrdersByStatuses(userId, 0, 1, 2);
            response.getWriter().print(trackingOrdersToJson(delivering));
            return;
        }

        if ("cart-data".equals(view)) {
            disableBrowserCache(response);
            response.setContentType("application/json;charset=UTF-8");
            if (userId <= 0) {
                response.getWriter().print("{\"items\":[],\"grand_total\":0}");
                return;
            }
            maybeRefillSampleCartForApi(request, userId);
            List<CartItem> cartItems = cartDAO.getCartByUserId(userId);
            double grandTotal = calculateGrandTotal(cartItems);
            response.getWriter().print("{\"items\":" + cartItemsToJson(cartItems) + ",\"grand_total\":" + grandTotal + "}");
            return;
        }

        if (userId <= 0) {
            AuthHelper.redirectToLogin(request, response);
            return;
        }

        disableBrowserCache(response);
        /* Tab Giỏ: sau đặt hàng giỏ bị xóa — với phiên đăng nhập demo, tự nạp lại món mẫu để luôn có dữ liệu hiển thị */
        if ("cart".equals(view)) {
            maybeRefillSampleCartForPage(request, userId);
        }

        List<CartItem> cartItems = cartDAO.getCartByUserId(userId);
        /* 0 = chờ xác nhận — phải hiện cùng tab với đơn đang xử lý */
        List<Order> deliveringOrders = getOrdersByStatuses(userId, 0, 1, 2);
        List<Order> historyOrders = getOrdersByStatuses(userId, 3, 4);

        Map<Integer, List<OrderItem>> orderItemsMap = new HashMap<>();
        for (Order order : deliveringOrders) {
            orderItemsMap.put(order.getId(), orderDAO.getOrderItems(order.getId()));
        }
        for (Order order : historyOrders) {
            orderItemsMap.put(order.getId(), orderDAO.getOrderItems(order.getId()));
        }

        double grandTotal = calculateGrandTotal(cartItems);

        request.setAttribute("cartItems", cartItems);
        request.setAttribute("grandTotal", grandTotal);
        request.setAttribute("deliveringOrders", deliveringOrders);
        request.setAttribute("historyOrders", historyOrders);
        request.setAttribute("orderItemsMap", orderItemsMap);
        request.setAttribute("activeView", view);
        request.setAttribute("emptyMessage", "Giỏ hàng của bạn đang trống");
        request.getRequestDispatcher("/cart.jsp").forward(request, response);
    }

    private static void disableBrowserCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }

    /** User đăng nhập bằng tài khoản demo (session username). */
    private static boolean isDemoSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object u = session.getAttribute("username");
        if (u == null) {
            return false;
        }
        return "demo".equalsIgnoreCase(String.valueOf(u).trim());
    }

    /** JSON giỏ (checkout): chỉ tự nạp mẫu cho phiên đăng nhập demo. */
    private void maybeRefillSampleCartForApi(HttpServletRequest request, int userId) {
        if (isDemoSessionUser(request)) {
            cartDAO.refillSampleCartIfEmpty(userId);
        }
    }

    /** Trang tab Giỏ: phiên demo tự nạp mẫu khi giỏ trống (không còn tham số URL). */
    private void maybeRefillSampleCartForPage(HttpServletRequest request, int userId) {
        if (isDemoSessionUser(request)) {
            cartDAO.refillSampleCartIfEmpty(userId);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        disableBrowserCache(response);
        response.setContentType("application/json;charset=UTF-8");

        int userId = AuthHelper.getUserId(request);
        if (userId <= 0) {
            response.setStatus(401);
            response.getWriter().print("{\"error\":\"Vui lòng đăng nhập để thao tác giỏ hàng\"}");
            return;
        }

        String action = request.getParameter("action");
        if ("add".equals(action)) {
            handleAdd(request, response, userId);
            return;
        }
        if ("update".equals(action)) {
            handleUpdate(request, response);
            return;
        }
        if ("delete".equals(action)) {
            handleDelete(request, response);
            return;
        }
        if ("reorder".equals(action)) {
            handleReorder(request, response, userId);
            return;
        }
        if ("review".equals(action)) {
            handleReview(request, response, userId);
            return;
        }

        response.setStatus(400);
        response.getWriter().print("{\"error\":\"Action không hợp lệ\"}");
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        int foodId = parseInt(request.getParameter("foodId"), 0);
        int quantity = parseInt(request.getParameter("quantity"), 1);

        if (foodId <= 0 || quantity <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Thông tin thêm giỏ hàng không hợp lệ\"}");
            return;
        }

        if (!cartDAO.checkStock(foodId, quantity)) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Hết hàng: mỗi lần chỉ đặt tối đa 10 món\"}");
            return;
        }

        boolean ok = cartDAO.addToCart(userId, foodId, quantity);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể thêm món vào giỏ hàng\"}");
            return;
        }

        response.getWriter().print("{\"message\":\"Đã thêm món vào giỏ hàng\"}");
        notificationDAO.createNotification(userId, "Thêm vào giỏ thành công",
                "Bạn vừa thêm món #" + foodId + " vào giỏ hàng.", "SYSTEM");
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = AuthHelper.getUserId(request);
        int cartItemId = parseInt(request.getParameter("cartItemId"), 0);
        int quantity = parseInt(request.getParameter("quantity"), 0);

        if (cartItemId <= 0 || quantity <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Số lượng cập nhật không hợp lệ\"}");
            return;
        }

        boolean ok = cartDAO.updateQuantity(cartItemId, quantity, userId);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể cập nhật số lượng\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Cập nhật số lượng thành công\"}");
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int userId = AuthHelper.getUserId(request);
        int cartItemId = parseInt(request.getParameter("cartItemId"), 0);
        if (cartItemId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Món trong giỏ không hợp lệ\"}");
            return;
        }

        boolean ok = cartDAO.removeFromCart(cartItemId, userId);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể xóa món khỏi giỏ\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Đã xóa món khỏi giỏ hàng\"}");
        notificationDAO.createNotification(userId, "Đã xóa món khỏi giỏ",
                "Một món ăn đã được xóa khỏi giỏ hàng của bạn.", "SYSTEM");
    }

    private void handleReorder(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), 0);
        if (orderId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Mã đơn hàng không hợp lệ\"}");
            return;
        }
        boolean ok = orderDAO.reorder(orderId, userId);
        if (!ok) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Không thể mua lại đơn này\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Đã thêm lại món vào giỏ hàng\"}");
        notificationDAO.createNotification(userId, "Mua lại thành công",
                "Đã thêm lại món từ đơn #" + orderId + " vào giỏ hàng.", "ORDER");
    }

    private void handleReview(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), 0);
        int foodId = parseInt(request.getParameter("foodId"), 0);
        int rating = parseInt(request.getParameter("rating"), 0);
        String comment = request.getParameter("comment");

        if (orderId <= 0 || foodId <= 0 || rating < 1 || rating > 5) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Thông tin đánh giá không hợp lệ\"}");
            return;
        }

        boolean ok = orderDAO.addReview(orderId, userId, foodId, rating, comment == null ? "" : comment.trim());
        if (!ok) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Chỉ có thể đánh giá đơn đã hoàn thành\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Đã gửi đánh giá thành công\"}");
    }

    private List<Order> getOrdersByStatuses(int userId, int... statuses) {
        List<Order> all = new ArrayList<>();
        for (int status : statuses) {
            all.addAll(orderDAO.getOrdersByStatus(userId, status));
        }
        return all;
    }

    private double calculateGrandTotal(List<CartItem> cartItems) {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    private String trackingOrdersToJson(List<Order> orders) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Order order : orders) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(order.getId()).append(",")
              .append("\"status\":").append(order.getStatus())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String cartItemsToJson(List<CartItem> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (CartItem item : items) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(item.getId()).append(",")
              .append("\"food_id\":").append(item.getFoodId()).append(",")
              .append("\"food_name\":\"").append(escapeJson(item.getFoodName())).append("\",")
              .append("\"price\":").append(item.getPrice()).append(",")
              .append("\"quantity\":").append(item.getQuantity()).append(",")
              .append("\"image\":\"").append(escapeJson(item.getImage())).append("\"")
              .append("}");
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
}
