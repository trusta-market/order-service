package com.trustamarket.orderservice.order.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.common.config.security.CustomAccessDeniedHandler;
import com.trustamarket.common.config.security.CustomAuthenticationEntryPoint;
import com.trustamarket.common.config.security.LoginFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Controller 슬라이스 테스트에서 common SecurityConfig 가 의존하는 빈을 실제 인스턴스로 제공.
// CommonAutoConfiguration 은 @WebMvcTest 에서 로드되지 않아 수동 등록 필요.
// trustGatewayHeaders=true — 헤더 없으면 LoginFilter 가 통과시키고, 테스트는 authentication() PostProcessor 로 SecurityContext 주입.
@TestConfiguration
public class WebSliceTestConfig {

    @Bean
    public LoginFilter loginFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new LoginFilter(resolver, true);
    }

    @Bean
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new CustomAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
        return new CustomAccessDeniedHandler(objectMapper);
    }
}
