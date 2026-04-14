# ADR-007: 좌석 점유 정합성 — Redis Lua 원자 스크립트 + 3층 방어 채택

- **상태(Status)**: Accepted
- **작성일**: 2026-04-14
- **결정자**: hanna
- **관련 문서**: PROJECT_PROPOSAL §7.4, SEAT_HOLD_DESIGN §3.1, §6, ADR-006

---

## Context (배경)

좌석 점유의 핵심 요구사항은 하나다.
"같은 좌석에 100명이 동시에 요청하면, 정확히 1명만 성공해야 한다."

여기에 부가 조건이 붙는다.
- 1인당 최대 4좌석 점유 제한을 동시에 검증해야 한다.
- Redis TTL 만료 후 MySQL에는 CONFIRMED인데 Redis에는 키가 없는 불일치 시나리오를 방어해야 한다.
- 결제 확정 시 MySQL unique 제약이 최종 방어선 역할을 해야 한다.

즉, "좌석 중복 점유 방지"는 단일 명령이 아니라 여러 조건의 원자적 검증이 필요한 문제다.

제약 조건:
- 단일 Redis + MySQL 스택 (ADR-001)
- 좌석 점유 TTL 7분, 진입 토큰 TTL 15분 (SEAT_HOLD_DESIGN §2)

---

## Alternatives Considered (고려한 대안)

### 대안 A: MySQL SELECT FOR UPDATE (비관적 락)

- 장점: 트랜잭션으로 정합성 완벽 보장. Redis 불필요.
- 단점: 모든 점유 요청이 DB까지 내려감.
  동시 100명 요청 시 99명이 락 대기 → 처리량 병목.
  대기열을 거쳐 통과한 사용자에게 다시 락 대기를 시키는 것은 UX 모순.

### 대안 B: Redisson 분산 락

- 장점: 락 획득/해제 API가 명확. 재진입 락, 공정 락 등 옵션 풍부.
- 단점: 단일 Redis 인스턴스에서 분산 락은 과잉 설계.
  락 획득 → 조건 검증 → 점유 → 락 해제의 4단계가 필요한데,
  Lua 스크립트는 이 전체를 1단계로 줄인다.
  Redisson 의존성 추가에 따른 학습·운영 비용 대비 이점 없음.

### 대안 C: Redis SETNX 단일 명령

- 장점: 가장 단순. 좌석 키가 없으면 SET, 있으면 실패.
- 단점: "사용자별 최대 4좌석" 제한을 원자적으로 묶을 수 없음.
  SETNX 성공 후 SCARD 체크 전에 다른 요청이 끼어들면
  한 사용자가 5좌석 이상 점유할 수 있다.
  SEAT_HOLD_DESIGN §3.1: "SETNX 하나로는 부족."

### 대안 D: Redis Lua 원자 스크립트

- 장점: EXISTS + SCARD + HSET + SADD를 원자적으로 실행.
  Redis 단일 스레드 특성상 스크립트 실행 중 다른 명령이 끼어들 수 없음.
  별도 락 메커니즘 없이 정합성 보장.
- 단점: Lua 스크립트 디버깅이 어려움. 스크립트 내부 로직 변경 시 배포 필요.

---

## Decision (결정)

**Redis Lua 원자 스크립트 + 3층 방어 구조를 채택한다.**

- **1층 (Redis Lua)**: 좌석 점유의 99%를 Redis에서 처리한다.
  EXISTS(좌석 점유 여부) + SCARD(사용자 점유 수) + HSET/SADD(점유 실행)를
  단일 Lua 스크립트로 원자 실행한다.

- **2층 (MySQL CONFIRMED 사전 검증)**: Redis Lua 실행 전에
  MySQL에서 해당 좌석이 이미 결제 확정(CONFIRMED)인지 체크한다.
  Redis TTL 만료 후 MySQL만 CONFIRMED인 불일치 시나리오를 방어한다.

- **3층 (MySQL unique 제약)**: 결제 확정 시 reservation 테이블의
  (game_id, seat_id) unique 제약이 최종 방어선 역할을 한다.
  1층과 2층을 모두 통과한 중복이 여기서 잡힌다.

이 결정은 seat 모듈의 좌석 점유·해제 흐름에 적용된다.
결제 확정(3층)은 reservation 모듈의 책임이다.

---

## Rationale (근거)

**Lua가 SETNX + 분산 락을 동시에 대체한다.**
Redis는 단일 스레드로 명령을 처리한다.
Lua 스크립트는 이 단일 스레드에서 원자적으로 실행되므로,
스크립트 내부의 여러 조건 검증 사이에 다른 명령이 끼어들 수 없다.
별도 락 없이도 "좌석 미점유 + 사용자 4좌석 미만"이라는 복합 조건을
원자적으로 검증하고 실행할 수 있다.

**3층 방어는 Redis-MySQL 이중 저장소의 불일치를 구조적으로 해결한다.**
단일 저장소였다면 1층만으로 충분하다.
그러나 Redis(실시간 점유)와 MySQL(영구 확정)이라는 두 저장소를 쓰므로,
TTL 만료·Redis 장애 등으로 불일치가 발생할 수 있다.
2층(MySQL 사전 검증)과 3층(MySQL unique 제약)은
이 불일치가 중복 점유로 이어지는 것을 차단한다.

**DB 비관적 락을 거부한 이유 — 대기열의 존재 의미와 충돌한다.**
ADR-006에서 대기열을 도입한 이유가 "락 경합 폭발 방지"다.
대기열을 통과한 사용자에게 다시 DB 락 대기를 시키면
대기열의 존재 의미가 희석된다.

---

## Consequences (결과·트레이드오프)

### Positive

- 좌석 점유 처리의 99%가 Redis에서 완결된다.
  MySQL까지 도달하는 요청은 Redis를 통과한 성공 케이스뿐이므로 DB 부하 최소화.
- 별도 락 인프라(Redisson, Zookeeper 등) 없이 정합성 보장.
- 3층 방어로 Redis 장애·TTL 만료 등 어떤 시나리오에서도
  한 좌석이 두 명에게 확정되는 것을 구조적으로 차단.

### Negative

- **Lua 스크립트의 SCARD 유령 항목 문제.**
  좌석 hold 키가 TTL로 만료되어도 사용자 holds Set에는 멤버가 남아있을 수 있다.
  SCARD가 실제보다 큰 값을 반환하여 점유가 거부될 수 있다.
  v0.1에서는 holds Set에 TTL을 걸어 자연 만료로 해소한다.
  부하 테스트에서 점유 거부율이 유의미하면 Lua 내 lazy cleanup을 추가한다.

- **Redis 단일 인스턴스 SPOF.**
  Redis 장애 시 좌석 점유 자체가 불가능하다.
  2층(MySQL CONFIRMED 체크)은 동작하지만, 1층(Lua 점유)이 막히므로
  신규 점유는 Redis 복구까지 중단된다.

### Neutral / 향후 영향

- Lua 스크립트는 RedisSeatHoldRepository 내부에 캡슐화된다.
  서비스 레이어는 `holdSeat(gameId, seatId, userId)` 인터페이스만 알면 된다.
- 3층(MySQL unique 제약)은 reservation 모듈 구현 시 자연스럽게 적용된다.
  seat 모듈 단독으로는 1층 + 2층까지만 동작한다.

---

## Revisit Triggers (재검토 조건)

- 부하 테스트에서 SCARD 유령 항목으로 인한 점유 거부율이 1%를 넘으면 → Lua 내 lazy cleanup 추가
- Redis 장애가 실제로 발생하면 → Sentinel/Cluster 전환 및 Lua 스크립트 호환성 검토
- 좌석 점유 조건이 복잡해지면 (등급별 제한, VIP 우선 등) → Lua 스크립트 분리 또는 서비스 레이어 전환 검토
