# ux-designer report — colour-vision (record the founder's Q-027 answer in the spec)
Date: 2026-10-24
Branch: feature/colour-vision (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; also the worktree's `.claude/rules/lessons.md`. Then ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: colour-vision (code-auditor re-review of the same branch)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/ux-designer.md` pins fable at xhigh. This ran on Opus because the Fable limit is reached, under the founder's Q-019 answer (option 3, re-confirmed 2026-10-24: every Fable-pinned role runs on Opus until 3.0.0 is published). Mitigation: this is a text-only record of a founder answer. CV-AC07's test re-proves the ratios from `Color.kt` on every Check, and `BoardMarksTest` pins the colour.
Fan-out: none. No helper was spawned.
Loops: 0
Lessons applied: none (the 0.6.2 snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist in the worktree)

## Summary
- Q-027 (answered 2026-10-24, option 2, "stars only") is recorded as **CV-13b**: the standard palette's unearned star takes `onSurfaceVariant`, `#635F69` in light and `#A79FB5` in dark. The earned star, the board and every token value are unchanged.
- All five places the developer's Open issue 1 named are updated. So are eight more that stated the old value or depended on it, §12.4 among them. Superseded text is kept, each with a dated pointer.
- A new **§13** records the answer, what changes, the recomputed contrast, what stays, and why no acceptance criterion changes. CV-AC05's cell gains one pointer.
- **Recomputed contrast**, normal vision and monochromacy: light 5.58 (background) and 5.31 (catalogue card); dark 6.95 and 5.59. The old values were 1.29 / 1.23 and 1.49 / 1.20. The every-vision floors are §3.5 row 11's: ≥ 5.02 in light and ≥ 5.56 in dark.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/docs/ux/colour-vision.md`, amended in the house style.
  - **The developer's list, item by item:**
    1. **CV-13a.** Following the house style, it is not rewritten in place. **CV-13b** sits directly after it and amends CV-13 and CV-13a, as CV-10a and CV-13a did before it.
    2. **§3.4's Standard unearned row:** a dated pointer to the hollow star in `onSurfaceVariant`, with both hexes.
    3. **§12.2:** a pointer on "The unearned star keeps `outlineVariant`", and one closing the "Its cost" block.
    4. **§12.3's colour line:** a pointer saying `onSurfaceVariant` in both palettes.
    5. **§12.6:** three pointers: F-CV4 is fixed, the question has been queued as Q-027, and it is answered.
  - **Beyond the list,** wherever the old value or its consequence was stated:
    - the front-matter `revised:` line;
    - §0's "What this does not touch" (a second amendment: one colour now changes too);
    - §2's star row (its "What is missing" cell);
    - a note above §3.4's table (the Standard column is unchanged except for the unearned star);
    - §3.5 row 11's "Today" cell;
    - §3.7;
    - CV-AC05's cell (the off-palette exception now covers the stars' colour as well as their form);
    - §7's intro, F-CV4's row and the founder-question paragraph;
    - §10 item 2 (closed, the original kept as the record);
    - §12's intro;
    - **§12.4**, whose standard wording reads "the hollow star in `outlineVariant`". The list missed this one;
    - §12.7's first bullet.
  - **New §13**, "Amendment for the founder's Q-027 answer: the standard empty star":
    - 13.1, the answer;
    - 13.2, what changes;
    - 13.3, the contrast, recomputed;
    - 13.4, what stays, stated plainly;
    - 13.5, the acceptance criteria (no change, with the reasons).
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/ux-designer-q027.md`: this report.

## Checks
- `Read`, in full:
  - `docs/ux/colour-vision.md` (967 lines, two reads);
  - `.planning/reports/colour-vision/ux-designer-star-ruling.md`;
  - `.planning/reports/colour-vision/android-game-dev-loop1.md`;
  - Q-027's entry in develop's `FOUNDER.md` (`PegSolitarie-dev`, lines 1–80; the entry is whole in that range).

  Pass.
- `Read` of the worktree's `ui/theme/Color.kt`: pass. The values:

  | Role | Light | Dark |
  |---|---|---|
  | `onSurfaceVariant` | `#635F69` | `#A79FB5` |
  | `outlineVariant` | `#DCD3EC` | `#3A3350` |
  | `background` | `#F4F1FA` | `#1B1626` |
  | `surfaceContainerHigh` | `#F1EAFB` | `#2E2740` |

- `Read` of the worktree's `ui/theme/BoardMarks.kt`, as found on disk: pass. One `val unearnedStar = scheme.onSurfaceVariant` serves both palettes. The standard palette's `earnedStarOutline` is `null` and its fill is the gold gradient, which matches CV-13b. I did not confirm the commit, because I have no Bash.
- **Contrast, worked by hand with §0's WCAG 2.x method:** pass.
  - **Method check.** The same arithmetic reproduces the spec's `outlineVariant` figures: 1.29 and 1.23 in light, 1.49 and 1.20 in dark.
  - **Relative luminance:** `#635F69` 0.1186, `#A79FB5` 0.3635, `#F4F1FA` 0.8905, `#F1EAFB` 0.8451, `#1B1626` 0.00947, `#2E2740` 0.0240.
  - **New ratios:** light 5.58 and 5.31; dark 6.95 and 5.59. Monochromacy is luminance only, so its ratio equals these.
- **Not computed:** the three Machado-2009 dichromat simulations, and the Results sky (its colours are not scheme roles, and I did not read them). §13.3 says so plainly. It relies on §3.5 row 11's floors, which cover the same colour on the same surfaces, and on CV-AC07's test.
- `./gradlew check`: not run. No code or resource changed, and this role has no Bash.
- `python3 …/ui-ux-pro-max/scripts/search.py`: not run (no Bash). I applied these by reading instead:
  - ui-ux-pro-max: Quick Reference §1 `color-contrast`, §6 `color-accessible-pairs`, and the "Icon Contrast" row (3:1 for larger glyphs);
  - android-accessibility §3: 3.0:1 for icons.
- Every edit returned success. Each `old_string` matched once. I did not re-read the file.

## Open issues / risks
1. **The worktree's `FOUNDER.md` has no Q-027 entry.** Develop's copy has it, and the developer's report noticed the same gap. The spec cites "FOUNDER.md on `develop`". The producer reconciles this at the next merge-in from develop.
2. **In light, the standard empty star now has more luminance contrast than the earned star.** The earned gold fill reads 1.09–2.22:1 (F-CV5, unchanged); the empty band reads 5.3–5.6:1. At normal vision the earned star still reads as the full one, by its filled area, its chroma and its glow. In greyscale it is the fainter shape, and the colour-blind palette is the path for that. §13.4 records this. QA's CV-AC11 off-palette baseline already captures it, and it is not a criterion. If the founder dislikes it on a device, the star half of option 3 is the follow-up after 3.0.0.
3. **The tie between the standard palette and row 11 rests on `BoardMarksTest`'s pin.** I cite that pin from the developer's report (RED-2, GREEN-2) and did not read the test. If the code-auditor wants the standard star's ratio proved by name, that is a test addition for the tech-lead to route. The spec does not require it.
4. **For the code-auditor's parallel re-review:** only one pass line moved. CV-AC05's exception now names the stars' colour as well as their form, which is how the developer's report already describes the off-palette comparison. Every other change is a pointer or the new §13.
5. **Staffing deviation (Q-019):** this ran on Opus. The mitigation is on the `Ran on:` line.

## Handoff
Next: producer —
1. Commit `docs/ux/colour-vision.md` and this report on `feature/colour-vision`, for example `docs(colour-vision): record the founder's Q-027 answer (CV-13b)`.
2. Tell the code-auditor that the spec now records Q-027: §13, CV-13b, and the new pointer in CV-AC05's cell.
3. Bring Q-027's `FOUNDER.md` entry onto the branch at the next merge-in from develop.

Next: qa-engineer — no change to the plan. CV-AC11's off-palette baseline will show the new grey on Results and on the catalogue's pips.
