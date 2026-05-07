package controller;

import dao.FoodDAO;
import model.Food;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * API endpoint cho danh sách món ăn.
 *
 * GET /FoodServlet
 *   → Trả toàn bộ danh sách món (active)
 *
 * GET /FoodServlet?search=trà&category=2&sort=price_asc
 *   → Tìm kiếm + lọc + sắp xếp
 *   Params:
 *     search   : tên món (LIKE %...%)
 *     category : category_id (số nguyên, 0 = tất cả)
 *     sort     : price_asc | price_desc | rating | newest
 *
 * GET /FoodServlet?featured=6
 *   → Lấy N món nổi bật (rating cao)
 *
 * GET /FoodServlet?id=3
 *   → Chi tiết 1 món ăn
 */
@WebServlet("/FoodServlet")
public class FoodServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        FoodDAO dao = new FoodDAO();

        // ===== Chi tiết 1 món =====
        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                int id = Integer.parseInt(idParam);
                Food f = dao.getFoodById(id);

                if (f == null) {
                    response.setStatus(404);
                    out.print("{\"error\":\"Không tìm thấy món ăn\"}");
                } else {
                    out.print(foodToJson(f));
                }

            } catch (NumberFormatException e) {
                response.setStatus(400);
                out.print("{\"error\":\"ID không hợp lệ\"}");
            }
            return;
        }

        // ===== Món nổi bật =====
        String featuredParam = request.getParameter("featured");
        if (featuredParam != null && !featuredParam.isEmpty()) {
            try {
                int limit = Integer.parseInt(featuredParam);
                List<Food> list = dao.getFeaturedFoods(limit);
                out.print(listToJson(list));
            } catch (NumberFormatException e) {
                response.setStatus(400);
                out.print("{\"error\":\"Limit không hợp lệ\"}");
            }
            return;
        }

        // ===== Tìm kiếm + lọc + sắp xếp =====
        String search = request.getParameter("search");
        String sort   = request.getParameter("sort");
        int categoryId = 0;

        String categoryParam = request.getParameter("category");
        if (categoryParam != null && !categoryParam.isEmpty()) {
            try {
                categoryId = Integer.parseInt(categoryParam);
            } catch (NumberFormatException ignored) {}
        }

        List<Food> list = dao.getFoods(search, categoryId, sort);
        out.print(listToJson(list));
    }

    // ===== Helper: List<Food> → JSON array =====
    private String listToJson(List<Food> list) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Food f : list) {
            if (!first) json.append(",");
            first = false;
            json.append(foodToJson(f));
        }
        json.append("]");
        return json.toString();
    }

    // ===== Helper: Food → JSON object =====
    private String foodToJson(Food f) {
        return "{"
            + "\"id\":" + f.getId() + ","
            + "\"category_id\":" + f.getCategoryId() + ","
            + "\"category_name\":\"" + escapeJson(f.getCategoryName()) + "\","
            + "\"name\":\"" + escapeJson(f.getName()) + "\","
            + "\"description\":\"" + escapeJson(f.getDescription()) + "\","
            + "\"price\":" + f.getPrice() + ","
            + "\"image_url\":\"" + escapeJson(f.getImageUrl()) + "\","
            + "\"is_active\":" + f.isActive() + ","
            + "\"created_at\":\"" + escapeJson(f.getCreatedAt()) + "\","
            + "\"rating\":" + f.getRating() + ","
            + "\"review_count\":" + f.getReviewCount()
            + "}";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
