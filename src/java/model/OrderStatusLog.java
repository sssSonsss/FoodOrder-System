package model;

public class OrderStatusLog {

    private int id;
    private int orderId;
    private int status;
    private String note;
    private String createdAt;

    public OrderStatusLog() {}

    public OrderStatusLog(int id, int orderId, int status, String note, String createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
