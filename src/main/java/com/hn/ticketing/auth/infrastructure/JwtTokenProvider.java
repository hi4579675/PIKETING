package com.hn.ticketing.auth.infrastructure;

import com.hn.ticketing.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 담당.
 *
 * 이 프로젝트는 Stateless 인증이므로 토큰 자체가 세션이다.
 * Refresh token은 비목표(§3.2).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final String secret;
    private final long validityInMilliseconds;
    private SecretKey key;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long validityInSeconds
    ) {
        this.secret = secret;
        this.validityInMilliseconds = validityInSeconds * 1000;
    }

    /**
     * 빈 초기화 시점에 SecretKey를 한 번만 생성.
     * 매 요청마다 만들면 낭비이고, 상수로 두면 테스트에서 교체 불가.
     */
    @PostConstruct
    protected void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 기준 최소 256bit(32byte) 필요. application.yml에서 충분히 긴 시크릿을 넣어야 함.
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 액세스 토큰 발급.
     * subject에 memberId를 넣어 인증 필터에서 사용자 식별에 쓴다.
     * claim에 role을 넣어 권한 체크를 토큰만으로 가능하게 한다.
     */
    public String createAccessToken(Long memberId, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 Claims 추출. 검증도 내부적으로 같이 수행됨.
     * 만료/서명 오류 시 JwtException 계열이 던져짐.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 필터에서 토큰 유효성만 빠르게 체크할 때 사용.
     * 예외를 던지지 않고 boolean 반환하는 편이 필터 코드가 깔끔해진다.
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public long getValidityInSeconds() {
        return validityInMilliseconds / 1000;
    }
}
