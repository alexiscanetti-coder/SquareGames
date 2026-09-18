package fr.campus.SquareGames;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@Tag(name = "Catalogue", description = "Types de jeux disponibles")
public class GameCatalogController {

    private final List<GamePlugin> gamePlugins;

    public GameCatalogController(List<GamePlugin> gamePlugins) {
        this.gamePlugins = gamePlugins;
    }

    @Operation(summary = "Lister les types de jeux disponibles",
            description = "Libellé localisé selon l'entête Accept-Language.")
    @GetMapping("/games")
    public List<GameInfo> getGames(Locale locale) {
        return gamePlugins.stream()
                .map(plugin -> new GameInfo(plugin.getGameFactoryId(), plugin.getName(locale)))
                .toList();
    }
}
