<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%@ page import="model.OrderStatusLog" %>
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
    Order order = (Order) request.getAttribute("order");
    List<OrderStatusLog> logs = (List<OrderStatusLog>) request.getAttribute("statusLogs");
    String currentStatus = order == null ? "" : order.getStatus();
    String[] timeline = {"CHO_XAC_NHAN", "DANG_CHUAN_BI", "SHIPPER_DA_LAY", "DANG_GIAO", "DA_GIAO_THANH_CONG"};
    String[] labels = {"Đã đặt đơn", "Quán xác nhận", "Shipper đã lấy hàng", "Đang đến", "Thành công"};
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
    <div class="navbar-actions"><a class="btn btn-ghost" href="OrderServlet?action=my-page">← Đơn hàng của tôi</a></div>
</nav>

<section class="order-page">
    <% if (order == null) { %>
        <div class="order-card"><p>Không tìm thấy đơn để theo dõi.</p></div>
    <% } else { %>
        <div class="order-card">
            <h2>🚚 Theo dõi đơn #<%= order.getId() %></h2>
            <p>Trạng thái hiện tại: <span class="status-chip status-<%= currentStatus.toLowerCase() %>"><%= statusLabel(currentStatus) %></span></p>
        </div>

        <div class="order-card">
            <h2>📍 Timeline đơn hàng</h2>
            <div class="timeline">
                <%
                    int currentIndex = -1;
                    for (int i = 0; i < timeline.length; i++) if (timeline[i].equals(currentStatus)) currentIndex = i;
                    for (int i = 0; i < timeline.length; i++) {
                        String cls = i < currentIndex ? "done" : (i == currentIndex ? "current" : "");
                %>
                    <div class="timeline-step <%= cls %>">
                        <strong><%= labels[i] %></strong>
                    </div>
                <% } %>
            </div>
        </div>

        <div class="order-card">
            <h2>🕒 Nhật ký trạng thái</h2>
            <% if (logs == null || logs.isEmpty()) { %>
                <p>Chưa có nhật ký cập nhật.</p>
            <% } else { for (OrderStatusLog log : logs) { %>
                <p><strong><%= log.getCreatedAt() %></strong> - <%= statusLabel(log.getStatus()) %> - <%= log.getNote() %></p>
            <% }} %>
        </div>
    <% } %>
</section>
</body>
</html>
