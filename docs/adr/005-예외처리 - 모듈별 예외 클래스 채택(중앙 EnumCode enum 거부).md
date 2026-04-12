배경:
- 이전 프로젝트(product/order 2도메인)에서는 중앙 ErrorStatus enum에
  모든 에러 코드를 모으는 방식이 깔끔하게 동작했음
- 이번 프로젝트는 8개 모듈의 모듈러 모놀리스
- ADR-003에서 BaseException 계층은 결정했으나,
  각 모듈의 예외를 어디에 정의할지는 미결이었음

고려한 대안:
A. 중앙 ErrorStatus enum (shared/status/)
- 모든 모듈의 에러 코드를 하나의 enum에 관리
- 장점: 한눈에 전체 에러 확인 가능
- 단점: shared가 모든 모듈의 도메인 지식을 알게 됨 (의존 역전)
에러 추가 시 shared 수정 필요 (모듈 독립성 깨짐)
MSA 분리 시 enum 쪼개기 필요
8개 모듈 × 5개 에러 = 40개 항목의 god enum

B. 모듈별 예외 클래스 (각 module/domain/exception/)
- 각 모듈이 자기 예외를 BaseException 상속으로 정의
- 장점: 모듈 독립성 유지, MSA 분리 시 폴더 복사로 끝
- 단점: 전체 에러 목록을 한눈에 볼 수 없음

결정:
B 채택. 각 모듈은 자기 domain/exception/ 안에 예외를 정의하고,
shared는 BaseException과 GlobalExceptionHandler만 제공.

근거:
- ADR-001(모듈러 모놀리스)의 핵심 원칙 "모듈 경계 유지"와 직접 연결
- shared는 가장 안정적인 레이어여야 하는데, 매 모듈 에러 추가 시
  shared가 수정되는 것은 이 원칙에 위배
- 함께 변경되는 것은 함께 있어야 한다 (응집도)

재검토 조건:
- 재검토 불필요
