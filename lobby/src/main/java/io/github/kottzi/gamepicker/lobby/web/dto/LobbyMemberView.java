package io.github.kottzi.gamepicker.lobby.web.dto;

public record LobbyMemberView(
        Long userId,
        String displayName,
        String avatarUrl,
        boolean profilePublic,
        boolean ready
) {
}