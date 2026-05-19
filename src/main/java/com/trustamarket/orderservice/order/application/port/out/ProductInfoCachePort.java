package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort.ProductInfo;

import java.util.Optional;
import java.util.UUID;

// product-service 단건 응답을 in-JVM 으로 캐시. 1000 VU burst 시 같은 product 동시 조회 시
// 첫 호출만 Feign 가고 나머지는 cache hit → product-service 부하 + connection hold 시간 감소.
// 구현체 (Caffeine / Redis 등) 는 adapter/out/product 의 cache 어댑터에서 결정.
public interface ProductInfoCachePort {

    Optional<ProductInfo> get(UUID productId);

    void put(UUID productId, ProductInfo info);

    // product.sold-out / product.updated / product.deleted 등 외부 이벤트 받아 invalidate.
    // TODO: order.product.sold-out Kafka listener 에서 evict 호출 (race window 30s → 거의 0 으로 좁힘).
    //       현재는 TTL (30s) 만으로 stale 허용 — 결제 흐름 자체 영향 X (product-service 의 SOLD_OUT atomic UPDATE 가 oversell 방지).
    void evict(UUID productId);
}
