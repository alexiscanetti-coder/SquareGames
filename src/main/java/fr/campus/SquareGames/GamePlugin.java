package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Locale;


public interface GamePlugin {

    String getGameFactoryId();

    String getName(Locale locale);

    Game createGame(GameCreationParams params);
}