package fr.campus.SquareGameUsers.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    public UUID id;
    public String name;
    @JsonIgnore
    public String password;
}
