# Phase 2: Classic Mode, Scoring & Navigation - Research

**Researched:** 2026-06-28 (revised 2026-06-28 — board catalog follow-up)
**Domain:** Jetpack Compose Navigation (type-safe), Repository-pattern data seam, pure-Kotlin scoring, DataStore, multi-footprint square-grid Canvas geometry
**Confidence:** HIGH (nav/DataStore/scoring/repo, board footprints, board solvability findings). The earlier Triangle-15 BLOCKING conflict is **RESOLVED** (Triangle-15 dropped per CONTEXT D-17/D-18).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Scoring — Stars**
- **D-01:** Stars from **absolute pegs remaining, identical on every board** — 1 peg = 3★, 2–3 pegs = 2★, finished-but->3 = 1★. The 2★ band cutoff (2–3) is a named constant the planner may sanity-check; the *approach* (absolute pegs) is locked.
- **D-02:** The target hole is **ignored for stars**. Stars depend solely on peg count. (Load-bearing this phase: several boards can only finish a single peg *away* from the start hole — D-02 means that still scores 3★.)

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
- **D-12:** ~~Triangle-15 → English-33 → European-37 → Diamond-41~~ **SUPERSEDED by D-17/D-18** — Triangle-15 dropped; ordering is ascending hole count over the new 5-board set.

**Board Catalog Resolution (post-research, user-approved 2026-06-28) — overrides the Triangle-15 discretion item, D-11/D-12 board list**
- **D-17 (BLOCKING conflict RESOLVED):** Triangle-15 is irreconcilable with the locked orthogonal-only jump rule (authentic triangular solitaire needs 3-axis/diagonal jumps `RulesEngine` cannot express). **Resolution: drop Triangle-15 entirely; adopt a 5-board all-orthogonal-square-grid catalog.** Orthogonal-only rule and `RulesEngine` stay unchanged; **no triangular-lattice geometry is built.**
- **D-18 (the locked catalog):** Exactly five boards, all playable orthogonally, all ≤64-hole `Long`-bitmask `BoardSpec`:
  1. **English-33** — already built in Phase 1 (reference pattern only).
  2. **European/French-37** — must use a **documented solvable off-center starting vacancy** (center-empty is provably unsolvable); lock with a recorded-solution golden test (D-19).
  3. **Diamond-41** — clean orthogonal board. **(Research correction: center is ALSO provably unsolvable — needs an off-center start; see Board Footprints.)**
  4. **J.C. Wiegleb-45** — German cross, 1779; new footprint.
  5. **Asymmetric 3-3-2-2** — George Bell's asymmetric cross; new footprint + hole count.
  - Only `BoardGeometry`/`BoardMetrics` change: **derive grid dimension per board** (away from the hardcoded `GRID_DIMENSION = 7`/`aspect = 1f`). **No `BoardLattice`/triangle strategy.**
  - Catalog order (replaces D-12), ascending hole count → **English-33 (33) → European-37 (37) → Asymmetric 3-3-2-2 (39) → Diamond-41 (41) → Wiegleb-45 (45)**. Defines the "Next board" wrap order (D-06).
- **D-19 (solvability validation, no Solver yet):** No Solver until Phase 4. Validate every non-trivial board's start is solvable to one peg by committing a **recorded-solution golden test** per board (replay a known winning move-list through `RulesEngine` to a single peg), reusing the existing `BoardCodec`/golden-master harness. Guarantees 3★ reachable (D-01 fairness) and forward-feeds the Phase-3 generator contract.

**Onboarding**
- **D-13:** First-time how-to-play is a **dismissable, illustrated overlay** (steps showing the orthogonal jump). Skippable.
- **D-14:** Triggers on **first app launch, over Home** (once).
- **D-15:** A **permanent "How to play" / help affordance** exists (Home or game top bar) for re-reading.
- **D-16:** "Onboarding seen" tracked with a **single DataStore-Preferences boolean (`onboardingSeen`)**, introduced now. Add `androidx.datastore:datastore-preferences` (1.2.1) for this flag only. All other persistence stays in Phase 5.

### Claude's Discretion
- **Navigation library wiring** — introduce Navigation Compose 2.9.8; type-safe nav graph for the destinations.
- **How the selected board reaches `GameViewModel`** — nav arg + `SavedStateHandle`, repo lookup by level id, etc.
- **`LevelRepository` interface shape** — real Classic source + stubbed Challenge source.
- **The four new board footprints + initial states** — European-37, Asymmetric 3-3-2-2, Diamond-41, Wiegleb-45 (per D-18); all fit the ≤64-hole `Long`-bitmask `BoardSpec`. (Triangle-15 removed per D-17.)
- **Board rendering (square grid only)** — all five boards are square-grid; only the hardcoded grid dimension needs generalizing per board. No triangular-lattice geometry. `onCellTap` stays bounds-checked.
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
| CLAS-01 | Browse and select from a catalog of classic boards | Catalog screen over `LevelRepository.classicCatalog()`; D-11 mini-previews via generalized `BoardGeometry`; D-18 ascending order (5 boards) |
| CLAS-02 | Play any classic board to completion with correct rules | 4 new `BoardSpec`/`Board` footprints (European-37, Asymmetric-39, Diamond-41, Wiegleb-45); **`RulesEngine` unchanged — all five are orthogonal square-grid boards** |
| SCOR-01 | 1–3 stars based on pegs remaining | `ScoreCalculator.stars()` pure domain, D-01 bands; JVM-tested |
| SCOR-02 | Elapsed time → separate numeric score | `ScoreCalculator.score()` D-03 formula; in-game timer (D-04) supplies `elapsedSeconds` |
| SCOR-03 | Results screen shows stars, pegs, time, score | Results destination (D-05) reading a `@Serializable` results route arg |
| NAV-01 | Home with entry to Classic and Challenge | Home mode-select (D-10); Challenge disabled "Coming soon" (D-09) |
| NAV-02 | Navigate Home ↔ mode selection ↔ gameplay ↔ results | `NavHost` with type-safe routes replacing `MainActivity`'s direct `GameRoute()` |
| ONB-01 | First-time how-to-play explanation of the jump rule | Onboarding overlay (D-13/14/15) gated on `onboardingSeen` DataStore flag (D-16) |
</phase_requirements>

## Summary

Phase 2 turns the Phase-1 single-screen walking skeleton into the full app shell. The work is overwhelmingly **integration + plumbing on top of an already-solid pure-domain core**, plus a small amount of genuinely new domain code (`ScoreCalculator`, four board specs). The biggest structural change is inserting a type-safe `NavHost` where `MainActivity` currently calls `GameRoute()` directly, and replacing the hardcoded `DomainModule.provideInitialBoard()` English-33 injection with a board that flows from the user's catalog selection through a `LevelRepository` seam.

Three new dependencies must be added to the version catalog (none currently present): **navigation-compose 2.9.8**, **kotlinx-serialization (Gradle plugin + json 1.11.0)** for type-safe route args, and **datastore-preferences 1.2.1** for the one `onboardingSeen` flag. All are first-party Google/JetBrains artifacts named in CLAUDE.md's stack. Critically, **stay on navigation-compose 2.9.8** — the 2.10.x line (alpha) bumps Compose `compileSdk` to **API 37**, uninstallable in this environment. `[VERIFIED: developer.android.com navigation release notes]`

**The board catalog is now a clean, fully-orthogonal 5-board set (CONTEXT D-17/D-18). The Triangle-15 BLOCKING conflict is resolved by dropping it — no lattice geometry, `RulesEngine` unchanged.** What remains load-bearing is **board solvability**, and the follow-up research surfaced an important correction:

1. **Two boards cannot be solved to one peg from the center hole** (provable position-class results, not opinion):
   - **European-37 center-empty is unsolvable** `[VERIFIED: Wikipedia / gibell.net / Durango Bill]`. Of its 37 holes, **16 are solvable single-vacancy starts**; the four orthogonal neighbours of center (e.g. `(3,2)`) are among them — use one of those.
   - **Diamond-41 center-empty is ALSO unsolvable** `[VERIFIED: gibell.net (George Bell) / Bell-Beasley]` — **this corrects the prior research's assumption A2.** Only **4** single-vacancy-to-single-survivor problems are solvable on the diamond (26 moves each); center is not one of them. Diamond-41 must use an off-center start.
2. **The other three boards solve from center:** English-33 (classic), **Wiegleb-45 central game is solvable in 22 moves** `[VERIFIED: gibell.net]`, and the **Asymmetric 3-3-2-2 board is "completely solvable"** (every vacancy, incl. center, reaches one peg) `[VERIFIED: gibell.net GenCross]`.
3. **D-02 (target ignored for stars) makes all this work:** on the French/diamond boards the survivor never lands in the start hole, but under D-02 *any* single-peg finish is 3★ — so an off-center start still earns the perfect score.

**Primary recommendation:** Plan in thin vertical slices — (a) nav shell + `LevelRepository` seam carrying English-33 end-to-end through Home→Catalog→Gameplay→Results first, then (b) add the other four square boards + `ScoreCalculator` + timer, then (c) onboarding. **Solvability for every non-trivial board is proven by a committed recorded-solution golden fixture (D-19), GENERATED once by an authoring-time DFS/IDDFS script that is NOT shipped in `:domain`** (see Don't Hand-Roll + Code Examples).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Board catalog data (the 5 classic levels) | Domain (`Boards`) | Data (`LevelRepository`) | Static board specs are pure-Kotlin domain data; the repository wraps them as the single source of truth |
| Serving a selected level (board+spec+meta) | Data (`LevelRepository`) | — | Repository pattern; Phase-3 generator is a second source behind the same seam |
| Star + score computation | Domain (`ScoreCalculator`) | — | Pure function of `(pegsRemaining, elapsedSeconds)`; zero Android deps (QUAL-01) |
| Move legality / game-over | Domain (`RulesEngine`) | — | Already exists; **unchanged for all five square boards** |
| Recorded-solution solvability proof | Test fixtures (`:domain` test) + one-off authoring tool | — | Committed move-list replayed through `RulesEngine`; no Solver shipped (D-19) |
| Elapsed-time timer (visible) | UI (`GameViewModel`) | — | Wall-clock/coroutine concern; lives in app layer, feeds `ScoreCalculator` |
| Navigation between 5 screens | UI (`NavHost` in `MainActivity`) | — | Compose Navigation; the structural insertion point |
| Route arg → board lookup | UI (`GameViewModel` + `SavedStateHandle`) | Data (`LevelRepository`) | VM reads the typed route arg, asks the repo for the board |
| `onboardingSeen` persistence | Data (`OnboardingRepository`/DataStore) | UI (`HomeViewModel`) | DataStore is a local data source behind a repository; VM consumes the `Flow` |
| Board rendering (incl. mini-previews) | UI (`BoardCanvas`/`BoardGeometry`) | — | Stateless Canvas leaf; geometry generalizes per square footprint (per-board grid dimension only) |

## Standard Stack

### Core (new dependencies to add this phase)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.navigation:navigation-compose | **2.9.8** | Type-safe `NavHost` + the 5-destination graph | Official Compose navigation; type-safe routes stable since 2.8.0. **2.9.8 is the latest stable** (2026-04-22); 2.10.x is alpha and needs compileSdk 37 — do NOT bump `[VERIFIED: developer.android.com]` |
| org.jetbrains.kotlin.plugin.serialization | **2.3.20** (= Kotlin) | Marks `@Serializable` route classes | Type-safe Navigation requires kotlinx.serialization to encode route args. Plugin version tracks the Kotlin pin `[CITED: CLAUDE.md stack]` |
| org.jetbrains.kotlinx:kotlinx-serialization-json | **1.11.0** | Serialization runtime for route args (and the committed solution fixtures, optional) | Named in CLAUDE.md stack; required runtime for the serialization plugin `[CITED: CLAUDE.md stack]` |
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
| One-off authoring DFS solver (test-only) | Ship a runtime Solver now | A runtime Solver is Phase 4 and breaks the "no Solver this phase" scope; the authoring tool runs once and only commits a static fixture (D-19) |

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
   │  • 5 cards, ascending size (D-18)│                 │
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
    └── components/    # MODIFIED: BoardGeometry derives grid dimension per board (square only)

domain/src/main/kotlin/com/app/pegsolitaire/domain/
├── model/Boards.kt    # MODIFIED: + European37, Asymmetric39, Diamond41, Wiegleb45 (all square-grid)
└── scoring/           # NEW: ScoreCalculator.kt + ScoreParams.kt (constants) + ScoreResult.kt

domain/src/test/.../golden/solutions/   # NEW: committed winning move-list fixtures per board (D-19)
tools/ (or domain test source set)      # NEW: one-off authoring DFS/IDDFS solver — NOT shipped in :domain main
```

### Pattern 1: Type-safe Navigation graph
**What:** `@Serializable` route classes; `NavHost` with `composable<Route>`; navigate with instances; read args via `toRoute()`.
**When to use:** All five destinations.
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
    fun classicCatalog(): List<ClassicLevelDescriptor>   // CLAS-01, D-18 order (5 boards)
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
- **Starting European-37 or Diamond-41 with the center hole empty.** Both are provably unsolvable to one peg from center (see Board Footprints) — that board could never earn 3★ under D-01.
- **Shipping any search Solver in `:domain` this phase.** The solvability proof is a *committed static fixture* generated by a one-off authoring tool (D-19); the runtime Solver is Phase 4.
- **Bumping navigation-compose to 2.10.x.** Alpha + compileSdk 37 → breaks the environment's pinned compileSdk 36.
- **Making game-over→Results a replayable state that re-navigates on every recomposition.** Use a one-shot effect (Channel/SharedFlow) per compose-skill, or a `LaunchedEffect` keyed on the *transition* to terminal status.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Screen navigation + back stack | A manual `when(currentScreen)` state machine | navigation-compose `NavHost` | Back stack, args, lifecycle, deep-link-ready, type-safe |
| Passing typed args between screens | Manual `Bundle`/string parsing | `@Serializable` routes + `toRoute()` | Compile-time safety, no key typos |
| Persisting the onboarding flag | SharedPreferences / a file | datastore-preferences | Async Flow API; folds into Phase 5; SharedPreferences is discouraged (CLAUDE.md) |
| Board solvability validation (no Solver this phase) | A runtime search solver in `:domain` | **Recorded-solution golden test** — replay a committed winning move-list through `RulesEngine` to 1 peg | Reuses the existing `BoardCodec`/golden harness; mirrors the Phase-3 recorded-solution pattern; ships no Solver (D-19) |
| Producing each board's winning move-list | Hand-transcribing published solutions into our coords (error-prone) | A **one-off authoring DFS/IDDFS script** (test/tools source set) that emits the committed fixture once | Generates lines in our exact `(row,col)`/`Move` encoding; runs once; never shipped (see Code Examples) |
| Star/score logic | Inline `if` in the Results composable | `ScoreCalculator` pure object | Testable, board-agnostic, single source of the D-01/D-03 rules |
| Timer | A `Handler`/manual thread | `viewModelScope` + `delay` + injected `TimeSource` | Lifecycle-scoped, cancellable, deterministically testable |

**Key insight:** This phase adds almost no new *algorithms* — the domain core (bitmask `Board`, `RulesEngine`, golden harness) already generalizes to every square footprint. The risk is concentrated in (a) the board-solvability findings (two boards need off-center starts) and (b) not over-engineering the nav/DI plumbing. Crucially, **`RulesEngine` needs zero changes for all five boards**, and the only geometry change is per-board grid-dimension derivation — no lattice strategy (Triangle-15 is gone).

## Board Footprints — Concrete Definitions

All five fit the ≤64-hole `Long`-bitmask `BoardSpec` and are **square-grid, orthogonal-only** — `RulesEngine` is unchanged. Follow the existing `Boards.kt` pattern: a `private object XLayout` of scoped constants (QUAL-03) → derived `inPlayPositions` → a `BoardSpec` + initial `Board` in the `Boards` object. **No Solver this phase — lock each non-trivial board's start with a recorded-solution golden fixture (D-19; see Code Examples).**

**Catalog order (D-18, ascending hole count):** English-33 (33) → European-37 (37) → **Asymmetric 3-3-2-2 (39)** → Diamond-41 (41) → Wiegleb-45 (45).

| Board | Holes | Grid (rows×cols) | Generation rule (in-play iff) | Start vacancy | Center solvable? |
|-------|-------|------------------|-------------------------------|---------------|------------------|
| English-33 | 33 | 7×7 | `row in 2..4 \|\| col in 2..4` | center `(3,3)` | **Yes** (Phase-1, classic) |
| European-37 | 37 | 7×7 | `col in trim[row]..(6-trim[row])`, `trim=[2,1,0,0,0,1,2]` | **off-center** `(3,2)` | **No** — use orthogonal neighbour of center |
| Asymmetric 3-3-2-2 | 39 | 8×8 | `row in 3..5 \|\| col in 2..4` | center `(4,3)` | **Yes** (board is "completely solvable") |
| Diamond-41 | 41 | 9×9 | `col in (4-hw[row])..(4+hw[row])`, `hw=[0,1,2,3,4,3,2,1,0]` | **off-center** (see below) | **No** — center provably unsolvable |
| Wiegleb-45 | 45 | 9×9 | `row in 3..5 \|\| col in 3..5` | center `(4,4)` | **Yes** (22-move central game) |

### English-33 (reference only — already built in Phase 1)
7×7, cross, 33 holes, center-empty `(3,3)`. Do not redefine — `Boards.English33` / `english33Initial` exist. Listed here only for catalog ordering and the recorded-solution fixture (its winning line is the classic, e.g. Bergholt's 31-move solution `[CITED: well-known]`).

### European-37 (Continental / octagon) — `RulesEngine` unchanged
7×7 lattice with the four 2-cell corner steps trimmed; 37 holes. Per-row in-play column ranges (cols 0–6):
```
row 0: cols 2..4   (3)            . . O O O . .
row 1: cols 1..5   (5)            . O O O O O .
row 2: cols 0..6   (7)            O O O O O O O
row 3: cols 0..6   (7)            O O X O O O O   ← X = start vacancy (3,2)
row 4: cols 0..6   (7)            O O O O O O O
row 5: cols 1..5   (5)            . O O O O O .
row 6: cols 2..4   (3)            . . O O O . .
```
Generation rule: `cornerTrim = [2,1,0,0,0,1,2]`; in-play iff `col in trim[row]..(6 - trim[row])`. Total = 3+5+7+7+7+5+3 = **37**. `GRID_DIMENSION = 7`.

> **Center-empty `(3,3)` is provably unsolvable to one peg** `[VERIFIED: Wikipedia / gibell.net / durangobill.com Peg37]` (position-class parity). Durango Bill's exhaustive analysis marks **16 of the 37 holes as solvable single-vacancy starts**; the **four orthogonal neighbours of center — `(3,2)`, `(3,4)`, `(2,3)`, `(4,3)` — are all solvable** (single survivor, 20–21 moves). **Recommended start: `(3,2)`** (hole immediately left of center). The survivor will not land in the start hole, but **D-02 ignores the target → still 3★.** The exact winning line is committed as a fixture (generated, see below).

### Asymmetric 3-3-2-2 (George Bell) — **NEW footprint**, `RulesEngine` unchanged
An English-style 3-wide cross whose four arms have lengths **3, 3, 2, 2 read cyclically** (up=3, right=3, down=2, left=2) — i.e. two *adjacent* long arms, giving **diagonal symmetry** (not 4-fold). 8×8 bounding box; **39 holes** `[VERIFIED: gibell.net GenCross — "(3,3,2,2)… 39-hole… completely solvable"]`.
Central 3×3 block at rows 3..5 × cols 2..4. Per-row in-play column ranges (cols 0–7):
```
row 0: cols 2..4   (3)            . . O O O . . .
row 1: cols 2..4   (3)            . . O O O . . .
row 2: cols 2..4   (3)            . . O O O . . .
row 3: cols 0..7   (8)            O O O O O O O O
row 4: cols 0..7   (8)            O O O X O O O O   ← X = center start (4,3)
row 5: cols 0..7   (8)            O O O O O O O O
row 6: cols 2..4   (3)            . . O O O . . .
row 7: cols 2..4   (3)            . . O O O . . .
```
Generation rule: in-play iff `row in 3..5 || col in 2..4`, over rows 0..7, cols 0..7. Total = 3+3+3+8+8+8+3+3 = **39**. Arms: up (rows 0–2) length 3, down (rows 6–7) length 2, left (cols 0–1) length 2, right (cols 5–7) length 3 → cyclic (3,3,2,2). `GRID_DIMENSION = 8` (square bounding box; the footprint is genuinely asymmetric within it — that is the board). Center hole = `(4,3)`. **Center-empty IS solvable** — Bell lists (3,3,2,2) among the boards "completely solvable" at every vacancy. **Catalog slot: index 2 (third), between European-37 and Diamond-41.**

### Diamond-41 — **NEW finding: center is UNSOLVABLE**, `RulesEngine` unchanged
9×9 lattice, square rotated 45°; 41 holes. Per-row in-play column ranges (cols 0–8):
```
row 0: col 4          (1)         . . . . O . . . .
row 1: cols 3..5      (3)         . . . O O O . . .
row 2: cols 2..6      (5)         . . O O O O O . .
row 3: cols 1..7      (7)         . O O O O O O O .
row 4: cols 0..8      (9)         O O O O O O O O O
row 5: cols 1..7      (7)         . O O O O O O O .
row 6: cols 2..6      (5)         . . O O O O O . .
row 7: cols 3..5      (3)         . . . O O O . . .
row 8: col 4          (1)         . . . . O . . . .
```
Generation rule: `halfWidth = [0,1,2,3,4,3,2,1,0]`; in-play iff `col in (4 - hw[row])..(4 + hw[row])`. Total = 1+3+5+7+9+7+5+3+1 = **41**. `GRID_DIMENSION = 9`.

> **CORRECTION to prior research (was A2 "center-empty `(4,4)` is solvable").** The 41-hole diamond is highly constrained: only **4** single-vacancy-to-single-survivor problems are solvable on it (each 26 moves), and **the center is NOT one of them** `[VERIFIED: gibell.net (George Bell) French/diamond page; corroborated by Bell-Beasley "New problems on old solitaire boards"]`. The solvable starts lie off-center (Bell's catalog: "vacate f8 or c5, finish at d8 or g5" and equivalents). **Diamond-41 must use an off-center start.** Mapping Bell's algebraic labels into our `(row,col)` gives candidate solvable vacancies such as `(4,2)` (two cells left of center on the middle row) or `(1,5)` — **the exact start is selected and validated by the authoring solver** (it enumerates the in-play holes and keeps the first vacancy that reduces to one peg). Center `(4,4)` is excluded. `[ASSUMED: specific (row,col); the *fact that center fails and an off-center start exists* is VERIFIED]`

### Wiegleb-45 (German cross, J.C. Wiegleb 1779) — **NEW footprint**, `RulesEngine` unchanged
The English cross with every arm one cell longer: a 3-wide cross on a 9×9 grid (equivalently, 9×9 minus the four 3×3 corner blocks). 45 holes. Per-row in-play column ranges (cols 0–8):
```
row 0: cols 3..5   (3)            . . . O O O . . .
row 1: cols 3..5   (3)            . . . O O O . . .
row 2: cols 3..5   (3)            . . . O O O . . .
row 3: cols 0..8   (9)            O O O O O O O O O
row 4: cols 0..8   (9)            O O O O X O O O O   ← X = center start (4,4)
row 5: cols 0..8   (9)            O O O O O O O O O
row 6: cols 3..5   (3)            . . . O O O . . .
row 7: cols 3..5   (3)            . . . O O O . . .
row 8: cols 3..5   (3)            . . . O O O . . .
```
Generation rule: `centralBand = 3..5`; in-play iff `row in 3..5 || col in 3..5`, over a 9×9 grid (identical shape to English-33's rule, scaled from 7×7 to 9×9). Total = 3+3+3+9+9+9+3+3+3 = **45**. `GRID_DIMENSION = 9`. Center hole = `(4,4)`. **Center-empty IS solvable** — the Wiegleb central game (start center-empty, finish one peg) is solvable in **22 moves, no fewer** `[VERIFIED: gibell.net — "central game can be solved in 22 moves, but no fewer"]`. (Of its 36 SVSS problems, all are solvable except the `(4,0)`/e1-corner complement — center is fine.)

## Board Geometry Generalization (square grid only — no lattice strategy)

The earlier "`BoardLattice` (Square/Triangle) strategy" recommendation is **DROPPED**: with Triangle-15 gone (D-17), every board is a square lattice and `BoardGeometry`/`BoardMetrics` need exactly one generalization — **derive the grid dimension from the `BoardSpec` instead of the hardcoded English-33 constant:**

1. **Derive grid extent from the spec**, not a constant: `rows = spec.inPlay.maxOf { it.row } + 1`, `cols = spec.inPlay.maxOf { it.col } + 1`; use `gridDimension = max(rows, cols)` for `cellSize`. Removes `BoardMetrics.GRID_DIMENSION = 7`. (English/European = 7, Asymmetric = 8, Diamond/Wiegleb = 9.)
2. **`center(pos) = origin + (col + 0.5, row + 0.5) * cell`** works unchanged once the dimension is derived. `positionAt(offset)` inverts it and bounds-checks against `spec` (the T-04-01 contract is preserved for every footprint).
3. **Asymmetric-39 note:** its 8×8 bounding box contains a footprint that is *not* centered (the arms differ) — render the bounding box as-is; the off-center shape is the board's identity. The catalog mini-preview (D-11) renders the same way at small size and doubles as a free visual smoke test that each `BoardSpec` is laid out correctly.
4. Add/extend a per-board JVM `BoardGeometryTest` asserting in-bounds mapping for dimensions 7, 8, 9.

## Common Pitfalls

### Pitfall 1: English-33-specific geometry breaks the new boards
**What goes wrong:** `BoardMetrics.GRID_DIMENSION = 7` and `BOARD_ASPECT_RATIO = 1f` are baked for English-33; European (7) happens to match but Asymmetric (8), Diamond (9), and Wiegleb (9) do not — pegs render off-grid or clipped.
**Why:** Phase-1 geometry was scoped to one board.
**How to avoid:** Derive grid dimension from the `BoardSpec` bounds; pass it into `BoardGeometry.forCanvas`. Add per-board `BoardGeometryTest` cases (dimensions 7/8/9).
**Warning signs:** Catalog previews look fine for English/European but Asymmetric/Diamond/Wiegleb are misaligned.

### Pitfall 2: Center-start European-37 / Diamond-41 can never reach 3★
**What goes wrong:** Player perceives the board as broken/unfair — a "perfect" solve is mathematically impossible.
**Why:** Position-class parity — center-complement is unsolvable on **both** the 37-hole European board and the 41-hole diamond `[VERIFIED]`.
**How to avoid:** Use a documented solvable off-center start (European `(3,2)`; Diamond an off-center hole validated by the authoring solver); lock it with a recorded-solution golden fixture. The other three boards (English, Asymmetric, Wiegleb) are fine from center.
**Warning signs:** Best achievable on those boards is always ≥2 pegs in playtesting.

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
**How to avoid:** `ClassicLevelSource.load` validates the id against the known catalog and fails fast in tests; the catalog only ever emits valid ids. Keep ids as named constants (QUAL-03), not string literals at call sites.

### Pitfall 6: kotlinx-serialization plugin not applied → `@Serializable` routes fail at runtime
**What goes wrong:** Type-safe routes compile but crash with a serializer-not-found at navigate time.
**Why:** The serialization Gradle plugin must be applied to `:app` (it generates serializers); adding only the json runtime is insufficient.
**How to avoid:** Apply `org.jetbrains.kotlin.plugin.serialization` in `:app` AND add `kotlinx-serialization-json`. Verify with one navigation in an instrumentation/Robolectric test.

### Pitfall 7: Transcribing a published solution into the wrong coordinate system
**What goes wrong:** A hand-copied move-list from a paper (algebraic labels, a flipped y-axis, a different vacancy) fails to replay, or "passes" against a board you laid out differently — silently validating nothing.
**Why:** Literature uses many coordinate conventions; our `BoardSpec` indexes `(row, col)` sorted, row 0 at top.
**How to avoid:** **Generate** each winning line with the one-off authoring solver against *our actual `Boards.*` value*, so the fixture is in our `Move` encoding by construction. Don't hand-transcribe (see Code Examples).

## Code Examples

### Recorded-solution golden test (solvability without a runtime Solver — D-19)
```kotlin
// domain/src/test/.../golden — reuses existing BoardCodec/GoldenMasterAssert
@Test fun `european37 recorded solution reduces to one peg`() {
    val solution: List<Move> = European37Solution.moves   // committed fixture (generated once)
    val finalBoard = solution.fold(Boards.european37Initial) { b, m ->
        require(m in RulesEngine.legalMoves(b)); RulesEngine.applyMove(b, m)
    }
    assertThat(finalBoard.pegCount).isEqualTo(1)          // proves 3★ is reachable (D-01)
}
```
Repeat per board; the committed move-list IS the proof of solvability and forward-feeds Phase 3's recorded-solution contract.

### One-off authoring solver (NOT shipped in `:domain` main)
```kotlin
// tools/ or domain/src/test — run ONCE to emit each *Solution fixture, then delete/ignore.
// Plain DFS with pagoda/visited pruning; 33–45 holes solve in well under a minute.
fun solveToOnePeg(start: Board): List<Move>? {
    val seen = HashSet<Long>()
    fun dfs(board: Board, path: ArrayDeque<Move>): List<Move>? {
        if (board.pegCount == 1) return path.toList()
        if (!seen.add(board.bits)) return null
        for (m in RulesEngine.legalMoves(board)) {        // D-02: any final peg counts
            path.addLast(m)
            dfs(RulesEngine.applyMove(board, m), path)?.let { return it }
            path.removeLast()
        }
        return null
    }
    return dfs(start, ArrayDeque())
}
// Authoring step: for each Boards.* board (and, for European/Diamond, each candidate off-center
// vacancy) run solveToOnePeg, then serialize the returned List<Move> to a committed fixture
// (kotlinx.serialization JSON or the existing BoardCodec text format) under
// domain/src/test/resources/golden/solutions/. The shipped golden test only REPLAYS the fixture.
```
**Why this satisfies D-19 without a Solver:** the search code lives in a test/tools source set, runs at authoring time, and produces a *static committed artifact*. Nothing in `:domain` main (or any shipped APK code path) performs a search — the runtime Solver remains a Phase-4 concern. For English-33, optionally cross-check the generated line against the published Bergholt solution.

### ResultsViewModel reading typed args + computing "Next board" (D-06)
```kotlin
// Source: developer.android.com type-safety + LevelRepository (this research)
@HiltViewModel
class ResultsViewModel @Inject constructor(
    levelRepository: LevelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args = savedStateHandle.toRoute<Results>()
    private val order = levelRepository.classicCatalog().map { it.id }   // D-18 ascending (5 ids)
    val nextLevelId: String = order[(order.indexOf(args.levelId) + 1) % order.size]  // wraps (D-06)
    val uiState = ResultsUiState(
        stars = args.stars, pegsRemaining = args.pegsRemaining,
        elapsedSeconds = args.elapsedSeconds, score = args.score,
        replayId = args.levelId, nextId = nextLevelId,
    )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| String-route Navigation DSL (`"game/{id}"`) | `@Serializable` type-safe routes + `toRoute()` | Navigation 2.8.0 (2024) | Compile-time arg safety; use it |
| SharedPreferences | DataStore Preferences (Flow-based) | Ongoing; ESP deprecated | `onboardingSeen` uses DataStore (D-16, CLAUDE.md) |
| `SavedStateHandle.get<>("key")` | `savedStateHandle.toRoute<Route>()` | Navigation 2.8.0 | No key strings; the VM reconstructs its typed route |

**Deprecated/outdated:**
- navigation-compose 2.10.x line — alpha, requires compileSdk 37; **not for this environment**.
- `GameOverDialog`/`GameOverOverlay` (Phase 1) — replaced by the Results destination (D-05).
- The Phase-2-research `BoardLattice` (Square/Triangle) proposal — **obsolete** now Triangle-15 is dropped (D-17); square-grid geometry only.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | European-37 start `(3,2)` (an orthogonal neighbour of center) is solvable to one peg | Board Footprints | Low — Durango Bill's exhaustive W/L map marks all four center-neighbours solvable `[VERIFIED]`; the authoring solver confirms the exact line. If it somehow failed, pick another of the 16 W holes |
| A2 | A specific off-center Diamond-41 vacancy (candidate `(4,2)`/`(1,5)`) is solvable | Board Footprints | The *existence* of a solvable off-center start is VERIFIED (4 SVSS problems, 26 moves); the exact `(row,col)` is solver-validated at authoring time. Risk only if the coordinate mapping guess is wrong — the solver enumerates and self-corrects |
| A3 | `PEG_VALUE=100`, `TIME_BONUS_CAP=5000`, `DECAY=10` are reasonable D-03 constants | ScoreCalculator | Wrong feel only; tunable, low risk; planner sanity-checks |
| A4 | navigation-compose 2.9.8 resolves without dragging in a compileSdk-37 transitive line | Standard Stack / Pitfall 3 | Build won't install; mitigated by `resolutionStrategy` guard (already a project pattern) |
| A5 | The authoring DFS solves all five boards (≤45 holes) to one peg within a practical authoring-time budget | Code Examples / D-19 | Low — plain DFS with a visited-set handles ≤45-hole boards in seconds–minutes; add pagoda pruning if needed |

**These assumptions need confirmation before they become locked decisions.** A1/A2/A5 are validated by committing the recorded-solution golden fixtures (no runtime Solver). A4 is validated by a dependency-resolution dry run. The center-unsolvability of European-37 and Diamond-41, and the center-solvability of Wiegleb-45/Asymmetric-39, are **VERIFIED facts** (not assumptions).

## Open Questions

1. **[RESOLVED] Triangle-15 vs. the locked orthogonal-only rule.**
   The earlier BLOCKING conflict (authentic triangular solitaire needs 3-axis/diagonal jumps `RulesEngine` cannot express) was escalated and **resolved by the user (CONTEXT D-17/D-18): Triangle-15 is dropped**, replaced by a 5-board all-orthogonal-square-grid catalog. No triangular-lattice geometry is built; `RulesEngine` and the orthogonal-only rule are unchanged. **No further action.**

2. **[RESOLVED] European-37 starting vacancy.**
   Center is unsolvable `[VERIFIED]`; the four orthogonal neighbours of center are solvable single-vacancy starts. **Locked recommendation: `(3,2)`**, proven by a committed recorded-solution fixture (generated). The exact winning line comes from the authoring solver — no literature transcription needed.

3. **Diamond-41 exact off-center start (`(row,col)`).**
   - What we know: center is unsolvable; only 4 SVSS problems solvable (26 moves) `[VERIFIED: gibell.net/Bell-Beasley]`; solvable starts are off-center (Bell's "f8/c5/d8/g5").
   - What's unclear: the precise `(row,col)` in our top-row-0 convention.
   - Recommendation: let the authoring solver enumerate in-play holes and select the first that reduces to one peg; commit that vacancy + its line as the fixture. Candidates to try first: `(4,2)`, `(1,5)` and their symmetric images.

4. **Game-end → Results transition mechanism.**
   - What we know: Phase 1 made terminal status replayable state; navigation needs a one-shot.
   - Recommendation: One-shot effect channel per compose-skill (preferred) or a guarded `LaunchedEffect(status)`. Planner picks; both documented above.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| navigation-compose | NAV-01/02 nav graph | Add to catalog | 2.9.8 (stable) | none — required |
| kotlinx-serialization plugin + json | Type-safe route args | Add to catalog | 2.3.20 / 1.11.0 | String-route DSL (no serialization), not recommended |
| datastore-preferences | ONB-01 `onboardingSeen` | Add to catalog | 1.2.1 (stable) | none needed |
| compileSdk 37 | navigation-compose **2.10.x only** | ✗ (not installable) | — | **Stay on 2.9.8** — avoids the requirement entirely |
| Runtime Solver (search) | Solvability validation | ✗ (Phase 4) | — | **Authoring-time DFS → committed recorded-solution fixtures** (no shipped search) |

**Missing dependencies with no fallback:** none (all three additions are first-party and current).
**Missing with fallback:** runtime Solver → authoring-time DFS + committed golden fixtures; compileSdk 37 → pin navigation 2.9.8.
**Action:** Run a `:app` dependency-resolution dry run after adding navigation to confirm no compileSdk-37 transitive line appears.

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
| CLAS-02 | each non-trivial board's recorded solution → 1 peg (incl. off-center European/Diamond starts) | unit (golden) | `./gradlew :domain:test --tests *SolutionTest*` | ❌ Wave 0 |
| CLAS-01 | catalog returns 5 descriptors in D-18 order | unit | `./gradlew :app:testDebugUnitTest --tests *LevelRepositoryTest*` | ❌ Wave 0 |
| CLAS-02 | new-board geometry maps in bounds (dims 7/8/9) | unit | `./gradlew :app:testDebugUnitTest --tests *BoardGeometryTest*` | ⚠️ extend existing |
| NAV-02 | Home→Catalog→Gameplay→Results reachable | UI (Robolectric) | `./gradlew :app:testDebugUnitTest --tests *NavFlowTest*` | ❌ Wave 0 |
| SCOR-03 | Results shows stars/pegs/time/score | UI | `./gradlew :app:testDebugUnitTest --tests *ResultsScreenTest*` | ❌ Wave 0 |
| ONB-01 | first launch shows overlay; markSeen hides it | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests *HomeViewModelTest*` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :domain:test` (fast pure-domain feedback)
- **Per wave merge:** `./gradlew :domain:test :app:testDebugUnitTest`
- **Phase gate:** full suite + `ktlintCheck detekt` + ArchUnit domain-purity green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `domain/.../scoring/ScoreCalculatorTest.kt` — SCOR-01/02 bands + formula + decay floor
- [ ] `domain/.../golden/*SolutionTest.kt` — recorded winning line per non-trivial board → 1 peg (European-37, Asymmetric-39, Diamond-41, Wiegleb-45; English-33 too). Doubles as the solvability proof (D-19)
- [ ] `domain/src/test/resources/golden/solutions/*` — committed move-list fixtures (emitted once by the authoring solver)
- [ ] One-off authoring solver (tools/ or test source set) — generates the fixtures; **must not appear in `:domain` main** (ArchUnit boundary)
- [ ] `app/.../data/LevelRepositoryTest.kt` — catalog order (D-18, 5 boards) + `loadClassic` round-trip
- [ ] `app/.../ui/components/BoardGeometryTest.kt` — extend for dims 8 (Asymmetric) and 9 (Diamond/Wiegleb)
- [ ] `app/.../navigation/NavFlowTest.kt` — Robolectric end-to-end across the 5 destinations
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
- en.wikipedia.org/wiki/Peg_solitaire — board variant list (English-33, European-37, Wiegleb-45, Diamond-41, Asymmetric 3-3-2-2); European center unsolvable, "3 non-congruent solvable positions"
- gibell.net/pegsolitaire/ (George I. Bell) — French/European board: 10 solvable SVSS (20–21 moves), center unsolvable; **Diamond-41: only 4 SVSS solvable (26 moves), center NOT solvable**; **Wiegleb-45: central game solvable in 22 moves, all 36 SVSS solvable except (4,0)/e1**
- gibell.net/pegsolitaire/GenCross/GenCrossBoards0.html — generalized cross boards: **Wiegleb (3,3,3,3)=45 holes; Asymmetric (3,3,2,2)=39 holes, "completely solvable" at every vacancy**; only arms ≤3 can be completely solvable
- durangobill.com/Peg37.html — exhaustive 37-hole French board analysis: W/L map of solvable starts (16 of 37 solvable; the four center-neighbours are W; complement-in-start-hole never solvable)
- arxiv.org/abs/math/0611091 — Bell & Beasley, "New problems on old solitaire boards" (corroborates diamond/Wiegleb results; PDF text not machine-extractable this session, cross-checked via the gibell.net summaries)
- Existing codebase: `BoardSpec.kt`/`Board.kt`/`Boards.kt`/`RulesEngine`/`BoardGeometry`/`GameViewModel`/`DomainModule`/`libs.versions.toml` (read this session)
- CLAUDE.md — pinned stack (navigation 2.9.8, datastore 1.2.1, kotlinx-serialization 1.11.0, Kotlin 2.3.20)

### Secondary (MEDIUM confidence)
- Per-row footprints derived this session from the cross/diamond/octagon definitions (English 7×7 cross; European octagon 3-5-7-7-7-5-3; Diamond 1-3-5-7-9-7-5-3-1; Wiegleb 9×9 cross 3/9/3; Asymmetric 8×8 cross arms 3,3,2,2) — coordinates computed and hole-counts re-verified (37/39/41/45)

### Tertiary (LOW confidence — flagged for validation)
- The exact `(row,col)` of the Diamond-41 off-center start — `[ASSUMED]`, selected and validated by the authoring solver
- ScoreCalculator constant values — `[ASSUMED]` sane defaults, planner to sanity-check

## Metadata

**Confidence breakdown:**
- Navigation / DataStore / serialization stack: HIGH — verified versions + compileSdk constraint this session
- LevelRepository seam + selected-board flow: HIGH — standard Repository + SavedStateHandle pattern over an existing clean seam
- ScoreCalculator placement + formula: HIGH (shape locked by D-01/D-03); constants MEDIUM (tunable)
- Board footprints (all 5, incl. new Wiegleb-45 & Asymmetric-39): HIGH — coordinates derived and hole-counts cross-verified against George Bell's generalized-cross analysis
- Board solvability (center un/solvability per board): HIGH — VERIFIED against gibell.net / Bell-Beasley / Durango Bill (European & Diamond centers unsolvable; English/Wiegleb/Asymmetric centers solvable)
- Exact off-center Diamond-41 vacancy: MEDIUM — existence proven; precise coordinate is solver-validated at authoring time
- D-19 golden-test approach (authoring DFS → committed fixtures): HIGH — standard, buildable, ships no Solver
- Pitfalls: HIGH — most grounded in the project's own compileSdk-36 history and verified solvability facts

**Research date:** 2026-06-28 (board follow-up revision)
**Valid until:** ~2026-07-28 (stack stable; the combinatorial board-solvability facts do not expire — re-verify navigation/datastore versions only if planning slips a month)
</content>
</invoke>
