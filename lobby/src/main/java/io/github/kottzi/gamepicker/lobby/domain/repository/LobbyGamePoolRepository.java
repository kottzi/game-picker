package io.github.kottzi.gamepicker.lobby.domain.repository;

import io.github.kottzi.gamepicker.catalog.domain.model.Game;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LobbyGamePoolRepository extends Repository<Game, Long> {

    @Query("""
            SELECT g.* FROM games g
            JOIN user_owned_games uog ON uog.game_id = g.id
            JOIN lobby_members lm ON lm.user_id = uog.user_id
            JOIN app_users u ON u.id = lm.user_id AND u.profile_public = true
            WHERE lm.lobby_id = :lobbyId
              AND (:genreIds::bigint[] IS NULL OR EXISTS (
                  SELECT 1 FROM game_genres gg WHERE gg.game_id = g.id AND gg.genre_id = ANY(:genreIds::bigint[])
              ))
              AND (:isFree::boolean IS NULL OR g.is_free = :isFree::boolean)
            GROUP BY g.id
            HAVING COUNT(DISTINCT uog.user_id) = (
                SELECT COUNT(*) FROM lobby_members lm2
                JOIN app_users u2 ON u2.id = lm2.user_id
                WHERE lm2.lobby_id = :lobbyId AND u2.profile_public = true
            )
            ORDER BY g.name
            """)
    List<Game> findIntersectionForLobby(
            @Param("lobbyId") Long lobbyId,
            @Param("genreIds") Long[] genreIds,
            @Param("isFree") Boolean isFree
    );
}
