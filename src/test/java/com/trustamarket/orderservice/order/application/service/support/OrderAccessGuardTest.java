package com.trustamarket.orderservice.order.application.service.support;

import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import com.trustamarket.orderservice.order.domain.model.Product;
import com.trustamarket.orderservice.order.domain.model.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAccessGuardTest {

    private static Order orderOf(UUID buyerId, UUID sellerId) {
        return Order.create(
                Buyer.of(buyerId, "구매자"),
                Seller.of(sellerId, "판매자"),
                Product.of(UUID.randomUUID(), "상품", Money.of(100_000)),
                OrderType.LOW,
                Money.of(3_000)
        );
    }

    @Test
    @DisplayName("verifyBuyer — buyer 본인이면 통과")
    void verifyBuyerSuccess() {
        UUID buyerId = UUID.randomUUID();
        Order order = orderOf(buyerId, UUID.randomUUID());
        assertThatCode(() -> OrderAccessGuard.verifyBuyer(order, buyerId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyBuyer — buyer 아니면 throw")
    void verifyBuyerFail() {
        Order order = orderOf(UUID.randomUUID(), UUID.randomUUID());
        assertThatThrownBy(() -> OrderAccessGuard.verifyBuyer(order, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("verifyBuyerOrSeller — buyer 본인이면 통과")
    void verifyBuyerOrSeller_buyer() {
        UUID buyerId = UUID.randomUUID();
        Order order = orderOf(buyerId, UUID.randomUUID());
        assertThatCode(() -> OrderAccessGuard.verifyBuyerOrSeller(order, buyerId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyBuyerOrSeller — seller 본인이면 통과")
    void verifyBuyerOrSeller_seller() {
        UUID sellerId = UUID.randomUUID();
        Order order = orderOf(UUID.randomUUID(), sellerId);
        assertThatCode(() -> OrderAccessGuard.verifyBuyerOrSeller(order, sellerId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("verifyBuyerOrSeller — buyer/seller 둘 다 아니면 throw")
    void verifyBuyerOrSeller_stranger() {
        Order order = orderOf(UUID.randomUUID(), UUID.randomUUID());
        assertThatThrownBy(() -> OrderAccessGuard.verifyBuyerOrSeller(order, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }
}
