---
phase: 07-sound
plan: 02
subsystem: audio-engine
tags: [audio, soundpool, hilt, compose, mute-gate, media-volume]
requires:
  - "R.raw.sfx_* — the eleven sound resource ids committed by plan 07-01"
  - "audioFocusLiteralCheck — the SOUND-06 build gate from plan 07-01"
  - "SettingsRepository.soundOn — the existing fail-open sound flag"
  - "@ApplicationScope CoroutineScope — the app-lifetime scope from DataModule"
provides:
  - "SoundPlayer — the five-method audio seam every later cue is fired through"
  - "SoundCue — the 11-entry enum carrying @RawRes id, volume and priority"
  - "SoundParams — scoped audio constants plus the pure jumpRate/starPopRate ladders"
  - "SoundPoolPlayer — the @Singleton SoundPool impl, mute gate and stream tracking"
  - "LocalSoundPlayer + withClickSound — the Compose-side access point"
  - "FakeSoundPlayer — the recording test double plans 07-03/04/05 assert against"
affects:
  - "app/src/main/java/com/app/pegsolitaire/App.kt (now preloads samples at startup)"
  - "app/src/main/java/com/app/pegsolitaire/MainActivity.kt (now provides LocalSoundPlayer)"
tech-stack:
  added: []
  patterns:
    - "Interface seam + @Inject-constructor impl + @Binds, cloned from AdGateway"
    - "@Singleton holder subscribing to a DataStore-backed flow on @ApplicationScope (ChallengeProgress shape)"
    - "Robolectric shadow subclass to record framework calls the stock shadow ignores"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/audio/SoundPlayer.kt
    - app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt
    - app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt
    - app/src/main/java/com/app/pegsolitaire/audio/NoOpSoundPlayer.kt
    - app/src/main/java/com/app/pegsolitaire/audio/SoundPoolPlayer.kt
    - app/src/main/java/com/app/pegsolitaire/audio/LocalSoundPlayer.kt
    - app/src/main/java/com/app/pegsolitaire/di/AudioModule.kt
    - app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt
    - app/src/test/java/com/app/pegsolitaire/audio/SoundPoolPlayerTest.kt
    - app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/App.kt
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
decisions:
  - "The soundOn mute gate lives inside SoundPoolPlayer, not in any UiState — only the holder of the live stream ids can satisfy SOUND-05's 'including already-queued one-shots'"
  - "D-07a: the jump ladder is explicitly clamped to SoundPool's 0.5–2.0 range, making the one-octave ceiling a tested contract rather than a silent engine surprise"
  - "Audio binds in its own AudioModule, never in PlatformModule — ..platform.. is the Play/AdMob SDK + Activity seam and three ArchUnit rules depend on that definition"
  - "A RecordingSoundPoolShadow subclasses Robolectric's ShadowSoundPool so stop()/autoPause() are recorded; the stock shadow records neither, and without it SOUND-05 could only be asserted as 'our own bookkeeping was cleared'"
metrics:
  duration: ~50 min
  completed: 2026-08-28
---

# Phase 7 Plan 02: Audio Engine Summary

A single application-scoped `SoundPool` now routes to the system media volume without ever
holding audio focus, decodes all eleven samples at process start, climbs a clamped semitone
ladder per same-peg jump, and silences streams already in flight the instant `soundOn` goes
false — with all of it asserted JVM-side.

## What Was Built

**Task 1 — the seam, the cue set and the pure maths** (`4c51eeb`).

`SoundParams` is the scoped constants object (QUAL-03): playback rates, the eleven per-cue
volumes, the celebration timings, the eight-voice budget, and two pure functions.
`jumpRate(runIndex)` multiplies by `SEMITONE_STEP` per rung; `starPopRate(step)` climbs a
major third per star so three stars outline a rising triad (D-15). Both `coerceIn` to
`MIN_RATE..MAX_RATE`, which is the T-07-02 mitigation, not decoration: `runIndex` and `step`
are derived from gameplay state and unbounded by type, and the clamp is what makes both
functions total over `Int`.

`SoundCue` is the 11-entry enum over the plan 07-01 assets, each carrying a compile-time
`@RawRes` id, its mix level and its voice-stealing priority. `Win` and `StarPop` take
`PRIORITY_CELEBRATION`; everything else takes `PRIORITY_DEFAULT`, so if the pool ever runs
out of voices it sacrifices a jump rather than the stinger.

`SoundPlayer` is the five-method seam, `NoOpSoundPlayer` the silent fallback.
`SoundParamsTest` covers the ladder, the ceiling, the negative index, the triad prefix and
the relative mix in **13 runner-free JVM tests**.

**Task 2 — the SoundPool implementation** (`073328e`).

`SoundPoolPlayer` is a `@Singleton` built through `SoundPool.Builder` with
`USAGE_GAME` + `CONTENT_TYPE_SONIFICATION`. `USAGE_GAME` is what routes the pool to
`STREAM_MUSIC` — the media volume slider — and that is one half of SOUND-06. The other half
is an omission: no `AudioManager` is obtained and no focus request is ever made, so the app
cannot duck the player's podcast, because ducking is initiated *by* a focus request. The
omission is explained in a `//` comment at the construction site (which
`audioFocusLiteralCheck` skips by design) and enforced by that gate.

State is three collections and a flag: `soundIds` (cue → sound id, whose presence check
makes `preload()` idempotent), `loaded` (ids whose decode completed), a `@Volatile enabled`
gate that fails open to `true`, and an `activeStreams` `ArrayDeque` bounded to
`MAX_STREAMS` with oldest-first eviction (T-07-03). `play()` returns early unless the gate
is open *and* the sample has finished decoding, so an unloaded sample is silence rather
than a crash. On a `soundOn` false emission the collector calls `silenceNow()`, which stops
every tracked id and then `autoPause()`s as a backstop — the pool is never torn down,
because that would discard the decoded samples and reintroduce the silent-first-tap failure
the preload exists to prevent.

`AudioModule` binds `SoundPlayer → SoundPoolPlayer` with `@Singleton`, in its own module.
`@Singleton` also sits on the class, and the KDoc at both ends says why: `@Binds @Singleton`
scopes the *interface* binding, so without the class annotation an injection of the concrete
type would build a second player owning a second `SoundPool` that the mute could not reach
(T-07-05).

`SoundPoolPlayerTest` is **11 Robolectric tests** covering the attributes, one-decode-per-cue,
preload idempotency, the load-before-play guard in both directions, the three-rung ladder,
the mute blocking new plays, the mute stopping in-flight streams, mute re-entrancy, the D-12
bypass, and the bounded buffer under 100 rapid plays.

**Task 3 — Compose wiring, startup preload and the test fake** (`90f91b2`).

`LocalSoundPlayer` is a `staticCompositionLocalOf` defaulting to `NoOpSoundPlayer()`, so
previews render silently instead of crashing. `withClickSound(cue, onClick)` is a
per-callsite wrapper whose KDoc names D-09 explicitly: folding the cue into
`Modifier.claySquish()` would blanket-apply it to the top-bar back buttons and the Settings
switches, which are exactly the controls D-09 requires to stay silent.

`App` injects the `SoundPlayer` **interface** plus the `@ApplicationScope` scope and launches
`preload()` after `super.onCreate()`; the existing ordering KDoc was extended rather than
duplicated. `MainActivity` injects the interface and wraps `setContent` in
`CompositionLocalProvider(LocalSoundPlayer provides soundPlayer)`. `FakeSoundPlayer` records
ordered `PlayedCue(cue, rate, gateBypassed)` events plus a `preloadCalls` count, with the
D-12 confirmation distinguishable so plan 07-05 can assert it.

## Key Decisions

**The mute gate is in the player, not in `GameUiState`.** SOUND-05 says "including
already-queued one-shots". A UI flag can gate future calls and nothing else; only the object
holding the stream ids returned by `SoundPool.play` can stop a sound that is already
sounding. The gate therefore subscribes to `SettingsRepository.soundOn` directly on the
application scope, which also means it is correct when no screen is composed.

**D-07a — the ladder is clamped, deliberately.** D-07 asked for an uncapped rise, but
`SoundPool` clamps at 2.0x regardless, so "uncapped" was never on offer. Clamping in
`SoundParams` turns the engine's silent ceiling into a documented, tested one. The longest
same-peg run in the repo's golden solutions is 3 (rate ~1.19), so canonical play is nowhere
near it.

**A custom Robolectric shadow, because the stock one cannot answer the question.**
`ShadowSoundPool` records `load` and `play` but not `stop` or `autoPause`. Asserting SOUND-05
against it would only have proved "the player cleared its own list". `RecordingSoundPoolShadow`
extends the stock shadow and records exactly those two calls, so
`switchingSoundOffStopsTheStreamsAlreadyInFlight` asserts that `stop` reached each id that
was in flight *and* that the backstop ran. Its limit is documented in the test KDoc: under
Robolectric `SoundPool.stop` is a no-op'd native method, so no JVM test can prove the
platform's behaviour when stopping a finished stream (RESEARCH assumption A1) — the
`silencingTwiceIsSafe` test proves only that this class's own path is re-entrant.

**`@param:` use-site targets in the new package.** Kotlin 2.3 warns (KT-73255) that a
qualifier on a constructor `val` will change default target. `@param:ApplicationContext` /
`@param:ApplicationScope` / `@param:RawRes` pin it where Dagger and the annotation actually
need it and keep the build warning-free; `@field:ApplicationScope` is used for `App`'s
injected field for the same reason, and that one is a correctness requirement — the default
would land the qualifier on the Kotlin property where Dagger's field injection cannot see
it, and the graph would fail to resolve an unqualified `CoroutineScope`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Two acceptance greps contradicted their own `<action>` text**

- **Found during:** Tasks 1 and 3
- **Issue:** The plan asked the `SoundCue` KDoc to explain "why never `resources.getIdentifier()`"
  while also requiring `grep -q 'getIdentifier' …/audio/` to return no match; the same shape
  recurred with `SoundPlayer`'s "no `suspend` modifier", `LocalSoundPlayer`'s "no `error(`",
  `SoundParams`' "no `STAR_STAGGER_MS`" and `MainActivity`'s "no `SoundPoolPlayer`". Written
  literally, each explanation would trip its own gate.
- **Fix:** Kept every argument and reworded it to avoid the literal token — e.g. "never
  resolved from a name string at runtime through the `Resources` name-lookup API", "the
  Results star-stagger token in `ClayAnim`", "naming the concrete implementation instead".
  The reasoning a future reader needs is intact; only the exact identifier is gone.
- **Files modified:** `SoundCue.kt`, `SoundPlayer.kt`, `SoundParams.kt`, `LocalSoundPlayer.kt`,
  `MainActivity.kt`
- **Commits:** `4c51eeb`, `90f91b2`

**2. [Rule 2 - Missing critical] `@field:ApplicationScope` on `App.appScope`**

- **Found during:** Task 3
- **Issue:** The plan specified `@Inject @ApplicationScope lateinit var appScope`. A
  qualifier with no declared `@Target` defaults to the Kotlin *property* on a `lateinit var`,
  where Dagger's field injection cannot see it — the graph would then look for an unqualified
  `CoroutineScope` and fail to compile.
- **Fix:** `@field:ApplicationScope`, with a `//` comment recording why the target is not
  stylistic.
- **Files modified:** `App.kt`
- **Commit:** `90f91b2`

**3. [Rule 2 - Missing critical] Two `@VisibleForTesting internal` accessors on the player**

- **Found during:** Task 2
- **Issue:** The plan's `<behavior>` requires asserting "11 distinct loads", "the tracked
  collection never exceeds `MAX_STREAMS`" and "`stop` reached the previously tracked stream
  ids". None of those are observable through `ShadowSoundPool`, and `shadowOf(pool)` needs
  the pool itself.
- **Fix:** `internal val pool`, `internal fun loadedSoundIds()` and
  `internal fun trackedStreamIds()`, all `@VisibleForTesting` and all read-only. No behaviour
  is exposed for mutation. This mirrors `GameViewModel.isOnRecordedLine()`, the existing
  precedent for an `internal` accessor read by the JVM suite.
- **Files modified:** `SoundPoolPlayer.kt`
- **Commit:** `073328e`

**4. [Rule 3 - Blocking] The audio attributes are read by reflection, not from the shadow**

- **Found during:** Task 2
- **Issue:** The plan says to assert `USAGE_GAME`/`CONTENT_TYPE_SONIFICATION` "via
  Robolectric's shadow of the built pool", but `ShadowSoundPool` exposes no attribute
  accessor and `SoundPool` has no public getter.
- **Fix:** `ReflectionHelpers.getField<AudioAttributes>(player.pool, "mAttributes")` reads
  the field the real `SoundPool` constructor stored, so the assertion is still against the
  pool that was actually built rather than against a copy the player kept.
- **Files modified:** `SoundPoolPlayerTest.kt`
- **Commit:** `073328e`

### Authentication Gates

None.

## Verification Performed

- `./gradlew :app:testDebugUnitTest --tests "…SoundParamsTest" --offline` — **13 tests, 0
  failures** (confirmed in the JUnit XML).
- `./gradlew :app:testDebugUnitTest --tests "…SoundPoolPlayerTest" --offline` — **11 tests, 0
  failures** (confirmed in the JUnit XML).
- `./gradlew :app:check --offline` — BUILD SUCCESSFUL, including detekt, ktlint,
  `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`, `DomainPurityTest`,
  `PlatformBoundaryTest`, `ManifestSurfaceTest` and the full JVM suite. No test regressed.
- `./gradlew :app:assembleDebug --offline` — BUILD SUCCESSFUL; the Hilt graph resolves
  `SoundPlayer` into both `App` and `MainActivity` (`App_MembersInjector.java`,
  `MainActivity_MembersInjector.java` and `SoundPoolPlayer_Factory.java` are all generated).
- Structural gates, all confirmed by grep: 11 `R.raw.sfx_` references and no runtime
  name-lookup in `audio/`; zero `requestAudioFocus`/`AudioManager` and zero
  `autoResume`/`release()` outside comments in `SoundPoolPlayer.kt`; `@Singleton` on the
  class line; no `SoundPlayer` in `PlatformModule.kt`; no `LocalSoundPlayer`/`withClickSound`
  in `Clay.kt`; no `SoundPoolPlayer` named in `App.kt`/`MainActivity.kt`; no `FakeSoundPlayer`
  reference anywhere under `src/main/`.
- Build is warning-free: the Kotlin 2.3 KT-73255 use-site-target warnings that the first
  compile emitted were removed by pinning `@param:`.

## Requirements

**SOUND-05 and SOUND-06 are marked complete** in `REQUIREMENTS.md` — both are decided
entirely inside this plan. SOUND-05's mute is implemented and asserted end-to-end at the
only layer that can satisfy it; SOUND-06 is implemented by the attributes plus the omission,
and is permanently enforced by a build gate.

**SOUND-01, SOUND-02 and SOUND-03 are deliberately left open**, even though this plan's
frontmatter lists them. This plan builds the mechanism (the pitch ladder, the cue volumes,
the star triad, the preload that makes the first tap audible) but fires no cue from any
screen. A player still hears nothing. They belong to plans 07-03/04/05, which wire the cues
to gameplay, the celebration and the buttons.

**Note on SOUND-06's manual leg.** The "start a podcast, play a level, the podcast never
dips" check cannot be automated and is not claimed here. The structural half — media-volume
attributes present, no focus API anywhere in `src/main` — is machine-enforced; the perceptual
half remains a manual UAT item, preferably on a physical device (RESEARCH §Environment).

## Threat Flags

None. This plan adds no network endpoint, no auth path, no schema change and no new
permission. Its three registered threats (T-07-02 rate totality, T-07-03 stream-buffer
growth, T-07-05 binding scope) are each mitigated and each has a test or a structural gate
naming it.

## Known Stubs

None. Every declaration in the new package is fully implemented. `NoOpSoundPlayer` is an
intentional silent implementation, not a stub: it is the documented fail-open default for
`LocalSoundPlayer` outside `MainActivity`.

## Self-Check: PASSED

All twelve created/modified files verified present on disk; all three commit hashes
(`4c51eeb`, `073328e`, `90f91b2`) verified present in `git log`.
