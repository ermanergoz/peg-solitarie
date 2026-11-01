# tech-lead report — tech-design-rm-close (sign-off before the merge to develop)
Date: 2026-10-24
Branch: feature/tech-design-rm-close (report only; read at `f9f3cc7`. The text under review is `300198e` (loop 0) and `bd3218c` (loop 1). The branch was cut from `develop` at `053e9cc`, which is still `develop` and the merge base)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, dispatch, stacks, testing, kotlin-android, compose, puzzle-design and play-store; also the snapshot's lessons.md. Plus android-architecture and android-gradle-logic
Ran in parallel with: none named in the dispatch (open worktrees: home-chip-arrival, colour-vision, licences-screen, talkback-board)
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The author, the code-auditor and this sign-off all ran on Opus, so this sign-off shares the author's model. I am a fresh tech-lead and wrote none of this branch. Mitigation: I re-derived the facts below from git and the sources with methods of my own: a static call count to set against the compiler counts, and my own table, numbering, sha, path and source checks. None of them is taken from a report alone (Checks)
Fan-out: none
Loops: 0
Read in full: `.planning/reports/reduced-motion-live-gate/tech-lead-signoff.md`; `.planning/reports/tech-design-rm-close/tech-lead-loop0.md`, `tech-lead.md`, `code-auditor-loop0.md` and `code-auditor.md`; `git diff develop...HEAD -- docs/tech-design.md`, all 147 lines. Read in part: `reduced-motion-live-gate/tech-lead-ruling.md` §9, F1.4 and FA.4 (the owed text, against which the transcriptions were checked); in `docs/tech-design.md`, §14.1's reading note, §17's preamble and its merge-in rule row; `STATUS.md:13` on `develop`
Lessons applied: none (the snapshot has 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- **SIGN-OFF: PASS.** The branch carries all nine items of the RM sign-off's §4(c), with §4(b)'s row as #32, each placed where §4(c) names it. The ruling's owed sentences (§9 item 2; F1.4 with FA.4's clause) are verbatim. The code-auditor's loop 1 is PASS on `bd3218c` (`code-auditor.md`), and it closes loop 0's text-only FAIL (`code-auditor-loop0.md`, F1–F4). QA does not apply, because the branch is docs only.
- **The figures hold by two methods.** The auditor's compiles print 39 deprecation lines on `053e9cc` and 34 on `6aa7624`. My static count of the non-comment v1 `createComposeRule(` calls gives the same 39 and 34.
- **I keep the four flagged additions and the header's loop record** (§2).
- **`develop` has not moved.** It is still `053e9cc`, the merge base. The trial merge is clean, and no unmerged branch edits `docs/tech-design.md`. The merge needs no Check re-run, because the merged tree's code is `develop`'s own (§3). §3 also says what to do if a parallel branch numbers a §16 row first.

## 1. The sign-off: PASS
Line numbers below are `docs/tech-design.md` on `f9f3cc7`.

| §4 item | Where | Verdict |
|---|---|---|
| (c)1 §14.1 row RM: Done | `:1597` | **Present and accurate.** "**Done.**" opens the last cell, and the old cell is kept after "*The record:*", as in R1's row. The cell names the merge `053e9cc`, the code range `7382351..45f84ec`, and the no-re-run ruling with its command, which exits 0 again today. It cites the reviews by path, the probe count and the mutants, Finding 1's rider, the riders by commit, F1's acceptance under ux §10's 15 s line, and QA's §8 PASS. Ruling §9 item 1's three sentences are in the Item cell, and the superseded sentence stays there, dated |
| (c)2 §16 #28: closed | `:1806` | **Present and accurate.** F1.4's sentence with FA.4's clause is verbatim (checked against `tech-lead-ruling.md:245`, `:316`). The close follows #21's pattern. The as-built facts hold on `053e9cc`: `Clay.kt:219-222` (the cell cites `:219-220`, the signature and the scale read), 15 gate lines in 9 files (14 calls plus the definition), and one `Settings.Global` line, a KDoc line (`Clay.kt:212`) |
| (c)3 §8.1 | `:834-852` | **Present and accurate.** Ruling §9 item 2's sentence is verbatim, as its own paragraph. The v2 KDoc is marked done (`2b98c5c`). The four classes and the star suite are on v1, and `2b98c5c` touches exactly those 11 files. The count is 38 files: 37 suites and the helper |
| (c)4 §9.8 figure | `:1140` | **39, and it holds** (Checks: the static count matches the auditor's compiler count on both trees). The caption at `:1132` sums to 249 (6 + 4 + 0 + 198 + 39 + 1 + 1, re-added). The replaced figure stays as a dated "until" note |
| (c)5 §15 RM row: closed | `:1766` | **Present.** It follows R-2's closed-row pattern and carries both residuals. The known-issue line matches `STATUS.md:13` on `develop` |
| (c)6 §14.2, two lines | `:1733-1745` | **Present.** The aim, its route and its cost are the sign-off's §2. O-5 is quoted from `15-founder-feedback/tech-lead-signoff.md:280-283`, which I re-read |
| (c)7 = (b) §16 #32 | `:1810` | **Present and accurate.** The title, status, severity, version and chain are §4(b)'s. The facts hold on `053e9cc`: the slot at `HintUndecidedNotice.kt:155-189`, the timer at `:163-168`, outside the gate's branch, `key(raiseKey)` at `:171`, and `clayEntrance(0)` at `:182`. `feature/talkback-board` is still `b8aa8c4`, and it does not touch that file |
| (c)8 §17, ruling §9 item 3 | `:1880` | **Present and accurate after loop 1** (F1). Its account of 7 tests in 5 files matches ruling §1 |
| (c)9 header sync sentence | `:46-56` | **Present.** It follows r1-close's sentence, as hunk (1) of the merge-in rule orders, and loop 1 completed it (F3) |

**Info (no change asked; none gates the merge):**
- **I-1.** `:1140`'s dated note reads "the RM code-auditor read 35 → 39". That is the auditor's first range: the four moved classes, before `45f84ec` (`reduced-motion-live-gate/code-auditor.md:29`). The RM sign-off's "35 → 40" is the full range, star suite included. The clause is cited only to show 34 + 1 at the base, and it shows that. It is accurate.
- **I-2.** r1-close's §17 row (`:1877`) still says audio's merge-in Check "printed 33 such `w:` lines"; the true count was 34. §17 is append-only, and none of its rows carries a forward pointer. The new row (`:1881`) and §9.8's dated note (`:1140`) carry the correction, so nothing is owed.
- **I-3.** The kotlin-reflect row's `:300` stays. The caption makes that row `e1875cb`'s, as both reviewers found.
- **I-4.** Row RM keeps **Blocks the branch cut** after **Done.**, as R1's row does. §14.1's note (`:1582-1583`) says a Done row blocks nothing, and checklist 1.1 passes a merged row either way.

**Why PASS.** Every owed item is in and accurate. No figure, sha or path is wrong: 16 shas and every cited path resolve, and the Compose and platform source lines match. The tables keep their shape. §16 runs 1–32, each number once. Nothing outside `docs/tech-design.md` and the step's own reports changes.

## 2. The additions beyond the nine items, and the header's loop record (tech-lead decisions)
1. **§17's figure row (`:1881`), 33 → 39: keep.** The file's rule lists every supersession in §17, and r1-close gave the 29 → 33 correction a row of its own. The row's claims hold by both methods: 33 importing files on `6aa7624` with 34 calls, and 38 files with 39 calls on `053e9cc`.
2. **The figure's other mentions (§8.1 `:840`; §9.8's caption `:1132`, its Fix column `:1140` and rule 2 `:1174-1175`): keep.** One thing gets one figure across the file. Leaving the repeats at 33 would give the file two counts for one fact. Each replaced count stays as a dated note.
3. **Merge-order step 5's note (`:1650-1651`): keep.** It is true: RM merged as `053e9cc`, and `feature/talkback-board` is not an ancestor of `develop`. It sits where a reader of the order looks, and it answers step 5's own "its slot against 5a is set in its row".
4. **#28's enforcement sentence (`:1806`): keep, as loop 1 worded it.** It says how the rule is held, by review, and it names the grep's limit. My own key grep over `app/src/main` finds no `LaunchedEffect` or `DisposableEffect` keyed on the gate, or on `GameActionDock`'s gate-derived `animate`.
5. **The header's loop record (`:52-56`): keep.** It follows r1-close's own "Its review loop 1 (…) corrected …" sentence. It cites the report whose findings drove the loop, and it names F1–F4 by what each corrected. "Both figures stand by the compiler on both trees" is what the auditor's two compiles show.

None of the five makes a new decision. Each restates a recorded fact or ruling, so none needs a §17 row beyond the two already added.

## 3. `develop`, the merge, and the §16 number
**`develop` has not moved.** `develop` is `053e9cc`, the merge base. The branch adds `docs/tech-design.md` and four reports, and nothing else. `git merge-tree --write-tree develop HEAD` is clean (tree `1804d8e`), so the merge is a fast-forward made `--no-ff`. None of the unmerged branches edits `docs/tech-design.md`: talkback-board `b8aa8c4`, home-chip-arrival `925f36f`, licences-screen `1034e87`, colour-vision `0f1f58f` and candy-backdrop `fd56cce`. No `release/*` branch exists, so merging now meets the deadline: row RM must say Done before checklist 1.1 runs.

**The Check: no re-run (tech-lead decision, as at r1-close).** Outside `.planning/` and `docs/tech-design.md`, HEAD equals `develop`. No build or test input reads `docs/tech-design.md`: the only `docs/` file the build reads is `docs/audio/CREDITS.md`, which is untouched. So the merged tree's code is `053e9cc`'s, which is `45f84ec`'s, where the last green Checks ran.

**The merge-time condition.** The producer runs this immediately before `git merge --no-ff feature/tech-design-rm-close`. It must exit 0, and the `awk` must print nothing:

    T=$(git merge-tree --write-tree develop feature/tech-design-rm-close) &&
    git diff --quiet develop "$T" -- . ':(exclude).planning' ':(exclude)docs/tech-design.md' &&
    git show "$T:docs/tech-design.md" | awk '/^## 16\./{f=1;next} /^## 17\./{f=0} f && /^\| *[0-9]+ \|/{split($0,a,"|"); gsub(/ /,"",a[2]); print a[2]}' | sort -n | uniq -d

The first command fails on a conflict. The second fails if the merge would bring code into `develop`. The `awk` prints any §16 number that appears twice.

**If `develop` moves first:**
- **(a) Commits that leave `docs/tech-design.md` alone** (the four code branches as they stand, records, STATUS.md): no merge-in. The condition still holds, and the merge goes ahead with no Check re-run, because the merged tree's code is `develop`'s own, checked at that branch's merge. The text stays true. §9.8's 39 names its tree (`053e9cc`), step 5's note holds, and #32's talkback-board clause is dated to `b8aa8c4`. A later tree's figure is the next sync's to measure.
- **(b) A branch that numbers a §16 row first,** so that #32 is taken on `develop`. Then this branch merges second, under §17's merge-in rule (`:1872`):
  1. It takes `develop` in on its own branch, never by a rebase. The rule names the producer for a docs-only branch with no developer.
  2. **Hunk (3):** its row #32 is renumbered to continue after `develop`'s highest (#33 if that is #32). Its four cross-references in this file follow: the header's sync sentence (`:51`), row RM's Done cell (`:1597`), §15's RM row (`:1766`) and #28's last sentence (`:1806`). A §17 row names the old number.
  3. **Hunks (1) and (5):** both header sync sentences stay, and so do both sides' §17 rows, `develop`'s first in each case. **Hunk (2):** amendments to other rows go in document order. **Hunk (4):** if the other branch also writes §9.8's `createComposeRule` cell, the cell keeps one figure: the merged tree's when a Check measures it, else the later tree's. The replaced figure stays as a dated note.
  4. Records that cite #32 stay as written: the RM sign-off, the RM-close reports and this one. The producer names the new number in its next STATUS.md line and in the 3.0.1 brief's dispatch.
  5. A fresh tech-lead re-reads the merge commit's conflict hunks (`git show --cc`) before the `--no-ff` merge. It re-runs the table, §16-numbering and cross-reference checks. Docs hunks need no code-auditor. A hunk that none of (1)–(5) decides stops and comes to the tech-lead before the merge-in is committed.
  6. The rule's Check re-run is met by the condition's second line, as long as the merge-in brings no code (same decision as above). If it brings code, the Check runs on the merged tree.
- **(c) This branch merges first,** which is the case today. Any later branch that adds §16 rows is then the second to merge, and it renumbers its own rows after #32 under the same hunk (3). Nothing more is owed here.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rmc/.planning/reports/tech-design-rm-close/tech-lead-signoff.md`: this sign-off. It is the only file written, and it is committed alone. Scratch files (the table script, `develop`'s copy of the file, the extracted sources) are in this session's scratchpad.

## Checks
All read-only, in `/Users/ermanergoz/Desktop/PegSolitarie-rmc`. No Gradle was run, because a sign-off re-runs no gate.
- `cat .claude/rules/VERSION` (0.6.2) against `plugin.json` (1.0.0): older, so the plugin rule files were read — pass.
- `git rev-parse HEAD develop`: `f9f3cc7`, `053e9cc`. `git merge-base develop HEAD`: `053e9cc`. `git diff --name-status develop...HEAD`: `docs/tech-design.md` M, and four reports A — pass.
- `git merge-tree --write-tree develop HEAD`: `1804d8e`, exit 0. `git diff --quiet develop HEAD -- . ':(exclude).planning' ':(exclude)docs/tech-design.md'`: exit 0. `git diff --quiet 45f84ec 053e9cc -- . ':(exclude).planning' ':(exclude)docs/tech-design.md' ':(exclude)docs/ux-spec.md'`: exit 0 — pass.
- `git grep -n -E 'tech-design|docs/'` over the build scripts, `build-logic`, `scripts/check.sh`, `.github` and the tests: the only `docs/` file read is `docs/audio/CREDITS.md` (`app/build.gradle.kts:466`, `AudioRepoFiles.kt:19`), and `ci.yml:75` is a comment — pass.
- For every local branch not merged into `develop`: `git diff --name-only develop...<b> -- docs/tech-design.md`. Only this branch edits the file. `git merge-base --is-ancestor` exits 1 for talkback-board, home-chip-arrival, licences-screen and colour-vision. `git branch --list 'release/*'` is empty — pass.
- Static count over `app/src/test*`, excluding the one `junit4.v2` importer (`GameScreenHintNoticeTest.kt`): `git grep -o 'createComposeRule('` gives 35 on `6aa7624` and 40 on `053e9cc`. Each includes one comment hit, `SystemBarInsets.kt:73`, so the v1 calls are **34** and **39**. The importing files number 33 and 38. `WrongTapBoards.kt` holds two calls, at `:495` on `6aa7624` and at `:496` and `:503` on `053e9cc`. The counts match the auditor's compiler counts — pass.
- `python3 -I <scratchpad>/rmc-signoff/tables.py`, my own pipe counter (code spans and `\|` excluded): `develop`'s file has 20 tables and 0 mismatches, and the branch's file has 20 tables and 0 mismatches. `awk` over §16 gives 1 … 32, each once. §9.8's Lines column is 6, 4, 0, 198, 39, 1, 1, which sums to 249 — pass.
- `git diff --check develop...HEAD -- docs/tech-design.md`: clean — pass.
- `sed -n '144,153p;244,246p;315,317p' reduced-motion-live-gate/tech-lead-ruling.md` against the added lines: §9 item 2 and F1.4 + FA.4 are verbatim, and §9 items 1, 3 and 4 are carried — pass.
- `git show 053e9cc:…/Clay.kt` `:205-222`, `…/HintUndecidedNotice.kt` `:150-192` and `…/GameActionDock.kt` `:147`, `:174`: as cited. `git grep -c 'rememberAnimationsEnabled()'` gives 15 lines in 9 files. `git grep -E 'Settings\.Global|contentResolver|ContentObserver'` finds one KDoc line, `Clay.kt:212`. `git grep -E '(LaunchedEffect|DisposableEffect)\([^)]*(animationsEnabled|rememberAnimationsEnabled|animate\b)'` exits 1 — pass.
- Sources: `ui-test-android-1.11.3-sources.jar`, `ComposeUiTest.android.kt:591-601` (the recomposer built from `effectContext`, with no scale element of its own) and `:764` (`WindowRecomposerPolicy.withFactory`); `ui-test-junit4-android-1.11.3-sources.jar`, `AndroidComposeTestRule.android.kt:66` (`createComposeRule(effectContext)`); android-35 `ContentResolver.java:715-726` (`NOTIFY_NO_DELAY`, `@hide`) — pass.
- `git cat-file -t` on the 16 shas in the added lines: all are commits, with subjects that fit their use. Every cited report path exists, and so do `docs/ux-spec.md` and `testing/SystemMotionScale.kt`. `sed -n 280,283p 15-founder-feedback/tech-lead-signoff.md`: O-5 — pass.
- `git show 2b98c5c --stat`: 11 files, the six `Settings.Global` classes, `GameScreenHintNoticeTest` and the four moved classes. `7382351` touches `GameScreenHintNoticeTest.kt` — pass.
- `git show develop:.planning/reports/STATUS.md`, line 13: it records the Q-PUBLISH-3.0.0 known issue — pass.
- The trailers of the branch's seven commits: exactly one each, the Fable line, last — pass.

## Open issues / risks
1. **Q-019.** The author, the auditor and this sign-off share Opus. The mitigation is recorded: the auditor's two compiles, plus my static count and my own checks. §16 #19's post-3.0.0 question about a Fable re-audit applies here too.
2. **The deadline.** This branch must be on `develop` before release checklist 1.1 runs. No `release/*` exists today.
3. **#32's number** moves only in case (b) of §3. The merge-time condition catches that case before the merge.
4. **Still owed, not this branch's:** the 3.0.1 notice brief (RM sign-off §4(a); §16 #32), due now that RM has merged and before the 3.0.1 developer dispatch.

## Handoff
Next: producer —
- run §3's merge-time condition. If it holds, `git merge --no-ff feature/tech-design-rm-close` into `develop`. If it fails, follow §3 (b);
- add the STATUS.md Done line: RM's tech-design close is on `develop`, so row RM reads Done for checklist 1.1;
- dispatch a fresh tech-lead for the 3.0.1 notice brief (RM sign-off §4(a), §16 #32);
- keep the three release/3.0.0 QA steps from the RM sign-off's Handoff.

Next: tech-lead (fresh) — the 3.0.1 notice brief. Also the merge-in hunk re-read, only if §3 (b) happens.

SIGN-OFF: PASS
VERDICT: PASS
