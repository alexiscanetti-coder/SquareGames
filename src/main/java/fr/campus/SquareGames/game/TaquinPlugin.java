package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TaquinPlugin extends AbstractGamePlugin {

    private static final int RESTORABLE_BOARD_SIZE = 4;

    public TaquinPlugin(
            MessageSource messageSource,
            @Value("${game.taquin.default-player-count}") int defaultPlayerCount,
            @Value("${game.taquin.default-board-size}") int defaultBoardSize) {
        super(new TaquinGameFactory(), defaultPlayerCount, defaultBoardSize, messageSource);
    }

    // The engine can create a Taquin of any size but only restores 4x4 boards (15 tiles),
    // so any other size would produce a game that cannot be reloaded from the database.
    @Override
    public Game createGame(GameCreationParams params, UUID creatorId) {
        if (params.boardSize() != null && params.boardSize() != RESTORABLE_BOARD_SIZE) {
            throw new IllegalArgumentException("Le Taquin ne supporte qu'un plateau de taille " + RESTORABLE_BOARD_SIZE);
        }
        return super.createGame(params, creatorId);
    }
}
