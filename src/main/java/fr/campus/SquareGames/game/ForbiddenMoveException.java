package fr.campus.SquareGames.game;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenMoveException extends RuntimeException {

    public ForbiddenMoveException(String message) {
        super(message);
    }
}
