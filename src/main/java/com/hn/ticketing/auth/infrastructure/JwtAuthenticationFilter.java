package com.hn.ticketing.auth.infrastructure;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 매 요청마다 한 번씩 실행되는 JWT 인증 필터.
 * OncePerRequestFilter를 상속해서 forward/include 시에도 중복 실행되지 않도록 보장.
 *
 * 흐름:
 *   1. Authorization 헤더에서 Bearer 토큰 추출
 *   2. 토큰 유효하면 SecurityContext에 Authentication 주입
 *   3. 유효하지 않으면 그냥 통과 (이후 SecurityConfig의 authorizeHttpRequests에서 401 반환)
 *
 * 여기서 직접 401을 던지지 않는 이유:
 *   - permitAll 경로(/health, /api/auth/**)는 토큰이 없어도 통과해야 함
 *   - 인증/인가 결정은 Spring Security가 일관되게 하도록 위임
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validate(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            Long memberId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);

            // principal에 memberId를 직접 넣는다.
            // 컨트롤러에서 @AuthenticationPrincipal Long memberId로 꺼낼 수 있음.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,  // credentials는 토큰 검증 후 불필요
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * "Bearer xxx.yyy.zzz" 형태에서 토큰 부분만 추출.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
