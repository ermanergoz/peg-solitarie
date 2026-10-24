# ux-designer report — ux-spec-folds, check (review of a spec revision this agent did not write)
Date: 2026-10-21
Branch: feature/ux-spec-folds (committed by the producer). Reviewed: `docs/ux-spec.md` and `docs/ui/DESIGN-SYSTEM.md` as they stand at the worktree head `dc3f77e`, the revision under review being `c9228a9..dc3f77e -- docs/`.
Skills loaded: ergoz-soft:standards (role ux-designer; the product's snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; and the product's `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort). **Staffing deviation:** the ux-designer is pinned to fable. This check ran on Opus under the founder's Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), in `.planning/questions/FOUNDER.md`. The revision itself says it was written on Opus under Q-019, so independence rests on this being a fresh agent that did not read the author's report.
Fan-out: none. One read-only helper (Explore, haiku) ran `find`/`grep`/`ls` for facts I could not get with Read; it ran no git command and wrote nothing.
Loops: 0
Read in full: `.planning/reports/15-founder-feedback/tech-lead-signoff-close.md`, `tech-lead-signoff.md`, `tech-lead-fonts.md`, `qa-engineer-loop3.md`, `qa-engineer-15e.md`; `docs/ux-spec.md` (lines 1–4916) and `docs/ui/DESIGN-SYSTEM.md` on this branch. Not read: `.planning/reports/ux-spec-folds/ux-designer.md` (the author's report) and `.planning/log/`.
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist in the worktree)

## Summary
- **Folds a–e landed and are faithful.** Each fold's text matches the tech-lead's word for word, and the superseded wording is kept as the record (table below).
- **FAIL on two MAJOR findings.** (1) D-03n's one-fifth footer breaks criterion 15g's "no more than a third of the unused space" with both chips on the 6.7-inch phone, and §4.2.5 claims the opposite. 15g passed in loop 3, and it gets no pointer. (2) Criterion 26b requires TalkBack to stop on "the board" in gameplay and in the tutorial. On this branch the board carries no label, text, role or action, so it can't be a stop, and 26b fails by its own rule unless the board's semantics change, which it says this refresh does not do.
- The 15e O-1 ruling (D-05f, criterion 29) uses the tech-lead's mechanism. The F-1 test (15s) can be built against `HomeScreen(state: HomeUiState, …)` as Decision 3 describes. D-05e's spoken form and D-03o's 200 % pill rule are consistent and testable.
- No new id collides with an existing decision, section or criterion. Every number cited from QA's or the tech-lead's measurements matches its source; three MINOR and five INFO items follow.

## Fold check (requirement 1)
| Fold | Where it landed | Faithful to the tech-lead's text | Superseded wording kept |
|---|---|---|---|
| a. §6.5 precondition 1 (2026-10-18 amendment) | `docs/ux-spec.md:3452-3457` | Yes, verbatim (`tech-lead-signoff.md` "Text for other roles' files") | Yes: the 2026-09-29 sentence at `:3449-3450`, named superseded at `:3464-3469` |
| a. precondition 9 | `:3507-3509` | Yes, verbatim, with attribution added | n/a (new) |
| a. "Which builds" 2026-10-18 pointer | `:3433-3442` | Yes, verbatim. The start-path list added is Decision 1's (see F-3) and the sitting result matches `qa-engineer-15e.md`: delta 33.0 ms, floor 3.0 ms, arm A `c3d3437`, arm B `ee16a2a` | Yes: "Arm A = `develop`" named superseded |
| a. criterion 15e, two replacements | `:4281-4283` | Yes, both replacements exact | Yes, the old words are in the note at `:4312-4322` |
| b. precondition 1, D-1 route | `:3459-3462` | Yes, verbatim (the prefix "Amended" added) | n/a (append) |
| c. criterion 15k part 2 | `:4436-4449` | Yes, verbatim. "M-edge120 passes and M-edge121 is cut" matches the close report's row 7. `HomeLayoutTest` is `@GraphicsMode(NATIVE)` (`:107`), and `w360dp-h560dp` is its `OVERFLOWING_PHONE_QUALIFIER` (`:773`) | Yes: "≈72dp over" and "≈48dp over" kept |
| d. 15e O-1 | D-05f `:1145-1163`, §6.2 `:3210-3234`, criterion 29 `:4749-4757`, DESIGN-SYSTEM `:44-48` | Ruled wanted; the mechanism is the tech-lead's (requirement 2 below) | n/a |
| e. DESIGN-SYSTEM §Typography | `DESIGN-SYSTEM.md:58-74` | Yes. The first sentence and the tabular-figures clause match `tech-lead-fonts.md` verbatim | Yes: the old sentence is quoted at `:69-71`. It matches the pre-revision copy in the main checkout word for word |
| e. F-1, the timer's spoken form, the 200 % pill labels | D-03n/§4.2.5, D-05e/§4.3.2, D-03o/§4.2.6 | Ruled (checked below) | n/a |

Also folded, and not one of a–e: the precondition-5 note (`:3486-3489`) and the pointers on preconditions 2, "Where the numbers go" and protocol step 3. They are the tech-lead's own rulings from the close report's D-1 section and P-4, with attribution. That is acceptable.

## Requirements 2–4, in brief
- **2 (15e O-1).** Requirement met. §6.2 names the window theme, `android:windowLightNavigationBar` true/false, `values-v27/` + `values-night-v27/` each restating the whole style, and a `WindowThemeTest` case. Criterion 29(1) pins it with the test at API ≥ 27, and 29(2) covers three-button and gesture navigation in both themes on a device. The code supports it: `res/` holds only `values` and `values-night`; both themes parent `android:Theme.Material.Light.NoActionBar`; and `WindowThemeTest` runs at Robolectric SDK 34, so its status-bar cases will exercise the v27 styles and catch a style that forgets to restate `windowLightStatusBar`. One INFO (F-10).
- **3 (the F-1 ruling).** Buildable. `HomeScreen` is stateless over `HomeUiState` (`HomeScreen.kt:124-137`). `classicResumeLevelId`/`classicResumeBoard` and `challengeResumeLevel` drive the chips (`HomeUiState.kt:101-103`). 15s emits no chip → Classic → both → none and no chip → Challenge → both, which contains Decision 3's no → one → two, and it asserts pinned bounds: the hero, the tiles, a footer of ⅕ S₀, a stage of max(0, ⅘ S₀ − R) and the ribbon's rise. S₀ can be read in the test from the no-chip stage (`HOME_STAGE_TEST_TAG`, `HomeStage.kt:30`) plus the footer. The motion uses `ClayAnim.ENTRANCE_MS` (240) and `ClayAnim.ENTRANCE_EASING`, which exist (`ClayAnim.kt:29`, `:112`). The cause cited (`HomeViewModel.kt:209-238`) is exact. One MAJOR finding (F-1) concerns 15g, not the test's buildability.
- **4 (the timer, 26's order, the 200 % labels).** The timer's form is consistent and feasible. `GameStatusStrip` takes `elapsedSeconds: Int` (`:91`), which is 18f's name, and the description is set in the same composition as the digits (`:200-221`). `GAME_TIMER_PILL_TEST_TAG` exists (`:41`), the app has three existing `plurals` as the spec says, and the four test sites cited for retargeting (`GameFlowTest.kt:129`, `GameScreenChromeTest.kt:62`/`:75`, `GameStatusStripTest.kt:312-314`) all match `elapsed_time` exactly. 26b has the board problem (F-2). D-03o/15w is consistent and testable on the JVM in NATIVE metrics, with INFO F-8.

## Findings
**F-1 — MAJOR — D-03n breaks passed criterion 15g in the both-chips state, and §4.2.5 says it does not.** `docs/ux-spec.md:2451-2453`, `:4361-4365`, `:4111`, `:2359-2360`
- 15g (`:4361-4365`) requires, on a 6.7-inch phone "in **both** states — a fresh install with no Continue chip, and a player with both", that "the empty space left below the second mode tile is no more than a third of the screen's unused space". QA's loop-3 reading of 15g treats the unused space as the slack, stage slot plus footer (`qa-engineer-loop3.md`, 15g row: "the footer is ¼ of the slack"). Under the 3 : 1 split the footer was always 25 % of the current slack, so the clause held in every state.
- Under D-03n the footer is fixed at ⅕ S₀ while the slack shrinks by R. With both chips:
  - on QA's native window: 81.6 / (134.3 + 81.6) = **37.8 %**, using §4.2.5's own table (`:2431`);
  - on the 6.7-inch design baseline (843dp; S₀ 343–403, both-chips slack 95–203 from the D-03m table at `:2240-2244`): **40–72 %**.
- Only the no-chip state (20 %) and the one-chip state (26 %) stay under a third. §4.2.5 nevertheless states that the footer is "inside 15g's 'no more than a third of the unused space'" (`:2451-2453`), and its status line says 15g stands (`:2359-2360`).
- 15g gets no pointer. §13's retarget row (`:4111`) keeps "15g's midpoint" and says nothing about the third clause, so the developer who retargets `HomeLayoutTest`'s 15g case must either fail it or weaken it without a ruling.
- 15g passed on the device in loop 3, so this is a passed criterion changed without a record (requirement: "no contradicted earlier criterion left live").
- **Owed by the author (a UX decision, not mine to make here):** either rule how "the screen's unused space" is read. "The no-chip slack S₀, fixed for the window" gives 20 % in every state and matches D-03n's own logic. Add it as a dated pointer on 15g and correct `:2451-2453`, or change the ratio.

**F-2 — MAJOR — Criterion 26b requires a TalkBack stop on the board, which the board on this branch cannot provide.** `docs/ux-spec.md:4730-4733`, `:4741-4743`
- 26b's gameplay order is "… → the timer → **the board**, whose semantics this refresh does not change and this criterion does not judge → `undo_action` …". Its tutorial order is "… `onboarding_skip` → **the board** → the caption …". It then rules: "A missing stop … fails" (`:4743`).
- On this branch the board has no accessibility label, text, role or action, so TalkBack has nothing to land on:
  - `BoardCanvas.kt:196-212` is a `Canvas` with a `testTag` and a `pointerInput` tap detector only;
  - gameplay wraps it in a `Box` with a `testTag` and three custom semantics keys (`pegCountValue`, `gameStatusValue`, `legalTargetCountValue`; `GameScreen.kt:193-207`), which the platform does not expose as speakable content;
  - the tutorial wraps it in a non-clickable `Surface` (`TutorialScreen.kt:245-263`).
  - So a swipe from the timer should go straight to Undo, and from Skip straight to the caption: a "missing stop", by 26b's own rule.
- The criterion therefore fails on a stop it says it does not judge, or it leaves QA to rule. That makes it a contradictory pass/fail line, not a testable one. §11's traversal (`:3944-3945`) has carried the word "board" since the first edition, and 26b turns it into a pass/fail rule.
- **Owed by the author:** say what the board is in the order. Either "not a stop today; if it becomes one, it is visited here and is not judged", or rule that it should be a stop (a UX decision with its own criterion and a developer task). Correct §11's two lines to match.

**F-3 — MINOR — The folded start-path list omits the window-theme files under the other qualifiers, including the two D-05f adds.** `docs/ux-spec.md:3437-3439`, `:1162-1163`, `:3231-3233`
- The list ("`App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, `res/values/themes.xml` and `AndroidManifest.xml`") is Decision 1's, faithfully. Decision 1 predates H-2's `res/values-night/themes.xml`, and D-05f now adds `values-v27/` and `values-night-v27/` and calls the window theme a start-path file (`:1162`, `:3231`).
- The tech-lead's own close report checks `res/values-night/themes.xml` as a start-path file (Checks) and calls both v27 files start-path files (INFO row 9). QA's 15e report uses `res/values*/themes.xml`.
- As written, §6.5 lets a reader conclude that a merge-in touching only a `values-night*` or `values-v27` theme leaves a sitting standing. Add a dated note that the list reads every `res/values*/themes.xml` (citing the close report), or route it to the tech-lead to confirm.

**F-4 — MINOR — 15v puts a non-15e device check on the reserved bench.** `docs/ux-spec.md:4576-4582` against `:3453-3457`
- §6.5 precondition 1 says `Pixel_9a_bench` is "used for no other testing", and the close report's D-1 condition 3 says "the bench stays reserved for 15e sittings". 15v asks QA to seed one-save and two-save players there and screen-record cold starts, with no word on when.
- The tech-lead's H-3 did run device checks on the bench, but after the sitting and in the same session.
- State that 15v runs on the bench only after the branch's own 15e sitting, in the same session (the H-3 pattern), or name another instrument.

**F-5 — MINOR — Superseded text left live without the pointer house style requires** (§3, `docs/ux-spec.md:389-391`: "the decision it amends gains a one-line pointer to it").
- D-03k (`:833-883`) has no 2026-10-21 pointer to D-03o, which "Amends D-03k part 2's 'the label wraps'" (`:977-978`). §4.2.3 part 2 item 2 (`:1836-1838`, "if the pair does not fit the pill's width, the label wraps, the pill grows") still reads as the contract.
- §4.2.2 clause 2 (`:1614-1615`) still declares "slack distributed 3 : 1 when there is slack" to be "the contract". The D-03n pointer sits only in clause 3 (`:1635`).
- §4.2.4's Status (`:1947-1949`, "the 3 : 1 ratio … untouched") and "The stage" bullet (`:2015-2016`, "`Modifier.weight(3f)` through the renamed constant") have no pointer. A developer reading that bullet rebuilds today's `STAGE_WEIGHT = 3f` (`HomeScreen.kt:729`).
- §4.2.3 (`:1821-1823`, "the 3 : 1 slack … as D-03i left them") has no pointer either.
- No criterion is contradicted by these, but each one specifies the old mechanism in the place a developer will read.

**F-6 — INFO — "R-3" collides with §7's R-3.** `docs/ux-spec.md:988`, `:2535`, `:2536` against `:3622`. The first mentions are qualified with `ux-designer-15p.md` (`:977`, `:2527`). The later bare "R-3" reads as §7's data requirement R-3 (`HomeScreen` loses `onShowHowToPlay`). Qualify each one.

**F-7 — INFO — One sentence about the table is false for half of it.** `docs/ux-spec.md:2436`
- "Every tile top in the table is below QA's 15g midpoint of the native window (476.8dp)": the three 360×640 rows (343.0–387.0dp) are above 476.8dp.
- Those rows belong to a window whose own content midpoint is 334dp (52 + 564 ÷ 2), and they are below that. Restrict the sentence to the native rows, or give both midpoints.

**F-8 — INFO — §4.2.6 rule 3 and rule 4 disagree in one named edge.** `docs/ux-spec.md:2551-2558`, `:4593-4594`
- Rule 3 says a value's arrival "can change no pill's shape or height". Rule 4 lets a too-wide value move under its label "in that pill alone", which is a value deciding one pill's shape. 15w part 4 ("the three pills share one shape") would not hold in that case.
- On the spec's own estimates ("Boards" ≈42 of ≈46.7dp), the ribbon stacks at about 1.12×. The six-digit case can therefore arise only between about 1.07× and 1.12× (for example a 1.1× OEM step), not "at about 1.15×".
- Add "except rule 4's case" to rule 3.

**F-9 — INFO — After fold c, two headline sentences describe LEGACY metrics only.** `docs/ux-spec.md:835-836` (D-03k: "a 360×640dp phone with both Continue chips scrolls"), `:1889` (§4.2.3: "So: a 360×640dp phone with both Continue chips scrolls"). The fold's text did not ask for pointers here. One to 15k's 2026-10-21 note would stop a reader taking them as device behaviour.

**F-10 — INFO — Criterion 29 leaves the qualifier choice to §6.2.** `docs/ux-spec.md:4749-4752`, `:3226-3229`. The criterion does not name `values-v27/`/`values-night-v27/`. At SDK 34 (`WindowThemeTest.kt:64`), part (1) cannot tell a v27-qualified style from the attribute placed in plain `values/`; lint's NewApi check would flag the latter. Requirement 2 is met through §6.2.

## Numbers checked against their sources (all match)
- §4.2.5:
  - QA frames 2.528–2.744 / 2.791 / 2.894 / 3.140 s; stage ≈306 → ≈163dp; first paint to chips ≈0.6 s; art ≈0.26 s late (`qa-engineer-loop3.md` F-1).
  - Native window 411×923dp, 845dp usable; stage slot 305.9 and footer ≈102; tile 1 at 557.3 / 581.3 / 605.3 (loop 3, 15g). 360×640: 564 usable, 80dp chips, 8dp gaps (loop 3, 15k band edges).
  - Every cell of the three-column table recomputes, as do 6.4 and 9.6dp to spare, S₀ ≈283 at 720dp, 130.4 and 116.25dp, +20.4dp, −27.6dp, and 3.6dp.
- D-05f: `#cdccd0`, buttons 3.59:1 (`qa-engineer-15e.md`, O-1 and the navigation-bar table).
- §6.5: the sitting's delta, floor and arms (`qa-engineer-15e.md`). D-05e: "On, Show possible moves, Switch" (`qa-engineer.md`, 21d).
- §4.2.6: pill 109.3 / content 93.3dp; the API 34 scale-2.0 mappings 12 → 24 and 16 → 28dp; "Sta/rs", "Bo/ard/s", "Leve/l" (loop 3, O-2).
- 18f: the nine `elapsedSeconds` values map exactly to the nine table rows.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-ux/.planning/reports/ux-spec-folds/ux-designer-check.md`: this report, with VERDICT FAIL. Not committed: the producer commits it. I edited no other file.

## Checks
- `Read .git/worktrees/PegSolitarie-ux/HEAD` and `.git/refs/heads/feature/ux-spec-folds`: pass. The worktree is on `feature/ux-spec-folds` at `dc3f77ef85e1…`.
- `git diff c9228a9..dc3f77e -- docs/`: **not run.** I have no Bash, and my role may not have a helper run git. I identified the revision's text by its 2026-10-21 dates and the tenth-revision index (`docs/ux-spec.md:291-326`), and read both documents in full. The DESIGN-SYSTEM record was compared with the pre-revision copy in the main checkout.
- `Read docs/ux-spec.md` (1–4916, in consecutive pages) and `docs/ui/DESIGN-SYSTEM.md` (full): pass.
- `Read` on the code: `HomeViewModel.kt:180-269`, `HomeUiState.kt`, `HomeScreen.kt:1-220`, `GameStatusStrip.kt`, `GameScreen.kt:150-349`, `TutorialScreen.kt:80-279`, `BoardCanvas.kt:180-219`, `Clay.kt`, `res/values/themes.xml`, `res/values-night/themes.xml`, `WindowThemeTest.kt`, `GameStatusStripTest.kt:300-324`, `GameScreenChromeTest.kt:55-79`, `GameFlowTest.kt:120-134`: pass. The facts are cited in the findings.
- Helper (haiku, read-only), `ls app/src/main/res | grep '^values'`: `values`, `values-night`.
- Helper, `grep -n -E 'semantics|contentDescription|testTag|clearAndSetSemantics' …/ui/components/BoardCanvas.kt`: only `testTag` (`:200`). This is F-2's evidence.
- Helper, `grep -c '<plurals' res/values/strings.xml`: 3.
- Helper, `grep -n -E 'GraphicsMode|w360dp-h560dp|h560' …/HomeLayoutTest.kt`: `@GraphicsMode(NATIVE)` at `:107`, `w360dp-h560dp` at `:773`.
- Helper, `grep -rn -E 'HOME_STAGE_TEST_TAG *=|STAGE_WEIGHT|…|weight\(' …/ui/home/`: `STAGE_WEIGHT = 3f` (`HomeScreen.kt:729`), `SLACK_BELOW_TILES_WEIGHT` (`:447`), `HOME_STAGE_TEST_TAG` (`HomeStage.kt:30`).
- Helper, `grep -rn -E 'ENTRANCE_MS|ENTRANCE_EASING' …/ui/theme/`: `ENTRANCE_MS = 240` and `ENTRANCE_EASING` exist (`ClayAnim.kt:29`, `:112`).
- Helper, id-collision greps over `docs/ux-spec.md` for `15s|15t|15u|15v|15w`, `D-03n|D-03o|D-05e|D-05f`, `4.2.5|4.2.6|4.3.2`, `R-3`, `3 : 1|weight(3f)|¾|three quarters`, `a third|unused space`: pass. Each new id occurs only in the tenth revision's own text; `R-3` gives F-6; the `3 : 1` hits give F-5. The helper's `\b`-anchored grep for `18f|18g|26b` and for `29` printed nothing, which is wrong, since `18f.` is at `:4638`. For those four ids the collision check rests on my full read: criteria run 18, 18a–18e, 18f, 18g, 19…; 26, 26a, 26b, 27, 28, 29, and none existed before.
- Helper, `grep -rn 'Pixel_9a_bench' docs`: eight hits, all in `docs/ux-spec.md`. Fold a has landed; the closing report's "no `Pixel_9a_bench` under `docs/`" is now resolved.
- The Check command was not run: this is a documents-only review, and the role has no Bash.
- I did not read `.planning/log/` or the author's report.

## Open issues / risks
- The diff itself was not seen. If the worktree holds uncommitted edits to `docs/` beyond `dc3f77e`, this check read them as part of the revision. The producer can confirm with `git -C PegSolitarie-ux status --short docs/`.
- Staffing deviation: author and checker both ran on Opus under Q-019, which leaves a fresh agent with no access to the author's reasoning as the only independence.
- F-2's device behaviour is inferred from the code: the board has no label, text, role or action. The TalkBack pass on a device will confirm it, but the criterion should not depend on that pass to say what it means.

## Handoff
Next: producer —
- Return F-1 and F-2 (MAJOR) and F-3 to F-5 (MINOR) to the revision's author, the ux-designer, on `feature/ux-spec-folds`. F-1 needs a UX ruling on how 15g's "unused space" is read, or a different ratio. F-2 needs a ruling on whether the board is a TalkBack stop. F-6 to F-10 are INFO, for the same pass at the author's discretion.
- Route F-3's question to the tech-lead if the author wants it confirmed: do all `res/values*/themes.xml` files count as start-path files?
- Then dispatch a fresh check of the amended revision. The folds a–e in this revision need no rework.

VERDICT: FAIL
