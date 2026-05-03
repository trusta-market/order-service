package com.trustamarket.orderservice.config;

import com.trustamarket.common.config.security.UserDetailsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

// common JpaConfig 의 @EnableJpaAuditing 을 그대로 사용하고, AuditorAware 만 본 서비스용으로 override.
// (@EnableJpaAuditing 을 service 측에서 추가 선언하면 jpaAuditingHandler 빈 중복)
// common 의 default auditorAware 는 SYSTEM_USER 만 반환 → 본 서비스에선 SecurityContext 의 UserDetailsImpl UUID 추출.
@Configuration
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
            return Optional.of(extractUserIdOrFallback(auth));
        };
    }

    private static boolean isUnauthenticated(Authentication auth) {
        return auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken;
    }

    // common 모듈의 LoginFilter 가 X-User-* 헤더 → UserDetailsImpl 을 SecurityContext 에 주입.
    private static UUID extractUserIdOrFallback(Authentication auth) {
        if (auth.getPrincipal() instanceof UserDetailsImpl user) {
            return user.getUuid();
        }
        return SYSTEM_USER;
    }
}
