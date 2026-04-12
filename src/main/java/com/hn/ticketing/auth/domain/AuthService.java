package com.hn.ticketing.auth.domain;

import com.hn.ticketing.auth.api.dto.LoginRequest;
import com.hn.ticketing.auth.api.dto.LoginResponse;
import com.hn.ticketing.auth.api.dto.SignupRequest;
import com.hn.ticketing.auth.domain.exception.DuplicateLoginIdException;
import com.hn.ticketing.auth.domain.exception.InvalidPasswordException;
import com.hn.ticketing.auth.domain.exception.MemberNotFoundException;
import com.hn.ticketing.auth.infrastructure.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입, 로그인 비즈니스 로직.
 *
 * 도메인 계층에 두는 이유:
 *   - AuthService는 MemberRepository 인터페이스에만 의존 (JPA 몰라도 됨)
 *   - PasswordEncoder, JwtTokenProvider는 인프라지만 Spring 표준 / 내부 컴포넌트이므로 허용
 *
 * 향후 인증 모듈을 서비스로 분리할 때 이 파일은 거의 수정 없이 옮길 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입.
     *
     * existsByLoginId 체크 후 save까지 사이에 race condition이 존재할 수 있다.
     * 그래서 DB의 unique 제약(uk_member_login_id)이 최종 방어선이다.
     * 동시 가입 시 DataIntegrityViolationException이 발생하는데,
     * 이건 GlobalExceptionHandler에서 별도 처리하거나 여기서 try-catch로 래핑할 수 있다.
     *
     * 현재는 학습용이므로 단순 체크만 하고, race는 드문 케이스라 감내한다.
     */
    @Transactional
    public Long signup(SignupRequest request) {
        if(memberRepository.existsByLoginId(request.loginId())) {
            throw new DuplicateLoginIdException();
        }

        // 비밀번호는 엔티티에 저장되기 전에 반드시 해싱
        String encoded = passwordEncoder.encode(request.password());
        Member member = Member.create(request.loginId(), encoded, request.nickname());
        Member saved = memberRepository.save(member);
        return saved.getId();
    }
    /**
     * 로그인 → JWT 발급.
     * 트랜잭션은 readOnly로 충분 (로그인 시 DB 쓰기 없음).
     */
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(MemberNotFoundException::new);

        if(!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new InvalidPasswordException();
        }
        String token = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        return LoginResponse.of(token, jwtTokenProvider.getValidityInSeconds());
    }

}
