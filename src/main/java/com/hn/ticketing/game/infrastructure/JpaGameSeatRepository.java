package com.hn.ticketing.game.infrastructure;

import com.hn.ticketing.game.domain.GameSeat;
import com.hn.ticketing.game.domain.GameSeatRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaGameSeatRepository extends GameSeatRepository, JpaRepository<GameSeat, Long> {

    // 좌석 2만 개 중 해당 경기 좌석만 빠르게 조회.
    @Override
    List<GameSeat> findByGameId(Long gameId);

    @Override
    Optional<GameSeat> findByGameIdAndSeatId(Long gameId, Long seatId);
}
