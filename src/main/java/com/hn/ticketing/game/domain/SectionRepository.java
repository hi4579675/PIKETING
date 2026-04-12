package com.hn.ticketing.game.domain;

import java.util.List;
import java.util.Optional;

public interface SectionRepository {
    Section save(Section section);

    Optional<Section> findById(Long id);

    // 특정 구장의 모든 구역 조회
    // 좌석 배치도 조회 시 구장 -> 구역 -> 좌석 순으로 탐색하므로 필요
    List<Section> findByStadiumId(Long stadiumId);
}
