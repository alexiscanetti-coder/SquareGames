package fr.campus.SquareGames.game;

import java.util.Set;
import java.util.UUID;

public record GameCreationParams(String gameType, Integer playerCount, Integer boardSize, Set<UUID> opponentIds) {
}
