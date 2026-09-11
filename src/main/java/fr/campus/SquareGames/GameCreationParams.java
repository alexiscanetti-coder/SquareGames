package fr.campus.SquareGames;

/**
 * Paramètres reçus dans le corps de {@code POST /games}.
 * <p>
 * Seul {@code gameType} est obligatoire. {@code playerCount} et {@code boardSize} peuvent être
 * {@code null} : le {@link GamePlugin} applique alors les valeurs par défaut du jeu.
 */
public record GameCreationParams(String gameType, Integer playerCount, Integer boardSize) {
}