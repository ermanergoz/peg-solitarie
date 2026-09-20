# android-game-dev report — phase 13 cloud save outcome code (tech-lead decision B)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (0437a2b..759f040, plus the commit carrying this report)
Skills loaded: ergoz-soft:standards (role android-game-dev; plugin rules 0.1.1 read because the repo snapshot is 0.1.0: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md), superpowers:test-driven-development, gitflow (commit-message and `.planning/` exclusions overridden by rules/gitflow.md and the task). Not invoked: compose-skill, compose-ui, android-viewmodel (no UI or ViewModel file touched); android-coroutines, android-testing (the change reuses the file's existing `runCatching` + cancellation-rethrow and MockK/`Tasks.forException` idioms unchanged).
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons)

## Summary
`CloudSaveStore.save` now returns an SDK-free `CloudSaveOutcome(stage, statusCode: Int?)` instead of `Boolean`, exactly as section B specifies; `PlayGamesCloudSaveStore` classifies `open`, `resolveConflict` and `commitAndClose` separately and copies only `ApiException.statusCode`.
`ProgressSubmitter.pushIfNeeded` records the outcome through one `CloudSyncStore.recordPushOutcome` edit that writes the pending flag (still `!committed`), `play_games_cloud_last_push_stage` and `play_games_cloud_last_push_status` (removed when null). No logging, no UI, no reader in the app.
Tests were written and committed first (0437a2b, compile-red on the missing type, method and keys), then the implementation (b39e6d6). I did not fix any section A candidate; two observations are listed under Open issues.
This change diagnoses the refusal; it does not fix it. Tests 10-13 will still fail until the stage and status code are read off a device and acted on.

## Outputs
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveOutcome.kt` — new: `CloudSaveOutcome` data class and `CloudSaveStage` enum (COMMITTED, NO_ACTIVITY, OPEN_FAILED, CONFLICT_UNRESOLVED, OVER_CAP, COMMIT_FAILED, FAILED_OTHER). Kotlin/JVM types only.
- `.../platform/games/save/CloudSaveStore.kt` — `save` returns `CloudSaveOutcome`; KDoc amended to name the outcome as the one thing the seam reports (the sentence the tech-lead flagged).
- `.../platform/games/save/PlayGamesCloudSaveStore.kt` — `awaitStage` wraps each of the three awaits; private message-less, cause-less `StageFailure`; outer `runCatching` kept as the never-throws net and maps the unclaimed remainder to FAILED_OTHER. Cancellation rethrow and the exactly-once close latch are unchanged. `load` still returns `ByteArray?`.
- `.../platform/games/save/NoOpCloudSaveStore.kt` — returns `FAILED_OTHER`, no status.
- `.../data/PlayGamesPreferences.kt` — `CLOUD_LAST_PUSH_STAGE_KEY` (string), `CLOUD_LAST_PUSH_STATUS_KEY` (int).
- `.../data/CloudSyncStore.kt` — `recordPushOutcome(outcome)`: three keys, one `edit`, failed write dropped, cancellation rethrown. `setPushPending` kept for the two callers that set the marker without attempting a push (`ProgressSubmitter.kt:357`, `:425`).
- `.../data/ProgressSubmitter.kt` — `attemptPush` (a throw becomes FAILED_OTHER, nothing read off the throwable) and `recordPushOutcome` at the one call site.
- Tests: new `CloudSaveOutcomeTest` (stage names pinned, only COMMITTED is committed, instance fields are exactly the enum and `Integer`, so no `String` slot exists); `PlayGamesCloudSaveStoreTest` (every former Boolean assertion now names its stage; 7 new cases incl. status code on open / resolve / commit / other, `load` still null, cancellation inside an await); `CloudSyncStoreTest` (6 new: keys and values, stale status removed on commit, exactly one `updateData`, literal key names, dropped write, cancellation); `ProgressSubmitterCloudTest` (4 new: refused push, landed push, throwing push, declined guard records nothing); `FakeCloudSaveStore` (`saveOutcome` replaces `saveResult`), `FakeCloudSaveStoreTest`, `NoOpCloudSaveStoreTest`, `ProgressSubmitterTest` fake.
- `.planning/reports/phase-13/tools/prefs.py` — already printed every `play_games*` key including string and int values, so the two new keys needed nothing. Changed anyway: ints/longs are now signed (a negative status code printed as an unsigned 64-bit number), and a local `preferences_pb` path is accepted in place of a serial.

## Checks
- `./gradlew :app:compileDebugUnitTestKotlin -q` at 0437a2b — fail, as intended (RED: unresolved `CloudSaveOutcome`, `CloudSaveStage`, `recordPushOutcome`, `CLOUD_LAST_PUSH_STAGE_KEY`, `CLOUD_LAST_PUSH_STATUS_KEY`)
- `./gradlew :domain:test :app:testDebugUnitTest` — pass (BUILD SUCCESSFUL; app 873 tests, domain 268 tests, 0 failures, 0 errors, 0 skipped; includes the ArchUnit `PlatformBoundaryRules`)
- `./gradlew detekt ktlintCheck --rerun-tasks` — pass (BUILD SUCCESSFUL; `:app` and `:domain` detekt, main/test/script ktlint all executed). First run failed on one 120-column KDoc line in a test file; rewrapped, no rule touched.
- `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/cloud/ app/src/main/java/com/app/pegsolitaire/data/ | wc -l` — pass (0; T-11-04)
- `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v 'com/app/pegsolitaire/platform/' | wc -l` — pass (0)
- `grep -rn "com.google.android.gms" app/src/main/java | grep -v "com/app/pegsolitaire/platform/" | wc -l` (13-11-PLAN wording of PLAT-04) — 5, and 5 at base 659edf6 (`git grep` on that commit): KDoc prose in `App.kt:44` and `PlatformModule.kt:119`, the downloadable-fonts provider constants in `ui/theme/Type.kt:53,54,60`. None is an import, none is from this task. The plan's "expects 0" does not hold as worded; see Open issues.
- `grep -rn "RESOLUTION_POLICY_" app/src/main/java | grep -v RESOLUTION_POLICY_MANUAL | wc -l` — pass (0); `grep -rn "modifyBytes" app/src/main/java | wc -l` — pass (0)
- `git diff 659edf6 -- app domain | grep -E "^\+.*(@Suppress|@Ignore|baseline)" | wc -l` — pass (0 added)
- `python3 .planning/reports/phase-13/tools/prefs.py <synthetic preferences_pb>` — pass (printed `play_games_cloud_last_push_stage = OPEN_FAILED`, `play_games_cloud_last_push_status = 26572`, and `-1` for a negative int). The file was hand-encoded to the DataStore Preferences proto layout; the tool was NOT run against a device, because I was told not to touch the emulators.
- `./gradlew check` — not run, on instruction (the producer runs the full sweep).
- Not verified: anything on a device. No APK was built or installed.

## Open issues / risks
- One test needed a correction after going green-minus-one: `anOutcomeCanHoldAStageAndAnIntegerAndNothingElse` saw a third field, the Compose compiler's static `$stable` int. The filter now excludes static fields (c25dd7f). I consider that a test bug rather than a weakened assertion, since a static cannot hold per-outcome data, but the auditor should judge it.
- Section A, found while in the file, NOT fixed:
  - A2 stands as the tech-lead described it. `discardQuietly` (`PlayGamesCloudSaveStore.kt:375-380`) swallows every failure of `discardAndClose`, and `load` calls it at `:113` then sets `closed = true` regardless, so a failed discard leaves an open handle and no trace. The pull (`ProgressSubmitter.kt:402`) precedes the push (`:376`) in the same pass. With this change the consequence becomes visible rather than fixed: it would read `OPEN_FAILED` plus a status code on every push after the first pull of a process.
  - Same function, not in the tech-lead's list: that `runCatching` also swallows `CancellationException`, unlike every other `runCatching` in the file. A cancel arriving during the discard is absorbed until the next suspension point. Low impact, but it is the one place the file's own WR-04 rule is not followed.
  - `resolveToOpenSnapshot` on exhaustion (`:320`) closes `unresolved.snapshot` only, not `conflictingSnapshot` (`:299`). I could not establish from the repo whether the SDK requires the second handle to be closed; very low likelihood given A6, recorded so it is not rediscovered.
  - A4 cannot be separated from a generic commit refusal by stage alone; `COMMIT_FAILED` plus the status code is what will show it.
- Stage mapping choices section B left open: a failed `resolveConflict` await is `CONFLICT_UNRESOLVED` with its status code (same stage as loop exhaustion, which has none); `NoOpCloudSaveStore` and a throwing store are `FAILED_OTHER`.
- The 13-11-PLAN PLAT-04 grep returns 5 at the base commit. Someone should either narrow it to imports or record it as a known non-zero before task 2-3 of that plan is closed.
- The repo rules snapshot is 0.1.0 and the plugin is 0.1.1; refresh is the producer's (`/ergoz-soft:status`).
- The tech-lead report says it was left uncommitted; it is in fact committed (d196ea6). No action, noted for accuracy.

## Handoff
Next: code-auditor and qa-engineer — review 0437a2b..HEAD on `feature/phase-13-cloud-save` against section B of `tech-lead-save-path-observability.md`. After review, qa-engineer installs a debug build on both AVDs, triggers one solve while signed in and online, and reads `play_games_cloud_last_push_stage` / `play_games_cloud_last_push_status` with `tools/prefs.py <serial>`; the developer then starts from A1, A2, A4 in the tech-lead's order using those two values. Producer: run `./gradlew check`.
