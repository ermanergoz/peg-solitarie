# tech-lead report — hint-undecided (brief for the android-game-dev)
Date: 2026-10-21
Branch: bugfix/hint-undecided (committed by the producer)
Skills loaded: ergoz-soft:standards (role tech-lead). The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch. I also read the product's `.claude/rules/lessons.md`. Further skills: android-architecture, android-gradle-logic.
Ran in parallel with: the ux-designer's ruling on the Undecided notice (`docs/ux-spec.md`, this branch); Gradle builds in two other worktrees
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the tech-lead is pinned to fable at xhigh (`agents/tech-lead.md`). This brief ran on Opus under the founder's Q-019 (answered 2026-10-17): Opus for every Fable-pinned role until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none. The snapshot holds 0 active lessons, and the product has no `.planning/recall/RECALL.md`.

## Summary
- R1 is one type change carried through four layers. `Solver.decide` returns `SolverOutcome` and `GetHintUseCase` returns `HintOutcome`, each Found / ProvenDead / Undecided. The ViewModel maps ProvenDead to today's banner and Undecided to a new `hintUndecided` flag, and the screen renders the ux-designer's notice from that flag.
- The solver does not tell a deadline from a cancellation, and must not. Any stop it did not prove is Undecided. The ViewModel already tells the two apart: a superseded job never resumes past `withContext`, and the board guard is a second lock. A deadline on an unchanged board is therefore the only latched stop that reaches the screen.
- Five commits, red-first: each reproduces today's fold in a test before removing it. No generator, book, budget, deadline, ad-funnel or start-path change. No 15e sitting.
- Merge order: this branch, then check-gates, then audio-review, then home-chip-arrival. The branch that merges second merges `develop` in and re-runs the other branch's named tests.
- **Amendment 1 (2026-10-21):** criterion 23k rides this branch, as commit H4b. The seam is the committed spec's: one Boolean, no tick, no announcement string, an inline notice.

## Amendment 1 — 2026-10-21: criterion 23k, and the seam aligned with the committed spec

*Read this before §0.* It overrides the sections it names. The original text stays as the record, and each overridden line is marked "(Amendment 1)". Inputs:
- `docs/ux-spec.md` at `f221ae4`: D-04f, §4.4.2, §4.4.1's two new rows, §6, §7 R-7, §8, and criteria 23a–23k;
- `.planning/reports/hint-undecided/ux-designer.md`, open issue 3.

### A1.1 Ruling: 23k rides `bugfix/hint-undecided`
1. **This branch creates the gap that 23k closes.** Once the Undecided notice speaks, a silent rescue banner makes the proof the one answer TalkBack does not give. Shipping the notice without 23k would ship a new accessibility defect inside the fix.
2. **It is small and uncontended.** It is one semantics property and one KDoc correction in a 111-line leaf. No other branch touches `NoWinBanner.kt` or `NoWinBannerTest.kt`: I checked audio-review, candy-backdrop, ux-spec-folds and check-gates, both committed and in their working trees.
3. **The file needs an edit on this branch anyway.** Its KDoc (`NoWinBanner.kt:29-31`) says the caller passes `nowin_message` "when the Solver confirms no winning move (or a budget miss, D-04)". H3 makes that false, and a freeze would leave a false comment behind.
4. **A separate branch would cost too much for one line**: a second review chain, Check and device sitting before `release/3.0.0`. QA already visits AC-19's dead position for 23j item 6.

The one effect outside the hint path is that the stuck banner, raised by a jump, gains its first polite announcement. D-04f rules it, so it rides too.

### A1.2 §6 amended: what `NoWinBanner.kt` may change, and nothing else
- **One polite live region on the message `Text` only**: `modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },` with its imports. The three `TextButton`s stay outside the region, as 23k requires.
- **The KDoc:**
  - drop "(or a budget miss, D-04)";
  - state that `nowin_message` now means a proven dead end (GD-HT-1);
  - add one sentence on the announcement (D-04f, 23k).
- **Unchanged:** the signature, `NOWIN_BANNER_TEST_TAG`, the `Surface`, its colours, shape, shadow, minimum height and padding, the three actions and their order, `nowin_message`, `stuck_message`, and the caller (`GameScreen.kt:237-251`). No `announceForAccessibility`.
- **Check (must print nothing).** It replaces the `NoWinBanner.kt` command in §6's banner row; the `strings.xml` deletion check in that row stands.

      git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/ui/game/NoWinBanner.kt | grep -E '^[-+][^-+]' | grep -vE '^[-+]\s*\*|^\+import androidx\.compose\.ui\.semantics\.|^\+\s*modifier = Modifier\.semantics \{ liveRegion = LiveRegionMode\.Polite \},?$'

### A1.3 Where 23k goes: commit H4b, right after H4
- **The commit:** `feat(hint): announce the rescue banner politely`. It holds only A1.2's edits and the test below, kept apart so that the auditor reads the banner hunk alone and a revert of the announcement cannot take the notice with it.
- **The test (Compose half):** `NoWinBannerTest.kt`, new `theBannersMessageIsAPoliteLiveRegionAndItsActionsAreOutsideIt`.
  - The node showing the message carries `SemanticsProperties.LiveRegion == LiveRegionMode.Polite`.
  - None of the Undo, Restart and Home nodes carries a live region, and none has an ancestor that does.
  - Red first: write the test before the line and watch it fail on the missing property.
  - The existing `rendersTheMessageAndInvokesEachActionExactlyOnce` stays unedited.
- **The device half (qa-engineer, with 23j):**
  - AC-19's position speaks "No winning move — undo or restart";
  - a jump that leaves no legal move speaks "No moves left — undo or restart".
  - Your report adds a move sequence to such a Stuck position on a Classic board, beside the AC-18 to AC-20 sequences (§7).

### A1.4 The seam, resolved to the spec (R-7, §8)

| The brief's option | Resolution |
|---|---|
| `GameUiState.hintUndecided: Boolean` | **Kept**, the single seam, exactly as §3.4 and R-7 describe it |
| `hintUndecidedTick: Int` | **Not added.** The form is inline. Every clause about a tick is void: §3.4's last bullet but one, §3.5's one-shot bullet, H3's "plus a tick…" in §5, §8's conditional row, the tick sentences in §10.1 and §10.3, and the Open-issues bullet about H4. |
| `hint_undecided_announcement` | **Not added.** TalkBack speaks the visible string (§4.4.2, "What is spoken is what is shown"). |
| `hint_undecided_message` | **Added**, with the en-US text verbatim from spec §8 and the apostrophe escaped (`couldn\'t`). No `values-tr`: the app has no Turkish locale, and the tr-TR text is copy of record only. |
| Form | **Inline:** `HintUndecidedNotice` in `ui/game/HintUndecidedNotice.kt`, tag `HINT_UNDECIDED_NOTICE_TEST_TAG`, a polite live region, and **not an M3 `Surface`**, so that touches pass through to the board. §4.4.2's layout table governs everything else. |

### A1.5 Further alignment with the spec (binding)
- **Timing state lives in the screen.** The display timer (`calculateRecommendedTimeoutMillis`), the saveable "this raise has timed out" memory and the raise count belong to the screen (§4.4.2, R-7). Keep them in `GameScreen`, or in a small remembered holder it owns.
  - The leaf `HintUndecidedNotice(message, modifier)` stays stateless.
  - Nothing is reported back to the ViewModel, and `GameUiState` gains no further field.
- **Tokens:**
  - `hint_notice_glyph_size` is an addition to `values/dimens.xml`, which §9's tripwire allows.
  - `HINT_NOTICE_DISPLAY_MS` and `HINT_NOTICE_EXIT_MS` go in `ui/game/GameAnim.kt`, which is off the start path.
  - `clayEntrance` and `ClayAnim` in `ui/theme/` are **reused, not edited**: an edit there trips §9.
- **Test placement:**
  - **23i (silence)** goes in `GameScreenHintNoticeTest.kt` with `FakeSoundPlayer`, **not** in `GameScreenSoundTest.kt`, which audio-review is editing in its working tree.
  - **21a:** add §4.4.1's two new Hint rows (Undecided and Cancelled, both Available) to `DockStateLanguageTest.kt`'s table.
  - **23f** extends H3's AC-13 test with its negative half. A selection, a deselection, a rejected tap, a Show-moves toggle, and an Undo or Restart tap that only opens a dialog must leave `hintUndecided` raised. That already holds: `performUndo` and the reset run only when the board really moves.
  - **23g** pairs with AC-15 (`StateRestorationTester` plus the resume test).
  - **One test where two ids overlap:** where a ruling AC and a spec criterion assert the same property (AC-16 and AC-17 with 23a), write one test named for the criterion and cite both ids in its KDoc.
- **H4's JVM list** becomes 23a–23i plus 21a's two rows. **23j** and **23k's device half** are the qa-engineer's. §14 of the spec asks that each test be watched failing once, the same red-first rule as §5.
- **`GameScreen`'s `LongMethod` comment** names "Undecided notice" (§3.5).
- **The spec has landed.** H4 no longer waits; the gate row in §5 is satisfied at `f221ae4`.

## 0. Read first

- **The requirement is the game-designer's ruling**, decisions GD-HT-1 to GD-HT-6 and AC-1 to AC-21. It is **not on `develop` or on this branch**. It sits on `feature/phase-14-release-docs` at `7929d1b`:
  `git show feature/phase-14-release-docs:.planning/reports/phase-14/game-designer-hint-timeout.md`
- `docs/tech-design.md` is not on `develop` either. Read §7.2, "What 'No winning move' means today", with:
  `git show feature/phase-14-release-docs:docs/tech-design.md`
  If the producer merges release-docs into `develop` before you finish, merge `develop` into this branch so reviewers have both files on the tree.
- This brief rules the mechanism. Where it and the ruling disagree on what the player sees, the ruling wins. Where they disagree on code, stop and report through the producer.
- Required skills before any edit: `compose-skill` (rules/compose.md: every Compose edit), `compose-ui`, `android-viewmodel`, `android-coroutines`, `android-testing`, and `superpowers:test-driven-development`.
- Line numbers below are `develop` at `0582e3f`.

## 1. The type, and where it lives

Two sealed types, both in the pure `:domain` module. Nothing new in `:app` except one UI-state field (§3.4).

| Type | File | Shape | Who sees it |
|---|---|---|---|
| `HintOutcome` | `domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/HintOutcome.kt` (new) | `sealed interface HintOutcome { data class Found(val move: Move); data object ProvenDead; data object Undecided }`, each value implementing `HintOutcome` | `GetHintUseCase`'s return type. The **only** outcome type `:app` imports. |
| `SolverOutcome` | `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverOutcome.kt` (new) | The same three values, implementing `SolverOutcome` | `Solver.decide`'s return type. Read by `GetHintUseCase` and the solver tests only. |

**Why `usecase/` for the type the UI sees.** `HintOutcome` is the use case's contract: it merges three sources (the recorded line, the book, the solver), and its ProvenDead has two provenances that the solver's does not (the book's proof as well as an exhausted search). The ViewModel depends on the use case and on nothing in `solver/`, except `SolverBudget.DEFAULT.deadline`, which it reads today.

**Why a second type in `solver/`.** The dependency runs `usecase → solver`, never back, so the solver cannot return a `usecase` type. Keeping the solver's own type also lets R2b (3.1) make `SolverOutcome.Found` carry the solver's whole line without touching the UI contract.

**Rejected:**
- One shared type in `model/`. The diff is smaller, but it couples the UI to the solver's result and puts a search verdict in the board-model package.
- `Move?` plus a `proven: Boolean`. That is two fields that can disagree, and the fold this defect came from.
- `Undecided(reason)`. Nobody reads a reason. The player is told the same thing for a deadline, a node ceiling and spent attempts, and the sweep's counts do not need it. R5 (3.1) may add one when it measures.

**Names.** `HintOutcome`, `SolverOutcome`, `Solver.decide` and the field in §3.4 are binding, because the ux-designer's spec and the reviewers cite them. The ruling leaves the type name to you. I am taking that choice so that the seam in §8 has names; raise it through the producer if you see a reason to differ.

## 2. Deadline versus cancellation

**Today.** The search has one latch, `WinningLineSearch.cancelled` (`Solver.kt:193`, `:301-305`). One lambda sets it, `shouldCancel = { !isActive || deadline.hasPassedNow() }` (`GameViewModel.kt:1368`). It covers both a superseded job and the 1.5 s deadline, as the ruling says.

**Ruling: the solver reports what it proved, never why it stopped.** Every stop it did not prove is `Undecided`: the latch, `MAX_ATTEMPTS` spent, or the cumulative `maxNodes` ceiling reached. `:domain` has no `Cancelled` value.

**The ViewModel tells the two causes apart, and already does, structurally:**
1. **A superseded search is never observed.** Every board mutation calls `cancelHintSearch()` → `hintJob.cancel()` (`:347-355`), and so does leaving the screen (`viewModelScope`). `withContext` returns into the original context with kotlinx.coroutines' *prompt cancellation guarantee*: if the calling job was cancelled by the time it resumes, the result is discarded and `CancellationException` is thrown. So no line after `:1370` runs for a superseded search, whatever the domain returned.
2. **The board guard stays as the second lock** (`:1375-1378`), and it stays **ahead of** the new `when`.
3. **A deadline leaves the job active and the board unchanged.** The outcome, Undecided, is applied, and the notice shows. This is AC-11, and it is why a latched stop can never become silence (GD-HT-3).

A `Cancelled` domain value would be produced only for results that point 1 makes unobservable. It would be dead code, and it would invite a later mapping to silence, the ruling's "fourth, worse state".

**Rejected:**
- Splitting the lambda into `isSuperseded` and `deadlinePassed`. That puts the caller's concern into `:domain`, leaves two lambdas to keep in step, and changes nothing anyone can observe.
- Reading a clock in `:domain`. `:domain` stays clock-free. D-07's deadline lives in the ViewModel's lambda.

**Constraints:**
- The `shouldCancel` lambda and the `deadline` line (`:1354`) stay byte-identical.
- No `isActive` or `ensureActive()` branch after `withContext`.
- No `catch` of `CancellationException` (or of `Exception`/`Throwable`) anywhere on this path.
- The outcome `when` is exhaustive over the sealed type, with **no `else`**, so a future value is a compile error rather than a silent default. The same holds for the `SolverOutcome → HintOutcome` mapping.

## 3. The change at each layer

### 3.1 `Solver` — `domain/.../solver/Solver.kt`, new `SolverOutcome.kt`
- `fun decide(board: Board, budget: SolverBudget, shouldCancel: () -> Boolean = { false }): SolverOutcome` **replaces** `firstWinningMove`. The old function is deleted rather than deprecated: its lossy `Move?` is the root of the defect, and no caller remains after commit H2 (§5).
- The mapping:

| Situation (today's code) | Outcome |
|---|---|
| `board.pegCount <= board.spec.minimumPegs` (`:126`) | `ProvenDead`. Behaviour-preserving: a board at its target has no legal move. It is unreachable in play, because a Won board routes to Results; say so in the KDoc. |
| `!Pagoda.mayBeSolvable(board)` (`:126`) | `ProvenDead` (ruling, AC-5) |
| an attempt returned a line (`:159`) | `Found(line.first())` |
| an attempt exhausted its subtree with neither `cancelled` nor `hitBudget` (`:160`) | `ProvenDead` |
| the loop ended otherwise: the latch, `MAX_ATTEMPTS` spent, the cumulative `maxNodes` reached, or `maxNodes <= 0` so no attempt ran | `Undecided` |

- **Edit only `firstWinningMove`/`decide` and `searchWithRestarts`.** Hoist `provenUnsolvable` into a `var` outside the loop and resolve the outcome after it. Nothing inside `WinningLineSearch` or `VisitedSet` changes: the same seeds, caps, escalation, `CANCEL_MASK`, ordering and `lastVisited`.
- **KDoc, text only.** Replace the D-04 fold wording with the three outcomes in:
  - `Solver.kt`: the class KDoc (`:12-16`), the old function's KDoc (`:112-120`), the `MAX_ATTEMPTS` comment (`:70`) and the "answers 'no winning move'" sentence in `ATTEMPT_CAP_GROWTH`'s KDoc (`:79-80`), which becomes "gives up undecided";
  - `SolverBudget.kt`: `:11-13`, `:35-37` and `:86-87`.
- **No constant in `SolverBudget.kt` changes** (§6).

### 3.2 `GetHintUseCase` — `domain/.../usecase/GetHintUseCase.kt`, new `HintOutcome.kt`
- `operator fun invoke(...)` returns `HintOutcome`. The five parameters and their defaults are unchanged, so every `invoke(any(), any(), any(), any(), any())` matcher still matches.
- The mapping:
  - a trusted stored move (`:96`) → `Found(move)`;
  - `BookAnswer.NoWinningMove` (`:117`) → `ProvenDead`, still with **no** solver call;
  - otherwise `Solver.decide(...)`, mapped one-to-one in an exhaustive `when`.
- The single legality guard (`:92-97`) and the priority order are unchanged.
- KDoc: rewrite path 3 (`:30-34`) and `@return` (`:79`) for the three outcomes; the "three book outcomes" paragraph (`:36-43`) stays true.
- `di/DomainModule.kt` does not change: the constructor is the same.

### 3.3 `GameViewModel` — `app/.../ui/game/GameViewModel.kt`
- **`onHintRequested`** (`:1341-1403`):
  - The opening update (`:1344-1351`) also writes `hintUndecided = false`.
  - `val move` becomes `val outcome`. After the unchanged board guard, an exhaustive `when (outcome)`:
    - `is HintOutcome.Found` → today's non-null path verbatim (`:1392-1401`): stash, clear `solverThinking`, funnel, reveal.
    - `HintOutcome.ProvenDead` → `noWinningMove = true, solverThinking = false` (today's `:1385`), return.
    - `HintOutcome.Undecided` → `hintUndecided = true, solverThinking = false`, return. No `pendingHint`, no `enterAssistFunnel`, no allowance, no gateway, no `countAssist` (GD-HT-5; AC-14, AC-21).
- **Clear the flag wherever `noWinningMove = false` is written today:** `onHintRequested` (`:1347`), `performJump` (`:931`) and `performUndo` (`:1280`). The board-install sites and the restart rebuild from `GameUiState.initial(...)` or `loading()`, which default the field to false. Do **not** carry it forward there. This gives AC-13 and AC-15.
- **Not persisted.** The resume snapshot (`data/resume/`) is not in the diff (AC-15).
- **KDoc:** rewrite `onHintRequested`'s "A `null` result sets…" paragraph (`:1305-1313`) and the inline comment at `:1379-1383` for the three outcomes. The `pendingHint` KDoc (`:306`, "the non-null branch") becomes "the Found branch".
- **Unchanged:** `cancelHintSearch`, `enterAssistFunnel`, `onAcceptAdOffer`, `onDismissAdOffer`, `grantReward`, `revealPendingHint` (D-09 counting at `:1549-1551`), and `onUndo`, which is ADS-04's banner Undo.

### 3.4 `GameUiState` — `app/.../ui/game/GameUiState.kt`
- **New `val hintUndecided: Boolean = false`**, declared right after `noWinningMove`. Also list it explicitly in `initial()` and `loading()`, which name every field today.
- **`showRescueBanner` stays `stuck || noWinningMove`** (`:142`). The new flag must never enter it (AC-9, AC-16, AC-17).
- **KDoc:**
  - `noWinningMove` (`:52-55`): the last hint *proved* there is no winning move (the book's proof, an exhausted search, or a pagoda reject). A search that stopped unproven sets `hintUndecided` instead.
  - `solverThinking` (`:49-51`): cleared on a highlight, `noWinningMove` or `hintUndecided`.
  - A new `hintUndecided` entry, citing GD-HT-2: transient, cleared on jump, undo, restart, a new Hint and a board install, and never persisted.
- *(Void, Amendment 1: the spec's form is inline, so no tick is added.)* **Only if the ux-designer picks a one-shot form** (a snackbar), also add `hintUndecidedTick: Int = 0` under the `adMessageTick` rules (`:98-103`):
  - incremented with each Undecided;
  - carried forward at all four board-install sites;
  - guarded `> 0` at the consumer.
  The Boolean still exists: it is the replayable state the tests assert. With an inline form there is no tick.
- By construction, at most one of `noWinningMove` and `hintUndecided` is true: every request clears both and every result sets one. A stuck board never yields Undecided, because a position with no legal move is an exhausted search.

### 3.5 UI — `GameScreen.kt`, `GameScreenPreviews.kt`, `strings.xml`, and a new composable if the form is inline
- **Build what `docs/ux-spec.md` specifies** for the notice: its form, text, placement and announcement. Choose no word and no form yourself. Commit H4 (§5) waits for the ux-designer's spec commit on this branch. *(It landed at `f221ae4`: D-04f, §4.4.2. Amendment 1.)*
- **`NoWinBanner` and its message choice** (`GameScreen.kt:237-251`, `:241`) are unchanged. The banner stays gated by `showRescueBanner`.
- **Inline form** *(the spec's form; §4.4.2's layout table governs, and the notice is not an M3 `Surface`. Amendment 1)*: a new stateless composable, `HintUndecidedNotice(message: String, modifier: Modifier = Modifier)`, in `ui/game/HintUndecidedNotice.kt`.
  - It has no actions.
  - Dimensions come from `dimens.xml` and colours from the theme.
  - Its semantics are as the spec says. If the spec wants an announcement, prefer a live region (`Modifier.semantics { liveRegion = … }`) to any View-level announce call. No API that compiles with a deprecation warning: after C7 that fails `:app`'s main compilation.
  - Light and dark `@Preview`s (rules/compose.md). Add a notice state to `GameScreenPreviews.kt`.
  - A test tag `HINT_UNDECIDED_NOTICE_TEST_TAG`.
- *(Void, Amendment 1.)* **One-shot form:** a `LaunchedEffect` keyed on `hintUndecidedTick`, on the existing `ClaySnackbarHost`. Do **not** reuse `adMessage`/`adMessageTick`: that channel is the ad's D-11 message (`GameViewModel.kt:1528-1532`).
- **`GameScreen`'s `@Suppress("LongMethod")` comment** (`:142`) lists every surface the composable hosts, and its KDoc (`:137-140`) says to amend that list. Add the new surface to it. Add no second suppression and no baseline entry.
- **Strings:** see §8. `app/src/main/res/values/strings.xml` is the only strings file (`values-night` holds no strings).

## 4. Tests: what to rewrite, what to add, where

Test names below are the ruling's. Deterministic only: forced cancels, node budgets and `TestTimeSource`, never a wall clock (rules/testing.md). Robolectric stays at SDK 34, so add no `@Config(sdk = …)`.

### 4.1 Existing tests that must change

| File | Site | Change |
|---|---|---|
| `GameViewModelAssistOrderingTest.kt` | `aHintWhoseSearchMissesItsBudgetIsAlsoFree` (`:162-183`) | **Rewrite as AC-9**, `anUndecidedHintIsFreeAndNeverRaisesTheNoWinningMoveBanner`. The stub returns `HintOutcome.Undecided`. Assert `noWinningMove` false, `showRescueBanner` false, `hintUndecided` true, `solverThinking` false, no offer, `gateway.requests == NO_REQUESTS`. Delete the old comment, whose premise the ruling voids. |
| `GameViewModelAdClockTest.kt` | `alwaysFindsAMove()` (`:397-403`) | The stub wraps its move: `RulesEngine.legalMoves(firstArg()).firstOrNull()?.let { HintOutcome.Found(it) } ?: HintOutcome.ProvenDead`. A no-move board kept today's banner, so ProvenDead preserves the meaning. |
| `GameViewModelTest.kt` | `countingHint` stub (`:982-987`) | Same wrapping |
| `GameViewModelHintStalenessTest.kt` | `recordingHint` stub (`:248-254`) | Same wrapping |
| `GetHintUseCaseTest.kt` | every assertion on a use-case result: `:39-60`, `:63-72`, `:81`, `:96`, `:115`, `:147`, `:181`, `:208`, `:243` | `isNotNull()` becomes "is `Found`", keeping each legality or equality check on `.move`. `OpeningBook.lookup(...)` preconditions (`:288`) are unchanged. |
| `GetHintUseCaseTest.kt` | `aSolverReturningNullYieldsNull` (`:153-164`) | Becomes `aBoardWithNoLegalMoveIsProvenDead` (same board) |
| `GetHintUseCaseTest.kt` | `SolverSpy` KDoc (`:296-305`) | "would force Solver to `null`" becomes "would force an Undecided" |
| `GetHintUseCaseTest.kt` | `anOffBookClassicPositionStillReachesTheSolver` (`:282-292`) | Keep it. Also assert the outcome is `Undecided`. |
| `SolverTest.kt` | the 8 `firstWinningMove` call sites | Call `decide`. `returnsNullOnAGenuinelyUnsolvablePosition` becomes `aGenuinelyUnsolvablePositionIsProvenDead`, and `returnsNullWhenAlreadySolved` becomes `aBoardAlreadyAtItsTargetIsProvenDead`. `!= null` at `:67` becomes `is SolverOutcome.Found`. |
| `SolverBudgetTest.kt` | `:67-72` (`isNotNull`) and `:97-98` (`isNull`) | `Found`, and **`ProvenDead`**. This strengthens the test: those samples run exhaustively, with no deadline. |
| `PagodaTest.kt` | `:45` | Comment text only |
| `DepthSweepTest.kt` | classification (`:187-221`), `BoardReport`, `render()`, and KDoc clause 3 (`:38-39`) | **AC-8**, see §4.3 |

**These keep their meaning and must stay green unedited (AC-7, AC-10's regression guard).** Each runs the real use case on a small, provably dead board, which now yields ProvenDead:
- `GameViewModelTest.hintOnAnUnsolvablePositionSetsNoWinningMove` (`:952`) and `noWinningMoveIsUndoneBackToAPlayableRescueFreeBoard` (`:766`);
- `GameViewModelTelemetryTest.aHintSearchThatFindsNoWinningMoveCountsNothing` (`:254`);
- `GameViewModelAllowanceTest.theRescueBannersUndoRaisesTheSameOfferOnceUndosAreExhausted` (`:557`).

If one of them turns Undecided, its board is not as small as it looks. Stop and report; do not loosen the test.

### 4.2 New tests

| AC | Where | Notes |
|---|---|---|
| Solver layer: `aSearchCancelledOnItsFirstPollIsUndecided`, `aSearchThatSpendsItsCumulativeCeilingIsUndecided`, `aBudgetThatAllowsNoAttemptIsUndecided` (`maxNodes = 0`) | `SolverTest.kt` | These pin the mapping where it lives. The use-case ACs pin it again end to end. |
| AC-1, AC-2, AC-2b, AC-3, AC-4, AC-5, AC-6 | `GetHintUseCaseTest.kt` | AC-4's board needs ≥ 1 legal move and Pagoda's admission, both asserted as preconditions, and a never-cancelling spy that records **polls > 0**. Otherwise AC-4 tests the pagoda path again. AC-5 asserts **0 polls**. Build AC-2's and AC-3's positions from explicit move sequences from the start: QA replays them on a device (AC-18, AC-19). |
| `aDiamond41GapPositionIsNeverReportedDead` (forced cancel → Undecided, book miss asserted) | `GetHintUseCaseTest.kt` | Not in the ruling's list. It is AC-20's domain half and gives QA its move sequence. |
| AC-10 | `GameViewModelAssistOrderingTest.kt`, beside AC-9 | Stub returns `ProvenDead`; everything exactly as today. |
| AC-11 | `GameViewModelHintStalenessTest.kt` | It already parks the search on its `GatedDispatcher` (`:92`, `:488`). Hoist the inline `TestTimeSource()` (`:428`) into a field the test holds. Use the real `GetHintUseCase` on an off-book Wiegleb-45 position, with the book miss asserted. Request the hint, `runCurrent()`, then `timeSource += 2.seconds`, then `gate.releaseAll()`: the first poll sees the deadline passed. Assert the notice is up and the banner is not. |
| AC-12, AC-13 | `GameViewModelHintStalenessTest.kt` | AC-12: for each of jump, undo, restart and a second Hint, the parked search is released afterwards, neither flag is raised by it, and nothing is charged. AC-13: the notice, once up, is cleared by each of the four. |
| AC-15 | `GameViewModelResumeTest.kt` | Its restore machinery is already there |
| AC-21 | `GameViewModelAssistOrderingTest.kt` | Spend the free hints with its `exhaustFreeHints`; the stub returns `Undecided`; no offer, no gateway request |

Three private gated dispatchers already exist (`ParkingDispatcher`, `GatedDispatcher`, `OrderingGatedDispatcher`). Add no fourth copy and no new ViewModel test file.
| AC-14 | `GameViewModelTelemetryTest.kt`, beside `:254` | Its telemetry fakes are already there. Assert the allowance through the next hint's offer behaviour, as the allowance tests do. |
| AC-16, AC-17 | new `GameScreenHintNoticeTest.kt` | Render `GameScreen` from hand-built states. AC-17 covers `hintUndecided = true` with `noWinningMove = false`: neither the `nowin_message` text nor the Undo/Restart/Home rescue actions are present. The TalkBack half of AC-16 asserts the semantics the ux-spec names. |

### 4.3 `DepthSweepTest` (AC-8)
- **Classify** each position by outcome and polls:
  - 0 polls and `Found` → served;
  - 0 polls and `ProvenDead` → authoritative;
  - polls > 0 → deferred, split into `deferredFound`, `deferredUndecided` and `deferredProvenDead`;
  - 0 polls and `Undecided` → a **failure**: an Undecided must come from a search, and with the default budget a search with a legal move polls at its first expansion.
- **Assert:**
  - `served + authoritative + deferred == examined` (unchanged);
  - `deferredFound + deferredUndecided + deferredProvenDead == deferred`;
  - every `Found` move is legal (unchanged);
  - `deferred == DOCUMENTED_GAP` and `authoritative == PROVEN_DEAD` (unchanged).
- **Print** the three deferred counts in `render()`. **Do not pin `deferredUndecided`**: it depends on the host's speed under a wall-clock deadline.
- **The measured table** (`:50-56`) gains an Undecided column only from your own green sweep's printed report, with the host load recorded. Otherwise leave the table as it is.

## 5. Commit order, red-first

Each commit compiles and is green on its targeted tasks. **Red-first means reproducing today's fold in the new test before removing it**, so every red is the defect itself rather than a compile error. Record each red run in your report: the command, the failing test name, and the "expected … but was …" line.

| # | Commit (≤ 72 chars) | Contents | Red first | Green on |
|---|---|---|---|---|
| H1 | `refactor(hint): give the solver a three-way outcome` | `SolverOutcome`; `Solver.decide`; `firstWinningMove` kept for this commit only, as a one-line adapter over `decide` (`GetHintUseCase` still calls it); the `SolverTest`/`SolverBudgetTest`/`PagodaTest` migrations and the three new solver tests; KDoc | First implement `decide` as the fold, `firstWinningMove(...)?.let(::Found) ?: ProvenDead`, and watch the three Undecided tests fail with "was ProvenDead". Then split. | `./gradlew :domain:test --tests 'com.app.pegsolitaire.domain.solver.*'` |
| H2 | `refactor(hint): return a HintOutcome from GetHintUseCase` | `HintOutcome`; the use case; `firstWinningMove` deleted; `GetHintUseCaseTest` migrated, with AC-1 to AC-6 and the Diamond gap test; `DepthSweepTest` (AC-8); the four `:app` stubs (the three `answers` stubs in §4.1, plus `AssistOrderingTest:169`'s `returns null`, which becomes `returns HintOutcome.Undecided` until H3 rewrites that test); the ViewModel's `when` with **ProvenDead and Undecided both still raising `noWinningMove`**. That is today's behaviour, kept deliberately so this commit is a pure type migration. | First map `SolverOutcome.Undecided` to `HintOutcome.ProvenDead` in the use case and watch AC-1, AC-2, AC-6 and the Diamond gap test fail. Then map one to one. | `./gradlew :domain:test`, `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.game.*'`, and `./gradlew :domain:depthSweep` once (≈ 11 min, quiet host) |
| H3 | `fix(hint): never raise "no winning move" on an undecided search` | `GameUiState.hintUndecided` (no tick: Amendment 1); 23f's negative half; the ViewModel's Undecided branch and the three clearing sites; the KDoc; AC-9 (the rewrite) to AC-15 and AC-21 | Write AC-9, AC-11 and AC-13 on top of H2: they fail, because the banner is raised. Then split the branch. | `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.game.*'` |
| — | *(ux-designer's spec commit on this branch)* | H4 starts only after it lands. If it has not landed when H3 is green, report and wait. Do not draft words. **Landed at `f221ae4` (Amendment 1).** | | |
| H4 | `feat(hint): show the notice when a hint is undecided` | The strings (§8); the composable or the snackbar effect; the `GameScreen` wiring and the suppression-comment amendment; previews; `GameScreenHintNoticeTest` (AC-16, AC-17) | Write AC-16 and AC-17 first: AC-16 fails, because no node carries the spec's text. *(Amendment 1: H4's tests are 23a–23i plus 21a's two rows.)* | the same task |
| H4b | `feat(hint): announce the rescue banner politely` | Criterion 23k's Compose half: Amendment 1, A1.2 and A1.3 | The new `NoWinBannerTest` test fails on the missing live region | the same task, then the full Check (§7) |
| H5 | `docs(hint): add the android-game-dev report` | `.planning/reports/hint-undecided/android-game-dev.md`, in its own commit (rules/reporting.md) | | |

Conventional commits, imperative, one studio trailer, as `rules/gitflow.md` and the prepare-commit-msg hook set it.

## 6. What must not change

| Frozen | Why | How it is checked (each must print nothing) |
|---|---|---|
| **The generator contract** (`domain/.../generator/**`; its golden fixtures) | design §2.2 and §7.1: every Challenge level is a pure function of the level number | `git diff develop...HEAD -- domain/src/main/kotlin/com/app/pegsolitaire/domain/generator domain/src/test/kotlin/com/app/pegsolitaire/domain/generator domain/src/test/kotlin/com/app/pegsolitaire/domain/golden` |
| **The opening books and their reader** (`golden/book/*.txt`, `assist/OpeningBook.kt`) | R2 (deeper books) is 3.1 work (GD-HT-6) | `git diff develop...HEAD -- domain/src/main/resources domain/src/main/kotlin/com/app/pegsolitaire/domain/assist` |
| **D-07: the 1.5 s deadline and every budget constant**: `DEFAULT_DEADLINE`, `DEFAULT_MAX_NODES`, `DEFAULT_PER_ATTEMPT_MAX_NODES`, `CANCEL_MASK`; the solver's `MAX_ATTEMPTS`, `ATTEMPT_CAP_GROWTH`, `MAX_CAP_ESCALATIONS`, `ATTEMPT_SEED_BASE` and `ORDER_JITTER_WINDOW`; the ViewModel's `deadline` and `shouldCancel` lines (`:1354`, `:1368`). No longer, progressive or background search (R3 and R4 are rejected; GD-HT-4). | GD-HT-4 | `git diff develop...HEAD -- domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverBudget.kt \| grep -E '^[-+][^-+].*(const val\|val DEFAULT)'`; `git diff develop...HEAD -- domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt \| grep -E '^[-+][^-+].*const val'`; `git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt \| grep -E '^[-+][^-+].*(markNow\|hasPassedNow)'` |
| **The rewarded-ad paths**: `enterAssistFunnel`, `onAcceptAdOffer`, `onDismissAdOffer`, `grantReward`, `revealPendingHint`, `AssistAllowance`, `platform/**`. The one change the ruling makes (GD-HT-5) is that an Undecided answer never *enters* the funnel, which is the new branch's early return and nothing inside these functions. ADS-04's banner Undo is untouched; it now appears only for a proven dead end, as the ruling intends. | GD-HT-5; ADS-01 to ADS-06; D-09 | `git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/platform domain/src/main/kotlin/com/app/pegsolitaire/domain/assist`; and a read of the `GameViewModel.kt` hunks: no hunk inside the five functions above |
| **The banner and its words**: `NoWinBanner.kt`, `nowin_message`, `stuck_message`. *(Amended, Amendment 1, A1.2: `NoWinBanner.kt` may take 23k's live region on its message and the KDoc correction, and nothing else. The words stay frozen.)* | The ux-designer may re-check the wording; that is a spec change, not yours | ~~`git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/ui/game/NoWinBanner.kt`~~ replaced by A1.2's command; `git diff develop...HEAD -- app/src/main/res/values/strings.xml \| grep -E '^-'` |
| **Resume, DI, the board renderer**: `data/resume/**`, `di/**`, `ui/components/BoardCanvas.kt` | AC-15; §9 | `git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/data app/src/main/java/com/app/pegsolitaire/di app/src/main/java/com/app/pegsolitaire/ui/components` |
| **No new suppression, ignore, baseline or `!!`** | rules/studio.md, Definition of done | `git diff develop...HEAD \| grep -nE '^\+.*(@Suppress\|@Ignore\|baseline\|ignoreFailures\|!!)'`, excluding report prose. The `LongMethod` *comment* amendment on the existing annotation is an edited line, not a new suppression; the code-auditor reads that one hunk. |

## 7. The Check and the acceptance commands

**The Check** is `./gradlew check` (`CLAUDE.md` on `develop`). If check-gates has merged and you have merged `develop` in, it is `./scripts/check.sh` (design §9.6, F2). Run it once at the head, after H4. Rules for the run:
- Run it in this worktree only, and only one Gradle build here at a time (§9.6, M1).
- Record `uptime` before it. `:domain:depthSweep` asserts a 3 s per-call wall-clock cap, so a red sweep on a loaded host is a host fact. Re-run it on a quiet host before reporting it as a failure, and report both runs.

**Acceptance, in your report, each with its one-line result:**
1. `./gradlew check` (or `./scripts/check.sh`) — green; ≈ 11 min or more.
2. `./gradlew :domain:depthSweep` — green. Paste the five printed board lines, with the deferred split (found / undecided / proven-dead) and `uptime`.
3. **Warning-free where C7 will gate.** Run `./gradlew :domain:compileKotlin --rerun :domain:compileTestKotlin --rerun :app:compileDebugKotlin --rerun :app:compileReleaseKotlin --rerun 2>&1 | grep -E '^w: '` and filter for `/src/main/`, `/src/release/` or `domain/`: 0 lines. `:app`'s unit-test warnings are a held deviation (design §9.8), but add none of your own.
4. `git grep -n "firstWinningMove" -- domain app` — 0 hits.
5. `git grep -nE "noWinningMove = true" -- app/src/main` — exactly 1 hit, inside the `ProvenDead` branch.
6. `git grep -nE "hintUndecided = true" -- app/src/main` — exactly 1 hit, inside the `Undecided` branch.
7. `git grep -c "hintUndecided = false" -- app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — at least the count of `noWinningMove = false` in the same file, with each pair in the same `copy(...)`.
8. `git grep -n "nowin_message" -- app/src/main` — the same two hits as on `develop` (`strings.xml`, `GameScreen.kt`), no new one.
9. `git grep -nE "showRescueBanner.*hintUndecided|hintUndecided.*showRescueBanner" -- app/src/main` — 0 hits.
10. Every "must print nothing" command in §6 — empty.
11. The start-path tripwire in §9 — empty.
12. `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.game.*'` and `./gradlew :domain:test` — green, with the count of tests run.

**Device (qa-engineer, AC-18 to AC-21).** Your report gives QA everything it needs to run them without reading code:
- AC-18: the three Wiegleb-45 sequences four jumps in, including the two reached without following Hint;
- AC-19: the English-33 sequence to a book-proven dead position;
- AC-20: the Diamond-41 sequence to a gap position;
- each one in the on-screen notation, and each the same sequence the domain tests in §4.2 use.

## 8. The seam left for the ux-designer

The ux-designer rules the notice's words, form and announcement in `docs/ux-spec.md` on this branch. The code meets that ruling at these names, and only these:

| Seam | Name | Meaning |
|---|---|---|
| UI-state field | `GameUiState.hintUndecided: Boolean` | True from an Undecided answer on the board on screen until the next jump, undo, restart, Hint tap or board install. Never true together with `noWinningMove`. Never persisted. This is the row key for the ux-spec §4.4 control-state table, beside `noWinningMove`'s row. While it is true, the Hint control is Available (`solverThinking` false). |
| UI-state field, conditional | `GameUiState.hintUndecidedTick: Int` | *(Void, Amendment 1: not added; the form is inline.)* Exists only if the spec chooses a one-shot form (a snackbar). Same rules as `adMessageTick`. |
| String | `hint_undecided_message` | The notice's visible text, copied verbatim from the spec |
| String, conditional | `hint_undecided_announcement` | *(Void, Amendment 1: not added; TalkBack speaks the visible text.)* Exists only if the spec gives TalkBack a text that differs from the visible text. Do not add an unused string: lint's `UnusedResources` flags it. |
| Composable, inline form only | `HintUndecidedNotice(message, modifier)` in `ui/game/HintUndecidedNotice.kt`; test tag `HINT_UNDECIDED_NOTICE_TEST_TAG` | No actions. Rendered only from `hintUndecided`. |

What the spec must give, so that H4 can start *(all given at `f221ae4`; see Amendment 1, A1.4)*:
- the text;
- the form (inline or one-shot);
- the placement relative to `NoWinBanner`'s slot (`GameScreen.kt:237-251`, bottom-centre over the board);
- whether a TalkBack announcement differs from the text;
- the §4.4 row.

Whatever the form, the ruling's information rules (§2 of the ruling, the "must never say" column) bind the wording.

## 9. Start path: no 15e sitting

**Ruling: this branch needs no 15e sitting, provided the tripwire below stays empty.**

- **The trigger** is "any later branch that changes Home's first frame": `docs/ux-spec.md` §6.5, the table row of that name. It is cited by section because the ux-designer is editing that file on this branch.
- **Home's first frame does touch one file on this branch.** It builds `GameUiState`: `HomeStage.kt:130` (`HomeStageBoardState`, lazy) and `HomeBoardArt.kt:49` (`GameUiState.initial(board)`). It draws it with `BoardCanvas`.
- **But this branch adds only defaulted fields to `GameUiState`, and `BoardCanvas` does not read them.** `BoardCanvas` reads `board`, `animating`, `hintSource`, `hintTarget`, `legalTargets`, `selected` and `showHints`. So the change adds no read, no I/O, no composable and no measurable allocation to Home's first frame.
- **`:domain` is off the start path.** `GetHintUseCase` is created only when `GameViewModel` is injected (`di/DomainModule.kt:55`), and the books load on the first hint.
- **`strings.xml` and `dimens.xml` only gain entries.** That adds no first-frame work.

**Tripwire.** Each line must print nothing. If either prints, the producer schedules a 15e sitting on `Pixel_9a_bench` before the tech-lead sign-off.

    git diff --name-only develop...HEAD | grep -E '^app/src/main/(java/com/app/pegsolitaire/(App|MainActivity)\.kt|java/com/app/pegsolitaire/(navigation|ui/home|ui/components|ui/theme|ui/onboarding|data|di|platform|audio)/|AndroidManifest\.xml|res/(font|drawable|mipmap|values/themes\.xml|values-night/))'
    git diff develop...HEAD -- app/src/main/res/values/strings.xml app/src/main/res/values/dimens.xml | grep -E '^-[^-]'

The second line enforces "entries are added, none edited or removed". Separately, the `GameUiState` hunk must add fields with defaults and change nothing else; the code-auditor reads that hunk.

## 10. Overlap with the in-flight branches, and the merge order

### 10.1 What each branch touches that this one touches

| Branch | State on 2026-10-21 | Files it shares with this branch | Kind of overlap |
|---|---|---|---|
| `feature/audio-review` (`PegSolitarie-audio`, `df903b3`; merge-base `0582e3f`) | In progress. Its committed diff is `audio/**`, `MainActivity.kt`, `di/AudioModule.kt` and `standards.gradle.kts`. Its working tree has uncommitted edits to `GameScreen.kt`, `GameFlowTest.kt`, `GameScreenSoundTest.kt` and the tutorial files: the haptics seam, under way. Still owed under `tech-lead-haptics.md` and `docs/ux/invalid-tap-feedback.md` §3.7–3.8: the wrong-tap shake and mark. | **`GameUiState.kt`**: a wrong-tap cell paired with `rejectedTapTick`, in the constructor, `initial()`, `loading()` and the KDoc. **`GameViewModel.kt`**: `onCellTap`'s wrong-tap branch (`:797-825`) and that tick carried forward at the four board-install sites. **`GameScreen.kt`**: the effects block (`:164-176`), the `JumpHaptic` removal (`:286-314`), a new private reject effect, and probably the `LongMethod` surfaces comment (`:142`). Also `BoardCanvas.kt`, `GameFlowTest.kt` and maybe `strings.xml`. | Textual conflicts are **certain but mechanical** in `GameUiState` (adjacent field additions in the same three blocks), in the `LongMethod` comment, and at the end of `strings.xml`. Different hunks in `GameViewModel` — unless the notice's form is one-shot, in which case both branches carry a tick at the same four install sites *(void, Amendment 1: the form is inline, so this branch adds no tick)*. Behaviour overlap: none. The two flags are independent, and neither branch reads the other's field. |
| `feature/phase-14-check-gates` (`PegSolitarie-gates`, cut at `0582e3f`) | In progress: C1 committed (`348279d`), C2 (`scripts/`, `CLAUDE.md`, `AGENTS.md`) in the working tree. Brief: `tech-lead-r2-gate-brief.md` plus A0–A2 and A3 (C7). | No shared file. Its list is build-logic, `scripts/check.sh`, `CLAUDE.md`/`AGENTS.md`, `LevelGenerator.kt`, `domain/src/test/**` (new generator and fuzz tests), `domain/build.gradle.kts`, the catalog, `ci.yml`, and for C7 the six `*Route.kt` files and four data/onboarding files. It does not plan to edit `DepthSweepTest`, `GetHintUseCaseTest`, `SolverTest` or `TestFlags.RUN_SWEEP`. | **Semantic, through three gates:**<br>**C7** fails any Kotlin warning in `:app`'s main compilations and in every `:domain` compilation, tests included. This branch's code must therefore be warning-free now (§7, item 3).<br>**C6** adds a kover line-coverage gate on `:domain`. The new outcome branches are covered by §4's tests and must not pull `:domain` below the bound C6 sets.<br>**C2** turns the Check command into `./scripts/check.sh`. |
| `bugfix/home-chip-arrival` | Pending. Not cut; the ux ruling comes first (`feature/ux-spec-folds`, `dc3f77e`). | No code: it is `ui/home/**`. `docs/ux-spec.md` is shared. This branch carries the ux-designer's Undecided-notice section, and `feature/ux-spec-folds` carries folds a–e and the F-1 ruling. | Docs only. The ux-designer owns the file, so a conflict there is routed to the ux-designer, never resolved by a developer. |
| `feature/candy-backdrop` (last in line, its own 15e) | Building last | `GameScreen.kt`'s `Scaffold` colour arguments (`tech-lead-haptics.md` notes this) | Adjacent hunks in `GameScreen` only |

### 10.2 Merge order into `develop`
1. **`bugfix/hint-undecided`.** It blocks `release/3.0.0` (GD-HT-1, MAJOR), it is the smallest, and it is the furthest from the build files. Merging it first means the other branches absorb it through the `develop` merge-in each already owes: audio's "merge develop in first", and check-gates' A3 merge-in rule. So C7's warning gate and C6's coverage bound are measured on a tree that already contains it, which is the strongest check it can get.
2. **`feature/phase-14-check-gates`**, as STATUS already orders it ahead of audio.
3. **`feature/audio-review`.**
4. **`bugfix/home-chip-arrival`.** No code overlap with this branch, so its position relative to this one is free.

Then `feature/candy-backdrop`, then `release/3.0.0`. If a later branch is ready first, merges stay serial and the first ready goes first. The duties below then fall on whichever branch merges second.

### 10.3 What the branch merging second must do
For every pair, the branch merging second:
- merges `develop` in (`git merge develop`, never a rebase);
- resolves the conflicts as below;
- re-runs its own Check and the other branch's named tests;
- has its tech-lead sign-off re-read the merge commit's conflict hunks.

**check-gates after this branch:**
- C7's acceptance runs on the merged tree. Its "0 `w:` from main and `domain/`" covers `Solver.kt`, `SolverOutcome.kt`, `GetHintUseCase.kt`, `HintOutcome.kt`, `GameViewModel.kt`, `GameUiState.kt`, `GameScreen.kt` and the notice file.
- A warning there that is one of A3's two mechanical kinds is fixed A3's way. Any other kind: stop and report. Do not edit this branch's files for anything else.
- C6's kover bound is measured after the merge.
- The `DepthSweepTest` that C1's self-heal protects is this branch's version.

**This branch after check-gates** (the reverse case):
- merge `develop` in, then use `./scripts/check.sh`;
- fix any `w:` in this branch's main or `:domain` files with no suppression;
- keep `:domain` at or above C6's bound;
- re-run §7 in full.

**audio-review after this branch** (or this branch after audio, with the same rules):
- `GameUiState`: keep both fields. `hintUndecided` follows `noWinningMove`; audio's wrong-tap cell sits beside `rejectedTapTick`. Both go explicitly into `initial()` and `loading()`, and both get KDoc entries.
- `GameViewModel`: keep both. If both branches carry a tick, both are carried forward at all four board-install sites *(void, Amendment 1: only audio carries a tick)*.
- `GameScreen`: keep both effects and the notice. The `LongMethod` comment names every surface from both branches, still on one annotation.
- `strings.xml`: keep both blocks.
- Then run this branch's tests: `./gradlew :domain:test` and `./gradlew :app:testDebugUnitTest --tests '*GameViewModelAssistOrderingTest' --tests '*GameViewModelHintStalenessTest' --tests '*GameViewModelResumeTest' --tests '*GameViewModelTelemetryTest' --tests '*GameScreenHintNoticeTest' --tests '*NoWinBannerTest' --tests '*DockStateLanguageTest'` (the last two added by Amendment 1). Also run audio's own `GameFlowTest` and its wrong-tap tests.
- If a conflict was resolved inside `onHintRequested`, the notice composable or the banner slot, the qa-engineer re-runs AC-18 and AC-21 on the device.

**ux-spec folds after this branch:** the ux-designer merges `develop` into `feature/ux-spec-folds` and resolves `docs/ux-spec.md`, keeping the Undecided section and its §4.4 row.

## Outputs

- `/Users/ermanergoz/Desktop/PegSolitarie-hint/.planning/reports/hint-undecided/tech-lead-brief.md` — this brief and report. It is the only file I wrote. I wrote no code, committed nothing and ran no Gradle, as dispatched.

## Checks

Reads and greps against `develop` at `0582e3f` (this worktree) unless named otherwise. No Gradle was run, as dispatched; two other worktrees were building.
- `cat .claude/rules/VERSION` vs `ergoz-soft/.claude-plugin/plugin.json` — 0.6.2 vs 1.0.0, so I read the plugin copies of the rules.
- `git worktree list`; `git log` — this branch is at `0582e3f` with a clean tree apart from the ux-designer's work. check-gates has `348279d` (C1) and C2 uncommitted. audio-review has `df903b3` and uncommitted `GameScreen.kt` edits.
- `git log develop -- docs/tech-design.md` and `-- .planning/reports/phase-14/game-designer-hint-timeout.md` — pass, both empty: neither file is on `develop`. Both are on `feature/phase-14-release-docs` (`7929d1b`).
- `Read` the ruling in full (332 lines) and `docs/tech-design.md` §2.1–3.1 and §6–9.8 (release-docs branch) — pass. §7.2's transcription matches the code below.
- `Read Solver.kt` (all 420 lines) — pass:
  - proof versus cut-off at `:160`, folded at `:121-131` and `:165`;
  - the one latch at `:193` and `:301-305`;
  - the early ProvenDead cases at `:126`;
  - the loop exits at `:153`.
- `Read GetHintUseCase.kt` (all), `SolverBudget.kt` (all) and `OpeningBook.kt:170-200` — pass. `NoWinningMove` short-circuits at `:117`; the D-07 constants are at `:53`, `:82`, `:95` and `:102`.
- `Read GameUiState.kt` (all), `GameViewModel.kt:280-360` and `:1180-1580`, `GameScreen.kt:90-290` — pass:
  - `null` becomes the banner at `:1384-1385`;
  - the supersession path runs `cancelHintSearch` → `hintJob.cancel()`, then the board guard at `:1375`;
  - the clearing sites are `:931`, `:1280` and `:1347`;
  - `showRescueBanner` is at `:142`, and `nowin_message` is chosen at `GameScreen.kt:241`.
- `grep firstWinningMove` over `domain/src` and `app/src` — 5 files: `Solver.kt`, `GetHintUseCase.kt`, `SolverTest` (8), `SolverBudgetTest` (2), `PagodaTest` (1, a comment).
- `grep GetHintUseCase` over `app/src/test` — 14 test files construct it. Three mockk `answers { … firstOrNull() }` stubs return `Move?` (`AdClockTest:399`, `GameViewModelTest:983`, `HintStalenessTest:249`); the type change makes the compiler find each one.
- `grep 'noWinningMove).isTrue'` over `app/src/test`, then reading each hit — four tests reach the banner through the **real** use case on small dead boards (`GameViewModelTest:766`, `:952`; `TelemetryTest:254`; `AllowanceTest:557`). They must stay green unedited.
- `grep 'class .*Dispatcher'` over the game tests — three private gated dispatchers (`GameViewModelTest:1738`, `HintStalenessTest:488`, `AssistOrderingTest:438`).
- `grep 'state\.'` over `BoardCanvas.kt` — it reads `board`, `animating`, `hintSource`, `hintTarget`, `legalTargets`, `selected` and `showHints`. `grep GameUiState` over `ui/home` — `HomeStage.kt:130` and `HomeBoardArt.kt:49` build it for Home's first frame.
- `Read di/DomainModule.kt` — `GetHintUseCase` is provided at `:55` and created on `GameViewModel` injection only.
- `ls app/src/main/res/values*`; `grep -c '<string' values-night/*.xml` — one strings file; `values-night` has 0 strings.
- `grep -n "Any later branch that changes Home" docs/ux-spec.md` — the 15e trigger row is in §6.5. It moved from line 2909 to 2974 during this session under the ux-designer's parallel edit, so I cite it by section.
- `git diff --stat` and `--name-only 0582e3f feature/audio-review`; `git -C PegSolitarie-audio status`; `Read` `tech-lead-haptics.md` (grep) and `invalid-tap-feedback.md` §3.7–3.8 — audio's committed diff has no `ui/` file. Its working tree edits `GameScreen.kt`, and its spec adds a wrong-tap cell to `GameUiState`.
- `Read tech-lead-design-sync.md` A3 and `tech-lead-r2-gate-brief.md` (commit table, §9, §10) on release-docs — C7 gates `:domain` tests and `:app` main; C6 adds kover on `:domain`; there is no planned check-gates edit to the hint files.
- `head` of `wiegleb45-k3.txt` and `diamond41-k4.txt`; entry counts per book — see Open issues.
- `ls -d` on every path named in §6's frozen-file commands — all exist.
- *(Amendment 1, 2026-10-21.)* `git log --oneline -5` — the spec landed at `f221ae4`, and the ux-designer's report at `ad7b83d`, on top of this brief's `61c4bb4`.
- *(Amendment 1.)* `sed -n 2591,2830p docs/ux-spec.md` (§4.4.2 in full), the criteria 23a–23k (`:4370-4448`), R-7 (`:3381`), the §8 string row (`:3422`) and the D-04f test table (`:3892-3902`) — pass:
  - one Boolean, no tick, no announcement string;
  - an inline, pass-through notice in the banner's slot;
  - 23k names `NoWinBanner.kt`'s message node.
- *(Amendment 1.)* `Read NoWinBanner.kt` (all 111 lines) — the KDoc at `:29-31` still names "a budget miss, D-04". The message `Text` is at `:68-72`, and the three `TextButton`s are siblings in a `Row`, so a live region on the `Text` leaves them outside it.
- *(Amendment 1.)* `git diff --name-only` from the merge-base for audio-review, candy-backdrop, ux-spec-folds and check-gates, and `git status` in the audio, candy and gates worktrees, for `NoWinBanner.kt` and `NoWinBannerTest.kt` — pass: no hits.
- *(Amendment 1.)* `grep -rln` for `GameAnim`, `ClayAnim` and `clayEntrance`, and for criterion 21a's test — `GameAnim.kt` is in `ui/game` (off the tripwire), `ClayAnim.kt` and `Clay.kt` are in `ui/theme` (reuse only), and 21a's table is `DockStateLanguageTest.kt`.

## Open issues / risks

- **Staffing (Q-019).** This brief was written on Opus, the developer's own model. Two of the studio's safeguards are suspended until 3.0.0 is published: a fable plan checked by a fresh fable agent, and code reviewed on a model superior to its author's. Mitigation: every behaviour the ruling names is a named test, the frozen surfaces have mechanical checks (§6), and the code-auditor reads the diff against the ruling and this brief.
- **The ruling and the tech design are off-branch** (§0). Reviewers need the `git show` paths until release-docs merges into `develop` and this branch merges `develop` in.
- **H4 waits on the ux-designer's spec commit.** If the spec picks the one-shot form, the tick adds four install-site lines that conflict with audio-review's own tick, a mechanical conflict covered in §10.3. *(Resolved, Amendment 1: the spec landed at `f221ae4` with the inline form, so there is no tick.)*
- *(Amendment 1.)* **`NoWinBanner.kt` now changes on this branch (23k).** No other branch touches it. Two items carry into the audio fold-in, both for the producer to route:
  - The ux-designer notes (open issue 5) that audio's planned "new §4.4.2" and its D-06 family must renumber, because D-04f and §4.4.2 are now taken. That is a spec matter with no code effect here.
  - Audio's wrong-tap announcement and this branch's two live regions all use the polite mode, so they agree.
- **Device dependence.** Undecided rates vary by device; Found and ProvenDead do not (the ruling's challenge 3). The sweep's `deferredUndecided` is host-dependent and therefore printed, not pinned. R5's census (3.1) is the measurement.
- **The sweep's timing cap under load.** Two other worktrees build on this host. `:domain:depthSweep`'s 3 s per-call cap can trip on load, not on the diff. Record `uptime`; re-run quiet.
- **The count mismatch the ruling raised is not reconciled here**, because it does not touch this branch.
  - The book headers declare 140 (Wiegleb-45) and 818 (Diamond-41) positions at depth ≤ K. The sweep examines 101 and 785.
  - The gaps agree in both: 4 and 440. The difference is entirely in the served entries: 136 book entries against 97 served, and 378 against 345.
  - So the books hold entries that the sweep's BFS frontier never reaches.
  - This belongs with the tech-design pass in the Handoff, before any copy quotes a position count.
- **`08-VERIFICATION.md` override 1** needs the correction note the ruling asks for. It is a gsd record and is routed through the producer.

## Handoff

Next: producer. Route the following.
- **android-game-dev** (opus, `PegSolitarie-hint`): this brief plus the ruling (§0). Build H1 to H3 now, and H4 after the ux-designer's spec commit. Report to `.planning/reports/hint-undecided/android-game-dev.md`.
- **ux-designer:** §8 is the seam. Name the text, the form, the placement, the announcement and the §4.4 row against `hintUndecided`, `hint_undecided_message` and, if needed, `hint_undecided_announcement` / `hintUndecidedTick`. *(Done at `f221ae4`. Amendment 1 resolves the seam and rules 23k onto this branch.)*
- *(Amendment 1.)* **android-game-dev:** this brief including Amendment 1 (H1–H4, then H4b), the ruling, and spec §4.4.2 with criteria 23a–23k. **qa-engineer:** AC-18 to AC-21, plus 23j and 23k's device half, recording the TalkBack traversal order it observes (ux-designer open issue 7).
- **code-auditor, then qa-engineer:** review against the ruling's AC-1 to AC-17 and §6 and §7 here. QA runs AC-18 to AC-21 on a device from the developer's sequences. No 15e sitting unless the §9 tripwire prints.
- **tech-lead:** sign-off after both reports.
- **Merge order:** §10.2. The branch that merges second does §10.3.
- **docs/tech-design.md, owed when this merges (on `feature/phase-14-release-docs` later, not now):** §7.2's "What 'No winning move' means today" rewritten to the three outcomes and §2's mechanism, with §10's Hint-latency row aligned; §16 #21 closed, citing GD-HT-1 to GD-HT-6 and this brief; the §15 "budget miss" risk row closed or reworded as the residual Undecided rate (R5); the book-count reconciliation goes with it.
