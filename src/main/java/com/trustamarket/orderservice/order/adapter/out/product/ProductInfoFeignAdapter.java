package com.trustamarket.orderservice.order.adapter.out.product;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort;
import com.trustamarket.orderservice.order.domain.exception.ProductLookupException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

// ProductInfoPort 구현 — Feign 호출 + CommonResponse unwrap + 통신 실패 변환.
// 404(상품 없음) / 5xx(통신 장애) 모두 ProductLookupException 으로 일원화.
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInfoFeignAdapter implements ProductInfoPort {

    private final ProductFeignClient feignClient;

    @Override
    public ProductInfo fetch(UUID productId) {
        try {
            ResponseEntity<CommonResponse<ProductFeignClient.ProductInfoFeignResponse>> resp =
                    feignClient.getProductInfo(productId);
            if (resp == null || resp.getBody() == null || resp.getBody().data() == null) {
                throw new ProductLookupException(productId);
            }
            ProductFeignClient.ProductInfoFeignResponse data = resp.getBody().data();
            // 외부 응답 엄격 검증 — 필수 필드 누락/비정상 시 fail-fast (금액 0 변환 같은 silent corruption 차단)
            if (data.id() == null
                    || !productId.equals(data.id())
                    || data.sellerId() == null
                    || data.title() == null || data.title().isBlank()
                    || data.price() == null || data.price() < 0L
                    || data.status() == null || data.status().isBlank()) {
//                log.error("[Product] 응답 무결성 위반 — productId={}, data={}", productId, data);
                log.warn("[Product] 응답 무결성 위반 - productId={}, reason=INVALID_PRODUCT_RESPONSE", productId);
                throw new ProductLookupException(productId);
            }
            return new ProductInfo(
                    data.id(),
                    data.sellerId(),
                    data.title(),
                    data.price(),
                    data.status()
            );
        } catch (FeignException e) {
            log.error("[Product] Feign 호출 실패 — productId={}, status={}",
                    productId, e.status(), e);
            throw new ProductLookupException(productId, e);
        }
    }
}
