# android-game-dev report — licences-screen
Date: 2026-10-24
Branch: feature/licences-screen (d23f7f0..1f14267)
Skills loaded: ergoz-soft:standards (role android-game-dev; the worktree's rules snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus the product's `.claude/rules/lessons.md`), superpowers:test-driven-development (with writing-good-tests.md), gitflow, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran in parallel with: the ux-designer's spec loop 2 (`8f0493a`, on this branch before my first commit); other worktrees' builds on the host
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

> Producer note: returned as text (the harness refuses report files from subagents); written here verbatim by the
> producer as brief commit 17. Brief commit 16 (merge `develop`, meet C7) waits for check-gates, hint and audio.

## Summary
- **Build-time notices:** a build-logic plugin, `google-notices`, generates the Google notices into every variant's assets. It writes `licenses/google/index.tsv` plus one `<stem>.txt` per distinct text, from the root `third_party_licenses.json`/`.txt` of each AAR on the variant's runtime classpath.
- **Parity in `check`:** `verifyGoogleNoticesParity`, wired into `:app:check`, fails when the debug and release trees differ.
- **Curated texts:** nine texts are bundled, each pinned by its full SHA-256 and read through the real `AssetManager`.
- **Screens:** four, plus a Settings › About › Licenses row. The section headers are headings, and the inert row is one TalkBack stop.
- **Acceptance:** each LS-AC is mapped to tests below, with red recorded for every commit. No dependency, permission or dimen token was added.

## Per-commit table
Every test was watched red before its code went in. Where a stub passed a test trivially, a mutation proved the test can fail.

| # | Commit | Red evidence | Green | Tests → LS-AC |
|---|---|---|---|---|
| 1 | `d23f7f0` merge into a sorted index | compile red, then 28 failures against stubs; 2 mutations caught (Long-overflow guard, locale-dependent fold) | build-logic 86/86 | GoogleNoticeJsonTest 14, GoogleNoticeMergeTest 18 → LS-AC12 (a) |
| 2 | `dba02c1` generate per variant | GoogleNoticeOutputTest 6/6 red; GoogleNoticeIndexAssetTest 4/4 red ("index is in the merged debug assets") | 92/92; asset test 4/4 | → LS-AC12 (b), LS-AC11 |
| 2a | `abbe4e8` parity in `check` | 5/6 red against a stub | 6/6; `:app:check` lists the task, writes "identical: debug, release" | VerifyGoogleNoticesParityTest 6 → LS-AC12, brief A1.3 |
| 3 | `237fffc` curated texts at their hashes | FileNotFoundException before the files existed; protected-words scan proven by planting "Sweet" in strings.xml and "King" in an asset | green; committed bytes re-hashed from git | BundledLicenseTextHashTest, UserFacingTextHasNoProtectedNamesTest 3 → LS-AC12, LS-AC04, LS-AC17 |
| 4 | `8cde96d` read off the main thread | 5/6 red; mutation to `Dispatchers.IO` caught | green | AssetLicenseTextSourceTest 4, GoogleNoticeTextTest 2 → LS-AC05, LS-AC14 |
| 5 | `70c43a8` re-flow | 7/8 red | 8/8 (the 800 KB synthetic text re-flows in 63 ms) | LicenseTextReflowTest 8 → LS-AC05 |
| 6 | `0f85860` parse the index | 7 red; 7 single-check mutations, each caught by its own test | green | GoogleNoticeIndexParserTest 13, AssetGoogleNoticeSourceTest 2, asset parse test 1 → LS-AC12 (b) |
| 7 | `ce64f1b` catalogue and strings | 3 red against an empty catalogue; 3 planted violations caught | 8/8 | LicenseCatalogTest 8 → LS-AC03 |
| 8 | `0db0321` LicenseTextViewModel | 6/6 red | 6/6 | LicenseTextViewModelTest 6 → LS-AC10, LS-AC14 |
| 9 | `c376bf3` notices ViewModels and ordinals | 12 red; 2 ordinal mutations caught | green | GoogleNoticeOrdinalsTest 3, GoogleNoticesViewModelTest 4, GoogleNoticeTextViewModelTest 7 → LS-AC13, LS-AC14 |
| 10 | `a77799e` refactor to SettingsRows.kt | no new tests | the five Settings suites pass unedited | (brief T3) |
| 10a | `f76fbf5` one stop per inert row; headings | 2/2 red | green | SettingsPlayGamesTest +1, SettingsScreenTest +1 → LS-AC06, LS-AC01 |
| 11 | `8eccd60` Licenses screen and entry card | 14/14 red against an empty screen | green | LicensesScreenTest 5, LicencesTalkBackContractTest 6, ButtonSoundTest +3 → LS-AC06, LS-AC09, LS-AC10 |
| 12 | `c8cec11` License text screen and delayed spinner | 9/9 red | green | LoadingAfterDelayTest 3, LicenseTextScreenTest 6 → LS-AC05, LS-AC10, LS-AC14, LS-AC16 |
| 13 | `7b5e085` notices list and notice text screens | 10/10 red | green | GoogleNoticesScreenTest 5, GoogleNoticeTextScreenTest 4, ButtonSoundTest +1 → LS-AC10, LS-AC13, LS-AC14, LS-AC15, LS-AC09 |
| 14 | `5f51dc8` four routes and the licences graph | 4/4 red against an empty graph | green | LicensesNavFlowTest 4 → LS-AC02, LS-AC13 |
| 15 | `20e85d7` About section and Licenses row | 5 red; the new Settings round-trip test was **not** watched red (see deviation 9) | green | SettingsScreenTest +3 (headings now four), ButtonSoundTest +1, LicensesNavFlowTest +1 → LS-AC01, LS-AC02, LS-AC09 |
| 15a | `1f14267` stagger cap (spec loop 2, F-23) | 1/1 red | green | LicensesUiTest 1 |

LS-AC07 is QA's and the auditor's (no test). LS-AC08 belongs to the candy branch (E7). LS-AC11 and the release half of LS-AC18 are the release-manager's.

## Pinned hashes (fetched 2026-10-24T01:34Z; for THIRD_PARTY.md and checklist 4.6a (c))
Every file matched its recorded size and its prefix or full hash, so no stop was needed.

| File | Bytes | Full SHA-256 | Source |
|---|---|---|---|
| Fredoka-OFL.txt | 4,482 | 782c313774e59f7f0e2face680137ad0b578822ba1582160f30cb333cf31f7e0 | already committed, not re-fetched (CRLF, as it was) |
| Nunito-OFL.txt | 4,479 | 66db16f4d402431d22c354c3e7c740ab550f1ba1ef7cd7b70ff2ba13829734a2 | already committed, not re-fetched (CRLF, as it was) |
| Apache-2.0.txt | 11,358 | cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30 | https://www.apache.org/licenses/LICENSE-2.0.txt |
| Jakarta-Inject-NOTICE.md | 1,074 | 992ed62dd3ee17d73ce6a4a5e02dd0fed9eb326bf386aa5304e6a512214df540 | `jakarta.inject-api-2.0.1.jar!/META-INF/NOTICE.md`, from the Gradle cache; no final newline, committed byte-identical |
| Protobuf-BSD-3-Clause.txt | 1,732 | 6e5e117324afd944dcf67f36cf329843bc1a92229a8cd9bb573d7a83130fea7d | raw.githubusercontent.com/protocolbuffers/protobuf/v22.3/LICENSE |
| Chromium-BSD-3-Clause.txt | 1,536 | 368cca1106be99d39ecd32a38d8305585d802a475effb66380b91ffc9bcf709b | raw.githubusercontent.com/chromium/chromium/143.0.7445.0/LICENSE |
| CheckerQual-MIT.txt | 1,126 | e90eaa08dea089df10f2bf7f9dba63a5f6237c01562c4741e2e86773e78582d5 | raw…/checker-framework-3.33.0/checker-qual/LICENSE.txt |
| PublicSuffixList-NOTICE.txt | 218 | 8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a | raw…/okhttp/parent-4.11.0/…/publicsuffix/NOTICE |
| MPL-2.0.txt | 16,726 | 3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04 | https://www.mozilla.org/media/MPL/2.0/index.txt |

The seven fetched texts are LF with no trailing spaces.

## Outputs
- `build-logic/src/main/kotlin/` — the build-time side: `GoogleNoticeJson.kt`, `GoogleNoticeMerge.kt` (pure parser and merge); `GoogleNoticeOutput.kt`, `GenerateGoogleNoticesTask.kt` (reading the AARs, writing the tree); `GoogleNoticesParity.kt`, `VerifyGoogleNoticesParityTask.kt` (debug/release comparison); `google-notices.gradle.kts` (the convention plugin); four test files beside them.
- `app/build.gradle.kts` — one plugin line, plus its comment.
- `app/src/main/java/com/app/pegsolitaire/data/licenses/` — `LicenseAsset`, `BundledLicenseText`, the two sources, `GoogleNoticeAssets`, `GoogleNotice`, the index parser.
- `app/src/main/java/com/app/pegsolitaire/ui/licenses/` — the catalogue, the three ViewModels, the four screens and routes, the shared top bar and card, the loading pieces, and the re-flow.
- `app/src/main/java/com/app/pegsolitaire/di/` — the new `DispatchersModule.kt`, and the licences block in `DataModule.kt`.
- `navigation/` — `Routes.kt`, `AppNavHost.kt`, and the new `LicensesDestinations.kt`.
- `ui/settings/` — the new `SettingsRows.kt`, and the About section in `SettingsScreen.kt` and `SettingsRoute.kt`.
- The seven curated texts in `app/src/main/assets/licenses/`, and the strings block at the end of `res/values/strings.xml`.

## Checks
- `./gradlew --max-workers=4 check` on `1f14267` — pass. `BUILD SUCCESSFUL in 11m 7s`, exit 0. `:app` 1,178 tests, `:domain` 274 plus the depth sweep (1), build-logic 98, all with 0 failures and 0 skipped (`:app` was 1,071 before this branch, build-logic 54). Lint: 0 errors, 58 warnings, the same set as the baseline.
- `./gradlew --max-workers=4 :app:check` at every `:app` commit — pass. Each build-logic step also ran `./gradlew -p build-logic test`.
- `./gradlew :app:generateDebugGoogleNotices` (brief §1.5) — pass. `index.tsv` has 108 lines; `cut -f4 | tr ',' '\n' | sort -u` gives exactly the ten measured artifactIds. 42 stems in 43 files (42 texts and the index), 2,065,947 B (2,053,748 B of texts, 12,199 B index). `diff -r` of the debug and release trees: identical.
- `./gradlew :app:mergeDebugAssets` — pass. The merged `licenses/` holds the nine curated texts (42,731 B) beside `google/` (43 files, 2,065,947 B).
- Protected-words grep over the generated `google/` directory (LS-AC17, information) — 0 files.
- LS-AC18 debug APK delta: `:app:assembleDebug` here 21,673,696 B; a throwaway worktree at `develop` `2d045d1` gives 20,977,393 B; delta **+696,303 B** (debug is not minified, so this overstates the release delta). The throwaway worktree had only `sdk.dir` and has been removed.
- `git diff 4d3c82b..HEAD -- gradle/libs.versions.toml build-logic/build.gradle.kts` — empty: no coordinate added (LN-7 item 6).
- `assets.open` call sites (LS-AC14) — exactly two: `AssetLicenseTextSource.kt:30` and `AssetGoogleNoticeSource.kt:31`.
- LS-AC07 grep for `Intent`, `Uri`, `URL`, `Socket`, `http` over `ui/licenses` and `data/licenses` — 0 hits.
- Added lines grepped for `@Suppress`, `!!`, `runBlocking`, `GlobalScope`, `@Ignore`, `tools:ignore` — 0 hits.

## Deviations
1. The task's file work moved into a Gradle-free object: `GoogleNoticeOutput` reads the AARs and writes the tree, with its own test (6 cases); `GenerateGoogleNoticesTask` is a thin shell over it.
2. The parity task's inputs take a different shape from the brief's: Gradle cannot fingerprint a `MapProperty<String, Directory>` as `@InputFiles`, so the task keeps the variant names in an `@Internal` map (for the error message) and fingerprints the trees through an `@InputFiles` file collection. It compares any number of variants and fails with fewer than two.
3. Some files and tests landed in a different commit from the brief's table: `GoogleNoticeText` and `GoogleNoticeAssets` in commit 4 (its test reads one); each route with its first consumer (`LicenseText` in 8, `GoogleNoticeText` in 9, the other two in 14); each `ButtonSoundTest` case with the screen it covers; the text screens' TalkBack checks in their own screen and loading suites.
4. Commit 15a (`1f14267`) is extra: spec loop 2 (F-23) answered the brief's open count (F6) — 8 items, the intro counted, on both lists. Both lists share one capped entrance helper, and `SettingsSectionHeader` takes an optional order (Settings always passes one).
5. The Jakarta credit follows its NOTICE (F-16): `licenses_jakarta_credit` reads "The Eclipse Jakarta Dependency Injection project", from the NOTICE's line "This content is produced and maintained by the Eclipse Jakarta Dependency Injection project." The NOTICE names the Eclipse Foundation only as trademark holder.
6. The date in `licenses_psl_parts` uses non-breaking hyphens (U+2011): it renders the same, won't wrap mid-date, and clears lint's TypographyDashes false positive without a suppression.
7. The card chrome is shared: `LicenceCard` is used by both the entry card and the notice row, so the merge, role and click label live in one place.
8. Two tests use fallback oracles: selection is checked structurally (every paragraph under the one tagged `SelectionContainer`, brief §4.1; the gesture is QA's §8.2 item 8); the reading-order oracle sorts by on-screen position, because the Scaffold puts its top bar after the body in the semantics tree.
9. One test was not watched red: `settingsOpensLicensesAndBackReturnsToSettings` passed on its first run, because the one-line Settings edge was wired with the parameter plumbing before it ran.
10. One untested branch: `AssetGoogleNoticeSource`'s IOException-to-null branch, because Robolectric cannot remove a merged asset.
11. New suites use the v2 Compose rule, so they add no deprecation warnings (tech design §9.8, no growth); existing suites were not migrated.
12. None of the brief's stop conditions was met: the real JSON is the flat shape; no AGP-internal import was needed; the non-deprecated `hiltViewModel` resolves; the outer `CollectionInfo` wins over the lazy layout's own; every hash matched.

## Open issues / risks
- Commit 16 waits for check-gates, hint and audio on `develop`; then the Check moves to `./scripts/check.sh`. New main code is already written to C7's two patterns (`@param:` targets, the new `hiltViewModel`).
- Opt-ins for the auditor: `@OptIn(ExperimentalMaterial3Api)` on `LicensesTopBar` (the existing `TopAppBar` idiom), and `@OptIn(ExperimentalCoroutinesApi)` in six test files.
- First frame (§5.3): `App.kt`, `MainActivity.kt`, `ui/home`, `ui/theme`, fonts, themes and the manifest are untouched. The two new bindings are unscoped and built only on request.

## Handoff
- **producer** — commit this report as brief commit 17; schedule commit 16 once check-gates, hint and audio are on `develop`.
- **code-auditor** — review `d23f7f0..1f14267` against the brief and spec: the LS-AC07/AC10/AC14 greps above, the first-frame confirmation, the opt-ins, the parity task's input shape, the U+2011 date, deviations 8–10.
- **qa-engineer** — the §8.2 device checks on the release build the release-manager supplies (generated assets included): the pass line per screen, Cronet's `gfxinfo` line, the last notice row against the AAB's `index.tsv`, Settings heading navigation.
- **release-manager** — copy the full hashes above into THIRD_PARTY.md and checklist 4.6a (c), adding `MPL-2.0.txt`'s `3f3d9e00`; record the release AAB delta at 4.6 (LS-AC18 release half; the debug delta is +696,303 B); note the Jakarta credit wording.
- **ux-designer** — confirm the Jakarta credit (F-16) and the U+2011 date.
- **tech-lead** — sign off, reading deviations 1–12.
