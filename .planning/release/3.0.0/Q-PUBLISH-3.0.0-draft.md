# Q-PUBLISH-3.0.0 — draft summary lines (release-manager, 2026-11-01)

Draft only. The producer puts the final one-screen summary to the founder at Gate 2 (checklist 4.8),
after the bundle exists. Lines below are collected from the sign-offs; each names its source.

## Version, artifact, command (filled at 4.6 / 4.8)
- Version 3.0.0, versionCode 30000, tag `v3.0.0`. AAB sha256: `<4.6>`.
- Exact command (T-6/T-11), from the `feature/publish-3.0.0-done` checkout, after the approval file:
  `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh` — or the Console route
  (operator checklist Step 8 item 1), whichever 1.7 records. The script is unverified until its first upload.

## Known limitations and deviations shipping in 3.0.0
**TalkBack block** (talkback-board sign-off §4):
- **D-TBb floor:** a jump taken through TalkBack's actions menu speaks its line, naming the destination; when the menu closes, focus stays on the (now empty) source cell, not the moved peg — the player swipes to it. The Actions reading control and double-tap land on the destination. In the tutorial the menu route reaches the new board stop.
- **D-TBd:** after a banner Restart is confirmed, "Board restarted. N pegs left." is spoken; focus then rests on "Navigate back", not on the board.
- **D-TBe:** in the tutorial, the new step's board stop is spoken first, then the step line. The jump that clears steps 1 and 2 speaks no line of its own. Opening speaks the caption alone; "Step 1 of 3" comes from the pips. At a very slow speech rate the coach's hint can replace a step line (3.0.1).
- **Cell-size deviation** (D-TBa item 10, tech-design §16 #23): gameplay cells are below the studio's 48 dp touch-target rule on narrow phones (on a 360 dp-wide phone ≈ 42.3 dp on 7-wide boards, 37.0 dp on 8-wide; 48 dp from a 400 dp window); recorded deviation, also TalkBack's focus rectangle.
- **Verification scope:** TalkBack 16 on one API-36 emulator image at the default speech rate. No physical phone; the hardware-keyboard check (L1-2) is untested.

**Other known issues**
- **B-10:** in landscape the board is clipped (3.0.1).
- **§6 notice re-speak:** after a live Remove-animations switch, the hint's "couldn't work it out" notice can be spoken again (tech-design §16 #32, 3.0.1 low).
- **Reduced-motion switch latency:** a system switch reaches the game about 10 s later (platform delay, accepted).
- **Chip room effect (C11 §3):** one animation effect on Home is keyed on the motion gate (`HomeBandColumn.kt:273`); P3 shows it strands nothing; fixed in 3.0.1.

**Candy backdrop** (shipped with the J-4b fix; `candy-backdrop/tech-lead-signoff-final.md` §4, closed by `ux-designer-bd23d-bs.md`; merged `da886ed`). Emulator only; no physical phone was measured:
- **Cold start (TTID): +44.5 ms** against develop on `Pixel_9a_bench` (455.0 vs 410.5 ms; null floor 6.5 ms; line 100 ms) — PASS (`qa-engineer-15e.md`). J-4b did not touch the start path.
- **Settle of the first five frames after Home's first paint: +57.7 ms** against develop (458.2 vs 400.5 ms; null floor 30.2 ms; line 100 ms) — PASS (`qa-engineer-j5.md`). Before the J-4b fix it was +216 to +225 ms.
- **Remaining frame-1 cost** comes from Home's own finishes: RenderThread +68 ms on frame 1 (208.1 vs 139.7 ms), +22 / +11 ms on frames 2–3, and a ≈ 7.5 MB stencil attachment; consistent with the +44.5 ms TTID (3.0.1 item).
- **The moved scene draw:** one stall of about **49 ms** (develop: 33 ms) on a still Home, about 0.6 s after the entrance. **No steady-state jank.**
- **What the player sees:** the waves and lights appear after Home's entrance (440 ms at animator scale 1) and fade in over 700 ms, once per host start (including every configuration-change recreation). A tap during the fade may be delayed once, never lost. With animations off, the scene appears on frame 3 with no fade.
- **Battery saver:** BD-23 (d) PASSES on the existing four runs (all rendered dark: API 36 forces dark in battery saver), item (4) at most 1.288 s after the Challenge tile settles against ≤ 1.56 s; the earlier `bs-light-2` 45 ms "miss" was a line defect (anchor inside the system splash), not a product finding (B-11d).
- **Scope:** one emulator, API 36, Skia (OpenGL) via host-GLES translation, debug APKs. The ≤ 2 s phone start bar (D-03j, PLAT-08) was not re-measured on a phone; a phone GPU with a warm shader cache will likely show smaller first-draw costs, which the bench cannot show either way.

**Evidence scope (whole release)**
- **Emulator-only evidence:** no physical device was available (founder directive 2026-10-31). Phone-only checks are unverified: tutorial step lines on a phone, L1-2, cold start ≤ 2 s on a phone, audio speaker-mute and wired-headset branches (Q-025, if not answered with option 1). CV-AC14 ran on the emulator (D-4 PASS on form cues).
- **Reduced QA** (directive 2): device re-runs checked only fixed defects plus one smoke pass; candy rows skipped are filed for 3.0.1.
- **Staffing:** every Fable-pinned review since 2026-10-24 ran on Opus (Q-019); a superior-model re-audit was recommended (e.g. before the first script-route upload).
- Audio blocked items (60 fps recording, P4c listen) if still open at the cut.

**Release Check (cut, 2026-11-01):** `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` green on `release/3.0.0` (code = `develop` `da886ed`): the whole 10,000-level space fuzzed with 0 failures, 1,937 unit tests, `:domain` line coverage 99.0 %, the four literal gates run.

**Release device pass (2026-11-01, emulator `ergoz_studio`; `release-3.0.0/qa-engineer-device-pass.md`, `tech-lead-device-pass-triage.md`).** The conditional §16 #30 line ("tree-order and speech-output evidence only") is withdrawn: the notice was raised. In its place:
- **TalkBack, hint focus (3.0.1):** after a double-tap on Hint, when the hint needs a search, TalkBack focus can move to the screen's first stop, "Navigate back", instead of staying on Hint (3 of 4 on the emulator). The "couldn't work it out" notice is still spoken once; its order and its Dismiss pass.
- **Unverified, no phone attached:** tutorial step lines on a phone (D-2; emulator device3 evidence stands), hardware keyboard with TalkBack (D-3, L1-2), audio speaker-mute and wired-headset branches (D-9, Q-025), cold start ≤ 2 s on a mid-range phone (D-10; emulator figures only).
- **Not compared:** English-33 landscape against published 2.0 (D-7) — no 2.0 build on this host; the 3.0.0 half shows the known landscape clipping (B-10, 3.0.1).
- **Founder-blocked:** D-1 (palette + TalkBack on the release AAB) waits for 4.3/4.6; D-12 (store copy) after upload.
- **Cold start with Remove animations on (D-15), emulator only:** on `Pixel_9a_bench` (debug build), the median cold start was **465.5 ms** with animations off against **440.5 ms** with them on (+25 ms, line 100 ms), the slowest off **508 ms** (< 2 s), and no frame ≥ 700 ms (no Davey logged in any of 60 launches) — **PASS** on the guarded re-measure's attempt 2 (`qa-engineer-d15-remeasure.md`; attempt 1 VOID by the post-block speed gate, the nap guard never fired). A first probe was voided: the studio Mac's App Nap halved the emulator's speed behind a locked screen, in both conditions. No phone was measured (D-10).
- **First frame with Remove animations on (by design):** Home appears settled on its first frame, so that frame draws all of Home at once. On the emulator it was about 50 ms longer than with animations on (about 300 against 250 ms, debug build, empty shader cache). The whole start is not longer: with animations on, the same drawing work happens just after Home appears. A phone's shader cache and GPU driver should make it smaller. This was not measured on a phone.
- **Store screenshots:** the 9 captures are drafts; the final set is recaptured from the release candidate.

## Checklist and store state (filled at 4.8)
- Checklist: `<n ticked / n>`; founder items F-1..F-14 status from the operator checklist.
- Store: en-US / tr-TR texts within limits; TalkBack line added or not; candy lines kept or deleted (2.4a).
