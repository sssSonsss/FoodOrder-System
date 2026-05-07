package test;

import dao.CategoryDAO;
import dao.FoodDAO;
import model.Category;
import model.Food;
import utils.DBConnection;

import java.sql.Connection;
import java.util.List;

/**
 * Test tổng hợp:
 *  1. Kiểm tra kết nối DB
 *  2. Test CategoryDAO.getAllCategories()
 *  3. Test CategoryDAO.getCategoryById()
 *  4. Test FoodDAO.getFoods() (không filter)
 *  5. Test FoodDAO.getFoods() với search
 *  6. Test FoodDAO.getFoods() với category filter
 *  7. Test FoodDAO.getFoods() với sort
 *  8. Test FoodDAO.getFoodById()
 *  9. Test FoodDAO.getFeaturedFoods()
 * 10. Test FoodDAO.getFoodsByCategory()
 */
public class TestFoodCategory {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {

        System.out.println("==================================================");
        System.out.println("  BTL FoodOrder - Test Suite: Food & Category");
        System.out.println("==================================================\n");

        test1_DBConnection();
        test2_GetAllCategories();
        test3_GetCategoryById();
        test4_GetAllFoods();
        test5_SearchFoods();
        test6_FilterByCategory();
        test7_SortFoods();
        test8_GetFoodById();
        test9_GetFeaturedFoods();
        test10_GetFoodsByCategory();

        System.out.println("\n==================================================");
        System.out.printf("  KẾT QUẢ: %d PASSED  |  %d FAILED%n", passed, failed);
        System.out.println("==================================================");
    }

    // ===== TEST 1: DB Connection =====
    static void test1_DBConnection() {
        System.out.println("[TEST 1] Kết nối Database...");
        try {
            Connection conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                pass("Kết nối DB thành công");
                conn.close();
            } else {
                fail("Connection null hoặc đã đóng");
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 2: Get All Categories =====
    static void test2_GetAllCategories() {
        System.out.println("\n[TEST 2] CategoryDAO.getAllCategories()...");
        try {
            CategoryDAO dao = new CategoryDAO();
            List<Category> list = dao.getAllCategories();

            System.out.println("  → Số danh mục: " + list.size());
            for (Category c : list) {
                System.out.println("    - [" + c.getId() + "] " + c.getName());
            }

            if (!list.isEmpty()) {
                pass("Lấy danh mục thành công (" + list.size() + " mục)");
            } else {
                fail("Danh sách rỗng - kiểm tra DB có data chưa?");
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 3: Get Category By ID =====
    static void test3_GetCategoryById() {
        System.out.println("\n[TEST 3] CategoryDAO.getCategoryById(1)...");
        try {
            CategoryDAO dao = new CategoryDAO();
            Category c = dao.getCategoryById(1);

            if (c != null) {
                System.out.println("  → Tìm thấy: " + c.getName());
                pass("getCategoryById(1) thành công");
            } else {
                fail("Không tìm thấy category id=1");
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 4: Get All Foods =====
    static void test4_GetAllFoods() {
        System.out.println("\n[TEST 4] FoodDAO.getFoods(null, 0, null)...");
        try {
            FoodDAO dao = new FoodDAO();
            List<Food> list = dao.getFoods(null, 0, null);

            System.out.println("  → Tổng số món: " + list.size());
            for (Food f : list) {
                System.out.printf("    - [%d] %s | %.0fđ | ⭐%.1f%n",
                    f.getId(), f.getName(), f.getPrice(), f.getRating());
            }

            if (!list.isEmpty()) {
                pass("Lấy danh sách món thành công (" + list.size() + " món)");
            } else {
                fail("Danh sách rỗng - kiểm tra DB có data chưa?");
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 5: Search Foods =====
    static void test5_SearchFoods() {
        System.out.println("\n[TEST 5] FoodDAO.getFoods(search='Trà', 0, null)...");
        try {
            FoodDAO dao = new FoodDAO();
            List<Food> list = dao.getFoods("Trà", 0, null);

            System.out.println("  → Kết quả tìm 'Trà': " + list.size() + " món");
            for (Food f : list) {
                System.out.println("    - " + f.getName());
            }

            pass("Tìm kiếm hoàn tất (" + list.size() + " kết quả)");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 6: Filter By Category =====
    static void test6_FilterByCategory() {
        System.out.println("\n[TEST 6] FoodDAO.getFoods(null, categoryId=1, null)...");
        try {
            FoodDAO dao = new FoodDAO();
            List<Food> list = dao.getFoods(null, 1, null);

            System.out.println("  → Món thuộc category 1: " + list.size() + " món");
            for (Food f : list) {
                System.out.println("    - " + f.getName() + " [cat=" + f.getCategoryName() + "]");
            }

            pass("Lọc theo category hoàn tất (" + list.size() + " kết quả)");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 7: Sort Foods =====
    static void test7_SortFoods() {
        System.out.println("\n[TEST 7] FoodDAO - sort price_asc / price_desc / rating / newest...");
        try {
            FoodDAO dao = new FoodDAO();

            String[] sorts = {"price_asc", "price_desc", "rating", "newest"};
            for (String sort : sorts) {
                List<Food> list = dao.getFoods(null, 0, sort);
                String first = list.isEmpty() ? "N/A" : list.get(0).getName();
                System.out.println("  → Sort [" + sort + "]: " + list.size() + " món, đầu tiên: " + first);
            }

            pass("Sắp xếp hoàn tất (4 kiểu sort)");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 8: Get Food By ID =====
    static void test8_GetFoodById() {
        System.out.println("\n[TEST 8] FoodDAO.getFoodById(1)...");
        try {
            FoodDAO dao = new FoodDAO();
            Food f = dao.getFoodById(1);

            if (f != null) {
                System.out.println("  → Tìm thấy: " + f.getName() + " | " + f.getPrice() + "đ");
                pass("getFoodById(1) thành công");
            } else {
                fail("Không tìm thấy food id=1");
            }
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 9: Get Featured Foods =====
    static void test9_GetFeaturedFoods() {
        System.out.println("\n[TEST 9] FoodDAO.getFeaturedFoods(6)...");
        try {
            FoodDAO dao = new FoodDAO();
            List<Food> list = dao.getFeaturedFoods(6);

            System.out.println("  → Món nổi bật (top 6): " + list.size() + " món");
            for (Food f : list) {
                System.out.printf("    - %s | ⭐%.1f (%d đánh giá)%n",
                    f.getName(), f.getRating(), f.getReviewCount());
            }

            pass("getFeaturedFoods(6) hoàn tất");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== TEST 10: Get Foods By Category =====
    static void test10_GetFoodsByCategory() {
        System.out.println("\n[TEST 10] FoodDAO.getFoodsByCategory(2)...");
        try {
            FoodDAO dao = new FoodDAO();
            List<Food> list = dao.getFoodsByCategory(2);

            System.out.println("  → Món thuộc category 2: " + list.size() + " món");
            pass("getFoodsByCategory(2) hoàn tất (" + list.size() + " kết quả)");
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        }
    }

    // ===== Helpers =====
    static void pass(String msg) {
        passed++;
        System.out.println("  ✅ PASS - " + msg);
    }

    static void fail(String msg) {
        failed++;
        System.out.println("  ❌ FAIL - " + msg);
    }
}
