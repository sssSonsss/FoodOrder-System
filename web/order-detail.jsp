<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%@ page import="model.OrderItem" %>
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
    Order order = (Order) request.getAttribute("order");
    List<OrderItem> items = (List<OrderItem>) request.getAttribute("items");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi tiết đơn hàng - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/order.css">
</head>
<body>
<nav class="navbar">
    <a class="navbar-brand" href="index.html"><div class="logo-icon">🍜</div>FoodOrder</a>
    <div class="navbar-actions"><a class="btn btn-ghost" href="CartServlet?view=tracking">← Đơn của tôi</a></div>
</nav>

<section class="order-page">
    <% if (order == null) { %>
        <div class="order-card"><p>Không tìm thấy thông tin đơn hàng.</p></div>
    <% } else { %>
        <div class="order-card">
            <div class="order-head">
                <div>
                    <h2>🧾 Đơn hàng #<%= order.getId() %></h2>
                    <p>Thời gian đặt: <%= order.getCreatedAt() %></p>
                </div>
                <div>
                    <span class="status-chip status-<%= order.getStatus() %>"><%= statusLabel(order.getStatus()) %></span>
                    <p style="font-weight:700;color:var(--primary);margin-top:6px;"><%= String.format("%,.0fđ", order.getTotalPrice()) %></p>
                </div>
            </div>
            <div class="order-actions" style="margin-top:14px;">
                <a class="btn btn-primary" href="OrderTrackingServlet?orderId=<%= order.getId() %>">Theo dõi đơn này</a>
            </div>
        </div>

        <div class="order-card order-items">
            <h2>🍱 Danh sách món</h2>
            <% if (items != null) for (OrderItem item : items) { %>
                <div class="order-flow-item">
                    <img class="order-flow-item__img"
                         src="<%= item.getImageUrl() == null ? "https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg" : item.getImageUrl() %>"
                         alt="Món ăn"
                         onerror="this.onerror=null;this.src='https://upload.wikimedia.org/wikipedia/commons/5/5f/B%C3%A1nh_m%C3%AC_th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng_in_Saigon.jpg'">
                    <div class="order-flow-item__body">
                        <div class="order-flow-item__name"><%= item.getFoodName() %></div>
                        <p class="order-flow-item__meta">Số lượng: <%= item.getQuantity() %> · <%= String.format("%,.0fđ", item.getPrice()) %> / món</p>
                        <div class="order-flow-item__price"><%= String.format("%,.0fđ", item.getPrice() * item.getQuantity()) %></div>
                    </div>
                </div>
                <% if (order.getStatus() == 3) { %>
                    <form class="form-review" onsubmit="submitReview(event, <%= order.getId() %>, <%= item.getFoodId() %>)">
                        <label>Đánh giá món: <strong><%= item.getFoodName() %></strong></label>
                        <select name="rating" required>
                            <option value="">Chọn số sao</option>
                            <option value="5">5 sao - Tuyệt vời</option>
                            <option value="4">4 sao - Rất ngon</option>
                            <option value="3">3 sao - Ổn</option>
                            <option value="2">2 sao - Chưa tốt</option>
                            <option value="1">1 sao - Cần cải thiện</option>
                        </select>
                        <textarea name="comment" rows="2" placeholder="Nhập nhận xét của bạn..."></textarea>
                        <button class="btn btn-outline" type="submit">Gửi đánh giá</button>
                    </form>
                <% } %>
            <% } %>
        </div>

        <div class="order-card">
            <h2>🛵 Thông tin shipper giả lập</h2>
            <p>Shipper: Nguyễn Văn Nhanh</p>
            <p>SĐT: 09xx xxx 888</p>
            <p>Biển số xe: 59X2-123.45</p>
        </div>
    <% } %>
</section>
<script src="js/order.js"></script>
<script src="js/notification-widget.js?v=3"></script>
</body>
</html>
