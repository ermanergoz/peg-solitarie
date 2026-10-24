---
role: ux-designer
task: candy-backdrop (companion item: the in-app licences surface)
slug: licences-screen
status: draft
created: 2026-10-17
stack: android-compose-game
design-system: docs/ui/DESIGN-SYSTEM.md
sources: THIRD_PARTY.md (feature/phase-14-release-docs), docs/ux-spec.md §4.5, ui/settings/SettingsScreen.kt
companion: docs/ux/candy-backdrop.md
---

# Peg Solitaire — In-app licences surface

> rules/ip-policy.md asks for attribution "in THIRD_PARTY.md **and** in the product's licenses screen
> or page". The app has neither a screen nor a page (THIRD_PARTY.md §7: "No in-app licences screen
> exists"). This document specifies the screen: a **Licenses** row in a new Settings **About** section,
> a list of what ships with its licence, and a reader for the licence texts the app bundles. It is a
> design document; it contains no code.

## 0. Scope and inputs

**Specified here:** the Settings entry point, the Licenses screen, the License text screen, what they
list and what they never list, their copy, accessibility, assets and acceptance criteria.

**Inputs:** `THIRD_PARTY.md` on `feature/phase-14-release-docs` (§1 ideas, §2 assets, §4 direct
dependencies, §5b families, §6 unverified rows, §7, §8); `SettingsScreen.kt` and `strings.xml` on
`feature/phase-15-ux-refresh`; the bundled `assets/licenses/Fredoka-OFL.txt` and `Nunito-OFL.txt`;
the candy-backdrop verdict's §3a and §6; `docs/ux-spec.md` §4.5 and §11.

**Not in scope:** editing THIRD_PARTY.md (the release-manager's), adding a dependency to generate the
list (the tech-lead's, LS-03), a developer-website licences page (founder-only work; the in-app screen
satisfies "screen or page" on its own).

**Spelling.** User-facing copy is US English — "Licenses" — because `res/values/` is the app's en-US
default for Play. This document and THIRD_PARTY.md keep British spelling.

## 1. Decisions

- **LS-01: The entry is a "Licenses" row in a new "About" section of Settings, directly above
  Privacy.** — Privacy stays pinned last (Phase 9 D-14: new rows go above it). Licences are not a
  privacy matter, and General holds things the player adjusts plus the tutorial; an About group is the
  platform's convention for this row. Inserting a section above Privacy is a renumbering of the entrance
  orders, which `SettingsScreen.kt`'s own KDoc anticipates (§2).
- **LS-02: Two screens, both Settings children:** *Licenses* (the list) and *License text* (one bundled
  licence, full text). Both carry the backdrop's Sparse density (`docs/ux/candy-backdrop.md` §2).
- **LS-03: A hand-curated, static list that mirrors THIRD_PARTY.md's shipped rows — not a generated
  dependency dump.** — It names things the way a player recognises them (a font, a sound pack, "AndroidX
  and Jetpack Compose") rather than Maven coordinates; it is offline, deterministic and testable; it
  matches THIRD_PARTY.md's family rows one for one. Considered: Google's `oss-licenses` plugin (a Play
  services activity in its own, non-Candy-Clay look, and another Google library); AboutLibraries (a
  Gradle plugin and a runtime library). Either could replace the data source later on the tech-lead's
  ruling without changing this design.
- **LS-04: Listed = every third-party item that ships and carries a licence or a credit; never the
  idea rows, own work, or anything that does not ship** (§3.2). The candy-backdrop idea row is excluded
  by name (its own Credit cell and the verdict's §6): this screen never names King, Candy Crush, or the
  word "candy" (verdict §3a).
- **LS-05: The screens never leave the app.** Full texts the app bundles open in-app; licences the app
  does not bundle (CC0, the Android SDK terms) are shown by name with their source address as
  selectable text, not as a link. — The privacy policy stays Settings' one outbound link, and these
  screens work in airplane mode.
- **LS-06: Tappable and inert entries look different by shape, not colour.** An entry that opens a text
  has a trailing chevron and squishes on press; an entry with nothing to open has neither and is inert
  (no click action, no focus stop as a control) — the `SettingsLinkRow` WR-06 pattern.
- **LS-07: Licence texts re-flow, and are never edited.** Hard line breaks inside a paragraph are joined
  so the text fits the screen at any font scale; blank lines separate paragraphs; no word is changed,
  added or removed.

## 2. Entry point — Settings

| Element | Spec |
|---|---|
| Section header | `settings_section_about` "About" — `SettingsSectionHeader` (`labelLarge`, `onSurfaceVariant`), `semantics { heading() }` |
| Row | a `SettingsLinkRow`: label `settings_licenses_label` "Licenses", supporting `settings_licenses_supporting` "Fonts, sounds and open-source software", **no trailing icon** (it opens in-app — the same rule that leaves the manage-choices row bare and gives the browser-bound policy row its glyph), `onClick` → Licenses |
| Sound | `withClickSound(SoundCue.ButtonForward, …)` at the call site — a navigational CTA (D-09), exactly like "How to play" |
| Position | after the Play Games section (or after General when Play Games is absent, D-11) and before Privacy |
| Entrance orders | renumbered top to bottom, contiguous: `ABOUT_HEADER_ORDER` 8, `LICENSES_ROW_ORDER` 9, then `PRIVACY_HEADER_ORDER` 10, `PRIVACY_POLICY_ROW_ORDER` 11, `PRIVACY_CHOICES_ROW_ORDER` 12 — a whole renumbering, so `SettingsScreenTest`'s rendered-top check passes |

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

### 3.2 What it lists

Contents follow THIRD_PARTY.md **as it stands on the release branch**; the rows below are its 3.0.0
rows today. Where THIRD_PARTY.md changes, this list changes with it (LS-AC03).

| Section | Entry title | Lines | Opens |
|---|---|---|---|
| **Fonts** (`licenses_section_fonts`) | Fredoka | "© 2016 The Fredoka Project Authors" · "SIL Open Font License 1.1" | `assets/licenses/Fredoka-OFL.txt` |
| | Nunito | "© 2014 The Nunito Project Authors" · "SIL Open Font License 1.1" | `assets/licenses/Nunito-OFL.txt` |
| **Sounds** (`licenses_section_sounds`) | Interface Sounds by Kenney | "7 sound effects" · "CC0 1.0 Universal (public domain)" · "kenney.nl/assets/interface-sounds" | — (inert) |
| | “Pop 01” by LilMati | "2 sound effects" · "CC0 1.0 Universal (public domain)" · "freesound.org/people/LilMati/sounds/506546" | — |
| | “Positive Blip Effect” by CogFireStudios | "1 sound effect" · "CC0 1.0 Universal (public domain)" · "freesound.org/people/CogFireStudios/sounds/531512" | — |
| **Open-source software** (`licenses_section_software`) | AndroidX and Jetpack Compose | "The Android Open Source Project" · "Apache License 2.0" · "Compose, Material 3, Material icons, Room, DataStore, Navigation, Lifecycle, Activity, Core, Hilt Navigation" | `assets/licenses/Apache-2.0.txt` |
| | Kotlin and kotlinx | "JetBrains" · "Apache License 2.0" · "Standard library, coroutines, serialization" | the same text |
| | Dagger and Hilt | "Google" · "Apache License 2.0" | the same text |
| | *one entry per further §5b family the release branch's `releaseRuntimeClasspath` listing confirms* (THIRD_PARTY.md §0, checklist step 6) — e.g. small annotation libraries; an MIT one bundles its own notice as `assets/licenses/<name>-MIT.txt` and opens it | as the row | its text |
| **Google libraries** (`licenses_section_google`) | Google Play services libraries | "Play Games Services, Mobile Ads, User Messaging Platform" · "Android Software Development Kit License Agreement" · "developer.android.com/studio/terms" | — (inert) |

**The Kenney entry** credits "Kenney" in words only — never Kenney's logo (their terms). The sound counts
are THIRD_PARTY.md §2's: Kenney seven (`sfx_jump`, `sfx_peg_remove`, `sfx_undo`, `sfx_win`,
`sfx_btn_forward`, `sfx_btn_dismiss`, `sfx_sound_enabled`), LilMati two (`sfx_select` and its derivative
`sfx_deselect`), CogFireStudios one (`sfx_star_pop`). Those rows describe the WAV set on
`feature/audio-review`; if that branch does not reach the release, the Sounds section is re-cut with
THIRD_PARTY.md.

**The User Messaging Platform** is named in the Google entry only once THIRD_PARTY.md's U-1 has
settled its licence; until then the entry lists Play Games Services and Mobile Ads.

**Never listed** (LS-04): every THIRD_PARTY.md §1 idea row — the traditional game, the board
geometries, the founder's earlier repository, and the candy-backdrop row, whose Credit cell excludes it
from the app; own work (the two in-house sounds, the launcher and cover art, the opening books, every
drawing, the backdrop); `font_certs.xml` and `ui-text-google-fonts` (they do not ship in 3.0.0); every
test-, debug-, compile- and build-only item (§8). The words King, Candy Crush and candy appear nowhere
on either screen.

### 3.3 States

The list is static: no loading, empty or error state exists for it.

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

## 5. Accessibility

- **Touch:** tappable entries ≥ 72dp and full-card; back 48dp; ≥ 8dp between cards (`spacing_md`).
- **Labels and roles:** each entry is one merged node read title first; a tappable entry has
  `Role.Button` and the click label `licenses_read_action` "Read the license"; an inert entry has no
  click action and no button role. The chevron is decorative. Section headers and the text screen's
  title carry `heading()`.
- **Traversal (Licenses):** back → title → intro → Fonts → Fredoka → Nunito → Sounds → its three entries
  → Open-source software → its entries → Google libraries → its entry.
- **Not colour alone:** tappable vs inert is the chevron plus the press response (LS-06).
- **Contrast:** card text uses the design system's verified pairs (`onSurface` / `onSurfaceVariant` on
  `surfaceContainerHigh`); text on the backdrop (intro, headers, licence paragraphs) is on Sparse's zone
  T: `onSurfaceVariant` ≥ 5.0:1 light and ≥ 6.9:1 dark (`docs/ux/candy-backdrop.md` §5.1).
- **Text scaling:** everything is an M3 type role and wraps; nothing truncates; both lists scroll; at
  200% a card simply grows.
- **Dark theme, RTL:** both screens use only theme roles; layout mirrors in RTL.
- **Reduced motion:** the entrance stagger and the squish are already gated by
  `rememberAnimationsEnabled()`.
- **Sound:** `ButtonForward` on the Settings row and on opening a text (D-09); back is silent; sound is
  never the only feedback — every press also navigates visibly.

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

The candy-backdrop words grep (BD-01) runs over these strings too and must print nothing.

## 7. Assets and licences

| Asset | Status | Licence | Record |
|---|---|---|---|
| `assets/licenses/Fredoka-OFL.txt`, `Nunito-OFL.txt` | ship today on `feature/phase-15-ux-refresh` | OFL-1.1 notice files | THIRD_PARTY.md §2 font rows |
| `assets/licenses/Apache-2.0.txt` | **new**: the verbatim Apache License 2.0 text (apache.org/licenses/LICENSE-2.0.txt) — §4(a) of that licence asks that recipients receive a copy of it | the licence document's own terms | a THIRD_PARTY.md row, and a one-line confirmation of the text's own redistribution terms by the research-analyst (non-blocking) |
| `assets/licenses/<name>-MIT.txt` | only if the release listing confirms an MIT-licensed artifact ships | MIT notice | its THIRD_PARTY.md family row |
| `Icons.AutoMirrored.Rounded.KeyboardArrowRight`, `ArrowBack` | `material-icons-core`, already a dependency | Apache-2.0 | already covered by the AndroidX family |

No raster, no new dependency, no permission.

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
