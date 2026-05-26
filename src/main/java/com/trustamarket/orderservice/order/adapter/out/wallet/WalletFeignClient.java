package com.trustamarket.orderservice.order.adapter.out.wallet;

import com.trustamarket.common.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

// wallet-service `/internal/v1/wallets/usages` 호출용 Feign client.
// X-User-* / Authorization 등 인증 헤더는 common FeignConfig 의 RequestInterceptor 가 자동 전파.
@FeignClient(name = "wallet-service")
public interface WalletFeignClient {

    @PatchMapping("/internal/v1/wallets/usages")
    ResponseEntity<CommonResponse<UseWalletResponse>> usePoint(@RequestBody UseWalletRequest request);

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
}
