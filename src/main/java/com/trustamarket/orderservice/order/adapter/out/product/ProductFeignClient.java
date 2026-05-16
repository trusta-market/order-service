package com.trustamarket.orderservice.order.adapter.out.product;

import com.trustamarket.common.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// product-service 의 internal API (`GET /internal/v1/products/{id}`) 호출.
// X-User-* 헤더는 common FeignConfig 의 RequestInterceptor 가 자동 전파.
@FeignClient(name = "product-service")
public interface ProductFeignClient {

    @GetMapping("/internal/v1/products/{productId}")
    CommonResponse<ProductInfoFeignResponse> getProductInfo(@PathVariable UUID productId);

    // product-service 의 ProductInfoResponse 와 1:1 매칭 (필드명/순서/타입).
    // price 는 product-service 가 Integer — order 측은 long 받음 (자동 widening).
    record ProductInfoFeignResponse(
            UUID id,
            UUID sellerId,
            String title,
            Long price,
            String status
    ) {}
}
