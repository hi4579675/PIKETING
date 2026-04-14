package com.hn.ticketing.seat.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidAdmissionTokenException extends BaseException {
    public InvalidAdmissionTokenException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_ADMISSION_TOKEN", "유효하지 않은 진입 토큰입니다");
    }
}
