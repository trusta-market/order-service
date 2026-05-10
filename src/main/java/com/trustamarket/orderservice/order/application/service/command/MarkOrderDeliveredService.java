package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.MarkOrderDeliveredUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarkOrderDeliveredService implements MarkOrderDeliveredUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public void markDelivered(UUID orderId) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(orderId));
        OrderStatus pre = order.getStatus();
        order.markDelivered();
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);
        orderRepository.save(order);
    }
}
