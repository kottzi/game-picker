package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LobbySseRegistryTest {

    private final LobbySseRegistry registry = new LobbySseRegistry();

    @Test
    void subscribeReturnsUsableEmitter() {
        SseEmitter emitter = registry.subscribe(1L);
        assertNotNull(emitter);
    }

    @Test
    void broadcastToLobbyWithoutSubscribersDoesNotThrow() {
        LobbyEventMessage event = LobbyEventMessage.of(LobbyEventType.PICK_ADDED, 999L, Map.of());
        assertDoesNotThrow(() -> registry.broadcastLocal(999L, event));
    }

    @Test
    void broadcastAfterSubscribeDoesNotThrow() {
        registry.subscribe(2L);
        LobbyEventMessage event = LobbyEventMessage.of(LobbyEventType.MEMBER_JOINED, 2L,
                Map.of("userId", 5));
        assertDoesNotThrow(() -> registry.broadcastLocal(2L, event));
    }

    @Test
    void heartbeatDoesNotThrowWithNoSubscribers() {
        assertDoesNotThrow(registry::sendHeartbeat);
    }
}
