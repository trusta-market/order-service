package com.trustamarket.orderservice.order.adapter.in.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// wallet-service 의 `wallet.cancellation.completed` 토픽 페이로드.
// wallet 이 escrow → buyer 복구 완료 후 발행. order 가 consume → markCancelled.
public record WalletCancellationCompletedMessage(
        UUID    eventId,
        UUID    orderId,
        long    cancelledAmount,
        Instant completedAt
) {}
