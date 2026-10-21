# android-game-dev report — 15-founder-feedback, slice S11 fix round 2 (fonts Q-013, D-03m, N-1, N-2)
Date: 2026-09-29
Branch: feature/phase-15-ux-refresh (472835f..a96ca18, plus this report's commit; worktree `/Users/ermanergoz/Desktop/PegSolitarie-p15`)
Skills loaded: ergoz-soft:standards (role android-game-dev; the plugin's 0.6.2 rule files were read where they differ from the product's 0.5.1 snapshot: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing, audio), compose-skill, compose-ui, android-testing, superpowers:test-driven-development (with its writing-good-tests reference), gitflow
Ran in parallel with: the ux-designer's §6.5 edit of `docs/ux-spec.md` in this worktree (committed as `f2fdffd` during this session). I did not edit or stage `docs/ux-spec.md`, and I was the only Gradle user here.
Ran on: claude-opus-5-5 (Opus 5.5) at effort max (agent file `model: opus`, `effort: max`)
Fan-out: none
Loops: 2
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **Fonts (Q-013; F-A, F-B, F-C):** the tech-lead's D1–D10 are done. Five unmodified static OFL files are bundled in `res/font`, and `AppFonts` (its own file) holds them as blocking `ResourceFont`s. `Type.kt` reads `AppFonts`, and the provider path is gone: `GoogleFont`, `font_certs.xml` and `ui-text-google-fonts`. T1–T3 were each red on the pre-fix code; M-a–M-d and M2 each turn a test red.
- **D-03m:** `home_hero_row_min_height` is retired. 15p is read between visible elements, and four new tests hold the row to its tallest child. All were red against the 72dp floor.
- **N-1 / N-2:** test-only. 18e now checks the horizontal half, red under T8. The one-node TalkBack test is red under T3, the RTL test under T5.
- `./gradlew check` is green. The build is installed on emulator-5560, and Home's title renders in Fredoka there.

## Outputs
- `app/src/main/res/font/{fredoka_medium,fredoka_semibold,nunito_regular,nunito_semibold,nunito_bold}.ttf` (472835f) — D1. The official Google Fonts static instances, renamed only. Every sha256 equals the ruling's D1 table (`8143bd54…`, `021e30b2…`, `b0885056…`, `c7fd1944…`, `ad443e47…`). Google has not revised the families, so the T2 oracle stands. I re-derived it with `hb-shape` on the committed files and got the same numbers.
- `app/src/main/assets/licenses/{Fredoka,Nunito}-OFL.txt` (472835f) — D2. Each family's `OFL.txt` from the official download manifest, byte for byte (CRLF, as served): 4,482 B, sha256 `782c3137…`, and 4,479 B, `66db16f4…`.
- `app/src/main/java/com/app/pegsolitaire/ui/theme/AppFonts.kt` (b5836ac, new) — D3. `internal object AppFonts` with `Fredoka` (Medium, SemiBold) and `Nunito` (Normal, SemiBold, Bold). Every font is `Font(resId, weight, loadingStrategy = FontLoadingStrategy.Blocking)`.
- `app/src/main/java/com/app/pegsolitaire/ui/theme/Type.kt` (b5836ac) — D4. The families, provider, constants and `googlefonts` imports are deleted. `heading()`/`body()` read `AppFonts`. The role → weight map is unchanged. The KDoc is rewritten, and `withTabularFigures()`'s KDoc notes that Fredoka has no `tnum`.
- `app/src/main/res/values/font_certs.xml` — deleted (D5; it carried F-B's corrupt certificate).
- `gradle/libs.versions.toml`, `app/build.gradle.kts` (b5836ac) — D6. The `ui-text-google-fonts` entry, its comment and its `implementation(...)` are gone. The release block's `font_certs.xml` paragraph is now one line: `res/font` is referenced through `R.font`, so the shrinker keeps it.
- `app/src/test/java/com/app/pegsolitaire/ui/theme/AppTypographyTest.kt` (b5836ac, new) — T1, JVM. Covers the 15 roles: each role's family is `AppFonts.Fredoka`/`Nunito` and never null, its weight is the mapped one, and the weight is present exactly in its family. Every font in both families is a `ResourceFont` with the D3 resource id, `FontStyle.Normal` and `Blocking`, checked against a table typed from the ruling, never read from `AppFonts`.
- `app/src/test/java/com/app/pegsolitaire/ui/theme/AppTypographyRendersBundledFacesTest.kt` (b5836ac, new) — T2. Robolectric NATIVE, SDK 34, mdpi. Every role inside `AppTheme` at 100sp and 0 tracking, three probes, each within ±1px of the HarfBuzz oracle, and at least one probe ≥ 8px from `FontFamily.Default` (a failing role is named; Truth `Expect` collects all 60 expectations).
- `app/src/test/java/com/app/pegsolitaire/ui/theme/TypeScaleFixture.kt` (b5836ac, new) — the role → face table and the oracle, typed once for T1 and T2.
- `app/src/test/java/com/app/pegsolitaire/ui/game/TimerDigitsTest.kt` (b99f5d3, a96ca18):
  - **T3:** `everyDigitSitsInACellAsWideAsFredokaMediumsWidestDigit` renders `TimerDigits` in the production `headlineSmall.withTabularFigures()` at 100sp on xxxhdpi, over 00:00, 08:08, 11:11, 59:59 and 22:22, and reads each digit's cell off the rendered cells. Precondition in the same test: the platform's widest digit is ≥ 3px from Fredoka's.
  - **T5:** the late-face test keeps its property; its late face is `R.font.fredoka_medium` (`ResourcesCompat.getFont`).
  - **N-2:** `theClockSpeaksAsOneNodeAndItsCellsAreHiddenFromAccessibilityServices` reads the view's `AccessibilityNodeProvider`: the clock's node is visible with its text and description, and each cell is not. `aClockReadsLeftToRightInARightToLeftLayout` checks that "05:12" reads "05:12" across the screen in RTL.
  - D8: the class KDoc drops "the face never resolves on the emulator".
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameStatusStripTest.kt` (b99f5d3, 87d137a):
  - **T4:** the 18d test uses `AppTheme`'s own type scale. The override and `fredokaMedium()` are deleted, and the "set widths spread by > 0.5dp" precondition is kept.
  - **N-1:** 18e adds `assertTimerInsideItsPillSideways`. Every cell's unclipped extent must lie inside the clock's node, and the node inside the pill.
- `app/src/main/java/com/app/pegsolitaire/ui/game/TimerDigits.kt` (b99f5d3) — D8, KDoc only: "bundled face", and the cells are measured while measuring for a face or font scale that changes after the first layout.
- `app/src/test/java/com/app/pegsolitaire/testing/ShippedFace.kt`, `app/src/test/resources/fixtures/fonts/` — deleted (T5). There is no second copy of the bytes under `src/test`.
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeHero.kt`, `app/src/main/res/values/dimens.xml` (5e0c202) — D-03m. The row's `heightIn(min = home_hero_row_min_height)` and the token are removed. The KDoc says the row is as tall as its tallest child.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeStageTest.kt` (5e0c202) — §13 retarget. `widestGap()` reads visible elements: the hero row is the span of its title and gear chip, and the ribbon the span of its pills. Nothing reads the row's box any more.
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeHeroRowTest.kt` (5e0c202) — four new tests: the row's height equals its tallest child's (±0.5dp) at font scale 1.0 and 2.0, on w412dp-h915dp and w360dp-h640dp.
- `app/src/test/java/com/app/pegsolitaire/ui/home/fixture/HomeTestFixture.kt` (5e0c202) — a `columnBoundsOf(SemanticsNode)` overload, so the gear chip (a parent node) is measured along the column.
- `.planning/qa/15-founder-feedback/loop3/dev-fredoka-check.png` (git-ignored, 204,646 B) — Home on emulator-5560 after the install. The title "Peg Solitaire" has Fredoka's rounded terminals, and the labels and supporting lines are in Nunito.
- `.planning/qa/15-founder-feedback/loop3/dev-before-roboto.png` (git-ignored, 198,014 B) — the same Home on the previous install (05:16), in Roboto, for comparison.

## Checks
- `./gradlew check` on a96ca18 — **pass**: `BUILD SUCCESSFUL in 11m 16s`, `98 actionable tasks: 29 executed, 1 from cache, 68 up-to-date`, exit 0.
  - `:app:testDebugUnitTest` executed: app 1,061 tests (1,049 + 12 new), 0 failures, 0 errors, 0 skipped.
  - Domain: 274 / 0 / 0 / 0, and `:domain:depthSweep` executed.
  - Lint: `0 errors, 57 warnings` (92 last round). None is in a file this round added; the three `dimens.xml` warnings are older unused tokens.
  - ktlint and detekt: up to date from `./gradlew :app:ktlintCheck :app:detekt --offline` on the final sources, which passed. `dpSpLiteralCheck`: executed, pass.
- **TDD red runs** (`./gradlew :app:testDebugUnitTest --offline --tests …`). Each test was watched fail before it was trusted:
  - T1 before `AppFonts` existed: compile error `Unresolved reference 'AppFonts'`.
  - T1 on the pre-fix `Type.kt`, 2/4 red. "displayLarge's font family / expected not to be: null" (F-A reproduced by a test), and "the weights in displayLarge's family / expected to contain: FontWeight(weight=600) / but was: []".
  - T1 on the pre-fix `Type.kt` with only the declaration order fixed: red, "expected specific instance: FontListFontFamily(fonts=[ResourceFont(…)]) / but was: FontListFontFamily(fonts=[Font(GoogleFont("Fredoka"…". That is the GoogleFont half of "red today".
  - T2 on the pre-fix code: 60/60 expectations red, e.g. "displayLarge: "1111" in px … expected 153.2 / but was 228.0", and every role's "widest difference from the platform face … at least 8 / but was 0".
  - T3 on the pre-fix code: "the cell of the digit at 0 in 00:00, in px / expected 235.0 / but was 228.0".
  - T4 on the pre-fix code: "how far apart the four readings are set in the face under test, in dp / expected to be greater than 0.5 / but was 0.0".
  - After the fix: T1–T4 green, and the whole app suite 1,055 / 0 / 0 / 0 at that point. Robolectric loads `res/font` in both LEGACY and NATIVE modes, so T5's fallback was not needed.
  - D-03m against the 72dp floor:
    - `HomeStageTest.onTheTallPhone…NoGapExceeds24dp`: "no chip: the widest gap / expected to be at most: 24.0.dp / but was: 28.0.dp".
    - The four row-height tests: "expected 48.0 / but was 72.0" at 1.0, and "expected 53.0 / but was 72.0" at 2.0, where the title is the tallest child.
- **Mutations.** Each one backed the file up, applied the change with an assertion that it took, ran the tests, then restored from the backup and checked byte-identity with `cmp`. Every run recompiled `:app:compileDebugKotlin`.

  | # | Mutation (production code) | Result |
  |---|---|---|
  | M-a | families declared after the type scale in `Type.kt` (F-A reintroduced) | red: T1 ×2, T2, T3, T4 |
  | M-b | `titleMedium` set to `FontFamily.Default` | red: T1 ×2, T2 |
  | M-c | `R.font.fredoka_medium` ↔ `fredoka_semibold` in `AppFonts` | red: T1 ("the fonts of Fredoka / missing (2) …"), T2 (27 expectations, e.g. "1111" 156.0 vs 153.2), T3 ("expected 235.0 / but was 231.0") |
  | M-d | `AppFonts.Nunito` rebuilt from `GoogleFont` (the dependency re-added for this run only, then restored) | red: T1 ("the fonts of Nunito / missing (3) …"), T2 (24 expectations, e.g. bodyLarge "1111" 225.0 vs 240.0) |
  | M2 | cells measured in `FontFamily.Default` (`TimerDigits.kt:88`) | red: T3 ("expected 235.0 / but was 232.0"). The strip's 18d test stays green under M2, the F-C gap T3 closes |
  | T8 | the auditor's pill fixed at 80dp (`.width(game_timer_pill_height * 2)`) | red: 18e, "cell 3 of the timer's clock, against the clock, in px / expected to be at most: 332.0 / but was: 347.0" |
  | T3 | `hideFromAccessibility()` removed from the cells | red: "whether cell 0 is visible to accessibility services / expected to be false" |
  | T5 | cells placed with `placeRelative` | red: "expected: 05:12 / but was: 21:50" |

- `git grep -n -E "googlefonts|GoogleFont|font_certs|ui-text-google-fonts|fixtures/fonts|shippedFaceFile" -- app gradle` (AC-F4) — **1 hit**: `app/src/main/assets/licenses/Nunito-OFL.txt:1:Copyright 2014 The Nunito Project Authors (https://github.com/googlefonts/nunito)`. This is the upstream copyright line, which D2 requires verbatim, not a provider reference. There are 0 hits in code, resources or the build.
- `git grep -n "home_hero_row_min_height" -- app` — 0 hits.
- AC-F5 on the installed debug APK:
  - `unzip -lv app/build/outputs/apk/debug/app-debug.apk | grep -E "res/font/|assets/licenses/"` lists the 5 fonts and 2 licences.
  - `unzip -p <apk> res/font/<name>.ttf | shasum -a 256` equals D1 for all five, and both licences equal their sources.
  - The APK has 0 `google-fonts` entries; the baseline had `META-INF/androidx.compose.ui_ui-text-google-fonts.version`.
- **APK size**, debug, baseline 51e7158 against a96ca18:
  - Raw file sizes are 21,412,824 → 20,977,221 B (−435,603). That number is a packaging artifact: the baseline APK carried 659,832 B of slack from AGP's in-place incremental packaging, against 4,096 B in the new one. I measured this by summing local headers, entry bytes, the central directory and the end record.
  - Normalized for that slack, the APK grows by **+220,133 B (≈215 KiB)**.
  - The new entries add +238,488 B compressed (fonts 234,318, licences 4,170). Dex shrinks by 10,477 B and `resources.arsc` by 2,992 B, with the Google Fonts library and `font_certs.xml` gone.
  - The release APK/AAB could not be measured here: `:app:assembleRelease` needs the four `PLAY_UPLOAD_*` secrets, which this session does not have.
- `hb-shape --font-size=1000 --output-format=json` (HarfBuzz 14.4.0) on the committed fonts. The oracle matches the ruling exactly (1552/5349/5686, 1532/5286/5715, 2400/6000/5682, 2400/6000/5797, 2400/6000/5937), and Fredoka Medium's "2" is 587, the widest digit.
- `git diff f2fdffd..HEAD -- . ':!.planning' ':!*.ttf' ':!*OFL.txt' | grep '^+' | grep -E '@Suppress|@Ignore|@Disabled|!!|baseline|SuppressLint|ktlint-disable|detekt:disable|OptIn'` — 0 hits. Removed `@Test` lines: 0. Added: 12.
- `ANDROID_SERIAL=emulator-5560 ./gradlew :app:installDebug --offline` — **pass**: "Installing APK 'app-debug.apk' on 'Pixel_9(AVD) - 16'", "Installed on 1 device", `lastUpdateTime=2026-09-29 15:17:57`. The emulator was not restarted, and app data was kept (`install -r`).
- `adb -s emulator-5560 shell am start -W -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity` — `Status: ok`, `TotalTime: 939`. This was one cold launch on a shared host, not a 15e reading.
- `adb -s emulator-5560 shell uiautomator dump` — Home is on screen: "Peg Solitaire" [42,189][912,305], Settings, Continue Classic, Classic, Challenge.
- `adb -s emulator-5560 exec-out screencap -p > .planning/qa/15-founder-feedback/loop3/dev-fredoka-check.png` — **Fredoka renders**. By eye, cropped next to the before shot: the title's "t", "l" and "i" end in round caps and the "g" is single-storey and round, where the Roboto build shows flat, angled cuts. The hero row sits about 24dp higher than in the before shot (D-03m). The app was left on Home.
- `adb -s emulator-5560 logcat -b crash -d -t '09-29 15:17:00.000'` — 0 lines. The only app-tagged warning is system_server's `WindowManager` at 15:17:58, as the install replaced the running process.

## Open issues / risks
- **T3's tolerance, for the auditor.** A digit's cell can only be read to within one pixel: a digit narrower than its cell is centred on whole pixels. T3 therefore holds each reading to ±1px of the whole-pixel cell a text layout gives Fredoka's "2", ⌈234.8⌉ = 235px (the ruling says ±1px of ≈234.8).
  - A correct layout reads 234 or 236 at some digits. That is exact integer arithmetic, so the result is deterministic, but it sits at the inclusive edge.
  - M2 reads 228–232, and the pre-fix scale 228.
- **T2 uses linear metrics (`TextMotion.Animated`).** With the default `TextMotion.Static`, each glyph's advance is hinted to a whole pixel, and 8 of the 60 expectations missed the oracle by 1.2–1.8px: Fredoka SemiBold "1111" 152 vs 153.2, and Nunito Regular "Peg Solitaire" 570 vs 568.2. The face is the same either way, and the KDoc says so.
- **AC-F4's grep has one hit**, the verbatim Nunito copyright line (above).
- **The OFL texts are CRLF, as served.** `.editorconfig` says LF, but no check reads `.txt` files, and D2 asks for the files verbatim.
- **The whole JVM suite now renders in Fredoka and Nunito**, since the faces load blocking and Robolectric loads `res/font`. The 15k, 15o, 18a, 18e and tutorial layout tests all pass in the real faces. The device re-walk still governs.
- **Release size and the AAB check** (AC-F5, second half) remain the release-manager's; only the debug APK is measured here.
- **The install is on the founder's Pixel_9 AVD** (emulator-5560), as dispatched. 15e must still run on the Pixel_9a per Ruling 2.
- **Optional, not done:** 18a (default scale) could carry the same sideways check as 18e. N-1 asked for 18e, and 18e already turns red under T8.
- **Lesson candidates for the retro:**
  - (a) Blocking `ResourceFont`s load under Robolectric in both graphics modes, so JVM tests can render the production type scale.
  - (b) A width oracle from HarfBuzz needs linear metrics (`TextMotion.Animated`); `TextMotion.Static` hints each advance to a whole pixel.
  - (c) Compare debug APK sizes by entry content, not file size: AGP's incremental packager leaves slack in the file (here 660 KB).

### THIRD_PARTY.md — row text for the release-manager (the file is on `feature/phase-14-release-docs`; this is the ruling's text, unchanged)
§2 "Assets shipped in the artifact". The opening sentence becomes: "The only binary assets in the app are twelve sound files **and five font files** (`app/src/main/res/font/`)." Replace the Fredoka/Nunito row with these two:

| Item | Source | Licence | How verified | Credit |
|---|---|---|---|---|
| Fredoka Medium, Fredoka SemiBold — `app/src/main/res/font/fredoka_medium.ttf`, `fredoka_semibold.ttf`, **bundled in the artifact** | Google Fonts, Fredoka **v17** static instances, unmodified (renamed only): `https://fonts.google.com/download/list?family=Fredoka`, entries `static/Fredoka-Medium.ttf` (sha256 `8143bd54…2304ae6fa`) and `static/Fredoka-SemiBold.ttf` (sha256 `021e30b2…d7688e43a8`, full hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/hafontia/Fredoka-One. © 2016 The Fredoka Project Authors | **OFL-1.1**, no Reserved Font Name | The `OFL.txt` shipped in the same family download (copyright line + full OFL 1.1) | Notice and licence ship in the APK as `assets/licenses/Fredoka-OFL.txt` (OFL §2, stand-alone text file); listed here; no in-app licences screen — see §7 |
| Nunito Regular, SemiBold, Bold — `app/src/main/res/font/nunito_regular.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf`, **bundled in the artifact** | Google Fonts, Nunito **v32** static instances, unmodified (renamed only): `https://fonts.google.com/download/list?family=Nunito`, entries `static/Nunito-Regular.ttf`, `static/Nunito-SemiBold.ttf`, `static/Nunito-Bold.ttf` (hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/googlefonts/nunito. © 2014 The Nunito Project Authors | **OFL-1.1**, no Reserved Font Name | The `OFL.txt` shipped in the same family download | Notice and licence ship in the APK as `assets/licenses/Nunito-OFL.txt`; listed here; see §7 |

The rest of the file:
- §3: delete the `font_certs.xml` row (the file is deleted).
- §4: delete the `androidx.compose.ui:ui-text-google-fonts` row (the dependency is removed).
- §7: in the "No in-app licences screen" bullet, replace "so **nothing shipped carries an attribution obligation**" with: "The two bundled font families carry OFL-1.1's notice condition. It is met by the stand-alone licence files in the APK (§2), so nothing shipped carries an **unmet** attribution obligation."
- §8: nothing to add, since the test-only fixture is deleted.

The files as committed match D1/D2 byte for byte (hashes above).

## Handoff
Next: code-auditor —
- Re-audit 472835f..a96ca18.
- Mutations to re-run: M-a to M-d and M2 (fonts); T8 (N-1); T3 and T5 (N-2); the 72dp floor restored (D-03m).
- To judge: T3's whole-pixel tolerance, T2's linear metrics, and AC-F4's single licence-text hit.

Next: qa-engineer —
- The build is on emulator-5560.
- First the ruling's two discriminators ("Level 111" ≈82.1dp; "Continue — Level 111" ≈142.6dp) and the airplane-mode cold start.
- Then the re-walk list: 18d at the five readings, 18e, 18a, 15o, 15k, 15m, 15p/15g in the visible-element reading, 15i/14, 15f, the tutorial captions, and the whole app.
- The founder's Q-013 screenshots.
- 15e per Ruling 2 on the Pixel_9a.

Next: producer — route the THIRD_PARTY.md rows above to the release-manager on `feature/phase-14-release-docs`, together with AC-F5's AAB check at the release branch.
