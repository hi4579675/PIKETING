package com.hn.ticketing.game.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// ── 구역 등록 요청 DTO ──
// POST /api/admin/sections 에서 사용.

public record CreateSectionRequest(

        @NotNull(message = "구장 ID는 필수입니다")
        Long stadiumId,

        @NotBlank(message = "구역 이름은 필수입니다")
        String name,

        // seatGrade — String으로 받아서 Service에서 SeatGrade.valueOf()로 변환.
        // enum을 직접 받을 수도 있지만, 잘못된 값이 오면
        // Jackson 역직렬화 에러가 나서 메시지가 불친절하다.
        // String으로 받고 Service에서 변환하면 친절한 에러 메시지를 줄 수 있다.
        @NotBlank(message = "좌석 등급은 필수입니다")
        String seatGrade,

        // totalSeats — 이 구역의 총 좌석 수.
        // 실제 Seat INSERT와는 별개. 비정규화 필드 용도.
        @NotNull(message = "총 좌석 수는 필수입니다")
        @Min(value = 1, message = "좌석 수는 1 이상이어야 합니다")
        Integer totalSeats
) {}
