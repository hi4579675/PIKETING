package com.hn.ticketing.waiting.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

// → 이미 대기열에 있는 사용자가 재진입 시도
// → HTTP 409 Conflict
public class AlreadyInQueueException extends BaseException {
    public AlreadyInQueueException() {
        super(HttpStatus.CONFLICT, "ALREADY_IN_QUEUE", "이미 대기열에 등록되어 있습니다");
    }
}