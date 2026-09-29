# tech-lead report — audio-review (silent-mode design and rulings)
Date: 2026-09-29
Branch: feature/audio-review (825dd2a..this commit)
Skills loaded: ergoz-soft:standards (role: tech-lead; game; android-compose-game), android-architecture, android-gradle-logic. Rule files read, from the v0.6.2 snapshot (the same version as plugin 0.6.2): studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, stacks.md, testing.md, audio.md, lessons.md. Also the plugin's dispatch.md.
Ran in parallel with: in this same worktree, the sound-designer's asset and credits work, the research-analyst's licence verdicts and the ux-designer's invalid-tap feedback
Ran on: opus (claude-opus-5-5) at the harness's effort. This is a **staffing deviation**; see below.
Fan-out: none
Loops: 0
Lessons applied: none (the v0.6.2 snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Staffing deviation
`agents/tech-lead.md` pins `model: fable` at `effort: xhigh`. The Fable allowance is exhausted, and
the founder's Q-011 answer (`.planning/questions/FOUNDER.md`, answered 2026-09-29, option 2) runs
four audio follow-up steps on Opus; this is one of them. It is not a standing policy change.

Two consequences follow from rules/studio.md, Model and effort:
- the design was written on Opus, so once Fable returns a fresh Fable tech-lead may re-check it
  (design O6);
- the developer's code built from it must be reviewed on Fable, and Q-011 does not cover that
  review (design O5).

## Summary
`docs/tech/audio-silent-mode.md` is the design the android-game-dev builds from. It is grounded in
`audio/SoundPoolPlayer.kt` (the playback owner the review names) and in all of its callers.

- **Silent/Vibrate:** game sound mutes only when the ringer is Silent or Vibrate *and* the sound
  would leave the phone's own speaker. The system is read through an injected `SystemAudioState`
  seam, event-driven, and entering the muted state stops streams already in flight.
- **Rulings:** P8 approved; P4a approved with conditions; a P5 test contract written; the nit
  approved. One item is added: V1, the volume keys.
- **Phase 15:** no file this design touches is changed by phase 15, and the build-after-phase-15
  plan is confirmed.
- **Cost:** about 2 developer-days plus a 2–3 h device session.

## Outputs
- `docs/tech/audio-silent-mode.md` — the design (commit 825dd2a). It has seven sections:
  - §0: decisions at a glance;
  - §1: the Silent/Vibrate gate — behaviour table, ringer and route APIs across API 24–36, the
    event-driven re-evaluation and why play-time was rejected, where the code lives, signatures,
    how it interacts with mute, volume, the SoundEnabled pop, focus and haptics, threading, failure
    posture, tests (pure, Robolectric, device D1–D10), cost, alternatives;
  - §2: rulings on P8, P4a, P5 and the nit, plus V1;
  - §3: sequencing against phase 15;
  - §4: dependencies and licences (none new, no copyleft);
  - §5: build order;
  - §6: risks and open items O1–O8.
- `.planning/reports/audio-review/tech-lead.md` — this report.

## Rulings (short form; the reasons are in the design)
- **Silent/Vibrate (Q-010 option 2):**
  - **How it reads the system:** `getRingerMode()` and `RINGER_MODE_CHANGED_ACTION` for the ringer.
    For the route, `getAudioDevicesForAttributes(the pool's own attributes)` on API 33+, and an
    allowlist of external output types from `getDevices(OUTPUTS)` on API 24–32.
  - **When it re-reads:** on the ringer broadcast, on `ACTION_AUDIO_BECOMING_NOISY` (read as the
    phone at once) and on `AudioDeviceCallback`. Reads run off the main thread; `play()` reads one
    volatile flag. Entering the muted state stops in-flight streams.
  - **What it touches:** the `SoundPlayer` interface, every caller and `:domain` are unchanged. No
    focus, no volume writes, no permission, no new dependency.
  - **The SoundEnabled pop** keeps bypassing only the in-app mute, and honours the new gate.
- **P8:** approved. On process `ON_STOP` (`ProcessLifecycleOwner.currentStateFlow`, not
  `AppForegroundTracker`), streams stop and new cues are blocked until foreground. Blocking matters
  because the star-pop coroutine keeps running after the app leaves.
- **P4a:** `VOLUME_JUMP` 0.9 → 1.0 approved, on this branch before it merges, because the
  re-levelled asset is already here (d1c6e9b). Three KDoc edits go with it, and
  `theWinStingerIsTheLoudestCue` is renamed to `noCueIsMixedAboveTheWinStinger`, since 1.0 ties the
  win.
- **P5:** the credits test covers every file in `res/raw` and reads the `## Files` table by header
  name.
  - **Sourced rows:** an https source and a licence from an allowlist (today `CC0 1.0`).
  - **In-house rows:** the source is `audio/recipes/<stem>.json`, and that file exists.
  - **Every row:** matched exactly by file name; no orphan recipes; a sha256 check if the column
    exists.
  - **New `AudioAssetFormatTest`:** WAV PCM mono 16-bit 44.1 kHz with the chunks walked, at most
    200 KB, a peak at most −1 dBFS, and one file per cue.
  - **Not tested here:** the re-render (it needs `sfx-synth.sh`, outside the repo).
- **Nit:** approved as comment only. The three star pops ring together from 1250 to about 2830 ms
  (animations off: 1500–2870 ms), not 1250–1900 ms, and the four-of-eight-voices conclusion stands.
- **V1 (added):** `volumeControlStream = STREAM_MUSIC` in `MainActivity`. Otherwise the volume keys
  step the ringer (Android 7–8, and OEM defaults such as Samsung's), and the new gate would then
  mute the game.

## Checks
- `git diff develop...feature/phase-15-ux-refresh --stat=200 -- <the 15 paths in design §3>` — pass (empty output: phase 15 changes none of the files this design edits)
- `git diff develop...feature/phase-15-ux-refresh --stat=200 -- GameScreen.kt GameUiState.kt BoardCanvas.kt GameViewModel.kt ui/onboarding JumpSoundEffect.kt ui/settings FOUNDER.md STATUS.md docs/ux-spec.md` — 13 neighbouring files changed; none is edited by this design (recorded in §3)
- `javap -cp <shadows-framework-4.16.jar> org.robolectric.shadows.ShadowAudioManager` and `… AudioDeviceInfoBuilder` — pass (`setRingerMode`, `getAudioDevicesForAttributes` / `setAudioDevicesForAttributes`, `addOutputDevice` / `removeOutputDevice`, `registerAudioDeviceCallback` and `getDevices` exist, so every Robolectric test in §1.10 is buildable)
- `ls ~/.m2/repository/org/robolectric/android-all-instrumented` — only the Android 14 image (plus 9 and 4.4 at i4) is cached, hence the "SDK 34 only" rule in §1.10
- `grep -rn "audioFocusMarkers" build-logic/src/main/kotlin/standards.gradle.kts` — the gate bans only `requestAudioFocus` / `abandonAudioFocusRequest`; reading `AudioManager` does not trip it
- `xxd -l 48 app/src/main/res/raw/sfx_{jump,invalid,star_pop}.wav` — PCM, 1 ch, 44,100 Hz, 16-bit, clean 44-byte headers; the star pop is 145,574 B
- `grep -rn "\.ogg" app/src build-logic` — 3 KDoc hits (`SoundCue.kt:44`, `SoundParams.kt:137,155`), listed in the P5 sweep
- `git diff --check -- docs/tech/audio-silent-mode.md` — pass
- `./gradlew --offline check` — **fail, and not run by me.** Another role's run of the same command in this worktree was in progress, and a later one started after it, so a run of my own would have competed with them. The finished run's log (git-ignored scratch, not a studio log) ends `BUILD FAILED in 11m 19s`, `EXIT=1`, with two failures:
  - `:app:testDebugUnitTest`: `945 tests completed, 2 failed`, both in `AudioCreditsTest` (`everyCommittedSoundHasACreditsRow` and `everyCreditsRowNamesASourceAndLicence`, at `AudioCreditsTest.kt:35,44`, `isNotEmpty`). This is the break predicted in design §2.3: since af2b554 and d1c6e9b, `res/raw` has no `.ogg` files.
  - `:domain:depthSweep`: `java.io.EOFException`. Nothing on this branch touches `:domain`, and the run shared the machine with other Gradle builds, so this looks environmental. That is **unverified**; the next clean `check` confirms or refutes it.

  My change is documentation only.

## Open issues / risks
- **`:domain:depthSweep` failed with an `EOFException`** in the same concurrent run. It is probably
  environmental, but that is unverified. The producer should have the next single `check` on this
  branch confirm it.
- **`feature/audio-review` is red now** (the AudioCreditsTest failures above), and the jump plays
  about 1 dB under the founder's tuning. Both clear with design §5 steps 1–2, which must land on this
  branch before it merges to develop.
- **The brief's "min-SDK 26" is wrong for this product: `minSdk` is 24**
  (`build-logic/src/main/kotlin/android-app.gradle.kts:29`). The design covers 24–36, and nothing in
  it depends on the open 24-vs-26 decision (`docs/tech-design.md` §9.1 on the phase-14 docs branch).
- **Device dependence:** D4, D5 and D9 need a physical phone with wired and Bluetooth headphones,
  because an emulator cannot plug headphones (O3).
- **Accepted limitations:**
  - a route change with no device change (the output switcher) goes unseen until the next event
    (O1);
  - on API 24–32, a connected Bluetooth sink reads as external even when media has been moved to the
    phone (O2).
- **A UX gap for the ux-designer:** while muted by the ringer, Settings still shows Sound "on" (O4).
- **Merge conflict:** merging develop into this branch after phase 15 will conflict in
  `.planning/questions/FOUNDER.md`, where both sides add questions at the top. That file is the
  producer's.
- **Founder questions:** none. The answer to Q-010 already says "mute on the phone speaker only",
  which settles Bluetooth speakers and cars. The Fable review of the build (O5) is the producer's to
  raise if Fable is still exhausted then.

## Handoff
Next: producer — when phase 15 merges to develop, merge develop into `feature/audio-review`
(resolving the FOUNDER.md conflict), then dispatch the android-game-dev with
`docs/tech/audio-silent-mode.md` §5. The branch cannot merge to develop before §5 steps 1–2 land.
Route O4 to the ux-designer. Raise O5 (Fable for the code-auditor and qa-engineer review) if Fable
is still out.
Next: android-game-dev — build §5 test-first, in this order: P5, P4a + nit, the gate, P8, V1. The
"must not" list is in §5.
Next: sound-designer — the Files table in `docs/audio/CREDITS.md` must meet the §2.3 contract:
- header names, in any column order;
- in-house rows with source `audio/recipes/<stem>.json`;
- optional `author` and `sha256` columns, which the test will check if present.

After the build: update the spec's Settings "System silent mode" line, review V1 and the integration
diff.
Next: qa-engineer — device checks D1–D10 (design §1.10) on a physical phone with wired and
Bluetooth headphones, after the build.
Next: code-auditor — review the build against the design. Note O8: `InlinedApi` warnings are
expected and must not be suppressed.
