package com.hn.ticketing.game.infrastructure;

import com.hn.ticketing.game.domain.Section;
import com.hn.ticketing.game.domain.SectionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaSectionRepository extends SectionRepository, JpaRepository<Section, Long> {
    @Override
    List<Section> findByStadiumId(Long stadiumId);
}
