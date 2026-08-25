# Phase 1: Domain Foundation & Playable Board - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-28
**Phase:** 1-domain-foundation-playable-board
**Areas discussed:** First board choice, Move interaction feel, End-state signal, Jump/removal animation

---

## First Board Choice

| Option | Description | Selected |
|--------|-------------|----------|
| English-33 | Iconic cross, 33 holes, single center-empty. Recognizable; pure orthogonal-grid; also a Phase-2 classic board. | ✓ |
| Triangle-15 | Smallest board, lightest fixture, but triangular geometry is an odd first case on an orthogonal grid. | |
| Diamond-41 | 41-hole diamond, larger fixture, not the canonical first impression. | |

**User's choice:** English-33
**Notes:** Recommended option; doubles as Phase-2 classic-board work.

---

## Move Interaction Feel

### Input model
| Option | Description | Selected |
|--------|-------------|----------|
| Tap-only | Tap peg to select+highlight, tap hole to jump. No drag. Simplest, reliable hit-testing. | ✓ |
| Tap + drag | Tap-to-jump and drag-to-hole both work. More tactile, doubles gesture/animation surface. | |

### Auto-jump on single legal move
| Option | Description | Selected |
|--------|-------------|----------|
| Always require destination tap | Consistent two-step in every case; no surprise moves. | ✓ |
| Auto-jump on single option | One tap when only one move exists; inconsistent, accident-prone. | |

### Deselect
| Option | Description | Selected |
|--------|-------------|----------|
| Tap-elsewhere deselects | Tapping selected peg / other peg / empty cell clears or re-selects. | |
| Only re-select, no explicit deselect | Selection only changes by tapping another peg. | |
| (free text) Tapping the selected peg again only | Deselect happens only by re-tapping the currently-selected peg. | ✓ |

**User's choice:** Tap-only; always two-step (no auto-jump); deselect by re-tapping the selected peg only.
**Notes:** Deselect was a free-text refinement — narrower than the offered "tap-elsewhere" option. Tapping another peg re-selects; empty/illegal cell is a no-op.

---

## End-State Signal

### End-state presentation
| Option | Description | Selected |
|--------|-------------|----------|
| Simple dialog | Modal overlay: pegs remaining + win/perfect message + Restart. Clear signal; seam for Phase-2 Results. | ✓ |
| Inline banner | Non-modal banner, board still visible; easier to miss. | |
| Just highlight + status text | Status line updates only; weak signal. | |

### Restart affordance
| Option | Description | Selected |
|--------|-------------|----------|
| Always-visible button, confirm mid-game | Persistent control any time; confirm if game in progress; no confirm at end-state. | ✓ |
| Always-visible button, no confirmation | Instant reset; accidental taps lose progress. | |
| Only in end-state dialog | Restart only once game over; weakens "restart at any time." | |

**User's choice:** Simple modal dialog; persistent Restart button that confirms only mid-game.
**Notes:** Both recommended options.

---

## Jump/Removal Animation

### Jump motion
| Option | Description | Selected |
|--------|-------------|----------|
| Straight slide | Peg translates in a straight line over the jumped peg; single Animatable Offset. | ✓ |
| Arc hop | Parabolic arc/lift; more satisfying, more work. | |
| Instant + fade | Snap to destination, no travel; least satisfying. | |

### Removal feedback
| Option | Description | Selected |
|--------|-------------|----------|
| Fade + shrink | Removed peg fades to 0 while scaling down, timed to landing. | ✓ |
| Quick pop | Scale-up then shrink-away; playful, more tuning. | |
| Instant remove | Peg vanishes immediately; flat. | |

**User's choice:** Straight slide + fade-and-shrink removal.
**Notes:** Baseline feel for Phase 1; arc/pop reserved for the Slice-2 visual-polish pass.

---

## Claude's Discretion

User explicitly chose "I'm ready for context" and left these technical items to research + planning:
- Gradle module topology / domain-purity enforcement (separate `:domain` JVM module vs single module + Konsist/ArchUnit boundary test).
- Internal board data representation and `Position`/coordinate model.
- Exact scope of the Phase-1 golden-master determinism scaffold.
- Visual styling of legal-destination highlighting.

## Deferred Ideas

- Drag-to-jump input → Slice-2 polish (no domain changes).
- Arc/hop jump motion + pop removal → Slice-2 visual-polish pass (POLI-01).
- Auto-jump on single legal move → considered and rejected for consistency (D-03).
