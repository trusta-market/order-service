package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.model.Order;

// 정산 요청 발행 port — application 레이어가 adapter (Kafka publisher) 에 직접 의존하지 않도록 분리.
// 구현은 adapter/out/messaging 의 SettlementMessagePublisher.
// MVP: 직접 발행. 운영 전환 시 Outbox 패턴 (common Events.trigger) 으로 교체 — port 시그니처는 유지.
public interface SettlementMessagePort {

    void publishForPaidOrder(Order order);
}
