package io.github.kottzi.gamepicker.lobby.domain.repository;

import io.github.kottzi.gamepicker.lobby.domain.model.LobbyMatchSnapshot;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LobbyMatchSnapshotRepository extends CrudRepository<LobbyMatchSnapshot, Long> {

    List<LobbyMatchSnapshot> findAllByLobbyIdOrderByRankPosition(Long lobbyId);
}
