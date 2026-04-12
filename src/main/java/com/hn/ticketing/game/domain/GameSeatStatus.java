package com.hn.ticketing.game.domain;

// ── 경기별 좌석의 "판매 가능 여부" enum ──
//
// 주의: 이건 좌석의 "점유 상태"(AVAILABLE/HELD/CONFIRMED)와 다르다.
// 점유 상태는 Redis에만 존재하고 DB에는 없다.
//
// GameSeatStatus는 "이 좌석을 애초에 판매 대상으로 열었는가"를 나타낸다.
// 예: 시야 불량석, 방송 카메라석 → UNAVAILABLE로 잠금.
//     이미 시즌권으로 팔린 좌석 → SOLD_OUT.
//
// 관리자가 경기 등록 시 설정하는 값이다. 실시간으로 바뀌지 않는다.
public enum GameSeatStatus {
    AVAILABLE,      // 판매 가능 — 일반 사용자가 점유 시도 가능
    UNAVAILABLE,    // 판매 불가 — 시야 불량, 카메라석 등
    SOLD_OUT;       // 매진 — 시즌권 등으로 이미 배정됨
}
