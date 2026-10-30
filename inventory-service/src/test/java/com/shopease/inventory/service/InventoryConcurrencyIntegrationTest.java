package com.shopease.inventory.service;

import com.shopease.inventory.exception.InsufficientStockException;
import com.shopease.inventory.model.Inventory;
import com.shopease.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InventoryConcurrencyIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        inventoryRepository.save(new Inventory(null, 42L, "SKU-42", 10));
    }

    @Test
    void concurrentOrdersNeverOversell() throws Exception {
        int buyers = 25;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < buyers; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    inventoryService.reduceStock(42L, 1);
                    return true;
                } catch (InsufficientStockException e) {
                    return false;
                }
            }));
        }
        start.countDown();

        int succeeded = 0;
        for (Future<Boolean> result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                succeeded++;
            }
        }
        pool.shutdown();

        assertEquals(10, succeeded);
        assertEquals(0, inventoryRepository.findByProductId(42L).orElseThrow().getQuantity());
    }

    @Test
    void reduceStock_persistsNewQuantity() {
        assertEquals(7, inventoryService.reduceStock(42L, 3).getQuantity());
        assertEquals(7, inventoryRepository.findByProductId(42L).orElseThrow().getQuantity());
    }
}
