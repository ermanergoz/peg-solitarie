# android-game-dev report — phase-15 H-2: F-2 light status bar and G-1 coach threshold (fix mode)
Date: 2026-10-18
Branch: feature/phase-15-ux-refresh (a62194f..3b27012)
Skills loaded: ergoz-soft:standards (role android-game-dev). The product's rules snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin's copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing. Also the product's `.claude/rules/lessons.md` and the plugin's `memory/LESSONS.md`. Then superpowers:test-driven-development (with its `writing-good-tests.md`), gitflow, android-testing, android-viewmodel, compose-skill, compose-ui.
Ran in parallel with: phase-14 R-1 tech-lead sign-off (another worktree), candy-backdrop design directions (docs only)
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons, and the plugin's `memory/LESSONS.md` holds none)

## Summary
- **F-2 is fixed in the window theme only.** `res/values/themes.xml` sets `android:windowLightStatusBar` to true on `Theme.PegSolitaire`. A new `res/values-night/themes.xml`, with the same style name and parent, sets only that item, to false. Nothing else changed, as Decision 3 requires.
- **`WindowThemeTest`** (Robolectric) resolves the style under `notnight` and `night` and reads the attribute. It went red in two stages before the XML was complete: both tests with no XML, then the dark one with only `values/`.
- **G-1:** a new `TutorialViewModelTest` case asserts that the third consecutive non-progressing tap raises the coach and the second does not. It was red with the constant at 2 and at 4, and green once restored from git.
- **The Check is green on `3b27012`:** `BUILD SUCCESSFUL in 11m 12s`. `:app` ran 1,064 tests, 0 failed and 0 skipped (loop 3 had 1,061; the 3 new tests account for the difference). The depth sweep passed. No suppression was added.

## Outputs
- `app/src/main/res/values/themes.xml` — `android:windowLightStatusBar` true on `Theme.PegSolitaire`. The comment says why the window theme sets it rather than Compose.
- `app/src/main/res/values-night/themes.xml` — new. Same style name and parent, one item: `android:windowLightStatusBar` false.
- `app/src/test/java/com/app/pegsolitaire/ui/theme/WindowThemeTest.kt` — new, two cases:
  - `theLightWindowThemeAsksForDarkStatusBarIcons`;
  - `theDarkWindowThemeAsksForLightStatusBarIcons`.
  - Each read defaults to the wrong answer, so an attribute that no theme in the chain sets cannot pass.
- `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialViewModelTest.kt` — new case `theThirdConsecutiveNonProgressingTapRaisesTheCoachAndTheSecondDoesNot`, plus the companion constants `NO_ESCALATION`, `T2_COACHED_DOT` and `THRESHOLD_IN_USE`.
  - On T2 with nothing held, it taps three empty holes.
  - After the second tap: `coachTick` 0, no ring, no dot.
  - After the third: `coachTick` 1, the ring on (0,1), the dot on (2,1).
  - The count is typed from criterion 4 and never read from the constant. The constant appears in the KDoc and in the failure messages, so a red run names the knob that moved.
- Commits, each body carrying its red-then-green record:
  - `a62194f` fix(15-h2): draw dark status-bar icons in the light theme (F-2)
  - `3b27012` test(15-h2): pin criterion 4's third-tap coach threshold (G-1)
- This report, in its own commit.
- **Unchanged, as Decision 3 requires:** `MainActivity.kt`, `Theme.kt`, every composable, the navigation bar, the window background and the parent theme. Nothing under `app/src/main/java`, in the manifest or in `:domain` changed.

## Checks
All commands ran from `/Users/ermanergoz/Desktop/PegSolitarie-p15`, prefixed with `ANDROID_HOME=$HOME/Library/Android/sdk`. Failure messages were read from `app/build/test-results/testDebugUnitTest/TEST-<class>.xml`.

### F-2: red, then green
Each run: `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.theme.WindowThemeTest' --rerun`.
1. **Test only, no XML change** — fail, 2 of 2:
   - light: "value of: windowLightStatusBar(...) / expected to be true". It read false.
   - dark: "value of: windowLightStatusBar(...) / expected to be false". It read true, which is the deliberately wrong default, so nothing in the parent chain sets the attribute. The device's white icons rule out an inherited true.
2. **`values/themes.xml` only** — fail, 1 of 2. The light test passes. The dark test fails, "expected to be false", because the night window inherited true. This is the case the dark test exists for: a missing or emptied `values-night` file.
3. **Both files** — pass, 2 of 2.

### G-1: red, then green
Each run: `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.TutorialViewModelTest.theThirdConsecutiveNonProgressingTapRaisesTheCoachAndTheSecondDoesNot' --rerun`. The constant was edited with `sed` in `TutorialAnim.kt` and restored with `git checkout --`.

The behaviour already ships, so this test-only gap shows its red by mutation, as the sign-off asks:
1. **At 3 (shipped)** — pass.
2. **At 2** — fail: "the coach after the second tap, with TutorialAnim.NON_PROGRESSING_TAPS_BEFORE_COACH = 2; criterion 4 asks for the third tap / expected: 0 / but was : 1".
3. **At 4** — fail: "the coach after the third tap, with TutorialAnim.NON_PROGRESSING_TAPS_BEFORE_COACH = 4; criterion 4 asks for the third tap / expected: 1 / but was : 0".
4. **Restored** with `git checkout`, `git diff --quiet` clean — pass.

The sequence ran twice. The first version asserted only the tick and the ring. I then added the destination dot, because §4.4 defines the coach as the ring and the dot, and re-ran the whole sequence on the final version, with the same results.

### Static checks, the suite, the Check
- `./gradlew :app:ktlintCheck :app:detekt` — pass, after each commit's edits.
- `./gradlew :app:lintDebug` — pass. No issue in `app/build/reports/lint-results-debug.xml` names either `themes.xml`.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.TutorialViewModelTest' --rerun` — pass, 14 of 14.
- `./gradlew :app:ktlintTestSourceSetCheck --rerun :app:detekt --rerun` — pass. See Open issues 4.
- `cd /Users/ermanergoz/Desktop/PegSolitarie-p15 && ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon check` on `3b27012`, the dispatch's exact command — **pass**: exit 0, `BUILD SUCCESSFUL in 11m 12s`, 98 actionable tasks (13 executed, 85 up-to-date). It ran in the background because `:domain:depthSweep` (`outputs.upToDateWhen { false }`) alone outlasts one 10-minute call. I waited on its Gradle process and ran no other build meanwhile. Before it started, no other Gradle process was running on the host.
  - `:app:testDebugUnitTest` executed: 99 classes, 1,064 tests, 0 failures, 0 errors, 0 skipped. Of these, `WindowThemeTest` is 2 of 2 and `TutorialViewModelTest` 14 of 14.
  - `:domain:depthSweep` executed: 1 of 1 passed.
  - `:app:lintDebug` executed: pass.
  - The `dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck` and `audioFocusLiteralCheck` gates executed: pass.
  - `:domain:test` was UP-TO-DATE: 274 tests, 0 failures, on inputs unchanged since its last run (2026-10-17 20:37). H-2 touches nothing in `:domain`, and `build/fuzz-failures.txt` is absent.
  - `detekt` and `ktlintCheck` were UP-TO-DATE: their last runs, above, were on this same tree. `:app:detekt` and the test-source ktlint check were forced with `--rerun`.
- ktlint probe, after the Check:
  - `printf 'private const val KTLINT_PROBE=0\n' >> app/src/test/java/com/app/pegsolitaire/ui/theme/WindowThemeTest.kt && ./gradlew :app:ktlintTestSourceSetCheck` — failed, as intended: `WindowThemeTest.kt:77:31 Missing spacing around "="`.
  - Then `git checkout -- app/src/test/java/com/app/pegsolitaire/ui/theme/WindowThemeTest.kt && ./gradlew :app:ktlintTestSourceSetCheck` — pass. `app/` and `domain/` match HEAD afterwards.
- `git diff --stat 04b6785..3b27012` — 4 files: the two `themes.xml` and the two tests. The same diff restricted to `app/src/main/java`, `app/src/main/AndroidManifest.xml` and `domain` is empty.
- `git diff 04b6785..3b27012 | grep '^+' | grep -c -E '@Suppress|@Ignore|baseline|ktlint-disable|detekt:disable'` — 0.
- `git grep -n NON_PROGRESSING_TAPS_BEFORE_COACH -- app/src/test` — 3 lines (0 before): the KDoc link and the failure-message constant.

## Open issues / risks
1. **The device check is owed (H-3).** The JVM test proves what the theme asks for; drawing the icons is the platform's job. QA confirms on `Pixel_9a_bench`, in light and dark, on Home, gameplay, the catalogue, Results, Settings and the tutorial, and confirms the navigation bar unchanged in gesture and three-button modes.
2. **Robolectric emulates SDK 34; the device runs 36.** Resolving this attribute through the theme does not depend on the SDK, but how API 36 draws the bar edge to edge is the device's to show (H-3).
3. **How G-1 references the constant.** The test names the constant in its KDoc and its failure messages, and no assertion reads it, by design. Asserting its value would be a change detector (it fails only when someone changes the number on purpose), and tapping "the constant" times would follow the constant. The behaviour assertions are what turned red at 2 and at 4. If the auditor reads "references the constant" as an assertion on its value, that is one line to add.
4. **One unexplained ktlint UP-TO-DATE, not reproduced.** Once, `:app:ktlintTestSourceSetCheck` reported UP-TO-DATE after I had edited `TutorialViewModelTest.kt` since its last run. The compile task in the same daemon had seen that edit. So I forced the check with `--rerun`, and it passed on the final tree. After the Check, I planted a violation in a test file and ran the task without `--rerun`. It executed and failed (`WindowThemeTest.kt:77:31 Missing spacing around "="`); restored from git, it passed. The gate catches a change, so this is a note only, for whoever next sees the same label.
5. **Commit trailer.** Both commits carry the one trailer the plugin's `rules/gitflow.md` (1.0.0) names, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`. The work ran on Opus 5.5 (see Ran on). The product's 0.5.1 snapshot names no trailer.
6. **15e arm B.** The code head after H-2 is `3b27012`, and this report's commit is records only. If H-1 adds code, arm B moves to that head (Decision 1, P-2).

## Handoff
Next: code-auditor — H-1: audit S10 (`a4d00ab..49db2ff`) together with this H-2 delta (`04b6785..3b27012`). That covers the XML diff, `WindowThemeTest` watched red in two stages, and the G-1 test watched red at 2 and at 4 (both records above, and in the commit bodies).
Then: qa-engineer — H-3 on `Pixel_9a_bench`: the 15e sitting with arm B = `3b27012` (or H-1's code head), then F-2 on the device (open issue 1).
