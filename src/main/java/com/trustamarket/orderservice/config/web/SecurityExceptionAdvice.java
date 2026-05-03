package com.trustamarket.orderservice.config.web;

import com.trustamarket.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @PreAuthorize 등 method security 가 throw 한 인가 예외를 RFC 9457 ErrorResponse 로 변환.
// common GlobalExceptionAdvice 의 handleException(Exception) 이 먼저 잡아 500 으로 처리되는 것을 막기 위해 HIGHEST_PRECEDENCE 로 등록.
// AuthorizationDeniedException 은 Spring Security 6.1+ 에서 method security 가 던지는 새로운 타입 (AccessDeniedException 의 서브타입).
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionAdvice {

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException e, HttpServletRequest request) {
        log.warn("[{}] Authorization Denied: {}", request.getRequestURI(), e.getMessage());
        return forbiddenResponse(request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException e, HttpServletRequest request) {
        log.warn("[{}] Access Denied: {}", request.getRequestURI(), e.getMessage());
        return forbiddenResponse(request);
    }

    private static ResponseEntity<ErrorResponse> forbiddenResponse(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.of(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        "접근 권한이 없습니다.",
                        request.getRequestURI())
        );
    }
}
