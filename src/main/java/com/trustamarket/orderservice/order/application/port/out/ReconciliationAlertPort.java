package com.trustamarket.orderservice.order.application.port.out;

import java.util.UUID;

// GIVEN_UP 상태 도달 시 운영자 알림 (Discord webhook 등) 발송 port.
// Adapter 가 notification-service / Discord 등 실제 채널로 전달.
// best-effort — 실패해도 reconciliation 본 흐름엔 영향 X.
public interface ReconciliationAlertPort {

    void notifyGivenUp(UUID orderId, int retryCount, String lastError);
}
