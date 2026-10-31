# android-game-dev report — phase-15 H-1 fix: F-1, F-2, F-3 and the LEGACY ruling (fix mode)
Date: 2026-10-21
Branch: feature/phase-15-ux-refresh (887c97d..af08b86)
Skills loaded: ergoz-soft:standards (role android-game-dev). The product's rules snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin's copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing. Also the product's `.claude/rules/lessons.md`. Then superpowers:test-driven-development (with its `writing-good-tests.md`), gitflow, compose-skill, compose-ui, android-testing.
Ran in parallel with: tr-TR store copy draft (release-manager, docs only)
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **Test-only, as dispatched.** Four commits touch two test files. `app/src/main`, `domain` and every other production file are unchanged, and no suppression was added.
- **F-1 and items 1–5:** `HomeLayoutTest` runs on NATIVE, in the app theme (the fixture now wraps Home in `AppTheme(darkTheme = false)`). 15k part 2 is asserted conditionally. Each 15k test also runs on `w360dp-h560dp`, which must overflow into a band. The trailing-gap edge is decided by §4.2.3's own definition of a false bottom.
- **F-2:** the compact-pill test now reads the pixels and sees a glyph. It was red against M3b.
- **F-3:** the 412dp test now reads the band gap, which tells the full set from the compact one. It was red against M5.
- **The Check is green on `af08b86`:** `BUILD SUCCESSFUL in 11m 12s`, exit 0. `:app` ran 1,067 tests (H-2's 1,064 plus the three new 15k tests), 0 failed, 0 skipped.

## Outputs
Commits (each body carries its red-then-green record):
- `887c97d` test(15-h1): render Home's shared test fixture in the app theme — item 2.
- `4f95561` test(15-h1): read 15k on real metrics, conditionally, on two windows — F-1, items 1, 3, 4, 5.
- `1757e7a` test(15-h1): see a glyph on the compact pill in its pixels (F-2).
- `af08b86` test(15-h1): tell the full band set from the compact at 412dp (F-3).

Files:
- `app/src/test/java/com/app/pegsolitaire/ui/home/fixture/HomeTestFixture.kt` — `renderHome` composes `HomeScreen` inside `AppTheme(darkTheme = false)`, the theme `MainActivity` draws. Light is stated, not read from the host's night mode.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt` — 13 tests become 16:
  - `@GraphicsMode(NATIVE)`. The class KDoc says why (LEGACY's 44dp of overflow that the device does not have).
  - 15k as three shared assertions read on two windows: `w360dp-h640dp` (the criterion's own) and `w360dp-h560dp` (`OVERFLOWING_PHONE_QUALIFIER`, same 24/48 insets).
    - Part 1: `theChipsAndTheFirstTileAreCompleteInTheFirstViewportOn{TheNarrowBaseline,ANarrowPhoneThatOverflows}`.
    - Part 2: `theFoldLeavesNoFalseBottomOnTheNarrowBaseline` (renamed from `theFoldCutsThroughTheSecondModeTileAndNotThroughAGap`) and `theFoldCutsThroughTheSecondModeTileOnANarrowPhoneThatOverflows`.
    - Part 3: `oneShortScrollCompletesHomeOn{TheNarrowBaseline,ANarrowPhoneThatOverflows}`.
  - F-2: `theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph` gains `assertEachPillPaintsOnlyItsSurfaceAndItsTexts()`.
  - F-3: `theStageArtIsPresentOnAPhoneAboveTheCompactThreshold` is renamed `aWidePhoneKeepsTheFullBandSetWithTheStageArtPresent` and gains the band-gap assertion.
- This report, in its own commit.

### How the conditional reads (items 3 and 5)
- **Part 2**, on the last band (the Challenge tile; part 1 holds every band above it whole):
  - In both branches, its top edge is inside the first viewport: displayed, below the status bar and above the fold. A fold above that edge is a fold through the gap above the tile.
  - **If a scroll moves the tile**, the fold cut through it.
  - **If not**, every band is whole, and only the column's trailing space may lie past the fold. That space is the gap after the last band plus the bottom padding, `spacing_sm` each, so 16dp. More than that is something hidden below a gap.
- **Item 5, decided rather than routed.** "Overflows" means "overflows into a band". §4.2.3 defines a false bottom as a fold in a gap *between two bands*, which hides the lower one. The trailing space has no band below it, so a fold that lands in it hides nothing. Read literally, the criterion would fail S9's M1b-N layout (12dp over, tile whole) for a fold that hides nothing. The ux-designer may still confirm this (open issue 1).
- **Part 3** holds in both branches as written. Where the column fits, the overflow is 0 and the scroll moves nothing. On h560 the bound and the reveal are read for real.
- **Part 4's "no stage art on an overflowing column"** (§13's retarget) is now asserted only when the column overflows at all. At 360×640 on real metrics the column fits, and the stage gets 42dp (below the 48dp art floor). The art assertion bites on h560.
- **Liveness guard.** `renderTheOverflowingNarrowWindow()` fails if h560 ever stops overflowing past its trailing space, rather than letting the overflow halves pass unread.

## Checks
All commands ran from `/Users/ermanergoz/Desktop/PegSolitarie-p15` with `ANDROID_HOME=$HOME/Library/Android/sdk`. Test verdicts and messages were read from `app/build/test-results/testDebugUnitTest/TEST-<class>.xml`. Each mutation was a production edit that I ran and then restored with `git checkout -- <file>`, after which `git diff --quiet -- app/src/main` was clean.

### Measurements (throwaway probe test, never committed)
These were read on NATIVE, in the app theme, with both chips, 24/48 insets and the fold at the root's bottom less 48:
- `w360dp-h640dp`: overflow 0dp; stage 42dp, no art; Classic tile 362..458; Challenge tile 466..562, against the 592 fold.
- `w360dp-h560dp`: overflow 24dp; stage 0; Classic tile 320..416; Challenge tile 424..520, against the 512 fold.
- `w412dp-h915dp`, zero insets: tile gap 16dp (603..699, then 715..811).
- Compact-pill pixel read: 0 stray pixels on all three pills at h640, h560 and h800. On the full pills at w412, the same read found the glyph ink of the Boards and Level pills: 196 px and 85 px.

### Item 2 — `887c97d`
- `./gradlew :app:testDebugUnitTest --tests '…HomeLayoutTest' --tests '…HomeScreenTest' --tests '…HomeStageTest' --tests '…HomeHeroRowTest' --tests '…ButtonSoundTest' --rerun` — pass. The counts were 13, 24, 10, 9 and 15, with 0 failures; `HomeLayoutTest` was still on LEGACY at this point. No verdict changed.

### F-1 — `4f95561`: red, then green
Each run: `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest' --rerun`.
1. **`@GraphicsMode(NATIVE)` alone, old test bodies:** fail, 1 of 13. `theFoldCutsThroughTheSecondModeTileAndNotThroughAGap`: "expected to be less than: 466.0.dp but was: 466.0.dp". This is the audit's M6a/M6b message.
2. **Rewritten:** pass, 16 of 16.
3. **M1c, `.heightIn(min = 172.dp)` on the hero row** (`HomeHero.kt`): fail, 3 of 16, exactly the three h560 tests.
   - Part 2: "The component with Text + InputText + EditableText contains 'Challenge' (ignoreCase: false) is not displayed!"
   - Part 1: "expected: 444.0.dp but was: 416.0.dp".
   - Part 3: "expected to be at most: 120.0.dp but was: 148.0.dp".
   - The 360×640 tests stayed green. On real metrics that window cannot see M1c, which is why h560 exists.
4. **M-edge, `.heightIn(min = 116.dp)`**, which reproduces M1b-N's geometry on HEAD: the probe read 12dp of overflow, with the tile whole at 492..588 against 592. All 16 pass. This is item 5's reading; the old unconditional test fails such a layout.
5. **M10, `Spacer(Modifier.height(72.dp))` after the Challenge tile** (a band hidden below a gap): fail, 1 of 16. `theFoldLeavesNoFalseBottomOnTheNarrowBaseline`: "every band is whole, so only the column's trailing space may lie past the fold / expected to be at most: 16.0.dp / but was: 24.0.dp". The fit branch has teeth.
6. **M-stage, after the Check:** `HomeStage(modifier = Modifier.height(48.dp))` instead of its weight, i.e. a stage that does not yield. Fail, 3 of 16.
   - On h560, the part-1 test failed on the conditional art assertion: "Did not expect any node but found '1' node that satisfies: (TestTag = 'home_stage_art')".
   - The 360×640 part-1 test stayed green, because that column fits and a board is allowed there.
   - The two 15g midpoint tests failed too ("at least: 457.5.dp but was: 436.0.dp" and "…244.0.dp"). That is right for a stage that stops taking the slack.
7. **Restored:** pass, 16 of 16.

### F-2 — `1757e7a`: red, then green
1. **At HEAD:** pass.
2. **M3b**, the full pill's own glyph put back inline before the label in `CompactPillContent` (the same `Icon`, its tint, gold fill when gilded, 20dp): fail, 1 of 16. The message: "pill 0 paints 131 px besides its surface and its texts, x 29..46, y 174..189 (px) / expected: 0 / but was: 131". The other 15 stayed green, the old one-line and ≤ 56dp checks included.
3. **Restored:** pass, 16 of 16.

### F-3 — `af08b86`: red, then green
1. **At HEAD:** pass, gap 16dp.
2. **M5**, `compact = availableHeight < compactThreshold || !narrow` (`HomeBandSet.kt:82`): fail, 1 of 16. The message: "the band gap between the two mode tiles, in dp / expected: 16.0 / but was: 8.0 / outside tolerance: 0.5".
3. **Restored:** pass, 16 of 16.

### Static checks, scope, the Check
- `./gradlew :app:ktlintTestSourceSetCheck :app:detekt` — pass, after each commit's edits. A first run on F-1 failed on two ktlint layout rules in `dimen()`; I fixed it before that commit.
- `git diff --stat 2e7094e..HEAD -- app/src/main domain` — empty.
- `git diff 2e7094e..HEAD | grep '^+' | grep -E '@Suppress|@Ignore|baseline|ktlint-disable|detekt:disable|assume'` — 2 lines, both prose: "the design baseline's system bars" and "Material 3's baseline faces". No suppression, `@Ignore`, baseline file or disable was added.
- `cd /Users/ermanergoz/Desktop/PegSolitarie-p15 && ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew --no-daemon check` on `af08b86` — **pass**: exit 0, `BUILD SUCCESSFUL in 11m 12s`, 98 actionable tasks (14 executed, 84 up-to-date). It ran in the background (started 14:35:01Z), and I waited on its exit file with bounded polls. No other Gradle process ran meanwhile.
  - `:app:testDebugUnitTest` executed: 99 classes, 1,067 tests, 0 failures, 0 errors, 0 skipped. `HomeLayoutTest` was 16 of 16.
  - `:domain:depthSweep` executed, 1 of 1. `:app:lintDebug` executed, pass. The `adId`, `gamesId`, `audioFocus` and `dpSp` literal gates executed for both modules, pass.
  - Up-to-date because their inputs did not change: `:domain:test` (274 tests, 0 failures), since nothing in `:domain` changed and `build/fuzz-failures.txt` is absent; `:build-logic:test` (54); `:app:detekt` and `:app:ktlintCheck`. The last two last ran on this same tree (above).
- Before the Check, `./gradlew --stop` and `pkill -f KotlinCompileDaemon`. After the last run, the same again; `pgrep -fl 'GradleDaemon|KotlinCompileDaemon'` then found no process. No other Gradle build ran during the Check. I never touched the `Pixel_9a_bench` emulator on `emulator-5554`.

## Open issues / risks
1. **Item 5 is a spec reading.** I took it from §4.2.3's definition of a false bottom, so I did not route it. If the ux-designer reads 15k part 2's "overflows at all" literally, the one place to change is `assertTheFoldLeavesNoFalseBottom`'s fit branch. Non-blocking.
2. **The liveness window is a harness choice.** `w360dp-h560dp` is no device the criterion names. If a later layout change makes it fit, its precondition fails on purpose, and the fix is a shorter window, not a weaker assertion.
3. **The pixel read assumes the light theme and the flat render.** It compares against `LightColors.surfaceContainerHigh`, because the fixture draws light. A future gradient or texture on the pill surface would need the read to change with it.
4. **Observation for whoever next audits semantics.** In the *full* pill, the gilded Stars glyph (`goldGradientFill`) shows up in the unmerged tree as a node with no text and no description; the other two glyphs do not. This is harmless to TalkBack (it is inside `clearAndSetSemantics`). It is why the F-2 read excludes only text nodes: a glyph that carried a node would still be read.
5. **The audit's INFO items in production files are untouched**, because this dispatch is test-only:
   - `HomeScreen.kt:272-277`, `:350-359` ("~72dp over") and the compact preview's KDoc (the fold "cut[s] by the bottom edge"). On real metrics, 360×640 fits.
   - `HomeBandSet.kt:45` ("under a full hero").
   - `HomeStageTest`'s class KDoc (LEGACY "one dp wide"; LEGACY in fact over-wraps).

   For the producer to route.
6. **The 15e sitting (H-3) stands.** No start-path file changed: everything here is under `app/src/test`. Nothing in `app/src/main` changed.

## Handoff
Next: code-auditor — re-audit this delta only (`887c97d..af08b86`, two test files), against `code-auditor-loop4.md`'s F-1, F-2, F-3 and items 1–5. The red-then-green records are above and in each commit body. Give it a new report path. No QA re-walk is owed: the change is test-only. Then the tech-lead's H-1 closure, per `tech-lead-signoff.md`.
