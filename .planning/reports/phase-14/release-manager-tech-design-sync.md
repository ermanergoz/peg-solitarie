# release-manager report — tech-design-sync, checklist (N-1b, N-2a, N-3, N-6, N-7)
Date: 2026-10-24
Branch: feature/tech-design-sync (committed by the producer; cut from develop `4ff4ec6`, uncommitted edits as dispatched)
Skills loaded: ergoz-soft:standards (role release-manager; game; android-compose-game). The snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy; also `.claude/rules/lessons.md`. gitflow. gsd-complete-milestone was not invoked: this task is a docs edit with no milestone close, and it has nothing to say about one.
Ran in parallel with: tech-lead edit of `docs/tech-design.md` in this worktree (N-1a, N-2b, N-4, N-5, N-8)
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
I closed the five release-manager items from `tech-lead-signoff-release-docs.md` §5 and `code-auditor-release-docs-loop2.md`. All edits are text only, in `docs/release-checklist.md` (1.1a, 1.4, 3.5, 4.6a) and `THIRD_PARTY.md` (`:93`, `:379-381`). I did not touch `docs/tech-design.md`, ran no Gradle and made no commit.
- 1.1a's evidence no longer passes vacuously. It now requires four things, and the bare `--is-ancestor` test never ticks it: the merge commit, board code in the merge diff, reviews of the merged `app/` tree, and a TalkBack device QA PASS (VOID and BLOCKED are not a PASS).
- 1.4 now holds pre-cut conditions only. U-6 and §5b's open item go to 3.5, and the AAB listing goes to 4.6a.

## Outputs
- `docs/release-checklist.md` — the changed items:
  - **1.1a (N-1b):** the evidence cell is rewritten, with four conditions (1)–(4).
  - **1.1a (N-7):** the Q-023 citation now points to FOUNDER.md on `develop` (`415e5b2`, merged at `4ff4ec6`).
  - **1.4 (N-2a):** reworded to the pre-cut half. `feature/licences-screen` must have merged, shown by its merge commit, not by ancestry alone. §16 #20 must have nothing open beyond what its own cell leaves to the cut.
  - **3.5:** marked as the post-cut half of 1.4.
  - **4.6a (N-3):** adds (a) `google/index.tsv`, (b) every stem file it names, (c) LN-5's texts with hash prefixes, plus `Apache-2.0.txt`, and (d) the index sources equal to the AARs that carry `third_party_licenses.json` (LN-7 item 4). These checks are never skipped.
- `THIRD_PARTY.md` (N-6):
  - **`:93`:** "a third licence text" now reads as the Licenses screen's full notice set: Apache-2.0, rows F–J, the Jakarta NOTICE, and the generated `google/` notices.
  - **`:379-381`:** "it is built" now reads "owed … **not yet built**: on 2026-10-24 the branch holds only its spec commit (`0bf578d`) and no code". This matches `:22` and `:130`.
- `.planning/reports/phase-14/release-manager-tech-design-sync.md` — this report.

## Checks
- `git log --oneline develop..feature/talkback-board` — 3 docs commits (`26910de`, `23ca39a`, `3e5b893`), only `tech-lead-brief.md`. A count > 0 alone would now pass vacuously too, so 1.1a requires board code in the merge diff, not a non-zero count.
- `git log --merges --first-parent develop --grep="Merge branch 'feature/talkback-board'" --format=%H -1` — prints nothing today, so 1.1a(1) fails correctly.
- Dry run of 1.1a(2) on `feature/ux-spec-folds`'s merge `4ff4ec6`: `git diff --name-only $M^1 $M -- app/src/main/` — empty, so a docs-only merge is rejected as intended.
- `git merge-base --is-ancestor 415e5b2 develop` — exit 0 (Q-023 is on `develop`; N-7 grounded). `grep -n '^## Q-023' .planning/questions/FOUNDER.md` — `:3`, answered, option 1.
- `git merge-base --is-ancestor feature/licences-screen develop` — not merged. `git log develop..feature/licences-screen` — `0bf578d` only. `git ls-tree` shows only the two OFL texts under `assets/licenses/` (N-6 wording grounded).
- `grep -c` of `6e5e1173 368cca11 e90eaa08 8a9c58fb 992ed62d` in `docs/tech-design.md` — 1 each; `cfc7749b` in `THIRD_PARTY.md` — 1 (4.6a's prefixes match their sources).
- Edit greps — each prints the expected line:
  - `grep -c 'The merge, not the ancestry test' docs/release-checklist.md` — 1
  - `grep -n -o 'FOUNDER.md\` on \`develop\`, recorded in \`415e5b2\`'` — `:32`
  - `grep -n -o 'the pre-cut half only'` — `:35`
  - `grep -n -o 'the post-cut half of item 1.4'` — `:73`
  - `grep -n -o 'google/index.tsv'` — `:90`
  - `grep -n -o 'Jakarta-Inject-NOTICE.md\` and \`Apache-2.0.txt\`'` — `:90`
- THIRD_PARTY greps:
  - `grep -n 'third licence text' THIRD_PARTY.md` — no hit
  - `grep -n -o "the Licenses screen's notice texts"` — `:93`
  - `grep -n -o 'holds only its spec commit (\`0bf578d\`)'` — `:381`
- `git diff --check` — clean. `git status --short` — only `THIRD_PARTY.md` and `docs/release-checklist.md` modified (plus this report).
- Gradle — not run, as dispatched.

## Open issues / risks
- **Paths in 4.6a(c) are not fixed yet.** 4.6a(c) names LN-5's texts by content and hash, not by file name, because `BundledLicenseText` does not exist yet. At the cut, the paths come from that class on `release/3.0.0`.
- **1.1a(2) assumes the brief's file layout.** It names `ui/components/BoardSpeech.kt` from the brief (`tech-lead-brief.md:156`, `:277`). If an amendment renames it, that one example goes stale, but the condition still holds: `app/src/main/` must be non-empty.
- **`THIRD_PARTY.md:147` still cites 4.6a for AC-F5 alone.** This is still correct, since 4.6a keeps AC-F5, so I left it.
- **No item is ticked.** Every box stays unticked until the cut.

## Handoff
Next: producer — review the diff (code-auditor) and commit these three files on `feature/tech-design-sync` together with the tech-lead's `docs/tech-design.md` edit. N-1b, N-2a and N-7 are now ready for the cut. N-3 and N-6 are done ahead of 4.6a and 2.4a.

## Pass 2 — code-auditor F-1, F-2, F-3, F-5, F-6, F-7 and the research-analyst's row J (2026-10-24)
Branch: feature/tech-design-sync (committed by the producer; uncommitted edits on top of `5308d2e`, as dispatched)
Ran on: claude-opus-5-5 at effort medium
Loops: 1
Lessons applied: none

### Summary
I closed the six release-manager findings of `code-auditor-tech-design-sync.md` and transcribed the research-analyst's row J changes (`PegSolitarie-lic/.planning/reports/licences-screen/research-analyst-mpl.md`, "Changes to transcribe"). I edited only `docs/release-checklist.md`, `THIRD_PARTY.md` and this report. I made no commit and ran no Gradle.

### Outputs
- `docs/release-checklist.md`:
  - **1.1a, F-2:** the founder's words are now quoted exactly: "release/3.0.0 waits for it only if it is the last item left". The reversed paraphrase "even when" is gone.
  - **1.1a (3), F-1 and F-5:** reworded to fit §14.1's pairwise path and brief §13.3.
    - The code-auditor and qa-engineer each name the code head `R` they reviewed.
    - After `R`, only merges of `develop` into the branch touch `app/` (`git log --no-merges R..$M^2 -- app/` is empty).
    - The tech-lead sign-off names a commit whose `app/` tree equals the merged head and records its re-read of each merge-in's conflict hunks.
    - The item now says "names as reviewed" instead of "`Branch:` range".
  - **1.4, F-7:** the condition now reads "LN-7 green: items 1–3, 5 and 6", where item 6 is the branch's own check that it adds no coordinate.
  - **4.6a (c), F-6:**
    - The citation now reads: §2 for Apache-2.0 and the Jakarta NOTICE, §5b rows F–J for the rest.
    - It also adds `MPL-2.0.txt` (`3f3d9e00`). LS-11 stands per the MPL verdict, so this text ships.
  - **3.5:** adds the okhttp re-check. Row J's address holds only for okhttp 4.11.0. If the listing prints another okhttp, the research-analyst re-derives the address by git-blob match, then the release-manager updates row J.
- `THIRD_PARTY.md`:
  - **`:381`, F-3:** the branch is now described as "docs and reports only … and no code", backed by a `git diff --name-only` test, instead of "only its spec commit".
  - **`:93`:** `MPL-2.0.txt` is added to the screen's texts.
  - **Row J, Source URL:** now leads with the exact version shipped, `github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat` (PSL 2023-02-15, sha256 `e8b273972eb5a70e…`, okhttp 4.11.0, re-derived at 3.5). The live list and the NOTICE address are kept.
  - **Row J, How credited:** "Optionally the MPL-2.0 text" is replaced by the bundled `assets/licenses/MPL-2.0.txt` with its source and full hash, plus the CLEAN verdict and "names the exact version shipped". The NOTICE now carries its full hash.

### Checks
- `git diff --name-only develop...feature/licences-screen | grep -vcE '^(docs/|\.planning/)'` — 0. `git log --oneline develop..feature/licences-screen` — 6 commits (`0bf578d`, `e23a4b5`, `ddac9ab`, `ecddc40`, `4069b53`, `076fe3d`); this grounds F-3's new wording.
- `sed -n 1396,1404p docs/tech-design.md` — read the pairwise rule, the ground for F-1. `sed -n 1286,1300p` — read LN-7 items 5 and 6, the ground for F-7.
- Pipe count per edited row, with escaped `\|` excluded. All match their headers:
  - checklist rows `:32`, `:35`, `:73`, `:90` — 5 pipes each, header `:29` has 5;
  - THIRD_PARTY row J `:242` — 7 pipes, header `:235` has 7.
- Greps that must hit:
  - `grep -n -o 'waits for it only if it is the last item left' docs/release-checklist.md` — `:32`
  - `grep -n -o "name the branch's code head they reviewed, \`R\`"` — `:32`
  - `grep -n -o 'items 1–3, 5 and 6'` — `:35`
  - `grep -n -o '§5b rows F–J for the rest'` — `:90`
  - `grep -n -o 'okhttp re-check (row J)'` — `:73`
  - `grep -n -o a312abe0e11421aff8fe909a5e02f36b7c5d28de THIRD_PARTY.md docs/release-checklist.md` — `THIRD_PARTY.md:242`, `docs/release-checklist.md:73`
  - `grep -n -o 8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a THIRD_PARTY.md` — `:242`
- Greps that must be empty:
  - `grep -c 'even when it is the last item left' docs/release-checklist.md` — 0
  - `grep -c 'end their \`Branch:\` range'` — 0
  - `grep -c 'holds only its spec commit' THIRD_PARTY.md` — 0
  - `grep -c 'Optionally the MPL-2.0 text' THIRD_PARTY.md` — 0
- `git diff --check` — clean. `git diff --stat` — `THIRD_PARTY.md` +3/−3, `docs/release-checklist.md` +4/−4.

### Open issues / risks
- **`MPL-2.0.txt` was added beyond the coordinator's list.** I added it to 4.6a (c) and `:93` because the research verdict makes LS-11's bundled text a shipped asset. If the ux-designer drops LS-11, both mentions must go at 2.4a.
- **4.6a (c) uses the asset's filename before the branch fixes it.** `MPL-2.0.txt` is the name in the verdict's transcription, and the branch has not fixed it in `BundledLicenseText` yet. 4.6a (c) already defers to that class's paths.
- **F-4 and F-8 belong to the tech-lead.** I did not touch them.

### Handoff
Next: producer — have the code-auditor re-check pass 2, then commit it on `feature/tech-design-sync`.

## Pass 3 — the tech-lead sign-off's 1.1a items and row J's citation (2026-10-24)
Branch: feature/tech-design-sync (committed by the producer; uncommitted edits on top of `62c815c`, as dispatched)
Ran on: claude-opus-5-5 at effort medium
Loops: 2
Lessons applied: none

### Summary
I made the two 1.1a edits in the exact text that `tech-lead-signoff-tech-design-sync.md` gives ("What the release-manager must change in 1.1a", items 1 and 2), and added the row J citation note the sign-off's INFO asks for. I made no commit and ran no Gradle.

### Outputs
- `docs/release-checklist.md:32` (1.1a):
  - **Waiver vehicle (item cell):** "put to the founder through `Q-PUBLISH-3.0.0`" is replaced, word for word as the sign-off gives it, by row TB's one pre-cut `Q-<NNN>` ("wait, or ship option 2"). The producer queues it only when this item is the last unticked pre-cut item. While it is open the cut waits; on "ship", this item is deferred with a named owner under 1.1 and the no-claim clause binds.
  - **S-1 (evidence condition 3):** the command now reads `git log --no-merges --first-parent --format=%h R..$M^2 -- app/`.
- `THIRD_PARTY.md:242` (row J, How credited): the `research-analyst-mpl.md` citation now says "on `feature/licences-screen` until it merges; this note goes at checklist 2.4a".

### Checks
- `grep -c 'through \`Q-PUBLISH-3.0.0\`' docs/release-checklist.md` — 0.
- `grep -n -o "row TB's one pre-cut \`Q-<NNN>\`" docs/release-checklist.md` — `:32`.
- `grep -n -o -F 'git log --no-merges --first-parent --format=%h R..$M^2 -- app/' docs/release-checklist.md` — `:32`.
- `grep -n -o 'on \`feature/licences-screen\` until it merges; this note goes at checklist 2.4a' THIRD_PARTY.md` — `:242`.
- S-1 reproduced: `git log --no-merges --format=%h ecddc40..feature/licences-screen -- docs/ | wc -l` gives 7; the same command with `--first-parent` gives 1. This matches the sign-off.
- Pipe count, with escaped `\|` excluded: checklist `:32` has 5, matching header `:29`; THIRD_PARTY `:242` has 7, matching header `:235`.
- `git diff --check` — clean. `git diff --stat` — `docs/release-checklist.md` +1/−1, `THIRD_PARTY.md` +1/−1.

### Open issues / risks
- **`--first-parent` assumes merge-ins are made in the usual direction.** It follows the branch's own line only when each merge-in runs on the branch with `git merge develop`, so the branch is the first parent. §14.1 prescribes exactly that ("merges `develop` in (never a rebase)").
- **At 2.4a,** remove row J's "until it merges" note once `feature/licences-screen` has merged.

### Handoff
Next: producer — commit pass 3 and merge `feature/tech-design-sync` into `develop` (`--no-ff`).
