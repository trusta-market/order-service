package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.inbox.InboxJpaRepository;
import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase.RequestPaymentCommand;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointResponse;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.exception.InsufficientPointBalanceException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPaymentServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @Mock WalletPaymentPort walletPaymentPort;
    @Mock InboxJpaRepository inboxRepository;
    @InjectMocks RequestPaymentService service;

    @Test
    @DisplayName("결제 시작 — Wallet 성공 → PAID 전이 + history 2건 + inbox 기록")
    void happyPath() {
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(inboxRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);
        when(walletPaymentPort.deduct(any())).thenReturn(new DeductPointResponse(50_000L, null));

        service.requestPayment(new RequestPaymentCommand(order.getId().value(), buyerId, idempotencyKey));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(historyRecorder).record(order.getId(), OrderStatus.REQUESTED, OrderStatus.PAYMENT_PENDING, null);
        verify(historyRecorder).record(order.getId(), OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, null);
        verify(orderRepository).save(order);
        verify(inboxRepository).save(any(InboxJpaEntity.class));
    }

    @Test
    @DisplayName("동일 Idempotency-Key 재호출 → no-op (도메인 로직 안 탐)")
    void duplicateIdempotencyKey() {
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        when(inboxRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(InboxJpaEntity.forIdempotencyKey(idempotencyKey, null, null)));

        service.requestPayment(new RequestPaymentCommand(UUID.randomUUID(), buyerId, idempotencyKey));

        verify(orderRepository, never()).findByIdOrThrow(any());
        verify(walletPaymentPort, never()).deduct(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔액 부족 → InsufficientPointBalanceException, save 호출 X (트랜잭션 롤백 의존)")
    void insufficientBalance() {
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(inboxRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);
        when(walletPaymentPort.deduct(any())).thenReturn(new DeductPointResponse(5_000L, 98_000L));

        assertThatThrownBy(() ->
                service.requestPayment(new RequestPaymentCommand(order.getId().value(), buyerId, idempotencyKey))
        ).isInstanceOf(InsufficientPointBalanceException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("buyer가 아닌 사용자 호출 → UnauthorizedOrderAccessException")
    void unauthorized() {
        UUID buyerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(inboxRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        assertThatThrownBy(() ->
                service.requestPayment(new RequestPaymentCommand(order.getId().value(), strangerId, idempotencyKey))
        ).isInstanceOf(UnauthorizedOrderAccessException.class);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
        verify(walletPaymentPort, never()).deduct(any());
        verify(historyRecorder, never()).record(any(), any(), any(), any());
        verify(orderRepository, never()).save(any());
    }
}
