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

Single Spring Boot module, packages organized **by feature**, not by layer: `fr.campus.SquareGameUsers` holds only `SquareGameUsersApplication` (the entry point — `@SpringBootApplication`'s component scan covers every sub-package from there), and each feature gets its own sub-package holding its controller/service/DAO/entity together (currently just `fr.campus.SquareGameUsers.user`; a new feature adds a sibling sub-package, it doesn't reshuffle `user`).

Within a feature package, same layering pattern as the companion game app: a **domain interface**, one `@Service`/`@Component` implementation, and a `@RestController` that constructor-injects the interface (never the impl).

- **`User`** — the JPA `@Entity` (table `users` — `user` is a reserved word in H2/SQL, hence the explicit `@Table(name = "users")`) and, since there is no separate engine here, also the type returned directly by the API (public fields, Jackson-serialized as-is — the same "no output DTO yet" simplification the game app makes for `Game`/`GameInfo`). Carries `password` (`@JsonIgnore`'d, BCrypt-hashed by `UserServiceImpl` before `save`) and `role` (a plain string, `"ROLE_USER"` or `"ROLE_ADMIN"` — matches Spring Security's `hasRole(...)` prefix convention directly, no enum).
- **`UserRepository`** — plain `JpaRepository<User, UUID>` plus `findByName`.
- **`UserDao`** / **`JpaUserDao`** — thin DAO abstraction over `UserRepository` (`save`, `findById`, `findByName`, `findAll`, `deleteById`, `existsById`), mirroring `GameDao`/`JpaGameDao` in the game app. Only one implementation exists (no in-memory/JDBC variants) since the exercise didn't ask for swappable persistence here. `findByName` exists solely for `security.UserDetailsServiceImpl` and `AuthController` to look users up by login username (there's no separate `username` field — `name` doubles as it).
- **`UserService`** / **`UserServiceImpl`** — constructor-injects `UserDao`. `createUser` generates the `UUID` server-side (the id is never client-supplied, so it's the source of truth for player ids the game app trusts) and always assigns `"ROLE_USER"` — there is no self-service or API path to create a `ROLE_ADMIN` user; one has to be seeded directly via `UserDao`/SQL (e.g. as `JwtLoginFlowTest` does).
- **`UserController`** — `POST /users`, `GET /users` (admin-only, list all), `GET /users/{userId}`, `DELETE /users/{userId}`, `GET /users/{userId}/valid`. See authorization rules below.
- **`UserNotFoundException`** — `@ResponseStatus(NOT_FOUND)`, thrown by `getUser`/`GET /users/{userId}` only. `GET /users/{userId}/valid` never throws — it always returns a plain `boolean` (`existsById`), because it exists specifically for the game app to *check* a player id without needing 404 handling.

### Security — JWT auth + roles (`fr.campus.SquareGameUsers.security`)

Stateless JWT auth guards everything except login: `SecurityConfig` disables CSRF and sessions, permits `POST /auth/login`, requires authentication on `anyRequest()` — **including `POST /users`**, so there's no open self-service registration; a user has to already exist before anyone can log in — and carries `@EnableMethodSecurity` so `@PreAuthorize` on controller methods is honored.

- **`JwtService`** — `io.jsonwebtoken` (`jjwt-api`/`impl`/`jackson`) wrapper: `generateToken(UUID userId, String role)` puts the user's **id as the JWT subject** and `role` as a claim (deliberately not the username — the subject is what callers, including the companion game app, need to identify *who*, not *what to call them*); `extractUserId`/`extractRole`/`isTokenValid` read it back. Signing key comes from `jwt.secret` in `application.properties` (a committed dev-only placeholder — a real deployment must inject it via env var instead, and **must stay byte-for-byte identical to `jwt.secret` in the companion SquareGames app** — both apps sign/verify with the same HMAC key, there's no key exchange).
- **`UserDetailsServiceImpl`** — loads a Spring Security `UserDetails` via `UserDao.findByName`, with `user.role` (already in `ROLE_*` form) passed straight through as the single granted authority. Used **only** at login time, inside `AuthenticationManager.authenticate(...)` — it is a DB read gated by password verification, not a per-request cost.
- **`AuthController`** — `POST /auth/login` takes `LoginRequest(username, password)`, authenticates via `AuthenticationManager` (this is what triggers `UserDetailsServiceImpl` + BCrypt check), then re-fetches the `User` via `UserDao.findByName` to pull `id`/`role` for `jwtService.generateToken(...)`. Returns `LoginResponse(token)` on success or a bare `401` on `AuthenticationException` — via `ResponseEntity`, not by throwing `ResponseStatusException`/letting Spring Security's `.exceptionHandling()` handle it: a thrown exception triggers Tomcat's `/error` dispatch, which then gets re-evaluated by the *same* security filter chain and gets turned into a `403` before it reaches the client. Keep returning `ResponseEntity` directly for any future auth-adjacent endpoint that needs a specific status code.
- **`JwtAuthenticationFilter`** — `OncePerRequestFilter` registered before `UsernamePasswordAuthenticationFilter`; reads `Authorization: Bearer <token>`, and on a valid token builds the `Authentication` **entirely from the token's claims** (principal = `userId.toString()`, authority = the `role` claim) — no DB/`UserDetailsService` call per request, by design (see companion-app note below for why this matters more there). Silently no-ops (leaves the request unauthenticated) on a missing/invalid header rather than rejecting outright, since `anyRequest().authenticated()` is what actually enforces the 401/403. Because the principal is the id string, `Authentication#getName()` returns it directly — that's what `@PreAuthorize` compares against for "own profile" checks.
- **`UserController`** `@PreAuthorize` rules: `GET /users` and `DELETE /users/{userId}` → `hasRole('ADMIN')`; `GET /users/{userId}` → `hasRole('ADMIN') or authentication.name == #userId.toString()` (a `ROLE_USER` can only read their own profile). `POST /users` and `GET /users/{userId}/valid` are unrestricted beyond plain authentication.

`JwtLoginFlowTest` (MockMvc, full Spring context) is the reference test for this flow: bad credentials → 401, protected endpoint without a token → 403, login → token → own profile → 200, a `ROLE_USER` token hitting another user's profile / `GET /users` / `DELETE` → 403, and a seeded `ROLE_ADMIN` user succeeding at all three.

### Companion application: SquareGames

`~/IdeaProjects/SquareGames` is the **separate** Spring Boot game engine app (its own Maven project, developed independently), running alongside this app on port `8080` (vs. `8081` here). It has its own JWT-validating filter, sharing this app's `jwt.secret` — it no longer calls `GET /users/{id}/valid` over the network per request; it trusts the id/role embedded in the JWT this app issued, validated locally. `GET /users/{id}/valid` itself still exists (unused by the game app now, but not removed — it's a harmless, generically useful existence check) and still requires authentication like every other endpoint.
