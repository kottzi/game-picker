package io.github.kottzi.gamepicker.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamAppListEntry(
        @JsonProperty("appid") long appId,
        @JsonProperty("name") String name
) {
}
