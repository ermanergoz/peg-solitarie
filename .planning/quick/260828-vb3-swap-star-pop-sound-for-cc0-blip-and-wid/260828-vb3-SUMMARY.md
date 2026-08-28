---
phase: quick-260828-vb3
plan: 01
subsystem: audio
tags: [audio, sfx, credits, cc0, freesound, results, celebration, motion, dead-code]
requires:
  - SoundCue / SoundPoolPlayer cue table (Phase 07-01/07-02)
  - StarPopSounds celebration sequencer (Phase 07-04)
  - AudioCreditsTest provenance gate (Phase 07-01)
provides:
  - "sfx_star_pop.ogg — the CC0 freesound 531512 blip, encoded from its 24-bit master"
  - "ClayAnim.STAR_POP_MS wired to EarnedStar for the first time (was a dead constant)"
  - "rememberPopIn(delayMillis, durationMillis) — per-caller pop duration, POP_IN_MS default"
affects:
  - app/src/main/res/raw/sfx_star_pop.ogg
  - docs/audio/CREDITS.md
  - app/src/main/java/com/app/pegsolitaire/ui/theme/ClayAnim.kt
  - app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsCelebration.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
tech-stack:
  added: []
  patterns:
    - "Peak-match libvorbis output by measuring the ENCODED peak, not the pre-encode PCM peak"
    - "Verify non-deterministic encoder output by measured properties (codec/rate/duration/peak), never by hash"
    - "Default-argument duration override on rememberPopIn, so one caller diverges without forking the helper"
key-files:
  created: []
  modified:
    - app/src/main/res/raw/sfx_star_pop.ogg
    - docs/audio/CREDITS.md
    - app/src/main/java/com/app/pegsolitaire/ui/theme/ClayAnim.kt
    - app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsCelebration.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsCelebrationSoundTest.kt
decisions:
  - "ClayAnim.STAR_POP_MS was a DEAD constant — every Results pop, stars included, ran on the shared POP_IN_MS (220 ms). Raising it alone would have been a no-op, so the value and its use site were fixed together"
  - "D-03's single-pack cohesion guarantee is knowingly reversed for one cue: 10 of 11 sounds are Kenney, sfx_star_pop.ogg is a freesound borrow. Cohesion is now editorial judgement, not a structural property"
  - "The +1.40 dB pre-encode gain was chosen against the ENCODED peak — libvorbis overshoots this transient, so normalising the PCM to the -3.25 dBFS target lands the encoded file audibly louder"
  - "Picture and sound deliberately do NOT end together: ~1.3 s of cue still rings after the last star settles. Aligning them would need a ~1.9 s pop, rejected as sluggish"
  - "MAX_STREAMS stays 8 — re-checked by arithmetic against the 1.65 s cue (worst case 4 voices), not assumed"
metrics:
  duration: ~50 min
  tasks: 3
  files: 7
  completed: 2026-08-28
---

# Quick Task 260828-vb3: Swap Star Pop for CC0 Blip & Widen Stagger Summary

The Results star pop is now the CC0 freesound "Positive Blip Effect" (531512, CogFireStudios)
encoded from its original 24-bit master — 1.65 s instead of 0.135 s — with the star stagger
widened to 500 ms and each star's own pop lengthened to 600 ms so the celebration stops looking
finished while it is still sounding.

## What Changed

**`sfx_star_pop.ogg`** — replaced Kenney's `glass_005.ogg` in place, keeping the filename so
`SoundCue`'s compile-time `R.raw.sfx_star_pop` reference and the release-shrink retention
argument are both untouched. One ffmpeg pass from the master: 1.65 s trim, stereo→mono,
48 kHz→44.1 kHz, 150 ms fade-out, +1.40 dB, `libvorbis -q:a 4`.

**`ClayAnim.STAR_STAGGER_MS`** 260 → 500, still the single token driving both the visual star
reveal and the audio pop spacing. No second constant was introduced.

**`ClayAnim.STAR_POP_MS`** 260 → 600 **and connected to the code for the first time** (see
below). `rememberPopIn` gained a `durationMillis` parameter defaulting to `POP_IN_MS`;
`EarnedStar` overrides it, while the headline and the "New best!" pill keep the default.

**`SoundParams.STAR_POP_STAGGER_MS`** 325 → 625, holding the documented 1.25x animations-off
ratio (D-13). **`MAX_STREAMS` stays 8**, with its KDoc rewritten to record that the three-voice
overlap it budgets for is now genuinely reached rather than hypothetical.

**`docs/audio/CREDITS.md`** — seven claims the swap falsified were corrected in place.

## The Dead Constant

The most valuable finding in this task, and the reason the ordered fix would not have worked.

`ClayAnim.STAR_POP_MS` was referenced **nowhere in the codebase** except its own declaration and
the prose of the neighbouring `STAR_STAGGER_MS` KDoc reasoning about it. `rememberPopIn` animated
every Results pop over the shared `ClayAnim.POP_IN_MS` (220 ms), so no star had ever run on
`STAR_POP_MS`. Consequences:

- The stars' real duration was **220 ms**, not the 260 ms every document and comment asserted.
- The phase 7 KDoc claim that 260 ms "matched `STAR_POP_MS` exactly — the point overlap reaches
  zero" described code that did not exist. The real gap was 280 ms, by accident.
- The claim this task's own Task 2 added — "each star finishes its pop and then holds still for
  240 ms... a chosen outcome, not drift" — was equally fictional the moment it was written.
- The device-check diagnosis that arrived with the defect report used 260 ms too, and was
  therefore also off.

**Raising the constant from 260 to 600, as instructed, would have changed nothing on screen** and
would have been reported as a fix. The value and its use site were changed together instead, and
both KDocs were rewritten to mark the old arithmetic void rather than annotate around it.

**No test pinned `STAR_POP_MS` or any derived celebration duration** — checked before editing;
there were none to update. That absence is exactly how it went dead unnoticed. **The wiring
remains guarded by review only:** an animation-duration assertion through Compose semantics was
judged too fragile to be worth adding, since the scale value is not exposed and the call site is
private. This is a known, accepted gap, not an oversight.

## Timing, Honestly

| | star 1 | star 2 | star 3 | all visual done | audio done |
|---|---|---|---|---|---|
| before this task | 0–220 | 500–720 | 1000–1220 | 1220 ms | 2900 ms |
| now | 0–600 | 500–1100 | 1000–1600 | 1600 ms | 2900 ms |

Each star now animates through ~36% of its own 1.65 s cue instead of ~13%. **Picture and sound
still do not end together — roughly 1.3 s of audio rings after the last star settles. That is
accepted, not fixed.** Closing it would need a ~1.9 s pop, which reads as sluggish rather than
stately. The goal was to remove the *impression* that the picture had finished, not to land both
on the same frame, and no KDoc or comment in the tree now claims otherwise.

Because 600 ms exceeds the 500 ms stagger, **consecutive stars now overlap in motion by 100 ms** —
a reversal of the (fictional) dead gap the previous KDoc documented.

## Measured Facts

**Master:** `pcm_s24le` / 2 ch / 48 kHz / 3.004646 s / 866,074 B, md5
`02c2514f4b3953424eda3181aec8249b` — all five re-checked immediately before encoding (T-07-01).

**Shipped asset:** `vorbis` / 1 ch / 44,100 Hz / **1.650000 s** / 17,726 B / encoded peak
**-3.338 dBFS** — within **0.09 dB** of the -3.248 dBFS `glass_005` it replaces, so the per-cue
balance CREDITS.md documents survives the swap.

**The gain had to be measured against the encoded peak.** libvorbis overshoots this transient, so
normalising the PCM to -3.25 dBFS before encoding lands the file audibly louder. The plan's
empirical sweep gave -3.591 dBFS at +1.20 dB, -3.338 dBFS at +1.40 dB, -3.014 dBFS at +1.50 dB.
+1.40 dB was used unchanged and reproduced the planner's predicted 17,726 B exactly.

**Decoded footprint:** 1.65 s × 44,100 Hz × 2 B = 145,530 B, ~14% of `SoundPool`'s 1 MB
per-sample limit despite the ~12x duration increase.

**libvorbis is not byte-deterministic** — identical invocations produce identical audio under
different md5s. CREDITS.md records that this file must be verified by measured properties, never
by hash, so no future reader writes a hash gate that fails spuriously.

## Deviations from Plan

### [Rule 1 - Bug] `ClayAnim.STAR_POP_MS` was wired to nothing

- **Found during:** Task 3 rework, after the device check reported a picture/sound mismatch
- **Issue:** The constant drove no animation; `rememberPopIn` used `POP_IN_MS` for all callers
- **Fix:** Added `durationMillis` (defaulting to `POP_IN_MS`) to `rememberPopIn`; `EarnedStar`
  passes `ClayAnim.STAR_POP_MS`; raised the value 260 → 600; rewrote both falsified KDocs
- **Files modified:** `ResultsCelebration.kt`, `ResultsScreen.kt`, `ClayAnim.kt`
- **Commit:** `b1f0178`

This also broke one of Task 2's own literal verification greps
(`rememberPopIn(delayMillis = order * ClayAnim.STAR_STAGGER_MS)`), because the call now passes a
second argument across multiple lines. The plan frontmatter's `key_links` regex
(`order \* ClayAnim\.STAR_STAGGER_MS`) still matches, and the invariant it protects — one shared
stagger token read by both picture and sound — is intact and was re-verified.

### [Rule 1 - Bug] The "240 ms of stillness" KDoc claim was removed, not annotated

Task 2 wrote it; the Task 3 fix falsified it within the same task. Left standing it would have
been the third generation of this comment describing code that did not exist.

## Verification

- `./gradlew :app:check --offline` — **green**: detekt, ktlint, lint, 356 unit tests, 0 failures.
  `ResultsScreenTest` (5), `ResultsCelebrationSoundTest` (7), `ResultsViewModelTest` (21) all pass.
- `AudioCreditsTest` — **proven fresh, not cached.** `CREDITS.md` is not a declared Gradle input,
  so a docs-only edit can leave `testDebugUnitTest` UP-TO-DATE and report a meaningless PASS
  (T-VB3-03). Forced with `--rerun`; the task ran without an UP-TO-DATE marker and the result XML
  showed 2 tests / 0 failures at a timestamp 7 seconds old.
- All 30 static gates from the plan's two `<verify>` blocks passed, including the six negative
  greps asserting the falsified CREDITS.md claims are gone.
- `ffprobe`/`astats` asset properties as tabled above.
- **Not run: a release build.** Nothing here can affect resource retention — the resource id and
  its compile-time reference are unchanged and only the payload bytes differ — so CREDITS.md
  date-scopes the existing shrink proof instead of restating it as current.

## Device Verification (Task 3)

Approved by the user after a device pass, with three caveats recorded rather than smoothed over:

- **The approval was global, not itemised.** The user replied "all good" for the star
  celebration rather than answering the nine numbered checks individually. Per-step results are
  therefore not claimed. The reply did explicitly cover the 600 ms pop against the 500 ms
  stagger, the 100 ms motion overlap, the overlapping audio tails, and the 1-star / 2-star cases.
- **Hardware was not stated.** Only `emulator-5554` was attached during this session, so this is
  **emulator-grade confirmation, not attested physical-device confirmation** — the same caveat
  phase 7's manual audit carries.
- The first device pass **rejected** the change, reporting that the animation speed did not match
  the sound; that defect is what produced the dead-constant finding above.

## Known Stubs

None.

## Carried Caveats

- The release APK built for the phase 7 shrink proof used **placeholder ad / Play Games IDs** and
  is a verification artifact, **not a shippable build**. CREDITS.md's shrink section is now
  date-scoped to that pre-swap run, and two of its figures are stale by design: the packaged-size
  range 4,087–7,011 B predates the 17,726 B star pop, and the recorded APK size with it.
- `ClayAnim.POP_IN_MS` (220 ms) still drives the `NewBestBadge` pop, which fires at
  `TOTAL_STARS * STAR_STAGGER_MS` (1500 ms) and settles at 1720 ms — also well before the audio
  ends. Left alone deliberately: the decision was about the stars.

## Self-Check: PASSED

- `app/src/main/res/raw/sfx_star_pop.ogg` — FOUND (17,726 B, 1.650000 s, -3.338 dBFS)
- `docs/audio/CREDITS.md` — FOUND, contains `### Star pop replacement (2026-08-28)`
- `ClayAnim.STAR_POP_MS = 600`, `STAR_STAGGER_MS = 500` — FOUND
- `SoundParams.STAR_POP_STAGGER_MS = 625`, `MAX_STREAMS = 8` — FOUND
- `ResultsScreen.kt` `durationMillis = ClayAnim.STAR_POP_MS` — FOUND
- Commits `c60a0a8`, `ebcc836`, `b1f0178` — all FOUND in `git log`
- All values re-confirmed present at branch HEAD after three later `260828-vrr` commits landed
  on top of this work
