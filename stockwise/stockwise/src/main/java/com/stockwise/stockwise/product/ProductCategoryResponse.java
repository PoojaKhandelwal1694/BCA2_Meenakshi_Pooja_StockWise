package com.stockwise.stockwise.product;

public class ProductCategoryResponse {

    private Integer productId;
    private String productName;
    private String sku;
    private Integer categoryId;
    private String categoryName;
    private Integer quantity;
    private Integer reorderLevel;

    public ProductCategoryResponse(
            Integer productId,
            String productName,
            String sku,
            Integer categoryId,
            String categoryName,
            Integer quantity,
            Integer reorderLevel) {

        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
         this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getSku() {
        return sku;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
     public Integer getQuantity() 
     { return quantity; }
    public Integer getReorderLevel() 
    { return reorderLevel; }
}
