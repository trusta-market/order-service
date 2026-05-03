package com.trustamarket.orderservice.order.application.dto.result;

import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;

import java.util.UUID;

// 상태 이력 read-model — admin status-history 응답.
public record OrderStatusHistoryView(
        UUID id,
        OrderStatus prevStatus,    // nullable — 최초 생성(REQUESTED)에는 이전 상태 없음
        OrderStatus nextStatus,
        String reason              // nullable — 사유 있는 전이만
) {
    public OrderStatusHistoryView {
        if (id == null) throw new IllegalArgumentException("id");
        if (nextStatus == null) throw new IllegalArgumentException("nextStatus");
        // prevStatus 는 의도적으로 null 허용 (최초 생성)
        if (reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("reason 은 빈 문자열이면 안됩니다.");
        }
    }

    public static OrderStatusHistoryView from(OrderStatusHistory h) {
        if (h == null) throw new IllegalArgumentException("h");
        return new OrderStatusHistoryView(
                h.id(),
                h.prevStatus(),
                h.nextStatus(),
                h.reason() == null ? null : h.reason().value()
        );
    }
}
