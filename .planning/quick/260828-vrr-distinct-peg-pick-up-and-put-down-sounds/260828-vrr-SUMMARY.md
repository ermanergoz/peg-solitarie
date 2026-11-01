---
phase: quick-260828-vrr
plan: 01
subsystem: audio
tags: [sound, sfx, ogg-vorbis, ffmpeg, soundpool, compose, cc0, provenance]

requires:
  - phase: 07-sound
    provides: SoundCue/SoundParams/SoundPoolPlayer, the cue-ordinal idiom in GameUiState, AudioCreditsTest, the release-shrink retention proof
  - phase: quick-260828-vb3
    provides: the encoded-peak sweep technique and the "verify by measured properties, never by hash" rule for libvorbis assets
provides:
  - A distinct put-down cue (sfx_deselect.ogg) derived from the pick-up by time reversal plus a whole-tone downshift
  - A replacement pick-up cue (sfx_select.ogg) from freesound 506546 "Pop 01" by LilMati, CC0 1.0
  - SoundCue.Deselect, the twelfth cue, and SoundParams.VOLUME_DESELECT derived at -10 dB from VOLUME_SELECT
  - GameUiState.deselectionTick, the fourth cue ordinal, splitting selection from deselection
  - A CREDITS.md with no claim the tree contradicts, including two pre-existing falsehoods corrected
  - A re-proven release-shrink retention result at 12 of 12 raw/sfx_ entries
affects: [phase-08-assist, phase-09-uat, any future audio re-levelling, any future res/raw addition]

tech-stack:
  added: []
  patterns:
    - "Retarget-by-re-sweep: when a peak target moves, re-run the encoded-peak sweep rather than interpolating, because libvorbis jitter makes the sweep non-monotonic"
    - "Derived-asset provenance: a synthesised file with no source page of its own cites its parent's URL and states the CC0 inheritance explicitly"
    - "Relative mix decisions live in SoundParams constants; absolute peak lives in the asset. Never both."

key-files:
  created:
    - app/src/main/res/raw/sfx_deselect.ogg
  modified:
    - app/src/main/res/raw/sfx_select.ogg
    - docs/audio/CREDITS.md
    - .planning/phases/07-sound/07-CONTEXT.md
    - app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt
    - app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt

key-decisions:
  - "Both cues shipped at -5.476 dBFS (the incumbent sfx_select's peak), NOT the plan's -3.3 dBFS. The -3.3 target rested on a false premise: there is no house normalisation level in this project."
  - "The put-down's +0.30 dB encoder compensation was re-swept at the new base rather than assumed to carry over from the -3.3 sweep."
  - "D-05a is half-reversed: the shared-cue half is gone, the never-fire-the-invalid-tap half stands and is now asserted in two specs."
  - "VOLUME_DESELECT is derived (VOLUME_SELECT * DESELECT_ATTENUATION), never a bare float, so the two constants cannot drift."
  - "The release-shrink proof was re-run because this added the first genuinely new res/raw id since it was written; vb3's payload-only exemption did not apply."

patterns-established:
  - "Cue ordinals: one ordinal per distinguishable event, enumerated at every state-rebuild site, never relying on the `= 0` default"
  - "Peak-matched assets with the level difference in code, so an attenuation is applied exactly once"

metrics:
  duration: ~75 min
  tasks: 3
  files: 12
  completed: 2026-08-28
---

# Quick Task 260828-vrr: Distinct Peg Pick-up and Put-down Sounds Summary

Picking a peg up and putting it back down now play two audibly different cues — freesound 506546 "Pop 01" by LilMati, and that same pop time-reversed and pitched down a whole tone — both shipped at the incumbent's level rather than the brief's, because the brief's target rested on a premise measurement did not support.

## What Was Built

`sfx_select.ogg` is now the CC0 LilMati pop, replacing Kenney's `select_002.ogg`. `sfx_deselect.ogg` is the twelfth asset in the set: the same normalised intermediate, `areverse`d, resampled down one whole tone (`asetrate=44100*0.8909`), and mixed 10 dB under its forward twin via `SoundParams.VOLUME_DESELECT`.

Under the hood, `selectionTick` used to mean two things — it advanced on both a selection and a re-tap deselect, which is what made the two events indistinguishable to a one-shot consumer and therefore what forced them to share a cue. It now means one thing. A new `deselectionTick` carries the put-down, enumerated in both `GameUiState` factories and carried forward at all four `GameViewModel` state-rebuild sites.

## Task Commits

| Task | Name | Commit |
|---|---|---|
| 1 | Encode the pair, repair the provenance record | `29a3480` |
| 2 | Split the tick, ship the twelfth cue, re-prove release retention | `abfd626` |
| 3 | Device listening checkpoint | (no commit — human gate) |

## The Retarget — the headline result

**The plan targeted -3.3 dBFS for both cues. They shipped at -5.476 dBFS instead.**

The -3.3 figure came from a false premise in the coordinator's brief: that the asset set is normalised to -3.3 dBFS. It is not. Measured across the eleven committed files on 2026-08-28:

| cue | peak (dBFS) | | cue | peak (dBFS) |
|---|---|---|---|---|
| `sfx_jump` | -0.018 | | `sfx_undo` | -4.971 |
| `sfx_win` | -1.582 | | `sfx_btn_forward` | -5.231 |
| `sfx_hint` | -2.539 | | `sfx_select` | **-5.476** |
| `sfx_star_pop` | -3.338 | | `sfx_peg_remove` | -5.959 |
| `sfx_sound_enabled` | -4.897 | | `sfx_btn_dismiss` | -7.961 |
| | | | `sfx_invalid` | -14.008 |

The set spans **14 dB** and is a deliberate per-cue balance. -3.3 dBFS was only ever the *star pop's* target, inherited from the file that cue replaced in quick task 260828-vb3.

**Consequence of the correction.** Shipping at -3.3 would have made peg selection — the most frequently heard sound in the game — **2.17 dB louder** than the cue it replaces, at an unchanged `VOLUME_SELECT`. That was never requested. The user tuned the pick-up/put-down pair by ear for their *relative* difference, not for absolute level against the rest of the mix. Matching the incumbent's -5.476 keeps this change scoped to "a different sound" rather than "a different **and louder** sound". **Peg selection did not get louder.**

### The re-sweep, and why interpolation was not an option

The plan documents that libvorbis jitters the encoded peak by ~0.2 dB between neighbouring pre-encode gains, which makes the sweep non-monotonic and interpolation unsound. That held at the new target too — the sweep is visibly not ordered:

| pre-encode gain | encoded peak |
|---|---|
| -5.40 dB | -5.382 dBFS |
| -5.50 dB | -5.341 dBFS |
| -5.60 dB | -5.321 dBFS |
| **-5.65 dB** | **-5.483 dBFS** |
| -5.70 dB | -5.576 dBFS |
| -5.80 dB | -5.860 dBFS |

A linear interpolation from the plan's -3.3 sweep would have predicted roughly -5.67 dB and landed between the -5.60 (-5.321) and -5.70 (-5.576) samples with no way to know which. **-5.65 dB** was measured, not derived: it gives **-5.483288 dBFS**, within **0.008 dB** of the incumbent.

The put-down's **+0.30 dB** compensation was re-swept at the new base rather than assumed to survive. It does — **-5.464545 dBFS**, within 0.019 dB of its forward twin — but that was confirmed by measurement, not by carrying the plan's number forward.

The RMS relationship that justifies `VOLUME_DESELECT` also re-verified at the new level: the put-down reads **+1.92 dB RMS** hotter at matched peak (the plan measured 1.90 dB at the old level), because a reversed transient is a slow swell and a swell carries more energy relative to its peak than a sharp attack does.

### Gates retargeted — stated, not silently left asserting a number I did not hit

The plan's two `astats` acceptance gates asserted `-3.5 .. -3.0 dBFS`. **I ran them at `-5.726 .. -5.226`** — the same ±0.25 dB window, recentred on the new target. Both pass. Every other gate in the plan ran unmodified.

`docs/audio/CREDITS.md` now records the -5.476 target, the sweep that found it, and an explicit statement that there is no set-wide normalisation level — so the next person to read it cannot re-derive the same false premise.

## Verification Results

| Check | Result |
|---|---|
| Master md5 | `2e5ddefd4d3741ad4221245950904ded` — matches the audited master |
| Both files codec/channels/rate | vorbis / 1 / 44100 |
| Durations | 0.067098 s and 0.075329 s |
| Encoded peaks | -5.483288 and -5.464545 dBFS (retargeted window) |
| Duration ratio | **1.122671** vs `2^(2/12)` = 1.122462 — the whole tone is present |
| Reversal correlation | **0.9875** after undoing both transforms |
| Negative control: plain reversal (round-1 variant) | 0.5393 — correctly fails |
| Negative control: copy of the pick-up | 0.0101 — correctly fails |
| `AudioCreditsTest` | passed in a **forced** `--rerun` run; `:app:testDebugUnitTest` executed, not UP-TO-DATE |
| `./gradlew :app:check --offline` | green (detekt and dpSpLiteralCheck both force-run to confirm) |
| Release shrink | **12 of 12** `raw/sfx_` entries; all twelve byte-identical to source and **Stored** |

The correlation gate was re-validated against its negative controls **at the new level**, not just trusted from the plan's dry-run — a level change could in principle have altered its discrimination. It did not.

## The Human Gate (Task 3) — what was and was not confirmed

**The approval was global, not itemised.** The plan's `how-to-verify` listed nine numbered checks and the checkpoint asked the user to rule on four named risks. The user replied **"all good"** rather than answering any of them individually. The reported outcome — cues distinct, still siblings at a whole tone, put-down audible despite being the quietest cue, 75 ms keeps up with rapid tapping, invalid tap untouched — is a global judgement, not per-step evidence. **No per-step result should be quoted from this run as though it were separately attested.**

**The confirmation is emulator-grade, not hardware.** Only `emulator-5554` was attached; the build was installed and launched there. No physical device was stated. This matters most for the put-down's **level**: at `VOLUME_DESELECT` ≈ 0.19f it is now the quietest cue in the game, below even the invalid tap, and "does it survive a real phone speaker" is precisely the question an emulator cannot answer. **That check remains genuinely unproven on hardware.** This repeats the limitation already recorded for phase 7's own audit (T-07-04 and T-07-16 are likewise emulator-only).

## Known-Open Items

- **Pick-up level against the jump was never isolated.** Equal peak is not equal perceived loudness, and the new pop has a different envelope from the Kenney tick it replaces — the same effect that forced `VOLUME_DESELECT` to exist in the first place. I raised this at the checkpoint; the user approved globally without being asked to isolate it, so it was **not** verified. If the pick-up ever feels wrong against the jump, the fix is **`VOLUME_SELECT` alone — one constant, no re-encode**, because the asset is correctly peak-matched to its predecessor.
- **The put-down on a physical phone speaker** (see above).

## Deviations from Plan

### Directed deviation

**1. [Coordinator correction] Peak target retargeted from -3.3 dBFS to -5.476 dBFS**
- **Found during:** pre-execution; the coordinator corrected its own brief after the planner flagged the premise
- **Issue:** the plan's -3.3 dBFS target assumed a house normalisation level that does not exist; honouring it would have made the game's most frequent cue 2.17 dB louder without a request
- **Fix:** re-swept both encodes at the new target and re-verified the put-down's compensation there; retargeted the two acceptance gates and documented the change in `CREDITS.md`
- **Files modified:** both `.ogg` assets, `docs/audio/CREDITS.md`
- **Commit:** `29a3480`

### Auto-fixed issues

**2. [Rule 1 - Bug] The new section's measurement table created a second CREDITS.md row per filename**
- **Found during:** Task 1 gate run
- **Issue:** I rendered the shipped-pair measurements as a markdown table. Its rows start with `|` and contain the filenames, so `AudioCreditsTest.tableRowFor` would have had two candidate "provenance rows" per file, and the plan's exactly-one-row gates failed. A duplicate row is a second, unpoliced place for a provenance claim to drift.
- **Fix:** converted the measurement table to a bullet list, and recorded *in the document* why it is not a table
- **Commit:** `29a3480`

**3. [Rule 1 - Bug] A negated KDoc sentence tripped a stale-claim gate**
- **Found during:** Task 2 gate run
- **Issue:** the plan gates `grep -Fc 'the quietest cue in the set'` to zero, to prove the superseded claim is gone. My replacement KDoc read "It is no longer **the quietest cue in the set** outright" — semantically correct, but a substring gate cannot tell a negation from an assertion
- **Fix:** reworded both sites to "the softest cue the game plays". The gate is right to be blunt here; arguing with it would have left a future reader grepping for a phrase that still appears
- **Commit:** `abfd626`

### Not deviations, but worth recording

**Two pre-existing CREDITS.md falsehoods were corrected in the same pass**, both found by measurement and neither introduced by this change:
- "the win stinger the loudest at −1.5 dBFS" — `sfx_jump` peaks at **-0.018 dBFS** and is the loudest file in the set. The win stinger is the loudest *celebration* cue, which is what the sentence should have said.
- "0.13–0.54 s for the ten Kenney cues" — the range silently excluded the then-incumbent `sfx_select.ogg` at 0.043 s. The nine remaining Kenney cues span **0.097–0.539 s**.

**One claim this change created and immediately retired.** "The invalid tap is the quietest cue" was asserted in KDoc in both `SoundCue.kt` and `SoundParams.kt`. `VOLUME_DESELECT` (0.18972f) is now below `VOLUME_INVALID` (0.3f), so both were rewritten — and `thePutDownIsTheQuietestCueInTheSet` pins the new truth, so the supersession is recorded in a test rather than only in prose. SOUND-02 is untouched: its point is that a rejection must not punish the player *relative to the jump it denies* (0.3 against 0.9), which `theRejectedTapIsQuieterThanTheJump` still guards.

## Decisions Recorded

- **D-05a is half-reversed.** The "reuses the selection sound — symmetric feedback, no extra asset" half is gone: pick-up and put-down have distinct cues and the asset set grew to twelve. The other half **stands**: a deselect is a SUCCESSFUL action and must never fire the invalid-tap cue. The deselect branch stays above the rejected-tap `else`, which makes the invalid cue *structurally unreachable* from a deselect rather than merely unused there. Now asserted in both `GameViewModelSoundTest` (`rejectedTapTick` stays 0) and `GameScreenSoundTest` (`doesNotContain(InvalidTap)`). The dated amendment is in `07-CONTEXT.md`.
- **`selectionTick` no longer means two things.** `deselectionTick` is the fourth cue ordinal. It must be enumerated in both `GameUiState` factories and carried at all **four** `GameViewModel` rebuild sites. The `= 0` default cannot be relied on to cover a missed site — a fresh `0` is a *changed* key for a `LaunchedEffect` sitting higher, so a default is exactly what would hide the bug. That is why the carry-forward sites are gated by exact-expression counts rather than an "appears somewhere" check, and why the Challenge-load and resume-restore paths (where no deselect is reachable, so no behavioural test can catch a drop) rely on those counts alone.
- **The asset set is not normalised to one level.** It is a per-cue peak balance spanning -0.018 dBFS (`sfx_jump`, the loudest file — not the win stinger, as CREDITS.md wrongly claimed until now) to -14.008 dBFS (`sfx_invalid`).
- **The put-down is the quietest cue in the mix** at `VOLUME_DESELECT` ≈ 0.19f. The reversed cue needs its own constant because it is measurably more present than its forward twin at equal peak (+1.92 dB RMS).
- **D-03's cohesion has moved again.** Nine of twelve files are Kenney; three come from two freesound sounds. The borrows now include the most frequently heard gameplay cue and its twin, so the borrowed material is at the centre of the mix rather than at its rim.
- **The release-shrink retention proof was re-run** because this change added the first genuinely new `res/raw` id since it was written. 260828-vb3's "only the payload changed" exemption did not apply — a new id the shrinker cannot trace is exactly the silent-in-release-only failure that section exists to catch.

## Threat Flags

None. The plan's threat register covered every surface this change touches (T-07-01 asset tampering, T-VRR-01/02 provenance repudiation, T-VRR-03 stale test result, T-07-15 shrinker retention), and all `mitigate` dispositions were discharged. No new network endpoint, auth path, file-access pattern or schema change was introduced.

## Known Stubs

None.

## Self-Check

- `app/src/main/res/raw/sfx_deselect.ogg` — FOUND
- `app/src/main/res/raw/sfx_select.ogg` — FOUND
- Commit `29a3480` — FOUND
- Commit `abfd626` — FOUND

## Self-Check: PASSED
