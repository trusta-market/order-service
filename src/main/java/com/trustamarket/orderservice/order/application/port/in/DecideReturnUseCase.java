package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidEnumException;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;

import java.util.UUID;

// 반송 승인/거절 — POST /api/orders/{id}/returns/{returnId}/decisions
// RETURN_REQUESTED → RETURN_APPROVED 또는 RETURN_REJECTED
// ADMIN 전용 (권한 검증은 컨트롤러의 @PreAuthorize에서 1차)
public interface DecideReturnUseCase {

    void decide(DecideReturnCommand command);

    enum Decision { APPROVE, REJECT }

    record DecideReturnCommand(
            UUID orderId,
            UUID adminId,
            Decision decision,
            String rejectReason   // REJECT일 때만 필수
    ) {
        public DecideReturnCommand {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (adminId == null) throw new InvalidIdException("adminId");
            if (decision == null) throw new InvalidEnumException("decision");
            if (decision == Decision.REJECT && (rejectReason == null || rejectReason.isBlank())) {
                throw new InvalidReasonException();
            }
        }
    }
}
