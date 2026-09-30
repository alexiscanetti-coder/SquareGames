package fr.campus.SquareGames.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.Set;
import java.util.UUID;

// Game-specific ranges (e.g. board size) are checked by each game's engine factory.
public record GameCreationParams(
        @NotBlank String gameType,
        @Positive Integer playerCount,
        @Positive Integer boardSize,
        Set<UUID> opponentIds) {
}
