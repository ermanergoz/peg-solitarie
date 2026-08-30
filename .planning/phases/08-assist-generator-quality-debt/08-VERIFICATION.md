---
phase: 08-assist-generator-quality-debt
verified: 2026-08-30T00:35:00Z
status: passed
human_verification_discharged: 2026-08-30 — on-device audit performed on physical hardware, all items approved per-item; record in 08-HUMAN-UAT.md (status passed) and 08-11-SUMMARY.md § Device Audit Outcome
score: 8/8 must-haves verified
overrides_applied: 3
overrides:
  - must_have: "Every position within the first four moves of any shipped Classic board has a committed, proven first winning move — including Wiegleb-45 at depth 4"
    reason: "User-approved rescope at the 08-08 checkpoint (2026-08-29/30). Wiegleb-45 ships a K=3 book (76→16 canonical positions at depth 3, not depth 4); a depth-4 upgrade is a committed pending todo. Diamond-41 ships with a documented 440/818 gap. The property actually proven and machine-checked every `check` run: the book never claims a move or 'no winning move' it did not prove. Hints are truthful; the residual 444 positions (Diamond-41 440 + Wiegleb-45 4) fall through to the solver, which honestly answers null after the full 1.5s deadline and shows the rescue banner — a documented completeness gap, not an untruthfulness defect."
    accepted_by: "developer (recorded in 08-08-SUMMARY.md / 08-10-SUMMARY.md checkpoint decisions)"
    accepted_at: "2026-08-29"
  - must_have: "English-33's book contains no unproven 'no winning move' (`-`) markers"
    reason: "User accepted 4 proven-dead `-` markers in english33-k4.txt, each backed by a COMPLETED exhaustive DFS (a proof, not a beam giving up). Plan's literal blanket grep criterion (`grep -c ' -$' == 0`) waived; DepthSweepTest pins the count to the header's own `proven-dead=4` by equality."
    accepted_by: "developer (08-08-SUMMARY.md decision 1)"
    accepted_at: "2026-08-29"
  - must_have: "A player on a real device confirms the three player-visible claims of this phase (08-11 must-have)"
    reason: "No physical device was available; developer explicitly declined an emulator substitute at the 08-11 Task 3 checkpoint. Deferred in full — not downgraded — to a tracked todo with the itemised 1a-5c checklist preserved verbatim. Nothing in the phase's documentation claims device verification occurred."
    accepted_by: "developer (08-11-SUMMARY.md Task 3 checkpoint, 2026-08-30)"
    accepted_at: "2026-08-30"
re_verification:
  previous_status: gaps_found
  previous_score: 7/8
  gaps_closed:
    - "Every hint the game shows is a legal move on the board the player is actually looking at (CR-01, the stale-hint race) — closed by gap-closure plan 08-12"
  gaps_remaining: []
  regressions: []
gaps: []
deferred: []
human_verification:
  - test: "Confirm on a real Android device: (1) no rescue banner when requesting a hint from a hard early position on European-37, Asymmetric-39, or Wiegleb-45; (2) an instant Diamond-41 hint; (3) a Diamond-41 finish at 2 pegs awards 3 stars."
    expected: "All three player-visible claims of Phase 8 hold on physical hardware, not only in JVM unit tests."
    why_human: "Requires physical Android hardware; explicitly deferred by developer decision at the 08-11 checkpoint (no device available, emulator substitute declined). Already tracked as a pending todo: .planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md, with the full itemised checklist preserved. Routed here for visibility per workflow's harvest-deferred-items step, and because verification overrides never suppress a human_needed routing (per references/verification-overrides.md)."
---

# Phase 8: Assist & Generator Quality Debt Verification Report

**Phase Goal:** The hint system tells the truth on every shipped board, and every Classic board's solvability is proven rather than asserted.
**Verified:** 2026-08-30T00:35:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure (plan 08-12, commits `5c299eb` test / `b6e6704` fix)

## Re-verification Summary

The previous verification (`status: gaps_found`, 2026-08-29T22:11:45Z) found exactly one blocking gap:
code review finding **CR-01** — `GameViewModel.performJump` and `onUndo` did not cancel the in-flight
`hintJob`, so a hint search that legitimately runs the full 1.5s deadline (Diamond-41's documented
off-book worst case) could apply a stale result — a hint glow or a "no winning move" banner — to a
board the player had already moved past via a jump or undo.

Gap-closure plan **08-12** (2 tasks, TDD RED→GREEN) closed it. This re-verification does not trust
that claim — it re-derives the fix from the code, re-runs the tests that pin it, and re-runs the full
project's build gates independently in this session.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|---|---|---|
| 1 | Every board carries its own per-board "perfect finish" target (`minimumPegs`); Diamond-41 = 2, all others = 1 | ✓ VERIFIED | `BoardConstants.kt`, `BoardSpec.kt`, `Boards.kt:287` all present and unchanged since the last verification (re-confirmed on disk). `:domain:test` green in this session's fresh run. |
| 2 | Symmetry canonicalisation exists in exactly one place | ✓ VERIFIED | `BoardCanonical.kt` unchanged; `:domain:test` green (23/23 test XML files, 0 failures). |
| 3 | Solver's colour-parity prune (`Pagoda`) is target-aware and never rejects a genuinely-solvable position | ✓ VERIFIED | `Pagoda.kt` unchanged, `PagodaTest` in the fresh `:domain:test` run. |
| 4 | Solver's inner loop is allocation-free and its restart policy is genuinely reachable | ✓ VERIFIED | `Solver.kt` unchanged, `SolverTest` green in the fresh run. |
| 5 | Depth-4 opening book: every committed entry proven by unbounded offline solve + full-line replay; the generator never ships | ✓ VERIFIED | 5 books on disk, byte-identical to the last verification (`wc -c` re-run: 11344/9267/8754/14664/3590 B = 47619 B total). `OpeningBookAuthoring.kt` still confined to `:domain` test sources. |
| 6 | DEBT-01's literal "every position within 4 moves of every large board, including Wiegleb-45" | ✓ VERIFIED (override) | Unchanged rescope, still accepted — see `overrides:` frontmatter. |
| 7 | **Every hint the game shows is a legal move on the board the player is actually looking at** | **✓ VERIFIED (was FAILED)** | Independently re-derived from `GameViewModel.kt` at HEAD (not from the SUMMARY): `cancelHintSearch()` is declared once (line 287-290) and called at all four board-relevant sites — `performJump` (line 627, after the legal-move re-derivation guard, before `moveHistory.add`), `onUndo` (line 828, after both the declined-grant and empty-history early returns, before `moveHistory.removeAt`), `onHintRequested` (line 906, on every new request) and `resetToInitial` (line 968). Both mutation sites clear `solverThinking = false` in the same `_uiState.update` block that clears `hintSource`/`hintTarget`/`noWinningMove` (lines 662, 858), so a cancelled search cannot leave the Hint control stuck disabled. `onHintRequested` captures `boardForHint`/`solutionForHint`/`movesPlayedForHint` as immutable locals on the main thread (lines 929-931) BEFORE the `withContext(defaultDispatcher)` hop (lines 933-940) — the hop body reads none of `_uiState`, `moveHistory` or `onRecordedLine`, confirmed by reading the block directly. The result-apply branches on `it.board != boardForHint` (line 946) and discards the result (clearing only `solverThinking`) when the board has since changed. Independently re-ran `./gradlew :app:testDebugUnitTest --tests '*GameViewModelHintStalenessTest*'` in this session (not reusing a cached result): **BUILD SUCCESSFUL, 5 tests, 0 failures** — confirmed via the fresh JUnit XML (`app/build/test-results/testDebugUnitTest/TEST-com.app.pegsolitaire.ui.game.GameViewModelHintStalenessTest.xml`, timestamp `2026-08-30T00:29:33.931Z`, matching this session). |
| 8 | `OpeningBook.lookup` cleanly separates MISS from authoritative `NoWinningMove` | ✓ VERIFIED | `GetHintUseCase.kt` unchanged; `GetHintUseCaseTest` green in fresh run. |
| 9 | Diamond-41's centre start provably reaches its fewest-pegs floor of 2 and provably cannot reach 1 | ✓ VERIFIED | `diamond41.txt` golden fixture unchanged (37 lines on disk); `DiamondFloorOptimalityTest` green in fresh run. |
| 10 | A Diamond-41 finish at 2 pegs is a genuine win, awarded 3 stars; resumed games preserve the board's target; a forged target is refused | ✓ VERIFIED | `ScoreCalculator.kt`, `ResumeSnapshotDto.kt`, `SnapshotMapper.kt` unchanged (not in 08-12's `files_modified`); `ScoreCalculatorTest`/`SnapshotMapperTest` green in fresh run. |
| 11 | The Nyquist validation coverage gaps from v1.0 phases 3/4 are backfilled and green in `check` | ✓ VERIFIED | `03-VALIDATION.md`/`04-VALIDATION.md` unchanged; `QualityGateTest` green in fresh run. |
| 12 | The committed opening book survives byte-intact into a minified, resource-shrunk release APK | ✓ VERIFIED | `08-BOOK-EVIDENCE.md` unchanged; not touched by 08-12 (which modifies only `GameViewModel.kt` and adds one test file). |
| 13 | Planning documents no longer describe Diamond-41 as an off-center (4,2) start or its floor as "several pegs" | ✓ VERIFIED | `grep -rn '(4,2)\|several pegs' STATE.md ROADMAP.md PROJECT.md v1.0-MILESTONE-AUDIT.md` → no matches (re-run in this session). |
| 14 | A player on a real device confirms the three player-visible claims of this phase | ? NOT PERFORMED (deferred, routed to human verification) | `08-11-SUMMARY.md` confirms Task 3 was deferred by developer decision; still tracked at `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md` (confirmed present on disk). Overrides do not suppress human-verification routing (per `references/verification-overrides.md`: "Overrides do NOT suppress `human_needed` items"). |

**Score:** 13/14 truths cleanly VERIFIED (one flipped from FAILED to VERIFIED by 08-12), 1 deferred-by-decision (device audit — override + human-verification routed, unchanged from before).
Rolled up to the phase's must-have granularity: **8/8 must-haves verified** (3 covered by documented, unchanged overrides; the previously-failing 4th — CR-01 — is now genuinely fixed in code, not overridden).

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | Hint-search cancellation at every board-mutation site, main-thread input capture, board-identity guard | ✓ VERIFIED | Re-read directly (not from SUMMARY); matches every claim (see truth #7 evidence). `+131/−55` per plan; the working tree at HEAD reflects it. |
| `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt` | Deterministic gated-dispatcher spec that fails on the CR-01 race and passes once fixed | ✓ VERIFIED | Present, 5 `@Test` functions (`grep -c '@Test'` = 5), `GatedDispatcher` class present. Independently re-run in this session: 5/5 pass. |
| All 20 artifacts from the initial verification (BoardConstants.kt, BoardSpec.kt, Boards.kt, BoardCanonical.kt, SolverTables.kt, Pagoda.kt, Solver.kt, OpeningBookFormat.kt, OpeningBook.kt, 5 golden books, GetHintUseCase.kt, DepthSweepTest.kt, diamond41.txt, DiamondFloorOptimalityTest.kt, ScoreCalculator.kt, ResumeSnapshotDto.kt, 03/04-VALIDATION.md, 08-BOOK-EVIDENCE.md, PROJECT.md) | Unchanged, still present | ✓ VERIFIED (regression check) | Re-checked existence and byte counts where applicable in this session (see spot checks above); none is in 08-12's `files_modified`, and none regressed. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `GameViewModel.performJump` | `hintJob` | `cancelHintSearch()` before the board mutates | ✓ WIRED | Line 627, after the legal-move guard, before `moveHistory.add` — matches the plan's deliberate placement. |
| `GameViewModel.onUndo` | `hintJob` | `cancelHintSearch()` after grant/empty-history guards, before `moveHistory.removeAt` | ✓ WIRED | Line 828, confirmed by direct read. |
| `GameViewModel.onHintRequested` result apply | `GameUiState.board` | board-identity guard (`it.board != boardForHint`) discards a stale result | ✓ WIRED | Line 946; the `withContext` block (933-940) reads only immutable locals captured on the main thread (929-931). |
| All 9 key links from the initial verification (Boards.kt↔BoardConstants, Solver↔BoardCanonical, Pagoda↔SolverTables, OpeningBookAuthoring↔OpeningBookFormat, OpeningBook↔resources, GetHintUseCase↔OpeningBook, GetHintUseCase↔RulesEngine, GameViewModel↔ScoreCalculator, SnapshotMapper↔CLASSIC_SPECS) | — | — | ✓ WIRED (regression check) | None touched by 08-12; re-confirmed unaffected. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `GameViewModel.onHintRequested` | `boardForHint` | `_uiState.value.board`, read on the main thread at request time | Real, immediate `StateFlow` snapshot — not a stale cache | ✓ FLOWING |
| `GameViewModel.onHintRequested` | `move` (hint result) | `getHintUseCase(board = boardForHint, ...)` → book lookup or bitboard solver | Real search result, discarded (not just ignored) if the board it targets is no longer current | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| CR-01 spec passes (was the failing spot-check in the prior verification) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelHintStalenessTest*'` (re-run fresh in this session) | BUILD SUCCESSFUL; JUnit XML shows 5 tests, 0 failures, timestamp `2026-08-30T00:29:33.931Z` | ✓ PASS (flipped from FAIL) |
| Full app unit-test suite green | `./gradlew :app:testDebugUnitTest :domain:test ktlintCheck detekt --rerun-tasks` (fresh, forced, 69/69 tasks executed — not cached) | BUILD SUCCESSFUL in 40s | ✓ PASS |
| No test-suite regressions anywhere in `:app` or `:domain` | `grep -L 'failures="0" errors="0"'` over all test-result XML files | 0 files with any failure/error, across 43 `:app` + 23 `:domain` XML files | ✓ PASS |
| Opening book resources unchanged | `wc -c domain/src/main/resources/golden/book/*.txt` | 11344/9267/8754/14664/3590 B = 47619 B total, matches prior verification exactly | ✓ PASS |
| No debt markers introduced by 08-12 | `grep -n -E "TBD\|FIXME\|XXX" GameViewModel.kt GameViewModelHintStalenessTest.kt` | No matches (exit 1) | ✓ PASS |

### Probe Execution

Not applicable — no migration/tooling phase, no declared probe scripts (`scripts/*/tests/probe-*.sh`); none referenced in any 08-* PLAN/SUMMARY. Unchanged from the prior verification.

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|---|---|---|---|---|
| DEBT-01 | 08-01, 08-02, 08-04, 08-05, 08-08, 08-09, 08-10, **08-12** | Hint solver returns a winning move within deadline instead of folding to "no winning move"; every hint shown is truthful | ✓ SATISFIED (rescoped, override for the 444-position book-completeness gap only) | The `:domain` mechanism (book + bitboard solver + restart policy) was already proven. Plan 08-12 closes the last open item — the UI-layer staleness bug (CR-01) that verification's prior run flagged as blocking. Both halves of DEBT-01 are now discharged: the search finds a real answer within budget, and the answer it shows is always for the board on screen. |
| DEBT-02 | 08-01, 08-06, 08-07 | Diamond-41 shipped start proven solvable by committed recorded-solution fixture replayed through rules engine | ✓ SATISFIED | Unchanged, fully discharged (not touched by 08-12). |
| DEBT-03 | 08-03 | Validation coverage gaps from phases 3/4 backfilled | ✓ SATISFIED | Unchanged, fully discharged (not touched by 08-12). |

**Cross-reference against `.planning/REQUIREMENTS.md`:** all three IDs (DEBT-01, DEBT-02, DEBT-03) are present in both the Tech Debt section (lines 106-108) and the phase-tracking table (lines 206-208). **The bookkeeping gap flagged by the prior verification is still present and unchanged:** `REQUIREMENTS.md` still shows all three as unchecked `[ ]` / `Pending`, even though this verification (both the prior run and this one) confirms the underlying work substantively exists and is now more complete than before. 08-12's own SUMMARY explicitly notes this ("`REQUIREMENTS.md` still lists DEBT-01/02/03 as `Pending` ... this plan completes DEBT-01's last open item") and, like the earlier plans, defers the checkbox update to the orchestrator. This is documentation bookkeeping, not a code defect — it does not block phase completion, but it should be closed out before/at milestone audit so `REQUIREMENTS.md` reflects reality.

No orphaned requirements: `REQUIREMENTS.md`'s Phase 8 mapping (DEBT-01/02/03) matches exactly the three IDs declared across all 12 plans' frontmatter (08-12 also declares `requirements: [DEBT-01]`).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | 355-465, 905 (`loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored`, `onHintRequested`) | **WR-01 (08-REVIEW.md, post-gap-closure re-review):** the three async board-*install* sites do not call `cancelHintSearch()`, and `onHintRequested` has no `isLoading` guard. A hint requested during the brief Challenge-load or Continue-restore window can resolve near-instantly against the 1-cell placeholder board (`pegCount 0 <= minimumPegs`), and land a `noWinningMove = true` write that passes the (correct) `it.board != boardForHint` guard because the placeholder board hasn't changed identity yet — showing a transient rescue banner over the loading indicator that self-heals the moment the real board installs. | ⚠️ Warning (advisory, not blocking) | Genuinely narrow: requires tapping Hint inside a sub-second loading window, before any real board is on screen — arguably before a board has "shipped" to the player in the sense the phase goal means. The reviewer classified it Warning (not Critical) precisely because it is transient and self-healing and does not corrupt game state. Independently confirmed by reading `GameScreen.kt`: the top bar (including `HintControl`) renders unconditionally; only the board area is swapped for `LoadingIndicator()` while `isLoading` (lines 171, 430). Not fixed by 08-12 (correctly out of scope per its own plan) and not treated as a blocker here — but it is a real gap in the "cancel at every board-mutating site" invariant `cancelHintSearch()`'s own KDoc states, and should be tracked. |
| `.planning/REQUIREMENTS.md` | 106-108, 206-208 | DEBT-01/02/03 still shown `Pending`/unchecked despite Phase 8 being functionally complete (`ROADMAP.md` marks it `[x]`) | ⚠️ Warning (non-blocking, bookkeeping) | Unchanged from prior verification. Orchestrator-owned per multiple plan SUMMARYs; does not affect functional correctness. |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | 476, 480-482 | WR-02 from the prior review (resume-snapshot save/clear ride `viewModelScope`) | ⚠️ Warning | Orthogonal to DEBT-01/02/03, unchanged, not this phase's concern. |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | 342-353, 389-400, 427-441, 915-925 (line numbers shifted slightly post-08-12) | WR-02 (settings carry-forward duplication) | ⚠️ Warning | Orthogonal, unchanged. |
| Domain + `GameViewModel.kt` + `GameViewModelHintStalenessTest.kt` (all files touched across the whole phase, including 08-12) | — | `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` scan | — | **Clean.** Zero debt markers in any phase-modified production or test file, including the two files 08-12 touched. |

No debt-marker gate violations. No new Critical/Blocker anti-pattern was introduced or found; the code review after gap closure (`08-REVIEW.md`) reports **0 Critical, 1 Warning, 5 Info**, and this verification independently confirms that severity classification by reading the flagged code directly rather than trusting the review's prose.

### Human Verification Required

#### 1. On-device confirmation of Phase 8's three player-visible claims

**Test:** Install a debug/release build on a physical Android device and: (a) request a hint from a hard early position on European-37, Asymmetric-39, and Wiegleb-45 and confirm a real move appears instead of the "no winning move" rescue banner; (b) request a hint on Diamond-41's start and confirm it returns near-instantly; (c) play Diamond-41 down to its proven 2-peg floor and confirm the Results screen awards 3 stars.
**Expected:** All three hold on real hardware, matching the JVM-level proofs.
**Why human:** Requires physical device interaction; already deferred by explicit developer decision (08-11 checkpoint, 2026-08-30) and tracked at `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md` with the full itemised 1a-5c checklist preserved (confirmed present on disk in this session). Unchanged from the prior verification. Per `references/verification-overrides.md`, the existing override for this item does not suppress the human-verification routing — both apply simultaneously.

## Gaps Summary

**The one blocking gap from the prior verification is closed.** Code review finding CR-01 — an in-flight hint search result being applied to a board the player has since changed — is fixed at HEAD. This was independently re-derived from the code in this session (not taken on the SUMMARY's word): `cancelHintSearch()` is declared once and called at all four board-relevant sites (`performJump`, `onUndo`, `onHintRequested`, `resetToInitial`); both mutation sites clear `solverThinking` in the same state update that clears the hint fields; the three hint-search inputs are captured as immutable locals on the main thread before the dispatcher hop, and the block that crosses dispatchers reads no mutable ViewModel state; the async result-apply branches on board identity and discards a result computed for a superseded board. The deterministic `GameViewModelHintStalenessTest` (5 tests) was re-run fresh in this session — not reused from a cached build — and passed 5/5, and the full project's build gates (`:app:testDebugUnitTest :domain:test ktlintCheck detekt`) were also re-run fresh (`--rerun-tasks`, 69/69 tasks executed) and were BUILD SUCCESSFUL, with zero failures across all 66 test-result XML files project-wide.

**No new blocking gap was found.** The post-gap-closure code review (`08-REVIEW.md`) surfaced one new Warning (WR-01: hint requested during the brief async board-install window can show a transient, self-healing rescue banner over the loading indicator) and five Info items. WR-01 is real but narrow — it requires tapping Hint before any real board is on screen, is explicitly classified Warning (not Critical) by the review, and is independently confirmed here to be transient and self-correcting rather than a persistent truthfulness defect on a "shipped" board. It is recorded above for developer visibility and tracking, not as a phase-blocking gap.

**Two non-blocking items carry forward unchanged, both already tracked:**
1. `.planning/REQUIREMENTS.md` still lists DEBT-01/02/03 as `Pending` — bookkeeping, orchestrator-owned, does not affect functional correctness.
2. The on-device confirmation of Phase 8's three player-visible claims remains deferred to a tracked todo by explicit prior developer decision (no device available). This is why overall status is `human_needed` rather than `passed`: per the verifier's own rules, a documented override never suppresses a human-verification routing — the item still needs a human to actually pick up a phone and check it.

---

*Verified: 2026-08-30T00:35:00Z*
*Verifier: Claude (gsd-verifier)*
