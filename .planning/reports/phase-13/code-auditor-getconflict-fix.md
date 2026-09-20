# code-auditor report — phase 13 `DataOrConflict.getConflict()` fix
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (f1eb206..12514cb — diff under review `git diff f1eb206..12514cb -- app domain`; nothing committed by this review, this report left uncommitted)
Skills loaded: ergoz-soft:standards (role code-auditor) — rules read from the plugin copy 0.1.1 because `.claude/rules/VERSION` is 0.1.0: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, testing.md; `.claude/rules/lessons.md`. Also loaded: code-reviewer, security-reviewer. Not invoked: `compose-skill` / `compose-performance-audit` (no Compose UI in this diff), `typescript-pro` / `swift-expert` (wrong stacks), `gsd-code-review` (a phase-scoped GSD workflow that drives its own planning artifacts; the scope here is a two-commit diff and its checklist is covered by code-reviewer — recorded as a deliberate substitution).
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; no `.planning/recall/RECALL.md` exists)

## Summary
The root cause is real, correctly identified and correctly fixed. I verified the SDK contract myself from the pinned bytecode rather than from the diff's comments: `isConflict()` is `zzb != null`, `getConflict()` throws when it is false, `getData()` throws when it is true, and both getters are `@Nullable`-annotated (which is what made the old `?:` compile). The loop now discriminates on `isConflict` and reads each getter strictly on its own side.
Handle ownership has no gap I can find: exactly one terminal call on every path of `load`, `save`, the resolution loop and the give-up branch, with no double close. `CancellationException` is never swallowed — cleanup runs under `withContext(NonCancellable)` and both happy paths re-check with `ensureActive()`, which is the documented idiom and the right fix for my earlier F-3.
Unchanged behaviour holds: `RESOLUTION_POLICY_MANUAL` is the only policy in the tree, no `modifyBytes`, the merge orientation and bound are still pinned, and `CloudSyncStore` (one `dataStore.edit`) is not touched by this diff.
Seven findings, all Low or Info, none blocking. The two real ones are test gaps, not defects: the `NonCancellable` cleanup and the "metadata built before the open" ordering are both unpinned — the suite stays green if either is reverted.
No suppression, baseline, `@Ignore`, disabled test, `!!` or new dependency appears in this diff, so there is nothing for me to approve.

## SDK contract — verified independently (requirement 1)
`javap -c -p` on `classes.jar` from the Gradle-cached `play-services-games-v2-22.0.0.aar`
(`~/.gradle/caches/9.5.1/transforms/4d846fcb.../play-services-games-v2-22.0.0/jars/classes.jar`):

```
public boolean isConflict();
  0: aload_0 ; 1: getfield zzb ; 4: ifnull 9 ; 7: iconst_1 ; 8: ireturn ; 9: iconst_0 ; 10: ireturn

public T getData();
  0: invokevirtual isConflict:()Z ; 4: ifeq 17
  7: new java/lang/IllegalStateException ; 11: ldc "getData called when there is a conflict." ; 16: athrow

public SnapshotsClient$SnapshotConflict getConflict();
  0: invokevirtual isConflict:()Z ; 4: ifne 17
  7: new java/lang/IllegalStateException ; 11: ldc "getConflict called when there is no conflict." ; 16: athrow
```

Three consequences I checked against the diff rather than taking on trust:

- `isConflict()` is exactly `zzb != null`, so `checkNotNull(result.conflict)` at `PlayGamesCloudSaveStore.kt:328` can never throw inside `while (result.isConflict)`. The comment at `:326-327` is accurate.
- Both getters carry `RuntimeInvisibleAnnotations: androidx.annotation.Nullable` on the return, which is why Kotlin accepted `result.conflict ?: break` and why the bug was invisible at compile time. The KDoc at `:321-324` says this correctly.
- `DataOrConflict` and `SnapshotConflict` are `final` with **public** constructors, so the test fixtures at `PlayGamesCloudSaveStoreTest.kt:809-827` are the pinned SDK's behaviour, not a model of it. This is the right call and it is the single most valuable change in the diff.

## Tests-first and could-they-fail (requirement 2)
`git show --stat 77817e7` touches only `app/src/test/...` (two files); `12514cb` carries the production change plus a 3-line ktlint reflow of one test fixture signature. So the tests preceded the fix and were not reshaped to match it.

Reasoning that they genuinely fail against the pre-fix store:

- Replacing the `dataResult` mock (`every { result.conflict } returns null`) with the real `DataOrConflict(value, null)` makes `result.conflict` **throw** at the pre-fix `:296` (`result.conflict ?: break`). The throw escapes `openResolved`, so `aPushOnTheNoConflictPathCommits` (`:593`) would end at `FAILED_OTHER` instead of `COMMITTED`, and `aPullOnTheNoConflictPathReturnsTheBytes` (`:609`) would return `null` instead of `REMOTE_BYTES`. That is the exact device signature in `qa-engineer-save-reason.md`.
- The same fixture change breaks every pre-existing no-conflict test, which is why the fix commit was needed to get back to green — the 33 tests the QA report says "never saw it" now all exercise the real contract.
- `cancellationDuringTheCapQueryPropagatesAndNeverCommits` (`:759`) fails pre-fix: `exceedsServerCap` had no `onFailure` rethrow, so the cancel was absorbed into `getOrNull() ?: return false` and the push went on to commit.
- `aPullCancelledWhileItsHandleIsClosingDoesNotReturnNormally` (`:714`) fails pre-fix: without `ensureActive()` the pull sets `closed = true` and returns the bytes, so `returnedNormally` would be `true`.

They pass now: `PlayGamesCloudSaveStoreTest` 47 tests / 0 failures / 0 skipped (33 before), 123 tests across the filter, 0 failures, 0 skipped.

## Handle ownership and cancellation (requirement 3)
I traced every path. Exactly one terminal call, no double close, no lost cancel:

| Path | Terminal call | Where |
|---|---|---|
| `load` happy + `readFully` throws | `discardAndClose` | `:120-122` `finally` — the only close site on the read path, so the "latch" is genuinely unnecessary here |
| `save` commit succeeds | `commitAndClose` | `:210`, `committed = true` |
| `save` `OVER_CAP`, write throws, commit refused | `discardAndClose` | `:213-215` `finally` |
| resolution loop: read/merge throws, resolution refused, cancel | `discardAndClose(conflict.snapshot)` | `:345-347` `finally` |
| retries exhausted | `discardAndClose(conflict.snapshot)` | `:332` then `return null` |
| open refused | nothing to close | `:271-281` — throws before a handle exists |

The give-up branch returns `null`, so the caller's `?:` at `:113` / `:168-169` exits before any `try`, and the loop's `finally` does not run when `resolveOnce` succeeded. No path closes the same handle twice.

Two ordering details that are load-bearing and correct: building `change` at `:166` **before** the open means nothing throwable sits between a handle coming back and `commitOrDiscard` entering its `try` (`metadataChangeFor` calls two seams that can throw); and `commitOrDiscard`'s first suspension point is inside its `try`, so a cancel cannot land in the gap.

`withContext(NonCancellable)` at `:445` is the correct idiom — it replaces only the `Job`, keeps the dispatcher, and does not pre-check the outer job, so the close completes even when the caller is already cancelled. The cancel is not lost: it stays pending on the caller's job, a `finally` lets a propagating `CancellationException` continue, and `currentCoroutineContext().ensureActive()` at `:126` / `:174` converts a cancel that arrived *during* a non-throwing close into a throw. The three callers in `ProgressSubmitter` (`attempt`, `attemptPush`, `attemptOrNull` at `:592-628`) all rethrow `CancellationException` before classification, so the new throw reaches structured concurrency intact and does not violate CLOUD-05's "a store that throws is indistinguishable from one that returns null" — cancellation is WR-04's documented exception to that, not a regression.

The retry bound is unchanged: `resolveOnce` still runs at most `MAX_CONFLICT_RETRIES` times (`attempts` is checked at `0..MAX` before each increment), and `aConflictThatKeepsConflictingStopsAtTheRetryBound` (`:234`) still pins `verify(exactly = MAX_CONFLICT_RETRIES)`.

## The developer's decision on my earlier F-4 (requirement 6)
**Accepted, and it is the safer of the two choices.** The give-up branch at `:329-334` still closes only `conflict.snapshot`.

The reasoning holds up. The SDK settles nothing here: `SnapshotConflict` exposes `getConflictingSnapshot()` with no documented ownership claim, and the bytecode is a plain field read. So the choice is between two unproven risks with very different blast radii. Leaving `conflictingSnapshot` open risks one leaked handle, whose worst case is `OPEN_FAILED` on the next push — visible, carries the SDK status integer, recovers on the pass after, and is now pinned by `aPullWhoseCloseFailedLeavesTheNextPushToReportTheRefusedOpen` (`:775`). Discarding it risks `discardAndClose` on the device-local modified version, which is precisely where an un-uploaded offline commit lives; CLOUD-02 forbids losing it and the loss would be silent. A reviewer cannot accept a silent-data-loss risk to avoid a visible-and-recoverable one.

The pinning test is appropriate and does the job a pin has to do. `aPushThatGivesUpOnAConflictReportsTheConflictStageAndClosesTheServerVersion` (`:634`) asserts both halves — `discardAndClose(server)` exactly once **and** `discardAndClose(any())` exactly once — so it fails if someone adds the second close *or* removes the first, which forces the decision back to a human instead of letting it drift. My earlier note said "today either choice passes"; that is no longer true. The test's own comment names F-4 and states that the SDK does not settle it, which is the honest record this needed.

## Findings

### F-1 — the `NonCancellable` cleanup is not pinned by any test (Low)
`app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt:445`.

The code is right; the coverage claim around it is not. All four cancellation tests (`PlayGamesCloudSaveStoreTest.kt:698`, `:714`, `:737`, `:759`) hand `await()` a `Task` that is **already complete** (`Tasks.forResult` / `Tasks.forException`), and `Task.await()` returns without suspending when `isComplete`, so no suspension point inside `discardQuietly` ever observes job cancellation. In `:698` the `CancellationException` is a Task *failure*, not a cancelled job, so the job is still active when the `finally` runs. Deleting `withContext(NonCancellable)` leaves all 47 tests green while reintroducing the abandoned-close leak the KDoc at `:432-436` claims to have closed — the Pitfall 1 shape.

Fix: one test with a `TaskCompletionSource`-backed `discardAndClose` that is only completed **after** the pass's job has been cancelled, asserting the discard Task was awaited to completion. Not a merge blocker: the implementation is the documented idiom and I verified it by reading, so this is a regression risk, not a current defect.

### F-2 — "metadata is built before the open" is not pinned either (Low)
`PlayGamesCloudSaveStore.kt:163-166`.

The comment states that nothing which can throw may sit between the open and `commitOrDiscard`, and moving `val change = …` above the open is the whole point of that hunk. But `descriptionFormatter.format` and `coverImageSource.coverOrNull()` are stubbed non-throwing for every test (`PlayGamesCloudSaveStoreTest.kt:101-102`; only the `null`-cover and `Bitmap`-cover variants are exercised at `:369`/`:383`), so moving the line back below the open passes the whole suite and silently restores a leak-on-throw window.

Fix: a test where `coverImageSource.coverOrNull()` throws, asserting `verify(exactly = 0) { client.open(any<String>(), any(), any()) }` and `FAILED_OTHER`. Same class of gap as F-4's, which this diff did close — worth closing consistently.

### F-3 — carried forward: each conflict iteration still abandons the previous pair (Info)
`PlayGamesCloudSaveStore.kt:342-347`. When `resolveOnce` returns *another* conflict, `resolved = true`, so the superseded `conflict.snapshot` gets no terminal call and the code relies on `resolveConflict` having superseded it. This is the second sentence of my earlier F-4 and is unchanged by this diff — no better, no worse. Recording it so the phase-13 handle-lifetime task keeps it.

### F-4 — `attempts == MAX_CONFLICT_RETRIES` rather than `>=` (Info)
`PlayGamesCloudSaveStore.kt:329`. Correct today: `attempts` increments by exactly one at `:335` and the bound is pinned at `:244`. But an equality bound inside `while (result.isConflict)` is one edit away from the unbounded loop the KDoc at `:307-311` says must not exist — if `attempts` ever steps past `MAX`, the loop never exits and `resolveConflict` is called for the life of the process. `>=` costs nothing and removes the class of bug.

### F-5 — a non-conflicted open with null data is reported as `CONFLICT_UNRESOLVED` (Info)
`PlayGamesCloudSaveStore.kt:350` returns `result.data`, which the SDK annotates `@Nullable`, into an `openResolved` that returns `Snapshot?`; `save` at `:168-169` then labels that `CONFLICT_UNRESOLVED` although no conflict occurred. Cannot fire in practice with `createIfNotFound = true`. Pre-existing stage mislabel, no leak (there is no handle to close).

### F-6 — a cancelled pull whose `readFully()` also threw returns `null` rather than propagating (Info)
`PlayGamesCloudSaveStore.kt:117-129`. `ensureActive()` sits after the `try`, so it is skipped when the body threw; the non-cancellation throwable then reaches `.onFailure { … }` / `getOrNull()` and a cancelled pass returns `null`. Harmless — `null` means "stay local", the caller is being cancelled anyway — and strictly better than the pre-fix "returns bytes". Noting it only so the WR-04 claim in the KDoc is read as "no cancel is swallowed", not "every cancel becomes a throw".

### F-7 — no `THIRD_PARTY.md` in the repo (Info, process)
My checklist requires a `THIRD_PARTY.md` row per new dependency. This diff adds **no** dependency (`git diff f1eb206..12514cb --stat` over all paths shows three source files and nothing under `gradle/`), so there is nothing to record and nothing blocks. The file itself does not exist anywhere in the tree; that is a standing item for the release-manager, not a defect in this diff.

## Suppressions and standards (requirement 5)
Nothing to approve: no `@Suppress`, `@Ignore`, disabled/skipped test, `eslint-disable`, `swiftlint:disable`, `!!` or baseline entry is added, and no detekt or lint baseline file exists in the repo at all (`config/detekt/` holds only `detekt.yml`).

Two things I looked at specifically and accept:

- `checkNotNull(result.conflict)` at `PlayGamesCloudSaveStore.kt:328` is **not** a `!!` and is not covered by the ban in rules/kotlin-android.md. It is also unreachable-by-construction (`isConflict()` *is* `zzb != null`, proved above), and if a future SDK broke that, the `IllegalStateException` degrades to `FAILED_OTHER` / `null` through the existing `runCatching` rather than crashing. Accepted as written, with its two-line justification in place.
- `CloudSaveOutcomeTest.kt:45-59` swaps `Modifier.isStatic` for an exclusion by name (`COMPOSE_STABILITY_FIELD`). This **strengthens** the test — a future `companion object { var lastError: String? }` compiles to a static and is exactly the PII leak the test exists to catch, so keeping statics in scope is right. The magic string is extracted to a named constant.

Containment and no-magic-values, checked by grep:

- `^import com.google.android.gms` outside `com/app/pegsolitaire/platform/` — none.
- `Log` / `Timber` / `println` / `System.out|err` under `platform/games/` and `data/` — none. `PlatformBoundaryTest` + `PlatformBoundaryGateTest` (6 tests) still green after `statusCodeOf` moved to file scope at `:466`.
- No new literal in production code; the only numeric bound is `CloudSaveConstants.MAX_CONFLICT_RETRIES`. Every new test literal is a named constant (`TINY_SERVER_CAP_BYTES`, `OVERSIZED_PAYLOAD`, `SDK_FAILURE`, `CANCELLED`, `API_STATUS_CODE`, `SERVER_BYTES`, `COMPOSE_STABILITY_FIELD`).

Security pass: no secret-shaped string, no network API, no `http`/OkHttp/Retrofit/WebView in any added line. The PII posture is unchanged and still enforced by type — `StageFailure` carries no message and no cause (`:457-459`), `statusCodeOf` reads only the `Int` (`:466`), `discardQuietly` swallows the SDK's own throwable, and `CloudSaveOutcome` has no String-typed slot (pinned by `CloudSaveOutcomeTest`). No input crosses a trust boundary here: the payload is the app's own serialized progress and the merge is the caller's.

## Unchanged behaviour (requirement 4)
- `RESOLUTION_POLICY_MANUAL` is the only resolution policy in `app` + `domain` (`PlayGamesCloudSaveStore.kt:279`, plus two test assertions). No automatic policy anywhere.
- `modifyBytes` appears nowhere; the full-overwrite `writeBytes` and its "do not switch to it" paragraph moved verbatim into `commitOrDiscard` (`:203-208`).
- CLOUD-05 / D-03 silent degradation intact: every failure still becomes `null` (read) or a stage-only `CloudSaveOutcome` (write), nothing user-visible, and `ProgressSubmitter`'s `attempt`/`attemptPush`/`attemptOrNull` still absorb a throwing seam.
- CLOUD-02 merge semantics untouched: `mergeRemote(serverBytes, modifiedBytes)` moved verbatim into `resolveOnce` (`:359-363`), and `aConflictedOpenMergesTheServerVersionAgainstTheModifiedVersionInThatOrder` (`:181`) still pins the orientation, `:201` the `EMPTY_CHANGE` + resolution-contents write.
- Outcome stage/status still written in one `dataStore.edit` — `CloudSyncStore.kt:119-126`, a file this diff does not touch.

## Outputs
- `.planning/reports/phase-13/code-auditor-getconflict-fix.md` — this report (uncommitted)

## Checks
- `javap -c -p -classpath <cached classes.jar> 'com.google.android.gms.games.SnapshotsClient$DataOrConflict'` — pass (independently confirms `getConflict()`/`getData()` throw `IllegalStateException` off their own side of `isConflict()`; both getters `@Nullable`)
- `./gradlew detekt ktlintCheck` — pass (`BUILD SUCCESSFUL in 383ms`; `:app:detekt`, `:domain:detekt`, `:app:ktlintCheck`, `:domain:ktlintCheck` all UP-TO-DATE against the current files, no violation reported)
- `./gradlew :app:testDebugUnitTest --tests "*CloudSave*" --tests "*ProgressSubmitter*"` — pass (`BUILD SUCCESSFUL in 5s`; 123 tests, 0 failures, 0 errors, 0 skipped — `PlayGamesCloudSaveStoreTest` 47, `ProgressSubmitterCloudTest` 30, `ProgressSubmitterTest` 20, `FakeCloudSaveStoreTest` 9, `CloudSaveDescriptionFormatterTest` 5, `CloudSaveConstantsTest` 4, `CloudSaveOutcomeTest` 4, `NoOpCloudSaveStoreTest` 4)
- `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*"` — pass (6 tests, 0 failures; the SDK-containment gate survives `statusCodeOf` moving to file scope)
- `grep -rn --include='*.kt' '^import com.google.android.gms' app/src/main domain | grep -v platform/` — pass (no match)
- `grep -rnE --include='*.kt' '(Log|Timber|println)[.(]|System\.(out|err)' app/src/main/.../platform/games app/src/main/.../data` — pass (no match)
- `grep -rn --include='*.kt' 'RESOLUTION_POLICY' app domain` — pass (only `RESOLUTION_POLICY_MANUAL`, one production site)
- `grep -rn --include='*.kt' 'modifyBytes' app domain` — pass (no match)
- `git diff f1eb206..12514cb -- app domain | grep '^+' | grep -E '@Suppress|@Ignore|!!|baseline|eslint-disable|swiftlint'` — pass (no match)
- `git diff f1eb206..12514cb --stat` over all paths — pass (3 files, none under `gradle/` or `*/build.gradle.kts`: no new dependency)
- `git diff … | grep '^+' | grep -iE 'api[_-]?key|secret|token|password|credential|AIza|-----BEGIN'` and the same for `http|okhttp|retrofit|Socket|WebView` — pass (no match)
- `git show --stat 77817e7` / `12514cb` — pass (tests-first: 77817e7 is test-only; 12514cb's test change is a 3-line ktlint reflow)
- `./gradlew check` — **not run** by me, deliberately: two emulators are in use and the producer runs it. My verdict is conditional on that run being green.
- Not verified: that a commit now succeeds on a device. No emulator or device was touched by this review; only the qa-engineer can close that.

## Open issues / risks
- F-1 and F-2 are the two items worth a follow-up: both guarantees this diff adds can be reverted with the suite staying green. They are one test file and belong on the existing phase-13 snapshot-handle-lifetime task together with F-3 and F-4, not on a new one.
- Every `Task` fixture in `PlayGamesCloudSaveStoreTest` is already complete when `await()` sees it, so **no** test in the file currently exercises a real suspension being cancelled. Worth stating in the QA brief: the cancellation story is proven by reading, not by the suite.
- A second, later cloud failure behind this one is still possible (the QA report's point: no run has ever reached `commitAndClose`). The new stage/status keys will name it. That is a device question, not an audit one.
- My verdict covers `detekt`, `ktlintCheck` and the two test filters I ran. It does not cover `lint`, `koverVerify` or the fuzz task in the repo's Check command.

## Handoff
VERDICT: PASS
Next: tech-lead — sign-off and merge to `develop` once the full `./gradlew check` is green and the qa-engineer confirms `stage = COMMITTED` on both devices. Carry F-1 and F-2 onto the existing phase-13 snapshot-handle-lifetime task alongside the still-open F-3 and F-4; F-5 and F-6 are notes on it. F-7 (`THIRD_PARTY.md` does not exist) goes to the release-manager as a standing item, not a blocker.
