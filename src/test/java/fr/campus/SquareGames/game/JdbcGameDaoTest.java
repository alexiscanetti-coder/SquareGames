package fr.campus.SquareGames.game;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"jdbc", "h2"})
class JdbcGameDaoTest extends GameDaoContractTest {
}
