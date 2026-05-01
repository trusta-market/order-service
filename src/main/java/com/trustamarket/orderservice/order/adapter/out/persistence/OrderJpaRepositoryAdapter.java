package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.query.OrderSearchCriteria;
import com.trustamarket.orderservice.order.domain.exception.OrderNotFoundException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
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

    // TODO: PK가 도메인에서 미리 생성되고 mapper가 매번 새 entity를 만드는 패턴이라
    //  JpaRepository.save()가 항상 merge() 타고 SELECT가 1회 추가
    //  Persistable<UUID> 구현은 동일 트랜잭션 내 재저장 시 세션 충돌 발생
    //  use case 흐름 정리되면서 함께 최적화 (findById → 도메인 행위 → 트랜잭션 dirty checking 패턴 검토)
    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
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
        return jpaRepository.findAll(toSpecification(criteria), pageable).map(mapper::toDomain);
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
