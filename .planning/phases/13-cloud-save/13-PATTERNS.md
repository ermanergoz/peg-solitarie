# Phase 13: Cloud Save - Pattern Map

**Mapped:** 2026-09-15
**Files analyzed:** 31 new/modified (18 production, 10 test, 3 resource/docs)
**Analogs found:** 29 / 31

Every path below is relative to the repo root. App sources live under
`app/src/main/java/com/app/pegsolitaire/`, app tests under `app/src/test/java/com/app/pegsolitaire/`,
domain sources under `domain/src/main/kotlin/com/app/pegsolitaire/domain/`.

---

## File Classification

### New production files

| New File | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `domain/…/progress/CloudProgress.kt` | model (pure) | transform | `domain/…/achievement/ProgressAggregate.kt` | exact |
| `domain/…/progress/ProgressMerge.kt` | domain service (pure object) | transform | `domain/…/achievement/AchievementRules.kt` | exact |
| `domain/…/progress/ProgressMergeLimits.kt` | config (scoped constants) | — | `domain/…/achievement/AchievementThresholds.kt` | exact |
| `data/cloud/CloudProgressDto.kt` | model (`@Serializable` DTO) + wire-key constants | transform | `data/resume/ResumeSnapshotDto.kt` (+ `ResumeSnapshotFormat`) | exact |
| `data/cloud/CloudProgressCodec.kt` | mapper/codec | transform | `data/resume/SnapshotMapper.kt` | exact |
| `data/CloudSyncStore.kt` (or 2nd key on `SubmissionSyncStore`) | store (DataStore marker) | CRUD | `data/SubmissionSyncStore.kt` | exact |
| `data/CloudProfileStore.kt` (last-merged player id) | store (DataStore scalar) | CRUD | `data/PlayGamesOptInRepository.kt` | exact |
| `platform/games/save/CloudSaveStore.kt` | seam (interface) | request-response | `platform/games/GamesService.kt` | exact |
| `platform/games/save/PlayGamesCloudSaveStore.kt` | platform gateway impl | request-response | `platform/games/PlayGamesService.kt` | exact |
| `platform/games/save/NoOpCloudSaveStore.kt` | stub impl | request-response | `platform/games/NoOpGamesService.kt` | exact |
| `platform/games/save/CloudSaveConstants.kt` | config (scoped constants) | — | `platform/games/GamesConstants.kt` | exact |
| `platform/games/save/CoverImageSource.kt` | platform utility (Context → Bitmap) | transform | `platform/games/PlayGamesService.kt` (`@ApplicationContext` injection) | partial |
| `platform/games/save/CloudSaveDescriptionFormatter.kt` | platform utility (strings.xml) | transform | `platform/games/PlayGamesService.kt` + `res/values/strings.xml` | partial |
| `ui/home/ProfileSwapDialog.kt` | component (stateless dialog) | event-driven | `ui/home/PlayGamesPromptDialog.kt` | exact |
| `res/drawable/cloud_save_cover.xml` | resource (layer-list) | — | `res/mipmap-anydpi/ic_launcher.xml` | partial |

### Modified production files

| Modified File | Role | Change | Pattern Source (in-file) |
|---------------|------|--------|--------------------------|
| `data/ProgressRepository.kt` | repository | extract merge to `:domain`; add `importProgress` / `exportProgress` | its own `recordResult` lines 205–246 |
| `data/ProgressSubmitter.kt` | service (orchestrator) | profile gate → pull-once → existing pass → push | its own `submitAll` lines 169–194 |
| `data/ChallengeProgress.kt` | store | add `suspend fun mergeLadder`; `advancePast` delegates to it | its own `advancePast` lines 80–95 |
| `data/PlayGamesPreferences.kt` | config | 2 new `Preferences` keys + fail-open defaults | its own lines 13–54 |
| `data/db/LevelResultDao.kt` | DAO | `all()`, `upsertAll()`, (D-14) `deleteAll()` | its own lines 27–99 |
| `data/db/PlaySessionDao.kt` | DAO | `totalElapsedMs()`, (D-14) `deleteAll()` | its own lines 55–73 |
| `data/db/ResumeDao.kt` | DAO | (D-14) `clearAll()` | its own `clear(mode)` lines 37–38 |
| `di/PlatformModule.kt` | config (Hilt) | `@Binds @Singleton bindCloudSaveStore` | its own lines 112–118 |
| `ui/home/HomeUiState.kt` | model (UI state) | profile-swap flag | its own lines 51–65 |
| `ui/home/HomeViewModel.kt` | viewmodel | flag collector + 2 answer handlers | its own lines 107–116 / 215–256 |
| `ui/home/HomeScreen.kt` | component | host the dialog, never stacked | its own lines 165–175 |
| `res/values/strings.xml` | resource | dialog copy + `%1$d`-style description | its own lines 227–230 |

### New / extended test files

| Test File | Role | Closest Analog | Match Quality |
|-----------|------|----------------|---------------|
| `domain/src/test/…/progress/ProgressMergeTest.kt` | unit (pure JVM) | `domain/src/test/…/achievement/AchievementRulesTest.kt` | exact |
| `app/src/test/…/data/cloud/CloudProgressCodecTest.kt` | unit (JVM) | `app/src/test/…/data/resume/SnapshotMapperTest.kt` | exact |
| `app/src/test/…/data/ProgressImportTest.kt` | unit (Robolectric + in-memory Room) | `app/src/test/…/data/ProgressRepositoryTest.kt` | exact |
| `app/src/test/…/data/ProgressSubmitterCloudTest.kt` | unit (virtual time + fakes) | `app/src/test/…/data/ProgressSubmitterTest.kt` | exact |
| `app/src/test/…/platform/games/save/FakeCloudSaveStore.kt` | test double | `app/src/test/…/platform/games/FakeGamesService.kt` | exact |
| `app/src/test/…/platform/games/save/CloudSaveConstantsTest.kt` | unit (JVM) | `app/src/test/…/BuildConfigIdsTest.kt` | role-match |
| `app/src/test/…/platform/games/save/CloudSaveDescriptionFormatterTest.kt` | unit (Robolectric, needs `Context`) | `app/src/test/…/ui/home/HomeScreenTest.kt` (Robolectric `@Config`) | partial |
| `app/src/test/…/ui/home/HomeViewModelCloudTest.kt` | unit (ViewModel + Turbine) | `app/src/test/…/ui/home/HomeViewModelTest.kt` | exact |
| `app/src/test/…/ui/home/HomeScreenTest.kt` (new cases) | Compose UI test | its own existing cases | exact |
| `app/src/test/…/ui/home/ProfileSwapDialogTest.kt` | Compose UI test | `app/src/test/…/ui/home/PlayGamesPromptDialogTest.kt` | exact |

---

## Pattern Assignments

### `domain/…/progress/CloudProgress.kt` + `ProgressMerge.kt` + `ProgressMergeLimits.kt` (pure domain)

**Analogs:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRules.kt`,
`…/achievement/ProgressAggregate.kt`, `…/achievement/AchievementThresholds.kt`

**Module contract** (`domain/build.gradle.kts` lines 1–9) — this is the hard constraint:

```kotlin
plugins {
    // Pure Kotlin/JVM ONLY — no Android plugin. Structural domain-purity guarantee.
    id("kotlin-jvm")
    id("standards")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
```

There is **no `implementation` block at all**. `kotlinx-serialization` is not on `:domain`'s
classpath, so `@Serializable` cannot appear on `CloudProgress` — the DTO mirror in `data/cloud/`
carries it (RESEARCH Pitfall 9, and the precedent is stated verbatim in `ResumeSnapshotDto.kt`
lines 17–21).

**Pure-object shape to copy** (`AchievementRules.kt` lines 22–25, 49–55):

```kotlin
data class AchievementOutcome(
    val unlocked: Set<AchievementId>,
    val steps: Map<AchievementId, Int>,
)

object AchievementRules {
    fun evaluate(aggregate: ProgressAggregate): AchievementOutcome =
        AchievementOutcome(
            unlocked = unlockedAchievements(aggregate),
            steps = tierSteps(aggregate),
        )
```

`ProgressMerge` follows this exactly: a top-level `object`, one public entry point, private
helpers, no injection, no suspension, no annotations.

**Model shape to copy** (`ProgressAggregate.kt` lines 46–52, in KDoc) — the two conventions the
planner must carry over verbatim:

- *"Every property is DEFAULTED, so a brand-new player is exactly `ProgressAggregate()` and a test
  fixture names only the field under test."* → `CloudProgress()` must be the empty/identity value,
  which also gives the `merge(a, EMPTY) == a` identity test a free fixture.
- *"Nullability is not decoration"* — `null` means omission, never zero. Apply this to
  `bestTimeMs`'s identity: it is `Long.MAX_VALUE` (a named constant in `ProgressMergeLimits`),
  never `0`, because `0` is the absorbing element of a `min` merge and poisons every merge it
  touches.

**The merge to extract, verbatim** (`data/ProgressRepository.kt` lines 237–246) — this already
*is* the merge; the phase names it rather than invents it:

```kotlin
levelResultDao.upsert(
    LevelResultEntity(
        mode = mode,
        levelId = levelId,
        stars = maxOf(result.stars, previous?.stars ?: ProgressConstants.NO_STARS),
        bestTimeMs = minOf(timeMs, previous?.bestTimeMs ?: Long.MAX_VALUE),
        bestScore = maxOf(result.score, previous?.bestScore ?: ProgressConstants.NO_SCORE),
        completedAt = if (newBests.any) nowMs else previous?.completedAt ?: nowMs,
    ),
)
```

The ladder half is `data/ChallengeProgress.kt` lines 87–92:

```kotlin
dataStore.edit { preferences ->
    val current =
        preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
            ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
    preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY] =
        maxOf(current, solvedLevel + 1)
}
```

**Constants pattern** (`ChallengeProgress.kt` lines 19–26 — the smallest example of the project's
scoped-constants rule):

```kotlin
/** Scoped constants for the Challenge-progress Preferences keys (QUAL-03 — no inline literals). */
internal object ChallengeProgressPreferences {
    /** The int key holding the player's current Challenge level (1-based). */
    val CURRENT_LEVEL_KEY = intPreferencesKey("challenge_current_level")

    /** Fail-open default: a fresh install — or a corrupt/IO read — starts at level 1. */
    const val DEFAULT_CURRENT_LEVEL = 1
}
```

---

### `data/cloud/CloudProgressDto.kt` (model, transform)

**Analog:** `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt`

**DTO pattern** (lines 98–115) — nullable-with-default for every field added after v1, so an older
build decodes a newer payload:

```kotlin
@Serializable
data class ResumeSnapshotDto(
    val mode: String,
    val levelId: String,
    val challengeLevel: Int? = null,
    val generatorVersion: Int? = null,
    val inPlay: List<IntArray>,
    ...
    val minimumPegs: Int? = null,
    val freeHintsRemaining: Int? = null,
)
```

The KDoc at lines 51–53 states the rule the cloud DTO inherits word for word:
*"Nullable-with-a-default is also what keeps today's saved games decodable at all: the writer's
`ignoreUnknownKeys` covers the forward direction, this covers the backward one."*

**Wire-constant pattern** (lines 117–127) — the mode tokens `"classic"`/`"challenge"` are a wire
contract and get their own object in `data/cloud/`, decoupled from
`ProgressPersistenceKeys.MODE_CLASSIC` / `MODE_CHALLENGE` (`data/ProgressPersistenceKeys.kt`
lines 64–67), which are *DB column values*:

```kotlin
/**
 * Scoped constants for the snapshot's on-disk tuple encoding (QUAL-03 — no inline
 * magic numbers). …
 *
 * **These indices are an on-disk contract.** Changing one changes how every stored
 * blob is read; a real format change needs a new field rather than a re-shuffle.
 */
internal object ResumeSnapshotFormat {
    const val POSITION_TUPLE_SIZE = 2
```

---

### `data/cloud/CloudProgressCodec.kt` (mapper, transform)

**Analog:** `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt`

**Json instance** (line 90–92):

```kotlin
object SnapshotMapper {
    /** Unknown keys are ignored so a future field cannot orphan today's saved games. */
    private val json = Json { ignoreUnknownKeys = true }
```

**Encode / never-throwing decode** (lines 236–248) — copy this shape exactly, then add the
`schemaVersion` gate (D-07) as a second `null` branch after a successful parse:

```kotlin
/** Serializes [dto] to the string stored in the snapshot row's blob column. */
fun encode(dto: ResumeSnapshotDto): String = json.encodeToString(dto)

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

`decode` returning `null` on an unreadable payload **is** CLOUD-05's fail-open at this layer; the
codec must never throw and never log.

The trim loop (`encode → measure → ProgressMerge.dropLowestChallenge(n) → repeat`) is new — no
analog exists. Keep the *policy* (which rows to drop) in `:domain` and the *measurement* (byte
length) here, per the RESEARCH responsibility map.

---

### `data/CloudSyncStore.kt` / new `PlayGamesPreferences` keys (store, CRUD)

**Analogs:** `data/SubmissionSyncStore.kt`, `data/PlayGamesPreferences.kt`,
`data/PlayGamesOptInRepository.kt`

**Store pattern** (`SubmissionSyncStore.kt` lines 51–88) — copy the class shape, the `open`
modifiers (so `ProgressSubmitterCloudTest` can subclass over a `MutableStateFlow` instead of a
real DataStore) and the `runCatching`/cancellation-rethrow write:

```kotlin
@Singleton
open class SubmissionSyncStore
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        open val pending: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map {
                    it[PlayGamesPreferences.SUBMISSIONS_PENDING_KEY]
                        ?: PlayGamesPreferences.DEFAULT_SUBMISSIONS_PENDING
                }

        open suspend fun setPending(pending: Boolean) {
            runCatching {
                dataStore.edit { it[PlayGamesPreferences.SUBMISSIONS_PENDING_KEY] = pending }
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
```

Its KDoc (lines 23–34) also decides the CONTEXT discretion point *"where the cloud-push-pending
marker lives"*: the **key** belongs in `PlayGamesPreferences` regardless ("every `play_games_*`
name is still declared in one scoped constants object"), while a **separate accessor class** is
justified only when the new flag has no atomicity relationship with the existing pair. The cloud
push-pending flag has none with `SUBMISSIONS_PENDING`, so either a second key on
`SubmissionSyncStore` (same atomicity family: both are "an outstanding Play Games write") or a
sibling store is defensible — the planner must state which and why, reusing this exact argument.

**Key + fail-open-default pattern** (`PlayGamesPreferences.kt` lines 40–54) — every new key needs
a `*_KEY` and a `DEFAULT_*` whose direction is argued in KDoc:

```kotlin
val SUBMISSIONS_PENDING_KEY = booleanPreferencesKey("play_games_submissions_pending")

/**
 * Fail-open default: a missing key (fresh install) or a corrupt/IO read means
 * "nothing pending".
 * …
 */
const val DEFAULT_SUBMISSIONS_PENDING = false
```

For D-12's last-merged player id use `stringPreferencesKey("play_games_merged_player_id")` with
`DEFAULT_MERGED_PLAYER_ID = null` — and note the fail-open direction argument that must be made:
an unreadable stored id must read as "no stored id", which per D-12 means **merge silently**, never
"show the destructive dialog".

**Multi-key atomic write** (`PlayGamesOptInRepository.kt` lines 93–132):

```kotlin
open suspend fun setOptedIn() {
    editFailOpen {
        it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] = true
        it[PlayGamesPreferences.OPTED_IN_KEY] = true
    }
}

private suspend fun editFailOpen(transform: suspend (MutablePreferences) -> Unit) {
    runCatching { dataStore.edit(transform) }
        .onFailure { if (it is CancellationException) throw it }
}
```

---

### `platform/games/save/CloudSaveStore.kt` (seam interface, request-response)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt`

**Seam contract to mirror** (lines 52–63) — including the explicit "this interface names no
Activity type and no Play Games SDK type in any signature":

```kotlin
/**
 * This interface names no Activity type and no Play Games SDK type in any signature, so no
 * ViewModel can obtain one through it. …
 *
 * The method set is deliberately minimal: only what a leaderboard-submitting puzzle
 * game needs. Cloud save (Saved Games) is explicitly out of scope here — it arrives
 * as a second data source behind `ProgressRepository`, not as a method on this seam.
 */
interface GamesService {
```

That last sentence is binding: **do not add `load`/`save` to `GamesService`.** Add a sibling
interface in `platform/games/save/` and delete/adjust nothing in this KDoc except, optionally, a
cross-reference to the new seam.

**Failure-as-return-value signature** (lines 206–214) — the exact `Boolean` contract
`CloudSaveStore.save` copies:

```kotlin
/**
 * @return `true` when the round trip completed without throwing; `false` for every
 *   "nothing was submitted" cause — signed out, backgrounded, offline, SDK failure.
 *   `false` is not an error: the caller records the pending-submissions flag (D-10)
 *   and resubmits later. This method does not throw for a submission failure.
 */
suspend fun submitScore(
    leaderboardId: String,
    score: Long,
): Boolean
```

`load()` has no `Boolean` precedent; use `ByteArray?` with the same doc discipline — `null` means
"stay local", never "empty progress".

---

### `platform/games/save/PlayGamesCloudSaveStore.kt` (platform gateway, request-response)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`

**Constructor / injection** (lines 126–136):

```kotlin
class PlayGamesService
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
        private val activityProvider: ForegroundActivityProvider,
        private val optInRepository: PlayGamesOptInRepository,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : GamesService {
        private val _session = MutableStateFlow<GamesSignInState>(GamesSignInState.Unknown)
```

Note `@param:ApplicationContext` (not the bare annotation) — that spelling is required by the
project's Kotlin version and is used consistently.

**Bounded Activity await** (lines 451–452) — the only place a timeout is applied, and the shape
the snapshot calls reuse:

```kotlin
private suspend fun awaitForegroundActivity(): Activity? =
    withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }
```

**The `submitting` helper — the whole session-gate + never-throw discipline** (lines 526–536):

```kotlin
private suspend fun <T> submitting(block: suspend (Activity) -> T): Boolean {
    // The session check gates the Activity read, not just the submission: a
    // signed-out or still-Unknown player never reaches the SDK at all.
    val activity =
        if (_session.value is GamesSignInState.SignedIn) activityProvider.current else null

    return activity != null &&
        runCatching { block(activity) }
            .onFailure { if (it is CancellationException) throw it }
            .isSuccess
}
```

`CloudSaveStore` has no `session` field of its own, so the cloud analogue of the gate is either an
injected `GamesService.session` read or the caller's gate in `ProgressSubmitter`. **Important
deviation the planner must decide explicitly:** `submitting` uses `activityProvider.current`
(no wait) because a submission is fire-and-forget; the cloud *pull* fires once per process right
after `SignedIn` and genuinely needs an Activity, so it should use `awaitForegroundActivity()`
(the `launchStockUi` branch, lines 549–553) instead. State which call uses which, and why, in the
plan — the KDoc at lines 506–516 spells out the cost of getting this backwards.

**SDK call shape** (lines 454–465):

```kotlin
override suspend fun submitScore(
    leaderboardId: String,
    score: Long,
): Boolean =
    submitting { activity ->
        PlayGames.getLeaderboardsClient(activity).submitScoreImmediate(leaderboardId, score).await()
    }
```

`kotlinx.coroutines.tasks.await` is already imported (line 24); `PlayGames.getSnapshotsClient(activity)`
slots into exactly this position.

**Best-effort wrapper for a void path** (lines 549–564) — the `try/finally`-free `runCatching`
idiom the `commitAndClose`/`discardAndClose` pairing must sit inside:

```kotlin
private suspend fun launchStockUi(
    requestCode: Int,
    intentFor: suspend (Activity) -> Intent,
) {
    val activity = awaitForegroundActivity() ?: return
    runCatching {
        val intent = intentFor(activity)
        activity.startActivityForResult(intent, requestCode)
    }.onFailure { if (it is CancellationException) throw it }
}
```

**No logging.** There is not a single `Log.`/`println`/`Timber` call anywhere in
`platform/games/` — that absence is a grep gate (T-11-04) and applies to `platform/games/save/`
and `data/cloud/` from the first commit.

---

### `platform/games/save/NoOpCloudSaveStore.kt` (stub)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt`
(lines 34–39, 64–71)

```kotlin
class NoOpGamesService
    @Inject
    constructor() : GamesService {
        private val signedOutSession = MutableStateFlow<GamesSignInState>(GamesSignInState.SignedOut)

        override val session: StateFlow<GamesSignInState> = signedOutSession.asStateFlow()

        /** `false`: nothing was submitted, and a caller's retry bookkeeping should say so. */
        override suspend fun submitScore(
            leaderboardId: String,
            score: Long,
        ): Boolean = false
```

Copy the one-line KDoc-per-override convention: each stub method states *what the caller should
conclude*, not "does nothing". `NoOpCloudSaveStore.load()` returns `null`; `save()` returns
`false`.

---

### `platform/games/save/CloudSaveConstants.kt` (config)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt` (lines 25–37)

```kotlin
object GamesConstants {
    /** … */
    const val ACTIVITY_WAIT_MS = 10_000L
```

New members needed: snapshot name (`"progress"`), max-data-size fallback floor (3 MB), trim margin,
`MAX_CONFLICT_RETRIES` (10), `COVER_PX` (512), description max length. detekt's `MagicNumber` is
active over `src/main` with only `-1/0/1/2` exempt (see `config/detekt/detekt.yml`), so **every one
of those numbers must be a named `const val` here** — `SnapshotsClient.RESOLUTION_POLICY_MANUAL` is
already a named SDK constant and needs no wrapper.

---

### `di/PlatformModule.kt` (config — modified)

**Analog:** its own lines 112–118:

```kotlin
    @Binds
    @Singleton
    abstract fun bindGamesService(impl: PlayGamesService): GamesService

    @Binds
    @Singleton
    abstract fun bindForegroundActivityProvider(impl: DefaultForegroundActivityProvider): ForegroundActivityProvider
}
```

Add `bindCloudSaveStore(impl: PlayGamesCloudSaveStore): CloudSaveStore` in the same shape. **The
binding type must be the seam**, never a gms type: `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks`
is bytecode-based and would catch a `Provider<SnapshotsClient>` field in the generated
`DaggerApp_HiltComponents_SingletonC`.

---

### `data/ProgressRepository.kt` (repository — modified)

**Analog:** its own `recordResult` (lines 205–220) — the transaction-boundary failsafe:

```kotlin
override suspend fun recordResult(...): NewBests =
    // Best-effort (T-05-13), guarded at the TRANSACTION boundary deliberately: a
    // throwing `find` must abort the whole merge, because degrading it to "no
    // previous row" inside the transaction would let the upsert overwrite better
    // stored bests. Failure drops the write and reports "nothing improved".
    storageFailSafe(fallback = NewBests(stars = false, time = false, score = false)) {
        database.withTransaction {
            recordResultInTransaction(mode, levelId, result, telemetry, nowMs)
        }
    }
```

`importProgress(remote: CloudProgress)` copies this exactly with `fallback = Unit`. The comment's
reasoning transfers verbatim: a throwing `all()` must abort the whole import rather than degrade to
"no local rows", which would let stale cloud rows clobber better local bests.

**`storageFailSafe` itself** (`data/StorageFailSafe.kt` lines 22–31) — `internal suspend fun`, so
it is available to anything in `data/`, including `data/cloud/`:

```kotlin
internal suspend fun <T> storageFailSafe(
    fallback: T,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ignored: Exception) {
        fallback
    }
```

**Interface-first convention** — `ProgressRepository` is an `interface` (line 34) implemented by
`DefaultProgressRepository` (line 197), each method KDoc'd on the interface only. New members go on
the interface with their full contract; `ProgressSubmitterTest`'s `FakeProgressRepository`
(lines 584–606) must then be extended, so add the narrowest possible surface.

**Return type is the CLOUD-04 guarantee.** `importProgress` returns `Unit` (never `NewBests`) —
`NewBests` is constructed in exactly one place (`recordResultInTransaction`, lines 231–236) and
consumed in exactly one place (`ResultsViewModel`), so an import path that returns no `NewBests`
structurally cannot fire "New best!" or the celebration.

**DAO additions** — the existing DAO style (`data/db/LevelResultDao.kt` lines 27–38, 98–99):

```kotlin
@Dao
interface LevelResultDao {
    @Query("SELECT * FROM $TABLE_LEVEL_RESULT WHERE $COLUMN_MODE = :mode AND $COLUMN_LEVEL_ID = :levelId")
    suspend fun find(
        mode: String,
        levelId: String,
    ): LevelResultEntity?

    @Query("SELECT * FROM $TABLE_LEVEL_RESULT WHERE $COLUMN_MODE = :mode")
    fun observe(mode: String): Flow<List<LevelResultEntity>>
    …
    @Upsert
    suspend fun upsert(entity: LevelResultEntity)
}
```

Table and column names are **always** interpolated from `ProgressPersistenceKeys` — never written
as SQL string literals. `COALESCE(...)` is the established way to make an aggregate non-null
(`LevelResultDao.kt` line 66: `(SELECT COALESCE(SUM($COLUMN_STARS), 0) FROM $TABLE_LEVEL_RESULT)`),
which is the exact shape `PlaySessionDao.totalElapsedMs()` needs for D-10.

For D-14's wipe, the delete precedent is `data/db/ResumeDao.kt` lines 37–38:

```kotlin
@Query("DELETE FROM $TABLE_RESUME_SNAPSHOT WHERE $COLUMN_MODE = :mode")
suspend fun clear(mode: String)
```

All of these are additive `@Query`/`@Upsert` members: **no entity change, no `DB_VERSION` bump, no
`Migration`, no `MigrationTest` edit.** If the planner finds a schema change is needed, the full
Room bump ritual applies (`data/db/Migrations.kt`, `di/DatabaseModule.kt`, exported schema JSON,
`app/src/test/…/data/db/MigrationTest.kt`).

---

### `data/ProgressSubmitter.kt` (orchestrator — modified)

**Analog:** its own `submitAll` (lines 169–194). This is the single most load-bearing excerpt in
the phase — the cloud pull, the profile gate and the push all go *inside* this `withLock` body:

```kotlin
private suspend fun submitAll(onlyIfPending: Boolean) =
    passMutex.withLock {
        // `Unknown` takes this branch too (RESEARCH Pitfall 8). It means "the first
        // check has not answered", and reading it as "offline" — or as "probably
        // fine" — is the mistake this line exists to prevent: the subsequent
        // SignedIn emission triggers the backfill, so nothing is lost by waiting.
        if (gamesService.session.value !is GamesSignInState.SignedIn) {
            syncStore.setPending(true)
            return@withLock
        }
        // Ordered AFTER the session gate on purpose: a signed-out trigger must
        // still set the flag, whatever its reason for running.
        if (onlyIfPending && !syncStore.pending.first()) return@withLock
        val aggregate = progressRepository.aggregate()
        val bests = progressRepository.classicBests()
        val outcome = AchievementRules.evaluate(aggregate)

        val results =
            submitLeaderboards(aggregate, bests) +
                submitUnlocks(outcome) +
                submitSteps(outcome)

        recordPassOutcome(issued = results.isNotEmpty(), allOk = results.all { it })
    }
```

Two structural notes the plan must respect:

1. **`onlyIfPending` must not gate the cloud pull.** The `if (onlyIfPending && !pending)` early
   return at line 181 would skip the once-per-process pull for a signed-in player with a clean
   flag — exactly the new-device case D-02 exists for. The profile gate and the pull therefore go
   **between** the session gate (line 178) and the pending check (line 181).
2. **The session collector stays singular** (lines 128–135):

```kotlin
open fun start() {
    if (!started.compareAndSet(false, true)) return
    scope.launch {
        gamesService.session.collect { session ->
            if (session is GamesSignInState.SignedIn) submitAll(onlyIfPending = true)
        }
    }
}
```

The once-per-process pull guard copies `PlayGamesService.bootstrapped` (lines 154, 129):
`private val pulled = AtomicBoolean(false)` … `if (!pulled.compareAndSet(false, true)) return`.

**Non-suspend fire-and-forget entry point** (lines 152–154) — `onResultRecorded` must stay
non-`suspend`; the cloud push joins the launched pass, never the caller's `viewModelScope`:

```kotlin
open fun onResultRecorded() {
    scope.launch { submitAll(onlyIfPending = false) }
}
```

**Per-call failure classification** (lines 277–280) — every `CloudSaveStore` call goes through this:

```kotlin
private suspend fun attempt(block: suspend () -> Boolean): Boolean =
    runCatching { block() }
        .onFailure { if (it is CancellationException) throw it }
        .getOrDefault(false)
```

**Flag bookkeeping** (lines 204–213) — the cloud marker copies the same three-case `when`:

```kotlin
private suspend fun recordPassOutcome(
    issued: Boolean,
    allOk: Boolean,
) {
    when {
        !allOk -> syncStore.setPending(true)
        issued -> syncStore.setPending(false)
        else -> Unit
    }
}
```

---

### `data/ChallengeProgress.kt` (store — modified)

**Analog:** its own `advancePast` (lines 80–95, quoted above). Add the awaitable sibling
`suspend fun mergeLadder(level: Int)` holding the `storageFailSafe { dataStore.edit { … maxOf … } }`
body, then reduce `advancePast` to `scope.launch { mergeLadder(solvedLevel + 1) }` so there is one
max-merge rather than two. The KDoc at lines 76–78 already states why the merge must be computed
*inside* the `edit` transaction — carry that sentence over.

`reset()` (lines 101–110) is the existing "back to level 1" write and is the D-14 replace path's
ladder half, but it is fire-and-forget; the replace path needs an awaitable version for the same
reason `mergeLadder` exists.

---

### `ui/home/ProfileSwapDialog.kt` (component, event-driven)

**Analog:** `app/src/main/java/com/app/pegsolitaire/ui/home/PlayGamesPromptDialog.kt`

**Test tag + dialog shell** (lines 33, 87–113):

```kotlin
const val PLAY_GAMES_PROMPT_DIALOG_TEST_TAG = "play_games_prompt_dialog"

@Composable
fun PlayGamesPromptDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val surfaceShape = MaterialTheme.shapes.extraLarge
        Surface(
            shape = surfaceShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_lg))
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .clayShadow(dimensionResource(R.dimen.elevation_dialog), surfaceShape)
                    .testTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG),
        ) {
            PlayGamesPromptContent(onAccept = onAccept, onDecline = onDecline)
        }
    }
}
```

`onDismissRequest = onDecline` is precisely D-13's "back / tap-outside counts as *Keep this
device's progress*" — wire `onDismissRequest = onKeepLocal`.

**Clay button pair** (lines 146–194) — the gradient primary + `TextButton` secondary, both with
sound cues and `dimensionResource` spacing:

```kotlin
Button(
    onClick = withClickSound(SoundCue.ButtonForward, onAccept),
    shape = actionShape,
    colors =
        ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    modifier =
        Modifier
            .claySquish()
            .background(brush = ctaGradientBrush(), shape = actionShape)
            .fillMaxWidth(),
) {
    Text(
        text = stringResource(R.string.play_games_prompt_accept),
        style = MaterialTheme.typography.labelLarge,
    )
}
TextButton(
    onClick = withClickSound(SoundCue.ButtonDismiss, onDecline),
    ...
```

**Which button gets the gradient is a decision, not a copy.** In the opt-in prompt the gradient CTA
is "Sign in". Here the safe, non-destructive default is "Keep this device's progress" (D-13), so the
gradient belongs on *that* button and the destructive "Use [account]'s cloud progress" should be the
`TextButton` — with `SoundCue.ButtonDismiss` on the destructive one.

**Strings** (`res/values/strings.xml` lines 227–230) — the naming pattern the new copy follows:

```xml
<string name="play_games_prompt_title">Sign in to Play Games?</string>
<string name="play_games_prompt_body">Compete on leaderboards, …</string>
<string name="play_games_prompt_accept">Sign in</string>
<string name="play_games_prompt_decline">Not now</string>
```

No new dimens are needed: `spacing_lg` (24dp), `spacing_md`, `spacing_sm`, `content_max_width`
(480dp) and `elevation_dialog` (12dp) already exist in `res/values/dimens.xml` (lines 15, 31, 97).

---

### `ui/home/HomeUiState.kt` + `HomeViewModel.kt` + `HomeScreen.kt` (modified)

**State flag** (`HomeUiState.kt` lines 51–58) — add a peer field with a defaulted value and a KDoc
paragraph stating the sequencing rule:

```kotlin
@Immutable
data class HomeUiState(
    val onboardingVisible: Boolean = false,
    val challengeCurrentLevel: Int = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
    val classicResumeLevelId: String? = null,
    val challengeResumeLevel: Int? = null,
    val playGamesPromptVisible: Boolean = false,
    val playGamesSignInErrorTick: Int = 0,
)
```

The `playGamesPromptVisible` KDoc (lines 33–38) is the template for the new flag's doc — *"A peer
of `onboardingVisible` rather than a mode of it: the two first-run surfaces are sequenced, never
stacked."* D-15 requires the profile-swap flag be a third peer, never stacked with either.
Because D-13's dialog names the account (`"Use [account]'s cloud progress"`), the flag most likely
needs to be nullable-carrying (`val profileSwapAccountName: String? = null`) rather than a
`Boolean`, following `classicResumeLevelId`'s "the screen carries the id, the host never guesses"
convention (lines 21–25).

**Flag collector** (`HomeViewModel.kt` lines 107–116) — the derived-visibility idiom:

```kotlin
viewModelScope.launch {
    combine(
        onboardingRepository.onboardingSeen.distinctUntilChanged(),
        optInRepository.promptAnswered.distinctUntilChanged(),
    ) { seen, answered -> seen && !answered }
        .distinctUntilChanged()
        .collect { shouldShow ->
            _uiState.update { it.copy(playGamesPromptVisible = shouldShow) }
        }
}
```

The profile-swap flag combines `gamesService.session` with the stored player id and (for the
never-stacked rule) `onboardingSeen` / `promptAnswered`.

**Answer handlers** (lines 215–227, 253–256) — clear the flag *synchronously first*, then launch
the work:

```kotlin
fun onAcceptPlayGamesPrompt() {
    _uiState.update { it.copy(playGamesPromptVisible = false) }
    viewModelScope.launch {
        optInRepository.setOptedIn()
        runCatching { gamesService.signIn() }
            .onFailure { failure ->
                if (failure is CancellationException) throw failure
                _uiState.update { state ->
                    state.copy(playGamesSignInErrorTick = state.playGamesSignInErrorTick + 1)
                }
            }
    }
}

fun onDeclinePlayGamesPrompt() {
    _uiState.update { it.copy(playGamesPromptVisible = false) }
    viewModelScope.launch { optInRepository.setNotNow() }
}
```

**Caution on `viewModelScope`:** the D-14 replace path (wipe + import) must **not** run on
`viewModelScope` — `ProgressSubmitter`'s KDoc (lines 146–150) names awaiting persistence work from a
screen scope as an anti-pattern, because back-navigation mid-write cancels it. The handler should
hand off to the application-scoped `ProgressSubmitter`/repository path and only flip UI state here.

**Hosting** (`HomeScreen.kt` lines 165–175):

```kotlin
if (state.onboardingVisible) {
    OnboardingOverlay(onDismiss = onDismissOnboarding)
}
…
if (state.playGamesPromptVisible) {
    PlayGamesPromptDialog(
```

The new dialog is a third sibling `if` in the same block — and because the ViewModel guarantees
mutual exclusion, `HomeScreenTest` must assert the two never render together.

---

### Test files

**Pure-domain test** — analog `domain/src/test/kotlin/…/achievement/AchievementRulesTest.kt`
(lines 1–4, 21, 37–45):

```kotlin
package com.app.pegsolitaire.domain.achievement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AchievementRulesTest {
    …
    /** Everything earned: the fixture that must reach all 20 roster entries. */
    private val maximalAggregate =
        ProgressAggregate(
            highestChallengeLevel = 500,
            …
```

Its KDoc (lines 14–19) carries the rule the merge tests must follow: *"Expectations about POLICY
values are written as their named constants, never as bare literals … Bare literals used as
arbitrary fixture values are fine."* So `Long.MAX_VALUE`'s identity role is asserted through
`ProgressMergeLimits.NO_TIME_RECORDED`, while a seeded random ladder of `137` is fine inline.
No JUnit runner, no Robolectric, no mocks — `:domain` has only `junit` + `truth`.

**Recording fake** — analog `app/src/test/…/platform/games/FakeGamesService.kt`
(lines 51–57, 81–101, 187–193, 221–228):

```kotlin
class FakeGamesService(
    session: GamesSignInState = GamesSignInState.SignedOut,
) : GamesService {
    private val sessionState = MutableStateFlow(session)

    override val session: StateFlow<GamesSignInState> = sessionState.asStateFlow()

    var submissionResult: Boolean = true

    var submittedScores: List<ScoreSubmission> = emptyList()
        private set

    override suspend fun submitScore(
        leaderboardId: String,
        score: Long,
    ): Boolean {
        submittedScores = submittedScores + ScoreSubmission(leaderboardId, score)
        return submissionResult
    }

    fun emitSession(state: GamesSignInState) {
        sessionState.value = state
    }

    private companion object {
        const val DEFAULT_PLAYER_ID = "test-player"
        …
    }
}
```

`FakeCloudSaveStore` copies this exactly: public `var` knobs for behaviour, `private set` lists for
recording, a `private companion object` for defaults, **no MockK**. RESEARCH's sketch already
matches this shape.

**Submitter test harness** — analog `app/src/test/…/data/ProgressSubmitterTest.kt`
(lines 65–80, 523–533, 638–647):

```kotlin
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent/advanceUntilIdle virtual-time control
class ProgressSubmitterTest {
    private val testDispatcher = StandardTestDispatcher()

    private val submitterScope = CoroutineScope(testDispatcher)

    @After
    fun tearDown() {
        submitterScope.cancel()
    }
    …
    private fun submitterFor(
        repository: ProgressRepository,
        games: GamesService,
        store: SubmissionSyncStore,
    ): ProgressSubmitter =
        ProgressSubmitter(
            progressRepository = repository,
            gamesService = games,
            syncStore = store,
            scope = submitterScope,
        )
}

private class FakeSubmissionSyncStore(
    initial: Boolean = false,
    private val writeGate: CompletableDeferred<Unit>? = null,
) : SubmissionSyncStore(unusedDataStore()) {
    private val state = MutableStateFlow(initial)

    override val pending: Flow<Boolean> = state

    val value: Boolean get() = state.value
```

`ProgressSubmitterCloudTest` must add the new constructor parameters to `submitterFor` and extend
`FakeProgressRepository` (lines 584–606) with the import/export members. Note the
`SubmissionSyncStore(unusedDataStore())` subclassing trick — it is why the store class and both its
members are `open`; any new store must be `open` too.

**Robolectric + in-memory Room test** — analog `app/src/test/…/data/ProgressRepositoryTest.kt`
(lines 33–59):

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PROGRESS_ROBOLECTRIC_SDK])
class ProgressRepositoryTest {
    private lateinit var database: PegSolitaireDatabase
    private lateinit var repository: ProgressRepository

    @Before
    fun openInMemoryDatabase() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    PegSolitaireDatabase::class.java,
                ).build()
        repository =
            DefaultProgressRepository(
                database,
                database.levelResultDao(),
                database.resumeDao(),
                database.playSessionDao(),
            )
    }

    @After
    fun closeDatabase() {
        database.close()
    }
```

`ProgressImportTest` copies this verbatim (including the file-scoped `PROGRESS_ROBOLECTRIC_SDK`
constant convention) and reads `database.resumeDao()` / `database.playSessionDao()` directly for
the CLOUD-04 negative assertions.

**Compose UI test** — analog `app/src/test/…/ui/home/HomeScreenTest.kt` (lines 32–37):

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()
```

Its KDoc (lines 19–31) names the "invisible feature" trap this phase must also cover: the ViewModel
can sequence the profile-swap flag perfectly while the screen hosts nothing.

**ViewModel test** — analog `app/src/test/…/ui/home/HomeViewModelTest.kt` (lines 1–42): Turbine
(`app.cash.turbine.test`), Truth, `StandardTestDispatcher` + `Dispatchers.setMain`/`resetMain`,
`FakePlayGamesOptInRepository` (`app/src/test/…/data/FakePlayGamesOptInRepository.kt`) and
`FakeGamesService`; MockK only for `LevelRepository`-shaped collaborators.

---

## Shared Patterns

### Fail-open storage

**Source:** `data/StorageFailSafe.kt` lines 22–31 (full text quoted above)
**Apply to:** `ProgressRepository.importProgress`/`exportProgress`, `ChallengeProgress.mergeLadder`,
every new DataStore accessor, the D-14 wipe

Every read degrades to "nothing stored", every write is dropped rather than thrown — and
`CancellationException` is rethrown before classification, in all four of the idioms the codebase
uses (`storageFailSafe`, `runCatching{}.onFailure{ if (it is CancellationException) throw it }`,
`Flow.catch { emit(emptyPreferences()) }`, `try/catch (IllegalArgumentException)`). This is CLOUD-05
at the data layer.

### Failure is a return value on the platform seam (PGSL-04)

**Source:** `platform/games/GamesService.kt` lines 206–214; `PlayGamesService.submitting` lines 526–536
**Apply to:** `CloudSaveStore.load()` (`null`) and `CloudSaveStore.save()` (`false`), both impls,
both fakes

The seam never throws for an operational failure, and the caller's persisted marker is what
guarantees the retry. A throwing fake must therefore be indistinguishable from a `false`-returning
one at the `ProgressSubmitter` boundary (`attempt`, lines 277–280).

### Scoped constants objects (QUAL-03)

**Source:** `platform/games/GamesConstants.kt` lines 25–37; `data/ChallengeProgress.kt` lines 19–26;
`data/resume/ResumeSnapshotDto.kt` lines 125–160; `data/PlayGamesPreferences.kt` lines 13–54
**Apply to:** `CloudSaveConstants` (platform), `CloudPayloadKeys` (data/cloud), `ProgressMergeLimits`
(domain), new keys on `PlayGamesPreferences`

One named object **per scope**, never one global dump; every constant KDoc'd with *why that value*.
detekt `MagicNumber` is active over `src/main` with only `-1/0/1/2` exempt.

### Platform boundary (PLAT-04)

**Source:** `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt` lines 33–43, 86–92

```kotlin
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

val uiAndDomainNeverTouchAnActivity: ArchRule =
    noClasses()
        .that()
        .resideInAnyPackage("..ui..", "..domain..", "..data..")
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("android.app.Activity")
```

**Exact rules the new packages must satisfy:**

| Rule | Constraint on Phase 13 |
|------|------------------------|
| `onlyPlatformDependsOnPlaySdks` | `com.google.android.gms..` may appear only under `com.app.pegsolitaire.platform..`. `platform/games/save/` qualifies; `data/cloud/` and `ui/home/` do not. Because the check is bytecode-based it also covers Hilt-generated components — so the `@Binds` in `PlatformModule` must bind `CloudSaveStore`, never a gms type, and no `@Provides SnapshotsClient` may exist anywhere. |
| `onlyPlatformReachesTheForegroundActivity` | `ForegroundActivityProvider` may be referenced only inside `..platform..` (plus the three name-exempted classes `App`, `App_MembersInjector`, `PlatformModule`). `PlayGamesCloudSaveStore` is inside `..platform..`; `ProgressSubmitter` must never see it. |
| `uiAndDomainNeverTouchAnActivity` | No `android.app.Activity` reference from `..ui..`, `..domain..` or `..data..`. The `CloudSaveStore` seam therefore names no `Activity` in any signature. |
| `DomainPurityTest.domainDoesNotDependOnAndroid` (`app/src/test/…/DomainPurityTest.kt` lines 24–30) | `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("android..", "androidx..")`. `CloudProgress`/`ProgressMerge` may not import `androidx.room`, `androidx.datastore`, or anything Android. Note `kotlinx.serialization` is not caught by this rule but is excluded anyway by `:domain`'s empty `implementation` dependency set. |
| `PlatformBoundaryGateTest` (lines 50–61) | The gate test proves the rules still fail on a deliberately-violating fixture (`archfixture/GmsBoundaryViolationFixture.kt`). No change needed — but if the planner adds a new rule, it needs a matching fixture and a `assertThrows(AssertionError::class.java)` case. |

Additional non-ArchUnit gates that apply: `./gradlew check` runs `dpSpLiteralCheck` (no hardcoded
dp/sp in composables — use `dimensionResource`), `gamesIdLiteralCheck`, `detekt`, `ktlintCheck`,
plus the T-11-04 grep gate (zero `Log.`/`println`/`Timber` under `platform/games/` and, from this
phase, `data/cloud/`).

### Room additions without a schema bump

**Source:** `data/db/LevelResultDao.kt` lines 27–99, `data/db/PlaySessionDao.kt` lines 27–73
**Apply to:** `LevelResultDao.all()/upsertAll()/deleteAll()`, `PlaySessionDao.totalElapsedMs()/deleteAll()`,
`ResumeDao.clearAll()`

Interpolate every table/column name from `ProgressPersistenceKeys`; `COALESCE(..., 0)` any aggregate
that must be non-null. Adding `@Query`/`@Upsert` members to an existing `@Dao` changes no schema, so
`DB_VERSION`, `Migrations.kt`, the exported schema JSON and `MigrationTest` are all untouched.

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| the conflict-resolution loop inside `platform/games/save/PlayGamesCloudSaveStore.kt` | platform gateway | request-response | No existing code opens a resource that must be closed on every path, retries a server-side merge, or handles a `DataOrConflict`. The closest discipline is `PlayGamesService.launchStockUi` (never-throw + cancellation rethrow), but the open/commit/discard lifecycle and the bounded retry have no precedent — use RESEARCH § Pattern 4 and § Code Examples 1–3. |
| the encode → measure → trim loop in `data/cloud/CloudProgressCodec.kt` | mapper | transform | `SnapshotMapper` serializes once with no size budget. Use RESEARCH § Code Example 4. |
| `platform/games/save/CoverImageSource.kt` (vector → `Bitmap`) | platform utility | transform | The repo has no rasterization code and deliberately no PNGs (`res/mipmap-anydpi/ic_launcher.xml` states "There are no PNG mipmaps anywhere in the tree by design"). Only the `@ApplicationContext` injection style is borrowable (`PlayGamesService.kt` line 129). Use RESEARCH § Pattern 5. |

---

## Metadata

**Analog search scope:** `app/src/main/java/com/app/pegsolitaire/{data,platform,ui,di}/`,
`domain/src/main/kotlin/com/app/pegsolitaire/domain/`, `app/src/test/java/com/app/pegsolitaire/`,
`domain/src/test/kotlin/com/app/pegsolitaire/`, `app/src/main/res/values/`, `domain/build.gradle.kts`
**Files scanned:** 38 read in full or in targeted ranges; 70 test files and 6 package directories enumerated
**Pattern extraction date:** 2026-09-15
