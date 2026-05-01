package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.Product;
import com.trustamarket.orderservice.order.domain.model.Seller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public OrderId createOrder(CreateOrderCommand cmd) {
        Order order = Order.create(
                Buyer.of(cmd.buyerId(), cmd.buyerName()),
                Seller.of(cmd.sellerId(), cmd.sellerName()),
                Product.of(cmd.productId(), cmd.productName(), Money.of(cmd.productPrice())),
                cmd.type(),
                Money.of(cmd.shippingFee())
        );
        Order saved = orderRepository.save(order);
        // 신규 생성 — 이전 상태 없음 (prev=null), 진입 상태 REQUESTED
        historyRecorder.record(saved.getId(), null, OrderStatus.REQUESTED, null);
        return saved.getId();
    }
}
