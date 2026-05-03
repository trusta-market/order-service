package com.trustamarket.orderservice.order.adapter.in.web;

import com.trustamarket.common.config.security.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

// Controller 슬라이스 테스트용 인증 헬퍼.
// SecurityUtil.getCurrentUser() 가 UserDetailsImpl 만 인정하므로 @WithMockUser 대신 본 헬퍼 사용.
final class TestAuth {

    private TestAuth() {}

    static Authentication memberAuth(UUID uuid, String name) {
        return authWith(uuid, name, "ROLE_MEMBER");
    }

    static Authentication adminAuth(UUID uuid) {
        return authWith(uuid, "관리자", "ROLE_ADMIN");
    }

    private static Authentication authWith(UUID uuid, String name, String role) {
        UserDetailsImpl user = UserDetailsImpl.builder()
                .uuid(uuid)
                .email("test@example.com")
                .name(name)
                .roles(role)
                .enabled(true)
                .build();
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
