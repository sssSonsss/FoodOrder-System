<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Voucher &amp; ưu đãi - FoodOrder</title>
    <link rel="stylesheet" href="css/style.css">
    <style>
        .voucher-wrap { max-width: 1100px; margin: 0 auto; padding: 28px 5% 64px; }
        .toolbar { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 16px; }
        .toolbar input[type="search"], .toolbar select {
            padding: 10px 12px; border: 1px solid var(--border); border-radius: 10px; min-width: 200px;
            background: #fff;
        }
        .toolbar .grow { flex: 1; min-width: 220px; }
        .tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
        .tab-btn {
            padding: 10px 16px; border: 1px solid var(--border); background: #fff;
            border-radius: 999px; font-weight: 700; color: var(--text-mid); cursor: pointer;
            transition: var(--transition);
        }
        .tab-btn.active { background: var(--primary); color: #fff; border-color: var(--primary); box-shadow: var(--shadow-primary); }
        .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 14px; }
        .v-card {
            background: linear-gradient(135deg, #fff8f5 0%, #ffffff 55%); border: 1px solid var(--border);
            border-radius: 14px; padding: 14px 14px 16px; box-shadow: var(--shadow-sm);
            display: flex; flex-direction: column; gap: 8px; min-height: 200px;
        }
        .v-card.history { background: #fafbff; }
        .v-code {
            font-size: 1.05rem; font-weight: 800; letter-spacing: .04em; color: var(--primary);
            border-bottom: 1px dashed var(--border); padding-bottom: 8px;
        }
        .v-title { font-weight: 700; font-size: 1rem; }
        .v-desc { color: var(--text-light); font-size: .9rem; line-height: 1.45; flex: 1; }
        .v-meta { font-size: .82rem; color: var(--text-mid); }
        .badge-type {
            display: inline-block; font-size: .72rem; font-weight: 700; padding: 3px 8px; border-radius: 99px;
            background: #e8f1ff; color: #1a62b8;
        }
        .badge-type.amount { background: #fff3da; color: #9f6b00; }
        .badge-type.freeship { background: #e5f7ec; color: #1c8f4d; }
        .toast-inline { margin-top: 8px; padding: 8px 10px; border-radius: 8px; font-size: .88rem; display: none; }
        .toast-inline.ok { display: block; background: #e5f7ec; color: #166534; }
        .toast-inline.err { display: block; background: #fdeceb; color: #991b1b; }
        .empty-hint { padding: 28px; text-align: center; color: var(--text-light); border: 1px dashed var(--border); border-radius: 14px; background: #fff; }
    </style>
</head>
<body>
<nav class="navbar">
    <a class="navbar-brand" href="index.html"><div class="logo-icon">🍜</div>FoodOrder</a>
    <ul class="navbar-nav">
        <li><a href="index.html">Trang chủ</a></li>
        <li><a href="menu.html">Thực đơn</a></li>
    </ul>
    <div class="navbar-actions">
        <div class="navbar-icons" id="navbar-icons"></div>
    </div>
</nav>

<section class="voucher-wrap">
    <div class="section-header" style="text-align:left;margin-bottom:16px;">
        <div class="section-tag">🎟️ Ưu đãi</div>
        <h2 class="section-title">Voucher của bạn</h2>
        <p class="section-desc" style="margin:0;">Nhận ưu đãi phù hợp, quản lý mã trong ví và xem lại lịch sử đã dùng.</p>
    </div>

    <div class="tabs" role="tablist">
        <button type="button" class="tab-btn active" data-tab="offers" id="tab-offers">Ưu đãi của tôi</button>
        <button type="button" class="tab-btn" data-tab="my" id="tab-my">Voucher của tôi</button>
        <button type="button" class="tab-btn" data-tab="history" id="tab-history">Lịch sử</button>
    </div>

    <div class="toolbar">
        <input type="search" id="kw" class="grow" placeholder="Tìm theo mã, tiêu đề..." autocomplete="off">
        <select id="filter-type" title="Lọc loại giảm giá">
            <option value="ALL">Tất cả loại</option>
            <option value="PERCENT">Giảm %</option>
            <option value="AMOUNT">Giảm tiền</option>
            <option value="FREESHIP">Freeship</option>
        </select>
        <button type="button" class="btn btn-primary" id="btn-search">🔍 Tìm / Lọc</button>
    </div>

    <div id="toast-bar" class="toast-inline"></div>
    <div id="grid" class="grid"></div>
    <p id="empty" class="empty-hint" style="display:none;">Chưa có dữ liệu hiển thị.</p>
</section>

<script>
    let activeTab = "offers";

    function showToast(message, ok) {
        const el = document.getElementById("toast-bar");
        el.textContent = message;
        el.className = "toast-inline " + (ok ? "ok" : "err");
        clearTimeout(showToast._t);
        showToast._t = setTimeout(() => { el.className = "toast-inline"; el.textContent = ""; }, 4200);
    }

    function typeBadgeClass(t) {
        if (!t) return "";
        if (t === "AMOUNT") return "amount";
        if (t === "FREESHIP") return "freeship";
        return "";
    }

    function typeLabel(t) {
        if (t === "PERCENT") return "Giảm %";
        if (t === "AMOUNT") return "Giảm tiền";
        if (t === "FREESHIP") return "Freeship";
        return t || "";
    }

    function scopeLabel(s) {
        if (!s || s === "ALL") return "Toàn hệ thống";
        if (s === "DRINK") return "Đồ uống";
        if (s === "FOOD") return "Món ăn";
        if (s === "SHIP") return "Giao hàng";
        return s;
    }

    function discountLine(v) {
        if (v.discountType === "PERCENT") return "Giảm " + v.discountValue + "% trên tạm tính";
        if (v.discountType === "AMOUNT") return "Giảm " + Number(v.discountValue || 0).toLocaleString("vi-VN") + "đ";
        if (v.discountType === "FREESHIP") return "Miễn phí vận chuyển";
        return "";
    }

    function renderCard(v) {
        const card = document.createElement("div");
        card.className = "v-card" + (activeTab === "history" ? " history" : "");

        const head = document.createElement("div");
        head.className = "v-code";
        head.textContent = v.code;

        const badge = document.createElement("span");
        badge.className = "badge-type " + typeBadgeClass(v.discountType);
        badge.textContent = typeLabel(v.discountType);

        const title = document.createElement("div");
        title.className = "v-title";
        title.textContent = v.title || "";

        const desc = document.createElement("div");
        desc.className = "v-desc";
        desc.textContent = v.description || "";

        const meta = document.createElement("div");
        meta.className = "v-meta";
        if (activeTab === "history") {
            meta.innerHTML = "Đã dùng: <strong>" + (v.usedAt || "-") + "</strong><br>"
                + "Đã tiết kiệm: <strong>" + Number(v.savedDiscount || 0).toLocaleString("vi-VN") + "đ</strong>"
                + (v.orderId ? "<br>Mã đơn liên quan: #" + v.orderId : "");
        } else if (activeTab === "my") {
            meta.textContent = "Nhận lúc: " + (v.claimedAt || "-") + " · " + discountLine(v)
                + " · HSD " + (v.expiryDate || "-") + " · " + scopeLabel(v.promoScope);
            const row = document.createElement("div");
            row.style.marginTop = "10px";
            const btnUse = document.createElement("button");
            btnUse.type = "button";
            btnUse.className = "btn btn-primary";
            btnUse.textContent = "Dùng ngay";
            btnUse.onclick = () => {
                window.location.href = "checkout.html?voucher=" + encodeURIComponent(v.code);
            };
            row.appendChild(btnUse);
            card.appendChild(head);
            card.appendChild(badge);
            card.appendChild(title);
            card.appendChild(desc);
            card.appendChild(meta);
            card.appendChild(row);
            return card;
        } else {
            meta.textContent = discountLine(v)
                + " · Đơn tối thiểu " + Number(v.minOrderValue || 0).toLocaleString("vi-VN") + "đ"
                + " · HSD " + (v.expiryDate || "-")
                + " · " + scopeLabel(v.promoScope)
                + (v.claimed ? " · Đã có trong ví" : "");
            const row = document.createElement("div");
            row.style.marginTop = "10px";
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = v.claimed ? "btn btn-outline" : "btn btn-primary";
            btn.disabled = !!v.claimed;
            btn.textContent = v.claimed ? "Đã nhận" : "Chọn & nhận voucher";
            btn.onclick = () => claimAndSwitch(v, btn);
            row.appendChild(btn);
            card.appendChild(head);
            card.appendChild(badge);
            card.appendChild(title);
            card.appendChild(desc);
            card.appendChild(meta);
            card.appendChild(row);
            return card;
        }

        card.appendChild(head);
        card.appendChild(badge);
        card.appendChild(title);
        card.appendChild(desc);
        card.appendChild(meta);
        return card;
    }

    function claimAndSwitch(v, btn) {
        if (v.claimed) return;
        btn.disabled = true;
        const body = new URLSearchParams({ action: "claim", voucherId: String(v.id) });
        fetch("VoucherServlet", { method: "POST", body })
            .then(r => r.json().then(data => ({ status: r.status, data })))
            .then(({ status, data }) => {
                if (status === 401) {
                    window.location.href = "login.html?redirect=" + encodeURIComponent("/VoucherServlet");
                    return;
                }
                if (data.ok) {
                    showToast(data.message || "Đã thêm vào Voucher của tôi.", true);
                    switchTab("my");
                    loadGrid();
                } else {
                    showToast(data.message || "Không nhận được voucher.", false);
                    btn.disabled = false;
                }
            })
            .catch(() => {
                showToast("Lỗi kết nối, thử lại sau.", false);
                btn.disabled = false;
            });
    }

    function loadGrid() {
        const grid = document.getElementById("grid");
        const empty = document.getElementById("empty");
        grid.innerHTML = "";
        const kw = document.getElementById("kw").value.trim();
        const filter = document.getElementById("filter-type").value;
        let url = "VoucherServlet?action=api&tab=" + encodeURIComponent(activeTab);
        if (activeTab === "offers") {
            url += "&keyword=" + encodeURIComponent(kw) + "&filter=" + encodeURIComponent(filter);
        }
        fetch(url)
            .then(r => {
                if (r.status === 401) {
                    let pathname = window.location.pathname || "/";
                    const segs = pathname.split("/").filter(Boolean);
                    if (segs.length >= 2) pathname = "/" + segs.slice(1).join("/");
                    window.location.href = "login.html?redirect=" + encodeURIComponent(pathname + "?action=api");
                    return Promise.reject();
                }
                return r.json();
            })
            .then(list => {
                if (!Array.isArray(list) || !list.length) {
                    empty.style.display = "block";
                    empty.textContent = "Không có voucher để hiển thị.";
                    return;
                }
                empty.style.display = "none";
                empty.textContent = "Không có dữ liệu hiển thị.";
                list.forEach(v => grid.appendChild(renderCard(v)));
            })
            .catch(() => {
                empty.style.display = "block";
                empty.textContent = "Không tải được danh sách voucher.";
            });
    }

    function switchTab(tab) {
        activeTab = tab;
        document.querySelectorAll(".tab-btn").forEach(b => b.classList.toggle("active", b.dataset.tab === tab));
        const toolbar = document.querySelector(".toolbar");
        toolbar.style.display = (tab === "offers") ? "flex" : "none";
        loadGrid();
    }

    document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.addEventListener("click", () => switchTab(btn.dataset.tab));
    });
    document.getElementById("btn-search").addEventListener("click", loadGrid);
    document.getElementById("kw").addEventListener("keydown", e => {
        if (e.key === "Enter") loadGrid();
    });

    switchTab("offers");
</script>
<script src="js/auth-navbar.js"></script>
<script src="js/notification-widget.js"></script>
</body>
</html>
