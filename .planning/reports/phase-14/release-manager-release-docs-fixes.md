# release-manager report — release-docs-fixes (code-auditor FAIL on `feature/phase-14-release-docs`)
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer). Worked on head `ab12bb8` in `PegSolitarie-r5`; facts checked against `develop` 0582e3f in `PegSolitarie-dev`, read-only.
Skills loaded: ergoz-soft:standards (role release-manager). The product snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies of studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android and compose in full. puzzle-design, play-store and ip-policy are byte-identical in both copies. I also read `.claude/rules/lessons.md`. `.planning/recall/RECALL.md` does not exist. I did not invoke gitflow or gsd-complete-milestone, because this pass cuts no branch, makes no commit and closes no milestone.
Ran in parallel with: the tech-lead editing `docs/tech-design.md` in this worktree (F-8 and the tech-design side of F-1); a research-analyst writing the F-2 licence report
Ran on: claude-opus-5-5 at effort medium (policy)
Fan-out: none
Loops: 1 (the second pass on this branch, after `.planning/reports/phase-14/code-auditor-release-docs.md` FAIL)
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- I fixed F-2 (the claims only), F-3, F-4, F-5, F-6 and F-7, the `CHANGELOG.md:139` factual slip from F-1, and I-1 and I-2. Everything is a text edit in the five files I was allowed to touch.
- `THIRD_PARTY.md` no longer claims a complete record or that no attribution obligation is unmet. It names the families that are known to be missing and points to tech design §16 #20. I added no new dependency rows; they come with the research-analyst's verdict.
- The release checklist gains five items: 1.7 (upload script), 2.4a (pending-branch clean-up), 3.5a (NOTICE scan), 3.5b (Google SDK POMs) and 4.6a (fonts in the AAB, AC-F5). Item 5.9 now refers to the bundled fonts. Every "step 6" and "step 1.4" reference is retargeted.
- `scripts/play-upload.sh` is now described as owed, with its owner. I did not create it.
- F-1's store copy is untouched and waits for the game-designer's ruling.

## Outputs
- `THIRD_PARTY.md` — F-2 claims, F-3 cross-references, F-4 line citations, I-1, I-2.
- `CHANGELOG.md` — F-1 count slip, F-3 reference, F-5 markers, font claim and commit count.
- `docs/release-checklist.md` — F-3, F-6, I-1.
- `docs/store/README.md` — F-3 reference, F-5, F-6.
- `docs/ad-testing-safety.md` — F-7.
- `.planning/reports/phase-14/release-manager-release-docs-fixes.md` — this report.

### Per finding

| Finding | File:line changed (after the edit) | How verified |
|---|---|---|
| F-1 (count slip only) | `CHANGELOG.md:140`: "the first four moves (three on Wiegleb-45)". The "proven" promise at `:137-141` and the four store texts are **not** changed | `OpeningBook.kt:120,129,159-163` on develop: `BOOK_DEPTH = 4`, `WIEGLEB_BOOK_DEPTH = 3`. The header of `golden/book/wiegleb45-k3.txt` reads `k=3` |
| F-2 (claims only) | `THIRD_PARTY.md:59-66` (§0 no longer says "licence-complete by publisher"); `:226-239` (the §5b open item now says the table is incomplete and blocks the release, names the missing families, `hsdp` and the Google `third_party_licenses.txt`, and points to §16 #20); `:250` (U-6 marked known-incomplete); `:341-347` (§7 no longer says "every dependency is Apache-2.0 or a permitted Google SDK" or "nothing shipped carries an unmet attribution obligation"). No dependency rows added | Family list taken from `.planning/reports/phase-14/research-analyst-font-licences.md` open issues 1–2. Wording matched against `docs/tech-design.md:1108` (§16 #20: owners, and a deadline before `release/3.0.0`). Guava and checker-qual are worded as named in §5b but unverified, because the existing row at `:224` already lists `com.google.guava:*` and `org.checkerframework:*` |
| F-3 | `docs/release-checklist.md:34` (1.4 also requires the §5b open item and §16 #20 to be closed); `:52` new 2.4a (pending-branch clean-up); `:71-73` 3.5 extended, new 3.5a (NOTICE scan, including the 51 unscanned coordinates) and new 3.5b; `:88` new 4.6a (AC-F5: `bundletool dump resources` for five fonts, `unzip -l` for two OFL texts); `:132` 5.9 now names the bundled fonts. References retargeted: `THIRD_PARTY.md:10,25,86,147,148` (the old 1.4 and step-6 references), `CHANGELOG.md:47`, `docs/store/README.md:41` | AC-F5's release half is quoted from `.planning/reports/15-founder-feedback/tech-lead-fonts.md:151` on develop. The five font names and two texts come from `ls app/src/main/res/font app/src/main/assets/licenses` on develop. Step 6 is merge/tag (`release-checklist.md` §6). `grep -rn 'step 6\|step 1\.4\|checklist 1\.4'` over my five files now returns 0 hits |
| F-4 | `THIRD_PARTY.md:39` → `app/build.gradle.kts:478-547`; `:42` → `:500-505`, both pinned to develop 0582e3f | `grep -n 'implementation\|compileOnly' app/build.gradle.kts` on develop gives the first `implementation` at 478 and the last at 547. The comment spans 500-504 and `compileOnly` is at 505 |
| F-5 | `CHANGELOG.md:57,122,126,130,149,174`: the phase-15 *(pending)* markers are removed. `:174-176`: the bullet is reflowed. `:178-180`: "downloadable-font certificates" is removed, so the bullet now names only sounds and books as proved, and the font check moves to the release branch. `:11-13`: 810 commits and 154 `feat` subjects, dated, re-counted at item 1.6. `docs/store/README.md:41-45`: phase 15 is no longer conditional, and both of its table rows are removed. `:114`: the screenshot timing is updated. `docs/release-checklist.md:7-8`: "exactly three places" becomes "every item flagged [F]" (the file has 11) | `git log -1 c9228a9` = "Merge branch 'feature/phase-15-ux-refresh' into develop". `git rev-list --count v1.0..develop` = 810. `git log --no-merges --format=%s v1.0..develop \| grep -cE '^feat'` = 154 (the old text said 132). `git ls-files app/src/main/res` on develop holds no `font_certs.xml`. `feature/candy-backdrop` (+8) and `feature/audio-review` (+24) are still unmerged, so their markers stay |
| F-6 | `docs/release-checklist.md:37` new 1.7 (the script is on develop and reviewed, or the producer has recorded a Console upload); `:58-59` the release-branch scope now includes `CLAUDE.md`'s Build command line; `:93-106` the old "belt and braces" sentence is replaced by "does not exist yet", with the owner (tech-lead assigns the android-game-dev on a `feature/*` branch off develop, code-auditor review, tech-lead sign-off, routed by the producer, before Gate 2) and the interim route (the founder's Console upload, operator checklist Step 8 item 1). The same paragraph now names `feature/publish-<X.Y.Z>-done` as the upload checkout, not "a develop checkout". `docs/store/README.md:5-8` says the same | `ls scripts` on develop: "No such file or directory". The script's specification is from `14-RELEASE-PLAN.md` item 2 (`:278-282`). Step 8 item 1 of `14-OPERATOR-CHECKLIST.md` on develop has the founder upload in the Console. The upload branch follows the release-manager procedure, step 8 |
| F-7 | `docs/ad-testing-safety.md:171-176`: the operator checklist is on develop since 0582e3f, but it lacks Steps A–E, so row 5.0 cannot be ticked from it. Lifting the steps in is owed by the release-manager | `git log --first-parent develop -- …14-OPERATOR-CHECKLIST.md` = 0582e3f. A case-insensitive grep for "test ad\|test device" on develop finds 2 lines: `:361`, "test ad-unit IDs", and `:453`, "on the test device". Neither is a registration or a "Test Ad" label check |
| I-1 | `docs/release-checklist.md:73` new 3.5b: read the POMs of `play-services-games-v2`, `ads-mobile-sdk`, UMP and `hsdp` from `dl.google.com/android/maven2`. `THIRD_PARTY.md:245` U-1 points to 3.5b | The auditor's cache finding (AARs only, no POM). I did not fetch any POM in this pass |
| I-2 | `THIRD_PARTY.md:85`: the row now carries https://github.com/ermanergoz/peg-solitarie | The URL is recorded in `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md:61` on develop |
| I-3, I-4 | not changed | I-3 is commit history (ruled not worth rewriting). I-4 concerns earlier reports, which are outside my files for this pass. This report carries `Ran on:` |

## Checks
- `git rev-list --count v1.0..develop` (dev) — pass (810).
- `git log --no-merges --format=%s v1.0..develop | grep -cE '^feat'` (dev) — pass (154).
- `grep -n 'implementation\|compileOnly' app/build.gradle.kts` (dev) — pass (478–547; `compileOnly` at 505).
- `sed -n 120,165p …/OpeningBook.kt` and `head -3 golden/book/*.txt` (dev) — pass (Wiegleb depth 3; the other four depth 4).
- `ls scripts` (dev) — "No such file or directory", so F-6 is confirmed.
- `git rev-list --count develop..<branch>` for check-gates, audio-review and candy-backdrop (dev) — 0, 24 and 8. check-gates has no commits yet, so the Check-script caveats in 1.2/3.1 stay.
- `git log --diff-filter=A` / `--first-parent develop` on `14-OPERATOR-CHECKLIST.md` (dev) — added at 82d9200, on develop at 0582e3f.
- `grep -rn 'step 6\|step 1\.4\|checklist 1\.4\|downloadable'` over my five files — pass. The remaining hits are only the two historical "downloadable" sentences, which describe the removal.
- `git diff --quiet -- docs/store/en-US docs/store/tr-TR` — pass. The store texts are unchanged, so the length table still holds: 13/76/2222/427 and 13/71/2521/447 (`python3 -I` `len()`).
- `git diff --name-only` — my five files, plus `docs/tech-design.md`, which is the tech-lead's concurrent edit and not mine.
- Not run, as dispatched: `./gradlew check` or any other Gradle task. No commit was made.

## Open issues / risks
- **F-1 is open and blocks the merge.** `docs/store/en-US/full_description.txt:15`, `en-US/changelogs/30000.txt:3`, the two tr-TR equivalents and `CHANGELOG.md:137-141` still promise that "no winning move" is proven. On develop, a solver budget miss also shows that banner (`GameViewModel.kt:1381-1385`). The copy is rewritten after the game-designer's ruling, in a follow-up release-manager pass. The banner behaviour itself belongs to the game-designer, the ux-designer and, if code changes, the tech-lead.
- **The F-2 rows are pending.** The family rows for OkHttp, Okio, Gson, Tink, `jakarta.inject-api` and the further annotation libraries (Apache-2.0), `protobuf-javalite` and Cronet (BSD-3), OkHttp's public-suffix data (MPL-2.0), and `hsdp` (unverified; it goes in §6 if still unverified), plus the closure of U-1, are added in a follow-up pass once the research-analyst's licence verdict lands. Until then, `THIRD_PARTY.md` §5b fails checklist item 1.4 by design. Tech design §16 #20 (notice duties, Google `third_party_licenses.txt`) keeps its deadline: before `release/3.0.0`. Owners are listed there.
- **`scripts/play-upload.sh` is owed** (F-6). Owner: the tech-lead, who assigns the android-game-dev on a `feature/*` branch off develop, with code-auditor review and tech-lead sign-off. The producer routes it, before Gate 2 (checklist item 1.7). `14-RELEASE-PLAN.md` item 2 still says the script is created on the release branch. That contradicts the checklist's no-code scope; the plan is not my file in this pass.
- **The operator checklist lacks the ad test-device steps** (F-7). Steps A–E of `docs/ad-testing-safety.md` §3a have to be lifted into `14-OPERATOR-CHECKLIST.md` before its Step 8. Owner: the release-manager, in a dispatch that permits editing that file. This blocks release-checklist row 5.0.
- The `CHANGELOG.md:11` commit count is a dated snapshot. It is re-counted at item 1.6.

## Handoff
Next: producer — commit these five files and this report on `feature/phase-14-release-docs`, then return the branch to the same fresh code-auditor for F-2 to F-7, I-1 and I-2. F-1 waits for the game-designer's ruling, and the F-2 rows wait for the research-analyst's verdict; route both back to the release-manager for one follow-up pass. Also route the play-upload script (tech-lead) and the operator-checklist Steps A–E (release-manager, with permission to edit that file).
