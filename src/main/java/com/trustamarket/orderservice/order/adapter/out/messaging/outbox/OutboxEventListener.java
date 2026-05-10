package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.common.event.OutboxEvent;
import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

// Events.trigger(OutboxEvent) → Spring ApplicationEventPublisher → 본 listener 가 받아서 outbox 테이블 INSERT.
// phase = BEFORE_COMMIT — 같은 트랜잭션에 묶여서 도메인 commit 과 outbox INSERT 가 atomic.
//   outbox INSERT fail 시 도메인 트랜잭션도 함께 rollback → DB 일관성 100%.
//
// eventType → topic 매핑은 본 클래스 내 switch — 새 이벤트 타입 추가 시 case + application.yaml 토픽 키 추가.
// 매핑 누락된 eventType 은 fail-fast (silent routing 차단).
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    // eventType 문자열 — 발행자와 공유. 신규 추가 시 양쪽 + topic 매핑 함께 변경.
    public static final String EVENT_SETTLEMENT_REQUESTED         = "ORDER.SETTLEMENT_REQUESTED";
    public static final String EVENT_PRODUCT_SOLD_OUT             = "ORDER.PRODUCT_SOLD_OUT";
    public static final String EVENT_ORDER_CANCELLATION_REQUESTED = "ORDER.CANCELLATION_REQUESTED";

    private final OutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${trusta.messaging.topic.settlement-requested:order.wallet-settlement.requested}")
    private String settlementTopic;
    @Value("${trusta.messaging.topic.product-sold-out:order.product.sold-out}")
    private String productSoldOutTopic;
    @Value("${trusta.messaging.topic.order-cancellation-requested:order.cancellation.requested}")
    private String orderCancellationRequestedTopic;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onEvent(OutboxEvent event) {
        String topic = resolveTopic(event.eventType());
        String payloadJson = serialize(event);

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

    // 매핑 누락 시 silent fallback 금지 — 잘못된 토픽 발행 사고를 막기 위해 fail-fast.
    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case EVENT_SETTLEMENT_REQUESTED         -> settlementTopic;
            case EVENT_PRODUCT_SOLD_OUT             -> productSoldOutTopic;
            case EVENT_ORDER_CANCELLATION_REQUESTED -> orderCancellationRequestedTopic;
            default -> throw new IllegalStateException(
                    "Outbox 토픽 매핑 누락: eventType=" + eventType + ". switch case + application.yaml 토픽 키 추가 필요.");
        };
    }

    private String serialize(OutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(event.payload());
        } catch (JsonProcessingException e) {
            // listener 안의 직렬화 실패는 도메인 트랜잭션을 같이 깨야 함 — runtime 으로 변환.
            throw new IllegalStateException(
                    "Outbox payload 직렬화 실패: eventType=" + event.eventType()
                            + ", payloadType=" + (event.payload() == null ? "null" : event.payload().getClass().getName()), e);
        }
    }
}
