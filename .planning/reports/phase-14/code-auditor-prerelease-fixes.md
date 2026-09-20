# code-auditor report — phase 14 pre-release fixes

Date: 2026-09-21
Branch: feature/phase-14-prerelease-fixes (4561e67..7df9b02)
Skills loaded: `ergoz-soft:standards` (role code-auditor) → plugin copies of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`, `play-store.md`, `kotlin-android.md`, `compose.md`, `testing.md` (plugin v0.5.2 is newer than the repo snapshot `.claude/rules/VERSION` 0.5.1, so the plugin copies were read); repo `.claude/rules/lessons.md`; `compose-skill`; `security-reviewer`. **Not loaded:** `code-reviewer`, `gsd-code-review` — the correctness/security/performance passes they cover were done by hand against the rule files; recorded here rather than left implicit.
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` absent)


Verdict: FAIL — one blocking finding (F-1: the new source sets are outside detekt).

> Saved by the producer from the code-auditor's final message.

## Summary
- Requirements 1–4 are all correctly implemented and the evidence behind them holds up: I re-derived the release exclusion and two of the three mutation results myself.
- One blocking finding: the new `debug` / `release` / `testDebug` source sets are outside `:app:detekt`, so code that was gated on `develop` is no longer gated, and the developer's Checks section asserts the reverse.
- No suppression, baseline, `@Ignore`, disabled test, `!!`, dependency, Room schema change, resource change, or logging violation. 125 scoped tests pass, 0 skipped.
- `ResultsViewModel` at exactly detekt's 11-function ceiling: acceptable on this branch, worth scheduling — reasoning in F-4.
- D-07a is a recording, not a fix: the codec is byte-for-byte untouched and all three constraints describe the code accurately.

## Findings

### F-1 — MEDIUM (blocking): detekt no longer analyses the files this diff moved
`app/src/debug/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPicker.kt:1` (whole file), `app/src/debug/java/com/app/pegsolitaire/platform/games/save/DebugPickerConstants.kt:1`, `app/src/debug/java/com/app/pegsolitaire/di/DebugSnapshotPickerModule.kt:1`, `app/src/release/java/com/app/pegsolitaire/di/ReleaseSnapshotPickerModule.kt:1`, `app/src/testDebug/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPickerTest.kt:1`.

Evidence (paired probe, both files created and deleted inside one command; tree verified clean after):

```
app/src/main/java/com/app/pegsolitaire/di/ZZProbeMain.kt:3:35 This expression contains a
  magic number... [MagicNumber]      → > Task :app:detekt FAILED
app/src/debug/java/com/app/pegsolitaire/di/ZZProbe.kt  (identical body)
                                     → BUILD SUCCESSFUL, 0 findings
```

Task-by-task, with the probe in `src/debug`:

| task | result | probe flagged |
|---|---|---|
| `:app:detektDebugSourceSet` | `NO-SOURCE` | 0 |
| `:app:detektReleaseSourceSet` | task absent | 0 |
| `:app:detektMain` / `:app:detektRelease` | task absent | 0 |
| `:app:detektDebug` | `FAILED` | 2 |

and `./gradlew :app:check --dry-run` lists **only** `:app:detekt` for detekt (plus the full ktlint set, which *does* include `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck`, `ktlintTestDebugSourceSetCheck`). `detektDebug` — the one task that sees the code — is not wired into `check`.

On `develop`, `git ls-tree` shows `PlayGamesSnapshotPicker.kt` under `app/src/main/…` and its test under `app/src/test/…`, both inside `:app:detekt`'s default source. So this is a regression in gate coverage caused by this diff, not a pre-existing hole.

This is functionally a blanket, undeclared detekt exclusion over ~360 lines. CLAUDE.md lists detekt as a mandatory build gate; rules/kotlin-android.md forbids suppressions "without code-auditor approval". **REJECTED** — I am not approving it, and I cannot: the PR text does not ask for it, it claims the opposite.

Remedy (one line, low risk): in `build-logic/src/main/kotlin/standards.gradle.kts`, either add `src/debug/java`, `src/release/java`, `src/testDebug/java` to the `detekt` task's `source`, or `tasks.named("check") { dependsOn("detektDebug", "detektRelease") }`. Either way, re-verify with the probe above before re-review.

### F-2 — LOW: the report's Checks section states the coverage that F-1 disproves
`.planning/reports/phase-14/android-game-dev-prerelease-fixes.md:122-124`:

> "It covers the new `debug`, `release` and `testDebug` source sets: `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck`, `ktlintTestDebugSourceSetCheck`, `detektDebugSourceSet`, `detektReleaseSourceSet` all exist and are wired into the aggregates."

The ktlint half is true. The detekt half is not: `detektDebugSourceSet` is `NO-SOURCE`, `detektReleaseSourceSet` does not exist, and existence is not wiring. A reader trusting this line will believe the gate covers `src/debug`. Correct the line whatever is decided about F-1. The same report at line 186 honestly flags the narrower `MagicNumber`/`**/testDebug/**` exclude; F-1 is the same root cause one level up and supersedes it.

### F-3 — LOW: the "never re-serves the level just played" invariant has an unnamed exception at the ceiling
`app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:332`

```kotlin
val nextLevel = ChallengeLevelBounds.clamp(maxOf(ladderHead, solvedLevel + 1))
```

`clamp` is `coerceIn(MIN, MAX)` (`domain/…/ChallengeLevelBounds.kt:56`), so at `solvedLevel == ChallengeLevelBounds.MAX` this returns `MAX` — "Next level" re-serves the board just solved, which is exactly the one-board loop the `max` exists to make unreachable by construction. `ResultsViewModel.kt:301-308` states that property without the exception. `ChallengeNextLevelTest.kt:203-219` pins the behaviour as expected and its KDoc frames it purely as a bounds question ("the action cannot point past MAX"), so the tension is enshrined rather than named.

Answer to the brief's specific question: **the clamp is correct as a bound and is not a regression** — `develop` reached the same place via `GameViewModel`'s WARN-03 clamp of the route arg — and it is unreachable in practice (`GeneratorConstants.kt:42`, `MAX_CHALLENGE_LEVEL = 10_000`, documented "well above any realistically hand-reached level"). But one sentence in the `onNextLevelTapped` KDoc and in the ceiling test should say the invariant holds for `level < MAX`, so the next reader does not take the stronger claim at face value.

### F-4 — LOW (the brief's question): `ResultsViewModel` at exactly 11 functions
`app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:130-613` — 11 functions in the class body (`onNextLevelTapped`, `onCatalogTapped`, `onNavigationHandled`, `claimContinue`, `dispatchContinue`, `shouldShowInterstitial`, `presentThenNavigate`, `claimWinOnce`, `recordSolvedPlayOnce`, `claimCelebrationPlayOnce`, `scoreResultFromArgs`), against detekt's default `TooManyFunctions` threshold of 11.

**It is a real smell, and acceptable on this branch.** The class carries four separable jobs: route-arg clamping and state construction; the record-once bests write plus the Play Games submission; the interstitial cadence decision and presentation; and the continue-tap claim/dispatch/destination. The natural extraction is the interstitial cluster — `claimWinOnce`, `shouldShowInterstitial`, `presentThenNavigate` plus `interstitialEligible`/`cadenceForThisResult` — which has its own collaborators (`cadenceStore`, `adGateway`), its own decision type (`InterstitialCadenceState`), and would drop the class to 8 and the constructor to 4.

Do not do it here. That code is the D-09/D-11/INT-02 ordering that four phases of tests pin, and re-cutting it days before a release branch is the wrong risk. Schedule it: the ceiling means the *next* change to this screen must either extract or suppress, and at that moment a `@Suppress` is by far the likelier outcome. The developer's own line 193-196 reaches the same conclusion; I am confirming it, not overriding it.

### F-5 — INFO: `runCatching` swallows `Error`, not just `Exception`
`app/src/main/java/com/app/pegsolitaire/platform/games/save/DebugSnapshotPickerEntry.kt:59`

```kotlin
scope.launch { runCatching { show() }.onFailure { if (it is CancellationException) throw it } }
```

`runCatching` catches `Throwable`, so `OutOfMemoryError`/`StackOverflowError` inside the debug picker are silently dropped. **No action.** L-2's requirement is literally "nothing may reach a scope with no handler", the path is debug-only, and rethrowing only `CancellationException` is the correct narrowing — a child's `CancellationException` does not cancel the parent job, so cooperative cancellation still works and nothing else escapes. The L-2 requirement is satisfied: `aFailureInsideTheShowNeverCancelsTheCallersScope` (`DebugSnapshotPickerEntryTest.kt:103-124`) asserts `activityScope.isActive` and that a later `launch` still runs. A late-arriving intent cannot crash either: `onNewIntent` → `onIntent(intent, lifecycleScope)` on a cancelled scope creates an already-cancelled coroutine whose body never runs.

### F-6 — INFO: the Challenge continue tap now always crosses a suspension
`app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:330-334` — `shouldShowInterstitial()`'s reads of `adGateway.interstitialReady.value` and `System.currentTimeMillis()` now happen *after* the ladder read rather than on the tap frame. Harmless today, and the claim is correctly taken synchronously before the launch (`claimContinue()` at line 329, documented at 353-361), so two taps in one frame cannot both pass. The reason the latency is invisible is `ChallengeProgress.currentLevel`'s `stateIn(scope, Eagerly, …)` at `ChallengeProgress.kt:64-74`: the shared `app_prefs` store is already initialised at app start, so `dataStore.data.first()` in `settledLevel()` (`ChallengeProgress.kt:127-131`) resolves from memory with no disk IO. That is now load-bearing for tap responsiveness and is not written down anywhere — worth a line if anyone makes `currentLevel` lazy.

### F-7 — INFO: `THIRD_PARTY.md` is absent
Zero dependencies were added or changed (`git diff develop...HEAD -- gradle/libs.versions.toml */build.gradle.kts settings.gradle.kts` is empty), so the "a row for every new dependency" rule is satisfied vacuously and this is not a finding against the diff. Flag for the release-manager before Gate 2.

## Verification of the four requirements

**1. D-04a "Next level".** Behaviour matches `.planning/reports/phase-13/game-designer-next-level-ruling.md` §B exactly: `ResultsViewModel.kt:327-335` serves `clamp(maxOf(settledLevel(), solvedLevel + 1))`, with `solvedLevel` the WARN-03-clamped `challengeLevel` (line 149). The arithmetic is out of `ResultsActions` (`onNextLevel` is now `() -> Unit`, `ResultsActions.kt:49,61`) and the resolved level reaches navigation through `pendingNavigation` (`ResultsRoute.kt:82-85`), so `AppNavHost` still receives an `Int` and nothing was widened.

- *Genuinely awaitable-and-settled, no cached path left.* `settledLevel()` is `dataStore.data.first()[CURRENT_LEVEL_KEY]` inside `storageFailSafe` (`ChallengeProgress.kt:127-131`) — a read of committed store state, not the `stateIn` cache. `grep` over `app/src` finds no `currentLevel` read anywhere in `ui/results`.
- *Can the claim or record-result race the read?* No. `claimContinue()` is synchronous on the tap, ahead of the suspension, and its KDoc says why. `recordSolvedPlayOnce()` writes Room only and never touches the ladder. The ladder write is `GameViewModel.kt:973`'s fire-and-forget `advancePast(N)` on the application scope, so the read is **not** in fact *ordered* after it — the `ResultsViewModel.kt:310-316` KDoc slightly overstates that — but the `max` makes every ordering outcome correct: a pre-commit read returns `N` (or the merged head) and `max(head, N+1) ≥ N+1` regardless. A concurrent cloud `mergeLadder` can only raise the head, which is the intended D-04a serve.
- *Mutation testing, run by me on a temporary edit, restored and `git status` verified clean:*
  - `settledLevel()` → `currentLevel.value`: **3 failed** — `nextLevelIsLadderHeadAfterSolvingBehindTheHead`, `nextLevelReadsTheSettledLadderRatherThanTheCachedValue`, `nextLevelNeverServesACompletedLevelWhenTheHeadIsAhead`. Matches the developer's claim exactly.
  - drop the `max` (`clamp(ladderHead)`): **`nextLevelNeverServesTheLevelJustPlayed` FAILED**, i.e. exactly 1 inside `ChallengeNextLevelTest` as reported; my wider scope also caught `ResultsViewModelTest.aStuckChallengeFinishIsNotAdEligibleAtAnyStarCount` and `aSecondContinueTapWhileTheAdIsInFlightIsANoOp`, so 3 in total. The reported count is consistent, not wrong.
  - The test harness earns its complexity: `ChallengeNextLevelTest.ladderAt` (lines 259-263) runs `ChallengeProgress` on a dispatcher the test never advances, which freezes the `stateIn` cache at the fail-open default while the committed value differs. That is what makes the settled-vs-cached distinction observable at all, and it is why mutation 3 is detectable.
- Retry of a behind-head level is covered by the same code path (ruling §E.1), and `nextLevelNeverServesACompletedLevelWhenTheHeadIsAhead` asserts it across the whole gap rather than at one point. D-03 is untouched: `git diff` shows zero changes under `app/src/*/res`, so `action_next_level` is still unnumbered and nothing is said to the player.

**2. Debug picker out of release + L-2.** Confirmed independently — see the lead. Method: deleted the AGP 9 built-in-Kotlin output roots, recompiled both variants with `--rerun-tasks`, enumerated `.class` files and binary-grepped them, then assembled the real R8-minified release APK and searched `classes.dex`. Release: `ReleaseSnapshotPickerModule`, `DebugSnapshotPickerEntry` and its `onIntent$1` only. Debug: those plus `PlayGamesSnapshotPicker` (+4 synthetic lambdas), `DebugPickerConstants`, `DebugSnapshotPickerModule`. Title string and `getSelectSnapshotIntent` present in debug, absent in release. The split is structurally sound: Hilt resolves one binding per type and the debug module names a class release cannot see, so both variants compiled green and neither module is deletable. Nothing in `src/main` or `src/release` references the picker except KDoc prose. The picker stayed inside `platform/…`, so the `^import com.google.android.gms` boundary rule still holds, and `PlatformBoundaryTest` (3 tests) plus `PlatformBoundaryGateTest` (3 tests) still cover it, because the `test` source set compiles against the debug variant. L-2: verified under F-5.

*Caveat worth recording:* `app/build.gradle.kts:110` refuses any release-named invocation without real AdMob IDs, which this machine's `local.properties` does not carry (27 PGS ids only). I supplied three placeholder values as environment variables for the compile and assemble, wrote nothing to the repo, and deleted the resulting unsigned APK afterwards. Same approach the developer used and it is the only way to run this check on this machine.

**3. FU-2 documentation corrections — accurate, not merely present.** Each correction states what the 22.0.0 javadoc says and separates it from inference, and each is hedged in the right direction: `PlayGamesCloudSaveStore.kt:46-58` splits the documented `commitAndClose` sentence from the undocumented "next open fails for the process" claim and keeps the practice on the ground that it is right either way; `13-RESEARCH.md` Pitfall 1 and the threat-table row match; `CloudPull.kt:39-51` records the `change_pending` negative with the concrete reason the measurement was misread (each launch's push guard produced a fresher commit) and names the usable signal as the conjunction with `lastModifiedAt`; `13-RESEARCH.md` A1 correctly explains that a zero-warning clean-device session is the *predicted* outcome of `COVER_PX = 384` and therefore silent on A1 in either direction, and that A1 rests on `512 × 512 × 4 = 1,048,576` exactly rather than on version-dependent inference. `ProgressSubmitter.kt:540-549` records the uncapped-comparison note with the arithmetic (2,883,584 B target vs ~400 KB full ladder) and the honest degradation. Two test files had their comments corrected in step so they no longer assert the folklore. No overclaim found.

**4. D-07a recorded, not fixed.** `git diff develop...HEAD --stat` contains no file under `app/src/main/java/com/app/pegsolitaire/data/cloud/` — the codec is untouched; the only change in `data/` is `ProgressSubmitter.kt`'s KDoc. All three constraints check out against the code:
- **C-1**: `CloudProgressCodec.kt:165-168` returns `null` for `schemaVersion > SCHEMA_VERSION`; `mergeBytes` (187-193) routes both bytes through `decodeOrEmpty` (196), which maps corrupt and newer-but-decodable to the same `CloudProgress.EMPTY`. Accurate, including "refuses to read, then deletes".
- **C-2**: no row-count cap anywhere in `encodeWithinCap`/`toCloudProgress`; `isWellFormed` (289) requires only `levelId.isNotBlank()` for Classic, with the Challenge id pinned by `hasLegalChallengeLevelId` (302). Accurate, and the "do NOT validate Classic ids against the shipped set" caveat is the right call — it would break the forward compatibility D-07 mandates.
- **C-3**: `dropCount` (141-145) is `maxOf(1, challengeRows * (encodedSize - maxBytes) / encodedSize + 1)`, all `Int`. 10,000 rows × an overshoot above ~214,748 B wraps; the `maxOf(1, …)` floor still terminates the loop at one row per pass. Accurate, including "~210 KB" and "only reachable together with C-2".

**5. Standards.** No new dependency, no Room schema change, no `@Suppress`/baseline/`@Ignore`/disabled test/`!!` in added lines, no detekt baseline file in the repo at all, no logging in `platform/games`, `data/`, `data/cloud` or the new `src/debug`, no `^import com.google.android.gms` outside `com/app/pegsolitaire/platform/` (the one hit is `app/src/test/…/archfixture/GmsBoundaryViolationFixture.kt:3`, the deliberate negative fixture for the ArchUnit gate, unchanged and pre-existing), no hardcoded `dp`/`sp` and no resource change at all, scoped constants preserved (`DebugPickerConstants` is a new per-concern object, and `CloudSaveConstants` documents what moved and why `DEBUG_PICKER_EXTRA` stayed). No secret-shaped string added. Gitflow: 7 linear conventional commits on a `feature/*` branch off `develop`, no merges, subjects within budget. Compose: the change strictly improves the rendering boundary — the leaf lost its arithmetic and now reports a tap; no new allocation, no new state, no stability change (the bound callable-reference pattern already existed for `onCatalogTapped`).

## Checks
- `./gradlew detekt ktlintCheck --max-workers=2 --rerun-tasks` — **pass** (`BUILD SUCCESSFUL in 7s`, `31 actionable tasks: 31 executed` — forced, so not a cache artefact; a first run reported all 31 up-to-date, which is why I re-ran).
- `./gradlew :app:testDebugUnitTest --max-workers=2 --tests "*ChallengeNextLevel*" --tests "*Results*" --tests "*SnapshotPicker*" --tests "*ChallengeProgress*" --tests "*PlatformBoundary*"` — **pass** (`BUILD SUCCESSFUL in 10s`; **125 tests, 0 failures, 0 skipped** over 16 classes, counted from the JUnit XML, including `ChallengeNextLevelTest` 7, `ResultsViewModelTest` 54, `DebugSnapshotPickerEntryTest` 7, `PlayGamesSnapshotPickerTest` 5).
- `rm -rf app/build/intermediates/built_in_kotlinc/{debug,release}` then `ADMOB_*=<placeholder> ./gradlew :app:compileReleaseKotlin :app:compileDebugKotlin --max-workers=2 --rerun-tasks` — **pass** (`BUILD SUCCESSFUL`; both `kspReleaseKotlin`/`kspDebugKotlin` and both compiles executed). Release 571 classes, debug 577.
- `find <release classes> -name '*SnapshotPicker*' -o -name '*DebugPicker*'` — `ReleaseSnapshotPickerModule`, its lambda, `DebugSnapshotPickerEntry`, its `onIntent$1`. **No picker, no constants object, no debug module.**
- `grep -ralF 'Saved games (debug)' <release classes>` — **0 hits**; same grep over debug — 2 hits (`DebugPickerConstants.class`, `PlayGamesSnapshotPicker.class`). `grep -ralF 'getSelectSnapshotIntent'` — release **0**, debug 1.
- `ADMOB_*=<placeholder> ./gradlew :app:assembleRelease --max-workers=2` — **pass** (`BUILD SUCCESSFUL in 1m 6s`, 3,743,881 B unsigned APK, R8 full mode + resource shrinker). `strings -a classes.dex | grep -i 'snapshotpicker|DebugPicker|Saved games'` — one hit, `debugSnapshotPickerEntry` (the Hilt-injected field name). Artefact deleted afterwards.
- Paired detekt-coverage probe (F-1) — **fail**: identical `MagicNumber` violation caught in `src/main`, missed in `src/debug`; `:app:detektDebugSourceSet` `NO-SOURCE`; `:app:detektReleaseSourceSet`/`detektMain`/`detektRelease` absent; `:app:detektDebug` catches it but `:app:check --dry-run` does not list it. Probe files removed; `git status` clean.
- Mutation A, `settledLevel()` → `currentLevel.value` — **3 failed** as reported. Mutation B, drop the `max` — **`nextLevelNeverServesTheLevelJustPlayed` failed** (1 in `ChallengeNextLevelTest`, 3 with `ResultsViewModelTest` in scope). Both reverted; `git diff HEAD` empty.
- `git diff develop...HEAD -- app domain | grep -E '^\+.*(@Suppress|@Ignore|Disabled|!!)'` — **0**. `find . -name '*baseline*.xml' -not -path '*/build/*'` — **none**.
- `grep -rnE 'android\.util\.Log|Log\.[dviwe]\(|println\(|System\.out|Timber'` over `platform/games`, `data/`, `src/debug` — **0**.
- `grep -rn '^import com.google.android.gms' app/src domain/src | grep -v 'com/app/pegsolitaire/platform/'` — 1 hit, the deliberate ArchUnit negative fixture (unchanged).
- `git diff develop...HEAD -- gradle/libs.versions.toml '*/build.gradle.kts' settings.gradle.kts` and `-- 'app/src/*/res' '*schemas*' '*Dao*' '*Entity*'` — **empty** (no dependency, resource or Room change).
- Secret scan over the diff (`ca-app-pub-`, PEM blocks, `AIza…`, keystore/password keys, bearer tokens) — **0 hits**.
- **Not run, by the task's constraints:** the full `./gradlew check` (the producer owns it), `:app:lint`, and anything touching adb or an emulator.

## Open issues / risks
- F-1 is the only gate to re-verify on re-review; the probe in this report is the check to re-run. Everything else in the diff is accepted as written.
- The D-04a user-visible half is still unexercised on a device: `11-HUMAN-UAT.md` test 11's D-04 sub-check at a gap of ≥ 2, plus one debug-build `am start` to confirm the extra still opens the picker from the Activity's scope. QA's, and the report already names both.
- I could not verify the SDK javadoc quotations against the artifact itself (no javadoc jar resolved locally). The FU-2 corrections are internally consistent and hedged in the safe direction, and each one *weakens* a previous claim rather than adding a new one, so a residual error there cannot mislead anyone into a stronger belief than the code warrants. Recorded rather than treated as verified.
- `app/src/release` exists to declare one binding and is easily mistaken for dead weight. Its KDoc says so explicitly; nothing to fix, but a second variant-specific binding belongs in those same two files.
- C-1 (D-07a) should gate any future `SCHEMA_VERSION` bump. I agree with the developer's ranking: it is a silent data loss on the device that is behind during a staged rollout, and today both the right and the wrong behaviour pass.

## Handoff
Next: **android-game-dev** (FAIL) — fix F-1 (one line in `build-logic/src/main/kotlin/standards.gradle.kts` bringing `src/debug`, `src/release`, `src/testDebug` under detekt, or `check.dependsOn(detektDebug, detektRelease)`), re-run detekt and fix whatever it now reports in those five files, and correct the Checks line at `.planning/reports/phase-14/android-game-dev-prerelease-fixes.md:122-124`. F-3's one-sentence KDoc/test note is cheap and worth doing in the same pass. F-4 goes to the **tech-lead** as a scheduled refactor, not a change on this branch. Nothing in requirements 1–4 needs rework.

`VERDICT: FAIL`


---

# code-auditor re-review — phase 14 pre-release fixes

Date: 2026-09-21
Branch: feature/phase-14-prerelease-fixes (88c7e2b..d1fc37f) — no branch switch, no commit, no source edit
Skills loaded: `ergoz-soft:standards` (role code-auditor) → plugin copies of `studio.md`, `reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `kotlin-android.md`, `testing.md` (plugin v0.5.2 > repo `.claude/rules/VERSION` 0.5.1, so the plugin copies were read); repo `.claude/rules/lessons.md`. **Not loaded:** `code-reviewer`, `security-reviewer`, `gsd-code-review`, `compose-skill` — the loop diff is 34 lines of Gradle config plus 18 lines of comment; the correctness/security/performance passes those skills cover were done by hand against the rule files and are recorded below. Recorded rather than left implicit.
Ran in parallel with: none
Loops: 1
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` absent)

> This addendum belongs at the end of `/Users/ermanergoz/Desktop/PegSolitarie/.planning/reports/phase-14/code-auditor-prerelease-fixes.md` as a `## Re-review (loop 1)` section. My harness forbids me writing report files, so the producer saves it from this message, as it did the first pass. Leave it uncommitted.

## Re-review (loop 1)

**Verdict: PASS.** F-1 is closed, and closed correctly rather than conveniently. F-2 and F-3 are corrected. Two new items are ruled on below; neither blocks this branch, both are phase-14 work before the release branch is cut.

### F-1 — verified fixed, by my own probe and by a negative control

The fix is one line at `build-logic/src/main/kotlin/standards.gradle.kts:64`, plus a 31-line comment block:

```kotlin
tasks.named<Detekt>("detekt").configure { source(variantSourceRoots) }
```

**My paired probe.** Four files, one per source set, each `internal fun zzAuditProbe<X>(): Int = 37 * 3`. Command: `./gradlew :app:detekt --rerun-tasks --max-workers=2` — exactly the task `check` invokes, confirmed independently below.

| probe file | with the fix (HEAD) | with line 64 disabled |
|---|---|---|
| `app/src/main/java/com/app/pegsolitaire/di/ZzAuditProbeMain.kt:3` | **3 findings, `:app:detekt` FAILED** | 3 findings, FAILED |
| `app/src/debug/java/com/app/pegsolitaire/di/ZzAuditProbeDebug.kt:3` | **3 findings, FAILED** | **0 — missed** |
| `app/src/release/java/com/app/pegsolitaire/di/ZzAuditProbeRelease.kt:3` | **3 findings, FAILED** | **0 — missed** |
| `app/src/testDebug/java/com/app/pegsolitaire/platform/games/save/ZzAuditProbeTestDebug.kt:3` | **4 findings, FAILED** | **0 — missed** |

Quoted from the run at HEAD:

```
e: …/app/src/debug/java/com/app/pegsolitaire/di/ZzAuditProbeDebug.kt:3:41 This expression
   contains a magic number… [MagicNumber]
e: …/app/src/release/java/com/app/pegsolitaire/di/ZzAuditProbeRelease.kt:3:43 … [MagicNumber]
e: …/app/src/testDebug/java/…/ZzAuditProbeTestDebug.kt:3:45 … [MagicNumber]
> Task :app:detekt FAILED
```

The **negative control** is the half the developer did not run and the half that matters: I reverted only line 64 (via a scratchpad copy, restored immediately) and re-ran with the same four probes. `src/debug`, `src/release`, `src/testDebug` all fell to **0** while `src/main` stayed at **3**. So (a) the coverage is caused by this line and not by an AGP or detekt behaviour change, and (b) `source(…)` *appends* — `src/main` and `src/test` were not displaced, which is the failure mode that would have silently traded one hole for a bigger one. `git status --porcelain` empty and `git diff --stat` empty afterwards; probe files and the config edit gone.

**Independent, non-failing confirmation** — the `detekt` task's resolved source, printed by an init script kept in the session scratchpad (nothing written to the repo):

```
CENSUS :app total=222   main=128  test=89  debug=3  release=1  testDebug=1
CENSUS   FILE app/src/debug/java/com/app/pegsolitaire/di/DebugSnapshotPickerModule.kt
CENSUS   FILE app/src/debug/java/com/app/pegsolitaire/platform/games/save/DebugPickerConstants.kt
CENSUS   FILE app/src/debug/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPicker.kt
CENSUS   FILE app/src/release/java/com/app/pegsolitaire/di/ReleaseSnapshotPickerModule.kt
CENSUS   FILE app/src/testDebug/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPickerTest.kt
CENSUS :domain total=75  main=39  test=36   (0 in all three variant roots)
```

Those are the five files F-1 named, all now inputs to the gate, and `:domain` is untouched. The developer's numbers reproduce exactly.

**`check` still invokes exactly one detekt task.** `./gradlew :app:check --dry-run` → `:app:detekt SKIPPED` is the only `detekt*` line (the full ktlint set is there, `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck` and `ktlintTestDebugSourceSetCheck` among them). Unchanged by the fix, which is the design: the one task `check` already ran now sees everything. That is what makes widening the task the correct lever rather than a workaround.

### Is the chosen approach right, or merely convenient? Right — I checked each reason

1. **`:domain` has no variant tasks — TRUE.** `./gradlew :domain:tasks --all | grep -i detekt` lists `detekt`, `detektMain`, `detektTest`, `detektMainSourceSet`, `detektTestSourceSet` and the baseline forms; **no `detektDebug`, no `detektRelease`**. `standards.gradle.kts` is applied by `domain/build.gradle.kts:4` as well as `:app`, so `check.dependsOn("detektDebug", "detektRelease")` in it would fail task resolution on `:domain` unless made variant-aware — which needs the AGP types the plugin deliberately avoids and whose absence its own dp/sp gate comment (`standards.gradle.kts:66-68`) already cites as precedent. This is the decisive reason and it holds.
2. **`detektRelease`'s name trips the ad-ID guard — TRUE, but weaker than stated.** `./gradlew :app:detektRelease --dry-run` (no env vars) dies at configuration: `Release build requires real ad/Play Games IDs.` at `app/build.gradle.kts:110`, because `isReleaseInvocation` (`app/build.gradle.kts:88-91`) matches `contains("elease")`. `detektReleaseSourceSet` trips it too — I hit that mid-run. **The nuance the developer missed:** the guard reads `gradle.startParameter.taskNames`, i.e. the *requested* names, so `check.dependsOn(detektRelease)` would **not** have tripped it — `./gradlew check` requests `check`. So this reason argues only that the wired task could not be invoked or reproduced directly, not that the wiring would break. Still a real objection (a gate nobody can run by hand is a trap), just not load-bearing.
3. **`detektDebugSourceSet` is NO-SOURCE — TRUE.** With probes present in `src/debug` and `src/testDebug`, `:app:detektDebugSourceSet` and `:app:detektDebugUnitTestSourceSet` both report `NO-SOURCE` and flag 0. The per-source-set tasks are a dead end. *Correction to my own first pass:* my F-1 table listed `detektReleaseSourceSet`, `detektMain` and `detektRelease` as "task absent". They exist — `:app:tasks` hides variant tasks unless `--all` is passed. The developer caught this and is right. The conclusion is unaffected: existing-and-empty is exactly the hole F-1 described.
4. **`detektDebug` re-analyses `src/main` — TRUE, and costlier than stated.** With probes in both `src/main` and `src/debug`, `:app:detektDebug` flagged **3 + 3** — it covers both, so `detektMain` + `detektDebug` + `detektRelease` would triple-report every main-source finding. It also took **44 s** against **2 s** for the parse-only `detekt`, confirming the type-resolution/compile-classpath cost claim with a number the report did not have.

**Judgement.** Four reasons, four verified (one weaker than written). The approach keeps one task, one config, one report and one name in `check`, needs no AGP types in a plugin shared with a pure-JVM module, and costs 2 s instead of 44 s. It is the right call, not the convenient one. The one thing genuinely better is a *derived* list — wiring the variant roots from `android.sourceSets` inside `app/build.gradle.kts`, where AGP types already exist, so a new flavour cannot silently reopen the hole. That costs locality (the gate would live in two files) and buys nothing today: there are no flavours and one Android module. Recorded as F-9 below with the trigger that should make someone do it.

### Zero findings in the five moved files, and nothing dodged

`./gradlew detekt ktlintCheck --rerun-tasks --max-workers=2` → `BUILD SUCCESSFUL in 6s`, `31 actionable tasks: 31 executed`. Combined with the census, that is a genuine zero and not a vacuous one: the files are proven inputs and a planted violation in each of their source sets proven to fail. `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck` and `ktlintTestDebugSourceSetCheck` all **executed** (not `NO-SOURCE`, not `SKIPPED`) — the ktlint half of the original claim was always true.

Nothing was suppressed, baselined or dodged. `git diff 88c7e2b..HEAD --name-only` is four files: the developer's own report, `ResultsViewModel.kt`, `ChallengeNextLevelTest.kt`, `standards.gradle.kts`. **`config/detekt/detekt.yml` is not among them** — no exclude was widened, no source set narrowed, no `excludes` entry added. No baseline file exists anywhere in the repo. Every `@Suppress` / `baseline` / `excludes` string in the loop diff is prose inside the report.

**On `MagicNumber`'s `**/test/**` exclude not matching `**/testDebug/**`: leaving it as-is is right, and the developer's reasoning is correct.** My `src/testDebug` probe returned **4** findings including two `MagicNumber` hits, so the rule is live there and `PlayGamesSnapshotPickerTest.kt` passing is a real pass (its only literals are `verify(exactly = 0/1)` and `0`/`1` are both in `ignoreNumbers`). Widening the exclude in the same commit that closed F-1 would have been dodging the finding it was fixing — the developer says exactly that and it is the correct instinct. Two further reasons to leave it: a stricter rule on one test file is not a defect, and the source set holds one file whose entire content this audit has read. If `src/testDebug` ever grows, the right fix is `'**/testDebug/**'` added to the `MagicNumber` excludes, which is a tech-lead config decision, not a developer's.

### F-2 and F-3 — corrected, not merely edited

**F-2 is corrected properly.** `.planning/reports/phase-14/android-game-dev-prerelease-fixes.md:120-137` quotes the false sentence verbatim, marks it CORRECTED, and separates the two errors — the false detekt half, and the method error of inferring coverage from a task list instead of proving it. It then states what is true after the fix and cites the probe rather than task names. It keeps the narrower `**/testDebug/**` note as a separate still-open item (line 201) instead of folding it away. The `Loops:` header went `0` → `1` (`Loops: 1  (loop 1 closes the code-auditor's F-1, F-2 and F-3 …)`), which rules/reporting.md requires. This is the correction I asked for and slightly better: it preserves the wrong claim so the error is auditable.

**F-3 is corrected with zero behaviour change, proven not asserted.** `git diff 88c7e2b..HEAD -- app/src domain/` filtered to added non-comment, non-blank lines returns **nothing** — every added line in `app/` and `domain/` begins with `*`, `//` or `/*`. `clamp` is still `coerceIn(MIN, MAX)`; no test expectation moved. Both sentences say the right thing: `ResultsViewModel.kt:321-329` states the guarantee holds for `N < MAX` only, that re-serving the ceiling is correct for a *bound*, that `MAX` is `GeneratorConstants.MAX_CHALLENGE_LEVEL` = 10,000 and so unreachable, and — the part that makes it useful — that anyone lowering the ceiling to a reachable number must revisit it. `ChallengeNextLevelTest.kt:208-215` names `nextLevelNeverServesTheLevelJustPlayed` explicitly so the two tests are not read as contradicting. Closed.

**F-4 untouched, as instructed.** `ResultsViewModel` is still at 11 functions; only KDoc was added. Still the tech-lead's scheduled extraction.

## New findings

### F-8 — MEDIUM (not blocking this branch): three literal gates do not read `app/src/release`, which ships

`build-logic/src/main/kotlin/standards.gradle.kts:121-122` — `adIdSourceRoots` is `src/main/{java,kotlin,res}`, shared by `adIdLiteralCheck` (:148), `gamesIdLiteralCheck` (:188) and `audioFocusLiteralCheck` (:230).

**Ruling: a phase-14 task before the release branch is cut. Not a blocker for this branch.** Reasoning, and the developer's framing needs one correction:

- **The `src/debug` half is cosmetic, not substantive.** Those gates exist to stop a real ad unit id or Play Games id reaching a *shipped* build. AGP build-type source sets are variant-exclusive by construction, and the first pass of this audit proved it empirically for these exact files (release class output and the R8-minified `classes.dex` contained no `src/debug` class, no picker, no constants object, no title string). So an ID pasted into `src/debug` **cannot** reach a release artifact. The developer's "`src/debug` is where QA-only code lands, which is exactly where a real ID gets pasted" is a real hygiene concern and worth closing, but it does not breach the gates' purpose.
- **The `src/release` half does breach it, and the developer under-stated this.** `app/src/release/java/…/ReleaseSnapshotPickerModule.kt` **is** compiled into the shipped variant, and none of the three gates reads it. That is a shipped source root outside a gate whose own KDoc says it is "what makes that a rule rather than a convention" (`standards.gradle.kts:115-120`). For the duration of a release, the only thing standing between a real ad unit id in `src/release` and production is code review — in precisely the place the studio already decided review is insufficient. `verifyReleaseAdIds` / `verifyReleaseGamesIds` (`app/build.gradle.kts:157,172`) are no compensating control: they assert the IDs *are configured*, never that they are absent from source.
- **Why it is not a blocker now.** Factual state, scanned by me across all three new source sets: `ca-app-pub-` **0 hits**, `CgkI` **0 hits**, `requestAudioFocus` **0 hits**, `abandonAudioFocusRequest` **0 hits**. `src/release` is one declaration-only file, read in full in this audit. And widening a gate's scope is a standards decision, not a developer's — doing it unreviewed in the same loop that closed F-1 is the scope creep the developer correctly declined (rules/studio.md standing rule 4).
- **Why it is not a later follow-up either.** The asymmetry decides it: the cost of acting is one line (add `src/release/{java,kotlin,res}` — and `src/debug/*` for defence in depth — to `adIdSourceRoots`) against a pure file-scan task with no build-graph effect, verifiable by the same paired-probe method in minutes. The cost of deferring is a real ad unit id in production. A hole you have written down and chosen not to close is worse than one you never noticed.

Owner: **tech-lead** decides the scope (it is a gate change), **android-game-dev** implements, verified by a probe per source root. Before the release branch.

### F-9 — INFO: `variantSourceRoots` is a hand-maintained list, so a new build type or flavour reopens F-1 silently

`build-logic/src/main/kotlin/standards.gradle.kts:58-62`. The list is `listOf("debug", "release", "testDebug")`, filtered by `File.exists()` at configuration time. **No action, and no risk today.** Configuration cache is off (`gradle.properties:8-10`, deliberately, for detekt 2.0 alpha), so the `exists()` filter is re-evaluated every build and cannot go stale; and the KDoc at :56-57 tells the next reader to extend the list. The failure mode is a future `staging` build type or a product flavour arriving with no one reading that comment — the same silent hole, by the same mechanism, one source set over. The durable form is to derive the roots from `android.sourceSets` inside `app/build.gradle.kts`, where AGP types already exist, leaving this plugin AGP-free. Do it the day a flavour or a third build type appears, not before.

### F-10 — MEDIUM (not blocking this branch): the DataStore test flake is a rules/testing.md violation across ten test classes, and the stated diagnosis is incomplete

`app/src/test/java/com/app/pegsolitaire/data/InterstitialCadenceStoreTest.kt:232-236`:

```kotlin
private fun realDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        produceFile = { tempFolder.newFile(TEST_STORE_FILE) },
    )
```

**Is the diagnosis right? Partly — the right enabling condition, the wrong proximate cause.**

- **Correct:** no `scope` argument means DataStore uses its default `CoroutineScope(Dispatchers.IO + SupervisorJob())`, so real file IO runs outside `runTest`'s virtual clock while the test body's `store.state.first { … }` waits against `runTest`'s **real** 60 s timeout (coroutines 1.11.0 default). The reported `time="60.041"` and `UncompletedCoroutinesError: After waiting for 1m` match that bound exactly. The test is wall-clock-dependent, which rules/testing.md forbids outright: *"Deterministic tests only: no wall-clock, no network, no unseeded random, no sleeps."*
- **Incomplete, and this matters:** the developer attributes the failure to contention under `--max-workers=4`. On my run the whole `InterstitialCadenceStoreTest` suite took **0.029 s** and the failing test **0.005 s**. 0.005 s → 60.041 s is four orders of magnitude. Contention does not do that; it turns 5 ms into 50 ms. That timing is the signature of an ordering-dependent block, not of load. The plausible mechanism, from the code: `InterstitialCadenceStore.kt:113-123` uses `stateIn(scope, SharingStarted.Eagerly, …)` with `scope = backgroundScope`, so `dataStore.data` is collected on the **single-threaded** test dispatcher, while DataStore's own actor and coordinator mutex live on `Dispatchers.IO`. This test is the one that constructs a **second** holder over the **same** `DataStore` (`:104-118`), i.e. two eager collectors on that one thread plus a write actor off it. A lost wakeup or a thread held across that boundary hangs until the timeout. I did **not** reproduce the failure, so that is a hypothesis — but "contention" is ruled out by the numbers, and the difference changes the fix.
- **Why that matters for the fix:** the developer's suggested remedy is "pass an explicit test scope, or drive the store on the test dispatcher." If the mechanism is what the timing suggests, handing DataStore the *test* scope moves its file IO onto the same single thread and can make it **worse**. The deterministic fix is the other direction: replace the real temp-file store with an in-memory `DataStore<Preferences>` over a shared `MutableStateFlow`. None of these tests asserts anything about the *file* — "killed and relaunched" is simulated by a second holder over the same store object (the class KDoc says so), which an in-memory double reproduces exactly, and the two hostile doubles already in the file show the pattern.
- **Scope is ten classes, not one.** `PreferenceDataStoreFactory.create` with no `scope` appears in `InterstitialCadenceStoreTest`, `ResultsViewModelTest:1659` (54 tests), `ChallengeProgressTest:269`, `SettingsRepositoryTest:152`, `CloudSyncStoreTest:302`, `SubmissionSyncStoreTest:102`, `CloudProfileStoreTest:99`, `PlayGamesOptInRepositoryTest:128`, `OnboardingRepositoryTest:74`, `AppForegroundTrackerTest:140`. Not one of them passes a scope. Any of the ten can flake; the one that did is just the one with two holders on one store.

**Pre-existing or made more likely by this branch? Pre-existing, and not made more likely.** `git diff develop...HEAD -- …/InterstitialCadenceStoreTest.kt` is empty; the file's last touch is `1e5a1e8` from phase 10. The branch moved one test from `src/test` to `src/testDebug`, but both compile into the same `testDebugUnitTest` task in the same JVM, so the load profile is unchanged. My own run: `:domain:test :app:testDebugUnitTest --rerun-tasks --max-workers=2` → **BUILD SUCCESSFUL in 58s**, **1203 tests, 0 failures, 0 errors, 0 skipped** over 109 classes. I did not see the flake.

**Must it be fixed before the release branch? Yes.** Three reasons, in order of weight. (1) rules/studio.md makes "the repo's Check command passes locally" the definition of done and rules/gitflow.md makes a green check a precondition for the release merge — a 60 s timeout failure fails it, so this can block a release at the worst moment, on a run nobody can reproduce on demand. (2) The studio forbids the cheap way out: no test may be `@Ignore`d or disabled, and I would reject a suppression here — there is a correct fix and it is not expensive. (3) It is a live, written-down violation of rules/testing.md's determinism rule in ten classes covering the ads, cloud-save, settings and progress stores.

**Not a blocker for this branch.** It is out of the task's scope, the file is untouched, and the developer was right not to edit a test it did not own to cure a failure it did not cause — that is rules/studio.md standing rule 4 working as intended. Owner: **qa-engineer** to write the reproduction and own the shared in-memory double, **tech-lead** to approve a helper used by ten classes. Verification must be N repeated full-suite runs under load, never a single pass in isolation — the developer's 3/3 in isolation is not evidence about this failure mode.

## Checks (re-review)
- `./gradlew detekt ktlintCheck --rerun-tasks --max-workers=2` — **pass** (`BUILD SUCCESSFUL in 6s`, `31 actionable tasks: 31 executed`; `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck`, `ktlintTestDebugSourceSetCheck` all executed, none `NO-SOURCE`/`SKIPPED`).
- `./gradlew :app:check --dry-run --max-workers=2 | grep -iE 'detekt|ktlint'` — **`:app:detekt` is the only detekt task**, plus the full ktlint set. Quoted: `:app:detekt SKIPPED`.
- Paired probe, four source sets, `./gradlew :app:detekt --rerun-tasks --max-workers=2` — **fail as intended**: `BUILD FAILED`, `> Task :app:detekt FAILED`, 13 findings; per source set main **3**, debug **3**, release **3**, testDebug **4** (two `MagicNumber` each, confirming the `**/test/**` exclude does not reach `**/testDebug/**`). Probe files deleted; `git status --porcelain` empty.
- **Negative control** — same probes with `standards.gradle.kts:64` commented out: main **3**, debug **0**, release **0**, testDebug **0**. Config restored from a scratchpad copy; `git status --porcelain` and `git diff --stat` both empty.
- detekt source census via a scratchpad-only init script (`-I`, nothing written to the repo) — `:app` **222** files: main 128, test 89, **debug 3, release 1, testDebug 1**, the five moved files listed by name; `:domain` **75**: main 39, test 36, **0** in all three variant roots.
- `./gradlew :domain:tasks --all | grep -i detekt` — **no `detektDebug`, no `detektRelease`** on `:domain` (reason 1 confirmed). `:app:tasks --all` lists both, plus `detektDebugSourceSet`/`detektReleaseSourceSet`/`detektDebugUnitTestSourceSet` — **correcting my own first-pass table, which called three of them absent**.
- `./gradlew :app:detektRelease --dry-run` (no env vars) — **fails at configuration**: `Release build requires real ad/Play Games IDs.` at `app/build.gradle.kts:110` (reason 2 confirmed; `detektReleaseSourceSet` too).
- `./gradlew :app:detektDebugSourceSet :app:detektDebugUnitTestSourceSet --rerun-tasks` with probes in place — both **`NO-SOURCE`**, 0 findings, `BUILD SUCCESSFUL` (reason 3 confirmed).
- `./gradlew :app:detektDebug --rerun-tasks` with probes in `src/main` and `src/debug` — flagged **3 + 3**, i.e. it re-analyses `src/main`; **44 s** against 2 s for the parse-only task (reason 4 confirmed, with the cost measured).
- `./gradlew :domain:test :app:testDebugUnitTest --rerun-tasks --max-workers=2` — **pass** (`BUILD SUCCESSFUL in 58s`; **1203 tests, 0 failures, 0 errors, 0 skipped** over 109 classes, counted from the JUnit XML). `InterstitialCadenceStoreTest` suite **0.029 s**; the flaky test **0.005 s** (versus 60.041 s in the reported failure).
- Added non-comment lines in `app/` and `domain/` across `88c7e2b..HEAD` — **none**; every addition is a comment (F-3 behaviour-neutral, proven).
- `git diff 88c7e2b..HEAD --name-only` — four files; **`config/detekt/detekt.yml` absent**, no baseline file anywhere (`find . -name '*baseline*.xml' -not -path '*/build/*'` → none), no `@Suppress`/`@Ignore`/disabled test/`!!` added, no dependency, resource or Room change.
- Literal-gate markers outside `src/main` (`ca-app-pub-`, `CgkI`, `requestAudioFocus`, `abandonAudioFocusRequest` over `app/src/{debug,release,testDebug}`) — **0 hits each** (F-8's factual basis).
- `grep -rln 'PreferenceDataStoreFactory.create' app/src domain/src` — **10 test classes**, none passing a `scope` (F-10's scope).
- Final state: `git status --porcelain` **empty**, HEAD `d1fc37f`, no probe residue, no commit, no source edit, nothing written into the repo by me.
- **Not run, by the task's constraints:** the full `./gradlew check` (the producer owns it), `:app:lint`, `assembleRelease`/`bundleRelease`, anything touching adb or an emulator. No release-named task was invoked, so no test ad ids were needed.

## Suppressions
**None to rule on.** No `@Suppress`, baseline entry, `@Ignore`, disabled test or narrowed exclude/source set appears in `88c7e2b..HEAD`. The undeclared blanket exclusion that F-1 described is removed rather than declared.

## Open issues / risks
- **F-8 and F-10 are both phase-14, both before the release branch**, and neither is this branch's to fix. F-8 → tech-lead (gate scope) + android-game-dev. F-10 → qa-engineer + tech-lead.
- Everything carried forward from the first pass still stands and is unchanged by this loop: the D-04a user-visible half is still unexercised on a device (`11-HUMAN-UAT.md` test 11's D-04 sub-check at a merge gap of ≥ 2, plus one debug-build picker launch); the SDK javadoc quotations behind FU-2 remain unverified against the artifact and are recorded, not verified; C-1 (D-07a) must gate any future `SCHEMA_VERSION` bump; F-4's `ResultsViewModel` extraction is scheduled, not done.
- `THIRD_PARTY.md` is still absent. Vacuously satisfied — zero dependencies changed on the whole branch — and still the release-manager's item before Gate 2.

## Handoff
Next: **tech-lead** (PASS) — sign off `feature/phase-14-prerelease-fixes` and merge to `develop` after qa-engineer PASS. Then two phase-14 items before the release branch is cut, neither of which belongs on this branch: **F-8**, decide the scope of `adIdLiteralCheck`/`gamesIdLiteralCheck`/`audioFocusLiteralCheck` and have android-game-dev add at least `src/release/{java,kotlin,res}` to `adIdSourceRoots`, verified per source root by the probe method above; and **F-10**, hand the DataStore test flake to the **qa-engineer** with my correction to the diagnosis attached — the 0.005 s versus 60.041 s gap rules out contention, so the remedy must not be "pass the test scope to DataStore" without first reproducing. **F-9** needs nothing until a flavour or a third build type appears.

`VERDICT: PASS`
