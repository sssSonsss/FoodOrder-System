package controller;

import dao.OrderDAO;
import model.Order;
import model.OrderItem;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/OrderServlet")
public class OrderServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        int userId = getCurrentUserId(request);

        if (userId <= 0) {
            response.setStatus(401);
            response.getWriter().print("{\"error\":\"Vui lòng đăng nhập để thao tác đơn hàng\"}");
            return;
        }

        if ("detail".equals(action)) {
            handleOrderDetail(request, response, userId);
            return;
        }

        if ("my-page".equals(action)) {
            String status = request.getParameter("status");
            List<Order> orders = orderDAO.getOrdersByUser(userId, status);
            request.setAttribute("orders", orders);
            request.setAttribute("activeStatus", status == null ? "" : status);
            request.getRequestDispatcher("/my-orders.jsp").forward(request, response);
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        List<Order> orders = orderDAO.getOrdersByUser(userId, request.getParameter("status"));
        response.getWriter().print(orderListToJson(orders));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String action = request.getParameter("action");
        int userId = getCurrentUserId(request);

        if (userId <= 0) {
            response.setStatus(401);
            response.getWriter().print("{\"error\":\"Vui lòng đăng nhập để thao tác đơn hàng\"}");
            return;
        }

        if ("create".equals(action)) {
            handleCreateOrder(request, response, userId);
            return;
        }

        if ("cancel".equals(action)) {
            handleCancelOrder(request, response, userId);
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

    private void handleCreateOrder(HttpServletRequest request, HttpServletResponse response, int userId)
            throws ServletException, IOException {

        int addressId = parseInt(request.getParameter("addressId"), 0);
        if (addressId <= 0) {
            request.setAttribute("errorMessage", "Địa chỉ giao hàng không hợp lệ.");
            request.getRequestDispatcher("/order-success.jsp").forward(request, response);
            return;
        }

        try {
            int orderId = orderDAO.createOrderFromCart(userId, addressId);
            if (orderId <= 0) {
                request.setAttribute("errorMessage", "Giỏ hàng đang trống, không thể tạo đơn.");
            } else {
                request.setAttribute("successMessage", "Đặt hàng thành công. Cảm ơn bạn đã tin tưởng FoodOrder.");
                request.setAttribute("orderId", orderId);
            }
            request.getRequestDispatcher("/order-success.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Có lỗi xảy ra khi tạo đơn hàng. Vui lòng thử lại.");
            request.getRequestDispatcher("/order-success.jsp").forward(request, response);
        }
    }

    private void handleOrderDetail(HttpServletRequest request, HttpServletResponse response, int userId)
            throws ServletException, IOException {

        int orderId = parseInt(request.getParameter("orderId"), 0);
        if (orderId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Mã đơn hàng không hợp lệ\"}");
            return;
        }

        Order order = orderDAO.getOrderById(orderId, userId);
        if (order == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Không tìm thấy đơn hàng\"}");
            return;
        }

        List<OrderItem> items = orderDAO.getOrderItems(orderId);

        if ("page".equals(request.getParameter("view"))) {
            request.setAttribute("order", order);
            request.setAttribute("items", items);
            request.getRequestDispatcher("/order-detail.jsp").forward(request, response);
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print("{\"order\":" + orderToJson(order) + ",\"items\":" + orderItemsToJson(items) + "}");
    }

    private void handleCancelOrder(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {

        int orderId = parseInt(request.getParameter("orderId"), 0);
        response.setContentType("application/json;charset=UTF-8");
        if (orderId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Mã đơn hàng không hợp lệ\"}");
            return;
        }

        boolean ok = orderDAO.cancelOrder(orderId, userId);
        if (!ok) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Chỉ được hủy khi đơn đang chờ xác nhận hoặc đang chuẩn bị\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Hủy đơn hàng thành công\"}");
    }

    private void handleReorder(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), 0);
        response.setContentType("application/json;charset=UTF-8");
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
        response.getWriter().print("{\"message\":\"Đã thêm lại món ăn vào giỏ hàng\"}");
    }

    private void handleReview(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        int orderId = parseInt(request.getParameter("orderId"), 0);
        int foodId = parseInt(request.getParameter("foodId"), 0);
        int rating = parseInt(request.getParameter("rating"), 0);
        String comment = request.getParameter("comment");

        response.setContentType("application/json;charset=UTF-8");
        if (orderId <= 0 || foodId <= 0 || rating < 1 || rating > 5) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Thông tin đánh giá không hợp lệ\"}");
            return;
        }

        boolean ok = orderDAO.addReview(orderId, userId, foodId, rating, comment == null ? "" : comment.trim());
        if (!ok) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Chỉ có thể đánh giá đơn đã giao thành công\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Gửi đánh giá thành công\"}");
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

    private String orderListToJson(List<Order> orders) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Order order : orders) {
            if (!first) sb.append(",");
            first = false;
            sb.append(orderToJson(order));
        }
        sb.append("]");
        return sb.toString();
    }

    private String orderItemsToJson(List<OrderItem> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (OrderItem item : items) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"order_id\":").append(item.getOrderId()).append(",")
              .append("\"food_id\":").append(item.getFoodId()).append(",")
              .append("\"food_name\":\"").append(escapeJson(item.getFoodName())).append("\",")
              .append("\"quantity\":").append(item.getQuantity()).append(",")
              .append("\"price\":").append(item.getPrice()).append(",")
              .append("\"image_url\":\"").append(escapeJson(item.getImageUrl())).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String orderToJson(Order order) {
        return "{"
            + "\"id\":" + order.getId() + ","
            + "\"user_id\":" + order.getUserId() + ","
            + "\"total_price\":" + order.getTotalPrice() + ","
            + "\"status\":\"" + escapeJson(order.getStatus()) + "\","
            + "\"address_id\":" + order.getAddressId() + ","
            + "\"created_at\":\"" + escapeJson(order.getCreatedAt()) + "\""
            + "}";
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
