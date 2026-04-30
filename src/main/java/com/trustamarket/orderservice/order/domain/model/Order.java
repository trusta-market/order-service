package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.AmountMismatchException;
import com.trustamarket.orderservice.order.domain.exception.SelfPurchaseException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Order Aggregate Root
// 상태 전이는 OrderTransition.apply()로 위임
// 불변식: seller != buyer, totalAmount = product.price + shippingFee
// BaseUserEntity 미상속 — JPA 의존성 도메인 침범 차단
// audit 필드는 보유만 하고 자동 채움은 JPA Entity(adapter)가 BaseUserEntity 상속해 처리.
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    // 식별자
    private OrderId id;

    // Snapshot (cross-domain 데이터, 주문 시점에 freeze)
    private Buyer buyer;
    private Seller seller;
    private Product product;

    // 정책
    private OrderType type;
    private OrderStatus status;
    private Money shippingFee;
    private Money totalAmount;       // = product.price + shippingFee

    // 사유 (nullable, 해당 액션 발생 시 채워짐)
    private Reason cancelReason;
    private Reason returnReason;
    private Reason rejectReason;

    // 시간 / Audit (common BaseUserEntity 패턴 매칭)
    // JPA 어댑터에서 BaseUserEntity 상속 + AuditorAware로 자동 채움 예정.
    // 도메인은 필드만 들고 있음 (적극 관리 X — application/adapter가 채움).
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;       // nullable, soft delete 시 채움
    private UUID createdBy;
    private UUID updatedBy;
    private UUID deletedBy;          // nullable

    // 비즈니스 시간
    private Instant confirmedAt;     // nullable, CONFIRMED 전이 시 채움

    // 동시성 제어
    private int version;

    // 정적 팩토리 — 신규 주문 생성

    // 새 주문을 생성. 초기 상태는 REQUESTED.
    // totalAmount는 자동 계산 (product.price + shippingFee)
    // createdAt/updatedAt/createdBy/updatedBy 는 JPA Auditing이 자동 채움 (어댑터 레이어)
    public static Order create(
            Buyer buyer,
            Seller seller,
            Product product,
            OrderType type,
            Money shippingFee
    ) {
        validateInvariants(buyer, seller);

        Order order = new Order();
        order.id = OrderId.generate();
        order.buyer = buyer;
        order.seller = seller;
        order.product = product;
        order.type = type;
        order.status = OrderStatus.REQUESTED;
        order.shippingFee = shippingFee;
        order.totalAmount = product.price().plus(shippingFee);
        order.version = 0;
        return order;
    }

    // 정적 팩토리 - DB 복원용 (Builder 패턴)
    // create()와 달리 id/createdAt/version 등 기존 값을 그대로 보존
    // totalAmount 정합성 검증으로 DB 변조 감지
    // 사용: Order.restoreBuilder().id(...).buyer(...)...build()
    @Builder(builderMethodName = "restoreBuilder", buildMethodName = "build")
    private static Order restoreInternal(
            OrderId id,
            Buyer buyer,
            Seller seller,
            Product product,
            OrderType type,
            OrderStatus status,
            Money shippingFee,
            Money totalAmount,
            Reason cancelReason,
            Reason returnReason,
            Reason rejectReason,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            UUID createdBy,
            UUID updatedBy,
            UUID deletedBy,
            Instant confirmedAt,
            int version
    ) {
        validateInvariants(buyer, seller);
        validateAmountConsistency(product.price(), shippingFee, totalAmount);

        Order order = new Order();
        order.id = id;
        order.buyer = buyer;
        order.seller = seller;
        order.product = product;
        order.type = type;
        order.status = status;
        order.shippingFee = shippingFee;
        order.totalAmount = totalAmount;
        order.cancelReason = cancelReason;
        order.returnReason = returnReason;
        order.rejectReason = rejectReason;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        order.deletedAt = deletedAt;
        order.createdBy = createdBy;
        order.updatedBy = updatedBy;
        order.deletedBy = deletedBy;
        order.confirmedAt = confirmedAt;
        order.version = version;
        return order;
    }

    // 불변식 검증
    // 자기 상품 구매 금지 — buyer.id == seller.id 차단
    private static void validateInvariants(Buyer buyer, Seller seller) {
        if (Objects.equals(buyer.id(), seller.id())) {
            throw new SelfPurchaseException();
        }
    }

    // 복원 시 totalAmount 정합성 검증 (DB 데이터 변조 감지)
    private static void validateAmountConsistency(Money productPrice, Money shippingFee, Money totalAmount) {
        long expected = productPrice.value() + shippingFee.value();
        if (totalAmount.value() != expected) {
            throw new AmountMismatchException(expected, totalAmount.value());
        }
    }


    // 행위 메서드 - 결제 / 배송 / 확정 흐름

    // REQUESTED → PAYMENT_PENDING
    // 사용자가 결제 시작 액션 (POST /api/orders/{id}/payment-intents)
    public void requestPayment() {
        this.status = OrderTransition.apply(this.status, OrderAction.REQUEST_PAYMENT);
    }

    // PAYMENT_PENDING → PAID
    // Wallet의 PaymentCompleted 이벤트 수신 시
    public void markPaid() {
        this.status = OrderTransition.apply(this.status, OrderAction.MARK_PAID);
    }

    // PAID → SHIPPING
    // Delivery의 DeliveryStarted 이벤트 수신 시
    public void startShipping() {
        this.status = OrderTransition.apply(this.status, OrderAction.START_SHIPPING);
    }

    // SHIPPING → DELIVERED
    // Delivery의 DeliveryCompleted 이벤트 수신 시
    public void markDelivered() {
        this.status = OrderTransition.apply(this.status, OrderAction.MARK_DELIVERED);
    }

    // DELIVERED → CONFIRMED
    // 사용자가 구매 확정 액션 (POST /api/orders/{id}/confirmations)
    // confirmedAt에 확정 시각 기록 → PurchaseConfirmedEvent payload에 사용
    public void confirm(Instant at) {
        this.status = OrderTransition.apply(this.status, OrderAction.CONFIRM);
        this.confirmedAt = at;
    }

    // CONFIRMED → SETTLEMENT_PROCESSING
    // PurchaseConfirmed 이벤트 발행 직후 자체 전이 (정산 시작)
    public void startSettlement() {
        this.status = OrderTransition.apply(this.status, OrderAction.START_SETTLEMENT);
    }

    // SETTLEMENT_PROCESSING → COMPLETED
    // Settlement의 SettlementCompleted 이벤트 수신 시
    public void complete() {
        this.status = OrderTransition.apply(this.status, OrderAction.COMPLETE);
    }


    // 행위 메서드 — 취소 / 환불

    // 취소 (배송 시작 전 단계만 허용 — REQUESTED/PAYMENT_PENDING → CANCELLED, PAID → REFUND_PROCESSING)
    // OrderTransition 표가 잘못된 상태 조합을 자동 차단 (CancelNotAllowedException은 application 레이어에서 사전 검증 시 사용)
    // OrderCancelledEvent를 application/adapter 레이어에서 발행 (Wallet 환불 트리거)
    public void cancel(Reason reason) {
        this.status = OrderTransition.apply(this.status, OrderAction.CANCEL);
        this.cancelReason = reason;
    }

    // REFUND_PROCESSING → REFUND_COMPLETED
    // Wallet의 RefundCompleted 이벤트 수신 시
    public void markRefunded() {
        this.status = OrderTransition.apply(this.status, OrderAction.MARK_REFUNDED);
    }


    // 행위 메서드 - 반송
    // 권한 검증(ADMIN 전용 결정)은 application/adapter 레이어 책임 (@PreAuthorize)

    // 반송 요청 - 배송 시작 후만 허용 (SHIPPING/DELIVERED → RETURN_REQUESTED)
    // 사용자(구매자) 액션. OrderReturnRequestedEvent를 application 레이어에서 발행
    public void requestReturn(Reason reason) {
        this.status = OrderTransition.apply(this.status, OrderAction.REQUEST_RETURN);
        this.returnReason = reason;
    }

    // 반송 승인 - RETURN_REQUESTED → RETURN_APPROVED
    // ADMIN 액션 (권한 검증은 어댑터 레이어)
    public void approveReturn() {
        this.status = OrderTransition.apply(this.status, OrderAction.APPROVE_RETURN);
    }

    // 반송 거절 - RETURN_REQUESTED → RETURN_REJECTED
    // ADMIN 액션. 거절 사유 필수
    public void rejectReturn(Reason reason) {
        this.status = OrderTransition.apply(this.status, OrderAction.REJECT_RETURN);
        this.rejectReason = reason;
    }


    // 소프트 삭제 - deletedAt + deletedBy 기록
    // 멱등성: 이미 삭제된 엔티티에 재호출돼도 최초 시각/주체 보존
    // 시각은 도메인이 시계에 의존하지 않도록 외부 주입 (헥사고날 원칙 + 테스트 용이성)
    public void delete(UUID userId, Instant at) {
        if (this.deletedAt != null) {
            return;
        }
        this.deletedAt = at;
        this.deletedBy = userId;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
