# code-auditor report — reduced-motion-live-gate
Date: 2026-10-24
Branch: bugfix/reduced-motion-live-gate (7382351..873fb9e; reviewed as `git diff 6aa7624...873fb9e`) (committed by the producer)
Skills loaded: ergoz-soft:standards (plugin 1.0.0 rule files), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh — staffing deviation: pinned fable; Fable limit reached; Q-019 answer 3; author also Opus. Mitigation: own Check and six probes.
Fan-out: none
Loops: 0
Lessons applied: none

> Producer note: returned as text; condensed by the producer with every finding, probe and check kept.

## Summary
The gate reads the composition's `MotionDurationScale`, no Settings read; all 14 call sites in 9 files follow it. `2b98c5c` matches the ruling's §3 table and changes no assertion. The 7c-race guards are live again (two race mutants green on pre-ruling tests, red on head). Tick cues survive a real rotation; the 14-of-63 probe count reproduces. One minor finding, not blocking.

## Findings
1. **Minor (not blocking): `ui/onboarding/TutorialScreen.kt:349-352`** — `CompletionStar` runs `LaunchedEffect(animationsEnabled) { if (animationsEnabled) scale.animateTo(…) }`, the only main-code effect keyed on the gate; switching animations off mid-pop cancels the pop and nothing settles the star. Probe (live `SystemMotionScale`): settled 656 px, mid-pop 76, after the flip + 90 frames 184 (≈0.53× scale), stuck until the screen leaves. Low likelihood (switch during the ~0.7 s spring while frames are drawn; a Settings round-trip pauses the clock and the spring finishes first, by source reading, not probed). Fix: `else scale.snapTo(SETTLED_SCALE)`, red first with a mid-pop flip test. Route: tech-lead (rider here or 3.0.1).
2. **Info: `ui/theme/Clay.kt:220`** — each gate instance remembers a `RememberedCoroutineScope` (one child `Job` on the recomposer's effect job, runtime 1.11.3 `Effects.kt:506, 617`), replacing a remembered `Settings.Global.getFloat`; no SDK call, no registration, no per-recomposition allocation. Compose's observer start moves to Home's first composition, same frame (`WindowRecomposer.android.kt:97, 117, 423-445`); QA's start-path logcat check covers the `error(…)` at `:444`.
Suppressions: none (nothing to approve or reject). THIRD_PARTY: no new dependency.

## What was verified
- Gate: on unless factor 0, on when absent; main has no `Settings.Global`/`contentResolver`/`ContentObserver` (one KDoc mention).
- 14 calls in 9 files read: 8 choose spec/structure per recomposition, 4 capture at an event-keyed effect start, 3 always `animateTo`, 1 keyed on the gate (Finding 1).
- Frozen gate restored (6aa7624's `Clay.kt`) → the six new live tests 6/6 red.
- 7c mutant A (removal layer back inside the keyed effect): head 4/18 red ("missing (1): PegRemove"), pre-ruling 18/18 green. Mutant B (20 ms wait before the tick): head 2/12 red ("missing (1): ContextClick"), pre 12/12 green.
- 14-of-63 probe reproduces (pre-ruling tests: 7).
- Tick cues: `StateRestorationTester` and real `ActivityScenario.recreate()` replay none of the four; reverted to `> 0` → 6/6 red ("…was: [Select, Deselect, Undo, InvalidTap]").
- `2b98c5c`: exactly the 11 §3 files; assertion-pattern grep prints nothing.
- `w:` 35 → 39, +4 v1 `createComposeRule` lines in the four moved classes (§9.8 rule 2 exception); 0 in main.
- `app/build.gradle.kts`: one block, `audioProvenance` + `tutorialSources`, both `RELATIVE`.

## Checks
- `./scripts/check.sh --max-workers=4` — pass, 11m 7s; `:app` 1,224 (122 suites), `:domain` 294, depthSweep 1, fuzz 1, build-logic 55 = 1,575, 0 failures; lint 0 errors, 64 warnings, none in the diff.
- Probe worktrees removed; `git status` clean at `ed6d396`.

## Open issues / risks
1. Finding 1 to the tech-lead. 2. Device-only items with QA (ruling §6, §8). 3. Same-model review, mitigated.

## Handoff
tech-lead (fresh): sign-off + Finding 1 routing. qa-engineer: the ruling's device session (§3 repro, §6, §8).

VERDICT: PASS
