package model;

/**
 * Mô hình voucher / mã giảm giá (bảng vouchers và hiển thị kèm metadata ví người dùng).
 */
public class Voucher {

    private int id;
    private String code;
    private String title;
    private String description;
    /** PERCENT | AMOUNT | FREESHIP */
    private String discountType;
    private double discountValue;
    private double minOrderValue;
    private Double maxDiscountAmount;
    private Integer usageLimit;
    private int usedCount;
    private String expiryDate;
    private boolean active;
    /** Nhóm giả lập để lọc UI: ALL, DRINK, FOOD, SHIP */
    private String promoScope;
    /** Đã nhận vào ví hay chưa (tab ưu đãi) */
    private boolean claimed;
    /** Thời điểm nhận (ví của tôi) */
    private String claimedAt;
    /** Lịch sử dùng */
    private String usedAt;
    private double savedDiscount;
    private Integer relatedOrderId;
    /** Khóa dòng voucher_usage (tab lịch sử — có thể nhiều lần dùng cùng mã). */
    private Integer usageRecordId;

    public Voucher() {
    }

    public Voucher(int id, String code, String title, String description,
                     String discountType, double discountValue, double minOrderValue,
                     Double maxDiscountAmount, Integer usageLimit, int usedCount,
                     String expiryDate, boolean active, String promoScope) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.expiryDate = expiryDate;
        this.active = active;
        this.promoScope = promoScope;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPromoScope() {
        return promoScope;
    }

    public void setPromoScope(String promoScope) {
        this.promoScope = promoScope;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public String getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(String claimedAt) {
        this.claimedAt = claimedAt;
    }

    public String getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(String usedAt) {
        this.usedAt = usedAt;
    }

    public double getSavedDiscount() {
        return savedDiscount;
    }

    public void setSavedDiscount(double savedDiscount) {
        this.savedDiscount = savedDiscount;
    }

    public Integer getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Integer relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public Integer getUsageRecordId() {
        return usageRecordId;
    }

    public void setUsageRecordId(Integer usageRecordId) {
        this.usageRecordId = usageRecordId;
    }
}
