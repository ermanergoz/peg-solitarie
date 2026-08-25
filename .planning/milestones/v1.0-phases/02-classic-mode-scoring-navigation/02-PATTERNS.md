# Phase 2: Classic Mode, Scoring & Navigation - Pattern Map

**Mapped:** 2026-06-28
**Files analyzed:** 30 (new + modified)
**Analogs found:** 26 / 30 (4 genuinely new: nav graph, DataStore repo, authoring solver, datastore wiring)

> Phase 1 shipped a single-screen walking skeleton: pure-Kotlin `:domain` (`Board`/`BoardSpec`/`Boards`/`RulesEngine`/`Move`) + an `:app` UI layer with one **Route → Screen → Leaf** flow (`GameRoute` → `GameScreen` → `BoardCanvas`) over an `@Immutable` `GameUiState` `StateFlow`, Hilt seam in `:app` (`DomainModule`), scoped-constant geometry (`BoardMetrics`), `dimens.xml` + `strings.xml`, and a golden-master harness (`BoardCodec`/`GoldenMasterAssert`). **Almost every Phase-2 file copies an existing Phase-1 analog** — this phase is integration + plumbing, not new algorithms. Package roots: `com.app.pegsolitaire` (`:app`), `com.app.pegsolitaire.domain` (`:domain`).

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `domain/.../scoring/ScoreCalculator.kt` | service (pure domain) | transform | `domain/.../rules/RulesEngine.kt` | exact (stateless `object`) |
| `domain/.../scoring/ScoreParams.kt` | config (constants) | — | `app/.../ui/game/BoardMetrics.kt` / `English33Layout` | exact (scoped consts `object`) |
| `domain/.../scoring/ScoreResult.kt` | model | — | `domain/.../model/Move.kt` | exact (immutable `data class`) |
| `domain/.../model/Boards.kt` (MOD) | model/data | — | `domain/.../model/Boards.kt` (`English33Layout`) | exact (self-pattern) |
| `domain/src/test/.../scoring/ScoreCalculatorTest.kt` | test | — | `domain/src/test/.../model/BoardTest.kt` + `GameViewModelTest` | exact |
| `domain/src/test/.../golden/solutions/*Solution.kt` + replay tests | test (fixture) | batch | `domain/src/test/.../golden/GoldenMasterTest.kt` + `BoardCodec.kt` | exact |
| authoring DFS solver (test/`tools/` source set, NOT shipped) | utility (one-off) | batch/transform | `domain/.../rules/RulesEngine.kt` (consumer) | role-match (no analog) |
| `app/.../data/LevelRepository.kt` (+ `Level`, descriptor) | repository (interface) | CRUD/lookup | `domain/.../rules/RulesEngine.kt` (seam) + `Move.kt` (data) | role-match |
| `app/.../data/ClassicLevelSource.kt` | service (data source) | CRUD/lookup | `domain/.../model/Boards.kt` (catalog data) | role-match |
| `app/.../data/ChallengeLevelSource.kt` (STUB) | service (data source) | — | — | no analog (trivial stub) |
| `app/.../data/OnboardingRepository.kt` | repository (DataStore) | event-driven (Flow) | — | no analog (new dependency) |
| `app/.../di/DataModule.kt` | config (Hilt module) | — | `app/.../di/DomainModule.kt` | exact |
| `app/.../di/DomainModule.kt` (MOD) | config (Hilt module) | — | `app/.../di/DomainModule.kt` | exact (self) |
| `app/.../navigation/AppNavHost.kt` + `Routes.kt` | route (nav graph) | request-response | — (`MainActivity` is insertion point) | no analog (new library) |
| `app/.../MainActivity.kt` (MOD) | provider (host) | — | `app/.../MainActivity.kt` | exact (self) |
| `app/.../ui/home/HomeRoute/Screen/ViewModel/UiState.kt` | controller+component | request-response | `app/.../ui/game/Game{Route,Screen,ViewModel,UiState}.kt` | exact |
| `app/.../ui/classic/ClassicCatalog{Route,Screen,ViewModel,UiState}.kt` | controller+component | request-response | `Game*` quadruple + `BoardCanvas` (preview) | exact |
| `app/.../ui/classic/BoardPreview.kt` | component (leaf) | — | `app/.../ui/components/BoardCanvas.kt` | role-match (read-only variant) |
| `app/.../ui/results/Results{Route,Screen,ViewModel,UiState}.kt` | controller+component | request-response | `Game*` quadruple + `GameOverDialog.kt` (tiered copy) | exact |
| `app/.../ui/onboarding/OnboardingOverlay.kt` | component (overlay) | — | `GameOverDialog.kt` / `RestartConfirmDialog` | role-match |
| `app/.../ui/game/GameViewModel.kt` (MOD) | controller | request-response | `app/.../ui/game/GameViewModel.kt` | exact (self) |
| `app/.../ui/game/GameUiState.kt` (MOD) | model | — | `app/.../ui/game/GameUiState.kt` | exact (self) |
| `app/.../ui/game/GameRoute.kt` (MOD) | controller (route) | event (one-shot) | `app/.../ui/game/GameRoute.kt` | exact (self) |
| `app/.../ui/game/GameScreen.kt` (MOD) | component | — | `app/.../ui/game/GameScreen.kt` | exact (self) |
| `app/.../ui/game/BoardMetrics.kt` (MOD) | config (constants) | — | `app/.../ui/game/BoardMetrics.kt` | exact (self) |
| `app/.../ui/components/BoardGeometry.kt` (MOD) | utility (geometry) | transform | `app/.../ui/components/BoardGeometry.kt` | exact (self) |
| `app/src/main/res/values/dimens.xml` (MOD) | config (resource) | — | `dimens.xml` | exact (self) |
| `app/src/main/res/values/strings.xml` (MOD) | config (resource) | — | `strings.xml` | exact (self) |
| `gradle/libs.versions.toml` + `app/build.gradle.kts` (MOD) | config (build) | — | self | exact (self) |
| `app/src/test/.../ui/{home,classic,results}/*ViewModelTest.kt` | test | — | `app/.../ui/game/GameViewModelTest.kt` | exact |

---

## Pattern Assignments

### `domain/.../scoring/ScoreCalculator.kt` (pure domain service, transform)

**Analog:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/rules/RulesEngine.kt` — the stateless pure-Kotlin `object` authority pattern. Copy its shape exactly: a top-level `object`, pure functions, named constants in a `private companion object` (or sibling `ScoreParams`), **zero `android.*` imports** (the `DomainPurityTest`/ArchUnit gate enforces QUAL-01).

**Package + structure pattern** (`RulesEngine.kt:1-21, 81-89`):
```kotlin
package com.app.pegsolitaire.domain.rules   // → domain.scoring for the new file

import com.app.pegsolitaire.domain.model.Board   // only domain.model imports allowed

object RulesEngine {                 // ← copy: object ScoreCalculator
    fun legalMoves(board: Board): List<Move> = ...
    fun pegCount(board: Board): Int = board.pegCount
}
```

**Target shape** (from RESEARCH Pattern 4; constants live in `ScoreParams`, see below):
```kotlin
object ScoreCalculator {
    fun stars(pegsRemaining: Int): Int = when {
        pegsRemaining <= ScoreParams.THREE_STAR_MAX_PEGS -> 3
        pegsRemaining <= ScoreParams.TWO_STAR_MAX_PEGS -> 2
        else -> ScoreParams.MIN_STARS              // D-01: >3 → 1★, never 0★
    }
    fun score(pegsCleared: Int, elapsedSeconds: Int): Int =
        pegsCleared * ScoreParams.PEG_VALUE +
            maxOf(0, ScoreParams.TIME_BONUS_CAP - elapsedSeconds * ScoreParams.DECAY_PER_SECOND)
}
```
`pegsCleared = initialBoard.pegCount - finalBoard.pegCount`. Wire into Hilt the same way `RulesEngine` is provided (see Shared Patterns → DI). **No magic numbers** — every literal except the `3`/`2`/`0`/`maxOf(0,…)` floor goes through `ScoreParams`.

---

### `domain/.../scoring/ScoreParams.kt` (scoped constants config)

**Analog:** `app/.../ui/game/BoardMetrics.kt` (a scoped-constant `object`) and `Boards.kt`'s `private object English33Layout` (`Boards.kt:14-41`). Copy the **scoped constants object** convention — one `object` per scope, every value a `const val` with a KDoc tying it to its decision (QUAL-03).

**Pattern** (`BoardMetrics.kt:12-50`):
```kotlin
object BoardMetrics {
    /** English-33 occupies a 7×7 lattice. */
    const val GRID_DIMENSION = 7
    /** Resting/jumping peg radius = ratio × cellSize. */
    const val PEG_RADIUS_RATIO = 0.34f
    ...
}
```
Target `ScoreParams` constants (D-01/D-03): `THREE_STAR_MAX_PEGS = 1`, `TWO_STAR_MAX_PEGS = 3`, `MIN_STARS = 1`, `PEG_VALUE`, `TIME_BONUS_CAP`, `DECAY_PER_SECOND` — each KDoc'd with its decision id. Planner sanity-checks the `[ASSUMED]` values from RESEARCH Pattern 4.

---

### `domain/.../scoring/ScoreResult.kt` (immutable model)

**Analog:** `domain/.../model/Move.kt` (`Move.kt:11-15`) — a minimal immutable `data class` with KDoc explaining each field and its invariants.
```kotlin
data class Move(val from: Position, val over: Position, val to: Position)
// → data class ScoreResult(stars, score, pegsRemaining, pegsCleared, elapsedSeconds) — all Int/val
```

---

### `domain/.../model/Boards.kt` (MODIFIED — add 4 board specs)

**Analog:** itself — the existing `English33Layout` + `Boards` pattern (`Boards.kt:14-63`). Each new board copies this exactly: a `private object <Name>Layout` of scoped constants → derived `inPlayPositions` via the double `for` loop → a `BoardSpec` + initial `Board` in the `Boards` object.

**Layout-object pattern to copy** (`Boards.kt:14-41`):
```kotlin
private object English33Layout {
    const val GRID_DIMENSION = 7
    private const val CENTRAL_BAND_MIN = 2
    private const val CENTRAL_BAND_MAX = 4
    private val centralBand = CENTRAL_BAND_MIN..CENTRAL_BAND_MAX
    val center = Position(row = 3, col = 3)
    val inPlayPositions: List<Position> = buildList {
        for (row in 0 until GRID_DIMENSION) {
            for (col in 0 until GRID_DIMENSION) {
                if (row in centralBand || col in centralBand) add(Position(row = row, col = col))
            }
        }
    }
}
```

**Spec + initial pattern to copy** (`Boards.kt:50-63`):
```kotlin
object Boards {
    val English33: BoardSpec = BoardSpec(inPlay = English33Layout.inPlayPositions, target = English33Layout.center)
    val english33Initial: Board =
        English33.inPlay
            .fold(Board(spec = English33, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(English33Layout.center)   // ← for off-center boards, withoutPeg(startVacancy) instead of center
}
```

Four new layout objects + specs + initials (per RESEARCH Board Footprints / CONTEXT D-18), all square-grid, `RulesEngine` unchanged:
- **European37Layout** — `GRID_DIMENSION = 7`, `cornerTrim = [2,1,0,0,0,1,2]`, in-play iff `col in trim[row]..(6 - trim[row])`; **start vacancy `(3,2)`** (off-center, center is unsolvable). `target` may be null or `(3,2)` (D-02 ignores target for stars).
- **Asymmetric39Layout** — `GRID_DIMENSION = 8`, in-play iff `row in 3..5 || col in 2..4`; center start `(4,3)`.
- **Diamond41Layout** — `GRID_DIMENSION = 9`, `halfWidth = [0,1,2,3,4,3,2,1,0]`, in-play iff `col in (4-hw[row])..(4+hw[row])`; **off-center start** validated by the authoring solver (candidate `(4,2)`); center unsolvable.
- **Wiegleb45Layout** — `GRID_DIMENSION = 9`, in-play iff `row in 3..5 || col in 3..5`; center start `(4,4)`.

`Board.withPeg`/`withoutPeg`/`pegCount` (`Board.kt:25-32`) and `BoardSpec(inPlay, target)` (`BoardSpec.kt:15-18`) are unchanged — all four boards are ≤64 holes so the `Long` bitmask carries them as-is.

---

### `domain/src/test/.../golden/solutions/` — recorded-solution fixtures + replay tests (D-19)

**Analog:** `domain/src/test/.../golden/GoldenMasterTest.kt` (`:45-65` plays a board out and asserts terminal state) + `BoardCodec.kt` (the committed text encoding). Reuse the harness verbatim — the replay test only folds a committed `List<Move>` through `RulesEngine` and asserts `pegCount == 1`.

**Replay pattern** (RESEARCH Code Examples; mirrors `GoldenMasterTest.kt:45-54`):
```kotlin
@Test fun `european37 recorded solution reduces to one peg`() {
    val finalBoard = European37Solution.moves.fold(Boards.european37Initial) { b, m ->
        require(m in RulesEngine.legalMoves(b)); RulesEngine.applyMove(b, m)
    }
    assertThat(finalBoard.pegCount).isEqualTo(1)   // proves 3★ reachable (D-01)
}
```
One test per non-trivial board (European-37, Asymmetric-39, Diamond-41, Wiegleb-45; English-33 already covered). `Truth.assertThat` + `org.junit.Test` import style copied from `GoldenMasterTest.kt:5-6`.

---

### authoring DFS/IDDFS solver (one-off, NOT shipped in `:domain` main)

**Analog:** none — but it is a pure **consumer** of `RulesEngine.legalMoves`/`applyMove`/`pegCount` (`RulesEngine.kt:42-82`) and `Board.bits`/`pegCount`. Lives in a test or `tools/` source set, runs once to emit each `*Solution` fixture, then is never invoked by shipped code (D-19). Plain DFS with a visited-`Long` set per RESEARCH Code Examples. Keep it OUT of `domain/src/main` so the `DomainPurityTest`/no-Solver scope holds.

---

### `app/.../data/LevelRepository.kt` + `Level`/`ClassicLevelDescriptor` (repository seam)

**Analog (seam shape):** `RulesEngine` as a stateless authority injected via `DomainModule`; **analog (data classes):** `Move.kt`/`GameUiState.kt` immutable types. No existing repository — this is the first; follow RESEARCH Pattern 3 and the Android architecture guide. Bind with `@Binds` in the new `DataModule` (copy `DomainModule`'s `@Module @InstallIn(SingletonComponent::class)` shell).

```kotlin
data class Level(val id: String, val displayName: String, val spec: BoardSpec, val initialBoard: Board, val holeCount: Int)
data class ClassicLevelDescriptor(val id: String, val displayName: String, val holeCount: Int, val previewBoard: Board)

interface LevelRepository {
    fun classicCatalog(): List<ClassicLevelDescriptor>   // CLAS-01, D-18 order (5 boards)
    fun loadClassic(levelId: String): Level              // CLAS-02
    fun loadChallenge(levelNumber: Int): Level           // Phase 3 — STUB
}
```
**Level ids are named constants** (QUAL-03, Pitfall 5), not string literals at call sites — follow the `const val` discipline from `Boards`/`BoardMetrics`.

---

### `app/.../data/ClassicLevelSource.kt` / `ChallengeLevelSource.kt`

**Analog:** `domain/.../model/Boards.kt` is the source of the static catalog data; `ClassicLevelSource` maps `Boards.*` → `Level`/descriptors in D-18 order. `ClassicLevelSource.load(id)` validates the id against the known catalog (fail-fast, Pitfall 5). `StubChallengeLevelSource.load` is a one-liner `error("Challenge generation arrives in Phase 3")` — never invoked this phase (no Challenge route, D-09). Constructor-injected via `@Inject constructor()` like `GameViewModel` (`GameViewModel.kt:35-40`).

---

### `app/.../data/OnboardingRepository.kt` (DataStore — new dependency)

**Analog:** none (first DataStore usage). Follow RESEARCH Pattern 6 verbatim. `@Inject constructor(@ApplicationContext ctx)`, exposes `val onboardingSeen: Flow<Boolean>` + `suspend fun markSeen()`. Single `booleanPreferencesKey("onboarding_seen")`, store name `"app_prefs"` (D-16). This is the ONLY persistence pulled into Phase 2.

---

### `app/.../di/DataModule.kt` (NEW) + `DomainModule.kt` (MODIFIED)

**Analog:** `app/.../di/DomainModule.kt` (`:1-32`) — copy the module shell exactly.

**Pattern** (`DomainModule.kt:21-32`):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides @Singleton
    fun provideRulesEngine(): RulesEngine = RulesEngine
    @Provides
    fun provideInitialBoard(): Board = Boards.english33Initial   // ← DELETE this (D: board now from selection)
}
```
**Modifications:** delete `provideInitialBoard()` (the board now flows from the selected `levelId` via `SavedStateHandle` + `LevelRepository`); add `@Provides fun provideScoreCalculator(): ScoreCalculator = ScoreCalculator`. **New `DataModule`** uses `@Binds` (abstract class form) to bind `LevelRepository`→`DefaultLevelRepository`, `ClassicLevelSource`/`ChallengeLevelSource` impls, and `@Provides` the DataStore-backed `OnboardingRepository`.

---

### `app/.../navigation/AppNavHost.kt` + `Routes.kt` (NEW — navigation-compose 2.9.8)

**Analog:** none — new library; `MainActivity` is the insertion point. Follow RESEARCH Pattern 1 (type-safe `@Serializable` routes). Requires applying `org.jetbrains.kotlin.plugin.serialization` to `:app` (Pitfall 6).
```kotlin
@Serializable data object Home
@Serializable data object ClassicCatalog
@Serializable data class Gameplay(val levelId: String)
@Serializable data class Results(val levelId: String, val stars: Int, val pegsRemaining: Int, val elapsedSeconds: Int, val score: Int)
```
`NavHost(navController, startDestination = Home){ composable<Home>{…} … }`. Back stack + Results "Next board" wrap (D-06) per RESEARCH ResultsViewModel example. **Pin navigation-compose 2.9.8** (Pitfall 3 — 2.10.x needs compileSdk 37).

---

### `app/.../MainActivity.kt` (MODIFIED — host NavHost)

**Analog:** itself (`MainActivity.kt:18-28`). Keep the `@AndroidEntryPoint` + `setContent { AppTheme { … } }` shell; replace the single `GameRoute()` call with `AppNavHost(rememberNavController())`.

---

### Home / Classic catalog / Results screens — the Route → Screen → ViewModel → UiState quadruple

**Analog (exact, copy 4-file shape per screen):** `GameRoute.kt` + `GameScreen.kt` + `GameViewModel.kt` + `GameUiState.kt`. This is the locked **Route → Screen → Leaf + UDF** convention. Each new screen reproduces it.

**Route pattern** (`GameRoute.kt:20-33`) — stateful seam, no logic:
```kotlin
@Composable
fun GameRoute(modifier: Modifier = Modifier) {
    val viewModel: GameViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameScreen(state = state, onCellTap = viewModel::onCellTap, … , modifier = modifier)
}
```
New routes additionally take nav lambdas (`onPlayClassic`, `onSelect = { id -> … }`, `onReplay`, `onHome`, …) forwarded from `AppNavHost`.

**UiState pattern** (`GameUiState.kt:26-53`) — `@Immutable data class` + `companion object` factory, all `val`:
```kotlin
@Immutable
data class GameUiState(val board: Board, val selected: Position?, …) {
    companion object { fun initial(board: Board = Boards.english33Initial): GameUiState = GameUiState(…) }
}
```
→ `HomeUiState(onboardingVisible, …)`, `ClassicCatalogUiState(boards: List<ClassicLevelDescriptor>)`, `ResultsUiState(stars, pegsRemaining, elapsedSeconds, score, headlineTier, replayId, nextId)`.

**ViewModel pattern** (`GameViewModel.kt:34-46`) — `@HiltViewModel`, `@Inject constructor`, private `MutableStateFlow` + public `asStateFlow()` (never leak the mutable flow):
```kotlin
@HiltViewModel
class GameViewModel @Inject constructor(private val rulesEngine: RulesEngine, …) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState.initial(initialBoard))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
}
```
- `HomeViewModel` injects `OnboardingRepository`, collects `onboardingSeen` into `HomeUiState`.
- `ClassicCatalogViewModel` injects `LevelRepository`, exposes `classicCatalog()` ordered list.
- `ResultsViewModel` injects `LevelRepository` + `SavedStateHandle`, reads `toRoute<Results>()`, computes "Next board" wrap (RESEARCH ResultsViewModel example).

**Screen pattern** (`GameScreen.kt:72-128`) — stateless `@Composable`, M3 `Scaffold` + `TopAppBar`, all dp via `dimensionResource(R.dimen.*)`, all copy via `stringResource`, all color via `MaterialTheme.colorScheme.*`, all type via `MaterialTheme.typography.*`. Copy this composition shell for `HomeScreen`/`ClassicCatalogScreen`/`ResultsScreen`. Catalog uses `LazyVerticalGrid(GridCells.Adaptive(dimensionResource(R.dimen.catalog_grid_min_cell_width)))`.

---

### `app/.../ui/classic/BoardPreview.kt` (read-only board leaf)

**Analog:** `app/.../ui/components/BoardCanvas.kt` (`:60-70`). The catalog mini-preview (D-11) reuses the Canvas render but is **non-interactive**: no `onCellTap`, no selection ring, no destination dots, no animation. Planner decides a read-only flag/param vs. a thin wrapper; the visual + non-tappable contract is fixed (UI-SPEC §2). Reuses `BoardGeometry.forCanvas(spec, size)` and `MaterialTheme.colorScheme` roles (`secondary` pegs, `surfaceContainerLowest` holes).

---

### `app/.../ui/results/ResultsScreen.kt` — star-tier headline

**Analog (tiered copy):** `app/.../ui/game/GameOverDialog.kt` (`:42-56`) — the status/pegCount → headline tier `when`. Reuse this exact tier-selection idea for the D-08 headline ("Perfect!"/"Solved!"/"Out of moves"), but render as a full-screen `displaySmall` headline + star row + metric block (UI-SPEC §4), not an `AlertDialog`. Stars: `Icons.Rounded.Star` (filled `primary`, earned) / `Icons.Rounded.StarBorder` (`outlineVariant`, unearned).

**`GameOverDialog.kt` / `GameOverOverlay` are DELETED** (D-05 — Results destination replaces them); remove the `GameOverOverlay`/`GameOverDialog` calls from `GameScreen.kt:130-164`.

---

### `app/.../ui/onboarding/OnboardingOverlay.kt`

**Analog:** `GameOverDialog.kt` / `RestartConfirmDialog` (`GameScreen.kt:167-204`) — a stateless overlay composable driven by hoisted state + dismiss lambdas. Render as a `scrim` `Box` + centered `surfaceContainerHigh` `Surface` (UI-SPEC §5), illustrated steps, `Button("Got it")` + `TextButton("Skip tutorial")`, both call `markSeen()` via a hoisted lambda.

---

### `app/.../ui/game/GameViewModel.kt` (MODIFIED — levelId lookup + timer)

**Analog:** itself (`GameViewModel.kt:34-46`). Add `levelRepository: LevelRepository`, `scoreCalculator: ScoreCalculator`, `savedStateHandle: SavedStateHandle` to the existing `@Inject constructor`; replace the injected `initialBoard: Board` with `levelRepository.loadClassic(savedStateHandle.toRoute<Gameplay>().levelId).initialBoard`. All existing select/jump/restart logic (`:57-152`) and the `statusFor` peg-count check (`:159-164`) stay unchanged. Add the visible timer (RESEARCH Pattern 5): inject a `TimeSource`/`() -> Long`, start on first move, tick `elapsedSeconds` into state via `viewModelScope.launch` + `delay(1_000)`, stop on terminal status.

---

### `app/.../ui/game/GameUiState.kt` (MODIFIED) + `GameRoute.kt` (MODIFIED)

**Analog:** themselves. `GameUiState`: add `val elapsedSeconds: Int` (default 0 in `initial(...)`). `GameRoute`: add a **one-shot** terminal→Results navigation effect (Pitfall 4) — a `LaunchedEffect` keyed on the *transition* to `Won`/`Stuck` (or a Channel/SharedFlow), calling `onFinished(Results(...))` exactly once, never per recomposition. `ScoreCalculator` computes `stars`/`score` at game end (never in the composable).

---

### `app/.../ui/game/BoardMetrics.kt` + `BoardGeometry.kt` (MODIFIED — per-board grid dimension)

**Analog:** themselves. **Remove the hardcoded `GRID_DIMENSION = 7`** (`BoardMetrics.kt:14`, `BoardGeometry.kt:82,93-94`); derive the dimension from the `BoardSpec` bounds: `gridDimension = max(maxRow+1, maxCol+1)` (English/European = 7, Asymmetric = 8, Diamond/Wiegleb = 9). Thread it into `BoardGeometry.forCanvas(spec, size)` (currently `cellSize = min(w,h) / BoardMetrics.GRID_DIMENSION`, `GRID_RANGE = 0 until BoardMetrics.GRID_DIMENSION`). `BOARD_ASPECT_RATIO = 1f` and every ratio constant stay unchanged; `positionAt` stays bounds-checked for every footprint (T-04-01). Add per-board `BoardGeometryTest` cases (dims 7/8/9).

---

### `dimens.xml` / `strings.xml` (MODIFIED)

**Analog:** themselves. Add the new tokens (UI-SPEC Spacing): `content_max_width` 480dp, `mode_tile_min_height` 96dp, `catalog_grid_min_cell_width` 160dp, `catalog_preview_size` 96dp, `results_star_size` 48dp, `onboarding_illustration_size` 160dp — all multiples of 4, KDoc'd like the existing tokens. Add all Phase-2 copy from the UI-SPEC Copywriting Contract to `strings.xml` (board names, "Coming soon", "Next board"/"Replay"/"Catalog"/"Home", headlines, "{n} holes" plural, onboarding captions, content descriptions). **No raw dp/sp in composables, no magic strings** (`dpSpLiteralCheck` + no-magic-strings gates).

---

### `gradle/libs.versions.toml` + `app/build.gradle.kts` (MODIFIED — 3 new deps)

**Analog:** itself. Add to the catalog (RESEARCH Standard Stack): `navigation-compose 2.9.8`, `kotlinx-serialization-json 1.11.0` + plugin `org.jetbrains.kotlin.plugin.serialization` (version.ref `kotlin`), `datastore-preferences 1.2.1`. Mirror the existing `[versions]`/`[libraries]`/`[plugins]` block style and the `resolutionStrategy.eachDependency` compileSdk-36 guard already in `app/build.gradle.kts` (extend it if navigation pulls a lifecycle/core 37 line — Pitfall 3). Apply the serialization plugin in `:app` (Pitfall 6).

---

### ViewModel tests — `app/src/test/.../ui/{home,classic,results}/*ViewModelTest.kt`

**Analog:** `app/.../ui/game/GameViewModelTest.kt` (`:1-30`) — Turbine `uiState.test { }` + `runTest` + `Truth.assertThat`, tiny custom `BoardSpec`s for deterministic cases. Copy the import block and `runTest { viewModel.uiState.test { awaitItem() … } }` structure. New `ScoreCalculatorTest` is a plain JVM test (no Turbine) like `BoardTest`/`GoldenMasterTest`.

---

## Shared Patterns

### Hilt DI seam (domain stays Android-free)
**Source:** `app/.../di/DomainModule.kt:21-32` + `App.kt` (`@HiltAndroidApp`) + `MainActivity.kt` (`@AndroidEntryPoint`).
**Apply to:** `ScoreCalculator`, `LevelRepository`/sources, `OnboardingRepository`, all new ViewModels.
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides @Singleton fun provideRulesEngine(): RulesEngine = RulesEngine
}
```
ViewModels use `@HiltViewModel class X @Inject constructor(...) : ViewModel()` (`GameViewModel.kt:34-40`); obtained in the Route via `hiltViewModel()` (`GameRoute.kt:22`). Domain types carry **no** Hilt/Android annotations (QUAL-01) — all bindings live in `:app`.

### Immutable UiState over StateFlow (UDF / state hoisting)
**Source:** `GameUiState.kt:26-53` + `GameViewModel.kt:43-46`.
**Apply to:** every new screen.
```kotlin
private val _uiState = MutableStateFlow(<UiState>.initial(...))
val uiState: StateFlow<<UiState>> = _uiState.asStateFlow()   // mutable flow never leaks
```
`@Immutable data class …UiState(val …)` with a `companion object` factory. Screens are stateless; Routes collect via `collectAsStateWithLifecycle()`.

### Scoped constants — no magic numbers (QUAL-03)
**Source:** `BoardMetrics.kt:12-50`, `Boards.kt`'s `English33Layout:14-41`.
**Apply to:** `ScoreParams`, every new board layout object, level-id constants, any new geometry/animation literal. One `object` per scope, every value a KDoc'd `const val`.

### Resource-only UI tokens (no raw dp/sp/hex/string in composables)
**Source:** `GameScreen.kt:89-124` (`dimensionResource`, `stringResource`, `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`), `dimens.xml`, `strings.xml`, `Type.kt`.
**Apply to:** Home, Catalog, Results, Onboarding screens. Board geometry stays as unitless ratios in `BoardMetrics` (NOT dimens). Two weights / four sp sizes only (UI-SPEC Typography). Enforced by `dpSpLiteralCheck` + detekt/ktlint gates.

### Golden-master harness (deterministic fixtures)
**Source:** `domain/src/test/.../golden/{BoardCodec,GoldenMasterAssert,GoldenMasterTest}.kt`.
**Apply to:** the D-19 recorded-solution replay tests for the four new boards (reuse `BoardCodec`/the fold-through-`RulesEngine` pattern verbatim; only inputs change).

---

## No Analog Found

| File | Role | Data Flow | Reason / Guidance |
|------|------|-----------|-------------------|
| `app/.../navigation/AppNavHost.kt` + `Routes.kt` | route | request-response | First navigation-compose usage; follow RESEARCH Pattern 1 (type-safe `@Serializable` routes). `MainActivity` is the only structural anchor. |
| `app/.../data/OnboardingRepository.kt` | repository | event-driven | First DataStore usage (new 1.2.1 dependency); follow RESEARCH Pattern 6. |
| `app/.../data/ChallengeLevelSource.kt` (stub) | service | — | Trivial `error(...)` stub; nothing to copy. Interface stability is the point (Phase 3 swaps it). |
| authoring DFS/IDDFS solver | utility (one-off) | batch | No analog; pure consumer of `RulesEngine`. Lives in test/`tools/`, never shipped (D-19). |

---

## Metadata

**Analog search scope:** `app/src/main/java/com/app/pegsolitaire/**`, `domain/src/main/kotlin/**`, `domain/src/test/kotlin/**`, `app/src/test/**`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/res/values/**`.
**Files scanned:** 24 source/test/config files read in full.
**Pattern extraction date:** 2026-06-28
</content>
</invoke>
