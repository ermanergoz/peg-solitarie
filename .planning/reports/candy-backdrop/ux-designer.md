# ux-designer report — candy-backdrop
Date: 2026-10-17
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; plugin rules v1.0.0 read in full because the product snapshot is older: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, dispatch; product `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: phase-15 qa-engineer loop 3
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the ux-designer is pinned to fable; this ran on Opus under the founder's Q-019 answer (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), recorded in `PegSolitarie-p15/.planning/questions/FOUNDER.md`. Mitigation: the specs go to fresh reviewers who run their own checks.
Fan-out: none. Two haiku Explore helpers did read-only lookups this role's tools cannot do (a heading index of the 326 KB phase-15 spec, a file listing, greps); no studio role was dispatched.
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
Ran on Opus, not the pinned Fable, by Q-019. Finished and corrected the cut-off draft of `docs/ux/candy-backdrop.md` (§0–§14), and wrote `docs/ux/licences-screen.md`.
The backdrop has three densities: Landscape (Home), Quiet (gameplay) and Sparse (every other screen, the tutorial included). It is drawn in Compose Canvas from new `Backdrop*` tokens, under the verdict's two-question test and do/don't list.
Every piece of text sits only on text-safe colours. In dark mode every scene colour is darker than the background or brighter than every container. On Home the sweets stand in the stage's wings.
The scene is kept off Home's first frame, motion is limited to today's two orbs, and 26 criteria (BD-01–BD-26) are written for the auditor and QA. The licences screen is an About › Licenses row in Settings that lists THIRD_PARTY.md's shipped rows and never names King or "candy".
One founder question is drafted below (non-blocking; the default is specified in full).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md`: the backdrop spec. It covers decisions B-01–B-16, the screens and their densities, layers, motifs, compositions per density and window, colour, dimension and metrics tokens (light and dark), legibility (text zones, hero edges, colour vision, system bars, screen readers), motion, the cold-start budget and degradation path, fit with the phase-15 layouts, assets, the verdict checklist, acceptance criteria BD-01–BD-26, tests owed, rejected alternatives and open items.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/licences-screen.md`: the licences surface. It covers decisions LS-01–LS-07, the Settings entry point, the Licenses and License text screens, what is listed and never listed, accessibility, copy, assets, criteria LS-AC01–LS-AC11 and open items.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer.md`: this report.

## Checks
- `./gradlew check`: not run. No code or resource changed, and this role has no Bash.
- `python3 .../ui-ux-pro-max/scripts/search.py ... --design-system`: not run (no Bash). I applied the skill's Quick Reference sections 1–7 and its light/dark and pre-delivery checklists by reading them instead.
- WCAG 2.x relative luminance and contrast, computed by hand from every `Color.kt` hex used: pass.
  - Zone-T text pairs: light worst 4.6:1 (`primary`) and 5.0:1 (`onSurfaceVariant`); dark worst 6.5:1 and 6.9:1.
  - The Home title's dark stop is ≥ 3:1 everywhere it can reach. Hero edges at rest are ≥ today's ratios in both themes (§5.2 table).
  - One pre-existing fail is recorded, not caused by this work: the Home title gradient's light stop is about 2.4:1 against the light background.
- Hue of every new token: pass (252°–331°, inside the specified 250°–335° violet–pink arc). There is no gold, blue, red, green or brown.
- Geometry arithmetic behind the §3.3 tables: pass.
  - Gameplay slab and dock positions match `docs/ux-spec.md` §4.3.1's 360×640 budget and the Pixel_9's bars (qa-engineer-loop2).
  - Stage-wing sizes use §4.2.4's stage heights as corrected by D-03m. The lollipop fits in every one-chip stage, and the both-chips stage is under 160dp, so no wings are drawn there.
  - Hill sprinkles stay 38–84dp below the slab's foot.
- Colour-blind palette option: none found. The helper's grep over app code ran on the develop worktree. I then confirmed directly on phase-15 that `SettingsScreen.kt` has only General, Play Games and Privacy, and that §11 of the phase-15 spec names no such option. Recorded as a pre-existing gap.
- The research-analyst's grep (its report's Checks) found no user-facing string containing any banned word. The backdrop adds no string. The licences strings are listed in `licences-screen.md` §6 and contain none.

## Open issues / risks
1. **Device readings govern.** Every ratio here is computed from hexes. BD-12, BD-13 and BD-24 are the device checks. In dark mode the clouds are deliberately faint wisps (hue only), so that card edges stay at least as separated as they are today.
2. **The stage wings change `HomeStage.kt`**, a phase-15 file. The sweets are drawn behind the art inside the stage slot (B-12), under D-03l clause 4's own terms. The tech-lead should confirm the fit, and this branch is built after `feature/phase-15-ux-refresh` merges (backdrop §14.6).
3. **Sky continuity across navigation (B-13, BD-21)** probably means hosting one backdrop behind the `NavHost`, an architecture call for the tech-lead.
4. **Cold start:** the scene is off the first frame by design (B-11). 15e must still PASS on this branch under §6.5's protocol, so the producer has to schedule a quiet-host Pixel_9a sitting.
5. **Pre-existing findings routed, not fixed here:**
   - the missing colour-blind palette option that rules/compose.md requires of games;
   - the Home title gradient's light stop at about 2.4:1.
6. **Release-manager items:**
   - THIRD_PARTY.md's §1 backdrop row travels with this branch;
   - a new row for the bundled Apache-2.0 text, with a one-line confirmation of its redistribution terms from the research-analyst;
   - U-1 (the UMP licence) decides whether the licences screen names the User Messaging Platform;
   - Apache NOTICE files to check in the release listing;
   - Play Console achievement and leaderboard names checked against the verdict's §3a word list;
   - store screenshots recaptured as plain captures after this lands (Q-016).
7. **Inputs read off-branch:** the second lookup helper ran on the develop worktree for most of its greps.
   - Every decision here rests on files I read directly from `PegSolitarie-p15`.
   - One exception: the Results and catalogue text roles came from develop. Phase 15 did not touch those screens (its spec §11), so the risk is low, but the developer confirms them.
8. The cut-off first draft is superseded wherever it differs (backdrop §0 lists what changed). Its tutorial-density contradiction is resolved as Sparse, and its title-wraps flag is retired.

## Handoff
Next: producer —
1. Commit the three files on `feature/candy-backdrop`.
2. Queue the founder question below as the next free `Q-<NNN>`, non-blocking. The build proceeds on option 1.
3. Dispatch the tech-lead on B-13's hosting and on the stage wings' fit with D-03l and `HomeStage`.
4. Once phase 15 has merged, dispatch the android-game-dev to build both specs.
5. Give the code-auditor and the qa-engineer BD-01–BD-26 and LS-AC01–LS-AC11. BD-02 (the two-question test) and BD-01 (the words grep) are the verdict's own acceptance items.
6. Schedule the 15e sitting on this branch.
7. Route backdrop §14 items 2–3 (the colour-blind option, the title light stop) as their own ux-designer tasks, and item 7 to the release-manager.

### Drafted founder question (FOUNDER.md format; id left for the producer)

    ## Q-NNN — How much of the candy scene goes on the game screen?   (open)
    Date: 2026-10-17    Asked by: ux-designer
    Context: The menu gets the full sweets landscape you asked for: frosted hills with drips and sprinkles, clouds, and a lollipop, a cane, a gumdrop and a cupcake beside the board picture. The game screen gets a calmer version so your eye stays on the pegs. All three options are readable and IP-safe; this is taste.
    Options: 1) Calm game screen (default, being built): cotton-candy sky, clouds behind the title, frosted hills with sprinkles under and behind the board; the standing sweets stay on the menu  2) Full scene on the game screen too: the lollipop, cane, gumdrop and cupcake also stand on the hills around the board (on phones they mostly peek out from below it; fully on tablets)  3) Plain game screen: sky and clouds only, no hills
    Recommendation: ux-designer picks 1 — the game screen joins the same candy world while nothing stands next to the board you are solving; 2 is the busiest candy-game frame, but on phones the sweets would mostly show as pieces peeking out from behind the board and the buttons; 3 is calmest but loses the candy feel you asked for.
    Blocks: nothing (option 1 is specified and buildable; 2 and 3 are constant changes, docs/ux/candy-backdrop.md §3.3)
    Answer:
