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
        .status-1 { background:#fff3da; color:#9f6b00; }
        .status-2 { background:#e8f1ff; color:#1a62b8; }
        .status-3 { background:#e5f7ec; color:#1c8f4d; }
        .status-4 { background:#fdeceb; color:#bc3528; }
        .status-demo { background:#f3f4f6; color:#5b6470; margin-left: 6px; }
        .notify-wrap { position: relative; }
        .notify-btn { position: relative; border: none; background: transparent; font-size: 1.2rem; padding: 8px; border-radius: 8px; }
        .notify-btn:hover { background: var(--border); }
        .notify-badge { position: absolute; top: -2px; right: -2px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 99px; background: var(--danger); color: #fff; font-size: .72rem; font-weight: 700; display: none; align-items: center; justify-content: center; }
        .notify-dropdown { position: absolute; right: 0; top: 42px; width: 360px; max-width: 92vw; background: #fff; border: 1px solid var(--border); border-radius: 12px; box-shadow: var(--shadow-md); display: none; z-index: 2000; overflow: hidden; }
        .notify-head { padding: 10px 12px; font-weight: 700; border-bottom: 1px solid var(--border); }
        .notify-list { max-height: 320px; overflow: auto; }
        .notify-item { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
        .notify-item:hover { background: #fafbfc; }
        .notify-item.unread { background: #fff6f2; }
        .notify-time { color: var(--text-light); font-size: .78rem; margin-top: 4px; display: block; }
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
        <div class="notify-wrap">
            <button class="notify-btn" id="notify-btn" onclick="toggleNotifications()" title="Thông báo">
                🔔
                <span class="notify-badge" id="notify-badge">0</span>
            </button>
            <div class="notify-dropdown" id="notify-dropdown">
                <div class="notify-head">Thông báo mới</div>
                <div class="notify-list" id="notify-list"></div>
            </div>
        </div>
        <a href="menu.html" class="btn btn-ghost">← Tiếp tục mua sắm</a>
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
            <p class="mb-3"><%= emptyMessage == null ? "Giỏ hàng của bạn đang trống" : emptyMessage %></p>
            <a class="btn btn-primary" href="menu.html">Quay lại mua sắm</a>
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
                    <tr id="row-<%= item.getId() %>" class="cart-item-row">
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
                    <a class="btn btn-primary" href="checkout.html">Tiến hành đặt hàng</a>
                </div>
            </div>
        </div>
    <% } %>
    <% } %>

    <% if ("cart".equals(activeView) && cartItems != null && !cartItems.isEmpty()) { %>
    <div class="sale-section" id="sale-section">
        <div class="section-header" style="text-align:left;margin-bottom:6px;">
            <div class="section-tag">🔥 Đang sale</div>
            <h3 class="section-title" style="font-size:1.4rem;">Gợi ý thực đơn cho bạn</h3>
            <p class="section-desc" style="margin:0;">Các món ăn đang khuyến mãi theo giờ vàng.</p>
        </div>
        <div id="sale-grid" class="sale-grid"></div>
    </div>
    <% } %>

    <% if ("tracking".equals(activeView)) { %>
    <div class="cart-card p-4">
        <% if (deliveringOrders == null || deliveringOrders.isEmpty()) { %>
            <p class="mb-0">Hiện chưa có đơn đang giao.</p>
        <% } else { for (Order order : deliveringOrders) { %>
            <div class="border rounded p-3 mb-3" id="track-order-<%= order.getId() %>" style="border:1px solid var(--border);border-radius:12px;padding:14px;margin-bottom:12px;">
                <div style="display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;margin-bottom:10px;">
                    <strong>Đơn #<%= order.getId() %></strong>
                    <div>
                        <span class="status-chip status-<%= order.getStatus() %>" id="status-text-<%= order.getId() %>"><%= statusLabel(order.getStatus()) %></span>
                        <% if (order.getId() >= 8900) { %><span class="status-chip status-demo">Dữ liệu demo</span><% } %>
                    </div>
                </div>
                <div class="timeline">
                    <div class="timeline-step <%= order.getStatus() > 0 ? "done" : "current" %>"><strong>Shipper đang lấy hàng</strong></div>
                    <div class="timeline-step <%= order.getStatus() > 1 ? "done" : (order.getStatus() == 1 ? "current" : "") %>"><strong>Shipper đang đến với bạn</strong></div>
                    <div class="timeline-step <%= order.getStatus() > 2 ? "done" : (order.getStatus() == 2 ? "current" : "") %>"><strong>Đã giao thành công</strong></div>
                </div>
                <p class="mb-1">Shipper: Nguyễn Văn Nhanh - Biển số: 59X2-456.78</p>
                <small style="color:var(--text-light);">Cập nhật: <%= order.getCreatedAt() %></small>
            </div>
        <% }} %>
    </div>
    <% } %>

    <% if ("history".equals(activeView)) { %>
    <div class="cart-card p-4">
        <% if (historyOrders == null || historyOrders.isEmpty()) { %>
            <p class="mb-0">Chưa có đơn trong lịch sử.</p>
        <% } else { for (Order order : historyOrders) { %>
            <div class="border rounded p-3 mb-3" style="border:1px solid var(--border);border-radius:12px;padding:14px;margin-bottom:12px;">
                <div style="display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;margin-bottom:10px;">
                    <strong>Đơn #<%= order.getId() %></strong>
                    <div>
                        <span class="status-chip status-<%= order.getStatus() %>"><%= statusLabel(order.getStatus()) %></span>
                        <% if (order.getId() >= 8900) { %><span class="status-chip status-demo">Dữ liệu demo</span><% } %>
                    </div>
                </div>
                <p style="margin-bottom:10px;">Tổng tiền: <strong><%= String.format("%,.0fđ", order.getTotalPrice()) %></strong></p>
                <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <button class="btn btn-outline" onclick="reorder(<%= order.getId() %>)">Mua lại</button>
                    <% if (order.getStatus() == 3) { 
                        List<OrderItem> itemList = orderItemsMap.get(order.getId());
                        if (itemList != null && !itemList.isEmpty()) {
                    %>
                    <button class="btn btn-primary" onclick="reviewItem(<%= order.getId() %>, <%= itemList.get(0).getFoodId() %>)">Đánh giá</button>
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
        const unitPriceText = row.children[2].textContent;
        const subtotalEl = document.getElementById("subtotal-" + cartItemId);
        subtotalEl.textContent = formatMoney(parseMoney(unitPriceText) * qty);
        updateGrandTotal();
        showToast("Đã cập nhật số lượng", "success");
    })
    .catch(() => showToast("Có lỗi khi cập nhật số lượng.", "error"));
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
    if (status === 1) return "Đang chuẩn bị";
    if (status === 2) return "Đang giao";
    if (status === 3) return "Hoàn thành";
    return "Chờ xác nhận";
}

if ("<%= activeView %>" === "tracking") {
    setInterval(() => {
        fetch("CartServlet?view=tracking-data")
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

let notificationsOpen = false;
function toggleNotifications() {
    notificationsOpen = !notificationsOpen;
    const dropdown = document.getElementById("notify-dropdown");
    dropdown.style.display = notificationsOpen ? "block" : "none";
    if (notificationsOpen) loadNotifications();
}

function loadNotifications() {
    fetch("NotificationServlet")
        .then(r => r.json())
        .then(data => {
            renderNotifications(data.items || []);
            updateBadge(data.unread || 0);
        })
        .catch(() => {});
}

function updateBadge(unread) {
    const badge = document.getElementById("notify-badge");
    if (!badge) return;
    if (unread > 0) {
        badge.style.display = "inline-flex";
        badge.textContent = unread > 99 ? "99+" : String(unread);
    } else {
        badge.style.display = "none";
    }
}

function renderNotifications(list) {
    const wrap = document.getElementById("notify-list");
    if (!wrap) return;
    if (!list.length) {
        wrap.innerHTML = '<div class="notify-item">Hiện chưa có thông báo mới.</div>';
        return;
    }
    wrap.innerHTML = "";
    list.forEach(item => {
        const div = document.createElement("div");
        div.className = "notify-item" + (item.is_read ? "" : " unread");
        div.onclick = () => markRead(item.id);
        div.innerHTML =
            "<strong>" + escapeHtml(item.title || "") + "</strong>" +
            "<div>" + escapeHtml(item.message || "") + "</div>" +
            "<span class='notify-time'>" + escapeHtml(item.created_at || "") + "</span>";
        wrap.appendChild(div);
    });
}

function markRead(notificationId) {
    if (!notificationId || notificationId <= 0) return;
    fetch("NotificationServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            action: "mark-read",
            notificationId: String(notificationId)
        }).toString()
    }).then(() => loadNotifications()).catch(() => {});
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

setInterval(loadNotifications, 60000);
loadNotifications();

const CART_BATCH = 6;
let cartVisible = CART_BATCH;

function initInfiniteCart() {
    const rows = Array.from(document.querySelectorAll(".cart-item-row"));
    if (!rows.length) return;

    function renderVisibleRows() {
        rows.forEach((row, idx) => {
            row.style.display = idx < cartVisible ? "" : "none";
        });
        updateGrandTotal();
    }

    renderVisibleRows();
    window.addEventListener("scroll", function () {
        const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 180;
        if (!nearBottom) return;
        if (cartVisible < rows.length) {
            cartVisible += CART_BATCH;
            renderVisibleRows();
        }
    });
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
                card.onclick = () => { window.location.href = `food-detail.html?id=${food.id}`; };
                card.innerHTML = `
                    <img class="sale-img" src="${food.image_url || 'images/food-placeholder.svg'}"
                         alt="${escapeHtml(food.name || '')}" onerror="this.src='images/food-placeholder.svg'">
                    <div class="sale-body">
                        <span class="sale-badge">Giảm ${salePercent}%</span>
                        <div class="sale-name">${escapeHtml(food.name || '')}</div>
                        <div>
                            <span class="sale-price">${formatMoney(newPrice)}</span>
                            <span class="sale-old">${formatMoney(oldPrice)}</span>
                        </div>
                    </div>
                `;
                grid.appendChild(card);
            });
        })
        .catch(() => {
            grid.innerHTML = "<p>Không tải được danh sách món sale lúc này.</p>";
        });
}

initInfiniteCart();
loadSaleSuggestions();
</script>
<div class="toast" id="toast"></div>
</body>
</html>
