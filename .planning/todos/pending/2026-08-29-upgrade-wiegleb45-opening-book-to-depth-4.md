---
created: 2026-08-29T00:00:00.000Z
title: Upgrade Wiegleb-45 opening book from K=3 to K=4 (overnight run)
area: domain
resolves_phase: null
files:
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/OpeningBookAuthoring.kt
  - domain/src/main/resources/golden/book/
---

## Problem

During Phase 8 (plan 08-08), the user decided to ship Wiegleb-45's opening book
at depth K=3 instead of the K=4 depth DEBT-01's must_have names, because the
K=4 authoring run projects to ~5.7 hours (~37 s per position x 553 raw
positions) and ~44 of them are likely undecidable even then (2 of the first 25
resisted beam width 200 000). The other four boards shipped at K=4.

Consequence until this runs: a hint requested at Wiegleb-45's 4th move falls
through to the runtime solver, which 08-05 measured as unable to decide early
Wiegleb positions inside the 1.5 s deadline — so those hints can show the
untruthful "no winning move" banner.

## Fix

1. In `OpeningBookAuthoring.POLICIES`, raise Wiegleb-45's book depth back to
   K=4 (policy is per-board; the other four committed books are untouched by
   design and regeneration is byte-stable for them).
2. Run detached overnight:
   `nohup env UPDATE_GOLDEN=true ./gradlew :domain:test --tests '*OpeningBookAuthoringTest*' > /tmp/wiegleb-k4.log 2>&1 &`
3. Expect ~90% coverage with a documented gap (same policy as Diamond-41's
   book header). Positions the run cannot decide stay absent and are listed in
   the file header.
4. Re-run `:domain:test` plus the DepthSweep proof from 08-10, commit the
   regenerated book file, and update the 08-08 SUMMARY / BOOK-EVIDENCE trail
   if 08-11 referenced the K=3 scope.
