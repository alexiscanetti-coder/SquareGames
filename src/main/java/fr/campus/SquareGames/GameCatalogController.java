package fr.campus.SquareGames;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
public class GameCatalogController {

    private final List<GamePlugin> gamePlugins;

    public GameCatalogController(List<GamePlugin> gamePlugins) {
        this.gamePlugins = gamePlugins;
    }

    /**
     * Catalogue des jeux disponibles. Le libellé de chaque jeu est traduit dans la langue
     * demandée par l'en-tête HTTP {@code Accept-Language} (résolue par Spring dans {@code locale}).
     */
    @GetMapping("/games")
    public List<GameInfo> getGames(Locale locale) {
        return gamePlugins.stream()
                .map(plugin -> new GameInfo(plugin.getGameFactoryId(), plugin.getName(locale)))
                .toList();
    }
}
