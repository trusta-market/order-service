package com.trustamarket.orderservice.order.adapter.out.product;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort;
import com.trustamarket.orderservice.order.domain.exception.ProductLookupException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            CommonResponse<ProductFeignClient.ProductInfoFeignResponse> resp =
                    feignClient.getProductInfo(productId);
            ProductFeignClient.ProductInfoFeignResponse data = resp.data();
            if (data == null) {
                throw new ProductLookupException(productId);
            }
            return new ProductInfo(
                    data.id(),
                    data.sellerId(),
                    data.title(),
                    data.price() == null ? 0L : data.price(),
                    data.status()
            );
        } catch (FeignException e) {
            log.error("[Product] Feign 호출 실패 — productId={}, status={}",
                    productId, e.status(), e);
            throw new ProductLookupException(productId, e);
        }
    }
}
