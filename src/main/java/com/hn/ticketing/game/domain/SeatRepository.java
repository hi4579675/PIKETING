package com.hn.ticketing.game.domain;

import java.util.List;

// ── 좌석 리포지토리 인터페이스 ──
// 좌석 저장 + 구역별 좌석 조회.

public interface SeatRepository {

    Seat save(Seat seat);

    // 여러 좌석을 한 번에 저장.
    // 관리자가 구역에 좌석을 일괄 등록할 때 사용.
    // 좌석 50개를 하나씩 save()하면 INSERT 50번.
    // saveAll()이면 JPA가 배치 INSERT로 최적화할 수 있다.
    List<Seat> saveAll(List<Seat> seats);

    // 특정 구역의 모든 좌석 조회.
    // 경기 등록 시 "이 구장의 모든 좌석"을 가져와서 GameSeat을 생성해야 하므로.
    List<Seat> findBySectionId(Long sectionId);
}
