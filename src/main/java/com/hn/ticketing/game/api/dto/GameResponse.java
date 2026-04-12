package com.hn.ticketing.game.api.dto;

import com.hn.ticketing.game.domain.Game;

import java.time.LocalDate;
import java.time.LocalTime;

public record GameResponse(
        Long GameId,
        String title,
        LocalDate gameDate,
        LocalTime startTime,
        String homeTeam,
        String awayTeam,
        String stadiumName
) {
    public static GameResponse from(Game game) {
        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getGameDate(),
                game.getStartTime(),
                game.getHomeTeam(),
                game.getAwayTeam(),
                game.getStadium().getName()
        );
    }
}