package fr.campus.SquareGames;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, String> {

}
