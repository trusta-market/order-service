package com.trustamarket.orderservice.order.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.orderservice.order.adapter.in.messaging.dto.DeliveryStartedMessage;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxJpaRepository;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxPurpose;
import com.trustamarket.orderservice.order.application.port.in.MarkOrderShippedUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// delivery.started 토픽 consume → PAID → SHIPPING 전이.
// 멱등성: (event_id, consumer_group) UNIQUE 로 중복 메시지 차단.
// payload 는 String JSON 으로 받아서 ObjectMapper 로 record 변환 (토픽별 다른 타입 대응).
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStartedListener {

    private static final String CONSUMER_GROUP = "order-delivery-started-group";

    private final MarkOrderShippedUseCase markOrderShippedUseCase;
    private final InboxJpaRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${trusta.messaging.topic.delivery-started:delivery.started}",
            groupId = CONSUMER_GROUP)
    @Transactional
    public void consume(String json, Acknowledgment ack) {
        DeliveryStartedMessage message;
        try {
            message = objectMapper.readValue(json, DeliveryStartedMessage.class);
        } catch (Exception e) {
            // payload 자체가 깨졌으면 재배달해도 결과 동일 — ack + skip (poison pill 방지).
            log.error("[DeliveryStarted] payload 파싱 실패 — ack + skip. raw={}", json, e);
            ack.acknowledge();
            return;
        }

        // 멱등성 체크
        if (inboxRepository.findByEventIdAndConsumerGroup(message.eventId(), CONSUMER_GROUP).isPresent()) {
            log.info("[DeliveryStarted] 중복 메시지 — ack + skip. eventId={}, orderId={}",
                    message.eventId(), message.orderId());
            ack.acknowledge();
            return;
        }

        try {
            log.info("[DeliveryStarted] consume — eventId={}, orderId={}", message.eventId(), message.orderId());
            markOrderShippedUseCase.markShipped(message.orderId());
            inboxRepository.save(InboxJpaEntity.forKafkaEvent(
                    message.eventId(), CONSUMER_GROUP, InboxPurpose.DELIVERY_STARTED));
            ack.acknowledge();
        } catch (com.trustamarket.orderservice.order.domain.exception.OrderException e) {
            // 비즈니스 예외 (Order not found / Invalid transition) — 재시도해도 결과 동일. ack + skip.
            log.warn("[DeliveryStarted] non-retryable, ack + skip — eventId={}, orderId={}",
                    message.eventId(), message.orderId(), e);
            ack.acknowledge();
        } catch (Exception e) {
            // 통신/일시 오류 — ack 안 함 → 재시도. 영구 실패 시 향후 DLT.
            log.error("[DeliveryStarted] 처리 실패 — eventId={}, orderId={}",
                    message.eventId(), message.orderId(), e);
            throw e;
        }
    }
}
