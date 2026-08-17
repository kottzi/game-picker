package io.github.kottzi.gamepicker.lobby.application;

import io.github.kottzi.gamepicker.auth.domain.model.AppUser;
import io.github.kottzi.gamepicker.auth.domain.repository.AppUserRepository;
import io.github.kottzi.gamepicker.catalog.domain.repository.GameRepository;
import io.github.kottzi.gamepicker.lobby.application.exception.InvalidLobbyStateException;
import io.github.kottzi.gamepicker.lobby.application.exception.NotLobbyHostException;
import io.github.kottzi.gamepicker.lobby.domain.model.GameMatch;
import io.github.kottzi.gamepicker.lobby.domain.model.Lobby;
import io.github.kottzi.gamepicker.lobby.domain.model.LobbyStatus;
import io.github.kottzi.gamepicker.lobby.domain.model.Pick;
import io.github.kottzi.gamepicker.lobby.domain.repository.*;
import io.github.kottzi.gamepicker.lobby.domain.service.MatchingAlgorithmService;
import io.github.kottzi.gamepicker.lobby.infrastructure.realtime.LobbyRealtimeNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;
    @Mock
    private LobbyMemberRepository lobbyMemberRepository;
    @Mock
    private PickRepository pickRepository;
    @Mock
    private LobbyGamePoolRepository lobbyGamePoolRepository;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private LobbyMatchSnapshotRepository snapshotRepository;
    @Mock
    private LobbyRealtimeNotifier realtimeNotifier;

    private final MatchingAlgorithmService matchingAlgorithmService = new MatchingAlgorithmService();

    private LobbyService service;

    private static final Long HOST_ID = 1L;
    private static final Long LOBBY_ID = 10L;

    @BeforeEach
    void setUp() {
        this.service = new LobbyService(
                lobbyRepository,
                lobbyMemberRepository,
                pickRepository,
                lobbyGamePoolRepository,
                gameRepository,
                appUserRepository,
                snapshotRepository,
                matchingAlgorithmService,
                realtimeNotifier
        );
    }

    @Test
    void createLobby_generatesCodeAndAddsHostAsMember() {
        when(appUserRepository.existsById(HOST_ID)).thenReturn(true);
        when(lobbyRepository.findByInviteCode(any())).thenReturn(Optional.empty());
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby passed = invocation.getArgument(0);
            return new Lobby(LOBBY_ID, passed.inviteCode(), passed.hostUserId(), passed.status(),
                    passed.createdAt(), passed.closedAt());
        });
        when(appUserRepository.findById(HOST_ID)).thenReturn(
                Optional.of(new AppUser(HOST_ID, "76561198000000001", "Host", null, true, Instant.now(), Instant.now())));

        Lobby lobby = service.createLobby(HOST_ID);

        assertEquals(LobbyStatus.OPEN, lobby.status());
        assertNotNull(lobby.inviteCode());
        verify(lobbyMemberRepository).save(argThat(member -> member.lobbyId().equals(LOBBY_ID) && member.userId().equals(HOST_ID)));
        verify(realtimeNotifier).memberJoined(eq(LOBBY_ID), eq(HOST_ID), eq("Host"));
    }

    @Test
    void joinLobby_wrongStatus_throws() {
        Lobby votingLobby = new Lobby(LOBBY_ID, "ABC123", HOST_ID, LobbyStatus.VOTING, Instant.now(), null);
        when(lobbyRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(votingLobby));

        assertThrows(InvalidLobbyStateException.class, () -> service.joinLobby("ABC123", 2L));
        verifyNoInteractions(lobbyMemberRepository);
    }

    @Test
    void addPick_lobbyNotVoting_throws() {
        Lobby openLobby = new Lobby(LOBBY_ID, "ABC123", HOST_ID, LobbyStatus.OPEN, Instant.now(), null);
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(openLobby));
        when(lobbyMemberRepository.existsByLobbyIdAndUserId(LOBBY_ID, HOST_ID)).thenReturn(true);

        assertThrows(InvalidLobbyStateException.class, () -> service.addPick(LOBBY_ID, HOST_ID, 100L));
        verify(pickRepository, never()).save(any());
    }

    @Test
    void startVoting_notHost_throwsForbidden() {
        Lobby lobby = new Lobby(LOBBY_ID, "ABC123", HOST_ID, LobbyStatus.OPEN, Instant.now(), null);
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(lobby));

        assertThrows(NotLobbyHostException.class, () -> service.startVoting(LOBBY_ID, 999L));
        verify(lobbyRepository, never()).save(any());
    }

    @Test
    void closeLobby_computesMatchesAndPersistsSnapshotsAndStatus() {
        Lobby votingLobby = new Lobby(LOBBY_ID, "ABC123", HOST_ID, LobbyStatus.VOTING, Instant.now(), null);
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(votingLobby));
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Pick> picks = List.of(
                new Pick(null, LOBBY_ID, 1L, 100L, Instant.now()),
                new Pick(null, LOBBY_ID, 2L, 100L, Instant.now()),
                new Pick(null, LOBBY_ID, 1L, 200L, Instant.now())
        );
        when(pickRepository.findAllByLobbyId(LOBBY_ID)).thenReturn(picks);
        when(lobbyMemberRepository.countByLobbyId(LOBBY_ID)).thenReturn(2L);
        when(gameRepository.findAllById(any())).thenReturn(List.of());

        List<GameMatch> matches = service.closeLobby(LOBBY_ID, HOST_ID);

        assertEquals(2, matches.size()); // 2 разных игры - обе показываем
        assertEquals(100L, matches.getFirst().gameId()); // у неё 2 пика против 1 - выше в рейтинге
        verify(snapshotRepository, times(2)).save(any());
        verify(realtimeNotifier).statusChanged(LOBBY_ID, LobbyStatus.CLOSED);
        verify(realtimeNotifier).matchComputed(eq(LOBBY_ID), any());
        verify(lobbyRepository).save(argThat(l -> l.status() == LobbyStatus.CLOSED && l.closedAt() != null));
    }
}
