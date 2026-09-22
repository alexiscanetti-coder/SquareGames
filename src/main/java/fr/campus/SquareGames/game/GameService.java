package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;

import java.util.List;
import java.util.UUID;


public interface GameService {

    Game createGame(GameCreationParams params, UUID creatorId);

    Game getGame(UUID gameId);

    List<Game> listGames(UUID userId);

    Game move(UUID gameId, CellPosition target, UUID userId);
}
