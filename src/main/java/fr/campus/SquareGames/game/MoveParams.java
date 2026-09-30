package fr.campus.SquareGames.game;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import jakarta.validation.constraints.NotNull;

// source is optional: only needed when several tokens already on the board can reach target (e.g. Taquin).
public record MoveParams(CellPosition source, @NotNull CellPosition target) {
}
