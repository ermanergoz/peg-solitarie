---
phase: 08-assist-generator-quality-debt
plan: 11
subsystem: planning-docs
tags: [release-apk, r8, resource-retention, documentation-trail, d-06, phase-audit, debt-01, debt-02, debt-03]

# Dependency graph
requires:
  - phase: 08-03
    provides: the DEBT-03 nyquist reconciliation in the v1.0 milestone audit (whose block this plan must not touch)
  - phase: 08-07
    provides: the per-board star bands and resume-path target preservation the device audit was to confirm
  - phase: 08-10
    provides: the book-aware hint pipeline and the depth sweep whose measured counts this plan quotes
provides:
  - 08-BOOK-EVIDENCE.md — release-APK retention proof for the five book resources, byte-compared against source
  - A corrected documentation trail across ROADMAP, STATE, PROJECT and the v1.0 milestone audit (D-06)
  - A tracked, itemised device-audit todo standing in for the deferred Task 3 check
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Prove the payload, not the id: packaged resources are extracted and byte-compared against source with cmp, because a surviving entry name is a weaker claim than surviving bytes"
    - "Measure the mechanism, not just the outcome: aapt2 returning 0 matches is what explains WHY isShrinkResources has no remit over JAR resources"
    - "Decompose a size delta rather than attributing all of it to the thing under test"
    - "A deferred human gate becomes a tracked artifact with the checklist intact, never a silent downgrade to weaker hardware"

key-files:
  created:
    - .planning/phases/08-assist-generator-quality-debt/08-BOOK-EVIDENCE.md
    - .planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md
  modified:
    - .planning/ROADMAP.md
    - .planning/STATE.md
    - .planning/PROJECT.md
    - .planning/milestones/v1.0-MILESTONE-AUDIT.md

key-decisions:
  - "RESEARCH assumption A5 resolved CONFIRMED at the payload level: all five book resources are byte-identical in a minified, resource-shrunk release APK"
  - "The book's cost is reported as its own 11 628 B packaged share, not the 85 970 B whole-APK delta, because most of that delta is Phase 8's domain code"
  - "The plan's five-file -k4 verify command was not satisfiable and was not made satisfiable by renaming an artifact — the fifth book is wiegleb45-k3.txt by the developer's 08-08 decision"
  - "The repo-wide 'several pegs' grep criterion was scoped to the plan's own four target files; correcting archived v1.0 documents would have falsified the historical record rather than the stale claim"
  - "Task 3's device audit was DEFERRED ENTIRELY by developer decision rather than discharged against an emulator — Phase 7's emulator-only, globally-approved audit is the recorded lesson being avoided"

requirements-completed: [DEBT-01, DEBT-02, DEBT-03]

# Metrics
duration: 27 min
completed: 2026-08-30
---

# Phase 8 Plan 11: Release-APK Retention, the D-06 Documentation Trail, and the Deferred Phase Audit Summary

**The opening book is proven to ship byte-intact through R8 into a minified release APK, all four planning surfaces now describe the Diamond-41 centre start and the true shape of the solver envelope — and the phase's on-device audit is explicitly recorded as NOT performed, deferred to a tracked todo rather than downgraded to an emulator.**

## Performance

- **Duration:** 27 min
- **Tasks:** 3 (2 executed, 1 deferred by developer decision at its checkpoint)
- **Files created:** 2 | **modified:** 4

## Accomplishments

- **RESEARCH assumption A5 is resolved — CONFIRMED, at the stronger payload level.** All five book resources are present in a minified, resource-shrunk release APK and every one is **byte-identical** to its committed source (`cmp` exit 0 ×5). This was load-bearing rather than precautionary: since 08-10 the book is on the production hint path and `OpeningBook` **fails closed**, so missing resources would have silently restored pre-book behaviour — including Wiegleb-45's untruthful banner — **with no error at all**.
- **The retention mechanism is measured, not reasoned.** `aapt2 dump resources` returns **0** matches for `golden/book`, confirming these are plain JAR resources arriving via `mergeReleaseJavaResource`, outside the Android resource table and therefore outside `isShrinkResources`' remit. R8 minified the code in the same build (`mapping.txt` emitted) and touched none of the book.
- **The book is cheap.** Its own packaged cost is **11 628 B** — 0.56 % of the APK — for 47 619 B of source text at 76 % compression. That is about a quarter of what a single `okhttp3` public-suffix table already costs this build.
- **Four planning surfaces now match the code.** The Diamond-41 centre start, its proven floor of 2, both halves of the proof, and the non-uniform solver envelope are recorded where future phases will actually read them.
- **The audit was deferred honestly.** No physical device was available; the developer declined an emulator run. The full 1a–5c checklist survives intact as a tracked todo instead of becoming a false approval.

## Task Commits

1. **Task 1: release-APK retention proof** — `1b32fb4` (docs)
2. **Task 1 follow-up: D-09 gate re-arming recorded** — `a73afd2` (docs)
3. **Task 2: the D-06 documentation trail across four surfaces** — `ef67354` (docs)
4. **Task 3: deferred to a tracked todo by developer decision** — `1713208` (docs)

**Plan metadata:** this commit (docs).

## Files Created/Modified

- `.planning/phases/08-assist-generator-quality-debt/08-BOOK-EVIDENCE.md` — commands and verbatim output, the five packaged entries with compressed/uncompressed sizes, per-file `cmp` results, the decomposed APK size delta, the A5 resolution, and the recorded cleanup of the throwaway ad IDs.
- `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md` — the deferred audit, checklist intact.
- `.planning/ROADMAP.md` — DEBT-02's criterion rewritten to the centre start, the floor of 2 and both proof halves; DEBT-01's sharpened to name the book + bitboard-solver mechanism.
- `.planning/STATE.md` — both carried blockers rewritten and moved into a new Resolved record; 9 Phase 8 decision lines added; the residual 444-position gap recorded as the one live concern.
- `.planning/PROJECT.md` — the flagged Revisit row replaced with the resolved decision, keeping its honest half.
- `.planning/milestones/v1.0-MILESTONE-AUDIT.md` — `tech_debt` marks DEBT-01/02 resolved with closing plans cited; `nyquist` untouched (08-03 owns it).

## The retention evidence (headline result)

| # | Packaged entry | Uncompressed (B) | Compressed (B) | `cmp` vs source |
|---|----------------|-----------------:|---------------:|-----------------|
| 1 | `golden/book/english33-k4.txt` | 8 754 | 2 175 | **identical** |
| 2 | `golden/book/european37-k4.txt` | 14 664 | 3 407 | **identical** |
| 3 | `golden/book/asymmetric39-k4.txt` | 11 344 | 2 720 | **identical** |
| 4 | `golden/book/diamond41-k4.txt` | 9 267 | 2 198 | **identical** |
| 5 | `golden/book/wiegleb45-k3.txt` | 3 590 | 1 128 | **identical** |
| | **Total** | **47 619** | **11 628** | **5 of 5** |

**APK size, extending Phase 6's ledger:**

| Row | Configuration | APK (bytes) | Δ |
|---|---------------|------------:|---|
| 3 (Phase 6) | Post-R8, SDK artifacts on the graph, unreferenced | 1 983 331 | baseline |
| **4 (this plan)** | Row 3 + all Phase 8 `:domain` work, including the book | **2 069 301** | **+85 970 B (+4.33 %)** |

The delta is deliberately **decomposed** rather than attributed to the book: 11 628 B (13.5 %) is the book as packaged; the remaining 74 342 B is `SolverTables`, the bitboard `Solver`, `OpeningBook`, `BoardCanonical`, target-aware `Pagoda`, the star bands, plus zip overhead. Reporting the whole delta as "the book" would have overstated its cost by 7×.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :app:assembleRelease` | **BUILD SUCCESSFUL in 24s** |
| Book entries in release APK | **5 of 5** present |
| Per-entry `cmp` against committed source | **5 of 5 byte-identical** (exit 0) |
| `aapt2 dump resources \| grep -c golden/book` | **0** — plain JAR resources, as expected |
| R8 active | `minifyReleaseWithR8` ran; `mapping.txt` emitted |
| Resource shrinking active | `convertShrunkResourcesToBinaryRelease` ran |
| `grep -c 'signingConfig' app/build.gradle.kts` | **0** |
| `git status --porcelain` (no APK/keystore staged) | **empty** |
| `./gradlew check` | **BUILD SUCCESSFUL in 11m 11s**, exit 0 |
| `:domain:depthSweep` | **passed** — 1 test, 0 failures |
| `grep -rn '(4,2)' STATE/ROADMAP/AUDIT` | **no matches** |
| `grep -rl 'several pegs' STATE/ROADMAP/PROJECT/AUDIT` | **no matches** |
| `grep -q 'DEBT-01' PROJECT.md` | present; **0** `Revisit` markers remain |
| `nyquist` block in milestone audit | **unchanged** |
| **Task 3 device audit** | **NOT PERFORMED — deferred (see below)** |

**The depth sweep independently reproduced 08-10's counts** on this worktree, which is a real cross-check that the book still resolves after the packaging work:

| Board | k | examined | book-served | auth. dead | solver fallback | of which answered | max/call |
|-------|---|---------:|------------:|-----------:|----------------:|------------------:|---------:|
| english33 | 4 | 373 | 369 | 4 | 0 | — | 22 ms |
| european37 | 4 | 609 | 609 | 0 | 0 | — | 6 ms |
| asymmetric39 | 4 | 461 | 461 | 0 | 0 | — | 1 ms |
| diamond41 | 4 | 785 | 345 | 0 | 440 | **0** | 1513 ms |
| wiegleb45 | 3 | 101 | 97 | 0 | 4 | **0** | 1500 ms |

Every count matches 08-10 exactly; only per-call maxima differ, as timing figures do between machines.

## The device audit — NOT performed

**Task 3, the blocking on-device phase audit, was deferred in full by developer decision at its checkpoint on 2026-08-30.** No physical device was available, and the developer explicitly declined to run it against `emulator-5554`.

Stated plainly, because the whole point of this plan is that the repository does not overstate itself:

- **No per-item approvals exist. No global approval exists.** Items 1a–1d, 2a–2c, 3a–3c, 4a–4b and 5a–5c are all outstanding.
- **Nothing in Phase 8 has been confirmed on real hardware** — not on a device, and not on an emulator either.
- Nothing in this SUMMARY, `08-BOOK-EVIDENCE.md`, STATE.md or the milestone audit may be read as "verified on device".

This is the deliberate application of Phase 7's recorded lesson: that audit was emulator-only *and* globally approved, and STATE.md carries the resulting ambiguity as a known weakness. Repeating the pattern would have converted a known gap into a false claim.

**What IS proven, by machine:** `./gradlew check` green including the exhaustive sweep; the book byte-intact in a shipping-configuration APK; the hint pipeline's truthfulness property asserted over all 2 329 swept positions. **What is NOT proven:** that a player sees it. Those are different claims and only the first is currently supported.

**Tracked at** `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md`, with the checklist, the ready APK path, the install command, the approved-behaviour note for the 444 gap positions, and the per-item-never-global approval rule preserved verbatim. It is flagged to share a session with the outstanding Phase 7 audio-hardware todo.

## Decisions Made

1. **A5 resolved at the payload level, not the id level.** Phase 7 learned that a surviving resource id is a weaker claim than a surviving payload, since `isShrinkResources` can retain an id while replacing what it holds. Every entry was therefore extracted and byte-compared, not merely listed.
2. **The size delta was decomposed rather than attributed.** Reporting +85 970 B as "the book" would have been the easy sentence and a wrong one.
3. **The `-k4` mismatch was resolved in favour of the artifact.** The plan's verify command expects five `-k4.txt` files; only four exist, because Wiegleb-45 ships at K=3 with a self-declaring name by the developer's own 08-08 decision. Renaming the artifact to satisfy the command would have made the filename lie about coverage — the exact failure 08-08 renamed it to prevent.
4. **The `several pegs` grep was scoped to the four target files.** Archived v1.0 documents, `08-RESEARCH.md`, `08-PATTERNS.md` and prior plans quote the phrase *as the thing being corrected*. Rewriting them would falsify the historical record rather than the stale claim.
5. **The release gate was satisfied by Phase 6's documented method, and the fact was recorded.** Throwaway all-zero IDs in the gitignored `local.properties`, then removed, with the gate verified to fire again afterwards.
6. **The audit was deferred, not downgraded.** See above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 — Bug] The plan's verify command asserts five `-k4.txt` book entries; only four exist**
- **Found during:** Task 1
- **Issue:** `<verify><automated>` requires `unzip -l | grep -c "golden/book/.*-k4\.txt"` to equal 5 and then loops `for f in ... wiegleb45` against `golden/book/$f-k4.txt`. The fifth book is **`wiegleb45-k3.txt`** — it ships at depth 3 by the developer's decision at 08-08's Task 3 checkpoint, and 08-08 deliberately derived the filename suffix from `policy.depth` so the artifact cannot misrepresent its own coverage. Written literally, the command fails on a correct repository.
- **Fix:** Proved the stronger, true property instead — **all five** book resources present and byte-identical under their real names, verified per file. The `-k4`-agnostic count (`golden/book/.*\.txt`) returns 5.
- **Files modified:** none (evidence documented rather than artifact renamed)
- **Verification:** 5 entries listed; 5 `cmp` runs exit 0.
- **Committed in:** `1b32fb4`

**2. [Rule 1 — Bug] The repo-wide `several pegs` criterion is unsatisfiable without falsifying history**
- **Found during:** Task 2
- **Issue:** An acceptance criterion and the plan-level verification require `grep -rn 'several pegs\|~5 pegs' .planning/` to return nothing. But the phrase appears throughout archived v1.0 phase documents, `08-RESEARCH.md`, `08-PATTERNS.md`, `08-CONTEXT.md`, `08-01/08-06-PLAN.md` and prior SUMMARYs — always **as the stale claim being corrected**. The plan's own `files_modified` lists only four planning files, so the repo-wide grep contradicts the plan's stated scope.
- **Fix:** Satisfied the plan's own `<verify><automated>` block, which is correctly scoped to the four target files, and confirmed none of them contains the phrase. Archived history left intact — a record of what was corrected is not a false claim.
- **Files modified:** none beyond the four in scope
- **Verification:** `grep -rl 'several pegs'` over the four files → no matches; `grep -rn '(4,2)'` over the three → no matches.
- **Committed in:** `ef67354`

**3. [Rule 3 — Blocking] `:app:assembleRelease` refused to configure without real ad/PGS IDs**
- **Found during:** Task 1
- **Issue:** Phase 6's D-09 gate fails a release invocation at configuration time when `ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID` or `PGS_PROJECT_ID` is unset. Real IDs still do not exist, and a fresh worktree has no `local.properties` at all. Task 1 cannot run without a release APK.
- **Fix:** Applied Phase 6's own documented method for exactly this situation (`06-BUILD-EVIDENCE.md` § APK size ledger, row 3): throwaway, all-zero, non-functional IDs in the untracked, gitignored `local.properties`, confirmed gitignored before use. Removed immediately after the build, with a follow-up `assembleRelease` verified to fail again naming all four keys. The main checkout's `local.properties` was never modified. The whole sequence is recorded in the evidence document rather than left invisible.
- **Files modified:** `local.properties` (gitignored, never staged, since reverted)
- **Verification:** Gate re-armed — `BUILD FAILED in 1s` naming all four keys.
- **Committed in:** `a73afd2` (the record; no source change)

**4. [Rule 1 — Bug] My own correction prose reintroduced the stale coordinate**
- **Found during:** Task 2 acceptance-criteria loop
- **Issue:** The milestone-audit prose I wrote quoted the old "(4,2) off-center" wording while describing it as stale, which tripped the `grep -rn '(4,2)'` criterion on my own edit.
- **Fix:** Rephrased to "off-center, literature-asserted" — the meaning is preserved and the stale coordinate no longer appears anywhere in the corrected surfaces.
- **Files modified:** `.planning/milestones/v1.0-MILESTONE-AUDIT.md`
- **Verification:** Criterion re-run → no matches.
- **Committed in:** `ef67354`

### Deferred by developer decision

**5. Task 3's on-device phase audit — deferred in full (2026-08-30 checkpoint).** Not a deviation I chose; a decision the developer made when no physical hardware was available, declining an emulator substitute. Tracked as a todo. See "The device audit — NOT performed".

---

**Total deviations:** 4 auto-fixed (2 plan assertions that were false of the repository, 1 blocking build gate, 1 self-inflicted), plus 1 developer-directed deferral.
**Impact on plan:** No scope creep, and no artifact was altered to make a stale command pass. Deviations 1 and 2 share the root cause that recurred through this whole phase — the plan was written against an idealised set of artifacts (uniform depth 4, a repo with no history) and the shipped artifacts are more honest than that. Each was resolved toward the property the repository actually supports.

## Threat Model Coverage

| Threat ID | Disposition | How it is now discharged |
|-----------|-------------|--------------------------|
| T-08-40 (book stripped/altered by R8 or shrinking) | mitigate | **Discharged.** Entries listed AND each byte-compared with `cmp`; the mechanism confirmed via `aapt2` returning 0. Escape hatch recorded, not needed, not implemented. |
| T-08-41 (documents asserting facts the code does not support) | mitigate | **Discharged.** All four D-06 surfaces corrected; every quoted figure traces to a plan SUMMARY (08-05 latencies, 08-06 terminal/move count, 08-08 book sizes, 08-10 sweep counts); grep gates confirm the stale strings are gone from the corrected surfaces. |
| T-08-42 (a globally-approved audit hiding a per-item failure) | mitigate | **Not applicable as feared, and explicitly not papered over.** No approval of any kind was given; the itemised requirement is preserved verbatim in the todo. The threat this addresses is now the todo's central instruction. |
| T-08-43 (committing a signing config or an APK) | mitigate | **Discharged.** `grep -c 'signingConfig'` → 0; `git status --porcelain` empty; no APK or keystore staged; throwaway IDs confined to the gitignored `local.properties` and removed. |
| T-08-SC | n/a | No packages installed. |

## Issues Encountered

- **Two plan assertions were false of the repository** (the `-k4` count, the repo-wide grep), both traceable to the plan being written against an idealised artifact set. Resolved toward the true property rather than by altering artifacts. Documented as deviations 1–2.
- **The release build's D-09 gate blocked Task 1** until Phase 6's documented placeholder method was applied and then reverted. Documented as deviation 3.
- **The phase's device-truthfulness audit is outstanding.** This is the one substantive gap the phase closes with, it is deliberate, and it is tracked.

## User Setup Required

None — no external service configuration required. (Real ad/PGS IDs remain an open external prerequisite for Phase 9 onward, unchanged by this plan.)

## Next Phase Readiness

**Phase 8 is code-complete and its documentation trail is honest. One human gate is outstanding.**

1. **The device checklist must be run before v1.1 closes.** `.planning/todos/pending/2026-08-30-run-phase-8-device-checklist-on-physical-hardware.md`. Pair it with the Phase 7 audio-hardware todo — one session closes both.
2. **DEBT-01's residual is real and recorded.** 444 positions across Diamond-41 and Wiegleb-45 still return `null` after the full 1.5 s and show the rescue banner. Approved, header-declared, pinned by equality in `DepthSweepTest`. Closing Diamond's 440-position gap is the honest lever and also removes `check`'s 11-minute cost.
3. **`check` costs ~11 minutes**, essentially all of it Diamond's gap positions. Unchanged by this plan.
4. **The book's release-packaging behaviour is now a known quantity.** Any future change to the packaging pipeline (a new shrinker mode, moving `:domain` to an Android library, an AAB-only track) should re-run the `08-BOOK-EVIDENCE.md` procedure — `OpeningBook` fails closed, so a packaging regression is silent by construction.

## Self-Check: PASSED

- Both created files exist on disk and are tracked by git; all four modified files present and committed.
- All four `08-11` task commits verified in `git log`: `1b32fb4`, `a73afd2`, `ef67354`, `1713208`.
- `git status --porcelain` empty apart from the gitignored `local.properties` (confirmed via `git check-ignore`; never staged, and reverted to `sdk.dir` only).
- Every plan-level `<verification>` command re-run and recorded in the Verification table, with the two unsatisfiable criteria documented as deviations 1–2 rather than silently skipped.
- `./gradlew check` exit 0; `:domain:depthSweep` passed with counts matching 08-10.
- Task 3 recorded as NOT performed, with no claim of device verification anywhere in this document.

---
*Phase: 08-assist-generator-quality-debt*
*Completed: 2026-08-30*
