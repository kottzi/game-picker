package io.github.kottzi.gamepicker.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamAppListResponse(SteamAppListWrapper applist) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SteamAppListWrapper(List<SteamAppListEntry> apps) {
    }
}
