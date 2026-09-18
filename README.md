# SquareGames

API de gestion de parties (Tic-tac-toe, Puissance 4, Taquin), en Spring Boot.

## Démarrage

```
./mvnw spring-boot:run
```

L'application démarre sur `http://localhost:8080` (profils par défaut : `jpa,h2`, une base H2 en
mémoire, sans dépendance externe).

Documentation interactive de l'API : `http://localhost:8080/swagger-ui/index.html`.

### Utiliser Postgres au lieu de H2

```
docker run --name sg-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=squaregames -p 5432:5432 -d postgres:16
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=jpa,postgres
```

(le profil `jdbc`, écrit à la main, ne fonctionne qu'avec `postgres` — voir `application-jdbc.properties`)

## Application de gestion des utilisateurs

Les requêtes de jeu (`POST /games`, `GET /games/mine`, `POST /games/{gameId}/moves`) exigent un
entête `X-UserId` identifiant le joueur, validé auprès de l'application
[`SquareGameUsers`](../SquareGameUsers), qui doit tourner en parallèle sur `http://localhost:8081`
(voir son propre `README.md` pour la démarrer). L'URL de ce service est configurable via
`users.service.url` dans `application.properties`.

## Tests

```
./mvnw test
```
