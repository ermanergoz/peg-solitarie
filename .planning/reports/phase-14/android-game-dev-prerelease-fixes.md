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
Loops: 1  (loop 1 closes the code-auditor's F-1, F-2 and F-3 — see "Review loop 1" at the end)
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
  not a cache artefact.)
  - **CORRECTED in review loop 1 — this line originally claimed coverage the gate did not have,
    and that false claim is what hid F-1.** It read: "It covers the new `debug`, `release` and
    `testDebug` source sets: `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck`,
    `ktlintTestDebugSourceSetCheck`, `detektDebugSourceSet`, `detektReleaseSourceSet` all exist and
    are wired into the aggregates." Two things were wrong with it. The detekt half was false:
    `detektDebugSourceSet` reports `NO-SOURCE` under AGP 9, and `:app:detekt` — the only detekt task
    `check` invokes — read `src/main` + `src/test` only, so the five files this diff moved were
    outside detekt entirely. And "exist" is not "are wired": I inferred coverage from a task list
    instead of proving it, which is the mistake, not the task names.
  - **What is true after the loop-1 fix.** ktlint always did cover all three new source sets:
    `ktlintDebugSourceSetCheck`, `ktlintReleaseSourceSetCheck` and `ktlintTestDebugSourceSetCheck`
    all execute (not `NO-SOURCE`, not `SKIPPED`) and are reached through `ktlintCheck` — that half
    of the original claim held up. detekt now covers them too, because
    `build-logic/.../standards.gradle.kts` adds `src/{debug,release,testDebug}/{java,kotlin}` to the
    `detekt` task's own source. `check` still invokes exactly one detekt task, `:app:detekt`, and
    that task's source now contains all five moved files. Proven by probe, not by task names — see
    "Review loop 1".
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
- **detekt's `MagicNumber` excludes `**/test/**`, which does not match `**/testDebug/**`.** Still true,
  and now load-bearing rather than theoretical: as of review loop 1 `src/testDebug` *is* analysed, so
  the moved test file is held to the production no-magic-number rule that its `src/test` siblings are
  exempt from. It passes as written (`verify(exactly = 0/1)` only, and `0`/`1` are in `ignoreNumbers`),
  but anything added there loses the exemption. I still have not widened the exclude — widening a gate
  is not mine to do, and doing it in the same commit that closed F-1 would have been dodging the very
  finding I was fixing. A one-line config decision for the tech-lead if that source set grows.
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

---

## Review loop 1

Input: `.planning/reports/phase-14/code-auditor-prerelease-fixes.md` — **FAIL**, one blocking finding.
Requirements 1–4 needed no rework and none was done; D-04a, L-3/L-2, FU-2 and D-07a are untouched by
this loop apart from two added KDoc sentences (F-3). All work is on `feature/phase-14-prerelease-fixes`,
onto which the producer had cherry-picked my original commits, so the SHAs differ from the ones above
while the content is identical. No branch switch, merge, push, rebase or history rewrite.

Commits: `fe31747` (F-1), `10ee7d7` (F-3), plus this report edit (F-2).

### F-1 (blocking) — the build-type source sets are back under detekt

`build-logic/src/main/kotlin/standards.gradle.kts` now adds
`src/{debug,release,testDebug}/{java,kotlin}` — filtered to the dirs that exist — to the `detekt`
task's own source:

```kotlin
val variantSourceRoots =
    listOf("debug", "release", "testDebug")
        .flatMap { listOf("src/$it/java", "src/$it/kotlin") }
        .map { layout.projectDirectory.dir(it).asFile }
        .filter { it.exists() }

tasks.named<Detekt>("detekt").configure { source(variantSourceRoots) }
```

**Why widening the one task, and not `check.dependsOn("detektDebug", "detektRelease")`** — the
auditor offered both; four reasons picked the first, and three of them I had to measure rather than
assume:

1. **This convention plugin is applied to `:domain` too**, a plain Kotlin JVM module where
   `detektDebug`/`detektRelease` **do not exist at all** (`./gradlew :domain:tasks --all` lists only
   `detekt`, `detektMain`, `detektTest` and their source-set forms). A name-based `dependsOn` in a
   shared convention plugin would have to become variant-aware — and this plugin deliberately carries
   no AGP types, which its own dp/sp gate comment states as the reason that gate is a file scan. The
   additive-source form needs no AGP types and is inert in `:domain` (`filter { it.exists() }` leaves
   the list empty), which I verified rather than assumed: `:domain:detekt` source is 75 files,
   0 of them under `src/debug`, `src/release` or `src/testDebug`.
2. **`detektRelease`'s NAME trips the ad-ID guard.** `app/build.gradle.kts:88-91` computes
   `isReleaseInvocation` from `gradle.startParameter.taskNames.any { it.contains("elease") }`, so
   `./gradlew :app:detektRelease` dies at configuration with "Release build requires real ad/Play
   Games IDs" on any machine without them — I hit exactly that, and it is also why the auditor read
   `detektRelease` as absent. Wiring a lint gate to a task nobody can invoke directly is a trap.
3. **The per-source-set tasks are not a working substitute.** With the probe in place,
   `:app:detektDebugSourceSet`, `:app:detektReleaseSourceSet` and `:app:detektDebugUnitTestSourceSet`
   each flagged **0** — they are `NO-SOURCE` under AGP 9. (Contrary to the auditor's table these
   tasks do all exist; `./gradlew :app:tasks` hides variant tasks unless `--all` is passed. The
   coverage conclusion is unaffected: existing and empty is exactly F-1.)
4. **`detektDebug`/`detektRelease` each re-analyse `src/main`**, so every main-source finding would be
   reported two or three times, and they add type resolution and a compile classpath to what is
   otherwise a parse-only gate. `src/testDebug` would need a third task (`detektDebugUnitTest`) on top.

The result keeps one task, one config, one report and one name in `check`, and matches every other
gate in the file: a list of relative source roots filtered to those that exist. The KDoc says all of
this, and says that a future build-type or flavour source set must be added to the list.

### The auditor's paired probe, before and after

Four probe files, one per source set, each the same three lines
(`internal fun zzProbe…(): Int = 37 * 3` — two `MagicNumber` hits, and `37`/`3` are outside the
config's `ignoreNumbers`). Command both times:
`./gradlew --max-workers=4 :app:detekt --rerun-tasks` — `:app:detekt` is exactly what `check` invokes.

| probe file | before the fix | after the fix |
|---|---|---|
| `app/src/main/java/.../di/ZZProbeMain.kt` | **2 findings, `:app:detekt` FAILED** | 2 findings, FAILED |
| `app/src/debug/java/.../di/ZZProbeDebug.kt` | **0 — missed** | **2 findings, FAILED** |
| `app/src/release/java/.../di/ZZProbeRelease.kt` | **0 — missed** | **2 findings, FAILED** |
| `app/src/testDebug/java/.../ZZProbeTestDebug.kt` | **0 — missed** | **2 findings, FAILED** |

Before: `BUILD FAILED`, 2 findings, both from `ZZProbeMain.kt` — the defect the auditor described,
reproduced exactly. After: `BUILD FAILED`, **8** findings, 2 from each of the four files.

All four probe files deleted; `git status --short` then showed only
` M build-logic/src/main/kotlin/standards.gradle.kts`, i.e. no probe residue. The repo is clean of
them now (`git status` clean at the end of the loop).

Second, independent proof that does not rely on a deliberately-failing file — the `detekt` task's
own resolved source, printed by a throwaway Gradle init script kept **outside the repo** (in the
session scratchpad, nothing written to the tree):

| module | total files | `src/main` | `src/test` | `src/debug` | `src/release` | `src/testDebug` |
|---|---|---|---|---|---|---|
| `:app` | 222 | 128 | 89 | **3** | **1** | **1** |
| `:domain` | 75 | 39 | 36 | 0 | 0 | 0 |

3 + 1 + 1 = the five files F-1 names, all now inputs to the task `check` runs, and `:domain` untouched.

### Did the new coverage expose any finding? No — and that is a real result, not a vacuous one

`./gradlew --max-workers=4 detekt ktlintCheck --rerun-tasks` passes with **0 findings** across the
five newly-covered files, so nothing needed fixing and **no `@Suppress`, baseline or source-set
narrowing was added**. The reason it is genuine and not silently-skipped is the probe above: a
violation planted in each of those source sets *does* fail the build. The one place I expected trouble
was `MagicNumber`, whose config excludes `**/test/**` — which does not match `**/testDebug/**`, so
the moved `PlayGamesSnapshotPickerTest.kt` is now held to the production rule. It passes because its
only literals are `verify(exactly = 0)` / `verify(exactly = 1)` and `0`/`1` are both in the config's
`ignoreNumbers`. I did **not** widen the exclude to `**/testDebug/**` — see Open issues.

### ktlint: the other half of the original claim, confirmed true

`ADMOB_*=<Google public test ids> ./gradlew --max-workers=4 :app:ktlintDebugSourceSetCheck
:app:ktlintReleaseSourceSetCheck :app:ktlintTestDebugSourceSetCheck --rerun-tasks` — **pass**, and all
three `runKtlintCheckOver…SourceSet` tasks **executed** (not `NO-SOURCE`, not `SKIPPED`). The three
env vars are needed only because `ktlintReleaseSourceSetCheck` contains "elease" and so trips the same
guard as reason 2 above; they are Google's documented public test units, they were passed as
environment variables only, and nothing was written to the repo. In the plain
`detekt ktlintCheck` run all three also execute as part of `ktlintCheck`. So ktlint covered the new
source sets all along; only detekt did not.

### F-2 — the report line that hid F-1

The Checks entry at lines 120-124 is rewritten in place: the false sentence is quoted verbatim, marked
CORRECTED, and split into what was wrong (the detekt half, plus inferring coverage from a task list
rather than proving it) and what is true after the fix. The narrower `**/testDebug/**` note in Open
issues is kept rather than folded in — it is a *different*, still-open decision that F-1 promoted from
theoretical to live — and now says so explicitly.

### F-3 — the ceiling exception is named, the behaviour is unchanged

No code changed; `clamp` is still `coerceIn(MIN, MAX)` and still returns `MAX` at
`solvedLevel == MAX`. Two KDoc additions (`10ee7d7`):
- `ResultsViewModel.onNextLevelTapped` — a paragraph saying the "never re-serves the level just
  played" guarantee holds for `N < MAX` only, that re-serving the ceiling is the correct answer for a
  *bound* (there is no level above it, and fail-safe clamping beats crashing), that it is unreachable
  in practice because `MAX` is `GeneratorConstants.MAX_CHALLENGE_LEVEL` = 10,000, and that anyone
  lowering the ceiling to a reachable number must revisit it.
- `ChallengeNextLevelTest.nextLevelStaysInsideTheLadderBoundsWhenTheLastLevelIsSolved` — the same
  point from the test's side, naming `nextLevelNeverServesTheLevelJustPlayed` so the two are not read
  as contradicting each other, and naming this expectation as the first to revisit if `MAX` moves.

### F-4 — untouched, as instructed

`ResultsViewModel` still has 11 functions and is still exactly at detekt's `TooManyFunctions` ceiling.
I added only KDoc. The scheduled extraction is the tech-lead's.

## Checks (review loop 1)
- `./gradlew --max-workers=4 :app:detekt --rerun-tasks` with the four probe files, **before** the fix —
  **fail, as expected**: `BUILD FAILED in 3s`, 2 `MagicNumber` findings, both in
  `app/src/main/java/com/app/pegsolitaire/di/ZZProbeMain.kt:3`; the `debug`, `release` and `testDebug`
  probes produced **0**. F-1 reproduced.
- Same command **after** the fix — **fail, as intended**: `BUILD FAILED in 8s`, **8** findings, 2 in
  each of `src/main`, `src/debug`, `src/release`, `src/testDebug`. Probe files then deleted;
  `git status --short` showed only the `standards.gradle.kts` edit.
- Per-task probe sweep, probes in place: `:app:detektDebugSourceSet` **0**,
  `:app:detektReleaseSourceSet` **0**, `:app:detektDebugUnitTestSourceSet` **0**, `:app:detektDebug`
  **4**, `:app:detektMain` **4**, `:app:detektRelease` **0** (configuration failed — ad-ID guard).
- `./gradlew --max-workers=4 detekt ktlintCheck --rerun-tasks` — **pass**
  (`BUILD SUCCESSFUL in 7s`, `31 actionable tasks: 31 executed`; `:app:detekt` and `:domain:detekt`
  both executed). Run after every edit in this loop, including the two KDoc changes.
- `detekt` task source census via a scratchpad-only init script — `:app` 222 files including **3**
  `src/debug`, **1** `src/release`, **1** `src/testDebug`; `:domain` 75 files, **0** in all three.
- `ADMOB_*=<Google public test ids> ./gradlew --max-workers=4 :app:ktlintDebugSourceSetCheck :app:ktlintReleaseSourceSetCheck :app:ktlintTestDebugSourceSetCheck --rerun-tasks`
  — **pass**, all three executed. Without the env vars the same command fails at configuration with
  "Release build requires real ad/Play Games IDs" — the `contains("elease")` guard.
- `./gradlew --max-workers=4 :app:check --dry-run` — the only detekt task `check` invokes is
  **`:app:detekt`** (plus the full ktlint set, `ktlintDebugSourceSetCheck`,
  `ktlintReleaseSourceSetCheck` and `ktlintTestDebugSourceSetCheck` among them). Unchanged by the fix
  *by design*: the point is that the one task `check` already ran now sees everything.
- `./gradlew --max-workers=4 :domain:test :app:testDebugUnitTest --rerun-tasks` — **pass**
  (`BUILD SUCCESSFUL in 1m 3s`; app **935 tests, 0 failures, 0 errors, 0 skipped** over 80 classes,
  domain **268 tests, 0 failures, 0 errors, 0 skipped** over 29 classes — counted from the JUnit XML).
- `git diff 88c7e2b..HEAD | grep -E '^\+.*(@Suppress|@Ignore|Disabled|!!|baseline)'` — **0**.
  No `@Suppress`, no baseline file, no `@Ignore`, no disabled test, no `!!`, no new dependency, no
  Room schema change, nothing under `app/src/main/java/com/app/pegsolitaire/data/cloud/`, nothing
  under `.planning/approvals/`, and no edit to any report but my own.
- **Not run, by the task's constraints:** the full `./gradlew check` (the producer owns it),
  `:app:lint`, `assembleRelease`/`bundleRelease`, and anything touching adb or an emulator.

## Open issues / risks (review loop 1)
- **One pre-existing test is wall-clock flaky under load, and it failed once on my first full run.**
  `InterstitialCadenceStoreTest.theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero` failed
  with `UncompletedCoroutinesError: After waiting for 1m, the test body did not run to completion`
  (`time="60.041"`) on the first `:domain:test :app:testDebugUnitTest` run of this loop — 935 tests,
  1 failed. Cause, and it is **not** my diff: `realDataStore()`
  (`InterstitialCadenceStoreTest.kt:232-235`) calls `PreferenceDataStoreFactory.create` with **no
  `scope` argument**, so DataStore uses its default `Dispatchers.IO` scope and the test does real file
  IO *outside* `runTest`'s virtual clock, while `store.state.first { … }` waits against `runTest`'s
  real 60-second wall-clock timeout. Under `--max-workers=4` with two test tasks in flight that can
  lose the race. It passed **3/3** in isolation and the full suite then passed clean (935/268, 0
  failures). I did **not** touch it: it is not in this task's scope, and editing someone else's test
  to cure a failure I did not cause is the wrong move even when the fix looks obvious (pass an
  explicit test scope, or drive the store on the test dispatcher). Flagged for the **qa-engineer** and
  the **tech-lead**; it is also a live tension with rules/testing.md's "deterministic tests only: no
  wall-clock". Nothing about it is release-blocking, but CI will see it eventually.
- **The three literal gates in `standards.gradle.kts` still scan `src/main` only.** `adIdLiteralCheck`,
  `gamesIdLiteralCheck` and `audioFocusLiteralCheck` share `adIdSourceRoots`
  (`src/main/{java,kotlin,res}`), so an ad ID, a Play Games ID or a focus request added under
  `src/debug` or `src/release` would pass. That is the **same shape of hole as F-1, one gate over**,
  and `src/debug` is where QA-only code lands, which is exactly where a real ID gets pasted "just to
  test". I did not widen them: the auditor's F-1 is specifically about detekt, each of those gates
  documents its `src/main`-only scope as a deliberate choice (`src/test` legitimately names the APIs
  and the test-publisher prefix), and widening three gates on my own initiative in a release-week
  branch exceeds what was asked. It is a one-line change if the **tech-lead** wants it — the roots
  list is already in the file, beside the one I just added.
- The `variantSourceRoots` list is explicit, so a **new** build type or a product flavour introduces
  the same hole silently. The KDoc says to add it to the list; a flavour-aware version would need the
  AGP types this plugin deliberately avoids. Worth revisiting only if flavours ever appear.

## Handoff (review loop 1)
Next: **code-auditor** — re-review `88c7e2b..HEAD`: `fe31747` (F-1, the only functional change in this
loop), `10ee7d7` (F-3, KDoc only) and this report's commit (F-2). The check to re-run is your own
paired probe; the before/after table and the task source census above are the two independent forms of
the evidence. Worth your attention: whether widening the `detekt` task's source is the right call
against `check.dependsOn(detektDebug, detektRelease)` given reasons 1-4, and the two residual holes in
Open issues (the three `src/main`-only literal gates, and `MagicNumber`'s `**/test/**` exclude not
matching `**/testDebug/**`) — both are decisions I deliberately left to you and the tech-lead rather
than take myself.

Then: **qa-engineer** — unchanged from the first pass (`11-HUMAN-UAT.md` test 11's D-04 sub-check at a
merge gap of ≥ 2, plus one debug-build picker launch), and additionally the
`InterstitialCadenceStoreTest` flake above, with the reproduction conditions named.
