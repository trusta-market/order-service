package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.in.OrderSearchCriteria;
import com.trustamarket.orderservice.order.domain.exception.OrderNotFoundException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// OrderRepository port의 JPA 구현 — application 레이어가 의존하는 단일 진입점
// Spring Data Repository는 package-private(OrderJpaRepository) → 외부 노출 X
@Repository
@RequiredArgsConstructor
public class OrderJpaRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    // EntityManager 는 @PersistenceContext 로 field injection (RequiredArgsConstructor 와 호환).
    // saveNew() 에서 persist() 직접 호출 시 사용.
    @PersistenceContext
    private EntityManager em;

    // 기존 entity merge — findById 로 가져온 managed entity 갱신 시 사용.
    // 신규 INSERT 시엔 saveNew() 사용 (SELECT 1회 절감).
    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    // 신규 entity 명시적 persist — JpaRepository.save() 의 merge 경로 (SELECT 1회) 회피.
    // PK 가 도메인에서 미리 생성 (OrderId.generate) 되어 detached 가 아니라 new 임이 보장됨.
    @Override
    public Order saveNew(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        em.persist(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Order findByIdOrThrow(OrderId id) {
        return findById(id).orElseThrow(() -> new OrderNotFoundException(id.value()));
    }

    // Query

    @Override
    public Page<Order> findByBuyerId(UUID buyerId, Pageable pageable) {
        return jpaRepository.findByBuyerId(buyerId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findBySellerId(UUID sellerId, Pageable pageable) {
        return jpaRepository.findBySellerId(sellerId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId, Pageable pageable) {
        return jpaRepository.findByBuyerIdOrSellerId(buyerId, sellerId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Order> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Order> search(OrderSearchCriteria criteria, Pageable pageable) {
        // null criteria → 빈 조건으로 전체 조회 폴백 (NPE 방지)
        OrderSearchCriteria safe = criteria != null
                ? criteria
                : new OrderSearchCriteria(null, null, null, null);
        return jpaRepository.findAll(toSpecification(safe), pageable).map(mapper::toDomain);
    }

    // 동적 검색 조건 — null 필드는 무시
    private static Specification<OrderJpaEntity> toSpecification(OrderSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c.status() != null) {
                predicates.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.fromDate()));
            }
            if (c.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), c.toDate()));
            }
            if (c.buyerName() != null && !c.buyerName().isBlank()) {
                predicates.add(cb.like(root.get("buyerName"), "%" + c.buyerName() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
