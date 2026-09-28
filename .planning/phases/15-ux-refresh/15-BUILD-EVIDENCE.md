# Phase 15 — build evidence

Written by the android-game-dev executing S5–S8 of `15-PLAN.md`, 2026-09-28, on
`feature/phase-15-ux-refresh` in the worktree `/Users/ermanergoz/Desktop/PegSolitarie-p15`.

---

## Cold-start measurement

**Status: NOT RUN — neither arm is recorded, and no delta is claimed.**

The gate was attempted in full and abandoned at the calibration step, because the instrument was
measured to be unable to resolve the threshold it is asked to decide. The evidence for that
statement is below, and it is measurement rather than argument.

### The two arms — not measured

| Field | Arm A (`develop`) | Arm B (`feature/phase-15-ux-refresh`) |
|---|---|---|
| Device | — NOT RUN — | — NOT RUN — |
| API level | — NOT RUN — | — NOT RUN — |
| Variant | — NOT RUN — | — NOT RUN — |
| Command | — NOT RUN — | — NOT RUN — |
| Samples | — NOT RUN — | — NOT RUN — |
| Seeded Challenge rows | — NOT RUN — | — NOT RUN — |
| TotalTime min / median / max | — NOT RUN — | — NOT RUN — |
| Raw samples | — NOT RUN — | — NOT RUN — |

### Pass conditions (criterion 15e, §11.2) — both UNRESOLVED

1. after-median ≤ 950 ms — **UNRESOLVED.** Not evaluated; see below.
2. (after-median − before-median) ≤ 50 ms — **UNRESOLVED.** Arm A was never built, so no
   before-median exists. No subtraction was performed and none may be inferred from this file.

### Why it was abandoned: the instrument fails its own null experiment

Everything up to the measurement was done, and is reusable:

| Step | Result |
|---|---|
| AVD | `Pixel_9a` booted headless on port 5566 (`ro.build.version.sdk` = **36**, `ro.product.model` = `sdk_gphone16k_arm64`) — the instrument criterion 15e names |
| Install | `ANDROID_SERIAL=emulator-5566 ./gradlew :app:installDebug --offline` → BUILD SUCCESSFUL in 19s |
| Launch component | `adb shell cmd package resolve-activity --brief com.erman.pegsolitarie` → **`com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`** — the resolved form §11.1 requires; the `$APP/.MainActivity` shorthand would have named a class that does not exist |
| Seeding | 200 Challenge rows + the 4 Classic rows the app had written; `SELECT COUNT(*) FROM level_result` = **204**, `SUM(stars)` = **413**. Verified on screen: the Home ribbon read `Stars, 413` · `Boards, 4/5` · `Level, 26` |

Then the protocol of §11.1 was run **twice on the same installed build**, back to back, nothing
changed between them. Two runs of one build against itself is a null experiment: its true delta
is zero by construction, so whatever it reports is the instrument's noise.

| Probe | Raw samples (ms), warm-up first | min | median | max | spread |
|---|---|---|---|---|---|
| 1 | *(1111)* 1057, 947, 978, 1018, 922, 957, 885, 923, 1013 | 885 | **957** | 1057 | **172** |
| 2 | *(1058)* 973, 1047, 1068, 1004, 993, 1038, 1025, 1014, 971 | 971 | **1014** | 1068 | **97** |

**The arithmetic that decides it.**

- Within-arm spread: **172 ms** and **97 ms**. Phase 6 calibrated the 50 ms threshold from a
  spread of 46 ms (603–649 ms around a 633 ms median, `06-BUILD-EVIDENCE.md:315-323`). This
  sitting is **2.1× to 3.7×** that.
- Null delta, same build against itself: |1014 − 957| = **57 ms**. The gate asks whether a
  between-arm delta is **≤ 50 ms**. The instrument reports **57 ms of difference where the true
  difference is zero**, so it fails the gate against itself.

A two-arm delta computed on this instrument today would be dominated by host noise and its sign
would be luck. §11.1's rule — *"Do not change instrument mid-comparison — a comparison across
variants or devices means nothing"* — applies in the time dimension too: arm A and arm B would
have been separated by a full debug build of `develop` on a host whose load is set by other
sessions.

**Why the host is not quiet, recorded so the qa-engineer can tell whether it has changed.** The
measurement ran while three other agent sessions were in flight in the phase-14 worktrees:
`load averages: 5.38 6.49 5.88` then `7.24 6.25 5.88` on a 10-core host, with an `opencode serve`
process at ~60% CPU, three `claude` processes, and a **second** emulator (`Pixel_9`,
`emulator-5560`) that has been up for 4 days 14 hours at ~12% CPU. None of those are this
developer's to stop: the second emulator may hold another role's device evidence, so it was left
running, and the Pixel_9a this measurement booted was shut down afterwards.

### What is NOT claimed

Neither probe is arm B. Both were taken on this branch's build, so it is tempting to read 957 or
1014 as "the after number" — **do not**. They were taken to characterise the instrument, on a
host that is measurably too noisy for the gate, and using them as arm B would smuggle a
contaminated number into a pass/fail decision. Criterion 15e needs two arms measured under one
set of conditions, and neither exists.

### One signal worth carrying forward, explicitly as a hypothesis and not a finding

Both probe medians (957 ms, 1014 ms) sit at or above the 950 ms absolute bar. If that survives on
a quiet host it is a **condition (1)** question, and §11.2 rules that a condition-(1)-only failure
is **PLAT-08 / Phase-14's** and escalates to the producer rather than being paid for out of this
refresh's visual budget. It is recorded here as a hypothesis to test, not a result: these numbers
are contaminated, and the pre-SDK 633 ms reference predates UMP, AdMob, Play Games, leaderboards
and cloud save.

### Hand-off — how to run it, with this attempt's corrections folded in

For the qa-engineer, on a host with no other build or agent session running. Run `uptime` first
and record it; if the 1-minute load average is above ~2 on a 10-core host, wait.

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
APP=com.erman.pegsolitarie          # applicationId (app/build.gradle.kts:279) — "-arie"
DB=peg_solitaire.db                 # ProgressPersistenceKeys.kt:20

$ANDROID_HOME/emulator/emulator -avd Pixel_9a -port 5566 -no-window -no-boot-anim &
export ANDROID_SERIAL=emulator-5566

# Arm A: build develop in a throwaway detached worktree so no branch is checked out twice.
#   git worktree add --detach /tmp/p15-armA develop
# Arm B: this branch's worktree. Re-install between arms; re-seed after every install.
./gradlew :app:installDebug --offline

# Launch once so Room creates the database, then force-stop before seeding.
COMPONENT="$(adb shell cmd package resolve-activity --brief "$APP" | tail -1 | tr -d '\r')"
adb shell am start -W -n "$COMPONENT"; sleep 5; adb shell am force-stop "$APP"
```

**Seeding — §11.1's on-device path does not work on this image and its host-side fallback is
incomplete.** `adb shell which sqlite3` prints nothing on `Pixel_9a`/API 36, so the fallback is
the only route; and the fallback as written pulls **only** `databases/$DB`, which on a
WAL-journalled Room database is a 4 KB stub whose `.schema` is empty — the tables live in the
`-wal` file. Pull the WAL too:

```bash
adb shell am force-stop "$APP"
adb exec-out run-as "$APP" cat databases/$DB      > /tmp/seed.db
adb exec-out run-as "$APP" cat databases/$DB-wal  > /tmp/seed.db-wal    # <-- the missing line
sqlite3 /tmp/seed.db <<'SQL'
PRAGMA wal_checkpoint(TRUNCATE);
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i+1 FROM n WHERE i < 200)
INSERT OR REPLACE INTO level_result (mode, levelId, stars, bestTimeMs, bestScore, completedAt)
SELECT 'challenge', CAST(i AS TEXT), 1 + (i % 3), 60000 + i, 1000 + i, 1700000000000 + i FROM n;
PRAGMA wal_checkpoint(TRUNCATE);
SELECT 'rows=' || COUNT(*) FROM level_result;
SQL
cat /tmp/seed.db | adb shell "run-as $APP sh -c 'cat > databases/$DB'"
adb shell "run-as $APP sh -c 'rm -f databases/$DB-wal databases/$DB-shm'"
```

Then, per arm:

```bash
for i in $(seq 1 10); do
  adb shell am force-stop "$APP"; sleep 2
  adb shell am start -W -n "$COMPONENT" | grep TotalTime
done
# discard sample 1; record min / median / max AND the spread of the remaining 9
```

**Add one step §11 does not have, and stop if it fails:** before trusting either arm, run the
10-sample loop **twice on the same arm without changing anything**. If the two medians differ by
more than 50 ms, or either spread exceeds ~50 ms, the host cannot resolve this gate that day —
record that, and do not compute a delta. That check is what turned this attempt back, and it costs
eight minutes.

**Precision caveat (carried verbatim from `06-BUILD-EVIDENCE.md:325-327`):** `am start -W`
displayed-time is coarse. It is affected by host load, emulator thermal/background state, and is
**not comparable across devices**. Both arms must be measured on the same AVD (`Pixel_9a`,
API 36) on the same host, in one sitting, for the comparison to mean anything.

---

## Criterion 15f — the visual check

**Status: NOT SETTLED by this developer. It belongs to the qa-engineer's device session.**

§11.3 requires 15f to be checked "at the same sitting" as the timing number; since the timing
number was not taken, 15f was not taken either. What *was* observed on `Pixel_9a` is recorded
below as a smoke check, not as the criterion:

- Home rendered the five bands with real values against the 200-row seed (`Stars, 413` ·
  `Boards, 4/5` · `Level, 26`), with **no spinner and no blank band** at any point observed.
- Whether the ribbon paints `—` placeholders **on first paint before the data arrives**, and
  whether the fill causes no layout shift, is the part 15f actually asks and the part a
  screenshot after the data has landed cannot answer. It needs a device session watching the
  first frame.

---

## Smoke verification of the S7 chrome (developer's own, not a QA sign-off)

Taken on `Pixel_9a` / API 36 from the arm-B build, to establish that the chrome does more than
compile. Artefacts were written to the session scratchpad and **not** committed (rules/testing.md).

| Observation | Evidence |
|---|---|
| No Material top app bar on gameplay | The strip renders as a transparent row: circular back chip, "Peg Solitaire" title, `00:00` timer pill. No bar background, no bar elevation. |
| All four actions in the dock, in the thumb zone | Accessibility bounds `Undo [173,2125]`, `Hint [393,2125]`, `Show possible moves [614,2125]`, `Restart Board [834,2125]` on a 1080×2424 screen — the dock sits at ~88% of screen height. |
| Criterion 20 — glyphs | Hint renders a lightbulb (no magnifying glass anywhere); Show-moves renders a peg with two trailing dots, matching no other glyph in the app. |
| Criterion 21 — fill, not tint | Show-moves **ON** renders as a filled `primaryContainer` disc; after one tap **OFF** renders as a transparent disc with an `outline` ring. The fill is the state channel. |
| Criterion 22 — Undo disabled at game start | Undo renders flat/`surfaceVariant` with `canUndo = false`, beside the two filled controls. |
| Oracle 2 (D-03f) live | Home → Classic reached the catalogue in one tap and each card still carried its star pips (`content-desc="3 of 3 stars"`). |

**One pre-existing, app-wide accessibility observation, explicitly not introduced by this phase.**
In `uiautomator`'s projection the *clickable* node and the *labelled* node are separate: for every
dock action the clickable node is 147 px (56 dp) and carries no label, while the label sits on a
74 px (28 dp) `clickable="false"` child. The **untouched** Classic catalogue's `TopAppBar`
navigation icon shows the identical split (`Navigate back`, `clickable=false`, `[43,195][106,258]`
= 24 dp), so this is how this Compose version maps `IconButton { Icon(contentDescription) }` into
the Android accessibility tree, not something S7 caused — and S7's clickable target grew from
48 dp to 56 dp. Compose's own merged tree is correct, which is why `ButtonSoundTest` can click
these controls by content description and still passes byte-unedited. **Criterion 26 can only be
settled by a real TalkBack pass**, which `uiautomator` is not.

---

## Deviation found in S6 while verifying its claims — reported, not fixed

`15-PLAN.md` §S6 specifies "`verticalScroll` **+ a weighted spacer** between bands 2 and 3, so
slack goes *above* the Continue chips and the tiles stay in the thumb zone". The committed code
(`HomeScreen.kt`, `HomeBands`) uses a **fixed** `Spacer(Modifier.height(spacing_lg))` instead, and
its KDoc states the reason: `Modifier.weight` does not work inside a `verticalScroll` column.

The reason is technically correct, but the consequence is visible: with the column top-aligned in
a `fillMaxSize` scroll container, all slack falls to the **bottom**. On `Pixel_9a` with no resume
saved, the two mode tiles occupy roughly 36–54% of screen height and the empty footer band takes
the bottom ~45%. Criterion 15 ("on a 6.7-inch phone the two mode tiles sit in the lower half")
is therefore at risk.

Not fixed here, deliberately: it is a layout judgement whose acceptance criteria (15, 15a) are
device checks, no unit test can verify the fix, and pushing the tiles down interacts with
criterion 15a in the opposite direction on a 360×640dp screen. The standard recipe if the producer
dispatches it is `BoxWithConstraints { Column(Modifier.verticalScroll(s).heightIn(min = maxHeight))
{ … Spacer(Modifier.weight(1f)) … } }`, which gives a weighted spacer that collapses to zero when
the content already overflows.
