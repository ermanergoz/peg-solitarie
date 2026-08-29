---
phase: 08-assist-generator-quality-debt
verified: 2026-08-29T22:11:45Z
status: gaps_found
score: 7/8 must-haves verified
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
gaps:
  - truth: "Every hint the game shows is a legal move on the board the player is actually looking at (plan 08-10 must-have; underlies DEBT-01's 'hint system tells the truth')"
    status: failed
    reason: "Code review finding CR-01 (08-REVIEW.md) is unresolved at HEAD. GameViewModel.onHintRequested launches a search that can legitimately run the full 1.5s deadline (the documented off-book Diamond-41 worst case). Neither performJump nor onUndo cancels the in-flight hintJob — verified directly in app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt (grep for `hintJob` shows cancellation only in onHintRequested and resetToInitial, lines 840/891-892 — not in performJump at line 592 or onUndo at line 772). When the stale search resolves, its unconditional `_uiState.update` writes hintSource/hintTarget (or a stale noWinningMove) computed for the OLD board onto the CURRENT board. This directly contradicts the truthfulness guarantee GetHintUseCase's own KDoc and every book/solver test assert: 'a highlighted move gameplay then rejects is a visible correctness failure.' The RulesEngine tap path still prevents the illegal move from being *played*, but the shown hint/banner can misrepresent the board currently on screen."
    artifacts:
      - path: "app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt"
        issue: "performJump (line 592) and onUndo (line 772) clear hint UI state but do not call hintJob?.cancel(); the async hint result handler (lines 839-876) applies its result unconditionally without checking whether the board it searched is still the board being shown."
    missing:
      - "hintJob?.cancel(); hintJob = null at the start of performJump and onUndo (or equivalent), per 08-REVIEW.md CR-01's suggested fix"
      - "Capture board/solution/movesPlayed on the main thread before crossing dispatchers, and discard (or re-check) a hint result against the board it was computed for before applying it to UI state"
deferred: []
human_verification:
  - test: "Confirm on a real Android device: (1) no rescue banner when requesting a hint from a hard early position on European-37, Asymmetric-39, or Wiegleb-45; (2) an instant Diamond-41 hint; (3) a Diamond-41 finish at 2 pegs awards 3 stars."
    expected: "All three player-visible claims of Phase 8 hold on physical hardware, not only in JVM unit tests."
    why_human: "Requires physical Android hardware; explicitly deferred by developer decision at the 08-11 checkpoint (no device available, emulator substitute declined). Already tracked as a pending todo: .planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md, with the full itemised checklist preserved. This is routed here for visibility, not as a new finding."
---

# Phase 8: Assist & Generator Quality Debt Verification Report

**Phase Goal:** The hint system tells the truth on every shipped board, and every Classic board's solvability is proven rather than asserted.
**Verified:** 2026-08-29T22:11:45Z (system date reported as 2026-08-30 during this session)
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Every board carries its own per-board "perfect finish" target (`minimumPegs`); Diamond-41 = 2, all others = 1 | ✓ VERIFIED | `BoardConstants.kt` (`MINIMUM_PEGS_DEFAULT=1`, `DIAMOND41_FEWEST_PEGS_FLOOR=2`), `BoardSpec.kt:20,54-55` (`init { require(minimumPegs in 1..size) }`), `Boards.kt:287` (`minimumPegs = BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR`). `BoardSpecTest` (14 tests) locks it; full `:domain:test` green. |
| 2 | Symmetry canonicalisation exists in exactly one place, reused by the solver, the generator dedup gate and the authoring tool | ✓ VERIFIED | `grep -rn 'fun symmetryPermutations' domain/src app/src` → exactly 1 hit (`BoardCanonical.kt`). `CanonicalTest` (6 tests, verified to fail 4/6 under a deliberate chunk-count regression) proves peg-count preservation, idempotence and naive-agreement across all five footprints. |
| 3 | Solver's colour-parity prune (`Pagoda`) is target-aware and never rejects a genuinely-solvable position | ✓ VERIFIED | `Pagoda.kt` reads `board.spec.minimumPegs`; `PagodaTest` (7 tests, verified to fail 3/7 under a deliberate degeneration) proves one-directionality and admits `Boards.diamond41Initial`. Honestly documents the prune is provably vacuous at Diamond's 2-peg target (not merely weaker). |
| 4 | Solver's inner loop is allocation-free and its restart policy is genuinely reachable (not dead code) | ✓ VERIFIED | `Solver.kt` — `board.pegCount <= board.spec.minimumPegs` goal test, `SolverTables`-backed mask ops, geometric per-attempt cap escalation (`ATTEMPT_CAP_GROWTH`, `MAX_CAP_ESCALATIONS`). `SolverTest` (8 tests) proves European-37 and Asymmetric-39 full starts resolve well inside the unchanged 1.5s deadline (previously folded to null); a restart-reachability test proves attempts beyond #0 are exercised. |
| 5 | Depth-4 opening book: every committed entry proven by unbounded offline solve + full-line replay through the unchanged `RulesEngine`; the generator never ships | ✓ VERIFIED | `OpeningBookAuthoring.kt` is `:domain` **test**-source (`grep -rn 'OpeningBookAuthoring\|BeamSearchAuthoring' domain/src/main` → no matches). Five committed books on disk (47 619 B total) match `08-08-SUMMARY.md`'s BookReport exactly (`wc -c` confirmed). `OpeningBookTest` (19 tests) independently re-derives each board's BFS frontier and cross-checks 2 045 entries for legality, chain closure, and header-exact coverage. |
| 6 | DEBT-01's literal "every position within 4 moves of every large board, including Wiegleb-45" | ✓ VERIFIED (override) | Rescoped by developer decision at the 08-08/08-10 checkpoints: Wiegleb-45 ships K=3 (not K=4), Diamond-41 ships with a documented 440/818 gap. The property actually delivered and machine-checked on every `:domain:depthSweep` run: the pipeline never claims a move, or a dead position, it did not prove. `DepthSweepTest` pins solver-fallback counts to each book's own header by equality. See `overrides:` frontmatter. |
| 7 | **Every hint the game shows is a legal move on the board the player is actually looking at** | ✗ FAILED | Code review finding CR-01 (`08-REVIEW.md`) is unresolved at HEAD. `GameViewModel.performJump`/`onUndo` do not cancel `hintJob`; a stale in-flight hint search (which can legitimately run the full 1.5s on Diamond-41's vacuous-prune worst case) can write a hint/banner for a board the player has since moved past. See Gaps below. |
| 8 | `OpeningBook.lookup` cleanly separates MISS (falls to solver) from authoritative `NoWinningMove` (short-circuits) | ✓ VERIFIED | `GetHintUseCase.kt` — `BookAnswer.NoWinningMove -> null` short-circuits without a solver call; `null` lookup (MISS) falls to `Solver.firstWinningMove`. `GetHintUseCaseTest` (11 tests) and `DepthSweepTest` both exercise this at scale (2 329 swept positions). |
| 9 | Diamond-41's centre start provably reaches its fewest-pegs floor of 2 (reachability) and provably cannot reach 1 (optimality) | ✓ VERIFIED | `domain/src/test/resources/golden/solutions/diamond41.txt` — 37 lines (38 moves), replayed through the unchanged `RulesEngine` by `SolutionTest`, terminal `{(2,2),(5,5)}`, 2 pegs, 0 legal moves at terminal (dead end). `DiamondFloorOptimalityTest` (6 tests) exhaustively excludes all 41 single-peg positions via the GF(4) invariant, with positive/negative controls, verified to fail under 2 deliberate breaks. |
| 10 | A Diamond-41 finish at 2 pegs is a genuine win, awarded 3 stars; resumed games preserve the board's target; a forged target is refused | ✓ VERIFIED | `ScoreCalculator.kt` — `stars(pegsRemaining, minimumPegs)`, `<=`-banded. `GameViewModel.statusFor` — `board.pegCount <= board.spec.minimumPegs -> GameStatus.Won` (line 950). `ResumeSnapshotDto.minimumPegs: Int? = null`; `SnapshotMapper` validates `in 1..positions.size` before construction, refusing (not clamping) an out-of-range value. `ScoreCalculatorTest` (16 tests incl. a `0..45` byte-identical non-regression sweep), `SnapshotMapperTest` (25 tests). |
| 11 | The Nyquist validation coverage gaps carried from v1.0 phases 3 and 4 are backfilled and green in `check` | ✓ VERIFIED | `03-VALIDATION.md`/`04-VALIDATION.md` — `nyquist_compliant: true`, `status: approved`, populated Per-Task Verification Maps with real files/commands. `v1.0-MILESTONE-AUDIT.md` — `compliant_phases: [01,02,03,04,05]`, `partial_phases: []`. `QualityGateTest.theDedupGateIsDeterministicAcrossRegeneration` closes the one genuinely-missing leg the audit found. |
| 12 | The committed opening book survives byte-intact into a minified, resource-shrunk release APK | ✓ VERIFIED | `08-BOOK-EVIDENCE.md` — `./gradlew :app:assembleRelease` BUILD SUCCESSFUL; all 5 packaged entries `cmp`-identical to committed source (exit 0 ×5); `aapt2 dump resources` returns 0 matches for `golden/book` (plain JAR resources, outside `isShrinkResources`' remit — confirmed, not assumed). |
| 13 | Planning documents no longer describe Diamond-41 as an off-center (4,2) start or its floor as "several pegs" | ✓ VERIFIED | `grep -rn '(4,2)\|several pegs' STATE.md ROADMAP.md PROJECT.md v1.0-MILESTONE-AUDIT.md` → no matches (re-run during this verification). `ROADMAP.md` phase-8 success criteria explicitly name the centre start and floor of 2. |
| 14 | A player on a real device confirms the three player-visible claims of this phase (08-11's own must-have) | ? NOT PERFORMED (deferred) | `08-11-SUMMARY.md` explicitly states Task 3 was deferred by developer decision — "No per-item approvals exist. No global approval exists." — and tracked as `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md`. Routed to Human Verification below and covered by an override entry (deliberate, tracked deferral — not a silent gap). |

**Score:** 12/14 truths cleanly VERIFIED, 1 FAILED (CR-01), 1 deferred-by-decision (device audit, override + human-verification routed).
Rolled up to the phase's must-have granularity per the frontmatter: **7/8 must-haves verified** (3 covered by documented overrides, 1 failed).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/src/main/kotlin/.../model/BoardConstants.kt` | Scoped constants, `DIAMOND41_FEWEST_PEGS_FLOOR=2` | ✓ VERIFIED | Present, matches SUMMARY exactly |
| `domain/src/main/kotlin/.../model/BoardSpec.kt` | `minimumPegs`, `footprint`, validation | ✓ VERIFIED | Present, wired, tested |
| `domain/src/main/kotlin/.../model/Boards.kt` | `CLASSIC_SPECS`, Diamond-41 target=2 | ✓ VERIFIED | Present |
| `domain/src/main/kotlin/.../generator/BoardCanonical.kt` | Single canonicalisation + chunked fast path | ✓ VERIFIED | `grep -c 'fun canonical('` → 1 repo-wide |
| `domain/src/main/kotlin/.../solver/SolverTables.kt` | Precomputed per-footprint tables | ✓ VERIFIED | Present, `SolverTablesTest` proves table-vs-RulesEngine agreement |
| `domain/src/main/kotlin/.../solver/Pagoda.kt` | Target-aware colour-parity | ✓ VERIFIED | `board.spec.minimumPegs` present |
| `domain/src/main/kotlin/.../solver/Solver.kt` | Bitboard search, per-board goal, working restarts | ✓ VERIFIED | `SOLVED_PEG_COUNT` deleted, per-attempt caps present |
| `domain/src/main/kotlin/.../assist/OpeningBookFormat.kt` | Shared fail-closed codec | ✓ VERIFIED | Present in `:domain` main |
| `domain/src/main/kotlin/.../assist/OpeningBook.kt` | Classpath loader, 3-outcome lookup | ✓ VERIFIED | `getResourceAsStream("/golden/book/` present |
| `domain/src/main/resources/golden/book/*.txt` (5 files) | Committed books, ~47.6 KB total | ✓ VERIFIED | 5 files on disk, byte counts match SUMMARY (11344/9267/8754/14664/3590 B) |
| `domain/src/main/kotlin/.../usecase/GetHintUseCase.kt` | Book-first hint pipeline, one legality guard | ✓ VERIFIED | `RulesEngine.legalMoves(` count = 1 in executable code |
| `domain/src/test/kotlin/.../solver/DepthSweepTest.kt` | D-08 exhaustive sweep, gated out of per-commit | ✓ VERIFIED | Present, 367 lines; `:domain:depthSweep` task wired into `check` (`domain/build.gradle.kts:43-60`) |
| `domain/src/test/resources/golden/solutions/diamond41.txt` | Committed 38-move floor line | ✓ VERIFIED | 37 lines (38 moves) on disk |
| `domain/src/test/kotlin/.../solver/DiamondFloorOptimalityTest.kt` | Exhaustive GF(4) optimality proof | ✓ VERIFIED | Present |
| `domain/src/main/kotlin/.../scoring/ScoreCalculator.kt` | Target-aware `stars`/`evaluate` | ✓ VERIFIED | Present |
| `app/src/main/java/.../data/resume/ResumeSnapshotDto.kt` | Nullable `minimumPegs` | ✓ VERIFIED | Present |
| `.planning/milestones/v1.0-phases/03.../03-VALIDATION.md` | `nyquist_compliant: true` | ✓ VERIFIED | Confirmed |
| `.planning/milestones/v1.0-phases/04.../04-VALIDATION.md` | `nyquist_compliant: true` | ✓ VERIFIED | Confirmed |
| `.planning/phases/08.../08-BOOK-EVIDENCE.md` | Release-APK retention proof | ✓ VERIFIED | Present, matches claims (byte-identical ×5) |
| `.planning/PROJECT.md` | Resolved Key Decisions row | ✓ VERIFIED | DEBT-01/02 row present, `Revisit` markers gone |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `Boards.kt` (Diamond41) | `BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR` | `minimumPegs = ...` | ✓ WIRED | Confirmed at `Boards.kt:287` |
| `Solver.kt` | `BoardCanonical` | import + delegation | ✓ WIRED | `Solver.kt` uses `canonicalTables`/`canonicalFast` |
| `Pagoda.kt` | `SolverTables` colour-class masks | popcounts | ✓ WIRED | `countOneBits()` present |
| `OpeningBookAuthoring.kt` (test) | `OpeningBookFormat.kt` (main) | shared codec | ✓ WIRED | Encoder/decoder co-located, no drift |
| `OpeningBook.kt` | `domain/src/main/resources/golden/book/` | `getResourceAsStream` | ✓ WIRED | Verified via `OpeningBookTest` (19 tests) and `08-BOOK-EVIDENCE.md`'s release-APK proof |
| `GetHintUseCase.kt` | `OpeningBook.lookup` | book lookup before solver fallback | ✓ WIRED | Confirmed in source, `GetHintUseCaseTest` (11 tests) |
| `GetHintUseCase.kt` | `RulesEngine.legalMoves` | single shared guard for both stored-move paths | ✓ WIRED | Call count pinned at 1 |
| `GameViewModel.kt` | `ScoreCalculator.evaluate(minimumPegs=...)` | finished-event call site | ✓ WIRED | `nextBoard.spec.minimumPegs` threaded (line 677) |
| `SnapshotMapper.kt` | `Boards.CLASSIC_SPECS` | legacy-blob target resolution | ✓ WIRED | `CLASSIC_SPECS` referenced 3×, footprint-keyed |
| **`GameViewModel.onHintRequested` result** | **`_uiState` (board the player sees)** | **unconditional apply, no board-identity check, no cancellation on mutation** | ✗ **NOT SAFELY WIRED** | CR-01 (see Gaps) — a stale search result can be written onto a board the search never ran against |

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|---|---|---|---|---|
| DEBT-01 | 08-01, 08-02, 08-04, 08-05, 08-08, 08-09, 08-10 | Hint solver returns a winning move within deadline instead of folding to "no winning move" | ✓ SATISFIED (rescoped, override) | Book+bitboard-solver mechanism proven for the shipped scope; residual 444-position gap documented, machine-checked, tracked. **However**, the UI-layer staleness bug (CR-01) means the delivered guarantee is not yet airtight end-to-end — see Gaps. |
| DEBT-02 | 08-01, 08-06, 08-07 | Diamond-41 shipped start proven solvable by committed recorded-solution fixture replayed through rules engine | ✓ SATISFIED | Fully discharged: golden line + GF(4) exhaustive optimality proof + star bands + resume preservation, all verified in code. |
| DEBT-03 | 08-03 | Validation coverage gaps from phases 3/4 backfilled | ✓ SATISFIED | Both contracts populated and signed off; milestone audit updated; one genuinely-missing test added. |

**Cross-reference against `.planning/REQUIREMENTS.md`:** all three IDs (DEBT-01, DEBT-02, DEBT-03) are present in the Tech Debt section and in the phase-tracking table. **Gap found:** none of the three is checked `[x]` or marked anything other than `Pending` in `REQUIREMENTS.md`, even though `ROADMAP.md` marks Phase 8 `[x]` completed and this verification confirms the underlying work substantively exists. This is a documentation-bookkeeping gap, not a code gap — flagged as a WARNING below, not a BLOCKER, since it does not affect functional correctness and is typically an orchestrator-owned post-verification update (08-01's and 08-06's own SUMMARYs explicitly defer this update to "the orchestrator").

No orphaned requirements found: `REQUIREMENTS.md`'s Phase 8 mapping (DEBT-01/02/03) matches exactly the three IDs declared across all 11 plans' frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | 592 (performJump), 772 (onUndo), 839-876 (onHintRequested) | Unresolved code-review Critical CR-01: in-flight hint result applied without board-identity check or cancellation on mutation | 🛑 Blocker | Undermines "every hint shown is legal on the board actually displayed" — the phase's own truthfulness contract. Confirmed unfixed at HEAD (no commit references CR-01; `hintJob?.cancel()` absent from both mutation sites). |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | 476, 480-482 | WR-01: resume-snapshot save/clear ride `viewModelScope`, can be silently cancelled on navigation-away | ⚠️ Warning | Orthogonal to DEBT-01/02/03's must-haves (affects PERS-03 from an earlier phase), noted for completeness — not blocking this phase's goal. |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | 342-353, 389-400, 427-441, 915-925 | WR-02: six-field cue/settings carry-forward copy-pasted at 4 sites | ⚠️ Warning | Orthogonal to this phase's must-haves; a latent maintenance hazard, not a DEBT-01/02/03 defect. |
| `.planning/phases/08-assist-generator-quality-debt/08-BOOK-EVIDENCE.md` | §5 "What this proves, and what it does not" | States "the on-device audit in this plan's Task 3 confirms a book-served hint on real hardware" — contradicted by the same plan's own SUMMARY, which records Task 3 as deferred and NOT performed | ℹ️ Info | Stale sentence, written before the Task 3 checkpoint decision and not updated afterward. Does not affect the retention proof itself (bytes/R8/aapt2 evidence is self-contained and correct); worth a one-line fix. |
| Domain production files (`BoardSpec.kt`, `Boards.kt`, `Solver.kt`, `Pagoda.kt`, `SolverTables.kt`, `OpeningBook.kt`, `OpeningBookFormat.kt`, `GetHintUseCase.kt`, `ScoreCalculator.kt`, `ScoreParams.kt`, `SolverBudget.kt`, `BoardCanonical.kt`, `BoardConstants.kt`) | — | `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` scan | — | **Clean.** Zero debt markers found in any phase-modified production file. |

No debt-marker gate violations (no unreferenced `TBD`/`FIXME`/`XXX` in phase-touched files).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Full `:domain` test suite green | `./gradlew :domain:test --rerun-tasks` | BUILD SUCCESSFUL in 32s | ✓ PASS |
| `ktlint`/`detekt` gates green | `./gradlew ktlintCheck detekt` | BUILD SUCCESSFUL | ✓ PASS |
| App-side regression on hint/resume/scoring suites | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelTest*' --tests '*GameFlowTest*' --tests '*SnapshotMapperTest*'` | BUILD SUCCESSFUL | ✓ PASS |
| Opening book resources present and correctly sized on disk | `wc -c domain/src/main/resources/golden/book/*.txt` | 11344/9267/8754/14664/3590 B, matches SUMMARY | ✓ PASS |
| Diamond-41 golden fixture present, correct line count | `wc -l domain/src/test/resources/golden/solutions/diamond41.txt` | 37 lines = 38 moves | ✓ PASS |
| Depth sweep is doubly gated (fast per-commit, slow in `check`) | `grep -n depthSweep domain/build.gradle.kts` | `depthSweep` task registered, `check.dependsOn(depthSweep)` | ✓ PASS |
| CR-01 fix present in `GameViewModel.kt` | `grep -n hintJob app/.../GameViewModel.kt` | cancellation only at `onHintRequested` (840) and `resetToInitial` (891-892); absent from `performJump`/`onUndo` | ✗ FAIL (confirms the gap) |

### Probe Execution

Not applicable — this is not a migration/tooling phase with declared probe scripts. No `scripts/*/tests/probe-*.sh` conventions apply; PLAN/SUMMARY files reference no probe harness. `./gradlew :domain:depthSweep` functions as this phase's exhaustive truthfulness check and is documented above (measured and reproduced twice, in `08-10-SUMMARY.md` and `08-11-SUMMARY.md`, both cited rather than re-run here per the task's guidance — an 11-minute run).

### Human Verification Required

#### 1. On-device confirmation of Phase 8's three player-visible claims

**Test:** Install a debug/release build on a physical Android device and: (a) request a hint from a hard early position on European-37, Asymmetric-39, and Wiegleb-45 and confirm a real move appears instead of the "no winning move" rescue banner; (b) request a hint on Diamond-41's start and confirm it returns near-instantly; (c) play Diamond-41 down to its proven 2-peg floor and confirm the Results screen awards 3 stars.
**Expected:** All three hold on real hardware, matching the JVM-level proofs.
**Why human:** Requires physical device interaction; already deferred by explicit developer decision (08-11 checkpoint, 2026-08-30) and tracked at `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md` with the full itemised 1a-5c checklist preserved. Routed here per the workflow's "harvest deferred items" step, not as a newly-discovered gap.

## Gaps Summary

Twelve of fourteen individually-checked truths are cleanly VERIFIED against the actual codebase — not just claimed in a SUMMARY. DEBT-02 (Diamond-41 floor proof) and DEBT-03 (Nyquist backfill) are **fully and rigorously discharged**: the golden line replays through the real `RulesEngine`, the GF(4) optimality proof is exhaustive with controls, the star bands and resume path are re-anchored and tested, and both validation contracts are populated with real, executed commands. DEBT-01's core mechanism (book + bitboard solver + restart policy) is real, tested, and the three rescope decisions (Wiegleb-45 K=3, Diamond-41's 440-position gap, English-33's proven-dead markers) are documented, developer-approved deviations that this verification accepts under `overrides:` rather than re-litigating.

**The one blocking gap:** code review finding CR-01 — an in-flight hint search result can be applied to a board the player has since changed (via a jump or undo) because neither `performJump` nor `onUndo` cancels the pending `hintJob`, and the async result handler applies its outcome unconditionally. This was found by the phase's own code review, is squarely inside the phase's stated goal ("the hint system tells the truth on every shipped board"), and remains unfixed at HEAD — verified directly by inspecting `GameViewModel.kt` rather than trusting the review's age. It does not corrupt game state (the `RulesEngine` tap path still rejects an actually-illegal move) but it can show a stale, wrong hint glow or a stale "no winning move" banner for a board that was never searched — precisely the "highlighted move gameplay then rejects" failure mode `GetHintUseCase`'s own KDoc calls a visible correctness failure. This needs a closure plan before the phase's truthfulness claim can be considered airtight; it is a small, well-scoped fix (the reviewer's suggested patch is ~10 lines plus two `hintJob?.cancel()` calls).

A secondary, non-blocking finding: `.planning/REQUIREMENTS.md` still lists DEBT-01/02/03 as `Pending`/unchecked despite `ROADMAP.md` marking Phase 8 complete and this verification confirming the underlying work. This is bookkeeping, not a code defect, and several plan SUMMARYs (08-01, 08-06) explicitly note this update is the orchestrator's responsibility post-verification.

---

*Verified: 2026-08-29T22:11:45Z*
*Verifier: Claude (gsd-verifier)*
