package fr.campus.SquareGames.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthFlowTest {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void gameEndpointsRequireAValidToken() throws Exception {
        mockMvc.perform(get("/games/mine"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/games/mine").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validTokenIdentifiesThePlayerCreatingAGame() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, "ROLE_USER");

        String body = mockMvc.perform(post("/games")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameType\":\"tictactoe\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String gameId = body.replace("\"", "").trim();

        mockMvc.perform(get("/games/" + gameId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(userId.toString())));

        mockMvc.perform(get("/games/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(gameId)));
    }

    private String tokenFor(UUID userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000))
                .signWith(key)
                .compact();
    }
}
