package fr.campus.SquareGameUsers.user;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private final UserDao userDao = mock(UserDao.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserServiceImpl service = new UserServiceImpl(userDao, passwordEncoder);

    @Test
    void createUserHashesThePasswordAndGivesTheUserRole() {
        when(userDao.findByName("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("s3cret!")).thenReturn("hashed");
        when(userDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.createUser(new UserCreationParams("alice", "s3cret!"));

        assertThat(user.id).isNotNull();
        assertThat(user.password).isEqualTo("hashed");
        assertThat(user.role).isEqualTo("ROLE_USER");
    }

    @Test
    void createUserRejectsATakenName() {
        when(userDao.findByName("alice")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.createUser(new UserCreationParams("alice", "s3cret!")))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(userDao, never()).save(any());
    }

    @Test
    void createUserReportsAConcurrentRegistrationAsAConflict() {
        when(userDao.findByName("alice")).thenReturn(Optional.empty());
        when(userDao.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.createUser(new UserCreationParams("alice", "s3cret!")))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void getUserThrowsWhenUnknown() {
        UUID id = UUID.randomUUID();
        when(userDao.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(id)).isInstanceOf(UserNotFoundException.class);
    }
}
