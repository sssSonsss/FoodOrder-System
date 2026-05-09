<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="model.CartItem" %>
<%@ page import="model.Order" %>
<%@ page import="model.OrderItem" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Collections" %>
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
    List<CartItem> cartItems = (List<CartItem>) request.getAttribute("cartItems");
    List<Order> deliveringOrders = (List<Order>) request.getAttribute("deliveringOrders");
    List<Order> historyOrders = (List<Order>) request.getAttribute("historyOrders");
    Map<Integer, List<OrderItem>> orderItemsMap = (Map<Integer, List<OrderItem>>) request.getAttribute("orderItemsMap");
    if (orderItemsMap == null) orderItemsMap = Collections.emptyMap();
    String emptyMessage = (String) request.getAttribute("emptyMessage");
    Double grandTotal = (Double) request.getAttribute("grandTotal");
    String activeView = (String) request.getAttribute("activeView");
    if (activeView == null) activeView = "cart";
    if (grandTotal == null) grandTotal = 0.0;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Cache-Control" content="no-store, no-cache, must-revalidate">
    <title>Giỏ hàng của tôi - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .cart-wrap { max-width: 1180px; margin: 0 auto; padding: 28px 5% 60px; }
        .cart-card { background: #fff; border: 1px solid var(--border); border-radius: 14px; box-shadow: var(--shadow-sm); overflow: hidden; margin-top: 12px; }
        .tabs { display:flex; gap:8px; flex-wrap:wrap; margin-bottom: 8px; }
        .tab-btn { padding:10px 14px; border:1px solid var(--border); background:#fff; border-radius:999px; font-weight:700; color:var(--text-mid); }
        .tab-btn.active { background: var(--primary); color:#fff; border-color: var(--primary); box-shadow: var(--shadow-primary); }
        .cart-table { width: 100%; border-collapse: collapse; }
        .cart-table th, .cart-table td { padding: 14px 12px; border-bottom: 1px solid var(--border); text-align: left; }
        .cart-thumb { width: 64px; height: 64px; border-radius: 10px; object-fit: cover; background: var(--border); border:1px solid var(--border); box-shadow: var(--shadow-sm); }
        .qty-wrap { display: inline-flex; align-items: center; gap: 6px; }
        .qty-btn { border: 1px solid var(--border); background: #fff; border-radius: 8px; width: 30px; height: 30px; }
        .qty-wrap input { width: 76px; padding: 6px 8px; border: 1px solid var(--border); border-radius: 8px; }
        .cart-footer { padding: 18px; display: flex; justify-content: space-between; gap: 10px; align-items: center; flex-wrap: wrap; }
        .cart-total { font-size: 1.1rem; font-weight: 700; color: var(--primary); }
        .timeline { position: relative; margin-left: 8px; }
        .timeline::before { content:""; position:absolute; left:13px; top:2px; bottom:2px; width:2px; background:#d5dce4; }
        .timeline-step { position: relative; padding-left: 36px; margin-bottom: 12px; }
        .timeline-step::before { content:""; position:absolute; left:5px; top:4px; width:16px; height:16px; border-radius:50%; background:#c7d2df; }
        .timeline-step.done::before { background:#27AE60; }
        .timeline-step.current::before { background:#FF6B35; }
        .status-chip { padding: 4px 10px; border-radius: 99px; font-size: .8rem; font-weight: 700; }
        .status-0 { background:#eef2f7; color:#4a5568; }
        .status-1 { background:#fff3da; color:#9f6b00; }
        .status-2 { background:#e8f1ff; color:#1a62b8; }
        .status-3 { background:#e5f7ec; color:#1c8f4d; }
        .status-4 { background:#fdeceb; color:#bc3528; }
        .status-demo { background:#f3f4f6; color:#5b6470; margin-left: 6px; }
        .sale-section { margin-top: 22px; }
        .sale-grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(220px,1fr)); gap:14px; margin-top:12px; }
        .sale-card { background:#fff; border:1px solid var(--border); border-radius:12px; box-shadow:var(--shadow-sm); overflow:hidden; cursor:pointer; transition:var(--transition); }
        .sale-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
        .sale-img { width:100%; height:140px; object-fit:cover; background:var(--border); border-bottom:1px solid var(--border); }
        .sale-body { padding:12px; }
        .sale-name { font-weight:700; margin-bottom:4px; }
        .sale-price { color:var(--primary); font-weight:800; }
        .sale-old { color:var(--text-light); text-decoration:line-through; font-size:.86rem; margin-left:6px; }
        .sale-badge { display:inline-block; font-size:.75rem; background:#ffe8de; color:#cc4b17; font-weight:700; padding:3px 8px; border-radius:99px; margin-bottom:6px; }
        .sale-actions { display:flex; gap:8px; flex-wrap:wrap; margin-top:10px; }
        .sale-actions button, .sale-actions a {
            flex:1; min-width:0; padding:8px 10px; border-radius:10px; font-size:.82rem; font-weight:700;
            border:1px solid var(--border); background:#fff; cursor:pointer; font-family:var(--font-main); text-align:center;
            text-decoration:none; color:var(--text-dark);
        }
        .sale-actions .btn-sale-detail:hover { border-color:var(--primary); color:var(--primary); }
        .sale-actions .btn-sale-quick {
            background:linear-gradient(135deg,var(--primary),var(--primary-dark)); color:#fff; border:none;
        }
        .sale-actions .btn-sale-quick:hover { filter:brightness(1.05); }
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
    </ul>
    <div class="navbar-actions">
        <div class="navbar-icons" id="navbar-icons"></div>
    </div>
</nav>

<section class="cart-wrap">
    <h2 style="margin-bottom: 14px;">🛒 Trung tâm mua sắm & theo dõi đơn</h2>

    <div class="tabs">
        <a class="tab-btn <%= "cart".equals(activeView) ? "active" : "" %>" href="CartServlet?view=cart">Giỏ hàng</a>
        <a class="tab-btn <%= "tracking".equals(activeView) ? "active" : "" %>" href="CartServlet?view=tracking">Đơn hàng của tôi</a>
        <a class="tab-btn <%= "history".equals(activeView) ? "active" : "" %>" href="CartServlet?view=history">Lịch sử</a>
    </div>

    <% if ("cart".equals(activeView)) { %>
    <% if (cartItems == null || cartItems.isEmpty()) { %>
        <div class="cart-card p-4">
            <p class="mb-2"><%= emptyMessage == null ? "Giỏ hàng của bạn đang trống" : emptyMessage %></p>
            <p class="mb-3" style="color:var(--text-light);font-size:.92rem;line-height:1.55;">
                Sau khi <strong>đặt hàng thành công</strong>, giỏ được làm trống (đúng quy trình).
                Nếu bạn <strong>đã đăng nhập</strong> và vừa thêm món mà vẫn trống, hệ thống sẽ tự làm mới sau vài giây hoặc thử tải lại trang.
                Phải <strong>đăng nhập</strong> trước khi &quot;Thêm vào giỏ&quot; ở thực đơn thì mới lưu được vào giỏ.
            </p>
            <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center;">
                <a class="btn btn-primary" href="menu.html">Thêm món từ thực đơn</a>
            </div>
        </div>
    <% } else { %>
        <div class="cart-card">
            <table class="cart-table">
                <thead>
                <tr>
                    <th>Chọn</th>
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
                    <tr id="row-<%= item.getId() %>" class="cart-item-row"
                        data-cart-item-id="<%= item.getId() %>"
                        data-unit-price="<%= item.getPrice() %>">
                        <td><input type="checkbox" class="item-check" checked onchange="updateSelectedTotal()"></td>
                        <td><img class="cart-thumb" src="<%= item.getImage() == null ? "images/food-placeholder.svg" : item.getImage() %>" alt="Món ăn" onerror="this.src='images/food-placeholder.svg'"></td>
                        <td>
                            <strong><%= item.getFoodName() %></strong>
                            <div style="color:var(--text-light);font-size:.86rem;">Mã món: #<%= item.getFoodId() %></div>
                        </td>
                        <td><%= String.format("%,.0fđ", item.getPrice()) %></td>
                        <td>
                            <div class="qty-wrap">
                                <button class="qty-btn" onclick="changeQty(<%= item.getId() %>, -1)">-</button>
                                <input type="number" min="1" value="<%= item.getQuantity() %>"
                                       onchange="updateQuantity(<%= item.getId() %>, this.value)">
                                <button class="qty-btn" onclick="changeQty(<%= item.getId() %>, 1)">+</button>
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
                    <button type="button" class="btn btn-primary" onclick="goCheckoutFromCart()">Tiến hành đặt hàng</button>
                </div>
            </div>
        </div>
    <% } %>
    <% } %>

    <%-- Gợi ý luôn hiện trên tab Giỏ (kể cả giỏ trống) để vào luồng đặt món --%>
    <% if ("cart".equals(activeView)) { %>
    <div class="sale-section" id="sale-section">
        <div class="section-header" style="text-align:left;margin-bottom:6px;">
            <div class="section-tag">✨ Gợi ý đơn cho bạn</div>
            <h3 class="section-title" style="font-size:1.4rem;">Chọn món để đặt</h3>
            <p class="section-desc" style="margin:0;">Bấm vào thẻ để xem chi tiết; dùng <strong>Đặt nhanh</strong> để sang bước đặt hàng ngay (1 món).</p>
        </div>
        <div id="sale-grid" class="sale-grid"></div>
        <p style="margin-top:14px;color:var(--text-light);font-size:.88rem;">
            Hoặc mở <a href="menu.html">toàn bộ thực đơn</a> để chọn thêm.
        </p>
    </div>
    <% } %>

    <% if ("tracking".equals(activeView)) { %>
    <div class="cart-card p-4">
        <% if (deliveringOrders == null || deliveringOrders.isEmpty()) { %>
            <p class="mb-2">Hiện không có đơn đang xử lý (chờ xác nhận / chuẩn bị / giao).</p>
            <p class="mb-0" style="color:var(--text-light);font-size:.9rem;">Đơn vừa đặt có trạng thái &quot;Chờ xác nhận&quot; sẽ hiển thị tại đây.</p>
        <% } else { for (Order order : deliveringOrders) {
            List<OrderItem> tItems = orderItemsMap != null ? orderItemsMap.get(order.getId()) : null;
            int os = order.getStatus();
            String[] tls = {"Chờ xác nhận", "Đang chuẩn bị", "Đang giao", "Hoàn thành"};
        %>
            <div class="border rounded p-3 mb-3" id="track-order-<%= order.getId() %>" style="border:1px solid var(--border);border-radius:12px;padding:14px;margin-bottom:12px;">
                <div style="display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;margin-bottom:10px;">
                    <strong>Đơn #<%= order.getId() %></strong>
                    <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                        <span class="status-chip status-<%= order.getStatus() %>" id="status-text-<%= order.getId() %>"><%= statusLabel(order.getStatus()) %></span>
                        <a class="btn btn-outline" style="padding:6px 12px;font-size:.82rem;" href="OrderTrackingServlet?orderId=<%= order.getId() %>">Theo dõi chi tiết</a>
                    </div>
                </div>
                <% if (tItems != null && !tItems.isEmpty()) { %>
                <ul style="margin:0 0 12px 1rem;padding:0;color:var(--text-mid);font-size:.9rem;">
                    <% for (OrderItem li : tItems) { %>
                    <li><%= li.getFoodName() %> × <%= li.getQuantity() %> — <%= String.format("%,.0fđ", li.getPrice() * li.getQuantity()) %></li>
                    <% } %>
                </ul>
                <% } %>
                <div class="timeline">
                    <% for (int ti = 0; ti < tls.length; ti++) {
                        String cls;
                        if (os >= 3) cls = "done";
                        else if (ti < os) cls = "done";
                        else if (ti == os) cls = "current";
                        else cls = "";
                    %>
                    <div class="timeline-step <%= cls %>"><strong><%= tls[ti] %></strong></div>
                    <% } %>
                </div>
                <% if (os >= 1 && os <= 2) { %>
                <p class="mb-1 mt-2">Shipper (demo): Nguyễn Văn Nhanh — 59X2-456.78</p>
                <% } %>
                <small style="color:var(--text-light);">Đặt lúc: <%= order.getCreatedAt() %> — Tổng: <strong><%= String.format("%,.0fđ", order.getTotalPrice()) %></strong></small>
            </div>
        <% }} %>
    </div>
    <% } %>

    <% if ("history".equals(activeView)) { %>
    <div class="cart-card p-4">
        <% if (historyOrders == null || historyOrders.isEmpty()) { %>
            <p class="mb-2">Chưa có đơn trong lịch sử (hoàn thành / đã hủy).</p>
            <p class="mb-0" style="color:var(--text-light);font-size:.9rem;">Đơn đã giao hoặc hủy sẽ hiển thị tại đây. Chạy script SQL demo trong <code>BTL_FoodMenu.sql</code> để có dữ liệu mẫu.</p>
        <% } else { for (Order order : historyOrders) {
            List<OrderItem> hItems = orderItemsMap != null ? orderItemsMap.get(order.getId()) : null;
        %>
            <div class="border rounded p-3 mb-3 history-order-card" style="border:1px solid var(--border);border-radius:12px;padding:14px;margin-bottom:12px;">
                <div style="display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;margin-bottom:10px;">
                    <div>
                        <strong>Đơn #<%= order.getId() %></strong>
                        <div style="color:var(--text-light);font-size:.86rem;margin-top:4px;">Đặt lúc: <%= order.getCreatedAt() %></div>
                    </div>
                    <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center;">
                        <span class="status-chip status-<%= order.getStatus() %>"><%= statusLabel(order.getStatus()) %></span>
                    </div>
                </div>
                <% if (hItems != null && !hItems.isEmpty()) { %>
                <ul class="history-items" style="margin:0 0 12px 1.1rem;padding:0;color:var(--text-mid);font-size:.92rem;line-height:1.5;">
                    <% for (OrderItem li : hItems) { %>
                    <li><%= li.getFoodName() %> × <%= li.getQuantity() %> — <%= String.format("%,.0fđ", li.getPrice() * li.getQuantity()) %></li>
                    <% } %>
                </ul>
                <% } %>
                <p style="margin-bottom:12px;">Tổng đơn: <strong style="color:var(--primary);"><%= String.format("%,.0fđ", order.getTotalPrice()) %></strong></p>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <a class="btn btn-outline" href="OrderServlet?action=detail&amp;view=page&amp;orderId=<%= order.getId() %>">Chi tiết</a>
                    <% if (order.getStatus() == 3) { %>
                    <a class="btn btn-outline" href="OrderTrackingServlet?orderId=<%= order.getId() %>">Xem tiến trình (đã xong)</a>
                    <% } %>
                    <button type="button" class="btn btn-outline" onclick="reorder(<%= order.getId() %>)">Mua lại</button>
                    <% if (order.getStatus() == 3) {
                        List<OrderItem> itemList = orderItemsMap.get(order.getId());
                        if (itemList != null && !itemList.isEmpty()) {
                    %>
                    <button type="button" class="btn btn-primary" onclick="reviewItem(<%= order.getId() %>, <%= itemList.get(0).getFoodId() %>)">Đánh giá</button>
                    <% }} %>
                </div>
            </div>
        <% }} %>
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
    const rows = document.querySelectorAll("#cart-tbody tr");
    let total = 0;
    rows.forEach(row => {
        const check = row.querySelector(".item-check");
        if (check && check.checked) {
            const subtotal = row.querySelector("[id^='subtotal-']");
            if (subtotal) total += parseMoney(subtotal.textContent);
        }
    });
    const totalEl = document.getElementById("grand-total");
    if (totalEl) totalEl.textContent = formatMoney(total);
}

function updateSelectedTotal() {
    updateGrandTotal();
}

/** Chỉ đặt các dòng giỏ đang được tích chọn */
function goCheckoutFromCart() {
    const ids = [];
    document.querySelectorAll("#cart-tbody tr.cart-item-row").forEach(function (row) {
        const check = row.querySelector(".item-check");
        if (check && check.checked) {
            const id = row.getAttribute("data-cart-item-id");
            if (id) ids.push(id);
        }
    });
    if (!ids.length) {
        alert("Vui lòng chọn ít nhất một món trong giỏ để đặt hàng.");
        return;
    }
    window.location.href = "checkout.html?cartItemIds=" + encodeURIComponent(ids.join(","));
}

function changeQty(cartItemId, delta) {
    const row = document.getElementById("row-" + cartItemId);
    if (!row) return;
    const input = row.querySelector("input[type='number']");
    const current = Number(input.value) || 1;
    const next = Math.max(1, current + delta);
    input.value = next;
    updateQuantity(cartItemId, next);
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
        credentials: "same-origin",
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
            showToast(data.error, "error");
            return;
        }

        const row = document.getElementById("row-" + cartItemId);
        if (!row) return;
        const unit = Number(row.dataset.unitPrice);
        const fallbackCell = row.cells && row.cells.length > 3 ? row.cells[3] : null;
        const unitPrice = Number.isFinite(unit) && unit > 0 ? unit : parseMoney(fallbackCell ? fallbackCell.textContent : "0");
        const subtotalEl = document.getElementById("subtotal-" + cartItemId);
        subtotalEl.textContent = formatMoney(unitPrice * qty);
        updateGrandTotal();
        showToast("Đã cập nhật số lượng", "success");
    })
    .catch(() => showToast("Có lỗi khi cập nhật số lượng.", "error"));
}

function removeItem(cartItemId) {
    if (!confirm("Bạn chắc chắn muốn xóa món này khỏi giỏ?")) return;

    fetch("CartServlet", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            action: "delete",
            cartItemId: String(cartItemId)
        }).toString()
    })
    .then(r => r.json())
    .then(data => {
        if (data.error) {
            showToast(data.error, "error");
            return;
        }
        const row = document.getElementById("row-" + cartItemId);
        if (row) row.remove();
        updateGrandTotal();
        showToast("Đã xóa món khỏi giỏ hàng", "success");
        if (!document.querySelector("#cart-tbody tr")) {
            window.location.reload();
        }
    })
    .catch(() => showToast("Có lỗi khi xóa món khỏi giỏ.", "error"));
}

function reorder(orderId) {
    fetch("CartServlet", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ action: "reorder", orderId: String(orderId) }).toString()
    }).then(r => r.json()).then(data => {
        if (data.error) return showToast(data.error, "error");
        showToast("Đã thêm lại món vào giỏ hàng.", "success");
    }).catch(() => showToast("Không thể mua lại đơn này.", "error"));
}

function reviewItem(orderId, foodId) {
    const rating = prompt("Nhập số sao đánh giá (1-5):", "5");
    if (!rating) return;
    const comment = prompt("Nhập nhận xét của bạn:", "Món ăn ngon, giao nhanh.");
    fetch("CartServlet", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            action: "review",
            orderId: String(orderId),
            foodId: String(foodId),
            rating: String(rating),
            comment: comment || ""
        }).toString()
    }).then(r => r.json()).then(data => {
        if (data.error) return showToast(data.error, "error");
        showToast("Cảm ơn bạn đã đánh giá.", "success");
    }).catch(() => showToast("Không thể gửi đánh giá.", "error"));
}

function statusLabelVi(status) {
    const n = Number(status);
    if (n === 0) return "Chờ xác nhận";
    if (n === 1) return "Đang chuẩn bị";
    if (n === 2) return "Đang giao";
    if (n === 3) return "Hoàn thành";
    if (n === 4) return "Đã hủy";
    return "Đang xử lý";
}

if ("<%= activeView %>" === "tracking") {
    setInterval(() => {
        fetch("CartServlet?view=tracking-data", { credentials: "same-origin" })
            .then(r => r.json())
            .then(list => {
                (list || []).forEach(order => {
                    const el = document.getElementById("status-text-" + order.id);
                    if (el) el.textContent = statusLabelVi(order.status);
                });
            })
            .catch(() => {});
    }, 30000);
}

function showToast(msg, type) {
    const t = document.getElementById("toast");
    if (!t) return;
    t.textContent = msg;
    t.className = "toast show " + (type || "");
    setTimeout(() => { t.className = "toast"; }, 2600);
}

function escapeHtml(s) {
    if (!s) return "";
    return String(s)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

/** Luôn hiện đủ dòng giỏ — tránh ẩn nhầm sau khi chuyển tab / BFCache */
function initCartTableUi() {
    const tbody = document.getElementById("cart-tbody");
    if (!tbody) return;
    tbody.querySelectorAll(".cart-item-row").forEach(function (row) {
        row.style.display = "";
        row.style.visibility = "";
    });
    updateGrandTotal();
}

function loadSaleSuggestions() {
    const grid = document.getElementById("sale-grid");
    if (!grid) return;
    fetch("FoodServlet?sort=rating")
        .then(r => r.json())
        .then(list => {
            const data = (list || []).slice(0, 8);
            grid.innerHTML = "";
            data.forEach(food => {
                const salePercent = (food.id % 4 === 0) ? 20 : ((food.id % 3 === 0) ? 15 : 10);
                const oldPrice = Number(food.price || 0);
                const newPrice = Math.max(0, oldPrice * (100 - salePercent) / 100);
                const card = document.createElement("div");
                card.className = "sale-card";
                const imageUrl = escapeHtml(food.image_url || "images/food-placeholder.svg");
                const foodName = escapeHtml(food.name || "");
                const fid = Number(food.id || 0);
                card.innerHTML =
                    '<img class="sale-img" src="' + imageUrl + '" alt="' + foodName + '" style="cursor:pointer;">' +
                    '<div class="sale-body">' +
                        '<span class="sale-badge">Giảm ' + salePercent + '%</span>' +
                        '<div class="sale-name" style="cursor:pointer;">' + foodName + '</div>' +
                        '<div>' +
                            '<span class="sale-price">' + formatMoney(newPrice) + '</span>' +
                            '<span class="sale-old">' + formatMoney(oldPrice) + '</span>' +
                        '</div>' +
                        '<div class="sale-actions">' +
                            '<button type="button" class="btn-sale-detail">Chi tiết</button>' +
                            '<button type="button" class="btn-sale-quick">Đặt nhanh</button>' +
                        '</div>' +
                    '</div>';
                const goDetail = () => { window.location.href = "food-detail.html?id=" + fid; };
                const goQuick = (e) => {
                    if (e) e.stopPropagation();
                    window.location.href = "order-select.html?foodId=" + fid + "&quantity=1";
                };
                card.querySelector(".sale-img").addEventListener("click", goDetail);
                card.querySelector(".sale-name").addEventListener("click", goDetail);
                card.querySelector(".btn-sale-detail").addEventListener("click", function (e) {
                    e.stopPropagation();
                    goDetail();
                });
                card.querySelector(".btn-sale-quick").addEventListener("click", goQuick);
                const img = card.querySelector(".sale-img");
                if (img) img.onerror = () => { img.src = "images/food-placeholder.svg"; };
                grid.appendChild(card);
            });
        })
        .catch(() => {
            grid.innerHTML = "<p>Không tải được gợi ý món. Thử <a href=\"menu.html\">mở thực đơn</a>.</p>";
        });
}

<% if ("cart".equals(activeView)) { %>
/** Trang giỏ render trống nhưng API vẫn có dòng (cache/BFCache): reload một lần */
(function syncCartFromApi() {
    const hasRows = document.querySelectorAll(".cart-item-row").length > 0;
    if (hasRows) return;
    fetch("CartServlet?view=cart-data", { credentials: "same-origin" })
        .then(r => r.json())
        .then(data => {
            const n = (data && data.items && data.items.length) ? data.items.length : 0;
            if (n > 0) window.location.reload();
        })
        .catch(function () {});
})();
<% } %>

document.addEventListener("DOMContentLoaded", function () {
    initCartTableUi();
});
window.addEventListener("pageshow", function (ev) {
    if (ev.persisted) initCartTableUi();
});
initCartTableUi();
loadSaleSuggestions();
</script>
<div class="toast" id="toast"></div>
<script src="js/auth-navbar.js"></script>
<script src="js/notification-widget.js"></script>
</body>
</html>
