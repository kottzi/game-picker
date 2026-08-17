package io.github.kottzi.gamepicker.steam.dto;

import java.time.LocalDate;
import java.util.List;

public record SteamAppDetails(
        long appId,
        String name,
        boolean isFree,
        LocalDate releaseDate,
        String headerImage,
        List<String> genreNames
) {
}
