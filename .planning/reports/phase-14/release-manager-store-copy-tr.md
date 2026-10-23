# release-manager report — phase-14 store copy, tr-TR (Q-022)
Date: 2026-10-21
Branch: feature/phase-14-release-docs (9071999..9071999)
Skills loaded: ergoz-soft:standards (plugin rules v1.0.0, read in full because the product snapshot is 0.5.1: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md; product .claude/rules/lessons.md)
Ran in parallel with: phase-15 H-1 test fix (another worktree)
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none

## Summary
Drafted the Turkish (tr-TR) Play listing for 3.0.0 in `docs/store/tr-TR/`, in the same fastlane layout as en-US. It translates the en-US title, short and full descriptions and the 30000 release notes. All four are within Play's limits. The title stays "Peg Solitaire", because the live Turkish listing uses that title too (public page `hl=tr&gl=TR`, read 2026-10-21). The research-analyst's word rules (§3a, A.4) hold in Turkish: both greps print nothing. `docs/store/README.md` now lists tr-TR, gives the Turkish grep and says which tr-TR lines depend on unmerged branches. Documentation only: no Gradle, no build, no upload, no Console.

## Outputs
- docs/store/tr-TR/title.txt — "Peg Solitaire" (13 / 30)
- docs/store/tr-TR/short_description.txt — 71 / 80
- docs/store/tr-TR/full_description.txt — 2,521 / 4,000
- docs/store/tr-TR/changelogs/30000.txt — 447 / 500
- docs/store/README.md — tr-TR rows in the file table, a tr-TR note, a tr-TR branch-dependency table, the Turkish word rules and greps, and an updated "Languages on the live listing" bullet

Lengths count Unicode characters without the final newline (`python3`, `len()`), as for en-US. title and short have no final newline, like en-US.

Translation choices:
- I used the founder's own words from the live Turkish text: piyon (peg), delik (hole), tahta (board), masa oyunu (board game). I wrote in the formal "siz" register, as the live text does.
- The app has no Turkish strings: no `values-*` locale on develop, main, phase-15-ux-refresh or candy-backdrop. So button names carry the app's English label in brackets: "Klasik (Classic)", "Meydan okuma (Challenge)", "İpucu (Hint)", "Geri al (Undo)", "Ayarlar (Settings)".
- I added one line that is not in en-US: "Oyunun arayüzü İngilizcedir." ("The game's interface is in English."). It keeps a Turkish listing from suggesting a Turkish app. The founder may delete it.
- Google's Turkish UI terms: "başarım" (achievement), "skor tablosu" (leaderboard).
- Ads line: "after you tap Continue" became "bir seviyeyi bitirip sonuç ekranından devam ettiğinizde" ("when you finish a level and continue from the results screen"). The interstitial fires on the Results screen's continue tap (`ResultsViewModel.dispatchContinue`, develop). That button's label is "Next level", not "Continue", so naming no label is accurate.

## Checks
- `curl -sL -A 'Mozilla/5.0' 'https://play.google.com/store/apps/details?id=com.erman.pegsolitarie&hl=tr&gl=TR'` — pass (`itemprop="name">Peg Solitaire`; the description is the old "Peg Solitaire, kendi kendine oynanır…" text, updated 18 Şub 2026). So the founder's Turkish title is "Peg Solitaire".
- `python3 -I -c '… len(open(f).read().rstrip("\n")) …'` on docs/store/tr-TR/**/*.txt — pass (13, 71, 2521, 447; limits 30, 80, 4000, 500)
- `LC_ALL=en_US.UTF-8 grep -inE 'candy|crush|saga|sugar|sweet|jelly|soda|king|gel|twist|tasty|delicious|divine|like |inspired|fans of|style|activision|microsoft|tiffi|toffee|yeti|odus|cards?|felt' docs/store/tr-TR/*.txt docs/store/tr-TR/changelogs/*.txt` — pass (no output, exit 1). The README's en-US grep, run on tr-TR. "gel" is avoided even inside Turkish words.
- `LC_ALL=en_US.UTF-8 grep -inE 'şeker|seker|bonbon|lolipop|karamel|çikolata|şurup|tatlı|tatli|jöle|jole|jel|soda|ezme|ezil|ezer|destan|kral|lezzet|nefis|enfes|leziz|ilahi|şahane|dokunuş|farklı bir yorum|bükül|gibi|benzer|esinlen|ilham|hayran|tarz|kart|iskambil|keçe|çuha|yeşil masa' docs/store/tr-TR/*.txt docs/store/tr-TR/changelogs/*.txt` — pass (no output, exit 1). The Turkish equivalents of §3a and A.4.
- `python3 -I` Turkish-aware check (İ→i, I→ı, then lower(), substring test for the same English and Turkish word lists) — pass (prints only "done"). This also covers capitals that grep -i might miss in Turkish.
- `grep -n 'Solitaire' docs/store/tr-TR/…` — pass ("Solitaire" appears only in "Peg Solitaire": the title, the release-notes opening, and the description's first line and ads paragraph. None is next to a sweet word or a twist phrase.)
- Not run: `./gradlew check`. This is a documentation-only task, and the dispatch excludes Gradle.

## Open issues / risks
- **The colorblind setting label was not found.** No `strings.xml` on develop, feature/candy-backdrop or feature/phase-15-ux-refresh contains "colorblind", "colour-blind" or "palette". The en-US and tr-TR "colorblind palette" lines and the en-US README's claim that the app labels the setting "colorblind" depend on that setting landing. If it does not reach `release/3.0.0`, delete those lines from both languages (README dependency tables).
- **en-US wording to check (not changed; it is not in this task's scope):** the en-US ads line says "after you tap Continue", but the Results button is labelled "Next level". The producer may want the en-US line reworded to match.
- Turkish fluency should get a native read. The founder is the natural reader at Gate 2; no studio role reviews Turkish.
- The Console's tr-TR translation must be replaced with these files (fastlane `supply` uploads `tr-TR/` as is). Otherwise the old Turkish copy stays live.

## Handoff
Next: producer — put `docs/store/tr-TR/` in front of the founder at Gate 2 next to en-US, together with the optional "Oyunun arayüzü İngilizcedir." line. At the release cut, delete the branch-dependent tr-TR lines together with the en-US ones (README tables). Consider rewording the en-US "tap Continue" line.
