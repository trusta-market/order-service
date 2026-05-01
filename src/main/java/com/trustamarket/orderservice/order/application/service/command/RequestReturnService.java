package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.RequestReturnUseCase;
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

// SHIPPING/DELIVERED → RETURN_REQUESTED
@Service
@RequiredArgsConstructor
public class RequestReturnService implements RequestReturnUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public void requestReturn(RequestReturnCommand cmd) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));
        OrderAccessGuard.verifyBuyer(order, cmd.buyerId());

        OrderStatus pre = order.getStatus();
        Reason reason = Reason.of(cmd.reason());
        order.requestReturn(reason);
        historyRecorder.record(order.getId(), pre, order.getStatus(), reason);

        orderRepository.save(order);
        // TODO: 다음 PR — OrderReturnRequestedEvent 발행
    }
}
