package com.hn.ticketing.game.domain;

// ── 좌석 등급 enum ──
// 가격 정책의 기준 단위. Section에 귀속된다.
// 같은 구역의 좌석은 모두 같은 등급.
//
// 의도적으로 3개로 단순화했다.
// 이 프로젝트의 핵심은 좌석 등급 세분화가 아니라 동시성/정합성이므로
// 등급이 많으면 시드 데이터와 테스트 복잡도만 올라간다.

public enum SeatGrade {

    PREMIUM,    // 테이블석, VIP 등 프리미엄 구역
    STANDARD,   // 1루/3루 내야 지정석
    OUTFIELD;   // 외야 자유석
}
