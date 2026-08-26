# Phase 6: Platform Foundation & Build Hardening - Pattern Map

**Mapped:** 2026-08-27
**Files analyzed:** 38 (18 new, 20 modified)
**Analogs found:** 34 / 38

> **How to use this document.** Every "NEW" file below has a named analog already in
> this repo. Copy that file's *shape* — package placement, KDoc density, `@Inject
> constructor` indentation style (note the project's 8-space continuation indent on
> `@Inject constructor(...)` bodies), scoped-constants placement, test structure —
> and change only what the requirement demands. Where RESEARCH.md supplies proposed
> code, this document tells you which existing file that proposed code must be made
> to *look like*.
>
> **Repo-wide conventions confirmed by reading the analogs (apply to every file):**
> - Every public declaration carries a KDoc that names the requirement/decision id
>   (`D-11`, `T-05-04`, `QUAL-03`, `PERS-03`) that motivates it. This is not optional
>   here — it is uniform across all 20 files read.
> - `@Inject constructor` classes use the ktlint `class X\n    @Inject\n    constructor(...) : Y {`
>   form with the body indented 8 spaces (`NoOpAdGateway.kt:12-18`,
>   `GeneratedChallengeLevelSource` at `ChallengeLevelSource.kt:32-52`,
>   `ResultsViewModel.kt:49-54`). Do not reformat; ktlint enforces it.
> - Constants live in a scoped `object` or a `private companion object` at the bottom
>   of the file, one KDoc line each, never inline (`GeneratorConstants`, `ScoreParams`,
>   `ResumeSnapshotFormat`, `GameViewModel`'s `private companion object` at `:811-823`).
> - Untrusted input is rejected at the boundary and degrades fail-safe to `null`,
>   never throws (`SnapshotMapper.decode` at `:118-123`). Programmer error fails fast
>   with `requireNotNull` (`ClassicLevelSource.kt:89`).

---

## File Classification

### New files

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `platform/activity/ForegroundActivityProvider.kt` | platform seam (interface) | event-driven | `platform/ads/AdGateway.kt` | exact (seam interface) |
| `platform/activity/DefaultForegroundActivityProvider.kt` | platform impl (`@Singleton`) | event-driven / StateFlow | `data/ChallengeProgress.kt` (`@Singleton` + `@Inject` + StateFlow) | role-match |
| `platform/ads/RewardOutcome.kt` | model (sealed) | value type | `domain/model/GameStatus.kt` (enum — see ⚠ below) | partial |
| `platform/ads/AdIds.kt` | config accessor | request-response | `data/ResumeSnapshotDto.kt` §`ResumeSnapshotFormat` (scoped-constants object) | partial |
| `platform/games/GamesSignInState.kt` | model (sealed) | value type | `domain/model/GameStatus.kt` | partial |
| `domain/generator/ChallengeLevelBounds.kt` | domain policy object | transform (clamp) | `domain/generator/GeneratorConstants.kt` + `DifficultyTiers.kt:25-27` | exact |
| `app/proguard-rules.pro` | build config | n/a | *(none — first `.pro` in repo)* | **no analog** |
| `local.properties.template` | config template | n/a | *(none)* | **no analog** |
| `build-logic/src/main/kotlin/AdIdResolution.kt` | build utility (pure fn) | transform | `build-logic/src/main/kotlin/standards.gradle.kts:36-73` (pure-Kotlin gate logic in build-logic) | partial |
| `build-logic/src/test/kotlin/AdIdResolutionTest.kt` | test (JVM) | n/a | `app/src/test/.../SnapshotMapperTest.kt` (pure-JVM, Truth, no runner) | role-match |
| `test/.../PlatformBoundaryTest.kt` | test (ArchUnit) | n/a | `app/src/test/.../DomainPurityTest.kt` | exact |
| `test/.../PlatformBoundaryRules.kt` | test utility (shared rules) | n/a | `DomainPurityTest.kt` rule body + `ResumeSnapshotFormat` object idiom | partial |
| `test/.../PlatformBoundaryGateTest.kt` | test (arch-negative) | n/a | *(none — no negative-gate test exists)* | **no analog** |
| `test/.../archfixture/*.kt` | test fixture (violating) | n/a | `GameViewModelTest.kt:1292-1334` (private test doubles at file bottom) | partial |
| `test/.../ManifestSurfaceTest.kt` | test (Robolectric) | n/a | `data/db/PegSolitaireDatabaseTest.kt` | exact |
| `test/.../BuildConfigIdsTest.kt` | test (JVM) | n/a | `platform/ads/NoOpAdGatewayTest.kt` | exact |
| `test/.../platform/activity/ForegroundActivityProviderTest.kt` | test (JVM + MockK + Turbine) | n/a | `platform/ads/NoOpAdGatewayTest.kt` (shape) + `SnapshotMapperTest.kt` (MockK use) | role-match |

### Modified files

| Modified File | Role | Data Flow | Change Driver | Pattern Source |
|---------------|------|-----------|---------------|----------------|
| `platform/ads/AdGateway.kt` | seam interface | request-response | D-01 return type + KDoc correction | itself + `GamesService.kt` KDoc density |
| `platform/ads/AdReason.kt` | enum | value type | D-03 strike stale `RETRY` reservation | itself |
| `platform/ads/NoOpAdGateway.kt` | platform impl | request-response | D-02 return `Granted` | itself |
| `platform/games/GamesService.kt` | seam interface | request-response | PLAT-03 additive methods | itself |
| `platform/games/NoOpGamesService.kt` | platform impl | request-response | PLAT-03 silent no-ops | itself |
| `di/PlatformModule.kt` | DI module | n/a | 3rd `@Binds` + KDoc correction | itself + `di/DataModule.kt` |
| `App.kt` | application | event-driven | `onCreate` + `attachTo` | itself (currently a 2-line class) |
| `app/build.gradle.kts` | build config | n/a | buildFeatures/buildTypes/ID resolution | itself (comment style at `:36-56`) |
| `app/src/main/AndroidManifest.xml` | manifest | n/a | `INTERNET`, `xmlns:tools`, provider removal | itself |
| `gradle/libs.versions.toml` | build config | n/a | SDK artifacts (D-14) | itself (`:6-70` / `:130-138`) |
| `ui/game/GameViewModel.kt` | ViewModel | request-response | `RewardOutcome` `when` + WARN-03 getter | itself (`:125-152`, `:673-761`) |
| `ui/results/ResultsViewModel.kt` | ViewModel | request-response | WARN-03 defence in depth | `GameViewModel.kt:134-135` clamp idiom |
| `data/resume/ResumeSnapshotDto.kt` | DTO + codec helpers | file-I/O (decode) | WARN-02 coordinate bound | itself (`:110-117`, `:124-137`) |
| `data/ChallengeLevelSource.kt` | data source | transform | D-15 use consolidated bounds | itself (`:38`, `:55`) |
| `domain/generator/LevelGenerator.kt` | domain service | transform | D-15 use consolidated bounds | itself (`:63`, `:287`) |
| `build-logic/build.gradle.kts` | build config | n/a | test source set + `testImplementation(libs.junit)` | itself (`:8-18`) |
| `test/.../NoOpAdGatewayTest.kt` | test | n/a | assertions → `RewardOutcome.Granted` | itself |
| `test/.../NoOpGamesServiceTest.kt` | test | n/a | new-method coverage | itself |
| `test/.../GameViewModelTest.kt` | test | n/a | `FakeAdGateway` 3-state; `Unavailable` tests; WARN-03 | itself (`:1292-1334`) |
| `test/.../GameViewModelChallengeAdvanceTest.kt` | test | n/a | `GrantingAdGateway` (`:292`) | itself |
| `test/.../GameViewModelResumeTest.kt` | test | n/a | `GrantingAds` (`:643`) | itself |
| `test/.../SnapshotMapperTest.kt` | test | n/a | WARN-02 regression cases | itself |
| `test/.../ResultsViewModelTest.kt` | test | n/a | WARN-03 clamp cases | itself |
| `test/.../DomainPurityTest.kt` | test (ArchUnit) | n/a | **DO NOT MODIFY** — wrong `@AnalyzeClasses` scope (Pitfall 7) | n/a |

> ⚠ **`GameStatus` is an `enum class`, not sealed** (`domain/model/GameStatus.kt:12`).
> RESEARCH already flags this correction to D-01's rationale. There is **no existing
> sealed type in this repo** — `RewardOutcome` and `GamesSignInState` will be the
> first two. Copy `GameStatus`'s *documentation* pattern (a KDoc listing each member
> with a one-line meaning, plus a note on where the related policy lives) but use
> `sealed interface` + `data object` members as D-01 locks.

---

## Pattern Assignments

### `platform/activity/ForegroundActivityProvider.kt` (platform seam interface, event-driven)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt` (whole file, 25 lines)

The seam-interface shape to copy — package-level KDoc naming the requirement, a
sentence saying who fulfils it now and who will fulfil it later, and one KDoc per
method describing the *contract callers must honour*, not the implementation:

```kotlin
// AdGateway.kt:1-25 — the exact shape a new seam interface takes here
package com.app.pegsolitaire.platform.ads

/**
 * The platform seam through which gameplay requests rewarded grants and shows
 * interstitials (spec §8.5, D-11). In Slice 1 this is fulfilled by an auto-grant
 * [NoOpAdGateway] stub so hint/undo are free; Slice 2 swaps only the Hilt binding
 * (`PlatformModule`) for a real AdMob-backed implementation — no gameplay or UI
 * change. Modelled on the [com.app.pegsolitaire.data.LevelRepository] interface +
 * `@Inject`-constructor-impl + `@Binds` pattern.
 */
interface AdGateway {
    /**
     * Requests a rewarded grant for [reason], returning whether the reward is granted.
     * ...callers must treat a `false` return as "not granted" and skip the assisted action.
     */
    suspend fun requestRewarded(reason: AdReason): Boolean

    fun showInterstitial()
}
```

**Apply:** RESEARCH.md §Pattern 1 already supplies the target interface body. Wrap it
in this exact KDoc register — name PLAT-01, name `PlatformBoundaryTest` as the gate
that enforces containment, and (per RESEARCH) document the deliberate
`onActivityPaused` null so Phase 9 does not "fix" it.

**Also copy from this analog:** the split of interface and impl into two files in the
same package (`AdGateway.kt` / `NoOpAdGateway.kt`), rather than nesting the impl.

---

### `platform/activity/DefaultForegroundActivityProvider.kt` (platform impl, event-driven)

**Analog (impl shape):** `app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt`
**Analog (`@Singleton` + StateFlow):** `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt`

Impl formatting — the ktlint-enforced `@Inject constructor` layout with the 8-space
body indent:

```kotlin
// NoOpAdGateway.kt:12-18
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
        override suspend fun requestRewarded(reason: AdReason): Boolean = true

        override fun showInterstitial() = Unit
    }
```

`@Singleton`-on-the-class placement (this is the pattern that avoids the Hilt scoping
trap RESEARCH boxes in Pattern 1 — `ChallengeProgress` is the one existing class in
the repo annotated `@Singleton` on the class itself, precisely because it holds state):

```kotlin
// ChallengeProgress.kt:43-49
@Singleton
class ChallengeProgress
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        val currentLevel: StateFlow<Int> = ...
```

> **Deliberate divergence:** `ChallengeProgress` injects `@ApplicationScope
> CoroutineScope` to drive `stateIn`. RESEARCH.md §Pattern 1 explicitly refines away
> that dependency — `resumed.mapNotNull { it?.get() }.first()` needs no scope. Copy
> the `@Singleton class ... @Inject constructor()` *shell* from `ChallengeProgress`,
> not its `stateIn` body.

**Contrast to document in KDoc:** `NoOpAdGateway` / `NoOpGamesService` deliberately do
**not** carry `@Singleton` on the class (only the `@Binds` in `PlatformModule` is
scoped) because nothing injects their concrete type. State that this provider differs
and why, so a future reader does not "normalise" it back.

---

### `platform/ads/RewardOutcome.kt` and `platform/games/GamesSignInState.kt` (models)

**Analog:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/GameStatus.kt` (whole file, 16 lines)

```kotlin
// GameStatus.kt:1-16 — the member-by-member KDoc register to copy
package com.app.pegsolitaire.domain.model

/**
 * Lifecycle status of a game.
 *
 * - [Playing]: at least one legal move remains.
 * - [Won]: the board is solved (a single peg remains).
 * - [Stuck]: no legal moves remain but more than one peg is left.
 *
 * Win/lose copy tiers (D-05) live in the UI layer; the domain only reports status.
 */
enum class GameStatus {
    Playing,
    Won,
    Stuck,
}
```

**Apply:** keep the bulleted `- [Member]: meaning` list and the closing sentence that
says where the *adjacent* concern lives. For `RewardOutcome` that closing sentence is
the ADS-03 fail-open rule (`Unavailable` → the caller proceeds anyway). RESEARCH.md
§Pattern 2 supplies the member set and per-member KDoc — it is already written in this
register; use it near-verbatim.

**File placement:** one type per file, named after the type, in the same package as
its consumer seam (`platform/ads/`, `platform/games/`) — matching
`AdReason.kt` sitting beside `AdGateway.kt`.

---

### `domain/generator/ChallengeLevelBounds.kt` (domain policy object, D-15)

**Analog (constants object):** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/GeneratorConstants.kt`
**Analog (clamp function on a policy object):** `domain/generator/DifficultyTiers.kt:19-27`

The scoped-constants-object header and per-constant KDoc weight to match:

```kotlin
// GeneratorConstants.kt:1-15 + :31-42
package com.app.pegsolitaire.domain.generator

/**
 * Scoped, frozen constants for the deterministic Challenge generator.
 *
 * Split by scope (QUAL-03's no-magic-number rule): every value is a KDoc'd
 * `const val` rather than a literal inlined in [LevelGenerator], mirroring the
 * `ScoreParams` precedent. ...
 */
object GeneratorConstants {
    /**
     * Hard upper ceiling on the (untrusted) Challenge level number (CR-01, T-03-09).
     * `generate` replays the deterministic chain from level 1 to the target ...
     * a forged nav arg (e.g. `challengeLevel = 2_000_000_000`) would otherwise loop
     * billions of times and hang. ... a value above it plays the same board as the cap.
     */
    const val MAX_CHALLENGE_LEVEL = 10_000
```

The clamp-helper idiom (a policy object exposing a function whose KDoc states the
untrusted-input rationale):

```kotlin
// DifficultyTiers.kt:19-27
object DifficultyTiers {
    /**
     * The [DifficultyTier] for [levelNumber]. Any `levelNumber < 1` is clamped to
     * level 1 (T-03-01: an untrusted nav-arg level must never crash), so
     * `forLevel(0)` and `forLevel(-5)` both return the level-1 tier.
     */
    fun forLevel(levelNumber: Int): DifficultyTier {
        val level = levelNumber.coerceAtLeast(Curve.MIN_LEVEL)
```

**The four duplication sites the new object must absorb** (verified by grep at HEAD):

| Site | Current code |
|------|--------------|
| `data/ChallengeLevelSource.kt:55` | `private const val MIN_CHALLENGE_LEVEL = 1` |
| `data/ChallengeLevelSource.kt:38` | `levelNumber.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)` |
| `ui/game/GameViewModel.kt:822` | `const val MIN_CHALLENGE_LEVEL = 1` (in `private companion object`) |
| `ui/game/GameViewModel.kt:135` and `:393` | the same `coerceIn(...)` expression, twice |
| `domain/generator/LevelGenerator.kt:287` | `private const val MIN_LEVEL = 1` |
| `domain/generator/LevelGenerator.kt:63` | `levelNumber.coerceIn(MIN_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)` |
| `domain/generator/DifficultyTiers.kt:82` | `const val MIN_LEVEL = 1` (inside `private object Curve`) |

> **Scope caution.** `DifficultyTiers.Curve.MIN_LEVEL` is documented as a *curve*
> parameter ("Levels between each +1 step…" neighbours it) and `Curve` is explicitly a
> playtest-tunable knob block. Absorbing it is optional; absorbing the other three
> (which are all literally "the lowest valid Challenge level, clamp target") is the
> D-15 intent. Whichever you choose, say so in the new object's KDoc.

**Placement:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/` beside
`GeneratorConstants.kt`. Note that `MAX_CHALLENGE_LEVEL` already lives on
`GeneratorConstants` with a load-bearing KDoc and is referenced by name in six places —
prefer *adding* `MIN` + `clamp(Int)` in a new sibling object that delegates to it over
moving `MAX_CHALLENGE_LEVEL` (a move touches the frozen-generator-contract file).

---

### `data/resume/ResumeSnapshotDto.kt` — WARN-02 coordinate bound (decode boundary)

**Analog:** the file itself — `toMoveOrNull` at `ResumeSnapshotDto.kt:124-137` and the
fail-safe boundary at `SnapshotMapper.kt:118-123`.

The exact helper being modified, and its sibling that already shows the
`with(ResumeSnapshotFormat) { ... }` + guard + `null` shape:

```kotlin
// ResumeSnapshotDto.kt:109-117 — THE GAP (size checked, values not)
/** The position a `[row, col]` tuple denotes, or `null` if it is not a well-formed pair. */
internal fun IntArray.toPositionOrNull(): Position? =
    with(ResumeSnapshotFormat) {
        if (size == POSITION_TUPLE_SIZE) {
            Position(row = this@toPositionOrNull[POSITION_ROW], col = this@toPositionOrNull[POSITION_COL])
        } else {
            null
        }
    }
```

The constant to reuse — **do not introduce a new bound**:

```kotlin
// ResumeSnapshotDto.kt:96-100
/**
 * A `Board`'s peg mask is a single `Long`, so a footprint can hold at most this
 * many holes — a stored footprint longer than this is corrupt, not a big board.
 */
const val MAX_BOARD_POSITIONS = Long.SIZE_BITS
```

The fail-safe philosophy this fix extends, quoted so the new KDoc matches it:

```kotlin
// SnapshotMapper.kt:113-123
/**
 * Parses a stored blob, or `null` if it is corrupt, truncated or missing required
 * fields. `SerializationException` is an `IllegalArgumentException`, so this single
 * catch covers every malformed-input failure kotlinx.serialization raises.
 */
fun decode(blob: String): ResumeSnapshotDto? =
    try {
        json.decodeFromString<ResumeSnapshotDto>(blob)
    } catch (ignored: IllegalArgumentException) {
        null
    }
```

And the already-existing sibling validations at the same boundary, which the new
coordinate check joins (this is the "footprint is well-formed" block the advisory said
was incomplete):

```kotlin
// SnapshotMapper.kt:146-161
private fun ResumeSnapshotDto.toBoardSpecOrNull(): BoardSpec? {
    val positions = inPlay.mapNotNull { it.toPositionOrNull() }
    val row = targetRow
    val col = targetCol
    val target = if (row != null && col != null) Position(row = row, col = col) else null
    val footprintIsWellFormed =
        positions.size == inPlay.size &&
            positions.size in 1..MAX_BOARD_POSITIONS &&
            positions.distinct().size == positions.size
    val targetIsWellFormed = (row == null) == (col == null) && (target == null || target in positions)
    ...
}
```

**Apply:** RESEARCH.md §DEBT-04 supplies the patched `toPositionOrNull`. Note that
`positions.size == inPlay.size` at `:152` is what turns a rejected coordinate into a
whole-snapshot rejection — the fix needs no change at the `SnapshotMapper` level, which
is why the phase touches only `ResumeSnapshotDto.kt`. Confirm `targetRow`/`targetCol`
(read raw at `:148-150`, guarded only by `target in positions` at `:155`) in the test
rather than adding a second check.

**Test analog:** `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt`
— pure JVM, no runner annotation, Truth + MockK (`spyk`/`verify`), a class-level KDoc
enumerating the *contracts* proven ("Fail-safe deserialization (Security §V5 /
T-05-03 / T-05-04)"). Add the six WARN-02 cases under that third bullet and extend the
class KDoc to name WARN-02.

---

### `ui/game/GameViewModel.kt` — WARN-03 getter + `RewardOutcome` call sites

**Analog:** the file itself. Both patterns already exist inside it.

The clamp idiom, already applied twice — the fix makes the public getter read the
already-correct value rather than introducing a third `coerceIn`:

```kotlin
// GameViewModel.kt:125-152 — the three related values at HEAD
val challengeLevel: Int? get() = stampedChallengeLevel ?: routeChallengeLevel   // :125 UNCLAMPED (WARN-03)

private val clampedChallengeLevel: Int? =
    routeChallengeLevel?.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)   // :134-135

/**
 * The level this Challenge game is actually being played as ... Everything
 * level-keyed after construction reads THIS: the score multiplier, the ladder
 * advance on solve, and the level stamped into a re-saved snapshot.
 */
private val activeChallengeLevel: Int? get() = stampedChallengeLevel ?: clampedChallengeLevel   // :152
```

```kotlin
// GameViewModel.kt:391-394 — the stamp is clamped on assignment, so activeChallengeLevel
// is sound on both paths
restored.challenge
    ?.level
    ?.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)
    ?.let { stampedChallengeLevel = it }
```

The two `requestRewarded` call sites being converted (exact current text):

```kotlin
// GameViewModel.kt:673-676 (onUndo)
fun onUndo() {
    viewModelScope.launch {
        if (!adGateway.requestRewarded(AdReason.UNDO)) return@launch
        if (moveHistory.isEmpty()) return@launch
```

```kotlin
// GameViewModel.kt:737-742 (onHintRequested) — note the abort path MUST keep clearing solverThinking
hintJob =
    viewModelScope.launch {
        if (!adGateway.requestRewarded(AdReason.HINT)) {
            _uiState.update { it.copy(solverThinking = false) }
            return@launch
        }
```

**Apply:** RESEARCH.md §Pattern 2 / §Code Examples supply the exhaustive `when`
replacements verbatim. The KDoc above each function (`:661-672`, `:712-726`) currently
says "Slice-1 stub auto-grants" — update that phrase in both to describe the three-way
outcome and the ADS-03 fail-open rule, matching the density of the surrounding KDoc.

**Constant cleanup:** `MIN_CHALLENGE_LEVEL` at `:822` lives in the file's
`private companion object` alongside `SOLVED_PEG_COUNT`, `TIMER_TICK_MILLIS`,
`MILLIS_IN_SECOND` (`:811-823`). D-15 removes it from there; leave the other three.

---

### `ui/results/ResultsViewModel.kt` — WARN-03 defence in depth

**Analog:** `GameViewModel.kt:134-135` (the clamp) applied to this file's own arg-reading style.

The four raw `args.challengeLevel` reads to replace, in this file's current form:

```kotlin
// ResultsViewModel.kt:55, :65-72 — construction-time reads
private val args = savedStateHandle.toRoute<Results>()

private val _uiState =
    MutableStateFlow(
        ResultsUiState(
            ...
            headlineTier = tierFor(stars = args.stars, pegsRemaining = args.pegsRemaining,
                                   challengeLevel = args.challengeLevel),   // :68
            replayId = args.levelId,
            challengeLevel = args.challengeLevel,                            // :71
        ),
    )
```

```kotlin
// ResultsViewModel.kt:95-101 — the DB-key reads
val mode =
    if (args.challengeLevel != null) {              // :96
        ProgressPersistenceKeys.MODE_CHALLENGE
    } else {
        ProgressPersistenceKeys.MODE_CLASSIC
    }
val levelId = args.challengeLevel?.toString() ?: args.levelId    // :101
```

**Apply:** introduce one `private val challengeLevel: Int? = ChallengeLevelBounds.clamp(args.challengeLevel)`
directly under `args` at `:55` and route all four sites through it — this mirrors
`GameViewModel`'s "one clamped value, everything reads THIS" structure
(`GameViewModel.kt:146-152`), which is the invariant the advisory says was broken.

**Note the existing threat-model comment register in this file** (`:26-46`): the class
KDoc already carries a "Two guards keep that write honest" section citing D-08/T-05-09.
Add the WARN-03 clamp as a third bullet there rather than only commenting inline.

**Test analog:** `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`
(Robolectric, `@Config(sdk = [ROBOLECTRIC_SDK])`, `private const val ROBOLECTRIC_SDK = 34`
at file bottom `:415`). Assert on the recorded `levelId` and `uiState.challengeLevel`,
never on a private field.

---

### `di/PlatformModule.kt` (DI module) — third binding

**Analog:** the file itself and `di/DataModule.kt:37-62`.

```kotlin
// PlatformModule.kt:26-36 — current whole body
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway

    @Binds
    @Singleton
    abstract fun bindGamesService(impl: NoOpGamesService): GamesService
}
```

`DataModule` shows the same abstract-`@Binds` shell scaled to six bindings plus the
`companion object` escape hatch for instance `@Provides`:

```kotlin
// DataModule.kt:64-75 — the documented reason instance @Provides live on the companion
/**
 * Instance `@Provides` cannot live on an abstract `@Module`, so the single shared
 * `app_prefs` `DataStore<Preferences>` ... are provided here on the module's
 * companion object.
 */
companion object {
    @Provides
    @Singleton
    fun provideAppDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.appDataStore
```

**Apply:** add `bindForegroundActivityProvider` in the same three-line `@Binds @Singleton`
form. **Correct two stale KDoc claims** (`PlatformModule.kt:13-25`): "These two bindings
are the *only* Slice-2 swap points" (now three) and "the ad stub grants (`true`)" (now
returns `RewardOutcome.Granted`).

> **Pitfall 8 constraint (RESEARCH):** any *future* SDK-typed `@Provides` must NOT go
> in this file — `di/` is outside `..platform..` and Hilt's generated component would
> trip the PLAT-04 rule. Record that constraint in this file's KDoc now, while it is
> being edited, so Phase 9 does not have to rediscover it.

---

### `App.kt` (application) — `onCreate` + `attachTo`

**Analog:** the file itself (whole file, 15 lines) — currently `@HiltAndroidApp class App : Application()`
with a 7-line KDoc explaining the Hilt/KSP codegen. There is **no `onCreate` anywhere
in the repo** to copy; the closest lifecycle-callback precedent is `MainActivity`.

```kotlin
// App.kt:6-15 — the whole file at HEAD
/**
 * Application entry point and Hilt's `SingletonComponent` root.
 *
 * `@HiltAndroidApp` triggers Hilt's code generation: the processor (running under
 * KSP — no KAPT) emits the component graph so `@AndroidEntryPoint` activities and,
 * later, `@HiltViewModel` view models can be injected with zero further build
 * setup. ...
 */
@HiltAndroidApp
class App : Application()
```

**Apply:** RESEARCH.md §Pattern 1 supplies the target body (`@Inject lateinit var
activityProvider: ForegroundActivityProvider` + `onCreate` calling `attachTo(this)`).
Extend the existing KDoc with a paragraph on why field injection is safe after
`super.onCreate()` — matching the file's habit of explaining *why the mechanism works*,
not just what it does. `App` is also the class exempted by
`PlatformBoundaryTest`'s `haveNameNotMatching(".*\\.App")` clause; cross-reference that
here so the exemption is discoverable from both ends.

---

### `test/.../PlatformBoundaryTest.kt` + `PlatformBoundaryRules.kt` (ArchUnit gates)

**Analog:** `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt` (whole file, 31 lines)

```kotlin
// DomainPurityTest.kt:1-31 — the complete ArchUnit gate idiom in this repo
package com.app.pegsolitaire

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.runner.RunWith

/**
 * Named domain-purity boundary gate (QUAL-01).
 *
 * Belt-and-suspenders to the structural guarantee that `:domain` is a pure-JVM
 * module with no Android plugin on its classpath: this ArchUnit test reads
 * BYTECODE (so it is immune to the Kotlin-2.4 source-parse risk that affects
 * source-parsing tools) and fails if any class in the domain package depends on
 * `android.*` or `androidx.*`. Runs under `:app:testDebugUnitTest`, which is
 * wired into `check`.
 */
@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(packages = ["com.app.pegsolitaire.domain"])
class DomainPurityTest {
    @ArchTest
    val domainDoesNotDependOnAndroid: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("android..", "androidx..")
}
```

**Copy exactly:** root package `com.app.pegsolitaire` (not a `test`/`arch` subpackage),
`@RunWith(ArchUnitRunner::class)`, `@ArchTest val <descriptiveName>: ArchRule =`,
the one-clause-per-line fluent chain formatting (this is the ktlint-stable form),
and the KDoc that states *what structural guarantee this is belt-and-suspenders to*
and *which lifecycle task runs it*.

**Change only:** `@AnalyzeClasses(packages = ["com.app.pegsolitaire"], importOptions = [ImportOption.DoNotIncludeTests::class])`
and the three rule bodies from RESEARCH.md §Pattern 4.

**Do NOT touch `DomainPurityTest.kt`** — RESEARCH Pitfall 7: its `@AnalyzeClasses` scope
is `..domain` only, so a "nothing outside platform" rule evaluated there is vacuous.

**`PlatformBoundaryRules` shared object:** VALIDATION requires the rule bodies live in
one `internal object` referenced by both the `@ArchTest` fields and the negative test.
The nearest naming/placement precedent is `ResumeSnapshotFormat`
(`ResumeSnapshotDto.kt:65`) — an `internal object` of named contract values living in
the same file family as its consumers, with a KDoc warning that the values are a
contract, not incidental. Give `PlatformBoundaryRules` an equivalent warning:
*"these rule definitions are referenced by both the production gate and the negative
gate; two copies is how a gate quietly stops matching what it claims to check."*

---

### `test/.../PlatformBoundaryGateTest.kt` + `archfixture/` (negative gate) — NO ANALOG

No negative/meta test exists in this repo. Two partial precedents to lean on:

**Test-double placement:** `GameViewModelTest.kt:1287-1334` puts `private class`
doubles at the *bottom of the test file*, each with a KDoc explaining precisely which
assertion the double makes possible:

```kotlin
// GameViewModelTest.kt:1306-1321
/**
 * An [AdGateway] whose [requestRewarded] suspends on a shared gate until [release],
 * counting how many grants [started] (entered the gate) and how many [proceeded] past
 * it. A cancelled hint job's suspended `await` throws before [proceeded] increments, so
 * `proceeded == 1` after two rapid requests proves the first search was cancelled ...
 */
private class GatedAdGateway : AdGateway {
```

The `archfixture/` classes cannot follow that (they must be top-level, in their own
package, so `ClassFileImporter().importPackages(...)` can scope to them) — but each
fixture class must carry the same style of KDoc: *which rule it violates and why it
exists.* Otherwise a future reader deletes it as dead code.

**Take the test body verbatim from RESEARCH.md §Validation Architecture** (it is
already written), and honour assumption A5: after the fixture lands, `PlatformBoundaryTest`
staying green is itself the evidence that `DoNotIncludeTests` excludes `app/src/test`.

---

### `test/.../ManifestSurfaceTest.kt` (Robolectric)

**Analog:** `app/src/test/java/com/app/pegsolitaire/data/db/PegSolitaireDatabaseTest.kt`

```kotlin
// PegSolitaireDatabaseTest.kt:16-41
/**
 * JVM (Robolectric) coverage of the Phase-5 Room stack ...
 *
 * Robolectric supplies a real application `Context` so `Room.inMemoryDatabaseBuilder`
 * can open a throw-away SQLite database on the JVM; the project has no `androidTest`
 * source set, so this is where DAO behaviour is proven. ...
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PegSolitaireDatabaseTest {
    private lateinit var database: PegSolitaireDatabase

    @Before
    fun openInMemoryDatabase() {
        database =
            Room.inMemoryDatabaseBuilder(
                RuntimeEnvironment.getApplication(),
                PegSolitaireDatabase::class.java,
            ).build()
    }
```

**Repo-wide Robolectric convention (verified across all 11 Robolectric test files):**
- `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [<CONST>])`
- the SDK level is a **file-private top-level `const val` declared at the bottom of the
  file**, value `34`, with a file-unique name when the package already has one
  (`ROBOLECTRIC_SDK`, `HOME_ROBOLECTRIC_SDK`, `PROGRESS_ROBOLECTRIC_SDK`,
  `ADVANCE_ROBOLECTRIC_SDK`, `RESUME_ROBOLECTRIC_SDK`). `ManifestSurfaceTest` sits in
  the root `com.app.pegsolitaire` package, which has no existing const — use
  `ROBOLECTRIC_SDK`.
- Context is obtained via `RuntimeEnvironment.getApplication()` in this repo, **not**
  `ApplicationProvider.getApplicationContext()` (which RESEARCH's sketch uses). Prefer
  the repo idiom unless `ApplicationProvider` is already a dependency you can confirm.
- Truth (`assertThat`) for all assertions.

---

### `test/.../BuildConfigIdsTest.kt` and `test/.../ForegroundActivityProviderTest.kt` (JVM unit)

**Analog:** `app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt` (whole file, 31 lines)

```kotlin
// NoOpAdGatewayTest.kt:1-31 — the platform-layer unit-test shape
package com.app.pegsolitaire.platform.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the Slice-1 [NoOpAdGateway] stub auto-grants every rewarded reason (D-11) and
 * that its interstitial hook is a safe no-op. This is the contract 04-03 (Hint) reuses
 * unchanged; Slice 2 swaps the binding, not this behavior.
 */
class NoOpAdGatewayTest {
    private val gateway = NoOpAdGateway()

    @Test
    fun requestRewardedGrantsHint() =
        runTest {
            assertThat(gateway.requestRewarded(AdReason.HINT)).isTrue()
        }
    ...
}
```

Note the conventions: no runner annotation for pure-JVM tests; subject as a `private val`
field; `= runTest { ... }` expression-body form for suspend tests; one behaviour per
`@Test` with a sentence-shaped method name; class KDoc opening with "Proves …".

**For `ForegroundActivityProviderTest`:** MockK is already a `testImplementation`
(`app/build.gradle.kts:127`) and is used as `io.mockk.spyk` / `verify` in
`SnapshotMapperTest.kt`. `mockk<Activity>()` needs no Robolectric — keep this test
runner-free like `NoOpAdGatewayTest`. Turbine (`app.cash.turbine.test`) is used in
`PegSolitaireDatabaseTest.kt:4` for Flow assertions; use it for `awaitActivity()`.

**For `BuildConfigIdsTest`:** it reads `com.app.pegsolitaire.BuildConfig` (debug variant
only — Pitfall 9 means there is no `testReleaseUnitTest`). No analog exists for reading
`BuildConfig` in a test; keep the assertion to "the debug ad unit IDs carry Google's
test-publisher prefix" and put the prefix in a file-private `const val` at the bottom
(the repo's Robolectric-SDK-const idiom).

---

### `test/.../NoOpAdGatewayTest.kt`, `NoOpGamesServiceTest.kt` (modified)

**Analog:** each other. `NoOpGamesServiceTest.kt:7-17` carries the "deliberate contrast"
KDoc explaining why the ad stub grants while the games stub declines:

```kotlin
// NoOpGamesServiceTest.kt:7-19
/**
 * Proves the Slice-1 [NoOpGamesService] stub reports "not signed in" and that its
 * score-submission hook is a safe no-op.
 *
 * Note the deliberate contrast with `NoOpAdGatewayTest`: the ad stub *grants*
 * (`true`) so hint/undo stay free without an SDK, whereas this stub *declines*
 * (`false`) — Play Games really is unavailable in Slice 1 ...
 */
class NoOpGamesServiceTest {
    private val service = NoOpGamesService()
```

**Apply:** the contrast paragraph must survive the D-02 change — `true` becomes
`RewardOutcome.Granted` in both this KDoc and `NoOpAdGatewayTest`'s. The same phrase
appears in three more places that must be updated together: `NoOpAdGateway.kt:6-11`,
`NoOpGamesService.kt:5-15`, `PlatformModule.kt:22-24`.

---

### `platform/games/GamesService.kt` (modified, PLAT-03)

**Analog:** itself. Its KDoc (`:3-16`) already contains the exact scoping sentence that
must survive the extension — quote it in the plan so it is not dropped:

```kotlin
// GamesService.kt:13-16
 * The method set is deliberately minimal: only what a leaderboard-submitting puzzle
 * game needs. Cloud save (Saved Games) is explicitly out of scope here — it arrives
 * as a second data source behind `ProgressRepository`, not as a method on this seam.
```

The fire-and-forget precedent the new `unlockAchievement`/`incrementAchievement` follow:

```kotlin
// GamesService.kt:27-35
/**
 * Submits [score] to the leaderboard identified by [leaderboardId]. Fire-and-forget
 * by design — a failed submission must never block or fail a solved level. A no-op
 * in Slice 1.
 */
fun submitScore(
    leaderboardId: String,
    score: Long,
)
```

Note the multi-parameter wrapping style (one param per line, trailing comma) — ktlint
enforces it; `NoOpGamesService.kt:21-24` mirrors it in the override.

---

### `app/build.gradle.kts` (modified) — buildTypes, buildConfig, ID resolution

**Analog:** itself. This file's existing comment register is unusually heavy and is the
standard the new `buildTypes` block must meet — every non-obvious build decision carries
a paragraph explaining the failure it prevents:

```kotlin
// app/build.gradle.kts:36-56 — the comment density to match, and the ordering constraint
// Put the exported Room schema on the DEBUG variant's assets so Robolectric's
// MigrationTestHelper can resolve `<db-class>/1.json` JVM-side ...
//
// RESEARCH Pitfall 1 proposed the `test` source set — that does NOT work: AGP
// points Robolectric at the MAIN variant's merged assets ... `debug` is the
// narrowest source set Robolectric can actually see; release APKs never carry
// the schema JSON.
sourceSets.getByName("debug").assets.srcDir(roomSchemaDir)

// The schema JSON is produced by KSP and copied into `schemas/` by the Room plugin's
// `copyRoomSchemas` task, so the asset merge above must run after it ...
tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn("copyRoomSchemas")
}
```

**Placement constraint (assumption A2):** `buildTypes { }` goes *inside* the existing
`android { }` block, **above** the `sourceSets.getByName("debug")` line at `:48`.
Declaring `debug { }` configures the existing type rather than creating one, so this
wiring and the `mergeDebugAssets → copyRoomSchemas` dependency at `:54-56` keep their
meaning. Verify immediately with `--tests "*MigrationTest"`.

**Version-pinning discipline to preserve:** `:58-77` shows the repo's rule — no hand-pinned
literal in a module build file; versions come from `libs.versions.*` with a `because(...)`
explanation. Any new dependency (D-14 SDK artifacts) is a catalog entry first.

**Catalog analog:** `gradle/libs.versions.toml:1-4` states the contract
("No module may hand-pin a version literal"), and entries carry multi-line `#` comments
justifying non-obvious pins (`:9-12` Kotlin, `:20-24` Hilt, `:31-34` androidx.core).
The D-14 SDK entries and the `compose-group-mapping` visibility note (RESEARCH Finding 2)
belong in that register. Library lines follow
`name = { group = "...", name = "...", version.ref = "..." }` (`:130-138`).

---

### `build-logic/` — new test source set + `AdIdResolution`

**Analog (module build file):** `build-logic/build.gradle.kts` (whole file, 18 lines)

```kotlin
// build-logic/build.gradle.kts:1-18
plugins {
    `kotlin-dsl`
}

// The precompiled convention plugins (kotlin-jvm, android-app) apply these
// third-party plugins by id, so their implementations must be on the classpath.
// Versions come from the shared catalog — nothing is hand-pinned.
dependencies {
    implementation(libs.android.gradlePlugin)
    ...
    // NOTE: KSP + Hilt Gradle plugins are NOT on the convention classpath ...
}
```

The catalog is already wired into this included build (`build-logic/settings.gradle.kts:15-21`
reuses `../gradle/libs.versions.toml`), so `testImplementation(libs.junit)` and
`testImplementation(libs.truth)` resolve with no extra setup.

**Analog (pure-Kotlin logic inside build-logic):** `build-logic/src/main/kotlin/standards.gradle.kts:26-73`
— the `dpSpLiteralCheck` gate is this repo's only precedent for non-trivial logic in
build-logic. Note what it does well and what `AdIdResolution` should do better:

```kotlin
// standards.gradle.kts:26-42
// ---- QUAL-03: custom "no raw dp/sp literal" gate -----------------------------
// Captured at configuration time (the convention plugin has no compose/AGP types,
// so the gate is a pure file scan over the module's ui/ Kotlin sources).
val uiSourceRoots = ...
// Matches a numeric literal immediately converted to dp/sp (e.g. 12.dp, 8.sp,
// 4f.dp). Legitimate sizing flows through dimensionResource(...) ...
val dpSpRegex = Regex("""(?<![\w.])\d+(?:\.\d+)?[fF]?\.(?:dp|sp)\b""")
```

and the error-message register to copy for D-09's configuration-time failure:

```kotlin
// standards.gradle.kts:65-72 — actionable failure naming the offenders and the fix
if (offenders.isNotEmpty()) {
    throw GradleException(
        "Raw dp/sp literals are forbidden in UI code (QUAL-03). " +
            "Use dimensionResource(...) or a Dimens accessor:\n  " +
            offenders.joinToString("\n  "),
    )
}
```

> **The improvement `AdIdResolution` must make:** `dpSpLiteralCheck`'s logic is inlined
> in a `.gradle.kts` script and is therefore untestable — which is exactly why
> VALIDATION requires the ID resolution be a **plain `.kt` file** in
> `build-logic/src/main/kotlin/` (not a `.gradle.kts` script), taking `Map<String,String>`
> inputs and returning a result, so `build-logic/src/test/kotlin/AdIdResolutionTest.kt`
> can drive it. RESEARCH.md §Pattern 6 supplies the resolution logic and the exact
> error-message text; wrap it in the `GradleException` register above.

**Wiring `check`:** `standards.gradle.kts:75-77` is the precedent for adding to the
lifecycle — `tasks.named("check").configure { dependsOn(...) }`. The included-build
variant (`gradle.includedBuild("build-logic").task(":check")`) goes in the **root**
`build.gradle.kts`, or is documented as a separate command in `06-VERIFICATION.md`.

---

### `app/src/main/AndroidManifest.xml` (modified) — NO CLOSE ANALOG

The manifest is 21 lines with zero comments, zero permissions, and no `xmlns:tools`
(`AndroidManifest.xml:1-21`). Nothing in it models a commented merge directive. Take
the block verbatim from RESEARCH.md §Manifest Merge Finding 3 — it already carries the
D-07 / D-10 comment text including the corrected justification (D-17: production cold
start + CONS-02, **not** test-suite isolation, and no `MobileAdsInitProvider` exists).

The one convention to preserve: attributes are one-per-line, `android:name` first
(`:11-14`).

---

## Shared Patterns

### Seam pattern (interface + `@Inject constructor` impl + `@Binds @Singleton`)

**Source:** `platform/ads/AdGateway.kt` + `platform/ads/NoOpAdGateway.kt` + `di/PlatformModule.kt:29-35`
**Apply to:** `ForegroundActivityProvider` / `DefaultForegroundActivityProvider`, and any new platform seam

```kotlin
// 1. interface, own file
interface AdGateway { suspend fun requestRewarded(reason: AdReason): Boolean }

// 2. impl, sibling file — NoOpAdGateway.kt:12-18
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
        override suspend fun requestRewarded(reason: AdReason): Boolean = true
    }

// 3. binding — PlatformModule.kt:29-31
@Binds
@Singleton
abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway
```

The original of this pattern is `data/LevelRepository.kt:71-102` (`LevelRepository`
interface + `DefaultLevelRepository @Inject constructor`), bound at `DataModule.kt:40-42` —
and both `AdGateway.kt:8-9` and `NoOpAdGateway.kt:9-10` name it explicitly in their
KDoc. New seams should cite their model the same way.

---

### Untrusted-input handling: fail-safe at the boundary vs fail-fast on programmer error

**Sources:** `SnapshotMapper.kt:118-123` (fail-safe) · `ClassicLevelSource.kt:89` (fail-fast)
**Apply to:** WARN-02 (fail-safe), WARN-03 (fail-safe clamp), D-09 release-ID check (fail-fast)

```kotlin
// Fail-safe — stored/route data that could be corrupt or forged. Never throws.
// SnapshotMapper.kt:118-123
fun decode(blob: String): ResumeSnapshotDto? =
    try { json.decodeFromString<ResumeSnapshotDto>(blob) } catch (ignored: IllegalArgumentException) { null }

// Fail-safe clamp — an untrusted nav arg is bounded, not rejected.
// ChallengeLevelSource.kt:38
val clamped = levelNumber.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)

// Fail-fast — a programmer/caller error, with the offending value in the message.
// ClassicLevelSource.kt:89
requireNotNull(levelsById[levelId]) { "Unknown Classic level id: $levelId" }
```

D-09's configuration-time release-ID failure is the **fail-fast** branch (RESEARCH cites
this same `requireNotNull` idiom) and must name the missing property in its message, the
way `ClassicLevelSource` names the id and `dpSpLiteralCheck` names the offending lines.

---

### Scoped constants (QUAL-03 — no magic numbers)

**Sources:** `domain/generator/GeneratorConstants.kt` · `domain/scoring/ScoreParams.kt` ·
`data/resume/ResumeSnapshotDto.kt:65-101` (`ResumeSnapshotFormat`) ·
`data/ChallengeProgress.kt:19-26` (`ChallengeProgressPreferences`) ·
`ui/game/GameViewModel.kt:811-823` (file-local `private companion object`)
**Apply to:** `ChallengeLevelBounds`, the Google-test-ID constants in `build.gradle.kts`,
the `BuildConfigIdsTest` prefix constant, the Robolectric SDK constant in `ManifestSurfaceTest`

Three placements are in use, and the choice is by scope:

| Scope | Placement | Example |
|-------|-----------|---------|
| Shared across modules/layers | top-level `object` in its own file | `GeneratorConstants`, `ScoreParams` |
| One file family / one persistence concern | `internal object` at the top or bottom of the owning file | `ResumeSnapshotFormat`, `ChallengeProgressPreferences`, `ClassicLevelIds` |
| One class only | `private companion object` at the class bottom | `GameViewModel:811-823`, `ResultsViewModel:130-162` |
| One test file only | file-private top-level `const val` at file bottom | `private const val ROBOLECTRIC_SDK = 34` |

> **The documented exception:** ad unit IDs must NOT become a scoped constants object —
> they are `buildConfigField` / `resValue` only (RESEARCH Anti-Patterns, Pitfall 3). Say
> so in a comment where a reader would otherwise expect an `AdConstants` object.

---

### Test-double conventions

**Source:** `ui/game/GameViewModelTest.kt:1287-1334`
**Apply to:** the `FakeAdGateway` 3-state rewrite, `GrantingAdGateway`
(`GameViewModelChallengeAdvanceTest.kt:292`), `GrantingAds` (`GameViewModelResumeTest.kt:643`),
and the new `archfixture` classes

- `private class` / `private object`, declared **at the bottom of the test file**, below
  the test class — never in a shared `testFixtures` module (none exists).
- Each carries a KDoc naming the assertion it enables.
- Recorded state is exposed as `var x = ...` with `private set` (`FakeAdGateway.lastReason`).

```kotlin
// GameViewModelTest.kt:1287-1304 — the fake to convert
/**
 * A fake [AdGateway] that records the last requested [AdReason] and returns a
 * configurable [grant] (default `true`, matching the Slice-1 auto-grant stub) ...
 */
private class FakeAdGateway(
    private val grant: Boolean = true,
) : AdGateway {
    var lastReason: AdReason? = null
        private set

    override suspend fun requestRewarded(reason: AdReason): Boolean {
        lastReason = reason
        return grant
    }

    override fun showInterstitial() = Unit
}
```

RESEARCH's recommendation — `FakeAdGateway(outcome: RewardOutcome = RewardOutcome.Granted)` —
preserves this exact shape with one parameter type changed. The three call sites in this
file (`:386`, `:405`, `:647`, `:660`, `:1079`) change from `FakeAdGateway(grant = false)`
to `FakeAdGateway(outcome = RewardOutcome.Declined)` and gain a third,
`RewardOutcome.Unavailable`, which has **no prior art** in the repo.

---

### Test framework selection (which runner for which test)

**Sources:** `SnapshotMapperTest.kt` (none) · `NoOpAdGatewayTest.kt` (none, `runTest`) ·
`PegSolitaireDatabaseTest.kt` (Robolectric) · `DomainPurityTest.kt` (`ArchUnitRunner`)

| Test kind | Runner | Repo example |
|-----------|--------|--------------|
| Pure logic / codec / stub | *no runner*; `org.junit.Test` + Truth (+ `runTest` for suspend) | `SnapshotMapperTest`, `NoOpAdGatewayTest` |
| Needs a `Context` / merged manifest / Room | `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])` | `PegSolitaireDatabaseTest`, `ResultsViewModelTest` |
| Bytecode boundary gate | `@RunWith(ArchUnitRunner::class)` + `@AnalyzeClasses` | `DomainPurityTest` |
| Flow/StateFlow emissions | Turbine `.test { }` | `PegSolitaireDatabaseTest.kt:4` |
| Collaborator-call proof | MockK `spyk` / `verify` | `SnapshotMapperTest.kt:10-11` |

`ForegroundActivityProviderTest` belongs in row 1 (MockK-mocked `Activity`, no
Robolectric); `ManifestSurfaceTest` in row 2; `PlatformBoundaryTest` in row 3;
`PlatformBoundaryGateTest` in row 1 (it constructs `ClassFileImporter()` directly and
needs no runner).

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/proguard-rules.pro` | build config | n/a | No `.pro` file exists anywhere in the repo; `buildTypes` has never been declared. Use RESEARCH.md §Code Examples' complete file verbatim — it already carries the required per-rule "why" comments (D-11) and the strict-full-mode warning. |
| `local.properties.template` | config template | n/a | No template/example config file exists. `local.properties` currently holds only `sdk.dir`. Use the four-key `REPLACE_ME_*` layout from RESEARCH.md §Pattern 6. |
| `test/.../PlatformBoundaryGateTest.kt` | test (arch-negative) | n/a | No test in the repo asserts that another gate *fails*. Body supplied verbatim in RESEARCH.md §Validation Architecture. |
| `AndroidManifest.xml` merge directives | manifest | n/a | The manifest has no `xmlns:tools`, no permissions and no comments. Block supplied verbatim in RESEARCH.md §Manifest Merge Finding 3. |

*(For all four, RESEARCH.md is the pattern source and its code is already written to
this repo's comment-density standard. Do not invent an alternative shape.)*

---

## Cross-Cutting Corrections This Phase Must Make While Touching These Files

Documentation debt located during pattern extraction — each is a *false statement in a
file this phase already opens*, so fixing it costs nothing extra:

| File:line | Stale claim | Correct |
|-----------|-------------|---------|
| `AdGateway.kt:6-8` | "Slice 2 swaps only the Hilt binding … no gameplay or UI change" | False — every real SDK call needs an Activity; Phase 9 adds an opt-in dialog |
| `AdGateway.kt:13-16` | "`callers must treat a `false` return as not granted`" | Three-way `RewardOutcome`; `Unavailable` means *proceed* (ADS-03) |
| `AdReason.kt:5-7` | "`RETRY` is reserved for Slice-2 … interstitials" | D-03: retry is permanently free; no `RETRY` will ever be added |
| `NoOpAdGateway.kt:6-7` | "returns `true`" | returns `RewardOutcome.Granted` |
| `PlatformModule.kt:17-18` | "These two bindings are the *only* Slice-2 swap points" | Three bindings after PLAT-01 |
| `PlatformModule.kt:22-23` | "the ad stub grants (`true`)" | grants `RewardOutcome.Granted` |
| `NoOpAdGatewayTest.kt:8` / `NoOpGamesServiceTest.kt:10-12` | the `true`/`false` contrast prose | restate against the sealed outcome |
| `GameViewModel.kt:662`, `:715` | "(Slice-1 stub auto-grants)" | describe the three-way outcome + ADS-03 |
| `GameViewModel.kt:113-125` | `challengeLevel` KDoc describes the unclamped route arg as intentional | WARN-03: must be the clamped value |
| `CLAUDE.md` stack table | `play-services-ads 23.6.0`, `play-services-games-v2 ~21.x`, Hilt `2.57.1` | GMA Next-Gen `ads-mobile-sdk` 1.4.0, PGS v2 22.0.0, Hilt 2.60 (catalog is already 2.60) |

---

## Metadata

**Analog search scope:** `app/src/main/java/com/app/pegsolitaire/**` (platform, di, data,
data/resume, ui/game, ui/results, root), `domain/src/main/kotlin/**` (model, generator,
scoring), `app/src/test/java/com/app/pegsolitaire/**`, `build-logic/**`,
`gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`

**Files read in full:** 22 · **Files read in part (targeted ranges):** 4
(`GameViewModel.kt`, `GameViewModelTest.kt`, `DifficultyTiers.kt`, `PegSolitaireDatabaseTest.kt`)
**Greps run:** 8 (clamp sites, fail-fast sites, Robolectric config, ad-gateway fakes,
`@Singleton` holders, catalog entries)

**Pattern extraction date:** 2026-08-27
