package com.hn.ticketing.game.domain;

import com.hn.ticketing.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// ── 구역 엔티티 ──
// 구장 안의 구역. 예: "1루 내야 A구역", "3루 외야"
// 좌석 등급(SeatGrade)의 단위. 같은 구역 = 같은 등급 = 같은 기본 가격.

//
// 왜 Seat과 분리하나:
//   개별 좌석(Seat)은 물리적 위치(열, 번호)만 가지고,
//   등급·가격 같은 정책 정보는 Section이 가짐.
//   좌석 1만 개에 각각 등급을 넣으면 데이터 중복이고
//   정책 변경 시 1만 행 UPDATE.
//
// 관계: Stadium (1) ←── (N) Section (1) ←── (N) Seat

@Entity
@Table(name = "section")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Section extends BaseEntity {

    // 이 구역이 속한 구장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    // 필드
    // 구역 이름. 예: "1루 내야 A구역"
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 좌석 등급
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_grade", nullable = false, length = 20)
    private SeatGrade seatGrade;

    // 이 구역의 총 좌석 수
    // 비정규화 필드. Seat 테이블 COUNT로도 구할 수 있지만
    // 조회 때마다 COUNT 쿼리를 날리는 건 비효율.
    // 경기 목록에서 "잔여석 수" 요약할 때 빠르게 쓸 수 있다.
    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    // 이 구역에 속한 좌석 목록
    @OneToMany(mappedBy = "section")
    private List<Seat> seats = new ArrayList<>();

    // ── 생성자 ──

    @Builder
    private Section(Stadium stadium, String name, SeatGrade seatGrade, int totalSeats) {
        this.stadium = stadium;
        this.name = name;
        this.seatGrade = seatGrade;
        this.totalSeats = totalSeats;
    }
}
