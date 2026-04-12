package com.hn.ticketing.auth.domain;

import java.util.Optional;

/**
 * 도메인 계층의 Repository 인터페이스.
 * JPA에 직접 의존하지 않도록 인터페이스로 정의하고,
 * 구현체는 infrastructure 계층의 JpaMemberRepository가 담당.
 *
 * 이렇게 분리하면 테스트에서 Fake 구현체로 쉽게 교체 가능하고,
 * 향후 저장소가 바뀌어도 도메인 코드는 영향받지 않는다.
 */
public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
