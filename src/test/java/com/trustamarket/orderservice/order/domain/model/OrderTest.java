package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.AmountMismatchException;
import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;
import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;
import com.trustamarket.orderservice.order.domain.exception.InvalidStatusTransitionException;
import com.trustamarket.orderservice.order.domain.exception.InvalidTimestampException;
import com.trustamarket.orderservice.order.domain.exception.RestoreStateMismatchException;
import com.trustamarket.orderservice.order.domain.exception.SelfPurchaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Money PRICE = Money.of(100_000);
    private static final Money SHIPPING = Money.of(3_000);

    private static Buyer buyer() {
        return Buyer.of(UUID.randomUUID(), "구매자");
    }

    private static Seller seller() {
        return Seller.of(UUID.randomUUID(), "판매자");
    }

    private static Product product() {
        return Product.of(UUID.randomUUID(), "상품", PRICE);
    }

    private static Order newOrder() {
        return Order.create(buyer(), seller(), product(), OrderType.LOW, SHIPPING);
    }

    @Nested
    class Create {
        @Test
        @DisplayName("create()는 REQUESTED 상태로 시작하고 totalAmount는 자동 계산")
        void create() {
            Order order = newOrder();

            assertThat(order.getId()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
            assertThat(order.getTotalAmount().value()).isEqualTo(PRICE.value() + SHIPPING.value());
            assertThat(order.getVersion()).isZero();
        }

        @Test
        @DisplayName("buyer.id == seller.id 면 SelfPurchaseException")
        void rejectSelfPurchase() {
            UUID sameId = UUID.randomUUID();
            Buyer buyer = Buyer.of(sameId, "본인");
            Seller seller = Seller.of(sameId, "본인");

            assertThatThrownBy(() -> Order.create(buyer, seller, product(), OrderType.LOW, SHIPPING))
                    .isInstanceOf(SelfPurchaseException.class);
        }

        @Test
        @DisplayName("필수 인자 null이면 도메인 예외 (NPE 노출 차단)")
        void rejectNullArguments() {
            assertThatThrownBy(() -> Order.create(null, seller(), product(), OrderType.LOW, SHIPPING))
                    .isInstanceOf(InvalidIdException.class);
            assertThatThrownBy(() -> Order.create(buyer(), null, product(), OrderType.LOW, SHIPPING))
                    .isInstanceOf(InvalidIdException.class);
            assertThatThrownBy(() -> Order.create(buyer(), seller(), null, OrderType.LOW, SHIPPING))
                    .isInstanceOf(InvalidIdException.class);
            assertThatThrownBy(() -> Order.create(buyer(), seller(), product(), null, SHIPPING))
                    .isInstanceOf(InvalidIdException.class);
            assertThatThrownBy(() -> Order.create(buyer(), seller(), product(), OrderType.LOW, null))
                    .isInstanceOf(InvalidMoneyException.class);
        }
    }

    @Nested
    class HappyPath {
        @Test
        @DisplayName("REQUESTED → CONFIRMED 전체 흐름")
        void fullFlow() {
            Order order = newOrder();

            order.requestPayment();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

            order.markPaid();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            order.startShipping();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);

            order.markDelivered();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);

            Instant confirmedAt = Instant.parse("2026-04-30T12:00:00Z");
            order.confirm(confirmedAt);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getConfirmedAt()).isEqualTo(confirmedAt);
        }

        @Test
        @DisplayName("REQUESTED 상태에서 markPaid()는 차단")
        void invalidJump() {
            Order order = newOrder();
            assertThatThrownBy(order::markPaid)
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    class Cancel {
        @Test
        @DisplayName("REQUESTED에서 cancel은 CANCELLED + reason 채워짐")
        void cancelBeforePayment() {
            Order order = newOrder();
            Reason reason = Reason.of("변심");

            order.cancel(reason);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("PAID에서 cancel은 CANCELLATION_PROCESSING")
        void cancelAfterPayment() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();

            order.cancel(Reason.of("환불"));

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLATION_PROCESSING);
        }

        @Test
        @DisplayName("CANCELLATION_PROCESSING에서 markCancelled는 CANCELLATION_COMPLETED")
        void markCancelled() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.cancel(Reason.of("환불"));

            order.markCancelled();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLATION_COMPLETED);
        }

        @Test
        @DisplayName("SHIPPING에서 cancel 차단")
        void cancelAfterShipping() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();

            assertThatThrownBy(() -> order.cancel(Reason.of("늦음")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    class Return {
        @Test
        @DisplayName("SHIPPING에서 requestReturn은 RETURN_REQUESTED + reason 채워짐")
        void requestReturnFromShipping() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();

            Reason reason = Reason.of("불량품");
            order.requestReturn(reason);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
            assertThat(order.getReturnReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("approveReturn은 RETURN_APPROVED")
        void approveReturn() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();
            order.requestReturn(Reason.of("불량"));

            order.approveReturn();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);
        }

        @Test
        @DisplayName("rejectReturn은 RETURN_REJECTED + rejectReason 채워짐")
        void rejectReturn() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();
            order.requestReturn(Reason.of("불량"));

            Reason rejectReason = Reason.of("증빙 부족");
            order.rejectReturn(rejectReason);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REJECTED);
            assertThat(order.getRejectReason()).isEqualTo(rejectReason);
        }

        @Test
        @DisplayName("PAID에서 requestReturn 차단 (배송 시작 전)")
        void rejectReturnBeforeShipping() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();

            assertThatThrownBy(() -> order.requestReturn(Reason.of("불량")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    class SoftDelete {
        @Test
        @DisplayName("delete()는 deletedAt + deletedBy 기록")
        void delete() {
            Order order = newOrder();
            UUID userId = UUID.randomUUID();
            Instant at = Instant.parse("2026-04-30T12:00:00Z");

            order.delete(userId, at);

            assertThat(order.isDeleted()).isTrue();
            assertThat(order.getDeletedAt()).isEqualTo(at);
            assertThat(order.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("이미 삭제된 엔티티에 재호출해도 최초 시각/주체 보존 (멱등성)")
        void deleteIdempotent() {
            Order order = newOrder();
            UUID firstUser = UUID.randomUUID();
            Instant first = Instant.parse("2026-04-30T12:00:00Z");
            Instant later = Instant.parse("2026-04-30T13:00:00Z");

            order.delete(firstUser, first);
            order.delete(UUID.randomUUID(), later);

            assertThat(order.getDeletedAt()).isEqualTo(first);
            assertThat(order.getDeletedBy()).isEqualTo(firstUser);
        }

        @Test
        @DisplayName("userId가 null이면 InvalidIdException")
        void rejectNullUserId() {
            Order order = newOrder();
            assertThatThrownBy(() -> order.delete(null, Instant.now()))
                    .isInstanceOf(InvalidIdException.class);
        }

        @Test
        @DisplayName("at이 null이면 InvalidTimestampException")
        void rejectNullAt() {
            Order order = newOrder();
            assertThatThrownBy(() -> order.delete(UUID.randomUUID(), null))
                    .isInstanceOf(InvalidTimestampException.class);
        }
    }

    @Nested
    class NullGuards {
        @Test
        @DisplayName("confirm(null)은 InvalidTimestampException")
        void confirmNull() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();
            order.markDelivered();
            assertThatThrownBy(() -> order.confirm(null))
                    .isInstanceOf(InvalidTimestampException.class);
        }

        @Test
        @DisplayName("cancel(null)은 InvalidReasonException")
        void cancelNull() {
            Order order = newOrder();
            assertThatThrownBy(() -> order.cancel(null))
                    .isInstanceOf(InvalidReasonException.class);
        }

        @Test
        @DisplayName("requestReturn(null)은 InvalidReasonException")
        void requestReturnNull() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();
            assertThatThrownBy(() -> order.requestReturn(null))
                    .isInstanceOf(InvalidReasonException.class);
        }

        @Test
        @DisplayName("rejectReturn(null)은 InvalidReasonException")
        void rejectReturnNull() {
            Order order = newOrder();
            order.requestPayment();
            order.markPaid();
            order.startShipping();
            order.requestReturn(Reason.of("불량"));
            assertThatThrownBy(() -> order.rejectReturn(null))
                    .isInstanceOf(InvalidReasonException.class);
        }
    }

    @Nested
    class Restore {
        @Test
        @DisplayName("restoreBuilder는 기존 값을 그대로 보존")
        void restore() {
            OrderId id = OrderId.generate();
            Buyer buyer = buyer();
            Seller seller = seller();
            Product product = product();
            Money totalAmount = PRICE.plus(SHIPPING);
            Instant now = Instant.parse("2026-04-30T12:00:00Z");

            Order order = Order.restoreBuilder()
                    .id(id)
                    .buyer(buyer)
                    .seller(seller)
                    .product(product)
                    .type(OrderType.HIGH)
                    .status(OrderStatus.PAID)
                    .shippingFee(SHIPPING)
                    .totalAmount(totalAmount)
                    .createdAt(now)
                    .updatedAt(now)
                    .createdBy(buyer.id())
                    .updatedBy(buyer.id())
                    .version(3)
                    .build();

            assertThat(order.getId()).isEqualTo(id);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.getVersion()).isEqualTo(3);
            assertThat(order.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("totalAmount가 productPrice + shippingFee와 다르면 AmountMismatch")
        void rejectAmountMismatch() {
            assertThatThrownBy(() -> Order.restoreBuilder()
                    .id(OrderId.generate())
                    .buyer(buyer())
                    .seller(seller())
                    .product(product())
                    .type(OrderType.LOW)
                    .status(OrderStatus.REQUESTED)
                    .shippingFee(SHIPPING)
                    .totalAmount(Money.of(999_999))
                    .version(0)
                    .build())
                    .isInstanceOf(AmountMismatchException.class);
        }

        @Test
        @DisplayName("buyer.id == seller.id이면 SelfPurchase (DB 변조 감지)")
        void rejectSelfPurchase() {
            UUID sameId = UUID.randomUUID();
            Buyer buyer = Buyer.of(sameId, "본인");
            Seller seller = Seller.of(sameId, "본인");

            assertThatThrownBy(() -> Order.restoreBuilder()
                    .id(OrderId.generate())
                    .buyer(buyer)
                    .seller(seller)
                    .product(product())
                    .type(OrderType.LOW)
                    .status(OrderStatus.REQUESTED)
                    .shippingFee(SHIPPING)
                    .totalAmount(PRICE.plus(SHIPPING))
                    .version(0)
                    .build())
                    .isInstanceOf(SelfPurchaseException.class);
        }

        // 상태별 메타데이터 불변식 검증 (DB 변조 감지) — 도메인 행위 메서드를 우회하는 유일한 경로

        private static Order.OrderBuilder baseBuilder(OrderStatus status) {
            return Order.restoreBuilder()
                    .id(OrderId.generate())
                    .buyer(buyer())
                    .seller(seller())
                    .product(product())
                    .type(OrderType.LOW)
                    .status(status)
                    .shippingFee(SHIPPING)
                    .totalAmount(PRICE.plus(SHIPPING))
                    .version(0);
        }

        @Test
        @DisplayName("CONFIRMED인데 confirmedAt == null이면 RestoreStateMismatch")
        void confirmedWithoutConfirmedAt() {
            assertThatThrownBy(() -> baseBuilder(OrderStatus.CONFIRMED).build())
                    .isInstanceOf(RestoreStateMismatchException.class);
        }

        @Test
        @DisplayName("CANCELLED/CANCELLATION_PROCESSING/CANCELLATION_COMPLETED는 cancelReason 필수")
        void cancelledWithoutCancelReason() {
            assertThatThrownBy(() -> baseBuilder(OrderStatus.CANCELLED).build())
                    .isInstanceOf(RestoreStateMismatchException.class);
            assertThatThrownBy(() -> baseBuilder(OrderStatus.CANCELLATION_PROCESSING).build())
                    .isInstanceOf(RestoreStateMismatchException.class);
            assertThatThrownBy(() -> baseBuilder(OrderStatus.CANCELLATION_COMPLETED).build())
                    .isInstanceOf(RestoreStateMismatchException.class);
        }

        @Test
        @DisplayName("RETURN_REQUESTED/APPROVED/REJECTED는 returnReason 필수")
        void returnWithoutReturnReason() {
            assertThatThrownBy(() -> baseBuilder(OrderStatus.RETURN_REQUESTED).build())
                    .isInstanceOf(RestoreStateMismatchException.class);
            assertThatThrownBy(() -> baseBuilder(OrderStatus.RETURN_APPROVED).build())
                    .isInstanceOf(RestoreStateMismatchException.class);
        }

        @Test
        @DisplayName("RETURN_REJECTED는 rejectReason 추가 필수")
        void rejectedWithoutRejectReason() {
            assertThatThrownBy(() -> baseBuilder(OrderStatus.RETURN_REJECTED)
                    .returnReason(Reason.of("불량"))
                    .build())
                    .isInstanceOf(RestoreStateMismatchException.class);
        }

        @Test
        @DisplayName("deletedAt만 채워지고 deletedBy가 null이면 RestoreStateMismatch")
        void halfDeletedAt() {
            assertThatThrownBy(() -> baseBuilder(OrderStatus.REQUESTED)
                    .deletedAt(Instant.parse("2026-04-30T12:00:00Z"))
                    .build())
                    .isInstanceOf(RestoreStateMismatchException.class);
        }

        @Test
        @DisplayName("deletedBy만 채워지고 deletedAt이 null이면 RestoreStateMismatch")
        void halfDeletedBy() {
            assertThatThrownBy(() -> baseBuilder(OrderStatus.REQUESTED)
                    .deletedBy(UUID.randomUUID())
                    .build())
                    .isInstanceOf(RestoreStateMismatchException.class);
        }

        @Test
        @DisplayName("CONFIRMED + confirmedAt 정상 조합은 통과")
        void confirmedWithConfirmedAt() {
            Order order = baseBuilder(OrderStatus.CONFIRMED)
                    .confirmedAt(Instant.parse("2026-04-30T12:00:00Z"))
                    .build();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        }
    }
}
