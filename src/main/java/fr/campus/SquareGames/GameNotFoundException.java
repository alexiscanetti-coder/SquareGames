package fr.campus.SquareGames;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Levée quand aucune partie ne correspond à l'identifiant demandé. Traduite en {@code 404}.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(UUID gameId) {
        super("Partie introuvable : " + gameId);
    }
}
