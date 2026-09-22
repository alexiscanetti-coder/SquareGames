package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.TokenPosition;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


public interface GamePlugin {

    String getGameFactoryId();

    String getName(Locale locale);

    Game createGame(GameCreationParams params, UUID creatorId);

    Game restoreGame(UUID gameId, int boardSize, List<UUID> players,
                      Collection<TokenPosition<UUID>> boardTokens,
                      Collection<TokenPosition<UUID>> removedTokens);
}