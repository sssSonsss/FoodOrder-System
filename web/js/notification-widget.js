/* Trung tâm thông báo — icon chuông + panel dropdown gắn navbar */
(function () {
    /** URL ảnh trực tiếp (HTTPS). Link trang HTML không dùng được làm src của <img>. */
    var FOOD_IMG_BY_THEME = {
        fallback: "https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg",
        drink: "https://upload.wikimedia.org/wikipedia/commons/1/18/Bubble_Tea.png",
        banhmi: "https://upload.wikimedia.org/wikipedia/commons/5/5f/B%C3%A1nh_m%C3%AC_th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng_in_Saigon.jpg",
        com: "https://upload.wikimedia.org/wikipedia/commons/8/8c/C%C6%A1m_t%E1%BA%A5m.jpg",
        bunpho: "https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg",
        chay: "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Vegetable_stir_fry_002.jpg/640px-Vegetable_stir_fry_002.jpg",
        dessert: "https://upload.wikimedia.org/wikipedia/commons/4/48/Flan_Caramel.jpg"
    };
    function ensureNavbarIcons(actions) {
        var el = document.getElementById("navbar-icons");
        if (!el) {
            el = document.createElement("div");
            el.id = "navbar-icons";
            el.className = "navbar-icons";
            actions.insertBefore(el, actions.firstChild);
        }
        return el;
    }

    const tabs = [
        { id: "general", label: "Thông báo chung" },
        { id: "orders", label: "Đơn đã đặt hàng" },
        { id: "promos", label: "Danh sách khuyến mãi" }
    ];

    const fallbackGeneral = [
        {
            id: 0,
            title: "Chào mừng bạn quay lại FoodOrder",
            message: "Hôm nay có nhiều món đang chạy ưu đãi theo giờ vàng.",
            type: "SYSTEM",
            is_read: false,
            created_at: "Vừa xong"
        },
        {
            id: 0,
            title: "Cập nhật giỏ hàng",
            message: "Bạn đang có 3 món demo trong giỏ để kiểm thử luồng đặt hàng.",
            type: "SYSTEM",
            is_read: false,
            created_at: "5 phút trước"
        },
        {
            id: 0,
            title: "Hệ thống thanh toán",
            message: "COD đang được bật ở chế độ demo, chưa cần cấu hình cổng thanh toán.",
            type: "SYSTEM",
            is_read: true,
            created_at: "Hôm nay"
        }
    ];

    const demoOrders = [
        {
            id: 9002,
            title: "Đơn #9002 đang giao",
            status: "Đang giao",
            message: "Shipper Nguyễn Văn Nhanh đang đến với bạn trong khoảng 10 phút nữa.",
            total: 210000,
            items: "Phở bò tái nạm gầu, Trà đào cam sả, Cơm tấm",
            href: "CartServlet?view=tracking",
            time: "Cập nhật 2 phút trước"
        },
        {
            id: 9001,
            title: "Đơn #9001 đang chuẩn bị",
            status: "Đang chuẩn bị",
            message: "Quán đã nhận đơn và đang hoàn tất món trước khi giao.",
            total: 148000,
            items: "Trà sữa trân châu đen, Bánh mì pate thịt, Kem matcha",
            href: "CartServlet?view=tracking",
            time: "Cập nhật 8 phút trước"
        },
        {
            id: 8998,
            title: "Đơn #8998 đã hoàn thành",
            status: "Hoàn thành",
            message: "Đơn demo đã giao thành công. Bạn có thể xem lại trong lịch sử.",
            total: 175000,
            items: "Cơm tấm, Chè bà ba, Cà phê sữa đá",
            href: "CartServlet?view=history",
            time: "Hôm qua"
        }
    ];

    const fallbackPromos = [
        {
            id: 15,
            name: "Phở bò tái nạm gầu",
            category: "Bún & Phở",
            image_url: FOOD_IMG_BY_THEME.bunpho,
            price: 65000,
            discount: 20,
            description: "Ưu đãi giờ trưa cho món bán chạy nhất hôm nay."
        },
        {
            id: 1,
            name: "Trà sữa trân châu đen",
            category: "Đồ uống",
            image_url: FOOD_IMG_BY_THEME.drink,
            price: 35000,
            discount: 15,
            description: "Mua kèm món chính để nhận giảm giá đồ uống."
        },
        {
            id: 7,
            name: "Bánh mì pate thịt",
            category: "Bánh mì",
            image_url: FOOD_IMG_BY_THEME.banhmi,
            price: 25000,
            discount: 10,
            description: "Combo ăn sáng nhanh, đang chạy trong khung giờ demo."
        },
        {
            id: 23,
            name: "Kem tươi matcha",
            category: "Tráng miệng",
            image_url: FOOD_IMG_BY_THEME.dessert,
            price: 35000,
            discount: 18,
            description: "Món tráng miệng đang được đẩy banner khuyến mãi."
        }
    ];

    let state = {
        activeTab: "general",
        unread: 0,
        needLogin: false,
        general: fallbackGeneral,
        orders: demoOrders,
        promos: fallbackPromos
    };

    document.addEventListener("DOMContentLoaded", initNotificationCenter);

    /** @type {HTMLElement|null} */
    let notifyWrapEl = null;

    function bindGlobalNavUi() {
        if (window.__foNavUiBound) return;
        window.__foNavUiBound = true;
        if (typeof window.closeFoodOrderNavDropdowns !== "function") {
            window.closeFoodOrderNavDropdowns = function () {
                document.querySelectorAll(".fo-user-wrap.is-open").forEach(function (w) {
                    w.classList.remove("is-open");
                    var b = w.querySelector(".fo-user-trigger");
                    if (b) b.setAttribute("aria-expanded", "false");
                });
            };
        }
        document.addEventListener("click", function () {
            window.closeFoodOrderNavDropdowns();
        });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape") window.closeFoodOrderNavDropdowns();
        });
    }

    function initNotificationCenter() {
        const navActions = document.querySelector(".navbar-actions");
        if (!navActions || document.querySelector(".fo-notify-wrap")) return;

        bindGlobalNavUi();
        const cluster = ensureNavbarIcons(navActions);
        const widget = buildNotifyWidget();
        notifyWrapEl = widget;
        cluster.insertBefore(widget, cluster.firstChild);
        loadWidgetData();
    }

    function buildNotifyWidget() {
        const wrap = document.createElement("div");
        wrap.className = "fo-dropdown-wrap fo-notify-wrap";

        const button = document.createElement("button");
        button.type = "button";
        button.className = "fo-icon-btn fo-notify-trigger";
        button.title = "Thông báo";
        button.setAttribute("aria-label", "Mở thông báo");
        button.setAttribute("aria-expanded", "false");
        button.setAttribute("aria-haspopup", "true");
        button.innerHTML = '<span class="fo-notify-icon">🔔</span><span class="fo-notify-badge" hidden>0</span>';
        button.addEventListener("click", function (e) {
            e.stopPropagation();
            if (typeof window.closeFoodOrderNavDropdowns === "function") {
                window.closeFoodOrderNavDropdowns();
            }
            /* Chỉ mở khi đang đóng; đóng panel chỉ bằng nút × (không toggle bằng chuông). */
            if (!wrap.classList.contains("is-open")) {
                wrap.classList.add("is-open");
                button.setAttribute("aria-expanded", "true");
                renderTabs();
                renderActiveTab();
            }
        });

        const panel = document.createElement("div");
        panel.className = "fo-notify-dropdown-panel";
        panel.innerHTML =
            '<div class="fo-notify-dialog" role="dialog" aria-modal="true" aria-labelledby="fo-notify-title">' +
                '<div class="fo-notify-head">' +
                    '<div>' +
                        '<div class="fo-notify-kicker">FoodOrder</div>' +
                        '<h2 id="fo-notify-title">Thông báo</h2>' +
                    '</div>' +
                    '<button type="button" class="fo-notify-close" aria-label="Đóng">×</button>' +
                '</div>' +
                '<div class="fo-notify-tabs" role="tablist"></div>' +
                '<div class="fo-notify-body" id="fo-notify-body"></div>' +
            '</div>';

        panel.querySelector(".fo-notify-close").addEventListener("click", function (e) {
            e.stopPropagation();
            wrap.classList.remove("is-open");
            button.setAttribute("aria-expanded", "false");
        });

        /* Không bubble lên document — tránh bindGlobalNavUi / auth-navbar đóng panel khi đổi tab hoặc click trong sheet */
        panel.addEventListener("click", function (e) {
            e.stopPropagation();
        });

        wrap.appendChild(button);
        wrap.appendChild(panel);
        return wrap;
    }

    function notifyPanelOpen() {
        return notifyWrapEl && notifyWrapEl.classList.contains("is-open");
    }

    function renderTabs() {
        const wrap = document.querySelector(".fo-notify-tabs");
        if (!wrap) return;
        wrap.innerHTML = "";
        tabs.forEach(tab => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "fo-notify-tab" + (state.activeTab === tab.id ? " is-active" : "");
            button.textContent = tab.label;
            button.addEventListener("click", function () {
                state.activeTab = tab.id;
                renderTabs();
                renderActiveTab();
            });
            wrap.appendChild(button);
        });
    }

    function renderActiveTab() {
        const body = document.getElementById("fo-notify-body");
        if (!body) return;
        if (state.activeTab === "orders") {
            body.innerHTML = renderOrders();
        } else if (state.activeTab === "promos") {
            body.innerHTML = renderPromos();
            bindPromoImageFallbacks();
        } else {
            body.innerHTML = renderGeneral();
        }
    }

    function renderGeneral() {
        if (state.needLogin) {
            return '<div class="fo-notify-login-hint">'
                + '<p>Vui lòng đăng nhập để xem thông báo của bạn.</p>'
                + '<a class="btn btn-primary fo-notify-login-btn" href="' + escAttr(loginPageHref()) + '">Đăng nhập</a>'
                + "</div>";
        }
        const list = state.general.length ? state.general : fallbackGeneral;
        return '<div class="fo-notify-list">' + list.map(item =>
            '<button type="button" class="fo-notify-item' + (item.is_read ? "" : " is-unread") + '" data-notification-id="' + Number(item.id || 0) + '">' +
                '<span class="fo-notify-dot"></span>' +
                '<span>' +
                    '<strong>' + esc(item.title) + '</strong>' +
                    '<small>' + esc(item.message) + '</small>' +
                    '<em>' + esc(item.created_at || item.time || "Vừa xong") + '</em>' +
                '</span>' +
            '</button>'
        ).join("") + '</div>';
    }

    function renderOrders() {
        return '<div class="fo-order-list">' + state.orders.map(order =>
            '<a class="fo-order-card" href="' + escAttr(order.href) + '">' +
                '<div class="fo-order-main">' +
                    '<strong>' + esc(order.title) + '</strong>' +
                    '<span class="fo-order-status">' + esc(order.status) + '</span>' +
                '</div>' +
                '<p>' + esc(order.message) + '</p>' +
                '<div class="fo-order-meta">' +
                    '<span>' + esc(order.items) + '</span>' +
                    '<b>' + money(order.total) + '</b>' +
                '</div>' +
                '<em>' + esc(order.time) + '</em>' +
            '</a>'
        ).join("") + '</div>';
    }

    function renderPromos() {
        return '<div class="fo-promo-list">' + state.promos.map(promo => {
            const oldPrice = Number(promo.price || 0);
            const discount = Number(promo.discount || 10);
            const newPrice = Math.max(0, Math.round(oldPrice * (100 - discount) / 100));
            return '<a class="fo-promo-banner" href="food-detail.html?id=' + Number(promo.id || 1) + '">' +
                '<img src="' + escAttr(promo.image_url || FOOD_IMG_BY_THEME.fallback) + '" alt="' + escAttr(promo.name) + '">' +
                '<span class="fo-promo-copy">' +
                    '<span class="fo-promo-badge">Giảm ' + discount + '%</span>' +
                    '<strong>' + esc(promo.name) + '</strong>' +
                    '<small>' + esc(promo.description || promo.category || "Khuyến mãi đang chạy") + '</small>' +
                    '<span class="fo-promo-price">' + money(newPrice) + '<del>' + money(oldPrice) + '</del></span>' +
                '</span>' +
            '</a>';
        }).join("") + '</div>';
    }

    function bindPromoImageFallbacks() {
        document.querySelectorAll(".fo-promo-banner img").forEach(img => {
            img.onerror = function () {
                img.onerror = null;
                img.src = FOOD_IMG_BY_THEME.banhmi;
            };
        });
    }

    function loadWidgetData() {
        fetch("NotificationServlet", { headers: { Accept: "application/json" } })
            .then(r => r.json().then(data => ({ ok: r.ok, status: r.status, data })))
            .then(({ status, data }) => {
                state.needLogin = status === 401;
                if (state.needLogin) {
                    state.general = [];
                    state.unread = 0;
                    updateBadge();
                    if (notifyPanelOpen()) renderActiveTab();
                    return;
                }
                if (data && Array.isArray(data.items) && data.items.length) {
                    state.general = data.items;
                    state.unread = Number(data.unread != null ? data.unread : data.items.filter(x => !x.is_read).length);
                } else {
                    state.general = [];
                    state.unread = 0;
                }
                updateBadge();
                if (notifyPanelOpen()) renderActiveTab();
            })
            .catch(() => {
                state.needLogin = false;
                state.general = fallbackGeneral;
                state.unread = fallbackGeneral.filter(x => !x.is_read).length;
                updateBadge();
                if (notifyPanelOpen()) renderActiveTab();
            });

        fetch("OrderServlet", { headers: { Accept: "application/json" } })
            .then(r => r.json().then(raw => ({ ok: r.ok, status: r.status, raw })))
            .then(({ status, raw }) => {
                if (status === 401 || !Array.isArray(raw)) return;
                state.orders = raw.map(o => ({
                    id: o.id,
                    title: "Đơn #" + o.id,
                    status: orderStatusVi(o.status),
                    message: "Trạng thái: " + orderStatusVi(o.status) + ". Theo dõi trong mục Giỏ & đơn.",
                    total: o.total_price,
                    items: "Đơn của bạn",
                    href: "CartServlet?view=" + (o.status === 3 || o.status === 4 ? "history" : "tracking"),
                    time: o.created_at || ""
                }));
                if (notifyPanelOpen()) renderActiveTab();
            })
            .catch(() => {});

        fetchJson("FoodServlet?sort=rating").then(list => {
            if (!Array.isArray(list) || !list.length) return;
            state.promos = list.slice(0, 5).map(food => ({
                id: food.id,
                name: food.name,
                category: food.category_name,
                image_url: food.image_url || imageByCategory(food.category_name),
                price: food.price,
                discount: discountById(food.id),
                description: "Banner khuyến mãi demo cho " + (food.category_name || "món ăn nổi bật") + "."
            }));
        }).catch(() => {});
    }

    function loginPageHref() {
        let pathname = window.location.pathname || "/";
        const segs = pathname.split("/").filter(Boolean);
        if (segs.length >= 2) pathname = "/" + segs.slice(1).join("/");
        return "login.html?redirect=" + encodeURIComponent(pathname + (window.location.search || ""));
    }

    function orderStatusVi(st) {
        const n = Number(st);
        if (n === 0) return "Chờ xác nhận";
        if (n === 1) return "Đang chuẩn bị";
        if (n === 2) return "Đang giao";
        if (n === 3) return "Hoàn thành";
        if (n === 4) return "Đã hủy";
        return "Đơn hàng";
    }

    function updateBadge() {
        const badge = document.querySelector(".fo-notify-badge");
        if (!badge) return;
        const count = Math.max(0, Number(state.unread || 0));
        if (count > 0) {
            badge.hidden = false;
            badge.textContent = count > 99 ? "99+" : String(count);
        } else {
            badge.hidden = true;
        }
    }

    function fetchJson(url) {
        return fetch(url, { headers: { "Accept": "application/json" } }).then(response => {
            if (!response.ok) throw new Error("HTTP " + response.status);
            return response.json();
        });
    }

    function discountById(id) {
        if (id % 5 === 0) return 25;
        if (id % 4 === 0) return 20;
        if (id % 3 === 0) return 15;
        return 10;
    }

    function imageByCategory(categoryName) {
        const name = String(categoryName || "");
        if (name.includes("Đồ uống")) return FOOD_IMG_BY_THEME.drink;
        if (name.includes("Bánh mì")) return FOOD_IMG_BY_THEME.banhmi;
        if (name.includes("Bún") || name.includes("Phở")) return FOOD_IMG_BY_THEME.bunpho;
        if (name.includes("Cơm")) return FOOD_IMG_BY_THEME.com;
        if (name.includes("Chay")) return FOOD_IMG_BY_THEME.chay;
        if (name.includes("Tráng")) return FOOD_IMG_BY_THEME.dessert;
        return FOOD_IMG_BY_THEME.fallback;
    }

    function money(value) {
        return Number(value || 0).toLocaleString("vi-VN") + "đ";
    }

    function esc(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function escAttr(value) {
        return esc(value).replace(/'/g, "&#39;");
    }
})();
