package com.hn.ticketing.game.infrastructure;

import com.hn.ticketing.game.domain.Stadium;
import com.hn.ticketing.game.domain.StadiumRepository;
import org.springframework.data.jpa.repository.JpaRepository;

// ── JPA 구장 리포지토리 구현 ──
//
// 왜 두 개를 동시에 extends 하나:
//
// 1) StadiumRepository (도메인 인터페이스)
//    → Service가 의존하는 계약. save(), findById() 메서드 시그니처 정의.
//
// 2) JpaRepository<Stadium, Long> (Spring Data JPA)
//    → Spring이 런타임에 구현체를 자동 생성해준다.
//    → save(), findById(), findAll() 등이 이미 구현되어 있다.
//
// 이 두 개를 합치면:
//    Spring이 자동 생성한 JPA 구현체가 곧 도메인 인터페이스의 구현체가 된다.
//    별도 클래스에 @Repository 붙이고 메서드 하나하나 구현할 필요 없음.
public interface JpaStadiumRepository extends StadiumRepository, JpaRepository<Stadium, Long> {

}
