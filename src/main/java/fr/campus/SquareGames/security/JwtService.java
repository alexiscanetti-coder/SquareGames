package fr.campus.SquareGames.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

// Validates JWTs issued by the companion SquareGameUsers app; never issues tokens itself.
@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Signature, expiration, subject and role are all checked from a single parse of the token.
    public Optional<JwtPrincipal> authenticate(String token) {
        try {
            Claims claims = parseClaims(token);
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            if (claims.getExpiration() == null || subject == null || role == null || role.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new JwtPrincipal(UUID.fromString(subject), role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
