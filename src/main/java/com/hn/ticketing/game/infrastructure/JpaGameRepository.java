package com.hn.ticketing.game.infrastructure;

import com.hn.ticketing.game.domain.Game;
import com.hn.ticketing.game.domain.GameRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaGameRepository extends GameRepository, JpaRepository<Game, Long> {

    // findByGameDate → WHERE game_date = ?
    // Game 엔티티에서 gameDate를 LocalDate로 분리했기 때문에
    // 날짜 비교가 단순 등호(=)로 가능하다.
    // LocalDateTime이었으면 BETWEEN이나 DATE() 함수를 써야 했을 것.
    @Override
    List<Game> findByGameDate(LocalDate gameDate);
}
