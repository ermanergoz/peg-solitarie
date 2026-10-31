---
phase: 14-release-readiness
type: measurement-spec
status: pending
requirement: PLAT-08
owner: release-manager (spec) / qa-engineer (execution)
---

# Phase 14 — PLAT-08 cold start: the baseline, the procedure, the budget

**PLAT-08:** *App cold-start time is measured before and after SDK integration and stays within an
agreed budget.*
**ROADMAP success criterion 4:** *Cold-start time on the SDK-integrated build is measured against
the pre-SDK baseline captured in Phase 6 and is within the agreed budget.*

---

## 1. The pre-SDK baseline: it exists

Phase 6 was told to capture it and **did**. It is committed, complete, and honestly caveated — so
this section is a location, not a finding.

**Where:** `.planning/phases/06-platform-foundation-build-hardening/06-BUILD-EVIDENCE.md`,
§ *Cold-start baseline*. Produced by plan `06-08` (Task 1, operator-run), summarised in
`06-08-SUMMARY.md`, and verified during the phase's own verification pass
(`06-VERIFICATION.md` row 8: *"Cold-start baseline has device (`Pixel_9a`, API 36), variant,
command, 9 samples, min/median/max, and an explicit precision caveat"*).

**What it says, verbatim in substance:**

| Field | Value |
|-------|-------|
| Captured | 2026-08-27, **before** plan 06-09 put any SDK artifact on the dependency graph |
| Device | AVD `Pixel_9a` (`emulator-5554`) |
| API level | 36 |
| Variant | **debug** (`:app:installDebug`) |
| Command | `adb shell am force-stop com.app.pegsolitaire`, ~2 s pause, `adb shell am start -W -n com.app.pegsolitaire/.MainActivity`, ×10 |
| Samples | 10 taken, first discarded as warm-up, **9 recorded** |
| Metric | `am start -W` **TotalTime** (ms) |
| Result | min **603** · median **633** · max **649** |
| Raw | 649, 633, 629, 633, 649, 622, 609, 639, 603 (discarded warm-up: 721) |

**Its own caveat, which governs everything below:** *"`am start -W` displayed-time is coarse. It is
affected by host load, emulator thermal/background state, and is not comparable across devices.
Phase 14 must re-measure on the same AVD (`Pixel_9a`, API 36) on the same host for the PLAT-08
comparison to mean anything. This is an honest smoke-level baseline, not a benchmark-grade
number."*

The ordering constraint that made it trustworthy was also honoured: D-14 required the baseline to
land in an *earlier plan* than the one adding the SDK artifacts, and it did —
`git log -S "libs.ads.mobile.sdk" -- app/build.gradle.kts` puts the SDKs on the graph at
**`3921135`** (*"chore(06-09): put the three Slice-2 SDK artifacts on the graph, unreferenced"*),
whose parent is `33c2d0d`, *after* the 06-08 baseline commit. The before-picture is genuinely
before.

### Four properties of this baseline that constrain phase 14

These are findings, and each one changes what may legitimately be compared to `633 ms`.

1. **It is a *debug* measurement.** The only valid PLAT-08 comparison is therefore
   **debug-to-debug**. A release-variant number is not comparable at all: R8 full mode,
   `shrinkResources`, no `debuggable` flag and a different JIT/dex-layout profile all move the
   number in ways that have nothing to do with SDK integration. Phase 6 measured a one-off release
   launch at **890 ms** and was careful to label it *"uninstrumented single launch, not a
   baseline"* — that number is not a comparand and must not be used as one.
2. **The measured component name has changed.** The baseline used
   `com.app.pegsolitaire/.MainActivity`. D-20 changed the `applicationId` to
   `com.erman.pegsolitarie` while leaving the code namespace at `com.app.pegsolitaire`, so the
   re-measure command is `am start -W -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`.
   `06-CONTEXT.md` recorded this ahead of time and states the comparison stays valid: *"Same code,
   same device."*
3. **The baseline launch did no Play Games sign-in, no consent bootstrap and no ads init, because
   none of it existed.** Today `MainActivity` calls `consentGateway.bootstrap()` **before**
   `setContent`, and phase 11 added silent sign-in on the launch path. So part of any measured delta
   is *the feature set*, not a regression — it is work the app is supposed to be doing now. A single
   number cannot separate the two, which is why §3 specifies more than one measurement condition.
4. **The baseline device is the wedged control AVD, and that creates a hard sequencing rule.**
   `Pixel_9a` is the AVD the phase-13 tech-lead sign-off describes as *"wedged and untouched,
   correctly preserved as a control"* for a Play Games Saved Games fault judged to be an
   environment problem. It still boots and still runs the app — the wedge is in GMS's Saved Games
   state, not the AVD — so it remains measurable.
   **But it is the only valid PLAT-08 comparand in existence, and clearing Google Play services
   data on it, wiping it, or deleting it destroys that comparand permanently.**
   > **Measure cold start on `Pixel_9a` before anyone attempts to unwedge, reset or delete that
   > AVD.** If a future investigation wants it cleared, PLAT-08 must be measured first. This is
   > the single cheapest risk on the whole phase to retire and the most expensive to recover.

### If the comparand is lost anyway — the honest recovery, not a substitute

Do **not** measure HEAD on a different AVD and present the delta against `633 ms`. The baseline's
own caveat forbids it and the resulting number would be measurement noise dressed as a finding.

The recoverable path is to **rebuild both halves of the comparison on whatever clean AVD exists**:

```sh
# a pre-SDK tree, at the commit immediately before the SDKs joined the graph
git worktree add ../PegSolitarie-presdk 33c2d0d
# measure HEAD-debug and 33c2d0d-debug on the SAME clean AVD, same host, same session,
# alternating runs to absorb host drift; report both medians and the delta.
```

That reconstructs a valid before/after pair at the cost of one extra build. It replaces the
`633 ms` figure rather than pretending to compare to it, and the report says so.

---

## 2. What is explicitly out of scope

Macrobenchmark / `androidx.benchmark` is **not** used. `06-VALIDATION.md` recorded the reason as
assumption A7 and accepted the manual path: *"No benchmark module, no `androidX` test source set,
no device tooling"* — and `ls app/src/` still returns `main` and `test` only. Standing up a
benchmark module and an `androidTest` source set purely to measure one number at the end of a
milestone is a larger change than the thing it measures, and it would produce a number that is
*also* not comparable to `633 ms`. The manual `am start -W` path is kept precisely because it is
the path the baseline used.

---

## 3. Measurement procedure

**Executed by:** qa-engineer. **Device work only; no source change.**
**Recorded in:** `.planning/phases/14-release-readiness/14-COLD-START-EVIDENCE.md`, appended, with
raw samples — not a bare median.

### 3.0 Host and device hygiene (non-optional)

The baseline's caveat names host load as a confounder, and phase 13 hit it for real: the producer's
gate evidence records that *"host load near 80 caused the emulator hang detector to kill two
running emulators mid-test."*

- [ ] Exactly **one** emulator running: `Pixel_9a`. Shut every other AVD down first.
- [ ] No Gradle daemon build, no `check`, no IDE sync in flight. Record the host load average at
      the start and end of the run.
- [ ] `adb devices` shows exactly one device.

### 3.1 Build and install — debug, the comparand variant

```sh
./gradlew :app:installDebug
adb shell dumpsys package com.erman.pegsolitarie | grep -E 'versionCode|versionName'
```

No ad IDs and no keystore are needed: **debug builds require none of the founder's blocked items.**
This is the one measurement in phase 14 that is fully runnable today.

### 3.2 The sampling loop — the baseline's method, reproduced verbatim

Reproduce it, do not improve it. A better method produces a number that cannot be compared.

```sh
COMP=com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity
for i in $(seq 1 10); do
  adb shell am force-stop com.erman.pegsolitarie
  sleep 2
  adb shell am start -W -n "$COMP" | awk -F': ' '/TotalTime|WaitTime/ {printf "%s ", $2} END {print ""}'
done
```

- 10 iterations; **discard the first** as warm-up; record the remaining **9**.
- Metric of record: **TotalTime**. Capture `WaitTime` alongside it (plan 06-08 asked for both; only
  TotalTime was recorded then) — a large `WaitTime − TotalTime` gap points at system contention
  rather than at the app.
- Report **min / median / max and all nine raw samples**, exactly as the baseline does. A median
  with no dispersion cannot be argued with, which means it cannot be checked either.
- Cross-check the first run against logcat: `adb logcat -d -s ActivityTaskManager | grep Displayed`.
  The `Displayed` line is an independent read of the same event; if the two disagree by more than
  the sample spread, something in the run is wrong.

### 3.3 The four conditions

| # | Condition | Device state | Compared to | Why it exists |
|---|---|---|---|---|
| **C1** | **Comparand — steady state** | `Pixel_9a`, debug, online, signed in as tester, onboarding already seen | **`633 ms`** — this is the PLAT-08 number | The returning-player launch. The only condition that is judged against the baseline. |
| **C2** | Attribution — offline | identical, but **airplane mode** | C1 | Isolates network-bound init (silent sign-in, consent refresh, ad prefetch) from local work. `C1 − C2` is the network-dependent share of any regression. |
| **C3** | First run | after `pm clear`, consent form + onboarding on the path | **nothing** — reported absolute | The baseline was a steady-state launch and no consent form existed, so there is no before-picture. Reported for the 2 s ceiling only. |
| **C4** | Release variant, store build | the downloaded internal-track build (T-13 in `14-RELEASE-PLAN.md`) | **nothing** — reported absolute | What a player actually gets. Not comparable to a debug baseline; recorded against the studio's 2 s ceiling and against phase 6's 890 ms single-launch note as a sanity read. |

C1 and C2 are runnable today. C3 is runnable today. **C4 requires the founder's Step 6/7 and the
track download**, and is the only part of PLAT-08 that is founder-blocked.

---

## 4. Proposed budget

Three thresholds, and a rule for what happens when the first one is missed.

### Primary gate — PLAT-08 pass/fail

> **C1 median TotalTime ≤ 950 ms** on AVD `Pixel_9a`, API 36, debug variant, same host,
> measured by §3.2.

That is the baseline median (`633 ms`) **+ 50%**, i.e. **317 ms of headroom** for three SDKs.

Why 50% and not 10% or 200%:

- **The noise band is known and small.** The baseline's own nine samples span 603–649 ms around a
  median of 633 — **±3.6%**. A budget has to sit far outside that band or it measures the host, not
  the app; 317 ms is roughly **12× the observed noise**. A 10% budget (696 ms) would be inside
  two bad samples of noise and would fail for reasons nobody could act on.
- **317 ms is genuinely generous for what was added, because this app already paid for cold start
  once.** Every automatic-initialisation path on the critical path was deliberately removed or
  never existed: `PlayGamesInitProvider` is stripped with `tools:node="remove"` **specifically for
  production cold start** (D-17, and the manifest comment says so — there is a build gate asserting
  that comment), and GMA Next-Gen declares no ContentProvider at all, requiring an explicit
  `MobileAds.initialize()` which phase 9 gates behind consent resolution. So the new synchronous
  pre-first-frame work in C1 is a larger Hilt graph, more classes to load out of a bigger dex, and
  `consentGateway.bootstrap()`. If that costs more than 317 ms on a debug build, something is on
  the critical path that should not be — which is a finding worth having, and that is what a budget
  is for.
- **It leaves the studio-wide ceiling intact.** 950 ms on an emulator is comfortably under the
  studio's "launches in under 2 seconds on a mid-range phone", with room for a real device to be
  slower than this AVD.

### Secondary gate — the run has to be trustworthy

> **max sample ≤ 1.25 × median** across the nine recorded samples.

A pass produced by one lucky run is not a pass, and a fail produced by one host hiccup is not a
fail. The baseline itself passes this trivially (649 / 633 = 1.03), so it is not a moved goalpost.
If a run misses it, the run is discarded and repeated after re-checking §3.0 — it is a
measurement-quality gate, not a product gate.

### Hard ceiling — device-independent, from the studio product baseline

> **Median ≤ 2000 ms in every condition measured, on every device measured**, including C3 (first
> run with the consent form) and C4 (release build).

This is the studio's own product baseline and it applies regardless of what the comparand says. A
build inside the 950 ms delta budget but over 2 s in C3 is still a failure, just a different one.

### When C1 misses the budget — attribution before code

A C1 median above 950 ms **does not fail the phase automatically**; it escalates with C2 attached,
because C2 says what to do about it:

| C1 | C2 | Reading | Remedy |
|---|---|---|---|
| > 950 | ≤ 950 | Network-bound init is on the critical path | Defer sign-in / consent refresh past first frame; no architectural change |
| > 950 | > 950 | Local work — class loading, Hilt graph construction, eager singletons | Lazy injection at the seams; `Provider<T>` for the platform gateways |
| ≤ 950 | > 950 | Measurement fault (airplane mode changed more than the network) | Re-run §3.0 |

Distinguishing the first two rows before anyone edits a file is the entire reason C2 is in the
procedure. Measuring one number and guessing at its cause is how a cold-start budget turns into a
week of speculative optimisation.

---

## 5. Definition of done for PLAT-08

- [ ] C1, C2 and C3 measured on `Pixel_9a`/API 36/debug, nine raw samples each, appended to
      `14-COLD-START-EVIDENCE.md` with host load recorded.
- [ ] C1 median compared to the baseline's `633 ms`, with the delta stated in both ms and percent.
- [ ] C1 median ≤ 950 ms, or an escalation carrying the C1/C2 attribution table above.
- [ ] Every condition's median ≤ 2000 ms.
- [ ] C4 measured on the store-downloaded build, reported absolute and explicitly labelled
      non-comparable to the debug baseline.
- [ ] `REQUIREMENTS.md` PLAT-08 ticked with the evidence path, and `ROADMAP.md` criterion 4 closed.
- [ ] The §1.4 sequencing note discharged: the measurement happened while `Pixel_9a` was still
      intact, and the report records that it did.
