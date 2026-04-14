package com.hn.ticketing.waiting.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisWaitingRepository {

    private final StringRedisTemplate redisTemplate;

    // ── 키 생성 헬퍼 ──
    // Sorted set
    private String queueKey(Long gameId)                    { return "waiting:queue:" + gameId; }
    // Hash
    private String userKey(Long gameId, Long userId)        { return "waiting:user:" + gameId + ":" + userId; }
    // String
    private String cursorKey(Long gameId)                   { return "waiting:cursor:" + gameId; }

    /**
     * ZADD NX — 이미 존재하면 score 덮어쓰지 않음 (새로고침 순번 보호 핵심).
     * 성공(신규 진입)이면 Hash 메타데이터도 저장.
     * @return true = 신규 진입, false = 이미 존재
     */
    public boolean enterQueue(Long gameId, Long userId) {
        double score = System.currentTimeMillis(); // 진입 시각을 score로 사용
        Boolean added = redisTemplate.opsForZSet()
                .addIfAbsent(queueKey(gameId), String.valueOf(userId), score);
        if (Boolean.TRUE.equals(added)) {
            redisTemplate.opsForHash()
                    .putAll(userKey(gameId, userId), Map.of(
                            "enteredAt", String.valueOf(System.currentTimeMillis()),
                            "status", "WAITING"));
            return true;
        }
        return false;
    }

    /**
     * ZRANK — 0-based rank 반환.(23번째)
     * 없으면 null.
     */
    public Long getRank(Long gameId, Long userId) {
        return redisTemplate.opsForZSet()
                .rank(queueKey(gameId), String.valueOf(userId));
    }

    /**
     * ZCARD — 전체 대기자 수.
     */
    public long getQueueSize(Long gameId) {
        Long size = redisTemplate.opsForZSet().size(queueKey(gameId));
        return size != null ? size : 0L;
    }

    /**
     * ZRANGE start~end (index 기반) — Worker 배치 조회용.
     */
    public Set<String> getUsersInRange(Long gameId, long start, long end) {
        Set<String> result = redisTemplate.opsForZSet()
                .range(queueKey(gameId), start, end);
        return result != null ? result : Set.of();
    }

    /**
     * GET waiting:cursor:{gameId} — 없으면 0.
     */
    public long getCursor(Long gameId) {
        String value = redisTemplate.opsForValue().get(cursorKey(gameId));
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * INCRBY — Worker가 N명 처리 후 커서 전진.
     */
    public void incrementCursor(Long gameId, long count) {
        redisTemplate.opsForValue().increment(cursorKey(gameId), count);
    }

    /**
     * HSET — 입장 허가 후 상태를 ADMITTED로 업데이트.
     */
    public void updateUserStatus(Long gameId, Long userId, String status) {
        redisTemplate.opsForHash().putAll(userKey(gameId, userId), Map.of(
                "status", status,
                "admitted_at", String.valueOf(System.currentTimeMillis()))) ;

    }

    /**
     * HGETALL — status / entered_at / admitted_at 조회.
     */
    public Map<Object, Object> getUserStatus(Long gameId, Long userId) {
        return redisTemplate.opsForHash().entries(userKey(gameId, userId));
    }

    /**
     * ZREM + DEL — 대기열 이탈 시 큐와 메타데이터 모두 제거.
     */
    public void removeFromQueue(Long gameId, Long userId) {
        redisTemplate.opsForZSet().remove(queueKey(gameId), String.valueOf(userId));
        redisTemplate.delete(userKey(gameId, userId));
    }
}
