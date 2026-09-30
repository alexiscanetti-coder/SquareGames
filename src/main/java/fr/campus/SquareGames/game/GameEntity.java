package fr.campus.SquareGames.game;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import java.util.List;

@Entity
public class GameEntity {
    @Id
    public String id;
    public String factoryId;
    public int boardSize;
    // One row per player (instead of a comma-separated string) so games can be looked up by player in SQL.
    // Not named game_players: that table belongs to the hand-written JDBC schema (schema.sql).
    @ElementCollection
    @CollectionTable(name = "game_entity_players",
            joinColumns = @JoinColumn(name = "game_id"),
            indexes = @Index(columnList = "player_id"))
    @Column(name = "player_id")
    @OrderColumn(name = "player_order")
    public List<String> playerIds;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "game_id")
    public List<GameTokenEntity> tokens;
}
