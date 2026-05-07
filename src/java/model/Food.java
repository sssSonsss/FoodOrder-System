package model;

public class Food {

    private int id;
    private int categoryId;
    private String categoryName;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private boolean isActive;
    private String createdAt;
    private double rating;
    private int reviewCount;

    public Food() {}

    public Food(int id, int categoryId, String categoryName, String name,
                String description, double price, String imageUrl,
                boolean isActive, String createdAt, double rating, int reviewCount) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    @Override
    public String toString() {
        return "Food{id=" + id + ", name='" + name + "', price=" + price + ", rating=" + rating + "}";
    }
}
