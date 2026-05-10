package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;

import java.util.UUID;

// 주문 → 포인트 사용 (Wallet 동기 호출) port — Wallet 팀과 합의된 시그니처
// HTTP 응답은 CommonResponse<DeductPointResponse>로 래핑 — Feign 어댑터(후속 PR)에서 unwrap 후 반환
// application 레이어는 CommonResponse 모름 (HTTP 디테일 격리)
public interface WalletPaymentPort {

    // 주문 결제 시점에 포인트 차감 요청
    DeductPointResponse deduct(DeductPointRequest request);
    // 취소 흐름은 sync 호출이 아닌 Kafka (order.cancellation.requested) 로 진행 — 본 port 와 무관.

    // 주문 -> 포인트 사용 요청 DTO
    // 금액은 룰 [3]에 따라 long(원시형) 사용. compact constructor로 입력 검증
    record DeductPointRequest(
            UUID orderId,
            UUID buyerId,
            long totalAmount     // 차감 요청 금액 = product price + shipping fee, 양수
    ) {
        public DeductPointRequest {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (buyerId == null) throw new InvalidIdException("buyerId");
            if (totalAmount <= 0) throw new InvalidMoneyException(totalAmount);
        }
    }

    // 주문 <- 포인트 사용 응답 DTO
    // shortage 양수 → 잔액 부족, null/0 → 성공. 음수는 비정상 응답 (계약 위반)
    record DeductPointResponse(
            Long balance, // 차감 후 또는 현재(부족 시) 잔액
            Long shortage // 부족 금액 (null/0 = 성공)
    ) {
        public boolean isSuccess() {
            return shortage == null || shortage == 0;
        }
    }
}
