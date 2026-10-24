# ux-designer report — hint-undecided (check of D-04g, loop 2: §4.4.2, criteria 23a–23k, 21a's pointer, §11's traversal correction)
Date: 2026-10-23
Branch: bugfix/hint-undecided (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The product snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and dispatch; the snapshot copies (with testing, ip-policy, stacks, audio and the product's `.claude/rules/lessons.md`) came in whole through CLAUDE.md's imports. Further skills: ui-ux-pro-max, android-accessibility, compose-skill (and its `references/accessibility.md`).
Ran in parallel with: the tech-lead's amendment of `tech-lead-brief.md` for D-04g and the android-game-dev's H-commits in this worktree (per the dispatch). I treated the brief's A1.4 and §8 as stale by design and did not read `.planning/reports/hint-undecided/ux-designer.md`.
Ran on: claude-fable-5-1 at effort xhigh (the agent file's pin; the harness names no effort. Fable re-allowed for supervision on 2026-10-23, FOUNDER.md Q-019 addendum.)
Fan-out: none. Two haiku Explore helpers ran `grep`, `ls`, `wc` and `find` and returned raw output, because this role has no Bash, Grep or Glob. Neither ran git; neither wrote or committed anything.
Loops: 1 (this is the second check round of the spec; loop 1 is `ux-designer-check.md` at `008d1c5`)
Lessons applied: none. The snapshot holds 0 active lessons, and the product has no `.planning/recall/RECALL.md`.

## Summary
- **D-04g is sound on the merits and faithful to the ruling.** A tap that clears an opaque notice is safer than a blind tap that acts on a hidden cell; the dock stays live; the accessibility timeout is still honoured; TalkBack gets an equal path (a `dismiss` action, M3's own snackbar idiom); and the traversal is ruled to follow the layout, which is what Compose produces for this screen. Every id is unique, every criterion is a JVM test or a named TalkBack procedure, and the code on this branch (H3 landed, H4 not yet) can build it.
- **All five loop-1 findings and all seven INFO notes are closed** in the text (table below).
- **No BLOCKER or MAJOR. Four MINOR findings, six INFO notes.** F-1 is a faithfulness slip against the ruling's Amendment 1 that landed after loop 1 (23j still sends QA to the superseded "fifth Hint" position); F-2 and F-3 are testability gaps in 23c (a floor the test cannot reach, and a negative with no positive control); F-4 is a contradiction with criterion 26a's activation line.
- F-1 must be corrected before the qa-engineer's 23j sitting; F-2 and F-3 before the developer writes 23c's test; F-4 before the device pass. None of them waits on H4, and H4 waits on none of them.

## Findings

Severity scale, as in loop 1: BLOCKER (the amendment cannot ship), MAJOR (it must change before the build depends on it), MINOR (fix in a follow-up spec commit), INFO (no change required).

**F-1 — MINOR — Criterion 23j names the device position the ruling's Amendment 1 superseded.**
- Lines: `docs/ux-spec.md:4610-4611` ("At the game-designer's AC-18 position (Wiegleb-45, the fifth Hint)").
- The ruling's Amendment 1 (`game-designer-hint-timeout.md`, lines 21-73) replaces AC-18 with AC-18a–d because following Hint from the untouched start never leaves the opening book (AC-2c): on Wiegleb-45 the fifth followed Hint is a book answer and shows a move at once. At the position 23j names, items 1–7 cannot run; a qa-engineer reading the gloss would log a false FAIL or a Found.
- The pointer "the game-designer's AC-18 position" now resolves to AC-18a–d, which is right; the gloss contradicts it. The developer is already on Amendment 1 (`cb5463f`, "replay the ruling's Wiegleb-45 walks A to C").
- Loop 1 could not have seen this: it read the ruling at 333 lines; Amendment 1 makes it 426.
- **Asked of the spec:** replace the gloss with "the game-designer's **AC-18b** position — Wiegleb-45 after Amendment 1's walk A, played by hand with no Hint until the fourth jump is done — and one AC-20 position". Item 3 ("Hint again on the unchanged board … a fresh notice arrives") already holds there. Before the 23j sitting.

**F-2 — MINOR — 23c's 48dp clause rests on a one-line notice that this string never produces.**
- Lines: `docs/ux-spec.md:4561-4562` (23c: "at least 48dp tall at font scale 1.0 on a `w600dp` window, where it is one line"); `:2743` (Height row, "a one-line notice on a wide window"); `:3104` (the token's owner text).
- The arithmetic: the notice is capped at `board_max_size` 480dp (`:2742`), so its text column is 480 − 2 × `spacing_md` − 20 (glyph) − `spacing_sm` = **420dp**. `hint_undecided_message` is 71 characters in `bodyMedium`, which is M3's default 14sp role in the body face (`Type.kt:49`); at scale 1.0 that is ≈470–520dp of text, and even at Android's smallest 0.85 ≈400–440dp. It wraps to two lines (≈56dp) on every window at every scale, so `heightIn(min = hint_notice_min_height)` never binds and 23c's clause passes without exercising it — a green that proves nothing, the pattern §13 forbids (`:4007-4009`).
- The token is still right as a floor (a shorter translation, a future rewording); only the test's premise is wrong.
- **Asked of the spec:** test the floor where the property lives — the leaf, rendered with a short message ("Short"), measures ≥ 48dp — and word the Height row and the token's owner as "a short notice" rather than "a one-line notice on a wide window". Before 23c's test is written.

**F-3 — MINOR — 23c's "onCellTap is not invoked" has no positive control, so it cannot fail for the right reason.**
- Lines: `docs/ux-spec.md:4556-4558`; code `BoardGeometry.kt:77-94`, `BoardCanvas.kt:206-208`.
- `positionAt` returns `null` for a tap outside a cell's inscribed hit radius (`HIT_RADIUS_RATIO`), off the footprint, or in a cell's dead corner, and `BoardCanvas` then never calls `onCellTap`. A point "inside both" the notice and the canvas can therefore be one that would not reach `onCellTap` even with the notice absent — at default scale the notice covers the bottom ≈16dp of the last row, which includes dead corners and off-footprint cells. The third bullet (a tap outside the notice reaches `onCellTap`) controls a different point, not this one. §13's rule from MB-1 (`:4007-4009`) applies: an assertion that cannot fail is a defect on its own terms.
- **Asked of the spec:** one clause — "and the same point, with the notice absent, invokes `onCellTap` with the cell beneath" — or fix the point as a covered cell's centre (at 200% any bottom-row centre is covered). Before 23c's test is written.

**F-4 — MINOR — Criterion 26a's activation line now reads as a FAIL on the notice.**
- Lines: `docs/ux-spec.md:4660-4671` (26a), against `:2885-2887`, `:4623-4625` (no role, no `OnClick`, no "double-tap to activate"; Dismiss in the actions menu) and `:4636` (23j cites "criteria 25–27", so 26 and 26a reach it).
- D-04g makes the notice an interactive element (criterion 26), yet keeps it role-less with no click action by design. 26a says a control **fails** "when the label sits on something that cannot be activated"; a double-tap on the focused notice activates nothing. A careful QA pass would log that, and the spec gives no rule for which line wins.
- I agree with the design choice (the snackbar idiom; no "double-tap to activate" on a status message). What is missing is the sentence that reconciles it.
- **Asked of the spec:** add to 23j item 5 or to 26a: the notice is a status message, not a control; 26a's activation clause does not apply to it, and its pass line is 23j item 5 — one focus stop, its text, no role, and Dismiss in the actions menu that puts it away. Before the device pass.

**I-1 — INFO — 23c's first wording is abridged.**
- Lines: `docs/ux-spec.md:4553-4554` ("… a tap at a point inside both reaches `onCellTap`").
- The house rule for a replaced criterion that a reviewer passed is the first edition verbatim beneath it (`:150-152`, 15e). 23j records its two first wordings in full (`:4608-4610`). Optional: quote the whole first 23c, or point to `008d1c5`.

**I-2 — INFO — Two mechanics the contract implies, for the developer, with nothing to change.**
- "One accessibility node carrying its text" (`:4555`) needs the container to merge descendants, with the live region and the `dismiss` action on that merged node; an unmerged container would be a second, text-less stop and 23a's "its text is exactly `hint_undecided_message`" on the tagged node would fail — so the tests enforce it.
- With no `OnClick`, a TalkBack double-tap on the focused notice either does nothing or is turned into a touch by the platform's fallback; either way it is not a 23j failure, because item 5 asserts only the actions menu. QA should not log a double-tap result either way.

**I-3 — INFO — The ruled traversal order is what the code produces.**
- `docs/ux-spec.md:1093-1094`, `:2887-2894`, `:3833-3846`.
- The dock (`GameActionDock.kt:97-105`), the back chip (`GameStatusStrip.kt:124`), the timer pill (`:202`), the board slab (`GameScreen.kt:213`) and the banner (`NoWinBanner.kt:58`) are M3 `Surface`s, which are traversal groups; the board slab has no semantic descendant and the board-region `Box` carries only custom keys (`GameScreen.kt:202-207`), so neither is a focus stop. A plain-container notice is hoisted to the Scaffold's level and sorted by geometry; its bottom edge is 32dp above the dock's top (the Box's `board_screen_margin`, `GameScreen.kt:201`, plus the dock's `spacing_md`, `:261`), so it falls in its own row before the dock's. No `traversalIndex` or `isTraversalGroup` exists in the app (helper grep, 0 hits). Hence back → title → timer → notice → Undo → Hint → Show-moves → Restart, as ruled; the same holds if the notice is built on a `Surface` (a group of one).

**I-4 — INFO — The tech-lead's brief is stale against D-04g, as the dispatch expected.**
- `tech-lead-brief.md` A1.4 ("not an M3 `Surface`, so that touches pass through") and §8's `HintUndecidedNotice(message, modifier)` carry no dismiss callback. The amended brief must give the leaf, or its host, the tap-to-dismiss and the `dismiss` semantics action, and drop the pass-through reason. The reflog shows `158ffaa` ("amend the hint brief - D-04g form, freeze, AC-18 walks") after `cdf618c`; the copy I read predates it. Not a spec defect.

**I-5 — INFO — D-04g's precedent is accurate.** M3's `SnackbarHost` places `liveRegion = Polite` and a `dismiss` semantics action on each snackbar; Compose maps `SemanticsActions.Dismiss` to `ACTION_DISMISS`, which TalkBack lists as "Dismiss". `View.announceForAccessibility` is deprecated from API 36 (`:2895-2896`), as stated.

**I-6 — INFO — The tappable notice gives no press feedback and draws no boundary.** The exit fade is its feedback and its text identifies it (`:2912-2915`). Acceptable for a transient status surface; no change.

## Checks against the dispatch

**Faithful to the ruling** (GD-HT-1/2/3/5, §2, Amendment 1): **pass, except F-1.**

| Ruling point | Where D-04g keeps it |
|---|---|
| Never the rescue banner, never "no winning move" | unchanged: `:2680`, `:2786-2788`, 23a, 23b, 23h |
| No undo/restart advice, no "try again" | unchanged: `:2707-2716`, `:2986-2990` |
| Free, not counted | `:2684-2686` |
| Neutral, short-lived; clears on jump/undo/restart/Hint/leaving | items 2–5 unchanged (`:2801-2806`); item 6 adds the player's own dismissal (`:2807-2809`), which is the surface's form and this role's to rule |
| A cancelled search is silent; a deadline on an unchanged board is not | `:2681`, `:2781-2784` |
| AC-18a–d (Amendment 1) | **F-1**: 23j's gloss still names the superseded AC-18 position |

**The merits of D-04g** — judged, not deferred to either side. D-04f's pass-through made the instinctive tap on an opaque notice a move on a cell the player could not see, worst at 200% where the notice hides most of the board for up to two minutes; the undo of such a jump can reach the opt-in dialog. One tap that clears the notice removes that hazard, keeps the dock live, keeps the timeout honoured, and gives TalkBack an equal path. The price is small and bounded: for at most 5 s (or the timeout the player set), the bottom ≈16dp of the last row of holes is a dismiss target rather than a cell at default scale (`:2761`); a tap there clears the notice and the next tap plays. The alternative loop 1 floated — the first board touch after 5 s ends it — would make board taps time-dependent; the chosen form is simpler and predictable. Ruling the traversal to follow geometry (WCAG 2.2 SC 1.3.2, 2.4.3) rather than forcing the old line with a modifier is the right call, and I-3 confirms the layout gives the ruled order.

**Consistent with the rest of the spec and with phase 15's passed criteria:** pass, except F-4.
- Every D-04g touch point agrees: the header (`:299-306`, `:337-354`), §2 (`:413-419`), D-04f's pointer (`:1074-1076`), §4.4.2's Layout, coverage, items 6, TalkBack and targets (`:2743`, `:2750`, `:2756-2779`, `:2807-2809`, `:2885-2894`, `:2912-2922`), §5 (`:2969-2973`, `:2986-2990`), §6.1 (`:3104`), §7 R-7 (`:3519`), §11 (`:3833-3846`, `:3864-3896`), §12 (`:3950`), §13 (`:4057`, `:4062-4065`), §14 (23c, 23g, 23j) and §16 (`:4824-4827`).
- Criterion 23 ("a second tap cannot start a second search") is relied on, not changed (`:2567-2569`); 21a's pointer is dated (`:4494-4498`); the D-04f group heading no longer claims the rest of §14 is unchanged (`:4533-4535`).
- 26/26a: F-4.

**Unique ids, by grep:** pass.
- `D-04f`: 49 mentions, one definition (`:1052`). `D-04g`: 36 mentions, one definition (`:1078`), none in the other six worktree copies of `docs/ux-spec.md` (`-dev`, `-audio`, `-gates`, `-tb`, `-r5`, `-ux`), none in any `docs/ux/*.md`; in reports only the brief and the author's report, which cite it. `hint_notice_min_height`: one definition (`:3104`), five uses, no `dimens.xml` on any worktree declares it yet. Criteria 23a–23k: one definition each (`:4540`, `:4547`, `:4552`, `:4568`, `:4575`, `:4586`, `:4596`, `:4601`, `:4604`, `:4608`, `:4638`); 21a: one (`:4488`).

**Every criterion testable:** pass, except F-2 and F-3 (both 23c).
- JVM: 23a–23i and 23k's Compose half, with named matchers (`SemanticsActions.Dismiss`, `LiveRegionMode.Polite`, `HINT_UNDECIDED_NOTICE_TEST_TAG`), the test clock, a fake `LocalAccessibilityManager` (a providable `AccessibilityManager?` local), `StateRestorationTester`, and `Settings.Global.ANIMATOR_DURATION_SCALE` on Robolectric, which `rememberAnimationsEnabled()` reads at composition (`Clay.kt:208-218`).
- Device: 23j items 1–7 and 23k's device half are named TalkBack procedures; F-1 fixes 23j's position.

**Accessibility:** pass, except F-4.
- Polite live region, one string, focus unmoved: `:2872-2896`, 23d. Dismiss action: `:2885-2887`, 23c, 23j item 5. 48dp target, 32dp from the dock: `:2919-2922`, `:3104`, `:3868-3871`, 23c (F-2 on the test's premise). Timeout honoured, dismissal allowed: `:2792-2800`, `:3879-3881`, 23e, 23j items 5 and 7. Reduced motion: `:2860-2862`, `:3701-3703`, 23e's JVM clause, 23j item 4. Not colour alone: `:2899-2903`, `:3882-3884`, 23j item 6. Contrast not recomputed here; criterion 27 on a device governs, as in loop 1.

**Feasible against the code on this branch:** pass.
- H3 has landed: `GameUiState.hintUndecided` (`GameUiState.kt:135`, in `initial()` and `loading()`), set in the Undecided branch (`GameViewModel.kt:1405-1408`), cleared at the Hint request (`:1361`), the jump (`:933`) and the undo (`:1283`), outside `showRescueBanner` (`GameUiState.kt:152`) — exactly R-7.
- H4 has not: no `hint_undecided_message`, no `hint_notice_*` token, no `HINT_NOTICE_*` duration, no live region, no `HintUndecidedNotice.kt` (helper greps and listing). Every name the spec reserves is free.
- Host and slot: the board-region `Box` with the banner at `BottomCenter` (`GameScreen.kt:193-252`). Tap capture: a `pointerInput` with `detectTapGestures` on the topmost sibling is a direct hit that stops hit-testing before the canvas's own `pointerInput(geometry)` (`BoardCanvas.kt:206-208`), so "reaches nothing on the board" holds whether the notice is a `Surface` or a plain container; `clickable` would add the `OnClick` that 23a and 23c reject, so the tests catch the wrong choice.
- Motion: `clayEntrance` (`Clay.kt:182-201`), `ClayAnim.ENTRANCE_MS` = 240 (`ClayAnim.kt:29`), so 160 ms is two thirds; `GameAnim` has no notice constants yet (`GameAnim.kt:17-44`).
- Tokens: `nowin_banner_min_height` and `board_min_touch_target` are both 48dp (`dimens.xml:174`, `:21`), as the token row says; `board_max_size` 480dp (`:20`).
- Banner: the message `Text` (`NoWinBanner.kt:68-72`) is a sibling of the action `Row` (`:73-107`), so a live region on it leaves the buttons outside; the KDoc still says "or a budget miss, D-04" (`:29-31`), which H4b corrects. `NoWinBannerTest.kt` holds one test; the 23k test is additive.
- 21a: `DockStateLanguageTest.LOOK_ROWS` (`:122-161`) takes the two new rows as `FRESH_BOARD.copy(hintUndecided = true)` and `FRESH_BOARD`, both `Available`.
- The snackbar rejection stands: `ClaySnackbarHost` is M3's `SnackbarHost` and `Snackbar` (`ClaySnackbarHost.kt:54-68`).

**TalkBack order of item 6 against what Compose will traverse:** matches (I-3).

## Loop-1 findings, verified closed

| Loop 1 | Closed at |
|---|---|
| F-1 23k's branch | `:328-331`, `:2930-2936`, `:4643-4645` — rides this branch as H4b, per the brief's Amendment 1 (`e8eb5f2`) |
| F-2 Cancelled row | `:2555` (causes: jump, undo, restart, leaving; state vector stated), `:2566-2571`, `:2681`, `:4057`, `:4494-4498` |
| F-3 21a pointer | `:4494-4498`; the group heading `:4533-4535` |
| F-4 traversal | D-04g item 6 (`:1093-1094`), `:2887-2894`, §11's dated correction (`:3833-3846`), 23j item 5 (`:4623-4625`) |
| F-5 coverage at 200% | D-04g items 1–5 (`:1084-1092`), `:2750`, `:2756-2779`, `:2807-2809`, `:2919-2922`, 23c, 23j item 7 |
| I-1 audio fold-in | `:349-351` |
| I-2 tr-TR wording | `:2722` |
| I-3 recreation mid-display | `:2831-2833` (accepted, stated) |
| I-4 null `LocalAccessibilityManager` | `:2798-2800`, 23e `:4580-4582` |
| I-5 citations | `:2566`, `:2703-2705` |
| I-6 not counted as a hint used | `:2684-2686` |
| I-7 JVM reduced-motion check | 23e `:4582-4584` |

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-hint/.planning/reports/hint-undecided/ux-designer-check-loop2.md` — this report. I edited no other file and committed nothing.

## Checks
This role has no Bash. Every check is a full or ranged `Read`, or a helper's `grep`, `ls`, `wc` or `find`. Nothing ran git.
- `Read /Users/ermanergoz/Desktop/PegSolitarie/.git/logs/refs/heads/bugfix/hint-undecided` — pass: `cdf618c` is "D-04g - a tap puts the notice away; TalkBack position"; the three commits after it (`ddb6e23`, `158ffaa`, `cb5463f`) name the loop-1 report, the brief and the walk tests, so the working-tree `docs/ux-spec.md` I read is the D-04g text.
- `Read docs/ux-spec.md` `:1-460`, `:880-1130`, `:2380-4102`, `:4340-4841` — pass. The unread ranges (`:1130-2380`, `:4102-4340`) hold no `D-04f`, `D-04g`, `23[a-k]` or `21a` hit (helper grep), so the amendment does not reach them.
- `wc -l docs/ux-spec.md` (helper): 4841. `grep -n` for `D-04f`, `D-04g`, `hint_notice_min_height`, `Tenth revision`, `\b23[a-k]\b`, `\b21a\b`, `traversal`, `dismiss`, and all `^#+ ` headings (helper): as cited above.
- `grep -c 'D-04g'` over every worktree's `docs/ux-spec.md`, `grep -rn 'D-04g'` over `docs/ux/` and `.planning/reports/`, `grep -rn 'hint_notice_min_height'` over every worktree's `docs/` and `app/src/main/res/` (helper): only this worktree's spec defines either.
- `grep -rn 'traversalIndex|isTraversalGroup|liveRegion|LiveRegionMode'`, `'hint_undecided|hint_notice|HINT_NOTICE|HINT_UNDECIDED'`, `'calculateRecommendedTimeoutMillis|LocalAccessibilityManager'` over `app/src/main` (helper): 0 hits each. `grep -rn 'hintUndecided'` (helper): the sites cited under Feasible. `find … -name 'values-*'`: `values-night` only. `grep -n bodyMedium Type.kt`: `defaults.bodyMedium.body(FontWeight.Normal)`.
- `ls` of `ui/game/`, `ui/components/`, `ui/theme/` and `app/src/test/…/ui/game/` (helper): `HintIcon.kt`, `NoWinBannerTest.kt` and `DockStateLanguageTest.kt` exist; no `HintUndecidedNotice.kt` or `GameScreenHintNoticeTest.kt` yet.
- `Read` the ruling (all 426 lines, with Amendment 1), the loop-1 check (all 218), the brief (all 475, two pages), `NoWinBanner.kt`, `GameUiState.kt`, `GameScreen.kt`, `GameActionDock.kt`, `GameStatusStrip.kt`, `BoardCanvas.kt`, `BoardGeometry.kt`, `Clay.kt`, `ClayAnim.kt`, `GameAnim.kt`, `ClaySnackbarHost.kt`, `strings.xml`, `dimens.xml`, `NoWinBannerTest.kt`, `DockStateLanguageTest.kt` (all in full) and `GameViewModel.kt:1296-1420` — pass, as cited in the findings.
- **Not run:** `./gradlew check`. I have no Bash, and a docs-only diff changes no Check input.

## Open issues / risks
- **F-1 before the 23j sitting.** Until the gloss is corrected, a qa-engineer following 23j literally reaches a book answer, not the notice.
- **The brief's amendment for D-04g was in flight** (`158ffaa`); the copy I read still has A1.4's pass-through sentence and a leaf signature without a dismiss callback (I-4). The producer should route the dismiss seam to the tech-lead if the amended brief lacks it.
- **Contrast** (`:2905-2915`) was not independently recomputed; criterion 27 on a device governs.
- **TalkBack's double-tap on the notice** is unspecified by the platform's fallback (I-2); 23j item 5 is written so that this cannot produce a false verdict either way, provided F-4's sentence lands.

## Handoff
Next: producer.
- **ux-designer (the spec's author), one follow-up spec commit:** F-1 (23j's position → AC-18b, walk A) before QA's sitting; F-2 and F-3 (23c's floor test on the leaf with a short message; the positive control for the "not invoked" clause) before the developer writes 23c's test; F-4 (the 26a sentence) before the device pass. I-1 is optional.
- **tech-lead:** confirm the amended brief gives `HintUndecidedNotice` (or its host in `GameScreen`) the tap-to-dismiss and the `dismiss` semantics action, and no `OnClick` (I-4).
- **android-game-dev (H4):** nothing in this check blocks H4; write 23c's test after F-2/F-3 land, or write it to the asked-for shape now.
- **qa-engineer:** run 23j at AC-18b's walk A and one AC-20 position; read 26a as F-4 asks for the notice.
- **Commit:** this report, after reading it.

VERDICT: PASS
