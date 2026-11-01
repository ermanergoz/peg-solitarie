# android-game-dev report — licences-screen (fix mode: the device re-run's F-2, F-3 and F-4)
Date: 2026-10-31
Branch: feature/licences-screen (6e3d5e0..1606039)
Skills loaded: compose-skill, compose-ui, android-testing, superpowers:test-driven-development, ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin is 1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; also `.claude/rules/lessons.md`), gitflow
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0 (the one developer loop the re-run ruling owes)
Lessons applied: none (the snapshot lists 0 active lessons)

> Producer note: returned as text (the role declined to write report files); written here verbatim by the producer.

## Summary
- **Merge-in:** `develop` (ae8bd2d) merged in with no conflict. The Check is green on the merged tree: 1,337 `:app` tests, and reduced-motion's 13 suites (74 tests) pass. No licences test drives reduced motion, so nothing was retargeted.
- **F-2:** `ProvideLicencesText(edge)` gives every text on the four screens `ContentOrLtr` and LS-16's two edges. The ruled tests forced two additions: the text screens' columns place short texts at the left (D3), and the oracle re-measures each text as drawn (D2).
- **F-3:** the order is now `.widthIn(max).fillMaxWidth()` at the four licences sites and on Settings, in separate commits. At 800 dp each column is 480 dp wide and centred; at 360 dp it fills the window.
- **F-4:** the Licenses body is a non-lazy `Column`. On a phone window all five headings are in the tree at the top, middle and end of the list, and no existing test was edited.
- Every new test was watched red before its code. The Check is green at all five commits. Two runs failed before their commits, on detekt and on ktlint; both are listed under Checks.

## Outputs
- `6e3d5e0` chore(licences): merge develop again (reduced motion, RM close)
- `f7cdcdc` fix(licences): lay licence texts out by content, with LS-16's two edges
- `d1ed051` fix(licences): cap the licences columns at content_max_width
- `4fcc89e` fix(settings): cap the Settings column at content_max_width
- `1606039` fix(licences): compose the whole Licenses list for heading navigation
- New: `/Users/ermanergoz/Desktop/PegSolitarie-lic/app/src/main/java/com/app/pegsolitaire/ui/licenses/LicencesTextDirection.kt` (the wrapper) and `LicencesTextEdge.kt` (the edge enum).
- Changed: `LicensesUi.kt` (`TEXT_DIRECTION`), `LicensesScreen.kt`, `GoogleNoticesScreen.kt`, `LicenseTextScreen.kt`, `GoogleNoticeTextScreen.kt`, `LicensesTopBar.kt` and `LicenseEntryCard.kt` (chevron tag), all in the same `ui/licenses/` folder, and `ui/settings/SettingsScreen.kt`.
- New tests: `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesRtlTextDirectionTest.kt` (11) and `ContentColumnWidthCapTest.kt` (5).
- Tests added to existing files: `LicencesTalkBackContractTest.kt` (+4) and `ui/settings/SettingsScreenTest.kt` (+2).

## Step 1 — the merge-in
- **Conflict hunks:** none; `git merge --no-ff develop` auto-merged. Two files took disjoint hunks from both sides:
  - `app/build.gradle.kts` @@ -456,31 +456,29: develop folds its two `tasks.withType<Test>().configureEach` input blocks into one. The branch's `id("google-notices")` line is untouched.
  - `ui/settings/SettingsScreen.kt` @@ -56,9 +56,9: develop's KDoc lines on the live switches ("Both switches are live. Sound's gate is the sound player's own (SOUND-05)…"). The branch's About section and orders are untouched.
- **`ANIMATOR_DURATION_SCALE` before the merge:** 7 hits, all in suites develop owns and itself retargeted in 2b98c5c:
  - `GameScreenSoundTest:363`, `GameFlowTest:245`, `GameScreenHintNoticeTest:354`
  - `ResultsCelebrationSoundTest:187`, `BoardCanvasWrongTapReducedMotionTest:55`
  - `TutorialHapticsTest:80`, `TutorialSoundTest:217`
- **After the merge:** only `testing/SystemMotionScale.kt:16` (a KDoc line) and develop's own `AnimationsEnabledGateWithoutScaleTest.kt:29`.
- **Licences tests retargeted to `SystemMotionScale`:** none, because none drives reduced motion. The grep over `ui/licenses`, `ui/settings` and `ButtonSoundTest` for animat|motion|Settings.Global|durationScale|effectContext finds only `onAnimationFinished = {}` at `ButtonSoundTest:502`.
- **Reduced-motion's suites in Check 1** (74 tests, 0 failed): AnimationsEnabledGateTest 2, AnimationsEnabledGateWithoutScaleTest 1, BoardCanvasWrongTapLiveMotionTest 2, BoardCanvasWrongTapReducedMotionTest 6, ResultsStarsLiveMotionTest 1, TutorialCompletionStarLiveMotionTest 1, GameScreenCueRotationTest 4, GameScreenSoundTest 13, GameFlowTest 8, GameScreenHintNoticeTest 20, ResultsCelebrationSoundTest 7, TutorialHapticsTest 4, TutorialSoundTest 5.

## The red runs, quoted
Every run below was `./gradlew :app:testDebugUnitTest --tests '<class>' --max-workers=4`.

**F-2, run 1** (the ruled oracle as written: `getParagraphDirection(0)` read through `GetTextLayoutResult`): 11 tests, 8 failed.
- It went red only on texts inside a `SelectionContainer`:
  - "the paragraph direction of "Fredoka" — expected: Ltr, but was: Rtl"
  - "the paragraph direction of "// Copyright 2015 The Chromium Authors // …" — expected: Ltr, but was: Rtl"
- Every plain-string text reported Ltr. So `theErrorTextsAreLaidOutLeftToRightInARightToLeftLayout` passed vacuously.
- The list edge tests failed on the oracle, not the screen: "line 1 of "Fredoka": its right edge, against 379.0 px … but was 1.6039124".

**F-2, run 2** (the structural half added): 11 tests, 9 failed.
- "the text direction the style of "Peg Solitaire is made with these fonts, sounds and open-source software. Thank you to their authors." names — expected: ContentOrLtr, but was: Unspecified"

**F-2, run 3 — the recorded red** (both halves; behaviour read from the drawn layout): 11 tests, 7 failed.
- All five LS-AC19 tests fail with "the direction its style names, and its paragraph's as drawn — expected: (ContentOrLtr, Ltr), but was: (Unspecified, Rtl)". The first failing text in each:
  - the Licenses intro;
  - "Fredoka" on the text screen, and again in the Failed state;
  - the notices intro;
  - "Found in ads-mobile-sdk, hsdp, play-services-ads-identifier, play-services-appset, play-services-base, play-services-basement".
- Both text-screen edge tests fail:
  - "line 1 of "Fredoka": its left edge, against 16.0 px — expected to be at most 1.0, but was 318.6039"
  - "line 1 of "Found in ads-mobile-sdk, …": its left edge, against 16.0 px — 175.31642"
- Green, as the ruling predicts: both list edge tests, `theChromeStillMirrorsInARightToLeftLayout`, and the LTR guard.

**F-2, intermediate run** (`ContentOrLtr` in, `LayoutStart` alignment not yet in), as A1.1 asks: 11 tests, 4 failed.
- List edges:
  - "line 1 of "Fredoka": its right edge, against 379.0 px — but was 1.6039124"
  - "line 1 of "Kotlin": its right edge, against 379.0 px — but was 1.7648315"
- Text edges:
  - "line 1 of "Fredoka": its left edge, against 16.0 px — but was 317.0"
  - "line 1 of "Notice 2 of 4": its left edge, against 16.0 px — but was 295.0"
- All five direction tests are green.
- An earlier intermediate run had a fifth failure, "missing (1): None". It was a `toSet()` precedence slip in my own coverage assertion, fixed in the test and re-run clean.

**F-2, alignment added:** 11 tests, 2 failed — the two text-screen edge tests again ("Fredoka" 317.0, "Notice 2 of 4" 295.0). This is D3. After the container alignment: 11/11 green.

**F-3, licences:** `ContentColumnWidthCapTest`, 5 tests, 4 failed.
- "the width of licenses_list on a 800px window, against the cap — expected: 480.0, but was: 800.0"
- The same for `license_text_column`, `google_notices_list` and `google_notice_text_list`.
- The narrow-window guard passed (it is the guard).

**F-3, Settings:** `SettingsScreenTest`, 9 tests, 1 failed.
- "the Settings column's width on a 800px window, against the cap — expected: 480.0, but was: 800.0"
- The narrow-window guard passed.

**F-4:** `LicencesTalkBackContractTest`, 11 tests, 4 failed.
- At the top: ""Open-source software" in the tree as a heading, with the list at its top | Reason: Expected exactly '1' node but could not find any node that satisfies…"
- In the middle: ""Fonts" … with the list scrolled to its middle …"
- At the end: ""Fonts" … with the list scrolled to its end …"
- Unscrolled: "cogfirestudios-positive-blip: its title in the tree, unscrolled, on a phone's window | Reason: … could not find any node …"
- The seven existing tall-window tests passed.

## Fallback taken (the F-2 structural oracle) and how the behavioural half was kept
- **Why the ruled oracle fails.** In foundation 1.11.3, a plain-string `Text` (`TextStringSimpleNode.kt:332`) answers `GetTextLayoutResult` with `ParagraphLayoutCache.slowCreateTextLayoutResultOrNull` (lines 366–402).
  - That function re-lays the text out from the style as given, without `resolveDefaults` against the layout direction.
  - It also lays out at the incoming maximum width (`prevConstraints.copyMaxDimensions()`), not the text's own width.
  - The drawn path does resolve defaults (`ParagraphLayoutCache.kt:277`, `MultiParagraphLayoutCache.kt:343`).
  - So wherever the style names no direction, the action reports a left-to-right, start-aligned paragraph, whatever is drawn. The oracle cannot go red on those nodes, which is the ruling's fallback case.
- **Structural half (the fallback):** on every text node, `layoutInput.style.textDirection == LicensesUi.TEXT_DIRECTION`.
- **Behavioural half, kept and made faithful:** each node's own layout input is measured again with `TextMeasurer`. Its `layout()` resolves defaults against the layout direction (ui-text 1.11.3, `TextMeasurer.kt:273`) and sizes the paragraph by the drawn path's width rule (lines 306–330).
  - Each node then asserts that the re-measured width equals its drawn width.
  - The paragraph direction, and every line's left and right edge, are read from that re-measured result.
- With this oracle, every red and green fell exactly where the ruling predicted (run 3 above).

## Existing tests edited
- No existing test's body or assertion was changed.
- `LicencesTalkBackContractTest.kt` gained 4 tests, 2 private helpers, 2 constants and imports.
- `SettingsScreenTest.kt` gained 2 tests, 3 constants and imports.
- `LicensesNavFlowTest`, `ButtonSoundTest`, `LicensesScreenTest` and LS-AC02's round trips pass unedited on the `Column`.

## Departures from the ruling, each for the reviewers
- **D1 — test window.** The RTL tests run on `w411dp-h891dp`, with native graphics. Probe 8 says none runs on the 4,000 dp window; F-2's setup named the tall window. I followed probe 8 and kept F-2's "every text composed" intent: `atEveryPositionOf` scrolls each lazy list through. Geometry is read unclipped (`positionInRoot`, `size`), because an off-screen node's clipped bounds are empty.
- **D2 — the oracle.** See the fallback section above.
- **D3 — LS-16 on the text screens.**
  - A1.1's `TextStart` (`TextAlign.Start`) hangs lines from the left only within each text's own box.
  - A short text's box is placed by its column's layout-relative `Alignment.Start`, which is the right in RTL.
  - So `LicencesTextEdge` also carries a `horizontalAlignment`: `LayoutStart` → `Alignment.Start`, `TextStart` → `AbsoluteAlignment.Left`.
  - The text screens apply it to `TextColumn`, to `EntryHeading`'s `Column`, and to the notice `LazyColumn`, through one `BODY_EDGE` per screen. Probe 2b's swap therefore moves both together.
  - Nothing changes in an LTR layout.
- **D4 — file split.** The enum lives in its own `LicencesTextEdge.kt`. detekt's `MatchingDeclarationName` failed a wrapper file whose single class is the enum. The wrapper stays in the ruled `LicencesTextDirection.kt`. No suppression.
- **D5 — column tag name.** `LICENSE_TEXT_COLUMN_TAG` replaces the ruled `LICENSE_TEXT_BODY_TAG`, because that name already tags the `SelectionContainer` used by LS-AC05's structural oracle (`LicenseTextScreenTest:144`).
- **D6 — chevron tag.** A test tag, `LICENCE_CARD_CHEVRON_TAG`, on the tappable card's decorative chevron. It adds no description and no TalkBack stop, and lets the chrome test find which side the chevron is on.
- **D7 — commit subjects** shortened to rules/gitflow.md's 72-character limit: F-2 from 75 to 71 characters ("the" dropped), F-4 from 78 to 69.
- **D8 — typography.** The wrapper merges the 15 public styles through `Typography.copy`. Material 3 1.4.0's 15 emphasized styles are `internal` and pass through unchanged; no text on the four screens uses them. The public `MaterialTheme` overload passes `motionScheme` through.
- **D9 — naming.** `LicensesItems` is PascalCase, a Unit-returning composable (lint's ComposableNaming), written as the ruled `ColumnScope` extension.

## Checks
- `./scripts/check.sh --max-workers=4` at `6e3d5e0` — pass (11m 18s; `:app` 1,337 tests, 0 failed)
- `./scripts/check.sh --max-workers=4` on the F-2 tree, first run — fail (`:app:detekt` MatchingDeclarationName at `LicencesTextDirection.kt:18:21`; fixed by D4, then `./gradlew :app:ktlintFormat` on the new test)
- `./scripts/check.sh --max-workers=4` at `f7cdcdc` — pass (11m 10s; 1,348 tests)
- `./scripts/check.sh --max-workers=4` on the F-3 tree, first run — fail (`:app:ktlintTestSourceSetCheck`, `ContentColumnWidthCapTest.kt:89, :103`, "Expected newline before '.'"; ktlintFormat fixed the test file only)
- `./scripts/check.sh --max-workers=4` at `d1ed051` — pass (11m 7s; 1,353 tests)
- `./scripts/check.sh --max-workers=4` at `4fcc89e` — pass (11m 7s; 1,355 tests)
- `./scripts/check.sh --max-workers=4` at `1606039` — pass (11m 7s; 1,359 tests)
- `grep -rn 'textAlign\|textDirection' app/src/main/java/com/app/pegsolitaire/ui/licenses` — pass (only `LicencesTextDirection.kt`); no `LocalLayoutDirection provides` anywhere
- `grep -rn -A1 '\.fillMaxWidth()$' …/ui/licenses …/ui/settings | grep 'widthIn(max'` — pass (none)
- `git diff --name-only 6e3d5e0 1606039` — pass (only `ui/licenses/`, `ui/settings/SettingsScreen.kt` and tests; nothing on the frozen list); `git diff --name-only develop...HEAD`, filtered for the 15e void list — none
- `git diff 6e3d5e0 -- app/src/main | grep -E '^\+.*(Intent|Uri|http|URL|Socket)'` — pass (none); no new dp/sp literal, dimens token or coordinate

## Open issues / risks
1. D3 adds to A1.1's mechanism. On the text screens in RTL, short lines now sit at the left, as LS-16 words it; the tech-lead confirms this at sign-off.
2. A house testing caveat, a candidate retro lesson: `GetTextLayoutResult` on a plain `Text` does not show the drawn direction, alignment or width when the style names none. A future RTL or alignment oracle needs the re-measure used here.
3. App-wide RTL paragraphs (Home, Settings, the game, Results) are untouched, as routed in the ruling's F-2 point 5. Settings is capped now, but its texts are still RTL-laid in `[ar-XB]`.
4. The device half is pending: QA's re-check items 1–4.
5. Staffing: written on Opus. Q-019's mitigation applies to the review.

## Handoff
Next:
- **producer:** write this report to the path above and commit it alone.
- **code-auditor:**
  - the diff `1b79094..1606039`, read from the code at `bd3ce00`, plus `6e3d5e0`'s two auto-merged hunks;
  - probes 1–8, with 2, 2a, 2b and 2c as amended;
  - D2 to D6 in particular.
- **qa-engineer:** the re-check list:
  - `[ar-XB]` alone, on all four screens;
  - 800 × 1280 and 360 × 640 dp, Settings included;
  - heading navigation from the top, the middle and the bottom;
  - the `Column` and merge-in regressions.
- **tech-lead:** sign-off, re-reading the merge-in hunks and departures D1–D9.
