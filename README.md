# 🎟️ Daejeon Ticketing

> 오픈런 트래픽을 견디는 단일 구단 티켓팅 백엔드 시스템

[![Status](https://img.shields.io/badge/status-design--phase-yellow)](./docs/PROJECT_PROPOSAL.md)
[![Stack](https://img.shields.io/badge/stack-Spring%20Boot%203-brightgreen)]()
[![DB](https://img.shields.io/badge/DB-MySQL%208-blue)]()
[![Cache](https://img.shields.io/badge/cache-Redis%207-red)]()
[![MQ](https://img.shields.io/badge/MQ-Kafka-black)]()
 
---

## 한 줄 소개

KBO 인기 매치 티켓 오픈런 상황  *5만 명이 1분 안에 몰리는 트래픽* — 을 견디도록 설계한 티켓팅 백엔드 API. 모듈러 모놀리스, Outbox 패턴, Redis 기반 대기열·좌석 점유, Kafka 비동기 파이프라인으로 *고정합성과 고처리량을 동시에* 달성하는 것을 목표로 한다.

> ⚠️ 본 프로젝트는 학습·포트폴리오 목적의 시뮬레이션이며, 실제 구단·구장과 어떠한 제휴나 데이터 연동도 없습니다. 좌석 배치 등은 공개 정보를 모티브로 한 가상 모델입니다.
 
---

## 🎯 왜 이 프로젝인가

KBO 리그의 인기 매치(한국시리즈, 신축 구장 개막전, 라이벌 매치)는 티켓 오픈 직후 1분 안에 수만 명의 동시 접속이 몰린다. 
 매년 기존 예매 플랫폼이 *대기열 폭주, 좌석 중복 점유, 결제 실패 후 좌석 미반환* 같은 장애를 반복해 뉴스에 오른다.
본 프로젝트는 이러한 **고트래픽·고정합성이 동시에 요구되는 도메인**을 직접 구현하면서, 분산 시스템의 핵심 패턴(모듈러 모놀리스, Transactional Outbox, 분산 상태 원자 점유, Kafka 기반 비동기 처리, 3층 방어 정합성)이 *실제로 어떤 문제를 풀기 위해 존재하는지*를 코드와 실험으로 검증하는 것을 목표로 한다.
**본 프로젝트의 진짜 가치는 "무엇을 만들었는가"가 아니라 "왜 이렇게 만들었는가"의 기록이다.** 그래서 상세 설계 문서와 의사결정 기록(ADR)이 코드만큼 중요한 산출물이다.
 
---

## 🔥 핵심 기술적 도전 — 5가지

본 프로젝트가 풀고자 하는 *진짜 문제들*. 각각 별도 설계 문서가 있다.

### 1. 좌석 동시 점유 — 단일 자원에 대한 동시 쓰기

수백 명이 같은 좌석을 동시에 노릴 때 *정확히 한 명*에게만 점유를 허용해야 한다. 
Redis Lua 스크립트로 EXISTS + HSET을 원자적으로 실행하여 *분산 락 없이* 해결. 
최종 방어선으로 MySQL 유니크 제약을 포함한 **3층 방어** 구성.

→ 상세: [`docs/SEAT_HOLD_DESIGN.md`](./docs/SEAT_HOLD_DESIGN.md)
### 2. 결제-예매 정합성 — Dual Write 문제

결제 성공을 DB와 Kafka에 *동시에* 쓰는 것은 원자적으로 불가능하다. 
**Transactional Outbox 패턴**으로 이중 쓰기를 *DB 단일 쓰기*로 환원. 
별도 워커가 `SELECT ... FOR UPDATE SKIP LOCKED`로 다중 인스턴스 안전성 확보 후 Kafka로 발행.

→ 상세: [`docs/OUTBOX_PATTERN_DESIGN.md`](./docs/OUTBOX_PATTERN_DESIGN.md), [`docs/PAYMENT_SIMULATOR_DESIGN.md`](./docs/PAYMENT_SIMULATOR_DESIGN.md)

### 3. 대기열 공정성 — FIFO + 새로고침 안전

5만 명이 동시 접속하는 오픈런에서 *공정한 순서*를 보장하고, 사용자가 새로고침해도 순번이 밀리지 않게. 
Redis Sorted Set(`ZADD NX`) + Hash + 진입 토큰의 조합. *대기 중*과 *입장 허가* 상태를 명확히 분리.

→ 상세: [`docs/WAITING_QUEUE_DESIGN.md`](./docs/WAITING_QUEUE_DESIGN.md)

### 4. 읽기·쓰기 트래픽 비대칭

좌석 조회는 초당 수천, 좌석 점유는 초당 수백. 같은 모델로 처리하면 조회가 점유를 압박한다. 
**좌석 상태는 Redis, 좌석 확정은 MySQL**로 저장소를 분리. *ERD에 좌석 상태 컬럼을 의도적으로 두지 않는* CQRS 사상의 부분 적용.

→ 상세: [`docs/ERD.md`](./docs/ERD.md) §1.3

### 5. 장애 격리 — 인프라 레벨 격리

알림 컨슈머의 장애가 통계·검색에 영향 없어야 한다. *코드 try-catch*가 아니라 **Kafka 컨슈머 그룹 분리**로 격리. 
컨슈머 그룹·DLT·멱등 테이블·알람 임계치의 **4가지 층위**로 독립 격리.

→ 상세: [`docs/KAFKA_CONSUMER_DESIGN.md`](./docs/KAFKA_CONSUMER_DESIGN.md)
 
---

## 🧰 기술 스택

| 구분 | 기술 | 선정 근거 |
|---|---|---|
| **언어·런타임** | Java 21 (LTS) | Virtual Thread로 I/O 바운드 처리 효율 |
| **프레임워크** | Spring Boot 3.x | 한국 백엔드 시장 표준 |
| **인증** | Spring Security + JWT | Stateless, 다중 EC2 분산 구조와 정합 ([ADR-0003](./docs/adr/0003-include-jwt-authentication.md)) |
| **주 저장소** | MySQL 8 (InnoDB) | 강의 자료 일관성·시장 정합성 ([ADR-0004](./docs/adr/0004-mysql-as-primary-datastore.md)) |
| **캐시·상태** | Redis 7 | 대기열·좌석 점유·분산 동시성 제어 |
| **메시지 브로커** | Kafka | 결제 후 비동기 파이프라인, 장애 격리 |
| **ORM** | Spring Data JPA | 도메인 중심 매핑 + Native Query 병행 |
| **모듈 경계 강제** | ArchUnit | 모듈 간 의존성을 빌드 시점에 검증 ([ADR-0002](./docs/adr/0002-modular-monolith.md)) |
| **부하 테스트** | k6 | JS 기반 시나리오, 결과를 코드로 관리 |
| **로컬 개발** | Docker Compose | MySQL·Redis·Kafka 통합 환경 |
| **배포 (예정)** | AWS EC2 + RDS + ALB | 6개월차 인프라 작업 |
| **CI/CD (예정)** | GitHub Actions | 무료, 레포 내 가시성 |

**아키텍처 스타일:** 모듈러 모놀리스 (Modular Monolith) — MSA가 *아닌* 의도된 선택. 자세한 이유는 [ADR-0002](./docs/adr/0002-modular-monolith.md).
 
---