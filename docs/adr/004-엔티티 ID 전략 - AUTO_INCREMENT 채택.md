배경:
- 분산 시스템에서 Snowflake, TSID, UUID v7 등 분산 ID 전략이 존재
- 이 프로젝트는 EC2 2대 + RDS 1대 구조

고려한 대안:
A. AUTO_INCREMENT (IDENTITY) — DB가 원자적으로 ID 관리
B. UUID v4 — 충돌 없지만 16byte, InnoDB 클러스터 인덱스 성능 최악 (랜덤 삽입)
C. UUID v7 — 시간순, 충돌 없지만 16byte
D. Snowflake / TSID — 8byte, 시간순, 분산 안전하지만 worker ID 관리 + clock skew 문제

결정:
AUTO_INCREMENT 채택

근거:
- DB가 단일 인스턴스(RDS 1대)이므로 ID 충돌이 구조적으로 불가능
- Snowflake는 worker ID 관리, clock skew 대응이라는 새로운 인프라 문제를 유발
- JPA @GeneratedValue(IDENTITY)와의 궁합이 가장 자연스러움
- InnoDB 클러스터 인덱스에 순차 삽입이 되어 쓰기 성능 최적

트레이드오프:
+ 구현 단순, 성능 최적, JPA 표준 동작
- DB 샤딩 또는 MSA 분리 시 전환 필요

재검토 조건:
- payment 모듈을 별도 서비스로 분리하고 자체 DB를 가질 때
- DB 샤딩이 필요해질 때
