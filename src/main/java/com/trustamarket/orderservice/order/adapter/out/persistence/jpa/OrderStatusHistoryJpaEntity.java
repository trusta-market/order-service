package com.trustamarket.orderservice.order.adapter.out.persistence.jpa;

import com.trustamarket.common.domain.BaseCreatedEntity;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;

import java.util.UUID;

// p_order_status_history — 상태 전이 이력 (append-only, 수정/삭제 없음)
// BaseCreatedEntity 상속 (createdAt만), createdBy는 직접 추가 (BaseCreatedEntity에 없음)
@Entity
@Table(name = "p_order_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistoryJpaEntity extends BaseCreatedEntity {

    @Id
    @Column(name = "order_status_history_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "prev_status", nullable = false, length = 30, updatable = false)
    private OrderStatus prevStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_status", nullable = false, length = 30, updatable = false)
    private OrderStatus nextStatus;

    @Column(name = "reason", length = 200, updatable = false)
    private String reason;

    // BaseCreatedEntity는 createdAt만 가짐. createdBy는 BaseUserEntity의 책임이라 따로 추가
    @CreatedBy
    @Column(name = "created_by", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID createdBy;

    @Builder
    private OrderStatusHistoryJpaEntity(
            UUID id, UUID orderId,
            OrderStatus prevStatus, OrderStatus nextStatus,
            String reason
    ) {
        this.id = id;
        this.orderId = orderId;
        this.prevStatus = prevStatus;
        this.nextStatus = nextStatus;
        this.reason = reason;
    }
}
