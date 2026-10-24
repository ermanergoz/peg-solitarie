# code-auditor report — licences-screen
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer) — reviewed `d23f7f0..1f14267` (head `0df66ec`, base `4ff4ec6`)
Skills loaded: ergoz-soft:standards (role code-auditor; the worktree snapshot is 0.6.2 and the plugin 1.0.0, so the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing and ip-policy were read in full; `.claude/rules/lessons.md` read), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: other worktrees' builds on this host (no emulator used)
Ran on: claude-opus-5-5 at effort xhigh. **Deviation:** the agent file pins fable. Fable's limit is reached, and the founder's Q-019 answer (option 3) allows Opus for every Fable-pinned role until 3.0.0 is published. So this review ran on the developer's own model family. To make up for that, every result below was re-run or re-derived here, and no recorded result was trusted.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
The build-time notices are correct and verified independently:
- A separate script of mine, reading the ten AARs, rebuilds `index.tsv` byte-identical to the generated one.
- Debug and release are byte-identical. The configuration cache stores and reuses the task graph.
- Every curated text matches its pinned SHA-256, and re-fetching each one from its public URL gives the same bytes.

The Check passes, and 24 of 25 mutation probes fail a test. Three things must still be fixed:
- **F1 (Major):** the new main code adds seven Kotlin compiler warnings of exactly the kind C7 gates. This contradicts LS-AC10, LN-7 item 6 and the brief.
- **F2 (Minor):** the overflow test does not pin the overflow guard.
- **F3 (Minor):** the notice text screen joins the sources in composition, which spec §6 forbids, and the separator is duplicated.

## Findings
Severity: Critical / Major / Minor / Info. **F1–F3 must be fixed for a PASS**; F4–F10 are advisory or records.

1. **F1 — Major — the new code is not warning-free under C7.** Recompiling `1f14267` from scratch (`:app:compileDebugKotlin --rerun` in a throwaway worktree) prints seven new `w:` lines. Each reads "This annotation is currently applied to the value parameter only, but in the future it will also be applied to field.":
   - `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicenseCatalog.kt:15` (`BundledTextRef.heading`)
   - `LicenseCatalog.kt:34` (`LicenseLine.Plain.text`)
   - `LicenseCatalog.kt:44` (`LicenseLine.Address.text`)
   - `LicenseCatalog.kt:60` (`LicenseEntry.title`)
   - `LicenseCatalog.kt:61` (`LicenseEntry.licenseName`)
   - `LicenseCatalog.kt:69` (`LicenseSection.title`)
   - `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicenseTextUiState.kt:35` (`LoadedTextPart.heading`)

   Each is a `@StringRes` on a data-class constructor `val`. Tech design §9.8 makes `:app` main warning-free and gated (C7), and names the fix for this exact kind: an explicit `@param:` target. LS-AC10 (amended) and LN-7 item 6 require the branch to be "warning-free under C7". The brief's §2.3 and §6 require the same "from the first line": whichever of this branch and check-gates lands second will fail its Check on these seven lines.

   The developer's report says "New main code is already written to C7's two patterns". That holds for the `@Inject` qualifiers and `hiltViewModel`, but not for these seven annotations. **Fix:** `@param:StringRes` at the seven sites. The ten older main warnings (four annotation-target, six deprecated `hiltViewModel`) predate this branch and are check-gates' to fix.
2. **F2 — Minor — the overflow test does not test the overflow guard.**
   - `build-logic/src/test/kotlin/GoogleNoticeMergeTest.kt:103-108` (`aSliceWhoseEndOverflowsFailsNamingTheAar`) uses `start = Long.MAX_VALUE, length = 1`. The earlier condition `slice.start <= txt.size` (`build-logic/src/main/kotlin/GoogleNoticeMerge.kt:122`) already rejects that start, so the guard the test names, `slice.length <= txt.size - slice.start` (`GoogleNoticeMerge.kt:123`), is never reached.
   - **Proof:** I replaced line 123 with the naive `slice.start + slice.length <= txt.size`, and all 98 build-logic tests stayed green.
   - Under that regression, `start = 1, length = Long.MAX_VALUE` wraps to a negative sum, passes the check, and `copyOfRange` throws `IllegalArgumentException`. The build would then fail without the `GoogleNoticeMergeException` that names the AAR.
   - **Fix:** use a small start and a huge length, for example `start 1, length ${Long.MAX_VALUE}`. The test's KDoc claim is then true.
   - The developer's report says this mutation was caught; my probe shows it is not.
3. **F3 — Minor — sources joined in composition, and the separator duplicated.**
   - `app/src/main/java/com/app/pegsolitaire/ui/licenses/GoogleNoticeTextScreen.kt:93` builds the `google_notices_found_in` argument in composition with `header.notice.sources.joinToString(SOURCES_SEPARATOR)`.
   - Spec §6 says that argument is "built once per row in the ViewModel's row model, never in composition".
   - The `", "` separator is declared twice: `GoogleNoticeTextScreen.kt:136` and `GoogleNoticesViewModel.kt:54`. If the two drift, the same notice reads differently on its two screens.
   - This is not among the developer's deviations.
   - **Fix:** carry the joined string in `LicenseTextHeader.Notice`, built in `GoogleNoticeTextViewModel` by the same function the list uses, with one constant.
4. **F4 — Info — inaccuracies in the developer's report** (the code's substance holds):
   - **LS-AC07 grep:** "0 hits" is really 5 lines. All of them are the `okhttp` identifier or KDoc (`LicenseCatalog.kt:83,221,222,223,288`). No `Intent`, `Uri`, `URL`, `Socket` or link API appears in `ui/licenses` or `data/licenses`.
   - **LS-AC14 grep:** the literal grep the brief and spec name, `grep -rn 'assets.open' app/src/main`, prints **0** lines, not "exactly two". ktlint splits the call across lines: `AssetLicenseTextSource.kt:29-30`, `AssetGoogleNoticeSource.kt:30-31`. `grep -rn '\.assets\b'` confirms these are the only two `AssetManager` uses. Each opens either a `LicenseAsset.assetPath` or the constant `GoogleNoticeAssets.INDEX_PATH`, so nothing from a route is opened. The spec's grep oracle should be corrected (ux-designer/tech-lead).
   - **Two other false claims:** the overflow-mutation claim (F2) and the "C7 patterns" claim (F1).
5. **F5 — Info — the two `GoogleNoticeText` types clash in name.**
   - The route `navigation/Routes.kt:191` and the asset `data/licenses/LicenseAsset.kt:28` share the name `GoogleNoticeText`.
   - That forces `import … as GoogleNoticeTextRoute` in `ui/licenses/GoogleNoticeTextViewModel.kt:22`, which is the name of the composable `GoogleNoticeTextRoute.kt:16` in the same package.
   - It compiles, because classes and functions live in different namespaces, but it misleads readers. Advisory: rename the asset type, for example to `GoogleNoticeTextAsset`.
6. **F6 — Info — `GoogleNoticeMerge.kt:83` (`texts[stem] = text`) overwrites without checking.** If two different texts ever shared a 64-bit stem, it would keep one silently. The chance is about 2⁻⁵⁸ for 42 texts. Advisory: a `check` that an existing entry is byte-equal would cost one comparison.
7. **F7 — Info — an allocation in the lazy content.** `GoogleNoticeTextScreen.kt:82` runs `state.parts.flatMap { it.paragraphs }` inside the lazy content lambda, copying a list of 2,497 references on each state change (2–3 per open). Negligible (compose-performance-audit); advisory.
8. **F8 — Info, for the release-manager — the sound counts.** `LicenseCatalog.kt:140-142` (7/2/1) follow THIRD_PARTY.md §2's WAV rows from `feature/audio-review`. This tree's `res/raw/` still ships the `.ogg` set, which spec §3.2.1 says has nine Kenney cues. The spec assigns the re-cut to the release-manager at the cut. CC0 carries no attribution duty. Still, if this branch reaches `develop` before audio-review, `develop` under-counts Kenney by two cues until audio-review lands.
9. **F9 — Info — `licenses_psl_parts` (`res/values/strings.xml:499`) writes its date with U+2011 non-breaking hyphens** instead of the spec's ASCII hyphens (deviation 6). It is a content choice, not a suppression, and it is accepted. QA should note how TalkBack reads the date; the ux-designer confirms the string (the handoff already exists).
10. **F10 — Info — deviation 10 accepted.** The `IOException` → `null` branch at `data/licenses/AssetGoogleNoticeSource.kt:34-35` is untested, because Robolectric cannot remove a merged asset. It is one line, and its sibling in `AssetLicenseTextSource` is tested (`returnsNullForAStemThatDoesNotExist`).

## Suppressions and opt-ins
- **No new suppressions.** I found no new `@Suppress`, baseline, `@Ignore`, `tools:ignore`, ktlint or detekt disable, `!!`, `runBlocking` or `GlobalScope`. I checked every added line of `develop...1f14267`: every hit is in `.planning/` or `docs/` prose. So there is nothing to approve.
- APPROVED OPT-IN: `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicensesTopBar.kt:26` — `@OptIn(ExperimentalMaterial3Api::class)` for `TopAppBar`. It is the same idiom as `SettingsScreen.kt`, `HomeScreen.kt`, `ClassicCatalogScreen.kt` and `GameScreen.kt`. It is an opt-in, not a suppression.
- APPROVED OPT-IN: `@OptIn(ExperimentalCoroutinesApi::class)` in the six new test files. They use the coroutines-test virtual-time API, which is the fix tech design §9.8 names for that warning kind. The new test sources print no `w:` line.
- Noted, not a suppression: `catch (ignored: IOException)` (`AssetLicenseTextSource.kt:33`, `AssetGoogleNoticeSource.kt:34`) uses detekt's allowed name. The swallow is the documented error state.

## What was verified and holds
- **Hashes.** All nine curated texts were hashed from the committed blobs (`git show 1f14267:…`), and each equals its constant in `BundledLicenseTextHashTest`. I re-fetched the seven new ones from public URLs (apache.org, mozilla.org, raw.githubusercontent.com at the pinned tags, and Maven Central's `jakarta.inject-api-2.0.1.jar!/META-INF/NOTICE.md`): sizes and SHA-256 match byte for byte. The full hashes:

  | File | Size | SHA-256 |
  |---|---|---|
  | `Apache-2.0.txt` | 11,358 B | `cfc7749b…3d30` |
  | `Jakarta-Inject-NOTICE.md` | 1,074 B | `992ed62d…a512214df540` |
  | `Protobuf-BSD-3-Clause.txt` | 1,732 B | `6e5e1173…fea7d` |
  | `Chromium-BSD-3-Clause.txt` | 1,536 B | `368cca11…f709b` |
  | `CheckerQual-MIT.txt` | 1,126 B | `e90eaa08…582d5` |
  | `PublicSuffixList-NOTICE.txt` | 218 B | `8a9c58fb…876a` |
  | `MPL-2.0.txt` | 16,726 B | `3f3d9e00…9d04` |

  The two OFL texts equal THIRD_PARTY.md §2's prefixes and suffixes.
- **Google notices, regenerated** (both variants, `--rerun`):
  - `index.tsv`: 108 lines, 108 unique keys, 42 stems; 43 files (42 texts of 2,053,748 B, plus the 12,199 B index).
  - Every indexed stem has a file, and every file's SHA-256 starts with its stem.
  - The `<sources>` set is exactly the ten measured artifactIds.
  - `diff -r` of debug and release: identical.
  - An independent re-derivation from the ten AARs in the Gradle cache (my own script) is **byte-identical** to the generated `index.tsv`.
  - `:app:dependencies --configuration releaseRuntimeClasspath` lists exactly those ten Google AAR modules, plus `okhttp:4.11.0`. The one other AAR in the cache with notices, `play-services-nearby`, is not on the graph.
  - The protected-words grep over the generated directory finds 0 files, which agrees with the developer's record.
- **Licence obligations.**
  - The catalogue equals spec §3.2/§3.2.1 in order (`LicenseCatalogTest`, mutation-proven), and every THIRD_PARTY.md row named there reaches an entry.
  - MPL-2.0 §3.2(a): the Public Suffix List entry shows the exact-version address `github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat`. That `.dat` re-fetched at `a312abe0` equals okhttp `parent-4.11.0`'s copy (SHA-256 `e8b27397…cdd3`, 246,016 B), and the release graph's okhttp is 4.11.0. The entry also opens the NOTICE, then the full MPL text.
  - The BSD-3, MIT, Apache-2.0 (with the Jakarta NOTICE first) and OFL texts are bundled whole. Re-flow changes whitespace only (`reflowChangesWhitespaceOnlyForEveryCuratedText`, mutation-proven).
  - No text is a link, and no new permission exists.
- **Architecture.**
  - `data/licenses` imports nothing from `ui/`, and `ui/licenses` imports only the data types and interfaces (T1 holds).
  - `:domain` is unchanged by the branch and has no Android import.
  - No new coordinate: `git diff develop...1f14267 -- gradle/libs.versions.toml build-logic/build.gradle.kts` is empty, as are the manifest, `dimens.xml` and `THIRD_PARTY.md`. There are no dp, sp or colour literals in the new UI.
  - Light and dark previews exist for all four public screens.
  - The two new bindings are unscoped; the IO dispatcher is qualified and injected.
- **First frame.** `App.kt`, `MainActivity.kt`, `ui/home/`, `ui/theme/`, `res/font/`, the themes and the manifest are untouched. No singleton does work at construction, and nothing is read from the root. `navigation/` and `di/` changes only add graph entries and unscoped bindings. **No 15e sitting is needed** (brief §5.3).
- **Configuration cache.** `--configuration-cache` over the three notices tasks first printed "Configuration cache entry stored." with no problems. The second run printed "Reusing configuration cache." and ran all three tasks from the cached graph; parity wrote "identical: debug, release".
- **Parity is wired into the Check.** The verbose Check log shows `:app:verifyGoogleNoticesParity` inside `check`.
- **Lint.** 0 errors and 58 warnings, none of them in a new file or in the licences block of `strings.xml`. That matches the developer's count.
- **Deviations 1–12.**
  - 1, 2, 3, 4, 5 (verified against the jar's NOTICE line 3), 7, 11 and 12 are sound.
  - 6: see F9.
  - 8: the structural selection oracle is allowed by LS-AC05; the geometric reading-order oracle approximates TalkBack and is accepted, with QA's device pass the authority.
  - 9: I watched `settingsOpensLicensesAndBackReturnsToSettings` fail by removing the Settings → Licenses edge, so it is now proven.
  - 10: see F10.
- **Mutation probes.** 25 in all, in a throwaway `git worktree add --detach` of `1f14267` (since removed). 24 were killed, each by its named test:

  | Area | Mutation | Test that failed |
  |---|---|---|
  | Index parser | no stem hex check | `aStemThatIsNotSixteenHexDigitsIsNull` |
  | Index parser | no key uniqueness | `aDuplicateKeyIsNull` |
  | JSON parser | no duplicate-name check | `aDuplicateNameIsRejected` |
  | Merge | locale fold (`lowercase(Locale.getDefault())`) | `theOrderDoesNotDependOnTheDefaultLocale` |
  | Merge | tab/line-break check removed | `aNameWithATabOrLineBreakFailsNamingTheAar` |
  | Merge | JSON without TXT contributes nothing | `jsonWithoutItsTextFailsNamingTheAar` |
  | Parity | byte comparison dropped | `aDifferingByteFailsNamingTheFile` |
  | Parity | minimum variants 1 | `aSingleVariantFails` |
  | Hash test | one byte appended to `CheckerQual-MIT.txt` | `everyBundledTextHashesToItsPinnedSha256` |
  | Words scan | "Sweet" planted in `strings.xml` | `noStringResourceNamesAProtectedWord` |
  | Words scan | "King" in a planted `assets/licenses/google/` file | `noCuratedLicenceTextNamesAProtectedWord` and `theLicencesAssetDirectoryHoldsFilesOnly` |
  | Merge on inert rows | merge dropped on `LicenceCard` | `inertEntriesAreOneMergedNodeWithNoClickActionAndNoRole` |
  | Merge on inert rows | merge dropped on `SettingsLinkRow` | `theSignedInStatementRowIsOneStopWithNoClickActionAndNoRole` |
  | Headings | dropped from `SettingsSectionHeader` | `everySectionHeaderIsAHeading` and the Licenses headings test |
  | Headings | dropped from the top-bar title | `aTwoTextEntryShowsNoticeThenItsLicenceHeadingInThatOrder` |
  | Off-main reads | `AssetLicenseTextSource` on a hard-coded `Dispatchers.IO` | `readsOnTheInjectedDispatcher` |
  | Off-main reads | `AssetGoogleNoticeSource` without its switch | `readsOnTheInjectedDispatcher` |
  | Off-main reads | either ViewModel re-flowing on the caller | `theReflowRunsOnTheInjectedDispatcher` |
  | Delayed spinner | `delay(0)` | both timing tests in `LoadingAfterDelayTest` |
  | Collection semantics | outer `CollectionInfo` dropped | `showsEveryIndexLineInOrderWithItsFoundInLine` (5 ≠ 4) |
  | Catalogue | two entries swapped | `theCatalogueEqualsTheThirdPartyMappingInOrder` |
  | Re-flow | join space dropped | six re-flow tests |
  | Navigation | Settings edge removed | `settingsOpensLicensesAndBackReturnsToSettings` |

  The one survivor is the overflow guard: see F2.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/code-auditor.md` — this report, not committed (as dispatched).

## Checks
- `cd PegSolitarie-lic && ./gradlew --max-workers=4 --console=verbose check :app:testDebugUnitTest --rerun :build-logic:test --rerun` (HEAD `0df66ec`, clean tree) — **pass**: `BUILD SUCCESSFUL in 11m 33s`, exit 0. Results:
  - `:app`: 1,178 tests in 120 classes, 0 failures, 0 skipped, run fresh.
  - build-logic: 98 tests, 0 failures, run fresh.
  - `:domain`: 274 tests (up to date; the branch does not touch `:domain`) and the depth sweep, 1 test, run fresh.
  - detekt and ktlint up to date. Lint: `0 errors, 58 warnings`.
  - `:app:generateDebugGoogleNotices`, `:app:generateReleaseGoogleNotices` and `:app:verifyGoogleNoticesParity` are in the graph.

  An earlier run's log at the shared scratchpad path `check1.log` was clobbered by another agent's PegSolitarie-gates run writing to the same file name. It was discarded, and the run above is the record.
- `./gradlew --configuration-cache --configuration-cache-problems=warn :app:generateDebugGoogleNotices --rerun :app:generateReleaseGoogleNotices --rerun :app:verifyGoogleNoticesParity --rerun`, twice — pass ("Configuration cache entry stored.", then "Reusing configuration cache."; `BUILD SUCCESSFUL`).
- `./gradlew -q :app:dependencies --configuration releaseRuntimeClasspath` — pass (the ten Google AAR modules; `okhttp:4.11.0`).
- `python3 -I oracle.py` (my own re-derivation of LN-3 from the ten cached AARs), then `cmp` with the generated `index.tsv` — pass (byte-identical; 108 lines, 42 texts, 2,053,748 B).
- `diff -r generateDebugGoogleNotices generateReleaseGoogleNotices`; a per-stem `shasum -a 256` prefix check; the stem-file presence check — pass.
- `git show 1f14267:app/src/main/assets/licenses/<f> | shasum -a 256` for the nine texts, and `curl -sS -L` of the seven public sources, then `shasum -a 256` — pass (all equal the pins).
- `curl` of the PSL `.dat` at `a312abe0…` and of okhttp `parent-4.11.0`'s test copy, then `shasum -a 256` — pass (both `e8b27397…`).
- The words grep (16 words, whole-word, case-insensitive) over `app/build/generated/assets/generateDebugGoogleNotices/licenses/google/` — 0 files.
- `grep -rn -E 'Intent|Uri|http|URL|Socket'` over `ui/licenses` and `data/licenses` — 5 lines, all `okhttp` (F4). `grep -rn '\.assets\b' app/src/main` — the two sources only.
- `git diff develop...1f14267 -- gradle/libs.versions.toml build-logic/build.gradle.kts`, the manifest, `dimens.xml`, `THIRD_PARTY.md`, `App.kt`, `MainActivity.kt`, `ui/home`, `ui/theme`, `res/font` — empty.
- Throwaway worktree: `:app:compileDebugKotlin --rerun :app:compileDebugUnitTestKotlin --rerun` — **fail on the acceptance bar**: compiled, but 7 new main `w:` lines (F1); 0 `w:` lines in the new test files.
- Throwaway worktree, app mutations: `:app:testDebugUnitTest --tests …` (14 classes) — 17 of 89 failed, as intended. Then 4 classes — 10 of 24 failed, as intended.
- Throwaway worktree, build-logic mutations: `./gradlew -p build-logic test` — 6 of 98 failed, as intended. The overflow guard mutant survived (F2).
- The refactor commit `a77799e`: the removed and added blocks diffed modulo `private` → `internal`. Pass (a pure move); the behaviour changes are in `f76fbf5`, as the brief orders.

## Open issues / risks
- F1–F3 go back to the android-game-dev. Then a re-review covering the fix and the pending brief commit 16 (merge `develop`, which has since taken in `bugfix/hint-undecided` at `b1a6cf9`, plus check-gates). That re-review must run `:app:compileDebugKotlin --rerun` and confirm no new `w:` line.
- The device half (LS-AC05/06/13/15/16, §8.2) is QA's, on the release build the release-manager supplies. The release AAB checks (LS-AC11, LS-AC12 (c), 4.6a) are the release-manager's.
- The shared session scratchpad is written by several agents under colliding names. My first `check1.log` collided with another agent's gates run, and I overwrote a stale `added.txt` there before moving my work into `scratchpad/ca-lic/`. The producer may want each dispatch to name its own scratch subdirectory.
- The spec's LS-AC14 grep oracle prints nothing on ktlint-formatted code (F4). The ux-designer or tech-lead should change it to `grep -rn '\.assets\b' app/src/main`.

## Handoff
Next: producer — send F1, F2 and F3 to the android-game-dev in fix mode, in `PegSolitarie-lic`. F4–F10 are advisory or records:
- F8 goes to the release-manager.
- F9 goes to the ux-designer and QA.
- F4's grep oracle goes to the ux-designer or tech-lead.

After the fix, re-dispatch the code-auditor on the fix range together with the commit-16 merge-in. There is no tech-lead sign-off until then.

VERDICT: FAIL
