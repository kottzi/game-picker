package io.github.kottzi.gamepicker.lobby.application.exception;

public class NotLobbyHostException extends RuntimeException {

    public NotLobbyHostException(Long lobbyId, Long userId) {
        super("Пользователь %d не является хостом лобби %d".formatted(userId, lobbyId));
    }
}
