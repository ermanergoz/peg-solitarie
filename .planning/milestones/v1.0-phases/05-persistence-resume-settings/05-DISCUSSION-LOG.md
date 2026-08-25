# Phase 5: Persistence, Resume & Settings - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-13
**Phase:** 5-persistence-resume-settings
**Areas discussed:** Resume UX & lifecycle, "Best result" meaning, Continue & replay Challenge, Settings screen & toggles

**Preamble (locked, not discussed):** Storage technology was pre-decided by the `CLAUDE.md` tech stack (Room for per-level results + resume snapshot; DataStore Preferences for Challenge progress + settings; migration-safe with `exportSchema=true`, explicit `Migration`, no `fallbackToDestructiveMigration`). The `ChallengeProgress` in-memory seam (Phase 3) and `SettingsRepository` (`hintsEnabled`) already exist. The `GamesService` no-op stub is roadmap-mandated (Claude's discretion, not discussed).

---

## Resume UX & lifecycle

### Reopen with a mid-progress game
| Option | Description | Selected |
|--------|-------------|----------|
| Home "Continue" entry | Land on Home, show a Continue affordance that drops back into the saved board; can also start fresh | ✓ |
| Auto-resume into board | Skip Home, reopen directly on the in-progress board | |
| Continue / New prompt | Launch dialog: Continue vs Discard/New | |

### Which modes get resume
| Option | Description | Selected |
|--------|-------------|----------|
| Both Classic & Challenge | Any mid-game resumable; snapshot stores mode + levelId | ✓ |
| Challenge only | Only Challenge resumes; Classic always starts fresh | |

### How many in-progress games at once
| Option | Description | Selected |
|--------|-------------|----------|
| One slot (latest wins) | Single saved game; new game overwrites (spec's single InProgressGame row) | |
| One per mode | Classic + Challenge snapshots coexist; up to two Continue entries | ✓ |

### When to write the snapshot
| Option | Description | Selected |
|--------|-------------|----------|
| On leave (onStop) + safety | Persist when app backgrounds / game screen left; no per-move churn | ✓ |
| After every move | Write after each jump/undo | |

### When to clear the snapshot
| Option | Description | Selected |
|--------|-------------|----------|
| On solve or explicit exit | Clear on solve/restart/new-game-in-mode; leaving to Home keeps it | ✓ |
| Only on solve | Persists through everything except a completed solve | |
| Solve, restart, or Home | Also clear on navigating Home | |

**Notes:** "One per mode" deliberately overrides the design spec §10 single-row `InProgressGame` shape — the resume store is keyed by mode (max two live snapshots).

---

## "Best result" meaning

### How "best" is tracked
| Option | Description | Selected |
|--------|-------------|----------|
| Per-field best | Best stars, best time, best score tracked independently across plays | ✓ |
| Best by score, as a unit | Store the single highest-scoring play as a set | |

### Do unsolved attempts record
| Option | Description | Selected |
|--------|-------------|----------|
| Only solved plays record | A per-level best exists only once solved | ✓ |
| Record attempts too | Track best pegs-remaining / attempt count for unsolved plays | |

### New-best cue on Results
| Option | Description | Selected |
|--------|-------------|----------|
| Yes — "New best!" cue | Badge/label when a play beats a stored best | ✓ |
| No cue | Saved best stays silent | |

### Per-board bests in Classic catalog
| Option | Description | Selected |
|--------|-------------|----------|
| Yes — stars on catalog cards | Show earned stars on each Classic board card | ✓ |
| Not this phase | Bests stored, shown on Results only | |

---

## Continue & replay Challenge

### Home Challenge tile continuation
| Option | Description | Selected |
|--------|-------------|----------|
| "Continue — Level N" | Tile shows the resume level; one-tap resumable | ✓ |
| Plain "Challenge", enters at level N | Plain label but tapping enters at saved level | |

### Replay earlier levels
| Option | Description | Selected |
|--------|-------------|----------|
| Frontier only (+ retry current) | Play at current level; retry current; no picker | ✓ |
| Level picker to any passed level | New Challenge level-select 1..highestReached | |

### Persisted Challenge position model
| Option | Description | Selected |
|--------|-------------|----------|
| currentLevel only | Persist just current level (backs ChallengeProgress with DataStore) | ✓ |
| currentLevel + highestLevelReached | Persist both (spec shape); needed for picker/leaderboard | |

**Notes:** With a linear no-skip ladder (advance only on solve), `currentLevel` == highest reached, so persisting it alone satisfies CHAL-05 and PERS-02.

---

## Settings screen & toggles

### Toggle location
| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated Settings screen | Gear on Home opens a Settings destination | ✓ |
| Inline sheet/dialog | Bottom-sheet/dialog from Home, no separate destination | |

### Haptics scope
| Option | Description | Selected |
|--------|-------------|----------|
| Wire it now (haptic on jump) | Light haptic on jump/removal gated by toggle | ✓ |
| Persist flag only | Store preference, emit nothing yet | |

### Sound scope
| Option | Description | Selected |
|--------|-------------|----------|
| Persist flag only (no audio yet) | Store preference; SFX are a later polish item | ✓ |
| Add minimal SFX now | Add a couple of sound effects gated by the toggle | |

### Fresh-install defaults
| Option | Description | Selected |
|--------|-------------|----------|
| Both ON | Sound on, haptics on (fail-open) | ✓ |
| Haptics ON, sound OFF | Quieter first impression | |
| Both OFF | Opt-in only | |

---

## Claude's Discretion

- Room schema/entity shapes, DAO surface, the board+move-history `TypeConverter` format, and the concrete initial `Migration` + migration-test scaffold.
- Whether `ProgressRepository` is single or split (spec names one).
- Exact `GamesService` interface + no-op stub (roadmap/spec §8.4 mandate; bind alongside `AdGateway`).
- Whether the existing in-game "Show possible moves" (`hintsEnabled`) toggle is also mirrored into the Settings screen (default: stays in-game).
- Home Continue-affordance layout, Settings gear placement, and all DataStore key naming within the shared `app_prefs` store.

## Deferred Ideas

- Real sound effects / audio engine (assets + playback) — later polish; SET-01 flag ships ready.
- Challenge level picker / replaying arbitrary passed levels — needs `highestLevelReached` + a new screen.
- `highestLevelReached` persistence + leaderboards / achievements / cloud save — Slice 2 (behind `ProgressRepository` / `GamesService`).
- Attempt/loss statistics for unsolved plays — not this phase.
- Mirroring the in-game hints toggle into Settings — optional, planner's call.
