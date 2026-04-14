package com.hn.ticketing.waiting.api.dto;

public record WaitingStatusResponse(
        String status,  // "WAITING" | "ADMITTED"
        Long rank,      // WAITING일 때만 (0-based)
        Long ahead,     // WAITING일 때만 (내 앞에 남은 인원)
        String token    // ADMITTED일 때만 (진입 토큰)
) {
    public static WaitingStatusResponse waiting(long rank, long ahead) {
        return new WaitingStatusResponse("WAITING", rank, ahead, null);
    }

    public static WaitingStatusResponse admitted(String token) {
        return new WaitingStatusResponse("ADMITTED", null, null, token);
    }
}