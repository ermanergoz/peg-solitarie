# tech-lead report — licences-screen (ruling on F-4, reopened by the QA re-check)
Date: 2026-11-01
Branch: feature/licences-screen (report only) — rules on QA's re-check at `f908571` (code `9b6d232`)
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0: the plugin copies of compose, testing, kotlin-android, reporting and dispatch read in full; logging, puzzle-design and play-store are identical to the snapshot in context; studio, gitflow, skills-policy, memory and stacks read as the snapshot plus a diff against the plugin copy; `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), android-architecture, android-gradle-logic, android-accessibility
Ran in parallel with: home sign-off, candy 15m oracle spec
Ran on: claude-opus-5-5. **Deviation:** my agent file pins fable at xhigh. Q-019 puts Fable-pinned roles on Opus, and the founder's 2026-10-31 directive lowers every role one effort step; the harness names no effort. The code under ruling was written on Opus too, so every cause below is checked against the pinned Compose source, not taken from QA's report.
Fan-out: none
Loops: 0 (the second tech-lead ruling on F-4; the first is A1.2 of `tech-lead-rerun-ruling.md`)
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **The root cause is verified in Compose 1.11.3's source.** Compose hands TalkBack only the nodes that are on screen. The non-lazy `Column` put every header into Compose's semantics tree, which the JVM tests read. It could not put them into the accessibility tree that TalkBack searches. My A1.2 treated those two trees as one, and that was wrong.
- **So TalkBack reaches a header that is off screen only by scrolling.** A pinned title that is a heading, a full-page animated scroll, and TalkBack's own choice of where to focus after it account for all four observations. **No change to this screen's composables can deliver LS-15 as written.** Per spec §9 item 18, LS-15 goes back to the ux-designer through the producer, with the options and my recommendation in R1.
- **The card read without its title (O-1, pass line 2) is an app defect, and it is fixable now.** A card's speech is built from whichever of its children are on screen. **R2:** the card carries its whole label itself, and its texts are hidden from accessibility. This is one developer loop, test-first. It holds whatever the ux-designer rules.
- **QA's re-check (R4)** grades pass line 2 on the loop head. Pass line 4 is graded against the ux-designer's ruling. If LS-15 stands as written, F-4 stays open by construction until a redesign is built (R1, option E).

## The root cause, verified

**Layer 1: Compose exposes only on-screen nodes to accessibility.** The source is `ui-android-1.11.3-sources.jar`.
- `SemanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap` (`SemanticsOwner.kt:162-330`), in `findAllSemanticNodesRecursive` (249-313):
  - It adds a node only if its *clipped* touch bounds intersect the window's uncovered region (`region.intersect(unaccountedSpace)`, 267).
  - A header scrolled out of the `verticalScroll` viewport has empty clipped bounds. It never enters `currentSemanticsNodes`.
- **Nothing the accessibility layer builds can see it.**
  - The child lists come from that map (`AndroidComposeViewAccessibilityDelegateCompat.android.kt:676-697`, `currentSemanticsNodes.contains(child.id)`).
  - So does the traversal order (`setTraversalValues`, 3430-3453, `isVisible = { currentSemanticsNodes.containsKey(it.id) }`).
- **So off-screen headers are absent from the tree TalkBack searches, whatever the body is made of.** A `LazyColumn` does not compose them. A `Column` composes them, and then Compose withholds them.
- **The A1.3 tests read Compose's semantics tree, not the exposed one.** `./gradlew :app:testDebugUnitTest --tests '…LicencesTalkBackContractTest'` passes 11 of 11 on the code QA failed (Checks). They are necessary, because they fail a lazy body. They are not sufficient. QA's "in the tree is necessary, not sufficient" is right.

**Layer 2: TalkBack reaches a header that is off screen only by scrolling, and Compose's scroll moves a full page.**
- Compose turns `ACTION_SCROLL_FORWARD` and `ACTION_SCROLL_BACKWARD` into `ScrollBy` by the whole viewport length (delegate 1584-1611; `getScrollViewportLength` falls back to the viewport height).
- The scroll is animated (`foundation-android-1.11.3`, `Scrollable.kt:1147-1160`, `semanticsScrollBy` → `animate`).
- After one such scroll, the node TalkBack searched from has left the exposed tree.

**Each observation, explained.** TalkBack's own source is not on this machine. Where a step depends on TalkBack's internals, it is marked as inference. Every Compose step is verified.

| QA observation (`tb-f4-*.txt`) | Cause |
|---|---|
| **Up from the last entry:** "Google libraries", then the title; Open-source software, Sounds and Fonts skipped (`tb-f4-up-from-last.txt`) | The top-bar title is a heading, it is always on screen, and it comes before the list in traversal order. TalkBack's backward search finds it among the exposed nodes, so it never scrolls the list back. Any scrolling list under a pinned heading title does this. |
| **From the title, with the list mid-scrolled:** three swipes down, the title each time (`tb-f4-mid-down.txt`) | No heading after the title is exposed. The title is outside the scroll container, so TalkBack has no scrollable ancestor of the focus to scroll *(inference; consistent with three swipes and no movement in `f4-03` = `f4-04`)*. |
| **Down:** a stray stop "Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations. Button" between Open-source software and Google libraries (`tb-f4-down-from-title.txt`) | No heading is exposed below "Open-source software", so TalkBack scrolls a full, animated page. The pivot then leaves the exposed tree, and TalkBack focuses the first newly exposed node: the Gson card, whose top three lines are above the viewport *(the page length and the pruning are verified; the focus fallback is TalkBack's, inferred)*. |
| **A card read without its title** (the Gson card above; the Cronet card in `tb-f4-linear-to-last.txt`) | A merging card has no text of its own. A node's accessibility text is its own unmerged `Text` (delegate 1218-1220, 3470-3474), so TalkBack builds the card's speech from its children. Compose does include the children of a partially off-screen merging card (`SemanticsOwner.kt:274-298`, flag `isAccessibilityShouldIncludeOffscreenChildrenEnabled = true`). But a child whose clipped bounds are empty is marked not visible to the user (delegate 812, 1200-1201), and TalkBack leaves it out. The title, which is above the viewport, is dropped. **This one is the app's to fix (R2).** |
| **Linear forward swipes from the title, with the list at its end,** cycle over the eight visible stops (`tb-f4-linear-to-last.txt`) | Forward navigation enters the list at its first exposed node, and a forward swipe never scrolls back. That is standard platform behaviour, and it is not a finding by itself. Its defect is the Cronet card read without its title, which is R2. |

**A1.2 is withdrawn.** Its sentence "TalkBack's heading granularity then finds the next and previous heading in the node tree" was false. The evidence it cited is Settings. Settings passes only because it nearly fits on one screen. By the same mechanism, it would be expected to fail at 200 %. That is an inference and has not been observed.

## The rulings

### R1 — LS-15 as written is not deliverable by a change to this screen; it returns to the ux-designer (spec §9 item 18)
**Why.** TalkBack can jump only to nodes on screen (Layer 1).
- Every off-screen header is reached through TalkBack's own scroll, and TalkBack chooses the focus after it. The app does not control that choice.
- A pinned title that is a heading is always the nearest exposed heading going up.
- "From any stop and at any scroll position … nothing that is not a heading between two headings" therefore depends on TalkBack, not on the composable tree.

**Options considered:**

| # | Option | Ruling |
|---|---|---|
| A | Keep the non-lazy `Column` | **Kept.** It is necessary. A lazy body would also lose the headers that the A1.3 tests guard. |
| B | A shorter accessibility page through the public `getScrollViewportLength` semantics, so the pivot stays on screen after TalkBack's scroll | **Rejected.** It retunes every TalkBack scroll on the screen, the two-finger page included. Whether TalkBack then continues from the pivot is TalkBack's behaviour, which cannot be proven on the JVM. It does nothing for the up or mid-list failures. |
| C | Custom accessibility actions ("jump to Sounds") | **Rejected again.** They are ear-only, which is against LS-13's rule. |
| D | The title scrolls with the list, and only a back button stays pinned | **Not recommended.** It is a chrome change, and the ux-designer's to choose. Going up, TalkBack would then scroll and land through the same post-scroll fallback that produced the stray stop. It is unproven without a device spike. |
| E | All five headings always on screen: a pinned, visible section index (four section controls that scroll the list) | **The only option that meets LS-15 as written,** by construction, and it is visible to everyone, not ear-only. It is a redesign: the ux-designer's, and the founder's if it is a question of taste or scope. |
| F | Restate LS-15's device half to what the platform gives a scrolling list | **My technical recommendation.** Down from the top, every heading is reached in order. Any stop TalkBack makes after its own scroll is read whole (pass line 2, kept by R2). Going up, and from the pinned title with the list scrolled, TalkBack's behaviour is the platform's: it is recorded and not graded. Settings at 200 % behaves the same way, so the "adjacent screens agree" principle still holds. |

I edit no spec text. The ux-designer rules the words. The producer queues a founder question only if the ux-designer frames E against F as a question of taste or scope.

### R2 — what the developer builds now: a card's speech never depends on where it is clipped
This fixes O-1 and pass line 2 in every kind of navigation. It holds under every outcome of R1, so it does not wait for the ux-designer.

**The exact change** (`ui/licenses/` only):
1. **The separator.** Add `LicensesUi.CARD_LABEL_SEPARATOR = ". "` with a KDoc line. It reproduces the pauses QA's transcripts record today ("Public Suffix List. Mozilla and the list's contributors. …"). No call site writes a literal.
2. **The label.** `LicenceCard` gains a required `label: String` parameter. Its existing merged node sets `contentDescription = label` inside the same `.semantics(mergeDescendants = true) { … }` block, before the caller's `semantics`.
   - Compose emits a merging node's description as a fake first child (`SemanticsNode.kt:478-491`).
   - That child carries the card's clipped bounds (`SemanticsOwner.kt:183-194`), so it is exposed whenever any part of the card is.
3. **The hidden texts.** Every `Text` inside a card gets `Modifier.semantics { hideFromAccessibility() }`, through one private helper in `LicenseEntryCard.kt` that both callers use. Without this, TalkBack would read the label and then the visible texts a second time.
   - Hidden texts still merge into the card's merged config: `HideFromAccessibility`'s merge policy keeps the parent's value (`SemanticsProperties.kt:115-119`), and `mergeConfig` ignores visibility (`SemanticsNode.kt:265-279`).
   - They keep `GetTextLayoutResult`.
   - So the merged-tree oracles of LS-AC06 and the unmerged oracles of LS-AC19 and LS-16 keep their nodes.
   - `isHidden` reads the unmerged config (`SemanticsOwner.kt:140-147`), so the card itself is not hidden.
4. **One source for every string.**
   - `LicenseEntryCard` resolves the title and each line once into a `List<String>`, with `stringResource` and `pluralStringResource` as today. It renders one `Text` per item and passes `label = texts.joinToString(LicensesUi.CARD_LABEL_SEPARATOR)`.
   - `GoogleNoticesScreen`'s row does the same with its name, the "Found in" line and the ordinal.
   - No text is resolved twice.
5. **KDoc corrections.** Rewrite `LicensesScreen.kt:44-47` and the A1.3 tests' KDoc (`LicencesTalkBackContractTest.kt:145-149`). Both claim that being in the tree makes TalkBack reach every header. The new text says that being in the tree is necessary but not sufficient, and that the tests guard against lazy composition.
6. **Unchanged:**
   - the `Column`, the top bar and the title's `heading()`;
   - the scroll semantics, `collectionItemInfo`, `Role`, the click labels and the chevron;
   - `dimens.xml`, `strings.xml`, `libs.versions.toml`, `build-logic`, `ui/theme/`, `ui/home/`, `ui/game/`.

   No `@Suppress`, `@Ignore`, baseline or `!!` is added.

**Red first.** Each named test is watched red before the code, and the report records the red runs. Every string comes from resources and the catalogue, never a literal.
- **a.** `LicencesTalkBackContractTest.everyLicensesCardSpeaksItsWholeLabelAsItsOwnDescription`. For each of the 18 entries, the merged node's `ContentDescription` is exactly the title and its lines in order, joined by the separator. **Red:** no card has a description.
- **b.** `LicencesTalkBackContractTest.everyTextInsideALicensesCardIsHiddenFromAccessibility`. Each unmerged `Text` node under a card's node carries `HideFromAccessibility`. **Red.**
- **c.** In `GoogleNoticesScreenTest`, `everyRowSpeaksItsWholeLabelAsItsOwnDescription` and `everyTextInsideARowIsHiddenFromAccessibility`, with a fixture row that has an ordinal. **Red.**
- **d. The device-faithful oracle, one time-boxed attempt:** a new `LicencesAccessibilityExposureTest.aCardHalfAboveTheViewportIsExposedWithItsTitleFirst`.
  - **Setup.** The phone window `w411dp-h891dp`, and accessibility enabled through Robolectric's `ShadowAccessibilityManager`.
  - **Position.** Scroll with `ScrollBy` by an offset computed from the card's bounds, so that the card's title lies above the list's viewport and its last line inside it. No literal offset.
  - **Read.** Take the `AccessibilityNodeInfo` tree from the `AndroidComposeView`'s node provider.
  - **Assert.** The card's visible children's text and descriptions, joined, start with its title.
  - **Red today:** the title child is not visible to the user.
  - **The time box.** If Robolectric yields no provider or no nodes within one attempt, the developer reports that, and a–c and QA stand.
- **Existing tests whose oracle changes.** These edits are part of this ruling, not edits made to pass. The report lists each one and the criterion it keeps.
  - `theLicensesScreenReadsInTheContractsOrder`: `said()` now returns the card's label. The expected list becomes the labels in the same order, plus a per-entry assertion that the label starts with its title. That keeps LS-06's "read title first".
  - `theChevronCarriesNoDescription`: the chevron's own unmerged node has no `ContentDescription`, and the merged descriptions are exactly back plus the 18 labels.
  - Any `GoogleNoticesScreenTest` oracle that reads `ContentDescription`.
  - Any other test that goes red is reported with its criterion and not silently edited.

**Commits:**
1. `fix(licences): give each licence card its whole label at any scroll position`: a–d, the change, and the two oracle edits.
2. `docs(licences): say what the heading tests can and cannot prove`: point 5.
3. `docs(licences): record the android-game-dev F-4 loop`: the report.

The Check is green on every commit. Run it with `./scripts/check.sh`, the branch's Check since the C7 merge-in.

### R3 — the code-auditor
- **Scope:** the loop's diff from `f908571`. Q-019's mitigation stands: the auditor runs the Check itself, with the source tasks forced.
- **Probes:**
  1. Drop step 3 on one card, and test b goes red. Drop the label, and test a goes red.
  2. Each label is built from the same list that is rendered. A grep shows no second `stringResource` for a card text.
  3. LS-AC19, the LS-16 edge tests, and LS-AC06's merged-tree and address oracles are green and unedited, except the two named oracle edits.
  4. No literal separator, no new string, and no description on the chevron.
  5. The frozen paths are untouched.

### R4 — QA's re-check, F-4 only (the founder's reduced scope)
- **Setup.** The loop head, as a debug APK. Light mode at 100 %, TalkBack 16 with the reading control on Headings. Every stop is recorded.
- **The runs:**
  1. Down from the back button, 7 swipes.
  2. Up from the last entry, 6 swipes.
  3. With the list mid-scrolled: from the title, 3 swipes down; and from an entry in the middle, both ways (§8.2 item 2's form).
  4. A linear swipe forward from the title, with the list at its end, as in `tb-f4-linear-to-last.txt`.
  5. **R2's regression run:**
     - a fully visible Licenses card and a notices row are each read once, label first, with "Button" and the hint as before;
     - one linear run of about 15 swipes on the notices list, across one TalkBack scroll: each row is read once, name first, with its "k of" position.
- **Grading:**
  - **Pass line 2** is graded now. Every card stop in runs 1–5 is read whole, title first.
  - **Pass line 4** is graded against the ux-designer's R1 ruling. If that ruling is not in by QA's session, QA records the transcripts and grades them afterwards. If LS-15 stands as written, runs 1–3 are expected to fail as before, and the verdict says so. Do not repeat them hoping for another result.
  - **200 %** is not owed in this re-check, because R1's outcome decides what it would grade.

### R5 — the merge condition
The merge waits for four things:
1. the ux-designer's LS-15 ruling;
2. the auditor's PASS;
3. QA's PASS on pass line 2 and on pass line 4 as ruled;
4. my sign-off.

If the ux-designer keeps LS-15 and chooses E, that is a new design-and-build loop, and the producer decides whether it rides this branch or a follow-up.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/tech-lead-f4-ruling.md` — this report, committed alone. No code, spec or tech-design text was written.

## Checks
- `Read` in full:
  - `qa-engineer-recheck.md`;
  - `tech-lead-rerun-ruling.md` (with A1.1–A1.5);
  - `ux-designer-rerun.md`;
  - `LicensesScreen.kt`, `LicenseEntryCard.kt` and `LicensesTopBar.kt`;
  - `LicencesTalkBackContractTest.kt` lines 1–60 and 130–292.

  Pass.
- `cat` of every `tb-f4-*.txt` under `.planning/qa/licences-screen/recheck/` (6 files; `tb-f4-rep-focus-google-libs.txt` is empty, as QA says) — pass. The transcripts match the report.
- The spec, read in parts — pass:
  - `docs/ux/licences-screen.md` LS-15 (374–400), §5.1 "Headings are headings" (920–935), the pass line (1023–1056), §8.2 item 2 (1228–1240) and §9 item 18 (1430–1442);
  - LS-AC06 and LS-AC19 from `grep -n` output.
- `tech-lead-brief.md`, read only from `grep -n 'heading\|LazyColumn\|3\.4\|TalkBack'` output: §3.4, A1.1, A1.2 and A1.4 — pass.
- `unzip` of `ui-android-1.11.3-sources.jar` and `foundation-android-1.11.3-sources.jar` to the scratchpad, then reads of each passage below — pass. Each cause is as stated.
  - `SemanticsOwner.kt`: 140–147, 162–330 and 339–361;
  - the delegate: 635–745, 807–812, 1190–1220, 1494–1616 and 3430–3474;
  - `SemanticsNode.kt`: 95–106, 265–299 and 467–512;
  - `SemanticsProperties.kt`: 108–119 and 409–411;
  - `SemanticsUtils.android.kt`: 71–79;
  - `ComposeUiFlags.kt`: 121–125;
  - `Scrollable.kt`: 594–611 and 1147–1160.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.LicencesTalkBackContractTest' --console=plain -q` — pass: 11 of 11 green, 0 skipped, on the code QA failed on the device. The Compose oracle cannot see F-4.
- `./gradlew check` — not run. This ruling changes no code. The developer runs the Check on every commit, and the auditor runs it forced.

## Open issues / risks
1. **TalkBack's own halves are inferred.** These are the focus after an auto-scroll, and the absence of a scroll from a focus outside the list. TalkBack's source was not available here. The Compose halves are verified, and the inferences match all four transcripts.
2. **R2 relies on Compose's fake-description child** and on TalkBack reading a visible child's description in place of hidden texts. That is the common `mergeDescendants` and `contentDescription` pattern, but the device decides. If QA hears a card twice, or hears nothing, it is a finding that returns to me, with R2's test d as the oracle to extend.
3. **LS-15 is open until the ux-designer rules.** Under F, F-4 closes with this loop's QA pass. Under E, it needs a redesign loop.
4. **Settings at 200 % is expected to show the same pattern.** That is an inference, not observed. It bears on R1's consistency argument. A single QA run there is optional and is the producer's call.
5. **The tech design (§3.6) still lacks T4, the F-2 wrapper and now R2.** That is a docs sync for the producer to schedule.
6. **Staffing (Q-019):** this ruling ran on Opus, the author's model.

## Handoff
Next: producer —
1. **ux-designer:** rule LS-15 with R1's options (F recommended, E the only exact fit). Also update pass line 2's note, "LS-15's fix is expected to remove it", which R2 now covers. Inputs: this report, `qa-engineer-recheck.md` and the `tb-f4-*.txt` transcripts.
2. **android-game-dev, in parallel:** R2, test-first, as one loop. Required skills: `compose-skill` first, then `compose-ui`, `android-testing` and `superpowers:test-driven-development`. Inputs: this report and `qa-engineer-recheck.md`.
3. **code-auditor, then qa-engineer:** R3, then R4.
4. **Tech-lead:** the sign-off once both PASS and the ux-designer has ruled.
