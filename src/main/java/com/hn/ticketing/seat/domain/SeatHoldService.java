package com.hn.ticketing.seat.domain;


import com.hn.ticketing.game.domain.GameSeat;
import com.hn.ticketing.game.domain.GameSeatRepository;
import com.hn.ticketing.game.domain.GameSeatStatus;
import com.hn.ticketing.seat.api.dto.SeatHoldResponse;
import com.hn.ticketing.seat.domain.exception.InvalidAdmissionTokenException;
import com.hn.ticketing.seat.domain.exception.SeatAlreadyHeldException;
import com.hn.ticketing.seat.domain.exception.SeatHoldExpiredException;
import com.hn.ticketing.seat.infrastructure.RedisSeatHoldRepository;
import com.hn.ticketing.waiting.infrastructure.RedisAdmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 좌석 점유 비즈니스 로직.
 *
 * 좌석 점유 설계서의 3층 방어:
 *   1층: Redis Lua (처리량 최적화 — 여기서 99%의 요청이 걸러짐)
 *   2층: MySQL CONFIRMED 사전 검증 (Redis-MySQL 불일치 방어)
 *   3층: MySQL unique 제약 (최종 방어선 — 결제 확정 시)
 *
 * 이 서비스는 1층과 2층을 담당. 3층은 reservation 모듈에서 결제 확정 시.
 */
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final RedisSeatHoldRepository seatHoldRepository;
    private final RedisAdmissionRepository admissionRepository;
    private final GameSeatRepository gameSeatRepository;

    private static final long HOLD_TTL_SECONDS = 420;


    // ── holdSeat ──
    public SeatHoldResponse holdSet(Long gameId, Long seatId, Long memberId, String admissionToken) {
        //   1단계: 진입 토큰 검증
        //   tokenValue에서 gameId와 userId를 파싱해서
        //   요청의 gameId/memberId와 일치하는지 검증.
        //   → 다른 사람의 토큰, 다른 경기의 토큰 차단.
        String tokenValue = admissionRepository.validateToken(admissionToken);
        if (tokenValue == null) {
            throw new InvalidAdmissionTokenException();
        }

        // tokenValue = "gameId:userId" 형태. 요청의 gameId/ memberId와 일치하는지 검증
        String expected = gameId + ":" + memberId;
        if (!expected.equals(tokenValue)) {
            throw new InvalidAdmissionTokenException();
        }

        // 2단계 : MySQL에서 이미 확정된 좌석인지 체크 (2층 방어)
        //     Redis TTL 만료 후 MySQL은 CONFIRMED인데 Redis에는 키가 없는 경우.
        //     Redis만 보면 AVAILABLE로 판단하지만 실제론 확정된 좌석.
        GameSeat gameSeat = gameSeatRepository.findByGameIdAndSeatId(gameId, seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다"));
        if (gameSeat.getStatus() == GameSeatStatus.CONFIRMED) {
            throw new SeatAlreadyHeldException();
        }

        // 3단계: Redis Lua로 점유 시도 (1층 방어)
        boolean success = seatHoldRepository.holdSeat(gameId, seatId, memberId);
        if (!success) {
            throw new SeatAlreadyHeldException();
        }

        // 4단계: MySQL GameSeat 상태를 HELD로 변경
        //   gameSeat.updateStatus(GameSeatStatus.HELD)
        //   왜: Redis가 죽어도 MySQL에 HELD 기록이 남아있도록.
        //   주의: 이 UPDATE가 실패해도 Redis 점유는 이미 됨.
        //   → TTL 만료 시 자동 정리되므로 불일치는 일시적.
        gameSeat.updateStatus(GameSeatStatus.HELD);

        // 5단계: 응답반환
        long expiresAt = Instant.now().plusSeconds(HOLD_TTL_SECONDS).toEpochMilli();
        return new SeatHoldResponse(seatId, expiresAt);
    }

    public void releaseSeat(Long gameId, Long seatId, Long memberId) {
        // 1. redis 점유 해제
        boolean released = seatHoldRepository.releaseSeat(gameId, seatId, memberId);
        if (!released) {
            throw new SeatHoldExpiredException();
        }

        // 2. MySQL GameSeat 상태를 AVAILABLE
        gameSeatRepository.findByGameIdAndSeatId(gameId, seatId)
                .ifPresent(gameSeat -> gameSeat.updateStatus(GameSeatStatus.AVAILABLE));

    }
}



