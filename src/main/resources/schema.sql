CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY,
    factory_id VARCHAR(50) NOT NULL,
    board_size INT NOT NULL
);

CREATE TABLE IF NOT EXISTS game_players (
    game_id UUID NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    player_id UUID NOT NULL,
    player_order INT NOT NULL,
    PRIMARY KEY (game_id, player_order)
);

CREATE TABLE IF NOT EXISTS game_tokens (
    id BIGSERIAL PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    token_name VARCHAR(50) NOT NULL,
    owner_id UUID,
    x INT NOT NULL,
    y INT NOT NULL,
    removed BOOLEAN NOT NULL
);
