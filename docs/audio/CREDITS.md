# Peg Solitaire — Audio Credits

Source: phase 7 (Sound) asset curation, 2026-08-28. Anchor pack: **Kenney _Interface Sounds_
(1.0)**, `https://kenney.nl/assets/interface-sounds`, released under **CC0 1.0**. Every one of
the eleven cues is drawn from that single pack, so the set is cohesive by construction (D-03)
and no third-party borrow was needed.

Every file in `app/src/main/res/raw/` is **CC0 1.0 (public domain dedication)**. Attribution is
therefore a courtesy, not an obligation — Kenney's own licence states crediting is "not
mandatory". We credit anyway, and this table is the in-repo provenance record D-01 requires.
Kenney's *logo* is explicitly excluded from the CC0 grant and is not used here.

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
| `sfx_star_pop.ogg` | `Audio/glass_005.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_btn_forward.ogg` | `Audio/click_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_btn_dismiss.ogg` | `Audio/back_004.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |
| `sfx_sound_enabled.ogg` | `Audio/confirmation_001.ogg` | https://kenney.nl/assets/interface-sounds | CC0 1.0 |

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

Each source clip was converted with ffmpeg 8.0 (`libvorbis`) into mono 44.1 kHz Vorbis (`-q:a 4`),
with leading silence trimmed (`silenceremove`) and a hard duration cap applied. Levels were
peak-normalised per cue rather than loudness-normalised: several cues are far shorter than
`loudnorm`'s ~3 s analysis window (`sfx_select.ogg` is 43 ms), where dynamic `loudnorm` yields
unusable gain. Peak targets encode the relative balance the requirements ask for — the invalid
tap is the quietest file in the set at −14 dBFS, the win stinger the loudest at −1.5 dBFS.

All eleven files are under 0.7 s (the win stinger, the longest, is 0.54 s), keeping every sample
far below `SoundPool`'s 1 MB decoded-PCM per-sample limit.

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
- **Codec confirmation per file.** `ffprobe -select_streams a:0 -show_entries stream=codec_name`
  reports `vorbis` for all eleven committed files, and `stream=channels` reports `1`, confirming
  each is genuine mono audio and not a renamed non-audio payload.
- **Duration confirmation per file.** `ffprobe -show_entries format=duration` reports under 0.7 s
  for all eleven.
- **Licence provenance.** All eleven files come from one CC0 pack whose licence text is
  reproduced above; no freesound (or other per-file-licence) borrow was made, so the per-file
  licence-confirmation step that a freesound borrow would require does not apply.
- **Completeness is enforced.** `AudioCreditsTest` fails the build if any file in
  `app/src/main/res/raw/` lacks a row here, or if a row omits an HTTPS source or a licence.
