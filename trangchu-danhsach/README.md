# 🍜 FoodOrder – Trang Chủ & Danh Sách Món Ăn

## Cấu trúc dự án

```
trangchu-danhsach/
├── BTL_FoodMenu.sql                    ← Script SQL tạo bảng + data mẫu
├── web/
│   ├── index.html                      ← Trang chủ
│   ├── menu.html                       ← Danh sách món ăn
│   ├── food-detail.html                ← Chi tiết món ăn
│   ├── css/
│   │   ├── style.css                   ← CSS dùng chung (design system)
│   │   ├── index.css                   ← CSS trang chủ
│   │   ├── menu.css                    ← CSS trang menu
│   │   └── food-detail.css             ← CSS trang chi tiết
│   └── js/
│       ├── index.js                    ← JS trang chủ
│       ├── menu.js                     ← JS trang menu
│       └── food-detail.js              ← JS trang chi tiết
└── src/java/
    ├── model/
    │   ├── Category.java
    │   └── Food.java
    ├── dao/
    │   ├── CategoryDAO.java
    │   └── FoodDAO.java
    ├── controller/
    │   ├── CategoryServlet.java        ← GET /CategoryServlet
    │   └── FoodServlet.java            ← GET /FoodServlet
    ├── utils/
    │   └── DBConnection.java
    └── test/
        └── TestFoodCategory.java       ← Test 10 function
```

## Cài đặt Database

1. Mở **SQL Server Management Studio**
2. Chạy file `BTL_FoodMenu.sql` (sau khi đã chạy `BTL.sql` gốc)
3. Kết quả: 6 danh mục + 24 món ăn mẫu

## API Endpoints

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/CategoryServlet` | Tất cả danh mục |
| GET | `/CategoryServlet?id=1` | Chi tiết danh mục |
| GET | `/FoodServlet` | Tất cả món ăn |
| GET | `/FoodServlet?search=trà` | Tìm kiếm theo tên |
| GET | `/FoodServlet?category=1` | Lọc theo danh mục |
| GET | `/FoodServlet?sort=price_asc` | Sắp xếp (price_asc/price_desc/rating/newest) |
| GET | `/FoodServlet?featured=6` | Món nổi bật (top N) |
| GET | `/FoodServlet?id=1` | Chi tiết 1 món |

## Chạy Test

Mở **TestFoodCategory.java** → Run → Console hiển thị 10 test cases:
- Test 1: Kết nối DB
- Test 2-3: CategoryDAO
- Test 4-10: FoodDAO (tất cả + search + filter + sort + detail + featured)

## Tính năng Frontend

### index.html (Trang chủ)
- Hero section với search bar + gợi ý tự động
- Danh mục món ăn (load từ API)
- Món nổi bật top 6 (load từ API)
- Hướng dẫn 3 bước đặt món
- Banner voucher NEWUSER

### menu.html (Danh sách)
- Sidebar: lọc danh mục, khoảng giá, đánh giá
- Tìm kiếm real-time với debounce
- Sắp xếp 4 kiểu
- Chuyển đổi Grid/List view
- Phân trang (12 món/trang)
- Active filter tags

### food-detail.html (Chi tiết)
- Ảnh + thông tin đầy đủ
- Rating stars
- Chọn số lượng
- Thêm vào giỏ / Đặt ngay
- Món cùng danh mục (gợi ý)
- Yêu thích (lưu localStorage)
