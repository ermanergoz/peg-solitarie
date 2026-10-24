# ux-designer report — licences-screen (spec update, second edition)
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; plugin rules v1.0.0 read in full because the worktree snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; product `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none named in the dispatch
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none — two read-only haiku helpers, one after the other, for `git show` of files on sibling branches that my tools cannot read (rules/dispatch.md: a helper only where my own tools cannot do the work); neither named a studio role, and neither wrote inside a repository
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
`docs/ux/licences-screen.md` is now a second edition that meets tech design §13.1 (LN-1 to LN-8), THIRD_PARTY.md as merged at c5f2c54, the still-open D10/D11 fold, the branch split (A9, E6, E7), and the house accessibility rules with Q-023's direction. Four screens (Licenses, License text, Google notices, Google notice text), six new decisions LS-08 to LS-13, eleven Open-source software entries mapped one-to-one to THIRD_PARTY.md (§3.2.1), eight curated texts at the research-analyst's hashes plus the generated `google/` directory, a TalkBack contract with a seven-point pass line (§5.1), eighteen acceptance criteria (seven amended, seven new) and a ten-item device list (§8.2). Nothing earlier is deleted; §10 indexes every supersession. Three UX decisions were taken where LN-5 left the call to this role: the Apache-2.0 libraries grouped by publisher into four entries (LS-10), the MPL-2.0 text bundled (LS-11), and the notices list shown as Google supplies it with a "Found in" line (LS-12).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/docs/ux/licences-screen.md` — the amended spec (second edition): frontmatter and edition note; §0 scope; §1 LS-02/03/04/05/07 pointers and LS-08–LS-13; §2 note; §3.1 note; §3.2 table rebuilt with the replaced rows quoted, §3.2.1 mapping; §4 D10 fold and multi-text entries; §4A and §4B new screens; §5.1 TalkBack contract; §6 added strings; §7 added assets; §8.1 amended and new criteria; §8.2 QA device list; §9 item statuses and items 6–12; §10 record.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/ux-designer.md` — this report.

## Checks
- `Read` in full: `docs/ux/licences-screen.md` (first edition), `THIRD_PARTY.md` (c5f2c54), `docs/tech-design.md` §3.1–§3.6 and §9.8–§17 (lines 1–457, 900–1486; §13.1 LN-1 to LN-8, §14.1 row LN, §16 #20, §17), `candy-backdrop/tech-lead-architecture.md` (R4, R5, D1–D11, Open issues, Handoff), `candy-backdrop/tech-lead-direction-b.md` (A0–A11, E1–E12, open issue 7, the brief), `phase-14/research-analyst-release-docs-licences.md`, `phase-14/research-analyst-font-licences.md`, `docs/audio/CREDITS.md` (develop), `docs/ux-spec.md` §4.5 and §11, `docs/ui/DESIGN-SYSTEM.md`, `app/src/main/res/values/dimens.xml`, `.planning/questions/FOUNDER.md` (this branch) — pass (every input the dispatch named was read; §13.1's facts, the D10/D11 rows and A9/E6/E7 are quoted from source in the spec).
- Helper 1 (haiku, read-only): `git -C <main repo> show feature/ux-spec-folds:.planning/questions/FOUNDER.md`, `git show feature/audio-review:docs/audio/CREDITS.md`, `git show feature/audio-review:THIRD_PARTY.md`, `git diff develop...feature/audio-review -- THIRD_PARTY.md`, `git worktree list`, heading dumps of `docs/tech-design.md` and `docs/ux-spec.md`, `ls` of `res/font` and `res/raw`, `dimens.xml`, `libs.versions.toml` into the scratchpad — pass, with one finding: `feature/audio-review` has **no** `THIRD_PARTY.md` (`fatal: path 'THIRD_PARTY.md' does not exist`), so the audio branch adds no row; the twelve WAV rows are already on this branch via `develop`. Q-023 read in full (answer: option 1, 2026-10-21).
- Helper 2 (haiku, read-only): `git show feature/ux-spec-folds:docs/ux-spec.md` and the D-TB/D-TBa block (lines 1256–1489), `git show feature/audio-review:docs/audio/CREDITS.md` lines 63–110 (the `## Files` table), `git show feature/ux-spec-folds:.planning/reports/ux-spec-folds/game-designer-talkback-board.md` — pass (D-TB's shape used for §5.1; the Files table confirms seven Kenney, two from sound 506546, one from sound 531512, two in-house).
- Cross-check, THIRD_PARTY.md → §3.2.1: every §2 asset row, every §4 coordinate, every §5b family row A–J and every line of the Apache table is mapped to exactly one entry or to "not listed (LS-04)" — pass (33 lines; no shipped, licence- or credit-bearing row is unmapped; the two in-house sounds, the idea rows, own work and §8 are the only unmapped ones, each by LS-04).
- Cross-check, hashes and sizes: `992ed62d…a512214df540` 1,074 B (Jakarta), `6e5e117324afd944` 1,732 B, `368cca1106be99d3` 1,536 B, `e90eaa08dea089df` 1,126 B, `8a9c58fbded5ea34` 218 B, `3f3d9e0024b1921b` 16,726 B, `cfc7749b…3d30` 11,358 B — pass (each copied from the research-analyst's two reports and THIRD_PARTY.md; prefixes only where the source gave prefixes, and the spec says so).
- Counts recomputed after the re-read: fourteen tappable stops and four inert on the Licenses screen; seven entries open `Apache-2.0.txt`; eleven Open-source software entries; eight curated texts — pass (two wrong counts found and corrected in the same pass).
- `./gradlew check` — not run (no Bash on this role; a docs-only change; no code or resource was touched).

## Open issues / risks
1. **Three UX decisions, recorded in the spec, that the tech-lead's brief and the release-manager's file should follow:** LS-10 (four publisher-grouped Apache entries, with every library named in a parts line), LS-11 (the MPL-2.0 text is bundled, so one more asset at `3f3d9e0024b1921b`), LS-12 (the notices list shows Google's names and artifact ids verbatim). None is an art-direction choice between equals; no founder question is drafted.
2. **The research-analyst owes one non-blocking line** on the MPL-2.0 text's own redistribution terms (LS-11; §9 item 6). If it says no, the Public Suffix List entry opens the NOTICE alone and LS-11 is superseded; nothing else moves.
3. **Hash prefixes.** The research-analyst recorded 16 hex digits for five of the six new texts. The spec makes the developer fetch each text from the URL THIRD_PARTY.md names, confirm the prefix, pin the full SHA-256 in the LS-AC12 test and report it; the release-manager copies it into THIRD_PARTY.md (§7, §9 item 8). The full hashes are therefore a build output, not a spec input.
4. **The words rule and the generated notices.** The BD-01 test scans `src/main/assets/**`; the generated `google/` directory is under `build/` and outside it. The spec records that Google's texts are reproduced verbatim and are not edited to the rule, and asks the developer to grep them for information (LS-AC17). If the code-auditor reads the rule as reaching third-party verbatim texts, that is a question for the producer, not a spec change.
5. **Two-text entries need a type shape** (`LicenseEntry.text` → a list or a pair). The spec says only what is shown; §9 item 7 routes the shape to the tech-lead's build brief, together with the `google/` exclusion in LS-AC03's asset test and the row model for the notices list.
6. **The audio branch's status still decides the Sounds counts.** `develop` ships `.ogg` with nine Kenney cues; the spec and THIRD_PARTY.md describe `feature/audio-review`'s WAV set (seven). The re-cut rule stands (§3.2); it is one plural argument per entry.
7. **E7 ownership of LS-AC08.** Whichever of `feature/licences-screen` and `feature/candy-backdrop` lands second adds the four routes to `BackdropDensityTest` and `TransparentContainersTest`; the first to land asserts nothing about the backdrop. The producer's merge order (tech design §14.1, 6a before 7) makes that the candy branch today.
8. **A fresh checker follows** (per the dispatch): this spec was written on fable and must be checked by a fresh fable agent that did not write it (rules/studio.md, Model and effort).
9. **Commit.** I have no Bash; nothing is committed. The producer commits `docs/ux/licences-screen.md` and this report on `feature/licences-screen` after reading them; the commit carries rules/gitflow.md's single trailer.

## Handoff
Next: producer —
1. Read and commit the two outputs on `feature/licences-screen` (docs commit, then the report in its own commit).
2. Dispatch the fresh checker on the spec (fable, not this agent); then the tech-lead for the build brief, with §9 item 7's three shape questions, the spec's §3.2.1 mapping, §7's asset names and LN-7 folded into §8.1.
3. Research-analyst, non-blocking: the one-line MPL-2.0 confirmation (§9 item 6).
4. Release-manager: the "How credited" cells and the Sounds re-cut rule (§9 item 8); the full hashes once the developer reports them.
5. android-game-dev, when briefed: build to this spec and the tech-lead's brief; the tests of §8.1 (LS-AC03, LS-AC05, LS-AC06, LS-AC10, LS-AC12–LS-AC16 are the developer's); record the LS-AC17 grep and the LS-AC18 size delta in its report.
6. code-auditor and qa-engineer, at review: §8.1 by id, §8.2 on the device with the release build's generated assets, §5.1's pass line per screen.

## Loop 1
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer)
Loops: 1 (the fresh check `ux-designer-check.md`, committed `ecddc40`: FAIL on the acceptance side, PASS on the licence content; F-01 to F-18)
Read in full: `.planning/reports/licences-screen/ux-designer-check.md`; `.planning/reports/licences-screen/research-analyst-mpl.md`; `.planning/questions/FOUNDER.md` Q-023 and `docs/ux-spec.md` §11.1's D-TB heading on this branch (`develop` 4069b53 merged in), to cite rather than restate
Lessons applied: none

### What changed in `docs/ux/licences-screen.md`
Every finding is folded at the line the check named, with a dated loop-1 note that keeps the superseded wording, and indexed in §10 rows 11–24. No decision LS-01 to LS-13 is reopened; the THIRD_PARTY.md ↔ screen mapping is unchanged.
- **F-01 (High):** LS-AC15 and LS-AC10 scroll the §4B **list** node (`performScrollToNode(hasText("End of text"))` or `performScrollToIndex`), then assert; the second edition's `onNodeWithText(...).performScrollTo()` is superseded. The list's test tag is the brief's (§9 item 7(k)).
- **F-02 (High):** every entry card carries `Modifier.semantics(mergeDescendants = true) {}` with no role and no action; a tappable entry adds `clickable`; "through `clickable`, as every `SettingsLinkRow` is" is superseded (§3.1, §5.1, LS-06 pointer). LS-AC06 asserts one node per inert entry with no `OnClick` and no `Role`; pass lines 1 and 3 fixed. Whether the fix lives in a new entry composable or in `SettingsLinkRow`'s `onClick = null` branch is owed to the brief (7(f)).
- **F-03:** nine curated texts; LS-AC11 names all nine; LS-AC05, LS-AC17, §6, §7 corrected.
- **F-04:** UX intent stated (position by ear, rows counted without the intro); the mechanism (`collectionInfo`/`collectionItemInfo` or the BOM's own) owed to the brief (7(g)); LS-AC13's oracle rewritten so it holds without it (the last row's name, sources and ordinal against the AAB's last index line; "N of N" only once delivered).
- **F-05:** pass line 6 and LS-AC16: spoken once when it appears, never repeated spontaneously, read again only when focus lands on it.
- **F-06:** LS-AC12 (b) requires all ten AARs of §13.1's table; the debug-for-release statement is written with its reason (the two runtime classpaths differ only by `ui-tooling` and `ui-test-manifest`) and owed to the brief as a check instead of an assumption (7(h)); (c) gets its procedure (`unzip -l` of each cached AAR from checklist 3.5's listing, compared by artifactId).
- **F-07:** Q-023 and D-TB/D-TBa are cited from this branch; "the studio ships nothing that TalkBack cannot use" is marked as this role's reading of a board-specific answer, with the question's premise as the reason.
- **F-08:** a UX decision — same-name notice rows carry "Notice n of m" (`google_notices_ordinal`), built once in the row model, shown again on the text screen; LS-AC13's fixture uses identical sources.
- **F-09:** intent stated — all four Settings section headers are headings through `SettingsSectionHeader`; owed to the brief (7(i)); LS-AC01 amended; §8.2 item 11.
- **F-10 to F-18:** the `androidx.*` count, "then by stem", the no-backdrop contrast pairs computed from DESIGN-SYSTEM.md's hexes (light ≈ 11.7/11.1/5.6/5.3/5.1:1, dark ≈ 14.8/11.9/7.0/5.6/6.5:1), the "Found in" heading withdrawn, §4B aligned with the gate, pass line 1 scoped on the text screens, Jakarta's credit sourced from its NOTICE, the CREDITS.md branch note, the LS-11 fall-back completed (moot).
- **The MPL verdict:** LS-11 stands; §9 item 6 closed; §7's MPL row carries the full SHA-256 `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04` and the NOTICE row `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a`; the Public Suffix List entry now names the exact version shipped — `licenses_psl_address` is `github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat`, the live-list address stays in the NOTICE the entry opens — and §9 item 13 records the re-derivation if U-6 shows another okhttp.

### Checks
- `Read .planning/reports/licences-screen/ux-designer-check.md` (101 lines) — pass (all eighteen findings read; each has a loop-1 note at the location it cited).
- `Read .planning/reports/licences-screen/research-analyst-mpl.md` (185 lines) — pass (verdicts 1–3 and "Changes to transcribe" folded; the two full hashes copied character for character).
- `Read .planning/questions/FOUNDER.md` lines 1–16 and `docs/ux-spec.md` lines 1256–1261 on this branch — pass (Q-023 and D-TB are present after the `develop` merge, so the spec cites them).
- Contrast arithmetic for F-12: WCAG relative luminance from `docs/ui/DESIGN-SYSTEM.md`'s hexes, computed by this role — pass (every pair ≥ 4.5:1 for text and ≥ 3:1 for the spinner glyph in both themes; QA measures on the device).
- Counts after the edits: nine curated texts; fourteen tappable and four inert stops; seven entries share `Apache-2.0.txt`; ten AARs; §10 rows 1–24 each point at a live section — pass.
- `./gradlew check` — not run (no Bash on this role; a docs-only change).

### Open issues / risks (loop 1)
1. Six items are owed to the tech-lead's brief, being written in parallel in this worktree (§9 item 7(f)–(k)); each carries this role's intent, so the brief can rule the mechanism without a further UX round.
2. The §13.1 ten-AAR list in LS-AC12 (b) is written at today's versions; the release listing (U-6) may change it, and the test's list then changes with a verdict.
3. The Public Suffix List address is tied to okhttp 4.11.0 (§9 item 13).
4. A fresh fable checker re-checks this state of the document (§9 item 14); the developer dispatch follows its PASS.

### Handoff (loop 1)
Next: producer — read and commit the amended spec and this report (not committed by me); re-dispatch a fresh checker on the spec; hand the tech-lead §9 item 7(f)–(k) for the brief; release-manager: row J's two cells and the NOTICE's full hash (§9 item 8), the U-6 re-derivation note (item 13).
