package com.trustamarket.orderservice.order.application.port.out;

import java.util.UUID;

// 주문 → 포인트 사용 (Wallet 동기 호출) port — Wallet 팀과 합의된 시그니처
// HTTP 응답은 CommonResponse<DeductPointResponse>로 래핑 — Feign 어댑터(후속 PR)에서 unwrap 후 반환
// application 레이어는 CommonResponse 모름 (HTTP 디테일 격리)
public interface WalletPaymentPort {

    // 주문 결제 시점에 포인트 차감 요청
    DeductPointResponse deduct(DeductPointRequest request);
    // TODO: 환불 흐름은 MVP scope 외 — 추후 별도 PR에서 추가 (sync vs 이벤트 결정 포함)

    // 주문 -> 포인트 사용 요청 DTO
    record DeductPointRequest(
            UUID orderId,
            UUID buyerId,
            Long totalAmount     // 차감 요청 금액 = product price + shipping fee
    ) {}

    // 주문 <- 포인트 사용 응답 DTO
    // shortage > 0 이면 잔액 부족, null/0 이면 성공
    record DeductPointResponse(
            Long balance, // 차감 후 또는 현재(부족 시) 잔액
            Long shortage // 부족 금액 (null/0 = 성공)
    ) {
        public boolean isSuccess() {
            return shortage == null || shortage <= 0;
        }
    }
}
