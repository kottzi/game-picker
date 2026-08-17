package io.github.kottzi.gamepicker.lobby.web.dto;

import jakarta.validation.constraints.NotNull;

public record AddPickRequest(
        @NotNull Long gameId
) {
}
