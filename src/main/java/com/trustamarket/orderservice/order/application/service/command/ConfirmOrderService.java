package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.common.event.Events;
import com.trustamarket.common.event.OutboxEvent;
import com.trustamarket.orderservice.order.adapter.out.messaging.outbox.OutboxEventListener;
import com.trustamarket.orderservice.order.application.event.messaging.ProductSoldOutMessage;
import com.trustamarket.orderservice.order.application.event.messaging.SettlePointSettlementMessage;
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

// DELIVERED → CONFIRMED. confirm 직후 두 이벤트 발행:
//   - ORDER.SETTLEMENT_REQUESTED → wallet 정산 (escrow → seller + 수수료)
//   - ORDER.PRODUCT_SOLD_OUT     → product status SOLD_OUT 전이
// 발행은 common 의 Outbox facade (Events.trigger) 사용 — DB ↔ Kafka 일관성 보장.
@Service
@RequiredArgsConstructor
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private static final String DOMAIN_TYPE = "ORDER";

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public void confirm(ConfirmOrderCommand cmd) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));
        OrderAccessGuard.verifyBuyer(order, cmd.buyerId());

        OrderStatus pre = order.getStatus();
        Instant confirmedAt = Instant.now();
        order.confirm(confirmedAt);
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);

        orderRepository.save(order);

        // Outbox 발행 — BEFORE_COMMIT listener 가 받아서 outbox INSERT, poller 가 Kafka publish.
        // 메시지 record 는 application/event/messaging/ — adapter 의존 차단을 위해 primitive factory 사용.
        Events.trigger(OutboxEvent.of(
                DOMAIN_TYPE, order.getId().value(),
                OutboxEventListener.EVENT_SETTLEMENT_REQUESTED,
                SettlePointSettlementMessage.of(
                        order.getId().value(),
                        order.getBuyer().id(),
                        order.getSeller().id(),
                        order.getProduct().id(),
                        order.getProduct().name(),
                        order.getProduct().price().value(),
                        order.getShippingFee().value(),
                        order.getTotalAmount().value(),
                        confirmedAt)));

        Events.trigger(OutboxEvent.of(
                DOMAIN_TYPE, order.getId().value(),
                OutboxEventListener.EVENT_PRODUCT_SOLD_OUT,
                ProductSoldOutMessage.of(
                        order.getId().value(),
                        order.getProduct().id(),
                        confirmedAt)));
    }
}
