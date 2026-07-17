package com.shopease.order.service;

import com.shopease.order.client.InventoryClient;
import com.shopease.order.client.PaymentClient;
import com.shopease.order.client.ProductClient;
import com.shopease.order.dto.*;
import com.shopease.order.event.OrderPlacedEvent;
import com.shopease.order.kafka.OrderProducer;
import com.shopease.order.model.Order;
import com.shopease.order.model.OrderItem;
import com.shopease.order.repository.OrderItemRepository;
import com.shopease.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private PaymentClient paymentClient;

    @Autowired
    private OrderProducer orderProducer;

    public OrderResponse placeOrder(OrderRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        for (OrderItemRequest itemReq : request.getItems()) {
            ProductDto product = productClient.getProductById(itemReq.getProductId());

            Map<String, Boolean> stockResult = inventoryClient.checkStock(itemReq.getProductId());
            if (stockResult.get("inStock") == null || !stockResult.get("inStock")) {
                throw new RuntimeException("Product out of stock: " + product.getName());
            }

            double itemTotal = product.getPrice() * itemReq.getQuantity();
            totalAmount += itemTotal;

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(itemReq.getQuantity());
            item.setPrice(product.getPrice());
            orderItems.add(item);
        }

        Order order = new Order(request.getUserId(), totalAmount, request.getShippingAddress());
        order = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemRepository.save(item);
        }

        for (OrderItemRequest itemReq : request.getItems()) {
            inventoryClient.reduceStock(itemReq.getProductId(), itemReq.getQuantity());
        }

        PaymentRequest paymentReq = new PaymentRequest(order.getId(), totalAmount, "CARD");
        paymentClient.processPayment(paymentReq);

        order.setStatus("CONFIRMED");
        orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(),
                orderItems.get(0).getProductId(),
                orderItems.get(0).getQuantity(),
                totalAmount,
                "CARD"
        );
        orderProducer.sendOrderPlacedEvent(event);

        return mapToResponse(order);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                order.getShippingAddress(),
                itemResponses);
    }
}
