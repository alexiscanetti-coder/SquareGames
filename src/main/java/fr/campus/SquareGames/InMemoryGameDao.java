package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("in-memory")
public class InMemoryGameDao implements GameDao {
    private final Map<String, Game> games = new ConcurrentHashMap<>();

    @Override
    public void save(Game game) {
        games.put(game.getId().toString(), game);
    }

    @Override
    public Optional<Game> findById(UUID gameId) {
        return Optional.ofNullable(games.get(gameId.toString()));
    }

    @Override
    public List<Game> findByPlayerId(UUID playerId) {
        return games.values().stream()
                .filter(game -> game.getPlayerIds().contains(playerId))
                .toList();
    }
}
