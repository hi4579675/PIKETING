package com.hn.ticketing.game.domain;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ── 경기 리포지토리 인터페이스 ──
public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(Long id);

    // 특정 날짜의 경기 목록
    // API : GET /api/games?date=2026-04-15
    List<Game> findByGameDate(LocalDate gameDate);

    // 전체 경기 목록
    // 날짜 필터 없이 조회
    List<Game> findAll();
}
