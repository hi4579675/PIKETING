package com.hn.ticketing.auth.api;

import com.hn.ticketing.auth.api.dto.LoginRequest;
import com.hn.ticketing.auth.api.dto.LoginResponse;
import com.hn.ticketing.auth.api.dto.SignupRequest;
import com.hn.ticketing.auth.domain.AuthService;
import com.hn.ticketing.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    /**
     * 회원가입 API.
     * @Valid로 SignupRequest의 Bean Validation 동작.
     * 검증 실패 시 MethodArgumentNotValidException → GlobalExceptionHandler가 400으로 응답.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@Valid @RequestBody SignupRequest request) {
        Long memberId = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입 성공", memberId));
    }

    /**
     * 로그인 API. 성공 시 JWT 반환.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }
}
