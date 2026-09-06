# Phase 12: Leaderboards & Achievements - Context

**Gathered:** 2026-09-06
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase makes the player's progress **competitive and recognized** through the real `GamesService` bound in Phase 11. On every solved level the app submits the highest Challenge level to a ladder leaderboard, the cumulative Challenge score to its own leaderboard, and the per-board best to one of five Classic leaderboards (**7 leaderboards**, PGSL-01/02/03). A failed submission never blocks, fails, or alters a solved level (PGSL-04). A player who played offline and later signs in has their existing bests and earned achievements submitted through a self-healing sync (PGSL-05). The player can open the stock Play Games leaderboard and achievements UIs from Settings (PGSL-06, PGSA-05).

The phase ships a **roster of 20 visible achievements** (PGSA-01): three skill achievements (hint-free, undo-free, last peg in the center — PGSA-02), six incremental progression tiers over the Challenge ladder and star total (PGSA-03), and eleven one-shots including four reachable in the first hour. The stock in-app unlock popup shows the moment one is earned (PGSA-04).

Making the skill achievements earnable requires **per-session play telemetry** (hints used, undos used, whether the last peg landed on the board's target) that does not exist today. It lands as a **Room v1 → v2 migration** adding a `play_session` table, proven progress-preserving with the existing `MigrationTest` harness before any achievement wiring (PGSA-06). Every existing saved best, star total, and ladder position survives.

**Not this phase:** Saved Games / cloud save (Phase 13, behind `ProgressRepository`); Play Console declarations and signed-track verification (Phase 14); any custom in-app leaderboard or achievement UI (v1.1 out of scope — stock Google UI only); per-level Challenge leaderboards (70-leaderboard cap); social/rank-context surfaces (v2 backlog).

**Carried forward, not re-decided here:**
- Submission hooks into `ResultsViewModel.recordSolvedPlayOnce()`, which already has the process-death-safe exactly-once guard — **not** `GameViewModel`, whose `finishedEmitted` is reset by `onUndo()` and would double-submit (ROADMAP Phase 12 note).
- Aggregates are SQL-backed, never an in-memory fold: Challenge reaches thousands of rows (ROADMAP note).
- No Play Console PGS project exists yet, so verification is JVM tests against a fake `GamesService` plus a pre-release device checklist (Phase 11 D-04); the operator checklist gains the leaderboard/achievement creation and icon upload.
- Play Games surfaces hide when signed out rather than erroring (PGS-02); the Settings Play Games section from Phase 11 D-08 is where the new rows go.
- IDs are build-config values, test IDs in debug and real IDs in release, no literals in source (PLAT-06); `GamesConstants` explicitly reserves ID resolution for this phase following the `AdIds` precedent.

</domain>

<decisions>
## Implementation Decisions

### Achievement roster (20 achievements)
- **D-01:** The **hint-free** and **undo-free** skill achievements unlock on any solved level in **either mode**. **"Center finish"** means solving **English-33 with the last peg on the center hole `(3,3)`** — the classic target and the only board where it is a real skill goal. European-37 starts off-center, Diamond-41 cannot reach one peg, and Challenge boards have no defined center, so it applies nowhere else.
- **D-02:** The **four first-hour early wins** (PGSA-01) are: **First solve** (any mode), **First 3-star** (any mode), **Challenge level 5** (one-shot), and the first of the **per-board Classic unlocks** (D-04). A separate "First Classic board" achievement was considered and **dropped** as fully implied by the per-board unlocks.
- **D-03:** Progression (PGSA-03) is **two tiered axes, each tier its own Play Games incremental achievement** so the stock UI shows a progress bar: **ladder tiers at Challenge level 10 / 50 / 100** and **star tiers at 25 / 100 / 250 total stars**. Six achievements. Steps are reported as **absolute values** (highest Challenge level reached; total stars across both modes) via D-12, never as deltas. Both aggregates are SQL over `level_result`.
- **D-04:** The remaining one-shots: **Grand tour** (every one of the five Classic ids has a stored result), **Perfectionist** (every Classic board's stored best is at that board's minimum-peg finish — Diamond-41's proven minimum of 2 counts as perfect), **five per-board first-solve unlocks** (one per Classic id: English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45), and **Speed solve** (any level solved in **under 60 seconds** of `elapsedSeconds`).
- **D-05:** Roster total is **20 visible achievements**, which means **20 unique 512×512 icons** — the ROADMAP External Prerequisites row budgeted 10–12 and must be updated. Names, descriptions, and point values are at the planner's discretion (see Claude's Discretion).

### Per-session telemetry and score definitions
- **D-06:** Telemetry is stored in a **new `play_session` Room table, one row per solved play**: `mode`, `levelId`, `stars`, `score`, `elapsedMs`, `hintsUsed`, `undosUsed`, `lastPegOnTarget`, `finishedAt`. **`level_result` is untouched**, so the **v1 → v2 migration is a pure `CREATE TABLE`** and trivially progress-preserving; it still ships with a `MigrationTest` case that seeds v1 rows and asserts they survive, plus the exported `2.json` schema. All achievement predicates over telemetry become SQL over this table.
- **D-07:** **"Cumulative Challenge score" (PGSL-02) = `SUM(bestScore)` over `level_result` where `mode = challenge`** — the sum of per-level bests. It is recomputable, monotonic, only rises when a best improves, and stays identical across devices once Phase 13 syncs bests. It is **not** a running total of every play.
- **D-08:** A new **pure-Kotlin `SolveTelemetry(hintsUsed, undosUsed, lastPegOnTarget)`** type in `:domain` rides **alongside `ScoreResult`** through `GameViewModel`'s finish event and the Results nav args. `GameViewModel` counts assists and reads the last peg's position from the finishing board. **`ProgressRepository.recordResult` writes `level_result` and `play_session` in one transaction**, under the existing `recordSolvedPlayOnce()` exactly-once guard. `ScoreResult` stays a scoring type.
- **D-09:** **Every hint and every undo counts**, free-allowance or rewarded. The **counters persist in the resume snapshot** as nullable fields (the `freeHintsRemaining` / `freeUndosRemaining` precedent), so a resumed game cannot launder an assist by killing the process; absent fields on an old snapshot read as zero. **Restart and a new level reset** the counters.

### Offline backfill and submission orchestration
- **D-10:** Backfill (PGSL-05) is a **dirty-marker sync**: a persisted **"submissions pending"** flag is set by **every `recordResult` while the session is not `SignedIn`** and by **any failed submission**. **Whenever `session` resolves to `SignedIn`**, if the flag is set, everything is resubmitted from SQL aggregates and the flag is cleared **only on success**. Self-healing and idempotent; clean launches make no calls.
- **D-11:** Backfill covers **leaderboards and achievements**, both re-derived from `level_result` + `play_session`. A player who reached Challenge 50 offline receives the ladder tiers, per-board unlocks, Grand tour, and Perfectionist on sign-in. **One `AchievementRules` pass over the aggregates serves both live solves and backfill** — there is no second rule set.
- **D-12:** The `GamesService` seam **gains `setAchievementSteps(achievementId: String, steps: Int)`** (absolute, matching Play Games' `setSteps`), and **all tiered achievements use it** so resubmission is always safe. `incrementAchievement` stays declared but unused, or is removed — planner's call. `NoOpGamesService` and the test fake implement it trivially.
- **D-13:** Orchestration lives in **one data-layer `@Singleton` (working name `ProgressSubmitter`)** that owns the dirty flag, computes aggregates through `ProgressRepository` SQL, applies `AchievementRules`, and calls `GamesService`. `ResultsViewModel` calls it once after `recordResult` returns; the same object observes `session` for the D-10 backfill. **Nothing in `ui/` touches a leaderboard or achievement ID.** All calls are fire-and-forget from the caller's point of view (PGSL-04).

### Entry points, popup, and ID plumbing
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 12 entry (goal, five success criteria, and the **Notes** paragraph: migration-before-achievements, `recordSolvedPlayOnce()` as the hook, SQL-backed aggregates, icons as a prerequisite); External Prerequisites table row for achievement icons (must be updated from 10–12 to 20).
- `.planning/REQUIREMENTS.md` — PGSL-01…06 and PGSA-01…06 (the twelve requirements this phase closes); PLAT-03 (seam covers achievements + stock UI intents, No-Op degrades silently), PLAT-06 (IDs as build-config values), PLAT-07 (JVM suite stays SDK-free); Key Decisions row "Skill achievements → Accept the Room migration"; Out of Scope rows for per-level Challenge leaderboards and custom Play Games UI.

### Prior decisions this phase builds on
- `.planning/phases/11-play-games-session/11-CONTEXT.md` — D-01/D-02 (opt-in-gated bootstrap, `session` resolves once per process — the D-10 backfill observes this flow), D-04 (no Console project; fake-backed JVM verification + pre-release checklist), D-05 (stock popup untouched — D-16 mirrors it), D-08 (Play Games section placement — D-14 extends it), D-11/D-12 (`Unknown` hides the section; every "not signed in" cause is `SignedOut`), Claude's Discretion note that the Phase-12 seam methods were left as no-ops.
- `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` — the emulator checklist this phase extends with submission / popup / stock-UI items.
- `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md` — D-08 (IDs in `local.properties` + CI secret, never committed; release fails fast) — the pattern D-17 copies.
- `.planning/phases/09-consent-rewarded-ads/09-CONTEXT.md` — the per-level free hint/undo allowance and the assist funnel (`AssistKind`, `AdReason`) whose success path is where D-09's counters increment; D-14 (Privacy section pinned last, so new rows go above it).

### Milestone research
- `.planning/research/ARCHITECTURE.md` — `AchievementRules` as a pure `:domain` function, `ProgressRepository.aggregate(mode)`, `GamesIds` under `platform/games/`, `ResultsViewModel` as the submission caller; the "cadence and achievement rules are pure Kotlin" rationale.
- `.planning/research/PITFALLS.md` — "Trusting the client's score for leaderboards" (keep the formula deterministic and recomputable — D-07 follows), the no-magic-strings section on `LeaderboardIds` / `AchievementIds` constants and detekt complexity around SDK callbacks, and the Play Console ordering pitfalls (OAuth client must be created through Play Console to associate with leaderboards/achievements; you are not a tester by default).
- `.planning/research/STACK.md` — `play-services-games-v2` 22.0.0 and `kotlinx-coroutines-play-services` 1.11.0.

### Code standards
- `CLAUDE.md` — no magic strings/numbers (IDs via build config, thresholds and tier values in scoped constants objects), dimens resource file, state hoisting, detekt + ktlint gates, Gitflow. Domain purity: `SolveTelemetry` and `AchievementRules` carry zero Android imports.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt` — the seam; `submitScore`, `unlockAchievement`, `incrementAchievement`, `showLeaderboard`, `showAchievements` are declared with KDoc already stating fire-and-forget semantics. Gains `setAchievementSteps` (D-12).
- `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt` — real implementation; the five Phase-12 methods are `= Unit` no-op bodies (around lines 339–353) with a KDoc note deferring them to this phase. Already owns `ForegroundActivityProvider` access, `Task.await()` bridging, and the `session` flow.
- `app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt` + its test — the stub and the model for the recording fake.
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt` — timing/status tokens; its KDoc explicitly reserves ID resolution for Phase 12 following `AdIds`.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt` + `build-logic/src/main/kotlin/AdIdResolution.kt` + `app/build.gradle.kts` (`buildConfigField` / `resValue` per build type, `AdIdKeys.ALL`, fail-fast on release) — the exact pattern for `GamesIds` (D-17).
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt` — `recordSolvedPlayOnce()` (line ~311): `savedStateHandle` guard set before the write, then `progressRepository.recordResult(...)`; the D-13 submitter call goes right after `recordResult` returns.
- `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt` — `recordResult` runs `recordResultInTransaction` with the monotonic merge (max stars / min time / max score); `play_session` insert joins this transaction (D-08). `LevelResult` and `NewBests` types.
- `app/src/main/java/com/app/pegsolitaire/data/db/PegSolitaireDatabase.kt` (`DB_VERSION = 1`, `exportSchema = true`), `app/schemas/.../1.json`, `app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt` (where the `Migration` is registered), `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt` (the `MigrationTestHelper` harness waiting for its first real migration).
- `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultEntity.kt` / `LevelResultDao.kt` — `(mode, levelId)` keyed bests with `stars`, `bestTimeMs`, `bestScore`, `completedAt`; the source of every leaderboard aggregate. `ProgressPersistenceKeys` holds table/column names as the on-disk contract — new names go there.
- `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt` + `SnapshotMapper.kt` — nullable `freeHintsRemaining` / `freeUndosRemaining` and `targetRow` / `targetCol` fields; the precedent for D-09's persisted counters and the target cell needed for `lastPegOnTarget`.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — finish path (~lines 878–915) builds `ScoreResult` from the finishing board and sends `_finished`; `onUndo()` (~1076) and `onHintRequested()` (~1205) go through `enterAssistFunnel(AssistKind, AdReason)` — the counters increment on the funnel's success path.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` — English-33 `center = Position(3, 3)`; per-board `minimumPegs` on the spec (Perfectionist, D-04). `ClassicLevelIds` (in `app/.../data/LevelRepository.kt`) enumerates the five ids.
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreResult.kt` / `ScoreParams.kt` — `elapsedSeconds` (Speed solve), `stars`, `score`; the constants-object style for thresholds.
- `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt` — `PlayGamesSection` (line ~257): `Unknown` returns early, `SignedIn` / `SignedOut` rows via `SettingsLinkRow(label, supporting, entranceOrder, trailingIcon, onClick)`; D-14/D-15 add two rows inside the `SignedIn` branch.
- `app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt` / `PlayGamesPreferences.kt` — DataStore keys for the opt-in flags; the natural home for the D-10 dirty flag.
- `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` — persisted `currentLevel` DataStore flow; must agree with the SQL ladder aggregate.

### Established Patterns
- **Seam + `@Binds` in `di/PlatformModule.kt`**; SDK types never reach `ui/` — enforced by `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryGateTest.kt` / `PlatformBoundaryTest.kt`; `:domain` purity enforced by `DomainPurityTest.kt`. `SolveTelemetry` and `AchievementRules` must pass both.
- **Exactly-once via `SavedStateHandle` set-before-launch** (`recordSolvedPlayOnce`, `claimCelebrationPlayOnce`) — the submitter call inherits this guard for free by living inside it.
- **Best-effort storage with `StorageFailSafe`** — a failed `recordResult` reports "nothing improved" rather than crashing; submissions follow the same never-fail-the-level rule (PGSL-04) and additionally set the dirty flag.
- **Platform singleton owns its own state flow; ViewModels collect into UiState in `init`** — the submitter observes `GamesService.session` the same way `SettingsViewModel` does.
- **Scoped constants objects per concern** (`GamesConstants`, `AdConstants`, `ScoreParams`) — tier thresholds and the 60 s speed limit get their own object (e.g. `AchievementThresholds` in `:domain`).
- **Exported Room schema + `MigrationTest`** — the KDoc on `PegSolitaireDatabase` spells out the bump-version / add-Migration / commit-schema-JSON ritual.
- **Nullable-with-default snapshot fields** — old blobs deserialize with `null` and the mapper supplies the default; D-09's counters follow this.

### Integration Points
- `GamesService` / `NoOpGamesService` / `PlayGamesService` / test fake — `setAchievementSteps` added; five no-op bodies replaced.
- `domain/.../SolveTelemetry.kt`, `domain/.../achievement/AchievementRules.kt` (+ thresholds object) — new pure-Kotlin types and rules.
- `GameViewModel` — assist counters, `lastPegOnTarget`, finish event payload; `GameRoute` → `ResultsRoute` nav args extended.
- `ResumeSnapshotDto` / `SnapshotMapper` — persisted counters.
- `data/db/` — `PlaySessionEntity`, DAO, `DB_VERSION = 2`, `Migration(1, 2)` in `DatabaseModule`, `app/schemas/.../2.json`, `MigrationTest` case; `ProgressPersistenceKeys` gains the new names.
- `ProgressRepository` — `recordResult` takes `SolveTelemetry`, writes both tables in one transaction; gains the aggregate queries D-03/D-07/D-11 need.
- `data/ProgressSubmitter` (new) — dirty flag, aggregates → rules → `GamesService`; observes `session`.
- `ResultsViewModel.recordSolvedPlayOnce()` — one call into the submitter after `recordResult`.
- `SettingsUiState` / `SettingsViewModel` / `SettingsScreen.PlayGamesSection` — two rows, signed-in only, calling `showLeaderboard` / `showAchievements`.
- `platform/games/GamesIds.kt`, `build-logic` resolution, `app/build.gradle.kts`, `local.properties.template`, CI secrets — 27 IDs.
- `.planning/ROADMAP.md` External Prerequisites row (icons 10–12 → 20) and `11-HUMAN-UAT.md` checklist — operator-facing updates.

</code_context>

<specifics>
## Specific Ideas

- Roster is fixed at these 20: First solve · First 3-star · Challenge level 5 · Hint-free · Undo-free · Center finish (English-33) · Speed solve (< 60 s) · Ladder 10 / 50 / 100 · Stars 25 / 100 / 250 · Cleared English-33 / European-37 / Asymmetric-39 / Diamond-41 / Wiegleb-45 · Grand tour · Perfectionist.
- "Center finish" is English-33 `(3,3)` and nothing else — do not generalize it to "finish on the starting hole".
- Cumulative score is the sum of bests, chosen specifically so it stays consistent with Phase 13's bests-only cloud sync.
- The dirty-marker sync is the *only* backfill path; there is no separate "first sign-in" special case.
- Settings row labels: **"Leaderboards"** and **"Achievements"**; supporting text at the planner's discretion, all through `strings.xml`.

</specifics>

<deferred>
## Deferred Ideas

- A "View leaderboard" shortcut on the solved Results screen and a trophy button on Home — considered, rejected for this phase (D-14); revisit with the v2 social-first leaderboard UI.
- Bottom-gravity or custom-styled unlock popup — rejected (D-16); custom Play Games UI is v2.
- `play_session` pruning / retention cap — only if research or profiling shows a cost.
- Additional achievement tiers with new content — v1.1 explicit future-consideration item; the roster above is the v1.1 set.

### Reviewed Todos (not folded)
- **Decide whether Stuck finishes should reach Results (1–2 star bands are unreachable)** — telemetry records solved plays only, so this remains an independent scoring/flow decision.
- **Confirm audio cues on physical hardware** — keyword-only match; Phase 7 audio debt.
- **Upgrade Wiegleb-45 opening book from K=3 to K=4** — keyword-only match; Phase 8 solver debt.

</deferred>

---

*Phase: 12-leaderboards-achievements*
*Context gathered: 2026-09-06*
