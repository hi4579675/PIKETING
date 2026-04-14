package com.hn.ticketing.seat.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**

 좌석 점유 Redis 조작.
 좌석 점유 설계서의 핵심:
 "상태가 어디에 기록되어 있는지로 상태를 판단한다"
 Redis 키 존재 = HELD, MySQL CONFIRMED 행 존재 = CONFIRMED, 둘 다 없음 = AVAILABLE
 */
@Repository
@RequiredArgsConstructor
public class RedisSeatHoldRepository {
    private final StringRedisTemplate redisTemplate;

    private static final long HOLD_TTL_SECONDS = 420;
    private static final int MAX_HOLDS_PER_USER = 4;

    // 키 생성 헬퍼
    private String holdKey(Long gameId, Long seatId) {
        return "seat:hold:" + gameId + ":" + seatId;
    }
    private String userHoldsKey(Long gameId, Long seatId) {
        return "seat:holds:user:" + gameId + ":" + seatId;
    }

    // ── holdSeat (Lua 스크립트) ──
    // 왜 Lua 스크립트인가
    // 좌석 점유 설계시 : "SETNX" 하나로는 부족, 좌서이 비었나? 만 원자적으로 체크
    // 실제 좌석은 "비었나?" + 좌석 수 4개 제한 + 둘 다 충족해야 점유 성공(사용자 목록 추가)
    // 이 3단계가 원자적이여야 100명이 동시에 같은 좌석을 요청해도 정확히 1명만 성공한다.
    private static final String HOLD_SEAT_SCRIPTS = """
            local holdKey = KEYS[1]
            local userKey = KEYS[2]
            local userId = ARGV[1]
            local expiresAt = ARGV[2]
            local ttlSeconds = tonumber(ARGV[3])
            local maxHolds = tonumber(ARGV[4])
            local seatId = ARGV[5]
            
            -- 1. 이미 점유된 좌석인가?
            if redis.call('EXISTS',holdKey) == 1 then
                return 0
            end
            
            -- 2. 사용자 점유 수 초과?
            if redis.call('SCARD', userKey) >= maxHolds then
                return 0
            end
            
            -- 3. 점유 실행
            redis.call('HSET', holdKey, 'userId', userId, 'expiresAt', expiresAt)
            redis.call('EXPIRE', holdKey, ttlSeconds)
            redis.call('SADD', userKey, ARGV[5])
            redis.call('EXPIRE', userKey, ttlSeconds)
            return 1
            """;

    public boolean holdSeat(Long gameId, Long seatId, Long userId) {
        long expiresAt = Instant.now().plusSeconds(HOLD_TTL_SECONDS).toEpochMilli();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(HOLD_SEAT_SCRIPTS, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(holdKey(gameId, seatId), userHoldsKey(gameId, userId)),
                String.valueOf(userId),
                String.valueOf(expiresAt),
                String.valueOf(HOLD_TTL_SECONDS),
                String.valueOf(MAX_HOLDS_PER_USER),
                String.valueOf(seatId)
        );
        return result != null && result == 1L;
    }

    public static final String RELEASE_SEAT_SCRIPT = """
            local holdKey = KEYS[1]
            local userKey = KEYS[2]
            local userId = ARGV[1]
            local seatId = ARGV[2]
            
            local owner = redis.call('HGET', holdKey, 'userId');
            if owner == userId then
                redis.call('DEL', holdKey)
                redis.call('SREM', userKey, seatId)
                return 1
            else
                return 0
            end
            
        """;

    public boolean releaseSeat(Long gameId, Long seatId, Long userId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SEAT_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(holdKey(gameId, seatId), userHoldsKey(gameId, userId)),
                String.valueOf(userId),
                String.valueOf(seatId)
        );
        return result != null && result == 1L;
    }

    // ── getHoldInfo ──
    // HGETALL seat:hold:{gameId}:{seatId}
    // null이면 AVAILABLE 상태.
    public Map<Object, Object> getHoldInfo(Long gameId, Long seatId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(holdKey(gameId, seatId));
        return entries.isEmpty() ? null : entries;
    }


    // ── getUserHolds ──
    // SMEMBERS seat:holds:user:{gameId}:{userId}
    public Set<String> getUserHolds(Long gameId, Long userId) {
        return redisTemplate.opsForSet().members(userHoldsKey(gameId, userId));
    }


    // ── extendHoldTtl ──
    public void extendHoldTtl(Long gameId, Long seatId, long additionalMinutes) {
        redisTemplate.expire(holdKey(gameId, seatId), Duration.ofMinutes(additionalMinutes));
    }
}
