package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.application.dto.result.OrderDetailView;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;

import java.util.UUID;

// 주문 단건 조회 — GET /api/v1/orders/{id}
// 권한: buyer 또는 seller 본인만 조회 가능 (ADMIN 은 SearchOrders/ListOrders 로 우회)
public interface GetOrderUseCase {

    OrderDetailView getOrder(GetOrderQuery query);

    record GetOrderQuery(
            UUID orderId,
            UUID actorId   // 권한 검증용 (buyer.id == actorId 또는 seller.id == actorId)
    ) {
        public GetOrderQuery {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (actorId == null) throw new InvalidIdException("actorId");
        }
    }
}
