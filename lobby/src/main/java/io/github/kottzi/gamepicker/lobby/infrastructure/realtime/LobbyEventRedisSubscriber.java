package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
public class LobbyEventRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(LobbyEventRedisSubscriber.class);

    private final LobbySseRegistry sseRegistry;
    private final ObjectMapper objectMapper;

    public LobbyEventRedisSubscriber(LobbySseRegistry sseRegistry, ObjectMapper objectMapper) {
        this.sseRegistry = sseRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        if (!channel.startsWith(LobbyEventPublisher.CHANNEL_PREFIX)) {
            return;
        }
        Long lobbyId = Long.parseLong(channel.substring(LobbyEventPublisher.CHANNEL_PREFIX.length()));
        try {
            LobbyEventMessage event = objectMapper.readValue(message.getBody(), LobbyEventMessage.class);
            sseRegistry.broadcastLocal(lobbyId, event);
        } catch (JacksonException e) {
            log.warn("Не удалось десериализовать событие лобби {}: {}", lobbyId, e.getMessage());
        }
    }
}
