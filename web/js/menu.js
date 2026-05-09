/* ==============================================
   menu.js – Trang Danh Sách Món Ăn
   ============================================== */

// ===== STATE =====
let allFoods = [];
let filteredFoods = [];
let categories = [];

let activeCategory = 0;
let activeSearch = '';
let activeSort = '';
let activeMinPrice = 0;
let activeMaxPrice = 0;
let activeRating = 0;
let currentView = 'grid';
let currentPage = 1;
const PAGE_SIZE = 12;

const CAT_EMOJI = {
    'Đồ uống': '🧋', 'Bánh mì': '🥖', 'Cơm': '🍚',
    'Bún & Phở': '🍜', 'Chay': '🥗', 'Tráng miệng': '🍮',
};

function getCatEmoji(name) {
    for (const k in CAT_EMOJI) { if (name && name.includes(k)) return CAT_EMOJI[k]; }
    return '🍽️';
}

// ===== INIT =====
window.addEventListener('DOMContentLoaded', function () {
    readUrlParams();
    loadCategories();
    loadFoods();
});

function readUrlParams() {
    const p = new URLSearchParams(window.location.search);
    if (p.get('category')) activeCategory = parseInt(p.get('category')) || 0;
    if (p.get('search'))   activeSearch   = p.get('search');
    if (p.get('sort'))     activeSort     = p.get('sort');

    const si = document.getElementById('search-input');
    const ss = document.getElementById('sort-select');
    if (si && activeSearch) si.value = activeSearch;
    if (ss && activeSort)   ss.value = activeSort;
}

// ===== LOAD CATEGORIES =====
function loadCategories() {
    fetch('CategoryServlet')
        .then(r => r.json())
        .then(data => { categories = data; renderCatFilter(data); })
        .catch(err => {
            console.error('CategoryServlet error:', err);
            const list = document.getElementById('cat-filter-list');
            if (list) {
                list.innerHTML = `<p style="color:red;font-weight:bold;">Lỗi kết nối Database: ${err.message}</p>`;
            }
        });
}

function renderCatFilter(cats) {
    const list = document.getElementById('cat-filter-list');
    list.innerHTML = '';

    // "Tất cả" item
    const all = document.createElement('div');
    all.className = 'cat-filter-item' + (activeCategory === 0 ? ' active' : '');
    all.id = 'cat-filter-all';
    all.onclick = () => selectCategory(0);
    all.innerHTML = `<span class="cat-filter-icon">🍽️</span> Tất cả`;
    list.appendChild(all);

    cats.forEach(c => {
        const item = document.createElement('div');
        item.className = 'cat-filter-item' + (activeCategory === c.id ? ' active' : '');
        item.id = `cat-filter-${c.id}`;
        item.onclick = () => selectCategory(c.id);
        item.innerHTML = `
            <span class="cat-filter-icon">${getCatEmoji(c.name)}</span>
            ${escHtml(c.name)}`;
        list.appendChild(item);
    });
}



function selectCategory(catId) {
    activeCategory = catId;
    currentPage = 1;
    document.querySelectorAll('.cat-filter-item').forEach(el => el.classList.remove('active'));
    const target = document.getElementById(catId === 0 ? 'cat-filter-all' : `cat-filter-${catId}`);
    if (target) target.classList.add('active');
    applyFilters();
}

// ===== LOAD FOODS =====
function loadFoods() {
    renderFoodSkeleton();

    const params = new URLSearchParams();
    if (activeSearch)   params.set('search',   activeSearch);
    if (activeCategory) params.set('category', activeCategory);
    if (activeSort)     params.set('sort',      activeSort);

    fetch('FoodServlet?' + params.toString())
        .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(data => {
            allFoods = data;
            filteredFoods = [...data];
            applyClientFilters();
            renderFoods();
        })
        .catch(err => {
            console.error('FoodServlet error:', err);
            const grid = document.getElementById('food-grid');
            if (grid) {
                grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:red;font-weight:bold;">Lỗi lấy dữ liệu món ăn từ DB: ${err.message}</p>`;
            }
        });
}

// ===== APPLY FILTERS (CLIENT-SIDE) =====
function applyFilters() {
    // Re-fetch with server params (search + category + sort)
    loadFoods();
}

function applyClientFilters() {
    filteredFoods = allFoods.filter(f => {
        if (activeMinPrice > 0 && f.price < activeMinPrice) return false;
        if (activeMaxPrice > 0 && f.price > activeMaxPrice) return false;
        if (activeRating > 0  && f.rating < activeRating)  return false;
        return true;
    });
}

// ===== SEARCH =====
let searchTimer = null;
function handleSearchInput() {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => doSearch(), 500);
}

function handleSearchKeyup(e) {
    if (e.key === 'Enter') doSearch();
}

function doSearch() {
    activeSearch = document.getElementById('search-input').value.trim();
    currentPage = 1;
    loadFoods();
    updateResultTags();
}

// ===== SORT =====
function doSort() {
    activeSort = document.getElementById('sort-select').value;
    currentPage = 1;
    loadFoods();
}

// ===== PRICE FILTER =====
function applyPriceFilter() {
    const minEl = document.getElementById('price-min');
    const maxEl = document.getElementById('price-max');
    activeMinPrice = parseFloat(minEl.value) || 0;
    activeMaxPrice = parseFloat(maxEl.value) || 0;
    currentPage = 1;
    applyClientFilters();
    renderFoods();
    updateResultTags();
}

// ===== RATING FILTER =====
function filterByRating(minRating) {
    activeRating = minRating;
    currentPage = 1;
    document.querySelectorAll('.rating-option').forEach(el => el.classList.remove('active'));
    const map = { 0: 'rating-all', 4: 'rating-4', 4.5: 'rating-45' };
    const el = document.getElementById(map[minRating]);
    if (el) el.classList.add('active');
    applyClientFilters();
    renderFoods();
}

// ===== VIEW TOGGLE =====
function setView(view) {
    currentView = view;
    const grid = document.getElementById('food-grid');
    const btnGrid = document.getElementById('btn-grid-view');
    const btnList = document.getElementById('btn-list-view');

    if (view === 'grid') {
        grid.classList.remove('list-view');
        btnGrid.classList.add('active');
        btnList.classList.remove('active');
    } else {
        grid.classList.add('list-view');
        btnList.classList.add('active');
        btnGrid.classList.remove('active');
    }
}

// ===== RENDER FOODS =====
function renderFoods() {
    const grid = document.getElementById('food-grid');
    grid.innerHTML = '';

    const start = (currentPage - 1) * PAGE_SIZE;
    const page  = filteredFoods.slice(start, start + PAGE_SIZE);

    // Result info
    const countEl = document.getElementById('result-count');
    if (countEl) {
        const total = filteredFoods.length;
        countEl.textContent = `Hiển thị ${Math.min(start + 1, total)}–${Math.min(start + PAGE_SIZE, total)} / ${total} món ăn`;
    }

    if (filteredFoods.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column:1/-1">
                <div class="empty-state__icon">🔍</div>
                <h3>Không tìm thấy món ăn</h3>
                <p>Thử thay đổi từ khóa hoặc bộ lọc khác nhé.</p>
            </div>`;
        renderPagination(0);
        return;
    }

    page.forEach(f => grid.appendChild(createFoodCard(f)));
    renderPagination(filteredFoods.length);
}

function renderFoodSkeleton() {
    const grid = document.getElementById('food-grid');
    grid.innerHTML = '';
    for (let i = 0; i < 8; i++) {
        const el = document.createElement('div');
        el.className = 'food-card';
        el.id = `skeleton-card-${i}`;
        el.style.cssText = 'min-height:300px';
        el.innerHTML = `
            <div class="skeleton" style="height:180px;border-radius:14px 14px 0 0"></div>
            <div style="padding:16px;display:flex;flex-direction:column;gap:10px">
                <div class="skeleton" style="height:14px;width:50%;border-radius:6px"></div>
                <div class="skeleton" style="height:20px;width:80%;border-radius:6px"></div>
                <div class="skeleton" style="height:14px;width:60%;border-radius:6px"></div>
                <div class="skeleton" style="height:40px;border-radius:6px;margin-top:6px"></div>
            </div>`;
        grid.appendChild(el);
    }
}

// ===== FOOD CARD =====
function createFoodCard(f) {
    const card = document.createElement('div');
    card.className = 'food-card';
    card.id = `food-card-${f.id}`;
    card.onclick = () => window.location.href = `food-detail.html?id=${f.id}`;

    const emoji = getCatEmoji(f.category_name);

    card.innerHTML = `
        <div class="food-card__img">
            ${f.image_url
                ? `<img src="${escHtml(f.image_url)}" alt="${escHtml(f.name)}" loading="lazy"
                        onerror="this.parentElement.innerHTML='<span style=\\'font-size:3rem;\\'>${emoji}</span>'">`
                : `<span style="font-size:3rem;">${emoji}</span>`}
            ${f.rating >= 4.8 ? '<div class="food-card__badge hot">🔥 HOT</div>' : ''}
            <button class="food-card__fav" id="fav-menu-${f.id}"
                onclick="event.stopPropagation(); toggleFav(this, ${f.id})">🤍</button>
        </div>
        <div class="food-card__body">
            <div class="food-card__cat">${escHtml(f.category_name || '')}</div>
            <div class="food-card__name">${escHtml(f.name)}</div>
            <div class="food-card__desc">${escHtml(f.description || '')}</div>
            <div class="food-card__footer">
                <div>
                    <div class="food-card__price">${formatMoney(f.price)}</div>
                    <div class="food-card__rating">
                        <span class="star">★</span> ${f.rating.toFixed(1)} (${f.review_count})
                    </div>
                </div>
                <button class="food-card__add" id="add-cart-${f.id}"
                    onclick="event.stopPropagation(); quickAddToCart(${f.id}, '${escAttr(f.name)}', ${f.price})">+</button>
            </div>
        </div>`;
    return card;
}

// ===== PAGINATION =====
function renderPagination(total) {
    const pg = document.getElementById('pagination');
    pg.innerHTML = '';
    const totalPages = Math.ceil(total / PAGE_SIZE);
    if (totalPages <= 1) return;

    const addBtn = (label, page, disabled, active) => {
        const btn = document.createElement('button');
        btn.className = 'page-btn' + (active ? ' active' : '');
        btn.textContent = label;
        btn.id = `page-btn-${page}`;
        if (disabled) btn.disabled = true;
        btn.onclick = () => { currentPage = page; renderFoods(); window.scrollTo({top:300, behavior:'smooth'}); };
        pg.appendChild(btn);
    };

    addBtn('«', 1, currentPage === 1, false);
    addBtn('‹', currentPage - 1, currentPage === 1, false);

    for (let i = Math.max(1, currentPage - 2); i <= Math.min(totalPages, currentPage + 2); i++) {
        addBtn(i, i, false, i === currentPage);
    }

    addBtn('›', currentPage + 1, currentPage === totalPages, false);
    addBtn('»', totalPages, currentPage === totalPages, false);
}

// ===== RESULT TAGS =====
function updateResultTags() {
    const tagsEl = document.getElementById('result-tags');
    if (!tagsEl) return;
    tagsEl.innerHTML = '';

    if (activeSearch) {
        tagsEl.innerHTML += `<span class="result-tag">🔍 "${escHtml(activeSearch)}"
            <button onclick="clearSearch()">×</button></span>`;
    }
    if (activeMinPrice || activeMaxPrice) {
        const label = (activeMinPrice ? formatMoney(activeMinPrice) : '0') + ' – ' + (activeMaxPrice ? formatMoney(activeMaxPrice) : '∞');
        tagsEl.innerHTML += `<span class="result-tag">💰 ${label}
            <button onclick="clearPrice()">×</button></span>`;
    }
}

function clearSearch() {
    activeSearch = '';
    document.getElementById('search-input').value = '';
    currentPage = 1;
    loadFoods();
    updateResultTags();
}

function clearPrice() {
    activeMinPrice = 0; activeMaxPrice = 0;
    document.getElementById('price-min').value = '';
    document.getElementById('price-max').value = '';
    applyClientFilters();
    renderFoods();
    updateResultTags();
}

// ===== FAVORITES =====
function toggleFav(btn, foodId) {
    const favs = JSON.parse(localStorage.getItem('food_favs') || '[]');
    if (favs.includes(foodId)) {
        btn.textContent = '🤍';
        localStorage.setItem('food_favs', JSON.stringify(favs.filter(x => x !== foodId)));
        showToast('Đã bỏ yêu thích');
    } else {
        btn.textContent = '❤️';
        favs.push(foodId);
        localStorage.setItem('food_favs', JSON.stringify(favs));
        showToast('❤️ Đã thêm yêu thích', 'success');
    }
}

// ===== CART =====
function quickAddToCart(id, name, price) {
    fetch("CartServlet", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            action: "add",
            foodId: String(id),
            quantity: "1"
        }).toString()
    })
    .then(r => r.json().then(data => ({ status: r.status, data })))
    .then(({ status, data }) => {
        if (status === 401) {
            const path = window.location.pathname.split("/").filter(Boolean);
            const rel = path.length >= 2 ? "/" + path.slice(1).join("/") : window.location.pathname;
            window.location.href = "login.html?redirect=" + encodeURIComponent(rel + window.location.search);
            return;
        }
        if (data.error) {
            showToast(data.error, "error");
            return;
        }
        showToast(`🛒 Đã thêm "${name}"`, 'success');
    })
    .catch(() => {
        showToast("Không thể thêm món vào giỏ hàng", "error");
    });
}

// ===== HELPERS =====
function formatMoney(v) { return Number(v).toLocaleString('vi-VN') + 'đ'; }
function escHtml(s) {
    if (!s) return '';
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function escAttr(s) { return !s ? '' : String(s).replace(/'/g,"\\'"); }
function showToast(msg, type = '') {
    const t = document.getElementById('toast');
    t.textContent = msg; t.className = 'toast show ' + type;
    setTimeout(() => { t.className = 'toast'; }, 3000);
}


