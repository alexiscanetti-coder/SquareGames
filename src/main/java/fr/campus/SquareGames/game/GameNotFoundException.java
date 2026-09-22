package fr.campus.SquareGames.game;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(UUID gameId) {
        super("Partie introuvable : " + gameId);
    }
}
