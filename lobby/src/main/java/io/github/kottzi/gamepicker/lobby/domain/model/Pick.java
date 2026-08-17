package io.github.kottzi.gamepicker.lobby.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("picks")
public record Pick(
        @Id Long id,
        Long lobbyId,
        Long userId,
        Long gameId,
        Instant pickedAt
) {
}
