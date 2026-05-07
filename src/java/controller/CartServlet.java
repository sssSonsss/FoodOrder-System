package controller;

import dao.CartDAO;
import model.CartItem;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/CartServlet")
public class CartServlet extends HttpServlet {

    private final CartDAO cartDAO = new CartDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int userId = getCurrentUserId(request);
        if (userId <= 0) {
            request.setAttribute("emptyMessage", "Vui lòng đăng nhập để xem giỏ hàng.");
            request.getRequestDispatcher("/cart.jsp").forward(request, response);
            return;
        }

        List<CartItem> cartItems = cartDAO.getCartByUserId(userId);
        double grandTotal = 0;
        for (CartItem item : cartItems) {
            grandTotal += item.getPrice() * item.getQuantity();
        }

        if (cartItems.isEmpty()) {
            request.setAttribute("emptyMessage", "Giỏ hàng của bạn đang trống");
        }

        request.setAttribute("cartItems", cartItems);
        request.setAttribute("grandTotal", grandTotal);
        request.getRequestDispatcher("/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        int userId = getCurrentUserId(request);
        if (userId <= 0) {
            response.setStatus(401);
            response.getWriter().print("{\"error\":\"Vui lòng đăng nhập để thao tác giỏ hàng\"}");
            return;
        }

        String action = request.getParameter("action");
        if ("add".equals(action)) {
            handleAdd(request, response, userId);
            return;
        }
        if ("update".equals(action)) {
            handleUpdate(request, response);
            return;
        }
        if ("delete".equals(action)) {
            handleDelete(request, response);
            return;
        }

        response.setStatus(400);
        response.getWriter().print("{\"error\":\"Action không hợp lệ\"}");
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        int foodId = parseInt(request.getParameter("foodId"), 0);
        int quantity = parseInt(request.getParameter("quantity"), 1);

        if (foodId <= 0 || quantity <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Thông tin thêm giỏ hàng không hợp lệ\"}");
            return;
        }

        if (!cartDAO.checkStock(foodId, quantity)) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Món ăn không còn đủ số lượng để thêm vào giỏ\"}");
            return;
        }

        boolean ok = cartDAO.addToCart(userId, foodId, quantity);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể thêm món vào giỏ hàng\"}");
            return;
        }

        response.getWriter().print("{\"message\":\"Đã thêm món vào giỏ hàng\"}");
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int cartItemId = parseInt(request.getParameter("cartItemId"), 0);
        int quantity = parseInt(request.getParameter("quantity"), 0);

        if (cartItemId <= 0 || quantity <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Số lượng cập nhật không hợp lệ\"}");
            return;
        }

        boolean ok = cartDAO.updateQuantity(cartItemId, quantity);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể cập nhật số lượng\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Cập nhật số lượng thành công\"}");
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int cartItemId = parseInt(request.getParameter("cartItemId"), 0);
        if (cartItemId <= 0) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Món trong giỏ không hợp lệ\"}");
            return;
        }

        boolean ok = cartDAO.removeFromFile(cartItemId);
        if (!ok) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Không thể xóa món khỏi giỏ\"}");
            return;
        }
        response.getWriter().print("{\"message\":\"Đã xóa món khỏi giỏ hàng\"}");
    }

    private int getCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object[] keys = {
                session.getAttribute("userId"),
                session.getAttribute("user_id"),
                session.getAttribute("uid")
            };
            for (Object key : keys) {
                int id = parseObjectToInt(key);
                if (id > 0) return id;
            }
        }
        int userIdFromParam = parseInt(request.getParameter("userId"), 0);
        if (userIdFromParam > 0) return userIdFromParam;
        // Fake user để test end-to-end khi chưa có đăng nhập
        return 1;
    }

    private int parseObjectToInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
