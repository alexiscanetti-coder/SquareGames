package fr.campus.SquareGames.game;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnknownUserException extends RuntimeException {

    public UnknownUserException(UUID userId) {
        super("Utilisateur inconnu : " + userId);
    }
}
