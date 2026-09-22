package fr.campus.SquareGameUsers.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(UserCreationParams params) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.name = params.name();
        user.password = passwordEncoder.encode(params.password());
        user.role = "ROLE_USER";
        return userDao.save(user);
    }

    @Override
    public User getUser(UUID userId) {
        return userDao.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public void deleteUser(UUID userId) {
        userDao.deleteById(userId);
    }

    @Override
    public boolean isValid(UUID userId) {
        return userDao.existsById(userId);
    }
}
