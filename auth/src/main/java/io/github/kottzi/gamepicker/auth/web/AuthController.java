package io.github.kottzi.gamepicker.auth.web;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.auth.infrastructure.SessionService;
import io.github.kottzi.gamepicker.auth.application.SteamAuthService;
import io.github.kottzi.gamepicker.steam.SteamOpenIdService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_COOKIE_NAME = "gp_session";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(30);

    private final SteamOpenIdService openIdService;
    private final SteamAuthService steamAuthService;
    private final SessionService sessionService;
    private final AppUserRepository appUserRepository;
    private final String baseUrl;

    public AuthController(
            SteamOpenIdService openIdService,
            SteamAuthService steamAuthService,
            SessionService sessionService,
            AppUserRepository appUserRepository,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.openIdService = openIdService;
        this.steamAuthService = steamAuthService;
        this.sessionService = sessionService;
        this.appUserRepository = appUserRepository;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/steam/login")
    public ResponseEntity<Void> login() {
        String returnTo = baseUrl + "/api/auth/steam/callback";
        String redirectUrl = openIdService.buildLoginRedirectUrl(returnTo, baseUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/steam/callback")
    public ResponseEntity<Void> callback(@RequestParam Map<String, String> allParams, HttpServletResponse response) {
        Optional<String> steamId = openIdService.verifyAndExtractSteamId(allParams);
        if (steamId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = steamAuthService.loginOrRegister(steamId.get());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, COOKIE_MAX_AGE).toString());

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(baseUrl + "/"))
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token,
            HttpServletResponse response
    ) {
        sessionService.invalidate(token);
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AppUser> me(@CookieValue(name = SESSION_COOKIE_NAME, required = false) String token) {
        return sessionService.resolve(token)
                .flatMap(appUserRepository::findById)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(baseUrl.startsWith("https"))
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
