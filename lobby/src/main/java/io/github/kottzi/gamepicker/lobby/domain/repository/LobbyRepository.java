package io.github.kottzi.gamepicker.lobby.domain.repository;

import io.github.kottzi.gamepicker.lobby.domain.model.Lobby;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LobbyRepository extends CrudRepository<Lobby, Long> {

    Optional<Lobby> findByInviteCode(String inviteCode);
}
