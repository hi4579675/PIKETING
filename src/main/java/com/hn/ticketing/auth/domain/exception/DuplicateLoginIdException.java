package com.hn.ticketing.auth.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class DuplicateLoginIdException extends BaseException {
    public DuplicateLoginIdException() {
        super(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "이미 존재하는 아이디입니다");
    }
}