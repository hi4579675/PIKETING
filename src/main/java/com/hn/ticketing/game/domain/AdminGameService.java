package com.hn.ticketing.game.domain;

// ── 관리자용 경기 서비스 ──
// 경기 등록, 구역 등록, 좌석 일괄 등록.
// 데이터를 변경하므로 메서드 레벨에 @Transactional.

import com.hn.ticketing.game.api.dto.CreateGameRequest;
import com.hn.ticketing.game.api.dto.CreateSectionRequest;
import com.hn.ticketing.game.domain.exception.SectionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGameService {
    private final StadiumRepository stadiumRepository;
    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;
    private final GameRepository gameRepository;
    private final GameSeatRepository gameSeatRepository;

    // ── 경기 등록 ──
    // 경기를 만들고, 해당 구장의 모든 좌석에 대해 GameSeat을 자동 생성.
    // 이 API가 있어야 테스트할 데이터가 생긴다.
    //
    // 흐름:
    //   1) Stadium 조회
    //   2) Game INSERT
    //   3) Stadium의 모든 Section 조회
    //   4) 각 Section의 모든 Seat 조회
    //   5) 각 Seat에 대해 GameSeat 생성 (등급별 기본 가격)
    //   6) GameSeat 일괄 INSERT
    @Transactional
    public Long createGame(CreateGameRequest request) {
        Stadium stadium = stadiumRepository.findById(request.stadiumId())
                .orElseThrow(() -> new IllegalArgumentException("구장을 찾을 수 없습니다"));

        Game game = Game.builder()
                .stadium(stadium)
                .title(request.title())
                .gameDate(request.gameDate())
                .startTime(request.startTime())
                .homeTeam(request.homeTeam())
                .awayTeam(request.awayTeam())
                .build();

        Game savedGame = gameRepository.save(game);

        // 구장의 모든 구역 → 모든 좌석 → GameSeat 생성
        List<Section> sections = sectionRepository.findByStadiumId(stadium.getId());
        List<GameSeat> gameSeats = new ArrayList<>();

        for (Section section : sections) {
            int price = getDefaultPrice(section.getSeatGrade());
            List<Seat> seats = seatRepository.findBySectionId(section.getId());

            for (Seat seat : seats) {
                gameSeats.add(GameSeat.createAvailable(savedGame, seat, price));
            }
        }

        gameSeatRepository.saveAll(gameSeats);
        return savedGame.getId();
    }
    // ── 구역 등록 ──
    @Transactional
    public Long createSection(CreateSectionRequest request) {
        Stadium stadium = stadiumRepository.findById(request.stadiumId())
                .orElseThrow(() -> new IllegalArgumentException("구장을 찾을 수 없습니다"));

        SeatGrade grade = SeatGrade.valueOf(request.seatGrade());

        Section section = Section.builder()
                .stadium(stadium)
                .name(request.name())
                .seatGrade(grade)
                .totalSeats(request.totalSeats())
                .build();

        Section saved = sectionRepository.save(section);
        return saved.getId();
    }

    // ── 좌석 일괄 등록 ──
    // 특정 구역에 좌석 N개를 한 번에 생성.
    // seatRow와 시작 번호, 끝 번호를 받아서 생성.
    // 예: createSeats(sectionId, "A", 1, 20) → A열 1~20번 좌석 20개
    @Transactional
    public int createSeats(Long sectionId, String seatRow, int startNumber, int endNumber) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(SectionNotFoundException::new);

        List<Seat> seats = new ArrayList<>();
        for (int i = startNumber; i <= endNumber; i++) {
            seats.add(Seat.builder()
                    .section(section)
                    .seatRow(seatRow)
                    .seatNumber(i)
                    .build());
        }

        seatRepository.saveAll(seats);
        return seats.size();
    }

    // ── 등급별 기본 가격 ──
    // 경기 등록 시 GameSeat에 넣을 기본 가격.
    // 실제 서비스면 DB에서 관리하겠지만,
    // 이 프로젝트에서는 하드코딩으로 충분.
    private int getDefaultPrice(SeatGrade grade) {
        return switch (grade) {
            case PREMIUM -> 50_000;
            case STANDARD -> 30_000;
            case OUTFIELD -> 15_000;
        };
    }
}
