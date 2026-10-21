# tech-lead report — release-docs-blockers
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer). I worked on head `428b6eb`, with `develop` merged in, in `PegSolitarie-r5`, and made no commit.
Skills loaded: ergoz-soft:standards (role tech-lead). The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch. I also read the product's `.claude/rules/lessons.md`. Further skills: android-architecture, android-gradle-logic.
Ran in parallel with: Gradle builds in three other worktrees (the dispatch gave no slugs)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the tech-lead is pinned to fable at xhigh (`agents/tech-lead.md`). This ran on Opus under the founder's Q-019 (answered 2026-10-17): Opus for every Fable-pinned role until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none. The snapshot holds 0 active lessons, and the product has no `.planning/recall/RECALL.md`.

## Summary
- **§14.1** now marks what release-checklist item 1.1 reads. It adds three branch-cut blockers: R1 (`bugfix/hint-undecided`), LN (the Licenses screens with every notice) and PU (`scripts/play-upload.sh`, a Gate 2 item, not a branch-cut one). The merge order is reconciled: R1 is first among the code branches (brief §10), followed by check-gates, audio and F-1. Licences, colour-vision and the upload script are lettered steps, and candy-backdrop is last.
- **§16 #20 is ruled (§13.1, LN-1 to LN-8).** The Google SDK notices are generated at build time from each variant's runtime classpath, with no new dependency, and shown behind the Google entry. Rows F–J and the Jakarta NOTICE ship verbatim, and checker-qual is credited. `oss-licenses`, a committed copy and a link-out are rejected.
- **§16 #21 and the §15 row record the ruling** (MAJOR, R1). §7.2's "today" text stays until R1 merges, under a forward pointer. `08-VERIFICATION.md` has its dated correction note.
- **The book counts are reconciled in §7.2.** The extra entries are the committed golden line's states past depth K. In play they are book hits, so a hint-follower from the start never reaches the solver. **AC-18's first scenario therefore cannot pass as written** (new §16 #22).
- I also corrected a stale point in this file: the licences screens have their own branch since direction-B A9. §3.6, §13, §14.1, §16 #4 and §17 now say so.

## Outputs
- `docs/tech-design.md`:
  - header provenance;
  - §3.6: the branch correction and the LazyColumn exception;
  - §7.2: the row-1 correction, the "Ruled" box, two in-place corrections and "The book counts, reconciled";
  - §10: Hint-latency pointer;
  - §13: UMP settled, `hsdp` row, notice families row, the policy sentence, new **§13.1**;
  - §14.1: the item 1.1 note, markers on R-2 and F-1, new rows R1, LN and PU, the candy correction, merge order steps 3a, 6a, 6b and 6c and the second-to-merge duties;
  - §15: the hint row updated, two new rows;
  - §16: #4 corrected, #20 and #21 ruled, new #22;
  - §17: eight supersession rows.
- `.planning/phases/08-assist-generator-quality-debt/08-VERIFICATION.md`: the section "Correction — 2026-10-21: override 1's 'not an untruthfulness defect' is overturned (GD-HT-1)", inserted after the header and before "Re-verification Summary". The frontmatter is untouched, so gsd's parser is unaffected.
- `.planning/reports/phase-14/tech-lead-release-docs-blockers.md`: this report.

### The notices ruling in brief (§13.1)
| | Decision |
|---|---|
| Route (LN-1) | Generate the notices from each variant's runtime-classpath AARs at build time, bundle them as assets, and show them in-app. Rejected: `oss-licenses` (its activity has its own look; the plugin is unverified on AGP 9.1's new DSL and lists about 190 POM URLs that carry no notice text); a committed copy (2 MB, and it goes stale silently); a link to Google's page (LS-05). |
| Build (LN-2, LN-3) | A new precompiled plugin, `google-notices.gradle.kts`, applied by `:app` only. It registers `generate<Variant>GoogleNotices` per variant and calls the pure `GoogleNoticeMerge`, tested in build-logic. It writes `assets/licenses/google/<sha16>.txt` (byte-identical slices, deduplicated) and `index.tsv` (`key`, `name`, `stem`, `sources`). Every distinct text is kept. No new dependency. |
| App (LN-4) | `GoogleNoticeSource` and its parser; routes `GoogleNotices` and `GoogleNoticeText(key)`, where a key never becomes a path; a list ViewModel; a text ViewModel that renders one paragraph per `LazyColumn` item. |
| Screen (LN-5) | Entries for Protocol Buffers, Chromium (Cronet), the Public Suffix List, Checker Framework qualifiers (credited, a tech-lead decision) and Jakarta Inject, with the Apache families. The Google libraries section keeps its inert SDK-licence entry, which now names UMP, `hsdp` and the Play services libraries, and gains a tappable entry that opens the generated notices list. |
| Who (LN-6) | ux-designer amends `licences-screen.md`; android-game-dev builds on `feature/licences-screen`; research-analyst gives a verdict only if a dependency appears (none expected); release-manager writes the rows and the AAB listing; code-auditor and qa-engineer review; tech-lead signs off. |
| Acceptance (LN-7) | Merge tests (red first), `:app` asset and hash tests, Compose UI tests, the AAB listing equal to the AARs on `releaseRuntimeClasspath` that carry notices, device checks (the 785 KB Cronet notice with no ANR), no new catalog coordinate, and the APK delta recorded. |

## Checks
- `cat .claude/rules/VERSION`; `grep version ergoz-soft/.claude-plugin/plugin.json` — 0.6.2 against 1.0.0, so I read the plugin copies.
- `git status --short` (r5) — pass: only `docs/tech-design.md` and `08-VERIFICATION.md` are modified, plus this report.
- **Item 1:**
  - `grep -n -E '^\| (R1|LN|PU) \|' docs/tech-design.md` — pass, lines 1312, 1315 and 1316.
  - `grep -c 'Blocks the branch cut'` — 4: R-2, F-1, R1 and LN. The note above the table wraps the phrase across a line.
  - `grep -n 'Blocks Gate 2, not the branch cut'` — pass, line 1316.
  - `grep -n -E '^(3a|6a|6b|6c)\. '` — pass, lines 1340, 1348, 1352 and 1355.
  - `grep -n 'How release-checklist item 1.1 reads this table'` — pass, line 1299.
- **Item 2:**
  - `grep -n 'Ruled 2026-10-21'` — pass: the §7.2 box (567), §15 (1408), §16 #20 (1440) and #21 (1441).
  - `grep -n 'Until R1 merges, the text below remains the'` — pass, line 574.
- **Item 3:**
  - `grep -n -E 'The book counts, reconciled|^\| Wiegleb-45 \| 3 \| 43 \| 136 \| 101 \| 97 \| 4 \| 39 \||Diamond-41 lacks 440 of its 785'` — pass, lines 608, 619 and 631.
  - `grep -n '^| 22 |'` — pass, line 1442.
- **Item 4:** `grep -n -E '^### 13\.1|^\*\*LN-[1-8] '` — pass: §13.1 at 1137, LN-1 to LN-8 at 1164–1289.
- **Item 5:** `grep -n 'Correction — 2026-10-21' .planning/phases/08-*/08-VERIFICATION.md` — pass, line 43.
- `python3 -I tables.py docs/tech-design.md` (scratchpad script; it compares each table row's cell count with its header's) — pass: 0 mismatched rows, as at HEAD.
- **Book reconciliation:**
  - `python3 -I book_line.py <book> <golden line> <start pegs> <K>` over all five books — pass. Past depth K there is exactly one entry per depth, and each equals the golden line's move at that depth: 26, 30, 32, 33 and 39 entries. Each depth-0 entry equals the line's first move. The per-depth frontier entries sum to 373, 609, 461, 345 and 97.
  - The line move counts: `grep -o -- '->(…)->' golden/solutions/*.txt | wc -l` — 31, 35, 37, 38 and 43.
  - `sed -n 645,665p OpeningBookAuthoring.kt` — the gap note's "of M" is `undecided.size + entries.size`.
  - `sed -n 184,192p OpeningBook.kt` — `lookup` has no depth check.
  - `grep` in `Boards.kt` — the in-game Wiegleb-45 start is `withoutPeg(center)`, `(4,4)`, the fixture's `startVacancy`.
  - `sed -n 1,120p DepthSweepTest.kt` — the measured table: 373, 609, 461, 785 and 101 examined.
- **Notice measurements:**
  - `unzip -l` over the 10 cached Google AARs (`~/.gradle/caches/modules-2/files-2.1`) — each carries `third_party_licenses.json` and `.txt`.
  - `python3 -I tpl_stats.py` and `tpl_merge.py` — 100 names, 108 pairs and 42 texts, 2,053,748 B, 272,647 B deflated (zlib -9). Kotlin has 4 texts, J2ObjC 5 and Cronet 2.
  - `tpl_paras.py` — the largest text, Cronet's in `play-services-cronet`, is 785,183 B in 2,497 paragraphs.
  - `tpl_words.py` — 0 hits for the protected-word list (jelly, soda, sugar, sweet, candy, crush, king) in the notice texts.
- `git show feature/candy-backdrop:docs/ux/licences-screen.md` (LS-03, LS-05, §3.2, §4, LS-AC) and `git show feature/candy-backdrop:.planning/reports/candy-backdrop/tech-lead-direction-b.md` (A9, A11) — read; they are the source of the branch correction.
- Read in full: `hint-undecided/tech-lead-brief.md` (the hint worktree), `game-designer-hint-timeout.md`, `research-analyst-release-docs-licences.md` and `release-manager-release-docs-fixes.md`. Read in part: the open issues of `research-analyst-font-licences.md`, the `docs/release-checklist.md` items 1.x and the §4 note, and `14-RELEASE-PLAN.md` §4a.
- **Not run, as dispatched:** Gradle and the Check (`./gradlew check`). This change is docs only and touches no Check input. No commit was made.

## Open issues / risks
- **§16 #22: AC-18's first scenario contradicts the code.**
  - A player who follows Hint from the untouched Wiegleb-45 start stays on the committed golden line, and every state of that line is a book entry. The fifth hint is therefore an instant Found, not Undecided.
  - The hint brief's §7 Device line ("including the two reached without following Hint") on `bugfix/hint-undecided` inherits the same premise. I did not edit it: it is outside this dispatch's files.
  - The game-designer owns AC-18. It must be corrected before QA runs it.
  - The same fact narrows F-1's "follow the hints" example: on every Classic board a hint-follower from the start never reaches the solver. The defect stands for every position off the line.
- **Q-019 deviation.** This ruling and its design text were written on Opus, so no fresh-fable check of a fable-written plan exists. The code-auditor's re-review on Opus is the check, as the dispatch says.
- **Release-checklist item 1.1's evidence column** points at "the blocker table in the latest `.planning/reports/phase-*/tech-lead-*.md`". It should point at `docs/tech-design.md` §14.1, which now carries the markers. That is the release-manager's file.
- **Checklist 1.7 and `14-RELEASE-PLAN.md` item 2** say the upload script itself refuses to run without the approval file. Plugin rules/stacks.md 1.0.0, scaffold contract 9, says the script "checks no approval itself (hooks/publish-gate.sh does)". §14.1 PU follows the rule, and §17 records the supersession. The release-manager aligns both texts.
- **`08-VERIFICATION.md` override 1 also says "440/818".** The depth-≤4 frontier is 785 (§7.2). I did not add that to the file, because the dispatch allowed edits there for item 5 only. If a second note is wanted, it needs its own dispatch. The game-designer's report quotes the same figure; it is a record, and §7.2 governs.
- **The measured notice figures** come from the main checkout's Gradle cache, at the versions the research-analyst resolved. The release listing (checklist 3.5) is the authority, and LN-7 item 4 compares the index against it.
- **§16 #20 stays open** until `feature/licences-screen` merges with LN-7 green and the release-manager's rows and AAB listing are done. LN is a branch-cut blocker, so a slip there holds the release. §15 records the risk.
- **The colour-blind palette's tech-lead brief is still owed** (direction-B A9). It is recorded in merge order step 6b and was not part of this dispatch.

## Handoff
Next: producer. Route the following.
- **code-auditor:** re-review this branch: `docs/tech-design.md` (§3.6, §7.2, §10, §13, §13.1, §14.1, §15, §16, §17) and the `08-VERIFICATION.md` note, against the ruling, the brief, the research report and F-6.
- **game-designer:** §16 #22. Amend AC-18 (and, if wanted, the reach table's "440 of the 818"), then return it to the hint branch's developer and QA.
- **android-game-dev on `bugfix/hint-undecided`:** supply AC-18's sequences off the committed golden line, each with the book miss asserted in its domain test. Merge `develop` in once release-docs has merged (brief §0).
- **ux-designer:** amend `docs/ux/licences-screen.md` for §13.1 LN-5, on `feature/licences-screen` once it is cut.
- **android-game-dev:** `feature/licences-screen` (§14.1 LN; §13.1 LN-2 to LN-5, test-first), and `feature/play-upload` (§14.1 PU).
- **release-manager:**
  - point checklist item 1.1 at §14.1;
  - align item 1.7 and `14-RELEASE-PLAN.md` item 2 with §14.1 PU;
  - transcribe `THIRD_PARTY.md` rows F–J, close U-1, add `hsdp` and fix the §4 wording;
  - run LN-7 item 4 at the cut.
- **research-analyst:** nothing new unless the licences branch proposes a dependency.
- **tech-lead:** sign-offs on R1, LN and PU after their reviews. When R1 merges, rewrite §7.2 to the three outcomes and close §16 #21.
