# code-auditor report — phase-14 release docs (`feature/phase-14-release-docs` before its merge into `develop`)
Date: 2026-10-21
Branch: feature/phase-14-release-docs (report only). Reviewed `git diff develop...feature/phase-14-release-docs` in `PegSolitarie-r5`: head `4962d13`, merge-base `8dc9f09`, `develop` at `0582e3f`; 18 commits, 28 files, +4,342/−0. I checked claims against `develop` read-only in `PegSolitarie-dev`.
Skills loaded: ergoz-soft:standards (role code-auditor). The product snapshot is 0.5.1 and the plugin is 1.0.0, so I read these plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing, ip-policy. I also read the product's `.claude/rules/lessons.md`. Skills: code-reviewer, security-reviewer. compose-skill, compose-performance-audit, typescript-pro and swift-expert do not apply, because the diff contains no code.
Ran in parallel with: an unnamed Gradle build in another worktree (per the dispatch)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the code-auditor is pinned to fable. This review ran on Opus under the founder's Q-019 (2026-10-17, `.planning/questions/FOUNDER.md`): Opus for every Fable-pinned role until 3.0.0 is published, with fresh reviewers. I am a fresh reviewer. I received no author reasoning and did not read `.planning/log/`.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- The merge preconditions in row 3 of the sign-off's merge-order table are met. Phase 15 is on `develop` (`c9228a9`). The font rows carry `tech-lead-fonts.md`'s text. The OFL-1.1 verdict is recorded. `docs/tech-design.md` carries every supersession, plus the additions from the close report. The merge with `develop` is clean.
- The listing limits pass, the word rules pass, and no secrets are present. Every font hash and all 29 store-art hashes match the files. Founder answers Q-015 to Q-018 and Q-022 are respected.
- **FAIL on three MAJOR findings.**
  - F-1: the store copy and the changelog promise that "no winning move" is always true. On `develop` it is not.
  - F-2: `THIRD_PARTY.md` says nothing else needs a licence or a notice. The research-analyst's report on this same branch disproves that, and it lists dependencies with no row, one of them with an unknown licence.
  - F-3: the release checklist lacks checks that three other documents say it contains.
- All the fixes are text edits. No code is involved.

## Findings

### MAJOR

**F-1 — The listing promises that Hint's "no winning move" is a proof. On `develop` it is not.**
- What the documents say:
  - `docs/store/en-US/full_description.txt:15`: "when it says there is no winning move, there is none from that position".
  - `docs/store/en-US/changelogs/30000.txt:3`: "Hints that tell the truth on every board".
  - The same claims in Turkish: `docs/store/tr-TR/full_description.txt:15` and `docs/store/tr-TR/changelogs/30000.txt:3`.
  - `CHANGELOG.md:136-140`: "no winning move" now means *proven*, not *gave up*.
- What the code does on `develop`:
  - The hint uses the opening book only within its depth. Beyond that, `GetHintUseCase.kt:120` runs the solver with a 1.5 s deadline.
  - That code's own KDoc (`GetHintUseCase.kt:32-34`) says: "unsolvable, budget-missed and cancelled all fold to the same `null` … its `null` is 'no move found in time', not a proof".
  - `GameViewModel.kt:1381-1385` turns that `null` into `noWinningMove = true`. The comment there reads: "A solver budget miss also returns null … the player was told 'no winning move'".
  - The banner then says "No winning move — undo or restart" (`strings.xml:108`).
- Where this happens:
  - The Wiegleb-45 book covers only 3 moves (`OpeningBook.kt:129,159`; `wiegleb45-k3.txt:2`, with 4 of 140 positions absent).
  - The solver resolves 0 of 76 Wiegleb depth-≤4 positions inside the deadline (`GetHintUseCase.kt:54-58`).
  - Diamond-41's book lacks 440 of its 818 depth-≤4 positions (`diamond41-k4.txt:2`). The branch's own `docs/tech-design.md:874` says those positions "wait out the deadline".
- A concrete case: a player who follows Hint for three moves on Wiegleb-45 stands on a proven winning line. A fourth Hint is off the book. It times out and shows "No winning move".
- Why it matters: this is a false claim about app behaviour in text bound for the Play listing, which is a Metadata and misleading-claims exposure. It also contradicts the branch's own tech design.
- **Fix (text):**
  - Drop the guarantee from the four listing texts and from `CHANGELOG.md:136-140`.
  - Correct "first four moves" at `CHANGELOG.md:139`: Wiegleb-45's book covers three.
- Whether the in-app banner on a budget miss should change is a product question outside this diff (see Open issues).

**F-2 — `THIRD_PARTY.md` claims a complete licence and notice record. Its own branch's evidence disproves this.**
- `research-analyst-font-licences.md:77-88` (open issue 1, on this branch) lists families on the last release dependency graph that have no row in `THIRD_PARTY.md` §5b:
  - BSD-3-Clause: `protobuf-javalite`, and Chromium's `cronet-*`;
  - MIT: `checker-qual`, which §5b mentions only "if present";
  - MPL-2.0 data: OkHttp's public-suffix list, with its NOTICE;
  - Apache-2.0: `okhttp`, `okio`, `gson`, `tink-android`, `jakarta.inject-api`;
  - `com.google.android.play:hsdp`, whose licence is unverified.
- I confirmed that `hsdp`, `cronet-*`, `okhttp`, `gson`, `tink-android`, `protobuf-javalite`, `checker-qual` and `jakarta.inject-api` are all named in `PegSolitarie/app/build/intermediates/metadata_library_dependencies_report/release/collectReleaseDependencies/dependencies.pb` (2026-09-15).
- Despite this, the file still says:
  - `THIRD_PARTY.md:222-224`: "Every family above is permissively or permittedly licensed, so no *licence* risk hides in the gap";
  - `THIRD_PARTY.md:326-330`: "every dependency is Apache-2.0 or a permitted Google SDK … **nothing shipped carries an unmet attribution obligation** and this is not a legal blocker for 3.0.0".
- The tech design on this branch has already put that sentence under review. `docs/tech-design.md:986-990` and §16 #20 (`:1108`) say: "BSD-3 and MIT make the notice a condition of redistribution". They assign "`THIRD_PARTY.md` §5b and §7 wording" to the release-manager.
- **Two shipped artifacts have an unknown licence.**
  - UMP is recorded as U-1 (`THIRD_PARTY.md:197,231`).
  - `hsdp` has no row at all.
  - The dispatch's requirement is "no … unknown entries", and the matrix in `rules/ip-policy.md` treats an unknown licence as "no reuse".
- **Fix:**
  - Add family rows for the families above, with the licences the research-analyst reported.
  - Add `hsdp` to §6 as an unverified row.
  - Rewrite the two sentences so they point at tech design §16 #20.
  - Preferably, close U-1 and `hsdp` now. Each needs one POM fetch from `maven.google.com`, and no Gradle run.

**F-3 — The release checklist lacks checks that other documents say it carries, and it still names the removed downloadable fonts.**
- **The font check is missing.** `THIRD_PARTY.md:143` says the release branch checks "that the AAB keeps all five fonts and both texts (`tech-lead-fonts.md` AC-F5; release-checklist 1.4)".
  - Item 1.4 (`docs/release-checklist.md:34`) checks only that licences are present.
  - No item runs `bundletool dump resources`.
  - AC-F5's release half is routed to the release-manager on `release/3.0.0` by `tech-lead-signoff.md`'s routed table and by `docs/tech-design.md:917-919`.
- **The pending-branch clean-up is missing.** `THIRD_PARTY.md:24-25`, `CHANGELOG.md:46` and `docs/store/README.md:39-40` say that rows and lines from branches that never reached `release/3.0.0` are deleted "at checklist step 1.4". Item 1.4 says nothing about it.
- **Four cross-references point at the wrong step.** `THIRD_PARTY.md:10`, `:59`, `:144` and `:225` send the dependency enumeration, and the NOTICE scan of the 51 unscanned coordinates, to "checklist step 6".
  - Step 6 is merge, tag and back-merge.
  - The enumeration is item 3.5.
  - No item scans for NOTICE files.
- **One item checks something that no longer exists.** Item 5.9 (`docs/release-checklist.md:117`) has QA check that "the downloadable fonts" survive the shrink. That path was deleted at `c9228a9` (tech design §11).
- **Fix:** add the AC-F5 check, the pending-branch clean-up and the NOTICE scan as checklist items. Retarget the "step 6" references to 3.5. Rewrite 5.9 for the bundled fonts.

### MINOR

**F-4 — Line citations are stale against `develop`.**
- `THIRD_PARTY.md:39` cites `app/build.gradle.kts:465-537`. On `develop` the `implementation(...)` block is at lines 478-547.
- `THIRD_PARTY.md:42` cites `:490-495` for the `compileOnly` comment. On `develop` it is at 500-505.
- Both ranges matched the merge-base `8dc9f09` to within a line. Phase 15 moved them.

**F-5 — Phase 15 is still marked pending after its merge, and one font claim contradicts the same file.**
- `CHANGELOG.md:56`, `:121`, `:125`, `:129`, `:148` and `:173` still carry "*(pending: `feature/phase-15-ux-refresh`)*".
  - Phase 15 merged at `c9228a9`.
  - The changelog's own rule (`CHANGELOG.md:45`) says the marker is removed on merge.
- `docs/store/README.md:38-39`, `:44` and `:52` still list phase 15 among "branches that have not merged yet".
- `CHANGELOG.md:177-178` says "the downloadable-font certificates" are proved to survive the shrink. That contradicts `:173-175` in the same file.
- INFO: `CHANGELOG.md:11` says "647 commits since `v1.0`". `git rev-list --count v1.0..develop` gives 810 today.

**F-6 — The release checklist describes an upload script that does not exist.**
- `docs/release-checklist.md:88-89` says "the gated script (`scripts/play-upload.sh`) refuses without it as well".
- `docs/store/README.md:5` says the script uploads with `fastlane supply`.
- No `scripts/` directory exists on `develop` or on any branch.
- `14-RELEASE-PLAN.md:278-282` creates the script on the release branch. But the checklist's own release-branch scope (`docs/release-checklist.md:55-59`) admits only `VERSION`, the version lines, `CHANGELOG.md`, the checklist, `THIRD_PARTY.md` and `docs/`. That scope also omits `CLAUDE.md`, whose Build-command edit item 2.6 expects (`docs/tech-design.md:744-746`).
- No item creates the script or has it reviewed.
- Also, `docs/release-checklist.md:7-8` says the founder's list and this one "meet in exactly three places, flagged **[F]**". The checklist has 11 rows marked [F].

**F-7 — `docs/ad-testing-safety.md:171-173` says `14-OPERATOR-CHECKLIST.md` "does not exist on this branch yet".**
- It exists on `develop` (merged at `0582e3f`).
- It carries none of §3a's Steps A to E. A grep for "test device" or "Test Ad" in it finds 0 hits.
- Yet release-checklist row 5.0 marks the test-device registration as founder-only and as a gate.
- The founder's own list therefore lacks the step the gate depends on. Owner: the release-manager.

**F-8 — `docs/tech-design.md` has four inaccuracies against `develop`.**
- `:535` says "committed depth-4 opening book". Wiegleb-45's book is depth 3 (`OpeningBook.kt:129`).
- `:978` counts "9 Kenney + 3 freesound borrows". That is the `.ogg` set on `develop`. It contradicts §12 (`:929`) and `THIRD_PARTY.md` §2, which give 7 Kenney, 3 freesound-derived and 2 in-house files for 3.0.0.
- `:778-780` states as current that CI runs on `release/**`. `develop`'s `ci.yml:8-11` triggers on `[develop, main]` only, because check-gates C3b is not built yet.
- `:152` omits `:domain`'s `tutorial/` package. `:253` says `TutorialBoards` "lives in `:domain` beside `Boards.kt`". It is in `domain/tutorial/`, and `Boards.kt` is in `domain/model/`.

### INFO
- **I-1 — the Google SDK licence evidence needs re-reading at the release.** `THIRD_PARTY.md:195-196` cites POM `<licenses>` for `play-services-games-v2` and `ads-mobile-sdk`. Today's Gradle cache holds only the AARs for both: no `.pom` and no `.module`. That is the same condition U-1 records for UMP. Checklist 3.5 should re-establish all three, not only UMP.
- **I-2 — one idea row has no URL.** The row "The founder's own earlier peg-solitaire repository" (`THIRD_PARTY.md:81`) carries no source URL. It is own work, but `rules/ip-policy.md` asks for a URL on every row.
- **I-3 — two early commits carry the Opus trailer.** `e5253f4` and `d9f9a60` predate the trailer rule. Rewriting them is not worth the churn, as the phase-15 sign-off also ruled.
- **I-4 — two reports lack a `Ran on:` line.** `release-manager-ad-safety.md` and `release-manager-release-docs.md` were written under the 0.5.1 reporting rule, which did not require it. `release-manager-founder-items.md` and `tech-lead-r2-gate-brief.md` are documents, not reports.
- **Suppressions:** none in the diff. The diff contains no code and adds no dependency. **APPROVED SUPPRESSION:** none.

### Verified as correct (spot-checks against `develop`)
- **Versions.**
  - `VERSION` is `3.0.0`, and `versionCode = 30_000` (`app/build.gradle.kts:291`) equals 3·10000+0·100+0.
  - `versionName = "3.0.0"` (`:292`).
  - `minSdk = 24` and `targetSdk = 36` (`android-app.gradle.kts:29-30`). This matches Q-018 and `rules/play-store.md`.
- **Dependencies.** All 20 direct `implementation` coordinates in `THIRD_PARTY.md` §4 match `app/build.gradle.kts`, `libs.versions.toml` and `[bundles] compose`. Nothing is missing and nothing is extra. `domain/build.gradle.kts:7-10` declares test dependencies only.
- **Fonts.**
  - The five font sha256s match `tech-lead-fonts.md` D1, and the two OFL texts match `782c3137…cf31f7e0` and `66db16f4…829734a2`.
  - `font_certs.xml` and `ui-text-google-fonts` are gone from `develop`.
  - `res/` contains 0 raster images.
- **Store art.** All 29 sha256s in `THIRD_PARTY.md` §7 match the founder's Desktop files byte for byte.
- **Listing claims.**
  - 5 Classic boards.
  - The 10,000-level cap (`GeneratorConstants.kt:42`).
  - 20 achievements and 7 leaderboards (`GamesIds.kt:92-201`).
  - The privacy URL (`PrivacyLinks.kt:47`).
  - The tutorial replay in Settings › General.
  - No `values-tr` on any branch.
  - A manifest that declares only `INTERNET`, and no analytics SDK.
  - Rewarded ads are opt-in, the game clock freezes during ads, and interstitials show only after Continue.
  - The colour-blind palette and the backdrop are correctly marked as coming from `feature/candy-backdrop`.
- **Founder answers.** Nothing contradicts Q-015 (keep the existing key), Q-016 or Q-022 (drafts for Gate 2), Q-017 (studio own work, with hashes), or Q-018 (minSdk 24 indefinitely, `docs/tech-design.md` §1.3).

## Outputs
- `.planning/reports/phase-14/code-auditor-release-docs.md` — this report. I edited no other file and committed nothing.

## Checks
- **Footprint:** `git diff --stat develop...feature/phase-14-release-docs` — pass. 28 files, 4,342 insertions, 0 deletions, all docs and `.planning/`.
- **Merge:** `git merge-tree --write-tree develop feature/phase-14-release-docs` — pass. Exit 0, tree `7408b40`, no conflicts.
- **Listing limits:** `python3 -I` `len()` over `docs/store/**/*.txt` — pass.
  - en-US: title 13, short 76, full 2,222, notes 427.
  - tr-TR: title 13, short 71, full 2,521, notes 447.
  - Limits: 30, 80, 4,000 and 500.
- **Word rules:** the three grep commands in `docs/store/README.md:74,85,86` — pass, no output (exit 1 for each).
- **Secrets:** `git diff develop...feature/phase-14-release-docs | grep -E '^\+'` scanned for private keys, `.jks`/keystore files, passwords, `AIza…`, `ca-app-pub-` IDs, `CgkI…`, tokens, `pub-` IDs and emails — pass. The only hits are hash fragments, a quoted `noreply@anthropic.com` trailer, and the checklist's own grep text.
- **Font hashes:** `shasum -a 256` over the font files and `assets/licenses/*` on `develop` — pass, equal to D1.
- **Store-art hashes:** `shasum -a 256` over the founder's Desktop art, compared with `THIRD_PARTY.md` §7 — pass ("ALL 29 HASHES MATCH").
- **Line citations:** `grep -n 'implementation\|compileOnly' app/build.gradle.kts` on `develop` and at `8dc9f09` — fail. Lines 478-547 and 505 on `develop`, against 466-537 and 495 at the merge-base (F-4).
- **Hint evidence:** `grep -n` over `GetHintUseCase.kt`, `GameViewModel.kt`, `OpeningBook.kt` and `golden/book/*.txt` headers — fail against the listing. Budget misses fold to `noWinningMove` (`GameViewModel.kt:1385`). The documented book gaps are 440/818 on Diamond-41 and 4/140 on Wiegleb-45, and Wiegleb's book is depth 3 (F-1).
- **Release graph:** `strings dependencies.pb | grep -E 'hsdp|protobuf|cronet|okhttp|checker|tink|gson|jakarta|user-messaging'` — fail against `THIRD_PARTY.md` §5b. All present (F-2).
- **Gradle cache:** `find ~/.gradle/caches/modules-2/files-2.1/<ump|games-v2|ads-mobile-sdk>` — AARs and javadoc only, no POMs (I-1).
- **Upload script:** `ls scripts` on `develop`, plus `git ls-tree` for `scripts/play-upload.sh` over every local branch — absent everywhere (F-6).
- **Operator checklist:** `grep -n -i 'test ad\|test device' 14-OPERATOR-CHECKLIST.md` on `develop` — 0 hits (F-7).
- **CI triggers:** `sed -n 1,22p .github/workflows/ci.yml` on `develop` — `branches: [develop, main]` (F-8).
- **Not run, as dispatched:** the Check command `./gradlew check`. The branch changes no Check input, and another worktree was building. No Check output exists to quote.

## Open issues / risks
- **A product question outside this diff:** the in-app banner says "No winning move" on a solver budget miss, not only on a proven dead end (`GameViewModel.kt:1381-1385`). Fixing F-1's text makes the listing honest. Whether the banner wording or the behaviour should change belongs to the game-designer and the ux-designer, and the tech-lead if the code changes. The producer routes it.
- **The F-2 notice duties** (tech design §16 #20) keep their own deadline: before `release/3.0.0`, with the Licenses screen. F-2 asks only that the record stop asserting the opposite, and that the known families and their unknown licences be listed.
- **Staffing deviation:** this review ran on Opus, the authors' model, under Q-019. It rests on my own reading against `develop` and on reproducible commands, not on the authors' recorded results.

## Handoff
Next: producer — FAIL. Route the fixes to their owners. Every fix is a text edit on this branch, and the same fresh code-auditor review re-checks them:
- **release-manager:** F-1 (the four store texts and `CHANGELOG.md:136-140`), F-2, F-3, F-4, F-5, F-6, F-7.
- **tech-lead:** F-8, and the `docs/tech-design.md` side of F-1's depth wording.
- **research-analyst:** the licences of UMP (U-1) and `hsdp`, and the per-family verdicts for F-2, if the release-manager needs them before the rows can be written.
- The banner question under Open issues goes to the game-designer and the ux-designer.

VERDICT: FAIL
