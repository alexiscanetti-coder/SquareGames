package fr.campus.SquareGameUsers.user;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User createUser(UserCreationParams params);

    User getUser(UUID userId);

    List<User> getAllUsers();

    void deleteUser(UUID userId);

    boolean isValid(UUID userId);
}
