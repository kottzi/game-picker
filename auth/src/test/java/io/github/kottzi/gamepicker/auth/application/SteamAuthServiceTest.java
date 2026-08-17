package io.github.kottzi.gamepicker.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.steam.SteamWebApiClient;
import io.github.kottzi.gamepicker.steam.dto.SteamPlayerSummary;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.kottzi.gamepicker.auth.infrastructure.SessionService;

@ExtendWith(MockitoExtension.class)
class SteamAuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private SteamWebApiClient steamWebApiClient;
    @Mock
    private SessionService sessionService;
    @Mock
    private SteamLibrarySyncService librarySyncService;

    private SteamAuthService service;

    private static final String STEAM_ID = "76561198000000001";

    @BeforeEach
    void setUp() {
        service = new SteamAuthService(appUserRepository, steamWebApiClient, sessionService, librarySyncService);
    }

    @Test
    void newUser_createdWithDataFromSteamSummary() {
        when(appUserRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
        when(steamWebApiClient.getPlayerSummary(STEAM_ID)).thenReturn(Optional.of(
                new SteamPlayerSummary(STEAM_ID, "CoolNick", "https://avatar.url/a.jpg", 3)
        ));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser passed = invocation.getArgument(0);
            return new AppUser(1L, passed.steamId(), passed.displayName(), passed.avatarUrl(),
                    passed.profilePublic(), passed.createdAt(), passed.lastLoginAt());
        });
        when(sessionService.createSession(1L)).thenReturn("token-abc");

        String token = service.loginOrRegister(STEAM_ID);

        assertEquals("token-abc", token);
        verify(appUserRepository).save(argThat(user ->
                user.steamId().equals(STEAM_ID)
                        && user.displayName().equals("CoolNick")
                        && user.profilePublic()
        ));
        verify(librarySyncService).syncLibrary(any(AppUser.class));
    }

    @Test
    void librarySyncFailure_doesNotBreakLogin() {
        when(appUserRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
        when(steamWebApiClient.getPlayerSummary(STEAM_ID)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser passed = invocation.getArgument(0);
            return new AppUser(9L, passed.steamId(), passed.displayName(), passed.avatarUrl(),
                    passed.profilePublic(), passed.createdAt(), passed.lastLoginAt());
        });
        doThrow(new RuntimeException("Steam недоступен")).when(librarySyncService).syncLibrary(any());
        when(sessionService.createSession(9L)).thenReturn("token-resilient");

        String token = service.loginOrRegister(STEAM_ID);

        assertEquals("token-resilient", token);
    }

    @Test
    void newUser_withoutApiKeyConfigured_fallsBackToPlaceholderName() {
        when(appUserRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
        when(steamWebApiClient.getPlayerSummary(STEAM_ID)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser passed = invocation.getArgument(0);
            return new AppUser(2L, passed.steamId(), passed.displayName(), passed.avatarUrl(),
                    passed.profilePublic(), passed.createdAt(), passed.lastLoginAt());
        });
        when(sessionService.createSession(2L)).thenReturn("token-def");

        service.loginOrRegister(STEAM_ID);

        verify(appUserRepository).save(argThat(u ->
                u.displayName().startsWith("Player") && u.profilePublic()));
    }

    @Test
    void existingUser_displayNameRefreshedFromSteam() {
        AppUser existing = new AppUser(5L, STEAM_ID, "OldNick", "old.jpg", true, Instant.now(), Instant.now());
        when(appUserRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.of(existing));
        when(steamWebApiClient.getPlayerSummary(STEAM_ID)).thenReturn(Optional.of(
                new SteamPlayerSummary(STEAM_ID, "NewNick", "new.jpg", 1)
        ));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionService.createSession(5L)).thenReturn("token-ghi");

        service.loginOrRegister(STEAM_ID);

        verify(appUserRepository).save(argThat(user ->
                user.displayName().equals("NewNick") && !user.profilePublic()));
    }
}
