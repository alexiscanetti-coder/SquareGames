package fr.campus.SquareGames.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, String> {

    @Query("SELECT DISTINCT g FROM GameEntity g JOIN g.playerIds p WHERE p = :playerId")
    List<GameEntity> findByPlayerId(@Param("playerId") String playerId);
}
