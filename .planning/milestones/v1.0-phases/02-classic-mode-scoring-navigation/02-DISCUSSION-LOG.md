# Phase 2: Classic Mode, Scoring & Navigation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-28
**Phase:** 2-classic-mode-scoring-navigation
**Areas discussed:** Scoring rules, Results screen, Home & catalog, Onboarding

---

## Scoring rules

### Star thresholds across the four boards
| Option | Description | Selected |
|--------|-------------|----------|
| Absolute pegs left | 1 peg = 3★, 2–3 = 2★, finished-but-more = 1★, same rule on every board | ✓ |
| Relative to board size | Stars from % of pegs cleared | |
| Per-board thresholds | Hand-tuned cutoffs per board | |

**User's choice:** Absolute pegs left
**Notes:** Matches the canonical "solve to one peg" goal; board-agnostic and easy to explain.

### Does the target hole matter for top stars?
| Option | Description | Selected |
|--------|-------------|----------|
| Peg count only | Target hole is flavor, not scored | ✓ |
| Target-aware 3★ | 3★ requires 1 peg AND on target, else capped at 2★ | |

**User's choice:** Peg count only

### How elapsed time feeds the numeric score
| Option | Description | Selected |
|--------|-------------|----------|
| Peg base + time bonus | pegsCleared×PEG_VALUE + decaying time bonus; stars & score independent | ✓ |
| Pure speed score | Function of time only | |
| Multiplicative | pegsCleared × timeMultiplier | |

**User's choice:** Peg base + time bonus

### Timer visibility during play
| Option | Description | Selected |
|--------|-------------|----------|
| Visible during play | Running timer in the game top bar | ✓ |
| Hidden until results | Time tracked silently, revealed at the end | |

**User's choice:** Visible during play

---

## Results screen

### Full screen vs modal overlay
| Option | Description | Selected |
|--------|-------------|----------|
| Full screen | Dedicated nav destination (the 5th screen) | ✓ |
| Modal overlay | Dialog over the finished board | |

**User's choice:** Full screen

### Post-level actions
| Option | Description | Selected |
|--------|-------------|----------|
| Replay / Catalog / Home | Three actions; no "Next" | |
| Add "Next board" | Replay / Next board / Catalog / Home | ✓ |
| Replay / Home only | Minimal | |

**User's choice:** Replay / Next board / Catalog / Home (user explicitly wanted Next board added)
**Notes:** Next board = next in catalog order, wrapping at the end.

### Stuck (loss) end flow
| Option | Description | Selected |
|--------|-------------|----------|
| Same results screen | Stuck yields a low star count + score on the same screen | ✓ |
| Distinct retry prompt | Separate "No moves left" framing | |

**User's choice:** Same results screen

### Headline tiering
| Option | Description | Selected |
|--------|-------------|----------|
| Tiered to stars | 3★ "Perfect!", 2★ "Solved!", 1★ "Cleared"/"Stuck" | ✓ |
| Single neutral line | "Level Complete" regardless | |

**User's choice:** Tiered to stars

---

## Home & catalog

### Presenting the stubbed Challenge mode
| Option | Description | Selected |
|--------|-------------|----------|
| Visible, disabled + "Coming soon" | Greyed-out entry with a badge | ✓ |
| Hidden until Phase 3 | Not shown yet | |
| Enabled → placeholder screen | Routes to a "Coming soon" screen | |

**User's choice:** Visible, disabled + "Coming soon"

### Home / catalog structure
| Option | Description | Selected |
|--------|-------------|----------|
| Home + separate catalog | Home = mode select → Classic opens a catalog screen | ✓ |
| Home IS the catalog | Home shows the 4 boards directly | |

**User's choice:** Home + separate catalog

### Catalog item style
| Option | Description | Selected |
|--------|-------------|----------|
| Cards w/ shape preview | Grid of cards rendering each board footprint + name + hole count | ✓ |
| Text list rows | Name + hole count per row | |
| Swipeable carousel | One large board preview at a time | |

**User's choice:** Cards with board-shape preview (reuses Phase 1 BoardCanvas/BoardGeometry)

### Board order (also the Next-board sequence)
| Option | Description | Selected |
|--------|-------------|----------|
| Ascending size | Triangle-15 → English-33 → European-37 → Diamond-41 | ✓ |
| Iconic first | English-33 first | |
| Alphabetical | Diamond → English → European → Triangle | |

**User's choice:** Ascending size

---

## Onboarding

### Form
| Option | Description | Selected |
|--------|-------------|----------|
| Dismissable overlay | Illustrated steps of the jump rule | ✓ |
| Dedicated screen | Full swipeable onboarding screen | |
| Interactive demo | Guided first move on a live mini board | |

**User's choice:** Dismissable overlay

### Trigger
| Option | Description | Selected |
|--------|-------------|----------|
| First time a board opens | Over the board on first gameplay entry | |
| First app launch (over Home) | Once, up front over Home | ✓ |

**User's choice:** First app launch (over Home)

### Permanent re-access
| Option | Description | Selected |
|--------|-------------|----------|
| Yes — Help affordance | A "How to play" icon to re-read the rule | ✓ |
| No — first-time only | Show once, never again | |

**User's choice:** Yes — Help affordance

### "Seen" tracking (persistence is Phase 5)
| Option | Description | Selected |
|--------|-------------|----------|
| One DataStore flag now | Single `onboardingSeen` DataStore-Preferences boolean, forward-compatible to Phase 5 | ✓ |
| In-memory this phase | Re-appears on cold start until Phase 5 | |
| Always-available, no auto-popup | Help button only, no first-time detection | |

**User's choice:** One DataStore flag now
**Notes:** Pulls a single, contained slice of persistence into Phase 2; Phase 5 folds the key into the settings store.

---

## Claude's Discretion

Deferred to research + planning (technical, not user-vision):
- Navigation Compose 2.9.8 wiring and the type-safe five-destination nav graph (Phase 1 currently routes `MainActivity → GameRoute` directly).
- The mechanism by which the selected board reaches `GameViewModel` (today a hardcoded inject) — nav arg + SavedStateHandle, repository lookup by level id, etc.
- The `LevelRepository` interface shape (real Classic source + stubbed Challenge source) so Phase 3's generator drops in behind it.
- European-37 / Triangle-15 / Diamond-41 footprints + starting empty hole(s) (standard definitions; all ≤64 holes).
- Triangle-15 rendering on a non-square lattice (generalizing BoardCanvas/BoardGeometry).
- `ScoreCalculator` placement (pure domain) and its exact constants (PEG_VALUE, TIME_BONUS_CAP, DECAY, the 2★ band).

## Deferred Ideas

- Saved "best stars / best score" badges on catalog cards — needs PERS-01 persistence (Phase 5).
- Challenge mode generation, difficulty curve, sequential level stream — Phase 3 (only the stub + disabled entry here).
- Full settings (sound/haptics) and the broader DataStore/Room store — Phase 5 (only the `onboardingSeen` flag now).
- Interactive onboarding demo — considered for ONB-01, deferred in favor of the dismissable overlay.
- Drag-to-jump input + arc/pop animation polish — already deferred to Slice 2 from Phase 1.
