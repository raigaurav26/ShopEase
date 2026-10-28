package com.shopease.order.service;

import com.shopease.order.client.InventoryClient;
import com.shopease.order.client.PaymentClient;
import com.shopease.order.client.ProductClient;
import com.shopease.order.dto.OrderItemRequest;
import com.shopease.order.dto.OrderRequest;
import com.shopease.order.dto.OrderResponse;
import com.shopease.order.dto.PaymentRequest;
import com.shopease.order.dto.ProductDto;
import com.shopease.order.event.OrderPlacedEvent;
import com.shopease.order.kafka.OrderProducer;
import com.shopease.order.model.Order;
import com.shopease.order.model.OrderItem;
import com.shopease.order.repository.OrderItemRepository;
import com.shopease.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderItem orderItem;
    private ProductDto product;

    @BeforeEach
    void setUp() {
        order = new Order(100L, 159998.0, "Kalyan, Maharashtra");
        order.setId(1L);
        order.setStatus("PLACED");

        orderItem = new OrderItem(1L, 10L, "Laptop", 2, 79999.0);

        product = new ProductDto();
        product.setId(10L);
        product.setName("Laptop");
        product.setPrice(79999.0);
    }

    private OrderRequest orderRequest(int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(10L);
        item.setQuantity(quantity);

        OrderRequest request = new OrderRequest();
        request.setUserId(100L);
        request.setShippingAddress("Kalyan, Maharashtra");
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void placeOrder_ShouldChargeReduceStockAndPublishEvent_WhenInStock() {
        when(productClient.getProductById(10L)).thenReturn(product);
        when(inventoryClient.checkStock(10L)).thenReturn(Map.of("inStock", true));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.placeOrder(orderRequest(2));

        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(159998.0, response.getTotalAmount());
        assertEquals(1, response.getItems().size());

        verify(inventoryClient).reduceStock(10L, 2);

        ArgumentCaptor<PaymentRequest> payment = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentClient).processPayment(payment.capture());
        assertEquals(1L, payment.getValue().getOrderId());
        assertEquals(159998.0, payment.getValue().getAmount());

        verify(orderProducer).sendOrderPlacedEvent(any(OrderPlacedEvent.class));
    }

    @Test
    void placeOrder_ShouldThrowAndNotCharge_WhenOutOfStock() {
        when(productClient.getProductById(10L)).thenReturn(product);
        when(inventoryClient.checkStock(10L)).thenReturn(Map.of("inStock", false));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.placeOrder(orderRequest(1)));

        assertTrue(ex.getMessage().contains("out of stock"));
        verify(orderRepository, never()).save(any());
        verify(inventoryClient, never()).reduceStock(anyLong(), anyInt());
        verifyNoInteractions(paymentClient, orderProducer);
    }

    @Test
    void placeOrder_ShouldReject_WhenNoItems() {
        OrderRequest request = orderRequest(1);
        request.setItems(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder(request));
        verifyNoInteractions(productClient, inventoryClient, paymentClient, orderRepository, orderProducer);
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(orderItem));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PLACED", response.getStatus());
        assertEquals("Laptop", response.getItems().get(0).getProductName());
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
