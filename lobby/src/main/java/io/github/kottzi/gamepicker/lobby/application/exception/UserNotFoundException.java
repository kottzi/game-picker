package io.github.kottzi.gamepicker.lobby.application.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Пользователь не найден: " + userId);
    }
}
