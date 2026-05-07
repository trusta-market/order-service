package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.common.event.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

// Events.trigger(OutboxEvent) → Spring ApplicationEventPublisher → 본 listener 가 받아서 outbox 테이블 INSERT.
// phase = BEFORE_COMMIT — 같은 트랜잭션에 묶여서 도메인 commit 과 outbox INSERT 가 atomic.
//   outbox INSERT fail 시 도메인 트랜잭션도 함께 rollback → DB 일관성 100%.
//
// eventType → topic 매핑은 본 클래스 내 TOPIC_MAP 으로 관리. 새 이벤트 추가 시 매핑 한 줄 추가.
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private static final Map<String, String> TOPIC_MAP = Map.of(
            "ORDER.SETTLEMENT_REQUESTED", "order.wallet-settlement.requested",
            "ORDER.PRODUCT_SOLD_OUT",     "order.product.sold-out"
    );

    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${trusta.messaging.topic.settlement-requested:order.wallet-settlement.requested}")
    private String settlementTopicOverride;
    @Value("${trusta.messaging.topic.product-sold-out:order.product.sold-out}")
    private String productSoldOutTopicOverride;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onEvent(OutboxEvent event) {
        String topic = resolveTopic(event.eventType());
        String payloadJson = serialize(event.payload());

        OutboxJpaEntity row = OutboxJpaEntity.builder()
                .id(UUID.randomUUID())
                .correlationId(event.correlationId())
                .domainType(event.domainType())
                .domainId(event.domainId())
                .eventType(event.eventType())
                .topic(topic)
                .payload(payloadJson)
                .build();

        outboxRepository.save(row);
        log.debug("[Outbox] enqueue — correlationId={}, eventType={}, topic={}",
                event.correlationId(), event.eventType(), topic);
    }

    private String resolveTopic(String eventType) {
        // application.yml 의 trusta.messaging.topic.* 값을 우선 사용 (운영 토픽 변경 대비).
        return switch (eventType) {
            case "ORDER.SETTLEMENT_REQUESTED" -> settlementTopicOverride;
            case "ORDER.PRODUCT_SOLD_OUT"     -> productSoldOutTopicOverride;
            default -> TOPIC_MAP.getOrDefault(eventType,
                    "order.unknown." + eventType.toLowerCase().replace('.', '-'));
        };
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // listener 안의 직렬화 실패는 도메인 트랜잭션을 같이 깨야 함 — runtime 으로 변환.
            throw new IllegalStateException("Outbox payload 직렬화 실패: eventType=" + payload.getClass().getName(), e);
        }
    }
}
