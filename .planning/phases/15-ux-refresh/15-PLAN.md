---
phase: 15-ux-refresh
plan: 00
type: phase-plan
wave: all
depends_on: []
autonomous: true
requirements: [ONB-01, NAV-01, PERS-03, CHAL-05, CLAS-01, CLAS-02, ASST-01, ASST-02, SCOR-02, SOUND-01, SOUND-04, PLAT-08, QUAL-03, UX-01, UX-02, UX-03]
design_contract: docs/ux-spec.md
binding_rulings:
  - .planning/reports/phase-15/tech-lead-architecture.md      # architecture — wins on mechanism
  - .planning/reports/phase-15/ux-designer-amendments.md      # screens/interaction — wins on what the player sees
  - .planning/reports/phase-15/game-designer-tutorial-boards.md # board content
  - .planning/reports/phase-15/qa-engineer-tutorial-audio.md  # D-01h FAIL — the jump cue must fire
  - .planning/reports/phase-15/ux-designer-audio-amendment.md # ruled into the spec as D-01i
  - .planning/reports/phase-15/release-manager-sequencing.md  # lands before release/3.0.0; VERSION untouched

must_haves:
  truths:
    - "Every slice ends on a green `check` and one commit; no slice leaves a red build for the next one to inherit."
    - "The two places this phase can go wrong — the Home progress aggregate and the tutorial's persist-before-pop exit — are built in slices 2 and 4, not discovered at the end."
    - "The four §5.1 prohibitions are each proven by a named command or a named test, so a reviewer verifies rather than trusts."
    - "The two tests that break by design are retargeted to readable assertions — never `@Ignore`d, never deleted."
    - "`VERSION` is not touched: the publish hook and `scripts/play-upload.sh` resolve the approval file from it at run time."
  artifacts:
    - path: ".planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md"
      provides: "the two-arm cold-start tables (arm A develop, arm B this branch) and the criterion-15f visual check"
      contains: "## Cold-start measurement"
---

<objective>
Turn the phase-15 design and architecture contracts into eight ordered, test-first slices, each
ending at a green `./gradlew check` and one conventional commit.

Purpose: the design is settled — `docs/ux-spec.md` §4 specifies every screen, §5.1 states four
hard prohibitions, §6 names 15 new dimens and 2 retirements, §8 the copy, §14 the criteria; the
tech-lead has ruled hosting, the data read, the C1 gate and the screenshot question; the
game-designer has fixed and proven the three tutorial boards; and the qa-engineer's FAIL on D-01h
is now ruled into the spec as **D-01i**, so the tutorial's jumps sound and its two exits speak the
app's press vocabulary. What is left is an ordering that puts risk first, a named failing test in
front of every behaviour, and a proof for each prohibition.

Output: eight slices, S1…S8, with explicit inter-slice dependencies; a file-by-file inventory;
the exact `check` invocation this worktree needs; and a runnable cold-start procedure.

**Decision-id note.** The tutorial's audio contract is **D-01i** (`ux-spec.md:233-262`), which
*amends* D-01h rather than replacing it: D-01h's silencing of the other seven cues is **upheld**,
and the completion CTA it left unruled is now ruled. Cite **D-01i** in code comments and commit
bodies; D-01h carries a pointer to it and is not the current contract on its own. §5's enumerated
both-halves contract is at `ux-spec.md:826-838`; §5.1's "what these four do not forbid"
clarification — the one that makes S3's lift legal — is at `ux-spec.md:864-877`, and it is a
clarification, **not** a relaxation: lifting one effect out of `GameScreen.kt` is not licence to
host the file.

**House-shape note.** The repo's convention is one `NN-MM-PLAN.md` per slice
(`.planning/phases/13-cloud-save/13-03-PLAN.md`, `.planning/phases/08-assist-generator-quality-debt/08-02-PLAN.md`).
This document is the ordered *phase* plan the task asked for; if the producer wants per-slice
files, each `## S<n>` section below is the body of `15-0<n>-PLAN.md` unchanged — the frontmatter
`depends_on` for each is in the slice map (§1), and each slice already carries its own
`<tasks>`-shaped test order, verify command, acceptance criteria and commit subject.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/ROADMAP.md
@docs/ux-spec.md
@.planning/reports/phase-15/tech-lead-architecture.md
@.planning/reports/phase-15/game-designer-tutorial-boards.md
@.planning/reports/phase-15/qa-engineer-tutorial-audio.md
@.planning/reports/phase-15/ux-designer-amendments.md
@.planning/reports/phase-15/release-manager-sequencing.md
</context>

---

## 0. Preconditions — read before the first `git add`

### 0.1 The check command does not work in this worktree as written

`CLAUDE.md` names `./gradlew check`. **In this worktree that command fails in under a second**,
before a single test runs, because `local.properties` is git-ignored and exists only in the
primary worktree:

```
$ ./gradlew check --offline
FAILURE: Build failed with an exception.
* What went wrong:
Could not determine the dependencies of task ':app:lintReportDebug'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable
  or by setting the sdk.dir path in your project's local properties file at
  '/Users/ermanergoz/Desktop/PegSolitarie-p15/local.properties'.
BUILD FAILED in 633ms
```

Gradle *configuration* succeeds (`./gradlew help --offline` → BUILD SUCCESSFUL in 623ms); the
failure is at `:app:lintReportDebug` dependency resolution. So it is not a code defect and not a
Gradle-version problem — it is a missing SDK path.

**The exact invocation every slice in this plan uses, and the only one whose result may be quoted
in a report or a summary:**

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

`--offline` is not optional: PLAT-07/D-18 makes offline-green an enforced invariant, and
`06-BUILD-EVIDENCE.md:354-370` records that any new dependency needs an online warm on every
machine and CI runner before offline `check` works again. This phase adds **no** dependency
(§12.6), so `--offline` must stay green throughout; the first slice where it does not is the
slice that accidentally added one.

The developer may instead write `sdk.dir=/Users/<user>/Library/Android/sdk` into a local
`local.properties`. That is equivalent and must **not** be committed — it is already in
`.gitignore`.

### 0.2 The pre-refresh baseline

| Field | Value |
|---|---|
| Command | `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` |
| Tree | `feature/phase-15-ux-refresh`, unmodified (docs-only commits since the `develop` cut) |
| Result | BUILD SUCCESSFUL, 98 actionable tasks (10 executed, 1 from cache, 87 up-to-date) |
| Wall time | **11m09s**, measured by this plan on this tree; independently **11m10s** by the tech-lead on the same tree (`tech-lead-architecture.md:480`). The two runs agree to one second, so 11m10s is a stable baseline and not a one-off. |
| Scope | `:app` + `:domain` ktlintCheck, detekt, lint, `dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck`, `audioFocusLiteralCheck` (`build-logic/src/main/kotlin/standards.gradle.kts:265-274`) and all unit tests |

Treat 11m10s as the pre-refresh baseline. An eight-slice phase runs `check` at least eight times,
so the phase's own `check` bill is ~90 minutes of wall clock. During a slice, iterate on the
narrow task (`./gradlew :domain:test --tests '*TutorialBoardsTest*'` runs in seconds) and spend
the full `check` once, at the end of the slice, before the commit.

### 0.3 Standing constraints

- **Do not touch `VERSION`.** It reads `3.0.0`; both the publish hook and `scripts/play-upload.sh`
  resolve `.planning/approvals/publish-<VERSION>.approved` from it at run time, so a bump here
  makes the gate refuse an already-approved upload. Gitflow bumps it on the release branch
  (`release-manager-sequencing.md:214-226`).
- **Do not edit** `docs/ux-spec.md` (ux-designer's), `.planning/reports/STATUS.md`,
  `.planning/ROADMAP.md`, `.planning/questions/FOUNDER.md`.
- **No `@Suppress`, no baseline, no `@Ignore`, no disabled test, no `!!`.** The two existing
  vector files carry `@Suppress("MagicNumber")` on path data with a justification comment
  (`ux-spec.md:983-985`); the two new vectors follow that established precedent exactly and add
  nothing else. Anything beyond that is a code-auditor question, not a developer decision.
- **`build-logic` is not in scope.** `dpSpLiteralCheck` fails the build on a raw `.dp`/`.sp`
  literal in a composable — that is the gate, not an obstacle; every new dimension is a token.

---

## 1. Slice map

Ordered for execution top to bottom on one branch. The **Independent of** column is what a
producer needs if the phase is split across worktrees; the **Risk** column is why the order is
what it is.

| # | Slice | Depends on | Independent of | Risk | Commit subject |
|---|-------|-----------|----------------|------|----------------|
| **S1** | `:domain` `TutorialBoards` + the dead-end-freeness proof | — | S2, S3 | Low, but it is S4's dependency root and its test loop is seconds | `feat(15): add the three tutorial boards with a dead-end-freeness proof` |
| **S2** | Home progress aggregate: DAO → projection → repository → 11 doubles → `HomeViewModel` | — | S1, S3 | **HIGH — risk 1.** One new `@Query` on the start destination; the wrong mechanism (a Kotlin fold) is invisible in the lab and worst on the most engaged player | `feat(15): read the Home ribbon values from one SQL aggregate` |
| **S3** | Lift `JumpSound` out of `GameScreen` into a shared effect | — | S1, S2 | Low, but it is what stops the pitch ladder being duplicated in S4 | `refactor(15): lift the jump cue into a shared effect` |
| **S4** | Tutorial destination, `TutorialViewModel`, persist-before-pop exit, screen, audio | S1, S3 | S2 | **HIGH — risk 2.** A fire-and-forget `markSeen()` loses a one-frame race and Home re-pushes the tutorial forever | `feat(15): add the interactive tutorial as a nav destination` |
| **S5** | Entry points: info button deleted, Settings row added, `tutorialPending`, two test retargets | S4 | S2 | Medium — this is where the two by-design test breakages land | `feat(15): move the tutorial entry point from Home to Settings` |
| **S6** | Home recomposition: hero, ribbon, tile art, thumbnail, scroll; `HomeTopBar` deleted | S2, S5 | S7 | Medium — five bands plus the first-paint rule and the three D-03f oracles | `feat(15): recompose Home into five bands with a progress ribbon` |
| **S7** | Gameplay chrome: status strip + action dock, two new vectors, `GameTopBar` deleted | S3 (same file, disjoint region) | S6 | Low — the largest diff, the fewest unknowns | `feat(15): replace the gameplay top bar with a status strip and action dock` |
| **S8** | Token retirement, cold-start measurement, `15-BUILD-EVIDENCE.md` | S6, S7 | — | Medium — the C1 gate is the one pass/fail nobody can shortcut | `chore(15): retire two dimens and record the cold-start measurement` |

**Why this order and not another.**

1. **S2 before any UI.** The tech-lead rejected R-1's client-side fold because over
   `MODE_CHALLENGE` it loads the unbounded ladder into memory on the start destination
   (`LevelResultDao.kt:41-47` already refuses this for a *background* submitter). Building the
   aggregate first means the Home band in S6 has real data to render and the C1 arm in S8 measures
   the shipped mechanism. Building it last would mean discovering a cold-start regression after
   five slices of visual work were already committed on top of it.
2. **S4 before S5 and S6.** The exit-ordering bug is a navigation loop, not a cosmetic late
   prompt: a dropped `markSeen()` leaves `tutorialPending` true, Home's `LaunchedEffect` re-pushes
   `Tutorial`, and `launchSingleTop` does not help because `Tutorial` is no longer on top
   (`tech-lead-architecture.md:119-133`). Proving the ordering on a bare screen is cheap; proving
   it under a finished one is not.
3. **S3 split out of S4 deliberately.** The qa-engineer names the scope risk explicitly: *"If the
   developer instead re-implements `JumpSound` inside `TutorialScreen.kt`, the
   `REMOVAL_LAYER_DELAY_MS` layering and the `runIndex` pitch ladder become duplicated and will
   drift. The reviewer should insist on the lift, not the copy."*
   (`qa-engineer-tutorial-audio.md:389-392`.) A separate commit whose only acceptance criterion is
   *`GameScreenSoundTest` passes byte-unchanged* makes the lift reviewable as a pure refactor, and
   makes the copy structurally unavailable to S4.
4. **S8 last because two of its inputs only exist then.** `top_bar_touch_target` currently has
   **six** callers (§12.2) and cannot be deleted until S7 retargets all of them; the C1 arm B
   measurement is meaningless before S6 and S7 have both landed.

**Reordering note (the D-01i audio ruling).** The qa-engineer's FAIL on D-01h added a shared-composable
extraction and four audio tests to the tutorial work. That did not change the risk ordering — S4
is still wave 2 and still after S1 — but it did make S4 large enough that the extraction became
its own slice (S3). That is the only structural change the ruling caused.

**Same-file caution for parallel worktrees.** S3, S4 and S7 all touch `GameScreen.kt`, in disjoint
regions: S3 the effects block (`:157-164`, `:321-330`), S7 the `Scaffold`/`GameTopBar`
(`:171-187`, `:489-527`). If the producer parallelises, the merge order is **S3 → S7**, and S4
touches the file not at all once S3 has landed.

---

## S1 — `:domain` `TutorialBoards` and the dead-end-freeness proof

**Depends on:** nothing. **Independent of:** S2, S3.

<objective>
Put the three proven tutorial footprints in `:domain` as `object TutorialBoards`, guarded by the
permanent test the game-designer specified, so D-01c ("a wrong tap is never a failure") is a
property of the layouts rather than of a report.
</objective>

This slice is pure Kotlin in the module the studio rules call the testable core
(`rules/kotlin-android.md`: `:domain` is Android-free, immutable data, seeded randomness only).
Its test loop is `./gradlew :domain:test --tests '*TutorialBoardsTest*'`, which the game-designer
measured at **2s**.

### Test-first order

1. **Write `TutorialBoardsTest` first, against a `TutorialBoards` that does not exist.** It will
   not compile. That is the failing state: run it, see the compile error, then write the object.
   Do not write the object first and the test after — the game-designer's own harness found that
   a confidently-reasoned board (the 3×5 perimeter ring) was wrong
   (`game-designer-tutorial-boards.md:250`), which is exactly the class of error this ordering
   catches.

   | # | Test | Asserts |
   |---|------|---------|
   | 1 | `everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg` | For each board in `TutorialBoards.ALL`: enumerate every position reachable via `RulesEngine.legalMoves` / `RulesEngine.applyMove`, memoised on `Board.pegs`; assert every position with **no** legal move has `pegCount == 1`. Largest reachable set is 10, so the test is effectively free. |
   | 2 | `theFirstTutorialStepHasExactlyOneLegalMoveAndItIsHorizontal` | `legalMoves(T1).size == 1` and that move's source and destination share a row. |
   | 3 | `theSecondTutorialStepHasExactlyOneLegalMoveAndItIsVertical` | `legalMoves(T2).size == 1` and that move's source and destination share a column. |
   | 4 | `noTutorialBoardAppearsInTheClassicCatalogue` | `Boards.CLASSIC_SPECS.size == 5` **and** no `TutorialBoards.ALL` footprint equals any `CLASSIC_SPECS` footprint. This is prohibition 5 (§10) at unit level. |
   | 5 | `everyTutorialBoardStartsAboveItsTargetAndUsesTheDefaultMinimumPegs` | Each board starts with `pegCount > 1`, `target == null`, `minimumPegs == BoardConstants.MINIMUM_PEGS_DEFAULT` — pins the three properties the game-designer's ruling rests on (`game-designer-tutorial-boards.md:36-38`). |

   Tests 2 and 3 are the unit-level guard for §14 criterion 2 (both axes performed), which
   otherwise has only a device check (`ux-spec.md:1141-1143`).

2. **Then write the object**, at
   `domain/src/main/kotlin/com/app/pegsolitaire/domain/tutorial/TutorialBoards.kt`:

   ```
   T1  P P .      in play (0,0)(0,1)(0,2)    pegs (0,0)(0,1)                   1 move,  horizontal
   T2  . P .      in play all nine of 3×3    pegs (0,1)(1,1)                   1 move,  vertical
       . P .
       . . .
   T3  P P .      in play all nine of 3×3    pegs (0,0)(0,1)(1,0)(1,1)(1,2)    4 moves, 3 openings
       P P P
       . . .
   ```

   Built the way `Boards.kt` builds a spec: `Board(spec, pegs = 0L).withPeg(...)`; bit order is
   `BoardSpec.footprint`, row then col (`Board.kt:13-32`, `BoardSpec.kt:17-38`). `target = null`,
   `minimumPegs` left at its default. Expose `val ALL: List<BoardSpec>` (or `List<Board>` — pick
   one and let the test's iteration shape decide) in **T1, T2, T3 order**, because S4's step
   sequence indexes it.

   KDoc must state, in one paragraph: these are bespoke teaching miniatures; they are **not**
   Classic geometry and must never join `Boards.CLASSIC_SPECS`, because that list is the Classic
   catalogue and the denominator of the Home Boards pill; and no Classic board could serve, since
   none is dead-end-free and the smallest is 33 holes in a 7×7 box — 40dp cells at
   `tutorial_board_size` 280dp, under the spec's own 48dp floor
   (`game-designer-tutorial-boards.md:136-167`).

3. **Run green**, then run the full `check`.

### Files

| Action | Path |
|---|---|
| create | `domain/src/main/kotlin/com/app/pegsolitaire/domain/tutorial/TutorialBoards.kt` |
| create | `domain/src/test/kotlin/com/app/pegsolitaire/domain/tutorial/TutorialBoardsTest.kt` |

### Verify

```bash
./gradlew :domain:test --tests '*TutorialBoardsTest*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- `TutorialBoardsTest.kt` contains at least 5 `@Test` methods, with the five names above verbatim.
- `grep -rn 'TutorialBoards' domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` → **0 hits**.
- `grep -rn 'CLASSIC_SPECS' domain/src/main/kotlin/com/app/pegsolitaire/domain/tutorial/` → **0 hits** (the object does not even mention the list it must stay out of).
- `grep -rn 'android' domain/src/main/kotlin/com/app/pegsolitaire/domain/tutorial/` → **0 hits** (`:domain` stays Android-free).
- Deliberately break it once: change T2's peg set to the rejected 5-cell plus with 3 pegs and confirm test 1 **fails** with `worstTerminalPegs`-style evidence, then restore. A proof that cannot fail is not a proof.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** the three boards exist in `:domain`, every legal line of every one of them is proven
to end on one peg by a test that runs in `check`, and neither the object nor the catalogue
mentions the other.

---

## S2 — The Home progress aggregate (risk 1)

**Depends on:** nothing. **Independent of:** S1, S3.

<objective>
Add one `Flow`-returning SQL aggregate that computes both derived ribbon values in a single row,
surface it as one repository member with the house fail-degraded contract, and collect it in
`HomeViewModel` as a sixth independent collector — with no UI consuming it yet.
</objective>

Ending this slice with the state fields present and unread is deliberate: it makes the data
mechanism reviewable on its own, and it makes S6's band a rendering change rather than a
data-plus-rendering change.

### Test-first order

1. **`ProgressRepositoryTest.theHomeProgressFlowSumsStarsAcrossBothModesAndCountsOnlyClearedClassicBoards`**
   — against the **real in-memory Room DB**, as that file already does. Seed Classic *and*
   Challenge rows so both the present `WHERE` and the deliberately absent one are pinned:
   - `totalStars` sums `stars` over **both** modes with **no** `WHERE` — the same both-modes
     semantics `totals()` has (`LevelResultDao.kt:66`), which is what the pill's label ("Stars", a
     total) means.
   - `classicBoardsCleared` counts rows with `mode = 'classic'` **and** `stars >= :minStars`
     bound to `ProgressConstants.ANY_STARS`. Assert **parity with the existing cleared predicate**
     (`ProgressRepository.kt:575-577`: `filter { it.stars >= minStars }` with the same default) —
     `ANY_STARS` is `0` and its KDoc is explicit that *"a stored row means the board was cleared
     regardless of how well"* (`ProgressRepository.kt:711-716`). The test must state that, so a
     later reader does not "fix" `>= 0` into `>= 1` and silently change a shipped number.
   - The flow **re-emits** after a new row is upserted (Room invalidates on write), so the ribbon
     cannot go stale — this is the property `aggregate()` structurally cannot have.

2. **`ProgressRepositoryFailureTest.theHomeProgressFlowDegradesToUnknownWhenTheDatabaseReadThrows`**
   — the sibling of `observeResultsDegradesToAnEmptyListWhenTheDatabaseReadThrows`
   (`ProgressRepositoryFailureTest.kt:63`). A throwing DAO must produce `HomeProgress.UNKNOWN`,
   **never `0`** and never a crash. This is criterion 13a and it is the one that protects a player
   with forty stars from being told they have none on the first screen they see.

3. **Then the production code**, in this order:
   - `LevelResultDao.observeHomeProgress(classicMode: String, minStars: Int): Flow<HomeProgressTotals>`
     — the `FROM`-less correlated-subquery shape of `totals()` (`LevelResultDao.kt:59-75`), so it
     always returns exactly one row and the type is non-null; the same `COALESCE` on the only
     nullable aggregate; the same `ProgressPersistenceKeys.COLUMN_*` / `TABLE_LEVEL_RESULT`
     constant bindings every other query in that file uses. **No literal column name, no literal
     mode string, no literal `0`** (QUAL-03).
   - `HomeProgressTotals` — a two-field projection in
     `app/src/main/java/com/app/pegsolitaire/data/db/ProgressProjections.kt`, beside
     `LevelResultTotals` (`:41`) and `ClassicStanding` (`:63`).
   - `HomeProgress` — a UI-layer read model (`data class HomeProgress(val totalStars: Int, val classicBoardsCleared: Int)`)
     with a `UNKNOWN` companion value, so no Room type reaches a ViewModel (R-6). Put it beside
     `LevelResult` in `ProgressRepository.kt`.
   - `ProgressRepository.observeHomeProgress(): Flow<HomeProgress>` — interface member + Room impl,
     carrying `.catch { emit(HomeProgress.UNKNOWN) }`, the same T-05-10 contract as
     `observeResults` (`ProgressRepository.kt:580-585`).

4. **Then the doubles.** The compiler names every site. **There are 11, not 16** — see §12.5 for
   the corrected count, the file:line of each, and the five files that need nothing. Each gets one
   line:
   `override fun observeHomeProgress(): Flow<HomeProgress> = flowOf(HomeProgress.UNKNOWN)`
   — except `HomeViewModelTest`'s double, which needs a settable backing flow so test 5 below can
   drive it.

5. **`HomeViewModelTest.theRibbonValuesAreNullUntilTheFirstProgressEmission`** — Turbine. Assert
   the first `HomeUiState` has `totalStars == null` and `classicBoardsCleared == null`, and only
   the emission after the repository emits carries values. This is D-03c's placeholder rule as an
   assertion rather than a paragraph.

6. **Then `HomeUiState` + `HomeViewModel`:**
   - `HomeUiState` gains `val totalStars: Int? = null` and `val classicBoardsCleared: Int? = null`.
   - `HomeViewModel.init` gains **one** collector in the existing independent-collector shape:
     ```
     viewModelScope.launch {
         progressRepository.observeHomeProgress().collect { p ->
             _uiState.update { it.copy(totalStars = p.totalStars, classicBoardsCleared = p.classicBoardsCleared) }
         }
     }
     ```

### Forbidden in this slice — each is a plausible wrong turn, and each is an acceptance criterion

- **No Kotlin fold over `observeResults`** anywhere. Over `MODE_CHALLENGE` that is the whole
  unbounded ladder in memory on the start destination.
- **No `combine(...)` restructuring of `HomeViewModel.init`.** `combine` emits nothing until every
  input has emitted once, which would make the whole Home state wait on a database round trip and
  destroy the placeholder rule structurally. Keep the independent collectors.
- **No second collection** for the second pill. One query, two fields, one collector.
- **No `aggregate()`** — `suspend`, one-shot, goes stale after every solve, and its `totalStars`
  has no `WHERE`.
- **No `runBlocking`, no `first()` on the start path, no Home-wide loading state.**
- **No schema change.** A new `SELECT` over existing columns is not a migration: the exported Room
  schema and `roomSchemaVersion` must be byte-identical after this slice.

### Files

| Action | Path |
|---|---|
| modify | `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt` (+1 `@Query`) |
| modify | `app/src/main/java/com/app/pegsolitaire/data/db/ProgressProjections.kt` (+1 projection) |
| modify | `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt` (+1 interface member, +1 read model, +1 impl) |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt` (+2 nullable fields) |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt` (+1 collector) |
| modify | 11 test files (§12.5), one `override` line each |
| modify | `app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryTest.kt`, `ProgressRepositoryFailureTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt` (+1 test each) |

### Verify

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*ProgressRepository*' --tests '*HomeViewModelTest*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- `grep -c 'observeHomeProgress' app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt` returns 1 — one query, not two.
- `grep -rn 'observeResults' app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt` → **0 hits** (no fold).
- `grep -rn 'combine(' app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt` → **0 hits**.
- `grep -rn 'runBlocking\|\.first()' app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt` → **0 hits**.
- `grep -rnE '"classic"|"challenge"|"level_result"|"stars"|"mode"' app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt` → **0 hits outside the `@Query` template's constant interpolations** (QUAL-03: no magic strings).
- `git diff --stat app/schemas/` → **empty**; `grep -rn 'roomSchemaVersion\|version = ' app/src/main/java/com/app/pegsolitaire/data/db/PegSolitaireDatabase.kt` unchanged vs `develop`.
- Every one of the 11 doubles in §12.5 compiles with exactly one added line; `git diff --stat` on those files shows `+1` insertion each (except `HomeViewModelTest.kt`).
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** both ribbon values arrive from one aggregated row, a failed read renders as unknown
rather than zero, the ViewModel holds them as nullable placeholders, and no UI reads them yet.

---

## S3 — Lift the jump cue into a shared effect

**Depends on:** nothing. **Independent of:** S1, S2. **Must land before:** S4.

<objective>
Move `JumpSound` from a `private @Composable` in `GameScreen.kt` to a shared effect both the game
screen and the tutorial can call, changing nothing about when it fires or what it plays.
</objective>

**Why it is its own commit.** The qa-engineer's ruling costs "one lifted composable and one call
site"; its named risk is that a developer re-implements the effect in `TutorialScreen.kt` and the
`REMOVAL_LAYER_DELAY_MS` layering and the `runIndex` pitch ladder drift apart
(`qa-engineer-tutorial-audio.md:389-392`). A commit whose only acceptance criterion is *the
existing sound tests pass byte-unchanged* makes the lift reviewable as a pure refactor, and makes
the copy unavailable to S4.

**Why this does not touch §5.1.** The four prohibitions are about `GameScreen` *the composable* and
`GameViewModel` *the state holder*, not about where an effect is declared — now stated in the spec
itself as a **clarification, not a relaxation** (`ux-spec.md:864-877`; originally
`qa-engineer-tutorial-audio.md:78-87`). `JumpSound` takes a `MoveTransition?` and reads
`LocalSoundPlayer`; it touches no `GameViewModel`, no repository, no navigation and no persistence.
**Lifting one effect out of that file is not licence to host the file**, and the §10 grep gates
still apply verbatim. After this slice the tutorial calls a shared effect from
`com.app.pegsolitaire.audio`; it still hosts `BoardCanvas`, still runs `TutorialViewModel`, still
writes no `LevelResult`, still has only `popBackStack()`. And `BoardCanvas.kt` gains nothing:
`grep -n 'SoundCue\|SoundPlayer\|sound' .../ui/components/BoardCanvas.kt` must stay at **0 hits**.

### Test-first order

There is no new behaviour, so there is no new test — the discipline here is the **inverse**: the
existing tests are the specification and they must pass **without being edited**.

1. Run `./gradlew :app:testDebugUnitTest --tests '*GameScreenSoundTest*'` and record it green
   **before** touching anything. `aJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt`
   (`GameScreenSoundTest.kt:76`) and `aFreshlyComposedScreenMakesNoSoundAtAll` (`:187`) are the
   two that matter.
2. Move the body of `JumpSound` (`GameScreen.kt:321-330`) verbatim into a new
   `app/src/main/java/com/app/pegsolitaire/audio/JumpSoundEffect.kt`, package
   `com.app.pegsolitaire.audio`, beside `LocalSoundPlayer.kt` — the file that already holds the
   house's other composable audio helper, `withClickSound` (`LocalSoundPlayer.kt:36`). Name it
   `JumpSoundEffect(transition: MoveTransition?)`, visibility `internal`. Carry its KDoc across
   unchanged and add one sentence: it is shared by the gameplay screen and the tutorial screen
   because SOUND-01 says *every* peg jump sounds, and both screens jump real pegs.
3. `GameScreen.kt:158` becomes `JumpSoundEffect(transition = state.animating)`; the private
   function is deleted. **Nothing else in `GameScreen.kt` changes** — the other six effects stay
   private where they are, because no other screen needs them and every one of them is ruled
   silent on the tutorial (`qa-engineer-tutorial-audio.md:154-161`).
4. Run the same test command and confirm it is still green with **zero diff** in
   `GameScreenSoundTest.kt`.

### Files

| Action | Path |
|---|---|
| create | `app/src/main/java/com/app/pegsolitaire/audio/JumpSoundEffect.kt` |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` (one call site; one private function deleted) |

### Verify

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*GameScreenSoundTest*' --tests '*ButtonSoundTest*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- `git diff --stat app/src/test/` for this commit is **empty** — not one test file changed. That is the whole proof that this is a refactor.
- `grep -c 'private fun JumpSound' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` returns **0**.
- `grep -rn 'REMOVAL_LAYER_DELAY_MS' app/src/main` returns exactly **one** call site (in `JumpSoundEffect.kt`) plus its constant declaration — the layering exists once.
- `grep -rn 'playJump' app/src/main` returns exactly one call site plus the `SoundPlayer`/`SoundPoolPlayer` declarations — the pitch ladder exists once.
- `grep -n 'SoundCue\|SoundPlayer\|sound' app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt` → **0 hits**.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** the jump cue is declared once, called from `GameScreen` exactly as before, and every
existing sound test passes without having been touched.

---

## S4 — The tutorial destination, its ViewModel, and the exit ordering (risk 2)

**Depends on:** S1 (boards), S3 (shared jump effect). **Independent of:** S2.

<objective>
Build the tutorial as a `@Serializable` nav destination with a destination-scoped
`TutorialViewModel`, whose single exit path **awaits** `markSeen()` on the application scope before
emitting its navigation signal — and a screen that hosts `BoardCanvas`, never `GameScreen`.
</objective>

This is the slice §5.1 exists for. Read `docs/ux-spec.md` §5.1 before writing a line: hosting
`GameScreen` or `GameViewModel` here is the one mistake in this phase that looks correct on screen
and is wrong in the database, because `GameViewModel`'s `Won` path calls `clearSnapshot()`
(`GameViewModel.kt:963`) and `challengeProgress.advancePast(...)` (`:973`), and **every tutorial
step ends on exactly one peg**, so it would fire three times per run and destroy two of the three
cloud-save device oracles.

### Test-first order

**Group A — the exit ordering. Write these first; they are the reason this slice exists.**

| # | Test | Asserts |
|---|------|---------|
| 1 | `TutorialViewModelTest.theExitSignalIsEmittedOnlyAfterTheSeenFlagIsPersisted` | A `TestDispatcher` plus a fake `OnboardingRepository` whose `markSeen()` **suspends** until released. Trigger exit; assert **no** exit signal has been emitted; release the write; assert the signal arrives *then*. This asserts the **ordering**, not merely the call — a test that only checks "markSeen was called" passes against the fire-and-forget bug. |
| 2 | `TutorialViewModelTest.everyExitRouteSkipBackAndCompletionPersistsTheSeenFlagExactlyOnce` | Three callers, one function, one write each. |
| 3 | `TutorialViewModelTest.aSecondExitIsALatchedNoOp` | Two rapid exits → one write, one signal. |
| 4 | `TutorialViewModelTest.theSeenFlagIsWrittenOnAScopeThatOutlivesTheScreen` | Cancel the ViewModel's own scope immediately after the exit is requested and assert the write still completes — the `@ApplicationScope` clause (`di/DataModule.kt:88-89`; precedent `ProgressSubmitter.onProfileSwapResolved`, `HomeViewModel.kt:313-323`). |

**Group B — the step machine.**

| # | Test | Asserts |
|---|------|---------|
| 5 | `TutorialViewModelTest.aStepAdvancesOnlyWhenItsBoardHoldsExactlyOnePeg` | `board.pegCount == 1` is the predicate, for all three steps. **No `GameStatus`, no `minimumPegs` comparison in `:app`** — reaching for the game's status type is the first step down the path that ends at `GameViewModel`. (Mechanical note for the developer: `GameUiState` *requires* a `status` field, and `BoardCanvas` never reads it — verified: `grep -n 'state\.' BoardCanvas.kt` shows `board`, `animating`, `selected`, `showHints`, `legalTargets`, `hintSource`, `hintTarget` and nothing else. So pass `GameStatus.Playing` as a constant and evaluate the predicate separately.) |
| 6 | `TutorialViewModelTest.theThreeStepsRunInTheOrderT1T2T3AndTheTutorialEndsAfterTheThird` | Indexes `TutorialBoards.ALL` in order. |
| 7 | `TutorialViewModelTest.showHintsIsForcedOnRegardlessOfThePersistedSetting` | D-01b — legal destinations are always visible, so no step can become a guessing game. |
| 8 | `TutorialViewModelTest.aWrongTapNeverLosesProgressAndNeverProducesAnErrorState` | D-01c: tapping a peg with no legal jump selects it and yields zero destinations; tapping an illegal destination keeps the selection. No error field exists on the state to assert — assert that too. |
| 9 | `TutorialViewModelTest.theTutorialViewModelsOnlyCollaboratorsAreTheOnboardingRepositoryAndTheApplicationScope` | Reflection over the primary constructor's parameter types. This is prohibition 3 (§10) as a test rather than a grep: it cannot be satisfied by a class that injects `ProgressRepository`, `ResumeRepository` or `ChallengeProgress`. |

**Group C — navigation.**

| # | Test | Asserts |
|---|------|---------|
| 10 | `NavFlowTest.theTutorialDestinationPopsBackToWhereItWasEnteredFrom` | Driven from Home **and** from Settings; asserts the destination after the pop in each case. |
| 11 | `NavFlowTest.theTutorialDestinationsOnlyOutboundEdgeIsAPopBackToItsEntryPoint` | Prohibition 4: no path from `Tutorial` reaches `Results`, `Gameplay` or `ClassicCatalog`. |

**Group D — audio (D-01i, `ux-spec.md:233-262`; enumerated contract `:826-838`).**

| # | Test | Asserts |
|---|------|---------|
| 12 | `TutorialSoundTest.aCompletedTutorialJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt` | `FakeSoundPlayer` (`app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt`) installed via `CompositionLocalProvider(LocalSoundPlayer provides …)`; the template is `GameScreenSoundTest.kt:76`. Cue order **and** pitch. |
| 13 | `TutorialSoundTest.theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep` | The negative half. `ButtonSoundTest.kt:36-52` is the repo's own rule: *"selectivity is only a real property if both halves are asserted."* |
| 14 | `TutorialSoundTest.theCompletionStarPopsWithoutTheWinOrStarPopCue` | A tutorial is not a victory. |
| 15 | `TutorialSoundTest.theCompletionButtonPlaysTheForwardPressCueAndSkipPlaysTheDismissCue` | SOUND-04 / D-11, via the house `withClickSound` idiom (`LocalSoundPlayer.kt:36`; call sites `HomeScreen.kt:447`, `ResultsActions.kt:96`). This is the gap the qa-engineer found unruled — a developer reaching for the house idiom would otherwise have shipped either a violation or an omission with nothing catching it. |

**Group E — the screen.**

| # | Test | Asserts |
|---|------|---------|
| 16 | `TutorialScreenTest.theTutorialAdvancesOnlyWhenThePlayerCompletesARealJumpAndNeverByAButton` | The one Compose UI test per screen's main flow (`rules/testing.md`). Finds the screen by `TUTORIAL_SCREEN_TEST_TAG` and the board by `BOARD_CANVAS_TEST_TAG` (`BoardCanvas.kt:49`) — **never by button copy**, which is the flaw §13 already names. Asserts there is no Next/Continue node at any step. |
| 17 | `TutorialScreenTest.skipIsReachableOnEveryStep` | Criterion 5. |
| 18 | `TutorialScreenTest.theStepPipRowAnnouncesItselfAsOneNodeSayingStepNOfThree` | `clearAndSetSemantics` + `tutorial_step_progress`, the `EarnedStarPips` idiom (`BoardPreview.kt:100-104`). |

### Then the production code

1. **`Routes.kt`** — `@Serializable data object Tutorial`, KDoc in the file's established voice:
   no arguments; the only outbound edge is `popBackStack()`; it is deliberately *not* reachable
   from `Gameplay` or `Results`.
2. **`TutorialViewModel`** — `@HiltViewModel`, constructor-injected `OnboardingRepository` and the
   `@ApplicationScope CoroutineScope`, and **nothing else**. State in a plain
   `MutableStateFlow<TutorialUiState>`; **no `SavedStateHandle`** — a saved step cursor would
   survive process death and resume at T2, contradicting D-01e (§5, `ux-spec.md:691-694`). One
   `SharedFlow`/`Channel` exit signal. One `exit()` function, latched, awaiting `markSeen()` on the
   application scope before emitting.
3. **`TutorialUiState`** — `@Immutable`; the step index, the assembled `GameUiState` for
   `BoardCanvas`, the caption string resource id, and the nudge/escalation tick. No error field
   (D-01c), no score, no retry count.
4. **`TutorialAnim`** — a scoped object holding `STEP_ADVANCE_DELAY_MS`, `NUDGE_DELAY_MS`,
   `COACH_PULSE_MS` (unitless ms, so not `dimens.xml`; §6.4).
5. **`TutorialRoute`** — `hiltViewModel()` **inside** `composable<Tutorial>`, so the ViewModel binds
   to the `NavBackStackEntry` and is cleared on pop; `collectAsStateWithLifecycle()`; collects the
   exit signal and calls the hoisted `onExit`.
6. **`TutorialScreen`** — stateless. Four bands in a `verticalScroll` column: Header (pips +
   `Skip`), Board (`BoardCanvas`, `interactive = true`, on a `surfaceContainerHigh` clay slab at
   `tutorial_board_size`), Caption (`tutorial_caption_min_height` reserved so a 1↔2-line swap never
   reflows the board), Footer (empty until T3 completes, then the `CtaGradient` CTA). Carries
   `TUTORIAL_SCREEN_TEST_TAG`. Light and dark `@Preview` pair (`rules/compose.md`).
7. **`BackHandler`** on the destination, routing to the same `exit()`. This becomes the codebase's
   **second** `BackHandler` — the first is `AdQuiescenceScrim.kt:61` — and must carry the same
   kind of comment explaining why it exists, as that one does.
8. **`AppNavHost`** — `composable<Tutorial> { tutorialContent { navController.popBackStack() } }`,
   in the existing content-slot idiom (`AppNavHost.kt:101-145`). `popBackStack()` and nothing else.
9. **Audio wiring** — `JumpSoundEffect(transition = state.animating)` from S3; `withClickSound`
   on the CTA (`ButtonForward`) and on `Skip` (`ButtonDismiss`). Nothing else.
10. **Strings and dimens** — the five tutorial strings and three tutorial dimens from §12.1/§12.3.

### Files

| Action | Path |
|---|---|
| create | `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialRoute.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModel.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialUiState.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialAnim.kt` |
| modify | `app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt` (+1 destination) |
| modify | `app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt` (+1 `composable<Tutorial>`, +1 content slot) |
| modify | `app/src/main/res/values/strings.xml` (+5 tutorial strings) |
| modify | `app/src/main/res/values/dimens.xml` (+3 tutorial dimens) |
| create | `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModelTest.kt` |
| create | `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialScreenTest.kt` |
| create | `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialSoundTest.kt` |
| modify | `app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt` (+2 tests) |

`OnboardingOverlay.kt` is **not** deleted in this slice — Home still hosts it until S5, so the
build stays green and the two by-design test breakages land in one place.

### Verify

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*Tutorial*' --tests '*NavFlowTest*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- Every prohibition command in §10 passes.
- `grep -rn 'SavedStateHandle' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **0 hits**.
- `grep -c 'const val TUTORIAL_SCREEN_TEST_TAG' app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt` returns 1.
- `grep -rni 'undo' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **0 hits**, and no `tutorial_*` string in `strings.xml` contains "Undo" — §5: no Undo control and no copy naming Undo anywhere in the tutorial.
- `grep -rn '@Preview' app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt` returns **2** (light + dark).
- 18 named tests exist with the names above; `./gradlew :app:testDebugUnitTest --tests '*Tutorial*'` exits 0.
- Deliberately break test 1 once: make `exit()` fire-and-forget (`viewModelScope.launch { markSeen() }` then emit immediately) and confirm `theExitSignalIsEmittedOnlyAfterTheSeenFlagIsPersisted` **fails**, then restore. If it still passes, the test asserts the call and not the ordering and must be rewritten.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** the tutorial is a destination the player can complete or skip, the seen flag is
provably written before the screen leaves, the jump sounds and nothing else on the board does, and
every §5.1 prohibition has a passing proof.

---

## S5 — Move the entry point: Home loses the info button, Settings gains the row

**Depends on:** S4. **Independent of:** S2.

<objective>
Delete the Home How-to-play `IconButton` and its string, retire `onShowHowToPlay` as a Home
concern, rename the standing flag to `tutorialPending`, add the Settings › General row, fix one
stale supporting line — and retarget the two tests that break by design.
</objective>

### Test-first order

The two breakages are the specification here. Retarget them **before** the code change, watch them
fail, then make them pass.

1. **`ButtonSoundTest.theHomeTopBarHowToPlayAndSettingsIconsMakeNoSound` (`ButtonSoundTest.kt:105-123`).**
   It clicks `R.string.how_to_play` by content description; that button and that string are both
   deleted. **Retarget, do not delete:** split it into
   - `theHomeSettingsGearMakesNoSound` — the gear half of the assertion, valid unchanged (and still
     valid after S6 moves the gear into the hero, because `settings_action` is preserved), and
   - `theSettingsHowToPlayRowPlaysTheForwardCueAndOpensTheTutorial` — the row's cue under D-09's
     "a navigational CTA speaks" rule (§4.5).
   Both halves asserted: that is the file's own stated standard (`ButtonSoundTest.kt:36-52`).
2. **`HomeScreenTest.theProfileSwapDialogIsNeverStackedOnTheOnboardingOverlay` (`HomeScreenTest.kt:148-159`).**
   It locates the overlay by the hardcoded literal `"Got it"`
   (`ONBOARDING_DISMISS_LABEL`, `HomeScreenTest.kt:367`) and Home stops hosting the overlay.
   **Retarget, do not delete:** `theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending` —
   find the dialog by `PROFILE_SWAP_DIALOG_TEST_TAG` (`ProfileSwapDialog.kt:33`) exactly as today,
   and the pending state by the **state field** `HomeUiState.tutorialPending`. Delete the
   `ONBOARDING_DISMISS_LABEL` constant with it: a screen found by its button copy is the defect the
   retarget exists to remove, not a thing to preserve.
   **Neither test may be `@Ignore`d and neither may be deleted** (`rules/kotlin-android.md`; §13).
3. **`HomeViewModelTest.theTutorialIsOfferedOnceAndNeverAgainOnceTheFlagIsPersisted`** — new;
   pins that `tutorialPending` is the mirror of `!onboardingSeen` and that the Play-Games prompt
   and profile-swap dialog remain gated on the **persisted** flag, not on visibility
   (`HomeViewModel.kt:119-128`, `:145-160`).
4. **`SettingsScreenTest.theGeneralSectionOpensWithAHowToPlayRowAboveTheSoundToggle`** — new;
   asserts order, because the entrance cascade is positional.

### Then the production code

- **Delete** `HomeScreen.kt:348-354` (the `Icons.Rounded.Info` action), the `onShowHowToPlay`
  parameter (`HomeScreen.kt:129`, passed at `:151`, wired at `HomeRoute.kt:48`), and
  `HomeViewModel.onShowHowToPlay()` (`HomeViewModel.kt:227-229`). Once Home has no affordance the
  handler has no caller, and leaving it would leave a second writer of a flag the guard defends
  against (D-02b).
- **Delete** `app/src/main/java/com/app/pegsolitaire/ui/onboarding/OnboardingOverlay.kt` and its
  hosting at `HomeScreen.kt:174-176`.
- **Rename** `HomeUiState.onboardingVisible` → `tutorialPending` (the mirror of `!seen`), and
  `HomeScreen.kt:198`'s guard to `!state.tutorialPending`. The guard survives **in shape**: the
  wipe question may be withheld, never raised early.
- **Auto-start** — a `LaunchedEffect` in `HomeRoute` on the standing `tutorialPending` flag calling
  a hoisted `onStartTutorial`, which `AppNavHost` implements as
  `navigate(Tutorial) { launchSingleTop = true }`. First-run behaviour is unchanged (D-02a).
- **Settings row** — a `SettingsLinkRow` in `GeneralSection` (`SettingsScreen.kt:194-218`),
  **above** the Sound toggle: `settings_how_to_play_label` / `settings_how_to_play_supporting`,
  `SoundCue.ButtonForward`, full-row target at `settings_row_min_height`. `SettingsScreen` gains an
  `onShowHowToPlay` parameter; `AppNavHost` routes it to `navigate(Tutorial)`.
- **Renumber the entrance cascade.** The order constants are positional and currently run 0..9
  (`SettingsScreen.kt:664-673`). Inserting the row means a new `HOW_TO_PLAY_ROW_ORDER = 1` and
  `+1` on the eight that follow. This is mechanical and easy to get half-right — it is called out
  because a half-renumbered cascade shows as two rows entering together and no test catches it.
- **Strings:** delete `how_to_play` (`strings.xml:62`); add the two Settings strings; fix
  `settings_sound_supporting` (`strings.xml:143`, "Saved for the upcoming sound effects" — stale
  since Phase 7) to describe what the toggle now does. See §13.2 for the `onboarding_step_*`
  question this slice must route rather than decide silently.

### Files

| Action | Path |
|---|---|
| delete | `app/src/main/java/com/app/pegsolitaire/ui/onboarding/OnboardingOverlay.kt` |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/home/{HomeScreen,HomeRoute,HomeUiState,HomeViewModel}.kt` |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt` (+1 row, +1 param, cascade renumbered) |
| modify | `app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt` (two `navigate(Tutorial)` call sites) |
| modify | `app/src/main/res/values/strings.xml` (−1, +2, 1 fixed) |
| modify | `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` (retarget) |
| modify | `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt` (retarget) |
| modify | `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/…` (+1 test each) |

### Verify

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*ButtonSoundTest*' --tests '*HomeScreen*' --tests '*Settings*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- `grep -rn 'how_to_play' app/src/main/res/values/strings.xml` → **0 hits**; `grep -rn 'R.string.how_to_play' app/src` → **0 hits**.
- `grep -rn 'Icons.Rounded.Info' app/src/main` → **0 hits** (this also removes half the glyph collision D-04b fixes).
- `grep -rn 'onShowHowToPlay' app/src/main/java/com/app/pegsolitaire/ui/home/` → **0 hits**.
- `grep -rn 'onboardingVisible\|OnboardingOverlay' app/src` → **0 hits**.
- `grep -rn '@Ignore' app/src/test` → **0 hits**; `git diff --stat` shows `ButtonSoundTest.kt` and `HomeScreenTest.kt` **modified, not deleted**, and the retargeted test names are present.
- `grep -c '_ORDER = ' app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt` returns **11**, and the values are `0..10` with no duplicate: `grep -o '_ORDER = [0-9]*' … | sort -u | wc -l` returns 11.
- `grep -n 'settings_sound_supporting' app/src/main/res/values/strings.xml` no longer contains "upcoming".
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** Home carries zero tutorial chrome, Settings › General opens the tutorial, and the two
tests that had to break read as sentences about the new behaviour.

---

## S6 — Home recomposition

**Depends on:** S2 (the data), S5 (the info button and overlay already gone).
**Independent of:** S7.

<objective>
Recompose Home into the five bands of §4.2 — hero, progress ribbon, Continue chips, mode tiles,
empty footer — inside one `verticalScroll` column capped at `content_max_width`, with the
`TopAppBar` deleted and the gear moved into the hero.
</objective>

### Test-first order

1. **`HomeScreenTest.aRibbonPillWithNoValueYetShowsTheUnknownPlaceholder`** — criterion 13:
   `stat_value_unknown` ("—"), never `0`, never blank, announced `stat_value_unknown_desc`.
2. **`HomeScreenTest.aRibbonPillWhoseReadFailedAlsoShowsThePlaceholderAndNeverZero`** — criterion
   13a. The UI-side twin of S2's repository test; together they make "not yet" and "the read
   failed" indistinguishable on screen and both distinguishable from zero.
3. **`HomeScreenTest.theBoardsPillDenominatorIsTheClassicCatalogueSize`** — asserts **5**,
   sourced from `Boards.CLASSIC_SPECS.size` / the `ClassicBoard` enum and never from a stored
   count. Criterion 13b, and the second half of prohibition 5 (§10): a tutorial board joining
   `CLASSIC_SPECS` cannot change a shipped number silently.
4. **`HomeScreenTest.theLevelPillAndTheChallengeTileShowTheSameNumber`** — criteria 12 / 15b. Both
   read `challengeCurrentLevel`, so they cannot diverge by construction; the test pins that they
   are wired to the same field. (Copy is "Level N" — the **next** level to play, off-by-one from
   the highest banked, by design: `ProgressRepository.kt:107-113`. Do not "reconcile" them.)
5. **`HomeScreenTest.eachRibbonPillSpeaksAsOneNodeSayingItsLabelThenItsValue`** — "Stars, 7",
   never a bare number.
6. **`HomeScreenTest.theProgressPillsAreNotTappable`** — §5's negative contract; three more
   competing targets is the opposite of what the founder asked for.
7. **`HomeScreenTest.bothContinueChipsAndBothModeTilesRenderWhenBothResumesExist`** — the D-03f
   oracle 3 guard at unit level. The full first-paint rule (criterion 15a: fully visible without
   scrolling at default font scale on 360×640dp) is a **device** check, not a unit test — record it
   as such in §11.2 rather than claiming it from a Compose rule.

### Then the production code

- **Delete `HomeTopBar` (`HomeScreen.kt:335-363`)** and the `Scaffold`'s `topBar` slot. Its
  gradient Fredoka title moves into the hero at `displaySmall` with `ctaGradientBrush()`; the gear
  becomes a 48dp clay circular chip at the hero's top-end keeping `Icons.Rounded.Settings` and the
  `settings_action` content description **unchanged** (which is what keeps S5's retargeted gear
  assertion passing).
- **Hero band** — `home_hero_min_height`, decorative board art at `home_hero_art_size`.
  **Q-901 is answered: the static peg cluster (option 1).** `contentDescription = null`; fully
  static when animations are off; **no ambient motion of its own** — the two existing `HomeBlobs`
  remain the only drifting elements. `home_hero_min_height` is a *minimum*: the hero is the band
  that yields (art first, then title scale) before anything below it leaves the fold.
- **Progress ribbon** — one `Row`, three equal-weight clay pills at `home_stat_pill_min_height`
  with `home_stat_pill_glyph_size` glyphs, `spacing_sm` apart, each a `surfaceContainerHigh`
  `shapes.medium` surface with `clayShadow(elevation_card)`. Carries `HOME_PROGRESS_RIBBON_TEST_TAG`.
  Pills sized from their **labels**, not their values, so the fill causes no reflow.
- **Continue chips** — behaviour, ordering and data **unchanged** (`HomeScreen.kt:293-318`). One
  visual addition: the Classic chip's trailing accent becomes a `home_resume_thumb_size` board
  thumbnail (`classicResumeLevelId` already identifies the board; the catalogue is static, so no
  new data). The Challenge chip keeps the play arrow.
- **Mode tiles** — containers, glyph badges, titles, supporting slots, entrance stagger, squish,
  `ButtonForward` cue and every callback unchanged (`HomeScreen.kt:498-560`). Each gains trailing
  board art at `home_tile_art_size`.
- **Footer** — empty, and specified as empty so a later phase does not read the gap as an invitation.
- **`verticalScroll` + a weighted spacer between bands 2 and 3**, so slack goes *above* the
  Continue chips and the tiles stay in the thumb zone. Home does not scroll today
  (`HomeScreen.kt:285-292` is a plain `Column`) and at 200% font scale with two Continue chips it
  already overflows — the scroll is part of this change, not polish.
- **Strings and dimens** — the six Home strings and six Home dimens from §12.1/§12.3.

### Non-negotiable (D-03f / D-03g)

- No blocking read, no image decode, no asset load, no bitmap, no new SDK call on first frame.
- Nothing behind a tap, an expander, a "show more" or a scroll position.
- The Challenge tile's level chip stays the oracle; the ribbon's Level pill is **additive**.
- The Stars pill is a **total** and is explicitly not a replacement for the Classic catalogue's
  per-board pips. Do not "simplify" the catalogue by pointing at it.
- No Play Games chip (D-03e). No loading state, skeleton or spinner (§5).

### Files

| Action | Path |
|---|---|
| modify | `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` (bands; `HomeTopBar` deleted) |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/home/ModeGlyphs.kt` (tile/hero art helpers) |
| modify | `app/src/main/res/values/{strings,dimens}.xml` |
| modify | `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt` (+7 tests) |

### Verify

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*HomeScreenTest*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- `grep -rnE 'TopAppBar|BottomAppBar|NavigationBar' app/src/main/java/com/app/pegsolitaire/ui/home/ | grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'` → **0 hits** (comment lines excluded; four `ui/game` files mention `TopAppBar` only in KDoc today).
- `grep -c 'HOME_PROGRESS_RIBBON_TEST_TAG' app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` returns ≥ 2 (declaration + use).
- `grep -rn 'verticalScroll' app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` returns ≥ 1.
- `grep -rn '\.dp\b\|\.sp\b' app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` → no raw numeric literal (the `dpSpLiteralCheck` gate is the real enforcer; this is the fast local check).
- `grep -rn 'CircularProgressIndicator\|isLoading' app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` → **0 hits** (no spinner on the start destination).
- `grep -rn '@Preview' app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` returns ≥ 2 (light + dark).
- `git diff app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt | grep -c '^-.*challenge_tile_continue'` returns **0** — the Challenge tile's level chip was not touched.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** Home is five bands, the ribbon paints "—" and fills without reflow, and all three
D-03f oracles are exactly where the device checklist expects them.

---

## S7 — Gameplay chrome: status strip and action dock

**Depends on:** S3 landing first if parallelised (same file, disjoint region).
**Independent of:** S6.

<objective>
Delete the `TopAppBar` and replace it with a chrome-free status strip (back, title, timer) above
the board and a clay action dock (Undo, Hint, Show-moves, Restart) below it, in the thumb zone —
**Q-902 is answered: the split.**
</objective>

### Test-first order

The strongest tests here are the ones that must pass **unchanged** — they are the proof that the
chrome moved and the function did not.

1. **Record green first, do not edit:** `ButtonSoundTest.kt:226-244` (finds the back control by
   `navigate_back` and the toggle by `show_moves_on`) and `GameFlowTest.kt:129` (finds the timer by
   `elapsed_time`). All three content descriptions are deliberately preserved (D-05b, §4.3, §4.4).
   If either test needs editing, a content description drifted and the fix is in the production
   code, not the test.
2. **`GameScreenChromeTest.theGameplayScreenHasNoMaterialTopAppBar`** — criterion 16.
3. **`GameScreenChromeTest.theBackControlTitleAndTimerAreAllInTheStatusStrip`** — by
   `GAME_STATUS_STRIP_TEST_TAG`; criteria 17, 18.
4. **`GameScreenChromeTest.allFourActionsLiveInTheActionDock`** — by `GAME_ACTION_DOCK_TEST_TAG`;
   criterion 19.
5. **`GameScreenChromeTest.showMovesCarriesItsStateByFillAndNotByTintAlone`** — criterion 21 and
   D-04b. This is a **fix, not a restatement**: `HintToggleControl.kt:45-51` carries state in
   `primary` vs `onSurfaceVariant` tint and nothing else today, which breaks rules/compose.md's
   "never colour alone". Assert the ON node has the filled-pill marker and the OFF node does not,
   alongside the existing `show_moves_on`/`show_moves_off` descriptions.
6. **`UndoControlTest` / `HintControlTest`** — extend, do not rewrite: `enabled = canUndo`
   (criterion 22) and the thinking-state spinner with `enabled = false` (criterion 23) are correct
   today and this is a reskin of them (D-04d).
7. **`GameScreenChromeTest.noControlShowsAnAllowanceCountBadgeOrAdGlyph`** — criterion 24; the
   negative contract inherited verbatim from the ad-allowance ruling at `GameUiState.kt:131-135`
   (an **earlier phase's** D-05, not this document's toolbar D-05).

### Then the production code

- **New `GameStatusStrip.kt`** — a plain `Row`, `game_status_strip_height`, transparent,
  `statusBarsPadding()`, `spacing_md` horizontal padding. Leading: back chip at
  `game_status_back_size`, `surfaceContainerHigh`, `clayShadow(elevation_pressed)`, `claySquish()`,
  `Icons.AutoMirrored.Rounded.ArrowBack` at `top_bar_icon_size`, `navigate_back`, **silent**.
  Centre: `GameTopBarTitle` logic **verbatim** (`GameScreen.kt:537-552`), `titleMedium`,
  `maxLines = 1`, `weight(1f)`, `semantics { heading() }`. Trailing: timer pill at
  `game_timer_pill_height`, `shapes.small`, `GameTimerFormat.format()` in
  `titleMedium.withTabularFigures()`, `elapsed_time`. Carries `GAME_STATUS_STRIP_TEST_TAG`.
  **No actions live here.**
- **New `GameActionDock.kt`** — one clay slab below the board: `surfaceContainerHigh`,
  `shapes.large`, `clayShadow(elevation_card)`, `game_action_dock_height`,
  `navigationBarsPadding()`, four `game_action_button_size` targets in a `SpaceEvenly` `Row` with
  ≥ `spacing_sm` gaps, glyphs at `game_action_glyph_size`. Order and treatment per §4.4: Undo and
  Hint filled `primaryContainer` (loud); Show-moves and Restart transparent with an `outline`
  border (quiet); Show-moves ON adds a filled `primaryContainer` pill. Carries
  `GAME_ACTION_DOCK_TEST_TAG`. **Every content description is the existing string — no new copy.**
- **New `HintIcon.kt`** (lightbulb) — replaces `Icons.Rounded.Search`, which reads as "search" or
  "zoom", not "show me a winning move" (`HintControl.kt:5`, `:69`).
- **New `ShowMovesIcon.kt`** (a peg with two trailing destination dots — literally what the feature
  draws) — replaces `Icons.Rounded.Info`, whose meaning collided with the Home info button now
  deleted in S5.
  Both follow the `UndoIcon.kt` / `ModeGlyphs.kt` precedent exactly: 24-unit viewport, filled
  opaque black, recoloured by the host `Icon`'s tint, `@Suppress("MagicNumber")` on the path data
  **with the same justification comment the existing two carry** — that is the established,
  auditor-visible precedent, not a new suppression.
- **Delete** `GameTopBar` (`GameScreen.kt:489-527`) and the `Scaffold`'s `topBar` slot
  (`:171-187`). **Keep** all five controls, the context-aware title, the timer label, and every
  handler signature `GameScreen` already takes. Nothing the player can *use* is removed.
- **Retarget the four control composables** from `top_bar_touch_target` to
  `game_action_button_size`: `UndoControl.kt:38`, `HintControl.kt:56` and `:66`,
  `HintToggleControl.kt:39`, `RestartControl.kt:35`. This is what frees the token for S8.
- **Not an M3 bar, under any name** (D-05c). The strip is not a `TopAppBar` or
  `CenterAlignedTopAppBar`; the dock is not a `BottomAppBar` or `NavigationBar` —
  `NavigationBar` in particular would announce four in-game actions to TalkBack as navigation
  destinations, which they are not.

### Files

| Action | Path |
|---|---|
| create | `app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/game/HintIcon.kt` |
| create | `app/src/main/java/com/app/pegsolitaire/ui/game/ShowMovesIcon.kt` |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` (`GameTopBar` deleted; strip + dock placed) |
| modify | `app/src/main/java/com/app/pegsolitaire/ui/game/{Hint,HintToggle,Undo,Restart}Control.kt` |
| modify | `app/src/main/res/values/dimens.xml` (+6 gameplay dimens) |
| create | `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenChromeTest.kt` |
| modify | `app/src/test/java/com/app/pegsolitaire/ui/game/{UndoControlTest,HintControlTest}.kt` |

### Verify

```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*Game*' --tests '*ButtonSoundTest*'
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline
```

### Acceptance criteria

- `git diff --stat app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt` for this commit is **empty** — the two preserved-description tests were not edited.
- `grep -rnE 'TopAppBar|BottomAppBar|NavigationBar' app/src/main/java/com/app/pegsolitaire/ui/game/ | grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'` → **0 hits**.
- `grep -rn 'Icons.Rounded.Search\|Icons.Rounded.Info' app/src/main` → **0 hits**.
- `grep -rn 'top_bar_touch_target' app/src/main` → **0 hits** (all six callers retargeted; this is S8's precondition).
- `grep -c 'GAME_STATUS_STRIP_TEST_TAG\|GAME_ACTION_DOCK_TEST_TAG' app/src/main/java/com/app/pegsolitaire/ui/game/*.kt` ≥ 4.
- `git diff app/src/main/res/values/strings.xml` for this commit is **empty** — the dock adds no copy.
- `grep -c '@Suppress' app/src/main/java/com/app/pegsolitaire/ui/game/HintIcon.kt app/src/main/java/com/app/pegsolitaire/ui/game/ShowMovesIcon.kt` returns 1 each, each `@Suppress("MagicNumber")` on path data with a justification comment, matching `UndoIcon.kt` verbatim in shape. Any other suppression is a code-auditor question.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

**Done when** the gameplay screen has no Material bar, every control still works, all four actions
sit in the thumb zone, and the two accessibility defects (Search-as-Hint, tint-only Show-moves) are
closed.

---

## S8 — Retirement, measurement, evidence

**Depends on:** S6, S7. **Independent of:** nothing — this closes the phase.

<objective>
Delete the two tokens that have lost their last caller, run the two-arm cold-start measurement,
and write `15-BUILD-EVIDENCE.md`.
</objective>

### Order

1. **Retire the two tokens.** `top_bar_touch_target` (`dimens.xml:110`) — freed by S7;
   `onboarding_illustration_size` (`dimens.xml:51`) — freed by S5. **Check the callers first, in
   this order**, because a premature deletion fails the build in a confusing place:
   ```bash
   grep -rn 'top_bar_touch_target\|onboarding_illustration_size' app/src   # must be 2 hits, both in dimens.xml
   ```
   `onboarding_illustration_size` never took effect in the first place: `BoardPreview.kt:79`
   overrides its caller's size with `catalog_preview_size` 96dp, so the 160dp the overlay passed at
   `OnboardingOverlay.kt:144` has been lying about the layout since Phase 2. Say so in the commit
   body.
2. **Run the cold-start measurement** (§11) and write both tables.
3. **Write `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`**, using the *identical* field
   shape as `06-BUILD-EVIDENCE.md:315-323`.
4. **Final `check`.**

### Acceptance criteria

- `grep -rn 'top_bar_touch_target\|onboarding_illustration_size' app/src` → **0 hits**.
- `15-BUILD-EVIDENCE.md` exists and contains `## Cold-start measurement`, **two** tables (arm A and
  arm B), the seeded-ladder row, the same precision caveat as `06-BUILD-EVIDENCE.md:325-327`, and
  both pass conditions evaluated explicitly.
- Criterion 15f (placeholders, no spinner, no reflow) is recorded as a **separate visual check at
  the same sitting** — the timing number does not cover it.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0.

---

## 10. The §5.1 prohibitions as checks

Each row is a command or a named test a reviewer runs. None requires trusting the developer's
description of the diff. Run the whole block after S4, and again after S8.

| # | Prohibition | Proof |
|---|-------------|-------|
| **P1** | The tutorial hosts `BoardCanvas` and **never `GameScreen`** | `grep -rn 'GameScreen' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **0 hits**, **and** `grep -rn 'BoardCanvas(' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **≥ 1**. Both halves: absence alone would also be satisfied by a tutorial with no board. |
| **P2** | Its state holder is `TutorialViewModel` and **never `GameViewModel`** | `grep -rn 'GameViewModel' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **0 hits**, **and** `TutorialViewModelTest.theTutorialViewModelsOnlyCollaboratorsAreTheOnboardingRepositoryAndTheApplicationScope` (test 9, S4) — reflection over the primary constructor, so a later refactor that injects a progress dependency fails a test, not just a grep. |
| **P3** | Writes **no `LevelResult`**, clears **no resume snapshot**, calls **no ladder advance** | `grep -rnE 'recordResult\|clearSnapshot\|advancePast\|saveSnapshot\|ProgressRepository\|ResumeRepository\|ChallengeProgress' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **0 hits**. Backed by the codebase fact that the only writer of `level_result` is `ProgressRepository.recordResult`, whose only caller in `main` is `ResultsViewModel.kt:501`. |
| **P4** | **No route to Results** — the only outbound edge is `popBackStack()` | `awk '/composable<Tutorial>/,/^        }/' app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt \| grep -c 'navigate('` → **0**, **and** `NavFlowTest.theTutorialDestinationsOnlyOutboundEdgeIsAPopBackToItsEntryPoint` (test 11, S4). |
| **P5** | `TutorialBoards` **never** joins `Boards.CLASSIC_SPECS` | `grep -rn 'TutorialBoards' domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` → **0 hits**, **and** `TutorialBoardsTest.noTutorialBoardAppearsInTheClassicCatalogue` (test 4, S1), **and** `HomeScreenTest.theBoardsPillDenominatorIsTheClassicCatalogueSize` asserting **5** (test 3, S6). Three layers because this is, in the game-designer's words, "the single easiest mistake to make with this ruling". |
| **P6** | The tutorial names **only** the cues the spec names (D-01i; §5's enumerated contract, `ux-spec.md:826-838`) | Allow-list grep — the shape the tech-lead anticipated ("must be empty, or contain only cues the spec names"): <br>`grep -rho 'SoundCue\.[A-Za-z]*' app/src/main/java/com/app/pegsolitaire/ui/onboarding/ \| sort -u` → must print **exactly** `SoundCue.ButtonDismiss` and `SoundCue.ButtonForward`, and nothing else. <br>**Reconciling two allow-lists, so a reviewer is not confused when two names are missing.** §13 states the allow-list as the four cues the screen *plays*: `Jump`, `PegRemove`, `ButtonForward`, `ButtonDismiss`. The grep sees only **two**, because `Jump` and `PegRemove` are named inside `JumpSoundEffect` (`audio/JumpSoundEffect.kt`, S3) and the tutorial's call site is `JumpSoundEffect(transition = …)`, which names no cue at all. §13's list is the behavioural set; this grep is its mechanical shadow. If `Jump` or `PegRemove` ever *does* appear under `ui/onboarding/`, the effect was copied instead of called — which is exactly the drift S3 exists to prevent — so the stricter two-name form is the better gate. Backed by `TutorialSoundTest` tests 12–15 (S4). |
| **P7** | **No `SoundCue` reference added to `BoardCanvas`** | `grep -c 'SoundCue\|SoundPlayer\|sound' app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt` → **0**. Gaining one would put audio on the shared board component for every screen at once. |
| **P8** | **No M3 `TopAppBar`, `BottomAppBar` or `NavigationBar`** on the three refreshed screens | `grep -rnE 'TopAppBar\|BottomAppBar\|NavigationBar' app/src/main/java/com/app/pegsolitaire/ui/{home,game,onboarding} \| grep -vE '^[^:]+:[0-9]+: *(\*\|//\|/\*)'` → **0 hits**. The comment filter is required: four `ui/game` control files mention `TopAppBar` only in KDoc today, and those KDocs must be updated by S7 anyway. `ui/settings`, `ui/classic` and `ui/results` keep theirs — they are out of scope. |
| **P9** | **No Undo control and no copy naming Undo** in the tutorial | `grep -rni 'undo' app/src/main/java/com/app/pegsolitaire/ui/onboarding/` → **0 hits**, and no `tutorial_*` string contains "Undo". The retired `tutorial_undo_nudge` named a control the player cannot see and a situation the boards make impossible. |
| **P10** | **No allowance counter, badge or ad glyph** on the dock | `grep -rniE 'allowance\|badge\|×[0-9]\|adReason' app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt` → **0 hits**, and `GameScreenChromeTest.noControlShowsAnAllowanceCountBadgeOrAdGlyph` (test 7, S7). |

**A reviewer who runs these ten does not need the developer's reasoning, and should not be given
it** (rules/studio.md standing rule 1).

---

## 11. Cold-start measurement (criterion 15e, PLAT-08 C1)

Written so someone who did not build the phase can run it.

### 11.0 Why it is a delta and not a number

The Phase-6 reference of **633 ms** is **pre-SDK** (`06-BUILD-EVIDENCE.md:310-328`): UMP, AdMob,
Play Games, leaderboards and cloud save all landed after it. Comparing Phase 15 against it charges
five phases of SDK cost to a UX refresh. The post-SDK absolute number is **PLAT-08's, and Phase 14
owns it**. Phase 15's own gate is a two-arm delta; the absolute ≤ 950 ms is checked but a failure
there **escalates rather than being paid for out of this refresh's visual budget**.

### 11.1 Procedure

**Instrument:** exactly Phase 6's. AVD **`Pixel_9a`, API 36**; **debug** variant
(`:app:installDebug`); `am start -W` TotalTime; 10 samples; the first discarded as warm-up. Do not
change instrument mid-comparison — a comparison across variants or devices means nothing
(`06-BUILD-EVIDENCE.md:325-327`). A `:macrobenchmark` module would be benchmark-grade but needs a
new module and a profileable release build; that is a Phase-14-or-later decision.

**Two arms, same AVD, same host, same sitting, back to back.** Arm A = `develop` (control),
arm B = `feature/phase-15-ux-refresh`. **Re-install between arms** so neither benefits from the
other's warm ART state.

**Step 0 — seed the player state, identically in both arms.** This is not optional: with an empty
`level_result` table the one read this phase adds is measured on nothing and proves nothing about
a real player. Seed **≥ 200 banked Challenge levels**.

```bash
APP=com.erman.pegsolitarie          # app/build.gradle.kts:279
DB=peg_solitaire.db                 # ProgressPersistenceKeys.kt:20

# Precondition — check once; if this prints nothing, use the host-side fallback below.
adb shell which sqlite3

adb shell am force-stop $APP
adb shell "run-as $APP sqlite3 databases/$DB \"
  WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i+1 FROM n WHERE i < 200)
  INSERT OR REPLACE INTO level_result (mode, levelId, stars, bestTimeMs, bestScore, completedAt)
  SELECT 'challenge', CAST(i AS TEXT), 1 + (i % 3), 60000 + i, 1000 + i, 1700000000000 + i FROM n;\""
adb shell "run-as $APP sh -c 'rm -f databases/$DB-wal databases/$DB-shm'"
adb shell "run-as $APP sqlite3 databases/$DB 'SELECT COUNT(*) FROM level_result;'"   # expect >= 200
```

*Fallback if the image has no `sqlite3`:* pull the database with
`adb exec-out run-as $APP cat databases/$DB > /tmp/seed.db`, run the same SQL against it with the
host's `sqlite3`, then write it back with
`cat /tmp/seed.db | adb shell "run-as $APP sh -c 'cat > databases/$DB'"` followed by the same
`-wal`/`-shm` removal. Verify the row count with the last command above before measuring — a
corrupted push shows as a crash on launch, not as a bad number.

Column names are the entity's field names (`LevelResultEntity.kt:27-37`); the mode literal is
`ProgressPersistenceKeys.MODE_CHALLENGE` = `"challenge"` (`:67`).

**Step 1 — measure, per arm:**

```bash
git checkout develop        # arm A;  git checkout feature/phase-15-ux-refresh for arm B
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:installDebug
# re-seed (Step 0) after every install — a fresh install clears app data

# The launch COMPONENT, asked of the device rather than hard-coded (ux-spec.md §6.5):
# `$APP/.MainActivity` would expand the shorthand against the launch package and so name
# `com.erman.pegsolitarie.MainActivity`, a class that does not exist — `applicationId` is
# `com.erman.pegsolitarie` (app/build.gradle.kts:279, "-arie") while `namespace` is
# `com.app.pegsolitaire` (:263, "-aire") and the manifest declares the one activity
# relatively (AndroidManifest.xml:33). A wrong id makes `am start -W` print an error instead
# of a TotalTime, so the phase would close with NO measurement rather than a bad one.
COMPONENT="$(adb shell cmd package resolve-activity --brief "$APP" | tail -1 | tr -d '\r')"
echo "$COMPONENT"   # expect com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity

for i in $(seq 1 10); do
  adb shell am force-stop "$APP"          # force-stop takes the applicationId alone
  sleep 2
  adb shell am start -W -n "$COMPONENT" | grep TotalTime
done
# discard sample 1 as warm-up; record min / median / max of the remaining 9
```

*(Corrected 2026-09-21 by the developer executing S5–S8: the line above previously read
`am start -W -n com.erman.pegsolitarie/.MainActivity`, which names a class that does not
exist here. Criterion 15e's `am start -W` means the resolved-component form, as
`docs/ux-spec.md` §6.5 already spells out. `$APP` is set in Step 0.)*

### 11.2 Pass condition — both parts must hold

1. **after-median ≤ 950 ms**, and
2. **(after-median − before-median) ≤ 50 ms**.

50 ms is this instrument's noise floor on this AVD, not a guess: Phase 6's nine samples spanned
603–649 ms — a 46 ms range around a 633 ms median. A delta smaller than that is not measurable
with `am start -W`, so claiming one would be dishonest; a delta larger than it is real.

- **If (2) fails** — walk §6.5's ordered degradation path and **re-measure after each step**,
  stopping at the first pass: (a) mode tile art, (b) Continue chip thumbnail, (c) hero board art
  (which reverts to Q-901 option 3, so the founder's answer is already implemented), (d) ribbon
  values — the pills stay and show "—". **Never** delete the ribbon, and **never** touch the
  Challenge tile chip, the Continue chips or the Classic catalogue: those are D-03f oracles and are
  not available to trade.
- **If (1) fails while (2) passes** — the regression is not this refresh's. **Escalate to the
  producer as PLAT-08 / Phase-14 work.** Do not walk the degradation path to pay for someone
  else's cost.

### 11.3 Where the numbers go

`.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, section `## Cold-start measurement`, **one
table per arm** in the identical field shape as `06-BUILD-EVIDENCE.md:315-323`:

| Field | Arm A (`develop`) | Arm B (`feature/phase-15-ux-refresh`) |
|---|---|---|
| Device | | |
| API level | | |
| Variant | | |
| Command | | |
| Samples | | |
| Seeded Challenge rows | | |
| TotalTime min / median / max | | |
| Raw samples | | |

Plus the same precision caveat ("not comparable across devices"), and both pass conditions
evaluated explicitly with their arithmetic shown. **The phase does not close without both tables.**

**Criterion 15f is a separate visual check at the same sitting** — on first paint, before any
progress data has loaded, Home shows the ribbon with "—" placeholders, never a spinner, never a
blank band, never a blocked screen, and the values fill in without the layout shifting. The timing
number does not cover it.

**Also device-verified at the same sitting, and claimed nowhere else** (these are readings a unit
test cannot make):

- **Criterion 15a** — at default font scale on a 360×640dp phone with **both** resumes saved, the
  Continue chips and both mode tiles are fully visible without scrolling.
- **Criterion 25 with OS animations off + criterion 7c.** `runJumpAnimation` takes a snap path when
  animations are off (`BoardCanvas.kt:298-304`), so `animating` is set and cleared inside a very
  short window. Whether `JumpSoundEffect`'s `LaunchedEffect` always wins that race is a **device
  check, not a reading** (`qa-engineer-tutorial-audio.md:167-176`). It is the one place the audio
  ruling could fail in practice, and it is cheap to check: turn animations off in Developer
  options, run the tutorial, confirm every completed jump still sounds.
- **Criterion 7a** — on a player who has a saved Challenge game and existing stars, run the tutorial
  end to end *and* skip it, and confirm the Home Stars pill, both Continue chips and the Challenge
  tile's level are **exactly as they were**.

---

## 12. File-by-file inventory

### 12.1 Strings — `app/src/main/res/values/strings.xml`

**New: 13.** *(Not 14 — `tutorial_undo_nudge` was retired before it shipped when the boards ruling
made "boxed in" impossible; §8's own text says the contract "carries one string fewer". A brief or
a reader working from the pre-amendment count will expect 14. See §13.1.)*

| # | Name | Value | Slice |
|---|------|-------|-------|
| 1 | `tutorial_step_progress` | `Step %1$d of %2$d` | S4 |
| 2 | `tutorial_step_1_caption` | `Tap a peg, then tap the empty hole two cells away.` | S4 |
| 3 | `tutorial_step_2_caption` | `Jumps work up, down, left and right — never diagonally.` | S4 |
| 4 | `tutorial_step_3_caption` | `Keep jumping. Clear the board down to one peg.` | S4 |
| 5 | `tutorial_done_caption` | `That's the whole game. Go clear a board.` | S4 |
| 6 | `settings_how_to_play_label` | `How to play` | S5 |
| 7 | `settings_how_to_play_supporting` | `Replay the tutorial` | S5 |
| 8 | `stat_value_unknown` | `—` | S6 |
| 9 | `stat_value_unknown_desc` | `not yet available` | S6 |
| 10 | `home_stat_stars_label` | `Stars` | S6 |
| 11 | `home_stat_boards_label` | `Boards` | S6 |
| 12 | `home_stat_level_label` | `Level` | S6 |
| 13 | `home_stat_boards_value` | `%1$d/%2$d` | S6 |

**Deleted: 1.** `how_to_play` (`strings.xml:62`) — its only caller is the deleted info button. S5.
**Fixed: 1.** `settings_sound_supporting` (`strings.xml:143`) — "Saved for the upcoming sound
effects" is stale since Phase 7. S5.
**Orphaned, and needing a ruling rather than a silent decision:** `onboarding_step_1/2/3`
(`strings.xml:161-163`) — §8 does not list them as deleted but `OnboardingOverlay.kt` is their only
caller. See §13.2.
**Reused unchanged and therefore not to be touched:** `onboarding_got_it`, `onboarding_skip`,
`app_name`, `navigate_back`, `elapsed_time`, `challenge_level_indicator(_desc)`, `undo_action`,
`hint_action`, `hint_thinking`, `show_moves_on`, `show_moves_off`, `restart_board`, the tile and
resume strings, `settings_action`, `catalog_stars_earned`.

### 12.2 Dimens — retired: 2

| Token | Current line | Callers **today** | Freed by |
|---|---|---|---|
| `top_bar_touch_target` (48dp) | `dimens.xml:110` | **six**: `UndoControl.kt:38`, `HintControl.kt:56`, `HintControl.kt:66`, `HintToggleControl.kt:39`, `RestartControl.kt:35`, `GameScreen.kt:509` | **S7** (all six retargeted) |
| `onboarding_illustration_size` (160dp) | `dimens.xml:51` | **one**: `OnboardingOverlay.kt:144` | **S5** (file deleted) |

The spec says these "lose their last caller". `top_bar_touch_target` has six, not one: **the token
cannot be deleted until S7 has retargeted every control.** Deleting it earlier fails the build at
resource resolution, which is a confusing place to discover a sequencing error. That is why the
retirement is S8 and not S5.

### 12.3 Dimens — new: 15, all multiples of 4

| Token | Value | Slice |
|---|---|---|
| `tutorial_board_size` | 280dp | S4 |
| `tutorial_caption_min_height` | 48dp | S4 |
| `tutorial_step_pip_size` | 12dp | S4 |
| `game_status_strip_height` | 56dp | S7 |
| `game_status_back_size` | 48dp | S7 |
| `game_timer_pill_height` | 32dp | S7 |
| `game_action_dock_height` | 72dp | S7 |
| `game_action_button_size` | 56dp | S7 |
| `game_action_glyph_size` | 28dp | S7 |
| `home_hero_min_height` | 160dp | S6 |
| `home_hero_art_size` | 120dp | S6 |
| `home_stat_pill_min_height` | 64dp | S6 |
| `home_stat_pill_glyph_size` | 20dp | S6 |
| `home_resume_thumb_size` | 40dp | S6 |
| `home_tile_art_size` | 56dp | S6 |

Each is declared even where an existing token shares its value — the file's own one-token-per-owner
convention (`dimens.xml:113-132`). **Reused unchanged:** `spacing_xs/sm/md/lg/xl`,
`content_max_width`, `mode_tile_min_height`, `resume_tile_min_height`, `settings_row_min_height`,
`settings_row_icon_size`, `top_bar_icon_size`, `elevation_card`, `elevation_pressed`,
`elevation_dialog`, `entrance_slide_distance`, `mode_glyph_size`, `blob_size`, `blob_drift_range`,
`peg_selected_ring_width`, all five `shape_corner_*`.

### 12.4 Source files

**Created (16)**

| Path | Slice |
|---|---|
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/tutorial/TutorialBoards.kt` | S1 |
| `domain/src/test/kotlin/com/app/pegsolitaire/domain/tutorial/TutorialBoardsTest.kt` | S1 |
| `app/src/main/java/com/app/pegsolitaire/audio/JumpSoundEffect.kt` | S3 |
| `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt` | S4 |
| `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialRoute.kt` | S4 |
| `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModel.kt` | S4 |
| `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialUiState.kt` | S4 |
| `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialAnim.kt` | S4 |
| `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModelTest.kt` | S4 |
| `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialScreenTest.kt` | S4 |
| `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialSoundTest.kt` | S4 |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt` | S7 |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt` | S7 |
| `app/src/main/java/com/app/pegsolitaire/ui/game/HintIcon.kt` | S7 |
| `app/src/main/java/com/app/pegsolitaire/ui/game/ShowMovesIcon.kt` | S7 |
| `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenChromeTest.kt` | S7 |
| `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md` | S8 |

**Deleted (1)** — `app/src/main/java/com/app/pegsolitaire/ui/onboarding/OnboardingOverlay.kt` (S5).

**Modified (main)** — `navigation/{Routes,AppNavHost}.kt` (S4, S5);
`data/db/{LevelResultDao,ProgressProjections}.kt`, `data/ProgressRepository.kt` (S2);
`ui/home/{HomeScreen,HomeRoute,HomeUiState,HomeViewModel,ModeGlyphs}.kt` (S2, S5, S6);
`ui/settings/SettingsScreen.kt` (S5);
`ui/game/GameScreen.kt` (S3, S7); `ui/game/{Hint,HintToggle,Undo,Restart}Control.kt` (S7);
`res/values/{strings,dimens}.xml` (S4–S7).

**Not touched, and deliberately so:** `ui/components/BoardCanvas.kt` (P7),
`ui/classic/ClassicCatalogScreen.kt` and `ui/components/BoardPreview.kt` (oracle 2 — the per-board
star pips stay exactly where the device checklist reads them), `ui/results/*`,
`domain/…/model/Boards.kt` (P5), `VERSION`, `app/schemas/`.

### 12.5 The repository test doubles — 11, not 16

The tech-lead's report says *"16 test files implement `ProgressRepository` and every one needs a
one-line override"* (`tech-lead-architecture.md:294`, F-9). **Re-counted for this plan: 16 is the
number of test files that *mention* the type; 11 declare an implementation the compiler will force
to add the override.** The difference matters because five of them exercise the *real* Room-backed
repository and a developer expecting 16 compiler errors will go looking for five that never arrive.

**Needs the one-line override (11):**

| # | File | Declaration |
|---|------|-------------|
| 1 | `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt:594` | `FakeResumeProgressRepository` |
| 2 | `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelCloudTest.kt:340` | `NoResumeProgressRepository` (object) |
| 3 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt:586` | `RecordingProgressRepository` |
| 4 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt:597` | `InMemoryProgressRepository` |
| 5 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt:751` | `TelemetryProgressRepository` |
| 6 | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt:811` | `StoredSnapshotRepository` |
| 7 | `app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt:235` | `FakeProgressRepository` |
| 8 | `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:1698` | `FakeProgressRepository` |
| 9 | `app/src/test/java/com/app/pegsolitaire/ui/results/ChallengeNextLevelTest.kt:354` | `NoBestsProgressRepository` |
| 10 | `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt:603` | `FakeProgressRepository` |
| 11 | `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt:1192` | `FakeCloudRepository` |

**Needs nothing (5)** — these construct the real Room-backed implementation, so the new member
arrives for free: `data/ProgressRepositoryTest.kt`, `data/ProgressRepositoryFailureTest.kt`,
`data/ProgressAggregateTest.kt`, `data/ProgressImportTest.kt`, `data/ResumeRepositoryTest.kt`.
Two of them gain a *new test* in S2 — that is different from an override.

**Also needs nothing:** the `mockk(relaxed = true)` defaults at
`GameViewModelAllowanceTest.kt:633` and `GameViewModelTelemetryTest.kt:581` — a relaxed mock
answers a new `Flow`-returning member without a line of code.

Reproduce the count:
```bash
grep -rn ': ProgressRepository' app/src/test | grep -vE 'mockk|: ProgressRepository[ ,)=]' 
grep -rln 'ProgressRepository' app/src/test | wc -l      # 16 — mentions
```

### 12.6 Assets, dependencies, licences

- **Two new in-house vectors**: `HintIcon` (lightbulb — the core icon set has no lightbulb and the
  extended set is banned) and `ShowMovesIcon` (a peg with two trailing destination dots). Both
  project-owned, both following `UndoIcon.kt` / `ModeGlyphs.kt` exactly.
- **No third-party asset.** No new `THIRD_PARTY.md` row and no new `docs/audio/CREDITS.md` row is
  required by this phase. *(This is a statement to verify at review, not to assume: if any slice
  adds a dependency or an asset file, a `THIRD_PARTY.md` row is mandatory and the release skill
  blocks the release without a licence — rules/ip-policy.md.)*
- **No new audio asset.** The audio ruling reuses `SoundCue.Jump` (`R.raw.sfx_jump`) and
  `SoundCue.PegRemove` (`R.raw.sfx_peg_remove`), both already shipped and both already credited as
  CC0 1.0 (`docs/audio/CREDITS.md:38-39`). `AudioCreditsTest` already fails the build if any file in
  `app/src/main/res/raw/` lacks a row, so completeness stays enforced and unchanged.
- **No new Gradle dependency**, which is what keeps `check --offline` green without an online warm.

---

## 13. Unresolved — routed to the producer, not decided here

Where the spec and the tech-lead disagreed, the tech-lead won on mechanism and the ux-designer on
screens, as instructed; every such case was already reconciled inside `docs/ux-spec.md`'s
amendment pass. These four are things **neither** of them resolved, so this plan records rather
than picks.

1. **The new-string count is 13, not 14 — and the spec is right; only downstream counts are stale.**
   Verified against the live document: §8's New-strings table has exactly **13** rows
   (`awk '/^### New strings/,/^The three new tutorial captions/' docs/ux-spec.md | grep -c '^| \`'`
   → 13). `tutorial_undo_nudge` was retired when the boards ruling made "boxed in" impossible, and
   §8 records it in prose ("carries one string fewer"). D-01i added **no** copy — it places existing
   cues only. So there is nothing for the ux-designer to fix: **build 13**, and treat any "14" in a
   brief or a summary as a pre-amendment count.

2. **`onboarding_step_1/2/3` (`strings.xml:161-163`) are orphaned by S5 and §8 does not say what
   becomes of them.** §8's "Deleted" section names only `how_to_play`; its "New strings" section
   says the three tutorial captions are *rewrites* of these three, which implies replacement but
   does not state it. **Decided default: delete all three in S5**, on the precedent §8 sets by
   retiring `tutorial_undo_nudge` for exactly this reason (a string with no referent). Cost of the
   alternative is three lines. **Route to the ux-designer for confirmation; nothing waits.**

3. **`docs/audio-spec.md` does not exist**, though rules/audio.md makes it the cue list of record
   and this phase now places a cue on a new screen (`qa-engineer-tutorial-audio.md:198-229`, checks
   C4/C5). It is the reason D-01h could narrow SOUND-01 invisibly. **This predates Phase 15 and
   must not block it**; the qa-engineer has already written the exact rows the tutorial needs. It
   is a sound-designer dispatch, or a tech-lead follow-up if none is staffed.

4. **`docs/tech-design.md` does not exist for this product**, so the four architecture rulings this
   plan builds against live only in a report — which is not where rules/stacks.md says a tech-lead
   decision belongs (`tech-lead-architecture.md:493-499`). The stack itself is recorded in
   `CLAUDE.md` and `.claude/rules/kotlin-android.md`, so nothing is unrecorded. Out of scope here;
   named so it is not lost.

5. **§13's owed-test list has three `TutorialSoundTest` names; criterion 7e needs a fourth.**
   `ux-spec.md:1328-1333` names the completed-jump test, the no-other-board-cue test, the
   star-pops-silently test and the grep guard — but criterion 7e (`ux-spec.md:1398`) requires the
   CTA and `Skip` press cues, and no test in §13's list asserts them. **Decided default: S4 carries
   a fourth, `theCompletionButtonPlaysTheForwardPressCueAndSkipPlaysTheDismissCue` (test 15).** This
   plan is a superset of §13's bill, never a subset; no ruling is needed and nothing waits.

**Nothing in this plan is a founder question, and no escalation is owed.** Q-901 (static peg-cluster
hero) and Q-902 (the split) are both answered and built to. No copyleft dependency is needed; no
dependency at all is. No slice needs a suppression beyond the `@Suppress("MagicNumber")` path-data
precedent the two existing in-house vectors already carry.

---

<threat_model>
## Trust boundaries

| Boundary | Description |
|---|---|
| tutorial → persisted progress | The tutorial must cross this boundary **exactly once**, to write `onboardingSeen`. Every other crossing — a `LevelResult`, a cleared resume snapshot, a ladder advance — is a defect that is invisible on screen and destructive in the database. |
| Home first frame → Room | One asynchronous single-row read, never blocking, never folded, never `first()`-ed on the start path. |

## Register

| ID | Category | Component | Disposition | Mitigation |
|---|---|---|---|---|
| T-15-01 | Tampering (data integrity) | Tutorial routed through `GameViewModel` | mitigate | `GameViewModel.Won` calls `clearSnapshot()` (`:963`) and `advancePast()` (`:973`); every tutorial step ends on one peg, so it would fire three times per run and destroy two of the three cloud-save device oracles, making `11-HUMAN-UAT.md` tests 10/11/13 unverifiable. Proofs: §10 P1, P2, P3, P4 — two greps and two named tests. |
| T-15-02 | Denial of service (player-facing) | Fire-and-forget `markSeen()` | mitigate | The pop cancels the tutorial's `viewModelScope`, the `DataStore.edit` may never complete, `OnboardingRepository.markSeen` swallows the failure by design (`:60-66`), `tutorialPending` stays true and Home re-pushes the tutorial — a loop the player cannot leave. Mitigation: await the write, on `@ApplicationScope`, latched; S4 tests 1–4, with test 1 asserting the **ordering** and not the call. |
| T-15-03 | Denial of service (performance) | An unbounded read on the start destination | mitigate | A Kotlin fold over `observeResults(MODE_CHALLENGE)` grows with the player's history — cheap in the lab, worst on the most engaged player. Mitigation: one SQL aggregate (S2), the independent-collector shape preserved, and the §11 measurement seeded with ≥ 200 banked levels so the arm exercises a real ladder. |
| T-15-04 | Information disclosure (false report) | A failed read rendering as `0` | mitigate | A player with forty stars told they have none, on the first screen they see. Mitigation: `.catch { emit(HomeProgress.UNKNOWN) }` plus two tests, one at the repository (S2) and one at the screen (S6), so "not yet" and "the read failed" render identically and neither renders zero. |
| T-15-05 | Repudiation | A phase closed on an unrun or unrecorded C1 measurement | mitigate | §11.3 requires **two** tables in `15-BUILD-EVIDENCE.md` with raw samples and both pass conditions evaluated; the qa-engineer checks both numbers at the gate and the phase does not close without them. |
| T-15-06 | Tampering | Duplicated jump-cue logic drifting from the game's | mitigate | S3 lifts the effect and its acceptance criteria assert exactly one `playJump` call site and exactly one `REMOVAL_LAYER_DELAY_MS` call site repo-wide. |
| T-15-SC | Tampering | package-manager installs | n/a | **Zero new dependencies** in this phase (§12.6); `check --offline` stays green throughout, which is itself the detector. |
</threat_model>

<verification>
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` exits 0 at the end of every one of S1…S8, and its wall time is recorded in each slice's summary against the 11m10s baseline.
- All ten §10 prohibition commands pass after S4 and again after S8.
- `grep -rn '@Ignore\|@Suppress' app/src/test` → 0 hits; `git log --oneline develop..HEAD -- app/src/test | wc -l` shows every test change is in a slice commit, and no test file was deleted (`git diff --diff-filter=D --name-only develop..HEAD -- '*Test.kt'` → empty).
- `git diff develop..HEAD -- VERSION app/schemas/ .planning/approvals/` → **empty**.
- `15-BUILD-EVIDENCE.md` carries two cold-start tables and the criterion-15f, 15a, 25/7c and 7a device readings.
- Every §14 criterion in `docs/ux-spec.md` maps to either a named test in this plan or a named device check in §11.3.
</verification>

<success_criteria>
- Eight commits on `feature/phase-15-ux-refresh`, each green, each a coherent unit a reviewer can read alone.
- The two risky mechanisms — the Home aggregate and the persist-before-pop exit — are proven by tests that fail when the mechanism is wrong, verified by deliberately breaking each once.
- The four §5.1 prohibitions hold and are each provable in one command or one named test.
- The two tests that had to break read as sentences about the new behaviour; neither was ignored, deleted, or edited to pass.
- `./gradlew check --offline` is green offline, with no new dependency, no suppression and no baseline.
</success_criteria>

<output>
Create `.planning/phases/15-ux-refresh/15-0<n>-SUMMARY.md` per slice as each lands, and
`.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md` in S8.
Handoff on completion: **code-auditor** (the §10 block plus the S2 "no fold" rule is the review
contract) and **qa-engineer** (criteria 3, 4, 7a, 7b, 7c, 7d, 7e, 13a, 13b, 15a, 15e, 15f, 25).
</output>
