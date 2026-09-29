# Peg Solitaire — Audio Credits

**Current state (2026-09-29, `feature/audio-review`).** `app/src/main/res/raw/` holds **twelve**
files, all mono 16-bit 44.1 kHz PCM WAV (rules/audio.md; founder answer Q-009). **Ten** are
sourced and **two** are made in-house:

- **Seven** come from the Kenney _Interface Sounds_ (1.0) pack, CC0 1.0. They are rendered to WAV
  straight from the pack's own files.
- **Three** come from two freesound sounds, both CC0 1.0. `sfx_star_pop.wav` is sound 531512 by
  CogFireStudios. `sfx_select.wav` is sound 506546 by LilMati, and `sfx_deselect.wav` is derived
  from it.
- **Two** are made in-house from committed recipes: `sfx_invalid.wav` (the "soft tok") and
  `sfx_hint.wav` (the "soft glint"). The founder chose both in Q-008. They replace Kenney's
  `bong_001` and `pluck_002`, which no longer ship.

How each WAV was made, and the sha256 that pins it, is in
[WAV conversion and in-house renders](#wav-conversion-and-in-house-renders-2026-09-29). **The
sections after that are the record of the OGG set curated on 2026-08-28.** They are kept as
history and name the `.ogg` files of that time. Where the conversion changed a fact, the
conversion section says so.

Source: phase 7 (Sound) asset curation, 2026-08-28. Anchor pack: **Kenney _Interface Sounds_
(1.0)**, `https://kenney.nl/assets/interface-sounds`, released under **CC0 1.0**. The set was
then **twelve** files: **nine** drawn from that single anchor pack, and **three** third-party
borrows from freesound, coming from **two** freesound sounds — sound 531512 by CogFireStudios
(`sfx_star_pop.ogg`, swapped in on 2026-08-28, see
[Star pop replacement](#star-pop-replacement-2026-08-28) below) and sound 506546 by LilMati
(`sfx_select.ogg` and the `sfx_deselect.ogg` derived from it, 2026-08-28, see
[Peg pick-up / put-down pair](#peg-pick-up--put-down-pair-2026-08-28) below). All three borrows
are CC0 1.0.

Those borrows **reverse D-03's single-pack cohesion guarantee**. D-03 held that drawing every cue
from one pack made the set cohesive *by construction*; with three cues sourced elsewhere, cohesion
is now an editorial judgement rather than a structural property of the asset set. That understates
the position as of the pick-up/put-down swap: the borrow is no longer one celebration cue at the
edge of the set, it now includes the **most frequently heard gameplay cue** — peg selection — and
its derived twin, so the borrowed material is at the centre of the mix rather than at its rim. The
user made both trades knowingly, after auditioning rendered candidates against the incumbents.
On 2026-09-29 the set moved further from one pack: the wrong tap and the hint are now in-house
synth cues. The founder chose them in Q-008 after A/B clips against the incumbents.

Every **sourced** file in `app/src/main/res/raw/` is **CC0 1.0 (public domain dedication)** —
the seven Kenney cues and the three freesound borrows alike. The two in-house files are the
studio's own work, rendered from recipes committed in this repo, and no third party has a claim
on them. For the sourced files, attribution is a courtesy, not an obligation,
and that follows from CC0 itself: the dedication waives copyright and neighbouring rights to the
extent permitted by law and imposes no attribution condition on downstream use. Kenney's own
licence text says the same thing in its own words ("this is not mandatory"), but the reasoning does
not rest on that sentence, because it covers only seven of the twelve files. We credit anyway,
and this table is the in-repo provenance record D-01 requires. Kenney's *logo* is explicitly
excluded from the CC0 grant and is not used here. Each sourced file's licence verdict is the
research-analyst's to give (`docs/audio/licence-verdicts.md`), not this file's.

Source filenames were renamed on commit because Android resource names must be `[a-z0-9_]` and
cannot start with a digit; the `original filename` column preserves provenance across that
rename. For an in-house file there is no original: its `source` cell is the committed recipe it is
rendered from.

## Files

One row per file in `app/src/main/res/raw/`, and no other table in this document names a
`res/raw` filename (see the pick-up/put-down section for why).

| res/raw filename | original filename | source | licence | author | sha256 |
|------------------|-------------------|--------|---------|--------|--------|
| `sfx_jump.wav` | `Audio/drop_003.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | 830661a553299e102cdf045739d91079c2aed0b604bf756b1e8c993d557d4993 |
| `sfx_peg_remove.wav` | `Audio/drop_002.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | 1a3e31d59853c77b584b59e536eb81ccf2d364ce7b0700ce462de0202209530c |
| `sfx_select.wav` | `506546__lilmati__pop-01.wav` (the original WAV download, not a preview), by way of the single-encode `sfx_select.ogg` at c5b02ef | https://freesound.org/people/LilMati/sounds/506546/ | CC0 1.0 | freesound user LilMati; the file's own tags name the artist as Mattias "MATTIX" Lahoud | f2301135f4c1657f7671781749bf86deeb92e74002b8204655fb626e9ead7e01 |
| `sfx_deselect.wav` | derived from `506546__lilmati__pop-01.wav` — the same normalised intermediate as the pick-up, time-reversed and pitched down a whole tone via `ffmpeg -af areverse` plus `asetrate`, by way of the single-encode `sfx_deselect.ogg` at c5b02ef | https://freesound.org/people/LilMati/sounds/506546/ | CC0 1.0 | freesound user LilMati; derivative made by the studio | 264e7716247b5b371c17091efbc5e6b5ba5ec69dbc207ea7e85d5f593d189dcc |
| `sfx_undo.wav` | `Audio/minimize_008.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | 953208209159b712e4b4249ac8d8ffa9d1c7603638a37677c7cd8eb166225b04 |
| `sfx_invalid.wav` | none — in-house "soft tok", rendered with ergoz-soft `scripts/sfx-synth.sh` | audio/recipes/invalid.json | own work (ergoz-soft) | ergoz-soft sound-designer | 35e3ce1ede68b2a839dc307a1f28a6206fc6b7f7bdf64187411b7bf5cb3c13cb |
| `sfx_hint.wav` | none — in-house "soft glint", rendered with ergoz-soft `scripts/sfx-synth.sh` | audio/recipes/hint.json | own work (ergoz-soft) | ergoz-soft sound-designer | d4d73592076cf81c40726e8c1c573485f3d806a2230c9460681714f1dff86060 |
| `sfx_win.wav` | `Audio/confirmation_002.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | 6955beab73b662dfdbb3eb9c15aa3dbf1cd0cd445e068a55d1b6172c756da223 |
| `sfx_star_pop.wav` | `531512__cogfirestudios__positive-blip-effect.wav` (the original WAV download, not a preview), by way of the single-encode `sfx_star_pop.ogg` at c5b02ef | https://freesound.org/people/CogFireStudios/sounds/531512/ | CC0 1.0 | freesound user CogFireStudios | 22637d42c4b42372d384e77f238a968ef11d5fa8d311deb100654c654a2362ae |
| `sfx_btn_forward.wav` | `Audio/click_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | 2b03e4da0042c0c70d9a820da610043b71f8c5cd2ef2a0ca7a3860ce12d95e36 |
| `sfx_btn_dismiss.wav` | `Audio/back_004.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | fc63ab48c36cfb38411dacf621a03104b6c6b70a64f6021313bb4f92496a26ad |
| `sfx_sound_enabled.wav` | `Audio/confirmation_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 | Kenney (www.kenney.nl) | fc2a03331d67a172bce7527fa55fa86d00f27abfd6d07a0ead2e43ec8a976c69 |

Retired on 2026-09-29 and no longer shipped: `Audio/bong_001.ogg` (the old wrong tap) and
`Audio/pluck_002.ogg` (the old hint), both Kenney, CC0 1.0.

## WAV conversion and in-house renders (2026-09-29)

Founder answers: Q-008 (the wrong tap becomes candidate a, the "soft tok"; the hint becomes
candidate a, the "soft glint"; peg removal **keeps** its sound) and Q-009 (convert the set to WAV,
re-levelled in the same pass). The sound-designer made the renders with ffmpeg 9.0.2 and the
ergoz-soft 0.6.2 synthesizer.

**The two in-house cues.** Each recipe is the candidate the founder auditioned, committed
unchanged. It renders to the byte-identical file, and a second render is identical too:

```
SYN=<ergoz-soft plugin root>/scripts/sfx-synth.sh
"$SYN" audio/recipes/invalid.json app/src/main/res/raw/sfx_invalid.wav
"$SYN" audio/recipes/hint.json    app/src/main/res/raw/sfx_hint.wav
```

The recipes are levelled for the unchanged `VOLUME_INVALID` 0.3 and `VOLUME_HINT` 0.6, so nothing in
code changes for them. The soft tok is a triangle falling 420→360 Hz through a 1.2 kHz low-pass,
with a 20 ms muffled noise tap, 90 ms long, peaking at −5.5 dBFS. The soft glint is 1320 Hz with a
fifth above it and a faint 2640 Hz triangle, 220 ms long, peaking at −12.5 dBFS.

**The Kenney pack, pinned.** The pack was fetched again over HTTPS on 2026-09-29, and the archive
now carries a pinned hash, as the two freesound masters already did:

- URL: `https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip`
- 834,536 B; sha256 `f2193d072726d6758a5f7871b2dcc54dcce0d5c35c6f0a62f92549b327c81232`;
  md5 `763b97e426354c8f8e2bcf900927244b`.
- `unzip -l` before extraction listed `License.txt`, `Kenney.url`, `Patreon.url` and 100
  `Audio/*.ogg`, the same allow-list as in 2026-08. Only `Audio/*` and `License.txt` were
  extracted, and the licence text matches the one quoted below.

**The freesound masters are not used.** Their "download" links redirect to a login page, so the
original WAVs need an account. A freesound preview would be a worse source, because it is a lossy
transcode. The three freesound-derived cues are therefore decoded from their committed OGGs at
c5b02ef. Each of those is a single encode from the pinned master (see the sections below), so the
WAV adds no further loss. If an account-holder fetches the masters later, re-run the chains
below with `-c:a pcm_s16le` in place of libvorbis, and match the level to these files.

**The ten conversions, exactly as run** (from the repo root, with the pack extracted to `Audio/`):

```
for c in sfx_select sfx_deselect sfx_star_pop; do
  git show c5b02ef:app/src/main/res/raw/$c.ogg > $c.ogg
done
R=app/src/main/res/raw
wav() {  # <source> <start_sample> <end_sample or -> <gain dB or -> <output>
  af="atrim=start_sample=$2"; [ "$3" != - ] && af="$af:end_sample=$3"
  af="$af,asetpts=PTS-STARTPTS,aformat=sample_fmts=flt:channel_layouts=mono"
  [ "$4" != - ] && af="$af,volume=$4dB"
  ffmpeg -v error -y -i "$1" -af "$af" -ar 44100 -c:a pcm_s16le -map_metadata -1 \
    -fflags +bitexact -flags:a +bitexact "$5"
}
wav Audio/drop_003.ogg         0   -    -3.00 $R/sfx_jump.wav           # P4a: peak -1.10 dBFS
wav Audio/drop_002.ogg         0   -    -7.91 $R/sfx_peg_remove.wav     # level kept
wav Audio/minimize_008.ogg     809 -    -4.00 $R/sfx_undo.wav           # level kept
wav Audio/confirmation_002.ogg 2   -    -0.62 $R/sfx_win.wav            # level kept
wav Audio/click_001.ogg        0   -    0.35  $R/sfx_btn_forward.wav    # P4b: +4.14 dB, peak -1.00 dBFS
wav Audio/back_004.ogg         2   -    -7.15 $R/sfx_btn_dismiss.wav    # level kept
wav Audio/confirmation_001.ogg 2   -    -4.03 $R/sfx_sound_enabled.wav  # level kept
wav sfx_select.ogg             0   2959 -     $R/sfx_select.wav         # as shipped
wav sfx_deselect.ogg           0   3322 -     $R/sfx_deselect.wav       # as shipped
wav sfx_star_pop.ogg           0   -    -     $R/sfx_star_pop.wav       # as shipped
```

Term by term:

- **`aformat=sample_fmts=flt:channel_layouts=mono` comes before `volume`**, so the downmix and the
  gain both run in float and the only rounding is the final conversion to 16-bit. This matters
  because the two drop files decode to +1.9 dBFS in float. Converting to 16-bit before the gain
  would clip their attack.
- **The trims reproduce the shipped timing.** `start_sample` is where each shipped OGG's audio
  starts inside the pack file, found by cross-correlation. For undo this is 809 samples of
  lead-in, a trim the 2026-08 `silenceremove` made. For win, dismiss and sound-enabled it is
  2 samples, and for the rest it is 0. The tail is the whole decode, so undo, win and
  sound-enabled get back up to 128 samples (2.9 ms) of natural decay that the old OGG round trip
  lost. `end_sample` on select (2,959) and deselect (3,322) removes the Vorbis block padding and
  restores the designed lengths: 0.0671 s, and the whole-tone stretch of it.
- **The kept levels are measured, not guessed.** Each gain is the least-squares gain that
  matches the pack file's decode to the shipped OGG's decode, rounded to 0.01 dB. Correlation is
  0.993 or better, and the residual is the old second-generation Vorbis noise. The eight cues that
  keep their level measure within 0.1 LU of the OGG they replace (loudest 100 ms, K-weighted, full
  range, in `docs/audio-spec.md`). On the phone-speaker stand-in, dismiss reads 0.3 LU louder,
  because its centroid rises from 442 to 461 Hz with treble the second encode had cut.
- **P4a, the jump.** The pack file decodes to +1.9 dBFS, so the old OGG reached −0.02 dBFS and
  broke the −1 dBFS rule. At −3.00 dB the WAV peaks at −1.10 dBFS, 1.12 dB under the old level.
  `VOLUME_JUMP` 0.9 → 1.0 (+0.92 dB) is the android-game-dev's half of P4a. Together they move the
  jump by −0.2 dB in game, and the jump + removal layer peaks at −1.10 dBFS on every rung of the
  pitch ladder (simulated with linear resampling for rates 1.0 to 2.0). Until the constant
  changes, the jump plays 1.1 dB quieter and the layer peaks at −2.0 dBFS.
- **P4b, the forward button.** The proposal was +4.2 dB. The pack file's own peak allows +4.14 dB
  before the −1 dBFS rule, so the WAV peaks at −1.00 dBFS (true peak −0.9 dBTP). In game, forward is
  now louder than dismiss both at full range (−28.7 against −29.0 LUFS) and through the
  phone-speaker stand-in (−30.0 against −33.4). That matches `SoundCue.kt`'s "dismiss is softer",
  with no code change.
- **P4c, the deselect level, is not in this pass.** It is a code constant
  (`DESELECT_ATTENUATION`), not an asset level. The qa-engineer listens on a phone speaker first.
- **`-map_metadata -1 -fflags +bitexact -flags:a +bitexact`** write a bare 44-byte RIFF header
  with no tag chunk and no encoder string, so the bytes depend only on the audio. Two runs give
  identical files.
- **Decode with ffmpeg, not Apple's `afconvert`.** The Kenney files were encoded by GameSynth, and
  their first-page granule positions do not match their audio. `afconvert` honours the granule
  and starts `drop_003` 342 samples and `click_001` 646 samples later than ffmpeg does, which
  cuts off the attack.
  ffmpeg's decoder keeps the attack. The shipped OGGs were made from ffmpeg's decode, and these
  renders are too.

**Pinned output.** PCM output is deterministic, so unlike the old OGGs these files **can** be
verified by hash. The `sha256` column of the Files table pins every file:
`shasum -a 256 app/src/main/res/raw/*.wav` must print the same twelve values. The hashes hold for
ffmpeg 9.0.2 and the ergoz-soft 0.6.2 synthesizer. A different ffmpeg build may round
differently, and then the measured properties below are the gate.

**Measured properties.** `ffprobe` reports `pcm_s16le`, 1 channel, 44,100 Hz for all twelve.

- **Sizes:** 5,962 B (select) to 145,574 B (star pop), 327,394 B in total. Every file is under the
  200 KB limit.
- **Sample peaks:** −12.50 dBFS (hint) to −1.00 dBFS (forward). Every file is at or under −1 dBFS.
- **Durations:** within 128 samples (2.9 ms) of the OGGs they replace, except for the two in-house
  cues (90 ms and 220 ms). The old Vorbis containers and ffmpeg's decoder disagree by 128 samples
  on most of those files.
- **APK size:** the packaged `res/raw` grows by **255,712 B**, from 71,682 B of OGG to 327,394 B of
  WAV. Every entry is Stored, not deflated: aapt2 lists `.wav` as a no-compress extension, so no
  `noCompress` configuration is needed. On a clean `:app:packageDebug` the APK grew by exactly that
  much, from 20,637,687 B to 20,893,399 B. The release APK was not rebuilt, because it needs the
  signing secrets. It grows by the same number of stored bytes, from the 2,057,007 B recorded below
  to about 2.31 MB (+12.4 %).
- **The pick-up / put-down RMS pair, corrected.** The WAVs measure RMS −25.26 dB (select) and
  −24.53 dB (deselect) over their designed lengths, and their peaks are unchanged (−5.48 and
  −5.46 dBFS). The 2026-08 figure of −26.46 dB for `sfx_select.ogg` was measured over ffmpeg's
  decode of it. That decode runs 945 samples of Vorbis block padding past the sound's 2,959, which
  lowers the RMS by 1.2 dB. So the put-down's RMS lead at equal peak is **0.73 dB, not 1.92 dB**.
  The audio did not change, only the measurement window. The −10 dB `DESELECT_ATTENUATION` was
  tuned by ear and stands (its re-check is P4c), but any KDoc that quotes the old RMS pair should
  quote these figures.
- **Resource ids are unchanged.** `R.raw.sfx_*` resolves by resource name, not by extension, and the
  debug APK packages all twelve `res/raw/sfx_*.wav`. The release shrink check below was run on the
  OGG set, but it still holds for the same reason the star-pop swap gave: only the bytes behind
  each unchanged id changed.

**What the conversion leaves for others.** `AudioCreditsTest` scans only `*.ogg`, so it now finds
no sound files and fails its non-empty assertion. It also requires an `https://` source, and the
two in-house rows have a recipe path instead. Both are the android-game-dev's changes to make; the
test is not edited here. The KDoc in `SoundCue.kt` and `SoundParams.kt` still names
`sfx_deselect.ogg`.

### Star pop replacement (2026-08-28)

`sfx_star_pop.ogg` was Kenney's `Audio/glass_005.ogg` (0.135 s) until 2026-08-28. It is now
freesound sound **531512, "Positive Blip Effect" by CogFireStudios**, licensed **CC0 1.0**
(D-VB3-01). The Results celebration fires this cue once per earned star, up the major-third
ladder D-15 defines, and the glass sample never resolved the way the user wanted at the top of
that ladder. The user auditioned rendered previews of the candidates and chose this one.

This was the **first** borrow that reverses D-03, and it was accepted deliberately: a cue outside
the anchor pack means the set's coherence is something a listener judges rather than something the
sourcing rule guarantees. **This section is scoped to the star-pop swap and to the state of the
set on the morning of 2026-08-28**, when it was written: at that point nothing else in the set had
moved, and additional borrows were not anticipated. Both of those expectations were overtaken later
the same day by the pick-up/put-down swap — see
[Peg pick-up / put-down pair](#peg-pick-up--put-down-pair-2026-08-28) below, which replaced
`sfx_select.ogg` from a second freesound sound and added a twelfth file derived from it. The
sentence is corrected rather than deleted so the record shows both the expectation and what
overtook it.

**The trim is the full tail (D-VB3-04).** The shipped cue is the master's first **1.65 s**. An
earlier revision trimmed at 1.20 s; that figure was inherited from measuring freesound's lossy
*preview*, whose transcode had partly buried the decaying tail, so the short trim was an artefact
of the preview rather than an edit anyone chose. Measured against the master,
`silencedetect=n=-45dB:d=0.05` puts the first silence at 1.552 s with only faint residue after it
and true silence from 1.795 s, so 1.65 s captures the whole audible tail while the 1.20 s trim cut
into it. The user auditioned both trims rendered from the master and chose the full tail.

**Provenance quality.** The shipped file is a *single* lossy encode made directly from the
original 24-bit master downloaded from the sound's freesound page — the same provenance quality as
every Kenney file in the set, which are likewise one encode from their own sources. No preview
transcode is in the shipped bytes.

**Consequences recorded elsewhere in this file:** the duration spread is no longer "all under
0.7 s" (see Processing and Verification), and the release-shrink section below predates the swap
and is date-scoped accordingly.

### Peg pick-up / put-down pair (2026-08-28)

The user asked for the two halves of the selection gesture to sound different. Picking a peg up
and putting it back down previously played the *same* cue (D-05a as originally written). They are
now two sounds: `sfx_select.ogg` is freesound sound **506546, "Pop 01" by LilMati**, licensed
**CC0 1.0**, replacing Kenney's `Audio/select_002.ogg`; and `sfx_deselect.ogg` is that identical
audio time-reversed and pitched down one whole tone.

**The reproduction chain.** Both files come from one shared normalised intermediate, which is what
makes "the put-down is the pick-up reversed" a fact about the bytes rather than a claim about
intent. The intermediate is a build artefact and is not committed:

```
ffmpeg -y -i 506546__lilmati__pop-01.wav \
  -af "atrim=start=0.006:end=0.0731,asetpts=PTS-STARTPTS,volume=-5.65dB" \
  -ac 1 -ar 44100 -c:a pcm_s24le /tmp/vrr_pop_norm.wav

ffmpeg -y -i /tmp/vrr_pop_norm.wav -c:a libvorbis -q:a 4 \
  app/src/main/res/raw/sfx_select.ogg

ffmpeg -y -i /tmp/vrr_pop_norm.wav \
  -af "areverse,asetrate=44100*0.8909,aresample=44100,volume=0.30dB" \
  -c:a libvorbis -q:a 4 app/src/main/res/raw/sfx_deselect.ogg
```

Term by term:

- **`atrim` cuts silence off *both* ends**, and the trailing cut is the one that matters.
  `silencedetect=n=-45dB:d=0.005` puts the master's audible content at 0.00605–0.07308 s, with
  25 ms of sub-−45 dB silence after it. Leaving that in and then applying `areverse` would move
  it to the *front* of the put-down — 25 ms of dead air before the sound starts, which reads as a
  late, sloppy cue rather than as a response to a tap.
- **There is deliberately no fade, at either end.** The trimmed region's peak sample sits
  **1.41 ms** in (sample 62 of 2,959), so a conventional 3 ms fade-in would halve the transient
  that gives the pop its attack. No fade is needed: the boundary samples after trimming are −115
  and −178 out of 32,768, i.e. **−49 dBFS and −45 dBFS**, so the discontinuity at each end is
  already inaudible. A fade here would also have had to be symmetric to be reversal-invariant,
  which is a constraint worth avoiding when nothing requires the fade in the first place.
- **`-ac 1` and `-ar 44100`** fold to the mono 44.1 kHz every other cue in the set uses.
- **`pcm_s24le`** keeps the intermediate above the 16-bit source so neither of the two encodes
  that branch off it carries an avoidable requantisation.
- **`0.8909` is `2^(-2/12)`** to four places — one equal-tempered whole tone down. `asetrate`
  reinterprets the sample rate, so it shifts pitch and duration together; `aresample=44100`
  returns the stream to the set's rate afterwards.
- **`volume=0.30dB` on the put-down is encoder/resampler compensation, not a mix decision.** The
  reversal-plus-resample loses roughly 0.4 dB of *encoded* peak at equal PCM gain, and this trim
  puts it back. The mix decision lives in code (see below).

**The tuning history — why a whole tone.** The parameter has this value because two other values
were tried and rejected by ear, and a future reader who thinks the pitch shift is redundant should
find that out here rather than by shipping it.

- **Round 1 was a plain reversal at equal pitch**, no `asetrate` at all. The user rejected it as
  indistinguishable from the pick-up — "too close to each other". A ~98 ms transient played
  backwards simply does not read as reversed at that length; there is not enough envelope for the
  ear to hear the swell. **The pitch shift, not the reversal, is what carries the distinction.**
- **Round 2 tried a fifth down** (7 semitones). The user judged it too far apart — "reduce the
  difference" — because at that interval the put-down stops reading as a sibling of the pick-up
  and starts reading as an unrelated event.
- **A whole tone (2 semitones) is where it landed**, and it is the approved middle: clearly the
  same sound family, clearly not the same event.

Deleting the `asetrate` term re-creates the round-1 variant that was already rejected. Widening it
re-creates round 2.

**The −10 dB is not in the file.** Both shipped assets are peak-matched on disk on purpose; the
put-down's level sits in `SoundParams.VOLUME_DESELECT`, derived as `VOLUME_SELECT` minus 10 dB.
Attenuating the asset *and* keeping the constant would apply the drop twice, so the file on disk
is deliberately not a quiet outlier. The reason the put-down needs its own level at all is
measured, not assumed: at the same encoded peak the put-down's RMS is **1.92 dB higher** than the
pick-up's (−24.536 dB against −26.459 dB), because a slow swell carries more energy relative to
its peak than a sharp attack does. Peak-matched, the reversed cue simply sounds more present.

**The peak target, and a correction to the brief that produced it.** These two files are recorded
at **−5.476 dBFS**, which is the measured peak of the `sfx_select.ogg` they replace — *not* the
−3.3 dBFS that the star pop uses.

There is no house normalisation level in this project. Measured across the set on 2026-08-28 the
peaks span **−0.018 dBFS (`sfx_jump`) to −14.008 dBFS (`sfx_invalid`)**; −3.3 dBFS was only ever
the star pop's own target, inherited in turn from the file that cue replaced. The set is a
deliberate per-cue balance, not one level, and a "normalise the new asset to −3.3 like everything
else" instruction rests on a premise that measurement does not support.

Matching the incumbent's −5.476 keeps this change scoped to *a different sound* rather than *a
different and louder sound*. Peg selection is the most frequently heard cue in the game; recording
it at −3.3 would have made it **2.17 dB louder in game** at the unchanged `VOLUME_SELECT`, an
unrequested change to the mix. The user tuned the pick-up/put-down pair by ear for their
**relative** difference, so the relative relationship is what is preserved here and the absolute
level is held at the incumbent's.

**The gain was found by sweeping and measuring the encoded result**, never by normalising the PCM
and trusting it. libvorbis overshoots and jitters the peak on this transient by ~0.2 dB between
neighbouring pre-encode gains, so the sweep is visibly non-monotonic and a gain **must not** be
interpolated from neighbouring measurements. Measured with `astats` on the encoded `.ogg`:
−5.382 dBFS at −5.40 dB, −5.341 at −5.50, −5.321 at −5.60, **−5.483 at −5.65**, −5.576 at −5.70,
−5.860 at −5.80. −5.65 dB was chosen: it lands within **0.008 dB** of the −5.476 dBFS the
incumbent measured. The put-down's `volume=0.30dB` trim was swept the same way at that new base
and re-confirmed there rather than assumed to carry over — it gives −5.465 dBFS, within 0.019 dB
of its forward twin. `loudnorm` was not used, for the same reason it is rejected elsewhere in this
document: these cues are ~70 ms against its ~3 s analysis window.

**The shipped pair, measured.** Given as prose rather than as a second markdown table on purpose:
the Files table above is the single provenance record, and exactly one table row may exist per
filename (`AudioCreditsTest` matches the first table line containing a filename, and a duplicate
row would be a second, unpoliced place for a provenance claim to drift).

- `sfx_select.ogg` — vorbis, 1 ch, 44100 Hz, 0.067098 s, 4,595 B, peak −5.483288 dBFS,
  RMS −26.459491 dB.
- `sfx_deselect.ogg` — vorbis, 1 ch, 44100 Hz, 0.075329 s, 4,643 B, peak −5.464545 dBFS,
  RMS −24.535964 dB.

**The duration ratio is the proof that the pitch shift is present.** Because `asetrate` couples
pitch and time, 0.075329 / 0.067098 = **1.122671**, against `2^(2/12)` = **1.122462** — a whole
tone, to within 0.0002. Undoing both transforms on the shipped put-down and correlating the result
against the shipped pick-up (searching lags, to absorb the two files' differing Vorbis encoder
padding) scores **0.9875**. That gate is not trivially satisfiable: the round-1 plain reversal
scores 0.539 and a copy of the pick-up scores 0.010, both far below the 0.90 threshold.

**The derivative inherits the CC0 dedication.** `sfx_deselect.ogg` is a derived work with no
freesound page of its own, and its provenance row therefore cites its parent's URL. That is the
honest reading rather than a convenience: CC0 waives copyright and neighbouring rights and imposes
no share-alike and no notice condition, so a derivative of a CC0 work carries the same public-domain
status as its source, and no separate per-file licence confirmation is possible or needed for it.
The confirmation that matters is the one performed on sound 506546 itself (see Verification).

**Verify both files by measured properties, never by hash.** libvorbis is not byte-deterministic:
three identical invocations of the pick-up encode here produced identical *audio* (same peak, same
byte count) under three different md5s. A hash gate on either `.ogg` would fail spuriously on any
re-encode.

## Licence text as shipped by the source

Reproduced verbatim from the pack's own `License.txt`:

> Interface Sounds (1.0)
> Created/distributed by Kenney (www.kenney.nl)
> Creation date: 11-02-2020
>
> License: (Creative Commons Zero, CC0)
> http://creativecommons.org/publicdomain/zero/1.0/
>
> This content is free to use in personal, educational and commercial projects.
> Support us by crediting Kenney or www.kenney.nl (this is not mandatory)

## Processing

*The OGG set of 2026-08-28. The WAV conversion above superseded it on 2026-09-29.*

Each of the nine Kenney clips was converted with ffmpeg 8.0 (`libvorbis`) into mono 44.1 kHz Vorbis
(`-q:a 4`), with leading silence trimmed (`silenceremove`) and a hard duration cap applied. Levels
were peak-normalised per cue rather than loudness-normalised: several cues are far shorter than
`loudnorm`'s ~3 s analysis window (`sfx_select.ogg` is 67 ms), where dynamic `loudnorm` yields
unusable gain. Peak targets encode the relative balance the requirements ask for, and that balance
spans a wide range: measured on 2026-08-28 the quietest file in the set is `sfx_invalid.ogg` at
**−14.0 dBFS** and the loudest is `sfx_jump.ogg` at **−0.02 dBFS**. The win stinger, at
−1.6 dBFS, is the loudest *celebration* cue but is not the loudest file.

Eleven of the twelve files are under 0.7 s (the win stinger, the longest of those, is 0.54 s).
`sfx_star_pop.ogg` is the deliberate exception at **1.65 s**, and `SoundPool`'s 1 MB
decoded-PCM per-sample limit still has room to spare for it: 1.65 s × 44,100 Hz × 2 B
(16-bit mono) = **145,530 B**, about 14 % of the limit. No sample in the set comes near it — the
two newest files, `sfx_select.ogg` and `sfx_deselect.ogg`, decode to **5,918 B** and **6,644 B**
respectively, well under 1 % of the limit each.

### The star-pop chain

`sfx_star_pop.ogg` is reproducible from the master in one ffmpeg pass:

```
ffmpeg -y -i 531512__cogfirestudios__positive-blip-effect.wav -t 1.65 -ac 1 -ar 44100 \
  -af "afade=t=out:st=1.50:d=0.15,volume=1.40dB" \
  -c:a libvorbis -q:a 4 app/src/main/res/raw/sfx_star_pop.ogg
```

Term by term: `-t 1.65` is the full-tail trim (D-VB3-04); `-ac 1` folds the stereo master to mono
as every other cue is; `-ar 44100` resamples from the master's 48 kHz to the set's 44.1 kHz;
`afade` fades out over the last 150 ms (1.50 s → 1.65 s) so the trim cannot click; `volume=1.40dB`
is the peak normalisation; `-q:a 4` matches the rest of the set.

**`silenceremove` is deliberately absent.** The master has no leading silence — its transient is
at the very start — so trimming would move the onset off the rendering the user approved rather
than tidy anything.

**The +1.40 dB gain was measured against the *encoded* peak, not the pre-encode peak.** libvorbis
overshoots on this transient, so lifting the PCM to the −3.25 dBFS target before encoding lands
the encoded file audibly louder than the sample it replaced. Sweeping the gain and measuring the
result with `astats` gave −3.591 dBFS at +1.20 dB, **−3.338 dBFS at +1.40 dB**, and −3.014 dBFS at
+1.50 dB. +1.40 dB was chosen: it lands within 0.09 dB of the old asset's −3.248 dBFS, so the
per-cue balance documented above survives the swap unchanged. `loudnorm` was not used here either,
for the same reason it was rejected for the rest of the set.

**Verify this file by measured properties, never by hash.** libvorbis is not byte-deterministic —
two identical invocations here produced bit-identical *audio* (same peak, same byte count) under
different md5s. A hash gate on the `.ogg` would fail spuriously on any re-encode.

## Verification

*For the OGG set of 2026-08-28. The WAV set's checks (pack hash, listing, codec, hashes) are in
the conversion section above.*

Checks performed at commit time, mitigating T-07-01 (tampering across the internet → repository
trust boundary):

- **HTTPS-only download.** The pack was fetched with `curl -L --proto '=https' --tlsv1.2`, so no
  plaintext or protocol-downgrade fetch was possible.
- **Archive listing inspected before extraction.** `unzip -l` was run on the downloaded zip
  *before* anything was unpacked. The listing contained only `License.txt`, two 45/55-byte
  plain-text `.url` shortcuts, and 100 `Audio/*.ogg` files — no executable, script, `.sh`,
  `.command`, `.jar`, or nested archive.
- **Selective extraction.** Only `Audio/*` and `License.txt` were extracted. The two `.url`
  shortcut files were deliberately excluded rather than unpacked, since they fall outside the
  audio + licence allow-list.
- **Both freesound borrows were fetched and pinned the same way.** Each master was obtained over
  HTTPS from its sound page as the **original WAV download**, not a preview. There is no archive
  to `unzip -l` in either case because each is a single file rather than a pack, so the
  archive-inspection step is replaced by pinning the master itself, re-checked immediately before
  encoding:
  - sound **531512** (`sfx_star_pop.ogg`): `pcm_s24le` / 2 ch / 48 kHz / 3.004646 s / 866,074 B,
    md5 `02c2514f4b3953424eda3181aec8249b`.
  - sound **506546** (`sfx_select.ogg`, and `sfx_deselect.ogg` derived from it): `pcm_s16le` /
    2 ch / 44100 Hz / 0.098526 s / 17,706 B, md5 `2e5ddefd4d3741ad4221245950904ded`.

  The `ffprobe` codec and channel confirmation below covers the committed `.ogg` files exactly as
  it covers the Kenney ones, so the renamed-non-audio-payload check is unbroken for all of them.
- **Codec confirmation per file.** `ffprobe -select_streams a:0 -show_entries stream=codec_name`
  reports `vorbis` for all twelve committed files, and `stream=channels` reports `1`, confirming
  each is genuine mono audio and not a renamed non-audio payload.
- **Duration confirmation per file.** `ffprobe -show_entries format=duration` reports
  **0.097–0.539 s** across the nine Kenney cues, **0.067098 s** for `sfx_select.ogg` and
  **0.075329 s** for `sfx_deselect.ogg` (the two freesound pop files, the shorter of which is the
  shortest cue in the set), and **1.650000 s** for `sfx_star_pop.ogg`. That last outlier is by
  design (D-VB3-04), not an unnoticed encode error. An earlier revision of this bullet gave the
  Kenney range as 0.13–0.54 s, which was already wrong before the pop swap — it silently excluded
  the then-incumbent `sfx_select.ogg` at 0.043 s.
- **Licence provenance, and the per-file confirmations the borrows trigger.** The nine Kenney cues
  come from one CC0 pack whose licence text is reproduced above. Three files are freesound
  borrows, so the per-file licence-confirmation step this document reserves for exactly that case
  **does** apply and was performed for each source sound:
  - freesound sound **531512**, "Positive Blip Effect", uploaded by **CogFireStudios**, whose
    sound page states Creative Commons 0 and links
    `https://creativecommons.org/publicdomain/zero/1.0/`. Confirmed at audition time, 2026-08-28.
  - freesound sound **506546**, "Pop 01", uploaded by **LilMati**, whose sound page states
    Creative Commons 0 and links `https://creativecommons.org/publicdomain/zero/1.0/`. Confirmed
    at audition time, 2026-08-28.

  `sfx_deselect.ogg` needs no separate confirmation of its own: it is a derivative of the same
  CC0 source (506546) and inherits that dedication, as explained in the pick-up/put-down section
  above.
- **Completeness is enforced.** `AudioCreditsTest` fails the build if any file in
  `app/src/main/res/raw/` lacks a row here, or if a row omits an HTTPS source or a licence.
  (As of 2026-09-29 the test still scans `*.ogg` only. It must learn `*.wav` and recipe-path
  sources before it polices the WAV set again; see the conversion section.)

## Release shrink verification

**Scope note: this section records a run performed on 2026-08-28 *after* the pick-up/put-down
swap, and covers the twelve-file OGG set of that date.** The 2026-09-29 WAV conversion kept every
resource id and changed only the bytes behind them, so the retention conclusion carries over. The
packaged sizes and the Stored check for the WAV set are in the conversion section, from a debug
package. A release re-run needs the signing secrets. It supersedes an earlier run that predated
both the star-pop swap and this one.

**Why it was re-run this time, when the star-pop swap deliberately skipped it.** That swap
replaced only the *payload bytes* behind an unchanged resource id, so nothing about it could
alter whether the shrinker keeps the resource, and the retention conclusion carried over. The
pick-up/put-down swap is different in kind: `sfx_deselect.ogg` is the first genuinely **new**
`res/raw` id added since this section was written. A new id the shrinker cannot trace is exactly
the failure this section exists to catch — a put-down that is silent in release builds only — so
the earlier exemption does not apply and the verification was performed again in full.

Performed 2026-08-28, mitigating T-07-15. The release build runs with `isMinifyEnabled = true`
and `isShrinkResources = true`, so what ships is not what debug testing exercised: a `res/raw`
entry the shrinker cannot trace is dropped, and the failure mode is a game that is silent **in
release only**. Every cue is referenced as a compile-time `R.raw.*` constant from `SoundCue`, so
retention is expected — but expected is not verified. This repeats, for audio, the technique
`app/build.gradle.kts` already documents for `font_certs.xml`.

```
./gradlew :app:assembleRelease --offline
AAPT2=$(find ~/.gradle/caches -maxdepth 6 -type d -name 'aapt2-9.1.0*' | head -1)/aapt2
$AAPT2 dump resources app/build/outputs/apk/release/app-release-unsigned.apk | grep 'raw/sfx_'
```

Release invocations require the four real ad / Play Games IDs (D-09), which are untracked. This
verification supplied them as deliberately non-real environment variables for the single
invocation (`ADMOB_APP_ID` / `ADMOB_REWARDED_AD_UNIT_ID` / `ADMOB_INTERSTITIAL_AD_UNIT_ID` /
`PGS_PROJECT_ID`) — the documented CI path, nothing written to disk or committed. The IDs do not
affect resource shrinking; they only gate whether the build starts.

**Artifact:** `app/build/outputs/apk/release/app-release-unsigned.apk` (2,057,007 B at the time of
the run; unsigned by design, D-16).

**Result: 12 of 12 `raw/sfx_` entries present.** `sfx_btn_dismiss`, `sfx_btn_forward`,
**`sfx_deselect`**, `sfx_hint`, `sfx_invalid`, `sfx_jump`, `sfx_peg_remove`, `sfx_select`,
`sfx_sound_enabled`, `sfx_star_pop`, `sfx_undo`, `sfx_win` — each named individually in the dump,
including the newly added `sfx_deselect` this run existed to check. Nothing was stripped, and no
`resources.getIdentifier()` workaround exists anywhere in `app/src/main/` (which would have
defeated the shrinker rather than satisfied it).

A surviving table entry is a weaker claim than a surviving sound, because the shrinker can keep a
resource id while replacing its payload. Each entry was therefore followed to its packaged file
and compared byte-for-byte with its source. Resource **paths** are shortened in the shrunk APK
(`raw/sfx_jump` → `res/NK.ogg`), which is expected and harmless — the resource table carries the
mapping, and `SoundCue` resolves by id, never by path:

```
$AAPT2 dump resources <apk> | grep -A2 'raw/sfx_'      # id -> shortened res/<xx>.ogg
unzip -l <apk> res/<xx>.ogg                            # packaged size per entry
```

All twelve packaged entries were byte-identical to the committed sources and **Stored**, not
deflated — confirming aapt2 9.1.0's built-in `.ogg` handling and that no `noCompress`
configuration is needed (`grep noCompress app/build.gradle.kts` returns nothing). Packaged sizes
span **4,087 B** (`sfx_invalid`) to **17,726 B** (`sfx_star_pop`, the 1.65 s outlier); the two
newest entries are `sfx_select` at 4,595 B and `sfx_deselect` at 4,643 B.

`./gradlew :app:check --offline` remains green after this verification; no source changed.
