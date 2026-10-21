# ux-designer report — ux-spec-folds
Date: 2026-10-21
Branch: feature/ux-spec-folds (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The product's rules snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch. I also read the product's `.claude/rules/lessons.md`. Then ui-ux-pro-max, android-accessibility and compose-skill.
Ran in parallel with: none known to this role
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort). **Staffing deviation:** the ux-designer is pinned to fable. This ran on Opus under the founder's Q-019 (2026-10-17: "Opus for every Fable-pinned role until 3.0.0 is published"), in `.planning/questions/FOUNDER.md`.
Fan-out: none. I dispatched no studio role. Five read-only Explore helpers on haiku ran greps and `git status` for me, because this role has no Bash (rules/dispatch.md).
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)

## Summary
- **Folds a–e are applied** in the tech-lead's words, and every superseded line is kept as the record. They cover §6.5 (the `Pixel_9a_bench` instrument, the D-1 route to it, new precondition 9, arm A as the merge-base, the precondition-5 reading), criterion 15e's two replacements, the 15k part-2 record with the trailing-gap reading, and DESIGN-SYSTEM.md §Typography.
- **F-1 ruling (D-03n, §4.2.5):** a Continue chip arriving after first paint moves nothing a player can tap. The hero row and both tiles are pinned. The footer becomes ⅕ of the no-chip slack, fixed for the window. The stage gives up the chips' room and the ribbon rides on its edge, with the motion smooth. The first frame draws the stage and tile art whole.
- **Timer (D-05e):** TalkBack reads "Elapsed time, 5 minutes, 12 seconds", never as a live region. Criterion 26b writes out the spoken order per screen.
- **200 % pills (D-03o):** no word is ever broken. When any label no longer fits in half its pill, all three pills stack. The shape is decided by the labels, never by the values.
- **15e O-1 (D-05f):** yes, a dark three-button navigation bar is wanted in dark mode. Its criterion is 29.
- All four rulings are built on `bugfix/home-chip-arrival`, and that branch needs its **own 15e sitting**.

## Outputs
- `docs/ux-spec.md` — the tenth revision, dated 2026-10-21, with this change list:
  - **Folds.**
    - The top-of-file "Tenth revision" note, and the frontmatter `revised:` date.
    - §6.5: precondition 1 (the 2026-10-18 and 2026-10-21 amendments plus a fold note), a pointer at precondition 2 (P-4), the precondition-5 note, the new precondition 9, the "Which builds" pointer for 2026-10-18, a pointer at protocol step 3, the "eight → nine" pointer under "Where the numbers go", and a pointer at the Ruling 2 note.
    - Criterion 15e's two replacements, with the old words in a 2026-10-18 amendment note.
    - Criterion 15k part 2's 2026-10-21 record.
  - **New decisions.**
    - D-03n and D-03o in §3 Home; D-05e and D-05f in §3 Toolbar.
    - New subsections §4.2.5 (D-03n), §4.2.6 (D-03o) and §4.3.2 (D-05e).
    - D-05f's specification in §6.2.
    - New criteria: 15s–15v, 15w, 18f, 18g, 26b and 29.
  - **Dated pointers**, at:
    - D-03i and D-03l;
    - §4.2.2, in clause 3 and at the "reshuffles" sentence;
    - §4.2.4's column table;
    - §4.3's note and §4.3.1's status;
    - §6.1 (the compact-pill token's note), §6.4, §8 (the new strings), §10 (one table row and one reduced-motion line), §11 (text scaling, traversal, contrast), §12's dark-theme row, §13 (a retarget table and an owed list) and §16;
    - criteria 15m and 15r.
- `docs/ui/DESIGN-SYSTEM.md`:
  - §Typography takes the tech-lead's text (`tech-lead-fonts.md`), and the replaced sentence is kept in a dated note.
  - The Color › Dark section gains a dated "system bars take the theme" paragraph (D-05f).
- `.planning/reports/ux-spec-folds/ux-designer.md` — this report.

## The F-1 ruling, for the developer (`bugfix/home-chip-arrival`)
**May chip arrival move Home?**
- It may move the **stage**: the stage gives up exactly the chips' room, and its art shrinks with it.
- It may move the **ribbon**, which rides on the stage's bottom edge and goes up by exactly the room.
- It may **never** move the hero row (title and gear) or either mode tile.
- On a window where the stage cannot give the whole room, the tiles move down by the shortfall and no further. On every phone window §14 measures at default font scale, there is no such window.

**The rule (§4.2.5):**
- S₀ is the no-chip slack: the available height less the hero row, the ribbon, the two tiles, the padding and the five no-chip band gaps, floored at 0.
- R is the chips' room: each chip's height plus one band gap.
- Footer = min(S₀/5, max(0, S₀ − R)).
- Stage = max(0, ⅘S₀ − R).
- The tiles move by max(0, R − ⅘S₀).
- The room opens over `ClayAnim.ENTRANCE_MS` with `ClayAnim.ENTRANCE_EASING`, without overshoot, and is open in one frame with animations off.
- The mechanism is the developer's. If it needs more than layout, the tech-lead rules on it (sign-off Decision 3, step 2).

**The bounds the test pins** (criterion 15s, the test the tech-lead named):
- **Windows:** each of the six 15p part-2 windows, at default font scale, NATIVE metrics in the app theme, with 24/48dp insets.
- **Emissions:** no chip → Classic → both → none, and no chip → Challenge → both.
- **Bounds:** the title's, the gear's and both tiles' bounds equal their no-chip bounds (±0.5dp). The footer is fixed at S₀/5. The stage equals max(0, ⅘S₀ − R), and the ribbon rises by exactly R. The chips are complete in the first viewport.
- **15t:** at 2.0 the tiles move by exactly the shortfall.
- **15u:** frame by frame on a paused clock, the tiles hold still in every frame.
- **15v:** QA's recorded cold start, the device half.

**Why ⅕, not ¼** — QA loop-3 device numbers, projected:
- ⅕ pins the tiles at 360×640 with both chips (6.4dp to spare), where ¼ of the no-chip slack moves them 5dp.
- It keeps the founder's own one-chip state within 3.6dp of what they approved.
- At the bottom of the full set it keeps the one-chip board at ≥ 120dp (≈130dp, against ≈116dp).

## Checks
This role has no Bash. Each command below was run read-only by a haiku Explore helper in `/Users/ermanergoz/Desktop/PegSolitarie-ux`, and its output was returned verbatim. The line numbers come from the last run, after the last edit.

**Fold a** (the sign-off's text):
- `grep -c 'Pixel_9a_bench' docs/ux-spec.md` — pass, 8. The close report's own check found 0 under `docs/`.
- `grep -n 'Amended 2026-10-18 — tech-lead phase-15 sign-off, Decision 1' docs/ux-spec.md` — pass. Two hits: precondition 1 at :3452, and criterion 15e's amendment note at :4312.
- `grep -n -E '^9\. `/data` free ≥ 4 GB' docs/ux-spec.md` — pass, :3507, the new precondition 9.
- `grep -n 'Arm A is the merge-base of `develop` and the branch at the sitting' docs/ux-spec.md` — pass, :3433, the "Which builds" pointer.
- `grep -n -E 'Arm A = the merge-base of `develop` and the branch|one AVD \(`Pixel_9a_bench`, §6.5 precondition 1\)' docs/ux-spec.md` — pass, :4282 and :4283, the two replacements in criterion 15e.

**Fold b** (D-1):
- `grep -n 'Amended 2026-10-21 — tech-lead, phase-15 close, D-1' docs/ux-spec.md` — pass, :3459.

**Fold c** (15k part 2, INFO row 7):
- `grep -n -E 'Those figures were Robolectric LEGACY readings|M-edge120 passes and M-edge121 is cut' docs/ux-spec.md` — pass, :4436 and :4447.

**Fold d** (15e O-1, the navigation bar):
- `grep -n -E '^- \*\*D-05f\*\*|^\*\*The system bars take the theme\*\*|^29\. ' docs/ux-spec.md` — pass. The decision is at :1145, the specification in §6.2 at :3210, and criterion 29 at :4749.

**Fold e** (DESIGN-SYSTEM.md §Typography):
- `grep -n -E 'bundled as static|ui-text-google-fonts|Fredoka has no `tnum`|The system bars take the theme' docs/ui/DESIGN-SYSTEM.md` — pass.
  - The new sentence is at :58, and the old one is kept as the record at :70, inside the amendment note.
  - The `tnum` clause is on :62–63 of the same paragraph. It is wrapped across a line, so its own pattern has no hit.
  - The D-05f paragraph is at :44.

**The rulings:**
- `grep -n -E '^- \*\*D-03n\*\*|^- \*\*D-03o\*\*|^- \*\*D-05e\*\*|^#### 4\.2\.5|^#### 4\.2\.6|^#### 4\.3\.2' docs/ux-spec.md` — pass. The decisions are at :944, :975 and :1129. The subsections are at :2351, :2524 and :2700.
- `grep -n -E '^15[s-w]\. |^18[fg]\. |^26b\. ' docs/ux-spec.md` — pass: 15s :4543, 15t :4561, 15u :4567, 15v :4576, 15w :4584, 18f :4638, 18g :4645, 26b :4725.
- `grep -o -E '^(1[0-9][a-z]?|2[0-9][a-z]?)\. ' docs/ux-spec.md | sort | uniq -d` — pass, empty: no criterion label is duplicated.
- `grep -n -E 'elapsed_time_spoken|duration_minutes|duration_seconds|duration_under_a_minute' docs/ux-spec.md` — pass. The §8 rows and the §4.3.2 reference are present.
- `grep -n -E '^revised: |Tenth revision' docs/ux-spec.md` — pass, :7 `revised: 2026-10-21` and :291.

**Scope:**
- `git status --short` — only `docs/ui/DESIGN-SYSTEM.md` and `docs/ux-spec.md` are modified, plus this report directory (untracked). No other file was touched.
- `git diff --stat` — `DESIGN-SYSTEM.md | 28 +-`, `ux-spec.md | 776 +++…`, 795 insertions(+) and 9 deletions(-). The 9 deleted lines are exactly the edits the folds and their notes required: the frontmatter date; criterion 15e's two replaced lines; the §12 dark-theme cell, which gained a dated sentence; and the five lines of the old §Typography sentence, now quoted in its amendment note. No decision text was rewritten in place.
- `git branch --show-current; git log -1 --oneline` — `feature/ux-spec-folds`, `c9228a9 Merge branch 'feature/phase-15-ux-refresh' into develop`. Nothing is committed: the producer commits.

- The Check command (`./gradlew check`) was **not run**. This task changes documents only and no code, and this role has no Bash.
- I did not read `.planning/log/`.

## Open issues / risks
- **Staffing deviation.** This spec amendment was written on Opus, not on the pinned Fable (Q-019). Under rules/studio.md a spec written on fable is checked by a fresh agent that did not write it. The producer may want a fresh ux-designer check of the tenth revision before the developer is dispatched, as was done for D-03m on 2026-09-29.
- **D-03n's numbers are projections** from QA's loop-3 device readings. Their NATIVE values may differ by a few dp, and the tightest margin is 360×640 with both chips (6.4dp, or 9.6dp behind a 48dp bar). 15s gives a failure route back to me with the numbers. It is never closed by letting the tiles move.
- **D-03n changes the fresh-install look the founder saw in H-4 shots 01/02.**
  - The tiles sit ≈20dp lower and the board reaches its 320dp cap.
  - The founder's own one-chip state (shots 03/04) moves by 3.6dp.
  - This is a UX decision inside my authority, not taste between equals, so no founder question is drafted.
  - The producer may show the branch's QA shots for information.
- **D-03n part 4 (a slab drawn without its pegs, a tile without its art, ≈0.26s at first paint) has no diagnosed cause.** The developer finds it, and the tech-lead rules if the fix needs more than drawing.
- **D-05e's fallback depends on how TalkBack treats a focused node whose description changes every second.** I could not settle that by reading. 18g captures it, and the whole-minute fallback is already ruled, so a finding needs code and no new ruling.
- **D-03o's "half" line has ≈5dp of estimated margin at default scale** for "Boards" (≈42 of ≈47dp). If 15w measures more, the finding comes back to me. 15m's one-line shape at default scale is not traded for it.
- **26b is the first written-out TalkBack order for Home and the tutorial.** The pass may surface defects that have nothing to do with these rulings.
- **The `docs/tech-design.md` rows that mirror these rulings are the tech-lead's to write:**
  - §16 row 9 is now ruled "wanted";
  - the F-1 row gains D-03n's scope;
  - the timer's TalkBack row is answered by D-05e.

## Handoff
Next: producer —
1. Commit `docs/ux-spec.md`, `docs/ui/DESIGN-SYSTEM.md` and this report on `feature/ux-spec-folds`, then merge into `develop` as a docs-only pipeline branch. Folds a–e then govern the next 15e sitting and `release/3.0.0`. Pass §6.5 to the next qa-engineer who sits 15e.
2. Dispatch android-game-dev on `bugfix/home-chip-arrival`, cut from `develop` after this merges, test first. The branch builds:
   - **D-03n** (§4.2.5): the footer at S₀/5, the stage at max(0, ⅘S₀ − R), the room's motion, and the stage and tile art whole in their first frame. Tests 15s, 15t and 15u, with the 15g/15p retargets listed in §13.
   - **D-03o** (§4.2.6): the ribbon's single shape, decided by the labels, and the never-squeezed label. Test 15w.
   - **D-05e** (§4.3.2): the timer's description and the §8 strings. Retarget `GameFlowTest.kt:129`, `GameScreenChromeTest.kt:62/75` and `GameStatusStripTest.kt:312-314`. Test 18f.
   - **D-05f** (§6.2): `android:windowLightNavigationBar` in `values-v27/` and `values-night-v27/`, each restating the whole style. Add 29's `WindowThemeTest` case.
   - The tech-lead's close-report INFO rows 1, 3, 4 and 5, already routed to this branch.
3. If the developer finds that D-03n's heights need more than a layout change, the tech-lead rules on the mechanism (sign-off Decision 3, step 2).
4. Then the code-auditor, then the qa-engineer:
   - **Yes, this branch needs its own 15e sitting** on `Pixel_9a_bench`, with arm A at its merge-base with `develop`. The branch changes Home's first frame (the ⅕ split, the column's measuring in `ui/home/`, the stage and tile art in the first frame), and `res/values*/themes.xml` is a start-path file too.
   - The same sitting covers the device checks 15v, 15w's device half, 18g, 26b and 29's device half.
   - Then the tech-lead's sign-off, all before `release/3.0.0` is cut.
5. Tech-lead, on `feature/phase-14-release-docs`: mirror D-03n, D-05e and D-05f in `docs/tech-design.md` §14.1 and §16.
