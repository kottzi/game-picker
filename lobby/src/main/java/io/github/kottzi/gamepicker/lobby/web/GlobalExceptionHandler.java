package io.github.kottzi.gamepicker.lobby.web;

import io.github.kottzi.gamepicker.lobby.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.github.kottzi.gamepicker.lobby.application.exception.InvalidLobbyStateException;
import io.github.kottzi.gamepicker.lobby.application.exception.LobbyNotFoundException;
import io.github.kottzi.gamepicker.lobby.application.exception.NotLobbyHostException;
import io.github.kottzi.gamepicker.lobby.application.exception.NotLobbyMemberException;
import io.github.kottzi.gamepicker.lobby.application.exception.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({LobbyNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({NotLobbyHostException.class, NotLobbyMemberException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(InvalidLobbyStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(InvalidLobbyStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }
}
