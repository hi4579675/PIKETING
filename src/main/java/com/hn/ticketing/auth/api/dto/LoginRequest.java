package com.hn.ticketing.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}