package com.hn.ticketing.shared.exception;

import com.hn.ticketing.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 모든 커스텀 예외를 한 곳에서 처리.
     * BaseException을 상속한 어떤 모듈의 예외든 여기로 들어옴.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        log.warn("[{}] {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.error(e.getStatus().value(), e.getErrorCode(), e.getMessage()));
    }

    /**
     * @Valid 검증 실패 시 자동으로 던져지는 예외.
     * 첫 번째 필드 에러 메시지를 반환.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, "VALIDATION_FAILED", message));
    }

    /**
     * 예상치 못한 모든 예외의 최종 방어선.
     * 스택트레이스는 로그에만 남기고, 클라이언트엔 일반 메시지만 반환 (정보 노출 방지).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(500, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다"));
    }
}