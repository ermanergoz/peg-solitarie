# ux-designer report — phase-15 Home layout ruling (vertical slack) + the split semantics nodes

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (4410c74..9650f3d, plus this docs commit)
Skills loaded: ergoz-soft:standards (role: ux-designer), compose-skill
Ran in parallel with: the phase-14 external/release track (other worktrees; untouched here)
Loops: 0
Lessons applied: none

Staffing note (one line, per the dispatch): this role is normally Fable; its allowance is exhausted,
so this ruling ran on Opus by the founder's decision recorded as Q-003 — an authoring role, so a
staffing note and not an independence question.

## Summary

Home's fixed `Spacer` is wrong on **both** screen sizes, not a compromise between them: it leaves
~45% dead space on a 6.7-inch phone and, by the spec's own token arithmetic, the full band set
needs ≈716dp against a 360×640dp window's ~568dp, so criterion 15a was already unreachable before
anything was pushed down. Ruled **D-03i** (§4.2.2): two weighted `Spacer`s inside a column whose
minimum height is the viewport — the one thing that makes a weight work under `verticalScroll` —
plus a compact band set (no hero art, one-row hero at 72dp, `spacing_sm`) below 720dp of available
height. **The ruling requires a change to the build.** The split accessibility nodes are **not** a
defect and need **no** code change; the TalkBack pass criterion 26 always demanded stays owed.

## Outputs

- `docs/ux-spec.md` — amended **additively**, nothing rewritten in place:
  - header **"Fourth revision — 2026-09-28"** note; `revised:` bumped to 2026-09-28
  - **D-03i** in §3 (Home block), with a one-line pointer added to D-03
  - **§4.2.2 "Vertical slack, and the band set a short screen gets"** — the layout contract:
    arithmetic for both baselines, the `BoxWithConstraints` + `heightIn(min = …)` mechanism, the
    contract modifier order, the 3:1 slack split, the ordered yield, what never yields, the
    cold-start obligation, and the tests it owes
  - a pointer paragraph under §4.2's **"Thumb zone"** (its text left legible and unchanged)
  - §6.1: two new tokens — `home_hero_compact_min_height` 72dp, `home_compact_threshold_height` 720dp
  - §11: **"The unmerged tree is not the contract"** — the accessibility ruling
  - §12: an additive note, incl. a correction of fact on the "Tablet / large" row
  - §14: new criteria **15g, 15h, 15i, 15j** (Home vertical slack) and **26a** (how 26 is checked)
  - §16: an additive line on PERS-03 / CHAL-05 / PLAT-08
- `.planning/reports/phase-15/ux-designer-home-layout.md` — this report

## Checks

I hold Read/Write/Edit/Skill/Agent only — **no Bash tool** — so no command was run by me. Every
check below is a read of the branch's own files at the cited `file:line`; two read-only Explore
helpers collected the wider citations and a general-purpose helper ran the `git` commit (no studio
role was dispatched, per rules/dispatch.md). Verdicts are against the built code, not against the
developer's reasoning, which I did not read except for the two findings I was asked to rule on.

**The built `HomeScreen.kt` (what I actually read)**

- `HomeScreen.kt:296-303` — the band column: `fillMaxSize()` → `verticalScroll(rememberScrollState())`
  → `widthIn(max = content_max_width)` → `padding(spacing_md)`, `Arrangement.spacedBy(spacing_md)`
  — **fail** (two separate faults: no slack distribution, and `fillMaxSize()` ahead of `widthIn`
  makes the 480dp cap and §12's centring a no-op on a window wider than 480dp).
- `HomeScreen.kt:311-313` — the fixed `Spacer(Modifier.height(spacing_lg))` — **fail** (24dp of
  height, and 40dp of real cost: as a child of `spacedBy(spacing_md)` it brings a second 16dp gap,
  16dp of which expresses no intent).
- `HomeScreen.kt:272-279` — the `HomeBands` KDoc still describes "The weighted spacer … so on a tall
  phone the slack goes *above* the Continue chips" — **fail** (the doc asserts a behaviour the code
  does not have; nothing in the column consumes leftover height). Same mismatch in the inline
  comment at `:311-312` ("the slack goes here").
- `HomeScreen.kt:352-353` — band 5 is a comment only, no composable — **noted**: §4.2.2 makes the
  reserved footer the second slack holder, still empty of content.
- `HomeScreen.kt:396` (`resume_tile_min_height`), `:568` (`mode_tile_min_height`), `:611-614`
  (`CONTENT_WEIGHT = 1f` and why a shared literal is not reused) — **pass**, and they are the floors
  §4.2.2's arithmetic is built from.
- No `Modifier.weight` on any direct child of that column; every `weight` in `ui/home` is a `Row`
  weight (`HomeProgressRibbon.kt:70,80,90`, `HomeScreen.kt:407`, `:593`) — **pass** as a reading:
  the build's diagnosis that `weight` cannot distribute under `verticalScroll` is **correct**, and
  §4.2.2 states why (`Column` distributes over its maximum main-axis size while that is finite and
  over its **minimum** when it is not).
- `HomeHero.kt:51-56` (`heightIn(min = home_hero_min_height)`, no fixed height, no `fillMaxHeight`),
  `:58-63` (art 120dp + `spacing_sm` + `displaySmall` ⇒ **≈172dp as built**), `:70-83` (the gear) —
  **pass**; the hero is a minimum, which is what lets it be the band that yields.
- `HomeProgressRibbon.kt:129-136` (`heightIn(min = home_stat_pill_min_height)` +
  `clearAndSetSemantics`) — **pass**, and it is why §4.2.2 rules the ribbon *cannot* yield: pill
  content is ~60dp inside a 64dp floor, so a smaller token buys 4dp and lies about the layout.
- `dimens.xml:12-16` (spacing scale), `:31` (480dp cap), `:33` (96dp tiles), `:80` (160dp hero
  minimum, with the file's own comment that it is a minimum so the hero yields), `:88` (64dp pill),
  `:139` (72dp chip) — **pass**; these are the numbers in §4.2.2's table.

**Each criterion I reasoned against**

- **Criterion 14** (`ux-spec.md:1593-1594`, max font size, nothing clipped) — **preserved, and it is
  the reason no content band may carry a weight**: a weighted child with `fill = true` is measured at
  exactly its share, so a weighted hero would be measured at 0 the moment the fixed bands exceed the
  viewport. §4.2.2 clause 4 forbids it and criterion 15i re-checks 14 on both baselines.
- **Criterion 15** (`:1595`, tiles in the lower half of a 6.7-inch phone) — **at risk on the build,
  and left textually untouched.** Measured 36–54% with ~45% below; my model of the built layout
  reproduces it (fixed bands ≈540dp of an ≈843dp content area in the fresh-install state). D-03i's
  3:1 split puts the first tile's top at 62–68% in both states, and the worst case at the threshold
  (fresh install at exactly 720dp) is 405dp against a 360dp midpoint. 15g is the both-sizes
  restatement; criterion 15's own wording is **not** reworded — the layout was wrong, not the words.
- **Criterion 15a** (`:1600-1602`, 360×640dp, both games saved, chips + both tiles without
  scrolling) — **fails on the build, and not because of anything a spacer does.** Full set with both
  chips: padding 32 + gaps 112 + hero ≈172 + ribbon 64 + chips 144 + tiles 192 = **≈716dp**, against
  ≤640dp of window and ~568dp of content area. The overflow is ~148dp; the spacer is 40dp of it. The
  hero's *minimum* is 160dp, so "the hero yields" as §4.2 states it cannot close the gap — which is
  why D-03i rules a compact hero (72dp, no art) and `spacing_sm`, reaching ≈544dp. **This ruling is
  what makes 15a reachable**; 15h states it so it is checkable with the compaction visible.
- **Criterion 15d** (`:1608-1609`, nothing behind a tap, an expander or a scroll position) —
  **preserved explicitly**: the compact set removes one decorative drawing and 64dp of whitespace,
  never a readout, a control or a band, and collapses nothing.
- **§4.2.1's four considered-and-rejected simplifications** (`:818-830`) — **none reintroduced**;
  §4.2.2 names all four as out of bounds for the compact set (no merged Continue button, the ladder
  level stays on the tile chip, no per-board pips on Home, no recent-activity band).
- **Criterion 15e/15f + §6.5** (`:1613-1624`, `:1076-1167`) — the mechanism adds no read, no decode,
  no asset and no SDK call (one `SubcomposeLayout`, two empty `Spacer`s; the compact set *removes* a
  `BoardCanvas` draw), but it changes the start destination's layout, so §6.5's two-arm measurement
  is taken on the branch that lands it. Recorded in §4.2.2 and in §16.
- **§6.1 / QUAL-03** (`:1012-1044`) — both new values are named tokens and multiples of 4; the two
  slack weights are unitless and go beside `CONTENT_WEIGHT`, per the header's scoped-constants rule.
  The `dpSpLiteralCheck` gate is satisfied by construction.
- **§12** (`:1415-1423`) — the "Tablet / large" row is contract and is **not true of the build**;
  corrected additively rather than rewritten, with criterion 15j as its check.
- **Criterion 26 / rules/compose.md accessibility** (`:1653-1654`, `.claude/rules/compose.md:13-16`)
  — see the accessibility ruling below.

**Does the ruling require a change to the build? — YES, for the layout; NO, for the accessibility
finding.** Concretely, for the producer to route:

1. `HomeBands` gets a `BoxWithConstraints` root; the column's chain becomes
   `align(TopCenter)` → `verticalScroll` → `heightIn(min = available height)` →
   `widthIn(max = content_max_width)` → `padding(…)`, with `fillMaxSize()` removed.
2. The fixed `Spacer` at `:313` becomes a weighted one; band 5 becomes the second weighted one; the
   3:1 ratio lands as two named constants beside `CONTENT_WEIGHT`.
3. `HomeHero` gains a compact form (one row: gradient title + 48dp gear, ≥72dp, no art) selected on
   available height, not on font scale; band spacing/padding switch with it.
4. Two tokens in `dimens.xml`; the `HomeBands`/`:311-312` KDoc and comment stop describing a
   weighted spacer that is not there.
5. Four JVM layout guards (§4.2.2 "What this owes in tests"), and §6.5's cold-start measurement
   taken on the resulting branch.

## Accessibility ruling — the split clickable/labelled nodes (asked for, and not fixed)

**Verdict: acceptable. It is not a defect at all, and therefore not a defect this phase is shipping
on top of.** Reasoning, recorded in §11 so it is contract rather than an opinion in a report:

- The shape is Material 3's own `IconButton { Icon(contentDescription = …) }` — click on the button
  node, label on the glyph (`UndoControl.kt:48-59`, `HintControl.kt:66-76`,
  `HintToggleControl.kt:50-62`, `RestartControl.kt:39-47`, `GameStatusStrip.kt:105-125`,
  `HomeHero.kt:78-83`), identical on four untouched screens (`ClassicCatalogScreen.kt:102-110`,
  `SettingsScreen.kt:500`, `ResultsScreen.kt:173`).
- rules/compose.md requires "contentDescription or semantics role on every interactive element"; it
  does not legislate node counts in an unmerged projection. `Modifier.clickable` merges its
  descendants' semantics, TalkBack consumes the **merged** tree, and every existing test finds and
  clicks these controls by description through it (`ButtonSoundTest.kt:312-315`) — which is the
  evidence that the label and the action are on one node where it counts.
- Not introduced and not worsened: the only dimension this phase changed moved the right way —
  48dp → `game_action_button_size` 56dp (`dimens.xml:113-118`) — better for touch, neutral for
  TalkBack.
- **Do not "fix" it.** `clearAndSetSemantics` or a merge on the dock chips would drop `Role.Button`
  and the `enabled = false` "disabled" announcement D-04d depends on, and would break the
  deliberately-unmerged helper at `GameScreenChromeTest.kt:164-172`. `HintControl`'s thinking branch
  (`HintControl.kt:50-64`) is the one sanctioned variant and only because its child is a decorative
  spinner with no label of its own.
- **What stays owed:** criterion 26 was never satisfied by a `uiautomator` dump. New criterion 26a
  fixes the bright line — one focus stop, label + role announced, activatable, ≥48dp focus
  rectangle; two focus stops or an unlabelled one is a FAIL — and requires the catalogue back arrow
  as the control case so a real finding can be told from this app's baseline.

## Open issues / risks

1. **Compact-set headroom is thin.** ≈544dp against ~568dp of content area on a 360×640dp device
   with a 48dp navigation bar. Every number is a minimum, so a device that overflows is possible;
   §4.2.2 says **stop and report the measured numbers** rather than improvise, because the next
   candidate to yield is a readout or a target and that is mine to rule on measured evidence.
2. **A 360×780dp-class device takes the compact set** although the full set fits its fresh-install
   state. Deliberate: a height-only threshold keeps the screen's shape stable per device, instead of
   reshaping the hero the first time a player leaves a game unfinished. Flagged so it is not read as
   an oversight.
3. **The 480dp cap and centring are broken on the build** (`HomeScreen.kt:296-303`) — a §12 contract
   since Phase 2, unrelated to the slack but in the same modifier chain, so D-03i fixes it in one
   pass. Tablet is not a target device, which is why this is a risk line and not an escalation.
4. **Criterion 26 remains unverified** app-wide (no TalkBack pass on any screen yet; there is no
   `androidTest` source set, so nothing covers it off-device either).
5. **No `docs/audio-spec.md`** still — unchanged by this ruling, the sound-designer's, already
   tracked by the producer. Not re-raised beyond this line.
6. I could not run the repo Check command (no Bash tool) and did not need to: this change is
   documentation only. Nothing under `app/src/` or `domain/src/` was touched, and no file another
   role authored was edited.

## Handoff

Next: **producer** — route item 1–5 under "Does the ruling require a change to the build?" to
**android-game-dev** as a follow-up slice on this branch or the next, then re-run §6.5's cold-start
measurement on whatever branch lands it. Tell the **code-auditor** and the **qa-engineer** now in
flight that `docs/ux-spec.md` gained §4.2.2, criteria 15g–15j and 26a **after** the build they are
judging: criteria 14, 15, 15a and 15d are unchanged and are still the contract for that diff, and
the accessibility finding in `android-game-dev-s5-s8.md:213-220` is ruled **not a defect** (§11), so
neither report needs to hold a verdict for it. The qa-engineer additionally owns the TalkBack pass
that criterion 26a now specifies.
