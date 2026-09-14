---
phase: 12
slug: leaderboards-achievements
status: verified
threats_open: 0
asvs_level: 1
created: 2026-09-14
---

# Phase 12 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Register authored at plan time (all 8 PLAN files carry `<threat_model>` blocks, 54 rows). Plans reuse
> threat IDs (12-01 and 12-02 both use T-12-04..07; 12-03 and 12-06 both use T-12-30..35), so every
> row below is qualified as `{plan}/{id}`.
> Verified 2026-09-14 by gsd-security-auditor in verify-mitigations mode against HEAD `24ea8ef`, which
> includes the four post-plan review fixes (`80a4df7` CR-01, `4807c8a` WR-01, `ffbe012` WR-02, `027ea09`
> WR-03). Every CLOSED row was confirmed by locating the construct (file:line) in implementation or its
> pinning test, not by reading SUMMARY claims. Full per-threat evidence table is appended at the end.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Shipped v1 SQLite file → v2 Room open path | One-way, unrecoverable schema migration (no cloud backup until Phase 13) | Stars, best times, ladder position |
| Generated `2.json` → hand-written `MIGRATION_1_2` | Authored DDL must match generated schema byte-for-byte | Schema contract |
| `Results` nav args / resume snapshot blob → `SolveTelemetry` → `play_session` | Untrusted, player-reachable inputs feeding skill achievements | Hint/undo counters, last-peg flag, `minimumPegs` |
| `level_result` + `play_session` → SQL aggregates → `:domain` `AchievementRules` | Local truth recomputed, never folded in Kotlin | Bests, totals, cleared boards |
| `data/` → `platform/games` → Play Games Services v2 (Google process, IPC) | Boolean-returning seam; IDs resolved only in `platform/games` | Scores, achievement unlocks/steps |
| `ui/` ↔ `platform/games` | ArchUnit-enforced: no ID, `AchievementId` or submit call under `ui/` | Primitives only |
| `SubmissionSyncStore` (DataStore, player-writable) → submission scheduling | Forgeable flag can only reschedule an idempotent pass | Pending boolean |
| Developer machine / CI → build output → public git | 27 Play Games IDs live only in gitignored `local.properties` / CI env | Leaderboard & achievement IDs |
| Release build → R8 minification → runtime | Reflective `gms.games` classes must survive shrinking | Keep rules |
| Operator → Play Console → app | Ordering of OAuth, credentials, tester list; immutable published config | Console configuration |

---

## Threat Register

54 threats across the 8 plan `<threat_model>` blocks (see each 12-NN-PLAN.md for the full row), plus one
post-plan row registered here.

**Summary of dispositions after the 2026-09-14 audit:**

| Group | Threats | Disposition | Status |
|-------|---------|-------------|--------|
| Code-verified mitigations | 12-01/T-12-01..05; 12-02/T-12-05..07; 12-03/T-12-30..33, 35, 36; 12-04/T-12-40, 41, 43; 12-05/T-12-20..23; 12-06/T-12-30, 31, 34; 12-07/T-12-70..72, 74, 75; 12-08/T-12-08-01 (30 threats) | mitigate | closed |
| Transfer | 12-03/T-12-34 (R8 keep rules, to release build gate); 12-04/T-12-44 (client scores, to Play Games server-side leaderboards + Console bounds) | transfer | closed |
| Gated operator actions, documented as device items | 12-08/T-12-08-02 (R8 `missing_rules.txt`, UAT 12-HUMAN-UAT #6 / 11-HUMAN-UAT #9); 12-08/T-12-08-03 (Console OAuth / SHA-1 / tester ordering, OPERATOR-CHECKLIST Step 0) | mitigate | closed — **carried to Phase 14** |
| Accepted risks | 12-01/T-12-06, T-12-07; 12-02/T-12-04 (residual); 12-04/T-12-42; 12-05/T-12-24; 12-06/T-12-33; 12-07/T-12-73, T-12-76; 12-08/T-12-08-04 | accept | closed — see Accepted Risks Log |
| Not applicable | eight `T-12-SC` supply-chain rows (no dependency added; `libs.versions.toml` byte-identical to develop), 12-06/T-12-32 (parameterised `@Query`, injection structurally impossible), 12-06/T-12-35, 12-07/T-12-77 | N/A | closed |

**Post-plan addition (registered here, not in any PLAN):**

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| 12-FIX/T-12-78 | Tampering | `Results.minimumPegs` nav arg (`Routes.kt:128`, added by CR-01 fix `80a4df7`) | mitigate | Clamped at the nav boundary to `1..BoardConstants.HIGHEST_MINIMUM_PEGS` (`ResultsViewModel.kt:154-157`), so a forged value cannot widen `pegsRemaining <= minimumPegs` beyond any shipped board's floor; pinned by `ResultsViewModelTest.kt:463, 489` | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

Notable code-verified anchors (full table appended):
- 12-01/T-12-01..03: `Migrations.kt:44-48` one additive `CREATE TABLE`, byte-identical to `schemas/…/2.json`; registered at `DatabaseModule.kt:52`; zero `fallbackToDestructive` in `app/src`
- 12-01/T-12-05, 12-06/T-12-32: every `@Query` binds `:params` and interpolates only `const` table/column names (`LevelResultDao.kt:62-66`, `PlaySessionDao.kt:55-73`)
- 12-02/T-12-06, 12-05/T-12-21 (WR-03): forged negatives resolve against the forger — `SolveTelemetry.kt:117` → `UNACCOUNTED_ASSIST`; `SnapshotMapper.kt:188` refuses the resume
- 12-05/T-12-22: `ResumeSnapshotDto.kt:113-114` nullable defaults keep shipped resume blobs decodable
- 12-05/T-12-23: exactly two `countAssist` sites (`GameViewModel.kt:1264`, `:1550`), each after the assist is applied, serving free and rewarded paths alike
- 12-06/T-12-30: `play_session` insert inside `withTransaction` (`ProgressRepository.kt:217-252`), rollback tested in both directions
- 12-06/T-12-31: `SUM(bestScore)` is `Long?` end-to-end (`ProgressProjections.kt:43` → `ProgressAggregate.kt:93` → submitter), pinned near `Int.MAX_VALUE`
- 12-06/T-12-34: `storageFailSafe` on all three repository paths; aggregate reads share one transaction
- 12-07/T-12-70/71/72/75: `ProgressSubmitter.kt:152` fire-and-forget, `:175` session gate before any read, `:170`+`:181` mutex covers the flag read (WR-01), `:205-213` a zero-call pass never clears the flag
- 12-03/T-12-32/33/35: `PlayGamesService.kt:437-446` fails fast without a foreground Activity (WR-02); `CancellationException` rethrown at `:445/:474/:274`; SDK Intents started unmodified at `:473`
- 12-03/T-12-31/36, 12-07/T-12-74: seam names only primitives (`GamesService.kt:149-222`); zero hits for `GamesIds|AchievementId|submit*` under `ui/`; ArchUnit `PlatformBoundaryRules.kt:33,86` + `DomainPurityTest.kt:30`
- 12-04/T-12-40/41/43, 12-08/T-12-08-01: `local.properties` untracked; `gamesIdLiteralCheck` wired into `check` (`standards.gradle.kts:231-239`); two independent release gates (`app/build.gradle.kts:137-150`, `:172-188`); zero `CgkI` in `src/main` and in `12-OPERATOR-CHECKLIST.md`; zero `android.util.Log` in `app/src/main`

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-12-01 | 12-01/T-12-06 | `play_session` holds gameplay counters and timestamps only, in app-private storage, never logged (`PlaySessionEntity.kt:33-54`). | user (plan-time disposition, confirmed at audit) | 2026-09-14 |
| AR-12-02 | 12-01/T-12-07, 12-02/T-12-04, 12-05/T-12-24, 12-06/T-12-33, 12-07/T-12-73 | A rooted device can edit the SQLite file or resume blob and forge history. No server exists this milestone; the control is determinism and recomputability of every submitted value from stored rows (D-07, `12-CONTEXT.md:38`; PITFALLS.md "Trusting the client's score", `:489`). Play Games leaderboards are server-side and per-player, so the blast radius is one player's own entries. | user | 2026-09-14 |
| AR-12-03 | 12-04/T-12-42 | A debug build honours a configured ID and writes real scores. Play Games Services publishes no test IDs; per-tester scores are resettable in Play Console (`GamesIdResolution.kt:228-240`, `local.properties.template:52`). | user | 2026-09-14 |
| AR-12-04 | 12-04/T-12-44 | Client-supplied scores are inherent to the platform; Console per-leaderboard upper bounds are the only backstop (PITFALLS.md `:489`). Recorded as transfer/accept. | user | 2026-09-14 |
| AR-12-05 | 12-07/T-12-76 | A forged sync flag can only schedule a redundant idempotent pass or delay one; no submitted value derives from it (`PlayGamesPreferences.kt:43-54`). | user | 2026-09-14 |
| AR-12-06 | 12-08/T-12-08-04 | Published leaderboard sort order, achievement type and step totals are immutable with no migration path. Made loud in `12-OPERATOR-CHECKLIST.md:82,119`; a mistake costs a delete-and-recreate with score loss. | user | 2026-09-14 |

*Accepted risks do not resurface in future audit runs.*

---

## Residual Warnings (advisory, non-blocking)

Controls that exist but whose guarantee is narrower than the register text, or gaps surfaced by the audit
outside the register:
- `gamesIdLiteralCheck` skips comment-prefixed lines (`standards.gradle.kts:166-169`), so a real ID pasted into a source comment would evade the 12-04/T-12-40 gate. Compensating control: the same ID would also have to appear in `local.properties` to be useful, and `12-OPERATOR-CHECKLIST.md` forbids recording IDs anywhere tracked. Fix path: drop the comment exemption on next touch.
- `12-01-SUMMARY.md` and `12-08-SUMMARY.md` carry no `## Threat Flags` section (12-01 uses `## Threat Model Disposition` instead). No threat was missed; the executor template drifted.
- Five Info findings in `12-REVIEW.md` (IN-01..IN-05) remain open by scope. None is a security control. Fix path: `/gsd:code-review 12 --fix --all`.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-09-14 | 55 (54 plan-time + 1 post-plan) | 55 | 0 | gsd-security-auditor (verify-mitigations mode, HEAD `24ea8ef`) + user acceptance of AR-12-01..06 |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-09-14

---

## Appendix — Per-threat evidence table (auditor output, verbatim)


Audited HEAD: `24ea8ef` (branch `feature/phase-12-leaderboards-achievements`), 2026-09-14.
Mode: verify-mitigations. ASVS level 1. Register authored at plan time across 12-01..12-08 `<threat_model>` blocks.
Register row count as supplied/enumerated: **54** (the prompt header says "55 rows"; the supplied table
and the eight plan blocks both enumerate 54 — discrepancy noted, no row is missing).

Post-plan review fixes verified against the CURRENT construct where the plan text described the pre-fix shape:
`80a4df7` (CR-01), `4807c8a` (WR-01), `ffbe012` (WR-02), `027ea09` (WR-03).

Status legend: CLOSED (construct located) · CLOSED-accepted (rationale documented) · CLOSED-transfer ·
CLOSED-N/A (stated reason re-verified) · CLOSED-gated (operator/device action, documented as a gate) · OPEN.

---

## Plan 12-01 — Room v2 migration + `play_session`

| id | category | component | disposition | status | evidence (file:line) | notes |
|----|----------|-----------|-------------|--------|----------------------|-------|
| 12-01/T-12-01 | Tampering / DoS | Migrations.kt migration data loss | mitigate | CLOSED | `app/src/main/java/com/app/pegsolitaire/data/db/Migrations.kt:44-48` — `migrate()` body is exactly one `db.execSQL(CREATE_PLAY_SESSION_SQL)`; zero `ALTER`/`DROP`/`INSERT INTO ... SELECT` in the file. Test `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt:64` `version1ProgressSurvivesTheMigrationToVersion2`, `:74-78` `runMigrationsAndValidate(..., MIGRATION_1_2)` with `validateDroppedTables` const at `:132` | Additive-only verified by reading the whole file, not by grep alone |
| 12-01/T-12-02 | DoS | DatabaseModule missing addMigrations | mitigate | CLOSED | `app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt:52` `.addMigrations(MIGRATION_1_2)` on the single production builder (`:42-53`). Test `MigrationTest.kt:100,111` opens a seeded v1 file through `DatabaseModule.providePegSolitaireDatabase` itself | Only one `databaseBuilder` exists in `app/src/main`; no second unmigrated builder |
| 12-01/T-12-03 | DoS | hand-written DDL vs generated schema | mitigate | CLOSED | `Migrations.kt:22-27` vs `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/2.json` `play_session.createSql` — compared programmatically, byte-identical modulo `${TABLE_NAME}` → `` `play_session` ``. `2.json` tracked (`git ls-files app/schemas/`) | Column order, `AUTOINCREMENT`, every `NOT NULL` match |
| 12-01/T-12-04 | DoS | destructive fallback | mitigate | CLOSED | `grep -rn "fallbackToDestructive" app/src/` → 0 matches. Rationale KDoc `DatabaseModule.kt:28-33` | Absence-type control verified by count, as planned |
| 12-01/T-12-05 | Tampering | SQL injection via mode/levelId | mitigate | CLOSED | `PlaySessionDao.kt:30-31` `@Insert`; `:55-73` the one `@Query` interpolates only `const` `$TABLE_*`/`$COLUMN_*` names and binds `:classicMode`, `:english33Id`, `:speedLimitMs`. `Migrations.kt:22-27` DDL interpolates no runtime value | Plan text said "only `@Insert`"; a `@Query` was added by 12-06 — current shape still parameter-bound, so the threat holds closed |
| 12-01/T-12-06 | Info Disclosure | play_session row contents | accept | CLOSED-accepted | Columns are gameplay counters + timestamps only — `PlaySessionEntity.kt:33-54`; no player id / name / device id. `grep -rn "android.util.Log" app/src/main` → 0 | Rationale: 12-01-PLAN `<threat_model>` row T-12-06 |
| 12-01/T-12-07 | Tampering | rooted-user SQLite edit | accept | CLOSED-accepted | Rationale: 12-01-PLAN T-12-07 row + `.planning/research/PITFALLS.md:489` (local-first single-player standing answer) | Coercion of untrusted *inputs* happens at the nav/blob boundary (12-05), verified separately |
| 12-01/T-12-SC | supply chain | dependency graph | N/A | CLOSED-N/A | `git diff ffa90694..HEAD -- gradle/libs.versions.toml` → empty | Re-verified, not assumed |

## Plan 12-02 — `:domain` achievement rules + telemetry

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-02/T-12-04 | Tampering | AchievementRules / ProgressAggregate | mitigate (partial, residual accepted) | CLOSED-accepted | `domain/.../achievement/AchievementRules.kt:51-56` `evaluate(aggregate)` — total, side-effect-free, single `ProgressAggregate` parameter, no I/O; recomputability chain confirmed at `data/ProgressRepository.kt:268-306` (all values from SQL). Residual rationale: PITFALLS.md:489 + 12-CONTEXT D-07 (`:38`) | Forgery is NOT prevented and the plan does not claim it is |
| 12-02/T-12-05 | DoS | tierSteps zero/negative steps | mitigate | CLOSED | `AchievementRules.kt:130-137` `putIfPositive` — `if (steps != null && steps > 0) put(...)`, used for all six tiers (`:113-121`). Pinned by `app/src/test/.../data/ProgressSubmitterTest.kt:259` `noStepSubmissionEverCarriesANonPositiveValue` | Covers both axes (ladder + stars) — all entry points |
| 12-02/T-12-06 | Tampering | SolveTelemetry | mitigate (bounded) | CLOSED (strengthened) | `domain/.../telemetry/SolveTelemetry.kt:99-108` `coerced(...)`, `:117` `orUnaccountedIfNegative()`; `:68` `UNACCOUNTED_ASSIST = 1`. Test `ResultsViewModelTest.kt:1197` | **Deviates from plan text** (`coerceAtLeast(0)`): WR-03 `027ea09` resolves a forged negative to 1 rather than 0, i.e. against the forger. Strictly stronger for this threat |
| 12-02/T-12-07 | Info Disclosure | domain achievement/telemetry packages | mitigate | CLOSED | `grep -rn CgkI domain/src` → 0; `grep -rn "import android\|androidx" domain/src/main` → 0 real imports (one KDoc mention); ArchUnit gate `app/src/test/.../DomainPurityTest.kt:30` forbids `android..`/`androidx..` from `..domain..`. ID map lives in `platform/games/GamesIds.kt:32+` | |
| 12-02/T-12-SC | supply chain | libs.versions.toml, domain/build.gradle.kts | N/A | CLOSED-N/A | `git diff ffa90694..HEAD` on both paths → empty | |

## Plan 12-03 — `GamesService` seam + `PlayGamesService`

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-03/T-12-30 | Info disclosure | submission + stock-UI bodies | mitigate | CLOSED | `grep -rEn "android\.util\.Log\|Timber\|println\(" app/src/main/java/com/app/pegsolitaire/platform/games/` → 0; failure expressed as `false` at `PlayGamesService.kt:441-446` | Repo-wide: 0 `android.util.Log` in `app/src/main` |
| 12-03/T-12-31 | Elevation | GamesService seam signatures | mitigate | CLOSED | `platform/games/GamesService.kt:149-222` — only `String`/`Long`/`Int`/`Boolean`/`Unit`; both Intents obtained AND started inside `platform` (`PlayGamesService.kt:396-403`, `:460-474`). ArchUnit `PlatformBoundaryRules.kt:33` (`onlyPlatformDependsOnPlaySdks`) and `:86` (`uiAndDomainNeverTouchAnActivity`), run by `PlatformBoundaryTest.kt:50,57` | |
| 12-03/T-12-32 | DoS | submitting{} / stock-UI launches | mitigate | CLOSED (reshaped) | `PlayGamesService.kt:437-446` — session gate `_session.value is SignedIn` then `activityProvider.current` (no await, fails fast). Stock UI keeps the bound: `:362-363` `withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS)`. Tests `PlayGamesServiceTest.kt:470,483,523,593` | Plan described an ACTIVITY_WAIT_MS cap on submissions; WR-02 `ffbe012` replaced the wait with immediate `false`. Bound is now zero — threat still closed |
| 12-03/T-12-33 | Tampering / Repudiation | Task.await() failures in runCatching | mitigate | CLOSED | `PlayGamesService.kt:444-446` (`onFailure { if (it is CancellationException) throw it }`), `:474` (stock UI), `:274` (signIn). Tests `PlayGamesServiceTest.kt:353,579` | Cancellation rethrown *before* classification at all three sites |
| 12-03/T-12-34 | Tampering | R8 stripping of games-v2 reflective classes | transfer | CLOSED-transfer (gated) | Transfer target documented: `.planning/phases/12-leaderboards-achievements/12-HUMAN-UAT.md:42` and `.planning/phases/11-play-games-session/11-HUMAN-UAT.md:60-61` (assembleRelease → inspect `app/build/outputs/mapping/release/missing_rules.txt` for `com.google.android.gms.games`, then re-run test 5 on the minified build) | Gate not yet executable: needs 27 real IDs (`verifyReleaseGamesIds` blocks otherwise) + release signing. Carry to Phase 14 |
| 12-03/T-12-35 | Tampering | the two stock-UI Intents | mitigate | CLOSED | Intent source is SDK-only: `PlayGamesService.kt:398` `getAllLeaderboardsIntent()`, `:403` `getAchievementsIntent()`; started unmodified at `:473` `startActivityForResult(intent, requestCode)` — no extras/data/component set anywhere in `launchStockUi` (`:460-474`). Tests `:551,566` | |
| 12-03/T-12-36 | Spoofing | ID injection from ui/ | mitigate | CLOSED | `GamesService.kt:216,222` — `showLeaderboards()` / `showAchievements()` take no parameters; `grep -rn "GamesIds\|AchievementId\|submitScore\|unlockAchievement\|setAchievementSteps" app/src/main/java/com/app/pegsolitaire/ui/` → 0; build gate `standards.gradle.kts:154-186` | Only `ui/settings/SettingsViewModel.kt:254,262` call the seam, argument-free |
| 12-03/T-12-SC | supply chain | package installs | N/A | CLOSED-N/A | catalog diff empty; `play-services-games-v2` pinned by Phase 6 | |

## Plan 12-04 — build-time ID resolution

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-04/T-12-40 | Info disclosure / Tampering | real IDs committed to source | mitigate | CLOSED (with caveat) | `.gitignore:7` `local.properties`; `git ls-files local.properties` → 0; `local.properties.template` committed with `REPLACE_ME_` only; `build-logic/src/main/kotlin/standards.gradle.kts:154-186` `gamesIdLiteralCheck` (marker `CgkI` at `:103`) wired into `check` at `:231-239`; `grep -rn CgkI app/src/main` → 0 | **Caveat (advisory):** the gate skips comment-prefixed lines (`standards.gradle.kts:166-169`), so an ID pasted into a source comment would pass. Does not absent the control |
| 12-04/T-12-41 | Tampering | release shipping placeholder IDs | mitigate | CLOSED | Config-time: `app/build.gradle.kts:137-150` throws `GradleException` for a release invocation (`isReleaseInvocation` `:88-91`), otherwise stamps `UNRESOLVED-…` (`:95`,`:148`). Execution-time: `:172-184` `verifyReleaseGamesIds`, hooked at `:186-188` to `assembleRelease`/`bundleRelease`. `GamesIdResolution.kt:253-258` `present()` treats `UNCONFIGURED-` as absent. `build-logic/src/test/kotlin/GamesIdResolutionTest.kt` | Two independent gates verified, not one |
| 12-04/T-12-42 | Tampering | debug build submitting to production leaderboards | accept (bounded) | CLOSED-accepted | Rationale in code + docs: `build-logic/src/main/kotlin/GamesIdResolution.kt:228-240` (resettable per tester via Management API) and `local.properties.template:52` ("fill in real IDs here and your debug builds write to the REAL leaderboards"); repeated in `12-OPERATOR-CHECKLIST.md` Step 4 | |
| 12-04/T-12-43 | Info disclosure | an ID reaching logcat | mitigate | CLOSED | `platform/games/GamesIds.kt` contains no logging call (BuildConfig getters only, `:92-144+`); `grep -rn "android.util.Log" app/src/main` → 0 | |
| 12-04/T-12-44 | Spoofing / Elevation | client-supplied leaderboard scores | transfer / accept | CLOSED-accepted | `.planning/research/PITFALLS.md:489` standing answer; `12-CONTEXT.md:38` D-07 (deterministic, recomputable `SUM(bestScore)`); PGS leaderboards are server-side and per-player | Receiving control (Console upper score bounds) is 12-08 checklist work — operator-gated |
| 12-04/T-12-SC | supply chain | npm/pip/cargo | N/A | CLOSED-N/A | catalog diff empty; no package manager in project | |

## Plan 12-05 — telemetry capture at the nav / snapshot boundaries

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-05/T-12-20 | Tampering | Routes.kt Results, ResultsViewModel.solveTelemetry | mitigate (bounded) | CLOSED | `navigation/Routes.kt:129-131` — `hintsUsed: Int = 0`, `undosUsed: Int = 0`, `lastPegOnTarget: Boolean = false`; `ui/results/ResultsViewModel.kt:204-208` single `SolveTelemetry.coerced` call (the only read of the three args). Tests `ResultsViewModelTest.kt:1167,1176,1197,1224` | |
| 12-05/T-12-21 | Tampering | SnapshotMapper, ResumeSnapshotDto | mitigate (bounded) | CLOSED (reshaped, stricter) | Absent → `0` via `SnapshotMapper.kt:202-203` `orNoAssists()` (`:233`); negative → resume REFUSED at `:188` `if (dto.hasForgedAssistCounter()) return null` (`:219-222`); huge preserved (test `SnapshotMapperTest.kt:432`). Tests `:390,:407` | **Deviates from plan text** ("never refused"): WR-03 `027ea09` refuses on negative. Stricter against tampering; the availability cost is bounded to values no honest writer can produce |
| 12-05/T-12-22 | DoS | decode on an old blob | mitigate | CLOSED | `data/resume/ResumeSnapshotDto.kt:113-114` `hintsUsed: Int? = null`, `undosUsed: Int? = null`; `SnapshotMapper.kt:92` `Json { ignoreUnknownKeys = true }`. Test `SnapshotMapperTest.kt:390` `aLegacyBlobWithNoAssistCounterKeysRestoresToZeroAssists` (blob with neither key) | |
| 12-05/T-12-23 | Spoofing | GameViewModel.countAssist call sites | mitigate | CLOSED | Exactly two writes: `ui/game/GameViewModel.kt:1264` (inside `performUndo`, after the empty-history guard and `moveHistory.removeAt` at `:1244`) and `:1550` (inside `revealPendingHint`, after the `pendingHint ?: return` at `:1544` and the staleness `return` at `:1547`). All entry points funnel here: sole rewind site `:1238-1244` (callers `:1216`, `:1445`), sole reveal site `:1543-1551` (callers `:1400` free, `:1444` rewarded); only `:1551` ever sets `hintSource/hintTarget` non-null | Checked ALL call paths, not just the two cited lines |
| 12-05/T-12-24 | Tampering | whole telemetry chain (device-local) | accept (residual, documented) | CLOSED-accepted | 12-05-PLAN `<threat_model>` T-12-24 (incl. the force-kill-before-`ON_STOP` sub-case) + PITFALLS.md:489 | |
| 12-05/T-12-SC | supply chain | libs.versions.toml | N/A | CLOSED-N/A | catalog diff empty | |

## Plan 12-06 — persistence + aggregates

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-06/T-12-30 | Tampering (torn state) | recordResultInTransaction | mitigate | CLOSED | `data/ProgressRepository.kt:217-221` `database.withTransaction { recordResultInTransaction(...) }`; `playSessionDao.insert` at `:252` is INSIDE, after `levelResultDao.upsert` at `:237`. Tests `ProgressRepositoryTest.kt:266,280,303` (row counts on both tables after forced failure, both directions) | |
| 12-06/T-12-31 | Tampering (integer overflow) | challengeScoreTotal / LevelResultDao.totals | mitigate | CLOSED | `data/db/ProgressProjections.kt:43` `challengeScoreTotal: Long?`; `domain/.../ProgressAggregate.kt:93` `Long?`; SQL `LevelResultDao.kt:64` `SUM(bestScore)`; carried as `Long` into `ProgressSubmitter.submitLeaderboards` (`:216-230`). Test `ProgressAggregateTest.kt:118` `theChallengeScoreTotalIsALongAndNeverWrapsThroughIntMaxValue` | End-to-end type chain checked, not just the projection |
| 12-06/T-12-32 | Tampering (SQL injection) | LevelResultDao, PlaySessionDao | N/A — structurally impossible | CLOSED-N/A | All `@Query` strings interpolate only `const` `$TABLE_*`/`$COLUMN_*` identifiers and bind values as `:params` (`LevelResultDao.kt:62-66`, `PlaySessionDao.kt:55-73`); grep for a non-const `$` interpolation in either DAO → 0 | |
| 12-06/T-12-33 | Tampering (client-trusted score) | aggregate → submission chain | accept (residual, documented) | CLOSED-accepted | PITFALLS.md:489; `12-CONTEXT.md:38` D-07; determinism verified — every submitted number is read from SQL in `ProgressRepository.aggregate()` (`:268-306`), never a running in-memory total | |
| 12-06/T-12-34 | DoS (storage fault) | aggregate(), classicBests(), recordResult | mitigate | CLOSED | `ProgressRepository.kt:216` `storageFailSafe(fallback = NewBests(false,false,false))` (write), `:271` `fallback = ProgressAggregate()`, `:309` `fallback = emptyMap()`; the three aggregate reads share one `withTransaction` at `:276`. Tests `ProgressAggregateTest.kt:344,355`; `ProgressRepositoryFailureTest.kt:106,124` | |
| 12-06/T-12-35 | Info Disclosure | DAOs, ProgressRepository | N/A | CLOSED-N/A | 0 logging calls in `app/src/main`; no player id / name / PGS ID held in `data/db` — verified by reading `PlaySessionEntity.kt` and `ProgressProjections.kt` | |
| 12-06/T-12-SC | supply chain | libs.versions.toml | N/A | CLOSED-N/A | catalog diff empty | |

## Plan 12-07 — `ProgressSubmitter`

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-07/T-12-70 | DoS | recordSolvedPlayOnce → onResultRecorded | mitigate | CLOSED | `data/ProgressSubmitter.kt:152-153` — `open fun onResultRecorded()` is non-suspend and its whole body is `scope.launch { submitAll(onlyIfPending = false) }` on `@ApplicationScope`; seam calls wrapped `:278-279` with cancellation rethrow. Call site `ui/results/ResultsViewModel.kt:435`, after `recordResult` returns and behind the `isSolved` gate at `:413` | |
| 12-07/T-12-71 | Info Disclosure | submitAll session gate | mitigate | CLOSED | `ProgressSubmitter.kt:175-177` — `if (session.value !is SignedIn) { setPending(true); return }` placed BEFORE any `progressRepository` read (`:184-186`). Tests `ProgressSubmitterTest.kt:85` (SignedOut), `:102` (Unknown) | `Unknown` provably takes the same branch |
| 12-07/T-12-72 | Tampering | concurrent passes racing the flag write | mitigate | CLOSED | `ProgressSubmitter.kt:169-170` `passMutex.withLock`; `:129` `started.compareAndSet(false, true)`; pending flag read INSIDE the lock at `:181` (WR-01 `4807c8a`). Tests `:440` double-start, `:461` overlapping passes serialize, `:484` sign-in during in-flight flag write | |
| 12-07/T-12-73 | Tampering | client-computed leaderboard scores | accept (residual) | CLOSED-accepted | PITFALLS.md:489; D-07 `12-CONTEXT.md:38`; recompute determinism verified in 12-06/T-12-33 row | |
| 12-07/T-12-74 | Info Disclosure | ID strings escaping into ui/ or logcat | mitigate | CLOSED | `grep -rn "GamesIds\|AchievementId\|submitScore\|unlockAchievement\|setAchievementSteps" app/src/main/java/com/app/pegsolitaire/ui/` → 0; `grep -rn "android.util.Log" app/src/main` → 0; `GamesIds` read only from `data/ProgressSubmitter.kt` and `platform/games/` | |
| 12-07/T-12-75 | Tampering | storage fault clearing the pending flag | mitigate | CLOSED | `ProgressSubmitter.kt:205-213` `recordPassOutcome` — `when { !allOk -> setPending(true); issued -> setPending(false); else -> Unit }`; `issued` derives from `results.isNotEmpty()` at `:193`. Test `ProgressSubmitterTest.kt:351` `aPassThatIssuedZeroCallsNeverClearsTheFlag` | |
| 12-07/T-12-76 | Tampering | forged SubmissionSyncStore value | accept | CLOSED-accepted | `data/PlayGamesPreferences.kt:43-54` KDoc records the fail-open default and its tradeoff; `const val DEFAULT_SUBMISSIONS_PENDING = false` at `:54`; no submitted value derives from the flag (`ProgressSubmitter.kt:181` uses it only to skip) | |
| 12-07/T-12-77 | Spoofing / Repudiation / Elevation | — | N/A | CLOSED-N/A | No credential handled in app code — sign-in is `PlayGames.getGamesSignInClient(...)` inside `platform/games` only; writes are per-signed-in-player and server-side | |
| 12-07/T-12-SC | supply chain | package installs | N/A | CLOSED-N/A | catalog diff empty | |

## Plan 12-08 — operator checklist + release gates

| id | category | component | disposition | status | evidence | notes |
|----|----------|-----------|-------------|--------|----------|-------|
| 12-08/T-12-08-01 | Info Disclosure | 12-OPERATOR-CHECKLIST.md tracked in git | mitigate | CLOSED | `grep -c CgkI .planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` → **0**; explicit prohibition at `12-OPERATOR-CHECKLIST.md:195-199` (Step 4 opening: never paste an ID into this file, `.planning/`, or `src/main`) | Also 0 `CgkI` anywhere under `app/src/main` |
| 12-08/T-12-08-02 | Tampering | release APK, gms.games.* under R8 | mitigate | CLOSED-gated | Gate documented: `12-HUMAN-UAT.md:42` and `11-HUMAN-UAT.md:60-61` (assembleRelease → `missing_rules.txt` must name no `com.google.android.gms.games`; re-run test 5 on the minified build); prerequisites stated at `11-HUMAN-UAT.md:32` | **Not yet executed.** Gate: a Play Console project supplying all 27 IDs (else `verifyReleaseGamesIds` fails) + release signing. Carry to Phase 14 |
| 12-08/T-12-08-03 | Spoofing | Play Console OAuth client / tester list | mitigate | CLOSED-gated | `12-OPERATOR-CHECKLIST.md:30` Step 0, items at `:43` (publish OAuth consent screen), `:56` (Play App Signing SHA-1, not the upload cert), `:61` (add yourself to the tester list) — all four failure modes ordered and explicit | Documentation control fully present; execution is operator-gated (Phase 14) |
| 12-08/T-12-08-04 | Repudiation / config integrity | published sort order, achievement type, step totals | accept (documented, one-way) | CLOSED-accepted | `12-OPERATOR-CHECKLIST.md:82` (leaderboard sort order / format / decimals IMMUTABLE) and `:119` (achievement type and step totals IMMUTABLE after publication, step totals must equal `AchievementThresholds`) | No code control possible |
| 12-08/T-12-08-SC | supply chain | npm/pip/cargo | N/A | CLOSED-N/A | Docs-only plan; catalog diff empty | |

---

## Post-plan review fixes — current-construct verification

| fix | commit | construct at HEAD | verdict |
|-----|--------|-------------------|---------|
| CR-01 Diamond-41 record gate | `80a4df7` | `ui/results/ResultsViewModel.kt:154-157` clamps `args.minimumPegs` into `[BoardConstants.MINIMUM_PEGS_DEFAULT, BoardConstants.HIGHEST_MINIMUM_PEGS]` (`domain/.../BoardConstants.kt:23,61`); `:173` `isSolved = args.pegsRemaining <= minimumPegs`; gates the record at `:413`; `ResultsUiState.kt:99-100` shares the same derivation. Tests `ResultsViewModelTest.kt:398,463,489` | Clamp ceiling is a board constant, never the play's own `pegsRemaining` — a forged huge `minimumPegs` cannot declare an unsolved board solved (test `:463`) |
| WR-01 pending flag under pass mutex | `4807c8a` | `ProgressSubmitter.kt:181` inside `passMutex.withLock` (`:170`) | Satisfies 12-07/T-12-72; strengthens it |
| WR-02 submissions fail fast without Activity | `ffbe012` | `PlayGamesService.kt:437-446` reads `activityProvider.current`, no await; stock UI keeps `withTimeoutOrNull(ACTIVITY_WAIT_MS)` at `:362-363` | Satisfies 12-03/T-12-32 with a tighter bound than planned |
| WR-03 negative assist counters refused | `027ea09` | `SolveTelemetry.kt:117` (nav boundary → `UNACCOUNTED_ASSIST`); `SnapshotMapper.kt:188,219-222` (blob boundary → refuse resume) | Satisfies 12-02/T-12-06 and 12-05/T-12-21; resolves tampering AGAINST the forger rather than toward zero |

## Unregistered flags (WARNING — not blockers)

1. **`minimumPegs` as a new untrusted `Results` nav arg.** `navigation/Routes.kt:128` was introduced by CR-01 (`80a4df7`) after the plans were written; no register row names it (12-05/T-12-20 names only the three telemetry args). It IS clamped at the same boundary (`ResultsViewModel.kt:154-157`) and pinned by two forged-arg tests (`ResultsViewModelTest.kt:463,489`), so it is mitigated-in-fact but unregistered.
2. **`12-01-SUMMARY.md` and `12-08-SUMMARY.md` carry no `## Threat Flags` section.** 12-01 substitutes `## Threat Model Disposition` (`:178-180`); 12-08 has neither. The claim "their `## Threat Flags` sections all say None" is true for 12-02..12-07 only. Process gap, no code impact.
3. **`gamesIdLiteralCheck` comment exemption.** `standards.gradle.kts:166-169` skips comment-prefixed lines, so an ID pasted into a source comment would not trip the gate that 12-04/T-12-40 relies on. Advisory narrowing.
4. **Register count discrepancy.** Prompt header says 55 rows; the supplied table and the eight plan blocks both enumerate 54.

## Residual warnings cross-referenced (from 12-REVIEW.md — not re-audited)

- IN-01 unreachable `null` branch in `submitLeaderboards`; IN-02 dead `CLASSIC_BOARD_TOTAL`; IN-03 `data` ↔ `platform.games` package cycle; IN-04 no in-flight guard on the stock-UI handlers (a double tap can launch two Intents — narrows nothing in 12-03/T-12-35, which concerns Intent content); IN-05 unchecked cast in `app/build.gradle.kts`. IN-06 closed by CR-01.
