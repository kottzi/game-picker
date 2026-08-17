package io.github.kottzi.gamepicker.lobby.domain.repository;

import io.github.kottzi.gamepicker.lobby.domain.model.Pick;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PickRepository extends CrudRepository<Pick, Long> {

    List<Pick> findAllByLobbyId(Long lobbyId);

    List<Pick> findAllByLobbyIdAndUserId(Long lobbyId, Long userId);

    boolean existsByLobbyIdAndUserIdAndGameId(Long lobbyId, Long userId, Long gameId);

    void deleteByLobbyIdAndUserIdAndGameId(Long lobbyId, Long userId, Long gameId);
}
