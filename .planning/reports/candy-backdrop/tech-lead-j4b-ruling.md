# tech-lead report — candy-backdrop: J-4b ruling on the developer's performance-audit proposal
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; HEAD `24fb33c`, whose code equals `d029950`; merge-base with develop `d251c3b`; develop is now `452f187`, docs only since)
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so I compared the snapshot against the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks and testing, and read the plugin's reporting and dispatch in full; `.claude/rules/lessons.md`)
Ran in parallel with: the ux-designer's B-11a ruling on the same proposal
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh. The founder's Q-019 answer puts Fable-pinned roles on Opus until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Ruling: ACCEPT WITH AMENDMENTS (A1–A6).** The change is `android-game-dev-perf-audit.md` §5: open the scene after Home's entrance cascade and fade it in. It goes ahead **only if the ux-designer accepts the visible change**, which includes the case in A5. If the ux-designer rejects it, I have no backdrop-only change worth ≥ 120 ms within the day, and J-4c applies now.
- **15e is not owed**, under the two conditions in §3.
- **Both test amendments are accepted** in the exact form in §4. Each is "amended, not weakened".
- **J-5 is amended before any arm-C data exists.** It gains an informative RT_late read, `sleep 2` in place of `sleep 1`, and an informative read that tells whether the stencils are Home's or the scene's (§5). The line, T and the verdict rules do not change.
- **Stencil risk:** no cheap check exists before J-5, and J-5's own dumps answer it at no extra bench cost. J-4c stands as the plan for a FAIL. **VERDICT: HOLD.** BD-23 (c) stays open until J-5.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-j4b-ruling.md` — this ruling: the amendments to the change, the 15e conditions, the two ruled test edits, the J-5 amendment and the stencil-risk plan.
- Scratch, git-ignored, never committed: `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/7be27af9-9fc5-49d9-b194-5a5598b4253b/scratchpad/j4b/reach.py`. It checks whether J-2's dumps still hold the frame where a scene opened at 440 ms would first draw (§5).

## 1. Why accept
- **The audit closes its sums.** RT for frames 1–3 is +222 ms, against an S excess of +228 ms. The cache build costs 0.2 ms and runs once, so it is not a cause.
- **The proposal is my own candidate 1 from J-2's ruling** (§2), and it is the cheapest one: three files in `ui/backdrop/` and about one developer-day.
- **The rejected candidates are rejected for the right reasons:**
  - **Pre-warming** while the scene is invisible saves nothing, because HWUI skips nodes at alpha 0.
  - **A simpler first pose** pays the full pose's cost again when it switches in, and it adds a second code path.
  - **Cheaper geometry (4a)** has an outcome the JVM cannot show.
  - **The bitmap (4b)** breaks backdrop spec §7 ("never on Home's start path, at any frame: … a bitmap"). It adds 5.8–10.5 MB and 7.7 ms of UI-thread raster on every Home ↔ game navigation.
- **What I accept:** the change moves the cost onto a still screen, where BD-23 (c)'s concern (a hitch during Home's entrance) no longer applies. It does not remove the cost. The moved cost is measured (A6 and §5) and goes into the Q-PUBLISH-3.0.0 material as a number. It is not hidden behind S.

## 2. Amendments to the change (binding on the J-4b dispatch)
**A1 — The open waits on the entrance's clock *and* its scale.**
- `clayEntrance`'s tweens run on the frame clock under the composition's `MotionDurationScale`. The open must elapse the same way: `BACKDROP_SCENE_OPEN_MS` of scaled animation time, counted from the gate's first awaited frame.
- One way is to wait on a throwaway `Animatable`'s `tween(BACKDROP_SCENE_OPEN_MS, easing = LinearEasing)` inside the existing `LaunchedEffect(Unit)`. The developer chooses the code.
- Why the scale matters: with an unscaled `withFrameNanos` count, an animator scale of 1.5–5× (developer options, some OEM settings) opens the scene in the middle of a cascade that has slowed down. That puts the cost back where this change removes it from.
- At scale 0 the wait ends on its first frame, so the scene opens on frame 2 as it does today. Test 3 pins this frame-exactly.
- Wall-clock `delay` is still forbidden.
- If the scaled wait cannot open on frame 2 at scale 0, the fallback is the developer's own mechanism: `rememberAnimationsEnabled()` read once when the effect starts.
- Either way, **no effect is keyed on the gate** (tech design §16 #28's rule).

**A2 — The tokens are named, and none of them points into `ui/home/`.**
- `BACKDROP_SCENE_OPEN_MS = ClayAnim.ENTRANCE_MS + HOME_LAST_ENTRANCE_ORDER × ClayAnim.ENTRANCE_STAGGER_MS`.
- `HOME_LAST_ENTRANCE_ORDER = 5` is a named constant in `BackdropMotion.kt`. Its KDoc cites Home's orders (hero 0, ribbon 1, chips 2–3, tiles 4–5). It is not a bare `5`.
- `ui/backdrop/` main code imports nothing from `ui/home/`. Test 5 is the only coupling. It fails if Home lengthens its cascade.
- `BACKDROP_SCENE_FADE_MS` is the ux-designer's value. It goes in `BackdropMotion.kt`, defined in terms of the `ClayAnim` token they name.

**A3 — There is one fade, and it starts from where the gate is.**
- The scene's alpha `Animatable` starts at **1** when the flag is already true at its first composition (`AlwaysReady`: previews, component tests). It runs 0 → 1 only on an observed false → true flip.
- With animations off, the scene's first composed frame draws at alpha 1. That frame must not be held at alpha 0 for one frame, or the animations-off scene would land a frame later than today. Test 3 asserts alpha 1 on the very frame the layers become {Sky, Waves, Lights}.
- The test-only `BackdropSceneAlphaKey` is written from the same value the `graphicsLayer` reads, so the test reads the source, not a copy.
- The fade's effect is keyed on the flag, never on the gate.

**A4 — `CompositingStrategy.ModulateAlpha` on the scene node is accepted on technical grounds.** It needs no offscreen buffer. A group fade would allocate a 10.5 MB full-window layer on the open frame, the very frame this change is meant to keep cheap. How it looks (the back wave showing through the front wave during the fade) is the ux-designer's call.

**A5 — Once per host start, made precise for the ux-designer.**
- The manifest declares no `configChanges`. So the host restarts, and the open and fade replay, on every activity recreation: rotation, a dark-mode switch, a font-scale or locale change, a multi-window resize, and a process-death restore (the developer's case).
- Today this reset is invisible, because it lasts one frame. After the change it is 0.44 s plus the fade, each time.
- Making the flag survive recreation (`rememberSaveable`) is **rejected**. A process-death restore is a process start, and the flag would put the scene on that start's frame 1.
- So this behaviour is part of what the ux-designer accepts or rejects.

**A6 — Where the moved cost lands goes into the record.** The developer's report states three things:
- the frame-clock time and frame index of the open in tests 1 and 5;
- that the first visible draw follows the cascade's end;
- that the steady-state draw is unchanged. The cache still builds once, and the draw block allocates nothing; the J-4a host check, re-run.

**Rider R-1, not gating.** `SceneBackdrop`'s existing density cross-fade is `LaunchedEffect(density, animationsEnabled)`. That keys an effect on the gate, against tech design §16 #28's rule. The R1 architecture specified `LaunchedEffect(density)`, and my sign-off's adherence row missed the difference.
- The behaviour is benign: a live flip during a cross-fade completes it at once and strands nothing.
- J-4b edits this file anyway. The developer drops the gate key, keeps the gate read at the effect's start, and adds one test: a `SystemMotionScale` flipped 1 → 0 in the middle of a cross-fade, settled within two frames, with nothing stranded.
- If R-1 costs a review loop, it moves to 3.0.1 and the producer files it.

## 3. 15e: not owed, on two conditions
Tech design §10's start-path list is `App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, the four `themes.xml` files and `AndroidManifest.xml`. `ui/backdrop/` is not on it.
- **Condition 1.** `git diff --name-only d029950..<J-4b head> -- app/src/main` lists only files under `ui/backdrop/`. `AppNavHost.kt:170` keeps its call to `rememberBackdropSceneReady()` unchanged.
- **Condition 2.** Frame 1 gains no system read, registration, decode, asset load or bitmap.
  - Remembering an `Animatable` and starting the existing `LaunchedEffect` are allowed.
  - `rememberAnimationsEnabled()` is already read on frame 1 by `SceneBackdrop`, so it adds nothing.
  - The battery-saver read and the clock stay behind the flag.

The code-auditor checks both conditions on the diff. If either one fails, 15e is owed on `Pixel_9a_bench` and the deviation comes to me first.

Since `d251c3b`, `develop` has gained only `.planning/`, `docs/`, `CLAUDE.md` and `AGENTS.md` changes, so no start-path file. The 15e PASS (+44.5 ms) stands.

## 4. The two test amendments: accepted, in this form
**`BackdropFirstFrameTest` (BD-23 (a)).**
- The frame-1 half is unchanged, word for word: after the host's first frame, the layers are {Sky}, the motion is AtRest, and the clock has requested no frame.
- The second half becomes **bounded on both sides**:
  - on every frame before the scaled open, the layers stay {Sky}, the motion is AtRest, and no clock frame has been awaited;
  - within two `advanceTimeByFrame()` calls after the open, the layers are {Sky, Waves, Lights}.
- The frame counts are computed from `BACKDROP_SCENE_OPEN_MS` and the test clock's frame time, never written as literals.
- With animations off, A6's original "within two frames" form is kept as a test of its own (test 3).
- This is stricter than today's test, because it adds a lower bound. It is **amended, not weakened**.

**`BackdropSettleTest.start()`.**
- The helper advances past the open plus two frames, computed from the token, instead of three frames.
- Every settle assertion is unchanged, and so is every time measured from `start()`. The clock starts at the open, so the idle delays count from the same point as before.
- This is **amended, not weakened**.

**Any other test that turns red is held, not edited.**
- If a red test needs only its setup advanced past the open (using the token), with every assertion unchanged, that edit is pre-ruled acceptable. The developer lists each such file in the report, and the code-auditor checks that only setup lines changed. Likely candidates are `SkyContinuityTest` and `BackdropReducedMotionTest`, which use `autoAdvance = false`.
- Any change to an assertion comes back to me first.

## 5. J-5 amended (registered now, before any arm-C data; everything else in J-5 stands)
**Why J-5 needs an amendment.**
- J-5's S stops at row 4. The moved cost lands later, at the first visible frame k + 3, which is ok5 or later and about ok9 typically (audit §5).
- On J-2's dumps, the last kept row is a median of about 1,020 ms after ok0 in every arm (`reach.py`). The moved draw would complete at about 440 ms plus up to about 180–290 ms of RT, plus the frames in between, so the margin is thin.
- J-2's B and N dumps already lose the k + 3 frame in 1/22 and 7/35 samples.

**Amendments.**
1. **Per launch: `sleep 2` replaces `sleep 1`** before `dumpsys gfxinfo … framestats`, for every label, A and C alike.
   - S is a difference of rows 0–4, so the extra second does not change S.
   - At A's measured rate (at most 40 rows in about 1 s), 2 s stays well under the 120-frame history.
   - **Validity, added:** a sample whose PROFILEDATA block holds 120 or more rows is invalid, because the history is full and row 0 may have been evicted.
2. **Informative RT_late, both arms.**
   - RT_late = max(`SwapBuffers − SyncStart`) over the kept rows from index 5 to the last.
   - Record its row index and its `FrameStartTime` offset from row 0.
   - Report the median per arm, C and A. Expect C at about 120–290 ms on this bench and A at about 15 ms or less.
   - It never enters the verdict. It goes into the Q-PUBLISH-3.0.0 material as "one stall of about X ms on a still Home, about 0.5 s after the entrance".
3. **Informative stencil split.**
   - Report the median RT (`SwapBuffers − SyncStart`) of rows 2 and 3, for C and for A. J-2's B was +37 and +29 ms over A.
   - Record the dump's `StencilAttachment` line per arm.
   - If C keeps rows 2–3 at B's excess, the stencils are Home's finishes'. If C's rows 2–3 fall to A's level, they are the scene's.

**Unchanged:** the line, T, PASS / VOID / FAIL, the sequence and the kept targets. This adds about 44 seconds of bench time.

## 6. The stencil risk: accept J-4c, no pre-check
- **No useful JVM check exists.** Robolectric `NATIVE` rasterises on the CPU. Whether Skia's GPU path renderer allocates a stencil for a non-convex path cannot be seen there. The audit's own JVM run could not see it either.
- **A device probe means a bench visit.** The emulator is excluded for this task, and the one bench visit this week is J-5's. Amendment 3 in §5 makes that visit answer the question in its own dumps, at no extra cost.
- **What happens on each J-5 result:**
  - On **PASS**, the question is moot.
  - On **FAIL**, J-5 is not re-run, and **J-4c applies**: one founder `Q-<NNN>`, ship or slip. The question carries S(C) − S(A), RT_late and the stencil split, so the founder sees exactly what is left. Most likely that is Home's finishes, at about +45 to +105 ms.
  - A further fix to Home's finish lips would touch `ui/theme/` and `ui/home/`. That makes 15e owed again and needs a second bench visit, so it is not this week's.
- **My recommendation for J-4c is unchanged: ship.** The remaining cost is one-off, falls within the entrance, is absent from frame 0 and from steady state, and is measured on the emulator's GL translation. Phone frame timing is on the 3.0.1 list.

## Checks
- `git rev-parse --abbrev-ref HEAD; git rev-parse --short HEAD; git rev-parse --short develop; git merge-base HEAD develop` — pass (`feature/candy-backdrop`, `24fb33c`, `452f187`, `d251c3b`).
- `git diff --quiet d029950 HEAD -- . ':(exclude).planning' ':(exclude)docs'` — pass (exit 0; the code is still the audited arm B).
- `git diff --name-only d251c3b develop | grep -vE '^\.planning/|^docs/'` — pass (`AGENTS.md`, `CLAUDE.md` only; no start-path file, so 15e stands).
- `grep -rn 'rememberBackdropSceneReady\|LocalBackdropSceneReady' app/src/main` — pass (one call, `AppNavHost.kt:170`, provided at `:177`; one reader, `SceneBackdrop.kt:82`).
- `grep -n 'configChanges' app/src/main/AndroidManifest.xml` — no match (the basis of A5).
- `grep -rn 'import com.app.pegsolitaire.ui.home' app/src/main/java/com/app/pegsolitaire/ui/backdrop/` — pass (no output; A2 keeps it that way).
- Read `BackdropSceneReady.kt`, `SceneBackdrop.kt:75–130`, `Clay.kt:197–240` (`clayEntrance`, `rememberAnimationsEnabled`) and the `ClayAnim` values (`ENTRANCE_MS` 240, `ENTRANCE_STAGGER_MS` 40, `NAV_TRANSITION_MS` 700) — pass (the audit's description is accurate; `SceneBackdrop`'s gate-keyed effect gives R-1).
- `python3 -I <scratchpad>/j4b/reach.py .planning/qa/candy-backdrop/jank2` — pass. Kept rows: A median 32 (7–38), B 23, N 24. The ok0-to-last span is about 1,020–1,034 ms in every arm. 440 ms is reached in every sample. The k + 3 frame is in the dump in A 24/24, B 21/22 and N 28/35. This is the basis of §5's `sleep 2`.
- Not run: the Check, Gradle and any emulator. This is a ruling on a document, with no code under review, and gsd's and the auditor's gates are not re-run. `.planning/log/` was not read.

## Open issues / risks
1. **J-5 may FAIL on Home's finishes** (audit Open issue 1). The expected residual is +45 to +105 ms against T = 100. J-4c is the plan, and §5's stencil read attributes the result.
2. **The moved stall swallows the start of the fade on the bench.** On this bench, up to about 120–290 ms of RT lands on the fade's first visible frame. A fade shorter than about 300 ms would read as a pop here. `NAV_TRANSITION_MS` (700 ms) keeps most of the fade visible. On a phone with a warm shader cache the stall is probably smaller. This is information for the ux-designer's choice of `BACKDROP_SCENE_FADE_MS`.
3. **A tap during the stall is delayed once per host start** (A5 widens "once" to every recreation). Nothing that is moving gets interrupted.
4. **The branch must take in `develop` (`452f187`) before its merge.** That brings docs only, but `docs/` conflicts are possible. The producer orders it after J-5.
5. **Trailer.** This commit carries the project rules' single Fable trailer (the snapshot's rules/gitflow.md, imported by CLAUDE.md), not the harness's session lines. The last two commits on the branch carry the session lines, which is the same open question the branch's other reports raise.
6. **The ux-designer's uncommitted `docs/ux/candy-backdrop.md` is in the worktree.** I did not stage it.

## Handoff
Next: producer —
1. **Wait for the ux-designer's B-11a ruling.** It covers the 0.44 s open, the fade value, the `ModulateAlpha` look and A5's every-recreation replay.
   - **Accepted:** dispatch android-game-dev for J-4b, test-first, with §2 (A1–A6, R-1), §3's conditions and §4's test forms. The five red-first tests are §5 of the audit, as amended by A1 and A3.
   - **Rejected:** J-4c applies now. Put the founder `Q-<NNN>` with J-2's numbers. My recommendation is ship.
2. After the developer: the code-auditor reviews the diff, including §3's two conditions and §4's setup-only edits.
3. Then the qa-engineer runs **J-5 as amended in §5**, once, at one bench visit. No 15e is needed if §3 holds.
4. Re-dispatch me with `qa-engineer-j5.md` for the sign-off addendum.
5. Add RT_late and Open issue 2 to the Q-PUBLISH-3.0.0 material.

**Ruling: ACCEPT WITH AMENDMENTS (A1–A6, rider R-1), conditional on the ux-designer's acceptance; 15e not owed under §3; both test amendments accepted as written in §4; J-5 gains RT_late, `sleep 2` and the stencil split (informative); stencil risk handled by J-5's own read and J-4c.**

VERDICT: HOLD
