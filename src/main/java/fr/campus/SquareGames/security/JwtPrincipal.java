package fr.campus.SquareGames.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String role) {
}
