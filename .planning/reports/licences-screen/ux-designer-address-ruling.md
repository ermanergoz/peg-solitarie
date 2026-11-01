# ux-designer report — licences-screen (address ruling, after QA F-1)
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the worktree snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; the product's `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: home-chip-arrival (tech-lead ruling), talkback-board (QA), licences-screen (code-auditor), reduced-motion (ux ruling), colour-vision (auditor, tech-lead and ux rulings)
Ran on: claude-opus-5-5 at effort xhigh. This deviates from the role's pin, fable at xhigh: the Fable limit is reached, and the founder's Q-019 answer (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published.
Fan-out: none. One read-only haiku Explore helper listed `.planning/qa/licences-screen/` and located lines in `ui/licenses/`, `ui/theme/` and the tests, because my tools cannot list directories or search. It named no studio role and wrote nothing.
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
1. **Address selection: deferred, with a recorded follow-up.** The loss is accepted for 3.0.0. The new decision LS-14 fixes the follow-up's shape: a long-press on the card copies its address, and the same long-click action "Copy address" serves TalkBack, so the card stays one stop. The developer's accessibility-only action is rejected, because it would give the ear an action the eye lacks.
2. **The two strings are confirmed** (no earlier ux-designer report on this branch had confirmed them). The Jakarta credit, "The Eclipse Jakarta Dependency Injection project", is the NOTICE's own attribution line. The U+2011 hyphens in the Public Suffix List date are confirmed too. One non-blocking check remains: how the speech engine says the date, which QA could not hear. Its fix is decided in advance.
3. **The 200 % id break is acceptable for 3.0.0.** The id `play-services-ads-identifier` is wider than the line, so the platform cuts it between two letters. No content is lost, nothing is truncated, and TalkBack reads the id whole. The finer break, after the id's own hyphen, is recorded as optional polish with its shape.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/docs/ux/licences-screen.md` — the ruling written into the spec, with dated pointers and nothing deleted:
  - frontmatter, plus an "Address ruling" paragraph in the edition note;
  - LS-05's pointer, and the new LS-14;
  - §3.1's pointer, and three pointers in §3.2: the Jakarta row (credit confirmed), the PSL row ("selectable" superseded) and the Google entries;
  - §5.1: the table cell and the Text-scaling rule for overlong tokens;
  - LS-AC06: the unmerged-tree oracle;
  - §6: the two confirmed strings, and the fix for the date decided in advance;
  - §8.2: a note on item 1, and a new item 12 (the date by ear);
  - §9: new items 15 (copy address), 16 (id-break polish) and 17 (the Jakarta "How credited" cell);
  - §10: rows 38 to 42.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/ux-designer-address-ruling.md` — this report.

## The rulings

**1. Address selection: deferred, with a recorded follow-up (LS-14).**
- **Why the loss is acceptable for 3.0.0:**
  - No obligation rests on copying. MPL-2.0 §3.2(a) asks the app to *inform* recipients, and the shown exact-version address and its date do that, as `research-analyst-mpl.md` verdict 2 required.
  - Every bundled and notice text stays selectable on its reader. QA passed §8.2 item 8, copy included, so the PSL NOTICE's two addresses can still be copied there.
  - Eye and ear are now equal: neither can copy.
  - A new interaction would reopen the build, the audit and QA on the branch that blocks the release cut.
- **The real cost** is the 97-character PSL address with its 40-character commit hash. That cost is why copying is deferred and not dropped.
- **The by-ear cost, also accepted:** TalkBack now reads that hash at the end of the PSL card's announcement, before "Button". The address comes last, and a swipe interrupts it.
- **The follow-up's exact shape:**
  - A long-press anywhere on any of the five address cards copies that card's address, exactly as shown.
  - The same action is the card's long-click semantics action, `licenses_copy_address_action` "Copy address", on its one merged node.
  - An inert card gains no click action, no role, no chevron and no squish, so LS-06 and pass line 3 stand.
  - Feedback: on Android 13 and later, the system's clipboard confirmation and nothing from the app. Below 13, `licenses_address_copied` "Address copied", polite. The long-press haptic, and no sound.
  - The tests and the QA steps are named in LS-14. The mechanism is the tech-lead's.
- **The developer's suggested "Copy address" accessibility action is rejected as the replacement.** On its own it is ear-only, the reverse of LS-13's rule.

**2a. Jakarta credit: confirmed.**
- `licenses_jakarta_credit` reads "The Eclipse Jakarta Dependency Injection project".
- These are the words of `Jakarta-Inject-NOTICE.md` line 3, "This content is produced and maintained by the Eclipse Jakarta Dependency Injection project."
- The NOTICE names the Eclipse Foundation only as trademark holder (line 9), so the spec's "Eclipse Foundation" is superseded.
- By ear the card reads "Jakarta Dependency Injection. The Eclipse Jakarta Dependency Injection project. Apache License 2.0. Button" (QA's transcript). The repetition is the rights holder's own wording and is accepted.

**2b. U+2011 in the PSL date: confirmed.**
- **By eye:** at 200 % in dark (`29-licenses-dark-200-p07.png`), "2023‑02‑15" renders like the hyphen of "public-suffix" on the line above and stays on one line.
- **No lint suppression:** it clears TypographyDashes without one.
- **By ear:**
  - TalkBack passes the characters to speech unchanged, with no symbol name (`tb-licenses-linear.txt` line 35).
  - What the speech engine makes of them was not heard: the emulator muted TTS audio (`tts-check.txt`: "AudioHardening background playback would be muted for com.google.android.tts").
  - §8.2 item 12 is the non-blocking check on a device with audible speech.
  - If the check hears a character name or loses a number, the fix is already decided: "…, as it stood on February 15, 2023", ASCII only.

**3. `play-services-ads-identifier` at 200 %: acceptable for 3.0.0.**
- **What happens** (`31-gnotices-mid-dark-200.png`, J2ObjC notice 4 of 5): the line reads "play-services-ads-identifi" / "er, play-services-cronet".
- **Where the platform already breaks well:**
  - between ids at ", " (`43-gnotices-top-light-200.png`, `44-gnotices-mid-light-200.png`);
  - after "/" in addresses (`42-licenses-end-light-200.png`).
- **Why the cut falls between letters here:** it never splits at a hyphen with hyphenation off. "inline-install", "open-source" and "public-suffix" stay whole. So a token wider than the line is cut where it overflows. The PSL commit hash is cut the same way, and it has no boundary at all.
- **Why it is accepted:**
  - every character is shown;
  - nothing is truncated, scrolls sideways or overlaps;
  - TalkBack reads the id whole;
  - the ids are where-found metadata two taps deep.
- **Why neither available fix is better for 3.0.0:** automatic hyphenation would *add* hyphens that are not in Google's ids, and invisible break characters change the string that TalkBack and the tests read.
- **The finer break** (after the id's own last hyphen that fits, never between letters, never adding a hyphen, display-only) is §9 item 16. It is optional and not scheduled.

## Checks
No Bash on this role: every check is a read or a computation, recorded as such.
- `Read docs/ux/licences-screen.md` (1,139 lines, three pages) — pass. LS-05, §3.1, §3.2, §5.1, §6, §8, §9 and §10 located; every place that said "selectable" for an address now carries a pointer.
- `Read .planning/reports/licences-screen/qa-engineer.md` — pass. F-1, the §8.2-8 PASS (selection and copy) and the 200 % cosmetic note read.
- `Read .planning/reports/licences-screen/android-game-dev.md` — pass. Deviations 5 and 6, "Loop 2 — QA F-1" (`2d433b8`) and the handoff's ux-designer line read.
- `Read` this role's three earlier reports (`ux-designer.md`, `ux-designer-check.md`, `ux-designer-check-loop2.md`) — pass. None confirms the Jakarta credit or the U+2011 date (the loop-1 check raised F-16 as "sourced from the NOTICE"; the wording was never confirmed), so both are ruled here.
- `Read app/src/main/assets/licenses/Jakarta-Inject-NOTICE.md` and `PublicSuffixList-NOTICE.txt` — pass. The credit's words are line 3. The NOTICE carries `publicsuffix.org/list/…` and `mozilla.org/MPL/2.0/`, which stay copyable on the reader.
- `Read app/src/main/res/values/strings.xml` lines 380–539 (the licences block) — pass. `licenses_jakarta_credit` and `licenses_psl_parts` (`2023&#8209;02&#8209;15`) are as the developer reported. The five address keys are `translatable="false"`.
- `Read` QA screenshots `43-gnotices-top-light-200.png`, `44-gnotices-mid-light-200.png`, `47-gnotices-cronet-rows-light-200.png`, `31-gnotices-mid-dark-200.png`, `42-licenses-end-light-200.png`, `29-licenses-dark-200-p07.png` and `29-licenses-dark-200-p08.png` — pass. The overlong-token cut was seen on the J2ObjC row and on the PSL hash. The U+2011 date renders and stays on one line.
- `Read .planning/qa/licences-screen/tb-licenses-linear.txt` and `tts-check.txt` — pass. The pre-fix transcript shows the five address stops (F-1) and the date passed through unchanged. TTS audio was muted.
- Character counts, by hand — pass:
  - PSL address = 11 + 13 + 5 + 5 + 40 + 1 + 22 = 97, with a 40-character hash.
  - `play-services-ads-identifier` = 28.
  - `user-messaging-platform` = 23, which nearly fills the 200 % line on the test phone.
- `./gradlew check` — not run. No Bash; a docs-only change; no code or resource touched.

## Open issues / risks
1. **A stale KDoc, for the developer and the code-auditor (not mine to fix):** `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicenseCatalog.kt:325` still says "its address is shown as selectable text (LS-05)", which `2d433b8` made untrue. Line 42 of the same file has the right wording.
2. **The TalkBack transcript on disk predates `2d433b8`.** QA's re-run of pass lines 1 and 3 should hear each of the five addresses as the last line of its card's one stop. On the PSL card that means the hash before "Button".
3. **On narrower phones the overflow cut reaches more ids.** By the 200 % measurements, ids of about 20 characters or more will be cut on a 360dp window. It is accepted under the same rule. The blocked 360 × 640 check (§8.2 item 7) will show it, and it is not a finding (§8.2 item 1 note).
4. **The date by ear is unverified** (§8.2 item 12, non-blocking; it needs audible speech).
5. **Two follow-ups await scheduling after 3.0.0:** §9 item 15 (copy address, shape fixed) and item 16 (id-break polish, optional). Neither needs a founder question: neither is an art-direction choice between equals, and neither touches money or a gate.
6. **Staffing:** this ruling ran on Opus under Q-019. Whether this small, pointer-only fold wants a fresh check is the producer's call. I recommend none: the build is unchanged, and the one new decision (LS-14) is deferred.

## Handoff
Next: producer —
1. Read and commit the spec change and this report on `feature/licences-screen`. I have no Bash, so nothing is committed.
2. qa-engineer, in the already-scheduled re-run of §5.1 pass lines 1 and 3:
   - the addresses read inside their cards (open issue 2);
   - §8.2 item 12 if speech is audible;
   - the overflow cut is accepted (§8.2 item 1 note).
3. code-auditor, reviewing `2d433b8`: the stale KDoc at `LicenseCatalog.kt:325` (open issue 1).
4. release-manager: §9 item 17, the Jakarta "How credited" cell.
5. Backlog after 3.0.0: §9 items 15 and 16, which can go together as one licences polish task (tech-lead brief, then android-game-dev, then QA).
