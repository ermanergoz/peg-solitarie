---
phase: quick-260828-vrr
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/res/raw/sfx_select.ogg
  - app/src/main/res/raw/sfx_deselect.ogg
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
autonomous: false
requirements: [SOUND-02, D-01, D-02, D-03, D-05a, QUAL-03, T-07-01, T-07-15]

must_haves:
  truths:
    - "Picking a peg up and putting it back down play two audibly different cues"
    - "Putting a peg back down never fires the invalid-tap cue (D-05a's surviving half)"
    - "The put-down asset is provably the pick-up asset reversed and pitched down one whole tone"
    - "07-CONTEXT.md records that D-05a's 'reuses the selection sound / no extra asset' half was reversed"
    - "docs/audio/CREDITS.md contains no claim the tree contradicts"
    - "The new res/raw resource survives the release resource shrinker"
    - "AudioCreditsTest is proven to have re-run against the edited CREDITS.md, not served from cache"
    - "./gradlew :app:check --offline is green"
  artifacts:
    - path: "app/src/main/res/raw/sfx_select.ogg"
      provides: "The CC0 LilMati Pop 01 pick-up cue, mono 44.1 kHz Ogg Vorbis"
      contains: "vorbis, 1 channel, 44100 Hz, duration 0.067098 s, encoded peak -3.5..-3.0 dBFS"
    - path: "app/src/main/res/raw/sfx_deselect.ogg"
      provides: "The same pop time-reversed and pitched down a whole tone — the put-down cue"
      contains: "vorbis, 1 channel, 44100 Hz, duration 0.075329 s, encoded peak -3.5..-3.0 dBFS"
    - path: "app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt"
      provides: "A twelfth cue, Deselect, bound to R.raw.sfx_deselect as a compile-time constant"
      contains: "Deselect(R.raw.sfx_deselect"
    - path: "app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt"
      provides: "VOLUME_DESELECT derived as VOLUME_SELECT minus 10 dB"
      contains: "VOLUME_DESELECT"
    - path: "app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt"
      provides: "deselectionTick — the ordinal that separates put-down from pick-up"
      contains: "val deselectionTick: Int = 0,"
    - path: ".planning/phases/07-sound/07-CONTEXT.md"
      provides: "The dated D-05a amendment"
      contains: "D-05a amended"
  key_links:
    - from: "app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt"
      to: "GameUiState.deselectionTick"
      via: "the re-tap deselect branch of onCellTap"
      pattern: "deselectionTick = it\\.deselectionTick \\+ 1,"
    - from: "app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt"
      to: "SoundCue.Deselect"
      via: "DeselectionSound, a LaunchedEffect keyed on deselectionTick"
      pattern: "sound\\.play\\(SoundCue\\.Deselect\\)"
    - from: "app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt"
      to: "R.raw.sfx_deselect"
      via: "compile-time @RawRes constant, so the resource shrinker can trace it"
      pattern: "R\\.raw\\.sfx_deselect"
---

<objective>
Give picking a peg up and putting it back down two distinct sounds. The pick-up becomes the
CC0 freesound "Pop 01" by LilMati; the put-down becomes that same pop time-reversed and
pitched down one whole tone, mixed 10 dB under its forward twin.

Purpose: the user asked for the two halves of the selection gesture to be audibly different,
auditioned two rounds of rendered candidates, and approved this exact combination. This plan
ships it and repairs every in-repo claim it falsifies.

Output: a re-encoded `sfx_select.ogg`, a new `sfx_deselect.ogg`, a twelfth `SoundCue`, a
`deselectionTick` that separates the two events in `GameUiState`, a truthful `CREDITS.md`, a
dated amendment to D-05a, and a re-proven release-shrink retention check.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md

@docs/audio/CREDITS.md
@.planning/phases/07-sound/07-CONTEXT.md
@app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt
@app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt
@app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
@app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
@app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
@app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt
@app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt
@app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt
@app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt
@app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt

Branch: `git branch --show-current` reported `feature/phase-07-sound` at planning time. Stay
on that branch — this is continued phase 7 tuning. If the working tree is on `develop` or
`main`, cut `feature/sound-pickup-putdown-cues` off it first; Gitflow forbids direct commits
to either (CLAUDE.md).
</context>

<locked_decisions>
The user auditioned rendered pick-up/put-down pairs across two rounds. Do not re-litigate any
of these, and do not "simplify" any of them into a later pass.

- **D-VRR-01 — the pick-up sample.** freesound sound **506546**, "Pop 01" by **LilMati**,
  licence **CC0 1.0**. It replaces Kenney `select_002.ogg` as `sfx_select.ogg`.
- **D-VRR-02 — the put-down is a derived asset**, produced from the *same normalised
  intermediate* as the pick-up so the pair is provably the same audio: `areverse`, then
  `asetrate=44100*0.8909`, then `aresample=44100`. That is a time reversal plus a **whole
  tone (2 semitones) down**.
- **D-VRR-03 — the whole tone is the tuning, and its history is the reason.** Round 1 shipped
  a *plain* reversal at equal pitch and the user rejected it as indistinguishable from the
  pick-up ("too close to each other") — a 98 ms transient reversed does not read as reversed,
  so pitch is what carries the distinction. Round 2 tried a fifth down and the user judged it
  too far apart ("reduce the difference"). A whole tone is where it landed: clearly a sibling
  of the pick-up, clearly not the same event. **Record this history in CREDITS.md** — the
  rejected variants are why the parameter has the value it has.
- **D-VRR-04 — reverse and pitch are baked into the shipped asset; level is not.**
  `SoundCue` carries no rate field (playback rate is passed at `play()` only for the jump and
  star-pop ladders, via dedicated player methods), so baking the pitch keeps the put-down a
  plain `sound.play(SoundCue.Deselect)` call and leaves the audio engine untouched.
- **D-VRR-05 — the −10 dB is a `VOLUME_DESELECT` constant, never baked into the file.**
  Per-cue level is exactly what `SoundParams.VOLUME_*` is for (`VOLUME_INVALID = 0.3f` is the
  precedent). **Do not attenuate the asset and also drop the constant** — that applies −10 dB
  twice. Both shipped files stay peak-normalised to the same target.
- **D-05a is half-reversed, not abandoned.** The "reuses the selection sound — no extra
  asset" half is reversed. The other half **stands and must be preserved**: a deselect is a
  SUCCESSFUL action and must **never** fire the invalid-tap cue. `GameViewModel.onCellTap`
  carries a comment saying so at the deselect branch; keep its intent intact and keep the
  branch above the rejected-tap `else`.
</locked_decisions>

<measured_facts>
Every number below was measured in this repository with ffmpeg 8.0 during planning, against
the actual master and the actual committed assets. Use them as expected values. Do not
re-derive them by guesswork, and do not substitute round numbers for them.

**Master** (`/Users/ermanergoz/Downloads/506546__lilmati__pop-01.wav`):
`pcm_s16le` / 2 ch / 44100 Hz / **0.098526 s** / 17,706 B / peak **-0.004773 dBFS** /
md5 `2e5ddefd4d3741ad4221245950904ded`. Page title "Pop 01", uploader LilMati, licence CC0
1.0 linking `creativecommons.org/publicdomain/zero/1.0/` — confirmed by the user at audition
time and re-confirmed against the sound page during planning.

**The master is hot and has silence at both ends.** `silencedetect=n=-45dB:d=0.005` reports
silence `0 → 0.00605442`, audible content `0.00605442 → 0.0730839`, silence
`0.0730839 → 0.098526`. Both ends must be trimmed, and the trailing 25 ms is the important
one: leave it in and `areverse` turns it into **25 ms of latency on the put-down cue**, which
reads as a sloppy, late sound.

**No fade is permitted at the head.** The trimmed region's peak sample sits **1.41 ms** in
(sample 62 of 2,959). A 3 ms fade-in would halve the transient. The boundary samples after
trimming are -115 and -178 out of 32,768 — that is -49 dBFS and -45 dBFS, so the
discontinuity at each end is already inaudible and needs no fade. A symmetric fade would also
have been the only reversal-invariant option; it is not needed, so do not add one.

**Peak target: -3.3 dBFS encoded**, the same target and the same ±0.25 dB acceptance window
quick task 260828-vb3 used, measured with `astats` on the **encoded** Ogg. `loudnorm` remains
forbidden — these cues are ~70 ms against its ~3 s analysis window.

**Forward gain sweep** (gain applied to the PCM intermediate, peak measured on the encoded
`.ogg`; each value is deterministic across repeated runs):

| pre-encode gain | encoded peak | size |
|---|---|---|
| -3.60 dB | -3.394 dBFS | 4,603 B |
| **-3.50 dB** | **-3.307 dBFS** | **4,600 B** |
| -3.45 dB | -3.334 dBFS | 4,602 B |
| -3.40 dB | -3.157 dBFS | 4,607 B |
| -3.35 dB | -3.207 dBFS | 4,607 B |
| -3.30 dB | -3.184 dBFS | 4,604 B |
| -3.00 dB | -2.876 dBFS | 4,600 B |

**-3.50 dB is the intermediate's gain.** The sweep is visibly non-monotonic — libvorbis
jitters the peak by ~0.2 dB between neighbouring gains — which is exactly why the gain is
chosen by measuring the encoded result rather than by normalising the PCM and hoping.

**Put-down trim sweep** (applied *after* `areverse,asetrate,aresample`, on top of the same
intermediate, to bring the encoded peak back onto the same target):

| trim | encoded peak | size |
|---|---|---|
| +0.00 dB | -3.772 dBFS | 4,630 B |
| +0.20 dB | -3.383 dBFS | 4,639 B |
| **+0.30 dB** | **-3.280 dBFS** | **4,640 B** |
| +0.40 dB | -3.472 dBFS | 4,645 B |
| +0.50 dB | -3.491 dBFS | 4,650 B |
| +0.60 dB | -3.164 dBFS | 4,647 B |

**+0.30 dB is the put-down's trim.** The reversal-plus-resample loses ~0.47 dB of encoded
peak at equal PCM gain, so the trim is an *encoder-and-resampler compensation*, not a mix
decision — the mix decision is `VOLUME_DESELECT` and lives in code (D-VRR-05).

**The shipped pair:**

| file | codec | ch | rate | duration | size | peak | RMS |
|---|---|---|---|---|---|---|---|
| `sfx_select.ogg` | vorbis | 1 | 44100 | 0.067098 s | 4,600 B | -3.307156 dBFS | -24.268079 dB |
| `sfx_deselect.ogg` | vorbis | 1 | 44100 | 0.075329 s | 4,640 B | -3.279679 dBFS | -22.371446 dB |

**The duration ratio is the pitch proof.** `asetrate` couples pitch and time, so
0.075329 / 0.067098 = **1.122671**, against `2^(2/12)` = 1.122462 — a whole tone, to within
0.0002. A duration-ratio gate is therefore a real, cheap proof that the shift is present.

**The reversed cue is measurably more present at equal peak**, which is what D-VRR-05's
−10 dB is for: the put-down's RMS is **1.90 dB higher** than the pick-up's despite both files
sitting at the same peak, because a slow swell carries more energy relative to its peak than a
sharp attack does.

**`VOLUME_DESELECT` arithmetic.** `VOLUME_SELECT` is `0.6f`. Ten dB down is
`0.6 × 10^(-10/20)` = **0.189737**. Deriving it in code as `VOLUME_SELECT * 0.3162f` gives
**0.18972f**, which is **-10.0007 dB** below `VOLUME_SELECT` — closer to the intent than the
rounded literal `0.19f` (-9.988 dB), and it keeps the two constants from drifting apart, the
same reasoning `STAR_POP_BASE_RATE` already uses.

**Decoded PCM footprint** against `SoundPool`'s 1 MB per-sample limit: 5,918 B for the
pick-up, 6,644 B for the put-down. Both are rounding error; `MAX_STREAMS` needs no re-check
(neither cue is long enough to overlap itself, unlike the 1.65 s star pop).

**libvorbis is not byte-deterministic.** Three identical invocations produced identical
*audio* (same peak, same byte count) under three different md5s. **Do not write a hash gate
for either `.ogg`.** Verify by measured properties, exactly as CREDITS.md already instructs.

## Two facts that contradict the brief, and one pre-existing falsehood

State these in the summary; do not let them pass silently.

1. **The asset set is NOT normalised to -3.3 dBFS.** Measured across the eleven committed
   files on 2026-08-28: `sfx_jump` -0.018, `sfx_win` -1.582, `sfx_hint` -2.539,
   `sfx_star_pop` -3.338, `sfx_sound_enabled` -4.897, `sfx_undo` -4.971, `sfx_btn_forward`
   -5.231, `sfx_select` **-5.476**, `sfx_peg_remove` -5.959, `sfx_btn_dismiss` -7.961,
   `sfx_invalid` -14.008. -3.3 dBFS was the *star pop's* target in quick task 260828-vb3,
   inherited from the file that cue replaced. The set is a deliberate per-cue balance, not one
   level.
2. **Consequence, and it is user-visible.** The incumbent `sfx_select.ogg` peaks at -5.476
   dBFS; the replacement peaks at -3.307. With `VOLUME_SELECT` unchanged at `0.6f`, the
   pick-up cue becomes **2.17 dB louder in game** than the sound it replaces. Shipping at
   -3.3 dBFS is the instructed target and is what the user auditioned, so ship it — but the
   loudness change is a real consequence of the instruction and Task 3 step 4 puts it in front
   of the user rather than hiding it.
3. **CREDITS.md already contains a false claim, independent of this change.** Its Processing
   section says "the invalid tap is the quietest file in the set at −14 dBFS, the win stinger
   the loudest at −1.5 dBFS". `sfx_jump` peaks at **-0.018 dBFS** and is the loudest file in
   the set. That sentence is being rewritten by this plan anyway, so correct it in the same
   pass with the measured figures — do not extend a falsehood.
</measured_facts>

<pipeline_reference>
Run from the repository root.

```
MASTER=/Users/ermanergoz/Downloads/506546__lilmati__pop-01.wav
```

**Confirm the source before using it** (T-07-01 — these bytes came off the internet):

```
md5 -q "$MASTER"          # expect 2e5ddefd4d3741ad4221245950904ded
ffprobe -hide_banner -v error -show_entries format=duration,size \
  -show_entries stream=codec_name,channels,sample_rate -of default=nw=1 "$MASTER"
```

Expect `codec_name=pcm_s16le`, `channels=2`, `sample_rate=44100`, `duration=0.098526`,
`size=17706`. If any differ, stop — the file is not the audited master.

**Step 1 — the one shared normalised intermediate.** Write it outside the repository (it is a
build artefact, not a committed asset):

```
ffmpeg -y -i "$MASTER" \
  -af "atrim=start=0.006:end=0.0731,asetpts=PTS-STARTPTS,volume=-3.50dB" \
  -ac 1 -ar 44100 -c:a pcm_s24le /tmp/vrr_pop_norm.wav
```

`atrim` removes the master's leading 6 ms and trailing 25 ms of sub-−45 dB silence (the
trailing cut is what stops the reversal from arriving late); `-ac 1` folds to mono as every
other cue is; `volume=-3.50dB` is the `astats`-measured peak normalisation from
`<measured_facts>`; `pcm_s24le` keeps the intermediate above the 16-bit source so neither
encode carries an avoidable requantisation. Expect `duration=0.067098`, PCM peak -3.506 dBFS.

**Step 2 — the pick-up, straight off the intermediate:**

```
ffmpeg -y -i /tmp/vrr_pop_norm.wav -c:a libvorbis -q:a 4 \
  app/src/main/res/raw/sfx_select.ogg
```

**Step 3 — the put-down, off the same intermediate (D-VRR-02):**

```
ffmpeg -y -i /tmp/vrr_pop_norm.wav \
  -af "areverse,asetrate=44100*0.8909,aresample=44100,volume=0.30dB" \
  -c:a libvorbis -q:a 4 app/src/main/res/raw/sfx_deselect.ogg
```

`0.8909` is `2^(-2/12)` to four places — one whole tone down. `aresample=44100` returns the
stream to the set's sample rate after `asetrate` reinterprets it. `volume=0.30dB` is the
encoder/resampler compensation from `<measured_facts>`, not a mix decision.

**Property verification of both committed files:**

```
for f in app/src/main/res/raw/sfx_select.ogg app/src/main/res/raw/sfx_deselect.ogg; do
  ffprobe -hide_banner -v error -show_entries format=duration \
    -show_entries stream=codec_name,channels,sample_rate -of default=nw=1 "$f"
  ffmpeg -hide_banner -i "$f" -af astats -f null - 2>&1 | grep -m1 "Peak level dB"
done
```

Expect `codec_name=vorbis`, `channels=1`, `sample_rate=44100`; `duration=0.067098` and
`0.075329`; both peaks inside **-3.5 .. -3.0 dBFS**. If a peak falls outside, adjust that
file's `volume=` term and re-encode — but re-measure rather than extrapolating, because the
sweep is non-monotonic.

**The reversal proof.** Write this script once (not committed); it undoes the reversal and the
pitch shift on the shipped put-down and correlates the result against the shipped pick-up,
searching lags to absorb the two files' differing Vorbis encoder padding:

```
cat > /tmp/vrr_reverse_check.py <<'PY'
import array, sys
def rd(p):
    a = array.array('h'); a.frombytes(open(p, 'rb').read()); return a.tolist()
x, y = rd('/tmp/vrr_fwd.raw'), rd('/tmp/vrr_rev.raw')
def corr(lag):
    a = x[max(0, lag):]; b = y[max(0, -lag):]; n = min(len(a), len(b))
    if n < 1000: return -2.0
    a = a[:n]; b = b[:n]
    d = (sum(p * p for p in a) ** .5) * (sum(q * q for q in b) ** .5)
    return sum(p * q for p, q in zip(a, b)) / d if d else -2.0
m = max(corr(l) for l in range(-900, 901))
print('corr=%.4f' % m)
sys.exit(0 if m > 0.90 else 1)
PY
```

Driven by:

```
ffmpeg -v error -y -i app/src/main/res/raw/sfx_select.ogg -f s16le /tmp/vrr_fwd.raw
ffmpeg -v error -y -i app/src/main/res/raw/sfx_deselect.ogg \
  -af "areverse,asetrate=44100*1.122462,aresample=44100" -f s16le /tmp/vrr_rev.raw
python3 /tmp/vrr_reverse_check.py
```

Measured during planning: the real pair scores **0.9871**. The gate is not trivially
satisfiable — a plain reversal with no pitch shift (the variant the user rejected in round 1)
scores 0.5249, a copy of the pick-up scores 0.0106, and an unrelated cue scores 0.1185. All
three fail at the 0.90 threshold.

**The release-shrink re-run** (Task 2). Requires the four untracked IDs; supplying
deliberately non-real values for one invocation is the documented CI path and nothing is
written to disk:

```
ADMOB_APP_ID=not-a-real-id ADMOB_REWARDED_AD_UNIT_ID=not-a-real-id \
ADMOB_INTERSTITIAL_AD_UNIT_ID=not-a-real-id PGS_PROJECT_ID=000000000000 \
  ./gradlew :app:assembleRelease --offline
AAPT2=$(find ~/.gradle/caches -maxdepth 6 -type d -name 'aapt2-9.1.0*' | head -1)/aapt2
$AAPT2 dump resources app/build/outputs/apk/release/app-release-unsigned.apk | grep -c 'raw/sfx_'
```

Expect **12**.
</pipeline_reference>

<gate_notes>
Read before writing or "fixing" any grep gate. Every gate below was dry-run against the real
tree during planning; each change-gate was confirmed to **fail now**, and each invariant-gate
to **pass now**.

- **`selectionTick` is a substring of `deselectionTick`.** An unanchored
  `grep -c 'selectionTick = 0,'` counts **4** lines after this change, not 2 — it is
  self-invalidating. Gates on that pattern carry a **leading space** for exactly this reason.
  Do not remove it. `selectionTick = it.selectionTick,` and its `+ 1` form are safe unanchored
  (the deselect line differs at `it.d` vs `it.s`), and were verified as such.
- **`AudioCreditsTest.tableRowFor` takes the FIRST table line containing the filename.**
  `sfx_select.ogg` is not a substring of `sfx_deselect.ogg` and vice versa, so row order does
  not matter — but exactly one row per filename must exist, and the gates enforce that.
- **`AudioCreditsTest` splits rows on `|`, drops empty cells, then requires ≥4 cells with
  `https://` in index 2 and a non-empty index 3.** Every cell of both new rows must be
  non-empty and no cell text may contain a literal `|`.
- **`AudioCreditsTest` reads `CREDITS.md` from the working tree at runtime and that file is
  not a declared Gradle input.** A docs-only edit can leave `:app:testDebugUnitTest`
  UP-TO-DATE, so the gate never runs and a cached PASS means nothing. Force it with `--rerun`
  (works on this project's Gradle 9.5.1 wrapper); escalate to `--rerun-tasks` if Gradle still
  reports UP-TO-DATE.
</gate_notes>

<tasks>

<task type="auto">
  <name>Task 1: Encode the pick-up/put-down pair and make the provenance record true</name>
  <files>app/src/main/res/raw/sfx_select.ogg, app/src/main/res/raw/sfx_deselect.ogg, docs/audio/CREDITS.md, .planning/phases/07-sound/07-CONTEXT.md</files>
  <action>
Verify the master against the md5 and five properties in `<pipeline_reference>`, then run its
three encode steps: one shared normalised intermediate, the pick-up straight off it, the
put-down off it through `areverse,asetrate,aresample` plus the measured compensation trim.
Confirm both files against the property-verification block and the reversal proof before
touching any document. Per D-VRR-01, D-VRR-02 and D-VRR-04.

Keep the resource filename `sfx_select.ogg` exactly as it is — `SoundCue` resolves it as a
compile-time `R.raw.sfx_select` constant and renaming it would break both that reference and
the existing release-shrink argument. The new file is `sfx_deselect.ogg`, chosen because
Android resource names must be `[a-z0-9_]` and because neither name is a substring of the
other, which keeps `AudioCreditsTest.tableRowFor` unambiguous.

**Then repair `docs/audio/CREDITS.md`.** Re-read it as it stands now — quick task 260828-vb3
rewrote large parts of it hours ago, so do not work from memory of an earlier revision. Nine
claims become false with this change and one was already false; each must be corrected in
place, not annotated around.

1. **Header (lines 3–7).** It currently reads "**Ten** of the eleven cues are drawn from that
   single pack. The eleventh, `sfx_star_pop.ogg`, is a single third-party borrow from
   freesound". After this change the set is **twelve** files: **nine** from the Kenney anchor
   pack and **three** freesound borrows, drawn from **two** freesound sounds — 531512 by
   CogFireStudios (the star pop) and 506546 by LilMati (the pick-up and the put-down derived
   from it). Rewrite with those counts.

2. **The D-03 paragraph (lines 9–12).** It says the star-pop swap reverses D-03's single-pack
   cohesion guarantee and that cohesion became an editorial judgement. That remains true and
   should stay, but it now understates the position: the borrow is no longer one celebration
   cue at the edge of the set, it now includes the **most frequently heard gameplay cue** and
   its twin. Say so plainly. Keep the existing statement that the user made the trade
   knowingly after auditioning rendered candidates — it is true of this change too.

3. **Attribution paragraph (lines 14–21).** "Every file in `app/src/main/res/raw/` is CC0 1.0"
   stays true and must be kept — sound 506546 is CC0 as well. Update the "ten Kenney cues and
   the one freesound borrow" arithmetic to nine and three.

4. **Table rows.** Rewrite the `sfx_select.ogg` row and insert a `sfx_deselect.ogg` row
   directly after it. Four columns, leading `|`, every cell non-empty, no literal `|` inside
   any cell (see `<gate_notes>`), and exactly one row per filename — edit in place, never add
   a second row for a filename. The select row's original-filename cell names
   `506546__lilmati__pop-01.wav` and notes it is the original WAV download rather than a
   preview. The deselect row's original-filename cell must make the derivation legible in the
   table itself: same source WAV, time-reversed and pitched down a whole tone via
   `ffmpeg -af areverse` plus `asetrate` from the same normalised intermediate as
   `sfx_select.ogg`. Both rows carry source
   `https://freesound.org/people/LilMati/sounds/506546/` and licence `CC0 1.0`. The put-down
   is a **derived work with no page of its own**, and naming its parent's page as its source is
   the honest reading — CC0 imposes no share-alike or notice condition, so a derivative
   inherits the dedication; say this explicitly in the new section below rather than leaving a
   reader to infer why a synthesised file cites someone else's URL.

5. **Processing, first paragraph (lines 89–94).** "Each of the **ten** Kenney clips…" becomes
   nine. The parenthetical "(`sfx_select.ogg` is 43 ms)" is now wrong — that file is 67 ms;
   pick a still-true example of a cue far shorter than `loudnorm`'s window (`sfx_select.ogg` at
   67 ms still is, so restate the figure rather than removing the example). And correct the
   final sentence: it claims "the invalid tap is the quietest file in the set at −14 dBFS, the
   win stinger the loudest at −1.5 dBFS". The second half **was already false before this
   change** — `sfx_jump` peaks at −0.02 dBFS and is the loudest file in the set. Replace the
   sentence with the measured extremes from `<measured_facts>`: quietest `sfx_invalid` at
   −14.0 dBFS, loudest `sfx_jump` at −0.02 dBFS, and note that the win stinger at −1.6 dBFS is
   the loudest *celebration* cue rather than the loudest file. Flag in the summary that this
   was a pre-existing error found by measurement, not one this change introduced.

6. **Processing, second paragraph (lines 96–99).** "**Ten** files are under 0.7 s" becomes
   **eleven** of twelve, with `sfx_star_pop.ogg` still the sole exception at 1.65 s. The 1 MB
   decoded-PCM arithmetic for the star pop is unchanged and stays. Add the two new files'
   decoded footprints (5,918 B and 6,644 B) so the new assets are covered by the same argument.

7. **A new section, heading exactly `### Peg pick-up / put-down pair (2026-08-28)`.** Place it
   after the existing `### Star pop replacement (2026-08-28)` section. It carries what the
   table cannot:
   - What changed and why: the user asked for the two halves of the selection gesture to sound
     different; `sfx_select.ogg` is now freesound 506546 "Pop 01" by LilMati (CC0 1.0) and
     `sfx_deselect.ogg` is that same pop reversed and pitched down a whole tone.
   - **The full reproduction chain**, verbatim from `<pipeline_reference>`: the shared
     `atrim` + mono + `volume=-3.50dB` intermediate, the plain encode for the pick-up, and the
     `areverse,asetrate=44100*0.8909,aresample=44100,volume=0.30dB` chain for the put-down.
     Explain each term: why both ends are trimmed (the trailing 25 ms would otherwise become
     25 ms of latency on the reversed cue), why there is **no fade** (the transient peaks
     1.41 ms in, so a fade-in would halve it, and the trimmed boundaries already sit at −45 to
     −49 dBFS), why `0.8909` is `2^(-2/12)`, and that `volume=0.30dB` is encoder/resampler
     compensation rather than a mix decision.
   - **The tuning history (D-VRR-03).** A plain reversal at equal pitch was rejected as
     indistinguishable — a 98 ms transient reversed does not read as reversed, so the pitch
     shift is what carries the distinction. A fifth down was then judged too far apart. A whole
     tone is the approved middle. Write this as the reason the parameter has its value; a
     future reader who deletes the pitch shift as redundant must be able to find out why it is
     not.
   - **Where the −10 dB lives and why it is not in the file** (D-VRR-05): both assets are
     peak-normalised to the same −3.3 dBFS target, and the put-down's level sits in
     `SoundParams.VOLUME_DESELECT`, so the file on disk is not a quiet outlier and the
     attenuation is not applied twice. Note the measurement that motivates it — the reversed
     cue's RMS is 1.90 dB higher than the forward's at the same peak, because a swell carries
     more energy relative to its peak than an attack does.
   - **The peak-target caveat.** −3.3 dBFS is the star pop's target from 260828-vb3, not a
     set-wide level; the set spans −0.02 to −14.0 dBFS as a deliberate per-cue balance.
     Recording the pick-up at −3.3 rather than at the −5.48 of the file it replaces makes it
     2.17 dB louder in game at the unchanged `VOLUME_SELECT`. That was the instructed target
     and what the user auditioned; state it as a known, accepted consequence.
   - That the derivative inherits CC0 (point 4 above).

8. **The star-pop section's forward-looking claim (lines 53–56).** It says "Nothing else in
   the set moved, and no further borrows are planned." Both halves are now false. Correct
   them — date-scope that section to the star-pop swap it describes and point forward to the
   new section rather than deleting the sentence, so the record shows the expectation and what
   overtook it.

9. **`## Verification` bullets (lines 132–166).**
   - The codec-confirmation bullet says `ffprobe` reports `vorbis` and `channels=1` "for all
     **eleven** committed files" — re-run it over all twelve and restate.
   - The duration bullet says "0.13–0.54 s for the **ten** Kenney cues and 1.650000 s for
     `sfx_star_pop.ogg`". Two problems: the count is now nine, and the range was **already
     wrong** because it included `sfx_select.ogg` at 0.043 s. Measured, the nine remaining
     Kenney cues span **0.097–0.539 s**; add the two freesound pop files at 0.067098 s and
     0.075329 s alongside the 1.650000 s star pop. Give real figures, not a hand-wave.
   - The freesound-fetch bullet currently pins one master. Extend it to both: keep the 531512
     pin as it stands and add 506546 pinned the same way — `pcm_s16le` / 2 ch / 44100 Hz /
     0.098526 s / 17,706 B, md5 `2e5ddefd4d3741ad4221245950904ded`, obtained over HTTPS as the
     original WAV download from the sound page, with no archive to `unzip -l` because it is a
     single file.
   - The licence-provenance bullet performs the per-file CC0 confirmation for 531512. Do the
     same for **506546**: uploader LilMati, title "Pop 01", the sound page states Creative
     Commons 0 and links `https://creativecommons.org/publicdomain/zero/1.0/`, confirmed by the
     user at audition time (2026-08-28). Note that `sfx_deselect.ogg` needs no separate
     confirmation because it is a derivative of that same CC0 source.

**Leave `## Licence text as shipped by the source` untouched** — the verbatim Kenney block is
still accurate about the pack it quotes. **Leave `## Release shrink verification` untouched in
this task** — Task 2 re-runs it once `SoundCue` actually references the new resource, and
editing it before the run would mean writing a result that does not exist yet.

Write the document so a reader who has never seen this plan cannot come away with a false
belief.

**Finally, amend D-05a in `.planning/phases/07-sound/07-CONTEXT.md`.** Insert a dated bullet
directly after the existing D-05a line (line 30) in the same style as the D-07a entry — for
example `**D-05a amended (2026-08-28, quick task 260828-vrr):**` — recording that the "reuses
the selection sound — symmetric feedback, no extra asset" half is **reversed**: pick-up and
put-down now have distinct cues (`SoundCue.Select` and `SoundCue.Deselect`, the latter the
same pop reversed and a whole tone down), and the asset set grew by one. State explicitly that
the other half **stands**: a deselect is a successful action and must never fire the
invalid-tap cue. Do not edit or delete the original D-05a text — amend beside it, so the
decision's history stays readable.
  </action>
  <verify>
    <automated>ffprobe -hide_banner -v error -show_entries format=duration -of default=nw=1 app/src/main/res/raw/sfx_select.ogg | grep -qx 'duration=0.067098'</automated>
    <automated>ffprobe -hide_banner -v error -show_entries format=duration -of default=nw=1 app/src/main/res/raw/sfx_deselect.ogg | grep -qx 'duration=0.075329'</automated>
    <automated>ffprobe -hide_banner -v error -show_entries stream=codec_name,channels,sample_rate -of default=nw=1 app/src/main/res/raw/sfx_select.ogg | tr '\n' ' ' | grep -q 'codec_name=vorbis sample_rate=44100 channels=1'</automated>
    <automated>ffprobe -hide_banner -v error -show_entries stream=codec_name,channels,sample_rate -of default=nw=1 app/src/main/res/raw/sfx_deselect.ogg | tr '\n' ' ' | grep -q 'codec_name=vorbis sample_rate=44100 channels=1'</automated>
    <automated>ffmpeg -hide_banner -i app/src/main/res/raw/sfx_select.ogg -af astats -f null - 2>&amp;1 | grep -m1 'Peak level dB' | awk '{ if ($NF &gt; -3.5 &amp;&amp; $NF &lt; -3.0) exit 0; else exit 1 }'</automated>
    <automated>ffmpeg -hide_banner -i app/src/main/res/raw/sfx_deselect.ogg -af astats -f null - 2>&amp;1 | grep -m1 'Peak level dB' | awk '{ if ($NF &gt; -3.5 &amp;&amp; $NF &lt; -3.0) exit 0; else exit 1 }'</automated>
    <!-- The whole-tone shift, proven by the asetrate duration coupling: 0.075329/0.067098 = 1.12267 vs 2^(2/12) = 1.12246 -->
    <automated>python3 -c "import subprocess as s; g=lambda f: float(s.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0','app/src/main/res/raw/'+f])); r=g('sfx_deselect.ogg')/g('sfx_select.ogg'); print('ratio=%.6f'%r); raise SystemExit(0 if abs(r-2**(2/12))&lt;0.005 else 1)"</automated>
    <!-- The reversal proof: undo areverse + the pitch shift, then correlate. Real pair 0.9871; plain reversal 0.5249; copy 0.0106; unrelated cue 0.1185. -->
    <automated>ffmpeg -v error -y -i app/src/main/res/raw/sfx_select.ogg -f s16le /tmp/vrr_fwd.raw &amp;&amp; ffmpeg -v error -y -i app/src/main/res/raw/sfx_deselect.ogg -af "areverse,asetrate=44100*1.122462,aresample=44100" -f s16le /tmp/vrr_rev.raw &amp;&amp; python3 /tmp/vrr_reverse_check.py</automated>
    <automated>test $(grep -c '^| .sfx_select\.ogg' docs/audio/CREDITS.md) -eq 1</automated>
    <automated>test $(grep -c '^| .sfx_deselect\.ogg' docs/audio/CREDITS.md) -eq 1</automated>
    <automated>test $(grep -c '^| .sfx_select\.ogg.*https://freesound\.org/people/LilMati/sounds/506546/' docs/audio/CREDITS.md) -eq 1</automated>
    <automated>test $(grep -c '^| .sfx_deselect\.ogg.*https://freesound\.org/people/LilMati/sounds/506546/' docs/audio/CREDITS.md) -eq 1</automated>
    <automated>test $(grep -c '^| .sfx_select\.ogg.*kenney\.nl' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc '**Ten** of the' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'eleven cues are drawn from that single pack' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'no further borrows' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'is 43 ms' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'the win stinger the loudest at' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'Each of the ten Kenney clips' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'Ten files are under 0.7 s' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'for the ten Kenney cues' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc 'for all eleven committed files' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc '### Peg pick-up / put-down pair (2026-08-28)' docs/audio/CREDITS.md) -eq 1</automated>
    <automated>test $(grep -Fc '2e5ddefd4d3741ad4221245950904ded' docs/audio/CREDITS.md) -ge 1</automated>
    <automated>test $(grep -Fc 'LilMati' docs/audio/CREDITS.md) -ge 2</automated>
    <automated>test $(grep -Fc 'areverse' docs/audio/CREDITS.md) -ge 1</automated>
    <automated>test $(grep -Fc 'asetrate' docs/audio/CREDITS.md) -ge 1</automated>
    <automated>test $(grep -Fc 'twelve' docs/audio/CREDITS.md) -ge 1</automated>
    <automated>test $(grep -Fc 'D-05a amended' .planning/phases/07-sound/07-CONTEXT.md) -eq 1</automated>
    <automated>test $(grep -Fc 'never fire the invalid-tap cue' .planning/phases/07-sound/07-CONTEXT.md) -ge 1</automated>
    <automated>./gradlew :app:testDebugUnitTest --tests '*AudioCreditsTest*' --rerun --offline</automated>
  </verify>
  <done>
`sfx_select.ogg` is the LilMati pop (mono 44.1 kHz Vorbis, 0.067098 s, encoded peak in
−3.5..−3.0 dBFS) and `sfx_deselect.ogg` is provably that same audio reversed and pitched down
one whole tone (duration ratio 1.1227, lag-aligned correlation above 0.90 after undoing both
transforms), at the same peak target. `CREDITS.md` credits freesound 506546 / LilMati / CC0 1.0
for both files in the table, records the reproduction chain, the two-round tuning history, why
the −10 dB is not in the file, and the corrected counts, durations and peak extremes — with no
sentence the tree contradicts, including the pre-existing "win stinger is loudest" error.
07-CONTEXT.md carries a dated D-05a amendment recording the reversal and preserving the
surviving half. `AudioCreditsTest` passed in a run that was **forced**, not reported
UP-TO-DATE.
  </done>
</task>

<task type="auto">
  <name>Task 2: Split the tick, ship the twelfth cue end to end, and re-prove release retention</name>
  <files>app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt, app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt, app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt, app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt, app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt, app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt, app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt, app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt, docs/audio/CREDITS.md</files>
  <action>
**`SoundParams` — the level decision (D-VRR-05, QUAL-03).** Directly after `VOLUME_SELECT`,
add a derived pair rather than a bare float, following the `STAR_POP_BASE_RATE` precedent so
the two cannot drift: an attenuation constant equal to `10^(-10/20)` (`0.3162f`) and
`const val VOLUME_DESELECT = VOLUME_SELECT * DESELECT_ATTENUATION`, which evaluates to
`0.18972f` — 10.0007 dB below the pick-up. Both are compile-time constant expressions, so
`const val` is valid for each. KDoc must record *why* the put-down sits below its forward twin
rather than level with it: a reversed transient's slow swell reads as more present than a
sharp attack at equal peak, and it is measurably so — the shipped put-down's RMS is 1.90 dB
above the pick-up's at the same encoded peak. Note that the two assets are peak-matched on
disk on purpose and that the whole level difference lives here, so nobody later "fixes" it by
also attenuating the file.

**`SoundParams.VOLUME_INVALID`'s KDoc becomes false and must be corrected.** It reads "the
quietest cue in the set". At `0.18972f` the put-down is now the quietest cue by mix. Restate
it as the quietest *rejection* cue and keep the SOUND-02 "quiet, non-punishing" intent, which
is about not punishing the player relative to the jump (0.3 against 0.9) and is untouched.

**`SoundCue` — the twelfth entry.** Add `Deselect(R.raw.sfx_deselect, SoundParams.VOLUME_DESELECT,
SoundParams.PRIORITY_DEFAULT)` immediately after `Select`, with KDoc naming it the put-down —
the `Select` pop reversed and a whole tone down (D-05a as amended). The class KDoc says "The
**eleven** sound effects the game can play" — make it twelve. The `InvalidTap` entry's KDoc
says "deliberately the quietest cue"; correct it the same way as the `SoundParams` one. The
`@RawRes` compile-time-constant argument in the class KDoc applies to the new entry unchanged
and needs no restatement.

**`GameUiState` — separate the two events.** `selectionTick` currently means two things: its
own KDoc says it is "incremented on every peg selection AND on every re-tap deselection". Add
`val deselectionTick: Int = 0` immediately after `selectionTick`, following the existing
ordinal idiom exactly. Rewrite `selectionTick`'s KDoc so it describes selection only, and give
`deselectionTick` its own KDoc in the same register as `undoTick` and `rejectedTapTick`:
incremented once per re-tap deselect; it exists because a deselect returns `selected` to
`null`, which is where it started, so it leaves no distinguishable state trace for a one-shot
consumer; guard on `> 0` at the consumer.

**The `= 0` default matches its three siblings and is kept for consistency — but nothing may
rely on it.** Enumerate `deselectionTick` explicitly in **both** factories (`initial()` and
`loading()`) and carry it forward at **all four** state-rebuild sites in `GameViewModel`
(`loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored`, `resetToInitial`).
Plan 07-03 found that a missed carry-forward is a latent phantom-cue bug — a fresh `0` is a
*changed* key for a `LaunchedEffect` sitting at a higher value — and the default is exactly
what would hide a missed site. The gates below count the carry-forward sites individually for
that reason.

**`GameViewModel.onCellTap` — the branch split.** The deselect branch currently does
`selectionTick = it.selectionTick + 1`; change it to `deselectionTick = it.deselectionTick + 1`
and leave `selectionTick` untouched in that branch. `select()` keeps its own
`selectionTick + 1` — after this change that expression appears **exactly once** in the file.
**Do not move the deselect branch or change the `when` ordering**: it must stay above the
rejected-tap `else`, because D-05a's surviving half is that a deselect is a successful action
and must never be counted as a rejection. Update the branch's comment to say the ordinal it now
advances is the deselection one and that the cue is now distinct, while keeping its existing
statement that this is a successful action and never a rejected tap. Also update
`resetToInitial`'s comment, which says "The **three** cue ordinals ride along" and names the
four rebuild sites — it is four ordinals now; keep the sites list and the Pitfall-2 reasoning
intact. Do not write the identifier `deselectionTick` inside any comment in this file; the
gates count non-comment occurrences by exact expression and a prose mention would be counted.

**`GameScreen` — the cue.** Add `DeselectionSound(deselectionTick = state.deselectionTick)`
immediately after the `SelectionSound(...)` call, and a `DeselectionSound` composable directly
after `SelectionSound` that is a clone of its idiom: `LaunchedEffect(deselectionTick)` firing
`sound.play(SoundCue.Deselect)` behind the same `> 0` guard, with KDoc explaining the guard by
reference to `SelectionSound` as `UndoSound` already does. Rewrite `SelectionSound`'s KDoc: it
currently states D-05a's "advances on BOTH … reuses this cue … no extra asset", which is the
half being reversed. `InvalidTapSound`'s KDoc says "A deselect is not one of those (D-05a) — it
takes [SelectionSound]"; retarget it to `[DeselectionSound]` and keep the point it is making,
which is unchanged. Two counts in this file also go stale: the file-header comment on line 2
("the **seven** one-shot feedback effects (one haptic + **six** sound cues)") and the
`JumpSound` KDoc's "**The six sound cues** take no enable/disable gate parameter". Both count
the effect composables; they become eight and seven respectively. The gate-asymmetry argument
that KDoc makes applies to the new cue verbatim and needs no rewording beyond the count.

**Update the tests to the new contract — tighten, never delete.** Plan 07-03 hit the same
situation and handled it by strengthening assertions; follow that precedent.

- `SoundParamsTest`: `EXPECTED_CUE_COUNT` 11 → 12 and its "The eleven cues" KDoc → twelve. Add
  a test pinning the mix decision in decibels rather than in floats — assert
  `20 * log10(Deselect.volume / Select.volume)` is within a small tolerance of `-10.0`, with
  both the target and the tolerance as named constants — so a future edit to either volume
  that breaks the approved relationship fails here. Add a second test recording that the
  put-down is now the quietest cue in the set and is below `InvalidTap`, which is the durable
  record that the old "invalid tap is the quietest" claim was knowingly superseded rather than
  forgotten. `theWinStingerIsTheLoudestCue` and `theRejectedTapIsQuieterThanTheJump` still hold
  and need no change.
- `GameViewModelSoundTest`: retarget `reTappingTheSelectedPegIsASelectionNotARejection` to the
  new contract and rename it accordingly — after the re-tap, `selectionTick` must **stay at 1**,
  `deselectionTick` must be `1`, `rejectedTapTick` must stay `0` and `selected` must be null.
  That is strictly stronger than the assertion it replaces. Add `deselectionTick == 0`
  assertions to `tappingAnEmptyCellWithNothingSelectedIsARejectedTap`,
  `theSelectionOrdinalDoesNotAdvanceOnAJump` and `aFreshGameStartsEveryCueOrdinalAtZero`. Extend
  `aRestartCarriesTheCueOrdinalsForwardInsteadOfResettingThem` to perform a deselect in its
  setup and to assert the fourth ordinal is carried — that test is the only automated proof the
  `resetToInitial` carry-forward exists. Update the class KDoc, which says "The **three** cue
  ordinals tick exactly once per real event — including D-05a, where a re-tap deselect …
  advances the selection ordinal": four ordinals, and the amended D-05a. The Challenge-load and
  resume-restore carry-forward tests can only reach the rejected-tap ordinal (every tap on a
  loading board is a rejection), so leave their assertions as they are — the gates cover those
  two carry-forward sites structurally.
- `GameScreenSoundTest`: retarget
  `selectingAPegThenReTappingItPlaysTheSelectionCueTwiceAndNeverTheInvalidOne` — rename it and
  assert the cue list is exactly `[Select, Deselect]` in order, still with the
  `doesNotContain(InvalidTap)` assertion, which is D-05a's surviving half and must stay. Add
  `doesNotContain(SoundCue.Deselect)` to
  `tappingAnEmptyCellWithNothingSelectedPlaysOnlyTheInvalidTapCue`. `FakeSoundPlayer` needs no
  change — it records any cue through `play`. `SoundPoolPlayerTest` needs no change either; it
  iterates `SoundCue.entries` and hardcodes no count.

**Then re-prove release retention (T-07-15) and close the shrink section.** This is the first
change since that section was written that adds a genuinely **new** `res/raw` resource id, so
260828-vb3's reason for skipping the re-run ("only the payload bytes behind an unchanged id
differ") does not apply here — a new id that the shrinker cannot trace is precisely the
"silent in release only" failure the section exists to catch. Run the `assembleRelease` +
`aapt2 dump resources` sequence from `<pipeline_reference>` and expect **12** `raw/sfx_`
entries. Then rewrite `## Release shrink verification` in `docs/audio/CREDITS.md`: replace the
vb3-era scope note and the "Result: 11 of 11" line with this run's date, the new APK size, the
12-of-12 result naming `sfx_deselect` among the entries, and an updated packaged-size range.
Keep the section's existing explanation of why paths are shortened and why no `noCompress`
config is needed. If the release build cannot be produced, **stop and report** rather than
writing an unverified claim — do not paper over it with "expected to be retained".
  </action>
  <verify>
    <!-- The tick split. Pattern B counts 2 today (select + deselect branches) and must count 1. -->
    <automated>test $(grep -c 'selectionTick = it.selectionTick + 1,' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 1</automated>
    <automated>test $(grep -c 'deselectionTick = it.deselectionTick + 1,' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 1</automated>
    <!-- All four rebuild sites carry the new ordinal: three use `it.`, resetToInitial uses `_uiState.value.` -->
    <automated>test $(grep -c 'deselectionTick = it.deselectionTick,' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 3</automated>
    <automated>test $(grep -c 'deselectionTick = _uiState.value.deselectionTick,' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 1</automated>
    <!-- Invariants: the three existing ordinals keep every carry-forward site they had. -->
    <automated>test $(grep -c 'selectionTick = it.selectionTick,' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 3</automated>
    <automated>test $(grep -c 'selectionTick = _uiState.value.selectionTick,' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 1</automated>
    <automated>test $(grep -c 'rejectedTapTick = it.rejectedTapTick + 1' app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt) -eq 1</automated>
    <!-- LEADING SPACE IS LOAD-BEARING: unanchored, this counts 4 after the change. See gate_notes. -->
    <automated>test $(grep -c ' selectionTick = 0,' app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt) -eq 2</automated>
    <automated>test $(grep -c ' deselectionTick = 0,' app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt) -eq 2</automated>
    <automated>test $(grep -c 'val deselectionTick: Int = 0,' app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt) -eq 1</automated>
    <!-- The screen fires the new cue, and still fires the old one. -->
    <automated>test $(grep -c 'DeselectionSound(deselectionTick = state.deselectionTick)' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt) -eq 1</automated>
    <automated>test $(grep -c 'if (deselectionTick > 0) sound.play(SoundCue.Deselect)' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt) -eq 1</automated>
    <automated>test $(grep -c 'if (selectionTick > 0) sound.play(SoundCue.Select)' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt) -eq 1</automated>
    <automated>test $(grep -Fc 'six sound cues' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt) -eq 0</automated>
    <automated>test $(grep -Fc 'seven one-shot feedback effects' app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt) -eq 0</automated>
    <!-- The cue and its level. -->
    <automated>test $(grep -c 'Deselect(R.raw.sfx_deselect, SoundParams.VOLUME_DESELECT, SoundParams.PRIORITY_DEFAULT),' app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt) -eq 1</automated>
    <automated>test $(grep -Fc 'The eleven sound effects' app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt) -eq 0</automated>
    <automated>test $(grep -Fc 'deliberately the quietest cue' app/src/main/java/com/app/pegsolitaire/audio/SoundCue.kt) -eq 0</automated>
    <automated>test $(grep -Fc 'the quietest cue in the set' app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt) -eq 0</automated>
    <automated>test $(grep -c 'VOLUME_DESELECT = VOLUME_SELECT \*' app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt) -eq 1</automated>
    <automated>test $(grep -Fc 'const val VOLUME_SELECT = 0.6f' app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt) -eq 1</automated>
    <automated>test $(grep -Fc 'const val VOLUME_INVALID = 0.3f' app/src/main/java/com/app/pegsolitaire/audio/SoundParams.kt) -eq 1</automated>
    <automated>test $(grep -Fc 'EXPECTED_CUE_COUNT = 12' app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt) -eq 1</automated>
    <!-- The tests carry the new contract rather than having been deleted. -->
    <automated>test $(grep -Fc 'SoundCue.Deselect' app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt) -ge 2</automated>
    <automated>test $(grep -Fc 'SoundCue.Deselect' app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt) -ge 2</automated>
    <automated>test $(grep -Fc 'SoundCue.InvalidTap' app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt) -ge 2</automated>
    <automated>test $(grep -Fc 'deselectionTick' app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt) -ge 5</automated>
    <!-- Release retention for the NEW resource id, and the record of it. -->
    <automated>test $(grep -Fc 'Result: 11 of 11' docs/audio/CREDITS.md) -eq 0</automated>
    <automated>test $(grep -Fc '12 of 12' docs/audio/CREDITS.md) -ge 1</automated>
    <automated>test $(grep -Fc 'sfx_deselect' docs/audio/CREDITS.md) -ge 2</automated>
    <automated>./gradlew :app:testDebugUnitTest --tests '*AudioCreditsTest*' --rerun --offline</automated>
    <automated>./gradlew :app:check --offline</automated>
  </verify>
  <done>
`SoundCue` has twelve entries with `Deselect` bound to `R.raw.sfx_deselect` as a compile-time
constant; `VOLUME_DESELECT` is derived as `VOLUME_SELECT` minus 10 dB with KDoc explaining why
a reversed transient sits below its forward twin. `GameUiState` carries `deselectionTick`,
enumerated in both factories and carried at all four `GameViewModel` rebuild sites, and
`selectionTick` no longer advances on a deselect. The screen fires `SoundCue.Deselect` on the
put-down and still fires `SoundCue.InvalidTap` only on a genuine rejection. No KDoc or comment
in `SoundCue.kt`, `SoundParams.kt`, `GameUiState.kt`, `GameViewModel.kt` or `GameScreen.kt`
asserts a relationship the code no longer has — including the cue counts and the two
"quietest cue" claims. Every previously-passing test still exists and asserts more than it did.
The release APK provably contains **12 of 12** `raw/sfx_` entries including `sfx_deselect`, and
`CREDITS.md` records that run rather than the superseded eleven-entry one.
`./gradlew :app:check --offline` is green.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Listen to the pick-up/put-down pair on a device</name>
  <action>
Stop and hand off. Present the <what-built> and <how-to-verify> blocks below to the user and
wait for the <resume-signal>. This gate is not auto-approvable: every claim it covers is a
judgement about how something sounds, which no gate in Tasks 1 or 2 can make. A green
`:app:check` is not a substitute for it.
  </action>
  <what-built>
Picking a peg up and putting it back down are now two different sounds.

The pick-up is freesound 506546 "Pop 01" by LilMati (CC0 1.0), replacing Kenney's
`select_002`. The put-down is that identical audio time-reversed and pitched down one whole
tone — the round-2 tuning you approved after a plain reversal read as too close to the pick-up
and a fifth down read as too far apart. Both files are peak-matched to the same −3.3 dBFS
target; the put-down's 10 dB drop lives in `SoundParams.VOLUME_DESELECT`, not in the file, so
the attenuation is applied exactly once.

Under the hood, `selectionTick` used to fire for both halves of the gesture; it now fires only
for the pick-up and a new `deselectionTick` fires for the put-down. D-05a's surviving half is
intact and tested: a put-down is a successful action and never plays the invalid-tap sound.
`CREDITS.md` and `07-CONTEXT.md` record the reversal, and the release APK was re-checked and
contains all twelve sounds.

**Four things no automated gate can settle, all needing your ears on a device:**

*The pair.* The gates prove the put-down is mathematically the pick-up reversed and a whole
tone down. They cannot tell you whether that reads as *two related sounds* rather than one
sound played twice, which is the thing you rejected in round 1.

*The level.* −10 dB was chosen by ear from a rendered pair. In game the put-down now sits
below even the invalid-tap cue — it is the quietest sound in the set. Check that it is still
clearly audible over the board rather than disappearing.

*The pick-up got louder.* This is the one consequence you have not been asked about. The old
Kenney tick peaked at −5.48 dBFS on disk; the new pop is normalised to −3.31, and
`VOLUME_SELECT` did not change — so **the pick-up is about 2.2 dB louder in game than before**.
That followed from the instructed −3.3 dBFS target, and it is a real change to the mix.

*The put-down is slightly longer.* Pitching down by resampling stretches it: 67 ms becomes
75 ms. Confirm the put-down still feels instant rather than draggy on a fast tap-tap.
  </what-built>
  <how-to-verify>
1. Install and launch the debug build on a device or emulator from a cold start.
2. Open any Classic level.
3. **The pair.** Tap a peg to pick it up, then tap it again to put it down. Repeat several
   times. Confirm the two sounds are clearly different events — and specifically that the
   put-down does not read as a second pick-up, which is what you rejected in round 1.
4. **Not too far apart.** Confirm the whole tone still reads as the *same* sound family. If it
   has drifted toward the fifth-down version you rejected in round 2, say so.
5. **The put-down's level.** Confirm it is audible but subordinate — it should sit under the
   pick-up without vanishing. Compare it against tapping an empty hole (the invalid-tap cue):
   the put-down is now deliberately quieter than that.
6. **The pick-up's new loudness.** Play a few jumps. Confirm the pick-up does not now
   over-power the jump pop it precedes. This is the 2.2 dB increase described above; if it is
   too hot, say so and the fix is one constant, not a re-encode.
7. **The invalid tap is untouched.** Tap the selected peg again (put-down), then tap an empty
   hole with nothing selected (invalid tap), then tap an illegal destination with a peg
   selected (invalid tap). Confirm the put-down never plays the invalid-tap sound — that is
   D-05a's surviving half.
8. **Speed.** Tap-tap-tap the same peg quickly. Confirm the 75 ms put-down keeps up and does
   not feel draggy or clipped.
9. **Cold start.** Confirm the very first pick-up after launch is not silent — the new cue is
   preloaded like the other eleven, but this is the check that proves it.
  </how-to-verify>
  <resume-signal>Type "approved", or say which of the four — the pair's distinctness, the put-down's level, the pick-up's new loudness, or the put-down's length — needs another pass.</resume-signal>
</task>

</tasks>

<coverage_audit>
Every locked decision and every hard problem named in the brief maps to a task. Nothing is
deferred, simplified, or left to a later pass.

| Item | Source | Covered by |
|---|---|---|
| D-VRR-01 pick-up sample is freesound 506546 | user audition | Task 1 |
| D-VRR-02 put-down derived from the same normalised intermediate | user + constraint | Task 1 (encode + correlation gate) |
| D-VRR-03 whole tone, and the two-round history recorded | user audition | Task 1 (encode + CREDITS narrative) |
| D-VRR-04 reverse + pitch baked into the asset, engine untouched | coordinator | Task 1 (asset), Task 2 (plain `play()` call) |
| D-VRR-05 −10 dB as a constant, never doubled into the file | coordinator | Task 2 (`VOLUME_DESELECT`), Task 1 (both assets at the same peak) |
| D-05a: surviving half — a deselect never fires the invalid tap | locked | Task 2 (branch order preserved, two tests assert it) |
| D-05a: reversed half recorded as a dated amendment | locked | Task 1 (07-CONTEXT.md) |
| HP1 `selectionTick` means two things; split it, enumerate at every site | brief | Task 2 (+ 6 counting gates) |
| HP2 Update the shared-cue tests rather than deleting them | brief | Task 2 (three test files, tightened) |
| HP3 Derived-work CREDITS row with HTTPS source + licence | brief | Task 1 (row gates + parsing rules in `<gate_notes>`) |
| HP4 `AudioCreditsTest` stale-UP-TO-DATE trap | brief | Tasks 1 and 2 (`--rerun`, escalation documented) |
| HP5 Re-read CREDITS.md as vb3 left it; fix falsified claims | brief | Task 1 (nine corrections, itemised) |
| HP6 Measure the ENCODED peak and iterate, for both files | brief | Task 1 (two sweeps in `<measured_facts>`) |
| HP7 Whether the reversed cue needs its own volume constant | brief | Resolved: it does. Measured at +1.90 dB RMS at equal peak; `VOLUME_DESELECT` is Task 2 |
| New `res/raw` id must survive the release shrinker (T-07-15) | derived | Task 2 (12-of-12 re-run; vb3's skip rationale does not apply) |
| Pre-existing false claim: "win stinger is the loudest" | found by measurement | Task 1 correction 5 |
| Pre-existing false claim: "0.13–0.54 s for the ten Kenney cues" | found by measurement | Task 1 correction 9 |
| New false claim created here: "invalid tap is the quietest cue" (×2) | derived | Task 2 (`SoundCue.kt`, `SoundParams.kt`, plus a test pinning the new truth) |
</coverage_audit>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| internet → repository | A binary audio asset downloaded from freesound.org is committed to `app/src/main/res/raw/` and ships inside the APK |
| untrusted tap → ViewModel | `onCellTap` routes an arbitrary board position through the branch table; the split adds a branch outcome |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-07-01 | Tampering | `app/src/main/res/raw/sfx_select.ogg`, `sfx_deselect.ogg` | mitigate | Master md5 (`2e5ddefd…`), codec, channels, sample rate, duration and size pinned in this plan and re-checked before use; `ffprobe` confirms both committed files are genuine mono Vorbis and not renamed non-audio payloads; provenance rows recorded in `CREDITS.md` and enforced by `AudioCreditsTest` |
| T-VRR-01 | Repudiation | `docs/audio/CREDITS.md` | mitigate | The per-file CC0 confirmation the document's own policy requires for a freesound borrow is performed and recorded for sound 506546 (uploader, title, CC0 deed URL, confirmation date), and the derived file's inheritance of that dedication is stated rather than assumed |
| T-VRR-02 | Repudiation | the shipped put-down asset | mitigate | The claim "this is the pick-up reversed and a whole tone down" is not taken on trust: a duration-ratio gate proves the resampling shift and a lag-aligned correlation gate proves the audio identity after both transforms are undone. Both were dry-run against three negative controls, including the round-1 variant the user rejected |
| T-VRR-03 | Tampering | stale `AudioCreditsTest` result | mitigate | `AudioCreditsTest` reads `CREDITS.md` from the working tree at runtime and that file is not a declared Gradle input, so a docs edit can leave `testDebugUnitTest` UP-TO-DATE; both tasks force the task with `--rerun` and document `--rerun-tasks` as the escalation |
| T-07-15 | Denial of service (feature) | resource shrinker stripping `raw/sfx_deselect` | mitigate | This is the first genuinely new `res/raw` id since the retention proof was written, so vb3's "only the payload changed" exemption does not apply. Task 2 re-runs `assembleRelease` + `aapt2 dump resources` and requires 12 of 12; the failure mode it guards is a put-down that is silent in release builds only |
| T-VRR-04 | Denial of service | `SoundPool` voice budget | accept | Both new cues are ~70 ms, far shorter than the 1.65 s star pop that forced the last `MAX_STREAMS` re-check, and a pick-up and a put-down cannot overlap each other (the second requires the first to have completed a state transition). Decoded footprints are 5,918 B and 6,644 B against the 1 MB per-sample limit. `MAX_STREAMS = 8` is untouched |
| T-VRR-05 | Information disclosure | ffmpeg re-encode | accept | The source is a public CC0 sound effect with no embedded PII; only encoder metadata is carried, as with the other eleven cues |
| T-07-SC | Tampering | npm/pip/cargo installs | n/a | No package-manager install occurs in this plan — the only new artifacts are an audio file and Kotlin source |
</threat_model>

<risks>
- **The peak target contradicts a measurement.** The instructed −3.3 dBFS is the star pop's
  target, not a set-wide level; the set spans −0.02 to −14.0 dBFS. Shipping the pick-up at
  −3.3 makes it 2.17 dB louder in game than the file it replaces. The instruction is honoured
  because it is what the user auditioned, but the consequence is surfaced at Task 3 step 6
  rather than absorbed silently. If the user calls it too hot, the fix is `VOLUME_SELECT`, not
  a re-encode.
- **The put-down becomes the quietest cue in the set**, below the invalid tap. That follows
  from the approved −10 dB and is not a bug, but it does retire two KDoc claims and it is the
  most likely thing to read as "missing" on a phone speaker. Task 3 step 5 is the only evidence
  that will exist.
- **A missed carry-forward site is invisible on the paths that would hide it.** The Challenge
  load and resume restore start from a loading board where no deselect is reachable, so a
  dropped `deselectionTick` there cannot be caught by a behavioural test. The counting gates on
  those exact expressions are the only guard; do not relax them into a single "appears
  somewhere" check.
- **`libvorbis` output is not byte-reproducible.** Re-running either encode gives identical
  audio under a different md5. Any future verification must compare measured properties, never
  hashes — and the peak sweep is non-monotonic, so a gain must be re-measured rather than
  interpolated.
- **The release build needs four untracked IDs.** Non-real values are the documented CI path
  and nothing is written to disk, but if the build cannot be produced the correct action is to
  stop and report, not to write an unverified retention claim into `CREDITS.md`.
</risks>

<verification>
Run from the repository root:

1. Asset properties — `ffprobe`/`astats` per `<pipeline_reference>`: both files vorbis / 1
   channel / 44100 Hz; durations 0.067098 s and 0.075329 s; both encoded peaks in
   −3.5 .. −3.0 dBFS.
2. The pair proof — duration ratio within 0.005 of `2^(2/12)`, and the reversal correlation
   above 0.90 after undoing `areverse` and the pitch shift.
3. `./gradlew :app:testDebugUnitTest --tests '*AudioCreditsTest*' --rerun --offline` — the
   credits gate, forced to execute. An UP-TO-DATE report means the gate did not run and the
   result is meaningless; escalate to `--rerun-tasks`.
4. `./gradlew :app:check --offline` — full gate: detekt, ktlint, the dp/sp literal check,
   domain purity, and the unit tests including `SoundParamsTest`, `GameViewModelSoundTest`,
   `GameScreenSoundTest` and `SoundPoolPlayerTest`.
5. `assembleRelease` + `aapt2 dump resources | grep -c 'raw/sfx_'` — expect **12**.
6. Human verification (Task 3) — on-device listening, which is the only evidence for the
   distinctness, the level balance and the pick-up's new loudness.
</verification>

<success_criteria>
- `app/src/main/res/raw/sfx_select.ogg` is freesound 506546 encoded from the original WAV:
  mono, 44.1 kHz, Ogg Vorbis, 0.067098 s, encoded peak within 0.25 dB of −3.3 dBFS.
- `app/src/main/res/raw/sfx_deselect.ogg` is provably the same audio time-reversed and pitched
  down one whole tone, at the same peak target, produced from the same normalised
  intermediate — proven by the duration-ratio and correlation gates, not asserted.
- `SoundCue` has twelve entries; `Deselect` binds `R.raw.sfx_deselect` as a compile-time
  constant and plays through the ordinary `play()` path with no engine change.
- `SoundParams.VOLUME_DESELECT` is derived from `VOLUME_SELECT` at exactly −10 dB, with KDoc
  giving the measured reason, and a test pins the ratio in decibels.
- `GameUiState.deselectionTick` exists, is enumerated in both factories, and is carried at all
  four `GameViewModel` rebuild sites; `selectionTick` no longer advances on a deselect.
- A put-down plays `SoundCue.Deselect` and never `SoundCue.InvalidTap`, asserted in both the
  ViewModel and the screen spec.
- Every test that asserted the old shared-cue behaviour still exists and asserts strictly more.
- No KDoc or comment in the touched Kotlin files asserts a relationship the code no longer has
  — including the three cue-count claims and the two "quietest cue" claims.
- `docs/audio/CREDITS.md` contains no false claim: corrected counts (twelve files, nine Kenney,
  three freesound from two sounds), the real duration and peak ranges, both freesound masters
  pinned and licence-confirmed, the full reproduction chain, the two-round tuning history, the
  derived-work rationale, and a shrink section recording a **12 of 12** run.
- `.planning/phases/07-sound/07-CONTEXT.md` carries a dated D-05a amendment recording the
  reversal and preserving the surviving half.
- `AudioCreditsTest` passed in a forced (non-cached) run; `./gradlew :app:check --offline` is
  green.
- The user has confirmed on a real device that the pick-up and put-down read as two related but
  distinct events, and has ruled on the pick-up's 2.2 dB level increase.
</success_criteria>

<output>
Create `.planning/quick/260828-vrr-distinct-peg-pick-up-and-put-down-sounds/260828-vrr-SUMMARY.md` when done.

Record in it, for STATE.md:
- D-05a is **half reversed**: pick-up and put-down are now distinct cues with a twelfth asset.
  The surviving half — a deselect is a successful action and never fires the invalid-tap cue —
  is unchanged and is now asserted in two specs. The amendment is dated in 07-CONTEXT.md.
- `selectionTick` no longer means two things. `deselectionTick` is the fourth cue ordinal and
  must be enumerated in both `GameUiState` factories and carried at all four `GameViewModel`
  rebuild sites; the `= 0` default cannot be relied on to cover a missed site, which is why the
  carry-forward sites are gated by exact-expression counts.
- The asset set is **not** normalised to one level. It is a per-cue peak balance spanning
  −0.02 dBFS (`sfx_jump`, the loudest file — not the win stinger, as CREDITS.md wrongly claimed
  until now) to −14.0 dBFS (`sfx_invalid`). −3.3 dBFS is the star pop's target from 260828-vb3.
  Recording the new pick-up there rather than at its predecessor's −5.48 made it 2.17 dB louder
  in game; the user ruled on that at the checkpoint.
- The put-down is now the quietest cue in the mix at `VOLUME_DESELECT` ≈ 0.19f, below
  `VOLUME_INVALID`. The reversed cue needed its own constant because it is measurably more
  present than its forward twin at equal peak (+1.90 dB RMS).
- D-03's cohesion has moved again: nine of twelve files are Kenney, three come from two
  freesound sounds, and the borrows now include the most frequently heard gameplay cue.
- The release-shrink retention proof was re-run because this change added the first genuinely
  new `res/raw` id since it was written — 260828-vb3's "only the payload changed" exemption did
  not apply.
</output>
