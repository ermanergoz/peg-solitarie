# sound-designer report — audio-review
Date: 2026-09-29
Branch: feature/audio-review (24f11da..this commit)
Skills loaded: ergoz-soft:standards (role: sound-designer). Rule files read: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, ip-policy.md, from the v0.6.2 snapshot, which matches plugin 0.6.2.
Ran in parallel with: none
Ran on: opus (claude-opus-5-5) at effort xhigh — a **staffing deviation**, see below
Fan-out: none
Loops: 0
Lessons applied: none (the v0.6.2 snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Staffing deviation
The agent file `agents/sound-designer.md` pins `model: fable`, `effort: xhigh`. The Fable allowance
is exhausted, and the founder granted a **one-time** exception for this task only, recorded in
`.planning/questions/FOUNDER.md` Q-006 (answered 2026-09-29: "run it on opus only for once"). This
review therefore ran on Opus. It is not a standing policy change: the next Fable step that cannot
get Fable fails and is queued again. No product code was written, so the model rule about
reviewing code on a higher model is not engaged. The spec and this report are Opus-written, so
under studio.md any check of them by a fresh agent should run on Fable once Fable is available.

## Summary
I reviewed all 12 cues against rules/audio.md, ip-policy.md and CLAUDE.md: each one traced to its
event, measured, and checked against its neighbours. I also reviewed the playback integration and
wrote `docs/audio-spec.md`, the first cue list of record. The overall verdict is **FAIL, on 6 of 15
items**. The invalid tap has no visual partner and is inaudible on a phone speaker. Peg removal is
the jump sound again. Silent mode is ignored. The files are OGG, not WAV. The jump peaks at
−0.02 dBFS. There is no THIRD_PARTY.md or research-analyst verdict on develop. All three sources
still show CC0 on their live pages. I rendered five in-house candidates plus A/B clips (not
committed) and drafted three founder questions.

## Outputs
- `docs/audio-spec.md` — the cue table as it stands (event → cue → file → partner → source, with
  code lines), the phase-15 tutorial and dock rulings, measurements, mix targets, settings,
  accessibility, assets, and **Proposed changes** ranked by player impact (commit 24f11da).
- `.planning/reports/audio-review/sound-designer.md` — this report.
- Local only, git-ignored, **not committed** (`.planning/qa/audio-review/`, 1.1 MB):
  - `candidates/peg_remove_a.json` + `sfx_peg_remove_a.wav` (9,746 B) — removal "clay squish", 110 ms
  - `candidates/peg_remove_b.json` + `sfx_peg_remove_b.wav` (7,100 B) — removal "pfft", 80 ms
  - `candidates/invalid_a.json` + `sfx_invalid_a.wav` (7,982 B) — invalid "soft tok", 90 ms
  - `candidates/invalid_b.json` + `sfx_invalid_b.wav` (10,628 B) — invalid "muted bwop", 120 ms
  - `candidates/hint_a.json` + `sfx_hint_a.wav` (19,448 B) — hint "soft glint", 220 ms (optional)
  - `candidates/ab/ab_jump_run_with_remove_{incumbent,a,b}.wav` (147–154 KB each) — a
    three-jump same-peg run on the real pitch ladder, removal layered at +60 ms, at in-game volumes
  - `candidates/ab/ab_select_invalid_x2_deselect_{incumbent,a,b}.wav` (157 KB each) — pick-up,
    two invalid taps, put-down, at in-game volumes
  - `candidates/ab/ab_hint_{incumbent,a}.wav` (41 KB, 46 KB)
  - `tools/*.py` — the stdlib measuring scripts (K-weighted L100/L400, the phone-speaker
    stand-in, spectra and envelopes, cross-correlation); `measurements/*.txt` — their raw output

## Integration review (rules/audio.md, spec, CLAUDE.md) — FAIL
| # | Item | Verdict | Reason |
| --- | --- | --- | --- |
| 1 | One UI-layer class owns playback, never game-core | PASS | `audio/SoundPoolPlayer.kt` (app module, `@Singleton`, bound in `di/AudioModule.kt`); `:domain` has no audio |
| 2 | Preloads | PASS | all 12 `SoundCue` entries are loaded once from `App.onCreate` on the application scope (`Dispatchers.Default`, `App.kt:102`, `SoundPoolPlayer.kt:135-141`) |
| 3 | Triggers on state changes | PASS | tick ordinals, `MoveTransition.id`, `hintSource` and `status`, each as its own `LaunchedEffect` (`GameScreen.kt:321-429`) |
| 4 | Every cue wired to its event | PASS on phase 15, FAIL on develop | all 12 are wired (spec Cue table). On develop, the `PegRemove` layer is cancelled when OS animations are off; fixed by 3400828 on `feature/phase-15-ux-refresh` |
| 5 | Mute honoured | PASS | `soundOn` gate in the player, and in-flight streams are stopped (`:128-131`, `:206-212`); the enable pop bypasses it by design (D-12) |
| 6 | Volume honoured | PASS | no in-app volume exists; `USAGE_GAME` → media stream, so the system media volume controls it; no audio focus is ever requested |
| 7 | System silent mode honoured | **FAIL** | no ringer-mode check anywhere; Silent and Vibrate do not affect the media stream |
| 8 | No audio work on the main thread beyond the play call | PASS (note) | decode runs off the main thread; `pool.play` and a trivial `synchronized` bookkeeping step run on main. Note: the `SoundPool` is constructed during Hilt field injection in `App.onCreate` on the main thread — allocation only, no decode |
| 9 | Every cue paired with a visual or haptic | **FAIL** | `InvalidTap` has no partner; the board does not change (`GameUiState.kt:79-83`) |
| 10 | Format mono 16-bit 44.1 kHz WAV | **FAIL** | all 12 are Vorbis `.ogg` (mono and 44.1 kHz do conform) |
| 11 | ≤ 200 KB per SFX | PASS | largest is 17,726 B |
| 12 | Peak ≤ −1 dBFS per asset and mix | **FAIL** | `sfx_jump` −0.02 dBFS; the jump + removal mix −0.93 dBFS; everything else passes |
| 13 | Nothing plays unnamed; no unused asset | PASS | 12 cues, 12 assets, all named in the spec |
| 14 | Sourced assets: THIRD_PARTY.md row + research-analyst verdict | **FAIL** | no `THIRD_PARTY.md` on develop; no verdict for any file |
| 15 | In-house assets re-render byte-identically | n/a | no in-house asset ships; the 5 candidates each re-render byte-identically |

## Findings, ranked by severity
1. **High — the invalid tap gives no feedback on a phone.** 99.9 % of its energy is below 500 Hz
   (a 236 Hz bong). Through the phone-speaker stand-in it measures **−59.4 LUFS** in game, 50 LU
   under the win. It also has no visual or haptic partner. With sound off, on a phone speaker, or
   for a player who cannot hear, a wrong tap does nothing observable. (P1: an in-house replacement
   plus a ux-designer/developer visual partner.)
2. **High — peg removal is the jump again.** `drop_002` and `drop_003` have a third-octave
   spectrum similarity of 0.998 and an envelope similarity of 1.000. The waveform correlation is
   0.87 once a 0.8-semitone shift is removed. From the second jump of a run, the jump's pitch lands
   on the removal's. It plays on every jump, and SOUND-02 asks for distinct removal feedback. (P2)
3. **Medium — silent and vibrate modes are ignored** (rules/audio.md). (P3; tech-lead; founder
   taste in Q-010)
4. **Medium — the format is OGG, not WAV** (rules/audio.md). Conforming costs about +255 KB
   (+12 % APK). (P5; Q-009)
5. **Medium, blocks the release — provenance is incomplete.** There is no `THIRD_PARTY.md` on
   develop. A draft on the unmerged `feature/phase-14-release-docs` (e5253f4) has four audio rows,
   groups the nine Kenney files in one row, and has no verdict column. `docs/audio/CREDITS.md`
   gives a source URL and licence for all 12 files, but authors appear only in prose, with no
   author column. The Kenney pack archive is not hash-pinned (the freesound masters are). The
   licences themselves check out: rechecked 2026-09-29, Kenney *Interface Sounds* shows "Creative
   Commons CC0", and freesound 506546 (LilMati) and 531512 (CogFireStudios) show "Creative Commons
   0", with specs matching the pinned masters. No file carries an unverifiable licence; what is
   missing is the research-analyst's formal verdict. (P6)
6. **Low — the jump peaks at −0.02 dBFS**, and the jump + removal mix at −0.93 dBFS. (P4a)
7. **Low — the button levels are inverted.** `ButtonDismiss` is 3.8 LU *louder* than
   `ButtonForward` in game, although `SoundCue.kt` calls it softer. Forward is also quieter than
   the peg pick-up. (P4b: +4.2 dB on the forward asset, no code change)
8. **Low — `Deselect` at −41.5 LUFS** through the stand-in is at the edge of audibility. The
   founder tuned it, so QA listens on a device first. (P4c)
9. **Low — the removal layer drops with animations off** on develop. It is fixed on phase 15.
   (P9)
10. **Low — nothing happens on background.** One-shots ring for up to about 2.9 s. (P8)
11. **Nit — the hint is the brightest cue** (centroid 4.5 kHz), a little glassy for the candy-clay
    brief. (P7, optional)
12. **Nit — the `SoundParams.MAX_STREAMS` KDoc overlap arithmetic uses 1.65 s for every star pop.**
    At rates 0.63 and 0.79 the first two pops last 2.62 s and 2.08 s, so all three ring until about
    2.9 s. The 4-of-8-voices conclusion still holds. (android-game-dev, comment only)

**Loudness spread (L100, K-weighted, loudest 100 ms):** 20 LU on disk (−29.7 to −9.6 LUFS); 31 LU
in game after the `SoundParams` volumes (−40.2 to −9.6); 50 LU through the phone-speaker stand-in
(−59.4 to −9.7), or 32 LU without the invalid tap. Sample peaks span 14 dB (−14.0 to −0.02 dBFS).

## Checks
- `ffprobe -v error -select_streams a:0 -show_entries stream=codec_name,channels,sample_rate,sample_fmt,bits_per_sample:format=duration,size,bit_rate -of default=nw=1:nk=0 <file>` on all 12 files — fail against rules/audio.md (vorbis, 1 ch, 44100 Hz, 0.067–1.650 s, 4,087–17,726 B; the format is not WAV)
- `ffmpeg -i <file> -af astats=metadata=0:measure_perchannel=none:measure_overall=Peak_level+RMS_level+RMS_peak -f null -` on all 12 — fail (`sfx_jump` peak −0.017829 dBFS > −1; the other 11 are ≤ −1.58)
- `ffmpeg -i <file> -af "apad=pad_dur=1,ebur128=peak=true:framelog=quiet" -f null -` on all 12 — pass as a cross-check (true peaks match the sample peaks within 1.2 dB; L400 matches the script to ±0.1 LU on sub-400 ms cues)
- `python3 .planning/qa/audio-review/tools/measure.py <cue=volume,…> app/src/main/res/raw/*.ogg` — measurement only (the spec's Measurements table)
- `python3 .planning/qa/audio-review/tools/measure_phone.py <cue=volume,…> app/src/main/res/raw/*.ogg` — fail (`sfx_invalid` −59.4 LUFS in game through the stand-in)
- `python3 .planning/qa/audio-review/tools/spectral.py app/src/main/res/raw/*.ogg` and `xcorr.py sfx_jump.ogg sfx_peg_remove.ogg` — fail (jump/removal spectrum 0.998, envelope 1.000, waveform 0.865 at rate 0.9544)
- `ffmpeg … -filter_complex "…amix=inputs=2:normalize=0…,astats…"` simulating jump + removal at rungs 0–3 and the win + three star pops — fail / pass (−0.93 dBFS; −1.58 dBFS)
- `"/Users/ermanergoz/Desktop/ergoz-soft/scripts/sfx-synth.sh" --check <recipe>` for the 5 candidate recipes — pass (ok ×5)
- Render each candidate twice with `sfx-synth.sh` and `cmp` the two renders — pass (5/5 byte-identical; `ffprobe`: pcm_s16le, 1 ch, 44100 Hz, 7.1–19.4 KB)
- `./gradlew --offline :app:testDebugUnitTest --tests 'com.app.pegsolitaire.audio.*'` — pass (31 tests: AudioCreditsTest 2, SoundParamsTest 18, SoundPoolPlayerTest 11; 0 failures)
- `./gradlew --offline check` — pass (BUILD SUCCESSFUL in 11m 18s, 98 tasks; docs-only change, run for the definition of done)

## Drafted founder questions (for the producer to queue; I did not edit FOUNDER.md)
The ids are provisional. Q-007 is the newest on develop, and Q-003 to Q-005 are on the phase-15
branch.

    ## Q-008 — Pick new sounds for peg removal and the wrong-tap   (open)
    Date: 2026-09-29    Asked by: sound-designer
    Context: The removal sound is the jump sound again, so every jump sounds doubled; the wrong-tap
    "bong" is too low for a phone speaker to play. Listen at in-game level:
    .planning/qa/audio-review/candidates/ab/ (incumbent vs a vs b for each).
    Options: 1) removal "clay squish" (a) + wrong-tap "soft tok" (a) 2) removal "pfft" (b) +
    wrong-tap "muted bwop" (b) 3) mix and match — name one of each 4) keep both as they are.
    Optional in the same answer: hint "soft glint" (ab_hint_a) instead of the bright pluck; put-down
    6 dB instead of 10 dB under the pick-up if QA cannot hear it on a phone.
    Recommendation: sound-designer — 1: the squish is the most unlike the jump while staying under
    it, and the tok is the gentlest "nope" a phone speaker still plays, at the pick-up's level. Keep
    the hint as it is unless you dislike it.
    Blocks: nothing
    Answer:

    ## Q-009 — Sound file format: convert to WAV, keep OGG, or remake in-house?   (open)
    Date: 2026-09-29    Asked by: sound-designer
    Context: The studio's audio rule wants WAV for sound effects (OGG only for music); all 12 are
    OGG (72 KB in all). WAV adds about 255 KB to the app (+12 %); quality holds or improves.
    Options: 1) convert the kept sounds to WAV from their originals, fixing levels in the same pass
    2) keep OGG and ask the studio to allow OGG for sourced effects 3) remake the whole set in-house
    from recipes (exactly reproducible, but loses the sounds you auditioned; most effort)
    Recommendation: sound-designer — 1: small cost, meets the rule, keeps every sound you chose,
    and fixes the too-hot jump peak at the same time.
    Blocks: nothing
    Answer:

    ## Q-010 — Should the game go quiet when the phone is on Silent or Vibrate?   (open)
    Date: 2026-09-29    Asked by: sound-designer
    Context: Today game sounds follow media volume only, so Silent/Vibrate does not mute them; the
    studio's audio rule says silent mode must be honoured. A strict reading would also mute players
    wearing headphones on Vibrate.
    Options: 1) mute on Silent/Vibrate, always 2) mute on Silent/Vibrate only on the phone speaker;
    headphones keep sound 3) keep today's behaviour and record an exception to the rule
    Recommendation: sound-designer — 2: quiet in a meeting, never a surprise for a headphone player.
    The tech-lead confirms the cost before it is built.
    Blocks: nothing
    Answer:

## Open issues / risks
- **The phone-speaker numbers are a stand-in** (a 400 Hz fourth-order high-pass), not a device
  measurement. The qa-engineer should confirm on a real phone that the invalid tap is inaudible
  today, that the put-down is audible, and that the candidates play.
- **Inputs missing on develop:** `docs/gdd.md` does not exist. `docs/ux-spec.md` exists only on
  `feature/phase-15-ux-refresh` and `docs/tech-design.md` only on `feature/phase-14-release-docs`.
  I read both through `git show <branch>:<path>` from this worktree and touched no other checkout
  or worktree.
- **The tutorial jump has no haptic** (`JumpHaptic` is private to `GameScreen`), so a jump buzzes
  in the game and not in the tutorial. It is not a rules/audio.md violation, since the slide is the
  partner. The ux-designer should confirm the asymmetry is intended.
- **Adopting a candidate or converting to WAV needs a test change.** `AudioCreditsTest` scans only
  `*.ogg` and requires an https source per row. In-house assets need a recipe-path rule.
  (android-game-dev, with qa-engineer review)
- **No asset was changed.** `res/raw/` is untouched, and no candidate is committed. Every level
  change in P4 waits for the P5 answer, so the assets are re-normalised once.

## Handoff
Next: producer — queue Q-008, Q-009 and Q-010 (none blocks anything). Dispatch the
**research-analyst** for a per-file licence verdict and the Kenney archive pin (P6). Dispatch the
**tech-lead** for the silent-mode design (P3). Dispatch the **ux-designer** for the invalid-tap
visual partner (P1).
Next: android-game-dev — after the answers: the invalid-tap visual partner (P1), the ringer-mode
gate (P3), `VOLUME_JUMP` 0.9 → 1.0 alongside my −1.1 dBFS jump re-level (P4a), the
`AudioCreditsTest` update for WAV and recipes (P5), background silencing (P8), and the
`MAX_STREAMS` KDoc arithmetic (nit 12). I deliver the recipes and assets on a feature branch
first.
Next: qa-engineer — on a phone speaker at half media volume: confirm the invalid tap today, the
put-down's audibility, and the chosen candidates. After the phase-15 merge, confirm the removal
layer sounds with animations off (P9).
Next: sound-designer (re-dispatch) — once Q-008 and Q-009 are answered, commit the chosen
recipes under `audio/recipes/`, render and normalise the assets, update the spec's Assets table,
then review the developer's integration diff.
