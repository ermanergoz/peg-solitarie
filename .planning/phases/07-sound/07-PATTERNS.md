# Phase 7: Sound - Pattern Map

**Mapped:** 2026-08-28
**Files analyzed:** 27 (9 new source + 9 new test + 1 new doc + 13 modified, some overlap)
**Analogs found:** 25 / 27

Every new file in this phase has a close in-repo precedent. This is deliberate: RESEARCH.md
concluded "almost every requirement is satisfied by using the platform correctly and doing
nothing extra" — the plan's real risk is *deviating* from established idioms, not inventing
new ones. Copy the shapes below rather than re-deriving them.

---

## File Classification

### New files

| New file | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `app/src/main/java/com/app/pegsolitaire/audio/SoundPlayer.kt` | service interface (seam) | event-driven (fire-and-forget) | `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt` | exact |
| `app/src/main/java/com/app/pegsolitaire/audio/SoundPoolPlayer.kt` | service impl (`@Singleton`) | event-driven + settings-flow subscription | `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` (+ `platform/activity/DefaultForegroundActivityProvider.kt` for the `@Singleton`-on-class rationale) | role-match |
| `app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt` | config / scoped constants | pure (no I/O) | `app/src/main/java/com/app/pegsolitaire/ui/game/GameAnim.kt` | exact |
| `app/src/main/java/com/app/pegsolitaire/audio/LocalSoundPlayer.kt` | provider (CompositionLocal) + UI hook | request-response (composable → player) | `app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt:89` (`LocalClayColors`) | role-match |
| `app/src/main/java/com/app/pegsolitaire/di/AudioModule.kt` | DI config | n/a | `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` | exact |
| `app/src/main/res/raw/sfx_*.ogg` (≈12 files) | binary resource | file-I/O (load-once) | — no `res/raw` exists in the repo | **none** |
| `docs/audio/CREDITS.md` | documentation | n/a | `docs/ui/DESIGN-SYSTEM.md` (table-driven in-repo reference doc) | partial |
| `app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt` | test (pure JVM) | pure | `app/src/test/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProviderTest.kt` (runner-free style) | role-match |
| `app/src/test/java/com/app/pegsolitaire/audio/SoundPoolPlayerTest.kt` | test (Robolectric) | event-driven | `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt` (Robolectric `@Config(sdk=…)` shell) | role-match |
| `app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt` | test fake (recording) | event-driven | `RecordingHapticFeedback` in `GameFlowTest.kt` (used at `:81`, provided at `:247`) | exact |
| `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt` | test (ViewModel) | state-flow | `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt` | exact |
| `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt` | test (Compose + Robolectric) | request-response | `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt` | exact |
| `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsCelebrationSoundTest.kt` | test (Compose + Robolectric) | event-driven | `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt` | role-match |
| `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` | test (Compose + Robolectric) | request-response | `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt` | role-match |
| `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt` | test (ViewModel) | state-flow | `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt` | exact |
| `app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt` | test (file scan) | file-I/O | `build-logic/.../standards.gradle.kts` file-scan gates (`:40`, `:94`) — closest *scan* precedent; no JVM file-scan test exists | partial |

### Modified files

| Modified file | Role | Data Flow | Pattern to copy (in-file precedent) | Match Quality |
|---------------|------|-----------|-------------------------------------|---------------|
| `app/src/main/java/com/app/pegsolitaire/ui/game/MoveTransition.kt` | model (render instruction) | pure | add `runIndex` beside the existing `id` field (`:20-25`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt` | model (UiState) | pure | add two `Int` ordinals + carry them in `initial()`/`loading()` (`:65-138`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | viewmodel | state-flow | `performJump` (`:518`), `onCellTap` else-branch (`:496`), `onUndo` (`:689`), carry-forward `.copy(...)` (`:345`, `:385`, `:418`, `:822`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt` | component (stateless screen) | event-driven | `JumpHaptic` (`:220-231`), called at `:118` | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsCelebration.kt` | component (motion helpers) | event-driven | `rememberPopIn` (`:22-37`) + `rememberAnimationsEnabled` gate | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt` | component | event-driven | `EarnedStar` stagger (`:221`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt` | viewmodel | state-flow + play-once latch | `recordSolvedPlayOnce()` (`:110-133`) + `RECORDED_KEY` (`:155`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt` | model (UiState) | pure | add `playCelebration: Boolean` beside `newBests` (`:34-42`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt` | viewmodel | state-flow | `onToggleSound` (`:52-54`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsActions.kt` | component (CTAs) | request-response | `PrimaryAction`/`NeutralAction` `onClick` (`:99`, `:129`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` | component (CTAs) | request-response | `ClassicTile`/`ChallengeTile`/`ResumeTile` `onClick` (`:307`, `:371`, `:402`) | exact |
| `app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt` | component (CTAs) | request-response | `BoardCard` `onClick = { onSelect(descriptor.id) }` (`:138`) | exact |
| `app/src/main/java/com/app/pegsolitaire/MainActivity.kt` | activity host | n/a | its own `setContent` block (`:22-29`) + `App.kt`'s `@Inject lateinit var` (`:34-35`) | exact |
| `app/src/main/java/com/app/pegsolitaire/App.kt` | application root | event-driven (startup) | `onCreate` ordering comment + `activityProvider.attachTo(this)` (`:37-40`) | exact |
| `build-logic/src/main/kotlin/standards.gradle.kts` | build config | file-I/O scan | `adIdLiteralCheck` (`:94-126`) + `check` wiring (`:128-130`) | exact |
| `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt` | test | state-flow | extend existing | exact |

---

## Pattern Assignments

### `audio/SoundPlayer.kt` (service interface, event-driven)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt`

The interface-with-heavy-KDoc + `@Inject constructor` impl + `@Binds` in a DI module trio is
this repo's standard seam. Copy the KDoc shape too — every seam interface here explains *why*
it is a seam and names the module that binds it.

**Interface + seam KDoc pattern** (`AdGateway.kt:3-30`):
```kotlin
/**
 * The platform seam through which gameplay requests rewarded grants and shows
 * interstitials (spec §8.5, D-11). Today it is fulfilled by an auto-grant
 * [NoOpAdGateway] stub so hint/undo are free. Swapping the Hilt binding
 * (`PlatformModule`) for a real ad-backed implementation is necessary but **not
 * sufficient**: ...
 * Modelled on the
 * [com.app.pegsolitaire.data.LevelRepository] interface + `@Inject`-constructor-impl
 * + `@Binds` pattern.
 */
interface AdGateway {
    suspend fun requestRewarded(reason: AdReason): RewardOutcome

    fun showInterstitial()
}
```

**Enum-of-reasons companion pattern:** `AdReason` / `RewardOutcome` live as *separate small
files beside the interface* (`platform/ads/AdReason.kt`, `platform/ads/RewardOutcome.kt`), not
nested inside it. The `SoundCue` / `UiSoundCue` enums should follow the same file-per-type
placement in `audio/`.

**Contract note for the planner:** unlike `AdGateway`, `SoundPlayer` methods are **not**
`suspend` — playback is fire-and-forget (RESEARCH Pattern 1). And unlike every other seam here,
this one is **not** a Slice-2 swap point; say so in the KDoc so nobody wires it into
`PlatformModule` (see the explicit prohibition in `PlatformModule.kt:35-41`).

---

### `audio/SoundPoolPlayer.kt` (service impl, `@Singleton`, event-driven)

**Analog:** `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt`
**Secondary analog:** `app/src/main/java/com/app/pegsolitaire/platform/activity/DefaultForegroundActivityProvider.kt`

`ChallengeProgress` is the repo's only other `@Singleton` that (a) holds mutable state, (b)
injects `@ApplicationScope`, and (c) subscribes to a DataStore-backed flow. That is exactly the
`SoundPoolPlayer` shape.

**`@Singleton` + `@ApplicationScope` constructor pattern** (`ChallengeProgress.kt:43-49`):
```kotlin
@Singleton
class ChallengeProgress
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
```
Note the repo's ktlint formatting: `@Inject` on its own line, `constructor(` indented, and the
whole class body indented one extra level. `DefaultForegroundActivityProvider.kt:39-43` shows
the same shape with an interface supertype:
```kotlin
@Singleton
class DefaultForegroundActivityProvider
    @Inject
    constructor() :
    ForegroundActivityProvider,
        Application.ActivityLifecycleCallbacks {
```

**`@Singleton`-on-the-class rationale** — copy this KDoc argument verbatim in spirit
(`DefaultForegroundActivityProvider.kt:20-31`):
```kotlin
/**
 * **This class carries `@Singleton`, and that is not a stylistic slip.**
 * `@Binds @Singleton fun bind(impl: X): Y` scopes the binding of *`Y`*, not of `X` — so
 * if anything injected the concrete type it would get a **second, unscoped instance** ...
 */
```
This matters more for `SoundPoolPlayer` than for anything before it: a second, unscoped instance
would own a second `SoundPool`, and the SOUND-05 mute would reach only one of them.

**Injected `@ApplicationContext` pattern** (`di/DataModule.kt:73-75` — the only current
`@ApplicationContext` consumer):
```kotlin
@Provides
@Singleton
fun provideAppDataStore(
    @ApplicationContext context: Context,
): DataStore<Preferences> = context.appDataStore
```
`SoundPool.load(context, R.raw.x, 1)` needs only this Application `Context` — never
`LocalActivity`/`ForegroundActivityProvider`. `PlatformBoundaryTest` forbids `android.app.Activity`
from `..ui..`/`..domain..`/`..data..`, and a new `..audio..` package should hold that line too.

**Settings-flow subscription pattern** — the repo has two forms; use the `collect` form
(`GameViewModel.kt:294-298`), not `stateIn`, because the mute needs a side effect per emission:
```kotlin
viewModelScope.launch {
    settingsRepository.hapticsOn.collect { on ->
        _uiState.update { it.copy(hapticsOn = on) }
    }
}
```
In `SoundPoolPlayer` the scope is `@ApplicationScope` rather than `viewModelScope`, and the body
sets `enabled = on` then calls `silenceNow()` when `on` is false (RESEARCH Pattern 6).

**Fail-safe / best-effort idiom** (`ChallengeProgress.kt:80-95`): every app-lifetime write in
this repo is wrapped so a failure cannot crash the process. `SoundPool` calls do not throw, but
the *comment discipline* — stating that the `SupervisorJob` isolates but does not swallow — is
the house style for anything launched on `@ApplicationScope`.

**Constants placement** (`ChallengeProgress.kt:19-26`): scoped constants object at the top of the
file or in a sibling file — never inline literals. `SoundParams` is the sibling-file form.

---

### `audio/SoundParams.kt` (config, pure)

**Analog:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameAnim.kt` (whole file, 44 lines)
**Secondary:** `app/src/main/java/com/app/pegsolitaire/ui/theme/ClayAnim.kt:9-47`

Both files establish the exact precedent RESEARCH relies on: *unitless* Int ms and Float scales
live in a scoped `object`, **not** `dimens.xml`. Copy the header KDoc justification verbatim in
structure — it is what keeps the QUAL-03 reviewer from asking for a dimens resource.

**Scoped-constants object pattern** (`GameAnim.kt:8-34`):
```kotlin
/**
 * Animation timing tokens (milliseconds + easings) for the jump/removal/selection
 * feedback.
 *
 * Durations are unitless `Int` ms (not dp/sp), so they live in this scoped object
 * rather than `dimens.xml`. Both the jump slide (D-07) and the jumped-peg removal
 * (D-08) launch together keyed on the move id; the removal ([REMOVAL_MS]) finishes
 * just as the jumper lands ([JUMP_SLIDE_MS]).
 */
object GameAnim {
    /** Jumper straight-line slide source → destination (D-07). */
    const val JUMP_SLIDE_MS = 200

    /** Jumped peg alpha 1→0 + scale 1→`REMOVAL_SCALE_TARGET` (D-08). */
    const val REMOVAL_MS = 180
```
Every constant carries a one-line KDoc naming the decision it implements. Do the same for
`JUMP_SEMITONE_STEP` (D-05), `MAX_RATE` (D-07a — document the SoundPool 2.0× ceiling here, per
the resolved decision), `STAR_POP_STAGGER_MS` (D-13), `CELEBRATION_LEAD_IN_MS` (Open Question 3)
and the per-cue volumes (SOUND-02 "quiet, non-punishing").

**Cross-object reference discipline** (`ClayAnim.kt:12-16`):
```kotlin
 * Durations are unitless `Int` ms and scales/alphas unitless `Float`s (not
 * dp/sp), so they live in this scoped object rather than `dimens.xml`
 * (QUAL-03). Board-specific ratios/durations stay in `BoardMetrics`/`GameAnim`;
 * this object owns the cross-screen clay juice ...
```
`ClayAnim.STAR_STAGGER_MS` is **120** (`ClayAnim.kt:44`). Per CONTEXT, star-pop timing must
*read* this, not redeclare it; `SoundParams.STAR_POP_STAGGER_MS` exists only for the
animations-off path (D-13).

**Private-const-at-file-bottom variant** (`ResultsCelebration.kt:56-61`) — use this form for
values that are private to one file rather than shared:
```kotlin
/** Pop-in animation endpoints. */
private const val POP_START_SCALE = 0f
private const val SETTLED_SCALE = 1f
```

**detekt constraint:** `config/detekt/detekt.yml` whitelists only `-1, 0, 1, 2`. A `1.05946f`
step or a `0.35f` volume inlined anywhere **fails the build**.

---

### `audio/LocalSoundPlayer.kt` (provider + UI hook, request-response)

**Analog:** `app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt:89` — the repo's only
existing `CompositionLocal`.

**CompositionLocal declaration pattern** (`Clay.kt:88-89`):
```kotlin
/** The active clay palette, provided by `AppTheme` alongside the color scheme. */
val LocalClayColors = staticCompositionLocalOf { LightClayColors }
```
`staticCompositionLocalOf` is correct here too: the player never changes after `MainActivity`
provides it, so the cheaper static form applies (a changing value would invalidate the whole
subtree, which is fine for a value that never changes).

**Default value:** `LocalClayColors` has a real default. `LocalSoundPlayer` should default to a
**no-op player** rather than `error("not provided")`, so `@Preview`s and any composable rendered
outside `MainActivity` (previews, `screenshotTest`) do not crash. That is the same fail-open
posture as `SettingsRepository`'s defaults (`SettingsRepository.kt:20-24`).

**`withClickSound` helper** — the repo has no direct analog for a click-wrapping composable
function, but `ctaGradientBrush()` (`Clay.kt:96-103`) is the shape to copy for "a `@Composable`
helper function in the theme/infra layer that reads a CompositionLocal and returns a value":
```kotlin
@Composable
fun ctaGradientBrush(): Brush {
    val stops = LocalClayColors.current.ctaGradient
    return Brush.verticalGradient(...)
}
```

**Anti-pattern warning, with the exact code not to touch** — `Modifier.claySquish()`
(`Clay.kt:152-169`) is applied to *every* tappable clay surface including the top-bar back
buttons (`GameScreen.kt:261`, `ResultsScreen.kt:150`, `ClassicCatalogScreen.kt:100`,
`HomeScreen.kt:213`) and the Settings switches. Bolting the click sound onto it would blanket
D-09's exclusions. Keep the sound at the explicit per-callsite `onClick`.

---

### `di/AudioModule.kt` (DI config)

**Analog:** `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt`

**Module pattern** (`PlatformModule.kt:43-57`):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway
    ...
}
```

**Why a new module rather than adding to `PlatformModule`** — the file says so itself
(`PlatformModule.kt:35-41`): `..platform..` is scoped to the Play/AdMob SDK seam and three
ArchUnit rules depend on that definition. RESEARCH reaches the same conclusion. Put the
`@Binds SoundPlayer -> SoundPoolPlayer` in a new `AudioModule` and mirror `DataModule.kt:28-36`'s
"mirrors X's shell" KDoc convention.

**If a `@Provides` is ever needed** (e.g. to construct the `SoundPool` separately), copy
`DataModule.kt:70-89`: instance `@Provides` goes on the abstract module's `companion object`.

---

### `res/raw/sfx_*.ogg` (binary resources)

**No analog** — the app has no `res/raw` directory today (`app/src/main/res/` contains only
`values/`). Follow RESEARCH's naming scheme and Pitfall 7 (`[a-z0-9_]`, no leading digit).

The nearest *convention* precedent is `res/values/dimens.xml` + `font_certs.xml`, and the
release-shrink verification technique RESEARCH cites is already documented in
`app/build.gradle.kts`'s release block for `font_certs.xml` — reuse that `aapt2 dump resources`
approach for Pitfall 6.

---

### `docs/audio/CREDITS.md` (documentation)

**Analog:** `docs/ui/DESIGN-SYSTEM.md` (partial — the only in-repo reference doc).

Copy its structure: `# Title` → provenance line naming the source and date → markdown tables
with a header row per column of fact. Its opening lines:
```markdown
# Peg Solitaire — "Candy Clay" Design System (v2 UI)

Source: ui-ux-pro-max design-system run, 2026-08-26. Style: **Claymorphism (mobile)** — the
recommended style for casual puzzle games. Replaces the stock-AOSP baseline M3 theme.
```
For CREDITS the table columns are the four facts D-01 requires plus the Pitfall-7 rename:
`res/raw filename | original filename | source URL | licence`.

---

### `ui/game/GameScreen.kt` — the cue composables (component, event-driven)

**Analog:** `JumpHaptic` in the same file — this is the anchor CONTEXT names.

**The one-shot-cue idiom** (`GameScreen.kt:220-231`):
```kotlin
@Composable
private fun JumpHaptic(
    animatingMoveId: Int?,
    hapticsOn: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(animatingMoveId) {
        if (animatingMoveId != null && hapticsOn) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
}
```

**Its KDoc — copy the argument, it pre-answers the reviewer** (`GameScreen.kt:203-219`):
```kotlin
/**
 * Fires one light haptic tick as each jump animation begins (SET-02 / D-15).
 *
 * Haptic emission is a Compose UI concern, so it lives here rather than in the ViewModel:
 * the screen reads the already-collected [GameUiState.hapticsOn] gate (it never touches
 * `SettingsRepository`) and the effect is keyed on the [MoveTransition.id] move ordinal —
 * so it fires exactly once per jump, not once per recomposition. ...
 */
```

**Call-site placement** — first line of the screen body, before the `Scaffold`
(`GameScreen.kt:118`):
```kotlin
    JumpHaptic(animatingMoveId = state.animating?.id, hapticsOn = state.hapticsOn)
```

**The one deliberate divergence to document:** sound cues take **no `soundOn` parameter**. The
gate lives inside the player (SOUND-05 needs to reach in-flight streams). Write that into the
new composables' KDoc explicitly, or a future reader will "fix" it by copying the `hapticsOn`
plumbing — the asymmetry is intentional.

**Also in this file, for D-09 button coverage:** `RestartConfirmDialog`'s confirm
(`GameScreen.kt:373`) gets the `Forward` cue and its dismiss (`GameScreen.kt:389`) the `Dismiss`
cue (D-11); the top-bar back `IconButton` (`GameScreen.kt:257`) stays silent.

---

### `ui/game/MoveTransition.kt` — `+ runIndex` (model, pure)

**Analog:** the file itself (25 lines), which is already the per-jump render instruction.

**Existing shape** (`MoveTransition.kt:19-25`):
```kotlin
@Immutable
data class MoveTransition(
    val from: Position,
    val to: Position,
    val removed: Position,
    val id: Int,
)
```
Its KDoc convention explains what each field drives and why `id` exists (`:6-18`); add a
`runIndex` line in the same voice, naming D-05/D-06 and stating that it is *derived from
`moveHistory`, never stored* (which is what makes D-08 free).

---

### `ui/game/GameUiState.kt` — `+ undoTick`, `+ rejectedTapTick` (model, pure)

**Analog:** the file itself.

**`@property` KDoc + default-valued field convention** (`GameUiState.kt:36-41, 64-82`):
```kotlin
 * @property canUndo whether there is at least one move to undo (ASST-02, D-05): mirrors
 *   `moveHistory.isNotEmpty()`, so it is `false` at the initial board, `true` after any
 *   jump, and `false` again once undo walks back to the start. ...
@Immutable
data class GameUiState(
    val board: Board,
    ...
    val canUndo: Boolean = false,
    ...
)
```

**Critical: both factories must list the new fields.** `initial()` (`:94-112`) and `loading()`
(`:120-138`) enumerate **every** field explicitly rather than relying on defaults:
```kotlin
        fun initial(board: Board = Boards.english33Initial): GameUiState =
            GameUiState(
                board = board,
                selected = null,
                ...
                stuck = false,
            )
```
Follow that (add `undoTick = 0, rejectedTapTick = 0`), then read Pitfall 2 — the fresh `0` is a
*changed key* for a `LaunchedEffect` sitting at `3`, so the ordinals must **also** be carried
forward at every rebuild site (below) **and** guarded with `if (tick > 0)`.

---

### `ui/game/GameViewModel.kt` — run index + two ordinals (viewmodel, state-flow)

**Analog:** the file itself, four specific sites.

**1. Where `MoveTransition` is built — add `runIndex`** (`GameViewModel.kt:536-542`):
```kotlin
            val transition =
                MoveTransition(
                    from = move.from,
                    to = move.to,
                    removed = move.over,
                    id = moveHistory.size,
                )
```
`moveHistory` is a plain `private val ... : MutableList<Move> = mutableListOf()`
(`GameViewModel.kt:226`) — the pure `runIndexAt(history, index)` function from RESEARCH Pattern 2
reads it directly, no new state.

**2. The invalid-tap branch — currently unobservable** (`GameViewModel.kt:486-498`):
```kotlin
        fun onCellTap(position: Position) {
            val state = _uiState.value
            when {
                // Re-tapping the selected peg is the only deselect (D-04); clear inline.
                state.selected == position ->
                    _uiState.update { it.copy(selected = null, legalTargets = emptyList()) }
                state.selected != null && position in state.legalTargets ->
                    performJump(from = state.selected, to = position)
                state.selected == null && state.board.cellAt(position) == Cell.Peg ->
                    select(position)
                else -> Unit
            }
        }
```
`else -> Unit` becomes the `rejectedTapTick++` site. Per D-05a the **first** branch (deselect)
reuses the selection cue and must not increment `rejectedTapTick`.

**3. The undo site** (`GameViewModel.kt:699-728`) — increment `undoTick` inside the existing
`_uiState.update { it.copy(...) }` block, only after the history actually shrank:
```kotlin
                if (moveHistory.isEmpty()) return@launch
                moveHistory.removeAt(moveHistory.lastIndex)
                ...
                _uiState.update {
                    it.copy(
                        board = board,
                        selected = null,
                        legalTargets = emptyList(),
                        animating = null,
                        status = status,
                        canUndo = moveHistory.isNotEmpty(),
                        hintSource = null,
                        hintTarget = null,
                        noWinningMove = false,
                        stuck = false,
                    )
                }
```
Note the early return at `:697` for `RewardOutcome.Declined` — a declined undo must not tick.

**4. The four state-rebuild sites that must carry the ordinals forward** — this is the exact
existing mechanism for the exact same class of bug (`GameViewModel.kt:817-825`):
```kotlin
            // Preserve the player's hint + haptics choices across a restart; the collected
            // settings flows only re-emit on change, so carry them forward explicitly.
            _uiState.value =
                GameUiState
                    .initial(board)
                    .copy(
                        showHints = _uiState.value.showHints,
                        hapticsOn = _uiState.value.hapticsOn,
                    )
```
The same `.copy(showHints = it.showHints, hapticsOn = it.hapticsOn)` appears at
`:342-346` (`loadChallengeBoardIfNeeded`), `:383-386` (`installClassicBoard`) and
`:411-421` (`applyRestored`). All four need the two new ordinals added.

**5. Companion-object constants** (`GameViewModel.kt:840-849`) — the house form for
ViewModel-local constants:
```kotlin
        private companion object {
            /** A board solved to a single remaining peg is a Won game (D-05 "Perfect"). */
            const val SOLVED_PEG_COUNT = 1
```

---

### `ui/results/ResultsCelebration.kt` — `StarPopSounds` (component, event-driven)

**Analog:** `rememberPopIn` in the same file — the celebration's own timing helper.

**Reduced-motion gate + `LaunchedEffect` sequencing** (`ResultsCelebration.kt:21-37`):
```kotlin
@Composable
internal fun rememberPopIn(delayMillis: Int): () -> Float {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale = remember { Animatable(if (animationsEnabled) POP_START_SCALE else SETTLED_SCALE) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = SETTLED_SCALE,
            animationSpec =
                tween(
                    durationMillis = ClayAnim.POP_IN_MS,
                    delayMillis = delayMillis,
                    easing = ClayAnim.OVERSHOOT_EASING,
                ),
        )
    }
    return { scale.value }
}
```
Note the file-header comment (`:11-14`) stating that both helpers "respect the OS
animator-duration-scale gate". `StarPopSounds` deliberately **breaks** that symmetry per D-13
(audio survives reduced motion) — call that out in its KDoc, since the file currently promises
the opposite.

**The gate itself** (`ui/theme/Clay.kt:209-218`):
```kotlin
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            DEFAULT_ANIMATOR_SCALE,
        ) != DISABLED_ANIMATOR_SCALE
    }
}
```

**The stagger to mirror** (`ResultsScreen.kt:219-221`):
```kotlin
@Composable
private fun EarnedStar(order: Int) {
    val scale = rememberPopIn(delayMillis = order * ClayAnim.STAR_STAGGER_MS)
```
`ClayAnim.STAR_STAGGER_MS` = 120 (`ClayAnim.kt:43-44`). `StarRow` (`ResultsScreen.kt:193-216`)
is where `earned` is known and `TOTAL_STARS` is iterated — the natural sibling call site for
`StarPopSounds(earned = earned, play = state.playCelebration)`.

---

### `ui/results/ResultsViewModel.kt` — D-16 play-once latch (viewmodel)

**Analog:** `recordSolvedPlayOnce()` in the same file — CONTEXT names it explicitly.

**The `SavedStateHandle` latch** (`ResultsViewModel.kt:110-113` + `:154-155`):
```kotlin
        private fun recordSolvedPlayOnce() {
            if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return
            if (savedStateHandle.get<Boolean>(RECORDED_KEY) == true) return
            savedStateHandle[RECORDED_KEY] = true
            ...
        }

        private companion object {
            /** SavedStateHandle flag marking this play as already merged into the stored bests. */
            const val RECORDED_KEY = "results_recorded"
```
Called once from `init { recordSolvedPlayOnce() }` (`:98-100`). The guard is set **before** the
async work is launched — copy that ordering exactly; it is what makes a second ViewModel over
the same handle unable to race.

**Why `SavedStateHandle` and not `remember`** — already argued in the class KDoc (`:38-41`):
```kotlin
 *  - **It is recorded at most once (T-05-09).** The guard flag lives in the
 *    [SavedStateHandle], so neither a configuration change (same instance) nor a
 *    process-death restore (new instance, same handle) can double-count the play.
```
D-16 names exactly those two re-entry paths.

**Exposing it to the screen** (`ResultsViewModel.kt:131`) — the state update idiom:
```kotlin
                _uiState.update { it.copy(newBests = newBests) }
```
Add `playCelebration: Boolean` to `ResultsUiState` beside `newBests` (`ResultsUiState.kt:42`),
which already documents a field whose whole job is "a one-shot cue that recomposition can never
re-fire" (`:28-31`).

---

### `ui/settings/SettingsViewModel.kt` — D-12 enable blip (viewmodel)

**Analog:** `onToggleSound` in the same file.

**The toggle** (`SettingsViewModel.kt:51-54`):
```kotlin
        /** Persists the player's sound choice (SET-01); the collected flow re-renders the switch. */
        fun onToggleSound(on: Boolean) {
            viewModelScope.launch { settingsRepository.setSoundOn(on) }
        }
```
Per RESEARCH Pitfall 3, the blip goes **here** (guarded `if (on)`), calling a documented
gate-bypassing method on the player — *not* in the `init` flow collector (`:38-49`), which fires
on the initial Settings-screen emission and would violate D-12.

This makes `SettingsViewModel` gain a second constructor dependency. Keep the KDoc's existing
"the repository is the single authority" claim honest by noting that the blip is feedback only
and never a state source.

**Do not touch:** `SettingsScreen.kt:90` / `:97` (`onCheckedChange = onToggleSound` /
`onToggleHaptics`) — the switches stay silent per D-09; the blip is a ViewModel-side effect.

---

### Button-sound call sites (SOUND-04 / D-09 / D-11)

**Analog:** `ui/results/ResultsActions.kt` — the cleanest CTA file, and every excluded control is
in the same files, so the contrast is visible in review.

**Forward-cue sites** (`ResultsActions.kt:52-62`, `:76`, `:99-104`):
```kotlin
            PrimaryAction(
                textRes = R.string.action_next_level,
                onClick = { onNextLevel(challengeLevel + 1) },
            )
        }
        NeutralAction(textRes = R.string.action_home, onClick = onHome)
```
```kotlin
@Composable
private fun PrimaryAction(
    textRes: Int,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Button(
        onClick = onClick,
```
Wrap at the **leaf** (`PrimaryAction`/`NeutralAction`/`ClassicActions`' `OutlinedButton`) or at
each caller — planner's call, but be consistent and greppable.

**Home tiles** (`HomeScreen.kt:307`, `:371`, `:402`):
```kotlin
        onClick = onResume,          // ResumeTile
        onClick = onPlayClassic,     // ClassicTile
        onClick = { onPlayChallenge(currentLevel) },   // ChallengeTile
```
All three funnel through `ModeTile(onClick = ...)` (`HomeScreen.kt:428-440`).

**Catalog board cards** (`ClassicCatalogScreen.kt:138`):
```kotlin
        onClick = { onSelect(descriptor.id) },
```

**Explicitly silent (D-09) — do not wrap:**
- back nav `IconButton`s: `HomeScreen.kt:213` (`onShowHowToPlay`), `:220` (`onOpenSettings`),
  `ClassicCatalogScreen.kt:100`, `ResultsScreen.kt:150`, `GameScreen.kt:257`
- Settings switches: `SettingsScreen.kt:90`, `:97`
- `HintToggleControl`, and `UndoControl`/`HintControl` (they carry their own SOUND-02 cues)

---

### `MainActivity.kt` + `App.kt` (composition root)

**Analog:** both files, as they stand.

**Field injection into an `@AndroidEntryPoint`** — copy `App.kt`'s form (`App.kt:32-40`):
```kotlin
@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var activityProvider: ForegroundActivityProvider

    override fun onCreate() {
        super.onCreate()
        activityProvider.attachTo(this)
    }
}
```
Its KDoc states the two rules that apply verbatim to the sound player (`App.kt:16-25`):
injection completes only after `super.onCreate()` returns, and the declared type must be the
**interface** — naming the concrete class would yield a second, unscoped instance.

**Providing to Compose** (`MainActivity.kt:20-30`):
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppNavHost(navController = rememberNavController())
            }
        }
    }
}
```
Wrap with `CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) { ... }`. Its KDoc
ends "the activity adds no logic beyond hosting" (`:18`) — providing a CompositionLocal keeps
that true; **do not** put preload or lifecycle logic here (that belongs in `App.onCreate`, per
RESEARCH Pitfall 1).

---

### `build-logic/src/main/kotlin/standards.gradle.kts` — `audioFocusLiteralCheck`

**Analog:** `adIdLiteralCheck` in the same file — a near-identical negative-assertion file scan.

**The full task shape to clone** (`standards.gradle.kts:77-126`):
```kotlin
// ---- PLAT-06: custom "no ad-unit-ID literal in source" gate ------------------
val adIdSourceRoots = listOf("src/main/java", "src/main/kotlin", "src/main/res")
    .map { layout.projectDirectory.dir(it).asFile }

val adIdMarker = "ca-app-pub-"

// Comment prefixes across the scanned file types (Kotlin, KDoc continuation, XML,
// properties/shell). A comment explaining *why* IDs are not in source must not itself
// trip the gate.
val commentPrefixes = listOf("//", "*", "<!--", "#")

val adIdLiteralCheck =
    tasks.register("adIdLiteralCheck") {
        group = "verification"
        description =
            "Fails on ad-unit-ID literals in src/main (use buildConfigField/resValue)."
        doLast {
            val offenders = mutableListOf<String>()
            adIdSourceRoots.filter { it.exists() }.forEach { root ->
                root.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        file.readLines().forEachIndexed { index, line ->
                            val trimmed = line.trimStart()
                            if (commentPrefixes.any { trimmed.startsWith(it) }) {
                                return@forEachIndexed
                            }
                            if (line.contains(adIdMarker)) {
                                val where = file.relativeTo(moduleDir).invariantSeparatorsPath
                                offenders += "$where:${index + 1}: $trimmed"
                            }
                        }
                    }
            }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "Ad-unit-ID literals are forbidden in src/main (PLAT-06 / D-05). " +
                        ... +
                        offenders.joinToString("\n  "),
                )
            }
        }
    }
```

**Wiring into `check`** (`standards.gradle.kts:128-130`):
```kotlin
tasks.named("check").configure {
    dependsOn("detekt", "ktlintCheck", dpSpLiteralCheck, adIdLiteralCheck)
}
```

**Two things to also update:** the file-header KDoc (`:4-15`) currently says "Wires **four**
checks" and enumerates them — a fifth means editing that count and adding a bullet. And note the
comment-skip is load-bearing: the KDoc for `SoundPoolPlayer` *will* contain the word
`requestAudioFocus` (explaining why it is absent), so the gate must skip `//`/`*` lines exactly
as `adIdLiteralCheck` does.

---

### Test files

**Recording-fake pattern** — `FakeSoundPlayer` clones `RecordingHapticFeedback`
(`GameFlowTest.kt:80-81`, provided at `:245-264`):
```kotlin
    /** Captures every haptic the screen fires, so the SET-02 gate is asserted, not assumed. */
    private val haptics = RecordingHapticFeedback()
...
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    GameScreen(
                        state = state,
                        onCellTap = viewModel::onCellTap,
                        ...
                    )
                }
            }
        }
        composeRule.waitForIdle()
```
`CompositionLocalProvider(LocalSoundPlayer provides sounds)` slots in identically — a second
provider on the same line or nested.

**Negative-gate assertion style** (`GameFlowTest.kt:210-223`) — the model for "sound off ⇒ no
cue, but gameplay unchanged":
```kotlin
    @Test
    fun noHapticFiresOnAJumpWhileHapticsAreOff() {
        val viewModel =
            startGame(Boards.english33Initial, settings = FixedSettingsRepository(hapticsOn = false))
        assertThat(viewModel.uiState.value.hapticsOn).isFalse()

        tapCell(row = 3, col = 1)
        tapCell(row = 3, col = 3)

        // The jump still happened — only the feedback is silenced (the switch is a gate,
        // never a gameplay change).
        assertPegCount(INITIAL_PEG_COUNT - 1)
        assertThat(haptics.events).isEmpty()
    }
```

**Robolectric shell** (`GameFlowTest.kt:72-78`, `:298-299`):
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class GameFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()
...
/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
```
`SoundPoolPlayerTest` needs the same `@Config(sdk = …)` pin (it must not assume SDK 36 exists)
and gets its `Context` from `RuntimeEnvironment.getApplication()`.

**ViewModel test shell** (`SettingsViewModelTest.kt:37-58`):
```kotlin
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bothTogglesDefaultOnBeforeTheStoreEmits() =
        runTest(testDispatcher) {
            val viewModel = SettingsViewModel(NeverEmittingSettingsRepository)
            runCurrent()

            assertThat(viewModel.uiState.value.soundOn).isTrue()
        }
```
Note the fake-repository naming convention in this file: `FakeSettingsRepository`,
`NeverEmittingSettingsRepository`, `AllOnSettingsRepository`, `FixedSettingsRepository` —
descriptive `<Behavior><Type>` names, declared as file-private types at the bottom of the test
file (or shared where two files need them).

**Runner-free pure test** (`ForegroundActivityProviderTest.kt:21-31`) — the model for
`SoundParamsTest`, which needs no Android runtime at all:
```kotlin
class ForegroundActivityProviderTest {
    private val provider = DefaultForegroundActivityProvider()

    @Test
    fun resumePublishesTheActivity() {
        val activity = mockk<Activity>()

        provider.onActivityResumed(activity)

        assertThat(provider.current).isSameInstanceAs(activity)
    }
```
Arrange / act / assert separated by blank lines, one behaviour per test, Truth assertions,
descriptive `camelCase` sentence method names. That is the house style throughout.

---

## Shared Patterns

### KDoc-as-design-record
**Source:** every file read for this map — `AdGateway.kt:3-13`, `PlatformModule.kt:15-42`,
`DefaultForegroundActivityProvider.kt:13-37`, `GameUiState.kt:10-63`, `ResultsViewModel.kt:21-57`
**Apply to:** every new source file

This codebase's most consistent convention: each class/function KDoc names the requirement and
decision IDs it implements, states *why* a non-obvious choice was made, and pre-empts the
"helpful" refactor that would break it. `DefaultForegroundActivityProvider.kt:31` literally says
`Do not "normalise" this one back to match them.` Phase 7 has three such traps to document:
- the sound cues take no `soundOn` param (gate is in the player — SOUND-05);
- `SoundPoolPlayer` carries `@Singleton` on the class (a second instance owns a second pool);
- no `requestAudioFocus()` call is an implementation, not an omission (SOUND-06).

### Scoped constants, never inline literals
**Source:** `GameAnim.kt`, `ClayAnim.kt`, `SettingsRepository.kt:45-64`,
`ChallengeProgress.kt:19-26`, `GameViewModel.kt:840-849`, `ResultsUiState.kt:54-58`
**Apply to:** `SoundParams.kt` and every new file

Four shapes exist, all in use — pick per scope:
| Scope | Shape | Example |
|-------|-------|---------|
| Cross-file, thematic | top-level `object X { const val … }` | `GameAnim`, `ClayAnim` |
| Package-internal keys/defaults | `internal object XPreferences` | `SettingsPreferences` (`SettingsRepository.kt:46`) |
| Class-private | `private companion object` | `GameViewModel` (`:840`), `ResultsViewModel` (`:150`) |
| File-private | `private const val` at file bottom | `ResultsCelebration.kt:56-61` |

detekt `MagicNumber` whitelists only `-1, 0, 1, 2`.

### Fail-open defaults
**Source:** `SettingsRepository.kt:20-24, 80-93`, `ChallengeProgress.kt:38-41`
**Apply to:** `LocalSoundPlayer`'s default value, `SoundPlayer` behaviour on a not-yet-loaded
sample
```kotlin
 * Every read **fails open** to `true` (D-17): a missing key (fresh install) or a
 * corrupt/IO read both degrade to "helper on", so a damaged store can never silently
 * strip a beginner's help — it only re-enables the harmless assists.
```
```kotlin
        override val soundOn: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[SettingsPreferences.SOUND_KEY] ?: SettingsPreferences.DEFAULT_SOUND }
```
`soundOn` already exists and needs no change — consume it as-is. For audio, "fail open" means a
missing/unloaded sample is silently skipped, never a crash, and `LocalSoundPlayer` outside
`MainActivity` resolves to a no-op rather than throwing.

### State hoisting / stateless screens
**Source:** `GameUiState.kt:10-17`, `ResultsUiState.kt:6-11`, `GameScreen.kt:62-65`,
`SettingsScreen.kt:43`
**Apply to:** every screen touched

All cue inputs must be hoisted `UiState` values (`animating.runIndex`, `undoTick`,
`rejectedTapTick`, `status`, `playCelebration`). No composable-internal `remember` may own a cue
trigger — RESEARCH's "Don't Hand-Roll" table rejects `remember {}` for play-once explicitly.

### Read-only `StateFlow` exposure
**Source:** `GameViewModel.kt:228-234`, `ResultsViewModel.kt:77-96`, `SettingsViewModel.kt:33-36`
**Apply to:** any new ViewModel state
```kotlin
        private val _uiState = MutableStateFlow(SettingsUiState())

        /** Read-only state stream for the screen; the mutable flow never leaks. */
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
```

### `@Binds` seam with an `@Inject constructor` impl
**Source:** `AdGateway.kt` + `NoOpAdGateway.kt` + `PlatformModule.kt:46-56`;
`SettingsRepository.kt:25/75` + `DataModule.kt:52-54`
**Apply to:** `SoundPlayer` → `SoundPoolPlayer` in the new `AudioModule`
```kotlin
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
```
```kotlin
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway
```

### Architectural boundaries the plan must not cross
**Source:** `DomainPurityTest.kt`, `PlatformBoundaryTest.kt` + `PlatformBoundaryRules.kt`,
`PlatformModule.kt:35-41`, `standards.gradle.kts:128-130`
**Apply to:** package placement of every new file
- Nothing audio-related may reference `..domain..` from Android code or add `android..` to
  `:domain` — the run index is derived in `GameViewModel`, not `RulesEngine`.
- The new `..audio..` package must not touch `android.app.Activity`; `SoundPool` needs only the
  Application `Context`.
- Do **not** fold audio into `..platform..` — `PlatformModule.kt:35-41` defines that package as
  the SDK/Activity seam and three ArchUnit rules depend on the definition.
- Any new ArchUnit test class must use the `DoNotIncludeAndroidTestOutput` import option
  (`app/src/test/java/com/app/pegsolitaire/DoNotIncludeAndroidTestOutput.kt`).

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/src/main/res/raw/sfx_*.ogg` | binary resource | file-I/O | No `res/raw` directory exists; `app/src/main/res/` holds only `values/`. Use RESEARCH's naming scheme (Pitfall 7) and the `aapt2 dump resources` shrink check already documented in `app/build.gradle.kts` for `font_certs.xml` (Pitfall 6). |
| `app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt` | test (file scan) | file-I/O | No JVM test in the repo scans repository files. Closest precedent is the *Gradle-side* scan in `standards.gradle.kts:94-126`; alternatively the planner may implement D-01 provenance as a sixth `standards.gradle.kts` gate instead of a JVM test — which would then have an exact analog. |

Two partial-analog notes for the planner:
- **`SoundPoolPlayer`'s `SoundPool` lifecycle** (load-complete listener, stream-ID ring buffer,
  `autoPause()`): no in-repo precedent — it is platform-API work. Follow RESEARCH Pattern 6 and
  Pitfalls 1/4/8 directly.
- **`withClickSound`**: no existing composable wraps a click callback. `ctaGradientBrush()`
  (`Clay.kt:96-103`) is the nearest shape (a `@Composable` helper reading a CompositionLocal).

---

## Metadata

**Analog search scope:**
`app/src/main/java/com/app/pegsolitaire/**` (79 files listed, 22 read),
`app/src/test/java/com/app/pegsolitaire/**` (41 files listed, 4 read),
`build-logic/src/main/kotlin/`, `app/src/main/res/`, `docs/`

**Files scanned:** 120 listed / 27 read
**Pattern extraction date:** 2026-08-28
