package com.trustamarket.orderservice.config;

import com.trustamarket.common.config.security.UserDetailsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

// Spring Data Auditing 활성화 + AuditorAware 빈 등록
// BaseUserEntity의 createdBy/updatedBy를 SecurityContext의 사용자 UUID로 자동 채움
// 인증 컨텍스트가 없는 부트스트랩/배치/시스템 트랜잭션에서는 SYSTEM_USER로 fallback
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    // 인증 컨텍스트가 없을 때 사용하는 시스템 UUID (all zeros)
    static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    AuditorAware<UUID> auditorProvider() {
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

    // common 모듈의 LoginFilter가 X-User-* 헤더 → UserDetailsImpl을 SecurityContext에 주입.
    // principal 타입이 다르면(테스트용 임시 인증 등) SYSTEM_USER로 fallback.
    private static UUID extractUserIdOrFallback(Authentication auth) {
        if (auth.getPrincipal() instanceof UserDetailsImpl user) {
            return user.getUuid();
        }
        return SYSTEM_USER;
    }
}
