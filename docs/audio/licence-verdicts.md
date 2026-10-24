# Licence verdicts — sourced sound effects

Per-file licence verdicts under `.claude/rules/ip-policy.md` for every **sourced** sound that
stays in the game after the founder's answers to Q-008 (new sounds) and Q-009 (WAV). Written
2026-09-29 by the research-analyst, task `audio-review`. All evidence was checked on 2026-09-29.

- **Scope.** Ten shipped files, taken from three sources: seven from Kenney *Interface Sounds*
  1.0, and three from two freesound sounds (531512, and 506546 plus one file derived from it).
  File names are the `.wav` names the sound-designer committed in d1c6e9b (Q-009), with the
  basenames unchanged. The seven Kenney files were rendered from the pack's own `.ogg` files.
  The three freesound-derived files were decoded from the single-encode `.ogg` files committed
  up to `develop` 3f2af90, because downloading the freesound masters needs an account. Every
  check below was run on the files as committed.
- **Out of scope.** `sfx_invalid.wav` and `sfx_hint.wav` are in-house renders (Q-008 answer:
  "soft tok" and "soft glint", commit af2b554). Under `rules/audio.md` (In-house) they are
  rendered from `audio/recipes/invalid.json` and `audio/recipes/hint.json` with the studio's
  `sfx-synth.sh`. That script only generates oscillators
  (sine, square, triangle, saw) and noise seeded from the recipe, with no sample input and no
  network access. So the renders contain no third-party material. `ip-policy.md` asks for a
  row per *borrowed* asset only, so these two files need no third-party verdict. See
  [In-house cues](#in-house-cues-no-verdict-needed).
- **Matrix row applied.** Every row below uses the `ip-policy.md` row
  `| CC0 | yes | yes | attribution optional |`. Both code and asset reuse are allowed. We credit
  the sources anyway, in `docs/audio/CREDITS.md` and `THIRD_PARTY.md`.
- **Verdict scale.** CLEAN = reuse allowed, no escalation. ESCALATE = founder decision needed.
  NO = must not ship.

## Verdicts, one row per shipped file

| # | Shipped file | Source URL | Source file | Author | Licence | Evidence (checked 2026-09-29) | Pin | Verdict |
|---|---|---|---|---|---|---|---|---|
| 1 | `sfx_jump.wav` | https://kenney.nl/assets/interface-sounds | `Audio/drop_003.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 1.000 (E-P) | archive `f2193d07…81232`; file sha256 `43d086d50f90493a98cd2e09aa6dbf150d164f1578e1ec768d21cb185571ad87` | **CLEAN** |
| 2 | `sfx_peg_remove.wav` | https://kenney.nl/assets/interface-sounds | `Audio/drop_002.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 1.000 (E-P) | archive `f2193d07…81232`; file sha256 `4ac4d1cef7e936965cbf795852ca2020300b9e2ba7daa59f2bf4f1f7bf416218` | **CLEAN** |
| 3 | `sfx_undo.wav` | https://kenney.nl/assets/interface-sounds | `Audio/minimize_008.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 0.999 (E-P) | archive `f2193d07…81232`; file sha256 `bc14c17875436dcaaf809c25db5e61454dbc35612880f329ff37a95ef9b6a865` | **CLEAN** |
| 4 | `sfx_win.wav` | https://kenney.nl/assets/interface-sounds | `Audio/confirmation_002.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 0.927 (E-P) | archive `f2193d07…81232`; file sha256 `33b17a9a9a2397c62b285c52c33a907fdffb476909c99e42dde603f6a7a8b12c` | **CLEAN** |
| 5 | `sfx_btn_forward.wav` | https://kenney.nl/assets/interface-sounds | `Audio/click_001.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 1.000 (E-P) | archive `f2193d07…81232`; file sha256 `ccfb7fa0cccdd9faec0eb16033c732b1e308d139d80f799161495d58f7adcdb9` | **CLEAN** |
| 6 | `sfx_btn_dismiss.wav` | https://kenney.nl/assets/interface-sounds | `Audio/back_004.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 0.984 (E-P) | archive `f2193d07…81232`; file sha256 `c22e3c49f42c2a5ba6748e91283efe124f32822469c1258c3e5d7b3d7ab9361c` | **CLEAN** |
| 7 | `sfx_sound_enabled.wav` | https://kenney.nl/assets/interface-sounds | `Audio/confirmation_001.ogg` | Kenney (kenney.nl) | CC0 1.0 | E-K1, E-K2, E-K3; provenance 0.989 (E-P) | archive `f2193d07…81232`; file sha256 `063564703b6094d70718a3e787a55cc9141611e4ecd6b6637f8828f79b4a8c3a` | **CLEAN** |
| 8 | `sfx_star_pop.wav` | https://freesound.org/people/CogFireStudios/sounds/531512/ | "Positive Blip Effect", original WAV `531512__cogfirestudios__positive-blip-effect.wav` | CogFireStudios | CC0 1.0 | E-F1, E-F2, E-F3, E-F4, E-F6; provenance 0.998 against the public preview (E-P) | master md5 `02c2514f4b3953424eda3181aec8249b` (CREDITS.md), then the intermediate `sfx_star_pop.ogg` at 3f2af90, sha256 `8b74db456822b6b6bec1821ec9a907b7a1a2d40b1504c8c50e27336e4d193c2a`; see [Pins](#pins) | **CLEAN** |
| 9 | `sfx_select.wav` | https://freesound.org/people/LilMati/sounds/506546/ | "Pop 01", original WAV `506546__lilmati__pop-01.wav` | LilMati (the same account was named MATRIXXX_ until at least 2023-12); the master's tags name Mattias "MATTIX" Lahoud | CC0 1.0 | E-F1, E-F2, E-F3, E-F4, E-F5, E-F6; provenance 0.975 against the public preview (E-P) | master md5 `2e5ddefd4d3741ad4221245950904ded` (CREDITS.md), then the intermediate `sfx_select.ogg` at 3f2af90, sha256 `3f967b4ca8dcadcbe64917a65ca276e5069afb39a3cdac8395726234a0eb0738`; see [Pins](#pins) | **CLEAN** |
| 10 | `sfx_deselect.wav` | https://freesound.org/people/LilMati/sounds/506546/ (parent) | derived in-repo from the 506546 master: trimmed, reversed, one whole tone lower | LilMati (parent); the transformation is ours | CC0 1.0 (derivative of a CC0 work) | Parent evidence as row 9; E-D; provenance 0.996 after undoing the reversal and the pitch shift (E-P) | the parent master pin (row 9), then the intermediate `sfx_deselect.ogg` at 3f2af90, sha256 `071c3cbee4403be13e26dab9152577e6f7766990e50871ba74d1b9e50c5818a7` | **CLEAN** |

**Result: 10 CLEAN, 0 ESCALATE, 0 NO.** No copyleft, no share-alike, no non-commercial and no
unknown licence reaches the product.

## Evidence

### Kenney *Interface Sounds* 1.0 (rows 1–7)

- **E-K1 — live asset page.** https://kenney.nl/assets/interface-sounds (HTTP 200, 2026-09-29).
  The page states "License: Creative Commons CC0", "Updates: 1.0 Released in 2020" and "Files:
  100×". Its metadata reads "Download this package (100 assets) for free, CC0 licensed!".
  Kenney's support page (https://kenney.nl/support) adds that the assets "are public domain
  licensed (CC0)", that attribution is not required, and that you may credit "Kenney" but must
  not use the Kenney logo.
- **E-K2 — licence file in the archive.** `License.txt` in the pinned archive says:
  "Interface Sounds (1.0) / Created/distributed by Kenney (www.kenney.nl) / Creation date:
  11-02-2020 / License: (Creative Commons Zero, CC0)
  http://creativecommons.org/publicdomain/zero/1.0/ / This content is free to use in personal,
  educational and commercial projects." This matches, word for word, the text that
  `docs/audio/CREDITS.md` quoted in phase 07. The file's sha256 is listed under [Pins](#pins).
- **E-K3 — licence at release.** The oldest Wayback Machine capture of the asset page,
  https://web.archive.org/web/20200622144310/https://www.kenney.nl/assets/interface-sounds,
  says the pack is "CC0 1.0 Universal". That capture is four months after the pack's creation
  date. So the pack has been CC0 from 2020 until today, with no other licence observed at any
  time.
- **Author.** Kenney. `License.txt` names Kenney as the pack's creator and distributor (E-K2),
  and nothing in the pack or on the page credits anyone else. `ip-policy.md`'s 95-year term for
  corporate-authored works is beside the point, because the rights holder released the pack
  under CC0.

### freesound 531512 and 506546 (rows 8–10)

- **E-F1 — live sound pages** (HTTP 200, 2026-09-29). Both pages show the licence as "Creative
  Commons 0" and link https://creativecommons.org/publicdomain/zero/1.0/, with the text "You can
  copy, modify, distribute and perform the sound, even for commercial purposes, all without the
  need of asking permission to the author."
  - 531512 "Positive Blip Effect" by CogFireStudios. Uploaded 2020-08-20, in the uploader's own
    pack "Dreamy Beeps" (29936). Page specs: WAV, 0:03.004, 845.8 KB, 48 kHz, 24-bit, stereo.
    These match the master pinned in CREDITS.md (866,074 B = 845.8 KiB).
  - 506546 "Pop 01" by LilMati. Uploaded 2020-02-21, tags include "mouth" and "cartoon". Page
    specs: WAV, 0:00.098, 17.3 KB, 44.1 kHz, 16-bit, stereo. These match the pinned master
    (17,706 B = 17.3 KiB).
- **E-F2 — archived pages.**
  - https://web.archive.org/web/20231215104553/https://freesound.org/people/MATRIXXX_/sounds/506546/
    shows "Pop 01.wav by MATRIXXX_", uploaded February 21st, 2020, signed "MATRIXXX a.k.a. Lil
    Mati", licence Creative Commons 0.
  - https://web.archive.org/web/20241216014427/https://freesound.org/people/CogFireStudios/sounds/531512/
    shows the same title, uploader and upload date, licence Creative Commons 0.
  - These are the oldest captures the Wayback Machine holds for either sound.
- **E-F3 — the studio's record at download.** `docs/audio/CREDITS.md` (Verification, commit
  abfd626 and its parents) records that the licence was confirmed as CC0 on both sound pages at
  audition time, 2026-08-28. The masters were downloaded for the two swaps made that day.
- **E-F4 — licence at upload time.** This cannot be established from public records.
  freesound does not publish a sound's licence history, and no capture exists from 2020. It
  does not affect the verdict, for three reasons:
  1. freesound's own position (core developer alastairp, https://freesound.org/forum/freesound-project/42067/)
     is that uploaders may change a sound's licence. A person who downloaded the sound is
     governed by "the license that it had at that time", meaning the time of download. That
     licence was CC0 (E-F3).
  2. CC0 1.0 is irrevocable. The legal code says the affirmer "irrevocably and unconditionally
     waives, abandons, and surrenders" its copyright and related rights (§2). Its fallback
     licence is "irrevocable and unconditional" (§3).
  3. Every observation we have shows CC0: 2023-12-15, 2024-12-16, 2026-08-28 and 2026-09-29.
     None shows any other licence.

  The downloading account's attribution list, https://freesound.org/home/attribution/, records
  the licence each sound carried at download (per the same forum post). It is a stronger record
  than E-F3 if one is ever wanted. It is optional and not needed for this verdict.
- **E-F5 — the renamed account and the link in 506546's description.** The account was renamed.
  https://freesound.org/people/MATRIXXX_/ redirects (HTTP 301) to `/people/LilMati/`, so
  MATRIXXX_ and LilMati are one account. The description of 506546 links to 506545, and that
  sound links back to 506546. 506545 is "Pop 02", uploaded by the same account on the same day
  and licensed CC0. The link is a cross-reference between two sibling uploads, not a declared
  source. Neither 506546 nor 531512 declares any freesound source sound (no sources section,
  `data-remix-group="false"` on both pages).
- **E-F6 — metadata embedded in the masters.** The tags were carried into the committed
  single-encode `.ogg` files (3f2af90) and read with `ffprobe`. `sfx_select.ogg` carries
  `artist=MATTIAS "MATTIX" LAHOUD`, `title=CARTOON POP 01` and `date=2020`. That title is the
  "Cartoon_Pop_01" name the uploader's own cross-link uses (E-F5), and the uploader signs as
  "MATRIXXX a.k.a. Lil Mati". `sfx_star_pop.ogg` carries `encoded_by=REAPER` and
  `date=2020-08-20`, which is the day the sound was uploaded. Both fit each uploader having
  made the sound themselves. Nothing in either file names a third party.
- **E-D — the derivative (row 10).** `sfx_deselect` is made from the 506546 master by an
  in-repo transformation: trim, reverse, a whole tone lower, and gain (chain in CREDITS.md). CC0
  allows adaptation with no condition: no share-alike, no notice, no attribution. So the
  derivative carries no licence obligation, and it contains no material except the parent and
  our own processing. There is no separate freesound page to check. The parent's evidence
  applies, and the THIRD_PARTY row cites the parent's URL.
- **Residual risk (CC0 §4(c)).** CC0 carries no warranty that the uploader held the rights.
  freesound requires uploaders not to upload material they do not have the rights to (FAQ,
  https://freesound.org/help/faq/). Neither sound declares a source. The embedded tags fit
  each uploader being the author (E-F6). 506546 is tagged as a mouth pop, and 531512 sits in
  its author's own pack of designed beeps. This is the ordinary
  risk of any freesound CC0 asset. It is low and needs no action.

### Provenance (E-P)

The shipped files were checked against the sources they claim, so each verdict applies to the
right source. Method: decode each file to mono 11,025 Hz with ffmpeg 9.0.2, trim samples below
−50 dBFS from the start, and take the peak normalised cross-correlation within ±25 ms. The
check ran twice: on the `.wav` files as committed in d1c6e9b (the numbers in the table and
below), and on the `.ogg` files they replace, as committed on `develop` 3f2af90. The two runs
agree to within 0.011. The Kenney sources were taken from the pinned archive. The freesound
sounds were compared against their public HQ previews
(`https://cdn.freesound.org/previews/506/506546_6142149-hq.mp3`,
`https://cdn.freesound.org/previews/531/531512_7614679-hq.mp3`), because the originals need a
login.

- Each Kenney-sourced file matched its claimed source at 0.927 to 1.000. Its best match among
  the other eight pack files used in the game was at most 0.683: `sfx_jump` against `drop_002`
  and `sfx_peg_remove` against `drop_003`, two sibling drops that the audio spec already notes
  are nearly the same sound. `sfx_win` scores lowest, at 0.927, and its best match among the
  other files is 0.074.
- `sfx_select` scored 0.975 against 506546 and 0.087 against 531512. `sfx_star_pop` scored
  0.998 against 531512 and 0.089 against 506546. `sfx_deselect` scored 0.996 against 506546
  after undoing the reversal and the pitch shift, and 0.027 without undoing them.

So every shipped file comes from the source its row names. For the record, the sha256 values
of the files checked, as committed in d1c6e9b, are below. A later re-render (for example a
level change) changes these hashes but does not change the verdict, as long as the render
comes from the same pinned source. Re-run this check if the source changes.

| File (d1c6e9b) | sha256 |
|---|---|
| `sfx_jump.wav` | `830661a553299e102cdf045739d91079c2aed0b604bf756b1e8c993d557d4993` |
| `sfx_peg_remove.wav` | `1a3e31d59853c77b584b59e536eb81ccf2d364ce7b0700ce462de0202209530c` |
| `sfx_undo.wav` | `953208209159b712e4b4249ac8d8ffa9d1c7603638a37677c7cd8eb166225b04` |
| `sfx_win.wav` | `6955beab73b662dfdbb3eb9c15aa3dbf1cd0cd445e068a55d1b6172c756da223` |
| `sfx_btn_forward.wav` | `2b03e4da0042c0c70d9a820da610043b71f8c5cd2ef2a0ca7a3860ce12d95e36` |
| `sfx_btn_dismiss.wav` | `fc63ab48c36cfb38411dacf621a03104b6c6b70a64f6021313bb4f92496a26ad` |
| `sfx_sound_enabled.wav` | `fc2a03331d67a172bce7527fa55fa86d00f27abfd6d07a0ead2e43ec8a976c69` |
| `sfx_star_pop.wav` | `22637d42c4b42372d384e77f238a968ef11d5fa8d311deb100654c654a2362ae` |
| `sfx_select.wav` | `f2301135f4c1657f7671781749bf86deeb92e74002b8204655fb626e9ead7e01` |
| `sfx_deselect.wav` | `264e7716247b5b371c17091efbc5e6b5ba5ec69dbc207ea7e85d5f593d189dcc` |

## In-house cues (no verdict needed)

| Shipped file | Made from | Third-party material | Verdict |
|---|---|---|---|
| `sfx_invalid.wav` | recipe `audio/recipes/invalid.json` ("soft tok", Q-008, af2b554), rendered with `sfx-synth.sh` | none: one triangle oscillator and one layer of seeded noise | own work, not a borrowed asset (`rules/audio.md`, In-house) |
| `sfx_hint.wav` | recipe `audio/recipes/hint.json` ("soft glint", Q-008, af2b554), rendered with `sfx-synth.sh` | none: two sine oscillators and one triangle oscillator | own work, not a borrowed asset |

The Kenney files these two replace, `bong_001.ogg` and `pluck_002.ogg`, stop shipping. Both are
CC0 from the same pack, so no clean-up obligation follows from dropping them. Both committed
recipes are identical to the candidates the founder auditioned. Rendering each one again with
`sfx-synth.sh` gives output byte-identical to the committed `.wav` (checked 2026-09-29, `cmp`).
So each shipped in-house file is exactly the synthesiser's output and nothing else.

## Trademark and name risk: none

No source's title, logo, character or distinctive identity appears in the product. The source
names "Kenney", "freesound", "CogFireStudios" and "LilMati" appear only in credit lines, in
`docs/audio/CREDITS.md` and `THIRD_PARTY.md`. The sound titles "Pop 01" and "Positive Blip
Effect" are not shown anywhere in the app. Crediting by name is what Kenney's support page
invites ("you can do so by mentioning 'Kenney'"). The Kenney logo, which Kenney reserves, is not
used. CC0 §4(a) leaves the affirmer's trademark rights in place, but that only matters when a
mark is used as a mark, and none is used that way here. A general web search surfaced no
registration for "Kenney". No trademark register was queried, and none needs to be, because
the name is used only to give credit.

**No trademark or name risk, and no escalation.**

## Pins

### Kenney *Interface Sounds* 1.0 archive

| Field | Value |
|---|---|
| Version | 1.0 (`License.txt`; the page says "1.0 Released in 2020"; creation date 11-02-2020, and the zip entry for `License.txt` is dated 2020-02-11) |
| Asset page | https://kenney.nl/assets/interface-sounds |
| Download URL (2026-09-29) | https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip |
| Response | HTTP 200, `content-type: application/zip`, `content-length: 834536`, `last-modified: Fri, 15 May 2026 22:51:54 GMT`, `etag: "6a07a38a-cbbe8"` |
| Archive sha256 | `f2193d072726d6758a5f7871b2dcc54dcce0d5c35c6f0a62f92549b327c81232` (834,536 B) |
| Listing (`unzip -l`, before extraction) | 104 entries: `License.txt` (574 B), `Kenney.url` (45 B), `Patreon.url` (55 B), the `Audio/` directory, and 100 `Audio/*.ogg`. No executable, script or nested archive. This is the listing CREDITS.md recorded in phase 07. |

Per-file pins. These are the durable pins. Kenney's download path embeds an upload token
(`fa43c1dd4d-1677589452`), so a re-upload of the pack can change the URL and the archive hash
while the files stay the same.

| Archive entry | Size | CRC-32 (zip) | sha256 | Used by |
|---|---|---|---|---|
| `License.txt` | 574 | `ade6ad32` | `f7966c773bbed0eca6a9c75081c44a178b38eae112724dbb5fdfbd4192d118a9` | the licence |
| `Audio/drop_003.ogg` | 6,288 | `91273942` | `43d086d50f90493a98cd2e09aa6dbf150d164f1578e1ec768d21cb185571ad87` | `sfx_jump.wav` |
| `Audio/drop_002.ogg` | 6,411 | `45ddd7f4` | `4ac4d1cef7e936965cbf795852ca2020300b9e2ba7daa59f2bf4f1f7bf416218` | `sfx_peg_remove.wav` |
| `Audio/minimize_008.ogg` | 9,298 | `50233c33` | `bc14c17875436dcaaf809c25db5e61454dbc35612880f329ff37a95ef9b6a865` | `sfx_undo.wav` |
| `Audio/confirmation_002.ogg` | 14,169 | `71815aee` | `33b17a9a9a2397c62b285c52c33a907fdffb476909c99e42dde603f6a7a8b12c` | `sfx_win.wav` |
| `Audio/click_001.ogg` | 4,876 | `83c52c9b` | `ccfb7fa0cccdd9faec0eb16033c732b1e308d139d80f799161495d58f7adcdb9` | `sfx_btn_forward.wav` |
| `Audio/back_004.ogg` | 6,923 | `21c11b04` | `c22e3c49f42c2a5ba6748e91283efe124f32822469c1258c3e5d7b3d7ab9361c` | `sfx_btn_dismiss.wav` |
| `Audio/confirmation_001.ogg` | 8,968 | `7c18d5a5` | `063564703b6094d70718a3e787a55cc9141611e4ecd6b6637f8828f79b4a8c3a` | `sfx_sound_enabled.wav` |

### freesound chain

The masters sit behind a freesound login and were not downloaded again, either by this task or
for the WAV conversion. The chain is pinned at every step:

| Step | `sfx_star_pop` (531512) | `sfx_select` (506546) | `sfx_deselect` (from 506546) |
|---|---|---|---|
| Master, pinned in CREDITS.md (2026-08-28) | md5 `02c2514f4b3953424eda3181aec8249b`, 866,074 B, pcm_s24le, 2 ch, 48 kHz, 3.004646 s | md5 `2e5ddefd4d3741ad4221245950904ded`, 17,706 B, pcm_s16le, 2 ch, 44.1 kHz, 0.098526 s | the 506546 master |
| Intermediate `.ogg` at `develop` 3f2af90 (git blob / sha256) | `0cfc6379…` / `8b74db456822b6b6bec1821ec9a907b7a1a2d40b1504c8c50e27336e4d193c2a` | `fd5d5838…` / `3f967b4ca8dcadcbe64917a65ca276e5069afb39a3cdac8395726234a0eb0738` | `068f35f4…` / `071c3cbee4403be13e26dab9152577e6f7766990e50871ba74d1b9e50c5818a7` |
| Shipped `.wav` (d1c6e9b) | see the E-P table | see the E-P table | see the E-P table |

The freesound pages' stated specs still match the master pins (E-F1). If anyone downloads a
master again, they should check its md5 against the pin, stop if it differs, and record its
sha256 next to the md5 in CREDITS.md.

### Re-verifying at a release

    curl -sSL --proto '=https' --tlsv1.2 -o kenney_interface-sounds.zip \
      https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip
    shasum -a 256 kenney_interface-sounds.zip          # expect f2193d07…81232
    unzip -l kenney_interface-sounds.zip               # expect 104 entries, nothing executable
    unzip -q kenney_interface-sounds.zip License.txt 'Audio/*' -d k && \
      (cd k && shasum -a 256 License.txt Audio/drop_003.ogg Audio/drop_002.ogg \
        Audio/minimize_008.ogg Audio/confirmation_002.ogg Audio/click_001.ogg \
        Audio/back_004.ogg Audio/confirmation_001.ogg)   # expect the table above
    curl -sSL https://freesound.org/people/CogFireStudios/sounds/531512/ | grep -o 'publicdomain/zero/1.0/'
    curl -sSL https://freesound.org/people/LilMati/sounds/506546/ | grep -o 'publicdomain/zero/1.0/'

If the Kenney URL has moved, take the new link from the asset page's Download button, then
compare the per-file hashes. A new archive hash alone is not a finding. A changed hash for a
source file, or a changed licence text, is a finding: raise it to the research-analyst before
the release.
