package fr.campus.SquareGameUsers;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    User save(User user);
    Optional<User> findById(UUID id);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
