package com.hn.ticketing.game.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class SectionNotFoundException extends BaseException {
    public SectionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "SECTION_NOT_FOUND", "구역을 찾을 수 없습니다");
    }
}