package com.trustamarket.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// TransactionTemplate 빈 등록.
// Saga 패턴 (CreateOrderService / RequestPaymentService) 에서 Feign call 을 tx 밖으로 빼기 위해
// 짧은 DB 작업만 tx 안에 명시적으로 묶을 때 사용.
@Configuration
public class TransactionConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }
}
