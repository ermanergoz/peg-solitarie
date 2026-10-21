# ux-designer report — phase-15 UX refresh (design)

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (2397732..this commit — one `docs(15):` commit)
Skills loaded: ergoz-soft:standards (role: ux-designer), compose-skill, android-accessibility
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, audio.md, ip-policy.md,
testing.md, stacks.md, lessons.md
Ran in parallel with: phase-14 (in flight in the primary worktree — not touched)
Loops: 0
Lessons applied: none

## Summary
Wrote `docs/ux-spec.md` — the studio's first UX spec for this product — covering the founder's
five asks as decisions D-01…D-05 with sub-decisions, screen-by-screen contracts, an interaction
model, tokens, copy, accessibility and 34 acceptance criteria.
The load-bearing finding: **"remove toolbar" and "redesign the icons on the top" are the same
surface** — `GameScreen.kt:502` is one `TopAppBar` holding the back arrow, the title *and* all
five controls — so D-04/D-05 are one change with one replacement, not two.
No code was touched; nothing under `app/src/` and not `VERSION`.

## Outputs
- `docs/ux-spec.md` — the UX spec: 17 sections, decisions D-01…D-05 (+13 sub-decisions),
  five screen contracts, a negative contract, token/copy/asset tables, accessibility checklist,
  candidate acceptance criteria, two queued founder questions.
- `.planning/reports/phase-15/ux-designer.md` — this report.

## Checks
This is a design task and I hold no Bash tool, so no build was run. The verification below is what
the role's quality gate asks for: every load-bearing claim in the spec traced to the code or
document it came from.

- `read docs/ui/DESIGN-SYSTEM.md` — pass (design system of record; spec adds no colour, no
  gradient, no second visual language; §6 maps every new surface to an existing role)
- `read app/src/main/java/.../ui/game/GameScreen.kt:171-187, :490-527` — pass (**the D-05
  reconciliation**: one `Scaffold.topBar` → one `GameTopBar` → one `TopAppBar` at `:502` whose
  slots are back `:503-517`, title `:518`, five actions `:519-525`. There is no second control
  row. Toolbar and top icons are one surface.)
- `read .../ui/home/HomeScreen.kt:340-363` — pass (the exact deletion for D-02: the
  `Icons.Rounded.Info` `IconButton` at `:348-354`, `contentDescription = R.string.how_to_play`;
  param at `:129`, passed at `:151`)
- `read .../ui/home/HomeRoute.kt:48` — pass (`onShowHowToPlay = viewModel::onShowHowToPlay` — the
  wiring that moves to Settings)
- `read .../ui/home/HomeUiState.kt:69-85` — pass (verified against the brief: Home holds the
  challenge level and the two resume ids and **no** star/score/time data — so the ribbon's two
  derived values are marked as data requirements R-1/R-2, not assumed)
- `read .../data/ProgressRepository.kt:83, :115, :284-291` — pass (`observeResults` is a `Flow`
  carrying `stars`/`bestTimeMs`/`bestScore`; `aggregate()` is a `suspend` one-shot — which is why
  the spec folds the flow and forbids the aggregate)
- `read .../data/db/LevelResultDao.kt:59-75` — pass (`totalStars` is a deliberate both-modes
  `SUM` with no `WHERE` at `:66`, so it cannot answer "Classic boards cleared" — recorded in R-2)
- `read .../data/ChallengeProgress.kt:64-74` + `ProgressRepository.kt:107-113` — pass
  (`currentLevel` is the **next** level and is off-by-one from the highest banked, by design —
  the spec fixes the Level-pill copy to "Level N" and forbids reconciling the two)
- `read .../ui/game/HintToggleControl.kt:42-52` — pass (**defect confirmed**: ON/OFF carried by
  `primary` vs `onSurfaceVariant` tint alone — a rules/compose.md "never colour alone" violation,
  fixed by D-04b's filled-vs-outlined pill)
- `read .../ui/game/HintControl.kt:5, :69` and `.../ui/home/HomeScreen.kt:23` — pass (**two glyph
  defects confirmed**: Hint uses `Icons.Rounded.Search`; `Icons.Rounded.Info` is used for two
  unrelated meanings — the game's show-moves toggle and Home's how-to-play)
- `read .../ui/classic/BoardPreview.kt:79, :86` — pass (**latent defect confirmed**:
  `BoardFootprint` fixes its own size at `catalog_preview_size` 96dp, so the
  `onboarding_illustration_size` 160dp passed at `OnboardingOverlay.kt:144` has never taken
  effect; and `interactive = false` means the current tutorial board can never be tapped — which
  is why D-01a routes the new tutorial through `BoardCanvas` directly)
- `read .../ui/components/BoardCanvas.kt:87-93, :205-212, :298-304` — pass (takes a whole
  `GameUiState`, gates taps on `interactive`, and already resolves instantly when animations are
  off — the interactive tutorial inherits all three)
- `read .../ui/theme/Clay.kt:150-173, :209-218` and `ClayAnim.kt` — pass (reduced-motion gate is
  `rememberAnimationsEnabled()`; `claySquish`/`clayEntrance` already no-op — §10 reuses them and
  adds gates only for genuinely new motion)
- `read .../navigation/AppNavHost.kt:131` + `.../ui/results/AdQuiescenceScrim.kt:61` — pass
  (gameplay back is a bare `popBackStack()` and there is **no** `BackHandler` on gameplay — the
  only one in the repo is the scrim — so D-05b makes a visible back control mandatory)
- `read .../audio/SoundCue.kt` + `.planning/phases/07-sound/07-CONTEXT.md` D-09 — pass (all 12
  cues the refresh needs already exist; toggles and back navigation are contractually silent —
  **zero new audio assets**, so the asset table in §9 has no new licence row)
- `read app/src/main/res/values/dimens.xml:2-9, :22, :51, :110` — pass (the `dpSpLiteralCheck`
  gate and the one-token-per-owner convention; §6.1 declares 15 new tokens under it and retires 2)
- `read app/src/test/.../ButtonSoundTest.kt:105-123, :226-244` — pass (**test impact confirmed**:
  the how-to-play test breaks on D-02; the game-screen test passes unchanged because D-05b/§4.4
  deliberately preserve `navigate_back` and `show_moves_on`)
- `read app/src/test/.../ui/home/HomeScreenTest.kt:148-159, :367` and `.../game/GameFlowTest.kt:129`
  — pass (the overlay is found only by the hardcoded literal `"Got it"`; the timer by
  `elapsed_time`, which §4.3 preserves)
- `grep roborazzi|paparazzi|screenshotTest` across the version catalog and build files — pass
  (**zero hits, and no `androidTest` source set**: there are no screenshot tests despite CLAUDE.md
  naming Roborazzi the primary tool — raised in §13 as a risk for a visual refresh)
- `read .planning/phases/09-consent-rewarded-ads/09-UI-SPEC.md` + `07-CONTEXT.md` + `ROADMAP.md`
  — pass (house conventions adopted: section order, `- **D-nn:** <ruling> — <rationale>` decision
  style, `**Success Criteria** (what must be TRUE)` present-tense criteria with requirement ids,
  the `Checker Sign-Off` block, and the `3a.` late-addition sub-numbering used for 15a–15f)
- `read .claude/rules/{compose,puzzle-design,audio,ip-policy,testing}.md` — pass (48dp/8dp,
  colour-never-alone, reduced motion, light+dark, thumb zone, teach-without-text, first failure
  impossible, sound never the only feedback, licence per asset — each mapped to a spec section
  and at least one acceptance criterion)
- `write docs/ux-spec.md` — pass (created; role quality gate: every screen in the core flow has a
  spec section, the accessibility section is complete, every asset carries a licence)
- `./gradlew check` — **not run** (design-only task; no file under `app/src/` was created,
  edited or deleted, so the build is unchanged from the branch point)

## Open issues / risks

**Two founder questions, both non-blocking, both with a decided default (spec §15).** Per the
brief I did not ask the founder; these are queued for the producer to file in
`.planning/questions/FOUNDER.md`.
- **Q-901 — Home hero art direction.** Static peg cluster / drifting mini-board / title only.
  Default and recommendation: **static**. It is also the cheapest on the cold-start gate.
  Blocks: nothing.
- **Q-902 — do the in-game actions move to the bottom?** The founder said "redesign the icons on
  the top" and "remove toolbar" in one breath. I read that as one surface and split it: status
  stays top, the four actions move to a thumb-zone dock. Default and recommendation: **the
  split**, because rules/compose.md requires primary actions in the lower thumb zone and Undo —
  the control a peg-solitaire player uses most — is currently in the worst-reachable corner. If
  the founder wants them visually on top, §4.3 and §4.4 swap places and nothing else changes.
  Blocks: nothing.

**Risks and handoffs, in priority order.**
1. **Home is on a measured gate.** PLAT-08 C1 (≤ 950 ms cold start) makes every added Home element
   first-frame work. §6.5 sets a budget (no blocking read, no decode, no asset load), flags
   R-1/R-2 as the single real risk, and gives an **ordered degradation path** that costs no
   oracle. This is a design budget, not a measurement — the developer must measure C1 with the
   fuller Home before the phase closes.
2. **Three Home readouts are device-test oracles** (§4.2.1, D-03f): the Challenge tile's level
   chip, the Continue chips, and the Classic catalogue's per-board star pips. `11-HUMAN-UAT.md`
   tests 10/11/13 read pass/fail off them, so hiding one makes cloud save unverifiable on a
   device. The spec is designed so all three run unchanged, and four tempting simplifications are
   recorded as considered-and-rejected. **Note for accuracy:** per-board stars have never been on
   Home — they live on the Classic catalogue, which this refresh does not touch; what the spec
   protects is the one-tap path to them and the rule that Home's aggregate Stars pill never
   replaces them.
3. **Two existing tests break by design** and need rewriting with the implementation:
   `ButtonSoundTest.kt:105-123` (clicks the deleted `how_to_play` button) and
   `HomeScreenTest.kt:148-159` (finds the tutorial by the hardcoded literal `"Got it"`, and Home
   stops hosting it). Two others pass unchanged because I preserved their content descriptions
   deliberately.
4. **Three real accessibility/UI defects are fixed by this refresh, not introduced by it**:
   show-moves state carried by tint alone (`HintToggleControl.kt:45-51`), `Icons.Rounded.Info`
   used for two unrelated meanings, and `Icons.Rounded.Search` standing in for "hint". Worth
   calling out so they are not read as gratuitous churn in review.
5. **Tutorial board content is a game-designer input, not mine** (§7 R-4). T2/T3 footprints must
   be Solver-verified one-peg-reachable from every legal line so "first failure impossible"
   (rules/puzzle-design.md) holds structurally. Defaults are specified so nothing blocks.
6. **Hosting mechanism for the tutorial is the tech-lead's call** (D-01f). I decided it is a
   *screen* (screen inventory is this role's authority); whether that is a nav destination or a
   full-screen composable is architecture. §7 names the one sequencing invariant it must
   preserve: the Play Games prompt and profile-swap dialog gate on the **persisted**
   `onboardingSeen` flag, so `markSeen()` must land before the player returns to Home, and
   whatever replaces `HomeScreen.kt:198`'s guard must keep the same one-way safety.
7. **No screenshot tests exist** (no Roborazzi/Paparazzi, no `androidTest`) even though CLAUDE.md
   names Roborazzi the primary tool. A five-screen visual refresh is exactly what a golden-image
   suite would protect. Raised, not decided — tech-lead and qa-engineer own it.
8. **Path convention.** The house convention for a phase contract is
   `.planning/phases/<NN>-<slug>/<NN>-UI-SPEC.md`; the role's required output is
   `docs/ux-spec.md`. I wrote the required path and followed the `09-UI-SPEC.md` section order,
   and noted in the spec header that a phase-scoped `15-UI-SPEC.md`, if wanted, should point at
   this file rather than fork it. There is also no `ROADMAP.md` Phase 15 entry and no
   `.planning/phases/15-*` directory yet — §14's criteria are written to drop into one.

Not touched, per instruction: `app/src/**`, the root `VERSION`, `.planning/reports/STATUS.md`,
and every worktree other than `PegSolitarie-p15`.

## Handoff
Next: **producer** — file Q-901 and Q-902 in `.planning/questions/FOUNDER.md` (both
`Blocks: nothing`), add the Phase 15 roadmap entry from §14, and route:
- **game-designer** — §7 R-4, the T2/T3 tutorial footprints (non-blocking; defaults ship).
- **tech-lead** — D-01f tutorial hosting + the §7 sequencing invariant; §7 R-1/R-2 and the §6.5
  cold-start budget; the §13 screenshot-test gap.
- **android-game-dev** — build from §4 (surfaces), §6 (tokens, incl. 15 new dimens and 2 retired),
  §8 (copy: 14 new strings, 1 deleted, 1 stale fix) and §9 (2 new in-house vectors, no
  third-party asset, no new audio). Read §13 before starting: two tests break by design, two must
  keep passing.
- **qa-engineer** — §14's 34 acceptance criteria are written as device-checkable truths; 15a–15f
  cover the oracle and cold-start constraints, and §11 is the accessibility pass list.
