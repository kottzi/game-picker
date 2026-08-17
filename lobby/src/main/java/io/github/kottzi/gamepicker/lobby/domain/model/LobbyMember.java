package io.github.kottzi.gamepicker.lobby.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("lobby_members")
public record LobbyMember(
        @Id Long id,
        Long lobbyId,
        Long userId,
        Instant joinedAt,
        boolean ready
) {
    public LobbyMember withReady(boolean ready) {
        return new LobbyMember(
                id,
                lobbyId,
                userId,
                joinedAt,
                ready
        );
    }
}
