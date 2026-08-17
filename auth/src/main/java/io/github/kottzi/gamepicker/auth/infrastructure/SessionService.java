package io.github.kottzi.gamepicker.auth.infrastructure;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Service
public class SessionService {

    private static final String KEY_PREFIX = "session:";
    private static final Duration SESSION_TTL = Duration.ofDays(30);

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;

    public SessionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createSession(Long userId) {
        String token = generateToken();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, String.valueOf(userId), SESSION_TTL);
        return token;
    }

    public Optional<Long> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            return Optional.empty();
        }
        redisTemplate.expire(KEY_PREFIX + token, SESSION_TTL);
        return Optional.of(Long.parseLong(value));
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            redisTemplate.delete(KEY_PREFIX + token);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
