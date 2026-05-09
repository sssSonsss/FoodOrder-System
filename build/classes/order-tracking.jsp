<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%@ page import="model.OrderStatusLog" %>
<%!
    private String statusLabel(int status) {
        switch (status) {
            case 0: return "Chờ xác nhận";
            case 1: return "Đang chuẩn bị";
            case 2: return "Đang giao";
            case 3: return "Hoàn thành";
            case 4: return "Đã hủy";
            default: return "Cập nhật";
        }
    }
%>
<%
    Order order = (Order) request.getAttribute("order");
    List<OrderStatusLog> logs = (List<OrderStatusLog>) request.getAttribute("statusLogs");
    String trackingError = (String) request.getAttribute("trackingError");
    int st = order != null ? order.getStatus() : -1;
    String[] stepLabels = {"Chờ xác nhận", "Đang chuẩn bị", "Đang giao", "Hoàn thành"};
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Theo dõi đơn hàng - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/order.css">
</head>
<body>
<nav class="navbar">
    <a class="navbar-brand" href="index.html"><div class="logo-icon">🍜</div>FoodOrder</a>
    <div class="navbar-actions"><a class="btn btn-ghost" href="CartServlet?view=tracking">← Đơn hàng của tôi</a></div>
</nav>

<section class="order-page">
    <% if (order == null) { %>
        <div class="order-card">
            <p><%= trackingError != null ? trackingError : "Không tìm thấy đơn để theo dõi." %></p>
            <p style="margin-top:12px;"><a class="btn btn-outline" href="CartServlet?view=tracking">Xem đơn của tôi</a></p>
        </div>
    <% } else { %>
        <div class="order-card">
            <h2>🚚 Theo dõi đơn #<%= order.getId() %></h2>
            <p>Trạng thái hiện tại:
                <span class="status-chip status-<%= order.getStatus() %>"><%= statusLabel(order.getStatus()) %></span>
            </p>
        </div>

        <% if (st == 4) { %>
            <div class="order-card">
                <h2>📍 Trạng thái</h2>
                <p>Đơn hàng đã bị hủy. Nếu cần hỗ trợ, vui lòng liên hệ hotline.</p>
            </div>
        <% } else { %>
        <div class="order-card">
            <h2>📍 Tiến trình giao hàng</h2>
            <div class="timeline">
                <%
                    for (int i = 0; i < stepLabels.length; i++) {
                        String cls;
                        if (st >= 3) {
                            cls = "done";
                        } else if (i < st) {
                            cls = "done";
                        } else if (i == st) {
                            cls = "current";
                        } else {
                            cls = "";
                        }
                %>
                    <div class="timeline-step <%= cls %>">
                        <strong><%= stepLabels[i] %></strong>
                    </div>
                <% } %>
            </div>
        </div>
        <% } %>

        <div class="order-card">
            <h2>🕒 Nhật ký trạng thái</h2>
            <% if (logs == null || logs.isEmpty()) { %>
                <p>Chưa có nhật ký cập nhật.</p>
            <% } else { for (OrderStatusLog log : logs) { %>
                <p><strong><%= log.getCreatedAt() %></strong> — <%= statusLabel(log.getStatus()) %> — <%= log.getNote() != null ? log.getNote() : "" %></p>
            <% }} %>
        </div>
    <% } %>
</section>
<script src="js/notification-widget.js"></script>
</body>
</html>
