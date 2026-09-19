# Kotlin / Android rules (android-compose-game, android-compose-app)

Toolchain (pinned in the templates): Gradle 9.6.0, AGP 9.4.0 (built-in
Kotlin: never apply `org.jetbrains.kotlin.android`), Kotlin 2.4.20, Compose
BOM 2026.09.00, Material 3, Hilt 2.60.1 via KSP 2.3.12, JDK 17, compileSdk
36, targetSdk 36, minSdk 26.

Modules:
- Game template: `:game-core` (pure Kotlin JVM, no Android; GameState, Move,
  GameRules, GameReducer, PuzzleGenerator, PuzzleValidator, Solver,
  ScoringEngine, FuzzHarness; immutable data classes; pure reducers; seeded
  randomness only), `:game-data`, `:game-ui`, `:app`.
- App template: `:app`, `:core-ui` (theme, navigation), `:core-data`
  (DataStore, Retrofit scaffold, repositories), `:feature-*` (one per
  screen group, each with `ui/`, `domain/`, `data/` packages as needed).

Architecture: unidirectional data flow; ViewModel exposes one
`StateFlow<UiState>` and one `SharedFlow<UiEvent>`; UI sends intents via
`onIntent(Intent)`. Repositories behind interfaces; offline-first where
data is cached. Structured concurrency only; dispatchers injected; no
`GlobalScope`. Hilt constructor injection only.

Build: Gradle Kotlin DSL, version catalog, convention plugins in
`build-logic/`. Quality: `./gradlew check` runs ktlintCheck, detekt, lint,
test, koverVerify (and the fuzz test for games). Forbidden without
code-auditor approval: `@Suppress`, baselines, `@Ignore`, disabled tests,
`!!` in production code, reflection hacks.
