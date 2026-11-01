---
phase: 13-cloud-save
plan: 02
subsystem: data-persistence
tags: [datastore, cloud-save, play-games, fail-open]
requires:
  - "PlayGamesPreferences (Phase 11) — the one scoped object every play_games_* key lives in"
  - "SubmissionSyncStore (Phase 12 D-10) — the accessor shape both new stores copy"
provides:
  - "CloudSyncStore — the D-01 cloud-push-pending marker (open class, open members)"
  - "CloudProfileStore — the D-12 last-merged player id (open class, open members)"
  - "PlayGamesPreferences.CLOUD_PUSH_PENDING_KEY / DEFAULT_CLOUD_PUSH_PENDING"
  - "PlayGamesPreferences.MERGED_PLAYER_ID_KEY (no default constant — absence is null)"
affects:
  - "13-08 ProgressSubmitter — consumes both stores; subclasses them in tests"
  - "13-10 Home profile-swap dialog — reads CloudProfileStore.mergedPlayerId"
tech-stack:
  added: []
  patterns:
    - "DataStore accessor: @Singleton open class + @Inject internal constructor(DataStore<Preferences>)"
    - "Read fail-open: .catch { emit(emptyPreferences()) } then map with an explicit default"
    - "Write fail-open: runCatching { dataStore.edit { … } }.onFailure { if (it is CancellationException) throw it }"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/CloudSyncStore.kt
    - app/src/main/java/com/app/pegsolitaire/data/CloudProfileStore.kt
    - app/src/test/java/com/app/pegsolitaire/data/CloudSyncStoreTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/CloudProfileStoreTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt
decisions:
  - "Cloud push marker lives in a sibling CloudSyncStore, not a fifth key on SubmissionSyncStore — no atomicity relationship with SUBMISSIONS_PENDING"
  - "MERGED_PLAYER_ID_KEY has no DEFAULT_* constant — absence is Kotlin null, and null means merge silently (D-02), never show the destructive dialog"
  - "CloudProfileStore exposes no clear() — D-12 only ever overwrites the stored id after the swap is answered (D-15)"
metrics:
  duration: ~18 min
  tasks: 2
  commits: 3
  files-changed: 5
  tests-added: 14
  completed: 2026-09-15
---

# Phase 13 Plan 02: Cloud-Save Device-Local State Summary

Two `open` DataStore accessor classes — `CloudSyncStore` (D-01's cloud-push retry marker) and `CloudProfileStore` (D-12's last-merged player id) — plus their two `play_games_*` keys, each failing open in a direction argued in KDoc and pinned by a test.

## What Was Built

**Task 1 — two new keys on `PlayGamesPreferences`** (commit `6fc52fb`)

- `CLOUD_PUSH_PENDING_KEY` = `"play_games_cloud_push_pending"` with `DEFAULT_CLOUD_PUSH_PENDING = false`. Its KDoc argues the `false` direction the same way `DEFAULT_SUBMISSIONS_PENDING` does (`true` is more recoverable but contradicts "clean launches make no calls"; worst case bounded at one solve's delay), and states explicitly that it is *not* the same flag as `SUBMISSIONS_PENDING_KEY` — a pass can succeed at the leaderboards and fail at the Saved Games snapshot.
- `MERGED_PLAYER_ID_KEY` = `"play_games_merged_player_id"` via `stringPreferencesKey`, deliberately with **no** `DEFAULT_*` constant. Its KDoc spells out the fail-open direction: a missing key (fresh install, first-ever sign-in) and a corrupt/IO read both read as `null`, and `null` means "merge silently per D-02", never "show the profile-swap dialog" — because that dialog owns the only destructive path in the app.

**Task 2 — the two accessors, TDD** (RED `49a181f`, GREEN `1e0f836`)

- `CloudSyncStore`: `open val pushPending: Flow<Boolean>`, `open suspend fun setPushPending(pending: Boolean)`. Class KDoc carries this plan's discretion resolution (sibling store, not a fifth key) and notes no Hilt module entry is needed.
- `CloudProfileStore`: `open val mergedPlayerId: Flow<String?>`, `open suspend fun setMergedPlayerId(playerId: String)`, and no `clear()` — D-12 never un-remembers an id, it only overwrites after the swap is answered (D-15). KDoc records that the comparison happens *before* any pull or push (Pitfall 8) and that the id is never logged.
- 14 tests across two files, copying `SubmissionSyncStoreTest`'s harness verbatim (JUnit4 + Truth + `runTest`, real temp-file-backed `DataStore`, no MockK), including both directions of the independence assertion over one shared `DataStore`.

## Key Decisions

**A sibling store rather than a fifth key on `SubmissionSyncStore`.** The plan flagged this as discretion; the resolution rests on `SubmissionSyncStore`'s own KDoc argument — a flag with no atomicity relationship to the ones already in a class deserves its own accessor. The leaderboard/achievement submissions and the Saved Games snapshot are different backends that fail independently, and each outcome must be independently recoverable. Sharing one boolean would either lose the snapshot retry (cleared because leaderboards landed) or re-issue submissions that already succeeded (retained because the snapshot failed). Keys stay together in `PlayGamesPreferences`; accessors stay apart.

**No default constant for the player id.** Any default would have to be a sentinel string, and every sentinel is a value that could accidentally equal — or accidentally differ from — a real player id. `null` carries the meaning directly and makes the fail-open direction unambiguous at the type level.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing `local.properties` in the worktree**
- **Found during:** Task 1 verification
- **Issue:** `./gradlew :app:testDebugUnitTest` failed with "SDK location not found" — `local.properties` is gitignored and therefore absent from a fresh worktree, so no Gradle command could run.
- **Fix:** Created a minimal `local.properties` containing only `sdk.dir=/Users/ermanergoz/Library/Android/sdk`. Debug unit tests resolve the Play Games / ad IDs from committed placeholder defaults, so the real IDs were not needed.
- **Files modified:** `local.properties` (gitignored — confirmed absent from `git status` and from every commit in this plan)
- **Commit:** none — the file is intentionally untracked

No other deviations. Both tasks executed as written.

## Threat Model Coverage

| Threat ID | Disposition | How it is pinned |
|-----------|-------------|------------------|
| T-13-04 | mitigated | `aFreshStoreHasNoMergedPlayerId` and `aFailingReadFailsOpenToNoMergedPlayerId` both assert `null`; `grep` confirms no `DEFAULT_MERGED_PLAYER_ID` constant exists. A corrupt preferences file cannot surface the destructive dialog. |
| T-13-05 | mitigated | `grep -rn "Log\.\|println\|Timber"` over both new files returns 0 hits. |
| T-13-06 | mitigated | `aFailingWriteIsDroppedRatherThanThrown` (both stores) plus `aCancelledWriteStillPropagatesAsCancellation` — a full disk drops the marker, cancellation still propagates. |

No new security-relevant surface was introduced beyond the register: both files are app-private DataStore reads/writes with no network, no IPC and no new schema at a trust boundary.

## Verification Results

All commands run from the worktree; output confirmed, not assumed.

| Command | Result |
|---------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*CloudSyncStoreTest*" --tests "*CloudProfileStoreTest*" --tests "*SubmissionSyncStoreTest*" --tests "*PlayGamesOptInRepositoryTest*"` | BUILD SUCCESSFUL |
| Test XML counts | CloudSyncStoreTest 6/0/0, CloudProfileStoreTest 8/0/0, SubmissionSyncStoreTest 7/0/0, PlayGamesOptInRepositoryTest 6/0/0 — 27 tests, 0 skipped, 0 failures, 0 errors |
| `./gradlew :app:ktlintCheck :app:detekt` | BUILD SUCCESSFUL |
| `grep -c` for both key strings in `PlayGamesPreferences.kt` | 2 |
| `grep -rn` for both key strings outside `PlayGamesPreferences.kt` | 0 hits |
| `grep -n "fun clear"` in `CloudProfileStore.kt` | 0 hits |
| `grep -rn "Log\.\|println\|Timber"` over both new main-source files | 0 hits |
| `git diff --stat` vs base | 5 files, 527 insertions, 0 deletions — no Hilt module edited |

TDD gate sequence confirmed in git log: `test(13-02)` (`49a181f`, RED — both classes unresolved) precedes `feat(13-02)` (`1e0f836`, GREEN). No refactor commit was needed; both stores were mechanical copies of an established shape.

## Known Stubs

None. Both stores are fully wired to the real shared `app_prefs` DataStore. They have no consumers yet — `ProgressSubmitter` (13-08) and the Home dialog (13-10) are the planned callers — which is by design for a wave-1 plan, not a stub.

## Self-Check: PASSED

Files verified present:
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/CloudSyncStore.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/CloudProfileStore.kt`
- FOUND: `app/src/test/java/com/app/pegsolitaire/data/CloudSyncStoreTest.kt`
- FOUND: `app/src/test/java/com/app/pegsolitaire/data/CloudProfileStoreTest.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt` (modified)

Commits verified in git log:
- FOUND: `6fc52fb` feat(13-02): add cloud push pending and merged player id preference keys
- FOUND: `49a181f` test(13-02): add failing tests for CloudSyncStore and CloudProfileStore
- FOUND: `1e0f836` feat(13-02): implement CloudSyncStore and CloudProfileStore
