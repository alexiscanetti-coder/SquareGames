package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@Profile("jdbc")
public class JdbcGameDao implements GameDao {

    private final NamedParameterJdbcTemplate template;
    private final Map<String, GamePlugin> gamePlugins;

    public JdbcGameDao(NamedParameterJdbcTemplate template, List<GamePlugin> gamePlugins) {
        this.template = template;
        this.gamePlugins = gamePlugins.stream().collect(Collectors.toUnmodifiableMap(GamePlugin::getGameFactoryId, plugin -> plugin));
    }

    @Override
    @Transactional
    public void save(Game game) {
        UUID gameId = game.getId();
        template.update("DELETE FROM game_tokens WHERE game_id = :gameId", Map.of("gameId", gameId));
        template.update("DELETE FROM game_players WHERE game_id = :gameId", Map.of("gameId", gameId));
        MapSqlParameterSource gameParams = new MapSqlParameterSource()
                .addValue("id", gameId)
                .addValue("factoryId", game.getFactoryId())
                .addValue("boardSize", game.getBoardSize());
        // Portable upsert (no Postgres-only ON CONFLICT) so this DAO also runs on H2.
        int updated = template.update(
                "UPDATE games SET factory_id = :factoryId, board_size = :boardSize WHERE id = :id", gameParams);
        if (updated == 0) {
            template.update("INSERT INTO games (id, factory_id, board_size) VALUES (:id, :factoryId, :boardSize)", gameParams);
        }

        List<UUID> players = List.copyOf(game.getPlayerIds());
        SqlParameterSource[] playerParams = IntStream.range(0, players.size())
                .mapToObj(order -> new MapSqlParameterSource()
                        .addValue("gameId", gameId)
                        .addValue("playerId", players.get(order))
                        .addValue("playerOrder", order))
                .toArray(SqlParameterSource[]::new);
        template.batchUpdate(
                "INSERT INTO game_players (game_id, player_id, player_order) VALUES (:gameId, :playerId, :playerOrder)",
                playerParams);

        List<SqlParameterSource> tokenParams = new ArrayList<>();
        game.getBoard().forEach((position, token) -> tokenParams.add(tokenParams(gameId, token, position.x(), position.y(), false)));
        game.getRemovedTokens().forEach(token -> tokenParams.add(tokenParams(gameId, token, 0, 0, true)));
        if (!tokenParams.isEmpty()) {
            template.batchUpdate(
                    "INSERT INTO game_tokens (game_id, token_name, owner_id, x, y, removed) VALUES (:gameId, :tokenName, :ownerId, :x, :y, :removed)",
                    tokenParams.toArray(SqlParameterSource[]::new));
        }
    }

    private static SqlParameterSource tokenParams(UUID gameId, Token token, int x, int y, boolean removed) {
        return new MapSqlParameterSource()
                .addValue("gameId", gameId)
                .addValue("tokenName", token.getName())
                .addValue("ownerId", token.getOwnerId().orElse(null))
                .addValue("x", x)
                .addValue("y", y)
                .addValue("removed", removed);
    }

    @Override
    public Optional<Game> findById(UUID gameId) {
        return findByIds(List.of(gameId)).stream().findFirst();
    }

    @Override
    public List<Game> findByPlayerId(UUID playerId) {
        List<UUID> gameIds = template.query(
                "SELECT DISTINCT game_id FROM game_players WHERE player_id = :playerId",
                Map.of("playerId", playerId),
                (rs, rowNum) -> (UUID) rs.getObject("game_id"));
        return findByIds(gameIds);
    }

    // Loads any number of games with a fixed number of queries (one per table), instead of 4 per game.
    private List<Game> findByIds(Collection<UUID> gameIds) {
        if (gameIds.isEmpty()) {
            return List.of();
        }
        Map<String, Object> params = Map.of("gameIds", gameIds);

        Map<UUID, List<UUID>> playersByGame = new HashMap<>();
        template.query(
                "SELECT game_id, player_id FROM game_players WHERE game_id IN (:gameIds) ORDER BY game_id, player_order",
                params,
                rs -> {
                    playersByGame.computeIfAbsent((UUID) rs.getObject("game_id"), id -> new ArrayList<>())
                            .add((UUID) rs.getObject("player_id"));
                });

        Map<UUID, List<TokenPosition<UUID>>> boardTokensByGame = new HashMap<>();
        Map<UUID, List<TokenPosition<UUID>>> removedTokensByGame = new HashMap<>();
        template.query(
                "SELECT game_id, token_name, owner_id, x, y, removed FROM game_tokens WHERE game_id IN (:gameIds)",
                params,
                rs -> {
                    UUID gameId = (UUID) rs.getObject("game_id");
                    UUID ownerId = (UUID) rs.getObject("owner_id");
                    String tokenName = rs.getString("token_name");
                    if (rs.getBoolean("removed")) {
                        removedTokensByGame.computeIfAbsent(gameId, id -> new ArrayList<>())
                                .add(new TokenPosition<>(ownerId, tokenName, 0, 0));
                    } else {
                        boardTokensByGame.computeIfAbsent(gameId, id -> new ArrayList<>())
                                .add(new TokenPosition<>(ownerId, tokenName, rs.getInt("x"), rs.getInt("y")));
                    }
                });

        return template.query(
                "SELECT id, factory_id, board_size FROM games WHERE id IN (:gameIds)",
                params,
                (rs, rowNum) -> {
                    UUID gameId = (UUID) rs.getObject("id");
                    String factoryId = rs.getString("factory_id");
                    GamePlugin plugin = gamePlugins.get(factoryId);
                    if (plugin == null) {
                        throw new InvalidGameOperationException("Type de jeu inconnu : " + factoryId);
                    }
                    return plugin.restoreGame(gameId, rs.getInt("board_size"),
                            playersByGame.getOrDefault(gameId, List.of()),
                            boardTokensByGame.getOrDefault(gameId, List.of()),
                            removedTokensByGame.getOrDefault(gameId, List.of()));
                });
    }
}
