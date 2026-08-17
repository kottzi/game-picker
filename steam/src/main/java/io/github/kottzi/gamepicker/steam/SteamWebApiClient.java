package io.github.kottzi.gamepicker.steam;

import io.github.kottzi.gamepicker.steam.dto.SteamOwnedGame;
import io.github.kottzi.gamepicker.steam.dto.SteamOwnedGamesResponse;
import io.github.kottzi.gamepicker.steam.dto.SteamPlayerSummariesResponse;
import io.github.kottzi.gamepicker.steam.dto.SteamPlayerSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Service
public class SteamWebApiClient {

    private static final Logger log = LoggerFactory.getLogger(SteamWebApiClient.class);
    private static final String PLAYER_SUMMARIES_URL = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/";
    private static final String OWNED_GAMES_URL = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/";

    private final RestClient restClient;
    private final String apiKey;

    public SteamWebApiClient(RestClient.Builder restClientBuilder, @Value("${steam.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
    }

    public Optional<SteamPlayerSummary> getPlayerSummary(String steamId64) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            SteamPlayerSummariesResponse response = restClient.get()
                    .uri(PLAYER_SUMMARIES_URL + "?key={key}&steamids={steamIds}", apiKey, steamId64)
                    .retrieve()
                    .body(SteamPlayerSummariesResponse.class);
            if (response == null || response.response() == null || response.response().players().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(response.response().players().getFirst());
        } catch (Exception e) {
            log.warn("Steam GetPlayerSummaries недоступен для {}: {}", steamId64, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<List<SteamOwnedGame>> getOwnedGames(String steamId64) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            SteamOwnedGamesResponse response = restClient.get()
                    .uri(OWNED_GAMES_URL + "?key={key}&steamid={steamId}&include_appinfo=true&include_played_free_games=true&format=json", apiKey, steamId64)
                    .retrieve()
                    .body(SteamOwnedGamesResponse.class);
            if (response == null || response.response() == null || response.response().games() == null) {
                return Optional.empty();
            }
            return Optional.of(response.response().games());
        } catch (Exception e) {
            log.warn("Steam GetOwnedGames недоступен для {}: {}", steamId64, e.getMessage());
            return Optional.empty();
        }
    }
}
