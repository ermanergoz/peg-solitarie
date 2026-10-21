# tech-lead report — phase-15 ux-refresh: closing the HOLD (H-1 to H-4), sign-off for the merge into develop
Date: 2026-10-21
Branch: feature/phase-15-ux-refresh (committed by the producer). Reviewed at `06f1a0b`. The branch's code head is `af08b86`, and `b6a6230..06f1a0b` adds records only.
Skills loaded: ergoz-soft:standards (role tech-lead). The product's snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch. Also the product's `.claude/rules/lessons.md`. Then android-architecture, android-gradle-logic.
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort). **Staffing deviation:** the tech-lead is pinned to fable. This ran on Opus under the founder's Q-019 (2026-10-17: "Opus for every Fable-pinned role until 3.0.0 is published"), in `.planning/questions/FOUNDER.md`.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/15-founder-feedback/tech-lead-signoff.md`, `android-game-dev-h2.md`, `code-auditor-loop4.md`, `android-game-dev-h1-fix.md`, `code-auditor-loop5.md`, `qa-engineer-15e.md`
Lessons applied: none (the snapshot holds 0 active lessons, and `.planning/recall/RECALL.md` does not exist)

VERDICT: PASS

## Summary
- **PASS: phase 15 may merge into develop.** H-1 to H-4 have closed as my HOLD wrote them (table below). This sign-off ran on Opus, not the pinned Fable, under Q-019.
- **D-1 is accepted.** QA made the bench AVD by copying the Pixel_9a's own files instead of running `avdmanager`. That route meets P-1.2, which is the property P-1.1 existed to produce. I reproduced the diff myself. Later sittings must record that diff again.
- **The INFO items and 15e O-1 are routed with owners.** None of them is a merge condition. None may land on this branch: each one that touches a start-path file goes to a branch that runs its own 15e sitting.
- **Docs still owed:** the tech-lead owes the `docs/tech-design.md` edits on `feature/phase-14-release-docs`. The ux-designer owes the `docs/ux-spec.md` folds: my sign-off's Decision 1 text is still unapplied, and this report adds three items. The text for both is below.

## The four conditions: closed
| Condition | Closes on (sign-off) | Evidence | Closed |
|---|---|---|---|
| **H-2**: F-2 and G-1, test first | the developer's delta, audited in H-1 | `android-game-dev-h2.md` (`a62194f`, `3b27012`). Audited in `code-auditor-loop4.md` § H-2's delta, where M7, M8, M8b and thresholds 1, 2 and 4 all turn red | yes |
| **H-1**: S10, H-2's delta and the merge-in, audited | a PASS, with the Check green and every task executed | Loop 4 (`code-auditor-loop4.md`): S10's production code is sound, MB-1 and A-10 are closed, the four named mutations turn red, the merge `ee16a2a` resolved no code hunk, and the Check is green 98/98. It FAILED on three test-only defects, F-1 to F-3. The fix (`887c97d..af08b86`, two test files) was re-audited in loop 5 (`code-auditor-loop5.md`): **PASS**, every new assertion red under its mutation, and the Check green with `--rerun-tasks`, 98/98 executed on `b6a6230` (app 1,067, domain 274, depthSweep 1, build-logic 54; 0 failed, 0 skipped) | yes |
| **H-3**: 15e on `Pixel_9a_bench`, then F-2 on the device, the removal cue and the light shots | 15e PASS, every device check PASS, the shots recaptured | `qa-engineer-15e.md`; `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, "the sitting of record". 15e: delta 33.0 ms, floor 3.0 ms, threshold 100 ms, preconditions 1–9 held (`Data-Free` 13.9 / 13.8 GB). Arm A `c3d3437` = the merge-base; arm B `ee16a2a` = the code head carrying H-2 and the merge-in. F-2: PASS on six screens in both themes, with arm A as the positive control (1.04:1). The navigation bar is pixel-identical between the arms in both modes and both themes. The removal cue: Jump, then PegRemove 63 ms later in gameplay and 68 ms later in the tutorial. All eight light shots were recaptured | yes, with D-1 ruled below |
| **H-4**: the founder sees the 16 shots | done (not an approval gate) | `STATUS.md` In flight, resume order item 1: "H-4 is done (screens page v2, light shots retaken …)". FOUNDER.md holds no objection: Q-020 to Q-022 are about candy and the store copy | yes |

**The sitting stands on the head.** The order of events was H-2, then the develop merge-in, then loop 4, then the sitting, then the test-only fix, then loop 5. That is the order my HOLD allowed: H-2's production delta was audited before the sitting, and an H-1 test-only follow-up may run "before or after the sitting, but never during it". Nothing ran during it: the fix's Check started on 2026-10-21, three days after the sitting. Since arm B, `git diff ee16a2a HEAD` outside `.planning/` touches only `HomeLayoutTest.kt` and `HomeTestFixture.kt`. No start-path file changed, and nothing under `app/src/main` or `domain` changed. A test-only commit leaves the sitting standing (Decision 1, "What stays valid"), and the founder's light shots still show the APK that will merge.

## Ruling D-1: a bench AVD written from the Pixel_9a's own files is accepted
**Accepted. No re-run is owed, now or for later sittings.**

**Why the route does not matter.** P-1.1, the `avdmanager` command, was only a means. P-1.2 states the property it was meant to produce: the bench must carry the Pixel_9a's hardware profile and system image, and only the name, the paths and `disk.dataPartition.size` may differ. QA's route reaches that state more directly than the literal one would have. `avdmanager` writes its defaults from the device profile, and then each key has to be edited to the 9a's value. A copy of the 9a's own `config.ini` starts at those values.

**I re-checked the instrument myself**, read-only. I touched no emulator.
- `diff Pixel_9a.avd/config.ini Pixel_9a_bench.avd/config.ini` shows exactly three keys: `AvdId`, `avd.ini.displayname`, and `disk.dataPartition.size` (6G → 16G).
- The two `.ini` files differ only in `path` and `path.rel`. Both target `android-36.1`.
- The bench's `config.ini` still carries the values Decision 1 named: `image.sysdir.1` `android-36.1/google_apis_playstore_ps16k/arm64-v8a/`, `hw.device.name=pixel_9a`, 4 cores, 2048 MB RAM, a 228 MB heap, 1080×2424 at 420, and `hw.gpu.mode=auto`.
- **No key points into `Pixel_9a.avd`.** The only absolute path is `skin.path`, the SDK's own read-only skin directory. So the bench shares no image with the 9a.
- **No state was copied.** The bench's `userdata-qemu.img` base is 15.7 MB, written at 01:38 on first boot. The 9a's is 6.4 MB, dated April 23. `config.ini`'s mtime is still its creation time.
- **One difference in the file set, not in the configuration:** the 9a has a 512 MB `sdcard.img` and the bench has none. P-1.1's literal command had no `-c` flag, so `avdmanager` would not have made one either. The app reads no external storage (`grep` over `app/src/main` finds no external-storage API or permission), and both arms run on the same bench. So the absence cannot move the relative delta.

**Conditions for later sittings** (candy-backdrop, and F-1 if it carries one):
1. The bench is built once, so P-1.1 to P-1.4 are not repeated.
2. Each sitting records the P-1.2 diff again, and both `Data-Free` readings, in its own evidence. A diff with any extra key makes that sitting VOID.
3. The bench stays reserved for 15e sittings. It is still running at `emulator-5554` with arm B installed and the ladder seeded (`qa-engineer-15e.md`, Open issues). The producer may shut it down: the next sitting cold-boots it anyway (P-1.8).

**Precondition 5, as QA read it, is accepted.** A debuggable APK stays at `[status=verify]` after `compile -m speed -f`. Both arms read the same status, and equal ART state is what the precondition protects. The per-swap `dexopt` column in QA's driver read the wrong package and was not used. The next sitting's qa-engineer fixes that helper before running it (git-ignored, never committed).

## INFO items, open issues and 15e O-1: owners and where they land
**Rule for all of them.** None of these items lands on `feature/phase-15-ux-refresh`. Whether a comment-only edit to a start-path file leaves a sitting standing is not ruled here. Each item goes to a branch that already reshapes Home, and that branch's own QA decides whether it needs a sitting.

| # | Item (source) | Ruling | Owner, branch | Deadline |
|---|---|---|---|---|
| 1 | The pixel read cannot see a glyph drawn as **text**; mutation M3d stayed green (`code-auditor-loop5.md`, INFO 1) | Harden: the compact pill's `textsWithin()` must be exactly `[label, value]`, and must be watched red against M3d. F-2 as filed is closed: the shipped glyphs are `ImageVector` icons, and M3b/M3c are red | android-game-dev (test-only), on `bugfix/home-chip-arrival` (it edits `HomeLayoutTest` anyway) | before `release/3.0.0` |
| 2 | Two assertions in `assertTheFoldLeavesNoFalseBottom` cannot fail (loop 5, INFO 2) | Accepted as harmless. No action is owed; the F-1 developer may remove them when touching the helper | — | — |
| 3 | At ≥ 400 dp the full pill is guarded only by accident: M5c is caught only by `HomeStageTest`, by 1 dp (loop 5, INFO 3) | Harden: assert the full pill's glyph ink at `w412dp-h915dp`, the inverse of item 1's read. The developer's probe already measured 196 px and 85 px there (`android-game-dev-h1-fix.md`, Measurements). It must be watched red against M5c | android-game-dev (test-only), on `bugfix/home-chip-arrival` | before `release/3.0.0` |
| 4 | Stale KDoc in production: `HomeScreen.kt:272-277` and `:350-358` (the retired 15h claim, "~72dp over"), the compact preview's KDoc, and `HomeBandSet.kt:45` ("under a full hero") (loop 4 INFO, carried in loop 5) | Fix the text to the measured truth: 0 dp over at 360×640 on NATIVE and on the device. These lines are in `ui/home/`, a start-path directory, so the edit must not land on this branch after the sitting | android-game-dev, on `bugfix/home-chip-arrival` (its fix edits `ui/home/` anyway) | before `release/3.0.0` |
| 5 | `HomeStageTest`'s class KDoc says LEGACY under-wraps; it over-wraps (loop 4 INFO) | Fix the comment | android-game-dev, on the same branch | before `release/3.0.0` |
| 6 | The `ExperimentalCoroutinesApi` opt-in warnings, 244 `w:` lines in the Check (loop 4 INFO) | Folded into the warnings-as-errors ruling I already owe (`tech-lead-signoff.md`, deferred items) | tech-lead | before `release/3.0.0` (unchanged) |
| 7 | Item 5, the trailing-gap reading of 15k part 2 (`android-game-dev-h1-fix.md`, open issue 1; loop 5 § Item 5) | **I adopt the code-auditor's ruling.** "Overflows" means overflow into a band. The trailing gap and the bottom padding are not bands (§4.2.2 clause 5). The 16 dp tolerance is exact: M-edge120 passes and M-edge121 is cut. No ux-designer ruling is owed; the spec records the reading (fold c below) | ux-designer (records it) | with the spec fold |
| 8 | The pixel read assumes the light, flat pill surface (`android-game-dev-h1-fix.md`, open issue 3) | If candy direction B changes the ribbon pill's surface, `assertEachPillPaintsOnlyItsSurfaceAndItsTexts` changes with it, and is watched red against M3b again | android-game-dev, on `feature/candy-backdrop` | with that branch |
| 9 | **15e O-1:** in dark mode the three-button navigation bar shows a light grey scrim (`#cdccd0`, buttons 3.59:1). Pre-existing: arm A is pixel-identical (`qa-engineer-15e.md`, Findings) | Not a defect: it matches no criterion, and 3.59:1 clears the 3:1 bar for UI components. Whether a dark three-button bar is wanted is the ux-designer's decision. **If it is wanted, the mechanism is the window theme, as for F-2:** set `android:windowLightNavigationBar` to true in light and false in night. That attribute is API 27, and minSdk is 24, so it goes in `values-v27/` and `values-night-v27/`, each restating the whole style. Styles do not merge across qualifiers. A `WindowThemeTest` case pins it. Both files are start-path files, so the change rides a branch that runs its own 15e sitting: `bugfix/home-chip-arrival` if that branch runs one, else `feature/candy-backdrop`. The device check covers three-button and gesture modes in both themes | ux-designer rules; then android-game-dev → code-auditor + qa-engineer → tech-lead | the ruling before `release/3.0.0`; the change only if it is ruled wanted, otherwise post-3.0.0 |
| 10 | Both audits say "a Fable re-audit is owed when the policy allows" (loops 4 and 5, Open issues) | Not a merge condition. Q-019 is the founder's answer: Opus for the review chain until 3.0.0 is published, with fresh reviewers, the Check and mutation probes run by the reviewer. Both audits did exactly that. Whether phase 15's diff gets a Fable re-audit after 3.0.0 is a policy and money question | producer → the 3.0.0 retro; a non-blocking founder question if it costs money | after 3.0.0 is published |
| 11 | `avdmanager` is not installed, because the SDK has no `cmdline-tools` (D-1) | A machine-tools gap, not a product item. It is a candidate for the README's install rows | producer → the 3.0.0 retro | — |
| 12 | QA's O-2: the absolute TotalTime here, ≈ 0.4 s | No action: it describes this host and AVD, not a phone (D-03j, PLAT-08) | — | — |

Every row of my sign-off's "Routed after the merge" table stands unchanged. That includes F-1 (`bugfix/home-chip-arrival`, before `release/3.0.0`), the timer's TalkBack form, the 200 % pill labels, criterion 27's remainder, the THIRD_PARTY font rows, AC-F5's release half, the B1 mechanism in check-gates C1/C2, and the retro items. Rows 1, 3, 4 and 5 above add to F-1's branch, which is already reshaping Home.

## The merge, for the producer
- **`develop` has not moved.** It is at `c3d3437`, which is the branch's merge-base. So phase 15 merges with `git merge --no-ff`, and no merge-in is needed.
- **If `develop` moves first:**
  - A commit under `.planning/` or `docs/` only (for example `feature/phase-14-release-plan`, row 1 of Decision 4) changes nothing that was reviewed. Merge with no new Check or review.
  - A code commit means taking `develop` in and re-running the Check before the merge. A code-conflict resolution goes to the code-auditor. A merge-in that changes a start-path file comes back to me, because the sitting may no longer stand.
- **Record the PASS in STATUS,** citing `code-auditor-loop4.md`, `code-auditor-loop5.md`, `qa-engineer-15e.md`, `tech-lead-signoff.md` and this report. The merge commit carries the trailer that rules/gitflow.md (1.0.0) names.
- **Then Decision 4's order continues:**
  1. `feature/phase-14-release-docs`;
  2. `feature/phase-14-check-gates`, cut from `develop` now that R-1 and phase 15 have both merged;
  3. audio, with `develop` merged in first;
  4. `bugfix/home-chip-arrival`, the ux ruling first;
  5. `feature/candy-backdrop`, last, with its own 15e sitting on the bench.

## Docs edits still owed
### `docs/tech-design.md`: tech-lead, on `feature/phase-14-release-docs`, before that branch merges
`866dffb` already carries my sign-off's §10, §14.1 and §17 text. This close adds:
- **§10, the row "Cold-start regression (criterion 15e)":**
  - After "Runs once on phase 15 before its merge (H-3)", add: "Sat 2026-10-18 on `Pixel_9a_bench`: PASS, delta 33.0 ms, floor 3.0 ms, arm A `c3d3437`, arm B `ee16a2a` (`.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`)".
  - Add an instrument note: "The bench was written from `Pixel_9a.avd`'s own `config.ini` and `.ini`, since `avdmanager` is not installed. `avdmanager` and the copy are both valid. Either way the sitting records the diff against `Pixel_9a.avd`, and only `AvdId`, `avd.ini.displayname`, `disk.dataPartition.size` and the `.ini`'s `path`/`path.rel` may differ. An extra key makes the sitting VOID."
  - Add an ART note: "A debuggable APK stays at `[status=verify]` after `compile -m speed -f`. The ART precondition holds when both arms read the same status."
- **§14.1, the row "Phase 15":** its "Depends on" becomes "Signed off PASS 2026-10-21 (`15-founder-feedback/tech-lead-signoff-close.md`): H-1 `code-auditor-loop4.md` + `code-auditor-loop5.md`; H-3 `qa-engineer-15e.md`; H-4 recorded in STATUS 2026-10-18". Then record the merge.
- **§14.1, the row "F-1":** add rows 1, 3, 4 and 5 of the table above to its scope.
- **§16, new rows:**
  - 15e O-1, the dark three-button scrim (row 9: owner, mechanism, the sitting condition);
  - the Fable re-audit question (row 10).
- **§17, a new row:**

| Superseded | By | Where |
|---|---|---|
| P-1.1 "`avdmanager create avd -n Pixel_9a_bench …`" as the only route to the bench (`tech-lead-signoff.md` Decision 1) | `avdmanager`, or a copy of `Pixel_9a.avd`'s `config.ini` and `.ini`; either is closed by the P-1.2 diff, which every sitting records | §10; `tech-lead-signoff-close.md` D-1 |

### `docs/ux-spec.md` and `docs/ui/DESIGN-SYSTEM.md`: ux-designer
**When:** one dispatch after the phase-15 merge, off `develop`. It may be the same dispatch as the F-1 ruling. It must land before the next 15e sitting (candy's, or F-1's), whose qa-engineer reads §6.5, and before `release/3.0.0` is cut. Until it lands, my two reports govern.

**What.** House style: superseded wording is kept as the record.
- **a. Still unapplied from my sign-off.** `grep` finds no `Pixel_9a_bench` anywhere under `docs/`. Apply the text in `tech-lead-signoff.md`, "Text for other roles' files": §6.5 precondition 1, the new precondition 9, the "Which builds" pointer, and the two replacements in criterion 15e.
- **b. §6.5 precondition 1, after the 2026-10-18 amendment:**
  > *(2026-10-21 — tech-lead, phase-15 close, D-1.)* The bench may be made with `avdmanager` or by copying `Pixel_9a.avd`'s `config.ini` and `.ini`. Either way each sitting records the diff against `Pixel_9a.avd`. Only the name, the paths and `disk.dataPartition.size` may differ; any other difference makes the sitting VOID.
- **c. Criterion 15k part 2.** Keep the parenthetical at `docs/ux-spec.md:3805-3807` ("≈72dp over", "≈48dp over since D-03m") as the record, and append:
  > *(2026-10-21.)* Those figures were Robolectric LEGACY readings. On real metrics (Robolectric NATIVE in the app theme, and the device), 360×640dp with both chips overflows 0dp. So part 2 holds there in its no-overflow branch. The overflow branch is read on `w360dp-h560dp` (`HomeLayoutTest`). "Overflows" means overflows into a band: the column's trailing space, the gap after the last band plus the bottom padding, is not a band (§4.2.2 clause 5), so a fold inside it hides nothing and passes.
- **d. 15e O-1.** Rule whether the dark three-button navigation bar should be dark (row 9). If yes, it becomes an amendment with its criterion.
- **e. Still owed from my sign-off, unchanged:** `docs/ui/DESIGN-SYSTEM.md` §Typography (`tech-lead-fonts.md`), the F-1 ruling, the timer's spoken form, and the 200 % pill labels.

## Outputs
- `.planning/reports/15-founder-feedback/tech-lead-signoff-close.md`: this report. VERDICT PASS; it closes the HOLD in `tech-lead-signoff.md`, accepts D-1, routes the INFO items and 15e O-1, and gives the docs text owed. It is not committed: the producer commits it. I edited no other file.

## Checks
- `git -C PegSolitarie-p15 diff --stat ee16a2a HEAD -- . ':!.planning'` — pass. Two files, both under `app/src/test`: `HomeLayoutTest.kt` and `HomeTestFixture.kt`.
- `git -C PegSolitarie-p15 diff --name-only ee16a2a HEAD -- app/src/main domain build-logic gradle app/build.gradle.kts settings.gradle.kts build.gradle.kts` — pass (empty). The same check over the start-path files of Decision 1 (`MainActivity.kt`, `App.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, `res/values/themes.xml`, `res/values-night/themes.xml`, `AndroidManifest.xml`) — pass (empty). The sitting stands.
- `git -C PegSolitarie-p15 diff --stat b6a6230 HEAD -- . ':!.planning'` — pass (empty). Loop 5's Check on `b6a6230` covers the head's code.
- `git -C PegSolitarie-p15 rev-parse --short develop` and `git merge-base develop HEAD` — both `c3d3437`; `git log c3d3437..develop` is empty. Arm A is still the merge-base, and the merge needs no merge-in.
- `git -C PegSolitarie-p15 log --oneline ee16a2a..HEAD` — the sequence: loop 4's FAIL record, the H-3 record, the test-only fix commits `887c97d`, `4f95561`, `1757e7a`, `af08b86`, then records.
- `diff ~/.android/avd/Pixel_9a.avd/config.ini ~/.android/avd/Pixel_9a_bench.avd/config.ini` — three keys: `AvdId`, `avd.ini.displayname`, `disk.dataPartition.size`. `cat Pixel_9a.ini Pixel_9a_bench.ini` — only `path` and `path.rel` differ.
- `grep -n -i 'path\|/Users\|\.avd\|sdcard\|sysdir\|hw.device…' Pixel_9a_bench.avd/config.ini` — the Decision 1 values hold. No key points into `Pixel_9a.avd`; the only absolute path is `skin.path`, under the SDK.
- `ls -la` on both AVD directories — the bench's userdata base is 15.7 MB, written on 2026-10-18; the 9a's is 6.4 MB, dated April 23, so nothing was copied. The bench has no `sdcard.img` and the 9a has one.
- `grep -rn -E 'EXTERNAL_STORAGE|getExternalFilesDir|getExternalStorage|MediaStore|Environment\.getExternal' app/src/main` — no match.
- `grep -n -E 'windowLight|parent=' res/values{,-night}/themes.xml`, and `ls res | grep ^values` — `values` and `values-night` only, both on `android:Theme.Material.Light.NoActionBar` (row 9's mechanism).
- `grep -rln 'Pixel_9a_bench' PegSolitarie-p15/docs` — no match: the ux-spec fold is unapplied (fold a).
- `grep -n 'Pixel_9a_bench|instrument of record|…' PegSolitarie-r5/docs/tech-design.md` — §10 and §17 already carry my sign-off's text (`866dffb`). This report's additions are not there yet.
- The Check command was **not run** by me. Loop 5 ran `./gradlew --no-daemon --no-build-cache --rerun-tasks check` on `b6a6230`: green, 98/98 executed. Nothing after it is code. I do not re-run gsd's or the reviewers' gates (agents/tech-lead.md step 4).
- I did not read `.planning/log/`.

## Open issues / risks
- **Staffing deviation.** This sign-off, both audits and the QA sitting all ran on Opus, the developer's model (Q-019). The merge rests on the founder's answer and its mitigations: fresh reviewers, and the Check and mutations run by the reviewers themselves. Both audits did both. Row 10 carries the Fable re-audit question to the retro.
- **The bench is a single instrument with one sitting of history.** Its first floor, 3.0 ms, is far inside the VOID line. Candy's sitting will be its second.
- **F-1 is still on the branch,** and it lands on `develop` with this merge. Its deadline keeps it out of 3.0.0, and `develop` ships nothing.
- **The spec still names the `Pixel_9a` as 15e's instrument.** Until fold a lands, `tech-lead-signoff.md` and this report govern. A qa-engineer who sat 15e on the 9a would produce a VOID sitting, so the producer passes both reports to the next sitting's qa-engineer.

## Handoff
Next: producer —
1. Commit this report. Record phase 15's PASS in STATUS, citing the five reports above.
2. Merge `feature/phase-15-ux-refresh` into `develop` with `--no-ff` (see "The merge" for what to do if `develop` moves first).
3. Then follow Decision 4's order.
4. Dispatch the ux-designer for the spec folds a–e, before the next 15e sitting and before `release/3.0.0`. Fold d, 15e O-1, may share a dispatch with the F-1 ruling.
5. Add rows 1, 3, 4 and 5 of the INFO table to the `bugfix/home-chip-arrival` dispatch, and row 8 to candy's.
6. Dispatch the tech-lead later for the `docs/tech-design.md` additions on `feature/phase-14-release-docs`, together with the sign-off's own owed edits.
7. Carry rows 10 and 11 to the 3.0.0 retro.

VERDICT: PASS
