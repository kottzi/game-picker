package io.github.kottzi.gamepicker.catalog.domain.repository;

import org.springframework.data.repository.CrudRepository;
import io.github.kottzi.gamepicker.catalog.domain.model.Game;

public interface GameRepository extends CrudRepository<Game, Long> {
}
