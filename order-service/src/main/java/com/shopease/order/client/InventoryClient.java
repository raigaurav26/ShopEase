package com.shopease.order.client;

import com.shopease.order.dto.InventoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventory/{productId}/check")
    Map<String, Boolean> checkStock(@PathVariable Long productId);

    @PutMapping("/api/inventory/{productId}/reduce")
    InventoryDto reduceStock(@PathVariable Long productId, @RequestParam int qty);
}
