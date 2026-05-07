package model;

public class OrderItem {

    private int orderId;
    private int foodId;
    private String foodName;
    private int quantity;
    private double price;
    private String imageUrl;

    public OrderItem() {}

    public OrderItem(int orderId, int foodId, String foodName, int quantity, double price, String imageUrl) {
        this.orderId = orderId;
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = quantity;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
