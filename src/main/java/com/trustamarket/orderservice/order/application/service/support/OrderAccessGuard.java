package com.trustamarket.orderservice.order.application.service.support;

import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.domain.model.Order;

import java.util.UUID;

// 권한 검증 유틸 — buyer 본인 외 행위 차단
// ADMIN 검증은 컨트롤러 @PreAuthorize 1차 + 별도 service에서 처리 (필요 시)
public final class OrderAccessGuard {

    private OrderAccessGuard() {}

    public static void verifyBuyer(Order order, UUID actorId) {
        if (!order.getBuyer().id().equals(actorId)) {
            throw new UnauthorizedOrderAccessException(order.getId().value(), actorId);
        }
    }

    // GetOrderUseCase 용 — buyer 또는 seller 둘 중 하나라도 본인이면 됨 (ADMIN은 별도 endpoint)
    public static void verifyBuyerOrSeller(Order order, UUID actorId) {
        boolean isBuyer = order.getBuyer().id().equals(actorId);
        boolean isSeller = order.getSeller().id().equals(actorId);
        if (!isBuyer && !isSeller) {
            throw new UnauthorizedOrderAccessException(order.getId().value(), actorId);
        }
    }
}
