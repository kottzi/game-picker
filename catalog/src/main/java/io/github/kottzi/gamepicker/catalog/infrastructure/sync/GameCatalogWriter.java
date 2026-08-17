package io.github.kottzi.gamepicker.catalog.infrastructure.sync;

import io.github.kottzi.gamepicker.steam.dto.SteamAppDetails;
import io.github.kottzi.gamepicker.steam.dto.SteamAppListEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Repository
public class GameCatalogWriter {

    private final JdbcTemplate jdbcTemplate;

    public GameCatalogWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertBareGames(List<SteamAppListEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO games (id, name, is_free, created_at)
                VALUES (?, ?, false, now())
                ON CONFLICT (id) DO NOTHING
                """;
        List<Object[]> args = entries.stream()
                .map(entry -> new Object[]{entry.appId(), entry.name()})
                .toList();
        jdbcTemplate.batchUpdate(sql, args);
    }

    public List<Long> findGameIdsMissingMetadata(int limit) {
        return jdbcTemplate.queryForList("""
                SELECT g.id FROM games g
                WHERE g.metadata_synced_at IS NULL
                ORDER BY EXISTS (SELECT 1 FROM user_owned_games uog WHERE uog.game_id = g.id) DESC, g.id ASC
                LIMIT ?
                """, Long.class, limit);
    }

    @Transactional
    public void applyMetadata(SteamAppDetails details) {
        jdbcTemplate.update("""
                UPDATE games
                SET name = ?, is_free = ?, release_date = ?, header_image = ?, metadata_synced_at = now()
                WHERE id = ?
                """, details.name(), details.isFree(), details.releaseDate(), details.headerImage(), details.appId());

        jdbcTemplate.update("DELETE FROM game_genres WHERE game_id = ?", details.appId());
        for (String genreName : details.genreNames()) {
            long genreId = findOrCreateGenreId(genreName);
            jdbcTemplate.update(
                    "INSERT INTO game_genres (game_id, genre_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    details.appId(),
                    genreId
            );
        }
    }

    public void markMetadataAttempted(long appId) {
        jdbcTemplate.update("UPDATE games SET metadata_synced_at = now() WHERE id = ?", appId);
    }

    private long findOrCreateGenreId(String name) {
        List<Long> existing = jdbcTemplate.queryForList("SELECT id FROM genres WHERE name = ?", Long.class, name);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "INSERT INTO genres (name) VALUES (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id",
                Long.class,
                name
        ));
    }
}