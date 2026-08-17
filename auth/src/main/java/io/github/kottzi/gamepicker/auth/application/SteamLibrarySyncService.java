package io.github.kottzi.gamepicker.auth.application;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.catalog.infrastructure.sync.GameCatalogWriter;
import io.github.kottzi.gamepicker.steam.SteamWebApiClient;
import io.github.kottzi.gamepicker.steam.dto.SteamAppListEntry;
import io.github.kottzi.gamepicker.steam.dto.SteamOwnedGame;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SteamLibrarySyncService {

    private final SteamWebApiClient steamWebApiClient;
    private final GameCatalogWriter catalogWriter;
    private final AppUserRepository appUserRepository;
    private final JdbcTemplate jdbcTemplate;

    public SteamLibrarySyncService(
            SteamWebApiClient steamWebApiClient,
            GameCatalogWriter catalogWriter,
            AppUserRepository appUserRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.steamWebApiClient = steamWebApiClient;
        this.catalogWriter = catalogWriter;
        this.appUserRepository = appUserRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void syncLibrary(AppUser user) {
        Optional<List<SteamOwnedGame>> ownedGames = steamWebApiClient.getOwnedGames(user.steamId());

        if (ownedGames.isEmpty()) {
            if (user.profilePublic()) {
                appUserRepository.save(user.withLoginRefresh(user.displayName(), user.avatarUrl(), false));
            }
            return;
        }
        if (!user.profilePublic()) {
            appUserRepository.save(user.withLoginRefresh(user.displayName(), user.avatarUrl(), true));
        }

        List<SteamOwnedGame> games = ownedGames.get();
        catalogWriter.upsertBareGames(games.stream()
                .map(game -> new SteamAppListEntry(game.appId(), game.name()))
                .toList()
        );
        replaceOwnedGames(user.id(), games);
    }

    private void replaceOwnedGames(Long userId, List<SteamOwnedGame> games) {
        jdbcTemplate.update("DELETE FROM user_owned_games WHERE user_id = ?", userId);
        if (games.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO user_owned_games (user_id, game_id, playtime_minutes, synced_at)
                VALUES (?, ?, ?, now())
                """;
        List<Object[]> args = games.stream()
                .map(game -> new Object[]{userId, game.appId(), game.playtimeForeverMinutes()})
                .toList();
        jdbcTemplate.batchUpdate(sql, args);
    }
}
