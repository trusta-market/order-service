package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;

import java.util.List;
import java.util.UUID;

// 상태 이력 조회 — GET /api/orders/{id}/status-history (ADMIN)
// 한 주문당 history 적어 페이징 X — List 반환 (created_at 오름차순)
public interface GetStatusHistoryUseCase {

    List<OrderStatusHistory> getHistory(UUID orderId);
}
