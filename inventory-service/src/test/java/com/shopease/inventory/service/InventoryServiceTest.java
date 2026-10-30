package com.shopease.inventory.service;

import com.shopease.inventory.dto.InventoryRequest;
import com.shopease.inventory.dto.InventoryResponse;
import com.shopease.inventory.exception.InsufficientStockException;
import com.shopease.inventory.exception.InventoryNotFoundException;
import com.shopease.inventory.model.Inventory;
import com.shopease.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void addStock_savesAndReturnsResponse() {
        when(inventoryRepository.save(any(Inventory.class)))
                .thenReturn(new Inventory(1L, 10L, "SKU-10", 5));

        InventoryResponse response = inventoryService.addStock(new InventoryRequest(10L, "SKU-10", 5));

        assertEquals(1L, response.getId());
        assertEquals(5, response.getQuantity());
        assertTrue(response.isInStock());
    }

    @Test
    void addStock_rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.addStock(new InventoryRequest(10L, "SKU-10", -1)));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void addStock_rejectsMissingProductId() {
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.addStock(new InventoryRequest(null, "SKU-10", 3)));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void getStockByProductId_unknownProduct_throwsNotFound() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.getStockByProductId(99L));
    }

    @Test
    void isInStock_falseWhenQuantityIsZero() {
        when(inventoryRepository.findByProductId(10L))
                .thenReturn(Optional.of(new Inventory(1L, 10L, "SKU-10", 0)));

        assertFalse(inventoryService.isInStock(10L));
    }

    @Test
    void reduceStock_returnsUpdatedQuantity() {
        when(inventoryRepository.decrementIfAvailable(10L, 2)).thenReturn(1);
        when(inventoryRepository.findByProductId(10L))
                .thenReturn(Optional.of(new Inventory(1L, 10L, "SKU-10", 3)));

        InventoryResponse response = inventoryService.reduceStock(10L, 2);

        assertEquals(3, response.getQuantity());
    }

    @Test
    void reduceStock_notEnoughStock_throwsInsufficientStock() {
        when(inventoryRepository.decrementIfAvailable(10L, 5)).thenReturn(0);
        when(inventoryRepository.findByProductId(10L))
                .thenReturn(Optional.of(new Inventory(1L, 10L, "SKU-10", 2)));

        InsufficientStockException ex = assertThrows(InsufficientStockException.class,
                () -> inventoryService.reduceStock(10L, 5));
        assertTrue(ex.getMessage().contains("Available: 2"));
    }

    @Test
    void reduceStock_unknownProduct_throwsNotFound() {
        when(inventoryRepository.decrementIfAvailable(99L, 1)).thenReturn(0);
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.reduceStock(99L, 1));
    }

    @Test
    void reduceStock_rejectsZeroOrNegativeQty() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reduceStock(10L, 0));
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reduceStock(10L, -3));
        verify(inventoryRepository, never()).decrementIfAvailable(anyLong(), anyInt());
    }

    @Test
    void getAllStock_mapsEveryRow() {
        List<Inventory> rows = Arrays.asList(
                new Inventory(1L, 10L, "SKU-10", 4),
                new Inventory(2L, 11L, "SKU-11", 0));
        when(inventoryRepository.findAll()).thenReturn(rows);

        List<InventoryResponse> all = inventoryService.getAllStock();

        assertEquals(2, all.size());
        assertTrue(all.get(0).isInStock());
        assertFalse(all.get(1).isInStock());
    }
}
