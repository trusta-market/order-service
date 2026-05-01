package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 내 판매 주문 조회 — GET /api/orders/me/sales (seller 본인 입장)
public interface GetMySalesUseCase {

    Page<Order> getMySales(GetMySalesQuery query);

    record GetMySalesQuery(
            UUID sellerId,
            Pageable pageable
    ) {}
}
