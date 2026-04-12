package com.hn.ticketing.auth.infrastructure;

import com.hn.ticketing.auth.domain.Member;
import com.hn.ticketing.auth.domain.MemberRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA를 사용한 MemberRepository 구현체.
 *
 * 한 인터페이스가 도메인의 MemberRepository와 Spring Data의 JpaRepository를 동시에 상속.
 * → Spring이 자동으로 구현체 빈을 생성해주고, 도메인은 MemberRepository 타입으로만 주입받음.
 *
 * 패키지는 infrastructure인데 타입은 domain.MemberRepository를 상속하는 이유:
 * → 의존성 방향을 domain ← infrastructure로 유지하기 위함 (DIP)
 */
public interface JpaMemberRepository extends MemberRepository, JpaRepository<Member, Long> {

    // findByLoginId, existsByLoginId는 Spring Data JPA가 메서드 이름 규약으로 자동 구현.
    // MemberRepository 인터페이스에 선언만 해두면 여기서 추가 선언 불필요.

    @Override
    Optional<Member> findByLoginId(String loginId);

    @Override
    boolean existsByLoginId(String loginId);
}
