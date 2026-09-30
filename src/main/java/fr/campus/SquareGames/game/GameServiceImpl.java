package fr.campus.SquareGames.game;

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

@Service
public class GameServiceImpl implements GameService {

    private final Map<String, GamePlugin> gamePlugins;
    private final GameDao gameDao;

    public GameServiceImpl(List<GamePlugin> gamePlugins, GameDao gameDao) {
        this.gamePlugins = gamePlugins.stream().collect(Collectors.toUnmodifiableMap(GamePlugin::getGameFactoryId, plugin -> plugin));
        this.gameDao = gameDao;
    }

    @Override
    public Game createGame(GameCreationParams params, UUID creatorId) {
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
        return gameDao.findByPlayerId(userId).stream()
                .filter(game -> game.getStatus() == GameStatus.ONGOING)
                .toList();
    }

    @Override
    public Game move(UUID gameId, MoveParams params, UUID userId) {
        CellPosition target = params.target();
        if (target == null) {
            throw new InvalidGameOperationException("La position cible (target) est obligatoire");
        }
        Game game = getGame(gameId);
        if (!userId.equals(game.getCurrentPlayerId())) {
            throw new ForbiddenMoveException("Ce n'est pas le tour de " + userId);
        }
        Token token = params.source() != null
                ? tokenAt(game, params.source(), target)
                : tokenReaching(game, target);
        try {
            token.moveTo(target);
            gameDao.save(game);
        } catch (InvalidPositionException e) {
            throw new InvalidGameOperationException(e.getMessage());
        }
        return game;
    }

    private static Token tokenAt(Game game, CellPosition source, CellPosition target) {
        Token token = game.getBoard().get(source);
        if (token == null) {
            throw new InvalidGameOperationException("Aucun jeton en " + format(source));
        }
        if (!token.getAllowedMoves().contains(target)) {
            throw new InvalidGameOperationException(
                    "Le jeton en " + format(source) + " ne peut pas aller en " + format(target));
        }
        return token;
    }

    // Tokens already on the board are distinct, so several candidates means the move is ambiguous;
    // remaining (not yet placed) tokens are interchangeable, so any of them will do.
    private static Token tokenReaching(Game game, CellPosition target) {
        List<Token> boardCandidates = game.getBoard().values().stream()
                .filter(candidate -> candidate.getAllowedMoves().contains(target))
                .toList();
        if (boardCandidates.size() > 1) {
            throw new InvalidGameOperationException("Plusieurs jetons peuvent aller en " + format(target)
                    + " : précisez la position du jeton à déplacer (source)");
        }
        return boardCandidates.stream().findFirst()
                .or(() -> game.getRemainingTokens().stream()
                        .filter(candidate -> candidate.getAllowedMoves().contains(target))
                        .findFirst())
                .orElseThrow(() -> new InvalidGameOperationException("Aucun jeton ne peut aller en " + format(target)));
    }

    private static String format(CellPosition position) {
        return "(" + position.x() + ", " + position.y() + ")";
    }
}
