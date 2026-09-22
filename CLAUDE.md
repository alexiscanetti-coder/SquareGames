# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Writing code — keep it minimal

Does this need to exist? Speculative need = skip it. (YAGNI)

1. Already in this codebase? Reuse the helper, util, or pattern that already lives here.
2. Does the standard library do it? Use it.
3. Native platform feature covers it? `<input type="date">` over a picker lib.
4. Already-installed dependency solves it? Use it. Don't add a new one.
5. Can it be one line? One line.
6. Only then: the minimum code that works.

## Commands

Uses the Maven wrapper; no local Maven needed.

- Build / compile: `./mvnw compile`
- Run all tests: `./mvnw test`
- Run a single test: `./mvnw test -Dtest=SquareGamesApplicationTests#contextLoads`
- Run the app: `./mvnw spring-boot:run` (Tomcat on port 8080)
- Package a jar: `./mvnw package` (runs tests; produces `target/SquareGames-0.0.1-SNAPSHOT.jar`)

Add `-o` to run offline once dependencies are cached in `~/.m2`.

## Toolchain

- Spring Boot **4.2.0-SNAPSHOT** (pre-release), pulled from the `spring-snapshots` repository declared in `pom.xml`.
- `pom.xml` targets Java 21; a newer JDK on the machine also works.
- Web stack is `spring-boot-starter-webmvc` (servlet MVC, not WebFlux); tests use `spring-boot-starter-webmvc-test`.
- API documented via `springdoc-openapi-starter-webmvc-ui` (Swagger UI at `/swagger-ui/index.html`, OpenAPI JSON at `/v3/api-docs`) — pin an explicit version compatible with this Boot 4 snapshot, Maven Central's "latest" tag can lag.

## External engine dependency

`fr.le-campus-numerique.square-games:engine:1.0-SNAPSHOT` is a third-party jar resolved from the local `~/.m2` repository (no public remote for it). Its sources jar is also in `~/.m2` — read it there when you need engine behavior details.

Engine model (all game logic lives here, not in this repo):
- `GameFactory` — one per game type. Implementations and their `getGameFactoryId()`: `TicTacToeGameFactory` = `"tictactoe"`, `ConnectFourGameFactory` = `"connect4"`, `TaquinGameFactory` = `"15 puzzle"`. Each exposes `getPlayerCountRange()` / `getBoardSizeRange(playerCount)` and `createGame(playerCount, boardSize)`.
- `Game` — a live game instance: `getStatus()` (`SETUP` / `ONGOING` / `TERMINATED`), `getCurrentPlayerId()` (winner once terminated, `null` on a draw), `getBoard()` (`Map<CellPosition, Token>`), `getRemainingTokens()`, `getRemovedTokens()`.
- Players are bare `UUID`s — there is no player model.
- The **only** way to change game state is `Token.moveTo(CellPosition)`; a move is legal iff the target is in `Token.getAllowedMoves()` (throws `InvalidPositionException` otherwise). For Tic-tac-toe / Connect Four only the next unplaced token in turn order has non-empty allowed moves.
- Engine classes carry `jakarta.validation` constraint annotations, but there is no validation provider on the classpath, so they are inert. The `OptionalValidatorFactoryBean ... NoProviderFoundException` line at startup is expected and harmless.

## Architecture

Single Spring Boot module, packaged by feature under `fr.campus.SquareGames`. Entry point: `SquareGamesApplication`, kept at the package root — `@SpringBootApplication`'s default component scan covers every subpackage below it, so no `@ComponentScan` override is needed. Feature packages:

- `heartbeat` — `HeartbeatSensor` / `RandomHeartbeat` / `HeartbeatController`.
- `catalog` — `GameCatalogController` / `GameInfo` (imports `game.GamePlugin`, the one cross-package reference in the codebase).
- `game` — everything else: `GameController`, `GameService`/`GameServiceImpl`, the `GamePlugin` extension point (below), the three `GameDao` implementations and JPA entities, and the game-related exceptions. Not further split — it's one cohesive feature (creating, persisting and playing a game), and splitting it by layer (controller/service/dao) would cut across that without adding clarity.
- `security` — JWT validation, shared with the companion SquareGameUsers app (see below).

The codebase follows one consistent pattern per feature: a **domain interface**, one `@Service`/`@Component` implementation, and a `@RestController` that constructor-injects the interface (never the impl). Current vertical slices:

- **Heartbeat** — `GET /heartbeat` returns an `int`.
- **Game catalog** — `GET /games` returns the game catalog (id + localized name) as `List<GameInfo>`.
- **Game instances** — create a game, read its state, list a player's ongoing games, play a move. Two controllers both own `/games` without conflict: `GameCatalogController` maps `GET /games`, `GameController` maps `POST /games`, `GET /games/{gameId}`, `GET /games/mine`, `POST /games/{gameId}/moves` — a new games endpoint must keep using a distinct method/path combo to avoid an ambiguous-mapping startup failure (`/games/mine` coexists with `/games/{gameId}` because Spring MVC prefers an exact literal segment over a path variable at the same position).

`POST /games`, `GET /games/mine` and `POST /games/{gameId}/moves` identify the calling player from the validated JWT (an `Authentication` controller-method parameter, `UUID.fromString(authentication.getName())`) rather than a client-supplied header — see the Security section below. `GET /games/{gameId}` (read a single game) doesn't use the identity at all, but like every other endpoint it still requires *some* valid `Authorization: Bearer` token, since `SecurityConfig` here has no `permitAll` routes.

`GameController` and `GameCatalogController` deliberately return engine types (`Game`, `GameInfo` wraps engine data, no game-state DTO) — `Game`/`Token`/`CellPosition` are serialized by Jackson as-is (`Game.getBoard()` keys, a `Map<CellPosition, Token>`, render as the record's `toString()`, e.g. `"CellPosition[x=0, y=0]"`). This was a deliberate step-by-step simplification, not a final design; introducing output DTOs would replace this.

### The `GamePlugin` extension point

`GameServiceImpl` and `GameCatalogController` never touch the engine's `GameFactory` directly — they depend only on `GamePlugin`:

- **`GamePlugin`** (interface) — `getGameFactoryId()` (the id used as `gameType` in the API), `getName(Locale)` (localized label), `createGame(GameCreationParams, UUID creatorId)` (tolerant creation: falls back to per-game defaults when `playerCount`/`boardSize` are `null`; the caller — `X-UserId` — always becomes a player, `opponentIds` when given fill the rest, otherwise random `UUID`s do), `restoreGame(...)` (rebuilds a `Game` from persisted state via the engine's `GameFactory.createGameWithIds`, used by the JDBC/JPA DAOs).
- **`AbstractGamePlugin`** — holds the engine `GameFactory`, the default player count/board size, and a `MessageSource`; implements `getName` (message code `game.name.<id>`, spaces replaced with `_`, e.g. `game.name.15_puzzle`) and the default-filling `createGame`/`restoreGame`.
- **`TicTacToePlugin` / `ConnectFourPlugin` / `TaquinPlugin`** — one `@Component` per game type, each a thin subclass whose constructor takes `MessageSource` plus two `@Value`-injected defaults (e.g. `${game.tictactoe.default-player-count}`, `${game.tictactoe.default-board-size}`) and passes its concrete `GameFactory` to `super(...)`.
- **`GameServiceImpl`** — constructor-injects `List<GamePlugin>` (Spring auto-collects every `@Component` implementing it) and indexes it into a `Map<String, GamePlugin>` keyed by `getGameFactoryId()`. `GameCatalogController` injects the same `List<GamePlugin>` directly.

**Adding a new game type** = one new `@Component` class extending `AbstractGamePlugin` (wrapping the engine's `GameFactory` for that game) + its two `default-player-count`/`default-board-size` properties + its `game.name.<id>` message keys in `messages*.properties`. Nothing else changes — `GameServiceImpl`, `GameController`, `GameCatalogController` stay untouched.

### Errors

Business exceptions (not `ResponseStatusException`) carry the HTTP mapping via `@ResponseStatus`, so the service layer stays free of any web dependency:

- `GameNotFoundException` → `404` (unknown `gameId`)
- `InvalidGameOperationException` → `400` (unknown `gameType`, player count/board size out of range, illegal move)
- `ForbiddenMoveException` → `403` (the JWT-derived player calling `POST /games/{gameId}/moves` is not `Game#getCurrentPlayerId()`)

There used to be an `UnknownUserException`/`401` here too, thrown when a client-supplied `X-UserId` didn't correspond to a real user (checked via a network call to the companion app). It's gone now that identity comes from a signed JWT: an invalid/expired token is rejected by `SecurityConfig` before the controller ever runs, and a validly-signed token is trusted as referring to a real user by construction (SquareGameUsers only ever signs one for a `User` it just looked up).

`server.error.include-message=always` in `application.properties` puts the exception message in the JSON error body — dev convenience, keep it in mind if this API is ever exposed publicly.

### i18n

`messages.properties` (base = French, the fallback when no matching locale file exists) and `messages_en.properties`, keyed `game.name.<factory id, spaces→_>`. `GameCatalogController#getGames` takes a `Locale` controller-method parameter, resolved by Spring's default `AcceptHeaderLocaleResolver` from the `Accept-Language` header. `spring.messages.fallback-to-system-locale=false` is set so an unmatched language falls back to the base bundle (French) rather than the JVM's system locale.

## Security — JWT validation (`fr.campus.SquareGames.security`)

This app never issues tokens, only validates ones minted by the companion SquareGameUsers app — see the Security section in that app's `CLAUDE.md` for how a token is built (subject = user id, `role` claim).

- **`JwtService`** — a validate-only trim of the Users app's class: `isTokenValid(token)`, `extractUserId(token)`, `extractRole(token)`. Signing key comes from `jwt.secret` in `application.properties`, which **must be byte-for-byte identical** to `jwt.secret` in SquareGameUsers — there is no key-exchange mechanism, just a shared committed dev placeholder (a real deployment injects both from the same secret store).
- **`JwtAuthenticationFilter`** — same shape as the Users app's filter: reads `Authorization: Bearer <token>`, and on a valid token builds an `Authentication` straight from its claims (principal = `userId.toString()`, authority = the `role` claim) — no call back to SquareGameUsers. This is the whole point of the JWT switch: previously every `X-UserId`-bearing request paid a synchronous network round-trip to `GET /users/{id}/valid`; now a signature check (local, in-process) is what stands in for that trust.
- **`SecurityConfig`** — stateless sessions, CSRF disabled, `anyRequest().authenticated()` with no `permitAll` routes at all (this app has no login endpoint of its own — tokens only ever come from SquareGameUsers).

`JwtAuthFlowTest` is the reference test: no/garbage token → 403 on a protected endpoint; a token minted in-test with the same secret (mirroring what SquareGameUsers would issue) → `POST /games` succeeds and the created game's player id matches the token's subject.

Note: Spring Boot's `UserDetailsServiceAutoConfiguration` still logs a "Using generated security password" line on every startup — harmless. It fires because this app defines no `UserDetailsService`/`AuthenticationManager` bean of its own (it doesn't need one; `JwtAuthenticationFilter` is the only thing that ever populates `SecurityContextHolder` here), and Boot's default fallback bean creation doesn't know that.

## Tests

`SquareGamesApplicationTests` only verifies the Spring context loads (`@SpringBootTest`). Mockito (inline mock maker) is on the test classpath via the webmvc-test starter.

## Companion application: user management

`~/IdeaProjects/SquareGameUsers` is a **separate** Spring Boot application (its own Maven project, not a module of this repo, its own Claude Code session), dedicated to user management. It runs alongside this app on `server.port=8081` (this app: `8080`, set explicitly in `application.properties`). It's also where JWTs come from — `POST /auth/login` there is the only place a token is minted; this app only ever validates them (see Security section above).

`GameDao#findByPlayerId(UUID)` (added alongside the original `X-UserId` integration, implemented in all three DAOs) backs `GET /games/mine` — `GameServiceImpl` filters its result down to `GameStatus.ONGOING`.
