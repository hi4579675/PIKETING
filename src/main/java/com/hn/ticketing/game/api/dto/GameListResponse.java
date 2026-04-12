package com.hn.ticketing.game.api.dto;

import com.hn.ticketing.game.domain.Game;

import java.time.LocalDate;
import java.time.LocalTime;

// ── 경기 목록 응답 DTO ──
public record GameListResponse(
        Long gameId,
        String title,
        LocalDate gameDate,
        LocalTime startTime,
        String homeTeam,
        String awayTeam
) {
    public static GameListResponse from(Game game) {
        return new GameListResponse(
                game.getId(),
                game.getTitle(),
                game.getGameDate(),
                game.getStartTime(),
                game.getHomeTeam(),
                game.getAwayTeam()
        );
    }
}