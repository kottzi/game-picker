package io.github.kottzi.gamepicker.lobby.domain.service;

import io.github.kottzi.gamepicker.lobby.domain.model.GameMatch;
import io.github.kottzi.gamepicker.lobby.domain.model.Pick;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchingAlgorithmService {

    private static final int TOP_RESULTS_LIMIT = 3;

    public List<GameMatch> computeMatches(List<Pick> picks, Map<Long, String> gameNames, int totalMembers) {
        if (picks.isEmpty() || totalMembers <= 0) {
            return List.of();
        }
        Map<Long, Long> pickCountByGame = picks.stream()
                .collect(Collectors.groupingBy(Pick::gameId, Collectors.counting()));
        List<Map.Entry<Long, Long>> rankedGames = pickCountByGame.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .toList();

        int resultSize = Math.min(rankedGames.size(), TOP_RESULTS_LIMIT);
        List<GameMatch> result = new ArrayList<>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            Map.Entry<Long, Long> entry = rankedGames.get(i);
            long gameId = entry.getKey();
            long pickCount = entry.getValue();

            BigDecimal percentage = BigDecimal.valueOf(pickCount)
                    .divide(BigDecimal.valueOf(totalMembers), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            result.add(new GameMatch(
                    gameId,
                    gameNames.getOrDefault(gameId, "Unknown game"),
                    (int) pickCount,
                    percentage,
                    i + 1
            ));
        }

        return result;
    }
}
