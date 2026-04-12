package com.hn.ticketing.auth.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 * Bean Validation 어노테이션은 @Valid가 붙은 컨트롤러 파라미터에서 자동 검증되어
 * GlobalExceptionHandler의 handleValidationException으로 전달된다.
 */
public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 4~20자 영문/숫자/언더바만 가능")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 30, message = "비밀번호는 8~30자")
        String password,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 15, message = "닉네임은 2~15자")
        String nickname
) {
}