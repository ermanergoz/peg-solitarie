# Audio spec — Peg Solitaire

The cue list of record (rules/audio.md). Written 2026-09-29 by the sound-designer as a review of
the set that shipped in phase 07 and its two follow-ups (quick tasks 260828-vb3 and 260828-vrr).
It was updated the same day after the founder's answers to Q-008 and Q-009. **Assets,
Measurements, Mix and the File, Character and Length columns now describe the WAV set on
`feature/audio-review` at d1c6e9b. Code references, volumes and Settings still describe `develop`
at 3f2af90.** The tutorial rows describe `feature/phase-15-ux-refresh` at bffe558, which is not
merged yet. "Proposed changes" gives each item's status and who does what is left.

Per-file provenance stays in `docs/audio/CREDITS.md`. This file points at it rather than
repeating it.

## Character
Material: soft recorded "candy-clay" pops, plucks and blips (Kenney *Interface Sounds* plus two
freesound CC0 sounds). Two soft in-house synth cues match them: the wrong tap and the hint. There
is no music. Register: mid (dominant 270 Hz – 1.3 kHz). Length: at most 0.54 s, except the star
pop at 1.65 s. One word: **pop**.

## Cue table

Code references are `app/src/main/java/com/app/pegsolitaire/`. Volume is the `SoundCue` level
from `audio/SoundParams.kt`. Priority 1 = celebration (never stolen for a jump), 0 = everything
else. Character is measured, not guessed (see Measurements).

| Event (what fires it) | Cue | File | Character | Length | Priority | Volume | Haptic | Visual partner | Source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A peg completes a jump; +1 semitone per consecutive jump by the same peg, capped at 2.0× (`ui/game/GameScreen.kt:321-331`, keyed on `MoveTransition.id`) | `Jump` | `sfx_jump.wav` | rounded mid "drop" pop, steady ~730 Hz, instant attack, −20 dB after 40 ms | 188 ms | 0 | 0.9 | `ContextClick` when haptics are on (`GameScreen.kt:285-295`) | peg slides, landing squash | Kenney `drop_003`, CC0; re-levelled to a −1.10 dBFS peak (P4a) |
| The jumped peg is removed — same effect, 60 ms after the jump | `PegRemove` | `sfx_peg_remove.wav` | the same drop pop ~0.8 semitone higher, rising 800→925 Hz; **kept by the founder** (Q-008, see P2) | 188 ms | 0 | 0.45 | — (the jump's) | jumped peg fades and shrinks | Kenney `drop_002`, CC0 |
| A peg is picked up (`selectionTick`, `GameScreen.kt:346-350`) | `Select` | `sfx_select.wav` | short bright pop, ~1.46 kHz transient | 67 ms | 0 | 0.6 | none | selection ring, breathing pulse, destination dots pop in | freesound 506546 (LilMati), CC0 |
| The held peg is re-tapped to put it down (`deselectionTick`, `GameScreen.kt:362-366`) | `Deselect` | `sfx_deselect.wav` | the select pop reversed and a whole tone lower: a soft swell | 75 ms | 0 | 0.1897 (−10 dB under `Select`) | none | ring and dots disappear | derived from 506546, CC0 |
| Undo actually steps the board back (`undoTick`, `GameScreen.kt:378-382`) | `Undo` | `sfx_undo.wav` | low descending "whoop", 410→260 Hz | 207 ms | 0 | 0.7 | none | the last move reverses on the board | Kenney `minimize_008`, CC0 |
| A tap that changes nothing: an empty hole with nothing held, or another peg or a hole that is not a legal destination while a peg is held (W1–W3 in `docs/ux/invalid-tap-feedback.md` §1.1; an out-of-play cell never reaches the ViewModel) (`rejectedTapTick`, `GameScreen.kt:393-397`) | `InvalidTap` | `sfx_invalid.wav` | "soft tok": a triangle falling 420→360 Hz through a 1.2 kHz low-pass, with a 20 ms muffled noise tap. Its ~400 Hz fundamental survives a phone speaker (0.4 % of its energy is below 300 Hz) | 90 ms | 0 | 0.3 | none today; specified: the platform *reject* haptic, API 30+, under Settings › Haptics (D-06b) | **none today** — the board does not change (`GameUiState.kt:79-83`). Specified, not yet built: a head-shake of the tapped cell, a redirect while a peg is held, and a screen-reader announcement (D-06, `docs/ux/invalid-tap-feedback.md`, 9abac3c) | in-house, `audio/recipes/invalid.json` (Q-008) |
| A hint is revealed: the search resolved to a move (`hintSource` set, `GameScreen.kt:408-412`) | `Hint` | `sfx_hint.wav` | "soft glint": 1320 Hz with a fifth above (1980 Hz) and a faint 2640 Hz triangle, centroid 1.4 kHz, a 3 ms attack and a 200 ms decay | 220 ms | 0 | 0.6 | none | source glow, target ring, faint arrow | in-house, `audio/recipes/hint.json` (Q-008) |
| The winning move lands: status becomes `Won`, i.e. pegs ≤ the board's minimum (`GameScreen.kt:425-429`) | `Win` | `sfx_win.wav` | rising four-note arpeggio 1.18→4.7 kHz | 539 ms | 1 | 1.0 | none | final jump, then the Results entrance | Kenney `confirmation_002`, CC0 |
| Each earned star lands on Results, once per win (D-16) (`ui/results/ResultsCelebration.kt:99-114`, from `ResultsScreen.kt:225`) | `StarPop` ×1–3 | `sfx_star_pop.wav` | rising blip 775→1184 Hz with a long ring; played at rates 0.63 / 0.79 / 1.0 (a major sixth below, a major third below, unison) | 1650 ms (2.62 s at rate 0.63) | 1 | 0.85 | none | star pops in (lands instantly with animations off) | freesound 531512 (CogFireStudios), CC0 |
| A forward or commit button (D-09/D-11) — `withClickSound(ButtonForward, …)`: Home resume and mode tiles (`ui/home/HomeScreen.kt:447`, `:581`), a Classic board (`ui/classic/ClassicCatalogScreen.kt:143`), Results Next level / Retry / Replay / Home / Catalog (`ui/results/ResultsActions.kt:96`, `:123`, `:152`), dialog confirms (`GameScreen.kt:631` restart, `ui/game/AdOfferDialog.kt:197`, `ui/home/ProfileSwapDialog.kt:188` keep local, `ui/home/PlayGamesPromptDialog.kt:161`) | `ButtonForward` | `sfx_btn_forward.wav` | short neutral click, centroid 1.6 kHz; raised 4.1 dB (P4b) | 97 ms | 0 | 0.7 | none | `claySquish` press, then the screen or dialog changes | Kenney `click_001`, CC0 |
| A dismiss or cancel button (`GameScreen.kt:647` restart cancel, `AdOfferDialog.kt:237`, `ProfileSwapDialog.kt:209` use cloud — deliberately inverted, `PlayGamesPromptDialog.kt:182`) | `ButtonDismiss` | `sfx_btn_dismiss.wav` | darker click, 270/540 Hz, centroid 460 Hz | 94 ms | 0 | 0.7 | none | `claySquish` press, the dialog closes | Kenney `back_004`, CC0 |
| The player switches Sound **on** in Settings; user action only, bypasses the mute gate (D-12) (`ui/settings/SettingsViewModel.kt:136-137`) | `SoundEnabled` | `sfx_sound_enabled.wav` | rising arpeggio 390→1570 Hz: the win's shape, about 1.5 octaves lower | 290 ms | 0 | 0.6 | none | the switch moves to on | Kenney `confirmation_001`, CC0 |
| The player switches Sound **off** | none | — | silence is the confirmation (D-12) | — | — | — | none | the switch moves to off | — |
| Haptics and Show-moves switches in Settings; the Show-moves control on the board | none | — | toggles stay quiet (D-09) | — | — | — | none | the switch or control changes state | — |
| Restart control on the board (the confirm dialog's buttons sound, above) | none | — | assist control, not a navigation (D-09) | — | — | — | none | the confirm dialog opens | — |
| Back navigation and the top-bar icons (How to play, Settings) | none | — | D-09 | — | — | — | none | the screen changes | — |
| The board is stuck: no legal move and pegs above the minimum | none | — | deliberate: phase 07 left "lose/stuck stingers" out of scope | — | — | — | none | the rescue banner (`ui/game/NoWinBanner.kt`) | — |
| The hint search finds no winning move; the solver is thinking | none | — | — | — | — | — | none | no-win banner; spinner in the Hint control | — |
| Rescue-banner actions Undo / Restart / Home | `Undo` via `undoTick` when the board steps back; otherwise none | — | — | — | — | — | none | the board or screen changes | — |
| Score count-up, achievement pop-up, snackbars, cloud-save sync, a new Challenge level loading | none | — | — | — | — | — | none | each has its own visual | — |

### Tutorial and dock — phase 15 (on `feature/phase-15-ux-refresh`, not yet on develop)

From `docs/ux-spec.md` on that branch: D-01h first made the tutorial silent end to end; **D-01i
amended it the same day**, so a completed jump sounds and the two exit buttons sound. No new
asset and no new level.

| Event | Cue | Ruling | Visual partner |
| --- | --- | --- | --- |
| A tutorial peg completes a jump | `Jump` + `PegRemove`, through the shared `audio/JumpSoundEffect.kt` (same levels, same pitch ladder) | fires (D-01i, SOUND-01) | slide and removal fade; no haptic here, because `JumpHaptic` is private to `GameScreen` |
| A tutorial peg is picked up or put down | `Select`, `Deselect` | silent (D-01h, upheld) | ring, dots |
| A wrong tap in the tutorial | `InvalidTap` | silent (D-01c/D-01h): a rejection sound would make a wrong tap feel like failure | dots re-pop, coach ring |
| A hint in the tutorial | — | no `Hint` effect is hosted on the screen | — |
| The completion star | `Win`, `StarPop` | silent: a tutorial is not a victory | the star pops |
| "Got it" completion button | `ButtonForward` | fires (D-01i, D-11) | press, exit |
| Skip | `ButtonDismiss` | fires (D-01i) | press, exit |
| System back in the tutorial | — | silent | exit |
| Settings "How to play" row | `ButtonForward` | fires (§4.5) | press, navigation |
| Dock: Undo / Hint / Show-moves / Restart | `Undo` / `Hint` on reveal / none / none | unchanged (D-09) | the dock's state looks (§4.4.1) |

## Measurements

Measured on `feature/audio-review` at d1c6e9b with ffmpeg 9.0.2 on 2026-09-29. Every file is PCM
WAV, mono, 16-bit, 44.1 kHz. Sizes are under the 200 KB limit. The sha256 of each file is pinned
in `docs/audio/CREDITS.md`.

**L100** is K-weighted loudness (ITU-R BS.1770 weighting) over the loudest 100 ms window, in
LUFS. These cues are 67–540 ms long, shorter than the 400 ms EBU momentary window and far shorter
than `loudnorm`'s 3 s, so 100 ms is the window that ranks them the way a listener does. On the
cues shorter than 400 ms, the 400 ms figures agree with ffmpeg's `ebur128` to ±0.1 LU. **In game** adds the cue's
`SoundParams` volume as it is on develop, so `VOLUME_JUMP` is still 0.9. **Phone** is the same
measurement after a 400 Hz fourth-order high-pass,
used as a stand-in for a phone loudspeaker's bass roll-off. It is an estimate, not a device
measurement, and QA confirms it on a device. **Was** is the phone figure of the OGG each file
replaced (develop 3f2af90).

| Cue | Duration | Size | Sample peak dBFS | True peak dBTP | RMS dB | L100 on disk | L100 in game | L100 in game, phone | Was, phone | Energy < 500 Hz |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `sfx_win` | 0.539 s | 47,580 B | −1.58 | −1.6 | −15.5 | −9.6 | −9.6 | −9.7 | −9.7 | 0.1 % |
| `sfx_star_pop` | 1.650 s | 145,574 B | −3.34 | −3.3 | −22.5 | −11.9 | −13.3 | −13.5 | −13.5 | 0.1 % |
| `sfx_jump` | 0.188 s | 16,642 B | −1.10 | −1.1 | −19.6 | −17.5 | −18.4 | −19.3 (−18.4 at `VOLUME_JUMP` 1.0) | −18.2 | 7 % |
| `sfx_sound_enabled` | 0.290 s | 25,604 B | −4.96 | −5.0 | −15.3 | −12.6 | −17.0 | −22.0 | −22.1 | 62 % |
| `sfx_hint` | 0.220 s | 19,448 B | −12.50 | −12.5 | −23.1 | −19.1 | −23.6 | −23.6 | −23.8 | 0.0 % |
| `sfx_undo` | 0.207 s | 18,302 B | −4.94 | −4.9 | −16.4 | −15.0 | −18.1 | −26.0 | −26.0 | 100 % |
| `sfx_peg_remove` | 0.188 s | 16,642 B | −5.96 | −6.0 | −24.7 | −22.5 | −29.4 | −30.3 | −30.3 | 7 % |
| `sfx_select` | 0.067 s | 5,962 B | −5.48 | −5.3 | −25.3 | −26.7 | −31.2 | −32.2 | −32.2 | 22 % |
| `sfx_invalid` | 0.090 s | 7,982 B | −5.50 | −5.5 | −14.9 | −16.1 | −26.6 | −32.2 | **−59.4** | 99 % (0.4 % below 300 Hz) |
| `sfx_btn_forward` | 0.097 s | 8,610 B | −1.00 | −0.9 | −26.1 | −25.6 | −28.7 | −30.0 | −34.2 | 38 % |
| `sfx_btn_dismiss` | 0.094 s | 8,360 B | −8.09 | −8.1 | −24.9 | −25.9 | −29.0 | −33.4 | −33.7 | 67 % |
| `sfx_deselect` | 0.075 s | 6,688 B | −5.46 | −5.1 | −24.5 | −25.7 | −40.2 | −41.5 | −41.5 | 28 % |

**Loudness spread.** On disk, L100 runs from −26.7 to −9.6 LUFS, a range of **17 LU** (20 LU
with the OGGs). In game, after the volume multipliers, it runs from −40.2 to −9.6, a range of
**31 LU**. That is unchanged, because `Deselect` sets the floor and its level is P4c; without it the range is
22 LU. Through the phone stand-in it runs from −41.5 to −9.7, a range of **32 LU** (was
**50 LU**), or 24 LU without `Deselect`. The drop from 50 comes from the wrong tap: it rose
27 LU on the phone and is no longer the floor. Sample peaks span −12.5 to −1.0 dBFS, and every file meets the −1 dBFS
rule (the OGG jump peaked at −0.02). There is still no single house level. The set is a per-cue
balance that the founder tuned by ear. The conversion kept it to within 0.1 LU at full range,
apart from the two re-levels (P4a, P4b) and the two new cues. On the phone stand-in, dismiss reads
0.3 LU louder, because the WAV keeps treble that the old second encode cut.

**Distinctness of neighbours** (similarity of third-octave spectra / of 5 ms envelopes, 1.0 =
identical):

| Pair | Spectrum | Envelope | Reading |
| --- | --- | --- | --- |
| `Jump` / `PegRemove` | **0.997** | **1.000** | Not distinct, and **kept by the founder** (Q-008). The two files are the same drop sound about 0.8 semitone apart (waveform correlation 0.87 once that shift is removed). On the second jump of a same-peg run, the jump climbs a semitone and lands on the removal's pitch. See P2 for SOUND-02. |
| `Select` / `Deselect` | 0.957 | 0.088 | Distinct by design: the same material, reversed and a whole tone lower. The founder tuned this over two rounds (CREDITS.md). |
| `InvalidTap` / `Deselect` | 0.442 | 0.186 | Distinct. Both follow a tap on the board, and the soft tok is a dull thud where the put-down is a swell. |
| `ButtonForward` / `ButtonDismiss` | 0.850 | 0.819 | Distinct in timbre: dismiss is darker (centroid 460 Hz against 1.6 kHz). Forward is now the louder of the two, by 0.3 LU at full range and 3.4 LU on the phone stand-in, as `SoundCue.kt`'s "dismiss is softer" intends (P4b). |
| `Hint` / `StarPop` | 0.591 | 0.921 | Similar bright decays, but they play on different screens. |
| `Win` / `SoundEnabled` | 0.171 | 0.946 | The same arpeggio shape in two registers. They sound like one family and are not confusable. |
| `Select` / `ButtonForward` | 0.898 | 0.917 | Similar short clicks, but they play on different screens. |

## Mix

Peak: at most −1 dBFS per asset and for the mix (rules/audio.md). Every asset now passes:
`sfx_jump` peaks at −1.10 dBFS (it was −0.02). The jump + removal layer peaks at −2.02 dBFS on
every rung of the ladder at today's `VOLUME_JUMP` 0.9. It will peak at −1.10 dBFS once the
android-game-dev sets the constant to 1.0 (P4a); before the conversion it peaked at −0.93 and
failed. The worst celebration overlap (the win, then three star pops that ring together from
1.25 s to 2.9 s) peaks at −1.58 dBFS and passes, unchanged.

Levels below are L100 in game through the phone stand-in, which is how most players hear the
game ("Now" is the WAV set at today's volumes). The targets keep every cue within 24 LU of the
win, so nothing drops below −34. Every cue meets that except `Deselect`.

| Category | Cues | Now | Target | Notes |
| --- | --- | --- | --- | --- |
| success | `Win` | −9.7 | −10 | the anchor; unchanged |
| success | `StarPop` | −13.5 | −13 to −14 | unchanged |
| move | `Jump` | −19.3 | −18 | asset done at a −1.10 dBFS peak; `VOLUME_JUMP` 0.9 → 1.0 (android-game-dev) brings it to −18.4 (P4a) |
| move | `PegRemove` | −30.3 | −30 | kept by the founder (Q-008); the P2 target of −26 was for a replacement sound |
| ui (board) | `Select` | −32.2 | −30 to −32 | unchanged |
| ui (board) | `Deselect` | −41.5 | −36 to −38 | at the edge of audibility on a speaker; the founder tuned it; QA checks it on a device first (P4c, open) |
| ui (assist) | `Undo`, `Hint` | −26.0, −23.6 | −24 to −26 | undo on target; the hint, now the soft glint (P7), sits 0.4 LU above the band, as the old one did |
| failure | `InvalidTap` | −32.2 | −32 | on target; the soft tok (P1), up from −59.4 |
| ui (nav) | `ButtonForward` | −30.0 | −30 | on target (P4b), up from −34.2 |
| ui (nav) | `ButtonDismiss` | −33.4 | −33 to −34 | unchanged; now under forward |
| ui (settings) | `SoundEnabled` | −22.0 | −22 | unchanged |
| music | — | none | none | the product has no music |

## Settings

- **Mute:** `soundOn` in DataStore, default on (fail-open, D-17), is the Settings → Sound switch.
  The gate lives in `audio/SoundPoolPlayer.kt` (`:105`, `:128-131`, `:165`). Switching it off
  stops every stream already playing (`silenceNow`, `:206-212`, SOUND-05). Switching it on plays
  `SoundEnabled` through the one documented bypass. On develop the switch's supporting copy is
  still "Saved for the upcoming sound effects" (`res/values/strings.xml:143`); phase 15 corrects
  it.
- **Volume:** the app has no volume slider of its own. The pool plays as `USAGE_GAME` /
  `CONTENT_TYPE_SONIFICATION` (`SoundPoolPlayer.kt:82-91`), which routes it to the media stream,
  so the system media volume controls it. The app never requests audio focus, so it never ducks
  the player's music (SOUND-06, enforced by the `audioFocusLiteralCheck` build gate).
- **System silent mode:** **not honoured.** Ringer Silent or Vibrate does not touch the media
  stream, so cues play whenever media volume is above zero. rules/audio.md requires honouring it
  (P3).
- **Background:** nothing happens. One-shots finish their tail (at most about 2.9 s, the three
  star pops), and there is no music to pause (P8).
- **Haptics:** a separate `hapticsOn` switch, default on. The only haptic in the game is on the
  jump.

## Accessibility

Eleven of the twelve cues have a visual partner, and the jump also has a haptic (see the
Cue table). **`InvalidTap` has none.** A rejected tap leaves no trace on the board. The new soft
tok is audible on a phone speaker (−32 LUFS through the stand-in, up from −59). But with sound
off, on Silent once P3 lands, or for a player who cannot hear, a wrong tap still produces no
feedback at all. This fails rules/audio.md until the ux-designer's answer is built (P1). That
answer is specified in `docs/ux/invalid-tap-feedback.md` (D-06): a head-shake of the tapped cell,
a reject haptic and an announcement. None of the three depends on the sound setting.

Reduced motion: the star pops keep their own fixed 625 ms stagger when OS animations are off
(D-13), and the jump cue does not depend on animation. On develop, the removal layer is dropped
when animations are off: the snap path clears `animating` before the 60 ms delay ends. The fix is
3400828 on `feature/phase-15-ux-refresh`, and the phase-15 merge brings it to develop (P9).

## Sourcing

Ten shipped cues are sourced, and two are in-house recipes (P1, P7). Nothing new was sourced, so
there are no sourcing candidates.

| Cue | Candidate | Source URL | License | Why |
| --- | --- | --- | --- | --- |
| — | none proposed | — | — | replacements are in-house recipes |

Founder pick: none pending for sourced material.
Inbox: `.planning/audio-inbox/` — local and git-ignored. Files dropped there are normalised and
then committed as assets below.

Licence status, rechecked 2026-09-29 against the live pages: Kenney *Interface Sounds* 1.0 says
"Creative Commons CC0". Freesound 506546 "Pop 01" by LilMati and 531512 "Positive Blip Effect" by
CogFireStudios both say "Creative Commons 0", and their stated specs match the masters pinned in
CREDITS.md. Done on 2026-09-29: CREDITS.md has an author column, and the Kenney pack archive has a
pinned hash. Still missing: a research-analyst verdict per file (`docs/audio/licence-verdicts.md`,
being written) and a `THIRD_PARTY.md` on develop (P6). One point for that verdict: the tags inside
the 506546 file name the artist as Mattias "MATTIX" Lahoud, while the freesound uploader is
LilMati.

## Assets

| Cue | File | Format | Size | Recipe or source + license |
| --- | --- | --- | --- | --- |
| `Jump` | `app/src/main/res/raw/sfx_jump.wav` | PCM mono 16-bit 44.1 kHz | 16,642 B | Kenney *Interface Sounds* `drop_003`, CC0 1.0; −3.00 dB to a −1.10 dBFS peak (P4a); verdict pending |
| `PegRemove` | `app/src/main/res/raw/sfx_peg_remove.wav` | PCM mono 16-bit 44.1 kHz | 16,642 B | Kenney `drop_002`, CC0 1.0; level kept; verdict pending |
| `Select` | `app/src/main/res/raw/sfx_select.wav` | PCM mono 16-bit 44.1 kHz | 5,962 B | freesound 506546 by LilMati, CC0 1.0, from its single-encode OGG; master md5 pinned in CREDITS.md; verdict pending |
| `Deselect` | `app/src/main/res/raw/sfx_deselect.wav` | PCM mono 16-bit 44.1 kHz | 6,688 B | derived from 506546 (reversed, −2 semitones), CC0 1.0, from its single-encode OGG; verdict pending |
| `Undo` | `app/src/main/res/raw/sfx_undo.wav` | PCM mono 16-bit 44.1 kHz | 18,302 B | Kenney `minimize_008`, CC0 1.0; level kept; verdict pending |
| `InvalidTap` | `app/src/main/res/raw/sfx_invalid.wav` | PCM mono 16-bit 44.1 kHz | 7,982 B | in-house recipe `audio/recipes/invalid.json`, rendered with `sfx-synth.sh`; re-renders byte-identically |
| `Hint` | `app/src/main/res/raw/sfx_hint.wav` | PCM mono 16-bit 44.1 kHz | 19,448 B | in-house recipe `audio/recipes/hint.json`, rendered with `sfx-synth.sh`; re-renders byte-identically |
| `Win` | `app/src/main/res/raw/sfx_win.wav` | PCM mono 16-bit 44.1 kHz | 47,580 B | Kenney `confirmation_002`, CC0 1.0; level kept; verdict pending |
| `StarPop` | `app/src/main/res/raw/sfx_star_pop.wav` | PCM mono 16-bit 44.1 kHz | 145,574 B | freesound 531512 by CogFireStudios, CC0 1.0, from its single-encode OGG; master md5 pinned; verdict pending |
| `ButtonForward` | `app/src/main/res/raw/sfx_btn_forward.wav` | PCM mono 16-bit 44.1 kHz | 8,610 B | Kenney `click_001`, CC0 1.0; +4.14 dB to a −1.00 dBFS peak (P4b); verdict pending |
| `ButtonDismiss` | `app/src/main/res/raw/sfx_btn_dismiss.wav` | PCM mono 16-bit 44.1 kHz | 8,360 B | Kenney `back_004`, CC0 1.0; level kept; verdict pending |
| `SoundEnabled` | `app/src/main/res/raw/sfx_sound_enabled.wav` | PCM mono 16-bit 44.1 kHz | 25,604 B | Kenney `confirmation_001`, CC0 1.0; level kept; verdict pending |

Twelve cues and twelve assets, 327,394 B in all (71,682 B as OGG). Every asset is used by a cue,
no cue plays that this file does not name, and each cue has exactly one file in `res/raw`. The two
in-house assets have committed recipes. The conversion command, the trims and gains, and the
sha256 of every file are in `docs/audio/CREDITS.md`.

---

## Proposed changes — ranked by player impact, with status after Q-008 to Q-010

Founder answers of 2026-09-29: **Q-008** chose the wrong-tap soft tok (candidate a) and the hint
soft glint (candidate a), and **kept today's peg-removal sound**. **Q-009** chose to convert the set
to WAV and re-level it in the same pass. **Q-010** chose to mute on Silent or Vibrate only on the
phone speaker, so headphones keep sound. The two adopted candidates are committed: their recipes
are in `audio/recipes/` and their renders in `app/src/main/res/raw/` (af2b554). The candidates
not adopted stay rendered but uncommitted in the git-ignored
`.planning/qa/audio-review/candidates/`, next to their recipes and the A/B clips. Every candidate
was levelled to drop into the **existing** `SoundParams` volume, so the adopted ones needed no
code change.

**P1 — The invalid tap gives no feedback on a phone.** *Status: sound done (Q-008, af2b554). The
visual, haptic and announcement are specified (ux-designer, 9abac3c) and not yet built
(android-game-dev).*

Nearly all of the cue's energy is a
236 Hz bong that a phone speaker does not reproduce: it measures −59 LUFS in game through the
stand-in, 50 LU under the win. The event also has no visual or haptic partner, which fails
rules/audio.md.
- Sound (sound-designer, done): `sfx_invalid.wav` is `invalid_a`, rendered from
  `audio/recipes/invalid.json`. It measures −32.2 LUFS in game on the phone stand-in, as designed.
  The two candidates were these. `invalid_a` is a "soft tok": a triangle 420→360 Hz through a 1.2 kHz
  low-pass with a 20 ms muffled tap, 90 ms. `invalid_b` is a "muted bwop": a sine falling a fourth,
  520→390 Hz, 120 ms. Both measure −32.2 LUFS in game through the stand-in at the unchanged
  `VOLUME_INVALID` 0.3. That is level with the peg pick-up and 14 LU under the jump, so SOUND-02's
  "quiet, non-punishing" relation to the jump still holds. Files: `candidates/sfx_invalid_a.wav`,
  `candidates/sfx_invalid_b.wav`. A/B clips: `candidates/ab/ab_select_invalid_x2_deselect_{incumbent,a,b}.wav`.
- Visual partner (ux-designer specifies, android-game-dev builds). Specified in
  `docs/ux/invalid-tap-feedback.md` (D-06, 9abac3c): a head-shake of the tapped cell, a redirect
  while a peg is held, a reject haptic (D-06b) and a screen-reader announcement (D-06c). The
  sound fires exactly as before.

**P2 — Peg removal sounds like a second jump.** *Status: not adopted. The founder keeps today's
sound (Q-008).*

`drop_002` and `drop_003` are one sound about 0.8 semitone apart (spectrum similarity 0.998). Played 60 ms after the jump, the removal reads as
a flam on the jump rather than as a peg being taken. Every jump in the game plays it, and SOUND-02
asks for distinct removal feedback. Replace `sfx_peg_remove` with an in-house candidate:
- `peg_remove_a`, "clay squish": a sine sweep 560→300 Hz, a triangle overtone and a 40 ms
  low-passed noise squelch, 110 ms. It sits under the jump's pitch, and its spectrum similarity
  to the jump is 0.26.
- `peg_remove_b`, "pfft": 40 ms of airy noise and a 1.4→0.8 kHz sine flick, 80 ms. It sits above
  the jump, and its similarity is 0.18.
Both land at −26 LUFS in game through the stand-in, 8 LU under the jump. Today the removal is
12 LU under. Neither the volume (0.45) nor the 60 ms delay changes. A/B clips:
`candidates/ab/ab_jump_run_with_remove_{incumbent,a,b}.wav` (a three-jump run on the real pitch
ladder).

**Q-008 against SOUND-02: keep.** The founder heard the three-jump run with each removal and chose
the incumbent (`ab_jump_run_with_remove_incumbent`). `sfx_peg_remove` stays Kenney `drop_002`,
converted to WAV at its current level. Neither candidate is committed. For SOUND-02 ("distinct
removal feedback") the audible distinction stays as it is: the same drop material 0.8 semitone
higher, 60 ms after the jump and 12 LU under it. The distinct feedback is visual, the jumped peg
fading and shrinking. That is the founder's ruling on the sound, and the sound-designer does not
re-open it. If SOUND-02's acceptance test asks for an audibly different removal *sound*, the
requirement's owner records this as an accepted deviation.

**P3 — System silent mode is not honoured** (a rules/audio.md FAIL). *Status: Q-010 answered
(option 2, speaker only). The tech-lead is designing it in `docs/tech/audio-silent-mode.md`, and
the android-game-dev builds it.*

A player on Silent or Vibrate hears the game whenever media volume is up. This is playback work that no design covers, so it
goes to the **tech-lead** to design and the android-game-dev to build. One way is to gate the
ordinary cues on `AudioManager.getRingerMode()` and track `RINGER_MODE_CHANGED_ACTION`. That is
not an audio-focus call, so the SOUND-06 gate still holds. The headphones question is settled:
Q-010 keeps sound on headphones and mutes the phone speaker only.

**P4 — Levels.**
- (a) The `sfx_jump` peak rule (sound-designer): re-level the asset to −1.1 dBFS and set
  `VOLUME_JUMP` 0.9 → 1.0 (android-game-dev). The jump moves by −0.2 dB in game, and the mix peak
  falls to about −1.1 dBFS. Re-levelling the asset alone makes the jump about 1 dB quieter.
  *Status: asset done (d1c6e9b, −3.00 dB to a −1.10 dBFS peak). The constant is open for the
  android-game-dev, and until it changes the jump plays 1.1 dB quieter than before. After it
  changes, the layer peaks at −1.10 dBFS on every rung (simulated).*
- (b) The button pair (sound-designer): raise `sfx_btn_forward` +4.2 dB, to a −1.0 dBFS peak. Forward
  then reads −30.0 LUFS on the phone stand-in (−28.6 full range) against dismiss's −33.7 (−29.0),
  so forward is louder on both. That matches `SoundCue.kt`'s "dismiss is softer", with no code
  change. *Status: done (d1c6e9b). The source file's own peak allowed +4.14 dB before the −1 dBFS
  rule, not +4.2, so the WAV peaks at −1.00 dBFS (−0.9 dBTP). Forward measures −28.7 full range
  and −30.0 on the stand-in, against dismiss's −29.0 and −33.4.*
- (c) `Deselect` measures −41.5 on the stand-in, at the edge of audibility in any room noise. The
  founder tuned it by ear, so the **qa-engineer** listens first on a phone speaker at half media
  volume. If it is lost, change `DESELECT_ATTENUATION` from 10 dB to 6 dB. *Status: not done. It
  needs the device listen first, it is a code constant rather than an asset level, and Q-008's
  answer did not rule on it. The qa-engineer listens, and the android-game-dev changes the
  constant if the put-down is lost.*

**P5 — Format** (a rules/audio.md FAIL: SFX must be mono 16-bit 44.1 kHz WAV, and OGG is only for
sourced music). *Status: done (Q-009 option 1; af2b554, d1c6e9b). All twelve are WAV and no
`.ogg` is left in `res/raw`. On a clean debug package the APK grows by exactly 255,712 B, and the
release APK grows by the same stored bytes. Open for the android-game-dev: `AudioCreditsTest`
still scans `*.ogg` only, so it now finds no files and fails. The KDoc at `SoundCue.kt:44` and
`SoundParams.kt:137` and `:155` still names `sfx_deselect.ogg`.*

The trade-offs and the recommendation are in Q-009. Converting the set means
+255 KB of stored WAV: 326 KB against 72 KB today, of which the star pop alone is 146 KB. That is
about 12 % on the 2.06 MB release APK. aapt2 stores `.wav` uncompressed, and deflate recovers only
about 22 %, so the download grows by about the same amount. Quality holds or improves when the
cues are rendered from their masters rather than from today's OGG. The Kenney originals are
themselves OGG, and a WAV render drops the second lossy generation. PCM output is deterministic,
unlike libvorbis, so a hash gate becomes possible. It costs the android-game-dev one test change:
`AudioCreditsTest` looks only at `*.ogg`, and in-house assets have a recipe path instead of an
https source. `R.raw.*` ids do not change.

**P6 — Provenance** (no player impact, but it blocks the release). *Status: CREDITS.md's author
column and the Kenney archive's hash are done. The research-analyst's verdicts are being written
in `docs/audio/licence-verdicts.md`, and `THIRD_PARTY.md` is the release-manager's.*

The research-analyst gives a verdict per sourced file. The release-manager lands `THIRD_PARTY.md` on develop with one row per
sourced asset and a "Verdict by" column. The draft on `feature/phase-14-release-docs` (e5253f4)
has four audio rows, puts the nine Kenney files in one row, and has no verdict. CREDITS.md gains
an author column. The Kenney pack archive gets a pinned hash, as the two freesound masters
already have.

**P7 — Hint character** (taste, optional). *Status: done. Q-008 adopted `hint_a`, and
`sfx_hint.wav` is rendered from `audio/recipes/hint.json` (af2b554).*

`pluck_002` is the brightest cue in the set, with a spectral centroid of 4.5 kHz, about 2.4 times the next brightest (the win). It is a little glassy against
the "soft candy-clay" brief (D-02). Candidate `hint_a` is a "soft glint": 1320 Hz plus a fifth
above, 220 ms. It is levelled to the incumbent (−23.6 against −23.8 LUFS in game) and asked as an
option in Q-008. A/B clips: `candidates/ab/ab_hint_{incumbent,a}.wav`.

**P8 — Background** (low). Stop in-flight one-shots when the process goes to the background.
`AppForegroundTracker` already reports this. android-game-dev.

**P9 — The removal layer with animations off.** This is already fixed on phase 15 (3400828) and
needs nothing beyond that merge. The qa-engineer re-checks it on develop afterwards.

**Not proposed:** a "stuck" cue. The rescue banner is a neutral moment, and phase 07 kept it
silent on purpose.
