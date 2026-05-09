package controller;

import dao.OrderDAO;
import model.Order;
import model.OrderStatusLog;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.AuthHelper;

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

        int userId = AuthHelper.getUserId(request);
        int orderId = parseInt(request.getParameter("orderId"), 0);

        if (orderId <= 0) {
            request.setAttribute("order", null);
            request.setAttribute("trackingError", "Mã đơn hàng không hợp lệ.");
            request.getRequestDispatcher("/order-tracking.jsp").forward(request, response);
            return;
        }
        if (userId <= 0) {
            AuthHelper.redirectToLogin(request, response);
            return;
        }

        Order order = orderDAO.getOrderById(orderId, userId);
        if (order == null) {
            request.setAttribute("order", null);
            request.setAttribute("trackingError", "Không tìm thấy đơn hàng hoặc đơn không thuộc tài khoản của bạn.");
            request.getRequestDispatcher("/order-tracking.jsp").forward(request, response);
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

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
