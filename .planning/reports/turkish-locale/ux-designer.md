# ux-designer report — turkish-locale
Date: 2026-11-01
Branch: feature/turkish-locale (committed by the producer)
Skills loaded: ergoz-soft:standards (rules read in full from the plugin, since the snapshot is 0.6.2 against plugin 1.0.0: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, dispatch.md; the product's .claude/rules/lessons.md via CLAUDE.md; .planning/recall/RECALL.md does not exist in this worktree), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: feature/home-continue-live-boards (not read; reconciliation rule in the spec §9.3)
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none (one read-only Explore helper on haiku for the directory listings and greps this role's tools lack: ux-spec headings and slot rules, store files, font sizes, manifest and Gradle locale settings, case-transform and format calls)
Loops: 0
Lessons applied: none

## Summary
Wrote the complete Turkish localisation spec: register (siz in sentences, bare imperative on commands, matching the tr-TR listing and Android's own Turkish), glossary aligned with the listing, proper-name rules (board names and the title stay English), the full translation table for every key of values/strings.xml on develop 37d3052 (about 179 strings and 7 plurals in values-tr; 9 more keys become translatable="false"), a truncation watchlist with shorter alternatives, build and QA locale rules, and Given/When/Then acceptance criteria per screen.
Two flags outside the copy: the bundled Fredoka files (~50 KB each) may be Latin-only subsets lacking İ, Ğ, ğ, Ş, ş — the developer must prove glyph coverage before QA; and the tr-TR store description currently says the interface is English, which becomes false with this branch.
Per-app language: follow the system plus android:localeConfig (en, tr); no in-app picker in 3.0.0.

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-tr/docs/ux/localisation-tr.md — the spec: §1 decisions D-1..D-14, §2 register and style, §3 glossary, §4 proper names, §5 translation table (5.1 gameplay/Home/catalog/controls, 5.2 TalkBack board/banner/Undecided notice/Results, 5.3 Settings/ribbon/Continue/tutorial/ads/privacy, 5.4 Play Games/cloud/licences, 5.5 plurals, 5.6 count), §6 truncation watchlist, §7 locale rules (casing, numbers, fonts, resource layout and per-app language, TalkBack, tests, what stays English), §8 acceptance criteria, §9 open items and proposed founder questions, §10 change log
- /Users/ermanergoz/Desktop/PegSolitarie-tr/.planning/reports/turkish-locale/ux-designer.md — this report

## Checks
- `./gradlew check` — not run (this role has no Bash and the change is docs-only; the developer runs it once values-tr exists, where lint's MissingTranslation/ExtraTranslation/MissingQuantity and the parity test of spec §7.6 become the gate)
- Hand walk of every `<string>`/`<plurals>` in app/src/main/res/values/strings.xml against spec §5.1–5.5 — pass (every translatable key has a row; the 35 keys already translatable="false" and the 9 that become so are listed in §4/§5.4 and excluded)
- Format-argument check by hand on every row with `%` — pass (indices and conversions preserved; the two ad bodies keep `%1$d` twice as in English; `catalog_stars_earned`, `board_a11y_action_jump` and `board_a11y_announce_hint` reorder by positional index only)
- Spec §4.4.2 Turkish copy of record for `hint_undecided_message` reproduced verbatim in §5.2 — pass ("İpucu bu konumu süresi içinde çözemedi. Belki hâlâ kazanılabilir.")

## Open issues / risks
- Fonts (spec D-13, §7.3): fredoka_medium.ttf and fredoka_semibold.ttf are 49,932 and 49,900 bytes; Google Fonts' "latin" subset range has ı (U+0131) but not İ, Ğ, ğ, Ş, ş. If the files are subsets, Turkish words render with fallback glyphs. Not confirmable from this seat; the developer proves coverage of all five TTFs (cmap for 13 code points listed in §7.3) and swaps in the full OFL files if needed. QA criterion C-2.
- Store copy (D-14): docs/store/tr-TR/full_description.txt line 37 "Oyunun arayüzü İngilizcedir." must be deleted; the two 3.0.0 changelogs gain a Turkish-interface line; docs/store/README.md lines 25–26 say there are no Turkish strings. Release-manager's files — producer routes.
- Six slots run long (spec §6): the two Continue tile titles, "KALAN PİYON", the Results action rows, the rescue banner message and its "Yeniden başlat" button, the ad-offer "Hayır, teşekkürler". None clips by construction (the slots wrap), but at 200 % some stack; §6 names the shorter alternative for each and the rule for switching (only on a device finding, recorded in the developer's report).
- TalkBack's Turkish hint template is assumed to be "<label> için iki kez dokunun", so the three tap-action labels are infinitives; criterion A-7 verifies it and the fallback is three strings (§7.5).
- The parallel branch may add or remove keys; §9.3 gives the reconciliation rule (new keys come to the ux-designer in review mode for a one-line ruling; the spec's §5 gets the row in the same commit).
- Noted, unchanged: the Results score is ungrouped in both languages; GameTimerFormat formats in the default locale (harmless for en/tr); both store listings say "Asymmetric-39" where the app says "Asymmetric 3-3-2-2" in both languages (pre-existing).
- Proposed founder questions, non-blocking, drafted in spec §9.1 in FOUNDER.md format for the producer to queue: Q-A register (siz, recommended, vs sen), Q-B naming "Solo Test" in the tr-TR listing after a research-analyst trademark verdict (recommended if clean). The build proceeds on the recommendations.

## Handoff
Next: producer — read and commit docs/ux/localisation-tr.md and this report on feature/turkish-locale (`docs(ux): specify the Turkish localisation for 3.0.0` or similar, ≤ 72 chars); queue Q-A and Q-B from spec §9.1; route §9.2 to the release-manager (store copy), the research-analyst ("Solo Test" mark) and the tech-lead (localeConfig/localeFilters sign-off); then dispatch android-game-dev with the spec.
Next: android-game-dev — build from the spec: (1) `app/src/main/res/values-tr/strings.xml` from §5.1–5.5 verbatim (escape `'` as `\'`; same key order and comments as the base; both plural items); (2) mark the 9 keys of §4 `translatable="false"` in the base file; (3) prove font glyph coverage per §7.3 and replace any subset with the full OFL file; (4) `android:localeConfig` with `res/xml/locales_config.xml` (en, tr) or AGP `generateLocaleConfig`, and `localeFilters`/`resourceConfigurations` for en+tr, both for the tech-lead's sign-off (§7.4); (5) no case transform on displayed text (§7.1); (6) tests of §7.6: the JVM parity test, `UserFacingTextHasNoProtectedNamesTest` extended to values-tr, the three per-screen UI tests under a tr configuration, Roborazzi goldens in tr at font scale 1.0 and 2.0 for the §6 slots; (7) `./gradlew check` green; (8) reconcile the delta from feature/home-continue-live-boards per §9.3. Any §6 alternative used is named in the developer's report.
Next: qa-engineer — the device session in Turkish per spec §8 (cross-cutting C-1..C-7, then per screen), at 100 % and 200 % font scale on a 360×640dp window; the TalkBack pass A-1..A-7; screenshots of any slot that stacks or wraps for the §6 decision.
