package com.hn.ticketing.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


/**
 * 모든 커스텀 예외의 부모 클래스.
 *
 * 필드:
 *   - status: HTTP 응답 코드 (Spring의 HttpStatus 사용 → 매직 넘버 방지)
 *   - errorCode: 클라이언트가 프로그래밍적으로 식별할 수 있는 코드 ("DUPLICATE_LOGIN_ID" 등)
 *                같은 status여도 에러 원인을 구분할 수 있게 함.
 *   - message: 사람이 읽는 메시지 (super(message)로 RuntimeException에 전달)
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected BaseException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
