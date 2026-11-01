---
created: 2026-08-28T20:46:20.377Z
title: Confirm audio cues on physical hardware
area: general
resolves_phase: null
files:
  - app/src/main/java/com/app/pegsolitaire/audio/SoundPoolPlayer.kt:95-135
  - app/src/main/java/com/app/pegsolitaire/App.kt:61
  - build-logic/src/main/kotlin/standards.gradle.kts:141
  - .planning/phases/07-sound/07-VERIFICATION.md
---

## Problem

Every human audio checkpoint in Phase 7 and its two follow-up quick tasks (`260828-vb3`,
`260828-vrr`) was discharged against `emulator-5554`. No physical device was ever attached.
Phase 7's own verification passed 6/6 and explicitly carried this forward as a pre-release
recommendation rather than a blocking gap. Two requirements are affected, and they carry very
different amounts of residual risk:

**SOUND-06 (sound never ducks the player's own music/podcast) — low risk, confirmation only.**
Android ducks another app's audio only if the app *requests audio focus*. This app never does,
and that is enforced structurally: `audioFocusLiteralCheck` in
`build-logic/src/main/kotlin/standards.gradle.kts` fails the build if the API literal appears
anywhere in `src/main`. The pool is declared `USAGE_GAME` / `CONTENT_TYPE_SONIFICATION`, which
routes it to the media stream. So the property follows from the platform contract. A device run
confirms it against a real audio policy service (and any OEM quirk); it is not expected to
discover a defect.

**SOUND-01 (the first tap after a cold start is audible) — the real risk.**
`SoundPoolPlayer` tracks decoded samples in a `loaded` set populated by
`setOnLoadCompleteListener`, and `play()` drops any cue not yet in that set — playing it would
be silent anyway. So a cue fired before its decode completes is **silently lost, not queued**.
`preload()` is launched from `App.onCreate` and the assets are small (4–7 KB each; the star pop
17 KB), and the player must still cross Home into a level before a peg can be tapped, so there
is normally seconds of headroom against milliseconds of decode. That margin is narrowest on a
cold, loaded, low-end device — precisely the case an emulator models least faithfully.

## Solution

Before the v1.1 milestone closes, run both checks on a physical device. Prefer an older /
low-end handset over a flagship, since the cold-start margin is what is being probed.

1. **Cold start (SOUND-01):** force-stop the app, launch it, go straight into a level and tap a
   peg as fast as possible. The pick-up cue must be audible. Repeat three times. A missing
   first cue means the decode race is real on that hardware.
2. **Podcast (SOUND-06):** start a podcast or music in another app, switch to Peg Solitaire
   without pausing it, and play a full level through to Results — jumps, an undo, a hint, an
   invalid tap, the win, the star pops, the buttons. The other audio must never dip, fade or
   pause, game sound must ride the *media* volume slider, and nothing should stop when the game
   is backgrounded.

If check 1 fails, the fix is not a bigger preload — it is to stop dropping undecoded cues.
Options: block the first cue briefly on decode completion, or keep a tiny always-resident cue.
Decide only if it actually reproduces.

Two smaller items worth eyeballing in the same session, both recorded as open elsewhere:
- The pick-up cue's perceived balance against the jump cue was never isolated for judgement.
  Equal peak is not equal loudness and the envelopes differ. Fix if needed is `VOLUME_SELECT`
  alone — no re-encode, the asset is correctly peak-matched.
- `ClayAnim.STAR_POP_MS` (600 ms) is confirmed by human audition only; no automated test pins
  the star animation's on-screen duration.
