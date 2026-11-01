# ux-designer report — 15-founder-feedback (founder-feedback amendment to docs/ux-spec.md)
Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (b4234bf..885c6a9) — b4234bf the spec amendment, 17ceda6 FOUNDER.md (Q-005 + the producer's Q-004/Q-003 edits), 885c6a9 a wording follow-up to the spec; this report is the commit after it
Skills loaded: ergoz-soft:standards (role: ux-designer), ui-ux-pro-max:ui-ux-pro-max, android-accessibility, compose-skill. Rules read: studio.md, reporting.md, gitflow.md, compose.md and dispatch.md from the plugin (0.6.2, because the product snapshot is 0.5.1); skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, lessons.md from the product snapshot. No .planning/recall/RECALL.md exists.
Ran in parallel with: none
Ran on: opus (claude-opus-5-5) at effort xhigh. STAFFING DEVIATION: the agent file pins `model: fable`. The Fable allowance is exhausted (HTTP 429), and the founder's Q-003 answer in .planning/questions/FOUNDER.md substitutes Opus for Fable-staffed roles on phase 15. rules/studio.md says "a fable-written plan, spec or lesson is checked by a fresh fable agent that did not write it". This spec amendment was written on Opus, so whatever checks it cannot be a model superior to its author's while Q-003 stands.
Fan-out: none. One general-purpose helper ran the git commands and read-only greps, because this role has no Bash tool. It made no design decision and stood in for no studio role.
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **Home (D-03l, §4.2.4, criteria 15n–15r):** the peg-cross logo is withdrawn. The title sits start-aligned in one row with the gear on every window. D-03i's upper slack (measured ≈172dp of blank on the founder's phone) becomes a **stage** in the hero: a static clay board showing an English-33 game in progress, sized by the slack and not drawn below 48dp. The largest blank any window can now show is under 80dp. The ribbon, cards and palette are kept; only the ribbon's position changes (forced).
- **Dock (D-04e, §4.4.1, criteria 21a–21g):** the root cause is a channel collision. Fill-vs-outline encoded both importance (D-04c) and state (D-04b), at ≈1.04:1 against the slab. The fix is five looks: actions (Undo, Hint, Restart) are Available, Working or Unavailable and never on/off; Show-moves alone is On (solid `primary` plus a check mark) or Off (a ring plus a slashed glyph). Every pair differs in ≥ 2 non-hue channels. Show-moves becomes a `Role.Switch` with one name, and `show_moves_off` is deleted. A spent allowance is not a state.
- **Strip (D-05d, §4.3.1, criteria 18a–18e):** the title moves to `titleLarge` (22sp) and the timer to `headlineSmall` (24sp) in a 40dp-minimum clay pill. I also found and ruled a **double system-bar inset** (the strip and dock re-apply the Scaffold's insets). It costs a 360×640dp phone 72dp and shrinks the board from 328 to 304dp. With it fixed, the larger type costs the board nothing at 1.0× and 2.0× font scale.
- **Q-005 written** (board stage / no art, one block / bigger cards; Blocks: nothing). The default, option 1, is fully specified.

## Outputs
- docs/ux-spec.md — the founder-feedback amendment dated 2026-09-28:
  - a seventh-revision block, a §1 note and a §2 note;
  - decisions D-03l, D-04e and D-05d, with pointers on D-03a, D-03b, D-03i, D-04b, D-04c and D-04d;
  - new subsections §4.2.4, §4.3.1 and §4.4.1, plus pointers in §4.2, §4.2.2, §4.3 and §4.4;
  - amendments in §5, §6.1, §6.2, §6.3, §6.5, §8, §9, §10, §11, §12, §13 (a retarget table), §14 (17 new criteria), §15 (Q-901 superseded, Q-005 added) and §16.
- .planning/questions/FOUNDER.md — Q-005 above Q-004, committed together with the producer's uncommitted Q-004 entry and Q-003 "answered" edit. Q-005 as filed: *Home hero: how should the top of Home fill the screen?* Options: 1) board stage (default); 2) no art, one block in the lower part of the screen; 3) bigger cards on tall phones (this changes the cards the founder kept). Recommendation: 1. Blocks: nothing.
- .planning/reports/15-founder-feedback/ux-designer.md — this report.

## Checks
All run by the helper in the worktree before commits b4234bf and 17ceda6 (verbatim results):
- `git -C …/PegSolitarie-p15 branch --show-current` — pass (`feature/phase-15-ux-refresh`)
- `git -C …/PegSolitarie-p15 diff --check -- docs/ux-spec.md .planning/questions/FOUNDER.md` — pass (exit=0, no whitespace errors)
- `grep -cE '^(15[n-r]|18[a-e]|21[a-g])\. ' docs/ux-spec.md` — pass (17: 5 Home + 5 strip + 7 dock criteria, as designed)
- `grep -nE '^#### 4\.(2\.4|3\.1|4\.1) ' docs/ux-spec.md` — pass (all three new subsections present, lines 1707 / 1915 / 2071)
- `grep -n '^## Q-00' .planning/questions/FOUNDER.md` — pass (Q-005 at line 3, above Q-004 at line 22; order newest first)
- `git -C …/PegSolitarie-p15 status --porcelain` after commit 17ceda6 — pass (empty)
- The follow-up spec commit and this report's commit are each made only if `git diff --check` exits 0 on the file being committed.
- `ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew check --offline` — not run. This is a docs-only change that touches no source, resource or test, and this role has no Bash tool.

## Open issues / risks
1. **The double inset is inferred, not measured.** It comes from reading the code (`GameScreen.kt:170-175` applies the Scaffold's content padding; `GameStatusStrip.kt:81` adds `statusBarsPadding()`; `GameActionDock.kt:95` adds `navigationBarsPadding()`) and from the screenshot's geometry (back chip top at ≈112dp with a ≈52dp status bar). Criterion 18b settles it either way. The same pattern may exist on other screens that pair a Scaffold with their own bar padding, such as the tutorial. I did not audit them; the producer may route a check.
2. **§4.2.4's band heights at 412dp are estimates** (given as ranges). Criterion 15p part 3 sends a shortfall back to this role with numbers instead of letting a band shrink.
3. **P10 grep collision risk.** The plan's prohibition grep `allowance|badge|×[0-9]|adReason` over `GameActionDock.kt` would fire on the word "badge". The spec names the new element the "on mark" and forbids "badge" in code, KDoc and test names. The code-auditor should know why.
4. **Test retargets owed** (§13's new table). `GameScreenChromeTest.showMovesCarriesItsStateByFillAndNotByTintAlone` and anything reading `DockActionFilledKey` must be retargeted to the new look property and switch semantics. Home geometry tests in `HomeLayoutTest`/`HomeScreenTest` that assert hero art also need retargeting. Retarget, never delete or `@Ignore`. `ButtonSoundTest.kt:226-244` and `GameFlowTest.kt:129` are designed to pass unedited.
5. **One more `@Suppress("MagicNumber")`.** The new in-house `ShowMovesOffIcon` carries it on path data, under the existing vector precedent (`UndoIcon.kt`, `ModeGlyphs.kt`). The code-auditor sees it.
6. **Fredoka may not honour `tnum`.** If it doesn't, the timer jitters, and criterion 18d catches it on a device.
7. **Compact thresholds kept deliberately.** They stay at 720/880/400dp although their derivation assumed the 172dp hero. They are now conservative, and re-deriving them is a later tuning decision that changes no criterion.
8. **FOUNDER.md authorship.** The plugin's 0.6.2 studio.md says FOUNDER.md entries are the producer's alone, and my agent definition says to draft art-direction questions in the report. I wrote Q-005 into FOUNDER.md on the producer's explicit dispatch instruction (it is also drafted above) and asked the founder nothing. The producer owns it from here.
9. **Commit trailer.** The plugin's gitflow.md fixes the trailer as `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`. My commits carry `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` because that is the model that wrote them (Q-003); a Fable trailer would misattribute the work. The branch's recent commits use `Claude Opus 5 (1M context)`. The producer should confirm the form.
10. **Stale snapshots.** The product rules snapshot (.claude/rules/VERSION 0.5.1) is older than the plugin (0.6.2); `/ergoz-soft:status` refreshes it. Separately, `15-PLAN.md` S7 still describes the loud/quiet dock and `statusBarsPadding()`. That plan is the planner's and producer's to supersede with a follow-up slice, not mine to edit.
11. **The design rests on reading.** Taste risk on the Home default is covered by Q-005 (non-blocking). The dock's legibility claim is checked on a device by 21f (contrast) and 21g (greyscale read). Only the founder can confirm that the new dock reads as on/off.

## Handoff
Next: producer — dispatch **android-game-dev** on a follow-up slice for D-03l, D-04e and D-05d. It builds against §4.2.4, §4.3.1 and §4.4.1, with criteria 15n–15r, 18a–18e and 21a–21g and the retargets in §13's founder-feedback table. §6.1 lists the seven new tokens, the one changed value (`game_timer_pill_height` 32→40dp, now a minimum) and the three retired tokens. Then:
- **code-auditor:** the P10 naming, the one extra `@Suppress` under precedent, and that retargeted tests are neither deleted nor `@Ignore`d.
- **qa-engineer**, on a device: 15p part 3, 18b, 18d, 21f, 21g, 26/26a and 27, in light and dark, with the Q-003 deviation noted.
- **Q-005** is queued and blocks nothing; build option 1 unless the founder answers otherwise. If the answer is 2 or 3, §15 states exactly what changes in §4.2.4 and which criteria stand.
