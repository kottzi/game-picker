-- Игры
CREATE TABLE games (
    id                  BIGINT PRIMARY KEY,
    name                VARCHAR(512) NOT NULL,
    is_free             BOOLEAN NOT NULL DEFAULT FALSE,
    release_date        DATE,
    header_image        VARCHAR(1024),
    metadata_synced_at  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_games_name ON games (lower(name));

-- Жанры игр
CREATE TABLE genres (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

-- Игры <-> Жанры
CREATE TABLE game_genres (
    game_id  BIGINT NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genres (id) ON DELETE CASCADE,
    PRIMARY KEY (game_id, genre_id)
);
CREATE INDEX idx_game_genres_genre ON game_genres (genre_id);


-- Авторизация через Steam
CREATE TABLE app_users (
    id             BIGSERIAL PRIMARY KEY,
    steam_id       VARCHAR(32) NOT NULL UNIQUE,
    display_name   VARCHAR(128) NOT NULL,
    avatar_url     VARCHAR(1024),
    profile_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Игры пользователя
CREATE TABLE user_owned_games (
    user_id          BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    game_id          BIGINT NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    playtime_minutes INTEGER NOT NULL DEFAULT 0,
    synced_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, game_id)
);


-- Лобби
CREATE TABLE lobbies (
    id            BIGSERIAL PRIMARY KEY,
    invite_code   VARCHAR(12) NOT NULL UNIQUE,
    host_user_id  BIGINT NOT NULL REFERENCES app_users (id),
    status        VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','VOTING','CLOSED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at     TIMESTAMPTZ
);

-- Участники лобби
CREATE TABLE lobby_members (
    id         BIGSERIAL PRIMARY KEY,
    lobby_id   BIGINT NOT NULL REFERENCES lobbies (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES app_users (id),
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ready      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (lobby_id, user_id)
);

-- Пики (выборы пользователей)
CREATE TABLE picks (
    id         BIGSERIAL PRIMARY KEY,
    lobby_id   BIGINT NOT NULL REFERENCES lobbies (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES app_users (id),
    game_id    BIGINT NOT NULL REFERENCES games (id),
    picked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (lobby_id, user_id, game_id)
);
CREATE INDEX idx_picks_lobby ON picks (lobby_id);

-- Снапшот итогового результата, фиксируется при закрытии лобби (VOTING -> CLOSED)
CREATE TABLE lobby_match_snapshots (
    id               BIGSERIAL PRIMARY KEY,
    lobby_id         BIGINT NOT NULL REFERENCES lobbies (id) ON DELETE CASCADE,
    game_id          BIGINT NOT NULL REFERENCES games (id),
    pick_count       INTEGER NOT NULL,
    match_percentage NUMERIC(5, 2) NOT NULL,
    rank_position    SMALLINT NOT NULL,
    computed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_snapshot_lobby ON lobby_match_snapshots (lobby_id);
