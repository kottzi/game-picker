package io.github.kottzi.gamepicker.steam;

import io.github.kottzi.gamepicker.steam.dto.SteamAppDetails;
import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SteamStoreClient {

    private static final Logger log = LoggerFactory.getLogger(SteamStoreClient.class);
    private static final String APP_DETAILS_URL = "https://store.steampowered.com/api/appdetails";

    private final RestClient restClient;

    public SteamStoreClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public Optional<SteamAppDetails> fetchAppDetails(long appId) {
        try {
            JsonNode root = restClient.get()
                    .uri(APP_DETAILS_URL + "?appids={appId}", appId)
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            JsonNode entry = root.get(String.valueOf(appId));
            if (entry == null || !entry.path("success").asBoolean(false)) {
                return Optional.empty();
            }
            JsonNode data = entry.get("data");
            if (data == null || !"game".equals(data.path("type").asText(""))) {
                return Optional.empty();
            }
            List<String> genreNames = new ArrayList<>();
            for (JsonNode genre : data.path("genres")) {
                String description = genre.path("description").asText(null);
                if (description != null) {
                    genreNames.add(description);
                }
            }

            LocalDate releaseDate = ReleaseDateParser.parse(data.path("release_date").path("date").asText(null));
            return Optional.of(new SteamAppDetails(
                    appId,
                    data.path("name").asText(""),
                    data.path("is_free").asBoolean(false),
                    releaseDate,
                    data.path("header_image").asText(null),
                    genreNames
            ));
        } catch (Exception e) {
            log.warn("appdetails недоступен для appid {}: {}", appId, e.getMessage());
            return Optional.empty();
        }
    }
}
