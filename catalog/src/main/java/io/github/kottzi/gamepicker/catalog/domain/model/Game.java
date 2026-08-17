package io.github.kottzi.gamepicker.catalog.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

@Table("games")
public record Game(
        @Id Long id,
        String name,
        boolean isFree,
        LocalDate releaseDate,
        String headerImage,
        Instant metadataSyncedAt,
        Instant createdAt
) {
}
