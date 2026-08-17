package io.github.kottzi.gamepicker.lobby.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("lobbies")
public record Lobby(
        @Id Long id,
        String inviteCode,
        Long hostUserId,
        LobbyStatus status,
        Instant createdAt,
        Instant closedAt
) {
    public Lobby withStatus(LobbyStatus newStatus) {
        return new Lobby(id, inviteCode, hostUserId, newStatus, createdAt, closedAt);
    }

    public Lobby withClosedStatus(Instant closedAtValue) {
        return new Lobby(id, inviteCode, hostUserId, LobbyStatus.CLOSED, createdAt, closedAtValue);
    }
}
