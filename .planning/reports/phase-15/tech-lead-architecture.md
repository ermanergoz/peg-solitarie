# tech-lead report — phase-15 UX refresh (architecture rulings)

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (2397732..this commit — cut from develop at 2397732; this report is the branch's eighth `docs(15):` commit, after 7c73616)
Skills loaded: ergoz-soft:standards (role: tech-lead — rules/studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, stacks.md, testing.md, audio.md, ip-policy.md, lessons.md), android-architecture, android-gradle-logic
Ran in parallel with: phase-14 (release readiness, five `feature/phase-14-*` branches in other worktrees)
Loops: 0
Lessons applied: none

## Summary
Four rulings, all decided, none deferred. (1) The tutorial is a `@Serializable` nav
destination — it is the only option that scopes its ViewModel per visit and the only one that
makes "a tutorial win is not a level win" a fact about the nav graph. (2) R-1/R-2's
client-side fold over `observeResults` is **rejected**; the two ribbon values come from one
new `Flow`-returning SQL aggregate. (3) The §6.5 budget is achievable given (2); C1 is
measured as a two-arm delta against `develop`, not against the stale Phase-6 figure.
(4) No golden-image suite in Phase 15 — it is filed as a post-v1.1 follow-up.

## Outputs
- `.planning/reports/phase-15/tech-lead-architecture.md` — this ruling (the only file this task writes; no production code was touched)

---

## 0. Facts this ruling rests on

Everything below was established by reading the worktree at `5635d8c`/`7c73616`, not assumed.

| # | Fact | Evidence |
|---|---|---|
| F-1 | `markSeen()` is fire-and-forget on `viewModelScope`; the write itself swallows `IOException` and fails open ("not persisted" → re-teach) | `HomeViewModel.kt:232-235`; `OnboardingRepository.kt:60-66` |
| F-2 | The PGS prompt and the profile-swap dialog are gated on the **persisted** flag, not on overlay visibility, and each input is `distinctUntilChanged` to stop a re-emission flicker | `HomeViewModel.kt:119-128`, `:145-160` |
| F-3 | `HomeScreen.kt:198`'s guard is defence-in-depth against the *second* writer of `onboardingVisible` (`onShowHowToPlay`, `HomeViewModel.kt:227-229`), which D-02b retires | `HomeScreen.kt:188-204` |
| F-4 | `HomeUiState` carries no star/score/time data | `HomeUiState.kt:69-85` |
| F-5 | `observeResults(mode)` streams **every row** of that mode, mapped one-for-one into `LevelResult` | `ProgressRepository.kt:83`, `:580-585`; `LevelResultDao.kt:35-37` |
| F-6 | Challenge rows are one per solved level and the ladder is unbounded — the DAO's own KDoc rejects Kotlin folds for exactly this reason ("the Challenge ladder reaches thousands of rows, and folding them in memory would load the entire history to compute a `MAX`") | `ProgressRepository.kt:85-96`; `ResultsViewModel.kt:493` |
| F-7 | `totals()` already computes both wanted scalars in one `FROM`-less SQL round trip — but it is `suspend`, and its `totalStars` is deliberately both-modes with no `WHERE` | `LevelResultDao.kt:59-75` |
| F-8 | `classicStandings()` is the per-mode Classic read, also `suspend` | `LevelResultDao.kt:77-95` |
| F-9 | 23 files declare or implement `ProgressRepository` — 7 in `main`, 16 test doubles | `grep -rln ': ProgressRepository\b'` |
| F-10 | The precedent for a Home-side derived flow is `ClassicCatalogViewModel`: `observeResults → map → stateIn(Eagerly, initialValue = <empty>)` | `ClassicCatalogViewModel.kt:52-60` |
| F-11 | Pre-SDK cold-start baseline: `am start -W` TotalTime, AVD `Pixel_9a` API 36, **debug**, 10 samples less warm-up, min 603 / median **633** / max 649 ms; explicitly "not comparable across devices", re-measure on the same AVD and host | `06-BUILD-EVIDENCE.md:310-328` |
| F-12 | The baseline is **pre-SDK**. Phases 9–13 added UMP, AdMob, Play Games, leaderboards and cloud save since. No post-SDK number exists on any branch | `06-BUILD-EVIDENCE.md:312-313`; ROADMAP Phase 14 line |
| F-13 | No screenshot infrastructure of any kind; the only Robolectric use is JVM Compose tests. No `androidTest` source set (`app/src/{debug,main,release,test,testDebug}`) | `grep` over `gradle/libs.versions.toml`, `app/build.gradle.kts`, `build-logic/`; `ls app/src` |
| F-14 | CI runs `ubuntu-latest`; development is macOS/arm64 | `.github/workflows/ci.yml:22` |
| F-15 | `./gradlew check --offline` is **green** here and takes **11m10s**; `--offline` green is the enforced PLAT-07/D-18 invariant and `assembleRelease` needs an online warm first | this report's Checks; `06-BUILD-EVIDENCE.md:354-370` |
| F-16 | 16 Compose UI test files, 91 test files, 3 `@Preview`s in the whole app | `grep -rln 'createComposeRule\|createAndroidComposeRule' app/src/test`; `find app/src/test -name '*.kt'` |
| F-17 | Every sound cue on the game screen — including `SoundCue.Win` — is played by a private effect in **`GameScreen.kt`** (`:327`, `:349`, `:365`, `:381`, `:396`, `:411`, `:428`), never by `BoardCanvas.kt`, which contains no `SoundCue` reference at all | `grep -n 'SoundCue\.' GameScreen.kt`; `grep SoundCue components/BoardCanvas.kt` → 0 hits |
| F-18 | A `Won` status in **`GameViewModel`** does three irreversible things beyond emitting `finished`: it clears the mode's resume snapshot and calls `challengeProgress.advancePast(level)` | `GameViewModel.kt:956-975` |
| F-19 | The only writer of `level_result` is `ProgressRepository.recordResult`, whose only caller in `main` is `ResultsViewModel.kt:501` | `grep -rn 'recordResult(' app/src/main` |
| F-20 | `hiltViewModel()` resolves against `LocalViewModelStoreOwner`; inside `composable<Route>` that is the `NavBackStackEntry`, above the `NavHost` it is the `ComponentActivity` | Navigation-Compose/Hilt contract; `AppNavHost.kt:101-145` is the only `NavHost` |
| F-21 | The gameplay destination has no `BackHandler`; the only one in the codebase is `AdQuiescenceScrim.kt:61` | `grep -rn BackHandler app/src/main` |
| F-22 | An `@ApplicationScope CoroutineScope` already exists and is the established home for work that must outlive a screen | `di/DataModule.kt:88-89`; `ProgressSubmitter.onProfileSwapResolved` per `HomeViewModel.kt:313-323` |

**Worktree note the developer needs on day one:** this worktree has **no `local.properties`**, so
`./gradlew check` fails at `:app:lintReportDebug` with "SDK location not found" before running a
single test. It is git-ignored and exists only in the primary worktree. Either provision it per
worktree or export `ANDROID_HOME`; both `check` lines in Checks below show the difference.

---

## 1. D-01f — how the tutorial is hosted

### Ruling

**A `@Serializable data object Tutorial` destination in `Routes.kt`, registered as a
`composable<Tutorial>` in `AppNavHost`.** Not a full-screen composable above the `NavHost`.

Supporting rulings that come with it, and are binding:

- **R-5 is resolved:** step state lives in a `@HiltViewModel TutorialViewModel` obtained with
  `hiltViewModel()` **inside** the destination, held in a plain `MutableStateFlow`, and
  **not** in `SavedStateHandle`.
- **The exit path persists before it pops.** `markSeen()` is awaited, and only then does the
  screen navigate. Belt and braces: the write runs on the `@ApplicationScope` scope (F-22) so a
  cancelled coroutine cannot drop it.
- **Home's auto-start is a `LaunchedEffect` in `HomeRoute` on the standing "tutorial pending"
  flag**, calling a hoisted `onStartTutorial` that `AppNavHost` implements with
  `navigate(Tutorial) { launchSingleTop = true }`.
- **`BackHandler` on the tutorial destination** routes system back through the identical exit
  path as Skip (D-01d). This becomes the codebase's second `BackHandler` (F-21); that is
  deliberate and should be commented as such, the way `AdQuiescenceScrim` is.
- **`HomeScreen.kt:198`'s guard survives in shape.** Keep a standing `Boolean` on `HomeUiState`
  — I recommend renaming `onboardingVisible` → `tutorialPending`, the mirror of `!seen` —
  and keep `!state.tutorialPending` in the `ProfileSwapDialog` condition.

### Why a destination and not a composable above the NavHost

1. **ViewModel scoping is the deciding argument (F-20).** Inside `composable<Tutorial>`,
   `hiltViewModel()` binds to the `NavBackStackEntry`, which is cleared on pop — so every visit
   gets a fresh `TutorialViewModel` and the replay from Settings always starts at T1. Above the
   `NavHost`, `LocalViewModelStoreOwner` is the Activity: the tutorial's ViewModel would live
   for the whole process, and a player who skipped at T2 and later opened Settings › How to play
   would land on T2 with the previous board still installed. That is a real bug reachable by an
   ordinary player, and avoiding it in the hosted-composable design means hand-rolling the
   lifecycle the nav graph already gives for free.
2. **It makes the game-designer's three prohibitions structural (§1a below).** The tutorial
   destination's only outbound edge is `popBackStack()`. "The tutorial can never reach Results"
   then becomes a property of `AppNavHost` that a reviewer checks by reading fifteen lines,
   instead of an absence-of-call argument over the whole tutorial package.
3. **It is the house contract.** `Routes.kt` is documented as "the single source of truth the
   `NavHost`, the catalog, Results and `GameViewModel` all build against"; NAV-02 is type-safe
   routing. A sixth screen that is a screen everywhere except in the nav graph is a special case
   with no compensating benefit.
4. **Back and "return where you came from" are free.** `popBackStack()` lands on Home on first
   run and on Settings on replay (D-01d/§4.1's Exit clause) with no code of its own.

### State restoration, back stack and process death — the consequences, stated

| Event | What happens, and why it is right |
|---|---|
| **Configuration change (rotation)** | `NavBackStackEntry` survives, the `TutorialViewModel` is retained, the step and the board are unchanged. D-01e's "within a session, rotation keeps the current step" and acceptance criterion 6 hold with no `rememberSaveable` and no `SavedStateHandle` key. |
| **Back stack** | One entry deep: `Home → Tutorial` on first run, `Home → Settings → Tutorial` on replay. Popping returns exactly where the player came from. `launchSingleTop = true` makes a duplicate push impossible. |
| **Process death mid-tutorial** | `NavHost` restores the saved back stack, so the player returns to the Tutorial destination — but with a **fresh** `TutorialViewModel`, so the step resets to T1. `onboardingSeen` is still `false`, so a cold relaunch that restores to Home re-offers it from T1 too. This is exactly D-01e ("a process death mid-tutorial leaves the flag false, so the tutorial offers itself again from T1") and it is **why `SavedStateHandle` is forbidden here**: a saved step cursor would survive process death and resume at T2, contradicting the decision the ux-designer already took, at the cost of a key, a migration and a test matrix. |
| **`markSeen()` ordering** | See below — this is the one place the mechanism can go wrong. |

### The ordering bug this ruling exists to prevent

The invariant §7 names is real and the code is exactly as described (F-1, F-2, F-3). The failure
mode a nav destination introduces that the overlay did not have is a **navigation loop**, not
merely a late prompt:

1. Home composes with `tutorialPending = true` → `LaunchedEffect` pushes `Tutorial`.
2. Player finishes; the tutorial fires `markSeen()` on `viewModelScope` (F-1, fire-and-forget)
   and pops immediately.
3. The pop clears the tutorial's `NavBackStackEntry` → cancels its `viewModelScope` → the
   `DataStore.edit` may never complete, and `OnboardingRepository.markSeen` swallows the failure
   by design (`OnboardingRepository.kt:60-66`).
4. Home re-enters composition. `tutorialPending` is *still* true. The effect fires again and
   pushes `Tutorial` a second time — `launchSingleTop` does not help, because `Tutorial` is no
   longer on top.

Even when the write does land, the race is one frame wide and non-deterministic. And on the
launch after a dropped write, the PGS opt-in prompt and the profile-swap dialog are both
withheld for a whole extra launch (F-2) — which is the Phase-11 defect class the brief names.

**Therefore, binding on the developer:**

- `TutorialViewModel` exposes exactly one exit path. It **awaits** `markSeen()` and only then
  emits a one-shot exit signal; the screen navigates on that signal, never on the tap directly.
  Skip, Back and the completion CTA all route through it — one function, three callers.
- The write is issued on the `@ApplicationScope` scope (F-22), so even a pathological cancel
  cannot drop it. This is the same reasoning `ProgressSubmitter.onProfileSwapResolved` already
  records for the wipe (`HomeViewModel.kt:313-323`).
- A double exit is a no-op: latch it in the ViewModel.

**Named tests (rules/testing.md — every acceptance criterion maps to a named test):**

- `TutorialViewModelTest.theExitSignalIsEmittedOnlyAfterTheSeenFlagIsPersisted` — a
  `TestDispatcher` with a suspending fake repository proves the ordering, not just the call.
- `TutorialViewModelTest.everyExitRouteSkipBackAndCompletionPersistsTheSeenFlagExactlyOnce`.
- `HomeScreenTest.theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending` — this is the
  **retarget** of `HomeScreenTest.kt:148-159`, which must not be deleted. Find the dialog by
  `PROFILE_SWAP_DIALOG_TEST_TAG` as today and the tutorial-pending state by the state field —
  never by the string "Got it" (`HomeScreenTest.kt:367`), which is the flaw §13 already names.
- `NavFlowTest.theTutorialDestinationPopsBackToWhereItWasEnteredFrom`, driven from both Home and
  Settings.

### 1a. Folding in the game-designer's ruling: a tutorial win must not be a level win

The game-designer's report (`game-designer-tutorial-boards.md`, Open issues 1–2) lands on this
question, and it **does not change my ruling — it strengthens it.** It also raises the stakes
beyond what that report claims. `GameViewModel`'s `Won` path does not only emit `finished`: it
**clears the mode's resume snapshot and calls `challengeProgress.advancePast(level)`**
(F-18, `GameViewModel.kt:956-975`). Two of the three D-03f cloud-save device oracles — the
Continue affordance and the Challenge ladder position — are written on that path. A tutorial
routed through `GameViewModel` would not merely inflate the Stars pill; on a player who had a
saved Challenge game it would **delete their resume point and advance their ladder** three times,
once per step, and make `11-HUMAN-UAT.md` tests 10/11/13 unverifiable.

The structural answer, and it is cheap because the codebase is already shaped for it:

| Prohibition | Made structural by | Checkable in one grep |
|---|---|---|
| No `SoundCue.Win` | Every cue is played by a private effect in `GameScreen.kt` (F-17); `BoardCanvas.kt` contains no `SoundCue` at all | `grep SoundCue app/src/main/.../ui/onboarding/` → must be empty, or contain only cues the spec names |
| No route to Results, no `LevelResult`, no snapshot clear, no ladder advance | The tutorial hosts **`BoardCanvas`**, never `GameScreen`, and its state holder is **`TutorialViewModel`**, never `GameViewModel`. The only `recordResult` caller is `ResultsViewModel` (F-19), reachable only from the `Results` destination, and the Tutorial destination's only outbound edge is `popBackStack()` | `grep -n 'GameViewModel\|GameScreen' app/src/main/.../ui/onboarding/` → must be empty |
| `TutorialBoards` never becomes a Classic board | It is a new `object TutorialBoards` in `:domain` beside `Boards.kt`, absent from `Boards.CLASSIC_SPECS` | The Boards-pill denominator test in §2 below pins the count at 5 |

This is the second reason the nav destination wins: each of those three prohibitions reduces to
a statement about a file or an edge, not about the absence of a call somewhere in a package.

**Two consequences the developer must not be surprised by:**

1. **The tutorial board is silent unless someone decides otherwise.** Because every cue lives in
   `GameScreen` (F-17), a tutorial hosting `BoardCanvas` gets *no* peg-select, peg-remove,
   invalid-tap or undo cue for free. rules/audio.md says nothing plays that the spec does not
   name — so the default is silence and that is compliant, but it is a design choice nobody has
   consciously taken. Routed to the ux-designer and sound-designer in Handoff; not blocking.
2. **The completion predicate is now uniform.** The game-designer's three boards all complete on
   "the board holds one peg", so `TutorialViewModel` needs one predicate, not three. Take the
   simplification: `board.pegCount == 1`, evaluated on the tutorial's own state, with **no**
   `GameStatus` and no `minimumPegs` comparison in `:app` — because reaching for `GameStatus` is
   the first step down the path that ends at `GameViewModel`.

---

## 2. §7 R-1 and R-2 — the two new Home values

### Ruling

**R-1 and R-2's stated mechanism is rejected. The two ribbon values come from one new
`Flow`-returning SQL aggregate, not from a client-side fold over `observeResults`.**

The designer's *conclusions* all stand, and D-03d's headline claim stays true: no schema change,
no migration, no new column, no new DataStore key, no DI change, placeholder-first, one
collection for both pills, `aggregate()` still not used. Only the read mechanism changes.

### Why the fold is wrong

R-1 says "folds `ProgressRepository.observeResults(mode)` over both modes and sums
`LevelResult.stars`". Over `MODE_CLASSIC` that is at most five rows and is fine. Over
`MODE_CHALLENGE` it is **every level the player has ever solved** (F-5, F-6) — one row per
level, an unbounded ladder — loaded into a `List<LevelResult>`, mapped entity-by-entity
(`ProgressRepository.kt:580-585`), and **re-materialised on every write to the table**, because
Room invalidates the whole `Flow` on any `level_result` change. The Continue chips' collector,
which also runs in `HomeViewModel.init`, is on the same `Dispatchers.IO` and the same connection.

This is not my preference against the designer's; it is the rule this codebase already wrote for
itself. `LevelResultDao.totals`' own KDoc states it: *"Every field is produced by a `@Query`
projection rather than a Kotlin fold over rows, per the ROADMAP's Phase 12 note: the Challenge
ladder reaches thousands of rows, and folding them in memory would load the entire history to
compute a `MAX`."* (`LevelResultDao.kt:41-47`, `ProgressRepository.kt:85-96`.) Phase 12 refused
this exact fold for a once-per-solve background submitter. Doing it on the **start destination**,
on every visit to Home, is the same mistake in a worse place. `ClassicCatalogViewModel` (F-10) is
not a counter-precedent: it folds five rows, `MODE_CLASSIC` only, and off the cold-start path.

### What to build instead

**One new DAO query.** It is the `Flow` sibling of `totals()` (F-7) narrowed to the two numbers
Home needs, and it answers the thing `totals()` structurally cannot (F-7: `totalStars` has no
`WHERE`, so it cannot count Classic boards):

```
@Query("""
    SELECT
      (SELECT COALESCE(SUM(stars), 0) FROM level_result)            AS totalStars,
      (SELECT COUNT(*) FROM level_result
         WHERE mode = :classicMode AND stars >= :minStars)          AS classicBoardsCleared
""")
fun observeHomeProgress(classicMode: String, minStars: Int): Flow<HomeProgressTotals>
```

- Same `FROM`-less correlated-subquery shape as `totals()`, so it always returns exactly one row
  and the type is non-null; same `COALESCE` on the only nullable aggregate; same
  column-name-constant bindings (`ProgressPersistenceKeys.COLUMN_*`, `TABLE_LEVEL_RESULT`) that
  every other query in that file uses.
- `totalStars` keeps the deliberate both-modes semantics the pill's label ("Stars", a total)
  means — the same number D-03 star tiers count.
- `classicBoardsCleared` is mode-filtered and `stars >= :minStars` bound to
  `ProgressConstants.ANY_STARS`, which is already the "cleared" predicate in
  `ProgressRepository.kt:573-577`. **No new magic number** (QUAL-03).
- `HomeProgressTotals` is a two-field projection in `data/db/ProgressProjections.kt`, beside
  `LevelResultTotals` and `ClassicStanding`.

**One new repository member**, `fun observeHomeProgress(): Flow<HomeProgress>`, on the
`ProgressRepository` interface, with the same `.catch { emit(HomeProgress.UNKNOWN) }`
fail-degraded contract `observeResults` has (T-05-10, `ProgressRepository.kt:583-585`) — a failed
read must render as "—", never as `0` and never as a crash. `HomeProgress` is a UI-layer read
model (`data class HomeProgress(val totalStars: Int, val classicBoardsCleared: Int)`) so no Room
type reaches a ViewModel, exactly as `LevelResult` does today.

**One collector in `HomeViewModel`,** in the existing independent-collector shape:

```
viewModelScope.launch {
    progressRepository.observeHomeProgress().collect { p ->
        _uiState.update { it.copy(totalStars = p.totalStars, classicBoardsCleared = p.classicBoardsCleared) }
    }
}
```

`HomeUiState` gains `totalStars: Int? = null` and `classicBoardsCleared: Int? = null`, null until
the first emission (D-03c's "—"). The denominator stays `Boards.CLASSIC_SPECS.size`/the
`ClassicBoard` enum — never a stored count.

**Forbidden, and worth writing into the plan because each is a plausible wrong turn:**

- No `runBlocking`, no `first()` on the start path, no Home-wide loading state (§6.5 already
  says this; I am restating it as an architecture rule).
- **Do not restructure `HomeViewModel.init` into a single `combine(...)` over all inputs.**
  `combine` emits nothing until *every* input has emitted once, which would make the whole Home
  state wait on a database round trip. The five independent collectors over one
  `MutableStateFlow(HomeUiState())` are what make the placeholder rule structurally true. Keep
  the shape.
- Do not add a second collection for the second pill. One query, two fields.
- Do not use `aggregate()` (suspend, one-shot, goes stale) — the designer is right and the
  reason is recorded at `ProgressRepository.kt:85-96`.

### What it costs, honestly

| Cost | Size |
|---|---|
| `LevelResultDao` | +1 `@Query` |
| `ProgressProjections.kt` | +1 two-field data class |
| `ProgressRepository` interface + Room impl | +1 member, +1 read model |
| **Test doubles** | **16 test files implement `ProgressRepository` (F-9)** and every one needs a one-line `override fun observeHomeProgress() = flowOf(HomeProgress.UNKNOWN)`. This is the whole real cost and it is mechanical; the compiler names all 16. |
| `HomeViewModel` / `HomeUiState` | +1 collector, +2 nullable fields |
| Migration | **None.** No table, column or index changes — a new `SELECT` over existing columns is not a schema change, so `docs/ux-spec.md` §7's opening sentence ("Nothing here needs a schema change, a migration, a new DataStore key or a DI change") remains literally true. The exported Room schema and `roomSchemaVersion` are untouched. |

Compared with the fold, which costs zero new members and an unbounded read on the start
destination forever, that is the right trade and it is not close.

**Named tests:**

- `ProgressRepositoryTest.theHomeProgressFlowSumsStarsAcrossBothModesAndCountsOnlyClearedClassicBoards`
  — against the real in-memory Room DB, with Classic and Challenge rows present, so the `WHERE`
  and the missing `WHERE` are both pinned.
- `ProgressRepositoryFailureTest.theHomeProgressFlowDegradesToUnknownWhenTheDatabaseReadThrows`
  — sibling of `observeResultsDegradesToAnEmptyListWhenTheDatabaseReadThrows`
  (`ProgressRepositoryFailureTest.kt:63`), asserting "—" and not `0`.
- `HomeViewModelTest.theRibbonValuesAreNullUntilTheFirstProgressEmission` — the placeholder rule
  as an assertion, with Turbine.
- `HomeScreenTest.aRibbonPillWithNoValueYetShowsTheUnknownPlaceholder` — criterion 13.
- A denominator guard so the game-designer's warning cannot come true silently:
  `HomeScreenTest.theBoardsPillDenominatorIsTheClassicCatalogueSize`, asserting `5`.

---

## 3. §6.5 — the cold-start budget and how C1 is measured

### Ruling: the budget is achievable, **given §2**

With the SQL aggregate, the added first-frame work is what §6.5's table claims: up to four
`BoardCanvas` draws of static vector geometry plus three `Surface`s, and one asynchronous
single-row query that starts after composition and paints into a placeholder. `am start -W`
TotalTime ends at the first frame; a Room `Flow` collected in `viewModelScope` is not on it.
The one thing that *would* be on it — the whole state waiting on the query — is excluded
structurally by keeping the independent-collector shape (§2). So the design budget is sound.

**With the fold as R-1 writes it, I would not sign this off.** The named specific risk: the read
would start in `HomeViewModel.init`, in the same window the activity draws its first frame,
competing on `Dispatchers.IO` and on the single Room connection with the two `loadSnapshot()`
reads the Continue chips already perform there (`HomeViewModel.kt:176-205`) — and its size grows
with the player's Challenge history, so the regression is invisible on a fresh install and
appears only on the players who have played most. That is the worst possible shape for a
measured gate: it passes in the lab and fails in the field.

### The measurement risk nobody has priced yet

**The 633 ms figure is pre-SDK and is not a usable reference for Phase 15 (F-11, F-12).** It was
taken before UMP, AdMob, Play Games, leaderboards and cloud save existed. The 317 ms of apparent
headroom under C1 belongs to Phases 9–13, not to Phase 15, and nobody has measured how much of it
is left — that measurement is PLAT-08, which is Phase 14's, and Phase 14 is in flight. A Phase 15
developer who measures once and compares to 633 ms would be attributing five phases of SDK cost
to a UX refresh.

**So Phase 15's own gate is a delta, and PLAT-08's absolute ≤ 950 ms stays Phase 14's.**

### How the developer measures C1 — exact procedure

Replicate the Phase-6 instrument exactly (F-11); do not change instrument mid-comparison. A
`:macrobenchmark` module would be benchmark-grade but needs a new module and a profileable
release build, which is a Phase-14-or-later decision, not a Phase-15 one.

**Two arms, same emulator, same host, same session, back to back.**

```bash
# Arm A (control) — develop, and Arm B (after) — feature/phase-15-ux-refresh.
# Run both from the same worktree/host in one sitting; do not compare across days.
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:installDebug      # AVD Pixel_9a, API 36
for i in $(seq 1 10); do
  adb shell am force-stop com.app.pegsolitaire
  sleep 2
  adb shell am start -W -n com.app.pegsolitaire/.MainActivity | grep TotalTime
done
# Discard sample 1 as warm-up; record min / median / max of the remaining 9.
```

- Device **AVD `Pixel_9a`, API 36**; variant **debug** (`:app:installDebug`) — both as Phase 6,
  because a comparison across variants or devices means nothing (`06-BUILD-EVIDENCE.md:325-327`).
- Measure **first launch after install for arm A too**, i.e. re-install between arms, so neither
  arm benefits from the other's warm ART state.
- The player state must be identical across arms and must be **non-trivial**: seed a Challenge
  ladder of at least ~200 banked levels before measuring, or the one read this phase adds is
  measured on an empty table and proves nothing about a real player. (With the §2 aggregate the
  SQL is O(rows) in SQLite, not in Kotlin heap — that is precisely what the seeded arm proves.)

**Pass condition for Phase 15**, both parts:

1. after-median ≤ **950 ms** (PLAT-08 C1, acceptance criterion 15e), and
2. (after-median − before-median) ≤ **50 ms**.

50 ms is the noise floor of this instrument on this AVD, not a guess: Phase 6's nine samples
spanned 603–649 ms, a 46 ms range around a 633 ms median (F-11). A delta smaller than that is
not measurable with `am start -W`, so claiming one would be dishonest; a delta larger than it is
real. If (2) fails, walk §6.5's ordered degradation path (tile art → chip thumbnail → hero art →
ribbon values) and re-measure after each step, stopping at the first pass. If (1) fails while
(2) passes, the regression is not Phase 15's — it is PLAT-08's, and it escalates to the producer
for Phase 14 rather than being paid for out of the UX refresh's visual budget.

**Where the number goes:** `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, section
`## Cold-start measurement`, using the *identical* table fields as
`06-BUILD-EVIDENCE.md:315-323` (Device / API level / Variant / Command / Samples / TotalTime
min·median·max / Raw samples) — **one such table per arm**, plus the seeded-ladder row and the
same precision caveat. The phase does not close without it, and the qa-engineer checks the number
against both conditions at the gate. Criterion 15f (placeholders, no spinner, no reflow) is a
separate visual check at the same sitting, not something the timing number covers.

---

## 4. §13 — the screenshot-test gap

### Ruling

**No golden-image suite in Phase 15. Yes as its own follow-up, scheduled after v1.1 ships — not
before.** This is my call and I am making it; nothing here goes to the founder.

### Weighed honestly — the case for, first

The designer is right that a five-screen visual refresh is the archetypal golden-image use case,
that CLAUDE.md names Roborazzi the primary screenshot tool, and that the gap (F-13) is a real
divergence from the intended stack. If the suite existed today I would insist it be kept green.

### Why it still does not land in Phase 15

1. **A suite recorded during Phase 15 has zero regression value for Phase 15, and reviewing it
   would be self-approval.** Goldens catch *unintended* visual change. Every pixel on all five
   screens is intentionally changing, so every pre-existing golden is invalidated by the refresh
   and every new golden is recorded *from the diff under review* — an image of the
   implementation, not of the contract. rules/studio.md standing rule 1 is precisely about not
   letting the author's output become its own oracle. The suite only starts paying in Phase 16.
2. **The counter-argument in the brief points the other way.** Two existing tests break by design
   (`ButtonSoundTest.kt:105-123`, `HomeScreenTest.kt:148-159`) — both because they assert on
   *copy and content descriptions* of surfaces being redesigned, and both are cheaply retargeted
   to a named assertion a reviewer can read. A golden suite would have broken on **five** screens
   instead of two, and each break would be a binary diff the reviewer must eyeball. It would have
   increased this phase's review burden while catching nothing the semantic tests miss.
3. **The real cost is host parity, and it deserves its own plan.** CI is `ubuntu-latest` and
   development is macOS/arm64 (F-14). Roborazzi goldens differ across those on font rasterisation
   and the software Skia path. The project would immediately face a genuine decision — CI-only
   recording (a red-then-download-then-commit loop on every UI change), per-platform baselines,
   or a pixel-difference threshold that weakens the assertion. Taking that decision in the corner
   of a UX phase is how a flaky gate gets born.
4. **It collides with the offline contract.** PLAT-07/D-18 requires `./gradlew check --offline`
   green, and `06-BUILD-EVIDENCE.md:354-370` records that a new dependency needs an online warm
   on every machine and CI runner before offline `check` works again. Landing that inside the
   phase whose `check` is already red from two intentionally-broken tests conflates two failure
   sources during exactly the window a reviewer needs them separable.
5. **Budget, against what the phase already owes.** `check` is 11m10s today (F-15) and Phase 15
   already carries: a Compose UI test for a brand-new interactive screen, retargeting two broken
   tests, six-plus new named tests from §1 and §2 of this report, **and** — new since the
   game-designer's ruling — `TutorialBoardsTest.everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg`
   over `TutorialBoards.ALL` plus two axis guards in `:domain`. That is a full test bill before a
   single golden image.
6. **Opportunity cost against R-2.** STATUS.md records the v1.1 cut as blocked on R-2 — no fuzz
   harness, no `koverVerify` — and names it the Core-Value gate. Screenshot tests protect how the
   game looks; the fuzz harness protects "every generated puzzle is solvable", which CLAUDE.md
   names the product's core value. On the same engineering budget, R-2 wins.
7. **AGP's built-in `screenshotTest` is not a cheap substitute here.** The app has **three**
   `@Preview`s in total (F-16); previews would have to be authored first, which is the same work
   plus a second tool.

### What Phase 15 owes instead — binding, not advisory

- **Every new or replaced surface carries a `testTag` constant**: `TUTORIAL_SCREEN_TEST_TAG`,
  `HOME_PROGRESS_RIBBON_TEST_TAG`, `GAME_STATUS_STRIP_TEST_TAG`, `GAME_ACTION_DOCK_TEST_TAG`,
  in the same idiom as `PROFILE_SWAP_DIALOG_TEST_TAG` / `PLAY_GAMES_PROMPT_DIALOG_TEST_TAG`.
  This closes §13's second named gap ("the overlay carries no `testTag`") and is what stops the
  next test from finding a screen by its button copy.
- **The two breaking tests are retargeted, never deleted, never `@Ignore`d.** rules/kotlin-android.md
  forbids `@Ignore` and disabled tests without code-auditor approval; a UX refresh is not a
  reason to seek one. `ButtonSoundTest.kt:105-123` keeps its gear half and moves its
  how-to-play half to the Settings row (D-02a); `HomeScreenTest.kt:148-159` becomes the §1 test.
- **One Compose UI test per changed screen's main flow** (rules/testing.md), the tutorial
  included — §13 correctly notes it has none today.
- **The `:domain` tutorial-board test** the game-designer specified, in
  `domain/src/test/kotlin/com/app/pegsolitaire/domain/tutorial/`.

### The follow-up, specified so it is schedulable

Producer to file a roadmap item — **"Golden-image screenshot suite (Roborazzi)"**, owner
qa-engineer with tech-lead sign-off, **after** the v1.1 release and **after** R-2 — whose first
task is the host-parity decision (CI-only recording vs per-platform baselines vs tolerance), and
whose second is the offline-warm procedure. Recording it against the *post*-refresh screens is
also strictly better: it captures the design the studio intends to keep.

---

## Checks

- `cd /Users/ermanergoz/Desktop/PegSolitarie-p15 && ./gradlew check --offline` — **fail** (`:app:lintReportDebug`: "SDK location not found"; this worktree has no `local.properties`, which is git-ignored and exists only in the primary worktree — a day-one blocker for the developer, not a code defect)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **pass** (BUILD SUCCESSFUL in 11m10s, 98 actionable tasks; `:app` + `:domain` ktlint, detekt, lint, the `dpSpLiteralCheck`/`adIdLiteralCheck`/`gamesIdLiteralCheck`/`audioFocusLiteralCheck` gates and all unit tests green; the branch is unmodified, so this is the pre-refresh baseline)
- `grep -rn 'roborazzi\|paparazzi\|screenshotTest' gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts build-logic/` — **0 hits**; `ls app/src` → `debug main release test testDebug` (no `androidTest`) — confirms F-13
- `grep -rln 'createComposeRule\|createAndroidComposeRule' app/src/test | wc -l` — 16; `find app/src/test -name '*.kt' | wc -l` — 91; `grep -rn '@Preview' app/src/main | wc -l` — 3 (F-16)
- `grep -rln ': ProgressRepository\b\|ProgressRepository {' app/src/main app/src/test | wc -l` — 23 (7 main + 16 test doubles) (F-9)
- `grep -rn 'recordResult(' app/src/main` — one caller, `ResultsViewModel.kt:501` (F-19)
- `grep -n 'SoundCue\.' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` — 10 hits incl. `:428` Win; `grep SoundCue app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt` — **0 hits** (F-17)
- `grep -rn 'BackHandler' app/src/main` — one hit, `AdQuiescenceScrim.kt:61` (F-21)
- `grep -rn 'ApplicationScope' app/src/main` — `di/DataModule.kt:88-89` plus 8 consumers (F-22)
- Read, for the facts cited by file:line: `docs/ux-spec.md` (§3 D-01f/D-02b/D-03c–g, §4.1, §4.2, §4.2.1, §6.5, §7 R-1..R-5, §13, §14, §15); `HomeViewModel.kt:96-235`, `:313-323`; `HomeScreen.kt:121-205`, `:285-319`; `HomeUiState.kt:69-85`; `HomeRoute.kt:31-58`; `AppNavHost.kt:66-209`; `Routes.kt:22-68`; `OnboardingRepository.kt:15-66`; `ProgressRepository.kt:60-135`, `:280-291`, `:570-585`; `LevelResultDao.kt:27-120`; `ProgressProjections.kt:41-67`; `ClassicCatalogViewModel.kt:40-80`; `GameViewModel.kt:950-975`; `GameScreen.kt:168-190`, `:405-432`; `HomeScreenTest.kt:95-175`; `ButtonSoundTest.kt:100-123`; `06-BUILD-EVIDENCE.md:310-370`; `.github/workflows/ci.yml:22`; `.planning/reports/phase-15/{ux-designer,release-manager-sequencing,game-designer-tutorial-boards}.md`
- **Not run:** any build of the phase-15 changes (none exist yet); any device or emulator measurement — the C1 procedure in §3 is specified for the developer, and running a device is the qa-engineer's authority, not mine

## Open issues / risks

1. **`docs/tech-design.md` does not exist for this product.** These rulings therefore live only
   in this report, which is not where rules/stacks.md says a tech-lead decision belongs. The
   stack choice itself is recorded in `CLAUDE.md` and `.claude/rules/kotlin-android.md`, so
   nothing is unrecorded, but the document is missing. **Recommendation, decided default:** the
   producer schedules a `docs(tech-design)` task for me after Phase 15's plans are written, so
   the design captures the shipped architecture rather than a forecast. Out of scope for this
   task, which was asked as four questions.
2. **Phase 14 owns the post-SDK PLAT-08 number and Phase 15 depends on it (F-12).** If Phase 14
   measures and the post-SDK median is already close to 950 ms, Phase 15's §6.5 degradation path
   may have to be walked even with a clean delta. **Recommendation, decided default:** Phase 15
   proceeds on the delta gate in §3 and does not wait; the producer routes Phase 14's number to
   the phase-15 developer the moment it exists. Nothing blocks.
3. **The tutorial board is silent by default (F-17).** No peg-select, peg-remove, invalid-tap or
   undo cue reaches `BoardCanvas`; all of them live in `GameScreen`. rules/audio.md makes silence
   compliant (nothing plays that the spec does not name), but it is an unmade decision rather
   than a taken one. **Recommendation, decided default: ship it silent**, since D-01c's "a wrong
   tap is never a failure" argues actively against an `InvalidTap` cue in a tutorial. Routed to
   the ux-designer and sound-designer; not blocking and not a founder question.
4. **Q-901 and Q-902 are the ux-designer's founder questions and I do not reopen them.** I note
   only that Q-901's option 2 (a drifting mini board) is the one option that puts a running
   animation on the cold-start path, and that §6.5's step 3 already reduces to option 3, so the
   founder's answer is compatible with either measurement outcome.
5. **No copyleft dependency, no new dependency at all, and no template gap.** Nothing in these
   four rulings adds a library; the stack template `templates/android-compose-game` exists.
   Nothing escalates to FOUNDER.md from this task.

### Proposed diffs to `docs/ux-spec.md` — I did not edit the file

The ux-designer owns it; the producer routes these.

- **§7 R-1 and R-2 — replace the mechanism, keep every conclusion.** "folds
  `ProgressRepository.observeResults(mode)` over both modes and sums `LevelResult.stars`" →
  "reads `ProgressRepository.observeHomeProgress()`, a `Flow` over one SQL aggregate
  (`LevelResultDao.observeHomeProgress`), which computes both values in a single row"; and in
  R-2, "Same fold over `MODE_CLASSIC`, counting rows with `stars >= 1`" → "the same single query,
  whose `classicBoardsCleared` field is mode-filtered and `stars >= ProgressConstants.ANY_STARS`".
  **The §7 opening sentence stands unchanged** — a new `SELECT` over existing columns is not a
  schema change and needs no migration. Reason: a Kotlin fold over `MODE_CHALLENGE` loads the
  unbounded ladder into memory on the start destination, which `LevelResultDao.kt:41-47` already
  rules out for a background submitter. Tech-lead decision; reasoning in §2 above.
- **§7 R-5 — mark resolved:** "a Hilt `TutorialViewModel` scoped to the Tutorial nav destination;
  step state in a plain `MutableStateFlow`, **not** `SavedStateHandle` (a saved cursor would
  survive process death and contradict D-01e). Tech-lead ruling, 2026-09-21."
- **§7, after the "Invariant the hosting mechanism must preserve" paragraph — add:** "Resolved
  (tech-lead, 2026-09-21): the tutorial is a `@Serializable` nav destination; `markSeen()` is
  awaited before the pop, on the application scope; the guard at `HomeScreen.kt:198` survives as
  `!state.tutorialPending`."
- **§6.5, "Handoff" — add the measurement contract:** two arms (`develop` vs the phase branch),
  same AVD/host/session, seeded Challenge ladder, and the pass condition "after-median ≤ 950 ms
  **and** delta ≤ 50 ms"; recorded in `15-BUILD-EVIDENCE.md` in the `06-BUILD-EVIDENCE.md:315-323`
  table shape. Reason: the 633 ms reference is pre-SDK and would charge Phases 9–13's cost to
  this refresh.
- **§13, the "Risk worth naming" paragraph — close it:** "Ruled (tech-lead, 2026-09-21): no
  golden-image suite in Phase 15; filed as a post-v1.1 follow-up. Phase 15 instead owes a
  `testTag` on every new surface, retargeted (never deleted) versions of the two breaking tests,
  and one Compose UI test per changed screen."
- **§5 / §4.1 — add the tutorial's negative contract** (from §1a, and beyond what
  `game-designer-tutorial-boards.md` asks for): the tutorial hosts `BoardCanvas` and never
  `GameScreen`; its state holder is `TutorialViewModel` and never `GameViewModel`; it writes no
  `LevelResult`, clears no resume snapshot and never calls `challengeProgress.advancePast` —
  because `GameViewModel.kt:956-975` does the latter two on any `Won`, and they would destroy two
  of the three D-03f device oracles.

## Handoff

Next: **producer** — (a) route the six `docs/ux-spec.md` diffs above to the ux-designer, together
with the game-designer's own §3 diff; (b) file the roadmap follow-up "Golden-image screenshot
suite (Roborazzi)" for after v1.1 and after R-2; (c) route Open issue 3 (tutorial audio) to the
ux-designer and sound-designer as a non-blocking decision; (d) schedule `docs/tech-design.md`
(Open issue 1) after the phase-15 plans exist; (e) hand this report to the phase-15 planner — §1
and §2 each specify the files to touch and the named tests, so they are plannable as written.

Then: **android-game-dev** — provision `local.properties` (or export `ANDROID_HOME`) in this
worktree before anything else; build in this order, because §2's data work is the only item on
the measured path: the `Tutorial` destination + `TutorialViewModel` + the persist-before-pop exit
(§1), then `observeHomeProgress` end to end (§2), then the Home recomposition, then the gameplay
chrome. Measure C1 per §3 before the last plan closes.

Then: **qa-engineer** — the gate reads two numbers from `15-BUILD-EVIDENCE.md` (absolute and
delta), plus criteria 15f, 3 and 4 on a device; and confirms that no tutorial step banked a star,
cleared a resume snapshot or advanced the Challenge ladder (§1a) by checking Home's ribbon,
Continue chips and Challenge tile before and after a full tutorial run on a player who has a
saved Challenge game.

Then: **code-auditor** — §1a's three prohibitions are each one grep over
`app/src/main/.../ui/onboarding/`; §2's rule is "no Kotlin fold over `observeResults(MODE_CHALLENGE)`
anywhere"; and I am the wrong role to review the diff that implements these rulings.
