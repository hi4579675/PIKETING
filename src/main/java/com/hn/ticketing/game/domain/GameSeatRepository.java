package com.hn.ticketing.game.domain;

import java.util.List;
import java.util.Optional;

public interface GameSeatRepository {
    GameSeat save(GameSeat gameSeat);

    Optional<GameSeat> findById(Long id);

    // 특정 경기의 전체 좌석 조회
    List<GameSeat> findByGameId(Long gameId);
}
