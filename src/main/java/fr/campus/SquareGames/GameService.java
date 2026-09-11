package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;

import java.util.UUID;


public interface GameService {

    Game createGame(GameCreationParams params);

    Game getGame(UUID gameId);

    Game move(UUID gameId, CellPosition target);
}
