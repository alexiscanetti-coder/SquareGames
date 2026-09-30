package fr.campus.SquareGames.game;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"jpa", "h2"})
class JpaGameDaoTest extends GameDaoContractTest {
}
