package controller;

import dao.CategoryDAO;
import model.Category;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * API endpoint cho danh mục món ăn.
 *
 * GET /CategoryServlet
 *   → Trả JSON: danh sách tất cả categories đang active
 *
 * GET /CategoryServlet?id=1
 *   → Trả JSON: chi tiết 1 category
 */
@WebServlet("/CategoryServlet")
public class CategoryServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        // Cho phép CORS khi test local
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        CategoryDAO dao = new CategoryDAO();

        String idParam = request.getParameter("id");

        // ===== Chi tiết 1 category =====
        if (idParam != null && !idParam.isEmpty()) {
            try {
                int id = Integer.parseInt(idParam);
                Category c = dao.getCategoryById(id);

                if (c == null) {
                    response.setStatus(404);
                    out.print("{\"error\":\"Không tìm thấy danh mục\"}");
                } else {
                    out.print(categoryToJson(c));
                }

            } catch (NumberFormatException e) {
                response.setStatus(400);
                out.print("{\"error\":\"ID không hợp lệ\"}");
            }
            return;
        }

        // ===== Danh sách tất cả categories =====
        List<Category> list = dao.getAllCategories();

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (Category c : list) {
            if (!first) json.append(",");
            first = false;
            json.append(categoryToJson(c));
        }

        json.append("]");
        out.print(json.toString());
    }

    // ===== Helper: Category → JSON string =====
    private String categoryToJson(Category c) {
        return "{"
            + "\"id\":" + c.getId() + ","
            + "\"name\":\"" + escapeJson(c.getName()) + "\","
            + "\"description\":\"" + escapeJson(c.getDescription()) + "\","
            + "\"image_url\":\"" + escapeJson(c.getImageUrl()) + "\","
            + "\"is_active\":" + c.isActive() + ","
            + "\"item_count\":" + c.getItemCount()
            + "}";
    }

    // Escape ký tự đặc biệt trong JSON
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
