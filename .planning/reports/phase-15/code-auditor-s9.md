# code-auditor report — phase-15 S9 (review-fix slice) re-audit
Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (f9ad048..30a2cad, plus the report commit f13888c)
Skills loaded: ergoz-soft:standards (role: code-auditor), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none (sole agent and sole Gradle build in worktree `PegSolitarie-p15`)
Ran on: opus (`claude-opus-5[1m]`) at effort xhigh — **DEVIATION**, see the note below; `agents/code-auditor.md` pins `model: fable`
Fan-out: none
Loops: 0
Lessons applied: none

Rule files read (plugin copies — `.claude/rules/VERSION` is 0.5.1 against plugin 0.6.2, so the
snapshot is stale): `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`,
`memory.md`, `puzzle-design.md`, `testing.md`, `kotlin-android.md`, `compose.md`.
Lessons: `.claude/rules/lessons.md` — 0 active.

---

## DEVIATION FROM STUDIO STAFFING — read this first

**This re-audit ran on Opus, the same model as the author of the code it reviews.**

`agents/code-auditor.md` pins this role to Fable because rule 1 of the studio's model policy makes
a diff reviewable only on a model above its author's — LLM self-review research shows a reviewer
favours output it recognises as its own, and `android-game-dev` is `model: opus`. Fable's allowance
is exhausted; the founder decided the review proceed on Opus (`.planning/questions/FOUNDER.md`
Q-003). The agent definition says a review that cannot get Fable **does not happen on another
model**. It happened anyway, so this report is a deviation from the studio's own staffing rule and
**should not be treated as satisfying rule 1**.

What I did to compensate, deliberately:

- I took **no** recorded result on trust. Every line in `## Checks` is a command I ran.
- I read **none** of `.planning/reports/phase-15/android-game-dev-*.md` and none of
  `.planning/log/`. I judged the diff against `docs/ux-spec.md`, `15-PLAN.md` §10 and the rules.
  (I did re-read my own earlier report, which the task permits, and the four commit messages,
  which are part of the diff.)
- Where a guard mattered I **broke the implementation and confirmed the test fails** — six
  mutation probes, all reverted, restoration proved by SHA-256.
- I did not accept the developer's "13 new tests, none weakened" claim, or criterion 15h's green
  test. **I hunted assertions that cannot fail, and I found one.**

---

## Summary

`./gradlew check` is **green** in this worktree (11m07s, exit 0), which clears my earlier B1. All
ten §10 prohibitions still pass, §5.1's four still hold in fact, the suppression count is
unchanged at 13, nothing is `@Ignore`d, and 13 new tests were added with none removed or
weakened by deletion. Five of the six guards I broke were caught, including the exact shape
§4.2.2 clause 4 forbids. F-1, F-2, A-1 and the `fillMaxSize()` ordering bug are genuinely fixed.

**VERDICT: FAIL** — one merge blocker, and it is in the diff, not in a gate: criterion **15h** is
certified by an assertion that is **arithmetically incapable of failing**, and on the very window
that test measures the last band overflows the viewport by **44 dp**. The slice reports as met a
criterion that is not met.

## Outputs

- `.planning/reports/phase-15/code-auditor-s9.md` — this report

---

## Disposition of my earlier report's blockers

| Item | Disposition |
|---|---|
| **B1** — `./gradlew check` red | **CLEARED.** My own run: `BUILD SUCCESSFUL in 11m 7s`, exit 0, 98 actionable tasks. Not by a source change — the tech-lead's diagnosis (two concurrent Gradle builds in one worktree clobber the binary result store) is right, and with one build in the worktree the gate is green. The gate's own defect is unfixed and scheduled as `bugfix/check-gate-concurrency` off `develop`; the DoD condition is met on this branch. |
| **B2** — criterion 15e NOT RUN | **Reassigned, still outstanding, no longer chargeable to this diff.** The ux-designer re-specified 15e as a relative gate against a same-sitting null control (`ux-spec.md:1462-1553`) and ruled phase 15 closes with `:NOT RUN`; `ux-spec.md:1485` makes 15e a **closing condition of this slice**, and the producer assigned it to the qa-engineer on this branch. It blocks the merge as a slice-closing condition; it is not a code defect and nothing in the diff can satisfy it. |
| **B3** — criterion 15f unsettled | **STILL STANDING, unchanged.** `ux-spec.md:2113-2116` explicitly detaches 15f from 15e and keeps it a phase-15 sign-off item on this build. Owner: qa-engineer, device session. |
| **Criterion 15** — tiles in the lower half | **CLEARED in mechanism, and the guard is live.** Probe 3a2: with the pre-fix fixed `Spacer` restored, `theFirstModeTileStartsBelowTheMidpointOnAFreshInstall` fails at `expected to be at least: 457.5.dp but was: 376.0.dp` — the device defect, in dp. On the reference window with real insets my own measurement puts the Classic tile at 476..584 dp and the Challenge tile at 748..864 dp against a 421 dp midpoint, no scroll needed. Device confirmation still owed to QA. |

---

## MERGE BLOCKER

### MB-1 — criterion 15h is certified by an assertion that cannot fail, and the criterion is not met

`app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt:300`
(test `bothChipsAndBothTilesAreInsideTheViewportOnAShortPhoneWithNoHeroArt`, `:293-301`)

```kotlin
assertThat(bottomOf(challengeTileTitle)).isAtMost(contentBottom())
```

Severity: **blocker**. Owner: **android-game-dev** for the assertion; **ux-designer** for what
yields next; **qa-engineer** for the device numbers.

**Half 1 — the assertion is a tautology.** `getBoundsInRoot()` returns bounds **clipped to the
root**, so `bottomOf(anything) ≤ contentBottom()` holds by construction; a node pushed entirely
off-screen reports `0.0.dp..0.0.dp`. Established by execution, not by reading: I ran that exact
line verbatim while the Challenge tile was completely outside the first viewport (a 24 dp + 48 dp
system-bar inset supplied through the same `ViewCompat` path `TutorialScreenTest` uses):

```
PROBE[tautology] bottomOf(challengeTile)=0.0.dp contentBottom()=640.0.dp
PROBE[tautology] the committed assertion PASSED with the tile at 0.0.dp
PROBE[tautology] assertIsDisplayed(challengeTile) = false
PROBE[tautology] challengeTile unscrolled=0.0..0.0 scrolledIntoView=476.0..592.0
```

The test's companion `assertIsDisplayed()` calls do not close the gap: Compose satisfies
`assertIsDisplayed()` on **partial** visibility, so they pass with a band clipped (they do fail in
the fully-off-screen case above). Probe 3d confirms the only live assertion in that test is the
hero-art one: with the compact band set disabled the test fails on
`assertDoesNotExist` for `home_hero_art` and never reaches the containment line.

**Half 2 — and the property is false on the test's own window.** At `w360dp-h640dp` with
Robolectric's zero insets — exactly what the committed test measures — the Challenge tile is
clipped:

```
PROBE[360x640 zero insets] challengeTile unscrolled=568.0..640.0  scrolledIntoView=524.0..640.0
PROBE[360x640 zero insets] OVERFLOW (scroll needed) = -44.0.dp
```

The tile is 116 dp tall; 72 dp of it is visible and **44 dp sits below the fold**. Criterion 15h
(`ux-spec.md:2130-2134`) requires Home to show "**without scrolling** — both Continue chips and
both mode tiles". It does not. With the spec's own device inset figure (24 dp status + 48 dp
navigation, `ux-spec.md:972`) the tile leaves the first viewport entirely and needs a 476 dp scroll.

**Why §4.2.2's arithmetic missed it.** The table at `ux-spec.md:961-970` totals the compact
both-chips set at ≈544 dp from band **minimums**. Measured at 360 dp width the bands are much
taller, because the text wraps:

| Band | §4.2.2 floor | Measured (360 dp wide, default font) |
|---|---|---|
| Hero (compact) | 72 | 44 (title) inside a 72 dp row |
| Ribbon | ≥ 64 | **116** (`88.0..204.0`) |
| Continue chip | 72 | **108** (`220.0..328.0`) |
| Mode tile | 96 | **108** / **116** (`452.0..560.0`, `524.0..640.0`) |
| **Total** | **≈ 544** | **≈ 684** |

That is ~140 dp over the table, and the gap is systematic, not marginal. The exact dp are
Robolectric's text metrics and QA should re-measure on glass, but the direction is not in doubt:
684 dp of content in a 640 dp viewport, before a single dp of inset.

**The spec prescribed the action and it was not taken.** `ux-spec.md:1077-1081`: "**If the compact
set still does not fit** on a real 360×640dp device with both chips at default font scale — the
headroom is ~24dp on the worst inset case and every number above is a minimum — **stop and report
the measured numbers to the producer.** The next candidate to yield is a readout or a target, and
that is a design change I will make against measured numbers rather than guess at here." A green
test was committed instead. Commit `30a2cad`'s own evidence for this test is only the art half:
"Failed before: the hero art was found at 360x640dp" — the containment half is never claimed to
have failed against anything, which is consistent with its being unable to.

**Remedy, and it is small.** Make the assertion real, then act on what it says. Two idioms already
exist in this diff:
1. Assert the node is **not moved** by a scroll — `before.top == onNode(...).performScrollTo().getBoundsInRoot().top`
   — which is what my probe measured; or
2. Supply the insets the way `TutorialScreenTest.applySystemBarInsets()` (`:299-320`) already
   does, so the JVM window is the ~568 dp a 360×640 dp phone actually offers rather than 640 dp.

The inset harness is in the same diff, written for F-1, and was not carried across to the Home
test where the margin is 24 dp instead of 96 dp. That is the single change that turns criterion
15h's test from green into decisive.

**What this is not.** It is not a regression: before this slice the 360×640 dp case was worse (the
full band set needs ~148 dp more than the window, per `ux-spec.md:973-974`). The compact band set
is a large, correct improvement. The blocker is the **false green**, not the layout. A slice may
legitimately fail to fit that window and say so; it may not certify that it fits with an assertion
incapable of failing.

---

## Advisory findings (none blocks the merge)

- **A-10 — `TutorialScreenTest.kt:141-157`: `theCompletionButtonClearsTheNavigationBar` is not a
  guard.** Probe 2 removed `.safeDrawingPadding()` from `TutorialScreen.kt:139` and ran the class:
  `5 tests completed, 1 failed` — and the failure is the *other* test. This one **passes against
  the broken implementation**. Its own KDoc says it was put on a short window "**deliberately**"
  so it could not be "the kind of test that passes for the wrong reason", and that is exactly what
  it is: at 360×640 dp the tutorial's four bands total ~592 dp, so without the 80 dp of inset
  padding the column no longer overflows, `performScrollTo` is a no-op and the CTA has ~40 dp of
  air below it. Advisory rather than blocking because, unlike MB-1, it certifies something that
  **is** true: one `safeDrawingPadding()` call insets all four edges, and that one call is proven
  live by the top-edge test. It adds no coverage; it removes none.
- **A-11 — `HomeScreenTest.kt:271-276`: `theFirstModeTileStartsBelowTheMidpointWithBothContinueChips`
  would not have caught the defect it is named for.** Probe 3a2 restored the pre-fix shape (one
  fixed `Spacer(spacing_lg)`, no weights) and only the *fresh-install* test failed; this one
  passed. That matches QA's device reading exactly — the both-chips state was never the failure —
  so it is an honest regression guard for a state that already held, not a proof of the fix. Not a
  defect: criterion 15g requires both states. Worth one line so nobody reads it as the load-bearing
  test.
- **A-12 — the 60 ms layering interval is unguarded (pre-existing, but this slice now owns it).**
  `FakeSoundPlayer.PlayedCue` (`FakeSoundPlayer.kt:9-12`) records `cue`, `rate` and
  `gateBypassed` — **no timestamp** — and no test references `SoundParams.REMOVAL_LAYER_DELAY_MS`
  as a value (only two comments naming it). Deleting `delay(...)` from `JumpSoundEffect.kt:79`
  would leave every test green while collapsing the texture layer into the doubled hit
  `SOUND-02` exists to avoid. The shipped fix **does** preserve the delay — I read it, and the
  order is `playJump()` synchronously, then a launch that suspends 60 ms before `play(PegRemove)`,
  so nothing is reordered and nothing fires before the state clears. The gap predates the slice;
  the slice is the moment to name it, because it is the slice that moved the delay's owner out of
  the keyed effect.
- **A-13 — `HomeScreen.kt:751-755`: the new compact `@Preview` is dark-only.** rules/compose.md
  asks for light and dark. `HomeScreen`'s own two previews cover both, so the rule is met for the
  public composable; the third preview shows a band set in one theme only. Nit.
- **A-14 — one new `SubcomposeLayout` on the start destination, unmeasured.**
  `BoxWithConstraints` (`HomeScreen.kt:338`) is the right tool — the threshold must be read
  **after** the `Scaffold` has subtracted the insets, which `LocalConfiguration` cannot give — and
  `ux-spec.md:1083-1088` licenses the cost. But it is still a cold-start cost on the screen whose
  cold-start gate has never run, and 15e is now a closing condition of this branch, so the
  measurement is scheduled rather than missing. Pairs with my earlier A-5.
- **Carried forward, unfixed, still advisory:** A-2 (`TutorialViewModelTest.kt:90-94`, three
  identical lambdas behind a name promising three routes), A-4 (`TutorialScreen.kt:207`
  `PIP_BORDER_WIDTH.dp` where the same diff uses `dimensionResource` for a border width), A-8, A-9.
  A-5 and A-7 are deferred to the tech-lead's sign-off per `STATUS.md` item 8.
- **Fixed from my earlier report:** **A-1** ✓ (`d20a27a` — both `sounds.clear()` calls deleted;
  the five-gesture test now asserts over all five and the star test sees the whole playthrough).
  **A-3** ✓ **both instances** — Home's chain in `30a2cad` and the tutorial's copy in `f9ad048`,
  which is what I asked for when I recorded the second one.

---

## Checks

Every line below I executed, in `/Users/ermanergoz/Desktop/PegSolitarie-p15`, as the only Gradle
build in the worktree.

- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — **pass**
  (`BUILD SUCCESSFUL in 11m 7s`, exit 0, `98 actionable tasks: 10 executed, 88 up-to-date`;
  `:domain:depthSweep` printed its full five-board report — `english33 examined=373`,
  `european37 609`, `asymmetric39 461`, `diamond41 785`, `wiegleb45 101` — and completed. No
  `rm -rf` was needed: the store was intact because no second build touched it.)
- `… ./gradlew :app:testDebugUnitTest :domain:test --offline --no-build-cache --rerun-tasks` —
  **pass** (`BUILD SUCCESSFUL in 1m 17s`; parsed from the JUnit XML: **app 1005 tests**,
  **domain 274 tests**, **0 failures, 0 errors, 0 skipped**. The `check` run had both test tasks
  `UP-TO-DATE`, so this is the run in which every test actually executed under my hand.)
- `… ./gradlew :app:detekt :domain:detekt :app:ktlintCheck :domain:ktlintCheck :app:dpSpLiteralCheck :domain:dpSpLiteralCheck :app:lint --offline --rerun-tasks --no-build-cache` —
  **pass** (`BUILD SUCCESSFUL in 37s`, zero findings; the `check` run had detekt/ktlint/lint
  `UP-TO-DATE`, so this is the uncached reading.)
- All thirteen new tests confirmed present in the executed results by name — **pass** (each found
  in exactly one result XML).
- `15-PLAN.md` §10's ten prohibition commands — **all pass**, run by me (table below).
- Secrets / attack-surface scan over the slice diff — **pass** (no credential shapes; no `http`,
  socket, Retrofit, OkHttp, `openConnection`, `File(` or `getSharedPreferences` added;
  `AndroidManifest.xml` untouched; `git diff develop...HEAD` over `gradle/libs.versions.toml`,
  both `build.gradle.kts`, `settings.gradle.kts` and the root build file is **empty**, so
  `THIRD_PARTY.md` correctly needs no new row — rules/ip-policy.md).

### §10's ten prohibition checks — all pass

`$ONB` = `app/src/main/java/com/app/pegsolitaire/ui/onboarding/`. Where §10 writes `\|` inside
`grep -E` (a literal pipe, making the command vacuous) I ran the corrected ERE form as well; both
agree.

| # | Result |
|---|---|
| **P1** | `grep -rn 'GameScreen' $ONB` → **0**; `grep -rn 'BoardCanvas(' $ONB` → **1**. Both halves. |
| **P2** | 3 raw hits, all KDoc prose; §10's own comment filter → **0**. |
| **P3** | ERE form → **0**. |
| **P4** | `awk '/composable<Tutorial>/,/^        }/' AppNavHost.kt \| grep -c 'navigate('` → **0**. The block's one statement is `tutorialContent { navController.popBackStack() }` (`AppNavHost.kt:141`). |
| **P5** | → **0**. |
| **P6** | Exactly `SoundCue.ButtonDismiss` and `SoundCue.ButtonForward`, nothing else. |
| **P7** | → **0**. |
| **P8** | 4 raw hits, **all KDoc explaining the absence** (`HomeHero.kt:36`, `HomeHero.kt:100` — new, the compact hero's "this is not a top bar" — `GameStatusStrip.kt:48`, `GameActionDock.kt:59`); filtered → **0**. |
| **P9** | 1 raw hit (prose, the English verb); filtered → **0**. `grep 'name="tutorial_' strings.xml \| grep -ci undo` → **0**. |
| **P10** | 4 raw hits, all KDoc stating the negative contract; filtered → **0**. |

### Mutation probes — six guards broken, five caught

| Probe | Mutation | Result |
|---|---|---|
| **1** | `JumpSoundEffect.kt` reverted to pre-fix (`3400828~1`) | **4 caught.** `18 tests completed, 4 failed` — exactly the four animations-off tests, each `missing (1): PegRemove — expected: [Jump, PegRemove] but was: [Jump]`. The QA device reading (1 audio event instead of 2) reproduced in the JVM. The animations-**on** control `aJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt` still passed, which proves the new tests exercise the snap path and not a second copy of the old one. |
| **2** | `.safeDrawingPadding()` removed from `TutorialScreen.kt:139` | **1 caught, 1 missed.** `skipsWholeTouchTargetClearsTheSystemBarsOnEveryStep` **FAILED** with `expected to be at least: 56.0.dp but was: 36.0.dp` — the identical message `f9ad048`'s body records as its pre-fix failure. `theCompletionButtonClearsTheNavigationBar` **passed** → finding A-10. |
| **3a** | slack ratio inverted 3:1 → 1:3 | 1 caught (`…OnAFreshInstall`). |
| **3a2** | pre-fix shape restored (one fixed `Spacer(spacing_lg)`, no weights) | **1 caught:** `…OnAFreshInstall` **FAILED**, `expected to be at least: 457.5.dp but was: 376.0.dp`. `…WithBothContinueChips` passed → finding A-11. |
| **3b** | `fillMaxSize()` put back ahead of `widthIn(max = …)` | **caught.** `theContentColumnIsCappedAndCentredOnAWindowWiderThanTheCap` **FAILED**. Item 5's guard is live. |
| **3c** | the hero wrapped in `Box(Modifier.weight(CONTENT_WEIGHT))` — **the exact shape §4.2.2 clause 4 forbids** | **caught.** `everyBandSurvivesAndStaysReachableAtDoubleFontScale` **FAILED**: `The component with Text … contains 'Peg Solitaire' … is not displayed!` — the hero measured at 0 and clipped to nothing at 200% font scale, precisely as the spec predicts. Clause 4 is not merely obeyed; it is defended. |
| **3d** | compact band set disabled (`compact` forced false) | **caught** on the art assertion; the containment assertion was never reached → MB-1. |

All probes reverted. `app/src/main/java/com/app/pegsolitaire/{audio/JumpSoundEffect.kt,
ui/onboarding/TutorialScreen.kt, ui/home/HomeScreen.kt, ui/home/HomeHero.kt}` verified
**byte-identical to HEAD** by SHA-256 diff; the one probe file I added under `app/src/test/` was
deleted; `git status --porcelain` is **empty**. A final clean
`:app:testDebugUnitTest :domain:test --rerun-tasks` is **BUILD SUCCESSFUL**. I wrote no production
code and left no edit behind.

---

## The five items, item by item

**1 — F-1, the tutorial's window insets: FIXED, and correctly at the screen level.**
`safeDrawingPadding()` sits on the **column** (`TutorialScreen.kt:139`) and **outside**
`verticalScroll` (`:140`), so the scrolling viewport itself is inset and no band can be scrolled
under a bar — not a nudge on one control. It holds on every step because the header carrying
`Skip` is band 1 of a step-independent column, and the test walks all three
(`TutorialScreenTest.kt:110-125`). It holds on **both entry paths** because there is exactly one
composition path: Home's first-run effect (`HomeRoute.kt:63-64`) and Settings' How-to-play row both
`navigate(Tutorial)`, whose body is `TutorialRoute` → `TutorialScreen`, and the screen's
`modifier` parameter lands on the `Surface` (`:115`), not on the inset column, so no caller can
displace it. No ancestor consumes the insets (no `Scaffold`, no `enableEdgeToEdge`, no
`setDecorFitsSystemWindows` anywhere above the nav host), so `WindowInsets.safeDrawing` reads the
real values under the SDK-36 edge-to-edge enforcement the commit cites. The chain also fixes A-3
for this screen. Guard: live (probe 2), with the caveat of A-10 on the bottom edge.

**2 — F-2, the peg-removal layer with OS animations off: FIXED, and fixed the right way.**
The delayed cue moves to `rememberCoroutineScope()` (`JumpSoundEffect.kt:74,78-81`); the pitched
pop stays inside the keyed effect and the key stays `transition?.id`. That matters and the code
says why: the id is the move ordinal and recycles after an undo, so latching the last non-null
transition would have survived the snap path and silenced the replay instead — and
`aJumpReplayedAfterAnUndoSoundsAgainEvenThoughItsMoveIdIsReused` exists to fail exactly that wrong
fix (probe 1 shows it failing on the pre-fix code, so it is live).
- **Layers not reordered:** `playJump()` is called synchronously *before* the launch, the launch
  suspends 60 ms before `play(PegRemove)`, and two tests pin it —
  `containsExactly(Jump, PegRemove).inOrder()` plus `sounds.events.first().cue == Jump`
  (`GameScreenSoundTest.kt:117-120`).
- **Nothing fires before the state clears:** the delay is intact at `:79`. This was the failure
  mode to look for and it did not happen. The *interval* has no test — see A-12.
- **Pitch ladder undisturbed:** `playJump(runIndex = transition.runIndex)` unchanged;
  `theJumpPitchStillClimbsWhenTheOsAnimationScaleIsZero` asserts `jumpRate(0)` then `jumpRate(1)`
  and fails on the pre-fix code.
- The mechanism is real, not asserted: `disableOsAnimations()` writes the same
  `Settings.Global.ANIMATOR_DURATION_SCALE` that `rememberAnimationsEnabled()`
  (`Clay.kt:209-218`) reads, before composition because that read is `remember(context)`, so
  `runJumpAnimation` takes its genuine snap path (`BoardCanvas.kt:298-304`). The scope is the same
  shape `BoardCanvas` already uses for the landing squash, and it dies with the composition, so
  leaving the screen inside the 60 ms still drops the layer — the one case where dropping it is
  right. This was a defect of the **shipped** game, so fixing it in the shared effect fixes both
  screens at once, which is what S3's lift was for.

**3 — A-1, `sounds.clear()`: FIXED.** Both clears deleted (`d20a27a`). The five-gesture test now
runs all five taps under one assertion, and the star test now sees the whole playthrough — the
commit proves the second one live by adding `Jump` to the `containsNoneOf` list and watching it
fail. Residual, unchanged from before: both are negative assertions with no in-test positive
control, so they lean on the sibling positive tests (which use the same `tapCell` helper and
assert `Jump` fires) to establish that the taps land. Acceptable; the widening is strictly better
than what it replaced.

**4 — D-03i / §4.2.2: implemented to the letter, and clause 4 holds.**
- Modifier order is the contract, verbatim: `align(Alignment.TopCenter)` → `verticalScroll` →
  `heightIn(min = availableHeight)` → `widthIn(max = content_max_width)` → `padding(bandSpacing)`
  (`HomeScreen.kt:344-350`). No other modifier in the chain sets a height.
- `fillMaxSize()` is on the `BoxWithConstraints` and **nowhere else** in the band column: the only
  three occurrences in `ui/home` are `HomeBlobs.kt:48`, `HomeScreen.kt:142` (the `Scaffold`'s own
  modifier), `:148` (the screen `Box` that applies `contentPadding`) and `:338`. `HomeBlobs` is
  still the band column's sibling behind it, as clause 1 requires.
- `maxHeight` is read **after** the `Scaffold`'s inset subtraction, because the
  `BoxWithConstraints` is inside the `Box(padding(contentPadding))` — so the 720 dp threshold and
  the slack are both measured on available height, as specified, and against height only, never
  the font scale.
- **No content band is weighted.** I enumerated every `.weight(` in `ui/home`: eight. Two are the
  slack `Spacer`s (`:364`, `:406`); the other six are `CONTENT_WEIGHT` on **`Row`** children
  (`HomeProgressRibbon.kt:70,80,90`, `HomeScreen.kt:461,647`, `HomeHero.kt:116`) — free *width*
  beside a fixed accent, which is a different axis and a different constant, correctly not reused.
  Probe 3c proves the rule is defended, not merely followed.
- The two new tokens are 4 dp multiples with KDoc naming D-03i, and the slack ratio is two named
  `private const val`s rather than a reused `CONTENT_WEIGHT`.
- **Where it does not deliver: criterion 15h — MB-1.**

**5 — the `fillMaxSize()`-before-`widthIn(max)` ordering bug: FIXED on both screens, guard live.**
`theContentColumnIsCappedAndCentredOnAWindowWiderThanTheCap` fails when the ordering is restored
(probe 3b). The assertion is meaningful because `onNodeWithText(classicTileTitle)` resolves to the
**merged** clickable tile node, not the bare `Text` — confirmed by
`theOnlyClickableThingsOnHomeAreTheGearTheTwoChipsAndTheTwoTiles` asserting
`assertHasClickAction()` on the same locator — so `tile.right - tile.left` really is the capped
column's width, and the symmetric-margin check really is centring.

---

## Test integrity

- **13 new test functions, 0 removed.** `git diff f9ad048~1..30a2cad -- 'app/src/test/*'` shows 13
  added `fun` declarations, **zero** removed `fun` declarations and **zero** removed `@Test`
  annotations. All 13 confirmed executed by name in the result XML.
- **Nothing ignored or disabled.** `@Ignore`, `@Disabled`, `assumeTrue`, `assumeFalse` across
  `app/src` + `domain/src`: **0**.
- **No new suppression.** `@Suppress`/`@file:Suppress` in `app/src/main` + `domain/src/main`:
  **13 on `develop`, 13 on HEAD**. The slice adds and removes none — `grep` over the four dev
  commits finds no `Suppress` line added or deleted at all. No baseline file exists anywhere in the
  repo, and no detekt/lint baseline is configured.
- **`!!` in production code: 0. `GlobalScope`: 0. Android imports in `:domain`: 0.**
- **No magic numbers or strings.** No raw `.dp`/`.sp` literal added to a composable; two new
  `dimens` tokens; every new numeric in production code is a documented `private const val`; no new
  string resource. `dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck` and
  `audioFocusLiteralCheck` all pass.
- **Two assertions that cannot fail** — MB-1 and A-10. That is what this section is for, and it is
  the one place the slice's "none weakened" claim needs qualifying: no existing test was weakened,
  but two of the thirteen new ones do not bite.

### APPROVED SUPPRESSION (restated, unchanged)

**approved by code-auditor** — `@Suppress("TooManyFunctions")` at
`app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:51`. My earlier report
established by execution that it is load-bearing, hides exactly one finding, and leaves the net
suppression count at zero while trading a file-level suppression for a declaration-level one. This
slice does not touch it and the count is still 13/13. The approval stands.

---

## Standards, architecture, accessibility, security

- **State hoisting / UDF.** `HomeHero(compact: Boolean, onOpenSettings: () -> Unit)` decides
  nothing — `HomeBands` reads the height and passes the flag, as §4.2.2 requires ("decided by
  `HomeBands` … never by this composable"). `TutorialScreen` is still stateless; `TutorialRoute`
  and `HomeRoute` are still the only collectors, via `collectAsStateWithLifecycle()`. One
  `StateFlow<UiState>` per holder. `HeroTitle` and `SettingsGear` are factored so the `heading()`
  semantics and the gear's `contentDescription` **cannot diverge between the two band sets** —
  the right way to split a band that has two shapes.
- **Accessibility (rules/compose.md).** The gear is the same `IconButton` in both sets, at the
  framework's 48 dp minimum, and it is the one thing §4.2.2 says may never yield — it does not.
  In the compact row it is the only target, so the ≥ 8 dp rule is trivially met. The title carries
  no `maxLines` in either set, so 200% font wraps it and the column scrolls rather than clipping
  (criterion 14/15i) — proven live by probe 3c. `HOME_HERO_ART_TEST_TAG` is a test tag on
  decorative art, which TalkBack does not announce, and its KDoc says so. `clayEntrance` gates the
  compact hero's entrance on reduced motion exactly as the full hero's does. No colour-only
  channel added; no hard-coded colour; Material 3 theme throughout.
- **One-handed use.** This is the change's purpose and it is delivered on the reference window:
  Classic tile `476..584 dp`, Challenge tile `748..864 dp`, both wholly below the 421 dp midpoint,
  no scroll needed — my measurement, with 24 dp + 48 dp of real insets applied.
- **Performance (`compose-performance-audit`).** No lazy list, no image decode, no work in
  composition, no unstable parameter added; the slack `Spacer`s carry no semantics, entrance or
  background, so they cost a measure pass and nothing else. The one new cost is the
  `SubcomposeLayout` behind `BoxWithConstraints` — justified, licensed by the spec, and unmeasured
  (A-14). `rememberCoroutineScope()` allocates once and each jump launches one 60 ms coroutine on a
  per-gesture path, not a per-frame one.
- **Security (`security-reviewer`).** Clean. Attack surface unchanged: no dependency, no network
  call, no file or preference access, no permission, no manifest change, no credential shape, no
  new input surface. The two test-only platform pokes (`Settings.Global.putFloat`,
  `ViewCompat.dispatchApplyWindowInsets`) touch Robolectric shadows and have no production path.
- **rules/ip-policy.md.** No new dependency, so `THIRD_PARTY.md` correctly needs no row and is
  untouched. Offline `check` stayed green, so PLAT-07/D-18's offline invariant holds.

## The process deviation — `compose-skill` and TDD invoked after the code

The developer self-reports invoking `compose-skill` and the TDD skill *after* writing the code
rather than before, as rules/compose.md requires, and says it worked test-first throughout and
re-reviewed the diff against both afterwards with no resulting change.

**The diff bears that out, and I checked rather than took it.** On the Compose side the code
satisfies the skill's checklist on the points a late read would most likely have missed: modifier
order matches the spec's contract character for character, `fillMaxSize()` is removed from the one
chain where it was doing harm, the two shapes of the hero share their semantics-bearing leaves so
they cannot drift, and the forbidden weighted-content-band shortcut — the one an author under time
pressure reaches for — is not merely avoided but fenced by a test that I broke and watched catch
it. On the TDD side the claim is checkable and it checks out: `f9ad048`'s body records the
pre-fix failure as `expected to be at least: 56.0.dp but was: 36.0.dp` and `30a2cad`'s as
`top 376.0dp against a 457.5dp midpoint`; probes 2 and 3a2 reproduced **both messages verbatim**,
including the decimals. A test written after a passing implementation does not come with the
failing output of the implementation it replaced.

So: a real order violation of rules/compose.md, honestly disclosed, with no detectable effect on
the artefact. Recording it as a process finding for the producer and the retro, not as a code
finding, and not against the verdict. The one place the discipline did lapse is not the skill
order — it is MB-1 and A-10, where two assertions were written without being watched fail.

## Open issues / risks

- MB-1's remedy will most likely report that the compact band set does **not** fit a 360×640 dp
  device with both chips, which is a **design** question the spec reserved to the ux-designer
  against measured numbers. My numbers are Robolectric's; QA's device numbers should govern.
- Criterion **15a** therefore remains unmet on its own baseline, and §4.2.2's claim that "the
  compact band set below is what keeps them there" (`ux-spec.md:975`) is not borne out by
  measurement. The ux-designer had already conceded 15a was unreachable before the refresh; this
  says the compact set narrows the gap from ~148 dp to ~44 dp without closing it.
- 15e, 15f, 15, 15g, 15i's device half, 25, 7a, 7c and 26 all remain device readings. 15e is now a
  closing condition of **this** branch and is assigned.
- The `check` gate's own fragility is unfixed. It was green for me only because I was the sole
  build in the worktree; the recovery (`rm -rf domain/build/test-results/depthSweep`) and the fix
  are on `bugfix/check-gate-concurrency`.
- `docs/ux-spec.md` was amended on this branch by the ux-designer (`a89fd61`) and STATUS by the
  producer (`e5d2607`) **mid-slice**. I attributed by commit, not by range: those two are not the
  developer's work and nothing in them is charged to this diff. I judged the diff against the
  amended spec because §4.2.2 and criteria 15g–15j *are* this slice's contract.

## Handoff

- **android-game-dev** — **MB-1** (`HomeScreenTest.kt:300`): make the criterion-15h assertion
  capable of failing (assert a scroll does not move the node, or supply the insets the way
  `TutorialScreenTest.kt:299-320` already does), then report what it says rather than fixing the
  layout to suit it. Same treatment for **A-10** (`TutorialScreenTest.kt:141-157`). Optional:
  A-12 (a timestamp on `PlayedCue` would let one test pin the 60 ms), A-13, A-4, A-2.
- **ux-designer** — §4.2.2's ≈544 dp compact total is ~140 dp light because it sums band minimums
  while the bands wrap to 116/108/108 dp at 360 dp width. This is the "stop and report the measured
  numbers" branch of `ux-spec.md:1077-1081`: the next thing to yield on a 360×640 dp phone is a
  readout or a target, and that is yours to decide.
- **qa-engineer** — 15e on this branch per the second-edition procedure, and 15f, 15, 15g, 15h, 15i,
  25, 7a, 7c, 26 in the same sitting. 15h now needs a real 360×640 dp reading; my JVM numbers say
  it fails and they are Robolectric's, so yours govern. Note the precondition the spec sets and the
  worktree defect enforces: **no concurrent Gradle build in any worktree**.
- **tech-lead** — B1's mechanism is confirmed by a green run under single-build conditions;
  `bugfix/check-gate-concurrency` is the fix and is still owed. A-5 and A-7 remain on your
  sign-off list; A-14 adds one `SubcomposeLayout` to the start destination to that conversation.
- **producer** — this FAIL is one blocker **inside the diff** (a false green on criterion 15h),
  not a gate condition, so unlike my last report it is fixable by the developer in this slice. B1
  is cleared; B2 is assigned to QA on this branch; B3 stands. The mid-slice documentation commits
  in this worktree (`a89fd61`, `e5d2607`) are the producer error you already recorded — they cost
  me nothing but they did put a moving contract under a diff under review. And this audit is still
  a rule-1 deviation: re-audit on Fable when the allowance returns.

**VERDICT: FAIL** — one merge blocker: **MB-1**, criterion 15h is certified by an assertion that
cannot fail while the property it certifies is false by 44 dp on the test's own window. Everything
else in the slice is sound: `check` green (B1 cleared), all ten §10 prohibitions pass, §5.1 holds,
1279 tests green with 0 skipped, 13 tests added and none removed, the suppression count unchanged
at 13, and five of six broken guards caught — including the exact weighted-content-band shape
§4.2.2 clause 4 forbids. F-1, F-2, A-1 and the `fillMaxSize()` ordering bug are genuinely fixed.
