# Phase 3: Challenge Generation & Difficulty Curve - Pattern Map

**Mapped:** 2026-07-01
**Files analyzed:** 25 new/modified files
**Analogs found:** 24 / 25 (1 partial — freeform footprint generation has no exact analog)

> Every new file has a strong in-repo analog. This phase's headline warning is that
> **nearly all "hard" primitives already exist and were built with Phase 3 in mind**
> (sorted move enumeration, immutable bitmask `Board`, byte-stable snapshotting,
> golden-master asserting, symmetry canonicalization). The planner should have new
> files copy these patterns rather than invent parallel machinery.

---

## File Classification

### Domain (`:domain` main — pure Kotlin, zero Android)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `domain/…/generator/LevelGenerator.kt` | service (pure object) | transform (seed→board) | `domain/…/rules/RulesEngine.kt` + test `AuthoringSolver.kt` | exact (role) + strong (algorithm) |
| `domain/…/generator/GeneratedLevel.kt` | model | request-response value | `domain/…/model/Move.kt`, `domain/…/scoring/ScoreResult.kt` | exact |
| `domain/…/generator/DifficultyTier.kt` | model/config | data-table | `domain/…/scoring/ScoreParams.kt` (structure), `domain/…/model/Boards.kt` (layout objects) | exact |
| `domain/…/generator/DifficultyTiers.kt` (`forLevel`) | service/config | transform (level→tier) | `domain/…/model/Boards.kt`, `DefaultClassicLevelSource` catalog map | role-match |
| `domain/…/generator/GeneratorConstants.kt` (seed/version consts) | config | — | `domain/…/scoring/ScoreParams.kt` | exact |
| freeform footprint builder (in generator) | utility | transform (positions→BoardSpec) | `domain/…/model/Boards.kt` `*Layout.inPlayPositions` + `BoardSpec.kt` | partial (graph/connectivity check is new) |
| `ScoreCalculator.kt` (modify: level-scaled score) | service | transform | `domain/…/scoring/ScoreCalculator.kt` (self) + `ScoreParams.kt` | exact (extend in place) |

### Domain test (`:domain` test — JVM golden-master + invariants)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `ChallengeGeneratorGoldenTest.kt` + `challenge_level_{1,2,10,100,1000}.txt` | test (golden) | — | `domain/…/golden/GoldenMasterTest.kt` + `BoardCodec` + `GoldenMasterAssert` | exact (reuse harness verbatim) |
| `ChallengeSolvabilityTest.kt` | test (invariant) | — | `RulesEngineTest.kt`, `SolutionTest` replay-reduces-to-one-peg | exact |
| `DifficultyTierTest.kt` | test | — | `ScoreCalculatorTest.kt`, `RulesEngineTest.kt` | exact |
| `QualityGateTest.kt` | test | — | `RulesEngineTest.kt` (deterministic assertions) | role-match |
| freeform validity tests (connected, ≤64) | test | — | `BoardGeometryTest.kt`, `RulesEngineTest.kt` | role-match |

### Data (`:app` data + DI)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `GeneratedChallengeLevelSource.kt` (replaces `StubChallengeLevelSource`) | service (repo source) | CRUD-read (level#→Level) | `DefaultClassicLevelSource.kt`, `StubChallengeLevelSource.kt` | exact |
| `LevelRepository.kt` / `Level` (carry `solution`+`generatorVersion`) | model + repository | request-response | `data/LevelRepository.kt` (self) | exact (extend in place) |
| `ChallengeProgress.kt` (in-memory holder) | store/holder | event-driven (advance-on-solve) | `data/OnboardingRepository.kt`, `SettingsRepository` (seam shape) | role-match |
| `di/DataModule.kt` (rebind Challenge source) | config (DI) | — | `di/DataModule.kt` (self) | exact (one-line `@Binds` swap) |
| `di/DomainModule.kt` (provide `LevelGenerator` if injected) | config (DI) | — | `di/DomainModule.kt` (self) | exact |

### UI / navigation (`:app`)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `navigation/Routes.kt` (add Challenge route + Results mode/`showNextLevel`) | route | — | `navigation/Routes.kt` (self — `Gameplay`, `Results`) | exact |
| `navigation/AppNavHost.kt` (wire Challenge + Challenge-aware Results) | route wiring | — | `navigation/AppNavHost.kt` (self) | exact |
| `ui/game/GameViewModel.kt` (load Challenge board; `Level N`) | component (VM) | request-response | `ui/game/GameViewModel.kt` (self) | exact (generalize load path) |
| `ui/game/GameRoute.kt` / `GameScreen.kt` (`Level N` top-bar title) | component | — | `GameRoute.kt`, `GameScreen.kt` `GameTopBar` | exact |
| `ui/results/ResultsViewModel.kt` / `ResultsUiState.kt` (mode/`showNextLevel`) | component (VM) | request-response | self (Phase-2 Results trio) | exact |
| `ui/results/ResultsScreen.kt` / `ResultsRoute.kt` (Next level vs Retry) | component | — | self | exact |
| `ui/home/HomeScreen.kt` + `HomeRoute.kt`/`HomeViewModel.kt` (enable `ChallengeTile`) | component | — | `HomeScreen.kt` `ClassicTile`/`ChallengeTile` | exact |
| `res/values/strings.xml` + `dimens.xml` (new Challenge copy) | config (resources) | — | existing entries (see UI-SPEC copywriting table) | exact |

### Wiring test (`:app` test)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| Challenge wiring test (advance-on-solve; stuck→Retry) | test | — | `ui/game/GameViewModelTest.kt`, `navigation/NavFlowTest.kt` | exact |

---

## Pattern Assignments

### `domain/…/generator/LevelGenerator.kt` (pure service, transform)

**Analogs:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/rules/RulesEngine.kt` (shape + sorted enumeration); `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt` (seeded `Random`, symmetry canonicalization).

**Top-level pure `object` with scoped-const shape** (`RulesEngine.kt:21-35`) — copy this exact structure (object, KDoc'd `private val` direction list, named `const` span, deterministic-sorted enumeration). This IS the determinism-discipline precedent the generator depends on:
```kotlin
object RulesEngine {
    private val DIRECTIONS: List<Direction> =
        listOf(
            Direction(rowDelta = 0, colDelta = -1),
            Direction(rowDelta = 0, colDelta = 1),
            Direction(rowDelta = -1, colDelta = 0),
            Direction(rowDelta = 1, colDelta = 0),
        )
    private const val JUMP_SPAN = 2
```

**Sorted candidate enumeration — the determinism linchpin** (`RulesEngine.kt:42-63`). The reverse-move enumerator MUST mirror this comparator exactly so the PRNG index maps to the same move on every device (RESEARCH §Determinism, Pitfall 1). Never draw a PRNG index against a `Set`/`buildSet`/`Map`:
```kotlin
fun legalMoves(board: Board): List<Move> =
    buildList {
        for (from in board.spec.inPlay) {
            if (board.cellAt(from) != Cell.Peg) continue
            for (direction in DIRECTIONS) { /* … build Move … */ }
        }
    }.sortedWith(
        compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col }),
    )
```

**Inverse move = reuse the forward `Move` + `Board` transforms** (`RulesEngine.kt:69-77`, `Board.kt:26-29`). `applyReverse` is the mirror of `applyMove`; do NOT introduce a `ReverseMove` type. The recorded reverse list, reversed, replays directly through the unchanged `applyMove`:
```kotlin
// RulesEngine.applyMove — the forward transform the generator inverts:
board.withoutPeg(move.from).withoutPeg(move.over).withPeg(move.to)
// Board.withPeg/withoutPeg are copy-based, never mutate (Board.kt:26-29):
fun withPeg(position: Position): Board = copy(pegs = pegs or maskFor(position))
fun withoutPeg(position: Position): Board = copy(pegs = pegs and maskFor(position).inv())
```

**Seeded PRNG + symmetry-canonical dedup key** (`AuthoringSolver.kt:83-85`, `:157-205`). Copy `Random(BASE_SEED + attempt)` seeding-with-salt and **lift `canonical`/`symmetryPermutations` verbatim** for the dedup quality gate (RESEARCH §Quality Gates — "lift that canonicalization rather than re-deriving it"):
```kotlin
val random = Random(BASE_SEED + attempt)          // per-attempt deterministic salt
// canonical(pegs, symmetries): min mask under the footprint's dihedral group — the dedup key
```

**Guardrails to honor (from CLAUDE.md / RESEARCH):** stays in `:domain`, zero `android.*` (enforced by `DomainPurityTest`); all seed/version/threshold literals are scoped `const val` in `GeneratorConstants` (no inline magic numbers — see `ScoreParams` precedent); single-threaded synchronous `generate` (no coroutines inside the loop).

---

### `domain/…/generator/GeneratedLevel.kt` (model, value)

**Analogs:** `domain/…/model/Move.kt`, `domain/…/scoring/ScoreResult.kt`.

**Minimal immutable `data class` of plain values with KDoc'd properties** (`ScoreResult.kt:17-23`). Add `levelNumber`, `startingBoard`, `solution: List<Move>`, `generatorVersion` — all `val`, all pure-domain types:
```kotlin
data class ScoreResult(
    val stars: Int,
    val score: Int,
    val pegsRemaining: Int,
    val pegsCleared: Int,
    val elapsedSeconds: Int,
)
```
The `solution` reaches Phase 4 (hints); `generatorVersion` reaches Phase 5 (resume) — carry both through now even if unused (RESEARCH Open Q1).

---

### `domain/…/generator/DifficultyTier.kt` + `DifficultyTiers.kt` (config data-table + resolver)

**Analogs:** `domain/…/scoring/ScoreParams.kt` (scoped-const discipline), `domain/…/model/Boards.kt` (private `*Layout` objects → derived specs).

**Every knob is a KDoc'd `const val` tied to its decision, NOT an inline literal** — copy `ScoreParams.kt:11-52` exactly for the tier structure. Knob VALUES are deferred to the playtest loop (D-07); only the *structure* ships now:
```kotlin
object ScoreParams {
    /** Max pegs remaining for a 3-star result (D-01). … */
    const val THREE_STAR_MAX_PEGS = 1
    const val PEG_VALUE = 100
    const val TIME_BONUS_CAP = 5000
    …
}
```

**`forLevel(levelNumber)` as a continuous mapping** — model the resolver on `Boards.kt`'s derived-from-layout-rules generation (`Boards.kt:14-41`), not a stepped `when`. RESEARCH A4 flags "continuous vs stepped" as a MEDIUM design choice to confirm with the user during planning.

---

### `ScoreCalculator.kt` — MODIFY for level-scaled score (D-11)

**Analog:** self (`domain/…/scoring/ScoreCalculator.kt:35-58`).

**Additive, pure change only:** add a difficulty/level multiplier as a **pure parameter passed in** — do NOT couple `ScoreCalculator` to level state (RESEARCH §Wiring, D-11). Keep the star bands (`stars()`) untouched; scale only `score()`:
```kotlin
fun score(pegsCleared: Int, elapsedSeconds: Int): Int =
    pegsCleared * ScoreParams.PEG_VALUE +
        maxOf(0, ScoreParams.TIME_BONUS_CAP - elapsedSeconds * ScoreParams.DECAY_PER_SECOND)
```
New multiplier constant goes in `ScoreParams` (no inline literal). `stars()` stays as-is — a 1-peg solve remains 3★.

---

### `ChallengeGeneratorGoldenTest.kt` + fixtures (test — REUSE HARNESS VERBATIM)

**Analogs:** `domain/…/golden/GoldenMasterTest.kt`, `BoardCodec.kt`, `GoldenMasterAssert.kt`. This is the **#1 deliverable — committed and CI-green BEFORE any tier knob is tuned** (RESEARCH §"Lock it FIRST").

**Copy the assert call shape** (`GoldenMasterTest.kt:16-32`) — only the inputs change; the codec + assert + `UPDATE_GOLDEN` workflow are reused unchanged:
```kotlin
GoldenMasterAssert.assertMatches(
    fixtureName = "english33_initial.txt",
    actual = BoardCodec.encodeBoard(Boards.english33Initial),
)
```
Encode generated levels via `BoardCodec.encodeBoard(level.startingBoard)` + `BoardCodec.encodeMoves(level.solution)` for levels 1/2/10/100/1000 (RESEARCH §Code Examples). Fixtures land under `domain/src/test/resources/golden/` alongside the existing `english33_*.txt`. Regenerate deliberately with `UPDATE_GOLDEN=true` (`GoldenMasterAssert.kt:29-41`).

---

### `ChallengeSolvabilityTest.kt` (test — invariant)

**Analogs:** `RulesEngineTest.kt:44-55` (fold + assert legal each step), `SolutionTest` replay precedent.

**Fold the recorded solution through `RulesEngine`, asserting each step legal and the final board is one peg** (RESEARCH §Code Examples):
```kotlin
val finalBoard = level.solution.fold(level.startingBoard) { b, m ->
    assertThat(m in RulesEngine.legalMoves(b)).isTrue()
    RulesEngine.applyMove(b, m)
}
assertThat(finalBoard.pegCount).isEqualTo(1)
```
Test-code magic numbers (board coordinates) are acceptable — see `RulesEngineTest.kt:13-16` KDoc.

---

### `GeneratedChallengeLevelSource.kt` (data service — replaces stub)

**Analogs:** `data/DefaultClassicLevelSource.kt`, `data/ChallengeLevelSource.kt` (`StubChallengeLevelSource`).

**Constructor-injected impl behind the existing interface** (`DefaultClassicLevelSource.kt:37-41`, `StubChallengeLevelSource.kt:20-24`) — the interface `ChallengeLevelSource.load(levelNumber): Level` already exists; drop the real generator-wrapper in behind it. Map `levelNumber → stableSeed → generate → Level` (RESEARCH §Wiring). Clamp `levelNumber < 1` at this boundary (RESEARCH §V5):
```kotlin
class StubChallengeLevelSource
    @Inject
    constructor() : ChallengeLevelSource {
        override fun load(levelNumber: Int): Level = error("Challenge generation arrives in Phase 3")
    }
```

---

### `di/DataModule.kt` — MODIFY (one-line rebind)

**Analog:** self (`di/DataModule.kt:43-45`).

**Swap the `@Binds` target** from `StubChallengeLevelSource` to the real impl — everything else in the module stays:
```kotlin
@Binds
@Singleton
abstract fun bindChallengeLevelSource(impl: StubChallengeLevelSource): ChallengeLevelSource
// → bind the new GeneratedChallengeLevelSource
```

---

### `navigation/Routes.kt` — MODIFY (add Challenge route + Results awareness)

**Analog:** self (`navigation/Routes.kt:36-61`).

**`@Serializable data class` route carrying only minimal identifying state** (`Routes.kt:36-39`) — add a Challenge gameplay route carrying `levelNumber: Int` (never a `Board`; the repo is the single source of truth per the file header). Extend `Results` with a mode/`showNextLevel` + `nextLevelNumber` (or a parallel Challenge-results route — planner's call per D-01/D-10):
```kotlin
@Serializable
data class Gameplay(
    val levelId: String,
)
```

---

### `navigation/AppNavHost.kt` — MODIFY

**Analog:** self (`AppNavHost.kt:78-93` gameplay/results wiring, `:102-104` pop-up helper).

**`composable<Route>` with hoisted default-valued content slots + `toRoute` back-stack pop discipline** (`AppNavHost.kt:85-93`). The "Next level"→N+1 advance should reuse the `popUpTo` pattern (`AppNavHost.kt:102-104`) so Challenge board-hops don't grow the back stack. Keep `if (challenge) … else …` OUT of the graph — routing stays in the repository (RESEARCH Anti-Pattern 2).

---

### `ui/game/GameViewModel.kt` — MODIFY (generalize load path)

**Analog:** self (`GameViewModel.kt:63-84`, `:167-211`).

**Route-arg → repository load, read via `SavedStateHandle.toRoute`** (`GameViewModel.kt:74-77`). Generalize this Classic-specific load to also handle `loadChallenge(levelNumber)` (unified `LevelRef` vs. a Challenge branch — RESEARCH Open Q2). Preserve the one-shot `Channel<ScoreResult>` finish pattern verbatim (`GameViewModel.kt:86-94`, `:198-210`) — carry Challenge context so Results shows "Next level" on Won, Retry-only on Stuck:
```kotlin
val levelId: String = savedStateHandle.toRoute<Gameplay>().levelId
private val initialBoard: Board = levelRepository.loadClassic(levelId).initialBoard
…
private val _finished = Channel<ScoreResult>(Channel.CONFLATED)
val finished: Flow<ScoreResult> = _finished.receiveAsFlow()
```

---

### `ui/results/ResultsViewModel.kt` / `ResultsUiState.kt` / `ResultsScreen.kt` — MODIFY

**Analogs:** self (`ResultsViewModel.kt:27-63`, `ResultsUiState.kt:26-33`, `ResultsScreen.kt:75-98`).

**Display-only VM reading typed args, `@Immutable` state, single accent `Button`** (`ResultsUiState.kt:25-33`, `ResultsScreen.kt:60-63` KDoc). Add a mode/`showNextLevel` field; render exactly ONE filled `primary` Button per state (Next level when solved / Retry when stuck) + neutral Home — per UI-SPEC S3 accent discipline. Nothing trust-sensitive is re-derived (`ResultsViewModel.kt:20-24`).

---

### `ui/home/HomeScreen.kt` — MODIFY (enable ChallengeTile)

**Analog:** self — `ClassicTile` (`HomeScreen.kt:117-147`) is the enabled-tile template; `ChallengeTile` (`:155-181`) is what changes.

**Make `ChallengeTile` a peer of `ClassicTile`:** copy the `ClassicTile` `Card(onClick=…)` shape, drop `enabled = false` and the `ComingSoonBadge`, add the "Endless levels" supporting line. Stays neutral `surfaceContainerHigh` (NOT accent — UI-SPEC S1/Color):
```kotlin
Card(
    onClick = onPlayClassic,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.mode_tile_min_height)),
) { … titleMedium + bodyMedium … }
```
Add an `onPlayChallenge` lambda threaded `HomeRoute` → `HomeScreen` exactly like `onPlayClassic` (`HomeRoute.kt:22-35`).

---

### Challenge wiring test (`:app` test)

**Analogs:** `ui/game/GameViewModelTest.kt:55-70` (Robolectric + Turbine + `TestTimeSource` + `StandardTestDispatcher`), `navigation/NavFlowTest.kt:37-55` (stub-destination nav reachability).

**Copy the Robolectric/Turbine harness** (`GameViewModelTest.kt:55-65`) for advance-on-solve, and the `AppNavHost` stub-destination pattern (`NavFlowTest.kt:47-55`) to prove stuck→Retry (no Next level). Assert deterministically on semantics/state, not pixels (`GameViewModelTest.kt` precedent).

---

## Shared Patterns

### No magic numbers / strings — scoped `const val` split by scope (QUAL-03)
**Source:** `domain/…/scoring/ScoreParams.kt` (whole file); `domain/…/model/Boards.kt:14-41` (`*Layout` objects).
**Apply to:** `LevelGenerator`, `DifficultyTier`, `GeneratorConstants` (seed mix constants, `GENERATOR_VERSION`, `MAX_ATTEMPTS`, all gate thresholds), the `ScoreCalculator` multiplier.
Every literal is a KDoc'd `const val` in a scope object tied to its decision — never inlined in logic. detekt `MagicNumber` is a build gate.

### Immutable, copy-based `Board` — never mutate (Pitfall 6)
**Source:** `domain/…/model/Board.kt:26-29`.
**Apply to:** the entire reverse-walk in `LevelGenerator`.
```kotlin
fun withPeg(position: Position): Board = copy(pegs = pegs or maskFor(position))
fun withoutPeg(position: Position): Board = copy(pegs = pegs and maskFor(position).inv())
```
No mutable scratch board may leak into UI state.

### Domain purity — zero Android in `:domain` (QUAL-01)
**Source:** `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt` (ArchUnit `noClasses().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("android..","androidx..")`).
**Applies to (automatically):** all new `:domain` generator files. Seeds/level numbers are plain `Int`/`Long`; no `Log`/`Context`/`Dispatcher`.

### Golden-master snapshot contract — reuse verbatim
**Source:** `domain/…/golden/BoardCodec.kt` (`encodeBoard`/`encodeMoves`) + `GoldenMasterAssert.kt` (`assertMatches` + `UPDATE_GOLDEN`).
**Apply to:** `ChallengeGeneratorGoldenTest`. Do NOT write a new serializer or assert helper — only the inputs change.

### MVVM: Route → Screen → Leaf, `@Immutable` `StateFlow`, one-shot nav via `Channel`
**Source:** `GameRoute.kt:29-63` (hoisted VM refs + `LaunchedEffect(Unit)` one-shot), `GameViewModel.kt:81-94` (`asStateFlow()` never leaks the mutable flow; `Channel.receiveAsFlow()`), `HomeRoute.kt`/`ResultsRoute.kt` (`hiltViewModel()` + `collectAsStateWithLifecycle()`).
**Apply to:** every new/modified Challenge UI surface. Stateless composables; state owned by the VM; nav effects are one-shot, never re-fired on recomposition (Pitfall 4).

### Hilt `@Binds` interface→impl, `@InstallIn(SingletonComponent)`
**Source:** `di/DataModule.kt:34-49`, `di/DomainModule.kt:24-33`.
**Apply to:** the Challenge source rebind (`DataModule`) and, if injected, `LevelGenerator` provision (`DomainModule` — the domain object is Hilt-annotation-free, so the seam is provided in `:app`).

### UI resources — dimens + strings, no raw literals
**Source:** `HomeScreen.kt` uses `dimensionResource(R.dimen.*)` / `stringResource(R.string.*)` throughout; files at `app/src/main/res/values/{dimens.xml,strings.xml}`.
**Apply to:** the `Level N` top-bar copy, the "Next level"/"Retry"/"Endless levels" strings (all keys enumerated in UI-SPEC §Copywriting), and any spacing. UI-SPEC confirms **no new dimens token is required** — reuse `mode_tile_min_height`, `content_max_width`, `results_star_size`, etc.

---

## Freeform Footprint — the one partial-analog area

**File:** freeform footprint builder inside `LevelGenerator` (D-08/D-09).
**Closest analogs:** `Boards.kt` `*Layout.inPlayPositions` (generating a `List<Position>` from layout rules) + `BoardSpec.kt` (stable-bit-index construction from an in-play list) + `BoardGeometry.kt:23-36` (renders ANY `BoardSpec` up to its `gridDimension = max(maxRow,maxCol)+1` — verified general; no rendering change needed for new shapes).

**Genuinely new (no analog):** the connectivity check (orthogonal BFS/DFS flood-fill over 4-neighbor adjacency) and the ≤64-cell cap enforcement. These are small pure-graph checks operating on a candidate `List<Position>`; there is no existing flood-fill in the repo to copy. Build against the `BoardSpec`/`Position` types and keep freeform bounding boxes proportionate (RESEARCH §Freeform, grid-dimension caution). Prefer symmetric shapes by reusing `AuthoringSolver.symmetryPermutations` to score symmetry cheaply.

| File | Role | Data Flow | Reason no exact analog |
|------|------|-----------|------------------------|
| freeform connectivity/validity check | utility | transform | No graph flood-fill exists in-repo; the rest (spec construction, rendering) reuses `Boards.kt`/`BoardSpec`/`BoardGeometry` unchanged |

---

## Metadata

**Analog search scope:** `domain/src/main` (model, rules, scoring), `domain/src/test` (golden harness, AuthoringSolver, tests), `app/src/main` (data, di, navigation, ui/{game,results,home,components}), `app/src/test` (VM/nav/domain-purity tests), `app/src/main/res/values`.
**Files scanned:** ~40 Kotlin files + resource dirs (all `.kt` under `domain/` and `app/`, non-build).
**Pattern extraction date:** 2026-07-01
