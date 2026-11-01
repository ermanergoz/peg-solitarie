---
phase: 13-cloud-save
plan: 09
subsystem: infra
tags: [play-games, saved-games, cloud-save, snapshots, conflict-resolution, hilt, mockk, junit4, truth]

# Dependency graph
requires:
  - phase: 13-cloud-save
    provides: "13-06's CloudSaveStore seam (mergeRemote on both load and save), CloudSaveConstants and the temporary NoOp binding this plan repoints"
  - phase: 13-cloud-save
    provides: "13-07's CloudSaveDescriptionFormatter and CoverImageSource — the two collaborators attached to every commit's SnapshotMetadataChange"
  - phase: 13-cloud-save
    provides: "13-08's ProgressSubmitter cloud pass — the caller whose load/save calls now reach a real SnapshotsClient"
  - phase: 11-play-games-session
    provides: "PlayGamesService's awaitForegroundActivity member, GamesConstants.ACTIVITY_WAIT_MS, the never-throw runCatching/WR-04 discipline, and PlayGamesServiceTest's mockkStatic + Tasks.forResult harness copied wholesale"
  - phase: 09-ads
    provides: "PlatformModule's SDK-containment rule — the bound type must be the SDK-free seam because the generated Hilt component carries a Provider<T> per binding"
provides:
  - "PlayGamesCloudSaveStore — the real SnapshotsClient plumbing: open under RESOLUTION_POLICY_MANUAL, bounded merge-on-conflict loop, read/write, exactly-one terminal call"
  - "The Hilt binding flip: CloudSaveStore now resolves to the real store for every build type"
  - "A runtime getMaxDataSize() guard that aborts an over-cap push cleanly instead of committing something the server rejects permanently"
  - "PlayGamesCloudSaveStoreTest — 26 MockK-driven tests over the open/conflict/commit/close lifecycle and every failure cause"
affects: [13-10, 13-11, cloud-save, saved-games, conflict-resolution, platform-boundary]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Every Saved Games type confined to one file; the client is constructed per call from an Activity and stored in no field, so no SDK type is reachable from the Hilt graph"
    - "Nullable SDK discriminator (getConflict()) driven directly as the loop predicate rather than the boolean isConflict(), so the compiler enforces the narrowing"
    - "A shared private open helper so the name/createIfNotFound/policy triple is written exactly once and cannot drift onto an automatic policy at one of two call sites"
    - "try/finally with a local latch per opened handle: exactly one terminal call on every path, including early returns out of the runCatching lambda"
    - "A runtime capability query used as a guard, not a precondition — a query that fails leaves the operation unchanged"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStoreTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt

key-decisions:
  - "The loop is driven off the nullable getConflict() rather than isConflict(): the SDK annotates getConflict() @Nullable, so the boolean form forced a !! or a safe call on every read — the nullable form makes the narrowing the compiler's job"
  - "A while/attempts loop replaced the RESEARCH sketch's repeat(MAX) { if (!isConflict) return data }: the sketch returns null when the LAST resolveConflict succeeds, discarding a perfectly good resolved handle"
  - "An exhausted conflict loop discards the handle it was holding rather than abandoning it — Google's own sample abandons it, but a leaked handle disables cloud save for the rest of the process (Pitfall 1)"
  - "The cap check was deliberately held back from task 1's commit so task 2 had a genuine RED gate; the file otherwise had to be whole because the interface has two methods and detekt rejects unused constructor properties"
  - "Pitfall 5's review-visible comment describes the partial-write API without naming it, because the plan's own acceptance grep requires that name to be absent from the file"
  - "NoOpCloudSaveStore stays in the tree, unbound, and PlatformModule no longer names it at all — so the bound implementation can only be read off the binding, never inferred from prose"

patterns-established:
  - "When a plan's acceptance grep forbids a symbol that its action text asks the code to name, the grep wins and the prose describes the symbol instead (second occurrence in this phase, after 13-06)"
  - "A capability-query guard is tested in both directions — the abort AND the failing query that must not abort — because only the second pins that it is a guard rather than a precondition"

requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05]

# Metrics
duration: 34min
completed: 2026-09-16
---

# Phase 13 Plan 09: Play Games Saved Games Plumbing Summary

**The one snapshot named `progress` now opens under `RESOLUTION_POLICY_MANUAL` — the only policy Google documents as never losing a player's changes — resolves conflicts by running the caller's real merge over both versions inside a proven bound, commits with its description, played time and cover, closes its handle exactly once on every path, and reports every possible failure as a quiet `null` or `false`.**

## Performance

- **Duration:** ~34 min
- **Started:** 2026-09-16T09:41Z
- **Completed:** 2026-09-16T10:15Z
- **Tasks:** 2 of 2
- **Files modified:** 3 (2 created, 1 modified)

## Accomplishments

- **The policy mandate is honoured and pinned.** `open` passes `SnapshotsClient.RESOLUTION_POLICY_MANUAL` explicitly, and no other `RESOLUTION_POLICY_*` constant appears anywhere in the file. The test asserts the whole triple literally — name, `createIfNotFound`, policy — rather than through a helper, because this is the line the phase is judged on and a helper would hide which integer was actually passed (T-13-34, CLOUD-02).
- **Conflicts merge, they never choose.** Both versions are read, handed to the caller's `mergeRemote` in server-then-modified order, written into `getResolutionSnapshotContents()` with `writeBytes`, and resolved through the four-argument `resolveConflict` with `EMPTY_CHANGE`. The orientation is pinned by a test even though the merge is commutative today, so a future non-commutative field cannot be added silently.
- **No handle can leak.** Every opened snapshot runs through a `try`/`finally` with a local latch, so exactly one of `commitAndClose`/`discardAndClose` is called on every path — success, thrown SDK call, an early return out of the `runCatching` lambda, and the exhausted conflict loop. The read path discards rather than commits, so a pull that changed nothing cannot move the server's `lastModifiedTimestamp` (T-13-36, Pitfalls 1 and 2).
- **The runtime cap is an enforced guard, in both directions.** An over-cap payload aborts the push and discards; a `getMaxDataSize()` that *fails* leaves the push unchanged. Only the second test makes the first one meaningfully a guard rather than a precondition (T-13-39).
- **The seam is now real end to end.** `PlatformModule` binds `PlayGamesCloudSaveStore`, and `ProgressSubmitter`'s wave-3 pull and push — written and tested green against the stub — now reach an actual `SnapshotsClient` with no change above the seam. That is the entire return on having written the stub first.
- **Containment held.** Every Saved Games type lives in this one file, the client is built per call and stored in no field, and `PlatformBoundaryTest` / `PlatformBoundaryGateTest` stay green with the real store bound (T-13-41, PLAT-04).

## Task Commits

1. **Task 1 (TDD RED): failing spec for the manual-policy open and the conflict loop** — `3e6a728` (test)
2. **Task 1 (TDD GREEN): `PlayGamesCloudSaveStore` — open, resolve, read, close** — `71ad444` (feat)
3. **Task 2 (TDD RED): failing spec for the runtime data-size cap guard** — `8ed5609` (test)
4. **Task 2 (TDD GREEN): the cap guard and the Hilt binding flip** — `6cc4ec4` (feat)
5. **Spec KDoc widened to cover the push half's failure answers** — `0c54563` (docs)

No REFACTOR commits — neither gate needed cleaning after GREEN.

## Files Created/Modified

- `platform/games/save/PlayGamesCloudSaveStore.kt` — the store. Class KDoc carries the policy mandate in Google's own words, the exactly-one-terminal-call rule, the Pitfall-4 note on offline commits reporting success, and the no-logging reservation. `load` / `save` / `openResolved` / `resolveToOpenSnapshot` / `metadataChangeFor` / `exceedsServerCap` / `discardQuietly` / `awaitForegroundActivity`.
- `test/…/save/PlayGamesCloudSaveStoreTest.kt` — 26 tests. 13 over the read path (Activity bound, open triple, bytes, empty-is-null, discard-never-commit, merge orientation, resolution write, retry bound, exhausted-loop close, thrown call, failed open, cancellation) and 13 over the write path (Activity bound, shared triple, folding existing bytes, verbatim first push, the three metadata fields, null cover, both terminal-call outcomes, cap abort, failing cap query, thrown write, unresolvable conflict, cancellation).
- `di/PlatformModule.kt` — the binding now names `PlayGamesCloudSaveStore`. The class KDoc's "three kinds of binding" bullet collapses back to two, because cloud save is no longer the odd one out; the unbound-stub paragraph now covers all three stubs.

## Decisions Made

Beyond the plan's pre-resolved discretion (the awaiting Activity accessor, `EMPTY_CHANGE` mid-loop, `writeBytes` only), four judgements were made during execution:

1. **The conflict loop is driven off `getConflict()`'s nullability, not `isConflict()`.** The SDK annotates `getConflict()` `@Nullable`, so the plan's sketched `result.conflict` after an `isConflict` test does not compile without `!!` — the compiler cannot connect the boolean to the narrowing. Reading the nullable directly (`result.conflict ?: break`) makes the discriminator and the value the same expression, which is both safer and shorter. `isConflict()` is never called.
2. **`while (attempts < MAX)` replaced `repeat(MAX) { if (!isConflict) return data }`.** The RESEARCH sketch has a real bug at the boundary: if the **tenth** `resolveConflict` succeeds, the `repeat` block ends and the function falls through to `return null`, throwing away a resolved handle and reporting a failed pass. The `while` form checks the discriminator after the loop as well, so a resolution that lands on the last permitted attempt is honoured.
3. **An exhausted loop closes the handle it was holding.** Google's sample simply returns `null`. That is a leak by the standard Pitfall 1 sets — the abandoned handle makes the *next* `open` fail for the rest of the process — so the unresolved conflict's snapshot is discarded before giving up. The plan's `<behavior>` asks for exactly this ("returning `null` with the final handle closed"); the RESEARCH sketch did not.
4. **The cap check was held out of task 1's commit on purpose.** See deviation 1: the class had to be whole in task 1 or detekt would reject it, so the cap guard was the one piece deliberately deferred in order to give task 2 a genuine RED gate rather than a spec that passed the moment it was written.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `save()` was implemented in task 1's commit rather than task 2's**

- **Found during:** Task 1, GREEN phase
- **Issue:** The plan's task 1 says to declare `coverImageSource` and `descriptionFormatter` but leave them "unused until task 2". They cannot be unused: `CloudSaveStore` has two methods, so the class does not compile without `save`, and a constructor property that nothing reads trips detekt's `UnusedPrivateProperty` — which task 1's *own* acceptance criteria require to be green (`./gradlew :app:ktlintCheck :app:detekt` exits 0). The plan's stated intermediate shape cannot satisfy the plan's stated intermediate gate.
- **Fix:** Task 1's GREEN commit contains the whole class **except** the runtime cap guard, which was held back precisely so task 2 kept a real RED gate. Task 2 then added `exceedsServerCap` and flipped the binding. Both TDD cycles observed a genuine failure first: task 1 RED was a compilation failure on `Unresolved reference 'PlayGamesCloudSaveStore'`, task 2 RED was exactly one assertion failure (`aPayloadLargerThanTheServerCapAbortsThePushWithoutCommitting`, "expected to be false") against 25 passing tests.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt`
- **Verification:** both gate commits present in order, `test(13-09)` → `feat(13-09)` twice.
- **Committed in:** `71ad444` and `6cc4ec4`

**2. [Rule 1 - Gate violation] The Pitfall 5 comment describes the partial-write API without naming it**

- **Found during:** Task 2
- **Issue:** The plan's action text asks for a review-visible comment naming the partial-write method, while the plan's own acceptance criteria require `grep -c "modifyBytes" PlayGamesCloudSaveStore.kt` to return **0**. Writing the comment as specified would have made the file fail its own gate. This is the second occurrence of the pattern in this phase — 13-06 hit the identical conflict with a gms package name in a KDoc.
- **Fix:** The comment names the behaviour and the consequence ("the partial write sibling on `SnapshotContents` … leaves the excess in place … producing `{...valid json...}xyz`") without the literal identifier. The reason is fully legible to a reviewer; the grep stays 0.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt`
- **Verification:** `grep -c "modifyBytes" …` returns 0; the comment is present at the `writeBytes` call site.
- **Committed in:** `71ad444`

**3. [Rule 1 - Bug] A ktlint-rejected inline block comment on the `createIfNotFound` argument**

- **Found during:** Task 1, after GREEN
- **Issue:** The RESEARCH sketch's `/* createIfNotFound = */ true` (Kotlin cannot use named arguments for a Java method) fails ktlint: *"A block comment may not be followed by any other element on that same line."*
- **Fix:** Replaced with a line comment above the argument, which also gave room to say *why* the flag is `true` for both halves rather than only labelling it.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt`
- **Verification:** `./gradlew :app:ktlintCheck :app:detekt` — BUILD SUCCESSFUL.
- **Committed in:** `71ad444`

**4. [Rule 1 - Bug] `PlatformModule`'s prose still named the stub after the flip**

- **Found during:** Task 2
- **Issue:** The acceptance criterion is `grep -c "NoOpCloudSaveStore" PlatformModule.kt` returns **0** — not just "the binding no longer takes it". Three KDoc references survived the parameter change, and a fourth reintroduced it by naming the stub's test class (`NoOpCloudSaveStoreTest` contains the forbidden substring).
- **Fix:** The class KDoc's "third kind of binding" bullet was collapsed (cloud save is now an ordinary swap point, so the module has two kinds of binding, not three), and the unbound-stub paragraph was extended to cover the cloud stub by description rather than by name, stating explicitly why the name is absent.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt`
- **Verification:** `grep -c "NoOpCloudSaveStore" …` returns 0; `ls …/NoOpCloudSaveStore.kt` still succeeds — the stub file was not deleted.
- **Committed in:** `6cc4ec4`

### Acceptance Criterion Not Literally Satisfiable

**`grep -rn "com.google.android.gms" app/src/main --include=*.kt | grep -vc "com/app/pegsolitaire/platform/"` returns 0** — it returns **5**, and returned **6** on the base commit before this plan ran. None of the five is an import or a type reference; they are:

| File | Kind |
|---|---|
| `App.kt:44` | KDoc prose stating the class imports nothing from the SDK |
| `di/PlatformModule.kt:119` | KDoc prose stating the Hilt component must see no SDK type |
| `ui/theme/Type.kt:53,54,60` | Downloadable-fonts provider authority/package strings and a certs resource name |

The property the criterion exists to protect — no `com.google.android.gms` **type** outside `platform/` — holds, and is enforced structurally rather than by grep: `PlatformBoundaryTest` and `PlatformBoundaryGateTest` read bytecode (including the generated Hilt component) and are green with the real store bound. This plan *reduced* the count by one. No action taken beyond recording it; the phase verifier should read the criterion as "no gms type", which the ArchUnit gate already proves.

---

**Total deviations:** 4 auto-fixed (1× Rule 3, 3× Rule 1), plus 1 acceptance criterion recorded as not literally satisfiable on any commit.
**Impact on plan:** No scope change. Every file touched is in `files_modified`. Nothing under `ui/home/` or `strings.xml` was read or written (plan 13-10 runs in parallel), and `NoOpCloudSaveStore.kt` was left in place rather than deleted.

## Issues Encountered

Two compile-level surprises against the real AAR, both resolved inside task 1:

1. **`DataOrConflict.getConflict()` is `@Nullable`.** Six errors of the form *"Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'SnapshotsClient.SnapshotConflict?'"*. Resolved by making the nullable the loop predicate (decision 1) rather than by adding `!!`, and the test fixture now models both halves of the discriminator.
2. **`SnapshotMetadataChange.Builder` runs for real in a plain JVM test.** `setCoverImage` constructs a `BitmapTeleporter`, which was a live risk for a non-Robolectric test. It works: a `mockk<Bitmap>()` passes through the builder and `change.coverImage` reads back non-null, so the CLOUD-03 metadata assertions inspect the real built object rather than verifying call counts.

## Verification Results

All commands run from the worktree, results as observed:

| Command | Result |
|---|---|
| `:app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*"` (task 1 RED) | FAILED — `Unresolved reference 'PlayGamesCloudSaveStore'` |
| `:app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*"` (task 1 GREEN) | BUILD SUCCESSFUL — 13 tests, 0 failures, 0 errors |
| `:app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*"` (task 2 RED) | FAILED — 26 tests, 1 failure: `aPayloadLargerThanTheServerCapAbortsThePushWithoutCommitting` |
| `:app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*" --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*"` (task 2 GREEN) | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest --tests "*CloudSave*" --tests "*PlayGamesCloudSaveStoreTest*" --tests "*PlatformBoundary*" --tests "*DomainPurityTest*"` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` (full app suite) | BUILD SUCCESSFUL |
| `:app:assembleDebug` | BUILD SUCCESSFUL — the Hilt graph resolves `CloudSaveStore` to the real store |
| `:app:ktlintCheck :app:detekt` | BUILD SUCCESSFUL |
| `grep -c "RESOLUTION_POLICY_MANUAL"` | 2 (≥ 1 required) |
| `grep -c` any other `RESOLUTION_POLICY_*` | 0 |
| `grep -c "modifyBytes"` | 0 |
| `grep -c "ACTIVITY_WAIT_MS"` | 2 (≥ 1 required) |
| `grep -c "activityProvider.current"` | 0 — the awaiting accessor, not the no-wait one |
| `grep -c "setDescription\|setPlayedTimeMillis\|setCoverImage"` | 3 |
| `grep -c "getMaxDataSize"` | 2 (≥ 1 required) |
| `grep -c "bindCloudSaveStore(impl: PlayGamesCloudSaveStore)" PlatformModule.kt` | 1 |
| `grep -c "NoOpCloudSaveStore" PlatformModule.kt` | 0 |
| `ls …/save/NoOpCloudSaveStore.kt` | succeeds — the stub was not deleted |
| `grep -rn "Log\.\|println\|Timber" platform/games/save/` | 0 hits (T-13-40) |
| `git diff --diff-filter=D --name-only 748004c..HEAD` | 0 files — no deletions anywhere in this plan |

Behavioural criteria, each with its own test:

| Criterion | Test |
|---|---|
| A conflicted open plus 10 further conflicts yields exactly `MAX_CONFLICT_RETRIES` `resolveConflict` calls and `null` | `aConflictThatKeepsConflictingStopsAtTheRetryBound` |
| A non-conflicted `load` calls `discardAndClose` once and `commitAndClose` zero times | `loadDiscardsItsHandleAndNeverCommits` |
| A `null` cover still yields one `commitAndClose` and `save() == true` | `aNullCoverOmitsTheArtAndStillCommits` |
| A 10-byte cap against a 100-byte payload yields `false`, one `discardAndClose`, zero `commitAndClose` | `aPayloadLargerThanTheServerCapAbortsThePushWithoutCommitting` |

**TDD gate compliance:** `test(13-09)` `3e6a728` (RED observed) → `feat(13-09)` `71ad444` (GREEN), then `test(13-09)` `8ed5609` (RED observed) → `feat(13-09)` `6cc4ec4` (GREEN). Both cycles present, in order.

## Requirements

`requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05]` — this is the plan where all four stop being scaffolding. Plans 13-06 and 13-07 deliberately left the field empty because nothing they built reached the cloud; with the binding flipped, 13-08's pull and push now run against a real `SnapshotsClient`, the commit carries its description, played time and cover, conflicts merge under `RESOLUTION_POLICY_MANUAL`, and every failure degrades to `null`/`false`.

**`REQUIREMENTS.md` was not edited** — plan 13-10 runs in parallel and names overlapping IDs, so the phase verifier reconciles the file centrally. Two caveats the verifier should carry forward, neither of which is a code gap:

- **Device verification is still outstanding.** No `SnapshotsClient` call has run against a live Play Console project. **Saved Games must be enabled in the Play Console** (Play Games Services → Configuration/Features) before any of this works on a device — it is off by default and its absence fails *every* snapshot call silently. This is already on the ROADMAP's External Prerequisites list and belongs in the phase's manual UAT.
- **R8 keep rules are unverified for the new reflection surface** (RESEARCH Pitfall 13 / assumption A5). The snapshot entities are SafeParcelables and `BitmapTeleporter` crosses a Binder. Expected clean by transitive coverage from `play-services-basement`; confirm with one `:app:assembleRelease` and an inspection of `app/build/outputs/mapping/release/missing_rules.txt` for `com.google.android.gms.games`.

## Known Stubs

None. Both seam methods are fully implemented against the real SDK and the binding points at them. `NoOpCloudSaveStore` remains in the tree deliberately and unbound, exactly as `NoOpAdGateway` and `NoOpGamesService` do, because its contract test is where the seam's fail-quiet reference behaviour stays executable — it is not a stub standing in for missing work.

## Threat Flags

None new. This plan introduces no schema change and no new trust boundary: it implements the app side of the `app → Play Games Saved Games backend` boundary the phase already declared, and payload content validation remains at the decode boundary in `data/cloud` (plan 13-04). All eight threats in this plan's register are `mitigate` and all are verified above — T-13-34 (policy grep plus the literal open-triple test), T-13-35 (`writeBytes` only, grep 0), T-13-36 (latched `try`/`finally`, both terminal-call tests), T-13-37 (bounded loop, exactly-10 test), T-13-38 (`withTimeoutOrNull`, both no-Activity tests, no-wait accessor grep 0), T-13-39 (cap abort plus the failing-query test), T-13-40 (zero logging, grep 0), T-13-41 (boundary tests green, gms type containment).

## Next Phase Readiness

Ready. The seam is real, bound, and green under the full app suite. Plan 13-10 (Home surfaces) and 13-11 (phase verification) are unblocked with no signature changes: `CloudSaveStore` is exactly what 13-06 declared, and everything this plan added lives behind it. The only remaining work for CLOUD-01/02/03/05 is on-device confirmation, which needs the Play Console Saved Games toggle rather than any further code.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStoreTest.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` — FOUND (modified)
- `app/src/main/java/com/app/pegsolitaire/platform/games/save/NoOpCloudSaveStore.kt` — FOUND (deliberately untouched)
- Commits `3e6a728`, `71ad444`, `8ed5609`, `6cc4ec4`, `0c54563` — all FOUND in `git log`
- No tracked file deleted by any commit in this plan
- `STATE.md` and `ROADMAP.md` not modified — the orchestrator owns those after the wave merges
- `REQUIREMENTS.md` not modified — parallel plan 13-10 names overlapping IDs

---
*Phase: 13-cloud-save*
*Completed: 2026-09-16*
