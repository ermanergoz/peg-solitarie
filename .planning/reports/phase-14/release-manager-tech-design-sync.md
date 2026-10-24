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
