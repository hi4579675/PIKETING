package com.hn.ticketing.game.domain;

// ── 경기별 좌석 상태 enum ──
// GameSeat 엔티티에서 사용.
//
// 주의: 실시간 점유 판단은 Redis 키 존재 여부로 한다 (SEAT_HOLD_DESIGN 참조).
// 이 enum은 MySQL에 "최종 확정 상태"를 영속적으로 기록하는 역할.
//
// 상태 전이:
//   AVAILABLE → HELD → CONFIRMED (결제 성공)
//                   → AVAILABLE  (TTL 만료 / 자진 해제)
//   CONFIRMED → CANCELLED (예매 취소)
//   CANCELLED → AVAILABLE (취소 후 재오픈)

public enum GameSeatStatus {

    AVAILABLE,   // 예매 가능
    HELD,        // 임시 점유 중 (Redis TTL 만료 시 AVAILABLE로 복귀)
    CONFIRMED,   // 결제 완료, 영구 확정
    CANCELLED;   // 예매 취소됨
}
