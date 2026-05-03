package com.trustamarket.orderservice.order.adapter.in.web.dto.request;

import com.trustamarket.orderservice.order.application.port.in.CancelOrderUseCase.CancelOrderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// POST /api/v1/orders/{id}/cancel 요청 본문
// orderId 는 path, actorId 는 인증 사용자에서 채움
public record CancelOrderRequest(
        @NotBlank @Size(max = 200) String reason
) {
    public CancelOrderCommand toCommand(UUID orderId, UUID actorId) {
        return new CancelOrderCommand(orderId, actorId, reason);
    }
}
