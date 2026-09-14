package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Profile("jpa")
public class JpaGameDao implements GameDao {

    private static final String PLAYER_ID_SEPARATOR = ",";

    private final GameEntityRepository repository;
    private final Map<String, GamePlugin> gamePlugins;

    public JpaGameDao(GameEntityRepository repository, List<GamePlugin> gamePlugins) {
        this.repository = repository;
        this.gamePlugins = gamePlugins.stream().collect(Collectors.toUnmodifiableMap(GamePlugin::getGameFactoryId, plugin -> plugin));
    }

    @Override
    public void save(Game game) {
        GameEntity entity = new GameEntity();
        entity.id = game.getId().toString();
        entity.factoryId = game.getFactoryId();
        entity.boardSize = game.getBoardSize();
        entity.playerIds = game.getPlayerIds().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(PLAYER_ID_SEPARATOR));

        List<GameTokenEntity> tokens = new ArrayList<>();
        game.getBoard().forEach((position, token) -> tokens.add(toTokenEntity(token, position.x(), position.y(), false)));
        game.getRemovedTokens().forEach(token -> tokens.add(toTokenEntity(token, 0, 0, true)));
        entity.tokens = tokens;

        repository.save(entity);
    }

    private static GameTokenEntity toTokenEntity(Token token, int x, int y, boolean removed) {
        GameTokenEntity entity = new GameTokenEntity();
        entity.ownerId = token.getOwnerId().map(UUID::toString).orElse(null);
        entity.name = token.getName();
        entity.x = x;
        entity.y = y;
        entity.removed = removed;
        return entity;
    }

    @Override
    public Optional<Game> findById(UUID gameId) {
        return repository.findById(gameId.toString()).map(this::toGame);
    }

    private Game toGame(GameEntity entity) {
        List<UUID> players = Arrays.stream(entity.playerIds.split(PLAYER_ID_SEPARATOR))
                .map(UUID::fromString)
                .toList();

        List<TokenPosition<UUID>> boardTokens = entity.tokens.stream()
                .filter(token -> !token.removed)
                .map(token -> toTokenPosition(token, token.x, token.y))
                .toList();

        List<TokenPosition<UUID>> removedTokens = entity.tokens.stream()
                .filter(token -> token.removed)
                .map(token -> toTokenPosition(token, 0, 0))
                .toList();

        GamePlugin plugin = gamePlugins.get(entity.factoryId);
        if (plugin == null) {
            throw new InvalidGameOperationException("Type de jeu inconnu : " + entity.factoryId);
        }
        return plugin.restoreGame(UUID.fromString(entity.id), entity.boardSize, players, boardTokens, removedTokens);
    }

    private static TokenPosition<UUID> toTokenPosition(GameTokenEntity token, int x, int y) {
        UUID ownerId = token.ownerId != null ? UUID.fromString(token.ownerId) : null;
        return new TokenPosition<>(ownerId, token.name, x, y);
    }
}
