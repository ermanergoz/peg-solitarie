# code-auditor report — phase 13 convergence build
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (71a66e0..f30de3d, diff base c2c1006)
Skills loaded: ergoz-soft:standards (role code-auditor). The repo rules snapshot is 0.1.0 and the plugin is 0.1.1, so I read the plugin copies: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, testing.md, kotlin-android.md, compose.md, puzzle-design.md, play-store.md. Repo `.claude/rules/lessons.md` read (0 active lessons). `code-reviewer`, `security-reviewer`, `gsd-code-review` applied as review method. `compose-skill` / `compose-performance-audit` not invoked: the diff touches no composable and no `.dp`/`.sp` (0 hits).
Ran in parallel with: none
Loops: 0
Lessons applied: none (0 active lessons; `.planning/recall/RECALL.md` does not exist)


Verdict: PASS — no blocking findings (four Low/Info findings, none gating).

> Saved by the producer from the code-auditor's final message; the agent's harness did not let it write the file.

## Summary
- All five requirement blocks (cover, push guard, pull probe, debug picker, F-1/F-2 pinning) are implemented as the tech-lead specified, with the one specified deviation (`writtenByThisDevice` dropped) argued in code and allowed by section C.
- I mutation-tested the five load-bearing gates in a scratch copy of the tree: every one of them fails when the behaviour is removed, and nothing else does. The suite is not decorative.
- The `land()` change cannot strand progress: the push obligation moved from a durable marker to a per-pass reconstruction from the cloud's own bytes, and every failed push still arms the retry marker through `recordPushOutcome`.
- Security review of the exported-Activity entry: clean. The extra is not read at all on a release build (pinned by test), the incoming intent supplies only a boolean, and no result is consumed anywhere in the app.
- Four non-blocking findings: the probe drops its conflict count in exactly the conflict-storm case it was built to diagnose (L-1), the picker runs on a process-lifetime scope rather than the lifecycle (L-2), the picker and its title ship in release behind a runtime boolean (L-3), and the guard's uncapped comparison (I-1) is unreachable in practice.

## Scope
`git diff c2c1006..f30de3d -- app domain .planning/phases/13-cloud-save/13-RESEARCH.md`, 10 commits in tests-first pairs, 25 files (+1,460/−117). `domain/` is untouched by the diff. One file in the commit range is outside the review scope: `.planning/reports/phase-13/tools/prefs.py` (docstring only — see I-2).

## Requirement verification

**1. Cover — met.** `CloudSaveConstants.kt:61` is `COVER_PX = 384`; 384² × 4 = 589,824 ≤ 819,200 (arithmetic re-run independently). The new pin `CoverImageSourceTest.kt:101` computes the product from the constants (`COVER_PX * COVER_PX * BYTES_PER_ARGB_8888_PIXEL` vs `MAX_COVER_BYTES`), not from a literal, so it tracks the constant. KDoc corrected at `CoverImageSource.kt:39-49` and `CloudSaveConstants.kt:49-60`; RESEARCH A1 marked falsified with the measured GMS line, and I verified that line exists verbatim at `.planning/reports/phase-13/qa-engineer-device-pass-rerun.md:62` — the citation is faithful, not reconstructed.

**2. Post-pull push guard — met, and the `land()` change is safe.** `ProgressSubmitter.kt:508-512` + `:541-544`. Four cases tested at `ProgressSubmitterCloudTest.kt:984, 1009, 1033, 1060`, plus a fifth for played time. One-round convergence is preserved and asserted on the pushed payload's decoded keys (`:1060`), not merely on the call count.

On the removal of `cloudSyncStore.setPushPending(true)` from `land()` (`ProgressSubmitter.kt:436-440`) I checked four ways a push could be lost, and found none:
- *A "both ahead" push that fails.* `pushIfNeeded` ends at `recordPushOutcome(outcome)` (`:529`), and `CloudSyncStore.kt:119` writes `CLOUD_PUSH_PENDING_KEY = !outcome.committed` in the same edit. The marker is armed by the failure itself, never by `land`. Next launch takes the `unconditional` branch at `:508`. Unchanged from before.
- *Process death between `land` and the push.* The obligation is reconstructed, not remembered: the next launch pulls the same remote, local still holds the extra rows, `merge(remote, local) != remote`, and the push runs. The only state that could make the guard say "no" is a cloud that already holds everything — in which case nothing was owed.
- *Pull fails on a later launch.* `pulledRemote == null` and the marker is clear, so no push is attempted. Previously an attempt would have been made and would have failed (a pull failing offline means a push fails offline), arming the marker for a retry that the guard now reconstructs on the first launch with a working pull. Strictly fewer attempts, same terminal state. Accepted.
- *Undecodable cloud payload.* `remote` is `null`, so no land and no push — identical to the pre-diff behaviour (`pulledPayload` was `false` in the same case). Not a regression introduced here.

CLOUD-02 union semantics are untouched: `ProgressMerge.merge` is unchanged, `resolveOnce` and `commitOrDiscard` are unchanged, and the guard uses `merge` read-only for a comparison. `CloudProgress` is a `data class` over a `Map` (`domain/.../CloudProgress.kt:100-103`), so the `!=` is content-based and order-independent — no spurious "push" from map ordering. D-04 is unaffected: nothing in the diff reaches the in-progress game; the push payload is still `exportProgress(ladder)`.

**3. Pull probe — met.** Seam returns `CloudPull` (`CloudSaveStore.kt`), `PlayGamesCloudSaveStore.kt:500-508` reads exactly three values, one DataStore edit at `CloudSyncStore.kt:147-164` pinned by a counting-store test, key names pinned by name, push marker and push diagnostic proven untouched. `grep -rn "\.probe" app/src/main/java` → one hit, the write at `ProgressSubmitter.kt:410`: write-only, no `Flow`, no UI, no branch. Privacy is enforced by the *type* (`CloudPullProbeTest` asserts the field types) and by `verify(exactly = 0)` on six SDK string getters including `deviceName`. All three implementations (`PlayGamesCloudSaveStore`, `NoOpCloudSaveStore`, `FakeCloudSaveStore`) and their tests are updated; there is no fourth implementation. Dropping `writtenByThisDevice` is argued at `CloudPull.kt:45-54` on documented grounds ("two emulators of the same system image share one `Build.MODEL`") and section C permitted it.

*Probe semantics coherence:* "never opened" is coherent — `CloudPull.NOTHING` for a refused open, a timed-out Activity, or a store that threw, all recorded as `opened=false` with the other three keys **removed** (tested). "Conflict resolution exhausted" is where the semantics are thin — see finding L-1.

**4. Debug picker — met; security review clean.**
- *Release inertness:* `DebugSnapshotPickerEntry.kt:55` returns before the extra is read. `DebugSnapshotPickerEntryTest.aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt` asserts both the no-show and `verify(exactly = 0) { intent.getBooleanExtra(...) }`. I mutated the gate away and that test — and only that test — failed.
- *Capability ceiling on debug:* `PlayGamesSnapshotPicker.kt:61-67` passes `allowAddButton = false, allowDelete = false, maxSnapshots = 1`, asserted literally in the test. The picker is Google's own screen; the app performs no snapshot operation of its own on this path.
- *Result handling:* `grep -rn "onActivityResult" app/src/main/java` returns only comments. Nothing in the app consumes request code 9005, so no tapped snapshot can load or overwrite progress.
- *Intent redirection:* the incoming intent is read only via `getBooleanExtra`. No `getParcelableExtra`, no intent is forwarded, and the started intent comes from the SDK. No redirection surface.
- *Crash paths:* null intent returns (tested); not signed in returns after a bounded `withTimeoutOrNull` (tested, `PlayGames.getSnapshotsClient` never reached); no foreground Activity returns (tested); an SDK refusal is swallowed (tested). `onNewIntent` (`MainActivity.kt:152-155`) calls `super` first and adds nothing else.
- The 30 s wait is bounded but not lifecycle-bound — finding L-2. Shipping the class in release — finding L-3.

**5. F-1 / F-2 genuinely pinned — confirmed by mutation, not by reading.** In a scratch copy of the tree (`/private/tmp/.../scratchpad/mut`, the repo itself untouched) I removed `withContext(NonCancellable)` from `discardQuietly` and moved `metadataChangeFor` to after `openResolved`. Result: `49 tests completed, 2 failed` — exactly `aCloseStillInFlightWhenThePassIsCancelledIsAwaitedToCompletion` (`PlayGamesCloudSaveStoreTest.kt:774`) and `metadataThatCannotBeBuiltFailsThePushBeforeAnyHandleIsOpened` (`:400`). Both tests are load-bearing; neither passes vacuously.

**6. Containment and standards — met.** See Checks. No new dependency, no Room schema, no baseline, no `@Suppress`/`@Ignore`/`!!`/disabled test, no magic values (every literal the picker needs is a named constant in `CloudSaveConstants`), CLOUD-05/D-03 unchanged. The detekt-driven restructuring is sound: `probeOf` and `OpenedSnapshot` are **file-private** top-level declarations (`PlayGamesCloudSaveStore.kt:486, 500`), i.e. visible only inside that file — the same shape as the pre-existing `statusCodeOf`, and no wider than a private member in any way another file can observe. `OpenedSnapshot` holds the SDK `Snapshot` and never crosses the seam. The test split (`ProgressSubmitterCloudGuardTest`, `PlayGamesCloudSaveStorePullProbeTest`) keeps each new class on one subject and reuses the original file's fixtures; cohesion is not damaged.

## Findings

**L-1 (Low) — `PlayGamesCloudSaveStore.kt:339-344` / `CloudPull.kt:7-10`: the probe records "never opened" when conflict resolution is exhausted, discarding the conflict count in exactly the case the probe exists to diagnose.**
When `attempts == MAX_CONFLICT_RETRIES`, `resolveToOpenSnapshot` discards and returns `null`, `load` returns `CloudPull.NOTHING`, and `recordPullProbe(null)` writes `opened=false` and **removes** `play_games_cloud_last_pull_conflicts_resolved`. A device stuck in the conflict loop that hypothesis H3 describes therefore records no conflict count at all — indistinguishable on disk from a device whose `open` was simply refused. The same is true of the "ten conflicts, each resolved" path if the tenth resolution throws. Impact is diagnostic only; no player-visible behaviour and no data at risk. Suggested (not required for this phase): carry `conflictsResolved` on the exhaustion path, or note in the `CloudPull` KDoc that exhaustion is deliberately folded into "never opened" so the next reader does not chase a missing key.

**L-2 (Low) — `DebugSnapshotPickerEntry.kt:46, 57` with `di/DataModule.kt:88-89`: the picker's 30 s wait runs on the process-lifetime `@ApplicationScope`, not on the Activity lifecycle.**
`provideApplicationScope` is `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, so `scope.launch { show() }` survives the `MainActivity` that read the extra. Worst case is bounded (`DEBUG_PICKER_SIGN_IN_WAIT_MS` 30 s, then `ACTIVITY_WAIT_MS`), so this is not a leak, and a background start would be refused by the platform and swallowed by `runCatching` — but the picker can surface on a *later* Activity instance than the one launched with the extra, which will read as flakiness in a QA session. Secondarily, the two `withTimeoutOrNull` waits sit outside the `runCatching` at `PlayGamesSnapshotPicker.kt:57`, so a throw from `awaitActivity()` would escape into a scope with no exception handler. Both are debug-only. `lifecycleScope` at the call site would remove both.

**L-3 (Low) — `PlayGamesSnapshotPicker.kt` (whole file) and `CloudSaveConstants.kt:79, 86`: the picker class, the extra name and the title string are compiled into the release APK; only a runtime boolean stands in front of them.**
My judgement: **acceptable for this build, with a follow-up.** The gate is a constructor-injected `BuildConfig.DEBUG`, which is `false` in release whether or not R8 folds it, and the test proves the extra is never even read; the worst an attacker gains by decompiling is the knowledge that a debug-only read-only picker exists. But `DebugSnapshotPickerEntry` is injected into `MainActivity`, so Hilt's generated factory keeps `PlayGamesSnapshotPicker` reachable and it will not be stripped. The developer's own KDoc names the reason ("There is no `debug` source set in this module"), so this is a choice, not an oversight. Recommend to the tech-lead: either add a `src/debug` source set (or a build-type-gated `@Binds`) if this entry outlives the phase-13 investigation, or delete the entry once CLOUD-03 is verified. Not a blocker for a diagnostic build that is not being published.

**I-1 (Info) — `ProgressSubmitter.kt:511-512`: the developer's own caveat (guard compares the uncapped export) is correct but unreachable in practice; not a finding.**
`holdsSomethingTheCloudLacks` compares `exportProgress(ladder)` against the remote, while the push writes `encodeWithinCap(progress)`. A player whose payload is trimmed would push on every launch. The arithmetic says this cannot happen: `TRIM_TARGET_BYTES` is 3 MiB − 256 KiB = 2,883,584 bytes, and the codec's own KDoc measures the full 10,000-level ladder at ~400 KB — roughly 7× headroom, and `dropLowestChallenge` is documented as "expected never to fire". Even if it did fire, the behaviour degenerates to the *pre-diff* every-launch push, so it is not a regression. Accepted as-is; worth one sentence in the guard's KDoc rather than a code change.

**I-2 (Info) — `.planning/reports/phase-13/tools/prefs.py`: a committed QA helper script, touched (docstring only) by this range.**
`rules/testing.md` says QA helper scripts live under `.planning/qa/<task-slug>/` and are "never committed to a product repo". The file was introduced by the qa-engineer before this diff's base and is outside the reviewed paths; the developer only appended four lines of documentation to it. Raising it for the producer, not against this author.

## Approved suppressions
None. The diff introduces no `@Suppress`, no detekt/lint baseline entry, no `@Ignore`, no disabled or skipped test, and no `!!` in production code. Nothing required my approval.

## Checks
- `./gradlew detekt ktlintCheck --max-workers=2` — pass (`BUILD SUCCESSFUL in 432ms`, 25 tasks up-to-date).
- `./gradlew :app:detekt :domain:detekt :app:ktlintMainSourceSetCheck :app:ktlintTestSourceSetCheck :domain:ktlintCheck --rerun-tasks --max-workers=2` — pass (`BUILD SUCCESSFUL in 9s`, `23 actionable tasks: 23 executed` — re-run from scratch so the result is not a cache artefact).
- `./gradlew :app:testDebugUnitTest --max-workers=2 --tests "*CloudSave*" --tests "*ProgressSubmitter*" --tests "*CloudPull*" --tests "*SnapshotPicker*" --tests "*CoverImage*" --tests "*CloudSyncStore*"` — pass (`BUILD SUCCESSFUL in 10s`; 15 result files, **181 tests, 0 failures, 0 skipped**).
- `./gradlew :app:testDebugUnitTest --max-workers=2 --tests "*PlatformBoundary*"` — pass (6 tests, 0 failures: the Play SDK bytecode boundary still holds with the picker in the Hilt graph).
- Mutation round 1 (scratch copy, `NonCancellable` removed + metadata moved after the open) — `49 tests completed, 2 failed`: `aCloseStillInFlightWhenThePassIsCancelledIsAwaitedToCompletion`, `metadataThatCannotBeBuiltFailsThePushBeforeAnyHandleIsOpened`. F-1 and F-2 are genuinely pinned.
- Mutation round 2 (scratch copy, guard forced true + `if (!debugBuild) return` deleted + `COVER_PX` back to 512) — `20 tests completed, 5 failed`: `aCleanLaunchWhoseCloudAlreadyEqualsLocalDoesNotPush`, `aLaunchWhoseCloudIsAheadLandsItAndDoesNotPush`, `theProbeChangesNothingAboutWhatThePassDoes`, `aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt`, `theRawArgbCoverFitsThePlayGamesCoverCeiling`. Every gate is load-bearing.
- `grep -rn '^import com\.google\.android\.gms' app/src/main/java | grep -v 'com/app/pegsolitaire/platform/'` — pass (0 hits).
- `grep -rnE 'android\.util\.Log|Timber|println\(|System\.out|Log\.[dviwe]\(' app/src/main/java/com/app/pegsolitaire/platform/games app/src/main/java/com/app/pegsolitaire/data` — pass (0 hits).
- `grep -rn "RESOLUTION_POLICY" app/src/main/java` — pass (one code use, `RESOLUTION_POLICY_MANUAL` at `PlayGamesCloudSaveStore.kt:289`; one KDoc mention).
- `grep -rn "modifyBytes" app/src` — pass (0 hits).
- `git diff c2c1006..f30de3d -- app domain | grep -E '^\+.*(@Suppress|@Ignore|!!|baseline)'` — pass (0 hits).
- `git diff --name-only c2c1006..f30de3d` vs `gradle/libs.versions.toml`, `*/build.gradle.kts`, `*schema*`, `*baseline*` — pass (no dependency, no Room schema, no baseline touched; THIRD_PARTY review not applicable, 0 new dependencies).
- `grep -rn "\.probe" app/src/main/java` — pass (1 hit, the write at `ProgressSubmitter.kt:410`; nothing reads it).
- `grep -rn "CLOUD_LAST_PULL" app/src/main/java` outside the key object and the store — pass (0 hits: write-only, nothing user-visible).
- `grep -rn "onActivityResult" app/src/main/java` — pass (comments only; request code 9005 has no consumer).
- `git status --short` — pass (clean; I edited nothing in the repo, all mutation work was done on a copy under the session scratchpad).
- Not run, by the environment constraint: `./gradlew check` in full, any connected/install task, anything touching adb or the emulators.

## Open issues / risks
- The four cases are pinned against fakes. Whether the guard actually stops the every-launch rewrite on hardware, and what `play_games_cloud_last_pull_change_pending` reads on Pixel_9a, is QA's E1/E2 work — this review cannot establish it.
- L-1 means the probe will be least informative on the device the probe was built for, if H3 is right. If E2's first read comes back with `opened=false` on Pixel_9a, that is the case to reinterpret, not a store bug.
- L-3 should get an explicit decision on when the picker entry is removed; a diagnostic entry that outlives its investigation is how debug surfaces reach production.

## Handoff
Next: tech-lead — sign-off on merge to `develop`. Nothing is required from android-game-dev; L-1, L-2 and L-3 are advisory and belong on the snapshot-handle-lifetime backlog task or a follow-up, at the tech-lead's discretion. The producer owns I-2 (`prefs.py` under `.planning/reports/`, against rules/testing.md) and the CLOUD-03 wording question the triage routed to game-designer.
