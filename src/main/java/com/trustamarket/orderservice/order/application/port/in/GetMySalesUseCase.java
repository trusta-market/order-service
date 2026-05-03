package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// 내 판매 주문 조회 — GET /api/v1/orders/me/sales (seller 본인 입장)
public interface GetMySalesUseCase {

    Page<OrderSummaryView> getMySales(GetMySalesQuery query);

    record GetMySalesQuery(
            UUID sellerId,
            Pageable pageable
    ) {
        public GetMySalesQuery {
            if (sellerId == null) throw new InvalidIdException("sellerId");
            if (pageable == null) throw new InvalidIdException("pageable");
        }
    }
}
