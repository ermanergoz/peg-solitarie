# android-game-dev report — phase-14 pre-release fixes

Date: 2026-09-20
Branch: **not** `feature/phase-14-prerelease-fixes` — see "Open issues". My seven commits landed on
`feature/phase-14-release-plan` (367eef3..8f35019, excluding the release-manager's 156bf55), because
the shared working tree was switched under me mid-task; the producer is moving them.
Commits, in order: `367eef3`, `2ca03f3`, `1aa94a3`, `1a8c11b`, `11d2981`, `8f35019`, plus this report.
Skills loaded: `ergoz-soft:standards` (role android-game-dev) → plugin copies (v0.5.2) of `studio.md`,
`reporting.md`, `gitflow.md`, `skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`,
`play-store.md`, `kotlin-android.md`, `compose.md`, `testing.md`, plus the repo snapshot
`.claude/rules/lessons.md` (v0.5.1, 0 active lessons)
Ran in parallel with: `phase-14-release-plan` (release-manager, same working tree — see Open issues)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md`
does not exist)

## Summary
- **D-04a shipped.** "Next level" now serves `max(challenge_current_level, N + 1)`, resolved in
  `ResultsViewModel` off the awaitable `ChallengeProgress.settledLevel()`; `ResultsActions` passes no
  number. Seven JVM tests, three of which I mutation-checked.
- **The debug snapshot picker is out of the release build structurally**, not behind a boolean: it
  lives in a new `app/src/debug` source set with its four QA-only tokens, and the one Hilt binding is
  declared per build type (`src/debug` wires `picker::show`, `src/release` an empty one). L-2 folded in:
  the wait runs on the Activity's `lifecycleScope` and cannot throw into it.
- **FU-2's documentation corrections are corrections, not annotations** — the undocumented
  "unclosed handle blocks the next open" claim is separated from the one sentence the javadoc does
  support (in both places the file states it), the `change_pending` negative is written where the probe
  is defined, and the push guard's uncapped comparison is recorded.
- **The decode findings are recorded as D-07a in `13-CONTEXT.md`, not fixed** — three constraints a
  future `SCHEMA_VERSION` bump must satisfy.
- No new dependency, no Room schema change, no new `@Suppress`/baseline/`@Ignore`/disabled test/`!!`.

## Outputs

### 1. "Next level" after a merged resume (D-04a) — `367eef3`, `2ca03f3`
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt` — takes `ChallengeProgress`
  (read-only); `onNextLevelTapped()` lost its parameter and resolves
  `ChallengeLevelBounds.clamp(maxOf(challengeProgress.settledLevel(), solvedLevel + 1))` inside
  `viewModelScope`. The continue claim moved into `claimContinue()` so it is still taken
  *synchronously on the tap*, ahead of the suspending ladder read; the ad/fast-path split is
  unchanged (`dispatchContinue`, ex-`continueWith`). Classic returns before claiming anything.
- `ResultsActions.kt` — `onNextLevel: () -> Unit`; the leaf owns no navigation arithmetic. The label
  was already unnumbered, so nothing on screen changed and **nothing is told to the player** (D-03
  stands unamended, per the ruling).
- `ResultsScreen.kt`, `ResultsRoute.kt`, `GameViewModel.kt` — signature and KDoc follow-through.
  `ResultsRoute`'s nav lambda still receives the resolved `Int` from `pendingNavigation`.
- `app/src/test/java/com/app/pegsolitaire/ui/results/ChallengeNextLevelTest.kt` — new, 7 tests: the
  four the game-designer named, plus `nextLevelReadsTheSettledLadderRatherThanTheCachedValue`,
  `nextLevelStaysInsideTheLadderBoundsWhenTheLastLevelIsSolved` and
  `aClassicResultNeverPublishesANextLevelTarget`. The ladder fixture is an in-memory `DataStore` on a
  scope the test never advances, which freezes `currentLevel`'s `stateIn` cache at the default and is
  what makes the settled-versus-cached distinction *observable* rather than asserted.
- `ResultsViewModelTest.kt`, `ResultsScreenTest.kt` — adapted to the new signatures (3 tap call sites,
  one screen callback assertion that can no longer carry a number); no assertion was weakened, and the
  pre-D-04a expectations hold verbatim because a fresh ladder makes `max(head, N + 1) == N + 1`.
- Retry needs no code: solving a retried behind-head level goes through the same path, which is
  exactly what the ruling's section E asks for.

### 2. The debug picker behind `src/debug` (L-3) + L-2 — `1aa94a3`, `1a8c11b`
- `app/src/debug/java/.../platform/games/save/PlayGamesSnapshotPicker.kt` — moved (git rename), kept
  because it is the only instrument that can verify CLOUD-03.
- `app/src/debug/java/.../platform/games/save/DebugPickerConstants.kt` — new: the picker's title,
  one-snapshot bound, request code and sign-in wait moved out of `src/main`'s `CloudSaveConstants`, so
  the QA-only label is not in the release APK either. `DEBUG_PICKER_EXTRA` deliberately stays in
  `src/main`, because the release build's *ignoring* of the extra is a tested behaviour.
- `app/src/debug/java/.../di/DebugSnapshotPickerModule.kt` and
  `app/src/release/java/.../di/ReleaseSnapshotPickerModule.kt` — one binding declared per build type.
  A single `src/main` module cannot do this: no `src/main` file may name a class the release variant
  does not compile. Release gets `debugBuild = false` **and** an empty `show`.
- `app/src/main/java/.../DebugSnapshotPickerEntry.kt` — no longer holds `@ApplicationScope`;
  `onIntent(intent, scope)` launches on the caller's scope inside
  `runCatching { }.onFailure { if (it is CancellationException) throw it }`, so neither
  `withTimeoutOrNull` wait can cancel a `lifecycleScope` that has no handler.
- `MainActivity.kt` — both call sites pass `lifecycleScope`.
- `app/src/testDebug/java/.../PlayGamesSnapshotPickerTest.kt` — moved beside the class it tests (5
  tests, unchanged assertions). `DebugSnapshotPickerEntryTest` stays in `src/test` and keeps
  `aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt` verbatim; two cases added for the scope contract.
- The adb command is unchanged:
  `adb -s <serial> shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true`.

### 3. Documentation corrections (FU-2) — `11d2981`
- `PlayGamesCloudSaveStore.kt` class KDoc — splits the documented sentence (`commitAndClose` fails on
  a snapshot not opened / already closed) from the undocumented one (an unclosed handle fails the next
  open for the process), states that the bundled 22.0.0 javadoc does not contain the latter and that
  the phase's UAT could not exercise it, and keeps the `try`/`finally` practice with its own
  justification. The same claim at the second site (`resolveToOpenSnapshot`'s item 1) is corrected the
  same way, as are the three places `PlayGamesCloudSaveStoreTest` restated it.
- The `:59-61` "no API can observe the upload" claim **was already corrected on the phase-13 branch**
  (commit `94cfe6f`); I tightened the surviving changelog-style wording and pointed it at the probe's
  new negative rather than re-stating the fix.
- `CloudPull.kt` (`CloudPullProbe`) — new section "What `changePending` does NOT mean": it is a fact
  about the snapshot at the moment of the open, it was measured `true` on a device whose content had
  demonstrably already reached another device (each launch's push guard made a fresh local commit),
  and the usable oracle is `false` **together with** `lastModifiedAt` equal to this device's own push.
- `ProgressSubmitter.holdsSomethingTheCloudLacks` — one paragraph recording the I-1 caveat: the guard
  compares the **uncapped** export against the capped push, ~7x headroom makes it unreachable, and if
  it ever fired the behaviour degrades to the pre-guard every-launch push, not to a regression.
- `13-RESEARCH.md` — Pitfall 1 rewritten with a CORRECTED banner (claim, what is actually documented,
  why the UAT is silent, and that the avoidance advice is unchanged); its threat-model row softened;
  the A1 row now records that the later clean-device sessions are silent on A1 **in both directions**,
  because a `COVER_PX = 384` build (384²x4 = 589,824 ≤ 819,200) is arithmetically incapable of
  emitting the warning, while A1's own falsification rests on the exact integers 512²x4 = 1,048,576.

### 4. Decode findings recorded, not fixed — `8f35019`
- `.planning/phases/13-cloud-save/13-CONTEXT.md` — new decision note **D-07a** under D-07, as three
  constraints on a future schema bump: **C-1** (F-1: `mergeBytes` maps "corrupt" and "decodable but
  newer" to the same empty identity, so a payload this build refuses to read is silently overwritten
  as a full overwrite — unreachable at `SCHEMA_VERSION = 1`, live during any staged rollout; must
  return the remote bytes verbatim or abort the push), **C-2** (F-2: no row-count cap and no Classic
  `levelId` validation; rows persist, feed `LevelResultDao.totals` into leaderboards/achievements, and
  can wedge every later push at `OVER_CAP` — a row-count cap is the fix, and id validation explicitly
  is **not**, because it would break D-07 forward compatibility), **C-3** (F-3: `dropCount`'s `Int`
  multiplication overflows at ~210 KB overshoot on a 10,000-row ladder; widen to `Long`). Each notes
  that it needs a test that fails first, because today both behaviours pass.

## Checks
- `./gradlew --max-workers=4 :domain:test :app:testDebugUnitTest` — **pass**
  (`BUILD SUCCESSFUL in 33s`; app **935 tests, 0 failures, 0 skipped** over 80 classes; domain **268
  tests, 0 failures, 0 skipped** over 29 classes — counted from the JUnit XML, not from the log).
- `./gradlew --max-workers=4 detekt ktlintCheck --rerun-tasks` — **pass**
  (`BUILD SUCCESSFUL in 9s`, `31 actionable tasks: 31 executed` — re-run from scratch, so the result is
  not a cache artefact. It covers the new `debug`, `release` and `testDebug` source sets:
  `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck`, `ktlintTestDebugSourceSetCheck`,
  `detektDebugSourceSet`, `detektReleaseSourceSet` all exist and are wired into the aggregates.)
- **Item 2's proof that the picker is not in release** — exactly what I ran, in order:
  1. `ADMOB_APP_ID=x ADMOB_REWARDED_AD_UNIT_ID=x ADMOB_INTERSTITIAL_AD_UNIT_ID=x ./gradlew --max-workers=4 :app:compileReleaseKotlin :app:transformReleaseClassesWithAsm`
     — **pass**. The three placeholder *environment variables* are needed only because
     `app/build.gradle.kts` fails any release-named invocation without real ad IDs, which this machine's
     `local.properties` does not carry (it carries the 27 PGS ids). Nothing was written to the repo, no
     placeholder is committed, and no APK/AAB was produced.
  2. `rm -rf app/build/intermediates/classes/release` before the run, because that directory held
     **stale** classes from the producer's 22:31 `assembleRelease` — without deleting it the check
     would have reported the old picker and been wrong in the alarming direction.
  3. `find app/build/intermediates/classes/release -iname "*PlayGamesSnapshotPicker*" -o -iname "*DebugPicker*" | wc -l`
     — **0** (the class set that feeds packaging).
  4. `grep -rl "Saved games (debug)" app/build/intermediates/classes/release | wc -l` — **0** (the
     QA-only title string is gone too, not just the class).
  5. `find app/build/intermediates/classes/release -iname "*ReleaseSnapshotPickerModule*" | wc -l` —
     **5** (the inert binding *is* there, so the graph still resolves: absence of the picker is not
     absence of a binding).
  6. Contrast, same command shape over
     `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes` — the picker,
     `DebugPickerConstants` and `DebugSnapshotPickerModule` are **all present**, so the check can fail.
- `grep -rnE 'android\.util\.Log|Timber|println\(|System\.out|Log\.[dviwe]\(' app/src/main/java/com/app/pegsolitaire/platform/games app/src/main/java/com/app/pegsolitaire/data app/src/debug/java | wc -l`
  — **0** (T-11-04, extended to the new `debug` source set).
- `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v 'com/app/pegsolitaire/platform/' | wc -l`
  — **0**. Also `grep -rn '^import com.google.android.gms' app/src/debug/java | grep -v 'com/app/pegsolitaire/platform/'`
  — **0** (the moved picker stayed inside `platform/`).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests "*PlatformBoundary*"` — **pass** (6 tests):
  the bytecode SDK-containment rule still holds with the picker provided from a variant module.
- `git diff f9d6ef9..HEAD -- app/src domain/src | grep -E '^\+.*(@Suppress|@Ignore|!!|baseline|@Disabled)' | wc -l`
  — **0**.
- **Mutation checks on the new D-04a tests** (scratch edits to `ResultsViewModel`, all reverted):
  `clamp(solvedLevel + 1)` (the old behaviour) → **3 failed**
  (`nextLevelIsLadderHeadAfterSolvingBehindTheHead`, `nextLevelNeverServesACompletedLevelWhenTheHeadIsAhead`,
  `nextLevelReadsTheSettledLadderRatherThanTheCachedValue`);
  `clamp(settledLevel())` (drop the `max`) → **1 failed**
  (`nextLevelNeverServesTheLevelJustPlayed`);
  `maxOf(currentLevel.value, solvedLevel + 1)` (cached read) → **3 failed**. Every clause is
  load-bearing. The third mutation initially passed against a real temp-file store, which is why the
  fixture freezes the `stateIn` cache — worth knowing before anyone "simplifies" that harness.
- **Red-first evidence:** `367eef3` and `1aa94a3` were committed with the tests failing to compile
  against the then-current API (`:app:compileDebugUnitTestKotlin FAILED`, "No value passed for
  parameter 'nextLevel'" / "Unresolved reference 'onIntent'"), before `2ca03f3` and `1a8c11b`.
- **Not run, by the task's constraints:** the full `./gradlew check` (the producer owns it),
  `:app:assembleRelease`/`bundleRelease`, `:app:lint`, and anything touching adb or an emulator. No
  emulator was started.

## Open issues / risks
- **My commits are on the wrong branch, by the producer's own account.** The release-manager ran
  `git checkout -b feature/phase-14-release-plan develop` in the shared tree mid-task, so
  `367eef3`, `2ca03f3`, `1aa94a3`, `1a8c11b`, `11d2981`, `8f35019` and this report's commit sit on that
  branch; `feature/phase-14-prerelease-fixes` is still at `develop`. I was instructed not to repair
  history and did not: no checkout, rebase, reset, cherry-pick or branch. `156bf55`
  (`docs(14): plan the phase 14 external and release track for v1.1`) is the release-manager's and is
  not mine; I touched nothing under `.planning/phases/14-release-readiness/` or
  `.planning/reports/phase-14/release-manager-plan.md`.
- **Nothing here was exercised on a device.** D-04a's user-visible half is
  `11-HUMAN-UAT.md` test 11's D-04 sub-check at a gap of ≥ 2, which QA must re-run; the picker move
  needs one debug-build `am start` to confirm the extra still opens the screen from the Activity's
  scope. Both are QA's, and both are cheap.
- **`app/src/release` exists solely to declare one binding.** That is load-bearing (deleting it breaks
  the release build, it does not shrink it) and its KDoc says so, but it is a new, easily-missed source
  set. A reviewer adding a second variant-specific binding should put it in the same two files rather
  than creating a third pair.
- **detekt's `MagicNumber` excludes `**/test/**`, which does not match `**/testDebug/**`.** The one
  file I moved there has no bare literals so it passes today, but anything added to `src/testDebug`
  loses that exemption. I did not widen the exclude, because widening a gate is not mine to do — worth
  a one-line config decision from the tech-lead if that source set grows.
- **C-1 (D-07a) is the one recorded finding I would not let a v1.2 schema bump start without.** It is a
  silent data loss on the device that is behind during a staged rollout, and both the right and the
  wrong behaviour pass the current suite.
- The `ResultsViewModel` constructor is now 6 parameters and the class 11 functions — exactly at
  detekt's `TooManyFunctions` ceiling. I inlined the next-level arithmetic into `onNextLevelTapped`
  rather than take a suppression; the next function added to that class will need a real
  restructuring, not a `@Suppress`.

## Handoff
Next: **code-auditor** — review `367eef3..8f35019` (mine only; skip `156bf55`). The three places I
would look hardest: `ResultsViewModel.onNextLevelTapped`'s claim-then-suspend ordering (a second tap
must still be impossible), the two variant Hilt modules (one binding, two build types — and whether
`DebugSnapshotPickerEntry` staying in `src/main` is the right line), and whether D-07a's C-2 records
the Classic-id decision the way the sweep intended (cap rows, do **not** validate ids).

Then: **qa-engineer** — `11-HUMAN-UAT.md` test 11's D-04 sub-check re-run at a merge gap of **≥ 2**
(a gap of 1 cannot distinguish the fix: `head == N + 1` there), and one debug-build picker launch with
the adb command quoted above to confirm the source-set move did not break CLOUD-03 verification. No
copy, layout or new surface changed, so the ux-designer is not needed.
