/**
 * Icon tài khoản + menu dropdown (đăng nhập / đăng ký / điều hướng).
 * Dùng chung #navbar-icons với notification-widget.js
 */
(function () {
    window.closeFoodOrderNavDropdowns = window.closeFoodOrderNavDropdowns || function () {
        document.querySelectorAll(".fo-dropdown-wrap.is-open").forEach(function (w) {
            w.classList.remove("is-open");
            var b = w.querySelector(".fo-user-trigger, .fo-notify-trigger");
            if (b) b.setAttribute("aria-expanded", "false");
        });
    };

    if (!window.__foNavUiBound) {
        window.__foNavUiBound = true;
        document.addEventListener("click", function () {
            window.closeFoodOrderNavDropdowns();
        });
        document.addEventListener("keydown", function (e) {
            if (e.key === "Escape") window.closeFoodOrderNavDropdowns();
        });
    }

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

    document.addEventListener("DOMContentLoaded", function () {
        var actions = document.querySelector(".navbar-actions");
        if (!actions || document.getElementById("fo-user-wrap")) return;

        var cluster = ensureNavbarIcons(actions);

        var wrap = document.createElement("div");
        wrap.id = "fo-user-wrap";
        wrap.className = "fo-dropdown-wrap fo-user-wrap";

        var btn = document.createElement("button");
        btn.type = "button";
        btn.className = "fo-icon-btn fo-user-trigger";
        btn.setAttribute("aria-expanded", "false");
        btn.setAttribute("aria-haspopup", "true");
        btn.title = "Tài khoản";
        btn.setAttribute("aria-label", "Mở menu tài khoản");
        btn.innerHTML = '<span class="fo-user-icon" aria-hidden="true">👤</span>';

        var panel = document.createElement("div");
        panel.className = "fo-dropdown-panel fo-user-panel";
        panel.setAttribute("role", "menu");

        btn.addEventListener("click", function (e) {
            e.stopPropagation();
            var opening = !wrap.classList.contains("is-open");
            window.closeFoodOrderNavDropdowns();
            if (opening) {
                wrap.classList.add("is-open");
                btn.setAttribute("aria-expanded", "true");
            }
        });

        wrap.appendChild(btn);
        wrap.appendChild(panel);
        cluster.appendChild(wrap);

        function menuLink(href, label, danger) {
            var a = document.createElement("a");
            a.className = "fo-menu-link" + (danger ? " is-danger" : "");
            a.href = href;
            a.textContent = label;
            a.setAttribute("role", "menuitem");
            return a;
        }

        function divider() {
            var d = document.createElement("div");
            d.className = "fo-menu-divider";
            return d;
        }

        function appendQuickSearch(panelEl) {
            if (document.body.dataset.page !== "home") return;
            var qs = document.createElement("button");
            qs.type = "button";
            qs.className = "fo-menu-btn";
            qs.textContent = "Tìm kiếm nhanh";
            qs.setAttribute("role", "menuitem");
            qs.addEventListener("click", function () {
                window.closeFoodOrderNavDropdowns();
                if (typeof window.toggleSearchBar === "function") window.toggleSearchBar();
            });
            panelEl.appendChild(qs);
        }

        function renderLoggedOut() {
            panel.innerHTML = "";
            var redir = encRedirect();
            panel.appendChild(menuLink("login.html?redirect=" + redir, "Đăng nhập"));
            panel.appendChild(menuLink("register.html?redirect=" + redir, "Đăng ký"));
            panel.appendChild(divider());
            panel.appendChild(menuLink("CartServlet?view=cart", "Giỏ hàng"));
            panel.appendChild(menuLink("VoucherServlet", "Voucher"));
            panel.appendChild(divider());
            panel.appendChild(menuLink("index.html#about", "Giới thiệu"));
            panel.appendChild(menuLink("index.html#contact", "Liên hệ"));
            appendQuickSearch(panel);
        }

        function renderLoggedIn(s) {
            panel.innerHTML = "";
            var name = (s.fullName && s.fullName !== "null") ? s.fullName : (s.username || "Bạn");
            var head = document.createElement("div");
            head.className = "fo-user-panel-head";
            var strong = document.createElement("strong");
            strong.textContent = name;
            head.appendChild(strong);
            if (s.username && s.username !== "null" && s.username !== name) {
                var sub = document.createElement("span");
                sub.className = "fo-user-panel-sub";
                sub.textContent = s.username;
                head.appendChild(sub);
            }
            panel.appendChild(head);
            panel.appendChild(divider());
            panel.appendChild(menuLink("CartServlet?view=cart", "Giỏ hàng"));
            panel.appendChild(menuLink("CartServlet?view=tracking", "Đơn của tôi"));
            panel.appendChild(menuLink("VoucherServlet", "Voucher"));
            panel.appendChild(divider());
            panel.appendChild(menuLink("index.html#about", "Giới thiệu"));
            panel.appendChild(menuLink("index.html#contact", "Liên hệ"));
            appendQuickSearch(panel);
            panel.appendChild(divider());
            panel.appendChild(menuLink("AuthServlet?action=logout", "Đăng xuất", true));
        }

        fetch("AuthServlet?action=status", { headers: { Accept: "application/json" } })
            .then(function (r) { return r.json(); })
            .then(function (s) {
                if (s.loggedIn) renderLoggedIn(s);
                else renderLoggedOut();
            })
            .catch(function () {
                renderLoggedOut();
            });

        wrap.addEventListener("click", function (e) {
            if (e.target.closest("a")) window.closeFoodOrderNavDropdowns();
        });
    });

    function encRedirect() {
        try {
            var q = window.location.search || "";
            var pathname = window.location.pathname || "/";
            var segs = pathname.split("/").filter(Boolean);
            if (segs.length >= 2) {
                pathname = "/" + segs.slice(1).join("/");
            } else if (segs.length === 1 && !segs[0].includes(".")) {
                pathname = "/index.html";
            }
            return encodeURIComponent(pathname + q);
        } catch (e) {
            return encodeURIComponent("/index.html");
        }
    }
})();
