package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import java.time.Instant;
import java.util.Map;

public record LobbyEventMessage(
        LobbyEventType type,
        Long lobbyId,
        Instant occurredAt,
        Map<String, Object> payload
) {
    public static LobbyEventMessage of(LobbyEventType type, Long lobbyId, Map<String, Object> payload) {
        return new LobbyEventMessage(type, lobbyId, Instant.now(), payload);
    }
}
