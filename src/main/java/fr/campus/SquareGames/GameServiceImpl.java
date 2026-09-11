package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GameServiceImpl implements GameService {

    private final Map<String, GamePlugin> gamePlugins;

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    public GameServiceImpl(List<GamePlugin> gamePlugins) {
        this.gamePlugins = gamePlugins.stream()
                .collect(Collectors.toUnmodifiableMap(GamePlugin::getGameFactoryId, plugin -> plugin));
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
            games.put(game.getId(), game);
            return game;
        } catch (IllegalArgumentException e) {
            throw new InvalidGameOperationException(e.getMessage());
        }
    }

    @Override
    public Game getGame(UUID gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            throw new GameNotFoundException(gameId);
        }
        return game;
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
        } catch (InvalidPositionException e) {
            throw new InvalidGameOperationException(e.getMessage());
        }
        return game;
    }
}