# Phase 4: Assist — Hint & Undo - Research

**Researched:** 2026-07-02
**Domain:** Pure-Kotlin search (budgeted peg-solitaire Solver) + MVVM assist pipeline + Compose highlight/banner UI
**Confidence:** HIGH (all findings grounded in the existing codebase; Solver design derived from the shipped `AuthoringSolver`; budget methodology backed by known state-space figures)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Hint presentation**
- **D-01: Hint reveals source + destination.** On a found winning move, glow **both** the peg to move (A) and the target hole (B), with a faint A→B arrow. The player then taps to perform it. **Do NOT auto-perform the jump** — it's an assist, the player still makes the move.
- **D-02: Disambiguate from the existing "hint dots."** The current `showHints`/`HintToggleControl` (persisted legal-destination dots) is renamed to **"Show possible moves"** (or equivalently clear label). The new winning-move action is **"Hint"**. The dots toggle stays a render gate; "Hint" is a distinct action.

**Hint scope & fallback**
- **D-03: Hint works in BOTH Classic and Challenge.** Challenge uses the recorded `solution` (instant, guaranteed) then the `Solver` on deviation; **Classic uses the `Solver` on every hint** (Classic ships no recorded line).
- **D-04: Budget miss is treated as "no winning move."** Any `Solver` failure to return a move within budget surfaces the same "no winning move" state. **Planning constraint:** because Classic is Solver-only and a budget miss reads as "unsolvable," the `Solver`'s node/time budget MUST be tuned high enough that genuinely-solvable full-size classic positions (English-33, Wiegleb-45, etc. from arbitrary mid-game states) essentially never hit the budget. Verify Solver latency/completeness on the largest classic footprints during research/planning.

**Undo behavior**
- **D-05: Multi-step, repeatable Undo back to the start.** Each Undo reverts one more move; repeated taps walk all the way back to the initial board. Backed by the existing `moveHistory` list. **No redo.**
- **D-06: Undo rescues a Stuck board.** A stuck game (2+ pegs, no legal moves) NO LONGER auto-routes straight to Results. Instead it shows an in-board terminal state offering **Undo**, plus Retry/Home. Modifies Phase-3 D-10. A **Won** game still proceeds to Results.
- **D-07: Undo does not touch the timer or apply a penalty.** Undo rewinds board + move history only; the elapsed timer keeps counting real time.

**"No winning move" state & Solver feedback**
- **D-08: Inline, non-blocking banner on the live board.** When the Solver confirms unsolvable (or budget-miss per D-04): banner over the still-visible, still-interactive board: "No winning move — undo or restart." Not a modal.
- **D-09: No-win actions = Undo · Restart · Home.** Restart reloads the same deterministic board; Home exits.
- **D-10: "Thinking…" indicator on the Hint control.** While the Solver computes off-thread, the Hint button shows a brief spinner/disabled state, then resolves to the highlighted move (D-01) or the no-win state (D-08).

**AdGateway seam**
- **D-11: Establish the `AdGateway` interface + no-op auto-grant stub this phase.** Gameplay calls something like `adGateway.requestRewarded(reason): Boolean` and the Slice-1 stub auto-grants (returns true). Bind the stub in a Hilt module so Slice 2 swaps only the binding. `showInterstitial()` is stubbed no-op.

### Claude's Discretion
- Exact `Solver` algorithm, memoization/transposition, and the concrete node/time **budget values** (subject to the D-04 completeness constraint) — implementation + measurement.
- Exact button placement/iconography for Hint and Undo, following `compose-skill` + existing `RestartControl`/`HintToggleControl` conventions.
- The precise label for the renamed dots toggle (D-02) and whether Won ever offers Undo (default: no — Won → Results).
- Whether the recorded `solution` is threaded via `GameViewModel` reading `Level.solution` or a small use case (`GetHintUseCase`) — planner's call; behavior is fixed by D-01..D-04.
- Exact `AdGateway` method signature and reason enum (D-11).

### Deferred Ideas (OUT OF SCOPE)
- Rewarded-ad gating of hint/undo/retry + real AdMob impl — Slice 2. Only the `AdGateway` interface + auto-grant stub are built now.
- Redo — out of scope.
- Persisting move history / resume with undo intact — Phase 5 (PERS-03). This phase keeps `moveHistory` in-memory only.
- Interstitial "retry between levels" ads — Slice 2.
- Hint animation polish / richer arrow visuals beyond the source+dest highlight — Slice-2 polish (POLI-01); the functional highlight ships now.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| ASST-01 | Player can use a hint that reveals the next winning move (free in Slice 1) | State-relative hint pipeline (§Pattern 2); recorded-line lookup relative to current board; production `Solver` (§Pattern 1); `AdGateway` auto-grant seam (§Pattern 5). Source+dest highlight fields on `GameUiState` (§Pattern 6). |
| ASST-02 | Player can undo the last move | Undo mechanics — replay-from-`initialBoard` over the existing `moveHistory` (§Pattern 3). Timer untouched (D-07). Interacts with the one-shot `finished` Channel / `finishedEmitted` guard (§Pattern 4). |
| ASST-03 | Hint handles deviation — falls back to solver, shows clear "no winning move" state when unsolvable | Solver on `defaultDispatcher`, node+time budgeted, cooperatively cancellable (§Pattern 1); budget-miss ⇒ no-win (D-04); inline non-blocking banner (§Pattern 6); Stuck-rescue state machine change (§Pattern 4). |
</phase_requirements>

## Summary

This is a UI + domain phase with **one genuinely hard, high-risk component: a production, budgeted, pure-`:domain` `Solver`.** Everything else (state-relative hint routing, undo, the stuck-rescue state change, the AdGateway seam, the Compose highlights/banner) is well-supported by patterns already established in this codebase (MVVM `StateFlow` UiState, `defaultDispatcher` off-main work, `Channel` one-shot nav, scoped constants + dimens, stateless leaf controls).

The good news on the Solver: the repo already contains a **working, correct, symmetry-reducing, memoized DFS** in `domain/src/test/.../golden/solutions/AuthoringSolver.kt` that solves all four one-peg-solvable catalog boards including the 9×9 45-hole Wiegleb board. The production Solver is a *port and hardening* of that code into `:domain` main, not a from-scratch build. The three changes required are: (1) accept an **arbitrary mid-game board** (not just a single-vacancy start) and return the **first move of a winning line** rather than the whole line; (2) add a **node budget AND a cooperative cancellation check** so a coroutine on `Dispatchers.Default` can abandon it; (3) drop the authoring-only jitter/restart machinery in favor of a single deterministic exhaustive-within-budget search. The existing `VisitedSet` (open-addressed `long[]` memo) and `symmetryPermutations`/`canonical` symmetry reduction port over unchanged and are exactly what keeps this tractable.

The single subtle correctness point that drives the D-04 budget: **finding a solution is fast (heuristic + memo surface one early), but *proving no solution exists* requires exhausting the reachable forward subtree.** That exhaustive case is the budget's worst case. The saving grace is that peg count is strictly monotonically decreasing (every jump removes one peg), so the forward-reachable set from any mid-game position is *bounded and shrinks with fewer pegs* — the full-board authoring solve of Wiegleb-45 (observed ~4.5M canonical positions to first solution) is a useful reference scale for the largest board. The plan must specify a **measurement task** that samples real mid-game states and sizes the budget with a wide safety margin, plus a fast necessary-condition prune (pagoda/resource-count) to prove many dead positions unsolvable *without* full search.

**Primary recommendation:** Port `AuthoringSolver` into a new pure-`:domain` `Solver` object that takes `(Board)` and returns `Move?` (the first move of a winning line, or `null`), guarded by a `SolverBudget(maxNodes, deadline)` and a `shouldCancel: () -> Boolean` hook; add a pagoda-function fast-reject; run it from `GameViewModel` on the injected `defaultDispatcher` via a cancellable `Job`; route hint through recorded-line-first (Challenge) → Solver → no-win; implement Undo as replay-from-`initialBoard` over `moveHistory`; gate the existing `finished` emission so Stuck/no-win shows an in-board rescue state instead of navigating.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Winning-move search (Solver) | Domain (`:domain`, pure Kotlin) | — | QUAL-01 requires pure-Kotlin domain logic; JVM-unit-testable; reuses `RulesEngine`. Zero Android. |
| Off-thread execution + cancellation of the Solver | UI (`GameViewModel` on `defaultDispatcher`) | Domain (cooperative cancel hook) | Coroutine scoping is an Android/ViewModel concern; the domain exposes a pure `shouldCancel` seam so it stays Android-free. |
| Recorded-line vs Solver routing (hint pipeline) | UI (`GameViewModel`) or Domain (`GetHintUseCase`) | Domain (`Solver`) | Behavior is fixed (D-01..D-04); a thin `GetHintUseCase` in `:domain` keeps the VM lean and is the spec's §10 shape — recommended (see Pattern 2). |
| Undo (board reconstruction) | UI (`GameViewModel` + in-memory `moveHistory`) | Domain (`RulesEngine.applyMove` replay) | `moveHistory` already lives in the VM; reconstruction is replay via the domain engine. |
| Stuck / no-win terminal state machine | UI (`GameViewModel` `finished` gating + `GameUiState`) | — | Navigation and one-shot events are UI concerns. |
| AdGateway grant seam | Platform (`platform/ads` interface) + DI (Hilt stub binding) | UI (VM requests grant) | Spec §8.4 — platform adapter behind an interface; Slice 2 swaps the binding only. |
| Hint highlight / thinking / banner rendering | UI (Compose: `BoardCanvas` overlay + screen banner) | — | Pure presentation of hoisted `GameUiState` fields. |

## Standard Stack

**No new external dependencies are introduced this phase.** Everything needed is already in the Phase 1–3 version catalog: kotlinx-coroutines (`Dispatchers.Default`, `Job`, `withContext`, `ensureActive`), Compose BOM (Canvas/animation/Material 3), Hilt (`@Binds` stub), and the test stack (JUnit4, Turbine, Truth, MockK, Robolectric/Roborazzi). `[VERIFIED: codebase — gradle/libs.versions.toml + existing GameViewModel imports]`

### Core (all already present)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| kotlinx-coroutines | 1.11.0 | Solver off-thread on `defaultDispatcher`; cancellable `Job`; `ensureActive()`/`isActive` cooperative cancel | Already the project async substrate; `GameViewModel` already injects `defaultDispatcher` for O(level) generation `[VERIFIED: codebase — GameViewModel ctor + DomainModule.provideDefaultDispatcher]` |
| Compose BOM | 2026.06.00 | Board highlight overlay (glow A + target ring B + arrow Path), thinking spinner, inline banner | `BoardCanvas` already draws circles/strokes and animates via `Animatable`; extend the same DrawScope `[VERIFIED: codebase — BoardCanvas.kt]` |
| Hilt (Dagger) | 2.60 | Bind `AdGateway` → `NoOpAdGateway` stub via `@Binds` | Mirrors existing `DataModule` `@Binds` bindings `[VERIFIED: codebase — DataModule.kt]` |

### Supporting (testing — already present)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit4 | 4.13.2 | Pure-JVM Solver correctness + budget tests in `:domain` | Solver search correctness, "returns null iff unsolvable", budget-miss behavior |
| Turbine | 1.2.1 | Assert `GameUiState` emissions (thinking → highlighted / no-win) | Hint pipeline + undo + stuck-rescue VM tests |
| MockK | 1.14.x | Stub `AdGateway` in VM tests | Assert VM requests a grant before hinting/undoing |
| Robolectric + Roborazzi | latest | VM tests needing `SavedStateHandle.toRoute`; screenshot the highlight/banner | Existing `GameViewModelTest` already runs under Robolectric `[VERIFIED: codebase — GameViewModelTest @RunWith(RobolectricTestRunner)]` |

**Installation:** None required. No `libs.versions.toml` change for this phase.

## Package Legitimacy Audit

**Not applicable — this phase installs no external packages.** All libraries used are already declared in `gradle/libs.versions.toml` and verified in Phase 1–3. The slopcheck gate is unnecessary because there is nothing new to install. If the planner later decides to add a helper library (not recommended — nothing here needs one), it must run the Package Legitimacy Gate before adding it.

## Architecture Patterns

### System Architecture Diagram

```
                    ┌─────────────────────────────────────────────┐
   tap "Hint" ─────▶│ GameViewModel.onHintRequested()             │
                    │  1. adGateway.requestRewarded(HINT) ─────────┼──▶ AdGateway (stub: true)
                    │  2. set uiState.solverThinking = true        │
                    │  3. launch on viewModelScope:                │
                    │       withContext(defaultDispatcher){        │
                    │         GetHintUseCase(board, solution,      │
                    │            movesPlayed)                      │
                    │       }                                      │
                    └───────────────┬─────────────────────────────┘
                                    │
              ┌─────────────────────▼──────────────────────────────┐
              │ GetHintUseCase (:domain, pure)                      │
              │                                                     │
              │  Challenge & still on recorded line?                │
              │    └─ YES ─▶ next recorded Move (instant) ──────┐   │
              │    └─ NO / Classic ─▶ Solver.firstWinningMove( )│   │
              │                          board, budget, cancel  │   │
              │                              │                  │   │
              │              ┌───────────────▼──────────────┐   │   │
              │              │ Solver (:domain)              │   │   │
              │              │  pagoda fast-reject ──▶ null  │   │   │
              │              │  memoized symmetry-canonical  │   │   │
              │              │  DFS over RulesEngine.legal-  │   │   │
              │              │  Moves/applyMove, node+time   │   │   │
              │              │  budgeted, cooperative cancel │   │   │
              │              └───────────────┬──────────────┘   │   │
              │                        Move? / null              │   │
              └─────────────────────────────┬────────────────────┘   │
                                            │◀───────────────────────┘
                    ┌───────────────────────▼─────────────────────┐
   result Move ────▶│ GameViewModel updates GameUiState:           │
                    │   Move  ⇒ hintSource=A, hintTarget=B,        │
                    │            solverThinking=false              │
                    │   null  ⇒ noWinningMove=true (D-08 banner),  │
                    │            solverThinking=false              │
                    └───────────────────────┬─────────────────────┘
                                            │ state down
                    ┌───────────────────────▼─────────────────────┐
                    │ BoardCanvas: glow A + ring B + A→B arrow     │
                    │ GameScreen:  thinking spinner / no-win banner│
                    └──────────────────────────────────────────────┘

   tap "Undo" ─────▶ GameViewModel.onUndo(): moveHistory.removeLast();
                     board = replay(initialBoard, moveHistory);
                     recompute status (clears Stuck/no-win); timer UNTOUCHED (D-07)
```

### Recommended Project Structure (additions only)
```
domain/src/main/kotlin/.../domain/
├─ solver/
│   ├─ Solver.kt            # firstWinningMove(board, budget, shouldCancel): Move?
│   ├─ SolverBudget.kt      # maxNodes + optional deadline; scoped constants
│   └─ Pagoda.kt            # necessary-condition fast-reject (optional but recommended)
└─ usecase/
    └─ GetHintUseCase.kt    # recorded-line-first → Solver → null (pure, injectable)

app/src/main/java/.../
├─ platform/ads/
│   ├─ AdGateway.kt         # interface: requestRewarded(reason): Boolean; showInterstitial()
│   ├─ AdReason.kt          # enum: HINT, UNDO (RETRY reserved for Slice 2)
│   └─ NoOpAdGateway.kt     # auto-grant stub (returns true)
├─ di/
│   └─ PlatformModule.kt    # @Binds AdGateway -> NoOpAdGateway   (or add to Domain/DataModule)
└─ ui/game/
    ├─ HintControl.kt       # new: winning-move Hint action (+ thinking spinner)
    ├─ UndoControl.kt       # new: undo action
    ├─ NoWinBanner.kt       # inline non-blocking banner (D-08/D-09)
    └─ (rename) HintToggleControl -> "Show possible moves" label/strings (D-02)
```
Matches the spec §8.5 package layout (`domain/solver`, `domain/usecase`, `platform/ads`). `[CITED: docs/superpowers/specs/2026-06-28-peg-solitaire-design.md §8.5]`

### Pattern 1: Production Solver — port and harden `AuthoringSolver` (HIGHEST PRIORITY)

**What:** A pure-`:domain` object `Solver` exposing `fun firstWinningMove(board: Board, budget: SolverBudget, shouldCancel: () -> Boolean = { false }): Move?` — returns the first move of *some* winning (reduce-to-one-peg) line from `board`, or `null` if none exists within budget / genuinely unsolvable.

**Why port, not rebuild:** `AuthoringSolver.forwardSolve` is already a *correct memoized DFS with symmetry reduction* proven to solve English-33, European-37, Asymmetric-39 and Wiegleb-45. Reuse verbatim:
- `VisitedSet` — the open-addressed `long[]` memo (~16 bytes/entry) that makes tens of millions of positions fit in heap. **Do not use `HashSet<Long>`** (the AuthoringSolver doc explains it boxes to ~72 bytes/entry and OOMs).
- `symmetryPermutations(spec)` + `canonical(pegs, symmetries)` — collapse symmetric positions onto one memo key (up to 8× reduction; "the difference between intractable and tractable for Wiegleb-45").
- `RulesEngine.legalMoves` / `applyMove` / (implicitly) game-over as the move authority — **reuse, never reimplement rules** (Anti-Pattern below).

**Four required changes vs the authoring version:**
1. **Arbitrary start, first-move return.** Accept any mid-game `Board` (not just a single-vacancy start) and return `path.first()` (the first move) rather than the whole list. The recursion already threads a path; return the head.
2. **Node budget.** Replace `MAX_DISTINCT_PER_ATTEMPT` (25M, authoring-tuned) with an injected `SolverBudget.maxNodes`. When `seen.size >= maxNodes`, stop expanding and treat as "not found within budget" → `null` (D-04). Track whether the budget was actually hit vs the subtree was fully exhausted (see the correctness note — this distinction matters for the completeness argument, not for the returned value).
3. **Cooperative cancellation + time budget.** Check `shouldCancel()` (wired to `coroutineContext.isActive` / a deadline) every N node expansions (e.g. every 4096) so a coroutine on `Dispatchers.Default` can abandon the search promptly. **A tight recursive loop never suspends, so structured-cancellation alone will NOT stop it** — you must poll cooperatively. `[VERIFIED: kotlinx.coroutines cancellation semantics — cooperative; CPU-bound loops must check isActive/ensureActive]`
4. **Drop authoring-only machinery.** Remove `MAX_RESTARTS`, `INWARD_JITTER`, seeded restarts. For a *first-move* query you want one deterministic search. **Keep** the inward move-ordering heuristic (`inwardness`) as a pure ordering — it makes solvable positions surface a solution early (cheap win) — but with no random jitter so the Solver stays deterministic and pure (PITFALLS determinism note).

**Correctness note that drives the budget (critical):** Returning a move is fast when one exists (heuristic surfaces it early, then the memo prevents re-exploration). But **returning `null` correctly (proving unsolvable) requires exhausting the entire reachable forward subtree.** That exhaustive traversal is the budget's worst case. Because every jump removes exactly one peg, the forward-reachable set is monotone-shrinking and bounded; the full-board Wiegleb-45 solve memoized ~4.5M canonical positions to *first* solution, so an exhaustive proof-of-unsolvability from a *mid-game* Wiegleb position (fewer pegs, smaller subtree) is in a comparable-or-smaller scale. English-33 has 23,475,688 reachable positions total (from center-vacant), ~2.9M after 8× symmetry folding — a safe upper bound for an English-33 exhaustive search. `[CITED: en.wikipedia.org/wiki/Peg_solitaire — 23,475,688 reachable board positions]`

**Example (structure, derived from AuthoringSolver):**
```kotlin
// Source: derived from domain/src/test/.../golden/solutions/AuthoringSolver.kt
object Solver {
    fun firstWinningMove(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean = { false },
    ): Move? {
        if (board.pegCount == SOLVED_PEG_COUNT) return null            // already solved: no move to give
        if (!Pagoda.mayBeSolvable(board)) return null                  // fast necessary-condition reject
        val symmetries = symmetryPermutations(board.spec)
        val seen = VisitedSet()
        var counter = 0

        fun solvable(b: Board): Boolean {
            if (b.pegCount == SOLVED_PEG_COUNT) return true
            if (seen.size >= budget.maxNodes) return false             // budget miss ⇒ treat as unsolvable (D-04)
            if ((counter++ and CANCEL_MASK) == 0 && shouldCancel()) return false
            if (!seen.add(canonical(b.pegs, symmetries))) return false  // already explored this class
            for (m in RulesEngine.legalMoves(b).sortedByDescending { inwardness(it, center) }) {
                if (solvable(RulesEngine.applyMove(b, m))) return true
            }
            return false
        }

        return RulesEngine.legalMoves(board)
            .sortedByDescending { inwardness(it, center) }
            .firstOrNull { solvable(RulesEngine.applyMove(board, it)) }
    }
}
```
(The planner should decide whether to fold the top-level move loop into the recursion; the shape above returns the first move whose resulting position is solvable. `center`/`inwardness`/`canonical`/`symmetryPermutations`/`VisitedSet` port from AuthoringSolver.)

**Budget starting values + measurement mandate (D-04):** The plan MUST include a *measurement* task, not a guessed constant. Recommended methodology:
- **Starting values (to be validated, not shipped blind):** `maxNodes ≈ 40_000_000` canonical positions and a soft `deadline` of ~1500 ms. Rationale: comfortably > the ~4.5M observed Wiegleb-45 full solve and > ~2.9M symmetry-folded English-33 total, with ~8–10× headroom for mid-game asymmetry (broken symmetry reduces the fold factor).
- **Validation task:** for each one-peg-solvable Classic footprint (English-33, European-37, Asymmetric-39, Wiegleb-45), generate a corpus of reachable mid-game states by replaying the committed golden solution to every prefix length (and a few off-line deviations that remain solvable), then assert `firstWinningMove != null` for every solvable sample and record max `seen.size` and wall-time. Separately, construct a few *known-unsolvable* mid-game states (e.g. isolated-peg positions, or a position failing pagoda) and assert `firstWinningMove == null` and record the exhaustive node count. Set `maxNodes = safetyFactor × observed_max_exhaustive` (recommend `safetyFactor ≥ 4`).
- **"Essentially never" operationalized:** define it as *"every solvable state reachable by legal play from any shipped Classic board's canonical start returns a move well within budget, verified over the golden-solution-prefix corpus (100% pass), with ≥4× node headroom to the largest observed exhaustive search."* The plan should state this as an explicit acceptance gate.
- **Pagoda fast-reject (recommended, `Pagoda.kt`):** peg-solitaire pagoda (Conway resource-count) functions give a cheap *necessary* condition — if a weighted sum over occupied cells drops below the target's weight, the position is provably unsolvable and the Solver returns `null` in O(cells) without any search. This makes the common "player blundered into a dead position" case instant and shrinks the exhaustive worst case. The existing GF(4)/colour invariant used for Diamond-41 (see `SolutionFixtures` doc in AuthoringSolver.kt) is the same family of position-invariant necessary conditions. `[CITED: en.wikipedia.org/wiki/Peg_solitaire — pagoda functions / resource count]` `[VERIFIED: codebase — AuthoringSolver.kt documents the GF(4) colour invariant]`

**Purity guard:** `Solver`, `SolverBudget`, `Pagoda`, `GetHintUseCase` all live in `:domain` main and must have zero `android.*`/`androidx.*` deps — `DomainPurityTest` (ArchUnit, reads bytecode) will fail the build otherwise. No `kotlinx.coroutines` import in the Solver itself; cancellation enters via the plain `shouldCancel: () -> Boolean` lambda, keeping the domain Android- and coroutine-free. `[VERIFIED: codebase — DomainPurityTest.kt asserts no android../androidx.. in ..domain..]`

### Pattern 2: State-relative hint pipeline (recorded-line → Solver → no-win)

**What:** A thin `GetHintUseCase` in `:domain` (recommended over inlining in the VM — matches spec §10, keeps the VM lean, is unit-testable without Robolectric):
```kotlin
// GetHintUseCase(board, solution, movesPlayed): Move?  (pure)
// 1. Challenge + still on recorded line?  →  return solution[movesPlayed]
// 2. otherwise (Classic, or deviated)     →  Solver.firstWinningMove(board, budget, shouldCancel)
```
**When to use:** every "Hint" tap. The recorded-line branch is instant and guaranteed; the Solver branch is the off-thread budgeted fallback (D-03/D-04).

**The "still on the recorded line" test — do NOT blind-index `solution[moveCount]`.** The CONTEXT is explicit that blind `solution[moveCount]` indexing is forbidden. The robust, cheap check: the player is on the recorded line iff **the current board equals the board produced by replaying the first `movesPlayed` recorded moves from `initialBoard`.** Because `Board` is a `data class` over a `Long` with free structural `equals` `[VERIFIED: codebase — Board.kt data class over Long pegs]`, this is one equality check against a replay (or, cheaper: track a `boolean onRecordedLine` that starts true and flips false the first time a *played* move differs from `solution[moveCount]`, and can never flip back — deviation is permanent for the recorded line). Recommend the tracked-boolean approach in `GameViewModel`: compare each committed move in `performJump` against `solution?.getOrNull(moveHistory.size - 1)`; once a mismatch occurs, set `onRecordedLine = false`. Undo must recompute this flag (Undo can return you *to* the recorded line if you undo your deviation — recompute by comparing `moveHistory` prefix against `solution`).

**Tradeoff — VM vs UseCase:** `GetHintUseCase` in `:domain` wins on testability (pure JVM, no Robolectric) and matches the spec's documented use-case list. The VM still owns the *threading* (launch on `defaultDispatcher`, `solverThinking` flag, cancellation) because coroutine scoping is a UI concern. **Recommendation: `GetHintUseCase` for the pure decision + Solver call; `GameViewModel` for the async orchestration and state writes.**

### Pattern 3: Undo — replay-from-initialBoard over `moveHistory`

**What:** `onUndo()` pops the last `Move` off `moveHistory` and rebuilds the board by folding `RulesEngine.applyMove` over the remaining history from `initialBoard`.

**Snapshot-per-move vs replay-from-initial — recommendation: replay-from-initial.**
| Approach | Pros | Cons |
|----------|------|------|
| **Replay from `initialBoard`** (recommended) | Zero new stored state — `moveHistory` already exists; trivially correct; O(historyLen) per undo is negligible (boards ≤ ~45 moves); reuses the exact `applyMove` authority | Recomputes each time (irrelevant at this scale) |
| Snapshot stack (push board per move) | O(1) undo | Adds a parallel `List<Board>` to keep in sync with `moveHistory`; more state to get wrong; no measurable benefit here |

Boards fold in tens of moves; replay is instant and eliminates a whole class of desync bugs. `initialBoard` is already retained in the VM `[VERIFIED: codebase — GameViewModel.initialBoard]`.

**Undo must (per D-05/D-06/D-07):** be repeatable back to the empty-history initial board; clear `selected`/`legalTargets`/`animating`/hint-highlight/`noWinningMove`; recompute `status` via `statusFor(board)` (which will flip Stuck→Playing once a move is undone into a playable position — the rescue); recompute the `onRecordedLine` flag; and **not touch `startMark`, `tickerJob`, or `elapsedSeconds`** (timer keeps running, D-07). Undo should be a no-op (disabled) when `moveHistory.isEmpty()`.

### Pattern 4: Stuck-rescue state-machine change (D-06) — gate the one-shot `finished` emission

**What:** Today `performJump` fires the one-shot `finished` Channel on *any* terminal status (Won OR Stuck), and `finishedEmitted` guards re-fire; `GameRoute` navigates to Results on that emission `[VERIFIED: codebase — GameViewModel.performJump lines 271–287; GameRoute LaunchedEffect]`. D-06 requires: **Won still navigates to Results; Stuck (and the Solver-confirmed no-win) must NOT navigate — it shows an in-board rescue state (Undo · Retry · Home) so Undo can rescue.**

**Cleanest change:** narrow the emission condition from `status != GameStatus.Playing` to `status == GameStatus.Won`. On `status == GameStatus.Stuck`, instead of emitting `finished`, set a `GameUiState` rescue flag (e.g. reuse/extend the no-win banner — Stuck and no-win share the same D-08/D-09 in-board terminal UI: banner + Undo/Restart/Home). Because `finished` no longer fires for Stuck, the timer-stop that currently rides inside the emission block must move: **stop the ticker whenever `status != Playing`** (both Won and Stuck freeze the timer), but only *emit finished* for Won. Undo out of a Stuck state must clear the rescue flag and (per D-07) leave the timer as-is — note the timer was stopped at Stuck; undoing into a Playing position should *resume* ticking (re-launch the ticker) since real time keeps counting. The planner should decide resume-ticker semantics explicitly and cover it in a test.

**Interaction with `finishedEmitted`:** keep the guard for Won (still one-shot). Stuck no longer sets it, so a later Won (not reachable after Stuck without Undo, but reachable *after Undo* then re-solving) still emits correctly. Reset `finishedEmitted=false` on Undo-to-playable and on Restart (Restart already resets it `[VERIFIED: codebase — resetToInitial sets finishedEmitted=false]`).

**This is the trickiest state interaction in the phase** — call it out for careful VM test coverage (Stuck→Undo→Playing→re-solve→Won→Results; and no-win→Undo→Playing).

### Pattern 5: AdGateway seam (D-11)

**What:** A `platform/ads` interface + no-op auto-grant stub, Hilt-bound so Slice 2 swaps only the binding.
```kotlin
// platform/ads/AdGateway.kt
interface AdGateway {
    suspend fun requestRewarded(reason: AdReason): Boolean   // Slice 1 stub: always true
    fun showInterstitial()                                    // Slice 1 stub: no-op
}
// platform/ads/AdReason.kt
enum class AdReason { HINT, UNDO }   // RETRY reserved for Slice 2 (interstitials)
// platform/ads/NoOpAdGateway.kt
class NoOpAdGateway @Inject constructor() : AdGateway {
    override suspend fun requestRewarded(reason: AdReason) = true
    override fun showInterstitial() = Unit
}
```
Bind via `@Binds` in a Hilt module (new `PlatformModule` or fold into `DataModule`) exactly like the existing repository bindings `[VERIFIED: codebase — DataModule @Binds pattern]`. `GameViewModel` requests a grant before performing a hint/undo: `if (adGateway.requestRewarded(AdReason.HINT)) { …run hint… }`. Signature note: `requestRewarded` should be `suspend` (Slice 2 shows an ad and awaits the reward) — the stub returns immediately; the VM already runs in `viewModelScope`. Spec §8.4/§9 documents `requestRewarded(reason): Boolean` + `showInterstitial()`. `[CITED: spec §8.4, §9]`

### Pattern 6: Compose highlight / thinking / banner (state-hoisted)

**New `GameUiState` fields** (all `val`, keep `@Immutable`): `hintSource: Position?`, `hintTarget: Position?` (or a small `HintHighlight(source, target)` value), `solverThinking: Boolean = false`, `noWinningMove: Boolean = false`, and a Stuck/no-win rescue flag (can be unified — see Pattern 4). Follow the existing field-doc style. `[VERIFIED: codebase — GameUiState.kt @Immutable data class]`

- **Source+dest highlight (D-01):** extend `DrawScope.drawBoard`/`drawCell` in `BoardCanvas` — draw a glow ring around `hintSource` (reuse the accent-ring style already used for `selected`), a target ring around `hintTarget` (reuse the destination-dot / hole-outline styling), and a faint `Path` line/arrow from `geometry.center(source)` to `geometry.center(target)`. All new visual magnitudes are **ratios in `BoardMetrics`** (unitless × cellSize) and colors from `MaterialTheme.colorScheme` — never inline literals (QUAL-03). `[VERIFIED: codebase — BoardMetrics ratios + BoardPalette color capture]`
- **Thinking indicator (D-10):** the Hint control shows a small `CircularProgressIndicator` (or disabled tint) while `solverThinking`; disable the button to prevent double-taps. Mirror `RestartControl`/`HintToggleControl` (icon `IconButton`, `top_bar_icon_size` dimen, `stringResource` content description). `[VERIFIED: codebase — RestartControl.kt / HintToggleControl.kt]`
- **No-win / stuck banner (D-08/D-09):** an inline, non-blocking banner over the still-interactive board (NOT an `AlertDialog`) with text "No winning move — undo or restart" and Undo · Restart · Home actions. New `NoWinBanner.kt` stateless leaf; strings in `strings.xml`, dimens in `dimens.xml`.
- **Rename (D-02):** change the `HintToggleControl` strings from `hints_on`/`hints_off` to a "Show possible moves" phrasing (e.g. `show_moves_on`/`show_moves_off`); the toggle stays a pure render gate for the legal-destination dots. The new winning-move action is the separate `HintControl`. `[VERIFIED: codebase — strings.xml hints_on/hints_off + HintToggleControl]`

All controls stay **stateless leaves** with hoisted state/handlers per `compose-skill` (installed at `~/.claude/skills/compose-skill`) and the project's Route/Screen/Leaf convention. `[VERIFIED: filesystem — ~/.claude/skills/compose-skill exists]`

### Anti-Patterns to Avoid
- **Reimplementing rules inside the Solver.** The Solver MUST call `RulesEngine.legalMoves`/`applyMove` — the same authority gameplay uses. Duplicating jump logic risks the Solver "solving" a position gameplay can't reach. `[VERIFIED: codebase — RulesEngine is the single move authority; AuthoringSolver already uses it]`
- **Blind `solution[moveCount]` indexing.** Explicitly forbidden by CONTEXT — index only when the current board is verified to still match the recorded line (Pattern 2).
- **Relying on structured cancellation to stop the Solver.** A CPU-bound recursion never suspends; without a cooperative `shouldCancel()` poll it runs to completion even after the coroutine is cancelled. `[VERIFIED: kotlinx.coroutines cooperative-cancellation semantics]`
- **`HashSet<Long>` for the memo.** Boxes to ~72 B/entry and OOMs the search — use the ported `VisitedSet` `long[]`. `[VERIFIED: codebase — AuthoringSolver VisitedSet doc]`
- **Auto-performing the hinted jump.** D-01: highlight only; the player executes the move.
- **Navigating to Results on Stuck.** D-06 replaces that with the in-board rescue state.
- **Applying a score/time penalty on Undo.** D-07: board + history only.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Winning-move search | A fresh DFS/solver from scratch | Port `AuthoringSolver` (memo + symmetry + inward heuristic) | It already solves all four one-peg boards incl. 9×9 Wiegleb; correct + tuned `[VERIFIED: codebase]` |
| Memoization set | `HashSet<Long>` | Ported `VisitedSet` (`long[]` open addressing) | 16 vs 72 bytes/entry — tens of millions of states fit in heap `[VERIFIED: codebase]` |
| Symmetry reduction | Bespoke transform math | `symmetryPermutations` + `canonical` from AuthoringSolver | Up to 8× state reduction, already proven `[VERIFIED: codebase]` |
| Legal moves / apply | New jump code in the Solver | `RulesEngine.legalMoves`/`applyMove` | Single rules authority; deterministic sorted enumeration `[VERIFIED: codebase]` |
| Board reconstruction for Undo | Parallel snapshot stack | Replay `applyMove` over `moveHistory` from `initialBoard` | `moveHistory` + `initialBoard` already exist; no desync `[VERIFIED: codebase]` |
| Prove a position unsolvable cheaply | Full exhaustive search every time | Pagoda / resource-count necessary condition first | O(cells) reject of dead positions; shrinks worst case `[CITED: Wikipedia peg solitaire]` |
| One-shot navigation | New event bus | Existing `finished` `Channel` (narrow its condition to Won) | Pattern already in place `[VERIFIED: codebase]` |

**Key insight:** the hardest part of this phase already exists as tested code in the repo's test source set. The engineering is *porting it to production with a budget and a cancel hook*, not inventing a solver.

## Common Pitfalls

### Pitfall 1: Budget too low ⇒ false "no winning move" on a solvable Classic board (D-04 core risk)
**What goes wrong:** Classic is Solver-only; a budget miss reads as "unsolvable." Too-low `maxNodes`/deadline makes a genuinely-solvable Wiegleb-45 mid-game position report "no winning move" — directly breaks the game's fairness promise.
**Why it happens:** guessing a constant instead of measuring; underestimating that broken mid-game symmetry reduces the fold factor and grows the search.
**How to avoid:** the measurement task in Pattern 1 — sample real solvable mid-game states from golden-solution prefixes across all footprints, size `maxNodes ≥ 4× observed exhaustive max`, assert 100% return-a-move on solvable samples in a JVM test.
**Warning signs:** any solvable sample state returning `null`; observed `seen.size` approaching `maxNodes` on a solvable position.

### Pitfall 2: Solver keeps running after the coroutine is cancelled
**What goes wrong:** player taps Hint, then navigates away / taps again; the abandoned search still burns a CPU core.
**Why it happens:** structured cancellation can't interrupt a non-suspending recursion.
**How to avoid:** poll `shouldCancel()` (wired to `isActive` and/or a deadline) every ~4096 node expansions; cancel the prior hint `Job` before launching a new one; disable the Hint button while `solverThinking` (D-10).
**Warning signs:** UI jank / battery drain after leaving gameplay; overlapping searches.

### Pitfall 3: Undo desyncs the timer, hint flag, or `finishedEmitted`
**What goes wrong:** Undo clears the board but leaves a stale `noWinningMove` banner, stale hint highlight, a stuck `finishedEmitted=true` (blocking a later Won emission), or wrongly restarts the timer.
**Why it happens:** partial state reset.
**How to avoid:** a single `onUndo()` that rebuilds the *whole* `GameUiState` from the replayed board — clear `selected`/`legalTargets`/`animating`/`hintSource`/`hintTarget`/`noWinningMove`/rescue flag, recompute `status` and `onRecordedLine`, reset `finishedEmitted` when returning to Playing, and leave timer fields untouched (resume ticker if Stuck→Playing). Cover with a Turbine VM test.
**Warning signs:** banner persists after Undo; timer jumps; Won never navigates after an Undo-then-resolve.

### Pitfall 4: Domain purity break pulls Android/coroutines into `:domain`
**What goes wrong:** importing `kotlinx.coroutines` or an Android type into `Solver`/`GetHintUseCase` fails `DomainPurityTest` and the build.
**Why it happens:** reaching for `isActive`/`ensureActive` inside the domain for cancellation.
**How to avoid:** cancellation crosses the boundary as a plain `shouldCancel: () -> Boolean`; the VM (in `:app`) constructs that lambda from `coroutineContext`. Keep the Solver a pure function of `(Board, budget, lambda)`.
**Warning signs:** `DomainPurityTest` red; an `androidx`/`kotlinx.coroutines` import in a `domain/solver` or `domain/usecase` file.

### Pitfall 5: Recorded-line hint given after deviation (stale index)
**What goes wrong:** player deviated, but the hint still returns `solution[moveCount]` — an illegal or non-winning move.
**Why it happens:** blind indexing without the on-line check.
**How to avoid:** the tracked `onRecordedLine` boolean (Pattern 2), recomputed on Undo; only use the recorded move when `onRecordedLine && solution has a next move`.
**Warning signs:** hint highlights a move whose `from` cell has no peg, or a Solver test shows the recorded move isn't in `legalMoves(currentBoard)`.

## Runtime State Inventory

Not applicable — this is a **greenfield feature phase**, not a rename/refactor/migration. No stored data, live-service config, OS-registered state, secrets, or build artifacts are renamed or migrated. The only mutable runtime state introduced (`moveHistory`, hint/thinking/no-win flags) is **in-memory only** (persistence of move history is explicitly Phase 5 / PERS-03, per the Deferred section). Verified: no DataStore/Room writes are added this phase (grep of Deferred + CONTEXT confirms in-memory-only scope).

## Code Examples

### Cooperative cancellation from the ViewModel (the coroutine ↔ pure-domain seam)
```kotlin
// Source: derived from GameViewModel (defaultDispatcher already injected) + kotlinx.coroutines
private var hintJob: Job? = null

fun onHintRequested() {
    hintJob?.cancel()                                   // abandon any prior search
    _uiState.update { it.copy(solverThinking = true, noWinningMove = false) }
    hintJob = viewModelScope.launch {
        if (!adGateway.requestRewarded(AdReason.HINT)) return@launch   // Slice-1 stub: true
        val deadline = timeSource.markNow() + SolverBudget.DEADLINE
        val move = withContext(defaultDispatcher) {
            getHintUseCase(
                board = _uiState.value.board,
                solution = solution.takeIf { onRecordedLine },
                movesPlayed = moveHistory.size,
                shouldCancel = { !isActive || deadline.hasPassedNow() },  // pure lambda, no domain coroutine dep
            )
        }
        _uiState.update {
            if (move != null) it.copy(hintSource = move.from, hintTarget = move.to, solverThinking = false)
            else it.copy(noWinningMove = true, solverThinking = false)   // D-04 + D-08
        }
    }
}
```

### Undo via replay (timer untouched — D-07)
```kotlin
// Source: derived from GameViewModel.moveHistory + RulesEngine
fun onUndo() {
    if (moveHistory.isEmpty()) return
    moveHistory.removeAt(moveHistory.lastIndex)
    val base = initialBoard ?: return
    val board = moveHistory.fold(base) { b, m -> rulesEngine.applyMove(b, m) }
    onRecordedLine = solution?.let { s ->
        moveHistory.indices.all { i -> moveHistory[i] == s.getOrNull(i) }
    } ?: false
    val status = statusFor(board)
    if (status == GameStatus.Playing) finishedEmitted = false          // allow a future Won emission
    // NOTE: startMark / elapsedSeconds / ticker deliberately untouched (D-07);
    //       resume the ticker if we came back from Stuck→Playing.
    _uiState.update {
        it.copy(
            board = board, selected = null, legalTargets = emptyList(), animating = null,
            hintSource = null, hintTarget = null, noWinningMove = false, status = status,
        )
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Phase-3 D-10: Stuck → Results → Retry | Stuck → in-board rescue (Undo · Retry · Home), no auto-nav | This phase (D-06) | Narrow the `finished` emission to Won only |
| Legal-dots toggle labeled "Hints" | Toggle relabeled "Show possible moves"; separate "Hint" action | This phase (D-02) | String/label change + new control |
| `Solver` exists only as test-time `AuthoringSolver` | Production `Solver` in `:domain` main, budgeted + cancellable | This phase | New pure-domain component + purity guard |

**Deprecated/outdated:** none — no library churn this phase.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `maxNodes ≈ 40M` / `deadline ≈ 1.5s` are safe starting points | Pattern 1 | These are *starting* values explicitly gated behind the measurement task; if the measured exhaustive max is higher, bump `maxNodes`. Low risk because the plan mandates measurement, not blind shipping. |
| A2 | Mid-game exhaustive search ≤ full-board authoring solve scale (peg count monotone-decreasing bounds the forward set) | Pattern 1 | If a pathological mid-game position exhausts more than the full solve, a solvable board could false-negative. Mitigated by the ≥4× safety factor and the pagoda fast-reject; the measurement corpus should include worst-case-looking states. |
| A3 | A tracked `onRecordedLine` boolean correctly captures "still on the recorded line" | Pattern 2 | If two distinct move orders reach the same board, strict move-equality could mark "deviated" when the board still matches the line — a false deviation only costs a Solver call (still correct), never a wrong hint. Board-equality check is the stricter fallback if needed. |
| A4 | Pagoda/resource-count necessary conditions apply to all shipped footprints (incl. freeform Challenge shapes) | Pattern 1 | Pagoda is board-agnostic (weights per cell); worst case it proves nothing and the search runs — never produces a wrong answer, only a missed fast-reject. Low risk. |
| A5 | `requestRewarded` should be `suspend` | Pattern 5 | If the planner prefers a non-suspending stub, trivial to change; Slice 2 (real ad await) favors `suspend`. Cosmetic. |

## Open Questions (RESOLVED)

1. **Should the timer resume ticking after Undo rescues a Stuck board?**
   - What we know: D-07 says Undo doesn't touch the timer; the ticker currently stops at any terminal status.
   - What's unclear: after Stuck→Undo→Playing, does elapsed keep counting (real time passed) or stay frozen?
   - Recommendation: **resume** (real time is still passing; consistent with D-07's "undoing costs you time"). Planner should lock this and test it.
   - **RESOLVED:** locked to **resume** — implemented in 04-04 Task 2 (ticker relaunch factored out of `startTimerIfNeeded`; `onUndo` resumes the ticker on Stuck→Playing without nulling `startMark`) and covered by the ticker-resume-after-rescue-undo VM test.

2. **Does the "no winning move" banner and the "Stuck" rescue state share one UI, or are they distinct?**
   - What we know: both offer Undo · Restart · Home over the live board (D-06/D-08/D-09).
   - What's unclear: whether the copy differs ("no winning move…" vs a stuck message).
   - Recommendation: **one shared rescue banner component**, parameterized by message string; Stuck and Solver-confirmed-no-win both use it. Fewer states, less test surface.
   - **RESOLVED:** one shared `NoWinBanner(message, onUndo, onRestart, onHome)` — built in 04-04 Task 1 and driven by the unified rescue flag in 04-04 Task 2/3 for both Stuck and Solver-confirmed no-win.

3. **Where does "Home" navigate from the rescue banner (D-09)?**
   - What we know: Home exits the game.
   - What's unclear: exact nav target (mode-select vs Home root) — a routing detail.
   - Recommendation: route to Home root, consistent with existing back-nav; confirm against `AppNavHost`.
   - **RESOLVED:** routes to **Home root** via `onHome = onNavigateBack` — wired in 04-04 Task 3 (GameScreen `onHome` param + GameRoute).

## Environment Availability

Skipped — no new external tools/services/runtimes. The phase is pure Kotlin/Compose using the existing Gradle/JDK/Android toolchain and the already-declared dependency set from Phases 1–3. No CLI utility, database, or service is introduced.

## Validation Architecture

Nyquist validation is **enabled** (`workflow.nyquist_validation: true` in `.planning/config.json`) `[VERIFIED: codebase — .planning/config.json]`.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 (JVM unit + Robolectric); Turbine 1.2.1 (Flow); Truth 1.4.x; MockK 1.14.x; Roborazzi (screenshot) |
| Config file | Gradle version catalog `gradle/libs.versions.toml`; per-module `build.gradle.kts` (existing) |
| Quick run command | `./gradlew :domain:test` (pure Solver/hint-usecase tests — fast, no Android) |
| Full suite command | `./gradlew check` (includes `:app:testDebugUnitTest`, `DomainPurityTest`, detekt, ktlint) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ASST-01 | Recorded-line hint returns `solution[moveCount]` while on line (Challenge) | unit (JVM) | `./gradlew :domain:test --tests '*GetHintUseCaseTest*'` | ❌ Wave 0 |
| ASST-01 | Hint highlight fields set on `GameUiState`; Won never offers Undo | unit (Robolectric+Turbine) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | ✅ extend |
| ASST-03 | Solver returns a legal, win-preserving first move from solvable mid-game states (all footprints, golden-prefix corpus) | unit (JVM) | `./gradlew :domain:test --tests '*SolverTest*'` | ❌ Wave 0 |
| ASST-03 | Solver returns `null` on genuinely-unsolvable positions; pagoda fast-reject | unit (JVM) | `./gradlew :domain:test --tests '*SolverTest*'` | ❌ Wave 0 |
| ASST-03 (D-04) | Budget headroom: 100% of solvable golden-prefix samples return a move; record max nodes/time | unit (JVM, measurement) | `./gradlew :domain:test --tests '*SolverBudgetTest*'` | ❌ Wave 0 |
| ASST-03 (D-06) | Stuck shows rescue (no `finished` emission); Won still emits | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | ✅ extend |
| ASST-02 | Undo restores exact prior board; multi-step to start; timer untouched | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | ✅ extend |
| ASST-02/03 | Solver runs off-thread; cancellation stops it; thinking flag toggles | unit (Turbine + StandardTestDispatcher) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | ✅ extend |
| ASST-01/03 | Highlight overlay + no-win banner render; Hint disabled while thinking | Compose UI / Roborazzi | `./gradlew :app:testDebugUnitTest --tests '*BoardCanvas*'`, screenshot | ✅ extend / ❌ new banner test |
| QUAL-01 | `Solver`/`GetHintUseCase`/`Pagoda` have no android/androidx deps | ArchUnit | `./gradlew :app:testDebugUnitTest --tests '*DomainPurityTest*'` | ✅ existing (auto-covers new domain files) |

### Sampling Rate
- **Per task commit:** `./gradlew :domain:test` (Solver/usecase — sub-second, high signal)
- **Per wave merge:** `./gradlew :app:testDebugUnitTest` (VM + Compose + purity)
- **Phase gate:** `./gradlew check` green (all tests + detekt + ktlint) before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `domain/src/test/.../solver/SolverTest.kt` — correctness (returns legal win-preserving move on solvable states; `null` on unsolvable) — ASST-03
- [ ] `domain/src/test/.../solver/SolverBudgetTest.kt` — the D-04 measurement/headroom corpus over golden-solution prefixes for English-33/European-37/Asymmetric-39/Wiegleb-45 — ASST-03/D-04
- [ ] `domain/src/test/.../solver/PagodaTest.kt` — necessary-condition rejects known-dead positions, never rejects a solvable one — ASST-03
- [ ] `domain/src/test/.../usecase/GetHintUseCaseTest.kt` — recorded-line-first vs Solver fallback vs null routing (pure, no Robolectric) — ASST-01/03
- [ ] Extend `app/.../ui/game/GameViewModelTest.kt` — undo (multi-step, timer untouched, finishedEmitted reset), stuck-rescue gating (Won emits / Stuck doesn't), thinking flag, cancellation — ASST-02/03/D-06
- [ ] New `NoWinBanner`/`HintControl` Compose semantics + Roborazzi golden(s) for highlight + banner + thinking states — ASST-01/03
- [ ] Reuse the committed golden solutions (`domain/src/test/resources/golden/solutions/*.txt`) as the Solver test corpus source

*(Framework itself needs no install — the entire test stack is already configured from Phase 1–3.)*

## Security Domain

`security_enforcement` is absent from config ⇒ **enabled**. This phase is local, offline, single-player logic with no auth, network, crypto, or persistence added — the threat surface is narrow.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — (no auth this phase) |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | yes | Untrusted taps re-derived through `RulesEngine.legalMoves` before applying (existing authority); hinted moves also validated as members of `legalMoves` before highlight; untrusted nav args already clamped (`clampedChallengeLevel`) `[VERIFIED: codebase — performJump re-derives move; GameViewModel clamps level]` |
| V6 Cryptography | no | — (no secrets; never hand-roll — none needed) |

### Known Threat Patterns for Kotlin/Compose local game
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Untrusted UI tap asserts an illegal jump | Tampering | Re-derive from `RulesEngine.legalMoves`; never trust the tapped destination (existing pattern) |
| Malformed/forged nav arg (levelId, level number) | Tampering / DoS | Repository fails fast on unknown id; level number clamped to bound O(level) work `[VERIFIED: codebase]` |
| Unbounded Solver search hangs/drains battery (self-inflicted DoS) | Denial of Service | Node budget + time deadline + cooperative cancellation + pagoda fast-reject (Pattern 1/Pitfall 2) |

## Sources

### Primary (HIGH confidence)
- Codebase (authoritative for all integration): `AuthoringSolver.kt`, `GameViewModel.kt`, `GameUiState.kt`, `RulesEngine.kt`, `Board.kt`, `Move.kt`, `GeneratedLevel.kt`, `LevelRepository.kt`, `BoardCanvas.kt`, `GameScreen.kt`, `GameRoute.kt`, `HintToggleControl.kt`, `RestartControl.kt`, `DomainModule.kt`, `DataModule.kt`, `DomainPurityTest.kt`, `BoardMetrics.kt`, `GameAnim.kt`, `.planning/config.json`
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` §6 (Hints & Undo), §8.4/§9 (AdGateway), §8.5/§10 (package structure, Solver/GetHintUseCase in domain)
- `.planning/phases/04-assist-hint-undo/04-CONTEXT.md` (D-01..D-11); `.planning/ROADMAP.md` §Phase 4; `.planning/REQUIREMENTS.md` (ASST-01..03)

### Secondary (MEDIUM confidence)
- kotlinx.coroutines cancellation semantics (cooperative; CPU-bound loops must poll `isActive`/`ensureActive`) — standard, cross-verified against project's existing `isActive` ticker usage in `GameViewModel`

### Tertiary (LOW confidence — informational, for budget sizing only)
- [en.wikipedia.org/wiki/Peg_solitaire](https://en.wikipedia.org/wiki/Peg_solitaire) — 23,475,688 reachable positions (English-33, center start); pagoda/resource-count necessary conditions
- [George Bell — English 33-hole analysis](https://www.gibell.net/pegsolitaire/English/index.html) and [jaapsch.net peg solitaire analysis](https://www.jaapsch.net/puzzles/pegsolit.htm) — state-space / solvability background

*(These figures inform the budget *starting* values only; the D-04 budget is set by the in-repo measurement task, not by these numbers.)*

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — no new deps; all libraries verified present in the codebase.
- Architecture / integration: HIGH — every integration point read directly from current source; Solver derived from shipped, working code.
- Solver budget values: MEDIUM — starting values reasoned from known state-space scale + the observed ~4.5M Wiegleb authoring solve; the plan mandates measurement to finalize (D-04).
- Pitfalls: HIGH — grounded in the actual `GameViewModel` state machine and the `DomainPurityTest` boundary.

**Research date:** 2026-07-02
**Valid until:** 2026-08-01 (stable — pure in-repo work; no fast-moving external dependency)
