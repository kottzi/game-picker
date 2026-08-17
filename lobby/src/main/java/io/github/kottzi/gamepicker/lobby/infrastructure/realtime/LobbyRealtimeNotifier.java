package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import io.github.kottzi.gamepicker.lobby.domain.model.GameMatch;
import io.github.kottzi.gamepicker.lobby.domain.model.LobbyStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class LobbyRealtimeNotifier {

    private final LobbyEventPublisher publisher;

    public LobbyRealtimeNotifier(LobbyEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void memberJoined(Long lobbyId, Long userId, String displayName) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.MEMBER_JOINED, lobbyId,
                Map.of("userId", userId, "displayName", displayName)));
    }

    public void memberLeft(Long lobbyId, Long userId) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.MEMBER_LEFT, lobbyId,
                Map.of("userId", userId)));
    }

    public void pickAdded(Long lobbyId, Long userId, Long gameId) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.PICK_ADDED, lobbyId,
                Map.of("userId", userId, "gameId", gameId)));
    }

    public void pickRemoved(Long lobbyId, Long userId, Long gameId) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.PICK_REMOVED, lobbyId,
                Map.of("userId", userId, "gameId", gameId)));
    }

    public void readyChanged(Long lobbyId) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.READY_CHANGED, lobbyId, Map.of()));
    }

    public void statusChanged(Long lobbyId, LobbyStatus newStatus) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.LOBBY_STATUS_CHANGED, lobbyId,
                Map.of("status", newStatus)));
    }

    public void matchComputed(Long lobbyId, List<GameMatch> matches) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.MATCH_COMPUTED, lobbyId,
                Map.of("matches", matches)));
    }

    public void lobbyDeleted(Long lobbyId) {
        publisher.publish(LobbyEventMessage.of(LobbyEventType.LOBBY_DELETED, lobbyId, Map.of()));
    }
}