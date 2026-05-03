package com.trustamarket.orderservice.order.application.port.out;

import java.util.UUID;

// product-service 단건 조회 port — 주문 생성 시 product 가 ON_SALE 인지 검증 + snapshot fetch.
// 구현체는 adapter/out/product 의 Feign 어댑터 (lb://product-service 또는 직접 URL).
public interface ProductInfoPort {

    ProductInfo fetch(UUID productId);

    // 주문 도메인이 필요한 최소 필드만. product-service 의 ProductInfoResponse 와 1:1 매칭.
    // status 는 String — order 측에 ProductStatus enum 두지 않기 위해 (cross-service enum 결합 회피).
    record ProductInfo(
            UUID id,
            UUID sellerId,
            String name,
            long price,
            String status
    ) {
        public boolean isOnSale() {
            return "ON_SALE".equals(status);
        }
    }
}
