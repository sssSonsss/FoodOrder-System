<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%!
    private String statusLabel(int status) {
        switch (status) {
            case 0: return "Chờ xác nhận";
            case 1: return "Đang chuẩn bị";
            case 2: return "Đang giao";
            case 3: return "Hoàn thành";
            case 4: return "Đã hủy";
            default: return "Không xác định";
        }
    }
%>
<%
    List<Order> orders = (List<Order>) request.getAttribute("orders");
    String activeStatus = (String) request.getAttribute("activeStatus");
    if (activeStatus == null) activeStatus = "";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đơn hàng của tôi - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/order.css">
</head>
<body>
<nav class="navbar">
    <a class="navbar-brand" href="index.html"><div class="logo-icon">🍜</div>FoodOrder</a>
    <div class="navbar-actions"><a class="btn btn-ghost" href="CartServlet?view=tracking">← Giỏ &amp; đơn</a></div>
</nav>

<section class="order-page">
    <h2 style="margin-bottom:14px;">📦 Đơn hàng của tôi</h2>
    <div class="order-tabs">
        <a class="order-tab <%= "".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page">Tất cả</a>
        <a class="order-tab <%= "0".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&amp;status=0">Chờ xác nhận</a>
        <a class="order-tab <%= "1".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&amp;status=1">Đang chuẩn bị</a>
        <a class="order-tab <%= "2".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&amp;status=2">Đang giao</a>
        <a class="order-tab <%= "3".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&amp;status=3">Hoàn thành</a>
        <a class="order-tab <%= "4".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&amp;status=4">Đã hủy</a>
    </div>

    <% if (orders == null || orders.isEmpty()) { %>
        <div class="order-card"><p>Hiện chưa có đơn hàng nào trong trạng thái này.</p></div>
    <% } else { %>
        <% for (Order order : orders) { %>
            <div class="order-card">
                <div class="order-head">
                    <div>
                        <strong>Đơn #<%= order.getId() %></strong>
                        <p>Đặt lúc: <%= order.getCreatedAt() %></p>
                    </div>
                    <div style="text-align:right;">
                        <span class="status-chip status-<%= order.getStatus() %>"><%= statusLabel(order.getStatus()) %></span>
                        <p style="font-weight:700;color:var(--primary);margin-top:4px;"><%= String.format("%,.0fđ", order.getTotalPrice()) %></p>
                    </div>
                </div>
                <div class="order-actions">
                    <a class="btn btn-outline" href="OrderServlet?action=detail&amp;view=page&amp;orderId=<%= order.getId() %>">Xem chi tiết</a>
                    <a class="btn btn-primary" href="OrderTrackingServlet?orderId=<%= order.getId() %>">Theo dõi</a>
                    <button class="btn btn-ghost" onclick="reorder(<%= order.getId() %>)">Mua lại</button>
                    <% if (order.getStatus() == 0 || order.getStatus() == 1) { %>
                        <button class="btn btn-ghost" style="color:var(--danger);" onclick="cancelOrder(<%= order.getId() %>)">Hủy đơn</button>
                    <% } %>
                </div>
            </div>
        <% } %>
    <% } %>
</section>
<script src="js/order.js"></script>
<script src="js/notification-widget.js?v=3"></script>
</body>
</html>
