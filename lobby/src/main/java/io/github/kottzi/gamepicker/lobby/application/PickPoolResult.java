package io.github.kottzi.gamepicker.lobby.application;

import io.github.kottzi.gamepicker.catalog.domain.model.Game;

import java.util.List;

public record PickPoolResult(
        List<Game> games,
        List<Long> membersWithPrivateProfileUserIds
) {
}
