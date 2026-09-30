package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameServiceImplTest {

    private static final CellPosition TARGET = new CellPosition(1, 1);

    private final GamePlugin plugin = mock(GamePlugin.class);
    private final GameDao gameDao = mock(GameDao.class);
    private final Game game = mock(Game.class);
    private final UUID gameId = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();
    private GameServiceImpl service;

    @BeforeEach
    void setUp() {
        when(plugin.getGameFactoryId()).thenReturn("tictactoe");
        service = new GameServiceImpl(List.of(plugin), gameDao);
        when(gameDao.findById(gameId)).thenReturn(Optional.of(game));
        when(game.getCurrentPlayerId()).thenReturn(player);
        when(game.getRemainingTokens()).thenReturn(List.of());
    }

    @Test
    void createGameRejectsUnknownGameType() {
        assertThatThrownBy(() -> service.createGame(new GameCreationParams("chess", null, null, null), player))
                .isInstanceOf(InvalidGameOperationException.class)
                .hasMessageContaining("chess");
    }

    @Test
    void createGameTurnsEngineArgumentErrorsIntoBadRequests() {
        GameCreationParams params = new GameCreationParams("tictactoe", null, 99, null);
        when(plugin.createGame(params, player)).thenThrow(new IllegalArgumentException("invalid board size"));

        assertThatThrownBy(() -> service.createGame(params, player))
                .isInstanceOf(InvalidGameOperationException.class);
        verify(gameDao, never()).save(game);
    }

    @Test
    void moveIsForbiddenWhenItIsNotThePlayersTurn() {
        assertThatThrownBy(() -> service.move(gameId, new MoveParams(null, TARGET), UUID.randomUUID()))
                .isInstanceOf(ForbiddenMoveException.class);
        verify(gameDao, never()).save(game);
    }

    @Test
    void moveWithoutSourceIsRejectedWhenSeveralBoardTokensCanReachTarget() {
        Token left = tokenAllowedTo(TARGET);
        Token right = tokenAllowedTo(TARGET);
        when(game.getBoard()).thenReturn(Map.of(new CellPosition(0, 1), left, new CellPosition(2, 1), right));

        assertThatThrownBy(() -> service.move(gameId, new MoveParams(null, TARGET), player))
                .isInstanceOf(InvalidGameOperationException.class)
                .hasMessageContaining("source");
    }

    @Test
    void moveWithSourceMovesThatTokenAndSavesTheGame() throws Exception {
        Token left = tokenAllowedTo(TARGET);
        Token right = tokenAllowedTo(TARGET);
        when(game.getBoard()).thenReturn(Map.of(new CellPosition(0, 1), left, new CellPosition(2, 1), right));

        service.move(gameId, new MoveParams(new CellPosition(2, 1), TARGET), player);

        verify(right).moveTo(TARGET);
        verify(left, never()).moveTo(TARGET);
        verify(gameDao).save(game);
    }

    @Test
    void moveWithSourceThatCannotReachTargetIsRejected() {
        Token token = tokenAllowedTo(new CellPosition(0, 1));
        when(game.getBoard()).thenReturn(Map.of(new CellPosition(0, 0), token));

        assertThatThrownBy(() -> service.move(gameId, new MoveParams(new CellPosition(0, 0), TARGET), player))
                .isInstanceOf(InvalidGameOperationException.class);
    }

    @Test
    void moveWithoutSourcePlacesARemainingTokenWhenNoBoardTokenCanMove() throws Exception {
        Token remaining = tokenAllowedTo(TARGET);
        when(game.getBoard()).thenReturn(Map.of());
        when(game.getRemainingTokens()).thenReturn(List.of(remaining));

        service.move(gameId, new MoveParams(null, TARGET), player);

        verify(remaining).moveTo(TARGET);
    }

    @Test
    void listGamesKeepsOnlyOngoingGames() {
        Game finished = mock(Game.class);
        when(game.getStatus()).thenReturn(GameStatus.ONGOING);
        when(finished.getStatus()).thenReturn(GameStatus.TERMINATED);
        when(gameDao.findByPlayerId(player)).thenReturn(List.of(game, finished));

        assertThat(service.listGames(player)).containsExactly(game);
    }

    private static Token tokenAllowedTo(CellPosition position) {
        Token token = mock(Token.class);
        when(token.getAllowedMoves()).thenReturn(Set.of(position));
        return token;
    }
}
