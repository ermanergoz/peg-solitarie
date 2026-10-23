# ux-designer report — ux-spec-folds
Date: 2026-10-21
Branch: feature/ux-spec-folds (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The product's rules snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch. I also read the product's `.claude/rules/lessons.md`. Then ui-ux-pro-max, android-accessibility and compose-skill.
Ran in parallel with: none known to this role
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort). **Staffing deviation:** the ux-designer is pinned to fable. This ran on Opus under the founder's Q-019 (2026-10-17: "Opus for every Fable-pinned role until 3.0.0 is published"), in `.planning/questions/FOUNDER.md`.
Fan-out: none. I dispatched no studio role. Ten read-only Explore helpers on haiku ran greps, file reads and `git status` for me, because this role has no Bash (rules/dispatch.md): five in loop 0, two in loop 1, one in loop 2 and two in loop 3.
Loops: 3
Read in full: `.planning/reports/ux-spec-folds/ux-designer-check.md` (loop 1's input), `ux-designer-check-loop2.md` (loop 2's input), `ux-designer-check-loop3.md` and `game-designer-talkback-board.md` (loop 3's inputs); `.planning/questions/FOUNDER.md` Q-023
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
This role has no Bash. Each command below was run read-only by a haiku Explore helper in `/Users/ermanergoz/Desktop/PegSolitarie-ux`, and its output was returned verbatim. These are loop 0's results; the line numbers are from the end of loop 0, and loop 1's edits have since moved them. Loop 1's own checks are in the "Loop 1" section at the end.

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
   - After that sitting, in the same session (the H-3 pattern; the bench is otherwise reserved for 15e), the qa-engineer runs the device checks 15v, 15w's device half, 18g, 26b and 29's device half.
   - Then the tech-lead's sign-off, all before `release/3.0.0` is cut.
5. Tech-lead, on `feature/phase-14-release-docs`: mirror D-03n, D-05e and D-05f in `docs/tech-design.md` §14.1 and §16.

## Loop 1 — the fresh check's findings
Input: `.planning/reports/ux-spec-folds/ux-designer-check.md` (VERDICT FAIL; committed in `f510479`), read in full. I edited only `docs/ux-spec.md`, plus this report.

**How the edits were made.**
- Earlier decisions and criteria — 15g, D-03k, §4.2.2, §4.2.3, §4.2.4, §6.5 and §11 — were only **appended to**, with dated pointers or notes.
- Text that the tenth revision itself wrote today, and that the check found wrong, was **corrected in place**: §4.2.5, §4.2.6, D-03o, and criteria 15v, 15w, 26b and 29. That text is the revision under review in this same step, not yet the record of any build. Each correction is named in the table below.

| Finding | Severity | What I did | Where |
|---|---|---|---|
| F-1 — D-03n's fixed footer breaks 15g's "a third of the unused space" with both chips | MAJOR | **I ruled how 15g's "unused space" is read**, and kept the ratio. 15g's "the screen's unused space" is **S₀**, the slack the window leaves with no chip, fixed for the window. Its "empty space below the second tile" is the footer, without the band gap and the bottom padding, as QA's loop-3 reading already took it. Under D-03n the footer is ⅕ of S₀ in every state. Why this reading, and not a new ratio: (1) the clause exists to stop slack piling up at the bottom, as the 45 % dead space under D-03i's fixed `Spacer` did, and a footer that holds still while the chips take room above it does not do that; (2) no fixed footer can meet the current-slack reading together with 15p part 1 — on QA's native window the two bounds are 0.1764 and 0.1763 of S₀, under 0.1dp apart, and at the pessimistic end of the baseline the first is 0.092. Only a footer that shrinks as the chips arrive passes the old reading, and that moves the tiles. The false sentence in §4.2.5 ("inside 15g's 'no more than a third'") is replaced with the true per-state numbers (20 / 26 / 38 % of the current slack) and a ruled subsection. 15g gains a dated "Ruled" note; the loop-3 PASS stands on both readings for the build it measured. §13's retarget row and §4.2.5's owed-tests list now assert the clause against S₀ | §4.2.5 status line and the new "How 15g's 'unused space' is read under D-03n — ruled"; criterion 15g's note; §13's 2026-10-21 retarget row; §4.2.5 "What this owes in tests" |
| F-2 — 26b makes the board a TalkBack stop, which it cannot be on this branch | MAJOR | **I fixed the order and did not rule the board a stop.** 26b's gameplay order now runs timer → Undo, and its tutorial order Skip → caption, with an explicit "the board is not a stop" paragraph that cites the code the check named. If a later change gives the board semantics, it is visited in its screen position and is neither required nor judged. §11 gains a pointer: "board" in its two traversal lines names a position, not a stop. It also records that a TalkBack player cannot play the board. That gap predates this refresh and is outside 3.0.0. It is an open accessibility item, a design of its own, for this role with the game-designer later. Ruling the board a stop now would have put a new feature (cell focus, per-cell state, jump actions) on the branch that must land before `release/3.0.0` | criterion 26b; §11 "Screen-reader traversal" pointer |
| F-3 — the folded start-path list omits the other `values*` theme files | MINOR | A dated note under the "Which builds" pointer. The list reads as **every `res/values*/themes.xml`**, on the tech-lead's own text: the close report's Checks treat `res/values-night/themes.xml` as start-path, and INFO row 9 so names both v27 files. If `docs/tech-design.md` §10 states the list differently, that text governs. The tech-lead's wording above it is untouched | §6.5, after the 2026-10-18 pointer |
| F-4 — 15v uses the bench outside a 15e sitting | MINOR | 15v now runs on the bench **only after the branch's own 15e sitting, in the same session**, the H-3 pattern, citing precondition 1 and D-1 condition 3. §4.2.5's owed list and this report's handoff say the same | criterion 15v; §4.2.5; Handoff step 4 |
| F-5 — superseded passages left without pointers | MINOR | Dated pointers at five places. (1) D-03k: (a) LEGACY, which also covers F-9, and (b) D-03o amends part 3. (2) §4.2.2 clause 2's "slack distributed 3 : 1" → D-03n. (3) §4.2.3's "the 3 : 1 slack … as D-03i left them" → D-03n. (4) §4.2.3 part 2 item 2's "the label wraps" → D-03o. (5) §4.2.4's Status ("the 3 : 1 ratio … untouched") and its stage bullets: `Modifier.weight(3f)` is superseded and `STAGE_WEIGHT = 3f` is not to be rebuilt, and "static on every frame" means the board on the slab, while the art's edge follows the stage through a chip transition | D-03k; §4.2.2 clause 2; §4.2.3 (two places); §4.2.4 Status and "The stage" |
| F-6 — bare "R-3" collides with §7's R-3 | INFO | Fixed. Every bare mention is qualified as `ux-designer-15p.md` R-3, "not §7's R-3"; `grep '(R-3)'` now finds nothing | D-03o; §4.2.6 "What QA saw" |
| F-7 — "every tile top is below the 476.8dp midpoint" is false for the 360×640 rows | INFO | Fixed. The sentence now gives both midpoints: native rows against 476.8dp, and 360×640 rows against that window's 334dp (52 + 564 ÷ 2) | §4.2.5, under the table |
| F-8 — §4.2.6 rules 3 and 4 disagree; "about 1.15×" is wrong | INFO | Fixed. Rule 3 now reads "except in rule 4's case". Rule 4 names itself as the one case where a value decides a pill's shape. Its window is corrected to between about 1.07× and 1.12× (e.g. an OEM's 1.1× step), because the ribbon stacks as a whole above about 1.12×. 15w part 4 gains the same exception and notes that none of its tested values reaches it | §4.2.6 rules 3–4; criterion 15w part 4 |
| F-9 — two "a 360×640dp phone … scrolls" headlines read on LEGACY only | INFO | Fixed. Pointers to 15k's 2026-10-21 note at D-03k (pointer (a)) and after §4.2.3's "So: … scrolls" paragraph. The headlines stand as the record | D-03k; §4.2.3 |
| F-10 — criterion 29 leaves the qualifier choice to §6.2 | INFO | Fixed. 29(1) now names `values-v27/` and `values-night-v27/`, each restating the whole style and not in `values/` or `values-night/`. It says that placement is read in the diff and by lint's NewApi on minSdk 24, because at the test's SDK the two placements resolve alike | criterion 29 |

Also: the top-of-file tenth-revision note gains a dated "same day, after a fresh checker's review" paragraph that lists these changes.

### Loop 1 checks
Each command was run read-only by a haiku Explore helper in `/Users/ermanergoz/Desktop/PegSolitarie-ux`, after the last loop-1 edit to `docs/ux-spec.md`:
- `grep -n "How 15g's" docs/ux-spec.md` — pass, three hits: the §4.2.5 status line :2406, the ruled subsection :2505, and 15g's note :4470 (F-1).
- `grep -n 'never against the slack left once the chips are in' docs/ux-spec.md` — pass, :4215, the §13 retarget row (F-1).
- `grep -n "inside 15g's \"no more than a third" docs/ux-spec.md` — pass, no hit, exit 1: the false claim is gone (F-1).
- `grep -n 'The board is not a stop, on gameplay or in the tutorial' docs/ux-spec.md` — pass, :4858 (F-2).
- `grep -n -E 'sentence → the board|`onboarding_skip` → the board' docs/ux-spec.md` — pass, no hit, exit 1: neither order lists the board (F-2).
- `grep -n '"Board" in the tutorial and gameplay orders above names a' docs/ux-spec.md` — pass, :4048, the §11 pointer (F-2).
- `grep -n 'every `res/values\*/themes.xml`' docs/ux-spec.md` — pass, :3536 (F-3).
- `grep -n "only after the branch's own 15e sitting, in the same" docs/ux-spec.md` — pass, :4690, criterion 15v (F-4).
- `grep -n -E 'Pointers 2026-10-21\.|Amended 2026-10-21 — \*\*D-03o\*\*, §4.2.6|is superseded on every width|"Slack distributed 3 : 1 when there is slack" is no|no longer holds: the footer|`STAGE_WEIGHT = 3f` is' docs/ux-spec.md` — pass, six hits: :892, :1635, :1846, :1865, :1987 and :2083 (F-5, F-9).
- `grep -n -E '\(R-3\)' docs/ux-spec.md` — pass, no hit, exit 1 (F-6).
- `grep -n '334dp: 52 + 564' docs/ux-spec.md` — pass, :2483 (F-7).
- `grep -n -E "except in rule 4's case|except in|between about 1.07× and 1.12×|at about 1.15×" docs/ux-spec.md` — pass. Hits at :2637, :2644 and :4709. "at about 1.15×" no longer occurs (F-8).
- `grep -n 'is true on the LEGACY' docs/ux-spec.md` — pass, :1934 (F-9).
- `grep -n 'each restating the whole style, and not in' docs/ux-spec.md` — pass, :4874, criterion 29 (F-10).
- `grep -o -E '^(1[0-9][a-z]?|2[0-9][a-z]?)\. ' docs/ux-spec.md | sort | uniq -d` — pass, empty: still no duplicate criterion label.
- `git status --short` — ` M .planning/reports/ux-spec-folds/ux-designer.md`, ` M docs/ux-spec.md`. `docs/ui/DESIGN-SYSTEM.md` was not touched in loop 1.
- `git diff --stat` (against `f510479`) — `docs/ux-spec.md | 180`, with 157 insertions and 32 deletions across both files. Every deleted line in `docs/ux-spec.md` is tenth-revision text corrected as listed in the table. No line older than the tenth revision was removed.
- `git log -3 --oneline` — `f510479` (the check), `dc3f77e` (this report, loop 0) and `364e78f` (the tenth revision). Nothing is committed by me.
- The Check command was not run: this is docs only, and this role has no Bash.

### Loop 1 — open issues added
- **A TalkBack player cannot play the board.** This predates phase 15. *(Superseded in loop 2: "outside 3.0.0" was a release-scope call that is not this role's. The producer has routed it to the game-designer and the founder; see Loop 2.)* The design item stands for whenever it is scheduled: the ux-designer with the game-designer (cell focus, per-cell peg/hole/selected state, jump actions), then the developer.
- **The F-1 ruling reads a passed criterion more precisely.** It is a UX ruling inside this role's authority, not a lowered bar: on the build loop 3 measured, both readings pass. The next checker should judge it against 15g's intent as §4.2.5 argues it.
- **F-3's reading rests on the tech-lead's own close-report text.** If the tech-lead's `docs/tech-design.md` §10 lists the start-path files differently, that text governs, as the note says.

### Loop 1 handoff
Next: producer — commit `docs/ux-spec.md` and this report on `feature/ux-spec-folds`, then dispatch a fresh ux-designer check of the amended revision (loop 2). Steps 2–5 of the handoff above are unchanged once that check passes. Step 4's device checks now run after the branch's own 15e sitting, in the same session.

## Loop 2 — the second fresh check's findings
Input: `.planning/reports/ux-spec-folds/ux-designer-check-loop2.md` (VERDICT FAIL; committed in `d518a98`), read in full. It closed loop 1's F-1 to F-10, and it judged the F-1 reading of 15g faithful. I edited only `docs/ux-spec.md`, plus this report.

**How the edits were made.** As in loop 1:
- Older decisions and criteria — 26, 15g's existing note, the §12 range and two §13 rows — were only **appended to**, with dated pointers.
- Text this revision wrote — 15v, the §11 note, 26b, §4.2.5 "The motion", §10's new row and the revision index — was **corrected in place**, because it is still the revision under review.
- **The first edition's §11 traversal orders were not touched.**

| Finding | Severity | What I did | Where |
|---|---|---|---|
| F-11 — 15v can't pass with animations on, because §10's entrance cascade moves the hero, ribbon and tiles after first paint | MAJOR | 15v is restructured. Each state is recorded twice, animations off and on. (1) With animations off, the position read: every frame is the layout. (2) With animations on, the motion read: two motions are allowed, the cascade and the room. Position is read from the first frame in which every band drawn at first paint has settled its entrance, and the settled pixels must equal the animations-off recording's (±1px). (3) If the chips land mid-cascade, no entrance restarts or changes its target. Every band ends where it sits with animations off, and the read starts once both the cascade and the room have settled. A band that ends elsewhere, or re-enters, fails. (4) The whole-picture rule applies in every frame, the cascade included. §4.2.5 gains "The entrance cascade and the room are independent": the cascade is a `graphicsLayer` offset, never a layout move, so 15s and 15u are untouched | criterion 15v; §4.2.5 "The motion" and the new paragraph after it |
| F-12 — the §11 note decided 3.0.0's scope, re-read the first edition, and left 26's scope over the tutorial board unstated | MAJOR | (a) **The scope decision is removed.** The note now reads: "Release scope is routed by the producer to the game-designer (and the founder); the ruling is pending at `.planning/reports/ux-spec-folds/game-designer-talkback-board.md`. Until it lands, this document rules neither the board's semantics nor its release." (b) **The gap is recorded honestly.** The first-edition orders listed the board as a stop and the build has never met that. Those orders stand as written, as the design statement the build falls short of. The note also names the shortfall against rules/compose.md's "contentDescription or semantics role on every interactive element". 26b's order describes today's build so that the device pass can be read, and does not relax the first edition. The words "names a position, not a focus stop" are gone. (c) **Criterion 26 gains a dated pointer**: whether the tutorial board is inside 26 is pending the same ruling, and a pass of 26 records the board's result separately without deciding it. The gameplay board predates the refresh and is covered by the §11 gap. 26b's paragraph now points to the same pending ruling instead of calling the board "a separate item" | §11 "Screen-reader traversal" (recorded gap); criterion 26 pointer; criterion 26b's board paragraph; the revision index |
| F-13 — 15g's pointer over-states ("no footer that keeps the tiles still can pass") | INFO | Fixed, to the true claim: no footer ratio meets that reading and 15p part 1 together, except on a knife edge under 0.1dp wide on one window (§4.2.5) | criterion 15g's 2026-10-21 note |
| F-14 — a late chip's room opens ≈0.1 s before the chip can be seen | INFO | Ruled, because it is cheap and makes §4.2.5's "no gap appears ahead of its chip" true to the eye. **A chip that arrives after first paint takes no stagger delay**, so its fade starts in the frame its room starts opening. A chip present at first composition keeps its cascade order | §4.2.5 "The motion"; §10's D-03n row |
| F-15 — §12's tall-phone range is pre-D-03n | INFO | A dated pointer: the range is the 3 : 1 split's, and under D-03n the stage is 326.3 / 230.3 / 134.3dp on QA's window, with the tiles fixed | §12 |
| F-16 — a bare "R-3" in the revision index | INFO | Fixed: "O-2 and `ux-designer-15p.md` R-3 (not §7's R-3)". The same line now says the shape is decided "but for one named edge case" by the labels, which matches §4.2.6 rule 4 | revision index, D-03o bullet |
| F-17 — two superseded §13 rows have no pointer of their own | INFO | Fixed: a dated "Superseded 2026-10-21 — D-05e" note in each row's Fix cell, pointing to the 2026-10-21 table | §13, both `GameFlowTest.kt:129` rows |
| F-18 — 15v's bench use rests on precedent | INFO | Fixed. 15v now adds "if the tech-lead's text says otherwise, it governs", and "QA restores `wm size`, `wm density` and the seeded saves, as H-3 did" | criterion 15v |

The revision index gains a dated "after a second fresh check" paragraph listing these changes.

### Drafted for the producer: the board's TalkBack gap (F-12), the UX recommendation
The producer has routed release scope to the game-designer (and the founder), with the ruling pending at `.planning/reports/ux-spec-folds/game-designer-talkback-board.md`. The spec rules nothing on it. As input to that ruling, the ux-designer's view:

**Options:**
1. Ship 3.0.0 with the gap recorded as a rules/compose.md deviation.
2. Give the board minimal semantics in 3.0.0. That means one labelled node per board saying what it is and its state, for example "Board, English 33, 12 pegs left"; on the tutorial, the step's board and its peg count. It would have its own criterion, and a developer task on `bugfix/home-chip-arrival`, which already sits a 15e. It does not make the game playable without sight.
3. Full TalkBack play: cell focus, per-cell peg/hole/selected state, and jump actions. This is a design of its own and too large for the 3.0.0 deadline.

**Recommendation:** option 2 now, and option 3 as a scheduled post-3.0.0 design (ux-designer with game-designer).
- It meets the letter of rules/compose.md for the one interactive element that misses it.
- It tells a TalkBack player what the region is instead of skipping it silently.
- It costs one semantics node per board, on a branch that is already reshaping these screens.
- Option 1 ships a known rule gap on the screens the player uses most.

If the ruling picks option 2, I write the decision and its criterion in a follow-up spec pass.

### Loop 2 checks
Each command was run read-only by a haiku Explore helper in `/Users/ermanergoz/Desktop/PegSolitarie-ux`, after the last loop-2 edit to `docs/ux-spec.md`:
- `grep -n 'once with OS animations off and once with them on' docs/ux-spec.md` — pass, :4721 (F-11).
- `grep -n -E 'If the chips land while the cascade is still running|The entrance cascade and the room are independent' docs/ux-spec.md` — pass. Hits at :2576 and :2578 (§4.2.5) and :4735 (15v part 3) (F-11).
- `grep -n 'takes no' docs/ux-spec.md` — pass. :2570 holds the late chip's zero stagger (F-14); :340 is the revision index; :3961 is an older unrelated line.
- `grep -n -E 'outside 3\.0\.0|names a$|names a position, not a focus stop' docs/ux-spec.md` — pass, no hit, exit 1: the scope decision and the re-reading are gone (F-12).
- `grep -n 'game-designer-talkback-board.md' docs/ux-spec.md` — pass, three hits: the §11 note :4077, criterion 26's pointer :4876, and 26b :4917 (F-12).
- `grep -n 'Recorded gap, 2026-10-21' docs/ux-spec.md` — pass, :4066 (F-12).
- `grep -n -E 'Tutorial order: pips → Skip → board → caption → CTA|back → title → timer → board → dock' docs/ux-spec.md` — pass, :4058 and :4059: the first-edition orders are unchanged (F-12).
- `grep -n 'Whether the \*\*tutorial board\*\*' docs/ux-spec.md` — pass, :4873, criterion 26's pointer (F-12).
- `grep -n 'knife edge under 0.1dp wide on one window' docs/ux-spec.md` — pass, :4503 (F-13). `grep -n 'no footer that keeps the' docs/ux-spec.md` — pass, no hit, exit 1.
- `grep -n "The ≈150–280dp range above is the 3 : 1 split's" docs/ux-spec.md` — pass, :4142 (F-15).
- `grep -n -E 'O-2/R-3|\(R-3\)' docs/ux-spec.md` — pass, no hit, exit 1 (F-16).
- `grep -n 'Superseded 2026-10-21 — D-05e, §4.3.2' docs/ux-spec.md` — pass, :4157 and :4223 (F-17).
- `grep -n "if the tech-lead.s" docs/ux-spec.md` — pass, :4719; and `grep -n 'restores `wm size`' docs/ux-spec.md` — pass, :4723 (F-18).
- `grep -o -E '^(1[0-9][a-z]?|2[0-9][a-z]?)\. ' docs/ux-spec.md | sort | uniq -d` — pass, empty: no duplicate criterion label.
- `git status --short` — ` M .planning/reports/ux-spec-folds/ux-designer.md`, ` M docs/ux-spec.md`. `docs/ui/DESIGN-SYSTEM.md` was not touched.
- `git diff --stat` (against `d518a98`) — `docs/ux-spec.md | 114`, with 88 insertions and 34 deletions across both files. The deleted spec lines are this revision's own text, corrected as the table lists. No first-edition or earlier-amendment line was removed.
- `git log -2 --oneline` — `d518a98` (check loop 2), `de3477c` (this report, loop 1). Nothing is committed by me.
- The Check command was not run: this is docs only, and this role has no Bash.

### Loop 2 handoff
Next: producer —
1. Commit `docs/ux-spec.md` and this report on `feature/ux-spec-folds`, then dispatch a fresh ux-designer check (loop 3).
2. The board's TalkBack gap waits on the game-designer's ruling at `.planning/reports/ux-spec-folds/game-designer-talkback-board.md`, and on the founder if it reaches Gate 2. My recommendation is above. That ruling does not block this branch: the spec now records the gap and rules nothing on it.
3. Steps 2–5 of the original handoff stand. Add option 2's node and criterion to `bugfix/home-chip-arrival` only if the ruling picks it and I have written it into the spec.

## Loop 3 — the third check, and the TalkBack board contract
Date: 2026-10-23. Inputs, all read in full:
- `.planning/reports/ux-spec-folds/ux-designer-check-loop3.md` (VERDICT FAIL; `f53cb4e`);
- the game-designer's scope ruling, `game-designer-talkback-board.md` (GD-TB-1 to GD-TB-5, M1–M8, TB-1 to TB-12);
- the founder's answer to Q-023, option 1: the board becomes playable with TalkBack in 3.0.0 (`.planning/questions/FOUNDER.md`, `415e5b2`).

The ruling supersedes my loop-2 recommendation of minimal semantics only. I edited only `docs/ux-spec.md`, plus this report.

**How the edits were made.**
- Older text was only **appended to**, with dated pointers: §6.5, criteria 26 and 15s's header note, the §11 first-edition orders (untouched), and §4.2.5's reason 1.
- Text this revision wrote and the check found wrong was **corrected in place**, each with a dated note where the wording changed meaning:
  - §11's recorded-gap paragraph: the replaced sentence is quoted in a note;
  - 26b: the superseded paragraph is quoted in its amendment note;
  - 15v and 15u, and §4.2.5's motion and cascade paragraphs: the corrected claim is named in a note;
  - §4.2.5's footer row and §10's D-03n row and reduced-motion bullet;
  - the 15s–15w block header: its old words are quoted.
- **New decision id: D-TB**, for the board's spoken contract. A grep finds it only in the new text, and finds no `D-04f`, `D-04g` or `D-06x` anywhere in the spec. Criteria keep the game-designer's ids, TB-1 to TB-12, so the ruling, the tech-lead's build brief and QA name the same things. A grep finds no duplicate criterion label.

### The check's findings
| Finding | Severity | What I did | Where |
|---|---|---|---|
| F-19 — §11 said the board's gap "predates phase 15, which neither made it nor closed it", which is false for the tutorial board | MAJOR | §11's record is now **split by board**. The **gameplay board**'s gap predates phase 15: it had no semantics before, and phase 15 gave it none. The **tutorial board**'s gap is **phase 15's own**: phase 15 built its live board (`interactive = true`, D-01a) with no label, role or action, against its own first-edition order, which named the board a stop. Before phase 15, onboarding was a static `BoardPreview` sheet. The replaced sentence is quoted in a dated note. Criterion 26's new pointer now agrees: by 26's words the tutorial board is a new interactive element that fails 26 today | §11 "Screen-reader traversal", the recorded gap; criterion 26's 2026-10-23 pointer |
| F-20 — 15v part 2's "no third motion" and pixel comparison fail a correct build (the blobs; the late chip's own entrance) | MINOR | The two allowed motions are limited to Home's **bands**, and the blobs are named outside the read. The second motion is "the chips' arrival": the room opening together with the late chip's own fade and slide. Positions are read from **edges**, never from pixel values inside the hero row and the tiles, which the blobs change | criterion 15v (preamble and parts 1–2) |
| F-21 — under three-button navigation, 360×640dp breaks 15v part 1, and §4.2.5's "9.6dp behind a 48dp bar" named the wrong window | MINOR | 15v now records **in gesture navigation**, the bench's resting mode, as loop 3 and H-3 did. A new part 5 rules the three-button window: 540dp usable, S₀ 204dp, ⅘ S₀ 163.2dp against both chips' 176dp. The tiles move 12.8dp there (44dp under 3 : 1); that is 15t's shortfall case, read by 15t's rule if recorded. §4.2.5 reason 1 names both windows — 6.4dp on QA's device in gesture navigation, 9.6dp on 15s's 24 + 48dp window — and records the one short window and why no ratio fixes it without breaking 15p part 1 | criterion 15v part 5; §4.2.5 reason 1 |
| F-22 — a band's test bounds move with the entrance's `graphicsLayer` offset | MINOR | §4.2.5 now says the cascade moves a band's **drawn** position and not its **layout**, and that Compose's bounds (`getBoundsInRoot()`, `positionInRoot()`) map through the layer, so a bounds test lets the cascade settle first. The corrected 2026-10-21 claim is named in a note. 15u composes Home and advances the clock past the cascade before the emission; a chip that lands mid-cascade is 15v part 3's device read | §4.2.5 "The entrance cascade and the room are independent"; criterion 15u |
| F-23 — three statements say the tiles or the footer always hold still | INFO | Fixed. Each now holds "wherever the stage can give the room", with the shortfall behaviour stated | §4.2.5 footer row and the motion paragraph; §10's D-03n row; §10's reduced-motion bullet |
| F-24 — §6.5 keeps the 3 : 1 range | INFO | Fixed: a dated pointer. Under D-03n, step 3 would leave 134.3–326.3dp blank on QA's window | §6.5, after the D-03l note |
| F-25 — the stage art drops out in one frame mid-transition | INFO | Ruled, because it is cheap. When a transition's final stage height is below `home_stage_art_min_size`, the art **fades out over the same `ENTRANCE_MS`** while it shrinks, and a chip leaving fades it back in. Above the minimum it only follows the stage | §4.2.5, the new "The stage art at the end of its range"; §10's D-03n row |
| F-26 — the 15s–15w header understates the pointers | INFO | Fixed. It names 15g's ruled reading, 15k part 2's record, and 15m's and 15r's pointers, and its old words are quoted | §14, the 15s–15w block header |

### The TalkBack board contract (D-TB), as written
- **§3 D-TB** — the decision and its reasons. The words, the order, focus and announcements are this role's; what must be said and operable is the game-designer's (M1–M8, GD-TB-2 to GD-TB-5).
- **§11.1** — the contract, on both boards:
  - **coordinates:** row and column from 1, as drawn, never mirrored; "up" is towards row 1;
  - **the board stop:** "Board, 32 pegs left", with no role, and a double-tap on it changes nothing;
  - **one stop per in-play cell,** row-major, with the cell's own rectangle as its focus rectangle, so explore-by-touch works;
  - **what a cell says,** in fixed order: position, content, "selected", the jump count (spoken with Show-moves on or off), "jump target", and the hint parts;
  - **the tap as the accessibility click,** with the action labels "select", "put down" and "jump here";
  - **one custom action per legal jump,** "Jump up to row 4, column 4", offered while nothing is selected or on the selected peg, and never on another peg while one is held (GD-TB-5);
  - **twelve announcement lines,** polite and once each, a newer one replacing an unspoken one. The three rejected-tap lines are neutral, so D-01c holds by ear;
  - **the focus table** (M7), including the banner and tutorial cases;
  - **Results:** the star row speaks "2 of 3 stars", and a summary is announced on opening;
  - **the tutorial:** step, completion and coach lines;
  - **a ruled exemption** from 26a's 48dp line for cell focus rectangles (≈42dp on 7 × 7 and ≈33dp on 9 × 9 at 360dp), on WCAG 2.2 SC 2.5.8's essential exception, because the board geometry cannot grow;
  - **TB-11's sighted-play guarantee.**
- **§8** — 29 strings and two plurals, all spoken and none drawn. `stuck_message`, `nowin_message`, `tutorial_step_progress`, the captions, `tutorial_done_caption` and `catalog_stars_earned` are reused.
- **§14** — TB-1 to TB-12 with the exact strings. 26b is amended so that the board stop and the cells come after the timer and after Skip; its superseded words stay in the note. On a build without D-TB the board's absence is not a 26b failure.
- **"Pending" pointers replaced.** §11, 26 and 26b now cite GD-TB-4, Q-023's answer (option 1, 2026-10-21) and the report path. The 2026-10-21 pointers stay as the record, each with a 2026-10-23 "Ruled" or "Amended" note below it.
- **§13** — a D-TB retarget table and an owed list. **§16** — traceability. **Revision index** — an "Eleventh revision" paragraph.

### Loop 3 checks
Each command was run read-only by a haiku Explore helper in `/Users/ermanergoz/Desktop/PegSolitarie-ux`, after the last loop-3 edit but one. That last edit was a line re-wrap at §11 :4196-4198, which changes no grep result.

**D-TB and the ruling's pointers:**
- `grep -c -E 'D-TB\b' docs/ux-spec.md` — 40. `grep -n -E 'D-TB\b' …` shows every hit in the eleventh revision's own text: the index :347-348, §3 :1208-1236, §8 :3875, §11 :4205 and :4225-4311, §13 :4537 and :4546, 26 :5203, 26b :5240-5246, the TB block :5269-5363 and §16 :5509-5511. The id is unique.
- `grep -n -E '^TB-([1-9]|1[0-2])\. ' docs/ux-spec.md` — pass: TB-1 to TB-12 at :5277 to :5359.
- `grep -n -E 'D-04f|D-04g|D-06[a-z]' docs/ux-spec.md` — no hit, exit 1: no collision with the hint branch or the audio amendment.
- `grep -o -E 'board_a11y_[a-z_]+|results_a11y_summary' docs/ux-spec.md | sort -u | wc -l` — 31 names, matching §8's table: 29 strings and two plurals.
- `grep -n -E 'board stop\*\*, `board_a11y_label`|\*\*the board stop\*\*' docs/ux-spec.md` — :5226 (gameplay) and :5237 (tutorial): 26b places the board.
- `grep -n -E 'Q-023' docs/ux-spec.md` — eight hits, in the index, §3, §11, §11.1, 26, 26b and §16.
- `grep -n -E 'pending the|is pending|ruling is pending|pending at' docs/ux-spec.md` — two hits, both kept records: the loop-2 index paragraph :340, and criterion 26's 2026-10-21 pointer :5195, which its 2026-10-23 "Ruled" note answers. §11's "ruling is pending" sentence is gone; 26b's is quoted in its amendment note.

**The check's findings:**
- `grep -n 'which neither made it nor closed it' docs/ux-spec.md` — no hit, exit 1. The live sentence is gone; it survives only inside the correction note's quotation, which is wrapped across a line (F-19).
- `grep -n -E "The \*\*tutorial board\*\*'s gap is phase 15's own|The \*\*gameplay board\*\*'s gap predates phase 15" docs/ux-spec.md` — :4187-4188 (F-19).
- `grep -n -E 'Every recording is made in gesture navigation|Three-button navigation is not this criterion|outside this read|Positions are read from \*\*edges\*\*' docs/ux-spec.md` — :5034, :5045, :5058 (F-20, F-21).
- `grep -n -E 'composed and the clock advanced past its entrance cascade|test bounds include|map through the layer' docs/ux-spec.md` — :2646 (§4.2.5), :5013-5014 (15u) and :358 (index) (F-22).
- `grep -n -E 'wherever the stage can give the room' docs/ux-spec.md` — :2494, :2495, :4021 (F-23). The reduced-motion bullet carries "wherever the stage can give the room" across a line break.
- `grep -n -E 'The stage art at the end of its range|134.3–326.3dp' docs/ux-spec.md` — :2636 (F-25) and :3514 (F-24).
- `grep -n 'Header corrected 2026-10-23' docs/ux-spec.md` — :4986 (F-26).

**Whole-document checks:**
- `grep -o -E '^(1[0-9][a-z]?|2[0-9][a-z]?|TB-[0-9]+)\. ' docs/ux-spec.md | sort | uniq -d` — empty: no duplicate criterion label.
- `grep -n -E '^revised: |Eleventh revision' docs/ux-spec.md` — :7 `revised: 2026-10-23`, :344.
- `git status --short` — ` M docs/ux-spec.md`, then this report once written. `docs/ui/DESIGN-SYSTEM.md` was not touched in loop 3.
- `git diff --stat` (against `415e5b2`) — `docs/ux-spec.md | 546`, with 491 insertions and 55 deletions. The deletions are the in-place corrections listed above, all of them tenth-revision text plus the frontmatter date. No first-edition or earlier-amendment line was removed.
- `git log -3 --oneline` — `415e5b2` (Q-023's answer), `f53cb4e` (check loop 3) and `df37c3e` (Q-023 queued). Nothing is committed by me.
- The Check command was not run: this is docs only, and this role has no Bash.

### Loop 3 — open issues
- **The cell focus-rectangle exemption is a ruling against 26a's letter.** It rests on WCAG 2.2 SC 2.5.8's essential exception and on swipe navigation. The next checker should judge it. If the tech-lead or QA finds a board-wide focus pattern that meets 48dp without changing the board, that is better, and it comes back to me.
- **Timing between announcements and focus moves** — the jump by action, the step change, completion — is specified as "the announcement first, then the newly focused stop". Whether TalkBack keeps that order is for the device pass (TB-7, TB-9). If it cuts one off, the finding comes back to me with the speech log.
- **The board stop's double-tap.** TB-1 requires that a double-tap on the board stop changes no cell. The game-designer flags that TalkBack may inject a tap at a non-clickable node's centre. The mechanism is the developer's, and the device pass decides it.
- **26b's timing across two branches.** If `bugfix/home-chip-arrival`'s QA passes 26b before `feature/talkback-board` merges, the board's absence is not a 26b failure, as the amendment note says. The later branch's QA re-reads 26b with the board.
- **`feature/talkback-board` and `feature/colour-vision` both touch `BoardCanvas.kt`,** as the game-designer notes. Their merge order is the tech-lead's.

### Loop 3 handoff
Next: producer —
1. Commit `docs/ux-spec.md` and this report on `feature/ux-spec-folds`, then dispatch the fresh Fable check (loop 4).
2. The tech-lead writing the `feature/talkback-board` brief reads §11.1, §8's D-TB table, TB-1 to TB-12 and §13's D-TB rows. Those, not my loop-2 recommendation, are the contract.
3. The android-game-dev on `feature/talkback-board` builds D-TB test-first against TB-1 to TB-12. Then the code-auditor; then the qa-engineer's TalkBack device pass (TB-7, TB-9, TB-10 and 26b, with a working TTS engine confirmed first); then the tech-lead's sign-off.
4. `bugfix/home-chip-arrival`'s scope is unchanged by this loop, apart from the 15u, 15v and F-25 refinements to D-03n, which its developer and QA read from the spec.
