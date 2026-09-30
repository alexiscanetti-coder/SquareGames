package fr.campus.SquareGameUsers.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String role) {
}
