package com.hn.ticketing.game.domain;


import java.util.Optional;

public interface StadiumRepository {
    Stadium save(Stadium stadium);

    Optional<Stadium> findById(Long id);
}
