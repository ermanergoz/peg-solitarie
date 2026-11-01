# android-game-dev report — phase-15 S9 (the review-fix slice)

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (f9ad048..30a2cad — my four commits; `a89fd61` and `e5d2607`
are the ux-designer's/tech-lead's concurrent `docs(15)` commits, interleaved with mine on the
same branch in the same worktree, and are not mine)
Skills loaded: ergoz-soft:standards (role: android-game-dev) — rule files read:
studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md,
play-store.md, kotlin-android.md, compose.md, testing.md (snapshot `.claude/rules/`, v0.5.1);
compose-skill; superpowers:test-driven-development
Ran on: Opus 5 (1M context), one worktree (`PegSolitarie-p15`), no fan-out
Ran in parallel with: the ux-designer/tech-lead's criterion-15e re-spec (docs only, same branch)
Loops: 0
Lessons applied: none

## Summary

All five dispatched items are fixed, each test-first with the failure recorded before the fix.
F-1's insets, F-2's lost audio layer (a pre-existing defect of the shipped game, now fixed on
both screens), D-03i/§4.2.2's slack mechanism with its two new tokens, Home's dead 480dp width
cap, and A-1's `sounds.clear()`. `check` is green on the handed-over tree; all ten §10
prohibitions and all four §5.1 prohibitions hold; no suppression, baseline, `@Ignore`, disabled
test, `!!` or hardcoded dp/sp was added. 13 new tests, 0 removed, 0 weakened.

## Outputs

- `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt` — F-1:
  `safeDrawingPadding()` on the column outside `verticalScroll`; a `Box` so the `Surface`'s
  propagated minimum width stops making `widthIn(max = content_max_width)` a no-op (A-3).
- `app/src/main/java/com/app/pegsolitaire/audio/JumpSoundEffect.kt` — F-2: the delayed removal
  cue moved to the composition scope.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` — D-03i: `HomeBands` root is a
  `BoxWithConstraints`; the contract modifier chain; two weighted `Spacer`s 3 : 1; the compact
  band set's spacing; two new weight constants; a third `@Preview` showing the compact set.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeHero.kt` — the compact/full hero split
  (`FullHero`, `CompactHero`, and the shared `HeroTitle`/`SettingsGear` leaves).
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeBoardArt.kt` — `HOME_HERO_ART_TEST_TAG`,
  so "the hero art is absent" is asserted as composed-or-not rather than by its size.
- `app/src/main/res/values/dimens.xml` — the two new tokens: `home_hero_compact_min_height`
  72dp, `home_compact_threshold_height` 720dp (both multiples of 4).
- `app/src/test/.../ui/onboarding/TutorialScreenTest.kt` — +2 tests (F-1) and the inset harness.
- `app/src/test/.../ui/game/GameScreenSoundTest.kt` — +3 tests (F-2, the shipped game).
- `app/src/test/.../ui/onboarding/TutorialSoundTest.kt` — +1 test (F-2, criterion 7c); A-1's two
  `sounds.clear()` deletions.
- `app/src/test/.../ui/home/HomeScreenTest.kt` — +7 tests (criteria 15g–15j) and a font-scale
  overload of the render helper.
- `THIRD_PARTY.md` — **no row needed**: `git diff --stat b332131..HEAD -- gradle/
  '*build.gradle.kts' settings.gradle.kts` is empty, so no dependency was added and `--offline`
  stayed green throughout (PLAT-07/D-18).

## Checks

### The Check command — green on the tree being handed over

- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **pass** (BUILD
  SUCCESSFUL in 11m07s, exit 0, 98 actionable tasks: 25 executed, 73 up-to-date; the plan's
  baseline is 11m09s/11m10s). Run **after** the last source commit `30a2cad`;
  `git status --porcelain -- app/` is empty, so the green tree is the handed-over tree.
  `:domain:depthSweep` ran to completion and printed its five-board report with the same numbers
  the auditor's `develop` control run gave (`english33 examined=373`, `european37 609`,
  `asymmetric39 461`, `diamond41 785`, `wiegleb45 101`). B1 did not recur in this run and I did
  not have to delete the result store.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :domain:test --offline --rerun-tasks` —
  **pass** (BUILD SUCCESSFUL in 11s, 15 tasks executed). Run because `check` reported
  `:domain:test UP-TO-DATE` — correct, since `git diff b332131..HEAD -- domain/` is **empty** —
  and an up-to-date task is not an executed one. Forced, it is green: the generator/solvability
  gates ran on my tree.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --offline` — **pass**
  (BUILD SUCCESSFUL in 36s; **1005** tests, 0 failures, 0 errors, 0 skipped, counted from the
  JUnit XML).
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:ktlintCheck :app:detekt
  :app:dpSpLiteralCheck :app:audioFocusLiteralCheck --offline` — **pass** (zero findings).

**On `FUZZ_SEEDS`.** My quality gate says "fuzz honours `FUZZ_SEEDS`". That env var **does not
exist anywhere in this repo** (`grep -rn FUZZ domain build-logic` → 0 hits outside `build/`);
`:domain`'s harness is level-span and sweep based (`ChallengeSolvabilityTest`, `QualityGateTest`,
and the `RUN_SWEEP`-gated `DepthSweepTest` via the dedicated `:domain:depthSweep` task that
`check` depends on — `TestFlags.kt` documents the flag convention). That gap is pre-existing,
belongs to the tech-lead with A-7 (no kover either), and this slice changed **no** `:domain`
file, so those gates ran exactly as they do on `develop`.

### F-2 — how I proved both audio layers now fire with animations off

Not by reading. Three JVM tests set the **real** `Settings.Global.ANIMATOR_DURATION_SCALE` to
`0f` — the same value `rememberAnimationsEnabled()` reads — before composing, so `BoardCanvas`
takes its real snap path rather than a mocked approximation, and a recording `FakeSoundPlayer`
is the oracle. `composeRule.mainClock.advanceTimeBy(500)` lets the 60 ms layer land before the
assertion reads.

**Before the fix, they reproduced QA's `dumpsys audio` reading exactly** — one cue per jump:

```
aJumpStillLayersTheRemovalCueWhenTheOsAnimationScaleIsZero
    missing (1): PegRemove    expected: [Jump, PegRemove]        but was: [Jump]
theJumpPitchStillClimbsWhenTheOsAnimationScaleIsZero
    missing (1): PegRemove    expected: [Select, Jump, PegRemove] but was: [Select, Jump]
aJumpReplayedAfterAnUndoSoundsAgainEvenThoughItsMoveIdIsReused
    missing (1): PegRemove    expected: [Select, Jump, PegRemove] but was: [Select, Jump]
```

After the fix all three pass, asserting `containsExactly(Jump, PegRemove).inOrder()` — so both
layers fire, **in that order**, with animations off. Two further properties are pinned because
the task forbids buying the layer with a reordering or a lost ladder:

- **Order**: `sounds.events.first().cue` is `Jump`, and the assertion is `.inOrder()`. The
  pitched pop is still played **synchronously inside** the keyed effect; only the 60 ms-delayed
  cue moved.
- **Pitch ladder intact**: `theJumpPitchStillClimbsWhenTheOsAnimationScaleIsZero` plays a
  two-hop same-peg chain with animations off and asserts rungs `jumpRate(0)` then `jumpRate(1)`.
- **The replay case is a deliberate guard against the other obvious fix.** The move id is the
  move ordinal and recycles after an undo, so latching the last non-null transition and keying
  on its id would have survived the snap path *and silenced a replayed jump*. That test fails on
  the latching fix and passes on the one shipped.

The tutorial's own half is `TutorialSoundTest.aCompletedJumpStillLayersTheRemovalCueWhenTheOs
AnimationScaleIsZero` (criterion 7c), which also asserts the rung-0 rate. Both screens are
covered on purpose: the effect is shared, so a guard on one call site says nothing about the
other — which is how S3's lift carried the defect forward with a green suite.

The fix is `BoardCanvas.kt:129-134`'s existing, documented pattern for the identical
cancellation (`squashScope`), not a new idea: the same trigger (`onAnimationFinished()` clearing
`animating` changes the key and kills the coroutine), the same remedy.

### §10's ten prohibition checks — all pass, re-run on this tree

`$ONB` = `app/src/main/java/com/app/pegsolitaire/ui/onboarding/`;
`$FILTER` = `grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'` (§10's own P8 comment filter).

| # | Command | Result |
|---|---|---|
| **P1** | `grep -rn 'GameScreen' $ONB` / `grep -rn 'BoardCanvas(' $ONB` | **pass** — 0 hits; 1 hit (`TutorialScreen.kt:254`). Both halves. |
| **P2** | `grep -rn 'GameViewModel' $ONB \| $FILTER` | **pass** — 0. The 3 raw hits are KDoc prose (`TutorialViewModel.kt:122,123,126`). |
| **P3** | `grep -rnE 'recordResult\|clearSnapshot\|advancePast\|saveSnapshot\|ProgressRepository\|ResumeRepository\|ChallengeProgress' $ONB` | **pass** — 0 (ERE form; §10's literal `\|` form is vacuous, as the auditor noted). |
| **P4** | `awk '/composable<Tutorial>/,/^        }/' AppNavHost.kt \| grep -c 'navigate('` | **pass** — 0. |
| **P5** | `grep -rn 'TutorialBoards' domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` | **pass** — 0. |
| **P6** | `grep -rho 'SoundCue\.[A-Za-z]*' $ONB \| sort -u` | **pass** — exactly `SoundCue.ButtonDismiss`, `SoundCue.ButtonForward`. Unchanged by the F-2 fix, which is inside `JumpSoundEffect`. |
| **P7** | `grep -cE 'SoundCue\|SoundPlayer\|sound' BoardCanvas.kt` | **pass** — 0. The F-2 fix deliberately did **not** go into the shared board leaf. |
| **P8** | `grep -rnE 'TopAppBar\|BottomAppBar\|NavigationBar' ui/{home,game,onboarding} \| $FILTER` | **pass** — 0. Raw hits are now **4**, not 3: `CompactHero`'s KDoc adds one, documenting that the compact hero is *not* a bar. All four are ` * ` comment lines. |
| **P9** | `grep -rni 'undo' $ONB \| $FILTER` | **pass** — 0 (1 raw prose hit, `TutorialViewModel.kt:296`). `grep 'name="tutorial_' strings.xml \| grep -ci undo` → 0. |
| **P10** | `grep -rniE 'allowance\|badge\|×[0-9]\|adReason' GameActionDock.kt \| $FILTER` | **pass** — 0 (4 raw KDoc hits, `:63,65,66,67`). |

### §5.1's four prohibitions still hold

Unchanged by this slice and re-proved above: the tutorial hosts `BoardCanvas` and never
`GameScreen` (P1); its holder is `TutorialViewModel`, pinned by reflection over the primary
constructor; it writes no `LevelResult`, clears no snapshot, advances no ladder (P3); its only
outbound edge is `popBackStack()` (P4). I touched no navigation, no repository and no ViewModel.

### The other non-negotiables

- `@Suppress` occurrences in `app/src/main` + `domain/src/main`: **12 at the branch tip I
  started from (`b332131`), 12 at HEAD** — this slice adds **zero**. (`develop` is 11; the
  difference is the branch's `ProgressRepository` interface suppression, already **approved by
  code-auditor**.)
- No lint baseline, no `@Ignore`, no `@Disabled`, no `assumeTrue`, no disabled or deleted test:
  `git diff b332131..HEAD -- app domain | grep '^+' | grep -E '@Suppress|@Ignore|@Disabled|assumeTrue|!!'`
  → **0 hits**. (The four `grep` hits for "baseline" are the English word in KDoc — "the small
  baseline device".)
- No hardcoded dp/sp added: `git diff b332131..HEAD -- app/src/main | grep '^+' | grep -E
  '[0-9]+\.dp|[0-9]+\.sp|[A-Z_]+\.dp'` → **0 hits**; every new dimension is a token and
  `:app:dpSpLiteralCheck` passes.
- **Home's three load-bearing readouts survive, none behind a tap, expander or scroll.** The
  Challenge tile's level chip, both Continue chips and the Classic tile (the one tap to the
  catalogue's star pips) are all asserted *displayed* inside the viewport on the short baseline
  by `bothChipsAndBothTilesAreInsideTheViewportOnAShortPhoneWithNoHeroArt`, which is a stronger
  statement than the pre-existing presence tests. `theOnlyClickableThingsOnHomeAreTheGearTheTwo
  ChipsAndTheTwoTiles` pins that nothing new became tappable and the ribbon's pills still are
  not. No `maxLines` anywhere in `ui/home`; the compact hero's title has none either.
- **13 new tests, 0 removed, 0 weakened.** `HomeScreenTest` 24 → 31, `GameScreenSoundTest`
  10 → 13, `TutorialScreenTest` 3 → 5, `TutorialSoundTest` 4 → 5. The only deletions in a test
  file are A-1's two `sounds.clear()` lines, which *widen* two assertions.

### Test-first, with each failure recorded

| Item | The test written first | Its recorded failure before the fix |
|---|---|---|
| F-1 | `skipsWholeTouchTargetClearsTheSystemBarsOnEveryStep` | `expected to be at least: 56.0.dp but was: 36.0.dp` (the step pips, step 1) |
| F-2 | three tests, above | `missing (1): PegRemove` on all three |
| 15g | `theFirstModeTileStartsBelowTheMidpointOnAFreshInstall` | `expected to be at least: 457.5.dp but was: 376.0.dp` — QA's device reading in dp |
| 15h | `bothChipsAndBothTilesAreInsideTheViewportOnAShortPhoneWithNoHeroArt` | `Did not expect any node but found '1' ... Tag: 'home_hero_art'` at 360×640dp |
| 15j | `theContentColumnIsCappedAndCentredOnAWindowWiderThanTheCap` | `expected to be at most: 480.0.dp but was: 768.0.dp` on an 800dp window |
| A-1 | the widened assertions | probe: adding `Jump` to the star test's `containsNoneOf` **fails** with `full contents: [Jump, PegRemove, Jump, PegRemove, …]`, where before the deletion that list was empty |

Two of the seven new Home tests (`…WithBothContinueChips`, `everyBandSurvivesAndStaysReachable
AtDoubleFontScale`) passed on the first run. That is honest and deliberate: they pin properties
that already held — the both-chips state passed criterion 15 on the device, and nothing was
clipped at 200% — so they are regression guards for the mechanism I changed underneath them, not
TDD reds. I say so rather than pretending they were red first.

### Verified before building on it — the D-03i mechanism

§4.2.2 asks for a measurement rather than a fallback if the pinned Compose BOM does not
distribute weight this way. It does. Probe at 411×915dp, two 100dp bands, spacers 3 : 1 inside
`verticalScroll` + `heightIn(min = maxHeight)`: spacers took **476dp and 159dp** of 635dp of
slack (the arithmetic exactly). With one band at 2000dp the spacers took **0** and the column
scrolled. Probe deleted; the property is now covered by the four committed tests instead.

I also had to establish that a JVM test *can* see this at all, since neither insets nor window
size are available by default: `@Config(qualifiers = "w411dp-h915dp")` does size the Compose
root, and insets reach Compose only when dispatched at the `AndroidComposeView` itself
(`ViewCompat.dispatchApplyWindowInsets`) — dispatching at the decor view is silently swallowed
by Robolectric's shadowed `ViewRootImpl`, which is worth knowing before the next person spends
an hour on it.

## Deviations from the plan

1. **I invoked `compose-skill` and `superpowers:test-driven-development` after writing the code,
   not before.** `ergoz-soft:standards` was first, as dispatched, and I worked test-first
   throughout (every red is recorded above), but `rules/compose.md`'s "MUST invoke
   `compose-skill` first" was not honoured in order. I loaded both and re-reviewed the whole
   diff against them before writing this report; the review changed nothing, and its one
   observation is item 2 below. Recorded as a process deviation for the auditor rather than
   quietly listed as "skills loaded".
2. **`safeDrawingPadding()` sits ahead of `verticalScroll`, and `heightIn`/`widthIn` after it,
   which inverts `rules/compose.md`'s "layout → size → padding" rule of thumb.** Both positions
   are forced: the padding must be outside the scroll or bands scroll *under* the system bars,
   and `heightIn(min)` must be inside it or the weight mechanism cannot work (§4.2.2 makes the
   order contract and says no other modifier in the chain may set a height). Commented at both
   call sites.
3. **I also fixed the tutorial's copy of the `widthIn` no-op (code-auditor A-3),** which the
   dispatch listed only for Home (item 4). It is literally the same ordering defect in the exact
   modifier chain I was rewriting for F-1, the auditor asked that the follow-up "fix both", and
   the ux-designer's ruling ties the cap-and-centre restoration to the mechanism. On any phone
   narrower than 480dp it changes nothing; it changes tablets.
4. **I deleted A-1's second `sounds.clear()` as well** (`theCompletionStarPopsWithoutTheWinOr
   StarPopCue`), which the dispatch scoped to line 80. Same one-line shape, same auditor
   finding, and the probe above proves the widening is live.
5. **I added a production test tag (`HOME_HERO_ART_TEST_TAG`) and a third `@Preview`.** Neither
   is in §4.2.2's text. The tag is the only honest oracle for "the art is not composed at all"
   — asserting on its size would be a claim about the token — and the preview makes a band set
   that is otherwise invisible without a short device reviewable. §4.2.2 clause 5's "no test
   tag" applies to the **spacers**, which have none.
6. **Not attempted, as dispatched: the cold-start measurement (criterion 15e).** Its gate was
   being re-specified while I worked — by `a89fd61`/`e5d2607` on this very branch — and is not
   mine. No cold-start number is claimed anywhere in this slice.
7. **`15-BUILD-EVIDENCE.md` is not extended.** The evidence for this slice is this report, and
   that file's cold-start sections were being rewritten concurrently by another role; appending
   to it would have raced them.

## Open issues / risks

- **The worktree is not single-occupancy, contrary to the dispatch.** Two `docs(15)` commits
  (`a89fd61`, `e5d2607`) and an uncommitted `docs/ux-spec.md` edit appeared under me while I
  worked. I staged only my own paths on every commit (never `git add -A`) and touched none of
  theirs, but a reviewer reading `b332131..HEAD` as one diff will see `docs/ux-spec.md`,
  `.planning/reports/STATUS.md` and `.planning/reports/phase-15/ux-designer-coldstart-criterion.md`
  in it, and **those are not mine**. The four `*-s9` commits are. It also means another Gradle
  build could have been started in this worktree during my 11-minute `check`; it was not, and
  B1 did not recur, but the producer should know the isolation guarantee did not hold.
- **The compact set's real headroom on a 360×640dp *device* is still unmeasured.** §4.2.2 asks
  me to stop and report the numbers if the compact set does not fit with both chips at default
  font scale. In the JVM test it fits, but Robolectric reports **zero** insets, so the test sees
  640dp of available height where a real phone offers ~568dp (24dp status + 48dp navigation).
  My arithmetic against the measured band heights leaves single-digit dp of headroom there — the
  spec itself predicts ~24dp on the worst inset case — so **criterion 15h is a genuine device
  question, not a settled one.** If it overflows on the device, the next thing to yield is a
  readout or a target, which is a ux-designer decision against measured numbers and not mine.
- **`SoundParams.REMOVAL_LAYER_DELAY_MS` now outlives its keyed effect by design.** Navigating
  away inside those 60 ms drops the layer (the scope dies with the composition), which is the
  right behaviour but is new; a device pass that leaves a board mid-jump is the way to see it.
- Still open and not dispatched to me: **A-2** (two test names that overclaim), **A-4**
  (`TutorialScreen.kt`'s `PIP_BORDER_WIDTH.dp`, a layout dimension as a `const` rather than a
  token — pre-existing, and `dpSpLiteralCheck` cannot see it), **A-5** (`Board` stability at the
  UI boundary, now slightly more relevant since `BoxWithConstraints` adds a subcomposition to
  the start destination), **A-7** (no kover, and no `FUZZ_SEEDS`), **O-1** (the Challenge tile
  has a glyph where §4.2 specifies board art), **O-2** (light-theme status-bar icons —
  `isAppearanceLightStatusBars` is unset app-wide; *more* visible now that the tutorial's
  content no longer sits under the bar, though the bar was always light-on-light there),
  **O-3**, **O-5** (unlocked orientation).
- **B1** (`:domain:depthSweep`'s result-store wedge) did not recur in my run. The recovery, for
  whoever hits it next, is `rm -rf domain/build/test-results/depthSweep`.

## Criteria only a device session can re-settle

QA should re-test from this list; nothing in it is claimed as passing here.

| Criterion | What to check, and why the JVM cannot |
|---|---|
| **5 / F-1** | Tap the **centre of the "Skip tutorial" label** on **all three steps**, on **both entry paths** (first-run auto-start after clearing app data, and Settings › How to play) — the fix is in the screen both paths compose, but only the device proves the touch geometry. Expect the tutorial to exit and **1** audio event. Repeat at `font_scale 2.0` (the label must be legible and clear of the signal/wifi icons) and in light theme. Also re-check the step pips no longer sit over the system clock, and the completion CTA clears the navigation bar. |
| **7c / 25 / F-2** | `dumpsys audio` with all three animation scales at 0: **two** player events ~60 ms apart per completed jump, on the tutorial **and** on a real board. The JVM proof uses the real `Settings.Global` value, but only the device says the two samples are audible as a layered pair. |
| **15 / 15g / F-3** | **On a fresh profile** (the failing case): the Classic tile's top edge at or below the midline, and the bottom third no longer bare. QA's target to beat is centre 1173 against a 1212 px midline with 34% dead footer. Then the both-chips state, which must not regress. |
| **15h / 15a** | 360×640dp with **both** resumes at default font: both Continue chips and both mode tiles fully on screen **without scrolling**, hero art absent, hero one row. This is the criterion my headroom risk above is about — please record the measured band heights, not just pass/fail. |
| **15i / 14** | 200% font on **both** baselines: the gradient title neither truncated nor shrunk (it should wrap and the column should scroll), no band dropped, everything reachable. |
| **15j** | On a tablet or a wide window: the content column capped at 480dp and **centred**, on Home and now on the tutorial too. |
| **15f** | First-paint placeholders with no reflow — unchanged by this slice and still device-only. |
| **26** | The TalkBack half: the compact hero's traversal order (title → gear) and the gear's announcement, which the JVM cannot read. Geometry is unchanged: the gear is still `IconButton`'s 48dp. |
| **23, 27** | Unchanged by this slice and still owed (hint spinner; contrast measurement). |
| **15e** | **Not mine and not attempted** — the gate is being re-specified. |

## Handoff

Next: **code-auditor** — review `f9ad048..30a2cad` (the four `*-s9` commits only; the two
interleaved `docs(15)` commits belong to another role). The deviations above are the places to
look hardest: the modifier-order inversion, the two out-of-dispatch fixes (A-3, A-1's second
line), and the new production test tag. The `check` evidence is in `## Checks`, and I have named
the one gate whose JVM proof is weaker than its device question (15h's inset headroom).

Then: **qa-engineer** — the table above is the re-test list, and F-1's "both entry paths, every
step" and F-2's `dumpsys audio` delta are the two that closed FAIL items. Nothing in this slice
settles 15e, 15f, 23, 26 or 27.

Also for the **producer**: the worktree-isolation note under Open issues, and the still-open
advisory items (A-2, A-4, A-5, A-7, O-1 to O-5) that were not dispatched to me.
