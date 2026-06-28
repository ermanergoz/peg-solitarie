# Phase 2: Classic Mode, Scoring & Navigation - Research

**Researched:** 2026-06-28
**Domain:** Jetpack Compose Navigation (type-safe), Repository-pattern data seam, pure-Kotlin scoring, DataStore, multi-footprint Canvas geometry
**Confidence:** HIGH (nav/DataStore/scoring/repo), MEDIUM (board footprints), and one **BLOCKING** conflict surfaced (Triangle-15)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Scoring — Stars**
- **D-01:** Stars from **absolute pegs remaining, identical on every board** — 1 peg = 3★, 2–3 pegs = 2★, finished-but->3 = 1★. The 2★ band cutoff (2–3) is a named constant the planner may sanity-check; the *approach* (absolute pegs) is locked.
- **D-02:** The target hole is **ignored for stars**. Stars depend solely on peg count.

**Scoring — Score & Timer**
- **D-03:** `score = pegsCleared × PEG_VALUE + max(0, TIME_BONUS_CAP − elapsedSeconds × DECAY)`. Exact constants set in planning. Stars and score are **separate axes**.
- **D-04:** The timer is **visible during play** (game top bar). Elapsed time is the input to D-03's score.

**Results Screen**
- **D-05:** Results is a **full dedicated navigation destination** (5th screen), not a modal. Replaces the Phase-1 `GameOverDialog` seam.
- **D-06:** Results actions: **Replay (same board) / Next board / Catalog / Home**. "Next board" follows catalog ascending-size order and **wraps**.
- **D-07:** A "stuck" loss (no legal moves, >1 peg) uses the **same results screen** — low star count (typically 1★) + score. No separate loss screen.
- **D-08:** Results headline is **tiered to the star outcome** (3★ "Perfect!", 2★ "Solved!", 1★ "Cleared"/"Stuck"). Exact copy is the UI's.

**Home & Classic Catalog**
- **D-09:** Challenge mode is **shown but disabled with a "Coming soon" badge** on Home. No placeholder route built.
- **D-10:** Home is a **mode-select screen** (Classic / Challenge); the Classic catalog is a **separate screen**.
- **D-11:** Catalog presents each board as a **card with a mini board-shape preview** (reusing `BoardCanvas`/`BoardGeometry`) plus name and hole count.
- **D-12:** Boards ordered **ascending size: Triangle-15 → English-33 → European-37 → Diamond-41**. Defines the "Next board" wrap sequence (D-06).

**Onboarding**
- **D-13:** First-time how-to-play is a **dismissable, illustrated overlay** (steps showing the orthogonal jump). Skippable.
- **D-14:** Triggers on **first app launch, over Home** (once).
- **D-15:** A **permanent "How to play" / help affordance** exists (Home or game top bar) for re-reading.
- **D-16:** "Onboarding seen" tracked with a **single DataStore-Preferences boolean (`onboardingSeen`)**, introduced now. Add `androidx.datastore:datastore-preferences` (1.2.1) for this flag only. All other persistence stays in Phase 5.

### Claude's Discretion
- **Navigation library wiring** — introduce Navigation Compose 2.9.8 (in stack, not yet wired); type-safe nav graph for the destinations.
- **How the selected board reaches `GameViewModel`** — VM currently takes a hardcoded injected `Board`; planning decides the mechanism (nav arg + `SavedStateHandle`, repo lookup by level id, etc.).
- **`LevelRepository` interface shape** — real Classic source + stubbed Challenge source; designed so Phase 3's generator drops in behind the same seam.
- **The three new board footprints + initial states** — European-37, Triangle-15, Diamond-41; all fit the ≤64-hole `Long`-bitmask `BoardSpec`.
- **Triangle-15 rendering** — triangular lattice rather than the square grid `BoardCanvas`/`BoardGeometry` assume; must still emit bounds-checked `onCellTap`.
- **Where `ScoreCalculator` lives and its exact constants** (`PEG_VALUE`, `TIME_BONUS_CAP`, `DECAY`, the 2★ band) — pure-Kotlin domain, JVM-unit-tested.

### Deferred Ideas (OUT OF SCOPE)
- Saved "best stars / best score" badges on catalog cards (Phase 5 — PERS-01).
- Challenge mode itself — only the disabled entry + stubbed repo source here (Phase 3).
- Full settings (sound/haptics) and the broader DataStore/Room store (Phase 5).
- Interactive onboarding demo (deferred in favor of the static overlay, D-13).
- Drag-to-jump input and the arc/pop animation polish pass (Slice 2).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CLAS-01 | Browse and select from a catalog of classic boards | Catalog screen over `LevelRepository.classicCatalog()`; D-11 mini-previews via generalized `BoardGeometry`; D-12 ascending order |
| CLAS-02 | Play any classic board to completion with correct rules | 3 new `BoardSpec`/`Board` footprints; `RulesEngine` unchanged for square boards; **Triangle-15 rules conflict — see Open Question 1** |
| SCOR-01 | 1–3 stars based on pegs remaining | `ScoreCalculator.stars()` pure domain, D-01 bands; JVM-tested |
| SCOR-02 | Elapsed time → separate numeric score | `ScoreCalculator.score()` D-03 formula; in-game timer (D-04) supplies `elapsedSeconds` |
| SCOR-03 | Results screen shows stars, pegs, time, score | Results destination (D-05) reading a `@Serializable` results route arg |
| NAV-01 | Home with entry to Classic and Challenge | Home mode-select (D-10); Challenge disabled "Coming soon" (D-09) |
| NAV-02 | Navigate Home ↔ mode selection ↔ gameplay ↔ results | `NavHost` with type-safe routes replacing `MainActivity`'s direct `GameRoute()` |
| ONB-01 | First-time how-to-play explanation of the jump rule | Onboarding overlay (D-13/14/15) gated on `onboardingSeen` DataStore flag (D-16) |
</phase_requirements>

## Summary

Phase 2 turns the Phase-1 single-screen walking skeleton into the full app shell. The work is overwhelmingly **integration + plumbing on top of an already-solid pure-domain core**, plus a small amount of genuinely new domain code (`ScoreCalculator`, three board specs). The biggest structural change is inserting a type-safe `NavHost` where `MainActivity` currently calls `GameRoute()` directly, and replacing the hardcoded `DomainModule.provideInitialBoard()` English-33 injection with a board that flows from the user's catalog selection through a `LevelRepository` seam.

Three new dependencies must be added to the version catalog (none are currently present): **navigation-compose 2.9.8**, **kotlinx-serialization (Gradle plugin + json 1.11.0)** for type-safe route args, and **datastore-preferences 1.2.1** for the one `onboardingSeen` flag. All are first-party Google/JetBrains artifacts already named in CLAUDE.md's stack. Critically, **stay on navigation-compose 2.9.8** — the 2.10.x line (alpha) bumps Compose `compileSdk` to **API 37**, which is uninstallable in this environment and is exactly the constraint that already forced the core/lifecycle downgrades. `[VERIFIED: developer.android.com navigation release notes]`

**Two board findings are load-bearing and must reach the planner intact:**
1. **Triangle-15 fundamentally conflicts with the project's locked "orthogonal-only" rule.** Authentic triangular peg solitaire jumps along the triangle's *three* axes (horizontal **and** two diagonals) — it is a 3-axis lattice, not the 4-direction square grid `RulesEngine` hardcodes. You cannot represent 3 movement axes with an orthogonal direction set, and the project explicitly lists "Diagonal jumps" as out-of-scope. This is an irreconcilable conflict between two locked items (the 4-board catalog **and** orthogonal-only) — **escalate to the user, do not silently pick.** `[VERIFIED: gibell.net / arxiv triangular solitaire]`
2. **European-37 with a center-empty start is provably impossible to reduce to a single peg** (famous position-class result). A center-start European board can therefore never earn 3★ under D-01 — pick a documented solvable starting vacancy and lock it with a recorded-solution golden test. `[VERIFIED: Wikipedia / gibell.net position-class theory]`

**Primary recommendation:** Plan in thin vertical slices — (a) nav shell + `LevelRepository` seam carrying English-33 end-to-end through Home→Catalog→Gameplay→Results first, then (b) add the other square boards (European-37, Diamond-41) + `ScoreCalculator` + timer, then (c) onboarding, and (d) **gate Triangle-15 behind an explicit user decision on the rules conflict** before building its lattice geometry.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Board catalog data (the 4 classic levels) | Domain (`Boards`) | Data (`LevelRepository`) | Static board specs are pure-Kotlin domain data; the repository wraps them as the single source of truth |
| Serving a selected level (board+spec+meta) | Data (`LevelRepository`) | — | Repository pattern; Phase-3 generator is a second source behind the same seam |
| Star + score computation | Domain (`ScoreCalculator`) | — | Pure function of `(pegsRemaining, elapsedSeconds)`; zero Android deps (QUAL-01) |
| Move legality / game-over | Domain (`RulesEngine`) | — | Already exists; unchanged for square boards |
| Elapsed-time timer (visible) | UI (`GameViewModel`) | — | Wall-clock/coroutine concern; lives in app layer, feeds `ScoreCalculator` |
| Navigation between 4 screens | UI (`NavHost` in `MainActivity`) | — | Compose Navigation; the structural insertion point |
| Route arg → board lookup | UI (`GameViewModel` + `SavedStateHandle`) | Data (`LevelRepository`) | VM reads the typed route arg, asks the repo for the board |
| `onboardingSeen` persistence | Data (`OnboardingRepository`/DataStore) | UI (`HomeViewModel`) | DataStore is a local data source behind a repository; VM consumes the `Flow` |
| Board rendering (incl. mini-previews) | UI (`BoardCanvas`/`BoardGeometry`) | — | Stateless Canvas leaf; geometry generalizes per board footprint |

## Standard Stack

### Core (new dependencies to add this phase)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.navigation:navigation-compose | **2.9.8** | Type-safe `NavHost` + the 4-destination graph | Official Compose navigation; type-safe routes stable since 2.8.0. **2.9.8 is the latest stable** (2026-04-22); 2.10.x is alpha and needs compileSdk 37 — do NOT bump `[VERIFIED: developer.android.com]` |
| org.jetbrains.kotlin.plugin.serialization | **2.3.20** (= Kotlin) | Marks `@Serializable` route classes | Type-safe Navigation requires kotlinx.serialization to encode route args. Plugin version tracks the Kotlin pin `[CITED: CLAUDE.md stack]` |
| org.jetbrains.kotlinx:kotlinx-serialization-json | **1.11.0** | Serialization runtime for route args | Named in CLAUDE.md stack; required runtime for the serialization plugin `[CITED: CLAUDE.md stack]` |
| androidx.datastore:datastore-preferences | **1.2.1** | The single `onboardingSeen` boolean (D-16) | Latest stable (2026-03-11); Flow-based async; replaces SharedPreferences `[VERIFIED: developer.android.com]` |

### Supporting (already in catalog — reuse)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| androidx.hilt:hilt-navigation-compose | 1.3.0 | `hiltViewModel()` scoped per nav destination | Each new screen's VM (Home, Catalog, Results) obtained the same way `GameRoute` already does |
| androidx.lifecycle:lifecycle-viewmodel | 2.9.4 | `ViewModel` + `viewModelScope` for the timer | `GameViewModel` timer coroutine; new VMs |
| androidx.lifecycle:lifecycle-runtime-compose | 2.9.4 | `collectAsStateWithLifecycle` | Already the Route pattern |
| org.jetbrains.kotlinx:kotlinx-coroutines (test) | 1.11.0 | `runTest` virtual time for timer/score VM tests | New VM tests + timer tests |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Type-safe routes (`@Serializable`) | String-route DSL (`composable("game/{id}")`) | String DSL avoids the serialization dependency but loses compile-time arg safety and is against current Android guidance. Recommend type-safe |
| Pass `levelId` through nav, look up board in repo | Pass the whole serialized `Board` bitmask as a nav arg | Passing the id keeps the repository the single source of truth (spec §8.3); passing the board couples nav to domain encoding. **Use id-based lookup** |
| Results args via `@Serializable` route | Shared/activity-scoped ViewModel between Gameplay & Results | A shared VM works but adds scoping complexity; for MVP pass primitive result args (all Ints) through the typed route |
| DataStore Preferences for one flag | Proto DataStore / Room | Overkill for a single boolean; Preferences is the canonical fit and folds into Phase 5 (D-16) |

**Installation (version-catalog additions — `gradle/libs.versions.toml`):**
```toml
[versions]
navigationCompose = "2.9.8"          # latest STABLE; 2.10.x is alpha + needs compileSdk 37
kotlinxSerialization = "1.11.0"
datastore = "1.2.1"
# serialization plugin version tracks the existing `kotlin = "2.3.20"`

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**Version verification:** `navigation-compose 2.9.8` and `datastore-preferences 1.2.1` confirmed as current stable on developer.android.com this session. `kotlinx-serialization-json 1.11.0` is named in CLAUDE.md's verified stack (Maven Central) — confirm coordinates resolve at planning time.

## Package Legitimacy Audit

> Ecosystem is **Gradle/Maven (AndroidX + JetBrains)**, not npm/PyPI/crates. `slopcheck` targets npm/PyPI and is **not applicable**. All four additions are first-party Google Maven / Maven Central artifacts named in CLAUDE.md's verified stack — the lowest-risk possible provenance.

| Package | Registry | Provenance | Source | Disposition |
|---------|----------|-----------|--------|-------------|
| androidx.navigation:navigation-compose 2.9.8 | Google Maven | First-party AndroidX; verified on release notes this session | android.googlesource.com | Approved |
| androidx.datastore:datastore-preferences 1.2.1 | Google Maven | First-party AndroidX; verified stable this session | android.googlesource.com | Approved |
| org.jetbrains.kotlinx:kotlinx-serialization-json 1.11.0 | Maven Central | First-party JetBrains; in CLAUDE.md stack | github.com/Kotlin/kotlinx.serialization | Approved |
| org.jetbrains.kotlin.plugin.serialization 2.3.20 | Gradle Plugin Portal | First-party JetBrains; ships with Kotlin | github.com/JetBrains/kotlin | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none (slopcheck N/A to Maven ecosystem)
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                          ┌─────────────────────────────────────────┐
   App launch ──────────► │ MainActivity → NavHost (type-safe graph) │
                          └───────────────┬─────────────────────────┘
                                          │ start destination
                                          ▼
   ┌──────────────────────────────────────────────────────────────────────────┐
   │  HomeRoute  ──(onboardingSeen==false on first launch)──► Onboarding overlay │
   │   • Classic (enabled)        ◄── reads Flow<Boolean> ── OnboardingRepository │
   │   • Challenge (disabled,                                  └─ DataStore<Prefs>│
   │     "Coming soon" D-09)                                                      │
   │   • "How to play" (D-15) ──► same overlay                                    │
   └───────┬──────────────────────────────────────────────────────────────────┘
           │ navigate(ClassicCatalog)
           ▼
   ┌──────────────────────────────────┐     LevelRepository.classicCatalog()
   │ ClassicCatalogRoute              │ ◄───────────────┐  (single source of truth)
   │  • 4 cards, ascending size (D-12)│                 │
   │  • mini BoardCanvas preview(D-11)│        ┌────────┴───────────────────────┐
   └───────┬──────────────────────────┘        │ LevelRepository                │
           │ navigate(Gameplay(levelId))        │  ├─ ClassicLevelSource (REAL)  │
           ▼                                     │  └─ ChallengeLevelSource (STUB)│◄─ Phase 3
   ┌──────────────────────────────────┐         └────────┬───────────────────────┘
   │ GameRoute / GameViewModel         │                 │ loadClassic(levelId)
   │  • savedStateHandle.toRoute<>()   │ ────────────────┘  → Level{spec, initialBoard,…}
   │  • RulesEngine (move authority)   │
   │  • visible timer (D-04, vmScope)  │
   │  • on terminal status ─► effect ──┼──► navigate(Results(stars,pegs,secs,score,levelId))
   └───────┬──────────────────────────┘     │ score computed by ScoreCalculator (domain)
           │                                  ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │ ResultsRoute / ResultsViewModel  (reads @Serializable route args) │
   │  • stars/pegs/time/score (SCOR-03)  • headline tier (D-08)        │
   │  • Replay(same id) / Next(catalog order+wrap D-06) / Catalog / Home│
   └──────────────────────────────────────────────────────────────────┘
```

### Recommended Project Structure (additions to existing tree)
```
app/src/main/java/com/app/pegsolitaire/
├── navigation/        # NEW: AppNavHost.kt + @Serializable route definitions
├── data/              # NEW: LevelRepository (+ Classic/Challenge sources), OnboardingRepository
├── di/                # DomainModule (modified) + DataModule (NEW: @Binds repos, DataStore)
└── ui/
    ├── home/          # NEW: HomeRoute/HomeScreen/HomeViewModel
    ├── classic/       # NEW: ClassicCatalogRoute/Screen/ViewModel + BoardPreview card
    ├── results/       # NEW: ResultsRoute/Screen/ViewModel
    ├── onboarding/    # NEW: OnboardingOverlay (illustrated steps)
    ├── game/          # MODIFIED: GameViewModel takes levelId via SavedStateHandle + timer
    └── components/    # MODIFIED: BoardGeometry generalized per-footprint

domain/src/main/kotlin/com/app/pegsolitaire/domain/
├── model/Boards.kt    # MODIFIED: + European37, Diamond41 (+ Triangle15 pending Q1)
└── scoring/           # NEW: ScoreCalculator.kt + ScoreParams.kt (constants) + ScoreResult.kt
```

### Pattern 1: Type-safe Navigation graph
**What:** `@Serializable` route classes; `NavHost` with `composable<Route>`; navigate with instances; read args via `toRoute()`.
**When to use:** All four destinations.
```kotlin
// Source: developer.android.com/guide/navigation/design/type-safety
import kotlinx.serialization.Serializable

@Serializable data object Home
@Serializable data object ClassicCatalog
@Serializable data class Gameplay(val levelId: String)
@Serializable data class Results(
    val levelId: String, val stars: Int, val pegsRemaining: Int,
    val elapsedSeconds: Int, val score: Int,
)

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Home) {
        composable<Home> { HomeRoute(
            onPlayClassic = { navController.navigate(ClassicCatalog) }) }
        composable<ClassicCatalog> { ClassicCatalogRoute(
            onSelect = { id -> navController.navigate(Gameplay(id)) }) }
        composable<Gameplay> { GameRoute(           // VM reads levelId via SavedStateHandle
            onFinished = { r -> navController.navigate(r) /* Results instance */ }) }
        composable<Results> { ResultsRoute(
            onReplay = { id -> navController.navigate(Gameplay(id)) { popUpTo<ClassicCatalog>() } },
            onCatalog = { navController.popBackStack(ClassicCatalog, false) },
            onHome = { navController.popBackStack(Home, false) }) }
    }
}
```

### Pattern 2: Selected board reaches GameViewModel via SavedStateHandle
**What:** Replace the hardcoded `provideInitialBoard()` with a typed-route lookup.
```kotlin
// Source: developer.android.com/guide/navigation/design/type-safety (SavedStateHandle.toRoute)
@HiltViewModel
class GameViewModel @Inject constructor(
    private val rulesEngine: RulesEngine,
    private val levelRepository: LevelRepository,
    private val scoreCalculator: ScoreCalculator,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<Gameplay>()
    private val level = levelRepository.loadClassic(args.levelId)
    private val initialBoard: Board = level.initialBoard
    // ... existing select/jump/restart logic unchanged; GameUiState.initial(initialBoard)
}
```
**Planner action:** delete `DomainModule.provideInitialBoard()`; `GameUiState.initial(board)` already accepts any board.

### Pattern 3: `LevelRepository` seam (Repository pattern, single source of truth)
```kotlin
// data/LevelRepository.kt
data class Level(
    val id: String, val displayName: String, val spec: BoardSpec,
    val initialBoard: Board, val holeCount: Int,
)
data class ClassicLevelDescriptor(val id: String, val displayName: String, val holeCount: Int, val previewBoard: Board)

interface LevelRepository {
    fun classicCatalog(): List<ClassicLevelDescriptor>   // CLAS-01, D-12 order
    fun loadClassic(levelId: String): Level              // CLAS-02
    fun loadChallenge(levelNumber: Int): Level           // Phase 3 — STUB now
}

class DefaultLevelRepository @Inject constructor(
    private val classicSource: ClassicLevelSource,
    private val challengeSource: ChallengeLevelSource,
) : LevelRepository {
    override fun classicCatalog() = classicSource.descriptors()
    override fun loadClassic(levelId: String) = classicSource.load(levelId)
    override fun loadChallenge(levelNumber: Int) = challengeSource.load(levelNumber)
}

// ChallengeLevelSource STUB — never invoked this phase (D-09 Challenge route not built):
class StubChallengeLevelSource @Inject constructor() : ChallengeLevelSource {
    override fun load(levelNumber: Int): Level =
        error("Challenge generation arrives in Phase 3")   // intentionally unreachable
}
```
Bind with Hilt `@Binds` in a new `DataModule`; keep the interface stable so Phase 3 swaps `StubChallengeLevelSource` → real generator with **zero churn** to the repository or callers.

### Pattern 4: `ScoreCalculator` (pure domain, JVM-tested)
```kotlin
// domain/scoring/ScoreCalculator.kt  (no android.* imports — QUAL-01)
object ScoreParams {
    const val THREE_STAR_MAX_PEGS = 1       // D-01
    const val TWO_STAR_MAX_PEGS = 3         // D-01 (2–3 band cutoff — sanity-check this)
    const val PEG_VALUE = 100               // [ASSUMED] sane default
    const val TIME_BONUS_CAP = 5000         // [ASSUMED]
    const val DECAY_PER_SECOND = 10         // [ASSUMED] bonus hits 0 at 500s
    const val MIN_STARS = 1                 // D-01: any completed game ≥ 1★ (never 0)
}
data class ScoreResult(val stars: Int, val score: Int, val pegsRemaining: Int, val pegsCleared: Int, val elapsedSeconds: Int)

object ScoreCalculator {
    fun stars(pegsRemaining: Int): Int = when {
        pegsRemaining <= ScoreParams.THREE_STAR_MAX_PEGS -> 3
        pegsRemaining <= ScoreParams.TWO_STAR_MAX_PEGS -> 2
        else -> ScoreParams.MIN_STARS                       // D-01: >3 → 1★ (NOT 0★)
    }
    fun score(pegsCleared: Int, elapsedSeconds: Int): Int =
        pegsCleared * ScoreParams.PEG_VALUE +
            maxOf(0, ScoreParams.TIME_BONUS_CAP - elapsedSeconds * ScoreParams.DECAY_PER_SECOND)
}
```
`pegsCleared = initialBoard.pegCount - finalBoard.pegCount` (each jump removes exactly one peg). **Note the D-01 override of spec §7:** spec said "4+ pegs → no stars"; **D-01 wins — minimum is 1★**.

### Pattern 5: Visible in-game timer (D-04)
**What:** Accumulate wall-clock elapsed in the VM; tick a `Flow` each second for display; stop on terminal status.
```kotlin
// Inject a kotlin.time.TimeSource (or simple () -> Long provider) for deterministic tests.
private var startMark: TimeSource.Monotonic.ValueTimeMark? = null
fun onFirstMove() { if (startMark == null) startMark = TimeSource.Monotonic.markNow() }
private fun startTicker() = viewModelScope.launch {
    while (isActive && _uiState.value.status == GameStatus.Playing) {
        _uiState.update { it.copy(elapsedSeconds = elapsedSeconds()) }
        delay(1_000)
    }
}
```
**Config-change safety:** the `ViewModel` survives rotation, and elapsed is computed from a mark, so rotation does not reset the clock. **Process death / resume is Phase 5** — do not persist the timer now.

### Pattern 6: DataStore for `onboardingSeen`
```kotlin
// data/OnboardingRepository.kt
private val Context.dataStore by preferencesDataStore(name = "app_prefs")
class OnboardingRepository @Inject constructor(@ApplicationContext ctx: Context) {
    private val ds = ctx.dataStore
    private val seenKey = booleanPreferencesKey("onboarding_seen")
    val onboardingSeen: Flow<Boolean> = ds.data.map { it[seenKey] ?: false }
    suspend fun markSeen() { ds.edit { it[seenKey] = true } }
}
```
`HomeViewModel` collects `onboardingSeen`; first launch (`false`) shows the overlay over Home (D-14); dismiss calls `markSeen()`. The permanent "How to play" (D-15) opens the same overlay composable independent of the flag.

### Anti-Patterns to Avoid
- **Passing the whole `Board`/bitmask through nav args.** Pass `levelId`; let `LevelRepository` be the single source of truth.
- **Computing stars/score in the UI or ViewModel.** It is pure domain — keep it in `ScoreCalculator` (QUAL-01/QUAL-04).
- **Hardcoding `GRID_DIMENSION = 7` for new boards.** `BoardGeometry`/`BoardMetrics` are English-33-specific today; derive the grid extent from the `BoardSpec` (see Pitfall 1).
- **Bumping navigation-compose to 2.10.x.** Alpha + compileSdk 37 → breaks the environment's pinned compileSdk 36.
- **Making game-over→Results a replayable state that re-navigates on every recomposition.** Use a one-shot effect (Channel/SharedFlow) per compose-skill, or a `LaunchedEffect` keyed on the *transition* to terminal status.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Screen navigation + back stack | A manual `when(currentScreen)` state machine | navigation-compose `NavHost` | Back stack, args, lifecycle, deep-link-ready, type-safe |
| Passing typed args between screens | Manual `Bundle`/string parsing | `@Serializable` routes + `toRoute()` | Compile-time safety, no key typos |
| Persisting the onboarding flag | SharedPreferences / a file | datastore-preferences | Async Flow API; folds into Phase 5; SharedPreferences is discouraged (CLAUDE.md) |
| Board solvability validation (no Solver this phase) | A search solver written now | **Recorded-solution golden test** (replay a known winning move-list through `RulesEngine` to 1 peg) | Reuses the existing `BoardCodec`/golden harness; mirrors the Phase-3 recorded-solution pattern; no new machinery |
| Star/score logic | Inline `if` in the Results composable | `ScoreCalculator` pure object | Testable, board-agnostic, single source of the D-01/D-03 rules |
| Timer | A `Handler`/manual thread | `viewModelScope` + `delay` + injected `TimeSource` | Lifecycle-scoped, cancellable, deterministically testable |

**Key insight:** This phase adds almost no new *algorithms* — the domain core (bitmask `Board`, `RulesEngine`, golden harness) already generalizes to every square footprint. The risk is concentrated in (a) the two board-correctness findings and (b) not over-engineering the nav/DI plumbing.

## Board Footprints — Concrete Definitions

All fit the ≤64-hole `Long`-bitmask `BoardSpec`. Follow the existing `Boards.kt` pattern: a `private object XLayout` of scoped constants → derived `inPlayPositions` → a `BoardSpec` + initial `Board` in the `Boards` object. **Solvability has no Solver this phase — lock each board's start with a recorded-solution golden test (see Don't Hand-Roll).**

### European-37 (Continental / octagon) — square grid, `RulesEngine` works unchanged
7×7 lattice with the four 2×2-ish corners trimmed; 37 holes. Per-row in-play column ranges (cols 0–6):
```
row 0: cols 2..4   (3)            . . O O O . .
row 1: cols 1..5   (5)            . O O O O O .
row 2: cols 0..6   (7)            O O O O O O O
row 3: cols 0..6   (7)            O O O X O O O   ← X = start vacancy (see WARNING)
row 4: cols 0..6   (7)            O O O O O O O
row 5: cols 1..5   (5)            . O O O O O .
row 6: cols 2..4   (3)            . . O O O . .
```
Generation rule: `cornerTrim = [2,1,0,0,0,1,2]`; in-play iff `col in trim[row]..(6 - trim[row])`. Total = 3+5+7+7+7+5+3 = **37**. `GRID_DIMENSION = 7`, square aspect.

> **WARNING — center-empty European-37 is provably unsolvable to one peg.** `[VERIFIED]` The 37-hole center-complement problem has no solution (all six position-class parities even → no single-peg position reachable). Under D-01 a center-start European board can **never** earn 3★. **Recommendation:** choose a documented solvable starting vacancy (off-center) and validate it with a recorded-solution golden test, OR accept that European-37 tops out at 2★ and surface that to the user. The specific solvable vacancy is `[ASSUMED]` until validated — **do not assume center works.**

### Diamond-41 — square grid (rotated 45°), `RulesEngine` works unchanged
9×9 lattice, diamond/rhombus; 41 holes. Per-row in-play column ranges (cols 0–8):
```
row 0: col 4          (1)         . . . . O . . . .
row 1: cols 3..5      (3)         . . . O O O . . .
row 2: cols 2..6      (5)         . . O O O O O . .
row 3: cols 1..7      (7)         . O O O O O O O .
row 4: cols 0..8      (9)         O O O O X O O O O   ← X = center start (4,4)
row 5: cols 1..7      (7)         . O O O O O O O .
row 6: cols 2..6      (5)         . . O O O O O . .
row 7: cols 3..5      (3)         . . . O O O . . .
row 8: col 4          (1)         . . . . O . . . .
```
Generation rule: `halfWidth = [0,1,2,3,4,3,2,1,0]`; in-play iff `col in (4 - hw[row])..(4 + hw[row])`. Total = 1+3+5+7+9+7+5+3+1 = **41**. `GRID_DIMENSION = 9`, square aspect. Center-empty start `(4,4)` is the conventional one; `[ASSUMED]` solvable to one peg — validate with a recorded-solution golden test.

### Triangle-15 — **triangular lattice; see Open Question 1 before building**
5-row triangle, 15 holes. Coordinate scheme that fits the bitmask: `Position(row, col)` with `row in 0..4`, `col in 0..row`:
```
row 0:            (0,0)                    O
row 1:          (1,0)(1,1)                O O
row 2:        (2,0)(2,1)(2,2)            O O O
row 3:      (3,0)(3,1)(3,2)(3,3)        O O O O
row 4:    (4,0)(4,1)(4,2)(4,3)(4,4)    O O O O O
```
Total = 1+2+3+4+5 = **15** (≤64 ✓). Classic solvable start: apex `(0,0)` empty.
**BUT:** authentic triangle jumps run along the triangle's three axes — e.g. `(2,0)→(2,2)` over `(2,1)` (horizontal), `(2,0)→(0,0)` over `(1,0)` (a diagonal), `(2,2)→(0,0)` over `(1,1)` (the other diagonal). `RulesEngine`'s orthogonal `(±1 row / ±1 col)` directions do **not** produce these — and the third axis cannot be expressed with a 2-axis orthogonal direction set. **This board cannot be played authentically under the locked orthogonal-only rule** — resolve Open Question 1 first.

## Triangle-15 Rendering (geometry generalization)

Even setting the rules conflict aside, `BoardGeometry`/`BoardMetrics` are currently English-33-specific and must generalize for **all** new boards (catalog mini-previews D-11 reuse this):

1. **Derive grid extent from the spec**, not a constant: `cols = spec.inPlay.maxOf { col } + 1`, `rows = maxOf { row } + 1`; use `max(rows, cols)` (square boards) for `cellSize`. Removes the hardcoded `BoardMetrics.GRID_DIMENSION = 7`.
2. **Square boards (English/European/Diamond):** existing `center(pos) = origin + (col+0.5, row+0.5) * cell` works once the dimension is derived.
3. **Triangle lattice (if built):** rows are horizontally offset and vertically compressed:
   `x = originX + (col - row/2 + rowsMax/2) * cell`, `y = originY + row * cell * sin(60°)`. A peg at `(row, col)` sits at the midpoint of an equilateral cell. `positionAt(offset)` must invert this mapping and still bounds-check against `spec` (T-04-01 contract preserved).
4. Recommend a small **`BoardLattice` strategy** (`SquareLattice` / `TriangleLattice`) selected from the `BoardSpec` (e.g. a `lattice` field) so `BoardGeometry` stays one class with two coordinate mappers, and `onCellTap` remains bounds-checked for both. The catalog preview renders the same way at small size.

## Common Pitfalls

### Pitfall 1: English-33-specific geometry breaks the new boards
**What goes wrong:** `BoardMetrics.GRID_DIMENSION = 7` and `BOARD_ASPECT_RATIO = 1f` are baked for English-33; European (7) happens to match but Diamond (9) and Triangle (5) do not — pegs render off-grid or clipped.
**Why:** Phase-1 geometry was scoped to one board.
**How to avoid:** Derive grid dimension from the `BoardSpec` bounds; pass it into `BoardGeometry.forCanvas`. Add a per-board JVM `BoardGeometryTest` (the existing test already covers the pattern).
**Warning signs:** Catalog previews look fine for English/European but Diamond/Triangle are misaligned.

### Pitfall 2: Center-start European-37 can never reach 3★
**What goes wrong:** Player perceives the board as broken/unfair — a "perfect" solve is mathematically impossible.
**Why:** Position-class parity (verified result).
**How to avoid:** Use a documented solvable starting vacancy; lock it with a recorded-solution golden test. Escalate the start choice if a validated one can't be sourced without a Solver.
**Warning signs:** Best achievable on European is always ≥2 pegs.

### Pitfall 3: navigation-compose pulls a compileSdk-37 transitive line
**What goes wrong:** Build fails to install (compileSdk 37 unavailable) — the exact failure that already forced core→1.18.0 and lifecycle→2.9.4 pins.
**Why:** Newer AndroidX lines require compileSdk 37.
**How to avoid:** Pin navigation-compose **2.9.8** (stable, pre-37). Mirror the existing `resolutionStrategy.eachDependency` guard in `app/build.gradle.kts` if any transitive lifecycle/core line drifts up. Run a dependency-resolution check before coding.
**Warning signs:** `:app` dependency resolution reports lifecycle/core 2.11.x / 1.19.x after adding navigation.

### Pitfall 4: Game-over → Results fires on every recomposition
**What goes wrong:** Multiple Results navigations / back-stack pollution.
**Why:** Phase-1 made terminal status a *replayable* `GameUiState.status` (deliberately, to survive rotation as a dialog). Navigation is a one-shot.
**How to avoid:** Emit a one-shot effect (Channel/SharedFlow) when status *transitions* to terminal, collected once in `GameRoute`; or `LaunchedEffect(state.status)` guarding a single navigate. Remove `GameOverDialog`/`GameOverOverlay` (D-05 replaces it).
**Warning signs:** Results appears twice; back button returns to a finished game that immediately re-navigates.

### Pitfall 5: ViewModel arg-route lookup throws on a bad/missing levelId
**What goes wrong:** `loadClassic("typo")` crashes the Gameplay screen.
**Why:** Untrusted route arg.
**How to avoid:** `ClassicLevelSource.load` validates the id against the known catalog and fails fast in tests; the catalog only ever emits valid ids, so the surface is small. Keep ids as named constants (QUAL-03), not string literals at call sites.

### Pitfall 6: kotlinx-serialization plugin not applied → `@Serializable` routes fail at runtime
**What goes wrong:** Type-safe routes compile but crash with a serializer-not-found at navigate time.
**Why:** The serialization Gradle plugin must be applied to `:app` (it generates serializers); adding only the json runtime is insufficient.
**How to avoid:** Apply `org.jetbrains.kotlin.plugin.serialization` in `:app` (via the convention plugin or directly) AND add `kotlinx-serialization-json`. Verify with one navigation in an instrumentation/Robolectric test.

## Code Examples

### ResultsViewModel reading typed args + computing "Next board" (D-06)
```kotlin
// Source: developer.android.com type-safety + LevelRepository (this research)
@HiltViewModel
class ResultsViewModel @Inject constructor(
    levelRepository: LevelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<Results>()
    private val order = levelRepository.classicCatalog().map { it.id }   // D-12 ascending
    val nextLevelId: String = order[(order.indexOf(args.levelId) + 1) % order.size]  // wraps (D-06)
    val uiState = ResultsUiState(
        stars = args.stars, pegsRemaining = args.pegsRemaining,
        elapsedSeconds = args.elapsedSeconds, score = args.score,
        replayId = args.levelId, nextId = nextLevelId,
    )
}
```

### Recorded-solution golden test (solvability without a Solver)
```kotlin
// domain/src/test/.../golden — reuses existing BoardCodec/GoldenMasterAssert
@Test fun `english33 reference solution reduces to one peg`() {
    val solution: List<Move> = English33ReferenceSolution.moves   // committed fixture
    val finalBoard = solution.fold(Boards.english33Initial) { b, m ->
        require(m in RulesEngine.legalMoves(b)); RulesEngine.applyMove(b, m)
    }
    assertThat(finalBoard.pegCount).isEqualTo(1)
}
```
Repeat per board; the committed move-list IS the proof of solvability and forward-feeds Phase 3's recorded-solution contract.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| String-route Navigation DSL (`"game/{id}"`) | `@Serializable` type-safe routes + `toRoute()` | Navigation 2.8.0 (2024) | Compile-time arg safety; use it |
| SharedPreferences | DataStore Preferences (Flow-based) | Ongoing; ESP deprecated | `onboardingSeen` uses DataStore (D-16, CLAUDE.md) |
| `SavedStateHandle.get<>("key")` | `savedStateHandle.toRoute<Route>()` | Navigation 2.8.0 | No key strings; the VM reconstructs its typed route |

**Deprecated/outdated:**
- navigation-compose 2.10.x line — alpha, requires compileSdk 37; **not for this environment**.
- `GameOverDialog`/`GameOverOverlay` (Phase 1) — replaced by the Results destination (D-05).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | A solvable off-center starting vacancy exists & is sourceable for European-37 without a Solver | Board Footprints | If not sourced, European tops out at 2★ or needs an early Solver; affects D-01 fairness |
| A2 | Diamond-41 center-empty `(4,4)` is solvable to one peg | Board Footprints | If not, Diamond can't earn 3★; validate via recorded-solution test |
| A3 | `PEG_VALUE=100`, `TIME_BONUS_CAP=5000`, `DECAY=10` are reasonable D-03 constants | ScoreCalculator | Wrong feel only; tunable, low risk; planner sanity-checks |
| A4 | navigation-compose 2.9.8 resolves without dragging in a compileSdk-37 transitive line | Standard Stack / Pitfall 3 | Build won't install; mitigated by `resolutionStrategy` guard (already a project pattern) |
| A5 | Triangle-15 apex-empty `(0,0)` is the solvable start (if the board is built at all) | Board Footprints | Moot unless Q1 resolves to "build Triangle-15"; validate if so |

**These five assumptions need confirmation before they become locked decisions.** A1/A2/A5 are validated by committing recorded-solution golden fixtures (no Solver needed). A4 is validated by a dependency-resolution dry run.

## Open Questions

1. **[BLOCKING] Triangle-15 vs. the locked orthogonal-only rule — fundamental conflict.**
   - What we know: Authentic triangular peg solitaire jumps along **three** axes (horizontal + two diagonals) `[VERIFIED]`. `RulesEngine` is hardcoded to 4 orthogonal directions, and the project lists "Diagonal jumps" as **out-of-scope** (CLAUDE.md, REQUIREMENTS). The 4-board catalog including Triangle-15 is **also** locked (CLAS-01, D-11/D-12, roadmap success criterion).
   - What's unclear: Which locked item gives. You cannot have an authentic Triangle-15 **and** orthogonal-only rules.
   - Recommendation: **Escalate to the user.** Options: **(a)** Generalize the move model — add a per-board direction/lattice set so Triangle-15 uses its 3 triangular axes while square boards stay orthogonal (cleanest authentic path, but explicitly relaxes the "orthogonal-only" constraint → needs user sign-off). **(b)** Replace Triangle-15 in the catalog with a 4th orthogonal board (requirements change → needs user sign-off). **(c)** Defer Triangle-15 to a later phase and ship Phase 2 with three boards (descope). Do **not** ship a fake orthogonal "triangle" that isn't the real game. Recommend (a) if the user values the authentic catalog, scoped so the generalization doesn't leak diagonal moves into Challenge (Phase 3 stays orthogonal-only).

2. **European-37 starting vacancy (solvability).**
   - What we know: Center-empty is provably **not** reducible to one peg `[VERIFIED]`. No Solver exists until Phase 4.
   - What's unclear: A specific solvable off-center vacancy + its winning line, sourced from literature.
   - Recommendation: Source a documented solvable start, lock it with a recorded-solution golden test. If none can be sourced without search, either pull a minimal Solver forward or accept European caps at 2★ (surface to user).

3. **Game-end → Results transition mechanism.**
   - What we know: Phase 1 made terminal status replayable state; navigation needs a one-shot.
   - Recommendation: One-shot effect channel per compose-skill (preferred) or a guarded `LaunchedEffect(status)`. Planner picks; both documented above.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| navigation-compose | NAV-01/02 nav graph | Add to catalog | 2.9.8 (stable) | none — required |
| kotlinx-serialization plugin + json | Type-safe route args | Add to catalog | 2.3.20 / 1.11.0 | String-route DSL (no serialization), not recommended |
| datastore-preferences | ONB-01 `onboardingSeen` | Add to catalog | 1.2.1 (stable) | none needed |
| compileSdk 37 | navigation-compose **2.10.x only** | ✗ (not installable) | — | **Stay on 2.9.8** — avoids the requirement entirely |
| Solver (search) | Runtime solvability validation | ✗ (Phase 4) | — | Recorded-solution golden fixtures (no search) |

**Missing dependencies with no fallback:** none (all three additions are first-party and current).
**Missing with fallback:** Solver → recorded-solution golden tests; compileSdk 37 → pin navigation 2.9.8.
**Action:** Run a `:app` dependency-resolution dry run after adding navigation to confirm no compileSdk-37 transitive line appears (mirror the existing `resolutionStrategy.eachDependency` guard if it does).

## Validation Architecture

> `workflow.nyquist_validation` is enabled (config.json). Section included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for JVM Compose/UI |
| Config file | Gradle (`:domain` JVM tests, `:app` unit tests) — existing |
| Quick run command | `./gradlew :domain:test` (pure-domain: ScoreCalculator, board specs, recorded solutions) |
| Full suite command | `./gradlew :domain:test :app:testDebugUnitTest ktlintCheck detekt` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SCOR-01 | star bands (1→3★, 2–3→2★, >3→1★) | unit | `./gradlew :domain:test --tests *ScoreCalculatorTest*` | ❌ Wave 0 |
| SCOR-02 | score formula + time decay floor at 0 | unit | `./gradlew :domain:test --tests *ScoreCalculatorTest*` | ❌ Wave 0 |
| CLAS-02 | each board's recorded solution → 1 peg | unit (golden) | `./gradlew :domain:test --tests *ReferenceSolution*` | ❌ Wave 0 |
| CLAS-01 | catalog returns 4 descriptors in D-12 order | unit | `./gradlew :app:testDebugUnitTest --tests *LevelRepositoryTest*` | ❌ Wave 0 |
| CLAS-02 | new-board geometry maps in bounds | unit | `./gradlew :app:testDebugUnitTest --tests *BoardGeometryTest*` | ⚠️ extend existing |
| NAV-02 | Home→Catalog→Gameplay→Results reachable | UI (Robolectric) | `./gradlew :app:testDebugUnitTest --tests *NavFlowTest*` | ❌ Wave 0 |
| SCOR-03 | Results shows stars/pegs/time/score | UI | `./gradlew :app:testDebugUnitTest --tests *ResultsScreenTest*` | ❌ Wave 0 |
| ONB-01 | first launch shows overlay; markSeen hides it | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests *HomeViewModelTest*` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :domain:test` (fast pure-domain feedback)
- **Per wave merge:** `./gradlew :domain:test :app:testDebugUnitTest`
- **Phase gate:** full suite + `ktlintCheck detekt` + ArchUnit domain-purity green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `domain/.../scoring/ScoreCalculatorTest.kt` — SCOR-01/02 bands + formula + decay floor
- [ ] `domain/.../golden/*ReferenceSolutionTest.kt` — recorded winning line per board → 1 peg (CLAS-02; doubles as solvability proof)
- [ ] `app/.../data/LevelRepositoryTest.kt` — catalog order (D-12) + `loadClassic` round-trip
- [ ] `app/.../ui/components/BoardGeometryTest.kt` — extend for Diamond-9 / European-7 (+ Triangle if Q1 resolves)
- [ ] `app/.../navigation/NavFlowTest.kt` — Robolectric end-to-end across the 4 destinations
- [ ] `app/.../ui/home/HomeViewModelTest.kt` — `onboardingSeen` Flow + `markSeen` (fake DataStore/repo)

## Security Domain

> `security_enforcement` not explicitly disabled in config → included. Scope is narrow: a fully offline, single-player game with no network, no auth, no user accounts, no PII.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No accounts/sign-in until Slice 2 Play Games |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | No multi-user data |
| V5 Input Validation | yes | Tap offsets already bounds-checked (`BoardGeometry.positionAt`, T-04-01); **nav route args** (`levelId`) validated against the known catalog before `loadClassic` (Pitfall 5) |
| V6 Cryptography | no | `onboardingSeen` is non-sensitive; plain DataStore is correct (no encryption needed) |

### Known Threat Patterns for this stack
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Untrusted nav arg → repository lookup crash | Tampering / DoS | Validate `levelId` against catalog; named-constant ids; fail fast in tests |
| Untrusted tap pixel → out-of-bounds move | Tampering | Existing `positionAt` bounds-check + `RulesEngine` re-validates legality (unchanged) |
| Over-collecting persisted data | (privacy) | Persist only the single non-sensitive `onboardingSeen` boolean this phase |

No cryptography, secrets, or network controls are in scope for Phase 2.

## Sources

### Primary (HIGH confidence)
- developer.android.com/jetpack/androidx/releases/navigation — navigation-compose 2.9.8 latest stable; 2.10.x-alpha bumps compileSdk to 37
- developer.android.com/jetpack/androidx/releases/datastore — datastore-preferences 1.2.1 stable (2026-03-11)
- developer.android.com/guide/navigation/design/type-safety — `@Serializable` routes, `toRoute()`, `SavedStateHandle.toRoute()`
- en.wikipedia.org/wiki/Peg_solitaire + gibell.net/pegsolitaire — European-37 center-complement unsolvability (position-class theory)
- gibell.net/pegsolitaire/tindex.html + arxiv.org/pdf/math/0703865 — triangular solitaire jumps along diagonal + horizontal axes
- Existing codebase: `BoardSpec`/`Board`/`RulesEngine`/`Boards.kt`/`BoardGeometry.kt`/`GameViewModel.kt`/`DomainModule.kt`/`libs.versions.toml` (read this session)
- CLAUDE.md — pinned stack (navigation 2.9.8, datastore 1.2.1, kotlinx-serialization 1.11.0, Kotlin 2.3.20)

### Secondary (MEDIUM confidence)
- Board per-row footprints (European octagon 3-5-7-7-7-5-3; Diamond 1-3-5-7-9-7-5-3-1) — standard layouts cross-checked against multiple solitaire references; coordinates derived this session

### Tertiary (LOW confidence — flagged for validation)
- Specific solvable starting vacancies / winning lines for European-37, Diamond-41, Triangle-15 — `[ASSUMED]`, to be locked by recorded-solution golden fixtures (A1/A2/A5)
- ScoreCalculator constant values — `[ASSUMED]` sane defaults, planner to sanity-check

## Metadata

**Confidence breakdown:**
- Navigation / DataStore / serialization stack: HIGH — verified versions + compileSdk constraint this session
- LevelRepository seam + selected-board flow: HIGH — standard Repository + SavedStateHandle pattern over an existing clean seam
- ScoreCalculator placement + formula: HIGH (shape locked by D-01/D-03); constants MEDIUM (tunable)
- Board footprints (European/Diamond geometry): MEDIUM — coordinates derived, solvability starts need recorded-solution validation
- Triangle-15: HIGH confidence in the *conflict finding*; the *resolution* is a user decision (BLOCKING)
- Pitfalls: HIGH — most are grounded in the project's own documented compileSdk-36 history

**Research date:** 2026-06-28
**Valid until:** ~2026-07-28 (stack stable; re-verify navigation/datastore versions if planning slips a month)
