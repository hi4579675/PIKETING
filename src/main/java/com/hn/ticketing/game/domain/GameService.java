package com.hn.ticketing.game.domain;

import com.hn.ticketing.game.api.dto.GameListResponse;
import com.hn.ticketing.game.api.dto.GameResponse;
import com.hn.ticketing.game.api.dto.SeatLayoutResponse;
import com.hn.ticketing.game.domain.exception.GameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// ── 사용자용 경기 서비스 ──
// 경기 조회, 좌석 배치도 조회.
// 데이터를 변경하지 않으므로 클래스 레벨에 readOnly = true.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {
    private final GameRepository gameRepository;
    private final GameSeatRepository gameSeatRepository;

    // ── 경기 목록 조회 ──
    // date 파라미터가 있으면 해당 날짜, 없으면 전체.
    public List<GameListResponse> getGames(LocalDate date) {
        List<Game> games;
        if (date != null) {
            games = gameRepository.findByGameDate(date);
        } else {
            games = gameRepository.findAll();
        }
        return games.stream()
                .map(GameListResponse::from)
                .toList();
    }
    // ── 경기 상세 조회 ──
    public GameResponse getGame(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
        return GameResponse.from(game);
    }

    // ── 좌석 배치도 조회 ──
    // 특정 경기의 전체 좌석 + 상태.
    // 성능 목표: 3,000 req/s, p99 200ms 이하.
    //
    // 현재는 MySQL에서만 조회.
    // 3단계에서 Redis 점유 상태를 합치는 로직이 추가된다.
    // 지금은 GameSeat.status만으로 응답.
    public List<SeatLayoutResponse> getSeatLayout(Long gameId) {
        // 경기 존재 여부만 먼저 확인. 없으면 404.
        gameRepository.findById(gameId)
                .orElseThrow(GameNotFoundException::new);
        List<GameSeat> gameSeats = gameSeatRepository.findByGameId(gameId);

        return gameSeats.stream()
                .map(SeatLayoutResponse::from)
                .toList();
    }


}
