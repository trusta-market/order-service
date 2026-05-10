package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.common.event.Events;
import com.trustamarket.common.event.OutboxEvent;
import com.trustamarket.orderservice.order.application.event.messaging.OrderEventTypes;
import com.trustamarket.orderservice.order.application.event.messaging.OrderCancellationRequestedMessage;
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

import java.time.Instant;

// 결제 전: REQUESTED / PAYMENT_PENDING → CANCELLED (wallet 무관)
// 결제 후 (PAID): CANCELLATION_PROCESSING + Outbox publish → wallet escrow 복구 후
//                wallet.cancellation.completed 수신 → MarkOrderCancelledService 가 CANCELLATION_COMPLETED 마킹
@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private static final String DOMAIN_TYPE = "ORDER";

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

        // 결제 후 취소만 wallet 통신 필요 — 결제 전 취소는 wallet 차감 없으니 publish X.
        if (pre == OrderStatus.PAID) {
            Events.trigger(OutboxEvent.of(
                    DOMAIN_TYPE, order.getId().value(),
                    OrderEventTypes.ORDER_CANCELLATION_REQUESTED,
                    OrderCancellationRequestedMessage.of(
                            order.getId().value(),
                            order.getBuyer().id(),
                            order.getTotalAmount().value(),
                            Instant.now())));
        }
    }
}
