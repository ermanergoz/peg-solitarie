---
phase: 4
slug: assist-hint-undo
status: approved
nyquist_compliant: true
wave_0_complete: true
created: 2026-07-02
reconciled: 2026-08-29
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 (JVM unit) + kotlinx-coroutines-test + Turbine + MockK + Truth; Compose UI Test + Roborazzi (JVM/Robolectric) |
| **Config file** | Gradle (`:domain` JVM tests, `:app` unit + `testDebugUnitTest` for Robolectric) |
| **Quick run command** | `./gradlew :domain:test` |
| **Full suite command** | `./gradlew test detekt ktlintCheck` |
| **Estimated runtime** | ~60–120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (Solver/use-case tasks) or the affected module's unit tests
- **After every plan wave:** Run `./gradlew test`
- **Before `/gsd:verify-work`:** `./gradlew test detekt ktlintCheck` must be green
- **Max feedback latency:** ~120 seconds

---

## Per-Task Verification Map

> Populated 2026-08-29 by v1.1 Phase 8 plan 08-03 (DEBT-03). Solver correctness + budget
> behavior are the highest-value gates (D-04). Every row names the plan that produced it,
> the file that exists on disk, and a scoped command that was executed and exited 0 before
> this contract was signed off.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-02 | 02 | 1 | ASST-01 | T-04-CT | Solver returns a legal, win-preserving move via the same `RulesEngine` authority as gameplay; `null` only when genuinely unsolvable or already solved (goal-check-before-memo) | unit (JVM) | `./gradlew :domain:test --tests '*SolverTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTest.kt` | ✅ green |
| 04-02 ➕ | 02 | 1 | ASST-01 | T-04-SD | `SolverBudget.DEFAULT_MAX_NODES` keeps ≥4× node headroom over the largest deciding search across the tractable corpus, so a budget miss is never how a decidable board fails | budget/completeness (JVM) | `./gradlew :domain:test --tests '*SolverBudgetTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverBudgetTest.kt` | ✅ green |
| 04-02 ➕ | 02 | 1 | ASST-01 | T-04-SD | `Pagoda.mayBeSolvable` is a one-directional O(cells) fast-reject: rejects a known dead position, and never rejects any prefix of a real winning line (no false "board is dead") | property (JVM) | `./gradlew :domain:test --tests '*PagodaTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt` | ✅ green |
| 04-03 | 03 | 2 | ASST-01, ASST-03 | T-04-CT, T-04-PU | Recorded-line → Solver → `null` hint pipeline; the recorded move is returned only when in-range AND a member of `RulesEngine.legalMoves(board)` — never a blind `solution[moveCount]`; the use case stays pure `:domain` | unit (JVM) | `./gradlew :domain:test --tests '*GetHintUseCaseTest*'` | ✅ `domain/src/test/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCaseTest.kt` | ✅ green |
| 04-01 / 04-03 / 04-04 | 01, 03, 04 | 1, 2, 4 | ASST-02, ASST-03 | T-04-01-U, T-04-SD, T-04-SM | Undo rebuilds the board only by folding `RulesEngine.applyMove` over the recorded history; hint state + `solverThinking` flag + no-win flag; a repeat hint request cancels the in-flight job; the `finished` emission is narrowed to `Won` and guarded so Stuck→Undo→re-solve→Won emits exactly once | ViewModel (Robolectric + Turbine) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*'` | ✅ `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt` | ✅ green |
| 04-03b / 04-04 | 03b, 04 | 3, 4 | ASST-01, ASST-02, ASST-03 | T-04-3B-RP, T-04-3B-DT | Compose semantics: hint source+dest highlight is render-only and suppressed when non-interactive; the hint control is disabled while `solverThinking` (no overlapping searches); the inline no-win banner and the undo control expose their state | Compose semantics (Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*HintControlTest*' --tests '*NoWinBannerTest*' --tests '*UndoControlTest*'` | ✅ `app/src/test/java/com/app/pegsolitaire/ui/game/HintControlTest.kt`, `NoWinBannerTest.kt`, `UndoControlTest.kt` | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky · ➕ test that exists but the original draft contract never named*

*Plan 04-05 (wave 5) was the human-verification checkpoint plan — it built nothing and is
covered by the Manual-Only Verifications table below, not by a row here.*

### Known gap carried at v1.0 sign-off — DEBT-01

This contract's green is honest about its **envelope**, not universal. At v1.0 sign-off the
Solver's deadline envelope was a **known, accepted** limitation: hard *early* positions on the
large Classic boards (European-37, Asymmetric-39, Wiegleb-45) did not resolve inside
`SolverBudget.DEFAULT_DEADLINE` and, per D-04, folded to "no winning move". `SolverBudgetTest`
documents this explicitly and deliberately measures only the **tractable** corpus — so nothing
above ever claimed to cover those positions.

That gap was carried as **DEBT-01** and is closed by **v1.1 Phase 8** (plans 08-08/08-09/08-10),
which replaces the timing gamble with a committed depth-K opening book. Its coverage lands as
`DepthSweepTest` (the exhaustive symmetry-reduced depth-K sweep proving every position within K
moves resolves correctly) and `OpeningBookTest` (book integrity: every entry reachable, every
recorded move legal, every line terminating at the board's `minimumPegs`). **Those two tests
belong to Phase 8's contract, not this one** — they are named here only so a reader of this
sign-off can follow the gap to where it is actually closed.

---

## Wave 0 Requirements

*All four items verified present on disk and green on 2026-08-29 (DEBT-03 audit, v1.1 Phase 8 plan 08-03).*

- [x] `SolverTest` (`:domain`) — solve-correctness on catalog boards; returns null only when genuinely unsolvable. Budget behavior split out into `SolverBudgetTest` (the D-04 completeness corpus + ≥4× headroom assertion) and `PagodaTest` (the fast-reject's one-directional contract)
- [x] `GetHintUseCaseTest` (`:domain`) — recorded-line-relative → Solver fallback → null pipeline (ASST-01, ASST-03)
- [x] `GameViewModelTest` (`:app`, Turbine/coroutines-test) — hint state, thinking flag, no-win flag, undo, stuck-rescue gating (ASST-02, ASST-03)
- [x] Compose UI / semantics tests — source+dest highlight, "thinking" state, inline no-win banner, undo button → `HintControlTest`, `NoWinBannerTest`, `UndoControlTest`

*The draft folded budget behavior into `SolverTest`; 04-02 shipped it as the separate
`SolverBudgetTest` + `PagodaTest` files, now named as their own rows above.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Hint highlight visual polish (glow + A→B arrow) | ASST-01 | Visual affordance quality is subjective beyond semantics/Roborazzi | Launch app, request hint on/off the recorded line, confirm source+dest legibility |

*Solver correctness/budget, undo, and stuck-rescue all have automated verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies — every executable plan (04-01…04-04) is represented by a row with a scoped command; 04-05 was the human-verification checkpoint plan
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references — all four Wave-0 items exist on disk and are green
- [x] No watch-mode flags — every command is a one-shot `./gradlew` invocation
- [x] Feedback latency < 120s — `:domain:test` ~15 s, the scoped `:app:testDebugUnitTest` run ~5 s (measured 2026-08-29)
- [x] `nyquist_compliant` flag set to `true` in frontmatter

**Approval:** approved 2026-08-29 by v1.1 Phase 8 / DEBT-03 (plan 08-03). Every
`Automated Command` in the Per-Task Verification Map above was executed in this worktree
and exited 0 before these boxes were ticked:
`./gradlew :domain:test --tests '*SolverTest*' --tests '*SolverBudgetTest*' --tests '*PagodaTest*' --tests '*GetHintUseCaseTest*'`
and
`./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*' --tests '*HintControlTest*' --tests '*NoWinBannerTest*' --tests '*UndoControlTest*'`.
This sign-off is explicitly scoped by the **DEBT-01** note above: the Solver deadline
envelope on the large Classic boards was a known, accepted gap at v1.0 and is not claimed
as covered here. No box is ticked for anything not actually run.
