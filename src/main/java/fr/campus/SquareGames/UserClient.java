package fr.campus.SquareGames;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(RestClient.Builder builder, @Value("${users.service.url}") String usersServiceUrl) {
        this.restClient = builder.baseUrl(usersServiceUrl).build();
    }

    public boolean isValid(UUID userId) {
        Boolean valid = restClient.get()
                .uri("/users/{id}/valid", userId)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(valid);
    }
}
