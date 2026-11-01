# tech-lead report — candy-backdrop: rulings on J-4b's decisions D1–D6
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; HEAD `0c45666`; J-4b work `8fad1b0..eee370e`, R-1 `635c9b8`)
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch in full; `.claude/rules/lessons.md`)
Ran in parallel with: the code-auditor's review of `d029950..eee370e`
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh. The founder's Q-019 answer puts Fable-pinned roles on Opus until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **D1: ACCEPT AS BUILT.** `BackdropSceneAlphaKey` stays on the scene node, under the root's `clearAndSetSemantics`. No move, and no edit to `BackdropSemanticsTest`.
- **D5: ACCEPT AS BUILT.** R-1 (`635c9b8`) stays in 3.0.0. My rider's "settled within two frames" meant what is drawn. The incoming slot's removal on the 3rd frame does not strand anything, and the guard bounds it. Nothing is reverted, and nothing moves to 3.0.1.
- **D2, D3, D4 and D6: ACCEPTED.** None of them changes a ruling, so none needs a code change.
- **No change is owed from these rulings, so they add no review loop.** One optional tightening (§2, last item) rides only on a fix round the auditor opens for some other reason.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-j4b-d-rulings.md` — this report: the rulings on D1–D6.

## 1. D1 — where the alpha hook lives: on the scene node, as built
**What is true** (`SceneBackdrop.kt:65`, `:126-131`, `:254`; `BackdropTestHost.kt:94`; `BackdropSemanticsTest.kt:28-30`):
- The root's `clearAndSetSemantics` replaces every descendant's semantics for accessibility. The scene node's `.semantics { backdropSceneAlphaValue = alpha }` reaches a test only through `useUnmergedTree = true`, and TalkBack never sees it. BD-15's behaviour, which is no description, click, focus or new stop, is unchanged.
- BD-15's test asserts the root's exact key set: `TestTag`, density, layers and motion. That set is still exact, so the test needs no edit.

**Why the scene node is the better place, not just the cheaper one:**
- **The hook exists only while a scene is composed, once per slot that shows one.** So `sceneAlphas()` reports both whether a scene is present and its alpha. Test 4 (D4) and the R-1 guard's "scenes still composed … isEmpty()" depend on that.
- **On the root the key would always be defined.** A test would then have no structural view of the scene, and the "nothing stranded" assertions would need a second hook.
- **It sits on the node whose `graphicsLayer` calls the same reader** (`:221-223`). That is A3's "the test reads the source" in its most literal form.

**The spec's wording does not stand against it.**
- §12 says "one semantics node on the backdrop's root", and BD-15 counts the root's properties. Both concern what accessibility sees, and accessibility sees only the root.
- BD-23 (a)'s "a test-only hook beside the three existing ones" is satisfied: the hook is test-only and is a fourth backdrop property.
- B-08's "one test-only property" was already out of date at three.

**The record.** The ux-designer clarifies BD-23 (a) and B-08 in the next docs sync, wording only: "the scene node under the cleared root carries a fourth test-only property, the scene's alpha reader". I record the ruling in `docs/tech-design.md`'s candy-backdrop section at the same sync. Neither edit gates the merge.

## 2. D5 — R-1's settle: visible on frame 2, structural on frame 3, accepted
**What is true** (`SceneBackdrop.kt:105-118`; the guard test `turningAnimationsOffMidCrossFadeSettlesItWithinTwoFrames`, `635c9b8`):
- Under `LaunchedEffect(density)`, the running `fade.animateTo` takes scale 0 from its next frame. So by the 2nd frame after the switch, the incoming slot is at alpha 1.
- The incoming slot's sky is opaque, so it hides the slot beneath. The guard shows the window pixel-identical to the settled catalogue on that frame.
- The coroutine's `below = density; above = null` lands after `animateTo` returns, so the slot's removal recomposes on the 3rd frame. Under the old gate key, the restart removed it on the 2nd.

**Ruling: accept as built.**
- **What my rider R-1 measured.** R-1 (`tech-lead-j4b-ruling.md` §2) asked for "settled within two frames, with nothing stranded". "Settled" means what the player sees, as in the RM precedent: `TutorialCompletionStarLiveMotionTest` pins the star "settled no later than the second frame after a switch" by its drawn size.
- **"Nothing stranded" is a structural bound, and the guard has it.** After a cross-fade's length, no scene slot is composed. M6, the strand mutant, is red there, and M7, the mutant that ignores the scale, is red on the pixels.
- **The cost of the extra frame.** The covered landscape slot is drawn under an opaque sky for one more frame, once per live switch made during a cross-fade. That is an overdraw nobody can see, on an event that is rare by construction.
- **Why reverting `635c9b8` would be worse.**
  - It restores an effect keyed on the gate. Tech design §16 #28's rule forbids that, and #33 is already open for the same pattern in `rememberChipRoom`.
  - It costs a commit and a re-review to gain one frame of invisible clean-up.
  - The guard passes on both versions, so it would stay either way.
- **The record.** R-1 is closed in 3.0.0. No 3.0.1 item is filed for it.

**Optional, owed only in a fix round the auditor opens for another reason:** add one structural line to the guard, "no landscape slot is composed by the 3rd frame after the switch" (`FRAMES_TO_SETTLE + 1`), and show it red on M6. It tightens the bound from a cross-fade's length to one frame past the visible settle. If there is no such round, it is not owed and is not filed.

## 3. D2, D3, D4, D6 — accepted, no change
- **D2 — a `() -> Float` reader as the key's value: accepted.**
  - `remember(animationsEnabled)` keeps the lambda's identity stable (`:172`). So the semantics block re-runs only when the gate flips, and the backdrop never recomposes for the fade.
  - It follows the same idiom as `BackdropClockProbe` and `rememberChipRoom`.
  - That `remember` is keyed on the gate, but it is not an effect. §16 #28 governs effects, and the fade's effect is keyed on `ready` (A3).
- **D3 — test 3 pins the scene on frame 3: accepted, and it matches A1.**
  - "Opens on frame 2 as today" in A1 means the gate flips on frame 2, and the scene is composed and drawn on frame 3. That is the J-4a audit's ok0/ok1, and BD-23 (a)'s "within two `advanceTimeByFrame()` of the first frame".
  - A1 is not amended. This is a clarification of my wording.
- **D4 — test 4 corrected before its first commit: accepted, not a weakening.**
  - It is a new test, not one of §4's ruled amendments to existing tests. The correction was made while it was still red, before any commit.
  - The original form asked for a scene alpha on a frame where R1's existing behaviour composes no incoming slot yet. The corrected form asserts BD-23 (a)'s actual claim: every scene present reads 1, and one is present within two frames. M1, the re-fade mutant, stays red against it.
  - The auditor confirms the commit history shows it uncommitted in its first form. That is a check, not a ruling.
- **D6 — what starts on frame 1: accepted.** §3 condition 2 allows a remembered `Animatable` and an effect that returns at once. Neither reads, registers, decodes or loads anything. 15e stays not owed, subject to the auditor's check of §3 on the diff.

## Checks
- `git log --oneline -12` in the candy worktree: pass. HEAD is `0c45666`, R-1 is `635c9b8`, the feature commit is `8fad1b0` and the develop merge is `eee370e`.
- `git show 635c9b8 -- app/src/main` and `-- app/src/test`: pass. The only main change is the key `(density, animationsEnabled)` → `(density)`. The guard test asserts the pixels on frame 2, the empty `sceneAlphas()` a cross-fade later, and layers {Sky}.
- Read `SceneBackdrop.kt` in full at `0c45666`: pass. The facts used in D1, D2, D5 and D6 are at the line numbers cited above.
- Read `BackdropSemanticsTest.kt` in full: pass. The root's exact key set is unchanged.
- Read `BackdropTestHost.kt:84-110`: pass. `sceneAlphas()` uses `keyIsDefined` on the unmerged tree.
- `grep -n` in `docs/ux/candy-backdrop.md`: pass. I read B-08 (`:212-215`), BD-15 (`:1819`) and BD-23 (a) (`:1827`).
- `grep -n` in `docs/tech-design.md`: pass. I read §16 #28's rule, #33 and the RM precedent (`:1739`).
- Read in full: `android-game-dev-j4b.md` and `tech-lead-j4b-ruling.md`.
- Not run: Gradle, the Check and any emulator. These are rulings on design decisions with the developer's evidence quoted, and the auditor, who is reviewing concurrently, owns the code gate. No scratch worktree was needed. `.planning/log/` was not read.

## Open issues / risks
1. **Docs sync, not gating:** the ux-designer's wording for BD-23 (a) and B-08 (§1), and my tech-design line for D1. Both go in the next docs-only sync, after the merge.
2. **Trailer.** This commit carries the harness's attribution lines, as the developer's J-4b commits do. Plugin 1.0.0's gitflow asks for the single Fable trailer, and `3141dde` carries that. The producer settles it once for the branch.

## Handoff
Next: code-auditor — D1–D6 are ruled, and none needs a code change. Review against these rulings. The §2 tightening is owed only if you open a fix round for another reason.
Next: producer — no re-dispatch of the developer comes from these rulings. Proceed to QA's J-5 as amended in `tech-lead-j4b-ruling.md` §5, then re-dispatch me with `qa-engineer-j5.md` and the auditor's report for the sign-off addendum. Queue the docs sync in Open issue 1 after the merge.

**Rulings: D1 accept as built; D2 accept; D3 accept, as A1's meaning; D4 accept, not a weakening; D5 accept as built, R-1 kept in 3.0.0; D6 accept.**
