# Phase 13: Cloud Save - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-15
**Phase:** 13-cloud-save
**Areas discussed:** Sync timing & restore feedback, Payload shape & size cap, Play Games listing metadata, Account switch & verification

---

## Sync timing & restore feedback

### When should the app push progress to the cloud?

| Option | Description | Selected |
|--------|-------------|----------|
| After every solved level | Piggyback on the existing ProgressSubmitter pass after recordResult; one write per solve | ✓ |
| On app background only | Batch on ProcessLifecycleOwner background; a force-kill before backgrounding loses the session's solves until next time | |
| Both: every solve plus background flush | Push after each solve and flush pending on background | |

**User's choice:** After every solved level

### When should the app pull (load and merge) the cloud save?

| Option | Description | Selected |
|--------|-------------|----------|
| Once, when session resolves SignedIn | Rides the Phase 11 once-per-process sign-in moment | ✓ |
| At SignedIn plus every foreground return | Also re-pull on foreground; more SDK traffic, re-pulls under an open game | |
| Merge-on-open with MANUAL policy on every push | Each push merges cloud into local first; pull at SignedIn still needed | |

**User's choice:** Once, when session resolves SignedIn

### When a pull merges cloud progress into this device, should the player see anything?

| Option | Description | Selected |
|--------|-------------|----------|
| Fully silent | No toast, no dialog; flows update the UI; Welcome back popup is the only signal | ✓ |
| Snackbar only when something actually changed | One-line snackbar only if the merge improved a local value | |
| Snackbar on every successful pull | Confirms the sync even when nothing changed | |

**User's choice:** Fully silent

### In-progress Challenge game at level 10, cloud ladder at 50 — what happens to the in-progress game?

| Option | Description | Selected |
|--------|-------------|----------|
| Keep it; ladder jumps to 50 underneath | Continue still resumes level 10; solving it calls advancePast(10) = max(50, 11) | ✓ |
| Discard the in-progress game when below the merged ladder | Clear the Challenge resume snapshot; deletes local state on a cloud event | |
| You decide | Planner picks within the no-data-loss constraint | |

**User's choice:** Keep it; ladder jumps to 50 underneath

---

## Payload shape & size cap

### How should the cloud payload handle a very long Challenge ladder?

| Option | Description | Selected |
|--------|-------------|----------|
| Lossless by default, trim only at the cap | Every best compactly encoded; drop lowest Challenge levels only if the confirmed cap is approached | ✓ |
| Lossy window by design | Full Classic + ladder + aggregates + last K Challenge levels | |
| Aggregates only, no per-level Challenge bests | Tiny payload but fails CLOUD-01 for Challenge levels | |

**User's choice:** Lossless by default, trim only at the cap

### Does play_session telemetry ride in the cloud payload?

| Option | Description | Selected |
|--------|-------------|----------|
| No, stays local | Achievements already live server-side; payload stays a pure join-semilattice | ✓ |
| Yes, sync the derived flags only | Four booleans merged by OR | |
| Yes, sync the rows | Full history; unbounded growth; excluded by milestone scope | |

**User's choice:** No, stays local

### What encoding should the snapshot bytes use?

| Option | Description | Selected |
|--------|-------------|----------|
| kotlinx-serialization JSON with a version field | Same library/shape as the resume blob; ~40 bytes per level; schemaVersion field | ✓ |
| kotlinx-serialization CBOR or ProtoBuf | ~4x smaller; gives up readability; adds an artifact | |
| You decide | Planner picks after confirming the cap | |

**User's choice:** kotlinx-serialization JSON with a version field

### Which value is the Challenge ladder position in the payload?

| Option | Description | Selected |
|--------|-------------|----------|
| The persisted currentLevel, merged by max | Own field carrying ChallengeProgress.currentLevel; tile on a new device equals the old one | ✓ |
| Derive it from the highest synced Challenge best + 1 | No separate field; fragile under trimming and future ladder changes | |
| You decide | Planner picks with the tile-equality constraint | |

**User's choice:** The persisted currentLevel, merged by max
**Notes:** Verified during discussion that no UI path calls `ChallengeProgress.reset()`, so the ladder is purely monotonic today.

---

## Play Games listing metadata

### What should the cloud save's cover image be?

| Option | Description | Selected |
|--------|-------------|----------|
| Bundled board art from the launcher icon | Rasterize the approved English-33 vectors once into a PNG under 800 KB | ✓ |
| Rendered snapshot of the player's current Challenge board | Draw the current ladder board to a bitmap at push time | |
| You decide | Planner picks; PNG under 800 KB on every commit | |

**User's choice:** Bundled board art from the launcher icon

### Where does the played time metadata come from?

| Option | Description | Selected |
|--------|-------------|----------|
| Sum of elapsedMs over play_session, merged by max | One SQL SUM over solved plays; no new state; max never double-counts | ✓ |
| New lifetime play-time counter in DataStore | Accurate for abandoned games; new ticking state and a merge question | |
| Sum of bestTimeMs over level_result | Already in the payload but heavily undercounts | |

**User's choice:** Sum of elapsedMs over play_session, merged by max
**Notes:** Captured in CONTEXT.md as a `playedTimeMs` payload field so the metadata can reflect both devices.

### What should the snapshot description say in the Play Games saves list?

| Option | Description | Selected |
|--------|-------------|----------|
| Ladder + stars + Classic boards | "Challenge level 57 · 123 stars · 4/5 Classic boards" from Phase 12 aggregates | ✓ |
| Ladder only | "Challenge level 57" | |
| Ladder + last played date | Play Games already shows the modified time | |

**User's choice:** Ladder + stars + Classic boards

### Description locale?

| Option | Description | Selected |
|--------|-------------|----------|
| Device locale at commit time | Through strings.xml like every other string | ✓ |
| Always English | Hard-coded; contradicts the no-literal-strings rule | |

**User's choice:** Device locale at commit time

---

## Account switch & verification

### Device progress last merged with player X; a later launch resolves to player Y. What happens?

| Option | Description | Selected |
|--------|-------------|----------|
| Merge anyway; local progress is device-owned | Union into Y's save like a first sign-in; Y inherits X's progress on this device | |
| Dialog for this one case | Track last merged playerId; ask once: use Y's cloud (replace) or keep this device's progress (merge) | ✓ |
| Pull only, never push, until the ids match | Avoids contamination but silently stops syncing for Y | |

**User's choice:** Dialog for this one case
**Notes:** This is the single dialog the v1.1 milestone requirements reserved for the "different PGS profile" case.

### When the player picks "Use Y's cloud progress" (replace local), how far does the replace go?

| Option | Description | Selected |
|--------|-------------|----------|
| Bests, ladder and in-progress games all reset to Y | Wipe level_result, play_session, ladder and both resume snapshots, then import | ✓ |
| Bests and ladder only; keep in-progress games | Y would resume X's half-finished boards | |
| You decide | Planner picks; dialog copy must state what is replaced | |

**User's choice:** Bests, ladder and in-progress games all reset to Y

### Where and when does the profile-swap dialog appear; syncing meanwhile?

| Option | Description | Selected |
|--------|-------------|----------|
| Home only, sync paused until answered | Same shape as the Phase 11 opt-in prompt; dismissal = keep this device's progress | ✓ |
| Wherever the player is when the mismatch is detected | Interrupts play; ruled out by Phase 11 D-16 | |
| Home only, but sync pulls meanwhile | Early pull buys nothing if replace is chosen | |

**User's choice:** Home only, sync paused until answered

### How should the two-device merge be verified?

| Option | Description | Selected |
|--------|-------------|----------|
| Two emulators, one tester account | Pixel_9a + Pixel_9 AVDs on the existing tester account; profile-swap stays a recorded gap | ✓ |
| Emulator plus physical device, one account | Real-network behaviour; physical device was unavailable for Phase 10 | |
| JVM tests only, device run deferred | Cheapest now; least reversible slice argues against it | |

**User's choice:** Two emulators, one tester account

---

## Claude's Discretion

- Shape and location of the `:domain` merge function and its exhaustive property tests.
- Home of the cloud-push-pending marker and whether one submitter pass drains both submissions and the cloud push.
- Snapshot identity (fixed name), `SnapshotsClient` plumbing and conflict-handling seam inside `platform/games/`.
- Import mechanics into Room/DataStore and the guarantee that "New best!" / celebration never fire on import.
- Whether a cloud import re-fires the Phase 12 submission pass (recommended yes).
- Payload field names, trim threshold margin, cover PNG generation, dialog copy, threading/timeouts, test doubles.

## Deferred Ideas

- Cloud-syncing the in-progress resume snapshot (v1.2 per milestone requirements).
- Settings sync-status row or "Progress restored" snackbar.
- Rendered per-player board cover image.
- Re-pull on every foreground return.
- Syncing derived skill-achievement flags.
