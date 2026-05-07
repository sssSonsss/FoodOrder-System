<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%!
    private String statusLabel(String status) {
        if ("CHO_XAC_NHAN".equals(status)) return "Chờ xác nhận";
        if ("DANG_CHUAN_BI".equals(status)) return "Đang chuẩn bị";
        if ("SHIPPER_DA_LAY".equals(status)) return "Shipper đã lấy hàng";
        if ("DANG_GIAO".equals(status)) return "Đang giao";
        if ("DA_GIAO_THANH_CONG".equals(status)) return "Đã giao thành công";
        if ("DA_HUY".equals(status)) return "Đã hủy";
        return status == null ? "" : status;
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
    <div class="navbar-actions"><a class="btn btn-ghost" href="menu.html">← Thực đơn</a></div>
</nav>

<section class="order-page">
    <h2 style="margin-bottom:14px;">📦 Đơn hàng của tôi</h2>
    <div class="order-tabs">
        <a class="order-tab <%= "".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page">Tất cả</a>
        <a class="order-tab <%= "CHO_XAC_NHAN".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&status=CHO_XAC_NHAN">Chờ xác nhận</a>
        <a class="order-tab <%= "DANG_CHUAN_BI".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&status=DANG_CHUAN_BI">Đang chuẩn bị</a>
        <a class="order-tab <%= "DANG_GIAO".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&status=DANG_GIAO">Đang giao</a>
        <a class="order-tab <%= "DA_GIAO_THANH_CONG".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&status=DA_GIAO_THANH_CONG">Thành công</a>
        <a class="order-tab <%= "DA_HUY".equals(activeStatus) ? "active" : "" %>" href="OrderServlet?action=my-page&status=DA_HUY">Đã hủy</a>
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
                        <span class="status-chip status-<%= order.getStatus().toLowerCase() %>"><%= statusLabel(order.getStatus()) %></span>
                        <p style="font-weight:700;color:var(--primary);margin-top:4px;"><%= String.format("%,.0fđ", order.getTotalPrice()) %></p>
                    </div>
                </div>
                <div class="order-actions">
                    <a class="btn btn-outline" href="OrderServlet?action=detail&view=page&orderId=<%= order.getId() %>">Xem chi tiết</a>
                    <a class="btn btn-primary" href="OrderTrackingServlet?orderId=<%= order.getId() %>">Theo dõi</a>
                    <button class="btn btn-ghost" onclick="reorder(<%= order.getId() %>)">Mua lại</button>
                    <% if ("CHO_XAC_NHAN".equals(order.getStatus()) || "DANG_CHUAN_BI".equals(order.getStatus())) { %>
                        <button class="btn btn-ghost" style="color:var(--danger);" onclick="cancelOrder(<%= order.getId() %>)">Hủy đơn</button>
                    <% } %>
                </div>
            </div>
        <% } %>
    <% } %>
</section>
<script src="js/order.js"></script>
</body>
</html>
