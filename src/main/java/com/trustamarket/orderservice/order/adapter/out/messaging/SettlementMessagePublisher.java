package com.trustamarket.orderservice.order.adapter.out.messaging;

import com.trustamarket.orderservice.order.application.port.out.SettlementMessagePort;
import com.trustamarket.orderservice.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// 정산 요청 Kafka 발행 — wallet-service 의 PointSettlementListener 가 구독.
// SettlementMessagePort 구현 — application 레이어가 본 adapter 클래스에 직접 의존하지 않도록 분리.
// MVP: confirm 이 아닌 PAID 시점에 즉시 발행 (배송 이벤트 컨슈머가 아직 없어 confirm 도달 불가).
// 운영 전환 시: ConfirmOrderService 로 이동 + Outbox 패턴 도입 (트랜잭션 commit 후 발행 보장).
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementMessagePublisher implements SettlementMessagePort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${trusta.messaging.topic.settlement-requested}")
    private String topic;

    @Override
    public void publishForPaidOrder(Order order) {
        UUID eventId = UUID.randomUUID();
        SettlePointSettlementMessage message = new SettlePointSettlementMessage(
                eventId,
                order.getId().value(),
                order.getBuyer().id(),
                order.getSeller().id(),
                order.getProduct().id(),
                order.getProduct().name(),
                order.getProduct().price().value(),
                order.getShippingFee().value(),
                order.getTotalAmount().value(),
                Instant.now()
        );

        kafkaTemplate.send(topic, order.getId().value().toString(), message)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("[Settlement] 발행 실패 — eventId={}, orderId={}",
                                eventId, order.getId().value(), ex);
                    } else {
                        log.info("[Settlement] 발행 — eventId={}, orderId={}, totalAmount={}",
                                eventId, order.getId().value(), order.getTotalAmount().value());
                    }
                });
    }
}
