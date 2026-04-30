package com.trustamarket.orderservice.order.application.port.out;

import java.util.UUID;

// Wallet 동기 결제 호출 port (Wallet 팀과 합의된 시그니처 — 2026-05-01)
// 구현은 후속 PR (Feign 클라이언트)
public interface WalletPaymentPort {

    DeductPointResponse deduct(DeductPointRequest request);
    // TODO: 환불 흐름은 MVP scope 외 — 추후 별도 PR에서 추가 (sync vs 이벤트 결정 포함)

    record DeductPointRequest(
            UUID orderId,
            UUID buyerId,
            Long totalAmount
    ) {}

    // shortage > 0 이면 잔액 부족, null/0 이면 성공
    record DeductPointResponse(
            Long balance,
            Long shortage
    ) {
        public boolean isSuccess() {
            return shortage == null || shortage <= 0;
        }
    }
}
