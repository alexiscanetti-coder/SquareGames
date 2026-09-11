package fr.campus.SquareGames;

import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class TaquinPlugin extends AbstractGamePlugin {

    public TaquinPlugin(
            MessageSource messageSource,
            @Value("${game.taquin.default-player-count}") int defaultPlayerCount,
            @Value("${game.taquin.default-board-size}") int defaultBoardSize) {
        super(new TaquinGameFactory(), defaultPlayerCount, defaultBoardSize, messageSource);
    }
}
