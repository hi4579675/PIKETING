package com.hn.ticketing.game.api.dto;

import com.hn.ticketing.game.api.dto.CreateGameRequest;
import com.hn.ticketing.game.api.dto.CreateSectionRequest;
import com.hn.ticketing.game.domain.AdminGameService;
import com.hn.ticketing.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// ── 관리자용 경기 API ──
// 경기 등록, 구역 등록, 좌석 등록.
// 현재는 인증만 확인 (JWT 필수).
// ADMIN 권한 체크는 추후 추가 가능.

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminGameController {

    private final AdminGameService adminGameService;

    // POST /api/admin/games
    // 경기 등록 + 해당 구장의 모든 좌석에 대해 GameSeat 자동 생성.
    @PostMapping("/games")
    public ResponseEntity<ApiResponse<Long>> createGame(
            @Valid @RequestBody CreateGameRequest request) {
        Long gameId = adminGameService.createGame(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("경기 등록 성공", gameId));
    }

    // POST /api/admin/sections
    // 구역 등록.
    @PostMapping("/sections")
    public ResponseEntity<ApiResponse<Long>> createSection(
            @Valid @RequestBody CreateSectionRequest request) {
        Long sectionId = adminGameService.createSection(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("구역 등록 성공", sectionId));
    }

    // POST /api/admin/sections/{sectionId}/seats
    // 좌석 일괄 등록.
    // 예: seatRow=A, startNumber=1, endNumber=20 → A열 1~20번 생성
    @PostMapping("/sections/{sectionId}/seats")
    public ResponseEntity<ApiResponse<Integer>> createSeats(
            @PathVariable Long sectionId,
            @RequestParam String seatRow,
            @RequestParam int startNumber,
            @RequestParam int endNumber) {
        int count = adminGameService.createSeats(sectionId, seatRow, startNumber, endNumber);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("좌석 등록 성공", count));
    }
}
