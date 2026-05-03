package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;

import java.util.UUID;

// 상태 이력 응답 — GET /api/v1/admin/orders/{id}/status-history (ADMIN)
public record OrderStatusHistoryResponse(
        UUID id,
        OrderStatus prevStatus,    // nullable — 최초 생성 시 없음
        OrderStatus nextStatus,
        String reason              // nullable — 사유 있는 전이만
) {
    public static OrderStatusHistoryResponse from(OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
                history.id(),
                history.prevStatus(),
                history.nextStatus(),
                history.reason() == null ? null : history.reason().value()
        );
    }
}
