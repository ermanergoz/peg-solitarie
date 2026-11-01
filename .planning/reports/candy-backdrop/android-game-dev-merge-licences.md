# android-game-dev report — candy-backdrop: the ruled test edits, and the licences merge-in (E7)
Date: 2026-11-01
Branch: feature/candy-backdrop (bd8abf5..53911b9, on 1c98da9; 3752063 is the `--no-ff` merge of `develop` at bc95b2a)
Skills loaded: ergoz-soft:standards (role android-game-dev). The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read these plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing, and the product's `.claude/rules/lessons.md`. Other skills: superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **R-1 and R-2 applied as ruled**, each in its own commit with its reds: `ResultsStarPaletteTest` renders in `BackdropTestHost(Sparse)` and reads the unearned star against a frame of the same host with the screen hidden (bd8abf5); `renderHome` gains an empty-by-default `underlay`, filled only by the late-chip test, with the 0 px guard (c1e8c50).
- **Merged `develop` at bc95b2a** as 3752063 (`--no-ff`). Two conflicts, both resolved as R-3.3 rules. Talkback-board is not on `develop` and was not merged.
- **E7 / LS-AC08 built test-first:** the four licences `Scaffold`s and the shared `LicensesTopBar` are transparent (d0a65ae), the four routes are pinned Sparse (c7efee8), and the seven licences previews go through `BackdropPreviewHost` (53911b9, BD-25).
- **One finding on R-1's pixel guard:** as ruled it bites in dark only (1 of 2); the semantic guard bites 2 of 2 (Open issue 1).
- **Check: green.** `./scripts/check.sh --wait --max-workers=4` on 53911b9 ran "BUILD SUCCESSFUL in 11m 10s", with 1,528 app tests and 0 failures.

## Outputs
Commits (the trailer is `Co-Authored-By: Claude Fable 5.1`, per plugin rules/gitflow.md v1.0.0; see Open issue 5):

| Commit | Kind | What |
|---|---|---|
| bd8abf5 | test | R-1: `ResultsStarPaletteTest` in `BackdropTestHost(Sparse)`, the beneath-frame oracle and its two guards |
| c1e8c50 | test | R-2: `renderHome`'s `underlay` slot; the late-chip test's flat field and 0 px guard |
| 3752063 | merge | `develop` bc95b2a in, with the resolutions below |
| d0a65ae | feat | E7: the four licences `Scaffold`s and `LicensesTopBar` transparent; their four rows in `TransparentContainersTest` |
| c7efee8 | test | E7: the four licences routes Sparse in `BackdropDensityTest`, with stand-ins in `AppNavHostWithStandIns` |
| 53911b9 | fix | BD-25: the licences previews through `BackdropPreviewHost(Sparse)` |

Files written (paths under `app/src/`):
- `test/java/com/app/pegsolitaire/ui/results/ResultsStarPaletteTest.kt` — R-1, exactly the ruling's seven steps.
- `test/java/com/app/pegsolitaire/ui/home/fixture/HomeTestFixture.kt` — R-2 item 1: both `renderHome` overloads take `underlay`, composed before `HomeScreen`, with the ruled `@param` KDoc.
- `test/java/com/app/pegsolitaire/ui/home/HomeChipArrivalTest.kt` — R-2 item 2: `renderWithInsets` and `renderWithThePausedClock` pass the slot through; only `aChipThatArrivesAfterFirstPaintFadesInWithItsRoomWithNoStaggerDelay` passes `{ FlatBackground() }`; the guard and the KDoc sentence as ruled. One form change: the ribbon's bottom is read into a local `ribbonBottom` before the guard, because the ruled one-liner is 122 characters and ktlint caps lines at 120. The read is the same.
- `main/java/com/app/pegsolitaire/navigation/AppNavHost.kt` — the merge resolution (below).
- `main/java/com/app/pegsolitaire/ui/licenses/LicensesScreen.kt`, `LicenseTextScreen.kt`, `GoogleNoticesScreen.kt`, `GoogleNoticeTextScreen.kt` — `containerColor = Color.Transparent`, `contentColor = onBackground` on each `Scaffold` (R1's rule, as Settings and the catalogue have it); previews through the host.
- `main/java/com/app/pegsolitaire/ui/licenses/LicensesTopBar.kt` — `TopAppBarDefaults.topAppBarColors(containerColor = Transparent, scrolledContainerColor = Transparent)`, as every other top bar on the branch.
- `test/java/com/app/pegsolitaire/testing/BackdropTestHost.kt` — the stand-in host: Settings' third callback (merge), then a Settings licences stand-in and one stand-in per licences destination (c7efee8).
- `test/java/com/app/pegsolitaire/ui/backdrop/TransparentContainersTest.kt` — `onOpenLicenses = {}` (merge), then the four licences rows.
- `test/java/com/app/pegsolitaire/ui/backdrop/BackdropDensityTest.kt` — `everyLicencesScreenDrawsTheWash`.
- `docs/ux/licences-screen.md` — `develop`'s copy, unchanged.

### The merge (3752063)
1. **`docs/ux/licences-screen.md` (add/add):** `develop`'s copy. The branch's blob `9aa3437` equals `0bf578d:docs/ux/licences-screen.md`, which `develop`'s copy descends from (R-3.3). `git diff develop -- docs/ux/licences-screen.md` is empty.
2. **`navigation/AppNavHost.kt`:** the branch's host as it was — the F-32 `Surface`, the scene-ready flag and the clock provided, `BackdropClockDriver`, the `Box` with `backdropInputObserver`, `AppNavGraph` and then `NavBackdrop` at `BACKDROP_Z_INDEX` — with `develop`'s four licence slots added to `AppNavHost` before `backdropFrames` and passed through `AppNavGraph` to `develop`'s `licensesGraph(…)`. `AppNavGraph`'s `settingsContent` type takes `develop`'s `onOpenLicenses`; its KDoc says ten routes. The KDoc keeps both sides' text and adds that the licence routes take the density function's `else`, Sparse (E7). `backdropDensity()` itself is unchanged.
3. **Semantic fixes the test sources needed to compile:** `BackdropTestHost.kt`'s stand-in `settingsContent` takes the third callback; `TransparentContainersTest.kt` passes `onOpenLicenses = {}`. Without them, `:app:compileDebugUnitTestKotlin` failed: "BackdropTestHost.kt:136:27 Argument type mismatch: actual type is 'ComposableFunction2<…>', but 'ComposableFunction3<…>' was expected" and "TransparentContainersTest.kt:166:25 No value passed for parameter 'onOpenLicenses'".
4. **Auto-merged on both sides: `ui/settings/SettingsScreen.kt` only.** Its diff against `develop` is exactly the branch's transparent `Scaffold` (two lines) and `TopAppBar` colours, the same hunks as `git diff f1c399a c1e8c50 -- ui/settings/`. `git diff --name-only` on both sides of f1c399a shares no other file.

## Reds
Each red is a temporary, uncommitted mutation, run with `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '<class>'` and reverted (`git checkout --` or a copy back); `git status --porcelain` showed only the intended edits after each. Messages are the JUnit XML's.

**R-1, `ResultsStarPaletteTest` (unmutated: 2 of 2 pass):**

| Mutation | Result |
|---|---|
| N1, `BoardMarkRecipe.kt:63` `unearnedStarHollow = false` | 2 of 2 fail: "Standard: the unearned star's centre shows what lies beneath it / expected to be true" |
| The colour-blind twin, `BoardMarkRecipe.kt:77` | 2 of 2 fail: "Colorblind: the unearned star's centre shows what lies beneath it / expected to be true" |
| N8 (loop 0's M17): `ResultsScreen.kt:247` `StarGlyph(earned = false, …)` replaced by `Icon(Icons.Outlined.Star, null, tint = colorScheme.outlineVariant, Modifier.size(results_star_size))` | 2 of 2 fail: "Colorblind: the unearned star's centre shows what lies beneath it / expected to be true" |
| The hide step removed (`screenShown.value = false`) | 2 of 2 fail: "Failed to assert count of nodes. / Reason: Did not expect any node but found '1' node that satisfies: (TestTag = 'results_star_row')" |
| The hide step and the `assertCountEquals(0)` both removed | **1 of 2** fail, dark only: "the frame without the screen: no earned star 0's fill at its centre / expected to be false". Light passes (Open issue 1) |

**R-2, `HomeChipArrivalTest` (unmutated: 13 of 13 pass):**

| Mutation | Result |
|---|---|
| P9, `HomeScreen.kt:490` `entranceOrder = FIRST_CHIP_ENTRANCE_ORDER` | 1 of 13 fails: "the Classic chip arriving, frame 5, room 48.0.dp: px drawn in it / expected to be greater than: 0 / but was: 0" |
| The underlay removed from the one call (`renderWithThePausedClock()`) | 1 of 13 fails: "no chip yet: px drawn between the ribbon and the first tile / expected: 0 / but was: 6080" |

Every `renderHome` suite, with no change to its tree (the default `underlay` composes nothing): `HomeChipArrivalTest` 13/0, `HomeLayoutTest` 19/0, `HomeStageTest` 10/0, `HomeScreenTest` 24/0, `HomeHeroRowTest` 9/0, `HomeRibbonShapeTest` 12/0, `GelTileTest` 5/0, `ButtonSoundTest` 15/0, `BoardCanvasFirstFrameTest` 4/0.

**E7, `TransparentContainersTest`:**
- **Red first,** with the four rows added and no screen changed: "the licences list, dark = false / expected: -16711936 / but was: -263681" (`#ff00ff00`, the sentinel, against `#fffbf9ff`, `LightColors.surface`, the top bar).
- Green after d0a65ae's edits: 2 of 2.
- **Each container alone made opaque again** (the others transparent), 1 of 2 fails each time:
  - `LicensesTopBar`'s `colors` removed: "the licences list, dark = false / … but was: -263681" (`#fffbf9ff`, `surface`);
  - `LicensesScreen`'s `Scaffold`: "the licences list, dark = false / … but was: -724486" (`#fff4f1fa`, `background`);
  - `LicenseTextScreen`'s: "a licence text, dark = false / … -724486";
  - `GoogleNoticesScreen`'s: "the Google notices, dark = false / … -724486";
  - `GoogleNoticeTextScreen`'s: "a Google notice text, dark = false / … -724486".

**E7, `BackdropDensityTest`** (4 of 4 pass unmutated). The licence routes already take `else`, so no production change was needed, and the rows were watched failing under these mutations of `navigation/BackdropDestinations.kt` and `AppNavHost.kt`:
- `this?.hasRoute<Licenses>() == true -> BackdropDensity.Quiet` before `else`: "the licences list / expected: Sparse / but was: Quiet";
- the same for `LicenseText`: "a licence text / expected: Sparse / but was: Quiet";
- for `GoogleNotices`: "the Google notices / expected: Sparse / but was: Quiet";
- for `GoogleNoticeText`: "a Google notice text / expected: Sparse / but was: Quiet";
- Settings' `onOpenLicenses` edge in `AppNavGraph` cut to `{}`: "the licences list: the committed destination is Licenses / expected to be true". Every licences screen is Sparse, like Settings, so a density read alone would pass on Settings; each row reads the committed route first.

## Checks
- `uptime` before every Gradle run: 1-minute load 3.42–7.04 (bound 12). `runt.sh`, my scratch wrapper, waits for a load below 12 before each test run.
- `git merge-tree --write-tree --name-only HEAD develop` — the predicted conflicts: `navigation/AppNavHost.kt` and `docs/ux/licences-screen.md`.
- `git merge --no-ff develop` (bc95b2a) — 2 conflicts, resolved as above.
- `./gradlew --max-workers=4 :app:compileDebugUnitTestKotlin` — first **fail** (the two errors quoted in merge item 3), then pass.
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt :app:testDebugUnitTest --tests 'com.app.pegsolitaire.navigation.*' --tests 'com.app.pegsolitaire.ui.backdrop.*' --tests 'com.app.pegsolitaire.ui.licenses.*' --tests 'com.app.pegsolitaire.ui.settings.*' --tests '…ButtonSoundTest' --tests '…UserFacingTextHasNoProtectedNamesTest'` on the merge — pass: 40 suites, 257 tests, 0 failures.
- The reds above — each **fails as intended**; each unmutated run passes.
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt` after each commit's edits — pass, once a first **fail** on c7efee8's draft (`BackdropTestHost.kt:155` over 120 characters, ktlint and detekt `MaxLineLength`) was wrapped.
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.*' --tests 'com.app.pegsolitaire.navigation.*' --tests 'com.app.pegsolitaire.ui.backdrop.*'` after d0a65ae — pass: 32 suites, 156 tests, 0 failures.
- **Pixel reads in the licences and settings suites** (R-3.3's host rule): `grep -rn 'renderedWindow\|getPixel\|captureToImage\|toPixelMap\|\.background' app/src/test/java/com/app/pegsolitaire/ui/licenses app/src/test/java/com/app/pegsolitaire/ui/settings app/src/test/java/com/app/pegsolitaire/navigation/LicensesNavFlowTest.kt` — no match, so no licences suite needs `BackdropTestHost`.
- **`./scripts/check.sh --wait --max-workers=4` on 53911b9 — pass.** "BUILD SUCCESSFUL in 11m 10s", "107 actionable tasks: 31 executed, 16 from cache, 60 up-to-date". The 1-minute load was 5.44 when it started.
  - `:app:testDebugUnitTest` executed: 185 suites, 1,528 tests, 0 failures, 0 errors, 0 skipped. The XMLs are stamped 04:05–04:06Z, inside the run.
  - `:domain:test` 294/0, `:domain:fuzz` 1/0 and `:build-logic:test` 100/0, all FROM-CACHE. `:domain:depthSweep`, `:domain:koverVerify` and `:app:verifyGoogleNoticesParity` executed and passed. ktlint, detekt, `dpSpLiteralCheck` (both modules) and `:app:lint` are green.
  - The log has 0 `w:` lines from `src/main`.
- **Lint report** `app/build/reports/lint-results-debug.xml` — 0 errors, 62 warnings, the same total as the last round. None is in a file this round wrote: `gradle/libs.versions.toml` 36, `strings.xml` 16 (no diff against `develop`), `AudioRouteClassifier.kt` 5, `dimens.xml` 3, `App.kt` 1, `gradle-wrapper.properties` 1.
- **Suppressions:** `git diff develop...HEAD -- app/src domain | grep -nE '^\+.*(@Suppress|@Ignore|!!|@OptIn|ktlint-disable|detekt:|tools:ignore|noinspection)'` — no match (exit 1). `git diff --name-status develop...HEAD | grep -iE 'baseline|detekt|lint'` — nothing (exit 1).
- **BD-19:** `git diff --name-status develop...HEAD -- app/src/test | grep -v '^A'` lists `HomeChipArrivalTest.kt` (R-2), `HomeLayoutTest.kt` (O3, earlier rounds), `fixture/HomeTestFixture.kt` (O3 and R-2) and `ResultsStarPaletteTest.kt` (R-1). `develop`'s licences, navigation and settings suites have no diff against `develop`.
- **BD-05:** `git diff 3752063..HEAD -- app/src/main | grep -E '^\+' | grep -E '0x|[0-9]\.dp|[0-9]\.sp|Color\('` — no match (exit 1).
- **BD-04:** `git diff --stat develop...HEAD` over `res/drawable*`, `res/mipmap*`, `res/raw`, `assets/`, `libs.versions.toml` and `*.gradle.kts` prints nothing; `strings.xml` has no diff against `develop`. No dependency was added, so no `THIRD_PARTY.md` row is owed.
- **Start-path files the merge brought** (`git diff --name-only c1e8c50 3752063`, main sources, tech design §10's list): `di/DataModule.kt`, `di/DispatchersModule.kt`, `navigation/AppNavHost.kt`, `navigation/LicensesDestinations.kt`, `navigation/Routes.kt`. `git merge-base HEAD develop` = bc95b2a.

## Open issues / risks
1. **R-1's pixel guard, as ruled, bites in dark only.** With the hide step and the count both removed, it fails 1 of 2. The reference frame is taken after the flip to Colorblind. In dark, the colour-blind earned star is gold, so it lies between `boardMarksFor(Standard, dark).earnedStarFill`'s stops; in light it is amber, so the Standard-stop check never sees the star still drawn. The test then passes in light on a reference frame that still holds the screen. **The semantic guard bites 2 of 2,** so no vacuous pass gets through as committed. A probe (uncommitted, reverted) that checks both palettes' fills, `BoardMarkPalette.entries.any { isBetweenStops(px, boardMarksFor(it, dark).earnedStarFill) }`, fails 2 of 2 under the same mutation: "the frame without the screen: no earned star 0's fill at its centre / expected to be false". **I committed the ruled form unchanged.** Whether to widen it is the tech-lead's and the code-auditor's call.
2. **Two approval lines are owed** (the ruling's Open issue 1): the code-auditor's "amended, not weakened" line for R-1 (bd8abf5) and for R-2 (c1e8c50). It should re-apply at least N1 and the hide-step red (R-1), and P9 and the underlay red (R-2).
3. **Two `UserFacingTextHasNoProtectedNamesTest` classes now run:**
   - this branch's BD-01 test, `app/src/test/java/com/app/pegsolitaire/UserFacingTextHasNoProtectedNamesTest.kt`;
   - `develop`'s LS-AC17 test, `app/src/test/java/com/app/pegsolitaire/ui/UserFacingTextHasNoProtectedNamesTest.kt`.

   Both scan `values*/strings.xml` and `src/main/assets/**`. The licences brief says "candy owns its word list thereafter". Folding them into one is not mine to decide, and neither may be deleted without a ruling. **Routed to the tech-lead.**
4. **15e (R-3).** The merge brought the start-path files listed in Checks. Arm A for the sitting is `git merge-base HEAD develop` at the sitting — bc95b2a today, if nothing else lands. Talkback-board is not on `develop`; R-3.4's allowance and its two greps apply if it lands after the sitting.
5. **The commit trailer.** These commits carry plugin rules/gitflow.md v1.0.0's single `Claude Fable 5.1` trailer on Opus-written code, as in earlier rounds. The harness reminder asked for an Opus trailer and a `Claude-Session:` line; the studio rule says one trailer "whatever a harness reminder asks". The producer still has to settle the form.
6. **Device rows stay with QA:** the licences screens over the Sparse wash in light and dark (LS-AC08's eye check, BD-12/BD-13 on four new screens), the earlier rounds' rows, and 15e.

## Handoff
Next: producer —
1. **code-auditor (loop 2, with its existing list):**
   - the R-1 and R-2 diffs (bd8abf5, c1e8c50) and their reds, then an "amended, not weakened" line for each or a refusal (Open issues 1 and 2);
   - the merge's hand-resolved hunks (3752063);
   - E7 (d0a65ae, c7efee8) and the previews (53911b9).
2. **tech-lead:** Open issue 1 (the pixel guard's reach) and Open issue 3 (the two protected-names tests); the re-read of the merge-in; the sign-off after the auditor and QA.
3. **qa-engineer:** the device rows (Open issue 6). Then 15e on `Pixel_9a_bench` after the final merge-in, with **arm A = `git merge-base HEAD develop` at the sitting** (bc95b2a while `develop` stays where it is), arm B the branch head, the host quiet; then the jank block.

Merge commit: 3752063 (`develop` bc95b2a; talkback-board not merged). Final code HEAD: 53911b9. Check: **green**, `./scripts/check.sh --wait --max-workers=4`, "BUILD SUCCESSFUL in 11m 10s", 1,528 app tests and 0 failures.
