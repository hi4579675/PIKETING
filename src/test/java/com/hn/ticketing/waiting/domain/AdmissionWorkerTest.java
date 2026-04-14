package com.hn.ticketing.waiting.domain;

import com.hn.ticketing.waiting.infrastructure.RedisAdmissionRepository;
import com.hn.ticketing.waiting.infrastructure.RedisWaitingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AdmissionWorker 단위 테스트.
 *
 * ─────────────────────────────────────────────
 * [AdmissionWorker란?]
 *
 * 1초마다 대기열에서 다음 N명을 꺼내 진입 토큰을 발급하는 스케줄러.
 * 설계문서 §4.3의 "워커가 호출 (입장 허가 발급)" 흐름을 담당한다.
 *
 * 동작 방식:
 *   1. cursor(마지막으로 호출한 위치)를 읽는다.
 *   2. cursor부터 BATCH_SIZE만큼의 사용자를 ZRANGE로 조회한다.
 *   3. 각 사용자에게 토큰을 발급하고 상태를 ADMITTED로 바꾼다.
 *   4. cursor를 처리한 인원만큼 전진시킨다.
 *
 * [테스트에서 주의할 점]
 *
 * ■ @Scheduled 메서드 테스트
 *   @Scheduled는 "언제 실행되느냐"만 결정한다.
 *   테스트에서는 "무엇을 하느냐"만 검증하면 된다.
 *   → admit() 메서드를 직접 호출해서 로직을 테스트한다.
 *   스케줄러의 "1초마다 실행" 자체는 Spring 통합 테스트에서 검증할 영역.
 *
 * ■ LinkedHashSet을 쓰는 이유
 *   일반 Set(HashSet)은 순서가 보장되지 않는다.
 *   Redis ZRANGE는 score 순서대로 반환하므로,
 *   테스트에서도 순서가 보장되는 LinkedHashSet을 써야
 *   "10, 11, 12 순서로 처리"를 정확히 검증할 수 있다.
 * ─────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class AdmissionWorkerTest {

    @InjectMocks
    private AdmissionWorker admissionWorker;

    @Mock
    private RedisWaitingRepository waitingRepository;

    @Mock
    private RedisAdmissionRepository admissionRepository;

    // AdmissionWorker 내부에 하드코딩된 ACTIVE_GAME_ID와 동일하게 맞춘다.
    private static final Long GAME_ID = 1L;

    /**
     * [정상 경로 - 배치 처리]
     *
     * 시나리오: 대기열에 3명이 있고, cursor는 0이다.
     *   워커가 0~2 범위의 사용자를 조회해서 각각 토큰 발급 + 상태 변경.
     *   처리 후 cursor를 3만큼 전진.
     *
     * 검증 포인트:
     *   - 3명 모두에게 issueToken이 호출됐는가
     *   - 3명 모두 상태가 ADMITTED로 변경됐는가
     *   - cursor가 정확히 처리 인원(3)만큼 전진했는가
     *
     * 이게 왜 중요한가?
     *   이게 잘못되면 아무도 입장을 못 한다.
     *   배치 처리의 정확성은 시스템 전체의 공정성을 결정한다.
     */
    @Test
    @DisplayName("cursor < queueSize → 배치만큼 사용자에게 토큰 발급하고 커서 전진")
    void admit_processesBatch() {
        // given
        given(waitingRepository.getCursor(GAME_ID)).willReturn(0L);
        given(waitingRepository.getQueueSize(GAME_ID)).willReturn(3L);

        // LinkedHashSet: 삽입 순서가 보장되는 Set (Redis ZRANGE 결과를 흉내냄)
        Set<String> users = new LinkedHashSet<>();
        users.add("10");
        users.add("11");
        users.add("12");
        // end = min(0 + 100 - 1, 3 - 1) = min(99, 2) = 2 → 범위는 0~2
        given(waitingRepository.getUsersInRange(GAME_ID, 0L, 2L)).willReturn(users);

        // when
        admissionWorker.admit();

        // then — 3명 모두 토큰 발급 + 상태 변경
        verify(admissionRepository).issueToken(GAME_ID, 10L);
        verify(admissionRepository).issueToken(GAME_ID, 11L);
        verify(admissionRepository).issueToken(GAME_ID, 12L);
        verify(waitingRepository).updateUserStatus(GAME_ID, 10L, "ADMITTED");
        verify(waitingRepository).updateUserStatus(GAME_ID, 11L, "ADMITTED");
        verify(waitingRepository).updateUserStatus(GAME_ID, 12L, "ADMITTED");
        // cursor 전진: 3명 처리했으므로 3
        verify(waitingRepository).incrementCursor(GAME_ID, 3);
    }

    /**
     * [경계 조건 - 모든 사용자 처리 완료]
     *
     * 시나리오: cursor가 queueSize와 같다.
     *   → 더 이상 처리할 사용자가 없다.
     *   → 아무 작업도 하지 않아야 한다.
     *
     * never()란?
     *   Mockito의 검증 모드. "이 메서드가 한 번도 호출되지 않았어야 한다."
     *   times(0)과 같은 의미.
     *
     * anyLong()이란?
     *   "어떤 Long 값이든 상관없이" 매칭하는 Argument Matcher.
     *   특정 값이 아닌, "호출 자체가 없었는지"를 확인할 때 사용.
     */
    @Test
    @DisplayName("cursor >= queueSize → 아무 작업도 안 함")
    void admit_noWorkWhenCursorCaughtUp() {
        // given — 이미 모든 사용자 처리 완료
        given(waitingRepository.getCursor(GAME_ID)).willReturn(500L);
        given(waitingRepository.getQueueSize(GAME_ID)).willReturn(500L);

        // when
        admissionWorker.admit();

        // then — 토큰 발급도, 커서 전진도 없어야 함
        verify(admissionRepository, never()).issueToken(anyLong(), anyLong());
        verify(waitingRepository, never()).incrementCursor(anyLong(), anyLong());
    }

    /**
     * [엣지 케이스 - ZRANGE가 빈 Set 반환]
     *
     * 어떻게 이런 일이?
     *   cursor < queueSize이지만 ZRANGE가 빈 결과를 줄 수 있다.
     *   예: 다른 Worker가 동시에 cursor를 전진시켜 범위가 어긋난 경우.
     *   (설계문서에서 EC2 2대 동시 실행 방어를 언급한 부분)
     *
     * 검증: 빈 결과면 커서를 전진시키지 않아야 한다.
     *   커서만 전진하면 사용자를 건너뛰는 버그가 된다.
     */
    @Test
    @DisplayName("범위 내 사용자가 비어있으면 커서 전진 안 함")
    void admit_emptyRange_noIncrement() {
        given(waitingRepository.getCursor(GAME_ID)).willReturn(0L);
        given(waitingRepository.getQueueSize(GAME_ID)).willReturn(50L);
        given(waitingRepository.getUsersInRange(GAME_ID, 0L, 49L)).willReturn(Set.of());

        admissionWorker.admit();

        verify(admissionRepository, never()).issueToken(anyLong(), anyLong());
        verify(waitingRepository, never()).incrementCursor(anyLong(), anyLong());
    }

    /**
     * [장애 대응 - 예외 삼킴 테스트]
     *
     * 시나리오: Redis가 죽어서 getCursor()에서 RuntimeException 발생.
     *   (설계문서 시나리오 6: "Redis가 죽으면?")
     *
     * 왜 이 테스트가 중요한가?
     *   AdmissionWorker는 @Scheduled로 1초마다 실행된다.
     *   만약 예외가 밖으로 전파되면 Spring TaskScheduler가
     *   해당 태스크를 *영구 중단*시킨다.
     *   → Redis가 복구돼도 워커가 다시 안 돌아가는 치명적 장애.
     *
     *   admit() 안의 try-catch가 이를 방어한다.
     *   이 테스트는 "예외가 밖으로 새지 않는지"를 확인한다.
     *
     * 테스트 방법:
     *   예외를 던지도록 설정 후 admit()을 호출.
     *   테스트 메서드 자체가 예외 없이 끝나면 → 테스트 통과.
     *   (assertThat 없이도 "예외가 안 터짐" 자체가 검증)
     */
    @Test
    @DisplayName("예외 발생 시 스케줄러가 멈추지 않음 (예외 삼킴)")
    void admit_exceptionDoesNotPropagateToScheduler() {
        // given — Redis 장애 시뮬레이션
        given(waitingRepository.getCursor(GAME_ID))
                .willThrow(new RuntimeException("Redis down"));

        // when & then — 예외가 밖으로 전파되지 않아야 함
        admissionWorker.admit();  // 이 줄에서 예외가 터지면 테스트 실패
    }
}
