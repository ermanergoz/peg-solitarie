# android-game-dev report — phase-15 UX refresh, slices S5–S8
Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (c642676..57b2ad5)
Skills loaded: ergoz-soft:standards (role android-game-dev), compose-skill, superpowers:test-driven-development, gitflow, android-viewmodel, android-testing
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md, lessons.md
Ran in parallel with: phase-14 release track (other worktrees; not touched)
Loops: 0
Lessons applied: none

## Summary
S7 (gameplay chrome) and S8 (token retirement + evidence) are committed, each on its own
green `check`; S5 and S6 were already committed by the interrupted dispatch and are covered
here from their commits, with every acceptance criterion re-verified rather than inherited.
The S7 draft I was handed was ~90% sound; I fixed one genuine test defect in it, added a
`TextOverflow`, and proved every new assertion fails when its mechanism is removed.
**The C1 cold-start measurement is NOT RUN** — the instrument failed its own null experiment
(same build twice: 57 ms apart against a 50 ms threshold), so no number was invented.
Three §10 prohibition greps need a comment filter the plan wrote on P8 only; with it, all ten pass.

## Outputs
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameStatusStrip.kt` — back chip, context title, timer pill; no actions (§4.3)
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt` — the four actions on one clay slab, plus `DockActionFilledKey` (§4.4)
- `app/src/main/java/com/app/pegsolitaire/ui/game/HintIcon.kt` — lightbulb, replaces `Icons.Rounded.Search`
- `app/src/main/java/com/app/pegsolitaire/ui/game/ShowMovesIcon.kt` — peg + two dots, replaces `Icons.Rounded.Info`
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` — `GameTopBar`/`GameTopBarTitle`/`ElapsedTimeLabel` deleted; strip + board + dock column
- `app/src/main/java/com/app/pegsolitaire/ui/game/{Undo,Hint,HintToggle,Restart}Control.kt` — reskinned onto `DockActionChip`, retargeted to `game_action_button_size`
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenChromeTest.kt` — 5 tests (criteria 16–19, 21, 24)
- `app/src/test/java/com/app/pegsolitaire/ui/game/{Undo,Hint}ControlTest.kt` — +1 test each (criteria 22, 23)
- `app/src/main/res/values/dimens.xml` — +6 gameplay tokens, −2 retired
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` — stale `TopAppBar` KDoc S6 left behind, corrected
- `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md` — the C1 arms as NOT RUN with the calibration evidence, the corrected procedure, and the device-only criteria

## Checks

### Full `check` runs (the only invocation whose result may be quoted — §0.1)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` (end of S7) — **pass** (BUILD SUCCESSFUL in 11m07s; 98 tasks; baseline 11m09s)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` (end of S8) — **pass** (BUILD SUCCESSFUL in 11m07s; 98 tasks)
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests '*GameScreenChromeTest*' --tests '*UndoControlTest*' --tests '*HintControlTest*' --tests '*ButtonSoundTest*' --tests '*GameFlowTest*' --offline` — **pass** (33 tests, 0 failures, 0 skipped)
- Whole branch: **1302 tests, 0 failures, 0 skipped**

### The ten §10 prohibitions — all pass, three with a correction
The plan gives P8 a comment filter (`grep -vE '^[^:]+:[0-9]+: *(\*|//|/\*)'`) and omits it from
P2, P9 and P10, which therefore hit **KDoc prose only**. Both numbers are given so a reviewer
running the literal command is not surprised. Script: `/tmp/p15-prohibitions.sh` (reproduced in full below).

| # | Command | Raw | Filtered | Verdict |
|---|---------|-----|----------|---------|
| P1a | `grep -rn 'GameScreen' …/ui/onboarding/` | 0 | 0 | pass |
| P1b | `grep -rn 'BoardCanvas(' …/ui/onboarding/` | 1 (≥1) | — | pass |
| P2 | `grep -rn 'GameViewModel' …/ui/onboarding/` | **3** | **0** | pass — 3 KDoc citations of `GameViewModel.kt:788-796/:817/:823`, the lines that justify the tutorial's tap rule matching the game's. No code reference. Backed by `TutorialViewModelTest.theTutorialViewModelsOnlyCollaboratorsAre…` (reflection). |
| P3 | `grep -rnE 'recordResult\|clearSnapshot\|advancePast\|saveSnapshot\|ProgressRepository\|ResumeRepository\|ChallengeProgress' …/ui/onboarding/` | 0 | 0 | pass |
| P4 | `awk '/composable<Tutorial>/,/^        }/' AppNavHost.kt \| grep -c 'navigate('` | 0 | — | pass |
| P5 | `grep -rn 'TutorialBoards' domain/…/model/Boards.kt` | 0 | — | pass |
| P5b | `grep -rn 'CLASSIC_SPECS' domain/…/tutorial/` | 0 | — | pass |
| P6 | `grep -rho 'SoundCue\.[A-Za-z]*' …/ui/onboarding/ \| sort -u` | `SoundCue.ButtonDismiss` `SoundCue.ButtonForward` | — | pass — exactly the two-name allow-list |
| P7 | `grep -c 'SoundCue\|SoundPlayer\|sound' …/components/BoardCanvas.kt` | 0 | — | pass |
| P8 | `grep -rnE 'TopAppBar\|BottomAppBar\|NavigationBar' …/ui/{home,game,onboarding}` + filter | 3 | **0** | pass — the four `ui/game` KDoc hits the plan predicted are gone; the 3 remaining are `GameStatusStrip.kt:48` ("It is **not** a `TopAppBar`") and `HomeHero.kt:35` ("moves here from the **deleted** `TopAppBar`") |
| P9 | `grep -rni 'undo' …/ui/onboarding/` | **1** | **0** | pass — `TutorialViewModel.kt:296`, the English verb in "would undo a correct selection", quoting `ux-spec.md:896`. Left verbatim rather than reworded to game a grep. |
| P9b | `tutorial_*` string naming Undo | 0 | — | pass |
| P10 | `grep -rniE 'allowance\|badge\|×[0-9]\|adReason' GameActionDock.kt` | **4** | **0** | pass — the KDoc that *states* the prohibition triggers the grep for it. Backed by `GameScreenChromeTest.noControlShowsAnAllowanceCountBadgeOrAdGlyph`, verified failing when a badge is added. |

### S5 / S6 acceptance criteria — re-verified, not inherited
- `grep -c 'name="how_to_play"' strings.xml` → 0; `grep -rn 'R.string.how_to_play\b' app/src` → 0 — **pass**
- `grep -rn 'Icons.Rounded.Info' app/src/main` → 0 — **pass**
- `grep -rn 'onShowHowToPlay' …/ui/home/` → 0 — **pass**
- `grep -rn 'onboardingVisible\|OnboardingOverlay' app/src` → 0 — **pass**
- `grep -c '_ORDER = ' SettingsScreen.kt` → 11, distinct values → 11 — **pass** (cascade renumbered whole, no duplicate)
- `settings_sound_supporting` contains "upcoming" → 0 — **pass**
- `grep -rn '@Ignore' app/src/test domain/src/test` → 0 — **pass**
- `git diff --diff-filter=D --name-only develop..HEAD -- '*Test.kt'` → empty — **pass** (no test deleted; the two by-design breakages are retargeted)
- `grep -rn 'verticalScroll' …/ui/home/` → 4; `CircularProgressIndicator\|isLoading` → 0; `@Preview` in HomeScreen → 2 — **pass**
- `git diff develop..HEAD -- HomeScreen.kt | grep -c '^-.*challenge_tile_continue'` → 0 — **pass**

### Home's three load-bearing readouts (D-03f) — verified in code **and** on device
S6 claimed all three are preserved. Checked rather than inherited:
1. **Challenge tile level chip** — `HomeScreen.kt:520` renders `challenge_tile_continue` from
   `state.challengeCurrentLevel`; `challengeTileShowsThePersistedLevelAndEntersAtIt` and
   `theLevelPillAndTheChallengeTileShowTheSameNumber` assert it. On device: `Continue — Level 26`.
2. **One tap to the Classic catalogue's star pips** — `ClassicTile(onClick = onPlayClassic)`;
   `git diff develop..HEAD --stat -- app/src/main/java/com/app/pegsolitaire/ui/classic/` is
   **empty**, so `BoardPreview.kt` and `catalog_stars_earned` are byte-untouched. On device:
   Home → Classic reached the catalogue in one tap with `content-desc="3 of 3 stars"` on each card.
3. **Continue chips** — `HomeScreen.kt:315-342`, behaviour/data/ordering unchanged;
   `bothContinueChipsAndBothModeTilesRenderWhenBothResumesExist` asserts it.

### Suppression accounting for the whole branch
`git diff develop..HEAD -- app/src domain/src | grep -E '^[-+].*@[a-zA-Z:]*Suppress'`:
- **+1** `@Suppress("TooManyFunctions")` on the `ProgressRepository` **interface** (S2, already
  flagged in `android-game-dev-s1-s4.md` — awaits the code-auditor; not mine and not ratified here)
- **−1** `@file:Suppress("TooManyFunctions")` on `GameScreen.kt` (S7, mine — the three deleted
  functions put the file back under detekt's threshold, so the suppression was deleted with them)
- **±0** the `@Suppress("LongMethod")` on `GameScreen` — comment text updated, not added
- The two new vectors add **zero**
**Net: 0.** No baseline entry, no `@Ignore`, no disabled test, no `!!` added anywhere.

### Phase-wide invariants
- `git diff develop..HEAD --name-only -- VERSION app/schemas/ .planning/approvals/` → **empty**. `VERSION` still reads `3.0.0`.
- `--offline` green throughout → no dependency was added. No `THIRD_PARTY.md` row is owed (§12.6): the two new vectors are in-house, no asset and no library was added.

### C1 cold-start measurement — **NOT RUN**
Full detail in `15-BUILD-EVIDENCE.md`. Attempted in full, abandoned at calibration:

| Step | Result |
|---|---|
| AVD | `Pixel_9a` booted headless, `ro.build.version.sdk` = 36 — the named instrument |
| Install | `ANDROID_SERIAL=emulator-5566 ./gradlew :app:installDebug --offline` → BUILD SUCCESSFUL 19s |
| Component | resolved from the device: `com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity` |
| Seed | 204 rows / 200 Challenge / 413 stars, confirmed on screen (`Stars, 413` · `Boards, 4/5` · `Level, 26`) |
| Probe 1 (arm B build) | min 885 · median **957** · max 1057 · spread **172** |
| Probe 2 (same build, unchanged) | min 971 · median **1014** · max 1068 · spread **97** |

Two runs of **one build against itself** is a null experiment whose true delta is zero. It
reported **57 ms** — larger than the 50 ms the gate must decide — with spreads of 172 ms and
97 ms against the 46 ms Phase 6 calibrated the threshold from. The instrument cannot resolve the
question, so neither probe is reported as arm B, arm A was never built, and **no delta is
claimed**. The host was shared with three other in-flight agent sessions (load 5.4–7.2 on 10
cores); none of those processes were mine to stop, and the second emulator (up 4 days) may hold
another role's device evidence, so it was left running. The `Pixel_9a` I booted was shut down.

## Deviations from the plan

1. **§S7's "`grep -c '@Suppress'` … returns 1 each" for the two vectors — built at 0, not 1.**
   The same criterion also says "matching `UndoIcon.kt` verbatim in shape", and `UndoIcon.kt`
   carries **no** suppression: it writes its path as a string constant, where `ModeGlyphs.kt`'s
   builder-style glyphs need one. The draft followed `UndoIcon.kt`; I kept that. Fewer
   suppressions is the direction §0.3 wants, and the two halves of the criterion contradict
   each other.
2. **§S7's "`grep -rn 'top_bar_touch_target' app/src/main` → 0 hits" is not satisfiable at S7.**
   The token's own **declaration** is a hit and must survive until S8 deletes it. Read as
   intended (no *caller* left) it passes: `grep -rn 'R.dimen.top_bar_touch_target' app/src/main`
   → 0 after S7. S8's `app/src`-wide 0-hit gate does pass, once the two prose mentions were
   reworded.
3. **§10's P2, P9 and P10 need the comment filter §10 gives only to P8.** Table above. No code
   change was made to satisfy a grep — in particular `TutorialViewModel.kt:296`'s "undo" is a
   quotation of `ux-spec.md:896` and stays.
4. **S6 used a fixed `Spacer`, not the weighted spacer §S6 specifies.** Found while verifying
   S6, reported not fixed — see Open issues.
5. **`TextOverflow.Ellipsis` added to the status-strip title** (not in §4.3, which specifies only
   `maxLines = 1`). With the default `Clip` a long title truncates mid-glyph; the draft's own
   KDoc already claimed it "truncates".
6. **The two control tests were extended with different assertions than §S7 sketched.**
   `UndoControlTest` gained a both-states-labelled test and `HintControlTest` a
   second-tap-cannot-start-a-second-search test, because the `enabled = canUndo` and
   spinner/disabled halves of criteria 22/23 were already asserted by the three existing tests.

## What I inherited versus what I wrote
The uncommitted S7 draft (5 new files, ~309 changed lines) was another agent's, unverified by any
`check`. I treated it as a draft, not as my own finished work.

- **Inherited and kept**: `GameStatusStrip.kt`, `GameActionDock.kt`, `HintIcon.kt`,
  `ShowMovesIcon.kt`, the `GameScreen.kt` recomposition, all four control reskins, the six dimens,
  and the two control tests' bodies. The design is sound and matches §4.3/§4.4 closely; the
  `DockActionFilledKey` idea in particular is the right answer to "a fill is a pixel fact no
  ordinary assertion can reach".
- **Wrote / changed**:
  - **Fixed a real defect** in `GameScreenChromeTest`: it wrote to a `MutableState` **during
    composition** (`hints.value = showHints` inside the composable) to flip the toggle between
    the ON and OFF halves of `showMovesCarriesItsStateByFillAndNotByTintAlone`. Whether that
    works at all depends on which lambda's restart scope happens to own the read — a backwards
    write. Replaced with state hoisted outside the composition and written only from the test
    thread, plus a `setShowHints` helper; `mutableStateOf`/`Composable` moved from inline
    fully-qualified names into imports.
  - Added `TextOverflow.Ellipsis` to both title branches (deviation 5).
  - **Verified every new assertion by breaking its mechanism**, which the draft had not done:
    `filled = showHints` → `false` broke `showMovesCarriesItsStateByFillAndNotByTintAlone`; a
    `Text("x3")` in the dock broke `noControlShowsAnAllowanceCountBadgeOrAdGlyph`; moving
    `RestartControl` into the strip broke `allFourActionsLiveInTheActionDock` **and**
    `theGameplayScreenHasNoMaterialTopAppBar`. All three restored and re-verified.
  - All of S8: both token retirements, the two prose rewordings, the stale `HomeScreen` KDoc,
    `15-BUILD-EVIDENCE.md`, and the measurement attempt.

## Which §14 criteria each slice satisfies

| Slice | Criteria satisfied | Proof |
|---|---|---|
| **S5** (inherited, re-verified) | 8, 9, 10 | `HomeScreenTest` (no help icon; the retargeted never-stacked test), `SettingsScreenTest.theGeneralSectionOpensWithAHowToPlayRowAboveTheSoundToggle`, `ButtonSoundTest.theSettingsHowToPlayRow…`, `HomeViewModelTest.theTutorialIsOfferedOnceAndNeverAgain…` |
| **S6** (inherited, re-verified) | 11, 12, 13, 13a, 13b, 15b, 15c, 15d | the 7 new `HomeScreenTest` tests; `ui/classic/` byte-untouched; pills carry no click action |
| **S7** (mine to finish) | 16, 17, 18, 19, 20, 21, 22, 23, 24 | `GameScreenChromeTest` ×5, `UndoControlTest` ×3, `HintControlTest` ×3, and `ButtonSoundTest`/`GameFlowTest` passing **byte-unedited** (the empty test diff is criteria 17/18's proof) |
| **S8** (mine) | — | token retirement is QUAL-03 hygiene; 15e is NOT RUN, 15f not settled |

## Criteria only a device session can settle — the qa-engineer's list
None of these is claimed by any test or by me. Criteria 1–7e belong to S1–S4 and are listed for
completeness of the device pass.

| Criterion | What must be observed | Why no unit test reaches it |
|---|---|---|
| **15e** | The two-arm cold-start delta | **NOT RUN.** Needs a quiet host; run the null-experiment precheck in `15-BUILD-EVIDENCE.md` first and stop if it fails |
| **15f** | On **first paint**, before data loads: `—` placeholders, no spinner, no blank band, and the fill causes no layout shift | A unit test can render a null state but cannot watch the first frame of a real cold start |
| **15a** | 360×640dp, default font scale, **both** resumes saved: Continue chips **and** both mode tiles fully visible without scrolling | Robolectric's viewport is 320×470px; the fold is a device fact |
| **15** | On a 6.7-inch phone the two mode tiles sit in the lower half | See Open issue 1 — I observed them at ~36–54% of height with no resume saved |
| **14** | Max system font size + both Continue chips: whole screen reachable by scrolling, nothing clipped | Font-scale layout |
| **19** (half) | All four dock actions in the **lower half** of a real screen | I measured the dock at ~88% height on Pixel_9a; confirm on a 6.7-inch device |
| **25** | OS animations off: every refreshed screen usable, nothing stuck mid-animation | Animation-scale behaviour |
| **7c** | Every completed tutorial jump sounds, **including one pass with OS animations off** | `runJumpAnimation` takes a snap path when animations are off (`BoardCanvas.kt:298-304`); whether `JumpSoundEffect`'s `LaunchedEffect` wins that race is a device fact, not a reading |
| **7a** | On a player with a saved Challenge game and stars: running **and** skipping the tutorial leaves the Stars pill, both Continue chips and the Challenge tile level **exactly** as they were | The D-03f oracles; the destructive failure is in the database, not on screen |
| **7b, 7d, 7e, 3, 3a, 4, 5, 6** | Tutorial behaviour, silences and exits on a real device | — |
| **26** | TalkBack: every new element ≥48dp, ≥8dp apart, meaningful label, light and dark | See Open issue 2 — needs a real TalkBack pass, not `uiautomator` |
| **27** | 4.5:1 / 3:1 contrast on every new text/glyph pair, light and dark | Measurement, not assertion |
| **28** | No screen uses sound as its only feedback | — |

## Open issues / risks

1. **S6's fixed spacer leaves ~45% of Home empty below the tiles (criterion 15 risk).**
   §S6 specifies "a **weighted** spacer between bands 2 and 3 so slack goes *above* the Continue
   chips and the tiles stay in the thumb zone". `HomeBands` uses a fixed
   `Spacer(Modifier.height(spacing_lg))`; its KDoc gives the correct reason (`Modifier.weight`
   does not work inside a `verticalScroll` column) but the consequence is that all slack falls to
   the bottom. On `Pixel_9a` with no resume saved the tiles occupy ~36–54% of screen height.
   **Reported, not fixed**: it is a layout judgement whose acceptance criteria (15, 15a) are
   device checks, no unit test can verify the fix, and pushing tiles down moves criterion 15a in
   the opposite direction on a small screen. Recipe if dispatched:
   `BoxWithConstraints { Column(Modifier.verticalScroll(s).heightIn(min = maxHeight)) { … Spacer(Modifier.weight(1f)) … } }`.
2. **Pre-existing, app-wide: the clickable node and the labelled node are separate in
   `uiautomator`'s projection.** Each dock action's clickable node is 56dp with no label; the
   label sits on a 28dp `clickable="false"` child. The **untouched** Classic catalogue's
   `TopAppBar` back arrow shows the identical split at 24dp, so this is how this Compose version
   maps `IconButton { Icon(contentDescription) }` into the accessibility tree — **not introduced
   by S7**, whose clickable target in fact grew from 48dp to 56dp. Compose's merged tree is
   correct (that is why `ButtonSoundTest` clicks these by description and passes unedited).
   Criterion 26 still needs a real TalkBack pass.
3. **One suppression awaits the code-auditor** (S2's, already flagged): `@Suppress("TooManyFunctions")`
   on the `ProgressRepository` interface. I added none and removed one; I did not ratify it.
4. **`ButtonSoundTest.theGameTopBarBackIconAndHintToggleMakeNoSound` now has a stale name** — there
   is no top bar. §S7 requires that file's diff to be **empty** as the proof that no content
   description drifted, and that proof is worth more than the name. A rename belongs in a later
   commit, not this one.
5. **Both C1 probe medians (957, 1014 ms) sit at or above the 950 ms absolute bar.** Recorded in
   `15-BUILD-EVIDENCE.md` explicitly as a **hypothesis, not a finding** — the numbers are
   contaminated. If it survives on a quiet host, §11.2 makes a condition-(1)-only failure
   PLAT-08 / Phase-14's and an escalation to the producer, not this refresh's to pay for.
6. **`15-PLAN.md` §12.4 lists `BoardPreview.kt` under `ui/components/`; it is at `ui/classic/`.**
   Harmless, but a reviewer greping the stated path finds nothing.
7. **The plan's §11.1 seeding fallback is incomplete** and would have produced a corrupt push:
   it pulls only `databases/<db>`, which on a WAL-journalled Room database is a 4 KB stub with no
   tables. The `-wal` file must be pulled too. Corrected procedure is in `15-BUILD-EVIDENCE.md`.

## Handoff
Next: **code-auditor** — review `542292f` and `57b2ad5` against `docs/ux-spec.md` §4.3, §4.4,
§5.1 and `15-PLAN.md` §10. The review contract is the ten prohibitions (use the corrected,
comment-filtered forms in the Checks table above), the suppression accounting, and the six
deviations. Two decisions are yours, not mine: S2's `@Suppress("TooManyFunctions")` on the
`ProgressRepository` interface (still unratified), and whether §S7's "one `@Suppress` per vector"
criterion should be read as satisfied at zero.

Then: **qa-engineer** — the device session. Work from "Criteria only a device session can settle"
above. Two things are owed specifically to you: (a) **criterion 15e is NOT RUN** and
`15-BUILD-EVIDENCE.md` hands you the full procedure with three corrections this attempt found
(the resolved-component form, no `sqlite3` on the API-36 image, and the missing `-wal` pull) plus
a null-experiment precheck that costs eight minutes and stops a bad number at the door; (b) Open
issue 1, Home's empty lower band, is the one thing I found and deliberately did not fix.

Then: **tech-lead** — sign-off. Not merged, not pushed; the phase is not marked complete.
