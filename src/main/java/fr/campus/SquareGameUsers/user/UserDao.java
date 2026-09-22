package fr.campus.SquareGameUsers.user;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByName(String name);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
