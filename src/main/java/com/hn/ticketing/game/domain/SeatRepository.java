package com.hn.ticketing.game.domain;

import java.util.List;

// ── 좌석 리포지토리 인터페이스 ──
// 좌석 저장 + 구역별 좌석 조회.

public interface SeatRepository {

    Seat save(Seat seat);




    // 특정 구역의 모든 좌석 조회.
    // 경기 등록 시 "이 구장의 모든 좌석"을 가져와서 GameSeat을 생성해야 하므로.
    List<Seat> findBySectionId(Long sectionId);
}
