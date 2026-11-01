# Phase 6: Platform Foundation & Build Hardening — Research

**Researched:** 2026-08-27
**Domain:** Android build hardening (AGP 9.1.0 buildTypes / R8 / manifest merge), Hilt platform seams, ArchUnit boundary gates, input-boundary validation
**Confidence:** HIGH — the majority of findings in this document were verified **empirically against this repository and against the actual published artifacts**, not from prose.

---

## Summary

Phase 6 has eight requirements, and this research resolved the nine specific unknowns the orchestrator flagged. Five of them turned out to have answers that **contradict the milestone-level research** — in every case because the milestone documents reasoned from the *legacy* AdMob SDK or from general Android folklore rather than from this toolchain and these artifacts. The corrections are load-bearing:

1. **R8 does not work out of the box on this repo.** Enabling `isMinifyEnabled` today fails the build with `Missing class com.google.errorprone.annotations.CanIgnoreReturnValue` (referenced from Hilt's `ActivityRetainedComponentBuilder`). Exactly one hand-written rule fixes it. With that rule the release APK goes **9,890,332 B → 1,518,323 B (−84.6%)**, and the downloadable-font cert arrays survive `shrinkResources`. All measured in this repo this session.
2. **Enabling minification pulls a new artifact onto the build classpath** — `org.jetbrains.kotlin:compose-group-mapping:2.3.20`, required by AGP 9.1.0's `produceReleaseComposeMapping` task. It was **not** in the local Gradle cache, so the first minified build after this change fails under `--offline`. That directly threatens the "check green and offline" invariant.
3. **The GMA Next-Gen SDK declares no `MobileAdsInitProvider`.** Its AAR manifest (downloaded and read this session) declares only `AdActivity`, four permissions, and `<queries>`. There is nothing to `tools:node="remove"`. Only `PlayGamesInitProvider` is real.
4. **Robolectric 4.16 does not instantiate manifest-declared ContentProviders at startup.** Verified by bytecode inspection: providers are created *lazily*, only when a `ContentResolver` call resolves their authority. PITFALLS flagged this UNVERIFIED; it is now settled and **no spike task is needed**.
5. **`play-services-games-v2:22.0.0` ships no consumer ProGuard rules** (no `proguard.txt` in the AAR), contradicting STACK's claim that "both SDKs ship consumer rules". It relies on `play-services-basement`'s rules transitively, which do cover its SafeParcelable reflection — so the conclusion still holds, but for a different reason.

The rest of the phase is mechanical but has sharp edges: `buildFeatures { buildConfig = true }` is mandatory (default is `false`, verified in the AGP 9.1.0 DSL source); AGP 9 only creates a unit-test component for the *tested* build type, so a "release variant test" cannot be written without extra wiring; and R8 **strict** full mode is on by default in AGP 9.1.0, which changes what `-keep class A` means.

**Primary recommendation:** Sequence the phase as (1) seams + Activity bridge + gates — pure Kotlin, no build changes; (2) DEBT-04 boundary fixes; (3) build hardening last, because that is where the offline/CI risk lives. Capture the **9,890,332 B** unminified baseline recorded below as the D-12 pre-SDK number rather than re-measuring.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**AdGateway seam shape (PLAT-02)**

- **D-01:** `requestRewarded` returns a **sealed `RewardOutcome`** — `Granted` / `Declined` / `Unavailable` — replacing the current `Boolean`. The three-way distinction is required by ADS-03 (grant the assist anyway when no ad can be served) and a sealed type makes the compiler force every call site to handle the fail-open branch. Follows the project's existing sealed-state idiom (`GameStatus`).
- **D-02:** `NoOpAdGateway` returns **`Granted` unconditionally** in this phase. Phase 6's contract is byte-identical v1.0 behavior — hint and undo stay free, no dialog, no allowance. A configurable stub was considered and rejected: it would change debug behavior inside a phase whose success criterion is "no behavior change".
- **D-03:** `AdReason.RETRY` is **not added** — retry stays free permanently (locked at requirements time). The existing `AdReason` doc-comment that reserves `RETRY` for interstitials is stale and should be corrected while touching this file.
- **D-04 (binds Phase 9, not this phase):** the free per-level assist allowance lives in a **pure `:domain` policy object** (e.g. `AssistAllowance`) tracking hints/undos consumed in the current level — not in the gateway and not in `GameViewModel`. It must be unit-testable with zero Android dependencies, mirroring `ScoreCalculator` / `DifficultyTiers`. Phase 6 does not build it; Phase 9 must not put this logic anywhere else.

**Debug vs release SDK binding (PLAT-06, PLAT-07)**

- **D-05:** Debug builds use **Google's test ad units**; release builds use the real ones. Real ad units on a development device is what gets AdMob accounts suspended for invalid traffic. This also means the ad path (including no-fill handling) is exercisable during development rather than only in release.
- **D-06:** The split is implemented with **build types** (`debug` / `release`) carrying different `buildConfigField` values — not product flavors and not per-source-set Hilt modules. The project has exactly two variants today and already uses a debug-only source-set tweak for the Room schema assets; adding a flavor dimension would double the variant matrix and complicate that wiring.
- **D-07:** The SDKs' auto-init ContentProviders (`MobileAdsInitProvider`, `PlayGamesInitProvider`) are **removed via manifest merge** (`tools:node="remove"`), with initialization performed explicitly in code. This is required twice over: it keeps the JVM test suite SDK-free (PLAT-07) and it is the only way to satisfy CONS-02, which forbids any ad request before consent resolves.

**ID and secret handling (PLAT-06)**

- **D-08:** Real AdMob and Play Games IDs live in an **untracked local file (`local.properties`) plus a CI secret** — never committed. They are not cryptographic secrets (they ship inside the APK), but committing them lets a forked repo serve ads against this AdMob account. A fresh clone must build successfully with test IDs and must never accidentally serve real ads.
- **D-09:** A **release build fails at configuration time** if real IDs are missing or still placeholders, with an error naming the missing property. A warning was rejected — a warning in a long Gradle log is exactly what gets missed before a store upload. Matches the project's fail-fast `requireNotNull` idiom in `LevelRepository`.
- **D-10:** The **`INTERNET` permission is added in this phase**, alongside the manifest and build work. The app currently declares no permissions at all. The permission is inert until an SDK uses it, and adding it here means the foundation phase leaves a build that can actually reach the network.

**R8 and build hardening (PLAT-05)**

- **D-11:** Release builds use **R8 full mode with `shrinkResources` enabled**. The project has no `buildTypes` block at all today, so release is currently unminified — and the ads SDK alone adds ~5.5 MB before transitives. Room, Hilt, and kotlinx-serialization all publish consumer keep rules, so the keep-rule surface should be small; any rule that has to be hand-written must be commented with why.
- **D-12:** APK size is **baselined, not gated**. Record the pre-SDK release APK size in this phase so Phase 14 can compare, but do not fail builds on size while the SDKs are still landing. Also capture the **pre-SDK cold-start baseline** here — PLAT-08 in Phase 14 measures against it.

**Security advisories (DEBT-04)**

- **D-13:** WARN-02 (resume-snapshot footprint coordinates unbounded) and WARN-03 (raw route `challengeLevel` reaching Results unclamped on the non-restore path) are both closed by **bounding/validating at the boundary — decode boundary and route boundary respectively — each with a regression test proving a forged or out-of-range value cannot get through**. Fixes without tests were rejected: the v1.0 audit's finding was precisely that these paths lacked proof. This is the same fail-safe pattern the resume codec already applies to stored moves.

### Claude's Discretion

- The exact naming and file placement of the sealed outcome type, the Activity provider, and the build-config plumbing.
- The `ForegroundActivityProvider` mechanism was not discussed in detail — research recommends a `WeakReference` + `StateFlow` holder fed by `Application.registerActivityLifecycleCallbacks`, and that recommendation stands unless planning finds a reason to deviate. The binding constraint is fixed regardless: **the Activity must be reachable from platform-layer code and from nowhere else** — no ViewModel and no `:domain` class may obtain one (PLAT-01).
- The `GamesService` extension shape (PLAT-03) was not discussed. Constraint from requirements: it must cover achievements and the stock Play Games UI intents, with the No-Op implementation degrading silently. Saved Games stays **off** this interface — it arrives behind `ProgressRepository` in Phase 13, as the existing seam KDoc already specifies.
- How the extended purity gate expresses "gms/ump only inside `platform/`" (PLAT-04) — the existing `DomainPurityTest` forbids `android..`/`androidx..` but has no equivalent rule for `com.google.android.gms..` / `com.google.android.ump..`.

### Deferred Ideas (OUT OF SCOPE)

- **`AssistAllowance` implementation** — decided here (D-04) but built in Phase 9 with ADS-02.
- **Rewarded opt-in dialog** — Phase 9; must be built and tested against the still-in-place No-Op stub *before* the real ad SDK is wired in.
- **Interstitial cadence policy** — Phase 10.
- **Saved Games / cloud save** — Phase 13, behind `ProgressRepository`, never on the `GamesService` interface.
- **Cold-start budget enforcement** — Phase 14 (PLAT-08); Phase 6 only captures the baseline.
- **APK size gate** — deliberately not adopted now (D-12); revisit at Phase 14 if size proves to be a problem.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PLAT-01 | Activity-scoped bridge makes the foreground Activity available to ad/consent/Play Games impls without leaking into ViewModels or `:domain` | §Pattern 1 (`ForegroundActivityProvider` concrete shape, Hilt scoping trap), §Pattern 5 (structural enforcement: seam signatures + two ArchUnit rules). Verified: `ui/` has **zero** `Activity` references today, so the rule passes on day 1. |
| PLAT-02 | `AdGateway` extended to express offer / accept / decline / unavailable, No-Op still auto-granting | §Pattern 2 (`RewardOutcome` shape + the `Unavailable → proceed` mapping ADS-03 requires), §Seam Migration Blast Radius (complete call-site inventory: 2 production, 6 test files) |
| PLAT-03 | `GamesService` extended to cover achievements and stock Play Games UI intents; No-Op degrades silently | §Pattern 3 (proposed interface; the Activity bridge removes the need for `ActivityResultLauncher` plumbing in the UI layer) |
| PLAT-04 | Domain-purity CI gate additionally forbids `com.google.android.gms` / `com.google.android.ump` outside the platform layer | §Pattern 4 (concrete `ArchRule` shapes, ArchUnit scoping + `failOnEmptyShould` traps, Hilt-generated-code hazard), §Validation Architecture (deliberately-violating fixture proving the gate bites) |
| PLAT-05 | Release builds minified with R8, keep rules verified | §R8: What This Repo Actually Needs — **measured**: one rule required, 84.6% size reduction, `compose-group-mapping` offline blocker, AGP 9.1.0 strict-full-mode semantics |
| PLAT-06 | Ad unit IDs and Play Games IDs are build-config values, test in debug / real in release, no ID literals in source | §Build Types and BuildConfig (AGP 9.1.0-verified DSL, `buildConfig = true` mandatory), §Reading Untracked Properties (D-08/D-09 pattern + the "which build am I?" problem) |
| PLAT-07 | Full JVM test suite stays green and SDK-free; auto-init providers isolated from tests | §Manifest Merge and Auto-Init Providers — Robolectric 4.16 behaviour **settled empirically**; only `PlayGamesInitProvider` exists; exact removal syntax + a merged-manifest assertion test |
| DEBT-04 | Security advisories WARN-02 and WARN-03 closed | §DEBT-04 (exact code paths located, tightest sound bound derived, one-line WARN-03 fix, regression-test shapes) |
</phase_requirements>

---

## Project Constraints (from CLAUDE.md)

Directives the planner must not contradict:

| Directive | Bearing on this phase |
|-----------|----------------------|
| No magic numbers or strings in logic/UI; scoped constants objects, not one global dump | Ad unit IDs are the documented **exception** — they must be `buildConfigField`, not a constants object (Pitfall 3). Everything else new (coordinate bounds, provider names, timeouts) goes into scoped objects: `ResumeSnapshotFormat`, a new `AdConstants` / `GamesIds` under `platform/`. |
| Compose state hoisting; state owned by ViewModels | PLAT-01 must not push an `Activity` through composable lambdas. This is the reason `ForegroundActivityProvider` exists rather than `LocalActivity` threading. |
| Dimens resource file for all dp/sp | Phase 6 adds **no UI**, so `dpSpLiteralCheck` is not exercised by new code. It must still pass. |
| detekt + ktlint passing as a build gate, not optional | New code must clear `MagicNumber` (config at `config/detekt/detekt.yml`) and ktlint format. A 7-method `ActivityLifecycleCallbacks` implementation is within detekt's `TooManyFunctions` default. **No new `@Suppress` and no detekt baseline** (Pitfall 14). |
| Clean Architecture; pure-Kotlin `:domain` with zero Android deps | `ForegroundActivityProvider`, `RewardOutcome`, and the extended `GamesService` all live in `:app`'s `platform/` package. Nothing new belongs in `:domain` this phase. |
| Gitflow strictly — feature branches off `develop` | Current branch is `feature/ui-clay-redesign`; STATE says the clay redesign merged to `develop` on 2026-08-27. Phase branch template is `gsd/phase-{phase}-{slug}`. Branch off `develop`. |
| CLAUDE.md stack table pins `play-services-ads 23.6.0` | **Superseded** by the milestone decision (GMA Next-Gen `ads-mobile-sdk` 1.4.0). CONTEXT explicitly permits fixing this table in-phase. Also stale in that table: `play-services-games-v2 ~21.x` (actual 22.0.0) and Hilt `2.57.1` (catalog already at 2.60). |

**Project skills:** none. `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, `.codex/skills/` do not exist in this repo. The `compose-skill` referenced by CLAUDE.md is not present as a discoverable project skill; treat its state-hoisting mandate as the binding constraint.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Foreground Activity acquisition | Platform (`platform/activity/`) | Application (`App.onCreate`) | Activity lifetime is an Android-runtime concern. `App` is the only place `registerActivityLifecycleCallbacks` can be called; the provider is the only place a reference is held. |
| Rewarded-grant decision (`RewardOutcome`) | Platform (`platform/ads/`) | — | The fail-open policy (ADS-03) belongs *inside* the gateway, not in `GameViewModel`. The ViewModel only branches on the outcome. |
| Assist-proceed / assist-skip branching | ViewModel (`ui/game/GameViewModel`) | — | Unidirectional-data-flow state transition; no Android or SDK types involved. |
| Play Games session + achievements + UI intents | Platform (`platform/games/`) | — | Every PGS client getter needs an `Activity`; keeping it in platform is what lets `ui/` stay Activity-free. |
| Build-time ID selection (test vs real) | Build (`app/build.gradle.kts` + `local.properties`/env) | Platform (reads `BuildConfig`) | An ID that varies by variant cannot live in source (D-06/D-08). Only platform code may read `BuildConfig` ad fields. |
| Manifest surface (permissions, provider removal, meta-data) | Build/manifest (`app/src/main/AndroidManifest.xml`) | — | Merge-order concern; no runtime tier owns it. |
| Snapshot decode validation (WARN-02) | Data (`data/resume/`) | `:domain` (`Position` type) | The decode boundary is the trust boundary; `:domain` must not learn about untrusted input. |
| Route-arg clamping (WARN-03) | ViewModel (`GameViewModel` / `ResultsViewModel`) | `:domain` (`GeneratorConstants.MAX_CHALLENGE_LEVEL`) | Nav args are untrusted input arriving at the ViewModel; the bound itself is a domain constant. |
| Boundary gates (purity, gms containment) | Test (`app/src/test/…`, ArchUnit) | Build (`check` lifecycle) | Bytecode-level enforcement, already the established mechanism (`DomainPurityTest`). |

---

## Verified Repository Baseline

Everything in this table was read or executed in this repo on 2026-08-27. It supersedes any conflicting statement in the milestone research.

| Fact | Value | Evidence |
|------|-------|----------|
| Test suite size | **299 JVM tests**, 47 test files, `app/src/{main,test}` only (no `androidTest`) | `app/build/test-results/testDebugUnitTest/*.xml` sum |
| `app/build.gradle.kts` build types | **None declared.** No `buildTypes` block, no `.pro` files anywhere | file read; `ls app/*.pro` → no matches |
| `buildFeatures` | `compose = true` only (set in `build-logic/src/main/kotlin/android-app.gradle.kts`). **`buildConfig` is not enabled.** | convention plugin read |
| Release build today | Succeeds offline; produces `app-release-unsigned.apk`, **9,890,332 B (9.43 MiB)**, no signing config, `copyRoomSchemas` NO-SOURCE on release | `./gradlew :app:assembleRelease --offline` run this session |
| Configuration cache | **Off** (`org.gradle.configuration-cache=false`, gradle.properties comment cites `dpSpLiteralCheck` + detekt 2.0 alpha) | `gradle.properties` |
| Gradle / AGP / Kotlin | Gradle **9.5.1** wrapper, AGP **9.1.0**, Kotlin **2.3.20**, JDK 17 toolchain, compileSdk/targetSdk **36**, minSdk **24** | wrapper properties, catalog, convention plugin |
| `local.properties` | Exists, gitignored, contains only `sdk.dir` | file read |
| Manifest | Declares **zero permissions**; no `xmlns:tools`; single `MainActivity`; `android:allowBackup="true"` | `app/src/main/AndroidManifest.xml` |
| `App.kt` | `@HiltAndroidApp class App : Application()` — **empty body, no `onCreate` override** | file read |
| `ui/` package Activity references | **Zero** (`grep -rn "Activity" app/src/main/java/com/app/pegsolitaire/ui/` → 0 files) | grep |
| Reflection in `:app` main | Exactly one site: `PegSolitaireDatabase::class.java` in `DatabaseModule.kt:43` (covered by Room's consumer rule) | grep for `Class.forName`/`newInstance`/`::class.java`/`getDeclaredField` |
| `check` gate composition | `detekt`, `ktlintCheck`, `dpSpLiteralCheck` (custom file scan over `**/ui/**`), plus AGP's `testDebugUnitTest` | `build-logic/src/main/kotlin/standards.gradle.kts` |
| `GameStatus` | An **`enum class`**, not a sealed type | `domain/…/model/GameStatus.kt` |

> ⚠️ **Correction to CONTEXT D-01's rationale.** D-01 cites `GameStatus` as "the project's existing sealed-state idiom". `GameStatus` is an `enum class`. The decision to make `RewardOutcome` sealed still stands (locked, and it buys future payloads such as `Unavailable(cause)`), but the planner should not expect to find an existing sealed precedent to copy.

---

## Standard Stack

No new *runtime* libraries are required by Phase 6's own eight requirements. What follows is (a) the build-side surface being turned on, and (b) the SDK artifacts that Phase 6 may put on the dependency graph (see Open Question 1).

### Core — build-side, no new artifacts

| Facility | Where | Purpose | Why standard |
|----------|-------|---------|--------------|
| `buildFeatures { buildConfig = true }` | `app/build.gradle.kts` | Enables `BuildConfig` generation | **Mandatory.** `BuildFeatures.buildConfig` KDoc in the AGP 9.1.0 DSL sources: *"Default value is `false`."* Without it, `buildConfigField` silently produces nothing. `[VERIFIED: gradle-api-9.1.0-sources.jar → com/android/build/api/dsl/BuildFeatures.kt:47-53]` |
| `buildTypes { debug {} release {} }` | `app/build.gradle.kts` `android {}` | Per-type `buildConfigField` / `resValue` / minify | D-06. `buildConfigField(type, name, value)` lives on `VariantDimension`, inherited by every build type and flavor. `[VERIFIED: VariantDimension.kt:180]` |
| `isMinifyEnabled` / `isShrinkResources` / `proguardFiles(...)` | `buildTypes.release` | R8 + resource shrink | Stable DSL on `BuildType`. `[VERIFIED: BuildType.kt:116,123; VariantDimension.kt:79-100]` |
| `getDefaultProguardFile("proguard-android-optimize.txt")` | `ApplicationExtension` | AGP's baseline keep rules | `[VERIFIED: CommonExtension.kt:526]` |
| `resValue("string", …)` | `buildTypes.{debug,release}` | Per-type string resources referenced from the manifest | Manifest `meta-data` cannot read `BuildConfig`. PGS `APP_ID` **must** be `@string/...` because a bare numeric `android:value` is parsed as an integer. `[CITED: developers.google.com/games/services/android/troubleshooting]` |
| `tools:node="remove"` | `app/src/main/AndroidManifest.xml` | Strip library-declared auto-init providers | D-07. No-op when the target element is absent; markers are stripped from the merged output for app modules. `[CITED: developer.android.com/build/manage-manifests]` |

### Supporting — SDK artifacts (only if Open Question 1 resolves "add now")

| Artifact | Version | Purpose | Verified this session |
|----------|---------|---------|----------------------|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` | 1.4.0 | GMA Next-Gen (rewarded + interstitial) | AAR downloaded from `dl.google.com`: 5,811,670 B, **ships `proguard.txt` (5,634 B)**, manifest declares `AdActivity` + INTERNET/ACCESS_NETWORK_STATE/READ_BASIC_PHONE_STATE/AD_ID + `<queries>`, **no ContentProvider**, and already applies `tools:node="remove"` to `RECEIVE_BOOT_COMPLETED` itself |
| `com.google.android.ump:user-messaging-platform` | 4.0.0 | Consent | AAR downloaded: 408,905 B, ships `proguard.txt` (306 B, one proto-field keep rule) |
| `com.google.android.gms:play-services-games-v2` | 22.0.0 | Play Games | AAR downloaded: 424,790 B, **NO `proguard.txt`**; manifest declares `PlayGamesInitProvider` (authority `${applicationId}.playgamesinitprovider`, `initOrder=99`), `GamesResolutionActivity` (not exported), **`PlayGamesAppShortcutsActivity` (`android:exported="true"`)**, and a `<queries>` block |
| `com.google.android.gms:play-services-basement` | 18.9.0 | transitive floor for ads + games + UMP | AAR downloaded: ships `proguard.txt` (2,745 B) covering SafeParcelable/`ReflectedParcelable`/`@Keep`/DynamiteApi reflection — **this is what actually protects `play-services-games-v2`** |
| `com.google.android.gms:play-services-base` | 18.9.0 | transitive | AAR downloaded: ships `proguard.txt` (268 B) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.11.0 (`version.ref = coroutines`) | `Task<T>` → suspend | Not needed until Phase 11; listed for catalog completeness |

### Alternatives Considered

| Instead of | Could use | Tradeoff |
|------------|-----------|----------|
| `isMinifyEnabled` / `isShrinkResources` | AGP 9's `optimization { enable = true; packageScope; keepRules { … } }` block | `Optimization` and `KeepRules` are `@Incubating` in AGP 9.1.0 `[VERIFIED: Optimization.kt, KeepRules.kt]`. `keepRules.ignoreFrom(...)` is genuinely useful for suppressing a bad library rule later, but do not build the phase on an incubating API. Use the stable properties. |
| `resValue` for IDs referenced from the manifest | `manifestPlaceholders["admobAppId"] = …` | Placeholders work for the AdMob app ID (`ca-app-pub-…~…` starts with a letter) but **not** for the numeric PGS project ID. Using `resValue` for both keeps one mechanism. `manifestPlaceholders` is on `VariantDimension` if the planner prefers it for AdMob. `[VERIFIED: VariantDimension.kt:132]` |
| Build types (D-06, locked) | Product flavor dimension `play`/`nosdk` (PITFALLS' "structural" recommendation) | Rejected by D-06. The Robolectric finding below removes the main justification for flavors anyway — the test suite was never at risk from providers. |
| ArchUnit rule for the gms containment | detekt `ForbiddenImport` | detekt is source-parsing and cannot see Hilt/KSP-generated code; ArchUnit reads bytecode and already runs in `check`. Extend ArchUnit. (A detekt `ForbiddenImport` entry is a cheap *additional* belt for hand-written source, but not the gate.) |
| `sealed interface RewardOutcome` (D-01, locked) | `enum class RewardOutcome` | Both give exhaustive `when`. Sealed is locked and allows `Unavailable(cause)` later. |

**Installation:** no `npm`/`pip` step. Gradle catalog additions only, if Open Question 1 resolves "add now" — copy the `[versions]`/`[libraries]` block verbatim from `.planning/research/STACK.md` §Installation.

**Version verification:** every Maven coordinate above was confirmed to resolve and download from `https://dl.google.com/dl/android/maven2` on 2026-08-27 (HTTP 200, byte sizes recorded). AGP/Gradle/Kotlin/Robolectric versions were read out of the local Gradle cache, i.e. the exact bytes this build uses.

---

## Package Legitimacy Audit

This phase installs no npm/PyPI/crates packages. Its dependency additions (if any) are Maven artifacts from Google's own repository, which `slopcheck` does not cover.

`slopcheck` was **not run** — the tool targets npm/PyPI/crates ecosystems and has no Maven backend, so running it would produce no signal. In place of it, every artifact was verified by the strongest available method: **direct download from the canonical first-party repository and inspection of the AAR contents.**

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0` | Google Maven | released 2026-08-20 | n/a (Google Maven publishes no counts) | first-party (Google) | n/a — Maven | Approved — AAR downloaded 2026-08-27, 5,811,670 B, contents inspected |
| `com.google.android.ump:user-messaging-platform:4.0.0` | Google Maven | released 2025-10-31 | n/a | first-party (Google) | n/a — Maven | Approved — AAR downloaded, 408,905 B |
| `com.google.android.gms:play-services-games-v2:22.0.0` | Google Maven | released 2026-07-29 | n/a | first-party (Google) | n/a — Maven | Approved — AAR downloaded, 424,790 B |
| `com.google.android.gms:play-services-basement:18.9.0` | Google Maven | transitive | n/a | first-party (Google) | n/a — Maven | Approved — AAR downloaded, 439,122 B |
| `com.google.android.gms:play-services-base:18.9.0` | Google Maven | transitive | n/a | first-party (Google) | n/a — Maven | Approved — AAR downloaded |
| `org.jetbrains.kotlin:compose-group-mapping:2.3.20` | Maven Central / Google | matches Kotlin pin | n/a | JetBrains | n/a — Maven | **Approved but flagged** — pulled implicitly by AGP 9.1.0 when minification is on; not in `libs.versions.toml`, not in the local cache (see §R8) |

**Packages removed due to slopcheck `[SLOP]` verdict:** none.
**Packages flagged as suspicious `[SUS]`:** none. `compose-group-mapping` is flagged for a different reason (invisible/implicit acquisition), not legitimacy.

Typosquatting is a genuine risk in this space — `com.google.android.libraries.ads.mobile.sdk` vs the legacy `com.google.android.gms:play-services-ads` are easy to conflate. The mitigation used here is that each coordinate was resolved against `dl.google.com` directly rather than trusted from memory.

---

## Architecture Patterns

### System Architecture Diagram

```
                      COLD START
                          │
                          ▼
              ┌───────────────────────────┐
              │ App.onCreate()            │  @HiltAndroidApp — currently EMPTY BODY
              │  (NEW: needs an override) │
              └───────┬───────────────────┘
                      │ registerActivityLifecycleCallbacks(provider)
                      ▼
     ┌────────────────────────────────────────────┐
     │ ForegroundActivityProvider  @Singleton      │◄── Android runtime pushes
     │  MutableStateFlow<WeakReference<Activity>?> │    onActivityResumed / Paused /
     │  suspend awaitActivity(): Activity          │    Destroyed
     └───────┬────────────────────────────────────┘
             │  injected ONLY into platform impls
             │  (ArchUnit-enforced: §Pattern 4 rule 2)
             ▼
 ┌───────────────────────────────────────────────────────────┐
 │  PLATFORM SEAM LAYER   com.app.pegsolitaire.platform..     │
 │                                                            │
 │   AdGateway (iface)             GamesService (iface)       │
 │     suspend requestRewarded     suspend signIn             │
 │       (AdReason)                submitScore                │
 │       : RewardOutcome  ◄─NEW    unlockAchievement  ◄─NEW   │
 │     showInterstitial            showLeaderboard    ◄─NEW   │
 │        ▲                        showAchievements   ◄─NEW   │
 │        │ @Binds @Singleton (PlatformModule)                │
 │   NoOpAdGateway                 NoOpGamesService           │
 │   → Granted always (D-02)       → false / no-op            │
 │                                                            │
 │   ── the ONLY tier allowed to reference ──                 │
 │      com.google.android.gms.. / .ump.. / .libraries.ads..  │
 └───────────┬───────────────────────────────────────────────┘
             │ interfaces expose NO Activity, NO SDK type
             ▼
 ┌───────────────────────────────────────────────────────────┐
 │  VIEWMODEL LAYER    ui/game/GameViewModel                  │
 │    onUndo()          ─┐                                    │
 │    onHintRequested() ─┴─► when (requestRewarded(reason)) { │
 │                             Granted, Unavailable → proceed │
 │                             Declined              → abort  │
 │                           }                                │
 └───────────┬───────────────────────────────────────────────┘
             │ StateFlow<GameUiState>  (unchanged this phase)
             ▼
      Compose UI  — zero change, zero Activity references


  ── UNTRUSTED-INPUT BOUNDARIES CLOSED BY DEBT-04 ──

  Room blob ──► SnapshotMapper.toBoardSpecOrNull()
                 └─ toPositionOrNull(): + coordinate range check   [WARN-02]
                    reject → decodeOrNull() returns null → fresh board

  Nav arg  ──► GameViewModel.challengeLevel (public getter)
                 └─ must return the CLAMPED value                  [WARN-03]
                    └─► GameRoute → Results(challengeLevel=…)
                          └─► ResultsViewModel.args (re-clamp, defence in depth)


  ── BUILD-TIME PATHS (no runtime tier) ──

  local.properties ─┐
  ENV / CI secret  ─┴─► resolveAdIds(isReleaseBuild) ──┬─► buildConfigField (code reads)
                          └─ release + missing → FAIL  └─► resValue "string" (manifest reads)

  library manifests ──► manifest merger ──► merged manifest
                          ▲ tools:node="remove" PlayGamesInitProvider
                          └ app manifest adds INTERNET
```

### Recommended Project Structure

```
app/src/main/java/com/app/pegsolitaire/
├── App.kt                                   # MODIFIED: add onCreate; register provider
├── di/
│   └── PlatformModule.kt                    # MODIFIED: + ForegroundActivityProvider binding
│                                            #   (keep SDK-typed @Provides OUT of di/ — §Pattern 4)
├── platform/
│   ├── activity/
│   │   ├── ForegroundActivityProvider.kt     # NEW interface
│   │   └── DefaultForegroundActivityProvider.kt  # NEW impl (@Singleton, lifecycle callbacks)
│   ├── ads/
│   │   ├── AdGateway.kt                     # MODIFIED: RewardOutcome return + corrected KDoc
│   │   ├── AdReason.kt                      # MODIFIED: strike the stale RETRY reservation (D-03)
│   │   ├── RewardOutcome.kt                 # NEW sealed interface
│   │   ├── AdIds.kt                         # NEW: BuildConfig accessors (the ONLY BuildConfig readers)
│   │   └── NoOpAdGateway.kt                 # MODIFIED: returns Granted
│   └── games/
│       ├── GamesService.kt                  # MODIFIED: + achievements + UI intents + session
│       ├── GamesSignInState.kt              # NEW
│       └── NoOpGamesService.kt              # MODIFIED: new methods no-op
├── data/resume/
│   ├── ResumeSnapshotDto.kt                 # MODIFIED: coordinate bound in toPositionOrNull  [WARN-02]
│   └── SnapshotMapper.kt                    # possibly unchanged (bound applied one level down)
└── ui/game/GameViewModel.kt                 # MODIFIED: RewardOutcome when-branches; clamped getter [WARN-03]

app/src/test/java/com/app/pegsolitaire/
├── DomainPurityTest.kt                      # UNCHANGED (do not fold the new rule in — different scope)
├── PlatformBoundaryTest.kt                  # NEW: gms/ump containment + Activity containment
├── PlatformBoundaryGateTest.kt              # NEW: proves the rules FAIL on a violating fixture
└── archfixture/                             # NEW: deliberately-violating classes, test-only

app/
├── proguard-rules.pro                       # NEW: the one required rule + comments
└── build.gradle.kts                         # MODIFIED: buildFeatures, buildTypes, ID resolution
```

---

### Pattern 1: `ForegroundActivityProvider` — the singleton→Activity bridge (PLAT-01)

**What:** A process-lifetime `@Singleton` fed by `Application.registerActivityLifecycleCallbacks`, publishing the currently-resumed `Activity` behind a `WeakReference`.

**Refinement over the ARCHITECTURE.md sketch.** The milestone sketch used `_current.map{}.stateIn(scope, …)`, which requires injecting the `@ApplicationScope CoroutineScope` from `DataModule`. That dependency is avoidable — deriving the awaited value with an operator chain on the `MutableStateFlow` needs no scope at all and no eager sharing:

```kotlin
package com.app.pegsolitaire.platform.activity

import android.app.Activity
import android.app.Application

/**
 * The single source of the foreground [Activity] for platform-layer SDK calls
 * (rewarded-ad show, UMP consent form, Play Games client getters).
 *
 * PLAT-01: this type is reachable ONLY from `com.app.pegsolitaire.platform..`.
 * `PlatformBoundaryTest` fails the build if any ViewModel, UI or `:domain` class
 * depends on it. The `AdGateway` / `GamesService` interfaces deliberately expose
 * no Activity, so no caller outside platform can obtain one transitively.
 */
interface ForegroundActivityProvider {
    /** The resumed Activity, or `null` while the app is backgrounded. */
    val current: Activity?

    /**
     * Suspends until an Activity is resumed. Callers MUST apply their own timeout:
     * a backgrounded app may never resume, and an ad request that waits forever
     * leaves the assist control spinning.
     */
    suspend fun awaitActivity(): Activity

    /** Registers lifecycle callbacks. Called exactly once, from [App.onCreate]. */
    fun attachTo(application: Application)
}
```

```kotlin
@Singleton
class DefaultForegroundActivityProvider
    @Inject
    constructor() : ForegroundActivityProvider, Application.ActivityLifecycleCallbacks {
        private val resumed = MutableStateFlow<WeakReference<Activity>?>(null)

        override val current: Activity? get() = resumed.value?.get()

        override suspend fun awaitActivity(): Activity =
            resumed.mapNotNull { it?.get() }.first()

        override fun attachTo(application: Application) =
            application.registerActivityLifecycleCallbacks(this)

        override fun onActivityResumed(activity: Activity) {
            resumed.value = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            if (current === activity) resumed.value = null
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (current === activity) resumed.value = null
        }

        override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
        override fun onActivityStarted(a: Activity) = Unit
        override fun onActivityStopped(a: Activity) = Unit
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
    }
```

> ⚠️ **Hilt scoping trap — this is the one that bites.** `@Binds @Singleton fun bind(impl: DefaultX): X` scopes the binding *of `X`*, not of `DefaultX`. If `App` injects the concrete `DefaultForegroundActivityProvider` in order to call `registerActivityLifecycleCallbacks`, Hilt creates a **second, unscoped instance** and the one the platform layer sees is never fed any lifecycle events. Two ways out, both fine:
> 1. Annotate the impl class `@Singleton` **and** inject the interface everywhere, using `attachTo(application)` on the interface (shown above). Recommended — `App` never names the concrete type.
> 2. Annotate the impl class `@Singleton` and let `App` inject the concrete type. Works, but couples `App` to the impl.
>
> The existing `NoOpAdGateway` / `NoOpGamesService` do **not** carry `@Singleton` on the class, and that is correct for them because nothing injects the concrete type. Do not pattern-match blindly here.

**`onActivityPaused` nulls the reference — accept it deliberately.** Showing a fullscreen ad pauses the host Activity, so `current` becomes `null` *while an ad is on screen*. That is harmless for the intended usage (capture the Activity **before** calling `show()`), and it is the safer default. Note it in the KDoc so Phase 9 does not "fix" it by holding a strong reference across pause.

**`App.onCreate` becomes:**

```kotlin
@HiltAndroidApp
class App : Application() {
    @Inject lateinit var activityProvider: ForegroundActivityProvider

    override fun onCreate() {
        super.onCreate()
        activityProvider.attachTo(this)
    }
}
```

Hilt field injection into `Application` is available by the time `super.onCreate()` returns — that ordering is not optional.

**Testability.** `DefaultForegroundActivityProvider` is testable JVM-side without Robolectric by calling the callbacks directly with a `mockk<Activity>()`. The three behaviours worth locking: resumed activity is published; pausing the *same* activity clears it; pausing a *different* activity does not clear it.

---

### Pattern 2: `RewardOutcome` and the fail-open call-site mapping (PLAT-02)

```kotlin
package com.app.pegsolitaire.platform.ads

/**
 * The result of a rewarded-grant request (D-01).
 *
 * Sealed rather than boolean so the compiler forces every call site to decide what
 * happens when no ad could be served — ADS-03 requires the assist be granted anyway
 * in that case, and a `Boolean` cannot express the difference between
 * "the player said no" and "we had nothing to show them".
 */
sealed interface RewardOutcome {
    /** The reward is granted: an ad reported the reward earned, or none was required. */
    data object Granted : RewardOutcome

    /** The player was offered an ad and explicitly declined, or dismissed it unearned. */
    data object Declined : RewardOutcome

    /**
     * No ad could be served (no consent yet, no fill, load error, no foreground
     * Activity). Callers MUST proceed with the assist anyway (ADS-03) — the player
     * is never dead-ended by an ad that failed to arrive.
     */
    data object Unavailable : RewardOutcome
}
```

**The call-site mapping is the decision that matters**, and it is the one thing in this pattern that is *not* mechanical:

| Outcome | `GameViewModel` behaviour | Why |
|---------|---------------------------|-----|
| `Granted` | Proceed with the assist | — |
| `Unavailable` | **Proceed with the assist** | ADS-03. Mapping this to "skip" would silently break hint/undo offline the moment Phase 9 lands a real gateway — a v1.0 behaviour regression that would not surface in Phase 6 because the stub never returns it. |
| `Declined` | Abort (current `return@launch` behaviour) | The only honest decline. |

```kotlin
// GameViewModel.onUndo(), replacing `if (!adGateway.requestRewarded(AdReason.UNDO)) return@launch`
when (adGateway.requestRewarded(AdReason.UNDO)) {
    RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit   // ADS-03: assist proceeds
    RewardOutcome.Declined -> return@launch
}
```

```kotlin
// GameViewModel.onHintRequested() — note it must ALSO clear solverThinking on the abort path
when (adGateway.requestRewarded(AdReason.HINT)) {
    RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit
    RewardOutcome.Declined -> {
        _uiState.update { it.copy(solverThinking = false) }
        return@launch
    }
}
```

Prefer an explicit `when` over `if (outcome == Declined)` — the exhaustive form is the entire reason D-01 chose a sealed type, and it is what will force Phase 9/10 authors to think about new members.

---

### Pattern 3: `GamesService` extension (PLAT-03)

Requirements constraint: achievements + the stock Play Games UI intents; No-Op degrades silently; **Saved Games stays off this interface** (Phase 13, behind `ProgressRepository`).

```kotlin
interface GamesService {
    /** Observable sign-in state; `Unknown` until the first check completes. */
    val session: StateFlow<GamesSignInState>

    suspend fun signIn(): Boolean

    fun submitScore(leaderboardId: String, score: Long)

    /** Unlocks a one-shot achievement. Fire-and-forget: never blocks or fails a solve. */
    fun unlockAchievement(achievementId: String)

    /** Advances an incremental achievement by [steps]. Fire-and-forget. */
    fun incrementAchievement(achievementId: String, steps: Int)

    /** Opens the stock Play Games leaderboard UI. No-op when unavailable. */
    suspend fun showLeaderboard(leaderboardId: String)

    /** Opens the stock Play Games achievements UI. No-op when unavailable. */
    suspend fun showAchievements()
}
```

```kotlin
sealed interface GamesSignInState {
    data object Unknown : GamesSignInState
    data object SignedOut : GamesSignInState
    data class SignedIn(val playerId: String) : GamesSignInState
}
```

**Why `showLeaderboard`/`showAchievements` take no Activity and return no result.** PGS v2's `getLeaderboardIntent()` / `getAchievementsIntent()` return `Task<Intent>`; the real Phase-11 implementation resolves the Activity from `ForegroundActivityProvider` and calls `activity.startActivity(intent)`. ARCHITECTURE.md suggested threading an `ActivityResultLauncher` down from the Route; the Activity bridge makes that unnecessary, and PGS v2 returns no result the app must act on (the v1 `RESULT_RECONNECT_REQUIRED` path no longer exists). `[MEDIUM confidence — the v2 intent contract is documented, but this project has not yet exercised it.]`

**No-Op behaviour (unchanged philosophy):** `session` = `MutableStateFlow(GamesSignInState.SignedOut).asStateFlow()`, `signIn()` = `false`, everything else `Unit`. `GamesService` has **zero production call sites today**, so this extension has no blast radius beyond `NoOpGamesService` and `NoOpGamesServiceTest`.

`suspend` on the two `show*` methods costs nothing today and is what lets Phase 11 `awaitActivity()` with a timeout. `unlock`/`increment` stay non-suspend to match the fire-and-forget `submitScore` precedent already in the interface.

---

### Pattern 4: The extended boundary gates (PLAT-04, PLAT-01)

**Do not fold the new rules into `DomainPurityTest`.** ArchUnit's import scope is per-class (`@AnalyzeClasses`), and `DomainPurityTest` is scoped to `com.app.pegsolitaire.domain`. The containment rules need the whole app. Add a second class.

```kotlin
package com.app.pegsolitaire

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.runner.RunWith

/**
 * Platform-boundary gate (PLAT-04, PLAT-01) — the seam counterpart to
 * [DomainPurityTest]. `DomainPurityTest` proves `:domain` is Android-free; this
 * proves the Play/AdMob SDKs and the foreground Activity are confined to
 * `com.app.pegsolitaire.platform..`.
 *
 * Bytecode-based, so it also covers Hilt/KSP-generated classes.
 */
@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(
    packages = ["com.app.pegsolitaire"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class PlatformBoundaryTest {
    @ArchTest
    val onlyPlatformDependsOnPlaySdks: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("..platform..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.google.android.gms..",
                "com.google.android.ump..",
                "com.google.android.libraries.ads..",
            )

    @ArchTest
    val onlyPlatformReachesTheForegroundActivity: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("..platform..")
            .and()
            .haveNameNotMatching(".*\\.App")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.app.pegsolitaire.platform.activity.ForegroundActivityProvider",
            )

    @ArchTest
    val uiAndDomainNeverTouchAnActivity: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage("..ui..", "..domain..", "..data..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.app.Activity")
}
```

Traps the planner must budget for, each of which will otherwise surface as a confusing red build:

| Trap | Symptom | Handling |
|------|---------|----------|
| **`App` must be exempt from rule 2** | `App` injects `ForegroundActivityProvider` to call `attachTo` and lives in the root package, outside `..platform..` | The `haveNameNotMatching(".*\\.App")` clause above. Alternative: move the registration into an `@EntryPoint` read by a platform-side initializer — more machinery for no benefit. |
| **Hilt-generated code lives in app packages** | `DaggerApp_HiltComponents_SingletonC` and `*_Factory` classes sit in `com.app.pegsolitaire.*` and reference every binding's implementation type | Harmless *provided* SDK-typed `@Provides` methods live inside `..platform..`. **Therefore: do not put an `AdsModule` that provides gms/UMP types under `di/`** — ARCHITECTURE.md's proposed `di/AdsModule.kt` would violate rule 1 the moment it returns an SDK type. Put SDK-typed modules at `platform/ads/di/AdsModule.kt`. |
| **`failOnEmptyShould`** | ArchUnit 1.3.0 fails a rule whose `that()` clause matches zero classes | Not a risk for these three (`resideOutsideOfPackage("..platform..")` matches most of the app). If a narrower rule is added later, use `.allowEmptyShould(true)`. |
| **`areAssignableTo(Activity::class.java)`** | Unreliable — `android.app.Activity` is not inside the `@AnalyzeClasses` scope, so ArchUnit sees a stub and cannot resolve the hierarchy | Use `haveFullyQualifiedName(...)` / `resideInAnyPackage(...)` as above. Name-based matching is resolution-independent. |
| **Rule 3 and `androidx.activity.ComponentActivity`** | `MainActivity` extends it but lives in the root package, outside `..ui..` — verified: `ui/` has **zero** Activity references today | Rule 3 passes on day 1. If Phase 9 wants `LocalActivity` in a composable, this rule is exactly what should stop it. |

---

### Pattern 5: Build types with per-type IDs (PLAT-06, D-06)

Two constraints interact: the project has **no** `buildTypes` block *and* a debug-only asset source-set tweak whose ordering is already fragile.

**The existing Room-schema wiring survives untouched.** `sourceSets.getByName("debug").assets.srcDir(roomSchemaDir)` and `tasks.matching { it.name == "mergeDebugAssets" }.configureEach { dependsOn("copyRoomSchemas") }` are both keyed by the *existing* `debug` name. Declaring `buildTypes { debug { … } }` **configures** the always-present `debug` type rather than creating a new one, so neither line changes meaning. Verified indirectly this session: the R8 probe declared `release { … }` via init script and `:app:copyRoomSchemas` reported `NO-SOURCE` on release (correct) while the debug asset wiring stayed intact. Keep the two blocks in their current order in the file, with `buildTypes` inside the existing `android { }` block above the `sourceSets` line.

```kotlin
android {
    namespace = "com.app.pegsolitaire"

    defaultConfig { /* unchanged */ }

    // BuildConfig generation is OFF by default in AGP 8+/9 (BuildFeatures.buildConfig
    // KDoc: "Default value is false"). Without this, every buildConfigField below
    // silently generates nothing.
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // D-05: Google's test ad units. NEVER a real unit in a build a developer
            // or CI can run — invalid-traffic suspensions are non-appealable.
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"$TEST_REWARDED_AD_UNIT\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"$TEST_INTERSTITIAL_AD_UNIT\"")
            buildConfigField("String", "ADMOB_APP_ID", "\"$TEST_ADMOB_APP_ID\"")
            resValue("string", "admob_app_id", TEST_ADMOB_APP_ID)
            resValue("string", "game_services_project_id", TEST_GAMES_PROJECT_ID)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro"),
            )
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${ids.rewarded}\"")
            // …etc
        }
    }

    sourceSets.getByName("debug").assets.srcDir(roomSchemaDir)   // UNCHANGED
}
```

**`buildConfigField` value escaping** is the classic error: the third argument is a **Java/Kotlin source literal**, so a `String` field needs embedded quotes (`"\"$value\""`). Omitting them produces an unresolved-identifier compile error in generated code.

**Why `resValue` as well as `buildConfigField`.** Manifest `meta-data` cannot read `BuildConfig`. The AdMob app ID must appear in the manifest for UMP even though Next-Gen takes it programmatically via `InitializationConfig.Builder(appId)` `[CITED: developers.google.com/admob/android/next-gen/quick-start]`, and the PGS project ID **must** be a `@string` resource because a bare numeric `android:value` is parsed as an integer `[CITED: developers.google.com/games/services/android/troubleshooting]`. `resValue` per build type satisfies both with one mechanism. Resources generated by `resValue` and referenced from the manifest are kept by the resource shrinker.

**Google test IDs** `[VERIFIED: developers.google.com/admob/android/next-gen/test-ads]`:

| Format | Test unit ID |
|--------|--------------|
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Sample app ID | `ca-app-pub-3940256099942544~3347511713` |

Android emulators are test devices by default; physical devices need `RequestConfiguration.Builder().setTestDeviceIds(...)` (Phase 9 concern).

---

### Pattern 6: Reading untracked IDs, and failing the release build (D-08, D-09)

```kotlin
// app/build.gradle.kts — or, better, a helper in build-logic so it is unit-testable
//                        (see §Validation Architecture).

/**
 * Resolves a build-config secret from, in order: an environment variable (CI),
 * then `local.properties` (developer machine). Returns `null` when absent or when
 * the value is still the committed placeholder.
 *
 * D-08: real AdMob / Play Games IDs are never committed. A fresh clone resolves
 * nothing here, falls back to Google's test IDs, and therefore cannot serve real ads.
 */
fun Project.resolveId(propertyKey: String, envKey: String): String? {
    val fromEnv = providers.environmentVariable(envKey).orNull
    val fromLocal = providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull
        ?.let { text -> java.util.Properties().apply { load(text.reader()) } }
        ?.getProperty(propertyKey)
    return (fromEnv ?: fromLocal)?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("REPLACE_ME") }
}
```

`providers.environmentVariable` / `providers.fileContents` are the configuration-cache-safe accessors. This project has **CC disabled** (`org.gradle.configuration-cache=false`), so a plain `FileInputStream` would also work — but using the provider API costs nothing and removes a future migration blocker. Note that `local.properties` is *not* automatically exposed as Gradle properties; AGP reads only `sdk.dir` from it.

**The hard part of D-09 is "release only".** AGP configures *every* variant on *every* invocation, so a naive `require(...)` inside `buildTypes.release { }` fires during `assembleDebug` and during IDE sync — which breaks D-08's "a fresh clone builds successfully". Options, in descending recommendation order:

| Approach | Fires only for release? | Configuration-time? | Notes |
|----------|------------------------|---------------------|-------|
| **Gate on requested tasks** — `gradle.startParameter.taskNames.any { it.contains("elease") \|\| it.startsWith("bundle") }` before throwing `GradleException` | Yes | Yes | Honours D-09 literally. `startParameter.taskNames` is part of the CC key, so it stays CC-compatible. Empty during IDE sync → sync never fails, which is what you want. Recommended. |
| Register a verification task and wire `tasks.named("assembleRelease") { dependsOn(it) }` (plus `bundleRelease`) | Yes | No — execution-time | The most robust mechanically, but D-09 says configuration time. Good as a **second** belt. |
| `androidComponents.onVariants(selector().withBuildType("release")) { }` | No | Yes | Runs whenever the release variant is configured, i.e. on `assembleDebug` too. Does **not** solve the problem. |

Recommendation: implement the first, and add the second as an execution-time backstop. The error message must name the exact missing property, e.g.:

```
Release build requires real ad/Play Games IDs.
Missing: ADMOB_REWARDED_AD_UNIT_ID
Set it in local.properties (untracked) or as the ADMOB_REWARDED_AD_UNIT_ID environment variable.
Debug builds use Google test IDs and need no configuration.
```

Ship a committed `local.properties.template` (or a README block) listing the four keys with `REPLACE_ME_*` placeholders, so "a fresh clone" has a discoverable path to a release build.

---

### Anti-Patterns to Avoid

- **A `@Singleton` or `lateinit var` holding an `Activity` strongly.** Guaranteed leak across configuration change; LeakCanary-visible. Use the `WeakReference` provider.
- **Injecting the concrete `DefaultForegroundActivityProvider` into `App` while `@Singleton` is only on the `@Binds`.** Creates a second, never-fed instance. See the boxed warning in Pattern 1.
- **An SDK-typed `@Provides` under `di/`.** `di/` is outside `..platform..`; Hilt's generated component will reference the SDK type and trip the PLAT-04 rule. Put SDK-typed modules under `platform/*/di/`.
- **Ad unit IDs in a scoped constants object.** The project's no-magic-strings rule pushes toward this and it is exactly wrong here (Pitfall 3). Only `BuildConfig` / `resValue`.
- **Mapping `RewardOutcome.Unavailable` to "skip the assist".** Passes every Phase-6 test (the stub never returns it) and silently breaks offline hint/undo in Phase 9. Violates ADS-03.
- **Folding the gms rule into `DomainPurityTest`.** Wrong `@AnalyzeClasses` scope; would silently only check `:domain`.
- **Adding a detekt baseline or a `@Suppress` to get the phase green.** Pitfall 14 — a suppressed gate is permanent debt.
- **Deleting `NoOpAdGateway` / `NoOpGamesService`.** They stay forever as the test/preview bindings.

---

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---------|-------------|-------------|-----|
| Suppressing R8's missing-class errors | A guessed `-keep` rule | The generated `app/build/outputs/mapping/release/missing_rules.txt` | AGP writes the exact minimal rules. Guessing produces over-broad keeps that defeat the point of minifying. |
| Baseline R8 configuration | A hand-written rule set | `getDefaultProguardFile("proguard-android-optimize.txt")` | AGP's maintained baseline. `optimize` (not the plain variant) is the current default recommendation. |
| Keep rules for Room / Hilt / kotlinx-serialization / Navigation | Anything | Their consumer rules — **verified present**, see the table below | Library-shipped rules are R8-full-mode aware and updated with the library. |
| Removing SDK auto-init providers | A `ProviderInfo` disable at runtime, or `<provider android:enabled="false">` | `tools:node="remove"` in the app manifest | The documented merge mechanism; a no-op when the element is absent. |
| Reading `local.properties` | `File("local.properties").readLines()` | `providers.fileContents(...)` + `java.util.Properties` | Provider API is configuration-cache-safe and path-correct in composite builds. |
| Bridging GMS `Task<T>` to coroutines (Phase 11) | `suspendCancellableCoroutine` per call | `kotlinx-coroutines-play-services` `.await()`, version-ref'd to the existing `coroutines` pin | Official adapter; hand-rolled wrappers re-implement cancellation and exception mapping per call site. |
| Bounding untrusted resume coordinates | A new bespoke bound | `ResumeSnapshotFormat.MAX_BOARD_POSITIONS` (= `Long.SIZE_BITS` = 64) | See DEBT-04: 64 is the **tightest sound** bound and it already exists as a named constant. |

**Key insight:** the value-add in this phase is almost entirely in *deleting* the need for custom code — the correct outcome for PLAT-05 is one keep rule and a comment explaining it, not a `proguard-rules.pro` full of defensive keeps.

### Consumer keep rules — verified by AAR/JAR inspection this session

| Artifact | Ships consumer rules? | Content |
|----------|----------------------|---------|
| `androidx.room:room-runtime-android:2.8.4` | Yes | `-keep class * extends androidx.room.RoomDatabase { void <init>(); }` + two `-dontwarn`. Note it already spells out `<init>()` — strict-full-mode-safe by construction. |
| `androidx.room:room-ktx:2.8.4` | Yes | `proguard.txt` present |
| `com.google.dagger:hilt-android:2.60` | Yes | Four `-keep,allowobfuscation,allowshrinking` rules on `@EntryPoint`-family annotations |
| `com.google.dagger:dagger:2.60` | Yes | `META-INF/com.android.tools/r8/r8.pro` — `-identifiernamestring` |
| `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0` | Yes | Full R8-full-mode-aware set covering `Companion`, `INSTANCE`, `serializer(...)`, `$$serializer.descriptor`, `@NamedCompanion`, `-keepattributes RuntimeVisibleAnnotations` |
| `androidx.navigation:navigation-common-android:2.9.8` | Yes | `NavArgs.fromBundle` + `-keep,allowobfuscation,allowshrinking class * extends androidx.navigation.Navigator` |
| `androidx.navigation:navigation-compose-android:2.9.8` | Yes (empty) | *"Intentionally empty proguard rules to indicate this library is safe to shrink"* |
| `androidx.navigation:navigation-runtime-android:2.9.8` | Yes (empty) | same |
| `com.google.android.gms:play-services-basement:18.9.0` | Yes | SafeParcelable `CREATOR`, `ReflectedParcelable`, `@Keep`, `@KeepName`, `@DynamiteApi` |
| `com.google.android.gms:play-services-base:18.9.0` | Yes | small |
| `com.google.android.ump:user-messaging-platform:4.0.0` | Yes | one proto-field keep |
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0` | Yes | 5.6 KB: Gson type-adapters, `ads_mobile_sdk.**` name preservation, mediation adapter interfaces, protobuf fields |
| **`com.google.android.gms:play-services-games-v2:22.0.0`** | **NO** | **No `proguard.txt` in the AAR.** Covered transitively by `play-services-basement`'s SafeParcelable/`@Keep` rules. STACK.md's "both SDKs ship consumer ProGuard rules" is wrong about this one; the conclusion (no hand-written rules needed) survives, but by inheritance rather than by direct publication. Re-verify at Phase 11 by building a minified release **that actually calls PGS**. |

---

## R8: What This Repo Actually Needs (PLAT-05, D-11, D-12)

**Everything in this section was measured by running the real build.** Method: an `--init-script` overlay enabling `buildConfig`, `isMinifyEnabled`, `isShrinkResources` and `proguard-android-optimize.txt` on `:app`'s release type, then `./gradlew :app:assembleRelease`. No repository file was modified; the build tree was restored to its unminified state afterwards.

### Measurements

| Configuration | Release APK | Δ |
|---------------|-------------|---|
| Current (`main`/`develop` as committed — no `buildTypes`, no R8) | **9,890,332 B** (9.43 MiB) | baseline |
| `isMinifyEnabled` + `isShrinkResources` + optimize baseline + one keep rule | **1,518,323 B** (1.45 MiB) | **−8,372,009 B (−84.6 %)** |

Use **9,890,332 B** as the D-12 pre-SDK APK baseline. Also record the post-R8 pre-SDK number (1,518,323 B) — it is the more useful comparand for Phase 14, since Phase 14's build will also be minified.

### Finding 1 — R8 fails today; exactly one rule fixes it

First minified build:

```
ERROR: Missing classes detected while running R8. Please add the missing classes or apply
additional keep rules that are generated in .../mapping/release/missing_rules.txt.
ERROR: R8: Missing class com.google.errorprone.annotations.CanIgnoreReturnValue
  (referenced from: dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder
   dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder
     .savedStateHandleHolder(dagger.hilt.android.internal.managers.SavedStateHandleHolder))
```

`missing_rules.txt` contained exactly:

```proguard
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
```

With that single line in `proguard-rules.pro`, the build succeeded. This is the *entire* hand-written keep-rule surface for the current dependency set. It must carry a comment per D-11, e.g.:

```proguard
# Hilt 2.60's generated ActivityRetainedComponentBuilder is annotated with
# @CanIgnoreReturnValue, but hilt-android does not ship error_prone_annotations as a
# runtime dependency (it is compileOnly). The annotation is not needed at runtime.
# Verified 2026-08-27: this is the only rule R8 requires for this dependency set —
# Room, Hilt/Dagger, kotlinx-serialization and Navigation all publish consumer rules.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
```

> Do **not** pre-emptively add keep rules "just in case". Add rules only when `missing_rules.txt` or a runtime failure demands them, and comment each.

### Finding 2 — minification introduces a NEW network dependency

Enabling `isMinifyEnabled` activates AGP 9.1.0's `:app:produceReleaseComposeMapping` and `:app:mergeReleaseComposeMapping` tasks, which resolve a `composeMappingProducerClasspath` configuration:

```
> Could not resolve org.jetbrains.kotlin:compose-group-mapping:2.3.20.
  > No cached version of org.jetbrains.kotlin:compose-group-mapping:2.3.20 available for offline mode.
```

This artifact was **not** in the local Gradle cache and had to be fetched. Consequences the planner must handle:

- The **first** minified release build on any machine or CI runner requires network, even with an otherwise warm cache.
- The project's "`check` must stay green **and offline**" invariant (a stated Phase-6 success criterion) can be broken by this in a way that looks nothing like the change that caused it. `check` itself does not build release, so `check` is safe — but any `assembleRelease`/`bundleRelease` in CI is not.
- It is an implicit, unpinned artifact not present in `libs.versions.toml`, which contradicts the catalog's "single source of truth for every pinned version" rule. Consider adding it to the catalog for visibility even though AGP resolves it itself, or explicitly documenting the exception.
- Its version tracks the Kotlin pin (2.3.20), so it moves with `kotlin` in the catalog.

Mitigation: warm the cache once (`./gradlew :app:assembleRelease`) before asserting anything about offline behaviour, and state explicitly in `06-VERIFICATION.md` which commands are expected to work offline.

### Finding 3 — R8 **strict** full mode is on by default in AGP 9.1.0

Read directly from `com/android/build/gradle/options/BooleanOption.kt` in `gradle-9.1.0-sources.jar`:

```kotlin
FULL_R8("android.enableR8.fullMode", defaultValue = true, FeatureStage.SoftlyEnforced(VERSION_10_0))
R8_STRICT_FULL_MODE_FOR_KEEP_RULES("android.r8.strictFullModeForKeepRules", defaultValue = true, ...)
FAIL_ON_MISSING_PROGUARD_FILES("android.proguard.failOnMissingFiles", true, ...)
```

- **`android.enableR8.fullMode` is already `true`.** D-11's "R8 full mode" needs no `gradle.properties` entry. Adding it explicitly is harmless documentation; adding `=false` would be a regression.
- **`android.r8.strictFullModeForKeepRules` is `true`** — and this changes rule semantics. In strict mode, `-keep class A` **no longer implicitly keeps the default constructor**. Any hand-written rule intended to preserve a reflectively-instantiated class must spell it out: `-keep class A { <init>(); }`. This is why Room's consumer rule reads `-keep class * extends androidx.room.RoomDatabase { void <init>(); }`. It is the single most likely source of a *runtime* `NoSuchMethodException` in a build that compiled and shrank cleanly.
- **`android.proguard.failOnMissingFiles` is `true`** — a `proguardFiles(file("proguard-rules.pro"))` reference to a non-existent file fails the build. Create the file in the same commit as the reference.

### Finding 4 — `shrinkResources` is safe here, including the downloadable fonts

The one real resource-shrinking risk in this app is `app/src/main/res/values/font_certs.xml`, whose `com_google_android_gms_fonts_certs` array is an array **of array references** (`@array/..._dev`, `@array/..._prod`) reached from code as `R.array.com_google_android_gms_fonts_certs` in `ui/theme/Type.kt`. If the shrinker dropped the nested arrays, downloadable Fredoka/Nunito would silently fall back to the default font — a *visible* change in a phase whose success criterion is invisibility.

Verified against the shrunk APK with `aapt2 dump resources`:

```
resource 0x7f030000 array/com_google_android_gms_fonts_certs
    [@array/com_google_android_gms_fonts_certs_dev, @array/com_google_android_gms_fonts_certs_prod]
resource 0x7f030001 array/com_google_android_gms_fonts_certs_dev
resource 0x7f030002 array/com_google_android_gms_fonts_certs_prod
```

All three survive. The shrinker follows resource→resource references correctly. 236 resource entries remain in the shrunk APK.

`lintVitalRelease` also passed under minification — enabling R8 does not surface new lint-vital errors on this codebase.

### Finding 5 — no signing config exists

`assembleRelease` produces `app-release-unsigned.apk`. That is fine for a size baseline and for proving R8 works, but **"a release build still launches and plays" (Phase 6 success criterion 4) cannot be verified without signing it.** Options: add a debug-signed release variant temporarily (`release { signingConfig = signingConfigs.getByName("debug") }` — do **not** commit this), or `apksigner sign` with the debug keystore, or defer the launch check to a manual verification step. Flag as a manual-only verification, not an automated one.

---

## Manifest Merge and Auto-Init Providers (PLAT-07, D-07)

### Finding 1 — Robolectric 4.16 does NOT run manifest-declared ContentProviders at startup

PITFALLS flagged this as MEDIUM/unverified and recommended an empirical spike. **The spike is unnecessary; it is settled.** Method: unpacked `robolectric-4.16.jar` and `shadows-framework-4.16.jar` from the local Gradle cache and read the bytecode.

- `org.robolectric.android.internal.AndroidTestEnvironment` contains **no** loop over manifest providers. The only provider it installs is Robolectric's own `FakeMediaProvider`, and only when the system property `robolectric.installFakeMediaProvider` is set, via `Robolectric.setupContentProvider(...)`.
- No class in any Robolectric 4.16 jar references `installContentProviders` or `ContentProviderData`.
- `ShadowContentResolver.getProvider(String authority, Context)` shows the actual behaviour: on a cache miss it calls `PackageManager.resolveContentProvider(authority, 0)` and, only if that resolves, `createAndInitialize(providerInfo)`. **Instantiation is lazy and demand-driven** — it happens when a `ContentResolver` operation names the authority, never at application bring-up.

**Consequence for PLAT-07:** neither `PlayGamesInitProvider` nor any other library provider will execute in the ~10 Robolectric-backed Compose tests merely because it is in the merged manifest. The 299-test suite is not at risk from provider auto-init. **Do not budget a spike task for this.**

**D-07 still stands, for a different reason.** Provider removal remains required for production: `PlayGamesInitProvider` runs before `App.onCreate` on a real device, firing automatic sign-in on the cold-start path (Pitfall 15) at a moment the app did not choose. That is a cold-start and CONS-02 concern, not a test concern. The planner should re-word D-07's justification accordingly rather than claiming a test-suite benefit that does not exist.

### Finding 2 — there is no `MobileAdsInitProvider` in the Next-Gen SDK

The `ads-mobile-sdk:1.4.0` AAR manifest (downloaded and read in full, quoted below in abridged form) declares:

```xml
<manifest package="com.google.android.libraries.ads.mobile.sdk">
    <uses-sdk android:minSdkVersion="24" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_BASIC_PHONE_STATE" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" tools:node="remove" />
    <queries> … browser / CustomTabs / MRAID intents … </queries>
    <application>
        <activity android:name="…sdk.common.AdActivity" android:exported="false" … />
    </application>
</manifest>
```

**No `<provider>` element at all.** Next-Gen requires explicit `MobileAds.initialize(context, InitializationConfig.Builder(appId).build())` on a **background thread**, and throws `UninitializedPropertyAccessException` if you call other APIs first `[CITED: developers.google.com/admob/android/next-gen/quick-start]`. In other words the ads SDK is *already* explicit-init-only, which satisfies CONS-02's "no ad request before consent" by construction.

Also note: the ads SDK **removes `RECEIVE_BOOT_COMPLETED` itself** with `tools:node="remove"`. PITFALLS' concern about that permission appearing in the store listing is stale for Next-Gen.

**Correction to D-07:** `MobileAdsInitProvider` does not exist on the chosen SDK. Writing a `tools:node="remove"` for it would be a harmless no-op that documents a false belief. Recommended: remove only `PlayGamesInitProvider`, and add a comment in the manifest recording *why* there is no ads counterpart.

### Finding 3 — exact removal syntax

`xmlns:tools` must be declared on the `<manifest>` root — the current manifest does **not** declare it, so this is a required edit:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- D-10: the app was fully offline through v1.0 and declared no permissions.
         Inert until an SDK uses it; declared explicitly so the store permission
         diff is a deliberate commit rather than an implicit manifest-merge result. -->
    <uses-permission android:name="android.permission.INTERNET" />

    <application …>
        <!-- D-07: play-services-games-v2 declares PlayGamesInitProvider
             (authority ${applicationId}.playgamesinitprovider, initOrder 99), which
             fires automatic Play Games sign-in before App.onCreate completes.
             Removed so initialisation happens at a moment this app chooses
             (PlayGamesSdk.initialize in App.onCreate, Phase 11).
             There is deliberately NO MobileAdsInitProvider counterpart: the GMA
             Next-Gen SDK declares no ContentProvider and requires explicit
             MobileAds.initialize() — verified against ads-mobile-sdk 1.4.0's AAR
             manifest, 2026-08-27. -->
        <provider
            android:name="com.google.android.gms.games.provider.PlayGamesInitProvider"
            android:authorities="${applicationId}.playgamesinitprovider"
            tools:node="remove" />
    </application>
</manifest>
```

`tools:node="remove"` is a **no-op when the target element is absent from every lower-priority manifest** — not an error `[CITED: developer.android.com/build/manage-manifests]`. So this is safe whether or not the PGS dependency is on the graph yet. Marker attributes are stripped from the merged output for app modules.

`android:authorities` should be included so the merger matches by name; the merger keys `<provider>` on `android:name`, so name alone is sufficient, but including the authority documents what is being removed.

### Finding 4 — how to *prove* the removal worked

Two evidence artifacts, both cheap:

1. **Merged manifest diff** — `app/build/intermediates/merged_manifest/{debug,release}/AndroidManifest.xml`, plus the merger report at `app/build/outputs/logs/manifest-merger-{variant}-report.txt`. Commit the relevant excerpt into the phase artifacts (Phase 6 success criterion 5 wants "evidence committed to the repo").
2. **An automated Robolectric assertion** (better, because it is a permanent gate):

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ManifestSurfaceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun playGamesInitProviderIsNotInTheMergedManifest() {
        val pm = context.packageManager
        assertThat(pm.resolveContentProvider("${context.packageName}.playgamesinitprovider", 0))
            .isNull()
    }

    @Test
    fun theAppDeclaresInternetAndNothingElseUnexpected() {
        val declared = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        assertThat(declared).contains(Manifest.permission.INTERNET)
    }
}
```

The first test is meaningful only once `play-services-games-v2` is on the graph — before that it passes vacuously. Note that in a real assertion, and consider gating it behind a "dependency present" check or adding it in the same commit as the dependency.

---

## Seam Migration Blast Radius (PLAT-02, PLAT-03)

Complete inventory, from `grep -rn "requestRewarded|AdGateway|GamesService|AdReason" app/src --include="*.kt"`.

### `AdGateway.requestRewarded` — `Boolean` → `RewardOutcome`

**Production (2 call sites, both in `GameViewModel`):**

| Location | Current code | Change |
|----------|--------------|--------|
| `ui/game/GameViewModel.kt:675` (`onUndo`) | `if (!adGateway.requestRewarded(AdReason.UNDO)) return@launch` | exhaustive `when`; `Unavailable` must proceed |
| `ui/game/GameViewModel.kt:739` (`onHintRequested`) | `if (!adGateway.requestRewarded(AdReason.HINT)) { … solverThinking = false; return@launch }` | exhaustive `when`; abort branch must still clear `solverThinking` |

**Interface / impl / docs (4 files):**

| File | Change |
|------|--------|
| `platform/ads/AdGateway.kt` | Return type; KDoc rewrite. **The KDoc's "Slice 2 swaps only the Hilt binding … no gameplay or UI change" claim is false** and CONTEXT names correcting it as in-scope: every real SDK call needs an Activity, and Phase 9 adds an opt-in dialog. |
| `platform/ads/RewardOutcome.kt` | New file |
| `platform/ads/NoOpAdGateway.kt` | `= RewardOutcome.Granted` (D-02); KDoc |
| `platform/ads/AdReason.kt` | Strike the stale `RETRY` reservation (D-03) — retry is permanently free |
| `di/PlatformModule.kt` | KDoc only: it currently says *"the ad stub grants (`true`)"*, and the "only Slice-2 swap points" claim is now wrong (the Activity provider is a third binding) |

**Tests (6 files):**

| File | What breaks |
|------|-------------|
| `test/…/platform/ads/NoOpAdGatewayTest.kt` | 2 assertions `assertThat(...).isTrue()` → `isEqualTo(RewardOutcome.Granted)` |
| `test/…/ui/game/GameViewModelTest.kt` | `FakeAdGateway(grant: Boolean)` (~:1292), `GatedAdGateway` (~:1313), and four tests at ~:384, :405, :645, :660, :700 |
| `test/…/ui/game/GameViewModelChallengeAdvanceTest.kt` | `private object GrantingAdGateway` (:292) |
| `test/…/ui/game/GameViewModelResumeTest.kt` | `private object GrantingAds` (:643) |
| `test/…/ui/game/GameFlowTest.kt` | `adGateway = NoOpAdGateway()` (:241) — compiles unchanged |
| *(new)* | Tests forcing `Declined` **and** `Unavailable` at both call sites |

> **These are untested code paths, not proven ones.** `NoOpAdGateway` has always returned `true`, so the `false`-grant branches in `GameViewModel` have literally never executed in production. `GameViewModelTest` does exercise `grant = false` via `FakeAdGateway` — so the *decline* path has test coverage — but there has never been a third state. Every `Unavailable` branch added in this phase is brand-new behaviour with zero prior art. Treat the `Unavailable` tests as the highest-value new tests in the phase.

Recommendation: change `FakeAdGateway(grant: Boolean = true)` to `FakeAdGateway(outcome: RewardOutcome = RewardOutcome.Granted)` so future phases can drive all three states from one fake.

### `GamesService` — additive only

**Zero production call sites.** Only `NoOpGamesService.kt` and `NoOpGamesServiceTest.kt` change. This is the lowest-risk requirement in the phase.

---

## Runtime State Inventory

> Phase 6 is not a rename/refactor/migration phase in the string-replacement sense, but the `Boolean` → `RewardOutcome` seam change and the build-config introduction do have out-of-repo surface. Assessed explicitly.

| Category | Items found | Action required |
|----------|-------------|------------------|
| Stored data | **None.** `RewardOutcome` is transient — no ad state is persisted. The resume snapshot schema (`ResumeSnapshotDto`) is untouched by PLAT-02/03; DEBT-04's WARN-02 fix **tightens validation of existing stored blobs but does not change the format**, so no Room migration and no schema version bump. Verified: `app/schemas/` holds only v1; `SnapshotMapper` decode is already fail-safe (returns `null` → fresh board). | None — code edit only. But **add a regression test proving a pre-existing valid blob still decodes** after the bound is added (see DEBT-04). |
| Live service config | **None yet.** AdMob and Play Console PGS projects do not exist. Phase 6's build config only needs *placeholder* keys; the real IDs are the External Prerequisites the operator starts on day 1. | None in-phase. Operator action tracked in ROADMAP §External Prerequisites. |
| OS-registered state | **None.** No Task Scheduler / launchd / systemd / pm2 usage — this is an Android app repo. The one OS-registered surface that *appears* in this phase is the manifest's `PlayGamesAppShortcutsActivity` (`android:exported="true"`) which the PGS AAR merges in; it registers an app-shortcut surface with the launcher. | Note in the manifest audit; no action. |
| Secrets / env vars | **New keys introduced by D-08.** Four `local.properties` keys plus the matching CI environment variables (naming is Claude's discretion — suggest `ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID`, `PGS_PROJECT_ID`). `local.properties` today contains only `sdk.dir` and is gitignored. No existing secret is renamed. | Add a committed `local.properties.template` / README block; document the CI secret names. |
| Build artifacts | **Yes — `app/build/` will hold stale unminified release outputs.** This session's probe left and then restored `app/build/outputs/apk/release/`. More importantly, the first minified build fetches `org.jetbrains.kotlin:compose-group-mapping:2.3.20` into `~/.gradle/caches`, which will not be present on a fresh CI runner. `app/schemas/` (committed Room schema) is unaffected — `copyRoomSchemas` reports `NO-SOURCE` on release. | Run `./gradlew clean` before taking the official D-12 baseline measurement. Warm the cache before asserting offline behaviour. |

---

## DEBT-04: WARN-02 and WARN-03 (D-13)

Original advisory text located at `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-SECURITY.md:87-102`.

### WARN-02 — snapshot footprint coordinates are unbounded

**Advisory (verbatim, abridged):** *"`SnapshotMapper.kt:146-161` validates tuple arity, footprint size (1..64), duplicates, target membership and peg-mask width, but never the coordinate values. A tampered blob containing `[-5, 3]` or `[1073741824, 0]` passes validation (`BoardSpec` imposes no constraints), producing a degenerate off-canvas board. … Suggested fix: bound row/col in `toPositionOrNull()` against a named constant."*

**Code path, located and read:**

```
Room blob (TEXT)
  └─ SnapshotMapper.decodeOrNull()  →  Json.decodeFromString<ResumeSnapshotDto>  (catches IllegalArgumentException → null)
       └─ ResumeSnapshotDto.toInitialBoardOrNull()
            └─ toBoardSpecOrNull()                       SnapshotMapper.kt:145-161
                 └─ inPlay.mapNotNull { it.toPositionOrNull() }   ← THE GAP
                        ResumeSnapshotDto.kt:110-117 — checks only `size == POSITION_TUPLE_SIZE`
```

`domain/…/model/Position.kt` is a bare `data class Position(val row: Int, val col: Int)` with no constraints — confirming the advisory: nothing downstream rejects a wild coordinate.

**The tightest *sound* bound is `0 until ResumeSnapshotFormat.MAX_BOARD_POSITIONS` (i.e. `0..63`).** Derivation: `Board`'s peg mask is a single `Long`, so `MAX_BOARD_POSITIONS = Long.SIZE_BITS = 64` and no legal footprint can exceed 64 cells. A 64-cell footprint could in principle be a 1×64 line, so 63 is the largest coordinate any legal board could carry. A tighter bound (say 9, the largest `GRID_DIMENSION` across `Boards.kt` — English-33/Triangle use 7, European/Asymmetric use 8, Diamond-41 and Wiegleb-45 use 9; the freeform generator's `FREEFORM_BOUNDING_DIM` is 8) would be tighter *today* but would silently reject a stored snapshot if a future board grows — a fail-**closed** regression that loses a player's in-progress game. Use 64; it reuses an existing named constant, requires no new magic number, and eliminates the entire negative/absurd-coordinate class.

**Fix (in `ResumeSnapshotDto.kt`, at the decode helper — one level below the advisory's suggestion, so the target coordinates get the same treatment for free):**

```kotlin
/** The position a `[row, col]` tuple denotes, or `null` if it is not a well-formed pair. */
internal fun IntArray.toPositionOrNull(): Position? =
    with(ResumeSnapshotFormat) {
        if (size != POSITION_TUPLE_SIZE) return null
        val row = this@toPositionOrNull[POSITION_ROW]
        val col = this@toPositionOrNull[POSITION_COL]
        // WARN-02: a stored blob is untrusted input. A footprint can hold at most
        // MAX_BOARD_POSITIONS cells (the peg mask is one Long), so no legal coordinate
        // can fall outside 0 until MAX_BOARD_POSITIONS. Anything else is corrupt or
        // forged: reject, and let decodeOrNull's fail-safe path start a fresh board.
        if (row !in 0 until MAX_BOARD_POSITIONS || col !in 0 until MAX_BOARD_POSITIONS) return null
        Position(row = row, col = col)
    }
```

`targetRow`/`targetCol` are read as raw `Int`s in `toBoardSpecOrNull` (`SnapshotMapper.kt:148-150`) and do **not** flow through `toPositionOrNull`. They are still constrained by `target in positions`, so an out-of-range target is already rejected — but verify that in the regression test rather than assuming it.

**Regression test shape** (`SnapshotMapperTest`, JVM, no Robolectric needed):

| Case | Expectation |
|------|-------------|
| `inPlay` contains `[-5, 3]` | `decodeOrNull(...)` returns `null` |
| `inPlay` contains `[1073741824, 0]` | returns `null` |
| `inPlay` contains `[64, 0]` (boundary, just outside) | returns `null` |
| `inPlay` contains `[63, 63]` (boundary, just inside) | still decodes (proves the bound is not over-tight) |
| `targetRow/targetCol` out of range | returns `null` |
| **A previously-valid English-33 snapshot** | still decodes to the same board — **the non-regression proof** |

Assert on `decodeOrNull` (the public boundary), not on `toPositionOrNull` (internal), so the test proves the *system* rejects the forgery rather than that one helper does.

### WARN-03 — the public `challengeLevel` getter is unclamped

**Advisory (verbatim, abridged):** *"`GameViewModel.kt:125` exposes `challengeLevel = stampedChallengeLevel ?: routeChallengeLevel` — the raw route arg when no restore stamp exists — and `GameRoute.kt:69,88` passes it into the `Results` route and the screen title. … an out-of-range value would be recorded in the DB under the raw level id and drive 'Next level' as `raw + 1`, while the board actually played was the clamped one. … Suggested fix: have `GameRoute` read the clamped value."*

**The code, read at HEAD.** `GameViewModel` already holds three closely-related values:

```kotlin
val challengeLevel: Int? get() = stampedChallengeLevel ?: routeChallengeLevel        // :125  UNCLAMPED
private val clampedChallengeLevel: Int? =
    routeChallengeLevel?.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)  // :135
private val activeChallengeLevel: Int? get() = stampedChallengeLevel ?: clampedChallengeLevel   // :152
```

`stampedChallengeLevel` is itself clamped on assignment (`:393`). So `activeChallengeLevel` is *already* the correct, clamped, single-boundary value — and its own KDoc says *"Everything level-keyed after construction reads THIS."* The public getter is the one thing that does not.

**The fix is one line**, and it restores exactly the invariant T-05-11 relies on:

```kotlin
/**
 * The Challenge level this game is being played as … WARN-03: this MUST be the
 * clamped value. It flows into the Results route, the DB level key, and the
 * "Next level" action, and an unclamped route arg would key a result under a level
 * that was never actually played.
 */
val challengeLevel: Int? get() = activeChallengeLevel
```

**Defence in depth at the second boundary.** `ResultsViewModel` reads `args.challengeLevel` raw at four places (`:68, :71, :96, :101`) and `ResultsActions.kt:54` computes `onNextLevel(challengeLevel + 1)`. `Results` is a typed nav route in `AppNavHost`, so it is reachable independently of `GameRoute`. Clamp there too:

```kotlin
private val challengeLevel: Int? =
    args.challengeLevel?.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)
```

…and use it in all four places. Note `MIN_CHALLENGE_LEVEL` is currently **duplicated** — a `private const val` in `data/ChallengeLevelSource.kt:55`, a companion `const val` in `GameViewModel:822`, and a `MIN_LEVEL` in `domain/…/generator/LevelGenerator.kt`. Extracting a single `ChallengeLevelBounds` (with `MIN`, `MAX`, and `fun clamp(Int): Int`) into `:domain` alongside `GeneratorConstants` would fix a real no-magic-numbers violation and give both ViewModels one call. Recommended but optional — flag it to the planner as a small refactor rather than smuggling it in.

**Regression test shapes:**

| Test | Location | Assertion |
|------|----------|-----------|
| Route arg `Int.MAX_VALUE` | `GameViewModelTest` (or `GameViewModelChallengeAdvanceTest`) | `viewModel.challengeLevel == GeneratorConstants.MAX_CHALLENGE_LEVEL` |
| Route arg `0` / `-1` | same | `viewModel.challengeLevel == MIN_CHALLENGE_LEVEL` |
| Route arg in range | same | unchanged (non-regression) |
| Restore stamp wins over route arg | `GameViewModelResumeTest` (existing coverage — extend, don't duplicate) | stamped value returned, still clamped |
| Results arg out of range | `ResultsViewModelTest` | recorded `levelId` is the clamped string; `uiState.challengeLevel` is clamped, so `onNextLevel` cannot exceed `MAX + 1` |

The key phrasing for the phase artifact: *a forged or out-of-range value cannot reach the database key or the "Next level" action.* Assert on the observable outcome (recorded `levelId`, `uiState`), not on an internal field.

---

## Common Pitfalls

### Pitfall 1: `buildConfigField` silently generates nothing

**What goes wrong:** You add `buildConfigField(...)` and `BuildConfig.REWARDED_AD_UNIT_ID` does not resolve.
**Why:** `BuildFeatures.buildConfig` defaults to `false` in AGP 8+/9 `[VERIFIED: gradle-api-9.1.0-sources BuildFeatures.kt:49]`. The project does not enable it.
**Avoid:** `buildFeatures { buildConfig = true }` in the same commit.
**Warning sign:** "Unresolved reference: REWARDED_AD_UNIT_ID" that survives a clean build.

### Pitfall 2: String `buildConfigField` values without embedded quotes

**What goes wrong:** Generated `BuildConfig.java` fails to compile with an unresolved identifier.
**Why:** The third argument is a *source literal*, not a value.
**Avoid:** `"\"$id\""`. For `Boolean`/`Int`, no quotes.

### Pitfall 3: The release ID check fires on debug builds and IDE sync

**What goes wrong:** A fresh clone cannot `assembleDebug` or sync in Android Studio — directly violating D-08's "a fresh clone builds successfully".
**Why:** AGP configures every variant on every invocation; a `require(...)` in `buildTypes.release { }` runs during `assembleDebug`. `androidComponents.onVariants(withBuildType("release"))` has the same problem.
**Avoid:** Gate on `gradle.startParameter.taskNames` (see Pattern 6). It is empty during sync, which is the behaviour you want.
**Warning sign:** CI's debug/test job going red for a *release* reason.

### Pitfall 4: `-keep class A` no longer keeps `A`'s constructor

**What goes wrong:** The APK builds and shrinks cleanly, then throws `NoSuchMethodException` / `InstantiationException` at runtime.
**Why:** `android.r8.strictFullModeForKeepRules` defaults to `true` in AGP 9.1.0 `[VERIFIED: BooleanOption.kt:151-156]`.
**Avoid:** Any hand-written keep rule for a reflectively-instantiated class must be `-keep class A { <init>(); }`. Room's shipped rule shows the pattern.
**Warning sign:** A crash that only reproduces in release and mentions a constructor.

### Pitfall 5: The first minified build needs the network

**What goes wrong:** `assembleRelease --offline` fails to resolve `org.jetbrains.kotlin:compose-group-mapping:2.3.20` in a task nobody added.
**Why:** AGP 9.1.0 registers `produceReleaseComposeMapping` when minification is on. Measured this session — the artifact was not in the local cache.
**Avoid:** Warm the cache once online; scope offline claims to `check` (which does not build release).
**Warning sign:** A release build failing on an artifact you never declared.

### Pitfall 6: `@Binds @Singleton` does not scope the concrete type

**What goes wrong:** `App` registers lifecycle callbacks on one instance; the platform layer reads a different, never-updated instance. `awaitActivity()` hangs forever, silently.
**Why:** `@Singleton` on a `@Binds` scopes the *bound* type only.
**Avoid:** `@Singleton` on the impl class; inject the interface everywhere. See the boxed warning in Pattern 1.
**Warning sign:** Ads/consent/PGS calls that never resolve, with no exception.

### Pitfall 7: Folding the gms rule into `DomainPurityTest`

**What goes wrong:** The gate reports green while `ui/` freely imports gms.
**Why:** `@AnalyzeClasses(packages = ["com.app.pegsolitaire.domain"])` scopes the import to `:domain`; a "nothing outside platform" rule evaluated against only `:domain` is vacuous.
**Avoid:** Separate `PlatformBoundaryTest` with `packages = ["com.app.pegsolitaire"]`.
**Warning sign:** The new rule passing on the very first run without any code change — suspicious, and exactly why the violating fixture in §Validation Architecture exists.

### Pitfall 8: An SDK-typed `@Provides` under `di/`

**What goes wrong:** PLAT-04's rule fires on a Hilt-**generated** class, producing a confusing violation naming `DaggerApp_HiltComponents_SingletonC`.
**Why:** Generated components reference every provided type and live in `com.app.pegsolitaire.*`, outside `..platform..`.
**Avoid:** SDK-typed Hilt modules go in `platform/*/di/`. (ARCHITECTURE.md's proposed `di/AdsModule.kt` would trip this.)

### Pitfall 9: Assuming a release-variant unit test exists

**What goes wrong:** You write a test asserting `BuildConfig.REWARDED_AD_UNIT_ID` is not a Google test ID in release, and it never runs.
**Why:** `android.onlyEnableUnitTestForTheTestedBuildType` defaults to `true` in AGP 9.1.0 `[VERIFIED: BooleanOption.kt:157-162]` — only the `testBuildType` (default `debug`) gets a unit-test component, so `testReleaseUnitTest` does not exist.
**Avoid:** Either re-enable it via `androidComponents.beforeVariants { it.enableUnitTest = true }` (`HasUnitTestBuilder.enableUnitTest` `[VERIFIED]`), or — better — make the assertion a **build-time** check (Pattern 6 / a verification task), which is where D-09 already puts it.
**Warning sign:** A green suite that never mentions the test class name.

### Pitfall 10: `proguardFiles(file("proguard-rules.pro"))` before the file exists

**Why:** `android.proguard.failOnMissingFiles` defaults to `true` in AGP 9.1.0 `[VERIFIED: BooleanOption.kt:416]`.
**Avoid:** Create the file in the same commit as the reference.

### Pitfall 11: Losing downloadable fonts to `shrinkResources`

**What goes wrong:** Fredoka/Nunito silently fall back to the default font in release — a visible regression in a phase whose success criterion is invisibility.
**Why:** `com_google_android_gms_fonts_certs` is an array of array *references*.
**Status:** **Verified safe this session** via `aapt2 dump resources` on the shrunk APK — all three arrays survive.
**Avoid anyway:** Include a visual check of the release build in the manual verification list; it is the one player-visible thing R8 could plausibly break here.

---

## Code Examples

### Complete `app/build.gradle.kts` build-type block

```kotlin
android {
    namespace = "com.app.pegsolitaire"

    defaultConfig { /* unchanged */ }

    // AGP 9.1.0: BuildFeatures.buildConfig defaults to FALSE. Without this every
    // buildConfigField below generates nothing and BuildConfig.* will not resolve.
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_APP_ID", "\"${AdIdDefaults.TEST_APP_ID}\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${AdIdDefaults.TEST_REWARDED}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${AdIdDefaults.TEST_INTERSTITIAL}\"")
            resValue("string", "admob_app_id", AdIdDefaults.TEST_APP_ID)
            resValue("string", "game_services_project_id", AdIdDefaults.TEST_GAMES_PROJECT_ID)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro"),
            )
            buildConfigField("String", "ADMOB_APP_ID", "\"${releaseIds.appId}\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"${releaseIds.rewarded}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"${releaseIds.interstitial}\"")
            resValue("string", "admob_app_id", releaseIds.appId)
            resValue("string", "game_services_project_id", releaseIds.gamesProjectId)
        }
    }

    // UNCHANGED — `debug` is configured, not created, so this and the
    // mergeDebugAssets → copyRoomSchemas dependency below still mean the same thing.
    sourceSets.getByName("debug").assets.srcDir(roomSchemaDir)
}
```

### The single `proguard-rules.pro` this repo needs today

```proguard
# app/proguard-rules.pro
#
# PLAT-05 / D-11. This file is deliberately almost empty: Room, Hilt/Dagger,
# kotlinx-serialization, Navigation, DataStore and (later) the GMS SDKs all publish
# consumer keep rules, and this app performs reflection at exactly one site
# (PegSolitaireDatabase::class.java in DatabaseModule), which Room's own rule covers.
#
# Every rule here MUST carry a comment explaining why it exists. Do not add
# speculative keeps — add a rule only when R8 emits it into
# app/build/outputs/mapping/release/missing_rules.txt or a release crash demands it.
#
# NOTE: AGP 9.1.0 runs R8 in STRICT full mode (android.r8.strictFullModeForKeepRules
# defaults to true), so `-keep class A` does NOT implicitly keep A's default
# constructor. Any future -keep for a reflectively-instantiated class must spell out
# `{ <init>(); }`.

# Hilt 2.60's generated ActivityRetainedComponentBuilder references
# @CanIgnoreReturnValue, but hilt-android does not ship error_prone_annotations at
# runtime (it is compileOnly). The annotation is not needed at runtime.
# Verified against this exact dependency set on 2026-08-27: this is the ONLY rule R8
# requires. Without it the release build fails with
#   "Missing class com.google.errorprone.annotations.CanIgnoreReturnValue".
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
```

### `RewardOutcome` at the call site (the ADS-03-critical mapping)

```kotlin
fun onUndo() {
    viewModelScope.launch {
        when (adGateway.requestRewarded(AdReason.UNDO)) {
            // ADS-03: an assist that could not be paid for is still granted — the
            // player is never dead-ended by an ad that failed to load. Mapping
            // Unavailable to "skip" would break hint/undo offline the moment a real
            // gateway lands in Phase 9.
            RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit
            RewardOutcome.Declined -> return@launch
        }
        if (moveHistory.isEmpty()) return@launch
        // … unchanged
    }
}
```

---

## State of the Art

| Old approach | Current approach | When changed | Impact here |
|--------------|------------------|--------------|-------------|
| `-keep class A` implicitly keeps `A.<init>()` | Strict full mode: it does not | AGP default; `R8_STRICT_FULL_MODE_FOR_KEEP_RULES = true` in AGP 9.1.0 | Every hand-written keep rule must spell out members |
| `buildConfig` generated by default | Off by default; opt in per module | AGP 8.0; `gradle.properties` opt-outs removed in AGP 9 | Mandatory one-line addition |
| Unit tests generated for every build type | Only for the tested build type | `android.onlyEnableUnitTestForTheTestedBuildType = true`, AGP 9.0 | No `testReleaseUnitTest` without extra wiring |
| Legacy `BaseExtension` DSL | New public `com.android.build.api.dsl` interfaces only | AGP 9.0 | Already handled — the convention plugin uses `extensions.configure<ApplicationExtension>` |
| `play-services-ads` (legacy) with `MobileAdsInitProvider` auto-init | GMA Next-Gen with **mandatory explicit** `MobileAds.initialize()` and **no ContentProvider** | Next-Gen GA Apr 2026; preferred SDK from 2026-07-06 | D-07's ads half is moot; CONS-02 is satisfied structurally |
| PGS v1 `GoogleSignIn` + Drive scope for Saved Games | PGS v2 auto sign-in; Drive/Sign-In deps removed | `play-services-games-v2` 21.0.0 | Not this phase, but do not design `GamesService` around a sign-out control — v2 has none |

**Deprecated / outdated in this repo's own docs:**
- CLAUDE.md pins `play-services-ads 23.6.0` (deprecated 2026-02-17, sunsets 2027-06-30) and `play-services-games-v2 ~21.x` (actual 22.0.0) and Hilt 2.57.1 (catalog already at 2.60). CONTEXT permits fixing these in-phase.
- `AdGateway`'s KDoc claim that "Slice 2 swaps only the Hilt binding" is false (Activity requirement + opt-in dialog). CONTEXT names correcting it as in-scope.
- `AdReason`'s KDoc reserving `RETRY` is stale (D-03).
- `PlatformModule`'s KDoc calling its two bindings "the *only* Slice-2 swap points" becomes false once the Activity provider binds there.
- A `hiltViewModel` deprecation warning surfaces at `ui/settings/SettingsRoute.kt:26` ("Moved to package: androidx.hilt.lifecycle.viewmodel.compose"). Pre-existing, unrelated to this phase — mention only so it is not mistaken for phase-introduced noise.

---

## Environment Availability

| Dependency | Required by | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Gradle wrapper | all build work | ✓ | 9.5.1 | — |
| AGP | buildTypes / R8 | ✓ | 9.1.0 (cached) | — |
| JDK 17 toolchain | build | ✓ | resolved | — |
| Android SDK | build | ✓ | `sdk.dir=/Users/ermanergoz/Library/Android/sdk`, build-tools 36.0.0 | — |
| `aapt2` | verifying shrunk resources | ✓ | 36.0.0 | APK Analyzer in Android Studio |
| Network to `dl.google.com` | fetching SDK AARs, `compose-group-mapping` | ✓ (tested — HTTP 200) | — | — |
| `org.jetbrains.kotlin:compose-group-mapping:2.3.20` | **first minified release build** | ✗ → now ✓ (fetched during this session's probe) | 2.3.20 | none — must be downloaded |
| Signing config / keystore for release | proving "release build launches and plays" | ✗ | — | Debug-sign the release APK locally (do not commit the config), or `apksigner sign`; otherwise this is a manual/deferred check |
| `slopcheck` | package legitimacy | ✗ (npm/PyPI/crates only) | — | Direct Google Maven AAR inspection — performed, see §Package Legitimacy Audit |
| `ctx7` CLI / Context7 MCP | library docs | ✗ | — | Official docs via WebFetch + **direct artifact/bytecode inspection** (used throughout; a stronger source than either) |
| Android emulator / device | cold-start baseline (D-12), release smoke test | unknown — not probed | — | Cold-start baseline may need to be a manual/operator task; see Open Question 3 |
| macrobenchmark module | cold-start measurement | ✗ (no `benchmark` module in the repo) | — | `adb shell am start -W` displayed-time sampling, or defer instrumentation to Phase 14 |

**Missing dependencies with no fallback:** none that block the phase.
**Missing dependencies with fallback:** release signing (debug-sign locally); macrobenchmark (use `am start -W`).

---

## Validation Architecture

Nyquist validation is enabled (`workflow.nyquist_validation: true`).

**This phase's validation problem is unusual: its success criterion is the *absence* of change.** "The tests still pass" is nearly worthless as evidence, because they would also pass if half the phase were never implemented. Validation therefore has to prove three different kinds of thing, and the plan should treat them as three different kinds of task:

1. **Nothing broke** — the existing 299 tests stay green, and the release APK still renders the downloadable fonts.
2. **The new seams behave as specified** — including the `Unavailable` branch, which has *no prior art anywhere in the codebase*.
3. **The new gates actually bite** — a gate that has never been observed to fail is indistinguishable from a gate that cannot fail. This is the part that needs deliberate design, below.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2; Robolectric 4.16 for Compose/Android-runtime tests; ArchUnit 1.3.0 (`archunit-junit4`) for boundary gates |
| Config file | `app/build.gradle.kts` (test deps), `build-logic/src/main/kotlin/standards.gradle.kts` (`check` wiring), `config/detekt/detekt.yml` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*" --tests "*NoOpAdGateway*" --tests "*SnapshotMapper*"` |
| Full suite command | `./gradlew check` (runs detekt + ktlintCheck + dpSpLiteralCheck + `:app:testDebugUnitTest` + `:domain:test`) |
| Current baseline | **299 tests green** |

### Phase Requirements → Test Map

| Req ID | Behaviour | Test type | Automated command | File exists? |
|--------|-----------|-----------|-------------------|--------------|
| PLAT-01 | Resumed Activity is published; pausing that Activity clears it; pausing a different one does not | unit | `./gradlew :app:testDebugUnitTest --tests "*ForegroundActivityProviderTest"` | ❌ Wave 0 |
| PLAT-01 | `awaitActivity()` suspends until an Activity resumes, then returns it | unit (`runTest` + Turbine) | same | ❌ Wave 0 |
| PLAT-01 | No class outside `..platform..` (except `App`) depends on `ForegroundActivityProvider`; `ui`/`domain`/`data` never depend on `android.app.Activity` | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest"` | ❌ Wave 0 |
| PLAT-02 | `NoOpAdGateway.requestRewarded` returns `Granted` for every `AdReason` | unit | `--tests "*NoOpAdGatewayTest"` | ✅ (rewrite assertions) |
| PLAT-02 | `onUndo` **proceeds** on `Unavailable` (ADS-03) and aborts on `Declined` | unit | `--tests "*GameViewModelTest"` | ✅ (extend) |
| PLAT-02 | `onHintRequested` proceeds on `Unavailable`; on `Declined` clears `solverThinking` and does not run the solver | unit | `--tests "*GameViewModelTest"` | ✅ (extend) |
| PLAT-02 | End-to-end hint/undo behaviour through the Compose UI is byte-identical to v1.0 | Robolectric UI | `--tests "*HintControlTest" --tests "*UndoControlTest" --tests "*GameFlowTest"` | ✅ (must stay green **unmodified**) |
| PLAT-03 | `NoOpGamesService`: `signIn()` false, `session` = `SignedOut`, every new method is a silent no-op | unit | `--tests "*NoOpGamesServiceTest"` | ✅ (extend) |
| PLAT-04 | No class outside `..platform..` depends on `com.google.android.gms..` / `.ump..` / `.libraries.ads..` | arch | `--tests "*PlatformBoundaryTest"` | ❌ Wave 0 |
| PLAT-04 | **The rule fails when violated** | arch-negative | `--tests "*PlatformBoundaryGateTest"` | ❌ Wave 0 |
| PLAT-05 | Release build minifies and shrinks successfully | build | `./gradlew clean :app:assembleRelease` | n/a — build task |
| PLAT-05 | Downloadable-font cert arrays survive `shrinkResources` | build + tool | `aapt2 dump resources app/build/outputs/apk/release/*.apk \| grep fonts_certs` (expect 3 entries) | n/a |
| PLAT-05 | APK size recorded for D-12 | build | `ls -l app/build/outputs/apk/release/*.apk` — baseline **9,890,332 B**; post-R8 **1,518,323 B** | n/a — already measured |
| PLAT-06 | Debug `BuildConfig` ad unit IDs are Google test IDs (`ca-app-pub-3940256099942544/...`) | unit | `--tests "*BuildConfigIdsTest"` | ❌ Wave 0 |
| PLAT-06 | Release resolution **fails** when an ID is missing/placeholder; **succeeds** when present | unit (pure function in `build-logic`) | `./gradlew -p build-logic test` (see Wave 0 note) | ❌ Wave 0 |
| PLAT-06 | No ad-unit-ID literal anywhere in `app/src/main` | grep gate | `! grep -rn "ca-app-pub-" app/src/main` (as a `doLast` verification task, or a manual check) | ❌ Wave 0 |
| PLAT-07 | `PlayGamesInitProvider` is absent from the merged manifest | Robolectric | `--tests "*ManifestSurfaceTest"` | ❌ Wave 0 |
| PLAT-07 | `INTERNET` is declared | Robolectric | `--tests "*ManifestSurfaceTest"` | ❌ Wave 0 |
| PLAT-07 | Whole suite green with SDK artifacts on the graph | suite | `./gradlew check` | ✅ |
| PLAT-07 | Whole suite green **offline** | suite | `./gradlew check --offline` (after one warm online run) | ✅ |
| DEBT-04 / WARN-02 | Forged coordinates (`-5`, `2^30`, `64`) are rejected at `decodeOrNull`; `63` is accepted; a valid English-33 snapshot still decodes | unit | `--tests "*SnapshotMapperTest"` | ✅ (extend) |
| DEBT-04 / WARN-03 | `challengeLevel` is clamped for out-of-range route args; recorded `levelId` and `onNextLevel` cannot exceed the bound | unit | `--tests "*GameViewModelTest" --tests "*ResultsViewModelTest"` | ✅ (extend) |

### Proving the gates actually bite

This is the part the planner must not skip, and it is why the phase needs a `PlatformBoundaryGateTest` in addition to `PlatformBoundaryTest`.

**Technique: evaluate the production rule against a deliberately-violating fixture and assert that it throws.** ArchUnit rules are ordinary objects, so the same `ArchRule` can be run against a hand-picked import scope:

```kotlin
package com.app.pegsolitaire

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Proves the PLAT-04 / PLAT-01 boundary rules FAIL when violated.
 *
 * A gate that has only ever been observed to pass is indistinguishable from a gate
 * that cannot fail. `archfixture` contains classes that deliberately break each rule;
 * `PlatformBoundaryTest` cannot see them because it imports with
 * `ImportOption.DoNotIncludeTests`, so the production gate stays green while this
 * test proves it has teeth.
 */
class PlatformBoundaryGateTest {
    private val fixtures = ClassFileImporter().importPackages("com.app.pegsolitaire.archfixture")

    @Test
    fun theGmsContainmentRuleFailsOnAViolatingClass() {
        val rule = noClasses()
            .that().resideOutsideOfPackage("..platform..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.google.android.gms..", "com.google.android.ump..")
        assertThrows(AssertionError::class.java) { rule.check(fixtures) }
    }

    @Test
    fun theActivityContainmentRuleFailsOnAViolatingClass() { /* … */ }
}
```

Constraints and caveats the planner must budget for:

- **Keep the two rule definitions from drifting.** Extract each `ArchRule` into a shared `internal object PlatformBoundaryRules` in `src/test/`, referenced by both `@ArchTest` fields and the negative test. Two copies of a rule is how a gate quietly stops matching what it claims to.
- **The fixture must reference a real gms class**, which means `play-services-games-v2` must be on the test classpath — i.e. this negative test lands in the same plan that adds the dependency, or it uses a locally-declared decoy class in package `com.google.android.gms.fake` (workable but ugly). Prefer the former; see Open Question 1.
- **`ImportOption.DoNotIncludeTests` must actually exclude the fixture.** ArchUnit's heuristic keys on the classpath location (`.../test/...`), which holds for `app/src/test`. Verify by observing `PlatformBoundaryTest` stay green *after* the fixture is added — that observation is itself part of the evidence.

**For the build-side gates**, the same principle applies but the mechanism differs:

- **D-09's release-ID failure** cannot be proven by a JUnit test in `:app` (there is no `testReleaseUnitTest`, Pitfall 9). Extract the resolution into a pure function in `build-logic` (e.g. `object AdIdResolution { fun resolve(env: Map<String,String>, local: Map<String,String>, isRelease: Boolean): Result }`) and unit-test it in `build-logic/src/test/kotlin` with `junit` — including the "release + missing → failure with a message naming the key" case. `build-logic` is an included build, so wire root `check` to it (`tasks.named("check") { dependsOn(gradle.includedBuild("build-logic").task(":check")) }`) or accept that it runs as a separate command and say so explicitly in `06-VERIFICATION.md`.
- **`shrinkResources` not eating the fonts** is proven by the `aapt2 dump resources` grep above; commit the output as evidence.
- **The R8 keep-rule set being minimal** is proven by the *absence* of `missing_rules.txt` content after a successful `assembleRelease`. Consider a verification task asserting `missing_rules.txt` is empty or absent.

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "<touched area>"` (~20-40 s warm)
- **Per wave merge:** `./gradlew check` (~90-120 s warm) — must include detekt, ktlint, dpSpLiteralCheck, both ArchUnit classes and all 299+ tests
- **After any `app/build.gradle.kts` or manifest change:** additionally `./gradlew clean :app:assembleRelease` and re-check the APK size + `aapt2 dump resources` font-cert grep
- **Phase gate:** full suite green **and** `./gradlew check --offline` green (after one warm online run) before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProviderTest.kt` — covers PLAT-01
- [ ] `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryTest.kt` — covers PLAT-04, PLAT-01
- [ ] `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryGateTest.kt` + `app/src/test/java/com/app/pegsolitaire/archfixture/` — proves the gates bite
- [ ] `app/src/test/java/com/app/pegsolitaire/ManifestSurfaceTest.kt` — covers PLAT-07
- [ ] `app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt` — covers PLAT-06 (debug side)
- [ ] `build-logic/src/test/kotlin/AdIdResolutionTest.kt` + a `testImplementation(libs.junit)` line in `build-logic/build.gradle.kts` — covers PLAT-06 (release-failure side). **`build-logic` currently has no test source set at all.**
- [ ] `app/proguard-rules.pro` — must exist before `proguardFiles` references it (Pitfall 10)
- [ ] Extend existing: `NoOpAdGatewayTest`, `NoOpGamesServiceTest`, `GameViewModelTest`, `GameViewModelChallengeAdvanceTest`, `GameViewModelResumeTest`, `SnapshotMapperTest`, `ResultsViewModelTest`
- [ ] No framework install needed — every library required is already in the catalog

---

## Security Domain

`security_enforcement` is not set to `false`, so this section applies. Phase 6 is a security-relevant phase: it opens the app's first network permission, introduces the first build-time secret handling, and closes two logged advisories.

### Applicable ASVS categories

| ASVS category | Applies | Standard control |
|---------------|---------|-----------------|
| V2 Authentication | No | Play Games sign-in is Phase 11; the No-Op declines. Nothing in this phase authenticates. |
| V3 Session Management | No | No sessions. `GamesSignInState` is added as a type but always `SignedOut`. |
| V4 Access Control | No | Single-player, local-only. No authorisation surface. |
| V5 Input Validation | **Yes** | The whole of DEBT-04. Untrusted inputs: (a) the Room resume blob — decoded with `kotlinx-serialization`, arity/size/duplicate/target-membership checked, **coordinate range added by WARN-02**, moves re-validated against `RulesEngine.legalMoves` on replay; (b) nav route args — `coerceIn` against `GeneratorConstants.MAX_CHALLENGE_LEVEL`, **extended to the public getter and to `ResultsViewModel` by WARN-03**. Pattern: reject-and-fail-safe at the boundary, never trust and never crash. |
| V6 Cryptography | No | The AdMob/PGS IDs are **not** cryptographic secrets — they ship inside the APK. D-08's rationale is fork-abuse prevention, not confidentiality. Do not encrypt them, do not treat `local.properties` as a keystore, and do not let the phase drift into building a secrets manager. |
| V12 Files & Resources | Marginal | `local.properties` is read at build time only, is gitignored, and never ships. Verify it stays gitignored and that no ID is echoed into a build log. |
| V14 Configuration | **Yes** | New: `INTERNET` permission, R8/minification, provider removal, per-build-type IDs. Every one of these is a configuration-security change and belongs in the manifest-merger diff evidence. |

### Known threat patterns for this stack

| Pattern | STRIDE | Standard mitigation | Status in this phase |
|---------|--------|---------------------|---------------------|
| Forged/tampered local resume blob producing a degenerate board | Tampering | Bound every decoded field at the deserialization boundary; fail safe to a fresh board | **WARN-02 fix + 6 regression cases** |
| Out-of-range nav arg keying a DB row or driving navigation | Tampering | Clamp at the route boundary; single clamped value used everywhere | **WARN-03 fix + regression cases at both boundaries** |
| Real ad unit IDs shipped in a debug/CI build → invalid traffic → **non-appealable** AdMob suspension | Repudiation / policy | `buildConfigField` per build type; Google test IDs in debug; a test asserting the debug IDs carry the test publisher prefix | **PLAT-06 / D-05** |
| Real IDs committed to a public repo → a fork serves ads against this account | Spoofing | Untracked `local.properties` + CI secret; committed template with placeholders | **D-08** |
| Test IDs silently shipped to production (zero revenue, discovered weeks later) | — | Configuration-time failure on missing/placeholder release IDs | **D-09** |
| New `INTERNET` permission and (later) `AD_ID` misaligned with the Play Data-safety declaration | Information disclosure / policy | Diff the manifest-merger report in the same PR as each SDK; update Data safety and "Contains ads" before rollout | Manifest diff is a Phase-6 deliverable; the Console declarations are Phase 14 (REL-01) |
| SDK auto-init firing before consent (CONS-02) | Policy / privacy | Remove auto-init providers; explicit, post-consent initialisation | **D-07** — and note the ads half is satisfied structurally by Next-Gen's mandatory explicit init |
| An `Activity` retained by a process-lifetime singleton | DoS (memory) / crash | `WeakReference` + clear on pause/destroy; LeakCanary in debug | **PLAT-01 pattern** |
| An unminified release shipping full debug metadata and dead code | Information disclosure | R8 full mode + `shrinkResources` | **PLAT-05** — measured 84.6% reduction |
| A new exported Activity arriving via manifest merge (`PlayGamesAppShortcutsActivity`, `android:exported="true"`) | Elevation of privilege (minor) | Audit the merged manifest for every newly-exported component | Add to the manifest-diff evidence; it is a Google-signed shortcuts entry point, expected but should be *noticed* rather than discovered later |

**Threat-model note for the planner:** `05-SECURITY.md`'s register (T-05-01 … T-05-13) is the precedent format. Phase 6's new surface is small and enumerable: one new permission, one removed provider, two new build-time inputs, one new process-lifetime singleton, two tightened input boundaries. A short register is appropriate; a long one would be padding.

---

## Assumptions Log

| # | Claim | Section | Risk if wrong |
|---|-------|---------|---------------|
| A1 | `play-services-games-v2` needs no hand-written keep rules because `play-services-basement`'s consumer rules cover its SafeParcelable/`@Keep` reflection | Standard Stack, R8 | A release crash in Phase 11 only, after PGS is actually *called*. **Mitigation: re-run a minified release build in Phase 11 with live PGS calls before trusting this.** The AAR genuinely ships no `proguard.txt` — that part is verified. |
| A2 | Declaring `buildTypes { debug { … } }` configures the existing `debug` type and leaves the `sourceSets.getByName("debug")` / `mergeDebugAssets → copyRoomSchemas` wiring intact | Pattern 5 | The Room `MigrationTest` would fail with "Cannot find the schema file in the assets folder". Evidence is indirect (the R8 probe declared only `release` and left debug untouched). **Cheap direct check: run `--tests "*MigrationTest"` immediately after adding the block.** |
| A3 | Gating on `gradle.startParameter.taskNames` reliably distinguishes release invocations from IDE sync and debug builds | Pattern 6 | A fresh clone fails to sync (violating D-08), or a release slips through unvalidated. Multi-project task-name forms (`:app:assembleRelease`, `bundleRelease`, `assemble`) need care — `assemble` builds *both* variants. |
| A4 | PGS v2's leaderboard/achievement intents need only `startActivity`, not `startActivityForResult` | Pattern 3 | A Phase-11 UI affordance that opens and returns nothing useful. v1's `RESULT_RECONNECT_REQUIRED` is gone in v2, but this was not verified against a running app. |
| A5 | `ImportOption.DoNotIncludeTests` excludes `app/src/test` classes from `PlatformBoundaryTest`'s import | Pattern 4, Validation | The deliberately-violating fixture would fail the *production* gate, making the phase unmergeable. ArchUnit's heuristic is path-based and `app/src/test` matches, but confirm on first run. |
| A6 | The 299-test suite currently passes | Verified Baseline | Test-result XML was read from a previous build, not re-run this session. If the working tree has drifted, the "nothing broke" baseline is wrong. **Run `./gradlew check` once at phase start to establish the real number.** |
| A7 | No cold-start measurement tooling exists and no device/emulator was probed | Environment, Open Q3 | D-12's cold-start baseline may be unachievable as an automated task and may need to become an operator/manual step. |
| A8 | `MIN_CHALLENGE_LEVEL` being duplicated across three files is worth consolidating | DEBT-04 | Low. If the planner declines the refactor, the WARN-03 fix still works — it just repeats a constant. |

---

## Open Questions (RESOLVED)

> **All six questions below were resolved during planning on 2026-08-27** — the first four by explicit user decision (recorded as D-14 through D-18 in `06-CONTEXT.md`), the last two by follow-on decisions in the same session. Each carries an inline **RESOLVED** note. Retained in full because the reasoning that led to each answer is worth preserving.

1. **Does Phase 6 put the SDK artifacts on the dependency graph, or not?** — *the single most consequential planning question in this phase.* — **RESOLVED (D-14):** yes, add them unreferenced; the size/cold-start baselines are captured in earlier plans (06-03, 06-08), enforced structurally by 06-09's `depends_on`. Three APK numbers recorded.
   - What we know: CONTEXT says *"This phase ships no real SDK behavior"*; ROADMAP success criterion 3 says *"`./gradlew check` runs green and offline **with the new SDK artifacts on the graph**"*; STATE says the INTERNET/R8 work must land *"before a ~5.5MB ads SDK and its transitive graph reach a release build"*; D-12 says record the **pre-SDK** APK size here.
   - What's unclear: "ships no SDK behavior" and "with the SDK artifacts on the graph" are compatible (deps present, no code calls them) — but nothing states the intent.
   - Why it matters: without the artifacts, PLAT-07's provider-removal test passes vacuously, PLAT-04's violating fixture has no real gms class to reference, and PLAT-05's keep rules are unverified against the SDKs. With them, R8 strips nearly all of the unreferenced ads SDK, so a post-add size measurement is not the Phase-14 comparand D-12 wants.
   - **Recommendation:** add the artifacts, and split the phase so the size/cold-start baseline is captured in an *earlier plan* than the one adding them. Record **three** numbers: pre-R8 pre-SDK (9,890,332 B — already measured), post-R8 pre-SDK (1,518,323 B — already measured), post-R8 with-SDKs-unreferenced (new). That satisfies D-12's "pre-SDK" literally while making SC-3 and PLAT-07 provable.

2. **Does D-07 survive contact with the finding that `MobileAdsInitProvider` does not exist?** — **RESOLVED (D-17):** yes. Only `PlayGamesInitProvider` is removed; the justification is corrected to cold start + CONS-02 (not test isolation), recorded in the manifest comment.
   - What we know: verified — the Next-Gen AAR declares no ContentProvider; only `PlayGamesInitProvider` is real.
   - Recommendation: keep D-07's *outcome* (no auto-init on the cold-start path), remove `MobileAdsInitProvider` from its wording, and record the reason in the manifest comment. Also correct D-07's stated justification: the JVM test suite was never at risk (Robolectric finding), so the sole reason is production cold-start + CONS-02.

3. **How is the "pre-SDK cold-start baseline" (D-12) actually measured?** — **RESOLVED:** option (b) — `adb shell am start -W` sampled on a fixed device, committed as a text artifact. Planned as 06-08, marked manual/`autonomous: false`.
   - What we know: there is no `benchmark` module, no `androidTest` source set, and no device/emulator was probed.
   - Options: (a) add a macrobenchmark module — real work, and arguably Phase-14 scope; (b) `adb shell am start -W com.app.pegsolitaire/.MainActivity` sampled N times on a fixed device, recorded as a committed text artifact; (c) defer entirely to Phase 14 and accept there is no before-picture.
   - **Recommendation:** (b). It is a manual/operator task producing a committed number, honest about its precision, and costs nothing. Flag it as manual-only in VALIDATION.

4. **Can "a release build still launches and plays" (success criterion 4) be verified this phase?** — **RESOLVED (D-16):** yes, by debug-signing the release APK locally for the smoke test. No `signingConfig` is committed; real signing stays Phase 14.
   - Blocker: no signing config, so `assembleRelease` produces an unsigned APK that cannot be installed.
   - Recommendation: debug-sign the release APK locally for the smoke test (do **not** commit a `signingConfig`), or declare the criterion manual-only and satisfied by an operator-run check. Either way, say which in VERIFICATION rather than leaving it implied.

5. **Should `MIN_CHALLENGE_LEVEL` / `MAX_CHALLENGE_LEVEL` be consolidated into a `ChallengeLevelBounds` in `:domain`?** — **RESOLVED (D-15):** yes, consolidate. Planned as 06-06. `DifficultyTiers.Curve.MIN_LEVEL` is deliberately NOT absorbed (playtest-tunable curve parameter, semantically distinct) — the new object's KDoc must say so.
   - What we know: `MIN_CHALLENGE_LEVEL` exists as a private const in `data/ChallengeLevelSource.kt:55`, a companion const in `GameViewModel:822`, and as `MIN_LEVEL` in `domain/…/LevelGenerator.kt`. WARN-03's fix adds a fourth clamp site in `ResultsViewModel`.
   - Recommendation: consolidate. It is a genuine CLAUDE.md no-magic-numbers violation, the fix is small, and it makes the WARN-03 clamp a single call rather than a repeated expression. But surface it as an explicit decision — it touches `:domain` in a phase that otherwise does not.

6. **Which `check` invocation is the phase's offline contract?** — **RESOLVED (D-18):** `./gradlew check --offline` is the enforced invariant. `assembleRelease` needs one warm online run per machine/runner for `compose-group-mapping`; stated in VERIFICATION.
   - `./gradlew check` does not build release, so it is genuinely offline-safe. `assembleRelease` is not, until `compose-group-mapping` is cached.
   - Recommendation: state in VERIFICATION that `check --offline` is the enforced invariant and that `assembleRelease` requires one warm online run per machine/runner.

---

## Sources

### Primary — HIGH confidence (direct artifact / bytecode / build inspection, 2026-08-27)

- **This repository**, executed: `./gradlew :app:assembleRelease --offline` (baseline 9,890,332 B); the same with an `--init-script` R8 overlay (1,518,323 B, `missing_rules.txt`, `compose-group-mapping` resolution failure); `aapt2 dump resources` on the shrunk APK (font-cert arrays survive)
- **`com.android.tools.build:gradle-api:9.1.0-sources.jar`** (local Gradle cache) — `BuildFeatures.kt` (`buildConfig` default `false`), `BuildType.kt` (`isMinifyEnabled`, `isShrinkResources`), `VariantDimension.kt` (`buildConfigField`, `resValue`, `manifestPlaceholders`, `proguardFiles`), `CommonExtension.kt` (`getDefaultProguardFile`), `Optimization.kt` / `KeepRules.kt` (incubating), `ApplicationVariantDimension.kt`, `HasUnitTestBuilder.kt`
- **`com.android.tools.build:gradle:9.1.0-sources.jar`** — `com/android/build/gradle/options/BooleanOption.kt`: `FULL_R8` default `true`, `R8_STRICT_FULL_MODE_FOR_KEEP_RULES` default `true`, `FAIL_ON_MISSING_PROGUARD_FILES` default `true`, `ONLY_ENABLE_UNIT_TEST_BY_DEFAULT_FOR_THE_TESTED_BUILD_TYPE` default `true`
- **`org.robolectric:robolectric:4.16.jar`** and **`shadows-framework-4.16.jar`** — `javap` on `AndroidTestEnvironment` (only `FakeMediaProvider`, property-gated) and `ShadowContentResolver.getProvider(String, Context)` (lazy `PackageManager.resolveContentProvider` → `createAndInitialize`); no `installContentProviders` / `ContentProviderData` anywhere in the 4.16 jars
- **Google Maven AARs downloaded from `https://dl.google.com/dl/android/maven2`** and unpacked: `ads-mobile-sdk-1.4.0.aar` (manifest: **no ContentProvider**; `proguard.txt` present), `play-services-games-v2-22.0.0.aar` (manifest: `PlayGamesInitProvider`, `PlayGamesAppShortcutsActivity` exported; **no `proguard.txt`**), `user-messaging-platform-4.0.0.aar`, `play-services-basement-18.9.0.aar`, `play-services-base-18.9.0.aar`
- **Local Gradle cache consumer rules read directly:** `room-runtime.aar`, `room-ktx-2.8.4.aar`, `hilt-android-2.60.aar`, `dagger-2.60.jar`, `kotlinx-serialization-core-jvm-1.11.0.jar`, `navigation-common-release.aar`, `navigation-compose-release.aar`, `navigation-runtime-release.aar`
- **Repository source read in full:** `app/build.gradle.kts`, `build-logic/src/main/kotlin/{android-app,standards}.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `settings.gradle.kts`, `app/src/main/AndroidManifest.xml`, `App.kt`, `MainActivity.kt`, `platform/ads/*`, `platform/games/*`, `di/PlatformModule.kt`, `DomainPurityTest.kt`, `GameViewModel.kt`, `GameRoute.kt`, `ResultsViewModel.kt`, `ResultsActions.kt`, `data/resume/{ResumeSnapshotDto,SnapshotMapper}.kt`, `domain/…/{Position,BoardSpec,Boards,GameStatus,GeneratorConstants}.kt`, `config/detekt/detekt.yml`

### Secondary — MEDIUM-HIGH confidence (official documentation)

- https://developer.android.com/build/manage-manifests — `tools:node="remove"` is a no-op when the element is absent; `xmlns:tools` required on `<manifest>`; markers stripped from app-module output
- https://developers.google.com/admob/android/next-gen/quick-start — app ID passed via `InitializationConfig.Builder`; UMP requires the app ID in the manifest; `initialize()` is **mandatory** (else `UninitializedPropertyAccessException`); must be called on a **background thread**
- https://developers.google.com/admob/android/next-gen/test-ads — rewarded `ca-app-pub-3940256099942544/5224354917`, interstitial `.../1033173712`; emulators are test devices by default
- https://developers.google.com/games/services/android/troubleshooting — the PGS `APP_ID` meta-data must reference a **string** resource, digits only
- https://developer.android.com/build/releases/agp-9-0-0-release-notes and https://developer.android.com/build/releases/gradle-plugin-roadmap — AGP 9 uses the new public DSL interfaces exclusively; legacy `BaseExtension` removed

### Project artifacts

- `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md` — decisions D-01…D-13 (locked)
- `.planning/ROADMAP.md` §Phase 6, §External Prerequisites; `.planning/REQUIREMENTS.md` (PLAT-01…07, DEBT-04, ADS-03, CONS-02); `.planning/STATE.md`
- `.planning/research/{STACK,PITFALLS,ARCHITECTURE}.md` — milestone-level; **corrected in five places by this document** (ads init provider, games consumer rules, Robolectric providers, R8-out-of-the-box, `GameStatus` sealed-vs-enum)
- `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-SECURITY.md:87-102` — WARN-02 / WARN-03 original advisory text
- `.planning/milestones/v1.0-phases/05-persistence-resume-settings/05-VALIDATION.md` — the VALIDATION.md format this phase should follow

### Tools unavailable

- Context7 MCP and the `ctx7` CLI (`command -v ctx7` → not found). Compensated by direct artifact/bytecode inspection, which is a stronger source than either for the questions asked.
- `slopcheck` (npm/PyPI/crates only; no Maven backend). Compensated by first-party registry download + AAR inspection.

---

## Metadata

**Confidence breakdown:**

| Area | Level | Reason |
|------|-------|--------|
| Build DSL (buildTypes, buildConfig, resValue, minify) | **HIGH** | Read from the AGP 9.1.0 sources this build resolves, not from docs |
| R8 behaviour and keep-rule surface | **HIGH** | Measured by running the real build; the failure, the fix, and both APK sizes are observed facts |
| Robolectric ContentProvider behaviour | **HIGH** | Bytecode inspection of the exact 4.16 jars on the classpath; resolves a previously UNVERIFIED flag |
| SDK manifest/provider/keep-rule facts | **HIGH** | AARs downloaded from the canonical Google repository and unpacked |
| Repository state (call sites, gates, baseline) | **HIGH** | Read and executed directly |
| DEBT-04 code paths and fixes | **HIGH** | Advisory text located; every cited line read at HEAD; the bound derivation is from existing constants |
| ArchUnit rule shapes | **MEDIUM-HIGH** | Syntax is standard ArchUnit 1.3.0 and mirrors the working `DomainPurityTest`; the `App` exemption and `DoNotIncludeTests` behaviour are reasoned, not executed |
| `ForegroundActivityProvider` shape | **MEDIUM-HIGH** | Standard, well-established pattern; the Hilt scoping trap is a known Dagger semantic; not compiled in this repo |
| `GamesService` extension shape | **MEDIUM** | Judgement call in Claude's-discretion territory; the PGS v2 intent contract is documented but unexercised here |
| Release-ID configuration-time failure mechanism | **MEDIUM** | The `startParameter.taskNames` approach is widely used and CC-compatible, but not executed in this repo |
| Cold-start baseline mechanism | **LOW** | No tooling exists, no device probed — see Open Question 3 |

**Research date:** 2026-08-27
**Valid until:** ~2026-09-26 (30 days). The AGP/Gradle/Kotlin/Robolectric findings are pinned-version facts and stay true as long as the catalog does. The GMA Next-Gen findings are the fastest-moving — the SDK ships roughly monthly; re-verify the AAR manifest at Phase 9 if `ads-mobile-sdk` is bumped past 1.4.0.
