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

        int userId = getCurrentUserId(request);

        String view = request.getParameter("view");
        if (view == null || view.trim().isEmpty()) {
            view = "cart";
        }

        if ("tracking-data".equals(view)) {
            response.setContentType("application/json;charset=UTF-8");
            orderDAO.updateOrderStatusForTrackingTest(userId);
            List<Order> delivering = getOrdersByStatuses(userId, 1, 2);
            response.getWriter().print(trackingOrdersToJson(delivering));
            return;
        }

        List<CartItem> cartItems = cartDAO.getCartByUserId(userId);
        List<Order> deliveringOrders = getOrdersByStatuses(userId, 1, 2);
        List<Order> historyOrders = getOrdersByStatuses(userId, 3, 4);

        Map<Integer, List<OrderItem>> orderItemsMap = new HashMap<>();
        for (Order order : deliveringOrders) {
            orderItemsMap.put(order.getId(), orderDAO.getOrderItems(order.getId()));
        }
        for (Order order : historyOrders) {
            orderItemsMap.put(order.getId(), orderDAO.getOrderItems(order.getId()));
        }

        // Fake dữ liệu demo khi chưa có đơn thực tế để trình diễn giao diện
        if (deliveringOrders.isEmpty()) {
            List<Order> fakeDelivering = buildFakeDeliveringOrders();
            deliveringOrders.addAll(fakeDelivering);
            for (Order order : fakeDelivering) {
                orderItemsMap.put(order.getId(), buildFakeOrderItems(order.getId()));
            }
        }

        if (historyOrders.isEmpty()) {
            List<Order> fakeHistory = buildFakeHistoryOrders();
            historyOrders.addAll(fakeHistory);
            for (Order order : fakeHistory) {
                orderItemsMap.put(order.getId(), buildFakeOrderItems(order.getId()));
            }
        }

        double grandTotal = calculateGrandTotal(cartItems);

        if ("cart-data".equals(view)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"items\":" + cartItemsToJson(cartItems) + ",\"grand_total\":" + grandTotal + "}");
            return;
        }

        request.setAttribute("cartItems", cartItems);
        request.setAttribute("grandTotal", grandTotal);
        request.setAttribute("deliveringOrders", deliveringOrders);
        request.setAttribute("historyOrders", historyOrders);
        request.setAttribute("orderItemsMap", orderItemsMap);
        request.setAttribute("activeView", view);
        request.setAttribute("emptyMessage", "Giỏ hàng của bạn đang trống");
        request.getRequestDispatcher("/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        int userId = getCurrentUserId(request);
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
        int cartItemId = parseInt(request.getParameter("cartItemId"), 0);
        int quantity = parseInt(request.getParameter("quantity"), 0);

        if (cartItemId <= 0 || quantity <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Số lượng cập nhật không hợp lệ\"}");
            return;
        }

        boolean ok = cartDAO.updateQuantity(cartItemId, quantity);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể cập nhật số lượng\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Cập nhật số lượng thành công\"}");
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int cartItemId = parseInt(request.getParameter("cartItemId"), 0);
        if (cartItemId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Món trong giỏ không hợp lệ\"}");
            return;
        }

        boolean ok = cartDAO.removeFromFile(cartItemId);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể xóa món khỏi giỏ\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Đã xóa món khỏi giỏ hàng\"}");
        notificationDAO.createNotification(getCurrentUserId(request), "Đã xóa món khỏi giỏ",
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

    private List<Order> buildFakeDeliveringOrders() {
        List<Order> list = new ArrayList<>();
        list.add(new Order(9001, 1, 148000, 1, 1, "2026-05-08 00:20:00"));
        list.add(new Order(9002, 1, 210000, 2, 1, "2026-05-08 00:18:00"));
        return list;
    }

    private List<Order> buildFakeHistoryOrders() {
        List<Order> list = new ArrayList<>();
        list.add(new Order(8998, 1, 175000, 3, 1, "2026-05-07 19:30:00"));
        list.add(new Order(8997, 1, 92000, 4, 1, "2026-05-07 17:10:00"));
        return list;
    }

    private List<OrderItem> buildFakeOrderItems(int orderId) {
        List<OrderItem> list = new ArrayList<>();
        list.add(new OrderItem(orderId, 1, "Trà sữa trân châu đen", 2, 35000, "images/trasua-placeholder.svg"));
        list.add(new OrderItem(orderId, 7, "Bánh mì pate thịt", 1, 25000, "images/banhmi-placeholder.svg"));
        return list;
    }

    private int getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object[] keys = {
                session.getAttribute("userId"),
                session.getAttribute("user_id"),
                session.getAttribute("uid")
            };
            for (Object key : keys) {
                int id = parseObjectToInt(key);
                if (id > 0) return id;
            }
        }
        int userIdFromParam = parseInt(request.getParameter("userId"), 0);
        if (userIdFromParam > 0) return userIdFromParam;
        // Fake user để test end-to-end khi chưa có đăng nhập
        return 1;
    }

    private int parseObjectToInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
