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

(les DAO `jpa`, `jdbc` et `in-memory` sont interchangeables et fonctionnent avec `h2` comme avec `postgres`)

## Authentification

Toutes les requêtes de jeu exigent un JWT dans l'entête `Authorization: Bearer <token>`. Ce token
est délivré par l'application [`SquareGameUsers`](SquareGameUsers/README.md) (port `8081`) via
`POST /auth/login`, et validé localement par cette application grâce au secret partagé `jwt.secret`
(même valeur dans les deux `application.properties`) : aucun appel réseau vers `SquareGameUsers`.

Accessibles sans token : `GET /heartbeat` et la documentation Swagger.

## Jouer un coup

`POST /games/{gameId}/moves` avec un corps `{"source": {"x": 0, "y": 1}, "target": {"x": 1, "y": 1}}` :

- `target` : case d'arrivée (obligatoire) ;
- `source` : position du jeton à déplacer, facultative au Morpion et au Puissance 4 (le jeton est
  pris dans la réserve du joueur), mais obligatoire dès que plusieurs jetons du plateau peuvent
  atteindre `target`, comme au Taquin (sinon : `400`).

Le Taquin se crée avec `"gameType": "15 puzzle"` et uniquement en 4×4.

## Tests

```
./mvnw test
```
