package fr.campus.SquareGameUsers.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.campus.SquareGameUsers.user.User;
import fr.campus.SquareGameUsers.user.UserCreationParams;
import fr.campus.SquareGameUsers.user.UserDao;
import fr.campus.SquareGameUsers.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtLoginFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginThenAccessProtectedEndpointWithToken() throws Exception {
        var user = userService.createUser(new UserCreationParams("alice", "s3cret!"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/" + user.id))
                .andExpect(status().isForbidden());

        String token = login("alice", "s3cret!");

        mockMvc.perform(get("/users/" + user.id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alice")));
    }

    @Test
    void regularUserCannotAccessAnotherProfileOrAdminEndpoints() throws Exception {
        var alice = userService.createUser(new UserCreationParams("alice2", "s3cret!"));
        var bob = userService.createUser(new UserCreationParams("bob2", "s3cret!"));
        String aliceToken = login("alice2", "s3cret!");

        mockMvc.perform(get("/users/" + bob.id).header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/users/" + bob.id).header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAndDeleteAnyUser() throws Exception {
        var bob = userService.createUser(new UserCreationParams("bob3", "s3cret!"));
        seedAdmin("admin3", "s3cret!");
        String adminToken = login("admin3", "s3cret!");

        mockMvc.perform(get("/users/" + bob.id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bob3")));

        mockMvc.perform(delete("/users/" + bob.id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, LoginResponse.class).token();
    }

    private void seedAdmin(String name, String password) {
        User admin = new User();
        admin.id = UUID.randomUUID();
        admin.name = name;
        admin.password = passwordEncoder.encode(password);
        admin.role = "ROLE_ADMIN";
        userDao.save(admin);
    }
}
