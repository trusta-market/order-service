package com.trustamarket.orderservice.order.adapter.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.orderservice.order.adapter.in.messaging.dto.WalletCancellationCompletedMessage;
import com.trustamarket.orderservice.order.application.port.in.MarkOrderCancelledUseCase;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository.InboxPurposeKey;
import com.trustamarket.orderservice.order.domain.exception.OrderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// wallet.cancellation.completed 토픽 consume → CANCELLATION_PROCESSING → CANCELLATION_COMPLETED.
// 동일 패턴: port 의존, atomic dedup, afterCommit ack, raw JSON 로그 X.
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletCancellationCompletedListener {

    private static final String CONSUMER_GROUP = "order-wallet-cancellation-completed-group";

    private final MarkOrderCancelledUseCase markOrderCancelledUseCase;
    private final InboxRepository inboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${trusta.messaging.topic.wallet-cancellation-completed:wallet.cancellation.completed}",
            groupId = CONSUMER_GROUP)
    @Transactional
    public void consume(String json, Acknowledgment ack) {
        WalletCancellationCompletedMessage message;
        try {
            message = objectMapper.readValue(json, WalletCancellationCompletedMessage.class);
        } catch (Exception e) {
            log.error("[WalletCancellationCompleted] payload 파싱 실패 — ack + skip (length={})",
                    json == null ? -1 : json.length(), e);
            ack.acknowledge();
            return;
        }

        if (!inboxRepository.tryRecordKafkaEvent(message.eventId(), CONSUMER_GROUP, InboxPurposeKey.WALLET_CANCELLATION_COMPLETED)) {
            log.info("[WalletCancellationCompleted] 중복 메시지 — ack + skip. eventId={}, orderId={}",
                    message.eventId(), message.orderId());
            ack.acknowledge();
            return;
        }

        try {
            log.info("[WalletCancellationCompleted] consume — eventId={}, orderId={}, cancelledAmount={}",
                    message.eventId(), message.orderId(), message.cancelledAmount());
            markOrderCancelledUseCase.markCancelled(message.orderId());
            ackAfterCommit(ack);
        } catch (OrderException e) {
            log.warn("[WalletCancellationCompleted] non-retryable, ack + skip — eventId={}, orderId={}",
                    message.eventId(), message.orderId(), e);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[WalletCancellationCompleted] 처리 실패 — eventId={}, orderId={}",
                    message.eventId(), message.orderId(), e);
            throw e;
        }
    }

    private static void ackAfterCommit(Acknowledgment ack) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ack.acknowledge();
            }
        });
    }
}
