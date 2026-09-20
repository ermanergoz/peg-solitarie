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
