package io.github.kottzi.gamepicker.lobby.web.dto;

import io.github.kottzi.gamepicker.lobby.domain.model.LobbyStatus;

import java.util.List;

public record LobbyView(
        Long id,
        String inviteCode,
        LobbyStatus status,
        Long hostUserId,
        List<LobbyMemberView> members
) {
}
