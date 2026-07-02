# Phase 4: Assist — Hint & Undo - Context

**Gathered:** 2026-07-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Add two **assist affordances** to the existing playable board:

- **Hint** — reveals a genuine next **winning** move. State-relative: on the recorded winning line use `solution` directly; after deviation fall back to an off-thread, budgeted `Solver`; when the position is genuinely unsolvable surface an explicit "no winning move" state. Never blind `solution[moveCount]` indexing.
- **Undo** — reverts moves via the existing `moveHistory` stack.

Both are **free in Slice 1** through a no-op `AdGateway` auto-grant stub (gameplay requests "grant a hint/undo"; the gateway auto-grants). The Slice-2 rewarded-ad swap is out of scope — only the interface + stub are established here.

**Requirements:** ASST-01 (hint reveals next winning move), ASST-02 (undo last move), ASST-03 (deviation → Solver fallback + clear "no winning move" state).

**In scope:** production `Solver` (search, budgeted, `Dispatchers.Default`); threading the Challenge recorded `solution` into `GameViewModel`; the state-relative hint pipeline (recorded-line → Solver → no-win); multi-step Undo (incl. rescuing a Stuck board); the `AdGateway` interface + auto-grant stub bound in Hilt; and all Hint/Undo/no-win UI (button states, source+destination highlight, inline banner, "thinking" indicator). Rename the existing legal-dots toggle to disambiguate from the new Hint.

**Out of scope:** rewarded-ad gating / real AdMob impl (Slice 2); persistence/resume of move history (Phase 5); "retry between levels" interstitials (Slice 2); leaderboards; diagonal jumps (never — orthogonal-only both modes).
</domain>

<decisions>
## Implementation Decisions

### Hint presentation
- **D-01: Hint reveals source + destination.** On a found winning move, glow **both** the peg to move (A) and the target hole (B), with a faint A→B arrow. The player then taps to perform it (tap A then B, or tap to confirm). **Do NOT auto-perform the jump** — it's an assist, the player still makes the move.
- **D-02: Disambiguate from the existing "hint dots."** The current `showHints`/`HintToggleControl` (persisted legal-destination dots) is renamed to **"Show possible moves"** (or equivalently clear label). The new winning-move action is **"Hint"**. This resolves the two-things-called-"hint" collision. The dots toggle stays a render gate; "Hint" is a distinct action.

### Hint scope & fallback
- **D-03: Hint works in BOTH Classic and Challenge.** Challenge uses the recorded `solution` (instant, guaranteed) then the `Solver` on deviation; **Classic uses the `Solver` on every hint** (Classic ships no recorded line). Consistent player experience across modes.
- **D-04: Budget miss is treated as "no winning move."** Any `Solver` failure to return a move within budget surfaces the same "no winning move" state (simple, single failure path). **Planning constraint (important):** because Classic is Solver-only and a budget miss reads as "unsolvable," the `Solver`'s node/time budget MUST be tuned high enough that genuinely-solvable full-size classic positions (English-33, Wiegleb-45, etc. from arbitrary mid-game states) essentially never hit the budget — otherwise the game would falsely tell a player a solvable board is dead. Verify Solver latency/completeness on the largest classic footprints during research/planning.

### Undo behavior
- **D-05: Multi-step, repeatable Undo back to the start.** Each Undo reverts one more move; repeated taps walk all the way back to the initial board. Backed by the existing `moveHistory` list. **No redo** (out of scope).
- **D-06: Undo rescues a Stuck board.** A stuck game (2+ pegs, no legal moves) NO LONGER auto-routes straight to Results. Instead it shows an in-board terminal state offering **Undo** (back up into a playable position), plus Retry/Home. This modifies Phase-3 D-10 (which sent Stuck → Results → Retry). A **Won** (solved) game still proceeds to Results as before — the rescue is for Stuck and the unsolvable "no winning move" case, not for a win.
- **D-07: Undo does not touch the timer or apply a penalty.** Undo rewinds board + move history only; the elapsed timer keeps counting real time (score already factors elapsed time via `ScoreCalculator`). Undoing costs you time, not a separate score penalty.

### "No winning move" state & Solver feedback
- **D-08: Inline, non-blocking banner on the live board.** When the Solver confirms unsolvable (or budget-miss per D-04), show a banner over the still-visible, still-interactive board: "No winning move — undo or restart." Not a modal.
- **D-09: No-win actions = Undo · Restart · Home.** All three offered in the state (Restart reloads the same deterministic board; Home exits).
- **D-10: "Thinking…" indicator on the Hint control.** While the Solver computes off-thread, the Hint button shows a brief spinner/disabled state (prevents double-taps), then resolves to either the highlighted move (D-01) or the no-win state (D-08). Usually sub-second, so it may barely flash — that's fine.

### AdGateway seam (carried from roadmap/spec — not re-asked)
- **D-11: Establish the `AdGateway` interface + no-op auto-grant stub this phase.** Per the roadmap goal and design spec §6/§10, hint/undo are free in Slice 1 *via* the gateway: gameplay calls something like `adGateway.requestRewarded(reason): Boolean` (or a hint/undo grant), and the Slice-1 stub auto-grants (returns true). Bind the stub in a Hilt module so Slice 2 swaps only the binding — no gameplay/UI change. `showInterstitial()` is stubbed no-op.

### Claude's Discretion
- Exact `Solver` algorithm, memoization/transposition, and the concrete node/time **budget values** (subject to the D-04 completeness constraint) — implementation + measurement.
- Exact button placement/iconography for Hint and Undo in the game top bar/controls, following `compose-skill` and existing `RestartControl`/`HintToggleControl` conventions.
- The precise label for the renamed dots toggle (D-02) and whether Won ever offers Undo (default: no — Won → Results).
- Whether the recorded `solution` is threaded via `GameViewModel` reading `Level.solution` or a small use case (`GetHintUseCase`) — planner's call; behavior is fixed by D-01..D-04.
- Exact `AdGateway` method signature and reason enum (D-11).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Hint & Undo design (highest priority)
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` §6 "Hints & Undo" — the canonical hint definition (recorded-line relative to current state → `Solver` fallback → "no winning move — undo or restart"), undo = move-history stack, Slice-1 free via `AdGateway`. Also §5 (recorded winning solution used for hints), §9 platform interfaces (`AdGateway.requestRewarded(reason): Boolean`, no-op/auto-grant in Slice 1), §10 architecture (`Solver` in domain; `GetHintUseCase`; `platform/ads`).
- `.planning/ROADMAP.md` §"Phase 4: Assist — Hint & Undo" — goal (state-relative hint, off-thread budgeted Solver, explicit no-win state), success criteria 1–4, dependency on Phase 3's recorded solution + Solver.
- `.planning/REQUIREMENTS.md` — ASST-01, ASST-02, ASST-03.

### Generator / recorded-solution seam (Phase 3 output this phase consumes)
- `.planning/phases/03-challenge-generation-difficulty-curve/03-CONTEXT.md` — the `Level.solution` carry-through, `LevelRepository` routing, and D-10 (Stuck → Retry) that D-06 here modifies.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratedLevel.kt` — `solution: List<Move>` recorded winning line replayable via `RulesEngine.applyMove`.
- `.planning/research/PITFALLS.md` §"non-deterministic generation" / determinism notes — the `Solver` and any hint search must stay pure-deterministic if it ever feeds anything golden-mastered (hint fallback itself is runtime, not golden, but keep Solver pure `:domain`).

### Project constraints
- `.planning/PROJECT.md` — Core Value; `AdGateway`/`GamesService` behind interfaces; pure-Kotlin domain (Solver lives in `:domain`, no Android).
- `CLAUDE.md` — code standards (no magic numbers/strings → constants split by scope, dimens for UI, detekt+ktlint gate, state hoisting via `compose-skill`, Gitflow).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`GameViewModel.moveHistory: MutableList<Move>`** (app/.../ui/game/GameViewModel.kt) — already accumulates applied moves; Undo (D-05) pops from it and replays from `initialBoard`, or snapshots per move. The one-shot `finished` Channel + `finishedEmitted` guard and `statusFor()` terminal logic are where D-06 (stuck rescue instead of auto-navigate) must change.
- **`RulesEngine`** (`:domain`, Phase 1) — `legalMoves`/`applyMove`/`isGameOver`; the Solver reuses these; Undo replays with `applyMove`.
- **`Level.solution: List<Move>?`** (app/.../data/LevelRepository.kt) — Challenge-only recorded line, currently carried but unconsumed; `GameViewModel` today only reads `.initialBoard`. This phase threads `solution` into the ViewModel for the D-03 recorded-line hint.
- **Test-only `AuthoringSolver`** (domain/src/test/.../golden/solutions/AuthoringSolver.kt) — a working DFS solver used to author golden solutions; a reference/starting point for the **production** `Solver` (move it to `:domain` main or write a budgeted variant). Not currently in production.
- **`HintToggleControl` + `GameUiState.showHints` + `SettingsRepository.hintsEnabled`** — the existing legal-dots toggle to be RENAMED per D-02 (label/string change; it stays a render gate).
- **`RestartControl`** — pattern/placement analog for the new Hint and Undo controls.
- **`ScoreCalculator` / `elapsedSeconds` timer** — unchanged by Undo per D-07.

### Established Patterns
- Pure-Kotlin `:domain` (no Android) for the `Solver` → JVM unit tests, high coverage (search correctness + budget behavior).
- MVVM + `StateFlow<GameUiState>`, stateless composables (state hoisting); one-shot nav events via `Channel` (the `finished` hand-off to Results).
- Off-main-thread work via injected `defaultDispatcher` (already used for O(level) Challenge generation) — reuse for the budgeted Solver (D-10 "thinking" state while it runs).
- Data-driven constants split by scope, no magic numbers — Solver budget values, hint/undo strings, and dimens go in scoped constants/resources, not inline.

### Integration Points
- **`GameViewModel`** gains: hint request → (recorded-line lookup vs Solver on `defaultDispatcher`) → `GameUiState` fields for hint highlight (source+dest), a "thinking" flag, and a "no winning move" flag; undo request → pop `moveHistory` + recompute state; stuck state → in-board rescue (D-06) instead of immediate `finished`.
- **`GameUiState`** gains hint-highlight (source `Position` + target `Position`/arrow), `solverThinking: Boolean`, and `noWinningMove: Boolean` (or a sealed assist-state) fields.
- **`AdGateway`** interface (new `platform/ads` or equivalent) + no-op auto-grant stub, Hilt-bound; `GameViewModel` requests grants through it (D-11).
- **`GameScreen`/controls** gain Hint button (with thinking spinner), Undo button, source+dest highlight rendering on `BoardCanvas`, and the inline no-win banner (D-08/D-09).

</code_context>

<specifics>
## Specific Ideas

- Hint should feel like a **nudge, not autopilot** — show the move (A→B), let the player execute it (D-01).
- The two "hints" must be unmistakably different in the UI: **"Show possible moves"** (dots, passive/persisted) vs **"Hint"** (reveals the next winning move, on demand) (D-02).
- Undo is a genuine **rescue**: a player who gets stuck can back out and keep playing rather than only Retry (D-06) — this is the assist that makes the guaranteed-solvable ladder feel fair.
- The no-win message is exactly the spec's words: **"no winning move — undo or restart"** (D-08), non-blocking so the player can act immediately.

</specifics>

<deferred>
## Deferred Ideas

- **Rewarded-ad gating of hint/undo/retry + real AdMob impl** — Slice 2. Only the `AdGateway` interface + auto-grant stub are built now (D-11).
- **Redo** (re-apply an undone move) — out of scope; not requested. Undo is one-directional (D-05).
- **Persisting move history / resume with undo intact** — Phase 5 (PERS-03). This phase keeps `moveHistory` in-memory only.
- **Interstitial "retry between levels" ads** — Slice 2.
- **Hint animation polish / richer arrow visuals beyond the source+dest highlight** — can be revisited in the Slice-2 visual-polish pass (POLI-01) if desired; the functional highlight ships now.

None of the above are scope creep into Phase 4 — all are explicitly later-phase or later-slice.

</deferred>

---

*Phase: 4-assist-hint-undo*
*Context gathered: 2026-07-02*
