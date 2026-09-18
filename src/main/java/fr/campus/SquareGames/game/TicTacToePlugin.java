package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class TicTacToePlugin extends AbstractGamePlugin {

    public TicTacToePlugin(
            MessageSource messageSource,
            @Value("${game.tictactoe.default-player-count}") int defaultPlayerCount,
            @Value("${game.tictactoe.default-board-size}") int defaultBoardSize) {
        super(new TicTacToeGameFactory(), defaultPlayerCount, defaultBoardSize, messageSource);
    }
}
