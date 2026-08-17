package io.github.kottzi.gamepicker.steam.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SteamPlayerSummary(
        @JsonProperty("steamid") String steamId,
        @JsonProperty("personaname") String personaName,
        @JsonProperty("avatarfull") String avatarFull,
        @JsonProperty("communityvisibilitystate") int communityVisibilityState
) {

    // 1 = Private, 2 = Friends Only, 3 = Public
    // Видимость профиля в целом
    public boolean isProfilePublic() {
        return communityVisibilityState == 3;
    }
}
