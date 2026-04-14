package com.hn.ticketing.waiting.domain;

import com.hn.ticketing.waiting.api.dto.WaitingEnterResponse;
import com.hn.ticketing.waiting.api.dto.WaitingStatusResponse;
import com.hn.ticketing.waiting.domain.exception.AlreadyInQueueException;
import com.hn.ticketing.waiting.domain.exception.QueueNotEnteredException;
import com.hn.ticketing.waiting.infrastructure.RedisAdmissionRepository;
import com.hn.ticketing.waiting.infrastructure.RedisWaitingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class WaitingServiceTest {

    // 테스트 대상. @Mock으로 만든 가짜 의존성이 자동 주입된다.
    @InjectMocks
    private WaitingService waitingService;

    // 실제 Redis에 연결하지 않는 가짜 repository
    @Mock
    private RedisWaitingRepository waitingRepository;

    @Mock
    private RedisAdmissionRepository admissionRepository;

    // 테스트 전체에서 반복 사용하는 상수. 매직 넘버를 없앤다.
    private static final Long GAME_ID = 42L;
    private static final Long MEMBER_ID = 100L;

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // enterQueue 테스트 그룹
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Nested
    @DisplayName("enterQueue")
    class EnterQueue {

        /**
         * [정상 경로 테스트 - Happy Path]
         *
         * 시나리오: 사용자가 처음 대기열에 진입한다.
         *
         * given (준비):
         *   - enterQueue()가 true를 반환한다 (ZADD NX 성공 = 신규 진입)
         *   - getRank()가 4를 반환한다 (0-based, 즉 5번째)
         *   - getQueueSize()가 500을 반환한다 (현재 대기자 500명)
         *
         * when (실행):
         *   - waitingService.enterQueue(42, 100) 호출
         *
         * then (검증):
         *   - 응답의 rank가 4인지 확인
         *   - 응답의 total이 500인지 확인
         */
        @Test
        @DisplayName("신규 진입 → rank와 total 반환")
        void enterQueue_success() {
            // given — 이런 상황을 가정한다
            given(waitingRepository.enterQueue(GAME_ID, MEMBER_ID)).willReturn(true);
            given(waitingRepository.getRank(GAME_ID, MEMBER_ID)).willReturn(4L);
            given(waitingRepository.getQueueSize(GAME_ID)).willReturn(500L);

            // when — 테스트 대상 메서드를 실행한다
            WaitingEnterResponse response = waitingService.enterQueue(GAME_ID, MEMBER_ID);

            // then — 결과를 검증한다
            assertThat(response.rank()).isEqualTo(4L);
            assertThat(response.total()).isEqualTo(500L);
        }

        /**
         * [예외 경로 테스트 - 중복 진입]
         *
         * 시나리오: 이미 대기열에 있는 사용자가 다시 진입을 시도한다.
         *   (새로고침으로 순번 앞당기기 시도 — 설계문서 시나리오 1)
         *
         * ZADD NX가 false를 반환 → 이미 존재하는 멤버
         * → 서비스가 예외를 던져야 한다.
         *
         */
        @Test
        @DisplayName("중복 진입 시 예외 발생")
        void enterQueue_duplicate_throws() {
            // given — ZADD NX 실패 (이미 존재)
            given(waitingRepository.enterQueue(GAME_ID, MEMBER_ID)).willReturn(false);

            // when & then — 실행하면 예외가 터져야 한다
            assertThatThrownBy(() -> waitingService.enterQueue(GAME_ID, MEMBER_ID))
                    .isInstanceOf(AlreadyInQueueException.class);
        }

        /**
         * [엣지 케이스 테스트 - null 방어]
         *
         * 엣지 케이스(Edge Case)란?
         *   정상도 아니고 에러도 아닌, "경계"에 있는 상황.
         *   null, 0, 빈 컬렉션, 최대값 등이 대표적.
         *
         * 시나리오: ZADD는 성공했지만 ZRANK가 null을 반환하는 극히 드문 타이밍.
         *   (ZADD 직후 다른 프로세스가 ZREM을 한 경우 등)
         *   이때 NullPointerException이 터지면 안 되고, 0으로 안전하게 반환해야 한다.
         */
        @Test
        @DisplayName("rank가 null이면 0으로 반환")
        void enterQueue_nullRank_returnsZero() {
            given(waitingRepository.enterQueue(GAME_ID, MEMBER_ID)).willReturn(true);
            given(waitingRepository.getRank(GAME_ID, MEMBER_ID)).willReturn(null);
            given(waitingRepository.getQueueSize(GAME_ID)).willReturn(1L);

            WaitingEnterResponse response = waitingService.enterQueue(GAME_ID, MEMBER_ID);

            assertThat(response.rank()).isEqualTo(0L);
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // getStatus 테스트 그룹
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        /**
         * [예외 경로 - 대기열에 없는 사용자]
         *
         * 시나리오: 대기열에 진입하지 않은 사용자가 상태를 조회한다.
         *   Hash에 데이터가 없으므로 빈 Map이 반환된다.
         *   → QueueNotEnteredException (HTTP 404)
         *
         * 왜 이걸 테스트하나?
         *   프론트엔드가 실수로 진입 전에 폴링을 시작하거나,
         *   URL을 직접 때리는 경우를 방어하는 로직이 제대로 작동하는지 확인.
         */
        @Test
        @DisplayName("대기열 미등록 사용자 → QueueNotEnteredException")
        void getStatus_notEntered_throws() {
            // given — Hash에 데이터 없음 (진입한 적 없음)
            given(waitingRepository.getUserStatus(GAME_ID, MEMBER_ID))
                    .willReturn(Map.of());  // Map.of() = 비어있는 불변 Map

            // when & then
            assertThatThrownBy(() -> waitingService.getStatus(GAME_ID, MEMBER_ID))
                    .isInstanceOf(QueueNotEnteredException.class);
        }

        /**
         * [정상 경로 - WAITING 상태]
         *
         * 시나리오: 사용자가 대기 중이고, 아직 호출되지 않았다.
         *   rank = 150 (0-based), cursor = 100 (이미 100명 호출됨)
         *   → ahead = 150 - 100 = 50 (내 앞에 50명 남음)
         *
         * 이 테스트가 검증하는 핵심 로직:
         *   ahead = rank - cursor (설계문서 §4.2)
         *   클라이언트가 2초마다 폴링할 때 받는 응답의 정확성.
         */
        @Test
        @DisplayName("WAITING 상태 → rank, ahead 반환")
        void getStatus_waiting() {
            // given — 대기 중 상태
            given(waitingRepository.getUserStatus(GAME_ID, MEMBER_ID))
                    .willReturn(Map.of("status", "WAITING", "enteredAt", "123456"));
            given(waitingRepository.getRank(GAME_ID, MEMBER_ID)).willReturn(150L);
            given(waitingRepository.getCursor(GAME_ID)).willReturn(100L);

            // when
            WaitingStatusResponse response = waitingService.getStatus(GAME_ID, MEMBER_ID);

            // then
            assertThat(response.status()).isEqualTo("WAITING");
            assertThat(response.rank()).isEqualTo(150L);
            assertThat(response.ahead()).isEqualTo(50L);   // 핵심: 150 - 100 = 50
            assertThat(response.token()).isNull();          // WAITING이면 토큰은 null
        }

        /**
         * [정상 경로 - ADMITTED 상태]
         *
         * 시나리오: 워커가 이 사용자를 호출해서 토큰을 발급한 상태.
         *   클라이언트는 이 응답을 받으면 좌석 선택 페이지로 이동한다.
         *
         * 검증 포인트:
         *   - status가 "ADMITTED"인지
         *   - token이 정확히 반환되는지
         *   - rank/ahead는 null인지 (ADMITTED면 순번은 의미 없음)
         */
        @Test
        @DisplayName("ADMITTED 상태 → 토큰 반환")
        void getStatus_admitted() {
            given(waitingRepository.getUserStatus(GAME_ID, MEMBER_ID))
                    .willReturn(Map.of("status", "ADMITTED", "enteredAt", "123456"));
            given(admissionRepository.getUserToken(GAME_ID, MEMBER_ID))
                    .willReturn("abc-def-123");

            WaitingStatusResponse response = waitingService.getStatus(GAME_ID, MEMBER_ID);

            assertThat(response.status()).isEqualTo("ADMITTED");
            assertThat(response.token()).isEqualTo("abc-def-123");
            assertThat(response.rank()).isNull();
        }

        /**
         * [엣지 케이스 - ahead가 음수가 되는 상황]
         *
         * 어떻게 이런 일이?
         *   cursor = 100인데 rank = 50인 경우.
         *   워커가 커서를 전진시킨 후, 중간에 사용자가 ZREM으로 빠져서
         *   rank가 내려간 타이밍 등 레이스 컨디션(Race Condition)에서 발생 가능.
         *
         *   Race Condition: 두 작업이 동시에 같은 자원을 건드려
         *   예상치 못한 결과가 나오는 상황.
         *
         * Math.max(0, rank - cursor)로 음수 방어가 되는지 확인.
         */
        @Test
        @DisplayName("WAITING 상태에서 ahead가 음수면 0으로 보정")
        void getStatus_ahead_neverNegative() {
            given(waitingRepository.getUserStatus(GAME_ID, MEMBER_ID))
                    .willReturn(Map.of("status", "WAITING", "enteredAt", "123456"));
            given(waitingRepository.getRank(GAME_ID, MEMBER_ID)).willReturn(50L);
            given(waitingRepository.getCursor(GAME_ID)).willReturn(100L);  // cursor > rank!

            WaitingStatusResponse response = waitingService.getStatus(GAME_ID, MEMBER_ID);

            assertThat(response.ahead()).isEqualTo(0L);  // 음수가 아닌 0
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // leaveQueue 테스트 그룹
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Nested
    @DisplayName("leaveQueue")
    class LeaveQueue {

        /**
         * [정상 경로 - 토큰이 있는 상태에서 이탈]
         *
         * 시나리오: 사용자가 이미 ADMITTED 되어 토큰을 가진 상태에서 이탈.
         *   → 토큰도 삭제하고, 큐에서도 제거해야 한다.
         *
         * verify()로 확인하는 이유:
         *   leaveQueue()는 반환값이 void라서 assertThat으로 결과를 검증할 수 없다.
         *   대신 "이 메서드가 호출됐는가?"를 verify()로 확인한다.
         *   → "행위 검증(Behavior Verification)"이라 부른다.
         */
        @Test
        @DisplayName("토큰 있는 사용자 이탈 → 토큰 삭제 + 큐 제거")
        void leaveQueue_withToken() {
            // given — 토큰이 존재하는 상태
            given(admissionRepository.getUserToken(GAME_ID, MEMBER_ID))
                    .willReturn("token-123");

            // when
            waitingService.leaveQueue(GAME_ID, MEMBER_ID);

            // then — 토큰 삭제와 큐 제거가 모두 호출됐는지 확인
            verify(admissionRepository).deleteToken("token-123", GAME_ID, MEMBER_ID);
            verify(waitingRepository).removeFromQueue(GAME_ID, MEMBER_ID);
        }

        /**
         * [정상 경로 - 토큰 없이 이탈]
         *
         * 시나리오: WAITING 상태(아직 호출 안 됨)에서 사용자가 자진 이탈.
         *   토큰이 없으므로 deleteToken은 호출되면 안 되고,
         *   큐 제거만 일어나야 한다.
         *
         * verify가 없는 것도 검증이다:
         *   deleteToken에 대한 verify가 없으면,
         *   Mockito가 "예상치 못한 호출"로 실패하지는 않지만,
         *   의도적으로 호출 안 됨을 명시하고 싶으면
         *   verify(mock, never()).method()를 쓸 수도 있다.
         */
        @Test
        @DisplayName("토큰 없는 사용자 이탈 → 큐 제거만")
        void leaveQueue_withoutToken() {
            // given — 토큰이 없는 상태
            given(admissionRepository.getUserToken(GAME_ID, MEMBER_ID))
                    .willReturn(null);

            // when
            waitingService.leaveQueue(GAME_ID, MEMBER_ID);

            // then — 큐 제거만 호출됐는지 확인
            verify(waitingRepository).removeFromQueue(GAME_ID, MEMBER_ID);
        }
    }
}
