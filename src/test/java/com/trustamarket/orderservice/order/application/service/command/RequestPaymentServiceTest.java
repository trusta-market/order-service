package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase.RequestPaymentCommand;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository.InboxPurposeKey;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointResponse;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.exception.InsufficientPointBalanceException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPaymentServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @Mock WalletPaymentPort walletPaymentPort;
    @Mock InboxRepository inboxRepository;
    @Mock TransactionTemplate txTemplate;
    @InjectMocks RequestPaymentService service;

    @BeforeEach
    void setUpTxTemplate() {
        // txTemplate.execute(callback) 가 callback 을 즉시 실행하도록 stub
        // (실 트랜잭션 동작은 통합 테스트 에서 검증)
        lenient().when(txTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("결제 시작 — Wallet 성공 → PAID 전이 + history 2건 + inbox 첫 INSERT")
    void happyPath() {
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(inboxRepository.tryRecordIdempotencyKey(idempotencyKey, InboxPurposeKey.REQUEST_PAYMENT)).thenReturn(true);
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);
        when(walletPaymentPort.deduct(any())).thenReturn(new DeductPointResponse(50_000L, null));

        service.requestPayment(new RequestPaymentCommand(order.getId().value(), buyerId, idempotencyKey));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(historyRecorder).record(order.getId(), OrderStatus.REQUESTED, OrderStatus.PAYMENT_PENDING, null);
        verify(historyRecorder).record(order.getId(), OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, null);
        // saga 분리: segment 1 (PAYMENT_PENDING) + segment 2 (PAID) 두 번 save
        verify(orderRepository, times(2)).save(order);
    }

    @Test
    @DisplayName("동일 Idempotency-Key 재호출 → tryRecord 가 false → no-op")
    void duplicateIdempotencyKey() {
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        when(inboxRepository.tryRecordIdempotencyKey(idempotencyKey, InboxPurposeKey.REQUEST_PAYMENT)).thenReturn(false);

        service.requestPayment(new RequestPaymentCommand(UUID.randomUUID(), buyerId, idempotencyKey));

        verify(orderRepository, never()).findByIdOrThrow(any());
        verify(walletPaymentPort, never()).deduct(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔액 부족 → InsufficientPointBalanceException, REQUESTED 로 보상 (saga)")
    void insufficientBalance() {
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(inboxRepository.tryRecordIdempotencyKey(idempotencyKey, InboxPurposeKey.REQUEST_PAYMENT)).thenReturn(true);
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);
        when(walletPaymentPort.deduct(any())).thenReturn(new DeductPointResponse(5_000L, 98_000L));

        assertThatThrownBy(() ->
                service.requestPayment(new RequestPaymentCommand(order.getId().value(), buyerId, idempotencyKey))
        ).isInstanceOf(InsufficientPointBalanceException.class);

        // saga 분리: segment 1 (PAYMENT_PENDING save) + 보상 (rollbackPaymentRequest → REQUESTED save) = 2번
        verify(orderRepository, times(2)).save(any(Order.class));
        // 최종 상태는 REQUESTED 로 복귀 (재시도 가능)
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
    }

    @Test
    @DisplayName("buyer가 아닌 사용자 호출 → UnauthorizedOrderAccessException")
    void unauthorized() {
        UUID buyerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(inboxRepository.tryRecordIdempotencyKey(idempotencyKey, InboxPurposeKey.REQUEST_PAYMENT)).thenReturn(true);
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        assertThatThrownBy(() ->
                service.requestPayment(new RequestPaymentCommand(order.getId().value(), strangerId, idempotencyKey))
        ).isInstanceOf(UnauthorizedOrderAccessException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
        verify(walletPaymentPort, never()).deduct(any());
        verify(historyRecorder, never()).record(any(), any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotency-Key blank → InvalidIdException (compact constructor)")
    void blankIdempotencyKey() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        assertThatThrownBy(() ->
                new RequestPaymentCommand(orderId, buyerId, "  ")
        ).isInstanceOf(com.trustamarket.orderservice.order.domain.exception.InvalidIdException.class);
        assertThatThrownBy(() ->
                new RequestPaymentCommand(orderId, buyerId, null)
        ).isInstanceOf(com.trustamarket.orderservice.order.domain.exception.InvalidIdException.class);
    }
}
