package com.hn.ticketing.game.domain;

// ── 경기별 좌석 엔티티 (핵심) ──
// Game × Seat의 교차 테이블. 경기마다 좌석별 가격과 상태를 관리.
//
// 기획안 7.2:
//   같은 "1루 내야 A열 15번" 좌석이라도
//   개막전에는 30,000원, 평일 경기에는 15,000원일 수 있다.
//   Seat에 가격을 넣으면 경기별 가격 차이를 표현할 수 없다.
//
// status 필드에 대한 중요한 설계 판단:
//   실시간 점유 상태(HELD)는 Redis에서 판단한다 (SEAT_HOLD_DESIGN 참조).
//   이 컬럼은 "최종 확정 상태"를 MySQL에 영속적으로 기록하는 역할.
//   즉, Redis = 실시간 판단용, MySQL = 영구 기록용.
//   두 저장소가 각자 다른 의미의 상태를 담는다 (기획안 §7.4).
//
// @Table 설정:
//   uniqueConstraints — 같은 경기에 같은 좌석 두 번 등록 방지.
//     좌석 점유 설계서 3.3: "MySQL 유니크 제약이 정합성 최종 방어선"
//   indexes — 경기별 좌석 목록 조회가 핵심 쿼리이므로 game_id 인덱스 필수.
//     이 인덱스 없으면 좌석 배치도 조회 시 풀 테이블 스캔.

import com.hn.ticketing.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_seats", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_game_seat",
                columnNames = {"game_id", "seat_id"}
        )
}, indexes = {
        @Index(name = "idx_game_seat_id", columnList = "game_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameSeat extends BaseEntity {
    // ── 연관관계 ──

    // 어떤 경기의 좌석인가.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    // 어떤 물리 좌석인가.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // ── 필드 ──

    // 이 경기에서 이 좌석의 가격 (원 단위).
    // int → 약 21억까지 표현 가능. 티켓 가격으로 충분.
    @Column(name = "price", nullable = false)
    private int price;

    // 좌석 상태.
    // AVAILABLE → HELD → CONFIRMED / CANCELLED
    // 관리자 초기값은 AVAILABLE.
    // 실시간 점유 판단은 Redis, 여기는 영구 기록용.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameSeatStatus status;

    // ── 생성자 ──

    @Builder
    private GameSeat(Game game, Seat seat, int price, GameSeatStatus status) {
        this.game = game;
        this.seat = seat;
        this.price = price;
        this.status = status;
    }

    // ── 정적 팩토리 메서드 ──
    // 경기 등록 시 모든 좌석을 AVAILABLE + 등급별 가격으로 일괄 생성할 때 사용.
    // Builder보다 의도가 명확하다.

    public static GameSeat createAvailable(Game game, Seat seat, int price) {
        return GameSeat.builder()
                .game(game)
                .seat(seat)
                .price(price)
                .status(GameSeatStatus.AVAILABLE)
                .build();
    }
}
