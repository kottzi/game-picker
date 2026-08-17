package io.github.kottzi.gamepicker.lobby.application;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.catalog.domain.model.Game;
import io.github.kottzi.gamepicker.catalog.domain.repository.GameRepository;
import io.github.kottzi.gamepicker.lobby.application.exception.InvalidLobbyStateException;
import io.github.kottzi.gamepicker.lobby.application.exception.LobbyNotFoundException;
import io.github.kottzi.gamepicker.lobby.application.exception.NotLobbyHostException;
import io.github.kottzi.gamepicker.lobby.application.exception.NotLobbyMemberException;
import io.github.kottzi.gamepicker.lobby.application.exception.UserNotFoundException;
import io.github.kottzi.gamepicker.lobby.domain.model.GameMatch;
import io.github.kottzi.gamepicker.lobby.domain.model.Lobby;
import io.github.kottzi.gamepicker.lobby.domain.model.LobbyMatchSnapshot;
import io.github.kottzi.gamepicker.lobby.domain.model.LobbyMember;
import io.github.kottzi.gamepicker.lobby.domain.model.LobbyStatus;
import io.github.kottzi.gamepicker.lobby.domain.model.Pick;
import io.github.kottzi.gamepicker.lobby.domain.repository.LobbyGamePoolRepository;
import io.github.kottzi.gamepicker.lobby.domain.repository.LobbyMatchSnapshotRepository;
import io.github.kottzi.gamepicker.lobby.domain.repository.LobbyMemberRepository;
import io.github.kottzi.gamepicker.lobby.domain.repository.LobbyRepository;
import io.github.kottzi.gamepicker.lobby.domain.repository.PickRepository;
import io.github.kottzi.gamepicker.lobby.domain.service.InviteCodeGenerator;
import io.github.kottzi.gamepicker.lobby.domain.service.MatchingAlgorithmService;
import io.github.kottzi.gamepicker.lobby.infrastructure.realtime.LobbyRealtimeNotifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LobbyService {

    private static final int INVITE_CODE_MAX_ATTEMPTS = 10;

    private final LobbyRepository lobbyRepository;
    private final LobbyMemberRepository lobbyMemberRepository;
    private final PickRepository pickRepository;
    private final LobbyGamePoolRepository lobbyGamePoolRepository;
    private final GameRepository gameRepository;
    private final AppUserRepository appUserRepository;
    private final LobbyMatchSnapshotRepository snapshotRepository;
    private final MatchingAlgorithmService matchingAlgorithmService;
    private final LobbyRealtimeNotifier realtimeNotifier;

    public LobbyService(
            LobbyRepository lobbyRepository,
            LobbyMemberRepository lobbyMemberRepository,
            PickRepository pickRepository,
            LobbyGamePoolRepository lobbyGamePoolRepository,
            GameRepository gameRepository,
            AppUserRepository appUserRepository,
            LobbyMatchSnapshotRepository snapshotRepository,
            MatchingAlgorithmService matchingAlgorithmService,
            LobbyRealtimeNotifier realtimeNotifier
    ) {
        this.lobbyRepository = lobbyRepository;
        this.lobbyMemberRepository = lobbyMemberRepository;
        this.pickRepository = pickRepository;
        this.lobbyGamePoolRepository = lobbyGamePoolRepository;
        this.gameRepository = gameRepository;
        this.appUserRepository = appUserRepository;
        this.snapshotRepository = snapshotRepository;
        this.matchingAlgorithmService = matchingAlgorithmService;
        this.realtimeNotifier = realtimeNotifier;
    }

    @Transactional
    public Lobby createLobby(Long hostUserId) {
        requireUserExists(hostUserId);
        String inviteCode = generateUniqueInviteCode();
        Lobby lobby = lobbyRepository.save(new Lobby(null, inviteCode, hostUserId, LobbyStatus.OPEN, Instant.now(), null));
        addMemberInternal(lobby.id(), hostUserId);

        return lobby;
    }

    @Transactional
    public Lobby joinLobby(String inviteCode, Long userId) {
        Lobby lobby = lobbyRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new LobbyNotFoundException(inviteCode));
        if (lobby.status() != LobbyStatus.OPEN) {
            throw new InvalidLobbyStateException("Лобби " + lobby.id() + " уже не принимает участников (статус " + lobby.status() + ")");
        }
        requireUserExists(userId);
        if (!lobbyMemberRepository.existsByLobbyIdAndUserId(lobby.id(), userId)) {
            addMemberInternal(lobby.id(), userId);
        }
        return lobby;
    }

    public Lobby getLobby(Long lobbyId) {
        return getLobbyOrThrow(lobbyId);
    }

    @Transactional
    public Lobby startVoting(Long lobbyId, Long requesterUserId) {
        Lobby lobby = getLobbyOrThrow(lobbyId);
        requireHost(lobby, requesterUserId);
        if (lobby.status() != LobbyStatus.OPEN) {
            throw new InvalidLobbyStateException("Лобби " + lobbyId + " не в статусе OPEN, нельзя начать голосование");
        }
        Lobby updated = lobbyRepository.save(lobby.withStatus(LobbyStatus.VOTING));
        realtimeNotifier.statusChanged(lobbyId, LobbyStatus.VOTING);
        return updated;
    }

    public PickPoolResult getPickPool(Long lobbyId, Long[] genreIds, Boolean isFree) {
        getLobbyOrThrow(lobbyId);
        List<Game> games = lobbyGamePoolRepository.findIntersectionForLobby(lobbyId, genreIds, isFree);
        List<Long> privateProfileUserIds = lobbyMemberRepository.findMembersWithPrivateProfile(lobbyId).stream()
                .map(LobbyMember::userId)
                .toList();
        return new PickPoolResult(games, privateProfileUserIds);
    }

    public List<Long> getMyPickedGameIds(Long lobbyId, Long userId) {
        return pickRepository.findAllByLobbyIdAndUserId(lobbyId, userId).stream()
                .map(Pick::gameId)
                .toList();
    }

    @Transactional
    public void addPick(Long lobbyId, Long userId, Long gameId) {
        Lobby lobby = getLobbyOrThrow(lobbyId);
        requireMember(lobby, userId);
        if (lobby.status() != LobbyStatus.VOTING) {
            throw new InvalidLobbyStateException("Пик доступен только в статусе VOTING (лобби " + lobbyId + ")");
        }
        if (!pickRepository.existsByLobbyIdAndUserIdAndGameId(lobbyId, userId, gameId)) {
            pickRepository.save(new Pick(null, lobbyId, userId, gameId, Instant.now()));
            realtimeNotifier.pickAdded(lobbyId, userId, gameId);
        }
    }

    @Transactional
    public void removePick(Long lobbyId, Long userId, Long gameId) {
        Lobby lobby = getLobbyOrThrow(lobbyId);
        requireMember(lobby, userId);
        pickRepository.deleteByLobbyIdAndUserIdAndGameId(lobbyId, userId, gameId);
        realtimeNotifier.pickRemoved(lobbyId, userId, gameId);
    }

    @Transactional
    public void setReady(Long lobbyId, Long userId, boolean ready) {
        Lobby lobby = getLobbyOrThrow(lobbyId);
        requireMember(lobby, userId);
        if (lobby.status() != LobbyStatus.VOTING) {
            throw new InvalidLobbyStateException("Готовность можно менять только во время голосования (лобби " + lobbyId + ")");
        }
        LobbyMember member = lobbyMemberRepository.findByLobbyIdAndUserId(lobbyId, userId)
                .orElseThrow(() -> new NotLobbyMemberException(lobbyId, userId));
        lobbyMemberRepository.save(member.withReady(ready));
        realtimeNotifier.readyChanged(lobbyId);
    }

    @Transactional
    public List<GameMatch> closeLobby(Long lobbyId, Long requesterUserId) {
        Lobby lobby = getLobbyOrThrow(lobbyId);
        requireHost(lobby, requesterUserId);
        if (lobby.status() != LobbyStatus.VOTING) {
            throw new InvalidLobbyStateException("Закрыть можно только лобби в статусе VOTING (сейчас " + lobby.status() + ")");
        }

        long readyCount = lobbyMemberRepository.countByLobbyIdAndReadyTrue(lobbyId);
        long totalCount = lobbyMemberRepository.countByLobbyId(lobbyId);
        if (readyCount < totalCount) {
            throw new InvalidLobbyStateException("Не все участники готовы (" + readyCount + "/" + totalCount + ")");
        }

        List<Pick> picks = pickRepository.findAllByLobbyId(lobbyId);
        Map<Long, String> gameNames = loadGameNames(picks);

        List<GameMatch> matches = matchingAlgorithmService.computeMatches(picks, gameNames, (int) totalCount);
        matches.forEach(match -> snapshotRepository.save(LobbyMatchSnapshot.fromMatch(lobbyId, match)));

        lobbyRepository.save(new Lobby(lobby.id(), lobby.inviteCode(), lobby.hostUserId(), LobbyStatus.CLOSED, lobby.createdAt(), Instant.now()));

        realtimeNotifier.statusChanged(lobbyId, LobbyStatus.CLOSED);
        realtimeNotifier.matchComputed(lobbyId, matches);

        return matches;
    }

    public List<LobbyMatchSnapshot> getResults(Long lobbyId) {
        return snapshotRepository.findAllByLobbyIdOrderByRankPosition(lobbyId);
    }

    @Transactional
    public void deleteLobby(Long lobbyId, Long requesterUserId) {
        Lobby lobby = getLobbyOrThrow(lobbyId);
        requireHost(lobby, requesterUserId);
        if (lobby.status() != LobbyStatus.CLOSED) {
            throw new InvalidLobbyStateException("Удалить можно только завершённое лобби (сейчас " + lobby.status() + ")");
        }
        realtimeNotifier.lobbyDeleted(lobbyId);
        lobbyRepository.deleteById(lobbyId);
    }

    private Map<Long, String> loadGameNames(List<Pick> picks) {
        Set<Long> gameIds = picks.stream().map(Pick::gameId).collect(Collectors.toSet());
        if (gameIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        gameRepository.findAllById(gameIds).forEach(game -> names.put(game.id(), game.name()));

        return names;
    }

    private void addMemberInternal(Long lobbyId, Long userId) {
        lobbyMemberRepository.save(new LobbyMember(null, lobbyId, userId, Instant.now(), false));
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        realtimeNotifier.memberJoined(lobbyId, userId, user.displayName());
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < INVITE_CODE_MAX_ATTEMPTS; attempt++) {
            String code = InviteCodeGenerator.generate();
            if (lobbyRepository.findByInviteCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Не удалось сгенерировать уникальный код лобби за " + INVITE_CODE_MAX_ATTEMPTS + " попыток");
    }

    private Lobby getLobbyOrThrow(Long lobbyId) {
        return lobbyRepository.findById(lobbyId).orElseThrow(() -> new LobbyNotFoundException(lobbyId));
    }

    private void requireUserExists(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private void requireHost(Lobby lobby, Long userId) {
        if (!lobby.hostUserId().equals(userId)) {
            throw new NotLobbyHostException(lobby.id(), userId);
        }
    }

    private void requireMember(Lobby lobby, Long userId) {
        if (!lobbyMemberRepository.existsByLobbyIdAndUserId(lobby.id(), userId)) {
            throw new NotLobbyMemberException(lobby.id(), userId);
        }
    }
}