# research-analyst report — audio-review (per-file licence verdicts)
Date: 2026-09-29
Branch: feature/audio-review (10f7691..this commit; one commit)
Skills loaded: ergoz-soft:standards (role: research-analyst). I read these rule files from the
v0.6.2 snapshot, which matches plugin 0.6.2: studio.md, reporting.md, gitflow.md,
skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md,
compose.md, ip-policy.md and audio.md. I also loaded research-open-source,
research-competitive and market-research, as the agent file requires. Their intake questions
and `artifacts/research/` outputs were not used: only the producer asks the founder, and this
dispatch names the outputs. I applied their standards: every claim sourced, and facts kept
separate from inferences.
Ran in parallel with: the rest of the audio-review follow-up, in the same worktree. The
sound-designer committed af2b554, d1c6e9b, d129613 and 10f7691, the tech-lead 825dd2a, and the
ux-designer 9abac3c and 35eb4bc.
Ran on: opus (claude-opus-5-5) at effort xhigh. This is a **staffing deviation**, explained
below.
Fan-out: none
Loops: 0
Lessons applied: none. The v0.6.2 snapshot has 0 active lessons for game /
android-compose-game, and `.planning/recall/RECALL.md` does not exist.

## Staffing deviation
`agents/research-analyst.md` pins `model: fable`, `effort: xhigh`. Fable is exhausted. The
founder answered Q-011 in `.planning/questions/FOUNDER.md` on 2026-09-29: "2 — run them on
Opus". That exception covers only the four audio follow-up steps, this one included, and is
not a standing policy change. No product code was written. The verdict document and this
report were written on Opus. Under studio.md, any check of them by a fresh agent should run on
Fable once Fable is available.

## Summary
All ten sourced files that stay in the game are **CLEAN** under the `ip-policy.md` CC0 row:
seven from Kenney *Interface Sounds* 1.0, `sfx_star_pop` (freesound 531512), `sfx_select`
(freesound 506546), and `sfx_deselect`, a derivative of 506546. None is ESCALATE or NO. The
Kenney pack is pinned by archive sha256 and by the sha256 of each file used. The freesound
chain is pinned from each master's md5 through the committed intermediate `.ogg` files. Every
shipped `.wav` was matched to its claimed source by cross-correlation. There is no trademark or
name risk. The wrong-tap and hint cues are in-house renders that contain only synthesiser
output, so they need no third-party verdict.

## Outputs
- `docs/audio/licence-verdicts.md` — one row per shipped file (file, source URL, source file,
  author, licence, evidence with dates, pin, verdict). It also holds the evidence, the
  provenance check, the in-house confirmation, the trademark finding, the Kenney archive pin
  and per-file pins, the freesound chain, and the commands to re-verify at a release.
- `.planning/reports/audio-review/research-analyst.md` — this report, including the
  THIRD_PARTY.md delta below.
- Local only, not committed (scratchpad): the downloaded Kenney archive, the page captures,
  `xcorr.py` and its output.

## Findings in brief
1. **Reading of the rules for the in-house cues: confirmed.** `rules/audio.md` requires a
   THIRD_PARTY row and a research-analyst verdict only for **sourced** assets, and
   `ip-policy.md` requires a row per **borrowed** asset. `sfx_invalid.wav` and `sfx_hint.wav`
   are rendered from `audio/recipes/invalid.json` and `audio/recipes/hint.json` (af2b554).
   Those recipes are identical to the auditioned candidates. `sfx-synth.sh` only generates
   oscillators and seeded noise, with no sample input and no network access. Re-rendering both
   recipes gives output byte-identical to the committed files, so they need no verdict. They
   are listed in THIRD_PARTY as own work, like the vector icons.
2. **Kenney *Interface Sounds* 1.0.** The live page says "Creative Commons CC0". The archive's
   `License.txt` says CC0, word for word as CREDITS.md quoted it. The oldest Wayback capture
   (2020-06-22) says "CC0 1.0 Universal". **Pin:** version 1.0, download URL
   `https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip`,
   archive sha256 `f2193d072726d6758a5f7871b2dcc54dcce0d5c35c6f0a62f92549b327c81232`
   (834,536 B). The document also gives the sha256 of `License.txt` and of each of the seven
   source files used (drop_003, drop_002, minimize_008, confirmation_002, click_001, back_004,
   confirmation_001). The per-file hashes are the durable pin, because the download path
   embeds an upload token.
3. **freesound 531512 (CogFireStudios) and 506546 (LilMati).** Both live pages show CC0 1.0 as
   of 2026-09-29. The oldest archive captures, 2024-12-16 and 2023-12-15, also show CC0. The
   studio recorded CC0 at download on 2026-08-28.
   - **The licence at upload time (2020) cannot be established.** freesound publishes no
     licence history, and no capture exists from 2020. This is immaterial. By freesound's own
     policy, the licence at download governs, and that was CC0. CC0 is also irrevocable (legal
     code §2 and §3).
   - One apparent red flag was checked and resolved. The description of 506546 links to a
     "MATRIXXX_" sound. MATRIXXX_ is LilMati's former name for the same account (freesound
     redirects the old name with HTTP 301), and the linked sound, 506545, is a CC0 sibling
     upload, not a source.
   - **The derivative `sfx_deselect`** is CLEAN. CC0 places no condition on adaptation, and the
     only transforms are our own.
4. **Provenance.** Each shipped `.wav` (d1c6e9b) correlates 0.927–1.000 with its claimed Kenney
   file, and at most 0.683 with any other pack file used in the game. The three freesound files
   score 0.975–0.998 against their sources' public previews, with `sfx_deselect` scored after
   undoing the reversal and the pitch shift. Their scores against the other freesound sound
   are ≤ 0.089. The masters' embedded tags (artist and title for 506546; REAPER and the upload
   date for 531512) fit each uploader being the author.
5. **Trademark or name risk: none.** No title, logo, character or distinctive identity from any
   source ships. Names appear only in credit lines, which Kenney's support page explicitly
   invites. The reserved Kenney logo is not used.

## Checks
- `curl -sSL --proto '=https' --tlsv1.2 https://kenney.nl/assets/interface-sounds` — pass (HTTP 200; "License: Creative Commons CC0", "1.0 Released in 2020")
- `curl … https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip` then `shasum -a 256` — pass (HTTP 200, 834,536 B, sha256 `f2193d07…81232`)
- `unzip -l kenney_interface-sounds.zip` before extraction — pass (104 entries: License.txt, 2 `.url`, 100 `Audio/*.ogg`; nothing executable)
- `unzip -q … License.txt Audio/<9 files>` then `shasum -a 256` and `cat License.txt` — pass (licence text is CC0 and matches CREDITS.md word for word; per-file hashes recorded)
- `curl … https://kenney.nl/support` — pass (CC0; credit by the name "Kenney"; the logo is reserved)
- `curl … https://freesound.org/people/{LilMati/sounds/506546,CogFireStudios/sounds/531512}/` and grep for `publicdomain/zero/1.0/` — pass (both "Creative Commons 0"; page specs match the CREDITS.md master pins)
- `curl -sSI https://freesound.org/people/MATRIXXX_/` — pass (301 to `/people/LilMati/`); `curl … /people/MATRIXXX_/sounds/506545/` — pass (sibling "Pop 02", CC0)
- `curl https://web.archive.org/cdx/search/cdx?url=…` and the `id_` captures 20200622144310 (Kenney), 20231215104553 (506546), 20241216014427 (531512) — pass (all show CC0)
- `curl https://creativecommons.org/publicdomain/zero/1.0/legalcode.txt` and grep — pass (irrevocable waiver §2, irrevocable fallback licence §3, §4(a) trademark, §4(c) rights of others)
- `python3 xcorr.py < pairs-wav.txt` (ffmpeg 9.0.2 decode, 11,025 Hz, ±25 ms) on the d1c6e9b `.wav` files and on the 3f2af90 `.ogg` files — pass (claimed sources 0.927–1.000; the best non-claimed match is 0.683; the two runs agree within 0.011)
- `sfx-synth.sh audio/recipes/{invalid,hint}.json` from d1c6e9b, then `cmp` against the committed `.wav` — pass (both byte-identical)
- `diff` of the committed recipes against `.planning/qa/audio-review/candidates/{invalid_a,hint_a}.json` — pass (identical)
- `ffprobe` on the ten committed sourced `.wav` files — pass (pcm_s16le, 1 ch, 44.1 kHz, 16-bit)
- `ffprobe -show_entries format_tags:stream_tags` on the 3f2af90 `sfx_select.ogg` and `sfx_star_pop.ogg` — pass (`artist=MATTIAS "MATTIX" LAHOUD`, `title=CARTOON POP 01`; REAPER, 2020-08-20; no third party named)
- `shasum -a 256` on the d1c6e9b `.wav` files, compared with the sha256 column of CREDITS.md at 10f7691 — pass (all ten sourced files match)
- `git diff --check` — pass
- `./gradlew --offline check` — **fail** (BUILD FAILED in 11m 19s, 2 failures), caused by
  something other than this change:
  1. `:app:testDebugUnitTest`: `AudioCreditsTest` failed twice
     (`everyCommittedSoundHasACreditsRow`, `everyCreditsRowNamesASourceAndLicence`; both
     "expected not to be empty" at `AudioCreditsTest.kt:35` and `:44`). The test scans
     `res/raw` for `AUDIO_EXTENSION = "ogg"`, and since the sound-designer's d1c6e9b no `.ogg`
     files remain. The other 943 of 945 tests passed.
  2. `:domain:depthSweep`: `java.io.EOFException`. The sweep's own report printed all five
     boards. The failure came afterwards, from Gradle's test-result store (`KryoException:
     Buffer underflow` in `SerializableTestResultStore.hasResults`; `results-generic.bin` was
     0 bytes). Another role's `./gradlew check` was running in this same worktree at the same
     time (`./gradlew --status`: a second BUSY daemon; `ps`: a `gradlew check` and a
     `-DRUN_SWEEP=true` worker). Two builds were sharing `domain/build/`. Re-running
     `./gradlew --offline :domain:depthSweep` alone while that build was still going failed in
     1 s with the same EOF before any test ran. I deleted nothing and did not re-run the check.

  This change is docs only (a new `docs/audio/` file and this report). No test and no Gradle
  input reads either file. The `AudioCreditsTest` update is already routed to android-game-dev
  in the sound-designer's report ("Adopting a candidate or converting to WAV needs a test
  change"). The depth sweep should be re-run once no other build is using the worktree.

## THIRD_PARTY.md delta (for the release-manager)
Base: `feature/phase-14-release-docs:THIRD_PARTY.md` (last changed in e5253f4; branch tip
d9f9a60). Apply it when both branches are on the same line, that is, when this audio branch and
the release-docs branch meet on develop, so that the rows name the files that actually ship.
Nothing here edits THIRD_PARTY.md on this branch.

**1. Replace §2 lines 70–72** (the paragraph under "## 2. Assets shipped in the artifact"):

```markdown
The only binary assets in the app are twelve sound files in `app/src/main/res/raw/`, all mono
16-bit 44.1 kHz WAV: ten sourced under CC0 (seven from Kenney *Interface Sounds* 1.0, three from
two freesound sounds) and two rendered in-house from committed recipes. There is **no raster
image anywhere in `app/src/main/res`** — every drawable and the launcher icon are vector XML
authored in this repo (`app/src/main/res/drawable/`, `mipmap-anydpi-v26/`).
```

**2. Replace §2 rows 76–79** (the four audio rows; the table header stays) with these twelve
rows. They are in cue order. Rows 80–82 (icons, opening books, fonts) are unchanged.

```markdown
| `sfx_jump.wav` | Kenney, *Interface Sounds* 1.0, `Audio/drop_003.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | Asset page and the pack's `License.txt`; archive and source-file sha256 pinned; research-analyst verdict **CLEAN** (`docs/audio/licence-verdicts.md` row 1, 2026-09-29) | `docs/audio/CREDITS.md` |
| `sfx_peg_remove.wav` | Kenney, *Interface Sounds* 1.0, `Audio/drop_002.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | same; verdict **CLEAN** (row 2) | same |
| `sfx_select.wav` | freesound sound 506546 "Pop 01" by LilMati — https://freesound.org/people/LilMati/sounds/506546/ | **CC0 1.0** | Licence on the sound's page (live and archived); master md5 pinned; verdict **CLEAN** (row 9) | same |
| `sfx_deselect.wav` | Derived in-repo from freesound sound 506546 (reversed, a whole tone lower) — https://freesound.org/people/LilMati/sounds/506546/ | **CC0 1.0** (derivative of a CC0 work) | Parent's licence; verdict **CLEAN** (row 10) | same |
| `sfx_undo.wav` | Kenney, *Interface Sounds* 1.0, `Audio/minimize_008.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 3) | same |
| `sfx_invalid.wav` | Rendered in-house from `audio/recipes/invalid.json` with the studio's `sfx-synth.sh` (oscillators and seeded noise; no sampled material) | Own work | Not a borrowed asset, no verdict needed (`docs/audio/licence-verdicts.md`, In-house cues) | — |
| `sfx_hint.wav` | Rendered in-house from `audio/recipes/hint.json` with the studio's `sfx-synth.sh` (oscillators only; no sampled material) | Own work | same | — |
| `sfx_win.wav` | Kenney, *Interface Sounds* 1.0, `Audio/confirmation_002.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 4) | `docs/audio/CREDITS.md` |
| `sfx_star_pop.wav` | freesound sound 531512 "Positive Blip Effect" by CogFireStudios — https://freesound.org/people/CogFireStudios/sounds/531512/ | **CC0 1.0** | Licence on the sound's page (live and archived); master md5 pinned; verdict **CLEAN** (row 8) | same |
| `sfx_btn_forward.wav` | Kenney, *Interface Sounds* 1.0, `Audio/click_001.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 5) | same |
| `sfx_btn_dismiss.wav` | Kenney, *Interface Sounds* 1.0, `Audio/back_004.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 6) | same |
| `sfx_sound_enabled.wav` | Kenney, *Interface Sounds* 1.0, `Audio/confirmation_001.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 7) | same |
```

**3. Replace the §2 paragraph at lines 84–86** ("CC0 waives copyright … two copies of an
asset table drift."):

```markdown
CC0 waives copyright and imposes no attribution condition, so the credits above are a courtesy and
an in-repo provenance record. The audio rows are per file because `rules/ip-policy.md` asks for
one row per borrowed asset and `rules/audio.md` asks every sourced sound's row to carry the
research-analyst's verdict. `docs/audio/CREDITS.md` stays the per-file processing record, and
`docs/audio/licence-verdicts.md` holds the verdict evidence, the archive and file pins, and the
commands that re-verify them at a release.
```

**4. §6 (Unverified): nothing to clear.** §6 has no audio row. U-1 to U-6 cover UMP, the store
artwork and the classpath enumeration, and this task does not touch them. No audio item is
unverified.

**5. Optional, §7 lines 195–196:** "Every shipped asset is CC0 or OFL" becomes "Every shipped
asset is CC0, OFL or own work". The in-house cues, like the vector icons already listed, are own
work. The conclusion that nothing shipped carries an attribution obligation still holds.

## Open issues / risks
- **`./gradlew check` is red on this branch since d1c6e9b.** `AudioCreditsTest` scans `.ogg`
  only. The fix is android-game-dev's: scan `.wav`, and add a rule for in-house recipe rows,
  because the two in-house rows cannot carry an `https://` source. The branch must not merge
  until that fix lands.
- **Several roles are running Gradle in one worktree at once.** In this run that corrupted
  `:domain:depthSweep`'s result store. The producer should serialise Check runs in
  `PegSolitarie-audio`, or give each role its own worktree, and then re-run the depth sweep.
- **The freesound masters are pinned by md5 only.** They were not re-downloaded: the WAVs were
  decoded from the committed single-encode `.ogg` files, which are pinned by git blob id and
  sha256 in the verdict document. If anyone downloads a master again, they should check it
  against the md5 and record its sha256 in CREDITS.md.
- **The licence at upload time is unestablishable** for the two freesound sounds, as the
  findings above explain. It does not affect the verdict. If a stronger record is ever wanted,
  the attribution list of the freesound account that downloaded the masters
  (`https://freesound.org/home/attribution/`) shows the licence at download. This is optional,
  and I am not asking a founder question about it.
- **The CC0 §4(c) residual:** the studio has no warranty that the uploaders held the rights.
  This is ordinary freesound risk, low, and needs no action.
- **CREDITS.md and the verdicts agree.** The sound-designer's 10f7691 added author and sha256
  columns to CREDITS.md. Its ten sourced WAV hashes equal the d1c6e9b hashes checked here, and
  its sources match the verdict rows. I did not edit CREDITS.md.
- **No in-app licences screen** remains open in THIRD_PARTY §7. It carries no legal obligation,
  because every sourced asset is CC0.

## Drafted founder questions
None. No source is copyleft, share-alike, non-commercial or of unknown licence, and there is
no live trademark to consider. Nothing needs escalating.

## Handoff
Next: release-manager — apply the THIRD_PARTY.md delta above (§2 replacements 1–3; §6
unchanged; the §7 wording is optional) once `feature/audio-review` and
`feature/phase-14-release-docs` meet on develop. Add `docs/audio/licence-verdicts.md` §"Re-verifying
at a release" to release-checklist step 6.
Next: android-game-dev — update `AudioCreditsTest` for `.wav` and for in-house recipe rows, so
the Check command goes green again (already routed by the sound-designer).
Next: sound-designer — nothing required. If a freesound master is ever downloaded again,
verify its md5 and record its sha256. If a sourced WAV is re-rendered, re-run the E-P check in
`docs/audio/licence-verdicts.md` against the pinned source.
Next: producer — no founder question. When Fable is available, have a fresh Fable agent check
this Opus-written verdict document.
