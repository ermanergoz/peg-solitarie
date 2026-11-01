# tech-lead report — candy-backdrop: final technical sign-off (closes the HOLD in `tech-lead-signoff.md`)
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; reviewed at HEAD `a0adbc0`, whose code equals `fd45fd0`; develop `452f187` = `git merge-base HEAD develop`)
Skills loaded: ergoz-soft:standards (role tech-lead; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch in full, plus `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: the ux-designer's battery-saver ruling (`ux-designer-bd23d-bs.md`, not yet on disk when this report was written)
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins fable at xhigh. The founder's Q-019 answer 3 puts Fable-pinned roles on Opus, and its addendum of 2026-10-24 restores that answer (`.planning/questions/FOUNDER.md`, Q-019).
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **J-5 closes BD-23 (c).** QA's run of record (attempt 2) gives C − A = **+57.7 ms** against T = 100 ms (floor 30.2 ms). Before J-4b it was +216 / +225 ms. Attempt 1 was VOID, and attempt 2 was the one re-attempt the rule allows.
- **Every other condition is met:**
  - the auditor's PASS after F1, with Q-019's addendum of 2026-10-24 settling the auditor's model concern;
  - A1–A6, R-1 and D1–D6;
  - 15e not owed, since §3's conditions hold;
  - BD-23 (d) items (1)–(7);
  - boundaries;
  - the branch is up to date.
- **No develop merge-in is owed.** `develop` = `452f187`, which is already the merge-base (taken in by `eee370e`).
- **One condition is outstanding:** BD-23 (d)'s battery-saver clause. It FAILed item (4) by 45 ms in one of four runs, and the ux-designer is ruling on it.
- **VERDICT: PASS, conditional on that ruling.** It must close the clause on the existing data, as a line defect or a pass on QA's four runs, with no product change and no new device run. Otherwise HOLD, with the exact next steps in §3.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-signoff-final.md`: this report. It holds the condition table, the conditional verdict, the HOLD paths, the Q-PUBLISH-3.0.0 lines and the 3.0.1 list.
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-signoff.md`: a short closing addendum that points here.

## 1. Sign-off conditions

| Condition | Evidence | State |
|---|---|---|
| Check green on the code under review | Developer's Check at `fd45fd0`: 1,641 app tests and 294 domain tests, 0 failures, lint, ktlint, detekt, kover and depthSweep run (`android-game-dev-f1.md`). `git diff --quiet fd45fd0 HEAD -- . ':(exclude).planning' ':(exclude)docs'` exits 0 | met |
| code-auditor PASS | `code-auditor-j4b.md`: FAIL on F1 only, a test gap. `android-game-dev-f1.md` added the test. `code-auditor-j4b-loop1.md`: PASS, the new test red on ME and ME2, the D5 line red on M8 and M8b | met |
| Auditor's model concern (loop 1, Open issue 1) | `FOUNDER.md` Q-019, "Addendum 2026-10-24 … option 3 stands again: every Fable-pinned role runs on Opus". The addendum post-dates the 10-23 one the auditor cited, so its Opus run is the gate under the founder's standing answer. Its mitigations are in both auditor reports: the Check run by the auditor, forced ktlint and detekt, its own mutants | met |
| J-4b amendments A1–A6, rider R-1 | `code-auditor-j4b.md` §"What I verified": A1 scaled open (MC, MC2 red), A2 tokens with no `ui/home` import, A3 (first branch now pinned by `eef1f46`), A4 `ModulateAlpha`, A5 plain `remember`, A6 timeline (open on frame 30, scene composed on 31, nothing inside `drawWithCache` changed), R-1 `LaunchedEffect(density)` | met |
| D1–D6 | `tech-lead-j4b-d-rulings.md`. The optional D5 tightening has been made (`fd45fd0`) | met |
| Ruled test edits | §4 amendments: setup lines only, both bounded (auditor, J-4b). Since then: `eef1f46` and `fd45fd0` add lines only, with no assertion removed (auditor, loop 1) | met |
| 15e not owed | §3 condition 1: `git diff --name-only d029950 HEAD -- app/src/main` lists only `ui/backdrop/BackdropMotion.kt`, `BackdropSceneReady.kt` and `SceneBackdrop.kt`, and `AppNavHost.kt:170` is unchanged. Condition 2 was checked by the auditor on the diff | met |
| BD-23 (a) | `BackdropFirstFrameTest`, bounded on both sides; `BackdropSceneOpenTest` (8 tests), inside the green Check | met |
| BD-23 (b), 15e | `qa-engineer-15e.md`: PASS, +44.5 ms. No start-path file has changed since, on the branch or on develop | met |
| BD-23 (c), J-5 | `qa-engineer-j5.md`, attempt 2: S(C) 458.2 against S(A) 400.5 ms, Δ +57.7 ms, T = max(100, 2 × 30.2) = 100 → PASS. Every label met its kept target (12/12, 12/12, 8/8, 8/8), 120/120 launches were COLD, and no block held ≥ 120 rows. Arm C = `0c45666`, whose product code equals HEAD's (only two test files differ) | **met** |
| BD-23 (d), items (1)–(7) | `ux-designer-bd23d.md` (B-11c restated (2) and (6) as a measurement correction). `qa-engineer-bd23d-close.md` re-derived D at 486–733 ms (≥ 350) and (6) at 4/4 | met |
| BD-23 (d), battery-saver clause | `qa-engineer-bd23d-close.md`: (1)–(3) and the rest pose hold 4/4. **(4) FAILs in `bs-light-2`: 2.045 s against ≤ 2.0 s**, counted from H. Counted from Hs it is 1.823 s, a reading that was not registered | **condition (§2)** |
| 15f | J-3 PASS (`qa-engineer-jank2.md`) | met |
| BD-09 | `BackdropStatusBandTest`, exact equality with the theme's `background`. Unchanged | met |
| Boundaries | `develop...HEAD` touches `app/` only (navigation, ui, `res/values/dimens.xml`, tests) plus `.planning/` and `docs/`. No Gradle, catalogue, manifest, schema, domain, `THIRD_PARTY.md`, `res/font` or `themes.xml` change. New `@Suppress`/`@Ignore`/`!!`: 0. `ui/backdrop/` imports nothing from `data`, `domain` or `ui.home`, and uses no random source, wall clock, `painterResource` or DataStore. No new dependency, so no licence row is owed | met |
| Tech-design adherence | R1, R3 and the J-4b amendments as built. D1's wording in `docs/ux/candy-backdrop.md` (BD-23 (a), B-08) and my D1 line in `docs/tech-design.md` are not yet written. They are a docs sync after the merge, not gating (`tech-lead-j4b-d-rulings.md` §1) | met |
| Branch up to date | `git rev-parse develop` = `452f187` = `git merge-base HEAD develop`. `git log merge-base..develop` is empty, and `eee370e`'s second parent is `452f187`. **No further merge-in is owed**: the docs merges `0ac2abd`, `8a7cb89` and `452f187` are already in | met |

## 2. The verdict and its one condition
**PASS, conditional on `ux-designer-bd23d-bs.md`.** The ruling must close BD-23 (d)'s battery-saver clause on the existing data, with no product change and no new device run. Two kinds of ruling count:
- the line is defective, for example (4)'s clock starting at H inside the system splash cross-fade, or "per theme" being unreachable in light;
- the clause passes on QA's four runs.

When it does, the producer merges with no further dispatch of me and cites that report in STATUS.md.

**Technical input for the ux-designer's ruling:**
- The product's timing is fixed in tokens. After the gate's first awaited frame, the scene takes 440 ms (open) + 700 ms (fade) = **1.14 s** of scaled animation time to be fully in. That leaves (4)'s 2.0 s line about 0.86 s for H → the gate's first frame.
- On this bench that span depends on the bench, not the build. T − H was 0.94–0.97 s in the two light-setting battery-saver runs, against 0.52–0.59 s in the dark-setting ones. `bs-light-1`'s TotalTime was 4,287 ms.
- J-5's dark runs without battery saver already reached 1.959 s.
- The miss turns on one code value at the end of the fade.
- So the bench shows no product fault behind (4). It shows a line with almost no margin on a slow emulator start. The ruling on the line is the ux-designer's.

## 3. If the ruling is anything else: HOLD, with the exact next step
1. **A corrected line that needs a fresh run** (rules/testing.md: a corrected line counts only on a new run).
   - **Next step:** the qa-engineer runs the battery-saver clause under the corrected line, at one bench visit on `Pixel_9a_bench`, with J-5's arm C APK (`armC-0c45666.apk`, sha256 `3e974016…ed96b`). No build is needed, because HEAD's product code equals `0c45666`.
   - The report goes to `.planning/reports/candy-backdrop/qa-engineer-bd23d-bs2.md`.
   - On a **PASS**, the condition is met with no further dispatch of me.
   - On a **FAIL**, path 2 applies.
2. **A product finding.** The ux-designer rules that the player-visible timing in battery saver must change.
   - **Next step:** the ux-designer names the visible change, for example the fade's length or the open in battery saver.
   - The developer proposes it, test-first, confined to `ui/backdrop/`, and I rule on it. §3's two 15e conditions apply again.
   - Then the code-auditor reviews the diff, and the qa-engineer re-runs the battery-saver clause and every BD-23 (d) item the change touches. J-5 is owed only if the gate's timing with animations on changes.
   - If that cannot be done this week, the producer may put it to the founder as one `Q-<NNN>`: ship with the recorded residual, or slip. My recommendation would be **ship**, for two reasons. The miss is 45 ms, on an emulator, in a power mode that forces the dark theme. And it falls on a decorative fade that never blocks input.

## 4. Q-PUBLISH-3.0.0 lines from this branch (emulator only; no physical phone was measured)
- **Cold start (TTID): +44.5 ms** against develop on `Pixel_9a_bench` (455.0 against 410.5 ms; null floor 6.5 ms; line 100 ms; PASS, `qa-engineer-15e.md`). J-4b did not touch the start path.
- **Settle of the first five frames after Home's first paint: +57.7 ms** against develop (458.2 against 400.5 ms; null floor 30.2 ms; line 100 ms; PASS, `qa-engineer-j5.md`). Before the J-4b fix it was +216 to +225 ms.
- **Remaining frame-1 cost, from Home's own finishes:**
  - The RenderThread is +68 ms on frame 1 (208.1 against 139.7 ms) and +22 / +11 ms on frames 2 and 3.
  - The scene is not drawn on those frames. A stencil attachment of about 7.5 MB appears in every C dump and in no develop dump.
  - This is consistent with the +44.5 ms TTID.
- **The moved scene draw:** one stall of about **49 ms** (develop: 33 ms) on a still Home, about 0.6 s after the entrance.
- **There is no steady-state jank.**
- **What the player sees:**
  - The waves and lights now appear after Home's entrance, 440 ms at animator scale 1, and fade in over 700 ms.
  - This happens once per host start, which includes every configuration-change recreation.
  - A tap during the fade may be delayed once, never lost.
  - With animations off, the scene appears on frame 3 with no fade.
- **Measurement scope:**
  - Every number comes from one emulator, API 36, `Skia (OpenGL)` through host-GLES translation, with debug APKs.
  - The physical-phone ≤ 2 s start bar (D-03j, PLAT-08) was not re-measured on a phone.
  - A phone GPU with a warm shader cache will likely show smaller first-draw costs. The bench cannot show that either way.
- **Battery saver:**
  - The open and fade run at the rest pose.
  - On API 36, battery saver forces the dark theme, so the light theme is never shown in battery saver.
  - On the bench the scene was fully in 1.56–2.05 s after Home's first recorded frame.
  - Pending the ux-designer's ruling, the line reads: "one of four runs was 45 ms over the 2.0 s line, counted from the system splash; the cause is the bench's slow forced-dark cold start, not the fade".

## 5. 3.0.1 items (the producer files them)
**New from J-4b and J-5:**
1. **Home's finishes on the start path.** Audit the +68 ms RenderThread cost on frame 1 and the about 7.5 MB stencil attachment, which come from non-convex finish lips. A fix touches `ui/theme/` and `ui/home/`, so 15e is owed again.
2. **Phone frame and start timing.** Use Macrobenchmark `FrameTimingMetric` and `StartupTimingMetric` with `reportFullyDrawn()`. This replaces the adb `gfxinfo` and `screenrecord` reads: `screenrecord` writes frames only on change, at 16–420 ms on this bench. *(Carried; now also the instrument for item 1.)*
3. **BD-23 (d) (4)'s margin on slow dark and battery-saver starts.** QA open issue 3: J-5 `darkL-3` was at 1.959 s. This is for the ux-designer, unless §2's ruling already settles it.
4. **The D1 docs sync.** The ux-designer's wording for BD-23 (a) and B-08 ("the scene node under the cleared root carries a fourth test-only property, the scene's alpha reader"), and my D1 line in `docs/tech-design.md`'s candy-backdrop section. Due right after the merge.
5. **A static guard against gate-keyed effects in `ui/backdrop/`.** The R-1 undo (MD) is caught by no test. Fold it into tech design §16 #33's work.
6. **Test hygiene.** Move `Bitmap.differingPixels`, duplicated in `BackdropSceneOpenTest.kt` and `BackdropReducedMotionTest.kt`, into `testing/`.
7. **Bench infrastructure.** Exclude the AVD folders from Spotlight. `mds_stores` caused J-2's and J-5's first-attempt VOIDs.

**Carried from `tech-lead-signoff.md`, if not yet filed:**
- G-2, folding the two protected-names tests;
- `pseudoLocalesEnabled` on debug for BD-22;
- the QA rows skipped under reduced QA;
- the first-launch frame with black system bars.

## Checks
- `git rev-parse develop`; `git merge-base HEAD develop`; `git log --oneline <merge-base>..develop`; `git log -1 --format=%P eee370e`; `git merge-base --is-ancestor develop HEAD` — pass (`452f187` twice; empty; `635c9b8 452f187`; ancestor). `git -C ../PegSolitarie-dev log -1` is `452f187`. No remote.
- `git diff --quiet fd45fd0 HEAD -- . ':(exclude).planning' ':(exclude)docs'` — pass (exit 0). `git diff --name-only 0c45666 HEAD` without `.planning` and `docs` — pass (two test files only, so arm C is HEAD's product).
- `git diff --name-only d029950 HEAD -- app/src/main` — pass (three `ui/backdrop/` files). `grep -n 'rememberBackdropSceneReady()' AppNavHost.kt` — pass (`:170`).
- `git diff --name-only develop...HEAD | grep -E 'gradle|toml|THIRD_PARTY|build-logic|AndroidManifest|schemas|domain/|game-core|res/font|themes.xml|App.kt|MainActivity|/di/'` — pass (no output).
- `git diff develop...HEAD -- app/src/main | grep -cE '^\+.*(@Suppress|@Ignore|!!)'` — pass (0). The same over `app/src/test` for `@Suppress|@Ignore` — pass (0).
- `grep -rnE 'import com\.app\.pegsolitaire\.(data|domain|ui\.home)|java\.util\.Random|kotlin\.random|System\.currentTimeMillis|SystemClock|painterResource|DataStore' ui/backdrop/` — pass (no output).
- Read `FOUNDER.md` Q-019, both addenda — pass (2026-10-24 restores option 3).
- Read in full: `tech-lead-signoff.md`, `tech-lead-j2-ruling.md`, `tech-lead-j4b-ruling.md`, `tech-lead-j4b-d-rulings.md`, `code-auditor-j4b.md`, `android-game-dev-f1.md`, `code-auditor-j4b-loop1.md`, `qa-engineer-j5.md`, `ux-designer-b11a.md`, `ux-designer-bd23d.md` and `qa-engineer-bd23d-close.md`. Read in part: the summaries and outputs of `android-game-dev-j4b.md`; `android-game-dev-perf-audit.md` was already ruled on in `tech-lead-j4b-ruling.md` — pass.
- `ls .planning/reports/candy-backdrop/ux-designer-bd23d-bs.md` — not yet present, hence the conditional verdict.
- Not run: the Check, Gradle and any emulator. The developer's and the auditor's gates are not re-run (rules/studio.md), and nothing needed a build. Host load was 2.60. `.planning/log/` was not read.

## Open issues / risks
1. **The battery-saver ruling is outstanding.** The verdict is PASS only once it closes the clause as in §2. Until then, the producer must not merge.
2. **Emulator only.** Every number above is from one emulator's GL translation, and phone cost is unmeasured (§4, §5 item 2).
3. **The staffing deviation stands, under Q-019's 10-24 addendum.** Reviewers ran on the author's model, with the recorded mitigations.
4. **Trailer.** This commit carries the single Fable trailer that the product's snapshot `rules/gitflow.md` (imported by CLAUDE.md) requires, not the harness's session lines. The branch's mixed trailers remain the producer's to settle.

## Handoff
Next: producer —
1. Read `ux-designer-bd23d-bs.md` when it lands.
   - **If it closes the clause on the existing data** (a line defect, or a pass on QA's four runs): the sign-off is PASS. Merge `feature/candy-backdrop` into develop with `--no-ff`. No further merge-in is owed, and no further dispatch of me is needed. Cite this report and the ux report in STATUS.md.
   - **Otherwise:** follow §3, path 1 or path 2.
2. Put §4's lines into the Q-PUBLISH-3.0.0 material.
3. File §5's 3.0.1 items, and queue §5 item 4, the D1 docs sync, right after the merge.

VERDICT: PASS — conditional on `ux-designer-bd23d-bs.md` closing BD-23 (d)'s battery-saver clause on the existing data (line defect / PASS on QA's four runs, no product change, no new run); otherwise HOLD per §3.
