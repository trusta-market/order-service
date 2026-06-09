package com.trustamarket.orderservice.order.application.service.reconciliation;

import com.trustamarket.orderservice.order.application.port.out.PaymentReconciliationRepository;
import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

// reconciliation 큐를 주기적으로 폴링.
// 정상 흐름에서 row 거의 0 — 비정상 케이스 (timeout / 5xx 후 getUsage 도 실패) 만 등록됨.
//
// 폴링 주기 10초, 한 번에 처리할 row 수 50 으로 제한 (백프레셔).
// 처리 자체는 Processor 에 위임 — Feign 은 tx 밖, DB UPDATE 만 짧은 tx 안.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private static final int BATCH_SIZE = 50;

    private final PaymentReconciliationRepository reconciliationRepository;
    private final PaymentReconciliationProcessor processor;

    @Scheduled(fixedDelay = 10_000)
    public void process() {
        List<PaymentReconciliation> targets = reconciliationRepository.findRetriable(Instant.now(), BATCH_SIZE);
        if (targets.isEmpty()) {
            return;
        }
        log.debug("[Reconciliation] 폴링 — {} rows", targets.size());
        for (PaymentReconciliation r : targets) {
            try {
                processor.processOne(r);
            } catch (RuntimeException e) {
                // Processor 가 자체적으로 handleUnknown 까지 처리하지만, 만약 그 밖에서 던지면 다음 row 진행.
                log.error("[Reconciliation] processOne 실패 — orderId={}", r.getOrderId(), e);
            }
        }
    }
}
