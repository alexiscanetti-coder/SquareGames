package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class ConnectFourPlugin extends AbstractGamePlugin {

    public ConnectFourPlugin(
            MessageSource messageSource,
            @Value("${game.connect4.default-player-count}") int defaultPlayerCount,
            @Value("${game.connect4.default-board-size}") int defaultBoardSize) {
        super(new ConnectFourGameFactory(), defaultPlayerCount, defaultBoardSize, messageSource);
    }
}
