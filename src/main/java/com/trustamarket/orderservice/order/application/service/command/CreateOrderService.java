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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;
    private final ProductInfoPort productInfoPort;
    private final TransactionTemplate txTemplate;

    public CreateOrderService(
            OrderRepository orderRepository,
            OrderHistoryRecorder historyRecorder,
            ProductInfoPort productInfoPort,
            PlatformTransactionManager txManager) {
        this.orderRepository = orderRepository;
        this.historyRecorder = historyRecorder;
        this.productInfoPort = productInfoPort;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public CreateOrderResult createOrder(CreateOrderCommand cmd) {
        // ── tx 밖: Feign 호출 + 검증 (DB connection 점유 X) ──
        // product-service 검증 — ON_SALE 인 상품만 주문 생성 가능. 가격/이름은 server snapshot (변조 방지).
        ProductInfo product = productInfoPort.fetch(cmd.productId());
        if (!product.isOnSale()) {
            throw new ProductNotPurchasableException(cmd.productId(), product.status());
        }

        // ── tx 안: 순수 DB 작업만 (connection hold 시간 ~50ms) ──
        return txTemplate.execute(status -> {
            Order order = Order.create(
                    Buyer.of(cmd.buyerId(), cmd.buyerName()),
                    Seller.of(product.sellerId(), cmd.sellerName()),
                    Product.of(product.id(), product.name(), Money.of(product.price())),
                    cmd.type(),
                    Money.of(cmd.shippingFee())
            );
            Order saved = orderRepository.save(order);
            historyRecorder.record(saved.getId(), null, OrderStatus.REQUESTED, null);
            return CreateOrderResult.from(saved);
        });
    }
}
