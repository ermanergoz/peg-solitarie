# release-manager report — phase-14 store copy (3.0.0 listing, THIRD_PARTY items, pending CHANGELOG)
Date: 2026-10-18
Branch: feature/phase-14-release-docs (22cd54e..22cd54e)
Skills loaded: ergoz-soft:standards (role release-manager; the plugin's 1.0.0 rule files were read in full because the product snapshot is 0.5.1: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy; product `.claude/rules/lessons.md`), gitflow. gsd-complete-milestone was not run: this task is not a milestone close.
Ran in parallel with: phase-15 H-1 code audit, candy-backdrop spec rewrite, tutorial haptics ruling
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- `docs/store/` now exists. It holds the en-US title, short description, full description and the versionCode 30000 release notes, in `fastlane supply`'s layout. It also holds drafted App content answers (Data safety, Ads, Advertising ID, Target audience, Content rating), taken from the live privacy policy and F-5. Every text is within its limit and passes the §3a / A.4 word rules.
- The live listing has a **separate Turkish text** that describes the old app. It needs replacing or removing. Fourteen other languages show translated English; the public page cannot tell whether those translations are saved in the Console or are Play's automatic ones.
- THIRD_PARTY.md: (a) the backdrop idea row's "What was taken" cell is now A.5's text, word for word, and the ship clause is kept. (b) The font rows match `tech-lead-fonts.md`'s replacement text, but **no OFL-1.1 verdict line exists**. The research-analyst must give it. (c) The planned `assets/licenses/Apache-2.0.txt` row is added, marked as shipping from `feature/candy-backdrop`.
- CHANGELOG.md: pending entries, each marked with its branch, for the phase-15 tutorial, Home and gameplay dock, the bundled fonts, the light-theme status bar, the glossy backdrop, the colorblind palette and the Licenses screen.

## Outputs
- `docs/store/README.md`: index; a table of fields, limits and lengths; which lines to delete if a branch misses the release; the word rules and their grep; the languages found on the live listing; the 8 recommended screenshots; the other listing fields.
- `docs/store/en-US/title.txt`: "Peg Solitaire".
- `docs/store/en-US/short_description.txt`, `full_description.txt`, `changelogs/30000.txt`: the copy for Gate 2.
- `docs/store/app-content.md`: drafted answers for Data safety (6 data types, with their sources), Ads, Advertising ID, Target audience (13+), IARC (expected Everyone / PEGI 3), and the other declarations. It also lists two points where the policy says less than the SDK does.
- `THIRD_PARTY.md`: the A.5 cell; the change table (backdrop direction B, new Licenses-screen row); the §2 intro; the OFL verdict note and "pending" in both font rows' How-verified cells; the new Apache-2.0.txt row; a 2026-10-18 update to the §7 licences-screen bullet.
- `CHANGELOG.md`: the Sources line now reads phases 6–15; a note on what the pending marker means; 3 Added, 4 Changed, 1 Fixed and 1 Under-the-hood pending entries.

Character counts (Unicode characters, without the final newline):

| Text | Limit | Count |
|---|---|---|
| Title | 30 | 13 |
| Short description | 80 | 76 |
| Full description | 4,000 | 2,222 |
| Release notes (en-US, versionCode 30000) | 500 | 427 |

Words check: no candy, crush, saga, sugar, sweet, jelly, soda, King, gel or twist; no comparison or "games like"; no callouts. "Solitaire" appears only as "Peg Solitaire" (title.txt line 1, full_description lines 1 and 35, release notes line 1), never next to a sweet word or a "twist" phrase. No cards and no felt.

## Checks
- `grep -inE 'candy|crush|saga|sugar|sweet|jelly|soda|king|gel|twist|tasty|delicious|divine|like |inspired|fans of|style|activision|microsoft|tiffi|toffee|yeti|odus|cards?|felt' docs/store/en-US/*.txt docs/store/en-US/changelogs/*.txt` — pass (no output, exit 1)
- `python3 -I -c '… len(open(f).read().rstrip("\n")) …'` over the four texts — pass (13 / 76 / 2,222 / 427, each within its limit)
- `python3 -I` comparison of THIRD_PARTY.md's idea row against A.5's fenced cell in `PegSolitarie-candy/.planning/reports/candy-backdrop/research-analyst.md` — pass (cell matches A.5: True; ship clause kept: True; 5 columns)
- `curl -sL https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html` — pass (200, 10,972 bytes, "Last updated: September 21, 2026"; read in full)
- `curl -sL 'https://play.google.com/store/apps/details?id=com.erman.pegsolitarie&hl=<16 languages>&gl=US'` — pass (en-US is the old text, updated Feb 18, 2026; tr has its own text; the other 14 are translated English; category GAME_PUZZLE; rating "Everyone")
- `curl -sL https://developers.google.com/admob/android/privacy/play-data-disclosure` — pass (200; the legacy SDK's list was read: IP address, product interactions, diagnostics, device/account IDs, TLS)
- Claims checked against code on `feature/phase-15-ux-refresh`: `GeneratorConstants.MAX_CHALLENGE_LEVEL = 10_000` (the "10,000 levels" claim); the `LevelGenerator` KDoc "solvable by construction" (reverse generation); `isSystemInDarkTheme` themes; `strings.xml` board names and the tutorial captions; commit a62194f for the status-bar fix — pass
- `git -C …/PegSolitarie-r5 commit` (explicit paths) — pass (22cd54e)
- No Gradle, build, upload, approval file or Console access — as dispatched.

## Open issues / risks
1. **OFL-1.1 verdict missing (blocks this branch's merge).** `tech-lead-design-sync.md` item 5 and `tech-lead-signoff.md` (Decision 4, row 3) both require the line. The research-analyst gives it. The release-manager did not write it, because it is a licensing verdict. THIRD_PARTY.md §2 marks it pending.
2. **The Turkish listing describes the old app** ("5 farklı oyun modu"). It needs a founder decision: write it, have the studio draft tr-TR, or delete the translation. The founder also checks the Console's Translations page for saved machine translations of the old English text.
3. **Text tied to branches.** These lines depend on `feature/phase-15-ux-refresh` and `feature/candy-backdrop`, and the second is not built yet. Lines to delete if a branch misses: `docs/store/README.md`, "What the texts describe". Matching CHANGELOG entries carry *(pending: …)*.
4. **Data safety is a draft against the legacy SDK's disclosure page.** The founder re-reads Google's Next-Gen ("Android") list before filling the form. The Play Games rows say "not shared". If the Console's help text is read the other way, tick "shared" (the safe direction).
5. **The `AD_ID` permission comes from the SDK's manifest merge.** It is confirmed from the release candidate's merged manifest at the release branch, not here.
6. **Two small policy gaps (non-blocking, F-6).** The policy does not name the app set ID; it covers diagnostics only through "ad interaction and diagnostic data".
7. **Play Console-only names.** Achievement and leaderboard names live only in the Console. The founder checks them against A.4's words (A.6 item 4 of the research-analyst's addendum).
8. **Commit trailer.** `rules/gitflow.md` requires `Co-Authored-By: Claude Fable 5.1`, and that is what the commit carries. The work ran on Opus. This checkout has no prepare-commit-msg hook.

## Handoff
Next: producer —
- **research-analyst:** the OFL-1.1 verdict line for THIRD_PARTY.md §2. Also the non-blocking one-line confirmation of the Apache-2.0 text's redistribution terms (`licences-screen.md` §7).
- **Founder (queue, non-blocking):** the Turkish listing and the Console translations (open issue 2); entering F-5 from `docs/store/app-content.md`; approving the copy at Gate 2 (Q-016).
- **release-manager (at `release/3.0.0`):** remove the pending markers or delete entries per branch; capture the screenshots from the release candidate; read `AD_ID` from the merged manifest.
