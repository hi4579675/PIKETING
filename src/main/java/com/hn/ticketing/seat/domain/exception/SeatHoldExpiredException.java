package com.hn.ticketing.seat.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class SeatHoldExpiredException extends BaseException {
    public SeatHoldExpiredException() {
        super(HttpStatus.NOT_FOUND, "SEAT_HOLD_EXPIRED", "좌석 점유가 만료되었습니다");
    }
}
