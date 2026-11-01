# tech-lead report — phase 13 save-path observability (question mode)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (30f6198..30f6198 — nothing committed; this report is left uncommitted on the coordinator's instruction; no app source edited)
Skills loaded: ergoz-soft:standards (role tech-lead; rules studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, stacks.md, testing.md, kotlin-android.md, compose.md, puzzle-design.md, play-store.md from `.claude/rules/`, VERSION 0.1.0 = plugin 0.1.0). android-architecture / android-gradle-logic not invoked: no module or build-logic decision is made here.
Ran in parallel with: none
Loops: 0
Lessons applied: none (lessons.md lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
Read as written, the save path has no defect I can point at that would make `save` return false on a signed-in, online, foreground device; every candidate other than a server-side refusal of `open` is rated low. The Play Console toggle (or a published-but-not-yet-propagated toggle) remains by far the most likely cause.
QA's inference is sound: every read before the store call fails safe, so `setPushPending(!committed)` at `ProgressSubmitter.kt:501` was reached and `save` really returned false.
Tech-lead decision (B): yes, add a non-PII outcome code, persisted to the existing app-private Play Games DataStore, no logging, no UI. It does not block the re-run; it becomes the first task if the founder says the toggle is already on and published.

## A. Failure modes other than the toggle (signed-in, online, foreground)
Paths are under `app/src/main/java/com/app/pegsolitaire/`. "Same `false`" means indistinguishable from the toggle today.

| # | Candidate | Where | Likelihood | Why |
|---|-----------|-------|------------|-----|
| A1 | `open` rejected by GMS for a reason adjacent to the toggle: toggle on but PGS configuration not published, or enabled after the emulators' GMS cached the game's feature set / granted scopes (Saved Games rides on a Drive app-data grant that GMS obtains at sign-in) | `platform/games/save/PlayGamesCloudSaveStore.kt:197-206`, swallowed at `:160-161` | **medium** | Not a code bug, but it survives a "yes, it is on" answer. If the founder flips or confirms the toggle, re-run only after a sign-out/sign-in (or clearing Play Games + GMS data) on both AVDs. I could not verify the scope mechanics from the repo; treat as a hypothesis. |
| A2 | Handle left open by the pull that precedes every first push in a process: `load` opens with `createIfNotFound = true` and closes through `discardQuietly`, which swallows a failed `discardAndClose`; the class KDoc itself says a leaked handle makes the next `open` fail for the rest of the process | `load` `:98-110`, `discardQuietly` `:325-330`; ordering in `data/ProgressSubmitter.kt:370` then `:374` | **low** | Would reproduce QA's evidence exactly (pull and push share a pass on every launch), but only if discarding a freshly created, never-committed snapshot fails systematically, which Google's own sample does without issue. First thing to check if the toggle is confirmed on, because it is the only candidate that is structural rather than input-dependent. |
| A3 | No foreground Activity within 10 s | `PlayGamesCloudSaveStore.kt:135`, `:183-184`; `platform/games/GamesConstants.kt:37` | **very low** | The failing pushes were triggered from the Results screen and from a foreground relaunch; submissions in the same pass reached Google. A host at load ~80 could starve one pass, not every pass on two devices. |
| A4 | Commit rejected because of the metadata: cover bitmap (`setCoverImage`, 512 px ARGB_8888) or description | `metadataChangeFor` `:283-292`, commit `:154`; `CoverImageSource.kt:78-89`; `CloudSaveDescriptionFormatter.kt:61-69` | **low** | Cover is ~36 KB PNG against an 800 KB floor; the bitmap is a software bitmap, cached and never recycled. Format string `values/strings.xml:301` takes four `%d` and receives four `Int`s, so no `IllegalFormatException`. RESEARCH A1/A2 (cover accounting, description max) are still `[ASSUMED]`, which is why this is low and not nil. Cheap experiment: commit once with `EMPTY_CHANGE`. |
| A5 | Size cap | `exceedsServerCap` `:309-315`, early return `:145` | **very low** | Payload after four solves is a few hundred bytes; cap is >= 3 MB; a failed cap query returns "not exceeded". |
| A6 | Conflict loop exhausted (10) | `resolveToOpenSnapshot` `:245-271`; `CloudSaveConstants.kt:47` | **very low** | Needs an existing server snapshot; no commit has ever succeeded, and the Play Games app shows no save. |
| A7 | Wrong snapshot name | `CloudSaveConstants.kt:34` (`"progress"`) | **nil** | Legal charset, asserted by `CloudSaveConstantsTest`. |
| A8 | Something in the pass throws before the store is called, so the flag is stale rather than written | `ProgressSubmitter.kt:483-485`, `:492-501` | **nil** | `settledLevel` (`data/ChallengeProgress.kt:127-131`) and `exportProgress` (`data/ProgressRepository.kt:421-425`) are `storageFailSafe`; `encodeWithinCap` and the store call are inside `attempt`; the flag default is `false` (`data/PlayGamesPreferences.kt:89`). Line 501 is reached. |
| A9 | Stub bound instead of the real store, or binding absent from the tested APK | `di/PlatformModule.kt:146` | **nil** | Binds `PlayGamesCloudSaveStore`; the binding commit 6cc4ec4 is an ancestor of 2e1ca2c, the commit QA's byte-identical APK was built from. Debug is not minified, so R8 is not in play. |
| A10 | API misuse in the open/resolve/commit sequence | `:139-156`, `:248-263` | **very low** | `RESOLUTION_POLICY_MANUAL`, read-then-full-`writeBytes`, `commitAndClose` once, `EMPTY_CHANGE` during resolution, server/modified orientation: all match the SDK contract. Client is built from an Activity, off-main is allowed for Task APIs. |

One datum QA recorded that fits "`open` refused" better than any app-side cause: offline, with a signed-in session, the push also stayed pending (test 13), although an enabled Saved Games feature accepts an offline commit into its local queue. A never-synced snapshot may also refuse an offline open, so this is supporting, not proof.

Developer starting order if the toggle is confirmed on and published: A1 (re-consent / GMS cache, no code), A2, A4. All three are separated in one run by the outcome code in B.

## B. Recommendation — add a persisted, non-PII outcome code (tech-lead decision)
Yes. Without it a second device pass can fail in exactly the same undiagnosable way, and the only alternatives are a debugger session or a temporary log that the gate forbids.

Why not a debug-only log: T-13-40 / T-11-04 are enforced by a grep gate expecting 0 hits for `Log\.|println|Timber` under `platform/games/` and `data/` (13-11-PLAN.md:90), and `CloudSaveDescriptionFormatter`'s KDoc rules out DEBUG logging explicitly. A log would need a gate exception, and there is no `src/debug` source set to hide it in. Not worth it.

Smallest design that works:
1. `CloudSaveStore.save` returns a small SDK-free value instead of `Boolean`: `CloudSaveOutcome(stage, statusCode: Int?)` where `stage` is an enum `COMMITTED, NO_ACTIVITY, OPEN_FAILED, CONFLICT_UNRESOLVED, OVER_CAP, COMMIT_FAILED, FAILED_OTHER` and `committed` is `stage == COMMITTED`. Kotlin/JVM types only, so `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` stays green.
2. `statusCode` is `ApiException.statusCode` when the failure is an `ApiException`, else null. It is the datum that actually discriminates: `OPEN_FAILED` alone would still not separate toggle-off from a network or auth refusal. It is a Google status integer, not player data. The exception **message**, snapshot id, player id, description and payload are never captured.
3. `PlayGamesCloudSaveStore` classifies by wrapping the three awaits (`open`, `resolveConflict`, `commitAndClose`) separately instead of one outer `runCatching`; cancellation handling and the exactly-once close latch are unchanged.
4. `ProgressSubmitter.pushIfNeeded` keeps `setPushPending(!outcome.committed)` and writes two more keys in the same `CloudSyncStore` edit: `play_games_cloud_last_push_stage` (enum name) and `play_games_cloud_last_push_status` (int, absent when null). A throw caught by `attempt` maps to `FAILED_OTHER`.
5. No reader in the app. `tools/prefs.py` already dumps every `play_games_*` key through `run-as`, so QA needs no tool change. Written in all build types: one code path (what is tested is what ships), app-private storage, unreadable on a non-debuggable build, two scalars overwritten per push.

Consistency: T-11-04 / T-13-40 protect player id, display name, description, payload and SDK exception messages from logcat; an enum name and a status integer in app-private DataStore are none of those, and the grep gate stays at 0. CLOUD-05 and D-03 are untouched: no UI, no throw, failure is still a return value and the retry bookkeeping is identical. `CloudSaveStore`'s KDoc sentence "an implementation classifies inside `platform/` and reports nothing further" must be amended to name the outcome code as the one thing reported.

Out of scope on purpose: `load` keeps returning `ByteArray?`. `open` is shared, so the push outcome diagnoses the pull too; on a freshly wiped device QA forces a push with one solve. Extend to `load` only if a pull-only failure ever appears.

Cost: one interface signature, `PlayGamesCloudSaveStore`, `NoOpCloudSaveStore`, `FakeCloudSaveStore`, `CloudSyncStore`, `PlayGamesPreferences`, one call site, and their tests. New tests: one per stage in `PlayGamesCloudSaveStoreTest`, one `ProgressSubmitterTest` case asserting the keys, and an assertion that no outcome field carries a `String` from the SDK.

## Outputs
- `.planning/reports/phase-13/tech-lead-save-path-observability.md` — this report (uncommitted)

## Checks
- `git merge-base --is-ancestor 6cc4ec4 2e1ca2c` — pass (real `CloudSaveStore` binding is in the APK QA tested)
- `grep -rn "CloudSaveStore" app/src/main/java/com/app/pegsolitaire/di/` — pass (`PlatformModule.kt:146` binds `PlayGamesCloudSaveStore`)
- `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/ | wc -l` — pass (0; T-11-04 gate green today)
- `grep -rn "cloud_save" app/src/main/res/values*/strings.xml` — pass (one string, four `%d`, matches the four `Int` args at `CloudSaveDescriptionFormatter.kt:63-68`)
- `grep -rn "DEFAULT_CLOUD_PUSH_PENDING =" app/src/main/java` — pass (`false`, so a `True` flag was written, not defaulted)
- `grep -n "suspend fun settledLevel" -A8 data/ChallengeProgress.kt`, `grep -n "fun exportProgress" -A14 data/ProgressRepository.kt` — pass (both `storageFailSafe`; nothing throws before `:501`)
- `grep -n "playServicesGamesV2 =" gradle/libs.versions.toml` — pass (22.0.0, as pinned in CLAUDE.md)
- `grep -rn "minify\|applicationId" app/build.gradle.kts` — pass (minify only on release; applicationId `com.erman.pegsolitarie` matches the PGS-linked package)
- `ls app/src` — info (`main`, `test` only; no `debug` source set)
- `./gradlew check` — not run: no source changed
- Not verified: the GMS-side behaviour behind A1 and A2 (scope grant after a late toggle; discard of a never-committed snapshot). Both need a device with the outcome code, or a debugger.

## Open issues / risks
- `docs/tech-design.md` does not exist in this repo (the product predates the studio pipeline), so this decision is recorded only here. The producer should carry B into the phase-13 plan set (a 13-12 plan or a `/gsd-quick` task) where the developer will find it.
- A1 means "the founder confirmed the toggle" is not sufficient on its own for the re-run: both AVDs need a fresh Play Games sign-in after the configuration is published.
- B changes a seam signature that three implementations and several tests depend on; it is small but not a one-liner, and it must go through the normal auditor + QA review. I will not sign off code I specify in more detail than this.

## Handoff
Next: producer — put Step 0 to the founder as already planned. If the toggle was off: enable, publish, re-sign-in on both AVDs, QA re-runs tests 10-13; schedule B as a non-blocking task. If the toggle was already on and published: dispatch B to android-game-dev first, then QA re-runs and reads `play_games_cloud_last_push_stage` / `_status` with `tools/prefs.py`; the developer starts from A1, A2, A4 in that order.
