package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LobbyEventMessageSerializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void roundTripPreservesAllFields() {
        LobbyEventMessage original = LobbyEventMessage.of(
                LobbyEventType.PICK_ADDED,
                42L,
                Map.of("userId", 7, "gameId", 100)
        );

        String json = objectMapper.writeValueAsString(original);
        LobbyEventMessage restored = objectMapper.readValue(json, LobbyEventMessage.class);

        assertEquals(original.type(), restored.type());
        assertEquals(original.lobbyId(), restored.lobbyId());
        assertEquals(original.occurredAt(), restored.occurredAt());
        assertEquals(original.payload().get("userId"), restored.payload().get("userId"));
        assertEquals(original.payload().get("gameId"), restored.payload().get("gameId"));
    }
}
