# release-manager report — release-docs-pass2
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer; work on top of 7929d1b, uncommitted as dispatched)
Skills loaded: ergoz-soft:standards (role release-manager). The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and ip-policy. I also read the product's `.claude/rules/lessons.md`. I did not invoke `gitflow` or `gsd-complete-milestone`: this pass makes no branch, merge, tag or commit, and closes no milestone.
Ran in parallel with: none named
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- F-1: the four store texts and `CHANGELOG.md` now use only the "Today" column of the game-designer's §4. No text claims that "no winning move" is proven. The "Hints that tell the truth on every board" line and its Turkish twin are gone. A new optional checklist item, 2.5a, lets the copy be raised once `bugfix/hint-undecided` merges.
- F-2: rows A to J and the Apache table are in `THIRD_PARTY.md`. The Google-SDK wording now matches the evidence ("solely to develop applications"), the DataStore protobuf BSD-3 exception is in the androidx row, U-1 and I-1 are closed, and U-6 stays open.
- The changelog coverage line now uses the game-designer's wording: "an opening book that covers the first moves of every Classic board".
- Ad steps A to E are now Step 7a of the operator checklist, so checklist row 5.0 can be ticked from it. `ad-testing-safety.md` §3a says so.
- Release-plan item 2 now puts `scripts/play-upload.sh` on a `feature/*` branch off develop before Gate 2 (tech-lead → android-game-dev), matching checklist 1.7.

## Per-item table

| # | Item | Files | What changed | Status |
|---|---|---|---|---|
| 1 | F-1, store copy and changelog | `docs/store/en-US/full_description.txt:15`, `docs/store/tr-TR/full_description.txt:15`, `docs/store/en-US/changelogs/30000.txt:3`, `docs/store/tr-TR/changelogs/30000.txt:3`, `CHANGELOG.md:51-53, 104-105, 137-140`, `docs/store/README.md` (lengths, a hint-claims note), `docs/release-checklist.md` 2.5a (new) | **Listing:** "Hint shows a real winning move. Undo takes back your last jump." (designer's "Listing, today" line); TR: "İpucu (Hint), kazandıran gerçek bir hamle gösterir. Geri al (Undo), …". **Release note:** "Faster hints on the big boards, answered from a built-in opening book" (designer's "Changelog, today" line); TR: "Büyük tahtalarda, yerleşik bir açılış kitabıyla daha hızlı ipuçları". **CHANGELOG hint bullet:** retitled "Faster hints on the big boards."; "from a difficult early position" became "from the opening positions"; "instead of folding to the 'no winning move' banner" and "'no winning move' now means *proven*" are dropped. **Two more CHANGELOG lines carried the same false claim**, and I fixed them too: the intro's "where it was quietly guessing before — tells you the truth" (the "headline claim" the designer's reason (d) names) and :104-105, "A hint that honestly answers 'there is no winning move from here'", which became "A hint that shows no move". **2.5a** is optional and not a blocker. When `bugfix/hint-undecided` has merged into develop before Gate 2, it raises the copy to the "After R1" column (it quotes the designer's lines), checked with `git merge-base --is-ancestor`. Q-016/Q-022 direction kept: studio drafts, en-US and tr-TR in step, founder approves at Gate 2 | done |
| 2 | F-2, the rows | `THIRD_PARTY.md` §0, §4, §5b, §6, §7 | **§4:** UMP's "unverified" cell is replaced by the SDK licence and the POM URL (row B). Games v2 and Mobile Ads have the POM URL and 2026-10-21 in How verified (rows C and D; I-1 closed). All three licence cells now use the §4 wording from the research report's open issue 3. The "Not Apache-2.0" paragraph no longer says "their own terms allow redistribution". It quotes §3.1 "solely to develop applications" and §3.4, and gives the actual basis for shipping. **§5b:** the androidx row has the `datastore-preferences-external-protobuf` BSD-3 exception. The gms row now matches row E (it drops `-auth-*`, which is not on the graph). The guava/checker row points at the new tables, with checker-qual as MIT. A new table holds rows A and E to J as given, and a further table holds the 12 Apache-2.0 coordinates. The open item is rewritten: licences are settled (192/192, none unknown, none copyleft); U-6 and §16 #20 still block. **§0** and **§7:** their "not listed / unverified" sentences now match. **§6:** U-1 closed with evidence; U-6 kept open, its text updated | done |
| 3 | Changelog coverage line | `CHANGELOG.md:139-140` | "for the first four moves (three on Wiegleb-45)" became "from an opening book that covers the first moves of every Classic board" (designer's replacement, verbatim) | done |
| 4 | Operator checklist A–E | `14-OPERATOR-CHECKLIST.md` (new Step 7a; intro; Step 8 items 4 and 6; the final gate list), `docs/ad-testing-safety.md` §3a (old :169-176), `docs/release-checklist.md` 5.0 | Step 7a sits before Step 8 and lifts A to E word for word, with "Blocks", "Time" and "How to tell it is done" in the file's format. The intro says to do item A in the first sitting (24 h lead). Step 8 item 4 checks that A is at least 24 h old before the install, and item 6 runs B on the first ad. The final gate list gains a Step 7a line. `ad-testing-safety.md` §3a now says the steps are lifted, row 5.0 ticks from 7a A+B, and a change to either copy goes into both. Row 5.0 points at Step 7a | done |
| 5 | Release plan item 2 | `14-RELEASE-PLAN.md` §4a heading and item 2; `docs/release-checklist.md` §4 note | Item 2 now says the script is code, so it is not made on the release branch. It is owed on a `feature/*` branch off develop before Gate 2: tech-lead assigns it to android-game-dev, code-auditor reviews, tech-lead signs off, producer routes and merges. Until then the founder's Console upload (operator Step 8 item 1) is the only route, as checklist 1.7 says. The specification is unchanged. The heading no longer says "added on the release branch". The checklist §4 note's "the release plan puts it on the release branch" is replaced | done |

Other checklist edits that follow from item 2: 1.4 (the §5b open-item wording), 3.5 (a coordinate outside the 192 needs a verdict, and the listing must show no native `cronet-embedded`), 3.5a (the checker-qual mapping grep while row I is not credited; THIRD_PARTY row I cites it), and 3.5b (the four POMs were read 2026-10-21, and versions are re-confirmed at the cut). I also fixed row 5.0's pre-existing fourth cell in a three-column table: its evidence text moved into the Item cell.

## Store text character counts (Unicode characters, without the final newline, `python3 len()`; same method as `docs/store/README.md`)

| File | Limit | Before | After |
|---|---|---|---|
| `docs/store/en-US/full_description.txt` | 4,000 | 2,222 | 2,145 |
| `docs/store/en-US/changelogs/30000.txt` | 500 | 427 | 456 |
| `docs/store/tr-TR/full_description.txt` | 4,000 | 2,521 | 2,447 |
| `docs/store/tr-TR/changelogs/30000.txt` | 500 | 447 | 477 |
| `docs/store/en-US/title.txt`, `short_description.txt` (untouched) | 30, 80 | 13, 76 | 13, 76 |
| `docs/store/tr-TR/title.txt`, `short_description.txt` (untouched) | 30, 80 | 13, 71 | 13, 71 |

## Outputs
- `THIRD_PARTY.md`: rows A to J, the Apache table, the §4 wording, the androidx exception, U-1 closed, U-6 open.
- `CHANGELOG.md`: the intro, :104-105 and the hint bullet, all in the "Today" column.
- `docs/store/en-US/full_description.txt`, `docs/store/en-US/changelogs/30000.txt`, `docs/store/tr-TR/full_description.txt`, `docs/store/tr-TR/changelogs/30000.txt`: the hint lines.
- `docs/store/README.md`: lengths re-counted, plus a note on hint claims.
- `docs/release-checklist.md`: 1.4, 2.5a (new), 3.5, 3.5a, 3.5b and 5.0, and the §4 upload-script note.
- `docs/ad-testing-safety.md`: §3a now says the steps are lifted.
- `.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md`: Step 7a, plus links from Step 8 and the gate list.
- `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md`: §4a item 2 and its heading.
- `.planning/reports/phase-14/release-manager-release-docs-pass2.md`: this report.

## Checks
- `grep -n -E '^(adsMobileSdk|userMessagingPlatform|playServicesGamesV2|datastore) ' gradle/libs.versions.toml` in `PegSolitarie-dev` (develop 0582e3f) — pass (1.4.0, 4.0.0, 22.0.0, 1.2.1, as the rows state).
- `sed -n 535,547p app/build.gradle.kts` in `PegSolitarie-dev` — pass (the three Google SDKs at `implementation`).
- `ls PegSolitarie-dev` / `git ls-tree` facts reused from the checklist (no `scripts/` on develop 0582e3f) — consistent with the plan's new item 2. I did not re-run this.
- The `python3 -I` length count over the eight store files — pass (counts above, all within limits; the method reproduces the README's old figures from `git show HEAD:`).
- The en-US word-rule grep from `docs/store/README.md` — pass (no output, exit 1).
- The tr-TR word-rule greps (both) — pass (no output, exit 1). My first tr-TR draft, "kitabından gelen", tripped the `gel` rule. I reworded it to "kitabıyla" before this run.
- `grep -rinE 'no winning move|truth|doğruyu|kazandıran bir hamle olmadığını|proven|always|instant|every position' docs/store/en-US docs/store/tr-TR` — pass (no output).
- `grep -n -i -E 'depth|four moves|three on|first moves|book' CHANGELOG.md` — pass (no "four moves (three on Wiegleb-45)" left).
- `grep -c -E '^\| (A|E|F|G|H|I|J)\. ' THIRD_PARTY.md` — pass (7). `grep -n -E 'redistribution permitted|\*\*unverified\*\* — see' THIRD_PARTY.md` — pass (none).
- A table-shape check (`python3 -I`, cells per row vs header) over `THIRD_PARTY.md`, `docs/release-checklist.md` and `docs/store/README.md` — pass after the 5.0 fix (0 mismatches).
- `git diff --check` — pass. `git status --short` shows only the 11 permitted files plus this report.
- Not run, as dispatched: Gradle (any task), `git commit`.

## Open issues / risks
- **R1 is a release blocker, per the game-designer, and it is not on checklist 1.1's list yet.** The ruling says `bugfix/hint-undecided` must merge before `release/3.0.0` is cut. Item 2.5a treats only the *copy* raise as optional, as dispatched. The producer should make sure the tech-lead's blocker table carries R1, because checklist 1.1 reads that table.
- **The listing lost its only "no move" sentence.** The "Today" text says nothing about what happens when Hint finds no move. That is accurate, and 2.5a restores it after R1.
- **The CHANGELOG fixes at :51-53 and :104-105 went outside the :137-141 range named in the dispatch.** Both lines made the claim §4 rules false (the intro is the "headline claim" in the designer's reason (d)). I fixed them to the "Today" column. A reviewer may want to check them.
- **U-6 and §16 #20 still block the release** (`THIRD_PARTY.md` §5b open item; checklist 1.4 and 3.5). The Licenses screen entries for rows F to J need the ux-designer's amendment to `docs/ux/licences-screen.md`. The tech-lead's ruling on the Google SDK notices now also covers hsdp (70 components) and UMP (59).
- **Row I (checker-qual) is not credited yet.** Until the Licenses screen credits it, checklist 3.5a's mapping grep is a per-release duty.
- **Step 7a and `ad-testing-safety.md` §3a are two copies of the same steps.** Both now say that a change to one is made in the other.
- `THIRD_PARTY.md` §7's bullet still says the Licenses screen may be "left for 3.0.1", while §16 #20 is due before the cut. This predates this pass, and the producer decides it.

## Handoff
Next: producer. It should:
- Commit these 11 files and this report on `feature/phase-14-release-docs`.
- Route the code-auditor to re-check F-1, F-2 and I-1 against this diff.
- Confirm R1 is on the tech-lead's branch-cut blocker list.
- Route the ux-designer to the Licenses screen entries (rows F to J, and row I if credited).
- Route the tech-lead to the Google SDK notices ruling, and to scheduling `scripts/play-upload.sh` on a `feature/*` branch (android-game-dev) before Gate 2.
- Founder: nothing new. The copy stays for Gate 2 approval under Q-016 and Q-022.
