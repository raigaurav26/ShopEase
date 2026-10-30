package com.shopease.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int available, int requested) {
        super("Not enough stock for productId: " + productId
                + ". Available: " + available + ", Requested: " + requested);
    }
}
