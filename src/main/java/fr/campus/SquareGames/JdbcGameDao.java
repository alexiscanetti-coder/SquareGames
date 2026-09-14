package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.CellPosition;
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
        template.update("""
                INSERT INTO games (id, factory_id, board_size) VALUES (:id, :factoryId, :boardSize)
                ON CONFLICT (id) DO UPDATE SET factory_id = EXCLUDED.factory_id, board_size = EXCLUDED.board_size
                """,
                new MapSqlParameterSource()
                        .addValue("id", gameId)
                        .addValue("factoryId", game.getFactoryId())
                        .addValue("boardSize", game.getBoardSize()));

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
        List<Map<String, Object>> gameRows = template.queryForList(
                "SELECT factory_id, board_size FROM games WHERE id = :gameId", Map.of("gameId", gameId));
        if (gameRows.isEmpty()) {
            return Optional.empty();
        }
        String factoryId = (String) gameRows.get(0).get("factory_id");
        int boardSize = (Integer) gameRows.get(0).get("board_size");

        List<UUID> players = template.query(
                "SELECT player_id FROM game_players WHERE game_id = :gameId ORDER BY player_order",
                Map.of("gameId", gameId),
                (rs, rowNum) -> (UUID) rs.getObject("player_id"));

        List<TokenPosition<UUID>> boardTokens = template.query(
                "SELECT token_name, owner_id, x, y FROM game_tokens WHERE game_id = :gameId AND removed = FALSE",
                Map.of("gameId", gameId),
                (rs, rowNum) -> new TokenPosition<>(
                        (UUID) rs.getObject("owner_id"), rs.getString("token_name"), rs.getInt("x"), rs.getInt("y")));

        List<TokenPosition<UUID>> removedTokens = template.query(
                "SELECT token_name, owner_id FROM game_tokens WHERE game_id = :gameId AND removed = TRUE",
                Map.of("gameId", gameId),
                (rs, rowNum) -> new TokenPosition<>((UUID) rs.getObject("owner_id"), rs.getString("token_name"), 0, 0));

        GamePlugin plugin = gamePlugins.get(factoryId);
        if (plugin == null) {
            throw new InvalidGameOperationException("Type de jeu inconnu : " + factoryId);
        }
        return Optional.of(plugin.restoreGame(gameId, boardSize, players, boardTokens, removedTokens));
    }
}
