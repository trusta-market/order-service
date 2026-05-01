package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.model.Order;

import java.util.UUID;

// 주문 단건 조회 — GET /api/orders/{id}
// 권한: buyer 또는 seller 본인만 조회 가능 (ADMIN은 SearchOrders/ListOrders로 우회)
public interface GetOrderUseCase {

    Order getOrder(GetOrderQuery query);

    record GetOrderQuery(
            UUID orderId,
            UUID actorId   // 권한 검증용 (buyer.id == actorId 또는 seller.id == actorId)
    ) {}
}
