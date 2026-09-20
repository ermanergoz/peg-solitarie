# android-game-dev report — phase 13 cloud save: getConflict() fix (QA FAIL, review loop 1)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (77817e7..12514cb, plus the commit carrying this report)
Skills loaded: ergoz-soft:standards (role android-game-dev; plugin copies v0.1.1 because `.claude/rules/VERSION` is 0.1.0: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, kotlin-android.md, testing.md; `.claude/rules/lessons.md`). Not invoked, stated rather than implied: superpowers:test-driven-development, gitflow, android-coroutines, android-testing — their practice was followed (failing tests committed first, conventional commits on the assigned branch, structured-concurrency cleanup) but the Skill tool was not called for them. compose-skill / compose-ui not applicable: no UI file touched.
Ran in parallel with: none
Loops: 1
Lessons applied: none (lessons.md lists 0 active lessons)

## Summary
`resolveToOpenSnapshot` now discriminates with `isConflict()` and reads `conflict` or `data` only on its own side; the misleading "null exactly when" comment is gone. Every push and pull had died on `result.conflict ?: break`.
The test fixtures are now **real** `SnapshotsClient.DataOrConflict` / `SnapshotConflict` instances (final classes, public constructors), so the spec cannot disagree with the pinned SDK again; 29 of 47 store tests failed against the old store, and that failing state is commit 77817e7.
Handle ownership has no gap: the conflict loop closes what it holds on any throw or cancel, and `save` builds its metadata before the open. Cleanup runs `NonCancellable`; cancellation is re-checked after it (F-3).
F-2 and F-4 got tests but no behaviour change — see Open issues.

## What I verified in the SDK (not assumed)
`javap -c -p` on `classes.jar` from the Gradle-cached `play-services-games-v2-22.0.0.aar` (`…/22.0.0/ad47ab02…/`):
- `DataOrConflict.isConflict()` = `zzb != null` (the conflict field).
- `getConflict()`: `if (!isConflict()) throw IllegalStateException("getConflict called when there is no conflict.")`.
- `getData()`: `if (isConflict()) throw IllegalStateException("getData called when there is a conflict.")` — so the throwing contract is symmetric. The cached javadoc jar documents the `getData` throw and says nothing about `getConflict`, which is annotated `@Nullable` (Kotlin sees `SnapshotConflict?`); that annotation is what invited the `?:`.
- `DataOrConflict(Object, SnapshotConflict)` and `SnapshotConflict(Snapshot snapshot, String conflictId, Snapshot conflictingSnapshot, SnapshotContents resolution)` are public constructors on final classes; getter-to-field mapping confirmed (`getSnapshot`→zza, `getConflictId`→zzb, `getConflictingSnapshot`→zzc, `getResolutionSnapshotContents`→zzd).
- F-4: `games.internal.zzah` closes per `SnapshotContents` (`isClosed()` check → "Snapshot already closed", then `zzb()` on the contents of the one snapshot passed). So client-side the two conflict snapshots are distinct handles and a terminal call on one does not close the other. The javadoc (`SnapshotsClient`, `SnapshotConflict`) does not say whether the conflicting snapshot needs or tolerates a terminal call. Not verifiable from the AAR: what GMS Core does service-side.

## Outputs
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt` — B: `while (result.isConflict)` / `checkNotNull(result.conflict)` / `return result.data`; C: `resolveOnce` under a latch+`finally` inside the loop, new `commitOrDiscard` owner, metadata built before `open`; F-3: `discardQuietly` in `withContext(NonCancellable)`, `currentCoroutineContext().ensureActive()` after the close in `load` and `save`, cap query rethrows cancellation; `statusCodeOf` moved to file level (detekt `TooManyFunctions` 12 > 11, stateless helper). No new stage, key, dependency, log line or suppression.
- `app/src/test/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStoreTest.kt` — real SDK fixtures; 14 new cases: 2 SDK-contract pins, push/pull on the no-conflict path, conflicted push resolves and commits, give-up stage + which handle is closed, 4 mid-resolution ownership cases (unreadable server version, throwing merge, refused resolution, cancel), 3 cancellation cases, 1 F-2 characterization.
- `app/src/test/java/com/app/pegsolitaire/platform/games/save/CloudSaveOutcomeTest.kt` — E: filter is `it.isSynthetic || it.name == "$stable"` (named const); statics are back in scope.
- `.planning/reports/phase-13/android-game-dev-getconflict-fix.md` — this report.

## Checks
- `./gradlew :app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*" --tests "*CloudSaveOutcomeTest*"` at 77817e7 (tests only, old store) — fail as intended (47 tests, 29 failed; e.g. `aPushOnTheNoConflictPathCommits` got FAILED_OTHER, `aPullOnTheNoConflictPathReturnsTheBytes` got null). 3 new cases pass before the fix by design: the give-up pin and the two SDK-contract pins assert existing behaviour; the other 11 new cases failed.
- Same command at 12514cb — pass (47 + 4, 0 failures, 0 skipped).
- `./gradlew :domain:test :app:testDebugUnitTest` — pass (`BUILD SUCCESSFUL in 36s`; app 74 classes / 887 tests, domain 29 classes / 268 tests; failures 0, errors 0, skipped 0; includes PlatformBoundaryTest / PlatformBoundaryGateTest).
- `./gradlew detekt ktlintCheck` — pass (`BUILD SUCCESSFUL`). First run failed with 2 findings (TooManyFunctions, one MaxLineLength in the test); both fixed in code, nothing suppressed.
- `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/cloud/ app/src/main/java/com/app/pegsolitaire/data/ | wc -l` — pass (0) (T-11-04, the gate text from 13-11-PLAN.md:90)
- `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v 'com/app/pegsolitaire/platform/'` — pass (0)
- `grep -rn "RESOLUTION_POLICY_" app/src/main/java | grep -v RESOLUTION_POLICY_MANUAL` — pass (0)
- `grep -rn modifyBytes app/src/main/java` — pass (0)
- added-lines grep for `@Suppress|@Ignore|baseline|Disabled|!!` over `app/` — pass (0)
- `./gradlew check` — not run, per the brief (so no lint, koverVerify or fuzz evidence here). No emulator touched, nothing installed.
- Not verified: anything on a device. No run has ever reached `commitAndClose`; a second failure behind this one (QA's A4, cover/description) is still possible and would now show as a real stage + status.

## Open issues / risks
- **F-4 left as is, deliberately.** I could verify that the two conflict snapshots are separate client-side handles, but not that discarding `getConflictingSnapshot()` is safe. That snapshot is the device's *modified* version, possibly an offline commit GMS has not uploaded yet; if a discard dropped it, the give-up path would cause the data loss CLOUD-02 forbids — a worse outcome than a leaked handle. Today's choice (terminal call on the server version only) is now pinned by `aPushThatGivesUpOnAConflictReportsTheConflictStageAndClosesTheServerVersion`, and the mid-resolution failure paths follow the same choice. Needs a device experiment with two devices in conflict, for planning. Equally unverified and unchanged: the pair abandoned after each *successful* `resolveConflict`.
- **F-2: no behaviour change.** "The next open reflects reality" already held after the outcome diff (leaked handle → `OPEN_FAILED` + status); it is now pinned by `aPullWhoseCloseFailedLeavesTheNextPushToReportTheRefusedOpen` and written into `discardQuietly`'s KDoc. Telling a failed close apart from any other refused open needs a new stage (the 7-name enum is pinned by `CloudSaveOutcomeTest` and the tech-lead spec) or a third DataStore key — beyond a small change, so left for planning. I did not add a retry of a failed discard: it would break the exactly-once rule for no demonstrated gain.
- `withContext(NonCancellable)` means a discard await cannot be abandoned by the caller. A GMS Task that never completes would hold the pass; none of the class's other awaits are bounded either, so this is the existing risk, not a new one, but it is now also true under cancellation.
- `save` now renders the metadata (cached cover + format) even when the open is then refused. Cost is negligible; it buys a gap-free open→owner hand-off.
- `checkNotNull(result.conflict)` has an unreachable failure branch (field non-null iff `isConflict()`); it exists only to avoid `!!` on the SDK's `@Nullable`.
- Process note: CLAUDE.md asks for edits to start from a GSD command; this was a direct producer dispatch for a review loop, so none was started.
- The QA and auditor reports and QA evidence files in `.planning/reports/phase-13/` were already untracked/committed by others; I touched none of them.

## Handoff
Next: code-auditor — review 77817e7..HEAD (store + two test files); points worth a second pair of eyes: `NonCancellable` cleanup, metadata-before-open ordering, `statusCodeOf` at file level. Then qa-engineer — reinstall on both devices, solve once, read `play_games_cloud_last_push_stage` / `_status` (expect `COMMITTED`, or a real stage + code if something sits behind this), then UAT 10-13. Producer runs `./gradlew check`.
