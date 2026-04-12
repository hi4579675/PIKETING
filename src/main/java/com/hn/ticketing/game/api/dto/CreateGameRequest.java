package com.hn.ticketing.game.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// ── 경기 등록 요청 DTO ──
// POST /api/admin/games 에서 사용.
public record CreateGameRequest(
        // stadiumId — 어떤 구장에서 열리는 경기인가.
        // 본 프로젝트는 단일 구장이지만, API는 범용적으로 설계.
        @NotNull(message = "구장 ID는 필수입니다")
        Long stadiumId,

        @NotBlank(message = "경기 제목은 필수입니다")
        String title,

        @NotNull(message = "경기 날짜는 필수입니다")
        LocalDate gameDate,

        @NotNull(message = "시작 시간은 필수입니다")
        LocalTime startTime,

        @NotBlank(message = "홈팀은 필수입니다")
        String homeTeam,

        @NotBlank(message = "어웨이팀은 필수입니다")
        String awayTeam
) {
}
