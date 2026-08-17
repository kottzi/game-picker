package io.github.kottzi.gamepicker.lobby.domain.model;

import java.math.BigDecimal;

public record GameMatch(
        Long gameId,
        String gameName,
        int pickCount,
        BigDecimal matchPercentage,
        int rank
) {
}
