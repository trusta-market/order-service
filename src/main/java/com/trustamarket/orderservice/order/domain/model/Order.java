package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.AmountMismatchException;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;
import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;
import com.trustamarket.orderservice.order.domain.exception.InvalidTimestampException;
import com.trustamarket.orderservice.order.domain.exception.RestoreStateMismatchException;
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
        validateRequiredForCreate(buyer, seller, product, type, shippingFee);
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
        validateRequiredForRestore(id, buyer, seller, product, type, status, shippingFee, totalAmount);
        validateInvariants(buyer, seller);
        validateAmountConsistency(product.price(), shippingFee, totalAmount);
        validateStateConsistency(status, cancelReason, returnReason, rejectReason,
                confirmedAt, deletedAt, deletedBy);

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

    // 필수 인자 null 가드 — create()
    // 도메인 self-defending: 호출부 누락 시 NPE 대신 도메인 예외로 명시 실패
    private static void validateRequiredForCreate(
            Buyer buyer, Seller seller, Product product, OrderType type, Money shippingFee
    ) {
        if (buyer == null) throw new InvalidIdException("buyer");
        if (seller == null) throw new InvalidIdException("seller");
        if (product == null) throw new InvalidIdException("product");
        if (type == null) throw new InvalidIdException("orderType");
        if (shippingFee == null) throw new InvalidMoneyException("shippingFee");
    }

    // 필수 인자 null 가드 — restoreInternal()
    // create() 필수 인자 + 복원 전용(id, status, totalAmount). nullable: reasons, audit, confirmedAt
    private static void validateRequiredForRestore(
            OrderId id, Buyer buyer, Seller seller, Product product, OrderType type,
            OrderStatus status, Money shippingFee, Money totalAmount
    ) {
        if (id == null) throw new InvalidIdException("orderId");
        if (buyer == null) throw new InvalidIdException("buyer");
        if (seller == null) throw new InvalidIdException("seller");
        if (product == null) throw new InvalidIdException("product");
        if (type == null) throw new InvalidIdException("orderType");
        if (status == null) throw new InvalidIdException("status");
        if (shippingFee == null) throw new InvalidMoneyException("shippingFee");
        if (totalAmount == null) throw new InvalidMoneyException("totalAmount");
    }

    // 불변식 검증
    // 자기 상품 구매 금지 — buyer.id == seller.id 차단
    private static void validateInvariants(Buyer buyer, Seller seller) {
        if (Objects.equals(buyer.id(), seller.id())) {
            throw new SelfPurchaseException();
        }
    }

    // 복원 시 상태별 메타데이터 불변식 검증 — 도메인 행위 메서드를 우회하는 유일한 경로(restore)에서 잠금
    // 정상 도메인 흐름에서는 발생할 수 없는 조합 → DB 변조 감지
    private static void validateStateConsistency(
            OrderStatus status,
            Reason cancelReason,
            Reason returnReason,
            Reason rejectReason,
            Instant confirmedAt,
            Instant deletedAt,
            UUID deletedBy
    ) {
        // 확정 상태는 confirmedAt 필수
        if (status == OrderStatus.CONFIRMED && confirmedAt == null) {
            throw RestoreStateMismatchException.missingConfirmedAt(status);
        }

        // 취소 관련 상태는 cancelReason 필수
        if ((status == OrderStatus.CANCELLED
                || status == OrderStatus.CANCELLATION_PROCESSING
                || status == OrderStatus.CANCELLATION_COMPLETED)
                && cancelReason == null) {
            throw RestoreStateMismatchException.missingCancelReason(status);
        }

        // 반송 진입 이후는 returnReason 필수
        if ((status == OrderStatus.RETURN_REQUESTED
                || status == OrderStatus.RETURN_APPROVED
                || status == OrderStatus.RETURN_REJECTED)
                && returnReason == null) {
            throw RestoreStateMismatchException.missingReturnReason(status);
        }

        // 반송 거절은 rejectReason 추가 필수
        if (status == OrderStatus.RETURN_REJECTED && rejectReason == null) {
            throw RestoreStateMismatchException.missingRejectReason();
        }

        // soft delete 메타데이터는 둘 다 채워지거나 둘 다 비어야 함
        if ((deletedAt == null) != (deletedBy == null)) {
            throw RestoreStateMismatchException.inconsistentDeletionMetadata();
        }
    }

    // 복원 시 totalAmount 정합성 검증 (DB 데이터 변조 감지)
    // Math.addExact로 long 오버플로우 시 변조로 간주 (정상 주문 금액 범위에서 발생 X, 방어적 처리)
    private static void validateAmountConsistency(Money productPrice, Money shippingFee, Money totalAmount) {
        long expected;
        try {
            expected = Math.addExact(productPrice.value(), shippingFee.value());
        } catch (ArithmeticException overflow) {
            throw new AmountMismatchException(-1, totalAmount.value());
        }
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
        if (at == null) {
            throw new InvalidTimestampException("confirmedAt");
        }
        this.status = OrderTransition.apply(this.status, OrderAction.CONFIRM);
        this.confirmedAt = at;
    }


    // 행위 메서드 — 취소

    // 취소 (배송 시작 전까지만 허용 — REQUESTED/PAYMENT_PENDING → CANCELLED, PAID → CANCELLATION_PROCESSING)
    // OrderTransition 표가 잘못된 상태 조합을 자동 차단 (CancelNotAllowedException 은 application 레이어 사전 검증용)
    // PAID 분기에서는 application 이 후속으로 order.cancellation.requested Outbox 발행 → wallet escrow 복구.
    public void cancel(Reason reason) {
        if (reason == null) {
            throw new InvalidReasonException();
        }
        this.status = OrderTransition.apply(this.status, OrderAction.CANCEL);
        this.cancelReason = reason;
    }

    // CANCELLATION_PROCESSING → CANCELLATION_COMPLETED.
    // wallet.cancellation.completed 수신 시 호출 (escrow → buyer 복구 끝났다는 신호).
    public void markCancelled() {
        this.status = OrderTransition.apply(this.status, OrderAction.MARK_CANCELLED);
    }


    // 행위 메서드 - 반송
    // 권한 검증(ADMIN 전용 결정)은 application/adapter 레이어 책임 (@PreAuthorize)

    // 반송 요청 - 배송 시작 후만 허용 (SHIPPING/DELIVERED → RETURN_REQUESTED)
    // 사용자(구매자) 액션. OrderReturnRequestedEvent를 application 레이어에서 발행
    public void requestReturn(Reason reason) {
        if (reason == null) {
            throw new InvalidReasonException();
        }
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
        if (reason == null) {
            throw new InvalidReasonException();
        }
        this.status = OrderTransition.apply(this.status, OrderAction.REJECT_RETURN);
        this.rejectReason = reason;
    }


    // 소프트 삭제 - deletedAt + deletedBy 기록
    // 멱등성: 이미 삭제된 엔티티에 재호출돼도 최초 시각/주체 보존
    // 시각은 도메인이 시계에 의존하지 않도록 외부 주입 (헥사고날 원칙 + 테스트 용이성)
    public void delete(UUID userId, Instant at) {
        if (userId == null) {
            throw new InvalidIdException("deletedBy");
        }
        if (at == null) {
            throw new InvalidTimestampException("deletedAt");
        }
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
