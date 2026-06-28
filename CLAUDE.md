<!-- GSD:project-start source:PROJECT.md -->
## Project

**Peg Solitaire (Android)**

A polished native Android peg solitaire game with two modes: a **Classic** mode of well-known hand-defined boards, and a **Challenge** mode that serves an effectively infinite stream of sequential, procedurally-generated levels that are each unique and guaranteed solvable. Players earn 1–3 stars per level. For casual puzzle players on Android.

**Core Value:** Infinite, never-repeating, always-solvable Challenge levels that feel fair and satisfying — if nothing else works, the procedurally-generated puzzles must be solvable and fun.

### Constraints

- **Tech stack**: Native Kotlin + Jetpack Compose (Material 3) — chosen for performance and tight Play integration.
- **Architecture**: Clean Architecture per the official Android guide (https://developer.android.com/topic/architecture) — UI layer with **MVVM** (StateFlow UiState, unidirectional data flow), optional Domain layer of use cases, Data layer using the **Repository pattern** as single source of truth. Hilt for DI; Coroutines/Flow for async.
- **Game logic isolation**: domain logic is pure Kotlin, fully unit-testable, with no Android dependencies; ads and Play Games sit behind interfaces (`AdGateway`, `GamesService`).
- **Rules**: orthogonal-only jumps across both modes.
- **Code standards (mandatory)**:
  - No magic numbers or strings in logic/UI — extract to constants files, **split by scope** (e.g. per-feature/per-layer constants objects) rather than one global dump.
  - Compose: use **state hoisting** (stateless composables; state owned by ViewModels), per the locally installed `compose-skill`.
  - Use a **dimens** resource file for all UI dimensions (spacing, sizes) — no hardcoded dp/sp scattered in composables.
  - **detekt** and **ktlint** configured and passing — treated as a build/verification gate, not optional.
- **Git workflow**: follow **Gitflow strictly** — long-lived `main` (releases) and `develop` (integration); work on `feature/*` branches off `develop`, `release/*` for release prep, `hotfix/*` off `main`. No direct commits to `main`/`develop` outside merges.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Technologies
| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | **2.3.20** | Language | Pinned to 2.3.20 (relaxed from 2.4.0 on 2026-06-28) so a matching KSP build exists — KSP's latest (symbol-processing 2.3.9) targets Kotlin 2.3.20; no KSP build exists for 2.4.x. KSP2 is built-in/default from Kotlin 2.x. Pure-Kotlin domain layer is the project's core requirement. |
| Android Gradle Plugin (AGP) | **9.2.0** | Build | Latest stable (April 2026). Required by Hilt 2.57.x's Gradle plugin. New DSL is the default; opt-out removed in AGP 10. |
| Gradle | **9.1+** (use latest 9.x wrapper) | Build engine | AGP 9.x requires Gradle ≥9.1. |
| JDK | **17** | Toolchain | AGP 9.x minimum; set via Gradle toolchain (`jvmToolchain(17)`). |
| Jetpack Compose (BOM) | **2026.06.00** | UI toolkit | BOM pins all Compose artifacts to a mutually-compatible set (UI/Foundation ~1.11.x). Single source of truth for Compose versions — never hand-pin individual Compose libs. |
| Compose Compiler plugin | matches Kotlin (`org.jetbrains.kotlin.plugin.compose` @ 2.3.20) | Compose codegen | Since Kotlin 2.0 the Compose compiler ships with Kotlin and is applied as a Gradle plugin versioned to Kotlin — do **not** use the old standalone `composeCompiler` artifact. |
| Material 3 | via Compose BOM (`androidx.compose.material3`) | Design system | Material 3 (M3) is the current Compose design system; M3 Expressive components are available in the 2026 BOM. Use M3, not Material 2. |
| Hilt (Dagger) | **2.60** | Dependency injection | Standard DI for Android; required by the project. Pinned 2.60 (up from 2.57.1 on 2026-06-28): 2.57.1's Gradle plugin looks for the legacy Android `BaseExtension`, which AGP 9.2.1 no longer registers ("Android BaseExtension not found"); 2.60 uses AGP 9's new DSL. Run the compiler via **KSP**, not KAPT. |
| Hilt Navigation Compose | **androidx.hilt 1.3.0** | VM injection in Compose | `hiltViewModel()` for scoping ViewModels to nav destinations. |
| KSP (Kotlin Symbol Processing) | **2.3.9** (`com.google.devtools.ksp`) | Annotation processing | Faster than KAPT; required by modern Room/Hilt. KSP2 is the default engine. KSP now uses its own patch numbering aligned to the Kotlin line — `symbol-processing 2.3.9` is built against Kotlin compiler **2.3.20** (verify the `kotlin-stdlib` dependency of the chosen KSP build matches the Kotlin pin). |
| kotlinx-coroutines | **1.11.0** | Async / concurrency | Standard async substrate; `StateFlow`/`SharedFlow` for MVVM UiState + one-off effects. `-android` for Dispatchers.Main, `-test` for tests. |
| Navigation Compose | **2.9.8** | Screen navigation | Type-safe navigation between Home / Classic / Challenge / Gameplay / Results. Use the stable Navigation 2.9 line (see "What NOT to use" re: Navigation 3). |
### Persistence — Room vs DataStore (the flagged decision, resolved)
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
# gradle/libs.versions.toml
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
- Everything above except the Slice 2 SDK block.
- `AdGateway` and `GamesService` are no-op/auto-grant stubs bound in a Hilt module — production impls are swapped in Slice 2 without touching gameplay code.
- Domain layer (`RulesEngine`, `LevelGenerator`, `Solver`, `ScoreCalculator`) is a **pure-Kotlin module/package with zero Android deps** → JVM unit tests only, highest coverage target.
- Add `play-services-ads` + UMP behind `AdGateway`; add `play-services-games-v2` behind `GamesService`.
- Cloud save (Play Games Saved Games) becomes a *second data source* behind the existing `ProgressRepository` — repositories stay the single source of truth.
- Pin exact SDK versions at this point (they move fast).
## Version Compatibility
| Package | Compatible With | Notes |
|---------|-----------------|-------|
| AGP 9.2.1 | Gradle ≥9.1, JDK 17, Hilt **2.60** | AGP 9.2.1 dropped the legacy Android `BaseExtension`; Hilt's Gradle plugin needs **2.60** (2.57.x throws "Android BaseExtension not found" on AGP 9.2.1). |
| Kotlin 2.3.20 | KSP `2.3.9` (built against Kotlin 2.3.20), Compose compiler plugin 2.3.20, Coroutines 1.11.0, Serialization 1.11.0 | KSP's `kotlin-stdlib` dependency MUST equal the Kotlin pin. KSP2 is default. (Kotlin 2.3.21 has no matching KSP build — stay on 2.3.20.) |
| Compose BOM 2026.06.00 | Kotlin 2.3.20 via Compose compiler plugin | BOM pins UI/Foundation/Material3/Animation together — don't override member versions. Verified compatible with the Kotlin 2.3.20 Compose compiler. |
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
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
