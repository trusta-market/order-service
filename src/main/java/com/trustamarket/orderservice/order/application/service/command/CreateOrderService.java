package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.dto.result.CreateOrderResult;
import com.trustamarket.orderservice.order.application.exception.ProductNotPurchasableException;
import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort.ProductInfo;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
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
    private final ProductInfoPort productInfoPort;

    @Override
    @Transactional
    public CreateOrderResult createOrder(CreateOrderCommand cmd) {
        // product-service 검증 — ON_SALE 인 상품만 주문 생성 가능. 가격/이름은 server snapshot 사용 (client 입력 무시 — 변조 방지).
        ProductInfo product = productInfoPort.fetch(cmd.productId());
        if (!product.isOnSale()) {
            throw new ProductNotPurchasableException(cmd.productId(), product.status());
        }

        Order order = Order.create(
                Buyer.of(cmd.buyerId(), cmd.buyerName()),
                Seller.of(product.sellerId(), cmd.sellerName()),    // sellerId 도 product-service 진실
                Product.of(product.id(), product.name(), Money.of(product.price())),
                cmd.type(),
                Money.of(cmd.shippingFee())
        );
        Order saved = orderRepository.save(order);
        // 신규 생성 — 이전 상태 없음 (prev=null), 진입 상태 REQUESTED
        historyRecorder.record(saved.getId(), null, OrderStatus.REQUESTED, null);
        return CreateOrderResult.from(saved);
    }
}
