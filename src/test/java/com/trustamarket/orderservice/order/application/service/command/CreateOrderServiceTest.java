package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.dto.result.CreateOrderResult;
import com.trustamarket.orderservice.order.application.exception.ProductNotPurchasableException;
import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort.ProductInfo;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @Mock ProductInfoPort productInfoPort;
    @Mock TransactionTemplate txTemplate;
    @InjectMocks CreateOrderService service;

    @BeforeEach
    void setUpTxTemplate() {
        lenient().when(txTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("주문 생성 — happy path (product ON_SALE)")
    void createOrder() {
        UUID productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        CreateOrderCommand cmd = new CreateOrderCommand(
                UUID.randomUUID(), "구매자",
                UUID.randomUUID(), "판매자",
                productId, "client-claim-name", 999L,   // client 입력은 무시되어야 함 (server snapshot 사용)
                OrderType.LOW,
                3_000L
        );
        when(productInfoPort.fetch(productId)).thenReturn(
                new ProductInfo(productId, sellerId, "정공-상품명", 100_000L, "ON_SALE"));
        when(orderRepository.saveNew(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderResult result = service.createOrder(cmd);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(result.totalAmount()).isEqualTo(103_000L);   // 100k(server price) + 3k shipping
        verify(historyRecorder).record(any(OrderId.class), eq(null), eq(OrderStatus.REQUESTED), eq(null));

        // server snapshot 우선 검증 — sellerId / productName / productPrice 모두 product-service 진실값 사용 (client 입력 무시)
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveNew(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getSeller().id()).isEqualTo(sellerId);          // ProductInfo 의 sellerId
        assertThat(saved.getProduct().name()).isEqualTo("정공-상품명");    // ProductInfo 의 name (cmd 의 "client-claim-name" 아님)
        assertThat(saved.getProduct().price().value()).isEqualTo(100_000L);  // ProductInfo 의 price (cmd 의 999L 아님)
    }

    @Test
    @DisplayName("product 가 ON_SALE 아니면 → ProductNotPurchasableException + 주문 저장 X")
    void createOrder_notOnSale_rejects() {
        UUID productId = UUID.randomUUID();
        CreateOrderCommand cmd = new CreateOrderCommand(
                UUID.randomUUID(), "구매자",
                UUID.randomUUID(), "판매자",
                productId, "상품", 100_000L,
                OrderType.LOW,
                3_000L
        );
        when(productInfoPort.fetch(productId)).thenReturn(
                new ProductInfo(productId, UUID.randomUUID(), "검수중상품", 100_000L, "PENDING_INSPECTION"));

        assertThatThrownBy(() -> service.createOrder(cmd))
                .isInstanceOf(ProductNotPurchasableException.class);

        verify(orderRepository, never()).saveNew(any(Order.class));
    }
}
