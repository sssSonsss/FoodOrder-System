package controller;

import dao.OrderDAO;
import model.Order;
import model.OrderStatusLog;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/OrderTrackingServlet")
public class OrderTrackingServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int userId = getCurrentUserId(request);
        int orderId = parseInt(request.getParameter("orderId"), 0);

        if (userId <= 0 || orderId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Thông tin tracking không hợp lệ\"}");
            return;
        }

        Order order = orderDAO.getOrderById(orderId, userId);
        if (order == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Không tìm thấy đơn hàng\"}");
            return;
        }

        int fakeRealtimeStatus = calculateStatusByElapsedTime(order.getCreatedAt(), order.getStatus());
        if (order.getStatus() != fakeRealtimeStatus && order.getStatus() != 4) {
            orderDAO.updateOrderStatus(orderId, fakeRealtimeStatus);
            orderDAO.appendStatusLog(orderId, fakeRealtimeStatus, buildTrackingNote(fakeRealtimeStatus));
            order.setStatus(fakeRealtimeStatus);
        }

        List<OrderStatusLog> logs = orderDAO.getStatusLogs(orderId);
        request.setAttribute("order", order);
        request.setAttribute("statusLogs", logs);
        request.getRequestDispatcher("/order-tracking.jsp").forward(request, response);
    }

    private int calculateStatusByElapsedTime(String createdAt, int currentStatus) {
        if (currentStatus == 4 || currentStatus == 3) {
            return currentStatus;
        }

        try {
            LocalDateTime createdTime = LocalDateTime.parse(createdAt, DATE_TIME_FORMATTER);
            long minutes = Duration.between(createdTime, LocalDateTime.now()).toMinutes();

            if (minutes < 2) return 0;
            if (minutes < 5) return 1;
            if (minutes < 12) return 2;
            return 3;
        } catch (Exception ignored) {
            return currentStatus;
        }
    }

    private String buildTrackingNote(int status) {
        switch (status) {
            case 1:
                return "Quán đã xác nhận và đang chuẩn bị món";
            case 2:
                return "Đơn hàng đang trên đường giao đến bạn";
            case 3:
                return "Giao hàng thành công, chúc bạn ngon miệng";
            default:
                return "Đơn hàng đang chờ xác nhận";
        }
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
