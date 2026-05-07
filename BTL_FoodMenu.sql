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
-- MODULE ĐƠN HÀNG & TRACKING
-- ==================================================
CREATE TABLE IF NOT EXISTS orders (
    id           SERIAL PRIMARY KEY,
    user_id      INT NOT NULL REFERENCES users(id),
    total_price  NUMERIC(12,0) NOT NULL DEFAULT 0,
    status       VARCHAR(30) NOT NULL DEFAULT 'CHO_XAC_NHAN',
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
    status       VARCHAR(30) NOT NULL,
    note         VARCHAR(255),
    created_at   TIMESTAMP DEFAULT NOW()
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


-- ==================================================
-- DATA MẪU: DANH MỤC
-- ==================================================
INSERT INTO categories (name, description, image_url) VALUES
('Đồ uống',    'Trà sữa, cà phê, sinh tố và các loại nước giải khát',   'images/cat_drink.jpg'),
('Bánh mì',    'Bánh mì Việt Nam đa dạng nhân thơm ngon',                'images/cat_banhmi.jpg'),
('Cơm',        'Cơm văn phòng, cơm tấm, cơm gà các loại',                'images/cat_com.jpg'),
('Bún & Phở',  'Phở bò, bún bò Huế, bún riêu và các loại bún nước',      'images/cat_pho.jpg'),
('Chay',       'Món ăn thuần chay, tốt cho sức khỏe',                     'images/cat_chay.jpg'),
('Tráng miệng','Chè, kem, bánh ngọt và các món tráng miệng hấp dẫn',     'images/cat_dessert.jpg');


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
