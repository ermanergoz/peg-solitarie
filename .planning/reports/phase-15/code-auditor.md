# code-auditor report — phase-15 (UX refresh)
Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (4410c74..370e733; last source commit 57b2ad5, everything after it is `.planning/` + `docs/` only)
Skills loaded: ergoz-soft:standards (role: code-auditor), compose-performance-audit, security-reviewer
Ran in parallel with: phase-14 (worktrees `-r1`, `-r5`, primary), ux-designer amending `docs/ux-spec.md` concurrently
Loops: 0
Lessons applied: none

---

## DEVIATION FROM STUDIO STAFFING — read this first

**This audit ran on Opus, the same model as the author of the code it reviews.**

`agents/code-auditor.md` pins this role to Fable for one reason: rule 1 of the studio's model
policy makes a diff reviewable only on a model above its author's, because LLM self-review
research shows a reviewer favours output it recognises as its own. `android-game-dev` is
`model: opus`. Fable's allowance is exhausted (HTTP 429). The founder decided — against the
producer's stated recommendation, and reaffirmed — that the review proceed on Opus
(`.planning/questions/FOUNDER.md` Q-003, filed on this branch as `9650f3d`).

The agent definition says a review that cannot get Fable **does not happen on another model**:
the step fails and the report names Fable as the reason. That did not happen here. So this
report is a deviation from the studio's own staffing rule, and its findings carry less
independence than a Fable review's would. **It should not be treated as satisfying rule 1.**

What I did to compensate, deliberately, since model independence is gone:

- I took **no** recorded result on trust. Every claim in `## Checks` below is a command I ran.
- I read **none** of `.planning/reports/phase-15/android-game-dev-*.md`, and none of
  `.planning/log/`. I judged the code against `docs/ux-spec.md`, `15-PLAN.md` §10 and the rules.
- Where a guard mattered I **broke the implementation and confirmed the test fails** — five
  mutation probes (A–E below), all reverted, restoration verified by SHA-256.
- I ran the `:domain:depthSweep` failure to ground with a **control run on `develop`** rather
  than accepting either "it is the branch" or "it is the host".

---

## Summary

The code is strong and I found **no merge-blocking defect in it**. All ten §10 prohibition
checks hold; the four §5.1 prohibitions hold in fact, not just by name; the two retargeted
tests were narrowed and paired with positive controls rather than weakened, and I proved four
guards live by breaking them. The one `@Suppress` is **approved**; the two new vectors
**satisfy** §S7 on the reading that governs.

**VERDICT: FAIL** — not for the code, for two gate conditions the diff cannot merge without:
`./gradlew check` does not pass in this worktree (three runs; **not attributable to this
diff** — proof below), and criterion 15e's cold-start gate is **NOT RUN**, which
`ux-spec.md:1868-1869` makes a condition of the phase closing. Criterion 15 is additionally
unmet on the developer's own device reading. None of the three is fixed by editing this diff.

## Outputs

- `.planning/reports/phase-15/code-auditor.md` — this report

---

## Decision 1 — the `ProgressRepository` suppression: **APPROVED**

**approved by code-auditor** — `@Suppress("TooManyFunctions")` at
`app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:51` (interface
`ProgressRepository`).

Established by execution, not by reading the justification:

- **It is load-bearing and hides exactly one finding.** Probe E removed the annotation and ran
  detekt uncached. Output: `ProgressRepository.kt:51:11 Interface 'ProgressRepository' with
  '12' functions detected. The maximum allowed functions per interface is set to '11'
  [TooManyFunctions]` → `Analysis failed with 1 issues.` One issue, the one documented, nothing
  else masked. A suppression that hides a second, undocumented finding is the failure mode here
  and it does not occur.
- **The net suppression count is zero.** `app/src/main` + `domain/src/main` carry **13**
  suppression annotations on `develop` and **13** on HEAD. The branch adds this one and deletes
  `@file:Suppress("TooManyFunctions")` from `GameScreen.kt:7`.
- **The scope improves.** The deleted one was **file-level** — a blanket over every declaration
  in a 500-line screen file. The added one is **declaration-level** on one interface. Trading a
  file suppression for a member suppression is a strictly better position than the one develop
  is in.
- **The alternatives are worse.** Raising detekt's threshold to 12 weakens the gate for every
  file in both modules to buy one declaration — the wrong trade. Splitting the interface is
  feasible (the four resume members would make a `ResumeRepository` of 4 and leave 8) but pays
  a Hilt binding, every consumer signature and every test double to satisfy a counter, inside a
  phase about UX.

One correction to the justification text, which does not change the verdict:
`ProgressRepository.kt:47-49` argues that "splitting this interface … would mean a second
declared owner of the same tables". That is true of splitting the **implementation** and not of
splitting the **interface** — two interfaces bound to one `DefaultProgressRepository` create no
second writer. The honest grounds are the three above; the KDoc overstates its case. Advisory.

## Decision 2 — the two new vectors satisfy §S7: **ACCEPTED**

§S7's acceptance criterion (`15-PLAN.md:1055`, plan line ~106 of the S7 block) demands both
"returns 1 each, each `@Suppress("MagicNumber")` on path data" **and** "matching `UndoIcon.kt`
verbatim in shape". The two halves contradict each other. **The "matching `UndoIcon.kt`
verbatim in shape" half governs**, and what shipped satisfies it:

- `UndoIcon.kt` — the file the criterion names as the referent — carries **no suppression**, on
  `develop` and on HEAD (`grep -c '@Suppress' UndoIcon.kt` → 0). A criterion cannot be read to
  require a property its own named exemplar lacks.
- The reason is structural, not accidental. `UndoIcon.kt:43`, `HintIcon.kt:55` and
  `ShowMovesIcon.kt:56` put path data in a `private const val` **string**; `MagicNumber` cannot
  fire on characters inside a string literal, and `config/detekt/detekt.yml:31-32`
  (`ignoreConstantDeclaration: true`) exempts the two `const val … = 24f` declarations.
  `ModeGlyphs.kt` needs its two suppressions because it authors paths as **numeric builder
  calls** (`moveTo(3f, 3f)`, `ModeGlyphs.kt:49-50`) — a different authoring style, which is
  where the plan's "one `@Suppress` per vector" came from.
- Verified by execution, not inference: `:app:detekt` re-run with `--no-build-cache
  --rerun-tasks` is **BUILD SUCCESSFUL** with the two new vectors carrying no suppression.
- Adding one anyway would be actively wrong: a `@Suppress` that suppresses nothing is a
  standing licence that would silently absorb a *future* inlined numeric path — the exact drift
  the rule exists to catch. A plan can require a shape; it cannot require a suppression, because
  suppressions are mine to approve and I approve only ones that are needed.

**No suppression is required or wanted on `HintIcon.kt` or `ShowMovesIcon.kt`.** The plan's
§S7 wording should be corrected to name the `UndoIcon.kt` string-constant shape alone.

---

## Checks

My own `check` run, as the task requires, plus every §10 command. Everything below I executed.

- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew clean check --offline` — **fail** (BUILD FAILED in 11m08s; every task green except `:domain:depthSweep FAILED`, which printed its complete five-board report first — see Blocker B1)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **fail** (BUILD FAILED in 1s; `:domain:depthSweep` threw before any test ran, reading a 0-byte `results-generic.bin` left by an earlier run)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :domain:depthSweep --offline --stacktrace` — **fail** (BUILD FAILED in 1s; `java.io.EOFException` → `KryoException: Buffer underflow` at `SerializableTestResultStore.hasResults`)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :domain:depthSweep --offline` **in a detached `develop` worktree** — **pass** (BUILD SUCCESSFUL in 11m18s; the control run, see B1)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest :domain:test --offline --no-build-cache --rerun-tasks` — **pass** (BUILD SUCCESSFUL in 2m55s; every unit test in both modules, cache disabled, executed by me)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:detekt :domain:detekt :app:ktlintCheck :domain:ktlintCheck :app:dpSpLiteralCheck --offline --no-build-cache --rerun-tasks` — **pass** (BUILD SUCCESSFUL in 37s; zero findings)

### §10's ten prohibition checks — all pass

`$ONB` = `app/src/main/java/com/app/pegsolitaire/ui/onboarding/`.

- **P1** `grep -rn 'GameScreen' $ONB` — **pass** (0 hits) **and** `grep -rn 'BoardCanvas(' $ONB` — **pass** (1 hit, `TutorialScreen.kt:228`). Both halves.
- **P2** `grep -rn 'GameViewModel' $ONB` — **3 hits, all KDoc prose** (`TutorialViewModel.kt:122,123,126`, each a ` * ` line citing the gameplay tap rule). **Comment-filtered form used**, as the task directs: piping through §10's P8 filter `grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'` → **0 hits, pass**. Named test half also passes (see below).
- **P3** `grep -rnE 'recordResult|clearSnapshot|advancePast|saveSnapshot|ProgressRepository|ResumeRepository|ChallengeProgress' $ONB` — **pass** (0 hits). Note: §10 writes this with `\|` inside `-E`, where `\|` is a literal pipe and the command is vacuous; I ran **both** the literal form (0) and the corrected ERE form (0).
- **P4** `awk '/composable<Tutorial>/,/^        }/' AppNavHost.kt \| grep -c 'navigate('` — **pass** (0). The extracted block is the whole `composable<Tutorial>` body and its one statement is `tutorialContent { navController.popBackStack() }`.
- **P5** `grep -rn 'TutorialBoards' domain/.../model/Boards.kt` — **pass** (0 hits).
- **P6** `grep -rho 'SoundCue\.[A-Za-z]*' $ONB \| sort -u` — **pass** (exactly `SoundCue.ButtonDismiss`, `SoundCue.ButtonForward`; `TutorialScreen.kt:148` and `:293`).
- **P7** `grep -c 'SoundCue\|SoundPlayer\|sound' BoardCanvas.kt` — **pass** (0; corrected ERE form also 0).
- **P8** `grep -rnE 'TopAppBar|BottomAppBar|NavigationBar' ui/{home,game,onboarding} \| grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'` — **pass** (0 hits; literal `\|` form also 0). The three unfiltered hits are new KDoc explaining the *absence* (`HomeHero.kt:35`, `GameActionDock.kt:59`, `GameStatusStrip.kt:48`) — the four pre-existing `ui/game` KDoc mentions the plan predicted were indeed updated.
- **P9** `grep -rni 'undo' $ONB` — **1 hit, prose** (`TutorialViewModel.kt:296`, "there would undo a correct selection" — the English verb, not a control). **Comment-filtered form used** → **0 hits, pass**. `grep 'name="tutorial_' strings.xml | grep -i undo` → 0.
- **P10** `grep -rniE 'allowance|badge|×[0-9]|adReason' GameActionDock.kt` — **4 hits, all KDoc** (`:63,65,66,67`, the negative contract stated in prose). **Comment-filtered form used** → **0 hits, pass**. Literal `\|` form also 0.

### Mutation probes — five guards broken, five caught, all reverted

| Probe | Mutation | Result |
|---|---|---|
| A | `TutorialViewModel.onExit` made fire-and-forget (signal in a second `launch`) | `theExitSignalIsEmittedOnlyAfterTheSeenFlagIsPersisted` **FAILED** (`TurbineAssertionError at TutorialViewModelTest.kt:61`) |
| B | `!state.tutorialPending` dropped from `HomeScreen.kt:184` | `theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending` **FAILED** (`AssertionError at HomeScreenTest.kt:296`) |
| C | `StatPill` unknown value rendered `"0"` instead of `stat_value_unknown` | `aRibbonPillWithNoValueYetShowsTheUnknownPlaceholder` **and** `aRibbonPillWhoseReadFailedAlsoShowsThePlaceholderAndNeverZero` **FAILED** |
| D | `sounds.clear()` removed from `TutorialSoundTest.kt:80` | `TutorialSoundTest` still **passes** → see finding A-1 |
| E | interface `@Suppress` removed | `:app:detekt` **FAILED** with exactly 1 issue → Decision 1 |

A, B, C ran as one pass: `37 tests completed, 4 failed` — the four expected and nothing else.
All five files restored with `git checkout --` and verified byte-identical by SHA-256
(`TutorialViewModel.kt 55b56d25…`, `HomeScreen.kt 64e1599d…`, `HomeProgressRibbon.kt
57bb6497…`, `TutorialSoundTest.kt 144fd92c…`, `ProgressRepository.kt` by `diff -q`).
`git status --porcelain` is empty. I wrote no production code and left no edit behind.

---

## MERGE BLOCKERS

### B1 — `./gradlew check` does not pass; `domain/build.gradle.kts:41-56` (`depthSweep`)

Severity: **blocker** (studio DoD: "The repo's Check command passes locally"). Owner: **not the
developer** — no source change fixes it.

```
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':domain:depthSweep' (registered in build file 'domain/build.gradle.kts').
> java.io.EOFException
BUILD FAILED in 11m 8s
Caused by: java.io.EOFException
	at org.gradle.api.internal.tasks.testing.results.serializable.SerializableTestResultStore.openAndInitializeDecoder(SerializableTestResultStore.java:402)
	at ….SerializableTestResultStore.hasResults(SerializableTestResultStore.java:307)
Caused by: com.esotericsoftware.kryo.KryoException: Buffer underflow.
```

**It is not this diff.** Four independent facts:

1. **The source is byte-identical to a tree that passed.** `git diff --stat 57b2ad5 HEAD --
   app/src domain/src app/build.gradle.kts domain/build.gradle.kts build-logic config gradle`
   is **empty**. Commits after `57b2ad5` touch only `.planning/` and `docs/`.
2. **The same source passed `check` twice.** The Gradle daemon log
   `~/.gradle/daemon/9.5.1/daemon-42146.out.log` records, for this worktree,
   `BUILD SUCCESSFUL in 11m 7s` after a full `DepthSweepTest` run at ~15:50 and again at
   ~16:14, then `BUILD FAILED in 11m 7s` at ~16:31 and `BUILD FAILED in 11m 8s` (mine) at
   ~16:44. Two green, three red, one tree.
3. **`develop` passes the same task.** Control run in a detached `develop` worktree:
   `BUILD SUCCESSFUL in 11m18s`, exit 0, with sweep numbers identical to this branch's
   (`english33 examined=373`, `european37 609`, `asymmetric39 461`, `diamond41 785`,
   `wiegleb45 101`).
4. **No assertion failed.** On every red run the sweep printed its complete five-board report —
   the same numbers as `develop`'s green run and as `DepthSweepTest`'s own KDoc table — and
   then died with a 0-byte `results-generic.bin` and no XML report. A failed assertion is
   recorded and reported as a test failure; a 0-byte store is a worker that never flushed. The
   most recent variant reports `NoSuchFileException: …/binary/in-progress-results-generic.bin`.

Two aggravating properties of the task itself, which is why this is filed as a finding and not
just an incident:

- `domain/build.gradle.kts:55` sets `outputs.upToDateWhen { false }`, so `depthSweep` re-runs on
  every `check`; there is no cached green to fall back on.
- Once a truncated store exists, **every subsequent `check` fails in about one second, before a
  single test runs**, until someone manually deletes a git-ignored directory. I unwedged it with
  `rm -rf domain/build/test-results/depthSweep`. A ~11-minute release gate that can be left
  permanently red by a killed process, and whose recovery is undocumented, is a gate the studio
  cannot rely on — and `rules/puzzle-design.md` invariant 8 makes this class of task
  release-blocking.

Not mine to fix (reviewers do not touch source). Route to the tech-lead.

### B2 — criterion 15e (cold-start C1 gate) is NOT RUN

Severity: **blocker** by the contract's own words. `docs/ux-spec.md:1868-1869`: "Both arms'
tables are recorded in `15-BUILD-EVIDENCE.md` and **the phase does not close without them**."
`15-PLAN.md:993-995` repeats it as an S8 acceptance criterion.
`15-BUILD-EVIDENCE.md:10` states: "**Status: NOT RUN — neither arm is recorded, and no delta is
claimed.**"

I record this as **correct conduct badly constrained, not a shortcut**. The evidence file is
honest to a fault: it refuses to reuse either probe as arm B, names both pass conditions
UNRESOLVED, and justifies abandonment with a null experiment — two 10-sample runs of the *same*
build report medians 957 ms and 1014 ms, a 57 ms difference where the true difference is zero,
against a gate that asks whether a delta is ≤ 50 ms, with within-arm spreads of 172 ms and 97 ms
against Phase 6's calibrating 46 ms. An instrument that cannot resolve its own null cannot
decide the gate, and the host is documented as carrying three other agent sessions, load
averages 5.38/6.49/5.88 and a second emulator up for 4 days. Fabricating a number there would
have been the real failure. But the criterion is still unmet, and it is unmet on the one
requirement (`PLAT-08` C1 / D-03g) this refresh could plausibly regress.

### B3 — criterion 15f (first-paint placeholders, no reflow) is NOT settled

Severity: **blocker** as a phase-closure condition; `ux-spec.md:1872-1874`, and §11.3 requires
it "at the same sitting" as 15e. `15-BUILD-EVIDENCE.md:165` states it is not settled. The unit
layer covers what it can and I confirmed it does: `aRibbonPillWithNoValueYetShowsTheUnknown
Placeholder` and `aRibbonPillWhoseReadFailedAlsoShowsThePlaceholderAndNeverZero` are live
(probe C). What no unit test reaches — that the **first frame** shows "—" and the fill causes no
layout shift — needs a device session.

### Also unmet, remedy already scheduled outside this diff — criterion 15

`ux-spec.md:1845`: "On a 6.7-inch phone the two mode tiles sit in the lower half of the
screen." The shipped `HomeBands` (`HomeScreen.kt:313`) uses a **fixed**
`Spacer(Modifier.height(spacing_lg))` where `15-PLAN.md` §S6 specified a **weighted** spacer, so
with a top-aligned column in a `fillMaxSize` scroll container all slack falls to the bottom. The
developer measured the consequence and reported it rather than hiding it
(`15-BUILD-EVIDENCE.md:207-225`: tiles at ~36–54% of screen height, empty footer taking the
bottom ~45%). I confirm the mechanism in the code. Per the coordinator's scope note the
mechanism fix is now specified as follow-up work (`D-03i` / §4.2.2, which postdates this build
and which I have not held against it), so this is **recorded, not charged to this diff** — but
criterion 15 is in my contract and it is not met by what shipped.

Criteria **15a** and the "scroll position" half of **15d** are in the same bucket: the
ux-designer has established from the spec's own tokens that 15a was already unreachable on a
360×640dp window before this refresh, so I state what I see and move on. The other half of 15d
— no readout behind a tap, an expander or a "show more" — **passes**: zero expander/collapsible
constructs in `ui/home`, and `theProgressPillsAreNotTappable` asserts all three pills carry no
`OnClick` action. **Criterion 14** passes at code level: no `maxLines` anywhere in `ui/home`,
every band a `heightIn(min = …)` floor rather than a fixed height, whole column in
`verticalScroll` — so text growth expands and scrolls rather than clipping.

---

## What I verified holds, and how

### §5.1's four prohibitions hold in fact

The greps prove names are absent; these prove the behaviour:

- The tutorial hosts a real `BoardCanvas` in `interactive = true` mode
  (`TutorialScreen.kt:228-237`) driven by a `GameUiState` **assembled in
  `TutorialUiState.forStep`** (`TutorialUiState.kt:81-91`), not obtained from a game holder.
- `TutorialViewModel`'s constructor takes exactly `(OnboardingRepository, CoroutineScope)`
  (`TutorialViewModel.kt:74-77`), and
  `theTutorialViewModelsOnlyCollaboratorsAreTheOnboardingRepositoryAndTheApplicationScope`
  pins it **by reflection over the primary constructor** — so injecting a progress dependency
  later fails a test, not merely a grep.
- Step completion is `board.pegCount == TutorialUiState.STEP_COMPLETE_PEGS`
  (`TutorialViewModel.kt:227`), never a `GameStatus`, so the `Won` path that clears the resume
  snapshot and advances the ladder is structurally unreachable — which is what keeps two of the
  three D-03f cloud-save oracles intact.
- `TutorialBoards` lives in `:domain` with **zero** Android/androidx imports in the module
  (`grep -rn '^import androidx' domain/src` → 0), and is absent from `Boards.CLASSIC_SPECS` at
  three layers: the grep, `TutorialBoardsTest.noTutorialBoardAppearsInTheClassicCatalogue`
  (which compares *footprints*, not identities), and
  `HomeScreenTest.theBoardsPillDenominatorIsTheClassicCatalogueSize`.

### Test integrity — nothing ignored, deleted, or made vacuous

- `@Ignore`/`@Disabled` across `app/src` + `domain/src`: **0**. No `assumeTrue` skips. `!!` in
  `app/src/main` + `domain/src/main`: **0**. `GlobalScope`: **0**.
- **`ButtonSoundTest`'s how-to-play case was narrowed, then more than replaced.** The old
  `theHomeTopBarHowToPlayAndSettingsIconsMakeNoSound` asserted two icons silent; Home's
  how-to-play icon is deleted by D-02, so the surviving half became
  `theHomeSettingsGearMakesNoSound` and **two new tests** were added — the positive
  `theSettingsHowToPlayRowPlaysTheForwardCueAndOpensTheTutorial` and the negative
  `theSettingsSoundToggleMakesNoSound`. Both negatives are anti-vacuous by construction: each
  asserts the handler fired (`isTrue()` / `isNotNull()`) *before* asserting `sounds.cues`
  is empty, so a click that never landed cannot pass.
- **`HomeScreenTest`'s tutorial case is the one I was most suspicious of, and it is sound.**
  The retarget `theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending` keeps only an
  `assertDoesNotExist()`, which alone *would* be vacuously satisfiable — but it is paired with
  `theProfileSwapDialogIsRaisedOnceTheTutorialIsNoLongerPending`, whose state differs in
  `tutorialPending` alone and which asserts `assertIsDisplayed()`. Probe B settles it: dropping
  the guard fails the negative test. The retarget also removed a real defect — the old test
  located the tutorial by the literal string `"Got it"`.
- **The known state-write-during-composition hazard is handled explicitly, not by luck.**
  `GameScreenChromeTest.kt:176-186` hoists the toggle's state outside the composition and writes
  it only via `composeRule.runOnUiThread`, and its KDoc names the failure it is avoiding ("a
  state write during composition is a backwards write whose effect depends on which lambda's
  restart scope happens to own the read"). `HomeScreenTest`'s settable overload does the same
  from the test body. I found no test whose outcome depends on restart-scope ownership.
- Anti-vacuity checks that could have hidden a pass: `TutorialBoardsTest.terminalPegCountsOf`
  feeds `.max()`, which **throws** on an empty enumeration, so "0 positions examined" cannot
  read as green; `GameScreenChromeTest.filledState` returns `Boolean?` and Truth fails
  `isTrue()`/`isFalse()` on `null`, so a missing node fails rather than passes;
  `onNodeWithTag(...).fetchSemanticsNode()` throws when the tag is absent.
- `GameScreenSoundTest.kt` and `GameFlowTest.kt` are **not in the diff at all** — S3's "the lift
  is a pure refactor, provable because its sound test is byte-unchanged" and S7's "the content
  descriptions survived the move" are both true in the strongest available form.
- The dead-end-freeness proof is real: an exhaustive BFS over every reachable position of all
  three footprints asserting `max(terminal peg counts) == 1`
  (`TutorialBoardsTest.kt:107-125`). That is `rules/puzzle-design.md` invariants 1 and 4, and
  stronger than 4 — every line ends on one peg, so "a wrong tap is never a failure" is a
  property of the layouts rather than of a screen refusing taps.
- The risk-1 SQL aggregate is tested against a **real in-memory Room database**, not a fake:
  both-modes star sum, the `>= ANY_STARS` cleared floor pinned by a deliberate 0-star row (a
  `>= 1` implementation fails), a cross-check that the ribbon's count equals
  `aggregate().clearedClassicBoards.size` so the two readers cannot diverge, a Turbine
  re-emission test, and a delegating throwing DAO for the `HomeProgress.UNKNOWN` degradation.

### rules/compose.md accessibility — the three claimed fixes are real fixes

I verified the defects existed and are gone, rather than accepting the claim:

- **Show-moves state by tint alone → fixed.** On `develop`, `HintToggleControl.kt` carried ON/OFF
  in `primary` vs `onSurfaceVariant` tint only. HEAD adds **fill** as the primary channel
  (`DockActionChip`, `GameActionDock.kt:133-158`), keeps tint as the second and
  `show_moves_on`/`show_moves_off` as the third — and exposes the fill through a custom
  `DockActionFilledKey` semantics property so `showMovesCarriesItsStateByFillAndNotByTintAlone`
  re-proves it on every build instead of a screenshot having to settle it.
- **One glyph for two meanings → fixed.** On `develop`, `Icons.Rounded.Info` appeared at
  *both* `HintToggleControl.kt:42` and `HomeScreen.kt:350` — the same drawing meaning "help" on
  Home and "show possible moves" in gameplay. On HEAD: `grep 'Icons\..*\.\(Search\|Info\)'` → **0**.
- **Magnifying glass for "hint" → fixed.** `develop` used `Icons.Rounded.Search` at
  `HintControl.kt:69`; HEAD uses the local lightbulb `HintIcon`.
- Targets and spacing: every dock action is `game_action_button_size` = **56dp**
  (`dimens.xml`, above the 48dp floor), the strip's back chip `game_status_back_size` = **48dp**,
  and `Arrangement.SpaceEvenly` over four 56dp chips inside `spacing_sm` padding leaves ~24dp
  gaps on a 360dp width — well past the ≥8dp rule. `top_bar_touch_target` has **0** callers left.
- Semantics on every interactive element: all four dock controls and the back chip carry a
  `contentDescription`; the decorative pip row and stat pills speak once each via
  `clearAndSetSemantics` ("Step 2 of 3", "Stars, 7") instead of announcing anonymous dots, and
  `eachRibbonPillSpeaksAsOneNodeSayingItsLabelThenItsValue` asserts the exact spoken string.
- Non-colour channels elsewhere: the tutorial pips carry state as **filled vs outlined**
  (`TutorialScreen.kt:174-187`), not colour.
- Reduced motion: `rememberAnimationsEnabled()` gates the coach pulse
  (`TutorialScreen.kt:210-217`), the completion star (`:318-322`) and `HomeBlobs`.
- Light **and** dark previews exist for the one public composable added,
  `TutorialScreen` (`:355-381`).
- Per the coordinator's ruling I have **not** flagged the split clickable/labelled accessibility
  nodes: that is M3's own `IconButton { Icon(contentDescription = …) }` mapping, identical on
  four untouched screens, TalkBack consumes the merged tree, and "fixing" it would drop
  `Role.Button` and the disabled-state announcement D-04d depends on.

### Architecture, standards, security

- **No new dependency**: `git diff develop...HEAD -- gradle/libs.versions.toml
  app/build.gradle.kts domain/build.gradle.kts settings.gradle.kts build.gradle.kts` is
  **empty**, so `THIRD_PARTY.md` correctly needs no new row and is untouched
  (`rules/ip-policy.md`). Offline `check` stayed the invariant PLAT-07/D-18 requires.
- **UDF and state hoisting hold.** `TutorialRoute`/`HomeRoute` are the only collectors
  (`collectAsStateWithLifecycle`); `TutorialScreen`, `GameStatusStrip`, `GameActionDock`,
  `HomeProgressRibbon` and all four controls are stateless and take state + callbacks. One
  `StateFlow<UiState>` per holder; `TutorialViewModel` adds a buffered `Channel` for the
  one-shot exit signal (deliberately `CONFLATED`, because the signal is emitted from the
  application scope and a dropped one is a tutorial the player cannot leave).
- **Immutability**: `HomeUiState` and `TutorialUiState` are both `@Immutable` data classes;
  every `_uiState.update` is a `copy`. No mutable state escapes.
- **Seeded/deterministic**: no `Random`, no wall-clock and no sleeps in the new tests; the step
  beat and the idle nudge are driven by `advanceTimeBy` on a `StandardTestDispatcher` and by
  Robolectric's `Looper.idleFor` — and `TutorialTestHarness.kt:62-74` documents *why* the
  Robolectric looper and not `composeRule.mainClock` is the right clock, which is the kind of
  thing that is usually learned by an hour of confusion.
- **Structured concurrency**: the one write that must outlive its screen is on an injected
  `@ApplicationScope` (`TutorialViewModel.kt:165`) with a test
  (`theSeenFlagIsWrittenOnAScopeThatOutlivesTheScreen`) that cancels `viewModelScope`
  mid-flight; no `GlobalScope`.
- **No magic numbers or strings**: 15 new dimens, all multiples of 4, two retired with 0
  remaining callers; the DAO query interpolates table/column *constants*; every literal in the
  new composables is a named `private const val`. `dpSpLiteralCheck`, `adIdLiteralCheck`,
  `gamesIdLiteralCheck` and `audioFocusLiteralCheck` all pass.
- **Security — clean.** No secrets (the 13 apparent hits are the word "token" meaning *design
  token*), no network of any kind added, no new permission. The one new query
  (`LevelResultDao.kt:98-110`) binds `:classicMode` and `:minStars` as Room parameters — no
  string concatenation, no injection surface — and returns a non-null `Flow` of a one-row
  correlated-subquery projection. The new destination is `@Serializable data object Tutorial`
  with **no arguments**, and the manifest's only `intent-filter` is the launcher, so no deep
  link reaches it. The DataStore write is a boolean flag. The tutorial writes nothing else.
- **`:domain` purity intact**: 0 `android`/`androidx` imports in `domain/src`.

---

## Advisory findings (none blocks the merge)

- **A-1 — `TutorialSoundTest.kt:80`: `sounds.clear()` discards the cues the test's name claims
  to cover.** The test is `theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep`, and it
  taps select → deselect → re-select → an immovable peg, **then clears the recorder**, then taps
  one illegal destination and asserts `containsNoneOf(Select, Deselect, InvalidTap, Undo, Hint)`.
  Four of the five gestures it names are therefore not asserted; a `Select` cue on selection
  would pass. Probe D removed the line and the file **still passes**, which establishes both that
  nothing currently sounds on those taps *and* that the `clear()` is unnecessary — deleting it
  strictly widens coverage at zero cost. The same shape appears at `:98` in
  `theCompletionStarPopsWithoutTheWinOrStarPopCue`, where the clear happens after the star has
  already composed. Advisory rather than blocking because P6's allow-list grep and P7's
  `BoardCanvas` grep both hold, so the contract is structurally guarded as well. One-line fix,
  not mine to make.
- **A-2 — `TutorialViewModelTest.kt:90-94`: a test name that promises three routes and
  exercises one.** `everyExitRouteSkipBackAndCompletionPersistsTheSeenFlagExactlyOnce` iterates
  `listOf({ it.onExit() }, { it.onExit() }, { it.onExit() })` — three *identical* lambdas. It
  runs one route three times. The three real call sites are one-line wirings in
  `TutorialRoute.kt:43,53,54`, and Skip and the CTA are covered by `TutorialSoundTest`, so
  nothing is unguarded — but the name overclaims, and a reviewer reading names would be misled.
  `TutorialSoundTest.kt:68`'s "…OnAnyStep" overclaims the same way (it exercises T1 only).
- **A-3 — `TutorialScreen.kt:112-118` repeats the already-ruled `widthIn` no-op.** The modifier
  chain is `fillMaxSize().verticalScroll(…).widthIn(max = content_max_width).padding(…)`, the
  same order that makes the 480dp cap a no-op at `HomeScreen.kt:296-303`. That Home instance is
  already known and scheduled into the follow-up slice; I record the tutorial's copy **so the
  follow-up fixes both** rather than leaving a second one behind.
- **A-4 — `TutorialScreen.kt:181`: `PIP_BORDER_WIDTH.dp`, where the same branch uses a token.**
  A 2dp `BorderStroke` width from a private const, against `CLAUDE.md`'s "no hardcoded dp/sp
  scattered in composables". `dpSpLiteralCheck` cannot see it (its regex requires a digit before
  `.dp`), and `UndoIcon.kt:25` sets a `CONST.dp` precedent on `develop` — but the
  counter-example is in this same diff: `GameActionDock.kt:151` takes its border width from
  `dimensionResource(R.dimen.peg_selected_ring_width)`. The two new-icon `ICON_SIZE_DP.dp` uses
  are fine (an icon's authoring canvas, not a layout dimension); this one is a layout dimension.
- **A-5 — a new non-skippable composable on the start destination.** `ResumeTile`
  (`HomeScreen.kt:374-380`) and `HomeBoardArt` (`:64`) take `board: Board`, and `Board` carries
  no `@Immutable` — it cannot, since `:domain` has no Compose dependency — while transitively
  holding `BoardSpec.inPlay: List<Position>`. The Compose compiler therefore infers the
  parameter unstable and neither composable can skip, on the screen whose cold-start cost is
  exactly what criterion 15e measures and did not measure. `BoardPreview.kt:52,73` already does
  this on `develop`, so the pattern is house practice and this is consistent with it; I raise it
  only because B2 leaves the cold-start budget unverified. A tech-lead decision (annotate at the
  UI boundary, or hand the tile a pre-derived stable model), not a defect.
- **A-6 — one string beyond the plan's inventory, and it is the right one.** 14 strings are
  genuinely new, not §12.1's 13: the extra is `home_stat_pill_desc` (`"%1$s, %2$s"`), which the
  spec's own announcement contract requires (`ux-spec.md:802`, "one merged semantics node reading
  label-then-value"). The plan's table was incomplete, not the code wrong. The four deletions are
  contract-backed: `how_to_play` per §8, and `onboarding_step_1/2/3` ruled deleted at
  `ux-spec.md:82` and `:1463`, which supersedes §12.1's "needing a ruling".
- **A-7 — pre-existing, out of scope, flagged once because this diff adds `:domain` code.**
  `rules/kotlin-android.md` says `check` runs `koverVerify` and `rules/testing.md` requires
  ≥ 90% line coverage on the domain layer. **There is no kover in this repo** — zero references
  in `gradle/libs.versions.toml`, `build-logic/`, or either module — and
  `build-logic/src/main/kotlin/standards.gradle.kts:265-273` wires only detekt, ktlint and the
  four literal gates. So no coverage gate exists to enforce that rule. Tech-lead's call.
- **A-8 — pre-existing, carried across verbatim, worth a ux-designer glance.** `TimerPill`
  (`GameStatusStrip.kt:187-192`) sets `contentDescription = "Elapsed time"` on the `Text` whose
  own text is the `mm:ss` value. A content description overrides text for TalkBack, so the
  clock is likely announced as "Elapsed time" **without the time**. `develop`'s
  `ElapsedTimeLabel` did exactly the same (`GameScreen.kt:563-574`), and the KDoc's claim that
  the description is unchanged is accurate — so this diff neither caused nor worsened it, and
  `GameFlowTest` depends on the string. Not a finding against this diff.
- **A-9 — nit.** `ButtonSoundTest.kt:298`: `private fun clickHomeText(resId: Int) =
  clickSettingsRow(resId)` — a Home helper delegating to a function named for Settings. The KDoc
  explains it; the name still reads wrong at the call sites.

## Open issues / risks

- **The end-to-end loop-freedom of the first-run auto-start is argued, not tested.**
  `HomeRoute.kt:63-64`'s `LaunchedEffect(state.tutorialPending)` re-runs when Home re-enters
  composition on the pop, so freedom from "Home re-pushes the tutorial forever" depends on the
  flag already being `false` by that moment. The *mechanism* that guarantees it — `markSeen()`
  awaited before the exit signal — is tested and I proved that test live (probe A). The
  *composition* is not: `NavFlowTest` stubs Home, so the real effect never runs, and no test
  composes real `HomeRoute` + real `TutorialRoute` over a real `OnboardingRepository` across a
  pop. Criterion 7a's device run covers it; worth naming so it is not assumed covered.
- B1's flaky gate will hit the next role to run `check` in this worktree, and the recovery
  (`rm -rf domain/build/test-results/depthSweep`) is written down nowhere but here.
- 15e, 15f, 15, 15a, 25-with-animations-off, 7a, 7c and 26 all remain device readings. The
  developer's smoke observations (`15-BUILD-EVIDENCE.md:180-203`) are explicitly labelled "not a
  QA sign-off" and I have treated them as such.
- `docs/ux-spec.md` was amended twice while I worked (`fc16c6e`, `370e733`). Per the
  coordinator's scope note I judged the diff against the pre-amendment contract; `D-03i`,
  §4.2.2, criteria 15g–15j, 26a and the two new tokens are **not** part of this verdict.

## Handoff

- **tech-lead** — B1: `:domain:depthSweep` (`domain/build.gradle.kts:41-56`) is a
  release-blocking gate that can be left permanently red by a killed worker and needs a
  reliable result store or a documented recovery. Also A-5 (Board stability at the UI boundary)
  and A-7 (no coverage gate exists for a rule that names one).
- **qa-engineer** — B2 and B3: run §11's procedure on a quiet host, using the corrections
  `15-BUILD-EVIDENCE.md:95-159` already folded in (the WAL-pull fix is a real one), and settle
  15f, 15a, 25, 7a, 7c and 26 in the same sitting. Add the file's own two-runs-of-one-arm
  precheck before trusting either arm.
- **android-game-dev** — advisory only, and only if the producer dispatches it: A-1 (one line),
  A-2 (two test names), A-3, A-4. **No blocker requires a code change.**
- **producer** — criterion 15 is unmet and its remedy is already specified as follow-up work; B1
  is nobody's phase and needs scheduling; and this report's verdict rests on gate conditions, not
  on code quality, which the merge decision should reflect.

**VERDICT: FAIL** — three gate conditions unmet (B1 Check command red, B2 criterion 15e NOT RUN,
B3 criterion 15f unsettled), plus criterion 15 unmet with its remedy already scheduled
elsewhere. **No merge-blocking defect was found in the diff's code**: the ten §10 checks, the
four §5.1 prohibitions, the three accessibility fixes, the test integrity of both retargets and
the four guards I broke and saw caught all pass. One `@Suppress` approved; both new vectors
accepted. Re-audit on Fable when the allowance returns, given the deviation recorded at the top.
