package io.github.kottzi.gamepicker.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamOwnedGamesResponse(SteamOwnedGamesInner response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SteamOwnedGamesInner(
            @JsonProperty("game_count") Integer gameCount,
            List<SteamOwnedGame> games
    ) {
    }
}
