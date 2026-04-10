# ADR-003: Shared Layer 공통 패턴 — ApiResponse record 채택 + BaseException 계층

- **Status**: Accepted
- **Date**: 2026-04-09
- **Deciders**: hanna
- **관련 문서**: ADR-001 (모듈러 모놀리스)

---

## Context

모든 모듈이 공통으로 쓰는 두 가지 설계를 결정해야 했다.

1. **API 응답 형식** — 모든 컨트롤러가 `{ status, message, data }` 형태로 응답을 통일하기 위한 `ApiResponse` 클래스 설계
2. **도메인 예외 계층** — 모듈별 예외(`ReservationException`, `PaymentException` 등)를 `GlobalExceptionHandler`에서 어떻게 처리할 것인가

Java 21 + Spring Boot 3.5 환경이므로 record, sealed class 등 최신 언어 기능을 사용할 수 있다.

---

## Alternatives Considered

### [ApiResponse] 대안 A: `class` + private 생성자 + 정적 팩토리 메서드
- **장점**: 생성자를 private으로 숨겨 팩토리 메서드 사용을 강제할 수 있다
- **단점**: `@Getter`, private 생성자, 필드 선언 등 보일러플레이트가 많다. ApiResponse는 단순 데이터 운반 객체이므로 생성 방식 강제의 실익이 없다

### [ApiResponse] 대안 B: `record`
- **장점**: 보일러플레이트 제거. 불변성 보장. Java 21 + Spring Boot 3.x에서 Jackson 직렬화 완벽 지원. 정적 팩토리 메서드도 record 안에 정의 가능
- **단점**: canonical constructor를 private으로 숨길 수 없어 `new ApiResponse<>(...)` 직접 호출을 막을 수 없다

---

### [BaseException] 대안 A: 예외마다 `RuntimeException` 직접 상속
- **장점**: 별도 추상 클래스 불필요
- **단점**: `GlobalExceptionHandler`가 도메인 예외를 하나씩 `@ExceptionHandler`로 등록해야 한다. 예외 추가 시마다 핸들러 수정이 필요하고, HTTP 상태코드가 핸들러에 분산된다

### [BaseException] 대안 B: `BaseException(HttpStatus, message)` 추상 클래스
- **장점**: HTTP 상태코드를 예외 자체가 가지므로, `GlobalExceptionHandler`는 `BaseException` 하나만 잡으면 된다. 도메인 예외가 늘어도 핸들러를 수정하지 않아도 된다
- **단점**: 모든 도메인 예외가 `BaseException`을 상속해야 한다는 컨벤션이 생긴다

---

## Decision

- `ApiResponse<T>`는 **record**로 구현한다
- 모든 도메인 예외의 공통 부모로 `BaseException(HttpStatus, String)`을 두고, `GlobalExceptionHandler`는 `BaseException` 하나로 처리한다

---

## Rationale

**ApiResponse**: `ApiResponse`는 단순 데이터 운반이 목적이고, 생성 방식을 강제해야 할 이유가 없다. record가 더 간결하고 의도를 명확히 표현한다.

**BaseException**: 도메인 예외가 HTTP 상태코드를 직접 들고 다니면, 핸들러는 응답 조립만 담당하게 된다. 관심사 분리가 명확하고, 예외 추가 시 핸들러를 건드릴 필요가 없어 OCP를 지킨다.

---

## Consequences

### Positive
- `ApiResponse` 보일러플레이트 제거 (Lombok 불필요)
- 새 도메인 예외 추가 시 `GlobalExceptionHandler` 수정 불필요
- HTTP 상태코드가 예외 정의부에 집중되어 가시성 향상

### Negative
- `ApiResponse` 생성자 호출을 컴파일 타임에 막을 수 없다 (컨벤션으로 관리)
- 모든 도메인 예외가 반드시 `BaseException`을 상속해야 한다는 팀 컨벤션 필요

### Neutral
- 외부 라이브러리 예외(`DataIntegrityViolationException` 등)는 `BaseException`을 상속할 수 없으므로 `GlobalExceptionHandler`에 별도 핸들러를 추가해야 할 수 있다

---

## Revisit Triggers

- 재검토 불필요. 단, 외부 예외 처리가 복잡해지면 `ErrorCode` enum 도입을 검토한다
