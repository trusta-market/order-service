package com.trustamarket.orderservice.order.adapter.out.messaging;

import java.time.Instant;
import java.util.UUID;

// wallet-service `order.wallet-settlement.requested` 토픽 컨슈머의 payload 와 시그니처 매칭.
// 필드 이름/순서/타입 변경 시 양쪽 동시 수정 필요 (스키마 계약).
// productName 은 wallet/settlement 측 향후 spec 에 포함 예정 — 미리 발행 (consumer 가 unknown 필드면 무시).
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
) {}
