package io.github.kottzi.gamepicker.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.catalog.infrastructure.sync.GameCatalogWriter;
import io.github.kottzi.gamepicker.steam.SteamWebApiClient;
import io.github.kottzi.gamepicker.steam.dto.SteamOwnedGame;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamLibrarySyncServiceTest {

    @Mock
    private SteamWebApiClient steamWebApiClient;
    @Mock
    private GameCatalogWriter catalogWriter;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private SteamLibrarySyncService service;

    private static final String STEAM_ID = "76561198000000001";

    @BeforeEach
    void setUp() {
        this.service = new SteamLibrarySyncService(steamWebApiClient, catalogWriter, appUserRepository, jdbcTemplate);
    }

    @Test
    void privateLibrary_marksProfileAsNotPublic() {
        AppUser user = new AppUser(1L, STEAM_ID, "Nick", null, true, Instant.now(), Instant.now());
        when(steamWebApiClient.getOwnedGames(STEAM_ID)).thenReturn(Optional.empty());

        service.syncLibrary(user);

        verify(appUserRepository).save(argThat(saved -> !saved.profilePublic()));
        verifyNoInteractions(catalogWriter);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void privateLibrary_alreadyMarkedPrivate_doesNotResave() {
        AppUser user = new AppUser(1L, STEAM_ID, "Nick", null, false, Instant.now(), Instant.now());
        when(steamWebApiClient.getOwnedGames(STEAM_ID)).thenReturn(Optional.empty());

        service.syncLibrary(user);

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void publicButEmptyLibrary_upsertsNothingButFixesFlagIfNeeded() {
        AppUser user = new AppUser(1L, STEAM_ID, "Nick", null, false, Instant.now(), Instant.now());
        when(steamWebApiClient.getOwnedGames(STEAM_ID)).thenReturn(Optional.of(List.of()));

        service.syncLibrary(user);

        verify(appUserRepository).save(argThat(AppUser::profilePublic));
        verify(catalogWriter).upsertBareGames(List.of());
    }

    @Test
    void publicLibraryWithGames_upsertsGamesAndReplacesOwnership() {
        AppUser user = new AppUser(1L, STEAM_ID, "Nick", null, true, Instant.now(), Instant.now());
        List<SteamOwnedGame> games = List.of(
                new SteamOwnedGame(100, "Portal 2", 500),
                new SteamOwnedGame(200, "Left 4 Dead 2", 300)
        );
        when(steamWebApiClient.getOwnedGames(STEAM_ID)).thenReturn(Optional.of(games));

        service.syncLibrary(user);

        verify(appUserRepository, never()).save(any());
        verify(catalogWriter).upsertBareGames(argThat(entries -> entries.size() == 2));
        verify(jdbcTemplate).update(eq("DELETE FROM user_owned_games WHERE user_id = ?"), eq(1L));
        verify(jdbcTemplate).batchUpdate(anyString(), argThat((List<Object[]> args) -> args.size() == 2));
    }
}
