package com.trustamarket.orderservice.order.adapter.out.wallet;

import com.trustamarket.common.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.UUID;

// wallet-service `/internal/v1/wallets/usages` 호출용 Feign client.
// X-User-* / Authorization 등 인증 헤더는 common FeignConfig 의 RequestInterceptor 가 자동 전파.
@FeignClient(name = "wallet-service")
public interface WalletFeignClient {

    @PatchMapping("/internal/v1/wallets/usages")
    ResponseEntity<CommonResponse<UseWalletResponse>> usePoint(@RequestBody UseWalletRequest request);

    // saga 의 catch / reconciliation scheduler 가 호출 — orderId 로 차감 기록 조회.
    // wallet 측은 DB 만 읽음 (멱등). 차감 기록 있으면 result=DEDUCTED, 없으면 NOT_DEDUCTED.
    @GetMapping("/internal/v1/wallets/usages/{orderId}")
    ResponseEntity<CommonResponse<UsageStatusResponse>> getUsage(@PathVariable UUID orderId);

    // wallet-service 의 UseWalletRequest 와 동일 필드명/타입.
    record UseWalletRequest(
            UUID orderId,
            UUID buyerId,
            Long totalAmount
    ) {}

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
    ) {}
}
