# android-game-dev report — reduced-motion-live-gate, Finding 1
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (`45f84ec`)
Skills loaded: ergoz-soft:standards (plugin 1.0.0), compose-skill, superpowers:test-driven-development
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 1 (one stop under F1.6, then the tech-lead's F1 amendment, option 1)
Lessons applied: none

> Producer note: returned as text; condensed by the producer.

## Summary
- `45f84ec fix(motion): let the clock settle the tutorial's completion star` — `CompletionStar` keyed on `Unit`, always `animateTo(SETTLED_SCALE, ClayAnim.overshootSpring())`, the `if (animationsEnabled)` guard removed, FA.3's KDoc verbatim; starting size still from the gate.
- First pass stopped (F1.6): "next frame" fails because animation-core 1.11.3 `SuspendAnimation.kt:279-281` reads the scale before each frame await; the amendment asserts "within two frames".
- New test `TutorialCompletionStarLiveMotionTest.aStarCaughtMidPopByAnimationsTurningOffSettlesWholeWithinTwoFrames` (v1 rule + `SystemMotionScale`, `@GraphicsMode(NATIVE)`, mdpi; real tutorial play, re-shown for a fresh pop, switch 4 frames in).

## Red / green (star px in a 96×96 region, density 1)
| Reading | Head `a620e04` | `45f84ec` |
|---|---|---|
| Settled reference | 639 | 639 |
| 4 frames in | 68 | 68 |
| 1st frame after switch (not asserted) | 165 | 165 |
| **2nd frame after switch** | **165 — fails "expected 639 but was 165"** | **639** |
| 90 frames later | 165 (stuck ≈0.51×) | 639 |

## Checks
- `git show --stat 45f84ec` → the two files only (+206/−3); single Fable trailer.
- `ui.onboarding.*` + ktlint/detekt — pass, 39 tests.
- `./scripts/check.sh --max-workers=4` — pass, 11m 9s: 1,576 tests (`:app` 1,225), 0 failures; lint identical; no main `w:`; +1 v1 test-compile deprecation (house rule, §9.8 rule 2 exception).

## Handoff
code-auditor: short read of `45f84ec` against F1.2/FA.2/FA.3 with the mutant (gate key restored). qa-engineer: device session (§3, §6, §8). Fresh tech-lead: sign-off.
