package controller;

import dao.NotificationDAO;
import model.Notification;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/NotificationServlet")
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = getCurrentUserId(request);
        response.setContentType("application/json;charset=UTF-8");

        List<Notification> list = notificationDAO.getNotificationsByUserId(userId);
        int unread = notificationDAO.countUnreadByUserId(userId);
        response.getWriter().print("{\"unread\":" + unread + ",\"items\":" + listToJson(list) + "}");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String action = request.getParameter("action");
        if ("mark-read".equals(action)) {
            int id = parseInt(request.getParameter("notificationId"), 0);
            if (id <= 0) {
                response.setStatus(400);
                response.getWriter().print("{\"error\":\"Thông báo không hợp lệ\"}");
                return;
            }
            boolean ok = notificationDAO.markAsRead(id);
            if (!ok) {
                response.setStatus(500);
                response.getWriter().print("{\"error\":\"Không thể cập nhật trạng thái đã đọc\"}");
                return;
            }
            response.getWriter().print("{\"message\":\"Đã đánh dấu đã đọc\"}");
            return;
        }

        response.setStatus(400);
        response.getWriter().print("{\"error\":\"Action không hợp lệ\"}");
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

    private String listToJson(List<Notification> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Notification n : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{")
              .append("\"id\":").append(n.getId()).append(",")
              .append("\"title\":\"").append(escapeJson(n.getTitle())).append("\",")
              .append("\"message\":\"").append(escapeJson(n.getMessage())).append("\",")
              .append("\"type\":\"").append(escapeJson(n.getType())).append("\",")
              .append("\"is_read\":").append(n.isRead()).append(",")
              .append("\"created_at\":\"").append(escapeJson(n.getCreatedAt())).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
