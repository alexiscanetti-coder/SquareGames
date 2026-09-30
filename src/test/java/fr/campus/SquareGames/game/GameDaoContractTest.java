package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Same expectations for every GameDao implementation; each subclass activates one DAO profile.
abstract class GameDaoContractTest {

    @Autowired
    private GameDao gameDao;

    @Autowired
    private GameService gameService;

    @Test
    void findByIdRestoresPlayersTurnAndBoard() {
        UUID creator = UUID.randomUUID();
        UUID opponent = UUID.randomUUID();
        Game game = gameService.createGame(new GameCreationParams("tictactoe", null, null, Set.of(opponent)), creator);
        Game moved = gameService.move(game.getId(), new MoveParams(null, new CellPosition(1, 1)), creator);

        Game restored = gameDao.findById(game.getId()).orElseThrow();

        assertThat(restored.getPlayerIds()).containsExactlyInAnyOrder(creator, opponent);
        assertThat(restored.getCurrentPlayerId()).isEqualTo(moved.getCurrentPlayerId()).isNotEqualTo(creator);
        assertThat(restored.getBoard()).containsOnlyKeys(new CellPosition(1, 1));
    }

    @Test
    void findByIdOfUnknownGameIsEmpty() {
        assertThat(gameDao.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findByPlayerIdReturnsOnlyThatPlayersGames() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID aliceGame1 = gameService.createGame(new GameCreationParams("tictactoe", null, null, null), alice).getId();
        UUID aliceGame2 = gameService.createGame(new GameCreationParams("connect4", null, null, null), alice).getId();
        UUID sharedGame = gameService.createGame(new GameCreationParams("tictactoe", null, null, Set.of(alice)), bob).getId();
        gameService.createGame(new GameCreationParams("tictactoe", null, null, null), bob);

        List<UUID> aliceGames = gameDao.findByPlayerId(alice).stream().map(Game::getId).toList();

        assertThat(aliceGames).containsExactlyInAnyOrder(aliceGame1, aliceGame2, sharedGame);
        assertThat(gameDao.findByPlayerId(UUID.randomUUID())).isEmpty();
    }
}
