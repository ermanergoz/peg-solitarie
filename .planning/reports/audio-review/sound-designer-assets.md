# sound-designer report — audio-review: assets (Q-008 recipes, Q-009 WAV conversion)
Date: 2026-09-29
Branch: feature/audio-review (af2b554..10f7691)
Ran on: Opus (claude-opus-5-5), not the pinned Fable. Fable is exhausted, and the founder's Q-011 answer allows an exception scoped to this audio follow-up.
Skills loaded: ergoz-soft:standards (role: sound-designer). Rules: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, audio, ip-policy (snapshot 0.6.2 = plugin 0.6.2)
Ran in parallel with: audio-review research-analyst (licence verdicts), tech-lead (silent-mode design), ux-designer (wrong-tap feedback), all in the same worktree
Loops: 0
Lessons applied: none (the snapshot has 0 active lessons, and there is no `.planning/recall/RECALL.md`)

## Summary
- **Q-008.** The wrong tap is now the "soft tok" and the hint the "soft glint". Both recipes are committed under `audio/recipes/` and rendered with `sfx-synth.sh`. Each render matches the clip the founder auditioned byte for byte, and a second render matches too. Peg removal is kept, and the spec's P2 records that against SOUND-02.
- **Q-009.** The other ten cues are now mono 16-bit 44.1 kHz WAV. The seven Kenney cues come from the pack's own files; the pack was re-fetched and its hash pinned. The three freesound-derived cues come from their single-encode OGGs, because the masters need an account. **P4a** (jump peak −0.02 → −1.10 dBFS) and **P4b** (forward +4.14 dB, peak −1.00 dBFS) are applied. **P4c** is not: it needs a device listen first, and it is a code constant. No `.ogg` is left, each cue has one file, and the `R.raw` names are unchanged.
- **Loudness spread** (L100 in game, phone stand-in): **50 → 32 LU**, or 24 LU without Deselect. At full range it stays 31 LU (22 without Deselect). On disk it goes 20 → 17 LU. Sample peaks went from −14.0…−0.02 to −12.5…−1.00 dBFS, so every file now meets the −1 dBFS rule.
- **APK size:** +255,712 B stored. The clean debug package goes from 20,637,687 to 20,893,399 B, and the release APK from about 2.06 to 2.31 MB (+12.4 %). The release APK was not rebuilt, because that needs the signing secrets.
- **`./gradlew check` is red** only because of AudioCreditsTest, which fails as expected; the fix is the developer's. A `:domain:depthSweep` error in the same run (a missing Gradle results file) passes when the task is re-run alone.

## Outputs
- `audio/recipes/invalid.json` — the "soft tok" recipe, identical to `candidates/invalid_a.json`.
- `audio/recipes/hint.json` — the "soft glint" recipe, identical to `candidates/hint_a.json`.
- `app/src/main/res/raw/sfx_invalid.wav` (7,982 B) and `sfx_hint.wav` (19,448 B) — the in-house renders.
- `app/src/main/res/raw/sfx_{jump,peg_remove,select,deselect,undo,win,star_pop,btn_forward,btn_dismiss,sound_enabled}.wav` — the ten conversions, 5,962–145,574 B each. All twelve files total 327,394 B.
- The twelve `app/src/main/res/raw/sfx_*.ogg` files are deleted.
- `docs/audio/CREDITS.md` — the Files table uses `.wav` names and gains `author` and `sha256` columns. The in-house rows name their recipe path as the source. The file also gains the Kenney archive hash, a "WAV conversion and in-house renders" section with the exact commands, trims and gains, and a correction to the select/deselect RMS pair. The OGG-era sections are kept as history under scope notes.
- `docs/audio-spec.md` — the cue table (files, the two new characters, the wrong-tap rows aligned with the ux-designer's D-06) and the Measurements, Mix, Accessibility, Sourcing and Assets sections are updated. Proposed changes gives a status per founder answer, with Q-008's "keep" recorded against SOUND-02 in P2.
- This report.

## Checks
- `sfx-synth.sh --check` on both recipes: **pass**. Rendering each twice and running `cmp` against the committed WAV and against `candidates/sfx_{invalid,hint}_a.wav`: **pass**, byte-identical.
- The CREDITS.md conversion commands, re-run verbatim in a clean scratch directory from the pinned pack (sha256 `f2193d07…81232`) and `git show c5b02ef:…ogg`, then `cmp` against all twelve committed files: **pass**. Two runs give identical bytes, and the CREDITS `sha256` column matches all twelve.
- `ffprobe` on every file: `pcm_s16le`, 1 channel, 44,100 Hz, **pass**. Sizes: at most 145,574 B, **pass**. Sample peaks: at most −1.00 dBFS, **pass**. True peaks: at most −0.9 dBTP (forward); all others ≤ −1.1.
- `.planning/qa/audio-review/tools/measure.py` and `measure_phone.py`, run before and after: the eight kept cues are within 0.1 LU at full range (dismiss +0.3 LU on the phone stand-in). Jump −1.1 dB in game at `VOLUME_JUMP` 0.9 (−0.2 dB once it is 1.0). Forward +4.1 LU. Wrong tap −59.4 → −32.2 LUFS on the phone stand-in. Hint −23.8 → −23.6.
- Jump + removal layer, simulated on rungs 0–12 with linear resampling: **−2.02 dBFS** at `VOLUME_JUMP` 0.9, and **−1.10 dBFS** at 1.0. It was −0.93 before, a fail.
- `./gradlew :app:assembleDebug --offline` before and after, then `./gradlew :app:packageDebug --rerun --offline`: **pass**. All twelve `res/raw/sfx_*.wav` are packaged and Stored, and the APK delta is exactly +255,712 B. (An incremental package had shown +699,782 B, because zipflinger leaves free space behind; the clean repackage is the figure to use.)
- `./gradlew :app:assembleRelease --offline`: **fail, not audio**. It needs `PLAY_UPLOAD_KEYSTORE`, `…_KEY_ALIAS`, `…_KEY_PASSWORD` and `…_STORE_PASSWORD`, which are secrets from the environment only. I did not work around this.
- `./gradlew check --offline --continue`: **fail** (945 tests, 2 failed; detekt, ktlint, lint and both `audioFocusLiteralCheck` tasks pass):
  - `AudioCreditsTest > everyCommittedSoundHasACreditsRow FAILED` — `expected not to be empty` at `AudioCreditsTest.kt:35`.
  - `AudioCreditsTest > everyCreditsRowNamesASourceAndLicence FAILED` — `expected not to be empty` at `AudioCreditsTest.kt:44`.
  - Cause: `AUDIO_EXTENSION = "ogg"` (`:88`) keeps only `*.ogg`, and `res/raw` now holds only `*.wav`, so `soundFiles` is empty. This is the expected failure. **The test is not edited here.**
  - `:domain:depthSweep` — `java.nio.file.NoSuchFileException: …/domain/build/test-results/depthSweep/binary/in-progress-results-generic.bin`. The sweep itself printed its per-board results; the failure is Gradle's results file, in a module with no audio code. Re-run alone with `./gradlew :domain:depthSweep --offline --rerun`, it **passes** (BUILD SUCCESSFUL in 11m 12s). The results file probably vanished mid-run because another role ran Gradle in this shared worktree. It is not caused by this change.

## Open issues / risks
- **AudioCreditsTest** is red until the android-game-dev rewrites it to the tech-lead's contract (`docs/tech/audio-silent-mode.md` §2.3). CREDITS.md already follows that contract: one Files table read by header, `.wav` names, `CC0 1.0` on sourced rows, bare `audio/recipes/<stem>.json` on in-house rows, and a bare-hex `sha256` column.
- **P4a is half done** until `VOLUME_JUMP` goes 0.9 → 1.0 (tech-lead ruling B2). Until then the jump plays 1.1 dB quieter than before.
- **Stale KDoc:** `SoundCue.kt:44` and `SoundParams.kt:137,155` still say `sfx_deselect.ogg`. `VOLUME_DESELECT`'s KDoc also quotes an RMS gap of 1.92 dB. Over the designed lengths that gap is **0.73 dB** (−25.26 against −24.53 dB): the old −26.46 counted 945 samples of Vorbis padding. The audio is unchanged, the −10 dB was tuned by ear, and P4c re-checks it.
- **P4c** (Deselect at −41.5 LUFS on the phone stand-in) waits for the qa-engineer's listen.
- **The wrong tap is still sound-only**, which fails rules/audio.md accessibility, until the ux-designer's D-06 is built. That covers the head-shake, the reject haptic and the announcement.
- **Forward's true peak is −0.9 dBTP.** Its sample peak of −1.00 dBFS meets the rule as written, and in game at 0.7 it is −4.0 dBTP.
- **Three cues keep one lossy generation** (select, deselect, star pop). The freesound masters need an account, and a preview would be worse. If someone fetches the masters, re-render them per CREDITS.md.
- **For the research-analyst's verdict:** the embedded tags of 506546 name the artist as Mattias "MATTIX" Lahoud, while the freesound uploader is LilMati. The verdict should confirm that the uploader could dedicate the file to CC0.
- **The release APK was not rebuilt.** The release shrink and Stored check on the WAV set should be re-run by whoever holds the signing secrets. The ids are unchanged, so retention is expected.
- Founder questions: none.

## Handoff
Next: android-game-dev — rewrite `AudioCreditsTest` and add `AudioAssetFormatTest` per `docs/tech/audio-silent-mode.md` §2.3, set `VOLUME_JUMP` to 1.0 (P4a), sweep the `.ogg` KDoc and the deselect RMS figures, then run `./gradlew check`. This branch changes no product code.
Next: qa-engineer — on a device, check that every cue fires on its event and that none clicks or clips. In particular: the new wrong tap is audible on the speaker at half media volume; the hint; the jump ladder at `VOLUME_JUMP` 1.0; forward louder than dismiss; the P4c listen on Deselect.
Next: sound-designer (review mode) — review the developer's diff for the items above against rules/audio.md and this spec.
Also: research-analyst — the 506546 author note above. release-manager — the release shrink re-run.
