package io.github.kottzi.gamepicker.catalog.domain.repository;

import io.github.kottzi.gamepicker.catalog.domain.model.Genre;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface GenreRepository extends CrudRepository<Genre, Long> {

    Optional<Genre> findByNameIgnoreCase(String name);
}
