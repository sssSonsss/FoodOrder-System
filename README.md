# FoodOrder-System

Ứng dụng web đặt món trực tuyến phục vụ môn Lập trình web / BTL: người dùng xem thực đơn, xem chi tiết món, thêm vào giỏ, đặt hàng, theo dõi đơn và xem lịch sử; có đăng nhập / đăng ký, giỏ hàng tập trung (`CartServlet`), thông báo và voucher.

## Kiến trúc tóm tắt

| Thành phần | Công nghệ |
|------------|-----------|
| Backend | Java **17+**, Jakarta Servlet **6** (Tomcat **10**) |
| CSDL | **PostgreSQL** |
| Giao diện | HTML tĩnh, JSP, CSS/JS trong thư mục `web/` |
| Đóng gói | WAR (`build/FoodOrder-System.war`), không dùng Maven/Gradle trong repo |

Các servlet chính (REST/HTML): `CategoryServlet`, `FoodServlet`, `AuthServlet`, `CartServlet`, `OrderServlet`, `OrderTrackingServlet`, `NotificationServlet`, `VoucherServlet`.

---

## Cần cài đặt trước

1. **JDK 17 trở lên** — có `javac` và `jar` trên `PATH`.
2. **PostgreSQL** — server chạy local (mặc định port `5432`).
3. **Apache Tomcat 10.1.x** (Jakarta EE 10) — ví dụ giải nén vào `TOMCAT_HOME`.
4. **Trình duyệt** để kiểm tra giao diện.

Thư viện JDBC và Servlet API đã kèm trong `lib/`:

- `lib/postgresql-42.7.3.jar`
- `lib/jakarta.servlet-api-6.0.0.jar`

---

## Cấu hình cơ sở dữ liệu

1. Tạo database (hoặc dùng đúng tên trong script SQL / `DBConnection.java`).
2. Nạp schema và dữ liệu mẫu:

```bash
psql -U postgres -d laptrinhweb_btl_foodorder -f BTL_FoodMenu.sql
```

3. Thông tin kết nối mặc định trong `src/java/utils/DBConnection.java`:

- URL: `jdbc:postgresql://localhost:5432/laptrinhweb_btl_foodorder`
- User / password: chỉnh theo máy bạn (mặc định trong code thường là `postgres` / `123`)

Sau khi đổi DB, chạy lại `bash scripts/rebuild.sh` rồi deploy lại WAR.

---

## Build (compile + đóng gói WAR)

Chạy từ **thư mục gốc** của repo:

```bash
bash scripts/rebuild.sh
```

Kết quả:

- Bytecode Java trong `build/classes/`
- File `build/FoodOrder-System.war`

---

## Chạy ứng dụng (deploy Tomcat)

1. Đảm bảo đã build xong (`FoodOrder-System.war` tồn tại).
2. Đặt biến `TOMCAT_HOME` trỏ tới thư mục Tomcat của bạn (ví dụ bản 10.1.40).
3. Script sẽ copy WAR vào `webapps/`, gọi `startup.sh` và (trên macOS/Linux) cố gắng dừng tiến trình Tomcat cũ trùng đường dẫn.

```bash
TOMCAT_HOME=/đường/dẫn/apache-tomcat-10.1.x bash scripts/deploy-local-tomcat.sh
```

Mở trình duyệt (context path là tên WAR):

```text
http://localhost:<port>/FoodOrder-System/index.html
```

**Port:** mặc định Tomcat thường là `8080`. README và script mẫu trước đây dùng `8081` nếu bạn đã đổi `Connector port` trong `$TOMCAT_HOME/conf/server.xml` — hãy dùng đúng port Tomcat đang lắng nghe.

### Dừng Tomcat (khi dùng đường dẫn giống lúc deploy)

```bash
pkill -f "$TOMCAT_HOME"
```

Hoặc:

```bash
$TOMCAT_HOME/bin/shutdown.sh
```

---

## Kiểm tra nhanh sau khi chạy

- Trang chủ: `http://localhost:<port>/FoodOrder-System/index.html`
- API mẫu:  
  `.../FoodOrder-System/CategoryServlet`  
  `.../FoodOrder-System/FoodServlet`

---

## Test kết nối backend (tùy chọn)

Compile thêm lớp trong `src/java/test/` rồi chạy:

```bash
javac -cp "lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" \
      -d build/classes src/java/test/*.java src/java/utils/*.java src/java/model/*.java src/java/dao/*.java

java -cp "build/classes:lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" test.TestDBConnection
java -cp "build/classes:lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" test.TestFoodCategory
```

Kỳ vọng: kết nối DB OK; `TestFoodCategory` báo PASS.

---

## Quy trình làm việc gợi ý

1. Chỉnh `web/` hoặc `src/java/`.
2. `bash scripts/rebuild.sh`
3. `TOMCAT_HOME=... bash scripts/deploy-local-tomcat.sh`
4. Reload trình duyệt (hard refresh nếu cache CSS/JS).
5. Nếu lỗi DB: kiểm tra PostgreSQL đang chạy, tên DB/user/pass và file `BTL_FoodMenu.sql`.

---

## Build thủ công (khi cần debug)

Nếu không dùng script, có thể làm tương đương `scripts/rebuild.sh`: compile `src/java/utils`, `model`, `dao`, `controller`; copy `web/*` vào `build/classes`; đặt class vào `WEB-INF/classes`, JAR vào `WEB-INF/lib`; trong `build/classes` chạy `jar -cf ../FoodOrder-System.war .`.
