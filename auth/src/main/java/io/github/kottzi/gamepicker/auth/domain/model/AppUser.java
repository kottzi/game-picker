package io.github.kottzi.gamepicker.auth.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("app_users")
public record AppUser(
        @Id Long id,
        String steamId,
        String displayName,
        String avatarUrl,
        boolean profilePublic,
        Instant createdAt,
        Instant lastLoginAt
) {
    public AppUser withLoginRefresh(
            String displayName,
            String avatarUrl,
            boolean profilePublic
    ) {
        return new AppUser(
                id,
                steamId,
                displayName,
                avatarUrl,
                profilePublic,
                createdAt,
                Instant.now()
        );
    }
}
