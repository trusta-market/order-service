package com.trustamarket.orderservice.order.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.orderservice.order.adapter.in.messaging.dto.DeliveryStartedMessage;
import com.trustamarket.orderservice.order.application.port.in.MarkOrderShippedUseCase;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository.InboxPurposeKey;
import com.trustamarket.orderservice.order.domain.exception.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.trustamarket.common.messaging.IdempotentConsumer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// delivery.started 토픽 consume → PAID → SHIPPING 전이.
// 멱등성: (event_id, consumer_group) 로 inbox dedup. application port 만 의존.
// ack 순서: happy path 는 트랜잭션 commit 후 (afterCommit hook) 실행 — DB commit 실패 시 재배달 보장.
//   non-retryable (parsing fail / business 예외 / 중복) 은 즉시 ack — commit 결과와 무관.
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStartedListener {

    private static final String CONSUMER_GROUP = "order-delivery-started-group";

    private final MarkOrderShippedUseCase markOrderShippedUseCase;
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @IdempotentConsumer(CONSUMER_GROUP)
    @KafkaListener(topics = "${trusta.messaging.topic.delivery-started:delivery.started}",
            groupId = CONSUMER_GROUP)
    @Transactional
    public void consume(String json, Acknowledgment ack) {
        DeliveryStartedMessage message;
        try {
            message = objectMapper.readValue(json, DeliveryStartedMessage.class);
        } catch (Exception e) {
            // payload 자체가 깨졌으면 재배달해도 동일 결과 — ack + skip (poison pill 차단).
            // raw JSON 로그 X (PII 보호). 길이 / 위치 / 예외 타입만 기록.
            log.error("[DeliveryStarted] payload 파싱 실패 — ack + skip (length={})", json == null ? -1 : json.length(), e);
            ack.acknowledge();
            return;
        }

        // 멱등성 — atomic INSERT 시도. 이미 처리된 메시지면 false → ack + skip.
        if (!inboxRepository.tryRecordKafkaEvent(message.eventId(), CONSUMER_GROUP, InboxPurposeKey.DELIVERY_STARTED)) {
            log.info("[DeliveryStarted] 중복 메시지 — ack + skip. eventId={}, orderId={}",
                    message.eventId(), message.orderId());
            ack.acknowledge();
            return;
        }

        try {
            log.info("[DeliveryStarted] consume — eventId={}, orderId={}", message.eventId(), message.orderId());
            markOrderShippedUseCase.markShipped(message.orderId());
            ackAfterCommit(ack);
        } catch (OrderException e) {
            // 비즈니스 예외 (Order not found / Invalid transition) — 재시도해도 동일 결과. 즉시 ack + skip.
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

    // Kafka offset commit 을 DB 트랜잭션 commit 이후로 지연 — DB 실패 시 재배달 보장.
    private static void ackAfterCommit(Acknowledgment ack) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ack.acknowledge();
            }
        });
    }
}
