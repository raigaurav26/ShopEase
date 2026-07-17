package com.shopease.order.kafka;

import com.shopease.order.event.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void sendOrderPlacedEvent(OrderPlacedEvent event) {
        kafkaTemplate.send("order-placed", event);
    }
}
