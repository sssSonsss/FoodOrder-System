/* ==============================================
   food-detail.js – Chi Tiết Món Ăn
   ============================================== */

let currentFood = null;
let quantity = 1;
let isFavorite = false;

const CAT_EMOJI = {
    'Đồ uống':'🧋','Bánh mì':'🥖','Cơm':'🍚',
    'Bún & Phở':'🍜','Chay':'🥗','Tráng miệng':'🍮',
};

function getCatEmoji(name) {
    for (const k in CAT_EMOJI) { if (name && name.includes(k)) return CAT_EMOJI[k]; }
    return '🍽️';
}

// ===== INIT =====
window.addEventListener('DOMContentLoaded', function () {
    const params = new URLSearchParams(window.location.search);
    const id = parseInt(params.get('id'));

    if (!id || isNaN(id)) {
        showError();
        return;
    }

    loadFoodDetail(id);
});

// ===== LOAD FOOD DETAIL =====
function loadFoodDetail(id) {
    fetch(`FoodServlet?id=${id}`)
        .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(data => {
            if (data.error) { showError(); return; }
            currentFood = data;
            renderDetail(data);
            loadRelatedFoods(data.category_id, data.id);
        })
        .catch(() => {
            showError();
        });
}

// ===== RENDER DETAIL =====
function renderDetail(f) {
    // Update page title
    document.title = `${f.name} – FoodOrder`;

    const emoji = getCatEmoji(f.category_name);

    // Image
    const imgEl = document.getElementById('detail-img');
    if (f.image_url) {
        imgEl.innerHTML = `<img src="${escHtml(f.image_url)}" alt="${escHtml(f.name)}"
            onerror="this.parentElement.innerHTML='<span style=\\'font-size:7rem;\\'>${emoji}</span>'">`;
    } else {
        imgEl.innerHTML = `<span style="font-size:7rem;">${emoji}</span>`;
    }

    // Badge
    const badge = document.getElementById('detail-badge');
    if (f.rating >= 4.8) {
        badge.textContent = '🔥 HOT';
        badge.classList.add('show');
    }

    // Category
    document.getElementById('detail-cat').textContent = f.category_name || '';

    // Name
    document.getElementById('detail-name').textContent = f.name;

    // Rating stars
    const starsEl = document.getElementById('detail-stars');
    starsEl.innerHTML = '';
    for (let i = 1; i <= 5; i++) {
        const s = document.createElement('span');
        s.textContent = '★';
        if (i <= Math.round(f.rating)) s.classList.add('filled');
        starsEl.appendChild(s);
    }
    document.getElementById('detail-rating-num').textContent = f.rating.toFixed(1);
    document.getElementById('detail-reviews').textContent = `(${f.review_count.toLocaleString('vi-VN')} đánh giá)`;

    // Meta chip
    document.getElementById('detail-cat-chip').textContent = `${emoji} ${f.category_name}`;

    // Price
    document.getElementById('detail-price').textContent = formatMoney(f.price);

    // Description
    document.getElementById('detail-desc').textContent = f.description || 'Món ăn thơm ngon, chế biến từ nguyên liệu tươi sạch mỗi ngày.';

    // Favorite state
    const favs = JSON.parse(localStorage.getItem('food_favs') || '[]');
    isFavorite = favs.includes(f.id);
    document.getElementById('detail-fav').textContent = isFavorite ? '❤️' : '🤍';
    if (isFavorite) document.getElementById('detail-fav').classList.add('active');

    // Show content
    document.getElementById('detail-skeleton').style.display = 'none';
    document.getElementById('detail-content').style.display = 'grid';
}

function showError() {
    document.getElementById('detail-skeleton').style.display = 'none';
    document.getElementById('detail-content').style.display  = 'none';
    document.getElementById('detail-error').style.display    = 'block';
}

// ===== QUANTITY =====
function changeQty(delta) {
    quantity = Math.max(1, quantity + delta);
    document.getElementById('qty-val').textContent = quantity;
}

// ===== FAVORITE =====
function toggleFavorite() {
    if (!currentFood) return;
    const favs = JSON.parse(localStorage.getItem('food_favs') || '[]');
    const btn = document.getElementById('detail-fav');

    if (isFavorite) {
        isFavorite = false;
        btn.textContent = '🤍';
        btn.classList.remove('active');
        localStorage.setItem('food_favs', JSON.stringify(favs.filter(x => x !== currentFood.id)));
        showToast('Đã bỏ yêu thích');
    } else {
        isFavorite = true;
        btn.textContent = '❤️';
        btn.classList.add('active');
        favs.push(currentFood.id);
        localStorage.setItem('food_favs', JSON.stringify(favs));
        showToast('❤️ Đã thêm vào yêu thích', 'success');
    }
}

// ===== ADD TO CART =====
function addToCart() {
    if (!currentFood) return;
    const cart = JSON.parse(localStorage.getItem('food_cart') || '[]');
    const ex = cart.find(x => x.id === currentFood.id);
    if (ex) ex.quantity += quantity;
    else cart.push({ id: currentFood.id, name: currentFood.name, price: currentFood.price, quantity });
    localStorage.setItem('food_cart', JSON.stringify(cart));
    showToast(`🛒 Đã thêm ${quantity}x "${currentFood.name}" vào giỏ`, 'success');

    // Animate button
    const btn = document.getElementById('btn-add-cart');
    btn.textContent = '✓ Đã thêm!';
    setTimeout(() => { btn.textContent = '🛒 Thêm vào giỏ'; }, 2000);
}

// ===== RELATED FOODS =====
function loadRelatedFoods(categoryId, excludeId) {
    fetch(`FoodServlet?category=${categoryId}`)
        .then(r => r.json())
        .then(data => {
            const related = data.filter(f => f.id !== excludeId).slice(0, 4);
            if (related.length === 0) return;

            const section = document.getElementById('related-section');
            const grid    = document.getElementById('related-grid');
            section.style.display = 'block';
            grid.innerHTML = '';
            related.forEach(f => grid.appendChild(createRelatedCard(f)));
        })
        .catch(() => {});
}

function createRelatedCard(f) {
    const card = document.createElement('div');
    card.className = 'food-card';
    card.id = `related-card-${f.id}`;
    card.onclick = () => { window.location.href = `food-detail.html?id=${f.id}`; };
    const emoji = getCatEmoji(f.category_name);

    card.innerHTML = `
        <div class="food-card__img">
            ${f.image_url
                ? `<img src="${escHtml(f.image_url)}" alt="${escHtml(f.name)}" loading="lazy"
                    onerror="this.parentElement.innerHTML='<span style=\\'font-size:3rem;\\'>${emoji}</span>'">`
                : `<span style="font-size:3rem;">${emoji}</span>`}
        </div>
        <div class="food-card__body">
            <div class="food-card__cat">${escHtml(f.category_name || '')}</div>
            <div class="food-card__name">${escHtml(f.name)}</div>
            <div class="food-card__footer">
                <div class="food-card__price">${formatMoney(f.price)}</div>
                <div class="food-card__rating">
                    <span class="star">★</span> ${f.rating.toFixed(1)}
                </div>
            </div>
        </div>`;
    return card;
}

// ===== HELPERS =====
function formatMoney(v) { return Number(v).toLocaleString('vi-VN') + 'đ'; }
function escHtml(s) {
    if (!s) return '';
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function showToast(msg, type = '') {
    const t = document.getElementById('toast');
    t.textContent = msg; t.className = 'toast show ' + type;
    setTimeout(() => { t.className = 'toast'; }, 3000);
}

