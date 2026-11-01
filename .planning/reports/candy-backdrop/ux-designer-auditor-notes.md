# ux-designer report — candy-backdrop (rulings on the code-auditor's findings 5 and 7)
Date: 2026-11-01
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the rule files read from the product's `.claude/rules/` snapshot as loaded into context: studio, reporting, gitflow, testing, ip-policy, skills-policy, stacks, logging, memory, puzzle-design, play-store, kotlin-android, compose, lessons, audio)
Ran in parallel with: candy developer loop 1
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **(a) The frozen mid-pass glint is a 3.0.0 defect.** It breaks B-05a rule 9 (a glint lasts at most 2.5 s) and §6.1's still picture (no glint). New ruling B-22a: on the live clock, the gel sheen is drawn only while `backdropMotion` is Running. A settle that begins mid-pass ends the pass at once. The fix is one condition plus one test.
- **(b) BD-01 commands 1 and 2 now pass `--exclude=README.md`.** `docs/store/README.md` is working notes that quote the word rules. The rest of `docs/store/` is listing copy and stays in scope. Command 3 is unchanged, because finding 6's reversed word list is already accepted.
- These edits are to the spec only. I touched no code or test.

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md — the spec. Edits:
  - front matter `revised:` line;
  - new decision **B-22a** in §1.2, after B-22;
  - §3.4, the gel sheen's last bullet;
  - §6.1, the line after the table;
  - §6.3, step 2;
  - §11 **BD-16** row, the pass line and "By" column;
  - §11 **BD-01** row label;
  - BD-01's commands block and a new paragraph under it;
  - §12, the `BackdropSettleTest` row.
- /Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-auditor-notes.md — this report.

## Checks
- Read the code-auditor's report findings 5–7 (`code-auditor.md:78-80`), and the spec's §3.4, §6.1, §6.3, §11 BD-01/BD-16 and §12, in full.
- Read the tech-lead's A2, A5 and A10 (`tech-lead-direction-b.md`): the `backdropMotion` states and the injected-frame test seam.
- Read the code read-only to make the rule testable against what exists:
  - `BackdropClock.kt:31-40`: Settling covers both the ease to 0 and the wake ease.
  - `BackdropClock.kt:73-78`: `FixedBackdropClock` reports AtRest with `isStatic = false`. B-22a therefore says that a fixed clock keeps drawing its pose.
  - `GelTile.kt:66-69`: the sheen is hidden only on `isStatic`.
- No command run: this role has no Bash. I did not re-run BD-01's amended grep. The auditor's report says that `docs/store/` with README.md excluded prints nothing, and so do `strings.xml` and `assets/` (`code-auditor.md:30`). The amended commands should therefore print zero lines. QA or the auditor confirms on the next loop.

## Open issues / risks
1. **Pop-in on wake (accepted).** After a wake, if the clock time is inside a pass when the clock reaches Running again, the rest of that pass is drawn. The glint appears mid-lip and slides off in under 2.5 s. I accepted this rather than adding pass-tracking state. If QA finds it jarring on the device, the follow-up is "resume only at the next pass start", a post-3.0.0 refinement.
2. **The required skills were not invoked:** ui-ux-pro-max, android-accessibility and compose-skill. The dispatch was a narrow ruling on two items with no layout, colour or accessibility change. Record it as a deviation if the producer wants it counted.
3. **The plugin rule files were not re-read.** The worktree snapshot is v0.6.2 and the plugin is v1.0.0 (`code-auditor.md:4`). I used only the snapshot loaded into context.
4. **The finding 5 fix may land in the same files as F1's fix** (the clock and the tile), which the developer is changing in loop 1. The B-22a test fits `BackdropSettleTest`'s injected-frame setup, the same setup F1's ON→OFF→ON test uses.

## Handoff
Next: producer —
1. **Commit** `docs/ux/candy-backdrop.md` and this report on `feature/candy-backdrop`.
2. **android-game-dev (loop 1):** implement B-22a with its test, beside F1–F4.
3. **code-auditor (loop 1):**
   - run BD-01's amended commands 1 and 2;
   - check B-22a's test, including its red.
4. **qa-engineer:** on Home, after a 60 s settle, check that neither tile's lip shows a glint (BD-16 (c)).

**(a) is a 3.0.0 defect.** The testable rule (B-22a, BD-16 (c)):
- On the host's live clock, a gel tile draws its sheen only while `backdropMotion` is Running. While it is Settling or AtRest, no sheen is drawn. A `FixedBackdropClock` still draws its pose.
- **The test.** Use injected frames. Bring Classic's tile to a clock time inside its pass while Running; its lip differs from the same tile drawn with `StaticBackdropClock` (the control). Then let it idle until the settle begins inside that pass. From that frame on, through Settling and AtRest, the tile's lip region is pixel-identical in the same run to the tile drawn with `StaticBackdropClock`.
- **The red.** With the rule removed, the AtRest frame differs.
