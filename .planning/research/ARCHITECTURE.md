# Architecture Research

**Domain:** Native Android single-player puzzle game (peg solitaire) — Clean Architecture (UI/MVVM · Domain · Data) + Repository pattern
**Researched:** 2026-06-28
**Confidence:** HIGH

> Architecture is already decided (PROJECT.md, design spec §8, official Android guidance). This document is **not** a survey of alternatives — it specifies concrete component boundaries, data-flow direction, the reverse-generation generator design, and a dependency-ordered vertical-slice build sequence for the roadmap.

---

## Standard Architecture

### System Overview

Strict unidirectional dependency direction: **UI → Domain → Data**. Lower layers never import upper layers. State flows **down** (data → repository → use case → ViewModel `UiState` → composable); events flow **up** (composable → ViewModel → use case → repository). Platform SDKs sit behind interfaces owned by Domain, implemented at the edge.

```
┌──────────────────────────────────────────────────────────────────────┐
│  UI LAYER  (Jetpack Compose + Material 3, MVVM)                        │
│  ┌────────┐ ┌─────────┐ ┌───────────┐ ┌──────────┐ ┌─────────┐        │
│  │ Home   │ │ Classic │ │ Challenge │ │ Gameplay │ │ Results │  Routes│
│  │ Screen │ │ Picker  │ │  Screen   │ │  Screen  │ │ Screen  │        │
│  └───┬────┘ └────┬────┘ └─────┬─────┘ └────┬─────┘ └────┬────┘        │
│      │   collectAsStateWithLifecycle(UiState)  │ effects (Channel)     │
│  ┌───┴──────────┴─────────────┴────────────────┴───────────┴───────┐  │
│  │  ViewModels  (StateFlow<UiState> down · onEvent()/fns up)        │  │
│  │  GameViewModel · ClassicPickerVM · ChallengeVM · ResultsVM       │  │
│  │  Gameplay rendered on Compose Canvas (BoardCanvas + Animatable)  │  │
│  └───────────────────────────────┬─────────────────────────────────┘  │
├──────────────────────────────────┼─────────────────────────────────────┤
│  DOMAIN LAYER  (pure Kotlin · zero Android imports · commonTest-able)  │
│  ┌──────────────────────────────┐ ┌──────────────────────────────────┐│
│  │ Use Cases (orchestration)    │ │ Models  Board·Cell·Position·     ││
│  │ LoadClassicLevel·Generate-   │ │         Move·GameState·LevelResult││
│  │ ChallengeLevel·ApplyMove·    │ └──────────────────────────────────┘│
│  │ GetHint·CalculateResult·     │ ┌──────────┐┌──────────┐┌──────────┐│
│  │ SaveProgress·ResumeGame      │ │RulesEngine││LevelGen ││  Solver  ││
│  └──────────────┬───────────────┘ └──────────┘└──────────┘└──────────┘│
│   Repository INTERFACES + platform INTERFACES live here │ScoreCalc     │
│   LevelRepository·ProgressRepository·SettingsRepository │AdGateway·    │
│   (dependency inversion — domain declares, data/platform implement)    │GamesService
├──────────────────────────────────┼─────────────────────────────────────┤
│  DATA LAYER  (Repository = single source of truth)     ▼                │
│  ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────┐│
│  │ LevelRepositoryImpl   │ │ ProgressRepositoryImpl│ │SettingsRepoImpl ││
│  │  ├ ClassicLevelSource │ │  └ Room (DAO/entities)│ │ └ DataStore     ││
│  │  │   (static catalog) │ │                       │ │                 ││
│  │  └ ChallengeLevelSource│ │  (Slice 2: + Cloud   │ │                 ││
│  │     (LevelGenerator)  │ │   Saved-Games source) │ │                 ││
│  └──────────────────────┘ └──────────────────────┘ └──────────────────┘│
├────────────────────────────────────────────────────────────────────────┤
│  PLATFORM EDGE  (di-bound impls)  Slice 1 = no-op stubs                 │
│  NoOpAdGateway (auto-grant) · NoOpGamesService     Slice 2 = AdMob/PGS  │
└────────────────────────────────────────────────────────────────────────┘
        DI: Hilt modules wire interfaces → impls at the app boundary
```

### Component Responsibilities

| Component | Responsibility (owns) | Implementation |
|-----------|-----------------------|----------------|
| **Board / Cell / Position** | Immutable grid model. `Cell` ∈ {Blocked, Empty, Peg}; `Position(row,col)`; `Board` = footprint + cell map + optional target hole | `data class`, immutable; grid as `Map<Position,Cell>` or flat `Array` + bounds |
| **Move** | One jump: `from`, `over`, `to` (colinear, spacing 2) | `data class Move(from, over, to)` |
| **GameState** | Full game snapshot: current `Board`, `moveHistory: List<Move>`, `elapsedMs`, status (InProgress/Won/Stuck) | immutable `data class`; new instance per move |
| **RulesEngine** | Pure rules: `legalMoves(board)`, `apply(board, move)`, `isGameOver(board)`, `pegCount(board)`. No state, no I/O | stateless object / pure functions |
| **LevelGenerator** | Seeded **reverse-generation** → solvable starting board + recorded winning solution (see §Generator) | pure Kotlin, deterministic PRNG |
| **Solver** | DFS search to reduce a board to 1 peg; validates classic boards (tests) and supplies fallback hints when player deviates from canonical line | pure Kotlin, transposition table |
| **ScoreCalculator** | `stars(pegsRemaining)` (1→★★★, 2→★★, 3→★, 4+→0) and `score(pegsRemaining, timeMs)` | pure functions |
| **Use cases** | Thin orchestration across repos/engine. Only where multi-step (per official guidance — don't wrap single repo calls) | `class …UseCase { operator fun invoke() }` |
| **LevelRepository** | SSOT for "give me board for level X". Hides whether it's a static classic board or a generated challenge board | interface in domain, impl in data |
| **ProgressRepository** | SSOT for per-level results, highest level, in-progress resume snapshot | Room-backed impl |
| **SettingsRepository** | SSOT for sound/haptics/prefs | DataStore-backed impl |
| **ViewModels** | Own `StateFlow<UiState>`; map domain → UI state; turn events into use-case calls; emit one-off `Effect`s | `ViewModel` + `viewModelScope` |
| **BoardCanvas** | Render board + animate jump/removal from a declarative transition description. No game logic | Compose `Canvas` + `Animatable` |
| **AdGateway / GamesService** | Platform boundary interfaces. Slice 1 stubs (auto-grant / no-op) | interface in domain, impl in platform |

---

## Recommended Project Structure

```
com.app.pegsolitaire
├─ domain/                       # pure Kotlin, no Android imports
│  ├─ model/                     # Board, Cell, Position, Move, GameState, LevelResult, BoardSpec
│  ├─ rules/                     # RulesEngine
│  ├─ generator/                 # LevelGenerator, ReverseMove, DifficultyTier, Prng
│  ├─ solver/                    # Solver, transposition table
│  ├─ scoring/                   # ScoreCalculator
│  ├─ repository/                # LevelRepository, ProgressRepository, SettingsRepository (INTERFACES)
│  ├─ platform/                  # AdGateway, GamesService (INTERFACES)
│  └─ usecase/                   # LoadClassicLevel, GenerateChallengeLevel, ApplyMove,
│                                #   GetHint, Undo, CalculateResult, SaveProgress, ResumeGame
├─ data/
│  ├─ repository/                # LevelRepositoryImpl, ProgressRepositoryImpl, SettingsRepositoryImpl
│  ├─ source/
│  │  ├─ classic/                # ClassicLevelSource + static board definitions (English-33 …)
│  │  └─ challenge/              # ChallengeLevelSource (wraps LevelGenerator + seed mapping)
│  ├─ local/
│  │  ├─ room/                   # DB, DAOs, entities (LevelResultEntity, InProgressGameEntity)
│  │  └─ datastore/              # SettingsDataStore
│  └─ mapper/                    # entity ↔ domain mappers
├─ ui/
│  ├─ home/  classic/  challenge/  game/  results/   # each: Route + Screen + ViewModel + Contract
│  ├─ components/                # BoardCanvas, PegRenderer, StarBar, reusable leaves
│  ├─ navigation/                # NavHost / routes
│  └─ theme/                     # Material 3 theme (single theme for now)
├─ platform/
│  ├─ ads/                       # NoOpAdGateway (Slice 1) → AdMobAdGateway (Slice 2)
│  └─ games/                     # NoOpGamesService (Slice 1) → PlayGamesService (Slice 2)
└─ di/                           # Hilt modules: DomainModule, DataModule, PlatformModule
```

### Structure Rationale

- **`domain/` has no Android dependency** so `RulesEngine`, `LevelGenerator`, `Solver`, `ScoreCalculator` run in plain JVM unit tests with no emulator (the highest-value test target and the project's core risk — solvability).
- **Repository + platform interfaces live in `domain/`, impls in `data/` and `platform/`** — dependency inversion. Domain never imports Room, DataStore, AdMob, or Play Games.
- **`LevelGenerator` and `Solver` are domain, not data.** They are pure algorithms. The *data* layer (`ChallengeLevelSource`) only maps level-number → seed and calls the generator; classic boards come from a static source. **Both sources sit behind the single `LevelRepository`** so the UI/use cases never branch on "classic vs challenge" at the repository boundary.
- **Each UI feature folder is self-contained** (Route/Screen/ViewModel/Contract) per the compose-skill's slicing model — collect whole `UiState` at the Route, slice downward to leaves.

---

## Architectural Patterns

### Pattern 1: Unidirectional Data Flow with immutable `UiState` (MVVM)

**What:** Each screen exposes one immutable `StateFlow<UiState>`. UI renders state (down); user actions become ViewModel function calls or `onEvent()` (up); one-off effects (navigate, snackbar, haptic) go through a `Channel<Effect>` — never modeled as state.

**When:** Every screen. Gameplay is the demanding case.

**Trade-offs:** Slight boilerplate (contract types) bought back as testability and recomposition correctness. The compose-skill notes MVI vs MVVM is a UI-to-VM contract choice only; project preference is **MVVM naming** (named functions), UDF underneath either way.

```kotlin
data class GameUiState(
    val board: Board,                        // canonical source of truth
    val selected: Position? = null,
    val legalTargets: Set<Position> = emptySet(),
    val animating: MoveTransition? = null,   // what BoardCanvas should animate
    val status: GameStatus = GameStatus.InProgress,
    val elapsedMs: Long = 0,
    val canUndo: Boolean = false,
    val hint: Move? = null,
) {
    val pegsRemaining: Int get() = board.pegCount   // derive, don't duplicate
}

sealed interface GameEffect {
    data class NavigateToResults(val result: LevelResult) : GameEffect
    data object NoWinningMove : GameEffect
}
```

### Pattern 2: One Repository over two data sources (classic vs generated)

**What:** `LevelRepository.getLevel(ref: LevelRef): Board + Solution`. `LevelRef` is `Classic(boardId)` or `Challenge(levelNumber)`. The impl routes to `ClassicLevelSource` (static catalog) or `ChallengeLevelSource` (generator). Callers never know which.

**When:** All level loading.

**Trade-offs:** Single seam makes adding daily-challenge/cloud sources later trivial; the only cost is one routing `when`.

```kotlin
interface LevelRepository {
    suspend fun getLevel(ref: LevelRef): LoadedLevel   // board + recorded solution (null for classic until solved)
}
class LevelRepositoryImpl(
    private val classic: ClassicLevelSource,
    private val challenge: ChallengeLevelSource,
) : LevelRepository {
    override suspend fun getLevel(ref: LevelRef) = when (ref) {
        is LevelRef.Classic   -> classic.load(ref.boardId)
        is LevelRef.Challenge -> challenge.generate(ref.levelNumber)
    }
}
```

### Pattern 3: Reverse-generation level generator (the core mechanism)

**What:** Generate a guaranteed-solvable board by playing the game **backwards** from the solved state, recording the sequence; the forward solution is that sequence reversed.

**Inverse-move definition.** A forward jump moves peg `from`→`to` over `over` (3 colinear cells, spacing 2), removing `over`. Its inverse ("un-jump"), given a peg at `C`: require `C = Peg`, `B = Empty`, `A = Empty` (all in footprint, colinear, spacing 2). Apply: `C → Empty`, `B → Peg`, `A → Peg`. **Net +1 peg per reverse move.**

**Algorithm:**
```
fun generate(levelNumber): LoadedLevel
  seed     = stableHash(levelNumber)              // stable, platform-independent
  rng      = Prng(seed)                            // deterministic (kotlin.random.Random(seed))
  tier     = difficultyTier(levelNumber)           // footprint + target solution length
  footprint = pickFootprint(tier, rng)             // cross/diamond/rect/triangle/irregular
  target    = pickTargetHole(footprint, tier, rng) // where the final single peg sits
  board     = footprint.allEmpty().withPeg(target)
  reverseMoves = []
  repeat until reverseMoves.size == tier.solutionLength (or no legal reverse move):
      candidates = allLegalReverseMoves(board)      // via RulesEngine inverse
      if candidates.isEmpty: break                  // dead-end guard → retry/shorten
      m = candidates[rng.nextInt(candidates.size)]
      board = applyReverse(board, m); reverseMoves += m
  solution  = reverseMoves.reversed().map { it.toForwardMove() }
  return LoadedLevel(startingBoard = board, solution = solution)
```

**Guarantees & guards:**
- **Solvable by construction** — `solution` reduces `startingBoard` to one peg.
- **Deterministic** — same `levelNumber` → same seed → same board (retry/resume/cross-device parity).
- **Difficulty scaling** — `tier` raises footprint size/shape variety, peg density, and solution length with `levelNumber`; the starting empty-pattern varies (not just one center gap).
- **Degenerate-board guards (validate in tests):** reject boards with a trivial solution (length < tier min), boards that are nearly full (too few empties → no real puzzle), and dead-end walks (no legal reverse move before reaching target length → re-roll with a salted seed or reduce target length deterministically).

**When:** Every Challenge level. **Trade-off:** reverse-walk randomness can occasionally hit dead-ends or trivial boards; handled by deterministic re-roll + test-time validation rather than online search.

### Pattern 4: Solver (validation + fallback hints)

**What:** DFS over `RulesEngine.legalMoves`, reducing the board toward one peg, with a **transposition table** (hash of board state) to prune revisited states; optional board-symmetry canonicalization to shrink the search.

**When:** (a) test-time validation that every classic board and every sampled generated board is solvable; (b) runtime *fallback hint* when the player has deviated from the recorded canonical line.

**Trade-offs:** DFS is exponential on large boards. Acceptable because (i) classic boards are known-small and validated offline, (ii) challenge boards carry a recorded solution so the solver is only a *fallback* from mid-game states (shallower), and (iii) a node/time budget caps worst case — on budget exhaustion the hint returns "no winning move found, undo/restart" rather than hanging.

### Pattern 5: Canvas rendering with animate-from-state

**What:** The **domain `Board` is the single source of truth**; `BoardCanvas` is a pure function of `UiState`. On a move, the ViewModel applies the move to domain state immediately and sets `animating = MoveTransition(from→to, removed=over)`. The Canvas drives `Animatable`s for the sliding peg and the fading jumped peg, then clears `animating` on completion. **No game logic in the Canvas.**

**When:** Gameplay screen only.

**Trade-offs:** Animate-then-state-already-committed avoids the "snapshot drift" bug where animation and logical state disagree. The cost is describing transitions explicitly (the `MoveTransition`), which is also what makes animations testable/deterministic.

### Pattern 6: Platform adapters behind domain interfaces

**What:** `AdGateway` and `GamesService` are interfaces in `domain/platform`. Slice 1 binds **no-op stubs** via Hilt (`requestRewarded` auto-grants, `showInterstitial` no-ops, `GamesService` returns success/empty). Gameplay code calls `adGateway.requestRewarded(HINT)` and never imports an SDK.

**When:** From Slice 1 — wire the interfaces immediately so Slice 2 only swaps the Hilt binding.

**Trade-offs:** A little upfront interface design; the payoff is the core never depends on AdMob/Play Games and stays fully testable.

---

## Data Flow

### Gameplay move loop (the hot path)

```
[tap a peg]                       [tap a target hole]
     │ onPegTap(pos)                   │ onTargetTap(pos)
     ▼                                  ▼
GameViewModel: select peg,        GameViewModel:
  legalTargets = RulesEngine          move = Move(selected, over, target)
  .legalMoves(board)                  board' = ApplyMoveUseCase(board, move)
  → state.copy(selected,             → state.copy(board=board', animating=transition,
     legalTargets)                       canUndo=true, selected=null)
                                       if RulesEngine.isGameOver(board'):
                                          result = CalculateResultUseCase(board', elapsed)
                                          SaveProgressUseCase(result)            (→ ProgressRepository)
                                          sendEffect(NavigateToResults(result))
     ▲                                  │
     └────── state flows down ──────────┘
            BoardCanvas renders + animates transition, clears `animating` on end
```

### Level load / resume

```
ChallengeScreen "Continue" / Classic picker tap
   → ViewModel calls LoadClassicLevelUseCase / GenerateChallengeLevelUseCase
   → LevelRepository.getLevel(ref)
        ├ Classic   → ClassicLevelSource (static)        → Board
        └ Challenge → ChallengeLevelSource → LevelGenerator(seed) → Board + Solution
   → ResumeGameUseCase checks ProgressRepository for InProgressGame snapshot
   → ViewModel seeds GameUiState (fresh board OR restored snapshot+history)
```

### Hint flow

```
onHintClick → adGateway.requestRewarded(HINT)        (Slice 1 stub auto-grants)
   → GetHintUseCase(currentBoard, recordedSolution):
        if currentBoard matches a state on the canonical line → next canonical Move
        else  → Solver.findWinningMove(currentBoard) within budget
                 found → Move ;  none/budget-exceeded → null → Effect.NoWinningMove
   → state.copy(hint = move)  (UI highlights the move)
```

### State management (per screen)

```
Repositories / Room / DataStore  ── Flow ──►  UseCase  ──►  ViewModel
        ▲                                                     │  MutableStateFlow<UiState>.update{}
        │ writes via repo (SSOT)                              ▼
   SaveProgressUseCase ◄── events ◄── onEvent()/fns ◄── Composable ◄── collectAsStateWithLifecycle
                                                              │
                                              one-off Effects via Channel(BUFFERED).receiveAsFlow()
```

---

## Suggested Build Order (vertical slices)

Each slice is **end-to-end playable** and adds one capability. Interfaces (`LevelRepository`, `AdGateway`, `GamesService`) are introduced from VS1 even where impls are stubs, so later slices swap bindings rather than rewire call sites. This maps to the roadmap's Vertical-MVP requirement.

| # | Vertical slice (playable outcome) | Components built | Depends on |
|---|-----------------------------------|------------------|------------|
| **VS1** | **Play one hardcoded board end-to-end** — tap peg, tap hole, peg jumps with animation, game-over detected | `model/*`, `RulesEngine`, `BoardCanvas`, `GameViewModel`+`GameUiState`, one in-code board | — |
| **VS2** | **Classic catalog playable** — Home → pick 1 of 4 boards → play → Results with stars/score | `ClassicLevelSource` + 4 board defs, `LevelRepository`(+Challenge stub), `ScoreCalculator`, `CalculateResultUseCase`, Home/Classic/Results screens, nav | VS1 |
| **VS3** | **Infinite Challenge playable** — deterministic level N generated, solvable, difficulty scales | `LevelGenerator` (reverse-gen), `DifficultyTier`, `Prng`, `Solver` (validation), `ChallengeLevelSource`, `GenerateChallengeLevelUseCase`, Challenge screen | VS2 (RulesEngine, repo seam) |
| **VS4** | **Assist features** — hint (canonical + solver fallback) and undo | `GetHintUseCase`, `UndoUseCase`, move-history stack, `AdGateway` no-op auto-grant wired | VS3 (recorded solution, Solver) |
| **VS5** | **Progress persists** — per-level stars/best, highest level, resume in-progress | `ProgressRepository`+Room, `SettingsRepository`+DataStore, `SaveProgressUseCase`, `ResumeGameUseCase` | VS2 (results), VS4 (history snapshot) |
| **VS6** *(Slice 2)* | **Online + monetization** — swap stubs for AdMob + Play Games | `AdMobAdGateway`, `PlayGamesService`, cloud Saved-Games source behind existing repos | VS1–VS5 interfaces |

**Critical-path dependencies:** `model` + `RulesEngine` are the root — everything depends on them and they are pure, so build and unit-test them first and hardest (solvability is the project's #1 risk). `LevelRepository` seam must exist by VS2 so `LevelGenerator` (VS3) slots behind it without touching UI. `Solver` lands in VS3 for generator validation and is reused in VS4 for fallback hints. Persistence (VS5) is deliberately last among Slice-1 slices because the game is fully playable without it.

---

## Anti-Patterns

### Anti-Pattern 1: Game logic inside composables / Canvas

**What people do:** Compute legal moves, apply jumps, or detect game-over inside `BoardCanvas` or `onClick` lambdas using `remember` board state.
**Why it's wrong:** Recomposition becomes the execution model; logic is untestable; animation and logical state drift apart.
**Do this instead:** `Board` is owned by the ViewModel/domain. Canvas is a pure render of `UiState`; it emits `onPegTap`/`onTargetTap` up and animates a declarative `MoveTransition`.

### Anti-Pattern 2: Repository branches leaking into UI

**What people do:** UI/use cases check `if (challenge) generator else catalog`.
**Why it's wrong:** Couples every caller to source internals; new sources (daily, cloud) touch many files.
**Do this instead:** One `LevelRepository.getLevel(LevelRef)`; routing lives in the impl.

### Anti-Pattern 3: Online solvability search instead of construction

**What people do:** Generate a random board, then run the Solver to *check* it's solvable, retrying until one passes.
**Why it's wrong:** Solver is exponential; generation latency becomes unbounded and non-deterministic.
**Do this instead:** Reverse-generation produces solvable boards **by construction** with a recorded solution; the Solver is reserved for offline validation and runtime fallback hints.

### Anti-Pattern 4: Non-deterministic or platform-dependent PRNG seeding

**What people do:** Seed with `System.nanoTime()`, hashCode of strings, or a hash whose value differs across JVM/ART.
**Why it's wrong:** Breaks "level N is always the same board" across devices/sessions — kills retry/resume/leaderboard fairness.
**Do this instead:** `seed = stableHash(levelNumber)` with an explicit, value-stable hash; `kotlin.random.Random(seed)`; unit-test that the same level reproduces byte-identical boards.

### Anti-Pattern 5: One-off effects modeled as state

**What people do:** `showResultsDialog: Boolean` / `navigated: Boolean` flags in `UiState`.
**Why it's wrong:** Requires consume-and-reset logic; replays on rotation/recomposition.
**Do this instead:** `Channel<Effect>(BUFFERED).receiveAsFlow()` for navigation/snackbar/haptic (per compose-skill effect-delivery guidance).

### Anti-Pattern 6: Premature DI / multi-module overhead

**What people do:** Split into Gradle modules and elaborate Hilt graphs before the game is playable.
**Why it's wrong:** Slows the highest-risk work (rules + generator).
**Do this instead:** Single module, package-by-layer; Hilt for the interface→impl bindings that actually swap in Slice 2 (`AdGateway`, `GamesService`, repos). Modularize only if build times demand it.

---

## Integration Points

### External Services (Slice 2 — behind interfaces from day one)

| Service | Integration pattern | Notes |
|---------|---------------------|-------|
| Google Play Games Services v2 | `GamesService` interface; `PlayGamesService` impl in `platform/games`; cloud source behind `ProgressRepository` | Slice 1 = `NoOpGamesService`. Sign-in/leaderboards/achievements/Saved-Games swapped via Hilt binding only |
| Google Mobile Ads (AdMob) | `AdGateway` interface; `AdMobAdGateway` impl in `platform/ads` | Slice 1 = no-op auto-grant. Rewarded (hint/undo/retry) + interstitial between levels; gameplay only ever calls `requestRewarded(reason)` |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| UI ↔ Domain | Use-case calls (suspend/fns) down; immutable models up | ViewModel maps domain models → `UiState`; no domain type leaks rendering concerns |
| Domain ↔ Data | Repository **interfaces** in domain, impls in data | Dependency inversion; mappers at the data boundary (entity ↔ domain) |
| Domain ↔ Platform | `AdGateway` / `GamesService` interfaces in domain | No SDK import crosses into domain/UI |
| LevelGenerator ↔ RulesEngine | Generator calls RulesEngine inverse-move logic | Keep forward and reverse move math in one place to avoid rule divergence |

---

## Sources

- Android app architecture guide — https://developer.android.com/topic/architecture (HIGH — three layers, SSOT repositories, UDF, optional domain/use-cases, strict UI→Domain→Data dependency direction)
- Design spec §8 (Architecture), §5 (Level Generation), §7 (Scoring) — `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md`
- PROJECT.md constraints & key decisions
- Local `compose-skill` references: `architecture.md` (state ownership, domain-layer rules, effect delivery, state collection/slicing), `mvi.md` (UDF contract, effects-not-state) — `~/.claude/skills/compose-skill`

---
*Architecture research for: native Android peg solitaire (Clean Architecture + MVVM + Repository)*
*Researched: 2026-06-28*
