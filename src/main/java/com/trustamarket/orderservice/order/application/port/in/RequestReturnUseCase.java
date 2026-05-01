package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;

import java.util.UUID;

// 반송 요청 — POST /api/orders/{id}/returns
// SHIPPING/DELIVERED → RETURN_REQUESTED
public interface RequestReturnUseCase {

    void requestReturn(RequestReturnCommand command);

    record RequestReturnCommand(
            UUID orderId,
            UUID buyerId,    // 권한 검증용
            String reason
    ) {
        public RequestReturnCommand {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (buyerId == null) throw new InvalidIdException("buyerId");
            if (reason == null || reason.isBlank()) throw new InvalidReasonException();
        }
    }
}
