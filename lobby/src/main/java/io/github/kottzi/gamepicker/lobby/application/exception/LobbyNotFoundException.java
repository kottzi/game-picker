package io.github.kottzi.gamepicker.lobby.application.exception;

public class LobbyNotFoundException extends RuntimeException {

    public LobbyNotFoundException(Long lobbyId) {
        super("Лобби не найдено: " + lobbyId);
    }

    public LobbyNotFoundException(String inviteCode) {
        super("Лобби с кодом приглашения не найдено: " + inviteCode);
    }
}
