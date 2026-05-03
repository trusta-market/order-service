package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.application.dto.result.OrderStatusHistoryView;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;

import java.util.UUID;

// 상태 이력 응답 — GET /api/v1/admin/orders/{id}/status-history (ADMIN)
public record OrderStatusHistoryResponse(
        UUID id,
        OrderStatus prevStatus,    // nullable — 최초 생성 시 없음
        OrderStatus nextStatus,
        String reason              // nullable — 사유 있는 전이만
) {
    public static OrderStatusHistoryResponse from(OrderStatusHistoryView v) {
        return new OrderStatusHistoryResponse(
                v.id(),
                v.prevStatus(),
                v.nextStatus(),
                v.reason()
        );
    }
}
