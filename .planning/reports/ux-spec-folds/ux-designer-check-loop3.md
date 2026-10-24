# ux-designer report — ux-spec-folds, check loop 3 (a review of a spec revision this agent did not write)
Date: 2026-10-21
Branch: feature/ux-spec-folds (committed by the producer). Reviewed: `docs/ux-spec.md` (5,093 lines) and `docs/ui/DESIGN-SYSTEM.md` (121 lines) in the worktree `PegSolitarie-ux`, head `2e61c34` per the dispatch. The revision is everything dated 2026-10-21 since `c9228a9`. Loop 3's own changes are the loop-2 fixes `f2e7015` (reflog line 8).
Skills loaded: ergoz-soft:standards (role ux-designer). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch. Also the worktree's `.claude/rules/lessons.md`. Then ui-ux-pro-max, android-accessibility and compose-skill.
Ran in parallel with: the game-designer's TalkBack-board scope ruling (`game-designer-talkback-board.md`). It is not under review and I did not read it.
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort). **Staffing deviation:** the ux-designer is pinned to fable (`agents/ux-designer.md`). This check ran on Opus under the founder's Q-019 ("Opus for every Fable-pinned role until 3.0.0 is published", `.planning/questions/FOUNDER.md`), as the dispatch directs. The author also ran on Opus. Independence therefore rests on one fact: this is a fresh agent that never read the author's report.
Fan-out: none. Two read-only helpers (general-purpose, haiku) ran `wc`, `grep`, `ls`, `stat` and `shasum`, which my tools cannot run. Neither ran any git command, and neither wrote anything. I read the branch's git metadata myself, as plain files.
Loops: 2 (this is the third check round; the dispatch calls it loop 3)
Read in full:
- `.planning/reports/15-founder-feedback/tech-lead-signoff-close.md`, `tech-lead-signoff.md`, `tech-lead-fonts.md`, `qa-engineer-loop3.md` and `qa-engineer-15e.md`;
- `.planning/reports/ux-spec-folds/ux-designer-check.md` (loop 1) and `ux-designer-check-loop2.md` (loop 2);
- `docs/ux-spec.md`, lines 1–5093 in consecutive reads, and `docs/ui/DESIGN-SYSTEM.md`.

Not read: `.planning/reports/ux-spec-folds/ux-designer.md` (the author's report), `game-designer-talkback-board.md` and `.planning/log/`.
Lessons applied: none. The snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist.

## Summary
- **Loop 2's eight findings are answered.** F-11 and F-13 to F-18 are closed. F-12 is closed except for one sentence.
- **FAIL on one MAJOR finding (F-19).** §11's recorded gap says the board's TalkBack gap "predates phase 15, which neither made it nor closed it". That is false for the tutorial board, which phase 15 built, and it contradicts criterion 26's own new pointer. It is also the provenance the routed release-scope ruling reads.
- **Three MINOR findings sit in the new 15v and 15u text.** F-20: the blobs and a late chip's own entrance break 15v's "no third" motion. F-21: three-button navigation at 360×640dp breaks 15v part 1. F-22: `graphicsLayer` offsets show up in test bounds. Four INFO items follow.
- **What holds:** folds a–e, every id, 26b's order against §11 and §8, 18f's table, and every recomputed number. The documents I read are `2e61c34`'s: both files' mtimes predate that commit.

## Loop 2's findings: closed or answered
| Loop-2 finding | Where it is answered | Verdict |
|---|---|---|
| F-11, MAJOR: 15v cannot pass with animations on | 15v `docs/ux-spec.md:4716-4743`: two recordings each, animations off then on. The position read starts once the cascade has settled, and part 3 covers a chip that lands mid-cascade. See also §4.2.5 `:2576-2586` and §10 `:3908` | **Closed.** Everything F-11 owed is there. The new text raises F-20 and F-22 |
| F-12, MAJOR: the §11 note decided 3.0.0's scope | §11 `:4066-4078`: no release ruling; the first-edition orders "stand as written … the design statement the build falls short of"; the scope is routed and pending. Criterion 26 gains a pointer (`:4873-4878`), and 26b points at the ruling (`:4910-4917`) | **Closed, except the honesty half.** The release decision is gone, and 26 and 26b point at the pending ruling. One provenance sentence is false: F-19 |
| F-13, INFO: 15g's pointer over-stated the argument | 15g `:4497-4505` | Closed. "A knife edge under 0.1dp wide on one window" matches §4.2.5 `:2534-2535`: (0.176432 − 0.176269) × 407.9 = 0.066dp |
| F-14, INFO: a late chip's room opened before the chip was visible | §4.2.5 `:2569-2572`; §10 `:3908` | Closed. A late chip takes no stagger delay, and its fade starts with its room |
| F-15, INFO: §12 kept the 3 : 1 range | §12 pointer `:4142-4144` (326.3 / 230.3 / 134.3dp) | Closed in §12. The same range survives in §6.5: F-24 |
| F-16, INFO: one bare "R-3" | `:314` | Closed. Every other "R-3" in the revision is qualified (`:1000`, `:1011`, `:2630`, `:2638-2639`) |
| F-17, INFO: two §13 rows without pointers | `:4157`, `:4223` | Closed |
| F-18, INFO: 15v's bench use rested on a precedent | 15v `:4719-4723`: "if the tech-lead's text says otherwise, it governs", and the restore of `wm size`, `wm density` and the saves | Closed |

Loop 1's F-1 to F-10 stay closed. Loop 3's changes are the ones its revision index lists (`:337-342`). Of them, only 15g's pointer wording touches a loop-1 closure (F-13 above). Every other closure location loop 2 listed still reads as loop 2 accepted it.

## Findings

**F-19 — MAJOR — §11's recorded gap misstates where the tutorial half of the gap came from.** `docs/ux-spec.md:4071-4073`, against `:4873-4874`, `:377`, `:425-434`, `:1203` and `:4058`
- **The note says:** "The gap predates phase 15, which neither made it nor closed it."
- **True of the gameplay board.** It had no semantics before phase 15, and phase 15 did not give it any.
- **False of the tutorial board.**
  - Before phase 15, onboarding was a static sheet. Its illustrations went through `BoardPreview`, which hard-codes `interactive = false` (§2 `:377`; D-01a `:425-434`).
  - Phase 15 built the tutorial's live board: `TutorialScreen.kt:254-263`, `interactive = true`, with no label, role or action.
  - Phase 15's own first edition named that board a stop (`:4058`, "pips → Skip → board → caption → CTA").
- **The spec contradicts itself.** Criterion 26's new pointer calls the same board "§4.1's one interactive element, on a screen this refresh made new" (`:4873-4874`).
- **Why MAJOR:**
  - F-12's closure condition was an honest record of the gap against the first edition.
  - This sentence is the provenance the routed release-scope ruling reads: 26 (`:4877-4878`) and 26b (`:4916`) both send the reader to "§11's recorded gap".
  - A new interactive element that this release ships short of rules/compose.md is a different input to that decision from inherited debt. As written, the record tells the decider it is inherited.
- **Owed by the author:**
  - Split the sentence. The gameplay board's gap predates phase 15. The tutorial board's gap was introduced by phase 15 (D-01a), against that phase's own first edition.
  - Optionally, align 26's pointer with it: by its words, the tutorial board is a new interactive element that fails 26 today, and the routed ruling decides the release consequence.

**F-20 — MINOR — By its letter, 15v part 2's "no third" motion rule and its pixel comparison fail a correct build.** `:4727-4734`
- **Two more motions are on screen.** 15v part 2 reads: "Two motions are allowed and no third."
  - With animations on, Home's two ambient blobs drift in every frame: §10 `:3906-3907` ("the blobs remain the only drifting elements"), DESIGN-SYSTEM.md:104-105, and `ClayAnim.kt:34-38` (an 8s cycle at alpha 0.10). With animations off they are skipped.
  - A chip that arrives after first paint fades and slides in on its own entrance (`:2569-2572`). Part 2 covers that only through the parenthetical "(§4.2.5, "The motion")".
- **The pixel comparison fails too.** Part 2 requires "the hero row's and the tiles' pixels equal those of the animations-off recording (±1px)". The blobs pass behind the gradient title, so the pixel values in the hero row differ between the two recordings even when nothing moved. The ±1px tolerance suggests a reading of position.
- **Owed by the author:**
  - Limit the two motions to Home's bands, and put the blobs outside the read.
  - Name the late chip's own entrance as part of the second motion.
  - Read the hero row and the tiles by their edges, not by their pixel values.

**F-21 — MINOR — Under three-button navigation, 15v at 360×640dp fails a correct D-03n build, and §4.2.5's "9.6dp behind a 48dp three-button bar" reads as if it covered that case.** `:4716-4726`, `:2494-2496`, `:972-974`
- **The window 15v reads.** It has a 52dp status bar (`qa-engineer-loop3.md`, Device and build: 360×640dp through `wm size` and `wm density`, a 52dp status bar, 564dp usable with the gesture handle).
- **With a 48dp three-button bar:**
  - it leaves 540dp, so S₀ = 204dp (228 − 24) and ⅘ S₀ = 163.2dp;
  - both chips' room is 176dp, and both chips land in one emission (sign-off Decision 3);
  - so by §4.2.5's own rule (`:2464`) the tiles move 12.8dp down;
  - but part 1 holds them to ±1px in every frame.
- **Nothing pins the navigation mode.**
  - 15v names no mode.
  - The bench rests in gesture navigation (`qa-engineer-15e.md`, Open issues).
  - Criterion 29(2) (`:4930-4931`) switches the bench to three-button in the same session.
- **`:2496`'s 9.6dp is computed on a different window.** It is ⅘ × 232 − 176, on the design baseline's 24dp status bar (568dp usable), not on the device window 15v reads.
- **D-03n part 1 itself holds.** "Every phone window §14 measures" (`:972-974`) is true: §14's windows supply 24 + 48dp insets, and 15s passes there. The gap is the device read only.
- **Owed by the author:**
  - Either 15v records in gesture navigation, as loop 3 and H-3 did, or it reads a window where R > ⅘ S₀ by 15t's rule.
  - Name the window behind `:2496`'s 9.6dp.

**F-22 — MINOR — "The cascade … does not touch the bounds 15s and 15u pin" is not true of the bounds a Compose test reads.** `:2576-2578`, `:4707-4713`
- **Why the test bounds move.**
  - `clayEntrance` is a `graphicsLayer` with `translationY` (`Clay.kt:197-200`).
  - It is applied outermost on every band's `Card` (`HomeScreen.kt:487`, `:659`), so each band's click semantics sit inside the layer.
  - `getBoundsInRoot()`, `boundsInRoot()` and `positionInRoot()` all map through a layer's translation. So while the cascade runs, a band's test bounds move although its layout does not.
- **What that does to 15u.**
  - 15u runs "with OS animations on and the test clock paused".
  - A test that composes Home and emits the chip before advancing past the cascade (order × 40ms + 240ms) sees the tiles' bounds change frame by frame. That fails a correct build.
  - A chip landing mid-cascade cannot be checked against "settled bounds" on the JVM at all. 15v part 3 already covers that case on the device.
- **Where the claim came from.** This corrects the loop-2 check's own note under F-11 ("15u … asserts layout bounds, which a graphicsLayer translation does not move"), which the author relied on.
- **Owed by the author:**
  - 15u states that Home's entrance has settled before the emission.
  - §4.2.5 says that the cascade moves a band's drawn position and not its layout, and that test bounds include the drawn offset.

**F-23 — INFO — Three statements say the tiles or the footer always hold still; §4.2.5's rule moves them in the shortfall case.** `:2446`, `:3908`, `:3923-3925`, against `:2460` and `:2464`
- Once R > ⅘ S₀ — in 15t at 2.0, and on F-21's window — the footer is S₀ − R and the tiles move by the shortfall.
- Three statements do not allow for that:
  - the table's Footer row: "Holds still. Fixed for the window.";
  - §10's row: "the hero row and the mode tiles hold still in every frame";
  - §10's reduced-motion bullet: "because the room always comes out of the stage".
- The Tiles row (`:2445`) already carries the qualifier. Add "wherever the stage can give the room" to the three.

**F-24 — INFO — §6.5 keeps the 3 : 1 stage range.** `:3442`
- The D-03l note there reads "≈150–280dp on a 6.7-inch phone, §4.2.4". That is F-15's range, and only §12 gained a pointer for it.
- Under D-03n, degradation step 3 would leave a blank of 134–326dp on QA's native window.

**F-25 — INFO — The stage art drops out in one frame mid-transition.** `:2442`, `:2066-2069`, `:2569`, against the 360×640 rows at `:2486-2488`
- A two-save cold start on 360×640dp shrinks the stage from 182.4 to 6.4dp over `ENTRANCE_MS`.
- The art follows it down to `home_stage_art_min_size` (48dp), then leaves composition in a single frame.
- No criterion asserts against this. It is a polish choice for the author: fade the art out below some edge, or let it shrink to 0.

**F-26 — INFO — The 15s–15w block header understates the revision's pointers.** `:4680-4681`
- It says "Every criterion above stands; 15r gains a pointer".
- The same revision also rules 15g's reading (`:4497`) and adds pointers to 15k (`:4576`) and 15m (`:4619`). Name them, or say "with the dated pointers above".

## Fresh pass: what holds
- **Folds a–e.** Each still matches the tech-lead's text word for word:
  - fold a: `:3566-3571`, `:3621-3623`, `:3539-3541` and `:4413-4414`;
  - fold b: `:3573-3576`;
  - fold c: `:4576-4581`;
  - fold d: D-05f `:1169-1187`, §6.2 `:3316-3340`, criterion 29 `:4924-4935` and DESIGN-SYSTEM.md:44-48;
  - fold e: DESIGN-SYSTEM.md:58-74, with the old sentence kept.
- **Ids.** Loop 3 adds none. The helper's greps put every id the revision introduced, from 15s to 15w, 18f, 18g, 26b, 29, D-03n, D-03o, D-05e and D-05f, only in the revision's own text.
- **26b against §11 and §8.**
  - The three orders are §11's orders without the board.
  - Every string 26b names is in §8's tables.
  - 26b and §11 both describe today's build, Skip → caption and timer → Undo, and both are consistent with `BoardCanvas.kt:196-212` and `TutorialScreen.kt:254-263`.
- **The timer's spoken form.**
  - 18f's nine `elapsedSeconds` values map onto §4.3.2's nine rows.
  - The string count agrees in three places (`:325-326`, `:3794-3803`, `:5078-5079`): two strings and two plurals, plus one string with the fallback.
  - 18g's fallback uses `duration_minutes` and `duration_under_a_minute`.
- **Feasibility on this branch.**
  - `clayEntrance` takes its delay as `order × 40ms` inside a `LaunchedEffect(Unit)` (`Clay.kt:186-195`). So giving a late chip no stagger is a call-site change.
  - Chips inserted ahead of the tiles change the tiles' `entranceOrder` argument (`HomeScreen.kt:407-443`) but cannot restart their entrance, which is what 15v part 3 requires.
  - `ENTRANCE_EASING` is a deceleration curve (`ClayAnim.kt:111-113`), consistent with "no overshoot".
- **No passed criterion silently changed.** Loop 3 touches:
  - the wording of 15g's ruled pointer, which changes no verdict;
  - 15v, which has never been sat;
  - a pointer on 26, still PARTIAL since phase 15;
  - the new criterion 26b.

## Numbers checked against their sources
- **Loop 3's new or changed figures, all recomputed:**
  - 15g's "38 %": 81.6 ÷ (134.3 + 81.6) = 37.8 %;
  - §12's 326.3, 230.3 and 134.3dp, from §4.2.5's table;
  - §4.2.5's "≈114dp": 81.6 + 16 + 16;
  - the knife edge: 0.066dp.
- **F-21's figure is mine:** 640 − 52 − 48 = 540dp; S₀ 204dp; R₂ 176dp; 176 − 163.2 = 12.8dp. Its inputs are from `qa-engineer-loop3.md`: 564dp usable with the 52dp status bar, and S₀ 228dp from the 15k band edges.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-ux/.planning/reports/ux-spec-folds/ux-designer-check-loop3.md`: this report, VERDICT FAIL. It is not committed; the producer commits it. I edited no other file.

## Checks
- **Read on the documents, all pass:**
  - `docs/ux-spec.md`, lines 1–5093, in consecutive reads;
  - `docs/ui/DESIGN-SYSTEM.md` in full;
  - the seven input reports listed under "Read in full".
- **Read on the code, all pass:**
  - `Clay.kt:140-235`, `ClayAnim.kt:20-119`;
  - `HomeScreen.kt:380-499` and `:590-669`;
  - `BoardCanvas.kt:186-215`, `TutorialScreen.kt:236-265`.
- **Read on the git metadata, as plain files:**
  - `.git/worktrees/PegSolitarie-ux/HEAD` gives `ref: refs/heads/feature/ux-spec-folds`.
  - `.git/refs/heads/feature/ux-spec-folds` gives `df37c3e0…`.
  - The reflog `.git/logs/refs/heads/feature/ux-spec-folds` shows `2e61c34` (line 9, 22:34:06 +0300), then `d663b55` "record the game-designer TalkBack board scope ruling" and `df37c3e` "queue Q-023". Its line 8 is `f2e7015`, "close the spec-check loop-2 findings F-11 to F-18".
- **Helper 1 (haiku, no git), all pass:** `wc -l`, `grep -n '^#'`, `grep -n '2026-10-21'` over both documents, the id greps (`15s|15t|15u|15v|15w`, `18f|18g|26b`, `D-03n|D-03o|D-05e|D-05f`, `S₀`), `grep -i 'game-designer'`, `grep -i cascade`, `grep stagger` and `ls .planning/reports/ux-spec-folds/`.
- **Helper 2 (haiku, no git):**
  - `stat -f '%m %Sm %N'` on both documents: `ux-spec.md` 22:32:12 and `DESIGN-SYSTEM.md` 21:50:39. Both predate the `2e61c34` commit (22:34:06), so the text I read is that head's and not the later record commits'.
  - `shasum -a 256`: `ux-spec.md` `b8ebca9c…863f`, `DESIGN-SYSTEM.md` `4f21a067…02a9`.
  - `ls -la`: `game-designer-talkback-board.md` now exists, written at 22:40.
- **The Check command was not run.** This is a documents-only review, and my role has no Bash.
- I did not read `.planning/log/`, the author's report or the game-designer's report.

## Open issues / risks
- **The branch has moved past the head under review.** The ref is `df37c3e`, two record commits after `2e61c34`. The mtimes show neither commit touched the two documents. The producer can confirm with `git diff 2e61c34 df37c3e -- docs/`.
- **The pending ruling appears to have landed.** The game-designer's report exists, and Q-023 ("TalkBack-playable board for 3.0.0") is queued. The spec's "pending" pointers in §11, 26 and 26b will need folding against that ruling. F-19's fix should be made in the same pass, so that the provenance agrees with the ruling's premise.
- **F-22 corrects the previous checker.** It reverses a technical claim the loop-2 check made, and the author built on that claim. Whoever writes 15u's test should read F-22 before writing it.
- **Staffing deviation.** The author and all three checkers ran on Opus under Q-019. The only independence is fresh agents that did not read the author's report.

## Handoff
Next: producer —
- Return **F-19 (MAJOR)** and **F-20, F-21 and F-22 (MINOR)** to the revision's author, the ux-designer, on `feature/ux-spec-folds`. F-23 to F-26 are INFO, for the same pass at the author's discretion.
- Fold the game-designer's ruling in the same pass, if it has landed. F-19's provenance sentence and the "pending" pointers at `:4076-4078`, `:4873-4878` and `:4915-4917` all change with it.
- Then dispatch a fresh check, loop 4. Folds a–e, F-1 to F-18 and 26b's order itself need no rework.

VERDICT: FAIL
