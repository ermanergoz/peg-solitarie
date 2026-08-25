# Phase 5: Persistence, Resume & Settings - Pattern Map

**Mapped:** 2026-07-13
**Files analyzed:** 30 (new + modified)
**Analogs found:** 26 with a real in-repo analog / 30 total (Room stack = 4 files with NO local analog — structural nearest + external pattern given)

All analog paths below were confirmed to exist on disk this session. Line numbers are from the files as read on 2026-07-13.

---

## File Classification

### New files to create

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `data/db/PegSolitaireDatabase.kt` | Room `@Database` | Room | — (first Room DB in repo) | **no analog** — see "No Analog Found" |
| `data/db/LevelResultEntity.kt` | Room `@Entity` | Room CRUD | `domain/.../ScoreResult.kt` (data-class shape only) | partial (shape only; Room annotations new) |
| `data/db/LevelResultDao.kt` | Room `@Dao` | Room suspend/Flow | — (first DAO) | **no analog** |
| `data/db/ResumeSnapshotEntity.kt` | Room `@Entity` | Room CRUD | `domain/.../ScoreResult.kt` (data-class shape only) | partial |
| `data/db/ResumeDao.kt` | Room `@Dao` | Room suspend/Flow | — (first DAO) | **no analog** |
| `data/ProgressRepository.kt` (+ `Impl`) | repository (interface + impl) | Room read-modify-write + Flow | `data/SettingsRepository.kt` (interface + `Impl` + `@Binds`) | role-match (repo-over-persistence) |
| `data/ProgressPersistenceKeys.kt` | scoped constants object | n/a | `data/AppPreferences.kt`, `SettingsPreferences` object | exact (scoped-constants convention) |
| `data/resume/ResumeSnapshotDto.kt` | `@Serializable` DTO | serialization | `navigation/Routes.kt` (`@Serializable data class`) | role-match (only `@Serializable` classes in repo) |
| `data/resume/SnapshotMapper.kt` | mapper (DTO ⇄ domain) | transform | `GameViewModel` restore fold (lines 317-322 of RESEARCH pattern) + `Board`/`BoardSpec`/`Move`/`Position` | role-match |
| `platform/games/GamesService.kt` | platform interface | request-response | `platform/ads/AdGateway.kt` | **exact** |
| `platform/games/NoOpGamesService.kt` | no-op stub | request-response | `platform/ads/NoOpAdGateway.kt` | **exact** |
| `di/DatabaseModule.kt` | Hilt `@Module` (`@Provides`) | DI | `DataModule` companion `provideAppDataStore` (`@Provides @Singleton`) | role-match |
| `ui/settings/SettingsRoute.kt` | Compose route (stateful) | request-response | `ui/game/GameRoute.kt` | role-match |
| `ui/settings/SettingsScreen.kt` | Compose screen (stateless) | render | `ui/home/HomeScreen.kt` | **exact** (Scaffold + TopAppBar + tiles) |
| `ui/settings/SettingsUiState.kt` | UiState (`@Immutable`) | state | `ui/home/HomeUiState.kt` | **exact** |
| `ui/settings/SettingsViewModel.kt` | ViewModel (MVVM) | Flow collect → StateFlow | `ui/home/HomeViewModel.kt` | **exact** |
| **Tests** | | | | |
| `data/ProgressRepositoryTest.kt` | test (in-memory Room) | test | `data/SettingsRepositoryTest.kt` (structure) + Room in-memory (new) | role-match |
| `data/ResumeRepositoryTest.kt` / `SnapshotMapperTest.kt` | test | test | `data/SettingsRepositoryTest.kt` | role-match |
| `data/MigrationTest.kt` | test (Robolectric) | test | — (`MigrationTestHelper` new; no androidTest set) | **no analog** — see Pitfall 1 in RESEARCH |
| `platform/games/NoOpGamesServiceTest.kt` | test | test | `platform/ads/NoOpAdGatewayTest.kt` | **exact** |
| `ui/settings/SettingsViewModelTest.kt` | test | test | `ui/home/HomeViewModelTest.kt` | exact |

### Files to modify

| Modified File | Role | What Changes | Analog for the change |
|---------------|------|--------------|-----------------------|
| `data/ChallengeProgress.kt` | data seam | swap in-memory `MutableStateFlow` for DataStore-backed `StateFlow` via `stateIn`; `advanceToNext`/`reset` become `suspend`/scope writes | `SettingsRepository.kt` fail-open read + `data/AppPreferences.kt` shared store |
| `data/SettingsRepository.kt` | repository | add `soundOn`/`hapticsOn` Flows + setters + `SettingsPreferences` keys | its own `hintsEnabled` (lines 22-61) — copy verbatim |
| `data/ChallengeProgressTest.kt` | test | DataStore-backed + advance-wiring assertions | `SettingsRepositoryTest.kt` temp-file store pattern |
| `data/SettingsRepositoryTest.kt` | test | add sound/haptics cases | its own `hintsEnabled` cases (lines 30-57) |
| `ui/game/GameViewModel.kt` | ViewModel | inject `ProgressRepository` + `ChallengeProgress`; `onLeaveScreen()` save snapshot (Playing only); restore-on-resume in `init`; record result + advance on Won; clear snapshot on solve/restart | its own `init` collect (195-223), Won finalize (324-342), `resetToInitial` (524-540), undo fold (427-464) |
| `ui/game/GameRoute.kt` | Compose route | add `LifecycleEventObserver` → `viewModel.onLeaveScreen()` on `ON_STOP` | `collectAsStateWithLifecycle` style already here; RESEARCH Pattern 6 |
| `navigation/Routes.kt` | nav routes | add `@Serializable data object Settings`; add `resume: Boolean = false` to `Gameplay` | `Home`/`ClassicCatalog` data objects + `Gameplay` (lines 21-51) |
| `navigation/AppNavHost.kt` | nav host | add Settings destination + Home gear route + Continue routing; replace hardcoded `CHALLENGE_ENTRY_LEVEL` with persisted `currentLevel` | existing `composable<>` slots + `toChallengeBoard` (lines 71-132) |
| `ui/home/HomeViewModel.kt` | ViewModel | collect `ProgressRepository.resumeEntries` + `ChallengeProgress.currentLevel` into `HomeUiState` | its own `onboardingSeen` collect (37-45) |
| `ui/home/HomeUiState.kt` | UiState | add Continue entries + challenge level fields | its own `@Immutable data class` (16-19) |
| `ui/home/HomeScreen.kt` | Compose screen | add Continue tile(s) + Settings gear action | its own tiles + `HomeTopBar` action (90-186) |
| `ui/results/ResultsViewModel.kt` | ViewModel | call `ProgressRepository.recordResult`, surface "New best!" flags; advance ChallengeProgress | its own display-only ctor (28-51) — now needs a collaborator |
| `ui/results/ResultsUiState.kt` | UiState | add `newBest*` flags | `HomeUiState.kt` immutable-data-class shape |
| `ui/classic/BoardPreview.kt` (or the card) | Compose | add earned-star pips from `LevelResult` | `results_star_size` dimen + `BoardPreview` (1-51) |
| `di/DataModule.kt` | Hilt module | add `@Binds ProgressRepository`; keep shared DataStore provide | its own `bindSettingsRepository` (47-49) |
| `di/PlatformModule.kt` | Hilt module | add `@Binds GamesService → NoOpGamesService` | its own `bindAdGateway` (21-23) |
| `gradle/libs.versions.toml` | config | add `room` version + 4 room libs + room plugin | existing `[versions]`/`[libraries]`/`[plugins]` blocks |
| `app/build.gradle.kts` | config | apply room plugin, `room { schemaDirectory }`, wire room deps (`ksp`, `testImplementation`) | existing `plugins {}` + `dependencies {}` (1-87) |
| `res/values/dimens.xml` | resources | add star-pip / Continue / Settings dp tokens | existing token blocks (all `dimensionResource`) |
| `res/values/strings.xml` | resources | add Settings/Continue/New-best strings | existing (all `stringResource`) |

---

## Pattern Assignments

### `data/SettingsRepository.kt` (EDIT — repository, DataStore Flow) — HIGH-VALUE analog

**Analog:** itself — the `hintsEnabled` member is the exact fail-open template for `soundOn`/`hapticsOn` (D-15..D-17).

**Interface + scoped keys** (`SettingsRepository.kt` lines 22-37):
```kotlin
interface SettingsRepository {
    val hintsEnabled: Flow<Boolean>
    suspend fun setHintsEnabled(enabled: Boolean)
}

internal object SettingsPreferences {
    val HINTS_KEY = booleanPreferencesKey("hints_enabled")
    const val DEFAULT_HINTS = true
}
```

**Fail-open read + write** (lines 47-61):
```kotlin
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[SettingsPreferences.HINTS_KEY] ?: SettingsPreferences.DEFAULT_HINTS }

    override suspend fun setHintsEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsPreferences.HINTS_KEY] = enabled }
    }
}
```
**Replicate:** add `SOUND_KEY = booleanPreferencesKey("sound_on")` / `HAPTICS_KEY = booleanPreferencesKey("haptics_on")`, both `DEFAULT_* = true` (D-17 fail-open ON), plus `soundOn`/`hapticsOn` Flows and `setSoundOn`/`setHapticsOn` setters — one-for-one copies of the `hintsEnabled` triple. No new DataStore delegate (see Shared Patterns → DataStore).

---

### `data/ChallengeProgress.kt` (EDIT — data seam, DataStore-backed StateFlow)

**Analog:** current `ChallengeProgress` (public surface to preserve) + `SettingsRepository` (fail-open read) + `AppPreferences` (shared store).

**Current surface to keep intact** (`ChallengeProgress.kt` lines 25-43):
```kotlin
@Singleton
class ChallengeProgress @Inject constructor() {
    private val _currentLevel = MutableStateFlow(FIRST_CHALLENGE_LEVEL)
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()
    fun advanceToNext() { _currentLevel.value += 1 }
    fun reset() { _currentLevel.value = FIRST_CHALLENGE_LEVEL }
}
```
**Replicate / change (RESEARCH Pattern 4 + Pitfall 3):** inject the shared `DataStore<Preferences>` + a `CoroutineScope`; back `currentLevel` with
`dataStore.data.catch { emit(emptyPreferences()) }.map { it[CURRENT_LEVEL_KEY] ?: DEFAULT_CURRENT_LEVEL }.stateIn(scope, SharingStarted.Eagerly, DEFAULT_CURRENT_LEVEL)` so `.value` stays available for existing synchronous readers. `advanceToNext()`/`reset()` become writes through `dataStore.edit { }`. Keys go in a scoped object (`intPreferencesKey("challenge_current_level")`, `DEFAULT_CURRENT_LEVEL = 1`) alongside `SettingsPreferences`. **Constructor gains params → add a `@Provides`/constructor-inject scope; verify no `@Binds` needed (still `@Inject` `@Singleton`).**

---

### `platform/games/GamesService.kt` + `NoOpGamesService.kt` (NEW — platform seam) — EXACT analog

**Analog:** `platform/ads/AdGateway.kt` + `platform/ads/NoOpAdGateway.kt` — mirror exactly.

**Interface** (`AdGateway.kt` lines 11-25) → copy shape:
```kotlin
interface AdGateway {
    suspend fun requestRewarded(reason: AdReason): Boolean
    fun showInterstitial()
}
```
**Stub** (`NoOpAdGateway.kt` lines 12-18):
```kotlin
class NoOpAdGateway @Inject constructor() : AdGateway {
    override suspend fun requestRewarded(reason: AdReason): Boolean = true
    override fun showInterstitial() = Unit
}
```
**Replicate:** `GamesService` with a minimal method set (RESEARCH Pattern 5: e.g. `suspend fun signIn(): Boolean` → stub `false`; `fun submitScore(leaderboardId: String, score: Long)` → `Unit`). `NoOpGamesService @Inject constructor() : GamesService` returning the no-op values. **Note the semantic flip:** ad stub grants (`true`); games sign-in stub is "not signed in" (`false`).

**Stub test** (`NoOpAdGatewayTest.kt` lines 12-31) → copy for `NoOpGamesServiceTest`: `runTest { assertThat(gateway.signIn()).isFalse() }` + a `submitScore` does-not-throw case.

---

### `di/PlatformModule.kt` (EDIT — Hilt binding) — EXACT analog

**Analog:** itself (`PlatformModule.kt` lines 20-24):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway
}
```
**Replicate:** add a sibling `@Binds @Singleton abstract fun bindGamesService(impl: NoOpGamesService): GamesService` in the same abstract module.

---

### `di/DatabaseModule.kt` (NEW — Hilt `@Provides`) — structural analog

**Analog:** `DataModule` companion `provideAppDataStore` (`DataModule.kt` lines 56-62):
```kotlin
companion object {
    @Provides @Singleton
    fun provideAppDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.appDataStore
}
```
**Replicate (RESEARCH Code Examples):** an `object DatabaseModule` (`@Module @InstallIn(SingletonComponent::class)`) with `@Provides @Singleton fun provideDatabase(@ApplicationContext ctx): PegSolitaireDatabase = Room.databaseBuilder(ctx, PegSolitaireDatabase::class.java, PersistenceKeys.DB_NAME).build()` (NO `fallbackToDestructiveMigration` — Success Criterion 5) plus `@Provides` DAO accessors. `ProgressRepository` interface→impl binds go on `DataModule` as `@Binds` next to `bindSettingsRepository` (lines 47-49).

---

### `data/ProgressRepository.kt` (+ Impl) (NEW — repository) — role-match

**Analog:** `SettingsRepository` interface + `Impl` + `@Binds` shape (repo-over-persistence, single source of truth). The **Room mechanics have no local analog** — use RESEARCH Pattern 1 (`recordResult` read-modify-write in `withTransaction`, returning per-field "new best" flags for D-09) and Pattern 3 (resume upsert/get/clear by mode).

**Replicate:** interface exposing `recordResult(mode, levelId, ScoreResult): NewBests`, `observeResults(mode): Flow<List<LevelResult>>` (D-10 catalog pips), `saveSnapshot(...)`, `loadSnapshot(mode)`, `clearSnapshot(mode)`, `observeResumeModes(): Flow<List<String>>` (Home Continue). Bind `@Binds @Singleton` in `DataModule`. Defensive decode (Security §V5): wrap `Json.decodeFromString` in try/catch → treat corrupt blob as "no resume".

---

### `data/resume/ResumeSnapshotDto.kt` (NEW — @Serializable DTO) — role-match

**Analog:** `navigation/Routes.kt` — the only `@Serializable` classes in the repo (lines 22, 47-51):
```kotlin
@Serializable
data class Gameplay(
    val levelId: String = "",
    val challengeLevel: Int? = null,
)
```
**Replicate (RESEARCH Pattern 2):** an app-layer `@Serializable data class ResumeSnapshotDto(mode, levelId, challengeLevel, generatorVersion, inPlay, targetRow, targetCol, initialPegs, moves, elapsedMs)`. **`@Serializable` lives in `:app` only** — never on domain types (Pitfall 5; `:domain` purity gate `DomainPurityTest`). The serialization plugin is already applied to `:app` (`app/build.gradle.kts` line 11).

---

### `data/resume/SnapshotMapper.kt` (NEW — DTO ⇄ domain mapper)

**Analog:** the existing `GameViewModel` `initialBoard + moveHistory + fold(applyMove)` model IS the target shape.

**Domain types the mapper bridges** (all pure, no serializer):
- `Board(spec: BoardSpec, pegs: Long)` — `Board.kt` lines 13-16
- `BoardSpec(inPlay: List<Position>, target: Position?)` — **not a data class**, no serializer (`BoardSpec.kt` lines 15-18) → store `inPlay`/`target` explicitly
- `Move(from, over, to: Position)` — `Move.kt` lines 11-15
- `Position(row, col: Int)` — `Position.kt` lines 8-11

**Restore (rebuild by replay, D-06, RESEARCH Pattern 2):**
```kotlin
val spec = BoardSpec(dto.inPlay.map { Position(it[0], it[1]) }, dto.targetRow?.let { Position(it, dto.targetCol!!) })
val initial = Board(spec, dto.initialPegs)
val history = dto.moves.map { Move(Position(it[0],it[1]), Position(it[2],it[3]), Position(it[4],it[5])) }
val current = history.fold(initial) { b, m -> rulesEngine.applyMove(b, m) }
```
Matches `GameViewModel.onUndo` fold verbatim (`GameViewModel.kt` line 433: `moveHistory.fold(base) { b, m -> rulesEngine.applyMove(b, m) }`). **Never call `loadChallenge(n)` to rebuild** (anti-pattern).

---

### `ui/game/GameViewModel.kt` (EDIT — ViewModel) — its own patterns

**Analog:** itself. Concrete hook points:

**Won finalize** — add per-level record + Challenge advance here (`GameViewModel.kt` lines 324-342):
```kotlin
if (status != GameStatus.Playing) {
    tickerJob?.cancel(); tickerJob = null
    if (status == GameStatus.Won && !finishedEmitted) {
        finishedEmitted = true
        ...
        _finished.trySend(scoreCalculator.evaluate(...))
    }
}
```
**Replicate:** at the Won branch also `progressRepository.recordResult(mode, levelId, scoreResult)`, `progressRepository.clearSnapshot(mode)` (D-05 clear-on-solve), and for Challenge `challengeProgress.advanceToNext()` (Pitfall 7 wiring — currently **zero callers**). Whether record happens here or in `ResultsViewModel` is Claude's discretion (D-09 needs the "new best" flags at Results).

**Snapshot source fields already present:** `moveHistory` (line 145), `initialBoard` (line 131), `elapsedSeconds` (in `GameUiState`), `solution`/`generatorVersion` carry-through. `onLeaveScreen()` builds the DTO from these and saves **only while `status == Playing`** (Pitfall 4).

**Restore path:** mirror the `init` async-load branch (lines 207-222) — on `resume=true`, `loadSnapshot(mode)` → map → `GameUiState.initial(...)`, set `moveHistory := history`, `initialBoard := initial` (so undo works immediately).

**Settings-collect template** for haptics (lines 195-202):
```kotlin
viewModelScope.launch {
    settingsRepository.hintsEnabled.collect { enabled -> _uiState.update { it.copy(showHints = enabled) } }
}
```
→ add a parallel `hapticsOn` collect into `GameUiState.hapticsOn`.

---

### `ui/game/GameRoute.kt` (EDIT — lifecycle save)

**Analog:** existing `collectAsStateWithLifecycle` + one-shot `LaunchedEffect` (`GameRoute.kt` lines 35-51). **Replicate (RESEARCH Pattern 6):** add a `DisposableEffect(lifecycleOwner)` registering a `LifecycleEventObserver` that calls `viewModel.onLeaveScreen()` on `Lifecycle.Event.ON_STOP` (D-04), removed in `onDispose`.

---

### `navigation/Routes.kt` + `AppNavHost.kt` (EDIT — nav)

**Routes analog** (`Routes.kt` lines 21-27, 47-51): add `@Serializable data object Settings` (like `Home`) and a `resume: Boolean = false` field on `Gameplay` (defaulted like `challengeLevel`).

**AppNavHost analog** (`AppNavHost.kt` lines 72-104, 123-132): add a `composable<Settings>` slot; add a gear route from Home (`navController.navigate(Settings)`); replace the hardcoded `private const val CHALLENGE_ENTRY_LEVEL = 1` (line 132) + the Home tile call (line 76) so Challenge entry reads the **persisted** `currentLevel` (Pitfall 7); add Continue routing (`Gameplay(..., resume = true)`). Follow `toChallengeBoard`/`toBoardFromCatalog` popUpTo idioms (lines 112-125).

---

### `ui/settings/*` (NEW — screen/VM/UiState) — EXACT Home analogs

- **`SettingsViewModel.kt`** ← `HomeViewModel.kt` (lines 27-57): `@HiltViewModel`, inject `SettingsRepository`, collect `soundOn`/`hapticsOn` into `_uiState` in `init` (mirror the `onboardingSeen` collect at 40-44); `onToggleSound`/`onToggleHaptics` write through the repo in `viewModelScope.launch`.
- **`SettingsUiState.kt`** ← `HomeUiState.kt` (lines 16-19): `@Immutable data class SettingsUiState(val soundOn: Boolean = true, val hapticsOn: Boolean = true)`.
- **`SettingsScreen.kt`** ← `HomeScreen.kt` (lines 50-111): `Scaffold` + `TopAppBar` (with back nav) + centered `Column` capped at `content_max_width`; each toggle a Card/row + M3 `Switch`; all dp via `dimensionResource`, all text via `stringResource`.
- **`SettingsRoute.kt`** ← `GameRoute.kt` (lines 29-68): `hiltViewModel()` + `collectAsStateWithLifecycle` + forward method refs.

---

### `ui/home/*` (EDIT — Continue affordances + gear) — its own analogs

- **`HomeViewModel.kt`** (37-45): add collects of `progressRepository.observeResumeModes()` + `challengeProgress.currentLevel` into `HomeUiState`.
- **`HomeUiState.kt`** (16-19): add `classicResumeAvailable`, `challengeResumeLevel: Int?`, `challengeCurrentLevel: Int`.
- **`HomeScreen.kt`** (90-186): add a Settings gear to `HomeTopBar.actions` (copy the `IconButton` at 102-108, use `top_bar_icon_size`); render Continue tile(s) as peers of `ClassicTile`/`ChallengeTile` (D-01/D-11 "Continue — Level N").

---

### `ui/results/*` (EDIT — "New best!" cue) — its own analog

**`ResultsViewModel.kt`** (28-51) is currently **display-only** (`toRoute<Results>()` passthrough). D-09 requires it (or `GameViewModel`) to consult `ProgressRepository.recordResult` and surface `NewBests` flags into `ResultsUiState`. Add the flags to `ResultsUiState` (analog: `HomeUiState` immutable shape) and render a badge in `ResultsScreen` reusing `results_star_size` conventions.

---

### `ui/classic/BoardPreview.kt` (EDIT — star pips) — its own analog

**`BoardPreview.kt`** (33-51) is a stateless card body reading `dimensionResource`. D-10: add 0-3 earned-star pips sourced from `ProgressRepository.observeResults("classic")` (collected in `ClassicCatalogViewModel`, passed via UiState — do NOT read the repo in the composable). Reuse the `results_star_size` star token pattern.

---

## Shared Patterns

### DataStore fail-open (apply to ALL new DataStore reads)
**Source:** `SettingsRepository.kt` lines 53-56 / `OnboardingRepository.kt` lines 47-50
```kotlin
dataStore.data
    .catch { emit(emptyPreferences()) }
    .map { it[KEY] ?: DEFAULT }
```
**Apply to:** `soundOn`, `hapticsOn`, `currentLevel`. Fresh-install defaults ON/level-1 (D-17). Never throw on a corrupt read.

### Single shared DataStore delegate (do NOT create a second)
**Source:** `AppPreferences.kt` lines 8-24 (docstring: a second `preferencesDataStore` delegate crashes at runtime)
```kotlin
internal val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = AppPreferences.STORE_NAME) // "app_prefs"
```
**Apply to:** every new scalar key (`sound_on`, `haptics_on`, `challenge_current_level`) reuses this one store, Hilt-provided via `DataModule.provideAppDataStore` (`DataModule.kt` 56-62).

### Scoped constants objects (QUAL-03 — no magic strings/numbers)
**Source:** `SettingsPreferences` (`SettingsRepository.kt` 31-37), `AppPreferences` (`AppPreferences.kt` 21-24), `ChallengeProgress.FIRST_CHALLENGE_LEVEL` (line 10)
**Apply to:** DataStore keys + defaults; Room `DB_NAME`, table names, column names → a `ProgressPersistenceKeys` object; mode literals (`"classic"`/`"challenge"`) → a scoped constant. All UI dp/sp → `dimens.xml` (read via `dimensionResource`; the `dpSpLiteralCheck` gate fails on raw literals). All strings → `strings.xml`.

### Hilt binding style
**Source:** `DataModule` (`@Binds @Singleton` on an abstract module, lines 34-49) + companion `@Provides` for instances (56-62); `PlatformModule` (20-24)
**Apply to:** `ProgressRepository`→impl and `GamesService`→`NoOpGamesService` via `@Binds`; `PegSolitaireDatabase`/DAOs via `@Provides` (a `@Database`/DAO cannot be `@Binds`-bound). Note: `@Provides` instance methods can't live on an abstract `@Module` — use an `object DatabaseModule` or a companion.

### Constructor-injectable `@Singleton` seam (no `@Binds` when concrete)
**Source:** `ChallengeProgress` (25-28), `OnboardingRepository` (37-42), `NoOpAdGateway` (12-14)
**Apply to:** keep `ChallengeProgress` `@Inject`-constructor `@Singleton` (add DataStore + scope params) — no `@Binds` needed if it stays a concrete class.

### One-shot events / lifecycle collection (Compose)
**Source:** `GameRoute.kt` `LaunchedEffect(Unit) { viewModel.finished.collect {...} }` (38-51) + `collectAsStateWithLifecycle` (36)
**Apply to:** the `ON_STOP` `DisposableEffect` save (Pattern 6); "New best!" one-shot cue.

### Test style — fail-open DataStore over a TemporaryFolder
**Source:** `SettingsRepositoryTest.kt` (26-75): `TemporaryFolder` rule, `PreferenceDataStoreFactory.create`, `runTest`, `.first()`, plus a throwing store proving fail-open.
**Apply to:** extended `SettingsRepositoryTest` (sound/haptics), `ChallengeProgressTest` (DataStore-backed). For Room tests use `Room.inMemoryDatabaseBuilder` under `@RunWith(RobolectricTestRunner::class)` + Turbine on Flow DAOs (RESEARCH Pitfall 6).

### Stub test style
**Source:** `NoOpAdGatewayTest.kt` (12-31) — `runTest`, `assertThat(...).isTrue()`, does-not-throw.
**Apply to:** `NoOpGamesServiceTest` (assert `signIn()` is `false`, `submitScore` no-throw).

---

## No Analog Found

Room is brand-new to this repo — there is no existing `@Entity`, `@Dao`, `@Database`, `TypeConverter`, or `MigrationTestHelper` usage. For these files the planner should follow **RESEARCH.md Code Examples + Patterns 1/2/3** and the nearest *structural* analog noted:

| File | Role | Data Flow | Nearest structural analog | External pattern to follow |
|------|------|-----------|---------------------------|----------------------------|
| `data/db/PegSolitaireDatabase.kt` | Room `@Database` | Room | none (first DB) | RESEARCH Code Examples "Database declaration" (`@Database(v1, exportSchema=true)`) |
| `data/db/LevelResultDao.kt` | Room `@Dao` | Room suspend/Flow | none | RESEARCH Pattern 1 (`@Upsert` + Flow `observe`) |
| `data/db/ResumeDao.kt` | Room `@Dao` | Room suspend/Flow | none | RESEARCH Pattern 3 (get/upsert/clear/observeModes by mode) |
| `data/MigrationTest.kt` | test | test | none (no `androidTest` set) | RESEARCH Pitfall 1 + "Migration-test scaffold" (Robolectric + `schemas/` on test assets) |
| `data/db/*Entity.kt` | Room `@Entity` | Room CRUD | `ScoreResult` (data-class shape only) | RESEARCH Pattern 1/3 (composite PK `["mode","levelId"]`; single-row PK `mode`) |

**Closest repo-over-persistence analog for the whole Room stack:** how `SettingsRepositoryImpl` wraps DataStore behind an interface (`SettingsRepository.kt` 22-61) — the same "interface + `@Inject` impl + `@Binds` + single-source-of-truth + fail-open/degrade-gracefully" shape carries to `ProgressRepository` over Room, substituting DAOs for the DataStore.

---

## Metadata

**Analog search scope:** `app/src/main/java/com/app/pegsolitaire/{data,di,platform,navigation,ui}` + `domain/src/main/kotlin/.../model` + `domain/.../scoring` + `app/src/test/...` + `gradle/libs.versions.toml` + `app/build.gradle.kts` + `app/src/main/res/values`.
**Files scanned/read:** 24 source + test + config files (all analogs cited above confirmed on disk).
**Pattern extraction date:** 2026-07-13
