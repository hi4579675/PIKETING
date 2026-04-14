package com.hn.ticketing.seat.api.dto;

public record SeatHoldResponse(
        Long seatId,
        long expiresAt
) {
}
