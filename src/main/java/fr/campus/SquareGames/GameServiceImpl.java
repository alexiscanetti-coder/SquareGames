package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
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

    public GameServiceImpl(List<GamePlugin> gamePlugins, GameDao gameDao) {
        this.gamePlugins = gamePlugins.stream().collect(Collectors.toUnmodifiableMap(GamePlugin::getGameFactoryId, plugin -> plugin));
        this.gameDao = gameDao;
    }

    @Override
    public Game createGame(GameCreationParams params) {
        GamePlugin plugin = gamePlugins.get(params.gameType());
        if (plugin == null) {
            throw new InvalidGameOperationException(
                    "Type de jeu inconnu : " + params.gameType() + " (supportés : " + gamePlugins.keySet() + ")");
        }
        try {
            Game game = plugin.createGame(params);
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
    public Game move(UUID gameId, CellPosition target) {
        Game game = getGame(gameId);
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
}