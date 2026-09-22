package fr.campus.SquareGames.game;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class GameEntity {
    @Id
    public String id;
    public String factoryId;
    public int boardSize;
    public String playerIds;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "game_id")
    public List<GameTokenEntity> tokens;
}