package com.trustamarket.orderservice.config;

import com.trustamarket.common.config.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

// common JpaConfig 의 @EnableJpaAuditing 을 그대로 사용하고, AuditorAware 만 본 서비스용으로 override.
// (@EnableJpaAuditing 을 service 측에서 추가 선언하면 jpaAuditingHandler 빈 중복)
// common 의 default auditorAware 는 SYSTEM_USER 만 반환 → 본 서비스에선 SecurityContext 의 UserDetailsImpl UUID 추출.
// @EnableJpaAuditing 은 @DataJpaTest 슬라이스에서 common JpaConfig 가 로드되지 않아 필요.
// 풀 컨텍스트(@SpringBootTest) 에선 common 의 @EnableJpaAuditing 과 중복 등록 → allow-bean-definition-overriding 으로 해결 (test/local 한정).
@Slf4j
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    // 인증 컨텍스트가 없을 때 사용하는 시스템 UUID (all zeros)
    static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // common 의 동명 빈을 override — spring.main.allow-bean-definition-overriding=true 필요.
    @Bean
    @Primary
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (isUnauthenticated(auth)) {
                return Optional.of(SYSTEM_USER);
            }
            return Optional.of(extractUserId(auth));
        };
    }

    private static boolean isUnauthenticated(Authentication auth) {
        return auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken;
    }

    // common 모듈의 LoginFilter 가 X-User-* 헤더 → UserDetailsImpl 을 SecurityContext 에 주입.
    // 인증된 요청이지만 principal 타입이 다른 경우(테스트용 임시 인증, 다른 필터의 토큰 등) 는
    // SYSTEM_USER 무음 폴백 시 감사 추적이 왜곡되므로 경고 로그를 남겨 가시성을 확보한다.
    private static UUID extractUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetailsImpl user) {
            return user.getUuid();
        }
        log.warn("[Auditing] 인증 성공이지만 principal 타입이 예상 밖({}) — SYSTEM_USER 로 폴백합니다.",
                principal == null ? "null" : principal.getClass().getName());
        return SYSTEM_USER;
    }
}
