package fr.campus.SquareGames.game;

import java.util.Set;
import java.util.UUID;

/**
 * Paramètres reçus dans le corps de {@code POST /games}.
 * <p>
 * Seul {@code gameType} est obligatoire. {@code playerCount}, {@code boardSize} et
 * {@code opponentIds} peuvent être {@code null} : le {@link GamePlugin} applique alors les
 * valeurs par défaut du jeu et génère des adversaires aléatoires.
 */
public record GameCreationParams(String gameType, Integer playerCount, Integer boardSize, Set<UUID> opponentIds) {
}
