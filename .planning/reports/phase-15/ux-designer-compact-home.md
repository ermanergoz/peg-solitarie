# ux-designer report — phase-15 compact Home, the yield after §4.2.2 (D-03k)

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (57ab1ad..HEAD — one commit, `docs(15): rule what yields on a
360x640dp Home and correct the arithmetic`; 57ab1ad is `code-auditor-s9.md`'s own commit, so this
report's parent is the re-audit that produced the finding)
Skills loaded: ergoz-soft:standards (role: ux-designer), compose-skill
Ran in parallel with: none
Loops: 0
Lessons applied: none

Rule files loaded via the standards skill: `studio.md`, `reporting.md`, `gitflow.md`,
`skills-policy.md`, `logging.md`, `memory.md`, `puzzle-design.md`, `play-store.md`,
`kotlin-android.md`, `compose.md`, `dispatch.md`. Lessons read: `.claude/rules/lessons.md` — **0
active**, hence `none` above. No `.planning/recall/RECALL.md` exists on this branch.

Staffing note, recorded because the studio's model policy asks for it: this authoring task ran on
**Opus** rather than Fable per the founder's Q-003 decision. It is an authoring role, not a review,
so rule 1 is not engaged; I reviewed nobody's work and approved nothing.

---

## Summary

Ruled MB-1's design half. §4.2.2's ≈544dp compact total was **≈156dp light** — it summed band
minimums while the bands wrap at 360dp width — so the corrected arithmetic is ≈700dp of content
against **568dp** of usable height, a 132dp gap. One yield remains that costs no readout (the
progress pill's decorative glyph and its stacked third line, ~44dp), the threshold now reads width
as well as height, and after both the set is **still ≈72dp over** with every remaining dp in a
readout, a target or a route. **So criterion 15h is retired as false and 15k replaces it**: the
chips and the first tile above the fold, the second tile across it, one bounded scroll. Retiring
15h does **not** clear MB-1.

## Outputs

- `docs/ux-spec.md` — amended: **D-03k** (the ruling), corrected wrapped-height arithmetic in
  §4.2.2, new **§4.2.3**, three new tokens in §6.1, criterion **15h retired** and **15k/15l/15m**
  added in §14, plus pointers on every place that asserted the superseded fit.
- `.planning/reports/phase-15/ux-designer-compact-home.md` — this report.

## The ruling, in five lines

1. **The criterion was the thing that was wrong**, not the layout. A 360×640dp phone with both
   Continue chips **scrolls**, by ~72dp, and 15k says so instead of asserting a fit.
2. **What yields last:** the progress pill's `contentDescription = null` glyph and its stacked
   third line — label beside value on one line. It costs **no readout, no label, no value, no
   semantics node and no target**; TalkBack cannot tell the two pill shapes apart.
3. **What the player loses on a 360dp-wide phone:** one decorative glyph per pill, the hero's
   decorative art (already D-03i's), and the Challenge tile needing one short flick. Nothing else.
4. **The threshold now reads width as well as height** (880dp below 400dp of width, 720dp at or
   above it), because the ≈156dp error was wrap at narrow width and not height at all. Consequence,
   stated: **at 360dp width Home is always compact.**
5. **The ribbon is not dropped**, though that would have made the no-scroll fit arithmetically
   possible at ≈560dp against 568dp. An 8dp margin is not a margin; it would make criterion 11 false
   on the emptiest-looking phones; and it is the one change that would genuinely breach D-03f,
   because the Level pill is the ladder position's **above-the-fold witness** once the tile chip is
   below the fold.

## Checks

**No build, no test run, and no Gradle invocation of any kind.** Deliberate and on instruction: two
concurrent Gradle builds in this worktree corrupt the test-result store (the defect the tech-lead
diagnosed for B1; `bugfix/check-gate-concurrency` is the unlanded fix), and this task is
documentation only — it touches no `app/src`, no `domain/src`, no Gradle file and no resource. The
repo's Check command is therefore **not run and not claimed**; it is owed by the developer on the
slice that lands D-03k, not by this commit. `git status --porcelain` before this commit showed
exactly `M docs/ux-spec.md` and nothing else.

- **The auditor's measured numbers this ruling is built on**, cited as given in
  `.planning/reports/phase-15/code-auditor-s9.md` MB-1 (`:121-135`, `:111-114`, `:96-99`):
  ribbon **116dp** (`88.0..204.0`), Continue chip **108dp** (`220.0..328.0`), mode tiles **108dp**
  (`452.0..560.0`) and **116dp** (`568..684`, reported `568.0..640.0` unscrolled and
  `524.0..640.0` scrolled into view), hero title 44dp inside a **72dp** row; against §4.2.2's
  floors of 64 / 72 / 96 / 96 / 72. `OVERFLOW (scroll needed) = -44.0.dp`. The auditor's own
  summary: **≈684dp of content in a 640dp viewport, ~140dp over the table.** — **accepted, and
  used as the basis of D-03k.**
- **My arithmetic on top of those numbers, so it is checkable rather than asserted**
  (`docs/ux-spec.md:1141-1205`): fixed bands 72 + 116 + 108 + 108 + 108 + 116 = **628**; gaps
  `spacing_sm` × 7 = **56**; column padding × 2 = **16**; **total ≈700dp**. Two figures are marked
  in the spec as *derived, not measured* — the second Continue chip at `336..444` and the 700dp
  column total (684 + one 8dp gap + the zero-height band-5 `Spacer` + 8dp bottom padding).
  **Reconciling with the dispatch's "~116dp":** 684 − 568 = **116dp** is the last band's *bottom
  edge* against real usable height; 700 − 568 = **132dp** adds the column's own trailing gap and
  bottom padding, which is space the column genuinely needs. The layout must clear **132dp**. Both
  figures are in the spec with what each measures. — **pass (internally consistent; every row
  traceable to a measured edge or a stated token).**
- **The usable-height figure designed against, with system bars included** — stated as a number
  because the original error regenerates otherwise (`docs/ux-spec.md:1170-1178`): **568dp** =
  640 − 24 status − 48 three-button navigation. Also tabled: ≈592dp with a gesture handle
  (~24dp bottom inset) and 640dp for the zero-inset JVM window. **The ruling does not depend on
  which navigation mode is present** — every variant is short by more than any available yield. —
  **pass.**
- **The ribbon's yield is provable by reading, not only by measuring** — the one claim I verified
  in source rather than inheriting. `app/src/main/java/com/app/pegsolitaire/ui/home/HomeProgressRibbon.kt:129-163`
  composes `padding(spacing_sm)` + glyph `home_stat_pill_glyph_size` + `spacing_xs` + label
  (`labelMedium`) + `spacing_xs` + value (`titleMedium`) + `padding(spacing_sm)`; with
  `app/src/main/res/values/dimens.xml:13` (`spacing_sm` 8dp), `:12` (`spacing_xs` 4dp), `:90`
  (glyph 20dp) that is **44dp of chrome over two text lines, ≥84dp at default scale before any
  wrap**, inside a band §4.2.2 specified at `home_stat_pill_min_height` 64dp (`dimens.xml:88`).
  §4.2.2's "it cannot yield arithmetically — ~60dp inside a 64dp minimum" **omitted the glyph and
  one `spacing_xs`** (24dp) and read a minimum as a cap. The glyph is `contentDescription = null`
  (`HomeProgressRibbon.kt:145`) inside a `clearAndSetSemantics` node (`:136`), so it carries no
  information at all. — **pass; this is where the ribbon's +52dp starts and it is the yield D-03k
  spends.**
- **The post-yield arithmetic that forces the criterion change** (`docs/ux-spec.md:1440-1462`):
  72 (hero row) + 56 (compact ribbon target) + 216 (two chips) + 224 (two tiles) + 72 (gaps +
  padding) = **≈640dp** against **568dp** → **72dp over**, with a band-by-band table of why each
  remaining dp may not yield (48dp gear + never-scaled title; oracle 3; §11's 96dp tile floor ×2
  with oracle 1's level chip; `spacing_sm` already the smaller grid step). Derived fold positions
  at 568dp: hero `8..80` · ribbon `88..144` · chip `160..268` · chip `276..384` · tile `392..500` ·
  tile `508..624`; the fold cuts the Challenge tile with ~60 of its 116dp showing, and that holds
  across the ribbon's whole 40–72dp range. — **pass (and note 640 lands within a few dp of the
  640dp zero-inset window, which is the second reason 15k requires insets).**
- **Constraints honoured, each checked by re-reading the clause it protects** — none of §4.2.1's
  four rejected simplifications is reached for (`:1060-1068`, with the near-miss second one spelled
  out); the Challenge tile's level chip, both Continue chips and the one-tap path to the Classic
  catalogue's pips are **not** put behind a tap, an expander or a state change (`:1070-1090`,
  criterion 15d's pointer at `:2545`); the 48dp gear survives as the only route to Settings and so
  to How to play (`:1458` table row); **no content band is weighted** — D-03i clause 4 is untouched
  and restated as still binding (`:629-635`, `:1375-1383`). — **pass.**
- **`file:line` of everything I touched.** One file, `docs/ux-spec.md`; sites verified by grep
  against a hash-stable snapshot of the committed bytes (file is 2856 lines):

  | Site | Line | What changed |
  |---|---|---|
  | Header, Amendments | **161** | Sixth revision: the finding, the four-part ruling, and **which build the new criteria apply to** |
  | §1, D-03 row | **218** | "visible on first paint" qualified for the narrow class |
  | §3, D-03i | **629** | Pointer: mechanism stands, arithmetic amended |
  | §3, **D-03k** | **665** | **The new decision** — four parts, full rationale, what is deliberately not done |
  | §4.2, first-paint rule | **1015** | Pointer: band 3 absolute, band 4 corrected, hero yield order superseded |
  | §4.2.1, oracle 1 row | **1042** | The Level pill becomes the above-the-fold **witness** on the narrow class |
  | §4.2.1, rejections | **1060** | All four stand and are unreachable; the near-miss second one spelled out |
  | §4.2.1, new note | **1070** | **What "behind a scroll position" means and does not** — forbidden / permitted / never for oracle 3 |
  | §4.2.2, table pointer | **1111** | First-edition table marked superseded **in its numbers**, left legible |
  | §4.2.2, **corrected arithmetic** | **1141** | Wrapped-height table, the 568dp baseline table, three consequences, the full set recomputed at ≈872dp |
  | §4.2.2, yield list | **1277** | A fourth compact step on the narrow class; "the 64dp that closes the gap" corrected |
  | §4.2.2, threshold | **1293** | "Never the font scale" stands; "height only" does not |
  | §4.2.2, "ribbon cannot yield" | **1311** | First clause stands, second was wrong, with the 24dp omission named |
  | §4.2.2, "if it still does not fit" | **1336** | **That branch was taken and is answered**; its ~24dp headroom corrected to −132dp |
  | §4.2.2, owed tests | **1363** | The 15h line superseded; the two forbidden idioms named |
  | **§4.2.3 (new)** | **1373** | The narrow class: threshold, the compact pill, the post-yield table, why the ribbon is not dropped, owed tests |
  | §5, negative contract | **1640** | "All three oracles on first paint" sharpened; nothing is ever collapsed |
  | §6.1, tokens | **1732–1735** | `home_compact_threshold_height` amended; **three new tokens** (400dp / 880dp / 40dp, all 4dp multiples) |
  | §6.1, note | **1737** | `home_stat_pill_min_height` 64dp is a minimum that has never bound |
  | §11, text scaling | **2271** | The compact pill's 200% behaviour is the same requirement |
  | §12, responsiveness | **2324** | Narrow-phone and short-phone rows sharpened by measurement |
  | §13, test impact | **2388** | **Two test idioms this refresh now forbids** (the tautology; the zero-inset window) |
  | §14, 15a | **2530** | Pointer: chips half absolute, both-tiles half not achievable |
  | §14, 15d | **2545** | Pointer to §4.2.1's bright line |
  | §14, 15g block | **2610** | Pointer: three of four stand, 15h retired |
  | §14, **15h** | **2623** | **RETIRED**, text kept verbatim |
  | §14, retirement note | **2631** | Why it is retired; **retiring it does not clear MB-1**; what this is not |
  | §14, narrow-class block | **2668** | Which build 15k/15l/15m apply to |
  | §14, **15k** | **2674** | Replaces 15h — insets required, five parts, failure routes back to me |
  | §14, **15l** | **2699** | The width term, three checkable windows |
  | §14, **15m** | **2707** | The yield costs no readout; band ≤ 56dp |
  | §16, traceability | **2830** | PERS-03 / CHAL-05 / CLAS-01 / QUAL-03 / PLAT-08 consequences |

- **Amendment conventions (house style, §3)** — additive; nothing rewritten in place; a new `D-nn`
  (**D-03k**, the next free letter after D-03j); every superseded passage left legible with a
  pointer, including the whole first-edition table and criterion 15h verbatim; the header
  Amendments note updated with a **Sixth revision**; and the applicable build stated in **four**
  places (`:161`, `:665`, `:1375`, `:2668`) because two reviewers have already returned verdicts
  against earlier wordings. — **pass.**
- **`dpSpLiteralCheck` / QUAL-03 compatibility of what I specified** — three new `dimens.xml`
  tokens, all multiples of 4 (400, 880, 40), one owner each per the file's own convention; **no new
  string**; no hex; no new colour or type role. The developer adds the tokens, I do not. — **pass
  by inspection; the gate itself is the developer's to run.**

### What the developer must now change

Plainly, in order, and none of it is in this commit:

1. **Fix MB-1 on its own terms first, before any layout change.**
   `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt:300` —
   `assertThat(bottomOf(challengeTileTitle)).isAtMost(contentBottom())` is incapable of failing and
   must go. Replace containment with **"a scroll does not move this node"**
   (`before.top == onNode(…).performScrollTo().getBoundsInRoot().top`) **and** supply the insets the
   way `TutorialScreenTest.applySystemBarInsets()` (`:299-320`) already does. Then **report what it
   says** rather than adjusting the layout to suit it. Retiring 15h does not make that assertion
   acceptable, and `docs/ux-spec.md:2644` says so in the spec.
2. **Implement §4.2.3 part 1 — the width term.** Read `maxWidth` from the `BoxWithConstraints`
   D-03i already specified (no second measuring pass): compact below 880dp of height under 400dp of
   width, below 720dp at or above it. Three new tokens per §6.1 (`:1733-1735`).
3. **Implement §4.2.3 part 2 — the compact pill.** On the narrow class only: no glyph composed,
   label beside value on one line, floor `home_stat_pill_compact_min_height`, **neither text
   truncated nor scaled**, the `clearAndSetSemantics` description byte-identical to the full set's.
   `compose-skill` and `android-accessibility` before touching it (rules/compose.md).
4. **Re-measure and report against 15k, not 15h.** If the measured overflow exceeds 120dp, or
   anything in 15k part 1 is not complete in the first viewport, **stop and route the numbers back
   to me** — 15k part 5 makes that the specified outcome, not a judgement call, and it is
   explicitly not to be closed by trimming a readout.
5. **A-10 gets the same treatment** (`TutorialScreenTest.kt:141-157`), per the auditor's handoff —
   that one is the auditor's finding, not mine, and it is advisory.

I wrote no production code, no test code and no UI code (rules/studio.md, standing rule 1); I ran
no device, no emulator and no build; and I reviewed nobody's work.

## Open issues / risks

- **The ≤ 56dp compact-ribbon target is a target, not a measurement.** The 116dp I am decomposing
  is Robolectric's, and I could attribute ~84dp of it to composition by reading and ~32dp not at
  all. If the one-line pill measures above 56dp, criterion 15m fails on its own terms and the band
  grows and scrolls — which is the specified behaviour, not a surprise — but the 72dp overflow in
  §4.2.3's table grows with it. 15k's ≤ 120dp bound has ~48dp of room for that; **if it exceeds
  120dp, 15k part 5 routes it back to me.**
- **Every dp in this ruling is JVM.** The qa-engineer's device numbers govern, and a real device may
  differ in both directions — Fredoka's metrics, the bottom inset (48dp bar vs ~24dp handle), and
  the `w360dp-h640dp` qualifier's fidelity are all unverified on glass. 15k names the insets as a
  precondition and makes a zero-inset reading **neither a pass nor a fail**.
- **D-03f's letter is relaxed for one window class** and that is a real, deliberate narrowing of a
  release-manager ruling folded into this spec. I ruled it rather than deferring it because the
  producer asked for a ruling and because §4.2.3's arithmetic leaves no alternative that keeps the
  readouts. **The release-manager and the qa-engineer should both see it** — the reason D-03f exists
  (tests 10/11/13 stay verifiable on a device) is preserved, and oracle 3 is not relaxed at all,
  but a tester will now scroll Home once to read the Challenge tile's level chip on a 360dp-wide
  phone. If either owner disagrees, the fix is theirs to ask for and mine to make.
- **Criterion 15a's both-tiles half is now known-false and is kept verbatim.** So is 15h. Anyone
  reading §14 top-to-bottom meets two criteria that cannot pass before reaching their pointers. I
  judged legibility of the refused wordings to be worth that, per how 15e's first edition was
  handled on this same branch, but it is a readability cost and a fifth revision of §14 should
  consider a "retired criteria" appendix instead.
- **15e is unaffected and still `:NOT RUN`**; D-03k adds no read, no decode and no measuring pass,
  and removes three vector `Icon` draws from the start destination, so the cold-start direction is
  favourable. But 15e's two-arm measurement is owed on **the branch that lands the layout**, and
  that is now the D-03k slice rather than the audited one — §6.5's wording ("the branch that lands
  §4.2.2") should be read as "the branch that lands §4.2.2 **and** §4.2.3". I did not edit §6.5;
  flagging it to the producer as a one-line correction the tech-lead owns.
- **`docs/audio-spec.md` still does not exist** (rules/audio.md). Unchanged by this task, not mine,
  already tracked.

## Handoff

- **android-game-dev** — the five items under "What the developer must now change". Item 1 (MB-1)
  is independent of the layout and should land first so the numbers that follow are trustworthy.
  Invoke `compose-skill` **before** writing, not after (the S9 order violation is recorded in the
  auditor's report).
- **qa-engineer** — 15k, 15l and 15m are new device/JVM checks on the D-03k slice; 15k's device
  reading **governs** over my JVM numbers, and a zero-inset reading is void. Please take 15k in the
  same sitting as 15g/15i/15j so one Home session covers the whole slack contract. 15e, 15f, 15,
  15g, 15i's device half, 25, 7a, 7c and 26 remain as the auditor left them.
- **release-manager** — one item, non-blocking: §4.2.1's dated note (`docs/ux-spec.md:1070`)
  narrows D-03f's "behind a scroll position" for 360dp-wide phones only. The oracles stay
  verifiable and oracle 3 is untouched; `11-HUMAN-UAT.md` tests 10/11/13 gain one scroll gesture on
  that device class. Say so if that is not acceptable.
- **tech-lead** — §6.5's "the branch that lands §4.2.2" now needs to read "§4.2.2 and §4.2.3" for
  15e's measurement to land on the right branch. One line, yours.
- **producer** — this is a UX ruling, not a code change: no build was run and none was needed. **No
  founder question is raised** — this is not art direction between equals, it is arithmetic with one
  defensible answer, so I ruled it rather than queueing it. The one thing worth your routing is the
  release-manager item above, which is non-blocking. Note for the record that criterion **15h is
  retired and the S9 FAIL stands**: nothing here clears MB-1, and STATUS.md should not read this
  commit as closing the S9 review.
