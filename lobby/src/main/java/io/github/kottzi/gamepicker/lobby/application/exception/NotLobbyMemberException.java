package io.github.kottzi.gamepicker.lobby.application.exception;

public class NotLobbyMemberException extends RuntimeException {

    public NotLobbyMemberException(Long lobbyId, Long userId) {
        super("Пользователь %d не состоит в лобби %d".formatted(userId, lobbyId));
    }
}
