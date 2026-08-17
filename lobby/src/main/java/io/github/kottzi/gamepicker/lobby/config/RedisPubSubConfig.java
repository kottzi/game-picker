package io.github.kottzi.gamepicker.lobby.config;

import io.github.kottzi.gamepicker.lobby.infrastructure.realtime.LobbyEventRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    private static final String LOBBY_CHANNEL_PATTERN = "lobby:*";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            LobbyEventRedisSubscriber subscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(LOBBY_CHANNEL_PATTERN));
        return container;
    }
}
