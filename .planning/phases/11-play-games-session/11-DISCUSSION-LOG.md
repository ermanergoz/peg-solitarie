# Phase 11: Play Games Session - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-03
**Phase:** 11-play-games-session
**Areas discussed:** Sign-in timing, Signed-in feedback, Manual sign-in row, Unknown & offline

---

## Sign-in timing

### Init moment

| Option | Description | Selected |
|--------|-------------|----------|
| MainActivity.onCreate (Recommended) | Right after `consentGateway.bootstrap()`, fire-and-forget; every PGS client getter needs an Activity anyway | ✓ |
| App.onCreate | Earliest possible; `initialize()` fits, but `isAuthenticated` still awaits MainActivity | |
| After Home's first frame | Deferred post-first-composition; zero cold-start cost, signed-in UI flips in later | |

### Re-check policy

| Option | Description | Selected |
|--------|-------------|----------|
| Once per process (Recommended) | One check at launch; state changes only via the manual row | ✓ |
| Every foreground return | Re-run on each `AppForegroundTracker` foreground event | |
| Once per process + retry on failure | Retry once on next foreground if the check errored | |

### Consent gate

| Option | Description | Selected |
|--------|-------------|----------|
| Independent (Recommended) | CONS-02 governs the ads SDK only; privacy policy already discloses Play Games | ✓ |
| After consent resolves | Serialize behind `AdsReadiness` | |

### Play Console readiness

| Option | Description | Selected |
|--------|-------------|----------|
| PGS project + both SHA-1 creds exist | Emulator sign-in verification possible | |
| Project exists, credentials incomplete | Plan now, mark device verification blocked | |
| Nothing created yet | Ship against unit tests and fakes; emulator sign-in is a pre-release checklist item | ✓ |

**User's choice:** all recommended options; Console state is "nothing created yet".
**Notes:** none.

---

## Signed-in feedback

### PGS popup

| Option | Description | Selected |
|--------|-------------|----------|
| Keep stock popup, default position (Recommended) | Zero work; the only launch-time signal sign-in happened | ✓ |
| Keep it, but pin to the bottom | SDK popup gravity so it never overlaps Home tiles or a resumed board | |
| Suppress it if the SDK allows | Researcher verifies; fall back to bottom gravity | |

### Settings state when signed in

| Option | Description | Selected |
|--------|-------------|----------|
| Hide the row entirely (Recommended) | PGS-03 literally; no signed-in chrome | |
| Static 'Signed in to Play Games' line | Non-tappable confirmation, no name | |
| 'Signed in as <display name>' | Fetch display name via PlayersClient; needs loading/fallback state | ✓ |

### Identity on the seam

| Option | Description | Selected |
|--------|-------------|----------|
| playerId only (Recommended) | Keep the seam minimal | ✓ (initially) |
| Add displayName now | Carry it in `SignedIn` for Phase 12 surfaces | |

### Follow-up: name source (conflict resolution)

| Option | Description | Selected |
|--------|-------------|----------|
| Add displayName to SignedIn (Recommended) | Fetch once at sign-in; one source of truth; reverses the playerId-only pick | ✓ |
| Separate seam method | `suspend currentPlayerName()` called by Settings on demand | |
| Drop the name, static line instead | Revert Settings to the generic line | |

**User's choice:** stock popup; "Signed in as <display name>"; the initial "playerId only" pick was superseded by "Add displayName to SignedIn" once the conflict was surfaced.
**Notes:** none.

---

## Manual sign-in row

### Row placement

| Option | Description | Selected |
|--------|-------------|----------|
| New 'Play Games' section (Recommended) | Between General and Privacy; Privacy stays at the bottom (Phase 9 D-14) | ✓ |
| Inside General | Beneath Haptics | |
| Top of the screen, above General | Most discoverable, leads every open with a prompt | |

### Row copy

| Option | Description | Selected |
|--------|-------------|----------|
| 'Sign in to Play Games' / 'Leaderboards, achievements and cloud save' (Recommended) | Names the three things sign-in unlocks | ✓ |
| 'Sign in to Play Games' / 'Compete with friends and keep your progress' | Benefit-led wording | |
| You decide | Planner picks | |

### Tap failure

| Option | Description | Selected |
|--------|-------------|----------|
| Snackbar on failure only (Recommended) | `ClaySnackbarHost` on SDK error; plain dismiss silent; row remains | ✓ |
| Silent, rely on Play Games' own UI | No app-side message | |
| Inline supporting text | Row's supporting line becomes an error hint | |

**User's choice:** all recommended options.
**Notes:** none.

---

## Unknown & offline

### Unknown state

| Option | Description | Selected |
|--------|-------------|----------|
| Nothing until resolved (Recommended) | Section hidden until SignedOut/SignedIn; no flicker | ✓ |
| Show the sign-in row immediately | Treat Unknown like SignedOut | |
| 'Checking Play Games…' placeholder | Non-tappable row while Unknown | |

### No Play services / offline

| Option | Description | Selected |
|--------|-------------|----------|
| SignedOut; row shown; tap fails with snackbar (Recommended) | One state for every not-signed-in cause | ✓ |
| Hide the row when Play services are missing | `GoogleApiAvailability` check once | |
| Retry the silent check when connectivity returns | Connectivity listener; reverses once-per-process for offline | |

### After decline

| Option | Description | Selected |
|--------|-------------|----------|
| Row stays (Recommended) | SDK rate-limits its own prompts | ✓ |
| Hide for the rest of the session | Per-session declined flag | |

### Name lookup failure

| Option | Description | Selected |
|--------|-------------|----------|
| Fallback 'Signed in to Play Games' (Recommended) | `SignedIn(playerId, displayName = null)` is valid | ✓ |
| Stay Unknown until the name arrives | Section hidden until both resolve | |
| Treat as sign-in failure | Report SignedOut | |

**User's choice:** all recommended options.
**Notes:** none.

---

## Claude's Discretion

- Exact snackbar and signed-in-row wording (via `strings.xml`).
- Internal structure of the real implementation, `Task`→`suspend` bridging, dispatchers, display-name sequencing.
- Debug build-type binding (real vs `NoOpGamesService`) given no Console credentials exist.
- Fake `GamesService` shape for ViewModel tests.
- Whether Phase 12 seam methods get real bodies now or later.

## Deferred Ideas

- Leaderboard/achievement rows and UI intents — Phase 12.
- Cloud save — Phase 13.
- Play Console PGS project + dual SHA-1 credentials + tester list — operator work, start now.
- Emulator sign-in verification — pre-release checklist item once the Console project exists.
