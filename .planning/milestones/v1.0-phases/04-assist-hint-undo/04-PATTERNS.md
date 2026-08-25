# Phase 4: Assist — Hint & Undo - Pattern Map

**Mapped:** 2026-07-02
**Files analyzed:** 20 (11 new, 9 modified/extended)
**Analogs found:** 20 / 20 (every file has a strong in-repo analog — this phase is a *port + extend*, not greenfield)

> Every new file in this phase copies an existing, shipped, tested pattern from Phases 1–3. The Solver is a port of the working `AuthoringSolver`; the controls copy `RestartControl`/`HintToggleControl`; the AdGateway seam copies the `DataModule` `@Binds` + `LevelRepository`-interface pair; all VM/UiState/Canvas work extends the existing files in place. No file here invents a new pattern.

---

## File Classification

### New files

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `domain/src/main/kotlin/.../domain/solver/Solver.kt` | service (pure-domain search) | batch / transform | `domain/src/test/.../golden/solutions/AuthoringSolver.kt` | exact (port + harden) |
| `domain/src/main/kotlin/.../domain/solver/SolverBudget.kt` | config / constants | — | `ui/game/BoardMetrics.kt` (scoped constants object) | role-match |
| `domain/src/main/kotlin/.../domain/solver/Pagoda.kt` | utility (necessary-condition reject) | transform | `AuthoringSolver.canonical`/`symmetryPermutations` (position-invariant math) | role-match |
| `domain/src/main/kotlin/.../domain/usecase/GetHintUseCase.kt` | use case (pure decision + Solver call) | request-response | `domain/rules/RulesEngine.kt` (pure stateless domain authority) | role-match |
| `app/.../platform/ads/AdGateway.kt` | interface (platform seam) | request-response | `data/LevelRepository.kt` (`interface LevelRepository`) | role-match |
| `app/.../platform/ads/AdReason.kt` | model (enum) | — | `data/LevelRepository.kt` `ClassicLevelIds` (named-id value object) | role-match |
| `app/.../platform/ads/NoOpAdGateway.kt` | service (stub impl) | request-response | `data/LevelRepository.kt` `DefaultLevelRepository` (`@Inject constructor` impl) | exact |
| `app/.../di/PlatformModule.kt` | config / DI | — | `di/DataModule.kt` (`@Binds` interface→impl) | exact |
| `app/.../ui/game/HintControl.kt` | component (leaf control) | event-driven | `ui/game/RestartControl.kt` + `HintToggleControl.kt` | exact |
| `app/.../ui/game/UndoControl.kt` | component (leaf control) | event-driven | `ui/game/RestartControl.kt` | exact |
| `app/.../ui/game/NoWinBanner.kt` | component (stateless leaf) | event-driven | `ui/game/GameScreen.kt` `RestartConfirmDialog` (lines 249–287) | role-match |

### Modified / extended files

| Modified File | Role | Data Flow | Analog (self-extend) | Match Quality |
|---------------|------|-----------|----------------------|---------------|
| `app/.../ui/game/GameViewModel.kt` | controller (VM) | event-driven | itself — extend (hint job, undo, stuck-rescue gate) | exact |
| `app/.../ui/game/GameUiState.kt` | model (@Immutable state) | — | itself — add hint/thinking/no-win fields | exact |
| `app/.../ui/components/BoardCanvas.kt` | component (Canvas render) | transform | itself — extend `drawCell`/`drawBoard` | exact |
| `app/.../ui/game/BoardMetrics.kt` | config (ratios) | — | itself — add `HINT_*` ratios | exact |
| `app/.../ui/game/GameScreen.kt` | component (screen) | event-driven | itself — add controls + banner slots | exact |
| `app/.../ui/game/GameRoute.kt` | controller (route) | event-driven | itself — wire new VM handlers | exact |
| `app/.../ui/game/HintToggleControl.kt` | component (rename strings) | event-driven | itself — repoint strings (D-02) | exact |
| `app/src/main/res/values/strings.xml` | config (copy) | — | itself — add/rename keys per UI-SPEC | exact |
| `app/src/main/res/values/dimens.xml` | config (dimens) | — | itself — add `nowin_banner_min_height` | exact |

---

## Pattern Assignments

### `domain/solver/Solver.kt` (service, batch/transform) — HIGHEST PRIORITY

**Analog:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt` (the whole file is the source; port `object`, `VisitedSet`, `symmetryPermutations`, `canonical`, `inwardness`, `centroidOf` **verbatim** into `:domain` main).

**Object + constants shape to copy** (AuthoringSolver.kt lines 39–68):
```kotlin
object AuthoringSolver {
    private const val SOLVED_PEG_COUNT = 1
    private const val MAX_DISTINCT_PER_ATTEMPT = 25_000_000L   // → replace with injected SolverBudget.maxNodes
    var lastVisited: Long = 0
        private set
```

**Core memoized-DFS pattern to port** (AuthoringSolver.kt lines 80–118) — the recursion, the goal-check-before-memo, and the memo insert:
```kotlin
fun forwardSolve(start: Board): List<Move>? {
    val symmetries = symmetryPermutations(start.spec)
    val center = centroidOf(start.spec)
    ...
    fun search(board: Board, path: ArrayDeque<Move>): List<Move>? {
        if (board.pegCount == SOLVED_PEG_COUNT) return path.toList()   // goal FIRST (before memo)
        var solution: List<Move>? = null
        val withinBudget = seen.size < MAX_DISTINCT_PER_ATTEMPT
        if (withinBudget && seen.add(canonical(board.pegs, symmetries))) {
            val ordered = RulesEngine.legalMoves(board)
                .sortedByDescending { inwardness(it, center) + random.nextDouble() * INWARD_JITTER }
            for (move in ordered) {
                path.addLast(move)
                solution = search(RulesEngine.applyMove(board, move), path)
                path.removeLast()
                if (solution != null) break
            }
        }
        return solution
    }
    ...
}
```

**Four required changes vs the analog (from RESEARCH Pattern 1):**
1. **Arbitrary start + first-move return.** Signature becomes `fun firstWinningMove(board: Board, budget: SolverBudget, shouldCancel: () -> Boolean = { false }): Move?`. Accept any mid-game board; return `path.first()` / the top-level move whose resulting position is solvable, not the whole line.
2. **Node budget.** Replace `MAX_DISTINCT_PER_ATTEMPT` (line 57) with `budget.maxNodes`; `seen.size >= budget.maxNodes` ⇒ treat as not-found (`null`, D-04).
3. **Cooperative cancel + deadline.** Add `if ((counter++ and CANCEL_MASK) == 0 && shouldCancel()) return false` inside the recursion (every ~4096 nodes). **No `kotlinx.coroutines` import** — cancellation enters only via the plain `shouldCancel: () -> Boolean` lambda (purity, see Shared Pattern: Domain Purity).
4. **Drop authoring-only machinery.** Remove `MAX_RESTARTS` (line 47), `INWARD_JITTER` (line 65), `BASE_SEED` (line 44) and the per-restart `Random`. Keep `inwardness` ordering as a deterministic tie-break (no jitter).

**Reuse verbatim (do NOT reimplement):**
- `VisitedSet` (lines 278–335) — the `long[]` open-addressing memo (~16 B/entry). **Never `HashSet<Long>`** (boxes to ~72 B, OOMs — documented lines 270–277).
- `symmetryPermutations` (lines 157–186) + `canonical` (lines 188–205) — up to 8× state fold.
- `RulesEngine.legalMoves`/`applyMove` as the ONLY move authority (RulesEngine.kt lines 42–77) — never re-derive jump logic.

---

### `domain/solver/SolverBudget.kt` (config, constants)

**Analog:** `ui/game/BoardMetrics.kt` (lines 15–51) — a scoped `object` of named constants that satisfies QUAL-03 "no magic numbers," documented per-value. Also `AuthoringSolver.kt` lines 40–65 for the value-with-rationale doc style.

**Pattern to copy** (scoped constants object, BoardMetrics.kt):
```kotlin
object BoardMetrics {
    /** Resting/jumping peg radius = ratio × cellSize. */
    const val PEG_RADIUS_RATIO = 0.34f
    ...
}
```
Apply as a small `data class SolverBudget(val maxNodes: Long, val deadline: Duration)` **plus** a companion of scoped defaults (`DEFAULT_MAX_NODES`, `DEFAULT_DEADLINE`, `CANCEL_MASK`). RESEARCH starting values: `maxNodes ≈ 40_000_000`, `deadline ≈ 1500 ms` — but these are gated behind the D-04 measurement task (SolverBudgetTest), not shipped blind. `Duration` is `kotlin.time` (pure Kotlin, no Android — safe in `:domain`).

---

### `domain/solver/Pagoda.kt` (utility, transform)

**Analog:** `AuthoringSolver.kt` `canonical` (lines 188–205) + `symmetryPermutations` (157–186) — the same family of pure position-invariant bit math over `Board.pegs`. Iterate set bits with `java.lang.Long.numberOfTrailingZeros(remaining)` / `remaining and (remaining - 1)` exactly as `canonical` does (lines 196–201).

**Pattern to copy** (set-bit iteration, AuthoringSolver.kt lines 195–201):
```kotlin
var remaining = pegs
while (remaining != 0L) {
    val bit = java.lang.Long.numberOfTrailingZeros(remaining)
    // ... accumulate weighted resource-count over occupied cells ...
    remaining = remaining and (remaining - 1)
}
```
Expose `fun mayBeSolvable(board: Board): Boolean` — a *necessary* condition (weighted pagoda/resource-count sum ≥ target weight). Returns `true` when it cannot prove unsolvability; `false` only when provably dead (Solver short-circuits to `null` in O(cells)). The GF(4)/colour invariant documented in `AuthoringSolver.kt` `SolutionFixtures` (lines 230–243) is the same invariant family — reference it for the weighting rationale.

---

### `domain/usecase/GetHintUseCase.kt` (use case, request-response)

**Analog:** `domain/rules/RulesEngine.kt` (lines 21–89) — a pure, stateless `:domain` authority with no Android/coroutine deps; documented, single-responsibility. `GetHintUseCase` follows the same "pure object/class, plain params in → domain value out" shape.

**Decision pattern** (from RESEARCH Pattern 2):
```kotlin
// GetHintUseCase(board, solution, movesPlayed, budget, shouldCancel): Move?  (pure)
// 1. Challenge + still on recorded line?  →  return solution[movesPlayed]   (instant, guaranteed)
// 2. otherwise (Classic, or deviated)     →  Solver.firstWinningMove(board, budget, shouldCancel)
```
**Forbidden:** blind `solution[moveCount]` indexing. The caller passes `solution` only when `onRecordedLine` is true (the VM tracks that boolean — see GameViewModel assignment below). `Move`/`Board` are the domain types (`Move.kt`, `Board.kt`). Constructor-inject nothing Android; keep it a plain class (or `object` calling `Solver`) for pure-JVM tests without Robolectric.

---

### `platform/ads/AdGateway.kt` (interface, request-response)

**Analog:** `data/LevelRepository.kt` (lines 71–84) — an `interface` with KDoc per method, implemented by a `@Inject constructor` class and bound in a Hilt module.

**Pattern to copy** (interface shape, LevelRepository.kt lines 71–84) → applied:
```kotlin
interface AdGateway {
    suspend fun requestRewarded(reason: AdReason): Boolean   // Slice 1 stub: always true
    fun showInterstitial()                                    // Slice 1 stub: no-op
}
```
`requestRewarded` is `suspend` (Slice 2 awaits a real ad; the stub returns immediately). Lives in a new `app/.../platform/ads/` package (spec §8.5).

---

### `platform/ads/AdReason.kt` (model, enum)

**Analog:** `data/LevelRepository.kt` `ClassicLevelIds` (lines 52–62) — a small named-constant value object documenting each member.
```kotlin
enum class AdReason { HINT, UNDO }   // RETRY reserved for Slice 2 (interstitials)
```

---

### `platform/ads/NoOpAdGateway.kt` (service, stub impl)

**Analog:** `data/LevelRepository.kt` `DefaultLevelRepository` (lines 91–102) — `class … @Inject constructor(...) : Interface`.

**Pattern to copy** (`@Inject constructor` impl, LevelRepository.kt lines 91–96):
```kotlin
class DefaultLevelRepository @Inject constructor(
    private val classicSource: ClassicLevelSource,
    private val challengeSource: ChallengeLevelSource,
) : LevelRepository { ... }
```
→ applied:
```kotlin
class NoOpAdGateway @Inject constructor() : AdGateway {
    override suspend fun requestRewarded(reason: AdReason) = true
    override fun showInterstitial() = Unit
}
```

---

### `di/PlatformModule.kt` (config, DI)

**Analog:** `di/DataModule.kt` (lines 32–49) — `@Module @InstallIn(SingletonComponent::class) abstract class` with `@Binds @Singleton`.

**Pattern to copy** (DataModule.kt lines 32–49):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindLevelRepository(impl: DefaultLevelRepository): LevelRepository
    ...
}
```
→ applied: `@Binds @Singleton abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway`. Slice 2 swaps only this one binding. (Folding into `DataModule` is acceptable per RESEARCH; a dedicated `PlatformModule` matches spec §8.5 packaging.)

---

### `ui/game/HintControl.kt` (component, leaf) & `ui/game/UndoControl.kt` (component, leaf)

**Analog:** `ui/game/RestartControl.kt` (whole file, lines 27–40) for the plain action; `ui/game/HintToggleControl.kt` (lines 29–49) for the tint/state variation.

**Pattern to copy** (stateless `IconButton` leaf, RestartControl.kt lines 27–40):
```kotlin
@Composable
fun RestartControl(onRestart: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onRestart, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.restart_board),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
        )
    }
}
```

**`HintControl` deltas (UI-SPEC §1):** icon `Icons.Rounded.Search`; `contentDescription` = `R.string.hint_action`; resting tint `onSurface`. While `solverThinking`: swap the `Icon` for `CircularProgressIndicator` sized `top_bar_icon_size`, set `IconButton(enabled = false)`, and switch `contentDescription` to `R.string.hint_thinking`. (`CircularProgressIndicator` import already used in `GameScreen.kt` line 13 / `LoadingIndicator` lines 242–247 — copy that usage.)

**`UndoControl` deltas (UI-SPEC §2):** icon `Icons.AutoMirrored.Rounded.Undo` (mirror the `Icons.AutoMirrored.Rounded.ArrowBack` import style in `GameScreen.kt` line 11); `contentDescription` = `R.string.undo_action`; `enabled = canUndo`; tint `onSurface` when enabled else `onSurfaceVariant` (copy the enabled/disabled tint ternary from `HintToggleControl.kt` lines 40–46).

---

### `ui/game/NoWinBanner.kt` (component, stateless leaf)

**Analog:** `ui/game/GameScreen.kt` `RestartConfirmDialog` (lines 249–287) for the multi-`TextButton` action row + `stringResource`/`MaterialTheme.typography` usage; `GameScreen.kt` board-panel `Surface` (lines 126–134) for the M3 `Surface` + shape + color-role pattern.

**Pattern to copy — actions row** (RestartConfirmDialog.kt lines 269–286):
```kotlin
confirmButton = {
    TextButton(onClick = onConfirm) {
        Text(
            text = stringResource(R.string.restart_board),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
```

**Pattern to copy — M3 Surface** (GameScreen.kt lines 126–134):
```kotlin
Surface(
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.widthIn(max = dimensionResource(R.dimen.board_max_size))...,
) { ... }
```

**`NoWinBanner` deltas (UI-SPEC §4):** NON-blocking `Surface` (NOT `AlertDialog`), `color = errorContainer`, text `onErrorContainer` `bodyMedium`, `shape = MaterialTheme.shapes.medium`, content padding `spacing_md`, `heightIn(min = dimensionResource(R.dimen.nowin_banner_min_height))`. One shared component parameterized by `message: String` (`nowin_message` vs `stuck_message`) + three `TextButton`s in a `Row` with `spacing_sm` gaps: Undo · Restart · Home (`labelLarge`). Handlers hoisted (`onUndo`, `onRestart`, `onHome`). Attach a `testTag` + semantics like `BoardCanvas`/`GameScreen` do (BoardCanvas.kt line 37; GameScreen.kt lines 42–52) for the Compose/Roborazzi test.

---

### `ui/game/GameViewModel.kt` (controller/VM) — EXTEND

**Analog:** itself. Copy these existing idioms for the new work.

**Thread `solution` in (D-03).** Mirror the existing `classicBoard`/`initialBoard` load (lines 109–120) — add a `private var solution: List<Move>? = null` and a `private var onRecordedLine = true`. For Challenge, capture `levelRepository.loadChallenge(...).solution` inside the existing `init` async block (lines 176–185); Classic leaves it `null`.

**Off-thread cancellable job.** Copy the exact `Job` + `withContext(defaultDispatcher)` + `isActive` idiom already in the file:
- `Job` field pattern — `private var tickerJob: Job? = null` (line 158); add `private var hintJob: Job? = null`.
- Off-main pattern — `withContext(defaultDispatcher) { … }` (lines 178–181).
- Cooperative-cancel source — `while (isActive && …)` (line 302) is the existing precedent for the `shouldCancel` lambda `{ !isActive || deadline.hasPassedNow() }`.

**`onHintRequested()` shape** (RESEARCH Code Examples): `hintJob?.cancel()` → set `solverThinking=true` → `viewModelScope.launch { if (adGateway.requestRewarded(AdReason.HINT)) { val move = withContext(defaultDispatcher){ getHintUseCase(...) }; update state } }`. `AdGateway` is a new constructor param — add it to the `@Inject constructor` list (lines 71–81) exactly like `settingsRepository`.

**`onUndo()` — replay from initialBoard (D-05/D-07).** Copy the `moveHistory.fold` idea; `moveHistory` (line 134) and `initialBoard` (line 120) already exist. Rebuild board via `moveHistory.fold(base) { b, m -> rulesEngine.applyMove(b, m) }`, recompute `onRecordedLine`, recompute `status` via existing `statusFor` (lines 358–363), clear hint/no-win/selection fields, reset `finishedEmitted=false` when back to Playing (mirror `resetToInitial` line 347). **Do NOT touch `startMark`/`tickerJob`/`elapsedSeconds`** (D-07) — but resume ticker if Stuck→Playing (re-call `startTimerIfNeeded`-style relaunch).

**Stuck-rescue gate (D-06) — the trickiest change.** Narrow the existing one-shot emission (lines 271–287):
```kotlin
if (!finishedEmitted && status != GameStatus.Playing) {   // CHANGE → status == GameStatus.Won
    finishedEmitted = true
    tickerJob?.cancel(); tickerJob = null
    _finished.trySend(scoreCalculator.evaluate(...))
}
```
On `status == GameStatus.Stuck`, instead of emitting, set a rescue flag on `GameUiState` (banner). Move the ticker-stop so **both** Won and Stuck freeze the timer, but **only Won emits `finished`**. Keep `finishedEmitted` guard for Won.

---

### `ui/game/GameUiState.kt` (model) — EXTEND

**Analog:** itself (lines 36–89). Add `val` fields to the `@Immutable data class` and to both factory functions.

**Pattern to copy** (existing field + factory, GameUiState.kt lines 36–66): add `hintSource: Position? = null`, `hintTarget: Position? = null`, `solverThinking: Boolean = false`, `noWinningMove: Boolean = false` (or a unified rescue flag per RESEARCH Open-Q2). Keep every field `val`, keep `@Immutable`, add them to both `initial(...)` (lines 55–66) and `loading()` (lines 74–85) with defaults, and document each in the KDoc block (lines 20–34 style). `Position` is already imported (line 8).

---

### `ui/components/BoardCanvas.kt` (component, render) — EXTEND

**Analog:** itself. Extend `drawCell`/`drawBoard` and the `BoardPalette`.

**Palette capture pattern to copy** (BoardCanvas.kt lines 85–93): add `hint = MaterialTheme.colorScheme.tertiary` to `BoardPalette` (class lines 147–154) — the new accent role per UI-SPEC (keeps hint ≠ selection `primary`).

**Ring/dot draw pattern to copy** (`drawSelectedPeg`, lines 259–275, and the destination-dot, lines 222–228):
```kotlin
drawCircle(
    color = palette.accent,
    radius = geometry.selectedRingRadius,
    center = center,
    style = Stroke(width = palette.ringWidthPx),
)
```
Draw order (UI-SPEC §3): target ring (B) → source glow ring (A) → arrow `Path` A→B, all `tertiary`. Suppress under `interactive = false` exactly as selection/dots are (lines 172–174, 222). New magnitudes are `BoardMetrics` ratios (below) — never inline dp/float literals.

---

### `ui/game/BoardMetrics.kt` (config) — EXTEND

**Analog:** itself (lines 15–51). Add the UI-SPEC §3 ratios as documented `const val` (copy the existing `SELECTED_RING_RATIO`/`DESTINATION_DOT_ALPHA` doc style, lines 25–35): `HINT_SOURCE_RING_RATIO = 0.42f`, `HINT_TARGET_RING_RATIO = 0.30f`, `HINT_ARROW_WIDTH_RATIO = 0.06f`, `HINT_HIGHLIGHT_ALPHA = 0.9f`, `HINT_ARROW_ALPHA = 0.5f`.

---

### `ui/game/GameScreen.kt` (component) — EXTEND

**Analog:** itself. Add `HintControl` + `UndoControl` to the `actions` row (lines 182–186, alongside `RestartControl`); add new hoisted handler params (`onHint`, `onUndo`, `onHome`) to the `GameScreen`/`GameTopBar` signatures (lines 84–95). Render `NoWinBanner` conditionally over the board `Box` when the rescue flag is set (mirror the `if (state.confirmRestart) RestartConfirmDialog(...)` conditional, lines 146–151). Route the banner's Restart through the existing `RestartConfirmDialog` (lines 249–287).

---

### `ui/game/GameRoute.kt` (controller/route) — EXTEND

**Analog:** itself (lines 53–64). Wire the new VM method references through `GameScreen(...)` exactly as `onToggleHints = viewModel::onToggleHints` (line 60): add `onHint = viewModel::onHintRequested`, `onUndo = viewModel::onUndo`, `onHome = onNavigateBack` (Home root, RESEARCH Open-Q3).

---

### `ui/game/HintToggleControl.kt` + `strings.xml` (rename, D-02)

**Analog:** itself. Repoint `R.string.hints_on`/`hints_off` (HintToggleControl.kt lines 38–39) to new `show_moves_on`/`show_moves_off`. In `strings.xml`, rename keys at lines 78–79 and add the new Phase-4 keys per the UI-SPEC Copywriting table: `hint_action`, `hint_thinking`, `undo_action`, `show_moves_on`, `show_moves_off`, `nowin_message` ("No winning move — undo or restart"), `stuck_message`, `rescue_restart`. Reuse existing `action_home` (line 93) and `restart_board`/`keep_playing`/`restart_confirm_*` (lines 14, 25–27).

---

### `dimens.xml` — EXTEND

**Analog:** itself (lines 12–51). Add the single permitted new token `nowin_banner_min_height` = 48dp (multiple of 4, UI-SPEC §Spacing). All other banner spacing reuses `spacing_md`/`spacing_sm`.

---

## Shared Patterns

### Domain Purity (applies to ALL new `:domain` files: Solver, SolverBudget, Pagoda, GetHintUseCase)
**Source:** `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt` (lines 22–31) — ArchUnit reads bytecode and fails the build on any `android.*`/`androidx.*` dep in `..domain..`.
```kotlin
noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat().resideInAnyPackage("android..", "androidx..")
```
**Apply to:** every new domain file. **No `kotlinx.coroutines` import in the Solver/UseCase** — cancellation crosses the boundary only as a plain `shouldCancel: () -> Boolean` lambda constructed by the VM. `kotlin.time.Duration` and `java.lang.Long.*` (already used in `AuthoringSolver`/`Board`) are pure-JVM and allowed.

### Rules authority (applies to Solver, GetHintUseCase, Undo)
**Source:** `domain/rules/RulesEngine.kt` (lines 42–79) — `legalMoves`/`applyMove`/`isGameOver`, deterministically sorted.
**Apply to:** the Solver's search, the Undo replay-fold, and any move validation. **Never re-derive jump logic** (RESEARCH Anti-Pattern). The VM already re-derives taps through `legalMoves` (`performJump`, lines 245–248) — the hinted move must likewise be a member of `legalMoves(currentBoard)` before it is highlighted.

### Hilt `@Binds` interface→impl seam (applies to AdGateway/NoOpAdGateway/PlatformModule)
**Source:** `di/DataModule.kt` (lines 32–49) — `@Module @InstallIn(SingletonComponent::class) abstract class` + `@Binds @Singleton`; impls are `@Inject constructor` classes (`DefaultLevelRepository`, LevelRepository.kt lines 91–96).
**Apply to:** the entire AdGateway seam — Slice 2 swaps one binding, no gameplay/UI change.

### Stateless leaf control (applies to HintControl, UndoControl, NoWinBanner)
**Source:** `ui/game/RestartControl.kt` (lines 27–40) + `HintToggleControl.kt` (lines 29–49).
**Apply to:** every new composable — no state, hoisted handlers/values, `stringResource` for all copy, `dimensionResource(R.dimen.top_bar_icon_size)` for icon size, `MaterialTheme.colorScheme.*` for every color, core-set icons only (`material-icons-core`). State-hoisting per `compose-skill` (installed at `~/.claude/skills/compose-skill`; MVVM named-function convention, not MVI — the project already uses named VM functions).

### Off-thread cancellable coroutine (applies to GameViewModel hint pipeline)
**Source:** `GameViewModel.kt` — injected `defaultDispatcher` (line 79), `withContext(defaultDispatcher)` (lines 178–181), `Job` field + cancel (lines 158, 273), `while (isActive && …)` cooperative loop (line 302).
**Apply to:** `onHintRequested()` — cancel the prior `hintJob`, run the Solver in `withContext(defaultDispatcher)`, feed `!isActive || deadline.hasPassedNow()` as the Solver's `shouldCancel`.

### Test corpus + test structure (applies to all new/extended tests)
**Source:** committed golden solutions `domain/src/test/resources/golden/solutions/{english33,european37,asymmetric39,wiegleb45}.txt` — replay each to every prefix length for the D-04 solvable-mid-game corpus. `RulesEngineTest.kt` is the `:domain` JVM-test analog; `GameViewModelTest.kt` (Turbine + `TestTimeSource` + `StandardTestDispatcher`, lines 56–82) is the VM-test analog to extend for undo/stuck-rescue/thinking/cancellation.

---

## No Analog Found

None. Every file to be created or modified has a strong in-repo analog. The single high-risk component — the production `Solver` — is a port of the already-tested `AuthoringSolver` (test source set), so even it copies working code rather than inventing a pattern.

---

## Metadata

**Analog search scope:** `app/src/main/java/.../{ui/game, ui/components, di, data}`, `domain/src/main/kotlin/.../{rules, model, generator}`, `domain/src/test/.../golden/solutions`, `app/src/test/...`, `res/values/{strings,dimens}.xml`, `~/.claude/skills/compose-skill`.
**Files scanned:** 24 read in full or targeted (GameViewModel, GameUiState, AuthoringSolver, HintToggleControl, RestartControl, DataModule, DomainModule, BoardCanvas, BoardMetrics, GameScreen, GameRoute, GeneratedLevel, RulesEngine, DomainPurityTest, LevelRepository, Board, Move, GameViewModelTest, BoardSpec, strings.xml, dimens.xml, compose-skill SKILL.md, golden solutions dir).
**Pattern extraction date:** 2026-07-02
