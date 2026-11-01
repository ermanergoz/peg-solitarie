# ux-designer report — licences-screen (device re-run rulings: O-2, F-4, F-2)
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the worktree snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; the product's `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: the tech-lead's re-run ruling on this branch (licences-screen)
Ran on: claude-opus-5-5 at effort xhigh. This deviates from the role's pin, fable at xhigh: the Fable limit is reached, and the founder's Q-019 answer (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published.
Fan-out: none. One read-only haiku Explore helper grepped `docs/ux-spec.md`, `docs/ui/DESIGN-SYSTEM.md` and `app/src/main/java` for RTL and text-direction wording, because my tools cannot search. It named no studio role and wrote nothing.
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
1. **O-2:** LS-AC13 and §8.2 item 10 now pass on the **position**. Every row's spoken position must equal its index line number, and the last row's must be N. The total after "of" is TalkBack's wording ("over a hundred" for 108 in TalkBack 16) and is not graded. The exact total stays with the Compose test (`rowCount` N).
2. **F-4: required for 3.0.0** (new **LS-15**). Heading navigation reaches every heading in both directions, from any stop and at any scroll position, with nothing else between two headings. Lazy composition is not an accepted limit on the Licenses list. What TalkBack does past the last heading, or before the first, is the platform's and is now named in pass line 4. O-1's bare "Button" is not accepted.
3. **F-2: §4's intent confirmed and made exact** (new **LS-16**, new **LS-AC19**). Every text reads in its own direction, so its characters stay in written order. A list's lines hang from the mirrored start edge; a text screen's body hangs from its left edge; the chrome mirrors.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/docs/ux/licences-screen.md` — the rulings, with dated pointers and nothing deleted:
  - the frontmatter, and a "Device re-run rulings" paragraph in the edition note;
  - new decisions LS-15 and LS-16 in §1;
  - pointers in §4 Direction, §4A (the F-04 note), §4B Direction and §5 "Dark theme, RTL";
  - in §5.1: "Headings are headings", "Lists are collections", RTL, and pass lines 2 and 4;
  - LS-AC06 (F-4's Compose intent), LS-AC13 (restated), and the new LS-AC19;
  - §8.2 items 2, 6 and 10;
  - §9 items 18 and 19;
  - §10 rows 43 to 45.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/ux-designer-rerun.md` — this report.

## The rulings

**1. O-2: the notices list's total by ear.**
- **What was heard** (`tb-notices-linear-light100.txt`):
  - every row is "k of over a hundred", from 1 to 108;
  - entering the list, the intro is followed by "In list. over a hundred".
- **What stands:**
  - the semantics (`rowCount` N, the last `rowIndex` N − 1, rows counted alone);
  - the intent: the player hears where they are.
- **The restated device oracle.** From pass line 1's transcript of the whole list:
  - every row's position equals its line number in the index, 1 to N, in order, none skipped or repeated;
  - the last row's position is N;
  - the last row's name, sources and ordinal match the last index line;
  - the total after "of" is not graded, but if TalkBack speaks it as a number, that number is N.
- **Why not demand the exact total.** The total is TalkBack's wording on every list on the phone. Putting the count into each row's announcement, or into the intro, would add words to 108 stops and a number §4A chose not to show.
- **What still catches a broken build:**
  - the intro counted as a row would make the first row "2 of …";
  - missing per-row info would drop "k of" entirely;
  - a wrong total is the Compose test's to catch.

**2. F-4: heading navigation on the Licenses list (LS-15).**
- **What the player must experience, in 3.0.0.** With the reading control on Headings, from any stop and at any scroll position:
  - a swipe down goes to the next heading in the §5.1 table, scrolled into view and read whole;
  - a swipe up goes to the previous one;
  - from the top, all five headings are reached in order (the title, Fonts, Sounds, Open-source software, Google libraries), and from the bottom all five in reverse;
  - no non-heading is a stop between two headings.
- **Why required now, and not a named limit:**
  - **A skipped heading misinforms.** Up from "Google libraries", the re-run landed on the title, which tells the player the screen has two sections.
  - **This is the one screen of the four with sections, and Settings next to it passes.** Settings is a plain column with the same header component. Two adjacent screens must not answer the same gesture differently: the principle accepted with the brief's A1.1.
  - **The cost is small.** The list is 23 static items with no loading, so nothing argues for lazy composition.
- **Named as the platform's, and accepted:** a swipe past the last heading or before the first. That covers an end sound, nothing, or a scroll onto a paragraph. The two Jakarta paragraphs QA heard after "Apache License 2.0" are this case, so Jakarta now passes outright.
- **Not accepted:** O-1's card read without its title, or as a bare "Button". It fails pass line 2 (pass line 3 for an inert card) in any navigation. I expect it to go with the F-4 fix, since it was seen only after heading navigation's scroll fallback.
- **The tests:**
  - **Compose (intent; the tech-lead names the test):** with the list scrolled to its end, and again to its middle, the title and each section header is found as a heading in the semantics tree, header by header and never as a count. It should be red on `2d433b8`.
  - **QA (§8.2 item 2):** three runs (down from the top, up from the bottom, both ways from the middle), at 100 % and 200 %, in one theme.
- **The mechanism is the tech-lead's.** I only note that Settings meets the property as built. If the tech-lead finds LS-15 cannot be delivered in 3.0.0, it comes back to me through the producer.

**3. F-2: text direction (LS-16).**
- **§4's sentence rested on a wrong premise.** It assumed content-based direction is the default. In Compose, a text that names no direction takes the layout's. That is why `s23`–`s26` show:
  - ".partnership with others", "The Fredoka Project Authors 2016 ©", "sound effects 7", "Pop 01” by LilMati“";
  - ".POSSIBILITY OF SUCH DAMAGE" on Cronet;
  - an OFL dash rule broken around "SIL".
- **What an RTL-only player must see:**
  - **The chrome mirrors.** This was right on the re-run and stays.
  - **Every text reads in its own direction**, from its first strong character. English is a left-to-right paragraph in every locale, with every character where the string or the asset puts it.
  - **On the two lists**, every line hangs from the mirrored start edge, which is the right in RTL. A card keeps its mirrored shape: lines at the right, chevron at the left.
  - **On the two text screens**, the body is an English document and hangs from the column's left edge in every locale.
  - **By ear, nothing changes.**
- **Why two edges.**
  - A list row is mirrored interface. The house's one RTL alignment line, D-04f's Undecided notice in `docs/ux-spec.md`, already puts UI text at the start edge.
  - A licence is a document. A right-aligned page of English (`s24`, `s26`) is hard to follow line to line.
  - **Rejected: right everywhere**, because it leaves the legal texts ragged on the left.
  - **Rejected: left everywhere**, because it pushes each card's lines against its chevron, unlike every mirrored row.
- **New LS-AC19 (Compose; the tech-lead names the test):** under an RTL layout, each kind of text resolves to a left-to-right paragraph, read from its own text layout. QA's §8.2 item 6 checks the order and both edges by eye, with `[ar-XB]` as the only language.

## Checks
No Bash on this role: every check is a read, recorded as such.
- `Read .planning/reports/licences-screen/qa-engineer-rerun.md` (343 lines) — pass. O-1, O-2, O-3, F-2, F-3 and F-4 read, with the per-item table and the handoff.
- `Read docs/ux/licences-screen.md` (1,294 lines, three pages) — pass. Read before editing: §5.1 pass line 4, LS-AC13, §8.2 items 2, 6 and 10, §4 and §4B Direction, §5 and §5.1 RTL.
- `Read` this role's four earlier reports on the branch: `ux-designer.md` (second edition, loops 1 and 2), `ux-designer-check.md`, `ux-designer-check-loop2.md`, `ux-designer-address-ruling.md` — pass. No earlier ruling touches direction, heading navigation in a lazy list, or TalkBack's wording of a total. F-19's "per header, never a count" rule is applied to LS-15's Compose intent.
- `Read` the re-run transcripts `tb-licenses-headings-fromtop.txt`, `-up.txt`, `-up2.txt`, `-down3.txt`, `tb-jakarta-headings-down.txt`, `-up.txt` and `tb-notices-linear-light100.txt` (220 lines) — pass. The F-4 pattern is confirmed: up skips three headers, the middle start is stuck on the title, and a stray stop precedes "Google libraries". Jakarta is right in both directions, with two paragraphs after the last heading. The positions run 1 to 108 as "k of over a hundred".
- `Read` the screenshots `s23-arXB-only-licenses.png`, `s24-arXB-fredoka-text.png`, `s25-arXB-notices.png`, `s26-arXB-cronet.png` and `t02-licenses-sliver.png` — pass. The chrome mirrors correctly, every text is right-aligned with moved end characters, and the sliver card sits under the top bar.
- Helper grep (haiku, read-only) — pass. `docs/ux-spec.md` has no text-direction rule; its one RTL alignment line is 5414 (D-04f, read in context at 5406–5415). `DESIGN-SYSTEM.md` has no RTL wording. `app/src/main/java` sets `TextDirection` nowhere, which is consistent with QA's suspected cause.
- Consistency after the edits — pass, by re-reading my edit strings against the anchors:
  - LS-15 and LS-16 are referenced from §4, §4B, §5, §5.1, LS-AC06, LS-AC19, §8.2 and §9;
  - §10 rows 43–45 point at live sections;
  - the "N of N" wording now carries a superseding pointer at all four places it occurs: §4A, §5.1, LS-AC13 and §8.2 item 10.
- `./gradlew check` — not run. No Bash; a docs-only change; no code or resource was touched.

## Open issues / risks
1. **The tech-lead's ruling runs in parallel** (§9 item 18). It owns:
   - the mechanisms for LS-15 and LS-16;
   - the tests named in LS-AC06's F-4 half and in LS-AC19;
   - F-3, the width cap, which needs no UX ruling: the spec already says "capped at `content_max_width`, centred".
   If its mechanism for LS-16's two edges collides with these words, I would rather revisit the words than see the edges dropped.
2. **The rest of the app has the same RTL defect** (§9 item 19). Settings shares the section header and row pattern. I propose a house rule for `docs/ux-spec.md` §11 in LS-16's words, after the mechanism is known. That is a later task for this role, for the producer to schedule. It does not block this branch.
3. **O-3 is not ruled here, because it is outside this dispatch.** It covers Settings' title read without "Heading" and the Licenses row read without "Button". Settings' contract is `docs/ux-spec.md`, so the ruling is mine; I recommend the producer route it to me as its own small task. LS-AC01 already expects the title to be a heading "where it is one".
4. **Noted, not ruled:** the notices list has "kotlin" (row 95, lower case) between "Kotlin" rows 94 and 96–98. By ear they sound the same, but A1.5's ordinal groups by exact name, so row 95 has no ordinal while the others read "Notice n of 4". Each row's sources line still tells them apart, so no change is needed for 3.0.0. It could join §9 items 15 and 16 as polish: group the ordinal case-insensitively, as the index sorts.
5. **Not an art-direction choice between equals.** LS-16's two edges rest on legibility and on the house's existing RTL line, and LS-15 on the TalkBack contract. No founder question is drafted.
6. **Staffing and checking.** These rulings were made on Opus under Q-019. LS-16 is a new layout rule, so I recommend one fresh ux-designer check of this fold (LS-15, LS-16, LS-AC19 and the restated lines). It can run while the tech-lead rules the mechanism. Whether to run it is the producer's call.

## Handoff
Next: producer —
1. Read and commit `docs/ux/licences-screen.md` and this report on `feature/licences-screen`. I have no Bash, so nothing is committed.
2. tech-lead: rule the mechanisms for LS-15 and LS-16 and name the tests for LS-AC06's F-4 half and LS-AC19 (§9 item 18). If LS-15 cannot be met in 3.0.0, return it to me through the producer.
3. android-game-dev: build the F-2, F-3 and F-4 fixes to the tech-lead's ruling and this spec.
4. qa-engineer, on the next build:
   - §8.2 item 2's three heading runs at 100 % and 200 %, listening for O-1;
   - §8.2 item 6 with `[ar-XB]` alone: the order and both edges on all four screens;
   - §8.2 item 10 as restated, from pass line 1's transcript;
   - F-3's width cap.
5. Optional, the producer's call: a fresh ux-designer check of this fold; a small task for O-3 (Settings); later, the app-wide RTL house rule (§9 item 19).
