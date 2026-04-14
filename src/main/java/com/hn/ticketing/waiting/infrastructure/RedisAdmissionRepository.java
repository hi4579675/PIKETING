package com.hn.ticketing.waiting.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

/**
 * 진입 토큰 관련 Redis 조작.
 *
 * admission:token / admission:user 키 조작 담당.
 * 토큰 발급·검증·삭제만 처리. 로직은 WaitingService / AdmissionWorker에 있음.
 */
@Repository
@RequiredArgsConstructor
public class RedisAdmissionRepository {

    private final StringRedisTemplate redisTemplate;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);


    // ── 키 생성 헬퍼 ──
    private String tokenKey(String token)                   { return "admission:token:" + token; }
    private String userKey(Long gameId, Long userId)        { return "admission:user:" + gameId + ":" + userId; }

    /**
     * 토큰 발급.
     * 이미 토큰이 있으면 기존 토큰 반환 (멱등 — Worker 중복 실행 방어).
     * 없으면 UUID 생성 후 두 키 모두 저장.
     */
    public String issueToken(Long gameId, Long userId) {
        String existing = getUserToken(gameId, userId);
        if (existing != null) {
            return existing;
        }
        String token = UUID.randomUUID().toString();
        // "gameId:userId" 형태로 저장 - 트큰만으로 주인을 식별하기 위해
        redisTemplate.opsForValue().set(tokenKey(token), gameId + ":" + userId, TOKEN_TTL);
        // 역방향 매핑. 같은 사용자에게 토큰을 중복 방지하지 않기 위해
        redisTemplate.opsForValue().set(userKey(gameId, userId), token, TOKEN_TTL);
        return token;
    }

    /**
     * 토큰 검증 — "gameId:userId" 반환, 없으면 null.
     * seat 모듈에서 X-Admission-Token 헤더 검증 시 호출.
     */
    public String validateToken(String token) {
        return redisTemplate.opsForValue().get(tokenKey(token));
    }

    /**
     * 사용자의 현재 토큰 조회 — 없으면 null.
     * WaitingService.getStatus()에서 ADMITTED 상태일 때 토큰 반환 시 사용.
     */
    public String getUserToken(Long gameId, Long userId) {
        return redisTemplate.opsForValue().get(userKey(gameId, userId));
    }

    /**
     * 토큰 소비 (1회성).
     * 좌석 점유 성공 후 seat 모듈이 호출 — 같은 토큰으로 다른 좌석 점유 불가.
     */
    public void deleteToken(String token, Long gameId, Long userId) {
        redisTemplate.delete(tokenKey(token));
        redisTemplate.delete(userKey(gameId, userId));
    }
}
