package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;

import java.time.Instant;
import java.util.UUID;

// 주문 → 포인트 사용 (Wallet 동기 호출) port — Wallet 팀과 합의된 시그니처
// HTTP 응답은 CommonResponse<...>로 래핑 — Feign 어댑터(후속 PR)에서 unwrap 후 반환
// application 레이어는 CommonResponse 모름 (HTTP 디테일 격리)
public interface WalletPaymentPort {

    // 주문 결제 시점에 포인트 차감 요청
    DeductPointResponse deduct(DeductPointRequest request);
    // 취소 흐름은 sync 호출이 아닌 Kafka (order.cancellation.requested) 로 진행 — 본 port 와 무관.

    // saga 의 wallet 차감 호출이 timeout / 5xx 등 비정상 응답을 받았을 때 결과 확인용.
    // wallet 측은 DB 만 조회 (멱등).
    //   차감 기록 있음 → DEDUCTED
    //   거절 기록 있음 → INSUFFICIENT (잔액 부족 거절)
    //   기록 없음     → NOT_FOUND
    // wallet 자체가 응답 못 하는 케이스 (또 timeout / 5xx) 는 Adapter 가 RuntimeException → 호출자가 UNKNOWN 처리.
    UsageStatus getUsage(UUID orderId);

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

    // getUsage 응답 — wallet 측 DB 조회 결과 그대로 반영.
    //   DEDUCTED     : 차감 기록 있음. amount / balance / deductedAt 채워짐
    //   INSUFFICIENT : 거절 기록 있음 (wallet 이 받아 잔액 부족으로 거절). balance / shortage 채워짐
    //   NOT_FOUND    : 어떤 기록도 없음 (요청이 wallet 에 안 닿음). 다 null
    record UsageStatus(
            UUID orderId,
            Result result,
            Long amount,
            Long balance,
            Long shortage,
            Instant deductedAt
    ) {
        // 필수 필드 (orderId / result) 만 검증 — 그 외는 result 에 따라 nullable.
        // 호출부 (saga catch / Processor) 가 switch(usage.result()) 로 분기하므로 null 차단 필수.
        public UsageStatus {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (result == null) throw new InvalidIdException("result");
        }

        public boolean isDeducted() {
            return result == Result.DEDUCTED;
        }

        public enum Result {
            DEDUCTED,
            INSUFFICIENT,
            NOT_FOUND
        }
    }
}
