package com.hn.ticketing.seat.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class SeatAlreadyHeldException extends BaseException {
    public SeatAlreadyHeldException() {
        super(HttpStatus.CONFLICT, "SEAT_ALREADY_HELD", "이미 점유된 좌석입니다");
    }
}
