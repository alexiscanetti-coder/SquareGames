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
- Run a single test: `./mvnw test -Dtest=SquareGameUsersApplicationTests#contextLoads`
- Run the app: `./mvnw spring-boot:run` (Tomcat on port 8081)
- Package a jar: `./mvnw package` (runs tests; produces `target/SquareGameUsers-0.0.1-SNAPSHOT.jar`)

Add `-o` to run offline once dependencies are cached in `~/.m2`.

## Toolchain

- Spring Boot **4.2.0-SNAPSHOT** (pre-release), pulled from the `spring-snapshots` repository declared in `pom.xml`.
- `pom.xml` targets Java 21; a newer JDK on the machine also works.
- Web stack is `spring-boot-starter-webmvc` (servlet MVC, not WebFlux); tests use `spring-boot-starter-webmvc-test`.
- Persistence: `spring-boot-starter-data-jpa` (Hibernate) over an in-memory H2 database (`com.h2database:h2`) — no external DB needed to run or test.

## Architecture

Single Spring Boot module, one package `fr.campus.SquareGameUsers`, no sub-packages. Entry point: `SquareGameUsersApplication`.

Same layering pattern as the companion game app: a **domain interface**, one `@Service`/`@Component` implementation, and a `@RestController` that constructor-injects the interface (never the impl).

- **`User`** — the JPA `@Entity` (table `users` — `user` is a reserved word in H2/SQL, hence the explicit `@Table(name = "users")`) and, since there is no separate engine here, also the type returned directly by the API (public fields, Jackson-serialized as-is — the same "no output DTO yet" simplification the game app makes for `Game`/`GameInfo`).
- **`UserRepository`** — plain `JpaRepository<User, UUID>`.
- **`UserDao`** / **`JpaUserDao`** — thin DAO abstraction over `UserRepository` (`save`, `findById`, `deleteById`, `existsById`), mirroring `GameDao`/`JpaGameDao` in the game app. Only one implementation exists (no in-memory/JDBC variants) since the exercise didn't ask for swappable persistence here.
- **`UserService`** / **`UserServiceImpl`** — constructor-injects `UserDao`. `createUser` generates the `UUID` server-side (the id is never client-supplied), so it's the source of truth for player ids the game app later validates.
- **`UserController`** — `POST /users`, `GET /users/{userId}`, `DELETE /users/{userId}`, `GET /users/{userId}/valid`.
- **`UserNotFoundException`** — `@ResponseStatus(NOT_FOUND)`, thrown by `getUser`/`GET /users/{userId}` only. `GET /users/{userId}/valid` never throws — it always returns a plain `boolean` (`existsById`), because it exists specifically for the game app to *check* a player id without needing 404 handling.

### Companion application: SquareGames

`~/IdeaProjects/SquareGames` is the **separate** Spring Boot game engine app (its own Maven project, developed independently). It runs alongside this app on a different port (`8080`, vs. `8081` here). Its game-creation flow calls this app's `GET /users/{id}/valid` to check a player id exists before accepting it. Player ids in that app are bare `UUID`s with no player model of their own — this app's generated `User.id` is what fills that role.
