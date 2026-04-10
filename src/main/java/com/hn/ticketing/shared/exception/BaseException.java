package com.hn.ticketing.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;

    protected BaseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
