# sound-designer report — audio-review (integration review, 62c774d..8663710)
Date: 2026-10-23
Branch: feature/audio-review (report only; the doc edits are left uncommitted for the producer, as dispatched)
Skills loaded: ergoz-soft:standards (role: sound-designer; game; android-compose-game). Rule files read in full from the plugin (1.0.0, because the product snapshot is 0.6.2): studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, ip-policy.md; also the snapshot's lessons.md
Ran in parallel with: the code-auditor's review of the same range (same worktree; no Gradle run here)
Ran on: claude-opus-5-5 at effort high. Review mode is dispatched on fable (rules/studio.md, Model and effort); this ran on Opus under the founder's Q-019 answer (option 3, 2026-10-17: Opus for every Fable-pinned role until 3.0.0, deviation recorded in each report)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- The integration meets rules/audio.md and `docs/audio-spec.md` on every item: one playback owner with three AND-gated mute inputs, preload off the main thread, state-keyed triggers, Silent/Vibrate honoured on the speaker, background stop, no main-thread audio work beyond the play call, and every cue paired with a visual (two also with a haptic).
- Every cue the spec names is wired to its event; 12 cues, 12 assets, none unused; formats, sizes, peaks and hashes hold, and both recipes re-render byte-identically. `VOLUME_JUMP` 1.0 and `VOLUME_DESELECT` 0.18972 match the spec's mix.
- Two Minor findings and one Info, none blocking: a wrong-tap cue that replays after a rotation without its partners (pre-existing idiom), and stale Haptics copy.
- My own files are brought up to the code: the six stale places the tech-lead listed in `docs/audio-spec.md`, the status lines and code references that went stale with them, and CREDITS.md's prose about the credits test. No hash and no Files-table cell changed.

## Review — rules/audio.md and the spec, item by item
Paths are under `app/src/main/java/com/app/pegsolitaire/` unless given in full.

| # | Item | Verdict | Reason |
|---|---|---|---|
| R1 | One playback owner in the UI layer, never `game-core` | PASS | `audio/SoundPoolPlayer.kt:69-81`, a `@Singleton` in `:app`, is the only class that touches `SoundPool`. The new inputs, `SystemAudioState` (`audio/SystemAudioState.kt:48-55`) and `AppVisibility` (`audio/AppVisibility.kt:16-19`), are read-only seams that feed its gates; they play nothing. `grep -rl 'SoundPool\|SoundPlayer\|android\.media' domain` finds nothing. Haptics stay outside `audio/` (`ui/game/JumpHapticEffect.kt`, `ui/game/WrongTap.kt`), as the tech-lead ruled. |
| R2 | Preload | PASS | Unchanged: `preload()` loads every `SoundCue` entry once (`SoundPoolPlayer.kt:182-188`), called from `App.kt:102` on the application scope (`Dispatchers.Default`, `di/DataModule.kt:99`). |
| R3 | Triggers on state changes | PASS | Every cue keys on UI state: `animating.id` (`audio/JumpSoundEffect.kt:75-83`), `selectionTick`, `deselectionTick`, `undoTick`, `rejectedTapTick`, `hintSource`, `status` (`ui/game/GameScreen.kt:309-418`), the earned stars (`ui/results/ResultsCelebration.kt:99-114`), and user taps for the button and Settings cues. The integration adds `rejectedTapCell` beside the ordinal (`ui/game/GameViewModel.kt:823-827`) and leaves `InvalidTapSound` (`GameScreen.kt:356-361`) as it was, as brief item 16 requires. See F-1 for the replay on rotation. |
| R4 | Mute honoured | PASS | `soundOn` collector stops in-flight streams (`SoundPoolPlayer.kt:156-161`) and gates every cue (`:220`). The `SoundEnabled` bypass skips this gate only and keeps the speaker and background gates (`:210-213`), per design §1.7. Tests: `withSoundOffNothingPlaysAtAll`, `switchingSoundOffStopsTheStreamsAlreadyInFlight`, `theEnableConfirmationPopIsMutedOnVibrateThroughThePhoneSpeaker`. |
| R5 | Volume honoured | PASS | `USAGE_GAME` / `CONTENT_TYPE_SONIFICATION` from one definition (`audio/GameAudioAttributes.kt:15-22`), so the media stream and its slider; `volumeControlStream = STREAM_MUSIC` (`MainActivity.kt:142`, V1) keeps the volume keys off the ringer. No `requestAudioFocus`, `AudioFocusRequest`, `setStreamVolume` or `adjustStreamVolume` in `app/src/main` (grep: 0). |
| R6 | System silent mode honoured | PASS (code); device D1–D6, D8–D10 open | Q-010 option 2: `SystemAudioSnapshot.mutesGameSound` = ringer not Normal AND route BuiltIn (`audio/SystemAudioState.kt:29-30`). Ringer and route are read event-driven (`audio/AndroidSystemAudioState.kt:65-102`); `BECOMING_NOISY` reads as the phone at once (`:108`); an unknown output counts as the phone (`audio/AudioRouteClassifier.kt:31-40`); a failing reader fails open (`SoundPoolPlayer.kt:165-173`). Each gate has its own collector, never a `combine`, so a silent reader cannot disable the in-app mute (`aSystemReaderThatNeverEmitsLeavesTheInAppMuteWorking`). 12 gate tests in `SoundPoolPlayerTest`. |
| R7 | Background | PASS (code); device D7 open | `ProcessAppVisibility` maps the process lifecycle to `isAtLeast(STARTED)` (`audio/AppVisibility.kt:36-47`); the player stops streams and refuses cues while backgrounded (`SoundPoolPlayer.kt:174-179`, `:211`, `:220`), which also stops the star pops' delayed coroutine from sounding. It reads `ProcessLifecycleOwner` directly, not `AppForegroundTracker`, per design §2.1. |
| R8 | No main-thread audio work beyond the play call | PASS | The play path is two map lookups, three volatile reads and `pool.play` (`SoundPoolPlayer.kt:216-239`). Platform reads (`getRingerMode`, `getDevices`, `getAudioDevicesForAttributes`) and listener registration run in the collector's coroutine on `Dispatchers.Default`; the receiver and device callback only `trySend` a trigger (`AndroidSystemAudioState.kt:73-87`). `silenceNow()` runs on the collectors' dispatcher. See F-3 for the pool's construction. |
| R9 | Every cue paired with a visual or haptic | PASS | All twelve cues have a visual partner (spec, Cue table). The gap the spec recorded for `InvalidTap` is closed on the gameplay board: shake or still mark on the tapped cell (`ui/components/BoardCanvas.kt:483`, `ui/components/WrongTapFeedback.kt:151-199`), redirect (`ui/game/WrongTap.kt:68-78`), `Reject` haptic on API 30+ (`GameScreen.kt:376-386`, `WrongTap.kt:92-95`), polite announcement (`GameScreen.kt:164`, `:237`). None depends on Sound or the ringer (WT-13 tests `withSoundOffEachWrongTapStillShakesItsCellAndIsStillFelt`, `withSoundOnEachWrongTapStillPlaysExactlyOneInvalidTapCue`). The tutorial plays no wrong-tap sound, so its redirect-only answer needs no pairing. |
| R10 | Every cue the spec names is wired to its event | PASS | `Jump`/`PegRemove` (`JumpSoundEffect.kt:77-80`, gameplay `GameScreen.kt:167`, tutorial `TutorialScreen.kt:115`); `Select`, `Deselect`, `Undo`, `InvalidTap`, `Hint`, `Win` (`GameScreen.kt:312`, `:328`, `:344`, `:359`, `:399`, `:416`); `StarPop` (`ResultsCelebration.kt:111`); `ButtonForward`/`ButtonDismiss` at every site the spec lists, tutorial and Settings "How to play" included; `SoundEnabled` (`ui/settings/SettingsViewModel.kt:137`). The silent events the spec lists stay silent. |
| R11 | No asset ships unused; nothing plays the spec does not name | PASS | `res/raw` holds exactly the twelve `sfx_*.wav` the twelve `SoundCue` entries reference (`audio/SoundCue.kt:24-78`); no asset changed in the range. `AudioAssetFormatTest.resRawHoldsExactlyOneFilePerCue` now pins the count. |
| R12 | Formats, sizes, levels | PASS | ffprobe: all twelve `pcm_s16le`, 44,100 Hz, 1 channel. Sizes 5,962–145,574 B. Peaks −12.50 to −1.00 dBFS (spec, Measurements; now also `AudioAssetFormatTest.noSoundPeaksAboveMinusOneDbfs`). sha256 12/12 match CREDITS.md. `invalid.json` and `hint.json` re-render byte-identically with the plugin's `sfx-synth.sh`. |
| R13 | Mix values | PASS | `VOLUME_JUMP` 1.0 (`audio/SoundParams.kt:134`), landed with the re-levelled asset (2e82c3e after d1c6e9b), so the jump + removal layer peaks at −1.10 dBFS. `VOLUME_DESELECT` = `VOLUME_SELECT` × `DESELECT_ATTENUATION` = 0.6 × 0.3162 = 0.18972 (`:140`, `:149`, `:176`). The other nine volumes equal the spec's Volume column. The KDoc quotes the corrected 0.73 dB RMS pair (`:161-170`). |

## Findings

**F-1 — Minor (pre-existing, not a regression) — `app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt:356-361`.**
After a configuration change (a rotation: `AndroidManifest.xml:32` locks no orientation),
`InvalidTapSound` re-fires on the recreated screen's first composition, because its `> 0` guard
passes for the ordinal the ViewModel carried. The wrong tap's partners deliberately do not
replay: the board answer remembers its first ordinal (`ui/components/WrongTapFeedback.kt:161-171`),
and the haptic and the announcement go through `NewOrdinalEffect` (`ui/game/NewOrdinalEffect.kt:23-33`).
So after a rotation the soft tok plays with no event, no shake, no buzz and no announcement: an
unpaired cue (rules/audio.md, Accessibility) for an event that did not happen (Cues). The same
idiom replays `Select`, `Deselect`, `Undo` and `Hint` once (`GameScreen.kt:309-346`, `:396-401`),
whose partners are persistent states and so still visible. The android-game-dev recorded it
(open issue 1), and brief item 16 required the sound unchanged, so it does not block this
range. Recommendation: the tech-lead decides a follow-up that keys the tick cues through
`NewOrdinalEffect`, with one rotation test per cue; `InvalidTap` first.

**F-2 — Minor (copy and KDoc, not playback) — `app/src/main/res/values/strings.xml:160`; `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt:68-70`.**
The Haptics switch's supporting line still reads "Vibrate lightly on each jump", but the switch
now also gates the wrong tap's `Reject` buzz (D-06b; the amendment's §16 widens SET-02 to it).
The Settings KDoc still says Sound "persists the flag only this phase — there is no audio engine
yet", which phase 07 made untrue. The copy is the ux-designer's (fold it in with D-06's
renumbering and O4); the KDoc line is the android-game-dev's.

**F-3 — Info (pre-existing, outside the range) — `app/src/main/java/com/app/pegsolitaire/audio/SoundPoolPlayer.kt:97-103`.**
The `SoundPool` is built in the player's constructor, which Hilt runs on the main thread when
`App` is injected. That is a native allocation, not a decode (decodes run on the application
scope, `App.kt:102`), so it is within the rule's intent; noted only so the qa-engineer's
cold-start trace can confirm it costs nothing visible. No action asked.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/docs/audio-spec.md` — updated to the code at 8663710:
  - the six places the tech-lead listed: InvalidTap's Haptic and Visual columns; the tutorial jump ("no haptic here" → the shared `JumpHapticEffect` under `HapticsSetting`); Settings › System silent mode; Settings › Background; Settings › Haptics (two haptics, one gate); P8 (`ProcessLifecycleOwner` through `AppVisibility`, not `AppForegroundTracker`);
  - what went stale with them: the header's "what describes which commit"; the Jump row's volume (0.9 → 1.0) and haptic reference; the Settings › Mute and Volume bullets (gate lines, `GameAudioAttributes`, V1, the corrected Sound copy); the tutorial section's merge status and its wrong-tap row (redirect only, D-01j(2)); Accessibility (P1 closed); the Mix paragraph and the Jump row of the Mix table (−18.4 at 1.0); the Measurements note on which `VOLUME_JUMP` that table applies; the P1, P3, P4a, P5, P8 and P9 status lines; the code line references in the Cue table, which had described develop at 3f2af90.
  - Unchanged: the Measurements figures, Assets, Sourcing, every level decision, and P2, P4b, P4c, P6, P7.
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/docs/audio/CREDITS.md` — prose about the credits test brought up to the rewritten `AudioCreditsTest` and the new `AudioAssetFormatTest` (44b14bb, 6b4eb08): a note under the current state, the Files table's role and the header names the test reads, the hash gate, the RMS-pair KDoc (now done), "What the conversion left for others", the shipped-pair paragraph, and Verification › "Completeness is enforced". The `## Files` table, every sha256 and every asset are untouched.
- `/Users/ermanergoz/Desktop/PegSolitarie-audio/.planning/reports/audio-review/sound-designer-integration.md` — this report.

## Checks
- `git diff --stat 62c774d..8663710` — 69 files; no change under `app/src/main/res/raw/` or `audio/recipes/`.
- `ffprobe -v error -show_entries stream=codec_name,channels,sample_rate` over `app/src/main/res/raw/*.wav` — pass (12 × `pcm_s16le,44100,1`).
- `shasum -a 256 app/src/main/res/raw/*.wav`, each hash matched against its CREDITS.md row, before and after my edits — pass (12/12 both times).
- `/Users/ermanergoz/Desktop/ergoz-soft/scripts/sfx-synth.sh audio/recipes/{invalid,hint}.json <scratchpad>/sfx_{invalid,hint}.wav` then `cmp` against `res/raw` — pass (both byte-identical).
- `grep -rl 'SoundPool\|SoundPlayer\|android\.media' domain` — pass (no match).
- `grep -rn 'requestAudioFocus\|AudioFocusRequest\|setStreamVolume\|adjustStreamVolume' app/src/main` — pass (0).
- `grep` of every `SoundCue.`, `playJump`, `playStarPop`, `playSoundEnabledConfirmation` call site in `app/src/main/java` — pass (each of the 12 cues wired; matches the spec's Cue table).
- `git diff --check -- docs/` — pass (no whitespace errors).
- `git diff -- docs/audio/CREDITS.md | grep '^[-+]|'` — pass (no table line changed).
- Pipe count per row of the spec's Cue table (lines 26-50) and tutorial table — pass (11 and 5 on every row).
- `./gradlew check` — **not run**, as dispatched: the code-auditor holds the worktree's Gradle. My edits are docs only, but `app/build.gradle.kts` declares `docs/audio/CREDITS.md` as an input of every unit-test task (6b4eb08), so the next Check re-runs `AudioCreditsTest` against it; the Files table it parses is unchanged.

## Open issues / risks
1. Device checks are the qa-engineer's and are all still open: D1–D10 (silent mode, background, V1), WT-12, WT-14, WT-18, WT-15, the P4c `Deselect` listen and the P9 recheck.
2. F-1 needs a tech-lead decision on a follow-up; F-2 needs the ux-designer (copy) and the android-game-dev (KDoc).
3. Accepted design limits, recorded here so QA does not file them as bugs: a route change with no device change (the output switcher) is seen only at the next ringer or device event (O1); a tail can ring about 0.7 s after leaving the app; an output not on the allowlist (for example `TYPE_HDMI_EARC` below API 33) counts as the phone and mutes on Silent/Vibrate.
4. P6 is unchanged by this review: the research-analyst's per-file verdicts and `THIRD_PARTY.md` on develop still gate the release.
5. The doc edits are uncommitted, as dispatched.

## Handoff
Next: producer — commit the two doc edits and this report on `feature/audio-review`; route F-1 to the tech-lead and F-2 to the ux-designer and the android-game-dev.
Next: qa-engineer — the device checks in Open issues 1 on a physical phone with wired and Bluetooth headphones; listen that every cue fires on its event and that the soft tok sounds with each wrong tap and nowhere else.
Next: android-game-dev — no change asked by this review for the range; the F-2 KDoc line when the producer schedules it.

VERDICT: PASS
