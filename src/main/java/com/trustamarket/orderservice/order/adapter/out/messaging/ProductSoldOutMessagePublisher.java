package com.trustamarket.orderservice.order.adapter.out.messaging;

import com.trustamarket.orderservice.order.application.port.out.ProductSoldOutMessagePort;
import com.trustamarket.orderservice.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSoldOutMessagePublisher implements ProductSoldOutMessagePort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${trusta.messaging.topic.product-sold-out}")
    private String topic;

    @Override
    public void publishForConfirmedOrder(Order order) {
        UUID eventId = UUID.randomUUID();
        ProductSoldOutMessage message = new ProductSoldOutMessage(
                eventId,
                order.getId().value(),
                order.getProduct().id(),
                Instant.now()
        );

        kafkaTemplate.send(topic, order.getProduct().id().toString(), message)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("[ProductSoldOut] 발행 실패 — eventId={}, productId={}",
                                eventId, order.getProduct().id(), ex);
                    } else {
                        log.info("[ProductSoldOut] 발행 — eventId={}, productId={}",
                                eventId, order.getProduct().id());
                    }
                });
    }
}
