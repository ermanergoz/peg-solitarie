# Roadmap: Peg Solitaire (Android)

## Milestones

- ✅ **v1.0 Slice 1 — Playable Core** — Phases 1-5 (shipped 2026-08-26)
- 📋 **v1.1 Slice 2 — Play Games, AdMob & Polish** — planned (run `/gsd:new-milestone`)

## Phases

<details>
<summary>✅ v1.0 Slice 1 — Playable Core (Phases 1-5) — SHIPPED 2026-08-26</summary>

- [x] Phase 1: Domain Foundation & Playable Board (5/5 plans) — completed 2026-06-28
- [x] Phase 2: Classic Mode, Scoring & Navigation (8/8 plans) — completed 2026-06-29
- [x] Phase 3: Challenge Generation & Difficulty Curve (5/5 plans) — completed 2026-07-01
- [x] Phase 4: Assist — Hint & Undo (6/6 plans) — completed 2026-07-12
- [x] Phase 5: Persistence, Resume & Settings (7/7 plans) — completed 2026-08-25

Full details: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md) · Audit: [milestones/v1.0-MILESTONE-AUDIT.md](milestones/v1.0-MILESTONE-AUDIT.md)

</details>

### 📋 v1.1 Slice 2 (Planned)

Deferred scope from v1.0, slots in by swapping Hilt bindings behind existing seams:
- Play Games Services v2 behind `GamesService` (PGS-01..04)
- AdMob rewarded/interstitial + UMP consent behind `AdGateway` (ADS-01..02)
- Visual polish pass (POLI-01), sound playback consuming the persisted `soundOn` flag
- Carried tech debt: solver deadline envelope on hard early positions, Diamond-41 solvability fixture, Nyquist gaps on phases 3/4, security advisories WARN-02/03

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
| ----- | --------- | -------------- | ------ | --------- |
| 1. Domain Foundation & Playable Board | v1.0 | 5/5 | Complete | 2026-06-28 |
| 2. Classic Mode, Scoring & Navigation | v1.0 | 8/8 | Complete | 2026-06-29 |
| 3. Challenge Generation & Difficulty Curve | v1.0 | 5/5 | Complete | 2026-07-01 |
| 4. Assist — Hint & Undo | v1.0 | 6/6 | Complete | 2026-07-12 |
| 5. Persistence, Resume & Settings | v1.0 | 7/7 | Complete | 2026-08-25 |
