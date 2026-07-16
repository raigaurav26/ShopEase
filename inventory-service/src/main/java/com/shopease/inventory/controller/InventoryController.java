package com.shopease.inventory.controller;

import com.shopease.inventory.dto.InventoryRequest;
import com.shopease.inventory.dto.InventoryResponse;
import com.shopease.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> addStock(@RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.addStock(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllStock() {
        return ResponseEntity.ok(inventoryService.getAllStock());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getStockByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStockByProductId(productId));
    }

    @GetMapping("/{productId}/check")
    public ResponseEntity<Map<String, Boolean>> isInStock(@PathVariable Long productId) {
        boolean inStock = inventoryService.isInStock(productId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("inStock", inStock);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{productId}/reduce")
    public ResponseEntity<InventoryResponse> reduceStock(@PathVariable Long productId, @RequestParam int qty) {
        return ResponseEntity.ok(inventoryService.reduceStock(productId, qty));
    }
}
