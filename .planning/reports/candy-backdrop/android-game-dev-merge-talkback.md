# android-game-dev report — candy-backdrop: the talkback-board merge-in, and G-1
Date: 2026-11-01
Branch: feature/candy-backdrop (d9f4f92..3d3879f, on 5e2dadc. d9f4f92 is the `--no-ff` merge of `develop` at d251c3b. b542533 is the tech-lead's report, which I committed on the producer's instruction. This report is committed by the producer, because the harness blocked my write.)
Skills loaded: ergoz-soft:standards (role android-game-dev). The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read these plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing, plus the product's `.claude/rules/lessons.md`. Other skills: gitflow, compose-skill, compose-ui, superpowers:test-driven-development, android-testing. All were loaded before the first conflict edit.
Ran in parallel with: the tech-lead's R-1 guard ruling (report-only, written outside this worktree while the merge was open)
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 0 (a merge-in, not a review round)
Lessons applied: none (the snapshot lists 0 active lessons)

Producer note: the harness refused this agent's report-file write; the producer saved its returned text here verbatim.

## Summary
- **Merged `develop` at d251c3b** (feature/talkback-board) as **d9f4f92** (`--no-ff`). Two files conflicted, with 4 hand-resolved hunks: 1 in `TutorialScreen.kt` (imports) and 3 in `GameScreen.kt`. `ResultsScreen.kt` and `docs/ux-spec.md` merged on their own. Every screen keeps both sides: BD-14 transparency, and talkback's spoken channel, stops and focus.
- **The tech-lead's two 15e greps print nothing** on the merge-in (Checks). No start-path file, preference key, entity or database comes in. Arm A is now d251c3b.
- **`TransparentContainersTest` still covers all 10 routes**, and it fails as it should when each of the three merged screens is made opaque again. Talkback's four TalkBack suites pass and are **unedited**: none differs from `develop`.
- **G-1 is applied as ruled** (3d3879f). With the hide step and the node count removed, the widened pixel guard fails 2 of 2; the ruled form failed 1 of 2. The tech-lead's report is committed byte for byte as b542533.
- **Check: green.** `./scripts/check.sh --wait --max-workers=4 --console=plain` on 3d3879f: "BUILD SUCCESSFUL in 11m 7s", `CHECK_EXIT=0`, 1,632 app tests, 0 failures.

## Outputs
Commits (each carries the single trailer `Co-Authored-By: Claude Fable 5.1`, per rules/gitflow.md; see Open issue 3):

| Commit | Kind | What |
|---|---|---|
| d9f4f92 | merge | `develop` d251c3b merged in (parents 5e2dadc and d251c3b; tree 70233fb), with the resolutions below |
| b542533 | docs | `.planning/reports/candy-backdrop/tech-lead-r1-guard.md`: the tech-lead's report, copied byte for byte from the producer's scratchpad path and committed alone (sha256 `dda90d2a…a7a2` for both copies). Not my report; not edited |
| 3d3879f | test | G-1: `ResultsStarPaletteTest`'s pixel guard checks every palette's earned fill |

Files I wrote or resolved:
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt`: three merge hunks.
- `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt`: one merge hunk.
- `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsStarPaletteTest.kt`: G-1, exactly the tech-lead's statement.

### The merge (d9f4f92): every hand-resolved hunk
1. **`TutorialScreen.kt`, imports.** Candy's four `ui.backdrop` preview imports come first, then talkback's `ui.components.AnnouncementRegion`, in lexical order. The rest of the file merged on its own and carries both sides: candy's transparent root `Surface` (`Color.Transparent`, `contentColor = onBackground`); talkback's announcer, focus requesters and `TutorialBoardAnnouncements` above that `Surface`; `AnnouncementRegion` drawn last inside the root `Box`; `BoardStop` and the slab's traversal group; Got it's `focusRequester` and `focusProperties`; candy's two previews through `BackdropPreviewHost(Sparse)`.
2. **`GameScreen.kt`, imports.** Talkback's `androidx.compose.ui.focus.FocusRequester`, then candy's `androidx.compose.ui.graphics.Color`. Candy's removal of the `clayShadow` import, and its `contentColorFor`, `FinishedContainer`, `finishGlow` and `glossFinish` imports merged on their own.
3. **`GameScreen.kt`, the screen's root.** Talkback's structure is kept: a root `Box(modifier.fillMaxSize())` holds the `Scaffold` (re-indented, `Modifier.fillMaxSize()`), and then `AnnouncementRegion`, drawn last. Candy's two lines go inside that `Scaffold`: `containerColor = Color.Transparent` and `contentColor = MaterialTheme.colorScheme.onBackground`. Neither talkback addition paints over the backdrop: the root `Box` draws nothing, and the region is a 1 px `Box` with no background.
4. **`GameScreen.kt`, the board slab.**
   - Talkback's whole body is kept: the `GAME_STATE_TEST_TAG` box's semantics with `isTraversalGroup`; the slab's `.semantics { isTraversalGroup; traversalIndex = GameTraversal.BOARD_BEFORE_BANNER }` after its test tag; `BoardStop` before `BoardCanvas`; the banner's focus moves.
   - Candy's slab is applied to it: `val slab = surfaceContainerHigh`; the `Surface` is transparent, with `contentColor = contentColorFor(slab)` and its two comment lines; `finishGlow`, then `glossFinish(BoardSlab, …, fill = slab)`, at the head of the modifier chain. `clayShadow` stays gone.
   - One form change: at talkback's deeper indent (36 spaces), candy's one-line `.finishGlow(…)` and `.glossFinish(…)` calls would be 122 and 123 characters; ktlint allows 120. I wrapped them the way `develop` wrote `clayShadow`. Arguments and order are unchanged.

**Both sides kept, checked per file.** For each file both sides changed (`GameScreen.kt`, `TutorialScreen.kt`, `ResultsScreen.kt`, `docs/ux-spec.md`), I compared the sorted `+`/`-` lines of `git diff -U0 bc95b2a d251c3b -- <f>` against `git diff -U0 5e2dadc <merged tree> -- <f>` (talkback's side) and `git diff -U0 bc95b2a 5e2dadc -- <f>` against `git diff -U0 develop <merged tree> -- <f>` (candy's side). `TutorialScreen.kt`, `ResultsScreen.kt` and `docs/ux-spec.md`: identical on both sides. `GameScreen.kt`: compared with `-w` on talkback's side (it re-indented the body); every talkback line is carried, and the only extra lines are candy's wrapped finish calls. On candy's side the only difference is that wrap; `git diff develop HEAD -- GameScreen.kt` is exactly candy's transparency and finish change.

## G-1 (3d3879f), from the tech-lead's `tech-lead-r1-guard.md`
`ResultsStarPaletteTest.kt` lines 113–121 at 5e2dadc are now the ruled statement: `val centre = beneath.starPixel(…)`, then `.that(BoardMarkPalette.entries.any { isBetweenStops(centre, boardMarksFor(it, dark).earnedStarFill) }).isFalse()`. Unchanged: the message, the semantic guard, the unearned-star check, the earned-star checks, the KDoc and the imports. The longest line is 118 characters.

**Reds, on the merge d9f4f92.** Each is a temporary, uncommitted mutation, run through a scratchpad wrapper that waits for a 1-minute load under 12, then runs `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.results.ResultsStarPaletteTest'`; each reverted, `git status --porcelain` checked. Messages from the JUnit XML.

| Mutation | Guard | Result |
|---|---|---|
| The hide step (`screenShown.value = false`) and `assertCountEquals(0)` removed | As ruled in R-1 (before G-1) | **1 of 2** fail, dark only: "the frame without the screen: no earned star 0's fill at its centre / expected to be false". Light passes |
| The same | **G-1** | **2 of 2** fail, the same message, in `…InTheDarkTheme` and `…InTheLightTheme` |
| The hide step removed alone | G-1 | 2 of 2 fail at the node check: "Failed to assert count of nodes. / Reason: Did not expect any node but found '1' node that satisfies: (TestTag = 'results_star_row')". The node now carries talkback's `ContentDescription = '[2 of 3 stars]'` |
| N1, `BoardMarkRecipe.kt:63` `unearnedStarHollow = false` | G-1 | 2 of 2 fail: "Standard: the unearned star's centre shows what lies beneath it / expected to be true" |
| None | G-1 | **2 of 2 pass** |

Neither `UserFacingTextHasNoProtectedNamesTest` class was changed (G-2).

## Checks
- **Load.** `uptime` before every Gradle run: 1-minute load 3.14–7.24 (bound 12). `.gradle/ergoz-build.lock` was not held at any start.
- `git merge-tree --write-tree --name-only HEAD develop` — predicted conflicts in `GameScreen.kt` and `TutorialScreen.kt`, automatic merges of `ResultsScreen.kt` and `docs/ux-spec.md`.
- `git merge --no-ff --no-commit develop` (d251c3b) — 2 conflicts, resolved as above. `git grep -n -E '^(<<<<<<<|>>>>>>>|=======$)' -- app domain docs build-logic .planning` exits 1.
- `git grep 'wrongTapAnnouncement|WrongTapAnnouncement' -- app/src` — the only hit is `develop`'s own `TutorialSourceGuardTest.kt:61`, a forbidden-name list.
- `./gradlew --max-workers=4 --console=plain --continue :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:ktlintCheck :app:detekt` on the resolved index — pass ("BUILD SUCCESSFUL in 13s"); 0 `w:` from `src/main`, 0 `e:`; the 47 `w:` lines are all `src/test` `createComposeRule` deprecations.
- Focused suites on the merge — pass, 0 failures: `GameScreenTalkBackTest` 25/0, `TutorialTalkBackTest` 16/0, `BoardSemanticsTest` 20/0, `ResultsScreenTalkBackTest` 7/0; `TransparentContainersTest` 2/0, `BoardInteriorIdentityTest` 2/0, `BackdropDensityTest` 4/0, `ResultsStarPaletteTest` 2/0; every other `ui.backdrop` suite green.
- The four TalkBack suites are unedited: `git diff --quiet develop HEAD -- <each>` exits 0 for `GameScreenTalkBackTest.kt`, `TutorialTalkBackTest.kt`, `BoardSemanticsTest.kt`, `ResultsScreenTalkBackTest.kt`. No spec conflict arose.
- Test edits against `develop`: `git diff --name-status develop...HEAD -- app/src/test | grep -v '^A'` lists `HomeChipArrivalTest.kt` and `fixture/HomeTestFixture.kt` (R-2, O3), `HomeLayoutTest.kt` (O3) and `ResultsStarPaletteTest.kt` (R-1 + G-1).
- `TransparentContainersTest` covers all 10 routes in `navigation/Routes.kt` (Home, Game, Catalog, Results, Settings, Tutorial, Licenses, LicenseText, GoogleNotices, GoogleNoticeText); talkback adds no screen.
- `TransparentContainersTest` reds on the merged screens (one container made opaque, then reverted; `-724486` = `#fff4f1fa`, `LightColors.background`): `GameScreen` Scaffold lines removed → "the game screen, dark = false / expected: -16711936 / but was : -724486"; `TutorialScreen` root Surface back to `colorScheme.background` → "the tutorial, dark = false / … but was : -724486"; `ResultsScreen` Scaffold lines removed → "Results, dark = false / … but was : -724486".
- The tech-lead's two greps (R-3.4), arm A at a post-licences sitting = bc95b2a, talkback merge = d251c3b:
  - `git diff --name-only bc95b2a d251c3b | grep -E '(App\.kt|MainActivity\.kt|/di/|/navigation/|/ui/home/|/ui/theme/|res/font/|res/values(-night)?(-v27)?/themes\.xml|AndroidManifest\.xml)'` — no output, exit 1.
  - `git diff bc95b2a d251c3b | grep -E 'PreferencesKey|@Entity|@Database'` — no output, exit 1.
  - Cross-check on `5e2dadc..d9f4f92`: both print nothing, exit 1; widened to `res/values[^/]*/themes.xml`: nothing.
- Arm A now: `git merge-base HEAD develop` = d251c3b1c1a256e3c12c25002f89888d45ffdcc7.
- `./scripts/check.sh --wait --max-workers=4 --console=plain` on 3d3879f — pass: started 04:24:19Z, "BUILD SUCCESSFUL in 11m 7s", "107 actionable tasks: 21 executed, 7 from cache, 79 up-to-date", `CHECK_EXIT=0`. `app/build/test-results/testDebugUnitTest` deleted first; XMLs stamped inside the run. Executed: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:lint`, `:app:dpSpLiteralCheck`, `:domain:dpSpLiteralCheck`, `:domain:depthSweep`, `:domain:koverVerify`. Up to date / from cache: `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`, ktlint, detekt, `:domain:test` 294/0, `:domain:fuzz` 1/0, `:build-logic:test` 100/0, `:app:verifyGoogleNoticesParity`. App tests: 193 suites, 1,632 tests, 0 failures, 0 errors, 0 skipped (incl. the TalkBack suites, talkback's other suites, candy's suites `HomeChipArrivalTest` 13, `HomeLayoutTest` 19, and shared suites `NavFlowTest` 14, `LicensesNavFlowTest` 5, `BoardCanvasFirstFrameTest` 4). Log: 0 `w:` from `src/main`, 0 `e:`.
- Lint: `lint-results-debug.xml` 0 errors, 62 warnings, same count and files as last round; none in a file this round touched.
- Suppressions: `git diff develop...HEAD -- app/src domain | grep -nE '^\+.*(@Suppress|@Ignore|!!|@OptIn|ktlint-disable|detekt:|tools:ignore|noinspection)'` — no match; `git diff --name-status develop...HEAD | grep -iE 'baseline|detekt|lint'` — no match; `@Suppress` count in `app/src/main` 12 on `develop` and HEAD.
- BD-05: `git diff 5e2dadc..HEAD -- app/src/main | grep -E '^\+' | grep -E '0x|[0-9]\.dp|[0-9]\.sp|Color\('` — no match.
- BD-04: `git diff --stat develop...HEAD` over `res/drawable*`, `res/mipmap*`, `res/raw`, `assets/`, `libs.versions.toml`, `*.gradle.kts` prints nothing; `strings.xml` equals `develop`. No dependency added.
- C11: `git grep -n 'ANIMATOR_DURATION_SCALE' HEAD -- app/src/test` gives `SystemMotionScale.kt:16` (KDoc) and `develop`'s `AnimationsEnabledGateWithoutScaleTest.kt:29`; no new reduced-motion driver.
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
1. **The code-auditor still owes two approval lines:** R-1 together with G-1 (bd8abf5 and 3d3879f; re-apply the vacuous-pass red, now 2 of 2) and R-2 (c1e8c50).
2. **The hand hunks of d9f4f92 need the auditor's read:** `git show --cc d9f4f92 -- app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt`. The one form change is the wrapped finish chain (hunk 4).
3. **The commit trailer is still unsettled.** These commits carry the single `Claude Fable 5.1` trailer on Opus-written work, per rules/gitflow.md; this checkout has no `prepare-commit-msg` hook.
4. **This report was not committed by me** (harness block); the producer commits it.
5. **Device rows stay with QA:** TalkBack on the merged screens over the backdrop (the slab as the board stop; the channel speaking over the transparent `Scaffold`s); the earlier rounds' rows; 15e.
6. **15e.** Under R-3.4 both greps print nothing, so a sitting after the licences merge-in (arm A bc95b2a) would still stand. A sitting from now on uses arm A = `git merge-base HEAD develop` = d251c3b, as long as `develop` does not move.

## Handoff
Next: producer —
1. Commit this report.
2. code-auditor: d9f4f92's hand hunks; G-1 (3d3879f) and its reds; the "amended, not weakened" lines for R-1 with G-1, and for R-2.
3. qa-engineer: the device rows in Open issue 5; then 15e on `Pixel_9a_bench` (arm A = `git merge-base HEAD develop` at the sitting, d251c3b today; arm B the branch head; host quiet); then the jank block.
4. tech-lead: a re-read of this merge-in, then the sign-off after the auditor and QA.
