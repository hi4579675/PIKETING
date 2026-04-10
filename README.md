# 🎟️ PIKETING — KBO 티켓팅 백엔드

> KBO 인기 매치 오픈런 트래픽을 견디는 단일 구단 티켓팅 백엔드 API

[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)]()
[![Redis](https://img.shields.io/badge/Redis-7-red)]()
[![Kafka](https://img.shields.io/badge/Kafka-3.x-black)]()
[![Status](https://img.shields.io/badge/status-in--progress-yellow)]()
 
---

## 한 줄 소개

한화 신구장 티켓 오픈런처럼 **수만 명이 1분 안에 몰리는 상황**에서,
좌석 중복 점유 없이, 결제와 예매 상태가 어긋나지 않도록 설계한 티켓팅 백엔드입니다.

> ⚠️ 학습·포트폴리오 목적의 시뮬레이션입니다. 실제 구단·구장과 어떠한 제휴나 데이터 연동도 없습니다.

---

## 프로젝트 한 줄 소개

한화 신구장 티켓 오픈런처럼 **수만 명이 1분 안에 몰리는 상황**에서,
좌석 중복 점유 없이, 결제와 예매 상태가 어긋나지 않도록 설계한 티켓팅 백엔드입니다.

> ⚠️ 학습·포트폴리오 목적의 시뮬레이션입니다. 실제 구단·구장과 어떠한 제휴나 데이터 연동도 없습니다.

---

## 왜 만들었는가

신입 준비하면서 분산 시스템 이론 — Outbox 패턴, 낙관적 락, Redis 원자 연산 — 을 많이 읽었는데,
실제로 손으로 짜본 적이 없었습니다.

마침 KBO 티켓 오픈런 장애가 뉴스에 자주 나왔고,
그 원인을 추적하면 제가 공부하던 주제들이 그대로 나왔습니다.
"현실에서 매년 터지는 문제를 직접 코드로 풀어보자"는 게 출발점입니다.

기능을 만드는 게 목적이 아니라 **왜 이렇게 설계했는지**를 설명할 수 있는 것이 목적입니다.
그래서 코드만큼 설계 문서와 ADR이 중요한 산출물입니다.

---

## 핵심 기술 도전 — 5가지

### 1. 좌석 동시 점유

같은 좌석을 동시에 수십 명이 노릴 때 **정확히 한 명에게만** 점유를 허용해야 합니다.

- MySQL 비관적 락 / 낙관적 락 / Redis 원자 점유 세 가지를 직접 구현해서 k6로 비교
- Redis Lua 스크립트로 `EXISTS + HSET`을 원자 처리 → 분산 락 없이 해결
- MySQL 유니크 제약을 최후 방어선으로 두는 **3층 방어** 구성

→ [`docs/SEAT_HOLD_DESIGN.md`](./docs/SEAT_HOLD_DESIGN.md)

### 2. 결제-예매 정합성 (Dual Write 문제)

결제 DB 커밋과 Kafka 이벤트 발행을 **동시에 원자적으로** 처리할 방법은 없습니다.

- **1차**: 동기 트랜잭션으로 먼저 구현하고, 통합 테스트에서 이중 쓰기 문제를 직접 체감
- **2차**: Transactional Outbox 패턴으로 리팩토링 — DB에 먼저 쓰고, 워커가 Kafka로 발행

처음부터 Outbox로 짜지 않고 순서를 나눈 이유가 있습니다.
동기 버전을 먼저 짜야 "Kafka 죽여놓고 결제했더니 DB엔 성공인데 이벤트가 사라지네" 하는 순간을 직접 겪을 수 있기 때문입니다.
그 순간이 Outbox를 쓰는 이유를 체감하는 순간입니다.

→ [`docs/OUTBOX_PATTERN_DESIGN.md`](./docs/OUTBOX_PATTERN_DESIGN.md)

### 3. 대기열 공정성

5만 명이 동시 접속할 때 공정한 순서를 보장하고, 새로고침해도 순번이 밀리지 않아야 합니다.

- Redis Sorted Set + `ZADD NX` 옵션으로 FIFO 구현
  (`NX`가 빠지면 새로고침 = 순번 밀림 버그가 그대로 박힙니다)
- 진입 토큰을 대기열과 분리된 별개 자원으로 관리 (Redis String + TTL 15분)

→ [`docs/WAITING_QUEUE_DESIGN.md`](./docs/WAITING_QUEUE_DESIGN.md)

### 4. 읽기·쓰기 트래픽 비대칭

좌석 조회는 초당 수천 건, 점유는 초당 수백 건입니다.
같은 저장소로 처리하면 조회가 점유를 압박합니다.

- 좌석 **현재 점유 상태** → Redis (초당 수천 회 변경)
- 좌석 **영구 확정 여부** → MySQL `reservations` 테이블
- ERD에 `seat_status` 컬럼이 없는 이유가 이것입니다

### 5. 장애 격리

알림 처리가 느려져도 결제 플로우는 영향을 받으면 안 됩니다.

- Outbox + Kafka 컨슈머 분리로 알림 장애가 결제에 전파되지 않도록 격리
- 2차 보강 단계에서 실현 (1차는 동기 처리)

---

## 기술 스택

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

**아키텍처 스타일**: 모듈러 모놀리스 — MSA가 아닌 의도된 선택입니다. ([ADR-001](./docs/adr/001-modular-monolith.md))

---

## 프로젝트 구조

```
src/main/java/com/hn/ticketing/
├── shared/ # 공통 인프라 (BaseEntity, 설정, 공통 응답/예외)
├── auth/ # 회원가입, 로그인, JWT 필터
├── game/ # 경기 조회, 좌석 배치도
├── waiting/ # 대기열 진입, 순번 조회, 진입 토큰
├── seat/ # 좌석 점유, TTL 만료
├── reservation/ # 예매 확정, 내역 조회, 취소
├── payment/ # 결제 요청, 시뮬레이터
├── notification/ # Kafka 컨슈머, 알림 처리 (2차)
└── outbox/ # Outbox 이벤트, 워커 (2차)
```

모듈 간 의존성은 ArchUnit으로 빌드 시점에 검증합니다.
`shared` 외의 모듈은 서로 직접 참조하지 않습니다.

---
 

## 구현 범위

### 1차 완성본 (현재 진행 중)

- [x] 공통 인프라 (BaseEntity, Redis, Kafka, Security 설정)
- [ ] 회원가입 / 로그인 / JWT 인증 필터
- [ ] 경기 목록·상세 조회
- [ ] 좌석 배치도 조회
- [ ] 대기열 진입 / 순번 조회 / 진입 토큰 발급
- [ ] 좌석 원자 점유 (Redis Lua + MySQL 3층 방어)
- [ ] 결제 시뮬레이터 (성공 / 실패 / 멱등 재요청)
- [ ] 결제 → 예매 확정 (동기 트랜잭션)
- [ ] 점유 7분 TTL 자동 해제
- [ ] 예매 내역 조회 / 취소
- [ ] k6 부하 테스트 (MySQL 락 vs Redis 원자 점유 비교)

### 2차 보강 (1차 완료 후)

- [ ] Outbox 패턴 리팩토링 (결제-예매 정합성)
- [ ] Kafka 도입 + notification 컨슈머
- [ ] AWS 배포 (EC2 2대 + RDS + ALB)
- [ ] GitHub Actions CI/CD

---

## 성능 목표

| 지표 | 목표 |
|---|---|
| 좌석 조회 처리량 | 3,000 req/s 이상 |
| 좌석 점유 처리량 | 500 req/s 이상 |
| 좌석 조회 p99 응답시간 | 200ms 이하 |
| 좌석 점유 p99 응답시간 | 500ms 이하 |
| 대기열 진입 처리량 | 5,000 req/s 이상 |
| 동일 좌석 중복 점유 | 0건 (10만 건 부하 기준) |
| 결제 성공 후 좌석 미확정 | 0건 |

수치 미달이 실패는 아닙니다.
못 맞춘 원인 분석을 부하 테스트 리포트에 기록하는 것도 학습 산출물입니다.

---

## 로컬 실행

```bash
# 인프라 실행
docker-compose up -d

# 앱 실행
./gradlew bootRun
```

필요한 것

Docker
Java 21
(시드 데이터) 경기 1개, 좌석 2만 개
환경 변수는 application.yml 참고.