package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.ConfirmOrderUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.ProductSoldOutMessagePort;
import com.trustamarket.orderservice.order.application.port.out.SettlementMessagePort;
import com.trustamarket.orderservice.order.application.service.support.OrderAccessGuard;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// DELIVERED → CONFIRMED (정공) 또는 PAID → CONFIRMED (시연용 배송 우회 — OrderTransition 참조).
// CONFIRMED 전이 직후 정산 요청 Kafka 발행 (정공 시점 — 이전 PR 의 RequestPayment 시점 발행은 임시였음).
@Service
@RequiredArgsConstructor
public class ConfirmOrderService implements ConfirmOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;
    private final SettlementMessagePort settlementPublisher;
    private final ProductSoldOutMessagePort productSoldOutPublisher;

    @Override
    @Transactional
    public void confirm(ConfirmOrderCommand cmd) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));
        OrderAccessGuard.verifyBuyer(order, cmd.buyerId());

        OrderStatus pre = order.getStatus();
        order.confirm(Instant.now());
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);

        orderRepository.save(order);

        // 정산 트리거 — 구매 확정 직후. wallet-service 의 PointSettlementListener 가 escrow→seller+fee 분배.
        settlementPublisher.publishForPaidOrder(order);

        // product 도메인에 판매완료 신호 — product-service 의 ProductSoldOutListener 가 status SOLD_OUT 으로 전이.
        productSoldOutPublisher.publishForConfirmedOrder(order);
    }
}
