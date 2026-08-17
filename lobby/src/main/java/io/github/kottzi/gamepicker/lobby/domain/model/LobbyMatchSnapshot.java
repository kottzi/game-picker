package io.github.kottzi.gamepicker.lobby.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("lobby_match_snapshots")
public record LobbyMatchSnapshot(
        @Id Long id,
        Long lobbyId,
        Long gameId,
        int pickCount,
        BigDecimal matchPercentage,
        short rankPosition,
        Instant computedAt
) {
    public static LobbyMatchSnapshot fromMatch(Long lobbyId, GameMatch match) {
        return new LobbyMatchSnapshot(
                null,
                lobbyId,
                match.gameId(),
                match.pickCount(),
                match.matchPercentage(),
                (short) match.rank(),
                Instant.now()
        );
    }
}
