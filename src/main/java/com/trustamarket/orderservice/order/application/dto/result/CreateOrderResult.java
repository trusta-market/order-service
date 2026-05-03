package com.trustamarket.orderservice.order.application.dto.result;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

// 주문 생성 결과 read-model — application 레이어가 도메인을 매핑해 반환.
// adapter/in/web 은 본 record 만 보고 Response 를 만든다 (도메인 직접 접근 차단).
public record CreateOrderResult(
        UUID orderId,
        OrderStatus status,
        long totalAmount,
        Instant createdAt
) {
    public CreateOrderResult {
        if (orderId == null) throw new IllegalArgumentException("orderId");
        if (status == null) throw new IllegalArgumentException("status");
        if (totalAmount < 0) throw new IllegalArgumentException("totalAmount must be >= 0");
        // createdAt 은 JPA Auditing 이 commit 시점에 채움 — read-model 매핑 시점엔 null 가능 (fail-fast 회피)
    }

    public static CreateOrderResult from(Order order) {
        return new CreateOrderResult(
                order.getId().value(),
                order.getStatus(),
                order.getTotalAmount().value(),
                order.getCreatedAt()
        );
    }
}
