# Phase 12: Leaderboards & Achievements - Research

**Researched:** 2026-09-07
**Domain:** Play Games Services v2 leaderboards + achievements; Room v1→v2 migration; SQL aggregation; build-time ID plumbing
**Confidence:** HIGH for the SDK surface and Room mechanics (verified by `javap`/javadoc against the pinned 22.0.0 AAR and 2.8.4 artifact in this repo's own Gradle cache); MEDIUM for Play Console behaviour; LOW for signed-out failure taxonomy and popup rendering (no Console project exists — same `[UNVERIFIED — D-04]` posture Phase 11 established).

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Achievement roster (20 achievements)
- **D-01:** The **hint-free** and **undo-free** skill achievements unlock on any solved level in **either mode**. **"Center finish"** means solving **English-33 with the last peg on the center hole `(3,3)`** — the classic target and the only board where it is a real skill goal. European-37 starts off-center, Diamond-41 cannot reach one peg, and Challenge boards have no defined center, so it applies nowhere else.
- **D-02:** The **four first-hour early wins** (PGSA-01) are: **First solve** (any mode), **First 3-star** (any mode), **Challenge level 5** (one-shot), and the first of the **per-board Classic unlocks** (D-04). A separate "First Classic board" achievement was considered and **dropped** as fully implied by the per-board unlocks.
- **D-03:** Progression (PGSA-03) is **two tiered axes, each tier its own Play Games incremental achievement** so the stock UI shows a progress bar: **ladder tiers at Challenge level 10 / 50 / 100** and **star tiers at 25 / 100 / 250 total stars**. Six achievements. Steps are reported as **absolute values** (highest Challenge level reached; total stars across both modes) via D-12, never as deltas. Both aggregates are SQL over `level_result`.
- **D-04:** The remaining one-shots: **Grand tour** (every one of the five Classic ids has a stored result), **Perfectionist** (every Classic board's stored best is at that board's minimum-peg finish — Diamond-41's proven minimum of 2 counts as perfect), **five per-board first-solve unlocks** (one per Classic id: English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45), and **Speed solve** (any level solved in **under 60 seconds** of `elapsedSeconds`).
- **D-05:** Roster total is **20 visible achievements**, which means **20 unique 512×512 icons** — the ROADMAP External Prerequisites row budgeted 10–12 and must be updated. Names, descriptions, and point values are at the planner's discretion (see Claude's Discretion).

#### Per-session telemetry and score definitions
- **D-06:** Telemetry is stored in a **new `play_session` Room table, one row per solved play**: `mode`, `levelId`, `stars`, `score`, `elapsedMs`, `hintsUsed`, `undosUsed`, `lastPegOnTarget`, `finishedAt`. **`level_result` is untouched**, so the **v1 → v2 migration is a pure `CREATE TABLE`** and trivially progress-preserving; it still ships with a `MigrationTest` case that seeds v1 rows and asserts they survive, plus the exported `2.json` schema. All achievement predicates over telemetry become SQL over this table.
- **D-07:** **"Cumulative Challenge score" (PGSL-02) = `SUM(bestScore)` over `level_result` where `mode = challenge`** — the sum of per-level bests. It is recomputable, monotonic, only rises when a best improves, and stays identical across devices once Phase 13 syncs bests. It is **not** a running total of every play.
- **D-08:** A new **pure-Kotlin `SolveTelemetry(hintsUsed, undosUsed, lastPegOnTarget)`** type in `:domain` rides **alongside `ScoreResult`** through `GameViewModel`'s finish event and the Results nav args. `GameViewModel` counts assists and reads the last peg's position from the finishing board. **`ProgressRepository.recordResult` writes `level_result` and `play_session` in one transaction**, under the existing `recordSolvedPlayOnce()` exactly-once guard. `ScoreResult` stays a scoring type.
- **D-09:** **Every hint and every undo counts**, free-allowance or rewarded. The **counters persist in the resume snapshot** as nullable fields (the `freeHintsRemaining` / `freeUndosRemaining` precedent), so a resumed game cannot launder an assist by killing the process; absent fields on an old snapshot read as zero. **Restart and a new level reset** the counters.

#### Offline backfill and submission orchestration
- **D-10:** Backfill (PGSL-05) is a **dirty-marker sync**: a persisted **"submissions pending"** flag is set by **every `recordResult` while the session is not `SignedIn`** and by **any failed submission**. **Whenever `session` resolves to `SignedIn`**, if the flag is set, everything is resubmitted from SQL aggregates and the flag is cleared **only on success**. Self-healing and idempotent; clean launches make no calls.
- **D-11:** Backfill covers **leaderboards and achievements**, both re-derived from `level_result` + `play_session`. A player who reached Challenge 50 offline receives the ladder tiers, per-board unlocks, Grand tour, and Perfectionist on sign-in. **One `AchievementRules` pass over the aggregates serves both live solves and backfill** — there is no second rule set.
- **D-12:** The `GamesService` seam **gains `setAchievementSteps(achievementId: String, steps: Int)`** (absolute, matching Play Games' `setSteps`), and **all tiered achievements use it** so resubmission is always safe. `incrementAchievement` stays declared but unused, or is removed — planner's call. `NoOpGamesService` and the test fake implement it trivially.
- **D-13:** Orchestration lives in **one data-layer `@Singleton` (working name `ProgressSubmitter`)** that owns the dirty flag, computes aggregates through `ProgressRepository` SQL, applies `AchievementRules`, and calls `GamesService`. `ResultsViewModel` calls it once after `recordResult` returns; the same object observes `session` for the D-10 backfill. **Nothing in `ui/` touches a leaderboard or achievement ID.** All calls are fire-and-forget from the caller's point of view (PGSL-04).

#### Entry points, popup, and ID plumbing
- **D-14:** The stock UIs open from **two new Settings rows only** — **"Leaderboards"** (opens the all-leaderboards picker) and **"Achievements"** — placed **under the signed-in row in the Play Games section**, using the existing `SettingsLinkRow` shape. No Results or Home affordance.
- **D-15:** The two rows are **hidden while signed out**; only the sign-in row shows (PGS-02 parity with Phase 11's section). Signing in reveals them.
- **D-16:** The **stock achievement unlock popup stays, in its default position** — no `setGravityForPopups`, no suppression, no app-owned toast (parity with Phase 11 D-05 for the welcome popup). It renders over Results at solve time and that is intended.
- **D-17:** The **7 leaderboard IDs and 20 achievement IDs** are resolved from **`local.properties` keys (CI secrets) into `BuildConfig` fields and exposed through a `GamesIds` object modeled on `AdIds`**, per build type, with **release failing fast on missing values** exactly as the ad IDs do. No `games-ids.xml` in source.

### Claude's Discretion
- Achievement **names, descriptions, and Play Games point values** for all 20; whether any are marked hidden (default: all visible, per PGSA-01's "visible").
- **Ladder aggregate source**: `MAX` of the Challenge `levelId` in `level_result` (the column is TEXT — cast in SQL) versus `ChallengeProgress`'s persisted highest level; SQL over `level_result` is preferred per the ROADMAP note, and the two must agree.
- Exact **SQL for every aggregate and predicate** (`SUM(bestScore)`, `SUM(stars)` across modes, per-board best, Grand tour / Perfectionist checks, hint-free / undo-free / center existence over `play_session`), and whether they live on `LevelResultDao`, a new `PlaySessionDao`, or a small aggregate DAO.
- **`play_session` retention**: no pruning by default (thousands of rows is fine); add a cap only if research shows a cost.
- Where the **dirty flag** lives (a boolean key beside the opt-in flags in `PlayGamesPreferences` / `PlayGamesOptInRepository` is the obvious home) and whether it records *what* is pending or just *that* something is.
- How `SolveTelemetry` reaches Results: extending the existing `GameRoute` → `ResultsRoute` nav args versus a small sealed payload; how `GameViewModel` counts (increment on the assist funnel's success path in `onUndo()` / `onHintRequested()`, not on request) and how `lastPegOnTarget` is computed from the finishing board's single peg against the spec's target cell.
- Snapshot field names for the persisted counters and the `SnapshotMapper` defaults for old blobs.
- Whether `showLeaderboard(id)` / `showAchievements()` open through `ForegroundActivityProvider` inside `PlayGamesService` (the seam's current `suspend` shape) or via an `ActivityResultLauncher` owned by `SettingsRoute` (the ARCHITECTURE sketch); the seam signature must not gain an Activity type either way.
- `GamesIds` key naming in `local.properties.template` and the build-logic resolution (extend `AdIdResolution.kt` or add a sibling).
- Test doubles: a recording fake `GamesService` for `ProgressSubmitter` and `SettingsViewModel` tests; `MigrationTest` v1 → v2 fixture shape; `AchievementRules` JVM tests in `:domain`.
- Whether `PlayGamesService`'s five no-op Phase-12 bodies (`submitScore`, `unlockAchievement`, `setAchievementSteps`, `showLeaderboard`, `showAchievements`) get real bodies in one plan or alongside their first caller.
- **Operator checklist additions**: create 7 leaderboards + 20 achievements in Play Console, upload 20 icons, record the generated IDs into CI secrets and `local.properties`; extend `11-HUMAN-UAT.md`'s emulator checklist with submission, popup, and stock-UI checks.

### Deferred Ideas (OUT OF SCOPE)
- A "View leaderboard" shortcut on the solved Results screen and a trophy button on Home — considered, rejected for this phase (D-14); revisit with the v2 social-first leaderboard UI.
- Bottom-gravity or custom-styled unlock popup — rejected (D-16); custom Play Games UI is v2.
- `play_session` pruning / retention cap — only if research or profiling shows a cost.
- Additional achievement tiers with new content — v1.1 explicit future-consideration item; the roster above is the v1.1 set.
- **Reviewed Todos (not folded):** Stuck-finish-reaches-Results decision; confirm audio cues on physical hardware; Wiegleb-45 opening book K=3→K=4.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PGSL-01 | Highest Challenge level submitted to a ladder leaderboard on completion | `LeaderboardsClient.submitScore(String, long)` verified present in 22.0.0; ladder value from `MAX(CAST(levelId AS INTEGER))` (§SQL Aggregates); "Numeric" leaderboard format, larger-is-better |
| PGSL-02 | Cumulative Challenge score submitted to its own leaderboard | `SUM(bestScore) WHERE mode='challenge'` returned as **`Long?`** (§Pitfall 6 — Int overflow); one `submitScore` call |
| PGSL-03 | Per-Classic-board best submitted to that board's leaderboard | 5 board-keyed leaderboard IDs via `GamesIds.leaderboardForClassic(levelId)`; value = `level_result.bestScore` |
| PGSL-04 | A failed submission never fails, blocks, or alters a solved level | `ProgressSubmitter` runs on `@ApplicationScope`, wrapped in `runCatching`; `ResultsViewModel` never awaits it (§Pattern 2) |
| PGSL-05 | Existing bests submitted once on sign-in after offline play | Dirty-flag sync; `submitScore` server-side ignores worse scores (verified javadoc) ⇒ idempotent (§Pattern 4) |
| PGSL-06 | Player can open the leaderboard UI from the game | `getAllLeaderboardsIntent(): Task<Intent>` + **mandatory `startActivityForResult`** (verified javadoc) (§Pattern 5) |
| PGSA-01 | ≥10 visible achievements, ≥4 reachable in the first hour | Google's own quality bar is exactly "minimum 10 visible / ≥4 within 1 hour / 512×512 icons"; the 20-item D-05 roster clears it. **Console-side, not code-side** |
| PGSA-02 | Skill achievements: hint-free, undo-free, last peg in center | `play_session` predicates; `lastPegOnTarget` computed against `Boards.English33.target == Position(3,3)` (§SQL Aggregates) |
| PGSA-03 | Incremental progression achievements for ladder + stars | `AchievementsClient.setSteps(id, n)` — **verified monotonic and idempotent**: "calling this method while the achievement already has more steps than the provided value is a no-op" (§SDK Semantics) |
| PGSA-04 | In-app unlock popup when an achievement is earned | Rendered by the Play Games SDK itself. **v2 exposes no popup API at all** — `PlayGames` has no `getGamesClient()`, so `setViewForPopups`/`setGravityForPopups` are unreachable (verified by `javap`). D-16 requires literally zero code. **Not JVM-verifiable** (§Assumption A2) |
| PGSA-05 | Player can open the achievements UI from the game | `getAchievementsIntent(): Task<Intent>` + `startActivityForResult` (§Pattern 5) |
| PGSA-06 | Telemetry recorded via a progress-preserving schema migration | Pure `CREATE TABLE` `Migration(1,2)`; `MigrationTestHelper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)` verified present in room-testing 2.8.4 (§Pattern 6) |
</phase_requirements>

---

## Project Constraints (from CLAUDE.md)

| Directive | What it means for this phase |
|-----------|------------------------------|
| No magic numbers/strings; **scoped** constants objects | Tier thresholds (10/50/100, 25/100/250), the 60 s speed limit, the 5-board count, `startActivityForResult` request codes, table/column names — all into scoped objects (`AchievementThresholds` in `:domain`, `GamesConstants` for request codes, `ProgressPersistenceKeys` for on-disk names). detekt's `ignoreNumbers` only exempts `-1, 0, 1, 2`. |
| Clean Architecture; pure-Kotlin `:domain`, zero Android deps | `SolveTelemetry`, `AchievementId`, `AchievementRules`, `AchievementThresholds` go in `:domain` with **no** PGS ID strings — the enum→string mapping lives in `platform/games/GamesIds.kt`. `DomainPurityTest` is bytecode-based and will catch a violation. |
| Ads and Play Games behind `AdGateway` / `GamesService` | `ProgressSubmitter` (in `data/`) may only touch the `GamesService` **interface**. `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity` covers `..data..` too — the submitter can never hold an Activity. |
| Compose state hoisting | The two new Settings rows are stateless `SettingsLinkRow`s driven by `SettingsUiState.gamesSignInState`; actions are `SettingsViewModel` method refs. |
| dimens resource file, no hardcoded dp/sp | The new rows reuse `SettingsLinkRow`'s existing spacing; `dpSpLiteralCheck` fails the build on any `<number>.dp` under `ui/`. |
| detekt + ktlint passing as a build gate | `./gradlew check` runs `detekt`, `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`. |
| Gitflow strictly | Work on `feature/phase-12-leaderboards-achievements` (already the current branch), off `develop`. |
| Version catalog is the single source of truth | **No new dependency is needed** (§Standard Stack). Nothing to add to `libs.versions.toml`. |

---

## Summary

This phase adds no new dependency and no new SDK. Everything it needs — `play-services-games-v2:22.0.0`, `kotlinx-coroutines-play-services` (pinned to the coroutines 1.11.0 line), `room 2.8.4` + `room-testing` — is already on the graph and already exercised by Phase 11. The work is three separable pieces: a **pure `CREATE TABLE` Room v1→v2 migration** carrying per-session telemetry, a **pure-Kotlin `AchievementRules`** over SQL aggregates, and a **data-layer `ProgressSubmitter`** that is the only thing in the app that ever names a leaderboard or achievement ID.

Three findings change the shape of the plan and are each verified against the exact pinned artifacts rather than inferred:

1. **The fire-and-forget SDK forms cannot report failure, and D-10 requires failure detection.** `LeaderboardsClient.submitScore(String, long)`, `AchievementsClient.unlock(String)` and `setSteps(String, int)` all return `void` — verified by `javap` on the 22.0.0 AAR in this repo's Gradle cache. There is no callback, no `Task`, no exception. A submitter built on them can never set the "submissions pending" flag on a failed submission, which is half of D-10. The `*Immediate` variants (`submitScoreImmediate`, `unlockImmediate`, `setStepsImmediate`) each return a `Task`, which `Task.await()` turns into a throw-or-succeed the submitter can act on. **The seam's submission methods must therefore become `suspend fun …: Boolean`.** This is still fire-and-forget from `ResultsViewModel`'s point of view (D-13, PGSL-04) — the submitter runs on the application scope and the caller never awaits.

2. **Both key idempotency assumptions are documented facts, not hopes.** `submitScore`: *"The score is ignored if it is worse … than a previously submitted score for the same player."* `setSteps`: *"Sets an achievement to have **at least** the given number of steps completed. Calling this method while the achievement already has more steps than the provided value is a **no-op**."* Absolute steps (D-12) and the resubmit-everything backfill (D-10/D-11) are safe by the SDK's own contract. One guard is required: `numSteps` **must be greater than 0**, so a tier whose aggregate is 0 must be skipped, not sent.

3. **D-16 requires zero code, because v2 has no popup API.** `PlayGames` (verified by `javap`) exposes nine client getters and `getGamesClient` is not among them — `GamesClient.setGravityForPopups`/`setViewForPopups` still exist as classes in the AAR but are unreachable through the v2 entry point. The popup is entirely the Play Games app's business. The corollary is that PGSA-04 is **not verifiable by any JVM test**: the only code contribution is calling `unlock()`, and whether a popup appears depends on a Play Console project that does not exist and on the 20 icons being uploaded. It belongs on the `11-HUMAN-UAT.md` device checklist, exactly as Phase 11 D-04 handled sign-in.

**Primary recommendation:** land the migration and `AchievementRules` first (both fully JVM-testable, zero SDK), then `GamesIds` + the seam's `suspend`-returning submission methods, then `ProgressSubmitter`, and finally the two Settings rows — and treat the whole "does a real submission land" question as a device checklist item, not a phase blocker.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Deciding *which* achievements are satisfied | `:domain` (`AchievementRules`) | — | Pure function of aggregates; the highest-value JVM-testable slice. No Android, no PGS strings. |
| Storing per-session telemetry | Data / Room (`play_session`) | — | Relational, queried by SQL predicates; joins `recordResult`'s existing transaction. |
| Computing aggregates | Data / Room DAO (SQL) | — | ROADMAP note: Challenge reaches thousands of rows; never fold a row list in Kotlin. |
| Owning the dirty flag | Data / DataStore (`app_prefs`) | — | Scalar boolean beside the opt-in flags — a key-value tuple, not a table. |
| Orchestrating submission | Data (`ProgressSubmitter` `@Singleton`) | — | Owns dirty flag + aggregates + rules + seam calls; nothing in `ui/` sees an ID (D-13). |
| Mapping `AchievementId`/leaderboard → ID string | Platform (`platform/games/GamesIds.kt`) | Build (`buildConfigField`) | IDs are per-build-type build-config values (PLAT-06/D-17); `:domain` must not know them. |
| Talking to the PGS SDK | Platform (`PlayGamesService`) | — | Only `..platform..` may name `com.google.android.gms..` (`PlatformBoundaryRules`). |
| Resolving a foreground Activity | Platform (`ForegroundActivityProvider`) | — | Every `PlayGames.get*Client` takes an `Activity` — verified, no `Context` overload. |
| Launching the stock UI Intents | Platform (`PlayGamesService`) | — | `startActivityForResult` is mandatory; keeping it in platform keeps `Intent` out of `ui/` signatures. |
| Rendering the two Settings rows | UI (`SettingsScreen`) | UI (`SettingsViewModel`) | Stateless rows over the collected `gamesSignInState`; hidden unless `SignedIn` (D-15). |
| Counting assists / last-peg position | UI (`GameViewModel`) | `:domain` (`SolveTelemetry`) | The ViewModel is the only place that sees the assist funnel and the finishing board. |
| Creating leaderboards/achievements + icons | **Play Console (operator)** | — | No code path. External prerequisite; blocks device verification only. |

---

## Standard Stack

### Core — everything is already on the dependency graph

| Library | Version (catalog key) | Purpose | Why standard |
|---------|----------------------|---------|--------------|
| `com.google.android.gms:play-services-games-v2` | **22.0.0** (`playServicesGamesV2`) | `LeaderboardsClient`, `AchievementsClient`, stock UI Intents | Already `implementation` in `app/build.gradle.kts:348`; v1 APIs are being removed from the SDK. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | **1.11.0** (`version.ref = coroutines`) | `Task<T>.await()` | Already on the graph (`app/build.gradle.kts:354`); `PlayGamesService` already uses `kotlinx.coroutines.tasks.await`. |
| `androidx.room:room-runtime` / `-ktx` / `-compiler` | **2.8.4** (`room`) | `play_session` entity + DAO, `withTransaction` | Already wired; compiler under KSP. |
| `androidx.room:room-testing` | **2.8.4** | `MigrationTestHelper` | Already `testImplementation`; the harness is live and green (`MigrationTest.exportedVersion1SchemaOpens`). |
| `androidx.datastore:datastore-preferences` | **1.2.1** | The D-10 dirty flag | Already the home of `PlayGamesPreferences`. |

### Supporting (test-side, all already present)

| Library | Version | Use here |
|---------|---------|----------|
| Robolectric | 4.16 | `MigrationTest`, `ProgressRepositoryTest`-style in-memory Room tests |
| Truth | 1.4.4 | Assertions |
| Turbine | 1.2.1 | `SettingsUiState` emissions after the session flips to `SignedIn` |
| MockK | 1.14.2 | Only if a hand-written recording fake is not preferred — the codebase's convention is hand-written fakes (`FakeGamesService`, `FakePlayGamesOptInRepository`) |
| kotlinx-coroutines-test | 1.11.0 | `runTest` for the submitter |
| ArchUnit | 1.3.0 | `DomainPurityTest` / `PlatformBoundaryTest` already cover the new types automatically |

### Alternatives Considered

| Instead of | Could use | Tradeoff |
|------------|-----------|----------|
| `*Immediate` + `Task.await()` | Fire-and-forget `void` forms | Cheaper, but **failure is invisible** — D-10's "any failed submission sets the flag" becomes unimplementable. Recommended only if D-10 is narrowed to "not-signed-in" alone. |
| Single-row aggregate `@Query` with correlated subqueries | 8–12 separate scalar `@Query`s | One round trip vs. readability. Both are correct; the combined form matters most on the backfill path. |
| `startActivityForResult` inside `PlayGamesService` | `ActivityResultLauncher` in `SettingsRoute` (ARCHITECTURE sketch) | The launcher route forces `Intent` into a ViewModel/seam signature for zero benefit — PGS returns no result the app acts on. Keeps the existing `suspend fun showLeaderboard(id)` shape unchanged. |
| Sibling `GamesIdResolution.kt` in `build-logic` | Extending `AdIdResolution.kt` | `AdIdResolutionResult.Resolved` is a fixed 4-field data class with its own unit test; growing it to 31 fields would wreck both. A sibling returning `Map<String, String>` is the right shape for 27 keys. |
| `MAX(CAST(levelId AS INTEGER))` for the ladder | `ChallengeProgress.currentLevel` | The DataStore value is `highest solved + 1` and fails open to `1` on a corrupt read; SQL is the ROADMAP-mandated source. They must agree — see Pitfall 7. |

**Installation:** none. No `libs.versions.toml` change is required by this phase.

**Version verification:** the two SDK artifacts were verified *in place* rather than against a registry — `play-services-games-v2-22.0.0.aar` and `room-testing-android-2.8.4.aar` were unpacked from `~/.gradle/caches/modules-2/files-2.1/` and inspected with `javap`. The AAR's own class-file banner reads `com.google.android.gms:play-services-games-v2@@22.0.0`, compiled `07-29-2026`.

---

## Package Legitimacy Audit

**No external packages are installed by this phase.** Every artifact it uses is already declared in `gradle/libs.versions.toml` and was audited when it landed (Phase 6, plan 06-09 — see `06-RESEARCH.md § Package Legitimacy Audit`, which records that `slopcheck` has no Maven backend and the coordinates were verified by direct download from `dl.google.com` with AAR-content inspection instead).

| Package | Registry | Disposition |
|---------|----------|-------------|
| `com.google.android.gms:play-services-games-v2:22.0.0` | Google Maven | Already present; **re-verified this session** by unpacking the cached AAR and `javap`-ing `PlayGames`, `LeaderboardsClient`, `AchievementsClient`, `GamesSignInClient`, `GamesClientStatusCodes` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | Maven Central | Already present |
| `androidx.room:room-testing:2.8.4` | Google Maven | Already present; **re-verified** by `javap` on `MigrationTestHelper` |

**Packages removed due to slopcheck [SLOP] verdict:** none — no new packages.
**Packages flagged as suspicious [SUS]:** none.

`slopcheck` is not installed on this machine and has no Maven/Gradle backend regardless; since this phase adds zero coordinates, the gate is vacuous rather than skipped.

---

## Architecture Patterns

### System Architecture Diagram

```
                     ┌──────────────────────── GAMEPLAY ────────────────────────┐
  peg tap ──► GameViewModel.performJump ──► status == Won?
                     │  onUndo()/onHintRequested() ──► enterAssistFunnel
                     │        │ success path ──► undosUsed++ / hintsUsed++  (D-09)
                     │        └─ counters ride ResumeSnapshotDto (nullable, default 0)
                     ▼ yes
              freezeAndFinishIfTerminal
                     │  builds ScoreResult  +  SolveTelemetry(hints, undos, lastPegOnTarget)
                     │      lastPegOnTarget = nextBoard.singlePeg == nextBoard.spec.target
                     ▼
              _finished (Channel, CONFLATED)
                     │
             GameRoute LaunchedEffect ──► Results(levelId, stars, …, hintsUsed, undosUsed, lastPegOnTarget)
                     │                                   ▲ new nav args, defaulted
                     ▼
  ┌────────────────────────── RESULTS ──────────────────────────┐
  │ ResultsViewModel.recordSolvedPlayOnce()                     │
  │   guard: SavedStateHandle[RECORDED_KEY]  (process-death safe)│
  │       │                                                     │
  │       ├─► ProgressRepository.recordResult(mode, levelId,    │
  │       │        ScoreResult, SolveTelemetry, nowMs)          │
  │       │        └─ database.withTransaction {                │
  │       │              level_result  upsert (monotonic merge)  │
  │       │              play_session  insert (one row/solve)    │
  │       │           }                          ──► NewBests    │
  │       │                                                     │
  │       └─► progressSubmitter.onSolveRecorded()  ← fire &      │
  └──────────────────────────────│──────────────────  forget ───┘
                                 ▼
  ┌──────────────── data/ProgressSubmitter  @Singleton ────────────────┐
  │  runs on @ApplicationScope; never throws into the caller           │
  │                                                                    │
  │  session: StateFlow<GamesSignInState> ──collect──┐                 │
  │                                                  ▼                 │
  │        SignedIn?  ──no──► setPending(true); return  (D-10)         │
  │           │ yes                                                    │
  │           ▼                                                        │
  │   ProgressRepository.aggregate()  ── ONE SQL round trip ──►        │
  │        ProgressAggregate(highestChallengeLevel, challengeScore,    │
  │            totalStars, classicBests[5], grandTour, perfectionist,  │
  │            hintFreeSolve, undoFreeSolve, centerFinish, speedSolve) │
  │           │                                                        │
  │           ├─► leaderboard values ──► GamesService.submitScore ×≤7  │
  │           └─► :domain AchievementRules.evaluate(aggregate)         │
  │                     ──► Set<AchievementUnlock> + List<AchievementStep>
  │                     ──► GamesIds.of(id) ──► unlock / setAchievementSteps
  │                                                                    │
  │   every call returns Boolean; ANY false ──► setPending(true)       │
  │   all true ──► setPending(false)                                   │
  └───────────────────────────────│────────────────────────────────────┘
                                  ▼
  ┌────────── platform/games/PlayGamesService ──────────┐
  │ awaitForegroundActivity()  (10 s cap, null ⇒ false) │
  │ PlayGames.getLeaderboardsClient(activity)           │
  │      .submitScoreImmediate(id, score).await()       │
  │ PlayGames.getAchievementsClient(activity)           │
  │      .unlockImmediate(id).await()                   │
  │      .setStepsImmediate(id, steps).await()          │
  │      .getAchievementsIntent().await()               │
  │          └─► activity.startActivityForResult(…)     │
  └─────────────────────────────────────────────────────┘
                                  ▲
  Settings ─► "Leaderboards" / "Achievements" rows (SignedIn only, D-14/D-15)
              ─► SettingsViewModel ─► gamesService.showLeaderboard() / showAchievements()
```

### Component Responsibilities

| File | New/Modified | Responsibility |
|------|--------------|----------------|
| `domain/…/telemetry/SolveTelemetry.kt` | NEW | `data class SolveTelemetry(hintsUsed: Int, undosUsed: Int, lastPegOnTarget: Boolean)` |
| `domain/…/achievement/AchievementId.kt` | NEW | Enum of the 20 achievements — **no PGS strings** |
| `domain/…/achievement/AchievementRules.kt` | NEW | `evaluate(ProgressAggregate): AchievementOutcome` — pure |
| `domain/…/achievement/AchievementThresholds.kt` | NEW | 10/50/100, 25/100/250, 60 s, 5 boards, 3 stars |
| `domain/…/achievement/ProgressAggregate.kt` | NEW | Pure value type the DAO projection maps into |
| `app/…/data/db/PlaySessionEntity.kt` | NEW | 9 columns; `lastPegOnTarget: Boolean` → INTEGER |
| `app/…/data/db/PlaySessionDao.kt` | NEW | `insert`, plus the telemetry half of the aggregate query |
| `app/…/data/db/Migrations.kt` | NEW | `MIGRATION_1_2` — one `execSQL(CREATE TABLE …)` |
| `app/…/data/db/PegSolitaireDatabase.kt` | MOD | `DB_VERSION = 2`, `+ PlaySessionEntity::class`, `+ playSessionDao()` |
| `app/schemas/…/2.json` | NEW (generated) | Committed migration contract |
| `app/…/di/DatabaseModule.kt` | MOD | `.addMigrations(MIGRATION_1_2)`, `providePlaySessionDao` |
| `app/…/data/ProgressRepository.kt` | MOD | `recordResult(+ SolveTelemetry)`; `suspend fun aggregate(): ProgressAggregate` |
| `app/…/data/ProgressSubmitter.kt` | NEW | `@Singleton`; dirty flag, session observer, orchestration |
| `app/…/data/PlayGamesPreferences.kt` | MOD | `SUBMISSIONS_PENDING_KEY` |
| `app/…/platform/games/GamesService.kt` | MOD | Submission methods become `suspend … : Boolean`; `+ setAchievementSteps` |
| `app/…/platform/games/PlayGamesService.kt` | MOD | Real bodies for the five (now six) Phase-12 methods |
| `app/…/platform/games/NoOpGamesService.kt` | MOD | New signatures; returns `false` (nothing was submitted) |
| `app/…/platform/games/GamesIds.kt` | NEW | 27 `BuildConfig` accessors + `AchievementId`→string + leaderboard lookup |
| `app/…/platform/games/GamesConstants.kt` | MOD | Two `startActivityForResult` request codes |
| `build-logic/…/GamesIdResolution.kt` | NEW | Gradle-free `Map`-in/`Map`-out resolver + release failure message |
| `app/build.gradle.kts` | MOD | 27 `buildConfigField`s per build type (loop), `verifyReleaseGamesIds` |
| `local.properties.template` | MOD | 27 documented keys |
| `app/…/ui/game/GameViewModel.kt` | MOD | Counters, `lastPegOnTarget`, finish payload |
| `app/…/navigation/Routes.kt` | MOD | `Results` gains 3 defaulted args |
| `app/…/data/resume/ResumeSnapshotDto.kt` + `SnapshotMapper.kt` | MOD | Two nullable counters, defaulting to 0 |
| `app/…/ui/settings/SettingsScreen.kt` / `SettingsViewModel.kt` | MOD | Two rows in the `SignedIn` branch |

---

### Pattern 1: The seam's submission methods return `Boolean`, and that is what makes D-10 implementable

**What:** change `GamesService`'s three submission methods from `fun …: Unit` to `suspend fun …: Boolean`, where `true` means "the SDK accepted it".

**Why:** verified by `javap` against `play-services-games-v2-22.0.0.aar`:

```
public abstract void submitScore(java.lang.String, long);
public abstract Task<ScoreSubmissionData> submitScoreImmediate(java.lang.String, long);
public abstract void unlock(java.lang.String);
public abstract Task<java.lang.Void> unlockImmediate(java.lang.String);
public abstract void setSteps(java.lang.String, int);
public abstract Task<java.lang.Boolean> setStepsImmediate(java.lang.String, int);
```

The `void` forms cannot report anything. D-10 says the pending flag is set "by **any failed submission**" — with the `void` forms there is no such event to observe, and the flag would only ever be set by the signed-out branch. Google's own javadoc frames the choice exactly this way: use the fire-and-forget form *"if you don't need to know the status of the operation immediately"*; use `*Immediate` *"if you need … to have the status code delivered to your application."*

**The `Boolean` a `*Immediate` Task carries is not a success flag.** `setStepsImmediate` / `incrementImmediate` return `Task<Boolean>` where the payload means *"the achievement is now unlocked"*, and `submitScoreImmediate` returns `Task<ScoreSubmissionData>`. Success is "the Task completed without throwing", so the platform-side shape is:

```kotlin
private suspend fun <T> submitting(block: suspend (Activity) -> T): Boolean {
    val activity = awaitForegroundActivity() ?: return false
    return runCatching { block(activity) }
        .onFailure { if (it is CancellationException) throw it }
        .isSuccess
}
```

(Cancellation is rethrown before classification — the WR-04 rule `PlayGamesService.signIn` already follows.)

**Caller-side fire-and-forget is preserved (PGSL-04, D-13).** `ResultsViewModel` calls `progressSubmitter.onSolveRecorded()`, a **non-suspend** method whose body is `scope.launch { … }` on the injected `@ApplicationScope`. Nothing on the Results path awaits a network round trip; a submission that outlives the screen still completes.

**Anti-pattern:** making `ResultsViewModel` await the submitter inside `recordSolvedPlayOnce`'s existing `viewModelScope.launch`. That ties submission lifetime to the screen, and a back-navigation mid-submit cancels it — silently losing the dirty-flag write that was supposed to make it recoverable.

---

### Pattern 2: `recordSolvedPlayOnce` is the only submission trigger

`ResultsViewModel.recordSolvedPlayOnce()` (`ResultsViewModel.kt:311`) already has the guard the submission needs:

```kotlin
private fun recordSolvedPlayOnce() {
    if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return
    if (savedStateHandle.get<Boolean>(RECORDED_KEY) == true) return
    savedStateHandle[RECORDED_KEY] = true
    …
    viewModelScope.launch {
        val newBests = progressRepository.recordResult(mode, levelId, scoreResultFromArgs(), telemetryFromArgs(), now)
        _uiState.update { it.copy(newBests = newBests) }
        progressSubmitter.onSolveRecorded()   // ← non-suspend, launches on app scope
    }
}
```

`GameViewModel` is the wrong site and the reason is in the code: `performUndo()` sets `finishedEmitted = false` when the undone board returns to `Playing` (`GameViewModel.kt:1120`), so an undo/re-jump cycle re-emits `_finished` and would resubmit every time.

Do **not** add a second guard for the submitter — inheriting `RECORDED_KEY` is the point. It survives configuration change *and* process death, which no `LaunchedEffect` does.

---

### Pattern 3: `AchievementRules` is pure and speaks in enums, not IDs

```kotlin
// :domain — zero Android, zero PGS strings (DomainPurityTest is bytecode-based)
enum class AchievementId {
    FIRST_SOLVE, FIRST_THREE_STAR, CHALLENGE_LEVEL_5, SPEED_SOLVE,
    HINT_FREE, UNDO_FREE, CENTER_FINISH,
    LADDER_10, LADDER_50, LADDER_100,
    STARS_25, STARS_100, STARS_250,
    CLEARED_ENGLISH33, CLEARED_EUROPEAN37, CLEARED_ASYMMETRIC39,
    CLEARED_DIAMOND41, CLEARED_WIEGLEB45,
    GRAND_TOUR, PERFECTIONIST,
}

data class AchievementOutcome(
    val unlocked: Set<AchievementId>,
    /** Absolute step counts for the tiered achievements (D-12). Never zero — see below. */
    val steps: Map<AchievementId, Int>,
)

object AchievementRules {
    fun evaluate(aggregate: ProgressAggregate): AchievementOutcome { … }
}
```

`ProgressAggregate` is a pure `:domain` value type. `platform/games/GamesIds.kt` owns the `AchievementId → String` map. This layering is what makes D-13's "nothing in `ui/` touches an ID" true structurally rather than by convention, and it keeps the rules JVM-testable with a plain data-class fixture.

**Steps must never be reported as 0.** `setSteps`'s documented contract is *"numSteps … Must be greater than 0."* A brand-new player has `totalStars = 0` and `highestChallengeLevel = null`; emitting `setSteps(STARS_25, 0)` is an invalid call. `evaluate` must omit zero-valued entries from `steps` entirely.

**Steps above the tier's total are safe.** *"Once the achievement reaches the maximum number of steps, the achievement will automatically be unlocked, and any further mutation operations will be ignored."* So `setSteps(LADDER_10, 137)` on a 10-step achievement unlocks it and is thereafter inert — the tiers can all be driven from the same absolute aggregate with no clamping.

---

### Pattern 4: The dirty-marker sync, and the trap that makes it never run

```kotlin
@Singleton
class ProgressSubmitter @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val gamesService: GamesService,
    private val syncStore: SubmissionSyncStore,          // DataStore boolean
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val passMutex = Mutex()          // one pass at a time

    /** Called ONCE from MainActivity.onCreate, beside gamesService.bootstrapIfOptedIn(). */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            gamesService.session.collect { session ->
                if (session is GamesSignInState.SignedIn && syncStore.pending.first()) runPass()
            }
        }
    }

    /** Fire-and-forget from ResultsViewModel's point of view (PGSL-04). */
    fun onSolveRecorded() { scope.launch { runPass() } }

    private suspend fun runPass() = passMutex.withLock {
        if (gamesService.session.value !is GamesSignInState.SignedIn) {
            syncStore.setPending(true); return@withLock
        }
        val aggregate = progressRepository.aggregate()
        var allOk = true
        …submit leaderboards / unlock / setAchievementSteps, &&-ing into allOk…
        syncStore.setPending(!allOk)
    }
}
```

**The trap:** Hilt constructs a `@Singleton` lazily, on first injection. If `ProgressSubmitter` is injected only by `ResultsViewModel`, its `session` collector never exists until the player reaches a Results screen — so a player who signs in from Settings and closes the app gets no backfill at all, and the feature appears to work in every test while failing in production. **Give it an explicit `start()` called from `MainActivity.onCreate`,** next to `gamesService.bootstrapIfOptedIn()`, guarded by an `AtomicBoolean` exactly as `PlayGamesService.bootstrapped` is (`PlayGamesService.kt:114`).

**Serialise passes.** `onSolveRecorded()` and the session collector can fire together (solve a level while sign-in resolves). Two concurrent passes would double the SDK calls and race the flag write. `Mutex` mirrors `PlayGamesService.sdkMutex`.

**The dirty flag's fail-open direction.** Follow `PlayGamesPreferences`'s convention: `.catch { emit(emptyPreferences()) }` with `DEFAULT_SUBMISSIONS_PENDING = false`. A corrupt store then means "nothing pending", and the next solve sets it again — one solve's delay, never a crash. The opposite default (`true`) would be *more* recoverable (every call is idempotent, and an empty database issues no SDK calls) but contradicts D-10's "clean launches make no calls"; note the tradeoff and take `false`.

---

### Pattern 5: Stock UI Intents — `startActivityForResult` is mandatory, and it stays inside `platform/`

Verified in the 22.0.0 javadoc for **all three** getters (`getAllLeaderboardsIntent`, `getLeaderboardIntent`, `getAchievementsIntent`):

> *"Note that the Intent returned from the Task must be invoked with `Activity.startActivityForResult(Intent, int)`, so that the identity of the calling package can be established."*
> *"The returned Task can fail with a `RemoteException`."*

Keep the seam exactly as it is declared today (`suspend fun showLeaderboard(leaderboardId: String)` / `suspend fun showAchievements()`), and implement inside `PlayGamesService`:

```kotlin
override suspend fun showAchievements() {
    val activity = awaitForegroundActivity() ?: return
    runCatching {
        val intent = PlayGames.getAchievementsClient(activity).getAchievementsIntent().await()
        activity.startActivityForResult(intent, GamesConstants.ACHIEVEMENTS_REQUEST_CODE)
    }.onFailure { if (it is CancellationException) throw it }
    // silent no-op otherwise — the seam's existing KDoc already promises this
}
```

Why not the ARCHITECTURE sketch's `ActivityResultLauncher` in `SettingsRoute`: it requires the seam to hand back an `android.content.Intent`, which would put an Android framework type in a ViewModel signature for a call whose result nothing consumes. `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity` only bans `android.app.Activity`, so the launcher route is *legal* — it is simply more surface for no gain, and CONTEXT's discretion note ("the seam signature must not gain an Activity type either way") points the same direction.

Notes: `startActivityForResult` is deprecated on `ComponentActivity` but not removed, and this build sets no `allWarningsAsErrors` (verified in `build-logic/src/main/kotlin/android-app.gradle.kts` — there is no such flag), so it compiles with a warning. Request codes are magic numbers under detekt: put them in `GamesConstants` (Google's own samples use `9003` for achievements and `9004` for leaderboards). `MainActivity` overrides no `onActivityResult`, which is correct — nothing is consumed.

`showLeaderboard` per D-14 opens the **all-leaderboards picker**, so it should call `getAllLeaderboardsIntent()`. The seam's parameter is `leaderboardId: String`; the planner should either add a no-arg overload/branch or use `getLeaderboardIntent(id)` for a specific board. Given D-14 ("opens the all-leaderboards picker"), the cleanest change is to drop the parameter or add `showAllLeaderboards()` — flag it, since the current KDoc promises a per-id view.

---

### Pattern 6: The Room v1→v2 migration

The generated schema is the contract. `runMigrationsAndValidate` compares the migrated database against `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/2.json` and fails on any mismatch of column name, affinity, `NOT NULL`, default, primary key or index. **Do not hand-write the `CREATE TABLE`.** The reliable recipe:

1. Add `PlaySessionEntity`, bump `DB_VERSION = 2`, add it to `@Database(entities = …)`, add `playSessionDao()`.
2. Build once (`./gradlew :app:kspDebugKotlin`) — KSP writes `2.json` and the Room Gradle plugin's `copyRoomSchemas` copies it into `app/schemas/`.
3. Copy the new entity's `"createSql"` string verbatim out of `2.json`, replace `${TABLE_NAME}` with the literal table name, and paste it into `MIGRATION_1_2`.
4. Commit `2.json`.

Column-affinity facts that matter here (confirmed by the shape of the committed `1.json`): Kotlin `Int`/`Long`/**`Boolean`** all map to `INTEGER`, and non-null Kotlin properties become `NOT NULL`. `String` maps to `TEXT`. So `lastPegOnTarget: Boolean` is `` `lastPegOnTarget` INTEGER NOT NULL `` and SQL predicates must compare against `1`/`0`, not `true`/`false`.

`play_session` needs a surrogate key — one row per solved play, so `(mode, levelId)` is **not** unique here (unlike `level_result`). Use `@PrimaryKey(autoGenerate = true) val id: Long = 0`, which serialises as `` `id` INTEGER PRIMARY KEY AUTOINCREMENT ``… **careful:** Room emits `AUTOINCREMENT` only for `autoGenerate = true`, and the exported `createSql` will say so. Copying it verbatim (step 3) makes this a non-issue; writing it by hand is where it goes wrong.

Verified `MigrationTestHelper` surface in `room-testing 2.8.4` (via `javap`):

```
public SupportSQLiteDatabase createDatabase(String, int) throws IOException;
public SupportSQLiteDatabase runMigrationsAndValidate(String, int, boolean, Migration...);
```

The existing `MigrationTest` already uses the `(Instrumentation, Class)` constructor, which is the `SupportSQLiteDatabase`-flavoured pair above. The first real case:

```kotlin
@Test
fun version1ProgressSurvivesTheMigrationToVersion2() {
    helper.createDatabase(TEST_DB, SCHEMA_VERSION_1).apply {
        execSQL(
            "INSERT INTO level_result (mode, levelId, stars, bestTimeMs, bestScore, completedAt) " +
                "VALUES ('classic', 'english33', 3, 42000, 9001, 1700000000000)",
        )
        execSQL(
            "INSERT INTO resume_snapshot (mode, jsonBlob, updatedAt) VALUES ('challenge', '{}', 1700000000000)",
        )
        close()   // MUST close before migrating
    }

    val migrated = helper.runMigrationsAndValidate(TEST_DB, SCHEMA_VERSION_2, true, MIGRATION_1_2)

    migrated.query("SELECT stars, bestScore FROM level_result WHERE levelId = 'english33'").use { c ->
        assertThat(c.moveToFirst()).isTrue()
        assertThat(c.getInt(0)).isEqualTo(3)
        assertThat(c.getInt(1)).isEqualTo(9001)
    }
}
```

`validateDroppedTables = true` is the right third argument: it is the check that would catch an accidental `DROP TABLE`.

**The one thing this test does not prove is that the migration is registered.** `DatabaseModule.providePegSolitaireDatabase` builds the database with no `.addMigrations(...)` and, deliberately, no destructive fallback — so a forgotten registration is an `IllegalStateException` at runtime on an upgrading device while `MigrationTest` stays green. Add a second Robolectric test that seeds a v1 file with `MigrationTestHelper.createDatabase`, then opens it through **the production builder configuration** and reads a row back. That is the only assertion that covers the registration.

---

### Anti-Patterns to Avoid

- **Folding `observeResults()` in Kotlin for a counter.** Challenge reaches thousands of rows and the `Flow` re-maps all of them on every change. SQL only (ROADMAP note, ARCHITECTURE Scaling Priority 2).
- **Deriving the ladder from `ChallengeProgress`.** It stores *highest solved + 1* and fails open to `1`. See Pitfall 7.
- **A second "reported achievements" persisted store.** Rejected in discussion Q3 — absolute `setSteps` plus server-side idempotency is the single source of truth. A *process-lifetime, in-memory* de-dup set inside `ProgressSubmitter` is a different thing and is fine (see Pitfall 3).
- **Submitting from a `LaunchedEffect` in `ResultsRoute`.** Survives neither process death nor config change without inventing a second guard.
- **`android.util.Log`-ing an achievement id, player id or SDK message.** `PlayGamesService`'s KDoc records a grep gate on this; ASVS V7.
- **Writing an achievement or leaderboard ID literal into `GamesConstants`.** Its KDoc explicitly forbids exactly this and reserves ID resolution for this phase following `AdIds`.

---

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---------|-------------|-------------|-----|
| "Only submit if better" | A local high-score cache compared before each submit | `submitScore` | Documented: *"The score is ignored if it is worse … than a previously submitted score."* The client library additionally filters locally (PGS quality checklist §5.1). |
| "Don't re-award an achievement" | A persisted unlocked-set | `unlock` / `setSteps` | Server-side idempotent; `setSteps` is a documented no-op when current steps ≥ the value, and further mutations after max are ignored. |
| Offline submission queue | A Room table of pending submissions | Dirty flag + recompute from SQL | Aggregates are *recomputable* by construction (D-07 chose `SUM(bestScore)` for exactly this). A queue would be a second source of truth that can disagree with the database. |
| `Task<T>` → coroutine | `suspendCancellableCoroutine` | `kotlinx-coroutines-play-services` `Task.await()` | Already on the graph and already the pattern in `PlayGamesService`. |
| Migration `CREATE TABLE` SQL | Hand-written DDL | The generated `2.json` `createSql` | Any affinity/PK/AUTOINCREMENT discrepancy fails `runMigrationsAndValidate` — and would fail on a real device instead if the test were skipped. |
| Star banding for "Perfectionist" | Re-deriving each board's `minimumPegs` at query time | `stars >= 3` | `ScoreCalculator.stars` (verified, line 50) is `pegsRemaining <= minimumPegs → THREE_STARS`. `level_result.stars` already stores the max ever achieved, so "best is at the board's minimum-peg finish" **is** `stars == 3`. Diamond-41's floor of 2 is already baked in. |
| Popup positioning | Anything | Nothing | v2 exposes no popup API (§Pitfall 1). |

**Key insight:** every anti-cheat/idempotency concern in this phase is already solved server-side by Play Games. The app's only job is to compute a correct, recomputable number and hand it over; every local mirror of Google's state is a bug waiting to disagree.

---

## SQL Aggregates (Claude's Discretion, resolved)

**Challenge `levelId` is always decimal text.** `ResultsViewModel.recordSolvedPlayOnce` writes `val levelId = challengeLevel?.toString() ?: args.levelId`, and `mode` is `challenge` exactly when `challengeLevel != null` — so for `mode = 'challenge'` the column is always `Int.toString()`. `CAST(levelId AS INTEGER)` is therefore total and safe. (For `mode = 'classic'` it would be nonsense, which is why every ladder query is mode-filtered.)

Recommended: **one projection query per table**, so a backfill pass is two round trips rather than twelve.

```sql
-- LevelResultDao — returns a single row; every column nullable where SQL can yield NULL
SELECT
  (SELECT MAX(CAST(levelId AS INTEGER)) FROM level_result WHERE mode = :challenge)      AS highestChallengeLevel,
  (SELECT SUM(bestScore)                FROM level_result WHERE mode = :challenge)      AS challengeScoreTotal,
  (SELECT SUM(stars)                    FROM level_result)                              AS totalStars,
  (SELECT COUNT(*) FROM level_result WHERE mode = :classic AND levelId IN (:classicIds)) AS classicBoardsSolved,
  (SELECT COUNT(*) FROM level_result
     WHERE mode = :classic AND levelId IN (:classicIds) AND stars >= :threeStars)        AS classicBoardsPerfect,
  (SELECT COUNT(*) FROM level_result WHERE stars >= :threeStars)                         AS threeStarCount,
  (SELECT COUNT(*) FROM level_result)                                                    AS solvedLevelCount
```

```sql
-- PlaySessionDao — EXISTS(...) maps cleanly to Kotlin Boolean in Room
SELECT
  EXISTS(SELECT 1 FROM play_session WHERE hintsUsed = 0)                                AS anyHintFreeSolve,
  EXISTS(SELECT 1 FROM play_session WHERE undosUsed = 0)                                AS anyUndoFreeSolve,
  EXISTS(SELECT 1 FROM play_session
           WHERE mode = :classic AND levelId = :english33Id AND lastPegOnTarget = 1)    AS anyCenterFinish,
  EXISTS(SELECT 1 FROM play_session WHERE elapsedMs < :speedLimitMs)                    AS anySpeedSolve
```

Per-board bests for PGSL-03 reuse the existing `LevelResultDao.find(mode, levelId)` (one call per Classic id on the backfill path; on the live path only the board just played).

Derivations in `AchievementRules` (`:domain`, pure):

| Achievement | Predicate |
|-------------|-----------|
| First solve | `solvedLevelCount > 0` |
| First 3-star | `threeStarCount > 0` |
| Challenge level 5 | `highestChallengeLevel != null && highestChallengeLevel >= 5` |
| Speed solve | `anySpeedSolve` (`elapsedMs < 60_000`; D-04 says `elapsedSeconds < 60` and `elapsedMs == elapsedSeconds * 1000`) |
| Hint-free / Undo-free | `anyHintFreeSolve` / `anyUndoFreeSolve` |
| Center finish | `anyCenterFinish` |
| Cleared \<board\> ×5 | `classicBests[id] != null` |
| Grand tour | `classicBoardsSolved == 5` |
| Perfectionist | `classicBoardsPerfect == 5` |
| Ladder 10/50/100 | `setSteps(tier, highestChallengeLevel)` when `> 0` |
| Stars 25/100/250 | `setSteps(tier, totalStars)` when `> 0` |

Return types: `MAX(CAST(...))` and `SUM(...)` over an empty set are **`NULL`**, so the projection fields must be nullable. `SUM(bestScore)` must be **`Long?`**, not `Int?` — see Pitfall 6.

Nothing needs a `@Transaction` annotation: `ProgressRepository` already composes DAO calls inside `database.withTransaction { }` (`ProgressRepository.kt:150`), which is the room-ktx mechanism and works across any number of DAOs. The `play_session` insert simply joins `recordResultInTransaction`.

---

## Build-Config ID Plumbing (D-17), resolved

**Key list — 27 keys.** Naming that mirrors `AdIdKeys`' `SCREAMING_SNAKE` and is a valid Java identifier (BuildConfig field names are generated verbatim):

```
PGS_LB_CHALLENGE_LADDER        PGS_ACH_FIRST_SOLVE          PGS_ACH_LADDER_10
PGS_LB_CHALLENGE_SCORE         PGS_ACH_FIRST_THREE_STAR     PGS_ACH_LADDER_50
PGS_LB_CLASSIC_ENGLISH33       PGS_ACH_CHALLENGE_LEVEL_5    PGS_ACH_LADDER_100
PGS_LB_CLASSIC_EUROPEAN37      PGS_ACH_SPEED_SOLVE          PGS_ACH_STARS_25
PGS_LB_CLASSIC_ASYMMETRIC39    PGS_ACH_HINT_FREE            PGS_ACH_STARS_100
PGS_LB_CLASSIC_DIAMOND41       PGS_ACH_UNDO_FREE            PGS_ACH_STARS_250
PGS_LB_CLASSIC_WIEGLEB45       PGS_ACH_CENTER_FINISH        PGS_ACH_GRAND_TOUR
                               PGS_ACH_CLEARED_ENGLISH33    PGS_ACH_PERFECTIONIST
                               PGS_ACH_CLEARED_EUROPEAN37
                               PGS_ACH_CLEARED_ASYMMETRIC39
                               PGS_ACH_CLEARED_DIAMOND41
                               PGS_ACH_CLEARED_WIEGLEB45
```

**Add a sibling, do not extend `AdIdResolution.kt`.** `AdIdResolutionResult.Resolved` is a fixed 4-field data class with a dedicated `build-logic` unit test (`AdIdResolutionTest`), and `AdIdKeys.ALL` is a 4-element list the failure message iterates. Growing both to 31 entries would obscure the ad-ID contract the file exists to protect. A sibling `GamesIdResolution.kt` returning `Map<String, String>` keeps the same Gradle-free, `Map`-in/result-out shape (so it stays unit-testable in `build-logic`, which is the only place a release-failure contract *can* be proven — AGP 9.1.0 gives `:app` no `testReleaseUnitTest`).

**Debug must honour configured IDs — and this is the one place where copying `AdIdDefaults` would be wrong.** `AdIdResolution` hard-wires debug to Google's test units and deliberately ignores configuration, because a real ad unit on a development device causes a **non-appealable AdMob suspension** (DEF-06-01). No equivalent penalty exists for Play Games: there are **no published PGS "test" IDs**, the PGS project registers the debug-keystore SHA-1 *precisely so debug builds can sign in* (Phase 11 D-04 / the operator checklist), and pre-publication achievement unlocks and leaderboard scores are resettable per tester through the Play Games Management API (`achievements.reset`, `achievements.resetAll`, `scores.reset` — accessible only to whitelisted testers). Hard-wiring debug to a placeholder would make submission untestable on a device forever. So:

- **Debug:** environment variable → `local.properties` → a self-describing fallback (`"UNCONFIGURED-<KEY>"`). Never fails the build; a fresh clone still compiles and simply cannot submit.
- **Release:** identical fail-fast to the ad IDs — a `GradleException` naming each missing key at configuration time on a release invocation, plus an execution-time `verifyReleaseGamesIds` task hooked to `assembleRelease`/`bundleRelease` (the existing `verifyReleaseAdIds` is the template; keep them as two tasks so each failure message names its own domain).

Emit the fields in a loop rather than 27 hand-written lines per build type:

```kotlin
// app/build.gradle.kts, inside buildTypes { debug { … } } and { release { … } }
gamesIds.forEach { (key, value) -> buildConfigField("String", key, "\"$value\"") }
```

`GamesIds.kt` then exposes them behind nested objects and two lookup functions, so the enum→string mapping is exhaustive at compile time:

```kotlin
object GamesIds {
    object Leaderboards {
        val challengeLadder: String get() = BuildConfig.PGS_LB_CHALLENGE_LADDER
        …
        fun forClassic(levelId: String): String? = when (levelId) {
            ClassicLevelIds.ENGLISH33 -> classicEnglish33
            …
            else -> null
        }
    }
    fun of(id: AchievementId): String = when (id) { … }   // exhaustive; no else
}
```

`adIdLiteralCheck` scans for `ca-app-pub-` only, so PGS IDs do not trip it. Consider a sibling gate later; not required by any requirement in this phase.

---

## Runtime State Inventory

This phase changes an on-disk schema, so the inventory is mandatory.

| Category | Items found | Action required |
|----------|-------------|------------------|
| **Stored data** | `peg_solitaire.db` at **v1** on every installed device: `level_result` (mode, levelId, stars, bestTimeMs, bestScore, completedAt) and `resume_snapshot` (mode, jsonBlob, updatedAt). The published listing is at versionCode 2, so upgrading installs genuinely exist. | **Code edit only** — a pure `CREATE TABLE` migration; no existing row is read, written or dropped. No data migration. Proven by the `MigrationTest` case in §Pattern 6. |
| **Stored data (DataStore)** | `app_prefs`: `onboarding_seen`, `sound_on`, `haptics_on`, `hints_enabled`, `challenge_current_level`, `play_games_prompt_answered`, `play_games_opted_in`. | **Code edit only** — one new key added (`play_games_submissions_pending`). Additive; existing keys untouched; DataStore has no schema to migrate. |
| **Stored data (blob)** | `resume_snapshot.jsonBlob` = serialized `ResumeSnapshotDto`. Old blobs will lack the two new counters. | **Code edit** — add them as `Int? = null`, and `SnapshotMapper` resolves absence to **0**, not to the free-allowance default. The direction is deliberate and opposite to `freeHintsRemaining`: a missing *allowance* is a generosity (fail open to full), a missing *assist counter* is a claim of purity (fail closed to 0, i.e. "no assists recorded"). `Json { ignoreUnknownKeys = true }` already covers the forward direction. |
| **Live service config** | **The Play Console PGS project does not exist yet.** 7 leaderboards, 20 achievements, 20 × 512×512 icons, and per-leaderboard score format + ordering all live in the Console, not in git. Their generated IDs are the only source for the 27 build-config keys. Leaderboard **ordering (larger/smaller is better) is fixed after publication and cannot be changed** — all seven here are larger-is-better, Numeric format, 0 decimal places. | **Operator / manual.** Blocks device verification, not code. Add to the operator checklist. |
| **OS-registered state** | None — no scheduled tasks, no launchers, no system services. Verified: `AndroidManifest.xml` declares one Activity, one `<meta-data>` pair, and a `tools:node="remove"` provider entry. | None. |
| **Secrets / env vars** | `local.properties` and CI secrets currently hold 4 keys (`ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID`, `PGS_PROJECT_ID`). | **Manual** — 27 new keys added to `local.properties.template` and to CI secrets, per D-17. Existing keys unchanged. |
| **Build artifacts** | `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json` (committed). `app/build/generated/.../BuildConfig.java`. | **Generated + commit.** `2.json` must be generated by a build and committed; `mergeDebugAssets dependsOn copyRoomSchemas` is already wired (`app/build.gradle.kts:257`) so Robolectric can see it. `BuildConfig` regenerates automatically. |

**The canonical question — after every file in the repo is updated, what still holds old state?** Only two things: a player's v1 database file (handled by the migration, proven by test) and the Play Console project (which does not exist yet, so there is nothing stale to reconcile — only something to create).

---

## Common Pitfalls

### Pitfall 1: Expecting to control the unlock popup — there is no API in v2
**What goes wrong:** a plan task says "verify no `setGravityForPopups` call exists" or "call `setViewForPopups` so the popup renders".
**Why it happens:** every v1-era answer on the web says popups need `Games.getGamesClient(activity, account).setViewForPopups(...)`. That API belongs to `play-services-games` **v1**, which this project does not depend on.
**The fact:** `javap com.google.android.gms.games.PlayGames` on the pinned 22.0.0 AAR lists nine getters — `getAchievementsClient`, `getEventsClient`, `getGamesSignInClient`, `getRecallClient`, `getLeaderboardsClient`, `getPlayersClient`, `getPlayerStatsClient`, `getGameStatsClient`, `getSnapshotsClient` — and **no `getGamesClient`**. The `GamesClient` interface still ships in the AAR (with `setGravityForPopups`/`setViewForPopups`), but there is no supported way to obtain one. D-16 costs zero lines.
**Warning sign:** any task that mentions a popup API. Delete it; replace it with a `11-HUMAN-UAT.md` device check.

### Pitfall 2: Building the submitter on the `void` SDK forms, then discovering D-10 is unimplementable
**What goes wrong:** `submitScore`/`unlock`/`setSteps` return `void`. Failures are invisible, so the dirty flag can only ever be set by the signed-out branch, and a submission that fails while signed in (offline, `RemoteException`, misconfigured project) is lost forever with no retry.
**How to avoid:** use `submitScoreImmediate` / `unlockImmediate` / `setStepsImmediate` + `Task.await()`, and make the seam's methods `suspend … : Boolean` (§Pattern 1).
**Warning sign:** a `GamesService` signature in the plan that still returns `Unit`.

### Pitfall 3: ~29 SDK calls on every solved level
**What goes wrong:** a naive pass issues 3 leaderboard submits + 6 `setSteps` + up to 20 `unlock`s on **every** solve. Google's quality checklist (§3.4, §5.2, §5.3) explicitly asks for calls at critical transitions with batching, and the Cloud project carries a daily quota.
**How to avoid:** (a) gate the leaderboard submits on `NewBests.any` for the live path (ARCHITECTURE Pattern 5 — a replay that improved nothing has nothing to submit); (b) hold a **process-lifetime, in-memory** `Set<AchievementId>` of what this process has already reported, and diff against it so a normal session issues 0–3 calls after the first solve. Losing that set (process death) costs at most one redundant idempotent call — it is a de-dup cache, not a second source of truth, and so does not reopen discussion Q3. The dirty-flag backfill deliberately bypasses the diff and resubmits everything.
**Warning sign:** the submitter has no notion of "already reported this process".

### Pitfall 4: `setSteps(id, 0)` on a fresh player
**What goes wrong:** `numSteps` is documented as *"Must be greater than 0."* A new install has `totalStars = 0` and `highestChallengeLevel = null`; the tiered achievements would be sent zero.
**How to avoid:** `AchievementRules.evaluate` omits zero/absent step values from the returned map entirely.

### Pitfall 5: The `@Singleton` submitter that is never constructed
**What goes wrong:** Hilt instantiates a `@Singleton` on first injection. Injected only by `ResultsViewModel`, the `session` collector does not exist until the player finishes a level — so the backfill never runs for the player it was written for.
**How to avoid:** explicit `ProgressSubmitter.start()` from `MainActivity.onCreate`, `AtomicBoolean`-guarded (§Pattern 4). Test it: a JVM test can assert the collector reacts, but only a `MainActivity` test proves it is started.

### Pitfall 6: `SUM(bestScore)` overflowing `Int`
**What goes wrong:** `bestScore` is `Int`. `ScoreCalculator.score` is `(pegsCleared × 100 + max(0, 5000 − 10×seconds)) × levelMultiplier`, so a single Challenge level can score in the tens of thousands. A few tens of thousands of levels overflow `Int.MAX_VALUE`, and a wrapped negative would be **submitted to the leaderboard**.
**How to avoid:** the DAO field and the `ProgressAggregate` property are `Long?`; `submitScore` already takes a `long`. `MAX_CHALLENGE_LEVEL` bounds the ladder, so `Long` is comfortably sufficient.

### Pitfall 7: `ChallengeProgress` and the SQL ladder disagreeing
**What goes wrong:** `ChallengeProgress.advancePast(n)` stores `max(current, n + 1)` — *next* level, not highest solved — and fails open to `1` on a corrupt read. `MAX(CAST(levelId AS INTEGER))` over `level_result WHERE mode='challenge'` is highest **solved**. They differ by exactly one, and by more if a `recordResult` write failed (it is best-effort via `storageFailSafe`) while `advancePast` succeeded.
**How to avoid:** submit the **SQL** value (the ROADMAP-mandated source; it is the one that matches what the player actually banked). Do not "reconcile" them; document the off-by-one relationship in a comment so a future reader does not treat the difference as a bug.

### Pitfall 8: Submitting while the session is `Unknown`
**What goes wrong:** `GamesSignInState.Unknown` means "the first check has not answered". A submitter that treats `!= SignedIn` as "offline" sets the dirty flag on every solve during the launch window, and one that treats `Unknown` as "probably fine" issues calls the SDK drops.
**How to avoid:** `Unknown` takes the same branch as `SignedOut` — set the flag, submit nothing. The subsequent `SignedIn` emission triggers the backfill, so nothing is lost. This mirrors `SettingsScreen.PlayGamesSection`'s early return and Phase 9's `rewardedOfferable`-`Unknown` rule.

### Pitfall 9: A hand-written `CREATE TABLE` that `runMigrationsAndValidate` rejects
**What goes wrong:** a missing `NOT NULL`, a `BOOLEAN` where Room expects `INTEGER`, a missing `AUTOINCREMENT`, or a differently-ordered PK. The test fails with a schema diff that is tedious to read; worse, skipping the test ships a `IllegalStateException: Migration didn't properly handle` to devices.
**How to avoid:** copy the generated `2.json` `createSql` verbatim (§Pattern 6, step 3).

### Pitfall 10: Forgetting `.addMigrations(MIGRATION_1_2)` in `DatabaseModule`
**What goes wrong:** `MigrationTest` passes (it constructs the `Migration` object directly), the app compiles, and an **upgrading device** crashes on first database open — `DatabaseModule`'s KDoc records that the destructive fallback is deliberately absent, so there is no silent path.
**How to avoid:** the second Robolectric test in §Pattern 6 that opens a seeded v1 file through the production builder configuration.

### Pitfall 11: Play Console ordering (carried from PITFALLS.md, unchanged and still unresolved)
The OAuth client must be created **through Play Console**, not directly in Google Cloud, or it will not be associated with the leaderboards and achievements; the consent screen must be published before app publication; and the account that created the game is **not** a tester by default. The failure mode (`ERROR_NOT_AUTHORIZED`, or a silent `isAuthenticated() == false`) is indistinguishable from a code bug. None of this is code; all of it belongs on the operator checklist.

### Pitfall 12: ProGuard/R8 for `play-services-games-v2`
The 22.0.0 AAR ships **no `proguard.txt` of its own**; its SafeParcelable/`@Keep`/DynamiteApi reflection is covered only transitively by `play-services-basement`'s consumer rules (recorded as assumption A1 in `libs.versions.toml`). Phase 11 added the first real PGS calls; this phase adds `LeaderboardsClient`, `AchievementsClient` and two `Intent`-returning calls that cross the Dynamite boundary. **Re-run a minified release build** (`./gradlew :app:assembleRelease` — note it needs one warm online run for `compose-group-mapping`, D-18) and check `app/build/outputs/mapping/release/missing_rules.txt` before this phase is called done. `app/proguard-rules.pro` currently carries exactly one `-dontwarn`; any new rule must carry a comment explaining why, and must spell out `{ <init>(); }` for reflectively-instantiated classes because AGP 9.1.0 runs R8 in strict full mode.

### Pitfall 13: detekt `MagicNumber` on everything this phase introduces
`ignoreNumbers` is `[-1, 0, 1, 2]`. The tier thresholds (10, 50, 100, 25, 250), `60_000`, `3` (three stars), `5` (classic boards) and the two request codes are all violations unless declared as `const val` in a scoped object. `ignoreConstantDeclaration`/`ignorePropertyDeclaration` are `true`, so the constants object itself is exempt. `LongParameterList` will *not* fire on a 9-property `PlaySessionEntity` — it ignores data classes, and `ResumeSnapshotDto` already carries 13 properties and passes. `ReturnCount` (default max 2) has bitten this codebase before (`GameViewModel` and `ResultsViewModel.claimWinOnce` both carry in-source notes about it) — write the submitter's branchy logic as a `when` expression, not a chain of early returns.

### Pitfall 14: `min` API and the games AAR
The games AAR declares `minSdkVersion 24`; the app's `minSdk` is 24. No constraint. (Verified from the AAR's `AndroidManifest.xml` and `build-logic/src/main/kotlin/android-app.gradle.kts:29`.)

---

## Code Examples

### Verified SDK surface (`javap`, `play-services-games-v2-22.0.0.aar`)

```java
// com.google.android.gms.games.LeaderboardsClient
Task<Intent> getAllLeaderboardsIntent();
Task<Intent> getLeaderboardIntent(String leaderboardId);
void         submitScore(String leaderboardId, long score);
Task<ScoreSubmissionData> submitScoreImmediate(String leaderboardId, long score);

// com.google.android.gms.games.AchievementsClient
Task<Intent>  getAchievementsIntent();
void          unlock(String id);
Task<Void>    unlockImmediate(String id);
void          setSteps(String id, int numSteps);
Task<Boolean> setStepsImmediate(String id, int numSteps);   // Boolean = "is now unlocked"
void          increment(String id, int numSteps);

// com.google.android.gms.games.PlayGames  — every getter takes an Activity; no Context overload
static LeaderboardsClient getLeaderboardsClient(android.app.Activity);
static AchievementsClient getAchievementsClient(android.app.Activity);
// (no getGamesClient — see Pitfall 1)
```

### `PlayGamesService` submission body

```kotlin
// Source: shape verified against this AAR's javadoc + the file's existing signIn() conventions
override suspend fun submitScore(leaderboardId: String, score: Long): Boolean =
    submitting { activity ->
        PlayGames.getLeaderboardsClient(activity).submitScoreImmediate(leaderboardId, score).await()
    }

override suspend fun unlockAchievement(achievementId: String): Boolean =
    submitting { activity ->
        PlayGames.getAchievementsClient(activity).unlockImmediate(achievementId).await()
    }

/** Absolute steps (D-12). The SDK no-ops when current steps already exceed [steps]. */
override suspend fun setAchievementSteps(achievementId: String, steps: Int): Boolean =
    submitting { activity ->
        PlayGames.getAchievementsClient(activity).setStepsImmediate(achievementId, steps).await()
    }

/**
 * `false` for "nothing was submitted" — including a backgrounded app with no Activity
 * within [GamesConstants.ACTIVITY_WAIT_MS]. The caller's answer to `false` is the D-10
 * dirty flag, never an error shown to the player (PGSL-04).
 */
private suspend fun <T> submitting(block: suspend (Activity) -> T): Boolean {
    val activity = awaitForegroundActivity() ?: return false
    return runCatching { block(activity) }
        .onFailure { if (it is CancellationException) throw it }
        .isSuccess
}
```

### `MIGRATION_1_2` (skeleton — the SQL string is copied from the generated `2.json`)

```kotlin
// app/src/main/java/com/app/pegsolitaire/data/db/Migrations.kt
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Copied VERBATIM from app/schemas/<db>/2.json → entities[play_session].createSql,
        // with ${TABLE_NAME} replaced by the literal name. Do not retype this by hand:
        // runMigrationsAndValidate compares it against that exact string's effect.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `play_session` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`mode` TEXT NOT NULL, `levelId` TEXT NOT NULL, `stars` INTEGER NOT NULL, " +
                "`score` INTEGER NOT NULL, `elapsedMs` INTEGER NOT NULL, " +
                "`hintsUsed` INTEGER NOT NULL, `undosUsed` INTEGER NOT NULL, " +
                "`lastPegOnTarget` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL)",
        )
    }
}
```

### `lastPegOnTarget`, computed in `GameViewModel.freezeAndFinishIfTerminal`

```kotlin
// A solved board has exactly one peg. The target comes from the FINISHING board's own
// spec (Boards.English33.target == Position(row = 3, col = 3)) — never from a catalog
// lookup by levelId, which would be wrong for a resumed game whose spec is rebuilt
// structurally from the persisted blob (the same reasoning as minimumPegs above).
val target = nextBoard.spec.target
val lastPegOnTarget =
    pegsRemaining == PEGS_REMAINING_SOLVED &&
        target != null &&
        nextBoard.hasPeg(target)
```

D-01 restricts the *achievement* to English-33; that restriction belongs in `AchievementRules`/the SQL predicate (`mode = 'classic' AND levelId = 'english33'`), not here. Recording the general fact keeps `play_session` honest and makes a future "finish on the start hole" achievement a query change rather than a migration.

---

## State of the Art

| Old approach | Current approach | When changed | Impact here |
|--------------|------------------|--------------|-------------|
| `Games.getGamesClient(activity, account)` + `GoogleSignIn` | `PlayGames.get*Client(activity)`, automatic sign-in | PGS v2 (v1 APIs dropped from the SDK starting May 2026) | Every v1 tutorial's popup/positioning advice is inapplicable (Pitfall 1). |
| `setViewForPopups` / `setGravityForPopups` required for popups | Popups are entirely SDK-managed; no API is reachable | v2 | D-16 is free. |
| `increment(id, delta)` for progression | `setSteps(id, absolute)` | Both have always existed; the *choice* is D-12's | Idempotent under the resubmit-everything backfill. |
| KAPT for Room/Hilt | KSP | Kotlin 2.x | Already the case; nothing new. |
| `androidTest` `MigrationTestHelper` | JVM-side under Robolectric, schemas on the **debug** assets source set | Already solved in Phase 5 | Reuse it; do not move `schemas/` to the `test` source set (the in-source note at `app/build.gradle.kts:239-251` records that this does not work). |

**Deprecated/outdated:**
- `Activity.startActivityForResult` — deprecated on `ComponentActivity`, still required by PGS and still functional. No `allWarningsAsErrors` in this build.
- `incrementAchievement` on the seam — D-12 makes it dead. Removing it is cleaner than leaving an unused method the next reader must reason about; either is sanctioned by D-12.

---

## Assumptions Log

| # | Claim | Section | Risk if wrong |
|---|-------|---------|---------------|
| A1 | Calling `submitScore`/`unlock`/`setSteps` while signed out produces a **failed Task** (for the `*Immediate` forms) rather than a synchronous throw or a silently-successful Task. Google documents no failure taxonomy; the community reports "silent failure". `ProgressSubmitter` guards on `session != SignedIn` anyway, so this only matters for a session that flips between the guard and the call. | Pattern 1, Pitfall 8 | Low. Worst case the flag is set (or not set) one solve late; the next pass self-heals. |
| A2 | The stock unlock popup appears automatically on `unlock()` with a v2 sign-in, given (a) a signed-in player, (b) a foreground Activity, and (c) **icons uploaded in Play Console**. The icon dependency is community-reported, not documented. | PGSA-04, Pitfall 1 | Medium — PGSA-04's acceptance depends on it. But there is no alternative implementation to choose: v2 exposes no popup API. Verification is a device checklist item. |
| A3 | Google publishes **no test leaderboard/achievement IDs** analogous to AdMob's test ad units. Extensive doc reading found none; the documented testing story is tester whitelisting + the Management API reset endpoints. | Build-Config ID Plumbing | Low — if test IDs turn out to exist, the debug branch becomes a hard-wire like `AdIdDefaults`; a one-file change. |
| A4 | No hard per-player call quota is published for `submitScore`/`unlock`/`setSteps`; the constraint is a per-Cloud-project daily quota plus the qualitative quality-checklist guidance. | Pitfall 3 | Low — the de-dup recommendation is conservative regardless. |
| A5 | `MAX_CHALLENGE_LEVEL` × the per-level score fits comfortably in `Long` for the PGSL-02 leaderboard. Not computed exactly this session. | Pitfall 6 | Very low. |
| A6 | The 7 leaderboards will be configured **Numeric, 0 decimals, larger-is-better**. Ordering is **immutable after publication**, so this must be right the first time. | Runtime State Inventory | Medium-high operationally — a wrong ordering on the ladder board cannot be fixed without deleting and recreating the leaderboard (and losing its scores). Put it explicitly on the operator checklist. |
| A7 | The 20-achievement roster fits Play Games' budget. **Verified constraints:** max 400 achievements per game lifetime, **max 2,000 total points across all achievements**, max 200 points each, values must be **multiples of 5**. 20 achievements ⇒ an average budget of 100 points each; Google advises reserving headroom for future updates. This directly constrains D-05's "point values at the planner's discretion". | PGSA-01 | Low if respected; the Console rejects an over-budget set at creation time. |

---

## Open Questions (RESOLVED)

1. **Does `showLeaderboard(leaderboardId)` keep its parameter?**
   - What we know: D-14 says the row opens the **all-leaderboards picker**; the seam's declared method takes an id and its KDoc promises a per-id view; `getAllLeaderboardsIntent()` takes no argument.
   - What's unclear: whether to drop the parameter, add a `showAllLeaderboards()`, or keep the id and pass it to `getLeaderboardIntent`.
   - Recommendation: replace with a no-arg `showLeaderboards()` (and update the KDoc). Only one call site exists, D-14 fixes the behaviour, and a parameter no caller can meaningfully supply is worse than no parameter. Per-board deep links are the deferred v2 social work.

2. **Does `incrementAchievement` stay on the seam?**
   - D-12 explicitly leaves this to the planner. Recommendation: **remove it.** Nothing calls it, `setAchievementSteps` supersedes it, and the seam's KDoc already states the method set is "deliberately minimal". Removing it is a two-file change (`NoOpGamesService`, `FakeGamesService`) at a moment when all three implementations are being touched anyway.

3. **How does `SolveTelemetry` cross the nav boundary?**
   - Options: three defaulted scalar args on `Results` (mirrors how `challengeLevel` was added), or a nested `@Serializable` payload.
   - Recommendation: three defaulted scalars (`hintsUsed: Int = 0`, `undosUsed: Int = 0`, `lastPegOnTarget: Boolean = false`). `Results` is already a flat scalar route and `ResultsViewModel` already treats args as untrusted; a nested type in a type-safe nav route needs a custom `NavType` in Navigation 2.9 for anything non-primitive, which is real friction for no benefit. Mirror the existing clamp discipline: `coerceAtLeast(0)` the two counters at the ViewModel boundary.

4. **Is `play_session` written for non-solved finishes?**
   - CONTEXT is explicit: "one row per **solved** play", and the insert rides inside `recordSolvedPlayOnce`, which returns early unless `pegsRemaining == 1`. The deferred "should Stuck finishes reach Results" todo stays independent. No question for this phase — noted so a planner does not reopen it.

5. **Can PGSA-04 be closed at all this milestone?**
   - No JVM test can observe a Google-rendered popup. Recommendation: treat the code contribution (call `unlock`, call no popup API — the latter being structurally guaranteed) as complete, and record PGSA-04's runtime confirmation as a `11-HUMAN-UAT.md` item alongside the Phase 11 sign-in checks, with the same `[UNVERIFIED — D-04]` in-source marker convention `GamesConstants` and `PlayGamesService` already use.

---

## Environment Availability

| Dependency | Required by | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| JDK 17 | Gradle toolchain | ✓ | 17.0.15 (Homebrew) | — |
| Gradle wrapper | Build | ✓ | 9.5.1 | — |
| Android SDK (compileSdk 36) | `:app` | ✓ | `~/Library/Android/sdk` present (`ANDROID_HOME` unset; `local.properties` `sdk.dir` is how AGP finds it) | — |
| `adb` | Device work | ✓ | on PATH | — |
| `play-services-games-v2:22.0.0` AAR | Compilation | ✓ | cached, inspected this session | — |
| `room-testing:2.8.4` AAR | `MigrationTest` | ✓ | cached, inspected this session | — |
| Robolectric 4.16 | JVM Android tests | ✓ | catalog-pinned | — |
| **Play Console PGS project** | Real submission, real IDs, popup, stock UI | **✗** | — | JVM tests against a recording fake `GamesService` + a device checklist (Phase 11 D-04 precedent) |
| **7 leaderboards + 20 achievements + 20 icons in Console** | PGSL-01/02/03/06, PGSA-01/04/05 runtime | **✗** | — | Same |
| Emulator / device with Play Games | PGSA-04 popup, stock UI | **✗ (this session)** | — | `11-HUMAN-UAT.md` |
| `slopcheck` | Package audit | ✗ | — | N/A — no new packages, and it has no Maven backend |
| Network for `compose-group-mapping` | `assembleRelease` (Pitfall 12's R8 check) | ✓ assumed | — | One warm online run puts it in the Gradle cache (D-18) |

**Missing dependencies with no fallback:** none that block code, planning or the JVM suite.
**Missing dependencies with fallback:** the entire Play Console surface. Every requirement in this phase is *implementable* and *unit-testable* without it; none is *runtime-verifiable* with it absent. The plan must say so out loud rather than quietly producing green checkmarks.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for Android-runtime JVM tests; ArchUnit 1.3.0 for boundary gates |
| Config file | None (JUnit4 has none); Robolectric SDK pinned per class via `@Config(sdk = [34])`; detekt at `config/detekt/detekt.yml` |
| Modules | `:domain` (pure JVM, no Android) and `:app` (`testDebugUnitTest`; **there is no `androidTest` source set**, and AGP 9.1.0 provides no `testReleaseUnitTest`) |
| Quick run | `./gradlew :domain:test --tests "*AchievementRules*"` · `./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"` |
| Full suite | `./gradlew :domain:test :app:testDebugUnitTest` |
| Phase gate | `./gradlew check` — adds detekt, ktlintCheck, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`, **and `:domain:depthSweep`, which STATE.md records at ~11 minutes** |

Existing suite size: 80 test classes across `:app` + `:domain` (25 in `:domain`). Note that the checked-in build reports are stale (2 classes), so no wall-clock figure for the full suite could be measured this session — the reliable published number is the ~11-minute `depthSweep` that `check` depends on.

### Phase Requirements → Test Map

| Req | Behavior | Type | Automated command | File exists? |
|-----|----------|------|-------------------|--------------|
| PGSA-06 | v1 rows survive the migration; `play_session` is created | integration (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"` | ❌ Wave 0 (case; class exists) |
| PGSA-06 | The migration is **registered** in `DatabaseModule` | integration | same class | ❌ Wave 0 |
| PGSA-02/03, PGSL-01/02 | `AchievementRules` yields the right unlocks/steps per aggregate; never emits a 0 step | unit (pure JVM) | `./gradlew :domain:test --tests "*AchievementRulesTest*"` | ❌ Wave 0 |
| PGSA-02 | `SolveTelemetry` / `AchievementId` / `AchievementThresholds` carry no `android.*`/`androidx.*` | arch | `./gradlew :app:testDebugUnitTest --tests "*DomainPurityTest*"` | ✅ (auto-covers new types) |
| PGSL-01/02/03 | Aggregates: ladder MAX-cast, `SUM(bestScore)` as `Long`, star total, Grand tour, Perfectionist | integration (in-memory Room) | `./gradlew :app:testDebugUnitTest --tests "*ProgressAggregateTest*"` | ❌ Wave 0 |
| PGSA-02 | `play_session` predicates: hint-free, undo-free, center, speed | integration | same | ❌ Wave 0 |
| PGSL-04 | A throwing/`false`-returning `GamesService` never fails `recordResult` and never alters stored bests | unit | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterTest*"` | ❌ Wave 0 |
| PGSL-05 | Dirty flag set when signed out; a `SignedIn` emission triggers exactly one resubmit pass; flag cleared only on all-success | unit | same | ❌ Wave 0 |
| PGSL-05 | Two concurrent passes do not double-submit (Mutex) | unit | same | ❌ Wave 0 |
| PGSL-01/02/03 | `ResultsViewModel` calls the submitter exactly once, after `recordResult`, and not for a stuck finish | unit | `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest*"` | ✅ exists, ❌ cases |
| PGSA-02/D-09 | Counters increment on the assist funnel's success path only; a declined offer counts nothing; restart resets | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModel*"` | ✅ exists, ❌ cases |
| D-09 | Counters round-trip the snapshot; an old blob reads 0 | unit | `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapper*"` | ✅ exists, ❌ cases |
| PGSL-06/PGSA-05, D-14/D-15 | Both rows present when `SignedIn`, absent when `SignedOut`/`Unknown`; taps reach the seam | Robolectric Compose | `./gradlew :app:testDebugUnitTest --tests "*SettingsPlayGamesTest*"` | ✅ exists, ❌ cases |
| PLAT-03/PLAT-04 | No gms type escapes `..platform..`; no `..data..`/`..ui..` class touches an Activity | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*"` | ✅ (auto) |
| PLAT-06/D-17 | Release fails on any missing games ID; debug never fails | unit (build-logic) | `./gradlew :build-logic:test --tests "*GamesIdResolutionTest*"` | ❌ Wave 0 |
| PLAT-06/D-17 | `GamesIds` reads the right generated fields | unit | `./gradlew :app:testDebugUnitTest --tests "*BuildConfigIdsTest*"` | ✅ exists, ❌ cases |
| **PGSA-04** | The unlock popup appears | **manual only** | — device + Play Console project | `11-HUMAN-UAT.md` |
| **PGSL-06/PGSA-05** | The stock UI actually opens | **manual only** | — | `11-HUMAN-UAT.md` |
| **PGSL-01/02/03** | A score actually lands on a real leaderboard | **manual only** | — | `11-HUMAN-UAT.md` |
| Pitfall 12 | Minified release runs the new PGS calls without missing keep rules | **manual/CI** | `./gradlew :app:assembleRelease` then inspect `missing_rules.txt` | ❌ Wave 0 (checklist step) |

### Sampling Rate
- **Per task commit:** the narrowest `--tests` filter for the file touched (each completes in seconds).
- **Per wave merge:** `./gradlew :domain:test :app:testDebugUnitTest`.
- **Phase gate:** `./gradlew check` green (accept the ~11-minute `depthSweep`), plus one `assembleRelease` for the R8 check, before `/gsd:verify-work`.

### Wave 0 Gaps
- [ ] `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt` — v1→v2 case + registration case (class exists; add cases)
- [ ] `domain/src/test/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRulesTest.kt` — new
- [ ] `app/src/test/java/com/app/pegsolitaire/data/ProgressAggregateTest.kt` — new (in-memory Room, `ProgressRepositoryTest` is the template)
- [ ] `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt` — new
- [ ] `app/src/test/java/com/app/pegsolitaire/platform/games/RecordingGamesService.kt` — new; a recording fake. Prefer **extending `FakeGamesService`** (it already models the three sign-in outcomes and a settable session) with per-method call logs and a settable success/failure result, over a second parallel fake.
- [ ] `build-logic/src/test/kotlin/GamesIdResolutionTest.kt` — new (`AdIdResolutionTest` is the template)
- [ ] No framework install needed — every library is already on the test classpath.

---

## Security Domain

`security_enforcement` is not set to `false` in `.planning/config.json`, so this section applies.

### Applicable ASVS categories

| ASVS category | Applies | Standard control |
|---------------|---------|------------------|
| V2 Authentication | Delegated | Play Games v2 owns sign-in end-to-end; the app never handles a credential. Phase 11 owns this surface. |
| V3 Session Management | Delegated | `GamesSignInState` mirrors the SDK's session; no app-managed token. |
| V4 Access Control | yes | Leaderboards/achievements are server-side and per-player; the app can only ever write its own player's data. Nothing to enforce locally. |
| V5 Input Validation | **yes** | The new `Results` nav args (`hintsUsed`, `undosUsed`, `lastPegOnTarget`) are an untrusted boundary in their own right — `Results` is independently reachable in `AppNavHost`, which is exactly why `challengeLevel` is already clamped there. Coerce the counters to `>= 0`. The `play_session` insert must never build SQL by concatenation (Room forbids it; every query binds `:params`). |
| V6 Cryptography | no | Nothing encrypted, signed or hashed by this phase. |
| V7 Logging | **yes** | `PlayGamesService`'s KDoc records that no logging call may name a player id, display name or SDK exception message; achievement and leaderboard IDs join that list. The absence is enforced by a grep gate, not review. |
| V12 Secure File Upload / Intent handling | **yes** | The two stock-UI Intents come *from the SDK* (`Task<Intent>`), are launched at a Google-owned component, and carry no app-supplied URI — so the `PrivacyLinks` intent-redirection concern (T-09-27) does not recur here. Do not add any caller-supplied data to them. |

### Known threat patterns

| Pattern | STRIDE | Standard mitigation |
|---------|--------|---------------------|
| Forged nav args inflate/deflate telemetry (a hand-crafted `Results` with `hintsUsed = 0`) | Tampering | Accepted and bounded, deliberately: the same posture as `AssistAllowance.fromStoredOrFull` — refusing would let a one-byte edit destroy an honest player's progress, in exchange for denying a cheater a cosmetic badge in a single-player game. Coerce to a sane range; never crash, never refuse the write. |
| Client-supplied leaderboard score is spoofable | Tampering | PITFALLS.md's standing answer: keep the formula deterministic and recomputable (D-07 does exactly this) and accept that a local-first single-player game has limited anti-cheat. Optionally set Console-side upper score bounds to discard absurd submissions. |
| Tampered `resume_snapshot` blob laundering an assist | Tampering | D-09's whole point. Counters ride the blob; `SnapshotMapper` reads absence as **0**, and an out-of-range value is coerced rather than refused (matching the allowance precedent). |
| SQL injection via `levelId` | Tampering | Structurally impossible — Room compiles queries and forbids string concatenation; every existing DAO binds `:params` (T-05-05). |
| Player id / display name / achievement id in logcat | Information disclosure | No logging in `platform/games` (grep gate). |
| A real ad unit or a real PGS ID leaking into a debug build | — | `adIdLiteralCheck` covers ad IDs. PGS IDs carry no equivalent penalty (see Build-Config ID Plumbing), so no new gate is required — but no ID literal may appear in `src/main` either way, per PLAT-06. |

---

## Sources

### Primary (HIGH confidence)
- `play-services-games-v2-22.0.0.aar` — unpacked from this repo's Gradle cache; `javap` on `PlayGames`, `LeaderboardsClient`, `AchievementsClient`, `GamesSignInClient`, `GamesClient`, `GamesClientStatusCodes`, `GamesActivityResultCodes`, `PlayGamesSdk`, `ScoreSubmissionData`; plus the bundled `AndroidManifest.xml` (minSdk 24, `PlayGamesInitProvider`).
- `play-services-games-v2-22.0.0-javadoc.jar` — the authoritative text for `submitScore` ("ignored if it is worse"), `setSteps` ("at least … no-op … must be greater than 0 … further mutation operations ignored"), `unlock`, and the `startActivityForResult` requirement on all three Intent getters.
- `room-testing-android-2.8.4.aar` — `javap` on `MigrationTestHelper` (`createDatabase(String,int)`, `runMigrationsAndValidate(String,int,boolean,Migration...)`).
- https://developer.android.com/games/pgs/achievements — 400 achievements max, **2,000 total points max**, 200 points max each, multiples of 5, 10-minimum/4-in-first-hour, hidden/revealed/unlocked states.
- https://developer.android.com/games/pgs/leaderboards — **70 leaderboards max**, Numeric/Time/Currency formats, larger-vs-smaller-is-better **immutable after publication**, server-side better-only update.
- https://developer.android.com/games/pgs/quality — submit at critical transitions, batch increments, 512×512 icons, client-library caching and automatic combining under rate limiting.
- https://developer.android.com/games/pgs/android/achievements and .../leaderboards — the canonical `startActivityForResult` code samples.
- This repository: `GamesService.kt`, `PlayGamesService.kt`, `NoOpGamesService.kt`, `GamesConstants.kt`, `AdIds.kt`, `build-logic/src/main/kotlin/AdIdResolution.kt`, `app/build.gradle.kts`, `ResultsViewModel.kt`, `ProgressRepository.kt`, `PegSolitaireDatabase.kt`, `app/schemas/.../1.json`, `DatabaseModule.kt`, `MigrationTest.kt`, `LevelResultEntity.kt`, `LevelResultDao.kt`, `ResumeDao.kt`, `ProgressPersistenceKeys.kt`, `ResumeSnapshotDto.kt`, `SnapshotMapper.kt`, `GameViewModel.kt`, `GameRoute.kt`, `Routes.kt`, `AppNavHost.kt`, `Boards.kt`, `ScoreResult.kt`, `ScoreParams.kt`, `ScoreCalculator.kt`, `SettingsScreen.kt`, `SettingsViewModel.kt`, `SettingsUiState.kt`, `PlayGamesOptInRepository.kt`, `PlayGamesPreferences.kt`, `ChallengeProgress.kt`, `LevelRepository.kt`, `PlatformBoundaryRules.kt`, `DomainPurityTest.kt`, `FakeGamesService.kt`, `BuildConfigIdsTest.kt`, `standards.gradle.kts`, `android-app.gradle.kts`, `config/detekt/detekt.yml`, `proguard-rules.pro`, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `local.properties.template`.

### Secondary (MEDIUM confidence)
- https://developers.google.com/games/services/management/api/achievements/reset · .../achievements/resetAll · .../scores/reset — tester-only reset endpoints; the basis for A3's "debug may use the real IDs".
- https://support.google.com/googleplay/android-developer/answer/2990418 — Play Console games-services feature overview.
- `.planning/research/PITFALLS.md` — Play Console ordering (Pitfall 7 there), Play App Signing SHA-1 (Pitfall 8), no-magic-strings for IDs, `javap`-verified "every `PlayGames.get*Client` takes an Activity".
- `.planning/research/ARCHITECTURE.md` — Pattern 5 (submission site), Anti-Pattern 7, SQL-aggregates-from-day-one scaling note.

### Tertiary (LOW confidence — flagged, not relied on)
- Community reports that the v2 unlock popup fails to render when achievement icons are not uploaded in Play Console (assumption A2). Corroborates the ROADMAP prerequisite note but is not documented by Google.
- Community reports that `submitScore` fails silently when the player is not authenticated (assumption A1). No Google documentation of a signed-out failure taxonomy exists; search results also conflate v1 (`Games.getGamesClient(activity, account)` + `GoogleSignIn`) with v2, which is why the `javap` verification above supersedes them.

---

## Metadata

**Confidence breakdown:**
- SDK surface & semantics: **HIGH** — `javap` + bundled javadoc on the exact pinned AAR, not a web summary.
- Room migration mechanics: **HIGH** — `javap` on the exact `room-testing` artifact; the harness is already green in this repo and the `1.json` shape is committed.
- SQL aggregates: **HIGH** — every column, mode literal and levelId format read out of the actual entity, DAO and `ResultsViewModel` code.
- Codebase integration points: **HIGH** — all line references read this session.
- Build-config plumbing: **HIGH** for the mechanism (the `AdIds` pattern is fully read), **MEDIUM** for the debug-behaviour recommendation (rests on A3).
- Play Console constraints (limits, points, icons, ordering immutability): **MEDIUM-HIGH** — official docs, unverifiable against a project that does not exist.
- Signed-out failure taxonomy & popup rendering: **LOW** — assumptions A1/A2; device checklist items, exactly as Phase 11 D-04 established.

**Research date:** 2026-09-07
**Valid until:** ~2026-10-07 for the SDK/Room facts (both are pinned versions in this repo, so they cannot drift without a deliberate catalog change); Play Console policy pages are the faster-moving half — re-check achievement point budgets and leaderboard limits before the operator creates them.
