# code-auditor report — licences-screen (review of the re-run fixes: F-2, F-3, F-4)
Date: 2026-10-31
Branch: feature/licences-screen (report only) — reviews `1b79094..1606039` and the two auto-merged hunks of `6e3d5e0`, at head `e26dcd1`
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort high. **Deviation:** my agent file pins fable at xhigh. The founder's Q-019 answer puts every Fable-pinned role on Opus, and the founder's directive of 2026-10-31 sets every role one effort step lower. So Opus is reviewing Opus code here. Q-019's mitigation applies: I ran the Check myself, with the source tasks forced, and ran every probe myself.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **The production code is correct.** I found no defect in it. The forced Check is green: 1,359 `:app` tests, 0 failed. Probes 1 and 2–8 hold, and I accept departures D1–D9, D2 and D3 included.
- **Two ruled mechanism choices have no test that guards them.** I proved each by mutation. Removing the top bar's `LayoutStart` wrapper leaves all 36 tests of the four suites green. So does switching `TEXT_DIRECTION` from `ContentOrLtr` to `Content`.
- **Both gaps are reachable with real data.** Shipped notice names wrap in the top bar. 318 shipped paragraphs have no strong character. With a fixture of each, the existing tests are green on the head and red on the mutation.
- **The fixes are test-only:** two fixtures and one wrap assertion. No production code changes.
- **VERDICT: FAIL.**

## Findings (must fix)
- `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesRtlTextDirectionTest.kt:221` (and the LTR guard's `assertTopBarTitleHangsFrom`, `:303`, `:498-501`) — **the top bar title's edge check cannot fail.**
  - Every fixture title is one line: "Licenses", the licence names, "Google notices", and "Cronet" (`:653`).
  - A one-line `Text`'s box is its line, so `assertLinesHangFrom(title, title.edge(Right), Right)` holds for any alignment.
  - **Mutation M8:** replace `ProvideLicencesText(LicencesTextEdge.LayoutStart)` at `LicensesTopBar.kt:44` with `run`. All 36 tests stay green. I reproduced this in a second, clean worktree.
  - **The gap is reachable.** The real notice index has wrapping names, such as the 47-character "AndroidX lifecycle viewmodel savedstate library". With that name as the fixture's `NOTICE` name:
    - green on the head;
    - red with M8: `line 1 of "AndroidX lifecycle viewmodel savedstate library": its right edge, against 355.0 px … but was 53.695312`.
  - Without the wrapper, the text screens' outer `TextStart` reaches the title, so its wrapped lines hang from the left under mirrored chrome. A1.1 (`LicensesTopBar` takes `LayoutStart` on all four screens) and LS-16 ("the top bar is chrome and mirrors") are then unguarded.
  - A1.1 also asks that "the fixture must contain wrapped lines, so the check bites", and the edge tests do this with their `wrapped` sets. The title check does not.
  - **Fix:** give a text-screen fixture a title that wraps on the 411 dp window (a real long notice name), and assert that it wrapped, in RTL and in the LTR guard.
- `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicensesUi.kt:30`, guarded only by `LicencesRtlTextDirectionTest.kt:426-438` — **the ruled value `ContentOrLtr` is unpinned.**
  - The ruling's F-2 point 1 chose `ContentOrLtr` over `Content` for one reason: a paragraph with no strong character must fall back to left to right, not to the layout's direction.
  - The structural half compares the style against the constant itself. And no fixture paragraph lacks a strong character. So nothing checks the value.
  - **Mutation M11:** change `TextDirection.ContentOrLtr` to `TextDirection.Content`. All 36 tests stay green. I reproduced this in the second worktree.
  - **The gap is reachable.** 318 blank-line paragraphs of the shipped texts have no strong character: dash rules such as `------------------`, in notice `c15ad8d2096909c1` and others. I appended one to the notice-text fixture (`noticeParagraphs`):
    - green on the head;
    - red with M11 on `everyTextOnAGoogleNoticeTextIsLaidOutLeftToRightInARightToLeftLayout`: `"------------------": … expected: (Content, Ltr) but was : (Content, Rtl)`;
    - red with M11 on `everyLineOfAGoogleNoticeTextHangsFromTheLeftEdgeInARightToLeftLayout`.
  - On the device, under `Content`, such a rule would hang from the right on a notice text in `[ar-XB]`. That breaks LS-16's left edge.
  - **Fix:** put a paragraph with no strong character in the notice-text fixture, and in a curated-text fixture, so that the existing LS-AC19 and LS-16 oracles run on it.

## Probes (the ruling's 1–8, with 2a–2c as amended)
All runs used a throwaway `git worktree add --detach` of `e26dcd1` in the scratchpad, removed afterwards. Each run covered the four suites `LicencesRtlTextDirectionTest`, `ContentColumnWidthCapTest`, `LicencesTalkBackContractTest` and `SettingsScreenTest`: 36 tests, green on the unmutated baseline.

| Probe | Mutation or check | Result |
|---|---|---|
| 1 | grep: no `.fillMaxWidth()` is followed by `.widthIn(max` in `ui/licenses`, `ui/settings` | pass (none) |
| 1 | M1: Licenses column back to `fillMaxWidth().widthIn(max)` | red: `theLicensesColumnIsCapped…` "480.0 but was 800.0" |
| 1 (own) | M10: the same on Settings | red: `theSettingsColumnIsCapped…` "480.0 but was 800.0" |
| 2 | M2: wrapper removed from `GoogleNoticesScreen` | red: the notices LS-AC19 test and the error-texts test, "(Unspecified, Rtl)" |
| 2 (own) | M12: wrapper removed from `LicenseTextScreen` | red: the License-text LS-AC19 test, the error-texts test and the text edge test |
| 2 | Wrapper source: `colorScheme`/`shapes` default to the enclosing theme, and the public 4-argument M3 1.4.0 overload passes `MaterialTheme.motionScheme` (read in the sources jar); `derived` is `remember(typography, edge, layoutDirection)`; the direction is `LicensesUi.TEXT_DIRECTION` | pass |
| 2a | `grep -rn 'textAlign\|textDirection' …/ui/licenses` | pass: only `LicencesTextDirection.kt` and `LicensesUi.kt` |
| 2b | M4: Licenses list takes `TextStart` | red: `everyLineOnTheLicensesList…` "right edge … was 1.6039124" |
| 2b | M5: License text takes `LayoutStart` (`BODY_EDGE`; moves the line and the container edge together, D3) | red: `everyLineOfALicenseText…` "left edge … was 318.6039" |
| 2c | M6: `LayoutStart` alignment set to `Start`, direction kept | red: both list edge tests ("Fredoka", "Kotlin") |
| 3 | M3a: the whole License text screen inside `LocalLayoutDirection provides Ltr`, wrapper kept | red: `theChromeStillMirrors…` "back button's left edge … at least 298.0 but was 8.0" |
| 3 | M3b: the Licenses body only inside `provides Ltr` | red: the chrome test (chevron) and the list edge test |
| 4 | M7: `LicensesScreen.kt` as of `6e3d5e0` (lazy) | red: the three heading-position tests and the entry test, plus the old file's cap and direction |
| 4 | M7b: the head's file, the body alone made a `LazyColumn` of one item per child | red: exactly the four new TalkBack tests ("Fonts", "Open-source software" not found) |
| 4 | LS-AC02 round trips | pass unedited: `LicensesNavFlowTest` 5/5 in the Check, and no existing test line is removed in `6e3d5e0..1606039` |
| 4 | Stagger cap: every item and header goes through `LicensesUi.entranceOrderOf(order++)`, null from `STAGGERED_ROWS` | pass (code read; `LicensesUiTest` covers the function) |
| D3 (own) | M9: notice-text list's `horizontalAlignment` removed / M9b: License text column's removed | red: "Notice 2 of 4" left edge 295.0 / "Fredoka" left edge 141.0 |
| own | M8: top bar wrapper removed | **survives**: finding 1 |
| own | M11: `ContentOrLtr` changed to `Content` | **survives**: finding 2 |
| 5 | `git diff --name-only develop...HEAD` against the void list (`App.kt`, `MainActivity.kt`, `ui/home/`, `ui/theme/`, `res/font/`, `themes.xml`, the manifest) | pass: none. The five added `@Inject`s are in the licences sources and ViewModels, none in `App` |
| 6 | `Intent\|Uri\|http\|URL\|Socket` over `ui/licenses`, `data/licenses` | pass: only "okhttp" in catalogue ids and strings; no `Intent`, `Uri`, `URL` or `Socket` |
| 6 | asset reads | pass: `.open(` in exactly `AssetLicenseTextSource.kt:30` and `AssetGoogleNoticeSource.kt:31` |
| 7 | `1b79094..1606039` touches no `libs.versions.toml`, `build-logic/build.gradle.kts` or `res/`; no added dp/sp literal; `LicensesScreen`, `LicenseTextScreen`, `GoogleNotices*` keep their light and dark `@Preview`s; the new `internal` wrapper and enum are not public composables | pass |
| 8 | windows: RTL test `w411dp-h891dp` with `GraphicsMode.NATIVE`; cap tests `w800dp-h1280dp` and method-level `w360dp-h640dp`; the TalkBack tests are method-level `w411dp-h891dp`; Settings `w800dp`/`w360dp`; no new test on the 4,000 dp window | pass |

## The merge-in, `6e3d5e0`
- No conflict. The merge-base is `6aa7624`.
- For `app/build.gradle.kts` and `ui/settings/SettingsScreen.kt`, I compared:
  - develop's own change (`6aa7624..ae8bd2d`) against the merge's change over the branch parent (`cf54a49..6e3d5e0`);
  - the branch's own change (`6aa7624..cf54a49`) against the merge over develop (`ae8bd2d..6e3d5e0`).

  Both pairs match line for line. Each side is carried exactly, and nothing was added.
- The two hunks are develop's folded test-input block, and develop's KDoc on the live switches.

## Departures D1–D9
- **D1 (the phone window with scroll-through).** Accepted. It is what probe 8 asks, and every lazy item is still checked at some position.
- **D2 (the re-measuring oracle).** Accepted. I verified the cause in `foundation-android-1.11.3-sources.jar`:
  - `ParagraphLayoutCache.slowCreateTextLayoutResultOrNull` (line 366) builds `MultiParagraphIntrinsics` from the style as given, with no `resolveDefaults`, at `prevConstraints.copyMaxDimensions()` (line 373).
  - The drawn path resolves defaults (line 277).
  - So on a plain-string `Text`, the ruled oracle reads Ltr whatever is drawn. That is the ruling's own fallback case.
  - The re-measure with `TextMeasurer` resolves defaults the way the drawn path does. It is guarded by asserting the re-measured width equals the drawn width. Mutations M2, M4, M5, M6 and M12 show it goes red on exactly the predicted texts.
  - Its one blind spot is a value shared by the code and the oracle. That is finding 2.
- **D3 (`AbsoluteAlignment.Left` on the text screens' containers).** Accepted.
  - LS-16 puts a text screen's body on the left edge. A short text's box is placed by its column, so `TextAlign.Start` alone cannot meet it.
  - Nothing moves in LTR. The spinner is unaffected: it is a `fillMaxSize` box.
  - M9 and M9b show it is guarded.
- **D4–D9.** Accepted.
  - D4: the file split is detekt's naming rule, with no suppression.
  - D5: the tag is renamed to avoid a clash.
  - D6: the chevron `testTag` adds no description and no focus. The existing inert-card and chevron contract tests pass.
  - D7: the commit subjects are 71 and 69 characters.
  - D8: the 15 public `Typography` styles are merged. The emphasized styles are `internal` in M3 1.4.0 (`Typography.kt:120-159`), and no text on the four screens uses them.
  - D9: `LicensesItems` is PascalCase, for lint's ComposableNaming.

## Review notes (no action owed)
- **Architecture and UDF are unchanged.** The screens stay stateless, and Licenses still has no ViewModel (LS-09).
- **The nested `MaterialTheme` also re-provides two other locals:** `LocalIndication = ripple()` and `LocalTextSelectionColors` from the same scheme. Today they equal `AppTheme`'s, which sets neither. It also resets `LocalTextStyle` to the derived `bodyLarge`. The KDoc's "the colours, shapes and motion are the enclosing theme's" is true. The wrapper is temporary (ruling F-2, point 5).
- **Performance.**
  - The `derived` typography is remembered.
  - `ripple()` and `rememberTextSelectionColors` per wrapper are cheap.
  - The Licenses `Column` composes about 70 `Text`s once at open, as the ruling accepted.
  - There is no allocation in a hot path, and the item lambdas stay remembered in `LicenseEntryItem`.
- **Security.** No network, no `Intent`, no secrets, and no new input surface.
- **Dependencies.** No new dependency, so `THIRD_PARTY.md` needs no row.
- **Suppressions.** None added: no `@Suppress`, baseline, `@Ignore` or `!!` in `6e3d5e0..1606039`. There is nothing to approve.
- **A stale comment.** `LicencesTalkBackContractTest.kt:279-280`, the `TALKBACK_TALL_WINDOW` comment ("the lazy lists"), is now only true of the two Google screens. It is cosmetic.
- **Foreign files in the scratchpad.** My first throwaway worktree, at a generic scratchpad path, showed three untracked files I did not write (`fails.sh`, `p1.py`, `p2.py`) when I removed it. To rule out interference, I re-ran the baseline, M8, M11 and both reachability pairs in a second worktree under a unique name. The results were identical, and no foreign file appeared.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/code-auditor-rerun.md` — this report, committed alone. No source file was edited. The mutations lived only in the two throwaway worktrees, both removed.

## Checks
- `./scripts/check.sh --max-workers=4 --console=plain :app:compileDebugKotlin --rerun :app:compileDebugUnitTestKotlin --rerun :app:testDebugUnitTest --rerun :app:detekt --rerun :app:runKtlintCheckOverMainSourceSet --rerun :app:runKtlintCheckOverTestSourceSet --rerun :app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun` at `e26dcd1`, clean tree, 19:26:51Z to 19:38:00Z — **pass**.
  - Output: `BUILD SUCCESSFUL in 11m 8s`, `107 actionable tasks: 20 executed, 87 up-to-date`, exit 0.
  - Every forced task printed as executed, and `:app:check` ran.
  - `:app` tests: 1,359, 0 failed, 0 skipped. Classes started 19:27:20Z to 19:30:20Z. Among them: `LicencesRtlTextDirectionTest` 11, `ContentColumnWidthCapTest` 5, `LicencesTalkBackContractTest` 11, `SettingsScreenTest` 9, `LicensesNavFlowTest` 5.
  - Lint: `0 errors, 64 warnings`, none in `ui/licenses` or `ui/settings`. detekt: 0 issues.
- The same command without the test-compile force, 19:07:28Z to 19:18:43Z — exit 0, `BUILD SUCCESSFUL in 11m 14s`. Its captured log lacks the `:app:testDebugUnitTest` and `:app:check` lines, although that run's result files exist. I do not rely on it; the run above is the one of record.
- The mutation runs: 16, then 3, then 4 reachability runs. Each was `./gradlew :app:testDebugUnitTest --max-workers=4 --console=plain --offline --tests <the suites above>` in a throwaway worktree. The results are in the Probes table and the Findings. Baseline: 36 tests, 0 failed, both times.
- `git diff` comparisons of the merge sides for the two auto-merged files — pass (each side carried exactly).
- A Python scan of the shipped notice and licence texts for blank-line paragraphs with no strong bidi character: 51 files, 318 such paragraphs.

## Open issues / risks
- The two findings are test gaps on behaviour that is correct today. A later edit to the top bar, or to the constant, would go unnoticed until a device run in `[ar-XB]`.
- The device half of LS-15 and LS-16 is still QA's re-check (ruling, items 1–4, as amended).
- Staffing: an Opus review of Opus code (Q-019). The forced Check and the mutations are the mitigation.

## Handoff
Next: producer — route the two findings to the **android-game-dev** (fix mode). The fix is test-only:
1. a wrapping title in a text-screen fixture, with a wrap assertion, in RTL and in the LTR guard;
2. a paragraph with no strong character in the notice-text and curated-text fixtures.

Each new fixture is watched red with its mutation (M8, M11) and green on the code. Then the auditor reviews the fix (a small loop). QA's device re-check need not wait: no production code changes.

VERDICT: FAIL
