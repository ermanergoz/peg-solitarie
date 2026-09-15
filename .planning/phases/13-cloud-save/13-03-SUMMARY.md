---
phase: 13-cloud-save
plan: 03
subsystem: infra
tags: [play-games-services, play-console, saved-games, operator-checklist, uat, documentation]

# Dependency graph
requires:
  - phase: 12-leaderboards-achievements
    provides: "12-OPERATOR-CHECKLIST.md Step 0 — the PGS project, both OAuth credentials (debug-keystore SHA-1 and Play App Signing SHA-1) and the tester list this phase's Saved Games toggle hangs off, cross-referenced rather than restated"
  - phase: 11-play-games-session
    provides: "11-HUMAN-UAT.md as the project's single accumulating device checklist (Phase 12 D-04), and test 2's one-account gap whose root cause and wording the profile-swap gap reuses"
provides:
  - "13-OPERATOR-CHECKLIST.md — Step 0 'Enable Saved Games', its off-by-default state, the separate publish action, and the silent-failure symptom a missed toggle produces"
  - "The D-16 two-emulator script staged as executable steps (both AVDs, one tester account, exact launch commands, serial identification) before any cloud code exists"
  - "11-HUMAN-UAT.md tests 10-13 covering CLOUD-01/02/03/05 on device, all authored result: pending"
  - "A recorded profile-swap (D-12..D-15) coverage gap naming its compensating JVM coverage in plans 13-08 and 13-10"
affects: [13-cloud-save, play-console-operations]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "An operator checklist names the SYMPTOM of a missed step, not just the step — a silent failure indistinguishable from a code bug is diagnosed from the document rather than from hours of debugging"
    - "Device verification is authored before the code it verifies, so the pass is a procedure rather than an improvisation at the end of the phase"
    - "An untestable requirement is recorded as a named gap with its compensating coverage, never silently skipped"

key-files:
  created:
    - .planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md
  modified:
    - .planning/phases/11-play-games-session/11-HUMAN-UAT.md

key-decisions:
  - "The checklist leads with the failure MODE, not the toggle: Saved Games off produces the same null/false the offline path produces by design (CLOUD-05), so it presents as 'cloud save does nothing, no error anywhere' — byte-identical to a code bug. Naming that symptom is the document's whole value."
  - "Phase 13 creates no new Play Console identifiers, and the checklist says so explicitly — an operator arriving from Phase 12's 27-key recording table would otherwise go looking for a value to paste into local.properties."
  - "Phase 13's device tests append to 11-HUMAN-UAT.md rather than opening a 13-HUMAN-UAT.md, continuing Phase 12 D-04's single-accumulating-checklist rule."
  - "All four new tests are authored result: pending and no pre-existing result line was touched; plan 13-11 is the only plan permitted to write a real result (T-13-08)."
  - "The RESEARCH A1 measurement ask (getMaxCoverImageSize() and the PNG-compressed 512x512 cover size) is folded into test 12 and into the phase-done gate, because the operator is already in the Play Games saves UI at that moment — measuring an undocumented accounting basis once costs nothing there and is otherwise assumed forever."

patterns-established:
  - "Checkbox hygiene is stated in the document itself: a pre-ticked box in an operator checklist is a false claim about a system state nobody else can see"
  - "A known gap carries its compensating test seam by name (FakeGamesService.emitSession(SignedIn(playerId = \"other\")) in plans 13-08/13-10) so the gap cannot be read as missing coverage"

requirements-completed: []

# Metrics
duration: 6 min
completed: 2026-09-15
---

# Phase 13 Plan 03: Operator Checklist and Device UAT Summary

**A 124-line Play Console checklist whose headline is not the Saved Games toggle but the symptom of forgetting it — a silent `null`/`false` indistinguishable from offline — plus four device tests (10-13) and an honestly recorded profile-swap gap appended to the project's one accumulating checklist.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-09-15T20:03:00Z
- **Completed:** 2026-09-15T20:09:00Z
- **Tasks:** 2
- **Files modified:** 2 (1 created, 1 modified) — 154 insertions, 4 deletions

## Accomplishments

- **The expensive failure is named before it can happen.** `13-OPERATOR-CHECKLIST.md` opens by stating that Saved Games is off by default and that `SnapshotsClient` failures against a project without it resolve into exactly the `null`/`false` the offline path produces — which is *correct* behaviour under CLOUD-05's deliberate silent degradation, and therefore indistinguishable from a code bug. The document says plainly that this one checkbox blocks 11-HUMAN-UAT tests 10-13 in their entirety, i.e. 100% of the phase's device verification, and that 13-RESEARCH assumption A6 records the toggle's state as unverified (it may already be on — confirm, don't assume).
- **The publish trap is carried forward from Phase 12.** Turning the toggle on and publishing the Play Games Services configuration are separate actions; the checklist ties this explicitly to Step 3 of `12-OPERATOR-CHECKLIST.md`, where the same trap on the OAuth consent screen produces the same class of everything-looks-right-in-the-Console failure.
- **Phase 12's prerequisites are cross-referenced, not restated.** Step 1 re-confirms the PGS project link, both OAuth credentials and the tester account by pointing at 12-OPERATOR-CHECKLIST Step 0 items 2/4/6/8 — five references in total, zero duplicated instructions that could drift.
- **The D-16 two-emulator run is staged, not improvised.** Both AVD launch commands are recorded verbatim (`~/Library/Android/sdk/emulator/emulator -avd Pixel_9a` and `-avd Pixel_9`), along with the requirement that both carry the *same* tester account and that the operator can tell which `adb` serial is which — test 13 kills the network on `Pixel_9` specifically.
- **Four device tests now exist for the four CLOUD requirements the JVM cannot prove**, each with an expectation written from the locked decisions rather than from the requirement text: test 10 carries the D-03 inversion (an app-owned "Progress restored" surface is a **defect**, not a nicety, because it was considered and rejected), test 11 carries the D-04 sub-check that a stale in-progress game survives the ladder jumping underneath it, test 12 carries the A1 measurement ask, and test 13 asserts offline parity with v1.0 plus reconvergence after reconnect.
- **The one requirement that cannot be verified is recorded as a gap with its substitute named.** The profile-swap dialog needs a second Google account that has never authorised the game; none exists, same root cause as Phase 11 test 2. The gap entry names `FakeGamesService.emitSession(SignedIn(playerId = "other"))` in plans 13-08 and 13-10 as the compensating coverage, and says that if a second account ever exists, this is the test to run.
- **Nothing is pre-ticked and no existing result was rewritten.** `grep -c "x\]"` over the new checklist returns 0 across 16 checkboxes; `git diff 11-HUMAN-UAT.md | grep -c "^-.*result: pass"` returns 0, and the only removed lines in the whole plan are the frontmatter `source`/`updated` pair and the two Summary counters.

## Task Commits

Each task was committed atomically:

1. **Task 1: Phase 13 operator checklist — Enable Saved Games** — `36e6dbb` (docs)
2. **Task 2: Append Phase 13 device tests 10-13 and the profile-swap gap to 11-HUMAN-UAT.md** — `bf5ecd1` (docs)

## Files Created/Modified

- `.planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md` (created, 124 lines) — frontmatter `type: operator-checklist`, `status: pending`, `blocks: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05]`; "Why this exists"; Step 0 enable + publish + no-new-ID confirmation; Step 1 prerequisite chain by cross-reference; Step 2 the two-emulator staging; Step 3 the known gap; the "Before calling Phase 13 done" gate.
- `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` (modified, +30/-4) — new `## Phase 13 Tests — Cloud Save` section with tests 10-13 between the Phase 12 block and `## Summary`; Summary `total: 9 → 13` and `pending: 0 → 4` with `passed: 7`, `blocked: 1`, `partial: 1` untouched; frontmatter `source` gains `13-03-PLAN.md` and `updated` refreshed to 2026-09-15T20:06:55Z. Tests 1-9 are byte-identical and unrenumbered.

## Verification

All acceptance criteria were executed as written:

| Check | Expected | Actual |
|-------|----------|--------|
| `grep -c "Enable Saved Games" 13-OPERATOR-CHECKLIST.md` | ≥ 1 | 2 |
| `grep -c "off by default" 13-OPERATOR-CHECKLIST.md` | ≥ 1 | 1 |
| `grep -c "Pixel_9a" 13-OPERATOR-CHECKLIST.md` | ≥ 1 | 2 |
| `grep -c "12-OPERATOR-CHECKLIST" 13-OPERATOR-CHECKLIST.md` | ≥ 1 | 5 |
| `grep -c "x\]" 13-OPERATOR-CHECKLIST.md` | 0 | 0 (16 unticked boxes) |
| `grep -c "^### 1[0-3]\." 11-HUMAN-UAT.md` | 4 | 4 |
| `grep -c "^total: 13" / "^pending: 4"` | 1 / 1 | 1 / 1 |
| `grep -c "result: pending" 11-HUMAN-UAT.md` | 4 | 4 |
| `grep -c "profile-swap" 11-HUMAN-UAT.md` | ≥ 1 | 1 |
| `grep -c "13-OPERATOR-CHECKLIST" 11-HUMAN-UAT.md` | ≥ 1 | 1 |
| `git diff 11-HUMAN-UAT.md \| grep -c "^-.*result: pass"` | 0 | 0 |
| `ls .planning/phases/13-cloud-save/13-HUMAN-UAT.md` | fails | fails (no such file) |

Two criteria needed a correction mid-task, both caught by running the greps rather than assuming them:

- `off by default` initially returned 0 because the phrase had been split across a line wrap by the bold markers (`**off by` / `default**`). The sentence was rewrapped so the phrase is contiguous on one line. This matters beyond the grep: the acceptance criterion is the T-13-07 mitigation, and a phrase a reader's eye skips over is exactly the one an operator misses.
- `profile-swap` initially returned 0 because the gap entry opened with a sentence-capitalised "**Profile-swap dialog**". Reworded to "**The profile-swap dialog**" so the lowercase form appears. Same lesson: case-sensitive greps are part of the contract.

## Decisions Made

- **The checklist's thesis is the symptom, not the step.** Anyone can write "enable Saved Games". The reason this plan runs in wave 1, before any cloud code exists, is that the *consequence* of missing it is invisible: CLOUD-05 requires a cloud failure to degrade silently to local-only play, so a disabled toggle and a working offline fallback are the same observable. The document leads with that and repeats it in the Phase 13 test block's diagnosis note.
- **Phase 13 adds no Play Console identifiers, said out loud.** An operator arriving from Phase 12's 27-key `local.properties` table will reasonably expect a value to record. Step 0 item 3 is a checkbox confirming there isn't one — "if you find yourself copying a value out of the Console for this phase, you are on the wrong screen."
- **The A1 cover-image measurement rides along with test 12.** The operator is already in the Play Games saves UI to verify the description line; capturing `getMaxCoverImageSize()` and the PNG-compressed cover size there turns an undocumented accounting basis into a measured number for the cost of one extra line. If A1 is wrong the remedy is a single constant (`COVER_PX` → 384 or 256), which is only actionable if someone actually measured.
- **The gap entry states what the dialog *is*, not just that it is untested.** D-12..D-15's behaviour (different player id → one-time dialog, dismissal means keep, replace wipes everything) is summarised inside the gap so a future reader with a second account can run the test without reading the CONTEXT file first.
- **The `## Current Test` narrative block was deliberately left untouched.** It records the Phase 11/12 session history and is not a counter; the plan asked for the Summary counts and the frontmatter, and rewriting prose the phase did not author risks exactly the kind of silent history loss T-13-08 guards against.

## Deviations from Plan

None — both tasks executed as written. Two in-task corrections (the two wrapped/capitalised grep phrases above) were authoring fixes caught by the plan's own automated verification, not deviations from its instructions.

## Out of Scope — Noted, Not Acted On

- **`.planning/phases/12-leaderboards-achievements/12-HUMAN-UAT.md` still exists.** Task 2's acceptance criteria mention that neither a `12-HUMAN-UAT.md` nor a `13-HUMAN-UAT.md` should exist, but the only CLI check given is for the Phase 13 file (which does not exist and was not created). The Phase 12 file is pre-existing, predates this plan, is fully superseded — its own preamble says "a pass recorded in either file counts for both", and all six of its tests are recorded as passing in `11-HUMAN-UAT.md` tests 4-9 — and it is not in this plan's `files_modified`. Deleting a file this plan was not scoped to touch, from a parallel worktree, is exactly the destructive action the executor's deletion guard exists to prevent. Flagged here for whoever closes Phase 13: it is a stale duplicate carrying seven misleading `result: [pending]` lines, and removing it is a one-line quick task.

## Threat Flags

None. This plan writes two markdown files under `.planning/`, touches no source file, adds no dependency, and introduces no endpoint, auth path, file access pattern or schema change. The threats it addresses (T-13-07 toggle DoS, T-13-08 unrun-tests-recorded-as-passed, T-13-09 dropped gap) are mitigated by the acceptance criteria verified above.

## Known Stubs

None. Both artifacts are complete documents. The unticked checkboxes and the four `result: pending` lines are not stubs — they are the intended state of a document a human executes later, and plan 13-11 is the plan that fills them in.

## Notes for Next Phase

- **Wave 1 hand-off:** the operator can start Step 0 immediately and in parallel with engineering. It takes minutes and blocks every device test in the phase, so it should be done before plan 13-11 is scheduled, not when it starts.
- **Plan 13-11 owns the `result:` lines.** No other plan should write one. If a test is run early, record it there rather than editing the four pending lines in place.
- **Plans 13-08 and 13-10 are named in the Gaps entry** as the home of the profile-swap JVM coverage. If that coverage lands elsewhere, update the gap entry so the compensating-coverage claim stays true.
