package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;

import java.util.UUID;

// 구매 확정 — POST /api/orders/{id}/confirmations
// DELIVERED → CONFIRMED (거래 종결)
// 정산 흐름은 본 PR scope 외 (이전 PR에서 SETTLEMENT_PROCESSING/COMPLETED 제거된 상태)
public interface ConfirmOrderUseCase {

    void confirm(ConfirmOrderCommand command);

    record ConfirmOrderCommand(
            UUID orderId,
            UUID buyerId   // 권한 검증용
    ) {
        public ConfirmOrderCommand {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (buyerId == null) throw new InvalidIdException("buyerId");
        }
    }
}
