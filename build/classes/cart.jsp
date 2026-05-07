<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.CartItem" %>
<%
    List<CartItem> cartItems = (List<CartItem>) request.getAttribute("cartItems");
    String emptyMessage = (String) request.getAttribute("emptyMessage");
    Double grandTotal = (Double) request.getAttribute("grandTotal");
    if (grandTotal == null) grandTotal = 0.0;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Giỏ hàng của tôi - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .cart-wrap { max-width: 1100px; margin: 0 auto; padding: 28px 5% 60px; }
        .cart-card { background: #fff; border: 1px solid var(--border); border-radius: 14px; box-shadow: var(--shadow-sm); overflow: hidden; }
        .cart-table { width: 100%; border-collapse: collapse; }
        .cart-table th, .cart-table td { padding: 14px 12px; border-bottom: 1px solid var(--border); text-align: left; }
        .cart-thumb { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; background: var(--border); }
        .qty-wrap { display: inline-flex; align-items: center; gap: 8px; }
        .qty-wrap input { width: 76px; padding: 6px 8px; border: 1px solid var(--border); border-radius: 8px; }
        .cart-footer { padding: 18px; display: flex; justify-content: space-between; gap: 10px; align-items: center; flex-wrap: wrap; }
        .cart-total { font-size: 1.1rem; font-weight: 700; color: var(--primary); }
        @media (max-width: 860px) {
            .cart-table thead { display: none; }
            .cart-table tr { display: block; border-bottom: 1px solid var(--border); }
            .cart-table td { display: block; border-bottom: none; }
        }
    </style>
</head>
<body>
<nav class="navbar">
    <a class="navbar-brand" href="index.html">
        <div class="logo-icon">🍜</div>
        FoodOrder
    </a>
    <ul class="navbar-nav">
        <li><a href="index.html">Trang chủ</a></li>
        <li><a href="menu.html">Thực đơn</a></li>
        <li><a href="CartServlet" class="active">Giỏ hàng</a></li>
    </ul>
    <div class="navbar-actions">
        <a href="menu.html" class="btn btn-ghost">← Tiếp tục mua sắm</a>
    </div>
</nav>

<section class="cart-wrap">
    <h2 style="margin-bottom: 14px;">🛒 Giỏ hàng của bạn</h2>

    <% if (cartItems == null || cartItems.isEmpty()) { %>
        <div class="cart-card" style="padding: 22px;">
            <p style="margin-bottom: 12px;"><%= emptyMessage == null ? "Giỏ hàng của bạn đang trống" : emptyMessage %></p>
            <a class="btn btn-primary" href="menu.html">Quay lại mua sắm</a>
        </div>
    <% } else { %>
        <div class="cart-card">
            <table class="cart-table">
                <thead>
                <tr>
                    <th>Hình ảnh</th>
                    <th>Món ăn</th>
                    <th>Đơn giá</th>
                    <th>Số lượng</th>
                    <th>Thành tiền</th>
                    <th>Thao tác</th>
                </tr>
                </thead>
                <tbody id="cart-tbody">
                <% for (CartItem item : cartItems) { %>
                    <tr id="row-<%= item.getId() %>">
                        <td><img class="cart-thumb" src="<%= item.getImage() == null ? "" : item.getImage() %>" alt="Món ăn"></td>
                        <td>
                            <strong><%= item.getFoodName() %></strong>
                            <div style="color:var(--text-light);font-size:.86rem;">Mã món: #<%= item.getFoodId() %></div>
                        </td>
                        <td><%= String.format("%,.0fđ", item.getPrice()) %></td>
                        <td>
                            <div class="qty-wrap">
                                <input type="number" min="1" value="<%= item.getQuantity() %>"
                                       onchange="updateQuantity(<%= item.getId() %>, this.value)">
                            </div>
                        </td>
                        <td id="subtotal-<%= item.getId() %>"><%= String.format("%,.0fđ", item.getPrice() * item.getQuantity()) %></td>
                        <td>
                            <button class="btn btn-ghost" style="color:var(--danger);" onclick="removeItem(<%= item.getId() %>)">Xóa</button>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>

            <div class="cart-footer">
                <div class="cart-total">Tổng cộng: <span id="grand-total"><%= String.format("%,.0fđ", grandTotal) %></span></div>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <a class="btn btn-outline" href="menu.html">Thêm món</a>
                    <a class="btn btn-primary" href="checkout.html">Tiến hành đặt hàng</a>
                </div>
            </div>
        </div>
    <% } %>
</section>

<script>
function parseMoney(str) {
    return Number(String(str).replace(/[^\d]/g, "")) || 0;
}

function formatMoney(v) {
    return Number(v).toLocaleString("vi-VN") + "đ";
}

function updateGrandTotal() {
    const subtotalEls = document.querySelectorAll("[id^='subtotal-']");
    let total = 0;
    subtotalEls.forEach(el => { total += parseMoney(el.textContent); });
    const totalEl = document.getElementById("grand-total");
    if (totalEl) totalEl.textContent = formatMoney(total);
}

function updateQuantity(cartItemId, quantity) {
    const qty = Number(quantity);
    if (!qty || qty < 1) {
        alert("Số lượng phải lớn hơn 0.");
        window.location.reload();
        return;
    }

    fetch("CartServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            action: "update",
            cartItemId: String(cartItemId),
            quantity: String(qty)
        }).toString()
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            alert(data.error);
            return;
        }

        const row = document.getElementById("row-" + cartItemId);
        if (!row) return;
        const unitPriceText = row.children[2].textContent;
        const subtotalEl = document.getElementById("subtotal-" + cartItemId);
        subtotalEl.textContent = formatMoney(parseMoney(unitPriceText) * qty);
        updateGrandTotal();
    })
    .catch(() => alert("Có lỗi khi cập nhật số lượng."));
}

function removeItem(cartItemId) {
    if (!confirm("Bạn chắc chắn muốn xóa món này khỏi giỏ?")) return;

    fetch("CartServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            action: "delete",
            cartItemId: String(cartItemId)
        }).toString()
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            alert(data.error);
            return;
        }
        const row = document.getElementById("row-" + cartItemId);
        if (row) row.remove();
        updateGrandTotal();
        if (!document.querySelector("#cart-tbody tr")) {
            window.location.reload();
        }
    })
    .catch(() => alert("Có lỗi khi xóa món khỏi giỏ."));
}
</script>
</body>
</html>
