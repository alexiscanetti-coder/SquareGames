package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.*;

public interface GameDao {
    void save(Game game);
    Optional<Game> findById(UUID gameId);
    List<Game> findByPlayerId(UUID playerId);
}
