package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
            description = "Le créateur (X-UserId) devient un joueur de la partie. Les adversaires sont soit fournis "
                    + "via opponentIds, soit générés aléatoirement pour compléter playerCount.")
    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public UUID createGame(@RequestHeader("X-UserId") UUID userId, @RequestBody GameCreationParams params) {
        return gameService.createGame(params, userId).getId();
    }

    @Operation(summary = "Consulter une partie")
    @GetMapping("/games/{gameId}")
    public Game getGame(@PathVariable UUID gameId) {
        return gameService.getGame(gameId);
    }

    @Operation(summary = "Lister mes parties en cours",
            description = "Retourne les parties non terminées auxquelles participe X-UserId.")
    @GetMapping("/games/mine")
    public List<Game> listMyGames(@RequestHeader("X-UserId") UUID userId) {
        return gameService.listGames(userId);
    }

    @Operation(summary = "Jouer un coup",
            description = "Rejeté avec 403 si ce n'est pas le tour du joueur identifié par X-UserId.")
    @PostMapping("/games/{gameId}/moves")
    public Game move(@RequestHeader("X-UserId") UUID userId, @PathVariable UUID gameId, @RequestBody CellPosition target) {
        return gameService.move(gameId, target, userId);
    }
}
