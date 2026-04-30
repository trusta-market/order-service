package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import com.trustamarket.orderservice.order.domain.model.Product;
import com.trustamarket.orderservice.order.domain.model.Reason;
import com.trustamarket.orderservice.order.domain.model.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private static final Money PRICE = Money.of(100_000);
    private static final Money SHIPPING = Money.of(3_000);

    private final OrderMapper mapper = new OrderMapper();

    @Test
    @DisplayName("toEntity는 도메인 필드를 entity로 평탄화 (snapshot/Money/Reason 풀어냄)")
    void toEntityFlattening() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Order order = Order.create(
                Buyer.of(buyerId, "구매자"),
                Seller.of(sellerId, "판매자"),
                Product.of(productId, "상품", PRICE),
                OrderType.LOW,
                SHIPPING
        );

        OrderJpaEntity entity = mapper.toEntity(order);

        assertThat(entity.getId()).isEqualTo(order.getId().value());
        assertThat(entity.getBuyerId()).isEqualTo(buyerId);
        assertThat(entity.getBuyerName()).isEqualTo("구매자");
        assertThat(entity.getSellerId()).isEqualTo(sellerId);
        assertThat(entity.getProductId()).isEqualTo(productId);
        assertThat(entity.getProductPrice()).isEqualTo(PRICE.value());
        assertThat(entity.getType()).isEqualTo(OrderType.LOW);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(entity.getShippingFee()).isEqualTo(SHIPPING.value());
        assertThat(entity.getTotalAmount()).isEqualTo(PRICE.value() + SHIPPING.value());
        assertThat(entity.getCancelReason()).isNull();
        assertThat(entity.getConfirmedAt()).isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("toDomain은 entity에서 도메인 객체를 복원 (Order.restoreBuilder 검증 자동 작동)")
    void toDomainRoundTrip() {
        Order original = freshOrder();

        OrderJpaEntity entity = mapper.toEntity(original);
        // entity의 audit 필드(createdAt 등)는 JPA Auditing이 채우지만 단위 테스트에선 reflection으로 안 채움
        // toDomain이 audit null이어도 동작하도록 OrderId/주요 필드만 확인

        Order restored = mapper.toDomain(entity);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getBuyer()).isEqualTo(original.getBuyer());
        assertThat(restored.getSeller()).isEqualTo(original.getSeller());
        assertThat(restored.getProduct()).isEqualTo(original.getProduct());
        assertThat(restored.getStatus()).isEqualTo(original.getStatus());
        assertThat(restored.getTotalAmount()).isEqualTo(original.getTotalAmount());
        assertThat(restored.getVersion()).isEqualTo(original.getVersion());
    }

    @Test
    @DisplayName("cancelReason / returnReason / rejectReason이 nullable 그대로 round-trip")
    void nullableReasonsRoundTrip() {
        Order order = freshOrder();
        order.cancel(Reason.of("변심"));

        OrderJpaEntity entity = mapper.toEntity(order);
        assertThat(entity.getCancelReason()).isEqualTo("변심");
        assertThat(entity.getReturnReason()).isNull();
        assertThat(entity.getRejectReason()).isNull();

        Order restored = mapper.toDomain(entity);
        assertThat(restored.getCancelReason().value()).isEqualTo("변심");
        assertThat(restored.getReturnReason()).isNull();
        assertThat(restored.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("confirmedAt이 set된 Order round-trip")
    void confirmedAtRoundTrip() {
        Order order = freshOrder();
        order.requestPayment();
        order.markPaid();
        order.startShipping();
        order.markDelivered();
        Instant at = Instant.parse("2026-04-30T12:00:00Z");
        order.confirm(at);

        OrderJpaEntity entity = mapper.toEntity(order);
        assertThat(entity.getConfirmedAt()).isEqualTo(at);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        Order restored = mapper.toDomain(entity);
        assertThat(restored.getConfirmedAt()).isEqualTo(at);
        assertThat(restored.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("soft delete된 Order는 entity.isDeleted() true (BaseUserEntity.delete(UUID) 호출)")
    void softDeleteAppliedToEntity() {
        Order order = freshOrder();
        UUID userId = UUID.randomUUID();
        order.delete(userId, Instant.parse("2026-04-30T12:00:00Z"));

        OrderJpaEntity entity = mapper.toEntity(order);

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedBy()).isEqualTo(userId);
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    private static Order freshOrder() {
        return Order.create(
                Buyer.of(UUID.randomUUID(), "구매자"),
                Seller.of(UUID.randomUUID(), "판매자"),
                Product.of(UUID.randomUUID(), "상품", PRICE),
                OrderType.LOW,
                SHIPPING
        );
    }
}
