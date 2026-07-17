package com.shopease.order.service;

import com.shopease.order.client.InventoryClient;
import com.shopease.order.client.PaymentClient;
import com.shopease.order.client.ProductClient;
import com.shopease.order.dto.OrderRequest;
import com.shopease.order.dto.OrderResponse;
import com.shopease.order.model.Order;
import com.shopease.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setUserId(100L);
        order.setProductId(10L);
        order.setQuantity(2);
        order.setTotalPrice(159998.0);
        order.setStatus("PLACED");
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PLACED", response.getStatus());
    }

    @Test
    void getOrderById_ShouldThrowException_WhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    void getAllOrders_ShouldReturnList() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList(order));

        List<OrderResponse> orders = orderService.getAllOrders();

        assertEquals(1, orders.size());
        assertEquals(100L, orders.get(0).getUserId());
    }

    @Test
    void getOrdersByUserId_ShouldReturnUserOrders() {
        when(orderRepository.findByUserId(100L)).thenReturn(Arrays.asList(order));

        List<OrderResponse> orders = orderService.getOrdersByUserId(100L);

        assertEquals(1, orders.size());
        assertEquals(100L, orders.get(0).getUserId());
    }
}
