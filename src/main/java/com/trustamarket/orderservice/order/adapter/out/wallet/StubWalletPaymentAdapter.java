package com.trustamarket.orderservice.order.adapter.out.wallet;

import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 시연·로컬용 stub — 항상 success(잔액 충분) 응답. 실제 잔액 검증/차감 없음.
// TODO: 후속 PR 에서 WalletFeignClient + 실제 어댑터 도입되면 본 클래스 통째로 삭제.
@Slf4j
@Component
public class StubWalletPaymentAdapter implements WalletPaymentPort {

    @Override
    public DeductPointResponse deduct(DeductPointRequest request) {
        log.warn("[Wallet Stub] 실제 포인트 차감 없이 success 응답. orderId={}, buyerId={}, amount={}",
                request.orderId(), request.buyerId(), request.totalAmount());
        return new DeductPointResponse(0L, null);
    }
}
