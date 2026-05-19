package com.trustamarket.orderservice.order.adapter.out.product;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoCachePort;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort.ProductInfo;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * ProductInfoCachePort 의 Caffeine (in-JVM) 구현.
 * pod 별 독립 cache — 분산 cache 필요 시 RedisProductInfoCache 추가 + @Primary 또는 @Profile 로 교체.
 *
 * TTL 30s — product.sold-out 같은 외부 이벤트 받아 evict 하면 race window 더 좁아짐.
 */
@Component
public class CaffeineProductInfoCache implements ProductInfoCachePort {

    private final Cache<UUID, ProductInfo> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    @Override
    public Optional<ProductInfo> get(UUID productId) {
        return Optional.ofNullable(cache.getIfPresent(productId));
    }

    @Override
    public void put(UUID productId, ProductInfo info) {
        cache.put(productId, info);
    }

    @Override
    public void evict(UUID productId) {
        cache.invalidate(productId);
    }
}
