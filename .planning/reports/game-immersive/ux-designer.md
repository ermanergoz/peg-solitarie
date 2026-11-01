# ux-designer report — game-immersive
Date: 2026-11-01
Branch: feature/game-immersive (committed by the producer)
Skills loaded: ergoz-soft:standards (plugin rule copies, the snapshot's VERSION 0.6.2 being older than the plugin's 1.0.0: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, dispatch.md; `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none (one haiku Explore helper located sections, files and line numbers; no studio role was dispatched)
Loops: 0
Lessons applied: none (the snapshot lists no active lesson; `.planning/recall/RECALL.md` does not exist in this worktree)

## Summary
The founder's 3.0.0 note ("during the game, android statusbar is visible but there is a gray tint. I want statusbar to be invisible and fullscreen") is ruled as **D-05g** in `docs/ux-spec.md`: the gameplay screen and Results hide **both** system bars with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`; every screen reads the bars' **stable** insets plus the cutout, so no layout anywhere moves when a bar comes, goes or slides; the bands the bars leave are the backdrop's own sky and waves; and on every other screen the status bar is transparent over `background` on every supported API level. The grey tint is unexplained on the QA bench; §6.2 lists four candidates with device discriminators, the leading one a phone below API 35 whose window is not edge to edge (the platform's opaque `colorPrimaryDark` bar). Criterion 30 (30a–30t) is the developer's and QA's contract; B-09c carries the backdrop's half. Two taste points are drafted below as non-blocking founder questions, with the recommendation built.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-immersive/docs/ux-spec.md` — header entry (2026-11-01), §1 founder-note table, §2 note, **D-05g** in §3 (after D-05f), §4.3.1 and §4.4 pointers, **§6.2 "Immersive play"** (destinations, behaviour, stable-inset layout, the bands and the grey-tint candidates, what does not change), §11 accessibility line, §12 responsiveness line, §13 test-impact table, criterion 29's dated note and **criterion 30 (30a–30t)** in §14, §16 traceability entry, frontmatter `revised:` line.
- `/Users/ermanergoz/Desktop/PegSolitarie-immersive/docs/ux/candy-backdrop.md` — **B-09c** in §1.2, the B-09 status-table row, §3.3's status-bar sentence, §5.5 bullet, §8.3 bullet, BD-09 and BD-19 amended, frontmatter `revised:` line.
- `/Users/ermanergoz/Desktop/PegSolitarie-immersive/.planning/reports/game-immersive/ux-designer.md` — this report.

## Checks
- `./gradlew check` — not run (this role has no Bash; the change is documentation only; the developer runs it on the build, criterion 30t)
- Anchor reads before every Edit: every section the amendment cites exists at the cited heading (§3 house style l.657, D-05f l.1484–1502, §4.3.1 l.3531, §4.4 l.3714, §6.2 l.4478–4515, §11 l.5252, §12 l.5897, §13 l.5958, criterion 29 l.7069–7092, §16 l.7414; backdrop §1.2, §3.3, §5.5, §8.3, §11) — pass
- Edit results: 13 of 13 `ux-spec.md` edits applied (the §4.4 pointer on a second try, inside its blockquote); 8 of 8 `candy-backdrop.md` edits applied — pass
- Consistency: no decision rewritten in place; D-05d, D-05f, B-09, B-09b, BD-09, BD-19 and criterion 29 each carry a dated pointer or note; no string, token, colour, asset or dependency added; one scoped destination policy added — pass (by Read)

## Open issues / risks
1. **The grey tint's cause is a hypothesis until read on the founder's device.** Candidate 1 (API ≤ 34: the window is not edge to edge, the platform paints `Theme.Material.Light`'s `colorPrimaryDark` as an opaque status bar on every screen, dark icons on it at ~2:1) fits "gray tint" in both themes; candidates 2–4 are in §6.2 with their tells. Criteria 30i and 30s make the read a precondition of the fix. The producer may already know the founder's phone model and API level; if so, pass it to the developer.
2. **Below-API-35 edge to edge touches D-05f's ruled mechanism** (the `values-v35/` pair, B-1/B-1b history). The mechanism for part 5 of D-05g is the tech-lead's; criterion 29 must stay green and the sitting is owed (start-path files). If the tech-lead judges part 5 too risky for a 3.0.0 this week, the gameplay/Results half (parts 1–4) ships alone and part 5 becomes a 3.0.1 item — but on a founder phone below API 35 the grey bar would then remain on Home, which the founder will see. Recommendation: ship both.
3. **rules/compose.md's F-33 line** ("the navigation host pads off the system bars once, with `Modifier.safeDrawingPadding()`") is amended for this product by D-05g: stable insets (`systemBarsIgnoringVisibility` ∪ `displayCutout`), never visible ones. A lesson candidate for the retro: an edge-to-edge app that hides a bar on any screen must read stable insets everywhere, or the other screens jump.
4. **Gesture navigation costs a double swipe for the back gesture** on the two immersive screens (first swipe reveals, second navigates). Ruled and accepted (the strip's back chip is the exit; the double swipe guards against edge mis-taps); QA records it (30k). If the founder dislikes it, the fallback is to hide the status bar only and keep the gesture bar (a one-line policy change, not a redesign) — noted in Q-B's context below.
5. **Dialog windows must mirror the hidden state** (30c, 30l): the fiddliest mechanism (a Compose `Dialog` is its own window). No fallback is specified: a bar flashing for a dialog is a fail, and a developer who cannot reach the dialog's window reports to the producer for the tech-lead.
6. **15e sitting owed** on the branch (`MainActivity.kt`, `navigation/` are start-path files, §6.5); VOID is not a pass.
7. The locator found that the spec's cited `GameStatusStrip.kt:81` / `GameActionDock.kt:95` inset calls are comments on this worktree, i.e. the built form applies each bar through the `Scaffold`. D-05d's "developer's choice" stands; §4.3.1's new pointer is written for either form.
8. **Multi-window** is not asserted beyond "the layout is the same" (§6.2).

### Proposed founder questions (non-blocking; the producer assigns ids — FOUNDER.md's latest is Q-902)

**Q-A — Fullscreen on Results too?**   (open)
Date: 2026-11-01    Asked by: ux-designer
Context: The founder asked for fullscreen "during the game". D-05g makes Results fullscreen as well so Challenge's gameplay → Results → next-level loop shows no chrome change; the bars return on leaving to Home.
Options: 1) Gameplay and Results (built, recommended) 2) Gameplay only; the bars return at every Results 3) Gameplay, Results and the tutorial
Recommendation: 1 — the bars would otherwise toggle twice per Challenge level, the one visible chrome change in the loop. 2 is a one-line policy change if preferred.
Blocks: nothing
Answer:

**Q-B — A Settings switch to keep the status bar during play?**   (open)
Date: 2026-11-01    Asked by: ux-designer
Context: With the bars hidden, the clock and battery are one swipe away (transient). Some players like to see the clock while playing; a Settings › General switch ("Show the status bar during play", off by default) would keep the bars on gameplay and Results for them. It is a string, a row, a store key and tests.
Options: 1) No switch in 3.0.0; the swipe reveal is the escape hatch (recommended) 2) A Settings › General switch, off by default, in 3.0.0 3) In 3.0.1 if players ask
Recommendation: 1, with 3 as the follow-up: no evidence yet that anyone wants it, and 3.0.0 is this week.
Blocks: nothing
Answer:

## Handoff
Next: android-game-dev — the contract, in this order:
1. Worktree `/Users/ermanergoz/Desktop/PegSolitarie-immersive`, branch `feature/game-immersive`. Invoke `compose-skill` first (rules/compose.md).
2. Read, in full: `docs/ux-spec.md` D-05g (§3, after D-05f), §6.2 "Immersive play", the §4.3.1 and §4.4 pointers, §11's D-05g line, §12's D-05g line, §13's 2026-11-01 table, criterion 29's dated note and criterion 30 (30a–30t); `docs/ux/candy-backdrop.md` B-09c, §3.3, §5.5, §8.3, BD-09, BD-19.
3. **Before any fix (30i, 30s):** run the pre-D-05g build on the founder's device, or on the model and API level the producer names, in light and dark; record the status band's colour and which §6.2 candidate it matches, with screenshots under `.planning/qa/game-immersive/`. A cause outside the four candidates goes to the producer for this role.
4. **Build, test-first (30a–30f red before green):**
   a. One bar policy: `Gameplay`, `Results` → immersive (both bars hidden, `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`); every other destination → standard, as the `else` branch. A scoped constant beside the backdrop's density mapping; applied by the nav host in the frame the destination is composed, before its first draw; re-applied on every resume while an immersive destination is current; recorded through a seam a fake can observe (its name and home are the tech-lead's).
   b. Every screen reads the bars' **stable** insets (`systemBarsIgnoringVisibility` ∪ `displayCutout`, plus the IME where a screen has one). Remove every visible-inset read: `safeDrawingPadding()`, the `Scaffold`'s default content insets, `statusBarsPadding()`, `navigationBarsPadding()`. Every node's bounds are identical with the bars visible, hidden or sliding (30d); 18b's numbers are unchanged with the bars hidden (30e); no interactive node intersects a band (30f).
   c. Every dialog window that can open over gameplay or Results — the restart confirm, the ad offer, the Play Games opt-in prompt, the profile-swap dialog — takes the hidden state and behaviour on opening and gives nothing back on closing (30c). In-composition surfaces need nothing.
   d. Edge to edge on every supported API level: the status bar transparent over `background` on the standard screens; the cutout mode set so a hidden bar never letterboxes the cutout below API 35; D-05f's `values-v35/` pair untouched or re-verified and criterion 29 green (its dated note admits the below-35 navigation bar in either form).
   e. Extend `app/src/test/java/com/app/pegsolitaire/testing/SystemBarInsets.kt` — a `visible` parameter that also sets the ignoring-visibility insets for both bar types, and a `cutout` parameter — never a second helper (§13).
   f. Change nothing else: no colour, token, string, asset or dependency; no layout number; the backdrop, its clock, densities, poses and tests untouched (B-09c).
5. **Checks:** `./gradlew check` green; the 15e sitting per §6.5 on this branch — PASS, VOID is not a pass (`MainActivity.kt` and `navigation/` are start-path files); criteria 18a–18e, 21a–21g, 26b, 29, BD-09, BD-19 and BD-21 re-run green with no test edited but §13's helper extension (30t).
6. **Report** `.planning/reports/game-immersive/android-game-dev.md`: the grey-tint cause found in step 3; the mechanism chosen for each of 4a–4d; the tests added for 30a–30f; any point where §6.2 could not be met as written, routed to this role.
7. The device criteria 30g–30t are the qa-engineer's, after the build (through the producer): gesture and three-button; light and dark; the Pixel_9 (hole-punch cutout) and a 360×640dp window; an API 30 and an API 34 image; after an interstitial and a rewarded ad (test units); after lock/unlock, recents and the shade; after rotation, a dark-mode switch and 200% font; with TalkBack.

Also for the producer to route: **tech-lead** — the mechanism rulings D-05g leaves open (the seam and where the policy lives; the shared stable-inset source and the product-level deviation from rules/compose.md F-33; the below-35 edge-to-edge mechanism beside the v35 theme pair and the cutout mode; the dialog-window mirror); **FOUNDER.md** — Q-A and Q-B above, non-blocking.
