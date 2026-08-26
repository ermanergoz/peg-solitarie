# Phase 6: Platform Foundation & Build Hardening - Context

**Gathered:** 2026-08-27
**Status:** Ready for planning

<domain>
## Phase Boundary

The app gains the platform plumbing, build safety, and security posture that every later SDK integration depends on — **with zero player-visible change**. Requirements: PLAT-01 through PLAT-07 plus DEBT-04.

This phase ships **no real SDK behavior**. It extends the seams, adds the Activity bridge, hardens the build, and closes two security advisories. Real ads (Phase 9), interstitials (Phase 10), and Play Games (Phases 11–13) consume what is built here.

The success bar: after this phase, `hint`, `undo`, and every existing surface behave exactly as they did in v1.0, while the new seams, gates, and build config are in place and green.

</domain>

<decisions>
## Implementation Decisions

### AdGateway seam shape (PLAT-02)

- **D-01:** `requestRewarded` returns a **sealed `RewardOutcome`** — `Granted` / `Declined` / `Unavailable` — replacing the current `Boolean`. The three-way distinction is required by ADS-03 (grant the assist anyway when no ad can be served) and a sealed type makes the compiler force every call site to handle the fail-open branch. Follows the project's existing sealed-state idiom (`GameStatus`).
- **D-02:** `NoOpAdGateway` returns **`Granted` unconditionally** in this phase. Phase 6's contract is byte-identical v1.0 behavior — hint and undo stay free, no dialog, no allowance. A configurable stub was considered and rejected: it would change debug behavior inside a phase whose success criterion is "no behavior change".
- **D-03:** `AdReason.RETRY` is **not added** — retry stays free permanently (locked at requirements time). The existing `AdReason` doc-comment that reserves `RETRY` for interstitials is stale and should be corrected while touching this file.
- **D-04 (binds Phase 9, not this phase):** the free per-level assist allowance lives in a **pure `:domain` policy object** (e.g. `AssistAllowance`) tracking hints/undos consumed in the current level — not in the gateway and not in `GameViewModel`. It must be unit-testable with zero Android dependencies, mirroring `ScoreCalculator` / `DifficultyTiers`. Phase 6 does not build it; Phase 9 must not put this logic anywhere else.

### Debug vs release SDK binding (PLAT-06, PLAT-07)

- **D-05:** Debug builds use **Google's test ad units**; release builds use the real ones. Real ad units on a development device is what gets AdMob accounts suspended for invalid traffic. This also means the ad path (including no-fill handling) is exercisable during development rather than only in release.
- **D-06:** The split is implemented with **build types** (`debug` / `release`) carrying different `buildConfigField` values — not product flavors and not per-source-set Hilt modules. The project has exactly two variants today and already uses a debug-only source-set tweak for the Room schema assets; adding a flavor dimension would double the variant matrix and complicate that wiring.
- **D-07:** The SDKs' auto-init ContentProviders (`MobileAdsInitProvider`, `PlayGamesInitProvider`) are **removed via manifest merge** (`tools:node="remove"`), with initialization performed explicitly in code. This is required twice over: it keeps the JVM test suite SDK-free (PLAT-07) and it is the only way to satisfy CONS-02, which forbids any ad request before consent resolves.

### ID and secret handling (PLAT-06)

- **D-08:** Real AdMob and Play Games IDs live in an **untracked local file (`local.properties`) plus a CI secret** — never committed. They are not cryptographic secrets (they ship inside the APK), but committing them lets a forked repo serve ads against this AdMob account. A fresh clone must build successfully with test IDs and must never accidentally serve real ads.
- **D-09:** A **release build fails at configuration time** if real IDs are missing or still placeholders, with an error naming the missing property. A warning was rejected — a warning in a long Gradle log is exactly what gets missed before a store upload. Matches the project's fail-fast `requireNotNull` idiom in `LevelRepository`.
- **D-10:** The **`INTERNET` permission is added in this phase**, alongside the manifest and build work. The app currently declares no permissions at all. The permission is inert until an SDK uses it, and adding it here means the foundation phase leaves a build that can actually reach the network.

### R8 and build hardening (PLAT-05)

- **D-11:** Release builds use **R8 full mode with `shrinkResources` enabled**. The project has no `buildTypes` block at all today, so release is currently unminified — and the ads SDK alone adds ~5.5 MB before transitives. Room, Hilt, and kotlinx-serialization all publish consumer keep rules, so the keep-rule surface should be small; any rule that has to be hand-written must be commented with why.
- **D-12:** APK size is **baselined, not gated**. Record the pre-SDK release APK size in this phase so Phase 14 can compare, but do not fail builds on size while the SDKs are still landing. Also capture the **pre-SDK cold-start baseline** here — PLAT-08 in Phase 14 measures against it.

### Security advisories (DEBT-04)

- **D-13:** WARN-02 (resume-snapshot footprint coordinates unbounded) and WARN-03 (raw route `challengeLevel` reaching Results unclamped on the non-restore path) are both closed by **bounding/validating at the boundary — decode boundary and route boundary respectively — each with a regression test proving a forged or out-of-range value cannot get through**. Fixes without tests were rejected: the v1.0 audit's finding was precisely that these paths lacked proof. This is the same fail-safe pattern the resume codec already applies to stored moves.

### Claude's Discretion

- The exact naming and file placement of the sealed outcome type, the Activity provider, and the build-config plumbing.
- The `ForegroundActivityProvider` mechanism was not discussed in detail — research recommends a `WeakReference` + `StateFlow` holder fed by `Application.registerActivityLifecycleCallbacks`, and that recommendation stands unless planning finds a reason to deviate. The binding constraint is fixed regardless: **the Activity must be reachable from platform-layer code and from nowhere else** — no ViewModel and no `:domain` class may obtain one (PLAT-01).
- The `GamesService` extension shape (PLAT-03) was not discussed. Constraint from requirements: it must cover achievements and the stock Play Games UI intents, with the No-Op implementation degrading silently. Saved Games stays **off** this interface — it arrives behind `ProgressRepository` in Phase 13, as the existing seam KDoc already specifies.
- How the extended purity gate expresses "gms/ump only inside `platform/`" (PLAT-04) — the existing `DomainPurityTest` forbids `android..`/`androidx..` but has no equivalent rule for `com.google.android.gms..` / `com.google.android.ump..`.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope and decisions
- `.planning/REQUIREMENTS.md` — the 56 v1.1 requirements and the **Milestone Decisions** table (ads SDK choice, retry-stays-free, per-level allowance, cloud-save scope). PLAT-01..07 and DEBT-04 are this phase's.
- `.planning/ROADMAP.md` — Phase 6 goal, success criteria, notes, and the **External Prerequisites** table (start those on day 1 of this phase).
- `.planning/PROJECT.md` — project constraints, Key Decisions table, current state.

### Research (all four dimensions apply to this phase)
- `.planning/research/SUMMARY.md` — reconciled findings and the 8-phase build order.
- `.planning/research/STACK.md` — verified dependency graph, compileSdk-36 safety analysis, exact SDK versions, and the R8/`INTERNET` gaps found in this repo.
- `.planning/research/ARCHITECTURE.md` — the `ForegroundActivityProvider` pattern, integration points, anti-patterns.
- `.planning/research/PITFALLS.md` — the 15 pitfalls; six of them are prevented by this phase specifically. Also flags Robolectric 4.16 ContentProvider behavior as needing empirical verification.

### Existing code this phase modifies
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt` — the seam being extended (PLAT-02). Its KDoc's "Slice 2 swaps only the Hilt binding" promise is **false as written** and must be corrected: every real SDK call needs an Activity.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdReason.kt` — the stale `RETRY` reservation lives here.
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt` — the seam being extended (PLAT-03).
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` — the `@Binds` swap point.
- `app/build.gradle.kts` — has **no `buildTypes` block**; note the existing debug-source-set Room schema asset wiring and its `mergeDebugAssets`/`copyRoomSchemas` task dependency before adding build types.
- `app/src/main/AndroidManifest.xml` — declares **no permissions**; needs `INTERNET` (D-10) and the provider removals (D-07).

### Security advisories (DEBT-04)
- `.planning/milestones/v1.0-MILESTONE-AUDIT.md` — records WARN-02/WARN-03 as carried debt.
- The v1.0 Phase 5 `05-SECURITY.md` artifact — the original advisory text (under `.planning/milestones/v1.0-phases/`).

### Project standards
- `CLAUDE.md` — code standards (no magic numbers, dimens, detekt/ktlint gates), Gitflow. **Note:** its stack table still pins `play-services-ads 23.6.0`, which the milestone decision supersedes with GMA Next-Gen `ads-mobile-sdk` 1.4.0. Updating that table is reasonable in-phase housekeeping.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **The seam pattern itself**: `interface` + `@Inject constructor` impl + `@Binds @Singleton` in a `@InstallIn(SingletonComponent::class)` module. Both `AdGateway` and `GamesService` already follow it, mirroring `LevelRepository`. New platform components should match this shape.
- **Sealed state idiom**: `GameStatus` establishes sealed types for exhaustive branching — the model for `RewardOutcome` (D-01).
- **Pure `:domain` policy objects**: `ScoreCalculator`, `DifficultyTiers`, `ScoreParams` — the model for the Phase 9 `AssistAllowance` (D-04).
- **Fail-fast and fail-open precedents**: `requireNotNull` in `LevelRepository` (fail-fast on programmer error) versus the resume codec's fail-safe decode and DataStore's fail-open reads. D-09 follows the former; D-13 follows the latter.
- **Room `MigrationTestHelper` harness** already runs JVM-side via the debug-assets schema wiring — proven infrastructure for Phase 12's telemetry migration.

### Established Patterns
- **Domain purity is CI-enforced** (`DomainPurityTest`) and must stay that way; PLAT-04 extends it rather than replacing it.
- **All quality gates run in `check`**: detekt, ktlint, the custom no-dp/sp gate, ArchUnit purity, and 293 JVM tests. Anything added in this phase must keep `./gradlew check` green **and offline**.
- **Two variants only** (`debug`/`release`), with a debug-only asset source-set tweak — the reason D-06 chose build types over flavors.

### Integration Points
- `PlatformModule` is where the Activity provider, extended seams, and (later) real implementations all bind.
- `GameViewModel` is the only current consumer of `AdGateway.requestRewarded` (via `onUndo` and `onHintRequested`); the `Boolean` → `RewardOutcome` change lands there. **Every `false`-grant branch in the codebase has never executed**, because the stub always returns `true` — treat those paths as untested code, not proven code.
- `App` (the `@HiltAndroidApp` Application class) is where `registerActivityLifecycleCallbacks` and explicit SDK initialization will live.

</code_context>

<specifics>
## Specific Ideas

- The phrase that should govern review of this phase: **"a fresh clone builds with test IDs and never accidentally serves real ads."** (D-08)
- Phase 6 is explicitly allowed to be boring. If a reviewer can see any difference as a player, something is wrong.

</specifics>

<deferred>
## Deferred Ideas

- **`AssistAllowance` implementation** — decided here (D-04) but built in Phase 9 with ADS-02.
- **Rewarded opt-in dialog** — Phase 9; must be built and tested against the still-in-place No-Op stub *before* the real ad SDK is wired in.
- **Interstitial cadence policy** — Phase 10.
- **Saved Games / cloud save** — Phase 13, behind `ProgressRepository`, never on the `GamesService` interface.
- **Cold-start budget enforcement** — Phase 14 (PLAT-08); Phase 6 only captures the baseline.
- **APK size gate** — deliberately not adopted now (D-12); revisit at Phase 14 if size proves to be a problem.

</deferred>

---

*Phase: 6-Platform Foundation & Build Hardening*
*Context gathered: 2026-08-27*
