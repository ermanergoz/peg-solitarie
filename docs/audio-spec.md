# Audio spec — Peg Solitaire

The cue list of record (rules/audio.md). Written 2026-09-29 by the sound-designer as a review of
the set that shipped in phase 07 and its two follow-ups (quick tasks 260828-vb3 and 260828-vrr).
It was updated the same day after the founder's answers to Q-008 and Q-009. **Assets,
Measurements and the File, Character and Length columns describe the WAV set on
`feature/audio-review` at d1c6e9b; no asset has changed since.** On 2026-10-23, after the
android-game-dev's integration (`62c774d..8663710`), **the code references, volumes, haptics,
Settings, Accessibility, Mix and the tutorial rows describe `feature/audio-review` at 8663710**,
which carries phase 15 (merged at 62c774d). Line numbers move; the names govern. "Proposed
changes" gives each item's status and who does what is left.

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
| A peg completes a jump; +1 semitone per consecutive jump by the same peg, capped at 2.0× (`audio/JumpSoundEffect.kt:75-83`, called at `ui/game/GameScreen.kt:167`, keyed on `MoveTransition.id`) | `Jump` | `sfx_jump.wav` | rounded mid "drop" pop, steady ~730 Hz, instant attack, −20 dB after 40 ms | 188 ms | 0 | 1.0 (P4a) | `ContextClick` when Haptics is on, through the shared `ui/game/JumpHapticEffect.kt` (called at `GameScreen.kt:166`) | peg slides, landing squash | Kenney `drop_003`, CC0; re-levelled to a −1.10 dBFS peak (P4a) |
| The jumped peg is removed — same effect, 60 ms after the jump | `PegRemove` | `sfx_peg_remove.wav` | the same drop pop ~0.8 semitone higher, rising 800→925 Hz; **kept by the founder** (Q-008, see P2) | 188 ms | 0 | 0.45 | — (the jump's) | jumped peg fades and shrinks | Kenney `drop_002`, CC0 |
| A peg is picked up (`selectionTick`, `GameScreen.kt:309-314`) | `Select` | `sfx_select.wav` | short bright pop, ~1.46 kHz transient | 67 ms | 0 | 0.6 | none | selection ring, breathing pulse, destination dots pop in | freesound 506546 (LilMati), CC0 |
| The held peg is re-tapped to put it down (`deselectionTick`, `GameScreen.kt:325-330`) | `Deselect` | `sfx_deselect.wav` | the select pop reversed and a whole tone lower: a soft swell | 75 ms | 0 | 0.1897 (−10 dB under `Select`) | none | ring and dots disappear | derived from 506546, CC0 |
| Undo actually steps the board back (`undoTick`, `GameScreen.kt:341-346`) | `Undo` | `sfx_undo.wav` | low descending "whoop", 410→260 Hz | 207 ms | 0 | 0.7 | none | the last move reverses on the board | Kenney `minimize_008`, CC0 |
| A tap that changes nothing: an empty hole with nothing held, or another peg or a hole that is not a legal destination while a peg is held (W1–W3 in `docs/ux/invalid-tap-feedback.md` §1.1; an out-of-play cell or a cell's dead corner never reaches the ViewModel) (`rejectedTapTick`, `GameScreen.kt:356-361`; the ViewModel also records the tapped cell, `rejectedTapCell`) | `InvalidTap` | `sfx_invalid.wav` | "soft tok": a triangle falling 420→360 Hz through a 1.2 kHz low-pass, with a 20 ms muffled noise tap. Its ~400 Hz fundamental survives a phone speaker (0.4 % of its energy is below 300 Hz) | 90 ms | 0 | 0.3 | the platform `Reject` haptic, once per wrong tap, only while Settings › Haptics is on and only on API 30+ (`wrongTapHapticFires`, `ui/game/WrongTap.kt:92-95`; `WrongTapHaptic`, `GameScreen.kt:376-386`). Not gated by Sound, the ringer or the background (D-06b) | on the tapped cell: a 270 ms head-shake, or with animations off a dashed mark held 450 ms (D-06a); while a peg is held, a redirect — the ring nudges (W2) or the destination dots re-pop (W3, only with Show-moves on); and a polite screen-reader sentence on the board's live region (D-06c, `ui/game/WrongTapAnnouncement.kt`). Built in `ui/components/WrongTapFeedback.kt` and `BoardCanvas.kt` (8f4bf24, fe32924, 5e0af0d) | in-house, `audio/recipes/invalid.json` (Q-008) |
| A hint is revealed: the search resolved to a move (`hintSource` set, `GameScreen.kt:396-401`) | `Hint` | `sfx_hint.wav` | "soft glint": 1320 Hz with a fifth above (1980 Hz) and a faint 2640 Hz triangle, centroid 1.4 kHz, a 3 ms attack and a 200 ms decay | 220 ms | 0 | 0.6 | none | source glow, target ring, faint arrow | in-house, `audio/recipes/hint.json` (Q-008) |
| The winning move lands: status becomes `Won`, i.e. pegs ≤ the board's minimum (`GameScreen.kt:413-418`) | `Win` | `sfx_win.wav` | rising four-note arpeggio 1.18→4.7 kHz | 539 ms | 1 | 1.0 | none | final jump, then the Results entrance | Kenney `confirmation_002`, CC0 |
| Each earned star lands on Results, once per win (D-16) (`StarPopSounds`, `ui/results/ResultsCelebration.kt:99-114`, from `ResultsScreen.kt:225`) | `StarPop` ×1–3 | `sfx_star_pop.wav` | rising blip 775→1184 Hz with a long ring; played at rates 0.63 / 0.79 / 1.0 (a major sixth below, a major third below, unison) | 1650 ms (2.62 s at rate 0.63) | 1 | 0.85 | none | star pops in (lands instantly with animations off) | freesound 531512 (CogFireStudios), CC0 |
| A forward or commit button (D-09/D-11) — `withClickSound(ButtonForward, …)`: Home resume and mode tiles (`ui/home/HomeScreen.kt:482`, `:654`), a Classic board (`ui/classic/ClassicCatalogScreen.kt:143`), Results Next level / Retry / Replay / Home / Catalog (`ui/results/ResultsActions.kt:96`, `:123`, `:152`), the tutorial's "Got it" (`ui/onboarding/TutorialScreen.kt:323`), Settings "How to play" (`ui/settings/SettingsScreen.kt:232`), dialog confirms (`GameScreen.kt:523` restart, `ui/game/AdOfferDialog.kt:197`, `ui/home/ProfileSwapDialog.kt:188` keep local, `ui/home/PlayGamesPromptDialog.kt:161`) | `ButtonForward` | `sfx_btn_forward.wav` | short neutral click, centroid 1.6 kHz; raised 4.1 dB (P4b) | 97 ms | 0 | 0.7 | none | `claySquish` press, then the screen or dialog changes | Kenney `click_001`, CC0 |
| A dismiss or cancel button (`GameScreen.kt:539` restart cancel, the tutorial's Skip (`TutorialScreen.kt:178`), `AdOfferDialog.kt:237`, `ProfileSwapDialog.kt:209` use cloud — deliberately inverted, `PlayGamesPromptDialog.kt:182`) | `ButtonDismiss` | `sfx_btn_dismiss.wav` | darker click, 270/540 Hz, centroid 460 Hz | 94 ms | 0 | 0.7 | none | `claySquish` press, the dialog closes | Kenney `back_004`, CC0 |
| The player switches Sound **on** in Settings; user action only, bypasses the mute gate (D-12) (`ui/settings/SettingsViewModel.kt:136-137`) | `SoundEnabled` | `sfx_sound_enabled.wav` | rising arpeggio 390→1570 Hz: the win's shape, about 1.5 octaves lower | 290 ms | 0 | 0.6 | none | the switch moves to on | Kenney `confirmation_001`, CC0 |
| The player switches Sound **off** | none | — | silence is the confirmation (D-12) | — | — | — | none | the switch moves to off | — |
| Haptics and Show-moves switches in Settings; the Show-moves control on the board | none | — | toggles stay quiet (D-09) | — | — | — | none | the switch or control changes state | — |
| Restart control on the board (the confirm dialog's buttons sound, above) | none | — | assist control, not a navigation (D-09) | — | — | — | none | the confirm dialog opens | — |
| Back navigation and the top-bar icons (How to play, Settings) | none | — | D-09 | — | — | — | none | the screen changes | — |
| The board is stuck: no legal move and pegs above the minimum | none | — | deliberate: phase 07 left "lose/stuck stingers" out of scope | — | — | — | none | the rescue banner (`ui/game/NoWinBanner.kt`) | — |
| The hint search finds no winning move; the solver is thinking | none | — | — | — | — | — | none | no-win banner; spinner in the Hint control | — |
| Rescue-banner actions Undo / Restart / Home | `Undo` via `undoTick` when the board steps back; otherwise none | — | — | — | — | — | none | the board or screen changes | — |
| Score count-up, achievement pop-up, snackbars, cloud-save sync, a new Challenge level loading | none | — | — | — | — | — | none | each has its own visual | — |

### Tutorial and dock — phase 15 (merged to develop; on this branch since 62c774d)

From `docs/ux-spec.md`: D-01h first made the tutorial silent end to end; **D-01i amended it the
same day**, so a completed jump sounds and the two exit buttons sound. D-01j (the wrong-tap
amendment, `docs/ux/invalid-tap-feedback.md` §4) adds the jump's haptic and gives a tutorial wrong
tap its redirect only. No new asset and no new level.

| Event | Cue | Ruling | Visual and haptic partner |
| --- | --- | --- | --- |
| A tutorial peg completes a jump | `Jump` + `PegRemove`, through the shared `audio/JumpSoundEffect.kt` (same levels, same pitch ladder; `TutorialScreen.kt:115`) | fires (D-01i, SOUND-01) | slide and removal fade; the game's own `ContextClick` through the shared `ui/game/JumpHapticEffect.kt` (`TutorialScreen.kt:116`), under the persisted Haptics switch, which the tutorial reads through the read-only `HapticsSetting` seam (D-01j(1), tech-lead ruling 2026-10-18) |
| A tutorial peg is picked up or put down | `Select`, `Deselect` | silent (D-01h, upheld) | ring, dots |
| A wrong tap in the tutorial | `InvalidTap` | silent (D-01c/D-01h): a rejection sound would make a wrong tap feel like failure | the redirect only, while a peg is held: the coach ring nudges or the dots re-pop. No shake, no mark, no haptic, no announcement (D-01j(2)) |
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
`SoundParams` volume as it was on develop at 3f2af90, so this table still applies `VOLUME_JUMP`
0.9; the jump row gives the 1.0 figure in brackets, and since 2e82c3e 1.0 is the shipped value
(Mix uses it). **Phone** is the same
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
`sfx_jump` peaks at −1.10 dBFS (it was −0.02). With `VOLUME_JUMP` at 1.0 (P4a, 2e82c3e) the
jump + removal layer peaks at −1.10 dBFS on every rung of the ladder (simulated with linear
resampling for rates 1.0 to 2.0); at the old 0.9 it peaked at −2.02, and before the conversion
at −0.93, which failed. The worst celebration overlap (the win, then three star pops that ring together from
1.25 s to 2.9 s) peaks at −1.58 dBFS and passes, unchanged.

Levels below are L100 in game through the phone stand-in, which is how most players hear the
game ("Now" is the WAV set at today's volumes). The targets keep every cue within 24 LU of the
win, so nothing drops below −34. Every cue meets that except `Deselect`.

| Category | Cues | Now | Target | Notes |
| --- | --- | --- | --- | --- |
| success | `Win` | −9.7 | −10 | the anchor; unchanged |
| success | `StarPop` | −13.5 | −13 to −14 | unchanged |
| move | `Jump` | −18.4 | −18 | done (P4a): the asset at a −1.10 dBFS peak (d1c6e9b) and `VOLUME_JUMP` 1.0 (2e82c3e); −19.3 at the old 0.9 |
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

`audio/SoundPoolPlayer.kt` is the one playback owner, in the UI layer's `audio/` package and never
in `:domain`. It holds **three gates**, combined as an AND, each fed by its own collector on the
application scope (`Dispatchers.Default`), never a `combine`. Closing any gate stops what is in
flight (`silenceNow`, `:261-267`, SOUND-05's standard); opening one replays nothing.

- **Mute:** `soundOn` in DataStore, default on (fail-open, D-17), is the Settings → Sound switch
  ("Play the jump, button and celebration sounds"). Gate at `SoundPoolPlayer.kt:117-118`,
  `:156-161` and `:220`. Switching it on plays `SoundEnabled` through the one documented bypass
  (`:210-213`), which skips this gate only and still respects the other two.
- **Volume:** the app has no volume slider of its own. The pool plays as `USAGE_GAME` /
  `CONTENT_TYPE_SONIFICATION` (`audio/GameAudioAttributes.kt`, the one definition the pool and the
  route query share), which routes it to the media stream, so the system media volume controls
  it. In the app the hardware volume keys drive that stream even with nothing playing
  (`volumeControlStream = STREAM_MUSIC`, `MainActivity.kt:142`, V1), so turning the game down can
  never step the ringer into Vibrate. The app never requests audio focus, so it never ducks the
  player's music (SOUND-06, enforced by the `audioFocusLiteralCheck` build gate).
- **System silent mode:** **honoured on the phone speaker only** (Q-010 option 2,
  `docs/tech/audio-silent-mode.md` §1). On Silent or Vibrate, while game sound would come out of
  the phone itself (speaker or earpiece), no cue plays and anything ringing stops; through wired,
  USB or Bluetooth headphones or another external media output the cues keep playing.
  `audio/AndroidSystemAudioState.kt` reads the ringer and the output route, event-driven
  (`RINGER_MODE_CHANGED_ACTION`, `ACTION_AUDIO_BECOMING_NOISY`, an `AudioDeviceCallback`), off
  the main thread; `SystemAudioSnapshot.mutesGameSound` (`audio/SystemAudioState.kt:29-30`) is
  the decision, and `audio/AudioRouteClassifier.kt` the allowlist of external outputs (an
  unknown output counts as the phone). Pulling headphones counts as the phone at once. A reader
  that fails leaves sound playing (fail-open). The `SoundEnabled` pop is muted here too, and the
  switch moving is its partner. The Sound switch still reads "on" while the ringer mutes the
  speaker; whether it gets a supporting line is the ux-designer's open item O4.
- **Background:** when the process leaves the foreground (`ProcessLifecycleOwner`, read directly
  by `audio/AppVisibility.kt`; `ON_STOP` arrives about 0.7 s after the last Activity stops), every
  stream in flight stops and new cues, the star pops' delayed ones and the `SoundEnabled` pop
  included, are refused until the app is back (P8, design §2.1). Nothing replays on return.
- **Haptics:** a separate `hapticsOn` switch, default on. It gates the game's **two** haptics and
  nothing else gates them — not Sound, not the ringer, not the background: the jump's
  `ContextClick` (gameplay and tutorial, through the shared `ui/game/JumpHapticEffect.kt`) and the
  wrong tap's `Reject` (gameplay only, API 30+, `ui/game/WrongTap.kt:92-95`). So on Vibrate with
  the speaker, the jump and the wrong tap still buzz while their sounds are muted. The switch's
  supporting copy still reads "Vibrate lightly on each jump" (`res/values/strings.xml:160`), which
  no longer names the wrong tap's buzz; that copy is the ux-designer's.

## Accessibility

All twelve cues have a visual partner (see the Cue table), and two also have a haptic: the jump
(`ContextClick`) and the wrong tap (`Reject`, API 30+). **`InvalidTap`'s gap is closed** (P1,
8f4bf24, fe32924, 5e0af0d): a wrong tap on the gameplay board shakes the tapped cell, or shows a
dashed mark with animations off; redirects to the held peg's ring or dots while one is held;
buzzes; and is spoken politely to a screen reader. None of the four depends on the Sound switch,
the ringer or the background gate, so with sound off, on Silent, or for a player who cannot hear,
a wrong tap is still answered. A tutorial wrong tap plays no sound, so the redirect alone is
enough there (D-01j(2)).

Reduced motion: the star pops keep their own fixed 625 ms stagger when OS animations are off
(D-13); the jump cue and its removal layer do not depend on animation (3400828, phase 15, now on
this branch: the removal layer runs on the composition scope, so the snap path no longer drops
it; P9's device recheck is the qa-engineer's); and the wrong tap's shake becomes a still mark,
never nothing (D-06a).

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

**P1 — The invalid tap gives no feedback on a phone.** *Status: done in code. Sound: Q-008,
af2b554. Shake or mark, redirect, reject haptic and announcement: specified by the ux-designer
(9abac3c), built by the android-game-dev on this branch (09ce3eb, 8f4bf24, fe32924, 5e0af0d,
cc65702) and reviewed by the sound-designer on 2026-10-23
(`.planning/reports/audio-review/sound-designer-integration.md`). The qa-engineer's device checks
(WT-12, WT-14, WT-18) remain.*

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
  "quiet, non-punishing" relation to the jump still holds. Since `VOLUME_JUMP` became 1.0 (P4a) the
  gap to the jump is 13.8 LU (−32.2 against −18.4). Files: `candidates/sfx_invalid_a.wav`,
  `candidates/sfx_invalid_b.wav`. A/B clips: `candidates/ab/ab_select_invalid_x2_deselect_{incumbent,a,b}.wav`.
- Visual partner (ux-designer specified, android-game-dev built). Specified in
  `docs/ux/invalid-tap-feedback.md` (D-06, 9abac3c): a head-shake of the tapped cell, or a dashed
  mark with animations off (D-06a), a redirect while a peg is held, a reject haptic (D-06b) and a
  screen-reader announcement (D-06c). Built as specified; see the Cue table. The sound fires
  exactly as before: `InvalidTapSound` is unchanged by the integration.

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

**P3 — System silent mode is not honoured** (a rules/audio.md FAIL). *Status: done in code.
Q-010 answered option 2 (speaker only); the tech-lead designed it in
`docs/tech/audio-silent-mode.md` §1, and the android-game-dev built it (c2a16dc, 8b62414, 930d816;
V1, the volume keys, df903b3). See Settings › System silent mode. The qa-engineer's device checks
D1–D6, D8–D10 remain.*

A player on Silent or Vibrate hears the game whenever media volume is up. This is playback work that no design covers, so it
goes to the **tech-lead** to design and the android-game-dev to build. One way is to gate the
ordinary cues on `AudioManager.getRingerMode()` and track `RINGER_MODE_CHANGED_ACTION`. That is
not an audio-focus call, so the SOUND-06 gate still holds. The headphones question is settled:
Q-010 keeps sound on headphones and mutes the phone speaker only.

**P4 — Levels.**
- (a) The `sfx_jump` peak rule (sound-designer): re-level the asset to −1.1 dBFS and set
  `VOLUME_JUMP` 0.9 → 1.0 (android-game-dev). The jump moves by −0.2 dB in game, and the mix peak
  falls to about −1.1 dBFS. Re-levelling the asset alone makes the jump about 1 dB quieter.
  *Status: done. The asset (d1c6e9b, −3.00 dB to a −1.10 dBFS peak) and `VOLUME_JUMP` 1.0
  (2e82c3e) landed on the same branch, so the jump plays 0.2 dB under its old level and the
  layer peaks at −1.10 dBFS on every rung (simulated). `SoundParamsTest` pins the constant
  (`theJumpIsMixedAsLoudAsThePeakRuleAllows`, `noCueIsMixedAboveTheWinStinger`).*
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
release APK grows by the same stored bytes. The android-game-dev's half is done too: 44b14bb
rewrote `AudioCreditsTest` to the CREDITS.md contract of `docs/tech/audio-silent-mode.md` §2.3 and
added `AudioAssetFormatTest` (format, size, peak, one file per cue), 6b4eb08 makes an edit to
CREDITS.md or `audio/recipes/` re-run the unit tests, and 1572197 swept the `.ogg` KDoc.*

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
*Status: done in code (116df6e).* The tech-lead's design (`docs/tech/audio-silent-mode.md` §2.1)
reads `ProcessLifecycleOwner` directly through `audio/AppVisibility.kt`, **not**
`AppForegroundTracker`, which is the interstitial session boundary in `platform/`. Both read the
same process lifecycle, so they agree on what "background" means. New cues are also refused
until the app is back. The qa-engineer's device check D7 remains.

**P9 — The removal layer with animations off.** Fixed on phase 15 (3400828), which is merged to
develop and on this branch. The qa-engineer re-checks it on a device.

**Not proposed:** a "stuck" cue. The rescue banner is a neutral moment, and phase 07 kept it
silent on purpose.
