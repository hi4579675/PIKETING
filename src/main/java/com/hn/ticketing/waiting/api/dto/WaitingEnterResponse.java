package com.hn.ticketing.waiting.api.dto;

// rank: 0-based 순번, total: 전체 대기 인원
public record WaitingEnterResponse(
        long rank,
        long total
) {}