package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.CancelOrderUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderAccessGuard;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.Reason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// REQUESTED/PAYMENT_PENDING → CANCELLED, PAID → REFUND_PROCESSING
// markRefunded(환불 완료) 호출은 MVP scope 외 (Wallet 환불 호출 + 이벤트는 후속 PR)
@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public void cancel(CancelOrderCommand cmd) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));
        OrderAccessGuard.verifyBuyer(order, cmd.actorId());

        OrderStatus pre = order.getStatus();
        Reason reason = Reason.of(cmd.reason());
        order.cancel(reason);
        historyRecorder.record(order.getId(), pre, order.getStatus(), reason);

        orderRepository.save(order);

        // TODO: 다음 PR — PAID 이전이었으면 OrderCancelledEvent 발행 (Wallet 환불 트리거)
        //       지금 흐름: REFUND_PROCESSING 상태로 두고 markRefunded는 후속 PR에서 호출
    }
}
