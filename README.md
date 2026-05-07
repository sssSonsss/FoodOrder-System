# FoodOrder-System

Tài liệu này dùng cho luồng phát triển hằng ngày: sửa code -> rebuild -> deploy -> test lại.

## 1) Yêu cầu môi trường

- Java: JDK 17+ (có `javac`, `jar`)
- PostgreSQL local
- Tomcat 10 (đang chạy port `8081`)
- Thư viện trong dự án:
  - `lib/postgresql-42.7.3.jar`
  - `lib/jakarta.servlet-api-6.0.0.jar`

## 2) Cấu hình Database

Thông tin DB local đang dùng:

- Database: `laptrinhweb_btl_foodorder`
- User: `postgres`
- Password: `123`

File kết nối: `src/java/utils/DBConnection.java`

Nạp dữ liệu:

```bash
psql -U postgres -d laptrinhweb_btl_foodorder -f BTL_FoodMenu.sql
```

## 3) Rebuild sau mỗi lần sửa code

### Cách nhanh (khuyên dùng)

```bash
bash scripts/rebuild.sh
```

Kết quả:

- Compile toàn bộ backend Java vào `build/classes`
- Đóng gói WAR tại `build/FoodOrder-System.war`

### Cách thủ công (nếu cần debug)

```bash
rm -rf build/classes
mkdir -p build/classes

javac -cp "lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" \
      -d build/classes \
      src/java/utils/*.java src/java/model/*.java src/java/dao/*.java src/java/controller/*.java

mkdir -p build/classes/WEB-INF/classes build/classes/WEB-INF/lib
cp -R web/* build/classes/
cp -R build/classes/controller build/classes/WEB-INF/classes/
cp -R build/classes/dao build/classes/WEB-INF/classes/
cp -R build/classes/model build/classes/WEB-INF/classes/
cp -R build/classes/utils build/classes/WEB-INF/classes/
cp lib/postgresql-42.7.3.jar build/classes/WEB-INF/lib/
cp lib/jakarta.servlet-api-6.0.0.jar build/classes/WEB-INF/lib/

cd build/classes && jar -cf ../FoodOrder-System.war .
```

## 4) Deploy và chạy Tomcat local

### Cách nhanh (khuyên dùng)

```bash
TOMCAT_HOME=/tmp/foodorder-tomcat/apache-tomcat-10.1.40 bash scripts/deploy-local-tomcat.sh
```

Mở ứng dụng:

- `http://localhost:8081/FoodOrder-System/index.html`

### Dừng server

```bash
pkill -f "/tmp/foodorder-tomcat/apache-tomcat-10.1.40"
```

## 5) Test nhanh sau khi deploy

- Kiểm tra trang chủ:
  - `http://localhost:8081/FoodOrder-System/index.html`
- Kiểm tra API:
  - `http://localhost:8081/FoodOrder-System/CategoryServlet`
  - `http://localhost:8081/FoodOrder-System/FoodServlet`
  - `http://localhost:8081/FoodOrder-System/OrderServlet?action=my-page&userId=1`

## 6) Chạy test backend bằng tay

```bash
javac -cp "lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" \
      -d build/classes src/java/test/*.java src/java/utils/*.java src/java/model/*.java src/java/dao/*.java

java -cp "build/classes:lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" test.TestDBConnection
java -cp "build/classes:lib/postgresql-42.7.3.jar:lib/jakarta.servlet-api-6.0.0.jar" test.TestFoodCategory
```

Kỳ vọng hiện tại:

- `TestDBConnection`: kết nối DB OK
- `TestFoodCategory`: 10 PASSED | 0 FAILED

## 7) Checklist mỗi lần sửa code

1. Sửa backend/frontend.
2. Chạy `bash scripts/rebuild.sh`.
3. Chạy deploy script Tomcat.
4. Reload trình duyệt + test API.
5. Nếu có lỗi DB, kiểm tra `DBConnection.java` và dữ liệu trong `BTL_FoodMenu.sql`.
