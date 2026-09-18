package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GameServiceImpl implements GameService {

    private final Map<String, GamePlugin> gamePlugins;
    private final GameDao gameDao;
    private final UserClient userClient;

    public GameServiceImpl(List<GamePlugin> gamePlugins, GameDao gameDao, UserClient userClient) {
        this.gamePlugins = gamePlugins.stream().collect(Collectors.toUnmodifiableMap(GamePlugin::getGameFactoryId, plugin -> plugin));
        this.gameDao = gameDao;
        this.userClient = userClient;
    }

    @Override
    public Game createGame(GameCreationParams params, UUID creatorId) {
        validateUser(creatorId);
        GamePlugin plugin = gamePlugins.get(params.gameType());
        if (plugin == null) {
            throw new InvalidGameOperationException(
                    "Type de jeu inconnu : " + params.gameType() + " (supportés : " + gamePlugins.keySet() + ")");
        }
        try {
            Game game = plugin.createGame(params, creatorId);
            gameDao.save(game);
            return game;
        } catch (IllegalArgumentException e) {
            throw new InvalidGameOperationException(e.getMessage());
        }
    }

    @Override
    public Game getGame(UUID gameId) {
        return gameDao.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    @Override
    public List<Game> listGames(UUID userId) {
        validateUser(userId);
        return gameDao.findByPlayerId(userId).stream()
                .filter(game -> game.getStatus() == GameStatus.ONGOING)
                .toList();
    }

    @Override
    public Game move(UUID gameId, CellPosition target, UUID userId) {
        validateUser(userId);
        Game game = getGame(gameId);
        if (!userId.equals(game.getCurrentPlayerId())) {
            throw new ForbiddenMoveException("Ce n'est pas le tour de " + userId);
        }
        Token token = Stream.concat(game.getBoard().values().stream(), game.getRemainingTokens().stream())
                .filter(candidate -> candidate.getAllowedMoves().contains(target))
                .findFirst()
                .orElseThrow(() -> new InvalidGameOperationException(
                        "Aucun jeton ne peut aller en (" + target.x() + ", " + target.y() + ")"));
        try {
            token.moveTo(target);
            gameDao.save(game);
        } catch (InvalidPositionException e) {
            throw new InvalidGameOperationException(e.getMessage());
        }
        return game;
    }

    private void validateUser(UUID userId) {
        if (!userClient.isValid(userId)) {
            throw new UnknownUserException(userId);
        }
    }
}
