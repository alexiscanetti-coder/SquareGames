package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.Token;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GameMoveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameService gameService;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void taquinMoveWithoutSourceIsRejectedAsAmbiguous() throws Exception {
        UUID playerId = UUID.randomUUID();
        Game game = newTaquin(playerId);
        CellPosition empty = emptyCell(game);

        move(game.getId(), playerId, "{\"target\":" + json(empty) + "}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void taquinMoveWithSourceMovesTheChosenTile() {
        UUID playerId = UUID.randomUUID();
        Game game = newTaquin(playerId);
        CellPosition empty = emptyCell(game);
        List<CellPosition> movable = game.getBoard().entrySet().stream()
                .filter(entry -> entry.getValue().getAllowedMoves().contains(empty))
                .map(java.util.Map.Entry::getKey)
                .toList();
        // Pick the last candidate so the test would fail if the service still used findFirst().
        CellPosition source = movable.getLast();
        String tileName = game.getBoard().get(source).getName();

        Game after = gameService.move(game.getId(), new MoveParams(source, empty), playerId);

        assertThat(after.getBoard().get(empty).getName()).isEqualTo(tileName);
    }

    @Test
    void taquinMoveFromACellThatCannotReachTargetIsRejected() throws Exception {
        UUID playerId = UUID.randomUUID();
        Game game = newTaquin(playerId);
        CellPosition empty = emptyCell(game);
        CellPosition farTile = game.getBoard().entrySet().stream()
                .filter(entry -> !entry.getValue().getAllowedMoves().contains(empty))
                .map(java.util.Map.Entry::getKey)
                .findFirst().orElseThrow();

        move(game.getId(), playerId, "{\"source\":" + json(farTile) + ",\"target\":" + json(empty) + "}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void taquinCannotBeCreatedWithABoardTheEngineCannotRestore() {
        assertThatThrownBy(() -> gameService.createGame(
                new GameCreationParams("15 puzzle", null, 3, null), UUID.randomUUID()))
                .isInstanceOf(InvalidGameOperationException.class);
    }

    @Test
    void invalidPayloadsAreRejectedWithBadRequest() throws Exception {
        UUID playerId = UUID.randomUUID();
        for (String body : new String[]{"{}", "{\"gameType\":\" \"}", "{\"gameType\":\"tictactoe\",\"boardSize\":-1000}"}) {
            mockMvc.perform(post("/games")
                            .header("Authorization", "Bearer " + tokenFor(playerId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        Game game = gameService.createGame(new GameCreationParams("tictactoe", null, null, null), playerId);
        move(game.getId(), playerId, "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void ticTacToeMoveStillWorksWithoutSource() throws Exception {
        UUID playerId = UUID.randomUUID();
        Game game = gameService.createGame(new GameCreationParams("tictactoe", null, null, null), playerId);

        move(game.getId(), playerId, "{\"target\":{\"x\":1,\"y\":1}}")
                .andExpect(status().isOk());

        assertThat(gameService.getGame(game.getId()).getBoard()).containsKey(new CellPosition(1, 1));
    }

    private org.springframework.test.web.servlet.ResultActions move(UUID gameId, UUID playerId, String body) throws Exception {
        return mockMvc.perform(post("/games/" + gameId + "/moves")
                .header("Authorization", "Bearer " + tokenFor(playerId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Game newTaquin(UUID playerId) {
        Game created = gameService.createGame(new GameCreationParams("15 puzzle", null, null, null), playerId);
        return gameService.getGame(created.getId());
    }

    private static CellPosition emptyCell(Game game) {
        int size = game.getBoardSize();
        return IntStream.range(0, size * size)
                .mapToObj(i -> new CellPosition(i % size, i / size))
                .filter(position -> !game.getBoard().containsKey(position))
                .findFirst().orElseThrow();
    }

    private static String json(CellPosition position) {
        return "{\"x\":" + position.x() + ",\"y\":" + position.y() + "}";
    }

    private String tokenFor(UUID userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", "ROLE_USER")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
