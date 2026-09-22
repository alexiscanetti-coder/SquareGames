package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Games", description = "Création, consultation et déroulement des parties")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "Créer une partie",
            description = "Le créateur (identifié par le JWT Authorization: Bearer) devient un joueur de la partie. "
                    + "Les adversaires sont soit fournis via opponentIds, soit générés aléatoirement pour compléter playerCount.")
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID createGame(Authentication authentication, @RequestBody GameCreationParams params) {
        return gameService.createGame(params, userId(authentication)).getId();
    }

    @Operation(summary = "Consulter une partie")
    @GetMapping("/games/{gameId}")
    public Game getGame(@PathVariable UUID gameId) {
        return gameService.getGame(gameId);
    }

    @Operation(summary = "Lister mes parties en cours",
            description = "Retourne les parties non terminées auxquelles participe le joueur identifié par le JWT.")
    @GetMapping("/games/mine")
    public List<Game> listMyGames(Authentication authentication) {
        return gameService.listGames(userId(authentication));
    }

    @Operation(summary = "Jouer un coup",
            description = "Rejeté avec 403 si ce n'est pas le tour du joueur identifié par le JWT.")
    @PostMapping("/games/{gameId}/moves")
    public Game move(Authentication authentication, @PathVariable UUID gameId, @RequestBody CellPosition target) {
        return gameService.move(gameId, target, userId(authentication));
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
