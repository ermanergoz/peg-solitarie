---
phase: 13-cloud-save
plan: 11
subsystem: verification
tags: [build-gate, r8, device-pass, uat, validation, cloud-save]

# Dependency graph
requires:
  - phase: 13-cloud-save
    plan: "03"
    provides: "13-OPERATOR-CHECKLIST.md (the Play Console Saved Games precondition) and 11-HUMAN-UAT.md tests 10-13, authored unrun"
  - phase: 13-cloud-save
    plan: "09"
    provides: "the real PlayGamesCloudSaveStore bound in place of the stub — without it there is nothing for a device pass to observe"
  - phase: 13-cloud-save
    plan: "10"
    provides: "the profile-swap dialog, whose device coverage this plan records as a standing gap"
provides:
  - "a green phase build gate: ./gradlew check, producer-run, 11m07s at 9211416"
  - "the finding that the phase needed no new R8 keep rule — app/proguard-rules.pro is byte-identical to develop"
  - "recorded outcomes for 11-HUMAN-UAT tests 10-13, honest across three attempts"
  - "13-VALIDATION.md reconciled to the shipped layout, with the unplanned 13-12 represented as unplanned"
affects: [13-12, phase-13-merge, release]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "A device pass that fails is a finding, not a retry: each attempt's result line is kept and marked superseded rather than overwritten, so the three attempts read as a history"
    - "A grep gate whose count is wrong is corrected in the record, not silenced: the property is restated in a form the command can actually express"

key-files:
  created:
    - .planning/phases/13-cloud-save/13-11-SUMMARY.md
  modified:
    - .planning/phases/13-cloud-save/13-VALIDATION.md
    - .planning/phases/11-play-games-session/11-HUMAN-UAT.md
    - .planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md
  untouched-by-design:
    - app/proguard-rules.pro

key-decisions:
  - "13-12 is represented in 13-VALIDATION.md as unplanned work with no plan file, in its own table, with commands quoted from the reports that ran them rather than from a <verify> block it does not have. Folding it into the Per-Task Verification Map would have broken that map's contract that every Task ID resolves to a real plan file."
  - "nyquist_compliant is true, with the limit stated on the Approval line: every row carries a real automated command and the gate ran green at 9211416, but 9211416 predates every 13-12 commit and the full check has not been re-run on the final tree."
  - "Two of the plan's expected grep counts are recorded as wrong-as-written rather than made to pass. The PLAT-04 grep returns 5 and returned 5 before phase 13 existed; the resetAllProgress() grep returns 4. Both underlying properties hold and are restated with commands that express them."
  - "The 13-0X-XX catch-all row is deleted rather than kept: its Task ID resolved to no plan file, so it could not satisfy the map's own contract, and the arch commands it carried are already run by rows 13-06-01 and 13-09-02."

patterns-established:
  - "A validation document that names its own contradictions: where a plan's acceptance criteria are mutually exclusive, the document says which one won and why"

requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-05]

# Metrics
duration: three attempts across 2026-09-20 (device pass), reconcile ~60min
completed: 2026-09-20
---

# Phase 13 Plan 11: Gate, Device Pass and Reconcile — Summary

**The gate is green, the minified release needs no new keep rule, the two-emulator device pass took three
attempts and ended with tests 10-13 all passing on two clean devices, and the validation document now
describes the phase that shipped — including the twelfth body of work nobody planned.**

## Performance

- **Tasks:** 3 of 3
- **Task 1** (gate): `./gradlew check` green, producer-run, **11m07s at `9211416`**
- **Task 2** (device pass): three attempts; final run **PASS on all four tests**
- **Task 3** (reconcile): `13-VALIDATION.md` rewritten; 24 map rows + 7 unplanned-work rows + 7 phase-wide assertions
- **Files changed by this plan:** 3 planning documents. **Zero** source files, zero dependency coordinates, zero schema changes.

---

## Task 1 — Full build gate, minified release, phase-wide assertions

**The gate is green.** `./gradlew check` — detekt, ktlint, the four literal gates and
`:domain:depthSweep` — ran green at commit `9211416`, **11m07s**, run by the producer because two
emulators were in use on the host. `.planning/reports/STATUS.md` records it as "13-11 task 1 green".
The JVM suite is separately green at the phase's final tree, `f30de3d`: **1194 tests, 0 failures,
0 errors, 0 skipped** (`.planning/reports/phase-13/android-game-dev-convergence-build.md`).

**Limit, stated plainly.** `9211416` predates every 13-12 commit. On the final tree what has been run
is `:domain:test :app:testDebugUnitTest`, `detekt` and `ktlintCheck`; `lint`, `koverVerify`,
`:domain:depthSweep` and `:app:assembleRelease` have not been re-run since `9211416`. Both the
code-auditor's and the tech-lead's PASS verdicts on 13-12 are explicitly conditional on that run, and
the Approval line of `13-VALIDATION.md` now says so.

**No R8 keep rule was needed — assumption A5 confirmed.** `app/proguard-rules.pro` is byte-identical to
`develop`: `git log develop..HEAD -- app/proguard-rules.pro` is empty. Nothing about the snapshot
surface — the SafeParcelables, the `BitmapTeleporter` crossing a Binder — required a rule. This matches
what Phase 12 found and recorded at `11-HUMAN-UAT.md` test 9: `play-services-games-v2:22.0.0` ships no
`proguard.txt` of its own and R8 wrote no `missing_rules.txt` at all. "No rule needed" is the finding,
as the plan asked; T-13-47 is mitigated by the absence rather than by an addition.

**The phase-wide source assertions — and two counts the plan got wrong.**

| Assertion | Expected by the plan | Actual | Verdict |
|---|---|---|---|
| no logging in the new packages (T-11-04) | 0 | 0 | ✅ |
| no `com.google.android.gms` outside `platform/` (PLAT-04) | 0 | **5** | ⚠️ plan wrong, property holds |
| manual conflict resolution only | 0 | 0 | ✅ |
| never `modifyBytes` (Pitfall 5) | 0 | 0 | ✅ |
| `resetAllProgress()` containment (D-14) | exactly 2 | **4** | ⚠️ plan wrong, property holds |
| no Room schema bump | empty | empty | ✅ |
| zero new dependency coordinates (T-13-SC) | empty | empty | ✅ |

- **PLAT-04 returns 5, and it returned 5 before phase 13 existed.** Verified at base commit `659edf6`
  (`android-game-dev-cloud-save-outcome.md`). The hits are KDoc *prose* in `App.kt:44` and
  `di/PlatformModule.kt:119` — both of which are sentences saying the file imports nothing from the SDK
  — and the downloadable-fonts provider constants in `ui/theme/Type.kt:53,54,60`. None is an import,
  none came from phase 13. The property PLAT-04 states is that no gms **type** escapes `platform/`, and
  the grep that expresses it, `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v
  'com/app/pegsolitaire/platform/'`, returns **0** — corroborated over bytecode by `PlatformBoundaryTest`
  and `PlatformBoundaryGateTest`.
- **`resetAllProgress()` returns 4, not 2.** The four hits are the interface declaration
  (`ProgressRepository.kt:237`), its override (`:508`), a KDoc mention (`ChallengeProgress.kt:140`), and
  the single call site (`ProgressSubmitter.kt:331`). The plan's wording — "the declaration in
  `ProgressRepository.kt` and the single call in `ProgressSubmitter.kt`" — never counted the override or
  the KDoc. D-14 containment holds exactly as specified: **one** call, and it is on the answered-dialog
  path in `ProgressSubmitter`.

Neither count was made to pass by narrowing a grep in the plan or by suppressing anything. Both are
recorded as corrections in `13-VALIDATION.md` § Phase-wide source assertions.

**T-13-SC closed empirically.** `git diff develop -- gradle/libs.versions.toml app/build.gradle.kts` and
`git diff develop -- app/schemas` are both empty. The phase added zero dependency coordinates, exactly
as RESEARCH § Standard Stack predicted, and bumped no Room schema. No `THIRD_PARTY.md` row was needed
because no dependency was added (the auditor's separate F-7 — that the file does not exist at all — is a
standing release-manager item, not a phase-13 blocker).

---

## Task 2 — Two-emulator device pass, 11-HUMAN-UAT tests 10-13

Three attempts. All three are kept on the result lines of `11-HUMAN-UAT.md`; the two earlier ones are
marked **superseded** rather than overwritten, so the history reads honestly.

**Attempt 1** (`30f6198`, build `323f76d`, Pixel_9a + Pixel_9) — **10 blocked, 11 blocked, 12 blocked,
13 partial.** No snapshot ever committed: `play_games_submissions_pending` drained while
`play_games_cloud_push_pending` stayed true. That is exactly how 13-OPERATOR-CHECKLIST Step 0 says an
un-enabled Saved Games toggle presents, so Q-001 went to the founder — who answered that Saved Games was
already enabled and published. The refusal was therefore a defect, and the phase opened the unplanned
13-12 work.

**Attempt 2** (`9211416`, build `691015f` with the `getConflict` fix) — **10 fail, 11 fail (D-04 pass),
12 blocked, 13 partial.** Pushes now reached stage `COMMITTED`, so the fix was verified, but the two
devices did not converge and test 12 could not be run at all: Play Games app 2025.09.66390 has no Saved
games screen. Two measurements came out of it: RESEARCH **A1 was falsified** (GMS accounts the cover at
its raw ARGB size, 1,048,576 bytes at 512 px against an 819,200-byte cap), and the non-convergence was
triaged by the tech-lead into experiments plus the convergence build.

**Attempt 3, the final run** (`93874bd`, build `88c2fd7`, APK sha256 `1c480a55cc614da0`, two **clean**
AVDs `Pixel_9_clean` + `Pixel_9b_clean`, GMS 26.34.36) — **tests 10, 11, 12 and 13 all PASS**, with
test 11's D-04 sub-check partial. Source: `.planning/reports/phase-13/qa-engineer-uat-final.md`.

| Test | Requirement | Outcome |
|------|-------------|---------|
| 10 | CLOUD-01 | **PASS** — an app-wiped device restored Challenge level 11 and English-33 3/3, content it had never held, from the other device's confirmed commit. Every D-03 negative held: no app toast, dialog, snackbar or Settings sync row; the only launch-time signal was the stock Play Games greeting. |
| 11 | CLOUD-02, CLOUD-04 / D-04 | **PASS**, D-04 **partial** — full union in both directions on the first relaunch of each device, nothing lost, no prompt, zero conflicts. D-04: the in-progress board survived exactly as left and the ladder did not regress, but "Next level" served N+1 instead of the merged head. New low-severity defect. |
| 12 | CLOUD-03 | **PASS by substitute** — via the 13-12 debug snapshot picker, because the Play Games app no longer has a Saved games screen. Both devices list the **same single entry**: `Challenge level 13 · 48 stars · 4/5 Classic boards`, same played time, same last-modified, same unbroken cover. Every number cross-checks against the app. |
| 13 | CLOUD-05 | **PASS** — offline half clean (three offline solves, an offline relaunch, no error wall/snackbar/dialog/retry prompt, 0 FATAL); reconvergence half passes in both directions, through two genuine Play Games conflicts the app resolved without loss. |

**Why the first two attempts failed, established after the fact.** Both ran on emulators whose Google
Play services had stopped uploading: every local commit stayed pending for 9+ hours while a clean device
on the same account synced normally (`producer-clean-device-finding.md`,
`qa-engineer-upload-baseline.md`). An environment fault, not an app defect — and the final run proved it
by observing a genuine conflict fire on a brand-new device, log the full `Inconsistent DB:null/4`
signature inside GMS's own `PlayGamesContentProvider.delete`, and **upload normally afterwards**.

**The profile-swap gap is still a gap.** It stands in `11-HUMAN-UAT.md` § Gaps verbatim as plan 13-03
wrote it. It needs a second Google account that has never authorised the game; no such account exists on
this setup. It is covered on the JVM only, by 13-08 and 13-10. `grep -c "profile-swap"` returns 1.

**Gate greps for task 2, re-run during the reconcile:** `grep -c "result: pending"` → **0**;
`grep -c "^### 1[0-3]\."` → **4**; `grep -c "profile-swap"` → **1**; the `## Summary` block's
`total: 13` with `passed: 11` + `blocked: 1` + `partial: 1` = 13; `grep -c "x\]"` on
`13-OPERATOR-CHECKLIST.md` → **2**, so the checklist was executed and ticked.

---

## Task 3 — Reconcile 13-VALIDATION.md

`13-VALIDATION.md` was rewritten to describe the phase that shipped.

- **A "What actually shipped" section** naming the eleven plans by wave, plus the unplanned twelfth body
  of work, `13-12` — described as what it was: work opened during the phase in response to a
  device-found defect, dispatched from tech-lead reports, with **no `13-12-PLAN.md`**.
- **The Per-Task Verification Map** now has one row per task of the eleven plans, 24 rows, columns
  unchanged. Every Automated Command is copied verbatim from that task's own `<verify><automated>`
  block; every Task ID resolves to a plan file that exists. Threat Refs come from each plan's own
  register. Status is ✅ green for all 24, resting on the `f30de3d` full-suite run (1194 tests) plus
  the doc gates re-run during the reconcile.
- **13-11-02 now carries a real command.** It is a human checkpoint, but its `<verify>` block does have
  an automated half — `grep -c "result: pending" …` — which the previous revision had replaced with a
  dash. Filling it is what makes every row in the map machine-checkable.
- **A separate 13-12 table** with the same shape, whose commands are quoted from the reports that ran
  them and whose last column names that report. It records the code-auditor's three PASS verdicts and
  the tech-lead's conditional sign-off, and points at `13-12-SUMMARY.md` § Follow-ups for what is open.
- **A phase-wide source assertions table** carrying the seven commands from task 1 with their real
  counts and the two corrections.
- **Wave 0 Requirements** all ticked, each with the plan and task that created it appended, after
  checking every file individually on disk.
- **Manual-Only Verifications** updated from the final UAT report, with the profile-swap row kept as a
  recorded gap, and A1's history recorded — falsified on 26.15.33, then the warning absent on 26.34.36
  with `getMaxCoverImageSize()` still unmeasured.
- **Deleted:** the "Task IDs above are provisional" footnote, and the `13-0X-XX` catch-all row whose
  Task ID resolved to no plan file.
- **Frontmatter:** `status: complete`, `wave_0_complete: true`, `nyquist_compliant: true`.
- **Sign-off boxes:** six ticked, two left unticked and labelled — every manual verification carried out
  (no: profile-swap), and `./gradlew check` green on the final tree (no: the gate is at `9211416`).

### One acceptance criterion the plan could not satisfy

Plan 13-11 task 3 requires every Automated Command to be copied verbatim **and** requires row 13-11-03's
own first grep to return 0 on this file. That grep searches `13-VALIDATION.md` for the very word it is
written with, so copying it verbatim guarantees one hit. The verbatim rule wins: the count is **1**, and
the single hit is the self-reference inside that command cell. Every other use of the word, including
the footnote the plan asked to delete, is gone. The command's second half, `grep -c "13-11-"`,
returns 5.

---

## Task Commits

1. **Task 1: full build gate, minified release, phase-wide assertions** — no commit; nothing needed to
   change. `app/proguard-rules.pro` was deliberately left untouched, which is the finding.
2. **Task 2: two-emulator device pass** — `30f6198` (attempt 1), `659edf6` (Q-001), `9211416`
   (attempt 2), `93874bd` (final run, all pass). The `13-OPERATOR-CHECKLIST.md` ticks ride with the
   final run.
3. **Task 3: reconcile 13-VALIDATION.md** — this commit.

## Deviations from Plan

**1. `13-VALIDATION.md` carries a table the plan did not ask for.** The plan was written before 13-12
existed and describes a phase of eleven plans. Representing 13-12 inside the Per-Task Verification Map
would have broken that map's own contract — every Task ID resolving to a plan file, every command copied
from a `<verify>` block — so it went into its own table with its commands attributed to the reports that
ran them, and both facts are stated in the document.

**2. Two expected grep counts in task 1 are recorded as wrong rather than made to pass.** Detailed
above. Neither grep was narrowed in the plan and nothing was suppressed; the properties are restated in
forms the commands can express.

**3. Task 1's `assembleRelease` console output is not in any report.** `.planning/reports/phase-13/`
contains no record of the release build for phase 13, because the producer ran the gate and the
developer agents were barred from Gradle while emulators were live. The evidence quoted for A5 is
therefore the unchanged `app/proguard-rules.pro` plus Phase 12's recorded `missing_rules.txt` finding,
not a fresh inspection. Anyone who needs the fresh one re-runs `./gradlew :app:assembleRelease` with the
27 IDs configured.

**4. Task 2 ran three times, on four different AVDs.** The plan describes one pass on two AVDs
(`Pixel_9a`, `Pixel_9`). The first two attempts used those two; the final run used two clean AVDs
because the original pair's Play services had stopped uploading. `Pixel_9a` is deliberately left
untouched as a wedged control for forensics.

## Requirements Evidence

| Requirement | Machine-checked by | Observed on a device by |
|---|---|---|
| CLOUD-01 | 13-04, 13-05, 13-06, 13-08, 13-09 rows | UAT test 10 — PASS |
| CLOUD-02 | 13-01, 13-05, 13-08, 13-10 rows | UAT test 11 — PASS (union both directions) |
| CLOUD-03 | 13-07, 13-09 rows | UAT test 12 — PASS by substitute (debug picker) |
| CLOUD-04 | 13-04, 13-05 rows | UAT test 11 / D-04 — PARTIAL (see the "Next level" defect) |
| CLOUD-05 | 13-02, 13-04, 13-06, 13-08, 13-09 rows | UAT test 13 — PASS |

## Handoff

Next: **code-auditor** and **qa-engineer** — this plan's remaining artefact is documentation; what needs
reviewing is the accuracy of `13-VALIDATION.md`, `13-11-SUMMARY.md` and `13-12-SUMMARY.md` against the
reports under `.planning/reports/phase-13/`, which are the only sources any claim here rests on.

Then the **producer**, before any merge to `develop`: run `./gradlew check` on the final tree. Both the
code-auditor's and the tech-lead's PASS verdicts on 13-12 are conditional on it, and the gate on record
is at `9211416`, which predates every 13-12 commit.

Open items are listed in `13-12-SUMMARY.md` § Follow-ups. The one that belongs to another role is the
new low-severity "Next level serves N+1 after a merged resume" defect, which the qa-engineer routed to
the **game-designer** for a wording ruling before it becomes a code change.
