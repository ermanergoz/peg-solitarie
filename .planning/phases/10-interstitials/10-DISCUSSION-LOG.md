# Phase 10: Interstitials - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-02
**Phase:** 10-Interstitials
**Areas discussed:** Cadence numbers, Trigger surface, Quiescence pause, No-fill & edges

---

## Todo Cross-Reference

| Option | Description | Selected |
|--------|-------------|----------|
| Keep separate (Recommended) | Phase 9 deliberately kept the Stuck→Results decision separate; Phase 10 gates on "won" regardless | ✓ |
| Fold it in | Decide the Stuck→Results question here since the trigger hangs off the same Results actions | |

**User's choice:** Keep separate. Audio-hardware and Wiegleb-book todos also stay pending (unrelated).

---

## Cadence numbers

### Level gate
| Option | Description | Selected |
|--------|-------------|----------|
| Every 3rd win (Recommended) | ~One ad per 3 levels; casual-puzzle baseline; ad every 3–6 min at typical level length | ✓ |
| Every 2nd win | Higher revenue, risks the "resents" half of the phase goal | |
| Every 5th win | Conservative; leaves monetization mostly to rewarded ads | |

### Wall-clock gate
| Option | Description | Selected |
|--------|-------------|----------|
| Yes — 90s minimum (Recommended) | 3 levels AND 90s must both hold; protects against speed-cleared easy levels | ✓ |
| Yes — 3 minutes | Stronger time floor | |
| No — levels only | Simpler, but fast runs could trigger back-to-back ads | |

### New-install suppression
| Option | Description | Selected |
|--------|-------------|----------|
| Whole 1st session + 5 levels (Recommended) | No ads until first session over AND 5 lifetime levels done — whichever ends later | ✓ |
| First 3 levels only | Lighter runway; a long first session could show ads | |
| Whole 1st session + 10 levels | Most generous; delays first revenue longest | |

### Session cap & definition
| Option | Description | Selected |
|--------|-------------|----------|
| Cap 4/session; 30min bg = new session (Recommended) | Standard analytics session convention; survives process death via persisted last-foreground timestamp | ✓ |
| Cap 4/session; process lifetime = session | Simpler but task-switchers get fresh caps | |
| No session cap | Level/time gates alone; marathon sessions could show a dozen ads | |

---

## Trigger surface

### Eligible actions
| Option | Description | Selected |
|--------|-------------|----------|
| Forward-moving win actions only (Recommended) | Challenge "Next level" + Classic "Catalog" after a win; Replay/Home/stuck-Retry excluded | ✓ |
| Only Challenge "Next level" | Narrowest reading; Classic never shows interstitials | |
| Every Results action after a win | Max inventory but punitive on Replay/Home | |

### Counter scope
| Option | Description | Selected |
|--------|-------------|----------|
| One shared counter (Recommended) | One app, one frequency experience; mode-switching can't dodge the cap | ✓ |
| Separate per mode | More state; doubles effective exposure | |

---

## Quiescence pause

### Presentation
| Option | Description | Selected |
|--------|-------------|----------|
| Dim + input freeze (Recommended) | Scrim + swallowed input; the dim is the signal, no text needed | ✓ |
| Bare input freeze | Screen looks unresponsive | |
| Brief "Ad" label + dim | Most explicit, more UI | |

### Duration
| Option | Description | Selected |
|--------|-------------|----------|
| 500 ms (Recommended) | Exceeds typical double-tap intervals without reading as lag | ✓ |
| 300 ms | Snappier but a slow second tap could reach the ad | |
| 1 second | Max safety, perceptible drag | |

### Navigation ordering
| Option | Description | Selected |
|--------|-------------|----------|
| Ad first, then navigate (Recommended) | Ad over dimmed Results; navigate on dismiss; instant navigation if show fails | ✓ |
| Navigate first, ad on arrival | Ad would sit over the new board — violates INT-01's spirit | |

---

## No-fill & edges

### No-fill cadence handling
| Option | Description | Selected |
|--------|-------------|----------|
| Skip silently, counters keep running (Recommended) | Instant navigation, no pause; next eligible win can show once loaded | ✓ |
| Skip and reset counters | No-fill becomes a player reward; thin ads on flaky networks | |

### Preload timing
| Option | Description | Selected |
|--------|-------------|----------|
| At eligibility, on win (Recommended) | Load when a win makes the next tap eligible; no requests during suppression (mirrors Phase 9 D-09) | ✓ |
| Always keep one loaded | Wasted requests during suppressed first session | |
| At Results screen entry | Fewest requests but high no-fill skip rate | |

### Persistence split
| Option | Description | Selected |
|--------|-------------|----------|
| Persist all but session count (Recommended) | DataStore: wins-since-last, last-ad timestamp, lifetime wins, first-session flag, last-foreground timestamp. In-memory: session ad count (derivable) | ✓ |
| Persist everything including session count | Airtight vs kill-to-dodge, more persisted state | |

---

## Claude's Discretion

- DataStore keys/schema for cadence state
- Show-failure mid-presentation handling and reload cadence after failed load
- Scrim styling within Candy Clay theme
- Policy-object vs gateway boundary (whether vs how)
- Possible one-win-early preload if ad-ready rates need it
- Win-counter increment semantics relative to eligible taps
- Debug/test affordances for forcing the cadence

## Deferred Ideas

- **Stuck finishes → Results decision** — kept separate (existing pending todo); D-06 gates on "won" so it stays robust either way.
- No new deferred ideas surfaced — discussion stayed within phase scope.
