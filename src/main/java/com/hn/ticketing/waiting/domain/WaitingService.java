package com.hn.ticketing.waiting.domain;

// 대기열 진입·상태 조회·이탈 비즈니스 로직.

import com.hn.ticketing.waiting.api.dto.WaitingEnterResponse;
import com.hn.ticketing.waiting.api.dto.WaitingStatusResponse;
import com.hn.ticketing.waiting.domain.exception.QueueNotEnteredException;
import com.hn.ticketing.waiting.infrastructure.RedisAdmissionRepository;
import com.hn.ticketing.waiting.infrastructure.RedisWaitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WaitingService {

    private final RedisWaitingRepository waitingRepository;
    private final RedisAdmissionRepository admissionRepository;

    /**
     * 대기열 진입.
     * ZADD NX 덕분에 중복 진입 시 Redis 레벨에서 막히고 false 반환 → 예외.
     */
    public WaitingEnterResponse enterQueue(Long gameId, Long memberId) {
        boolean entered = waitingRepository.enterQueue(gameId, memberId);
        if (!entered) {
            throw new ArithmeticException();
        }
        Long rank = waitingRepository.getRank(gameId, memberId);
        long total = waitingRepository.getQueueSize(gameId);
        return new WaitingEnterResponse(rank != null ? rank : 0L, total);
    }

    /**
     * 순번 조회.
     * 클라이언트가 2초 간격으로 폴링. ADMITTED면 토큰 반환 → 좌석 선택 페이지 이동.
     */
    public WaitingStatusResponse getStatus(Long gameId, Long memberId) {
        Map<Object, Object> meta = waitingRepository.getUserStatus(gameId, memberId);
        if (meta.isEmpty()) {
            throw new QueueNotEnteredException();
        }
        String status = (String) meta.get("status");

        if ("ADMITTED".equals(status)) {
            String token = admissionRepository.getUserToken(gameId, memberId);
            return WaitingStatusResponse.admitted(token);
        }
        // WAITING 상태 — 남은 앞 순번 계산
        Long rank = waitingRepository.getRank(gameId, memberId);
        long cursor = waitingRepository.getCursor(gameId);
        // ahead = 내 rank에서 cursor(이미 처리된 수)를 빼면 나보다 앞에 남은 인원
        long ahead = rank != null ? Math.max(0L, rank - cursor) : 0L;
        return WaitingStatusResponse.waiting(rank != null ? rank : 0L, ahead);
    }
    /**
     * 대기열 이탈.
     * 발급된 토큰이 있으면 함께 삭제.
     */
    public void leaveQueue(Long gameId, Long memberId) {
        String token = admissionRepository.getUserToken(gameId, memberId);
        if (token != null) {
            admissionRepository.deleteToken(token, gameId, memberId);
        }
        waitingRepository.removeFromQueue(gameId, memberId);
    }
}
