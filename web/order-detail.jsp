<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Order" %>
<%@ page import="model.OrderItem" %>
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
    <div class="navbar-actions"><a class="btn btn-ghost" href="OrderServlet?action=my-page">← Quay lại đơn của tôi</a></div>
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
                    <span class="status-chip status-<%= order.getStatus().toLowerCase() %>"><%= statusLabel(order.getStatus()) %></span>
                    <p style="font-weight:700;color:var(--primary);margin-top:6px;"><%= String.format("%,.0fđ", order.getTotalPrice()) %></p>
                </div>
            </div>
        </div>

        <div class="order-card order-items">
            <h2>🍱 Danh sách món</h2>
            <% if (items != null) for (OrderItem item : items) { %>
                <div class="item-row">
                    <img class="thumb" src="<%= item.getImageUrl() == null ? "" : item.getImageUrl() %>" alt="Món ăn">
                    <div>
                        <strong><%= item.getFoodName() %></strong>
                        <p>Số lượng: <%= item.getQuantity() %></p>
                    </div>
                    <div style="font-weight:700;color:var(--primary);">
                        <%= String.format("%,.0fđ", item.getPrice() * item.getQuantity()) %>
                    </div>
                </div>
                <% if ("DA_GIAO_THANH_CONG".equals(order.getStatus())) { %>
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
</body>
</html>
