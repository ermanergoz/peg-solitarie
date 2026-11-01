---
created: 2026-08-30T20:00:00.000Z
title: Decide whether Stuck finishes should reach Results (1–2 star bands are unreachable)
area: general
files:
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt
---

## Problem

Discovered during the Phase 8 on-device audit (2026-08-30): `ScoreCalculator.stars()` defines a
1–3 star banding (`≤ minimumPegs → 3★`, `≤ minimumPegs+2 → 2★`, else `1★`), but the 2-star and
1-star bands are **unreachable by any player on any board**:

- `GameViewModel.freezeAndFinishIfTerminal` emits the finished event (→ Results, where stars are
  shown and `LevelResult` recorded) only when `status == GameStatus.Won`.
- `Won` requires `pegCount <= board.spec.minimumPegs` — so `pegsRemaining` at Results time is
  always inside the 3-star band.
- A finish above the perfect target is `Stuck` and routes to the rescue banner
  (Undo · Restart · Home, D-05/D-09) — never to Results, never recording a result.

Net effect: every Results screen the app can show says 3 stars. This is consistent v1.0 D-05
routing (not a Phase 8 regression — the phase only re-anchored Diamond's 3-star boundary), but it
contradicts the product description "Players earn 1–3 stars per level" (PROJECT/CLAUDE.md), and
the Phase 8 device checklist (08-11) assumed the 2★/1★ rows were observable.

## Decision needed

Pick one:

1. **Make Stuck finishes reach Results** — e.g. add a "Finish"/"Accept" action to the rescue
   banner (or auto-route after rescue is declined) so a dead-ended game records a 1–2 star
   `LevelResult`. Touches D-05/D-09 UX decisions; consider Challenge-ladder implications
   (Stuck must still never advance the ladder, D-02).
2. **Accept 3-star-only Results as the design** — then fix the docs instead: correct the product
   description and delete the dead 1–2 star bands (or keep them documented as defensive), and
   never write a checklist that asserts them again.

Either way the current mismatch between code, product description, and audit checklist should not
survive into the v1.1 milestone audit.
