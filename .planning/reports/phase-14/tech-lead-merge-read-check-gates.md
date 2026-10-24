# tech-lead report — re-read of check-gates' `develop` merge-in (`ffa49aa`) before its merge into develop
Date: 2026-10-24
Branch: feature/phase-14-check-gates (committed by the producer). Read: merge commit `ffa49aa` (parents `c632b45` = the branch, `b1a6cf9` = `develop`; merge-base `c5f2c54`); head `014e507` adds only the developer's merge report, and `docs/` is identical at `ffa49aa` and `014e507`. My edits to `docs/tech-design.md` are uncommitted in `/Users/ermanergoz/Desktop/PegSolitarie-gates` (+14/−6). I committed nothing, merged nothing and ran no Gradle, as dispatched.
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; plus `.claude/rules/lessons.md`). android-architecture and android-gradle-logic were not loaded: no code or build file conflicted, and the read covers two docs files only. The one build fact I cite, `build.gradle.kts:18-20`, I read in the file.
Ran in parallel with: none dispatched with this task
Ran on: claude-opus-5-5 at effort xhigh (policy). **Deviation from studio staffing:** the tech-lead's pin is fable at xhigh. The producer's dispatch states that the Fable limit is reached, under Q-019 (`.planning/questions/FOUNDER.md`, answered 2026-10-17 with option 3, "Opus for every Fable-pinned role until 3.0.0 is published"). Q-019's addendum of 2026-10-23 restored Fable for supervision; the dispatch says that limit is reached again today, and I cannot check that myself. As a result, an Opus merge (the android-game-dev at max) is re-read on Opus. Mitigation, as Q-019 asks: I am a fresh reader, and every resolution below is tested mechanically (row hashes, line sets, `git grep`, the JUnit XML), independent of the developer's report.
Fan-out: none
Loops: 0
Read in full: `git show --cc ffa49aa -- docs/tech-design.md docs/release-checklist.md` (205 lines, two pages); `.planning/reports/phase-14/tech-lead-signoff-tech-design-sync.md` (§4 = item 4, hunk rules 1–7 and the renumbering rule); `.planning/reports/phase-14/tech-lead-signoff-check-gates.md`; `.planning/reports/phase-14/android-game-dev-check-gates-merge.md`; Q-019 with its addendum. VERDICT and `Ran on:` lines read from `.planning/reports/phase-14/code-auditor-check-gates.md` and `.planning/reports/phase-14/qa-engineer-check-gates.md`. Not read: `.planning/log/`.
Lessons applied: none (0 active lessons in the snapshot; `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

## Summary
- **PASS: the merge may proceed** once my design edit is committed. All seven conflict regions follow hunk rules 1–7 and item 4's renumbering. Every line either side added survives, except the five rule points. Each conflicting checklist row was edited by one side only and was taken from that side byte for byte. The §14.1 region merged clean.
- **Renumbering is complete:** §16 now runs 1–27 with no duplicate number. The three renumbered rows and the header line equal check-gates' text in everything but the numbers. The header was check-gates' only cross-reference to its own rows, and no other tracked file cites §16 #23–#27.
- **§17:** I confirm the renumbering row (`:1684`) and add two facts to it. I wrote item 4's rule into §17 as its own row (`:1685`), which supersedes step 3b's "keeps both" when that is read as the whole rule; step 3b now points to it. Two clauses of that row are this re-read's own decisions, named below.
- **Issue 2, ruled and fixed:** the §8.1 cell now holds the merged tree's own figure, 294 tests in 33 classes from 41 `.kt` files. **Issue 3, ruled and fixed:** the root `check` has reached build-logic's tests since `7b28007`. §8.1 is corrected, and the stale `ci.yml:48-49` comment is queued with auditor finding 7's next edit (§9.7, §14.2), with a §17 row (`:1686`).

## The re-read, hunk by hunk
Line numbers are those of the merged file at `ffa49aa`.

| # | Region | Rule | Resolution | Verified |
|---|---|---|---|---|
| 1 | Header `:23-34` | 1: keep both, tech-design-sync's first; only "#23–#25" → "#25–#27" | tech-design-sync's sentence (`:23-29`), then check-gates' (`:30-34`) | Line set: only two header lines differ from both sides. The check-gates line equals the merged line with "#23–#25" substituted (md5). Develop's line "Every decision below is a tech-lead decision…" was dropped as a line, and the identical sentence survives once, at the end of check-gates' line (`grep -c` → 1). |
| 2 | §8.1 `:domain:test` row `:731` | 2: check-gates' cell, develop's count as a dated note | As ruled | Base row: "36 test files … \| JVM, JUnit4 + Truth \| per commit \| ~31 s". Develop changed only the item cell, so the runner and cost cells ("under kover's agent (§8.4)", "~30 s") are check-gates' alone and rightly kept. Superseded now by issue 2's fix. |
| 3 | Merge order `:1478-1490` | 3: keep both | Develop's 3a amendment and step 3b, then step 4 with check-gates' amendment, in the document's order | Line set: nothing dropped |
| 4 | §16 `:1612-1617` | 4 plus the renumbering rule | Develop's #22 (closed), #23 and #24, then check-gates' #25, #26 and #27 | Row numbers in §16: 1…27, no gap or duplicate. Each renumbered row equals check-gates' #23, #24 or #25 with only the number changed (md5 after substitution). |
| 5 | §17 `:1661-1678` | 5: keep both, tech-design-sync's then check-gates' | Develop's 10 rows, check-gates' 7, then the renumbering row | Counted; line set: nothing dropped |
| 6 | Checklist `:32-33` (1.1a, 1.2; 1.4 in the same hunk) | 6: adjacency, keep both | Develop's 1.1a and 1.4, check-gates' 1.2 | Row md5 at base, check-gates, develop and merge: 1.1a and 1.4 changed only on develop, 1.2 only on check-gates; each merged row equals its editing side's row |
| 7 | Checklist `:69-73` | 7: adjacency, keep both | Check-gates' 3.1–3.4, develop's 3.5 | Same test: 3.1–3.4 changed only on check-gates, 3.5 only on develop; each merged row equals its editing side's row |
| — | §14.1 table `:1434-1455` | not a conflict | git's auto-merge: R-2, R-6, R-7 and DT from check-gates; F-1, TB, Audio, LN and TDS from develop | Every row's cell count matches its header (20 tables in the file, 0 mismatches after my edits) |

No conflict markers remain. `git diff --check` is clean. In `docs/release-checklist.md`, no line either side added is missing and no line is new.

## #23–#25 → #25–#27 everywhere
- On check-gates (`c5f2c54..c632b45`), the only added line that cites the branch's own numbers, besides the rows themselves, is the header's "§16 #23–#25". It now reads "§16 #25–#27".
- §8.6, §9.6 (its known limit), §14.2, row DT and §15's `kill -9` row point to §8.6, §9.6 and §14.2, never to a §16 number. The one §16 pointer in those sections is §16 #8 at `:967`, which is unaffected.
- Develop's citations of its own #23 and #24 (row TDS, step 3b, the §17 TB row) still name the right rows.
- `git grep` at `ffa49aa` over `docs/` (excluding the design), `THIRD_PARTY.md`, `CLAUDE.md`, `AGENTS.md`, `README.md` and `CHANGELOG.md` finds no §16 #23–#27 citation. The reports that cite the old numbers (the R-2 sign-off's rulings 3–5) are records and stay as written, by the rule.

## §17: the renumbering row confirmed, and item 4's rule written in
**The renumbering row (`:1684`, formerly `:1678`) is confirmed.** `74eec52` is check-gates' transcription commit (2026-10-24, on the branch). `feature/tech-design-sync` reached `develop` first (`f478ae9`, before R1's `b1a6cf9`). "Here only the header's" is true. I added two facts and removed nothing: the merge commit `ffa49aa`, and "at the merge-in's re-read no other tracked file cited the three numbers".

**Item 4's rule (new row `:1685`).** It supersedes §14.1 step 3b's "takes `develop` in and keeps both, distinct sections" when that sentence is read as the whole rule for a merge-in that conflicts in this file. Step 3b gains a dated pointer to the row. The row says:
- The branch that merges second takes `develop` in on its own branch, never a rebase. Its developer makes the merge-in. On a docs-only branch with no developer, the producer makes it (item 4 (d)).
- The Check re-runs on the merged tree. A fresh tech-lead re-reads `git show --cc` before the `--no-ff` merge. Docs hunks need no code-auditor.
- Hunk rule (1), header sync sentences: keep both, develop's first.
- Hunk rule (2), different rows, steps or sections, and adjacency: keep both, in the document's order.
- Hunk rule (3), numbering: the second branch to merge continues after develop's highest number, updates every cross-reference in the file, and adds a §17 row naming the old numbers. Reports that cite the old numbers are records and stay as written.
- Hunk rule (4), one figure measured at two trees: not keep-both (see below).
- Hunk rule (5), §17 rows: keep both, develop's first.
- **Two clauses are this re-read's own tech-lead decisions**, recorded as such in the row:
  - **The figure clause** in rule (4): the cell takes the merged tree's own figure when the merge-in's Check measures it, else the later tree's. Item 4 said "the later tree's", and this merge-in shows why that is not enough: the later tree's 280 was already wrong for the merged tree.
  - **The stop:** a hunk that none of the rules decides is a choice of text, not merge mechanics. It stops and comes to the tech-lead before the merge-in is committed. This follows from item 4 (c) and rules/studio.md rule 4.

## Open issue 2 — the §8.1 count: ruled, fixed
**Ruling:** the cell describes the suite as it is on the tree it documents, and the merged tree is the one that goes to `develop`. Check-gates' "280 tests in 40 files" was correct for 280 tests on that branch, but it does not describe the merged tree, so the cell takes the merged tree's own measurement (the figure clause above).

**The facts, read here:**
- `git ls-tree` counts under `domain/src/test`: `c5f2c54` and `b1a6cf9` have 37 `.kt` files, 30 of them `*Test.kt`. `c632b45` and `ffa49aa` have 41 `.kt` files, 33 of them `*Test.kt`; the other 8 are helpers such as `TestFlags.kt` and `FuzzSeeds.kt`. Check-gates' "40 files" was a miscount: git counts 41 at `c632b45` and at `74eec52`. R1 added tests inside existing files.
- `domain/build/test-results/test/` holds 33 suites: 294 tests, 0 skipped, 0 failures, 0 errors. The files are dated 08:48:21, inside the developer's Check window (08:35:32–08:48:24) on this worktree, whose code is `ffa49aa`.

**Fixed:** `:731` now reads "294 tests in 33 classes, from 41 `.kt` files, 33 of them `*Test.kt` (measured on the merged tree `ffa49aa` …)". The three earlier texts are kept in the dated "until …" note, with the 40/41 discrepancy named. §17 `:1686` records the change.

## Open issue 3 — build-logic "not reachable from the root `check`": ruled, fixed
**Ruling:** the text is wrong and has been since 2026-08-27. `build.gradle.kts:18-20` makes the root `check` depend on `gradle.includedBuild("build-logic").task(":check")` (PLAT-06, `7b28007`, an ancestor of the base `c5f2c54`). `build-logic/build/test-results/test/` holds 6 suites with 55 tests, written at 08:35:47, in the first seconds of the developer's Check. CI's `./gradlew -p build-logic test` step therefore stays as a fail-fast first run of the release-gate contracts, but it is not their only run. This is a text correction only. No gate changes, and no build or workflow change is asked for.

**Fixed:**
- §8.1 `:736`: the Gate cell now reads "`check`: the root `build.gradle.kts:18-20` …; CI also runs them first, alone, to fail fast". The old text is kept, dated, with "stale since `7b28007`".
- §9.7: records that `ci.yml:48-49`'s step comment ("not part of the root `check` below") is stale.
- §14.2: the comment is corrected in the same edit as auditor finding 7 (`ci.yml:58`), the file's next edit. It is the android-game-dev's, never on the release branch, and no merge waits on it.
- §17 `:1686` records the change.

## Merge preconditions (rules/gitflow.md), as read
- **Check green on the merged tree:** the developer's report gives `./scripts/check.sh --max-workers=4` on `ffa49aa`, exit 0, 1,447 tests. The JUnit XML on this worktree agrees: `:app:testDebugUnitTest` 1,096 (100 suites, 08:38), `:domain:test` 294 and build-logic 55, all with 0 failures, skips or errors. Adding the developer's figures for depthSweep (1) and fuzz (1) gives 1,447. I re-ran no Gradle task, as dispatched.
- **Code-auditor PASS:** `.planning/reports/phase-14/code-auditor-check-gates.md` (VERDICT: PASS, on fable). It is not needed for this merge-in, because no code hunk conflicted and C7 met R1's code with no new warning. That is the developer's report (0 `w:`/`e:` in main or `:domain`); the XML cannot show it, and I accept it under the R-2 sign-off's ruling 2, which asks for no new tech-lead round on a green merged Check.
- **QA-engineer PASS:** `.planning/reports/phase-14/qa-engineer-check-gates.md` (VERDICT: PASS, on fable, the review-mode session of ruling 1).
- **Tech-lead sign-off:** `.planning/reports/phase-14/tech-lead-signoff-check-gates.md` (PASS), plus this re-read of the merge-in's hunks, as §14.1's pair rule requires.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-gates/docs/tech-design.md` — uncommitted, +14/−6:
  - §8.1 `:731` (issue 2) and `:736` (issue 3)
  - §9.7 `:1004-1007` (the `ci.yml:48-49` comment)
  - §14.1 step 3b `:1490-1492` (the pointer)
  - §14.2 `:1560-1561` (the comment queued with finding 7)
  - §17 `:1684` (two additions), new `:1685` (the merge-in rule) and new `:1686` (the §8.1 corrections)
  
  Every replaced text is kept in a dated note; nothing is deleted.
- `/Users/ermanergoz/Desktop/PegSolitarie-gates/.planning/reports/phase-14/tech-lead-merge-read-check-gates.md` — this report.

## Checks
All run in `/Users/ermanergoz/Desktop/PegSolitarie-gates`. No Gradle, no emulator, no `.planning/log/`.
- `cat .claude/rules/VERSION` → `0.6.2`; plugin `1.0.0` — pass (plugin rule files read)
- `git cat-file -p ffa49aa` → parents `c632b45`, `b1a6cf9` — pass. `git diff --quiet ffa49aa HEAD -- docs/` — pass (identical).
- `git show --cc ffa49aa -- docs/tech-design.md docs/release-checklist.md` — read in full (205 lines)
- Row md5 of 1.1a, 1.2, 1.4 and 3.1–3.5 at `c5f2c54`, `c632b45`, `b1a6cf9` and `ffa49aa` — pass (each row changed on one side only; merged = that side)
- `comm` line-set test, for both files and both sides — pass. Design: the only lines either side added that are missing from the merge are the two header lines, check-gates' #23–#25 rows and the two §8.1 cells, all rule points. The only merged lines in neither side are their resolutions and the §17 row. Checklist: none and none.
- md5 of check-gates' #23/#24/#25 rows and header line with the numbers substituted, against the merged #25/#26/#27 and header — pass (identical)
- `awk` over §16's rows → `1 … 27` — pass, before and after my edits
- `grep -c 'Every decision below is a tech-lead decision'` → 1 — pass
- `git diff c5f2c54 c632b45 -- docs/tech-design.md`, added lines matching `#2[3-5]` → only the header — pass. `awk` for `§16` in §8.6, §9.6 and §14.2 → only §16 #8 (`:967`) and §16 #20 — pass.
- `git grep -nE '§16 (item |#)?2[3-7]|item 2[3-7]\b' ffa49aa -- docs/ THIRD_PARTY.md CLAUDE.md AGENTS.md README.md CHANGELOG.md ':(exclude)docs/tech-design.md'` → no output — pass
- `git log -1 74eec52` → "docs(14): transcribe the check-gates figures and rulings into the design", on check-gates — pass. `git merge-base --is-ancestor fd2c220 b1a6cf9` — pass. `git log --merges --first-parent c5f2c54..b1a6cf9` → `4ff4ec6`, `f478ae9` (tech-design-sync), `2d045d1`, `b1a6cf9` (R1) — pass.
- `git ls-tree -r --name-only <rev> -- domain/src/test` counts: 37/30 at `c5f2c54` and `b1a6cf9`; 41/33 at `c632b45`, `74eec52` and `ffa49aa`; 38 at `2520fa7`
- JUnit XML totals:
  - `domain/build/test-results/test` → 33 suites, 294 tests, 0 skipped/failures/errors, 08:48:21
  - `app/build/test-results/testDebugUnitTest` → 100 suites, 1,096 tests, 0/0/0, 08:38
  - `build-logic/build/test-results/test` → 6 suites, 55 tests, 08:35:47
- `sed -n 18,20p build.gradle.kts` → `tasks.named("check") { dependsOn(gradle.includedBuild("build-logic").task(":check")) }` — pass. `git log -1 7b28007` → 2026-08-27 "adIdLiteralCheck gate and build-logic tests under root check"; ancestor of `c5f2c54` — pass.
- `sed -n 44,56p .github/workflows/ci.yml` → the stale comment at `:48-49` — confirmed
- After my edits:
  - Python pipe count per table row (escaped `\|` excluded) → 20 tables, 0 mismatched rows — pass
  - `git diff --check` — clean
  - conflict markers → 0
  - `git diff --stat` → `docs/tech-design.md | 20 ++++++++++++++------` (+14/−6)
  - `git status --short` → ` M docs/tech-design.md` (plus this report)
  - `wc -l` → 1,686 (was 1,678)
  - `grep -cF` of each new phrase → 1 for all eight
  - `git diff -U0`'s removed lines → exactly the six replaced lines (`:731`, `:736`, `:1004`, `:1487`, `:1555`, `:1678`)
- `./gradlew check` — **not run**, as dispatched

## Open issues / risks
- **My design edits have had no reader but me** (standing rule 1). They are a tech-lead's corrections to the tech-lead's own document, docs only, made as this dispatch allowed. The next text read of this file covers them: the code-auditor's read of `feature/tech-design-r1-close`, or the next sync's. This follows the tech-design-sync sign-off's precedent and does not hold the merge.
- **Q-019 deviation:** an Opus re-read of an Opus merge, recorded above. Every resolution was tested mechanically rather than taken from the developer's report.
- **`feature/tech-design-r1-close`** (R1's owed §7.2, §10, §15, §16 #21 and book-count edits; R1's sign-off, `.planning/reports/hint-undecided/tech-lead-signoff.md`): if it is cut from `develop` before check-gates merges, its merge-in falls under §17's new rule. It closes #21 and adds no §16 number, so expect rules (2) and (5) only.
- **`ci.yml:48-49`** — android-game-dev, with finding 7's next edit of the workflow (§14.2). It is never on the release branch and blocks nothing.
- **The merge report's open issue 1** (a shared scratchpad log overwritten by another worktree's build) is a dispatch matter, not a design one. The producer gives each dispatched Check a unique log path, as §9.6's operator rule already expects of hand-run builds.
- **The merge report's open issue 4** is done (the §17 rule, `:1685`).

## Handoff
Next: producer —
1. Read my `docs/tech-design.md` hunks (`git diff` in `PegSolitarie-gates`) and commit them, e.g. `docs(14): merge-in re-read - §17 merge rule, §8.1 figures`, then commit this report.
2. Merge `feature/phase-14-check-gates` into `develop` with `--no-ff`. The preconditions above are met: the merged-tree Check is green, and the code-auditor PASS, qa-engineer PASS, tech-lead sign-off and this re-read are in place.
3. Carry `ci.yml:48-49` into §14.2's next workflow edit (android-game-dev). Dispatch `bugfix/solver-deadline-task` (row DT) per the R-2 sign-off, now that R1 and R-2 will both be on `develop`.
4. STATUS.md: this task's Done line with `.planning/reports/phase-14/android-game-dev-check-gates-merge.md` and this report; the Q-019 deviation is recorded here.

VERDICT: PASS
