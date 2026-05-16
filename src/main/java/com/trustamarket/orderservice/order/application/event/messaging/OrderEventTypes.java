package com.trustamarket.orderservice.order.application.event.messaging;

// order 도메인이 발행하는 OutboxEvent 의 eventType 문자열 상수.
// application 과 adapter 양쪽이 본 클래스만 의존 — adapter 직접 참조 회피 (헥사고날 경계 보존).
// 신규 이벤트 추가 시 여기 + adapter/out/messaging/outbox/OutboxEventListener 의 topic 매핑 + application.yaml 토픽 키 함께 갱신.
public final class OrderEventTypes {

    public static final String SETTLEMENT_REQUESTED         = "ORDER.SETTLEMENT_REQUESTED";
    public static final String PRODUCT_SOLD_OUT             = "ORDER.PRODUCT_SOLD_OUT";
    public static final String ORDER_CANCELLATION_REQUESTED = "ORDER.CANCELLATION_REQUESTED";
    public static final String ORDER_PAID                   = "ORDER.PAID";

    private OrderEventTypes() {}
}
