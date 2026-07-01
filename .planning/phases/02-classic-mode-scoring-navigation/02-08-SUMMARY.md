---
phase: 02-classic-mode-scoring-navigation
plan: 08
subsystem: navigation
tags: [navigation, compose, material3, datastore, hilt, mvvm, onboarding, robolectric]

# Dependency graph
requires:
  - phase: 02-classic-mode-scoring-navigation
    plan: 05
    provides: "Routes.kt @Serializable Home/ClassicCatalog/Gameplay(levelId)/Results; GameRoute(onFinished,onNavigateBack); interim MainActivity stub"
  - phase: 02-classic-mode-scoring-navigation
    plan: 06
    provides: "ResultsRoute(onNextBoard,onReplay,onCatalog,onHome,onNavigateBack)"
  - phase: 02-classic-mode-scoring-navigation
    plan: 07
    provides: "ClassicCatalogRoute(onSelect,onNavigateBack); BoardPreview read-only board leaf"
  - phase: 02-classic-mode-scoring-navigation
    plan: 03
    provides: "datastore-preferences dep; Home/onboarding strings + dimens"
provides:
  - "Type-safe AppNavHost wiring the full 5-screen spine (Home -> ClassicCatalog -> Gameplay(levelId) -> Results) with back stack + Results actions (NAV-02)"
  - "MainActivity hosts AppNavHost, replacing the Phase-1 direct GameRoute() (app launchable end-to-end)"
  - "Home mode-select screen: enabled Classic tile, disabled Challenge tile + 'Coming soon' badge (NAV-01/D-09/D-10)"
  - "OnboardingRepository: single onboarding_seen DataStore boolean, fail-open (D-16)"
  - "HomeViewModel/HomeUiState gating the first-run + on-demand how-to-play overlay (ONB-01/D-13/D-14/D-15)"
  - "OnboardingOverlay: scrim + surfaceContainerHigh sheet teaching the orthogonal jump"
affects: [phase-03-challenge, navigation, home, onboarding]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Type-safe NavHost over @Serializable routes; navigate with instances, args via toRoute()"
    - "Test-injectable NavHost: each destination body is a default-valued @Composable slot wired to the real *Route, overridable with stand-ins so the graph is drivable without a Hilt host"
    - "DataStore-Preferences repository with an internal DataStore constructor seam (Hilt @Inject delegates) so the fail-open catch path is unit-testable"
    - "open repository class faked via a settable MutableStateFlow for ViewModel tests (no Hilt test infra)"
    - "First-run overlay gated on a Flow<Boolean> collected into @Immutable UiState; help affordance forces-visible independent of the flag"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/java/com/app/pegsolitaire/ui/onboarding/OnboardingOverlay.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/test/java/com/app/pegsolitaire/data/OnboardingRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt

key-decisions:
  - "AppNavHost destination bodies are default-valued @Composable slots (defaulting to the real *Route) so NavFlowTest can drive the graph with stand-ins; the production routes still call hiltViewModel() unchanged"
  - "OnboardingRepository has an internal DataStore-taking constructor + an @Inject Context constructor that delegates over the app's Preferences file — the seam makes default-false, markSeen->true, and the D-16 fail-open path all deterministically unit-testable"
  - "How-to-play affordance uses the core-set Icons.Rounded.Info (the spec's Icons.Rounded.HelpOutline is material-icons-extended, intentionally avoided project-wide)"
  - "Next board / Replay from Results navigate Gameplay(id) with popUpTo<ClassicCatalog>() so board-hops never grow the back stack unbounded (T-02-08-NAV)"
  - "Onboarding step illustrations reuse the tested read-only BoardPreview over a 1x3 jump board rather than a bespoke Canvas, keeping board-role rendering single-sourced and dodging new magic-number gates"

requirements-completed: [NAV-01, NAV-02, ONB-01]

# Metrics
duration: 35min
completed: 2026-06-30
---

# Phase 2 Plan 08: Home + Onboarding + Type-Safe NavHost Summary

**Closed the five-screen spine: a type-safe `AppNavHost` now wires Home -> ClassicCatalog -> Gameplay(levelId) -> Results into `MainActivity` (replacing the Phase-1 direct `GameRoute()`), behind a new Home mode-select screen (Classic live / Challenge "Coming soon") and a first-run how-to-play overlay gated on a fail-open `onboardingSeen` DataStore flag — the app is launchable end-to-end (NAV-01, NAV-02, ONB-01).**

## Performance
- **Duration:** ~35 min
- **Tasks:** 3 (Task 1 via TDD RED -> GREEN)
- **Files:** 10 created, 1 modified

## Accomplishments
- **`OnboardingRepository` (D-16):** a single `booleanPreferencesKey("onboarding_seen")` over a `preferencesDataStore(name="app_prefs")`. `onboardingSeen` is `dataStore.data.catch { emit(emptyPreferences()) }.map { it[SEEN_KEY] ?: false }` — the `catch` is the fail-open path (a corrupt/IO read degrades to "not seen" -> re-teach, never crashes Home). Store name/key/default are scoped constants (`OnboardingPreferences`). An `internal` `DataStore`-taking constructor is the test seam; the `@Inject @ApplicationContext` constructor delegates over the real file. `@Singleton`, constructor-injectable — no Hilt module entry.
- **`HomeViewModel`/`HomeUiState`:** collects `onboardingSeen` into `onboardingVisible = !seen` (first-launch gate, D-14); `onShowHowToPlay()` forces it visible independent of the flag (D-15); `onDismissOnboarding()` persists `markSeen()` + hides (D-13). Mutable flow never leaked.
- **`HomeScreen` + `HomeRoute`:** stateless M3 `Scaffold`/`TopAppBar` with a How-to-play `IconButton`; a `content_max_width`-capped column of two `mode_tile_min_height` tiles — an enabled Classic `Card` (whole-tile -> `onPlayClassic`) and a disabled Challenge `Card` (`enabled = false`) with a `secondaryContainer` "Coming soon" pill (D-09). Hosts `OnboardingOverlay` when `onboardingVisible`. `HomeRoute` is the `hiltViewModel` + `collectAsStateWithLifecycle` seam.
- **`OnboardingOverlay`:** a `scrim`-filled `Box` + centered `surfaceContainerHigh` `Surface` sheet (`shapes.large`, capped `content_max_width`) of three illustrated jump steps (each a read-only `BoardPreview` of a 1x3 jump board + a `bodyMedium` caption from `onboarding_step_1/2/3`) over a primary "Got it" `Button` and a "Skip tutorial" `TextButton`, both -> `onDismiss`.
- **`AppNavHost` (NAV-02):** `NavHost(startDestination = Home)` over the five `@Serializable` routes — `composable<Home/ClassicCatalog/Gameplay/Results>` — wiring Classic -> catalog, catalog -> `Gameplay(id)`, Gameplay finish -> `Results(...)`, Results Next/Replay -> `Gameplay(id)` `popUpTo<ClassicCatalog>()`, Catalog/Home pop-back, and back arrows pop. Destination bodies are default-valued composable slots (defaulting to the real `*Route`) so the graph is test-drivable.
- **`MainActivity`:** `setContent { AppTheme { AppNavHost(rememberNavController()) } }` — the Phase-1 direct `GameRoute()` host is gone; the app now launches into the full back stack.
- **Tests:** `OnboardingRepositoryTest` (real temp-file store: default-false, markSeen->true; throwing store: fail-open-false). `HomeViewModelTest` (fake repo over a settable `MutableStateFlow`: first-launch visible, help forces visible, dismiss persists+hides). `NavFlowTest` (Robolectric + real captured `NavController`: drives Home -> Catalog -> Gameplay -> Results, asserting each route reachable + the `levelId` round-trips via `toRoute<Gameplay/Results>()`).

## Task Commits
1. **Task 1 RED** — failing OnboardingRepository fail-open + HomeViewModel gate spec — `689e981` (test)
2. **Task 1 GREEN** — OnboardingRepository (DataStore fail-open) + HomeViewModel/HomeUiState — `875278c` (feat)
3. **Task 2** — HomeScreen mode tiles + How-to-play + OnboardingOverlay + HomeRoute — `0760a0f` (feat)
4. **Task 3** — AppNavHost wires all 5 destinations; MainActivity hosts it; NavFlowTest — `9afa281` (feat)

_TDD gate sequence verified in git log: the Task 1 `test()` RED (`689e981`) precedes the `feat()` GREEN (`875278c`)._

## Decisions Made
- **Test-injectable NavHost slots:** with no Hilt test infrastructure on the classpath (and adding `hilt-android-testing` out of scope), the real `*Route` composables' `hiltViewModel()` calls can't run in a plain Robolectric Compose test. `AppNavHost` therefore takes each destination body as a default-valued `@Composable` slot wired to the real route; `NavFlowTest` overrides them with tappable stand-ins, so the test exercises `AppNavHost`'s *actual* navigation lambdas (the part NAV-02 cares about) without a Hilt host. Production behavior is unchanged (defaults).
- **DataStore constructor seam:** the `internal constructor(DataStore<Preferences>)` makes the `.catch` fail-open path deterministically testable (a deliberately-throwing `DataStore` proves false-on-error) without touching Robolectric or the real app file.
- **`open` repository for VM faking:** `OnboardingRepository` is `open` so `HomeViewModelTest` can subclass it with a `MutableStateFlow`-backed fake — the same lightweight-fake discipline the codebase uses for `LevelRepository`, with no Hilt test infra.
- **Core-set help icon:** `Icons.Rounded.Info` substitutes the spec's extended-set `Icons.Rounded.HelpOutline`, consistent with 02-05/02-06 avoiding `material-icons-extended`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] AppNavHost made test-injectable to satisfy the NavFlowTest acceptance without Hilt test infra**
- **Found during:** Task 3 (NavFlowTest)
- **Issue:** The plan's `AppNavHost` calls the real `*Route` composables directly; those call `hiltViewModel()`, which crashes in a plain Robolectric `createComposeRule` test (the host activity isn't `@AndroidEntryPoint`). The project has no `hilt-android-testing` dependency, and adding one is out of scope (deps frozen in 02-03; per-rule package adds are not auto-permitted).
- **Fix:** Each destination body is now a default-valued `@Composable` slot defaulting to the real `*Route`; the test overrides the four slots with tappable stand-ins. `AppNavHost`'s navigation lambdas (the NAV-02 contract) are exercised unchanged; production composition uses the defaults, so runtime behavior is identical to the plan.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
- **Committed in:** `9afa281`

**2. [Rule 3 - Blocking] How-to-play icon substituted to the curated core set**
- **Found during:** Task 2 (HomeScreen)
- **Issue:** The UI-SPEC specifies `Icons.Rounded.HelpOutline`, which lives only in `material-icons-extended` — a dependency the project intentionally avoids (only `material-icons-core` is on the classpath).
- **Fix:** Used `Icons.Rounded.Info` (core set) for the How-to-play affordance, with the `how_to_play` content description preserved.
- **Files modified:** app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
- **Committed in:** `0760a0f`

**3. [Rule 3 - Blocking] NavFlowTest reachability via the route-string instead of `hasRoute<T>()`**
- **Found during:** Task 3 (NavFlowTest compile)
- **Issue:** The reified `NavDestination.hasRoute<T>()` is not resolvable on the pinned navigation-compose 2.9.8 test classpath (only the `String`-keyed `hasRoute` member resolved; the `KClass` extension import was unresolved).
- **Fix:** Assert reachability via `currentDestination?.route` containment of each route's simple name, and keep `currentBackStackEntry?.toRoute<Gameplay/Results>()?.levelId` for the typed-arg round-trip — the same NAV-02 coverage with a stable API.
- **Files modified:** app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
- **Committed in:** `9afa281`

**Total deviations:** 3 auto-fixed (all blocking — testability/classpath). No behavioral or scope change to the shipped app.

## Threat Model Coverage
- **T-02-08-DS (DoS on corrupt/empty DataStore read)** — mitigated: `onboardingSeen` wraps `dataStore.data` in `catch { emit(emptyPreferences()) }` and defaults the key to `false` (D-16 fail-open); `OnboardingRepositoryTest.onboardingSeenFailsOpenToFalseWhenTheReadErrors` covers the error path.
- **T-02-08-NAV (Tampering on nav route args)** — mitigated: route args come from trusted in-app sources (catalog-emitted `levelId`, VM-computed Results); `popUpTo<ClassicCatalog>()` on Next/Replay caps back-stack growth across board hops.
- **T-02-08-DEEP (external deep-link)** — accepted: no deep links/exported nav added; `MainActivity` is the single `@AndroidEntryPoint` host and the start destination is fixed to `Home`.

## Known Stubs
- The **Challenge tile is intentionally inert** (`enabled = false`, no `onClick`, no route — D-09): this is the planned Phase-3 reservation, not a blocking stub. Home, the catalog, and the spine are fully live.

## Threat Flags
None — no new security surface beyond the plan's `<threat_model>` (DataStore read + in-app nav route args).

## Verification
- `./gradlew :app:testDebugUnitTest` — green (OnboardingRepositoryTest, HomeViewModelTest, NavFlowTest, plus the full prior suite — no regressions).
- `./gradlew :app:compileDebugKotlin` — succeeds (all 4 Route signatures satisfied; @Serializable routes navigate).
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL (Hilt graph generates incl. OnboardingRepository/HomeViewModel; resources link).
- `./gradlew :app:ktlintCheck :app:detekt :app:dpSpLiteralCheck` — exit 0 (no magic numbers/strings, no raw dp/sp; UDF/state-hoisting preserved).

## Next Phase Readiness
- The MVP spine is live: launch -> Home -> pick a board -> play -> Results -> back, all navigable. Phase 3 drops the real Challenge generator behind the existing `ChallengeLevelSource` stub and can light up the reserved Challenge tile with a `Challenge` route added to `AppNavHost`.

## Self-Check: PASSED

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-30*
