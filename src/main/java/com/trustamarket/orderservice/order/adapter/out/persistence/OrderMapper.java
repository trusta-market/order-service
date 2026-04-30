package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.Product;
import com.trustamarket.orderservice.order.domain.model.Reason;
import com.trustamarket.orderservice.order.domain.model.Seller;
import org.springframework.stereotype.Component;

// 도메인 Order ↔ JPA Entity 양방향 매퍼
// toDomain은 Order.restoreBuilder()를 거쳐 4단계 검증(null/자기구매/totalAmount/상태별 메타데이터) 자동 작동 → DB 변조 감지
// toEntity는 매번 새 entity 생성 (immutable 패턴). soft delete는 BaseUserEntity.delete(UUID) 호출
@Component
public class OrderMapper {

    public OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(order.getId().value())
                .buyerId(order.getBuyer().id())
                .buyerName(order.getBuyer().name())
                .sellerId(order.getSeller().id())
                .sellerName(order.getSeller().name())
                .productId(order.getProduct().id())
                .productName(order.getProduct().name())
                .productPrice(order.getProduct().price().value())
                .type(order.getType())
                .status(order.getStatus())
                .shippingFee(order.getShippingFee().value())
                .totalAmount(order.getTotalAmount().value())
                .cancelReason(reasonOrNull(order.getCancelReason()))
                .returnReason(reasonOrNull(order.getReturnReason()))
                .rejectReason(reasonOrNull(order.getRejectReason()))
                .confirmedAt(order.getConfirmedAt())
                .version(order.getVersion())
                .build();

        if (order.isDeleted()) {
            // BaseUserEntity.delete(UUID): deletedAt = Instant.now(), deletedBy = userId
            // 도메인의 deletedAt(at)과 ms 차이 나는 건 무방 — 비즈니스 의미상 동일한 "삭제 시각"
            entity.delete(order.getDeletedBy());
        }

        return entity;
    }

    public Order toDomain(OrderJpaEntity entity) {
        return Order.restoreBuilder()
                .id(OrderId.of(entity.getId()))
                .buyer(Buyer.of(entity.getBuyerId(), entity.getBuyerName()))
                .seller(Seller.of(entity.getSellerId(), entity.getSellerName()))
                .product(Product.of(entity.getProductId(), entity.getProductName(),
                        Money.of(entity.getProductPrice())))
                .type(entity.getType())
                .status(entity.getStatus())
                .shippingFee(Money.of(entity.getShippingFee()))
                .totalAmount(Money.of(entity.getTotalAmount()))
                .cancelReason(toReason(entity.getCancelReason()))
                .returnReason(toReason(entity.getReturnReason()))
                .rejectReason(toReason(entity.getRejectReason()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .deletedBy(entity.getDeletedBy())
                .confirmedAt(entity.getConfirmedAt())
                .version(entity.getVersion())
                .build();
    }

    private static String reasonOrNull(Reason reason) {
        return reason == null ? null : reason.value();
    }

    private static Reason toReason(String value) {
        return value == null ? null : Reason.of(value);
    }
}
