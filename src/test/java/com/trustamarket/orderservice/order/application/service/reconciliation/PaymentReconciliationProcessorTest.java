package com.trustamarket.orderservice.order.application.service.reconciliation;

import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.PaymentReconciliationRepository;
import com.trustamarket.orderservice.order.application.port.out.ReconciliationAlertPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.UsageStatus;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.exception.WalletCommunicationException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import com.trustamarket.orderservice.order.domain.model.ReconciliationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationProcessorTest {

    @Mock WalletPaymentPort walletPaymentPort;
    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @Mock PaymentReconciliationRepository reconciliationRepository;
    @Mock ReconciliationAlertPort alertPort;
    @Mock TransactionTemplate txTemplate;
    @InjectMocks PaymentReconciliationProcessor processor;

    @BeforeEach
    void setUpTxTemplate() {
        lenient().when(txTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("DEDUCTED — markPaid + reconciliation DONE")
    void deducted() {
        Order order = paymentPendingOrder();
        PaymentReconciliation r = PaymentReconciliation.enqueue(order.getId().value(), Instant.now());
        when(walletPaymentPort.getUsage(order.getId().value())).thenReturn(
                new UsageStatus(order.getId().value(), UsageStatus.Result.DEDUCTED,
                        100_000L, 50_000L, null, Instant.now()));
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        processor.processOne(r);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.DONE);
        verify(reconciliationRepository).save(r);
    }

    @Test
    @DisplayName("INSUFFICIENT — rollbackToRequested + reconciliation DONE")
    void insufficient() {
        Order order = paymentPendingOrder();
        PaymentReconciliation r = PaymentReconciliation.enqueue(order.getId().value(), Instant.now());
        when(walletPaymentPort.getUsage(order.getId().value())).thenReturn(
                new UsageStatus(order.getId().value(), UsageStatus.Result.INSUFFICIENT,
                        null, 5_000L, 95_000L, null));
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        processor.processOne(r);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.DONE);
    }

    @Test
    @DisplayName("NOT_FOUND — rollbackToRequested + reconciliation DONE")
    void notFound() {
        Order order = paymentPendingOrder();
        PaymentReconciliation r = PaymentReconciliation.enqueue(order.getId().value(), Instant.now());
        when(walletPaymentPort.getUsage(order.getId().value())).thenReturn(
                new UsageStatus(order.getId().value(), UsageStatus.Result.NOT_FOUND,
                        null, null, null, null));
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        processor.processOne(r);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.DONE);
    }

    @Test
    @DisplayName("getUsage 실패 — recordUnknown (retry_count++), 알림 X (아직 PENDING)")
    void unknown_keepPending() {
        UUID orderId = UUID.randomUUID();
        PaymentReconciliation r = PaymentReconciliation.enqueue(orderId, Instant.now());
        when(walletPaymentPort.getUsage(orderId)).thenThrow(new WalletCommunicationException());

        processor.processOne(r);

        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.PENDING);
        assertThat(r.getRetryCount()).isEqualTo(1);
        verify(reconciliationRepository).save(r);
        verify(alertPort, never()).notifyGivenUp(any(), anyInt(), any());
    }

    @Test
    @DisplayName("getUsage 실패 — retry_count 가 MAX 도달 시 GIVEN_UP + 알림")
    void unknown_givenUp() {
        UUID orderId = UUID.randomUUID();
        PaymentReconciliation r = PaymentReconciliation.enqueue(orderId, Instant.now());
        r.recordUnknown("e1", Instant.now());     // count=1
        r.recordUnknown("e2", Instant.now());     // count=2
        when(walletPaymentPort.getUsage(orderId)).thenThrow(new WalletCommunicationException());

        processor.processOne(r);                   // count → 3 → GIVEN_UP

        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.GIVEN_UP);
        verify(alertPort).notifyGivenUp(eq(orderId), eq(PaymentReconciliation.MAX_RETRIES), any());
    }

    @Test
    @DisplayName("DEDUCTED 인데 order 가 이미 PAID — 멱등 처리 (markPaid 호출 X)")
    void deducted_alreadyPaid() {
        Order order = OrderTestFixtures.paidOrder(UUID.randomUUID(), UUID.randomUUID());
        PaymentReconciliation r = PaymentReconciliation.enqueue(order.getId().value(), Instant.now());
        when(walletPaymentPort.getUsage(order.getId().value())).thenReturn(
                new UsageStatus(order.getId().value(), UsageStatus.Result.DEDUCTED,
                        100_000L, 50_000L, null, Instant.now()));
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        processor.processOne(r);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.DONE);
        // 이미 PAID 라 save / outbox 호출 X
        verify(orderRepository, never()).save(any(Order.class));
    }

    // helper — PAYMENT_PENDING 상태 order 생성.
    private Order paymentPendingOrder() {
        Order order = OrderTestFixtures.requestedOrder();
        order.requestPayment();    // REQUESTED → PAYMENT_PENDING
        return order;
    }

    // Mockito ArgumentMatchers 헬퍼 static import 누락 회피 — 메서드 정의로 대체.
    private static <T> T eq(T value) { return org.mockito.ArgumentMatchers.eq(value); }
    private static int anyInt() { return org.mockito.ArgumentMatchers.anyInt(); }
}
