package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 내 구매 주문 조회 — GET /api/orders/me/purchases (buyer 본인 입장)
public interface GetMyPurchasesUseCase {

    Page<Order> getMyPurchases(GetMyPurchasesQuery query);

    record GetMyPurchasesQuery(
            UUID buyerId,
            Pageable pageable
    ) {
        public GetMyPurchasesQuery {
            if (buyerId == null) throw new InvalidIdException("buyerId");
            if (pageable == null) throw new InvalidIdException("pageable");
        }
    }
}
