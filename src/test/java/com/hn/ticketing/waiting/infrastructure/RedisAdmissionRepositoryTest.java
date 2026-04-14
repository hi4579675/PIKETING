package com.hn.ticketing.waiting.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * RedisAdmissionRepository 단위 테스트.
 *
 * ─────────────────────────────────────────────
 * [이 클래스가 하는 일]
 *
 * 진입 토큰(Admission Token)의 발급·검증·삭제를 담당하는 Repository.
 * 설계문서의 admission:token:{token}, admission:user:{gameId}:{userId} 키를 관리한다.
 *
 * [StringRedisTemplate Mock 구조]
 *
 * StringRedisTemplate은 직접 값을 저장하지 않고,
 * opsForValue(), opsForHash() 같은 Operations 객체를 통해 작업한다.
 *
 *   redisTemplate.opsForValue().set(key, value)
 *   ─────────┬───── ────┬───── ───┬──────────
 *       Mock 1       Mock 2    실제 호출
 *
 * 그래서 Mock이 2개 필요하다:
 *   1. @Mock StringRedisTemplate — opsForValue()가 호출되면 valueOps를 반환
 *   2. @Mock ValueOperations    — get(), set() 등 실제 동작을 스텁
 *
 * ■ 스텁(Stub)이란?
 *   Mock 객체에 "이 메서드가 호출되면 이 값을 돌려줘"라고 설정하는 것.
 *   given(mock.method()).willReturn(value) 가 스텁을 설정하는 코드.
 *
 * ■ eq(), contains(), any() — Argument Matcher
 *   verify()나 given()에서 파라미터를 유연하게 매칭할 때 사용.
 *   - eq("abc")    : 정확히 "abc"와 일치할 때
 *   - contains("x"): "x"를 포함하는 문자열일 때
 *   - any()        : 아무 값이나 매칭
 *   - anyString()  : 아무 String이나 매칭
 *
 *   주의: 하나라도 Matcher를 쓰면, 같은 메서드의 모든 인자에 Matcher를 써야 한다.
 *   ❌ verify(mock).method(eq("a"), "b")        — 컴파일은 되지만 런타임 에러
 *   ✅ verify(mock).method(eq("a"), eq("b"))     — 올바른 사용
 * ─────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class RedisAdmissionRepositoryTest {

    @InjectMocks
    private RedisAdmissionRepository admissionRepository;

    // Redis 연결 없이 동작하는 가짜 템플릿
    @Mock
    private StringRedisTemplate redisTemplate;

    // redisTemplate.opsForValue()가 반환할 가짜 Operations
    @Mock
    private ValueOperations<String, String> valueOps;

    private static final Long GAME_ID = 42L;
    private static final Long USER_ID = 100L;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // issueToken 테스트
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * [멱등성(Idempotency) 테스트]
     *
     * 멱등성이란?
     *   같은 요청을 여러 번 보내도 결과가 동일한 성질.
     *   예: "토큰 발급"을 3번 호출해도 토큰이 3개가 아닌 1개만 존재.
     *
     * 왜 멱등이어야 하나?
     *   설계문서 AdmissionWorker 주석:
     *   "EC2 2대에서 동시 실행 방어 — 혹시 토큰이 중복 발급되더라도
     *    issueToken()이 멱등이라 문제없음"
     *
     * 시나리오: 이미 토큰이 발급된 사용자에게 다시 issueToken 호출.
     *   → 기존 토큰을 그대로 반환하고, 새 토큰을 만들지 않는다.
     */
    @Test
    @DisplayName("기존 토큰이 있으면 기존 토큰 반환 (멱등)")
    void issueToken_existingToken_returnsIt() {
        // given — opsForValue() → valueOps를 반환하도록 연결
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // admission:user:42:100 키에 이미 토큰이 존재
        given(valueOps.get("admission:user:42:100")).willReturn("existing-token");

        // when
        String token = admissionRepository.issueToken(GAME_ID, USER_ID);

        // then — 기존 토큰을 그대로 반환
        assertThat(token).isEqualTo("existing-token");
        // 새 토큰을 저장하는 set()이 호출되지 않아야 함
        verify(valueOps, never()).set(
                contains("admission:token:"),  // "admission:token:"을 포함하는 아무 키
                anyString(),                   // 아무 값
                any(Duration.class)            // 아무 TTL
        );
    }

    /**
     * [신규 토큰 발급 - 양방향 저장 검증]
     *
     * 양방향 저장이란?
     *   같은 정보를 두 방향으로 저장하는 것.
     *   1. admission:token:{token} → "42:100"  (토큰 → 사용자 매핑)
     *   2. admission:user:42:100 → {token}     (사용자 → 토큰 매핑)
     *
     * 왜 두 개가 필요한가?
     *   - 좌석 API는 토큰으로 사용자를 찾아야 함 → 1번 사용
     *   - 대기열 상태 조회는 사용자로 토큰을 찾아야 함 → 2번 사용
     *   - 중복 발급 방지도 2번으로 체크
     *
     * 검증 포인트:
     *   - UUID 형태의 토큰이 생성됐는지
     *   - 두 키 모두 TTL 15분으로 저장됐는지
     *   - 값 매핑이 정확한지 ("42:100" 형태)
     */
    @Test
    @DisplayName("기존 토큰 없으면 새 UUID 토큰 발급 + 양방향 저장")
    void issueToken_newToken_storesBothKeys() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // 기존 토큰 없음
        given(valueOps.get("admission:user:42:100")).willReturn(null);

        // when
        String token = admissionRepository.issueToken(GAME_ID, USER_ID);

        // then — 토큰이 생성됐는지
        assertThat(token).isNotNull().isNotEmpty();

        // 정방향: admission:token:{token} → "42:100" (TTL 15분)
        verify(valueOps).set(
                eq("admission:token:" + token),  // 키: 방금 생성된 토큰
                eq("42:100"),                     // 값: gameId:userId
                eq(TOKEN_TTL)                     // TTL: 15분
        );

        // 역방향: admission:user:42:100 → {token} (TTL 15분)
        verify(valueOps).set(
                eq("admission:user:42:100"),      // 키: 사용자별 역참조
                eq(token),                        // 값: 방금 생성된 토큰
                eq(TOKEN_TTL)
        );
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // validateToken 테스트
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * [토큰 검증 - 유효한 토큰]
     *
     * 시나리오: 좌석 점유 API에서 X-Admission-Token 헤더를 검증할 때.
     *   Redis에 키가 존재하면 → "gameId:userId" 반환 → 통과.
     *
     * 이 값("42:100")으로 좌석 API는:
     *   1. JWT의 userId와 토큰의 userId 일치 여부 확인 (토큰 도용 방지)
     *   2. URL의 gameId와 토큰의 gameId 일치 여부 확인
     */
    @Test
    @DisplayName("유효한 토큰 → gameId:userId 반환")
    void validateToken_valid() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("admission:token:abc-123")).willReturn("42:100");

        String result = admissionRepository.validateToken("abc-123");

        assertThat(result).isEqualTo("42:100");
    }

    /**
     * [토큰 검증 - 만료된 토큰]
     *
     * 시나리오: 15분 TTL이 지나 Redis가 자동 삭제한 토큰.
     *   get()이 null을 반환 → 좌석 API에서 401 Unauthorized.
     *
     * 설계문서 §4.5:
     *   "TTL의 가장 큰 장점 — 만료 처리를 코드로 안 짜도 된다."
     *   Redis가 알아서 삭제하므로 별도 정리 작업(cleanup job)이 불필요.
     */
    @Test
    @DisplayName("만료된 토큰 → null 반환")
    void validateToken_expired() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("admission:token:expired")).willReturn(null);

        String result = admissionRepository.validateToken("expired");

        assertThat(result).isNull();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // deleteToken 테스트
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * [토큰 삭제 - 양방향 키 모두 삭제]
     *
     * 시나리오: 사용자가 대기열을 이탈하거나, 좌석 점유 후 토큰 소비.
     *   정방향(admission:token:{token})과 역방향(admission:user:{gameId}:{userId})
     *   모두 삭제해야 정합성이 유지된다.
     *
     * 만약 하나만 삭제하면?
     *   - 정방향만 삭제: getUserToken()으로 조회하면 토큰이 보이지만, 그 토큰은 무효
     *   - 역방향만 삭제: validateToken()은 통과하지만, 새 토큰 발급 시 중복 체크 실패
     *   → 둘 다 버그. 양쪽 모두 삭제해야 한다.
     */
    @Test
    @DisplayName("토큰 삭제 → 양방향 키 모두 삭제")
    void deleteToken_removesBothKeys() {
        // when
        admissionRepository.deleteToken("abc-123", GAME_ID, USER_ID);

        // then — 두 키 모두 삭제 확인
        verify(redisTemplate).delete("admission:token:abc-123");
        verify(redisTemplate).delete("admission:user:42:100");
    }
}
