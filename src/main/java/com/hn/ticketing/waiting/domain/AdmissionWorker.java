package com.hn.ticketing.waiting.domain;

import com.hn.ticketing.waiting.infrastructure.RedisAdmissionRepository;
import com.hn.ticketing.waiting.infrastructure.RedisWaitingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 1초마다 대기열 상위 N명에게 진입 토큰 발급.
 *
 * ZPOPMIN 대신 ZRANGE + cursor 전진을 쓰는 이유:
 *   팝 방식은 처리 실패 시 데이터 유실. 읽기 + 커서 전진은 실패해도 재시도 가능.
 *
 * EC2 2대에서 동시 실행 방어:
 *   INCRBY는 원자적 → 두 Worker가 같은 범위를 처리할 수 없음.
 *   혹시 토큰이 중복 발급되더라도 issueToken()이 멱등이라 문제없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdmissionWorker {

    private final RedisWaitingRepository waitingRepository;
    private final RedisAdmissionRepository admissionRepository;

    private static final int BATCH_SIZE = 100;

    // TODO: 활성 gameId를 game 모듈에서 조회하는 방식으로 교체 예정
    //       지금은 단일 경기 ID로 하드코딩
    private static final Long ACTIVE_GAME_ID = 1L;

    // ── admit ──
    @Scheduled(fixedDelay = 1000) //  ← 1초마다 실행
    public void admit() {
        try {
            processGame(ACTIVE_GAME_ID);
        } catch (Exception e) {
            // Worker 예외가 스케줄러 전체를 멈추지 않도록 여기서 잡음
            log.error("[AdmissionWorker] gameId={} 처리 중 오류: {}", ACTIVE_GAME_ID, e.getMessage(), e);
        }
    }

    private void processGame(Long gameId) {
        long cursor = waitingRepository.getCursor(gameId);
        long queueSize = waitingRepository.getQueueSize(gameId);

        if (cursor >= queueSize) {
            return;
        }

        long end = Math.min(cursor + BATCH_SIZE - 1, queueSize - 1);
        Set<String> userIds = waitingRepository.getUsersInRange(gameId, cursor, end);

        if (userIds.isEmpty()) {
            return;
        }

        int processed = 0;
        for (String userIdStr : userIds) {
            Long userId = Long.parseLong(userIdStr);
            admissionRepository.issueToken(gameId, userId);
            waitingRepository.updateUserStatus(gameId, userId, "ADMITTED");
            processed++;
        }

        waitingRepository.incrementCursor(gameId, processed);
        log.info("[AdmissionWorker] gameId={} {}명 입장 허가 (cursor {} → {})",
                gameId, processed, cursor, cursor + processed);
    }
}
