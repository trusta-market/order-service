package com.trustamarket.orderservice.order.adapter.in.web.dto.request;

import com.trustamarket.orderservice.order.application.port.in.RequestReturnUseCase.RequestReturnCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// POST /api/v1/orders/{id}/return 요청 본문
public record RequestReturnRequest(
        @NotBlank @Size(max = 200) String reason
) {
    public RequestReturnCommand toCommand(UUID orderId, UUID buyerId) {
        return new RequestReturnCommand(orderId, buyerId, reason);
    }
}
