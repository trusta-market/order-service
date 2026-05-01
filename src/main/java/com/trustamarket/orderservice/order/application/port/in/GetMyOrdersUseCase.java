package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 내 주문 목록 조회 — GET /api/orders/me
// buyer + seller 통합 — 사용자가 buyer 또는 seller로 참여한 모든 주문
public interface GetMyOrdersUseCase {

    Page<Order> getMyOrders(GetMyOrdersQuery query);

    record GetMyOrdersQuery(
            UUID actorId,
            Pageable pageable
    ) {}
}
