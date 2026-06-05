package com.trustamarket.orderservice.order.adapter.out.wallet;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.UUID;

// wallet-service `/internal/v1/wallets/usages` 호출용 Feign client.
// X-User-* / Authorization 등 인증 헤더는 common FeignConfig 의 RequestInterceptor 가 자동 전파.
@FeignClient(name = "wallet-service")
public interface WalletFeignClient {

    @PatchMapping("/internal/v1/wallets/usages")
    ResponseEntity<CommonResponse<UseWalletResponse>> usePoint(@RequestBody UseWalletRequest request);

    // saga 의 catch / reconciliation scheduler 가 호출 — orderId 로 거래 기록 조회.
    // wallet 측은 DB 만 읽음 (멱등).
    //   차감 기록 있음 → DEDUCTED
    //   거절 기록 있음 → INSUFFICIENT (잔액 부족으로 wallet 이 받아 거절)
    //   기록 없음     → NOT_FOUND (요청 자체가 wallet 에 안 닿음)
    // orderId 는 query parameter — 단건 조회지만 wallet 팀과의 컨벤션상 path 대신 query 사용.
    @GetMapping("/internal/v1/wallets/usages")
    ResponseEntity<CommonResponse<UsageStatusResponse>> getUsage(@RequestParam("orderId") UUID orderId);

    // wallet-service 의 UseWalletRequest 와 동일 필드명/타입.
    // idempotencyKey: 사용자 멱등 키. wallet 의 uk_idempotency_key 가 같은 키 재요청 시 멱등 응답 보장.
    record UseWalletRequest(
            UUID idempotencyKey,
            UUID orderId,
            UUID buyerId,
            Long totalAmount
    ) {
        public UseWalletRequest {
            if (idempotencyKey == null) throw new InvalidIdException("idempotencyKey");
            if (orderId == null) throw new InvalidIdException("orderId");
            if (buyerId == null) throw new InvalidIdException("buyerId");
            if (totalAmount == null) throw new InvalidMoneyException("totalAmount");
            if (totalAmount <= 0) throw new InvalidMoneyException(totalAmount);
        }
    }

    // wallet-service 의 UseWalletResponse 와 동일 필드명/타입.
    record UseWalletResponse(
            Long balance,
            Long shortage
    ) {}

    // wallet 측 GET /usages/{orderId} 응답.
    // result = "DEDUCTED" | "INSUFFICIENT" | "NOT_FOUND"
    //   DEDUCTED     : 차감 완료. amount/balance/deductedAt 채움
    //   INSUFFICIENT : 잔액 부족으로 wallet 이 거절. balance/shortage 채움
    //   NOT_FOUND    : 요청 자체가 wallet 에 안 닿음. 모두 null
    record UsageStatusResponse(
            UUID orderId,
            String result,
            Long amount,
            Long balance,
            Long shortage,
            Instant deductedAt
    ) {
        public UsageStatusResponse {
            // 기본 무결성 — 결과별 nullable 규칙은 Adapter 가 enum 변환 후 검증.
            if (orderId == null) throw new InvalidIdException("orderId");
            if (result == null || result.isBlank()) throw new InvalidIdException("result");
        }
    }
}
