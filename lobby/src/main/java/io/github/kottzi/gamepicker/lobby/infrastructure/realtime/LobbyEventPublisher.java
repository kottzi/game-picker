package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class LobbyEventPublisher {

    static final String CHANNEL_PREFIX = "lobby:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LobbyEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(LobbyEventMessage event) {
        String json = objectMapper.writeValueAsString(event);
        redisTemplate.convertAndSend(CHANNEL_PREFIX + event.lobbyId(), json);
    }
}
