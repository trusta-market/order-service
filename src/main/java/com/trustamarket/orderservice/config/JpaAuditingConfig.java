package com.trustamarket.orderservice.config;

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
// 인증 미구현 단계에서는 SYSTEM_USER UUID로 fallback (PR 6에서 인증 도입 시 본체 작성)
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    // 인증 컨텍스트가 없을 때 사용하는 시스템 UUID (all zeros)
    static final UUID SYSTEM_USER = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null
                    || !auth.isAuthenticated()
                    || auth instanceof AnonymousAuthenticationToken) {
                return Optional.of(SYSTEM_USER);
            }
            // PR 6 인증 구현 후 principal 구조에 맞게 UUID 추출 (예: ((UserPrincipal) auth.getPrincipal()).getUserId())
            return Optional.of(SYSTEM_USER);
        };
    }
}
