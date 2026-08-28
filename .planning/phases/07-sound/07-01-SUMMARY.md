---
phase: 07-sound
plan: 01
subsystem: audio-assets
tags: [audio, assets, licensing, build-gate, cc0]
requires: []
provides:
  - "R.raw.sfx_* — the eleven compile-time sound resource ids plans 07-02+ bind to"
  - "docs/audio/CREDITS.md — D-01 per-file licence provenance record"
  - "audioFocusLiteralCheck — CI enforcement of SOUND-06's never-request-focus negative"
affects:
  - "build-logic/src/main/kotlin/standards.gradle.kts (check now depends on a fifth gate)"
tech-stack:
  added: []
  patterns:
    - "File-scan Gradle verification task cloned from adIdLiteralCheck"
    - "Runner-free JVM test asserting on working-tree files"
key-files:
  created:
    - app/src/main/res/raw/sfx_jump.ogg
    - app/src/main/res/raw/sfx_peg_remove.ogg
    - app/src/main/res/raw/sfx_select.ogg
    - app/src/main/res/raw/sfx_undo.ogg
    - app/src/main/res/raw/sfx_invalid.ogg
    - app/src/main/res/raw/sfx_hint.ogg
    - app/src/main/res/raw/sfx_win.ogg
    - app/src/main/res/raw/sfx_star_pop.ogg
    - app/src/main/res/raw/sfx_btn_forward.ogg
    - app/src/main/res/raw/sfx_btn_dismiss.ogg
    - app/src/main/res/raw/sfx_sound_enabled.ogg
    - docs/audio/CREDITS.md
    - app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt
  modified:
    - build-logic/src/main/kotlin/standards.gradle.kts
decisions:
  - "All eleven cues sourced from Kenney Interface Sounds alone — D-03 satisfied fully, zero freesound borrows"
  - "Peak normalisation per cue instead of the planned loudnorm, because most cues are shorter than loudnorm's analysis window"
metrics:
  duration: ~35 min
  completed: 2026-08-28
---

# Phase 7 Plan 01: Sound Assets, Provenance & Focus Gate Summary

Eleven CC0 candy-clay SFX from a single Kenney pack now live in `res/raw` with a per-file
licence provenance table that a JVM test keeps complete, and `./gradlew check` now fails on any
audio-focus request in `src/main`.

## What Was Built

**Task 1 — the eleven sound files** (`3dfb91b`). Kenney *Interface Sounds* (CC0 1.0) was
downloaded over HTTPS, inspected with `unzip -l` **before** extraction, selectively extracted,
and eleven clips were converted into `app/src/main/res/raw/` under the exact names the
`<interfaces>` contract specifies. All eleven come from the anchor pack, so D-03's "one cohesive
pack family" holds with no borrowed clips.

Candidate selection was driven by an objective acoustic pass over all 100 pack sounds (duration,
mean spectral centroid, peak level), picking low-centroid/short samples for the soft round cues
and brighter ones where the contract asks for sparkle:

| res/raw name | source clip | why |
|---|---|---|
| `sfx_jump.ogg` | `drop_003.ogg` | round low "boop", centroid 782 Hz — the signature pop |
| `sfx_peg_remove.ogg` | `drop_002.ogg` | same family as jump, sits under it as texture |
| `sfx_select.ogg` | `select_002.ogg` | 43 ms light tick |
| `sfx_undo.ogg` | `minimize_008.ogg` | dark downward sweep — the jump in reverse character |
| `sfx_invalid.ogg` | `bong_001.ogg` | dull 507 Hz thud, mixed quietest — non-punishing |
| `sfx_hint.ogg` | `pluck_002.ogg` | pluck/chime, distinct from the button clicks |
| `sfx_win.ogg` | `confirmation_002.ogg` | celebratory stinger, 0.54 s |
| `sfx_star_pop.ogg` | `glass_005.ogg` | bright bubble pop for the pitch ladder |
| `sfx_btn_forward.ogg` | `click_001.ogg` | brighter forward/commit click (D-11) |
| `sfx_btn_dismiss.ogg` | `back_004.ogg` | softer, darker dismiss click (D-11) |
| `sfx_sound_enabled.ogg` | `confirmation_001.ogg` | soft confirmation pop (D-12) |

Every file is mono 44.1 kHz Vorbis, head-trimmed, and under 0.7 s — far below `SoundPool`'s 1 MB
decoded-PCM per-sample cap (Pitfall 5). Relative balance is encoded in per-cue peak targets:
`sfx_invalid.ogg` is the quietest at −14 dBFS, `sfx_win.ogg` the loudest at −1.5 dBFS. No
`noCompress` configuration was added — `.ogg` is already in aapt2 9.1.0's built-in list.

**Task 2 — provenance** (`c60a033`, formatting follow-up `0957bd5`). `docs/audio/CREDITS.md`
carries the four-column table D-01 requires plus the Pitfall-7 rename column, the pack's verbatim
CC0 licence text, the processing notes, and a `## Verification` section recording the T-07-01
evidence. `AudioCreditsTest` is a runner-free JVM test (no `@RunWith`, no `android.*`/`androidx.*`
import) that walks up to the directory holding `settings.gradle.kts` to find the repo root, then
asserts every `res/raw/*.ogg` has a **table row** (prose mentions do not count) with at least four
non-blank cells, an `https://` source and a non-blank licence.

**Task 3 — the SOUND-06 gate** (`d1c4423`). `audioFocusLiteralCheck` clones the `adIdLiteralCheck`
shape exactly — same `tasks.register`, `verification` group, `walkTopDown()` scan, `path:line: text`
offender format and `GradleException` — reusing `adIdSourceRoots` (so `src/test` may still name the
API) and `commentPrefixes`. It fails on `requestAudioFocus` or `abandonAudioFocusRequest`, and the
header KDoc now reads "Wires five checks".

## Key Decisions

**Peak normalisation instead of `loudnorm`.** The plan specified `loudnorm=I=-16:TP=-1.5`, but
`loudnorm`'s dynamic mode analyses over roughly a 3-second window and most of these cues are an
order of magnitude shorter (`sfx_select.ogg` is 43 ms), where it produces unusable gain. Each file
is instead peak-normalised to a per-cue target measured with `astats`. This is deterministic,
safe at any length, and directly expresses the relative-volume balancing the requirements ask for.
Documented in the CREDITS "Processing" section.

**The two `.url` files were excluded, not extracted.** The archive listing contained `License.txt`,
100 `.ogg` files, and two 45/55-byte plain-text Windows shortcut files. These are not executables,
scripts or nested archives, so the plan's STOP condition did not trigger — but they fall outside the
audio + licence allow-list, so extraction was scoped to `Audio/*` and `License.txt` only.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `loudnorm` unusable on sub-100 ms clips**
- **Found during:** Task 1
- **Issue:** The planned `loudnorm=I=-16:TP=-1.5` filter cannot meaningfully measure clips far
  shorter than its analysis window; applying it to a 43 ms sample yields arbitrary gain.
- **Fix:** Replaced with `astats`-measured peak normalisation to explicit per-cue dBFS targets,
  which also implements the requirement's relative-volume balancing more directly.
- **Files modified:** all eleven `app/src/main/res/raw/*.ogg`
- **Commit:** `3dfb91b`

**2. [Rule 1 - Bug] ktlint failure in the new test**
- **Found during:** Task 3 verification (`:app:check`)
- **Issue:** `ktlintTestSourceSetCheck` failed — a multiline chain in `AudioCreditsTest` did not
  start on a new line. CLAUDE.md makes ktlint a build gate, not optional formatting.
- **Fix:** Wrapped the chain onto its own lines.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt`
- **Commit:** `0957bd5`

## Verification Performed

- `ffprobe` confirms `vorbis` codec and `channels=1` for all eleven files; durations 0.043–0.539 s,
  all under the 0.7 s cap.
- `./gradlew :app:assembleDebug --offline` exits 0 — aapt2 accepted every resource name.
- `./gradlew :app:testDebugUnitTest --tests "…AudioCreditsTest" --offline` runs **2 tests, 0
  failures** (confirmed in the JUnit XML report).
- **Negative test of the credits test:** removing the `sfx_star_pop.ogg` row made *both* tests fail;
  the row was restored and they pass again.
- `./gradlew :app:audioFocusLiteralCheck` and `:domain:audioFocusLiteralCheck` both exit 0.
- `./gradlew :app:check --offline --dry-run` includes `audioFocusLiteralCheck`.
- **Negative test of the gate:** a file containing `val x = "requestAudioFocus"` under
  `app/src/main/java/` failed the gate with `src/main/java/GateProbe.kt:1: …`. Both comment forms
  (`// requestAudioFocus …` and ` * requestAudioFocus …`) left the gate passing — the KDoc case plan
  07-02 depends on. The probe file was deleted.
- `./gradlew :app:check --offline` and `:domain:check --offline` both BUILD SUCCESSFUL.
- `grep -r noCompress app/build.gradle.kts build-logic/` returns no match.

## Caveat for Later Plans

`AudioCreditsTest` reads `res/raw/` and `CREDITS.md` from the working tree at runtime, so neither is
a declared Gradle task input. Editing only `CREDITS.md` leaves `:app:testDebugUnitTest` UP-TO-DATE
and the test will not re-run until something on the test classpath changes (or `--rerun-tasks` is
passed). It still gates any commit that touches Kotlin, which is every realistic path to adding a
sound. Worth knowing if a future plan changes assets alone.

## Requirements

SOUND-01..04 and SOUND-06 are **not** marked complete: this plan lands the raw material and the
SOUND-06 enforcement mechanism, but no sound plays until the player and cue wiring arrive in later
plans of this phase. `REQUIREMENTS.md` is deliberately untouched.

## Threat Flags

None. This plan adds no network endpoint, auth path or schema change. The one trust boundary it
crosses (internet → repository) was the plan's own T-07-01 and is mitigated as specified.

## Known Stubs

None.

## Self-Check: PASSED

All five representative created/modified files verified present on disk; all four commit hashes
(`3dfb91b`, `c60a033`, `0957bd5`, `d1c4423`) verified present in `git log`.
