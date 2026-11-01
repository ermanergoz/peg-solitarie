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

**Candy backdrop** (candy sign-off and J-2 ruling; final wording depends on J-4/J-5):
- Cold start on the emulator bench costs **+44.5 ms** (455.0 vs 410.5 ms; null floor 6.5 ms; threshold 100 ms) — PASS; the ≤ 2 s phone bar was not re-measured on a phone.
- **If it ships unfixed:** a one-off hitch of about **+220 ms** in the first five frames after Home's first paint (the backdrop scene's first draw, on the RenderThread), measured on the emulator bench; no steady-state jank. Shipping it unfixed is a founder `Q-<NNN>` under J-4c, not a studio call.

**Evidence scope (whole release)**
- **Emulator-only evidence:** no physical device was available (founder directive 2026-10-31). Phone-only checks are unverified: tutorial step lines on a phone, L1-2, cold start ≤ 2 s on a phone, audio speaker-mute and wired-headset branches (Q-025, if not answered with option 1), CV-AC14 if not run.
- **Reduced QA** (directive 2): device re-runs checked only fixed defects plus one smoke pass; candy rows skipped are filed for 3.0.1.
- **Staffing:** every Fable-pinned review since 2026-10-24 ran on Opus (Q-019); a superior-model re-audit was recommended (e.g. before the first script-route upload).
- Audio blocked items (60 fps recording, P4c listen) if still open at the cut.

## Checklist and store state (filled at 4.8)
- Checklist: `<n ticked / n>`; founder items F-1..F-14 status from the operator checklist.
- Store: en-US / tr-TR texts within limits; TalkBack line added or not; candy lines kept or deleted (2.4a).
