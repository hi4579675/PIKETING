package com.hn.ticketing.waiting.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

// → 대기열에 없는 사용자가 순번 조회 시도
// → HTTP 404 Not Found
public class QueueNotEnteredException extends BaseException {
    public QueueNotEnteredException() {
        super(HttpStatus.NOT_FOUND, "QUEUE_NOT_ENTERED", "대기열에 등록되지 않은 사용자입니다");
    }
}