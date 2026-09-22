package fr.campus.SquareGameUsers.security;

import fr.campus.SquareGameUsers.user.User;
import fr.campus.SquareGameUsers.user.UserDao;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDao userDao;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserDao userDao) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDao = userDao;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).build();
        }
        User user = userDao.findByName(request.username()).orElseThrow();
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken(user.id, user.role)));
    }
}
