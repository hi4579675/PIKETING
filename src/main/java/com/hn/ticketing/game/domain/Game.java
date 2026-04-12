package com.hn.ticketing.game.domain;

import com.hn.ticketing.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

// ── 경기 엔티티 ──
// KBO 경기 한 건. 관리자가 등록한다.
// 사용자는 경기 목록을 조회하고, 특정 경기의 좌석을 예매한다.

// 왜 homeTeam/awayTeam을 String으로 두나:
//   Team 엔티티를 따로 만들면 도메인 복잡도만 올라가고
//   이 프로젝트의 핵심(동시성/정합성)과는 무관.
//   기획안 3.5: "다구단 통합은 비목표"
//
// 왜 gameDate와 startTime을 분리하나:
//   LocalDate + LocalTime으로 나누면
//   "날짜별 경기 목록 조회" 쿼리에서 gameDate만으로 WHERE 가능.
//   LocalDateTime이면 시간 부분을 잘라내는 변환이 필요해서 인덱스 활용이 어려움.

@Entity
@Table(name = "game")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game extends BaseEntity {
    // ── 연관관계 ──

    // 경기가 열리는 구장.
    // 본 프로젝트는 단일 구장이지만 FK로 연결해둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    // ── 필드 ──

    // 경기 제목. 예: "2026 KBO 한국시리즈 1차전"
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 경기 날짜. 예: 2026-04-15
    // LocalDate — 날짜만. 시간 정보 없음.
    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    // 경기 시작 시각. 예: 18:30
    // LocalTime — 시각만. 날짜 정보 없음.
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // 홈팀 이름. 예: "한화 이글스"
    // Team 엔티티 대신 단순 문자열.
    @Column(name = "home_team", nullable = false, length = 30)
    private String homeTeam;

    // 어웨이팀 이름. 예: "삼성 라이온즈"
    @Column(name = "away_team", nullable = false, length = 30)
    private String awayTeam;

    // ── 생성자 ──

    @Builder
    private Game(Stadium stadium, String title, LocalDate gameDate,
                 LocalTime startTime, String homeTeam, String awayTeam) {
        this.stadium = stadium;
        this.title = title;
        this.gameDate = gameDate;
        this.startTime = startTime;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }
}
