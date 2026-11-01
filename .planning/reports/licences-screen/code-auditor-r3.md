# code-auditor report — licences-screen (R3: review of ruling R2's implementation)
Date: 2026-11-01
Branch: feature/licences-screen (f908571..595e2a7; code 455ef4a..f11aebe, e406169 the ux-designer's docs-only LS-15 ruling) — report text returned to the producer to commit
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing were read in full; logging, puzzle-design and play-store are identical to the snapshot in context; `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: talkback QA, colour-vision reviews, candy auditor
Ran on: claude-opus-5-5 (the harness names no effort; the pin is xhigh). **Deviation:** my agent file pins fable. The founder's Q-019 answer (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published, so Opus reviewed Opus code. Q-019's mitigation applies: I ran the Check myself with the `:app` source tasks forced, and I ran every probe and mutation myself. I trusted no recorded result.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

Producer note: the harness refused this agent's report-file write; the producer saved its returned text here verbatim.

## Summary
- R2 is built as ruled.
  - `LicenceCard` takes a required `label`, set as the merged node's `contentDescription` before the caller's `semantics`.
  - Every card `Text` carries `hideFromAccessibility()` through one shared helper.
  - The label and the visible texts come from one resolved list, on the Licenses cards and on the notices rows.
  - The separator is a constant, `LicensesUi.CARD_LABEL_SEPARATOR`.
- I re-ran the Check with the `:app` tests, detekt, ktlint and lint forced: green, 1,364 `:app` tests, 0 failed, 0 skipped.
- In a throwaway worktree I reproduced the red of test d (word for word as the developer quoted it), of a, of c, of b and c's hidden halves, and of both ruled oracle edits.
- A probe shows that with the fix, the exposed accessibility tree speaks exactly the label, once and title first.
- Two Minor findings (test d's "read once" gap; one stale class KDoc) and three Info notes. Nothing blocks.

## Outputs
- This report (returned as text; the producer commits it at `.planning/reports/licences-screen/code-auditor-r3.md`).
- Throwaway worktree `…/scratchpad/lic-r3/wt` at `595e2a7`. I created it for the mutations and removed it afterwards (`git worktree list` shows none). The branch worktree is unchanged and clean.

## Checks
- `uptime` polled before every Gradle run. Each run started with the 1-minute load below 12 (7.64, 10.27, 9.73, 8.71).
- `./scripts/check.sh --wait --max-workers=4 --console=plain :app:testDebugUnitTest --rerun :app:detektMain --rerun …` — **fail, my error, not the branch.**
  - `:app:detektMain` is not part of `check`. It pulled in `:app:detektDebug`, the type-resolution variant, which failed with 54 issues in older files: `SnapshotMapper`, `di/*`, `BoardCanvas`, `ui/game/*`, `ui/home/*`, `ui/results/*`, `SettingsScreen`. None is in `ui/licenses/`.
  - I discarded that run and re-ran with the Check's own tasks.
- `./scripts/check.sh --wait --max-workers=4 --console=plain :app:testDebugUnitTest --rerun :app:detekt --rerun :app:runKtlintCheckOverMainSourceSet --rerun :app:runKtlintCheckOverTestSourceSet --rerun :app:lintDebug --rerun` — **pass.**
  - "BUILD SUCCESSFUL in 11m 10s", exit 0.
  - `:app:detekt`, `:app:lintDebug`, both ktlint runs and `:app:testDebugUnitTest` executed fresh.
  - Test XMLs: 1,364 tests, 0 failed, 0 errors, 0 skipped, and no XML older than the run.
  - `LicencesTalkBackContractTest` 13/13, `GoogleNoticesScreenTest` 7/7, `LicencesAccessibilityExposureTest` 1/1.
- The mutation runs below all used `./gradlew :app:testDebugUnitTest --tests '…LicencesTalkBackContractTest' --tests '…GoogleNoticesScreenTest' --tests '…LicencesAccessibilityExposureTest' --max-workers=4`, in the throwaway worktree.
  - **M1** (`contentDescription = label` → `if (label.isEmpty()) contentDescription = label`) — fail as expected: 21 tests, 5 failed.
    - `everyLicensesCardSpeaksItsWholeLabelAsItsOwnDescription`: "fredoka: its description … but was: []".
    - `everyRowSpeaksItsWholeLabelAsItsOwnDescription`: "Kotlin (000000000000000a) … but was: []".
    - `theLicensesScreenReadsInTheContractsOrder`: "missing (18)".
    - `theChevronCarriesNoDescription`: "missing (18)".
    - `aCardHalfAboveTheViewportIsExposedWithItsTitleFirst`: "expected to start with: Gson, Tink and Guava, but was: " (empty; the texts are hidden and there is no label).
  - **M2** (the `hideFromAccessibility()` modifier removed) — fail as expected: 21 tests, 2 failed, `everyTextInsideALicensesCardIsHiddenFromAccessibility` and `everyTextInsideARowIsHiddenFromAccessibility`. **Test d stays green** (Minor finding 1).
  - **M1 + M2** (the pre-fix state), test d only — fail. This reproduces the developer's red exactly: "expected to start with: Gson, Tink and Guava, but was: Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations".
  - **Probe P1**: test d's `.startsWith(title)` changed to `.isEqualTo("PROBE")`, on the real fix. The exposed speech is "Gson, Tink and Guava. Google. Apache License 2.0. Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations": exactly the label, once, title first.
  - **Probe P2**: P1 plus M2. The speech is the label followed by "Apache License 2.0. Gson, Tink, Guava, …" a second time.
- R3 probe 1 — pass. Dropping step 3 turns b (and c's hidden half) red (M2). Dropping the label turns a (and c, d and both oracle edits) red (M1).
- R3 probe 2 — pass.
  - `LicenseEntryCard.kt:63` resolves the title and the lines once into `texts`. Labels: `:67`. Render: `:70`.
  - `GoogleNoticesScreen.kt:129-134` resolves the name, "Found in" and the ordinal once. Label: `:138`. Render: `:145`.
  - The only other `stringResource` there is the click label (`:137`), not card text.
- R3 probe 3 — pass. `git diff --name-only f908571..595e2a7` touches 3 test files.
  - In `LicencesTalkBackContractTest`, the only edits are the two named oracles (`theLicensesScreenReadsInTheContractsOrder`, `theChevronCarriesNoDescription`), the two new tests, new helpers, and point 5's KDoc on the A1.3 test.
  - In `GoogleNoticesScreenTest`, the only edits are the two new tests and their helpers.
  - The LS-AC19, LS-16 and LS-AC06 merged-tree and address oracles are unedited and green in the forced run.
- R3 probe 4 — pass.
  - `". "` appears in `app/src/main` only in `LicensesUi.kt:36`, the constant.
  - `res/` is untouched (no new string).
  - The chevron keeps `contentDescription = null` (`LicenseEntryCard.kt:133`), and `theChevronCarriesNoDescription` now asserts that on each of the 14 chevrons' own nodes, with a count check.
- R3 probe 5 — pass. `git diff f908571..595e2a7 -- app/src/main/res gradle build-logic ui/theme ui/home ui/game` is 0 lines.
- `git diff f908571..595e2a7 | grep -E '^\+.*(@Suppress|@Ignore|!!|baseline)'` — pass: matches only report prose. No suppression to approve or reject.
- THIRD_PARTY.md — not applicable: no dependency added (`libs.versions.toml` unchanged).
- Security — pass: no network, no secrets, no new input path. UI-semantics change only.

## Findings
No Critical or Major findings.

**Minor**
1. `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesAccessibilityExposureTest.kt:88-89` — test d does not guard "read once".
   - With `hideFromAccessibility()` removed (M2), the exposed speech reads the card's two visible lines a second time after the label (P2), and d stays green, because it asserts only `startsWith(title)`.
   - The developer followed the ruling's wording, so this is a gap in the oracle, not a defect in the work.
   - The ruling's open issue 2 names d as the oracle to extend. P1 shows that on the real fix the exposed speech equals the label exactly. Asserting `isEqualTo(label)` would make d guard pass line 2's "once, whole, title first" end to end.
   - Today b and c guard only that the flag is set, not what it does to the tree TalkBack reads.
   - For the tech-lead to decide. Not blocking.
2. `app/src/test/java/com/app/pegsolitaire/ui/licenses/LicencesTalkBackContractTest.kt:30-31` — the class KDoc still calls the merged semantics tree "the tree TalkBack reads".
   - The ruling's verified Layer 1 contradicts this.
   - So does the new `LicencesAccessibilityExposureTest.kt:27`, which names the node provider's tree as the one TalkBack reads.
   - It is outside point 5's two named passages. The developer disclosed it (open issue 3). One-line follow-up.

**Info**
- I-1. `GoogleNoticesScreenTest.kt:109, 121` — `zip(ROWS)` with no row-count assertion would pass vacuously if rows stopped rendering. It is not vacuous today: M1 failed on row 1, and the 4-row fixture is composed whole in the tall window. A `hasSize(ROWS.size)` before the zip would close it.
- I-2. `LicenseEntryCard.kt:63,67` and `GoogleNoticesScreen.kt:129-138` — the `texts` list and `joinToString` are allocated on every composition of a card. The cards are static and the strings short, so the cost is negligible (compose-performance-audit). No action.
- I-3. Commit trailers. Every commit in range carries `Co-Authored-By: Claude Opus 5.5` and `Claude-Session:`. Plugin 1.0.0's gitflow asks for one Fable trailer only; the 0.6.2 snapshot does not. The producer's or release-manager's call (developer's open issue 4).

**Departures D1–D8** — each accepted.
- D1: `internal` is required for a helper shared across two files. It is still the one helper.
- D2: the subject is 69 characters, within 72.
- D3: `@GraphicsMode(NATIVE)` has precedent in `TimerDigitsTest` and the `ui/home` tests, and the reason (Compose's `SemanticsRegion` needs a real `android.graphics.Region`) is consistent with my runs: d exposes nodes and goes red and green as expected.
- D4: `setQueryFromAppProcessEnabled` is the public API-34 path, with no reflection. The full suite (1,364) stays green with d in it, so no cross-test leak showed.
- D5: `.orEmpty()` makes a missing description fail as an assertion; the M1 messages confirm it.
- D6: the constant adds no behaviour.
- D7: the per-entry title assertion reads the screen.
- D8: Gson is mid-list, so the scroll is never clamped, and d's three position guards held in every run.

## Open issues / risks
1. TalkBack's actual speech is still QA's to hear (R4 run 5). P1 shows that Compose's exposed tree gives exactly the label. Whether TalkBack then speaks it once is device behaviour.
2. Minor 1, if taken up, is a small test edit for the developer, scheduled by the tech-lead.
3. Staffing: Opus reviewed Opus code under Q-019. The re-run Check and the mutations are the mitigation.
4. The 54 `detektDebug` issues are outside this diff and are not run by `check`. Recorded only because my mistaken first run surfaced them. No action asked of this loop.

## Handoff
Next: qa-engineer — R4 on a debug APK at `595e2a7` (run 5 is R2's regression run), graded by `ux-designer-ls15.md`'s table. Then the tech-lead's sign-off (R5). The tech-lead decides whether Minor 1 and Minor 2 ride this branch or a follow-up.

VERDICT: PASS
