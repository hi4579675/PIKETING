package com.hn.ticketing.seat.api;

import com.hn.ticketing.seat.api.dto.SeatHoldResponse;
import com.hn.ticketing.seat.domain.SeatHoldService;
import com.hn.ticketing.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/{gameId}/seats")
@RequiredArgsConstructor
public class SeatHoldController {
    private final SeatHoldService seatHoldService;

    @PostMapping("/{seatId}/hold")
    public ResponseEntity<ApiResponse<SeatHoldResponse>> holdSeat(
            @PathVariable Long gameId,
            @PathVariable Long seatId,
            @AuthenticationPrincipal Long memberId,
            @RequestHeader("X-Admission-Token") String admissionToken) {

        SeatHoldResponse response = seatHoldService.holdSet(gameId, seatId, memberId, admissionToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @DeleteMapping("/{seatId}/hold")
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @PathVariable Long gameId,
            @PathVariable Long seatId,
            @AuthenticationPrincipal Long memberId) {
        seatHoldService.releaseSeat(gameId, seatId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
