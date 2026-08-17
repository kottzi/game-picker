package io.github.kottzi.gamepicker.auth.application;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import io.github.kottzi.gamepicker.auth.infrastructure.SessionService;

@Component
public class CurrentUserResolver {

    private final SessionService sessionService;
    private final AppUserRepository appUserRepository;

    public CurrentUserResolver(SessionService sessionService, AppUserRepository appUserRepository) {
        this.sessionService = sessionService;
        this.appUserRepository = appUserRepository;
    }

    public Optional<AppUser> resolve(String sessionToken) {
        return sessionService.resolve(sessionToken).flatMap(appUserRepository::findById);
    }
}
