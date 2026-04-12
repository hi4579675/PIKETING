package com.hn.ticketing.game.api;

import com.hn.ticketing.game.api.dto.GameListResponse;
import com.hn.ticketing.game.api.dto.GameResponse;
import com.hn.ticketing.game.api.dto.SeatLayoutResponse;
import com.hn.ticketing.game.domain.GameService;
import com.hn.ticketing.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// ── 사용자용 경기 API ──
// 인증된 사용자가 호출. JWT 필수.
// auth의 AuthController와 동일한 패턴.

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GameListResponse>>> getGames(
            @RequestParam(required = false) LocalDate date) {
        List<GameListResponse> games = gameService.getGames(date);
        return ResponseEntity.ok(ApiResponse.success(games));
    }

    // GET /api/games/{gameId}
    @GetMapping("/{gameId}")
    public ResponseEntity<ApiResponse<GameResponse>> getGame(
            @PathVariable Long gameId) {
        GameResponse game = gameService.getGame(gameId);
        return ResponseEntity.ok(ApiResponse.success(game));
    }

    // GET /api/games/{gameId}/seats
    // 좌석 배치도 조회. 성능 목표 3,000 req/s.
    @GetMapping("/{gameId}/seats")
    public ResponseEntity<ApiResponse<List<SeatLayoutResponse>>> getSeatLayout(
            @PathVariable Long gameId) {
        List<SeatLayoutResponse> seatLayout = gameService.getSeatLayout(gameId);
        return ResponseEntity.ok(ApiResponse.success(seatLayout));
    }
}
