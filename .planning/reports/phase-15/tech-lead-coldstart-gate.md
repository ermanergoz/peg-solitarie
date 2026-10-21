# tech-lead report — criterion 15e: re-specifying the cold-start gate

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (e757b3b..this commit — reports only, no source, no spec edit)
Skills loaded: ergoz-soft:standards (role: tech-lead), android-gradle-logic, android-architecture
Ran in parallel with: phase-14 (worktrees `-r1`, `-r5`, primary)
Ran on: Opus 5 1M (`claude-opus-5[1m]`) at the harness default effort — `agents/tech-lead.md` pins
`model: fable`; Fable's allowance is exhausted, so this ran on Opus per the founder's decision
Q-003. I specified the gate being corrected here, so this is a **self-correction, not a review**:
it takes a criterion I wrote off the board on evidence, which lowers no bar and approves no
work — the replacement gate is still checked by the qa-engineer, not by me (rules/studio.md, rule 1).
Fan-out: none
Loops: 0
Lessons applied: none

---

## Summary

Criterion 15e is **ill-posed, not merely unmeasured**, and the error is mine: I derived a 50 ms
delta threshold from a *within-arm range on a quiet host* and applied it to a *between-arm median
difference*. Two independent null experiments put that floor at **57 and 59 ms**. One correction to
the brief: both nulls were taken on the **same AVD**, so the 2× gap in absolute medians is host
load, not device class — which kills the absolute gate on an emulator while leaving the relative
one salvageable. Ruling below: the delta half becomes an interleaved A/B gate whose threshold is
derived from a null control measured in the same sitting; the absolute half goes back to
PLAT-08/phase-14 where phase 6 put it.

## RULING

### 1. The ≤ 50 ms delta threshold is retired as specified. It is my specification error.

Phase 6's 46 ms was the **range of nine samples of one build** (603–649 ms around a 633 ms
median, `06-BUILD-EVIDENCE.md:319-320`). I turned it into a threshold on **the difference between
two arms' medians**. Those are different statistics, and the second is the noisier of the two
because it also absorbs everything that drifts between the arms — host load, thermal state, and, in
this studio, whatever the other agent sessions are doing. Phase 6 even labelled its own number
*"an honest smoke-level baseline, not a benchmark-grade number"* and warned that phase 14 must
re-measure *"on the same AVD … on the same host"*. I built a benchmark-grade gate on it anyway.

Two nulls now measure the real floor, and they agree:

| Sitting | Arm medians (same build both times) | Null delta | Within-arm spreads | Host 1-min load |
|---|---|---|---|---|
| developer, `15-BUILD-EVIDENCE.md:52-53` | 957 / 1014 ms | **57 ms** | 172, 97 ms | loaded (3 agent sessions) |
| qa-engineer, `qa-engineer.md:162-163` | 1832 / 1773 ms | **59 ms** | 253, 225 ms | 6.43, 8.49 |

A gate that asks "is the delta ≤ 50 ms?" cannot be answered by an instrument that reports 57–59 ms
between a build and itself. Keeping it would force a fabricated pass or a permanent block. Both
roles were right to refuse it, and the developer's unprompted null experiment is exactly the right
instinct — it belongs in the protocol, which is where I am putting it.

### 2. Correction to the brief: it is one AVD, and that changes the conclusion.

The coordinator's note read the ~1.8 s medians as possibly device-specific. They are not. Both
sittings name **AVD `Pixel_9a`, API 36, `emulator-5566`, `:app:installDebug`**
(`15-BUILD-EVIDENCE.md:41-43`, `qa-engineer.md:41-45`) — the same named instrument, the same
variant. Phase 6 used the same AVD model (`emulator-5554`).

So the same AVD read **957 ms and 1832 ms — a factor of 1.83 — with nothing changing but host
load**, and the qa-engineer names the load: another session's `depthSweep` worker holding ~100% of
a core, plus a second `Pixel_9` emulator at ~44%. That is the decisive fact:

- The **absolute** reading is not a property of the app at all on a shared host. It cannot gate anything.
- The **relative** reading is far more robust: the null delta held at 57 → 59 ms (3.3%–5.8% of the
  median) across that same 1.83× swing. A regression gate is recoverable; an absolute budget is not.

### 3. What criterion 15e becomes — runnable by the qa-engineer, in one sitting

An **interleaved A/B regression gate with a same-sitting null control**. The two substantive
changes are interleaving (which cancels the drift that produced 57–59 ms between two *blocked*
runs of one build) and a threshold **derived from the instrument in that sitting** rather than from
a figure carried over from phase 6.

**Preconditions — all recorded; if any fails the sitting is void, not failed**
- AVD `Pixel_9a`, API 36, `:app:installDebug`, animations off per §11.
- **Exactly one emulator running.** The second `Pixel_9` must be down.
- **No concurrent Gradle build in any worktree** (the lock in `tech-lead-depthsweep.md` F2 makes
  this enforceable rather than hoped for), and no other agent session building.
- `uptime` recorded before and after; **1-minute load < 2.0** at both ends.
- ART state normalised on both arms before measuring:
  `adb shell cmd package compile -m speed -f com.erman.pegsolitarie`, so JIT/AOT warm-up is not a
  free variable between arms.
- Launch component resolved from the device, not assumed
  (`cmd package resolve-activity --brief`) — the correction `15-BUILD-EVIDENCE.md:43` already
  found, and the WAL-aware seeding fix at `:95-159`, both stand.

**Protocol**
- Arm A = `develop` build, Arm B = branch build, both installed and kept installed.
- **Interleave**: A/B/A/B in blocks of 5, **≥ 20 recorded samples per arm**, discarding the first
  sample of each block, `am force-stop` + 2 s between samples. Do not run one arm to completion and
  then the other — that is what the two nulls measured.
- **Null control first, in the same sitting, under the identical protocol**, with the *same* build
  in both positions. Record `floor = |median difference|`.

**Verdict**
- `median_B − median_A ≤ max(2 × floor, 100 ms)` → **PASS**
- greater → **FAIL**, and it is a real regression
- `floor > 150 ms` → **VOID**: the host is too noisy to decide. Re-run later. A void sitting is
  not a pass and not a fail, and it may not be reported as either.

On today's evidence the threshold would land at ~114–118 ms, which comfortably catches the scale
of regression a Home refresh could plausibly cause and refuses to claim 50 ms resolution nobody has.

**This raises the bar rather than lowering it.** The threshold is derived from a control measured
alongside the comparison, so a noisy host cannot buy a pass — it buys a *void*, and the run must be
repeated. Today a noisy host yields a number whose sign is luck and which a tired reader would take
as a result. That is the trade I am making: fewer decidable sittings, no undecidable verdicts.

### 4. The ≤ 950 ms absolute budget is retired as a phase-15 gate and goes back to PLAT-08 / phase 14.

This is not a deferral I invented; it restores two earlier decisions.

- Phase 6 wrote its 633 ms figure as *"the before-picture **PLAT-08 measures against in Phase
  14**"* (`06-BUILD-EVIDENCE.md:308-309`). The absolute comparison was always phase 14's.
- §11.2 already routes this exact case: a condition-(1)-only failure *"is PLAT-08 / Phase-14's and
  escalates to the producer rather than being paid for out of this refresh's visual budget"*.

And 950 ms is not defensible as a number any more: it derives from a **pre-SDK** 633 ms baseline
that predates UMP, AdMob, Play Games, leaderboards and cloud save, and from a build with a
different `applicationId` (`com.app.pegsolitaire` then, `com.erman.pegsolitarie` now). Ruling:

- **950 ms is demoted to a tracked target, not a gate.**
- The **gate** is rules/studio.md's product baseline — **under 2 seconds**, on a **named mid-range
  physical phone**, not an emulator. That is the studio's actual bar and the one a release must
  meet. QA's ~1.8 s on a loaded emulator is evidence about the host, not about the app, and must
  not be read as either passing or failing that bar.
- Owner: **PLAT-08 / phase 14**, with the reference device named in the phase-14 release evidence.

### 5. The delta runs once, on the follow-up slice — not against this build.

The qa-engineer's reason is correct and I am adopting it: Home's layout mechanism is changing in
the follow-up slice (`D-03i` / §4.2.2, the weighted-spacer fix the auditor and ux-designer both
recorded), so a delta measured against this layout would be superseded before anyone read it.

- 15e's delta is a **closing condition of the follow-up slice**, with `develop` as arm A.
- It is **not** a closing condition of this build, and `:NOT RUN` here is the correct record.
- Phase 15 therefore closes without a 15e number, on this ruling, not on an omission.

### 6. Instrument succession — its own task, because it is new infrastructure

`am start -W` is the wrong instrument for a budget and always was: it reports
ActivityManager's displayed-time, which on an emulator absorbs host scheduling wholesale. The right
one:

- A `:macrobenchmark` module using Jetpack Macrobenchmark `StartupTimingMetric` with
  `StartupMode.COLD` and `CompilationMode.Full()`, ≥ 10 iterations, on a physical device.
- Plus **`reportFullyDrawn()`** in `MainActivity`, which makes **time-to-full-display** measurable
  for the first time. That matters here specifically: TTFD is what Home's asynchronously-filled
  progress ribbon actually affects, and it is what criterion **15f** is about. No instrument in the
  repo can see it today — QA could not capture the placeholder frame because its window is shorter
  than a 250 ms screenshot interval (`qa-engineer.md:144`). This is the gap worth closing.
- New Gradle module + a new dependency (`androidx.benchmark`, Apache-2.0 — no copyleft, no
  THIRD_PARTY escalation) → **producer schedules**; **android-game-dev** implements;
  **qa-engineer** runs it. Not phase 15's, and not a blocker on anything now.

### 7. I did not edit the spec, and someone must.

`docs/ux-spec.md` is the ux-designer's file and this task forbade me to touch it. Criterion 15e's
§11.1 protocol, §11.2 pass conditions and the `:1868-1869` closing clause all need the rewrite this
report specifies. Route: **producer → ux-designer**, citing this report. Until that edit lands, this
report is the governing text for 15e and the spec is stale — which is a real, if short-lived, risk
worth naming.

## Outputs

- `.planning/reports/phase-15/tech-lead-coldstart-gate.md` — this report (the governing
  specification for criterion 15e until the ux-spec is amended)

## Checks

No build ran for this ruling; it rests on three evidence files, read directly, and on arithmetic I
recomputed rather than accepted. Every figure quoted to me in the brief was verified at source, and
one was corrected.

- `sed -n '160,185p' .planning/reports/phase-15/qa-engineer.md` — pass (probe medians **1832** /
  **1773**, spreads **253** / **225**, loads 6.43 / 8.49; AVD `Pixel_9a` API 36 `emulator-5566`)
- `grep -niE -B3 -A10 'null|957|1014|median' .planning/phases/15*/15-BUILD-EVIDENCE.md` — pass
  (probe medians **957** / **1014**, spreads **172** / **97**; same AVD, same variant)
- `sed -n '305,330p' .planning/phases/06-platform-foundation-build-hardening/06-BUILD-EVIDENCE.md`
  — pass (median **633**, raw range **603–649** = **46 ms**, AVD `Pixel_9a` `emulator-5554` API 36,
  debug; carries phase 6's own "not comparable across devices" and "PLAT-08 … in Phase 14" clauses)
- Arithmetic recomputed from the raw samples, not from the summaries — pass:
  - developer null delta `|1014 − 957|` = **57 ms**; spreads `1057−885` = **172**, `1068−971` = **97**
  - qa null delta `|1832 − 1773|` = **59 ms**; spreads `1926−1673` = **253**, `1939−1714` = **225**
  - phase 6 range `649 − 603` = **46 ms** — the figure I wrongly promoted to a between-arm threshold
  - null delta as a fraction of the pooled median: **5.8%** (57/985.5) and **3.3%** (59/1802.5)
  - absolute level ratio between sittings on one AVD: `1802.5 / 985.5` = **1.83×**
  - proposed threshold on today's floors: `max(2 × 57, 100)` = **114 ms**, `max(2 × 59, 100)` = **118 ms**
- Device identity cross-check (the correction to the brief) — pass: `qa-engineer.md:41` and
  `15-BUILD-EVIDENCE.md:41` name the **same** AVD, model and serial, so the 1.83× absolute gap
  cannot be device class.

## Open issues / risks

- **The ≤ 2 s studio bar on a named physical phone has never been measured for this app.** Every
  cold-start number in the repo is from an emulator. Recommendation: phase 14 measures it on the
  physical device the phase-8 checklist used, and names it in the release evidence.
  **Decided default if no physical device is available at phase 14: the release proceeds on the
  interleaved emulator A/B gate plus the Macrobenchmark task, with the absolute bar recorded as
  unverified in the release evidence rather than silently passed.** This is the one item here with
  a money/taste edge — buying a mid-range test phone is the founder's call, not mine. I have
  **not** queued a founder question; it belongs to the phase-14 release track, and the producer
  should fold it in there rather than opening a second thread now.
- **Two roles measured the same thing on a host neither controlled.** Both nulls are contaminated
  by the other's work — the qa-engineer was measuring cold starts while a `depthSweep` from another
  session held a core. The interleaved protocol tolerates that far better than blocked arms do, but
  the real fix is the same one B1 needs: one build, one worktree, one agent at a time.
- **`reportFullyDrawn()` is a source change to `MainActivity`** and therefore not free. It is the
  single highest-value item in this report for future phases, because it makes 15f decidable
  instead of "verified by reading". Worth its own small plan rather than being smuggled into the
  Macrobenchmark task.
- Criterion **15f** remains "verified by reading, not by the device" (`qa-engineer.md:144`). That
  is honest and, given the instrument, unavoidable today. I am not ruling on 15f here; it is part
  of the phase-15 sign-off.
- This report is **not** my phase-15 sign-off.

## Handoff

Next: **producer** — route §7's spec rewrite to the ux-designer (criterion 15e's §11.1 protocol,
§11.2 pass conditions and the `:1868-1869` closing clause); record that 15e's delta is now a
**follow-up-slice** closing condition and its absolute half is **PLAT-08 / phase-14's**; schedule
the `:macrobenchmark` + `reportFullyDrawn()` task; and carry the named-physical-device question
into the phase-14 release track.

- **ux-designer** — the criterion 15e text, per §3–§5 above. I did not touch `docs/ux-spec.md`.
- **qa-engineer** — do not re-run 15e against this build. When the follow-up slice is ready, run
  §3's protocol: null control first, interleaved arms, `develop` as arm A, and report **VOID**
  rather than a number if `floor > 150 ms`. Your two-runs-of-one-arm precheck is now the
  protocol's first step, not an extra.
- **android-game-dev** — nothing now. Later: the `:macrobenchmark` module and
  `reportFullyDrawn()`, once the producer schedules them.
