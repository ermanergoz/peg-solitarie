# tech-lead report — sign-off of `feature/tech-design-r1-close` into `develop` (docs only)
Date: 2026-10-24
Branch: feature/tech-design-r1-close (committed by the producer). Reviewed head `d88f313` over merge-base `6aa7624`, which is `develop`'s head. The branch has four commits: `cf68c27`, `f4fe205`, `2d5289e` and `d88f313`. `git diff --name-only develop...HEAD` lists `docs/tech-design.md` and the two reports. My own edit to `docs/tech-design.md` (ruling 3 below, +2/−1) is uncommitted in `/Users/ermanergoz/Desktop/PegSolitarie-r1c`. I committed nothing and merged nothing, as dispatched.
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch. I also read the snapshot's `.claude/rules/lessons.md`. I invoked android-architecture and android-gradle-logic too, but the diff has no code and no build file, so neither affected a ruling.
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The dispatch reports that the Fable limit is reached, so this sign-off runs on Opus under Q-019 (`.planning/questions/FOUNDER.md`, answer 3: "Opus for every Fable-pinned role until 3.0.0 is published"). The author and the code-auditor also ran on Opus. My mitigation: I re-derived every fact behind a ruling from git and the tree, and took none of them from either report. The commands are under Checks.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/phase-14/tech-lead-tech-design-r1-close.md` (loops 0 and 1); `.planning/reports/phase-14/code-auditor-tech-design-r1-close.md` (loop 0 FAIL, loop 1 PASS); `docs/tech-design.md` §15, §16 #28–#31, §14.1 row RM and the record-file rule, §17's last eight rows, and the header's sync and "How to read it" paragraphs; Q-019. Not read: `.planning/log/`.
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Gate: PASS.** The code-auditor's loop 1 is PASS on `2d5289e`. It closes its loop-0 MAJOR (F-1) and its six LOW findings (F-2 to F-7). QA does not apply (docs only). Outside `docs/tech-design.md` and `.planning/`, the merged tree is byte-identical to `9f0e67a`, the tree audio's merge-in Check ran green on (1,565 / 0). `git merge-tree` against `develop` is clean.
- **Loop-0 judgement calls: both stand.** §15's "high off the book / low" stands, and I give "low" my explicit word below. The record-file rule stays in §14.1, with the §17 row recording what it supersedes.
- **Loop 1's notice decision stands.** R1's notice is in RM's scope because it calls the same function. It needs no extra red-first test, only the one QA step. One dependency is noted for the RM sign-off.
- **I-3 is confirmed, and L1-1 is fixed in the text.** The opening words of §16 #30's status cell now carry both endings. A §17 row records that the founder-run fallback was superseded.
- **Routing:** `docs/release-checklist.md` item 1.1 should name RM beside R1 and LN. That file is the release-manager's (I-2).

## The gate

| Evidence | Read | Result |
|---|---|---|
| `.planning/reports/phase-14/code-auditor-tech-design-r1-close.md` | In full, both loops | Loop 0 FAIL on `cf68c27`: F-1 MAJOR (RM's counts were measured on audio's pre-R1 head), F-2 to F-7 LOW, I-1 to I-7 INFO. Loop 1 PASS on `2d5289e`: F-1 to F-7 closed, I-1, I-3 and I-4 accepted, L1-1 LOW left for this sign-off, L1-I1 and L1-I2 INFO. `Ran on:`, the deviation, Checks and `Lessons applied:` are present in both sections |
| `.planning/reports/phase-14/tech-lead-tech-design-r1-close.md` | In full, both loops | I read the author's report for its edit list and for the decisions it flags for this sign-off. As dispatched, I did not re-judge the content on its merits |
| QA | — | Not applicable: docs only, as dispatched. No runnable build changes |
| Check | — | Not re-run (rules/studio.md: the sign-off re-runs no gate). `git diff --quiet 6aa7624 HEAD -- . ':!docs/tech-design.md' ':!.planning'` holds, and so does `git diff --quiet 9f0e67a 6aa7624 -- . ':!.planning'`. So the merged tree's Check inputs are the tree of `audio-review/android-game-dev-merge.md:3` (`13c04c6..9f0e67a`), whose `./scripts/check.sh` passed (`:27`: 1,565 tests, 0 failures/errors/skips; fuzz 10,000; kover 99.02 % at `:29`). Of the Check's inputs under `docs/`, only `docs/audio/CREDITS.md` is read by a test (`app/build.gradle.kts:464`), and this branch does not touch it |
| The merge | — | `git merge-tree --write-tree develop HEAD`: clean. The merge-base is `develop`'s head, so this is a fast-forward made `--no-ff` |
| Boundaries, dependency rules, tech design | — | No module, dependency, licence cell or code is touched. The design edits transcribe recorded rulings, each dated with its source, plus the tech-lead decisions ruled below |
| Table shape and numbering | — | 20 tables and 0 column mismatches on `develop`, on `d88f313` and with my edit. §16 runs 1…31, each number once |

## Rulings

### 1. The two loop-0 judgement calls

**(a) §15's R1 row, "high off the book / low": it stands. My word on "low":** "low" rates the impact of one occurrence. The frequency belongs to "high", and the cell already records that (QA's 6/6 on the GM6, 17/17 on the emulator). One Undecided hint today:
- makes no false claim: only a proof raises "No winning move" (GD-HT-1, §7.2);
- costs the player nothing and offers no ad: the Undecided branch returns before `pendingHint`, the funnel, the allowance and the gateway (§7.2 `:610`; GD-HT-5 extends ADS-06);
- changes no state, and leaves undo, restart and every on-line hint (book or recorded line) as they were.

What remains is an assist that helps less off the line. That does not touch the Core Value, which is the levels' solvability, not the hint's reach. The rate, the lever and the owner are R5's census, R2 and R2b (3.1, GD-HT-6), and the cell names them.

"Low" holds on two conditions:
- no text promises that a hint always finds a move (the release-manager's "After R1" column; checklist 2.5a);
- an Undecided answer never enters the ad funnel.

If either changes, the row re-rates to medium, the loop-0 rating's "player trust" impact. The cell does not state these conditions yet. My edit permission covers ruling 3 only, so the next tech-lead sync of §15 records them (Open issues).

**(b) The record-file rule in §14.1, plus a §17 row: it stands.** It follows the file's own convention. A decision lives in the section that governs its subject: §14.1's second-to-merge paragraph governs the merge-in. §17 lists what the decision supersedes, and its Where column points back. The §17 row (`:1837`) summarises the rule and points to §14.1; it does not hold a second copy that could drift. Its (a) to (c) summary matches the §14.1 text (`:1665-1674`), which matches R.2 verbatim (auditor, loop 0). R.4 item 9 ("in §14.1") and the dispatch ("a §17 row") are both met. The one rule that lives inside a §17 row is `d656263`'s merge-in rule for this file. That is the exception, and this branch does not repeat it.

### 2. The notice decision: it stands

R1's notice is in RM's scope because of how it is built. It needs no extra red-first test, only QA's added repro step. I verified this on the tree, not from the reports:
- `UndecidedNoticeSlot` (`HintUndecidedNotice.kt:155-188`) reads the gate once (`:170`), like every other caller.
- The gate chooses only the exit: `AnimatedVisibility` with a 160 ms `fadeOut`, or the plain branch.
- The display time is `delay(accessibilityManager.noticeDisplayMillis())` and does not depend on the gate.

So the notice's only gate-dependent behaviour is its exit. Criterion 23e pins that exit at both values: `theNoticeLeavesAfterItsDisplayTimeAndItsExit` (`GameScreenHintNoticeTest.kt:315`) and `withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame` (`:352`). RM's (a)/(b) live flips prove that the one function reads live. A caller that reads it in composition inherits that.

**One dependency, for the RM sign-off, not this branch:** the "no extra test" ruling assumes that 23e's off-value test still drives the gate after RM. That test sets the value through `Settings.Global` (`:354`). After RM, the gate reads the composition's `MotionDurationScale` instead. `GameScreenHintNoticeTest` is also the one suite of the seven on the `junit4.v2` rule (`:26`). Row RM already requires all seven files to "stay green unedited" and the developer to report the count. If this file goes red, or has to be edited, that is a stop-and-report under row RM, as it would be for the other six. The RM sign-off then rules how the off value is driven, and the notice gets the board's red-first live flip.

### 3. I-3 with L1-1: I-3 confirmed, L1-1 reworded

**I-3.** "The founder never tests by hand" is a standing rule (`audio-review/tech-lead-signoff.md:94`). It is also the qa-engineer's definition ("The founder never has to test by hand"). The hint sign-off's founder-run fallback (`hint-undecided/tech-lead-signoff.md:92`) contradicted both. The loop-1 reconciliation keeps the founder question for hardware set-up only, as Q-024 and Q-025 do, and that is the right reading.

**L1-1.** The cell said both "owed before the cut" and "may ship disclosed". The opening words now carry both endings: "**Open, owned. Owed before `release/3.0.0`; if no hardware set-up makes TalkBack drivable by the cut, disclosed instead as one line in the Q-PUBLISH-3.0.0 summary**". A dated note keeps the old opening, confirms the reconciliation, and adds that the disclosure is the last resort, never a waiver: QA's TalkBack pass on row TB still closes the item. The disclosure has precedent: audio's open halves, and §15's cold-start row "recorded as unverified rather than passed". The founder holds Gate 2, so a gap disclosed there is decided by the founder, not by the studio.

**A §17 row.** The fallback supersedes a recorded ruling (the hint sign-off §1.1). The file's header says a supersession "is listed in §17", and loop 1 had not listed it. That is the second line of my edit.

**My edit, hunk by hunk** (standing rule 1: this is the signer's own text, listed here for the producer to read before committing, as in the tech-design-sync sign-off):
- `docs/tech-design.md:1768` (§16 #30, status cell): the opening rewritten as quoted above, and one dated italic note appended after the loop-1 note.
- `docs/tech-design.md:1838` (§17, new last row): Superseded "§16 #30's fallback as `hint-undecided/tech-lead-signoff.md` §1.1 ruled it …"; By, hardware set-up only, else one disclosed Q-PUBLISH-3.0.0 line (tech-lead decision, r1-close loop 1, I-3; confirmed with L1-1's wording here); Where, "§16 #30; `phase-14/tech-lead-signoff-tech-design-r1-close.md`".

### 4. The merge: PASS

The code-auditor's PASS covers the branch's text. QA does not apply. The Check inputs are the last green tree's. The merge is clean. The decisions flagged for this sign-off are ruled above. Merge once the producer has committed my two-line hunk and this report.

## Process notes (for the producer; none blocks the merge)
- **P-1:** the code-auditor's loop 1 was appended to its loop-0 file. Rules/studio.md (Parallel work) instead renames the round's report to `<role>-loop<N>.md` before re-dispatching, so that the earlier round cannot satisfy the next round's wait. The record is still unambiguous: two complete sections, and the file's last line is `VERDICT: PASS`.
- **P-2:** the author's report shares its work commit twice (`cf68c27`, `2d5289e`). Rules/reporting.md asks for the report "in its own commit". This is the auditor's I-5, recurred.
- **P-3 (L1-I1):** row RM says "the producer has told the RM developer", and no file shows it. The producer should make sure the RM dispatch names row RM's counts, 14 calls in 9 files and seven test files, over the brief's 13/8 and six.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r1c/docs/tech-design.md`: §16 #30's status cell (`:1768`) and a new §17 row (`:1838`), +2/−1, uncommitted.
- `/Users/ermanergoz/Desktop/PegSolitarie-r1c/.planning/reports/phase-14/tech-lead-signoff-tech-design-r1-close.md`: this report, uncommitted.

## Checks
All run in `/Users/ermanergoz/Desktop/PegSolitarie-r1c`, read-only except for the two files above. No Gradle, no commit.
- `cat .claude/rules/VERSION` vs the plugin's `plugin.json`: 0.6.2 vs 1.0.0, so I read the plugin rule files — pass.
- `git rev-parse develop`, `git merge-base develop HEAD`: both `6aa7624` — pass. `git log develop..HEAD`: four commits, each with the one studio trailer — pass.
- `git diff --name-only develop...HEAD`: `docs/tech-design.md` and the two reports — pass.
- `git diff --quiet 6aa7624 HEAD -- . ':!docs/tech-design.md' ':!.planning'` and `git diff --quiet 9f0e67a 6aa7624 -- . ':!.planning'`: both equal. `sed -n '3p;27p;29p' .planning/reports/audio-review/android-game-dev-merge.md`: `13c04c6..9f0e67a`, check.sh pass, 1,565 / 0, kover 99.02 % — pass (the Check gate holds without a re-run).
- `git grep -nE 'docs/|\.planning' -- '*.gradle.kts' 'build-logic/**' 'scripts/check.sh' '.github/**'`: the only `docs/` file the Check reads is `docs/audio/CREDITS.md` (`app/build.gradle.kts:464`), and it is untouched — pass.
- `git merge-tree --write-tree develop HEAD`: clean — pass.
- `git grep -c 'rememberAnimationsEnabled()' 6aa7624 -- app/src/main`: 15 lines in 9 files, including the definition at `Clay.kt:209`, so 14 calls in 9 files. `git grep -l ANIMATOR_DURATION_SCALE 6aa7624 -- app/src/test | wc -l`: 7. Both match row RM — pass.
- `sed -n 155,190p HintUndecidedNotice.kt`, and a `grep` of `GameScreenHintNoticeTest.kt` for test names, the rule import and `ANIMATOR_DURATION_SCALE`: the gate chooses only the exit, the display time is a `delay`, the tests sit at `:315` and `:352`/`:354`, and the suite uses `junit4.v2` (`:26`) — pass (ruling 2).
- `grep -n 'never charged\|GD-HT-5' docs/tech-design.md` and the hint reports: Undecided returns before the funnel and the gateway — pass (ruling 1).
- `grep -n 'never tests by hand' audio-review/tech-lead-signoff.md` gives `:94`; `grep -n 'founder-run' hint-undecided/tech-lead-signoff.md` gives `:92` — pass (ruling 3's quotes).
- `python3 -I <scratchpad>/r1c-signoff/tables.py` on `develop`, `d88f313` and the edited file: 20 tables, 0 mismatches each — pass. `awk` over §16: 1…31, `uniq -d` empty — pass. `git diff --check`: clean. `git diff --stat`: `docs/tech-design.md` +2/−1 — pass.

## Open issues / risks
- **Q-019:** the author, the auditor and this sign-off all ran on Opus. The mitigation is the re-derivation above. No fable reader saw this text.
- **§15's "low" conditions** (ruling 1) are recorded only here until the next tech-lead sync of §15 writes them into the cell.
- **The RM dependency** (ruling 2): 23e's off-value test after RM's mechanism. It is guarded by row RM's "seven unedited, report the count", and the RM sign-off rules it.
- **I-2:** checklist item 1.1 does not name RM. Its rule ("every row … whose last cell says **Blocks the branch cut**") still covers RM.

## Handoff
Next: producer — read my hunk (ruling 3), then commit `docs/tech-design.md` and this report on `feature/tech-design-r1-close`, for example `docs(14): tech-lead sign-off of r1-close; §16 #30 wording`. Then `git merge --no-ff feature/tech-design-r1-close` into `develop`. Do it before checklist 1.1 reads §14.1, because row RM is a **Blocks the branch cut** row. Make sure the RM dispatch carries row RM's counts (P-3).
Next: release-manager (routed by the producer) — name RM in `docs/release-checklist.md` item 1.1 beside R1 and LN (code-auditor I-2).
Next: tech-lead, at the next §15 sync — write ruling 1's two conditions for "low" into the R1 row. At RM's sign-off, rule 23e's off-value test if it does not stay green unedited.

VERDICT: PASS
