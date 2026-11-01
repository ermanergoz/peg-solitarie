---
phase: 07-sound
plan: 05
subsystem: button-audio
tags: [audio, buttons, compose, d-09, d-11, d-12, settings, gate-bypass]
requires:
  - "withClickSound + LocalSoundPlayer — the Compose click seam from plan 07-02"
  - "SoundCue.ButtonForward / ButtonDismiss / SoundEnabled — the button cue set (07-01/07-02)"
  - "SoundPlayer.playSoundEnabledConfirmation() — the single documented mute-gate bypass (07-02)"
  - "FakeSoundPlayer — the recording test double (07-02)"
  - "NoOpSoundPlayer — the silent implementation used at the existing Settings test call sites"
provides:
  - "The nine D-09 coverage-table CTAs, each wrapped at its leaf composable"
  - "SettingsViewModel.soundPlayer — the D-12 enable-only confirmation blip"
  - "ButtonSoundTest — positive AND negative coverage assertions for D-09/D-11"
  - "SettingsViewModelSoundTest — the three D-12 cases"
affects:
  - "app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt (constructor gained an argument)"
tech-stack:
  added: []
  patterns:
    - "Per-callsite @Composable click wrapper applied at the LEAF composable, so every caller inherits it once"
    - "Negative (silence) tests as first-class cases: excluded control's callback fired AND the recording player heard nothing"
    - "ViewModel-side one-shot feedback on the user event rather than on a flow collector"
key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsActions.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
decisions:
  - "The cue is wrapped at the LEAF composable (ModeTile, PrimaryAction, NeutralAction, BoardCard), so nine call sites cost six edits and no caller can forget it — while staying per-callsite rather than folded into the shared claySquish() modifier"
  - "D-10 requires no implementation: the pinned Compose stack emits no Android platform click tick, so there is no doubled sound to suppress and no soundEffectsEnabled API to turn off; recorded as a // note at the code"
  - "The D-12 blip fires BEFORE the DataStore write is launched and through the documented bypass, because at that instant the player's own gate is still closed and a gated call would be dropped"
  - "The four negative tests are the point of ButtonSoundTest: they assert the excluded control's callback DID fire and that the recording player heard nothing, so a future 'simplification' onto claySquish() fails the suite rather than passing it"
metrics:
  duration: ~40 min
  completed: 2026-08-28
---

# Phase 7 Plan 05: Button Cues & the Settings Blip Summary

Every navigational CTA now clicks — forward when you commit, softer when you back out — and
every control D-09 excludes is proven silent by a test rather than by inspection; flipping
sound on in Settings answers with one pop through the only bypass in the app.

## What Was Built

**Task 1 — the nine coverage-table call sites** (`4ba8dbb`).

Nine call sites, six edits, because each wrap sits at the **leaf** composable where one exists:
`ModeTile` covers both Home mode tiles, `PrimaryAction` covers Replay / Next level / Retry,
`NeutralAction` covers Home, and the `ClassicActions` `OutlinedButton` covers Catalog. The
remaining three — `ResumeTile`'s `Card` (it does not route through `ModeTile`), `BoardCard`'s
`Card`, and the restart dialog's confirm/dismiss pair — carry their own.

The restart dialog is the only place both cues meet, and that is exactly why D-11 wants two
assets: `confirmButton` takes `ButtonForward`, `dismissButton` takes `ButtonDismiss`, so a
player can hear which way they went without reading the label.

The tempting simplification was rejected on purpose and the code says so at four sites. Folding
the cue into `Modifier.claySquish()` would have been one edit — and `claySquish()` is on the
Home top-bar icons, the catalog back nav, the gameplay back nav, the hint toggle, the Restart
control and the Settings switches, which is precisely the D-09 exclusion list. Opting *out* per
callsite is also the wrong default: a new silent control would start making noise by omission.
Each of the four modified files carries a `//` comment naming D-09/D-11 and pointing at the
`withClickSound` KDoc's exclusion list, so the selectivity reads as deliberate rather than
half-finished.

`ButtonSoundTest` is **13 Robolectric tests**, of which **4 are negative cases** — the Home
top-bar How-to-play + Settings icons, the catalog back nav, the Results back nav, and the
gameplay back nav + hint toggle. Each clicks the excluded control, asserts its hoisted callback
DID fire, and asserts `sounds.cues` is empty. The nine positive cases each assert both the
single expected cue *and* that the original callback still ran with its original argument (the
persisted challenge level, the resume board id, the replay id, the tapped board id) — the
wrapper adds a sound, it never replaces or reorders behaviour (T-07-13).

**Task 2 — the D-12 confirmation blip** (`2257686`).

`SettingsViewModel` gained `private val soundPlayer: SoundPlayer`, and `onToggleSound` gained
one line: `if (on) soundPlayer.playSoundEnabledConfirmation()`.

Both halves of that line are load-bearing and both are argued in the KDoc. **Placement:** it is
on the user event, not in the `init` block's `soundOn` collector — that collector fires on its
initial emission every time the Settings screen is opened, which is exactly what D-12 forbids.
Hanging the blip off the event makes "only on a user-initiated enable" true by construction
rather than by a flag someone has to remember to reset. **Method:** it is the documented bypass,
not `play(SoundCue.SoundEnabled)`. The DataStore write is asynchronous, so at the instant the pop
is due the value has not round-tripped back through the collected flow, the player's `enabled`
gate is still closed, and a gated call would be swallowed (RESEARCH Pitfall 3).

The class KDoc's "the repository is the single authority" paragraph was extended rather than
contradicted: the player is a feedback sink only, nothing read from it reaches `SettingsUiState`,
and the switches still render exclusively from the collected repository flows.

`SettingsViewModelSoundTest` is **8 tests**: the enable blips exactly once, the blip is recorded
as gate-bypassed, the disable is silent, construction is silent whatever the stored value is,
a store already emitting `soundOn = true` still says nothing until the player toggles, the
enable still writes through to the repository, haptics are silent in both directions, and a
never-emitting store is silent on construction too. The six existing `SettingsViewModelTest`
cases gained a `NoOpSoundPlayer()` argument and nothing else.

## Key Decisions

**Leaf-level wrapping, per callsite.** These two are in tension and both matter. Wrapping at the
leaf means nine call sites cost six edits and no future caller of `ModeTile` or `PrimaryAction`
can forget the cue. Staying per-callsite (rather than on the shared modifier) means the coverage
is greppable and reviewable, and the acceptance criteria pin it: `Clay.kt` contains no audio
reference at all, and the five excluded control files contain no `withClickSound`. A future
"simplify this onto claySquish()" change fails four tests in this plan's own suite (T-07-14).

**D-10 needed no code, and that is recorded where someone would go looking.** The pinned Compose
stack never emits the Android platform click tick — there is no `View.playSoundEffect` /
`SoundEffectConstants` / `soundEffectsEnabled` path in Compose's click handling — so our cue
cannot double with the OS touch sound and there is no platform toggle to switch off. A `//` note
beside the `ResultsActions.kt` wrap says this, so nobody later hunts for a suppression call that
does not exist. `grep -rn 'soundEffectsEnabled\|SoundEffectConstants\|playSoundEffect'
app/src/main/` returns nothing, confirming there is no such call to remove. The perceptual half —
"with OS touch sounds enabled, the click is heard exactly once" — remains a manual UAT item in
plan 07-06.

**Exactly one gate bypass call site, and the tests pin it.** `playSoundEnabledConfirmation()` is
the only path in the app that plays audio while `soundOn` is false (T-07-12). It is reached from
one place, guarded by `if (on)`, and never from a collector or a screen. Two of the eight new
tests exist solely to prove the paths it must NOT be reached from — the disable path and the
initial-emission path — because those are the failures that would only ever be noticed by a
player who muted the game and then opened Settings.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Four acceptance greps counted the import line as a call site**

- **Found during:** Task 1
- **Issue:** The criteria ask that `grep -c 'withClickSound'` return 3 / 2 / 1 / 2 for
  `ResultsActions.kt` / `HomeScreen.kt` / `ClassicCatalogScreen.kt` / `GameScreen.kt`. Kotlin
  requires `import com.app.pegsolitaire.audio.withClickSound` in each file, and `grep -c` counts
  matching *lines*, so the bare-token count can never equal the call count. A wildcard import
  would satisfy the letter of it and fail ktlint's `no-wildcard-imports`.
- **Fix:** Verified against `grep -c 'withClickSound('` — the paren form, which is also what the
  plan's own `key_links.pattern` (`withClickSound\(`) specifies. It returns exactly **3 / 2 / 1 /
  2**, matching the intent precisely. The bare-token counts are 5 / 4 / 3 / 4 (import + calls +
  the D-09 explanatory comments).
- **Files modified:** none — this is a criterion-reading correction, not a code change.
- **Commit:** `4ba8dbb`

**2. [Rule 3 - Blocking] `performScrollTo()` is not usable on the four non-scrolling screens**

- **Found during:** Task 1 (RED run)
- **Issue:** A single `clickText` helper using `performScrollTo().performClick()` threw on Home,
  the catalog and the restart dialog — `performScrollTo` requires a scrollable ancestor, and only
  `ResultsScreen` has one (its `verticalScroll` content column).
- **Fix:** Split into `clickText` (plain click) and `clickResultsAction`
  (`performScrollTo().performClick()`), used only for the three Results CTAs. The helper's KDoc
  says why Results is the exception.
- **Files modified:** `ButtonSoundTest.kt`
- **Commit:** `4ba8dbb`

**3. [Rule 3 - Blocking] The existing repository fakes cannot be shared across spec files**

- **Found during:** Task 2
- **Issue:** The plan says to use "the existing file-private repository fakes"
  (`FakeSettingsRepository`, `NeverEmittingSettingsRepository`). File-private top-level
  declarations are not visible from a sibling file, so they cannot be reused.
- **Fix:** Redeclared the two shapes the new spec actually needs as
  `SettableSettingsRepository` / `SilentSettingsRepository`, file-private in
  `SettingsViewModelSoundTest.kt`, with a KDoc naming the original and why it is redeclared.
  Distinct names so the two specs never read as one shared fixture.
- **Files modified:** `SettingsViewModelSoundTest.kt`
- **Commit:** `2257686`

### Process note

Both tasks are marked `tdd="true"` and both were executed RED-first — Task 1's spec was written
and run against unmodified source (9 of 13 failing, the 4 negative cases passing vacuously),
Task 2's against the one-argument constructor (8 compile errors, all "Too many arguments"). Each
task is a single commit rather than a `test`/`feat` pair, matching plans 07-02/03/04 and keeping
every commit on the branch compiling.

### Authentication Gates

None.

## Verification Performed

- `./gradlew :app:testDebugUnitTest --tests "…ButtonSoundTest" --tests "…HomeScreenTest" --tests
  "…ResultsScreenTest" --tests "…GameFlowTest" --offline` — BUILD SUCCESSFUL; **13 tests, 0
  failures** in `ButtonSoundTest` (JUnit XML), no regression in the other three.
- `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.ui.settings.*" --offline` —
  BUILD SUCCESSFUL; **8 tests** in `SettingsViewModelSoundTest`, **6** in `SettingsViewModelTest`,
  0 failures in either.
- `./gradlew :app:check --offline` — BUILD SUCCESSFUL: **350 JVM tests, 0 failures, 0 errors**,
  plus detekt, ktlint (main + test source sets), `dpSpLiteralCheck`, `adIdLiteralCheck`,
  `audioFocusLiteralCheck` and lint.
- `./gradlew :app:assembleDebug --offline` — BUILD SUCCESSFUL; the generated
  `SettingsViewModel_Factory.java` declares `Provider<SoundPlayer> soundPlayerProvider`, so the
  Hilt graph resolves the new dependency through `AudioModule`'s existing binding.
- Structural gates, all confirmed by grep:
  - `withClickSound(` counts: `ResultsActions.kt` **3**, `HomeScreen.kt` **2**,
    `ClassicCatalogScreen.kt` **1**, `GameScreen.kt` **2** — the last split exactly one
    `ButtonForward` / one `ButtonDismiss`.
  - `ui/theme/Clay.kt` contains **zero** matches for `withClickSound|LocalSoundPlayer|SoundCue`.
  - `SettingsScreen.kt`, `HintToggleControl.kt`, `UndoControl.kt`, `HintControl.kt` and
    `RestartControl.kt` contain **no** `withClickSound`; `git diff --name-only HEAD` confirms
    `SettingsScreen.kt` is byte-identical to its pre-task state.
  - `grep -rn 'soundEffectsEnabled\|SoundEffectConstants\|playSoundEffect' app/src/main/` — **no
    match** (D-10 is a no-op, confirmed).
  - `SettingsViewModel.kt` declares `private val soundPlayer: SoundPlayer` and contains
    `if (on) soundPlayer.playSoundEnabledConfirmation()`; the four `soundPlayer` references are
    two KDoc lines, the constructor parameter and the one call inside `onToggleSound` — none in
    the `init` block. `SoundCue.SoundEnabled` appears **0** times in the file (the bypass method
    is used, not the gated `play`).

## Requirements

**SOUND-04 is complete.** Primary UI actions give button-press feedback, with D-09's selective
coverage and D-11's forward/dismiss pair both delivered and asserted in both directions. D-12's
enable-only confirmation is the one deliberate exception to toggle silence and is likewise
asserted, including the two cases it forbids.

The perceptual half — whether `sfx_btn_forward` and `sfx_btn_dismiss` are *audibly* distinct to a
listener, and whether the click is heard exactly once with OS touch sounds enabled (D-10) —
cannot be asserted JVM-side. Both are manual UAT items for plan 07-06.

## Threat Flags

None. This plan adds no network endpoint, auth path, schema change or permission. Its four
registered threats are each addressed as registered: **T-07-12** (gate-bypass elevation) by the
single `if (on)`-guarded call site, with the disable path and the initial-emission path each
asserted silent; **T-07-13** (wrapper ordering) by every positive test asserting BOTH the cue and
that the original callback fired with its original argument; **T-07-14** (blanket application via
`claySquish()`) by per-callsite coverage plus the `Clay.kt` and excluded-file greps, so a future
simplification onto the shared modifier fails four tests here; **T-07-06** accepted as planned —
`withClickSound` fires inside a click handler in a live composition, and the Settings blip fires
on a ViewModel method invoked from a live screen.

## Known Stubs

None. Every wrap added is wired to a real cue and exercised by a test.

## Self-Check: PASSED

All two created and six modified files verified present on disk; both commit hashes (`4ba8dbb`,
`2257686`) verified present in `git log`.
