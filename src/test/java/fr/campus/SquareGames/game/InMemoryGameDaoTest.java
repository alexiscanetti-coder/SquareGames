package fr.campus.SquareGames.game;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"in-memory", "h2"})
class InMemoryGameDaoTest extends GameDaoContractTest {
}
