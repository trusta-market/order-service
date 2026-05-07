package com.trustamarket.orderservice.order.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.orderservice.order.adapter.in.messaging.dto.DeliveryCompletedMessage;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxJpaRepository;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxPurpose;
import com.trustamarket.orderservice.order.application.port.in.MarkOrderDeliveredUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// delivery.completed 토픽 consume → SHIPPING → DELIVERED 전이.
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCompletedListener {

    private static final String CONSUMER_GROUP = "order-delivery-completed-group";

    private final MarkOrderDeliveredUseCase markOrderDeliveredUseCase;
    private final InboxJpaRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${trusta.messaging.topic.delivery-completed:delivery.completed}",
            groupId = CONSUMER_GROUP)
    @Transactional
    public void consume(String json, Acknowledgment ack) {
        DeliveryCompletedMessage message;
        try {
            message = objectMapper.readValue(json, DeliveryCompletedMessage.class);
        } catch (Exception e) {
            log.error("[DeliveryCompleted] payload 파싱 실패 — ack + skip. raw={}", json, e);
            ack.acknowledge();
            return;
        }

        if (inboxRepository.findByEventIdAndConsumerGroup(message.eventId(), CONSUMER_GROUP).isPresent()) {
            log.info("[DeliveryCompleted] 중복 메시지 — ack + skip. eventId={}, orderId={}",
                    message.eventId(), message.orderId());
            ack.acknowledge();
            return;
        }

        try {
            log.info("[DeliveryCompleted] consume — eventId={}, orderId={}", message.eventId(), message.orderId());
            markOrderDeliveredUseCase.markDelivered(message.orderId());
            inboxRepository.save(InboxJpaEntity.forKafkaEvent(
                    message.eventId(), CONSUMER_GROUP, InboxPurpose.DELIVERY_COMPLETED));
            ack.acknowledge();
        } catch (com.trustamarket.orderservice.order.domain.exception.OrderException e) {
            log.warn("[DeliveryCompleted] non-retryable, ack + skip — eventId={}, orderId={}",
                    message.eventId(), message.orderId(), e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[DeliveryCompleted] 처리 실패 — eventId={}, orderId={}",
                    message.eventId(), message.orderId(), e);
            throw e;
        }
    }
}
