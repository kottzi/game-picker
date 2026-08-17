package io.github.kottzi.gamepicker.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamOwnedGame(
        @JsonProperty("appid") long appId,
        @JsonProperty("name") String name,
        @JsonProperty("playtime_forever") int playtimeForeverMinutes
) {
}
