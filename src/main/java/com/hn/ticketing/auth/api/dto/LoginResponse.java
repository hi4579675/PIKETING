package com.hn.ticketing.auth.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,   // "Bearer"
        long expiresIn      // 초 단위 TTL
) {
    public static LoginResponse of(String token, long expiresInSeconds) {
        return new LoginResponse(token, "Bearer", expiresInSeconds);
    }
}