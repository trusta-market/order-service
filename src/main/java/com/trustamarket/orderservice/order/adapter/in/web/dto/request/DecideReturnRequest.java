package com.trustamarket.orderservice.order.adapter.in.web.dto.request;

import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase.DecideReturnCommand;
import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase.Decision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// POST /api/v1/admin/orders/{id}/return/decision 요청 본문 (ADMIN)
// rejectReason 은 Decision == REJECT 일 때만 필수 — record compact constructor 가 검증
public record DecideReturnRequest(
        @NotNull Decision decision,
        @Size(max = 200) String rejectReason
) {
    public DecideReturnCommand toCommand(UUID orderId, UUID adminId) {
        return new DecideReturnCommand(orderId, adminId, decision, rejectReason);
    }
}
