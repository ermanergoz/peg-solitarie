# tech-lead report — phase 13 final sign-off (merge to develop)

**Verdict: HOLD** — on gate evidence, not on the code. Three items, none of which requires a
line of app source to change. When all three are green the merge proceeds without a further
tech-lead round.

Date: 2026-09-20
Branch: feature/phase-13-cloud-save (a317c7a..750e6e0, 101 commits; base `ca07a7e`)
Skills loaded: ergoz-soft:standards (role: tech-lead) → rules/studio.md, reporting.md, gitflow.md,
skills-policy.md, memory.md, testing.md, stacks.md, kotlin-android.md (plugin copies, v0.5.1; the
repo snapshot `.claude/rules/VERSION` is 0.1.0 and therefore stale)
Ran in parallel with: game-designer's "Next level" ruling (landed as `750e6e0` during this review)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- The **code** is sound: boundaries hold, zero new dependencies, domain stays pure Kotlin, three
  independent code-auditor PASSes, and CLOUD-01/02/03/04/05 now have field evidence on two devices.
- The **gate evidence does not cover the code being merged.** The green `./gradlew check` at
  `9211416` (03:49) predates the entire convergence build (`044c33a`..`f30de3d`, 04:03–04:19 —
  24 files, +1447/−113, two new production classes). My predecessor's sign-off was explicitly
  conditional on that run; the condition has not been met at the right commit.
- The phase's own validation contract (`13-VALIDATION.md:32`) also requires `:app:assembleRelease`
  with no `com.google.android.gms.games` entries in `missing_rules.txt`. That has **never** been run
  since `PlayGamesSnapshotPicker` — a new GMS Snapshots surface reachable in release through Hilt —
  was added. RESEARCH A5 / Pitfall 13 is still an open assumption.
- Producer's claim verified independently: `git diff f30de3d..HEAD --stat -- app domain build-logic
  gradle` is **empty**. Everything after `f30de3d` is documentation.
- On the two questions I was asked to arbitrate: the producer is **right** about RESEARCH A1 (QA
  misread it), and QA is **right** about the `change_pending` oracle — but it undermines no
  conclusion the phase records rely on.

---

## Verdict: HOLD — exactly what must happen

None of these is a code change. Items 1 and 2 are commands; item 3 is one reviewer pass. They run
in parallel.

**H-1 (producer) — `./gradlew check` green at `750e6e0`.** The standing evidence is from `9211416`,
which is 1,447 lines of production and test code behind the merge candidate. Nobody has run `lint`,
`dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck` or `audioFocusLiteralCheck` over the
convergence build: the developer ran the JVM suite plus detekt/ktlint only (its own report,
`android-game-dev-convergence-build.md:84`: *"Not run … `./gradlew check`, lint, kover,
`assembleRelease`"*), and both auditors scoped their runs the same way and said their verdicts were
conditional on the producer's full run. `gamesIdLiteralCheck` and `dpSpLiteralCheck` are the ones
that matter here — the convergence build added new string constants (`DEBUG_PICKER_EXTRA`, a picker
title) and touched an Android drawable. `rules/gitflow.md` makes Check-green a precondition of the
merge, so this is not discretionary. `:domain:depthSweep` is unaffected (no `domain/` file changed
since `9211416`).

**H-2 (producer) — `./gradlew :app:assembleRelease --max-workers=2`, then inspect
`app/build/outputs/mapping/release/missing_rules.txt` for `com.google.android.gms.games` entries.**
This is the phase's own exit gate (`13-VALIDATION.md:32`) and it has not run since `aeb861e` added
`PlayGamesSnapshotPicker`, which calls `SnapshotsClient.getSelectSnapshotIntent` and is kept
reachable in release by Hilt's generated factory (the auditor establishes the reachability at
`code-auditor-convergence-build.md:63`). A missing keep rule here is a release-only crash on a path
no unit test can reach. If entries appear, that is a developer task and a new round; I expect none.

**H-3 (code-auditor) — approve the three `@Suppress` lines added on this branch, and sweep the
never-reviewed range in the same pass.** `git diff develop...HEAD -- app/src/main domain/src/main |
grep '^+.*@Suppress'` returns three: `ProgressRepository.kt` (`DefaultProgressRepository`),
`ProgressSubmitter.kt:151`, `CloudProgressCodec.kt:44` — all `TooManyFunctions`/`LongParameterList`,
all justified inline, all following the repo's existing `PlayGamesService`/`SnapshotMapper`
precedent. `rules/studio.md` requires an *"approved by code-auditor"* line in the PR text and no
auditor range has covered them (they came from `dc64a9f`/`27afe01`, plans 13-04/13-05, executed
under gsd before the studio was adopted). Approving three lines is five minutes; I am asking for it
together with a **scoped** sweep of the range no studio auditor has ever seen — plans 13-01..13-10,
roughly 13,000 of the diff's 15,617 lines. Scope it to PII/privacy, the `platform/` boundary, the
destructive paths (`resetAllProgress`, `ResumeDao.clearAll`, `LevelResultDao.deleteAll`) and the
decode boundary in `CloudProgressCodec`. **Not** a line-by-line audit of the whole range: the code
carries 1,194 passing tests, detekt/ktlint clean over the whole tree, and end-to-end field
verification of the union in both directions. This closes the single largest process gap in the
phase at proportionate cost.

**What I am explicitly not holding for.** No app source change. Not the "Next level" defect (the
game-designer has ruled it non-blocking). Not any auditor finding — every open F/L/I item is Low or
Info and none is a defect the field evidence supports. Not `docs/tech-design.md`, `THIRD_PARTY.md`
or the debug picker.

---

## Technical grounds for the code itself (the part that is a PASS)

Judged on `git diff develop...HEAD` (157 files, +15,617/−137), the three auditor reports and the
final UAT. I wrote none of this code.

- **Module boundaries hold.** `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v
  '/platform/'` → 0. `PlatformBoundaryTest` + `PlatformBoundaryGateTest` (6 tests) green at the
  merge candidate with the picker in the Hilt graph.
- **Domain purity is structural, not asserted.** `domain/build.gradle.kts` applies `kotlin-jvm` only
  — *"Pure Kotlin/JVM ONLY — no Android plugin"* — and `grep -rn '^import android|^import androidx|
  ^import com.google.android' domain/src/main` → 0. The new merge law (`ProgressMerge.kt`,
  `ProgressMergeLimits.kt`, `CloudProgress.kt`) lives there, pure and JVM-tested.
- **Dependency rules untouched across the whole branch**, not just the auditors' sub-ranges:
  `git diff develop...HEAD --stat -- gradle/ '*.gradle.kts' gradle.properties` is **empty**. Zero new
  coordinates, so no license row is owed by this phase and no copyleft question arises.
- **The two guarantees that could have been reverted silently are mutation-pinned**, not asserted:
  the auditor removed `withContext(NonCancellable)` and moved `metadataChangeFor` after the open in a
  scratch copy and got exactly the two expected failures; a second round forced the push guard true
  and reverted `COVER_PX` to 512 and got five. That is the standard I want on a seam this thin.
- **Field evidence closes the causation question the phase was stuck on.** Two genuine conflicts
  fired on clean devices, one carrying the full `Inconsistent DB:null/4` stack inside
  `PlayGamesContentProvider.delete` on GMS 26.34.36, and **both devices uploaded afterwards**. The
  app's conflict path is exonerated by direct observation rather than by elimination, which retires
  the sequencing constraint my predecessor put on the F-3 fix (`tech-lead-conflict-wedge.md` B.2).

---

## Question 1 — RESEARCH A1: the producer is right, QA misread it

**Confirmed: the absence of the warning is the fix working, not evidence against the accounting.**

- `git merge-base --is-ancestor 044c33a 88c2fd7` → **true**. `044c33a` is *"fix(13-12): shrink the
  cloud save cover to 384 px to fit the raw-size cap"*, and `88c2fd7` is the build installed on both
  clean devices (`base.apk` sha256 `1c480a55cc614da0`, cross-checked in
  `producer-clean-device-finding.md:16`, `qa-engineer-e2-probe.md:151`,
  `qa-engineer-upload-baseline.md:156`, and by QA itself in the final session).
- `CloudSaveConstants.COVER_PX = 384`. 384 × 384 × 4 = **589,824 bytes**, against the measured cap of
  **819,200**. The tested build is arithmetically incapable of emitting
  `Snapshot cover image is too large`. Zero occurrences across nine commits is the **predicted**
  result, not a surprise.
- The comparison QA drew is confounded twice over — different GMS version *and* different
  `COVER_PX`. The build that produced *"every commit"* on Pixel_9a ran the 512 cover.
- A1 itself does not rest on version-dependent inference. It rests on a logged pair of exact
  integers: `Currently at 1048576 bytes; max is 819200`, and 1,048,576 is 512 × 512 × 4 **exactly**.
  That is a direct measurement of raw-ARGB accounting, not folklore.

**Rulings.** (a) `COVER_PX = 384` stays; it is already shipped and correct. (b) QA's
recommendation to *"re-read it on a 26.34.x device before spending the change"* is moot — the change
was spent before the session and is what produced the clean logs. (c) The honest wording for the
record is *"this session provides no new information about A1 in either direction, because the build
under test cannot trigger the warning"* — not *"the evidence has weakened"*. That wording correction
goes into the doc task below. Nothing here blocks the merge, and no code changes.

## Question 2 — the `change_pending` oracle: QA is right, and it changes nothing that is recorded

**The observation is correct and well-evidenced.** `Pixel_9b_clean` reported `change_pending = true`
across three launches between 17:57 and 18:05 while its payload had demonstrably already reached the
other device (5556 read `challenge_current_level = 16` at 18:00:01). QA's explanation is right: each
launch's push guard produced a *fresh* local commit that had not flushed at read time, so the flag
was true about a newer commit than the one whose content had landed.

**It undermines no conclusion the phase records rely on**, for three reasons:

1. Every conclusion in the record that matters was drawn from the **conjunction**, never from the
   flag alone: `change_pending = false` **and** `last_modified_at` equal to this device's own last
   commit. That is the form used at `qa-engineer-upload-baseline.md`, in the 22-row table of the
   final UAT, and in the producer's clean-device finding. The conjunction is unaffected — a false
   flag against your own commit is still a proven flush.
2. The weakened direction is `true`, which is the *pessimistic* reading. The records that depend on
   the oracle all assert flushes (optimistic claims), so a lagging `true` cannot have manufactured
   one. It can only have made the wedged devices look *worse* than they were — which is the direction
   QA already flags for the Pixel_9a re-read, and Pixel_9a's wedge is an environment fault that is
   out of scope for this merge either way.
3. The CLOUD-0x verdicts do not rest on the flag at all. CLOUD-01 rests on content the receiver had
   never held; CLOUD-02 on the observed union in both directions plus two devices listing one
   identical snapshot; CLOUD-03 on the picker; CLOUD-05 on the absence of UI and on reconvergence.

**Should the pull-probe KDoc be reworded before merge? No.** I read it: `CloudPullProbe`'s property
doc says *"whether the opened snapshot still held changes that had not been uploaded"*, which is
literally correct — it describes the snapshot, not the device. What is missing is the **negative**,
and three reports have now read it as an upload oracle, so the negative is worth writing down: *"true
does not mean this device cannot upload; it means this device's most recent local commit has not
flushed. The upload signal is `false` together with `lastModifiedAt` equal to this device's own last
push."* That is a doc-only edit with no test and no behaviour attached; putting it in the HOLD round
would cost a developer dispatch and a re-review for one sentence. It goes into **FU-2** with the two
folklore corrections, which is the same task and the same reader.

---

## B. Disposition of every open finding

Ids are qualified by report, because three reports independently number from F-1.

### Already closed — no action
| Item | Why |
|---|---|
| `outcome` F-1 (`$stable` filter) | Superseded: the getconflict fix swapped `Modifier.isStatic` for the named `COMPOSE_STABILITY_FIELD` exclusion, which the auditor calls a **strengthening** — a future `companion object { var lastError: String? }` is back in scope. |
| `getconflict` F-1, F-2 | Closed by the convergence build and **mutation-proved** pinned (`code-auditor-convergence-build.md:50`): two targeted failures on reversal. |

### Accepted permanently — recorded, never to be "fixed"
| Item | Ruling |
|---|---|
| **A(iii)** (give-up path leaves a dangling conflict record) | **Accepted permanently.** It is the SDK's own designed state — `open` is documented to return a conflict *"which will need to be resolved"* and the next pass re-opens it. And it is unreachable: the branch needs **eleven consecutive conflicts**; the maximum ever observed in the whole investigation is two, in different launches. No code, no test, no doc. |
| `outcome` F-4's data-loss half / `conflict-wedge` B.1 | **Accepted permanently by decision.** Leaving `conflictingSnapshot` unclosed on the give-up branch is *correct*: `discardAndClose` discards all changes to the file, and on that branch `resolveOnce` never ran, so its bytes — exactly where an un-uploaded offline commit lives — were never read into anything. Closing it would be a silent data loss traded for a visible, recoverable leaked handle. Keep the pinning test that fails if either close is added or removed. |
| `outcome` F-5 (`FAILED_OTHER` covers two causes) | Accepted. Distinguishable by the presence of a status code, documented in both KDocs. |
| `outcome` F-7 (`pending=true` beside `stage=COMMITTED`) | Accepted — a legitimate state, and now confirmed in the field. Already carried into the QA brief. |
| `getconflict` F-5 (null data labelled `CONFLICT_UNRESOLVED`) | Accepted. Unreachable with `createIfNotFound = true`; a stage mislabel with no handle to leak. |
| `outcome` F-6 (enum name not shrinker-proof) | Accepted **conditionally**: harmless while the value is read only through `run-as` on a debuggable build. If a release-device dump is ever needed, add `-keepclassmembers enum …CloudSaveStage { *; }` then, not speculatively. |

### FU-1 — snapshot handle lifetime and pull diagnosability *(follow-up phase; not required for v1.1)*
**One line of scope:** make every opened snapshot handle's fate observable and bound the conflict
loop by construction — one file, one test class, no behaviour change a player can see.

Carries: `outcome` F-2 (a failed `discardAndClose` is swallowed and `load` latches `closed` anyway —
report the failed close as a distinct stage or a third key rather than making cleanup throw);
`outcome` F-3 residue (`coroutineContext.ensureActive()` after the `try` in `load`/`save`; note
`getconflict` F-6 records what the partial fix already achieved); `outcome` F-4 / `getconflict` F-3
(the superseded and final resolved pairs are abandoned — either close both or record the SDK
ownership claim, and **pin whichever is chosen**, because today either passes);
`getconflict` F-4 (`>=` instead of `==` on the retry bound — costs nothing, removes a class of
infinite loop); `convergence` L-1 (carry `conflictsResolved` on the exhaustion path, so the probe is
not blank on exactly the device it was built for).

**Why not before v1.1.** The field evidence retired the hypothesis these findings were insurance
against: two conflicts ran on healthy devices, one with the full wedge signature, and neither device
wedged. This is hygiene and diagnosability, and every item is Low or Info. Shipping v1.1 with it
open is a decision I am taking deliberately and recording here.

### FU-2 — say what was measured, not what was assumed *(doc-only; before v1.1)*
**One line of scope:** correct four documented claims that the artifact does not support and one that
the field has now qualified — no code, no test.

1. `PlayGamesCloudSaveStore.kt:44-53`: *"an open handle that is never closed makes the next open fail
   for the rest of the process."* My predecessor scanned the entire bundled 22.0.0 javadoc and it is
   **not there**; it comes from the developer guide or community lore. The `try`/`finally` practice
   it motivates is right either way — reword it as reasoning, not as SDK contract.
2. `commitOrDiscard`'s stale-`existing` reassurance (*"it will conflict later and the next open
   resolves it"*): undocumented **and** contradicted in the observed window (`conflict-wedge` B.3).
   Replace with *"a stale `existing` can produce a full-overwrite that drops another device's keys;
   the guard's re-push is what recovers it."*
3. `13-RESEARCH.md:730-736` Pitfall 1 states the same folklore as fact — same correction, same words.
4. `CloudPullProbe`'s KDoc: add the `change_pending` negative from Question 2 above.
5. `convergence` I-1: one sentence on `holdsSomethingTheCloudLacks` recording that the guard compares
   the uncapped export against the capped push, that the arithmetic (≈400 KB ladder vs a 2.75 MiB
   trim target, ~7× headroom) makes it unreachable, and that even if it fired the behaviour degrades
   to the pre-diff every-launch push — not a regression.
6. `13-RESEARCH.md:1230` A1: replace *"the evidence has weakened"* framing with the correct reading
   from Question 1 — the tested build cannot emit the warning, so the session is silent on A1.
   `COVER_PX = 384` stands on the original exact-integer measurement.

**Why before v1.1.** Phase 14 re-reads these docs, and a release-readiness phase reasoning from
folklore is how the last three days were spent. Cheap and high-leverage.

### FU-3 — release readiness carry-ins *(before v1.1; phase 14 owns them)*
- **The "Next level" defect.** The game-designer has ruled (`game-designer-next-level-ruling.md`):
  a low-severity **defect**, not loose UAT wording — D-04 decided it in the opposite direction and
  the code ships against its own recorded decision. Fix = serve `max(challenge_current_level, N + 1)`;
  tell the player nothing; before the v1.1 release branch is cut. **I concur, and I answer the
  architecture question the ruling routed to me (tech-lead decision, recorded here and carried into
  `docs/tech-design.md`):** the arithmetic must move out of `ResultsActions.kt:56`, where a stateless
  leaf composable today computes `onNextLevel(challengeLevel + 1)` from a parameter that is the level
  just solved and has no access to the ladder. It belongs in `ResultsViewModel.onNextLevelTapped`,
  which must read the ladder with the **awaitable** `ChallengeProgress.settledLevel()`
  (`ChallengeProgress.kt:127` — a suspend read straight from DataStore) and not the cached `stateIn`
  value, so the read is ordered after `GameViewModel`'s `advancePast(N)` (`GameViewModel.kt:972`).
  Keep the `max` even with the settled read: it makes the re-serve-level-N loop unreachable by
  construction if the ladder write ever lands late, and the repo already carries a documented
  ladder/load-window race (WR-01). `ResultsActions` then passes no number and owns no navigation
  arithmetic, which is also the correct state-hoisting shape.
- **The account-swap / D-14 wipe path has never been exercised on a device.** `ProfileSwapDialog.kt`
  (222 new lines) plus `resetAllProgress` / `ResumeDao.clearAll` / `LevelResultDao.deleteAll` is the
  one destructive path in the phase, and no phase-13 report mentions it — the UAT set (tests 10–13)
  does not include an account swap, because it needs a second Google account. It is unit-tested and
  safe by construction (every dismissal, including system back and a scrim tap, routes to
  `onKeepLocal` — the single assignment that *is* D-13). That is good design and it is not a device
  test. **Required before v1.1: one UAT with a second Play Games account**, covering keep-local,
  replace-with-cloud and walk-away. Not a merge blocker — merging to develop ships nothing.
- **`THIRD_PARTY.md` does not exist anywhere in the tree** (`getconflict` F-7). Phase 13 adds zero
  dependencies so nothing is owed *by this phase*, but the file is a release-checklist item under
  rules/play-store.md. **release-manager, before v1.1.**
- **`convergence` I-2 — `prefs.py` committed under `.planning/reports/phase-13/tools/`.**
  `rules/testing.md` is explicit that a QA helper script is never committed to a product repo.
  Producer's to resolve; my recommendation is to keep the decoder but move it out of `reports/`,
  because it is the only thing that can read those diagnostic keys and phase 14 will want it — and
  record the exception rather than leave the rule quietly broken.

### Escalations to the producer (product-level, older than phase 13)
- **The Check command does not meet the studio's stack rules.** `rules/kotlin-android.md` requires
  `check` to run `koverVerify`, and `rules/testing.md` requires a `FUZZ_SEEDS`-honouring fuzz harness
  for a game. Neither exists: `grep -rn kover build-logic app/build.gradle.kts gradle/libs.versions.toml`
  → 0 hits, and there is no fuzz task in the build. `check` here is detekt + ktlint + lint + test +
  four literal gates + `:domain:depthSweep`. The repo predates the studio (phases 1–12 under gsd
  alone), so this is inherited debt, not phase 13's. It must be closed before v1.1 ships or waived by
  a recorded decision; either way it is not this merge's blocker.
- **`docs/tech-design.md` does not exist** — the fifth report to say so, and it is my own required
  output. `docs/` holds `concept-brief.md`, `audio/`, `ui/`, `superpowers/` and nothing else, so
  every review in this phase has been audited against a report rather than a design. I own it; before
  v1.1, and it will carry the decisions recorded above (the seam contract, the `platform/` boundary
  rule, the ladder-read ordering, the FU-1 handle-ownership rule).

## C. L-3 — the debug snapshot picker

**Decision: keep it, move it behind a `src/debug` source set, and do it in phase 14 before v1.1
ships. Do not delete it, and do not merge-block on it.**

The facts I checked myself: `ls app/src/` → `main`, `test` only, so the developer's KDoc is accurate
that *"debug-only" cannot mean "not compiled into release"* today. The gate is a constructor-injected
`BuildConfig.DEBUG`, `false` in release whether or not R8 folds it, and `onIntent` returns on its
first line before the extra is read. `PlayGamesSnapshotPicker.kt` is 71 lines; it and
`DEBUG_PICKER_EXTRA` and the picker title ship in the APK, kept reachable by Hilt's generated factory.

**Why not delete.** The investigation is over, but the picker is not only an investigation tool — it
is the **only instrument that can verify CLOUD-03**. QA used it for test 12 precisely because *"the
Play Games app still has no Saved games screen"*, and the strongest CLOUD-02 corroboration in the
whole phase came from it (two devices listing one identical entry, cross-checked to the minute).
Deleting it leaves an acceptance criterion with no way to test it in phase 14 or in any later
regression.

**Why not keep as-is.** The exposure is genuinely small — a decompiler learns that a debug-only,
read-only picker exists, and the worst an attacker who repackages the app achieves is seeing their
own Play Games save for their own account, which is no privilege gain. But `rules/studio.md`'s
definition of done and the auditor's own sentence say it best: *"a diagnostic entry that outlives its
investigation is how debug surfaces reach production."* v1.1 is a published build; I am not signing a
release that carries an unreachable debug surface when the split is cheap.

**Why it is cheap, concretely.** `DebugSnapshotPickerEntry`'s primary constructor already takes
`show: suspend () -> Unit` rather than the picker — the design anticipated this. Move
`PlayGamesSnapshotPicker` into `app/src/debug/`, have the `main` Hilt module provide a no-op `show`,
and have a `debug` module provide `picker::show`. `MainActivity` and every test are untouched. Fold
**`convergence` L-2** into the same task while the file is open: move the 30 s wait off
`@ApplicationScope` onto `lifecycleScope` at the call site, which also brings the two
`withTimeoutOrNull` waits inside the `runCatching` — both are debug-only today but the current shape
lets the picker surface on a *later* Activity than the one launched with the extra, which will read
as flakiness in a QA session.

**Timing: required before v1.1 ships, not before this merge.** Nothing publishes from `develop`, and
the picker is load-bearing for CLOUD-03 verification right up until the release branch is cut.

## D. What is now known to be false and should be corrected

All of it is FU-2 above and **none of it blocks the merge** — every item is a comment or a research
note, and re-opening the branch for doc edits would cost a developer dispatch plus a re-review for
zero behavioural change. Listed once more in priority order, since the task asks for it explicitly:

1. **`PlayGamesCloudSaveStore.kt:44-53`** — "an unclosed handle makes the next open fail for the rest
   of the process" is presented as SDK behaviour and is not in the 22.0.0 javadoc. **False as
   sourced**, possibly true as behaviour. Note precisely: the UAT does *not* contradict it — every
   abandoned handle in that session was abandoned in a process that then ended, so the intra-process
   claim was never tested. Reword as reasoning; keep the practice.
2. **`commitOrDiscard`'s stale-`existing` reassurance** — undocumented **and** observed not to happen.
   The strongest of the four; it actively misleads.
3. **`13-RESEARCH.md:730-736`, Pitfall 1** — same folklore, same correction.
4. **`13-RESEARCH.md:1230`, A1** — the "weakened" framing that Question 1 refutes. A1 stands;
   `COVER_PX = 384` stands; the session is simply silent on it.
5. **`CloudPullProbe` KDoc** — add the `change_pending` negative. Not false, but incomplete in the
   exact way that misled three reports.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie/.planning/reports/phase-13/tech-lead-signoff.md` — this
  sign-off (uncommitted; the producer commits it with the merge)

## Checks
- `git diff f30de3d..HEAD --stat -- app domain build-logic gradle` — **pass** (empty: the producer's
  docs-only claim is independently verified at `750e6e0`, which includes the game-designer ruling
  that landed mid-review)
- `git merge-base --is-ancestor 9211416 f30de3d` — **true**, and `git diff 9211416..f30de3d --stat --
  app domain build-logic gradle` → **24 files, +1447/−113**. This is H-1: the green check predates the
  merged code. Commit times confirm it (`9211416` 03:49; `044c33a`..`f30de3d` 04:03–04:19).
- `git merge-base --is-ancestor 044c33a 88c2fd7` — **true** (Question 1: the tested build contains the
  384 px cover fix)
- `CloudSaveConstants.COVER_PX` — **384**; 384 × 384 × 4 = 589,824 ≤ 819,200 (Question 1 arithmetic)
- `git diff develop...HEAD --stat -- gradle/ '*.gradle.kts' gradle.properties` — **pass** (empty: zero
  new dependencies across the whole branch, not just the auditors' sub-ranges; no license row owed,
  no copyleft question)
- `git diff develop...HEAD -- app/src/main domain/src/main | grep -E '^\+.*(@Suppress|@Ignore|!!|baseline|Disabled)'`
  — **3 hits**, all `@Suppress` of the Size family with inline justification, from `dc64a9f`/`27afe01`
  (plans 13-04/13-05). **No auditor range covers them** → H-3.
- `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v '/platform/' | wc -l` — **0**
  (boundary holds at `750e6e0`)
- `grep -rn '^import android|^import androidx|^import com.google.android' domain/src/main | wc -l` —
  **0**; `domain/build.gradle.kts` applies `kotlin-jvm` only (domain purity is structural)
- `tasks.named("check")` in `build-logic/src/main/kotlin/standards.gradle.kts:231-240` — read: detekt,
  ktlintCheck, dpSpLiteralCheck, adIdLiteralCheck, gamesIdLiteralCheck, audioFocusLiteralCheck, on top
  of the AGP defaults; `:domain:depthSweep` wired in `domain/build.gradle.kts`
- `grep -rn kover build-logic/ app/build.gradle.kts gradle/libs.versions.toml` — **0 hits**; no
  `FUZZ_SEEDS`/fuzz task anywhere (escalation: the Check command does not meet rules/kotlin-android.md
  or rules/testing.md)
- `grep -rln 'ProfileSwap|profile swap|resetAllProgress' .planning/reports/phase-13/` — **0 hits**;
  `11-HUMAN-UAT.md` tests 10–13 contain no account swap (the destructive path has no device evidence)
- `ls app/src/` — **`main`, `test`** only (L-3: no `debug` source set exists; the KDoc's premise is
  accurate)
- `ls THIRD_PARTY.md` — **missing** (`getconflict` F-7 confirmed)
- `.planning/phases/13-cloud-save/13-VALIDATION.md:32` — read: the phase's own gate requires
  `./gradlew check` **and** `:app:assembleRelease` with no `com.google.android.gms.games` in
  `missing_rules.txt`; `android-game-dev-convergence-build.md:84` records that none of check, lint,
  kover or assembleRelease was run → H-2
- **Not run, by the task's environment constraint:** the full `./gradlew check`, `:app:assembleRelease`,
  any connected/install task, anything touching adb or the emulators. H-1 and H-2 exist precisely
  because I am barred from closing them myself.
- `git status --short` / `git rev-parse --abbrev-ref HEAD` — **pass** (clean; `feature/phase-13-cloud-save`;
  no branch switch, no commit, no app source, Gradle file or test edited by me)

## Open issues / risks
- **The largest residual risk is not a finding — it is review coverage.** Three auditor PASSes cover
  roughly 2,500 of 15,617 lines. Plans 13-01..13-10 — the merge law, the codec's decode boundary, the
  import/export path, `ProgressSubmitter`, `HomeViewModel`, `ProfileSwapDialog` — have never had an
  independent reviewer, because they were executed under gsd before the studio was adopted. Mitigated
  by 1,194 passing tests, a very high test-to-code ratio on exactly those files, detekt/ktlint over
  the whole tree, and field verification of the union in both directions. H-3's scoped sweep reduces
  it; it does not eliminate it, and I am accepting the remainder knowingly.
- **The one destructive path has no device evidence** (FU-3). Safe by construction and unit-tested,
  but "safe by construction" is what every silent data-loss post-mortem says beforehand.
- **`Pixel_9a` remains wedged and untouched**, correctly preserved as a control. It is an environment
  fault by the weight of evidence (clean AVDs on the same account, project and snapshot record work
  end to end) and it is out of scope for this merge. If phase 14 wants it closed, that is a
  GMS-side investigation, not an app change.
- **FU-1 is being deferred on the strength of field evidence, not on the strength of the code.** If a
  device is ever again observed unable to upload, FU-1 is the first place to look and its
  diagnosability items are what will make that look cheap.
- I did not verify the 1,194-test figure myself; I verified the two auditors' independent subsets
  (181 + 6 tests, plus two mutation rounds) and the mutation evidence, which is the stronger signal.
  H-1 closes the rest.

## Handoff
**Next: producer.** Run H-1 and H-2, and dispatch H-3 in parallel with them.

- **H-1** `./gradlew check --max-workers=2` at `750e6e0`.
- **H-2** `./gradlew :app:assembleRelease --max-workers=2`, then
  `grep -n 'com.google.android.gms.games' app/build/outputs/mapping/release/missing_rules.txt`
  (expect no hits).
- **H-3** code-auditor: approve the three `@Suppress` lines and record the approval line for the merge
  commit; sweep `13-01..13-10` scoped to PII, the `platform/` boundary, the destructive paths and the
  decode boundary.

**If all three are green, merge `feature/phase-13-cloud-save` into `develop` with
`git merge --no-ff` — no further tech-lead round is required.** If H-2 turns up a missing keep rule,
that is an android-game-dev task and a fresh review round; H-1 failing on a literal gate is likewise
a developer task. H-3 rejecting a suppression is the auditor's call and returns to the developer.

Then: file **FU-1** (follow-up phase), **FU-2** (doc-only, before v1.1) and the **FU-3** carry-ins
into phase 14's scope, and queue the two product-level escalations (the Check command's missing
coverage and fuzz gates; `docs/tech-design.md`, which I own). Nothing here needs the founder.
