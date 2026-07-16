package com.shopease.inventory.dto;

public class InventoryRequest {

    private Long productId;
    private String skuCode;
    private int quantity;

    public InventoryRequest() {
    }

    public InventoryRequest(Long productId, String skuCode, int quantity) {
        this.productId = productId;
        this.skuCode = skuCode;
        this.quantity = quantity;
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
}
