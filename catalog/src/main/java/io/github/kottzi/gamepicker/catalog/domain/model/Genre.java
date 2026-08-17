package io.github.kottzi.gamepicker.catalog.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("genres")
public record Genre(
        @Id Long id,
        String name
) {
}
