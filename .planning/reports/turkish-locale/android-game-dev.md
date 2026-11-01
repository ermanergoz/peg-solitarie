# android-game-dev report — turkish-locale
Date: 2026-11-01
Branch: feature/turkish-locale (a1fb23d..75cb0c7; this role's commits are a1fb23d..f289546, loop 1's 9ed227b and 2fd358d, and loop 2's 8e010ab..75cb0c7)
Skills loaded: ergoz-soft:standards (role: android-game-dev; rules read in full from the plugin, since the snapshot is 0.6.2 against plugin 1.0.0: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md, ip-policy.md; the product's .claude/rules/lessons.md), superpowers:test-driven-development (with writing-good-tests.md), gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran in parallel with: feature/home-continue-live-boards, feature/challenge-star-bands (not read; their string deltas come through the producer, spec §9.3)
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 2 (loop 1: the code-auditor's FAIL at `3d81752`; loop 2: the Q-039 font contract, `2eb1e95`)
Lessons applied: none (the snapshot has no active lessons)

## Summary
- Turkish is in, from spec §5 (the sen re-voice) verbatim: `values-tr` holds 179 strings and 7 plurals, and the nine proper-name keys are `translatable="false"`. Android 13+ lists English and Türkçe under App languages via `localeConfig`, and `localeFilters` (en, tr) keeps third languages English. There is no in-app picker.
- Every part is guarded by a test: the parity test, the app-languages test, Nunito glyph coverage, the protected-names scan over `values-tr`, and Home, Results and Settings run on a Turkish phone (`tr-rTR`). The first two were run red before their code. The other three guard state that already existed, and each was shown to fail by a mutation. Lint already fails the Check on MissingTranslation, ExtraTranslation and MissingQuantity (proven by injection), so no lint setting was needed.
- **Escalation: no Fredoka release has Ğ ğ İ Ş ş**, so there was nothing to swap in. Five Turkish strings drawn in Fredoka show the platform font for those letters, and C-2 will fail until the tech-lead and the ux-designer decide. *(Closed in loop 2, Q-039: both Fredoka files now carry the five letters, built from a committed recipe, `fonts/fredoka-tr/`; see Loop 2.)*
- Not built, escalated: the Roborazzi goldens of §7.6, which tech-design §8.5 rules out. One new lint warning (`UnusedAttribute` on `localeConfig`) is left for the code-auditor rather than suppressed.
- The Check is green (see Checks). The debug APK is 658,976 B smaller (−2.96 %). The Turkish strings add very little; `localeFilters` drops 84 library locale configurations from `resources.arsc`, which goes from 751,580 B to 92,248 B. The fonts are unchanged.

## Outputs
Commits on `feature/turkish-locale`, one per behaviour. The two `feat` commits were test-first: the red runs are in Checks. The three `test` commits guard what already held, and each one's mutation run is in Checks.
- `a1fb23d` feat(l10n): add the Turkish strings with a parity test
- `fad9f8b` feat(l10n): offer English and Turkish as per-app languages
- `88a4dee` test(l10n): prove the Nunito faces draw every Turkish letter
- `748e02b` test(l10n): hold the Turkish copy to the protected-names scan
- `f289546` test(l10n): run Home, Results and Settings on a Turkish phone

Files:
- `app/src/main/res/values-tr/strings.xml` (new). The Türkçe column of spec §5 on `1b8c89b` (the sen re-voice, Q-031), verbatim: 179 strings and 7 plurals, which matches §5.6's count. Each `'` is escaped `\'`. It keeps the base file's key order and section comments. There is no `translatable` attribute, and the file has its own header comment. I generated it with a scratch script (not committed) that walks the base file and copies each Turkish cell. A second, independent script re-parsed the XML, unescaped every value and compared all 186 entries with the spec cells: 0 mismatches. The 22 re-voiced keys the producer listed are among them.
- `app/src/main/res/values/strings.xml`. The nine keys of spec §4 are now `translatable="false"`: `app_name`, the five `board_*`, `settings_section_play_games`, `stat_value_unknown`, `home_stat_boards_value`. One stale comment is corrected: the Undecided notice's comment said "the app has no Turkish locale".
- `app/src/main/res/xml/locales_config.xml` (new). Lists `en` and `tr`. `app/src/main/AndroidManifest.xml` gains `android:localeConfig="@xml/locales_config"` on `<application>`, with a comment.
- `app/build.gradle.kts`. `android { androidResources { localeFilters += listOf("en", "tr") } }`, with a comment (spec §7.4, C-6).
- Tests, `app/src/test/java/com/app/pegsolitaire/`:
  - `locale/StringResourceFile.kt`. Test utility: reads a `strings.xml`; `formatArguments()` gives the multiset of `%n$x` specifiers.
  - `locale/TurkishStringsParityTest.kt`, 8 tests. Covers spec §7.6's parity test: equal key sets for strings and plurals, equal format arguments per string and per plural item, exactly `one` and `other` per plural, no English leftover except the five format joins of §5.6, and no `translatable` attribute in the Turkish file. It also checks the specifier reader itself.
  - `locale/AppLanguagesTest.kt`, 4 tests, Robolectric SDK 34. The merged manifest Robolectric runs points `android:localeConfig` at the list. The list names the base language plus every `values-<lang>` folder that has a `strings.xml`, derived from the folders. Every offered language resolves its own app strings and Compose's own strings. A language the app doesn't offer (German) gets Compose's English, never its German.
  - `locale/BundledFontGlyphCoverageTest.kt`, 2 tests. Each Nunito TTF's cmap, read through `java.awt.Font.canDisplay`, covers spec §7.3's 13 code points and every character of the Turkish copy.
  - `UserFacingTextHasNoProtectedNamesTest.kt` and `ui/UserFacingTextHasNoProtectedNamesTest.kt`. Both now assert that `values-tr/strings.xml` is in their scan (they already globbed `values*`). The `ui` copy adds a fixture showing that a word in Turkish casing ("KİNG", "tıffı") is still found.
  - `testing/DeviceLanguage.kt`. `TURKISH_DEVICE = "tr-rTR"`, plus `englishString()`, which resolves a string as an en-US device would.
  - `ui/home/HomeScreenTurkishTest.kt` (4 tests), `ui/results/ResultsScreenTurkishTest.kt` (3), `ui/settings/SettingsScreenTurkishTest.kt` (4). Each screen's main flow runs under `@Config(qualifiers = "tr-rTR")`. Nodes are found by resource id. Every test also asserts its strings differ from en-US, so a build without Turkish fails all 11 instead of passing in English.
- `.planning/reports/turkish-locale/android-game-dev.md`. This report.

No dependency was added, no font file changed, and THIRD_PARTY.md is untouched (nothing new ships). `docs/store/` is untouched (the release-manager's, spec §10).

## Checks
Run in `/Users/ermanergoz/Desktop/PegSolitarie-tr`. `SCRATCH` is this session's scratchpad. The scratch tools there are not committed.

The Check and the build:
- `./gradlew check` — pass: `BUILD SUCCESSFUL in 11m 11s`, 107 actionable tasks (23 executed, 15 from cache, 69 up-to-date), exit 0. JUnit XML totals:
  - `:app:testDebugUnitTest`: 200 suites, 1,667 tests.
  - `:domain:test`: 33 suites, 294 tests (from cache: `domain` is unchanged).
  - `:domain:depthSweep`: ran (never up-to-date).
  - `:domain:fuzz`: from cache, at the local `FUZZ_SEEDS` default.
  - `build-logic` `test`: 100 tests.
  - All with 0 failures, 0 errors, 0 skipped.
  - The log has 0 `w:` lines, because every compilation was already up to date. The `--rerun` compile under Style shows the branch's three test warnings.
  - Lint ran inside it (`:app:lintDebug`).
- `./gradlew :app:assembleDebug` — pass (`BUILD SUCCESSFUL`).
- APK size, measured with `python3 -I $SCRATCH/tools/apk_gaps.py <apk>` and `aapt2 dump configurations` (build-tools 36.0.0). Debug APKs:
  - Before: `1b8c89b`, built before any production change. 22,240,704 B, `resources.arsc` 751,580 B, 85 language-qualified resource configurations, all from libraries.
  - After: `f289546`, packaged from scratch. 21,581,728 B, `resources.arsc` 92,248 B, one language configuration (`tr`). The delta is **−658,976 B (−2.96 %)**.
  - Both have the same 4,096 B of zip gaps. The first branch build was packaged incrementally: it left 663,038 B of free space where the shrunken table had been, and showed +72 B. That is why the APK was repackaged before measuring.
  - `res/xml/locales_config.xml` adds 206 B compressed. The fonts are unchanged (0 B).
  - A signed release can't be built here without the `PLAY_UPLOAD_*` and ID variables. The table shrinks there too, since minification doesn't touch resource locales.

Test-first runs:
- `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.locale.TurkishStringsParityTest"` before `values-tr` existed — fail, as intended (8 of 8, `FileNotFoundException`: no Turkish file). After `a1fb23d`'s change — pass, 8 of 8.
- `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.locale.AppLanguagesTest"` before `localeConfig` — fail, as intended (status 1, "not specified", and no list). After the list and the manifest attribute, 3 of 4 pass; the fourth fails as intended ("expected: Navigation menu, but was: Navigationsmenü"). After `localeFilters` — pass, 4 of 4.

Mutation runs, each restored afterwards:
- Turkish file: `catalog_stars_earned` changed to `%2$s`, `onboarding_skip` left in English — fail, as intended (format-argument and leftover tests, 2 of 8).
- Manifest attribute and the `tr` list entry removed — fail, as intended ("but was an empty string"; "missing (1): tr").
- `localeFilters` cut to `en` — fail, as intended. AppLanguagesTest: "tr, the app's strings expected not to be: Settings". All 11 Turkish screen tests fail ("expected not to be: Solved!", "… General", …).
- `BundledFontGlyphCoverageTest` pointed at `fredoka_*` — fail, as intended ("[U+011E Ğ, U+011F ğ, U+0130 İ, U+015E Ş, U+015F ş]").

Lint gate, injection run:
- `./gradlew :app:lintDebug` with `coming_soon` removed from `values-tr`, an extra `only_in_turkish` key added and `holes_count`'s `one` item removed — fail, as intended: "Lint found 3 errors … aborting build" ([MissingTranslation], [ExtraTranslation], [MissingQuantity]). Restored.
- `./gradlew check --dry-run | grep ^:app:lint` — `:app:lint` → `:app:lintDebug` is in the Check.
- `./gradlew :app:lintDebug` on the branch — pass: 0 errors. New in touched files: only `AndroidManifest.xml:34` [UnusedAttribute] (open issue 3). `values/strings.xml` keeps its 10 UnusedResources and 5 PluralsCandidate, all pre-existing.

Verbatim and fonts:
- `python3 -I $SCRATCH/tools/verify_verbatim.py docs/ux/localisation-tr.md app/src/main/res/values-tr/strings.xml app/src/main/res/values/strings.xml` — pass ("checked 186 entries, mismatches: 0").
- `$SCRATCH/venv/bin/python -I $SCRATCH/tools/cmap_check.py app/src/main/res/font/*.ttf` (fontTools 4.68.0) — Nunito ×3: all 13 present. Fredoka ×2: Ğ ğ İ Ş ş missing.
- The same tool on the downloads in `$SCRATCH/dl-gf-fredoka/`, `$SCRATCH/dl-ghfonts-fredoka/` and `$SCRATCH/dl-upstream-fredoka/` — the same five missing in all five files (hashes in open issue 1). Sources: `curl` of `fonts.google.com/download/list?family=Fredoka` and the gstatic URLs it lists; raw.githubusercontent.com `google/fonts/main/ofl/fredoka/`; `hafontia-zz/Fredoka-One` at `35c584f`.

Style:
- `./gradlew :app:ktlintCheck :app:detekt` — pass.
- `./gradlew :app:compileDebugUnitTestKotlin --rerun` — 3 new `w:` lines, all the v1 `createComposeRule` deprecation in the three screen suites (open issue 5). None from the other new files.

## Open issues / risks

**1. ESCALATION: Fredoka cannot draw Ğ ğ İ Ş ş, and no official file to swap in exists (spec D-13, §7.3; criterion C-2 will fail as built).**

Evidence. I read the cmap of each file with fontTools 4.68.0 in a scratch venv. `BundledFontGlyphCoverageTest`'s `java.awt` reader, pointed at Fredoka as a mutation, gives the same answer.

| File | Version | Glyphs | 13 code points of §7.3 |
|---|---|---|---|
| `nunito_regular.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf` (shipped) | 3.602 | 1,098 | all 13 present; Latin Extended-A 126/128 |
| `fredoka_medium.ttf`, `fredoka_semibold.ttf` (shipped; sha256 `8143bd54…`, `021e30b2…`) | 2.001 | 327 | Ç ç ı Ö ö Ü ü â present; **Ğ ğ İ Ş ş missing** |

The shipped Fredoka files aren't subsets. They are byte-identical to what Google Fonts serves today, so the spec's assumption that the full file covers Latin Extended-A doesn't hold for Fredoka. All of these are v2.001, and none maps Ğ ğ İ Ş ş:
- **Google Fonts v17 static instances.** Fetched again from the `download/list?family=Fredoka` manifest: same sha256 as shipped.
- **Google Fonts v17 variable file.** `fonts.gstatic.com/s/fredoka/v17/X7nl4b87HvSqjb_WOCaQ4MTgAgk.ttf`, 157,744 B, sha256 `638482f8…`.
- **The google/fonts repository.** `ofl/fredoka/Fredoka[wdth,wght].ttf`, 159,184 B, sha256 `2ba02e68…`. Its METADATA.pb declares `latin-ext`.
- **The designer's upstream.** `github.com/hafontia-zz/Fredoka-One`, the only commit `35c584f` (2022-03-03): `fonts/ttf/Fredoka-Medium.ttf` (45,364 B, `024bec99…`) and `Fredoka-SemiBold.ttf` (47,108 B, `95839d50…`).

Fredoka's whole Latin Extended-A is ı Ł ł Œ œ Š š Ÿ Ž ž. The tech-lead font ruling's line "both faces carry … Turkish" (`.planning/reports/15-founder-feedback/tech-lead-fonts.md`, glyph coverage) is wrong for Fredoka.

What it costs on screen. I traced the Fredoka roles (display, headline and title styles, `ui/theme/Type.kt`) to the strings they draw: 42 Turkish keys. Five contain a missing letter. That letter draws in the platform font inside a Fredoka word:
- `restart_confirm_title` "Bu tahta yeniden başlatılsın mı?" (restart dialog title, headlineSmall)
- `settings_haptics_label` "Titreşim" (Settings row label, titleMedium)
- `settings_colorblind_label` "Renk körlüğü paleti" (same)
- `settings_play_games_achievements_label` "Başarımlar" (same; C-2 names this word)
- `licenses_annotations_title` "Ek açıklama ve bağımlılık ekleme API'leri" (licence entry title and its text screen heading)

`settings_play_games_signed_in_as` also draws the player's own display name in Fredoka, so a name like "Ayşe" hits the same gap.

C-2's own eight words fall like this:
- "Başarımlar" fails (Fredoka).
- "Mükemmel!" and "Çözüldü!" pass: their letters are in Fredoka.
- "İpucu" passes: it is drawn only in the Undecided notice, in Nunito.
- "Yazı tipleri", "Gizlilik" and "KALAN PİYON" pass: section headers and stat labels are labelLarge, Nunito.
- "Geçen süre" is spoken only.

So QA should also look at the other four strings above. Everything in Nunito is covered: body text, labels, buttons, the Results stat labels including "KALAN PİYON", and the section headers.

I didn't modify a font or change the type scale. Both are design and font-ruling decisions, and the dispatch's remedy (swap in the full upstream file) has no file to use. Options for the tech-lead (font ruling owner) with the ux-designer:
- (a) Ship 3.0.0 with the per-glyph fallback in those five strings, and have QA record C-2 as a known deviation.
- (b) The ux-designer rewords the five so they avoid ğ ş İ. This constrains the copy by a font.
- (c) Set the Fredoka roles in Nunito on a Turkish device. This needs locale-aware typography in `ui/theme`.
- (d) A Modified Version of Fredoka under OFL-1.1 (no Reserved Font Name), with composite Ğ ğ İ Ş ş built from Fredoka's own G g I S s, cedilla and dot, plus a breve, which Fredoka lacks and would have to be drawn. This is font design work and needs a licence-row update and visual QA.
- (e) A per-glyph fallback chain Fredoka → Nunito (`Typeface.CustomFallbackBuilder`, API 29+ only).

My recommendation as the developer: (c) or (b) for 3.0.0, because they are the only options that put no fallback glyph on screen without font engineering in a release week. The choice belongs to the tech-lead and the ux-designer. `BundledFontGlyphCoverageTest` covers the Nunito faces only. Its KDoc says why, and the Fredoka files join it once the decision lands.

**2. ESCALATION: spec §7.6's Roborazzi goldens conflict with the tech design.** `docs/tech-design.md` §8.5 (tech-lead ruling 2026-09-21, pointer 2026-10-17) keeps stored goldens out until a golden-image suite owned by QA, after v1.1 and R-2. Roborazzi isn't a dependency of the project. I built no goldens and added no dependency. The ruling allows in-run pixel comparisons, but those can't stand in for goldens that QA reviews against a device. For the tech-lead: confirm the goldens are dropped for 3.0.0 (the §6 slot decisions then rest on QA's device screenshots, C-7), or rule otherwise.

**3. One new lint warning, left unsuppressed for the code-auditor.** `AndroidManifest.xml:34: Warning: Attribute localeConfig is only used in API level 33 and higher (current min is 24) [UnusedAttribute]`. That behaviour is intended (D-9: Android 12 and below follow the system language). Tech-design §9.8 makes a new lint warning in a touched file a review finding. I added neither `tools:ignore` nor `tools:targetApi`, because both silence it. Options:
- (a) Accept it with an "approved by code-auditor" line.
- (b) Add `tools:targetApi="33"` on `<application>`. That is a declaration, but it would also hide future API-33+ attributes there.
- (c) Switch to AGP's `generateLocaleConfig = true` with `res/resources.properties` (`unqualifiedResLocale=en-US`). AGP then injects the attribute at merge and generates the list from the resource folders, which also removes the hand-kept list. This is untested here; it is the tech-lead's §7.4 choice. `AppLanguagesTest` would need its resource name read from the manifest.

Otherwise lint is unchanged in my files: 0 errors, and no warning of any kind in `values-tr/strings.xml`, so there is no typo flag to report (spec §7.4).

**4. For the tech-lead's sign-off (spec §7.4, §9.2):**
- The hand-written `locales_config.xml`. The dispatch named it; the spec allows either.
- `localeFilters` (en, tr). On a third-language device the libraries' strings now read English. `AppLanguagesTest` probes that with Compose UI's own `androidx.compose.ui.R.string.navigation_menu` ("Navigationsmenü" before the filter). If a Compose upgrade renames that key, the test fails to compile.
- A side effect to rule on: the filter `en` also drops the libraries' regional English (`en-rGB`, `en-rAU`, `en-rCA`, `en-rIN`, and the `en-rXC` pseudo-locale; `aapt2 dump configurations` before and after). An en-GB device then reads the libraries' default English, which differs only in the spelling of a few Compose and Material strings. The app has only default English of its own. Adding `en-rGB` etc. to the filter would keep them.

**4b. For QA, on C-6.** `localeFilters` controls only the resources packaged in this APK: the app's own strings and those of the bundled libraries, such as Compose's TalkBack words and play-services-base's dialogs. The UMP consent form is a page Google serves in the device's language, and the Play Games sign-in, leaderboard and achievement sheets are the Play Games app's own UI. So on a German device those stay German whatever this build does; the spec's §7.5 already says they are Google's and not ours to check. Read C-6's "including the Play Games and consent dialogs" as the app's own dialogs that lead to them.

**5. Unit-test compilation warnings.** The three new screen suites use the house v1 `createComposeRule`, which adds three deprecation `w:` lines. Tech-design §9.8's recorded exception (tech-lead, 2026-10-24) covers new suites until the §14.2 migration. The other new test files compile with no warning.

**6. Not this role's, through the producer:**
- `docs/store/tr-TR/` and `docs/store/README.md` lines 25–26 belong to the release-manager (spec §10, §9.2).
- The string deltas of `feature/home-continue-live-boards` and `feature/challenge-star-bands` are still to come (spec §9.3). Lint's MissingTranslation, ExtraTranslation and `TurkishStringsParityTest` will name every key that needs a Turkish row.

**7. Commit trailers.** The commits carry the two lines the dispatch asked for (`Co-Authored-By: Claude Opus 5.5`, `Claude-Session:`). The plugin's rules/gitflow.md 1.0.0 names a single different trailer, but the product's 0.6.2 snapshot has no trailer rule and the repo has no commit hook. Noted for the producer.

**Deviations from the spec, in one place:**
- §7.3, D-13: no font replaced, because none exists (issue 1).
- §7.6: no Roborazzi goldens (issue 2).
- §7.6: the Turkish screen tests are three new classes (`*ScreenTurkishTest`), not the existing suites re-run under `tr`. The existing Home suites pin English copy as literals in their shared fixture ("Continue — Level 7", "Continue Classic"); re-running them in Turkish would have meant weakening those English oracles.
- §7.6: they run under `tr-rTR`, a real device's locale, rather than a bare `tr`. A bare language with no region was what made a bare `en` resolve the libraries' `en-XC` pseudo-locale.
- §7.4: the hand-written list, not `generateLocaleConfig` (issue 4).
- §7.4 lint: no setting added, because the three issues already fail the Check (Checks).
- No §6 alternative was used (D-12: only on a device finding).

**8. Pre-existing, unchanged, as spec §9.4 notes.** `GameTimerFormat`'s `"%02d:%02d".format(…)` uses the default locale; the digits are identical for en and tr. The Results score is ungrouped in both languages.

Casing audit (D-3, §7.1). `grep` of `app/src/main` and `domain/src/main` for `uppercase(`, `lowercase(`, `capitalize(`, `decapitalize(`, `replaceFirstChar(`, `titlecase(`, `toUpperCase(`, `toLowerCase(`, `smcp` and `c2sc` finds nothing. The only `String.format` is the clock above, and no displayed list is sorted by text. The new test code formats with `Locale.ROOT`.

Language switch (C-4). Nothing resolves a string where it could outlive the switch:
- `BoardSpeechFormat` is remembered on `LocalResources.current`.
- `ClassicLevelSource` resolves only the board names, which are now `translatable="false"` and identical in both languages.
- `CloudSaveDescriptionFormatter` resolves at commit time, by design (the string's own comment).
- `MainActivity` declares no `configChanges`, so a language change recreates it.

## Loop 1 — the code-auditor's findings 1 and 2
Dispatched by the producer after the code-auditor's FAIL (`.planning/reports/turkish-locale/code-auditor.md`, `3d81752`). The two findings are fixed in `9ed227b` (finding 1) and `2fd358d` (finding 2). Everything above stands as the first round's record.

- **Finding 1, fixed.** `ui/UserFacingTextHasNoProtectedNamesTest.kt:36` asserted on `it.parentFile.name`. The toolchain types `File.parentFile` as `File?`, so the line added the warning "Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'File?'". The assertion now reads `it.relativeTo(appMain).invariantSeparatorsPath` and expects `res/values-tr/strings.xml`, the form the root-package copy already uses. There is no `!!` and no safe call. Correction to the first round: "None from the other new files" (Checks, Style) was wrong. My warning grep matched the new files' names and missed this touched file. This round matches the `w:` lines against the branch's full list of touched test files.
- **Finding 2, fixed.** `AppLanguagesTest` took every `values-<x>` folder holding a `strings.xml` as language `<x>`, so a later `values-night/strings.xml` would have demanded a "night" locale. It now reads each folder through `languageOfValuesFolder()`:
  - It skips the mobile-network qualifiers.
  - It accepts a two- or three-letter language with an optional `rXX` region (`tr`, `tr-TR`), or a `b+` BCP-47 tag (`sr-Latn`).
  - It returns null for anything else: `values`, `values-night`, `values-v35`, `values-night-v35`, `values-car` (the UI mode), `values-sw600dp`, and a folder that isn't a `values` folder.
  - Test-first: the self-test `theFolderReaderTakesALanguageOnlyFromALanguageQualifier` ran red against the old rule, extracted unchanged ("expected: tr-TR, but was: tr-rTR"), then green.
- **Not done, on the producer's instruction.** The tech-lead's M-1 to M-5 (Nunito headings on a Turkish device, `docs/tech-design.md` §11.1): the founder rejected that in Q-038 ("change the font for all languages"), and a new contract will follow. The `UnusedAttribute` lint warning stands, approved by the code-auditor (finding 3). Nothing else on the branch changed.

Checks, loop 1:
- `./gradlew :app:compileDebugUnitTestKotlin --rerun` — before: 74 `w:` lines, 4 of them in touched test files. After: 73, of which 3 are in touched files, all the v1 `createComposeRule` deprecation of the three screen suites (tech-design §9.8's recorded exception). Matched with `grep -F -f` against `git diff --name-only 37d3052 -- app/src/test`.
- `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.locale.AppLanguagesTest"` — fail, as intended (1 of 5: the self-test against the old rule), then pass, 5 of 5.
- `./gradlew :app:testDebugUnitTest --tests "*UserFacingTextHasNoProtectedNamesTest"` — pass (2 + 4).
- Mutation, the auditor's own scenario: a temporary `app/src/main/res/values-night/strings.xml` with one string — AppLanguagesTest passes 5 of 5 (the old rule would have demanded `night` in `locales_config.xml`). The file was removed.
- `./gradlew :app:ktlintCheck :app:detekt` — pass. Two lines were wrapped, and the reader was folded into one `when` (detekt's ReturnCount).
- `./gradlew check` — pass: `BUILD SUCCESSFUL in 11m 13s`, 107 actionable tasks (19 executed, 88 up-to-date), exit 0. `:app:testDebugUnitTest` 200 suites, 1,668 tests (one more than round 1, the new self-test), `:domain:test` 294, `:domain:depthSweep` 1 (ran), `:domain:fuzz` 1, `build-logic` 100: 0 failures, 0 errors, 0 skipped. `:app:lintDebug` ran: 0 errors.

## Loop 2 — the Q-039 font contract: Fredoka with the Turkish letters
Dispatched by the producer with the contract `docs/tech-design.md` §11.1, "Q-039" (`2eb1e95`), the tech-lead's addendum (`0d353fc`), the research-analyst's prototype report and the ux-designer's copy (`a50aa9c`, spec D-17, §5.4). M-1 to M-5 stay void. This section supersedes open issue 1 above: the Fredoka gap is closed in the font itself.

Commits: `8e010ab` (the shared folder reader), `959bb70` (the recipe, the two fonts, the prose date, the two font tests), `b5bb20f` (the licences line), `75cb0c7` (`THIRD_PARTY.md`).

**What was built**
- **`fonts/fredoka-tr/`**:
  - `upstream/Fredoka-Medium.ttf`, `upstream/Fredoka-SemiBold.ttf` and `upstream/OFL.txt`, byte-identical to the files that shipped until now (`8143bd54…`, `021e30b2…`, `782c3137…`);
  - `recipe.json`, holding the inputs' sources and hashes, the five glyphs, each weight's offsets from §11.1's table, the `name` suffixes, the outputs' hashes and sizes, and the tool versions;
  - `build.py`, `verify.py`, `requirements.txt` (`fonttools==4.60.1`, `opentype-sanitizer==9.2.0`), `build.sh` (shellcheck 0.11.0 clean), `README.md`, and a `.gitignore` for the venv.
- **`build.py`** is the research-analyst's prototype with the three ruled changes:
  - `recalcTimestamp`/`recalcBBoxes` off;
  - a probe-and-writer split;
  - `maxp`'s four composite limits raised by hand from the probe.
  It reads every offset from the recipe, derives none, and refuses an input whose hash differs. One addition: it also reaches pair-kerning subtables wrapped in Extension lookups (Fredoka has none, so the output is unaffected).
- **Faithful port, proven.** With name ID 3's suffix set to the tech-lead's `;TR1`, `build.py` reproduces his `build_fixed3.py` outputs byte for byte (`6b18ffda…`, `ee001bab…`).
- **The shipped files.** With §11.1's `;PegSolitaire-TR1`, `fredoka_medium.ttf` is `0effcf329f276fa382909af579febaed9505803210515eb6325f35fb1b157d4f`, 50,444 B, and `fredoka_semibold.ttf` is `5c764171a284545903f978dbc7a417b80318f89f9c8e01d7b0bfd0bc4890823c`, 50,408 B. That is +512 B and +508 B over the upstream, and +24 B over the tech-lead's reference run (the longer suffix in the name records). A second build reproduced both files byte for byte (`cmp`).
- **`BundledFontRecipeTest`**, 7 tests, JVM, no Python:
  - the shipped files' and the upstream copies' sha256 against the recipe;
  - the upstream `OFL.txt` against the shipped `Fredoka-OFL.txt`;
  - the displayable code points: upstream plus exactly the recipe's five;
  - every upstream glyph's advance;
  - every English string's kerned `TextLayout` advance;
  - each composite advancing and kerning as its Unicode base letter beside every ASCII letter.
- **`BundledFontGlyphCoverageTest`** in its derived shape. Every shipped language (from the folders, through the shared `ResourceFolders.kt`, moved out of `AppLanguagesTest`) × every role of `AppTypography` × the role's file, resolved through `ResourceFont.resId` → `getResourceEntryName`. Each language's characters come from its `strings.xml` plus the base file's untranslated keys, with the 13 letters as Turkish's floor, held to each file's cmap through `java.awt`. There is no allowance list, and `res/font/` must be the only font directory.
- **`licenses_psl_parts`.** English is now the prose date "15 February 2023" (spec §5.4). The U+2011 comment is removed from both strings files.
- **`licenses_fredoka_modified`**, en and tr verbatim from spec §5.4, as the Fredoka entry's third line in `LicenseCatalog.kt`, with `LicenseCatalogTest.EXPECTED` following spec §3.2's row.
- **`THIRD_PARTY.md`**: §2's Fredoka row and §8's lines, as §11.1 (3)(iii)–(iv) gives them.
- **`app/build.gradle.kts`**: `fonts/fredoka-tr/` (minus the venv) is an input of every `Test` task.
- Not touched: `ui/theme`, `ui/home`, `AppFonts`, `Type.kt`, any oracle, `docs/`.

**Red runs, in order**
- `BundledFontGlyphCoverageTest` on the old fonts and strings — fail, as intended, 24 gaps:
  - English × all 15 roles: `U+2011`;
  - Turkish × the 9 Fredoka roles: `U+011E Ğ, U+011F ğ, U+0130 İ, U+015E Ş, U+015F ş`.
- After the prose date — fail, as intended: the 9 Turkish Fredoka gaps only.
- After the rebuild — pass, 2 of 2.
- `BundledFontRecipeTest` before the recipe existed — fail, as intended, 7 of 7 (`FileNotFoundException`: `fonts/fredoka-tr/recipe.json`). After the build — pass, 7 of 7.
- `LicenseCatalogTest` with `EXPECTED` given the third line — fail, as intended (the catalogue lacked it). After the catalogue line it failed again, now on the never-listed scan (deviation 2 below). After the narrow exception — pass, 8 of 8.

**Mutations, each restored, the tree verified afterwards**
- An offset changed in `recipe.json` (Medium `Gbreve` 191 → 192):
  - without a rebuild, `verify.py` fails "Gbreve = G + uni0306 at (192, 200) …";
  - rebuilt without `--record`, `verify.py` fails "output sha256 is the recipe's" and `BundledFontRecipeTest` fails `theShippedFredokaFilesAreTheRecipesOutputs`.
- A sixth cmap entry (U+0122 → `Gbreve`) written into the shipped Medium: `BundledFontRecipeTest` fails the delta ("unexpected (1): 290") and the hash.
- A U+2011 in a string: the coverage test's first red run above is this case, for real.
- "Peg Solitaire" added to the Fredoka credit line: `LicenseCatalogTest` still fails "never listed: Peg Solitaire". The exception covers only the one line.

**`verify.py`, the recorded run** (`fonts/fredoka-tr/.venv/bin/python -I fonts/fredoka-tr/verify.py` on the final tree; Python 3.14.7, fontTools 4.60.1, opentype-sanitizer 9.2.0, HarfBuzz 14.4.0; exit 0). The 237 strings are the 238 base values the upstream can draw: all but `licenses_fredoka_modified`, which carries the five new letters.
```
PASS  input upstream/Fredoka-Medium.ttf sha256 is the recipe's
PASS  input upstream/Fredoka-SemiBold.ttf sha256 is the recipe's
PASS  input upstream/OFL.txt sha256 is the recipe's
== Medium: upstream/Fredoka-Medium.ttf against app/src/main/res/font/fredoka_medium.ttf
PASS  output sha256 is the recipe's
PASS  output size is the recipe's
PASS  upstream glyph order kept, exactly the recipe's glyphs appended
PASS  every original glyph's raw glyf bytes unchanged
PASS  every original loca offset unchanged
PASS  every original hmtx entry unchanged
PASS  same set of tables
PASS  tables byte-identical: GSUB OS/2 STAT gasp prep
PASS  head identical but for checkSumAdjustment
PASS  hhea identical but for numberOfHMetrics
PASS  maxp identical but for numGlyphs and the four composite limits
PASS  maxp numGlyphs counts the added glyphs
PASS  no maxp composite limit lowered
PASS  name differs in IDs 3 and 5 only
PASS  name IDs 3 and 5 are the upstream strings plus the recipe's suffixes
PASS  post header identical; its names are the glyph order above
PASS  cmap adds exactly U+011E, U+011F, U+0130, U+015E, U+015F in every Unicode subtable
PASS  every original GDEF class unchanged
PASS  Gbreve = G + uni0306 at (191, 200), base advance 712, lsb 49, inside the font's extremes
PASS  gbreve = g + uni0306 at (103, 0), base advance 559, lsb 32, inside the font's extremes
PASS  Idotaccent = I + uni0307 at (6, 100), base advance 224, lsb 51, inside the font's extremes
PASS  Scedilla = S + uni0327 at (137, 8), base advance 550, lsb 30, inside the font's extremes
PASS  scedilla = s + uni0327 at (106, -4), base advance 459, lsb 37, inside the font's extremes
PASS  same number of pair-kerning subtables
PASS  every original kerning pair keeps its value
PASS  each composite kerns as its base, on either side of every glyph
PASS  OTS sanitises the file with no warning
PASS  hb-shape: 237 base strings shape alike, glyph ids, advances and offsets
PASS  hb-shape: Ğ ğ İ Ş ş each shape to their one composite glyph ([327, 328, 329, 330, 331])
== SemiBold: upstream/Fredoka-SemiBold.ttf against app/src/main/res/font/fredoka_semibold.ttf
PASS  output sha256 is the recipe's
PASS  output size is the recipe's
PASS  upstream glyph order kept, exactly the recipe's glyphs appended
PASS  every original glyph's raw glyf bytes unchanged
PASS  every original loca offset unchanged
PASS  every original hmtx entry unchanged
PASS  same set of tables
PASS  tables byte-identical: GSUB OS/2 STAT gasp prep
PASS  head identical but for checkSumAdjustment
PASS  hhea identical but for numberOfHMetrics
PASS  maxp identical but for numGlyphs and the four composite limits
PASS  maxp numGlyphs counts the added glyphs
PASS  no maxp composite limit lowered
PASS  name differs in IDs 3 and 5 only
PASS  name IDs 3 and 5 are the upstream strings plus the recipe's suffixes
PASS  post header identical; its names are the glyph order above
PASS  cmap adds exactly U+011E, U+011F, U+0130, U+015E, U+015F in every Unicode subtable
PASS  every original GDEF class unchanged
PASS  Gbreve = G + uni0306 at (187, 200), base advance 715, lsb 42, inside the font's extremes
PASS  gbreve = g + uni0306 at (99, 0), base advance 552, lsb 23, inside the font's extremes
PASS  Idotaccent = I + uni0307 at (-6, 100), base advance 232, lsb 39, inside the font's extremes
PASS  Scedilla = S + uni0327 at (125, 10), base advance 546, lsb 21, inside the font's extremes
PASS  scedilla = s + uni0327 at (96, -4), base advance 458, lsb 26, inside the font's extremes
PASS  same number of pair-kerning subtables
PASS  every original kerning pair keeps its value
PASS  each composite kerns as its base, on either side of every glyph
PASS  OTS sanitises the file with no warning
PASS  hb-shape: 237 base strings shape alike, glyph ids, advances and offsets
PASS  hb-shape: Ğ ğ İ Ş ş each shape to their one composite glyph ([327, 328, 329, 330, 331])
verify.py: all checks passed
```

**Deviations and items for other roles**
1. **`licenses_fredoka_modified` is translatable** (en and tr), not `translatable="false"` as §11.1 (3)(ii) says. This is the ux-designer's D-17, because it is a sentence shown to players. For the tech-lead's record.
2. **One narrow exception in `LicenseCatalogTest.noNeverListedItemIsPresent`, for the code-auditor's approval.**
   - The never-listed "Peg Solitaire" stands for the traditional game, `THIRD_PARTY.md` §1's idea row, which no entry may credit (LS-04).
   - The D-17 line names the app as the font's modifier: the same words, a different referent.
   - `APP_NAMED_AS_MODIFIER` exempts exactly that pair, the line `licenses_fredoka_modified` and the name "Peg Solitaire". Every other word is still checked in that line, and every other line for "Peg Solitaire" (mutation above).
   - The alternative is the ux-designer's: reword the line without the app's name, for example "Modified under the SIL Open Font License: …". The exception would then go.
3. **`THIRD_PARTY.md` written on the producer's instruction.** §11.1 and the addendum name the research-analyst and the release-manager as the writers. Three earlier passages of the verdict note now read wrong for Fredoka, and are left for the research-analyst's "CLEAN as a Modified Version" wording:
   - line 99, "may ship bundled, unmodified";
   - lines 115–117, "All five files match … D1's sha256 and are renamed only";
   - line 160, "the files are unmodified".
4. **The English-advance comparison is derived, not an allowance list.** Both the JVM test and `verify.py`'s `hb-shape` parity compare the base strings the upstream file can draw. That is 237 of 238: the one excluded carries the five letters the upstream never had.
5. **For the release-manager:** checklist 4.6a's hash line, which checks that the AAB's two Fredoka files equal `recipe.json`'s outputs.
6. **For QA:**
   - C-2 on a Turkish device, every Turkish letter in its surrounding face, 100 % and 200 %, including the five strings of open issue 1, now in Fredoka;
   - C-2c on English screens against the earlier screenshots;
   - the licences screen's new line in both languages.
7. **CI risk.** The two font tests use `java.awt` (`Font.createFont`, `TextLayout`); `BundledFontGlyphCoverageTest` already did. The first `ubuntu-latest` run of the branch confirms its JDK's font stack. Locally, macOS ran them in the Check.

**Checks, loop 2**
- `fonts/fredoka-tr/build.sh --record` — pass (venv created, both weights built, recorded; `verify.py` all PASS). `fonts/fredoka-tr/build.sh` again — pass, byte-identical outputs.
- `shellcheck fonts/fredoka-tr/build.sh` — pass, clean.
- `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.locale.*" --tests "com.app.pegsolitaire.ui.licenses.*" …` — pass. Locale: 5 + 2 + 7 + 8. Licences: 16 suites, 0 failures. `AppTypographyRendersBundledFacesTest` 1 and `AppTypographyTest` 4, oracles unchanged. `TimerDigitsTest` 4, `ButtonSoundTest` 20.
- `./gradlew :app:ktlintCheck :app:detekt` — pass (long lines wrapped; the file note in `ResourceFolders.kt` made a block comment).
- `./gradlew :app:compileDebugUnitTestKotlin --rerun` — 73 `w:` in all, unchanged. In the branch's 12 touched test files, only the 3 house-rule `createComposeRule` lines. `:app:compileDebugKotlin --rerun`: 0 `w:`.
- `./gradlew check` on the final tree — pass: `BUILD SUCCESSFUL in 11m 8s`, 107 actionable tasks (26 executed, 81 up-to-date), exit 0. `:app:testDebugUnitTest` 201 suites, 1,675 tests; `:domain:test` 294; `:domain:depthSweep` 1; `:domain:fuzz` 1; `build-logic` 100: 0 failures, 0 errors, 0 skipped. Lint: 0 errors. The touched files carry only the approved `UnusedAttribute` and the pre-existing 10 UnusedResources and 5 PluralsCandidate; nothing on the new string or the fonts.
- `./gradlew :app:assembleDebug`, packaged from scratch — pass. The debug APK goes from 21,581,728 B (loop 0's) to 21,582,724 B, **+996 B**:
  - `fredoka_medium.ttf` 27,711 → 27,983 B compressed;
  - `fredoka_semibold.ttf` 27,982 → 28,289 B compressed;
  - `resources.arsc` 92,248 → 92,668 B (the new line in two languages, the prose date).
  `unzip -p <apk> res/font/fredoka_{medium,semibold}.ttf | shasum -a 256` gives `0effcf32…` and `5c764171…`, the recipe's outputs, which is checklist 4.6a's check.

## Handoff
Next: producer — the studio pauses after this task.
- To the tech-lead: deviation 1 (translatable line) and the branch sign-off once the reviews are in.
- To the code-auditor: deviation 2, approve the exception or have the ux-designer reword.
- To the research-analyst: deviation 3, the verdict note's three stale passages.
- To the release-manager: checklist 4.6a's hash line and the store copy (spec §10).
- When they land: the string deltas of `feature/home-continue-live-boards` and `feature/challenge-star-bands` (spec §9.3).

Next: code-auditor — review `8e010ab..75cb0c7` against `docs/tech-design.md` §11.1 Q-039 and this report's loop 2.
- Read `build.py` and `verify.py`.
- Re-run `fonts/fredoka-tr/build.sh` (it creates `.venv`, and needs a network once). It must change no byte and print all PASS.
- Repeat the named mutations: an offset in `recipe.json`, a sixth cmap entry, a U+2011 in a string.
- Rule on deviation 2.

Next: qa-engineer — a build of this branch on a device:
- C-2 in Turkish: every Turkish letter in its surrounding face, Fredoka headings included, at 100 % and 200 %. Check the five strings of open issue 1 in particular: the restart dialog title, "Titreşim", "Renk körlüğü paleti", "Başarımlar", and the annotations licence title.
- C-2c in English, against the earlier screenshots: nothing may move.
- The licences screen's Fredoka entry: three lines in both languages.
- The rest of spec §8 as before: C-1 to C-7, TalkBack A-1 to A-7, the §6 screenshots.
