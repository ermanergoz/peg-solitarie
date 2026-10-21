---
role: ux-designer
phase: 15
slug: ux-refresh
status: draft
created: 2026-09-21
revised: 2026-10-21
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
---

# Peg Solitaire — UX Specification

> The studio's UX contract for Peg Solitaire (rules/studio.md: the ux-designer owns screen
> inventory, layout, interaction model, feedback, theme and accessibility). It is written for the
> **phase-15 five-part UX refresh** the founder asked for; it specifies the
> surfaces that refresh touches completely and references — never re-specifies — everything
> Phases 1–14 already locked.
>
> **The design system of record is `docs/ui/DESIGN-SYSTEM.md` ("Candy Clay").** Nothing here
> introduces a second visual language. Every colour is an M3 role or a `LocalClayColors`
> gradient; every fixed dp lands in `res/values/dimens.xml` as its own named token; every
> duration/ratio lands in a scoped constants object (`ClayAnim`, `GameAnim`, `BoardMetrics`, or a
> new `TutorialAnim`); every string lands in `res/values/strings.xml`. The `dpSpLiteralCheck`
> gate fails the build on a raw `.dp`/`.sp` literal in a composable
> (`app/src/main/res/values/dimens.xml:2-9`).
>
> **Format note.** The house convention for a *phase* contract is
> `.planning/phases/<NN>-<slug>/<NN>-UI-SPEC.md` (the `09-UI-SPEC.md` template). This document
> lives at `docs/ux-spec.md` because rules/reporting.md and the ux-designer role definition name
> that path as the role's durable output, and because this refresh spans five screens rather
> than one phase's deltas. It follows the `09-UI-SPEC.md` section order so a reader moving
> between the two is never surprised. If the producer wants a phase-scoped copy, this file is
> the source and `15-UI-SPEC.md` is a pointer to it — not a fork.
>
> **Amendments — revised 2026-09-21.** This edition folds in two rulings the first edition had
> deliberately routed out of its own authority:
>
> - `.planning/reports/phase-15/game-designer-tutorial-boards.md` — the tutorial's board content
>   (§7 R-4), now closed: three boards proven dead-end-free to one peg by exhaustive enumeration.
> - `.planning/reports/phase-15/tech-lead-architecture.md` — four architecture rulings: how the
>   tutorial is hosted (D-01f/R-5), how the Home progress values are read (§7 R-1/R-2), how the
>   cold-start gate is measured (§6.5, criterion 15e), and the screenshot-suite question (§13).
>
> Each ruling is authoritative in its own domain and is **integrated, not re-argued**; where one
> forced a change to a screen, the screen's answer is this role's and is stated here. Per the
> house style in §3, no earlier decision is rewritten in place: three of the rulings are filed as
> new decisions — **D-01g** (hosting), **D-01h** (the tutorial's audio) and **D-03h** (the Home
> progress read) — and the fourth, the cold-start **measurement contract**, is a named subsection
> of §6.5 rather than a decision, because it is a gate procedure and not a design choice. Every
> decision they amend carries a one-line pointer (D-01a, D-01f, D-03d, D-03g).
>
> Consequences that follow from the rulings but were not themselves proposed as a diff — the
> tutorial's negative contract in §5.1, the tutorial's audio decision, and the §14 criteria the
> new boards and the new gate make wrong or unverifiable — are applied here too.
>
> **Second revision — 2026-09-21 (audio).** `.planning/reports/phase-15/qa-engineer-tutorial-audio.md`
> — the qa-engineer, standing in as the owner of tests and acceptance because no sound-designer is
> staffed for phase 15 (the recusal is recorded in that report) — reviewed **D-01h** and returned
> **FAIL**. The ruling is folded in here as **D-01i**: the tutorial is *not* silent end to end. A
> completed tutorial jump plays the game's own jump pair, because
> `.planning/REQUIREMENTS.md:38` (SOUND-01, marked Complete) says the player hears a jump sound on
> **every** peg jump and D-01h narrowed that requirement without amending it or recording the
> narrowing anywhere. Silence is upheld for everything else on the board and for the completion
> star, and the gap D-01h left — the completion CTA's and `Skip`'s press cues — is now ruled
> explicitly. The edits reach D-01h (pointer), **D-01i** (new), §4.1, §4.4, §5, §5.1 (clarified,
> not relaxed), §9, §13, §14 (7c replaced, 7d/7e new) and §16, which gains its first SOUND-nn
> rows so this class of contradiction is visible next time.
>
> **Correction pass — 2026-09-21 (measurement identity, orphaned strings).** Two items the
> developer's planning pass returned (`.planning/reports/phase-15/android-game-dev-plan.md`, Open
> issues 2–3), both corrections of fact rather than design changes; no decision is reopened.
> **(1)** §6.5 now states the **launch identity** the two cold-start arms are started with. The
> `applicationId` is `com.erman.pegsolitarie` (`app/build.gradle.kts:279`, "-arie") while the code
> `namespace` is `com.app.pegsolitaire` (`:263`, "-aire") — deliberately different strings since
> the v1.1 store decision — so the launch component is neither one alone. An `am start -W` against
> the wrong one prints an error instead of a `TotalTime`, which means criterion 15e would have
> produced **no measurement at all** rather than a failing one. This document never printed a
> package id itself; the wrong id is in `tech-lead-architecture.md:360-362`, another role's report
> and not this role's to edit, so the correction here is to put the right identity where the
> procedure lives and to flag the report back to the producer. **(2)** §8's Deleted list now rules
> on `onboarding_step_1/2/3`, orphaned by D-02: they are deleted, verified to have no other caller.
> The new-strings table is untouched at thirteen rows, and no count in this document states a
> different number.
>
> **Third revision — 2026-09-21 (tutorial tap semantics; one false reason retired).** Two items the
> developer's S1–S4 build surfaced (`.planning/reports/phase-15/android-game-dev-s1-s4.md`,
> Deviation 3 and its "Noted tension"). **(1)** §4.1 now rules an interaction this document had
> never specified — a tap on a **second** peg while one is already selected — and rules it to
> **match the gameplay board**: the held peg stays held and the tap is non-progressing, because the
> tutorial's premise is fidelity to the real mechanic and a tap rule *is* the mechanic. §4.4's
> escalation gains the one clause that makes that ruling actionable (it releases a held peg that
> cannot move), classes 1 and 2 are widened to say so, and §14 gains criterion **3a** plus a clause
> in criterion 3. **This differs from what S4 built**, which re-selects the second peg; the change
> is the developer's to make and the producer's to route. **(2)** D-01i's stated reason for the
> silent `Hint` cue was **factually wrong** — it rested on "`hintSource` never transitions", and
> §4.4's escalation transitions exactly that field by design
> (`TutorialViewModel.kt:270-279`). **The audio ruling itself is untouched and still tested**; only
> the reason is replaced with the true one (no `Hint` cue effect is hosted on the screen at all).
> Checked while there: the false premise appears in exactly one sentence in this document, and
> D-01b's own "`showHints` is forced on" (§3, §4.1) is true and makes no claim about `hintSource`.
> No other decision is reopened; §5.1 and the audio ruling's substance are not touched.
>
> **Fourth revision — 2026-09-28 (Home's vertical slack; the split semantics nodes).** Two rulings
> that **postdate the build now under audit** and change nothing a reviewer is currently judging.
> `.planning/reports/phase-15/android-game-dev-s5-s8.md` reports that §4.2's weighted spacer could
> not be built as written — `weight` does not distribute inside a `verticalScroll` column, which is
> correct — and that the fixed `Spacer` which went in instead (`HomeScreen.kt:311-313`) leaves the
> mode tiles at roughly **36–54% of screen height with ~45% dead space below them** on a device.
> That is the founder's original complaint reappearing at the bottom of the screen instead of in
> the middle of it, so the mechanism is ruled here: **D-03i** (§3, specified in **§4.2.2**) replaces
> the fixed spacer with two weighted spacers that work, and rules what yields on a short screen so
> that criteria 15 and 15a can both hold at once. Criterion 15 and §4.2's "Thumb zone" paragraph are
> **left exactly as they read** — they are what the audited build was built against — and each gains
> a one-line pointer; the new, both-sizes criteria are **15g–15j** and they apply to the follow-up
> change, not to the build. §6.1 gains two tokens and §12 a correction of fact about the same
> modifier chain.
>
> The second ruling answers the accessibility finding in that same report (the clickable node and
> the labelled node being separate in a `uiautomator` dump): **§11 gains a dated note and §14
> criterion 26a the bright line.** The verdict is that this is **not** a defect and needs **no code
> change** — the unmerged tree is not the tree the contract is written against — while the TalkBack
> pass criterion 26 always required stays owed.
>
> **Fifth revision — 2026-09-28 (the cold-start gate, re-specified; the ≤ 2 s bar's unmeasured
> state).** `.planning/reports/phase-15/tech-lead-coldstart-gate.md` — the tech-lead's ruling on
> **criterion 15e**, a gate it specified itself and has now corrected on evidence. The delta half of
> 15e was **ill-posed, not merely unmeasured**: its ≤ 50 ms threshold was Phase 6's **46 ms
> within-arm range** (nine samples of one build, 603–649 ms around a 633 ms median,
> `06-BUILD-EVIDENCE.md:319-320`) promoted to a threshold on a **between-arm median difference**,
> which is a different and noisier statistic. Two independent null experiments — the same build
> measured against itself, where the true delta is zero — returned **57 ms**
> (`15-BUILD-EVIDENCE.md:52-53`) and **59 ms** (`qa-engineer.md:162-163`), so the instrument's floor
> sits near 60 ms and a 50 ms gate cannot tell a regression from noise. Both nulls ran on the
> **same AVD**, so the **1.83×** gap between their absolute medians (957 ms against 1832 ms) is host
> load and not device class — which is what kills the absolute reading on an emulator and leaves the
> relative one recoverable.
>
> Two edits follow, and they are of different kinds. The **procedure** is replaced in §6.5 by a
> **second edition** — an interleaved A/B delta with a **same-sitting null control**, a threshold
> derived from that control (`max(2 × floor, 100 ms)`) and a **VOID** verdict above a 150 ms floor —
> filed as a named subsection and not as a decision, on the same reasoning the first measurement
> contract was (a gate procedure is not a design choice). The **ownership of the ≤ 950 ms absolute**
> is a contract change, so it is filed as a decision, **D-03j**: the absolute is not phase 15's gate,
> it is a tracked target owned by **PLAT-08 / phase 14** where Phase 6 explicitly put it
> (`06-BUILD-EVIDENCE.md:308-309`), and the bar of record is rules/studio.md's **under 2 seconds on
> a named mid-range physical phone**. §6.5 now also records, as a **named gap** rather than an
> implication, that **that bar has never been measured on a physical phone** — every cold-start
> number in this repo is from an emulator — with the tech-lead's recommendation and its decided
> default. Criterion **15e is replaced**, its first edition kept verbatim beneath it because two
> reviewers returned verdicts against that wording and a reader who meets those reviews must be able
> to find what they were refusing; criterion **15f is not deferred with it**; D-03g, §1's D-03 row
> and §16 gain pointers. Nothing else is reopened: D-03i / §4.2.2, the audio rulings and §5.1 are
> untouched, and no bar is lowered — a noisy host now buys a re-run instead of a pass.
>
> Out of scope here and **not** absorbed into this document: rules/audio.md makes
> `docs/audio-spec.md` the cue list of record and that file has never existed on any branch. The
> gap predates phase 15, belongs to the sound-designer, and the producer is tracking it
> separately. This spec names cues per screen; it does not stand in for that file.
>
> **Sixth revision — 2026-09-28 (the compact band set does not fit a 360×640dp phone; criterion
> 15h is retired because it is false).** `.planning/reports/phase-15/code-auditor-s9.md` **MB-1** —
> the reviewer's re-audit of the build that landed §4.2.2. Two findings, and both are this role's to
> answer. **(1)** §4.2.2's ≈544dp compact total is **≈156dp light**, because it sums band
> *minimums* while at 360dp width the bands stand well above their floors: measured, ribbon
> **116dp**, each Continue chip **108dp**, the mode tiles **108** and **116dp** — **≈700dp** of
> content (≈684dp to the last band's bottom edge). **(2)** On the test's own zero-inset
> `w360dp-h640dp` window the Challenge tile sits at `568..640` unscrolled and `524..640` once
> scrolled into view — **44dp below the fold** — where criterion 15h required both tiles visible
> *without* scrolling. §4.2.2 had itself named this the "stop and report the measured numbers"
> branch (`:1077-1081`) and reserved the next yield to this role; the numbers have arrived, so it is
> ruled here rather than deferred again.
>
> The ruling is **D-03k** (§3, specified in **§4.2.3**) and it has four parts. §4.2.2's arithmetic
> is **corrected additively** — the first-edition table is left legible with a pointer and a
> corrected, **wrapped-height** table sits beside it, together with the usable-height figure this
> document now designs against: **568dp**, not 640. The compact **threshold becomes a function of
> width as well as height**, because the ≈156dp error was wrap at narrow width and not height at
> all. **One new thing yields and it costs no readout** — the ribbon pill's `contentDescription =
> null` glyph and its stacked third line, which together are ~44dp of the 116dp. And then, on the
> corrected arithmetic, **criterion 15h is retired because it is false**: after every available
> yield the compact set still stands **72dp** over a real 360×640dp phone, and all 72dp sit in
> bands that are readouts, targets or routes. Its replacement, **15k**, says what is true — the
> hero row, the ribbon, both Continue chips and the *first* mode tile complete inside the first
> viewport, the second tile breaking the fold, the whole column one bounded scroll. **An amended
> criterion that tells the truth is worth more than a criterion that passes.**
>
> **Which build the new criteria apply to — stated once, because two reviewers have returned
> verdicts against earlier wordings.** **15k, 15l and 15m apply only to the slice that lands
> D-03k**, the slice *after* the build audited on 2026-09-28. They do **not** retro-certify that
> build. 15h stays legible beneath its replacement precisely because it is the criterion the S9
> build was measured against, and S9's verdict against it is **FAIL**. **Retiring 15h does not
> clear MB-1**: an assertion incapable of failing (`HomeScreenTest.kt:300`) is a defect on its own
> terms and is not made acceptable by a criterion changing underneath it. That assertion is still
> the developer's to fix, now against 15k and **with insets supplied** — which matters more than
> before, because on the corrected numbers the compact column lands within a few dp of the 640dp
> zero-inset window, so a zero-inset test would read "fits" on the very window where the device
> does not. Nothing else is reopened: D-03i's mechanism — the `BoxWithConstraints`, the modifier
> order, the two weighted `Spacer`s and clause 4's ban on a weighted content band — is **untouched
> and still binds** (the auditor broke clause 4's shape and watched the guard catch it), and
> criteria 15g, 15i and 15j stand unamended. No readout, no control, no touch target and no band
> is removed by this ruling, and none of §4.2.1's four rejected simplifications is reached for.
>
> **Seventh revision — 2026-09-28 — FOUNDER-FEEDBACK AMENDMENT (Home hero, dock state language,
> status strip).** The founder reviewed the phase-15 build on an emulator and returned three notes,
> recorded with their scoping answer as **Q-004** in `.planning/questions/FOUNDER.md`; all three are
> this role's. **(1) Home** — "I don't really like this screen", scoped by Q-004 to the **empty
> middle gap**, the **peg-cross logo** and **how the title sits**; the stats ribbon, the mode cards
> and the Candy Clay palette and style were *not* selected and are kept. **(2) The dock** — "I like
> the in-game icons, but I can't really say if they are on or off." **(3) The status strip** —
> "elapsed time and the title seem too small. They could be reworked." Three decisions, one per
> note, each with its own specification subsection and criteria a test can check:
>
> - **D-03l** (§4.2.4; criteria **15n–15r**) — the hero becomes **one row**, the gradient title
>   start-aligned beside the Settings gear, on every window; the peg-cross logo is **withdrawn**
>   (Q-901's answer is superseded by Q-004); and the slack that D-03i parked as a blank band between
>   the ribbon and the Continue chips moves into the hero as a **stage** — a static clay board
>   showing a game in progress, sized by the slack itself and simply not drawn when there is no
>   room. The ribbon therefore sits directly on the Continue chips; that change of **position** is
>   the only consequence for any band the founder asked to keep. Art direction between strong
>   alternatives is queued as **Q-005**, non-blocking, with this as the built default.
> - **D-04e** (§4.4.1; criteria **21a–21g**) — **one look, one meaning.** D-04c's loud/quiet split
>   drew Undo and Hint filled and Restart outlined, which is the on/off vocabulary, so the founder
>   saw two controls "on" and two "off"; and the fill it relied on is **≈1.04:1** against the dock
>   slab in light theme — a state channel nobody can see. Undo, Hint and Restart now share one
>   "button" look and change only between available, working and unavailable; Show-moves, the only
>   toggle, is the only control with an on/off look — a solid `primary` disc with a check mark when
>   on, a ring with a slashed glyph when off. Every pair of looks differs in at least two channels
>   that are not hue, and TalkBack gets the same fix: Show-moves becomes a real switch with one
>   stable name.
> - **D-05d** (§4.3.1; criteria **18a–18e**) — the title moves from `titleMedium` (16sp) to
>   **`titleLarge` (22sp)** and the timer to **`headlineSmall` (24sp)** in a taller clay pill; and
>   the strip and the dock apply the system-bar insets **once**. Today both are applied twice —
>   visible in the founder's screenshot as an empty band the height of the status bar above the
>   strip — which on a 360×640dp phone takes 72dp from the board's region and shrinks the board from
>   328dp to 304dp.
>
> **Not reopened:** every §5.1 prohibition, the tutorial and its audio rulings, D-03f's three
> oracles and §4.2.1's four rejections, D-03i's mechanism except the three clauses D-03l amends
> (stated in §4.2.4), D-03k's narrow-class arithmetic and criterion 15k, the dock's order, sizes,
> glyphs (bar one slashed variant for Show-moves Off) and cues, and the earlier-phase ad-allowance
> ruling (`GameUiState.kt:131-135`), which
> D-04e applies rather than relaxes. This amendment adds **no string** (it deletes one,
> `show_moves_off`), **no colour**, **no dependency** and **no third-party asset**; it adds one
> in-house vector and seven tokens, changes one token's value and retires three.
>
> **Eighth revision — 2026-09-29 — AMENDMENT (criterion 15p's gap reading; the hero row loses its
> floor).** `.planning/reports/15-founder-feedback/qa-engineer.md` **E-1**, from the qa-engineer's
> device pass of the D-03l slice. Criterion 15p part 1 passes or fails depending on what "hero row"
> means. Measured from the row's 72dp box, the hero→art gap is ≈16dp. Measured from the gear, the
> row's lowest visible edge, it is **27.4dp** in every chip state. The 72dp floor around a 48dp gear
> makes the two readings disagree by construction. Ruled as **D-03m** (§3, specified in the last
> subsection of **§4.2.4**): **the visible reading governs**, with "visible element" defined once, and
> **`home_hero_row_min_height` is retired**, so the row is exactly as tall as its tallest child and the
> gap is the ordinary band gap. The ruling is checked in **Fredoka and Nunito** as well as in the
> Roboto that every device reading so far was taken in (`qa-engineer-loop2.md` F-A, Q-013). The title
> row's width and height move by 0.25dp or less. Pointers go to D-03l, §4.2.3, §4.2.4, §6.1, §13, §14
> (15p) and §16. **Not reopened:** 15p's words, bounds and ≥ 120dp art, every other criterion, §4.2.2's
> mechanism, the stage, the gear and the compact thresholds. No string, no colour, no dependency; one
> token retired.
>
> **Ninth revision — 2026-09-29 — AMENDMENT (the tech-lead's font ruling and 15e arm swap; a check of
> D-03m by an agent that did not write it).** Two inputs, and neither reopens a decision.
> **(1)** `.planning/reports/15-founder-feedback/tech-lead-fonts.md` (Q-013, and the qa-engineer's
> loop-2 E-1). Fredoka and Nunito now ship as five **bundled** static OFL files in `res/font` and load
> blocking on first use. The 15e sitting swaps its arms at block boundaries, because both arms share
> one `applicationId`. The tech-lead's wording is applied as given:
> - §6.5 gains the one named exception to Home's first-frame hard rules, a cost row and a degradation
>   note;
> - §6.5 preconditions 1, 4 and 7 are amended: the `Pixel_9a` stays required and a `Pixel_9` sitting is
>   VOID; the host-quiet limit becomes half the host's logical cores; "kept installed" was an error;
> - criterion 15e follows them, and it and §6.5's "Which builds" table record that the sitting runs
>   once, on the branch head that carries both D-03m and the fonts;
> - §6.3 records that Fredoka has no `tnum`;
> - §9's font row and its opening paragraph record the bundled asset and its `THIRD_PARTY.md` rows;
> - criterion 18d names the discriminators.
>
> D-03g gains a pointer, since its rule gains the exception, and a note reconciles precondition 1's
> `:app:installDebug` with preconditions 3 and 7. The tech-lead's companion text for
> `docs/ui/DESIGN-SYSTEM.md` §Typography is **not** applied in this revision; until it lands, that
> file's "downloadable fonts" sentence is superseded by §6.3 and §9 here.
> **(2)** The eighth revision (D-03m) was checked by a ux-designer agent that did not write it. The
> ruling, its arithmetic and its real-face figures hold. Seven passages still stated the retired 72dp
> row or the ≈72dp overflow as current. They gain six dated pointers: D-03k, §4.2.2 (one pointer for
> two passages), §4.2.4, §12, and criteria 15h and 15k. §15 records Q-005's answer. D-03m's real-face check is re-anchored on
> the bundled static file, because the font ruling deletes the test fixture it cites.
> **Not reopened:** every decision, D-03m's ruling, 15e's verdict rule, and every bar. No string,
> colour or token changes. One third-party asset enters the artifact, the two font families, under
> OFL-1.1.
>
> **Tenth revision — 2026-10-21 — AMENDMENT (the phase-15 close folds; Continue-chip arrival; the pill
> at 200%; the timer's spoken form; the system bars).** Written on Opus under the founder's Q-019,
> not on the role's pinned Fable. Two kinds of input.
> **(1) The tech-lead's text, folded as given** — from `.planning/reports/15-founder-feedback/tech-lead-signoff.md`
> ("Text for other roles' files") and `tech-lead-signoff-close.md` ("Docs edits still owed"):
> - §6.5 precondition 1 names the 15e instrument, **`Pixel_9a_bench`** (2026-10-18), and the D-1 route
>   to it, `avdmanager` or a copy of `Pixel_9a.avd`'s files, closed by the recorded diff (2026-10-21);
> - precondition 9, `/data` free ≥ 4 GB at both ends, is new; precondition 5 gains the tech-lead's
>   `[status=verify]` reading;
> - "Which builds" gains the 2026-10-18 pointer, under which arm A is the branch's merge-base with
>   `develop`, with phase 15's own sitting recorded as PASS; the protocol and "Where the numbers go"
>   gain pointers to it;
> - criterion 15e makes its two replacements, with the old words in its amendment note;
> - criterion 15k part 2 records that 360×640dp overflows 0dp on real metrics, and the trailing-gap
>   reading the code-auditor ruled and the tech-lead adopted;
> - `docs/ui/DESIGN-SYSTEM.md` §Typography takes the font ruling's text, its old sentence kept.
>
> **(2) This role's rulings on the items routed to it**, each a decision in §3 with its own subsection
> and criteria:
> - **D-03n** (§4.2.5; criteria **15s–15v**) — on the qa-engineer's F-1: when a Continue chip arrives
>   ≈0.6s after first paint, **nothing a player can tap moves**. The footer becomes one fifth of the
>   no-chip slack, fixed for the window; the stage gives up the chips' room; the ribbon rides on the
>   stage's edge; the room opens smoothly; and the first frame draws the stage and the tile art whole.
> - **D-03o** (§4.2.6; criterion **15w**) — on O-2 and `ux-designer-15p.md` R-3 (not §7's R-3): **no
>   word in a progress pill is ever broken**; once a label no longer fits in half its pill, all three
>   pills stack, decided by the labels and, but for one named edge case, never by the values.
> - **D-05e** (§4.3.2; criteria **18f**, **18g**, **26b**) — on O-A/A-8/I-2: **the timer speaks its
>   value**, "Elapsed time, 5 minutes, 12 seconds", never on its own; and criterion 26's spoken order is
>   written out per screen.
> - **D-05f** (§6.2; criterion **29**) — on 15e O-1: a dark three-button navigation bar in dark mode
>   **is wanted**; the system bars take the theme.
>
> All four are built on `bugfix/home-chip-arrival`, which sits its own 15e. **Not reopened:** every
> other decision, every bar, 15e's verdict rule, the band order, the oracles, the tokens and the
> palette. Two strings and two plurals are added, with one more string only if D-05e's fallback is
> taken; one scoped ratio replaces two weights; no token, colour, dependency or asset changes.
>
> *(Same day, after a fresh checker's review, `.planning/reports/ux-spec-folds/ux-designer-check.md`.)*
> Criterion 15g's "the screen's unused space" is ruled to be the no-chip slack S₀ (§4.2.5), so the
> D-03n footer meets it in every state; 15g and §13 carry it. Criterion 26b no longer counts the board
> as a TalkBack stop, which it is not on this branch, and §11 records the board's TalkBack gap against
> the first edition's order, its release scope routed to the game-designer (and the founder). 15v waits
> for the branch's own 15e sitting on the bench. The start-path list reads every
> `res/values*/themes.xml`. Superseded 3 : 1 and "the label wraps" passages gain pointers, as do the two
> LEGACY "scrolls" headlines, and criterion 29 names its qualifiers.
>
> *(Same day, after a second fresh check, `.planning/reports/ux-spec-folds/ux-designer-check-loop2.md`.)*
> Criterion 15v reads position with animations off, and with them on only once §10's entrance cascade
> has settled; a chip that lands mid-cascade restarts no entrance. A chip arriving after first paint
> takes no stagger delay. The §11 gap no longer rules the board's release: it is routed, pending the
> game-designer's ruling, and criterion 26 gains a pointer for the tutorial board. 15g's pointer, §12's
> tall-phone range, two §13 rows and the revision index's "R-3" are corrected.

---

## 1. The founder's five asks, and what each became

The founder's words, verbatim:

> "make the tutorial more interactive. remove the info button that starts the tutorial. Homepage
> could be fancier it looks empty with just 2 buttons. Also during the game, redesign the icons
> on the top. remove toolbar from the game"

| Ask | Decision | Verdict |
|-----|----------|---------|
| Make the tutorial more interactive | **D-01** | The tutorial becomes three micro-puzzles the player actually plays on a live board. No step is advanced by a Next button. |
| Remove the info button that starts the tutorial | **D-02** | `HomeScreen.kt:348-354` is deleted. The replay path moves to a Settings › General row; first-run auto-start is unchanged. |
| Homepage looks empty with just 2 buttons | **D-03** | Home gains a hero block, a three-pill progress ribbon and board art on its tiles. The two mode tiles stay primary and stay in the thumb zone. **Bounded by two hard constraints: three existing readouts are device-test oracles that must stay visible on first paint (D-03f), and Home is on the measured ≤ 950 ms cold-start gate (D-03g).** *(The ≤ 950 ms figure is reattributed to PLAT-08 / phase 14 by **D-03j**, 2026-09-28; the sentence is left as it read.)* *(And by **D-03k**, 2026-09-28: "visible on first paint" is exact for the Continue chips on every window and for all three oracles at 400dp of width or more, but on a 360dp-wide phone the Challenge tile's level chip is across the fold — composed at full size, one plain scroll of ≤ 120dp away, with the ribbon's Level pill as its above-the-fold witness. §4.2.3 has the arithmetic that forces it; criterion 15k is the check; nothing is ever collapsed, truncated or behind a tap.)* |
| Redesign the in-game icons on the top | **D-04** | The five controls are re-glyphed, re-grouped and re-homed. Two real accessibility defects are fixed in the process. |
| Remove the toolbar from the game | **D-05** | The `TopAppBar` at `GameScreen.kt:502` is deleted. It is the **same surface** as the "icons on the top", so D-04 and D-05 are one change — see D-05 in §3. |

**The founder's three notes on the built screens, 2026-09-28 (Q-004) — the founder-feedback
amendment.** The table above is the record of the original asks and is left as it reads.

| Note (verbatim) | Decision | Verdict |
|-----------------|----------|---------|
| Home: "I don't really like this screen" — scoped to the empty middle gap, the peg-cross logo and how the title sits | **D-03l** | The logo goes; the title sits start-aligned in one row with the gear; the blank band becomes a stage — a static board showing a game in progress, sized by the slack. Ribbon, cards and palette kept. §4.2.4, Q-005. |
| "I like the in-game icons, but I can't really say if they are on or off" | **D-04e** | Icons kept. Actions get one look with no on/off; the one toggle gets the only on/off look, marked by shape (check mark / slash), fill and ring, and speaks as a switch. §4.4.1. |
| "Elapsed time and the title seem too small. They could be reworked." | **D-05d** | Title `titleLarge` 22sp, timer `headlineSmall` 24sp in a 40dp clay pill; the double system-bar inset removed, so the board loses nothing. §4.3.1. |

---

## 2. Screen inventory

| # | Screen | Status this refresh | Files |
|---|--------|---------------------|-------|
| 1 | **Tutorial** | **Rewritten.** Was a static illustrated sheet; becomes an interactive coached micro-board flow. Promoted from "a scrim+sheet over Home" to a screen in its own right — and, since D-01g, to a nav destination of its own. | `ui/onboarding/OnboardingOverlay.kt` (replaced), new `ui/onboarding/Tutorial*.kt` incl. `TutorialViewModel`, a `Tutorial` entry in `Routes.kt` + `AppNavHost.kt`, and `object TutorialBoards` in `:domain` |
| 2 | **Home** | **Recomposed.** Top bar deleted; hero, progress ribbon and tile art added; mode tiles unchanged in behaviour. | `ui/home/HomeScreen.kt`, `ui/home/ModeGlyphs.kt` |
| 3 | **Gameplay** | **Chrome replaced.** `TopAppBar` deleted; a status strip and an action dock take its place. Board, rules, animation, dialogs, banner, snackbar all unchanged. One **mechanical** extraction only, no behaviour change: the private `JumpSound` effect (`GameScreen.kt:320-330`) is lifted to a shared `internal` composable so the tutorial can call the same one (D-01i) — gameplay's own audio is byte-for-byte the same cue, level and pitch ladder it plays today. | `ui/game/GameScreen.kt`, `ui/game/{Hint,HintToggle,Undo,Restart}Control.kt` |
| 4 | **Settings** | **One row added** (How to play) + one stale supporting line fixed. | `ui/settings/SettingsScreen.kt` |
| 5 | Classic catalog | **Unchanged.** | — |
| 6 | Results | **Unchanged.** | — |

**Deliberately unchanged:** every rule, every **shipped** board footprint, the scoring/star model,
the jump/removal animation, the rescue banner, the ad-offer dialog, the restart confirm dialog,
the Play Games opt-in prompt, and the profile-swap dialog. This refresh is a presentation change
everywhere except one place: the tutorial gains three **new** teaching miniatures
(`object TutorialBoards`, §4.1), which live outside `Boards.CLASSIC_SPECS` and change no existing
board, no catalogue entry and no number the player has already earned.

*(2026-09-28 — founder-feedback amendment.)* No screen is added or removed. **Home**'s hero is
re-seated as one row and its slack becomes a stage (D-03l, §4.2.4); **Gameplay**'s strip is
re-sized with the insets applied once (D-05d, §4.3.1) and its dock gains a state language (D-04e,
§4.4.1). Files touched in addition to the rows above: `ui/home/HomeHero.kt` (the row; `FullHero`,
`CompactHero` and `HomeHeroArt` removed), a new stage composable beside it, `ui/game/GameStatusStrip.kt`,
`ui/game/GameActionDock.kt`, `ui/game/{Undo,Hint,HintToggle,Restart}Control.kt`, a new in-house
`ShowMovesOffIcon`, `res/values/{dimens,strings}.xml`, and `ui/game/GameScreen.kt` if the developer
chooses to keep the insets in the strip and dock and set the `Scaffold`'s content insets to none
(§4.3.1).

---

## 3. Decisions

House style per `.planning/phases/*/[0-9]*-CONTEXT.md`: `- **D-nn:** <ruling> — <rationale>.`
Decisions are never rewritten in place; a later ruling is filed as `D-nna` with its date, and the
decision it amends gains a one-line pointer to it.

**One warning about ids.** `D-01`…`D-05` and their letters belong to *this* document. Where a
section cites a decision id that is **not** in the list below — the audio `D-09`, the win
celebration's `D-14`, the info button's `D-15`, and the ad-allowance `D-05` the code records at
`GameUiState.kt:131-135` — it is naming an **earlier phase's** ruling, not one of these. The
ad-allowance id is the one genuine collision, and §4.4 qualifies it where it appears.

### Tutorial (ONB-01, proposed UX-01)

- **D-01:** The tutorial teaches by **making the player perform the jump**, not by describing it.
  Three micro-puzzles run in sequence on a live, tappable board; a step advances **only when the
  player completes it**, never on a Next button. — rules/puzzle-design.md requires the first
  puzzles to "teach without text, the way a level layout teaches", and the current sheet is the
  exact anti-pattern it names: three static `BoardPreview` illustrations plus three sentences of
  prose behind one "Got it" button (`OnboardingOverlay.kt:96-131`, `:179-184`). A player can
  dismiss it today without ever having jumped a peg.

- **D-01a:** The tutorial board is the **real `BoardCanvas` in `interactive = true` mode**, driven
  by a real `GameUiState` and the real rules engine — not a bespoke illustration. — `BoardCanvas`
  already takes a whole `GameUiState` and emits bounds-checked taps
  (`BoardCanvas.kt:87-93`, `:205-212`), so the tutorial inherits selection rings, the breathing
  pulse, legal-destination dots, the jump slide and the removal fade for free, and what the player
  learns is literally the mechanic they will use. The current sheet cannot do this: it routes
  through `BoardPreview`, which hard-codes `interactive = false` (`BoardPreview.kt:86`) and
  overrides its caller's size with `catalog_preview_size` 96dp (`BoardPreview.kt:79`) — which is
  why the `onboarding_illustration_size` 160dp token the overlay passes at
  `OnboardingOverlay.kt:144` has never had any effect on screen.
  *(Clarified 2026-09-21 — see D-01g and §4.1 "The board the tutorial drives": the `GameUiState`
  this ruling names is a **UI model assembled by `TutorialViewModel`**. It is not licence to host
  `GameScreen` or to reach for `GameViewModel`, both of which §5.1 forbids.)*

- **D-01b:** Steps are **T1 one jump → T2 the other axis → T3 clear it**, in that order, each with
  exactly one lesson. `showHints` is forced **on** inside the tutorial regardless of the persisted
  setting, so legal destinations are always visible. — One lesson per step is what makes a step
  completable without reading; forcing the dots on removes the only way a step could become a
  guessing game, which rules/puzzle-design.md forbids ("all needed information visible").

- **D-01c:** **A wrong tap is never a failure.** There is no error state, no red, no modal, no
  retry count, no score. The three wrong-tap classes are answered by escalating *assistance*, not
  by correction — see §4.4. — rules/puzzle-design.md: "first failure impossible before puzzle 5".
  The tutorial is puzzle 0; it must not be losable.

- **D-01d:** **Skip is available on every step** as a labelled text action, and exits immediately.
  System back does exactly what Skip does. Both persist `markSeen()`. — A tutorial the player
  cannot leave is worse than no tutorial. Routing back to the same answer as Skip follows the
  precedent already set for `ProfileSwapDialog`, whose back press is documented as one of three
  spellings of a single answer (`HomeScreen.kt:108-111`). The alternative — back exits without
  marking seen — re-forces the tutorial on the next launch for a player who deliberately left,
  which is the worse of the two failures.

- **D-01e:** There is **no cross-process resume.** `markSeen()` is written on completion or on an
  explicit Skip/back only; a process death mid-tutorial leaves the flag false, so the tutorial
  offers itself again from T1. Within a session, rotation keeps the current step. — Three micro-
  steps are under a minute of play; a persisted step cursor would cost a DataStore key, a
  migration and a test matrix to save a player fifteen seconds.

- **D-01f:** The tutorial is a **screen, not a dialog over another screen**. It owns the full
  viewport, its own state holder and its own back handling. — Screen inventory is this role's
  call. It now has multi-step state, a live board and two entry points; hosting that inside
  `HomeScreen`'s composition (as `OnboardingOverlay.kt` is hosted at `HomeScreen.kt:174-176`)
  would put a playable board inside the screen whose own job is mode selection. **The hosting
  mechanism — a `@Serializable` nav destination versus a full-screen composable switched above
  the `NavHost` — is the tech-lead's call, not mine**; §4.1 specifies what the player sees either
  way, and §7 lists the one sequencing invariant the mechanism must preserve.
  *(Answered 2026-09-21 — see D-01g. The player-facing contract in §4.1 is unchanged by the
  answer; D-01a's board contract is clarified there.)*

- **D-01g** *(2026-09-21 — tech-lead ruling, `tech-lead-architecture.md` §1/§1a, folded in)*: the
  tutorial is a **`@Serializable` nav destination** (`composable<Tutorial>` in `AppNavHost`) with
  a `@HiltViewModel` **`TutorialViewModel`** scoped to that destination; step state lives in a
  plain `MutableStateFlow` and **not** in `SavedStateHandle`. Three player-visible consequences
  are now contract rather than mechanism, and §4.1 and §14 state them:
  1. **A replay from Settings always starts at T1.** A destination-scoped ViewModel is cleared on
     pop, so a player who skipped at T2 and later taps Settings › How to play gets a fresh T1 —
     the bug an Activity-scoped holder would have shipped.
  2. **The exit persists before it leaves.** `markSeen()` is awaited — on the application scope,
     so a cancelled screen cannot drop it — and only then does the screen pop. Skip, system back
     and the completion CTA are one function with three callers, latched so a double exit is a
     no-op. Without this, Home re-pushes the tutorial the moment it recomposes, and the player is
     in a loop; that loop is a UX failure, which is why it is restated here and not only in §7.
  3. **The tutorial never runs the game's own screen or state holder.** It hosts `BoardCanvas`
     and never `GameScreen`; its state holder is `TutorialViewModel` and never `GameViewModel`.
     This is a **hard contract**, stated as a prohibition in §5.1 — not a preference — because
     `GameViewModel`'s `Won` path clears the mode's resume snapshot and advances the Challenge
     ladder (`GameViewModel.kt:956-975`: `clearSnapshot()` at `:963`,
     `challengeProgress.advancePast(...)` at `:973`). Every tutorial step ends on one peg, so a
     tutorial routed through `GameViewModel` would fire that path **three times per run** and
     destroy two of the three cloud-save device oracles §4.2.1 exists to protect.
  — Hosting is the tech-lead's authority and this spec routed it there; what comes back into this
  document is only what the player can see or lose.

- **D-01h** *(2026-09-21 — UX decision)* — ⚠️ **AMENDED THE SAME DAY: read D-01i first.** Its
  opening clause is **no longer true**: the jump cue *does* fire in the tutorial. Everything else
  this decision silenced is **upheld**, and the completion CTA it left unruled is ruled in D-01i.
  The text below is kept verbatim as the record of what was decided and why it was overturned; it
  is not the contract. — **the tutorial is silent, end to end.** No cue at all
  plays anywhere on the tutorial screen — not `Select`, `Deselect`, `Jump`, `PegRemove`,
  `InvalidTap` or `Undo` on the board, and not `StarPop` or `Win` on the completion star — and no
  cue is added to `BoardCanvas`. The only sound in the whole tutorial flow is the
  `SoundCue.ButtonForward` that the **Settings row** plays on the way in (§4.5), which belongs to
  Settings and not to this screen. — Every sound cue in the game is played by a private effect in `GameScreen`
  (`GameScreen.kt:327-428`); `BoardCanvas` contains no `SoundCue` reference at all, so a tutorial
  hosting the canvas is silent unless a second screen re-implements those effects. Three reasons
  to take the silence deliberately rather than inherit it: rules/audio.md says nothing plays that
  the spec does not name, so silence is compliant; every tutorial event already carries visual
  feedback (the selection ring, the legal-destination dots, the jump slide, the removal fade), so
  the "sound is never the only feedback" rule is satisfied with room to spare; and an `InvalidTap`
  cue would contradict D-01c, which says a wrong tap is never a failure — a rejection sound is the
  one channel that would make it feel like one. The first sound the player hears is therefore the
  first jump of their first **real** board, which is the right moment for it. §4.4's tutorial
  wrong-tap rows and §9's audio row are amended to match. Revisiting this is the sound-designer's
  to propose and mine to rule on; it is not a founder question.

- **D-01i** *(2026-09-21 — UX ruling, amending D-01h; from the qa-engineer's D-01h review,
  standing in for the unstaffed sound-designer — `.planning/reports/phase-15/qa-engineer-tutorial-audio.md`)*:
  **the tutorial is not silent end to end. A completed jump sounds; nothing else on the board
  does; and the two ways out speak the app's own press vocabulary.**

  | Event on the tutorial screen | Cue | Ruling |
  |---|---|---|
  | A peg **completes** a jump | `SoundCue.Jump`, pitched by `playJump(runIndex)`, with `SoundCue.PegRemove` layered `SoundParams.REMOVAL_LAYER_DELAY_MS` beneath it — the same pair, the same levels, the same pitch ladder as the real game | **Fires** |
  | A peg is selected / put back down | `Select`, `Deselect` | **Silent** — D-01h upheld |
  | A peg with no legal jump is tapped; an illegal destination is tapped | `InvalidTap` | **Silent** — D-01h upheld |
  | Undo | `Undo` | **Silent** — there is no Undo in the tutorial at all (§5) |
  | A hint is revealed | `Hint` | **Silent** — the tutorial hosts **no `Hint` cue effect at all**: `HintSound` is private to `GameScreen` (`GameScreen.kt:372-377`, called at `:162`), which §5.1 forbids the tutorial from hosting, and §5's allow-list for this screen is `Jump`, `PegRemove`, `ButtonForward`, `ButtonDismiss` and nothing else. There is also no Hint control for a player to press (§4.1 — the tutorial has no dock). *(Reason corrected 2026-09-21 — see the note below the table; the verdict is unchanged.)* |
  | The completion star, end of T3 | `StarPop`, `Win` | **Silent** — D-01h upheld; a tutorial is not a victory and D-14's two-beat celebration belongs to a real solve |
  | The completion CTA ("Got it") | `SoundCue.ButtonForward` | **Fires** — the gap D-01h left, now ruled |
  | `Skip`, on any step | `SoundCue.ButtonDismiss` | **Fires** — same ruling |
  | System back | — | **Silent**: there is no button press to sound. It is the same exit path (D-01d), not the same event. |

  **Why the `Hint` row's reason changed — corrected 2026-09-21, verdict untouched.** The row's
  first wording gave the reason as "`showHints` is forced on (D-01b), so nothing is ever *revealed*
  and `hintSource` never transitions". Both halves are false on this screen, and the second is
  false in the code. §4.4's escalation is required to make "the source ring pulse **and** the
  destination dot brighten", and §10 requires it to do that with the board's **own** highlight
  idiom rather than a new one — which is `hintSource`/`hintTarget`. The tutorial's state holder
  therefore sets both, by design (`TutorialViewModel.kt:270-279`, the update at `:276`; cleared
  again on the next jump at `:200`). The escalation is also a *reveal* in the only sense that
  matters here: it shows a move for a peg the player has **not** selected, which forced-on
  `showHints` never draws — forcing the dots on draws the destinations of the *selected* peg and
  says nothing about any other.

  **The ruling is unchanged, and it is the ruling the tests already pin**
  (`theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep` asserts `Hint`'s absence
  directly, §13). What changes is only the reason, because the silence never rested on the state at
  all — it rests on **where the cue effects live**. Five of the six board cue effects, `HintSound`
  among them, are still `private` to `GameScreen.kt` (`:310-394`); the single one lifted is the jump
  pair, called once from the tutorial (`TutorialScreen.kt:105`). A screen that hosts no `Hint`
  effect cannot play `Hint` however its state moves.

  Two consequences worth keeping, which the false reason hid. **First, the silence is one lift
  away from breaking:** `HintSound` is keyed on `hintSource` and plays on any transition to
  non-null (`GameScreen.kt:374-375`), so lifting *it* the way the jump effect was lifted would make
  the coach audible the same day — which is exactly why §13's allow-list grep and criterion 7d are
  the guards, and not this paragraph. **Second, a reason that is wrong is worse than no reason:** it
  invites the next reader to re-derive the ruling from a premise the code contradicts, find the
  contradiction, and conclude the *ruling* is stale. The developer who built S4 hit precisely that
  and flagged it (`.planning/reports/phase-15/android-game-dev-s1-s4.md`, "Noted tension").

  **Why the jump cue fires.** The decisive reason is not taste: `.planning/REQUIREMENTS.md:38`
  (SOUND-01) reads "Player hears a jump sound on **every** peg jump, pitch-varied so repeated
  jumps do not fatigue", and it is marked **Complete** (`:165`). The tutorial's jumps are real
  jumps on the real rules engine (D-01a). D-01h therefore made the tutorial the single place in
  the shipped product where a peg jump produces no jump sound — without amending SOUND-01,
  without scoping it, and without registering the narrowing anywhere, since §16 carried **no
  SOUND-nn row at all**. A UX decision may not narrow a Complete requirement invisibly. §16 now
  carries SOUND-01 and SOUND-04.

  Two design reasons agree with it. **Fidelity is the tutorial's whole premise** — D-01/D-01a
  stake the design on the real canvas and the real engine, "what the player learns is literally
  the mechanic they will use", and the tutorial already reproduces the selection ring, the
  destination dots, the jump slide and the removal fade; audio would have been the sole
  deliberate divergence, teaching for thirty seconds that a jump is a silent event and then
  contradicting that at the first real board. And **D-01c has stripped every other consequence
  from the screen** — no score, no failure, no retry count, no red, no modal — which is exactly
  why the completed jump is the tutorial's only moment of positive consequence; removing its one
  audible confirmation leaves the payoff thinner than the teaching needs.

  **What D-01h got right, and keeps.** Its `InvalidTap` reasoning is the load-bearing one and is
  upheld without reservation: a rejection sound is the one channel that would make a wrong tap
  feel like a failure, which D-01c forbids. Its other two reasons — that rules/audio.md permits
  an unnamed cue to be absent, and that every tutorial event already carries visual feedback —
  establish that silence is *permitted* and that either choice is legal. Neither is an argument
  that silence is *right*, and the decision rested on them.

  **Cost, and the shape the developer must use.** One lifted composable and one call site.
  `JumpSound` is `private fun JumpSound(transition: MoveTransition?)` at `GameScreen.kt:320-330`;
  it takes a `MoveTransition?` and reads `LocalSoundPlayer`, and touches no `GameViewModel`, no
  repository and no navigation. Lifting it to a shared `internal` composable that both screens
  call keeps §5.1's four prohibitions intact (see the note under §5.1) and keeps §5's "no
  `SoundCue` reference added to `BoardCanvas`" intact, because the effect lives *beside* the
  screen and never inside the canvas. The state it needs is already there: `BoardCanvas` draws
  the jump slide from `state.animating` (`BoardCanvas.kt:115`), so a tutorial that shows the
  slide D-01a promises has already populated the very field the cue is keyed on.
  **Re-implementing the effect inside the tutorial instead of lifting it is a defect** — the
  `REMOVAL_LAYER_DELAY_MS` layering and the `runIndex` pitch ladder would be duplicated and would
  drift. The reviewer insists on the lift, not the copy.

  **The completion CTA and `Skip`.** D-01h's blanket "no cue at all plays anywhere on the
  tutorial screen" collided with three things in this same document and repo: §4.1's full-width
  `CtaGradient` completion button, SOUND-04 ("Player hears button-press feedback on primary UI
  actions", `.planning/REQUIREMENTS.md:41`, also Complete) and D-11's forward/dismiss split
  (`SoundCue.kt:70-74`). §5's enumerated prohibition named only board and completion cues, so a
  developer building the CTA with the house idiom — `withClickSound(SoundCue.ButtonForward, …)`,
  as at `HomeScreen.kt:447` and `:581` — would have violated a spec sentence that no grep and no
  test could catch, while a developer omitting it would have violated SOUND-04. Ruled: **both
  press cues fire, with the app's standard cues.** They are *exit* actions and belong to the
  navigation vocabulary the rest of the app already speaks; neither can be mistaken for a verdict
  on the player's tapping, which is the only thing D-01c actually protects. §5's list now names
  `ButtonForward` and `ButtonDismiss` explicitly, so the ruling is checkable either way.

  **No new asset and no new licence row.** `Jump` → `R.raw.sfx_jump` (`SoundCue.kt:30`),
  `PegRemove` → `R.raw.sfx_peg_remove` (`SoundCue.kt:33`), `ButtonForward`/`ButtonDismiss`
  (`SoundCue.kt:70-74`) all ship today and are credited CC0 1.0 in `docs/audio/CREDITS.md`. §5's
  "No new audio asset" stands unamended, and §9's row is updated only to say which cues the
  tutorial now uses.

  **One thing this ruling cannot prove by reading, so QA checks it.** `JumpSound` consults no
  animation flag of any kind (`GameScreen.kt:320-330`), and the project's precedent is that cues
  survive reduced motion (`StarPopSounds` reads `rememberAnimationsEnabled()` only to change the
  stagger, `ResultsCelebration.kt:104-106`). But with OS animations off `runJumpAnimation` takes
  the snap path and calls `onAnimationFinished()` immediately (`BoardCanvas.kt:298-304`), so
  `animating` is set and cleared inside a very short window; whether the keyed effect always wins
  that race is a **device check, not a reading**. Criterion 7c therefore requires one pass with
  OS animations off.

  **Not in scope here.** rules/audio.md names `docs/audio-spec.md` as the cue list of record and
  that file does not exist on any branch. Writing it is the sound-designer's work and the
  producer is tracking it; this spec names cues per screen and does not substitute for it.

### The info button (NAV-01, D-15 superseded)

- **D-02:** **Delete the Home top bar's How-to-play `IconButton`** — `HomeScreen.kt:348-354`, the
  `Icons.Rounded.Info` action whose `contentDescription` is `R.string.how_to_play`
  (`strings.xml:62`). Its `onShowHowToPlay` parameter (`HomeScreen.kt:129`, passed to `HomeTopBar`
  at `:151`, wired at `HomeRoute.kt:48`) leaves `HomeScreen` entirely. — The founder asked for it,
  and it is also the weaker of the two affordances it competes with: it is icon-only, it sits in
  the hardest-to-reach corner of the screen, and it reuses `Icons.Rounded.Info` — the *same glyph*
  the in-game show-moves toggle uses for a completely unrelated meaning
  (`HintToggleControl.kt:5`, `:42`). This deletion also removes half of that collision.

- **D-02a:** The replay path is a **Settings › General row, "How to play"**, and that is the only
  permanent entry. First-run auto-start is **unchanged**. — Both routes are required and they
  answer different needs. First-run auto-start (today: `onboardingSeen == false` →
  `onboardingVisible = true`, `HomeViewModel.kt:99-103`) is how a new player meets the game and
  costs a returning player nothing, because `OnboardingRepository.onboardingSeen`
  (`OnboardingRepository.kt:48-51`) is already persisted and fails open to `false` — so a
  returning player is never re-taught and a player whose store read fails is re-offered the
  tutorial rather than silently denied it. The Settings row is where a casual-game player looks
  for "how does this work again", it is a labelled row rather than a bare icon, and
  `SettingsLinkRow` (`SettingsScreen.kt:599`) already supports exactly this shape. Home keeps
  zero tutorial chrome, which is what the founder asked for.

- **D-02b:** `HomeViewModel.onShowHowToPlay()` (`HomeViewModel.kt:227-229`) is **retired** as a
  Home concern. `onDismissOnboarding()`/`markSeen()` (`:232-235`) survive as the tutorial's own
  completion path. — Once Home has no affordance, a Home handler that forces `onboardingVisible`
  true has no caller; leaving it would leave a second writer of the flag that
  `HomeScreen.kt:192-198`'s guard exists to defend against, with nothing writing it.

### Home (NAV-01, PERS-03, CHAL-05, proposed UX-02)

- **D-03:** Home is composed of **five stacked bands** — hero, progress ribbon, Continue chips,
  mode tiles, footer — inside one scrolling column capped at `content_max_width`. The two mode
  tiles remain the primary actions and are **bottom-weighted into the thumb zone**. — The founder
  is right that two tiles on an empty field reads as unfinished, and the fix is not more buttons:
  it is giving the player something about *their own* game to look at. Everything added is either
  the game's own board art or a number the player earned.
  *(Amended 2026-09-28 — see D-03i and §4.2.2. The five bands, the cap and the bottom-weighting all
  stand; what is added is the mechanism that actually distributes the vertical slack, and the band
  set a screen too short to seat all five gets.)*

- **D-03a:** The Home `TopAppBar` (`HomeScreen.kt:340-363`) is **deleted**; its gradient title
  moves into the hero band and the Settings gear becomes a clay circular chip at the hero's
  top-end. — With the info button gone (D-02) the bar holds one icon and a title, which is chrome
  around a single control. The gradient Fredoka title is the brand moment and deserves display
  scale, not `titleLarge` in a 56dp bar.
  *(Amended 2026-09-28 — founder-feedback amendment, **D-03l**. The bar stays deleted and the
  title keeps display scale; what changes is where it sits: **start-aligned in one row with the
  gear on every window**, no longer centred under art at the top of a separate hero block.)*

- **D-03b:** The hero carries **decorative board art**, not a logo or an illustration. — The
  studio ships no logo, and rules/ip-policy.md means any sourced art needs a licence row. The
  game's own candy pegs and dimples are already an `ImageVector`-free, hex-free, licence-free
  visual language (`BoardCanvas.kt:480-522`); using them costs nothing and cannot be an IP
  problem. Art direction between two equally valid treatments is queued as **Q-901** (§15) with a
  default chosen so nothing blocks.
  *(Superseded in its art on 2026-09-28 — founder-feedback amendment, **D-03l**. The founder
  rejected the static peg cluster Q-901 chose (Q-004), so `HomeHeroArt` is withdrawn on every
  window. The rationale above still binds the replacement: the hero's decoration is still the
  game's own board art, licence-free and asset-free — now a clay board showing a game in progress,
  drawn into the slack rather than above the title. Q-005 asks the art direction again.)*

- **D-03c:** The progress ribbon shows **three pills: Stars · Boards · Level**, and each pill
  renders a neutral em-dash placeholder when its value is not yet known. — Three is the most the
  ribbon can hold at 200% font scale without wrapping into a second row, and the placeholder rule
  is what makes the band shippable before its data lands: the developer can build the ribbon in
  one plan and fill the two derived numbers in another without a half-drawn screen in between.

- **D-03d:** **No number on Home requires a new table, column or DataStore key.** Stars and Boards
  are folded client-side from `ProgressRepository.observeResults(mode)`
  (`ProgressRepository.kt:83`), which already streams `stars`, `bestTimeMs` and `bestScore` per
  level (`ProgressRepository.kt:284-291`); `HomeViewModel` already injects `ProgressRepository`
  (`HomeViewModel.kt:86`) and simply never reads it for stars. Level is
  `HomeUiState.challengeCurrentLevel`, which exists today (`HomeUiState.kt:72`). — This was
  checked against the data layer before the band was designed, per the brief. The
  `suspend fun aggregate()` path (`ProgressRepository.kt:115`) is deliberately **not** used: it is
  a one-shot, not a flow, so a Home band built on it would go stale the moment the player
  finished a level, and its `totalStars` is a deliberate both-modes sum with no `WHERE`
  (`LevelResultDao.kt:66`) that cannot answer "Classic boards cleared". §7 lists the exact
  requirement.
  *(Amended 2026-09-21 — see D-03h. Every conclusion above stands, including the refusal of
  `aggregate()`; only the read **mechanism** — a client-side fold — is superseded.)*

- **D-03e:** Home **does not** show Play Games sign-in state. — The flow exists
  (`GamesService.session`, `GamesService.kt:69`; `GamesSignInState.SignedIn.displayName`,
  `GamesSignInState.kt:63-66`) but only Settings collects it
  (`SettingsViewModel.kt:110-111`), and Settings already renders it well
  (`SettingsScreen.kt:289-330`). Putting an account chip on Home would duplicate a surface, add a
  third `Unknown`-state flicker to the start destination, and spend the player's first screen on
  something that is not playing the game. It would also put a Play Games SDK-backed flow on the
  cold-start critical path (D-03g), which is the second and harder reason.

- **D-03f:** **Three existing readouts are device-test oracles and are load-bearing, not
  decoration.** The Challenge ladder position, the Classic per-board stars and the Continue/resume
  affordance must remain visible, un-collapsed and un-deferred. Nothing in this refresh may hide
  any of them behind a tap, a expander, a scroll, or a later screen. — This is a release-manager
  ruling, not a preference: the cloud-save device checklist (`11-HUMAN-UAT.md`, tests 10/11/13)
  reads its pass/fail off exactly these three, so hiding one does not merely degrade the screen —
  it makes cloud save **unverifiable on a device**. §4.2.1 states where each one lives and what
  "survives" means for it, because two of the three are easy to break by accident and one of them
  does not live on Home at all.

- **D-03g:** **Home's first frame must add no blocking read, no image decode and no asset load.**
  Every visual this refresh adds to Home is vector or `Canvas` geometry drawn from state the app
  already holds; the one genuinely new data read (the progress ribbon, §7 R-1/R-2) paints
  placeholders immediately and fills on its first emission, so it can never hold up first paint. —
  PLAT-08 criterion C1 is a measured pass/fail at **≤ 950 ms cold start**, and a fuller Home is
  first-frame work by definition: every band added here is on that critical path. A "fancier"
  Home that misses C1 has failed, so the budget is designed in rather than discovered at the gate.
  §6.5 states the budget, the risk and the ordered degradation path if measurement shows a
  regression.
  *(Amended 2026-09-21 — the ruling stands, but **how** C1 is checked for this phase changed: it
  is a two-arm delta against `develop`, not a single absolute number against the pre-SDK 633 ms
  reference. The procedure is §6.5 "How C1 is measured for this phase" and the gate is criterion
  15e.)*
  *(Amended again 2026-09-28 — see **D-03j** and §6.5's **second edition** of that procedure. Every
  hard rule for Home's first frame above is untouched and still binds. What changed is the **number
  this phase is gated on**: the ≤ 950 ms absolute quoted in the rationale is no longer phase 15's
  gate, and the two-arm delta is now **interleaved** with a **null control** whose measured floor
  sets the threshold, because the old ≤ 50 ms threshold was below the instrument's own noise
  floor.)*
  *(Amended 2026-09-29 — tech-lead font ruling, `.planning/reports/15-founder-feedback/tech-lead-fonts.md`.
  The hard rules stand with **one named exception**: the app's two type faces, bundled as five
  static files in `res/font`, load blocking during Home's first composition, so the first frame is
  drawn in them. No other asset joins them. §6.5 states the exception, its cost row, and why it is
  not on the degradation path.)*

- **D-03h** *(2026-09-21 — tech-lead ruling, `tech-lead-architecture.md` §2/§3, folded in)*: the
  two derived ribbon values come from **one `Flow`-returning SQL aggregate**
  (`ProgressRepository.observeHomeProgress()` over `LevelResultDao.observeHomeProgress`), not from
  a client-side fold over `observeResults`. D-03d's conclusions are unchanged — no schema change,
  no migration, no new column, no new DataStore key, no `aggregate()`, one collection for both
  pills, placeholder-first — and so is every pixel of §4.2. Two things it changes are mine, which
  is why it is recorded on this side of the wall and not only in the report:
  1. **The placeholder rule gets stronger, not weaker.** A failed read must render `—`, never
     `0`: the new repository member carries the same fail-degraded `.catch { … }` contract
     `observeResults` has, so "database unavailable" and "no stars yet" stay distinguishable on
     screen. A `0` shown to a player who has earned forty stars is a lie about their own progress
     on the first screen they see. §14 gains a criterion for it.
  2. **The cold-start risk §6.5 named is the one that retires.** The fold's cost grew with the
     player's Challenge history — invisible on a fresh install, worst on the most engaged player.
     The aggregate is one row regardless of the ladder's length. §6.5's ordered degradation path
     is otherwise unchanged and still ends at "the pills stay and show —".
  — Data-layer mechanism is the tech-lead's authority; what this spec owns is what the player sees
  while the read is slow, empty or broken, and that is stated above and in §4.2.

- **D-03i** *(2026-09-28 — UX ruling. It **postdates the build under audit** and applies to a
  follow-up change; nothing above it is rewritten.)*: Home's vertical slack is distributed by **two
  weighted `Spacer`s inside a scrolled column whose minimum height is the viewport** — the one thing
  that makes a `weight` work where `verticalScroll` has made the column unbounded — and **the hero is
  the band that yields on a short screen**, by dropping its decorative art and taking a compact band
  set below `home_compact_threshold_height`. The layout contract, the arithmetic for both baselines,
  the ordered yield and what may never yield are **§4.2.2**; the criteria are 15g–15j.
  — A fixed `Spacer` cannot be right on two screen sizes at once, and the 24dp that shipped is right
  on neither: measured, the mode tiles sit at 36–54% of screen height with ~45% dead space below
  them on a 6.7-inch phone (`android-game-dev-s5-s8.md`), which is the "looks empty" the phase was
  commissioned to fix, moved rather than removed. The same arithmetic shows the other half of the
  problem is **not** caused by pushing content down: with the full band set, criterion 15a was
  already unreachable on its own baseline — hero 172dp as built + ribbon 64dp + the four oracle
  bands' 336dp + seven 16dp gaps + 32dp of padding is **≈716dp**, against a 360×640dp window that
  can offer at most 640dp and in practice ~568dp. Two tiles low on a tall phone and four oracles
  above the fold on a short one are therefore not a trade to be split down the middle with one
  number; they need a mechanism that reads the space it has, which is what §4.2.2 specifies.
  *(Amended 2026-09-28 — see **D-03k** and **§4.2.3**. Every clause of the mechanism stands: the
  `BoxWithConstraints`, the modifier order, the two weighted `Spacer`s, the 3 : 1 ratio and clause
  4's ban on a weighted content band are untouched and still bind. What is amended is the
  **arithmetic** this ruling argued from — it summed band minimums and the bands wrap — and with it
  the threshold, which now reads width as well as height, and the yield order, which gains one
  step. The conclusion this ruling reached is unchanged and is now stronger: one number cannot
  serve two screen sizes.)*
  *(Amended again 2026-09-28 — founder-feedback amendment, **D-03l**, §4.2.4. The
  `BoxWithConstraints`, the modifier order, `heightIn(min = available)`, the 3 : 1 ratio and clause
  6 stand. Three clauses gain one exception each: the upper weight moves from "between bands 2 and 3"
  into the hero, directly beneath its row (clause 3); it may carry **one** decorative drawing, which
  is the only weighted child on Home allowed to hold anything (clause 4); and that drawing borrows
  the hero's entrance order (clause 5). The founder's screenshot is why: the ¾ share this ruling
  parked between the ribbon and the chips measured **≈172dp of nothing** on a 6.7-inch phone.)*
  *(Amended 2026-10-21 — see **D-03n**, §4.2.5. The 3 : 1 split of the current slack is replaced: the
  footer is one fifth of the slack Home has with **no** Continue chip, fixed for the window, and the
  stage takes the rest less the chips' room, so a chip's arrival moves the stage and the ribbon and
  never the tiles. The rest of the mechanism stands: the `BoxWithConstraints`, the scroll column,
  `heightIn(min = available)`, clause 4 with its one exception, and clause 6.)*

- **D-03j** *(2026-09-28 — folded in from the tech-lead's ruling
  `.planning/reports/phase-15/tech-lead-coldstart-gate.md` §4, which corrects a gate that report's
  own author had specified. It postdates the build under audit and rewrites nothing above it.)*:
  **the ≤ 950 ms cold-start absolute is not a phase-15 gate. It is a tracked target owned by
  PLAT-08 / phase 14, and the bar of record for launch is rules/studio.md's product baseline —
  under 2 seconds on a named mid-range physical phone.** Phase 15's own gate is **relative only**:
  the interleaved A/B delta with a same-sitting null control specified in §6.5's second edition, and
  criterion 15e carries no absolute number. — This restores two earlier decisions rather than
  inventing a deferral. Phase 6 wrote its 633 ms figure as the before-picture "**PLAT-08 measures
  against in Phase 14**" (`06-BUILD-EVIDENCE.md:308-309`), so the absolute comparison was always
  phase 14's; and the first edition of §6.5 already routed a condition-(1)-only failure to
  PLAT-08 / phase 14 "rather than being paid for out of this phase's visual budget" — this ruling
  stops that routing from being a special case and makes it the rule. Three facts make 950 ms
  indefensible as a *gate* number now: it derives from a **pre-SDK** baseline that predates UMP,
  AdMob, Play Games, leaderboards and cloud save; that baseline was taken on a build with a
  different `applicationId` (`com.app.pegsolitaire` then, `com.erman.pegsolitarie` now); and on a
  shared host the absolute reading is not a property of the app at all — the **same AVD** returned
  **957 ms and 1832 ms**, a factor of **1.83×**, with nothing changing but host load
  (`15-BUILD-EVIDENCE.md:52-53`, `qa-engineer.md:162-163`). QA's ~1.8 s is therefore evidence about
  the host and **must not be read as either passing or failing the 2 s bar**. What this document
  owes in exchange is stated and not implied: §6.5 records that the 2 s bar **has never been
  measured on a physical phone**, with the recommendation and the decided default, so the
  reattribution cannot be read as the bar being dropped. Nothing in §4.2 or §4.2.2 may assume the
  bar is met; D-03g's hard rules for Home's first frame are the design-side answer and they hold
  whichever device the number eventually comes from.
  — Which number gates *this* phase is a UX-contract question because §14 is this document's, and
  the instrument and the ownership are the tech-lead's; this records the half that is mine.

- **D-03k** *(2026-09-28 — UX ruling on measured evidence, taking the branch D-03i's own §4.2.2
  reserved to this role: `.planning/reports/phase-15/code-auditor-s9.md` MB-1. It applies to the
  slice **after** the build audited on 2026-09-28 and rewrites nothing above it.)*: **a 360×640dp
  phone with both Continue chips scrolls, and the criterion says so.** Four parts, each specified
  in **§4.2.3**: **(1)** §4.2.2's compact arithmetic is corrected to sum **measured, wrapped** band
  heights at 360dp width (≈700dp, not ≈544dp) against **568dp** of usable height, not 640dp;
  **(2)** the compact threshold reads **width as well as height** — at an available width below
  400dp the height threshold is 880dp, so **every 360dp-wide phone is compact**, and at or above
  400dp nothing changes; **(3)** one new thing yields and it costs **no readout**: the progress
  pill's decorative glyph and its stacked third line, taking the band from a measured 116dp to
  ≤ 56dp with the same three values, the same three labels and the same single TalkBack sentence;
  **(4)** **criterion 15h is retired as false** and replaced by **15k**, which requires the hero
  row, the whole ribbon, **both** Continue chips and the **first** mode tile complete inside the
  first viewport, the **second** mode tile **breaking the fold**, and the rest reached by one plain
  vertical scroll of **≤ 120dp** with no tap, no expander and no state change. New criteria:
  **15k, 15l, 15m**.
  — The evidence forces part 1 and part 1 forces the rest. Measured at 360dp width the ribbon is
  **116dp** against a stated 64dp, each Continue chip **108dp** against 72dp, the mode tiles
  **108** and **116dp** against 96dp: ≈700dp of content where the table predicted ≈544dp. The
  ribbon's half of that error is provable by reading rather than measuring, and it is the reason
  §4.2.2 wrongly called the band unyieldable: the pill is
  `padding(spacing_sm)` + glyph 20dp + `spacing_xs` + label + `spacing_xs` + value +
  `padding(spacing_sm)` (`HomeProgressRibbon.kt:129-163`), i.e. **44dp of chrome over two text
  lines — ≥84dp before any wrap** — while §4.2.2's arithmetic omitted the glyph and one
  `spacing_xs` and read a 64dp *minimum* as if it were a cap. **After every yield that costs no
  readout, the compact set is still ≈640dp against 568dp usable — 72dp over — and all 72dp are in
  the hero's 48dp gear and gradient title, oracle 3's two Continue chips, §11's two 96dp tile
  floors carrying oracle 1's level chip, and `spacing_sm`.** There is nothing left to take that is
  not a readout, a target or a route, so a no-scroll fit is not available at any price this
  document is willing to pay, and asserting one would be a second false green three weeks after
  the first. **What is deliberately *not* done, and why it looks cheaper than it is:** dropping the
  ribbon would save 56–116dp and make the arithmetic close (≈560dp against 568dp), and it is
  rejected three times over. An 8dp margin is not a margin — it is inside the variance between
  Robolectric's text metrics and glass, between a 48dp navigation bar and a ~24dp gesture handle,
  and above any font scale over 1.0. It would make criterion 11 ("three progress pills", no
  screen-size qualifier) false on exactly the phones whose Home looks emptiest, un-answering the
  founder's ask where it was loudest. And it is the one change that would genuinely breach D-03f:
  §4.2.1 records that the Level pill and the Challenge tile's chip "read the same
  `challengeCurrentLevel`, so they cannot diverge by construction", which makes **the Level pill
  the ladder position's above-the-fold witness** once the tile chip is below the fold. The ribbon
  is not the band that disappears; it is the band that makes the scroll safe.
  — Both halves are this role's: §14 is this document's, so retiring a criterion is a UX decision
  and not a concession extracted by a failing test; and the band set, the threshold and the yield
  order are layout, which rules/studio.md puts here. What is **not** claimed here is a device
  reading — every dp above is Robolectric's, the qa-engineer's glass numbers govern, and 15k names
  the failure route back to this role if they differ.
  *(Corrected 2026-09-29 — see **D-03m**. The ruling and all four parts stand. One figure in the
  argument does not: 24dp of the "72dp over" was the hero row's 72dp **floor** above its 48dp gear,
  which is not a readout, a target or a route. With the floor retired the compact-narrow set is
  ≈616dp, **48dp over**, and the fold still cuts through the second tile (§4.2.3's pointer, §4.2.4's
  last subsection).)*
  *(Pointers 2026-10-21.)* **(a) The headline reads on Robolectric's LEGACY metrics.** On real metrics
  (Robolectric NATIVE in the app theme, and the device) a 360×640dp phone with both chips overflows
  0dp, so the screen does not scroll there. The overflow branch is read on `w360dp-h560dp` (criterion
  15k part 2's 2026-10-21 note). The ruling, its four parts and 15k are unchanged. **(b) Part 3's pill
  is amended by D-03o.** The compact pill keeps its one line at default font scale. Its "the label
  wraps" is replaced: no label or value is ever broken inside a word, and once any label no longer
  fits in half its pill, all three pills stack (§4.2.6).

- **D-03l** *(2026-09-28 — **founder-feedback amendment**, UX ruling on the founder's Q-004
  answer: "I don't really like this screen", scoped to the **empty middle gap**, the **peg-cross
  logo** and **how the title sits**. Amends D-03a, D-03b and clauses 3–5 of D-03i; specified in
  **§4.2.4**; criteria **15n–15r**; art direction queued as **Q-005**, non-blocking, with this as
  the built default.)*: **the hero is one row, and the slack becomes a stage.** Three parts.
  **(1)** The peg-cross logo (`HomeHeroArt`, Q-901 option 1) is **withdrawn** on every window.
  **(2)** The title stops floating centred under art and **sits start-aligned in one row with the
  Settings gear** — the geometry the compact band set already had, now the only hero Home has, at
  every size. **(3)** The weighted slack D-03i put between the ribbon and the Continue chips moves
  **into the hero, directly beneath its row**, and carries a **stage**: a static clay board slab
  showing a game in progress, whose edge is the slack's own height (capped), and which is not drawn
  at all when the slack is too small to hold it. The ribbon, the chips, the tiles, the palette and
  the blobs are unchanged in design; the ribbon now sits directly on the Continue chips, which is a
  change of **position** forced by the hero and the only one any kept band receives.
  — The founder named three things and they have one cause. D-03i kept the tiles low by parking
  three quarters of the slack **as nothing** in the middle of the screen; on the founder's phone
  that nothing measured ≈172dp between two islands of content, above which a small emblem sat over a
  centred word. Tiles in the thumb zone (rules/compose.md, criterion 15g) and no hole in the middle
  are only both achievable if what sits above the tiles **grows into** the space instead of being
  pushed down by a gap. D-03i's clause 4 is right that a *content* band must never do that — a
  weighted child is measured at 0 when the column overflows — but a decoration may, because 0 is
  its correct size on a short screen: that is exactly what the compact set already did to the hero
  art. So the stage is the one child on Home allowed to hold the weight, and the logo is not redrawn
  bigger; it is replaced by **the game itself**, on the game's own slab in the game's own pegs,
  which previews what the player is about to do instead of standing for it. The title's new seat is
  the same argument from the other side: a word centred under a picture and above a ribbon belongs
  to neither, while a title in one row with the gear is grounded by the only other element that
  shares its job, and it is the shape 360dp-wide phones have shown since D-03i — so the two width
  classes stop looking like two different apps. Screens, layout and decoration are this role's
  (rules/studio.md); which of several strong directions the founder *prefers* is taste, so Q-005
  asks it without blocking anything.
  *(Amended 2026-09-29 — see **D-03m**. The hero row, the stage and everything above stand. The row
  loses its 72dp minimum height: it is exactly as tall as its tallest child. Criterion 15p's gaps are
  measured between visible elements.)*
  *(Amended 2026-10-21 — see **D-03n**. The stage stands, and so does its art. Its height is no longer
  three quarters of the current slack. It is the slack less the chips' room less a footer fixed for
  the window, so the stage, and not the tiles, absorbs a Continue chip's arrival.)*

- **D-03m** *(2026-09-29 — **amendment**. UX ruling on the qa-engineer's escalation **E-1** on
  criterion 15p part 1, `.planning/reports/15-founder-feedback/qa-engineer.md`. Amends D-03l's hero
  row and §4.2.4's arithmetic, and is specified in the last subsection of **§4.2.4**. It adds no
  criterion; 15p gains a pointer.)*: **15p's gaps are measured between what is drawn, and the hero row
  loses the floor that hid a gap inside it.** Two parts.
  **(1)** Every gap or blank criterion on Home — the three parts of 15p, and 15g's "no single gap" —
  is read between **visible** elements. A visible element's edges are the layout bounds of a drawn
  surface or a laid-out text, never an undrawn box. A composite band's edges are the union of its
  visible children.
  **(2)** `home_hero_row_min_height` (72dp) is **retired**. The hero row is exactly as tall as its
  tallest child, which is the 48dp gear chip at default font scale.
  — The 72dp floor put 24dp of air around a 48dp gear. So the hero→art gap the player sees was
  16 + 12 = 28dp (27.4dp measured on the Pixel_9), while a test that measured the box read 16dp. The
  founder's complaint (Q-004) was a gap the eye sees, and a criterion that passes such a gap is a false
  green. With no floor, the row's box and its visible extent are the same. The gap is then the band gap
  in every state: 16dp in the full set, 8dp in the compact one. The stage gains the 24dp, about +18dp
  of board edge. §4.2.4's bound on the largest blank (< 80dp / < 64dp) becomes exact. The ruling holds
  in Fredoka as it does in Roboto. The title's 44dp line box is shorter than the gear's 48dp in either
  face. "Peg Solitaire" measures 205.7dp in Fredoka SemiBold against 205.5dp in Roboto, and the title
  has 296dp of room at 360dp width.

- **D-03n** *(2026-10-21 — **amendment**. UX ruling on the qa-engineer's loop-3 finding **F-1**,
  `.planning/reports/15-founder-feedback/qa-engineer-loop3.md`, routed by the tech-lead's phase-15
  sign-off, Decision 3, `tech-lead-signoff.md`. Amends D-03i's 3 : 1 split and how D-03l's stage gets
  its height; specified in **§4.2.5**; criteria **15s–15v**. Built on `bugfix/home-chip-arrival`.)*:
  **when a Continue chip arrives, nothing a player can tap moves — the stage gives up the room.** Four
  parts. **(1)** The hero row and both mode tiles keep the same bounds whether Home shows no chip, one
  or two, wherever the stage can give the chips their room, which on every phone window §14 measures
  at default font scale is everywhere. **(2)** The footer stops being a share of the current slack:
  it is **one fifth of the slack Home has with no Continue chip**, fixed for the window, and the stage
  takes the rest less the room the chips need (a chip's height plus the band gap it brings). So the
  chips take their room from the stage first, from the footer only once the stage is spent, and the
  tiles move by exactly what the stage could not give, never more. **(3)** What does move, moves
  continuously: the chips' room opens over the entrance duration and the stage and the ribbon above
  it yield in the same frames; with OS animations off it is open on the next frame. **(4)** The first
  frame is whole: the stage slab is never drawn without its pegs, nor a mode tile without its art.
  — The chips arrive after first paint by design, and nothing can bring them forward: the collector
  decodes both resume snapshots before it emits, which is the guard against a phantom Continue
  (WR-02, CR-01), and §6.5 forbids a blocking read before Home's first frame. On QA's AVD they land
  ≈0.6s after first paint, later on a slow phone (sign-off Decision 3). So the only question is what
  may move when they land. Under D-03i's 3 : 1 split everything below the hero moved: on the 6.7-inch
  phone the ribbon jumped ≈72dp up and both tiles ≈24dp down per chip, so the founder's own
  one-chip Home shifted on every cold start (`15f/r2/coldstart9s.mp4`). The tiles are the screen's
  primary targets, in the thumb zone, and the player who taps quickly reaches for them first. A tile
  that slides 24–48dp under a thumb sends a tap meant for "Classic" into "Continue Classic", or into
  the gap between them. The ribbon is read-only and the stage is decoration, so those two are what
  may move, and §4.2.2's own sentence — "a layout that reshuffles when a Continue chip appears is
  worse than one that is stable per device" — becomes true at the moment of arrival and not only
  after it. One fifth rather than one quarter is the smallest change that pins the tiles on every
  measured phone window, 360×640dp included. It also keeps the founder's own one-chip state within
  ≈4dp of the layout they approved (H-4). The arithmetic and the options weighed are in §4.2.5.

- **D-03o** *(2026-10-21 — **amendment**. UX ruling on the qa-engineer's **O-2**
  (`15-founder-feedback/qa-engineer.md`, carried open in `qa-engineer-loop3.md`) and this role's own
  **R-3** (`ux-designer-15p.md`), routed by the tech-lead's sign-off. Amends D-03k part 2's "the label
  wraps"; specified in **§4.2.6**; criterion **15w**. Built on `bugfix/home-chip-arrival`.)*: **no
  word in a progress pill is ever broken, and when the labels cannot sit beside their values, every
  pill stacks.** A label or a value breaks only between words, never inside one; "Sta/rs", "Bo/ard/s"
  and "Leve/l" at 200% on a 360dp phone are what this forbids. On the narrow class the compact pill
  keeps D-03k's one line while every label fits in half of its pill; once any one does not, all three
  pills take the **stacked** shape — the label on its own line, the value under it, no glyph — at one
  height. The shape is decided from the labels and the pill's width, never from the values (so a
  value's arrival cannot change it) and never from the font scale. The spoken sentence, the strings,
  the widths and the yield order are unchanged.
  — The pill measures its value first and gives the label what is left; at 200% that is ≈47dp, and a
  single word wider than that is broken wherever it falls (`ux-designer-15p.md` R-3, not §7's R-3).
  In Nunito, which is wider, it got
  worse. Nothing is clipped, so criterion 15i holds on its letter, but a label broken mid-word is one
  the player has to reassemble, at exactly the font size chosen by a player who reads with effort.
  Every label is one short word that fits a 360dp pill whole at 200% ("Boards" ≈77dp against ≈93dp of
  room), so stacking costs only height on a column that already scrolls there (15i). One shape for all
  three keeps the band reading as one band.

### In-game controls (ASST-01, ASST-02, CLAS-02, SCOR-02, proposed UX-03)

- **D-04:** The five controls are kept, **re-glyphed, re-grouped and re-homed**: read-only status
  (back, title, timer) stays at the top in a chrome-free strip; the four *actions* (Undo, Hint,
  Show-moves, Restart) move into a clay dock at the bottom, in the thumb zone. — Five 48dp actions
  plus a timer label plus a title currently share one `TopAppBar` actions row
  (`GameScreen.kt:519-525`). Four targets at `top_bar_touch_target` 48dp (`dimens.xml:110`) is
  192dp before the timer, on a 360dp-wide phone — the title is already squeezed at default font
  scale and collides at large scale. Splitting by *kind* fixes the crowding and the reach problem
  at once: rules/compose.md requires primary actions in the lower thumb zone of a 6.7-inch phone,
  and Undo — the most-used control in peg solitaire, since backtracking *is* the game — is
  currently in the single worst-reachable corner. The strip that remains carries exactly one icon,
  redrawn in Candy Clay.

- **D-04a:** **Hint stops using `Icons.Rounded.Search`.** A new in-house `HintIcon` (a lightbulb)
  replaces it. — A magnifying glass reads as "search" or "zoom", not "show me a winning move"
  (`HintControl.kt:5`, `:69`). The core icon set has no lightbulb and the extended set is banned,
  so the glyph is authored locally exactly as `UndoIcon.kt` and `ModeGlyphs.kt` already are.

- **D-04b:** **Show-moves stops using `Icons.Rounded.Info` and stops conveying its state by tint
  alone.** A new in-house `ShowMovesIcon` (a peg with two trailing destination dots — literally
  what the feature draws) replaces the glyph, and the ON state adds a filled `primaryContainer`
  pill behind it. — Two defects in one control. The glyph collides with the Home info button's
  meaning (`HintToggleControl.kt:5` vs `HomeScreen.kt:23`), and the on/off state is carried by
  `primary` vs `onSurfaceVariant` tint and nothing else (`HintToggleControl.kt:45-51`), which
  breaks rules/compose.md's "never colour alone (shape, pattern, label)". The content description
  already switches correctly (`show_moves_on`/`show_moves_off`, `strings.xml:90-91`) — the defect
  is purely visual, and a filled-vs-unfilled pill fixes it without new copy.
  *(Amended 2026-09-28 — founder-feedback amendment, **D-04e**, §4.4.1. The glyph fix stands. The
  state fix was right in kind and too weak in fact: the `primaryContainer` fill it added measures
  ≈1.04:1 against the dock slab, and two other controls wore the same fill as "loud" (D-04c), so
  the founder could not read on from off. ON is now a solid `primary` disc with a check mark; OFF a
  ring with a slashed glyph. And the sentence above about the description is **withdrawn**: the
  swapped pair was not correct. Both strings are imperatives and each names the *opposite* of what
  the next tap does, so Show-moves now speaks as a switch with one constant name, and
  `show_moves_off` is deleted.)*

- **D-04c:** Undo and Hint are **visually louder than** Show-moves and Restart, by fill and not by
  size. All four targets are the same `game_action_button_size`. — Hierarchy has to be legible,
  but four different button sizes in one row reads as a mistake, and unequal targets invite the
  smaller ones to drop under 48dp at some font scale. Fill is the safer channel.
  *(**Retired 2026-09-28** — founder-feedback amendment, **D-04e**. Equal sizes stand. The
  loud/quiet split by fill does not: fill-versus-outline is the on/off vocabulary, so a dock whose
  actions were filled or outlined by *importance* read to the founder as two controls on and two
  off. No control is louder than another by fill any more; Undo and Hint keep the first two
  positions under the thumb, and Restart's protection against a careless tap is its mid-game
  confirm dialog (D-06), where it always was.)*

- **D-04d:** The Hint "thinking" state keeps its current behaviour exactly — spinner in place of
  the glyph, `enabled = false`, `hint_thinking` description (`HintControl.kt:47-62`). Undo keeps
  `enabled = canUndo`. — These are correct today, including the TalkBack "disabled" reporting that
  `enabled = false` gives for free; the redesign is a reskin of them, not a rewrite.
  *(Stands, 2026-09-28. D-04e gives the two states their looks — Hint thinking is **Working**,
  Undo with nothing to undo is **Unavailable** — and changes no behaviour, description or
  `enabled` value.)*

- **D-04e** *(2026-09-28 — **founder-feedback amendment**, UX ruling on the founder's note "I like
  the in-game icons, but I can't really say if they are on or off" (Q-004). Amends D-04b, retires
  D-04c; specified in **§4.4.1**; criteria **21a–21g**.)*: **one look, one meaning.** The dock
  holds two kinds of control, and they get two visual grammars that cannot be confused.
  **Actions** — Undo, Hint, Restart — have no on/off at all; they share one "button" look (a pale,
  raised disc) and change only between **Available**, **Working** (Hint's search) and
  **Unavailable** (Undo with nothing to undo). **The toggle** — Show-moves — is the only control
  with an on/off, and the only one ever drawn **On** (a solid `primary` disc with a check mark) or
  **Off** (a ring with a slashed glyph). Every glyph the founder likes is kept; Show-moves Off gains
  a slash. Every pair of the five looks differs in at least two channels that are not hue, which a
  unit test asserts. A spent hint or undo allowance is **not** a state: the control looks exactly
  Available and the tap raises the opt-in dialog, which is the earlier-phase ruling at
  `GameUiState.kt:131-135` applied, not relaxed. TalkBack gets the same fix: Show-moves becomes a
  switch — constant name "Show possible moves", announced state On or Off.
  — The founder's diagnosis is exact and the cause is a **channel collision**, not the icons.
  Fill-versus-outline was carrying two meanings at once — importance (D-04c) and state (D-04b) — so
  the dock showed two filled controls and two outlined ones, and the eye reads that as two on and
  two off. Worse, the channel was nearly invisible: `primaryContainer` #EDE4FF on the dock's
  `surfaceContainerHigh` #F1EAFB is **≈1.04:1** and the `outline` ring **≈1.5:1**, against WCAG
  1.4.11's 3:1 for visual information that identifies a state. Separating the kinds by grammar and
  giving state its own marks fixes both, without touching the glyphs, the order, the sizes, the
  cues or the ad-allowance contract. The alternative the founder's words invite — text labels under
  the icons — is weighed and declined in §4.4.1.

### The toolbar (D-05 — and its reconciliation with D-04)

- **D-05:** **The "toolbar" and the "icons on the top" are one surface, not two.** The code is
  unambiguous: `GameScreen.kt:171-187` gives its `Scaffold` a single `topBar`, which is
  `GameTopBar` (`:490-527`), which is a single M3 `TopAppBar` (`:502`) whose three slots are the
  back arrow (`:503-517`), the title (`:518`) and the five actions (`:519-525`). There is no
  second control row anywhere on the screen. So "remove toolbar" and "redesign the icons on the
  top" cannot be satisfied independently, and this spec treats them as **one change with one
  replacement**.

- **D-05a:** **What goes:** the `TopAppBar` composable itself, the `Scaffold`'s `topBar` slot, the
  bar's surface/elevation/inset chrome, and `GameTopBar` as a function. **What stays:** all five
  controls, the context-aware title logic (`GameTopBarTitle`, `:537-552`), the timer label
  (`ElapsedTimeLabel`, `:563-574`), and every handler signature `GameScreen` already takes. —
  Nothing the player can *use* is being removed. What is being removed is the Material chrome
  wrapped around it, which is the only reading of "remove toolbar" that does not also delete
  functionality the game needs.

- **D-05b:** **Back-navigation survives as a visible, labelled control** — a 48dp clay circular
  back chip at the status strip's leading edge, keeping
  `Icons.AutoMirrored.Rounded.ArrowBack` and the `R.string.navigate_back` content description
  unchanged. — This is non-negotiable and needs stating plainly, because the top bar is currently
  the *only* visible way out of a game: `onNavigateBack` is a bare `navController.popBackStack()`
  (`AppNavHost.kt:131`) and there is no `BackHandler` on the gameplay destination — the only one
  in the codebase is `AdQuiescenceScrim.kt:61`. System back therefore still works after the bar
  is deleted, but system back alone is not an acceptable exit: it is invisible, and on
  gesture-navigation devices it is a discoverability failure for exactly the casual player this
  game is for. Keeping the same glyph and the same content description also keeps
  `ButtonSoundTest.kt:226-244` — which finds this control by `R.string.navigate_back` — passing
  unchanged.

- **D-05c:** The status strip is **not** an M3 `TopAppBar`, `CenterAlignedTopAppBar`, or any
  `*TopAppBar`, and the action dock is **not** an M3 `BottomAppBar` or `NavigationBar`. Both are
  plain `Row`s inside the screen's own layout. — Re-adding a Material bar under a different name
  would re-introduce the chrome the founder asked to remove, and `NavigationBar` in particular
  carries destination semantics that would announce four in-game actions to TalkBack as
  navigation targets, which they are not.

- **D-05d** *(2026-09-28 — **founder-feedback amendment**, UX ruling on the founder's note
  "elapsed time and the title seem too small. They could be reworked." (Q-004). Amends §4.3's
  Centre and Trailing rows and §6.3; specified in **§4.3.1**; criteria **18a–18e**.)*: **the title
  is `titleLarge` (22sp), the timer is `headlineSmall` (24sp) with tabular figures in a 40dp clay
  pill that grows with the font scale, and each system bar is applied to the gameplay screen
  exactly once.** Slots, order, content logic, content descriptions and "no actions live here" are
  unchanged. — `titleMedium` (16sp) was chosen in §4.3 so the strip would be *quiet*, and quiet
  turned out to mean unreadable at a glance: the two things a player looks up for mid-game were the
  smallest text on the screen. The timer takes the larger of the two roles because it is the one
  that changes — the number a player checks — while the title is context read once. Both fit a
  360dp phone at default scale with room to spare (§4.3.1's width budget), and the board loses
  nothing, because the same ruling returns space the screen has been wasting: the strip applies
  `statusBarsPadding()` and the dock `navigationBarsPadding()` inside a `Scaffold` whose content
  padding already contains both bars. The founder's screenshot shows it as an empty band as tall as
  the status bar above the strip; on a 360×640dp phone it takes 72dp from the board's region.

- **D-05e** *(2026-10-21 — **amendment**. UX ruling on the qa-engineer's loop-2 **O-A**
  (`15-founder-feedback/qa-engineer-loop2.md`), the code-auditor's **A-8** (`phase-15/code-auditor.md`)
  and **I-2** (`15-founder-feedback/code-auditor-loop2.md`), routed by the tech-lead's sign-off
  ("Routed after the merge"). Amends the timer's content description in §4.3 and §4.3.1, nothing
  else; specified in **§4.3.2**; criteria **18f**, **18g**, and **26b** for criterion 26's spoken
  order.)*: **the timer speaks its value — "Elapsed time, 5 minutes, 12 seconds".** It is one focus
  stop, as today. Its description is the label, then the elapsed time as a spoken duration. TalkBack
  reads it when the player moves to it, and it is never announced on its own.
  — The clock's node carries the text "05:12" and the description "Elapsed time", and TalkBack speaks
  a description in place of the text. So a TalkBack player has heard the label and never the time,
  which predates phase 15 (A-8: "pre-existing, carried across verbatim"). Criterion 18 asked only that
  the timer is "announced as the elapsed time", which is true and useless. The label stays first
  because the strip is scanned stop by stop — back, title,
  timer — and the label says which stop this is. The value is spelt as a duration because "05:12" is
  read as a time of day or digit by digit, depending on the voice, and neither is a duration.

- **D-05f** *(2026-10-21 — **amendment**, app-wide; filed beside D-05d because D-05d is where this
  document first ruled on the system bars. UX ruling on the qa-engineer's 15e-sitting observation
  **O-1** (`15-founder-feedback/qa-engineer-15e.md`), routed by the tech-lead's phase-15 close report,
  INFO row 9 (`tech-lead-signoff-close.md`). Specified in **§6.2**; criterion **29**.)*: **the system
  bars take the app's theme — light with dark icons in light mode, dark with light icons in dark
  mode — and that includes the three-button navigation bar.** The status bar already does, through
  the tech-lead's F-2 fix (sign-off Decision 3, H-2). The three-button navigation bar does not: in
  dark mode it draws a light grey scrim (`#cdccd0`) with its buttons at 3.59:1. From this amendment
  it is dark, with light buttons, in dark mode, and unchanged in light mode.
  — **Is a dark bar wanted in dark mode? Yes.** In dark mode that bar is the only light surface on the
  screen, and it sits directly under the mode tiles and the dock, the thumb zone the eye returns to
  most, so it reads as a fault rather than a choice. Nobody chose it: it is light because the window
  theme's parent is the light framework theme in both modes. DESIGN-SYSTEM.md's dark theme is
  "desaturated tonal variants (not inverted)", and a light band under a dark app is the light theme
  leaking into it. This is not art
  direction between equals, so no founder question is drafted: no one would pick a light bar under a
  dark app, and the platform's own convention is the dark one. The buttons already clear 3:1, so the
  change is polish, not an accessibility repair. It changes the window theme, a start-path file, so it
  rides a branch that sits its own 15e: `bugfix/home-chip-arrival`, which sits one under D-03n.

---

## 4. Interaction contract — new surfaces (Phase 15)

### 4.1 Tutorial — `TutorialScreen.kt` (ONB-01, D-01/D-01a–h, D-02a)

**Purpose.** Teach the orthogonal jump and the goal by having the player do both, once, in under
a minute, without reading.

**Layout**, top → bottom, full viewport, `background` with the Home blobs reused:

| Band | Content | Notes |
|------|---------|-------|
| Header | Step pips (leading) · `Skip` text button (trailing) | Pips are the only progress signal; no title bar |
| Board | The live `BoardCanvas`, `interactive = true`, on a `surfaceContainerHigh` clay slab (`shapes.extraLarge`, `clayShadow(elevation_card)`), square, `tutorial_board_size` | The one interactive element |
| Caption | One line, `bodyMedium`, `onSurfaceVariant`, centred, reserved at `tutorial_caption_min_height` | Reserved height so a 1↔2-line swap never reflows the board |
| Footer | Empty until T3 completes, then the completion CTA | Keeps the board optically centred during play |

**The four bands sit in a `verticalScroll` column**, exactly as Home does (§4.2) and as §12
already assumes. Scrolling is for the overflow case only: at default font scale on a 360×640dp
phone all four bands fit with no scrolling, and the board keeps `tutorial_board_size` and its
square aspect at every scale rather than shrinking — a 3×3 board there is ≈93dp per cell, and
compressing the one interactive element to win vertical space is the wrong trade. What yields at
200% font scale is the caption's growth pushing the footer below the fold, which is harmless
because the footer is empty until T3 completes.

**Step pips.** Three pips, `tutorial_step_pip_size`. Done = solid `GoldGradient` disc; current =
outlined disc with a `primary` ring; upcoming = outlined disc, `outlineVariant`. Filled-vs-outlined
carries the state, so colour is not the only channel. The row is one semantics node via
`clearAndSetSemantics` announcing `tutorial_step_progress` ("Step 2 of 3") — the same idiom
`EarnedStarPips` already uses (`BoardPreview.kt:100-104`).

**The three steps.**

| Step | Teaches | Footprint | Completes when |
|------|---------|-----------|----------------|
| **T1** | Tap a peg, tap a hole two cells away; the middle peg goes | 1×3 strip: in-play (0,0),(0,1),(0,2); pegs at (0,0),(0,1); hole at (0,2) — exactly one legal move, horizontal | one peg remains (1 move) |
| **T2** | The same jump works on the other axis, and the destination is two cells away — never the hole next door, never a diagonal | full 3×3: all nine cells in play; pegs at (0,1),(1,1); the other seven are holes — exactly one legal move, vertical | one peg remains (1 move) |
| **T3** | The goal is to leave one peg | full 3×3: pegs at (0,0),(0,1),(1,0),(1,1),(1,2); holes at (0,2),(2,0),(2,1),(2,2) — three legal openings, every line wins | one peg remains (4 moves) |

> **Board content is the game-designer's ruling** (R-4, closed 2026-09-21 —
> `.planning/reports/phase-15/game-designer-tutorial-boards.md`). All three footprints are proven
> dead-end-free to one peg by exhaustive enumeration of every reachable position, so D-01c holds
> **structurally**: no sequence of taps can strand the player above the target, and the tutorial
> cannot be lost by anyone. T1 is unchanged from `OnboardingOverlay.kt:155-171`. The
> step-completion predicate is therefore **the same for all three steps — "the board holds one
> peg"** — so no step needs a bespoke completion rule.
>
> Two consequences for the screen, both this role's to state. **First, the footprints fit:** the
> largest is 3×3, which inside `tutorial_board_size` 280dp gives ≈93dp cells — comfortably above
> §11's 48dp floor, so no token changes (§6.1 stands). **Second, T1 and T2 each have exactly one
> legal move.** That is deliberate — a step teaching one mechanic pays nothing for ambiguity — but
> it means the coach must never read as a puzzle refusing the player: with `showHints` forced on
> (D-01b), selecting the only movable peg draws exactly one destination dot, and §4.4's escalation
> is assistance toward that dot, never correction.
>
> **The boards live in `object TutorialBoards` in `:domain` and are never added to
> `Boards.CLASSIC_SPECS`** (§5, §7 R-4). That list is the Classic catalogue and the denominator of
> the Home "Boards" pill (§7 R-2); adding a teaching board to it would change a number the player
> reads on Home and put three miniatures on the catalogue screen.

**Advance.** On completion the caption swaps, the pip advances, and the next footprint installs
after one beat (`TutorialAnim.STEP_ADVANCE_DELAY_MS`) so the player sees their own jump land
before the board changes. No button, no tap-to-continue. A step is complete when its board holds
exactly one peg — the same test for all three steps, evaluated as `board.pegCount == 1` on the
tutorial's **own** state. Not `GameStatus`, not a `minimumPegs` comparison: reaching for the
game's status type is the first step down the path that ends at `GameViewModel`, which §5.1
forbids outright (D-01g).

**The board the tutorial drives.** `BoardCanvas` takes a whole `GameUiState`
(`BoardCanvas.kt:87-93`), and that stays true — but the value is assembled by `TutorialViewModel`
from the current `TutorialBoards` footprint. `GameUiState` here is a **UI model**, not evidence
that the game's own state holder is involved; D-01a's "a real `GameUiState` and the real rules
engine" means exactly that and nothing more (D-01g, §5.1).

**Tap semantics — identical to the real game** *(ruled 2026-09-21; the first edition left the
fourth row below unspecified)*. The board answers five tap kinds, and it answers them exactly as
the gameplay board does, in the gameplay board's own branch order
(`GameViewModel.kt:788-796` documents the rule, `:797-825` is the code):

| The player taps | Answer |
|---|---|
| the selected peg again | it is put back down — **the only deselect** |
| a legal destination dot | the jump runs |
| any peg while **nothing** is selected | it selects and its legal destinations draw — zero of them if it cannot move (§4.4 class 1) |
| **a different peg while one is already selected** | **nothing changes.** The held peg stays held, no dots move, and the tap is a non-progressing tap (§4.4 class 2) that feeds the escalation. It does **not** move the selection. |
| anything else — a hole that is not a legal destination, an out-of-play cell | nothing changes; non-progressing (§4.4 class 2) |

The fourth row is the ruling, and it is ruled **to match gameplay rather than to be friendlier
than gameplay**. `GameViewModel.onCellTap` selects only while nothing is selected
(`GameViewModel.kt:817`) and answers every other tap with the no-op that the quiet invalid-tap
ordinal records (`:823`); its own KDoc states the rule in full — "re-tapping the selected peg is
the ONLY way to deselect, and tapping a DIFFERENT peg while one is selected is a no-op"
(`:788-796`) — which is the tap rule quick task `260628-n4f` locked. Three reasons the tutorial
does not get a gentler rule of its own:

1. **Fidelity is the premise, and a tap rule is the mechanic, not the chrome.** D-01/D-01a stake
   the design on the real canvas and the real engine so that "what the player learns is literally
   the mechanic they will use", and D-01i refused to let audio be "the sole deliberate
   divergence". Re-selection would be a stronger divergence than any cue: it is a **gesture the
   tutorial would teach and the game would then refuse**.
2. **The divergence would bite immediately, not rarely.** A peg with no hole two cells past a
   neighbour cannot move, so "the peg I tapped cannot move" is the *ordinary* case on a full
   board — §4.4 already notes it fires on T2's middle peg and on three of T3's five, and on
   English-33's opening most of the 32 pegs are that peg. A graduate who was taught "tap another
   peg to switch" therefore meets the refusal within their first few taps of their first real
   board, where it also plays the `InvalidTap` cue the tutorial withholds.
3. **Matching costs nothing.** Recovery is one tap — put the held peg down, then pick the other
   one — and that recovery *is* the game's, so the extra tap teaches the deselect gesture rather
   than hiding it. Nothing is lost, no progress is undone, and §4.4's escalation exists for the
   player who does not find it on their own; it now carries the one clause (the release) that
   makes it actionable while a peg is held.

**Consequence for the build.** `TutorialViewModel.onCellTap` currently takes the other branch —
"any other peg SELECTS, including while another peg is already held"
(`TutorialViewModel.kt:119-136`, the branch at `:133`) — a decision the developer had to make at
the keyboard because this document had no line to consult
(`.planning/reports/phase-15/android-game-dev-s1-s4.md`, Deviation 3). The report is right about
the situation it names and wrong about the remedy: the player holding an immovable peg is freed by
the coach's release (§4.4), not by the board reinterpreting their next tap. This ruling supersedes
that choice and the branch comes out; the tutorial's non-progressing answer stays **silent**, as
§4.4 class 2 requires — matching the game's *semantics* never means importing its `InvalidTap`
cue (D-01i).

**The jump the player hears.** A **completed** jump plays the game's own jump pair — the pitched
`SoundCue.Jump` with `SoundCue.PegRemove` layered beneath it — on every step, including each of
T3's four jumps, and with the same pitch ladder when the same peg jumps twice in a row (D-01i,
SOUND-01). It is the same effect the gameplay screen uses, lifted to a shared composable rather
than re-implemented (`GameScreen.kt:320-330`), and it is keyed on the same `state.animating` the
canvas already draws the slide from (`BoardCanvas.kt:115`). **That is the only sound the board
makes:** selection, deselection, a tap with no legal jump, a tap on an illegal destination and
the completion star are all silent (D-01i, §4.4, §5). The contrast is the point — the one thing
the tutorial rewards audibly is the thing it is teaching.

**Completion.** After T3: the caption becomes `tutorial_done_caption`, a single gold star pops in
reusing the Results star vocabulary (`ClayAnim.STAR_POP_MS`, `goldGradientFill()`), and the footer
raises one full-width `CtaGradient` button labelled with the **existing** `onboarding_got_it`
("Got it"). Tapping it plays `SoundCue.ButtonForward` — the same press cue every other primary
CTA in the app plays, via the house `withClickSound` idiom (D-01i, D-11, SOUND-04) — then
persists `markSeen()` and leaves. **The star itself pops silently:** neither `SoundCue.Win` nor
`SoundCue.StarPop` plays (D-01h, upheld by D-01i) — a tutorial is not a victory, and D-14's
two-beat celebration belongs to a real solve. `ClayAnim.STAR_POP_MS` is borrowed as a *duration*,
not as a cue. Nor is a star banked, a resume snapshot cleared or the Challenge ladder advanced:
the gold star on this screen is an animation and nothing behind it moves (§5.1, D-01g).

**Exit.** `Skip` (every step), system back and the completion CTA are **one exit path with three
callers** (D-01d, D-01g). The two that are button presses sound like button presses: `Skip` plays
`SoundCue.ButtonDismiss` and the CTA plays `SoundCue.ButtonForward` (D-01i, D-11). System back is
silent — there is no press to sound, and it is the same exit path, not the same event. The path
persists `markSeen()` first and pops only then, so Home can never
re-open the tutorial on the frame after the player left it; a second exit is a latched no-op.
Popping returns the player exactly where they came from — Home on first run, Settings on replay —
and a replay always restarts at T1, because the step state is scoped to the destination and dies
with it.

### 4.2 Home — `HomeScreen.kt` (NAV-01, PERS-03, CHAL-05, D-03/D-03a–h)

Single `verticalScroll` column, `widthIn(max = content_max_width)`, five bands:

**1 — Hero.** `home_hero_min_height`. The app title in `displaySmall` (Fredoka) filled with
`ctaGradientBrush()`, and decorative board art at `home_hero_art_size`. The Settings gear is a
48dp clay circular chip at the band's top-end, keeping `Icons.Rounded.Settings` and the
`settings_action` content description. The art is `contentDescription = null` and is fully static
when animations are off. **No info button (D-02).**

> *(Superseded 2026-09-28 — founder-feedback amendment, **D-03l**, §4.2.4.)* Band 1 is now the
> **hero row** — the gradient `displaySmall` title start-aligned and the unchanged gear chip at the
> end of the same row, on every window — followed by the **stage** that holds the slack. There is
> no hero art above the title any more and no `home_hero_min_height`/`home_hero_art_size`. The
> gear, its description, its silence and "No info button" are unchanged.

**2 — Progress ribbon.** One `Row`, three equal-weight clay pills, `home_stat_pill_min_height`,
`spacing_sm` between them, each a `surfaceContainerHigh` `shapes.medium` surface with
`clayShadow(elevation_card)`:

| Pill | Glyph | Value | Source |
|------|-------|-------|--------|
| Stars | `Icons.Filled.Star` + `goldGradientFill()` | total stars earned, both modes | `observeHomeProgress()` — one row, one collector (§7 R-1, D-03h) |
| Boards | existing `rememberClassicGlyph()` | Classic boards cleared `/ 5` | the same row, same collector (§7 R-2, D-03h) |
| Level | existing `rememberChallengeGlyph()` | the Challenge ladder position | `challengeCurrentLevel` — **exists today** |

Each pill is one merged semantics node reading label-then-value ("Stars, 7"), never a bare number.
A pill with no value yet renders `stat_value_unknown` ("—") and announces
`stat_value_unknown_desc` ("not yet available"). **"Not yet" and "the read failed" render
identically, and neither ever renders as `0`** (D-03h): a pill shows a number only once a real
value has arrived for it. The pills are **not** tappable — they are status,
and making them navigate would put three more competing targets on the screen the founder already
finds busy.

> The Level pill shows `challengeCurrentLevel`, which is the **next** level to play and is
> off-by-one from the highest level banked, by design (`ProgressRepository.kt:107-113`,
> `ChallengeProgress.kt:64-74`). The copy is therefore "Level N" — the level the Challenge tile
> will enter — and never "highest" or "best". Do not "reconcile" the two numbers.

**3 — Continue chips.** Behaviour, ordering and data unchanged (`HomeScreen.kt:293-318`). One
visual addition: the Classic chip's trailing accent becomes a `home_resume_thumb_size` board
thumbnail of the saved board instead of a generic play arrow, since
`classicResumeLevelId` already identifies the board and the catalogue is static — no new data.
The Challenge chip keeps the play arrow (a procedural level has no memorable footprint).

**4 — Mode tiles.** `ClassicTile` and `ChallengeTile` keep their containers, glyph badges, titles,
supporting slots, entrance stagger, squish, `ButtonForward` cue and every callback
(`HomeScreen.kt:498-560`). Each gains trailing board art at `home_tile_art_size` — a real footprint
for Classic, the bolt for Challenge. Classic's supporting line stays `classic_tile_supporting`
("5 boards").

**5 — Footer.** Empty. Reserved, specified as empty so a later phase does not read the gap as an
invitation.

**Thumb zone.** A weighted spacer sits between bands 2 and 3, so on a tall phone the slack goes
*above* the Continue chips and the mode tiles stay in the lower half. When content exceeds the
viewport the column scrolls instead of compressing. Home does not scroll today
(`HomeScreen.kt:285-292` is a plain `Column`) and at 200% font scale with two Continue chips
present it already overflows — adding `verticalScroll` is part of this change, not optional
polish.

> *(Amended 2026-09-28 — see §4.2.2 and D-03i.)* The paragraph above stands as the intent and is
> **left as it reads**: it is what the audited build was built against. What it does not say is how
> a weight distributes inside a scrolling column — it does not, and the fixed `Spacer` that
> replaced it (`HomeScreen.kt:311-313`) is right on no screen size. §4.2.2 specifies the mechanism,
> the ordered yield for a screen too short to seat all five bands, and the arithmetic for both
> baselines. The band order, the cap, the scroll-for-overflow rule and the first-paint rule above
> are unchanged by it.
>
> *(Amended again 2026-09-28 — founder-feedback amendment, **D-03l**, §4.2.4.)* "A weighted spacer
> sits between bands 2 and 3" is superseded: the upper share of the slack now sits **between the
> hero row and the ribbon** and is the stage, so the ribbon, the Continue chips and the tiles form
> one contiguous lower group. The intent of this paragraph is unchanged and better served — the
> tiles stay in the lower half (criterion 15g, recomputed in §4.2.4), and the slack stops reading as
> a hole because it now holds the hero's board.

**First-paint rule (D-03f).** Scrolling exists for the overflow case, not as a place to put
things. At **default font scale on a 360×640dp phone with both Continue chips present**, bands 3
and 4 — the Continue chips and both mode tiles — must be **fully within the first viewport with no
scrolling**. The hero is the band that yields: `home_hero_min_height` is a *minimum*, and the hero
must be allowed to compress toward it (art first, then title scale) before anything below it
leaves the fold. A Continue chip that is only reachable by scrolling is a broken oracle, not a
layout compromise.

> *(Amended 2026-09-28 — D-03k, §4.2.3. Left as it reads; this is the rule criterion 15h was
> derived from, so it is where the correction has to be visible.)* **Band 3 stands, absolutely and
> on every window.** Both Continue chips are fully within the first viewport with no scrolling, and
> the last sentence above is the reason — oracle 3 is the only one of the three with no second
> witness anywhere in the app, so a scrolled Continue chip is a broken oracle and always will be.
> **Band 4 cannot hold on a 360×640dp phone and is corrected.** §4.2.2's corrected arithmetic puts
> the compact band set at ≈700dp — ≈640dp after the last yield that costs no readout — against
> **568dp** of usable height, and every remaining dp is a readout, a target or a route. So on that
> window the **first** mode tile is fully within the first viewport and the **second** breaks the
> fold, reached by one plain scroll of ≤ 120dp (criterion **15k**, which replaces 15h). **The
> hero's yield order in this paragraph is also superseded in one detail**: the hero does not
> "compress toward `home_hero_min_height`, art first then title scale" — §4.2.2 step 2 drops the
> art outright and takes `home_hero_compact_min_height`, and the title is **never** scaled down at
> any size. Everything else here — scrolling exists for overflow and not as a place to put things
> — is untouched and is exactly what §4.2.3 holds the narrow class to.

#### 4.2.1 Load-bearing readouts — do not hide these (D-03f)

Three readouts are the pass/fail oracles of the cloud-save device checklist
(`11-HUMAN-UAT.md`, tests 10/11/13). They are listed here so the developer and QA both treat them
as contract rather than decoration. **None may be moved behind a tap, an expander, a scroll
position, an animation, or a later screen** — and, since 2026-09-21, none may be *written* by the
tutorial either: oracles 1 and 3 are exactly what `GameViewModel`'s `Won` path rewrites, which is
why §5.1 forbids the tutorial from going near it.

| # | Oracle | Where it lives after this refresh | What "survives" means |
|---|--------|-----------------------------------|------------------------|
| 1 | **Challenge ladder position** | The Challenge tile's level chip (`challenge_tile_continue`, "Continue — Level N", `HomeScreen.kt:544-558`) — unchanged — **and** additionally the ribbon's Level pill | The tile chip is the oracle and stays exactly as it is. The Level pill is **additive**; if the two ever disagree the tile is right. Both read the same `challengeCurrentLevel`, so they cannot diverge by construction. *(2026-09-28, D-03k: on the **narrow class** the pill stops being merely additive and becomes this oracle's **above-the-fold witness**, because the tile chip is across the fold there — reached by one plain scroll of ≤ 120dp, criterion 15k. The tile chip is still the authority and still unchanged; "cannot diverge by construction" is what makes the witness sound, and it is why the ribbon may not be dropped to buy space.)* |
| 2 | **Classic per-board stars** | The **Classic catalog**, as gold pips under each board preview (`BoardPreview.kt:100-123`, `ClassicBoardUiState.earnedStars`) — this refresh does not touch that screen | Per-board stars have never been on Home and are not being moved there. What must survive is the **path**: the Classic tile remains a single tap from Home to the catalog, and the catalog's pips are untouched. The ribbon's **Stars pill is an aggregate total and is explicitly NOT a replacement for the per-board pips** — do not "simplify" the catalog by pointing at it. |
| 3 | **Continue / resume affordance** | Home band 3, up to two gold chips — behaviour, data and ordering unchanged (`HomeScreen.kt:293-318`) | Visible on first paint without scrolling (first-paint rule above). The added board thumbnail is cosmetic; the chip's presence, its title, its supporting line and its resume target are the oracle. |

**Considered and rejected**, so the trade-off is visible rather than lost:

- *Collapsing the Continue chips into a single "Continue" button that asks which mode.* Rejected:
  it costs a tap, it hides which modes actually have a saved game, and it destroys oracle 3 — the
  device test needs to see *that* a Classic resume and a Challenge resume exist independently,
  which a merged button cannot show.
- *Moving the Challenge level from the tile chip into the ribbon pill only.* Rejected under D-03f:
  the tile chip is the oracle, so the pill is additive and the chip stays.
- *Putting per-board star pips on the Home Classic tile.* Rejected: it needs the same per-board
  fold as the catalog on the cold-start path (D-03g) for information the catalog already shows
  one tap away, and it would create a second place for per-board stars to be wrong.
- *A "recent activity" or "last played" band.* Rejected: it is the Continue chip with worse copy,
  and it needs `completedAt` ordering — a new read on the cold-start path for no new information.

**All four rejections stand, unamended and unreachable** — restated here in 2026-09-28 because
D-03k adds a yield step and the next reader will wonder how far it goes. It goes as far as one
decorative drawing and one line of stacking; it does not reach any of the four. **The second one is
the near miss and is worth spelling out:** D-03k does **not** move the Challenge level "into the
ribbon pill only". The tile chip stays, unchanged, as the authority; what changes on a 360dp-wide
phone is only *where the fold falls relative to it*, and the pill — which has been there since
D-03c and reads the same field — is what keeps the value above that fold. A ruling that deleted the
tile chip and kept the pill would be this rejected option; a ruling that keeps both and scrolls to
one of them is not.

> **What "behind a scroll position" means, and what it does not** *(added 2026-09-28, D-03k —
> narrow by design; it licenses exactly one window class and nothing else)*. This subsection and
> criterion 15d forbid an oracle "behind a tap, an expander, a scroll position, an animation, or a
> later screen", and the stated reason is that `11-HUMAN-UAT.md` tests 10/11/13 read their
> pass/fail off these three, so hiding one makes cloud save **unverifiable on a device**. A tester
> can only be misled by a *hidden* readout: one whose absence and whose collapse look identical,
> or one that cannot be read without changing state. §4.2.3's arithmetic shows a 360×640dp phone
> with both chips cannot seat all five bands at any price this document will pay, so the letter and
> the reason have to be separated — once, explicitly, and not again:
>
> - **Forbidden on every window, unchanged:** a tap, an expander, a "show more", a later screen, an
>   animation that must finish, a state change of any kind, and **any collapsed, folded or
>   truncated region**. A reading that costs the tester a decision is hidden, whatever its cause.
> - **Permitted, on the narrow-and-short class only** (§4.2.3's compact-narrow set): a readout
>   composed at full size with its full label and its full semantics, entirely inside the one
>   scrolling column, reached by a **plain vertical scroll that reveals nothing which was not
>   already composed**. Criterion 15k bounds that scroll at ≤ 120dp — under one mode tile, one
>   flick — and requires the fold to **cut through a band** rather than land between two, so the
>   screen cannot present a false bottom.
> - **Never permitted for oracle 3, on any window.** Both Continue chips stay complete inside the
>   first viewport everywhere. §4.2's first-paint rule already says it in the right words — "A
>   Continue chip that is only reachable by scrolling is a broken oracle, not a layout compromise"
>   — and that sentence is untouched. Oracle 3 is the only one of the three with **no second
>   witness anywhere in the app**: oracle 1 has the Level pill on this same screen reading the same
>   field, and oracle 2 has never been on Home at all. That asymmetry is the whole of the licence
>   above; it is not a general relaxation and it may not be cited as one.

#### 4.2.2 Vertical slack, and the band set a short screen gets (D-03i — added 2026-09-28)

> **Status, before anything else.** This subsection **postdates the build under audit**. §4.2's
> "Thumb zone" paragraph, the first-paint rule, §4.2.1 and criterion 15 are left exactly as they
> read. What is here is the mechanism that replaces the fixed `Spacer` at `HomeScreen.kt:311-313`,
> and it is checked by the new criteria 15g–15j rather than by any criterion the build was measured
> against. A developer picks this up as a follow-up change; a reviewer reads it as what the screen
> is *going* to be, not as a goalpost moved under the diff in front of them.

**Why a fixed spacer cannot be the answer, in numbers.** Every band is a *minimum* height, so the
table is a floor, not a prediction. Available height = what is left inside the `Scaffold`'s content
padding, which is why a tall navigation bar makes a device behave as the shorter device it
effectively is.

> **The table immediately below is superseded in its numbers — 2026-09-28, D-03k.** It is left
> exactly as it read, because it is the arithmetic the audited build was built against and because
> the way it was wrong is the point. It sums band **minimums**; at 360dp width the bands stand well
> above their floors, and the compact total is **≈700dp, not ≈544dp**. The sentence it opens with —
> "every band is a *minimum* height, so the table is a floor, not a prediction" — is the warning
> the totals rows then ignored. **The corrected, wrapped-height arithmetic is the second table,
> below this one, and that is the one to compute against.** Its conclusion about the *full* set is
> unchanged and only strengthened; what it got wrong is the compact set's headroom, and with it
> criterion 15h.

| At default font scale | Full band set | Compact band set (below the threshold) |
|---|---|---|
| Column padding, top + bottom | `spacing_md` ×2 = **32** | `spacing_sm` ×2 = **16** |
| Gaps between children (7 with both chips) | `spacing_md` ×7 = **112** | `spacing_sm` ×7 = **56** |
| 1 Hero | ≥ 160; **≈172 as built** (art 120 + `spacing_sm` + `displaySmall`) | ≥ **72**, no art |
| 2 Ribbon | ≥ 64 | ≥ 64 — it cannot yield (see below) |
| 3 Continue chips, both present | 72 + 72 | 72 + 72 |
| 4 Mode tiles | 96 + 96 | 96 + 96 |
| **Total with both chips, zero slack** | **≈ 716** | **≈ 544** |
| **Total with no chip, zero slack** | **≈ 540** | **≈ 384** |

Against those two totals: a 360×640dp window offers at most 640dp and in practice ~568dp (24dp
status + 48dp navigation). **The full set therefore cannot satisfy criterion 15a on its own
baseline — it is ~148dp over — and no value of a fixed spacer changes that, including zero.** A
6.7-inch phone (412×915dp → ~843dp) has 127dp of slack in the both-chips state and 303dp in the
fresh-install state, and a fixed 24dp spends 24dp of it wherever the state happens to be. That is
the measured 45% dead space, and it is also why the spacer's real cost is **40dp** rather than 24:
as a child of `Arrangement.spacedBy(spacing_md)` it brings a second 16dp gap with it, 16dp of which
expresses no intent at all.

##### The same arithmetic, corrected — wrapped heights at 360dp width (D-03k, 2026-09-28)

Measured by `code-auditor-s9.md` MB-1 on a `w360dp-h640dp` window at default font scale, against
the shipped compact band set. **Sum wrapped heights, never floors.** The error this replaces was
not a rounding slip: it was summing a column of minimums and writing the result as a total.

| Compact band set, both chips | First-edition floor | Measured at 360dp width | Over its floor by |
|---|---|---|---|
| Column padding, top + bottom (`spacing_sm` ×2) | 16 | 16 | — |
| Gaps between children (`spacing_sm` ×7) | 56 | 56 | — |
| 1 Hero, one row, no art | 72 | **72** (title 44 inside it) | — |
| 2 Ribbon | 64 | **116** | **+52** |
| 3 Continue chip — Classic | 72 | **108** | **+36** |
| 3 Continue chip — Challenge | 72 | **108** | **+36** |
| 4 Mode tile — Classic | 96 | **108** | **+12** |
| 4 Mode tile — Challenge (carries oracle 1's level chip) | 96 | **116** | **+20** |
| **Total, both chips, zero slack** | **≈ 544** | **≈ 700** | **+156** |

The band edges are on the record, so the sum is checkable rather than asserted — hero `8..80`,
ribbon `88..204`, a Continue chip `220..328`, a mode tile `452..560`, the Challenge tile `568..684`
(reported as `568..640` unscrolled because `getBoundsInRoot()` clips to the root, and `524..640`
once scrolled into view). Two figures are **derived, not measured**, and are marked so they are not
mistaken for readings: the second Continue chip at `336..444` (the first tile's top, 452, less one
8dp gap and one 108dp chip) and the column's own total of **700** (the last tile's bottom, 684,
plus one 8dp gap, the zero-height band-5 `Spacer` and 8dp of bottom padding). The 16dp between the
ribbon's bottom and the first chip's top is the slack `Spacer` between bands 2 and 3 contributing
its two `Arrangement.spacedBy` gaps at zero height — **the mechanism working exactly as clause 2
specifies**, not a defect.

**The usable height this document designs against: 568dp.** Stated as a number and not as a
qualifier, because the original error is the kind that regenerates the moment the sum is left
loose:

| Window the short class is designed against | Usable height | ≈700dp of content is over by |
|---|---|---|
| **The design baseline** — 360×640dp, 24dp status + 48dp three-button navigation | **568dp** | **132dp** |
| The same phone with a gesture handle instead of a bar (~24dp bottom inset) | ≈592dp | ≈108dp |
| The JVM test window, zero insets (`w360dp-h640dp`) | 640dp | **60dp** — and the Challenge tile's bottom edge sits **44dp** below the fold |

Three consequences, each of which has to be written down or it comes back:

1. **A zero-inset window is a measurement artefact, not a device.** It is the most generous window
   in the table and it is the only one no player holds. Any criterion about the short class is
   measured with the insets supplied — the harness already exists in this repo
   (`TutorialScreenTest.applySystemBarInsets()`) and was not carried across to the Home test where
   the margin is 24dp instead of 96dp. Criterion 15k requires it.
2. **The 116dp figure and the 132dp figure are both right and measure different things.** 684 − 568
   = 116dp is the last band's *bottom edge* against real usable height; 700 − 568 = 132dp adds the
   column's own trailing gap and bottom padding, which is space the column genuinely needs. The
   layout must clear **132dp**; a test that stops at the last band's edge is reporting 116dp.
3. **The ribbon was never unyieldable.** The bullet below claiming it "cannot yield
   arithmetically … ~60dp inside a 64dp minimum" omitted the pill's glyph and one `spacing_xs`,
   and read a *minimum* as a cap. `HomeProgressRibbon.kt:129-163` composes
   `padding(spacing_sm)` + glyph `home_stat_pill_glyph_size` (20dp) + `spacing_xs` + label
   (`labelMedium`) + `spacing_xs` + value (`titleMedium`) + `padding(spacing_sm)` — **44dp of
   chrome over two text lines, ≥84dp at default font scale before any wrap**, inside a band
   specified at 64dp. That 24dp omission is provable by reading the file, not by measuring it, and
   it is where the ribbon's +52dp starts. §4.2.3 spends it.

**The full set, recomputed at the same width, for completeness.** Hero 172 (as built) + ribbon 116
+ chips 108 + 108 + tiles 108 + 116 = 728 fixed, plus `spacing_md` ×7 = 112 and `spacing_md` ×2 =
32 → **≈872dp** against 568dp usable. The first edition's "the full set is ~148dp over" was
therefore itself generous: it is **~304dp** over. The conclusion is unchanged and only harder —
the full set cannot serve a 360dp-wide phone of *any* height that ships, which is why D-03k makes
the threshold read width (§4.2.3).

**The mechanism.** One `BoxWithConstraints` and two empty `Spacer`s.

1. **`HomeBands`' root becomes a `BoxWithConstraints`** filling the screen `Box` that already
   applies `padding(contentPadding)` (`HomeScreen.kt:142-148`) — **`fillMaxSize()` belongs here, and
   only here**, which is also where the one removed from the column chain goes. Its `maxHeight` is
   "the available height" everywhere in this subsection, so the threshold and the slack are both
   measured **after** the system-bar insets the `Scaffold` has already subtracted. `HomeBlobs` stays
   its **sibling**, behind it and outside the band column, exactly as today (`HomeScreen.kt:149`).
2. **The band column's modifier order is part of the contract**, in this order:
   `align(Alignment.TopCenter)` → `verticalScroll(rememberScrollState())` →
   `heightIn(min = <the available height>)` → `widthIn(max = content_max_width)` →
   `padding(<the set's spacing>)`.
   - `heightIn(min = …)` **is the whole mechanism.** `verticalScroll` measures its content with an
     unbounded maximum height, and a `Column` distributes weight over its maximum height only while
     that is finite — over its **minimum** when it is not. Setting the minimum to the available
     height therefore hands the weighted children exactly the slack between the viewport and the
     fixed bands, and hands them **nothing** once the fixed bands exceed it, which is precisely when
     the column must scroll instead. No other modifier in the chain may set a height.
     The **behaviour** is the contract and the mechanism is how it is reached: slack distributed
     3 : 1 when there is slack, a scrolling column when there is not. If the pinned Compose BOM
     turns out not to distribute this way, the finding goes back to the producer with the
     measurement — **the fixed `Spacer` is not the fallback**, and neither is weighting a content
     band (clause 4).
     *(Pointer 2026-10-21 — D-03n, §4.2.5.)* "Slack distributed 3 : 1 when there is slack" is no
     longer the behaviour. The contract is now §4.2.5's rule: the footer is one fifth of the no-chip
     slack, fixed for the window, and the stage is the rest less the chips' room; still a scrolling
     column when there is no slack. The modifier order above and "no other modifier may set a height"
     stand; how the two heights are produced is the developer's.
   - **`fillMaxSize()` comes out of that chain and does not come back.** Ahead of `verticalScroll`
     it fixes the content's width to the window's, which makes the `widthIn(max = …)` that follows
     it a no-op and leaves the column hugging the leading edge on any window wider than 480dp —
     so §12's "capped at `content_max_width` and centred" row is not true of the audited build
     (`HomeScreen.kt:296-303`). Restoring the cap and the centring is part of this ruling because
     it is the same chain; criterion 15j checks it.
3. **Two weighted `Spacer`s, and only those two.** One between bands 2 and 3 — where §4.2 always
   put the slack, so the tiles stay low — and one **as band 5**: the footer stops being a comment
   (`HomeScreen.kt:352-353`) and becomes the slack it was reserved for, still empty of content. The
   ratio is **3 : 1** (above : below), named as two constants in the home package beside
   `CONTENT_WEIGHT` (`HomeScreen.kt:611-614`) and never by reusing `CONTENT_WEIGHT` — that file's
   own convention is that a shared literal is a place for two owners to disagree. 3 : 1 keeps the
   first mode tile's **top** edge at or below the midpoint in every state on every device at or
   above the threshold (worst case, fresh install at exactly 720dp: tile top 405dp against a 360dp
   midpoint) while leaving the tiles a visible margin above the navigation bar instead of sitting
   on it.
   *(Amended 2026-10-21 — **D-03n**, §4.2.5.)* The 3 : 1 split is replaced. The lower share, the
   footer, is **one fifth of the slack the column has with no Continue chip**, fixed for the window.
   The upper share, the stage since D-03l, is the rest less the chips' room. A split of the *current*
   slack moved the tiles every time a chip arrived; a footer fixed for the window does not. The
   midpoint property above still holds, with the tiles lower than before. The two weights become one
   ratio in a scoped constants object (§6.4), and how the stage and the footer receive their heights is
   the developer's: the behaviour is the contract, as clause 2 says.
4. **No content band may be weighted.** A weighted child with `fill = true` is measured at exactly
   its share, so a weighted hero would be measured at exactly **0** the moment the fixed bands
   exceed the viewport — clipped to nothing at 200% font scale, which criterion 14 forbids. This is
   why the hero does not simply grow to absorb the slack, and it is not available as a later
   "simplification". Empty `Spacer`s are the only safe weight holders on this screen.
5. **The spacers are not bands.** No `clayEntrance`, no `claySquish`, no semantics, no test tag,
   no background. §10's entrance cascade is unchanged and the chips and tiles keep the orders they
   are counted into today (`HomeScreen.kt:315-351`).
6. **The slack never goes between bands 3 and 4, or inside band 4.** The Continue chips and the two
   mode tiles stay one contiguous group; a gap opening between a Continue chip and the tiles would
   read as two unrelated regions and would push band 4 off a short screen again.

**What yields on a short screen, in order.** Below `home_compact_threshold_height` of available
height, Home takes the **compact band set** — all three of these together, never partially:

1. **The hero's decorative art is not composed at all** (`HomeHeroArt`, `home_hero_art_size`).
   §6.5's degradation path already ranks this art as the cheapest thing on Home to lose — step 3 of
   4 — so spending it for space is the budget the spec already set, merely spent on a different
   scarcity. Q-901's answered option 1 is unaffected: the art is what a screen with room for it
   shows.
2. **The hero's minimum becomes `home_hero_compact_min_height` (72dp) and the band becomes one
   row**: the `ctaGradientBrush()` `displaySmall` title, then the 48dp Settings gear chip at the
   trailing edge, `CenterVertically`. The title is **never truncated and never scaled down** — if
   the font scale makes it wrap, the band grows and the column scrolls (criterion 14). **This is
   not a top bar**: no surface, no elevation, no window insets of its own, it scrolls with the
   content and the title keeps display scale. D-03a deleted Home's bar and §5's "no M3
   `TopAppBar` anywhere" is untouched; what the compact hero shares with the deleted bar is one
   row's geometry, which is not a component.
3. **Band spacing and the column's padding drop from `spacing_md` to `spacing_sm`** — the 64dp that
   closes the remaining gap.

> *(Amended 2026-09-28 — D-03k, §4.2.3.)* **All three still go together and none is now optional.**
> What changes is the count and the last clause of the sentence above them: on the **narrow** class
> there is a **fourth** step, taken with the other three and never partially — the progress pill
> drops its `contentDescription = null` glyph and puts its label beside its value on one line
> (§4.2.3 part 2). And "the 64dp that closes the remaining gap" was the sentence that carried the
> arithmetic error: at 360dp width it closes 64dp of a **132dp** gap, which is why there is a
> fourth step at all and why, after it, the screen still scrolls by ≈72dp and criterion 15k says so.
>
> *(Amended 2026-09-28 — founder-feedback amendment, D-03l, §4.2.4.)* Step 1 is now **vacuous**:
> there is no hero art on any window, and the stage that replaced it draws only into slack, so it
> cannot cost a short screen a dp. Step 2's one-row hero is now what **every** window gets. Steps 3
> and 4 — the tighter spacing and, on the narrow class, the compact pill — are what "the compact band
> set" still means, and the thresholds that switch them are retained at their values (§4.2.4).

The threshold is **720dp**: the full set's own both-chips requirement (≈716dp) rounded up onto the
grid. Its consequence, stated rather than discovered: a device around 360×780dp takes the compact
set even though the full set would fit its *fresh-install* state. That is deliberate — the
alternative is a screen whose hero changes shape the first time the player leaves a game unfinished,
and a layout that reshuffles when a Continue chip appears is worse than one that is stable per
device. For the same reason the threshold reads **available height only and never the font scale**:
at 200% on a tall phone the set stays full and the column scrolls (criterion 15i).

> *(Amended 2026-09-28 — D-03k, §4.2.3.)* **"Never the font scale" stands. "Height only" does
> not.** The threshold's *derivation* is kept — it is still "the full set's own both-chips
> requirement at this width, rounded onto the grid" — but that requirement is **≈872dp at 360dp
> width** and ≈716dp only at the wider class, because the bands wrap. So the threshold becomes a
> function of **both** `BoxWithConstraints` dimensions: below `home_compact_threshold_width`
> (400dp) the height threshold is `home_compact_threshold_height_narrow` (880dp), and at or above
> it the 720dp above is unchanged. The font scale is still never read, for the reason given here.
> The stated consequence gains a blunter one: **at 360dp width Home is always compact**, which is
> deliberate and is argued in §4.2.3.
>
> *(Pointer 2026-10-21 — D-03n, §4.2.5.)* "A layout that reshuffles when a Continue chip appears is
> worse than one that is stable per device" was true of the band *set* and false of the moment of
> arrival: under the 3 : 1 split every band below the hero moved when the chips landed, ≈0.6s after
> first paint on every cold start (QA loop 3, F-1). D-03n makes it true at that moment too. The
> sentence stands as it reads.

**What never yields**, on any screen size, because each is a readout, a target, or a route:

- The **Settings gear**. It is the only route to Settings, and since D-02a Settings is the only
  permanent route to the tutorial; dropping it to save 48dp would strand How to play and the sound
  settings behind nothing.
- The **ribbon**: three pills, their labels and their semantics. It also cannot yield arithmetically
  — value (`titleMedium`) + `spacing_xs` + label (`labelMedium`) + `spacing_sm` padding is ~60dp
  inside a 64dp minimum, so a smaller token would buy 4dp and lie about the layout.
  > *(Amended 2026-09-28 — D-03k, §4.2.3.)* **The first clause stands; the second was wrong.**
  > Three pills, their labels, their values and their one-sentence semantics still never yield —
  > and on the narrow class the Level pill becomes load-bearing rather than additive, because it is
  > the ladder position's above-the-fold witness once the tile chip is below the fold (§4.2.1's
  > 2026-09-28 note). What was wrong is the arithmetic: the sum omitted the pill's 20dp glyph and
  > one `spacing_xs`, and treated a 64dp *minimum* as a cap on an ≥84dp content column
  > (`HomeProgressRibbon.kt:129-163`). The band has ~44dp of **decoration and stacking** in it, and
  > §4.2.3 spends exactly that: the `contentDescription = null` glyph and the third line. No pill,
  > no label, no value and no semantics node is touched, so nothing in the first clause moves.
- The **Continue chips**: both, independent, 72dp, present whenever a snapshot exists (§4.2.1
  oracle 3).
- The **mode tiles**: 96dp each (§11's tile floor), and the Challenge tile's level chip (oracle 1).
- Any **touch target**: ≥ 48dp with ≥ 8dp spacing (§11).
- **None of §4.2.1's four rejected simplifications may be reached for here.** The compact set does
  not merge the two Continue chips into one button, does not move the ladder level off the tile chip
  into the pill, does not put per-board pips on Home, and does not add a recent-activity band. It
  removes one decorative drawing and 64dp of whitespace; it removes no readout, no control and no
  band, and it collapses nothing behind a tap (criterion 15d).

**If the compact set still does not fit** on a real 360×640dp device with both chips at default
font scale — the headroom is ~24dp on the worst inset case and every number above is a minimum —
**stop and report the measured numbers to the producer.** The next candidate to yield is a readout
or a target, and that is a design change I will make against measured numbers rather than guess at
here.

> **This branch was taken, and it is answered — 2026-09-28, D-03k, §4.2.3.** The paragraph above
> stands as the instruction and it was the right instruction; the reviewer reported the numbers
> (`code-auditor-s9.md` MB-1) instead of a green test being accepted, and the ruling is made here
> rather than deferred a second time. Two of its own figures were wrong and the correction matters:
> the headroom is **not ~24dp**, it is **−132dp** against the design baseline, and the next
> candidate to yield turned out **not** to be "a readout or a target" — those were the only bands
> left *after* one more yield that costs neither. §4.2.3 spends the ribbon's decoration and its
> third line, and then rules that the remaining 72dp cannot be found: on the corrected arithmetic
> every dp of it is the hero's 48dp gear and gradient title, oracle 3's two Continue chips, and
> §11's two 96dp tile floors carrying oracle 1's level chip. So **the screen scrolls and the
> criterion says so** — criterion 15h is retired as false and 15k replaces it. Nothing in the
> mechanism above changes.

> *(Corrected 2026-09-29 — **D-03m**.)* "The remaining 72dp" above, and "still scrolls by ≈72dp" in
> the D-03k note on the compact steps, both count 24dp that was the hero row's floor above its 48dp
> gear, not a readout, a target or a route. With the floor retired, the Robolectric overflow is
> ≈48dp and the screen still scrolls (§4.2.4's last subsection).

**Cold start (D-03g, criterion 15e).** The mechanism adds no read, no image decode, no asset load
and no SDK call; its cost is one `SubcomposeLayout` for the band column and two zero-content
`Spacer`s, and the compact set *removes* one `BoardCanvas` draw on the devices that take it. It is
still a change to the start destination's layout, so **15e's two-arm measurement is taken on the
branch that lands it**, by §6.5's procedure verbatim: a cold-start number measured against a layout
that has since changed is not evidence.

**What this owes in tests** (deterministic, JVM, no device — the harness is the developer's and the
qa-engineer's; the properties are this document's):

- At a tall-phone window qualifier, the **top** edge of the first mode tile is at or below half the
  Home content height — asserted in both the no-chip and the both-chips state (criterion 15g).
- At a 360×640dp qualifier with both chips at default font scale, each Continue chip and both mode
  tiles are **fully inside** the viewport, and the hero art is absent (criterion 15h).
  > *(Superseded 2026-09-28 — D-03k.)* The hero-art half stands. The "both mode tiles fully
  > inside" half is **not achievable** (§4.2.2's corrected arithmetic, §4.2.3) and criterion 15h is
  > retired; **§4.2.3's owed-tests list replaces this line**, and it also forbids the two idioms
  > this one was written with — a containment assertion that `getBoundsInRoot()`'s clipping makes a
  > tautology, and a zero-inset window (§13, "Two test idioms this refresh now forbids").
- At the same qualifier with font scale 2.0, the column scrolls and every band is reachable, with no
  band dropped (criteria 14, 15i).
- Neither spacer is a clickable or focusable node, and the content column is capped at 480dp and
  centred on a window wider than that (criterion 15j).

#### 4.2.3 The narrow class: what yields last, and the criterion that tells the truth (D-03k — added 2026-09-28)

> **Status, before anything else.** This subsection **postdates the build audited on 2026-09-28**
> and applies to the slice that lands D-03k — the slice *after* that one. §4.2.2's mechanism is
> **untouched and still binds** in every clause. §4.2's "Thumb zone" paragraph, the first-paint
> rule, §4.2.1's oracle table and its four rejections, and criteria 14, 15, 15a, 15d and 15g–15j
> are all left exactly as they read. What is new is one yield step, a width term in the threshold,
> and the replacement of **criterion 15h**, which this subsection's arithmetic proves false rather
> than asserting it. **Retiring 15h does not clear the reviewer's MB-1**: an assertion incapable of
> failing is a defect on its own terms, and the criterion it certifies changing underneath it does
> not make it sound.

**The finding, in one line.** After §4.2.2's three compact steps the band set measures **≈700dp**
at 360dp width and the design baseline offers **568dp** (§4.2.2's corrected tables). The gap is
**132dp**.

**Part 1 — the threshold reads width as well as height.** The ≈156dp error was **wrap at narrow
width, not height**, so a threshold that reads height alone is measuring the wrong axis. The
derivation is kept and applied twice:

| Available width (`BoxWithConstraints.maxWidth`) | Compact below | Token | Why that number |
|---|---|---|---|
| < 400dp | **880dp** | `home_compact_threshold_height_narrow` | The full set's own both-chips height **at 360dp width** is ≈872dp (§4.2.2), rounded up onto the 8dp grid |
| ≥ 400dp | 720dp — unchanged | `home_compact_threshold_height` | ≈716dp at that width, as D-03i derived it |

400dp is the boundary because 360dp is the narrowest supported window and the next common width is
the 412dp class; **no shipping device sits between them**, so the boundary is not a judgement about
a real phone. The font scale is still never read (§4.2.2's reason is unchanged): at 200% a tall
phone keeps the full set and scrolls (criterion 15i).

**The consequence, stated rather than discovered — at 360dp width Home is always compact.** No
shipping 360dp-wide phone is 880dp tall, so the narrow branch is the only branch a 360dp phone
takes. That is the intent, not an accident: the bands wrap at 360dp whatever the height, so a
360×800dp phone needs the compact set exactly as much as a 360×640dp one — the full set is ≈872dp
against its ~728dp of usable height (**144dp over, scrolling**) while the compact set is ≈640dp
(**fits, with ~88dp of slack**). At or above 400dp of width **nothing changes at all**: the 720dp
threshold, the hero's decorative art, the 3 : 1 slack and everything criterion 15g measures are as
D-03i left them. Reading `maxWidth` costs nothing — it is a second read of a value the
`BoxWithConstraints` D-03i already specified has measured, not a second measuring pass.
*(Pointer 2026-10-21 — D-03n, §4.2.5.)* "The 3 : 1 slack" here is superseded on every width: the
footer is one fifth of the no-chip slack, fixed for the window, and the stage is the rest less the
chips' room. Everything else in this paragraph stands.

**Part 2 — the compact-narrow ribbon. The last yield, and it costs no readout.** Below
`home_compact_threshold_width`, and only there, each progress pill takes the **compact pill**:

1. **The pill's glyph is not composed at all.** It is `contentDescription = null` decoration inside
   a `clearAndSetSemantics` node (`HomeProgressRibbon.kt:136`, `:143-151`) — it carries **no
   information whatsoever**, which puts it in the same class as the hero's decorative art that
   §6.5's degradation path already ranks as the cheapest thing on Home to lose. 20dp, plus the
   `spacing_xs` beneath it.
2. **The label and the value share one line**, not two: label (`labelMedium`, `onSurfaceVariant`),
   `spacing_xs`, value (`titleMedium` with `withTabularFigures()`, `onSurface`), `CenterVertically`.
   **Neither is truncated and neither is scaled down** — if the pair does not fit the pill's width,
   the label wraps, the pill grows, the band grows and the column scrolls, exactly as criterion 14
   requires. The floor is `home_stat_pill_compact_min_height` (**40dp**: `spacing_sm` + one
   `titleMedium` line + `spacing_sm`) and the design target for the whole band at 360dp width at
   default font scale is **≤ 56dp**, which criterion 15m measures rather than assumes.
   *(Amended 2026-10-21 — **D-03o**, §4.2.6.)* "The label wraps" is superseded. No label or value is
   ever broken inside a word or a number, and once any label no longer fits in half its pill's
   content width, all three pills take the stacked shape at one height. The one-line shape, the
   floor and the ≤ 56dp target at default font scale stand.
3. **Nothing else about the ribbon changes.** Three pills, three labels, three values, equal
   `CONTENT_WEIGHT` widths, `spacing_sm` between them, `surfaceContainerHigh` +
   `clayShadow(elevation_card)`, `clayEntrance(RIBBON_ENTRANCE_ORDER)` on the `Row` so §10's
   cascade is untouched, not tappable, not focusable, and **one** `clearAndSetSemantics` node per
   pill speaking `home_stat_pill_desc` as "label, value". The em-dash placeholder rule (D-03c,
   D-03h) is unchanged: unknown and failed both render `—` and neither ever renders `0`.
   **No new string.** TalkBack cannot tell the two pill shapes apart, and that is the test that the
   yield costs no readout.

It also moves cold start the right way: three fewer vector `Icon` draws on the start destination,
no new read, no decode, no asset. D-03g is unaffected and §6.5's budget is not spent.

**Part 3 — and then it still does not fit, so the criterion changes.** With the ribbon at its 56dp
target the compact-narrow set is:

| | dp |
|---|---|
| Hero row — gradient title + 48dp Settings gear | 72 |
| Ribbon — compact pills | 56 |
| Continue chips ×2 | 216 |
| Mode tiles ×2 (Classic 108, Challenge 116) | 224 |
| Gaps (`spacing_sm` ×7) + column padding (×2) | 72 |
| **Total** | **≈ 640** |
| **Usable height, design baseline** | **568** |
| **Over by** | **72** |

**Every one of those 72dp is a readout, a target or a route, and this is the whole argument:**

| Band | May it yield? |
|---|---|
| Hero row, 72 | **No.** The gear is §11's 48dp floor and the only route to Settings and therefore to How to play (D-02a); the title is never truncated and never scaled (§4.2.2 step 2). |
| Ribbon, 56 | **Already yielded** — everything in it that is not a value, a label or a semantics node is gone. |
| Continue chips, 216 | **No.** Oracle 3, and the only oracle with no second witness anywhere in the app (§4.2.1's 2026-09-28 note). |
| Mode tiles, 224 | **No.** §11's 96dp tile floor twice over, and the Challenge tile carries oracle 1's level chip. |
| Gaps + padding, 72 | **No.** `spacing_sm` is already the smaller grid step; `spacing_xs` between bands would read as one undifferentiated slab and would breach §11's ≥ 8dp separation between targets. |

> *(Amended 2026-09-29 — **D-03m**, §4.2.4.)* The two "Hero row, 72" rows above were wrong by 24dp,
> in exactly the way this subsection warns about: 72 was the row's **floor**, not its content. The
> 24dp above its tallest child, the 48dp gear, was not a readout, a target or a route. D-03m retires
> the floor, so the row is 48dp. The compact-narrow set becomes ≈616dp against 568dp, so it is **48dp
> over, not 72**. The fold still cuts through the Challenge tile (`484..600`). Criterion 15k is
> unchanged and holds with more margin, and "a 360×640dp phone with both Continue chips scrolls" is
> still true on this Robolectric arithmetic. Everything else in the table stands.

Note where 640 lands: **within a few dp of the 640dp zero-inset JVM window.** That is the second
and sharper reason 15k must be measured with insets — at these numbers a zero-inset test reads
"fits" on precisely the window where the device does not.

**So: a 360×640dp phone with both Continue chips scrolls.** Criterion **15h is retired as false**
and **15k** replaces it, requiring what is achievable and measurable: the hero row, the whole
ribbon, **both** Continue chips and the **first** mode tile complete inside the first viewport; the
**second** mode tile **breaking the fold**, so the screen has no false bottom; and the rest reached
by one plain vertical scroll of **≤ 120dp** — under one tile, one flick — with no tap, no expander
and no state change. Where the fold falls, derived from the table above at 568dp of available
height and to be confirmed by 15k's own measurement: hero `8..80` · ribbon `88..144` · slack
`Spacer` (0) · chip `160..268` · chip `276..384` · tile `392..500` · tile `508..624`. The fold at
568 cuts through the Challenge tile with ~60 of its 116dp showing. **A fold through a band is the
strongest "there is more below" signal a scrolling screen has**, which is why 15k requires it
rather than merely permitting it: a fold landing in a gap is a false bottom, and a false bottom is
how a readout becomes hidden without anyone collapsing it. (15k states that part **conditionally on
the column overflowing**, so a taller window where everything fits passes it trivially instead of
failing for fitting. The requirement bites where the overflow is, which is the narrow-and-short
class.) The position is robust to the ribbon landing anywhere in its 40–72dp range: at 40dp the
tile sits `492..608`, at 72dp `524..640`, and the fold at 568 cuts through it in every case.

*(Pointer 2026-10-21.)* "A 360×640dp phone with both Continue chips scrolls" is true on the LEGACY
arithmetic above and not on real metrics. On Robolectric NATIVE in the app theme, and on the device,
that window overflows 0dp and does not scroll; the overflow branch is read on `w360dp-h560dp`
(criterion 15k part 2's 2026-10-21 note). 15k and everything it requires stand.

**Why the ribbon is not simply dropped**, since it saves 56–116dp and would make the arithmetic
close (≈560dp against 568dp). Rejected three times over, recorded so it is not re-proposed as a
"simplification":

1. **8dp is not a margin.** It is inside the variance between Robolectric's text metrics and
   glass, between a 48dp navigation bar and a ~24dp gesture handle, and above any font scale over
   1.0. A criterion that passes by 8dp on the JVM and fails on the device is the same false green
   MB-1 blocked, and this document will not write a second one.
2. **It would make criterion 11 false** — "a titled hero band, three progress pills and board art
   on the mode tiles", with no screen-size qualifier — on exactly the phones whose Home looks
   emptiest, un-answering the founder's ask where it was loudest. (Criterion 11 **is** met by the
   compact-narrow set as specified: the hero band is still titled, the three pills are still there,
   and the mode tiles' own `home_tile_art_size` art is **never** part of any yield.)
3. **It is the one change that would genuinely breach D-03f.** §4.2.1 records that the Level pill
   and the Challenge tile's chip "read the same `challengeCurrentLevel`, so they cannot diverge by
   construction" — which makes the Level pill the ladder position's **above-the-fold witness** once
   the tile chip is below the fold. The ribbon is not the band that disappears; it is the band that
   makes the scroll safe.

**What this owes in tests** (deterministic, JVM, no device — the harness is the developer's and the
qa-engineer's; the properties are this document's). Every one of these must be watched fail before
it is trusted, which is the lesson of MB-1:

- At `w360dp-h640dp` **with the device's system-bar insets supplied** — the
  `TutorialScreenTest.applySystemBarInsets()` idiom already in this repo — the hero row, the
  ribbon, both Continue chips and the first mode tile are complete in the first viewport, and the
  second mode tile's top edge is **above** the fold while its bottom edge is **below** it
  (criterion 15k, parts 1–2). "Complete in the viewport" is asserted as **"a scroll does not move
  this node"**, never as `bottom ≤ contentBottom()`, which `getBoundsInRoot()`'s clipping makes a
  tautology.
- The column's total scrollable overflow at that window is **≤ 120dp** (criterion 15k part 3).
- Three windows fix the threshold's width term: `w360dp-h800dp` compact, `w360dp-h640dp` compact,
  `w412dp-h915dp` full with the hero art present (criterion 15l).
- The compact pill: no glyph node, label and value present, the band ≤ 56dp at default scale, and
  the pill's spoken description **byte-identical** to the full set's (criterion 15m).
- At the same qualifier at font scale 2.0 nothing above changes in kind: the column scrolls, every
  band is reachable, no band is dropped and nothing is clipped (criteria 14, 15i — unchanged).

#### 4.2.4 The hero, re-seated: one row and a stage (D-03l — founder-feedback amendment, 2026-09-28)

> **Status.** This subsection answers the founder's review of the phase-15 build (Q-004) and
> applies to the slice that lands D-03l. It does not retro-certify any earlier build. §4.2.2's
> mechanism — the `BoxWithConstraints`, the modifier order, `heightIn(min = available)`, the 3 : 1
> ratio, clause 6 — and §4.2.3's narrow class and criterion 15k are **untouched**; clauses 3, 4 and
> 5 of §4.2.2 gain one exception each, stated below. The ribbon, the Continue chips, the mode tiles,
> the palette and the blobs are unchanged in design. The ribbon's **position** changes, and that is
> the only consequence of the hero change for any band the founder asked to keep.
>
> *(Pointer 2026-10-21 — D-03n, §4.2.5.)* "The 3 : 1 ratio … untouched" no longer holds: the footer
> is one fifth of the no-chip slack, fixed for the window, and the stage is the rest less the chips'
> room. Wherever this subsection says the stage is weight 3 or three quarters of the slack, and the
> footer weight 1 or a quarter, read §4.2.5. Everything else here stands: the hero row, the stage, its
> art, its tokens, its position and the clause exceptions.

**What the founder saw, measured off `home-founder.png`** (1080×2424px, a 411×923dp phone, one
Classic Continue chip): the peg cluster (120dp) above a centred title; the ribbon; then **≈172dp of
blank** between the ribbon's bottom edge and the Continue chip's top edge — nearly a fifth of the
screen — and the remaining quarter-share of slack (≈57dp) below the Challenge tile. The screen reads
as two islands with a hole between them, and the title belongs to neither.

**The column after D-03l**, top to bottom, inside §4.2.2's unchanged `BoxWithConstraints` and
scroll column (band spacing and padding `spacing_md` in the full set, `spacing_sm` in the compact
set, exactly as before):

| # | Child of the column | Height | Notes |
|---|---|---|---|
| 1 | **Hero row** — gradient title (start) + Settings gear chip (end) | `home_hero_row_min_height` (72dp) minimum; grows if the title wraps | Every window, both band sets. Replaces `FullHero`, `CompactHero` and `HomeHeroArt`. |
| 1s | **Stage** — the weighted slot, weight **3** | its share of the slack; **0** when the column overflows | Decoration only. Draws the stage art when its height is ≥ `home_stage_art_min_size`. The constant D-03i named for "the slack above the chips" is renamed for its new owner; its value is unchanged. |
| 2 | Progress ribbon | unchanged | Now directly above the Continue chips. |
| 3 | Continue chips (0, 1 or 2) | unchanged | |
| 4 | Mode tiles | unchanged | |
| 5 | Footer — weighted `Spacer`, weight **1** | its share | Unchanged (D-03i). |

The stage *replaces* the upper `Spacer` as a child, so the column has the same number of children
and the same number of `spacedBy` gaps as before in every chip state.

*(Amended 2026-10-21 — **D-03n**, §4.2.5. Rows 1s and 5's Height cells are superseded. The footer is
one fifth of the slack the column has with **no** Continue chip, fixed for the window. The stage is
the slack less the chips' room less that footer, and 0 when that is negative. Both stay empty of
anything a player can tap or hear, and the stage's art rule below is unchanged. The ¾ / ¼ columns of
the two arithmetic tables in this subsection are the record of the split this replaces; §4.2.5 has the
new figures.)*

*(Amended 2026-09-29 — **D-03m**. Row 1's Height cell is superseded. The hero row has **no minimum
height of its own**. It is exactly as tall as its tallest child: the 48dp gear at default font scale,
or the title when it is taller. `home_hero_row_min_height` is retired. See the last subsection of
§4.2.4.)*

**The hero row.**

- One `Row`, `CenterVertically`, `fillMaxWidth()`, `heightIn(min = home_hero_row_min_height)`,
  entering with `clayEntrance(HERO_ENTRANCE_ORDER)` as today.
- **Title:** `app_name` in `displaySmall` (Fredoka SemiBold, 36sp) filled with
  `ctaGradientBrush()`; **start-aligned**; `weight(1f)` so it takes the width the gear leaves;
  `semantics { heading() }`; **no `maxLines`** — never truncated, never scaled down; at a large
  font scale it wraps, the row grows and the column scrolls (criteria 14, 15i). One composable for
  every band set.
- **Gear:** the existing clay chip, unchanged in every respect — `Icons.Rounded.Settings`,
  `settings_action`, 48dp, `surfaceContainerHigh`, `clayShadow(elevation_pressed)`, silent.
- **The row is the first thing in the column.** Nothing is drawn above the title except the
  column's top padding and the ambient `HomeBlobs` behind everything.
- **It is not a top bar** — §4.2.2 step 2's paragraph applies verbatim: no surface, no elevation,
  no insets of its own, it scrolls with the content, and §5's "no M3 `TopAppBar`" is untouched.
- **Why start-aligned and not centred.** Centring was the logo's composition: a centred emblem
  wants a centred word under it. Without the emblem, a centred title floats between the screen's
  edge and the ribbon, which is the "how the title sits" complaint. Start-aligned in the gear's row
  it reads as the screen's heading, in the place the app's other screen titles sit, and it is the
  geometry the 360dp-wide class has shown since D-03i, so the two width classes stop looking like
  two different apps.

*(Amended 2026-09-29 — **D-03m**. In the first bullet, `heightIn(min = home_hero_row_min_height)`
is withdrawn, and the 72dp goes with it. Every other bullet above stands word for word.)*

**The stage.**

- A direct child of the column between the hero row and the ribbon: `Modifier.weight(3f)` through
  the renamed constant, `fillMaxWidth()`. **Nothing about it is tappable or focusable, and it
  announces nothing**; its only semantics is a test tag, `HOME_STAGE_TEST_TAG`, which TalkBack does
  not read.
- **The stage art:** a square of edge `e = min(stage height, the column's content width,
  home_stage_art_max_size)`, centred horizontally and vertically in the stage. It is composed
  **only if `e ≥ home_stage_art_min_size`**; below that, nothing is composed and the stage is plain
  slack.
- **What the art is:** the gameplay board slab in miniature — a `surfaceContainerHigh` square,
  `shapes.extraLarge`, `clayShadow(elevation_card)` — holding a `BoardCanvas` with
  `interactive = false`, inset by `e × HomeStageMetrics.SLAB_PADDING_FRACTION` (**0.06**, a
  unitless ratio, so a scoped constants object and not `dimens.xml`, §6.4's convention). **Not
  through `BoardPreview`**, which forces `catalog_preview_size` 96dp whatever size it is asked for
  (D-01a).
- **The position on it:** **English-33, mid-game** — a constant peg set of **12–16 pegs**,
  **reachable from the English start by legal play** (criterion 15q proves it by replaying a
  recorded move list in a unit test; nothing is replayed at start-up). No selected peg, no
  legal-target dots, no hint glow, so nothing on it pulses or pops and it is **static on every
  frame** with animations on or off (§10). It never depends on the player's data — it is not their
  saved board and not their progress — so it cannot contradict a readout and costs no read on first
  paint (D-03g). The developer picks the position; a scattered mid-game look reads better than a
  near-win, and a near-win is not required.
- `contentDescription = null`, no semantics node of its own (§11: decorative).
- **Entrance:** the art takes `clayEntrance(HERO_ENTRANCE_ORDER)`, so it arrives with the title
  instead of appearing unanimated while everything below it slides in; with animations off it is
  simply there. It adds no entrance order of its own, so §10's cascade and every chip's and tile's
  counted order are unchanged.

*(Amended 2026-10-21 — **D-03n**, §4.2.5.)* In the first bullet, "`Modifier.weight(3f)` through the
renamed constant" is superseded: the stage's height is max(0, ⅘ S₀ − R), and `STAGE_WEIGHT = 3f` is
not to be rebuilt. "Static on every frame" in the fourth bullet means the board on the slab: no peg
moves on it. When a Continue chip arrives or leaves while Home is drawn, the art's edge follows the
stage's height through the transition (§4.2.5, "The motion"). Every other bullet stands.

**The three clause exceptions, stated narrowly.**

- **Clause 3 (where the weight goes).** The upper weight moves from "between bands 2 and 3" to
  "between the hero row and band 2". The tiles stay low for the reason they always did — the whole
  upper share of the slack is above them — and the ribbon now joins the lower group, which is where
  it reads best: "your progress" directly above "continue" and "play". **No slack may sit between
  the ribbon and the Continue chips any more** — that is where the founder's hole was. Clause 6
  (never between bands 3 and 4, or inside band 4) is unchanged, so the ribbon, the chips and the
  tiles are one contiguous group.
- **Clause 4 (no content band may be weighted).** Its reason is that a weighted child is measured
  at its share, and therefore at **0** the moment the fixed bands exceed the viewport — fatal for a
  readout, which criterion 14 forbids clipping. The stage is the one child for which 0 is the
  correct answer: it carries no information and no target, so on an overflowing column it vanishes,
  exactly as the compact set already made the hero art vanish. The exception is **exactly one**
  weighted child, this one, and what it holds must be decorative, `contentDescription = null`,
  non-interactive and non-focusable. Every other sentence of clause 4 stands: no readout, no
  control and no band a player reads may be weighted, and the hero **row** is not weighted.
- **Clause 5 (spacers are not bands).** The footer `Spacer` is exactly as specified. The stage is
  not a band for §10's cascade either; its art borrows the hero's order (above).

**What this does to the compact set's rules, recorded so nobody re-implements them.** §4.2.2's
step 1 ("the hero's decorative art is not composed at all") has nothing left to act on — there is
no hero art on any window, and the stage draws only into slack, so it cannot cost a short screen a
single dp. §4.2.2's step 2 (one row, 72dp minimum) is now what **every** window gets. And the
compact thresholds (`home_compact_threshold_height` 720dp, `home_compact_threshold_height_narrow`
880dp, `home_compact_threshold_width` 400dp) are **retained at their values although their
derivation assumed the 172dp full hero**: the full set is now ≈100dp shorter, so the thresholds are
conservative, and the only things they still switch are the band spacing and, on the narrow class,
the pill shape. Re-deriving them would move which phones get the tighter spacing, which no founder
note asked for; it is a later tuning decision and changes no criterion. Criterion 15l's three
windows still hold, with "the hero's decorative art present" read as "the stage art present".

*(Amended 2026-09-29 — **D-03m**. "One row, 72dp minimum" in the paragraph above now reads "one row,
exactly as tall as its tallest child": the row has no floor on any window. The thresholds are still
retained at their values; the full set is now a further 24dp shorter than their derivation assumed,
so they are more conservative still.)*

**The arithmetic, on the 6.7-inch baseline.** 412×915dp with a 24dp status bar and a 48dp
navigation bar → **843dp** usable; full band set (`spacing_md`), hero row 72dp. Band heights at
412dp have not been measured, so each is a **range**: the low end is the band's own un-wrapped
estimate at that width, the high end the height it *measured* at 360dp in §4.2.2 — a deliberately
pessimistic ceiling. Criterion 15p's own measurement governs.

| State | Fixed: padding 32 + gaps + row 72 + ribbon 84–116 + chips 84–108 each + tiles 196–224 | Slack | Stage (¾) = art edge | Footer (¼) | First tile's top vs the 421dp midpoint |
|---|---|---|---|---|---|
| No Continue chip (5 gaps, 80) | 464–524 | 319–379 | **239–284** | 80–95 | 491–504 — below it ✓ |
| One chip — the founder's state (6 gaps, 96) | 564–648 | 195–279 | **146–209** | 49–70 | 522–529 — below it ✓ |
| Both chips (7 gaps, 112) | 664–772 | 71–179 | **53–134** | 18–45 | 553–554 — below it ✓ |

On the founder's own phone the ≈172dp hole becomes a ≈150–210dp board with ordinary 16dp gaps
either side of it. Criterion 15g's tall-phone property holds in every state at both ends of every
range. **The largest blank the stage can ever leave**, on any window, is just under
`home_stage_art_min_size` plus the two gaps around it: **< 80dp** in the full set (48 + 2×16) and
**< 64dp** in the compact set (48 + 2×8), against the founder's ≈172dp. Criterion 15p makes that a
bound, not a hope.

*(Amended 2026-09-29 — **D-03m**. The table above assumes the 72dp row floor. The corrected table,
with the row at 48dp, is in the last subsection of §4.2.4. Measured between visible elements, the
paragraph above was true only of a row with no floor: the 72dp row put another 12dp of blank inside
itself, beneath the gear. D-03m removes the floor, and that makes the < 80dp / < 64dp bound exact.)*

**The narrow class is unchanged where it matters.** On 360×640dp with both chips the column
overflows, so the stage is 0 and draws nothing. The only movement is that the stage's two 8dp gaps
now fall **above** the ribbon instead of below it: hero row `8..80` · stage (0) · ribbon `96..152`
· chip `160..268` · chip `276..384` · tile `392..500` · tile `508..624`. The chips, the tiles, the
fold through the second tile and 15k's ≤ 120dp overflow are exactly as §4.2.3 derived them. On a
taller narrow window with slack — 360×800dp, ≈88dp of it — the stage gets ≈66dp and draws a small
board: decoration where there is room, which costs the short screen nothing by construction.

*(Amended 2026-09-29 — **D-03m**. With the row at 48dp, every position above moves up 24dp and the
overflow falls from 72dp to 48dp. The fold still cuts through the second tile. On 360×800dp the slack
is ≈112dp and the stage ≈84dp. The last subsection of §4.2.4 has the corrected figures.)*

**Cold start (D-03g).** The stage is one `BoardCanvas` draw of a constant position; it replaces the
deleted cluster's one draw. No read, no decode, no asset, no SDK call, and no work that depends on
`HomeUiState` — it renders the same before and after the first progress emission. §6.5's table and
its degradation step 3 now name the stage.

**What yields, and what never does.** §4.2.2's "What never yields" list stands unchanged. The stage
is the first thing on Home to yield, and it yields **by construction** — it only ever gets what the
row, the ribbon, the chips and the tiles leave — so no threshold decides it and nothing ever yields
*to* it.

**Considered and rejected**, so none of these is re-proposed as a simplification:

- *Keep the logo, just bigger or better placed.* The founder rejected the logo, not its size.
- *Distribute the slack evenly across every gap.* It shrinks the hole by multiplying it — five
  medium gaps instead of one large one — and on a fresh install it lifts the first tile above the
  midpoint of a 6.7-inch phone, which criterion 15g forbids.
- *Let the mode tiles or Continue chips grow into the slack.* It changes the cards the founder asked
  to keep, and a weighted content band is exactly clause 4's forbidden case; a computed height is
  possible but spends the space on the part that was not the complaint. Offered to the founder as
  Q-005 option 3 with that cost stated.
- *Show the player's own saved board on the stage.* It duplicates the Continue chip's thumbnail,
  makes a decoration depend on data that can arrive after first paint (a visible swap), and a fresh
  install has no board to show.
- *Rotate, tilt or animate the stage board.* Motion on the start destination is on D-03g's critical
  path and competes with the two blobs — Q-901's argument against its option 2, still true.
- *Leave the stage empty and just move the title.* That is the complaint.

**What this owes in tests** (deterministic, JVM, with the device's system-bar insets supplied per
§13's forbidden-idioms rule; every one watched fail once before it is trusted):

- 15n — `HomeHeroArt` is gone and no node sits above the title row.
- 15o — the title's start edge is the column's content start, the gear's end edge its content end,
  their vertical ranges overlap, and the title is un-truncated at `displaySmall`.
- 15p — on `w412dp-h915dp` with insets, the stage art is present at ≥ 120dp and every gap is
  ≤ 24dp in the no-chip and one-chip states; and on a grid of phone windows in every chip state,
  no blank between consecutive visible elements above the second tile exceeds 80dp (full) or 64dp
  (compact).
- 15q — a unit test replays a recorded, legal move list from `Boards.english33Initial` and arrives
  exactly at the stage's constant peg set; the stage's board state has no selection, no targets and
  no hint.
- 15r — the stage is not clickable, not focusable and announces nothing; Home's TalkBack traversal
  is unchanged.
- *(2026-09-29, D-03m.)* 15p's gaps are measured between **visible** elements, from the lower of the
  title's and the gear's bottom edges. They are never measured from the hero row's box. The owed tests
  are at the end of the subsection below.

##### 15p's gap reading, ruled — the hero row loses its floor (D-03m — 2026-09-29 amendment)

> **Status.** This answers the qa-engineer's escalation **E-1** on criterion 15p part 1
> (`.planning/reports/15-founder-feedback/qa-engineer.md`, E-1 and the 15p row). It applies to the
> slice that lands D-03m. It does not retro-certify the builds measured on 2026-09-29 (`d017711`,
> `7cc46bf`). Everything earlier in §4.2.4 stands, except the hero row's minimum height and the
> arithmetic that assumed it; each of those carries a pointer here. Nothing here depends on how the
> tech-lead fixes the fonts (Q-013). Every number below is checked in Fredoka and Nunito, as well as in
> the Roboto that devices have been rendering.

**The finding.** Criterion 15p part 1 bounds "the vertical distance between two consecutive visible
elements — hero row, stage art, …" at ≤ 24dp. On the Pixel_9, in every chip state, the hero row's
**72dp box** ends ≈16dp above the stage art, which passes. The row's **visible** content ends
**27.4dp** above it, which fails: the 48dp gear chip spans 82.7–130.7dp and the art starts at
158.1dp. The 12dp difference is built in. `home_hero_row_min_height` centres a 48dp gear and a 44dp
title line in a box 24dp taller than either, so 12dp of nothing sits inside the row beneath the gear,
and the band gap is added on top of it. `HomeStageTest` and this subsection's own arithmetic ("row
72") counted the box. The qa-engineer counted what is drawn. Both readings can be argued from the
criterion's list, and they cannot both pass.

**Ruled: the visible reading governs.** It governs every part of 15p and every gap or blank criterion
on Home, 15g's "no single gap" included. There are three reasons.

1. **The criterion says "visible".** "Hero row" in its list names what the player sees in that
   position, the title and the gear. It does not name the layout box around them, and a box has no
   edge on screen.
2. **The founder's complaint is about what the eye sees.** Q-004 named an *empty gap*, and an empty
   gap is what the eye sees between two drawn things. On `s11-home.png` the band above the board reads
   deeper than the 16dp gaps below it. That is exactly the effect the criterion exists to forbid,
   only smaller. A reading that passes a gap the eye reads as a gap is a false green, and this document
   has already paid for one of those (MB-1, criterion 15h).
3. **Only the visible reading can be measured on glass.** `uiautomator` and a screenshot report the
   gear and the title. The row's box is a layout fact that only a JVM test can see. If a criterion's
   device reading and its JVM reading differ by construction, this question will keep coming back.

**What "visible element" means.** This is stated once, so that nobody needs a third reading.

- An element's edges are the layout bounds of something drawn: a surface or a laid-out text. The
  surfaces are the gear chip, the stage-art slab, a ribbon pill, a Continue chip and a mode tile; the
  text is the title.
- Glyph ink never counts, and neither does a shadow. An undrawn layout box never counts either: a
  `Row`'s or `Column`'s minimum height, a `Spacer`, or the stage slot itself.
- A composite band's edges are the union of its visible children. The hero row runs from the higher
  of the title's and the gear's top edges to the lower of their bottom edges. The ribbon runs from its
  pills' top edges to their bottom edges.
- A gap runs from one element's visible bottom edge to the next element's visible top edge, measured
  along the column, whether or not it is scrolled.

**Re-specified: the hero row has no minimum height of its own.** It is exactly as tall as its tallest
child. At default font scale that is the 48dp gear chip; at a large font scale it is the title, once
the title grows taller than the gear. **`home_hero_row_min_height` is retired** (§6.1). Every other
property of the row stands, as the bullets under "The hero row" above give it: one `Row`,
`CenterVertically`, `fillMaxWidth()` and `clayEntrance(HERO_ENTRANCE_ORDER)`. The title keeps its
style, its `weight(1f)`, its heading semantics and its lack of `maxLines`. The gear is unchanged in
every respect.

- **Why no floor at all, rather than a 48dp floor.** Any floor taller than the row's tallest visible
  child is an invisible band by construction, and that is the defect. A floor exactly equal to the gear
  only repeats what `IconButton` already guarantees. `HomeHero.kt`'s own KDoc turns down a token for the
  gear on that ground: it would be "a second, driftable statement of the same number". With no floor,
  the row's box and its visible extent are the same, so the two readings agree by construction and
  cannot drift apart again.
- **Why not keep 72dp and shrink the gap below the row.** The gap below the row is the column's
  `spacedBy`, the same for every band. Shrinking one gap to cancel air inside a box would add a special
  case to fix a special case. The 24dp is taken out where it was put in.
- **The touch target is untouched.** The gear is still a 48dp `IconButton` in a 48dp clay chip. The
  nearest target to it is the first Continue chip or mode tile, well below. The ribbon pills are not
  targets.
- **The top of the screen.** The row now starts right at the column's top padding. The title and the
  gear sit `spacing_md` (16dp) below the status bar in the full set, and `spacing_sm` (8dp) below it in
  the compact set; before this change it was 28dp and 20dp. That is still looser than the gameplay
  strip, whose back chip sits ≈4dp below the status bar (criterion 18b). The 24dp goes to the stage,
  which is where Home's slack is meant to go.

**The arithmetic on the 6.7-inch baseline, corrected additively.** The window is 412×915dp with a
24dp status bar and a 48dp navigation bar, giving **843dp** usable, with the full band set and the
hero row at **48dp**. The band ranges are the same as in the table earlier in §4.2.4.

| State | Fixed: padding 32 + gaps + row 48 + bands | Slack | Stage (¾) = art edge | Footer (¼) | First tile's top vs the 421dp midpoint |
|---|---|---|---|---|---|
| No Continue chip (5 gaps, 80) | 440–500 | 343–403 | **257–302** | 86–101 | 485–498 — below it ✓ |
| One chip — the founder's state (6 gaps, 96) | 540–624 | 219–303 | **164–227** | 55–76 | 516–523 — below it ✓ |
| Both chips (7 gaps, 112) | 640–748 | 95–203 | **71–152** | 24–51 | 547–548 — below it ✓ |

The art fills its stage in all three states, because its edge never reaches the 320dp cap or the
380dp content width. So **every visible gap is the band gap, 16dp**, and that includes hero→art.
Part 1's ≥ 120dp art in the one-chip state holds at both ends of its range (164–227dp). Criterion
15g's midpoint holds in every state, with the tiles ≈6dp higher than before.

On the device QA measured, the 24dp comes back as ≈18dp of stage and ≈6dp of footer. That device is
the Pixel_9: 411×923dp, a 54.1dp status bar and a 24dp gesture bar.

- The stage art goes from 289 / 216 / 144dp to about **307 / 234 / 162dp** (no chip, one chip, both
  chips).
- The gear's bottom edge moves from 130.7dp to about 118dp (below the 54.1dp status bar: 16dp of
  padding plus the 48dp gear).
- Hero→art goes from 27.4dp to **16dp** in every state.

These are projections from QA's device numbers, not readings. The re-walk of 15p governs.

**The narrow class.** On `w360dp-h640dp` with both chips, §4.2.3's Robolectric arithmetic becomes:
hero row `8..56` · stage (0) · ribbon `72..128` · chip `136..244` · chip `252..360` · tile `368..476`
· tile `484..600`.

- The column totals ≈616dp against 568dp usable: **48dp over, not 72**.
- The fold at 568 still cuts through the Challenge tile: its top (484) is above the fold and its bottom
  (600) below it.
- The overflow is 48dp against 15k's ≤ 120dp. Every part of 15k holds, with more margin, unchanged.
- The 24dp is the one item §4.2.3 got wrong when it said "every one of those 72dp is a readout, a
  target or a route". It was the row's floor above its tallest child, which is none of the three
  (pointer at §4.2.3).

On the device QA measured (360×640dp, a 52dp status bar, a 24dp gesture bar, 564dp usable), the
both-chips column already fitted, with ≈28dp of slack. That slack becomes ≈52dp. The stage gets ≈39dp,
which is under `home_stage_art_min_size`, so no art is drawn. The gear→ribbon blank goes from 49.0dp
to about **55dp**. That is inside part 2's compact bound of < 64dp, and the bound is now exact: the
largest blank is the stage's < 48dp plus two band gaps, with nothing hidden inside the row to add to
it.

**The ruling in the real faces.** QA's F-A found that Fredoka and Nunito have never rendered (Q-013),
so every device number above was taken in Roboto. The row was therefore re-checked against the OFL
binaries. Fredoka `[wdth,wght]` was instanced at wght 600, the weight `displaySmall` uses; it is
byte-identical to the test fixture `fixtures/fonts/Fredoka-Variable.ttf`. Google's Roboto v3 was
instanced at wght 600 for comparison. The strings were shaped with HarfBuzz, kerning applied.

- **Width.** "Peg Solitaire" is **5.715em** wide in Fredoka SemiBold, against 5.708em in Roboto
  SemiBold. At 36sp that is **205.7dp**, 0.25dp wider than Roboto.
  - The title's room is the content width less the gear: **296dp** at 360dp width (344 − 48), and
    332dp at 412dp (380 − 48). So at default scale the title fits on one line with ≥ 90dp to spare.
  - At 200% on API 34+, non-linear scaling turns 36sp into ≈43.3dp, so the title is ≈247.5dp: still
    one line on a 360dp phone.
  - On API ≤ 33, scaling is linear and the largest system step is 1.3×. At that step (≈46.8dp) the
    title is ≈267.5dp: still one line.
  - It wraps only above ≈51.8dp of rendered size. Even then it breaks at the space ("Peg" /
    "Solitaire"), never mid-word, and it is never truncated (criteria 14, 15o).
- **Height.** Fredoka sets `USE_TYPO_METRICS`, with an ascender of 0.974em and a descender of
  −0.236em. That totals **1.210em**, or 43.6sp at 36sp, which fits inside `displaySmall`'s 44sp line
  height (Roboto's total is 1.172em).
  - So the title's text layout is **44dp** tall at default scale in either face. That is under the
    gear's 48dp, so the row is 48dp in either face, and the choice of face cannot move this ruling.
  - At 200% the title's line height (≈50dp) is more than 48dp. The title is then the row's tallest
    child, and the gap to the stage is still the band gap, by construction.
- **Everything else.** The stage art and the gaps contain no text. The fits of the ribbon, the chips,
  the tiles and the status strip in the real faces are recorded in
  `.planning/reports/15-founder-feedback/ux-designer-15p.md`. None of them changes a band height at
  default font scale, so no number in this subsection depends on them.

*(Checked 2026-09-29 by a ux-designer agent that did not write D-03m, against the file that will
ship.)* The font ruling (`tech-lead-fonts.md`, Ruling 1) bundles Fredoka's **static** SemiBold
instance, `res/font/fredoka_semibold.ttf` (Google Fonts v17, sha256 `021e30b2…d7688e43a8`), and
deletes the test fixture cited above (T5). Every figure above holds in that file, measured
independently with HarfBuzz 14.4.0 and fontTools 4.66.0:

- "Peg Solitaire" is **5715** units per 1000 (kerning applied), the same as the variable font at
  wght 600. At 36sp that is 205.7dp, and it stops fitting 296dp only above 51.8dp of rendered size.
- `sTypoAscender` 974, `sTypoDescender` −236, line gap 0, `USE_TYPO_METRICS` set: **1.210em**, or
  43.6dp at 36sp, inside `displaySmall`'s 44sp line.
- Android's scale-2.0 table (`FontScaleConverterFactory`, API 35 sources) maps 36sp to **43.3dp**
  and 44sp to **50.4dp**, so at 200% the title is ≈247.5dp wide, on one line, and taller than the
  gear, as stated above.

So the ruling holds in the face that ships, not only in the fixture it was first checked against.

**What does not change:**

- criteria 15n, 15o, 15q and 15r;
- 15p's words, its three bounds (24, 80 and 64dp) and its ≥ 120dp art;
- criteria 15g, 15i, 15k, 15l and 15m;
- §4.2.2's mechanism and every clause exception D-03l made;
- the stage, its art, its tokens and its position;
- the gear;
- the compact thresholds.

No string, colour or dependency is added. One token is retired.

**What this owes in tests.** Each one must be watched failing once before it is trusted.

- **15p is measured between visible elements.** Every gap is measured as defined above: from the
  lower of the title's and the gear's bottom edges, never from the hero row's box. A test that measures
  from the box measures a quantity 15p does not name, and today it passes a layout the device fails.
  To watch it fail, restore the 72dp floor: on `w412dp-h915dp` with insets, hero→art then reads 28dp
  and the test goes red.
- **The row is as tall as its tallest child.** The hero row's height equals its tallest child's
  height (±0.5dp), at font scale 1.0 and 2.0, on both baselines. Any assertion that the row is
  ≥ 72dp, or equals `home_hero_row_min_height`, is retargeted to this. It is never deleted and never
  `@Ignore`d.
- **15k's tests stand.** Any expected Robolectric overflow falls from ≈72dp to ≈48dp. That is still
  inside ≤ 120dp, and the fold still cuts through the second tile.

#### 4.2.5 When the Continue chips arrive (D-03n — 2026-10-21 amendment)

> **Status.** This answers the qa-engineer's loop-3 finding **F-1**
> (`.planning/reports/15-founder-feedback/qa-engineer-loop3.md`) and the question the tech-lead's
> phase-15 sign-off routed with it (Decision 3, `tech-lead-signoff.md`): may a Continue chip's arrival
> move Home, and if not, which bounds are pinned? It applies to the branch that lands D-03n,
> `bugfix/home-chip-arrival`, and retro-certifies nothing: phase 15 merged with F-1 routed, due
> before `release/3.0.0` is cut. §4.2.2's mechanism stands except its 3 : 1 split. §4.2.4's stage, its
> art, its tokens and its position stand except how the stage gets its height. Criteria 15f, 15k, 15p
> and 15r stand, with the pointers noted below. Criterion 15g stands with one reading ruled: its
> "unused space" is the no-chip slack S₀ (below, "How 15g's 'unused space' is read").

**What QA recorded.** A cold start of a player with both saves, screen-recorded on the Pixel_9a at
its native window and read frame by frame (`15f/r2/coldstart9s.mp4`, frames `f_020`–`f_035`):

| Time | What the frame shows |
|---|---|
| 2.528–2.744s | Home's first frames, with no chip. The stage is an empty slab with no pegs, and the Classic tile has no art. |
| 2.791s | The pegs and the tile art appear. |
| 2.894s | Stars and Boards fill, and no pill moves (15f holds). |
| 3.140s | Both Continue chips appear. The stage shrinks from ≈306dp to ≈163dp, and the ribbon and the tiles jump. Frame 32 catches the stage mid-redraw. |

So two things change after first paint: the chips' arrival moves the layout, and two pictures
arrive in two pieces. D-03n rules on both.

**Why the chips are late, and stay late.** The chips are known only once `HomeViewModel`'s collector
has opened and decoded both resume snapshots (`HomeViewModel.kt:209-238`). That decode is the guard
against a phantom Continue that could only ever start a fresh board (WR-02, CR-01), and §6.5 forbids a
blocking read before Home's first frame, so no mechanism brings the chips into frame 1 (sign-off
Decision 3). Offering a chip from the saved-mode list before the decode would bring the phantom back;
the tech-lead rejects it. The design therefore assumes the chips land after first paint — ≈0.6s on
QA's AVD, later on a slow phone — and says what moves when they do.

**What may move, and what may not.**

| Element | When a chip arrives, or leaves |
|---|---|
| Hero row — title and gear | **Holds still.** Same bounds in every chip state. |
| Stage | **Gives up exactly the chips' room.** Its art shrinks with it and, as before, is not drawn below `home_stage_art_min_size`. |
| Progress ribbon | **Rides on the stage's bottom edge.** It moves up by exactly the chips' room; nothing else about it changes. |
| Continue chips | Appear in the room the stage gave up, between the ribbon and the tiles, in their unchanged order. |
| Mode tiles | **Hold still** — same bounds in every chip state — wherever the stage can give the room. Where it cannot, they move down by exactly the part it could not give, and no more. |
| Footer — the space below the second tile | **Holds still.** Fixed for the window. |

A chip seldom *leaves* while Home is on screen — the account-switch dialog's "use cloud progress"
can replace the saves — and then the same rule runs in reverse: the stage takes the room back, the
ribbon rides down, and the tiles hold still.

**The rule, stated once.** Every quantity is measured inside §4.2.2's unchanged `BoxWithConstraints`.

- **S₀, the no-chip slack:** the available height, less the hero row, the ribbon, the two tiles, the
  column's padding and the five band gaps the column has with no chip (the stage and the footer each
  bring one at any height), floored at 0. It depends on the window, the font scale and those bands,
  never on the chip state, so it is known in every state without measuring a chip that is not there.
- **R, the chips' room:** for each Continue chip present, its height plus the one band gap it brings.
  With no chip, R is 0.
- **Footer** = min(S₀ ⁄ 5, max(0, S₀ − R)).
- **Stage** = max(0, ⅘ S₀ − R).
- So the column is exactly the available height while it fits. Once R exceeds S₀, both are 0, and the
  column overflows and scrolls, as §4.2.2 has always required (criteria 14, 15i).
- **The tiles move by max(0, R − ⅘ S₀)** from where they sit with no chip, and only downward. That is
  0 whenever the stage can give the whole room.

The stage is still the one child that holds a drawing in space no band claims (§4.2.2 clause 4's single
exception), no content band takes slack (clause 4), and no slack sits between bands 3 and 4 (clause 6).
How the two heights are produced is the developer's; a measure policy over the band column is one way.
§4.2.2 clause 2's "the behaviour is the contract" applies, and if the mechanism needs more than layout
the tech-lead rules on it (sign-off Decision 3, route step 2). The one fifth is a unitless ratio in a
scoped constants object (§6.4), not a `dimens.xml` token.

**Why one fifth — the device arithmetic.** These are QA's loop-3 readings, projected; the re-walk on the
branch governs. On the native window of the Pixel_9a (411×923dp, 845dp usable), S₀ is 407.9dp — the
no-chip stage slot of 305.9dp plus the ≈102dp footer — and each chip's room is 96dp: an 80dp chip and a
16dp gap, which is why tile 1's top reads 557.3, 581.3 and 605.3dp with 0, 1 and 2 chips. On 360×640dp
(564dp usable, the compact set), S₀ is 228dp and each chip's room is 88dp: an 80dp chip and an 8dp gap,
from 15k's band edges. Each cell gives the stage / the footer / tile 1's top, in dp, top of screen = 0.

| Window, chips | Today: ¼ of the current slack | ¼ of the no-chip slack | **D-03n: ⅕ of the no-chip slack** |
|---|---|---|---|
| Native, none | 305.9 / 102.0 / 557.3 | 305.9 / 102.0 / 557.3 | **326.3 / 81.6 / 577.7** — art at its 320dp cap, centred |
| Native, one — the founder's | 233.9 / 78.0 / 581.3 | 209.9 / 102.0 / 557.3 | **230.3 / 81.6 / 577.7** |
| Native, both | 161.9 / 54.0 / 605.3 | 113.9 / 102.0 / 557.3 | **134.3 / 81.6 / 577.7** |
| 360×640, none | 171.0 / 57.0 / 343.0 | 171.0 / 57.0 / 343.0 | **182.4 / 45.6 / 354.4** |
| 360×640, one | 105.0 / 35.0 / 365.0 | 83.0 / 57.0 / 343.0 | **94.4 / 45.6 / 354.4** |
| 360×640, both | 39.0 / 13.0 / 387.0 | 0 / 52.0 / 348.0 — the tiles move 5dp | **6.4 / 45.6 / 354.4** — no art |

Every native tile top in the table sits below that window's 15g midpoint (476.8dp), and every
360×640dp tile top below that window's own content midpoint (334dp: 52 + 564 ÷ 2). Three reasons for
one fifth:

1. **It pins the tiles on every phone window measured.** A quarter of the no-chip slack pins them on the
   native window but not at 360×640dp with both chips, where the stage cannot give the last 5dp
   (171 against 176). One fifth leaves 6.4dp to spare there, and 9.6dp behind a 48dp three-button bar.
2. **It keeps the founder's own state where they approved it.** With one chip — the founder's state, and
   the returning-player shots of H-4 — the board shrinks by 3.6dp and the tiles rise by 3.6dp. A quarter
   of the no-chip slack would take 24dp off the board and lift the tiles 24dp.
3. **It keeps the one-chip board a board down to the full set's threshold.** At exactly 720dp of
   available height on the 412dp class, S₀ is ≈283dp. The one-chip stage is then ≈130dp under one fifth
   and ≈116dp under a quarter of the no-chip slack — below 15p's 120dp art.

**What it costs.** On a fresh install the tiles sit ≈20dp lower than in the H-4 shots 01/02, and the
board reaches its 320dp cap with ≈3dp to spare on each side, so hero→art reads ≈19dp, inside 15p part
1's 24dp. With both chips the board is ≈28dp smaller than today. The footer, the visible margin below
the tiles, is 81.6dp on the native window and 45.6dp at 360×640dp in every chip state: a fifth of the
no-chip slack, and far from the navigation bar. Against the *current* slack, though, it is 20 %, 26 %
and 38 % on the native window with no, one and both chips, and up to ≈72 % at the pessimistic end of
the 6.7-inch baseline. That breaks criterion 15g's "no more than a third of the screen's unused space"
as long as "unused space" means the slack left once the chips are in. So the reading is ruled next.

**How 15g's "unused space" is read under D-03n — ruled.** Criterion 15g requires, on a 6.7-inch
phone with no chip and with both, that "the empty space left below the second mode tile is no more
than a third of the screen's unused space". Under D-03i's 3 : 1 split the footer was always a quarter
of the current slack, so the two possible readings never parted. Under D-03n they do. **Ruled: since
D-03n, "the screen's unused space" in 15g is S₀, the slack the window leaves with no Continue chip,
fixed for the window** — the quantity the footer is defined against. The empty space below the second
tile is read as the footer itself (the band gap after the tile and the column's bottom padding are
not slack; QA's loop-3 reading already subtracted them). On that reading the footer is one fifth in
every state, inside the third with room, and 15g's other clauses — the first tile at or below the
midpoint, no band pinned to the bottom edge, no single gap larger than the tiles — are unchanged and
hold (the table above).
- **Why this is the clause's intent and not a weakening.** 15g's third clause was written against
  D-03i's fixed `Spacer`, which sent every unspent dp to the bottom: 45 % of the screen dead below the
  tiles (`android-game-dev-s5-s8.md`). What it guards is that the bottom of the screen is not where
  Home's room goes. A footer that holds still while the chips take their room from the stage above it
  is not slack piling up at the bottom. Read against the current slack, the clause would fail the
  layout precisely *because* the tiles held still, and the only way to pass it would be a footer that
  shrinks as the chips arrive — which moves the tiles, the defect D-03n removes.
- **No usable ratio passes both readings.** A footer fixed for the window meets the current-slack
  reading with both chips only if it is at most a third of S₀ − R₂, and it meets 15p part 1 only if
  the no-chip stage stays within the 320dp art cap plus 2 × 8dp, that is, if it is at least
  S₀ − 336dp. On QA's native window (S₀ 407.9, R₂ 192) the first needs at most 0.1764 of S₀ and the
  second at least 0.1763: a knife edge under 0.1dp wide, not a design. At the pessimistic end of the 6.7-inch
  baseline (S₀ 343, R₂ 248) the first needs at most 0.092. Only a footer that shrinks as the chips
  arrive — one that moves the tiles — meets the old reading on every phone.
- **The size stays bounded.** The footer is S₀ ⁄ 5, and S₀ is less than Home's content height, so the
  footer is always under a fifth of the screen's usable height. With its band gap and the bottom
  padding, the bottom margin on QA's native window is ≈114dp of 845dp in every chip state.
- **What QA reads.** 15g's third clause: the footer — from the second tile's bottom edge to the bottom
  of the viewport, less one band gap and the column's bottom padding — is at most a third of S₀, in
  both of 15g's states; under D-03n it is a fifth. S₀ is the no-chip stage slot plus the no-chip
  footer, read on the same window in the no-chip state.

**Considered and rejected.**

- *Reserve the chips' room before they arrive.* A reserved slot is a blank band on every Home with fewer
  saves than slots, a fresh install's included. That is the hole the founder named (Q-004), brought back
  so it can be filled 0.6s later.
- *Hold the ribbon, the chips and the tiles back until the chips are known.* No tile for 0.6s or more on
  every cold start, longer on a slow phone. Delaying a primary action to keep a layout still is the wrong
  trade, and first paint would no longer show the ribbon (15f).
- *Offer a chip from the saved-mode list before the decode.* That is the phantom Continue that WR-02 and
  CR-01 removed, and the tech-lead rejects it (sign-off Decision 3).
- *Pin the ribbon and let the tiles move.* The tiles are what a thumb is heading for, so they are the most
  expensive thing on the screen to move.
- *Animate the tiles to their new place.* A target that glides under a thumb is still a target that
  moved.
- *Move the chips above the ribbon, or below the tiles.* Either changes the band order the founder kept
  (Q-004) and splits "continue" from "play". The ribbon is read-only, so moving it costs nothing a player
  aims at.
- *Keep a quarter, taken of the no-chip slack.* The smallest change in spirit, but it fails all three
  reasons above.

**The motion.** The chips' room opens continuously from 0 to its full height over `ClayAnim.ENTRANCE_MS`,
with `ClayAnim.ENTRANCE_EASING` and no overshoot. The room includes the chip's band gap, so no gap
appears a frame ahead of its chip. In the same frames the stage gives up the same height, its art
follows the stage's size, and the ribbon rides up on the stage's edge. The chip itself enters with its
existing `clayEntrance` fade and slide, with one change: **a chip that arrives after first paint takes no
stagger delay**. Its fade starts in the frame its room starts opening, so no empty room shows ahead of
it. A chip present at first composition keeps its cascade order, 2..3, as §10 has it. Because the stage
gives up in each frame exactly what the room takes, the tiles hold still in every frame, not only at the
end.

**The entrance cascade and the room are independent.** §10's cascade moves a band by a drawing offset
(`graphicsLayer` alpha and translation) and never moves its layout position, so it does not touch the
bounds 15s and 15u pin. If the chips land while the cascade is still running, no band's entrance
restarts or changes its target, and each band finishes its entrance at its D-03n position. Criterion 15v
reads the device recordings that way: position with animations off, and position after the cascade
settles with them on. A chip leaving runs the same motion in
reverse. With OS animations off (`rememberAnimationsEnabled()` false) the room is open, settled, in the
next frame. The motion exists only when a chip arrives or leaves while Home is drawn; when the chips are
already in `HomeUiState` as Home is composed, Home's first frame is the settled layout. Nothing is
announced: there is no live region, TalkBack's focus stays where it is, and the chips join the traversal
in their place (§11).

**The first frame is whole (part 4).** In QA's recording the stage slab and the Classic tile were drawn
≈0.26s before their pegs and their art. Each of those pictures is one `BoardCanvas` draw of a constant
(§4.2.4; §6.5's first-frame table), so none has a reason to arrive in two pieces: **the first frame in
which the stage slab is drawn shows its pegs, and the first frame in which a mode tile is drawn shows its
art.** The cause is not diagnosed here. It is the developer's to find on the same branch, and the
tech-lead's to rule on if the fix needs more than drawing. §6.5's budget already counts these draws in
the first frame, so drawing them whole asks for nothing the budget does not hold, and 15e measures it.

**Cold start.** D-03n changes Home's first frame: the no-chip split goes from a quarter to a fifth, and
the column's measuring changes in `ui/home/`, a start-path directory. So `bugfix/home-chip-arrival` sits
its **own** 15e on `Pixel_9a_bench` (§6.5's "Any later branch" row, and its 2026-10-18 pointer), with
arm A its merge-base with `develop`.

**What does not change.**

- the band order, and the chips' behaviour, data, copy and oracle status — §4.2.1's oracle 3, both chips
  complete inside the first viewport on every window, holds because the room comes out of the stage
  above the chips and never pushes a chip below the fold;
- the stage's art, its tokens, its position and its decoration-only semantics;
- criterion 15k: the 360×640dp column has the same total height in every chip state, so its overflow and
  its fold are unchanged;
- criteria 15l, 15m, 15n, 15o and 15q, the compact thresholds, and "never the font scale".

**What this owes in tests.** This is the test the tech-lead's route names (sign-off Decision 3, step 3),
stated as criteria 15s–15u. Each is watched failing once on today's code before it is trusted.

- **15s.** Emit no chip → Classic only → both → none, and no chip → Challenge only → both, on each of 15p
  part 2's six windows at default font scale, insets supplied, NATIVE metrics in the app theme. Assert the
  hero row's and both tiles' bounds, the footer, the stage's fall and the ribbon's rise. Today's code
  fails it: the tiles move 22–24dp per chip.
- **15t.** The same emissions at font scale 2.0 on both baselines. The tiles move by exactly the shortfall.
- **15u.** The transition, frame by frame on a paused test clock, with animations on and off.
- Assertions that read a footer of a quarter of the current slack, or a stage of three quarters of it
  (`HomeStageTest`, and `HomeLayoutTest`'s 15g and 15p cases), are **retargeted** to the one-fifth footer
  and the D-03n stage. 15g's third clause is asserted against S₀, as ruled above: the footer is at most a
  third of S₀ — under D-03n, a fifth — in both of 15g's states. None is deleted or `@Ignore`d (§13).
- **15v** is a device check: QA's recorded cold start, on the bench only after the branch's own 15e
  sitting, in the same session (criterion 15v).

#### 4.2.6 The progress pill at a large font scale (D-03o — 2026-10-21 amendment)

> **Status.** This answers the qa-engineer's **O-2** (`15-founder-feedback/qa-engineer.md`), carried open
> through loop 3 as "worse in Nunito", and this role's **R-3** (`ux-designer-15p.md`), both routed by the
> tech-lead's sign-off. It applies to the branch that lands D-03o, `bugfix/home-chip-arrival`. D-03k
> part 2's compact pill stands word for word at default font scale. What changes is what the pill does
> when its label and its value can no longer sit side by side. Criteria 14, 15i and 15m stand.

**What QA saw.** At 360dp and font scale 2.0 the compact pills read "Sta/rs", "Bo/ard/s" over three
lines, and "Leve/l" (`qa-engineer-loop3.md`, `fs2/sheet-c360.png`). Nothing is clipped. The cause is in
the shape D-03k specified: the value is measured first, and the label, weighted, gets what is left of the
pill's ≈93dp. At 2.0 that is ≈47dp (`ux-designer-15p.md` R-3), and a word wider than its line is
broken wherever it falls. Nunito is wider than Roboto, so it got worse. That same R-3 (not §7's data
requirement R-3) also found a second half: at 200% a value's first
emission ("—" is 0.68em, "401" 1.52em) can move a label from one line to two, which is a layout shift at
first emission, against 15f.

**The rule.**

1. **No break inside a word or a number.** A pill's label and its value break only between words, in
   both pill shapes, at every font scale, on every supported window. Every label is one word and every
   value one token, so in practice neither ever breaks.
2. **One shape per ribbon, decided by the labels.** On the narrow class the compact pill keeps D-03k
   part 2's shape — label beside value, one line — while **every** label, measured, fits in **half** of
   its pill's content width. Once any one does not, **all three** pills take the **stacked** shape: the
   label on its own line and the value centred on the line under it, `spacing_xs` between them,
   `spacing_sm` padding, no glyph. That is §4.2's full pill without its glyph. All three are then one
   height, the tallest's.
3. **The values never decide the shape, except in rule 4's case.** A value is "—" at first paint and a
   number a moment later. Deciding from the labels means its arrival can change no pill's shape or
   height (15f), with the one exception rule 4 names. The font scale does not decide it directly either
   (§4.2.2): the labels are measured, so the shape follows what the text needs on this window.
4. **The label is never squeezed.** In the side-by-side shape the label takes its own width and the value
   the rest. A value too wide for the rest moves under its label, in that pill alone: the one case in
   which a value decides a pill's shape and the three pills do not share one. On today's numbers it
   needs a six-digit total, a hundred thousand stars, at a font scale between about 1.07× and 1.12× on a
   360dp phone (an OEM's 1.1× step, say), because the ribbon stacks as a whole above about 1.12×. It is
   named so it is not a surprise, not because it is expected.
5. **Unchanged:** three pills, equal widths, `spacing_sm` between them, the clay surface, the entrance
   order, no glyph on the narrow class, not tappable, not focusable, one `clearAndSetSemantics` node per
   pill speaking "label, value" byte for byte, the placeholder rule, no truncation and no scaling down.
   The pills grow, the band grows and the column scrolls (criteria 14, 15i). No new string, token or
   colour.

**Why half.** Half is the line where a side-by-side pill stops reading as a label and a value and starts
reading as a label crowding a number. At default font scale on a 360dp window the widest label,
"Boards", takes ≈42dp of the ≈47dp half, tracking included (an estimate; 15w measures), so D-03k's
one-line pill, and with it 15m's ≤ 56dp band and 15k's arithmetic, is untouched. The line is crossed
well before 2.0. If at default font
scale any label measures more than half its pill, that is a failure of this ruling, and it comes back to
the ux-designer with the numbers: 15m's one-line shape at default scale is not traded for it.

**The fit at 360dp and font scale 2.0.** These are estimates; criterion 15w measures in the real faces.
The pill is (360 − 2×8 − 2×8) ÷ 3 ≈ 109.3dp wide, and its content ≈93.3dp. Font scale 2.0 exists only
on API 34+, whose non-linear table maps `labelMedium`'s 12sp to 24dp and `titleMedium`'s 16sp to 28dp;
API ≤ 33 stops at 1.3×.

| Text, at 2.0 | Width | Against ≈93.3dp of content |
|---|---|---|
| "Boards" (Nunito SemiBold) | ≈77–84dp | Whole on one line. More than half, so the ribbon stacks. |
| "Stars", "Level" | ≈56–62dp | Whole |
| "401", "0/5" (Fredoka Medium) | ≈42–45dp | Whole |
| A five-digit level, "12345" | ≈75dp | Whole |

On the full pill (≥ 400dp of width), which already stacks, the content is ≈100dp at 412dp, so the rule
binds there only as "no break". Nothing changes on that class.

**What this owes in tests:** criterion 15w. Today's code fails it at 2.0 (the labels lay out on two and
three lines). `HomeProgressRibbon`'s KDoc on the weighted label is the developer's to update with the
code.

### 4.3 Gameplay status strip — `GameStatusStrip.kt` (CLAS-02, SCOR-02, D-04/D-05a/D-05b)

> *(Amended 2026-09-28 — founder-feedback amendment, **D-05d**, §4.3.1. The Centre row's
> `titleMedium` becomes `titleLarge`; the Trailing row's `titleMedium` becomes `headlineSmall` in a
> 40dp-minimum clay pill; and the status bar is applied once — the `statusBarsPadding()` below
> stacks on the `Scaffold`'s own content padding today, so one of the two goes. The table is left
> as it reads because it is what the audited build was built against.)*

Replaces `GameTopBar` (`GameScreen.kt:490-527`). A plain `Row`, `game_status_strip_height`,
transparent, `statusBarsPadding()`, `spacing_md` horizontal padding, `CenterVertically`:

| Slot | Content |
|------|---------|
| Leading | Back: `game_status_back_size` clay circular chip, `surfaceContainerHigh`, `clayShadow(elevation_pressed)`, `claySquish()`, `Icons.AutoMirrored.Rounded.ArrowBack` at `top_bar_icon_size`, `contentDescription = navigate_back`, **silent** (D-09) |
| Centre | The context title — `GameTopBarTitle` logic verbatim: `challenge_level_indicator` + its `_desc` for Challenge, `app_name` for Classic (`GameScreen.kt:537-552`). `titleMedium`, `onSurface`, `maxLines = 1`, `weight(1f)`, `semantics { heading() }` |
| Trailing | Timer pill: `game_timer_pill_height`, `surfaceContainerHigh`, `shapes.small`, `GameTimerFormat.format()` in `titleMedium.withTabularFigures()`, `contentDescription = elapsed_time` |

> `elapsed_time` must stay the timer's content description: `GameFlowTest.kt:129` locates it by
> that string.
>
> *(Amended 2026-10-21 — **D-05e**, §4.3.2.)* `elapsed_time` stays, with its value, as the **label at
> the start** of the timer's description, which is now the label followed by the spoken duration
> ("Elapsed time, 5 minutes, 12 seconds"). A test that finds the timer by the exact `elapsed_time`
> string is retargeted (§4.3.2, §13); the sentence above is kept as the record.

No actions live here. The strip is status only, which is what lets it be this quiet.

#### 4.3.1 The status strip, re-sized, and the insets applied once (D-05d — founder-feedback amendment, 2026-09-28)

> **Status.** Answers the founder's "elapsed time and the title seem too small. They could be
> reworked." (Q-004). Supersedes §4.3's Centre and Trailing rows in their type roles and the timer
> pill's height and shadow, and rules that each system bar is applied to the gameplay screen once —
> by the `Scaffold` or by the strip and the dock, never by both, as today. Unchanged: the three
> slots and their order, the title's content logic
> (`app_name` for Classic; `challenge_level_indicator` with its `_desc` for Challenge), `navigate_back`,
> `elapsed_time`, the heading semantics, start alignment, "no actions live here", the strip's 56dp
> minimum height and its `spacing_md` sides.
> *(Pointer 2026-10-21 — D-05e.)* `elapsed_time` is unchanged as a string. It is no longer the
> timer's whole description, only its first part; §4.3.2 has the rest.

| Slot | Before (§4.3) | After (D-05d) |
|------|---------------|---------------|
| Leading — back chip | 48dp clay chip | **Unchanged.** |
| Centre — title | `titleMedium` (Fredoka Medium **16sp**), `maxLines = 1`, `weight(1f)`, start-aligned | **`titleLarge` (Fredoka Medium 22sp)**; otherwise unchanged — `onSurface`, `maxLines = 1` with ellipsis, `weight(1f)`, start-aligned, `semantics { heading() }` |
| Trailing — timer | `titleMedium` (**16sp**) with tabular figures, in a fixed **32dp** `surfaceContainerHigh` pill, `spacing_sm` sides, no shadow | **`headlineSmall` (Fredoka Medium 24sp)** with `withTabularFigures()`, `onSurface`; the pill is `heightIn(min = game_timer_pill_height)` — **40dp**, and a **minimum**: it grows with the font scale instead of clipping the digits, which the fixed height would do at 200% — with `game_timer_pill_padding` (12dp) sides, `shapes.small`, `surfaceContainerHigh`, and **`clayShadow(elevation_pressed)`**, the back chip's own shadow, so the strip reads as two clay chips framing a title |
| The strip | `statusBarsPadding()` on top of the `Scaffold`'s, 56dp minimum | **The status bar applied once** — the strip's `statusBarsPadding()` or the `Scaffold`'s, not both (below); 56dp minimum unchanged — the tallest child at default scale is still the 48dp back chip |

**Why the timer is the bigger of the two.** The timer is what a player looks up to *check*; it
changes every second and must read in one glance from arm's length. The title is context, read once
per game. Both grow — the title by 38%, the timer by 50% — and the one that changes grows most.

**Width budget at 360dp, default font scale.** 360 − 2×16 (strip sides) − 48 (back chip) − 2×8
(`spacedBy`) = **264dp** for the title and the pill together. The timer "00:00" at 24sp is ≈64dp of
digits plus 2×12dp = **≈88dp**, which leaves the title **≈176dp**: "Peg Solitaire" at 22sp needs
≈150dp and "Level 1234" ≈120dp, so both render **un-truncated with ≥ 25dp to spare**. These are
estimates; criterion 18a measures.

**At 200% font scale the timer wins, unchanged in principle.** The timer is 48sp in a pill that has
grown to hold it (≈64dp tall); the title truncates before the timer can be clipped (§11 "Text
scaling"), and TalkBack still reads the title's full text — the Classic title's text node is the
whole string, and the Challenge title's description is "Challenge level N" whatever is drawn.

**Tabular figures must actually be tabular.** The pill's width must not change as the timer ticks.
`withTabularFigures()` requests `tnum`; if the resolved Fredoka build does not honour it on a device,
the digits shift width, and criterion 18d is the check that catches it. The requirement is the
property, not the feature flag, so the fix — if one is needed — is the developer's.

**The insets, applied once.** The gameplay `Scaffold` passes content padding that already contains
the status bar and the navigation bar (its default content window insets), and `GameScreen` applies
that padding to its column (`GameScreen.kt:170-175`). Inside the column, `GameStatusStrip` applies
`statusBarsPadding()` again (`GameStatusStrip.kt:81`) and `GameActionDock` applies
`navigationBarsPadding()` again (`GameActionDock.kt:95`). The founder's `gameplay.png` shows the
result: the back chip's top edge sits at ≈112dp on a phone whose status bar is ≈52dp tall — 52 +
52 + the strip's own few dp — so an empty band as tall as the status bar hangs above the strip, and
the dock rides a second navigation inset. **Ruled: on the gameplay screen each system bar is applied
exactly once.** Which layer applies it — the `Scaffold`'s padding, or the strip and the dock with
the `Scaffold`'s content insets set to none — is the developer's choice; the property is criterion
18b.

**The board budget after D-05d** — 360×640dp, 24dp status bar, 48dp three-button navigation,
**568dp** usable:

| | Today (insets twice, 16sp) | After, default font scale | After, 200% font scale |
|---|---|---|---|
| Double-applied insets | 24 + 48 = 72 | 0 | 0 |
| Status strip | 56 | 56 | ≈64 (the grown timer pill) |
| Dock slab + its `spacing_md` margin ×2 | 72 + 32 = 104 | 104 | 104 — the dock has no text |
| Board region less `board_screen_margin` ×2 | 568 − 72 − 56 − 104 − 32 = **304** | 568 − 56 − 104 − 32 = **376** | 568 − 64 − 104 − 32 = **368** |
| Board slab edge (width limit: 360 − 32 = **328**) | **304** — height-limited | **328** — full | **328** — full |

So the larger type costs the board **nothing** at either scale on the design baseline, and the
board **gains** 24dp of edge over today because the wasted inset is returned. On a 6.7-inch phone
the board is width-limited with more than 100dp to spare either way.

**Placement.** Unchanged: title start-aligned directly after the back chip, timer at the strip's end
edge, all three `CenterVertically`. **Considered and rejected:** centring the title (the pill is
≈40dp wider than the back chip, so a "centred" title is off-centre unless equal side slots are
reserved, and reserving them costs the title ≈40dp at 360dp — exactly where it has least); a
two-row strip (costs the board ≥ 28dp for nothing once both fit one row); a clock glyph in the pill
(the core icon set has none, and the `mm:ss` shape and the `elapsed_time` description already say
what the number is); a Nunito role for the digits (§6.3 maps title and headline roles to Fredoka, and
a one-off body role at 24sp would be a second type voice in a two-element strip).

**What this owes in tests:** criteria 18a (the rendered text styles and the pill's height, read off
the text layout), 18b (the strip's top edge and the dock's bottom edge against supplied insets), 18c
(the board slab's edge at 1.0 and 2.0 font scale), 18d (the pill's width across ticks, on a device)
and 18e (at 200% the timer is fully shown and only the title may ellipsise).

#### 4.3.2 How the timer speaks (D-05e — 2026-10-21 amendment)

> **Status.** This answers the qa-engineer's loop-2 **O-A**, the code-auditor's **A-8** and **I-2**, and
> the spoken-order half of criterion 26, all routed by the tech-lead's phase-15 sign-off. It applies to
> the branch that lands D-05e. It amends the timer's content description and nothing else: the clock's
> look, its digit cells, its node shape (one node, the cells hidden from accessibility — the N-2 guard
> in `TimerDigitsTest`), the pill and every other slot of the strip are unchanged.

**The finding.** On every reading so far the clock's one accessibility node has carried the text
"05:12" and the description "Elapsed time", with its five digit cells hidden (`qa-engineer-loop2.md`;
`code-auditor-loop2.md` I-2; `code-auditor-loop3.md` N-2). TalkBack speaks a node's description in place
of its text, so a TalkBack player hears "Elapsed time" and never the time. Nobody has captured the
utterance, because adb cannot drive TalkBack's gestures, but the node's shape allows no other reading.

**The spoken form.** The description is `elapsed_time_spoken`: the label `elapsed_time` ("Elapsed time"),
a comma, and the elapsed time as a duration.

| The pill shows | TalkBack says |
|---|---|
| 00:00 | "Elapsed time, 0 seconds" |
| 00:01 | "Elapsed time, 1 second" |
| 00:45 | "Elapsed time, 45 seconds" |
| 01:00 | "Elapsed time, 1 minute" |
| 01:01 | "Elapsed time, 1 minute, 1 second" |
| 05:12 | "Elapsed time, 5 minutes, 12 seconds" |
| 12:00 | "Elapsed time, 12 minutes" |
| 59:59 | "Elapsed time, 59 minutes, 59 seconds" |
| 100:00 | "Elapsed time, 100 minutes" |

- Minutes are said when there is at least one. Seconds are said when there is at least one, or when the
  whole time is zero. Minutes are never rolled into hours, just as the clock never rolls them
  (`GameTimerFormat`): the pill shows "100:00" and TalkBack says "100 minutes".
- "minute"/"minutes" and "second"/"seconds" come from `plurals` resources, as the app's three existing
  plurals do. The comma-joined parts are format strings, so their order belongs to the copy and not to
  the code (§8).
- The description is computed from the same elapsed seconds as the clock, in the same composition, so
  what TalkBack says and what the pill shows cannot disagree.

**Where it lives, and what does not change.**

- **One focus stop:** the clock's existing node. The cells stay hidden, the pill is not a second stop,
  and the node has no heading semantics and no role. In the strip it is the third stop: back → title →
  timer (§11).
- **It is never announced on its own.** There is no live region, polite or assertive: a timer that
  spoke every second would talk over the game. TalkBack reads it when the player moves to it, by swipe
  or by touch, and at no other time.
- **The description changes every second while the clock runs.** If TalkBack, on the device, speaks the
  timer again while focus rests on it — the reading criterion 18g captures — then the spoken duration is
  carried in **whole minutes** instead: "Elapsed time, 5 minutes", and "Elapsed time, under a minute"
  below one minute, so it changes at most once a minute. The label, the one stop and the no-live-region
  rule stand either way. The fallback is decided here so that a device finding needs a code change and
  not a new ruling.

**Considered and rejected.**

- *Drop the description and let TalkBack read the text.* "05:12" is then read as a time of day or digit
  by digit, depending on the voice, and the stop loses the label that says what it is.
- *Keep `elapsed_time` as the description and carry the value as a state description.* QA's TalkBack
  speaks state before name ("On, Show possible moves, Switch", `15-founder-feedback/qa-engineer.md`,
  21d), so the stop would say "5 minutes, 12 seconds, Elapsed time". TalkBack also speaks a change of
  state on the focused node, so it would speak every second. One description fixes the order and stays
  quiet.
- *Fold the time into the title's stop.* The title is a heading, read once per game. The time is what a
  player checks again and again, and it needs a stop of its own to be found by swiping.
- *Announce the time at milestones, say every minute.* That is a live region by another name; nobody
  asked for it, and it would talk over the board's own feedback.

**Test impact.** Three tests find the timer by the exact `elapsed_time` string and will stop finding it:
`GameFlowTest.kt:129`, `GameScreenChromeTest.kt:62` and `:75`, and the helper at
`GameStatusStripTest.kt:312-314`. They are **retargeted**, never deleted and never `@Ignore`d: find the
clock through `GAME_TIMER_PILL_TEST_TAG` and the node inside it, or by the label as a substring of the
description, and assert the whole sentence wherever the test is about the timer. §13's "passes
unchanged" rows for `GameFlowTest.kt:129` are superseded by this paragraph. New and owed: criterion 18f's
table test, watched failing on today's code, and the device capture, 18g.

**Criterion 26's spoken order.** The order in which TalkBack visits and speaks the refreshed screens'
stops has never been captured: criterion 26 has read "PARTIAL — the TalkBack half is not settled" since
the phase-15 QA pass (`phase-15/qa-engineer.md`). Criterion **26b** now lists, for each screen, the stops
in order and the string each must speak, from §11's traversal and §8's strings. The qa-engineer reads it
off TalkBack's speech output in one sitting with 18g. The order inside one utterance — name, role, state
— is TalkBack's own, varies by version, and is not judged.

### 4.4 Gameplay action dock — `GameActionDock.kt` (ASST-01, ASST-02, D-04/D-04a–d), and the tutorial's wrong-tap contract (D-01c, D-01h)

> Two unrelated surfaces share this section because the second is a single table: the dock below,
> then the tutorial-only wrong-tap answers at the end. The tutorial has **no dock** (§4.1).

A single clay slab below the board, in the thumb zone: `surfaceContainerHigh`, `shapes.large`,
`clayShadow(elevation_card)`, `game_action_dock_height`, `navigationBarsPadding()`, holding four
targets in a `SpaceEvenly` `Row`, each `game_action_button_size` (56dp) with a minimum
`spacing_sm` gap:

> *(Amended 2026-09-28 — founder-feedback amendment, **D-04e**, §4.4.1. The **Resting** and
> **State channel** columns below are superseded by §4.4.1's five looks: Undo, Hint **and Restart**
> share one Available look, Undo's disabled state becomes Unavailable, Hint's thinking state
> becomes Working, and Show-moves alone is drawn On or Off. The navigation bar is applied to the
> dock once — its `navigationBarsPadding()` or the `Scaffold`'s, not both (D-05d, §4.3.1). Order,
> glyphs, sizes, cues, descriptions other than Show-moves' and the
> no-allowance contract below are unchanged. The table is left as it reads because it is what the
> audited build was built against.)*

| Order | Control | Glyph | Resting | State channel | Cue |
|-------|---------|-------|---------|---------------|-----|
| 1 | **Undo** | local `UndoIcon` (unchanged) | `primaryContainer` fill, `onPrimaryContainer` glyph | `enabled = canUndo`; disabled → flat `surfaceVariant` fill + `onSurfaceVariant` glyph | `SoundCue.Undo` (existing, ASST-02) |
| 2 | **Hint** | new local `HintIcon` (lightbulb) | `primaryContainer` fill, `onPrimaryContainer` glyph | `solverThinking` → spinner in place of glyph + `enabled = false` + `hint_thinking` | `SoundCue.Hint` (existing) |
| 3 | **Show moves** | new local `ShowMovesIcon` (peg + two dots) | transparent, `outline` border, `onSurfaceVariant` glyph | **ON → filled `primaryContainer` pill + `primary` glyph; OFF → outlined + `onSurfaceVariant`.** Fill, not tint alone (D-04b) | **silent** (D-09: toggles stay quiet) |
| 4 | **Restart** | `Icons.Rounded.Refresh` (unchanged) | transparent, `outline` border, `onSurfaceVariant` glyph | — | **silent** (D-09) |

Every glyph renders at `game_action_glyph_size`. Every content description is the existing string —
`undo_action`, `hint_action`/`hint_thinking`, `show_moves_on`/`show_moves_off`, `restart_board`
(`strings.xml:84-91`, `:14`). **No new copy for the dock.**

> `show_moves_on` must stay the toggle's ON content description: `ButtonSoundTest.kt:226-244`
> locates it by that string.

**The dock never shows an allowance count.** No badge, no "×3", no ad glyph, no allowance-driven
disable. That negative contract is inherited verbatim from the **ad-allowance ruling the code
records at `GameUiState.kt:131-135`** — an earlier phase's `D-05`, not this document's toolbar
D-05 (§3) — and this refresh does not relax it.

**Wrong-tap handling on the board — tutorial only (D-01c).** Three classes, escalating assistance,
never correction:

| Class | Answer |
|-------|--------|
| Tapped a peg with no legal jump | The peg selects and draws zero destination dots. After `TutorialAnim.NUDGE_DELAY_MS` with no progress, the coachable peg's ring re-pulses — and if the immovable peg is **still held** at that moment, the escalation puts it back down first (the release clause in class 3 below), so the peg the coach points at can be tapped straight away. **That release, not a second tap rule, is how a player holding an immovable peg is freed** (§4.1 "Tap semantics"). **Silent** (D-01h, upheld by D-01i) — the ring and the absent dots are the whole answer, and no `Select` or `Deselect` cue fires either. |
| Tapped anything that is not a legal destination while a peg is selected — an illegal hole, an out-of-play cell, **or another peg** | The legal dots re-pop and the selection is kept. Nothing is lost, nothing is announced as wrong. **Another peg belongs to this class and is not a re-selection** (§4.1 "Tap semantics", ruled 2026-09-21): the held peg stays held, exactly as on the gameplay board (`GameViewModel.kt:788-796`, `:817`, `:823`), and recovery is the game's own — re-tap the held peg to put it down, then tap the other. **Silent** (D-01h, upheld by D-01i): the in-game `InvalidTap` cue is deliberately *not* carried into the tutorial, because a rejection sound is the one channel that would make a wrong tap feel like a failure — which D-01c forbids. This is the one reason in D-01h that carried its own weight, and D-01i upholds it without reservation. |
| Three consecutive non-progressing taps in a step (`TutorialAnim.NON_PROGRESSING_TAPS_BEFORE_COACH`), or `TutorialAnim.NUDGE_DELAY_MS` of idleness | The coach escalates once: the source ring pulses **and** the destination dot brightens **and** the caption re-shows the step's own caption. It never escalates past this, it never blocks, and **it never mentions Undo** — no tutorial board can box the player in, so there is nothing to be rescued from, and promising a rescue from an impossible failure teaches the player to expect failure. |

**The release clause** *(ruled 2026-09-21, with §4.1's tap semantics)*. **When the escalation fires
while a peg is held that is *not* the peg the coach is about to point at, the escalation puts that
peg back down**, silently, in the same state change that raises the ring — and it applies to both
triggers above, the tap count and the idle nudge alike.

Without it the help is unreachable rather than merely slow: while a peg is held, a tap on the
pulsing peg is class 2 above and changes nothing, so the coach would be pointing at a peg the
player cannot pick up, and every further tap would re-escalate onto the same unreachable
suggestion. Releasing is "more help, never less", which is what class 3 already promises. The one
case it must **not** release is when the held peg *is* the coached source — the player has already
selected correctly and only needs to find the dot — because letting go there would undo a correct
selection and make the help worse.

This diverges from nothing: escalating assistance is tutorial-only — the gameplay board's
equivalent is the dock's Hint control, specified earlier in this section — so unlike a tap rule it
has no gameplay counterpart it could contradict. It is also silent: no `Deselect` cue and no `Hint`
cue exist on this screen to fire (§5, D-01i).

Expect class 1 to fire on T2 and T3 in normal use: T2's middle peg, and **three of T3's five
pegs** — (1,0), (1,1) and (1,2), since all three of T3's legal openings start at (0,0) or (0,1) —
have no legal jump at the start. That is unavoidable in any peg-solitaire opening (a peg with no
hole two cells beyond a neighbour cannot move), so it is a case QA should see rather than a
defect; §14 criterion 3 names the tap to check.

**Every wrong tap in the table is silent; a *completed* jump is not** (D-01i, §4.1). That
asymmetry is deliberate and is the tutorial's whole audio design: the one sound on the screen
marks the one thing the player got right. It is also why the silences above are checkable
criteria (7d) and owed tests (§13) rather than an absence nobody asserts.

During normal gameplay (outside the tutorial) wrong-tap handling is **unchanged** — the existing
`rejectedTapTick` → `InvalidTap` path (`GameScreen.kt:393-398`), cue included.

#### 4.4.1 The dock's state language (D-04e — founder-feedback amendment, 2026-09-28)

> **Status.** Answers the founder's "I like the in-game icons, but I can't really say if they are
> on or off" (Q-004). Supersedes the Resting and State channel columns of the dock table above,
> §6.2's dock rows and §11's dock rows. Unchanged: the order (Undo · Hint · Show moves · Restart),
> every glyph except that Show-moves Off gains a slash, the 56dp targets and 28dp glyphs, the slab,
> `SpaceEvenly`, every cue, `undo_action`, `hint_action`, `hint_thinking`, `restart_board`,
> `show_moves_on`, and the no-allowance negative contract.

**What the founder saw, and why.** In `gameplay.png`, Undo and Hint are pale filled discs and
Show-moves and Restart are outlined rings. Fill-versus-outline is the on/off vocabulary, so the
dock read as two controls on and two off, when three of the four have no on/off at all and the one
that does was on. The channel was also nearly invisible: `primaryContainer` #EDE4FF on the slab's
`surfaceContainerHigh` #F1EAFB is **≈1.04:1**, and the `outline` #C9BEE0 ring **≈1.5:1** — WCAG
1.4.11 asks 3:1 of anything that identifies a state.

**Two kinds, two grammars.**

| Kind | Controls | Has an on/off? | Looks it can take |
|------|----------|----------------|-------------------|
| **Action** — press to do something | Undo, Hint, Restart | **No** | Available · Working (Hint only) · Unavailable (Undo only) |
| **Toggle** — a setting that is on or off | Show moves | **Yes** | On · Off |

**The five looks.** Every value is an existing M3 role or a §6.1 token; nothing is a new colour.

| Look | Disc fill | Ring | Shadow | Glyph | Mark | Responds to a press |
|------|-----------|------|--------|-------|------|---------------------|
| **Available** | `primaryContainer` (pale) | none | `clayShadow(elevation_pressed)` — raised | the control's own glyph, `onPrimaryContainer`, full strength | none | yes — `claySquish()` |
| **Working** | `primaryContainer` (pale) | none | **none** — pressed in while busy | a `CircularProgressIndicator` in `primary` at `game_action_glyph_size`, in place of the glyph | none | no |
| **Unavailable** | **none** — transparent | none | none | the control's own glyph, `onSurface` at `DockMetrics.UNAVAILABLE_GLYPH_ALPHA` (**0.38**, a unitless ratio in a scoped constants object) | none | no |
| **On** | `primary` (strong) | none | `clayShadow(elevation_pressed)` | `ShowMovesIcon`, `onPrimary` | **the on mark** — a `game_action_mark_size` (24dp) circle filled `onPrimary`, with a `game_action_ring_width` (2dp) `primary` border and `Icons.Rounded.Check` at `game_action_mark_glyph_size` (16dp) in `primary`, centred on the disc's rim at its top-end 45° point | yes — `claySquish()` |
| **Off** | **none** — transparent | `game_action_ring_width` (2dp), `onSurfaceVariant` | none | **`ShowMovesOffIcon`** — the same drawing with a diagonal slash from top-start to bottom-end, knocked out one stroke-width either side so it reads as a slash and not a scratch — `onSurfaceVariant`, full strength | none | yes — `claySquish()` |

Every disc stays `game_action_button_size` (56dp) and every glyph `game_action_glyph_size` (28dp).
The on mark is drawn, not a node, and it stays inside the 72dp slab: centred on the rim, its outer
edge sits ≈4dp below the slab's top.

**Which control is in which look — every state each can reach.**

| Control | Situation | Look | TalkBack (name · role · state) |
|---------|-----------|------|--------------------------------|
| Undo | at least one move to undo (`canUndo`) | Available | "Undo" · button |
| Undo | `canUndo`, and the free undos for this level are spent | **Available — identical** | identical |
| Undo | nothing to undo: a fresh board, undone back to the start, or a Challenge board still loading (`!canUndo`) | Unavailable | "Undo" · button · disabled |
| Hint | idle | Available | "Hint" · button |
| Hint | the free hints for this level are spent | **Available — identical** | identical |
| Hint | a hint is showing on the board (`hintSource != null`) | **Available — identical.** Hint has no "on": the gold glow on the board is the only sign a hint is up | identical |
| Hint | the last search found no winning move (`noWinningMove`) | **Available — identical**; the rescue banner carries the message | identical |
| Hint | a Challenge board is still loading | Available — the look follows `solverThinking` only; what a tap does while loading is the ViewModel's and unchanged | identical |
| Hint | the Solver is searching (`solverThinking`) | Working | "Finding a move" (`hint_thinking`, unchanged) · button · disabled |
| Show moves | dots on (`showHints`) | On | "Show possible moves" · switch · on |
| Show moves | dots off | Off | "Show possible moves" · switch · off |
| Restart | always — the ViewModel decides confirm or immediate | Available | "Restart Board" · button |

While the restart confirm or the ad opt-in dialog is up, every control keeps its look behind the
scrim; nothing in the dock changes because a dialog opened.

**The spent-allowance rows are an earlier ruling applied, not a new one.** `GameUiState.kt:131-135`
says the controls "must not change appearance" with the allowance counters — "no badge, no "×3"
chip, no allowance-driven disabled state" — because the opt-in dialog is the only signal the
allowance system exists. So "out of hints" and "out of undos" are **not** states of
the dock: the control looks and announces exactly as Available, and the tap raises the opt-in dialog
(ADS-01) — or, if no ad can load, grants the assist with the "refilled for free" snackbar (D-11).
Drawing Unavailable there would be worse than a rule breach: it would tell the player the assist is
gone when one tap would offer it.

**Why no two looks can be confused without colour.** The channels are fill (none / pale / strong —
"strong" is ≥ 3:1 against the slab, so it survives every colour-vision deficiency and a greyscale
screenshot), ring, shadow, glyph shape (normal / slashed / spinner), and mark. Hue is not counted.

| Pair | Channels that differ |
|------|----------------------|
| Available · Working | shadow, glyph shape |
| Available · Unavailable | fill, shadow (and glyph strength) |
| Available · On | fill tone, mark |
| Available · Off | fill, ring, shadow, glyph shape |
| Working · Unavailable | fill, glyph shape |
| Working · On | fill tone, shadow, glyph shape, mark |
| Working · Off | fill, ring, glyph shape |
| Unavailable · On | fill, shadow, mark |
| Unavailable · Off | ring, glyph shape (and glyph strength) |
| On · Off | fill, ring, shadow, glyph shape, mark |

Every pair differs in **at least two**. The recipe is data — one mapping from look to fill, ring,
shadow, glyph variant and mark — so criterion 21b asserts this over all ten pairs in a unit test
instead of trusting this table.

**Contrast**, computed from `Color.kt`; the device reading governs (criteria 21f, 27):

| Pair | Light | Dark | Needs |
|------|-------|------|-------|
| On disc `primary` against the slab `surfaceContainerHigh` | ≈4.9:1 | ≈5.2:1 | ≥ 3:1 — it identifies a state |
| Off ring `onSurfaceVariant` against the slab | ≈5.3:1 | ≈5.6:1 | ≥ 3:1 |
| On glyph `onPrimary` on `primary` | ≈5.7:1 | ≈5.6:1 | ≥ 3:1 |
| Check `primary` on the mark's `onPrimary` | ≈5.7:1 | ≈5.6:1 | ≥ 3:1 |
| Available glyph `onPrimaryContainer` on `primaryContainer` | ≈10.5:1 | ≈7.6:1 | ≥ 3:1 |
| Off glyph `onSurfaceVariant` on the slab | ≈5.3:1 | ≈5.6:1 | ≥ 3:1 |
| *Retired channel, for the record:* `primaryContainer` fill against the slab | ≈1.04:1 | ≈1.5:1 | it was carrying a state |

The Unavailable glyph is deliberately below 3:1; WCAG exempts an inactive control, and the look
does not rely on it alone (it also loses its fill and its shadow).

**TalkBack: Show-moves becomes a switch.** The toggle takes toggleable semantics with
`Role.Switch` and `ToggleableState.On`/`Off`, and its name is **constant** — `show_moves_on`,
"Show possible moves", in both states. TalkBack then reads name, role and state the way every
switch in Settings already does. The old pattern swapped the whole description between "Show
possible moves" (on) and "Hide possible moves" (off): two imperatives, each naming the **opposite**
of what the next tap does, so a TalkBack user heard "Hide possible moves" on a control whose tap
shows them. `show_moves_off` loses its only caller and is deleted (§8). `ButtonSoundTest.kt:226-244`
finds the toggle by `show_moves_on`, and that is now the toggle's name in both states, so the test
passes **unedited**. The toggle stays silent (D-09) and stays an `IconButton`-sized 56dp target; its
node is not merged or cleared (§11 "The unmerged tree is not the contract").

**Transitions.** On ↔ Off: the fill and the ring cross-fade over `ClayAnim.PRESS_IN_MS`, the glyph
cross-fades to its other variant over the same, and the on mark scales in with `overshootSpring()`
and out without overshoot. Available ↔ Unavailable: fill, shadow and glyph strength cross-fade over
`ClayAnim.PRESS_IN_MS`. Available → Working is instant, because it confirms the tap registered. All
of it is gated on `rememberAnimationsEnabled()`; with animations off each look is simply the new one.
Sound is unchanged: Undo plays `SoundCue.Undo`, Hint `SoundCue.Hint` on the reveal, Show-moves and
Restart are silent (D-09).

**The on mark is a state mark, not an allowance badge.** Criterion 24 and the plan's prohibition
grep P10 (`allowance|badge|×[0-9]|adReason` over `GameActionDock.kt`) forbid allowance surfaces, and
the mark is not one: it marks a *setting*, never a count, and it appears only on Show-moves. So that
the grep stays a meaningful zero rather than acquiring a false hit, the mark is called the **on
mark** (or check mark) in code, KDoc and test names — never a "badge".

**Labels were weighed and not added.** Text under each icon would name the actions and could spell
the toggle's state, and it is the obvious reading of the founder's note. It is declined for three
reasons: it costs the board ≥ 20dp at default scale and ≥ 40dp at 200% on the phone where the board
is tightest (§4.3.1's budget); it breaks the dock's "renders no text at all" property that makes the
allowance contract greppable; and the founder asked for the icons to stay. The marks do the state
job without text; if the founder later wants labels, they are an additive change to this section.

**Test hook.** `DockActionFilledKey` (a `Boolean`) cannot express five looks. It is replaced by one
custom semantics property carrying the look — `Available`, `Working`, `Unavailable`, `On`, `Off` —
on each dock control, in the same idiom (`GameScreen`'s `PegCountKey`), so every row of the table
above is assertable without a screenshot.

**What this owes in tests:** criteria 21a (every control in the look its state requires,
table-driven over the rows above), 21b (the ten pairs, over the recipe data), 21c (a spent
allowance changes nothing visible or announced), 21d (switch semantics, one name, `show_moves_off`
gone, `ButtonSoundTest` unedited), 21e (only Show-moves ever draws the mark or the slash); and on a
device 21f (contrast of the two state marks) and 21g (the greyscale read).

### 4.5 Settings — one row added (D-02a)

A `SettingsLinkRow` in `GeneralSection` (`SettingsScreen.kt:193-218`), **above** the Sound toggle:
label `settings_how_to_play_label` ("How to play"), supporting
`settings_how_to_play_supporting` ("Replay the tutorial"), `onClick` → the tutorial,
`SoundCue.ButtonForward` (D-09: a navigational CTA). Full-row target, ≥ `settings_row_min_height`
72dp.

Two mechanical notes for the developer: the section's entrance-order constants are positional, so
inserting a row means renumbering the cascade; and `settings_sound_supporting` is stale copy
("Saved for the upcoming sound effects", `strings.xml:143`) — sound shipped in Phase 7, and this
refresh should fix it to describe what the toggle now does.

---

## 5. Deliberate non-UI (negative contract)

Things a reader might expect from this spec and will not find, each on purpose:

- **No second control surface on the gameplay screen.** One status strip, one action dock, nothing
  else. The rescue banner, the ad-offer dialog, the restart confirm and the snackbar are the
  screen's only other surfaces and all four are unchanged.
- **No M3 `TopAppBar`, `BottomAppBar` or `NavigationBar` anywhere in this refresh** (D-05c).
- **No confirm-quit sheet on gameplay back.** Progress is already snapshotted for resume
  (PERS-03), so leaving loses nothing, and there is no `BackHandler` on gameplay to hang one from.
- **No allowance counters, badges or ad affordances** on the dock (§4.4).
- **No Play Games chip on Home** (D-03e).
- **No tappable progress pills** (§4.2).
- **No logo or emblem on Home** *(2026-09-28, D-03l)*. The hero's only decoration is the stage —
  the game's own board, drawn into the slack — and it is not tappable, not focusable and not
  announced (§4.2.4).
- **No text in the dock, and no on/off look on an action** *(2026-09-28, D-04e)*. Undo, Hint and
  Restart are never drawn On or Off, never carry the on mark and never draw a slashed glyph; only
  Show-moves does. A spent allowance never changes a control's look (§4.4.1).
- **No loading state, skeleton or spinner on Home.** The ribbon paints placeholders and fills;
  first paint never waits (D-03g, §6.5).
- **No per-board star pips on Home.** They stay on the Classic catalog, untouched (§4.2.1).
- **Nothing on Home collapsed behind a tap or an expander.** All three device-test oracles are
  visible on first paint (D-03f). *(Sharpened 2026-09-28, D-03k — the first sentence is the
  contract and it is **unchanged on every window**: nothing on Home is ever collapsed, folded,
  truncated, expandable or behind a tap. The second sentence was always loose, because oracle 2
  has never been on Home at all, and on the **narrow class** oracle 1's tile chip is across the
  fold — composed at full size, reached by one plain scroll of ≤ 120dp, with the ribbon's Level
  pill as its above-the-fold witness. Oracle 3's two Continue chips are on first paint everywhere
  and that is not relaxed. §4.2.1's dated note draws the line; criteria 15d and 15k check it.)*
- **No tutorial step persisted across process death** (D-01e) — and specifically **no
  `SavedStateHandle` step cursor**, which would survive process death and resume at T2, silently
  contradicting D-01e. Forbidding the cursor is D-01g's clause; the decision it protects is
  D-01e's.
- **No sound in the tutorial beyond the jump pair and the two exit presses** (D-01h as amended by
  D-01i). The enumerated contract, both halves, because only an enumerated one is checkable:
  - **Plays:** `SoundCue.Jump` (pitched by `playJump(runIndex)`) with `SoundCue.PegRemove`
    layered under it, on a **completed** jump; `SoundCue.ButtonForward` on the completion CTA;
    `SoundCue.ButtonDismiss` on `Skip`.
  - **Never plays:** `Select`, `Deselect`, `InvalidTap`, `Undo`, `Hint` on the board, and
    `StarPop` and `Win` on the completion star — at any point, on any step, on either entry path.
    System back is silent too.
  - **No `SoundCue` reference is added to `BoardCanvas`** — that file has none today and gaining
    one would put audio on the shared board component for every screen at once. The jump effect
    is a composable declared *beside* the screen and lifted from `GameScreen.kt:320-330`, never
    moved into the canvas.
- **No Undo control, and no copy naming Undo, anywhere in the tutorial** (§4.1, §4.4). The
  tutorial's bands are Header / Board / Caption / Footer; there is no dock to hold one, the
  boards make being stuck impossible, and Undo is met in the real game where it is genuinely
  useful.
- **No new audio asset.** Every cue this refresh needs already exists in `SoundCue` (§9).
- **No third-party art, font or icon asset** (§9).
- **No change to any rule, footprint, star band, score formula or generator.** Board *content* for
  all three tutorial steps is the game-designer's ruling (§4.1, §7 R-4), not a UX decision, and
  **`TutorialBoards` is never added to `Boards.CLASSIC_SPECS`** — that list is the Classic
  catalogue and the denominator of the Home Boards pill.

### 5.1 The tutorial's hard prohibitions (D-01g) — requirements, not inferences

These four are stated as a contract because the failure they prevent is silent, expensive and
**not** recoverable by the player. They bind the developer and the reviewer equally; each is
checkable by one grep over `app/src/main/.../ui/onboarding/`.

| # | Prohibition | Why it is a contract and not a preference |
|---|-------------|-------------------------------------------|
| 1 | The tutorial hosts **`BoardCanvas`** and **never `GameScreen`** | `GameScreen` owns every sound cue (`GameScreen.kt:327-428`) and the whole gameplay chrome; hosting it would drag the dock, the banner, the dialogs and `SoundCue.Win` into a teaching screen. |
| 2 | The tutorial's state holder is **`TutorialViewModel`** and **never `GameViewModel`** | `GameViewModel`'s `Won` path does three irreversible things: it emits `finished` → Results, it **clears the mode's resume snapshot** (`GameViewModel.kt:963`) and it **advances the Challenge ladder** (`:973`). Every tutorial step ends on exactly one peg, so the path would fire **three times per run**. |
| 3 | The tutorial writes **no `LevelResult`**, clears **no resume snapshot** and calls **no ladder advance** | This is prohibition 2's consequence, stated separately because it is the damage: a first-run tutorial on a player who already has a saved Challenge game would delete their Continue chip and push their level counter forward three times. Two of the three §4.2.1 device oracles are written on that path, so the loss is not only the player's — it makes `11-HUMAN-UAT.md` tests 10/11/13 **unverifiable**, and §4.2.1 exists precisely to keep them verifiable. |
| 4 | The tutorial has **no route to Results** | Its only outbound edge is `popBackStack()` (D-01g). A tutorial that can reach the Results screen can bank a star, and a banked tutorial star corrupts the Home Stars pill (§4.2) on the player's very first launch. |

A tutorial that trips any of these looks correct on screen and is wrong in the database. That is
the failure class this section exists to name.

**What these four do not forbid — clarified 2026-09-21, not relaxed (D-01i).** They bind **where
the tutorial's screen and its state holder come from**, not **where a `@Composable` effect is
declared**. `JumpSound` (`GameScreen.kt:320-330`) takes a `MoveTransition?` and reads
`LocalSoundPlayer`; it touches no `GameViewModel`, no repository, no navigation and no
persistence. Calling a shared `internal` copy of it from the tutorial leaves all four intact: the
tutorial still hosts `BoardCanvas` and never `GameScreen` (1), still runs `TutorialViewModel` and
never `GameViewModel` (2), still writes no `LevelResult`, clears no snapshot and advances no
ladder (3), and still has `popBackStack()` as its only outbound edge (4). Prohibition 1's stated
reason — that hosting `GameScreen` would drag the dock, the banner, the dialogs and `SoundCue.Win`
into a teaching screen — is unchanged and unweakened by this; **lifting one effect out of that
file is not licence to host the file**, and the grep gate over
`app/src/main/.../ui/onboarding/` still applies verbatim, now reading "references no `SoundCue`
the spec does not name" (§13) rather than "references no `SoundCue`".

---

## 6. Design tokens

### 6.1 New `dimens.xml` tokens

Following the file's own one-token-per-owner convention (`dimens.xml:113-132`), each new token is
declared even where an existing token shares its value. All multiples of 4.

| Token | Value | Owner |
|-------|-------|-------|
| `tutorial_board_size` | 280dp | The tutorial's interactive board slab — the largest footprint is 3×3, so ≈93dp cells, well above §11's 48dp floor |
| `tutorial_caption_min_height` | 48dp | Reserved caption height (1↔2 line swap must not reflow the board) |
| `tutorial_step_pip_size` | 12dp | One progress pip |
| `game_status_strip_height` | 56dp | The gameplay status strip |
| `game_status_back_size` | 48dp | The status strip's back chip |
| `game_timer_pill_height` | 32dp | The status strip's timer pill |
| `game_action_dock_height` | 72dp | The gameplay action dock slab |
| `game_action_button_size` | 56dp | One dock action target |
| `game_action_glyph_size` | 28dp | One dock action glyph |
| `home_hero_min_height` | 160dp | The Home hero band |
| `home_hero_art_size` | 120dp | The Home hero's decorative board art |
| `home_stat_pill_min_height` | 64dp | One progress-ribbon pill |
| `home_stat_pill_glyph_size` | 20dp | One progress-ribbon pill glyph |
| `home_resume_thumb_size` | 40dp | The Classic Continue chip's board thumbnail |
| `home_tile_art_size` | 56dp | A mode tile's trailing board art |
| `home_hero_compact_min_height` | 72dp | **(D-03i, added 2026-09-28)** The hero's minimum in the compact band set — one row holding the gradient title and the 48dp Settings gear, no art (§4.2.2) |
| `home_compact_threshold_height` | 720dp | **(D-03i, added 2026-09-28)** The available height below which Home takes the compact band set. It is the full set's own both-chips requirement (≈716dp) rounded onto the grid, and it is compared against available height only — never against the font scale (§4.2.2) — *and, since D-03k, only at an available width of `home_compact_threshold_width` or more; the narrow class uses the token below* |
| `home_compact_threshold_width` | 400dp | **(D-03k, added 2026-09-28)** The available **width** below which the compact threshold is the narrow one. 360dp is the narrowest supported window and the next common width is the 412dp class, so no shipping device sits on the boundary (§4.2.3) |
| `home_compact_threshold_height_narrow` | 880dp | **(D-03k, added 2026-09-28)** The compact threshold **at an available width below 400dp**: the full set's own both-chips height *at 360dp width* (≈872dp, where the bands wrap) rounded onto the grid. Its consequence is deliberate — at 360dp width Home is always compact (§4.2.3) |
| `home_stat_pill_compact_min_height` | 40dp | **(D-03k, added 2026-09-28)** One progress pill in the compact-narrow ribbon: `spacing_sm` + one `titleMedium` line + `spacing_sm`, with the label beside the value and no decorative glyph. A **floor**, not a height — the label wraps and the band grows before either text truncates (§4.2.3, criteria 14/15m) |
| `home_hero_row_min_height` | 72dp | **(D-03l, founder-feedback amendment, added 2026-09-28)** The hero row — title and 48dp gear — on **every** window. Same value as the retired `home_hero_compact_min_height`, new owner: the row is no longer a compact-only shape (§4.2.4). *Retired 2026-09-29 by **D-03m**; see below.* |
| `home_stage_art_min_size` | 48dp | **(D-03l)** The smallest stage-art edge that is drawn; below it the stage is plain slack. It is what bounds the largest blank Home can show at 48 + 2 × band spacing (§4.2.4, criterion 15p) |
| `home_stage_art_max_size` | 320dp | **(D-03l)** The largest stage-art edge; above it the art is centred in its stage (§4.2.4) |
| `game_timer_pill_padding` | 12dp | **(D-05d, founder-feedback amendment, added 2026-09-28)** Horizontal padding inside the gameplay timer pill (§4.3.1) |
| `game_action_mark_size` | 24dp | **(D-04e, founder-feedback amendment, added 2026-09-28)** The on mark's circle on Show-moves On (§4.4.1) |
| `game_action_mark_glyph_size` | 16dp | **(D-04e)** The check inside the on mark |
| `game_action_ring_width` | 2dp | **(D-04e)** The Off ring and the on mark's border. A sub-4dp stroke — the same documented exception as `peg_selected_ring_width` — and a token of its own so the dock stops borrowing the board's ring width |

**Changed value** *(D-05d, 2026-09-28)*: `game_timer_pill_height` **32dp → 40dp**, and it is now
applied as a **minimum** (`heightIn`), not a fixed height, so the pill grows with the font scale
(§4.3.1).

**Retired by D-03l** *(2026-09-28)*: `home_hero_min_height` (160dp), `home_hero_art_size` (120dp)
and `home_hero_compact_min_height` (72dp) lose their callers with `FullHero`, `HomeHeroArt` and
`CompactHero`. Two new ratios live in scoped constants objects, not here (§6.4):
`HomeStageMetrics.SLAB_PADDING_FRACTION` (0.06) and `DockMetrics.UNAVAILABLE_GLYPH_ALPHA` (0.38).

**Retired by D-03m** *(2026-09-29 — amendment)*: `home_hero_row_min_height` (72dp). The hero row has
no minimum height of its own and is exactly as tall as its tallest child (§4.2.4, last subsection).
Any floor taller than the 48dp gear is an invisible band, and a floor equal to it would only repeat
`IconButton`'s own guarantee. **No token replaces it.** The gear keeps `IconButton`'s own minimum, as
it already did.

**One note on `home_stat_pill_min_height`** *(added 2026-09-28, D-03k — a fact, not a change)*: at
64dp it is a minimum that **has never bound**. The pill it sizes composes `spacing_sm` + a 20dp
glyph + `spacing_xs` + `labelMedium` + `spacing_xs` + `titleMedium` + `spacing_sm`, i.e. ≥ 84dp at
default font scale (`HomeProgressRibbon.kt:129-163`), and it measured **116dp** at 360dp width
(§4.2.2). The token is harmless and stays as it is; it is recorded here so nobody reads it as the
band's height again, which is the mistake §4.2.2's first-edition arithmetic made.

*(2026-10-21, D-03o.)* `home_stat_pill_compact_min_height` stays a floor at 40dp. Its row's "the label
wraps" now means: once any label no longer fits in half its pill, all three pills stack, and no label
or value is broken inside a word (§4.2.6). No token is added, changed or retired by this amendment.

**Reused unchanged:** `spacing_xs/sm/md/lg/xl`, `content_max_width`, `mode_tile_min_height`,
`resume_tile_min_height`, `settings_row_min_height`, `settings_row_icon_size`, `top_bar_icon_size`,
`elevation_card`, `elevation_pressed`, `elevation_dialog`, `entrance_slide_distance`,
`mode_glyph_size`, `blob_size`, `blob_drift_range`, `peg_selected_ring_width`, all five
`shape_corner_*`.

**Retired:** `top_bar_touch_target` (48dp) and `onboarding_illustration_size` (160dp) lose their
last callers. `onboarding_illustration_size` never took effect in the first place
(`BoardPreview.kt:79` overrides it) — deleting it removes a token that has been lying about the
layout since Phase 2.

### 6.2 Colour

**No new colour, no new gradient, no hex.** Every surface here maps to a role already in
`docs/ui/DESIGN-SYSTEM.md`:

| Surface | Light / Dark role |
|---------|-------------------|
| Status strip, action dock, stat pill, tutorial slab | `surfaceContainerHigh` on `background` |
| Dock loud actions (Undo, Hint) | `primaryContainer` / `onPrimaryContainer` |
| Dock quiet actions (Show-moves OFF, Restart) | transparent + `outline` border, `onSurfaceVariant` glyph |
| Show-moves ON | `primaryContainer` fill + `primary` glyph |
| Disabled Undo | `surfaceVariant` fill + `onSurfaceVariant` glyph |
| Hero title, tutorial CTA | `ctaGradientBrush()` with `onPrimary` label |
| Stars pill glyph, done pips, completion star | `goldGradientFill()` — `tertiary` stays reserved for hint/reward semantics |
| Tutorial coach ring | `primary` (the existing selection-ring role) |
| Captions, supporting lines | `onSurfaceVariant` |

*(Amended 2026-09-28 — founder-feedback amendment, D-04e, D-03l, D-05d.)* The three dock rows above
("Dock loud actions", "Dock quiet actions", "Show-moves ON") and "Disabled Undo" are superseded by
§4.4.1's five looks: **Available** — Undo, Hint and Restart alike — `primaryContainer` /
`onPrimaryContainer`; **Working** the same fill with a `primary` spinner; **Unavailable** no fill and
an `onSurface` glyph at 0.38; **On** `primary` / `onPrimary` with the on mark (`onPrimary` fill,
`primary` border and check); **Off** no fill, an `onSurfaceVariant` ring and glyph. The Home stage
slab is `surfaceContainerHigh`, like the board slab it miniaturises, and the timer pill keeps
`surfaceContainerHigh`. **Still no new colour, no new gradient, no hex.**

**The system bars take the theme** *(added 2026-10-21 — **D-05f**, app-wide)*. Edge to edge, the
system bars have no colour of the app's own. What the app decides is their **appearance**, light or dark,
and that follows the theme on every screen:

| Bar | Light theme | Dark theme |
|---|---|---|
| Status bar | Dark icons over the app's background. Since the tech-lead's F-2 fix (sign-off Decision 3, H-2) | Light icons. Since H-2 |
| Navigation bar, three-button | The platform's light scrim, dark buttons. **Unchanged** | **The platform's dark scrim, light buttons.** New; today it is a light grey scrim, `#cdccd0`, with buttons at 3.59:1 |
| Navigation bar, gesture | A dark handle | A light handle |

- **Contrast:** the navigation buttons, and the gesture handle, are ≥ 3:1 against what is behind them, in
  both themes (WCAG 1.4.11; §11). The status-bar icons are unchanged from H-2.
- **No colour is added.** The scrims are the platform's; the app chooses only light or dark, so the "no
  hex" rule and `Color.kt` are untouched.
- **The window theme sets it, not Compose,** so it holds from the window's first frame, before any
  composition, exactly as the status bar's fix does. The tech-lead has given the mechanism
  (`tech-lead-signoff-close.md`, INFO row 9): `android:windowLightNavigationBar` true in light and false
  in night. The attribute is API 27, and minSdk is 24, so it goes in `values-v27/` and
  `values-night-v27/`, each restating the whole style, because styles do not merge across qualifiers;
  a `WindowThemeTest` case pins it. On API 24–26 the platform's own navigation bar stays as it is in both
  themes; this ruling asks nothing of it.
- **Where it lands.** The window theme is a start-path file, so the change rides a branch that sits its
  own 15e: `bugfix/home-chip-arrival`, which sits one under D-03n, and `feature/candy-backdrop` only if
  that branch does not (`tech-lead-signoff-close.md`, INFO row 9). The device check covers three-button
  and gesture navigation in both themes (criterion 29).

### 6.3 Typography

M3 roles only, per the Fredoka/Nunito mapping in the design system. Home title `displaySmall`;
gameplay title and timer `titleMedium`; stat-pill values `titleMedium`; stat-pill labels
`labelMedium`; tutorial caption `bodyMedium`; all button labels `labelLarge`. Timer and every
numeric stat value use `withTabularFigures()` so digits never shift width.

*(Amended 2026-09-28 — founder-feedback amendment, **D-05d** and **D-03l**.)* Gameplay title
**`titleLarge`** (22sp) and gameplay timer **`headlineSmall`** (24sp) with tabular figures — both
Fredoka Medium, per the design system's mapping (§4.3.1). The Home title stays `displaySmall`
(36sp), now start-aligned in the hero row on every window (§4.2.4).

*(Note 2026-09-29 — tech-lead font ruling.)* Fredoka has **no `tnum` feature**: its digits are
proportional ("1" is 388 units per 1000, "2" is 587) whatever `withTabularFigures()` requests, which
is why the timer sets its digits in cells (§4.3.1, `TimerDigits`). Nunito's digits are tabular by
design (600 each). Fredoka stat-pill values are proportional; they do not tick, so nothing jitters.

### 6.4 Motion

New constants go in a scoped `TutorialAnim` object (unitless ms/floats, so not `dimens.xml`):
`STEP_ADVANCE_DELAY_MS`, `NUDGE_DELAY_MS`, `COACH_PULSE_MS`. Everything else reuses `ClayAnim` and
`GameAnim` unchanged. See §10.

*(2026-10-21, D-03n.)* Home's two slack weights (3 and 1) give way to one unitless ratio, the footer's
share of the no-chip slack (one fifth), named in the home package's scoped constants the way the two
weights were. The chips' room opens with `ClayAnim.ENTRANCE_MS` and `ClayAnim.ENTRANCE_EASING`; no new
duration or curve.

### 6.5 Cold-start budget for Home (D-03g)

**PLAT-08 C1 is a measured pass/fail at ≤ 950 ms cold start, and Home is the start destination**
— so everything added to Home in §4.2 is on the critical path. The budget below is part of the
design, not a review note.

*(Pointer added 2026-09-28, **D-03j**. The sentence above is left as it read and its premise still
holds: Home is the start destination and everything §4.2 adds is on the critical path, so the
design budget below is unchanged and still binds. What the sentence may **no longer** be read as is
this phase's **gate**. The ≤ 950 ms figure is a tracked target owned by PLAT-08 / phase 14; the bar
of record is rules/studio.md's under-2-seconds on a named mid-range physical phone, which this
section now records as **never yet measured**; and phase 15's gate is the relative one in "How C1 is
measured for this phase — **second edition**" below.)*

**Hard rules for Home's first frame.** No blocking read. No image decode. No asset load. No
bitmap. No new SDK call.

> *(Amended 2026-09-29 — tech-lead font ruling, `.planning/reports/15-founder-feedback/tech-lead-fonts.md`.)*
> **One named exception: the app's type faces.** Fredoka and Nunito ship as five static font files
> in `res/font` and load blocking on first use, during Home's first composition, so the first frame
> is drawn in the real faces and nothing lays out again when they arrive. They are the only asset
> Home's first frame reads, their cost is inside 15e's measured delta, and they replace the
> downloadable path, which drew the first frames of every cold start in the platform face and then
> laid out Home again. No other asset, font or otherwise, joins them.

Every visual this refresh adds is one of:

| Added element | First-frame cost | Verdict |
|---------------|------------------|---------|
| Hero title (gradient text) | Brush + existing `displaySmall` role | Negligible — replaces a `TopAppBar` title that is itself being deleted |
| Hero board art | One `BoardCanvas` draw of a static footprint — vector geometry, no decode | Low |
| Ribbon pills (3) | Three `Surface`s + existing `ImageVector` glyphs | Negligible |
| Ribbon **values** | **One single-row SQL aggregate behind a `Flow`** (§7 R-1/R-2, D-03h) | **The one remaining risk — see below** |
| Continue chip thumbnail | One `BoardCanvas` draw, only when a Classic resume exists | Low, and conditional |
| Mode tile art (2) | Two `BoardCanvas` draws | Low |
| `verticalScroll` on the column | Scroll state allocation | Negligible |
| Type faces (Fredoka ×2, Nunito ×3, `res/font`) | Blocking resource load of the faces the first frame draws (four of the five on Home; 50–132 KB each) | Low — measured inside 15e; **not** on the degradation path (a face is not decoration) |

Net: Home's first frame gains up to **four `BoardCanvas` draws** (hero, thumbnail, two tiles) of
static geometry, and **one new single-row Room query** behind a Flow.

**The one remaining risk, and its mitigation.** R-1/R-2 add a `level_result` read to app start.
**D-03c's placeholder rule is the mitigation, and it is mandatory, not cosmetic:** the ribbon
paints `stat_value_unknown` ("—") on the first frame and fills on the collector's first emission.
The developer must **not** gate first paint on the query, must not `runBlocking` it, must not
`first()` it on the start path, and must not introduce a Home-wide loading state to accommodate it
— a spinner on the start destination would trade a measured gate for a worse one. The pills are
sized from their labels, not their values, so the fill causes no reflow.

The *unbounded* version of this risk is retired by D-03h: the read is one aggregated row whatever
the player's Challenge history, so it can no longer be cheap in the lab and expensive on the
players who have played most. What remains is an ordinary asynchronous query, and the placeholder
rule is what keeps it off the first frame.

**Ordered degradation path if measurement shows a C1 regression.** Drop in this order, stopping as
soon as C1 passes; each step is a visual loss only and costs no oracle (§4.2.1) and no function:

1. **Mode tile art** (2 draws) — the tiles already have glyph badges and read fine without it.
2. **Continue chip thumbnail** (1 conditional draw) — reverts to the existing play-arrow accent.
3. **Hero board art** (1 draw) — reverts to Q-901 option 3, the gradient title alone.
4. **Ribbon values** — the pills stay and show "—" permanently until a later phase moves the read
   off the start path. Do **not** delete the ribbon; the Level pill costs nothing (`HomeUiState`
   already carries it) and the band is what answers the founder's "looks empty".

**Nothing in the degradation path touches the Challenge tile chip, the Continue chips themselves,
or the Classic catalog** — those are D-03f oracles and are not available to trade.

*(Amended 2026-09-28 — founder-feedback amendment, **D-03l**.)* The table's "Hero board art" row
now reads **"Hero stage art — one `BoardCanvas` draw of a constant position, only when the slack
holds ≥ 48dp — Low"**: it replaces the withdrawn peg cluster's one draw, so the budget is unchanged,
and it depends on no `HomeUiState` field, so it can never wait on a read. Degradation **step 3** now
reads **"Hero stage art (1 draw) — the stage stays as plain slack"**. Be clear about its price:
that step brings back **the very blank band the founder objected to** (≈150–280dp on a 6.7-inch
phone, §4.2.4) and fails criterion 15p, so it is taken only if a measured sitting forces it, and
taking it sends 15p back to this role with the numbers rather than closing it. Q-901's "option 3",
which this step used to name, no longer exists: the hero is a title row either way.

*(2026-09-29.)* If 15e fails on the build that first bundles the faces, the tech-lead rules on their
share before any step is taken: the steps remove art, and removing art cannot pay for type.

**Handoff:** this is a design budget, not a measurement. The developer measures C1 with the fuller
Home before the phase closes; the qa-engineer checks the numbers at the gate.

*(Pointer 2026-09-28, D-03j: **not before this phase closes.** The measurement moves to the
follow-up slice that lands §4.2.2, because a delta measured against a layout that has since changed
is not evidence. The division of labour is unchanged — the developer measures, the qa-engineer
checks — on that slice's sitting.)*

**How C1 is measured for this phase** *(2026-09-21 — tech-lead ruling,
`tech-lead-architecture.md` §3, folded in)*. The Phase-6 reference of 633 ms is **pre-SDK**: UMP,
AdMob, Play Games, leaderboards and cloud save all landed after it, so a Phase-15 developer who
compared against it would be charging five phases of SDK cost to a UX refresh. Phase 15's own gate
is therefore a **two-arm delta**, and the absolute ≤ 950 ms stays PLAT-08's:

> ⚠️ **Superseded in part on 2026-09-28 — read the second edition below first.** Three parts of this
> first edition **stand unchanged** and are *not* repeated there: the **launch identity** the arms are
> started with (second bullet), the **seeded ~200-level ladder** (third bullet), and **where the
> numbers go** (last bullet). Two parts are **retired**: the *blocked* arm order (ten samples of one
> arm, then ten of the other) and the **pass condition** (≤ 950 ms absolute *and* ≤ 50 ms delta).
> The text is kept verbatim as the record of what was asked for and why it was overturned; it is not
> the contract. Its own instinct — that the absolute stays PLAT-08's — is what D-03j generalises.

- **Two arms, same AVD, same host, same sitting, back to back:** arm A = `develop` (control),
  arm B = `feature/phase-15-ux-refresh`. Phase 6's instrument exactly — AVD `Pixel_9a` API 36,
  **debug** variant, `am start -W` TotalTime, 10 samples, first discarded as warm-up, re-install
  between arms so neither benefits from the other's warm ART state.
- **The identity the arms are started with** *(correction 2026-09-21 — this spec never named it,
  and the id in `tech-lead-architecture.md:360-362` is the wrong one)*: the launch package is the
  **`applicationId` `com.erman.pegsolitarie`** (`app/build.gradle.kts:279` — note "-arie"), and the
  activity class is **`com.app.pegsolitaire.MainActivity`** (`MainActivity.kt:1` and `:47`), because
  the code `namespace` (`app/build.gradle.kts:263` — "-aire") is deliberately a different string
  from the store identity and the manifest declares the activity relatively (`AndroidManifest.xml:33`,
  the one `<activity>`, no alias, no `applicationIdSuffix`). The component is therefore
  **`com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity`**, spelled out on *both* sides; the
  `/.MainActivity` shorthand expands against the launch package and so names a class that does not
  exist here. `am force-stop` takes the `applicationId` alone. This is the one failure mode that
  costs the gate its evidence rather than its pass: a wrong id makes `am start -W` print an error
  instead of a `TotalTime`, so the phase closes with **no measurement** rather than with a bad one.
  The form that cannot rot, and the one to prefer:
  `adb shell am start -W -n "$(adb shell cmd package resolve-activity --brief com.erman.pegsolitarie | tail -1)"`
  — it asks the device for the launcher component instead of hard-coding it. Criterion 15e's
  `am start -W` means this command.
- **The player state is identical across arms and deliberately non-trivial:** seed a Challenge
  ladder of at least ~200 banked levels, or the one read this phase adds is measured on an empty
  table and proves nothing about a real player.
- **Pass condition, both parts:** (1) after-median ≤ **950 ms**, and (2)
  (after-median − before-median) ≤ **50 ms**. 50 ms is this instrument's noise floor on this AVD,
  not a guess — Phase 6's nine samples spanned 603–649 ms around a 633 ms median.
- **If (2) fails**, walk the ordered degradation path above and re-measure after each step,
  stopping at the first pass. **If (1) fails while (2) passes**, the regression is not this
  refresh's: it escalates to the producer as PLAT-08/Phase-14 work rather than being paid for out
  of this phase's visual budget.
- **Where the numbers go:** `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, section
  `## Cold-start measurement`, one table per arm in the identical field shape as
  `06-BUILD-EVIDENCE.md:315-323`, plus the seeded-ladder row and the same precision caveat. The
  phase does not close without both tables. Criterion 15f (placeholders, no spinner, no reflow) is
  a separate visual check at the same sitting — the timing number does not cover it.

**How C1 is measured for this phase — second edition** *(2026-09-28 — tech-lead ruling,
`.planning/reports/phase-15/tech-lead-coldstart-gate.md` §3/§5, folded in. A gate **procedure**, so
a named subsection and not a decision — the same call the first edition made. The one part of the
ruling that **is** a contract change is filed as **D-03j** in §3.)*

**Why it changed, in one paragraph.** The first edition's ≤ 50 ms delta threshold was Phase 6's
**46 ms within-arm range** — nine samples of *one* build, 603–649 ms around a 633 ms median
(`06-BUILD-EVIDENCE.md:319-320`) — applied to a **between-arm median difference**. Those are
different statistics, and the second is the noisier, because it also absorbs everything that drifts
between the arms. Two null experiments settled it: the same build measured against itself, where the
true delta is zero, returned **57 ms** (`15-BUILD-EVIDENCE.md:52-53`) and **59 ms**
(`qa-engineer.md:162-163`) — **5.8%** and **3.3%** of their own pooled medians. An instrument that
reports 57–59 ms between a build and itself cannot answer "is the delta ≤ 50 ms?", and keeping the
question would have forced either a fabricated pass or a permanent block. Both nulls were taken on
the **same AVD**, and their absolute medians differ by **1.83×** (957 ms against 1832 ms) with
nothing changing but host load — which is why the absolute reading is retired here (D-03j) and the
relative one, which held within 2 ms across that same 1.83× swing, is kept.

**Which builds this gate applies to — read this before booking an emulator.**

| Build | 15e |
|-------|-----|
| The phase-15 build audited on 2026-09-28 (the fixed `Spacer` at `HomeScreen.kt:311-313`) | **Not run — `:NOT RUN` is the correct record.** Home's layout mechanism changes in the follow-up slice (D-03i, §4.2.2), so a delta measured against this layout would be superseded before anyone read it. **Phase 15 closes without a 15e number, on this ruling and not on an omission.** |
| **The follow-up slice that lands §4.2.2 / D-03i** | **This is where 15e runs**, once, arm A = `develop`. It is a **closing condition of that slice**, and of no other. |
| Any later branch that changes Home's first frame | The same protocol, unchanged. |

*(Pointer 2026-09-29 — tech-lead font ruling, Ruling 2.)* The follow-up slices landed inside phase
15, on `feature/phase-15-ux-refresh`. 15e runs **once**, with arm A = `develop` and arm B = that
branch's head **after both D-03m and the bundled fonts**, because both change Home's first frame. A
sitting on an earlier head is not the gate's reading.

*(2026-10-18.)* Arm A is the merge-base of `develop` and the branch at the sitting, so A..B is exactly
the branch's diff; arm B is the head that also carries the light-status-bar fix (sign-off H-2). A
later `develop` merge-in leaves the sitting standing unless it changes a start-path file.
*(Tech-lead phase-15 sign-off, Decision 1, `.planning/reports/15-founder-feedback/tech-lead-signoff.md`;
folded 2026-10-21 by the ux-designer. The sign-off names the start-path files: `App.kt`,
`MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`,
`res/values/themes.xml` and `AndroidManifest.xml`. "Arm A = `develop`" in the table and the pointer
above is superseded by this sentence and kept as the record. Phase 15's sitting was sat on
2026-10-18 on `Pixel_9a_bench` and passed: delta 33.0 ms, floor 3.0 ms, arm A `c3d3437`, arm B
`ee16a2a`, `.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`.)*

*(Note 2026-10-21 — ux-designer, on the start-path list just above.)* The list is Decision 1's as the
tech-lead wrote it, which predates H-2's `res/values-night/themes.xml`. The tech-lead's close report
already treats that file as a start-path file (its Checks, `tech-lead-signoff-close.md`) and calls the
two theme files D-05f adds, `values-v27/` and `values-night-v27/`, start-path files too (INFO row 9).
So "`res/values/themes.xml`" in the list reads as **every `res/values*/themes.xml`**: a merge-in that
changes any of them does not leave a sitting standing. If the tech-lead's `docs/tech-design.md` §10
states the list differently, that text governs.

**Preconditions — every one recorded with its value in the evidence table. If any fails, the
sitting is VOID, not failed.**

1. AVD `Pixel_9a`, API 36, `:app:installDebug`; OS animations off (Developer options → Animator
   duration scale: off), the same device state criteria 7c and 25 are run in. *(Added 2026-09-29.)*
   A sitting on any other AVD is VOID; the `Pixel_9` is not a substitute (tech-lead-fonts.md,
   Ruling 2).

   *(Amended 2026-10-18 — tech-lead phase-15 sign-off, Decision 1, on the qa-engineer's loop-3 E-1:
   the `Pixel_9a`'s `/data` was 95 % full and refused both arms.)* The AVD is **`Pixel_9a_bench`**:
   the `Pixel_9a`'s hardware profile and system image (`android-36.1` `google_apis_playstore_ps16k`,
   arm64, 16 KB pages, 4 cores, 2 GB RAM, 1080×2424 at 420 dpi) with a 16 GB data partition, used for
   no other testing. A sitting on any other AVD is VOID; neither the `Pixel_9` nor the `Pixel_9a` is
   a substitute.

   *(Amended 2026-10-21 — tech-lead, phase-15 close, D-1.)* The bench may be made with `avdmanager`
   or by copying `Pixel_9a.avd`'s `config.ini` and `.ini`. Either way each sitting records the diff
   against `Pixel_9a.avd`. Only the name, the paths and `disk.dataPartition.size` may differ; any
   other difference makes the sitting VOID.

   *(Folded 2026-10-21 by the ux-designer, from
   `.planning/reports/15-founder-feedback/tech-lead-signoff.md` "Text for other roles' files" and
   `tech-lead-signoff-close.md` fold b; both amendments are the tech-lead's wording, applied as
   given.)* Superseded and kept as the record: "AVD `Pixel_9a`" at the head of this precondition, and
   the 2026-09-29 sentence "A sitting on any other AVD is VOID; the `Pixel_9` is not a substitute".
   Everything else in this precondition stands: API 36, the debug variant, OS animations off.
2. **Exactly one emulator running.** A second `Pixel_9` must be shut down, not merely idle.
   *(2026-10-21 pointer — sign-off Decision 1, P-4.)* With the bench as the instrument, that means
   the `Pixel_9a` and the `Pixel_9` are both shut down, and the bench alone runs.
3. **No concurrent Gradle build in any worktree**, and no other agent session building or
   measuring on this host.
4. `uptime` recorded **before** the sitting, with the emulator idle on the launcher for ≥ 60 s, and
   **after** it, 60 s after the last sample with the app force-stopped; the **1-minute load average
   ≤ half the host's logical cores** (`sysctl -n hw.ncpu` ÷ 2; **5.0** on the studio's 10-core host)
   at both ends. *(Amended 2026-09-29: the earlier "< 2.0" was unattainable on this host and
   stricter than decidability needs; the null control is the noise guard.)*

   *First wording, superseded 2026-09-29 and kept as the record:* "`uptime` recorded **before and
   after** the sitting; the **1-minute load average < 2.0 at both ends**."
5. ART state normalised on **both** arms before measuring —
   `adb shell cmd package compile -m speed -f com.erman.pegsolitarie` — so JIT/AOT warm-up is not a
   free variable between the arms.
   *(Note 2026-10-21 — tech-lead, phase-15 close, "Precondition 5, as QA read it, is accepted";
   folded by the ux-designer.)* A debuggable APK stays at `[status=verify]` after
   `compile -m speed -f`. The precondition holds when both arms read the same status: equal ART
   state between the arms is what it protects.
6. The launch component **resolved from the device, never assumed** — the first edition's
   `cmd package resolve-activity --brief` form, unchanged, and still the one failure mode that
   costs the gate its evidence rather than its pass.
7. **Each arm swapped in only at a block boundary.** Both arms carry `applicationId`
   `com.erman.pegsolitarie` and cannot be installed side by side, so the arm under measurement is
   installed at the start of its block — `adb install -r -d --no-incremental <arm>.apk`, then
   `cmd package compile -m speed -f com.erman.pegsolitarie`, then `am force-stop` — and never
   between samples; the installed `base.apk`'s sha256 is recorded after every swap and must equal
   that arm's. The null control swaps its one build back in at each of its block boundaries, so the
   floor contains whatever a swap costs. Valid only while both arms share the Room schema
   (`git diff <A>..<B> -- app/schemas` empty) and the DataStore keys; otherwise the sitting is VOID.
   *(Amended 2026-09-29, tech-lead ruling on qa-engineer E-1 — "both arms installed and kept
   installed" was a tech-lead error: the arms share one `applicationId`.)*

   *First wording, superseded 2026-09-29 and kept as the record:* "Both arms **installed and kept
   installed** for the whole sitting."
8. The first edition's seeded **~200-level** Challenge ladder, identical on both arms.
9. `/data` free ≥ 4 GB (`dumpsys diskstats`, `Data-Free`) at the start and the end of the sitting,
   both recorded. *(Added 2026-10-18 — tech-lead phase-15 sign-off, Decision 1 (P-3); folded
   2026-10-21 by the ux-designer.)*

*(Note 2026-09-29 — reading precondition 1 beside 3 and 7, under the tech-lead's Ruling 2.)*
`:app:installDebug` in precondition 1 and in criterion 15e names the **debug variant**, not a command
run during the sitting. Both arms are built beforehand with `:app:assembleDebug`, each in its own
clean worktree, and the daemons are stopped. Each arm is then installed only at a block boundary, by
precondition 7's `adb install`, so no Gradle process runs while the sitting is timed (precondition
3). The full procedure, with every step and what it records, is Ruling 2 in `tech-lead-fonts.md`.
*(2026-10-21 pointer.)* Ruling 2 as amended by the tech-lead's phase-15 sign-off, Decision 1 — P-1 the
bench, built once; P-2 the arms; P-3 storage, precondition 9; P-4 the quiet host — and by the close
report's D-1 (`.planning/reports/15-founder-feedback/tech-lead-signoff.md`,
`tech-lead-signoff-close.md`). Later sittings do not rebuild the bench; each records its `config.ini`
diff and both `Data-Free` readings again.

**Protocol — three steps, in this order.**

1. **Null control first, in the same sitting, under the identical protocol, with the *same* build in
   both positions.** Record `floor = |median difference|`. This is the developer's own unprompted
   null experiment promoted from a precheck to the protocol's first step.
2. **Interleave the real arms:** A/B/A/B in blocks of five, **≥ 20 recorded samples per arm**, the
   first sample of each block discarded, `am force-stop` plus a 2 s pause between samples. **Do not
   run one arm to completion and then the other** — blocked arms are exactly what the two nulls
   measured, and what produced the 57–59 ms floor.
3. Arm A = the `develop` build; arm B = the branch build.
   *(2026-10-21 pointer — tech-lead phase-15 sign-off, Decision 1.)* Arm A is the merge-base of
   `develop` and the branch at the sitting, not `develop`'s head (the 2026-10-18 pointer under
   "Which builds" above). The words above are kept as the record.

**Verdict — one comparison, three outcomes, no second judgement call.**

| Measurement | Verdict |
|-------------|---------|
| `median_B − median_A ≤ max(2 × floor, 100 ms)` | **PASS** |
| `median_B − median_A > max(2 × floor, 100 ms)` | **FAIL**, and it is a real regression: walk the ordered degradation path above and re-measure the **whole** sitting, null control included, after each step, stopping at the first sitting that passes. |
| `floor > 150 ms`, whatever the arms say | **VOID** — the host was too noisy to decide. Re-run later. A void sitting is **not a pass and not a fail and may not be reported as either**, and the arm numbers from a void sitting are not evidence of anything. |

On the two floors measured so far the threshold lands at **114 ms** and **118 ms**
(`max(2 × 57, 100)`, `max(2 × 59, 100)`) — comfortably inside the scale of regression a Home
refresh could plausibly cause, and it claims no 50 ms resolution that nobody has.

**This raises the bar; it does not lower it.** The threshold is derived from a control measured
alongside the comparison, so a noisy host can no longer buy a pass — it buys a **void** and a
repeat. Under the first edition a noisy host produced a number whose sign was luck and which a
tired reader would have taken for a result. The trade is deliberate: **fewer decidable sittings, no
undecidable verdicts.** "It passed" can no longer mean "the host was too noisy to tell".

**Where the numbers go.** The first edition's location and field shape are unchanged —
`.planning/phases/15-ux-refresh/15-BUILD-EVIDENCE.md`, section `## Cold-start measurement`, one
table per arm in `06-BUILD-EVIDENCE.md:315-323`'s shape, with the seeded-ladder row and the same
precision caveat — recorded on the slice that runs it. **Added:** the **null-control table**, the
eight preconditions with their recorded values, both `uptime` readings, and the computed `floor` and
threshold. A sitting reported without its null table cannot be checked by anyone and counts as
**void**.
*(2026-10-21 pointer — tech-lead phase-15 sign-off Decision 1 and close D-1; folded by the
ux-designer.)* "Eight preconditions" reads **nine** since precondition 9 was added on 2026-10-18.
Each sitting also records the bench's `config.ini` diff against `Pixel_9a.avd` (precondition 1;
a diff with any extra key makes the sitting VOID) and both `Data-Free` readings (precondition 9).

**Criterion 15f does not travel with 15e.** 15f is the visual check that Home's first paint shows
"—" placeholders with no spinner, no blank band, no blocked screen and no reflow as the values
arrive. It is not a timing measurement, it needs none of the preconditions above, and it stays a
**phase-15 sign-off item on this build**; the tech-lead's ruling is explicit that it does not rule
on 15f. What the ruling does name is why 15f is weaker than it looks: no instrument in the repo can
see time-to-full-display, and the placeholder frame's window is shorter than QA's 250 ms screenshot
interval (`qa-engineer.md:144`), so 15f is verified today by reading plus observation rather than by
capture. The instrument that would make it decidable — Jetpack Macrobenchmark's
`StartupTimingMetric` plus `reportFullyDrawn()` in `MainActivity` — is new infrastructure, is the
producer's to schedule, and is not this phase's or this document's to specify.

**The ≤ 2 s launch bar, and the fact that it has never been measured on a phone** *(named gap —
added 2026-09-28, D-03j, from the tech-lead ruling's Open issues. Recorded here because §6.5 is
where this document keeps its non-functional launch targets, and because an unmeasured bar everyone
assumes is met is worse than a gap with a name.)*

- **The bar.** rules/studio.md's product baseline: the product "launches in under 2 seconds on a
  mid-range phone". D-03j makes that the bar of record for launch, and a release must meet it.
- **The gap.** It has **never been measured for this app.** Every cold-start number in this repo is
  from an Android emulator on a shared host: Phase 6's 633 ms, this phase's 957 / 1014 ms, QA's
  1832 / 1773 ms. Since the *same* AVD returned 957 ms and 1832 ms in two sittings with nothing
  changing but host load, an emulator number is evidence about the host and not about the app —
  QA's ~1.8 s is **neither a pass nor a fail** against the 2 s bar and may not be reported as
  either, in either direction.
- **Owner.** PLAT-08 / phase 14, with the reference device named in the phase-14 release evidence.
- **The tech-lead's recommendation, recorded as given:** phase 14 measures the bar on the physical
  device the phase-8 checklist used, and names that device in the release evidence.
- **The decided default if no physical device is available at phase 14:** the release proceeds on
  the interleaved emulator A/B gate plus the scheduled Macrobenchmark task, with the absolute bar
  **recorded as unverified in the release evidence rather than silently passed**. Unverified is a
  legitimate record; a silent pass is not.
- **Where the money question goes.** Buying a mid-range test phone is money and therefore the
  founder's call, not a staff decision. No founder question is opened from this document: the
  tech-lead routed it into the **phase-14 release track** rather than opening a second thread, and
  this role is not re-routing it. The producer folds it in there.
- **What this document owes meanwhile.** Nothing in §4.2 or §4.2.2 may assume the bar is met.
  D-03g's hard rules for Home's first frame — no blocking read, no decode, no asset load, no new
  SDK call, placeholders first — are the design-side answer and hold whichever device the number
  eventually comes from.

---

## 7. Data requirements (for the tech-lead and developer)

Nothing here needs a schema change, a migration, a new DataStore key or a DI change. That sentence
survives D-03h unchanged — a new `SELECT` over existing columns is not a schema change, and the
exported Room schema and its version are untouched. What D-03h does cost, stated so the planner is
not surprised by it: one new `@Query`, one two-field projection, one new `ProgressRepository`
member — and therefore a one-line override in each of the repository's test doubles, of which the
tech-lead counted 16. That cost is mechanical and the compiler names every site.

| # | Requirement | Why it is cheap |
|---|-------------|-----------------|
| **R-1** ⚠️ | `HomeUiState` gains `totalStars: Int?`. `HomeViewModel` reads `ProgressRepository.observeHomeProgress()` — a `Flow` over **one** SQL aggregate (`LevelResultDao.observeHomeProgress`) that computes both ribbon values in a single row. | `ProgressRepository` is already injected (`HomeViewModel.kt:86`); the aggregate is the `Flow` sibling of the existing `totals()` shape. `null` until the first emission → the pill shows "—" (D-03c), and a **failed** read also emits unknown, so the pill never shows `0` for a broken read (D-03h). **⚠️ COLD-START (D-03g):** this is the only new read on the start destination. Nullable-and-placeholder is mandatory — no `runBlocking`, no `first()` on the start path, no Home-wide loading state. Measure C1 per §6.5. |
| **R-2** ⚠️ | `HomeUiState` gains `classicBoardsCleared: Int?`, from the **same single query**, whose `classicBoardsCleared` field is mode-filtered and counts rows with `stars >= ProgressConstants.ANY_STARS`. Denominator is the 5 `ClassicBoard` enum values / `Boards.CLASSIC_SPECS.size` — never a stored count. | One query, two fields, one collector: do not add a second collection for the second pill. **Must not** be a Kotlin fold over `observeResults` — over `MODE_CHALLENGE` that is the whole unbounded ladder in memory on the start destination, which `LevelResultDao.kt:41-47` already rules out for a *background* submitter (D-03h). **Must not** use `aggregate().totalStars` — deliberately both-modes with no `WHERE` (`LevelResultDao.kt:66`) and a `suspend` one-shot that goes stale after every solve. **⚠️ Same cold-start note as R-1.** |
| **R-3** | `HomeScreen` loses the `onShowHowToPlay` parameter; `SettingsScreen` gains an `onShowHowToPlay` parameter; `AppNavHost` routes the tutorial from Settings and from first run. | Mechanical. `HomeRoute.kt:48` moves to the Settings route. |
| **R-4** | **Closed 2026-09-21** (game-designer ruling). All three footprints are fixed in §4.1 and proven dead-end-free to one peg. They live in a new `object TutorialBoards` in `:domain` beside `Boards.kt` and **must not** join `Boards.CLASSIC_SPECS`. | The proof is exhaustive enumeration over the product's own `RulesEngine`; the invariant is guarded by a permanent `:domain` test, not by a report (§13). |
| **R-5** | **Resolved 2026-09-21** (tech-lead ruling): a Hilt `TutorialViewModel` scoped to the `Tutorial` nav destination; step state in a plain `MutableStateFlow`, **not** `SavedStateHandle`. | A saved cursor would survive process death and resume at T2, contradicting D-01e. Destination scoping is also what makes a Settings replay restart at T1 (§4.1, D-01g). |
| **R-6** | The two derived pill values reach the UI as a UI-layer read model, the way `LevelResult` does today — no Room type reaches a ViewModel. | Stated so the ribbon's contract survives a refactor of the query behind it. |

**Invariant the hosting mechanism must preserve (D-01f).** `HomeViewModel` sequences three
first-run surfaces — onboarding, then the Play Games opt-in prompt, then the profile-swap dialog —
and the latter two are gated on the **persisted** `onboardingSeen` flag, not on overlay visibility
(`HomeViewModel.kt:119-128`, `:145-160`). Moving the tutorial off Home therefore preserves the
sequencing by construction, **provided `markSeen()` is still written before the player returns to
Home**. `HomeScreen.kt:198`'s `!state.onboardingVisible` guard loses its meaning once Home hosts no
overlay; whatever replaces it must keep the same one-way safety — the wipe question may be
withheld, never raised early.

**Resolved** *(2026-09-21 — tech-lead ruling)*: the tutorial is a `@Serializable` nav destination;
`markSeen()` is **awaited before the pop**, issued on the application scope so a cancelled screen
cannot drop it; and the guard survives in shape as a standing boolean on `HomeUiState`
(`!state.tutorialPending`, the mirror of `!seen`). The player-visible reason this ordering is
binding is in D-01g: a fire-and-forget write that loses its race leaves `tutorialPending` true, so
Home re-pushes the tutorial on the next composition and the player cannot get out — and on the
launch after a dropped write, both the Play Games opt-in and the profile-swap question are
withheld for an extra launch.

---

## 8. Copywriting contract

### New strings

| Name | Value | Where |
|------|-------|-------|
| `tutorial_step_progress` | `Step %1$d of %2$d` | Pip row content description |
| `tutorial_step_1_caption` | `Tap a peg, then tap the empty hole two cells away.` | T1 |
| `tutorial_step_2_caption` | `Jumps work up, down, left and right — never diagonally.` | T2 |
| `tutorial_step_3_caption` | `Keep jumping. Clear the board down to one peg.` | T3 |
| `tutorial_done_caption` | `That's the whole game. Go clear a board.` | Completion |
| `settings_how_to_play_label` | `How to play` | Settings row |
| `settings_how_to_play_supporting` | `Replay the tutorial` | Settings row |
| `stat_value_unknown` | `—` | A ribbon pill with no value yet — or whose read failed (D-03h); the two render identically and neither ever renders `0` |
| `stat_value_unknown_desc` | `not yet available` | Its announcement |
| `home_stat_stars_label` | `Stars` | Ribbon pill label |
| `home_stat_boards_label` | `Boards` | Ribbon pill label |
| `home_stat_level_label` | `Level` | Ribbon pill label |
| `home_stat_boards_value` | `%1$d/%2$d` | Boards pill value |

The three new tutorial captions are **rewrites** of `onboarding_step_1/2/3`
(`strings.xml:161-163`) shortened to one line each, because the player is now doing the thing the
sentence used to describe. `tutorial_step_2_caption` ("never diagonally") is **earned** by T2's
footprint: the full 3×3 puts four diagonal holes and two adjacent holes visibly in play and gives
none of them a destination dot, so the sentence describes something the player can see withheld.

**Retired before it shipped.** `tutorial_undo_nudge` ("Boxed in? Undo takes your last jump back.")
was in the first edition's table and is **gone** — the tutorial has no Undo control to point at,
and the game-designer's boards make being boxed in impossible, so the line named a control the
player cannot see and a situation that cannot occur. The escalation re-shows the step's own
caption instead (§4.4), so the copy contract carries one string fewer and needs no per-step
escalation line.

**Added 2026-10-21 — D-05e, the timer's spoken form (§4.3.2).** Two strings and two plurals, plus a
third string only if the fallback is taken:

| Name | Value | Where |
|------|-------|-------|
| `elapsed_time_spoken` | `%1$s, %2$s` | The timer's content description: `elapsed_time`, then the spoken duration |
| `duration_minutes` (plurals) | one: `%d minute` · other: `%d minutes` | The minutes part |
| `duration_seconds` (plurals) | one: `%d second` · other: `%d seconds` | The seconds part |
| `duration_minutes_seconds` | `%1$s, %2$s` | The minutes part, then the seconds part, when both are said |
| `duration_under_a_minute` | `under a minute` | **Only with D-05e's whole-minute fallback**, if criterion 18g finds TalkBack re-speaking the timer; otherwise it is not added |

`elapsed_time` ("Elapsed time") is unchanged and is the label `elapsed_time_spoken` starts with.
`home_stat_pill_desc` has the same `%1$s, %2$s` value and is **not** reused: it is Home's "label, value"
sentence, and sharing it with a gameplay duration would give one string two owners. D-03n and D-03o add
no string.

### Reused unchanged

`onboarding_got_it` (the completion CTA), `onboarding_skip` (the Skip action), `app_name`,
`navigate_back`, `elapsed_time`, `challenge_level_indicator`, `challenge_level_indicator_desc`,
`loading_level`, `undo_action`, `hint_action`, `hint_thinking`, `show_moves_on`, `show_moves_off`,
`restart_board`, `classic_tile_title`, `classic_tile_supporting`, `challenge_tile_title`,
`challenge_tile_continue`, `resume_classic_title`, `resume_classic_supporting`,
`resume_challenge_title`, `resume_challenge_supporting`, `settings_action`, `catalog_stars_earned`.

### Deleted

`how_to_play` (`strings.xml:62`) — its only caller is the deleted info button. Removing it is what
makes D-02 complete rather than cosmetic.

`onboarding_step_1`, `onboarding_step_2`, `onboarding_step_3` (`strings.xml:161-163`) — deleted in
the same slice that deletes `OnboardingOverlay.kt`, on the same precedent as `how_to_play`: a
string with no referent does not stay in the file. Their only caller is
`OnboardingOverlay.kt:181-183`, which D-01/D-01g replace, and the three `tutorial_step_*_caption`
rows above are their rewrites, so nothing re-reads them. *(Verified 2026-09-21 by grep over
`*.kt`/`*.xml`: the three names appear at `strings.xml:161-163` and
`OnboardingOverlay.kt:181-183` and nowhere else in the app — no other layout, screen, test or
resource references them. Recorded here so the deletion is a ruling of the copy contract rather
than a discovery at review; the first edition's "rewrites" note above implied replacement without
saying what happened to the originals.)*

**Not deleted with them:** `onboarding_got_it` and `onboarding_skip` keep their names and their
values and move to the new tutorial (Reused unchanged, above). The shared `onboarding_` prefix is
not a licence to sweep the group.

`show_moves_off` ("Hide possible moves", `strings.xml:96`) — *deleted 2026-09-28 by the
founder-feedback amendment, D-04e.* Show-moves becomes a switch whose name is `show_moves_on`
("Show possible moves") in both states, with the state announced by the switch role, so the off
string has no caller; it also named the opposite of what a tap on the off control does (§4.4.1).
`show_moves_on` stays, value unchanged, and moves from "Reused unchanged" into service as the
toggle's constant name. **The amendment adds no string**: the stage has no text, the on mark and the
slash are drawn, and the strip's larger type reuses its existing strings.

### Fixed

`settings_sound_supporting` — "Saved for the upcoming sound effects" is stale since Phase 7.

---

## 9. Assets and licences

Every asset this refresh needs, with its licence. **No third-party asset is introduced, so no new
`THIRD_PARTY.md` or `docs/audio/CREDITS.md` row is required.** *(Amended 2026-09-29.)* The fonts are
the exception: bundled, they are a third-party asset in the artifact and need their `THIRD_PARTY.md`
rows (text in `tech-lead-fonts.md`).

| Asset | Kind | Source | Licence |
|-------|------|--------|---------|
| `Icons.AutoMirrored.Rounded.ArrowBack` | vector | `material-icons-core`, already a dependency | Apache-2.0 (notice already shipped) |
| `Icons.Rounded.Refresh` | vector | `material-icons-core` | Apache-2.0 |
| `Icons.Rounded.Settings` | vector | `material-icons-core` | Apache-2.0 |
| `Icons.Filled.Star` | vector | `material-icons-core` | Apache-2.0 |
| `UndoIcon` | vector | in-house, `ui/game/UndoIcon.kt` (existing) | project-owned |
| `rememberClassicGlyph`, `rememberChallengeGlyph` | vector | in-house, `ui/home/ModeGlyphs.kt` (existing) | project-owned |
| **`HintIcon`** (lightbulb) | vector | **new, in-house** — core set has no lightbulb and the extended set is banned | project-owned |
| **`ShowMovesIcon`** (peg + two dots) | vector | **new, in-house** — core set has no visibility glyph | project-owned |
| Hero / tile / thumbnail board art | drawn | `BoardCanvas` primitives, no asset file | project-owned |
| `SoundCue.Jump` (`R.raw.sfx_jump`, `SoundCue.kt:30`) + `SoundCue.PegRemove` (`R.raw.sfx_peg_remove`, `:33`) | audio | already in `res/raw`, credited in `docs/audio/CREDITS.md` — **newly placed on the tutorial screen** by D-01i, as the existing pair at existing levels; no asset, no volume and no pitch value changes | CC0 1.0 (as recorded) |
| `SoundCue.ButtonForward`, `SoundCue.ButtonDismiss` (`SoundCue.kt:70-74`) | audio | already in `res/raw`, credited — newly placed on the Settings "How to play" row (§4.5, `ButtonForward`), the tutorial's completion CTA (`ButtonForward`) and `Skip` (`ButtonDismiss`), all by the existing `withClickSound` idiom (D-01i, D-11) | CC0 1.0 (as recorded) |
| `SoundCue.Select`, `Deselect`, `Undo`, `InvalidTap`, `Hint`, `Win`, `StarPop` | audio | already in `res/raw`, credited — **unchanged, and explicitly never played on the tutorial screen** (D-01h as amended by D-01i, §5) | CC0 1.0 (as recorded) |
| Fredoka, Nunito *(superseded 2026-09-29 by the next row; kept as the record)* | font | downloadable Google Fonts, already wired | OFL (already shipped) |
| Fredoka Medium/SemiBold, Nunito Regular/SemiBold/Bold | font | **bundled** static instances in `app/src/main/res/font/` (Google Fonts, unmodified; tech-lead font ruling 2026-09-29) — replaces the downloadable-font path | OFL-1.1, no Reserved Font Name; notice and licence ship in the APK (`assets/licenses/`); **one `THIRD_PARTY.md` row per family** |

Both new vectors follow the `UndoIcon.kt` / `ModeGlyphs.kt` precedent: a 24-unit viewport, filled
opaque black, recoloured by the host `Icon`'s tint, `@Suppress("MagicNumber")` on the path data
with the same justification comment the existing two carry.

*(Added 2026-09-28 — founder-feedback amendment, D-03l / D-04e.)* Three rows join the table, none
third-party and none needing a `THIRD_PARTY.md` row beyond what already ships:

| Asset | Kind | Source | Licence |
|-------|------|--------|---------|
| `Icons.Rounded.Check` — the check inside Show-moves' on mark | vector | `material-icons-core`, already a dependency | Apache-2.0 (notice already shipped) |
| **`ShowMovesOffIcon`** — `ShowMovesIcon` with a knocked-out diagonal slash | vector | **new, in-house**, same precedent as the two above; it brings one more `@Suppress("MagicNumber")` on path data, which the code-auditor sees under the existing precedent rather than as a new kind of suppression | project-owned |
| Home stage art — a mid-game English-33 position on a clay slab | drawn | `BoardCanvas` primitives, no asset file; replaces the withdrawn peg-cluster art | project-owned |

---

## 10. Animation and motion

All new motion reuses the Candy Clay vocabulary; nothing invents a curve.

| Moment | Motion |
|--------|--------|
| Any tappable clay surface pressed | `Modifier.claySquish()` — unchanged |
| Home bands entering | `Modifier.clayEntrance(order)`, 40ms stagger: hero 0, ribbon 1, Continue chips 2..3, tiles next |
| Dock entering with the board | `clayEntrance` after the board slab |
| Show-moves toggling ON/OFF | pill fill cross-fades over `ClayAnim.PRESS_IN_MS` |
| Tutorial coach ring | the **existing** selection pulse (`BoardMetrics.PULSE_MAX_SCALE`, `GameAnim.PULSE_MS`, reverse-repeat) — the tutorial reuses the board's own idiom rather than adding a highlight |
| Tutorial step advance | caption cross-fade + pip fill, then the next footprint after `TutorialAnim.STEP_ADVANCE_DELAY_MS` |
| Tutorial nudge escalation | one `TutorialAnim.COACH_PULSE_MS` pulse, `TutorialAnim.NUDGE_DELAY_MS` after the last non-progressing tap |
| Tutorial completion star | `ClayAnim.STAR_POP_MS` + `overshootSpring()` + `goldGradientFill()` |
| Hero board art | idle — no ambient motion of its own; the two existing `HomeBlobs` remain the only drifting elements |
| **Hero stage art** *(2026-09-28, D-03l — replaces the row above)* | static on every frame: no selection, no dots, no hint, so nothing on the board pulses or pops; enters with the hero (`clayEntrance(HERO_ENTRANCE_ORDER)`); the blobs remain the only drifting elements |
| **A Continue chip arriving or leaving after first paint** *(2026-10-21, D-03n)* | the chip's room opens from 0 over `ClayAnim.ENTRANCE_MS` with `ClayAnim.ENTRANCE_EASING`, no overshoot; the stage gives up the same height in the same frames, its art following the stage's size (the pegs on it do not move relative to the board), and the ribbon rides on the stage's edge; the chip enters with its existing `clayEntrance` but with no stagger delay, its fade starting with its room; the hero row and the mode tiles hold still in every frame; the entrance cascade above is independent of it and is never restarted by it (§4.2.5) |
| **Show-moves On ↔ Off** *(2026-09-28, D-04e — replaces "Show-moves toggling ON/OFF")* | fill and ring cross-fade over `ClayAnim.PRESS_IN_MS`; glyph cross-fades to its variant (slashed / plain) over the same; the on mark scales in with `overshootSpring()` and out without overshoot |
| **Dock action Available ↔ Unavailable** *(D-04e)* | fill, shadow and glyph strength cross-fade over `ClayAnim.PRESS_IN_MS`; Available → Working is instant (it confirms the tap) |

**Reduced motion is a hard requirement.** Every decorative animation above is gated on
`rememberAnimationsEnabled()` (`Clay.kt:209-218`), which reads
`Settings.Global.ANIMATOR_DURATION_SCALE`. When it returns `false`:

- `claySquish` is already a no-op (`Clay.kt:152-153`) and `clayEntrance` already settles instantly
  (`Clay.kt:185`) — inherited, nothing to add.
- The tutorial coach pulse, the star pop, the caption cross-fade and the pill cross-fade **do not
  run**; the ring, the star, the caption and the fill appear settled.
- *(2026-09-28, D-04e / D-03l.)* The dock's look cross-fades and the on mark's scale-in **do not
  run**: every control simply shows its new look. The stage art has no motion to gate; it appears
  with the hero, settled.
- *(2026-10-21, D-03n.)* A Continue chip's room is open, settled, in the frame after the chip arrives;
  nothing glides. The tiles hold still with animations on or off, because the room always comes out of
  the stage.
- `TutorialAnim.STEP_ADVANCE_DELAY_MS` still applies. It is a **functional** beat — the player must
  see their jump resolve before the board is replaced — not decoration, and collapsing it would
  make the tutorial swap boards under the player's finger.
- The board's own jump/removal already resolve instantly in this mode
  (`BoardCanvas.kt:298-304`), so the tutorial inherits correct behaviour without a second gate.
- **Sound is not motion and is not gated on it.** The tutorial's jump pair (D-01i) still plays
  with animations off: `JumpSound` reads no animation flag (`GameScreen.kt:320-330`), and the
  project's precedent is that cues survive reduced motion (`ResultsCelebration.kt:104-106`
  consults `rememberAnimationsEnabled()` only to change the star stagger and still plays every
  cue). The one thing reading cannot prove is whether the keyed effect wins the race against the
  snap path at `BoardCanvas.kt:298-304`, which sets and clears `animating` in a very short
  window — so criterion 7c is exercised once with OS animations off, on a device.

---

## 11. Accessibility

Checklist for `android-compose-game` (rules/compose.md + the android-accessibility audit), stated
so QA can check each line on a device.

**Touch targets.** Every interactive element ≥ 48dp with ≥ 8dp spacing. Dock actions are 56dp
(`game_action_button_size`) with ≥ `spacing_sm` gaps — the largest targets in the app, deliberately,
because they sit under the thumb during play. The back chip is 48dp
(`game_status_back_size`). `Skip` is a `TextButton`, which applies
`minimumInteractiveComponentSize()`. Settings rows are full-width at ≥ 72dp. Mode tiles are ≥ 96dp
and tappable across their whole surface. The board's own hit-testing is unchanged
(`board_min_touch_target`).

**Labels.** Every interactive element carries a `contentDescription` or a text label; every
decorative element carries `contentDescription = null`. Decorative: the hero board art, the tile
art, the Continue thumbnail, the ribbon pill glyphs (the pill speaks as one node), the individual
step pips (the row speaks as one node), the completion star. Labelled: back, Settings gear, all
four dock actions, Skip, the completion CTA, the Settings row, both mode tiles, both Continue
chips, the timer.
*(2026-09-28, founder-feedback amendment.)* Decorative, added: the **Home stage art** (D-03l — and
the stage itself announces nothing and takes no focus) and Show-moves' **on mark** and **slash**,
which are drawn parts of one control whose state TalkBack reads from the switch role (D-04e). The
hero art is gone from the decorative list with the art itself.

**The unmerged tree is not the contract** *(ruled 2026-09-28, from the finding in
`.planning/reports/phase-15/android-game-dev-s5-s8.md:213-220`; no code change follows from it)*.
Every labelled icon control in this app is Material 3's own
`IconButton { Icon(contentDescription = …) }` — the click on the button node, the label on the glyph
inside it: the four dock actions (`UndoControl.kt:48-59`, `HintControl.kt:66-76`,
`HintToggleControl.kt:50-62`, `RestartControl.kt:39-47`), the status strip's back chip
(`GameStatusStrip.kt:105-125`), the Home hero's gear (`HomeHero.kt:78-83`) and four screens this
refresh does not touch (`ClassicCatalogScreen.kt:102-110`, `SettingsScreen.kt:500`,
`ResultsScreen.kt:173`). In a `uiautomator` projection those read as two nodes, the outer, clickable
one unlabelled. **That is not the tree this checklist is written against, and it is not a defect.**
`Modifier.clickable` merges its descendants' semantics, which is why every existing test finds and
clicks these controls by description through the **merged** tree (`ButtonSoundTest.kt:312-315`) and
why TalkBack announces label and role together, on the 56dp target. Three consequences, so this is
settled rather than re-litigated at every review:

1. **Phase 15 neither introduced nor worsened it.** The shape predates the refresh on screens it
   does not touch, and the one dimension the refresh did change moved the right way: a dock action's
   clickable target grew from the retired 48dp top-bar token to `game_action_button_size` 56dp
   (`dimens.xml:113-118`). A larger target is strictly better for touch and neutral for TalkBack.
2. **It is not to be "fixed" by collapsing the nodes.** `clearAndSetSemantics` or a merge on the
   dock chips would drop the `Role.Button` and the `enabled = false` "disabled" announcement that
   `IconButton` gives for free — which D-04d relies on for Undo and for Hint's thinking state — and
   would break the helper that reads the fill marker and the description off different nodes on
   purpose (`GameScreenChromeTest.kt:164-172`). The one sanctioned variant is `HintControl`'s
   thinking branch, which puts the description on the clickable node **because** its only child is a
   decorative spinner with no label of its own (`HintControl.kt:50-64`): a reason, not a style to
   copy across the dock.
3. **What is still owed is the device pass, and it was always owed.** The dump settled nothing in
   either direction; criterion 26 is a TalkBack check and criterion 26a now states its pass/fail
   line. If that pass finds an unlabelled focus stop, a label that cannot be activated, or two focus
   stops for one control, *that* is a defect and it is this refresh's to fix — on the control the
   pass names, not across the app.

**State, not just colour.** No control conveys state by hue alone.

| Control | Channels |
|---------|----------|
| Show-moves ON/OFF | **fill** (filled pill vs outlined) + tint + `show_moves_on`/`show_moves_off` |
| Undo enabled/disabled | `enabled` (TalkBack announces "disabled") + fill + tint |
| Hint idle/thinking | glyph→spinner swap + `enabled` + `hint_action`/`hint_thinking` |
| Step pips | **filled vs outlined** + `tutorial_step_progress` |
| Ribbon pill known/unknown | value text ("7" vs "—") + `stat_value_unknown_desc` |

This is a fix, not a restatement: `HintToggleControl.kt:45-51` carries its state in tint alone
today.

*(Amended 2026-09-28 — founder-feedback amendment, **D-04e**, §4.4.1. The table above was right in
intent and failed in fact: its "fill" channel for Show-moves and Undo measured ≈1.04:1 against the
dock, and the loud/quiet fills put two actions in the on/off vocabulary. It is superseded by these
rows.)*

| Control | Channels, since D-04e |
|---------|-----------------------|
| Show-moves On / Off | **strong fill vs none** + **ring** (Off) + **shadow** (On) + **glyph shape** (plain vs slashed) + **on mark** (On) + `Role.Switch` with announced state, one constant name |
| Undo Available / Unavailable | **fill** + **shadow** + glyph strength + `enabled` (TalkBack "disabled") |
| Hint Available / Working | **glyph shape** (glyph vs spinner) + **shadow** + `enabled` + `hint_action`/`hint_thinking` |
| Restart | always Available — it has one look, so there is no state to encode |
| Any action vs the toggle | an action is **never** drawn with the on mark, the slash, a strong fill or a ring |

Every pair of looks differs in at least two non-hue channels (§4.4.1's pair table, criterion 21b).

**Contrast.** ≥ 4.5:1 for all body and label text and ≥ 3:1 for glyphs and boundaries, in **both**
light and dark. The pairs in use are all from the design system's verified set. Two to check
explicitly on device because they are new pairings: `onPrimaryContainer` glyph on
`primaryContainer` fill (dock loud actions), and `onSurfaceVariant` glyph on `surfaceVariant` fill
(disabled Undo — perceivable even though WCAG exempts disabled controls). The gradient CTA label
sits on the gradient's dark half by construction (`Clay.kt:91-103`).
*(2026-09-28, D-04e.)* The dock's pairs to check on device are now §4.4.1's contrast table; the two
that carry a **state** — the On disc and the Off ring, each against the slab — must measure ≥ 3:1
in both themes (criterion 21f). The `onSurfaceVariant`-on-`surfaceVariant` pair above no longer
exists: an Unavailable action has no fill.
*(2026-10-21, D-05f.)* The system bars are checked too: the navigation bar's buttons and the gesture
handle ≥ 3:1 against what is behind them, and in dark mode a dark three-button scrim rather than a light
one (§6.2, criterion 29).

**Text scaling.** Every size is an M3 type role, so all text scales with the system font size.
Four layouts must be verified at 200%: the Home column (scrolls — §4.2), the tutorial column
(also scrolls, board unshrunk — §4.1), the three-pill ribbon
(each pill wraps its label under its value rather than truncating — *and on the narrow class,
where the two start out side by side, the label wrapping onto its own line is exactly the same
behaviour and the same requirement: it wraps, the band grows, the column scrolls, and neither the
label nor the value is ever truncated or scaled down; D-03k, §4.2.3, criteria 15i/15m*), and the
gameplay status strip
(the title is `maxLines = 1` with `weight(1f)` and truncates before it can push the timer pill off
screen — the timer must never be clipped, since it is the only place elapsed time appears).
*(2026-09-28, D-05d.)* The strip's roles are now `titleLarge` and `headlineSmall`, and the timer
pill's 40dp is a **minimum**, so at 200% the pill grows to hold 48sp digits rather than clipping
them; the rule above is unchanged and criterion 18e checks it.
*(2026-10-21, D-03o.)* "The label wrapping onto its own line" on the narrow class is made exact: a pill's
label and value never break inside a word or a number, and once any label no longer fits in half its
pill, all three pills stack — label line, value line, one height (§4.2.6, criterion 15w). Wrapping
*between* lines stays allowed; wrapping *inside* a word never was the intent.

**Screen-reader traversal.** Tutorial order: pips → Skip → board → caption → CTA. Gameplay order:
back → title → timer → board → dock (Undo → Hint → Show-moves → Restart) → any raised banner.
Home order: title → gear → ribbon pills → Continue chips → mode tiles. Titles carry
`semantics { heading() }` so a screen-reader user can jump bands.
*(2026-10-21 — D-05e, D-03n.)* The timer's stop speaks its value, "Elapsed time, 5 minutes, 12
seconds", and is never announced on its own (§4.3.2). A Continue chip arriving after first paint is not
announced either and moves no stop a player is on; it joins the order in its place. Criterion 26b lists
the stops, in order, with the string each speaks, for the device pass.
*(Recorded gap, 2026-10-21 — criterion 26b.)* **The two orders above list the board as a TalkBack
stop, and the build has never met that.** On this branch the board carries no label, text, role or
action (`BoardCanvas.kt:196-212`; `GameScreen.kt:193-207`, whose three custom keys are test-only;
`TutorialScreen.kt:245-263`), so TalkBack goes from Skip to the caption and from the timer to the dock,
and a TalkBack player can neither hear the board nor play it: no cell is reachable, and no peg can be
selected. The board is interactive on both screens, so this is also short of rules/compose.md's
"contentDescription or semantics role on every interactive element". The gap predates phase 15, which
neither made it nor closed it. The first-edition orders above stand as written; they are the design
statement the build falls short of, not a description of today's build. Criterion 26b's order, which
leaves the board out, describes today's build so that the device pass can be read; it does not relax
the orders above. **Release scope is routed by the producer to the game-designer (and the founder);
the ruling is pending at `.planning/reports/ux-spec-folds/game-designer-talkback-board.md`.** Until
it lands, this document rules neither the board's semantics nor its release.

**Sound is never the only feedback** (rules/audio.md). Every cue in this refresh is paired: the
jump has the slide and the removal; selection has the ring and the pulse; the invalid tap has the
dots staying put; Undo has the board stepping back; the completion cue has the star and the
caption. The dock's two silent controls (Show-moves, Restart) are silent by contract (D-09) and
both give immediate visual feedback. **The tutorial passes this rule twice over** (D-01h as
amended by D-01i): its one board cue — the jump pair on a completed jump — is paired with the
slide and the removal fade exactly as in the real game, and every event it deliberately leaves
silent (select, deselect, invalid tap, the completion star) carries visual feedback on its own,
so no information there is audio-only. Note the direction of the rule: it forbids sound as the
*sole* channel; it never requires silence. QA should read the absent select, deselect and
invalid-tap cues as the design and the present jump cue as required by SOUND-01 (§14, criteria
7c/7d/7e).

**One-handed use.** The four in-game actions and both Home mode tiles sit in the lower half of a
6.7-inch phone. This is the reach problem D-04 exists to fix.

---

## 12. Responsiveness

| Case | Behaviour |
|------|-----------|
| Narrow phone (≤ 360dp) | Home column scrolls; ribbon pills shrink to equal weights, never wrap to two rows; gameplay title truncates before the timer clips |
| Tall phone (6.7") | Home's weighted spacer pushes slack above the Continue chips so mode tiles stay low; the gameplay board centres between strip and dock |
| Short phone / 200% font | Home scrolls; the tutorial column scrolls with the board keeping `tutorial_board_size` and its aspect ratio (§4.1) — the caption grows and the empty footer leaves the fold, the board does not shrink; the dock stays pinned below the board |
| Tablet / large | Content capped at `content_max_width` 480dp and centred — unchanged; board capped at `board_max_size` 480dp — unchanged |
| Landscape | Not a supported orientation change in this refresh; existing behaviour preserved. Flagged to the tech-lead if the manifest does not lock orientation. |
| RTL | The back arrow and `UndoIcon` are already auto-mirrored (`UndoIcon.kt:29`); the status strip and dock are `Row`s and mirror by default; nothing uses absolute left/right padding |
| Dark theme | Every role above has a dark variant in the design system; both themes are in scope for every new composable's `@Preview` pair. *(2026-10-21, D-05f.)* The system bars follow the theme too: light icons and a dark three-button navigation scrim in dark mode (§6.2, criterion 29) |

*(Added 2026-09-28, D-03i.)* The rows above stand; §4.2.2 is where the first three are specified
mechanically for Home — the "Tall phone" row's slack, the "Short phone / 200% font" row's scroll,
and a **compact band set** the "Narrow phone" row did not anticipate for a *short* narrow phone.
§4.2.2 also corrects the **"Tablet / large"** row against the audited build rather than changing it:
`HomeScreen.kt:296-303` applies `fillMaxSize()` ahead of `verticalScroll`, which fixes the content's
width to the window's and makes the `widthIn(max = content_max_width)` after it a no-op, so Home's
column is today neither capped nor centred on a window wider than 480dp. The cap and the centring
are contract and have been since Phase 2; §4.2.2 specifies the modifier order that restores them and
criterion 15j checks it.

*(Added 2026-09-28, D-03k.)* Two rows above are sharpened by measurement, neither is replaced.
The **"Narrow phone (≤ 360dp)"** row's "ribbon pills … never wrap to two rows" is now specified
rather than hoped for: on the narrow class each pill drops its decorative glyph and puts its label
beside its value on **one** line (§4.2.3), which is what actually keeps the band off a second row —
the shipped stacked pill measured **116dp** at 360dp width, nearly double the 64dp the row assumed.
That row is also where the compact set is now *always* taken: a 360dp-wide window is compact at any
height that ships, because the wrap is a property of the width and not of the height. The
**"Short phone / 200% font"** row is unchanged and is still the honest description of what happens
above default scale — the slack goes, then the column scrolls, and no band is dropped (criterion
15i). What §4.2.3 adds to it is that on a 360×640dp phone the column scrolls at **default** scale
too, by ~72dp, and criterion 15k says so instead of asserting a fit.
*(Corrected 2026-09-29 — D-03m: ~48dp on the Robolectric arithmetic once the hero row's floor is
retired. The device QA measured, with shorter bands, already fitted; §4.2.4's last subsection.)*

*(Added 2026-09-28 — founder-feedback amendment.)* **Tall phone**: Home's slack is now the **stage**
(D-03l, §4.2.4) — it sits between the hero row and the ribbon and holds a board as large as the
slack allows (≈150–280dp on a 6.7-inch phone), so the tiles stay low without a blank band above
them; the largest blank any window can show is < 80dp. **Narrow and short**: unchanged — the stage
is 0 and 15k's numbers stand. **Gameplay, every size**: each system bar is applied once (D-05d,
§4.3.1), which is what lets the larger title and timer cost the board nothing on a 360×640dp phone
at both 1.0 and 2.0 font scale.

*(Pointer 2026-10-21 — D-03n, §4.2.5.)* The ≈150–280dp range above is the 3 : 1 split's. Under D-03n
the stage on QA's 6.7-inch window is 326.3, 230.3 and 134.3dp with no, one and both chips (the art
capped at 320dp), and the tiles hold the same place in all three states.

---

## 13. Test impact (read this before writing code)

Existing tests this refresh **will** break, with the fix:

| Test | Why it breaks | Fix |
|------|---------------|-----|
| `ButtonSoundTest.kt:105-123` `theHomeTopBarHowToPlayAndSettingsIconsMakeNoSound` | Clicks `R.string.how_to_play` by content description; that button and that string are deleted (D-02) | Retarget **narrowed, not moved**: keep the gear half (still silent, still on Home) and **drop** the how-to-play half rather than pointing it at the Settings row — that row is a navigational CTA and deliberately **plays** `SoundCue.ButtonForward` (§4.5, D-09), so re-aiming a *makes-no-sound* assertion at it would assert the opposite of the contract. The row's cue is covered positively by criterion 9 and by SOUND-04 (§16). |
| `HomeScreenTest.kt:148-159` `theProfileSwapDialogIsNeverStackedOnTheOnboardingOverlay` | Locates the overlay by the hardcoded literal `"Got it"` (`:367`) and the overlay stops being hosted by Home (D-01f/D-01g) | **Retarget, never delete:** `theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending`, finding the dialog by `PROFILE_SWAP_DIALOG_TEST_TAG` as today and the pending state by the state field — never by button copy |
| `ButtonSoundTest.kt:226-244` | Clicks `navigate_back` and `show_moves_on` on the game screen | **Passes unchanged** — both content descriptions are deliberately preserved (D-05b, §4.4) |
| `GameFlowTest.kt:129` | Finds the timer by `elapsed_time` | **Passes unchanged** — deliberately preserved (§4.3). *(Superseded 2026-10-21 — D-05e, §4.3.2: the description now starts with `elapsed_time` and goes on to the spoken duration, so this test is retargeted; see the 2026-10-21 table below.)* |

**Device checklist that must keep working.** `11-HUMAN-UAT.md` tests 10, 11 and 13 read their
pass/fail off the three Home readouts in §4.2.1. This refresh is designed so those tests run
**unchanged** — the Challenge tile chip, the Continue chips and the Classic catalogue's star pips
are all untouched in behaviour. If a plan proposes changing any of the three, it is changing a
test oracle and the qa-engineer and release-manager need to know before, not after. The other way
those three tests can break is not a layout change at all: a tutorial routed through
`GameViewModel` would rewrite two of them from underneath. §5.1 is why that is a prohibition, and
criterion 7a is the device check for it.

Two gaps QA should close rather than inherit:

- **The tutorial has no test of its own.** There is no `OnboardingOverlayTest.kt`; the only
  assertion that touches it is the incidental one above, and the captions, the skip action and the
  illustrations are asserted nowhere. An interactive tutorial needs a real Compose UI test per
  rules/testing.md ("one UI test per screen covering its main flow").
- **The overlay carries no `testTag`**, unlike `PROFILE_SWAP_DIALOG_TEST_TAG` and
  `PLAY_GAMES_PROMPT_DIALOG_TEST_TAG`. The replacement should carry one so no future test has to
  find a screen by its button copy.

**What this refresh owes in tests, beyond the two retargets** *(2026-09-21 — tech-lead and
game-designer rulings, folded in; the list is theirs, it is repeated here because it is the answer
to the two gaps above)*:

- Every new or replaced surface carries a `testTag` constant — `TUTORIAL_SCREEN_TEST_TAG`,
  `HOME_PROGRESS_RIBBON_TEST_TAG`, `GAME_STATUS_STRIP_TEST_TAG`, `GAME_ACTION_DOCK_TEST_TAG` — in
  the same idiom as the two existing dialog tags. This is what stops the next test finding a
  screen by its copy.
- The two breaking tests are **retargeted, never deleted and never `@Ignore`d**.
- One Compose UI test per changed screen's main flow, the tutorial included.
- A `:domain` test that every legal line of every tutorial board ends on exactly one peg, over
  `TutorialBoards.ALL`, plus the two axis guards (T1 horizontal-only, T2 vertical-only) — these
  are the unit-level guard for §14 criterion 2, which otherwise has only a device check.
- The ordering test for the exit path (the seen flag is persisted *before* the exit signal) and
  the "every exit route persists exactly once" test — criterion 5's real teeth.
- A denominator guard that the Boards pill's denominator is the Classic catalogue size (5), so a
  tutorial board joining `CLASSIC_SPECS` cannot change a shipped number silently.
- A placeholder test that a ribbon pill with no value yet shows "—", and a repository test that a
  failed read degrades to unknown rather than to `0` (D-03h).

**Two test idioms this refresh now forbids** *(added 2026-09-28, D-03k, from `code-auditor-s9.md`
MB-1; the layout properties D-03k owes are listed in §4.2.3)*. They are recorded here rather than
only in §4.2.3 because they are a **testing** contract and the next Home or tutorial layout
assertion will reach for the first of them again:

- **Never assert containment as `bottomOf(node) ≤ contentBottom()`.** `getBoundsInRoot()` returns
  bounds **clipped to the root**, so that comparison holds by construction and a node pushed
  entirely off screen reports `0.0.dp..0.0.dp` — the assertion is arithmetically incapable of
  failing. Assert instead that **a scroll does not move the node**
  (`before.top == onNode(…).performScrollTo().getBoundsInRoot().top`). `assertIsDisplayed()` does
  not close the gap either: Compose satisfies it on **partial** visibility, so it passes with a
  band clipped.
- **Never measure a short-window layout property on a zero-inset window.** The design baseline is
  **568dp** of usable height at 360dp width, not 640dp (§4.2.2), and at the corrected numbers the
  compact column lands within a few dp of 640 — so a zero-inset reading says "fits" on exactly the
  window where the device does not. Supply the insets the way `TutorialScreenTest`'s
  `applySystemBarInsets()` already does; the harness exists in this repo and was written for F-1 in
  the same slice. A reading taken without them is **neither a pass nor a fail** (criterion 15k).

**Founder-feedback amendment — what it breaks, what it retargets, what it owes** *(2026-09-28,
D-03l / D-04e / D-05d)*:

| Test | Effect | Fix |
|------|--------|-----|
| `ButtonSoundTest.kt:226-244` | Finds the back chip by `navigate_back` and the toggle by `show_moves_on` | **Passes unedited** — both descriptions survive, and `show_moves_on` is now the toggle's name in both states. If it needs an edit, a description drifted; fix the production code. |
| `GameFlowTest.kt:129` | Finds the timer by `elapsed_time` | **Passes unedited.** *(Superseded 2026-10-21 — D-05e, §4.3.2: retargeted, never deleted; see the 2026-10-21 table below.)* |
| `GameScreenChromeTest.showMovesCarriesItsStateByFillAndNotByTintAlone` (a phase-15 test) | Reads `DockActionFilledKey` and the `show_moves_on`/`show_moves_off` pair; the key is replaced and the off string deleted | **Retarget, never delete or `@Ignore`:** assert the On/Off look property, the `ToggleableState`, `Role.Switch` and the constant name. A name that says what it now proves, e.g. `showMovesCarriesItsStateByFillRingMarkAndSwitchStateNeverByTint`. |
| Any other test reading `DockActionFilledKey` | The key is replaced by the look property | Retarget to the look property. |
| `HomeLayoutTest` / `HomeScreenTest` assertions (Home's geometry moved to `HomeLayoutTest` in the S10 slice) that the hero art is present on a tall window, absent on the narrow baseline, or that the compact hero is a separate shape (criteria 15k part 4, 15l's third window) | There is no hero art on any window and one hero shape | Retarget "hero art present" to "stage art present" and "hero art absent" to "no stage art on an overflowing column"; the full/compact distinction is now spacing and pill shape only. Retarget, never delete or `@Ignore`. |

**D-03m — what it retargets** *(2026-09-29 amendment, §4.2.4 last subsection)*:

| Test | Effect | Fix |
|------|--------|-----|
| `HomeStageTest` (15p): measures the hero→art gap from the hero row's box | Reads 16dp where the device shows 27.4dp. The row's box is not a visible element (D-03m). | **Retarget**: measure from the lower of the title's and the gear's bottom edges. Watch it fail against the old 72dp floor (28dp on `w412dp-h915dp` with insets) before trusting it. |
| Any assertion that the hero row is ≥ 72dp or equals `home_hero_row_min_height` (for example in `HomeHeroRowTest`) | The token is retired. | **Retarget** to "the row's height equals its tallest child's (±0.5dp) at font scale 1.0 and 2.0". Never delete or `@Ignore`. |
| Any 15k test that pins the Robolectric overflow at ≈72dp | The overflow falls to ≈48dp. | Keep the ≤ 120dp bound and the fold-through-the-second-tile assertion. A test that pinned exactly 72dp was pinning the defect. |

**The 2026-10-21 amendment — what it retargets and what it owes** *(D-03n, D-03o, D-05e, D-05f; all
built on `bugfix/home-chip-arrival`)*:

| Test | Effect | Fix |
|------|--------|-----|
| `GameFlowTest.kt:129`, `GameScreenChromeTest.kt:62` and `:75`, the helper at `GameStatusStripTest.kt:312-314` | Each finds the timer by the exact `elapsed_time` description, which is now only the start of it (D-05e). This supersedes the "passes unchanged" and "passes unedited" rows for `GameFlowTest.kt:129` above. | **Retarget, never delete or `@Ignore`:** find the clock through `GAME_TIMER_PILL_TEST_TAG` and the node inside it, or by the label as a substring, and assert the whole sentence where the test is about the timer. |
| `HomeStageTest`, and `HomeLayoutTest`'s 15g and 15p cases, wherever they read a footer of a quarter of the current slack or a stage of three quarters of it | The split is a footer of one fifth of the no-chip slack, fixed for the window, and a stage of the rest less the chips' room (D-03n). | **Retarget** to the D-03n quantities. 15p's bounds (24, 80 and 64dp, and the ≥ 120dp art) and 15g's midpoint stay exactly as they are. 15g's third clause is asserted as ruled in §4.2.5: the footer is at most a third of **S₀**, the no-chip slack (a fifth under D-03n), in both the no-chip and the both-chips state — never against the slack left once the chips are in. |
| Any test that asserts the compact pill's label wraps beside its value at a large font scale | At a large font scale the ribbon stacks, and no label breaks inside a word (D-03o). | **Retarget** to criterion 15w. 15m's default-scale assertions stand unchanged. |
| `WindowThemeTest` | Gains the navigation-bar half (D-05f). | **Extend**: `android:windowLightNavigationBar` resolves true under `notnight` and false under `night` at API ≥ 27; the status-bar cases stand unchanged. |

**Owed, new, from this amendment** — every one watched failing once on today's code before it is
trusted: **15s, 15t, 15u** (Home's bounds across the chip emissions, at 1.0 and 2.0, and frame by frame),
**15w** (no word breaks; one shape per ribbon), **18f** (the timer's description table) and the `WindowThemeTest`
case of **29**. **15v**, **18g**, **26b** and the device half of **29** are the qa-engineer's, on a device.
The two §13 idioms still bind every new layout assertion: no `bottom ≤ contentBottom()` containment and
no zero-inset window.

**Owed, new** — every one watched fail once before it is trusted: criteria **15n–15r** (Home, §4.2.4,
including the unit test that replays the stage's recorded move list from the English start),
**18a–18e** (the strip, §4.3.1 — 18b and 18c with insets supplied, 18d on a device), and
**21a–21g** (the dock, §4.4.1 — 21b is a pure unit test over the recipe data, 21f and 21g device
checks). The two §13 idioms above still bind every new layout assertion: no
`bottom ≤ contentBottom()` containment and no zero-inset window.

**Four audio tests** *(2026-09-21 — added with D-01i; the list above carried none, and criterion
7c had a device check and nothing else)*. The project's own rule is that a named silence is only
a real property when both halves are asserted — `ButtonSoundTest.kt:41-46`: "Selectivity is only
a real property if both halves are asserted, so the negative cases here are first-class tests
rather than afterthoughts … without them a future 'simplification' that folds the cue into the
shared `Modifier.claySquish()` would pass every positive test while making exactly the excluded
controls noisy." The infrastructure exists and the tests are cheap: `FakeSoundPlayer`
(`FakeSoundPlayer.kt:25-48`) records every cue in order and at pitch, installed through
`CompositionLocalProvider(LocalSoundPlayer provides …)`, and `GameScreenSoundTest` is the
template — `aJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt` (`:76-87`) and
`aFreshlyComposedScreenMakesNoSoundAtAll` (`:186-194`) are the two shapes needed.

- `TutorialSoundTest.aCompletedTutorialJumpPlaysTheJumpCueThenLayersTheRemovalCueUnderIt`
- `TutorialSoundTest.theTutorialPlaysNoSelectDeselectInvalidTapOrUndoCueOnAnyStep`
- `TutorialSoundTest.theCompletionStarPopsWithoutTheWinOrStarPopCue`
- a grep-shaped guard that `app/src/main/java/com/app/pegsolitaire/ui/onboarding/` references no
  `SoundCue` the spec does not name — the shape the tech-lead already proposes, now with a
  non-empty allow-list (`Jump`, `PegRemove`, `ButtonForward`, `ButtonDismiss`).

Without these, the tutorial's audio contract is an assertion in a document rather than a property
of the build, and the first refactor that folds a cue into a shared modifier breaks it silently.

**Closed — the screenshot-suite question.** The first edition raised the absence of any
screenshot infrastructure (no Roborazzi, no Paparazzi, no `screenshotTest` source set, no
`androidTest` directory) as a gap against the stack CLAUDE.md names. **Ruled 2026-09-21
(tech-lead): no golden-image suite in Phase 15; filed as a post-v1.1 follow-up.** The reasoning I
accept and will not reopen: every pixel on all five screens is intentionally changing, so goldens
recorded during this phase would be images of the implementation under review rather than of the
contract — and recording them *after* the refresh captures the design the studio intends to keep.
What Phase 15 owes instead is the list above, which is binding rather than advisory.

---

## 14. Candidate acceptance criteria

Observable truths a QA engineer can check on a device, in the roadmap's house phrasing.

**Tutorial (D-01)**

1. On a first launch, the player reaches a tutorial that requires them to **complete a real jump**
   before it advances; no step can be advanced by a Next or Continue button. (ONB-01, D-01)
2. The player performs at least one horizontal jump and at least one jump on the other axis, on a
   live board, before the tutorial ends — **structurally**: T1 offers exactly one legal move and
   it is horizontal, T2 offers exactly one and it is vertical, so no sequence of taps can finish
   the tutorial without both. (ONB-01, D-01b)
3. On **T2**, tapping the middle peg — which has no legal jump — selects it, draws no destination
   dots, and produces no error, no red, no dialog and no lost progress; after a pause the tutorial
   re-pulses a peg that *can* move **and puts the tapped peg back down**, so the pulsing peg can be
   tapped straight away rather than through a peg the player is still holding. (D-01c, §4.4 release
   clause) *(The same holds for three of T3's five pegs — the whole middle row; this is normal peg
   solitaire, not a defect.)*

3a. With a peg already selected on **T2** or **T3**, tapping a **different** peg changes nothing:
    the first peg stays selected, its dots (if any) stay as they were, and no error, no red and no
    sound appear. Re-tapping the held peg puts it down, and the other peg then selects normally.
    **This is the gameplay board's behaviour, checked the same way on both screens** — on a real
    game, select a peg and tap another one: the selection does not move there either. (§4.1 "Tap
    semantics", §4.4 class 2, D-01a)
4. Three consecutive taps that make no progress cause the tutorial to show more help, not less;
   the player is never blocked from continuing; and the help **never mentions Undo** or tells the
   player they are stuck. (D-01c, §4.4)
5. `Skip` is reachable on every step and exits immediately; pressing system back does the same
   thing; after either, the player lands where they came from, Home does **not** immediately
   re-open the tutorial, and the tutorial does not appear again on the next launch. (D-01d,
   D-01g)
6. Rotating the device mid-tutorial keeps the player on the same step with the same board.
   (D-01e)
7. Completing the final step shows a single primary button that returns the player to where they
   started, and the tutorial does not appear again on the next launch. (D-01)
7a. **Nothing behind the tutorial moves.** On a player who has a saved Challenge game and existing
    stars, running the tutorial end to end — or skipping it — leaves the Home Stars pill, both
    Continue chips and the Challenge tile's level **exactly as they were**: no star banked, no
    resume snapshot cleared, no ladder advance, and the Results screen is never reached.
    (D-01g, §5.1, D-03f)
7b. Opening Settings › How to play after skipping mid-tutorial starts again at **step 1**, on the
    first board, every time. (D-01g)
7c. Each completed jump in the tutorial plays the **same jump cue as the real game** — the pitched
    jump with the peg-removal layer under it — on every step, including each of T3's four jumps,
    and with the game's own pitch ladder when the same peg jumps twice in a row. **Check this
    once more with OS animations off** (Developer options → Animator duration scale: off): with
    animations disabled the jump takes the snap path (`BoardCanvas.kt:298-304`), so `animating`
    is set and cleared inside a very short window, and whether the cue still fires in that window
    can only be settled on a device — it is not provable by reading the code.
    (SOUND-01, D-01h as amended by D-01i)

7d. **Nothing else on the tutorial screen makes a sound.** Tapping a peg, re-tapping to put it
    back, tapping a peg with no legal jump, tapping an illegal destination, and the completion
    star are all **silent** — no select, deselect, invalid-tap, undo, hint, star-pop or win cue
    at any point, on any step, on either entry path. (D-01c, D-01h as amended by D-01i,
    rules/audio.md)

7e. The tutorial's completion button plays the same press cue as every other primary button in
    the app, and `Skip` plays the app's dismiss press cue; neither adds any other sound, and both
    behave the same on the first-run path and on the Settings › How to play replay.
    (SOUND-04, D-11, D-01i)

**Info button (D-02)**

8. There is **no** information or help icon anywhere on the Home screen. (D-02)
9. Settings › General has a "How to play" row that opens the tutorial, and it works for a player
   who has already finished it. (D-02a)
10. A returning player who has seen the tutorial launches straight into Home and is never shown
    it again unless they ask from Settings. (D-02a)

**Home (D-03)**

11. Home shows more than the two mode tiles: a titled hero band, three progress pills
    (Stars · Boards · Level) and board art on the mode tiles. (D-03)
12. The Level pill and the Challenge tile show the **same** level number, and tapping the tile
    enters that level. (CHAL-05, D-03c)
13. A pill whose value is not yet known shows "—" rather than 0 or a blank, and a screen reader
    announces it as not yet available. (D-03c)
13a. A pill whose underlying read **fails** shows "—" as well — never `0`. A player with stars
     already earned never sees their total reported as zero. (D-03h)
13b. The Boards pill's denominator is the Classic catalogue size (5), and the Classic catalogue
     screen still lists exactly those five boards — no tutorial board appears on it. (D-03c, §5)
14. With the system font size at maximum and both Continue chips present, the whole Home screen is
    reachable by scrolling and nothing is clipped. (D-03)
15. On a 6.7-inch phone the two mode tiles sit in the lower half of the screen. (D-03)

**Home load-bearing readouts (D-03f)** — these are the cloud-save device oracles; a failure here
blocks tests 10/11/13 in `11-HUMAN-UAT.md`, not just the look of the screen.

15a. On a 360×640dp phone at default font scale, with **both** a Classic and a Challenge game
     saved, the Continue chips **and** both mode tiles are fully visible on Home without
     scrolling. (D-03f, PERS-03) *(Pointer 2026-09-28 — D-03k, §4.2.3: **left as it reads, and its
     two halves now part company.** The **Continue chips** half is absolute, holds on every
     supported window and is restated in 15k part 1. The **both mode tiles** half is **not
     achievable** — §4.2.3 measures the band set at ≈640dp after every yield that costs no readout,
     against 568dp of usable height — and it is superseded by **15k**, which puts the first tile
     inside the first viewport and the second across the fold. 15a is kept verbatim because it is
     what the S5–S8 and S9 builds were judged against; it is **not** the criterion the D-03k slice
     is judged against.)*
15b. The Challenge tile still shows the ladder position on its own face ("Continue — Level N"),
     and it shows the same number as the ribbon's Level pill. (D-03f, CHAL-05)
15c. Tapping the Classic tile reaches the catalogue in one tap, and each board card there still
     shows its earned-star pips. The Home Stars pill is a total and has not replaced them.
     (D-03f, CLAS-01)
15d. No readout on Home is behind a tap, an expander, a "show more", or a scroll position.
     (D-03f) *(Pointer 2026-09-28 — D-03k: **left as it reads**, and §4.2.1's dated note draws the
     one bright line it needs. The tap, the expander, the "show more", any collapsed or truncated
     region, any animation that must finish and any state change stay forbidden on **every**
     window. "A scroll position" is read, on the **narrow-and-short class only**, as forbidding a
     readout that a scroll *reveals* — not one that is fully composed at full size with full
     semantics inside a column the player scrolls, with the overflow bounded at ≤ 120dp and the
     fold cutting through a band (15k). Both Continue chips stay above the fold everywhere; that
     part is not relaxed anywhere by anything.)*

**Home cold start (D-03g, D-03j, PLAT-08 C1)**

15e. *(Replaced 2026-09-28 — D-03j and §6.5 "second edition"; the retired first edition is quoted
     below it.)* **Cold start is checked as an interleaved A/B delta against a null control
     measured in the same sitting — and it is checked on the follow-up slice that lands §4.2.2, not
     on the build audited on 2026-09-28.** Arm A = the merge-base of `develop` and the branch, arm B
     = that slice's branch; one AVD (`Pixel_9a_bench`, §6.5 precondition 1), `:app:installDebug`,
     each arm swapped in only at a block boundary, the
     null control swapping its one build the same way (§6.5 precondition 7), the
     seeded ~200-level Challenge ladder on both, the launch component resolved from the device, and
     **exactly one emulator with no concurrent Gradle build anywhere on the host** and a 1-minute
     load ≤ half the host's logical cores at both ends. The sitting runs
     the **null control first** — the *same* build in both positions, identical protocol — and
     records `floor = |median difference|`; then the two real arms **interleaved** A/B/A/B in blocks
     of five, **≥ 20 recorded samples per arm**, the first of each block discarded, `am force-stop`
     and 2 s between samples. **PASS:** `median_B − median_A ≤ max(2 × floor, 100 ms)`. **FAIL:**
     greater than that — a real regression; §6.5's degradation path is walked and the whole sitting,
     null control included, is re-measured after each step. **VOID:** `floor > 150 ms` — the host
     was too noisy to decide, the sitting is re-run later, and it is reported as **neither a pass
     nor a fail**; the arm numbers from a void sitting are not evidence. The null table, both arm
     tables, the eight §6.5 preconditions with their recorded values, both `uptime` readings and the
     computed floor and threshold all go in that slice's build evidence, and **that slice does not
     close without them**. **This criterion contains no absolute number**: the ≤ 950 ms figure is a
     tracked target owned by PLAT-08 / phase 14, and the bar of record is rules/studio.md's
     under-2-seconds on a named mid-range physical phone — which §6.5 records as never yet measured,
     with its owner and its decided default. **Phase 15 closes with 15e recorded as `:NOT RUN`**,
     which is the correct record and not an omission. (PLAT-08 C1, D-03j, §6.5 second edition)
     *(Amended 2026-09-29 — tech-lead font ruling, Ruling 2, on the qa-engineer's E-1. Before this
     date the paragraph read "both arms installed for the whole sitting", which no sitting could
     meet because both arms share one `applicationId`; the host-load clause is new. §6.5
     preconditions 1, 4 and 7 carry the full text, and a sitting on any AVD other than the
     `Pixel_9a` is VOID. The follow-up slices have since landed inside phase 15, on this branch, so
     "Phase 15 closes with 15e recorded as `:NOT RUN`" no longer describes the close: per Ruling 2,
     15e runs **once**, with arm B = the branch head that carries both D-03m and the bundled fonts,
     because both change Home's first frame (§6.5's "Which builds" table, pointer below it).)*
     *(Amended 2026-10-18 — tech-lead phase-15 sign-off, Decision 1, on the qa-engineer's loop-3
     E-1; folded 2026-10-21 by the ux-designer, from
     `.planning/reports/15-founder-feedback/tech-lead-signoff.md` "Text for other roles' files".
     Before this date the paragraph read "Arm A = `develop`" and "one AVD (`Pixel_9a`, API 36)". The
     `Pixel_9a`'s `/data` was 95 % full and refused both arms, so the instrument is the bench §6.5
     precondition 1 describes, and "a sitting on any AVD other than the `Pixel_9a` is VOID" in the
     note above now reads "other than `Pixel_9a_bench`". Arm A is the merge-base so that A..B is
     exactly the branch's diff whatever `develop` gains meanwhile. "The eight §6.5 preconditions"
     reads **nine** since precondition 9, `/data` free ≥ 4 GB at both ends, was added the same day.
     Phase 15's own sitting was sat on 2026-10-18 and passed (§6.5, the 2026-10-18 pointer under
     "Which builds").)*

> **15e — first edition, retired 2026-09-28** (`tech-lead-coldstart-gate.md` §1/§3/§4). Kept legible
> because two reviewers returned verdicts against *this* wording, and a reader who meets those
> reviews has to be able to find what they were refusing. It read: a two-arm delta, arms run one
> after the other, 10 `am start -W` samples each with the first discarded, re-installed between
> arms, and **both** of (1) after-median **≤ 950 ms** and (2) delta **≤ 50 ms** had to hold, with
> "the phase does not close without them". It is retired for two independent reasons, **neither of
> them a lowered bar**. **(2) was ill-posed:** 50 ms came from Phase 6's 46 ms *within-arm range*
> and was applied to a *between-arm median difference*, and two null experiments put that
> instrument's floor at **57 ms** and **59 ms** — the question could not be answered by the
> instrument being asked it, so the only available outcomes were a fabricated pass and a permanent
> block. **(1) was never this phase's:** Phase 6 wrote its 633 ms as the before-picture "PLAT-08
> measures against in Phase 14" (`06-BUILD-EVIDENCE.md:308-309`), and this criterion's own last
> clause already escalated a condition-(1)-only failure to PLAT-08 / phase 14 "rather than being
> paid for out of this refresh" — D-03j stops that from being a special case and makes it the rule.
> The replacement is **stricter where it matters**: a noisy host now buys a **void** and a re-run
> instead of a number whose sign is luck.

15f. On first paint, before any progress data has loaded, Home shows the ribbon with "—"
     placeholders — never a spinner, never a blank band, never a blocked screen — and the values
     fill in without the layout shifting. (D-03c, D-03g) *(Pointer 2026-09-28: **unchanged, and it
     does not travel with 15e.** 15f is a visual check, not a timing measurement; it needs none of
     §6.5's preconditions and stays a **phase-15 sign-off item on this build**. §6.5's second
     edition records how it is verified today and what instrument would make it decidable.)*

**Home vertical slack (D-03i, §4.2.2 — added 2026-09-28)** — these four check the change D-03i
requires and **not** the build measured before it. Criteria 14, 15, 15a and 15d above are unchanged
and stay the record of what that build was judged against; 15g and 15h are the same two intents
stated so that a tall phone and a short one can both pass, which one fixed spacer value cannot
deliver.

*(Pointer 2026-09-28 — D-03k.)* Three of these four stand exactly as written and are the criteria
the D-03k slice is judged against too: **15g** (the tall phone), **15i** (200% font scale) and
**15j** (the slack is empty space, and the column is capped and centred). The fourth, **15h**, is
**retired below as false** and replaced by **15k**; the block that follows 15j — "Home on the
narrow class" — carries 15k together with the width-term and ribbon-yield criteria, and states
which build all three apply to.

15g. On a 6.7-inch phone, in **both** states — a fresh install with no Continue chip, and a player
     with both — the **top** edge of the first mode tile is at or below the vertical midpoint of
     Home's content area, and the empty space left below the second mode tile is no more than a
     third of the screen's unused space. No band is pinned to the bottom edge and no single gap on
     the screen is larger than the space the tiles occupy. (D-03i, D-03, criterion 15)
     *(Ruled 2026-10-21 — **D-03n**, §4.2.5, "How 15g's 'unused space' is read". The words stand.
     "The screen's unused space" is **S₀**, the slack the window leaves with no Continue chip, fixed
     for the window, and "the empty space left below the second mode tile" is the footer, without the
     band gap after the tile or the column's bottom padding. Under D-03n the footer is a fifth of S₀ in
     every state, so the clause holds in both. Read against the slack left once the chips are in, the
     same footer would be 38 % on QA's native window with both chips. No footer ratio meets that
     reading and 15p part 1 together, except on a knife edge under 0.1dp wide on one window (§4.2.5);
     that reading worked only while the footer moved with the chips. The midpoint, the bottom edge and the largest-gap clauses are unchanged, and the loop-3
     PASS stands on both readings for the build it measured.)*

15h. *(**RETIRED 2026-09-28 — D-03k, §4.2.3. It is not achievable, and it was certified by an
     assertion that could not fail.** Replaced by **15k**. Read the retirement note below it before
     citing either.)* On a 360×640dp phone at default font scale with **both** a Classic and a
     Challenge game saved, Home shows — **without scrolling** — both Continue chips and both mode
     tiles, and the hero shows its gradient title and its Settings gear with no decorative art.
     Scrolling reveals nothing that was hidden, and the Challenge tile's level chip is on screen
     with the rest. (D-03i, criterion 15a, D-03f)

> **Why 15h is retired, and what it still does.** Kept verbatim above, not deleted, for two
> reasons: it is **the criterion the 2026-09-28 build was measured against**, and that build's
> verdict against it is **FAIL** (`code-auditor-s9.md` MB-1) — retiring it must not launder that
> verdict — and a reader who meets MB-1 has to be able to find the wording it was refusing.
>
> It is retired because **it is false, and §4.2.3 proves it false by arithmetic rather than
> asserting it**. Measured at 360dp width the compact band set is ≈700dp against 568dp of usable
> height; after the last yield that costs no readout (the ribbon's decorative glyph and its third
> line, D-03k part 2) it is ≈640dp — still **72dp over**, and every one of those 72dp is the hero's
> 48dp gear and gradient title, oracle 3's two Continue chips, or §11's two 96dp tile floors
> carrying oracle 1's level chip. No yield remains that is not a readout, a target or a route, and
> §4.2.1's four rejected simplifications are not available.
>
> *(Corrected 2026-09-29 — **D-03m**.)* Of those 72dp, 24dp was the hero row's floor above its 48dp
> gear, which is none of the three. With the floor retired the set is ≈48dp over, so 15h is still
> false and stays retired.
>
> **Retiring it does not clear MB-1.** The defect MB-1 names is an assertion that is
> *arithmetically incapable of failing* (`HomeScreenTest.kt:300`; `getBoundsInRoot()` clips to the
> root, so `bottom ≤ contentBottom()` holds by construction and a node pushed off-screen reports
> `0.0.dp..0.0.dp`). A criterion changing underneath such an assertion does not make it sound. The
> assertion is still the developer's to fix, and 15k specifies the two idioms that make it decisive
> — **assert a scroll does not move the node**, and **supply the insets** — because on the
> corrected numbers the compact column lands within a few dp of the 640dp zero-inset window, so a
> zero-inset test would read "fits" on exactly the window where the device does not.
>
> **What this is not.** It is not a lowered bar and not a regression. Before this refresh the same
> window was ~304dp short with the full band set; the compact set closes that to 72dp. The
> replacement is *stricter* in three places 15h never covered: it requires the insets, it bounds
> the overflow at ≤ 120dp, and it requires the fold to cut **through** the second tile so the
> screen cannot present a false bottom.

15i. At 200% font scale on **both** baselines the slack is what disappears, not the content: Home
     scrolls, every band is reachable, nothing is clipped, no band has been dropped or collapsed,
     and the hero's title is neither truncated nor shrunk. (D-03i, criteria 14, 26)

15j. The slack is empty space and not a control: nothing between the ribbon and the Continue chips,
     and nothing below the mode tiles, is tappable, focusable by TalkBack, or announced at all. On a
     window wider than 480dp the content column is still capped at `content_max_width` and
     **centred**, not pushed against one edge. (D-03i, §12, §4.2)

**Home on the narrow class (D-03k, §4.2.3 — added 2026-09-28)** — **these three apply only to the
slice that lands D-03k**, the slice *after* the build audited on 2026-09-28. They do **not**
retro-certify that build, and no earlier criterion is restated by them except where a pointer above
says so. Criteria 14, 15, 15g, 15i and 15j are unchanged and still bind; 15a's chips half and 15d's
prohibitions are unchanged and still bind; **15h is retired and 15k replaces it**.

15k. On a **360×640dp phone at default font scale with both a Classic and a Challenge game saved**,
     measured on a window with the device's **real system-bar insets applied** — a zero-inset
     window is a measurement artefact and a reading taken on one is **neither a pass nor a fail**:
     1. **Complete inside the first viewport, with no scrolling:** the hero row (gradient title and
        the 48dp Settings gear), the **whole** progress ribbon with all three values, **both**
        Continue chips, and the **first** mode tile.
     2. **If the column overflows at all, the fold cuts through a band and not through a gap** —
        on this window that band is the second mode tile, with its top edge above the fold and its
        bottom edge below it, so Home presents no false bottom. A fold landing in the gap between
        two bands fails this part as surely as a tile entirely off screen does. (Stated
        conditionally on purpose: a window where nothing overflows passes this part trivially, and
        §4.2.3's arithmetic says 360×640dp is not such a window — it is ≈72dp over.) *(≈48dp over
        since D-03m, 2026-09-29, which retires the hero row's floor. The conditional and every bound
        in 15k are unchanged.)*
        *(2026-10-21.)* Those figures were Robolectric LEGACY readings. On real metrics (Robolectric
        NATIVE in the app theme, and the device), 360×640dp with both chips overflows 0dp. So part 2
        holds there in its no-overflow branch. The overflow branch is read on `w360dp-h560dp`
        (`HomeLayoutTest`). "Overflows" means overflows into a band: the column's trailing space,
        the gap after the last band plus the bottom padding, is not a band (§4.2.2 clause 5), so a
        fold inside it hides nothing and passes.
        *(Recorded 2026-10-21 by the ux-designer from
        `.planning/reports/15-founder-feedback/tech-lead-signoff-close.md` fold c, the tech-lead's
        wording applied as given. The trailing-gap reading is the code-auditor's ruling in
        `code-auditor-loop5.md` § Item 5, adopted by the tech-lead in that report's INFO row 7; it
        is the reading this role would have given, so no further UX ruling is owed. In the
        tech-lead's words: "The 16 dp tolerance is exact: M-edge120 passes and M-edge121 is cut"
        (the auditor's mutation probes). The "≈72dp over" and "≈48dp over" parentheticals above
        stay as the record of what was believed when they were written.)*
     3. **One plain scroll completes the screen.** The second mode tile, the Challenge tile's level
        chip included, is fully visible after a vertical scroll of the column involving **no tap,
        no expander and no state change**, revealing nothing that was not already composed; and the
        column's total scrollable overflow is **≤ 120dp** — under one mode tile, one flick.
     4. Nothing is clipped, no band is dropped or collapsed, no readout is truncated, and the hero
        shows no decorative art.
     5. **If the measured overflow exceeds 120dp, or anything in part 1 is not complete in the
        first viewport, that is a failure of this criterion and it comes back to the ux-designer
        with the measured numbers** — it is not closed by trimming a readout, a label or a target,
        and it is not closed by weakening this criterion a second time. Every dp in §4.2.3 is
        Robolectric's; the qa-engineer's device numbers govern.
     (D-03k, §4.2.3, §4.2.1's 2026-09-28 note, D-03f, criteria 15a/15d/15h, PERS-03, CHAL-05)

15l. The compact band set's threshold reads **width as well as height**: below
     `home_compact_threshold_width` of available width Home is compact below
     `home_compact_threshold_height_narrow`, and at or above that width below
     `home_compact_threshold_height` as before. Checkable as three windows — `w360dp-h800dp`
     compact, `w360dp-h640dp` compact, `w412dp-h915dp` **full** with the hero's decorative art
     present and 15g's tall-phone behaviour intact. Nothing at or above 400dp of available width
     changes behaviour, and the font scale is still never read. (D-03k, D-03i, criteria 15g, 15i)

15m. **The last yield costs no readout.** On the narrow class each progress pill shows its label
     and its value **on one line with no decorative glyph**; all three values are present and
     correct; the em-dash placeholder rule is unchanged (unknown and failed both render "—",
     neither ever renders `0`); and TalkBack announces each pill **exactly once, as "label,
     value", word for word as the full set does**. No pill is tappable or focusable, neither label
     nor value is truncated or scaled down, and at default font scale at 360dp width the whole
     ribbon band measures **≤ 56dp** — if it measures more, the band grows and the column scrolls
     rather than anything clipping. (D-03k, D-03c, D-03h, criteria 13, 13a, 14, 15j, §11)
     *(Pointer 2026-10-21 — D-03o.)* Unchanged at default font scale. Above it, the one-line shape
     gives way to the stacked one for all three pills at once, and no label or value is ever broken
     inside a word; criterion 15w checks it.

**Home hero (D-03l, §4.2.4 — founder-feedback amendment, added 2026-09-28)** — these five apply to
the slice that lands D-03l and retro-certify nothing. Criteria 11, 14, 15, 15a's chips half, 15d,
15g, 15i, 15j, 15k, 15l and 15m stand unchanged; in 15l's third window, "with the hero's decorative
art present" now reads **"with the stage art present"**.

15n. **No logo.** Given Home on any window in any state, then no decorative art is composed above
     or beside the title, and `grep -rn 'HomeHeroArt' app/src/main` returns 0 hits. (D-03l)

15o. **The title sits in one row with the gear.** Given Home at default font scale on
     `w360dp-h640dp` and on `w412dp-h915dp`, with the system-bar insets supplied, then: the title's
     start edge equals the column's content start edge (±1dp); the gear's end edge equals the
     column's content end edge (±1dp); the title's and the gear's vertical ranges overlap; no node
     is composed above the title; and the title renders at `displaySmall` (36sp) with its full
     text, neither truncated nor scaled. At font scale 2.0 the title may wrap and the row grows,
     but it is still never truncated or scaled and the gear stays in the row. (D-03l, criteria 14,
     15i)

15p. **No empty middle gap.**
     1. Given Home at default font scale on `w412dp-h915dp` with a 24dp status bar and a 48dp
        navigation bar supplied (843dp usable), when there is no Continue chip, and again when only
        the Classic Continue chip is present, then the stage art is drawn with an edge **≥ 120dp**,
        and **no vertical distance between two consecutive visible elements** — hero row, stage art,
        ribbon, each chip, each tile — **exceeds 24dp**.
     2. Given each of `w360dp` × `h640dp`/`h720dp`/`h800dp` and `w412dp` × `h780dp`/`h850dp`/`h915dp`,
        insets supplied, in every chip state (none, Classic only, Challenge only, both), then no
        vertical distance between two consecutive visible elements above the second mode tile,
        measured along the column whether or not it is scrolled, exceeds **80dp** where the window
        takes the full band set or **64dp** where it takes the compact set.
     3. If the device's own band heights put the one-chip stage art below 120dp, that is a failure
        of this criterion and it returns to the ux-designer with the measured numbers; it is not
        closed by shrinking a band, a readout or a target. The qa-engineer's device numbers govern.
     (D-03l, criterion 15g)
     *(Ruled 2026-09-29 — **D-03m**, §4.2.4 last subsection. The words stand; what they measure is
     now settled. A "visible element" is a drawn surface or a laid-out text, measured by its layout
     bounds. It is never an undrawn box, glyph ink or a shadow. The hero row's edges are the union of
     the title's bounds and the gear's. On that reading the builds measured on 2026-09-29 do **not**
     meet part 1: hero→art is **27.4dp** on the Pixel_9, in every chip state. The cause is this
     document's 72dp row floor, not the implementation, which built what §4.2.4 said. D-03m removes
     the floor. This applies to the slice that lands D-03m, and the device re-walk in the real faces
     owed under Q-013 is the reading that closes it.)*

15q. **The stage shows a real game, and holds still.** A unit test replays a recorded move list
     from `Boards.english33Initial` — every move legal under the shipped rules engine — and arrives
     exactly at the stage art's peg set, which holds 12–16 pegs. The board state the stage draws has
     no selected peg, no legal targets and no hint, so nothing on it animates on any frame.
     (D-03l, D-03g, §10)

15r. **The stage is decoration.** The stage and its art are neither clickable nor focusable and
     announce nothing; Home's TalkBack traversal is unchanged — title → gear → ribbon pills →
     Continue chips → mode tiles; and the stage renders identically before and after `HomeUiState`
     receives its first progress emission. (D-03l, §11, criteria 15f, 15j, D-03g)
     *(Pointer 2026-10-21 — D-03n.)* "Renders identically" binds the **progress** emission, as it
     always said. A Continue chip's arrival is a different emission and, by D-03n, the one that is
     allowed to change the stage: the stage gives up the chips' room, and its art shrinks with it.
     Criteria 15s–15v check that emission.

**Home when the Continue chips arrive (D-03n, §4.2.5) and the pill at a large font scale (D-03o,
§4.2.6) — added 2026-10-21** — these apply to the branch that lands them, `bugfix/home-chip-arrival`,
and retro-certify nothing. Every criterion above stands; 15r gains a pointer.

15s. **Nothing a player can tap moves when a chip arrives.** Given Home at default font scale, with
     NATIVE text metrics in the app theme and a 24dp status bar and a 48dp navigation bar supplied, on
     each of `w360dp` × `h640dp`/`h720dp`/`h800dp` and `w412dp` × `h780dp`/`h850dp`/`h915dp`, when
     `HomeUiState` is emitted with no chip, then the Classic chip only, then both, then none again —
     and separately with no chip, then the Challenge chip only, then both — then in every emission:
     1. the title's, the gear's and both mode tiles' bounds equal their no-chip bounds (±0.5dp);
     2. the distance from the second mode tile's bottom edge to the bottom of the viewport is the same in
        every emission, and equals one fifth of the no-chip slack S₀, plus one band gap and the column's
        bottom padding (±0.5dp), S₀ as §4.2.5 defines it;
     3. the stage's height is max(0, ⅘ S₀ − R) (±0.5dp), where R is the room the chips present take,
        each chip's height plus one band gap; and wherever the stage is above 0, the ribbon's top edge
        sits higher than its no-chip top by exactly R (±0.5dp);
     4. every chip present is complete inside the first viewport (§4.2.1 oracle 3), and the column's
        scrollable overflow is what it is with no chip.
     Today's code fails it: the tiles move 22–24dp per chip. If on any of these six windows the stage
     cannot give the room at default font scale (R > ⅘ S₀), that comes back to the ux-designer with the
     numbers; it is not closed by letting the tiles move. (D-03n, D-03f, criteria 15g, 15k, 15p)

15t. **Where the stage cannot give the room, the tiles move by the shortfall and no more.** Given the
     same emissions at font scale 2.0 on `w360dp-h640dp` and `w412dp-h915dp`, insets supplied, then
     between any two emissions the mode tiles sit exactly max(0, R − ⅘ S₀) (±0.5dp) below their no-chip
     position, never above it; the hero row does not move; nothing is clipped and no band is dropped;
     and where the column overflows, it scrolls (criteria 14, 15i). (D-03n)

15u. **The room opens; nothing jumps.** With OS animations on and the test clock paused, when a chip
     arrives (no chip → Classic only) and when chips leave (both → none), then in every frame from the
     emission until the layout settles: the hero row's and both mode tiles' bounds equal their settled
     bounds (±0.5dp); the ribbon's top edge moves monotonically from its old position to its new one and
     never past it; and no frame shows a chip's band gap without the chip. The room starts opening in the
     frame after the emission and is fully open after `ClayAnim.ENTRANCE_MS`. With animations off
     (`rememberAnimationsEnabled()` false), the frame after the emission is the settled layout.
     (D-03n, §10, criterion 25)

15v. **On the device, Home holds still and draws whole.** On `Pixel_9a_bench` at its native window and
     at 360×640dp (`wm size` / `wm density`) — **only after the branch's own 15e sitting, in the same
     session**, as the tech-lead's H-3 ran its device checks, because the bench is otherwise reserved
     for 15e (§6.5 precondition 1; `tech-lead-signoff-close.md` D-1 condition 3; if the tech-lead's
     text says otherwise, it governs) — the qa-engineer screen-records a cold start for a player with
     one save and for one with two, **twice each: once with OS animations off and once with them on**,
     and reads each recording frame by frame from first paint until the chips have settled. Afterwards
     QA restores `wm size`, `wm density` and the seeded saves, as H-3 did.
     1. **With animations off — the position read.** Every frame is the layout. The hero row and both
        mode tiles sit on the same pixels (±1px) in every frame. The ribbon moves only upward, by the
        chips' room, and only in the frame the chips arrive.
     2. **With animations on — the motion read.** Two motions are allowed and no third. One is §10's
        entrance cascade, in which each band drawn at first paint fades and slides up into place over
        `ClayAnim.ENTRANCE_MS` after its stagger. The other is the chips' room opening (§4.2.5, "The
        motion"). The cascade is a drawing offset, not a layout change, so position is read from the
        first frame in which every band drawn at first paint has finished its entrance. From that frame
        on, part 1's rules apply, with the ribbon allowed to glide upward while the room opens. In the
        settled frames, the hero row's and the tiles' pixels equal those of the animations-off recording
        of the same window and state (±1px).
     3. **If the chips land while the cascade is still running** — possible on a phone that decodes the
        snapshots quickly — no band's entrance restarts or changes its target. Every band finishes its
        entrance at the position it holds in the animations-off recording, and the position read starts
        from the first frame in which both the cascade and the room have settled. A band that ends its
        entrance somewhere else, or whose entrance starts again, fails.
     4. **In every frame of both recordings, the cascade included:** the first frame that draws the stage
        slab draws its pegs (they fade together, at one alpha), and the first frame that draws a mode tile
        draws its art.
     (D-03n parts 1, 3 and 4; §10; criteria 15f, 15u, 25)

15w. **No word breaks; one shape per ribbon.** Given Home on `w360dp-h640dp`, insets supplied, with
     NATIVE text metrics in the app theme (the real faces), at font scales 1.0, 1.3 and 2.0, with Stars
     401, Boards 0/5 and Level 111 — again with Stars and Boards at "—", before the progress emission —
     and again with a five-digit level, 12345:
     1. every pill's label and every value lay out on exactly one line each, so nothing is broken inside
        a word or a number;
     2. at 1.0 all three pills keep D-03k's one-line shape, and the band is ≤ 56dp (15m);
     3. at 2.0 all three are stacked — each value's top below its label's bottom — at one height and one
        width (±0.5dp);
     4. at every scale, the three pills share one shape, and the shape and height with "—" equal those
        with the values: the progress emission changes no pill's shape or height (15f) — except in
        §4.2.6 rule 4's case, a value too wide to sit beside its label, which none of these values is;
     5. each pill's description is byte-identical to the full set's ("Stars, 401").
     At 2.0 on `w412dp-h915dp`, every full pill's label and value lay out on one line each. On a device at
     360dp and font scale 2.0, in light and dark, "Stars", "Boards" and "Level" read whole. Today's code
     fails part 1 at 2.0. (D-03o, criteria 14, 15f, 15i, 15m)

**In-game controls (D-04) and the toolbar (D-05)**

16. The gameplay screen has **no Material top app bar**: no bar background, no bar elevation, no
    bar title row. (D-05)
17. A visible, labelled back control remains on the gameplay screen and returns the player to
    where they came from; system back does the same. (D-05b)
18. The elapsed timer is still visible during play and is still announced as the elapsed time.
    (SCOR-02, D-05a)

**The status strip, re-sized (D-05d, §4.3.1 — founder-feedback amendment, added 2026-09-28)**

18a. **Bigger, and measured.** Given gameplay at default font scale, then the title's rendered text
     style is `titleLarge` (22sp) and the timer's is `headlineSmall` (24sp) carrying the `tnum`
     feature — read from the text layout, not assumed — and the timer pill is ≥ 40dp tall; and on
     `w360dp` both "Peg Solitaire" (Classic) and "Level 1234" (Challenge) render with no ellipsis.
     (D-05d, SCOR-02, CLAS-02)

18b. **Each system bar once.** Given gameplay on `w360dp-h640dp` with a 24dp status bar and a 48dp
     navigation bar supplied, then the status strip's top edge is at 24dp (±1dp) and the dock
     slab's bottom edge is at 576dp (±1dp) — 640 less the 48dp bar and the dock's 16dp margin. On a
     device, the back chip's top edge is within 8dp of the status bar's bottom edge. (D-05d)

18c. **The board keeps its space.** Same window: the board slab's edge is **328dp** — the window
     width less 2 × `board_screen_margin` — at font scale 1.0 **and** at 2.0. (D-05d, D-04)

18d. **The timer does not jitter.** On a device with the shipped font — the shipped font confirmed
     on that install by the discriminators in `tech-lead-fonts.md` — the timer pill's width is
     the same (±0.5dp) at 00:00, 08:08, 11:11 and 59:59. (D-05d, §6.3; the discriminator clause
     added 2026-09-29, tech-lead font ruling)

18e. **At 200% the timer wins.** Given gameplay on `w360dp` at font scale 2.0, then the timer's
     text is fully visible — not clipped, not ellipsised — in a pill that has grown to hold it; only
     the title may ellipsise; and TalkBack still reads the title's full text ("Peg Solitaire", or
     "Challenge level N"). (D-05d, §11, criterion 18)

**The timer's spoken form (D-05e, §4.3.2 — added 2026-10-21)** — these apply to the branch that lands
D-05e, `bugfix/home-chip-arrival`; criteria 18 and 18a–18e stand unchanged.

18f. **The timer's description carries its value.** Given the status strip with `elapsedSeconds` of 0,
     1, 45, 60, 61, 312, 720, 3599 and 6000, then the clock's single accessibility node has, word for
     word, §4.3.2's sentence for that time as its content description — "Elapsed time, 0 seconds"
     through "Elapsed time, 100 minutes" — starting with the `elapsed_time` string; the node has no
     spoken children (the cells stay hidden, as N-2 asserts); and it carries no live region. Today's
     code fails it: the description is "Elapsed time" alone. (D-05e, SCOR-02, criterion 18)

18g. **TalkBack says it, once.** On a device with TalkBack on and its speech output displayed — the
     "Display speech output" path QA's S11 loop 1 used — during a game: moving focus to the timer speaks
     §4.3.2's sentence for the time shown (one second either way), and leaving focus on the running
     timer for 10s brings no further speech. If TalkBack does speak again on a tick, D-05e's whole-minute
     fallback is built, and this criterion is read against it: the focused timer then speaks at most once
     a minute, as "Elapsed time, N minutes" or "Elapsed time, under a minute". (D-05e, §11, criterion 18)
19. Undo, Hint, Show-moves and Restart are all still reachable, all still work, and all four sit in
    the lower half of the screen. (ASST-01, ASST-02, D-04)
20. The Hint control does not use a magnifying-glass glyph, and the Show-moves control does not use
    the same glyph as any other control in the app. (D-04a, D-04b)
21. Turning Show-moves on and off changes the control's **fill**, not only its colour, and a
    screen reader announces which state it is in. (D-04b)

**The dock's state language (D-04e, §4.4.1 — founder-feedback amendment, added 2026-09-28)** —
criteria 19–24 above stand unchanged; these seven make "on or off" legible and checkable.

21a. **Every control is in the look its state requires.** A table-driven test over §4.4.1's "which
     control is in which look" rows: given each row's situation, the control's look property equals
     that row's look — including Undo **Unavailable** on a fresh board and **Available** after one
     jump, Hint **Working** while `solverThinking`, Hint **Available** with `hintSource` set and
     with `noWinningMove`, Show-moves **On** and **Off**, and Restart **Available**. (D-04e, ASST-01,
     ASST-02, criteria 22, 23)

21b. **No two looks share a recipe.** A unit test over the recipe data asserts that each of the ten
     pairs of looks differs in at least two of: fill (none / pale / strong), ring, shadow, glyph
     shape (normal / slashed / spinner), on mark. (D-04e, rules/compose.md "never colour alone")

21c. **A spent allowance changes nothing a player can see or hear.** Given Undo — and separately
     Hint — with its free allowance for the level spent, when the dock renders, then the control's
     look is Available, its semantics (name, role, enabled) equal those it had before the allowance
     was spent, the dock has the same nodes, and a tap raises the opt-in dialog. (D-04e; the
     earlier-phase ruling at `GameUiState.kt:131-135`; ADS-01; criterion 24)

21d. **Show-moves speaks as a switch.** The toggle exposes `Role.Switch` with `ToggleableState.On`
     when the dots are on and `Off` when they are off, and its content description is "Show
     possible moves" in both states; `grep -rn 'show_moves_off' app/src` returns 0 hits; and
     `ButtonSoundTest.kt:226-244` passes with zero diff. (D-04e, criterion 21)

21e. **Only the toggle is ever on or off.** Show-moves On draws the on mark and `ShowMovesIcon`;
     Off draws `ShowMovesOffIcon` and no mark; no other control, in any look, draws the mark, the
     slashed glyph, a `primary` fill or a ring. (D-04e, §5)

21f. **The state marks are visible.** On a device screenshot in light and in dark theme, the On
     disc and the Off ring each measure ≥ 3:1 against the dock slab. (D-04e, WCAG 1.4.11,
     criterion 27)

21g. **The greyscale read.** The qa-engineer captures the dock on a device with Show-moves On and
     with it Off, each on a fresh board (Undo Unavailable) and after one jump (Undo Available), plus
     one capture while a hint is being searched (Hint Working); converts each capture to greyscale;
     and records which look each control shows. Every reading must match §4.4.1's table without
     reference to colour. (D-04e, rules/compose.md)
22. With no moves made, Undo is visibly and announced-as disabled; after one jump it is enabled.
    (ASST-02, D-04d)
23. Requesting a hint shows a spinner in place of the glyph, the control reports as disabled while
    thinking, and a second tap cannot start a second search. (ASST-01, D-04d)
24. No control anywhere shows a hint or undo allowance count, badge or ad glyph. (§5)

**Cross-cutting**

25. With OS animations turned off, every screen in this refresh is fully usable, nothing is stuck
    mid-animation, and the tutorial still pauses long enough after a jump for the player to see it
    resolve. **Sound is unaffected by this switch** — run criterion 7c's animations-off pass at
    the same sitting; a jump that loses its cue when animations are off is a 7c failure, not an
    expected consequence of reduced motion. (rules/compose.md, §10, §14 7c)
26. Every new interactive element is at least 48dp, at least 8dp from its neighbours, and reachable
    by TalkBack with a meaningful label, in both light and dark. (rules/compose.md, §11)
    *(Pointer 2026-10-21 — §11's recorded gap.)* Whether the **tutorial board** — §4.1's one
    interactive element, on a screen this refresh made new — is inside this criterion is **pending the
    same ruling**: release scope is routed by the producer to the game-designer (and the founder), at
    `.planning/reports/ux-spec-folds/game-designer-talkback-board.md`. Until it lands, a pass of 26
    records the board's result separately and does not decide it either way. The gameplay board predates
    this refresh and is not a "new" element. It is covered by the same recorded gap in §11.

26a. **How 26 is checked, and what does not count as failing it** *(added 2026-09-28, §11 "The
     unmerged tree is not the contract")*. Criterion 26 is checked with **TalkBack running on a
     device**, on the merged semantics tree TalkBack consumes — never off a `uiautomator` XML dump.
     A control **passes** when swiping to it yields **one** focus stop that announces its label and
     its role, when activating that focus stop performs the action, and when the focus rectangle is
     ≥ 48dp. A control **fails** when a focus stop announces no label, when the label sits on
     something that cannot be activated, or when one control yields two focus stops. An
     `IconButton { Icon(contentDescription = …) }` showing up in an *unmerged* projection as a
     labelled child inside an unlabelled clickable parent is **not** a failure and is not to be
     "fixed" (§11, reason 2). Run the pass over one untouched control as the control case — the
     Classic catalogue's back arrow (`ClassicCatalogScreen.kt:102-110`) — so a finding can be told
     apart from this app's baseline. (rules/compose.md, §11)

26b. **The spoken order** *(added 2026-10-21 — D-05e, §4.3.2; criterion 26's spoken-order half,
     routed by the tech-lead's phase-15 sign-off)*. With TalkBack on and its speech output displayed,
     swiping forward from the first stop of each screen below visits exactly these stops, in this order,
     once each, and each stop's speech contains the string named. The order of name, role and state
     inside one utterance is TalkBack's own and is not judged; QA's device speaks state first.
     - **Gameplay, Classic:** `navigate_back` ("Navigate back") → `app_name` ("Peg Solitaire"), as a
       heading → the timer, with §4.3.2's sentence → `undo_action` → `hint_action`, or `hint_thinking`
       while a hint is searched → `show_moves_on`, as a switch with its state → `restart_board` → any
       raised banner. **Challenge:** the title's stop speaks `challenge_level_indicator_desc`
       ("Challenge level N").
     - **Home:** `app_name`, as a heading → `settings_action` → the Stars, Boards and Level pills, each
       once, as "label, value" (`home_stat_pill_desc`) → each Continue chip present, with its title
       (`resume_classic_title`, `resume_challenge_title`) → the Classic tile (`classic_tile_title`) →
       the Challenge tile (`challenge_tile_title`, with `challenge_tile_continue`). The stage, its art
       and the footer are never a stop.
     - **Tutorial:** the pip row (`tutorial_step_progress`) → `onboarding_skip` → the caption → on
       completion, `onboarding_got_it`.
     **The board is not a stop, on gameplay or in the tutorial.** On this branch it carries no label,
     text, role or action (`BoardCanvas.kt:196-212`; `GameScreen.kt:193-207`, whose three custom keys
     are test-only; `TutorialScreen.kt:245-263`), so TalkBack's swipe goes from the timer to Undo and
     from Skip to the caption, and that is the order this criterion expects of today's build. If a
     later change gives the board semantics, it is visited in its screen position — after the timer,
     after Skip — and is neither required nor judged here. Whether the board must become a stop, and
     whether 3.0.0 ships without it, is §11's recorded gap, pending the ruling routed to the
     game-designer (and the founder) at `.planning/reports/ux-spec-folds/game-designer-talkback-board.md`.
     A missing stop, an extra stop, a stop out of this order, two stops for one control, or a stop that
     speaks no label fails; 26a's other lines apply unchanged. Run once in light and once in dark, at
     default font scale, in the same sitting as 18g. (D-05e, §11, criteria 26, 26a)
27. Every text and glyph pair introduced by this refresh meets 4.5:1 / 3:1 contrast in both light
    and dark. (§11)
28. No screen in this refresh plays a sound as its only feedback for an event. (rules/audio.md)
29. **The system bars take the theme** *(added 2026-10-21 — D-05f, §6.2; applies to the branch that
    lands it, `bugfix/home-chip-arrival`)*. **(1)** `WindowThemeTest` resolves `Theme.PegSolitaire` at
    an API level of 27 or more under `notnight` and `night` and reads `android:windowLightNavigationBar`
    as true and false, watched failing before the theme changes; its status-bar cases stand. The
    attribute lives in `values-v27/` and `values-night-v27/`, each restating the whole style, and not in
    `values/` or `values-night/`: at the test's SDK the two placements resolve alike, so this half of the
    criterion is read in the diff and by lint's NewApi check on minSdk 24, not by the test. **(2)** On
    a device at API 36, in three-button and in gesture navigation, in light and in dark, on Home,
    gameplay and the tutorial: in dark mode the three-button bar's scrim is dark — sampled between the
    buttons, its relative luminance is at most 0.18 — with light buttons ≥ 3:1 against it; in light mode
    the bar is as it is today, light with dark buttons ≥ 3:1; the gesture handle is ≥ 3:1 against what is
    behind it in both themes; and the status-bar icons are as H-2 left them. (D-05f, §11, criterion 27)

---

## 15. Open questions for the founder (non-blocking)

Each is art direction between equals, which rules/studio.md routes to the founder. Each has a
default decided so no work waits. *(Q-901 and Q-902 are answered; Q-005 was added on 2026-09-28 by
the founder-feedback amendment and is open.)* *(Updated 2026-09-29: Q-005 is answered too — see its
entry below.)*

**Q-901 — Home hero treatment.** Options: (1) a **static** decorative peg cluster in Candy Clay;
(2) a **slowly drifting** mini board that idles behind the title; (3) no art — a larger gradient
title alone. **Recommendation and default: (1).** It fills the emptiness the founder named, costs
no asset licence, cannot distract from the two tiles, and adds no battery cost on the app's first
screen. Option 2 competes with the two `HomeBlobs` that already drift there, and it would put a
running animation on the cold-start critical path (D-03g). Option 3 is also step 3 of the §6.5
degradation path, so if C1 measurement forces it, the founder's answer is already implemented.
**Blocks: nothing.**
*(Answered 2026-09-21: option 1. **Superseded 2026-09-28** by the founder's Q-004 — having seen
option 1 built, the founder rejected the peg-cross logo. D-03l withdraws it and Q-005 below asks the
art direction again with the founder's three named problems as its brief.)*

**Q-902 — Do the in-game actions move to the bottom?** The founder said "redesign the icons on the
top" and "remove toolbar from the game" in the same breath. This spec reads that as one surface
(D-05) and splits it: read-only status stays top, the four actions move to a thumb-zone dock.
The alternative reading is to keep all four icons at the top, redrawn as a clay cluster.
**Recommendation and default: the split**, because rules/compose.md requires primary actions in
the lower thumb zone of a 6.7-inch phone and Undo — the control a peg-solitaire player uses most —
is currently in the worst-reachable corner of the screen. If the founder wants the icons visually
"on the top", §4.3 and §4.4 swap places and nothing else in this spec changes.
**Blocks: nothing.**
*(Answered 2026-09-21: the split. Unaffected by the 2026-09-28 amendment; D-04e and D-05d restyle
both halves of it without moving anything between them.)*

**Q-005 — Home hero: how should the top of Home fill the screen?** *(added 2026-09-28 —
founder-feedback amendment; queued in `.planning/questions/FOUNDER.md`)*. Per Q-004 the peg-cross
logo goes, the title is re-seated and the blank band between the stats and the cards must go. Three
directions are each buildable and each fixes all three named problems; choosing between them is
taste. Options: **(1) Board stage** — the title left-aligned beside the gear in one line, and below
it a large clay board showing a game in progress that grows or shrinks with the screen and is not
drawn on small phones (§4.2.4, D-03l). **(2) No art, one block** — no picture anywhere; the title
sits directly on the stats, and title, stats, Continue and mode cards form one block in the lower
part of the screen, with only the gear and the soft background blobs above it. **(3) Bigger cards**
— no picture; the title line as in (1); on taller phones the mode cards and Continue chips grow
taller (larger art, larger tap areas) to take the space — which changes the cards the founder said
to keep. **Recommendation and default: (1).** It removes the gap on every phone without moving the
mode cards out of thumb reach, shows the game itself instead of an emblem, and costs nothing at cold
start (one drawing, replacing the logo's). (2) leaves the top third of a tall phone empty — the
original "looks empty" complaint in a new place; (3) spends the space on the part the founder asked
to keep and needs a computed band height, since a weighted content band is forbidden (§4.2.2 clause
4). If the founder picks (2), §4.2.4's stage is deleted and the upper weight returns to a plain
`Spacer` **above** the hero row, and 15p part 1 is replaced by "the title sits directly on the
ribbon"; if (3), the stage is deleted and the tiles' and chips' minimum heights become a function of
the available height, capped. Either way 15n, 15o, 15g and 15k stand. **Blocks: nothing.**
*(Answered 2026-09-29: option 1, the board stage, which is the built default
(`.planning/questions/FOUNDER.md`). D-03m, the same day, changes nothing in option 1: the title stays
start-aligned beside the gear on one line, and the 24dp the retired row floor held goes back to the
slack, three quarters of it to the stage, so the board is larger and the title sits 12dp nearer the
top.)*

---

## 16. Traceability

| Requirement | Surface in this contract |
|-------------|--------------------------|
| ONB-01 | §4.1 Tutorial (rewritten, boards fixed by the R-4 ruling); §4.5 Settings replay row; §5.1 the tutorial's hard prohibitions |
| NAV-01 | §4.2 Home recomposition; §4.2 top bar deleted; D-01g the Tutorial nav destination |
| PERS-03 | §4.2 Continue chips — behaviour unchanged, thumbnail added; §5.1 #2/#3 — the tutorial may not clear a resume snapshot |
| CHAL-05 | §4.2 Level pill + Challenge tile, same number, same entry level; §5.1 #2/#3 — the tutorial may not advance the ladder |
| CLAS-02 | §4.3 status strip — title and timer preserved |
| SCOR-02 | §4.3 timer pill, `elapsed_time` preserved |
| ASST-01 | §4.4 Hint action, new glyph, thinking state unchanged |
| ASST-02 | §4.4 Undo action, new position, `canUndo` unchanged |
| SOUND-01 | §4.1 "The jump the player hears" + D-01i — a completed tutorial jump plays the game's own pitched jump with the removal layer under it, so the requirement's "**every** peg jump" holds on the tutorial's real jumps too; criterion 7c (including one pass with OS animations off); §13 `TutorialSoundTest`. **Added 2026-09-21:** §16 carried no SOUND-nn row, which is how D-01h narrowed a Complete requirement invisibly. |
| SOUND-02/03 | §4.1, §5, criterion 7d — the tutorial deliberately plays **none** of `Select`, `Deselect`, `InvalidTap`, `Undo`, `Hint`, `Win`, `StarPop`; gameplay and Results are untouched, so the requirements are unnarrowed. The exclusion is a stated, tested property (§13), not an absence. |
| SOUND-04 | §4.1 Completion/Exit and §4.5 — the tutorial's completion CTA plays `ButtonForward` and `Skip` plays `ButtonDismiss` via the house `withClickSound` idiom (D-01i, D-11), and the Settings "How to play" row plays `ButtonForward`; criterion 7e |
| SET-02 | Untouched — haptics gate is unchanged |
| ADS-01/02 | §5 — no allowance surface added; dialog unchanged |
| CLAS-01 | §4.2.1 — Classic catalogue and its per-board star pips untouched; one tap from Home; §5 / criterion 13b — `TutorialBoards` never joins `Boards.CLASSIC_SPECS`, so the catalogue and the Boards-pill denominator are unchanged |
| PLAT-08 (C1) | §6.5 cold-start budget **and** the two-arm measurement contract; §7 R-1/R-2 (one aggregate, D-03h); criteria 15e (delta gate) / 15f |
| QUAL-03 | §6.1 every new dp is a named token; §6.2 no hex; §6.4 durations in scoped objects |
| Proposed UX-01 | §4.1 — interactive tutorial (registry entry is the producer's to confirm) |
| Proposed UX-02 | §4.2 — Home composition |
| Proposed UX-03 | §4.3/§4.4 — gameplay chrome replacement |

*(Added 2026-09-28, D-03i.)* Three rows above gain a surface without changing what they trace to:
**PERS-03** and **CHAL-05** are now also checked on a *short* screen by criterion 15h (§4.2.2 —
both Continue chips, both tiles and the level chip on screen without scrolling on 360×640dp), and
**PLAT-08 (C1)** carries one more obligation — §6.5's two-arm measurement is taken on the branch
that lands the §4.2.2 layout, because a number measured against a layout that has since changed is
not evidence.

*(Added 2026-09-28, D-03j.)* The **PLAT-08 (C1)** row stands, with one substitution and one
addition. The *substitution*: what it traces to is §6.5's design budget plus §6.5's
**second-edition** measurement procedure, and criterion 15e is now a **relative** gate carrying no
absolute number. The *addition*: the ≤ 950 ms absolute **and** rules/studio.md's under-2-seconds
launch bar are both **PLAT-08 / phase-14's**, and §6.5 records that the second has never been
measured on a physical phone — owner, recommendation and decided default named there. The
2026-09-28 obligation immediately above, that the measurement is taken on the branch landing the
§4.2.2 layout, is unchanged and is now stated in the criterion itself.

*(Amended 2026-09-28, D-03k.)* The **PERS-03** and **CHAL-05** rows keep the extra surface the
D-03i note gave them, with the criterion substituted and the claim corrected. What checks them on
a *short* screen is now **criterion 15k**, not 15h, and what it checks is the truth rather than the
wish: **PERS-03**'s two Continue chips are complete inside the first viewport on a 360×640dp phone
with the insets applied — absolute, on every supported window — while **CHAL-05**'s level chip on
the Challenge tile is across the fold there and reached by one plain scroll of ≤ 120dp, with the
ribbon's Level pill as its above-the-fold witness reading the same `challengeCurrentLevel`
(§4.2.1's 2026-09-28 note, §4.2.3). **CLAS-01** is untouched: the catalogue and its per-board pips
are still one *tap* from Home, and a scroll is not a tap. **QUAL-03** gains three tokens
(`home_compact_threshold_width`, `home_compact_threshold_height_narrow`,
`home_stat_pill_compact_min_height`, §6.1) and no new string. **PLAT-08 (C1)**'s obligation is
unchanged and slightly cheaper to meet: the narrow ribbon removes three vector `Icon` draws from
the start destination and adds no read, no decode and no measuring pass.

*(Added 2026-09-28 — founder-feedback amendment.)* **NAV-01** and proposed **UX-02** gain §4.2.4
and criteria 15n–15r (the hero row and the stage, D-03l); **PLAT-08 (C1)**'s budget is unchanged —
the stage's one draw replaces the logo's. **CLAS-02** and **SCOR-02** gain §4.3.1 and criteria
18a–18e (the title and the timer, larger, with the insets applied once, D-05d). **ASST-01** and
**ASST-02** gain §4.4.1 and criteria 21a–21g (the dock's state language, D-04e). **ADS-01/02** gain
criterion 21c — a spent allowance changes nothing a player can see, which is the earlier-phase
ruling made testable rather than relaxed. **QUAL-03** gains seven tokens and one changed value
(§6.1), two scoped ratios, and one deleted string; no new colour and no new string.

*(Added 2026-09-29 — D-03m.)* **NAV-01** and proposed **UX-02** keep criterion 15p, which now has a
definition of how its gaps are measured (between visible elements). **QUAL-03** retires one token,
`home_hero_row_min_height`, and adds none. No string, colour or dependency changes.

*(Added 2026-09-29 — tech-lead font ruling.)* **PLAT-08 (C1)**'s budget gains one named item, the
bundled type faces (§6.5), which are measured inside 15e and are never on the degradation path.
§6.5's preconditions 1, 4 and 7 and criterion 15e change how the gate's sitting is run, not what it
decides. **CLAS-02** and **SCOR-02** gain 18d's discriminator clause. No string, colour or token
changes. One third-party asset enters the artifact, the two font families (OFL-1.1, §9).

*(Added 2026-10-21 — the tenth revision.)* **PLAT-08 (C1)**: §6.5's precondition 1 names
`Pixel_9a_bench` and the D-1 route to it, precondition 9 is new, and arm A is the branch's merge-base;
how the sitting is run changes, not what it decides. **NAV-01**, **PERS-03** and proposed **UX-02** gain
§4.2.5 and criteria 15s–15v: a Continue chip's arrival moves no target, and both chips stay complete in
the first viewport (D-03n). Proposed **UX-02** also gains §4.2.6 and criterion 15w (D-03o). **SCOR-02**
gains §4.3.2 and criteria 18f and 18g: the timer speaks its value (D-05e). Proposed **UX-03** gains
criterion 26b, the spoken order of the refreshed screens, and criterion 29, the system bars (D-05f).
**QUAL-03** gains two strings and two plurals, one more string only with D-05e's fallback, and one
scoped ratio; no token, no colour, no dependency.

---

## 17. Checker sign-off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Accessibility: PASS
- [ ] Dimension 7 Registry Safety: not applicable (native Android; no component registry)

**Approval:** pending
