package fr.campus.SquareGames;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levée quand la requête est comprise mais refusée par le moteur : type de jeu inconnu,
 * nombre de joueurs / taille de plateau hors limites, ou coup illégal. Traduite en {@code 400}.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidGameOperationException extends RuntimeException {

    public InvalidGameOperationException(String message) {
        super(message);
    }
}
