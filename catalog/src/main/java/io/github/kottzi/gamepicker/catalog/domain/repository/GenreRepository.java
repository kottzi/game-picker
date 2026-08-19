package io.github.kottzi.gamepicker.catalog.domain.repository;

import io.github.kottzi.gamepicker.catalog.domain.model.Genre;
import org.springframework.data.repository.CrudRepository;

public interface GenreRepository extends CrudRepository<Genre, Long> {
}
