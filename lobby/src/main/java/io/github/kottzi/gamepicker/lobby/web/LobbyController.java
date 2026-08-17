package io.github.kottzi.gamepicker.lobby.web;

import io.github.kottzi.gamepicker.auth.application.CurrentUserResolver;
import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.lobby.application.LobbyService;
import io.github.kottzi.gamepicker.lobby.application.PickPoolResult;
import io.github.kottzi.gamepicker.lobby.domain.model.GameMatch;
import io.github.kottzi.gamepicker.lobby.domain.model.Lobby;
import io.github.kottzi.gamepicker.lobby.domain.model.LobbyMatchSnapshot;
import io.github.kottzi.gamepicker.lobby.domain.repository.LobbyMemberRepository;
import io.github.kottzi.gamepicker.lobby.web.dto.AddPickRequest;
import io.github.kottzi.gamepicker.lobby.web.dto.LobbyMemberView;
import io.github.kottzi.gamepicker.lobby.web.dto.LobbyView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {

    private static final String SESSION_COOKIE_NAME = "gp_session";

    private final LobbyService lobbyService;
    private final CurrentUserResolver currentUserResolver;
    private final LobbyMemberRepository lobbyMemberRepository;
    private final AppUserRepository appUserRepository;

    public LobbyController(
            LobbyService lobbyService,
            CurrentUserResolver currentUserResolver,
            LobbyMemberRepository lobbyMemberRepository,
            AppUserRepository appUserRepository
    ) {
        this.lobbyService = lobbyService;
        this.currentUserResolver = currentUserResolver;
        this.lobbyMemberRepository = lobbyMemberRepository;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping
    public LobbyView create(@CookieValue(name = SESSION_COOKIE_NAME, required = false) String token) {
        AppUser user = requireUser(token);
        return toView(lobbyService.createLobby(user.id()));
    }

    @PostMapping("/join/{inviteCode}")
    public LobbyView join(
            @PathVariable String inviteCode,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        return toView(lobbyService.joinLobby(inviteCode, user.id()));
    }

    @GetMapping("/{lobbyId}")
    public LobbyView get(@PathVariable Long lobbyId) {
        return toView(lobbyService.getLobby(lobbyId));
    }

    @PostMapping("/{lobbyId}/voting/start")
    public LobbyView startVoting(
            @PathVariable Long lobbyId,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        return toView(lobbyService.startVoting(lobbyId, user.id()));
    }

    @GetMapping("/{lobbyId}/pool")
    public PickPoolResult pool(
            @PathVariable Long lobbyId,
            @RequestParam(required = false) Long[] genreIds,
            @RequestParam(required = false) Boolean isFree
    ) {
        return lobbyService.getPickPool(lobbyId, genreIds, isFree);
    }

    @GetMapping("/{lobbyId}/picks/mine")
    public List<Long> myPicks(
            @PathVariable Long lobbyId,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        return lobbyService.getMyPickedGameIds(lobbyId, user.id());
    }

    @PostMapping("/{lobbyId}/picks")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPick(
            @PathVariable Long lobbyId,
            @Valid @RequestBody AddPickRequest request,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        lobbyService.addPick(lobbyId, user.id(), request.gameId());
    }

    @DeleteMapping("/{lobbyId}/picks/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePick(
            @PathVariable Long lobbyId,
            @PathVariable Long gameId,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        lobbyService.removePick(lobbyId, user.id(), gameId);
    }

    @PostMapping("/{lobbyId}/ready")
    public LobbyView setReady(
            @PathVariable Long lobbyId,
            @RequestParam(defaultValue = "true") boolean ready,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        lobbyService.setReady(lobbyId, user.id(), ready);
        return toView(lobbyService.getLobby(lobbyId));
    }

    @PostMapping("/{lobbyId}/close")
    public List<GameMatch> close(
            @PathVariable Long lobbyId,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        return lobbyService.closeLobby(lobbyId, user.id());
    }

    @GetMapping("/{lobbyId}/results")
    public List<LobbyMatchSnapshot> results(@PathVariable Long lobbyId) {
        return lobbyService.getResults(lobbyId);
    }

    @DeleteMapping("/{lobbyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long lobbyId,
            @CookieValue(name = SESSION_COOKIE_NAME, required = false) String token
    ) {
        AppUser user = requireUser(token);
        lobbyService.deleteLobby(lobbyId, user.id());
    }

    private AppUser requireUser(String token) {
        return currentUserResolver.resolve(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход через Steam"));
    }

    private LobbyView toView(Lobby lobby) {
        List<LobbyMemberView> members = lobbyMemberRepository.findAllByLobbyId(lobby.id()).stream()
                .map(member -> appUserRepository.findById(member.userId())
                        .map(user -> new LobbyMemberView(user.id(), user.displayName(), user.avatarUrl(), user.profilePublic(), member.ready()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return new LobbyView(lobby.id(), lobby.inviteCode(), lobby.status(), lobby.hostUserId(), members);
    }
}