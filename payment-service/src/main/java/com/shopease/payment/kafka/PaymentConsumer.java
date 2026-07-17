package com.shopease.payment.kafka;

import com.shopease.payment.dto.PaymentRequest;
import com.shopease.payment.event.OrderPlacedEvent;
import com.shopease.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentConsumer {

    @Autowired
    private PaymentService paymentService;

    @KafkaListener(topics = "order-placed", groupId = "payment-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(event.getOrderId());
        request.setAmount(event.getTotalPrice());
        request.setPaymentMethod(event.getPaymentMethod() != null ? event.getPaymentMethod() : "UPI");

        paymentService.processPayment(request);
    }
}
