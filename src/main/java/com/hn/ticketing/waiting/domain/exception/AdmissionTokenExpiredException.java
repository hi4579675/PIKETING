package com.hn.ticketing.waiting.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

// → 토큰이 없거나 만료됨 (seat 모듈이 검증 시 사용)
// → HTTP 401 Unauthorized
public class AdmissionTokenExpiredException extends BaseException {
    public AdmissionTokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, "ADMISSION_TOKEN_EXPIRED", "진입 토큰이 만료되었거나 유효하지 않습니다");
    }
}