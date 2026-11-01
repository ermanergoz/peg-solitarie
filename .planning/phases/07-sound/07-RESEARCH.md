# Phase 7: Sound - Research

**Researched:** 2026-08-28
**Domain:** Android low-latency SFX playback (SoundPool), audio-focus policy, Compose one-shot side effects, CC0 asset sourcing
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Sound aesthetic & asset sourcing**
- **D-01:** Assets come from **curated CC0 packs** (e.g. Kenney audio sets, freesound CC0). The executor downloads, trims, and commits them during the phase. **License provenance must be recorded in-repo** (a CREDITS/LICENSES note listing each file's source and license).
- **D-02:** Aesthetic is **soft candy-clay pops** — squishy, rounded, playful, matching the Candy Clay visual redesign (merged to `develop` 2026-08-27, see `docs/ui/DESIGN-SYSTEM.md`). Not wooden, not sterile UI blips.
- **D-03:** Curation favors **one cohesive pack family**, borrowing individual sounds elsewhere only where the anchor pack has no fit. Consistency beats per-event optimality.
- **D-04:** Approval is **audit at verify/UAT** — the executor curates and wires the full set; the user judges by playing the build during phase verification and swaps any duds then. No mid-phase audition checkpoint.

**Jump pitch variation (SOUND-01)**
- **D-05:** Jump pitch **rises across a run**, resetting to base when the run ends. Random-only variation was rejected — the ladder should feel like a combo reward.
- **D-06:** A "run" is **same-peg only**: pitch climbs only while the same peg keeps jumping (the classic multi-capture run). Jumping with a different peg resets the ladder to base. This is the purist reading — chains are rarer and therefore mean more.
- **D-07:** The ladder has **no pitch cap** — every jump in a run steps up, without ceiling. Epic runs are allowed to get audibly silly; that is the point.
- **D-08:** **Undo steps the ladder back down one** rather than resetting it. The pitch ladder mirrors board state exactly: undoing a run jump then redoing it replays the same pitch it had before.

**Button-press coverage (SOUND-04)**
- **D-09:** The button sound covers **navigational CTAs**: Home mode buttons, board selection, Results actions (Continue/Retry/etc.), and dialog confirm buttons. In-game assist controls (Undo, Hint) keep their own distinct event sounds from SOUND-02; toggles and back navigation stay silent.
- **D-10:** Our button sound **replaces the Android system tick** on controls where it plays (suppress `soundEffectsEnabled` click on those controls) so users with OS touch-sounds on never hear a double click. Controls without our sound keep default OS behavior.
- **D-11:** Buttons use a **forward + dismiss pair**: a brighter click for forward/commit actions (Start, Continue) and a softer one for cancel/dismiss. This deliberately extends the asset set to ~12 sounds — still SOUND-04 scope, not a new capability.
- **D-12:** **Exception to toggle silence:** flipping `soundOn` **ON** in Settings plays one soft confirmation pop — instant proof the audio works. Turning it OFF stays silent (silence is the confirmation). This blip must not fire on the initial Settings-screen state load, only on user-initiated enable.

**Win & star-pop sequencing (SOUND-03)**
- **D-13:** With OS animations off (the celebration's `rememberAnimationsEnabled()` gate — stars land instantly), star pops still play on their **own short fixed stagger** (~150 ms apart, rising pitch). The audio celebration survives reduced-motion settings.
- **D-14:** The **win sound fires at the moment the winning move lands on the board** (final jump), then the star pops play with the Results celebration as stars land. Two-beat structure: instant payoff on the board, ceremony on Results.
- **D-15:** Star-pop pitches are a **ladder prefix**: pops always start at the same base note and climb, so a 2-star result plays notes 1–2 and the top/resolving note is audibly reserved for a 3-star finish.
- **D-16:** Celebration audio (win + pops on Results) plays **once per win** — mirroring the existing `recordSolvedPlayOnce()` play-once idiom. Re-entering the same Results (process-death restore, back navigation) shows stars silently.

### Claude's Discretion
- Audio engine choice (SoundPool is the obvious fit for low-latency one-shots with per-stream `setRate` pitch control — confirm at research), file format (OGG vs WAV), preload/warm-up strategy for "first tap never silent", and stream-count limits.
- Exact pitch step size, base pitch, and the invalid-tap character ("quiet, non-punishing" per SOUND-02) — tune by ear within the candy aesthetic.
- Relative volume balancing across the set (invalid tap noticeably quieter than jump; win louder but not startling).
- Architecture placement: whether sound triggering follows the `JumpHaptic` UI-effect pattern (composable keyed on state transitions) or a dedicated player owned at a different layer — subject to the SOUND-05 constraint that queued one-shots must be stoppable immediately when `soundOn` flips off, and SOUND-06's requirement to use game/media audio attributes without requesting audio focus.
- How the same-peg run counter and its undo mirroring integrate with existing move/undo state (likely derivable from `MoveTransition`/move history rather than new domain state — planner decides; keep `:domain` purity).

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope. (Background music, lose/stuck stingers, and per-board soundscapes were never raised and remain out of scope; the requirement set is SFX-only.)
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SOUND-01 | Player hears a jump sound on every peg jump, pitch-varied so repeated jumps do not fatigue | `SoundPool.play(..., rate)` per-stream pitch (§Standard Stack); run index derived free from existing `moveHistory` (§Pattern 2); **engine clamps rate to [0.5, 2.0]** — see Open Question 1 |
| SOUND-02 | Distinct feedback for peg removal, invalid tap (quiet, non-punishing), selection, and undo | `JumpHaptic` state-transition idiom (§Pattern 1); invalid tap and undo need two new monotonic tick fields — the current `onCellTap` `else -> Unit` branch leaves no state trace (§Pattern 3) |
| SOUND-03 | Win sound + star-pop sounds sequenced in rising pitch to the existing Results celebration animation | `ClayAnim.STAR_STAGGER_MS` (120) already drives `rememberPopIn(delayMillis = order * …)`; `rememberAnimationsEnabled()` gate handled per D-13 (§Pattern 4); play-once via `SavedStateHandle` mirroring `RECORDED_KEY` |
| SOUND-04 | Button-press feedback on primary UI actions | Explicit per-callsite `withClickSound(...)` wrapper (§Pattern 5). **D-10 is a no-op** — Compose never plays the platform tick (§Key Finding 3) |
| SOUND-05 | Turning off `soundOn` silences all sound immediately, including already-queued one-shots | Gate lives **inside** the player, not in `GameUiState`; tracked stream IDs + `stop()` / `autoPause()` — there is no `stopAll()` (§Pattern 6, §Pitfall 4) |
| SOUND-06 | Sound plays at system media volume and never interrupts or ducks the player's own music or podcast | `USAGE_GAME` → `STREAM_MUSIC` (media volume) **and never call `requestAudioFocus()`** — apps that never request focus cannot cause ducking (§Key Finding 2). Recommend a build gate (§Pattern 7) |
</phase_requirements>

---

## Summary

This phase needs no new dependency. Everything it requires is Android platform API (`android.media.SoundPool`, `android.media.AudioAttributes`) plus ~12 committed CC0 audio files. The two hard requirements — SOUND-06 (media volume, never duck) and SOUND-05 (immediate silence including queued one-shots) — are both satisfied by a single small `@Singleton` player class, and both have a specific, verifiable implementation: set `USAGE_GAME` + `CONTENT_TYPE_SONIFICATION` audio attributes and **never call `requestAudioFocus()`**; own the `soundOn` gate inside the player so muting can `stop()` in-flight streams that a `GameUiState` flag could never reach.

Three findings materially change the plan shape versus what the CONTEXT assumed. First, **D-10 is already satisfied and needs zero work**: the pinned Compose stack (foundation 1.11.3 / material3 1.4.0 / ui 1.11.3) contains no reference to `playSoundEffect`, `SoundEffectConstants`, `soundEffectsEnabled` or `performClick` — Compose does not play the platform click tick at all, so there is nothing to suppress. Second, **D-07's "no pitch cap" collides with a hard engine ceiling**: `SoundPool` clamps playback rate to `[0.5, 2.0]`, one octave. Third, **D-08's undo-steps-down behaviour is free**: the same-peg run index is a pure function of the existing `moveHistory`, so undoing and redoing a jump reproduces its pitch by construction with no new state to keep in sync.

The one genuinely new state the ViewModel needs is a signal for the two events that currently leave no trace: the invalid tap (today an unobservable `else -> Unit` in `onCellTap`) and the undo (today indistinguishable from any other board update). Everything else — jump, removal, selection, win — already has an observable transition that the established `JumpHaptic` idiom can key on.

**Primary recommendation:** Build one `@Singleton SoundPlayer` interface + `SoundPoolPlayer` impl in a new `com.app.pegsolitaire.audio` package, owning the `soundOn` gate and a `SoundPool` configured with `USAGE_GAME`/`CONTENT_TYPE_SONIFICATION` and no audio-focus request; preload 12 OGG Vorbis files from `res/raw` at `App.onCreate` on the existing `@ApplicationScope`; expose it to Compose as `LocalSoundPlayer` provided in `MainActivity` (mirroring how `GameFlowTest` already fakes `LocalHapticFeedback`); trigger every cue from `LaunchedEffect`s keyed on state ordinals, exactly like `JumpHaptic`.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Sample decoding, stream mixing, pitch shifting | Android platform (`SoundPool`/AudioFlinger) | — | Native audio service; never hand-rolled |
| Audio attributes, focus policy, mute-all | App singleton (`SoundPoolPlayer`) | — | SOUND-05/06 are process-wide policy, not screen state. A UI-layer gate cannot stop an already-playing stream |
| `soundOn` persistence | Data layer (`SettingsRepository`) | — | Already exists (SET-01); this phase adds **no** persistence |
| Which cue fires, and when | UI layer (Compose `LaunchedEffect` on state ordinals) | ViewModel (emits the ordinals) | Mirrors the locked `JumpHaptic` precedent: "Haptic emission is a Compose UI concern, so it lives here rather than in the ViewModel" (`GameScreen.kt:205`) |
| Same-peg run index (jump pitch input) | UI layer (`GameViewModel` → `MoveTransition.runIndex`) | — | Derived from existing `moveHistory`; must **not** enter `:domain` (`DomainPurityTest` forbids it, and it is a presentation concern) |
| Star-pop sequencing | UI layer (`ResultsScreen`/`ResultsCelebration`) | ViewModel (play-once latch) | Timing must reference `ClayAnim.STAR_STAGGER_MS`, which is a Compose motion token |
| Asset files | Build resources (`res/raw`) | — | Typed `R.raw.*` IDs; avoids the magic-string paths that `assets/` would force (CLAUDE.md no-magic-strings) |

**Nothing in this phase belongs to `:domain`, the data layer, or `..platform..`.** The `platform` package's own KDoc defines it as the Play/AdMob SDK and foreground-Activity seam; audio is neither, so a new sibling package keeps that definition true.

---

## Project Constraints (from CLAUDE.md)

| Directive | Consequence for this phase |
|-----------|---------------------------|
| **No magic numbers or strings in logic/UI — split by scope** | Every pitch step, base rate, volume, and stagger becomes a named `const` in a scoped object (e.g. `audio/SoundParams.kt`). detekt `MagicNumber` is **active** and only whitelists `-1, 0, 1, 2` (`config/detekt/detekt.yml:22-26`) — a `1.0595f` semitone ratio or a `0.35f` volume WILL fail the build if inlined. |
| **Compose state hoisting; state owned by ViewModels** | Sound triggers key on hoisted `GameUiState`/`ResultsUiState` values, never on composable-internal state. `GameScreen`/`ResultsScreen` stay stateless. |
| **Dimens resource file for all UI dimensions — no hardcoded dp/sp** | Not applicable: audio has no dp/sp. Durations/ratios go in a scoped constants object, following the existing precedent that `ClayAnim`/`GameAnim` hold "unitless Int ms and Float scales" outside `dimens.xml`. |
| **detekt and ktlint configured and passing — a build gate** | `./gradlew check` must stay green *and offline*. Add nothing to the dependency graph. |
| **Clean Architecture; game logic isolated as pure Kotlin with no Android deps** | `DomainPurityTest` (ArchUnit, bytecode) fails on any `android..`/`androidx..` reference from `..domain..`. The run-index derivation therefore lives in `GameViewModel`, not `RulesEngine`. |
| **Gitflow strictly — `feature/*` off `develop`** | Phase branch off `develop`; the CONTEXT notes Phase 7 runs in parallel with Phase 6. |

Additional repo-local gates the plan must respect:
- `dpSpLiteralCheck` — file scan over `**/ui/**` Kotlin sources; irrelevant to audio but will scan any new `ui/` file.
- `adIdLiteralCheck` — scans `src/main/{java,kotlin,res}` for `ca-app-pub-`; harmless.
- `PlatformBoundaryTest` — `..ui..`, `..domain..`, `..data..` may not depend on `android.app.Activity`. `SoundPool` needs only an **Application** `Context`, so this is satisfiable; do **not** reach for `LocalActivity`.
- `DoNotIncludeAndroidTestOutput` import option is required on any new ArchUnit test class.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `android.media.SoundPool` | Platform (API 1; Builder API 21) | Low-latency one-shot SFX playback with per-stream pitch | "SoundPool is typically used to play low-latency UI or game sounds… pre-loaded into memory, played with low latency since there is no CPU load and decompressing during playback." `play()`/`setRate()` expose the rate parameter the D-05 pitch ladder needs. [CITED: developer.android.com/reference/android/media/SoundPool] |
| `android.media.AudioAttributes` | Platform (API 21) | `USAGE_GAME` + `CONTENT_TYPE_SONIFICATION` routing | `USAGE_GAME` maps to `STREAM_MUSIC` — i.e. the system **media** volume slider, which is exactly SOUND-06's first half. [CITED: source.android.com/docs/core/audio/attributes] |
| OGG Vorbis assets in `res/raw` | n/a | The ~12 sound files | `.ogg` is in aapt2 9.1.0's built-in no-compress list, so `SoundPool.load(context, R.raw.x, 1)` (which needs an uncompressed file descriptor) works with **zero** build configuration. [VERIFIED: `strings aapt2-9.1.0-14792394-osx` — see §Package Legitimacy Audit] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Robolectric `ShadowSoundPool` | 4.16 (already on `testImplementation`) | JVM-side assertion of *which* sound played at *which* rate | The one test that proves the pitch ladder end-to-end. `getResourcePlaybacks(resId)` returns `Playback` records carrying `soundId, leftVolume, rightVolume, priority, loop, rate`. [CITED: robolectric.org/javadoc/4.16/org/robolectric/shadows/ShadowSoundPool.html] |
| Compose UI Test + Robolectric | BOM 2026.06.00 / 4.16 (already present) | Screen-level cue assertions | `GameFlowTest` already proves the pattern: it swaps `LocalHapticFeedback` for a recording fake via `CompositionLocalProvider` (`GameFlowTest.kt:247`). A `LocalSoundPlayer` gets the same treatment for free. |
| Truth / Turbine / MockK | 1.4.4 / 1.2.1 / 1.14.2 (already present) | Assertions, `StateFlow` emissions, fakes | Standard for this repo's ViewModel tests. |
| ffmpeg | 8.0 (local, `libvorbis`) | Trim / normalise / convert the downloaded CC0 files | Executor-side asset prep only — never a build step. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `SoundPool` | `MediaPlayer` | One decoder instance per sound, ~100 ms+ start latency, no pitch control before API 23, awkward for overlapping one-shots. Wrong tool for 200 ms pops. |
| `SoundPool` | `AudioTrack` / Oboe / AAudio | Lower latency, but you hand-roll mixing, voice-stealing, resampling and pitch shifting. "SoundPool is slower than AudioTrack, but the point of SoundPool is that it has a much friendlier interface and for most circumstances game developers should use this one." [CITED: developer.android.com/games/sdk/oboe/low-latency-audio ecosystem guidance] A grid puzzle does not need sub-20 ms latency. |
| `SoundPool` | AndroidX Media3 / ExoPlayer | Streaming/playback-session engine. Massive dependency, adds ~1 MB+, brings a `MediaSession` and focus model this phase specifically wants to avoid. |
| OGG Vorbis | 16-bit PCM WAV | WAV also in aapt2's no-compress list and decodes fastest, but ~5× the APK bytes. Keep WAV as the per-file fallback if a specific OGG misbehaves — both formats are equally supported here. |
| OGG Vorbis | Opus (`.opus`, or Opus-in-Ogg) | **Do not.** `.opus` is NOT in aapt2's no-compress list (verified), and Ogg/Opus decode support is not guaranteed at `minSdk 24`. |
| `res/raw` (typed `R.raw.*`) | `assets/` + `AssetFileDescriptor` | `assets/` forces string paths — magic strings, banned by CLAUDE.md — and loses the resource shrinker's static reachability analysis. |

**Installation:** none. No Gradle catalog entry, no dependency, no plugin. This phase adds only Kotlin files and binary resources.

---

## Package Legitimacy Audit

**This phase installs no external packages.** No entry is added to `gradle/libs.versions.toml`, no `implementation(...)` line changes, and no npm/PyPI/crates package is involved. `slopcheck` is not installed locally and has no Maven backend regardless (the same conclusion Phase 6 reached — see `06-RESEARCH.md § Package Legitimacy Audit`).

The supply-chain surface this phase *does* introduce is **binary audio assets downloaded from the internet and committed to the repo**. Treat it as such:

| Artifact | Source | License | Verification required before commit |
|----------|--------|---------|-------------------------------------|
| Anchor SFX pack (recommended: Kenney *Interface Sounds*, 100 sounds) | `kenney.nl/assets/interface-sounds` | **CC0 1.0** — "All game assets on the asset pages are public domain licensed (CC0)"; attribution not required; Kenney's *logo* is excluded [CITED: kenney.nl/support] | (1) download over HTTPS; (2) `unzip -l` and confirm the archive contains **only** audio + text/licence files — no scripts, no executables; (3) `ffprobe` each committed file to confirm it is really Vorbis/PCM audio; (4) record file → source URL → licence in the in-repo CREDITS file (D-01) |
| Gap-fill individual sounds | freesound.org filtered to CC0 | CC0 (per-file — **must be checked per file**, freesound hosts CC-BY and CC-BY-NC too) | Same four steps, plus record the freesound sound ID and uploader |

**Disposition:** Approved with the verification steps above as an explicit plan task. **The licence audit is not optional** — D-01 makes in-repo provenance a deliverable, and a CC-BY file slipping into a CC0 set is the realistic failure mode here.

---

## Architecture Patterns

### System Architecture Diagram

```
   USER GESTURE                    SETTINGS
   (tap peg / tap CTA /         (Settings switch)
    tap Undo / illegal tap)            │
        │                              │ onToggleSound(on)
        ▼                              ▼
  ┌───────────────┐            ┌──────────────────────┐
  │  GameScreen   │            │ SettingsViewModel    │
  │  ResultsScreen│            └──────────┬───────────┘
  │  HomeScreen   │                       │ setSoundOn()
  └───────┬───────┘                       ▼
          │ onCellTap / onUndo   ┌──────────────────────┐
          ▼                      │ SettingsRepository   │
  ┌───────────────┐              │  (DataStore)         │
  │ GameViewModel │              └──────────┬───────────┘
  │  moveHistory  │                         │ soundOn: Flow<Boolean>
  └───────┬───────┘                         │
          │ StateFlow<GameUiState>          │  (collected in @ApplicationScope)
          │  · animating.id + .runIndex     │
          │  · undoTick                     │
          │  · rejectedTapTick              │
          │  · status                       │
          ▼                                 ▼
  ┌──────────────────────┐        ┌───────────────────────────────┐
  │ LaunchedEffect(key)  │───────▶│  SoundPlayer (interface)      │
  │  — one per cue,      │ play() │  ↓ @Binds                     │
  │    JumpHaptic idiom  │        │  SoundPoolPlayer (@Singleton) │
  └──────────────────────┘        │   ├ mute gate ── soundOn      │
          ▲                       │   ├ stream-id ring buffer     │
          │ LocalSoundPlayer      │   └ rate(runIndex) ladder     │
  ┌───────┴────────┐              └───────────────┬───────────────┘
  │ MainActivity   │                              │
  │ (@AndroidEntry │                              ▼
  │  Point, injects│              ┌───────────────────────────────┐
  │  the player)   │              │  android.media.SoundPool      │
  └────────────────┘              │  AudioAttributes:             │
                                  │    USAGE_GAME                 │
  ┌────────────────┐  preload()   │    CONTENT_TYPE_SONIFICATION  │
  │ App.onCreate   │─────────────▶│  ── NO requestAudioFocus() ── │
  │ @ApplicationSc.│              └───────────────┬───────────────┘
  └────────────────┘                              │ load(R.raw.*)
                                                  ▼
                                  ┌───────────────────────────────┐
                                  │ res/raw/*.ogg  (12 CC0 files) │
                                  └───────────────────────────────┘

  SOUND-06 boundary: the player writes to STREAM_MUSIC and never touches
  AudioManager focus ⇒ the user's podcast is never ducked or interrupted.
  SOUND-05 boundary: the gate + stream-id buffer live inside the player,
  so a mute reaches streams the UI layer cannot see.
```

### Recommended Project Structure

```
app/src/main/
├── java/com/app/pegsolitaire/
│   ├── audio/
│   │   ├── SoundPlayer.kt        # interface + SoundCue enum(s) — the seam
│   │   ├── SoundPoolPlayer.kt    # @Singleton impl: attributes, gate, ladder
│   │   ├── SoundParams.kt        # scoped constants (rates, volumes, staggers)
│   │   └── LocalSoundPlayer.kt   # CompositionLocal + withClickSound() helper
│   └── di/
│       └── AudioModule.kt        # @Binds SoundPlayer -> SoundPoolPlayer
└── res/raw/                       # 12 lowercase .ogg files
docs/audio/CREDITS.md              # D-01 licence provenance
```

**On package placement:** a new top-level `audio` package is preferred over `platform/audio`. `PlatformModule`'s KDoc explicitly scopes `..platform..` to the Play/AdMob SDK swap points and the foreground-Activity seam; folding audio in there muddies a boundary that three ArchUnit rules depend on. Binding in a new `AudioModule` (rather than `PlatformModule`) keeps that separation legible.

### Pattern 1: One-shot cue as a `LaunchedEffect` keyed on a state ordinal — the `JumpHaptic` idiom

**What:** A small private composable takes a monotonic ordinal from `UiState` and fires exactly one side effect per new value. Recomposition cannot re-fire it because the key has not changed.

**When to use:** Every cue in this phase. It is the codebase's established, documented answer to "per-event side effect from state" and the CONTEXT names it as the anchor.

**Existing code being mirrored** (`GameScreen.kt:220-231`):

```kotlin
@Composable
private fun JumpHaptic(animatingMoveId: Int?, hapticsOn: Boolean) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(animatingMoveId) {
        if (animatingMoveId != null && hapticsOn) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
}
```

**Sound analogue:**

```kotlin
@Composable
private fun JumpSound(transition: MoveTransition?) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(transition?.id) {
        val t = transition ?: return@LaunchedEffect
        sound.playJump(runIndex = t.runIndex)   // pitched, D-05..D-08
        sound.playRemoval()                     // layered second cue, SOUND-02
    }
}
```

Note the deliberate asymmetry with `JumpHaptic`: **there is no `soundOn` parameter.** Unlike `hapticsOn`, the sound gate must live inside the player (see Pattern 6), so `soundOn` never needs to enter `GameUiState` at all. That is a simplification, not an oversight — call it out in the plan so a reviewer does not "fix" it by copying the `hapticsOn` collection.

### Pattern 2: Same-peg run index as a pure function of `moveHistory` (D-05..D-08 for free)

**What:** The jump-pitch input is not stored state. It is computed from the move list that `GameViewModel` already maintains.

```kotlin
/** How many same-peg jumps precede the move at [index] (0 = first jump of a run, D-06). */
private fun runIndexAt(history: List<Move>, index: Int): Int {
    var run = 0
    var i = index
    while (i > 0 && history[i].from == history[i - 1].to) { run++; i-- }
    return run
}
```

**Why this matters:** D-08 ("undo steps the ladder back down one… undoing a run jump then redoing it replays the same pitch it had before") is satisfied *by construction*. `onUndo()` already does `moveHistory.removeAt(moveHistory.lastIndex)`; a shorter history yields the same run index when the move is replayed. There is nothing to reset, nothing to desynchronise, and no new invariant to test beyond the function itself.

**Plumbing:** add one field to the existing render instruction rather than inventing a channel.

```kotlin
@Immutable
data class MoveTransition(
    val from: Position,
    val to: Position,
    val removed: Position,
    val id: Int,
    val runIndex: Int,   // NEW — jump-pitch ladder step (D-05/D-06)
)
```

`MoveTransition` lives in `ui/game/`, is already `@Immutable`, and is already the per-jump render instruction keyed by `id`. It is the correct home.

**Rate mapping** (pure, unit-testable, no Android):

```kotlin
object SoundParams {
    /** Base playback rate — the sample's own pitch (SoundPool 1.0 = original frequency). */
    const val JUMP_BASE_RATE = 1.0f
    /** One equal-tempered semitone per rung: 2^(1/12). */
    const val JUMP_SEMITONE_STEP = 1.05946f
    /** SoundPool hard ceiling — rates above this are clamped by the engine anyway. */
    const val MAX_RATE = 2.0f
    const val MIN_RATE = 0.5f
}

fun jumpRate(runIndex: Int): Float =
    (SoundParams.JUMP_BASE_RATE * SoundParams.JUMP_SEMITONE_STEP.pow(runIndex))
        .coerceIn(SoundParams.MIN_RATE, SoundParams.MAX_RATE)
```

Clamping explicitly (rather than letting the engine do it silently) makes the ceiling a documented, testable decision instead of a surprise. See Open Question 1 for the D-07 tension.

### Pattern 3: Signalling the two events that currently leave no state trace

`GameViewModel` today has **no observable signal** for an invalid tap or an undo:

- Invalid tap is `else -> Unit` in `onCellTap` (`GameViewModel.kt:496`) — literally nothing happens.
- Undo produces a state update indistinguishable from any other board change; `canUndo` stays `true` for all but the last undo, and `animating` is set to `null` (as it already is when settled).

**Recommendation:** two monotonic `Int` ordinals on `GameUiState`, matching the `MoveTransition.id` idiom exactly:

```kotlin
val undoTick: Int = 0,          // ++ on each undo that actually removed a move
val rejectedTapTick: Int = 0,   // ++ on each tap that changed nothing (SOUND-02 invalid tap)
```

Increment `rejectedTapTick` in the `else` branch of `onCellTap`; increment `undoTick` in `onUndo` after the history actually shrinks (not on the `RewardOutcome.Declined` early return, and not on an empty history).

**Deselect is a separate decision:** re-tapping the selected peg (`state.selected == position`) is a *successful* deselect, not a rejection. It should not fire the invalid-tap sound. Whether it gets the selection sound, a softer variant, or silence is Claude's discretion — recommend a quiet reuse of the selection cue.

**Selection and win need no new field** — `state.selected` transitions `null → Position`, and `state.status` transitions to `Won`, both already observable:

```kotlin
LaunchedEffect(state.selected) { if (state.selected != null) sound.playSelect() }
LaunchedEffect(state.status)   { if (state.status == GameStatus.Won) sound.playWin() }  // D-14
```

*(Alternative considered: a single unified `soundEvent: SoundCue?` + `soundEventId: Int` pair replacing all ordinals. It is tidier in the abstract but introduces a second mechanism alongside the existing `animating.id`, and hides which cue fires behind a sealed hierarchy. Two `Int` fields are more faithful to the codebase. Planner's call.)*

### Pattern 4: Star-pop sequencing against the existing celebration (SOUND-03, D-13/D-15/D-16)

The Results celebration timing already exists and must be *referenced*, not duplicated (CONTEXT: "star-pop timing should read these, not re-declare them").

`ResultsScreen.EarnedStar` (`ResultsScreen.kt:221`) currently does:
```kotlin
val scale = rememberPopIn(delayMillis = order * ClayAnim.STAR_STAGGER_MS)   // 120 ms
```
and `rememberPopIn` lands on its final value immediately when `rememberAnimationsEnabled()` is false.

```kotlin
@Composable
private fun StarPopSounds(earned: Int, play: Boolean) {
    val sound = LocalSoundPlayer.current
    val animationsEnabled = rememberAnimationsEnabled()
    LaunchedEffect(play, earned) {
        if (!play) return@LaunchedEffect                       // D-16 play-once latch
        val stagger =
            if (animationsEnabled) ClayAnim.STAR_STAGGER_MS    // in step with the pops
            else SoundParams.STAR_POP_STAGGER_MS               // D-13 reduced-motion fallback
        repeat(earned) { i ->
            sound.playStarPop(step = i)                        // D-15 ladder prefix
            delay(stagger.toLong())
        }
    }
}
```

- **D-15 is a pure function**: `starPopRate(step) = BASE * STEP^step`. A 2-star result plays steps 0–1; only 3 stars ever reaches step 2. Assert this directly in a JVM test.
- **D-16 play-once**: mirror `recordSolvedPlayOnce()`. Add a `SavedStateHandle` flag (e.g. `RESULTS_CELEBRATED_KEY = "results_celebrated"`) in `ResultsViewModel` and expose `ResultsUiState.playCelebration: Boolean`. The `SavedStateHandle` survives both configuration change (same instance) and process-death restore (new instance, same handle) — which is exactly the two re-entry paths D-16 names. Do **not** use a `remember {}` in the composable; it survives neither.
- **Note the D-13 arithmetic:** `ClayAnim.STAR_STAGGER_MS` is **120**, while D-13 specifies "~150 ms". Recommend either accepting 120 for both paths (simplest, and 120 is within "~150") or declaring `SoundParams.STAR_POP_STAGGER_MS = 150` for the animations-off path only. Flag for the user at verification (D-04 audit).

### Pattern 5: Selective button coverage (SOUND-04, D-09/D-11)

D-09 requires *selective* coverage — navigational CTAs get the sound; toggles and back navigation stay silent. The temptation is to bolt it onto the shared `Modifier.claySquish()`, which every tappable clay surface already uses. **Do not** — that would blanket-apply the sound to exactly the controls D-09 excludes.

Use an explicit, greppable per-callsite wrapper:

```kotlin
@Composable
fun withClickSound(cue: UiSoundCue, onClick: () -> Unit): () -> Unit {
    val sound = LocalSoundPlayer.current
    return remember(cue, onClick, sound) { { sound.play(cue); onClick() } }
}

// callsite, e.g. ResultsActions primary CTA
Button(onClick = withClickSound(UiSoundCue.Forward, onContinue)) { … }
// dialog dismiss (D-11 softer pair)
TextButton(onClick = withClickSound(UiSoundCue.Dismiss, onDismiss)) { … }
```

Coverage set per D-09: `HomeScreen` mode tiles + Continue chips, `ClassicCatalogScreen` board cards, `ResultsActions` (all), `RestartConfirmDialog` confirm (`Forward`) and dismiss (`Dismiss`). Explicitly **not**: `HintToggleControl`, the Settings switches (except D-12), and every top-bar back nav. `UndoControl`/`HintControl` keep their SOUND-02 event cues instead.

### Pattern 6: The mute gate lives inside the player (SOUND-05)

```kotlin
@Singleton
class SoundPoolPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) : SoundPlayer {

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)                       // -> STREAM_MUSIC (SOUND-06)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setMaxStreams(SoundParams.MAX_STREAMS)
        .setAudioAttributes(attributes)
        .build()
    // NOTE: no AudioManager, no requestAudioFocus() — see SOUND-06.

    @Volatile private var enabled = true
    private val activeStreams = ArrayDeque<Int>(SoundParams.MAX_STREAMS)

    init {
        scope.launch {
            settingsRepository.soundOn.collect { on ->
                enabled = on
                if (!on) silenceNow()          // SOUND-05: reaches queued/in-flight streams
            }
        }
    }

    private fun silenceNow() {
        synchronized(activeStreams) {
            activeStreams.forEach(pool::stop)   // stop() on a finished id is a harmless no-op
            activeStreams.clear()
        }
        pool.autoPause()                        // belt-and-braces: halts anything untracked
    }
}
```

**Why the gate cannot live in `GameUiState`:** a `soundOn` flag in UI state can only prevent *future* `play()` calls. SOUND-05 explicitly requires silencing "already-queued one-shots" — that needs a handle on the live stream IDs, which only the player has. Note there is **no `stopAll()`** on `SoundPool`; `autoPause()` (halts all active streams, resumable only via `autoResume()`, which we never call) plus tracked-ID `stop()` is the complete answer.

### Pattern 7: Make SOUND-06 a permanent build gate, not a one-time check

This repo already has two custom Gradle file-scan gates in `build-logic/src/main/kotlin/standards.gradle.kts` (`dpSpLiteralCheck`, `adIdLiteralCheck`) wired into `check`. SOUND-06's whole implementation is a *negative*: "never call `requestAudioFocus`". Negatives rot silently. A third gate in the identical shape makes it permanent:

```kotlin
// Fails on any requestAudioFocus / abandonAudioFocus reference in src/main (SOUND-06).
// Sound must never duck the player's own music: the app deliberately holds no focus.
val audioFocusCheck = tasks.register("audioFocusLiteralCheck") { … }
tasks.named("check").configure { dependsOn(audioFocusCheck) }
```

This is high-leverage and cheap: it turns "a player's podcast is never ducked" from a manual UAT observation into something CI re-proves on every commit. **Strongly recommended.**

### Anti-Patterns to Avoid

- **Requesting audio focus "to be polite."** It is the single change that would break SOUND-06. Also note: on **Android 15+ (API 35+) an app that is neither the top app nor running a foreground service cannot obtain focus at all** — `requestAudioFocus()` returns `AUDIOFOCUS_REQUEST_FAILED`. [CITED: developer.android.com/media/optimize/audio-focus]
- **Putting the mute gate only in `GameUiState`.** Cannot satisfy SOUND-05's "already-queued one-shots".
- **Bolting the click sound onto `claySquish()`.** Blanket-applies to toggles and back nav, violating D-09.
- **Calling `play()` in the same breath as `load()`.** Logs `sample N not READY` and is silently inaudible — the exact failure mode "the first tap of a session is never silent" forbids.
- **Loading assets by name via `resources.getIdentifier()`.** Defeats `isShrinkResources = true` (release builds strip resources with no static reference) *and* reintroduces magic strings.
- **Storing the pitch ladder as mutable state.** D-08's invariant then needs manual maintenance across undo/restart/resume; deriving it from `moveHistory` makes it impossible to desynchronise.
- **Adding audio anything to `:domain`.** `DomainPurityTest` fails the build on any `android..`/`androidx..` reference from `..domain..`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Pitch shifting | Manual resampling / rate conversion | `SoundPool.play(..., rate)` | Native, per-stream, free. Rate is a first-class play parameter. |
| Overlapping one-shots + voice stealing | Your own stream pool / manual mixing | `SoundPool.Builder().setMaxStreams(n)` | "If the maximum number of streams is exceeded, SoundPool will automatically stop a previously playing stream based first on priority and then by age within that priority" [CITED: SoundPool docs] |
| Media-volume routing | `AudioManager.setStreamVolume` / stream-type juggling | `AudioAttributes.USAGE_GAME` | Maps to `STREAM_MUSIC` automatically; touching stream volume directly is user-hostile and will not survive review. |
| Not ducking other apps | A ducking-avoidance workaround | Simply never call `requestAudioFocus()` | Ducking is *initiated by a focus request*. No request ⇒ no duck. Doing nothing is the correct implementation. |
| APK-uncompressed audio | `androidResources { noCompress += "ogg" }` | Nothing — it is already the aapt2 default | Verified against the pinned aapt2 9.1.0 binary. Adding it is harmless but is cargo cult. |
| Per-event side effects from state | A new event bus / `SharedFlow` per cue | `LaunchedEffect(ordinal)` — the `JumpHaptic` idiom | Already proven, already tested, already documented in this codebase. |
| Play-once across process death | `remember {}` / a `Boolean` field | `SavedStateHandle` flag (`recordSolvedPlayOnce` idiom) | Only `SavedStateHandle` survives both config change and process-death restore — the two paths D-16 names. |
| Same-peg run tracking | A mutable `runLength` counter with reset/undo handling | `runIndexAt(moveHistory, i)` pure function | Removes the entire class of desync bugs D-08 would otherwise create. |

**Key insight:** almost every requirement in this phase is satisfied by *using the platform correctly and doing nothing extra*. SOUND-06's implementation is an omission. SOUND-05's is one `stop()` loop. D-08's is a derivation. D-10's is nothing at all. The plan's real work is asset curation, constant tuning by ear, and wiring ~8 `LaunchedEffect`s — not engineering.

---

## Key Findings

### Key Finding 1 — SOUND-06 is satisfied by two decisions, both verifiable

1. `AudioAttributes.USAGE_GAME` maps to `AudioSystem.STREAM_MUSIC`, i.e. the system **media** volume slider. [CITED: source.android.com/docs/core/audio/attributes]
2. Automatic ducking is triggered only when a *second* app requests focus with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, and Android 12+ forced fade-out applies only to apps that *hold* `AUDIOFOCUS_GAIN`. An app that never requests focus can neither be ducked nor cause ducking. [CITED: developer.android.com/media/optimize/audio-focus]

Android 16 (API 36, this app's `targetSdk`) introduces **no** audio-focus, ducking, `AudioAttributes` or `SoundPool` behaviour changes. [CITED: developer.android.com/about/versions/16/behavior-changes-all]

The review phrase from the CONTEXT — *"start a podcast, play a full level — the podcast never dips"* — will hold, and the `audioFocusLiteralCheck` gate (Pattern 7) keeps it holding.

### Key Finding 2 — `SoundPool` playback rate is hard-clamped to `[0.5, 2.0]`; D-07's "no cap" cannot be literal

"The playback rate range is 0.5 to 2.0… Values outside this range are clamped." [CITED: developer.android.com/reference/android/media/SoundPool] That is exactly **one octave** of headroom — 12 equal-tempered semitones.

Concrete headroom data, computed from this repository's own committed canonical solutions:

| Recorded solution | Moves | Longest same-peg run |
|---|---|---|
| `english33.txt` | 31 | **2** |
| `european37.txt` | 35 | **3** |
| `asymmetric39.txt` | 37 | **3** |
| `wiegleb45.txt` | 43 | **3** |

[VERIFIED: computed locally over `domain/src/test/resources/golden/solutions/*.txt`]

With a semitone step, the ladder needs 12 rungs before it hits the ceiling and canonical play uses at most 3. A player can hand-construct longer chains, but the ceiling is only audible on genuinely exceptional runs — which is arguably the "audibly silly" outcome D-07 wanted anyway. See Open Question 1.

### Key Finding 3 — D-10 requires no work: Compose never plays the platform click tick

Scanned the exact artifacts the pinned Compose BOM 2026.06.00 resolves, from the local Gradle cache:

| Artifact | Kotlin sources scanned | `playSoundEffect` / `SoundEffectConstants` / `soundEffectsEnabled` / `performClick` |
|---|---|---|
| `androidx.compose.foundation:foundation-android:1.11.3` | all | **0 matches** |
| `androidx.compose.material3:material3-android:1.4.0` | 249 | **0 matches** |
| `androidx.compose.ui:ui-android:1.11.3` | 372 | **0 matches** |

[VERIFIED: `unzip` + `grep` over the `-sources.jar` files in `~/.gradle/caches/modules-2/files-2.1/`]

Compose's `clickable`/`Button` do not emit the Android system touch-sound. **There is no double-click to suppress, and no `soundEffectsEnabled` to turn off** — Compose exposes no such API. D-10 should become a one-line verification note in the plan ("confirmed: Compose emits no platform tick; nothing to suppress") plus one manual UAT check with OS touch sounds enabled, not an implementation task.

### Key Finding 4 — `.ogg` and `.wav` are already uncompressed in the APK; `.opus` is not

`SoundPool.load(Context, resId, priority)` requires an uncompressed raw resource (it opens a file descriptor); a compressed one throws `FileNotFoundException: This file can not be opened as a file descriptor; it is probably compressed`.

The pinned aapt2 (`aapt2-9.1.0-14792394-osx`) built-in no-compress extension list, extracted from the binary, is:

```
.3g2 .3gp .3gpp .3gpp2 .aac .amr .awb .gif .imy .jet .jpeg .jpg .m4a .m4v
.mid .midi .mkv .mp2 .mp3 .mp4 .mpeg .mpg .ogg .png .rtttl .smf .wav .webm .wma .wmv .xmf
```

[VERIFIED: `strings -a` over the aapt2 binary in the Gradle cache]

`.ogg` and `.wav` are both present; `.opus` is **absent**. No `androidResources { noCompress }` configuration is needed for OGG or WAV, and Opus must not be used.

---

## Common Pitfalls

### Pitfall 1: The first tap of the session is silent
**What goes wrong:** `SoundPool.load()` is asynchronous. `play()` before decoding completes returns stream ID `0`, logs `sample N not READY`, and produces nothing.
**Why it happens:** Decoding runs on the MediaPlayer service; the call returns a sound ID immediately, before the sample exists.
**How to avoid:** Call `preload()` from `App.onCreate()` on the existing `@ApplicationScope` `CoroutineScope` (off the main thread — do not block cold start), and register `setOnLoadCompleteListener` to track readiness. Guard `play()` on the sound being loaded. Optionally warm the audio path with one volume-0 play after the first load completes.
**Warning signs:** Logcat `SoundPool: sample <n> not READY`; the very first jump of a fresh launch is silent while every later one works. This is precisely the success criterion the requirement calls out, so make it an explicit verification step.
[CITED: SoundPool docs; corroborated across multiple community sources]

### Pitfall 2: Monotonic ordinals reset on restart and re-fire a phantom sound
**What goes wrong:** `resetToInitial()`, `loadChallengeBoardIfNeeded()`, `applyRestored()` and `installClassicBoard()` all build a **fresh** `GameUiState.initial(board)`. A new `undoTick`/`rejectedTapTick` of `0` is a *changed key* for a `LaunchedEffect` that was sitting at `3` — so the effect re-runs and plays a sound nobody triggered.
**Why it happens:** The existing code deliberately re-copies `showHints`/`hapticsOn` forward for exactly this class of reason (`GameViewModel.kt:818-825`); the new ordinals need the same treatment.
**How to avoid:** Carry the ordinals forward across every state rebuild (`.copy(undoTick = …, rejectedTapTick = …)`), **and** guard the effect body with `if (tick > 0)`. Both, not either.
**Warning signs:** A pop on restart, on Challenge board load, or on Continue-resume.

### Pitfall 3: The D-12 enable-blip is swallowed by its own gate
**What goes wrong:** `onToggleSound(true)` writes through DataStore asynchronously. The player's `enabled` flag only flips when the collected `soundOn` flow re-emits — which is *after* the write lands. A confirmation blip played immediately hits `enabled == false` and is dropped; played from the flow collector, it also fires on the **initial** emission at Settings-screen load, which D-12 explicitly forbids.
**How to avoid:** Give the player one documented, single-purpose bypass — e.g. `SoundPlayer.playSoundEnabledConfirmation()` — that ignores the gate, and call it **only** from `SettingsViewModel.onToggleSound(on = true)`. Because `SettingsScreen` is stateless and `onToggleSound` is a user event, "only on user-initiated enable" is then true by construction, not by a flag.
**Warning signs:** No blip at all when enabling; or a blip every time Settings is opened.

### Pitfall 4: `SoundPool` has no `stopAll()`
**What goes wrong:** Reaching for a non-existent API, or assuming `release()` is the only way to silence everything (it is, but it also destroys the loaded samples, reintroducing Pitfall 1).
**How to avoid:** Track stream IDs returned by `play()` in a bounded ring buffer and `stop()` each, plus `autoPause()` as a backstop. Never call `autoResume()`. Never `release()` on mute.
[CITED: SoundPool docs — `autoPause`/`autoResume`/`stop`/`release` only]

### Pitfall 5: Samples too long for `SoundPool`
**What goes wrong:** SoundPool decodes each sound to raw 16-bit PCM in memory, with "each decoded sound internally limited to one megabyte storage" — roughly 5.6 s at 44.1 kHz stereo.
**How to avoid:** Keep every file well under 1 s, mono. Twelve 300 ms mono 44.1 kHz clips decode to ~26 KB each — ~320 KB total, negligible. Trim hard in ffmpeg; a candy pop should be 80–250 ms.
**Warning signs:** Truncated playback of the longest sound (typically the win stinger).
[CITED: SoundPool docs]

### Pitfall 6: Release-build resource shrinking strips `res/raw`
**What goes wrong:** `isMinifyEnabled = true` + `isShrinkResources = true` are both on for release (`app/build.gradle.kts`). Resources with no static reference are removed. Direct `R.raw.jump_pop` references are traced and kept; anything reached via `getIdentifier()` is not.
**How to avoid:** Reference every sound as a compile-time `R.raw.*` constant (e.g. a `when` over a sealed cue type, or an `@RawRes Int` on an enum). Then verify empirically using the technique this repo already used for `font_certs.xml`: build the release APK and inspect with `aapt2 dump resources`. That precedent is documented in `app/build.gradle.kts`'s release block.
**Warning signs:** All sounds work in debug, all sounds silent in a minified release build — the worst possible discovery timing.

### Pitfall 7: `res/raw` filename constraints
**What goes wrong:** Kenney/freesound filenames contain spaces, capitals and hyphens (`Click Soft 01.ogg`). Android resource names must be `[a-z0-9_]` only, and cannot start with a digit.
**How to avoid:** Rename on commit to a documented scheme (`sfx_jump.ogg`, `sfx_peg_remove.ogg`, `sfx_star_pop.ogg`, `sfx_btn_forward.ogg`, `sfx_btn_dismiss.ogg`, …) and record the original filename in the CREDITS file so provenance survives the rename (D-01).

### Pitfall 8: The 12-sound set collides at `maxStreams`
**What goes wrong:** Too low a `maxStreams` and the win stinger gets voice-stolen by star pops.
**How to avoid:** Worst realistic concurrency is win + 3 star pops (4), or jump + removal + button (3). `MAX_STREAMS = 8` is comfortable and costs nothing. Give the win/celebration cues a higher `priority` than jump cues so voice stealing, if it ever happens, sacrifices the right thing.

---

## Runtime State Inventory

Not applicable — this is an additive feature phase, not a rename/refactor/migration. No stored data, live service config, OS registration, secret, or build artifact carries a string this phase changes.

*(One adjacent item worth stating explicitly since it could look like migration work: the `soundOn` DataStore key `"sound_on"` already exists and is unchanged. This phase consumes it; it does not migrate it.)*

---

## Code Examples

### Verified: Compose-side cue firing (mirrors the existing `JumpHaptic`)

```kotlin
// Source: app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt:220 (existing, adapted)
@Composable
private fun JumpSound(transition: MoveTransition?) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(transition?.id) {
        val t = transition ?: return@LaunchedEffect
        sound.playJump(runIndex = t.runIndex)
        sound.playRemoval()
    }
}
```

### Verified: providing the player to Compose (mirrors the existing test seam)

`MainActivity` is already `@AndroidEntryPoint`, so field injection works with no new plumbing:

```kotlin
// Source: app/src/main/java/com/app/pegsolitaire/MainActivity.kt (existing, extended)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
                AppTheme { AppNavHost(navController = rememberNavController()) }
            }
        }
    }
}
```

The test seam this buys is already proven in this repo — `GameFlowTest` swaps `LocalHapticFeedback` for a recording fake the same way:

```kotlin
// Source: app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt:247 (existing)
CompositionLocalProvider(LocalHapticFeedback provides haptics) { … }
```

### Verified: asserting the pitch ladder with Robolectric

```kotlin
// Source: robolectric.org/javadoc/4.16 ShadowSoundPool API
val playbacks = shadowOf(soundPool).getResourcePlaybacks(R.raw.sfx_jump)
assertThat(playbacks.map { it.rate })
    .containsExactly(1.0f, 1.05946f, 1.12246f)   // three-jump same-peg run
    .inOrder()
```

`ShadowSoundPool` also provides `wasResourcePlayed(resId)`, `notifyResourceLoaded(resId, success)` (to simulate load completion deterministically) and `clearPlayed()`.

### Verified: the audio attributes that satisfy SOUND-06

```kotlin
// Source: developer.android.com/reference/android/media/AudioAttributes
//         + source.android.com/docs/core/audio/attributes (USAGE_GAME -> STREAM_MUSIC)
val attributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_GAME)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

val pool = SoundPool.Builder()
    .setMaxStreams(SoundParams.MAX_STREAMS)
    .setAudioAttributes(attributes)
    .build()

// Deliberately absent: AudioManager, requestAudioFocus(), abandonAudioFocusRequest().
// SOUND-06 is implemented by this omission.
```

### Asset preparation with the locally available ffmpeg

```bash
# trim silence, normalise, mono, 44.1 kHz, OGG Vorbis q4 (~64 kbps) — small and clean
ffmpeg -i "Click Soft 01.ogg" \
  -af "silenceremove=start_periods=1:start_threshold=-50dB,loudnorm=I=-16:TP=-1.5" \
  -ac 1 -ar 44100 -c:a libvorbis -q:a 4 \
  app/src/main/res/raw/sfx_btn_forward.ogg

# confirm every committed file really is audio (supply-chain check, §Package Legitimacy Audit)
for f in app/src/main/res/raw/*.ogg; do ffprobe -hide_banner "$f" 2>&1 | grep -E "Audio:|Duration"; done
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `SoundPool(maxStreams, streamType, quality)` constructor | `SoundPool.Builder().setAudioAttributes(...)` | API 21 (deprecated constructor) | The old constructor takes a legacy **stream type**; only the Builder accepts `AudioAttributes`. SOUND-06 requires the Builder. |
| Legacy `AudioManager.STREAM_*` stream types | `AudioAttributes` usage/content-type | API 21 | Stream types are legacy-compat only. Use `USAGE_GAME`. |
| Any app may hold audio focus | Android 15+ (API 35+): focus requires being the top app or running a foreground service | API 35 | Irrelevant here *because* we never request focus — but it is another reason not to start. |
| — | Android 16 (API 36): **no** audio behaviour changes | 2026 | Nothing to adapt to. `targetSdk 36` is safe for this phase. |

**Not deprecated / still current:** `SoundPool` itself. It remains the platform's recommended API for short game/UI sounds; Oboe/AAudio exist for sub-20 ms real-time synthesis, which this phase does not need.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| ffmpeg (with `libvorbis`) | Trimming/normalising/converting CC0 assets | ✓ | 8.0 | `afconvert` (macOS, present) |
| `afconvert` | Format conversion fallback | ✓ | macOS built-in | ffmpeg |
| `sox` | (alternative audio processing) | ✗ | — | ffmpeg covers everything needed |
| `curl` | Downloading CC0 packs | ✓ | system | manual browser download |
| `adb` | On-device SOUND-05/06 manual verification | ✓ | Android SDK platform-tools | — |
| Android emulator | Running the build for UAT | ✓ | `emulator-5554` online; AVDs `Pixel`, `Pixel_9`, `Pixel_9a` | — |
| JDK 17 | Build toolchain | ✓ | OpenJDK 17.0.15 | — |
| python3 | Ad-hoc scripting | ✓ | 3.9.6 | — |
| Gradle / AGP 9.1.0 / aapt2 | Build | ✓ | cached, offline-capable | — |

**Missing dependencies with no fallback:** none.

**Missing dependencies with fallback:** `sox` — ffmpeg 8.0 (with both `libvorbis` and `libopus`) covers every conversion, trim and loudness-normalisation step the asset pipeline needs.

**Verification caveat — SOUND-06 needs a real audio scenario.** The connected emulator can play audio, but the review phrase (*"start a podcast, play a full level — the podcast never dips"*) is only convincing with two apps producing audio simultaneously. On the emulator, start any second audio source (a browser tab, a media file) before the game. A **physical device is strongly preferred** for this one check, since audio-focus/ducking policy is enforced by the platform audio policy service and emulator audio routing has historically been the least faithful part of the emulator. Plan this as a manual UAT item with an explicit device note.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + Robolectric 4.16 + Compose UI Test (BOM 2026.06.00) |
| Config file | `app/build.gradle.kts` (deps) + `build-logic/src/main/kotlin/android-app.gradle.kts` (`testOptions.unitTests.isIncludeAndroidResources = true`) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.audio.*" --tests "*Sound*" --offline` |
| Full suite command | `./gradlew check --offline` (detekt + ktlint + dpSpLiteralCheck + adIdLiteralCheck + ArchUnit gates + ~299 JVM tests) |

No `androidTest` source set exists — everything runs JVM-side via Robolectric. Keep it that way.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SOUND-01 | `jumpRate(runIndex)` is a rising semitone ladder, clamped at 2.0 | unit (pure JVM) | `./gradlew :app:testDebugUnitTest --tests "*SoundParamsTest"` | ❌ Wave 0 |
| SOUND-01 | `MoveTransition.runIndex` counts same-peg chains and resets on a different peg (D-06) | unit (VM) | `--tests "*GameViewModelSoundTest"` | ❌ Wave 0 |
| SOUND-01 / D-08 | Undo then redo of the same jump reproduces the identical rate | unit (VM) | `--tests "*GameViewModelSoundTest"` | ❌ Wave 0 |
| SOUND-01 | First tap of a session is audible (load completed before first play) | unit (Robolectric + `ShadowSoundPool.notifyResourceLoaded`) | `--tests "*SoundPoolPlayerTest"` | ❌ Wave 0 |
| SOUND-02 | Jump, removal, selection, undo, invalid tap each fire a distinct cue exactly once | integration (Robolectric + fake `LocalSoundPlayer`) | `--tests "*GameScreenSoundTest"` | ❌ Wave 0 |
| SOUND-02 | Invalid tap is quieter than the jump (relative volume) | unit | `--tests "*SoundParamsTest"` | ❌ Wave 0 |
| SOUND-03 | Win cue fires at `status == Won` on the board, before navigation (D-14) | integration | `--tests "*GameScreenSoundTest"` | ❌ Wave 0 |
| SOUND-03 / D-15 | N-star result plays exactly steps `0..N-1` of the rising ladder | integration | `--tests "*ResultsCelebrationSoundTest"` | ❌ Wave 0 |
| SOUND-03 / D-13 | Pops still sequence when `rememberAnimationsEnabled()` is false | integration | `--tests "*ResultsCelebrationSoundTest"` | ❌ Wave 0 |
| SOUND-03 / D-16 | Re-entering the same Results plays nothing | unit (VM, `SavedStateHandle`) | `--tests "*ResultsViewModelTest"` | ✅ extend existing |
| SOUND-04 | Navigational CTAs fire the button cue; toggles + back nav do not (D-09) | integration | `--tests "*ButtonSoundTest"` | ❌ Wave 0 |
| SOUND-04 / D-11 | Forward and dismiss are different cues | integration | `--tests "*ButtonSoundTest"` | ❌ Wave 0 |
| SOUND-05 | `soundOn → false` stops tracked streams and blocks subsequent plays | unit (Robolectric + `ShadowSoundPool`) | `--tests "*SoundPoolPlayerTest"` | ❌ Wave 0 |
| SOUND-06 | `AudioAttributes` are `USAGE_GAME` + `CONTENT_TYPE_SONIFICATION` | unit (Robolectric) | `--tests "*SoundPoolPlayerTest"` | ❌ Wave 0 |
| SOUND-06 | No source in `src/main` references `requestAudioFocus` | **build gate** | `./gradlew audioFocusLiteralCheck` | ❌ Wave 0 |
| SOUND-06 | A podcast is not ducked during a full level | **manual UAT** (physical device preferred) | — | manual-only: audio-focus/ducking is enforced by the platform audio policy service and cannot be faithfully asserted JVM-side |
| D-12 | Enabling sound plays one blip; opening Settings plays none | unit (VM) | `--tests "*SettingsViewModelSoundTest"` | ❌ Wave 0 |
| D-01 | Every committed `res/raw` file has a CREDITS entry with source + licence | unit (file scan) | `--tests "*AudioCreditsTest"` | ❌ Wave 0 — cheap and makes the licence audit permanent |
| D-02 / aesthetic | The set sounds like candy clay and nothing is a dud | **manual UAT** (D-04) | — | manual-only by explicit user decision |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "*Sound*" --offline`
- **Per wave merge:** `./gradlew check --offline`
- **Phase gate:** full `check` green + the two manual UAT items (podcast test, aesthetic audit) before `/gsd:verify-work`.

### Wave 0 Gaps

- [ ] `app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt` — SOUND-01, SOUND-02 (pure ladder + volume relationships)
- [ ] `app/src/test/java/com/app/pegsolitaire/audio/SoundPoolPlayerTest.kt` — SOUND-01 (preload), SOUND-05, SOUND-06
- [ ] `app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt` — recording fake for `LocalSoundPlayer`, mirroring `GameFlowTest`'s haptics fake
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt` — SOUND-01 run index, D-08 undo symmetry, SOUND-02 ticks
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt` — SOUND-02, SOUND-03 (win at final jump)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsCelebrationSoundTest.kt` — SOUND-03, D-13, D-15
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` — SOUND-04, D-09, D-11
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt` — D-12
- [ ] `app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt` — D-01 provenance
- [ ] `build-logic/src/main/kotlin/standards.gradle.kts` — add `audioFocusLiteralCheck` to `check`
- [ ] Extend `ResultsViewModelTest.kt` with the D-16 play-once assertion

**No framework install is needed** — every library required is already on `testImplementation`.

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Phase adds no auth surface |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | No new permissions; `SoundPool` needs none |
| V5 Input Validation | partial | The only new untrusted-ish input is `runIndex` / `star step`, both derived from already-validated in-process state. Clamp the resulting rate into `[0.5, 2.0]` explicitly rather than relying on the engine's silent clamp — a negative or absurd index must not produce undefined behaviour |
| V6 Cryptography | no | No crypto |
| V14 Configuration / supply chain | **yes** | Binary assets downloaded from the internet and committed. See §Package Legitimacy Audit: HTTPS download, archive content inspection (audio + licence files only, no scripts/executables), `ffprobe` type confirmation, per-file licence recorded in CREDITS |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Malicious/mislicensed binary asset committed from an untrusted download | Tampering / legal | Restrict to CC0 sources with a stated licence (Kenney: CC0, verified); inspect archive contents before extraction; `ffprobe` every committed file; record provenance in CREDITS (D-01) |
| Unbounded playback rate from a derived index | Denial of Service (audio artefacting) | Explicit `coerceIn(MIN_RATE, MAX_RATE)` in `jumpRate`/`starPopRate` |
| Unbounded stream accumulation on rapid tapping | Resource exhaustion | `setMaxStreams(8)` — SoundPool voice-steals by priority then age automatically; the tracked-ID buffer is bounded to the same size |
| Audio played while the app is backgrounded | Annoyance / policy | Cues are driven by `LaunchedEffect`s inside composables, which stop with the composition. No background playback path exists — do not add one |
| New `INTERNET`-dependent code path | Information disclosure | None: this phase is fully offline. Assets are compiled in; no runtime download |

**No new Android permission is required.** Playing to `STREAM_MUSIC` via `SoundPool` needs none.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `SoundPool.stop(streamID)` on an already-finished or invalid stream ID is a harmless no-op rather than throwing | Pattern 6, Pitfall 4 | Mute path could crash. **Mitigate:** the `SoundPoolPlayerTest` mute test exercises exactly this — stop a finished stream — so it is proven in Wave 0, not assumed at runtime |
| A2 | A volume-0 warm-up play meaningfully reduces first-play latency on modern Android | Pitfall 1 | Only a marginal loss; the real fix (preload + load-complete gating) is verified independently. Treat the warm-up as optional polish |
| A3 | `isShrinkResources = true` retains `res/raw` entries referenced by static `R.raw.*` from Kotlin | Pitfall 6 | All sounds silent in release. **Mitigate:** the plan must include an `aapt2 dump resources` check of the shrunk release APK — the exact technique already used and documented in `app/build.gradle.kts` for `font_certs.xml` |
| A4 | Kenney's *Interface Sounds* pack contains sounds that read as "soft candy-clay pops" (D-02) | Standard Stack, §Package Legitimacy Audit | Aesthetic miss. Accepted risk by design: D-04 makes this a verify/UAT audit with swap-out, and D-03 permits borrowing from elsewhere where the anchor pack has no fit |
| A5 | Robolectric's `ShadowSoundPool` records `rate` faithfully enough to assert the ladder | Validation Architecture | The rate assertion would need to move to a manual check. Javadoc confirms `Playback` carries `rate`; the risk is only whether the shadow intercepts the Builder-constructed pool |
| A6 | `SoundPool.load(Context, resId, priority)` internally opens a raw resource file descriptor (hence the compressed-resource failure mode) | Key Finding 4, Pitfall 6 | Low: `.ogg` is uncompressed by default regardless, so the mitigation holds either way |
| A7 | Freesound CC0-filtered results are reliably CC0 at the file level | §Package Legitimacy Audit | Licence contamination. **Mitigate:** per-file licence check is already a required step (D-01), not a spot check |

---

## Open Questions

> **All five questions RESOLVED at plan-phase (2026-08-28).** Q1 → user decision **D-07a** (cap at 2.0× via `coerceIn`, KDoc-documented; recorded in CONTEXT.md). Q2 → `ClayAnim.STAR_STAGGER_MS` (120) when animations on, `SoundParams.STAR_POP_STAGGER_MS = 150` for reduced-motion (plan 07-02/07-04). Q3 → `SoundParams.CELEBRATION_LEAD_IN_MS` constant, tuned at D-04 audit (plan 07-04). Q4 → removal cue as lower-volume texture layer at named offset (plan 07-02/07-03). Q5 → user decision **D-05a** (deselect reuses the selection cue, never invalid-tap; recorded in CONTEXT.md).

1. **(RESOLVED — D-07a)** **D-07 "no pitch cap" vs. `SoundPool`'s hard 2.0 ceiling.**
   - *What we know:* rate is clamped to `[0.5, 2.0]` — one octave, 12 semitones. The repo's own canonical solutions never exceed a 3-jump same-peg run; a player would need a 13-jump chain to reach the ceiling.
   - *What's unclear:* whether the user reads "no cap" as "must literally never stop rising" (impossible with `SoundPool`) or as "no *design* cap — don't stop it early".
   - *Recommendation:* implement a semitone step with an explicit `coerceIn` at 2.0, document the engine ceiling in the constants KDoc, and surface it to the user at the D-04 verification audit. If literal uncapped rise is later required, it needs a different engine (AudioTrack/Oboe with manual resampling) — a materially larger phase, and out of proportion to a 13+-jump edge case. **Do not silently change D-07 without flagging it.**

2. **(RESOLVED)** **Star-pop stagger: 120 ms or 150 ms?**
   - *What we know:* `ClayAnim.STAR_STAGGER_MS = 120` drives the visual pops; D-13 says "~150 ms apart" for the reduced-motion path.
   - *What's unclear:* whether the 150 ms was a deliberate distinct value or an approximation of the existing 120.
   - *Recommendation:* use `ClayAnim.STAR_STAGGER_MS` when animations are on (SOUND-03 requires being "in step with the animation"), and a named `SoundParams.STAR_POP_STAGGER_MS` for the animations-off path. Set it to 150 as written, and let the D-04 audit adjust by ear.

3. **(RESOLVED)** **Does the win sound land cleanly before Results appears (D-14)?**
   - *What we know:* `GameViewModel.performJump` emits `finished` synchronously with the winning move, and `GameRoute`'s `LaunchedEffect` navigates immediately. The board's own 200 ms jump animation (`GameAnim.JUMP_SLIDE_MS`) is therefore **not** a gap the win sound can occupy — Results appears essentially at once. `SoundPool` playback is fire-and-forget and survives navigation, so the sound *will* be heard; the question is whether it collides with the first star pop.
   - *What's unclear:* how much lead-in the star sequence needs to read as a distinct second beat.
   - *Recommendation:* add a named `SoundParams.CELEBRATION_LEAD_IN_MS` delay before the first star pop and tune it at the D-04 audit. This is a one-constant decision — do not restructure navigation for it.

4. **(RESOLVED)** **Peg-removal cue vs. jump cue are always simultaneous.**
   - *What we know:* every jump removes exactly one peg, so SOUND-01's jump and SOUND-02's removal always co-occur. There is no removal event that is not a jump.
   - *What's unclear:* whether the user expects two audibly distinct layers or a single composite.
   - *Recommendation:* play the removal cue at a short named offset (`GameAnim.REMOVAL_MS` = 180 is the visual reference) and at lower relative volume so it reads as a texture layer under the pitched jump rather than a competing hit. Tune at the D-04 audit.

5. **(RESOLVED — D-05a)** **Does re-tapping the selected peg (deselect) make a sound?**
   - *What we know:* `onCellTap`'s `state.selected == position` branch is a successful deselect, not a rejection; it must not fire the invalid-tap cue.
   - *Recommendation:* reuse the selection cue at reduced volume, or a pitched-down variant. Claude's discretion per the CONTEXT; call it out at the audit.

---

## Sources

### Primary (HIGH confidence)
- **Local Gradle cache — `aapt2-9.1.0-14792394-osx`** — binary `strings` extraction of the built-in no-compress extension list (`.ogg`/`.wav` present, `.opus` absent)
- **Local Gradle cache — `foundation-android-1.11.3-sources.jar`, `material3-android-1.4.0-sources.jar`, `ui-android-1.11.3-sources.jar`** — exhaustive grep proving Compose emits no platform click sound
- **Local repo — `domain/src/test/resources/golden/solutions/*.txt`** — computed longest same-peg run per canonical solution
- **Local repo source** — `GameScreen.kt`, `GameViewModel.kt`, `GameUiState.kt`, `MoveTransition.kt`, `ResultsScreen.kt`, `ResultsCelebration.kt`, `ResultsViewModel.kt`, `SettingsRepository.kt`, `SettingsViewModel.kt`, `ClayAnim.kt`, `GameAnim.kt`, `App.kt`, `MainActivity.kt`, `PlatformModule.kt`, `DataModule.kt`, `PlatformBoundaryRules.kt`, `DomainPurityTest.kt`, `standards.gradle.kts`, `android-app.gradle.kts`, `app/build.gradle.kts`, `libs.versions.toml`, `config/detekt/detekt.yml`, `docs/ui/DESIGN-SYSTEM.md`
- **developer.android.com/media/optimize/audio-focus** — ducking is triggered by a focus request; apps that never request focus are not ducked; Android 12+ forced fade-out; Android 15+ focus restrictions
- **developer.android.com/about/versions/16/behavior-changes-all** — no audio behaviour changes in Android 16
- **developer.android.com/reference/android/media/SoundPool** (content via mirror) — rate range 0.5–2.0, raw 16-bit PCM decode, `autoPause`/`autoResume`/`stop`/`release`, no `stopAll`
- **source.android.com/docs/core/audio/attributes** — `USAGE_GAME` → `STREAM_MUSIC`
- **robolectric.org/javadoc/4.16/org/robolectric/shadows/ShadowSoundPool.html** — shadow API surface
- **kenney.nl/support** — "All game assets on the asset pages are public domain licensed (CC0)"
- **kenney.nl/assets/interface-sounds** — CC0, 100 sounds
- **Local environment probe** — ffmpeg 8.0 (`libvorbis`, `libopus`), `afconvert`, `adb` (`emulator-5554` online), JDK 17.0.15, python3 3.9.6; `sox` absent

### Secondary (MEDIUM confidence)
- Community corroboration of the `sample N not READY` async-load failure mode and the `openRawResourceFd` compressed-resource `FileNotFoundException` (multiple independent sources, consistent with the primary docs)
- SoundPool per-sample ~1 MB decoded limit (~5.6 s at 44.1 kHz stereo) — widely reported, consistent with the documented in-memory PCM design

### Tertiary (LOW confidence)
- The volume-0 warm-up trick for reducing first-play latency (community folklore; logged as assumption A2, and not load-bearing)

---

## Metadata

**Confidence breakdown:**
- Standard stack: **HIGH** — no new dependency; every API verified against official docs, and the two build-level facts (aapt2 no-compress list, Compose sound-effect absence) verified by direct local artifact inspection rather than inference
- Architecture: **HIGH** — every recommended pattern is an existing, documented idiom in this codebase (`JumpHaptic`, `recordSolvedPlayOnce`, `@Binds` seam, `standards.gradle.kts` file-scan gates, `LocalHapticFeedback` test seam)
- Pitfalls: **HIGH** — Pitfalls 1, 4, 5 are documented platform behaviour; Pitfalls 2, 3, 6, 7 were derived from reading this repository's actual code paths, not from generic advice
- Asset aesthetic (D-02/D-03): **MEDIUM** — licence and availability are verified; whether the sounds *feel* like candy clay is by design a UAT judgement (D-04)

**Research date:** 2026-08-28
**Valid until:** 2026-09-27 (30 days — platform audio APIs are extremely stable; the only volatile input is the CC0 asset catalogue)
