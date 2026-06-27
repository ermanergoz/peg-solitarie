# Stack Research

**Domain:** Native Android single-player puzzle game (Kotlin + Jetpack Compose, Clean Architecture / MVVM)
**Researched:** 2026-06-28
**Confidence:** HIGH (core Android stack verified against official docs/release notes; Slice 2 SDK exact versions MEDIUM — pin at integration time)

> Compose *implementation details* (composable structure, UDF, state hoisting, modifiers, recomposition discipline) are governed by the locally installed **`compose-skill`** (`~/.claude/skills/compose-skill`). This file picks the **libraries and versions**; the skill dictates *how* the Compose code is written.

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | **2.4.0** | Language | Current stable. KSP2 is built-in/default from Kotlin 2.x; KSP1 is incompatible with Kotlin ≥2.3. Pure-Kotlin domain layer is the project's core requirement. |
| Android Gradle Plugin (AGP) | **9.2.0** | Build | Latest stable (April 2026). Required by Hilt 2.57.x's Gradle plugin. New DSL is the default; opt-out removed in AGP 10. |
| Gradle | **9.1+** (use latest 9.x wrapper) | Build engine | AGP 9.x requires Gradle ≥9.1. |
| JDK | **17** | Toolchain | AGP 9.x minimum; set via Gradle toolchain (`jvmToolchain(17)`). |
| Jetpack Compose (BOM) | **2026.06.00** | UI toolkit | BOM pins all Compose artifacts to a mutually-compatible set (UI/Foundation ~1.11.x). Single source of truth for Compose versions — never hand-pin individual Compose libs. |
| Compose Compiler plugin | matches Kotlin (`org.jetbrains.kotlin.plugin.compose` @ 2.4.0) | Compose codegen | Since Kotlin 2.0 the Compose compiler ships with Kotlin and is applied as a Gradle plugin versioned to Kotlin — do **not** use the old standalone `composeCompiler` artifact. |
| Material 3 | via Compose BOM (`androidx.compose.material3`) | Design system | Material 3 (M3) is the current Compose design system; M3 Expressive components are available in the 2026 BOM. Use M3, not Material 2. |
| Hilt (Dagger) | **2.57.1** | Dependency injection | Standard DI for Android; required by the project. 2.57.x adds AGP 9 support. Run the compiler via **KSP**, not KAPT. |
| Hilt Navigation Compose | **androidx.hilt 1.3.0** | VM injection in Compose | `hiltViewModel()` for scoping ViewModels to nav destinations. |
| KSP (Kotlin Symbol Processing) | plugin version matching Kotlin (e.g. `2.4.0-2.0.x`) | Annotation processing | Faster than KAPT; required by modern Room/Hilt. KSP2 is the default engine. Always pick the KSP plugin whose prefix equals your Kotlin version. |
| kotlinx-coroutines | **1.11.0** | Async / concurrency | Standard async substrate; `StateFlow`/`SharedFlow` for MVVM UiState + one-off effects. `-android` for Dispatchers.Main, `-test` for tests. |
| Navigation Compose | **2.9.8** | Screen navigation | Type-safe navigation between Home / Classic / Challenge / Gameplay / Results. Use the stable Navigation 2.9 line (see "What NOT to use" re: Navigation 3). |

### Persistence — Room vs DataStore (the flagged decision, resolved)

**Use both, split by data shape. Rule: queryable/multi-row history → Room; single small scalars → DataStore.**

| Data (from spec §10) | Store | Why |
|----------------------|-------|-----|
| `LevelResult` (per-level stars, bestTimeMs, bestScore, completedAt) | **Room** | Many rows, keyed by `(mode, levelId)`; you'll query "best score for level N", "total stars", "results since X". Relational queries + upsert-on-better-score are exactly Room's job. |
| `InProgressGame` resume snapshot (serializedBoard, moveHistory, elapsedMs) | **Room** (single-row table, board+history stored as a JSON blob via a TypeConverter) | Keep all game state in one transactional store; one persistence mechanism for results + resume avoids a second Proto schema. Serialize the blob with kotlinx.serialization. |
| `ChallengeProgress` (highestLevelReached, currentLevel) | **DataStore (Preferences)** | Two scalar ints, read on app start, no queries — a key-value tuple, not a table. |
| `Settings` (soundOn, hapticsOn, …) | **DataStore (Preferences)** | Canonical DataStore use case; replaces SharedPreferences with a Flow-based async API. |

| Technology | Version | Purpose |
|------------|---------|---------|
| Room | **2.8.4** (`androidx.room`) | Structured local DB for results + resume snapshot. **Use Room 2.8.x stable, NOT Room 3.0** (see What NOT to Use). Wire the compiler via KSP (`room-compiler`), use `room-ktx` for coroutine/Flow DAOs. |
| DataStore Preferences | **1.2.1** (`androidx.datastore:datastore-preferences`) | Settings + scalar challenge progress. |
| kotlinx-serialization-json | **1.11.0** | Serialize the board + move-history blob stored in the Room snapshot row; also enables type-safe nav args. Apply `org.jetbrains.kotlin.plugin.serialization` @ Kotlin version. |

### Board Rendering & Animation

| Technology | Source | Purpose | Why |
|------------|--------|---------|-----|
| Compose `Canvas` / `DrawScope` | Compose BOM (`androidx.compose.foundation`) | Draw the board grid, holes, pegs | A peg board is a small set of geometric primitives on a grid — `Canvas` (drawCircle/drawRect/Path for triangle/diamond outlines) gives full control, scales across board footprints, and is the spec-chosen approach. No game engine needed. |
| `pointerInput` + `detectTapGestures` / drag | `androidx.compose.foundation` | Peg selection / tap-to-jump (and optional drag) | Hit-testing tapped cell → board coordinate is trivial math over the grid. |
| Compose Animation (`Animatable`, `animate*AsState`, `updateTransition`) | Compose BOM (`androidx.compose.animation`) | Jump arc, peg removal fade/scale | Drive a peg's `Offset`/alpha with `Animatable` inside the Canvas; coroutine-based, frame-synced, cancellable. Sufficient for jump/removal feel; no physics lib required. |

### Slice 2 (deferred — interfaces now, SDKs later)

| Technology | Version | Purpose | Notes |
|------------|---------|---------|-------|
| Google Play Games Services v2 | `com.google.android.gms:play-services-games-v2` (pin latest at integration, ~21.x) | Sign-in, leaderboards, achievements, cloud (Saved Games) | v2 SDK only — v1 APIs are being removed (deprecated APIs dropped from the SDK starting May 2026). Sits behind the `GamesService` interface. |
| Google Mobile Ads (AdMob) | **play-services-ads 23.6.0** | Rewarded (hint/undo/retry) + interstitial (between levels) | Use 23.6.0 stable for current privacy/UMP compliance. A "Next-Gen" GMA SDK was announced Jan 2026 — evaluate at Slice 2, but the classic `play-services-ads` line remains the safe default. Sits behind the `AdGateway` interface. |
| User Messaging Platform (UMP) | `com.google.android.ump:user-messaging-platform` (latest) | GDPR/consent for ads | Required before serving ads in consent regions. |

### Testing Stack

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit4 | **4.13.2** | Test runner | Default for Android unit + instrumentation tests. Domain layer (RulesEngine, LevelGenerator, Solver, ScoreCalculator) = pure-Kotlin JVM tests — fast, no Android. |
| kotlinx-coroutines-test | **1.11.0** | Coroutine/virtual-time testing | `runTest`, `StandardTestDispatcher` for ViewModel/repository coroutine logic. |
| Turbine | **1.2.1** (`app.cash.turbine`) | Flow/StateFlow assertions | Assert `UiState` emissions from ViewModels and repository Flows deterministically. |
| Google Truth | **1.4.x** (`com.google.truth:truth`) | Fluent assertions | Readable assertions for domain + ViewModel tests. (AssertJ is an equally fine alternative.) |
| MockK | **1.14.x** | Mocking | Mock repositories/gateways (`AdGateway`, `GamesService` stubs) in ViewModel tests. Kotlin-native mocking. |
| Compose UI Test | via Compose BOM (`ui-test-junit4`, `ui-test-manifest`) | Compose interaction tests | `createComposeRule()` / `createAndroidComposeRule()` to drive screens; assert nodes by semantics. |
| Roborazzi | **1.x latest stable** (`io.github.takahirom.roborazzi`) | Screenshot / snapshot testing | **Primary screenshot tool.** Runs on the JVM via Robolectric, works with Hilt and supports post-interaction snapshots — lock the board/results visuals and catch regressions cheaply in CI. |
| Robolectric | latest (transitive w/ Roborazzi) | JVM Android runtime | Enables Roborazzi + JVM-side Android unit tests. |
| Compose Preview Screenshot Testing | AGP built-in (`screenshotTest` source set) | Lightweight preview snapshots | Optional secondary: drop `@Preview`s into `screenshotTest/` for zero-config golden images of static composables. Use alongside (not instead of) Roborazzi. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Gradle Version Catalog (`libs.versions.toml`) | Single source of truth for all versions | **Mandatory.** Declare versions, libraries, bundles, plugins in `gradle/libs.versions.toml`; reference via `libs.*`. Keeps the many androidx/Compose/Hilt/Room versions consistent. |
| KSP Gradle plugin | Symbol processing for Hilt + Room | Apply `com.google.devtools.ksp`; add Hilt + Room compilers as `ksp(...)` deps. No KAPT anywhere. |
| Hilt Gradle plugin | `com.google.dagger.hilt.android` | Required alongside the Hilt runtime. |
| Android Studio (current stable, 2026.x) | IDE | Match Compose/AGP tooling; needed for Compose previews + `screenshotTest`. |
| LeakCanary (debug-only) | Memory-leak detection | Optional but cheap insurance for a long-session game with animations. |

## Installation (Gradle version catalog excerpt)

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.4.0"
agp = "9.2.0"
ksp = "2.4.0-2.0.0"            # prefix MUST equal kotlin version; verify exact suffix at setup
composeBom = "2026.06.00"
hilt = "2.57.1"
androidxHilt = "1.3.0"
room = "2.8.4"
datastore = "1.2.1"
coroutines = "1.11.0"
serialization = "1.11.0"
navigationCompose = "2.9.8"
turbine = "1.2.1"
truth = "1.4.4"
mockk = "1.14.2"
roborazzi = "1.4.0"           # use latest stable 1.x; verify at setup
playAds = "23.6.0"

[libraries]
compose-bom            = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui             = { group = "androidx.compose.ui", name = "ui" }
compose-foundation     = { group = "androidx.compose.foundation", name = "foundation" }
compose-material3      = { group = "androidx.compose.material3", name = "material3" }
compose-animation      = { group = "androidx.compose.animation", name = "animation" }
compose-ui-tooling     = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
navigation-compose     = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android           = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler          = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose= { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "androidxHilt" }
room-runtime           = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx               = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler          = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
datastore-preferences  = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
coroutines-android     = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test        = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
serialization-json     = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serialization" }
turbine                = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
truth                  = { group = "com.google.truth", name = "truth", version.ref = "truth" }
mockk                  = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
roborazzi              = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose      = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
play-ads               = { group = "com.google.android.gms", name = "play-services-ads", version.ref = "playAds" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization= { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp                 = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt                = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
roborazzi           = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Room 2.8.4 (stable) | Room 3.0 (`androidx.room3`) | Only if you go Kotlin Multiplatform / iOS (explicitly out of scope here). Room 3.0 was still in alpha as of June 2026 — not for a production greenfield game now. |
| Room (for results + resume) | Proto DataStore (typed) | If you wanted *zero* SQL and a single typed object per concern. Rejected: you need queries/aggregation over per-level results, which DataStore can't do. |
| Compose `Canvas` | Custom `View` + `Canvas` / a 2D game engine (LibGDX/Godot) | Engine only if you later need particles/physics/sprite-heavy scenes. For a static grid puzzle it's massive overkill and breaks the native-Compose decision. |
| Roborazzi | Paparazzi | Paparazzi if you want pure-JVM rendering with no Robolectric. Rejected as primary: Paparazzi is tied to Studio/layoutlib releases and can't snapshot *after* an interaction; Roborazzi handles interactions + Hilt. |
| Navigation Compose 2.9 | Navigation 3 (`navigation3`) | Nav3 if you want full back-stack control and it has matured. Rejected for now: newer/less battle-tested; a 5-screen game doesn't need it. |
| MockK | mockito-kotlin | Either works; MockK is more idiomatic Kotlin. Many domain classes are pure and need no mocks at all. |
| Hilt | Koin / manual DI | Koin if you want runtime/no-codegen DI. Project explicitly chose Hilt (compile-time safety, official Android default). |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **Room 3.0 (`androidx.room3`) right now** | Still alpha as of June 2026; KMP-focused (a non-goal here); breaking package/API changes. | Room **2.8.4** stable. |
| **KAPT** | Slower builds; Room 3 dropped it and the ecosystem is KSP-first. | **KSP** for Hilt + Room. |
| **Standalone Compose Compiler artifact** (`androidx.compose.compiler:compiler`) | Obsolete since Kotlin 2.0 — the compiler ships with Kotlin. | `org.jetbrains.kotlin.plugin.compose` plugin at your Kotlin version. |
| **Material 2** (`androidx.compose.material`) | Legacy design system. | **Material 3** (`androidx.compose.material3`). |
| **SharedPreferences / EncryptedSharedPreferences** | Synchronous, error-prone; ESP deprecated. | **DataStore Preferences**. |
| **Play Games Services v1** | v1 APIs being removed from the SDK (starting May 2026). | **Play Games Services v2** (`play-services-games-v2`). |
| **Google H5 Games Ads** | HTML5-only; incompatible with native build (already in Out of Scope). | **AdMob** (`play-services-ads`). |
| **LiveData / RxJava for new state** | Coroutines/Flow is the project + ecosystem standard. | `StateFlow`/`SharedFlow`. |
| **Hand-pinned individual Compose versions** | Drift/incompatibility across Compose modules. | **Compose BOM** governs all Compose artifacts. |

## Stack Patterns by Variant

**Slice 1 (Playable Core):**
- Everything above except the Slice 2 SDK block.
- `AdGateway` and `GamesService` are no-op/auto-grant stubs bound in a Hilt module — production impls are swapped in Slice 2 without touching gameplay code.
- Domain layer (`RulesEngine`, `LevelGenerator`, `Solver`, `ScoreCalculator`) is a **pure-Kotlin module/package with zero Android deps** → JVM unit tests only, highest coverage target.

**Slice 2 (Online & Monetization):**
- Add `play-services-ads` + UMP behind `AdGateway`; add `play-services-games-v2` behind `GamesService`.
- Cloud save (Play Games Saved Games) becomes a *second data source* behind the existing `ProgressRepository` — repositories stay the single source of truth.
- Pin exact SDK versions at this point (they move fast).

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| AGP 9.2.0 | Gradle ≥9.1, JDK 17, Hilt 2.57.x | Hilt 2.57.x added AGP 9 support — older Hilt will break on AGP 9. |
| Kotlin 2.4.0 | KSP plugin `2.4.0-*`, Compose compiler plugin 2.4.0, Coroutines 1.11.0, Serialization 1.11.0 | KSP plugin prefix MUST equal the Kotlin version. KSP1 is incompatible with Kotlin ≥2.3 (KSP2 is default). |
| Compose BOM 2026.06.00 | Kotlin 2.4.0 via Compose compiler plugin | BOM pins UI/Foundation/Material3/Animation together — don't override member versions. |
| Room 2.8.4 | KSP, Coroutines (room-ktx) | Use `ksp(room-compiler)`, not kapt. |

## Sources

- developer.android.com/jetpack/androidx/releases/compose — Compose BOM 2026.06.00 (HIGH)
- developer.android.com/build/releases/agp-9-2-0-release-notes — AGP 9.2.0 / Gradle 9.1+ / JDK 17 (HIGH)
- kotlinlang.org/docs/releases.html + JetBrains blog — Kotlin 2.4.0 stable, KSP2 default (HIGH)
- mvnrepository.com/artifact/com.google.dagger/hilt-android + developer.android.com/jetpack/androidx/releases/hilt — Hilt 2.57.1, AGP 9 support; androidx.hilt 1.3.0 (HIGH)
- developer.android.com/jetpack/androidx/releases/room + android-developers blog "Room 3.0" + InfoQ — Room 2.8.4 stable / maintenance, Room 3.0 alpha & KMP focus (HIGH)
- developer.android.com/jetpack/androidx/releases/datastore — DataStore Preferences 1.2.1 (HIGH)
- github.com/Kotlin/kotlinx.coroutines/releases — Coroutines 1.11.0 (HIGH)
- github.com/Kotlin/kotlinx.serialization — kotlinx-serialization-json 1.11.0 (HIGH)
- developer.android.com/jetpack/androidx/releases/navigation — Navigation Compose 2.9.8; Navigation 3 status (HIGH)
- github.com/cashapp/turbine/releases — Turbine 1.2.1 (HIGH)
- github.com/takahirom/roborazzi/releases — Roborazzi 1.x; Robolectric+Hilt+interaction support (MEDIUM — pin exact stable at setup)
- developers.google.com/admob/android/sdk + rel-notes — Google Mobile Ads SDK 23.6.0; Next-Gen SDK announced Jan 2026 (HIGH for 23.6.0)
- developer.android.com/games/pgs/android/migrate-to-v2 + /downloads — Play Games Services v2 artifact; v1 removal timeline (MEDIUM — exact version pinned at Slice 2)

---
*Stack research for: native Android Kotlin/Compose puzzle game*
*Researched: 2026-06-28*
