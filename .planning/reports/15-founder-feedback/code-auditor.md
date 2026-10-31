# code-auditor report — 15-founder-feedback, slice S11 (D-03l, D-05d, D-04e)
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (0a644d8..d017711; audited diff `bffe558..d017711`, 10 commits, 32 files)
Skills loaded: ergoz-soft:standards (role code-auditor: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing — the plugin 0.6.2 copies were diffed over the product's 0.5.1 snapshot and read where they differ), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: the qa-engineer's device session on this slice (same worktree, emulator-5560, not touched by this role)
Loops: 0
Ran on: claude-opus-5-5 (Opus 5.5) at effort xhigh — **STAFFING DEVIATION:** the agent file pins fable/xhigh. Fable was exhausted (HTTP 429, req_011CfWkWg9kgDK8skQV5QcMF), so this ran on Opus under the founder's Q-003 answer (`.planning/questions/FOUNDER.md`). The author also ran on Opus, so independence rests only on this being a fresh agent. It received the requirements and the diff and did not read the developer's report, any other role's report on the slice, or `.planning/log/`. Every result below was produced by this role, not taken from a record.
Fan-out: none
Lessons applied: none (the snapshot and the plugin memory hold 0 active lessons)

## Summary
The production code implements D-03l, D-05d and D-04e as specified. There are no suppressions, `!!`, baselines, ignores or deleted tests, and a clean `./gradlew check` is green (98/98 tasks executed). Thirteen mutation runs confirm that most criteria have tests that go red. **One criterion does not:** 21e's "no other control … draws a `primary` fill or a ring". The code that turns a look's recipe into drawn fill, ring and shadow is never checked in rendered output. Three mutations of that code bring back the founder's on/off confusion, and all 1,044 app tests stay green (F-1). **VERDICT: FAIL** on F-1 alone. The fix is test-only.

## Outputs
- `.planning/reports/15-founder-feedback/code-auditor.md` — this report

## Checks
- `./gradlew check` in the shared worktree at `d017711` — pass, but **not evidence**: `BUILD SUCCESSFUL in 11m 7s` / `98 actionable tasks: 10 executed, 88 up-to-date`. `:app:testDebugUnitTest`, `:app:detekt`, `:app:ktlintCheck` and `:domain:test` were all `UP-TO-DATE`, i.e. replayed from someone else's run.
- `./gradlew check --no-build-cache` in a fresh detached worktree at `d017711` (`git worktree add --detach`, `local.properties` copied, removed afterwards) — **pass**: `BUILD SUCCESSFUL in 11m 32s` / `98 actionable tasks: 98 executed`. App unit tests: 1,044 run, 0 skipped, 0 failures, 0 errors; the 33 new tests all ran. Domain: 274 / 0 / 0. `:domain:depthSweep` ran. Detekt: `0 number of total findings`. ktlintCheck: pass. Lint: pass, with no issue in any changed Kotlin file. The 13 `UnusedResources` warnings all predate this slice, and every dimen it adds is referenced.
- `git diff bffe558..d017711 -- . ':!.planning' | grep '^+' | grep -E '@Suppress|@Ignore|@Disabled|!!|baseline|SuppressLint|ktlint-disable|detekt:disable'` — 0 code hits. The 11 hits are the prose phrase "design baseline".
- Removed `@Test` lines in `app/src/test`: 0. Added: 33. Build, Gradle, detekt, lint and baseline config files touched: none. No new dependency (`Icons.Rounded.Check` is in the existing `material-icons-core`), so no THIRD_PARTY.md row is owed.
- `grep -rn 'show_moves_off' app/src` → 0 (21d). `grep -rn 'HomeHeroArt' app/src/main` → 0 (15n).
- `git diff --stat bffe558..d017711 -- '*ButtonSoundTest*' '*GameFlowTest*' '*TutorialScreenTest*'` → empty. `ButtonSoundTest` passes unedited (15/15) (21d, §13).
- Security: no network, no secrets, no new permission, no new SDK call, no input surface. Nothing to report.
- **Mutation runs.** Each ran in the detached worktree only. Each was restored with `git checkout -- app`, and `git status --short` was empty afterwards in both that worktree and the shared one. Every run recompiled (`> Task :app:compileDebugKotlin`).

| # | Mutation (production code only) | Criterion | Result |
|---|---|---|---|
| A | Re-add `statusBarsPadding()` to the strip and `navigationBarsPadding()` to the dock, in their pre-slice positions | 18b, 18c | red — 3 of 6 `GameStatusStripTest` (18b, 18c ×2) |
| B | The same two insets placed **after** `.testTag(...)` | 18b | 18b **green**; both 18c red (F-2) |
| C | Timer pill `height(40dp)` instead of `heightIn`; `withTabularFigures()` removed | 18a, 18e | red — 18a and 18e |
| D | `WORKING_RECIPE.shadow = true`; one stage peg moved (6,2)→(6,3) | 21b, 15q | red — `DockLookRecipeTest`, `HomeStagePositionTest` |
| E | Undo always `DockLook.Available` | 21a, 21e (reach) | red — `DockStateLanguageTest`, `DockOnMarkAndSlashTest` reach test |
| F | `DockFill.Pale -> scheme.primary`; ring colour ignores `recipe.ring` | 21e, 21b | **all 1,044 app tests green** (F-1) |
| F2 | Ring never drawn (`dockRingColor` always transparent) | 21b, 21e | **all 1,044 app tests green** (F-1) |
| F3 | Shadow ignores `lift` (every disc raised) | 21b | **all 1,044 app tests green** (F-1) |
| G | `home_stage_art_min_size` 48dp → 100dp | 15p part 2 | red — 4 of 6 windows |
| H | Decorative `Icon(contentDescription = null)` between the hero title and the gear | 15n | **all Home tests green** (F-3) |
| H2 | 120dp decorative `Icon(contentDescription = null)` above the hero row | 15n, 15o | `HomeHeroRowTest` green; 6 `HomeLayoutTest`/`HomeStageTest` geometry tests red (F-3) |
| J | On mark drawn on every control; slashed glyph always | 21e (pixels) | red — 3 of 4 `DockOnMarkAndSlashTest` |
| K+L | Toggle `Role.Checkbox` with the name swapped when off; stage art `.clickable {}` | 21d, 15r | red — `GameScreenChromeTest` 21d test, `HomeStageTest` 15r test |

## Findings

**F-1 — MAJOR (blocking). No test observes the drawn fill, ring or shadow, so 21e's "a `primary` fill or a ring" clause and the on-screen form of 21b cannot fail.**
- Production: `app/src/main/java/com/app/pegsolitaire/ui/game/GameActionDock.kt:197-211` (`dockFillColor`, `dockRingColor`) and `:159` (`clayShadow(... * lift)`). These are the only code that turns a recipe into pixels.
- Tests: `DockOnMarkAndSlashTest.kt:99-104` asserts "draws a ring / a strong fill" against **recipe data**. The pixel probes at `:110-146` cover only the on mark and the slash. `DockLookRecipeTest.kt:14` claims "a recipe that passes here is a dock that reads without colour", and `DockOnMarkAndSlashTest.kt:79` claims reach plus recipe "is the whole of" 21e. The mutations show neither claim holds.
- Failure scenarios, each with 1,044/1,044 green:
  - **F:** Undo, Hint and Restart draw a strong `primary` disc with a ring. The actions look like toggles, which is exactly the "can't say if they are on or off" complaint D-04e exists to fix.
  - **F2:** Off loses its ring, so Off and Unavailable differ on screen only in glyph shape. That is one channel, below 21b's two.
  - **F3:** Working keeps its raised shadow, so it differs from Available only by the spinner.
- 21b as worded ("a unit test over the recipe data") is met. The gap is that nothing proves the dock draws what the recipe says. Device criteria 21f/21g would catch F2 in a QA session, but between device sessions the JVM suite is the only guard.
- Fix (developer's, test-only): extend `DockOnMarkAndSlashTest`'s NATIVE harness with rendered-output probes for each control in each look it can reach:
  - **Fill:** a point between the glyph and the rim. Expect `primaryContainer` for Available and Working, `primary` for On, and the slab for Unavailable and Off.
  - **Ring:** a point just inside the rim. Expect `onSurfaceVariant` for Off only.
  - **Shadow:** a point just below the disc edge, raised versus flat.
  - Watch each probe go red against F, F2 and F3 as quoted above.

**F-2 — MINOR. 18b measures the tagged wrapper, not the drawn strip and slab.** `GameStatusStripTest.kt:101-109` reads the bounds of `GAME_STATUS_STRIP_TEST_TAG` and `GAME_ACTION_DOCK_TEST_TAG` (`GameStatusStrip.kt:102`, `GameActionDock.kt:104`). Mutation B puts the second inset after the tag: the back chip sits at 48dp and the slab ends at 528dp, but 18b reads 24/576 and passes. Today only 18c catches it, and only because the 360x640dp board happens to be height-limited. Suggested: measure the `navigate_back` chip's top and the dock slab's drawn bottom. Not blocking, because the suite goes red.

**F-3 — MINOR. 15n/15o's own assertions see only semantics nodes, and decorative art is by definition not one.** `HomeHeroRowTest.kt:129`, `:135`, `:175` and `:216-218` filter semantics nodes. The KDoc at `:110-111` claims "a logo, an emblem or a stray board anywhere in or above the row fails", but a decorative `Icon(contentDescription = null)` (§11's idiom) is invisible to it:
- H (beside the title) leaves every Home test green, so "no decorative art … beside the title" is unguarded.
- H2 (120dp above the row) leaves `HomeHeroRowTest` green and is caught only incidentally by layout tests. A smaller emblem would likely pass them too (not run).

Suggested: assert that the weighted title's end edge meets the gear chip's start edge, and that the row's top equals the column's content top. Not blocking: the withdrawn peg cluster (a tagged `BoardCanvas`) and the 15n grep are both guarded.

**F-4 — INFO (for qa-engineer and ux-designer, not a defect against the spec). A Continue chip arriving after first paint now moves the ribbon and resizes the stage.** `HomeScreen.kt:399` puts the weighted stage above the ribbon. `HomeViewModel.kt:98` starts from `MutableStateFlow(HomeUiState())`, and the chips come from a later collector. On a device with a save, the first frame lays out the no-chip column. When the chip lands, the stage art shrinks and the ribbon rises by ≈¾ of (chip + gap), about 87dp on the 6.7-inch phone by §4.2.4's table. Before D-03l the upper `Spacer` absorbed this and the ribbon stayed put. This matches §4.2.4, and 15r's "first progress emission" is honestly tested (`HomeStageTest.kt:178`). Worth one device observation at cold start with a Classic save, alongside 15f.

**F-5 — INFO. `.onMark(...)` is composed on all four discs (`GameActionDock.kt:158`),** each keeping an `animateFloatAsState` and a `VectorPainter` though only the toggle can ever show the mark. The cost is negligible and nothing needs to change. The `@Composable` modifier factory (`DockOnMark.kt:54`) follows the project's existing `clayEntrance` convention.

**F-6 — INFO. 18a's no-ellipsis fit is measured with the platform font** (`GameStatusStripTest.kt:48`, `:86`), because Fredoka does not resolve under Robolectric. The device reading of "Peg Solitaire" and "Level 1234" at 360dp governs, and belongs with QA's 18d session.

## What holds (verified, not taken on trust)
- **Insets applied once, with no regression elsewhere.** Only `GameScreen` hosts the strip and the dock (`GameScreen.kt:188`, `:253`), and it applies the `Scaffold` padding once. The Tutorial keeps its own `safeDrawingPadding()`. `SystemBarInsets.kt` now dispatches `statusBars` and `navigationBars` separately. That fix is correct: `setInsets` on the combined `systemBars()` mask wrote the same `Insets` to every type in it. The union the Tutorial reads is unchanged, and `TutorialScreenTest` is 5/5.
- **Test teeth.** 18a, 18b and 18c (mutation A), 18e, 21a, 21b as data, the 21e mark and slash, 21d, 15p parts 1 and 2, 15q and 15r all go red under mutation.
- **§13 retargets.** `GameScreenChromeTest` and the three `HomeLayoutTest` hero-art checks were retargeted and renamed, never deleted or ignored.
- **§6.5 hard rules hold.** The stage reads no state, does no decode, loads no asset and makes no SDK call. It is one static `BoardCanvas` draw (`interactive = false`: no pointer input, no pulse), replacing the cluster's. `GameUiState.initial` is plain construction.
- **Code standards.** Tokens are in `dimens.xml` and ratios in the scoped `DockMetrics` / `HomeStageMetrics`. Every control is stateless. Modifier order is correct. Every new or changed public composable has light and dark previews. Targets are 56dp. Show-moves is a `Role.Switch` with one constant name.
- **Approved suppressions:** none requested, none added.

## Open issues / risks
- F-1 blocks the PASS. F-2 and F-3 are recommended in the same test pass. F-4 and F-6 are device observations for the qa-engineer.
- Device-only criteria (18d, 21f, 21g, the 18b back-chip check) and cold start (§6.5) are the qa-engineer's and were not run here.

## Handoff
Next: android-game-dev — add the rendered-output assertions F-1 requires (fill, ring and shadow per reachable look), watching each fail against mutations F, F2 and F3 exactly as quoted above. Optionally close F-2 and F-3 in the same pass. No production change is required. Then code-auditor re-audits the test delta. The PASS route to tech-lead waits on that.

VERDICT: FAIL
