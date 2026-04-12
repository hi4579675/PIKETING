package com.hn.ticketing.auth.domain;

import com.hn.ticketing.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티.
 * 기획안에서 인증은 "동작하는 수준"까지만 구현(Out-of-Scope: refresh token, 소셜 로그인 등).
 * 따라서 필드는 최소한만 둔다.
 */
@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                // loginId 중복 체크는 서비스 계층에서 먼저 하지만,
                // 동시 가입 요청에서의 race condition을 막기 위해 DB 유니크 제약이 최종 방어선이다.
                @UniqueConstraint(name = "uk_member_login_id", columnNames = "login_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA는 기본 생성자 필요, 외부에서는 Builder만 쓰도록 제한
public class Member extends BaseEntity {

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    // BCrypt 해시 값. 60자 고정.
    @Column(name = "password", nullable = false, length = 60)
    private String password;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)  // ORDINAL은 enum 순서 변경 시 재앙이므로 반드시 STRING
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Builder
    private Member(String loginId, String password, String nickname, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
    }

    /**
     * 회원가입 시 사용하는 팩토리 메서드.
     * 비밀번호 해싱은 서비스 계층에서 이미 완료된 상태로 전달받는다.
     * (엔티티가 PasswordEncoder에 의존하면 도메인이 인프라에 오염됨)
     */
    public static Member create(String loginId, String encodedPassword, String nickname) {
        return Member.builder()
                .loginId(loginId)
                .password(encodedPassword)
                .nickname(nickname)
                .role(Role.USER)
                .build();
    }
}