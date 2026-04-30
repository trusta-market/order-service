package com.trustamarket.orderservice.order.adapter.out.persistence.jpa;

import com.trustamarket.common.domain.BaseUserEntity;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

// p_order 테이블에 매핑되는 JPA Entity
// audit 필드(createdAt/updatedAt/deletedAt + createdBy/updatedBy/deletedBy)는 BaseUserEntity 상속 + Spring Data Auditing 자동
// snapshot은 평탄 컬럼(Embedded X)으로 — 쿼리 가독성 ↑
// soft delete: @SQLDelete + @SQLRestriction (실수로 hard delete 호출돼도 UPDATE로 변환되고 SELECT 시 자동 필터)
@Entity
@Table(name = "p_order")
@SQLDelete(sql = "UPDATE p_order SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity extends BaseUserEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    // Snapshot — 주문 시점에 freeze된 cross-domain 데이터
    @Column(name = "buyer_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID buyerId;

    @Column(name = "buyer_name", nullable = false, length = 100, updatable = false)
    private String buyerName;

    @Column(name = "seller_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "seller_name", nullable = false, length = 100, updatable = false)
    private String sellerName;

    @Column(name = "product_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 200, updatable = false)
    private String productName;

    @Column(name = "product_price", nullable = false, updatable = false)
    private long productPrice;

    // 정책
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "shipping_fee", nullable = false, updatable = false)
    private long shippingFee;

    @Column(name = "total_amount", nullable = false, updatable = false)
    private long totalAmount;

    // 사유 (nullable)
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "return_reason", length = 200)
    private String returnReason;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    // 비즈니스 시간 (nullable, CONFIRMED 전이 시 채움)
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    // 동시성 제어 — Hibernate가 UPDATE 시 자동 증가, 충돌 시 OptimisticLockException
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    // Mapper 전용 정적 팩토리 (Builder)
    // Mapper는 매 save마다 도메인 → 새 entity 생성 후 jpaRepository.save() — entity는 사실상 immutable로 사용
    @Builder
    private OrderJpaEntity(
            UUID id,
            UUID buyerId, String buyerName,
            UUID sellerId, String sellerName,
            UUID productId, String productName, long productPrice,
            OrderType type, OrderStatus status,
            long shippingFee, long totalAmount,
            String cancelReason, String returnReason, String rejectReason,
            Instant confirmedAt,
            int version
    ) {
        this.id = id;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.type = type;
        this.status = status;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.cancelReason = cancelReason;
        this.returnReason = returnReason;
        this.rejectReason = rejectReason;
        this.confirmedAt = confirmedAt;
        this.version = version;
    }
}
