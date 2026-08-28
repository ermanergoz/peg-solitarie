# Peg Solitaire — Audio Credits

Source: phase 7 (Sound) asset curation, 2026-08-28. Anchor pack: **Kenney _Interface Sounds_
(1.0)**, `https://kenney.nl/assets/interface-sounds`, released under **CC0 1.0**. **Ten** of the
eleven cues are drawn from that single pack. The eleventh, `sfx_star_pop.ogg`, is a single
third-party borrow from freesound (sound 531512 by CogFireStudios, also CC0 1.0), swapped in on
2026-08-28 — see [Star pop replacement](#star-pop-replacement-2026-08-28) below.

That swap **reverses D-03's single-pack cohesion guarantee**. D-03 held that drawing every cue
from one pack made the set cohesive *by construction*; with one cue sourced elsewhere, cohesion is
now an editorial judgement rather than a structural property of the asset set. The user made that
trade knowingly, after auditioning rendered previews of both the incumbent and the replacement.

Every file in `app/src/main/res/raw/` is **CC0 1.0 (public domain dedication)** — the ten Kenney
cues and the one freesound borrow alike. Attribution is therefore a courtesy, not an obligation,
and that follows from CC0 itself: the dedication waives copyright and neighbouring rights to the
extent permitted by law and imposes no attribution condition on downstream use. Kenney's own
licence text says the same thing in its own words ("this is not mandatory"), but the reasoning no
longer rests on that sentence, because it covers only ten of the eleven files. We credit anyway,
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
| `sfx_select.ogg` | `Audio/select_002.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
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

This is the borrow that reverses D-03, and it was accepted deliberately: one cue out of eleven now
comes from outside the anchor pack, so the set's coherence is something a listener judges rather
than something the sourcing rule guarantees. Nothing else in the set moved, and no further borrows
are planned.

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

Each of the ten Kenney clips was converted with ffmpeg 8.0 (`libvorbis`) into mono 44.1 kHz Vorbis
(`-q:a 4`), with leading silence trimmed (`silenceremove`) and a hard duration cap applied. Levels
were peak-normalised per cue rather than loudness-normalised: several cues are far shorter than
`loudnorm`'s ~3 s analysis window (`sfx_select.ogg` is 43 ms), where dynamic `loudnorm` yields
unusable gain. Peak targets encode the relative balance the requirements ask for — the invalid
tap is the quietest file in the set at −14 dBFS, the win stinger the loudest at −1.5 dBFS.

Ten files are under 0.7 s (the win stinger, the longest of those, is 0.54 s).
`sfx_star_pop.ogg` is the deliberate exception at **1.65 s**, and `SoundPool`'s 1 MB
decoded-PCM per-sample limit still has room to spare for it: 1.65 s × 44,100 Hz × 2 B
(16-bit mono) = **145,530 B**, about 14 % of the limit. No sample in the set comes near it.

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
- **The freesound borrow was fetched and pinned the same way.** The master for
  `sfx_star_pop.ogg` was obtained over HTTPS from the sound page as the **original WAV download**,
  not a preview. There is no archive to `unzip -l` because it is a single file rather than a pack,
  so the archive-inspection step is replaced by pinning the master itself: `pcm_s24le` / 2 ch /
  48 kHz / 3.004646 s / 866,074 B, md5 `02c2514f4b3953424eda3181aec8249b`, re-checked immediately
  before encoding. The `ffprobe` codec and channel confirmation below covers the committed `.ogg`
  exactly as it covers the other ten, so the renamed-non-audio-payload check is unbroken for it.
- **Codec confirmation per file.** `ffprobe -select_streams a:0 -show_entries stream=codec_name`
  reports `vorbis` for all eleven committed files, and `stream=channels` reports `1`, confirming
  each is genuine mono audio and not a renamed non-audio payload.
- **Duration confirmation per file.** `ffprobe -show_entries format=duration` reports 0.13–0.54 s
  for the ten Kenney cues and **1.650000 s** for `sfx_star_pop.ogg`. The outlier is by design
  (D-VB3-04), not an unnoticed encode error.
- **Licence provenance, and the per-file confirmation the borrow triggers.** The ten Kenney cues
  come from one CC0 pack whose licence text is reproduced above. The eleventh is a freesound
  borrow, so the per-file licence-confirmation step this document reserves for exactly that case
  **does** now apply and was performed: freesound sound **531512**, uploaded by
  **CogFireStudios**, whose sound page states Creative Commons 0 and links
  `https://creativecommons.org/publicdomain/zero/1.0/`. Confirmed at audition time, 2026-08-28.
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
