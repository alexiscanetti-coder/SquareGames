package fr.campus.SquareGames.game;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidGameOperationException extends RuntimeException {

    public InvalidGameOperationException(String message) {
        super(message);
    }
}
