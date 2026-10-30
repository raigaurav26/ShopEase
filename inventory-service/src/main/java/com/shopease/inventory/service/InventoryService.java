package com.shopease.inventory.service;

import com.shopease.inventory.dto.InventoryRequest;
import com.shopease.inventory.dto.InventoryResponse;
import com.shopease.inventory.exception.InsufficientStockException;
import com.shopease.inventory.exception.InventoryNotFoundException;
import com.shopease.inventory.model.Inventory;
import com.shopease.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse addStock(InventoryRequest request) {
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setSkuCode(request.getSkuCode());
        inventory.setQuantity(request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    public InventoryResponse getStockByProductId(Long productId) {
        return mapToResponse(findInventory(productId));
    }

    public boolean isInStock(Long productId) {
        return findInventory(productId).getQuantity() > 0;
    }

    @Transactional
    public InventoryResponse reduceStock(Long productId, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("qty must be greater than 0");
        }
        int updated = inventoryRepository.decrementIfAvailable(productId, qty);
        Inventory inventory = findInventory(productId);
        if (updated == 0) {
            throw new InsufficientStockException(productId, inventory.getQuantity(), qty);
        }
        return mapToResponse(inventory);
    }

    public List<InventoryResponse> getAllStock() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Inventory findInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        InventoryResponse response = new InventoryResponse();
        response.setId(inventory.getId());
        response.setProductId(inventory.getProductId());
        response.setSkuCode(inventory.getSkuCode());
        response.setQuantity(inventory.getQuantity());
        response.setInStock(inventory.getQuantity() > 0);
        return response;
    }
}
