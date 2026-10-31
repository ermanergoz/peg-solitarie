---
role: ux-designer
task: licences-screen (its own branch, `feature/licences-screen`, since 2026-10-18 — direction-B A9; first drafted on 2026-10-17 as candy-backdrop's companion item)
slug: licences-screen
status: draft — second edition, loop 2 (checked PASS at loop 2; the loop-2 fold is non-blocking and precedes the qa-engineer's session); address ruling after QA's F-1 (LS-14; §10 rows 38–42); device re-run rulings after QA's re-run, O-2, F-4 and F-2 (LS-15, LS-16, LS-AC19; §10 rows 43–45); LS-15 ruled for a scrolling list after QA's F-4 re-check and the tech-lead's R1 (§10 row 46)
created: 2026-10-17
revised: 2026-11-01 (LS-15 ruling, after `qa-engineer-recheck.md` and `tech-lead-f4-ruling.md` R1); 2026-10-24 (second edition; loops 1 and 2 the same day; the address ruling the same day, after the qa-engineer's F-1; the device re-run rulings the same day, after the qa-engineer's re-run)
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
sources: THIRD_PARTY.md (on this branch via `develop` c5f2c54; its 3.0.0 rows); docs/tech-design.md §3.1, §3.6, §13, §13.1 (LN-1–LN-8), §14.1 row LN, §16 #20, §17; docs/ux-spec.md §4.5, §11, §11.1 (D-TB, D-TBa; on this branch since `develop` 4069b53); .planning/questions/FOUNDER.md Q-023 (on this branch since 4069b53); .planning/reports/candy-backdrop/tech-lead-architecture.md R4, R5, D10, D11; .planning/reports/candy-backdrop/tech-lead-direction-b.md A9, E6, E7, open issue 7; .planning/reports/phase-14/research-analyst-release-docs-licences.md (rows A–J, the Apache table); .planning/reports/phase-14/research-analyst-font-licences.md (verdict 2, open issues 1–3); .planning/reports/licences-screen/research-analyst-mpl.md (the MPL-2.0 verdict, the exact PSL version, two full hashes); .planning/reports/licences-screen/ux-designer-check.md (F-01–F-18, folded in loop 1); docs/audio/CREDITS.md (feature/audio-review, the WAV set); docs/ui/DESIGN-SYSTEM.md (the no-backdrop contrast pairs); .planning/reports/licences-screen/tech-lead-brief.md (the build brief: §0–§8 and Amendment 1, A1.1–A1.9, which answer §9 item 7(f)–(k)); .planning/reports/licences-screen/ux-designer-check-loop2.md (PASS; F-19–F-30, folded in loop 2); ui/settings/SettingsScreen.kt; .planning/reports/licences-screen/qa-engineer.md (F-1; the §8.2 item 1 note on `play-services-ads-identifier`) and its screenshots and TalkBack transcript under `.planning/qa/licences-screen/` (the address ruling); .planning/reports/licences-screen/android-game-dev.md (deviations 5 and 6; "Loop 2 — QA F-1", `2d433b8`); app/src/main/assets/licenses/Jakarta-Inject-NOTICE.md; res/values/strings.xml (the licences block); .planning/reports/licences-screen/qa-engineer-rerun.md (O-1, O-2, F-2, F-4) and its TalkBack transcripts and screenshots under `.planning/qa/licences-screen/rerun/` (the device re-run rulings)
companion: docs/ux/candy-backdrop.md (the Sparse backdrop the four screens sit on; whichever branch lands second owns the backdrop rows, E7)
---

# Peg Solitaire — In-app licences surface

> rules/ip-policy.md asks for attribution "in THIRD_PARTY.md **and** in the product's licenses screen
> or page". The app has neither a screen nor a page (THIRD_PARTY.md §7: "No in-app licences screen
> exists"). This document specifies the screen: a **Licenses** row in a new Settings **About** section,
> a list of what ships with its licence, and a reader for the licence texts the app bundles. It is a
> design document; it contains no code.
>
> **Second edition — 2026-10-24 (ux-designer, on `feature/licences-screen`).** The first edition
> (2026-10-17) was written on `feature/candy-backdrop` against THIRD_PARTY.md as it then stood. Since
> then five things changed, and this edition folds each in. In the house style of `docs/ux-spec.md` and
> `docs/tech-design.md`, no earlier wording is deleted: a changed decision keeps its first text with a
> dated *superseded* pointer, a changed table keeps the rows it replaced, and §10 is the record.
>
> 1. **The tech-lead's R4 departures D10 and D11** (`candy-backdrop/tech-lead-architecture.md`, the
>    "Where this report departs" table), still outstanding on 2026-10-18 (`tech-lead-direction-b.md`,
>    open issue 7): the text body is a `Column` in `verticalScroll`, not a `LazyColumn` (**D10**, now
>    LS-08); the Licenses list has no ViewModel and the text screen has one (**D11**, now LS-09).
> 2. **The branch split** (`tech-lead-direction-b.md` A9, E6, E7; tech design §3.6 as corrected
>    2026-10-21): this screen ships from its own branch, cut from `develop`, whatever happens to the
>    backdrop; the Apache-2.0 carve-out travels with it; the Sparse-backdrop rows of BD-06 and BD-14
>    belong to whichever of the two branches lands second (LS-AC08).
> 3. **The notices ruling** (tech design §13.1, LN-1 to LN-8, 2026-10-21): the ten Google AARs on the
>    release graph carry `third_party_licenses.json`/`.txt` at their root (100 component names, 108
>    distinct name-and-text pairs, 42 distinct texts, 2,053,748 B raw), and AGP packages none of it. A
>    build-time task now generates them per variant into `assets/licenses/google/`, and the Licenses
>    screen shows them behind the **Google libraries** section as a scrolling list, two taps from the
>    Licenses screen (new LS-09 and §4A–§4B). The largest text, Cronet's, is 785,183 B in 2,497
>    paragraphs, which is why that one reader is a `LazyColumn` (LN-4; LS-08).
> 4. **THIRD_PARTY.md's new shipped rows** (research-analyst, 2026-10-18 and 2026-10-21; tech design
>    §13.1 LN-5): credited entries for Protocol Buffers (both copies), Chromium (Cronet), the Public
>    Suffix List (MPL-2.0, with the source pointer MPL §3.2(a) asks for), Jakarta Dependency Injection
>    (its `NOTICE.md`), the Apache-2.0 libraries behind the Google SDKs, and Checker Framework qualifiers
>    (credited by the tech-lead's decision although R8 keeps none of it). U-1 closed on 2026-10-21, so the
>    inert Google entry now names the User Messaging Platform, and the Play inline-install library
>    (`hsdp`) with it. The first edition's "one entry per further §5b family" placeholder is resolved.
> 5. **The house accessibility rules as they now read** — rules/compose.md (48dp targets, 8dp
>    spacing, text scaling, light and dark, no hard-coded colours or dimens) and the founder's Q-023
>    answer (option 1, 2026-10-21, `.planning/questions/FOUNDER.md`), which this role reads as: the
>    studio ships nothing that TalkBack cannot use (the question's own context: "every app meets its
>    platform's accessibility rules before it ships", and the founder chose the one option that meets
>    it). §5 is rewritten as a **TalkBack contract** for all four screens (LS-13), in the shape of
>    D-TB and D-TBa (`docs/ux-spec.md` §11.1), with the pass line QA reads.
>
> Nothing else is reopened. LS-01, LS-03 to LS-07, §2, §3.1 and §3.3 stand as written; the sounds the
> Sounds section names are still the `feature/audio-review` WAV set, which THIRD_PARTY.md now carries as
> 3.0.0's rows (§3.2).
>
> **Loop 1 — 2026-10-24 (ux-designer, after the fresh check, `ux-designer-check.md`: FAIL on the
> acceptance side, PASS on the licence content; and the research-analyst's MPL-2.0 verdict,
> `research-analyst-mpl.md`: CLEAN).** The check's eighteen findings are folded, each at the line it
> named and indexed in §10 (rows 11 to 24): the lazy body's scroll oracle (F-01, LS-AC15, LS-AC10);
> the merge every entry card carries, so an inert entry is one stop (F-02, §3.1, §5.1, LS-AC06); nine
> curated texts, not eight (F-03); the notices list's position oracle and the collection semantics it
> needs (F-04, LS-AC13, routed to the tech-lead's brief); the announcement pass line (F-05); LS-AC12's
> release procedure, its ten AARs and the debug-for-release statement (F-06, routed to the brief);
> Q-023 and D-TB cited from this branch, where `develop` 4069b53 brought them (F-07); same-name notice
> rows given an ordinal (F-08); the section headers as headings on all four Settings sections (F-09,
> routed to the brief); and the nine Low findings (F-10 to F-18). The research-analyst's verdict closes
> §9 item 6, confirms LS-11, and adds one condition the Public Suffix List entry now meets: it names the
> exact version of the list the app ships (§3.2, §6). No decision is reopened; the THIRD_PARTY.md ↔
> screen mapping is unchanged.
>
> **Loop 2 — 2026-10-24 (ux-designer, after the second fresh check, `ux-designer-check-loop2.md`:
> PASS, with twelve non-blocking findings F-19 to F-30 to fold before the qa-engineer reads §5.1's
> pass lines; and the tech-lead's build brief, `tech-lead-brief.md`, whose §8 and Amendment 1 rule
> the build and answer every item §9 item 7 owed).** The android-game-dev is building to the brief
> while this fold lands; nothing here changes the build, and where a line below records a brief
> ruling it is the brief's wording that governs the code. Folded, each at the line the check named
> and indexed in §10 (rows 25 to 37): the heading oracle asserts the property per section header
> shown, not a count (F-19); the bundled text screen's title for an unknown id is `licenses_title`
> (F-20); LS-AC12 asserts **equality** on the ten AARs (F-21); this branch writes
> `UserFacingTextHasNoProtectedNamesTest` and candy owns its word list thereafter (F-22); the
> entrance stagger is capped at eight items on both lists (F-23, a UX number); the notice text
> screen's title is "Notice" for any null header, `Loading(null)` included (F-24); the generated-asset
> and hash tests are Robolectric, not plain JVM (F-25); the conditionals on the collection semantics
> and on debug-for-release are closed by the brief's A1.2 and A1.3 (F-26); and the Low wordings
> F-27 to F-30. The check's open issue 3 (whether a selection span is assertable under Robolectric)
> narrows LS-AC05's Compose clause here, so the developer never has to drop a test. No decision is
> reopened.
>
> **Address ruling — 2026-10-24 (ux-designer, after the qa-engineer's device session, FAIL on F-1,
> and the developer's fix `2d433b8`).** QA heard each of the five source addresses as a TalkBack
> stop of its own, because its `SelectionContainer` was focusable inside the merged card. The fix
> makes the address plain text inside the card's one stop, and so it can no longer be copied. Three
> rulings, indexed in §10 rows 38 to 42:
> 1. **Copying is deferred, not required for 3.0.0.** The loss is accepted for this release, and
>    the follow-up's shape is fixed in the new decision **LS-14**: a long-press and the same
>    long-click action by ear, never an accessibility-only action.
> 2. **The developer's two string choices are confirmed:** the Jakarta credit "The Eclipse Jakarta
>    Dependency Injection project", taken from its NOTICE, and the U+2011 hyphens in the Public
>    Suffix List's date (§3.2, §6), with one non-blocking by-ear check (§8.2 item 12).
> 3. **A token wider than the line may break between two letters at 200 %.** This is accepted for
>    3.0.0, with the finer break recorded as optional polish (§5.1, §9 item 16).
> LS-05, §3.1, §3.2 and §5.1 carry dated pointers where "selectable" was written. No other
> decision is reopened, and nothing in the build changes.
>
> **Device re-run rulings — 2026-10-24 (ux-designer, after the qa-engineer's re-run on `2d433b8`,
> `qa-engineer-rerun.md`: FAIL on F-2, F-3 and F-4).** F-1 is fixed on the device, and pass lines 1
> and 3 pass. The re-run raised three things this role owns. F-3, the width cap, needs no UX ruling:
> §3.1, §4, §4A and §4B already say "capped at `content_max_width`, centred". Three rulings, indexed
> in §10 rows 43 to 45:
> 1. **O-2, the notices list's total by ear.** TalkBack 16 says "k of over a hundred" for the 108
>    rows, so "108 of 108" is never heard. The position is what the player needs, and it is spoken
>    exactly. The total is TalkBack's own wording. LS-AC13 and §8.2 item 10 now pass on the
>    position: every row's position is its line number in the index, and the last row's is N. The
>    exact total stays with the Compose half (`rowCount` N).
> 2. **F-4, heading navigation on the Licenses list: required for 3.0.0** (new decision **LS-15**).
>    From any stop and at any scroll position, heading navigation reaches every heading the §5.1
>    table marks, in both directions, and stops on nothing else between two headings. A list that
>    composes only what is on screen is not an accepted limit for this screen. What TalkBack does
>    past the last heading or before the first is the platform's, and pass line 4 now names it. The
>    mechanism is the tech-lead's, ruled in parallel.
> 3. **F-2, text direction: §4's intent confirmed and made exact** (new decision **LS-16**, new
>    criterion **LS-AC19**). Every text reads in its own direction, so English keeps every character
>    in its written order in any locale. Where the lines hang follows the surface: on the two lists,
>    the mirrored layout's start edge (the right, in an RTL locale); on the two text screens, the
>    document's own left edge.
> §4, §4A, §4B, §5, §5.1, LS-AC06, LS-AC13, §8.2 items 2, 6 and 10, and §9 carry dated pointers.
> No other decision is reopened. These rulings were made on Opus, not on the role's pinned Fable
> (the founder's Q-019, option 3).

## 0. Scope and inputs

**Specified here:** the Settings entry point, the Licenses screen, the License text screen, what they
list and what they never list, their copy, accessibility, assets and acceptance criteria.
*(Second edition, 2026-10-24:)* also the **Google notices** screen and the **Google notice text**
screen (§4A, §4B), the entries THIRD_PARTY.md gained on 2026-10-18 and 2026-10-21, the TalkBack
contract for all four screens (§5), the tests the developer writes and the checks QA runs (§8).

**Inputs:** `THIRD_PARTY.md` on `feature/phase-14-release-docs` (§1 ideas, §2 assets, §4 direct
dependencies, §5b families, §6 unverified rows, §7, §8); `SettingsScreen.kt` and `strings.xml` on
`feature/phase-15-ux-refresh`; the bundled `assets/licenses/Fredoka-OFL.txt` and `Nunito-OFL.txt`;
the candy-backdrop verdict's §3a and §6; `docs/ux-spec.md` §4.5 and §11.
*(Second edition:)* `THIRD_PARTY.md` as merged to `develop` at c5f2c54 and carried on this branch
(its rows A–J, the Apache table, the Jakarta NOTICE and the Apache-2.0 text row, all describing what
3.0.0 ships); `docs/tech-design.md` §3.6 and §13.1 (LN-1 to LN-8); the two tech-lead candy-backdrop
reports (R4, R5, D10, D11; A9, E6, E7); the two research-analyst licence reports (the hashes the
bundled texts are pinned to); Q-023 (`.planning/questions/FOUNDER.md`) and the D-TB and D-TBa spoken
contract (`docs/ux-spec.md` §11.1), both on this branch since `develop` 4069b53 was merged in (loop
1; the second edition read them on `feature/ux-spec-folds`); `docs/audio/CREDITS.md` on
`feature/audio-review` (the WAV set's seven, two, one and two). *(Loop 1:)* the fresh check
(`ux-designer-check.md`, F-01 to F-18) and the research-analyst's MPL-2.0 verdict
(`research-analyst-mpl.md`).

**Not in scope:** editing THIRD_PARTY.md (the release-manager's), adding a dependency to generate the
list (the tech-lead's, LS-03), a developer-website licences page (founder-only work; the in-app screen
satisfies "screen or page" on its own).
*(Second edition:)* also not in scope — the Gradle task that generates the Google notices (tech design
§13.1 LN-2 and LN-3: the tech-lead's design, the android-game-dev's build; this document only consumes
its two outputs, the index and the texts), the `THIRD_PARTY.md` "How credited" cells (the
release-manager transcribes §3.2's entry names into them), and any verdict on a licence (the
research-analyst's; every text this screen bundles already carries one).

**Spelling.** User-facing copy is US English — "Licenses" — because `res/values/` is the app's en-US
default for Play. This document and THIRD_PARTY.md keep British spelling. *(Second edition:)* "notices"
is spelt the same in both, so the Google screens need no second spelling.

## 1. Decisions

- **LS-01: The entry is a "Licenses" row in a new "About" section of Settings, directly above
  Privacy.** — Privacy stays pinned last (Phase 9 D-14: new rows go above it). Licences are not a
  privacy matter, and General holds things the player adjusts plus the tutorial; an About group is the
  platform's convention for this row. Inserting a section above Privacy is a renumbering of the entrance
  orders, which `SettingsScreen.kt`'s own KDoc anticipates (§2).
- **LS-02: Two screens, both Settings children:** *Licenses* (the list) and *License text* (one bundled
  licence, full text). Both carry the backdrop's Sparse density (`docs/ux/candy-backdrop.md` §2).
  *(Superseded in part 2026-10-24 by LS-09: **four** screens, all Settings descendants — Licenses,
  License text, Google notices and Google notice text. "One bundled licence" is widened by LS-10: an
  entry may open more than one bundled text. The Sparse density stands for all four; which branch adds
  the two new routes to the backdrop tests is E7's, LS-AC08.)*
- **LS-03: A hand-curated, static list that mirrors THIRD_PARTY.md's shipped rows — not a generated
  dependency dump.** — It names things the way a player recognises them (a font, a sound pack, "AndroidX
  and Jetpack Compose") rather than Maven coordinates; it is offline, deterministic and testable; it
  matches THIRD_PARTY.md's family rows one for one. Considered: Google's `oss-licenses` plugin (a Play
  services activity in its own, non-Candy-Clay look, and another Google library); AboutLibraries (a
  Gradle plugin and a runtime library). Either could replace the data source later on the tech-lead's
  ruling without changing this design.
  *(2026-10-24: stands. The tech-lead's LN-1 rejected both alternatives again on 2026-10-21 for the
  same reasons, and added a third rejection, a committed copy of Google's notices, which goes stale on
  the next SDK bump. The generated Google index is **data inside one curated entry** (LN-5 item 3),
  which is why it does not breach this decision: the Licenses list itself stays hand-curated and
  static, and the only generated content is the publisher's own notices, reached through one entry
  the list names.)*
- **LS-04: Listed = every third-party item that ships and carries a licence or a credit; never the
  idea rows, own work, or anything that does not ship** (§3.2). The candy-backdrop idea row is excluded
  by name (its own Credit cell and the verdict's §6): this screen never names King, Candy Crush, or the
  word "candy" (verdict §3a).
  *(2026-10-24: stands, with one recorded exception the tech-lead made: **Checker Framework
  qualifiers** is listed although R8 keeps none of its classes (LN-5 item 1; `THIRD_PARTY.md` §5b row
  I), because a 1 KB credit replaces a per-release mapping check. The in-house sounds `sfx_invalid` and
  `sfx_hint` are own work and stay unlisted. The banned-words rule applies to the studio's own strings
  and to the curated texts of §7; the generated Google notices are third-party texts reproduced verbatim
  and are not edited to it, §3.2 "Never listed".)*
- **LS-05: The screens never leave the app.** Full texts the app bundles open in-app; licences the app
  does not bundle (CC0, the Android SDK terms) are shown by name with their source address as
  selectable text, not as a link. — The privacy policy stays Settings' one outbound link, and these
  screens work in airplane mode.
  *(2026-10-24: stands, and gains one use: the Public Suffix List's source address, which MPL-2.0
  §3.2(a) asks the app to tell recipients, is the same kind of selectable, non-linked line, §3.2. The
  Google notices are bundled, so they open in-app too: LN-1 rejected a link to Google's page on exactly
  this decision.)*
  *(Address ruling, 2026-10-24, after the qa-engineer's F-1 — "with their source address as
  selectable text" is **superseded**: an address is **plain text inside its card's one stop**,
  neither selectable nor a link. On the device each address's `SelectionContainer` made it a
  focusable node of its own inside the merged card, a second TalkBack stop that §5.1 forbids (F-1,
  Pixel 9 API 36, TalkBack 16), and the developer's fix `2d433b8` removes the container. The rule
  that the screens never leave the app stands unchanged; only copying goes, and LS-14 records why
  3.0.0 ships without it and the shape it takes when it returns.)*
- **LS-06: Tappable and inert entries look different by shape, not colour.** An entry that opens a text
  has a trailing chevron and squishes on press; an entry with nothing to open has neither and is inert
  (no click action, no focus stop as a control) — the `SettingsLinkRow` WR-06 pattern.
  *(Loop 1, 2026-10-24, F-02: "no focus stop as a control" means no Button stop. An inert entry is
  still **one** TalkBack stop, as text: every entry card carries the merge itself, §3.1 and §5.1,
  because a `clickable` is not what merges it. `SettingsLinkRow` with `onClick = null` has no merge
  today; the developer's change is routed in §9 item 7.)*
- **LS-07: Licence texts re-flow, and are never edited.** Hard line breaks inside a paragraph are joined
  so the text fits the screen at any font scale; blank lines separate paragraphs; no word is changed,
  added or removed.
  *(2026-10-24: stands for every text the four screens show, the Google notices included (LN-4 names
  the same `reflowLicenseText`). The tech-lead's R4 made it a pure function: normalise line endings,
  split on blank lines, join a paragraph's lines with single spaces, drop empty paragraphs. Its
  whitespace-only property is LS-AC05's test.)*
- **LS-08: What a text body is made of — folded from the tech-lead's D10 and LN-4** *(2026-10-24;
  `candy-backdrop/tech-lead-architecture.md` R4 and departure D10, 2026-10-17; tech design §3.6 and
  §13.1 LN-4, 2026-10-21. The first edition's §4 wording, "a `LazyColumn` … the whole column inside a
  `SelectionContainer`", is superseded and kept in §4.)* **A bundled licence text (§7) is one `Column`
  in `verticalScroll`, the whole of it inside one `SelectionContainer`, one `Text` per paragraph. A
  Google notice (§4B) is a `LazyColumn`, one paragraph per item, each paragraph in its own
  `SelectionContainer`.** — The tech-lead's reason for D10 is accepted as this role's: a
  `SelectionContainer` around a lazy list can select only what is composed, so "selectable" would have
  held for the paragraphs on screen and no more, and the longest bundled text is 16,726 B (the MPL-2.0
  text, LS-11; the Apache text is 11,358 B). The reason for LN-4's exception is accepted the same way:
  Cronet's notice is 2,497 paragraphs, composing them at once is not acceptable on the 2 s-class phone
  (tech design §10), and a notice needs no whole-text selection. What the player keeps in both kinds:
  one node per paragraph for TalkBack, selectable text, a saveable scroll position, and the same
  re-flow.
- **LS-09: Four screens, and who holds their state — folded from the tech-lead's D11 and LN-4**
  *(2026-10-24; departure D11, 2026-10-17; tech design §3.1's recorded exception and §13.1 LN-4,
  2026-10-21.)* **The Licenses list has no state holder** — it is a stateless screen fed the static
  catalogue, and its only state is the list's own saveable scroll position. **The License text screen
  has one** (it reads an asset off the main thread). **The Google notices list and the Google notice
  text each have one** (both read generated assets). — The first edition named no state holder for the
  list; the tech-lead ruled it has none, because it would hold nothing, and this role records that as
  the design: a screen with no data source, no asynchronous work and no event beyond navigation shows
  no loading, empty or error state (§3.3), and a ViewModel there would only invent one. The three
  screens that do load share one rule, from §4: nothing for the first 300 ms, then a spinner.
- **LS-10: One entry per notice-bearing family, each opening its text verbatim; an entry may open
  more than one text** *(2026-10-24; tech design §13.1 LN-5 item 1; `THIRD_PARTY.md` §5b rows F–J and
  the Apache table; the research-analyst's font report, verdict 2 and open issue 3.)* Protocol Buffers
  (one entry for both copies), Chromium (Cronet), the Public Suffix List, Jakarta Dependency Injection
  and Checker Framework qualifiers each get an entry in **Open-source software**, and each opens the
  text THIRD_PARTY.md names at the research-analyst's hash (§7). **Where a family carries a notice as
  well as a licence, its entry opens both, on one License text screen, in order, each under its own
  heading** (§4): Jakarta's `NOTICE.md` then the Apache-2.0 text; the Public Suffix List's `NOTICE`
  then the MPL-2.0 text. **The Apache-2.0 libraries behind the Google SDKs are grouped by publisher
  into four entries** (§3.2), the ux-designer's call LN-5 leaves open — the same rule the first
  edition used for "AndroidX and Jetpack Compose", "Kotlin and kotlinx" and "Dagger and Hilt": a
  player recognises a publisher and a product name, never a Maven coordinate, and a coordinate-per-row
  list would be the dependency dump LS-03 rejects. Every grouped library is still named in its entry's
  parts line, so the list stays complete by name and LS-AC03's mapping stays one-to-one with
  THIRD_PARTY.md.
  *(Loop 2, 2026-10-24, F-30: "complete by name" holds for the four Apache-table groups, whose
  coordinates the research-analyst named one by one, and not for the two publisher families — the
  AndroidX and Kotlin entries name products (Compose, Room, "coroutines"), not the §5b family's
  members (annotation, collection, savedstate, sqlite, startup, profileinstaller, emoji2, tracing,
  versionedparcelable, atomicfu …). The property LS-AC03 tests, and THIRD_PARTY.md's own, is
  **one-to-one by row or family** (§3.2.1): every shipped row or family reaches exactly one entry.
  That sentence replaces "complete by name"; nothing in the design changes.)*
- **LS-11: The MPL-2.0 text ships** *(2026-10-24, a UX decision on the half LN-5 left to this role:
  "the full MPL-2.0 text is optional".)* The Public Suffix List entry opens OkHttp's `NOTICE` (218 B,
  which names the data's source and MPL-2.0 and so meets §3.2(a)) **and then the MPL-2.0 text itself**
  (16,726 B). — Every other entry in Open-source software opens the full text of its licence; an
  entry whose licence is named but not readable would be the one exception on the screen, and LS-05's
  rule that the screens work in airplane mode means the player could not go and read it. The cost is
  one more verbatim asset at a recorded hash (§7) and one more line in the research-analyst's
  non-blocking confirmation (§9).
  *(Loop 1, 2026-10-24: the confirmation is in — `research-analyst-mpl.md`, verdict 1: **CLEAN**,
  the text may ship verbatim in a commercial app; the steward reserves only modified and new versions
  (MPL §10.1, §10.3) and its own FAQ calls shipping the text "the best and easiest way". LS-11 stands.
  The same report adds one condition on the entry, not on the text: MPL §3.2(a)'s "such Covered
  Software" is the version the app ships, so the entry names the exact list compiled into okhttp
  4.11.0, not only the live list — §3.2's row and §6's `licenses_psl_address`.)*
- **LS-12: The Google notices list shows the publisher's names as supplied, with where each was
  found, and merges nothing** *(2026-10-24; tech design §13.1 LN-3 and LN-4.)* Each row of the index
  is one row of the list: its name as the title, and a supporting line "Found in <sources>", where
  `<sources>` is the index's comma-separated list of the Google artifacts that carry the pair, shown
  as the artifact names the index gives (`ads-mobile-sdk`, `user-messaging-platform`, …) and joined
  with a comma and a space. Rows that share a name (Kotlin four times, J2ObjC five, Cronet two) stay
  separate rows in the index's order; each opens its own text, and the sources line is what tells them
  apart. — The names are Google's own component names, and the artifact names are Google's own
  artifact ids; a table mapping either to friendlier words would be a second curated list that drifts
  with every SDK bump, which is the staleness LN-1 rejected. The screen is a notices display, not a
  catalogue page, and it says so in its intro (§6).
  *(Loop 1, 2026-10-24, F-08 — "the sources line is what tells them apart" is **superseded**: two of
  one name's texts can be carried by the same AARs, and then two rows would read the same by eye and
  by ear while opening different texts. A UX decision: **when two or more index lines share a name,
  each of those rows carries a third line, `google_notices_ordinal` "Notice %1$d of %2$d"**, numbered
  in the index's order and built once in the ViewModel's row model; a name that occurs once carries no
  ordinal. The ordinal is read as part of the row's node (§5.1) and shown again under the sources line
  on that notice's text screen (§4B). LS-AC13's fixture includes two same-name rows with identical
  sources, the hard case. The rest of LS-12 stands.)*
- **LS-13: Every one of the four screens is fully usable with TalkBack, and §5 is its spoken
  contract** *(2026-10-24; rules/compose.md; the founder's Q-023 answer — option 1, 2026-10-21,
  `.planning/questions/FOUNDER.md` — which this role reads as "the studio ships nothing that TalkBack
  cannot use", from the question's own premise that every app meets its platform's accessibility
  rules before it ships; the D-TB and D-TBa contract, `docs/ux-spec.md` §11.1, as the house shape.
  Loop 1, F-07: both are on this branch since `develop` 4069b53 and are cited, not restated.)*
  Fully usable means: every entry and every notice row is one stop with a role and a click label;
  every inert entry is readable and announced as text, never as a control; every heading is a heading;
  every paragraph of every text is a stop; a loading or error state announces itself once, politely;
  focus after a navigation lands in the new screen's reading order; back always returns. The rule
  these screens add to the app: **nothing is conveyed by the chevron, the squish or the sound alone**
  — the role and the click label carry the same distinction by ear (LS-06).
- **LS-14: A source address is read with its card, and in 3.0.0 it cannot be copied; copying
  returns later as one long-press action, the same by eye and by ear** *(address ruling,
  2026-10-24: the trade-off that the developer's fix `2d433b8` for the qa-engineer's F-1 left to
  this role.)* **3.0.0 ships the five addresses (Kenney, LilMati, CogFireStudios, the Public Suffix
  List and the SDK terms) as plain text inside each card's one merged node. The loss of copying them
  is accepted for this release. Copying is deferred, not dropped.** Four reasons make it
  acceptable now:
  1. Nothing the app owes rests on copying. MPL-2.0 §3.2(a) asks the app to *inform* recipients how
     to obtain the source, and the shown exact-version address and its date do that.
     `research-analyst-mpl.md` verdict 2 asked that the entry *name* the version, which it does. The
     CC0 credits and the SDK terms address are courtesies.
  2. Every licence and notice **text** stays selectable on its reader (LS-08; QA passed §8.2 item 8,
     copy included). So the two addresses inside the Public Suffix List's NOTICE, the live list and
     `mozilla.org/MPL/2.0/`, can still be copied there.
  3. The fix leaves eye and ear equal: neither can copy. On its own, the developer's suggested "Copy
     address" accessibility action would give the ear an action the eye lacks, which reverses LS-13's
     rule. It is **not** the replacement.
  4. A new interaction now would reopen the build, the audit and the device session on the branch
     that blocks the release cut (§9 item 5), for a convenience few players use.
  The real cost is the Public Suffix List's address: 97 characters around a 40-character commit
  hash, which nobody types by hand. That cost is why copying is deferred and not dropped. A second,
  smaller cost is by ear. The address is now read at the end of its card's announcement, so on the
  Public Suffix List card TalkBack reads the hash before "Button". That is accepted: the address is
  the information the entry exists to give, it comes after the title, credit, licence and parts, and
  a swipe interrupts it.
  **The follow-up's shape is fixed now, so it needs no UX round when it is scheduled** (§9 item 15):
  - **Copying.** On each of the five cards, **a long-press anywhere on the card copies that card's
    address**, exactly as shown, to the clipboard. The same action is **the card's long-click
    semantics action, labelled `licenses_copy_address_action` "Copy address"**, on its one merged
    node. TalkBack then says "double-tap and hold to copy address" and lists the action in its
    actions menu.
  - **What stays the same.** The card stays one stop. An inert card gains no click action, no role,
    no chevron and no squish, so LS-06 and pass line 3 stand: "button" and "double-tap to activate"
    are still never said. The Public Suffix List card keeps its tap and adds the long-press.
  - **Feedback.** The platform's long-press haptic, and no sound. On Android 13 and later, the
    system shows its own clipboard confirmation and the app shows nothing, because two
    confirmations would double up. On Android 12L and earlier, the app shows
    `licenses_address_copied` "Address copied", announced politely.
  - **Why a long-press, not restored selection.** A long-press copies the whole address in one
    gesture. Selection handles across an address that wraps over several lines at 200 % are fiddly.
    And a long-press is the platform's own way into copying text.
  - **Tests.** Each of the five cards has `OnLongClick` with that label, and invoking it puts
    exactly the shown address on a fake clipboard. No other card has it. The inert cards still have
    no `OnClick` and no `Role`. QA long-presses, uses TalkBack's actions menu, and pastes.
  - **Mechanism.** The clipboard seam, and how the long-press composes with the card's merge and
    squish, are the tech-lead's.
- **LS-15: Heading navigation reaches every heading, in both directions, from anywhere on the
  screen, and a lazy list is no excuse** *(device re-run ruling, 2026-10-24: the contract half of
  the qa-engineer's F-4. The mechanism is the tech-lead's, ruled in parallel.)* **What the player
  must experience, in 3.0.0:** with TalkBack's reading control on Headings, from any stop and at
  any scroll position, a swipe down moves to the next heading §5.1's table marks and a swipe up to
  the previous one. Each is scrolled into view and read whole. From the top, every heading is
  reached in the table's order; from the bottom, every one in reverse. Nothing that is not a
  heading is a stop between two headings. On the Licenses screen the headings are the title and
  the four section headers (Fonts, Sounds, Open-source software, Google libraries), whether or not
  they are on screen.
  The re-run heard otherwise on the Licenses list (`tb-licenses-headings-*.txt`):
  - up from the bottom, "Google libraries" and then the title, with Open-source software, Sounds
    and Fonts skipped;
  - from the middle of the list, nowhere to go;
  - going down, a stray stop before "Google libraries" on a card half under the top bar, read
    without its title.
  Three reasons make this required now, not a named limit:
  1. **A skipped heading misinforms.** A player who swipes up from "Google libraries" and lands on
     the title has been told the screen has two sections. Heading navigation is how a TalkBack
     player learns a page's shape, so headings that hide three sections are worse than none.
  2. **This is the one screen of the four with sections, and the screen before it passes.**
     Settings, a plain column, passed the same check in the first session, and its section headers
     and these are the same component (§2, the brief's A1.4). Two adjacent screens must not answer
     the same gesture differently: the principle this role accepted with the brief's A1.1 for the
     inert cards.
  3. **The cost is small.** The list is 23 static items with no data source and no loading
     (LS-09), so nothing argues for composing only what is on screen. This role does not rule the
     mechanism. It notes only that Settings meets the property as built.
  **Named as the platform's, and accepted:** what TalkBack does on a swipe past the last heading
  or before the first. That may be an end sound, nothing, or a scroll that lands on an entry or a
  paragraph (the two paragraphs after "Apache License 2.0" in `tb-jakarta-headings-down.txt`). The
  product promises the headings and the stretch between them; the ends belong to TalkBack.
  **Not accepted as the platform's:** a card read without its title, or as a bare "Button" (the
  re-run's O-1, seen only after heading navigation's scroll fallback). In any navigation that is a
  fail of pass line 2, or of pass line 3 for an inert card. This role expects it to go with the
  F-4 fix, and QA's heading runs look for it (§8.2 item 2).
  The rule binds all four screens, so a screen that later puts headings inside a lazy list
  inherits it. Today only the Licenses list has headings inside a list. The notices list and the
  notice text have one heading each, the top-bar title, which is always on screen. The License
  text body already holds all its headings, and Jakarta passed in both directions. If the
  tech-lead's ruling finds the experience cannot be delivered in 3.0.0, the question comes back to
  this role through the producer; the build does not downgrade it on its own.
  *(LS-15 ruling, 2026-11-01: **restated for a scrolling list.** The text above is kept as the
  record. It came back to this role as foreseen, through the tech-lead's R1
  (`tech-lead-f4-ruling.md`) after QA's re-check (`qa-engineer-recheck.md`, F-4).)*
  **What changed in the facts.** The text above rested on two premises, and both were wrong:
  - **"The headings are in the tree, so TalkBack can reach them."** Compose gives TalkBack only the
    nodes that are on screen, verified in the pinned Compose source (R1, Layer 1). TalkBack reaches
    a heading that is off screen only through its own page scroll, and TalkBack chooses where focus
    lands after that scroll. The top-bar title is a heading that is always on screen, so going up,
    whenever no section header above is on screen, it is the nearest heading TalkBack can see and
    TalkBack does not scroll back. No change to this screen's composables
    controls either behaviour.
  - **"Settings meets the property as built."** Settings passes only because it nearly fits on one
    screen. A Settings that scrolls is expected to behave the same way. So the "two adjacent
    screens must not answer the same gesture differently" reason now points the other way: the
    restated rule below is what both screens give.
  - Reason 3, "the cost is small", also falls. The only design that meets the text above is a
    redesign, option E below.
  **The contract for 3.0.0.** With TalkBack's reading control on Headings, on the Licenses screen:
  1. **Going down, every heading is reached in order.** From the back button, swipes down reach
     the title, Fonts, Sounds, Open-source software and Google libraries in that order, none
     skipped and none repeated. The same holds from an entry in the middle of the list: every
     heading below it is reached in order.
  2. **One kind of stop between two headings is accepted going down:** a stop that TalkBack makes
     after its own page scroll, when no heading is on screen. That stop must be a **whole card,
     title first**: pass line 2, or pass line 3 for an inert card. A card read without its title,
     or as a bare "Button", still fails. At most one such stop follows each TalkBack page scroll;
     a non-heading stop with no scroll before it is a finding, because TalkBack takes the next
     heading once it is on screen. (At 100 % one scroll, and so one such stop, was seen between
     Open-source software and Google libraries; at 200 % there may be more.)
  3. **Going up, and from the title with the list scrolled, the behaviour is the platform's.**
     QA records it and does not grade it. Today that is "Google libraries" then the title going
     up, and the title only from the title. Any card TalkBack stops on in these runs is still
     graded by pass line 2.
  4. **The list composes all its items** (the non-lazy `Column`, R1 option A). This is kept. It is
     necessary for point 1, and the Compose tests of LS-AC06 guard it. It is no longer claimed to
     be sufficient.
  5. **Past the last heading and before the first** stays the platform's, as above.
  **Why this is acceptable for the player.**
  - The screen always opens at the top, and its reading order starts there. Learning the page's
    shape is a pass downward, and that pass now names all five headings in order.
  - A player in the middle of the list who swipes up hears the title. That under-reports the
    sections, which is reason 1's harm. By R1's analysis it is what TalkBack does on any Compose
    list that scrolls under a pinned heading title, so it is the platform's behaviour rather than
    this screen's, and the way out is the platform's too: swipe down, or scroll to the top.
  - Linear swipes and TalkBack's scroll actions still reach every entry. Nothing is unreachable.
  **Options this role rejects for 3.0.0** (R1's table; the rejected B and C are the tech-lead's and
  stand):
  - **E, a pinned section index** (four visible controls that scroll the list, so all five headings
    stay on screen). It is the only design that meets the text above. It is rejected because it
    serves a gesture by adding chrome for everyone:
    - a second navigation row on a 23-item screen, and four more stops before the list;
    - a pattern that no other screen in the product has, and that Android does not use for a
      list of this length;
    - and a new design-and-build loop in the week 3.0.0 is meant to ship.
    It is not an art-direction choice between equals: the restated contract is the platform's
    own, and E costs every player screen space to change TalkBack's search. No founder question is
    drafted. E is recorded here as the shape to reach for if this screen ever grows past about
    eight sections.
  - **D, the title scrolls with the list.** It changes the top bar that every screen of the
    product shares, and going up TalkBack would still land through its own scroll, so it is
    unproven. Rejected.
  The four-screen scope is unchanged. The other three screens have one heading each, or hold all
  their headings (Jakarta passed both ways). A later screen with headings inside a scrolling list
  inherits this restated contract, not the text above.
- **LS-16: Every text reads in its own direction; a list's lines hang from the mirrored start
  edge, a text screen's body from its own left edge** *(device re-run ruling, 2026-10-24, on the
  qa-engineer's F-2. It confirms the intent of §4's "Direction" and of §5.1's "the texts stay left
  to right by content", and makes it a rule of these screens instead of an assumed default.)* The
  re-run found the premise wrong: in Compose, a text that names no direction takes the layout's.
  An RTL-only language list (`[ar-XB]` on the test phone) resolves the app to its English
  resources and lays it out right to left. Every studio string and every licence paragraph then
  became a right-to-left paragraph, and the characters at its ends moved: ".partnership with
  others", "The Fredoka Project Authors 2016 ©", "sound effects 7", "Pop 01” by LilMati“", and an
  OFL dash rule broken around "SIL" (`s23`–`s26`). That misrepresents texts that LS-07 shows
  verbatim. **What an RTL-only player sees on all four screens:**
  - **The chrome mirrors**, as in every app on that phone: the back arrow at the right pointing
    right, the top-bar title beside it, each card's chevron at its left edge pointing left, the
    padding, and the order of a row's parts. The re-run found this right, and it stays.
  - **Every text reads in its own direction**, taken from its first strong character and never
    from the layout. That covers the studio's strings, the intro, the section headers, the card
    lines, the "Found in" and ordinal lines, "End of text", the error texts, and every paragraph
    of every licence and notice. English is a left-to-right paragraph in every locale. Each line
    reads left to right, and every character stays where the string or the asset puts it: the
    full stop at the end, "© 2016 The Fredoka Project Authors", "7 sound effects", "“Pop 01” by
    LilMati", a URL in brackets, the OFL's dash rules whole.
  - **Where the lines hang follows the surface.**
    - On the two **lists** (Licenses, Google notices), the intro, the section headers and every
      line of every card hang from the **start edge of the mirrored layout**. That is the right
      edge in an RTL locale, where they hang today. A card keeps its mirrored shape, lines at the
      right and chevron at the left, and every line of one card shares that edge, wrapped lines
      included.
    - On the two **text screens** (License text, Google notice text), the body is an English
      document shown as supplied. The entry title and credit, the part headings, the "Found in"
      and ordinal lines, every paragraph and "End of text" hang from the **left edge** of the
      content column, ragged on the right, in every locale. The top bar is chrome and mirrors.
  - **By ear nothing changes.** The strings carry no direction marks (QA's dump), and TalkBack
    reads them in logical order.
  **Why two edges, not one.** A list row is part of the mirrored interface: an RTL reader scans
  its start edge, which is where the house's other mirrored text already sits (`docs/ux-spec.md`,
  D-04f's Undecided notice: "the text is start-aligned, so the notice mirrors with the layout").
  Lines pushed against the chevron at the far edge would break the row. A licence is a document:
  an English paragraph is read from its left edge, and a right-aligned page of English (`s24`) is
  hard to follow from line to line. Considered and rejected:
  - the right edge everywhere, which leaves the legal texts ragged on the left;
  - the left edge everywhere, which pushes each card's lines against its chevron, unlike every
    mirrored row on the phone.
  The rule still fits in one line: lists hang from the layout's start, documents from the text's
  start.
  **Scope.** QA notes that the cause would apply app-wide. This decision binds these four
  screens, the only ones whose contract names direction; the rest of the app is §9 item 19. The
  mechanism is the tech-lead's. LS-AC19 asserts the direction, and §8.2 item 6 checks the order
  and both edges by eye.

## 2. Entry point — Settings

| Element | Spec |
|---|---|
| Section header | `settings_section_about` "About" — `SettingsSectionHeader` (`labelLarge`, `onSurfaceVariant`), `semantics { heading() }` |
| Row | a `SettingsLinkRow`: label `settings_licenses_label` "Licenses", supporting `settings_licenses_supporting` "Fonts, sounds and open-source software", **no trailing icon** (it opens in-app — the same rule that leaves the manage-choices row bare and gives the browser-bound policy row its glyph), `onClick` → Licenses |
| Sound | `withClickSound(SoundCue.ButtonForward, …)` at the call site — a navigational CTA (D-09), exactly like "How to play" |
| Position | after the Play Games section (or after General when Play Games is absent, D-11) and before Privacy |
| Entrance orders | renumbered top to bottom, contiguous: `ABOUT_HEADER_ORDER` 8, `LICENSES_ROW_ORDER` 9, then `PRIVACY_HEADER_ORDER` 10, `PRIVACY_POLICY_ROW_ORDER` 11, `PRIVACY_CHOICES_ROW_ORDER` 12 — a whole renumbering, so `SettingsScreenTest`'s rendered-top check passes |

*(2026-10-24.)* The row and its orders stand. Two notes from the branch split (`tech-lead-direction-b.md`
A9): `feature/colour-vision` also inserts a Settings row and renumbers; whichever of the two branches
lands second renumbers on top of the first, and the numbers above are this branch's own, not a contract
with that one. The supporting line "Fonts, sounds and open-source software" still describes the whole
screen: the Google notices are open-source software, reached through the Google libraries section (§3.2).

*(Loop 1, 2026-10-24, F-09.)* The table's "`SettingsSectionHeader` … `semantics { heading() }`"
describes the intent, not today's component: `SettingsScreen.kt` carries no `heading()` on any section
header, and its own KDoc says the headers go together ("both headers or neither"). **The UX intent is
that all four Settings section headers — General, Play Games, About, Privacy — are headings**, as
`docs/ux-spec.md` §11 already asks of titles, so a TalkBack player jumps section to section on
Settings as on every other screen. That is one change to `SettingsSectionHeader`, never to the About
header alone. The mechanism and its place in the build are **owed to the tech-lead's brief** (§9 item
7); LS-AC01's Compose pass line gains "the four section headers are headings".
*(Loop 2, 2026-10-24, F-19 and the brief's A1.4: answered — `SettingsSectionHeader` gains
`.semantics { heading() }` in `SettingsRows.kt`, commit 10a, so every section header it renders is a
heading, on Settings and on the Licenses screen alike. "The four" was a count, and a count is the
wrong oracle: §2's own D-11 clause makes it three headers on a device without Play Games, and
`docs/ux-spec.md` §11 asks every title to be a heading too, so once the Settings title is one the
stops number five. The oracle is **per header**: each section header shown carries the heading
property (the brief's `everySectionHeaderIsAHeading`, three headings before About lands, four after);
QA's line reads "heading navigation stops on every section header shown, and on the title where it
is one" (LS-AC01, §8.2 item 11).)*

## 3. The Licenses screen

### 3.1 Layout

- A transparent `Scaffold` and a transparent M3 `TopAppBar` over the Sparse backdrop: back
  (`Icons.AutoMirrored.Rounded.ArrowBack`, `navigate_back`, 48dp, `claySquish`, silent) and the title
  `licenses_title` "Licenses" (`titleLarge`).
- A `LazyColumn` capped at `content_max_width` and centred, padding `spacing_md`, items spaced
  `spacing_md`, entering with `clayEntrance` in order top to bottom.
- First item: the intro `licenses_intro` (`bodyMedium`, `onSurfaceVariant`).
- Then four sections, each a `SettingsSectionHeader`-style header (`labelLarge`, `onSurfaceVariant`,
  heading semantics) over its entries.

**An entry** is a clay card, the exact peer of a Settings row: `surfaceContainerHigh`, `shapes.large`,
`clayShadow(elevation_card)`, `heightIn(min = settings_row_min_height)`, `spacing_md` padding. Inside:
the title (`titleMedium`, `onSurface`), then one to three lines (`bodyMedium`, `onSurfaceVariant`) —
credit, licence name, and the parts or source address — with no `maxLines` anywhere. A tappable entry
adds a trailing `Icons.AutoMirrored.Rounded.KeyboardArrowRight` (`settings_row_icon_size`,
`onSurfaceVariant`, decorative) and `claySquish()` + `clickable` with `ButtonForward`; an inert entry has
neither. Source addresses sit in a `SelectionContainer` so they can be copied.

*(Address ruling, 2026-10-24, LS-14: "Source addresses sit in a `SelectionContainer` so they can be
copied" is **superseded**. An address is a plain `Text` line of its card, like the credit and
licence lines, and is read inside the card's one merged node. Nothing on a card is selectable. QA's
F-1 found that the container made each address a TalkBack stop of its own, and `2d433b8` removes
it. Copying is deferred, with its shape fixed, in LS-14.)*

*(2026-10-24.)* The layout stands. Two additions: the list is keyed by entry id with
`rememberLazyListState()`, so the position survives the round trip to a text (R4; LS-AC02); and the
section count stays four — the Google notices are reached through an entry in the fourth section, not
through a fifth section, so the screen's shape does not change as Google's libraries change.

*(Loop 1, 2026-10-24, F-02 — the merge.)* **Every entry card merges its own descendants:
`Modifier.semantics(mergeDescendants = true) {}` on the card, with no role and no action.** A
tappable entry then adds `claySquish()` + `clickable` (role Button, the click label); an inert entry
adds nothing else. The merge is a property of the card, not of the click, so the four inert entries
(the three sounds, the SDK entry) are one TalkBack stop each, read title first, exactly as the
tappable ones are. The same holds for every row of the Google notices list (§4A), all of which are
tappable. `SettingsLinkRow` with `onClick = null` has no merge today (the check's locator); whether
the entry card reuses that component and so fixes it for its other callers, or is its own composable,
is the tech-lead's brief (§9 item 7). LS-AC06 asserts the result: one node per inert entry, no
`OnClick`, no `Role`.

*(Loop 2, 2026-10-24, F-23 — the entrance stagger, a UX number.)* "Entering with `clayEntrance` in
order top to bottom" is widened: **the stagger runs over the first `LicensesUi.STAGGERED_ROWS` = 8
items of the list — the intro, the headers and the entries counted alike — and every later item
enters plainly.** The same cap governs the Google notices list (§4A). The reason is the one the
brief's F6 gives for its default and this role confirms as the design: a card is ≥ 72dp + 16dp, so a
phone shows six to nine rows, and 8 × 40 ms after the 240 ms entrance has the first screenful in by
≈ 560 ms; staggering twenty-three items (the Licenses list: an intro, four headers, eighteen entries)
would animate rows nobody can see for another half-second. §4A's second-edition sentence "as it does
for every long list in the app" is **withdrawn**: no list in the app caps its stagger today (the
Classic catalogue has five cards), and this is the first that needs to. With animations off
(`rememberAnimationsEnabled()` false) nothing staggers, as before.

### 3.2 What it lists

Contents follow THIRD_PARTY.md **as it stands on the release branch**; the rows below are its 3.0.0
rows today. Where THIRD_PARTY.md changes, this list changes with it (LS-AC03).

*(Second edition, 2026-10-24 — the table below replaces the first edition's. Rows that stand are
unchanged; the rows it replaced are quoted beneath it. Sources: THIRD_PARTY.md §2 (sounds, fonts, the
Apache-2.0 text and the Jakarta NOTICE), §4, §5b rows A–J and the Apache table; tech design §13.1 LN-5.
Entry titles, credit lines, licence names and parts lines are the strings §6 names, one key each.)*

| Section | Entry title | Lines (credit · licence · parts or address) | Opens (§7) |
|---|---|---|---|
| **Fonts** (`licenses_section_fonts`) | Fredoka | "© 2016 The Fredoka Project Authors" · "SIL Open Font License 1.1" | `Fredoka-OFL.txt` |
| | Nunito | "© 2014 The Nunito Project Authors" · "SIL Open Font License 1.1" | `Nunito-OFL.txt` |
| **Sounds** (`licenses_section_sounds`) | Interface Sounds by Kenney | "7 sound effects" · "CC0 1.0 Universal (public domain)" · "kenney.nl/assets/interface-sounds" | — (inert) |
| | “Pop 01” by LilMati | "2 sound effects" · "CC0 1.0 Universal (public domain)" · "freesound.org/people/LilMati/sounds/506546" | — |
| | “Positive Blip Effect” by CogFireStudios | "1 sound effect" · "CC0 1.0 Universal (public domain)" · "freesound.org/people/CogFireStudios/sounds/531512" | — |
| **Open-source software** (`licenses_section_software`) | AndroidX and Jetpack Compose | "The Android Open Source Project" · "Apache License 2.0" · "Compose, Material 3, Material icons, Room, DataStore, Navigation, Lifecycle, Activity, Core, Hilt Navigation" | `Apache-2.0.txt` |
| | Kotlin and kotlinx | "JetBrains" · "Apache License 2.0" · "Standard library, coroutines, serialization" | `Apache-2.0.txt` |
| | Dagger and Hilt | "Google" · "Apache License 2.0" | `Apache-2.0.txt` |
| | OkHttp and Okio | "Square" · "Apache License 2.0" · "Networking and I/O inside Google Mobile Ads" | `Apache-2.0.txt` |
| | Gson, Tink and Guava | "Google" · "Apache License 2.0" · "Gson, Tink, Guava, Error Prone annotations, J2ObjC annotations" | `Apache-2.0.txt` |
| | Jakarta Dependency Injection | "Eclipse Foundation" (loop 1, F-16: the credit's words are taken from the bundled `NOTICE.md`'s own attribution; the developer's report quotes the line, and the string follows the NOTICE if it names the project otherwise) → **"The Eclipse Jakarta Dependency Injection project"** (address ruling, 2026-10-24: **confirmed**, `licenses_jakarta_credit`. These are the words of the NOTICE's attribution line, "This content is produced and maintained by the Eclipse Jakarta Dependency Injection project.", and the NOTICE names the Eclipse Foundation only as the trademark holder, so "Eclipse Foundation" is superseded) · "Apache License 2.0" | `Jakarta-Inject-NOTICE.md`, then `Apache-2.0.txt` (LS-10) |
| | Annotation and injection APIs | "The JSR-330 Expert Group, the FindBugs project, JSpecify, JetBrains" · "Apache License 2.0" · "javax.inject, JSR-305 annotations, JSpecify, JetBrains Java annotations" | `Apache-2.0.txt` |
| | Protocol Buffers | "Google" · "BSD 3-Clause License" · "The Java Lite runtime, and the copy inside AndroidX DataStore" | `Protobuf-BSD-3-Clause.txt` |
| | Chromium (Cronet) | "The Chromium Authors" · "BSD 3-Clause License" · "The Cronet networking classes inside Google Mobile Ads" | `Chromium-BSD-3-Clause.txt` |
| | Public Suffix List | "Mozilla and the list's contributors" · "Mozilla Public License 2.0" · "The public-suffix data compiled into OkHttp, as it stood on 2023-02-15" · "github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat" (the source address of the exact version shipped, selectable, LS-05 — address ruling, 2026-10-24: plain text inside the card's one stop, not selectable, LS-14; loop 1, from `research-analyst-mpl.md` §2 — the live list's address is in the NOTICE the entry opens) | `PublicSuffixList-NOTICE.txt`, then `MPL-2.0.txt` (LS-10, LS-11) |
| | Checker Framework qualifiers | "The Checker Framework developers" · "MIT License" | `CheckerQual-MIT.txt` |
| **Google libraries** (`licenses_section_google`) | Google Play services and SDKs | "Play Games Services, Mobile Ads, User Messaging Platform, Play inline-install library, Play services libraries" · "Android Software Development Kit License Agreement" · "developer.android.com/studio/terms" | — (inert) |
| | Open-source notices in Google libraries | "The notices Google supplies for the open-source software inside its libraries" | the **Google notices** screen (§4A), tappable, click label `licenses_google_notices_action` |

**Order inside Open-source software** is the table's: the three first-edition entries, the four
Apache-2.0 groups, then the three BSD/MPL entries and the MIT one. It is not alphabetical, because the
libraries a player has heard of should come first; the LS-AC03 test pins this order.

**The two Google entries** are the LN-5 item 2 shape: the inert SDK-licence entry (its terms address
as selectable text, LS-05; since the address ruling, 2026-10-24, plain text inside the card's one
stop, LS-14) and, under it, the one tappable entry that opens the generated notices list.
Every (name, text) pair in the index is two taps from the Licenses screen: this entry, then a row.

*The first edition's rows this table replaces (2026-10-17), kept as the record:*

> | | *one entry per further §5b family the release branch's `releaseRuntimeClasspath` listing confirms* (THIRD_PARTY.md §0, checklist step 6) — e.g. small annotation libraries; an MIT one bundles its own notice as `assets/licenses/<name>-MIT.txt` and opens it | as the row | its text |
> | **Google libraries** (`licenses_section_google`) | Google Play services libraries | "Play Games Services, Mobile Ads, User Messaging Platform" · "Android Software Development Kit License Agreement" · "developer.android.com/studio/terms" | — (inert) |

The placeholder is resolved by the research-analyst's 192-of-192 sweep (2026-10-21): the families are
the eight new Open-source software entries above, and the one MIT family is Checker Framework. The
Google entry is renamed and its parts line now names all five groups of Google code on the release
graph (U-1 closed 2026-10-21; `hsdp` row A).

#### 3.2.1 The mapping to THIRD_PARTY.md (LS-AC03's expected list)

One line per THIRD_PARTY.md row or family that ships with a licence or a credit, and the entry that
shows it. The LS-AC03 JVM test writes this table into the expected catalogue; the release-manager
diffs it against THIRD_PARTY.md at checklist items 1.4 and 6.

| THIRD_PARTY.md | Entry |
|---|---|
| §2 `sfx_jump`, `sfx_peg_remove`, `sfx_undo`, `sfx_win`, `sfx_btn_forward`, `sfx_btn_dismiss`, `sfx_sound_enabled` (Kenney, CC0) | Interface Sounds by Kenney — count 7 |
| §2 `sfx_select`, `sfx_deselect` (LilMati 506546 and its derivative, CC0) | “Pop 01” by LilMati — count 2 |
| §2 `sfx_star_pop` (CogFireStudios 531512, CC0) | “Positive Blip Effect” by CogFireStudios — count 1 |
| §2 `sfx_invalid`, `sfx_hint` (own work, recipes) | not listed (LS-04) |
| §2 Fredoka v17, Nunito v32 (OFL-1.1) | Fredoka; Nunito |
| §2 `assets/licenses/Apache-2.0.txt` (the text) | not an entry: the text behind every "Apache License 2.0" entry |
| §2 `Jakarta-Inject-NOTICE.md` (named in the Apache-2.0.txt row) | Jakarta Dependency Injection |
| §2 launcher and cover art, opening books; §7 store art | not listed (own work) |
| §4 the `androidx.*` coordinates (fourteen today; fifteen once check-gates C7 declares `hilt-lifecycle-viewmodel-compose`, tech design §13 — loop 1, F-10); §5b `androidx.*` family | AndroidX and Jetpack Compose |
| §4 `kotlinx-serialization-json`, `kotlinx-coroutines-play-services`; §5b `org.jetbrains.kotlin:*`, `org.jetbrains.kotlinx:*` | Kotlin and kotlinx |
| §4 `hilt-android`; §5b `com.google.dagger:*` | Dagger and Hilt |
| §4 `play-services-games-v2`, `ads-mobile-sdk`, `user-messaging-platform`; §5b row A `hsdp`, row E the `play-services-*` siblings | Google Play services and SDKs (inert); their bundled notices → Open-source notices in Google libraries |
| §5b Apache table `okhttp`, `okio`, `okio-jvm` | OkHttp and Okio |
| §5b Apache table `gson`, `tink-android`, `guava`, `failureaccess`, `listenablefuture`, `error_prone_annotations`, `j2objc-annotations` | Gson, Tink and Guava |
| §5b Apache table `jakarta.inject-api` | Jakarta Dependency Injection |
| §5b Apache table `javax.inject`, `jsr305`, `jspecify`, `org.jetbrains:annotations` | Annotation and injection APIs |
| §5b rows F and G (`protobuf-javalite`; `datastore-preferences-external-protobuf`) | Protocol Buffers |
| §5b row H (the five `org.chromium.net` artifacts) | Chromium (Cronet) |
| §5b row J (`publicsuffixes.gz` and its `NOTICE` inside `okhttp`) | Public Suffix List |
| §5b row I (`checker-qual`) | Checker Framework qualifiers |
| §1 idea rows; §3 (none); §8 not-shipped items | not listed (LS-04) |

**The Kenney entry** credits "Kenney" in words only — never Kenney's logo (their terms). The sound counts
are THIRD_PARTY.md §2's: Kenney seven (`sfx_jump`, `sfx_peg_remove`, `sfx_undo`, `sfx_win`,
`sfx_btn_forward`, `sfx_btn_dismiss`, `sfx_sound_enabled`), LilMati two (`sfx_select` and its derivative
`sfx_deselect`), CogFireStudios one (`sfx_star_pop`). Those rows describe the WAV set on
`feature/audio-review`; if that branch does not reach the release, the Sounds section is re-cut with
THIRD_PARTY.md.
*(2026-10-24: confirmed against `docs/audio/CREDITS.md` on `feature/audio-review` (its `## Files`
table: seven Kenney, two from sound 506546, one from sound 531512, two in-house) and against
THIRD_PARTY.md §2 as merged to `develop`, whose twelve rows are those files. `feature/audio-review` adds
no THIRD_PARTY.md row of its own — the rows are already on this branch. The re-cut rule stands: the
counts are the `licenses_sound_effects` plural's arguments, so a re-cut is a number change. If the
audio branch misses the release, `develop`'s `.ogg` set has nine Kenney cues, and this section then
reads nine, two and one with THIRD_PARTY.md. Loop 1, F-17: the WAV table is on `feature/audio-review`
only until that branch merges; `docs/audio/CREDITS.md` on this branch is still the `.ogg` record, nine
Kenney cues with `sfx_invalid` and `sfx_hint` as Kenney's `bong_001` and `pluck_002`. The
release-manager's re-cut check at the cut reads the CREDITS.md of the tree that ships, not this
branch's copy.)*

**The User Messaging Platform** is named in the Google entry only once THIRD_PARTY.md's U-1 has
settled its licence; until then the entry lists Play Games Services and Mobile Ads.
*(Superseded 2026-10-21 by the research-analyst's row B: U-1 is closed, the POM declares the Android
SDK licence, and the entry names it. Recorded here 2026-10-24.)*

**Never listed** (LS-04): every THIRD_PARTY.md §1 idea row — the traditional game, the board
geometries, the founder's earlier repository, and the candy-backdrop row, whose Credit cell excludes it
from the app; own work (the two in-house sounds, the launcher and cover art, the opening books, every
drawing, the backdrop); `font_certs.xml` and `ui-text-google-fonts` (they do not ship in 3.0.0); every
test-, debug-, compile- and build-only item (§8). The words King, Candy Crush and candy appear nowhere
on either screen.
*(2026-10-24: "on either screen" reads "on any of the four screens" for everything the studio writes
— every string in §6 and every curated text in §7, which BD-01's JVM test scans. The generated Google
notices are Google's own texts, reproduced verbatim and never edited, so the words rule does not reach
them; the developer's report records the result of the same grep over the generated directory, for
information, and the code-auditor reads it. Nothing in the app names a notice's content.)*
*(Loop 2, 2026-10-24, F-22: "BD-01's JVM test" named a test that exists on no branch yet — it was
candy-backdrop's step 1, and this branch lands first (A9; merge order 6a). **This branch writes
`UserFacingTextHasNoProtectedNamesTest`** (the brief's §4.3, commit 3): it scans
`src/main/res/values*/strings.xml` and `src/main/assets/**` (files only, recursive) for whole words,
case-insensitively — the verdict's twelve (`candy`, `candies`, `crush`, `saga`, `candyland`, `tiffi`,
`toffee`, `yeti`, `odus`, `king`, `activision`, `microsoft`) and A.4's four (`jelly`, `soda`,
`sugar`, `sweet`). The word list is a constant that the candy branch owns and may widen once it
merges `develop` in and finds the test already there. LS-AC17 names this branch as the author.)*

### 3.3 States

The list is static: no loading, empty or error state exists for it.
*(2026-10-24: stands for this screen (LS-09). The Google notices screens have states, §4A and §4B.)*

## 4. The License text screen

- **Top bar:** transparent; back (as §3.1); title = the opened entry's licence, e.g. "SIL Open Font
  License 1.1" for Fredoka and Nunito, "Apache License 2.0" for the software entries
  (`titleLarge`, one line allowed to wrap — no `maxLines`).
- **Body:** a `LazyColumn` capped at `content_max_width`, centred, padding `spacing_md`, items spaced
  `spacing_sm`: first the entry's title and credit line (`titleMedium`, `onSurface`; heading semantics),
  then the licence text one **paragraph per item** (`bodyMedium`, `onSurface`, directly on the Sparse
  backdrop, which is zone T: ≥ 10:1 light, ≥ 14:1 dark), the whole column inside a `SelectionContainer`.
  Paragraph per item keeps a 11 KB text fast and lets TalkBack move one paragraph at a time.
- **Re-flow (LS-07):** within a paragraph, line breaks are joined with a single space; blank lines
  separate paragraphs; the characters otherwise shown are exactly the asset's. The asset file itself is
  untouched (OFL §2's notice condition is met by the file, not by this view).
- **Loading:** the asset is read off the main thread when the screen opens. Nothing is shown for the
  first 300 ms; if the text is not in by then, a centred `CircularProgressIndicator` (`primary`) with
  description `licenses_loading` "Loading license" appears until it is.
- **Error:** if the asset cannot be read, the body shows `licenses_text_error` "This license text
  could not be shown." (`bodyMedium`, `onSurfaceVariant`) under the entry's title, credit and licence
  name. A test guarantees each asset exists, so this state should never ship to a player.
- **Direction:** the licence texts are English; the default content-based text direction keeps them
  left to right inside an RTL layout, while the chrome mirrors.
  *(Device re-run ruling, 2026-10-24, F-2: the intent is **confirmed**, and "the default
  content-based text direction" is **superseded**. In Compose a text that names no direction takes
  the layout's, and the re-run saw every text on this screen laid out right to left under
  `[ar-XB]`, its end punctuation moved (`s24`). The rule is now a decision, **LS-16**: every text
  reads in its own direction, and this screen's body, an English document, hangs from the
  column's left edge in every locale, while the chrome mirrors.)*

*(Second edition, 2026-10-24 — the body, the title and the multi-text entries. The bullets above stand
except where a line below says it is superseded.)*

- **Body, superseded by LS-08 (D10).** "A `LazyColumn` … the whole column inside a `SelectionContainer`"
  is replaced: the body is **one `Column` in `verticalScroll(rememberScrollState())`, the whole of it
  inside one `SelectionContainer`**, still one `Text` per paragraph (`bodyMedium`, `onSurface`, zone T),
  still capped at `content_max_width` and centred, padding `spacing_md`, paragraphs spaced
  `spacing_sm`. The scroll state is saveable. "Paragraph per item keeps a 11 KB text fast" is withdrawn
  as the reason; the reason for one node per paragraph is TalkBack (§5), and it holds in a `Column` too.
- **An entry with more than one text (LS-10).** The screen shows the entry's texts in the order §3.2
  gives, each under a heading (`titleMedium`, `onSurface`, `heading()` semantics): for Jakarta,
  `license_text_part_notice` "Notice" over the NOTICE, then `licenses_apache_name` "Apache License 2.0"
  over the Apache text; for the Public Suffix List, "Notice" over OkHttp's NOTICE, then
  `licenses_mpl_name` "Mozilla Public License 2.0" over the MPL text. One `Column`, one
  `SelectionContainer`, the headings inside it. A single-text entry shows no part heading, because its
  licence name is already the top bar's title.
- **Title.** The top bar's title stays the entry's licence name. For a two-text entry it is the
  licence, never the notice ("Apache License 2.0" for Jakarta; "Mozilla Public License 2.0" for the
  Public Suffix List).
  *(Loop 2, 2026-10-24, F-20 and the brief's F1: **for an unknown entry id the title is
  `licenses_title` "Licenses"** — the state's header is null, so there is no licence name to show,
  and an empty heading stop would be a defect by §5.1's own contract (pass line 4). An existing string,
  no new key; it still tells the player where they are. LS-AC14 asserts it.)*
- **End of text.** The last item of the body, after the last paragraph, is `license_text_end` "End of
  text" (`labelMedium`, `onSurfaceVariant`). It gives a sighted reader and a TalkBack reader the same
  definite end, and it is the node the scroll-to-end tests find (LS-AC05, LS-AC15).
- **Loading and error** stand as written, and both now announce themselves (§5): the spinner's
  description and the error text are polite live regions.
- **The state holder** is LS-09's `LicenseTextViewModel`: `Loading(entry)`, `Loaded(entry,
  parts)`, `Failed(entry?)`, where `Failed` also covers an unknown entry id from the route (R4: a route
  carries an id, never a path, and an unknown id is this error state, never a crash or a file lookup).
  How a two-text entry is typed (one `BundledLicenseText` per part) is the tech-lead's build brief, §9.

## 4A. The Google notices screen (the list)

*(New, 2026-10-24; tech design §13.1 LN-4 and LN-5 item 2; LS-09, LS-12, LS-13.)* Opened from the
Licenses screen's "Open-source notices in Google libraries" entry. It lists every (name, text) pair of
`assets/licenses/google/index.tsv`, which the build generates from the Google AARs on the variant's
runtime classpath (LN-2, LN-3; about 108 rows at today's versions). Route `GoogleNotices`.

**Purpose.** To display, inside the app and offline, the open-source notices Google supplies for the
software inside its libraries, which Google's guide puts on the app's developer and which AGP never
packages. It is a notices display: the names and texts are Google's, shown as supplied.

**Layout.**
- A transparent `Scaffold` and transparent `TopAppBar` over the Sparse backdrop (as §3.1): back
  (`navigate_back`, 48dp, `claySquish`, silent) and the title `google_notices_title` "Open-source
  notices" (`titleLarge`).
- A `LazyColumn` capped at `content_max_width`, centred, padding `spacing_md`, items spaced
  `spacing_md`, **keyed by the index's `<key>`**, `rememberLazyListState()` (saveable, so the position
  survives the round trip to a text). The entrance stagger (`clayEntrance`) applies to the first
  screenful only, as it does for every long list in the app; later rows enter plainly.
  *(Loop 2, F-23: "the first screenful" is **the first `LicensesUi.STAGGERED_ROWS` = 8 items**, the
  intro included, and "as it does for every long list in the app" is withdrawn — §3.1, loop 2, gives
  the number and the reason; the brief's F6 is confirmed.)*
- First item: the intro `google_notices_intro` (`bodyMedium`, `onSurfaceVariant`).
- Then one row per index line, in the index's order (sorted by name, case-insensitively, then by
  stem; LN-3 — loop 1, F-11: "then by text" was wrong). **A row** is the §3.1 clay card, merged as
  every entry card is (§3.1, F-02): title = the pair's name (`titleMedium`, `onSurface`), supporting
  line = `google_notices_found_in` "Found in %1$s" with the sources joined by ", " (`bodyMedium`,
  `onSurfaceVariant`, no `maxLines`), and — only when the name occurs more than once in the index —
  a third line `google_notices_ordinal` "Notice %1$d of %2$d" (`bodyMedium`, `onSurfaceVariant`;
  LS-12 as amended in loop 1, F-08), trailing chevron (decorative), `claySquish()` + `clickable` with
  `ButtonForward`, click label `google_notices_read_action` "Read the notice". Every row is tappable;
  there is no inert row on this screen.
- *(Loop 1, F-04 — position by ear.)* **The UX intent is that a TalkBack player hears where they are
  in this list**: about 108 rows with no sections is the one place in the app where "row 12 of 108"
  earns its words. The rows are counted alone, so the intro is not row 0 and the last row is "N of
  N". Whether the pinned Compose BOM's `LazyColumn` already publishes per-item collection info, or the
  rows set `collectionItemInfo(rowIndex = i)` under a list-level `collectionInfo(rowCount = N,
  columnCount = 1)` explicitly, is a mechanism **owed to the tech-lead's brief** (§9 item 7). LS-AC13's
  oracle does not depend on it (§8.1).
  *(Loop 2, 2026-10-24, F-26 and the brief's A1.2: **answered, and the conditional is closed.** The
  pinned Foundation's `LazyColumn` publishes list-level info with an unknown row count and no per-item
  info, so the build sets both explicitly: `collectionInfo(rowCount = N, columnCount = 1)` on the
  list's modifier, and `collectionItemInfo(rowIndex = i, rowSpan = 1, columnIndex = 0, columnSpan = 1)`
  inside each row's merged node, `i` the row's 0-based position among the rows, the intro carrying
  none. TalkBack then says "N of M" with M the index's line count. Every row therefore carries, inside
  its one merged node: the title, the "Found in" line, the ordinal when present, the Button role, the
  click label and its collection item info. LS-AC13 asserts it unconditionally.)*
  *(Device re-run ruling, 2026-10-24, O-2: the semantics hold, with `rowCount` N and the last
  `rowIndex` N − 1, but "the last row is 'N of N'" and "TalkBack then says 'N of M'" are not what
  the player hears. TalkBack 16 words this list's total as "over a hundred", so the last row is
  "108 of over a hundred". The position is exact, and the position is the intent: where the
  player is. The total is TalkBack's wording, the same as on every list on the phone. LS-AC13 and
  §8.2 item 10 are restated to match.)*
- No section headers: the list is flat and sorted. No search, no filter, no count — those are features
  a notices display does not need, and each would be a string and a state the release does not owe.

**States** (`GoogleNoticesViewModel`, LS-09: `Loading`, `Loaded(rows)`, `Failed`).
- **Loading:** nothing for the first `LicensesUi.SPINNER_DELAY_MS` (300 ms, the §4 rule); then a
  centred `CircularProgressIndicator` (`primary`) with description `google_notices_loading` "Loading
  notices", a polite live region, until the index is parsed.
- **Loaded:** the list above. An index with no lines is `Failed`, not an empty list: a build that
  produced no notices is a defect (LN-7 item 2), and an empty screen would hide it.
- **Failed** (the index is missing, unreadable, or has a malformed line, LN-4): under the intro, the
  text `google_notices_error` "These notices could not be shown." (`bodyMedium`, `onSurfaceVariant`),
  a polite live region. No retry: the asset cannot change at runtime. A test guarantees the index
  parses, so this state should never ship to a player.

**Behaviour.** A tap on a row opens the Google notice text screen for that row's key (route
`GoogleNoticeText(key)`); back and system back return one step, to the row's position. The screen
works in airplane mode and makes no network request (LS-05, LS-AC07).

## 4B. The Google notice text screen

*(New, 2026-10-24; tech design §13.1 LN-4; LS-08, LS-13.)* One (name, text) pair, full text. Route
`GoogleNoticeText(key)`; the key is resolved through the index read from the APK, and an unknown key
is the error state (R4's rule: nothing from a route reaches `AssetManager.open`).

**Layout.**
- Top bar as §4A; title = the pair's **name** (`titleLarge`, wraps, no `maxLines`); it is not a
  string resource, it is the index's name.
- Under the bar, inside the content column: the sources line, `google_notices_found_in` with the same
  joined sources (`bodyMedium`, `onSurfaceVariant`, `heading()` semantics so it is the screen's
  content heading), then the text.
  *(Loop 1, F-13: the `heading()` on the sources line is **withdrawn** — a list of artifact ids is a
  poor target for heading navigation, and the top-bar title, the name, is already the screen's one
  heading. The sources line is plain text, the first item of the body. When the row carried an
  ordinal (LS-12, F-08), the ordinal line follows the sources line, plain text too.)*
- **The body is a `LazyColumn`** (LS-08's exception), capped at `content_max_width`, centred, padding
  `spacing_md`, items spaced `spacing_sm`, **one re-flowed paragraph per item** (`bodyMedium`,
  `onSurface`, directly on the Sparse backdrop, zone T), **each paragraph inside its own
  `SelectionContainer`**, items keyed by paragraph index, `rememberLazyListState()`. The last item is
  `license_text_end` "End of text", as §4. The largest text is Cronet's, 785,183 B in 2,497
  paragraphs: it opens at its first paragraph and scrolls to its last without a dropped frame the
  player notices or an ANR (LS-AC15).
  *(Loop 1, F-14: "without a dropped frame the player notices" is **withdrawn** as a promise; what is
  gated is **no ANR**, and the janky-frames reading after the scroll is recorded, with no bound, for
  the tech-lead to read (LS-AC15). F-01: the list is the node a test scrolls — it carries the
  scroll-to-node action, and its test tag is the brief's — because a lazy item that is not on screen
  is not composed and cannot be found by its text.)*
- **Direction** as §4: the notices are English; content-based direction, mirrored chrome.
  *(Device re-run ruling, 2026-10-24, F-2: as §4 now reads, LS-16. The notice text is a
  document: its body, with the sources and ordinal lines, reads left to right and hangs from the
  column's left edge in every locale (the re-run's `s26` had it right-aligned and reordered). The
  chrome mirrors.)*

**States** (`GoogleNoticeTextViewModel`, LS-09, exposing the same sealed `LicenseTextUiState` as §4).
- **Loading:** the 300 ms rule, then the spinner with description `licenses_loading` "Loading
  license" — the same string as §4, because to the player a notice is a licence text; polite live
  region.
- **Loaded:** the list above. The text is read through `LicenseTextSource` at the stem the index
  gives, re-flowed by `reflowLicenseText` off the main thread (LN-4).
- **Failed** (unknown key, missing or unreadable stem): the title shows the name when the key is
  known and `google_notice_text_unknown_title` "Notice" when it is not; the body is
  `google_notice_text_error` "This notice could not be shown." (`bodyMedium`, `onSurfaceVariant`),
  a polite live region.
  *(Loop 2, 2026-10-24, F-24 and the brief's F2: **"Notice" is the title for any null header,
  `Loading(null)` included** — the name is known only once the index is parsed, so every open passes
  through a moment with no name, and the screen's one heading is never empty while TalkBack meets it.
  The same string serves both states; no new key.)*

**What it does not have.** No "copy all" and no share: the Licenses texts have neither, and a
per-paragraph selection is what LN-4's lazy body allows. No jump-to-end control: the end is reachable
by scrolling, and TalkBack's continuous reading reaches it (§5). If a later founder ask wants either,
it is a new decision, not a gap.

## 5. Accessibility

- **Touch:** tappable entries ≥ 72dp and full-card; back 48dp; ≥ 8dp between cards (`spacing_md`).
- **Labels and roles:** each entry is one merged node read title first; a tappable entry has
  `Role.Button` and the click label `licenses_read_action` "Read the license"; an inert entry has no
  click action and no button role. The chevron is decorative. Section headers and the text screen's
  title carry `heading()`.
- **Traversal (Licenses):** back → title → intro → Fonts → Fredoka → Nunito → Sounds → its three entries
  → Open-source software → its entries → Google libraries → its entry.
  *(Loop 2, 2026-10-24, F-29: "its entry" reads "its two entries" since the second edition — the SDK
  entry, then the notices entry — and Open-source software's entries are eleven; §5.1's table is the
  order of record.)*
- **Not colour alone:** tappable vs inert is the chevron plus the press response (LS-06).
- **Contrast:** card text uses the design system's verified pairs (`onSurface` / `onSurfaceVariant` on
  `surfaceContainerHigh`); text on the backdrop (intro, headers, licence paragraphs) is on Sparse's zone
  T: `onSurfaceVariant` ≥ 5.0:1 light and ≥ 6.9:1 dark (`docs/ux/candy-backdrop.md` §5.1).
- **Text scaling:** everything is an M3 type role and wraps; nothing truncates; both lists scroll; at
  200% a card simply grows.
- **Dark theme, RTL:** both screens use only theme roles; layout mirrors in RTL.
  *(Device re-run ruling, 2026-10-24, F-2: the layout mirrors and the texts do not. Every text
  reads in its own direction on all four screens, with the two edges of LS-16.)*
- **Reduced motion:** the entrance stagger and the squish are already gated by
  `rememberAnimationsEnabled()`.
- **Sound:** `ButtonForward` on the Settings row and on opening a text (D-09); back is silent; sound is
  never the only feedback — every press also navigates visibly.

### 5.1 The TalkBack contract for the four screens (LS-13)

*(Second edition, 2026-10-24. The bullets above are the first edition's and stand; this subsection
widens them to four screens and writes them as a spoken contract, in the shape of D-TB and D-TBa
(`docs/ux-spec.md` §11.1, on this branch since `develop` 4069b53 — loop 1, F-07): what is a stop,
what each stop says, where focus goes, what is announced, and the pass line. The founder's Q-023
answer (option 1, `.planning/questions/FOUNDER.md`) is about the board; this role reads it, with the
question's premise that every app meets its platform's accessibility rules before it ships, as:
nothing ships that TalkBack cannot use. These four screens are the simplest in the app to make true
to that, and so the contract is short. Loop 1 also folds F-02 (the merge), F-04 (position), F-05
and F-15 (the pass lines) and F-12 (contrast without the backdrop) below.)*

**What is a stop, and what it says.** TalkBack's swipe order is the reading order below; the words
in quotes are what the merged node carries, in this order, with TalkBack's own role and hint words
after them.

| Screen | Reading order | A stop says |
|---|---|---|
| **Licenses** (§3) | back → title (heading) → intro → `Fonts` (heading) → Fredoka → Nunito → `Sounds` (heading) → Kenney → LilMati → CogFireStudios → `Open-source software` (heading) → its eleven entries in §3.2's order → `Google libraries` (heading) → the SDK entry → the notices entry | A **tappable entry**: "<title>, <credit>, <licence>, <parts>", role Button, click label `licenses_read_action` "Read the license" (the notices entry: `licenses_google_notices_action` "Open the notices"). An **inert entry**: the same lines, merged into one node by its card (§3.1, loop 1 F-02), no role, no click action, so TalkBack adds no "double-tap to activate". A **source address** (Sounds, the SDK entry, the Public Suffix List) is read as part of its entry's node; its `SelectionContainer` adds no stop of its own *(address ruling, 2026-10-24, LS-14: superseded. On the device the container did add a stop, QA's F-1. There is no `SelectionContainer`: the address is plain text, read last in its card's node, and nothing on a card is a stop of its own)* |
| **License text** (§4) | back → title (heading: the licence name) → the entry's title and credit (heading) → [part heading] → each paragraph → [part heading → each paragraph] → "End of text" | A paragraph: its text. A part heading: "Notice" or the licence name, heading |
| **Google notices** (§4A) | back → title (heading) → intro → each row in the index's order | A row: "<name>, Found in <sources>[, Notice 2 of 4]" (the ordinal only on a repeated name, LS-12 as amended), role Button, click label `google_notices_read_action` "Read the notice" |
| **Google notice text** (§4B) | back → title (heading: the name) → "Found in <sources>" (plain text; loop 1 F-13 withdrew its heading) → [the ordinal line] → each paragraph → "End of text" | as the License text screen |

Three properties hold on every screen:
- **One stop per thing.** An entry or a row is **one merged node**, read title first. *(Loop 1,
  F-02: the mechanism is the card's own `Modifier.semantics(mergeDescendants = true) {}`, §3.1 — not
  the `clickable`, which only a tappable entry has. The second edition's "`mergeDescendants = true`
  through `clickable`, as every `SettingsLinkRow` is" is **superseded**: `SettingsLinkRow` with
  `onClick = null` merges nothing, and so would have left each inert entry as three or four stops.)*
  The chevron is decorative (`contentDescription = null`) and adds no stop. Nothing on these screens
  is a two-stop control.
- **Headings are headings.** The top-bar title, every section header, the text screens' entry title
  and part headings carry `heading()`, so heading navigation moves band to band and text to text.
  The intro, the paragraphs and (since loop 1, F-13) the "Found in" line do not.
  *(Device re-run ruling, 2026-10-24, F-4: and heading navigation reaches them. Every heading the
  table marks is reached in both directions, from any stop and at any scroll position, with
  nothing else between two headings (LS-15, pass line 4 as restated). On the Licenses screen that
  holds for the four section headers whether or not they are on screen.)*
  *(LS-15 ruling, 2026-11-01: restated for a scrolling list. Going down, every heading is reached
  in order, and a stop TalkBack makes after its own page scroll is accepted only if it is a whole
  card, title first. Going up, and from the title with the list scrolled, the behaviour is the
  platform's: recorded, not graded. Being in the semantics tree is necessary, not sufficient.
  LS-15, pass line 4.)*
- **Lists are collections.** The intent is that TalkBack announces position on the notices list
  ("row 12 of 108"), rows counted alone. *(Loop 1, F-04: "as Compose gives it" is **superseded** — a
  `LazyColumn` publishes list-level info and not, by itself, per-item info, and the intro would have
  been counted as a row. The mechanism — explicit `collectionInfo` on the list and `collectionItemInfo`
  on each row, or the BOM's own if it provides it — is owed to the tech-lead's brief, §4A and §9 item
  7; LS-AC13's oracle stands without it.)* *(Loop 2, F-26: answered by the brief's A1.2 — explicit
  `collectionInfo` on the list and `collectionItemInfo` on each row, rows counted alone; §4A. The
  conditional is closed: TalkBack announces "N of M" on the notices list.)* *(Device re-run
  ruling, 2026-10-24, O-2: by ear the position is exact and the total is TalkBack's own wording,
  "over a hundred" for 108 rows in TalkBack 16; §4A, LS-AC13.)* The Licenses list has fourteen tappable stops (two fonts,
  eleven software entries, the notices entry) and four inert ones (three sounds, the SDK entry); the
  notices list about 108; a text as many stops as paragraphs plus the headings and the end line.

**Focus.** Opening any of the four screens moves no focus: a screen that has just opened is met in
its reading order, like every other screen this product designs (D-TBa item 6, `docs/ux-spec.md`
§11.1). Returning with back
or system back restores the list's scroll position (LS-AC02); where TalkBack's focus lands on return
is the platform's own placement, and both the row that was opened and the top of the list are
accepted. The screens request no input focus and add no `traversalIndex`: the visual order is the
reading order on every window, so nothing needs pinning.

**Announcements.** Exactly two things announce themselves, each once, politely (`liveRegion =
LiveRegionMode.Polite` on the node that carries the text):
- the loading description (`licenses_loading`, `google_notices_loading`), when the spinner appears
  after the 300 ms rule — and so never when the text is in before then;
- the error text (`licenses_text_error`, `google_notices_error`, `google_notice_text_error`), when the
  failed state is shown.
Nothing else is announced: a navigation is heard through the new screen's title, a press through the
navigation it causes, a sound cue is never the only feedback (rules/audio.md), and no text is read
twice. With TalkBack off nothing is spoken.

**Long texts.** A text screen is a plain reading surface, so TalkBack's continuous reading ("read
from next item") proceeds paragraph by paragraph to "End of text" without a stop that is not a
paragraph. On the lazy body (§4B) that holds because TalkBack scrolls a `LazyColumn` as it reads; the
device pass reads at least twenty paragraphs of Cronet's notice continuously and scrolls, by TalkBack's
own scroll gesture, to its last (LS-AC15). Selection on the lazy body is per paragraph, and that is
the design (LS-08).

**The house checklist, restated for four screens** (rules/compose.md; `docs/ux-spec.md` §11):
- **Touch:** every tappable entry and row is full-card and ≥ `settings_row_min_height` 72dp; back
  48dp; ≥ `spacing_md` 16dp between cards (≥ 8dp). Paragraphs and headings are not targets.
- **Not colour alone:** tappable against inert is the chevron plus the press response by eye, the
  Button role plus the click label by ear (LS-06, LS-13).
- **Contrast:** card text on `surfaceContainerHigh` and backdrop text on Sparse's zone T, both from
  the design system's verified pairs: `onSurface` ≥ 10:1 light and ≥ 14:1 dark, `onSurfaceVariant`
  ≥ 5.0:1 light and ≥ 6.9:1 dark (`docs/ux/candy-backdrop.md` §5.1). The spinner is `primary` on the
  backdrop, a glyph at ≥ 3:1. Nothing names a hex: every colour is a `MaterialTheme.colorScheme` role.
  *(Loop 1, F-12: `candy-backdrop.md` is not on this branch, and this branch may land first, in
  which case the text sits on `background`, not on zone T. The figures QA reads then come from
  `docs/ui/DESIGN-SYSTEM.md`'s hexes, computed by this role: light `onSurface` #332F3A on
  `background` #F4F1FA ≈ 11.7:1 and on `surfaceContainerHigh` #F1EAFB ≈ 11.1:1, `onSurfaceVariant`
  #635F69 ≈ 5.6:1 and ≈ 5.3:1, `primary` #7C3AED on `background` ≈ 5.1:1; dark `onSurface` #EDE9F5 on
  `background` #1B1626 ≈ 14.8:1 and on `surfaceContainerHigh` #2E2740 ≈ 11.9:1, `onSurfaceVariant`
  #A79FB5 ≈ 7.0:1 and ≈ 5.6:1, `primary` #A78BFA on `background` ≈ 6.5:1. Every pair clears 4.5:1 for
  text and 3:1 for the glyph in both themes; QA measures on the device. The zone-T figures are the
  post-backdrop case, read once that branch lands.)*
- **Text scaling:** every size is an M3 type role; no `maxLines` anywhere on the four screens; cards
  grow, lists scroll; at 200 % a title wraps and the "Found in" line wraps under it. Nothing is a
  fixed height except the 48dp back target and the 72dp card minimum, both minimums.
  *(Address ruling, 2026-10-24, the qa-engineer's §8.2 item 1 note: **a single token wider than the
  line breaks where it overflows, and that is accepted for 3.0.0.** At 200 % on the test phone,
  `play-services-ads-identifier` (28 characters) is wider than the "Found in" line, and the
  platform cuts it between two letters: "…ads-identifi" / "er, play-services-cronet" (J2ObjC,
  notice 4 of 5, `31-gnotices-mid-dark-200.png`). The Public Suffix List address's 40-character
  commit hash is cut the same way on the Licenses card. Why it is accepted:
  - Every character is shown, nothing is truncated, nothing scrolls sideways or overlaps, and
    TalkBack reads the id whole, so §5's "nothing truncates" and the text-scaling rules hold.
  - The ids are where-found metadata on a page two taps deep, and no player acts on them.
  - Of the two ways to move the break, automatic hyphenation would **add** hyphens that are not in
    Google's ids ("identi-fier"), which misrepresents names LS-12 shows as supplied. Invisible break
    characters change the string that the tests and TalkBack read.
  Elsewhere the platform already breaks where a reader expects: between ids at ", ", and after "/"
  in an address. It only falls back to cutting between letters when one token has no break point
  that fits. The finer break, after an id's own hyphen, is a recorded polish item with its shape,
  §9 item 16, and not a 3.0.0 change.)*
- **Dimensions:** every dp is a `dimens.xml` token already in the file (`spacing_sm`, `spacing_md`,
  `content_max_width`, `settings_row_min_height`, `settings_row_icon_size`, `elevation_card`,
  `shape_corner_large`); these screens add **no new token** and no literal. Durations come from
  `ClayAnim` and the scoped `LicensesUi` object (the 300 ms).
- **Light and dark:** theme roles only; every new public composable has light and dark `@Preview`s
  (through `BackdropPreviewHost` once the backdrop lands, E7).
- **RTL:** the chrome mirrors (back arrow, chevron, padding); the texts stay left to right by content.
  *(Device re-run ruling, 2026-10-24, F-2: "by content" was assumed to be the default, and in
  Compose it is not. It is now a rule of these screens, LS-16. Every text takes its direction from
  its first strong character, so its characters stay in written order. A list's lines hang from
  the mirrored start edge, a text screen's body from its left edge. LS-AC19.)*
- **Reduced motion:** the entrance stagger and the squish are gated by `rememberAnimationsEnabled()`;
  the spinner is a progress indicator, which the platform keeps.
- **One-handed use:** the lists scroll under the thumb; the only actions are the back button and the
  cards themselves, and every card is reachable by scrolling.

**The pass line (LS-AC06, read by QA with TalkBack on, on each of the four screens, in light and
dark, at 100 % and 200 %).** PASS when all of these hold; any one failing is FAIL on the screen named:
1. swiping forward from the back button reaches every stop in the table's order and nothing else —
   on the two list screens through the whole list; on the two text screens through the chrome and the
   first screenful of paragraphs, because the body is point 5's *(loop 1, F-15: 2,500 swipes on
   Cronet's notice is not a pass line)*;
2. every tappable entry or row is announced with its lines, "button" and its click label, and a
   double-tap opens the text it names;
   *(device re-run ruling, 2026-10-24, on the re-run's O-1: a card read without its title, or as
   a bare "Button", fails this line, or line 3 for an inert card, whichever navigation lands on
   it. It is not accepted as platform behaviour. It was seen only after heading navigation's
   scroll fallback, and LS-15's fix is expected to remove it.)*
   *(LS-15 ruling, 2026-11-01: "LS-15's fix is expected to remove it" is superseded. The cause is
   the card's own speech, built from whichever of its texts are on screen, and the tech-lead's R2
   fixes it: each card carries its whole label as its own description, so it is read whole, title
   first, at any scroll position. The line itself stands, and it is graded on every card stop of
   every run, heading runs included.)*
3. every inert entry is announced as **one** stop with its lines, and without "button" or
   "double-tap to activate" *(loop 1, F-02)*;
4. heading navigation stops on exactly the headings the table marks;
   *(device re-run ruling, 2026-10-24, F-4 and LS-15: **restated**; the line above is kept as the
   record. With TalkBack's reading control on Headings, from any stop and at any scroll position,
   a swipe down moves to the next heading the table marks and a swipe up to the previous one, each
   scrolled into view. From the top every heading is reached in the table's order, and from the
   bottom every one in reverse. **Nothing that is not a heading is a stop between two headings**:
   no entry, no card half under the top bar, no paragraph. What TalkBack does on a swipe past the
   last heading or before the first is the platform's own and is not graded: an end sound,
   nothing, or a scroll that lands on a paragraph or an entry. On the Licenses list this holds
   whatever the list is made of. A list that composes only the rows on screen, and so hides the
   headers off screen, fails it. Run at 100 % and at 200 %, where more headers are off screen. One
   theme suffices, because colour does not change the tree.)*
   *(LS-15 ruling, 2026-11-01: **restated again**, and the restatement above is kept as the
   record. TalkBack reaches an off-screen heading only through its own page scroll, so "from any
   stop and at any scroll position" and "both directions" are not the app's to deliver. The line
   now reads, with TalkBack's reading control on Headings:
   (a) **down from the back button**, swipes reach the title, then every section header in the
   table's order, none skipped and none repeated; **down from an entry in the middle of the
   list**, every heading below it is reached in order;
   (b) between two headings going down, the only accepted non-heading stop is the stop TalkBack
   makes right after one of its own page scrolls, at most one per scroll, and it must be a whole
   card, title first (pass line 2 or 3). A non-heading stop with no TalkBack scroll before it, a
   card read without its title, or a paragraph or the intro as such a stop, fails;
   (c) **up from any stop, and down from the title with the list scrolled**, are recorded and not
   graded. Pass line 2 still grades every card stop in them;
   (d) past the last heading and before the first stay the platform's.
   The list still composes every item, and a lazy list still fails (LS-AC06's Compose half).
   On the text screens, which hold all their headings, the line is unchanged.)*
5. on a text screen, continuous reading proceeds to "End of text" (on Cronet's notice, at least
   twenty paragraphs are read continuously and TalkBack's scroll reaches the end);
6. the loading description, when it appears, and the error text are each spoken once when they
   appear and never repeated spontaneously; each is read again only when focus lands on it, because
   it is a stop in the reading order *(loop 1, F-05: "not again on the next swipe" was wrong — a
   swipe that lands on the node reads the node)*;
7. back and system back return one step, and nothing is spoken twice for one event.

## 6. Copy

New strings in `res/values/strings.xml`. Proper names, licence names and addresses carry
`translatable="false"`.

| Key | Text |
|---|---|
| `settings_section_about` | About |
| `settings_licenses_label` | Licenses |
| `settings_licenses_supporting` | Fonts, sounds and open-source software |
| `licenses_title` | Licenses |
| `licenses_intro` | Peg Solitaire is made with these fonts, sounds and open-source software. Thank you to their authors. |
| `licenses_section_fonts` | Fonts |
| `licenses_section_sounds` | Sounds |
| `licenses_section_software` | Open-source software |
| `licenses_section_google` | Google libraries |
| `licenses_read_action` | Read the license |
| `licenses_loading` | Loading license |
| `licenses_text_error` | This license text could not be shown. |
| `licenses_sound_effects` (plurals) | one: %d sound effect · other: %d sound effects |
| entry titles, credits, licence names, parts lines and addresses | exactly as §3.2's table, one key each (`licenses_fredoka_title`, `licenses_fredoka_credit`, `licenses_ofl_name`, `licenses_cc0_name`, `licenses_apache_name`, `licenses_android_sdk_name`, …) |

*(Second edition, 2026-10-24 — added strings. The table above stands. Proper names, licence names
and addresses stay `translatable="false"`. The Google notices' names and texts are **not** strings:
they are data from the generated index and are shown as supplied (LS-12).)*

| Key | Text |
|---|---|
| `licenses_google_sdk_title` | Google Play services and SDKs |
| `licenses_google_sdk_parts` | Play Games Services, Mobile Ads, User Messaging Platform, Play inline-install library, Play services libraries |
| `licenses_google_notices_title` | Open-source notices in Google libraries |
| `licenses_google_notices_supporting` | The notices Google supplies for the open-source software inside its libraries |
| `licenses_google_notices_action` | Open the notices |
| `google_notices_title` | Open-source notices |
| `google_notices_intro` | Google supplies these notices for the open-source software inside its libraries. Each one is shown as supplied. |
| `google_notices_found_in` | Found in %1$s |
| `google_notices_read_action` | Read the notice |
| `google_notices_loading` | Loading notices |
| `google_notices_error` | These notices could not be shown. |
| `google_notice_text_error` | This notice could not be shown. |
| `google_notice_text_unknown_title` | Notice |
| `license_text_part_notice` | Notice |
| `license_text_end` | End of text |
| `licenses_bsd3_name` | BSD 3-Clause License |
| `licenses_mit_name` | MIT License |
| `licenses_mpl_name` | Mozilla Public License 2.0 |
| `licenses_psl_address` | github.com/publicsuffix/list/blob/a312abe0e11421aff8fe909a5e02f36b7c5d28de/public_suffix_list.dat *(loop 1, from `research-analyst-mpl.md`: the exact version compiled into okhttp 4.11.0; the second edition's `publicsuffix.org/list/public_suffix_list.dat` is superseded — it serves a list that changes daily, and the NOTICE the entry opens already names it. **Re-derived before the cut** if the release listing, U-6, shows an okhttp other than 4.11.0: §9 item 13)* |
| `google_notices_ordinal` | Notice %1$d of %2$d *(loop 1, F-08; only on a repeated name, LS-12)* |
| the eight new entries' titles, credits and parts lines | exactly as §3.2's table, one key each, in the same pattern (`licenses_okhttp_title`, `licenses_okhttp_credit`, `licenses_okhttp_parts`, …, `licenses_checker_title`, `licenses_checker_credit`) |

Lint's `PluralsCandidate` has no new case: the only count is the existing `licenses_sound_effects`
plural. The `%1$s` of `google_notices_found_in` is the joined sources string, built once per row in
the ViewModel's row model, never in composition.

*(Address ruling, 2026-10-24: two strings confirmed as built, both from the developer's report,
deviations 5 and 6.)*
- `licenses_jakarta_credit` "The Eclipse Jakarta Dependency Injection project" is **confirmed**
  (§3.2, the Jakarta row).
- `licenses_psl_parts` writes its date with U+2011 non-breaking hyphens, "2023‑02‑15", and is
  **confirmed**.
  - By eye: the date renders as the font's hyphen. At 200 % in dark it matches the hyphen of
    "public-suffix" on the line above (`29-licenses-dark-200-p07.png`).
  - It never splits across lines.
  - It clears lint's TypographyDashes, which reads a date as a range, without a suppression.
  - By ear: TalkBack hands the characters to speech unchanged, with no symbol name
    (`tb-licenses-linear.txt`, the Public Suffix List node). How the speech engine says them was
    not heard, because the emulator muted TTS audio (`tts-check.txt`). §8.2 item 12 listens on a
    device with audio.
  - If that check hears the date as anything but its three numbers in order (a character name, or
    a number dropped), the fix is already decided: the string reads "…, as it stood on February 15,
    2023", ASCII only, and the tests that pin it follow.
  - Until then the ISO form stays. It is the form of the research-analyst's report and of the
    list's own repository, and it reads correctly by eye.

The candy-backdrop words grep (BD-01) runs over these strings too and must print nothing.
*(2026-10-24: and over the nine curated texts of §7 — loop 1, F-03: the second edition wrote
"eight"; nine ship, the two OFL texts, the Apache text and the six new files — the generated
`google/` directory is outside it, §3.2 "Never listed". Loop 2, F-22: "BD-01's grep" is, on this
branch, `UserFacingTextHasNoProtectedNamesTest`, which this branch writes — §3.2's loop-2 note and
LS-AC17.)*

## 7. Assets and licences

| Asset | Status | Licence | Record |
|---|---|---|---|
| `assets/licenses/Fredoka-OFL.txt`, `Nunito-OFL.txt` | ship today on `feature/phase-15-ux-refresh` | OFL-1.1 notice files | THIRD_PARTY.md §2 font rows |
| `assets/licenses/Apache-2.0.txt` | **new**: the verbatim Apache License 2.0 text (apache.org/licenses/LICENSE-2.0.txt) — §4(a) of that licence asks that recipients receive a copy of it | the licence document's own terms | a THIRD_PARTY.md row, and a one-line confirmation of the text's own redistribution terms by the research-analyst (non-blocking) |
| `assets/licenses/<name>-MIT.txt` | only if the release listing confirms an MIT-licensed artifact ships | MIT notice | its THIRD_PARTY.md family row |
| `Icons.AutoMirrored.Rounded.KeyboardArrowRight`, `ArrowBack` | `material-icons-core`, already a dependency | Apache-2.0 | already covered by the AndroidX family |

No raster, no new dependency, no permission.

*(Second edition, 2026-10-24. The rows above stand with two updates: the Apache-2.0 row's "new" is
now backed by THIRD_PARTY.md §2's row and the research-analyst's verdict 2 (2026-10-18, CLEAN, "may
be redistributed verbatim"); the conditional `<name>-MIT.txt` row is resolved — the one MIT family is
Checker Framework, below. The rows below are added. **Nine curated texts ship** (loop 1, F-03): the
two OFL texts already on the branch, the Apache text, and the six below. Every text is fetched by
the developer from the URL THIRD_PARTY.md names, committed byte-identical, and pinned by its full
SHA-256 in the LS-AC12 test;
the research-analyst recorded the first 16 hex digits of each, and the developer's report records the
full hash, which the release-manager copies into THIRD_PARTY.md. File names follow the existing
`<Name>-<Licence>.txt` pattern and are written in code once, in `BundledLicenseText` (R4).)*

| Asset | Status | Licence | Record |
|---|---|---|---|
| `assets/licenses/Jakarta-Inject-NOTICE.md` | **new**: byte-identical to `jakarta.inject-api-2.0.1.jar!/META-INF/NOTICE.md`, 1,074 B, sha256 `992ed62d…a512214df540` | an Apache-2.0 NOTICE; reproducing it is what Apache-2.0 §4(d) asks | THIRD_PARTY.md §2, the Apache-2.0.txt row; research-analyst font report, verdict 2 |
| `assets/licenses/Protobuf-BSD-3-Clause.txt` | **new**: protobuf's `LICENSE` at v22.3, verbatim, 1,732 B, sha256 prefix `6e5e117324afd944` | the BSD-3-Clause text; reproducing it is its condition 2 | THIRD_PARTY.md §5b rows F and G |
| `assets/licenses/Chromium-BSD-3-Clause.txt` | **new**: Chromium's `LICENSE` at 143.0.7445.0, verbatim, 1,536 B, sha256 prefix `368cca1106be99d3` | as above | §5b row H |
| `assets/licenses/CheckerQual-MIT.txt` | **new**: checker-qual 3.33.0's `LICENSE.txt`, verbatim, 1,126 B, sha256 prefix `e90eaa08dea089df` | the MIT text; reproducing it is its one condition | §5b row I |
| `assets/licenses/PublicSuffixList-NOTICE.txt` | **new**: OkHttp parent-4.11.0's `okhttp3/internal/publicsuffix/NOTICE`, verbatim, 218 B, full sha256 `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a` *(loop 1, `research-analyst-mpl.md` §3: the same hash at the OkHttp tag, in the jar and in the 2026-09-21 release build)* | an MPL-2.0 notice naming the source; what MPL §3.2(a) asks, together with the exact-version address on the entry (§3.2) | §5b row J |
| `assets/licenses/MPL-2.0.txt` | **new** (LS-11): the Mozilla Public License 2.0 text from `https://www.mozilla.org/media/MPL/2.0/index.txt` (the steward's plain text, the stable unhashed name), verbatim, 16,726 B, 373 lines, LF, full sha256 `3f3d9e0024b1921b067d6f7f88deb4a60cbe7a78e76c64e3f1d7fc3b779b9d04` *(loop 1: confirmed; not SPDX's or the PSL repository's copy, which differ by one trailing space, `research-analyst-mpl.md` §3)* | the licence document's own terms — **research-analyst, 2026-10-24: CLEAN**, may ship verbatim in a commercial app (verdict 1); the one-line confirmation §9 item 6 asked for is given, and the item is closed | §5b row J (the release-manager's "How credited" cell now reads "and the MPL-2.0 text", per the verdict's transcription table) |
| `assets/licenses/google/index.tsv` and `google/<stem>.txt` | **generated per variant** by the build (tech design §13.1 LN-2, LN-3), never committed; about 42 text files and one index at today's versions, 2,053,748 B raw, ≈ 273 KB deflated | Google's own `third_party_licenses.txt` slices, reproduced verbatim: what the Android SDK licence §3.8 and Google's open-source notices guide ask | THIRD_PARTY.md §4 (the three Google SDK rows) and §5b rows A and E; tech design §13.1 |

Still no raster, no new dependency (LN-2: the merge is a Gradle-free object in `build-logic`, read
with what its classpath already has), no permission. The AAB grows by the generated directory;
LN-7 item 6 records the delta as baselined, not gated.

## 8. Acceptance criteria

| # | Criterion | By | Pass line |
|---|---|---|---|
| **LS-AC01** | Settings shows an **About** header and the **Licenses** row after Play Games (or after General when Play Games is absent) and before Privacy; Privacy is still last; the entrance orders are renumbered whole (§2) | Test, QA | `SettingsScreenTest`'s rendered-top order check passes with the new rows |
| **LS-AC02** | The row opens Licenses; a tappable entry opens its text; back and system back return one step at a time; the list keeps its scroll position on return | Test, QA | as stated |
| **LS-AC03** | The list equals THIRD_PARTY.md's shipped, credit- or licence-bearing rows at the release branch, and contains none of §3.2's never-listed items | Test (a JVM test against the expected list), release-manager (diff against THIRD_PARTY.md at checklist steps 1.4 and 6) | equal; none present |
| **LS-AC04** | No banned word: BD-01's grep over these strings prints nothing; neither screen names King, Candy Crush or "candy" | Auditor, QA | zero lines |
| **LS-AC05** | Each bundled text opens, is complete (the shown text, whitespace normalised, equals the asset's), re-flows at 1.0 and 2.0 font scale, is selectable, and is one node per paragraph | Test, QA | as stated |
| **LS-AC06** | Accessibility as §5: TalkBack order and headings; tappable entries announce "Read the license" with a button role; inert entries are not announced as actionable; no truncation at 200%; light and dark; RTL chrome mirrored with LTR licence text | Test, QA (TalkBack pass) | as stated |
| **LS-AC07** | Offline: both screens work in airplane mode; no network access and no outbound intent from either | QA | as stated |
| **LS-AC08** | Both screens carry the Sparse backdrop (BD-06) with transparent `Scaffold` and `TopAppBar` (BD-14) | Test | as stated |
| **LS-AC09** | `ButtonForward` plays on the Settings row and on opening a text; back is silent | Test (`ButtonSoundTest` idiom), QA | as stated |
| **LS-AC10** | No new dependency and no new permission; colours and dimensions from tokens only; light and dark `@Preview`s for every new public composable; one Compose UI test per screen's main flow (Licenses: open, read the sections, open Fredoka; License text: load, scroll, and the error state) | Auditor, Check | as stated |
| **LS-AC11** | The release AAB contains `assets/licenses/Fredoka-OFL.txt`, `Nunito-OFL.txt` and `Apache-2.0.txt` | release-manager (archive listing at checklist step 1.4) | present |

### 8.1 Second edition — amended and new criteria (2026-10-24)

*(The table above is the first edition's and stays as the record. Where a row below carries the same
id, it replaces that row's wording; the tech-lead's R5 split (JVM, Robo, Compose, Pixel, Device,
Review) names the kind of each test, and tech design §13.1 LN-7 is folded in by id. Every test is
deterministic (rules/testing.md): the Compose test clock, fixtures for the generated assets, no wall
clock, no network. Each test is watched failing once before it is trusted (R5).)*

| # | Criterion | By | Pass line |
|---|---|---|---|
| **LS-AC01** (amended, loop 1 F-09; oracle rewritten in loop 2, F-19) | As the first edition's row, and: every Settings section header shown (General, Play Games when present, About, Privacy) carries `heading()` — one change to `SettingsSectionHeader`, never the About header alone (§2; the brief's A1.4, commit 10a) | Compose (`SettingsScreenTest`: the rendered-top order; `everySectionHeaderIsAHeading` asserts the heading property **on each section header node** rather than counting — three before About lands, four after, and three on a `Unknown`/no-Play-Games state), QA (with TalkBack's heading navigation, the stops on Settings are every section header shown, and the title where it is one) | the order check passes; each header shown is a heading |
| **LS-AC02** (amended) | Four round trips keep their place: Settings → Licenses → Fredoka → back → back; Licenses → Jakarta Dependency Injection (a two-text entry) → back; Licenses → Open-source notices → a row → back → back; and system back equals the back button on every screen. The Licenses list's and the notices list's first visible index survive their round trips | Compose (`AppNavHost` with `NavFlowTest`'s stand-ins, a fixture index), QA | as stated, on gesture and three-button navigation |
| **LS-AC03** (amended) | `LicenseCatalogTest`: the catalogue equals §3.2's table **in its order**, written into the test as the expected list from §3.2.1, and contains none of §3.2's never-listed items; every `BundledLicenseText.assetPath` names a file under `src/main/assets/licenses/` that exists and is non-empty; every committed file in that directory is opened by at least one entry (the first edition's "exactly once" is superseded: `Apache-2.0.txt` is opened by seven entries through one enum constant), and **the directory holds files only and no subdirectory** *(loop 2, F-27 and the brief's §0.2: the second edition's "`google/` excluded" excluded nothing — the generated tree lives under `build/` and is never in `src/main/assets/licenses/`; a committed subdirectory there would be LN-3's violation, and the files-only assertion fails on it whatever its name; the generated tree is LS-AC12's)*. The release-manager diffs §3.2.1 against THIRD_PARTY.md at checklist items 1.4 and 6 | JVM; release-manager | equal, in order; none present; every file referenced; no subdirectory |
| **LS-AC05** (amended; loop 1 F-03: nine, not eight) | Every curated text (the nine files of §7: `Fredoka-OFL.txt`, `Nunito-OFL.txt`, `Apache-2.0.txt`, `Jakarta-Inject-NOTICE.md`, `Protobuf-BSD-3-Clause.txt`, `Chromium-BSD-3-Clause.txt`, `CheckerQual-MIT.txt`, `PublicSuffixList-NOTICE.txt`, `MPL-2.0.txt`) opens and is complete: `reflowLicenseText`'s whitespace-normalised join equals the whitespace-normalised asset for each (JVM); each is read through the real `AssetManager` (Robo); on the License text screen the body is one `Column` with one node per paragraph, a selection can span two paragraphs *(loop 2, the check's open issue 3 and the brief's §4.1: if Robolectric cannot deliver the long-press-and-drag gesture, the Compose clause is the **structural** half — both paragraphs sit under one `SelectionContainer` and no paragraph has one of its own — stated so in the developer's report; §8.2 item 8 is the behavioural half, on the device. The test is narrowed, never dropped)*, a two-text entry shows "Notice" then its licence heading in that order, and "End of text" is the last node (Compose); re-flows at 1.0 and 2.0 font scale with no truncation (QA) | JVM (re-flow) + Robo (the reads through `AssetManager`) + Compose + QA | as stated |
| **LS-AC06** (amended; loop 1 F-02) | §5.1's TalkBack contract holds on all four screens: the reading order of its table; headings on exactly the nodes it marks (and not on the "Found in" line, F-13); `Role.Button` and the click label on every tappable entry and row; **each of the four inert entries is exactly one node in the merged tree, carrying its title, credit, licence and address text, with no `OnClick` action and no `Role`** (`onAllNodesWithText(<title>)` in the merged tree finds one node; its config has neither `SemanticsActions.OnClick` nor `SemanticsProperties.Role`); the chevron carries no description; the loading description and the error text are polite live regions (Compose, with `SemanticsProperties.LiveRegion` asserted); the seven-point pass line (QA, TalkBack on, light and dark, 100 % and 200 %). *(Address ruling, 2026-10-24, QA's F-1: the merged-tree assertions passed while the device read each address as a stop of its own, so the criterion gains an **unmerged-tree** oracle. No node that carries one of the five addresses is focusable or has a focus action of its own, and the address text is reachable only through its card's merged node: `LicencesTalkBackContractTest.anAddressIsPartOfItsCardsStopAndNeverAStopOfItsOwn`, red before `2d433b8` ("kenney-interface-sounds: … RequestFocus"), green after. The merged-tree assertions stand beside it.)* *(Device re-run ruling, 2026-10-24, F-4 and LS-15: pass line 4 is restated. Heading navigation reaches every heading in both directions, from any stop and at any scroll position, with nothing else between two headings. The Compose half is owed to the tech-lead's re-run ruling, which names the test. This role's intent is a test that is red on the build QA re-ran (`2d433b8`) and green on the fix. It asserts the property heading navigation depends on, header by header and never as a count (F-19's rule): with the Licenses list scrolled to its end, and again to its middle, the title and each of the four section headers is found by its text as a heading in the semantics tree.)* *(LS-15 ruling, 2026-11-01: that Compose half stays and is **necessary, not sufficient**. It guards against a list that composes only what is on screen, and it cannot see what TalkBack is given (`tech-lead-f4-ruling.md`, R1). The device half is pass line 4 as restated again on 2026-11-01: down in order, one whole-card stop at most after each TalkBack page scroll, and the up and title-mid-list runs recorded, not graded. R2's label tests join it for pass line 2: each card's merged node carries its whole label, title first, as its own description, and its texts are hidden from accessibility; the tests are the tech-lead's to name.)* | Compose + QA | as stated |
| **LS-AC08** (amended, E7) | All four routes — `Licenses`, `LicenseText`, `GoogleNotices`, `GoogleNoticeText` — are Sparse (BD-06's `BackdropDensityTest`) and draw transparent containers (BD-14's `TransparentContainersTest`). **Owned by whichever of `feature/licences-screen` and `feature/candy-backdrop` lands on `develop` second**; the first to land asserts nothing about the backdrop | Compose, on the second branch | as stated |
| **LS-AC10** (amended) | No new dependency (`git diff develop...HEAD -- gradle/libs.versions.toml build-logic/build.gradle.kts` adds no coordinate, LN-7 item 6) and no new permission; colours and dimensions from tokens only, no new `dimens.xml` token (§5.1); light and dark `@Preview`s for every new public composable; **one Compose UI test per screen's main flow, four screens**: Licenses (open from Settings, read the four sections, open Fredoka, open the notices entry); License text (load, `performScrollTo()` on the "End of text" node of the `Column` body, a two-text entry, the error state through a fake `LicenseTextSource`); Google notices (load from a fixture index, open a row, the error state from a malformed fixture); Google notice text (load a fixture text, scroll **the list** to "End of text" with `performScrollToNode` as LS-AC15 — loop 1 F-01 — then the unknown-key error state); warning-free under C7 once check-gates is on `develop` | Auditor, Check | as stated |
| **LS-AC11** (amended; loop 1 F-03) | `unzip -l <aab> \| grep 'base/assets/licenses/'` lists the **nine** curated texts of §7 by name — `Fredoka-OFL.txt`, `Nunito-OFL.txt`, `Apache-2.0.txt`, `Jakarta-Inject-NOTICE.md`, `Protobuf-BSD-3-Clause.txt`, `Chromium-BSD-3-Clause.txt`, `CheckerQual-MIT.txt`, `PublicSuffixList-NOTICE.txt`, `MPL-2.0.txt` — plus `google/index.tsv` and every stem file the index names (LN-7 item 4) | release-manager (checklist 1.4 and 4.6a) | all present |
| **LS-AC12** (new) | **The bundled notices match the build's dependencies.** (a) `build-logic`: `GoogleNoticeMergeTest`, the tech-lead's six cases (LN-7 item 1: two AARs sharing a pair give one line with both sources; one name with two texts gives two lines; permuted inputs give byte-identical output; each `<stem>.txt` equals its slice byte for byte; each LN-2 failure fails the build naming the AAR; an AAR without the JSON contributes nothing). (b) on the generated debug assets, read through the real `AssetManager` *(loop 2, F-25 and the brief's F8: these assertions are **Robolectric** in R5's vocabulary, not plain JVM — the merged assets' location under `build/` is AGP's, and the stream the test hashes is the one the app reads)*: `index.tsv` parses, has at least one line and unique keys; every stem it names exists and that file's SHA-256 starts with the stem; **the set of its distinct sources equals** the ten AARs tech design §13.1 measured at today's versions — `ads-mobile-sdk`, `hsdp`, `user-messaging-platform`, `play-services-games-v2`, `play-services-appset`, `play-services-basement`, `play-services-base`, `play-services-tasks`, `play-services-ads-identifier`, `play-services-cronet` — *(loop 1, F-06: four of ten would not catch a dropped sibling; **loop 2, F-21 and the brief's A1.3: "include" is superseded by "equals"** — the criterion's title is "match", (c) already demands equal sets at the release, and a new or dropped Google AAR must fail this test until the research-analyst's verdict and its THIRD_PARTY.md row land, because an eleventh AAR with notices is exactly the fact checklist 3.5 must hear about; the list is a constant in the test, `theSourcesAreExactlyTheTenMeasuredArtifacts`)*; `parseGoogleNoticeIndex` is tested on a well-formed, an empty and a malformed index (plain JVM); each curated text of §7 exists and its full SHA-256 equals the hash the test pins (Robolectric; the MPL text and the NOTICE at the full hashes §7 now records; the others' first 16 hex digits are the research-analyst's). **Why debug stands for the build that ships** *(loop 1, F-06; loop 2, F-26 and the brief's A1.3: answered)*: the UX intent is that the test proves what ships. On this app the two runtime classpaths differ only by `ui-tooling` and `ui-test-manifest` (THIRD_PARTY.md §0), neither a Google AAR — and that is now **a check, not an assumption**: (d) a Gradle task, `verifyGoogleNoticesParity`, in the same `google-notices` plugin, on which `:app:check` depends, fails naming the first differing file when the `debug` and `release` generated trees are not byte-identical or when fewer than two variants were registered; CI runs it on every PR, and it is not a release invocation (no `PLAY_UPLOAD_*`). The second edition's "whether the JVM test also runs against the release variant's generated directory" is superseded by (d): a JVM test cannot read that directory, and the Gradle check proves the equality the JVM test relies on. (c) Release *(loop 1, F-06: the procedure)*: from checklist 3.5's `releaseRuntimeClasspath` listing, for each AAR coordinate `<group>:<artifact>:<version>`, `unzip -l ~/.gradle/caches/modules-2/files-2.1/<group>/<artifact>/<version>/*/<artifact>-<version>.aar \| grep -c third_party_licenses.json`; the set of artifactIds with a hit equals the set of distinct sources in the AAB's `google/index.tsv` | JVM (build-logic: (a) and (d)'s `VerifyGoogleNoticesParityTest`); Robo (`:app`: the generated assets, the hash pins); JVM (`:app`: the parser); release-manager ((c)) | (a), (b) and (d) green; (c) equal sets, both listed in the release report |
| **LS-AC13** (new; oracle rewritten in loop 1, F-04 and F-08) | The notices list shows every index line: with a fixture index of N lines that includes two lines sharing a name **and identical sources** (the hard case), the screen shows N rows in the index's order, each with its "Found in" line; the two same-name rows carry "Notice 1 of 2" and "Notice 2 of 2" and open different texts; a row whose name occurs once carries no ordinal; and the list's `CollectionInfo.rowCount` is N and the last row's `CollectionItemInfo.rowIndex` is N − 1, rows counted without the intro (Compose) *(loop 2, F-26: unconditional since the brief's A1.2 set the semantics explicitly; the second edition's "when the brief delivers" is superseded)*. On the device the qa-engineer scrolls to the end and reads the last row's name, sources and ordinal against the last line of the AAB's `index.tsv`, which the release-manager supplies, and TalkBack's position announcement on that row says "N of N" with the same N as the index's line count (QA) *(Device re-run ruling, 2026-10-24, O-2: the QA half is **restated**, and "says 'N of N'" is superseded and kept as the record. TalkBack 16 speaks this list's total as "over a hundred", so every row is "k of over a hundred" and "108 of 108" is never heard. The QA half now reads: in pass line 1's transcript of the whole list, **every row's spoken position equals its line number in the index, 1 to N, in order, none skipped or repeated**. So the first row is "1 of …" (the intro is not counted), and the last row's position is N, the index's line count. The last row's name, sources and ordinal match the last index line. **The total after "of" is TalkBack's own wording and is not graded by ear.** A rounded form such as "over a hundred" passes, and a TalkBack that speaks the total as a number must speak N. The exact total is the Compose half's `rowCount` N, which no rounding reaches. Why this line and not a demand for the exact total: the player needs the position, and hears it exactly. The total is how TalkBack words every list on the phone. Making the app say it differently, with a count in each row's announcement or in the intro, would add words to 108 stops and a number §4A chose not to show.)* | Compose + QA | N rows, the ordinals as stated; `rowCount` N, last `rowIndex` N − 1; the last row matches the last index line and is announced "N of N" *(O-2: by ear, every row's position equals its index line number and the last is N; the total is TalkBack's wording, and if spoken as a number it is N)* |
| **LS-AC14** (new; loop 2 F-20) | Unknown ids are the error state, never a crash and never a file lookup: `LicenseText("missing")` shows §4's error with the entry's title absent **and the top-bar title `licenses_title` "Licenses"** (§4, loop 2); `GoogleNoticeText("missing")` shows §4B's error under the "Notice" title (Compose); on both, heading navigation finds one non-empty heading, the bar's (pass line 4). The only `assets.open` call sites in `:app` are the two sources, each opening a path from `BundledLicenseText` or from the parsed index, never from a route (Review: a grep) | Compose + Review | as stated |
| **LS-AC15** (new; oracle rewritten in loop 1, F-01) | The largest text reads end to end: with a fixture of 2,500 paragraphs the notice text screen opens with paragraph 1 displayed and "End of text" **not** composed; then the **list** node — the one carrying the scroll-to-node action, found by the test tag the brief names — is scrolled with `performScrollToNode(hasText("End of text"))` (or `performScrollToIndex(paragraphCount)`), after which `onNodeWithText("End of text").assertIsDisplayed()` passes (Compose; the second edition's `onNodeWithText("End of text").performScrollTo()` is superseded: a lazy item off screen is not composed and cannot be found). On the device Cronet's notice opens without an ANR, scrolls end to end, and the `dumpsys gfxinfo` janky-frames line read after the scroll is recorded in the QA report, unbounded, for the tech-lead (QA) | Compose + QA | as stated; no ANR |
| **LS-AC16** (new) | Loading announces itself only when it is shown: with the test clock paused and the text delivered before 300 ms, no spinner node is ever composed; delivered after, the spinner's node carries the loading description and a polite live region; the error node carries a polite live region (Compose). QA hears the loading description once on a slow open and the error text once in the error state, neither repeated spontaneously, and each read again only when a swipe lands focus on it *(loop 1, F-05)* | Compose + QA | as stated |
| **LS-AC17** (new; widens LS-AC04 from "neither screen" to the four screens; loop 1 F-03: nine; loop 2 F-22: the test's author) | `UserFacingTextHasNoProtectedNamesTest` — **written on this branch** (the brief's §4.3, commit 3; it was BD-01's test and existed nowhere, and this branch lands before candy) — covers §6's new strings and §7's nine curated texts: it scans `src/main/res/values*/strings.xml` and `src/main/assets/**` (files only, recursive) for whole words, case-insensitively, the verdict's twelve (`candy`, `candies`, `crush`, `saga`, `candyland`, `tiffi`, `toffee`, `yeti`, `odus`, `king`, `activision`, `microsoft`) and A.4's four (`jelly`, `soda`, `sugar`, `sweet`), a constant the candy branch owns and may widen thereafter; the generated `google/` directory lies outside its scan by construction (`build/`), and the developer's report records the same grep's result over it, for the code-auditor's information | JVM (this branch's test) + Review | zero lines in scope; the out-of-scope result recorded |
| **LS-AC18** (new; split in loop 2, F-28) | The size delta against `develop` is recorded in two places, because a release build is gated on the four `PLAY_UPLOAD_*` variables the developer does not hold (tech design §9.2.5): **the developer** records the debug APK delta (`:app:assembleDebug` on both arms) and the merged-assets byte count of `licenses/google/`; **the release-manager** records the release AAB delta at checklist 4.6 (LN-7 item 6; the estimate is ≈ 0.3 MB compressed). Baselined, not gated (tech design §10, D-12) | Developer (debug); release-manager (release) | both recorded |
| **LS-AC19** (new; device re-run ruling, 2026-10-24, F-2, LS-16) | **Every text reads in its own direction under an RTL layout.** (Compose; the tech-lead's re-run ruling names the test, red on `2d433b8` and green on the fix.) With the layout direction RTL, each kind of text on the four screens resolves to a left-to-right paragraph: the intro, a section header, a card's title and its other lines, a "Found in" line, an ordinal line, a paragraph of a curated text, a paragraph of a notice, and "End of text". The direction is read from each text's own layout, for example `TextLayoutResult.getParagraphDirection(0)` through the node's `GetTextLayoutResult` action; the handle is the tech-lead's. (QA) §8.2 item 6 by eye: on all four screens the characters stay in written order; on the two lists every line hangs from the mirrored start edge; on the two text screens the body hangs from the left edge | Compose + QA | as stated |

### 8.2 What QA checks on a device

The qa-engineer's session, on the device matrix of `docs/ux-spec.md` §11 and on the release build
where the generated assets are the release variant's. Each item is a screenshot or a recording under
`.planning/qa/licences-screen/`, listed in the QA report by path:

1. **The four screens, light and dark, at 100 % and 200 % font scale:** Licenses (all four sections),
   License text for Fredoka (one text), Jakarta Dependency Injection (two texts), Google notices (the
   top and a page in the middle), Google notice text for Cronet (its first screen, a middle screen, its
   end). No truncation anywhere; cards grow; the "Found in" line wraps.
   *(Address ruling, 2026-10-24: an id or a commit hash wider than the line, cut between two
   letters where it overflows, is accepted for 3.0.0 and is not a finding (§5.1, Text scaling).
   Anything clipped, overlapped or ellipsised still is.)*
2. **The §5.1 pass line with TalkBack**, on each of the four screens (LS-AC06), and the continuous
   read of Cronet's notice (LS-AC15).
   *(Device re-run ruling, 2026-10-24, F-4 and LS-15.)* Pass line 4, as restated, is run on the
   Licenses screen three ways, at 100 % and at 200 %, in one theme:
   - down from the back button to the last heading;
   - up from the last entry to the title;
   - from an entry in the middle of the list, both ways.
   Each run records every stop. Any card read without its title, or as a bare "Button", is
   recorded against pass line 2 (O-1).
   *(LS-15 ruling, 2026-11-01: the runs stay; their grading changes. Every run records every
   stop and every TalkBack scroll.)*
   | Run | Graded |
   |---|---|
   | Down from the back button to the last heading | **Pass line 4 (a), (b):** the five headings in order, none skipped or repeated; a non-heading stop only right after a TalkBack page scroll, at most one per scroll, a whole card, title first |
   | From an entry in the middle of the list, down | **Pass line 4 (a), (b)**, for the headings below that entry |
   | From an entry in the middle of the list, up; up from the last entry; from the title with the list scrolled, down | **Recorded, not graded** for heading order (pass line 4 (c)). Pass line 2 grades every card stop |
   | Every run, every card stop | **Pass line 2 or 3:** read once, whole, title first; "Button" and the hint on a tappable card only |
   - At 100 % on the release-candidate check. At 200 %, the down run is graded the same way when
     it is run; for the 3.0.0 re-check of F-4 it is not owed (the founder's reduced scope), and
     the producer decides whether to run it.
   - A run that fails is reported once, with its transcript. A recorded run is never repeated in
     the hope of another result.
3. **Round trips** (LS-AC02): both back routes, gesture and three-button; the lists return to where
   they were.
4. **Airplane mode** (LS-AC07): every screen and every text opens with no network; no outbound
   intent from any of them.
5. **Animator duration scale 0:** no entrance stagger and no squish; everything still opens.
6. **`ar-XB` pseudo-locale:** chrome mirrored (back arrow, chevron, padding), every text left to
   right, nothing clipped.
   *(Device re-run ruling, 2026-10-24, F-2 and LS-16: made exact.)* Set the language list to
   `[ar-XB]` alone, as the re-run did (with `en-US` also listed, the app resolves to English and is
   laid out left to right). Screenshot all four screens. PASS when all of these hold:
   - **the chrome mirrors**: the back arrow at the right pointing right, the chevrons at the left
     pointing left;
   - **every character stays in written order**: the full stop at the end of the intro and of
     "… partnership with others." in the OFL, "© 2016 The Fredoka Project Authors", "7 sound effects", "“Pop 01” by
     LilMati", the OFL's dash rules whole, and the full stop after "POSSIBILITY OF SUCH DAMAGE" on
     Cronet;
   - **on Licenses and Google notices**, every line hangs from the right edge, a card's wrapped
     lines included;
   - **on License text and Google notice text**, the body hangs from the left edge;
   - **nothing is clipped.**
7. **A 360 × 640 dp window and a tablet window:** the content column caps at `content_max_width`
   and stays centred; nothing draws under the status bar.
8. **Selection:** a paragraph of a curated text and a paragraph of a Google notice can be selected
   and copied; on the curated text the selection can span two paragraphs.
9. **Sound** (LS-AC09): `ButtonForward` on the Settings row, on an entry and on a notice row; back is
   silent.
10. **The last row** (LS-AC13, as rewritten in loop 1): scroll the notices list to its end and read
    the last row's name, sources and ordinal against the last line of the AAB's `index.tsv` (the
    release-manager supplies it); when the brief has delivered the collection semantics, TalkBack's
    position on that row says "N of N", N being the index's line count.
    *(Device re-run ruling, 2026-10-24, O-2: **restated**. "Says 'N of N'" is superseded, and
    "when the brief has delivered" was closed in loop 2 (F-26). TalkBack 16 says "k of over a
    hundred" for the 108 rows. The item passes when, in pass line 1's transcript of the whole list,
    every row's spoken position equals its line number in the index (1 to N, in order, none
    skipped or repeated), the last row's position is N, and the last row's name, sources and
    ordinal match the last index line. The total after "of" is TalkBack's wording and is not
    graded; if it is spoken as a number, it is N. LS-AC13.)*
11. **Settings headings** (LS-AC01, loop 1; reworded in loop 2, F-19): with TalkBack's heading
    navigation, the stops on Settings are every section header shown (three without Play Games,
    four with it), and the title where it is one.
12. **The Public Suffix List date by ear** *(address ruling, 2026-10-24; non-blocking, and only on a
    device or emulator whose speech is audible; the 2026-10-24 session's TTS was muted)*: with
    TalkBack on, focus the Public Suffix List card and listen to "as it stood on 2023‑02‑15".
    - PASS when the date is spoken as a date or as its three numbers in order.
    - FAIL when a character name is spoken or a number is dropped. The fix for a FAIL is already
      decided (§6): the date in words.
    - Record which of the two was heard.

## 9. Open items routed to the producer

1. **THIRD_PARTY.md** gains a row for the bundled Apache-2.0 text, and the research-analyst confirms its
   own redistribution terms in one line — the release-manager's and the research-analyst's,
   non-blocking.
2. **U-1** (the UMP licence) decides whether the Google entry names the User Messaging Platform (§3.2).
3. **The §5b families** this screen lists beyond the three named ones come from the release branch's
   `releaseRuntimeClasspath` listing (THIRD_PARTY.md §0, checklist step 6).
4. **Apache NOTICE files.** If any shipped Apache-2.0 artifact carries a `NOTICE` file, Apache §4(d)
   asks that its attribution notices travel with the app; the release-manager checks the listing for
   one, and if found, its text is appended to that family's entry as a second bundled text.
5. **Pulling this into 3.0.0 or 3.0.1** is the producer's call (THIRD_PARTY.md §7 records the gap as a
   studio-rule gap, not a legal blocker: every shipped notice obligation is already met by the files in
   the artifact).

*(Second edition, 2026-10-24 — status of items 1–5, and the items this edition adds.)*

- **Item 1 — closed.** THIRD_PARTY.md §2 carries the Apache-2.0.txt row, and the research-analyst's
  verdict 2 (2026-10-18) confirms the text may ship verbatim.
- **Item 2 — closed.** U-1 closed 2026-10-21 (row B); the Google entry names the User Messaging
  Platform (§3.2).
- **Item 3 — closed as a design question, open as a release check.** The research-analyst's sweep
  (192 of 192 POMs, 2026-10-21) gave the families; §3.2 lists them. U-6, the per-coordinate listing
  on `release/3.0.0`, stays the release-manager's (checklist 3.5): a coordinate outside the 192 needs a
  verdict and, if it carries a notice, an entry here before the cut.
- **Item 4 — closed.** One Apache NOTICE ships, Jakarta Inject's; it is bundled and opened by its own
  entry (LS-10, §3.2, §7). The scan of the 51 coordinates not in the local cache is checklist 3.5a's,
  on the release branch; a second NOTICE found there is a second entry of the same shape.
- **Item 5 — closed.** The producer's call was made and then overtaken: the Licenses screens block the
  branch cut (tech design §14.1 row LN; §16 #20's deadline, because BSD-3 and MIT make the notice a
  condition of redistribution). "Or 3.0.1" no longer applies (THIRD_PARTY.md §7, update 2026-10-23).

6. **Research-analyst, non-blocking:** one line confirming the MPL-2.0 text's own redistribution terms
   (LS-11, §7), as was done for the Apache text. Mozilla publishes the licence for exactly this use,
   so no change to the design is expected; if the line says otherwise, the Public Suffix List entry
   opens the NOTICE alone and LS-11 is superseded.
   *(Loop 1, 2026-10-24 — **closed.** `research-analyst-mpl.md`: CLEAN, the text may ship verbatim;
   LS-11 stands; the full hash is in §7. F-18's note on the fall-back is recorded for completeness:
   had the line said no, the entry would also have shown the licence's address, `mozilla.org/MPL/2.0/`,
   as selectable text (the LS-05 pattern), so that no Open-source software entry names a licence it
   cannot show. The fall-back is moot.)*
7. **Tech-lead, in the build brief:** (a) the shape of a two-text entry — this document says only what
   is shown (LS-10, §4); whether `LicenseEntry.text` becomes `texts: List<BundledLicenseText>` or a
   pair is the brief's; (b) LS-AC03's asset-reference test excludes `google/` (LS-AC12 owns it) and
   accepts one asset opened by several entries; (c) the notices list's row model carries the joined
   sources string, built once, and the key the route needs (LS-12, §4A); (d) `GoogleNoticesViewModel`
   treats an empty index as `Failed` (§4A); (e) `LicensesUi.SPINNER_DELAY_MS` is shared by all three
   loading screens.
   *(Loop 1 — four more, each with this role's intent stated and the mechanism the brief's:)* **(f)
   the merge (F-02, §3.1):** every entry card is one merged node with no role and no action of its
   own; whether that is `Modifier.semantics(mergeDescendants = true) {}` on a new entry composable, or
   a fix to `SettingsLinkRow`'s `onClick = null` branch that its other callers then inherit, is the
   brief's. **(g) position in the notices list (F-04, §4A):** rows counted alone, the intro excluded,
   so TalkBack can say "N of M"; whether the pinned BOM's `LazyColumn` gives per-item info or the rows
   set `collectionItemInfo` under an explicit `collectionInfo(rowCount, 1)`. **(h) debug for release
   (F-06, LS-AC12 (b)):** the test must prove what ships; whether the JVM test also reads the release
   variant's generated directory. **(i) the section headings (F-09, §2):** all four Settings section
   headers are headings, through `SettingsSectionHeader`; where in the build order the one-line change
   sits. **(j) the row model's ordinal (F-08, LS-12):** "Notice n of m" computed once per index load,
   only for repeated names. **(k) the list's test tag (F-01, LS-AC15):** the §4B list node's tag, so
   the scroll oracle has a handle.
   *(Loop 2, 2026-10-24, F-26 — **all answered** by the tech-lead's brief, `tech-lead-brief.md` §0 and
   Amendment 1; recorded here as pointers, the intent above unchanged, the brief's wording governing
   the code:)* (a) a list, `LicenseEntryTarget.Texts(parts: List<BundledTextRef>)`, never a pair
   (§0.1); (b) files only and no subdirectory, no name exclusion (§0.2; LS-AC03 as amended in loop 2);
   (c) `GoogleNoticeRow(key, name, foundIn, ordinal)` built once in the ViewModel (§0.3, A1.5); (d)
   and (e) confirmed (§0.3); **(f)** the merge is on the entry card (`LicenseEntryCard`, its own
   composable) **and** `SettingsLinkRow`'s `onClick = null` branch is fixed too, in commit 10a, so the
   signed-in Play Games statement row on Settings becomes one stop by LS-06's house rule (A1.1 — a
   consequence this role accepts: two inert cards on adjacent screens must not read differently);
   **(g)** explicit `collectionInfo`/`collectionItemInfo`, rows counted alone (A1.2; §4A); **(h)** not
   a JVM test but a Gradle parity task in `:app:check`, `verifyGoogleNoticesParity` (A1.3; LS-AC12
   (d)); **(i)** one line on `SettingsSectionHeader` in `SettingsRows.kt`, commit 10a, which also makes
   the Licenses screen's four section headers headings, since it reuses the composable (A1.4, T3);
   **(j)** `NoticeOrdinal(index, count)` from a pure `noticeOrdinals()` grouping by the exact name
   string, used by both ViewModels (A1.5); **(k)** `GOOGLE_NOTICE_TEXT_LIST_TAG`, with
   `GOOGLE_NOTICES_LIST_TAG` and `LICENSES_LIST_TAG` for LS-AC02's position checks (A1.6). The brief's
   §8 rulings F1, F2 and F6 are the UX calls this role has now made its own (§4 title, §4B title, the
   stagger cap of 8); F3, F4, F5 and F8 are folded as F-27, F-22, F-28 and F-25; F7 is the
   release-manager's; its §0.4 departures T1–T3 are the tech-lead's and change nothing the player sees.
   Nothing in item 7 is owed any longer.
8. **Release-manager:** THIRD_PARTY.md's "How credited" cells for rows A, E, F–J, the Apache table and
   the Apache-2.0.txt row name the §3.2 entry titles as written here (several of those cells still
   quote the research-analyst's suggested titles, e.g. "Checker Framework qualifiers" — unchanged — and
   the row-F/G "Protocol Buffers" — unchanged — but the Google rows should point at "Open-source notices
   in Google libraries"); the Sounds counts are re-cut if `feature/audio-review` misses the release
   (§3.2); the full hashes of the §7 texts, from the developer's report.
   *(Loop 1:)* row J's Source URL gains the exact-version address and its "How credited" cell reads
   "and the MPL-2.0 text" with the full hash (`research-analyst-mpl.md`, "Changes to transcribe");
   the NOTICE's full hash `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a`; checklist
   3.5 gains the U-6 re-derivation note (item 13 below).
   *(Loop 2:)* two record notes from the second check and the brief, neither a spec defect: THIRD_PARTY.md
   §2's Apache-2.0.txt row writes the Jakarta NOTICE hash as `992ed62d…512214df540` while tech design
   §13.1 and §7 here write `992ed62d…a512214df540` — the developer's full hash settles the spelling
   when it is copied into both; and checklist 4.6a (c) lists six prefixes and lacks `MPL-2.0.txt`'s
   `3f3d9e00` (the brief's F7), added with the full hashes.
9. **Code-auditor, at review:** LS-AC14's grep (`assets.open` call sites); LS-AC17's out-of-scope grep
   result; the previews; no new token, no literal, no new dependency.
10. **Qa-engineer:** §8.2, with the release build's generated assets; the Cronet janky-frames reading;
    the §5.1 pass line per screen.
11. **Whichever branch lands second** (`feature/licences-screen` or `feature/candy-backdrop`): LS-AC08's
    four routes in `BackdropDensityTest` and `TransparentContainersTest`, and the two new screens'
    containers made transparent with `contentColor = onBackground` (R1's rule), E7.
12. **The Settings renumbering against `feature/colour-vision`** (§2): whichever lands second
    renumbers on top of the first; nothing to decide, recorded so neither developer treats the other's
    numbers as a conflict.
13. **The Public Suffix List's exact-version address is tied to okhttp 4.11.0** (loop 1;
    `research-analyst-mpl.md`, open issue 2). `ads-mobile-sdk` 1.4.0 pulls that okhttp in. If the
    release listing (U-6, checklist 3.5) shows another okhttp, the address in `licenses_psl_address`
    is **re-derived before the cut** the same way the research-analyst derived this one: hash that
    tag's test-resource `public_suffix_list.dat` and find the PSL commit with the same git blob. The
    release-manager owns the check; the research-analyst the derivation; the string change is the
    developer's.
14. **The fresh re-check** (the dispatch's rule): this loop was written by the author on fable; a
    fresh fable agent that did not write it checks the third state of the document.

*(Address ruling, 2026-10-24: three items added. None blocks 3.0.0.)*

15. **Copy address, deferred (LS-14).** For the producer to schedule after 3.0.0; nothing in 3.0.0
    waits for it.
    - The shape is fixed in LS-14: a long-press on any of the five address cards, and the same
      long-click action labelled "Copy address" for TalkBack. The card stays one stop. The platform's
      clipboard confirmation on Android 13 and later; "Address copied" below it.
    - Two new strings: `licenses_copy_address_action` "Copy address" and `licenses_address_copied`
      "Address copied".
    - Who: the tech-lead's brief rules the mechanism, the android-game-dev builds it, and QA checks
      it by hand, by TalkBack's actions menu and by pasting.
    - It can ride with item 16 as one licences polish task.
16. **Ids break after their own hyphen: optional polish, not scheduled** (§5.1, Text scaling).
    - Shape: when an artifact id in a "Found in" line (§4A) or the sources line (§4B) is wider than
      the line, it breaks **after the last of its own hyphens that fits**, and never between two
      letters. No hyphen is ever added.
    - What TalkBack reads, what the tests read and the index's text stay the exact id, so any break
      opportunity is display-only.
    - The commit hash in the Public Suffix List address has no such boundary and keeps the
      platform's cut.
    - Mechanism: the tech-lead's. Either a display-only break opportunity after each hyphen inside an
      id, with the node's text semantics kept to the plain string, or a line-break setting of the
      pinned Compose that breaks at existing hyphens without adding any. Plain automatic hyphenation
      is ruled out, because it adds hyphens.
17. **Release-manager:** THIRD_PARTY.md's "How credited" cell for the Jakarta row quotes the credit
    as confirmed, "The Eclipse Jakarta Dependency Injection project" (§3.2; the developer's handoff
    asked for the same note).

*(Device re-run rulings, 2026-10-24: two items added.)*

18. **The tech-lead's re-run ruling, in parallel with this one.** It owns:
    - the mechanisms for LS-15 (F-4) and LS-16 (F-2);
    - the tests named in LS-AC06's F-4 half and in LS-AC19;
    - F-3, the width cap, which needs no UX ruling: §3.1, §4, §4A and §4B already say "capped at
      `content_max_width`, centred", and that stands.
    If the ruling finds that LS-15 cannot be delivered in 3.0.0, it comes back to this role through
    the producer. Nothing in the build waits on this role.
    *(2026-11-01 — **closed for LS-15.** It came back: `tech-lead-f4-ruling.md` R1 found LS-15 as
    written not deliverable by any change to this screen. This role restated it for a scrolling
    list (option F), rejected the section index (E) and the scrolling title (D), and kept the
    non-lazy list (A); LS-15, pass lines 2 and 4, LS-AC06, §8.2 item 2. The merge condition R5
    item 1 is met by this ruling. LS-16's half was closed by QA's re-check, F-2 PASS.)*
19. **RTL across the rest of the app: for the producer to schedule; it does not block this
    branch** (LS-16, Scope). QA notes that the cause, a text that names no direction taking the
    layout's, applies app-wide. Settings shares this screen's section header and row pattern. So an
    RTL-only player meets the same moved punctuation on every other screen. `docs/ux-spec.md`
    names no text-direction rule today; its one RTL alignment line is D-04f's Undecided notice.
    - This role proposes a house rule for `docs/ux-spec.md` §11 in LS-16's words: texts read in
      their own direction; lists and controls hang from the mirrored start; long reading surfaces
      hang from the text's start. It follows once the tech-lead's mechanism for these four screens
      shows how it generalises.
    - Whether the fix lands on this branch for these four screens only, or app-wide, is the
      tech-lead's call. A fix at the shared row or theme level that also corrects Settings is
      welcome to this role, as long as these four screens meet LS-16.

## 10. Record of amendments

Nothing in this document is deleted; a superseded wording stays with a dated pointer, and this table
is the index of them.

| # | Date | What changed | From | Where |
|---|---|---|---|---|
| 1 | 2026-10-24 | The text body is a `Column` in `verticalScroll`, whole-text selectable, not a `LazyColumn` | tech-lead departure **D10** (`candy-backdrop/tech-lead-architecture.md` R4, 2026-10-17); tech design §3.6 | LS-08; §4 |
| 2 | 2026-10-24 | The Licenses list has no ViewModel; the text screen has one | tech-lead departure **D11** (same report); tech design §3.1's exception | LS-09; §3.3 |
| 3 | 2026-10-24 | Its own branch, `feature/licences-screen`; the Apache-2.0 carve-out travels with it; the backdrop rows belong to the second branch to land | `tech-lead-direction-b.md` **A9, E6, E7** (2026-10-18); tech design §3.6 as corrected 2026-10-21 | frontmatter; LS-02; LS-AC08; §9 item 11 |
| 4 | 2026-10-24 | The Google SDK notices: generated at build time, shown behind one tappable entry as a scrolling list; the largest text one paragraph per lazy item | tech design **§13.1 LN-1 to LN-8** (2026-10-21) | LS-08, LS-09, LS-12; §3.2; §4A; §4B; §7; LS-AC11 to LS-AC13, LS-AC15 |
| 5 | 2026-10-24 | Eight new Open-source software entries; the Google entry renamed and widened; the "one entry per further family" placeholder resolved; U-1's condition superseded | research-analyst rows A–J and the Apache table (2026-10-21); the font report's verdict 2 and open issues 1–3 (2026-10-18); THIRD_PARTY.md §2, §4, §5b | LS-10, LS-11; §3.2, §3.2.1; §6; §7 |
| 6 | 2026-10-24 | Checker Framework credited although nothing of it ships | tech-lead decision in LN-5 item 1 | LS-04's pointer; §3.2 |
| 7 | 2026-10-24 | §5 rewritten as a TalkBack contract for four screens, with the pass line | rules/compose.md; the founder's **Q-023** answer (2026-10-21) and D-TB's shape (`feature/ux-spec-folds`) | LS-13; §5.1; LS-AC06, LS-AC16 |
| 8 | 2026-10-24 | "End of text" closes every text; part headings on two-text entries | this role (a consequence of LS-10 and LS-13) | §4; §4B; §6 |
| 9 | 2026-10-24 | LS-AC03's "every file referenced exactly once" → "at least once, `google/` excluded" | this role (seven entries share `Apache-2.0.txt`; the generated directory has its own test) | LS-AC03, LS-AC12 |
| 10 | 2026-10-24 | The Sounds section's counts confirmed against the audio branch's CREDITS.md; the re-cut rule kept | `docs/audio/CREDITS.md` on `feature/audio-review`; THIRD_PARTY.md §2 | §3.2 |
| 11 | 2026-10-24, loop 1 | LS-AC15's and LS-AC10's lazy-body scroll oracle: scroll the list node with `performScrollToNode`, then assert | check **F-01** | §4B; LS-AC10; LS-AC15; §9 item 7(k) |
| 12 | 2026-10-24, loop 1 | Every entry card merges its own descendants; "through `clickable`" superseded; inert entries one stop each | check **F-02** | LS-06 pointer; §3.1; §5.1; LS-AC06; pass line 3; §9 item 7(f) |
| 13 | 2026-10-24, loop 1 | Nine curated texts, not eight; LS-AC11 names them | check **F-03** | §6; §7; LS-AC05; LS-AC11; LS-AC17 |
| 14 | 2026-10-24, loop 1 | Position in the notices list: intent stated, mechanism owed to the brief; LS-AC13's oracle rewritten (last row against the last index line; "N of N" when delivered) | check **F-04** | §4A; §5.1; LS-AC13; §8.2 item 10; §9 item 7(g) |
| 15 | 2026-10-24, loop 1 | Announcements: "spoken once when it appears, read again only when focus lands on it" | check **F-05** | pass line 6; LS-AC16 |
| 16 | 2026-10-24, loop 1 | LS-AC12: all ten AARs required; the release procedure (`unzip -l` per cached AAR); debug-for-release stated and owed to the brief | check **F-06** | LS-AC12; §9 item 7(h) |
| 17 | 2026-10-24, loop 1 | Q-023 and D-TB/D-TBa cited from this branch (`develop` 4069b53); "the studio ships nothing that TalkBack cannot use" marked as this role's reading of a board-specific answer | check **F-07** | frontmatter; the edition note; §0; LS-13; §5.1 |
| 18 | 2026-10-24, loop 1 | Same-name notice rows carry "Notice n of m"; "the sources line is what tells them apart" superseded | check **F-08** (a UX decision) | LS-12; §4A; §4B; §5.1; §6; LS-AC13; §9 item 7(j) |
| 19 | 2026-10-24, loop 1 | All four Settings section headers are headings, through `SettingsSectionHeader`; owed to the brief | check **F-09** | §2; LS-AC01; §8.2 item 11; §9 item 7(i) |
| 20 | 2026-10-24, loop 1 | The Low findings: the `androidx.*` count (F-10), "then by stem" (F-11), the no-backdrop contrast pairs (F-12), the "Found in" heading withdrawn (F-13), §4B's promise aligned with the gate (F-14), pass line 1 scoped on the text screens (F-15), Jakarta's credit sourced from its NOTICE (F-16), the CREDITS.md branch note (F-17), the LS-11 fall-back completed (F-18) | check **F-10 to F-18** | §3.2.1; §4A; §5.1; §4B; §3.2; §9 item 6 |
| 21 | 2026-10-24, loop 1 | The MPL-2.0 text: CLEAN; §9 item 6 closed; LS-11 stands; full hashes for the MPL text and the NOTICE | `research-analyst-mpl.md` verdicts 1 and 3 | LS-11; §7; §9 item 6 |
| 22 | 2026-10-24, loop 1 | The Public Suffix List entry names the exact version shipped (the PSL commit compiled into okhttp 4.11.0); the live-list address superseded on the entry (it stays in the NOTICE) | `research-analyst-mpl.md` verdict 2 (MPL §3.2(a), "such Covered Software") | §3.2; §6 `licenses_psl_address`; §9 items 8 and 13 |
| 23 | 2026-10-24, loop 1 | The exact-version address is re-derived if U-6 shows another okhttp | `research-analyst-mpl.md` open issue 2 | §9 item 13 |
| 24 | 2026-10-24, loop 1 | Items the tech-lead's brief owes, each with this role's intent: (f)–(k) | the check's handoff item 3 | §9 item 7 |
| 25 | 2026-10-24, loop 2 | LS-AC01's heading oracle asserts the property per section header shown (three or four; never a count); QA's line reads "every section header shown, and the title where it is one" | loop-2 check **F-19**; the brief's A1.4 | §2; LS-AC01; §8.2 item 11 |
| 26 | 2026-10-24, loop 2 | The bundled text screen's title for an unknown entry id is `licenses_title` "Licenses" | loop-2 check **F-20**; the brief's F1 (a UX call made this role's own) | §4; LS-AC14 |
| 27 | 2026-10-24, loop 2 | LS-AC12 (b): the set of distinct sources **equals** the ten AARs; "include" superseded; a new or dropped Google AAR fails the test until a verdict and a row land | loop-2 check **F-21**; the brief's A1.3 | LS-AC12 |
| 28 | 2026-10-24, loop 2 | `UserFacingTextHasNoProtectedNamesTest` is written on this branch (commit 3), with its sixteen-word list; candy owns the list thereafter | loop-2 check **F-22**; the brief's §4.3 | §3.2 "Never listed"; LS-AC17; §6 |
| 29 | 2026-10-24, loop 2 | The entrance stagger is capped at `LicensesUi.STAGGERED_ROWS` = 8 items on both lists, the intro counted; "as it does for every long list in the app" withdrawn | loop-2 check **F-23**; the brief's F6 (a UX number, confirmed) | §3.1; §4A |
| 30 | 2026-10-24, loop 2 | The notice text screen's title is "Notice" for any null header, `Loading(null)` included | loop-2 check **F-24**; the brief's F2 | §4B |
| 31 | 2026-10-24, loop 2 | The generated-asset and hash-pin assertions are Robolectric, the parser and re-flow plain JVM | loop-2 check **F-25**; the brief's F8 | LS-AC05 (By); LS-AC12 (By) |
| 32 | 2026-10-24, loop 2 | The conditionals closed: explicit collection semantics (A1.2); debug-for-release as the Gradle parity task `verifyGoogleNoticesParity` in `check`, LS-AC12 (d) (A1.3); §9 item 7(f)–(k) all answered, with the two consequences (the Play Games statement row one stop; the Licenses headers headings through the shared composable) | loop-2 check **F-26**; the brief's A1.1–A1.6 | §4A; §5.1; LS-AC12; LS-AC13; §9 item 7 |
| 33 | 2026-10-24, loop 2 | LS-AC03: files only and no subdirectory; "`google/` excluded" superseded (it excluded nothing) | loop-2 check **F-27**; the brief's §0.2 | LS-AC03 |
| 34 | 2026-10-24, loop 2 | LS-AC18 split: the developer records the debug APK delta and the `licenses/google/` byte count; the release-manager the release AAB delta at 4.6 | loop-2 check **F-28**; the brief's F5 | LS-AC18 |
| 35 | 2026-10-24, loop 2 | §5's first-edition traversal bullet: Google libraries has two entries | loop-2 check **F-29** | §5 |
| 36 | 2026-10-24, loop 2 | LS-10: "complete by name" → one-to-one by row or family (§3.2.1) | loop-2 check **F-30** | LS-10 |
| 37 | 2026-10-24, loop 2 | LS-AC05's Compose selection clause narrowed to the structural half if Robolectric cannot deliver the gesture; the device half stands | loop-2 check, open issue 3; the brief's §4.1 | LS-AC05 |
| 38 | 2026-10-24, address ruling | A source address is plain text inside its card's one merged node, not selectable; "selectable" and "its `SelectionContainer` adds no stop" superseded; LS-AC06 gains the unmerged-tree oracle | qa-engineer **F-1** (Pixel 9 API 36, TalkBack 16); the developer's fix `2d433b8` | LS-05; §3.1; §3.2 (PSL row, the two Google entries); §5.1 table; LS-AC06 |
| 39 | 2026-10-24, address ruling | **LS-14**: copying an address is deferred past 3.0.0, the loss accepted for this release; the follow-up's shape fixed (long-press, plus the same long-click action "Copy address" by ear; one stop kept); an accessibility-only action rejected as the replacement | this role, on the trade-off the developer's loop-2 report left to it | LS-14; §9 item 15 |
| 40 | 2026-10-24, address ruling | Jakarta's credit confirmed as "The Eclipse Jakarta Dependency Injection project", the NOTICE's attribution line; "Eclipse Foundation" superseded | the developer's deviation 5; the bundled `Jakarta-Inject-NOTICE.md` line 3 | §3.2; §6; §9 item 17 |
| 41 | 2026-10-24, address ruling | The Public Suffix List date's U+2011 hyphens confirmed; a non-blocking by-ear check, with the fix decided in advance (the date in words) | the developer's deviation 6; QA's 200 % screenshots and TalkBack transcript | §6; §8.2 item 12 |
| 42 | 2026-10-24, address ruling | A single token wider than the line (an id, a commit hash) breaks where it overflows: accepted for 3.0.0; the finer break after an id's own hyphen recorded as optional polish with its shape | qa-engineer's §8.2 item 1 note (`play-services-ads-identifier` at 200 %) | §5.1 Text scaling; §8.2 item 1; §9 item 16 |
| 43 | 2026-10-24, device re-run | The notices list's position by ear: every row's position equals its index line number and the last is N; the total after "of" is TalkBack's wording ("over a hundred" for 108 in TalkBack 16) and is not graded; the exact total stays with the Compose half's `rowCount` N; "says 'N of N'" superseded | qa-engineer re-run **O-2** (`tb-notices-linear-light100.txt`) | §4A; §5.1 "Lists are collections"; LS-AC13; §8.2 item 10 |
| 44 | 2026-10-24, device re-run | **LS-15**: heading navigation reaches every heading in both directions, from any stop and at any scroll position, with nothing else between two headings; required on the Licenses list for 3.0.0, with no lazy-list limit accepted; what TalkBack does past the last heading or before the first is the platform's; a card read without its title or as a bare "Button" fails pass line 2 (O-1); pass line 4 restated | qa-engineer re-run **F-4** and **O-1** (`tb-licenses-headings-*.txt`, `tb-jakarta-headings-*.txt`, `t02-licenses-sliver.png`) | LS-15; §5.1 "Headings are headings"; pass lines 2 and 4; LS-AC06; §8.2 item 2; §9 item 18 |
| 45 | 2026-10-24, device re-run | **LS-16**: every text reads in its own direction, so its characters stay in written order in any locale; a list's lines hang from the mirrored start edge, a text screen's body from its left edge; the chrome mirrors; "the default content-based text direction" superseded; new **LS-AC19**; the rest of the app routed | qa-engineer re-run **F-2** (`s23`–`s26`) | LS-16; §4; §4B; §5; §5.1 RTL; LS-AC19; §8.2 item 6; §9 items 18 and 19 |
| 46 | 2026-11-01, LS-15 ruling | **LS-15 restated for a scrolling list** (option F): going down, every heading is reached in order, from the top and from a mid-list entry; a non-heading stop between two headings is accepted only right after a TalkBack page scroll, at most one per scroll, and only as a whole card, title first; going up, and from the title with the list scrolled, recorded and not graded; the non-lazy list kept, and its Compose tests named necessary, not sufficient; "from any stop and at any scroll position … both directions" superseded. Pass line 2's "LS-15's fix is expected to remove it" superseded by the tech-lead's R2 (each card speaks its whole label). The pinned section index (E) and the scrolling title (D) rejected for 3.0.0; no founder question | `qa-engineer-recheck.md` **F-4** (`tb-f4-*.txt`); `tech-lead-f4-ruling.md` **R1** (Compose exposes only on-screen nodes; TalkBack's page scroll) and **R2** | LS-15; §5.1 "Headings are headings"; pass lines 2 and 4; LS-AC06; §8.2 item 2; §9 item 18 |
