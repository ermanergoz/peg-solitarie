---
created: 2026-08-30T00:52:00.000Z
title: Run the Phase 8 device checklist on physical hardware
area: general
resolves_phase: 08-assist-generator-quality-debt
files:
  - .planning/phases/08-assist-generator-quality-debt/08-11-PLAN.md
  - .planning/phases/08-assist-generator-quality-debt/08-11-SUMMARY.md
  - .planning/phases/08-assist-generator-quality-debt/08-BOOK-EVIDENCE.md
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCase.kt
  - domain/src/main/resources/golden/book/
---

## Problem

Plan 08-11's Task 3 is the blocking, on-device phase audit for Phase 8 — the step that puts a
human in front of the three player-visible claims the phase makes. **It was not performed.**
At the checkpoint on 2026-08-30 no physical device was available, and the developer explicitly
chose to defer the whole check rather than discharge it against `emulator-5554`. That decision
is deliberate and follows directly from Phase 7's recorded lesson: its audit was emulator-only
*and* globally approved, and STATE.md now carries that ambiguity as a known weakness. Repeating
it here would have converted a known gap into a false "verified on device" claim.

So: **no per-item approvals exist, no global approval exists, and nothing in Phase 8 has been
confirmed on real hardware.** Everything below `:domain` is proven by machine — `./gradlew
check` is green in 11m 11s including the exhaustive `:domain:depthSweep`, and
`08-BOOK-EVIDENCE.md` proves the opening book ships byte-intact through R8 into a minified
release APK — but "the machine agrees" and "a player sees it" are different claims, and only the
first is currently supported.

The residual risk is genuinely low. Nothing above `:domain` changed in Phase 8:
`GameUiState.noWinningMove`, the rescue banner, the thinking indicator and the 1.5s deadline are
untouched. The plausible device-only surprises are timing and thermals, not correctness.

## Solution

Before the v1.1 milestone closes, run the checklist below on a **physical device**. An emulator
run does not close this item.

A debug APK was already built and left ready at `app/build/outputs/apk/debug/app-debug.apk`
(20.2 MB, built 2026-08-30). It may be stale by the time this is picked up — if in doubt,
rebuild. Install with:

```
./gradlew :app:installDebug
```

(No `local.properties` configuration is needed: debug builds are hard-wired to Google's test ad
units and never require real ad/PGS IDs.)

### 1. Large-board hints (DEBT-01)

- **1a.** Classic → **Wiegleb-45**. Tap Hint immediately. Expect a highlighted source and
  destination within about a second and **no** "no winning move" rescue banner. Before Phase 8
  this position always folded to the banner.
- **1b.** Play the hinted move, tap Hint again; repeat for **four moves total**. Every one should
  answer. On the fifth move you are past the book's guaranteed depth and the solver takes over —
  up to 1.5 s there is expected, not a regression.
- **1c.** Repeat 1a and 1b on **European-37** and **Asymmetric-39**.
- **1d.** Deliberately blunder, then tap Hint. If the banner appears that is **correct** — the
  perfect finish really is gone. Tap the rescue undo and confirm the hint answers again.

### 2. Diamond-41 hint (D-01/D-02)

- **2a.** Classic → **Diamond-41**, tap Hint at the start. Expect a move essentially instantly.
  Before Phase 8 this could only ever say "no winning move".
- **2b.** Follow the hint for several moves; each should stay instant while on the committed
  floor line.
- **2c.** Deviate deliberately, then tap Hint. Expect either a move or the banner — but **note
  the response time**. See the expectation note below.

### 3. Diamond-41 stars (D-03/D-04)

- **3a.** Play Diamond-41 to a finish; note pegs remaining and stars awarded.
- **3b.** Confirm the mapping: **2 pegs → 3 stars; 3–4 pegs → 2 stars; 5 or more → 1 star.**
- **3c.** Play any cross board to a finish and confirm its stars are unchanged from expectation.

### 4. Resume (Pitfall 3)

- **4a.** Start a Diamond-41 game, play a few moves, force-stop the app from Settings.
- **4b.** Relaunch → Continue → tap Hint. It **must** still answer. "No winning move" on a
  resumed Diamond game means the per-board target was lost across the resume boundary — the exact
  defect plan 08-07 exists to prevent.

### 5. Evidence review

- **5a.** Read `08-BOOK-EVIDENCE.md`; confirm the book is present and byte-intact in the release
  APK (assumption A5 resolved).
- **5b.** Confirm the APK size delta from the book is acceptable — the book's own packaged cost
  is **11,628 B**, about 0.56 % of the APK.
- **5c.** Confirm `./gradlew check` is green and `./gradlew :domain:depthSweep` passed. (Both were
  green on 2026-08-30; re-run if the tree has moved.)

## Expectation to set before testing — the 444 gap positions

On Diamond-41 and Wiegleb-45 there are **444 positions** (440 of Diamond's 785 canonical-covered,
4 of Wiegleb's 101) outside the committed books. On every one of them the solver returns `null`
after the full 1.5 s, so the player sees a **1.5 s spinner ending in the rescue banner**. This is
measured, header-declared in the book artifacts, pinned by equality in `DepthSweepTest`, and
**approved** — it is not a defect to report.

What *is* worth reporting: if the Diamond off-book hint noticeably **warms the device**, or
consistently burns the full 1.5 s in normal play. That is Pitfall 1's predicted cost (the
`Pagoda` prune is provably weaker at a two-peg target) showing up in practice — a known, accepted
trade, but one nobody has yet observed on real silicon.

## How approval must be recorded

**Per item, never globally.** Record 1a–1d, 2a–2c, 3a–3c, 4a–4b, 5a–5c each as approved or with
the specific discrepancy observed. The plan states this explicitly, and it exists precisely
because Phase 7's globally-approved audit is what created the ambiguity this project is now
working to avoid. A single "looks good" does not close this todo.

Write the itemised outcome into `08-11-SUMMARY.md` (which currently records this audit as
deferred), and update STATE.md's Phase 8 entry once it is genuinely confirmed.

## Related

Same pattern and, ideally, the same session as
`.planning/todos/pending/2026-08-28-confirm-audio-cues-on-physical-hardware.md` — Phase 7's
outstanding hardware confirmation. One device session can close both: SOUND-01 (first tap after a
cold start) is the sharpest item there and costs about a minute.
