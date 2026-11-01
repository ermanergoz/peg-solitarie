# ux-designer report — candy-backdrop: B-11a, opening the scene after Home's entrance (J-4a proposal)
Date: 2026-11-01
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). Because the product snapshot (0.6.2) is older than the plugin (1.0.0), I read the plugin copies of these rules in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch. I also read the product's `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist. Then ui-ux-pro-max, android-accessibility and compose-skill.
Ran in parallel with: tech-lead J-4b ruling (`tech-lead-j4b-ruling.md`, `3141dde`). The producer relayed it mid-task, and I read it in full and folded it in before finishing.
Ran on: claude-opus-5-5 at effort high (Q-019; the agent file pins fable)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **Verdict: ACCEPT.** The scene (waves and lights) now opens once Home's entrance cascade has ended, 440 ms after the first frame at animator scale 1, and fades in once per host start.
- **The fade:** 700 ms, linear. That is the sky cross-fade's own spec.
  - It lives in `BACKDROP_SCENE_FADE_MS`, defined as `ClayAnim.NAV_TRANSITION_MS` (J-4b A2).
  - It is chosen for J-4b's 120–290 ms bench stall: the worst first step is ≈ 0.44, and the rest stays a gradual ramp.
- **The J-4b points the producer raised are all answered** in B-11b:
  - **A5:** the replay on every recreation is accepted, and `rememberSaveable` is rejected.
  - **A1:** the open follows the scaled time.
  - **A3:** with animations off, the scene draws at alpha 1 on frame 2 with no fade.
  - **A4:** the `ModulateAlpha` look is accepted.
- **Unchanged:** frame 1 and the reduced-motion path (frame 2, no delay, no fade). 15e is not owed again.
- **Where it is recorded:** a new ruling, B-11b, with a pointer from B-11a. The house style never rewrites a ruling in place. §3.1, §4.4, §6.1, §6.4, §7, BD-23 and §12 are amended to match.
- **The device pass line** is BD-23 (d): a screen-recording read of the fade with numeric bounds.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md`, amended:
  - the front-matter `revised` line;
  - §1's status-table row for B-11;
  - a pointer at the end of B-11a;
  - the new **B-11b**;
  - §3.1's L2 row;
  - §4.4: a `BACKDROP_SCENE_OPEN_MS` row, and a note that the fade reuses `NAV_TRANSITION_MS`;
  - §6.1: the first-appearance row and the theme-change row;
  - §6.4: a third bullet;
  - §7: the battery-saver sentence, and an "amended" paragraph under the deferral;
  - BD-23: (a) amended, (c) annotated, (d) and (e) added;
  - §12: the `BackdropFirstFrameTest` row amended, and a `BackdropSceneOpenTest` row added.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-b11a.md`: this report.

## The decisions, in short
1. **Timing.** `BACKDROP_SCENE_OPEN_MS` = `ENTRANCE_MS + 5 × ENTRANCE_STAGGER_MS`, which is 440 ms at scale 1.
   - It is derived, never a literal.
   - It is measured in **animator-scaled** time, so it is 880 ms at 2×.
   - The clock, the battery-saver read and the sheen loop all start at the open.
2. **The fade.** It runs from 0 to 1 over `NAV_TRANSITION_MS` (700 ms) with linear easing, and scales with the animator scale.
   - I reuse the cross-fade's spec rather than the entrance's 240 ms decelerate curve. A short decelerating fade over the lower half of the screen reads as a flash, and a 150 ms stall would swallow it.
   - At 700 ms linear, the same stall steps the fade from about 2% to about 24% in one frame, and the rest of the fade stays gradual.
3. **What the player sees first.** Home's cascade over the Landscape sky alone: no wave, no light, no glint.
4. **Later visits to Home in the same host.** Only B-13a's density cross-fade: no delay and no second fade.
   - A host start is a cold start, a start after process death, or an activity recreated by a configuration change. In each case Home's entrance replays too.
   - A host restored onto the game screen gets the same open and fade. Accepted.
5. **Reduced motion.** B-11a as written: frame 2, at rest, no delay, no fade.
   - **Battery saver with animations on:** the same open and fade, at the rest pose. That is about 42 frames once per host start.
6. **`ModulateAlpha`.** Accepted. For 700 ms the back wave shows faintly through the front wave. A group fade would allocate a full-window layer on the very frame this change makes cheap.
7. **Accepted residuals:**
   - a tap made during the fade may be delayed once per host start (delayed, never lost);
   - a Continue chip whose data arrives after frame 1 may still be entering at the open.

## Checks
- **Spec consistency, by reading.** I re-read B-11, B-11a, B-13a, B-22/B-22a, B-24, §3.4, §6.1–§6.4, §7, BD-16, BD-21 and BD-23 against B-11b.
  - Pass: nothing still says "frame 2" for the animations-on path.
  - Pass: BD-21 is untouched, because the sky does not fade.
  - Pass: BD-16 (b)'s "only translation and alpha vary" admits the fade.
- **Constants, against source.** I read `ClayAnim.kt`, `Clay.kt` (`clayEntrance`), `BackdropMotion.kt`, `SceneBackdrop.kt` and `BackdropSceneReady.kt`.
  - Pass: `ENTRANCE_MS` 240, `ENTRANCE_STAGGER_MS` 40 and `NAV_TRANSITION_MS` 700.
  - Pass: the cross-fade is `tween(NAV_TRANSITION_MS, easing = LinearEasing)`.
  - Pass: `clayEntrance` runs on an `Animatable`, whose time follows the animator scale.
- **Step-bound arithmetic for BD-23 (d)(3), redone with J-4b's 120–290 ms stall** — pass. The first visible frame is at about 0.03 alpha. After the worst stall (290 + 16 ms), the next frame lands at ≈ 0.47, a step of ≈ 0.44, under the 0.50 bound.
  - A 240 ms fade fails the bound at the 120 ms low end (≈ 0.57), as intended, and so does any fade under ≈ 300 ms at the high end.
  - (2) still holds: after that step, ≈ 370 ms of ramp remains, so the visible rise spans ≈ 690 ms against ≥ 500.
  - (4) still holds: the scene is fully in by ≈ 1.5 s, against ≤ 2.0 s.
- **J-4b cross-check** — pass. A1 (scaled open), A2 (named `HOME_LAST_ENTRANCE_ORDER`, and `BACKDROP_SCENE_FADE_MS` defined as the `ClayAnim` token), A3 (alpha 1 on the first frame with animations off, and outside the host), A4 and A5 are each stated in B-11b and §4.4. BD-23 (c) now points to J-4b §5's `sleep 2` and RT_late.
- **`Edit` on `docs/ux/candy-backdrop.md`, 23 edits** — pass; each matched exactly once.
- **`./gradlew check`** — not run. This role has no Bash, and this is a document-only change.
- **Device** — not run, for the same reason. BD-23 (d) is QA's.

## Open issues / risks
1. **J-4b and B-11b agree.** On every point (A1–A5), the two rulings agree. The one place I first differed was the token: I had written "no `BACKDROP_SCENE_FADE_MS`". I now take J-4b A2's form, a constant defined as `NAV_TRANSITION_MS`, which cannot drift.
2. **The fade's first step on the bench.** At the worst 290 ms stall it is ≈ 0.44: the screen freezes once, then jumps to about half. I accept this rather than a slower fade, because the bench is the emulator's GL translation and phone frame timing is on the 3.0.1 list. If phone numbers show it is worse, the next step is a longer fade, not a new curve.
3. **BD-23 (c) stays open until J-5** (J-4b §5). The moved draw is measured there as RT_late and reported to the founder as a number.
4. **Battery saver.** B-11b lets the open's one-shot fade run under battery saver (about 42 frames once per host start), where §6.3 item 5 otherwise asks for no frames. The tech-lead may overrule it on battery grounds. If so, the fallback is an instant open at 440 ms, not frame 2.
5. **Phone cost is still unmeasured.** BD-23 (d)'s bounds are set from the emulator's numbers. A physical phone should pass them more easily.
6. **Not edited:** `docs/ux-spec.md` and `docs/ui/DESIGN-SYSTEM.md`, which are owed after the merge (§14 item 9). No string or asset changes, so THIRD_PARTY.md and BD-04 are unaffected.
7. **No founder question.** This is not art direction between equals. The fade's value follows from an existing token and the stall arithmetic. Because the verdict is ACCEPT, J-4c's founder question does not apply to the scene's share. It may still apply to the finishes' share (J-4a Open issue 1).

## Handoff
Next: producer — commit `docs/ux/candy-backdrop.md` and this report on `feature/candy-backdrop`. The tech-lead's condition is met (the visible change is accepted, A5 included), so J-4c does not apply now. J-4b may be dispatched.
Next: android-game-dev — build J-4b to the tech-lead's A1–A6 and R-1, and to B-11b and BD-23 (a) and (e), test-first. The test names are in §12's `BackdropSceneOpenTest` row.
Next: qa-engineer — run BD-23 (d) on the device, light and dark, ≥ 5 cold starts each, and list the sample pixel and recordings by path.
Next: code-auditor — confirm `BackdropFirstFrameTest` and `BackdropSettleTest.start()` were amended, not weakened.

## Ruling
**ACCEPT.** The amended B-11a is B-11b in `docs/ux/candy-backdrop.md` §1.2. The house style adds a lettered ruling and never rewrites one in place. B-11a carries this pointer:

> *Amended 2026-11-01 by B-11b: with animations on, "from frame 2 on" becomes "from the end of Home's entrance, with a fade". Frame 1 is unchanged. With animations off this entry stands as written.*

The B-11b text is quoted verbatim in the final message to the producer, and it is the spec file's §1.2 entry.
