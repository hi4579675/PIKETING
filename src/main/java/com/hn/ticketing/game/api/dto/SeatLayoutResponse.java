package com.hn.ticketing.game.api.dto;

import com.hn.ticketing.game.domain.GameSeat;
import com.hn.ticketing.game.domain.Seat;
import com.hn.ticketing.game.domain.Section;

// ── 좌석 배치도 응답 DTO ──
// GET /api/games/{gameId}/seats 에서 사용.
//
// 한 좌석의 정보를 평탄화(flatten)해서 내려준다.
// 프론트엔드(Postman)에서 좌석 하나의 정보를 한 눈에 볼 수 있도록:
//   구역명 + 등급 + 열 + 번호 + 가격 + 상태
//
// 엔티티 구조: GameSeat → Seat → Section 으로 타고 들어가야 하는데
// 응답에서는 이 계층을 풀어서 하나의 평면 객체로 만든다.
public record SeatLayoutResponse(
        Long gameSeatId,
        String sectionName,
        String seatGrade,
        String seatRow,
        int seatNumber,
        int price,
        String status
) {
    public static SeatLayoutResponse from(GameSeat gameSeat) {
        Seat seat = gameSeat.getSeat();
        Section section = seat.getSection();

        return new SeatLayoutResponse(
                gameSeat.getId(),
                section.getName(),
                section.getSeatGrade().name(),
                seat.getSeatRow(),
                seat.getSeatNumber(),
                gameSeat.getPrice(),
                gameSeat.getStatus().name()
        );
    }
}
