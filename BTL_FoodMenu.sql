-- ==================================================
-- BÀI TẬP LỚN WEB - PostgreSQL
-- Database: laptrinhweb_btl_foodorder
-- ==================================================

-- Tạo DB (chạy dòng này với psql hoặc pgAdmin trước,
-- sau đó connect vào DB mới và chạy phần còn lại):
-- CREATE DATABASE laptrinhweb_btl_foodorder;

-- ==================================================
-- TẠO BẢNG DANH MỤC MÓN ĂN
-- ==================================================
CREATE TABLE IF NOT EXISTS categories (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    image_url   VARCHAR(500),
    is_active   BOOLEAN DEFAULT TRUE
);


-- ==================================================
-- TẠO BẢNG MÓN ĂN
-- ==================================================
CREATE TABLE IF NOT EXISTS foods (
    id           SERIAL PRIMARY KEY,
    category_id  INT NOT NULL REFERENCES categories(id),
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    price        NUMERIC(12,0) NOT NULL,
    image_url    VARCHAR(500),
    is_active    BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT NOW(),
    rating       NUMERIC(3,1) DEFAULT 0,
    review_count INT DEFAULT 0
);

-- ==================================================
-- NGƯỜI DÙNG & ĐỊA CHỈ GIAO HÀNG (đăng nhập / đăng ký)
-- ==================================================
CREATE TABLE IF NOT EXISTS users (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(80) NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(30),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_addresses (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label           VARCHAR(80),
    recipient_name  VARCHAR(120),
    phone           VARCHAR(30),
    address_line    VARCHAR(500) NOT NULL,
    province        VARCHAR(120),
    district        VARCHAR(120),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_addresses_user ON user_addresses(user_id);

-- Nâng cấp DB cũ: thêm cột đăng nhập trước khi seed / INSERT dùng username
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(80);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
UPDATE users SET password_hash = TRIM(password)
WHERE password_hash IS NULL
  AND password IS NOT NULL
  AND TRIM(password) LIKE '%$%';
UPDATE users SET username = COALESCE(NULLIF(TRIM(username), ''), 'user_' || id::text);
CREATE UNIQUE INDEX IF NOT EXISTS users_username_lower_idx ON users (LOWER(username));
UPDATE users SET username = 'demo',
    email = COALESCE(NULLIF(TRIM(email), ''), 'demo@foodorder.local'),
    password_hash = COALESCE(NULLIF(TRIM(password_hash), ''),
        'RcTBa42mEa3BJX1XQTqcgw==$Qf7ZAV+vEQlCisqIQYcVcwOMgvj3hFtaqdoFyrzEnzc='),
    full_name = COALESCE(NULLIF(TRIM(full_name), ''), 'Người dùng demo'),
    phone = COALESCE(NULLIF(TRIM(phone), ''), '0900000001')
WHERE id = 1;
INSERT INTO user_addresses (user_id, label, recipient_name, phone, address_line, province, district, is_default)
SELECT u.id, 'Nhà', COALESCE(NULLIF(TRIM(u.full_name), ''), 'Khách'), COALESCE(NULLIF(TRIM(u.phone), ''), '0900000001'),
       '123 Đường Demo, Phường Bến Nghé', 'TP.Hồ Chí Minh', 'Quận 1', TRUE
FROM users u WHERE u.id = 1
AND NOT EXISTS (SELECT 1 FROM user_addresses ua WHERE ua.user_id = u.id);

CREATE TABLE IF NOT EXISTS cart_items (
    id           SERIAL PRIMARY KEY,
    user_id      INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_id      INT NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    quantity     INT NOT NULL CHECK (quantity > 0),
    UNIQUE(user_id, food_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_items_user_id ON cart_items(user_id);

-- ==================================================
-- MODULE ĐƠN HÀNG & TRACKING
-- ==================================================
CREATE TABLE IF NOT EXISTS orders (
    id           SERIAL PRIMARY KEY,
    user_id      INT NOT NULL REFERENCES users(id),
    total_price  NUMERIC(12,0) NOT NULL DEFAULT 0,
    status       INT NOT NULL DEFAULT 0,
    address_id   INT NOT NULL,
    created_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    order_id     INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    food_id      INT NOT NULL REFERENCES foods(id),
    quantity     INT NOT NULL CHECK (quantity > 0),
    price        NUMERIC(12,0) NOT NULL CHECK (price >= 0),
    PRIMARY KEY (order_id, food_id)
);

CREATE TABLE IF NOT EXISTS order_status_logs (
    id           SERIAL PRIMARY KEY,
    order_id     INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status       INT NOT NULL,
    note         VARCHAR(255),
    update_time  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reviews (
    id           SERIAL PRIMARY KEY,
    order_id     INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id      INT NOT NULL REFERENCES users(id),
    food_id      INT NOT NULL REFERENCES foods(id),
    rating       INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      VARCHAR(500),
    created_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE(order_id, user_id, food_id)
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_status_logs_order_id ON order_status_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_reviews_food_id ON reviews(food_id);
CREATE TABLE IF NOT EXISTS notifications (
    id          SERIAL PRIMARY KEY,
    user_id     INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(150) NOT NULL,
    message     VARCHAR(500) NOT NULL,
    type        VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

-- Nâng cấp dữ liệu cũ sang status dạng số (nếu đang dùng text)
ALTER TABLE orders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE orders ALTER COLUMN status TYPE INT
USING (
    CASE
        WHEN status::text IN ('0','1','2','3','4') THEN status::text::INT
        WHEN status::text = 'CHO_XAC_NHAN' THEN 0
        WHEN status::text = 'DANG_CHUAN_BI' THEN 1
        WHEN status::text IN ('SHIPPER_DA_LAY','DANG_GIAO') THEN 2
        WHEN status::text = 'DA_GIAO_THANH_CONG' THEN 3
        WHEN status::text = 'DA_HUY' THEN 4
        ELSE 0
    END
);
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 0;

ALTER TABLE order_status_logs ADD COLUMN IF NOT EXISTS update_time TIMESTAMP DEFAULT NOW();

-- Ảnh món trong DB phải là URL ảnh trực tiếp (https://...jpg|png), không dùng link trang HTML làm src của <img>.
-- Tham khảo chủ đề (bài báo / trang giới thiệu — không phải file ảnh):
--   Bánh mì: https://www.tripadvisor.com.vn/Restaurant_Review-g293925-d17757797-Reviews-Banh_Mi_362-Ho_Chi_Minh_City.html
--   Bún phở: https://thanhnien.vn/9-quan-bun-pho-khong-the-bo-qua-o-ha-noi-trong-danh-sach-michelin-185250224152555628.htm
--   Cơm:     https://lalago.vn/com-tam-hoi-an/
--   Đồ chay: https://fptshop.com.vn/tin-tuc/dien-may/quan-chay-quan-8-167167
--   Trà sữa / đồ uống: https://simexcodl.com.vn/tra-sua-ca-phe/

-- ==================================================
-- DATA MẪU: DANH MỤC (ảnh HTTPS minh họa — hiển thị được mọi trang)
-- ==================================================
INSERT INTO categories (name, description, image_url) VALUES
('Đồ uống',    'Trà sữa, cà phê, sinh tố và các loại nước giải khát',   'https://upload.wikimedia.org/wikipedia/commons/1/18/Bubble_Tea.png'),
('Bánh mì',    'Bánh mì Việt Nam đa dạng nhân thơm ngon',                'https://upload.wikimedia.org/wikipedia/commons/5/5f/B%C3%A1nh_m%C3%AC_th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng_in_Saigon.jpg'),
('Cơm',        'Cơm văn phòng, cơm tấm, cơm gà các loại',                'https://upload.wikimedia.org/wikipedia/commons/8/8c/C%C6%A1m_t%E1%BA%A5m.jpg'),
('Bún & Phở',  'Phở bò, bún bò Huế, bún riêu và các loại bún nước',      'https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg'),
('Chay',       'Món ăn thuần chay, tốt cho sức khỏe',                     'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Vegetable_stir_fry_002.jpg/640px-Vegetable_stir_fry_002.jpg'),
('Tráng miệng','Chè, kem, bánh ngọt và các món tráng miệng hấp dẫn',     'https://upload.wikimedia.org/wikipedia/commons/4/48/Flan_Caramel.jpg');


-- ==================================================
-- DATA MẪU: MÓN ĂN
-- ==================================================

-- ===== ĐỒ UỐNG (category_id = 1) =====
INSERT INTO foods (category_id, name, description, price, image_url, rating, review_count) VALUES
(1, 'Trà sữa trân châu đen',
   'Trà sữa thơm béo pha vị truyền thống, thêm trân châu đen dai giòn đặc trưng.',
   35000, 'images/trasua_tranchau.jpg', 4.8, 320),

(1, 'Cà phê sữa đá',
   'Cà phê phin truyền thống pha với sữa đặc, uống cùng đá bào mịn.',
   25000, 'images/caphe_suada.jpg', 4.7, 512),

(1, 'Sinh tố bơ',
   'Bơ tươi xay mịn pha sữa đặc và đá, béo ngậy, bổ dưỡng.',
   40000, 'images/sinhtobơ.jpg', 4.6, 198),

(1, 'Nước ép cam tươi',
   'Cam vàng tươi ép lạnh, nguyên chất 100%, giàu vitamin C.',
   30000, 'images/nuocepcam.jpg', 4.5, 145),

(1, 'Trà đào cam sả',
   'Trà thảo mộc thanh mát kết hợp đào, cam, sả tươi, giải nhiệt hoàn hảo.',
   38000, 'images/tradao.jpg', 4.9, 276),

(1, 'Matcha latte đá',
   'Matcha Nhật Bản pha cùng sữa tươi không đường, béo mượt và thơm mát.',
   45000, 'images/matcha.jpg', 4.7, 189);


-- ===== BÁNH MÌ (category_id = 2) =====
INSERT INTO foods (category_id, name, description, price, image_url, rating, review_count) VALUES
(2, 'Bánh mì pate thịt',
   'Bánh mì giòn rụm nhân pate gan, chả lụa, thịt nguội và rau thơm tươi.',
   25000, 'images/banhmi_pate.jpg', 4.6, 430),

(2, 'Bánh mì trứng ốp la',
   'Trứng ốp vàng ruộm, thêm xúc xích, dưa cải giòn, tương ớt.',
   20000, 'images/banhmi_trung.jpg', 4.4, 287),

(2, 'Bánh mì gà xé sốt',
   'Gà xé phay ướp sốt đặc biệt, kết hợp dưa leo, cà rốt ngâm chua ngọt.',
   28000, 'images/banhmi_ga.jpg', 4.7, 365),

(2, 'Bánh mì bơ mật ong',
   'Bánh mì nóng phết bơ mặn và mật ong hoa nhãn, ngọt dịu buổi sáng.',
   15000, 'images/banhmi_bo.jpg', 4.3, 156);


-- ===== CƠM (category_id = 3) =====
INSERT INTO foods (category_id, name, description, price, image_url, rating, review_count) VALUES
(3, 'Cơm tấm sườn bì chả',
   'Sườn heo nướng than thơm, bì heo giòn, chả trứng hấp, ăn kèm mắm ngọt đặc trưng Nam Bộ.',
   55000, 'images/comtam.jpg', 4.9, 621),

(3, 'Cơm gà xối mỡ',
   'Gà ta chiên giòn xối mỡ nóng, ăn kèm cơm trắng và canh gà thanh ngọt.',
   50000, 'images/comga.jpg', 4.8, 448),

(3, 'Cơm văn phòng',
   'Set cơm 3 món đa dạng thay đổi theo ngày: thịt kho, canh rau, trứng chiên.',
   35000, 'images/comvanphong.jpg', 4.5, 312),

(3, 'Cơm chiên dương châu',
   'Cơm rang thập cẩm kiểu Trung Hoa với tôm, xúc xích, ngô, đậu Hà Lan.',
   40000, 'images/comchien.jpg', 4.6, 267);


-- ===== BÚN & PHỞ (category_id = 4) =====
INSERT INTO foods (category_id, name, description, price, image_url, rating, review_count) VALUES
(4, 'Phở bò tái nạm gầu',
   'Nước dùng hầm xương 8 tiếng, thịt bò tái, nạm, gầu mềm, thêm rau thơm và tương hoisin.',
   65000, 'images/phobo.jpg', 4.9, 753),

(4, 'Bún bò Huế',
   'Nước lèo cay nồng sả ớt, bún sợi lớn, thịt bò và chả cua đặc sản Huế.',
   55000, 'images/bunbo.jpg', 4.8, 589),

(4, 'Bún riêu cua đồng',
   'Bún riêu miền Bắc nấu từ cua đồng giã nhuyễn, cà chua, đậu phụ, mắm tôm.',
   50000, 'images/bunrieu.jpg', 4.7, 342),

(4, 'Mì Quảng tôm thịt',
   'Mì Quảng đặc trưng miền Trung, nước nhân đậm đà, tôm thẻ và thịt ba chỉ.',
   52000, 'images/miquang.jpg', 4.7, 415);


-- ===== MÓN CHAY (category_id = 5) =====
INSERT INTO foods (category_id, name, description, price, image_url, rating, review_count) VALUES
(5, 'Cơm chay thập cẩm',
   'Cơm trắng ăn kèm đậu phụ sốt cà, nấm xào, rau luộc và tương đậu nành.',
   40000, 'images/comchay.jpg', 4.6, 218),

(5, 'Bún chay nấm hương',
   'Nước dùng từ nấm và rau củ tự nhiên, bún sợi tươi, đậu phụ chiên vàng.',
   38000, 'images/bunchay.jpg', 4.5, 164),

(5, 'Gỏi cuốn chay',
   'Cuốn bánh tráng với bún, đậu phụ, cà rốt, dưa leo, rau thơm và tương đen.',
   28000, 'images/goicuon_chay.jpg', 4.4, 132);


-- ===== TRÁNG MIỆNG (category_id = 6) =====
INSERT INTO foods (category_id, name, description, price, image_url, rating, review_count) VALUES
(6, 'Chè bà ba',
   'Chè truyền thống Nam Bộ với khoai lang, khoai môn, chuối, nước cốt dừa thơm béo.',
   25000, 'images/chebaba.jpg', 4.7, 289),

(6, 'Kem tươi matcha',
   'Kem tươi làm từ matcha Nhật và sữa tươi nguyên chất, tan chảy mát lịm.',
   35000, 'images/kem_matcha.jpg', 4.8, 341),

(6, 'Bánh flan caramel',
   'Bánh flan mềm mịn từ trứng và sữa, rưới caramel cháy nhẹ thơm ngon.',
   20000, 'images/banh_flan.jpg', 4.6, 198),

(6, 'Chè thái đặc biệt',
   'Chè thái rực rỡ với thạch, trái cây nhiệt đới, nước cốt dừa và đá bào.',
   30000, 'images/chethai.jpg', 4.7, 276);

-- Gán lại ảnh món sau INSERT (đường images/*.jpg trong repo có thể không tồn tại khi deploy)
UPDATE foods SET image_url = CASE category_id
    WHEN 1 THEN 'https://upload.wikimedia.org/wikipedia/commons/1/18/Bubble_Tea.png'
    WHEN 2 THEN 'https://upload.wikimedia.org/wikipedia/commons/5/5f/B%C3%A1nh_m%C3%AC_th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng_in_Saigon.jpg'
    WHEN 3 THEN 'https://upload.wikimedia.org/wikipedia/commons/8/8c/C%C6%A1m_t%E1%BA%A5m.jpg'
    WHEN 4 THEN 'https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg'
    WHEN 5 THEN 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Vegetable_stir_fry_002.jpg/640px-Vegetable_stir_fry_002.jpg'
    WHEN 6 THEN 'https://upload.wikimedia.org/wikipedia/commons/4/48/Flan_Caramel.jpg'
    ELSE image_url
END;

UPDATE categories SET image_url = CASE name
    WHEN 'Đồ uống' THEN 'https://upload.wikimedia.org/wikipedia/commons/1/18/Bubble_Tea.png'
    WHEN 'Bánh mì' THEN 'https://upload.wikimedia.org/wikipedia/commons/5/5f/B%C3%A1nh_m%C3%AC_th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng_in_Saigon.jpg'
    WHEN 'Cơm' THEN 'https://upload.wikimedia.org/wikipedia/commons/8/8c/C%C6%A1m_t%E1%BA%A5m.jpg'
    WHEN 'Bún & Phở' THEN 'https://upload.wikimedia.org/wikipedia/commons/6/6d/Ph%E1%BB%9F_b%C3%B2.jpg'
    WHEN 'Chay' THEN 'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fc/Vegetable_stir_fry_002.jpg/640px-Vegetable_stir_fry_002.jpg'
    WHEN 'Tráng miệng' THEN 'https://upload.wikimedia.org/wikipedia/commons/4/48/Flan_Caramel.jpg'
    ELSE image_url
END;


-- ==================================================
-- KIỂM TRA DỮ LIỆU
-- ==================================================
SELECT 'categories' AS bang, COUNT(*) AS so_dong FROM categories
UNION ALL
SELECT 'foods', COUNT(*) FROM foods;

SELECT
    f.id,
    c.name  AS category,
    f.name,
    f.price,
    f.rating,
    f.review_count
FROM foods f
JOIN categories c ON f.category_id = c.id
ORDER BY f.id;


-- ==================================================
-- MODULE VOUCHER (mã giảm giá, ví người dùng, lịch sử dùng)
-- ==================================================
CREATE TABLE IF NOT EXISTS vouchers (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    title           VARCHAR(150) NOT NULL,
    description     VARCHAR(500),
    discount_type   VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENT','AMOUNT','FREESHIP')),
    discount_value  NUMERIC(12,2) NOT NULL DEFAULT 0,
    min_order_value NUMERIC(12,0) NOT NULL DEFAULT 0,
    max_discount_amount NUMERIC(12,0),
    usage_limit     INT,
    used_count      INT NOT NULL DEFAULT 0,
    expiry_date     DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW(),
    promo_scope     VARCHAR(30) DEFAULT 'ALL'
);

CREATE TABLE IF NOT EXISTS user_vouchers (
    id           SERIAL PRIMARY KEY,
    user_id      INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    voucher_id   INT NOT NULL REFERENCES vouchers(id) ON DELETE CASCADE,
    claimed_at   TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, voucher_id)
);

CREATE INDEX IF NOT EXISTS idx_user_vouchers_user ON user_vouchers(user_id);

-- Nếu DB cũ từng tạo user_vouchers không UNIQUE: chạy một lần để gỡ trùng trước khi thêm ràng buộc.
-- DELETE FROM user_vouchers a USING user_vouchers b
-- WHERE a.user_id = b.user_id AND a.voucher_id = b.voucher_id AND a.id > b.id;

CREATE TABLE IF NOT EXISTS voucher_usage (
    id               SERIAL PRIMARY KEY,
    user_id          INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    voucher_id       INT NOT NULL REFERENCES vouchers(id),
    order_id         INT REFERENCES orders(id) ON DELETE SET NULL,
    discount_amount  NUMERIC(12,0) NOT NULL DEFAULT 0,
    used_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voucher_usage_user ON voucher_usage(user_id);

-- Dữ liệu mẫu (chạy lại an toàn nhờ ON CONFLICT)
INSERT INTO vouchers (code, title, description, discount_type, discount_value, min_order_value,
                      max_discount_amount, usage_limit, expiry_date, is_active, promo_scope)
VALUES
('NEWUSER', 'Chào thành viên mới', 'Giảm phần trăm cho đơn từ 50k (demo).', 'PERCENT', 20, 50000, 40000, NULL, '2099-12-31', TRUE, 'FOOD'),
('FREESHIP', 'Miễn phí ship', 'Freeship một lần khi đơn đạt ngưỡng tối thiểu.', 'FREESHIP', 0, 80000, NULL, NULL, '2099-12-31', TRUE, 'SHIP'),
('GIAM30K', 'Giảm 30.000đ', 'Áp dụng đơn từ 100k.', 'AMOUNT', 30000, 100000, NULL, NULL, '2099-12-31', TRUE, 'ALL'),
('PHANTRAM15', 'Giảm 15%', 'Tối đa 25k tiền giảm.', 'PERCENT', 15, 60000, 25000, NULL, '2099-12-31', TRUE, 'ALL'),
('TRASUA10', 'Ưu đãi đồ uống', 'Giảm 10% cho nhóm đồ uống (demo).', 'PERCENT', 10, 40000, 20000, NULL, '2099-12-31', TRUE, 'DRINK'),
('SHIP15K', 'Hoàn 15k phí ship', 'Hoàn tối đa bằng phí ship hiện tại.', 'FREESHIP', 0, 60000, NULL, NULL, '2099-12-31', TRUE, 'SHIP'),
('COMBO50', 'Combo tiết kiệm', 'Giảm cố định 50k cho đơn lớn.', 'AMOUNT', 50000, 200000, NULL, 500, '2099-12-31', TRUE, 'FOOD'),
('WEEKEND8', 'Cuối tuần -8%', 'Giảm nhẹ cuối tuần.', 'PERCENT', 8, 70000, 18000, NULL, '2099-12-31', TRUE, 'ALL')
ON CONFLICT (code) DO NOTHING;

-- Tài khoản demo: đăng nhập **demo** / mật khẩu **demo123** (chạy trước seed voucher của user)
INSERT INTO users (username, email, password_hash, full_name, phone)
SELECT 'demo', 'demo@foodorder.local',
        'RcTBa42mEa3BJX1XQTqcgw==$Qf7ZAV+vEQlCisqIQYcVcwOMgvj3hFtaqdoFyrzEnzc=',
        'Người dùng demo', '0900000001'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(TRIM(username)) = 'demo');

INSERT INTO user_addresses (user_id, label, recipient_name, phone, address_line, province, district, is_default)
SELECT u.id, 'Nhà', u.full_name, u.phone,
       '123 Đường Demo, Phường Bến Nghé', 'TP.Hồ Chí Minh', 'Quận 1', TRUE
FROM users u WHERE u.username = 'demo'
AND NOT EXISTS (SELECT 1 FROM user_addresses ua WHERE ua.user_id = u.id);

INSERT INTO user_vouchers (user_id, voucher_id)
SELECT u.id, v.id FROM users u, vouchers v
WHERE u.username = 'demo' AND v.code IN ('GIAM30K', 'FREESHIP')
ON CONFLICT (user_id, voucher_id) DO NOTHING;

INSERT INTO voucher_usage (user_id, voucher_id, order_id, discount_amount, used_at)
SELECT u.id, v.id, NULL, 12000, NOW() - INTERVAL '3 days'
FROM users u, vouchers v
    WHERE u.username = 'demo' AND v.code = 'PHANTRAM15'
  AND NOT EXISTS (
    SELECT 1 FROM voucher_usage hu
    JOIN vouchers vv ON hu.voucher_id = vv.id
    JOIN users u2 ON hu.user_id = u2.id
    WHERE u2.username = 'demo' AND vv.code = 'PHANTRAM15'
  );

-- ==================================================
-- Demo: giỏ hàng + vài đơn mẫu cho user **demo** (chạy sau khi đã có foods)
-- Giỏ: chỉ thêm nếu chưa có dòng (ON CONFLICT DO NOTHING).
-- Đơn: chỉ seed khi user demo chưa có đơn nào (tránh trùng khi chạy lại script).
-- ==================================================
DO $$
DECLARE
    uid   INT;
    aid   INT;
    fid1  INT;
    fid2  INT;
    fid3  INT;
    p1    NUMERIC(12,0);
    p2    NUMERIC(12,0);
    p3    NUMERIC(12,0);
    oid1  INT;
    oid2  INT;
    oid3  INT;
    t1    NUMERIC(12,0);
    t2    NUMERIC(12,0);
    t3    NUMERIC(12,0);
BEGIN
    SELECT u.id INTO uid FROM users u WHERE LOWER(TRIM(u.username)) = 'demo' LIMIT 1;
    IF uid IS NULL THEN
        RAISE NOTICE 'Không có user demo — bỏ qua seed giỏ/đơn.';
        RETURN;
    END IF;

    SELECT ua.id INTO aid
    FROM user_addresses ua
    WHERE ua.user_id = uid
    ORDER BY ua.is_default DESC, ua.id
    LIMIT 1;

    IF aid IS NULL THEN
        RAISE NOTICE 'User demo chưa có địa chỉ — bỏ qua seed đơn.';
        RETURN;
    END IF;

    SELECT f.id, f.price INTO fid1, p1 FROM foods f WHERE f.is_active = TRUE ORDER BY f.id LIMIT 1;
    SELECT f.id, f.price INTO fid2, p2 FROM foods f WHERE f.is_active = TRUE ORDER BY f.id LIMIT 1 OFFSET 1;
    SELECT f.id, f.price INTO fid3, p3 FROM foods f WHERE f.is_active = TRUE ORDER BY f.id LIMIT 1 OFFSET 2;

    IF fid1 IS NULL THEN
        RETURN;
    END IF;

    fid2 := COALESCE(fid2, fid1);
    fid3 := COALESCE(fid3, fid1);
    p2 := COALESCE(p2, p1);
    p3 := COALESCE(p3, p1);

    INSERT INTO cart_items (user_id, food_id, quantity)
    VALUES (uid, fid1, 2), (uid, fid2, 1)
    ON CONFLICT (user_id, food_id) DO NOTHING;

    IF EXISTS (SELECT 1 FROM orders WHERE user_id = uid) THEN
        RETURN;
    END IF;

    t1 := p1 * 1;
    INSERT INTO orders (user_id, total_price, status, address_id)
    VALUES (uid, t1, 0, aid)
    RETURNING id INTO oid1;

    INSERT INTO order_items (order_id, food_id, quantity, price)
    VALUES (oid1, fid1, 1, p1);

    INSERT INTO order_status_logs (order_id, status, note)
    VALUES (oid1, 0, 'Đơn hàng vừa được tạo (demo)');

    t2 := p2 * 2;
    INSERT INTO orders (user_id, total_price, status, address_id)
    VALUES (uid, t2, 2, aid)
    RETURNING id INTO oid2;

    INSERT INTO order_items (order_id, food_id, quantity, price)
    VALUES (oid2, fid2, 2, p2);

    INSERT INTO order_status_logs (order_id, status, note)
    VALUES
        (oid2, 0, 'Đơn hàng vừa được tạo (demo)'),
        (oid2, 1, 'Quán đang chuẩn bị'),
        (oid2, 2, 'Shipper đang giao');

    t3 := p3 * 1;
    INSERT INTO orders (user_id, total_price, status, address_id)
    VALUES (uid, t3, 3, aid)
    RETURNING id INTO oid3;

    INSERT INTO order_items (order_id, food_id, quantity, price)
    VALUES (oid3, fid3, 1, p3);

    INSERT INTO order_status_logs (order_id, status, note)
    VALUES
        (oid3, 0, 'Đơn hàng vừa được tạo (demo)'),
        (oid3, 3, 'Đơn đã hoàn thành');

END $$;

-- ==================================================
-- Bổ sung đơn **lịch sử** (hoàn thành / hủy) cho tab Lịch sử — chạy được nhiều lần,
-- chỉ chèn khi user demo đang có ít hơn 5 đơn thuộc lịch sử (status 3 hoặc 4).
-- ==================================================
DO $$
DECLARE
    uid       INT;
    aid       INT;
    hist_cnt  INT;
    need      INT;
    i         INT;
    fid_a     INT;
    fid_b     INT;
    pa        NUMERIC(12,0);
    pb        NUMERIC(12,0);
    new_id    INT;
    st        INT;
BEGIN
    SELECT u.id INTO uid FROM users u WHERE LOWER(TRIM(u.username)) = 'demo' LIMIT 1;
    IF uid IS NULL THEN
        RETURN;
    END IF;

    SELECT ua.id INTO aid
    FROM user_addresses ua
    WHERE ua.user_id = uid
    ORDER BY ua.is_default DESC, ua.id
    LIMIT 1;

    IF aid IS NULL THEN
        RETURN;
    END IF;

    SELECT COUNT(*)::INT INTO hist_cnt FROM orders WHERE user_id = uid AND status IN (3, 4);
    need := GREATEST(0, 5 - hist_cnt);
    IF need <= 0 THEN
        RETURN;
    END IF;

    SELECT f.id, f.price INTO fid_a, pa FROM foods f WHERE f.is_active = TRUE ORDER BY f.id LIMIT 1 OFFSET 2;
    SELECT f.id, f.price INTO fid_b, pb FROM foods f WHERE f.is_active = TRUE ORDER BY f.id LIMIT 1 OFFSET 5;
    fid_a := COALESCE(fid_a, (SELECT id FROM foods WHERE is_active = TRUE ORDER BY id LIMIT 1));
    fid_b := COALESCE(fid_b, fid_a);
    pa := COALESCE(pa, (SELECT price FROM foods WHERE id = fid_a));
    pb := COALESCE(pb, (SELECT price FROM foods WHERE id = fid_b));

    FOR i IN 1..need LOOP
        st := CASE WHEN i % 3 = 0 THEN 4 ELSE 3 END;

        INSERT INTO orders (user_id, total_price, status, address_id, created_at)
        VALUES (
            uid,
            CASE WHEN st = 4 THEN pa * 1 ELSE pa * 2 + pb * 1 END,
            st,
            aid,
            NOW() - ((i::text || ' days')::INTERVAL)
        )
        RETURNING id INTO new_id;

        IF st = 4 THEN
            INSERT INTO order_items (order_id, food_id, quantity, price)
            VALUES (new_id, fid_a, 1, pa);
            INSERT INTO order_status_logs (order_id, status, note)
            VALUES
                (new_id, 0, 'Đơn demo — chờ xác nhận'),
                (new_id, 4, 'Đơn demo — đã hủy');
        ELSE
            INSERT INTO order_items (order_id, food_id, quantity, price)
            VALUES
                (new_id, fid_a, 2, pa),
                (new_id, fid_b, 1, pb);
            INSERT INTO order_status_logs (order_id, status, note)
            VALUES
                (new_id, 0, 'Đơn demo — chờ xác nhận'),
                (new_id, 2, 'Đơn demo — đang giao'),
                (new_id, 3, 'Đơn demo — hoàn thành');
        END IF;
    END LOOP;
END $$;
