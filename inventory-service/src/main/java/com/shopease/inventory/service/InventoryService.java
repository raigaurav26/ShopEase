package com.shopease.inventory.service;

import com.shopease.inventory.dto.InventoryRequest;
import com.shopease.inventory.dto.InventoryResponse;
import com.shopease.inventory.model.Inventory;
import com.shopease.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryResponse addStock(InventoryRequest request) {
        Inventory inventory = new Inventory();
        inventory.setProductId(request.getProductId());
        inventory.setSkuCode(request.getSkuCode());
        inventory.setQuantity(request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    public InventoryResponse getStockByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for productId: " + productId));
        return mapToResponse(inventory);
    }

    public boolean isInStock(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for productId: " + productId));
        return inventory.getQuantity() > 0;
    }

    public InventoryResponse reduceStock(Long productId, int qty) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for productId: " + productId));
        if (inventory.getQuantity() < qty) {
            throw new RuntimeException("Not enough stock for productId: " + productId
                    + ". Available: " + inventory.getQuantity() + ", Requested: " + qty);
        }
        inventory.setQuantity(inventory.getQuantity() - qty);
        Inventory updated = inventoryRepository.save(inventory);
        return mapToResponse(updated);
    }

    public List<InventoryResponse> getAllStock() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
