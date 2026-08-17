package io.github.kottzi.gamepicker.auth.application;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.steam.SteamWebApiClient;
import io.github.kottzi.gamepicker.steam.dto.SteamPlayerSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import io.github.kottzi.gamepicker.auth.infrastructure.SessionService;

@Service
public class SteamAuthService {

    private static final Logger log = LoggerFactory.getLogger(SteamAuthService.class);

    private final AppUserRepository appUserRepository;
    private final SteamWebApiClient steamWebApiClient;
    private final SessionService sessionService;
    private final SteamLibrarySyncService librarySyncService;

    public SteamAuthService(
            AppUserRepository appUserRepository,
            SteamWebApiClient steamWebApiClient,
            SessionService sessionService,
            SteamLibrarySyncService librarySyncService
    ) {
        this.appUserRepository = appUserRepository;
        this.steamWebApiClient = steamWebApiClient;
        this.sessionService = sessionService;
        this.librarySyncService = librarySyncService;
    }

    public String loginOrRegister(String steamId64) {
        Optional<SteamPlayerSummary> summary = steamWebApiClient.getPlayerSummary(steamId64);

        AppUser user = appUserRepository.findBySteamId(steamId64)
                .map(existing -> refreshFromSummary(existing, summary))
                .orElseGet(() -> createFromSummary(steamId64, summary));
        AppUser savedUser = appUserRepository.save(user);

        try {
            librarySyncService.syncLibrary(savedUser);
        } catch (Exception e) {
            log.warn("Не удалось синхронизировать библиотеку для {}: {}", steamId64, e.getMessage());
        }

        return sessionService.createSession(savedUser.id());
    }

    private AppUser createFromSummary(String steamId64, Optional<SteamPlayerSummary> summary) {
        String fallbackName = "Player " + steamId64.substring(Math.max(0, steamId64.length() - 6));
        String displayName = summary.map(SteamPlayerSummary::personaName)
                .orElse(fallbackName);
        String avatarUrl = summary.map(SteamPlayerSummary::avatarFull)
                .orElse(null);
        boolean profilePublic = summary.map(SteamPlayerSummary::isProfilePublic)
                .orElse(true);

        return new AppUser(null, steamId64, displayName, avatarUrl, profilePublic, Instant.now(), Instant.now());
    }

    private AppUser refreshFromSummary(AppUser existing, Optional<SteamPlayerSummary> summary) {
        if (summary.isEmpty()) {
            return existing.withLoginRefresh(existing.displayName(), existing.avatarUrl(), existing.profilePublic());
        }
        SteamPlayerSummary s = summary.get();

        return existing.withLoginRefresh(s.personaName(), s.avatarFull(), s.isProfilePublic());
    }
}
