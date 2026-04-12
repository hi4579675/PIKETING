package com.hn.ticketing.game.infrastructure;

import com.hn.ticketing.game.domain.Seat;
import com.hn.ticketing.game.domain.SeatRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSeatRepository extends SeatRepository, JpaRepository<Seat, Long> {

    // findBySectionId → WHERE section_id = ?
    @Override
    List<Seat> findBySectionId(Long sectionId);
}
