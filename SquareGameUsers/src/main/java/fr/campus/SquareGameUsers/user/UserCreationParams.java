package fr.campus.SquareGameUsers.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// BCrypt ignores anything beyond 72 bytes, hence the password upper bound.
public record UserCreationParams(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(min = 6, max = 72) String password) {
}
