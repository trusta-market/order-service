package com.trustamarket.orderservice.order.application.exception;

import com.trustamarket.common.exception.CustomException;
import org.springframework.http.HttpStatus;

// 400 Bad Request — 검색 조건 자체가 잘못됨 (예: fromDate > toDate)
// application 영역 검증 예외 — 도메인 예외와 구분
public class InvalidSearchCriteriaException extends CustomException {

    public InvalidSearchCriteriaException(String field, String detail) {
        super(HttpStatus.BAD_REQUEST,
                "검색 조건이 잘못됐습니다 (%s): %s".formatted(field, detail),
                field);
    }
}
