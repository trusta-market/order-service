package com.trustamarket.orderservice.order.adapter.out.wallet;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.domain.exception.WalletCommunicationException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

// WalletPaymentPort 실 구현 — Feign 으로 wallet-service 의 /internal/v1/wallets/usages 호출.
// wallet 응답이 CommonResponse<UseWalletResponse> 로 래핑되어 있어 Adapter 에서 unwrap.
// 통신 실패(타임아웃/5xx 등)는 WalletCommunicationException 으로 변환 (도메인 의미로 추상화).
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletPaymentAdapter implements WalletPaymentPort {

    private final WalletFeignClient walletFeignClient;

    @Override
    public DeductPointResponse deduct(DeductPointRequest request) {
        try {
            ResponseEntity<CommonResponse<WalletFeignClient.UseWalletResponse>> response = walletFeignClient.usePoint(
                    new WalletFeignClient.UseWalletRequest(
                            request.orderId(),
                            request.buyerId(),
                            request.totalAmount()
                    )
            );
            CommonResponse<WalletFeignClient.UseWalletResponse> body =
                    response != null ? response.getBody() : null;
            WalletFeignClient.UseWalletResponse data =
                    body != null ? body.data() : null;
            if (data == null
                    || data.balance() == null
                    || data.balance() < 0L
                    || (data.shortage() != null && data.shortage() < 0L)) {
                log.error("[Wallet] 빈/비정상 응답 — orderId={}", request.orderId());
                throw new WalletCommunicationException();
            }
            return new DeductPointResponse(data.balance(), data.shortage());
        } catch (FeignException e) {
            log.error("[Wallet] Feign 호출 실패 — orderId={}, status={}",
                    request.orderId(), e.status(), e);
            throw new WalletCommunicationException(e);
        }
    }
}
