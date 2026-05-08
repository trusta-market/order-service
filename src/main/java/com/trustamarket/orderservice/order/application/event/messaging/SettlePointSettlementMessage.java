package com.trustamarket.orderservice.order.application.event.messaging;

import java.time.Instant;
import java.util.UUID;

// wallet-service `order.wallet-settlement.requested` 토픽 컨슈머의 payload 와 시그니처 매칭.
// 필드 이름/순서/타입 변경 시 양쪽 동시 수정 필요 (스키마 계약).
//
// 위치: application/event/messaging — outbound adapter 가 도메인 객체 직접 의존하지 않게 application 레이어 record 로 둠.
// factory 시그니처는 primitive — 도메인 의존 차단.
public record SettlePointSettlementMessage(
        UUID    eventId,           // 멱등성 키 — wallet 측 existsByEventId 로 중복 차단
        UUID    orderId,
        UUID    buyerId,
        UUID    sellerId,
        UUID    productId,
        String  productName,       // 정산 시 상품 이름 표시/감사 — settlement spec 추가 예정
        long    productPrice,
        long    shippingFee,
        long    totalAmount,
        Instant orderConfirmedAt
) {
    public static SettlePointSettlementMessage of(
            UUID orderId,
            UUID buyerId,
            UUID sellerId,
            UUID productId,
            String productName,
            long productPrice,
            long shippingFee,
            long totalAmount,
            Instant orderConfirmedAt
    ) {
        return new SettlePointSettlementMessage(
                UUID.randomUUID(),
                orderId, buyerId, sellerId, productId,
                productName, productPrice, shippingFee, totalAmount,
                orderConfirmedAt
        );
    }
}
