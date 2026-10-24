# ux-designer report — audio-review (wrong-tap feedback, sound-designer P1)
Date: 2026-09-29
Branch: feature/audio-review (9abac3c..this commit)
Skills loaded: ergoz-soft:standards (role: ux-designer), ui-ux-pro-max:ui-ux-pro-max, android-accessibility, compose-skill. Rule files read: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md (the v0.6.2 snapshot, which matches plugin 0.6.2), and the plugin's dispatch.md.
Ran in parallel with: the sound-designer (res/raw, audio/recipes, docs/audio/CREDITS.md, docs/audio-spec.md), the research-analyst (docs/audio/licence-verdicts.md) and the tech-lead (docs/tech/audio-silent-mode.md), all in this worktree on separate paths
Ran on: opus (claude-opus-5-5) at effort xhigh — a **staffing deviation**, see below
Fan-out: none. This role has no shell, so four general-purpose helpers ran the git reads, a heading index of the phase-15 spec, and the two commits. None was a studio role.
Loops: 0
Lessons applied: none (the v0.6.2 snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Staffing deviation
`agents/ux-designer.md` pins `model: fable`, `effort: xhigh`. The Fable allowance is exhausted.
The founder's answer to **Q-011** (`.planning/questions/FOUNDER.md`, 2026-09-29: "2 — run them on
Opus") scopes an exception to the four audio follow-up steps, and this is one of them. It is not a
standing policy change. The amendment and this report are written by Opus, so under studio.md any
check of them by a fresh agent should run on Fable once Fable is available.

## Summary
- **Wrong taps, traced:** a player can make exactly three on the game board: W1, an empty hole
  with nothing held; W2, another peg while one is held; W3, a hole that is not a destination while
  a peg is held. Taps off the footprint never reach the ViewModel. A put-down, a jump and a
  selection (even of a peg that cannot move) are not wrong.
- **Default feedback:** a 270 ms horizontal head-shake of the tapped cell. While a peg is held,
  one redirect plays with it: the dots re-pop on W3 with Show-moves on; otherwise the held ring
  nudges.
- **Reduced motion:** the shake becomes a still dashed `onSurfaceVariant` ring, held 450 ms.
- **Haptic:** the platform *reject* haptic on API 30 and later, under the Haptics switch. It is
  never the jump's `ContextClick`, and there is no fallback below API 30.
- **TalkBack:** a polite announcement in the game; the tutorial announces nothing.
- **Tutorial:** the jump-haptic asymmetry is **not intended**: lift `JumpHaptic` the way
  `JumpSoundEffect` was lifted. The tutorial's wrong taps get only the redirect. That builds the
  dot re-pop phase 15 specified but never built.

## Outputs
- `docs/ux/invalid-tap-feedback.md` — the amendment, keyed to the section numbers of the phase-15
  `docs/ux-spec.md` (d017711). It covers:
  - the traced tap tables for the game and the tutorial;
  - decisions D-06, D-06a–d and D-01j;
  - the motion spec (keyframes, easing, amplitude), the reduced-motion fallback, the interruption
    rules, the haptic, the announcement and the state the UI needs;
  - tokens (1 dimen and 7 scoped constants; no colour, no hex, no asset) and 3 strings;
  - 19 acceptance criteria (WT-1…WT-19), the test impact, a fold-in map, the rejected
    alternatives and a drafted Q-012.
- `.planning/reports/audio-review/ux-designer.md` — this report.
- Local only, outside the repo: the helper's extracts of the phase-15 spec and source, in the
  session scratchpad. They are not committed and not needed by anyone.

## Checks
- `git show feature/phase-15-ux-refresh:docs/ux-spec.md` (run by a helper into the scratchpad) — pass (3,730 lines at d017711; read §1–§4.4, §5, §5.1, §6.2, §6.4, §10, §11, §14, §15 and §16)
- `git show feature/phase-15-ux-refresh:<path>` for `GameViewModel.kt`, `GameScreen.kt`, `GameUiState.kt`, `BoardCanvas.kt`, `BoardGeometry.kt`, `BoardMetrics.kt`, `GameAnim.kt`, `TutorialViewModel.kt`, `TutorialScreen.kt`, `TutorialAnim.kt`, `Board.kt`, `dimens.xml`, `strings.xml` — pass (traced; `GameViewModel`, `GameUiState`, `BoardCanvas`, `BoardGeometry` and the theme files are identical on HEAD c5b02ef and on d017711)
- `git grep -n -E 'liveRegion|LiveRegionMode|announceForAccessibility|wobble|shake|rePop|repop' feature/phase-15-ux-refresh -- app/src/main` and the same on HEAD — pass (zero hits: nothing exists to collide with)
- WCAG 2.x relative-luminance contrast, computed by hand from the hex in `ui/theme/Color.kt` — pass (the mark `onSurfaceVariant` on the board slab `surfaceContainerHigh`: 5.31:1 light, 5.59:1 dark; the floor for graphics is 3:1)
- `git diff --cached --check -- docs/ux/invalid-tap-feedback.md` (run by a helper before 9abac3c) — pass (exit 0, no whitespace errors; the commit hooks ran clean without `--no-verify`)
- `./gradlew check` — **not run**. This is a docs-only change: no code, resource or test is touched. The shared worktree also carries the sound-designer's in-flight `res/raw` edits, so a run here would measure their tree, not this change. The last full check on this branch passed at 24f11da (sound-designer report).

## Open issues / risks
1. **Phase 15 promises a tutorial dot re-pop it never built.** §4.4 class 2 says a
   non-progressing tap makes "the legal dots re-pop". `TutorialViewModel.registerNonProgressingTap`
   (`:267-274`) changes no drawn state, and the canvas pops dots only when `selected` changes
   (`BoardCanvas.kt:159-169`). The amendment builds it with the game's wrong-tap signal. Until
   then, the tutorial's first two stray taps show nothing. There is no rules/audio.md breach,
   because the tutorial plays no invalid-tap sound either.
2. **The board has no per-cell semantics** (`BoardCanvas.kt:196-212`: one `Canvas`, no label, no
   actions). A TalkBack user cannot aim a tap at a cell, so the new announcement is correct but
   rarely reachable. This gap predates phase 15. It is its own accessibility task (a board spec
   from this role, then the android-game-dev), not part of this fix.
3. **How the tutorial reads `hapticsOn`** is open. `TutorialViewModel` is pinned to two
   collaborators by a reflection test. A settings read is not forbidden by §5.1, but the pin or
   the route has to change. That is the tech-lead's call.
4. **`HapticFeedbackType.Reject` in the pinned Compose BOM.** The developer confirms it maps to
   `HapticFeedbackConstants.REJECT`. The spec gates on `SDK_INT >= 30` in our own code, so a
   library fallback below 30 can never run.
5. **`docs/audio-spec.md` goes stale on four rows** once this is built (it belongs to the
   sound-designer, and I did not touch it):
   - the `InvalidTap` row: its trigger names an out-of-play cell, which the UI cannot deliver; its
     Haptic column should read "reject, API 30+"; its Visual partner should read "shake, or the
     mark with animations off, plus a redirect";
   - the tutorial wrong-tap row, which says "dots re-pop, coach ring" for what is today only the
     coach at the third tap;
   - Accessibility;
   - Settings › Haptics, which says "the only haptic in the game is on the jump".
6. **The phase-15 spec's §11** calls "the dots staying put" the invalid tap's partner. That is a
   false claim, corrected at fold-in.
7. **Ids D-06 and D-01j are provisional.** Phase 15 may add revisions before it merges. The
   fold-in renumbers them if needed and keeps the text.

## Drafted founder question (for the producer to queue; I did not edit FOUNDER.md)

    ## Q-012 — Should a wrong tap also buzz?   (open)
    Date: 2026-09-29    Asked by: ux-designer
    Context: A wrong tap on the board now shakes the tapped hole or peg (a still dashed ring when
    animations are off) and plays the new "soft tok". Android 11+ phones can also give a short
    "reject" buzz that feels different from a jump's tick; the Haptics switch still turns all off.
    Options: 1) buzz on a wrong tap (built by default) 2) no buzz — shake and sound only
    Recommendation: ux-designer — 1: it tells a player who is looking elsewhere, or cannot hear,
    that the tap did not take, and it cannot be mistaken for a jump.
    Blocks: nothing
    Answer:

## Handoff
Next: producer — queue Q-012 (non-blocking) if you agree. Schedule the build after
`feature/phase-15-ux-refresh` merges, because the tutorial half exists only there. Then dispatch
the ux-designer to fold `docs/ux/invalid-tap-feedback.md` into `docs/ux-spec.md` (map in its §11).
Track the board's per-cell TalkBack semantics as a separate task (open issue 2).
Next: tech-lead — rule how the tutorial reads the persisted `hapticsOn` without breaking
`TutorialViewModel`'s two-collaborator pin (amendment §4), and confirm the reject-haptic gating
(§3.4).
Next: android-game-dev — build D-06, D-06a–d and D-01j per amendment §3–§6, with tests WT-1…WT-19
(§8–§9).
Next: sound-designer — update the four `docs/audio-spec.md` rows in open issue 5.
Next: qa-engineer — the device criteria: WT-12 (an API < 30 phone), WT-14 (TalkBack), WT-15
(animations off) and WT-18 (a muted recording in light and dark, plus the mark's contrast).
Next: ux-designer (review mode) — review the developer's UI diff and QA's captures against the
amendment.
