# Peg Solitaire — Audio Credits

Source: phase 7 (Sound) asset curation, 2026-08-28. Anchor pack: **Kenney _Interface Sounds_
(1.0)**, `https://kenney.nl/assets/interface-sounds`, released under **CC0 1.0**. The set is now
**twelve** files: **nine** are drawn from that single anchor pack, and **three** are third-party
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

Every file in `app/src/main/res/raw/` is **CC0 1.0 (public domain dedication)** — the nine Kenney
cues and the three freesound borrows alike. Attribution is therefore a courtesy, not an obligation,
and that follows from CC0 itself: the dedication waives copyright and neighbouring rights to the
extent permitted by law and imposes no attribution condition on downstream use. Kenney's own
licence text says the same thing in its own words ("this is not mandatory"), but the reasoning no
longer rests on that sentence, because it covers only nine of the twelve files. We credit anyway,
and this table is the in-repo provenance record D-01 requires. Kenney's *logo* is explicitly
excluded from the CC0 grant and is not used here.

Source filenames were renamed on commit because Android resource names must be `[a-z0-9_]` and
cannot start with a digit; the `original filename` column preserves provenance across that
rename.

## Files

| res/raw filename | original filename | source | licence |
|------------------|-------------------|--------|---------|
| `sfx_jump.ogg` | `Audio/drop_003.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_peg_remove.ogg` | `Audio/drop_002.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_select.ogg` | `506546__lilmati__pop-01.wav` (the original WAV download, not a preview) | https://freesound.org/people/LilMati/sounds/506546/ | CC0 1.0 |
| `sfx_deselect.ogg` | derived from `506546__lilmati__pop-01.wav` — the same normalised intermediate as `sfx_select.ogg`, time-reversed and pitched down a whole tone via `ffmpeg -af areverse` plus `asetrate` | https://freesound.org/people/LilMati/sounds/506546/ | CC0 1.0 |
| `sfx_undo.ogg` | `Audio/minimize_008.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_invalid.ogg` | `Audio/bong_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_hint.ogg` | `Audio/pluck_002.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_win.ogg` | `Audio/confirmation_002.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_star_pop.ogg` | `531512__cogfirestudios__positive-blip-effect.wav` (the original WAV download, not a preview) | https://freesound.org/people/CogFireStudios/sounds/531512/ | CC0 1.0 |
| `sfx_btn_forward.ogg` | `Audio/click_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_btn_dismiss.ogg` | `Audio/back_004.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_sound_enabled.ogg` | `Audio/confirmation_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |

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

## Release shrink verification

**Scope note: this section records a run performed on 2026-08-28 *before* the star-pop swap, and
has not been re-run since.** The `sfx_star_pop.ogg` payload was replaced afterwards, which makes
two of the figures below stale — the packaged-size range 4,087–7,011 B was measured across the
pre-swap asset set and excludes the current star pop (17,726 B), and the recorded APK size is
correspondingly out of date. The *retention* conclusion is unaffected and was not re-run on
purpose: the resource id and its compile-time `R.raw.sfx_star_pop` reference from `SoundCue` are
unchanged, only the payload bytes behind that id differ, and no `resources.getIdentifier()` path
exists — so there is nothing in this change that could alter whether the shrinker keeps the
resource. Re-running the documented command below is the way to re-confirm packaged bytes if that
is ever wanted.

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

**Artifact:** `app/build/outputs/apk/release/app-release-unsigned.apk` (2,038,611 B at the time of
the run; unsigned by design, D-16).

**Result: 11 of 11 `raw/sfx_` entries present.** `sfx_btn_dismiss`, `sfx_btn_forward`, `sfx_hint`,
`sfx_invalid`, `sfx_jump`, `sfx_peg_remove`, `sfx_select`, `sfx_sound_enabled`, `sfx_star_pop`,
`sfx_undo`, `sfx_win` — each named individually in the dump. Nothing was stripped, and no
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

All eleven packaged entries were byte-identical to the committed sources (4,087–7,011 B across the
pre-swap set) and **Stored**, not deflated — confirming aapt2 9.1.0's built-in `.ogg` handling and
that no `noCompress` configuration is needed (`grep noCompress app/build.gradle.kts` returns
nothing).

`./gradlew :app:check --offline` remains green after this verification; no source changed.
