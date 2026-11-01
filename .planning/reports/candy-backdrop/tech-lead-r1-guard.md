# tech-lead report — candy-backdrop: R-1's pixel guard, and the two protected-names tests
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; read at 5e2dadc)
Skills loaded: ergoz-soft:standards (role tech-lead: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and the product's `.claude/rules/lessons.md`, as the product's CLAUDE.md imports them), android-architecture, android-gradle-logic
Ran in parallel with: talkback-board's merge-in, which the developer is running in this worktree
Ran on: claude-opus-5-5 (the founder's Q-019 answer, as in `tech-lead-merge-ruling.md`)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Open issue 1: widen R-1's pixel guard now.** Make it check both palettes' earned fills. The edit is one statement in `ResultsStarPaletteTest.kt`, below. My probe at 5e2dadc: green 2 of 2 unmutated, and red 2 of 2 under the vacuous-pass mutation, where the ruled form is red 1 of 2. It rides with the talkback merge-in round or the auditor loop's fix round, whichever comes first.
- **Open issue 3: keep both `UserFacingTextHasNoProtectedNamesTest` classes for 3.0.0, and record the fold for 3.0.1.** Both are additive scans that must both pass, so the duplicate cannot weaken BD-01 or LS-AC17. Folding means deleting a suite this branch did not author, which needs an auditor line and an edit to the licences spec. That is cost with no safety gain in the 3.0.0 week.

## Outputs
- `.planning/reports/candy-backdrop/tech-lead-r1-guard.md` — this ruling. No product or test file was edited on the branch.

## Rulings

### G-1 — R-1's pixel guard reaches both themes (developer's Open issue 1)

**What is true.**
- `frameWithoutTheScreen()` runs after `frameIn(Colorblind)`, so a reference frame that still held the screen would show the **colour-blind** earned star.
- In dark that star is gold and falls inside `boardMarksFor(Standard, dark).earnedStarFill`'s stops. In light it is amber, outside them, so the Standard-only check misses it. That is the developer's 1 of 2.
- The semantic guard (`assertCountEquals(0)`) still bites 2 of 2. So nothing vacuous passes as committed. The pixel guard is the second line of defence, and as ruled it only half-works. I wrote that form in R-1 step 4, so this corrects my own spec.

**Ruling: widen it now.** The edit is cheap, test-only and probed, and a guard that silently covers one theme of two should not ship.

**The exact edit.** Only `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsStarPaletteTest.kt` changes. Lines 113–121 at 5e2dadc become:
```kotlin
        stars.take(EARNED).forEachIndexed { index, box ->
            val centre = beneath.starPixel(box, STAR_CENTRE_UNITS, STAR_CENTRE_UNITS)
            assertWithMessage("the frame without the screen: no earned star $index's fill at its centre")
                .that(BoardMarkPalette.entries.any { isBetweenStops(centre, boardMarksFor(it, dark).earnedStarFill) })
                .isFalse()
        }
```
- It checks every palette, not just `palette.value` at the time, so the guard stays correct if the two `frameIn` calls are ever reordered.
- The message is unchanged. No import changes: `BoardMarkPalette`, `boardMarksFor` and `isBetweenStops` are already imported. The longest line is 118 characters, under ktlint's 120.
- Nothing else in the class changes: the semantic guard, the unearned-star oracle, the earned-star checks and the KDoc stay as they are.

**"Amended, not weakened": yes, and stronger.** Every assertion stays, and one now covers both themes. The code-auditor's existing R-1 line covers this edit if it re-applies the red below. Otherwise the line is owed for both commits together.

**Reds the developer shows** (both themes each):
- **The vacuous-pass mutation.** Remove the hide step (`screenShown.value = false`) and the `assertCountEquals(0)`. Result: 2 of 2 fail, "the frame without the screen: no earned star 0's fill at its centre / expected to be false". *Probed.*
- **The hide step removed alone:** 2 of 2 fail at `assertCountEquals(0)`, unchanged.
- **N1 (`BoardMarkRecipe.kt:63`):** still 2 of 2, unchanged. The edit does not touch the unearned path.
- Then green 2 of 2, and the full Check.

### G-2 — the two `UserFacingTextHasNoProtectedNamesTest` classes (developer's Open issue 3)

**What is true.**

| | Candy's (BD-01, D9) `app/src/test/java/com/app/pegsolitaire/UserFacingTextHasNoProtectedNamesTest.kt` | Develop's (LS-AC17) `app/src/test/java/com/app/pegsolitaire/ui/UserFacingTextHasNoProtectedNamesTest.kt` |
|---|---|---|
| Scope | `res/values*/strings.xml` and `assets/**` | the same |
| Words | the same sixteen, stored reversed | the same sixteen, in plain text |
| Callouts (`tasty!`, `delicious!`, `divine!`, `sweet!`) | yes | no |
| Failure message | one per file | `path:line: word` |
| Named by | candy-backdrop §11 BD-01 and §12's table | licences-screen LS-AC17, §3.2 and the decision log's row 28 |

- The two classes have different FQNs, so both compile and both run.
- Either one failing fails the Check, so together they enforce the union. Candy's already covers a superset of develop's.
- A drifted or narrowed list in one cannot open a gap, because the other still runs.

**Ruling: keep both for 3.0.0, no edit.** Merging now means deleting or rewriting develop's suite. That is a suite this branch did not author (A7.5), so it needs an auditor approval line, and LS-AC17 would have to be repointed in `docs/ux/licences-screen.md`. That is a ux-designer edit and a fresh review, for no safety gain.

**One rule until the fold.** Candy's class is the owner, as licences' own note says ("candy owns its word list thereafter"). A word added before 3.0.1 goes into both lists, stored reversed in candy's. Neither list is ever narrowed.

**For 3.0.1 (the producer files it as a follow-up item):**
- Fold into one class: candy's root-package class survives.
  - It keeps the reversed storage. Develop's plain-text list would print lines on BD-01 command 3 (`git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'`) as soon as any branch edits it.
  - It keeps the callouts.
  - It takes on develop's `path:line: word` messages and its separate non-empty check on `assets/`.
- Delete `ui/UserFacingTextHasNoProtectedNamesTest.kt`. Deleted lines are `-`, so command 3 still prints nothing.
- Repoint LS-AC17, `licences-screen.md` lines 86, 755, 1253 and 1331, to the surviving class. That is the ux-designer's edit.
- The code-auditor gives the approval line for the deletion.

## Checks
- `uptime` before each Gradle run: 1-minute load 2.31 and 6.21 (bound 12).
- `git worktree add --detach <scratchpad>/tl-r1 5e2dadc`: pass. I copied `local.properties` in. I edited only the scratch worktree, then ran `git worktree remove --force` and `git worktree prune`. `git -C …/PegSolitarie-candy log -1` was still 5e2dadc. I made no edit in the candy worktree.
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.results.ResultsStarPaletteTest' -q` with G-1's edit: pass. JUnit XML: tests="2" failures="0" errors="0".
- The same test with G-1's edit, the hide step and the `assertCountEquals(0)` removed: **fail** as intended, gradle exit 1. The XML reads tests="2" failures="2", both `theStarRowDrawsTheActivePaletteInTheDarkTheme` and `…LightTheme`, with "the frame without the screen: no earned star 0's fill at its centre / expected to be false".
- I read both protected-names classes in full at 5e2dadc. `grep -rn UserFacingTextHasNoProtectedNamesTest docs` finds them named in `licences-screen.md` (86, 755, 1253, 1331, 1625) and `candy-backdrop.md` (BD-01, §12).
- Sources read: `android-game-dev-merge-licences.md`, `tech-lead-merge-ruling.md`, and `ResultsStarPaletteTest.kt`, `StarPixels.kt` `isBetweenStops`, and `candy-backdrop.md` BD-01 with its three commands, all at 5e2dadc.

## Open issues / risks
1. **This report is not committed yet.** The developer's talkback merge was in progress in the worktree (`MERGE_HEAD` present, unmerged paths). A commit then would have wrapped or broken that merge. The text sits at the scratchpad path the producer was given. Commit it alone, `git add` this file only, once the merge commit lands.
2. **Rule-file freshness.** I loaded the rules as the product CLAUDE.md imports them, not the plugin v1.0.0 copies that the developer read. Nothing in this ruling depends on a rule that changed between them, as far as the two reports quote.
3. **The approval lines still owed:** R-1 (bd8abf5 together with the G-1 commit) and R-2 (c1e8c50), from the code-auditor.

## Handoff
Next: producer —
1. **android-game-dev** (talkback merge-in round or the auditor's fix round): apply G-1 exactly. Show the vacuous-pass red at 2 of 2 and the N1 red, then the full Check. No change to either protected-names class.
2. **code-auditor:** the "amended, not weakened" lines for R-1 with G-1, and for R-2.
3. **Backlog for 3.0.1:** G-2's fold (developer, ux-designer for LS-AC17's references, auditor for the deletion).
4. **tech-lead:** sign-off after the auditor and QA reports.
