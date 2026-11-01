# tech-lead report — candy-backdrop: the develop merge-in's two test conflicts, and the 15e baseline
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; read at eee36d0)
Skills loaded: ergoz-soft:standards (role tech-lead; the worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read these plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; and the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/tech-lead.md` pins `model: fable`, `effort: xhigh`. This ran on Opus under the founder's Q-019 answer, as `tech-lead-direction-b.md` did.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **R-1, `ResultsStarPaletteTest`:** render `ResultsScreen` inside `BackdropTestHost(BackdropDensity.Sparse)`. The unearned star's oracle becomes "its centre equals the pixel at the same place in a frame of the same host with the screen taken out", plus two guards. The ux-designer amends no row: CV-AC05 §12.5 already reads "the surface beneath it (the background, the Results sky or the catalogue card)". This is a test-harness matter. It is **amended, not weakened**, but the code-auditor's approval line is still required.
- **R-2, `HomeChipArrivalTest`:** `renderHome` gains an `underlay` slot that is empty by default. This one test draws a window-filling `background` `Surface` under Home, which restores its stated "flat render". It also gains a precondition guard: 0 px of ink before the chip comes. The oracle is unchanged. Probed: no chip, 0 px; P9 red at frame 5 (room 48 dp, 0 px). It is **amended, not weakened**, and the auditor's line is required.
- **R-3, 15e:** confirmed. The sitting runs only after the final merge-in of licences (bc95b2a) and talkback, and arm A = `git merge-base HEAD develop` at the sitting. f1c399a is not arm A. One allowance is set out in R-3.

## Outputs
- `.planning/reports/candy-backdrop/tech-lead-merge-ruling.md` — this ruling. No product or test file was edited on the branch.

## Rulings

### R-1 — `ResultsStarPaletteTest` against BD-14 (developer's Open issue 1)

**What is true.**
- The test composes `ResultsScreen` bare and reads the unearned star's centre against the `colorScheme.background` token.
- That held only while Results' `Scaffold` painted `background`. BD-14 makes the `Scaffold` transparent, and `TransparentContainersTest` pins that. So bare, the centre is `#00000000`.
- Production is not at fault.

**The three options.**

| Option | Verdict | Why |
|---|---|---|
| (a) unedited, inside `BackdropTestHost(Sparse)` | **rejected as it stands** | It passes only by tolerance. My probe reads the Sparse wash under the star as `#fff6f0fa` light and `#ff1b132c` dark, against `background` `#fff4f1fa` / `#ff1b1626`. In dark, the blue channel is 6 off, which is exactly `CHANNEL_TOLERANCE` (6). The assertion would also keep saying "shows the background", which is not what ships. A change to Results' layout or to the wash stops would redden it for no defect |
| (b) inside a `Surface(background)` | rejected | It renders a host that does not ship, the F-32 `Surface` without its scene. That is against candy-backdrop §12's host rule: "a suite that renders a whole screen and reads pixels next to a container renders that screen in `BackdropTestHost`, at the screen's density". It would also not test what CV-AC05 §12.5 names, "the Results sky" |
| (c) "shows what lies beneath it" | **adopted, on top of (a)'s host** | This is CV-AC05 §12.5's own wording. The beneath-pixel comes from the same host at the same pose, so the comparison is exact (probe: identical ARGB in all four cases), not a near-miss on a token |

**The exact edit.** It touches only `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsStarPaletteTest.kt`. My probe of this edit is green 2/2 at eee36d0.

1. **Imports.**
   - Add `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.test.assertCountEquals`, `androidx.compose.ui.test.onAllNodesWithTag`, `com.app.pegsolitaire.testing.BackdropTestHost` and `com.app.pegsolitaire.ui.backdrop.BackdropDensity`.
   - Drop `DarkColors` and `LightColors`; nothing else uses them.
2. **A screen toggle.** Add the field `private val screenShown = mutableStateOf(true)` beside `palette`.
3. **The content.** It becomes:
   ```kotlin
   AppTheme(darkTheme = dark, boardMarkPalette = palette.value) {
       BackdropTestHost(BackdropDensity.Sparse) {
           if (screenShown.value) {
               ResultsScreen(/* the same arguments, unchanged */)
           }
       }
   }
   ```
4. **The reference frame.** After `val stars = layout.starBoxes(starRow().boundsInRoot)`, take the frame without the screen and guard it at the pixel level:
   ```kotlin
   val beneath = frameWithoutTheScreen()
   stars.take(EARNED).forEachIndexed { index, box ->
       assertWithMessage("the frame without the screen: no earned star $index's fill at its centre")
           .that(isBetweenStops(beneath.starPixel(box, STAR_CENTRE_UNITS, STAR_CENTRE_UNITS),
               boardMarksFor(BoardMarkPalette.Standard, dark).earnedStarFill))
           .isFalse()
   }
   ```
5. **The helper.** It carries the semantic guard:
   ```kotlin
   /** The host alone, the screen taken out: what lies beneath every star, at the same pose. */
   private fun frameWithoutTheScreen(): Bitmap {
       composeRule.runOnUiThread { screenShown.value = false }
       composeRule.waitForIdle()
       composeRule.onAllNodesWithTag(RESULTS_STAR_ROW_TEST_TAG, useUnmergedTree = true).assertCountEquals(0)
       return composeRule.renderedWindow()
   }
   ```
6. **`assertTheStars`.**
   - It takes `beneath: Bitmap` after `frame`, and both calls pass it.
   - `val background = …` goes.
   - The unearned centre check becomes:
     ```kotlin
     val under = beneath.starPixel(box, STAR_CENTRE_UNITS, STAR_CENTRE_UNITS)
     assertWithMessage("$shown: the unearned star's centre shows what lies beneath it")
         .that(isColour(centre, Color(under)))
         .isTrue()
     ```
   - The earned-star checks, the band check, `EarnedStarCountKey` and `RowLayout` are unchanged.
7. **The class KDoc, its first bullet.** It reads: "an unearned star is the hollow star, so its centre shows what lies beneath it: on the production host, the Results sky (CV-13a; CV-AC05 as amended, §12.5). The screen is rendered in `BackdropTestHost` at Results' density, Sparse (candy-backdrop §12), and 'beneath' is read from the same host with the screen taken out."

**"Amended, not weakened": yes.**
- A filled unearned star still fails: its centre is `onSurfaceVariant`, not the sky.
- Every other assertion is untouched.
- The two guards close the one new way the test could pass vacuously, a reference frame that still holds the screen.
- **The code-auditor's approval line is required.** This is an edit to colour-vision's suite, and A7.5 and the dispatch rule ("unedited or reported, never retuned") apply until that line is given.

**Reds the developer shows, both themes each:**
- **N1, `BoardMarkRecipe.kt:63` `unearnedStarHollow = false`:** "Standard: the unearned star's centre shows what lies beneath it / expected to be true", 2 of 2. *Probed.*
- **The colour-blind twin, `BoardMarkRecipe.kt:77`:** the same message for Colorblind.
- **N8, the auditor's loop-0 M17,** Results' unearned star back to the pre-branch glyph: red.
- **The semantic guard, `screenShown.value = false` removed:** "Did not expect any node but found '1' node that satisfies: (TestTag = 'results_star_row')", 2 of 2. *Probed.*
- **The pixel guard, the toggle and the `assertCountEquals` both removed:** "the frame without the screen: no earned star 0's fill at its centre".
- Then green 2/2, and the full Check.

**Spec rows: none to amend.**
- CV-AC05 §12.5 (`docs/ux/colour-vision.md:958`) already reads "an unearned star's centre shows the surface beneath it (the background, the Results sky or the catalogue card)".
- candy-backdrop §12 (`docs/ux/candy-backdrop.md:1695`) already gives the host.
- The word "background" was only the test's surrogate. This is a test-harness matter, and nothing goes to the ux-designer.

### R-2 — `HomeChipArrivalTest.aChipThatArrivesAfterFirstPaintFadesInWithItsRoomWithNoStaggerDelay` (developer's Open issue 2)

**What is true.**
- The test's own KDoc says it is "read in the flat render": its oracle counts pixels that are not exactly `LightColors.background`.
- Home's opaque `Scaffold` used to supply that flat field. BD-14 removed it, and `renderHome` draws no host.
- So the gap reads `#00000000`, and every gap pixel counts as ink: 6,080 px with no chip, which is why P9 survives.

**Ruling: restore the flat field under this test only, keep the oracle, add a guard.**

The production host (`BackdropTestHost(Landscape)`) is rejected for this test. Its waves and lights fill the gap, which makes the "not background" oracle vacuous again. It could only work with a second oracle change, and the claim under test is the chip's fade timing, not the scene. Item 1 differs because there the spec itself names the Results sky as what the centre shows.

1. **`fixture/HomeTestFixture.kt`.**
   - Both `renderHome` overloads gain `underlay: @Composable () -> Unit = {}`, passed through like `overlay`.
   - Inside `AppTheme`, `underlay()` is composed **before** `HomeScreen`, mirroring `overlay()` after it.
   - The KDoc's `@param underlay` reads: "drawn under Home in the same composition and theme, before it. Empty by default, so every caller that passes none composes exactly the tree it always did. `HomeChipArrivalTest` draws the flat background Home's opaque `Scaffold` drew before BD-14 here."
   - This is O3's existing scope (the fixture already carries B-19a's additive `overlay`), and the default changes no other suite's tree.
2. **`HomeChipArrivalTest.kt`.**
   - `renderWithInsets(state, fontScale, underlay: @Composable () -> Unit = {})` and `renderWithThePausedClock(underlay: @Composable () -> Unit = {})` pass the slot through.
   - Only `aChipThatArrives…` passes it, as `renderWithThePausedClock(underlay = { FlatBackground() })`. The private composable is `@Composable private fun FlatBackground() = Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}`.
   - Right after `val noChip = readFrame()`, add the precondition guard:
     ```kotlin
     assertWithMessage("no chip yet: px drawn between the ribbon and the first tile")
         .that(inkBetween(bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)).bottom, noChip.classicTile))
         .isEqualTo(0)
     ```
   - The KDoc's last sentence gains: "on a window-filling `background` surface drawn under Home, as its opaque `Scaffold` drew before BD-14, and with the gap checked empty before the chip comes, so the read cannot pass on an empty room."
   - `inkBetween`, `INKED_ROOM_DP`, `FRAMES_READ` and every other test in the class are unchanged.

**My probe** (scratch worktree at eee36d0, the underlay as a `Surface` composed before `HomeScreen`, which is the form ruled above):
- **Unmutated:** 13 of 13 pass. No chip: 0 px. Frames 1–2: room 0 dp, 0 px. Frame 3: 21 dp, 2,079 px. Frame 5: 48 dp, 11,845 px. Settled: 96 dp, 29,772 px.
- **P9** (`HomeScreen.kt:490`, `entranceOrder = FIRST_CHIP_ENTRANCE_ORDER` always): 1 of 13 fails, "the Classic chip arriving, frame 5, room 48.0.dp: px drawn in it / expected to be greater than: 0 / but was: 0". Frames 3–5 read 0 px while the room opens.

**"Amended, not weakened": yes.** The oracle is unchanged, the flat field the test was written against is restored, and the new guard makes the vacuous pass impossible. **The code-auditor's approval line is required.** HomeChipArrivalTest is not in BD-19's list, but A7.5 holds for every suite this branch did not author.

**Reds the developer shows:**
- **P9 under the amended test:** the message above. The other 12 stay green.
- **The guard, `underlay` removed from the one call:** "no chip yet: px drawn between the ribbon and the first tile / expected: 0 / but was: 6080". So removing the underlay can no longer go unseen.
- Then 13/13, and every `renderHome` suite green with no diff in its tree: `HomeLayoutTest` 19, `HomeStageTest`, `HomeScreenTest`, `HomeHeroRowTest`, `HomeRibbonShapeTest`, `GelTileTest`, `ButtonSoundTest` and `BoardCanvasFirstFrameTest`.

**Spec rows:** none. This is a test-harness matter.

### R-3 — Candy's 15e baseline (developer's Open issue 3)

1. **8fac490 changed start-path files,** per tech design §10's list: `MainActivity.kt`, `ui/home/` and `ui/theme/`. Any arm A before it is dead. No sitting has run on this branch, so nothing is voided.
2. **f1c399a is not arm A.** Arm A is `git merge-base HEAD develop` **at the sitting**, so A..B is exactly the branch's diff (§10).
3. **The licences merge-in is mandatory before the sitting.**
   - bc95b2a changes `navigation/` (`AppNavHost.kt`, `LicensesDestinations.kt`, `Routes.kt`) and `di/` (`DataModule.kt`, `DispatchersModule.kt`). Both are start-path directories, so a sitting before it would be re-sat (A11).
   - `git merge-tree` predicts two conflicts: `navigation/AppNavHost.kt` and `docs/ux/licences-screen.md`.
   - **`docs/ux/licences-screen.md`:** take develop's copy. The branch's blob `9aa3437` is byte-identical to develop's 0bf578d, the A9 carry-over, and develop's copy descends from it.
   - **`AppNavHost.kt`:** the branch's host (the F-32 `Surface`, `NavBackdrop`, the density function, the input observer) with licences' destinations added. The licence routes take the function's `else`, Sparse.
   - **E7 falls to this branch, since licences landed first.** The licence `Scaffold`s and the shared `LicensesTopBar` become transparent, as BD-14 does elsewhere. There are four screens now, not E7's two: `LicensesScreen`, `LicenseTextScreen`, `GoogleNoticesScreen` and `GoogleNoticeTextScreen`. All four are added to `BackdropDensityTest` (Sparse) and `TransparentContainersTest`.
   - A licences suite that reads pixels next to a container then follows candy §12's host rule, as in R-1. A grep of bc95b2a's licences and settings tests finds no `background` read today.
4. **Talkback.** As of `feature/talkback-board` 01cb475, its diff from its merge-base 305f741 touches no start-path file and adds no Room entity, database version or DataStore key.
   - **Confirmed: the sitting runs only after the final merge-in** (licences, then talkback once it is on `develop`), with arm A = `develop` as merged at that time.
   - **Allowance, so 3.0.0 is not held on talkback's timing.** If talkback is not on `develop` when the branch is otherwise ready (Check green, auditor PASS, QA's device rows), the sitting may run after the licences merge-in alone. A later talkback merge-in then leaves it standing under §10's rule, if and only if two commands, run on the merge-in, print nothing:
     - `git diff --name-only <arm A> <talkback merge> | grep -E '(App\.kt|MainActivity\.kt|/di/|/navigation/|/ui/home/|/ui/theme/|res/font/|res/values(-night)?(-v27)?/themes\.xml|AndroidManifest\.xml)'`
     - the same diff grepped for `PreferencesKey|@Entity|@Database`
   - Otherwise it re-sits. The producer picks.
5. **Validity.** colour-vision's DataStore key `colorblind_palette_on` is in `develop` since 1bc442f, so both arms share it. The sitting is valid on keys and schema.
6. **The instrument stands:** `Pixel_9a_bench`, the host quiet, no Gradle anywhere (§10, A6), then the separate jank block (R3).

## Checks
- `uptime` before each Gradle run: 1-minute load 3.17–3.45 (bound 12).
- `git worktree add --detach <scratchpad>/tl-probe eee36d0`: pass. I copied `local.properties` in. Every edit was in the scratch worktree only. It was removed with `git worktree remove --force` and pruned. `git -C …/PegSolitarie-candy status --porcelain` printed nothing.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeChipArrivalTest'` with the R-2 underlay and a `println` probe: pass, 13/0. The ink reads are quoted in R-2.
- The same with P9 applied: **fail** as intended, 13 tests / 1 failure, frame 5's message as quoted.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.results.ResultsStarPaletteTest'` with R-1's edit and a probe: pass, 2/0. Centre equals beneath in all four cases: `ff1b132c` dark and `fff6f0fa` light.
- The same with N1 (`BoardMarkRecipe.kt:63`): **fail** as intended, 2/2, "Standard: the unearned star's centre shows what lies beneath it".
- The same with the toggle removed: **fail** as intended, 2/2, at `assertCountEquals(0)`.
- `git merge-tree --write-tree --name-only HEAD bc95b2a`: conflicts in `navigation/AppNavHost.kt` and `docs/ux/licences-screen.md`.
- `git diff --name-only f1c399a bc95b2a`: start-path files `di/DataModule.kt`, `di/DispatchersModule.kt`, `navigation/AppNavHost.kt`, `navigation/LicensesDestinations.kt` and `navigation/Routes.kt`.
- `git diff --name-only 305f741 01cb475` grepped for start-path files, and `git diff 305f741 01cb475` grepped for added preference keys, entities or databases: no match.
- `git rev-parse HEAD:docs/ux/licences-screen.md` = `9aa3437…`, identical to `0bf578d:docs/ux/licences-screen.md`.
- Sources read: `android-game-dev-merge-develop.md`, `tech-lead-direction-b.md` (A6, A7, A9, A11, E7, the file table), `docs/ux/candy-backdrop.md` (B-09, §12, BD-14, BD-19), `docs/ux/colour-vision.md` (CV-AC05, §12.5), `.planning/reports/colour-vision/code-auditor.md` (N1, N7, N8), `.planning/reports/home-chip-arrival/code-auditor.md` (P9), and tech design §10's 15e row.

## Open issues / risks
1. **Two approval lines are owed** before either edit counts: one for R-1 and one for R-2. The auditor re-applies at least N1 and the guard red (R-1) and P9 and the guard red (R-2) on the developer's commits. My probes are evidence for this ruling, not the developer's reds.
2. **The licences merge-in is real work, not a formality** (R-3.3). It means four transparent `Scaffold`s, the top bar, and two test tables. It needs its own Check, auditor and QA pass before the sitting.
3. **Commit trailer.** This report's commit carries `rules/gitflow.md` v1.0.0's single `Co-Authored-By: Claude Fable 5.1` trailer, though it ran on Opus. That is the same open question as the developer's Open issue 5, for the producer to settle.
4. **The developer's report has one small error.** It counts `values-v35` and `values-night-v35` `themes.xml` among the start-path files. §10 lists `values/`, `values-night/`, `values-v27/` and `values-night-v27/` only. It does not matter here: `MainActivity.kt`, `ui/home/` and `ui/theme/` changed anyway.

## Handoff
Next: producer —
1. **android-game-dev:** apply R-1 and R-2 exactly as ruled, show the listed reds, run the full Check, and report.
2. **code-auditor (loop 2, with its existing list):** the R-1 and R-2 diffs and their reds, and an "amended, not weakened" line for each, or a refusal.
3. **android-game-dev:** the licences merge-in (R-3.3, E7 extended to four screens), then talkback's when it is on `develop`, or under R-3.4's allowance.
4. **qa-engineer:** 15e on `Pixel_9a_bench` after the final merge-in, arm A = `git merge-base HEAD develop` at the sitting; then the jank block.
5. **tech-lead:** sign-off after the auditor and QA reports.
