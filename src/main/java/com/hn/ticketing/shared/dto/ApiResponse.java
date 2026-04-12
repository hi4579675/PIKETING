package com.hn.ticketing.shared.dto;

public record ApiResponse<T>(
        int status,
        String errorCode,   // 실패 시만 채워짐, 성공 시 null
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, null, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, null, "OK", data);
    }

    public static ApiResponse<Void> error(int status, String errorCode, String message) {
        return new ApiResponse<>(status, errorCode, message, null);
    }
}
