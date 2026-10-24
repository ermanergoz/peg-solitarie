# tech-lead report — phase-14 release-docs sign-off (`feature/phase-14-release-docs` into `develop`)
Date: 2026-10-23
Branch: feature/phase-14-release-docs (e5253f4..dfcf063) (committed by the producer: this report is the only file I wrote, and I made no commit)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The product snapshot is 0.6.2 (`.claude/rules/VERSION`) and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; the product's `.claude/rules/lessons.md`. Then android-architecture and android-gradle-logic.
Ran in parallel with: feature/talkback-board (the tech-lead's brief, per the dispatch); Gradle builds in other worktrees (no slugs given; Gradle not run here)
Ran on: claude-fable-5-1 at effort xhigh (the agent file's pin; Q-019 permits Opus for Fable-pinned roles until 3.0.0 is published, but loop 2 and this sign-off ran on the pin, so no staffing deviation applies to this gate)
Fan-out: none
Loops: 0 (this sign-off's first pass; the branch's own review ran one loop, FAIL then PASS)
Read in full: `.planning/reports/phase-14/code-auditor-release-docs.md`, `code-auditor-release-docs-loop2.md`, `tech-lead-design-close.md`, `tech-lead-release-docs-blockers.md`, `release-manager-font-rows.md` (this branch); `docs/tech-design.md` §14.1 and §16 #22 (this branch); the merge-order table of `/Users/ermanergoz/Desktop/PegSolitarie-dev/.planning/reports/15-founder-feedback/tech-lead-signoff.md` (row 3, line 167, and its routed table, line 302)
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **PASS.** The reviews are done: code-auditor loop 1 FAIL (Opus under Q-019, head `4962d13`) → every fix landed on the branch with its own report → code-auditor loop 2 **PASS on Fable** (head `4eedd40`; `dfcf063` adds only that report). Loop 2 verified F-1 to F-8, I-1 and I-2 closed in the documents and re-ran its own checks against `develop`.
- The merge preconditions of Decision 4 row 3 are met and re-verified three times (design-close table, `release-manager-font-rows.md`, loop 2) and once more by me below: phase 15 on `develop` (`c9228a9`), the §2/§7 font rows carry `tech-lead-fonts.md`'s text, the §3/§4 rows are gone (count 0), the OFL-1.1 verdict line is at `THIRD_PARTY.md:103`, and `docs/tech-design.md` carries the supersessions.
- Nothing executable changed: 39 files, +6,288/−10, all under `.planning/`, `docs/`, `CHANGELOG.md`, `THIRD_PARTY.md`; no Check input reads any of them. The Check was not run (as dispatched) and the **qa-engineer gate does not apply** to this merge (disposition below). `git merge-tree --write-tree develop HEAD` is clean (tree `34a9ae6`); the merge-base is `develop`'s head `0582e3f`, so the two-dot and three-dot diffs are the same.
- N-1 to N-8 are routed with an owner, a vehicle and a deadline (table below). Four must land before `release/3.0.0` is cut: N-1 (both halves), N-2a, N-4 and N-7 (unless moot). None blocks this merge: none makes a store text, a licence row or a checklist gate wrong in the unsafe direction.
- I did not write the tech-design content under review (`866dffb..aaaba67`) and this sign-off does not re-approve it on its merits; the code-auditor's Fable read is its review (standing rule 1).

## The gate

### 1. Reviews done
| Step | Report | Model | Verdict | What it settled |
|---|---|---|---|---|
| code-auditor loop 1, 2026-10-21 | `.planning/reports/phase-14/code-auditor-release-docs.md` | opus xhigh (Q-019) | **FAIL** | F-1 (listing called "no winning move" a proof), F-2 (THIRD_PARTY claimed completeness; two unknown licences), F-3 (checklist gaps), F-4 to F-8 MINOR, I-1 to I-4 INFO. Also confirmed row 3's preconditions met at that head (its Summary, line 12) |
| fixes on the branch | `release-manager-release-docs-fixes.md`, `release-manager-release-docs-pass2.md`, `tech-lead-release-docs-f8.md`, `tech-lead-release-docs-blockers.md`, `research-analyst-release-docs-licences.md`, `game-designer-hint-timeout.md` (ruling + Amendment 1, `1afae9f`) | — | — | Every fix is a text edit, each with a report that has `## Checks`, `Ran on:` and `Lessons applied:` |
| code-auditor loop 2, 2026-10-23 | `.planning/reports/phase-14/code-auditor-release-docs-loop2.md` | **fable xhigh** | **PASS** | F-1..F-8, I-1, I-2 closed in the documents (its table, lines 20–33); I-3, I-4 carried INFO; fresh pass found N-1..N-4 MINOR and N-5..N-8 INFO, "none merge-blocking"; re-verified hashes (29 store-art, 5 fonts, 2 OFL, Apache-2.0 text), four Google POM licences, listing limits and word rules in both languages, no TalkBack claim, Q-015..Q-018 and Q-022 respected, no secret in the diff |

Standing rule 1 holds: the tech-design text was written by tech-lead agents on Opus (`866dffb..aaaba67`, the Q-019 deviation recorded in each report) and reviewed by a fresh code-auditor on Fable in loop 2; I am a fresh tech-lead agent, wrote none of it, received no author reasoning and read no `.planning/log/`.

### 2. Merge preconditions — Decision 4 row 3 (`15-founder-feedback/tech-lead-signoff.md:167`)
| Precondition | State | My evidence on `dfcf063` |
|---|---|---|
| Merges after phase 15 | **met** | `git merge-base --is-ancestor c9228a9 develop` exits 0; `develop` = `0582e3f` = this branch's merge-base |
| `THIRD_PARTY.md` §2/§7 font rows carry `tech-lead-fonts.md`'s text | **met** | rows at `:146-147` cite D1 hashes, AC-F5 and the CLEAN verdict; `tech-lead-fonts` is cited 5 times in the file |
| §3 `font_certs.xml` row and §4 `ui-text-google-fonts` row deleted (the design-close's one NOT-met item) | **met** | `awk '/^## 3\./,/^## 5\./' THIRD_PARTY.md \| grep -c -E 'font_certs\|ui-text-google-fonts'` = **0** (was 2 at `f548e32`; closed at `9c0fae7`, `release-manager-font-rows.md` Checks line 1) |
| The research-analyst's OFL-1.1 verdict line | **met** | `THIRD_PARTY.md:103` |
| `docs/tech-design.md` with the sign-off's supersessions | **met** | the 13 items of `tech-lead-design-close.md` part C; my spot-check of six (`| 13 | F-1`, `| 14 | Kotlin and lint warnings`, `| 15 | How the timer is announced`, `R-1 **and phase 15** merged`, `Disjoint regions; phase 15 merges first`, the `Pixel_9a_bench` instrument line) each present |
| Audio and Apache-2.0 rows (release-manager's to place; not a precondition) | placed | design-close table, last row |
| Order relative to R-1 | free | R-1 merged at `c3d3437` |

Re-verified before me by: loop 1 (Summary line 12), `tech-lead-design-close.md` (its table; NOT READY on the one row, since closed), `release-manager-font-rows.md`, loop 2 (fonts, OFL, THIRD_PARTY rows, "Blocker items vs §14.1 and §13.1 … consistent").

### 3. The Check command and the qa-engineer gate
- **Check:** `./gradlew check` was not run, as dispatched. The branch changes no Check input: `git grep` over `build-logic/`, the Gradle files, `app/src/test`, `domain/src/test` and `.github` for every changed file's name finds four lines, all KDoc or comments that cite `14-RELEASE-PLAN.md`, `14-OPERATOR-CHECKLIST.md` or `08-VERIFICATION.md` by name (`app/build.gradle.kts:171`, `GameViewModelHintStalenessTest.kt:50`, `SigningKeystoreProbe.kt:24`, `SigningResolution.kt:5`); nothing reads or parses them. The merged tree's Check result is therefore `develop`'s, last green at the phase-15 merge `c9228a9` (STATUS 2026-10-21, line 105); `0582e3f` after it was the docs-only release-plan merge.
- **qa-engineer gate: not applicable to this merge — tech-lead disposition.** rules/gitflow.md asks for a qa-engineer PASS before a merge to `develop`; a QA pass is a hands-on session on a build (rules/studio.md, "QA checks every step that yields a runnable build"). This branch yields no build: no source, resource, manifest, Gradle or CI file changes, so no device session could observe a difference between `develop` and the merged tree. The device-level claims the documents make (the 15e sitting, the hint behaviour, the fonts in the bundle, the store-art hashes) were verified on their own branches and re-read against `develop` by the code-auditor in both loops, and the release checklist's own device items (4.6a, 5.x) run on `release/3.0.0`. Decision 4 row 3 names no QA step for this branch, and `feature/phase-14-release-plan` merged on the same footing at `0582e3f`. The merge rests on: no Check input touched, code-auditor PASS on Fable, this sign-off. The producer owns the branch's merge footing, as `tech-lead-design-close.md` ("Review") recorded.

### 4. Mechanics
- `git merge-tree --write-tree develop HEAD` → `34a9ae6…`, exit 0, no conflicts. Merge-base `0582e3f` = `develop` head, so nothing on `develop` moved since `49383ff` took it in.
- Footprint: 39 files, +6,288/−10; `git diff --name-only develop HEAD | grep -v -E '^(docs/|\.planning/|CHANGELOG\.md|THIRD_PARTY\.md)'` prints nothing (two-dot and three-dot alike).
- `develop` today has **no** `THIRD_PARTY.md`, `CHANGELOG.md`, `docs/tech-design.md` or `docs/release-checklist.md`; this merge brings all four to the integration branch for the first time (rules/ip-policy.md: THIRD_PARTY.md is mandatory). Holding the merge would keep `develop` without them and keep `bugfix/hint-undecided`'s reviewers waiting (§14.1 R1: it merges `develop` in once release-docs has merged).
- Commits: the trailer rule holds from `e5253f4`'s successors on; `e5253f4` and `d9f9a60` carry the Opus trailer (loop 1 I-3, carried; not worth a rewrite, as the phase-15 sign-off ruled).
- Report validity in the diff: every report added has `## Checks` and `Lessons applied:`; `release-manager-ad-safety.md` and `release-manager-release-docs.md` lack `Ran on:` (pre-1.0.0 rule; loop 1 I-4, carried); `release-manager-founder-items.md` and `tech-lead-r2-gate-brief.md` are documents, not reports (loop 1 I-4).
- Worktree `/Users/ermanergoz/Desktop/PegSolitarie-r5` was clean at `dfcf063` before this report.

### 5. The open items routed — N-1 to N-8 (loop 2)
Vehicle rule for every `docs/tech-design.md` edit below: the file reaches `develop` only with this merge, so a branch cut at `0582e3f` (`feature/talkback-board`, `bugfix/hint-undecided`) can edit it **only after it merges `develop` in post-merge**. Edits on concurrent branches go in distinct sections; §14.1's second-to-merge duties cover the rest (mechanical keep-both).

| Id | What | Owner | Vehicle (as a later docs commit) | Deadline | Must land before `release/3.0.0`? |
|---|---|---|---|---|---|
| N-1a | §14.1: a row for `feature/talkback-board` with **Blocks the branch cut** (Q-023 option 1, checklist 1.1a) and a lettered merge-order step; this closes the disagreement between §14.1's "no row, no cut deadline" note and 1.1a | **tech-lead** | the talkback brief's commit on `feature/talkback-board`, after that branch takes `develop` in (the brief fixes the branch's merge slot, so the row is written with it) | before that branch's first developer dispatch; in any case before checklist 1.1 is run | **yes** — 1.1 reads §14.1 at the cut |
| N-1b | 1.1a's evidence cell: `git merge-base --is-ancestor feature/talkback-board develop` is vacuous today (the branch is `develop`'s head, 0 commits). Name the merge commit (`git log --merges --grep='talkback-board' develop -1`) or require `git rev-list --count develop..feature/talkback-board` > 0 before the merge, beside the three sign-offs | **release-manager** | the release-manager's next checklist docs commit (a small docs feature branch off `develop` after this merge; the producer sets its review footing as for this branch) | before 1.1a is ticked | **yes** |
| N-2a | 1.4 is a pre-cut item worded to need 3.5 and the AAB (post-cut): as written it cannot be ticked. Reword to the pre-cut conditions only (every shipped row has a known licence; `feature/licences-screen` merged; §16 #20 closed); U-6 and the AAB listing stay with 3.5 and 4.6a | **release-manager** | with N-1b | before 1.4 is ticked | **yes** — else the cut's own checklist cannot be completed |
| N-2b | §13.1 LN-7 item 4 cites 1.4 for an AAB check; cite 3.5 and 4.6a | **tech-lead** | the first tech-design edit after this merge (with N-1a if the talkback brief is first; otherwise its own docs commit) | before `feature/licences-screen`'s qa-engineer runs LN-7 | yes in effect (LN is a cut blocker and its QA precedes the cut) |
| N-3 | 4.6a lists only the five fonts and two OFL texts; add LN's assets (`google/index.tsv`, its stems, rows F–J texts, the Jakarta NOTICE), conditional on `feature/licences-screen` having merged | **release-manager** | with N-1b, or as a docs commit on `release/3.0.0` (checklist §2 scope allows a docs commit there) | before 4.6a runs on the release branch | no — a post-cut docs commit is acceptable; before is cleaner |
| N-4 | §16 #22 still reads **Open**; the game-designer's Amendment 1 (`1afae9f`, AC-18a–d, AC-2c) closed it after the tech design's last edit (`aaaba67`). Set the status to closed with the amendment cited, and add the §17 row | **tech-lead** | with N-2b (the same tech-design commit) | before the qa-engineer dispatch on `bugfix/hint-undecided` (its §14.1 row sends QA to §16 #22) | **yes** in effect — R1 blocks the cut, and a stale "Open" would send its QA to wait for a correction that exists |
| N-5 | `docs/tech-design.md:203` and `:1068` cite `docs/tech/audio-silent-mode.md`, which is on `feature/audio-review` only; annotate "(on `feature/audio-review` until it merges)" | **tech-lead** | with N-2b; moot once `feature/audio-review` merges | next sync | no |
| N-6 | `THIRD_PARTY.md:379-381` reads as if `feature/licences-screen` were built (`:22`, `:130` say not yet; no such ref exists); `:93` "a third licence text" understates LN-5's set | **release-manager** | with N-1b, or at checklist 2.4a (the pending-branch clean-up) | before 2.4a | no — wording only |
| N-7 | 1.1a cites `.planning/questions/FOUNDER.md` for Q-023, which is on `feature/ux-spec-folds` (`415e5b2`), not on `develop`; name the branch until it merges | **release-manager** | with N-1b; moot if `feature/ux-spec-folds` merges first | before 1.1a is ticked | **yes**, unless moot by then |
| N-8 | `docs/tech-design.md:703` "36 test files" in `:domain:test`; `develop` has 37 `.kt` files (30 `*Test.kt`) | **tech-lead** | next sync | next sync | no |

Nothing above changes product code: the tech-lead items are text in the tech design; the release-manager items are text in the checklist and `THIRD_PARTY.md`.

## Outputs
- `.planning/reports/phase-14/tech-lead-signoff-release-docs.md` — this report. The only file written; nothing else edited; no commit; no merge.

## Checks
- `git -C PegSolitarie-r5 status --short` — pass (empty before this report; head `dfcf063`, `develop` `0582e3f`).
- `git merge-base develop HEAD` — pass (`0582e3f`, `develop`'s head).
- `git merge-tree --write-tree develop HEAD` — pass (exit 0, tree `34a9ae6a638e2fca3b1ca959a7f2baf1b5be3ba0`, no conflicts).
- `git diff --stat develop...HEAD` and `git diff --stat develop HEAD` — pass (both 39 files, +6,288/−10).
- `git diff --name-only develop HEAD | grep -v -E '^(docs/|\.planning/|CHANGELOG\.md|THIRD_PARTY\.md)'` — pass (no output; same for the three-dot form).
- `git cat-file -e develop:{THIRD_PARTY.md,CHANGELOG.md,docs/tech-design.md,docs/release-checklist.md}` — all absent on `develop` (this merge adds them).
- `git merge-base --is-ancestor c9228a9 develop` — pass (phase 15 is on `develop`).
- `awk '/^## 3\./,/^## 5\./' THIRD_PARTY.md | grep -c -E 'font_certs|ui-text-google-fonts'` — pass (0).
- `grep -n 'OFL-1.1 verdict' THIRD_PARTY.md` — pass (`:103`); `grep -c tech-lead-fonts THIRD_PARTY.md` — 5.
- `grep -c -F -- '<text>' docs/tech-design.md` for six part-C supersession strings — pass (each ≥ 1).
- `git grep -n -E 'THIRD_PARTY|CHANGELOG|tech-design|release-checklist|ad-testing-safety|docs/store|OPERATOR-CHECKLIST|RELEASE-PLAN|08-VERIFICATION' HEAD -- build-logic app/build.gradle.kts domain/build.gradle.kts build.gradle.kts settings.gradle.kts app/src/test domain/src/test .github` — 4 hits, all comment lines; no Check input reads a changed file.
- `git rev-list --count develop..feature/talkback-board` — 0 (N-1b's premise holds today).
- `grep -n -E '^\| *(1\.1|1\.1a|1\.4|1\.7|3\.5|3\.5a|4\.6a) ' docs/release-checklist.md` and `grep -n '^| 22 |' docs/tech-design.md` — read; N-1 to N-4 as loop 2 describes them (#22 at `:1442` reads "**Open.**").
- Report validity loop over every `.planning/reports/` file in the diff (`## Checks`, `Lessons applied:`, `Ran on:`) — pass except the two pre-1.0.0 reports and the two documents loop 1 I-4 names.
- `git log -5 --format='%(trailers)'` — pass (the Fable trailer on each of the last five commits).
- `grep -n -E '^## Q-' .planning/questions/FOUNDER.md` — Q-015..Q-022 on this branch; Q-019's answer is option 3 (Opus permitted); no later entry changes staffing; Q-023 is not on this branch (N-7).
- `./gradlew check` — **not run**, as dispatched; the branch changes no Check input. No Gradle was started.
- I did not read `.planning/log/`.

## Open issues / risks
1. **Concurrent tech-design edits.** After this merge, §14.1 and §16 will be edited on `feature/talkback-board` (N-1a, N-4, N-2b), by the R1-merge rewrite (§7.2, §16 #21, §14.1 R1 row) and by `feature/licences-screen`. Each branch edits distinct rows or sections; the second to merge takes `develop` in and resolves keep-both. Not a blocker; the producer sequences it.
2. **1.1a's evidence is vacuous today** (N-1b): the three-sign-offs clause is the only real guard until the release-manager rewords the cell. The release-manager must not tick 1.1a on `--is-ancestor` alone.
3. **§14.1 and checklist 1.1a disagree** on whether the cut waits for TalkBack until N-1a lands; the checklist is the stricter and is what the release-manager runs, so no unsafe release follows in the meantime.
4. **Owed by the tech-lead, not in this sign-off:** the colour-vision brief (§14.1 6b, direction-B A9); the R1-merge rewrite listed in §14.1's R1 row; the Google-notices ruling is made (§13.1), its screen is `feature/licences-screen`'s; `tech-lead-fonts.md` AC-F4's grep pattern is not literally true on `develop` (`Nunito-OFL.txt:1` carries the upstream URL; `release-manager-font-rows.md`, open issue 1) — harmless, fix the pattern at the next sync.
5. The two reports without `Ran on:` and the two Opus trailers stay as both loops left them.

## Handoff
Next: producer —
1. Commit this report on `feature/phase-14-release-docs` in its own commit (`docs(14): record the tech-lead release-docs sign-off (PASS)`), then `git checkout develop && git merge --no-ff feature/phase-14-release-docs` (serial; `merge-tree` is clean). STATUS Done line: `2026-10-23 | phase-14 release-docs | release-manager, research-analyst, game-designer, tech-lead, code-auditor ×2, tech-lead sign-off | PASS (loop 2 on Fable; no QA: docs only) | <the report paths above>`.
2. Tell the tech-lead writing the talkback brief: `docs/tech-design.md` exists on that branch only after it merges `develop` in post-merge; its commit carries N-1a, N-2b, N-4, N-5 (one tech-design edit, distinct sections).
3. Dispatch the release-manager for N-1b, N-2a, N-6, N-7 (one checklist/THIRD_PARTY docs commit, before the cut) and N-3 (before 4.6a runs; may ride `release/3.0.0`).
4. `bugfix/hint-undecided` may now merge `develop` in (§14.1 R1, brief §0) and go to its reviews; its qa-engineer reads §16 #22 as closed by Amendment 1 (`game-designer-hint-timeout.md`, `1afae9f`) until N-4 lands.

VERDICT: PASS
