# android-game-dev report — hint-undecided (Undecided is not "no winning move")
Date: 2026-10-23
Branch: bugfix/hint-undecided (4b54f77..7201742). My commits are `4b54f77`, `de7abd4`, `fecf13b`, `596ac7f`, `197f9f8`, `45f2b96` and `7201742`; the `docs(14)` commits between them belong to other roles.
Skills loaded: ergoz-soft:standards (role android-game-dev), superpowers:test-driven-development, gitflow, android-viewmodel, android-coroutines, android-testing, compose-skill, compose-ui. All were loaded on 2026-10-21, before the first edit.
Ran in parallel with: check-gates and audio-review work on the same host, in their own worktrees. In this worktree, on separate paths: the ux-designer's spec commits (`cdf618c`, `f2b722d`) and the producer's report commits.
Ran on: claude-opus-5-5 at effort max (the role's pin)
Fan-out: none
Loops: 0
Lessons applied: none (the 0.6.2 snapshot has 0 active lessons; there is no RECALL.md)

> Producer note: the agent returned this report as text because the harness refused a report file from a
> subagent; the producer wrote it here verbatim (HTML entities unescaped) and committed it as H5.

## Summary
- **Domain (H1, H2):** `Solver.decide` returns `Found`, `ProvenDead` or `Undecided`, and `ProvenDead` only on a proof. `GetHintUseCase` returns `HintOutcome`, and `firstWinningMove` is gone.
- **ViewModel (H3):** only `ProvenDead` raises the rescue banner. `Undecided` raises `hintUndecided` instead: it costs nothing, offers no ad, and is cleared with `noWinningMove`.
- **Walks (H3a):** the tests replay the ruling's walks A–C and four spare walks, each proven winnable. AC-2c proves that following Hint stays on the book on every Classic board.
- **Screen (H4, H4b):** H4 adds the D-04g notice: one polite live region, its own tap and a Dismiss action, up for 5 s or the accessibility timeout, never shown twice. H4b makes the banner's message a polite live region (23k).
- **Check:** green at `7201742`: app 1096/0, domain 288/0, the sweep, build-logic 54, lint, detekt, ktlint and the literal checks.

## Outputs
**Domain**
- `domain/.../solver/SolverOutcome.kt` (new) and `Solver.kt`: `decide(...)`. No constant changed.
- `domain/.../solver/SolverBudget.kt` and `domain/.../solver/Pagoda.kt`: KDoc only.
- `domain/.../usecase/HintOutcome.kt` (new) and `GetHintUseCase.kt`: an exhaustive mapping with no `else`.

**App**
- `app/.../ui/game/GameUiState.kt`: one defaulted field, `hintUndecided = false`, also listed in `initial()` and `loading()`.
- `app/.../ui/game/GameViewModel.kt`: the ProvenDead/Undecided split. The Undecided branch returns early and never enters the ad funnel. The two flags are cleared in pairs in 3 `copy`s.
- `app/.../ui/game/HintUndecidedNotice.kt` (new):
  - the leaf `HintUndecidedNotice(message, onDismiss, modifier)`;
  - `internal UndecidedNoticeSlot(raised, modifier)`, which owns the timer, the accessibility timeout, the saveable "ended" flag, the key per raise and the motion;
  - light and dark previews.
- `app/.../ui/game/GameScreen.kt`:
  - the slot sits in the banner `if`'s new `else` branch, at the bottom centre;
  - a KDoc paragraph and a comment;
  - the `LongMethod` comment is amended: an edited line, not a new suppression.
- `app/.../ui/game/GameScreenPreviews.kt`: a notice-state preview, light and dark.
- `app/.../ui/game/GameAnim.kt`: `HINT_NOTICE_DISPLAY_MS = 5_000` and `HINT_NOTICE_EXIT_MS = 160`.
- `app/.../ui/game/NoWinBanner.kt`: A1.2 only.
- `app/src/main/res/values/strings.xml`: `hint_undecided_message`.
- `app/src/main/res/values/dimens.xml`: `hint_notice_glyph_size` (20dp) and `hint_notice_min_height` (48dp).

**Tests**
- Domain: `SolverTest`, `SolverBudgetTest`, `PagodaTest` (comment), `GetHintUseCaseTest`, `DepthSweepTest`.
- App: `GameViewModelAssistOrderingTest`, `GameViewModelHintStalenessTest`, `GameViewModelResumeTest`, `GameViewModelTelemetryTest`, `GameScreenHintNoticeTest` (new, 20 tests), `DockStateLanguageTest` (two 21a rows), `NoWinBannerTest`, plus the stubs in `GameViewModelTest` and `GameViewModelAdClockTest`.
- No dependency was added, so `THIRD_PARTY.md` gets no row.

## Per-commit evidence
| Commit | Red | Green | Tests |
|---|---|---|---|
| H1 `4b54f77` | `decide` written as today's fold. `:domain:test --tests '…solver.*'`: 41 run, 4 failed, all "expected: Undecided / but was : ProvenDead" | solver 41/41; `:domain:test` 278/278 | `aSearchCancelledOnItsFirstPollIsUndecided`, `aSearchThatSpendsItsCumulativeCeilingIsUndecided`, `aBudgetThatAllowsNoAttemptIsUndecided`, `aSearchThatSpendsEveryAttemptIsUndecided` (beyond the brief); two renames to …IsProvenDead |
| H2 `de7abd4` | The use case still folded Undecided into ProvenDead. `:domain:test`: 286 run, 5 failed (AC-1, AC-2, AC-6, Diamond gap, off-book Classic), same message | domain 286/286; `ui.game.*` 204/204; `:domain:depthSweep` green in 11m7s (load 5.39 5.81 6.63) | AC-1 to AC-6; Diamond-41 gap; `DepthSweepTest`'s split with its own `deferred` count |
| `fecf13b` | — (KDoc table only: an "undecided" column, 0/0/0/440/4) | — | — |
| H3 `596ac7f` | Field present but never set. `ui.game.*`: 212 run, 7 failed: AC-9 and AC-11 "noWinningMove expected to be false"; AC-13 and 23f's negative "an Undecided answer must not raise the banner"; AC-21 "hintUndecided expected to be true"; AC-15 and AC-14 precondition `IllegalStateException`s. AC-10 and AC-12 passed (guards) | 212/212. The Check at `596ac7f` was green in 11m21s: app 1075, domain 286, sweep, build-logic 54 | AC-9 (the rewrite of `aHintWhoseSearchMissesItsBudgetIsAlsoFree`), AC-10 to AC-15, AC-21, 23f's negative |
| H3a `197f9f8` | Red by mutation (A2.4 item 7). AC-2c reading `golden[depth+1]`: "expected: Found((5,2)->(4,2)->(3,2)) / but was : Found((3,1)->(3,2)->(3,3))". Walks under ROTATION_180: "Move((7,3)->(6,3)->(5,3)) must be legal…". Spares under TRANSPOSE: "Move((5,1)->(5,2)->(5,3)) must be legal…" | domain 288/288; staleness 10/10 | walks A–C with the three preconditions; the spares test; AC-2b on walk A; AC-2c; AC-11 on walk A |
| H4 `45f2b96` | **Stage 0** (empty string, bare leaf, not wired): 21 run, 17 failed. 23b: "but was an empty string". The leaf floor: "expected to be at least: 48.0 / but was: 36.0". The rest: no node. **Stage 1** (bare leaf drawn on the flag): 21 run, 15 failed. A2.2's named red: "a tap on the notice must reach nothing on the board / expected to be empty / but was: [Position(row=4, col=0)]". Also: no merged text (23a), "missing [Dismiss]", "LiveRegion … but was : null", never leaves (23e ×5, 23g ×2), shown under the banner (23h) | 21/21; `ui.game.*` 232/232; ktlint, detekt and `dpSpLiteralCheck` pass | `GameScreenHintNoticeTest`: 23a, 23b, 23c ×7, 23d ×2, 23e ×5, 23g ×2, 23h, 23i. Two 21a rows |
| H4b `7201742` | `NoWinBanner.kt` untouched: 2 run, 1 failed: "the live region on "No winning move — undo or restart" / expected: Polite / but was : null" | 2/2, and the notice tests 20/20 | `theBannersMessageIsAPoliteLiveRegionAndItsActionsAreOutsideIt`, over both messages. The existing test is unedited |

**Mutations.** Each was applied by script, run, then reverted with a `cmp`-verified copy:

| Mutation | Test that failed | Failure |
|---|---|---|
| A Hint cue played on raise | 23i | "…was: [Hint]" |
| The dock shows Working while undecided | 21a | the Hint control is not found Available |
| The notice moved 60dp into the dock | 23c | "less than: 504.0.dp / but was: 532.0.dp" |
| No key per raise | 23d | "expected: 3 / but was: 2" |
| `remember` instead of `rememberSaveable` | both 23g tests | "found '1' node" |
| The accessibility timeout ignored | both lengthened-display 23e tests | — |
| Reduced motion ignored | the animations-off 23e test | — |
| (H4b) the live region widened to the whole column | 23k | "the live regions on and above the Undo action / … but was: [Polite]" |

**Hollow tests I found and fixed.** With the clock paused, `StateRestorationTester` never recreated anything: the dispose and the re-emit cancelled out with no frame between them. So both 23g tests passed against `remember`. The fix is test-only: `recreateTheScreen()` runs the clock for the restore alone. Idling then advances the entrance frames but never the 5 s display, which is why the `remember` mutation now fails.

## Checks
- `./gradlew --max-workers=4 check` at `7201742` — **pass** (BUILD SUCCESSFUL in 11m 7s, 23:09:46–23:20:53).
  - uptime before: load 15.97 24.78 31.71; after: 3.22 7.19 17.95.
  - Executed: `:app:testDebugUnitTest` 1096 tests in 100 classes, 0 failed, 0 skipped (ui.game 233); `:domain:depthSweep` 1/1; app lint; the literal checks.
  - Up to date with unchanged inputs: `:domain:test` 288/0 (domain is unchanged since `197f9f8`), build-logic 54/0, detekt, ktlint.
- `:domain:depthSweep` (inside the Check) — pass. The maximum per call stayed under the 3 s cap despite the load:
  - `english33 k=4 examined=373 (canonical=51) book-served=369 authoritative-dead=4 solver-fallback=0 (found=0 undecided=0 proven-dead=0) maxPerCall=14ms`
  - `european37 k=4 examined=609 (canonical=312) book-served=609 authoritative-dead=0 solver-fallback=0 (found=0 undecided=0 proven-dead=0) maxPerCall=3ms`
  - `asymmetric39 k=4 examined=461 (canonical=233) book-served=461 authoritative-dead=0 solver-fallback=0 (found=0 undecided=0 proven-dead=0) maxPerCall=3ms`
  - `diamond41 k=4 examined=785 (canonical=107) book-served=345 authoritative-dead=0 solver-fallback=440 (found=0 undecided=440 proven-dead=0) maxPerCall=1524ms`
  - `wiegleb45 k=3 examined=101 (canonical=16) book-served=97 authoritative-dead=0 solver-fallback=4 (found=0 undecided=4 proven-dead=0) maxPerCall=1500ms`
- Item 3: `./gradlew --max-workers=4 :domain:compileKotlin --rerun :domain:compileTestKotlin --rerun :app:compileDebugKotlin --rerun :app:compileReleaseKotlin --rerun 2>&1 | grep -E '^w: '` — all four compile tasks executed; 21 lines in total:
  - 20 in `app/src/main`: 10 warnings × debug and release. Six are `hiltViewModel` deprecations in Route files; four are annotation-target notices in `ChallengeProgress.kt`, `ClassicLevelSource.kt`, `InterstitialCadenceStore.kt` and `TutorialViewModel.kt`. All predate this branch, none is in a file it touches, and the set is the same as at `596ac7f`.
  - 1 Kotlin daemon notice.
  - 0 in `domain/` or `src/release/`.
- Item 4: `git grep -n "firstWinningMove" -- domain app` — 1 hit, the frozen `domain/src/test/.../golden/solutions/AuthoringSolver.kt:250`; 0 elsewhere.
- Item 5: `git grep -nE "noWinningMove = true" -- app/src/main` — exactly 1, at `GameViewModel.kt:1402` (ProvenDead).
- Item 6: `git grep -nE "hintUndecided = true" -- app/src/main` — **2 hits**: `GameViewModel.kt:1406` (Undecided) and `GameScreenPreviews.kt:64`, the preview state §3.5 asks for. The ViewModel has exactly one.
- Item 7: `hintUndecided = false` 3, `noWinningMove = false` 3, paired in the same `copy`s (`:932/933`, `:1282/1283`, `:1360/1361`).
- Item 8: `git grep -n "nowin_message" -- app/src/main`:
  - code hits are as on develop: `GameScreen.kt:250` (develop `:241`) and `strings.xml:108`;
  - comment hits are `strings.xml:278` (develop `:271`) and two in `NoWinBanner.kt`'s KDoc (`:32`, and `:34`, which is the sentence A1.2 asks for).
- Item 9: the `showRescueBanner`/`hintUndecided` grep — 0 hits. The gate is structural: banner `if`, notice slot in the `else`.
- §6 commands, each run with `develop...HEAD`:
  - Generator and golden — empty.
  - Books and their reader — empty.
  - The `SolverBudget.kt` constants, `Solver.kt` constants and ViewModel `markNow|hasPassedNow` greps — empty.
  - `platform/` and `domain/.../assist` — empty. The ViewModel's hunks end at new line 1409; the five ad functions start at 495, 1452, 1483, 1509 and 1566, and no hunk falls inside them.
  - `strings.xml | grep -E '^-'` — prints only the diff header `--- a/app/src/main/res/values/strings.xml`, because the file now has additions. `grep -E '^-[^-]'` prints nothing, so no line was deleted.
  - A1.2's `NoWinBanner.kt` command — empty.
  - `data`, `di`, `ui/components` — empty.
  - The suppression grep, code files only — 5 hits: the edited `LongMethod` comment line, and 4 uses of the spec's "design baseline" in test KDoc. There is no `@Ignore`, `!!`, `ignoreFailures` or lint baseline.
- §9 tripwire, both lines — empty.
- Item 12: ui.game 233/0 (in the Check's run; 232/0 in the filtered run at `45f2b96`); `:domain:test` 288/0.
- `git merge-tree --write-tree --name-only develop HEAD` — no conflict. Develop is at `c5f2c54`, with docs-only changes since `0582e3f`.
- The `@Config` check: with the SDK 34 pin removed, Robolectric fails with "Failed to create a Robolectric sandbox: Android SDK 36 requires Java 21 (have Java 17)". The pin was restored.

## Credit (A2.4)
My finding that each opening book holds every golden-line state, so following Hint from the start never reaches the solver, is the same as the tech-lead's §16 #22. The ruling's Amendment 1 rests on it, and so does the spec's loop-2 fix to 23j (F-1). Measured consecutive book answers from each start: English-33 31 moves, European-37 35, Asymmetric-39 37, Diamond-41 38, Wiegleb-45 43.

## Device sequences for the qa-engineer
**Notation**, as in the ruling: (row, col), 0-based, row 0 at the top and column 0 at the left. Each jump reads from → over → to. The centre is (3,3) on English-33 and (4,4) on Diamond-41 and Wiegleb-45, and each start has only its centre empty. These are the sequences the tests replay.

**AC-18a (control, no sequence):** on Wiegleb-45, from the untouched start, follow Hint for the level's free hints: **3** (`AssistAllowanceLimits.FREE_HINTS`). Every hint should show a move at once, with no notice and no banner.

**AC-18b, AC-18c and AC-18d: the ruling's walks, copied verbatim.** Play each walk by hand from the untouched start, with no Hint until the fourth jump. Then tap Hint. Restart between walks.

| Walk | Symmetry | The four jumps | Empty holes afterwards |
|---|---|---|---|
| **A** | left–right mirror, col → 8 − col | (2,4)→(3,4)→(4,4); (0,4)→(1,4)→(2,4); (3,2)→(3,3)→(3,4); (3,0)→(3,1)→(3,2) | (0,4), (1,4), (3,3), (3,1), (3,0) |
| **B** | 180° rotation | (6,4)→(5,4)→(4,4); (8,4)→(7,4)→(6,4); (5,2)→(5,3)→(5,4); (5,0)→(5,1)→(5,2) | (8,4), (7,4), (5,3), (5,1), (5,0) |
| **C** | transpose, (r,c) → (c,r) | (4,2)→(4,3)→(4,4); (4,0)→(4,1)→(4,2); (6,3)→(5,3)→(4,3); (8,3)→(7,3)→(6,3) | (4,0), (4,1), (5,3), (7,3), (8,3) |

**Walk A in words, for the device:**
1. The peg two above the centre jumps down into the centre.
2. The top peg of that arm jumps down two, into the hole the first jump's peg started from.
3. In the row just above the centre, the peg two left of the middle column jumps right, into the middle hole of that row. Hint would show the same target hole approached from the right; the walk takes it from the left.
4. The leftmost peg of that row jumps right two, into the hole the third jump's peg started from.

**Four spare walks, already verified.** The test `everyOtherSymmetryImageOfTheGoldenOpeningIsAWinnableBookMiss` proves each one is a book miss, not the golden depth-4 state, and winnable. Use them only under the ruling's replacement rule.

| Symmetry | The four jumps | Empty holes afterwards |
|---|---|---|
| top–bottom mirror | (6,4)→(5,4)→(4,4); (8,4)→(7,4)→(6,4); (5,6)→(5,5)→(5,4); (5,8)→(5,7)→(5,6) | (8,4), (7,4), (5,5), (5,7), (5,8) |
| anti-transpose | (4,6)→(4,5)→(4,4); (4,8)→(4,7)→(4,6); (2,5)→(3,5)→(4,5); (0,5)→(1,5)→(2,5) | (4,8), (4,7), (3,5), (1,5), (0,5) |
| 90° clockwise | (4,6)→(4,5)→(4,4); (4,8)→(4,7)→(4,6); (6,5)→(5,5)→(4,5); (8,5)→(7,5)→(6,5) | (4,8), (4,7), (5,5), (7,5), (8,5) |
| 270° clockwise | (4,2)→(4,3)→(4,4); (4,0)→(4,1)→(4,2); (2,3)→(3,3)→(4,3); (0,3)→(1,3)→(2,3) | (4,0), (4,1), (3,3), (1,3), (0,3) |

**AC-19, English-33 book-proven dead end:**
- Jumps: (1,3)→(2,3)→(3,3); (4,3)→(3,3)→(2,3); (3,1)→(3,2)→(3,3); (2,3)→(3,3)→(4,3).
- Result: empty holes (1,3), (2,3), (3,3), (3,1), (3,2); 28 pegs and 6 legal moves left.
- Then Hint shows "No winning move — undo or restart" at once.

**AC-20, Diamond-41 gap (depth 3):**
- Jumps: (2,4)→(3,4)→(4,4); (2,2)→(2,3)→(2,4); (4,2)→(3,2)→(2,2).
- Result: empty holes (2,3), (3,2), (3,4), (4,2); 37 pegs.
- Then Hint should show the notice, never "No winning move". On the host it ends Undecided at 1.5 s.

**AC-21:**
1. Run AC-18a, which spends the 3 free hints.
2. Restart. A restart does not refill the allowance (D-04, `resetToInitial`).
3. Play walk A and tap Hint.

Expected: the notice appears and no ad offer does. A walk that answers with a move while the hints are spent gets the normal offer; that is not an AC-21 failure.

**23k Stuck sequence (English-33, 26 pegs):**
- Jumps: (1,3)→(2,3)→(3,3); (4,3)→(3,3)→(2,3); (3,1)→(3,2)→(3,3); (3,4)→(3,3)→(3,2); (3,6)→(3,5)→(3,4); (6,3)→(5,3)→(4,3).
- Result: empty holes (1,3), (3,1), (3,3), (3,5), (3,6), (5,3), (6,3); no legal move is left.
- The sixth jump raises the stuck banner, which should speak "No moves left — undo or restart".

## Open issues / risks
- **SDK 34 pin.** The brief says "add no `@Config(sdk = …)`", but `GameScreenHintNoticeTest` pins SDK 34, as all 54 Robolectric classes in the repo do. Without the pin it cannot run (see the `@Config` check above). The code-auditor decides.
- **Beyond the brief's plan:**
  - commit `fecf13b`;
  - the extra solver test;
  - two KDoc sentences in `Pagoda.kt`;
  - H3a amended once (`cb5463f` → `197f9f8`, to fix a test name split across lines), before anything was committed on top of it;
  - the bottom-edge test also asserts §4.4.2's ≥ 32dp room above the dock (measured: 32dp).
- **GameScreen:** the banner's `if` gains an `else` branch for the slot, plus comments. The banner's own lines are unchanged.
- **Command artifacts:** §6's `strings.xml` grep matches the diff header. Suggest `grep -E '^-[^-]'`.
- **23c's wide window:** the premise that the notice is one line at `w600dp` is false (F-2): it is two lines there. The 48dp floor is proven on the leaf with a short message.
- **Behaviour to note:**
  - A configuration change while the notice is up restarts its display time. Once gone, it never comes back (23g).
  - A touch during the 160 ms fade still lands on the notice, so it still reaches nothing beneath.
  - In a static render, the screen preview's notice can show at alpha 0 because of `clayEntrance`, like every entrance surface in the app. The leaf previews show it settled.
- **Pre-existing test fragility:** in `GameViewModelResumeTest`, a failed precondition with a live ticker hangs `runTest`'s drain instead of failing. I hit this at H3, confirmed it with jstack, and killed only this worktree's worker (PID 12606). Only my new test is wrapped in try/finally; the other tests in that class share the pattern. To route.
- **Harness lessons for the retro:**
  - With a paused clock, `StateRestorationTester` is a no-op.
  - Writes from a tap handler need `Snapshot.sendApplyNotifications()`.
  - LEGACY graphics fakes text metrics, so layout tests need `GraphicsMode.NATIVE`.
- **Commit trailers:** each commit ends with `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, per `.claude/rules/gitflow.md` and the dispatch. The actual model is on the "Ran on" line.
- **Not run by me:** no device or emulator. 23j, 23k's device half and AC-18 to AC-21 are QA's.
- **H5:** not committed by me (the harness blocked the report file). This text is the report.

## Handoff
Next: **code-auditor** — review `git diff develop...HEAD -- app domain` (29 files) against the brief (with Amendments 1 and 2), the ruling and ux-spec D-04f/D-04g. Read in particular:
- the `GameUiState` hunk (one defaulted field);
- the ViewModel split;
- the A1.2 banner hunk;
- the `LongMethod` comment line;
- the SDK 34 pin;
- the test clock handling (`recreateTheScreen`, `deliverTheTapsWrites`).

Next: **qa-engineer** — on a device, using the sequences above:
- AC-18a to AC-18d, AC-19, AC-20 and AC-21;
- 23j: items 1–5 on walks A, B and C and on AC-20, then items 6–7 once on walk A;
- 23k's device half: AC-19's Hint speaks "No winning move — undo or restart"; the Stuck sequence's sixth jump speaks "No moves left — undo or restart"; swiping forward goes message → Undo → Restart → Home → dock. Record the traversal order.

Next: **tech-lead** — sign off after both reviews pass, and rule on the SDK 34 pin and the §6 command.

Next: **producer** — commit this report as H5.
