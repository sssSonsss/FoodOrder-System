<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt hàng thành công - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/order.css">
</head>
<body>
<nav class="navbar">
    <a class="navbar-brand" href="index.html"><div class="logo-icon">🍜</div>FoodOrder</a>
    <div class="navbar-actions"><a class="btn btn-ghost" href="menu.html">← Tiếp tục mua món</a></div>
</nav>

<section class="order-page">
    <div class="order-card" style="text-align:center;">
        <%
            String error = (String) request.getAttribute("errorMessage");
            String success = (String) request.getAttribute("successMessage");
            Object orderId = request.getAttribute("orderId");
        %>
        <% if (error != null) { %>
            <h2>⚠️ Đặt đơn chưa thành công</h2>
            <p><%= error %></p>
        <% } else { %>
            <h2>🎉 Đặt đơn thành công</h2>
            <p><%= success == null ? "Đơn hàng đã được ghi nhận." : success %></p>
            <% if (orderId != null) { %>
                <p>Mã đơn hàng của bạn: <strong>#<%= orderId %></strong></p>
                <div class="order-actions" style="justify-content:center;">
                    <a class="btn btn-primary" href="OrderTrackingServlet?orderId=<%= orderId %>">Theo dõi đơn hàng</a>
                    <a class="btn btn-outline" href="OrderServlet?action=my-page">Đơn hàng của tôi</a>
                </div>
            <% } %>
        <% } %>
    </div>
</section>
</body>
</html>
