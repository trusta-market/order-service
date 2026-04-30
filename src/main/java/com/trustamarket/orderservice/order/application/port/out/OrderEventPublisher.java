package com.trustamarket.orderservice.order.application.port.out;

// 도메인 이벤트 발행 port — 구현은 후속 PR (Outbox 패턴 + Kafka)
// 본 PR에선 인터페이스만, application service가 publish 호출 자리 마련
// 도메인 이벤트 record(PurchaseConfirmedEvent 등)는 후속 PR에서 정의 - marker interface로 처리
public interface OrderEventPublisher {

    void publish(DomainEvent event);

    // marker — 후속 PR에서 sealed interface로 도메인 이벤트 record들 묶을 예정
    interface DomainEvent {}
}
