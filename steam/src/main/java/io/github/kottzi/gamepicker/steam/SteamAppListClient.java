package io.github.kottzi.gamepicker.steam;

import io.github.kottzi.gamepicker.steam.dto.SteamAppListEntry;
import io.github.kottzi.gamepicker.steam.dto.SteamAppListResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class SteamAppListClient {

    private static final String APP_LIST_URL = "https://api.steampowered.com/ISteamApps/GetAppList/v2/";

    private final RestClient restClient;

    public SteamAppListClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<SteamAppListEntry> fetchAll() {
        SteamAppListResponse response = restClient.get()
                .uri(APP_LIST_URL)
                .retrieve()
                .body(SteamAppListResponse.class);

        if (response == null || response.applist() == null || response.applist().apps() == null) {
            return List.of();
        }
        return response.applist().apps();
    }
}
