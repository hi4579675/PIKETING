package com.hn.ticketing.waiting.api;

import com.hn.ticketing.shared.dto.ApiResponse;
import com.hn.ticketing.waiting.api.dto.WaitingEnterResponse;
import com.hn.ticketing.waiting.api.dto.WaitingStatusResponse;
import com.hn.ticketing.waiting.domain.WaitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/{gameId}/waiting")
@RequiredArgsConstructor
public class WaitingController {

    public final WaitingService waitingService;

    @PostMapping("/enter")
    public ResponseEntity<ApiResponse<WaitingEnterResponse>> enter(
            @PathVariable Long gameId,
            @AuthenticationPrincipal Long memberId) {
        WaitingEnterResponse response = waitingService.enterQueue(gameId, memberId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // GET /api/games/{gameId}/waiting/status
    //   @AuthenticationPrincipal Long memberId
    //   → waitingService.getStatus(gameId, memberId)
    //   → status가 ADMITTED면 token 포함, WAITING이면 ahead 포함
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<WaitingStatusResponse>> getStatus(
            @PathVariable Long gameId,
            @AuthenticationPrincipal Long memberId) {
        WaitingStatusResponse response = waitingService.getStatus(gameId, memberId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
    // DELETE /api/games/{gameId}/waiting/leave
    //   @AuthenticationPrincipal Long memberId
    //   → waitingService.leaveQueue(gameId, memberId)
    //   → 200 OK
    @DeleteMapping("/leave")
    public ResponseEntity<ApiResponse<Void>> leave(
            @PathVariable Long gameId,
            @AuthenticationPrincipal Long memberId
    ) {
        waitingService.leaveQueue(gameId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
