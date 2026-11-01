---
status: passed
phase: 08-assist-generator-quality-debt
source: [08-VERIFICATION.md]
started: 2026-08-30T00:40:00Z
updated: 2026-08-30T20:00:00Z
device: physical Android hardware (debug build incl. WR-01 fix, 2026-08-30)
---

## Current Test

[complete — all items discharged on physical hardware 2026-08-30]

## Tests

### 1. Hint truthfulness on hard early positions (real device)

expected: Requesting a hint from a hard early position on European-37, Asymmetric-39, or Wiegleb-45 produces a real winning move within the 1.5s deadline — no "no winning move" rescue banner.
result: passed — checklist items 1a–1d all approved. Note: tester also probed off-book play (deviating from the hinted first move on Wiegleb-45 and Diamond-41) and observed the rescue banner at move 6 — this matches the documented budgeted-search boundary (book depth 4/3 + 1.5s solver beyond it), pre-approved in the phase evidence, not a defect. No device warming or full-1.5s burns observed in normal (on-line) play.

### 2. Instant Diamond-41 hint (real device)

expected: A hint on Diamond-41 resolves instantly (opening-book lookup).
result: passed — items 2a–2c approved. On-line hints instant throughout.

### 3. Diamond-41 finish at 2 pegs awards 3 stars (real device)

expected: Finishing Diamond-41 at its proven fewest-pegs floor of 2 awards 3 stars.
result: passed — tester followed hints to the floor: final jump left 2 pegs with no moves remaining, the Results screen appeared immediately and showed 3 stars (items 3a, 3b's testable band, 3c). Cross-board finish at 1 peg showed Results as before.

## Summary

total: 3
passed: 3
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

_None blocking. One discrepancy recorded from item 3b: the "3–4 pegs → 2 stars" and "5+ → 1 star"
rows are untestable in the UI on any board — a finish above the perfect target is `Stuck` and
routes to the rescue banner, never Results, so `ScoreCalculator`'s 1–2 star bands are unreachable
by players. This is v1.0's D-05 routing design (not a Phase 8 regression) but contradicts the
product description ("Players earn 1–3 stars per level"). Captured as a product-decision todo:
`.planning/todos/pending/2026-08-30-decide-whether-stuck-finishes-reach-results-1-2-stars.md`._

Resume check (checklist item 4): passed — force-stop + Continue on Diamond-41, hint still answers
(per-board target survives the resume boundary; the 08-07 defect class did not reproduce).

Evidence review (checklist item 5): passed — 5a book byte-intact in release APK (cmp exit 0 on all
5 entries), 5b size cost ~11.6 KB (~0.56%), 5c `./gradlew check :domain:depthSweep` green on the
post-WR-01 tree (11m07s, 2026-08-30).
