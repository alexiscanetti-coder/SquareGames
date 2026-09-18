package fr.campus.SquareGameUsers;

import java.util.UUID;

public interface UserService {

    User createUser(UserCreationParams params);

    User getUser(UUID userId);

    void deleteUser(UUID userId);

    boolean isValid(UUID userId);
}
