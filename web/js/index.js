/* ==============================================
   index.js – Trang Chủ (Homepage)
   ============================================== */

// ===== CATEGORY EMOJI MAP =====
const CAT_EMOJI = {
    'Đồ uống':     '🧋',
    'Bánh mì':     '🥖',
    'Cơm':         '🍚',
    'Bún & Phở':   '🍜',
    'Chay':        '🥗',
    'Tráng miệng': '🍮',
};

function getCatEmoji(name) {
    for (const key in CAT_EMOJI) {
        if (name && name.includes(key)) return CAT_EMOJI[key];
    }
    return '🍽️';
}

function dedupeCategories(arr) {
    const seenId = new Set();
    const seenName = new Set();
    const out = [];
    for (const c of arr || []) {
        if (!c) continue;
        const id = Number(c.id);
        if (!Number.isFinite(id)) continue;
        const idKey = String(id);
        if (seenId.has(idKey)) continue;
        const nameKey = String(c.name || '').trim().toLowerCase();
        if (nameKey && seenName.has(nameKey)) continue;
        seenId.add(idKey);
        if (nameKey) seenName.add(nameKey);
        out.push(Object.assign({}, c, { id }));
    }
    return out;
}


// ===== INIT =====
window.addEventListener('DOMContentLoaded', function () {
    loadCategories();
    loadFeaturedFoods();
    initSearchSuggestions();
    animateStats();
});


// ===== LOAD CATEGORIES =====
function loadCategories() {
    fetch('CategoryServlet')
        .then(res => {
            if (!res.ok) throw new Error('HTTP ' + res.status);
            return res.json();
        })
        .then(data => renderCategories(dedupeCategories(data)))
        .catch(err => {
            console.error('CategoryServlet error:', err);
            const grid = document.getElementById('category-grid');
            if (grid) {
                grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:red;font-weight:bold;">Không thể lấy dữ liệu từ Database. Lỗi: ${err.message}. Vui lòng kiểm tra lại server (Tomcat) và DB.</p>`;
            }
        });
}

function renderCategories(categories) {
    const grid = document.getElementById('category-grid');
    grid.innerHTML = '';

    if (!categories || categories.length === 0) {
        grid.innerHTML = '<p style="grid-column:1/-1;text-align:center;color:var(--text-light)">Chưa có danh mục nào.</p>';
        return;
    }

    categories.forEach(c => {
        const card = document.createElement('a');
        card.className = 'cat-card';
        card.href = `menu.html?category=${c.id}`;
        card.id = `cat-card-${c.id}`;
        card.setAttribute('data-category-id', c.id);
        card.innerHTML = `
            <div class="cat-icon">${getCatEmoji(c.name)}</div>
            <div class="cat-name">${escHtml(c.name)}</div>
            <div class="cat-count">${c.item_count || 0} món</div>
        `;
        grid.appendChild(card);
    });
}




// ===== LOAD FEATURED FOODS =====
function loadFeaturedFoods() {
    fetch('FoodServlet?featured=6')
        .then(res => {
            if (!res.ok) throw new Error('HTTP ' + res.status);
            return res.json();
        })
        .then(data => renderFeaturedFoods(data))
        .catch(err => {
            console.error('FoodServlet error:', err);
            const grid = document.getElementById('featured-grid');
            if (grid) {
                grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:red;font-weight:bold;">Lỗi kết nối tới Server lấy món ăn: ${err.message}</p>`;
            }
        });
}

function renderFeaturedFoods(foods) {
    const grid = document.getElementById('featured-grid');
    grid.innerHTML = '';

    if (!foods || foods.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column:1/-1">
                <div class="empty-state__icon">🍽️</div>
                <h3>Chưa có món ăn nào</h3>
                <p>Hãy thêm dữ liệu vào database để xem kết quả.</p>
            </div>`;
        return;
    }

    foods.forEach(f => {
        grid.appendChild(createFoodCard(f));
    });

    // Cập nhật stat
    const statEl = document.getElementById('stat-foods');
    if (statEl) statEl.textContent = foods.length + '+';
}




// ===== CREATE FOOD CARD =====
function createFoodCard(f) {
    const card = document.createElement('div');
    card.className = 'food-card';
    card.id = `food-card-${f.id}`;
    card.onclick = () => openFoodDetail(f.id);

    const emoji = getFoodEmoji(f.category_name);
    const stars  = renderStars(f.rating);

    card.innerHTML = `
        <div class="food-card__img">
            ${f.image_url
                ? `<img src="${escHtml(f.image_url)}" alt="${escHtml(f.name)}" onerror="this.parentElement.innerHTML='<span style=\\'font-size:3.5rem;line-height:1;\\'>${emoji}</span>'">`
                : `<span style="font-size:3.5rem;line-height:1;">${emoji}</span>`
            }
            <div class="food-card__badge ${f.rating >= 4.8 ? 'hot' : ''}">
                ${f.rating >= 4.8 ? '🔥 HOT' : 'NEW'}
            </div>
            <button class="food-card__fav" onclick="event.stopPropagation(); toggleFav(this, ${f.id})"
                    id="fav-btn-${f.id}" title="Yêu thích">🤍</button>
        </div>
        <div class="food-card__body">
            <div class="food-card__cat">${escHtml(f.category_name || '')}</div>
            <div class="food-card__name">${escHtml(f.name)}</div>
            <div class="food-card__desc">${escHtml(f.description || '')}</div>
            <div class="food-card__footer">
                <div>
                    <div class="food-card__price">${formatMoney(f.price)}</div>
                    <div class="food-card__rating">
                        <span class="star">★</span>
                        ${f.rating.toFixed(1)} (${f.review_count})
                    </div>
                </div>
                <button class="food-card__add"
                    onclick="event.stopPropagation(); quickAddToCart(${f.id}, '${escAttr(f.name)}', ${f.price})"
                    id="add-btn-${f.id}" title="Thêm vào giỏ">+</button>
            </div>
        </div>
    `;
    return card;
}

function getFoodEmoji(catName) {
    if (!catName) return '🍽️';
    if (catName.includes('uống') || catName.includes('Trà') || catName.includes('Cà phê')) return '🧋';
    if (catName.includes('Bánh mì')) return '🥖';
    if (catName.includes('Cơm')) return '🍚';
    if (catName.includes('Bún') || catName.includes('Phở')) return '🍜';
    if (catName.includes('Chay')) return '🥗';
    if (catName.includes('Tráng miệng') || catName.includes('Chè')) return '🍮';
    return '🍽️';
}


// ===== SEARCH =====
let allFoodsCache = [];

function initSearchSuggestions() {
    fetch('FoodServlet')
        .then(res => res.json())
        .then(data => { allFoodsCache = data; })
        .catch(() => {});

    const input = document.getElementById('hero-search-input');
    if (input) {
        input.addEventListener('blur', () => {
            setTimeout(() => {
                const sug = document.getElementById('hero-suggestions');
                if (sug) sug.classList.remove('open');
            }, 200);
        });
    }
}

function handleHeroSearch(e) {
    if (e.key === 'Enter') { doHeroSearch(); return; }

    const q = document.getElementById('hero-search-input').value.trim().toLowerCase();
    const sug = document.getElementById('hero-suggestions');

    if (!q || q.length < 2) { sug.classList.remove('open'); return; }

    const matches = allFoodsCache
        .filter(f => f.name.toLowerCase().includes(q))
        .slice(0, 5);

    if (matches.length === 0) { sug.classList.remove('open'); return; }

    sug.innerHTML = matches.map(f =>
        `<div class="suggestion-item" id="sug-${f.id}" onclick="openFoodDetail(${f.id})">
            <span>${getFoodEmoji(f.category_name)}</span>
            <span>${escHtml(f.name)}</span>
            <span style="margin-left:auto;color:var(--primary);font-weight:700">${formatMoney(f.price)}</span>
        </div>`
    ).join('');

    sug.classList.add('open');
}

function doHeroSearch() {
    const q = document.getElementById('hero-search-input').value.trim();
    if (!q) return;
    window.location.href = `menu.html?search=${encodeURIComponent(q)}`;
}

// Navbar quick search
function toggleSearchBar() {
    const bar = document.getElementById('quick-search-bar');
    bar.classList.toggle('open');
    if (bar.classList.contains('open')) {
        setTimeout(() => document.getElementById('quick-search-input').focus(), 300);
    }
}

function doQuickSearch() {
    const q = document.getElementById('quick-search-input').value.trim();
    if (!q) return;
    window.location.href = `menu.html?search=${encodeURIComponent(q)}`;
}


// ===== PROMO CODE =====
function copyCode() {
    navigator.clipboard.writeText('NEWUSER').then(() => {
        showToast('✅ Đã copy mã NEWUSER!', 'success');
        document.getElementById('btn-copy-code').textContent = '✓ Copied!';
        setTimeout(() => {
            document.getElementById('btn-copy-code').textContent = '📋 Copy';
        }, 2000);
    }).catch(() => {
        showToast('Mã voucher: NEWUSER', 'success');
    });
}


// ===== FAVORITES =====
function toggleFav(btn, foodId) {
    const favs = getFavs();
    if (favs.includes(foodId)) {
        btn.textContent = '🤍';
        removeFav(foodId);
        showToast('Đã bỏ yêu thích', '');
    } else {
        btn.textContent = '❤️';
        addFav(foodId);
        showToast('❤️ Đã thêm vào yêu thích', 'success');
    }
}

function getFavs() {
    return JSON.parse(localStorage.getItem('food_favs') || '[]');
}

function addFav(id) {
    const favs = getFavs();
    if (!favs.includes(id)) { favs.push(id); }
    localStorage.setItem('food_favs', JSON.stringify(favs));
}

function removeFav(id) {
    const favs = getFavs().filter(x => x !== id);
    localStorage.setItem('food_favs', JSON.stringify(favs));
}


// ===== OPEN FOOD DETAIL =====
function openFoodDetail(foodId) {
    window.location.href = `food-detail.html?id=${foodId}`;
}


// ===== QUICK ADD TO CART =====
function quickAddToCart(id, name, price) {
    const cart = getCart();
    const existing = cart.find(x => x.id === id);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ id, name, price, quantity: 1 });
    }
    localStorage.setItem('food_cart', JSON.stringify(cart));
    showToast(`🛒 Đã thêm "${name}" vào giỏ`, 'success');
}

function getCart() {
    return JSON.parse(localStorage.getItem('food_cart') || '[]');
}


// ===== STATS ANIMATION =====
function animateStats() {
    // Simple visibility check - could enhance with IntersectionObserver
}


// ===== HELPERS =====
function formatMoney(amount) {
    return Number(amount).toLocaleString('vi-VN') + 'đ';
}

function renderStars(rating) {
    let html = '';
    for (let i = 1; i <= 5; i++) {
        html += `<span class="${i <= Math.round(rating) ? 'filled' : ''}">★</span>`;
    }
    return html;
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function escAttr(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

function showToast(msg, type = '') {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.className = 'toast show ' + type;
    setTimeout(() => { toast.className = 'toast'; }, 3000);
}
