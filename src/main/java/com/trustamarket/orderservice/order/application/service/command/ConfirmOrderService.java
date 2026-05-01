package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.ConfirmOrderUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderAccessGuard;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// DELIVERED → CONFIRMED (거래 종결)
// 정산 흐름은 본 PR scope 외 (PR 4에서 SETTLEMENT_PROCESSING/COMPLETED 제거됨)
@Service
@RequiredArgsConstructor
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public void confirm(ConfirmOrderCommand cmd) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));
        OrderAccessGuard.verifyBuyer(order, cmd.buyerId());

        OrderStatus pre = order.getStatus();
        order.confirm(Instant.now());
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);

        orderRepository.save(order);
        // TODO: 다음 PR — PurchaseConfirmedEvent 발행 (Settlement 트리거)
    }
}
