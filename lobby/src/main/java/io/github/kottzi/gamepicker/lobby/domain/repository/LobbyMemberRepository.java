package io.github.kottzi.gamepicker.lobby.domain.repository;

import io.github.kottzi.gamepicker.lobby.domain.model.LobbyMember;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LobbyMemberRepository extends CrudRepository<LobbyMember, Long> {

    List<LobbyMember> findAllByLobbyId(Long lobbyId);

    Optional<LobbyMember> findByLobbyIdAndUserId(Long lobbyId, Long userId);

    long countByLobbyId(Long lobbyId);

    long countByLobbyIdAndReadyTrue(Long lobbyId);

    boolean existsByLobbyIdAndUserId(Long lobbyId, Long userId);

    void deleteByLobbyIdAndUserId(Long lobbyId, Long userId);

    @Query("""
            SELECT lm.* FROM lobby_members lm
            JOIN app_users u ON u.id = lm.user_id
            WHERE lm.lobby_id = :lobbyId AND u.profile_public = false
            """)
    List<LobbyMember> findMembersWithPrivateProfile(@Param("lobbyId") Long lobbyId);

    @Query("""
            SELECT COUNT(*) FROM lobby_members lm
            JOIN app_users u ON u.id = lm.user_id
            WHERE lm.lobby_id = :lobbyId AND u.profile_public = true
            """)
    long countPublicMembers(@Param("lobbyId") Long lobbyId);
}