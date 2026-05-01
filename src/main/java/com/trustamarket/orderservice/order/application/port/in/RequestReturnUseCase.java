package com.trustamarket.orderservice.order.application.port.in;

import java.util.UUID;

// 반송 요청 — POST /api/orders/{id}/returns
// SHIPPING/DELIVERED → RETURN_REQUESTED
public interface RequestReturnUseCase {

    void requestReturn(RequestReturnCommand command);

    record RequestReturnCommand(
            UUID orderId,
            UUID buyerId,    // 권한 검증용
            String reason
    ) {}
}
