# code-auditor report — candy-backdrop, loop 2 (three develop merge-ins, G1, E7, R-1/G-1, R-2)
Date: 2026-11-01
Branch: feature/candy-backdrop (23bf220..38789b6; merges 8fac490, 3752063 and d9f4f92; `git merge-base HEAD develop` = d251c3b)
Skills loaded: ergoz-soft:standards (role code-auditor. The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read these plugin rule files in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing. logging, puzzle-design and play-store are identical to the snapshot, which CLAUDE.md imports. Also the product's `.claude/rules/lessons.md`.), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable. The developer wrote this code on Opus, so this review ran on the author's model. That is allowed by the founder's Q-019 answer, option 3, and its 2026-10-24 addendum (`.planning/questions/FOUNDER.md:118-121`). The mitigations were applied: I ran the Check myself with the app tests forced to re-run, and I re-applied every red the dispatch lists, plus two mutations of my own, in a throwaway worktree.
Fan-out: none
Loops: 2
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **All three merges keep both sides.** I re-derived each one with `git merge-tree`, and the conflict sets match the developer's reports. In every file both sides changed, each side's delta is carried in full; the only differences are KDoc rewording and line wraps, which I read by hand. Talkback's four TalkBack suites are byte-identical to `develop`.
- **Every red in the dispatch fails again as reported,** in both themes where a theme applies. So does each of my own mutations.
- **The approval lines below are given:** the merged 15m oracle and M1, R-1 with G-1, and R-2. E7, G1 and 9d6bbeb hold.
- **The Check is green,** with the app tests forced to re-run: 1,632 tests, 0 failures. There is no new suppression (`@Suppress` count 12 in `app/src/main`, as on `develop`).
- **Verdict: FAIL, on one LOW mechanical finding (H1).** BD-01's command 3 now prints two KDoc lines, where its pass line is zero lines. The fix is two comment rewords.

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/code-auditor-loop2.md — this report.
- No source file was edited. Every mutation ran in a throwaway `git worktree add --detach` of 38789b6 under the session scratchpad. After each run I reverted it with `git checkout -- app` and checked that `git status --porcelain` was empty. Then I removed it with `git worktree remove --force` and pruned it. `git worktree list | grep -c cand-l2` prints 0, and the candy worktree's `git status --porcelain` is empty. I did not run `./gradlew --stop`.

## Checks
- **Host.** `uptime` before every Gradle run gave 1-minute loads of 3.80 and 5.10–7.15, all under 12. `ps` showed no `Pixel_9a_bench`, only two emulator crashpad handlers and no qemu. I touched no emulator.
- **The Check** — **pass.**
  - Command: `./scripts/check.sh --wait --max-workers=4 --console=plain :app:testDebugUnitTest --rerun` at 38789b6. That is `./gradlew check` under the worktree lock, with the app test task forced to run.
  - It started at 04:39:01Z and ended "BUILD SUCCESSFUL in 11m 7s", "107 actionable tasks: 12 executed, 95 up-to-date", `CHECK_EXIT=0`.
  - Executed: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:lint`, `:app:dpSpLiteralCheck`, `:domain:dpSpLiteralCheck`, `:domain:depthSweep` and `:domain:koverVerify`. ktlint, detekt and `:domain:fuzz` were up to date.
  - The XMLs hold **193 suites and 1,632 tests: 0 failures, 0 errors, 0 skipped**, stamped 04:39:03–04:41:03Z, inside this run.
  - The log has 0 `w:` lines from `src/main` and 0 `e:` lines.
- **Lint** `app/build/reports/lint-results-debug.xml` — 0 errors and 62 warnings, the same count and files as last round: `libs.versions.toml` 36, `strings.xml` 16, `AudioRouteClassifier.kt` 5, `dimens.xml` 3, `App.kt` 1 and `gradle-wrapper.properties` 1. `strings.xml` equals `develop`'s. **This branch adds no warning.**
- **Suppressions** — **nothing to approve or reject.**
  - `git grep -c '@Suppress'` summed over `app/src/main` gives 12 on `develop` and 12 on HEAD. Over `app` and `domain` it gives 30 and 30.
  - `git diff develop...HEAD -- app domain build-logic | grep -nE '^\+.*(@Suppress|@Ignore|!!|@OptIn|ktlint-disable|detekt:|tools:ignore|noinspection|lint:ignore)'` exits 1.
  - `git diff --name-status develop...HEAD | grep -iE 'baseline|detekt|lint|config/'` exits 1.
- **The merges, re-derived** with `git merge-tree --write-tree --name-only <parent1> <parent2>`:
  - 8fac490 (23bf220 + f1c399a, base f918793): conflicts in `GameScreen.kt`, `GameStatusStrip.kt`, `HomeScreen.kt`, `HomeStage.kt`, `Theme.kt`, `HomeLayoutTest.kt` and `docs/ux/colour-vision.md`.
  - 3752063 (c1e8c50 + bc95b2a, base f1c399a): conflicts in `AppNavHost.kt` and `docs/ux/licences-screen.md`.
  - d9f4f92 (5e2dadc + d251c3b, base bc95b2a): conflicts in `GameScreen.kt` and `TutorialScreen.kt`.
  - All three sets match the developer's reports.
- **Both sides survive.** For every file changed on both sides, my script compared the sorted `+`/`-` lines of each side's delta from the merge base with the merge's delta over the other parent.
  - **Identical on both sides,** so nothing was lost:
    - at 8fac490: `GameScreen.kt`, `GameStatusStrip.kt`, `HomeProgressRibbon.kt`, `TutorialScreen.kt`, `ResultsScreen.kt`, `SettingsScreen.kt`, `Clay.kt`, `Color.kt` and `dimens.xml`;
    - at 3752063: `SettingsScreen.kt`;
    - at d9f4f92: `TutorialScreen.kt`, `ResultsScreen.kt` and `docs/ux-spec.md`.
  - **The remainder, read by hand:**
    - `Theme.kt`: one `CompositionLocalProvider` holds all four locals, and `develop`'s `remember(boardMarkPalette, darkTheme)` is kept.
    - `HomeStage.kt`: `develop`'s fade layer and its stage-level `clayEntrance` are kept, then `finishGlow`, the fill, and `glossFinish(StageArt)`.
    - `HomeScreen.kt`: the residue is the class KDoc only, merged from both texts. `develop`'s code lines are all present.
    - `HomeLayoutTest.kt`: the merged oracle. The approval lines below cover it.
    - `AppNavHost.kt`: the branch's host, with `develop`'s four licence slots threaded through `AppNavGraph` into `licensesGraph(…)`. Below the transitions block, `develop`'s graph body is unchanged.
    - `GameScreen.kt` at d9f4f92: on talkback's side, compared with `-w` because talkback re-indented the body, no talkback line is missing. The only extra lines are candy's `finishGlow(…)`/`glossFinish(…)` call, wrapped. `git diff d251c3b d9f4f92 -- GameScreen.kt` is exactly candy's transparent `Scaffold`, slab and imports.
  - **The docs:** `colour-vision.md` at 23bf220 and at 8f67d74 are the same blob (`4d09ff4`), and 8f67d74 is an ancestor of f1c399a. `licences-screen.md` at c1e8c50 and at 0bf578d are the same blob (`9aa3437`). Both merges took `develop`'s copy: `git diff --stat` against `develop` is empty.
- **The TalkBack suites are unedited.** `git diff --quiet develop HEAD` exits 0 for `GameScreenTalkBackTest.kt`, `TutorialTalkBackTest.kt`, `BoardSemanticsTest.kt` and `ResultsScreenTalkBackTest.kt`, and none has a commit in `d251c3b..HEAD`.
- **BD-01**, the three commands under the BD-01 table in `docs/ux/candy-backdrop.md` (lines 1636–1640), run from the repository root:
  - (1) and (2) print zero lines (exit 1).
  - **(3) prints two lines (exit 0): see H1.**
- **BD-04:** `git diff --stat develop...HEAD` over `res/drawable*`, `res/mipmap*`, `res/raw`, `assets/`, `libs.versions.toml`, `*.gradle.kts` and image files prints nothing. No dependency was added, so no `THIRD_PARTY.md` row is owed.
- **BD-05:** `git diff develop...HEAD -- app/src/main ':!…/ui/theme/Color.kt'`, keeping added non-comment lines and grepping for `0x|[0-9]\.dp|[0-9]\.sp|Color\(`, exits 1.
- **Security:** the branch delta touches no manifest, build file, catalog or secret. The only new receiver is loop 0's battery-saver one, `RECEIVER_NOT_EXPORTED` and unregistered on dispose. There is no network call and no new permission. Clean.
- **Mutations.** Each was run as `./gradlew --max-workers=4 --console=plain :app:testDebugUnitTest --tests '<class>' …` in the throwaway worktree; messages are from the JUnit XML. Unmutated, every class is green in the Check above.

| Run | Mutation | Result |
|---|---|---|
| 1 | **M5c (a):** the full pill's `Icon` deleted from `FullPillContent`. **4w:** a white 10dp `Icons.Filled.Star` as a third measurable in `CompactPillContent`, placed at (0, 0), which is inside the gloss band | `HomeLayoutTest` 19 tests, 4 failures, exactly the four oracle tests. Both full-pill tests: "pills painting under 20 px besides their finished surface and their texts: pill 0 0 px, none; pill 1 0 px, none; pill 2 0 px, none / expected to be empty / but was: [0, 1, 2]". Compact light: "G5: … pill 0 40 px … rows 9..16 below its top; pill 1 38 px …; pill 2 40 px …". Compact dark: 44 / 41 / 44 px, rows 9..16 |
| 2 | **M3d:** `Text("★")` as the first measurable of `CompactPillContent` | 2 of 19 fail, both compact tests: "pill 0: the texts it lays out, in order / unexpected (1): ★ / expected: [Stars, 7] / but was: [★, Stars, 7]". This fires ahead of every pixel guard |
| 3 | **G-red:** `references.take(0)` in `ReferenceFinishes` | 4 of 19 fail, all four oracle tests: "G2: frame 2 composes reference 0 once / expected: 1 / but was: 0" |
| 4 | **Mine:** `StatPill`'s fill changed to `surfaceContainerHighest` | 2 of 19 fail, both compact tests, at G5: pill 0 1,196 px, pill 1 568 px, … in both themes, the same as loop 0. **The two full-pill tests stay green,** which is right: they assert that a glyph is present (I3) |
| 5 | **R-1 with G-1, the vacuous pass:** `screenShown.value = false` and `assertCountEquals(0)` both removed. **`TransparentContainersTest`:** `GoogleNoticeTextScreen`'s two `Scaffold` lines removed. **Talkback:** `AnnouncementRegion(announcer)` dropped from `GameScreen` | `ResultsStarPaletteTest` **2 of 2** fail: "the frame without the screen: no earned star 0's fill at its centre / expected to be false", in light and dark. `TransparentContainersTest` 1 of 2: "a Google notice text, dark = false / expected: -16711936 / but was: -724486". `GameScreenTalkBackTest` 9 of 25 fail, for example "Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'announcement_region')" and "aServiceIsHandedTheBoardsChannelAsTheWindowsOnlyLiveRegionAndALeaf: expected 1 but was 0" |
| 6 | **P9:** the late Classic chip always takes `FIRST_CHIP_ENTRANCE_ORDER`. **Talkback:** the slab's `semantics { isTraversalGroup; traversalIndex = BOARD_BEFORE_BANNER }` removed | `HomeChipArrivalTest` 1 of 13: "the Classic chip arriving, frame 5, room 48.0.dp: px drawn in it / expected to be greater than: 0 / but was: 0". `GameScreenTalkBackTest` 1 of 25: `onAWindowThatLimitsTheBoardByHeightTheSlabIsOrderedBeforeTheBanner`, "expected to be less than: 0.0 / but was: 0.0". `BoardSemanticsTest` stays 20 of 20 (I4) |
| 7 | **R-2, no underlay:** `renderWithThePausedClock(underlay = { FlatBackground() })` reverted to `renderWithThePausedClock()` | 1 of 13: "no chip yet: px drawn between the ribbon and the first tile / expected: 0 / but was: 6080" |
| 8 | **Mine:** `renderHome` draws the underlay **after** `HomeScreen` and the overlay, so it covers Home | 1 of 13: "frame 5, room 48.0.dp: px drawn in it / … but was: 0". The guard passes on the covered gap, but the oracle then sees no chip. A misplaced underlay cannot pass |

## Findings

### Must fix (send back to the developer)
1. **H1 — LOW — BD-01's command 3 prints two lines; its pass line is zero.**
   - **What shows it:** `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'` prints:
     - `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicensesTopBar.kt:28`: "… as every top bar does (candy-backdrop BD-14, E7)." This came from d0a65ae, the developer's KDoc.
     - `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsStarPaletteTest.kt:46`: "… Sparse (candy-backdrop §12), and 'beneath' is read from". This came from bd8abf5. It is the tech-lead's verbatim R-1 step-7 wording (`tech-lead-merge-ruling.md`), so it was ruled in, not invented.
   - **Why it matters:** both lines are comments, not user-facing text, so no word reaches a player. But BD-01 is a mechanical row with an Auditor owner, and its pass line is "all three print **zero lines**" (`docs/ux/candy-backdrop.md:1603`). Loop 1 measured zero. Neither later round's report lists command 3: only BD-04 and BD-05 are recorded.
   - **What the fix must give:** command 3 prints zero lines. The branch already names the spec as "the backdrop spec" (e.g. `HomeScreen.kt`'s class KDoc, `AppNavHost.kt`), so rewording both references that way suffices.
   - **The R-1 line still holds:** the edit to `ResultsStarPaletteTest.kt` is comment-only, so my R-1 approval line below survives it. Re-check the edit with command 3, ktlint and the Check; no red needs re-showing.

### Approval lines (BD-19 / B-19a, A7.5)
- **Merged 15m oracle, at 8fac490 and unchanged to HEAD: APPROVED, amended, not weakened.** `git log 8fac490..HEAD -- HomeLayoutTest.kt` prints nothing. This covers `assertEachPillPaintsOnlyItsSurfaceAndItsTexts(darkTheme)`, `inkBesideTheTextsOfEachPill(darkTheme)` and the guards G1–G5. The reasons:
  - **Strength:** `PAINT_TOLERANCE` stays 1, and `GLYPH_INK_FLOOR_PX` stays 20.
  - **M3d:** `develop`'s check is kept verbatim (expected texts, `inOrder`), and it runs ahead of the pixel guards (run 2).
  - **The guards still bite:** G2 is shown by run 3. G5 is shown by runs 1 and 4 in both themes, with 4w landing at rows 9..16, inside the band. G3's draw-less red is the developer's, and its guard bodies are byte-identical to e07b105, which loop 0 approved.
  - **Scope:** the test edits stay inside O3's files.
- **M1, `develop`'s full-pill test moved onto the reference finish: APPROVED, amended, not weakened; strengthened.**
  - **Before:** on this branch, `develop`'s flat read counted the gloss as ink. It passed with the glyph deleted (loop 0's probe: 1,062–1,278 px against a floor of 20), so it was vacuous here.
  - **Now:** it reads ink against frame 2 under G1–G4 and counts 0 px with the glyph deleted, in both themes (run 1).
  - **Added:** a dark sibling, and it collects every pill before asserting.
  - **Lost:** nothing it caught on `develop`. A fill-token change is not this test's claim, and the compact tests still catch it (run 4, I3).
- **R-1 with G-1 (bd8abf5 + 3d3879f), `ResultsStarPaletteTest`: APPROVED, amended, not weakened.**
  - **Oracle:** the unearned star's centre is compared with the same pixel of the same host (`FixedBackdropClock(TEST_POSE_MS)`, the same pose) with the screen taken out. That is CV-AC05 §12.5's own wording, "the surface beneath it … the Results sky".
  - **Untouched:** every earned-star assertion, the band check and `EarnedStarCountKey`.
  - **The vacuous path is closed twice:**
    - the semantic guard, `assertCountEquals(0)` on the star row;
    - G-1's pixel guard, which is now palette-blind and fails 2 of 2 when both the hide step and the count are removed (run 5). The ruled form failed only in dark.
  - **N1 and N8 reds:** the developer's, 2 of 2 each. A filled unearned star (`onSurfaceVariant`) cannot equal the sky.
- **R-2 (c1e8c50), `HomeChipArrivalTest` and `renderHome`'s `underlay`: APPROVED, amended, not weakened.**
  - **The oracle is unchanged:** `inkBetween`, `INKED_ROOM_DP` and `FRAMES_READ`. The underlay restores the flat field the test's KDoc states.
  - **The new guard:** 0 px before the chip makes the empty-room pass impossible (run 7: 6,080 px without the underlay).
  - **P9 is caught again** at frame 5 (run 6). A misplaced underlay cannot pass either (run 8).
  - **Every other `renderHome` caller** composes the same tree, because the default is `{}`. Their suites are green in the Check.

### Verified
- **G1 (7ce3314) — fixed.** Both `GameScreenPreviews.kt` previews, light and dark at 360×640, go through `BackdropPreviewHost(BackdropDensity.Quiet)`. A sweep of every `@Preview` file in `app/src/main` finds every screen-level preview on the host:
  - `HomeScreen` 3, `HomeHero` 1, `GameStatusStrip` 1, `TutorialScreen`, `GameScreenPreviews` 2;
  - the four licences screens: 1 + 2 + 2 + 2.
  The rest are component previews. Results, Settings and the catalogue have no previews at all, as on `develop` (I5).
- **9d6bbeb (BD-17 per palette) — holds.** The slab-interior read runs for each `BoardMarkPalette` × theme. It first asserts `BoardMarkPaletteKey` on the board-state node, so a pass that did not switch palette fails. The developer showed the red. It is green in the Check, after talkback's traversal group landed on the same node.
- **E7 (d0a65ae, c7efee8, 53911b9) — built to R-3.3 and LS-AC08 as amended.**
  - **Transparency:** the four licences `Scaffold`s are transparent with `contentColor = onBackground`, as Settings is. `LicensesTopBar` sets both `containerColor` and `scrolledContainerColor` to transparent.
  - **The cards:** the entry and notice cards stay opaque `surfaceContainerHigh`. That matches the spec's §2 row, "Settings' children, treated exactly as their parent".
  - **TransparentContainers:** it now covers all ten routes, and run 5 shows a licences row biting.
  - **Density:** `BackdropDensityTest.everyLicencesScreenDrawsTheWash` reads the committed route before the density, so a stand-in that failed to navigate cannot pass on Settings' Sparse wash.
  - **Previews:** all seven licences preview functions use the host at Sparse.
  - **No pixel read in the licences suites:** `grep` of the licences, settings and `LicensesNavFlowTest` sources finds none, so §12's host rule asks nothing more of them.
- **The talkback merge-in on the screens.**
  - **`GameScreen`:** talkback's root `Box`, `AnnouncementRegion` (drawn last, 1 px, no background), the slab's traversal group and `BoardStop` are all present, next to candy's transparent `Scaffold` and finished slab. Run 5 and run 6 show the TalkBack suite still guards them on the merged screen.
  - **`TutorialScreen`:** its single hand hunk is imports only.

### Information (no change asked)
2. **I3 — the full-pill tests do not pin the pill's fill token** (run 4). The compact tests do, in both themes. This is how the oracle is divided up, not a gap.
3. **I4 — removing the slab's traversal group is caught by one test** (`…OnAWindowThatLimitsTheBoardByHeight…`); `BoardSemanticsTest` does not see it. That is talkback's coverage, unchanged by this branch.
4. **I5 — Results, Settings and the catalogue have no `@Preview`**, as on `develop` (rules/compose.md asks for light and dark ones on every public composable). This predates the branch; I list it for the producer only.
5. **I6 — R-1's tolerance.** If Results' `Scaffold` became opaque again, the R-1 oracle could pass within `CHANNEL_TOLERANCE`, because the background and the Sparse wash are near in light. `TransparentContainersTest`'s Results row is what holds that (the developer's red). The approved line does not rely on R-1 for it.

## Open issues / risks
1. **Model deviation (Q-019):** reviewed on the author's model. Mitigated by the forced re-run and my own probes.
2. **H1 came partly from a ruling's verbatim text.** For the tech-lead: ruled KDoc wording that names the spec file by its slug trips BD-01's command 3.
3. **Commit trailer.** As in loops 0 and 1, my commit carries the harness's attribution lines. The product's CLAUDE.md imports the v0.6.2 gitflow snapshot, which has no trailer rule. The developer's commits carry plugin v1.0.0's single `Claude Fable 5.1` trailer on Opus-written code. The producer still has to settle which form the studio wants.
4. **Device rows stay with QA:**
   - F1 ON→OFF with the app resident;
   - B-22a after Home's settle;
   - the licences screens over the Sparse wash in light and dark (LS-AC08's eye check);
   - TalkBack on the merged screens over the backdrop;
   - BD-02's captures, then this role's BD-02 line;
   - 15e on `Pixel_9a_bench` with arm A = `git merge-base HEAD develop` at the sitting, d251c3b today.

## Handoff
Next: producer —
1. **android-game-dev:** fix H1 by rewording two comments. Show that BD-01 command 3 prints zero lines, then run ktlint and the Check.
2. **code-auditor (loop 3):** H1 only: command 3, the diff, and the Check. The approval lines above stand for a comment-only change.
3. **tech-lead:** Open issue 2. The sign-off follows this loop and QA's report.
4. **qa-engineer:** Open issue 4.

VERDICT: FAIL
