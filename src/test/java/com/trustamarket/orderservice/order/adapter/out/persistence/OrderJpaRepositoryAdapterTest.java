package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.config.JpaAuditingConfig;
import com.trustamarket.orderservice.order.domain.exception.AmountMismatchException;
import com.trustamarket.orderservice.order.domain.exception.OrderNotFoundException;
import com.trustamarket.orderservice.order.domain.exception.RestoreStateMismatchException;
import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import com.trustamarket.orderservice.order.domain.model.Product;
import com.trustamarket.orderservice.order.domain.model.Reason;
import com.trustamarket.orderservice.order.domain.model.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)  // Testcontainers postgres 사용 (H2 대체 X)
@Testcontainers
@Import({OrderJpaRepositoryAdapter.class, OrderMapper.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class OrderJpaRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderJpaRepositoryAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager em;

    private static final Money PRICE = Money.of(100_000);
    private static final Money SHIPPING = Money.of(3_000);

    @Test
    @DisplayName("save → findById round-trip + audit 자동 채움 (createdAt/createdBy)")
    void saveAndFind() {
        Order saved = adapter.save(freshOrder());

        Order found = adapter.findByIdOrThrow(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getStatus()).isEqualTo(saved.getStatus());
        assertThat(found.getTotalAmount()).isEqualTo(saved.getTotalAmount());
        assertThat(found.getCreatedAt()).isNotNull();        // JPA Auditing 자동
        assertThat(found.getCreatedBy()).isNotNull();        // AuditorAware fallback (system UUID)
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getUpdatedBy()).isNotNull();
    }

    @Test
    @DisplayName("findByIdOrThrow는 없는 id에 대해 OrderNotFoundException")
    void findByIdOrThrowMissing() {
        OrderId missing = OrderId.generate();

        assertThatThrownBy(() -> adapter.findByIdOrThrow(missing))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("soft delete 후 findById는 empty (@SQLRestriction 자동 필터)")
    void softDeleteFiltered() {
        Order saved = adapter.save(freshOrder());
        saved.delete(UUID.randomUUID(), Instant.now());
        adapter.save(saved);
        em.flush();   // 영속성 컨텍스트 캐시 무효화 — @SQLRestriction이 실제 SELECT에만 적용
        em.clear();

        assertThat(adapter.findById(saved.getId())).isEmpty();

        // native query로 확인 — 행은 살아있고 deleted_at만 채워졌어야 함
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM p_order WHERE id = ? AND deleted_at IS NOT NULL",
                Integer.class, saved.getId().value());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("DB 변조 감지 — totalAmount 정합성 위반 시 AmountMismatchException")
    void dbTamperingAmountMismatch() {
        UUID id = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Instant now = Instant.now();

        // Mapper 우회 — 잘못된 totalAmount(99)로 직접 INSERT
        jdbcTemplate.update("""
                INSERT INTO p_order (id, buyer_id, buyer_name, seller_id, seller_name,
                                     product_id, product_name, product_price, type, status,
                                     shipping_fee, total_amount,
                                     created_at, updated_at, created_by, updated_by, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, buyerId, "구매자", sellerId, "판매자",
                UUID.randomUUID(), "상품", 100_000L,
                "LOW", "REQUESTED",
                3_000L, 99L,    // ← totalAmount 변조 (정상이면 103_000)
                Timestamp.from(now), Timestamp.from(now), buyerId, buyerId, 0);

        assertThatThrownBy(() -> adapter.findByIdOrThrow(OrderId.of(id)))
                .isInstanceOf(AmountMismatchException.class);
    }

    @Test
    @DisplayName("DB 변조 감지 — CONFIRMED인데 confirmed_at = NULL이면 RestoreStateMismatch")
    void dbTamperingMissingConfirmedAt() {
        UUID id = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Instant now = Instant.now();

        jdbcTemplate.update("""
                INSERT INTO p_order (id, buyer_id, buyer_name, seller_id, seller_name,
                                     product_id, product_name, product_price, type, status,
                                     shipping_fee, total_amount,
                                     created_at, updated_at, created_by, updated_by, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, buyerId, "구매자", sellerId, "판매자",
                UUID.randomUUID(), "상품", 100_000L,
                "LOW", "CONFIRMED",   // ← CONFIRMED인데 confirmed_at NULL (변조)
                3_000L, 103_000L,
                Timestamp.from(now), Timestamp.from(now), buyerId, buyerId, 0);

        assertThatThrownBy(() -> adapter.findByIdOrThrow(OrderId.of(id)))
                .isInstanceOf(RestoreStateMismatchException.class);
    }

    @Test
    @DisplayName("동시 수정 — stale version으로 save 시 OptimisticLockingFailureException")
    void optimisticLockConflict() {
        Order saved = adapter.save(freshOrder());
        em.flush();
        em.clear();

        // 같은 행을 두 번 fetch — 둘 다 version=0
        Order one = adapter.findByIdOrThrow(saved.getId());
        Order two = adapter.findByIdOrThrow(saved.getId());

        // one 먼저 commit → DB의 version 0 → 1
        one.requestPayment();
        adapter.save(one);
        em.flush();
        em.clear();

        // two는 stale version=0 상태로 commit 시도 → DB version=1과 불일치 → 예외
        two.requestPayment();
        assertThatThrownBy(() -> {
            adapter.save(two);
            em.flush();
        }).isInstanceOf(OptimisticLockingFailureException.class);
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
