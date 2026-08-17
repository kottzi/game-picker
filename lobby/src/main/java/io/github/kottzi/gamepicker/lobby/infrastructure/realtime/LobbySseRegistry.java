package io.github.kottzi.gamepicker.lobby.infrastructure.realtime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LobbySseRegistry {

    private static final long NO_TIMEOUT = 0L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByLobby = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long lobbyId) {
        SseEmitter emitter = new SseEmitter(NO_TIMEOUT);
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByLobby.computeIfAbsent(lobbyId, _ -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(lobbyId, emitter));
        emitter.onTimeout(() -> remove(lobbyId, emitter));
        emitter.onError(_ -> remove(lobbyId, emitter));

        send(emitter, () -> emitter.send(SseEmitter.event().name("connected").data(Map.of("lobbyId", lobbyId))));
        return emitter;
    }

    public void broadcastLocal(Long lobbyId, LobbyEventMessage event) {
        List<SseEmitter> emitters = emittersByLobby.get(lobbyId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            send(emitter, () -> emitter.send(SseEmitter.event().name(event.type().name()).data(event)));
        }
    }

    @Scheduled(fixedRate = 15_000)
    public void sendHeartbeat() {
        emittersByLobby.values().forEach(emitters ->
                emitters.forEach(emitter -> send(emitter, () -> emitter.send(SseEmitter.event().comment("ping"))))
        );
    }

    private void remove(Long lobbyId, SseEmitter emitter) {
        emittersByLobby.computeIfPresent(lobbyId, (_, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }

    private void send(SseEmitter emitter, EmitAction action) {
        try {
            action.run();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    @FunctionalInterface
    private interface EmitAction {
        void run() throws IOException;
    }
}
