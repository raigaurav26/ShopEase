package com.shopease.inventory.dto;

public class InventoryResponse {

    private Long id;
    private Long productId;
    private String skuCode;
    private int quantity;
    private boolean inStock;

    public InventoryResponse() {
    }

    public InventoryResponse(Long id, Long productId, String skuCode, int quantity, boolean inStock) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.quantity = quantity;
        this.inStock = inStock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }
}
