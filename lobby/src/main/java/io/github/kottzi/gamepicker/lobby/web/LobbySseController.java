package io.github.kottzi.gamepicker.lobby.web;

import io.github.kottzi.gamepicker.auth.application.CurrentUserResolver;
import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.lobby.domain.repository.LobbyMemberRepository;
import io.github.kottzi.gamepicker.lobby.infrastructure.realtime.LobbySseRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/lobbies")
public class LobbySseController {

    private static final String SESSION_COOKIE_NAME = "gp_session";

    private final LobbySseRegistry registry;
    private final CurrentUserResolver currentUserResolver;
    private final LobbyMemberRepository lobbyMemberRepository;

    public LobbySseController(
            LobbySseRegistry registry,
            CurrentUserResolver currentUserResolver,
            LobbyMemberRepository lobbyMemberRepository
    ) {
        this.registry = registry;
        this.currentUserResolver = currentUserResolver;
        this.lobbyMemberRepository = lobbyMemberRepository;
    }

    @GetMapping(path = "/{lobbyId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable Long lobbyId,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = currentUserResolver.resolve(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход через Steam"));
        if (!lobbyMemberRepository.existsByLobbyIdAndUserId(lobbyId, user.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не состоите в этом лобби");
        }
        return registry.subscribe(lobbyId);
    }
}
