package fr.campus.SquareGameUsers;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User createUser(UserCreationParams params) {
        User user = new User();
        user.id = UUID.randomUUID();
        user.name = params.name();
        return userDao.save(user);
    }

    @Override
    public User getUser(UUID userId) {
        return userDao.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
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
