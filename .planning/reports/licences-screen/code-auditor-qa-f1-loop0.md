# code-auditor report — licences-screen, QA F-1 fix (loop 2)
Date: 2026-10-24
Branch: feature/licences-screen (committed by the producer) — reviewed `e7fea02..2d433b8`, which is the one fix commit `2d433b8`. The Check ran at `4b5747b`, whose tree differs from `2d433b8` only by the developer's report. HEAD has since moved to `68ea217`, a docs-only commit: the ux-designer's ruling.
Skills loaded: ergoz-soft:standards (role code-auditor; the worktree's rules snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: home-chip-arrival tech-lead ruling, talkback-board QA, licences-screen ux ruling, reduced-motion ux ruling, colour-vision auditor/tech-lead/ux rulings
Ran on: claude-opus-5-5 at effort xhigh. **Deviation:** the agent file pins fable. The Fable limit is reached, and the founder's answer to Q-019 (option 3) puts every Fable-pinned role on Opus until 3.0.0 is published. So this review ran on the developer's own model. To make up for that, I re-ran the Check myself with the `:app` tests, detekt and ktlint forced fresh, and probed the oracle with my own mutations. I trusted no recorded result.
Fan-out: none
Loops: 2
Lessons applied: none (the snapshot lists 0 active lessons)

> Producer note: returned as text (the role declined to write report files); written here verbatim by the producer.

## Summary
- The fix removes the extra TalkBack stop for all five addresses. Each address is now a plain `Text` inside its card's one merged node.
- The new oracle walks the unmerged tree and fails on the old code. Of the seven contract tests, it is the only one that does.
- The cards' merge, roles and click labels are unchanged for all 18 entries.
- The Check is green: 1,326 `:app` tests, run fresh, with 0 failures.
- **One must-fix, F1:** a comment the fix left stale. F2 to F4 are advisory or for the record.

## Findings
1. **F1 — Minor — must fix.** `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicenseCatalog.kt:325` still reads "Inert: the SDK licence is not bundled, so its address is shown as selectable text (LS-05)."
   - This has been false since `2d433b8`. It now also contradicts LS-05 as amended and LS-14 (spec, `68ea217`).
   - The diff updated the two comments beside it (`LicenseCatalog.kt:42` and `LicenseEntryCard.kt:41-43`) but missed this one.
   - `git grep -n -i selectable 2d433b8 -- app/src/main/java/com/app/pegsolitaire/ui/licenses` finds only this line.
   - **Fix:** for example, "Inert: the SDK licence is not bundled, so its address is plain text read within the card's one stop (LS-05 as amended, LS-14)." The change is to a comment only.
2. **F2 — Minor — advisory: the oracle's scope.** `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesTalkBackContractTest.kt:101-129` walks up from the five address texts only, and checks only `Focused` and `RequestFocus`.
   - My mutant M3 survived with 7 of 7 tests passing. It put a long-click action on the five addresses and a focusable `SelectionContainer` around every plain line. My dump showed 50 stop-making nodes under the 18 cards.
   - The `SelectionContainer` part is exactly F-1's mechanism, moved to a credit or parts line.
   - This is not a gap against the amended LS-AC06, which asks for the five addresses only.
   - **Recommendation:** walk every unmerged node under every card, on both lists, and reject a merge boundary, `Focused`, `RequestFocus`, `OnClick` or `OnLongClick` below the card. This would also pin LS-14's rule that the card stays one stop once the deferred copy action lands: its long-click must sit on the card's merged node, never on the address `Text`.
   - A cheaper addition: the inert-entry test could assert that the inert cards' merged nodes have no `RequestFocus`. At `e7fea02` all four inert cards carried it, inherited from the address container, so a merged-tree test could have seen F-1.
   - **Route:** the tech-lead, for the LS-14 brief, and the developer at the next touch.
3. **F3 — Info, for QA.** The two text screens still use focusable `SelectionContainer` nodes of the same kind:
   - the Google notice text screen has one per paragraph, each wrapping a plain `Text` (`GoogleNoticeTextScreen.kt:106`);
   - the bundled text screen has one around the whole body (`LicenseTextScreen.kt:69`).

   Both are required by LS-08, and selection there passed §8.2 item 8. But TalkBack on those screens was blocked in QA's session, so pass lines 1 and 7 are unverified there. On Cronet's notice in particular, QA should listen for a paragraph read twice or an extra stop per paragraph. This is not a finding against this diff.
4. **F4 — Info: the developer's claims hold.**
   - M1 reproduces the recorded red message: "kenney-interface-sounds: … RequestFocus".
   - The Check's 1,326 tests match the developer's report.
   - QA's device dump confirms the cause: in `.planning/qa/licences-screen/tb-licenses-linear.txt`, line 9 is the Kenney card's stop, ending at the licence, and line 10 is its address as a separate stop. The other four addresses are lines 12, 14, 36 and 41.

**Not ruled here:** the selection trade-off. The ux-designer ruled it at `68ea217` (LS-14: the loss of copying is accepted for 3.0.0, and copying is deferred). The code at `2d433b8` matches LS-05 and §3.1 as amended, and the unmerged-tree oracle LS-AC06 gained. I read the ruling only to word F1's fix.

## What was verified and holds
1. **The stop is gone for all five addresses: Kenney, LilMati, CogFireStudios, the Public Suffix List and the SDK terms.**
   - In probe P0 (`2d433b8`, unmutated), each address is a plain `Text` child of its card with no merge, focus or click flag, and its first merging ancestor is that card.
   - Each card's merged text list ends with its address.
   - The new test loops over all five and passes. M1b shows it also catches the address on a tappable card, not only on the first inert one.
2. **The oracle walks the unmerged tree and fails on the old code.**
   - It finds each address with `onNodeWithText(…, useUnmergedTree = true)` and walks up through unmerged parents.
   - It exempts only the card node itself, whose focus is a tappable card's own stop. So it catches a focusable on the `Text` itself (M2).
   - On the old code (M1), it is the only one of the seven tests that fails. The other six pass on code with the defect, which shows the merged-tree tests could not see F-1.
3. **No regression in the merge, role or click label.**
   - `LicenceCard` (`LicenseEntryCard.kt:69-118`), which holds the merge, `Role.Button`, the click label and the chevron, is untouched by the diff.
   - The P0 and M1 dumps differ only by the five focusable nodes, and by the `Focused`/`RequestFocus` that the four inert cards' merged nodes inherited from them on the old code.
   - Across the 18 entries, the texts, roles and click labels are identical between the two.
   - The role, label, inert-entry, reading-order, heading and chevron tests pass. The Google notices rows share the unchanged `LicenceCard`, and their suites pass in the Check.
4. **Suppressions, dependencies, first frame and security.**
   - No new `@Suppress`, baseline, `@Ignore`, opt-in or `!!`, so there is nothing to approve. No dependency was added.
   - Only `ui/licenses` files changed, so no start-path file is touched and no 15e sitting is needed.
   - Security: the fix adds nothing. There is no network access, input or secret, and it removes one interaction.
   - Performance: one fewer node per address card.

## Mutation probes
I ran these in a throwaway `git worktree add --detach` of `2d433b8`, outside the repo, with `sdk.dir` only; it has since been removed. `AuditorProbeTest` is my own dump test. It lived only in that worktree and was never committed.

| Probe | Mutation of `LicenseEntryCard.kt` | `LicencesTalkBackContractTest` | Dump |
|---|---|---|---|
| P0 | none | 7/7 pass | each of the 18 cards is its entry's one merged node; 0 child nodes with a merge, focus, click or long-click flag |
| M1 | the `e7fea02` file: the address back in a `SelectionContainer` | **1 fails**, the new test: "kenney-interface-sounds: … RequestFocus : AccessibilityAction(… FocusableNode.requestFocus …), Focused : false" | 5 focusable nodes, one around each address; everything else as P0 |
| M1b | `SelectionContainer` around the Public Suffix List address only (a tappable card) | **1 fails**: "public-suffix-list: … Focused : false, RequestFocus …" | — |
| M2 | `Modifier.focusable()` on the address `Text` | **1 fails**: "kenney-interface-sounds: …" | — |
| M3 | a long-click action on the five addresses, plus `SelectionContainer` around every plain line | **7/7 pass (survives; F2)** | 50 flagged child nodes across all 18 cards |

## Outputs
- `.planning/reports/licences-screen/code-auditor-qa-f1.md` — this report. It was returned as text, because my operating instructions forbid writing report files; the producer writes it verbatim and commits it alone.

## Checks
- `./scripts/check.sh --max-workers=4 --console=plain :app:compileDebugKotlin --rerun :app:testDebugUnitTest --rerun` at `4b5747b` (code identical to `2d433b8`, clean tree) — **pass**: `BUILD SUCCESSFUL in 11m 14s`, exit 0, 107 tasks with 13 executed.
  - `:app`: 1,326 tests in 138 classes, 0 failures, 0 skipped, run fresh between 17:31 and 17:32Z.
  - `LicencesTalkBackContractTest`: 7/7.
  - 0 `w:` or `e:` lines. `lintDebug` and `:domain:depthSweep` executed.
- `./scripts/check.sh --max-workers=4 --console=plain :app:detekt --rerun :app:ktlintMainSourceSetCheck --rerun :app:ktlintTestSourceSetCheck --rerun` — **pass**: `BUILD SUCCESSFUL in 11m 25s`, exit 0.
  - detekt and both ktlint source-set checks executed.
  - Lint: `0 errors, 64 warnings`, the same count as merge-in 2. None is in `ui/licenses`, `data/licenses` or the licences strings block: the `strings.xml` hits are at lines 17–391, and the block ends at 537.
- In the throwaway worktree, `./gradlew --console=plain :app:testDebugUnitTest --tests '…LicencesTalkBackContractTest' --tests '…AuditorProbeTest'` for P0, M1, M1b, M2 and M3 — results as in the table above.
- `git diff e7fea02..2d433b8`; `git diff --stat 2d433b8 4b5747b` (only the developer's report); `git diff 4b5747b 68ea217 -- app build-logic domain gradle` (empty: docs only).
- `git grep` at `2d433b8` for `SelectionContainer`, for `focusable|focusRequester|onLongClick|combinedClickable`, and for `selectable` — pass. Only the two text screens keep a `SelectionContainer` (F3). No other focusable sits in `ui/licenses` or `ui/settings`. The one stale comment is F1.
- Read QA's `.planning/qa/licences-screen/tb-licenses-linear.txt` (lines 9–10, 12, 14, 36, 41) — consistent with F-1's cause.

## Open issues / risks
- QA's device re-run of §5.1 pass lines 1 and 3 on `2d433b8` is still owed. It can run now, because F1 changes no behaviour. F3's listen on the text screens goes with it.
- F2 matters most when the deferred copy action (LS-14, §9 item 15) is built.

## Handoff
Next: producer —
- Send F1 to the android-game-dev in fix mode: a one-line comment edit in `PegSolitarie-lic`.
- Dispatch QA's §5.1 re-run on `2d433b8` in parallel, with F3.
- Re-dispatch the code-auditor on the F1 commit: a diff read plus the Check.
- Then the tech-lead signs off.
- F2 goes to the tech-lead for the LS-14 brief.

VERDICT: FAIL
