package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Buyer / Seller / Product snapshot VO — 동일한 검증 패턴이라 한 클래스로 묶음
class SnapshotVoTest {

    @Nested
    class BuyerVo {
        @Test
        @DisplayName("정상 생성")
        void create() {
            UUID id = UUID.randomUUID();
            Buyer buyer = Buyer.of(id, "홍길동");
            assertThat(buyer.id()).isEqualTo(id);
            assertThat(buyer.name()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("id가 null이면 거부")
        void rejectNullId() {
            assertThatThrownBy(() -> Buyer.of(null, "홍길동"))
                    .isInstanceOf(InvalidIdException.class);
        }

        @Test
        @DisplayName("name이 빈 문자열이면 거부")
        void rejectBlankName() {
            UUID id = UUID.randomUUID();
            assertThatThrownBy(() -> Buyer.of(id, ""))
                    .isInstanceOf(InvalidNameException.class);
            assertThatThrownBy(() -> Buyer.of(id, null))
                    .isInstanceOf(InvalidNameException.class);
        }
    }

    @Nested
    class SellerVo {
        @Test
        @DisplayName("정상 생성")
        void create() {
            UUID id = UUID.randomUUID();
            Seller seller = Seller.of(id, "스토어");
            assertThat(seller.id()).isEqualTo(id);
            assertThat(seller.name()).isEqualTo("스토어");
        }

        @Test
        @DisplayName("id가 null이면 거부")
        void rejectNullId() {
            assertThatThrownBy(() -> Seller.of(null, "스토어"))
                    .isInstanceOf(InvalidIdException.class);
        }

        @Test
        @DisplayName("name이 빈 문자열이면 거부")
        void rejectBlankName() {
            assertThatThrownBy(() -> Seller.of(UUID.randomUUID(), "  "))
                    .isInstanceOf(InvalidNameException.class);
        }
    }

    @Nested
    class ProductVo {
        @Test
        @DisplayName("정상 생성")
        void create() {
            UUID id = UUID.randomUUID();
            Product product = Product.of(id, "노트북", Money.of(1_000_000));
            assertThat(product.id()).isEqualTo(id);
            assertThat(product.name()).isEqualTo("노트북");
            assertThat(product.price().value()).isEqualTo(1_000_000);
        }

        @Test
        @DisplayName("id가 null이면 거부")
        void rejectNullId() {
            assertThatThrownBy(() -> Product.of(null, "노트북", Money.of(1)))
                    .isInstanceOf(InvalidIdException.class);
        }

        @Test
        @DisplayName("name이 빈 문자열이면 거부")
        void rejectBlankName() {
            assertThatThrownBy(() -> Product.of(UUID.randomUUID(), "", Money.of(1)))
                    .isInstanceOf(InvalidNameException.class);
        }

        @Test
        @DisplayName("price가 null이면 거부")
        void rejectNullPrice() {
            assertThatThrownBy(() -> Product.of(UUID.randomUUID(), "노트북", null))
                    .isInstanceOf(InvalidIdException.class);
        }
    }

    @Nested
    class OrderIdVo {
        @Test
        @DisplayName("generate()는 UUID를 자동 생성")
        void generate() {
            OrderId a = OrderId.generate();
            OrderId b = OrderId.generate();
            assertThat(a.value()).isNotNull();
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("of(null)은 거부")
        void rejectNull() {
            assertThatThrownBy(() -> OrderId.of(null))
                    .isInstanceOf(InvalidIdException.class);
        }
    }
}
