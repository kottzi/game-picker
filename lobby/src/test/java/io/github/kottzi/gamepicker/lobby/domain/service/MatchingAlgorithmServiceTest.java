package io.github.kottzi.gamepicker.lobby.domain.service;

import io.github.kottzi.gamepicker.lobby.domain.model.GameMatch;
import io.github.kottzi.gamepicker.lobby.domain.model.Pick;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingAlgorithmServiceTest {

    private final MatchingAlgorithmService service = new MatchingAlgorithmService();

    private Pick pick(long lobbyId, long userId, long gameId) {
        return new Pick(null, lobbyId, userId, gameId, Instant.now());
    }

    @Test
    void singleGamePickedByEveryone_returnsHundredPercent() {
        List<Pick> picks = List.of(pick(1, 1, 100), pick(1, 2, 100), pick(1, 3, 100));
        Map<Long, String> names = Map.of(100L, "Portal 2");

        List<GameMatch> matches = service.computeMatches(picks, names, 3);

        assertEquals(1, matches.size());
        assertEquals(0, BigDecimal.valueOf(100.00).compareTo(matches.getFirst().matchPercentage()));
        assertEquals(1, matches.getFirst().rank());
    }

    @Test
    void twoDistinctGames_returnsBothWithoutTruncation() {
        List<Pick> picks = List.of(
                pick(1, 1, 100), pick(1, 2, 100),
                pick(1, 1, 200), pick(1, 3, 200)
        );
        Map<Long, String> names = Map.of(100L, "Portal 2", 200L, "Left 4 Dead 2");

        List<GameMatch> matches = service.computeMatches(picks, names, 3);

        assertEquals(2, matches.size());
    }

    @Test
    void threeOrMoreDistinctGames_returnsOnlyTop3() {
        List<Pick> picks = List.of(
                pick(1, 1, 100), pick(1, 2, 100), pick(1, 3, 100), pick(1, 4, 100),
                pick(1, 1, 200), pick(1, 2, 200), pick(1, 3, 200),
                pick(1, 1, 300), pick(1, 2, 300),
                pick(1, 1, 400)
        );
        Map<Long, String> names = Map.of(100L, "A", 200L, "B", 300L, "C", 400L, "D");

        List<GameMatch> matches = service.computeMatches(picks, names, 4);

        assertEquals(3, matches.size());
        assertEquals(100L, matches.get(0).gameId());
        assertEquals(200L, matches.get(1).gameId());
        assertEquals(300L, matches.get(2).gameId());
    }

    @Test
    void emptyPicks_returnsEmptyList() {
        assertEquals(List.of(), service.computeMatches(List.of(), Map.of(), 5));
    }
}
