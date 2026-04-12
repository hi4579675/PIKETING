package com.hn.ticketing.game.domain;

import com.hn.ticketing.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ── 물리 좌석 엔티티 ──
// 구장에 물리적으로 존재하는 좌석 하나하나.
// 경기와 무관하게 항상 존재한다. (의자는 안 바뀌니까)
//
// 기획안 7.2 — Seat과 GameSeat을 분리한 이유:
//   "물리 좌석은 바뀌지 않지만 경기마다 가격·상태가 다르다.
//    합쳐두면 한 좌석이 여러 경기에 동시 존재하는 걸 모델링할 수 없다."
//   Seat = 물리적 실체 (불변), GameSeat = 경기별 인스턴스 (가변).
//
// Seat은 한 번 INSERT 후 거의 변경되지 않는 불변 데이터.
@Entity
@Getter
@Table(name = "seat", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_section_row_number",
                columnNames = {"section_id", "seat_row", "seat_number"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    // ── 연관관계 ──

    // 이 좌석이 속한 구역.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    // ── 필드 ──

    // 열 식별자. 예: "A", "B", "AA"
    // String인 이유: 일부 구장에서 AA, BB 같은 문자열 열 번호를 쓴다.
    @Column(name = "seat_row", nullable = false, length = 10)
    private String seatRow;

    // 좌석 번호. 예: 1, 2, 3 ...
    // 같은 열 안에서의 순번. 이건 항상 숫자라 int.
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    // ── 생성자 ──

    @Builder
    private Seat(Section section, String seatRow, int seatNumber) {
        this.section = section;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
    }
}