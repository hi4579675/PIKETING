package com.hn.ticketing.game.domain.exception;

import com.hn.ticketing.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class GameNotFoundException extends BaseException {
    public GameNotFoundException() {
        super(HttpStatus.NOT_FOUND, "GAME_NOT_FOUND", "경기를 찾을 수 없습니다");
    }
}