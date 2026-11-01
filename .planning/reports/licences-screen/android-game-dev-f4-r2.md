# android-game-dev report — licences-screen (ruling R2: the title-less card fix)
Date: 2026-11-01
Branch: feature/licences-screen (455ef4a..f11aebe)
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing were read in full; logging, puzzle-design and play-store are identical to the snapshot in context; `.claude/rules/lessons.md`), superpowers:test-driven-development, compose-skill, compose-ui, android-testing, gitflow
Ran in parallel with: ux-designer LS-15 ruling, candy 15m oracle build
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- R2 is built as ruled. `LicenceCard` takes a required `label`, which is its merged node's `contentDescription`. Every text inside a card is hidden from accessibility. The label and the visible texts come from one resolved list, on the Licenses cards and on the notices rows.
- Every named test was watched red before the code: a, b, the two notices tests, the two ruled oracle edits, and test d. Test d is device-faithful, and it reproduces QA's stray stop on the JVM.
- Test d needed two changes before it worked: native graphics (D3), and Android's public query-from-app API to read a card's children (D4). It went red with "but was: Apache License 2.0. Gson, Tink, Guava, …", the Gson card read from below its title.
- The Check is green at both work commits, with 1,364 `:app` tests and 0 failed. No other existing test changed, and no frozen path changed. LS-15 was not touched.

## Outputs
- `455ef4a` fix(licences): give each licence card its whole label at any position
  - `app/src/main/java/com/app/pegsolitaire/ui/licenses/LicensesUi.kt`: `CARD_LABEL_SEPARATOR = ". "`, with its KDoc.
  - `…/ui/licenses/LicenseEntryCard.kt`:
    - `LicenceCard(label)` sets `contentDescription = label` inside its `semantics(mergeDescendants = true)` block, before the caller's `semantics`.
    - `LicenceCardTexts(texts)` renders one `Text` per item, each with `hideFromAccessibility()`. The first item is the title (`titleMedium`, `onSurface`) and the rest are lines (`bodyMedium`, `onSurfaceVariant`).
    - `licenseLineText(line)` resolves a line to its `String`.
    - `LicenseEntryCard` resolves `texts` once and passes `texts.joinToString(CARD_LABEL_SEPARATOR)`.
  - `…/ui/licenses/GoogleNoticesScreen.kt`: the row resolves its name, its "Found in" line and its ordinal once, and passes the same list to `LicenceCardTexts` and to the label.
  - Tests:
    - `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesTalkBackContractTest.kt`: +2 tests and 2 ruled oracle edits.
    - `…/GoogleNoticesScreenTest.kt`: +2 tests.
    - New `…/LicencesAccessibilityExposureTest.kt`: test d.
- `f11aebe` docs(licences): say what the heading tests can and cannot prove. This is point 5.
  - Rewrites the `LicensesScreen.kt` KDoc on the `Column`, and the A1.3 tests' KDoc.
  - Both now say that being in the semantics tree is necessary but not sufficient, and that the tests guard against lazy composition.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/android-game-dev-f4-r2.md`: this report, committed alone after `f11aebe`.

## The red runs, quoted
Every run below was `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.LicencesTalkBackContractTest' --tests 'com.app.pegsolitaire.ui.licenses.GoogleNoticesScreenTest' --tests 'com.app.pegsolitaire.ui.licenses.LicencesAccessibilityExposureTest' --max-workers=4 --console=plain`. The only production change in place was the separator constant (D6).

**Recorded red: 21 tests, 7 failed, every one on its assertion.**
- **a.** `everyLicensesCardSpeaksItsWholeLabelAsItsOwnDescription`:
  > "fredoka: its description — missing (1): Fredoka. © 2016 The Fredoka Project Authors. SIL Open Font License 1.1 — expected: [Fredoka. © 2016 …] but was: []"
- **b.** `everyTextInsideALicensesCardIsHiddenFromAccessibility`:
  > "fredoka: whether "[Fredoka]" is hidden — expected to be true"
- **c.** `GoogleNoticesScreenTest`:
  - `everyRowSpeaksItsWholeLabelAsItsOwnDescription`:
    > "Kotlin (000000000000000a): its description — missing (1): Kotlin. Found in ads-mobile-sdk, hsdp. Notice 1 of 2 — but was: []"
  - `everyTextInsideARowIsHiddenFromAccessibility`:
    > "000000000000000a: whether "[Kotlin]" is hidden — expected to be true"
- **d.** `LicencesAccessibilityExposureTest.aCardHalfAboveTheViewportIsExposedWithItsTitleFirst`:
  > "what the card's visible children say, in order — expected to start with: Gson, Tink and Guava — but was: Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations"
- **The ruled oracle edits:**
  - `theLicensesScreenReadsInTheContractsOrder`:
    > "missing (18) — #1: Fredoka. © 2016 The Fredoka Project Authors. SIL Open Font License 1.1 — #2: Nunito. …"
  - `theChevronCarriesNoDescription`: "missing (18)" with the same 18 labels. Its chevron half passed: the chevron is decorative today.

**Earlier attempts, before the recorded red:**
- **a and c** errored with `NullPointerException`, because Truth was given a null list. They now assert on `.orEmpty()` (D5).
- **d** failed first on its own guard: "IllegalStateException: the card is not exposed".
  - Under Robolectric's legacy graphics, the provider exposed no virtual id from 0 to 400.
  - Under native graphics (D3), the card (id 104) is exposed. Its children:
    - the title (108) is not exposed;
    - the credit "Google" (109) is exposed but not visible to the user;
    - "Apache License 2.0" (110) and the parts line (111) are visible.

  That is the ruling's Layer 1, measured. It is also QA's stray stop.
- **d's position guards passed on every run.** The title's bottom was at or above the viewport's top (64 px). The last line lay inside the viewport.

**Green after the change.** `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.*' --tests 'com.app.pegsolitaire.navigation.LicensesNavFlowTest' --tests 'com.app.pegsolitaire.ui.ButtonSoundTest' --max-workers=4 --console=plain` passes: 117 tests, 0 failed. With the label in place, d reads the card's first visible child, its description, and so says the title first.

## Existing tests whose oracle changed (as ruled)
- **`theLicensesScreenReadsInTheContractsOrder`** (LS-AC06 order, LS-06 "read title first"). Each card's expected stop is now its label: title and lines from the resources, joined by the separator.
  - The order is unchanged: back, title, intro, then each section's header over its cards' labels.
  - A per-entry assertion checks that what each card's stop says (`said(entryNode(entry))`) starts with its title.
  - The assertion reads the screen, not the test's own expected label (D7).
- **`theChevronCarriesNoDescription`** (§5.1, the chevron is decorative).
  - Each of the 14 chevrons' own unmerged nodes, found by `LICENCE_CARD_CHEVRON_TAG`, has no `ContentDescription`. The count is checked, so the test cannot pass on zero chevrons.
  - The merged descriptions are exactly the back button plus the 18 labels.
- **`GoogleNoticesScreenTest`**: no existing oracle reads `ContentDescription`, so none was edited.
- **No other test went red** in the targeted runs or in either Check. The tests kept unedited:
  - LS-AC06: `inertEntriesAreOneMergedNodeWithNoClickActionAndNoRole`, merged `Text` with lines + 1;
  - the address test;
  - LS-AC19's and LS-16's RTL oracles, which read `GetTextLayoutResult` on the hidden texts;
  - LS-AC10 and LS-AC13;
  - `LicensesNavFlowTest` and `ButtonSoundTest`.

  Hidden texts still merge into the card and keep their layout result, as the ruling says.

## Departures, each for the reviewers
- **D1: `LicenceCardTexts` is `internal`, not `private`.** The ruling asks for "one private helper in LicenseEntryCard.kt that both callers use". But the notices row is in `GoogleNoticesScreen.kt`, and a Kotlin top-level `private` is file-private. It is still the single helper both callers use, and it also holds the two text styles the cards already shared.
- **D2: Commit 1's subject** is shortened from the ruled 76 characters to 69 ("at any scroll position" became "at any position"), within rules/gitflow.md's 72.
- **D3: Test d runs under `@GraphicsMode(NATIVE)`.**
  - Compose's `SemanticsRegion` is backed by `android.graphics.Region`, which is a stub under legacy graphics.
  - So the uncovered-nodes map was empty, and the provider exposed nothing.
  - `TimerDigitsTest`'s provider test already runs native.
- **D4: How test d reads the children.**
  - The children are read through the public API-34 `AccessibilityNodeInfo.setQueryFromAppProcessEnabled(host, true)` and then `getChild(i)`. This is the platform's path for testing tools.
  - Robolectric 4.16 then uses the real `AccessibilityNodeInfo`. It answers through the view root's direct connection on the same thread, and the real `enforceSealed` lets a direct connection through.
  - This reaches the description child, whose id is not in the semantics test API. There is no reflection, and the shadow's `@Resetter` clears the mode after the test.
  - Accessibility is on through `ShadowAccessibilityManager`: enabled, touch exploration on, one enabled service. The window is `w411dp-h891dp`.
  - The scroll is `ScrollBy` by an offset computed from the card's bounds: the viewport's top lands halfway between the title's bottom and the last line's top. There is no literal offset.
- **D5:** Tests a and c assert on `.orEmpty()`, so a missing description fails as an assertion, not as Truth's NPE.
- **D6:** `CARD_LABEL_SEPARATOR` was added before the tests, so the red runs fail on assertions, not on compilation. It adds no behaviour.
- **D7:** See the oracle edits above.
- **D8: Test d's card** is Gson, Tink and Guava, the card QA heard read without its title (`tb-f4-down-from-title.txt`). It sits mid-list, so the computed scroll is never clamped.

## Checks
- `./gradlew :app:testDebugUnitTest --tests '…LicencesTalkBackContractTest' --tests '…GoogleNoticesScreenTest' --tests '…LicencesAccessibilityExposureTest' --max-workers=4 --console=plain` — fail, as intended: the red run, 21 tests and 7 failed, quoted above.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.licenses.*' --tests 'com.app.pegsolitaire.navigation.LicensesNavFlowTest' --tests 'com.app.pegsolitaire.ui.ButtonSoundTest' --max-workers=4 --console=plain` — pass (117 tests, 0 failed).
- `./gradlew :app:ktlintCheck :app:detekt --max-workers=4 --console=plain`:
  - first run — fail: `ktlintTestSourceSetCheck`, `LicencesAccessibilityExposureTest.kt:15:1 Unused import`. That was `assertThat`, which I removed.
  - second run — pass.
- `./scripts/check.sh --wait --max-workers=4 --console=plain` on the `455ef4a` tree — pass.
  - Exit 0 in 669 s ("BUILD SUCCESSFUL in 11m 8s").
  - `:app` ran 1,364 tests, 0 failed, 0 skipped.
  - The script runs `./gradlew check` under the worktree lock. It is the ruling's Check command, and the dispatch's `./gradlew check`.
- `./scripts/check.sh --wait --max-workers=4 --console=plain` on the `f11aebe` tree — pass (exit 0 in 672 s; 1,364 tests, 0 failed, 0 skipped).
  - Another agent overwrote my log file in the shared scratchpad while this ran.
  - So the exit is read from my own background task's output, and the counts from this worktree's `app/build/test-results/testDebugUnitTest`, whose newest XML was written during this run.
- `git diff --name-only e406169 f11aebe` — pass. Only `ui/licenses/` sources and tests changed: none of `dimens.xml`, `strings.xml`, `libs.versions.toml`, `build-logic/`, `ui/theme/`, `ui/home/` or `ui/game/`.
- `git diff e406169 f11aebe | grep -E '^\+.*(@Suppress|@Ignore|!!|baseline)'` — pass (none).
- `git diff e406169 f11aebe -- app/src/main | grep '". "'` outside the constant — pass (none).
- `grep -n 'stringResource\|Text(' LicenseEntryCard.kt`, and the same on `GoogleNoticeRowCard` — pass.
  - Each card text is resolved once, into `texts`.
  - The only `Text(` in either card is `LicenceCardTexts`'.
  - The chevron keeps `contentDescription = null`.

## Open issues / risks
1. **The device decides the speech.** R2 relies on TalkBack reading the description child in place of the hidden texts. Test d proves that Compose exposes the label first, with the card's bounds, at a straddling scroll. TalkBack's speech is still QA's to hear in R4 run 5. If a card is heard twice or not at all, it goes back to the tech-lead, with test d as the oracle to extend (the ruling's open issue 2).
2. **Test d trusts Robolectric.** It is a JVM reading through Robolectric's real-ANI path, so it holds only as long as Robolectric's direct-connection emulation stays faithful to the platform.
3. **One more KDoc overstates the tree.** `LicencesTalkBackContractTest`'s class KDoc still calls the merged semantics tree "the tree TalkBack reads". Point 5 named only the two passages I rewrote, so I left it. It is a one-line follow-up, if the tech-lead wants it.
4. **Commit trailers.**
   - The plugin's 1.0.0 gitflow rule asks for one Fable trailer, added by a prepare-commit-msg hook. No hook is installed in this repo.
   - I followed the session's attribution lines, as the ux-designer's `e406169` did.
   - Which trailer applies is the producer's or the release-manager's call.
5. **Shared scratchpad.** The concurrent agents share one session scratchpad, and a fixed log name like `check2.log` was overwritten mid-run. Candidate retro lesson: give each agent its own log names.
6. **Not mine:** the tech design §3.6 docs sync (the ruling's open issue 5).

## Handoff
Next:
- **code-auditor**: R3 on the diff from `f908571` (code `455ef4a..f11aebe`; `e406169` is the ux-designer's docs commit). Run the Check forced, the probes 1–5, and the departures D1–D8. D3 and D4 are the ones to read closely.
- **qa-engineer**: R4 on a debug APK built at this branch's head. Run 5 is R2's regression run.
- **tech-lead**: the sign-off, once the auditor and QA pass and the LS-15 ruling is in.
