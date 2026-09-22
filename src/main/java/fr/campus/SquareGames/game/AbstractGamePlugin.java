package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.InconsistentGameDefinitionException;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import org.springframework.context.MessageSource;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Base commune aux {@link GamePlugin} : porte la {@link GameFactory} du moteur, les valeurs par
 * défaut (injectées par les sous-classes via {@code @Value}) et la résolution i18n du libellé.
 * Chaque sous-classe concrète est un {@code @Component} qui fournit sa factory, ses défauts et le {@link MessageSource}.
 */
public abstract class AbstractGamePlugin implements GamePlugin {

    private final GameFactory factory;
    private final int defaultPlayerCount;
    private final int defaultBoardSize;
    private final MessageSource messageSource;

    protected AbstractGamePlugin(GameFactory factory, int defaultPlayerCount, int defaultBoardSize,
                                 MessageSource messageSource) {
        this.factory = factory;
        this.defaultPlayerCount = defaultPlayerCount;
        this.defaultBoardSize = defaultBoardSize;
        this.messageSource = messageSource;
    }

    @Override
    public String getGameFactoryId() {
        return factory.getGameFactoryId();
    }

    @Override
    public String getName(Locale locale) {
        String code = "game.name." + factory.getGameFactoryId().replace(' ', '_');
        return messageSource.getMessage(code, null, factory.getGameFactoryId(), locale);
    }

    @Override
    public Game createGame(GameCreationParams params, UUID creatorId) {
        int boardSize = params.boardSize() != null ? params.boardSize() : defaultBoardSize;
        Set<UUID> playerIds = new LinkedHashSet<>();
        playerIds.add(creatorId);
        if (params.opponentIds() != null) {
            playerIds.addAll(params.opponentIds());
        } else {
            int playerCount = params.playerCount() != null ? params.playerCount() : defaultPlayerCount;
            while (playerIds.size() < playerCount) {
                playerIds.add(UUID.randomUUID());
            }
        }
        return factory.createGame(boardSize, playerIds);
    }

    @Override
    public Game restoreGame(UUID gameId, int boardSize, List<UUID> players,
                             Collection<TokenPosition<UUID>> boardTokens,
                             Collection<TokenPosition<UUID>> removedTokens) {
        try {
            return factory.createGameWithIds(gameId, boardSize, players, boardTokens, removedTokens);
        } catch (InconsistentGameDefinitionException e) {
            throw new InvalidGameOperationException(e.getMessage());
        }
    }
}
