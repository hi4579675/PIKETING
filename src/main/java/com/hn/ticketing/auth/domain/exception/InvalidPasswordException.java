package com.hn.ticketing.auth.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BaseException {
    public InvalidPasswordException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다");
    }
}