# Release checklist — the studio side of cutting `release/<X.Y.Z>`

**Owner:** release-manager. **Scope:** everything the studio can do on this machine.

This is *not* the founder's list. What only the founder can reach — the Play Console, the AdMob
UI, the developer website, the keystore, a second Google account — lives in
`.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md`, and the two lists meet at every
item flagged **[F]** below. A `[F]` item that is still unticked when the artifact is ready
is an **escalation**, not a judgement the studio may make on the founder's behalf
(`rules/studio.md`: only the producer talks to the founder).

Every box starts unticked, on every release. A pre-ticked box is a false claim about a system
someone else has to trust. Tick an item only after observing its evidence, and record the evidence
— a command's exit code, a hash, a line of output — in
`.planning/reports/release-<X.Y.Z>/release-manager.md`.

Conventions: **[S]** studio-verifiable, **[F]** founder-only, **[S→F]** the studio prepares it and
the founder acts on it.

**Before anyone runs a release build that carries the real ad IDs, read
`docs/ad-testing-safety.md`.** It is binding, not advisory: it decides what QA and the founder may
tap, and §5 below cannot be started until its §3 (test-device registration) is done. Its one rule
is short enough to put here — **never tap an ad that does not carry the "Test Ad" label.**

---

## 1. Before the branch is cut

| # | Item | Who | Evidence that ticks it |
|---|---|---|---|
| 1.1 | [ ] `develop` is clean and every release blocker on the tech-lead's ordered list is merged or explicitly deferred with a named owner | [S] | `git status --porcelain` empty; every row of `docs/tech-design.md` §14.1 whose last cell says **Blocks the branch cut** is merged into `develop` or deferred with a named owner (the table's own note, "How release-checklist item 1.1 reads this table", says how). For 3.0.0 that includes R1, the Undecided hint outcome (`bugfix/hint-undecided`), and LN, the Licenses screens with every notice (`feature/licences-screen`), besides R-2 and F-1; each checked with `git merge-base --is-ancestor <branch> develop`. PU, the upload script, is **not** a branch-cut blocker: it blocks Gate 2 through item 1.7 |
| 1.1a | [ ] **The board is playable with TalkBack:** `feature/talkback-board` is merged into `develop` before the cut. The founder answered Q-023 with option 1 (2026-10-21; `.planning/questions/FOUNDER.md`): the board becomes TalkBack-playable in 3.0.0, and the release waits for it, even when it is the last item left. Shipping without it (Q-023's option 2, board named and peg count spoken but not playable, or option 3) is the founder's call only, put to the founder through `Q-PUBLISH-3.0.0`, never the studio's. Read with 1.1, which reads the same branch if `docs/tech-design.md` §14.1 lists it | [S] | `git merge-base --is-ancestor feature/talkback-board develop` exits 0 and the branch's code-auditor PASS, qa-engineer PASS (TalkBack on a device) and tech-lead sign-off exist. If it has not merged, no store text or `CHANGELOG.md` bullet claims TalkBack or screen-reader support (`docs/store/README.md`, Accessibility claims) |
| 1.2 | [ ] The **Check command passes at release settings** on `develop`, on the merged tree — not on any contributing branch | [S] | `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` exits 0 (`./scripts/check.sh` is the Check command, `CLAUDE.md` line 6, since `feature/phase-14-check-gates`; it passes `--rerun-tasks` to `./gradlew check` verbatim; `docs/tech-design.md` §16 item 9). 100,000 clamps to the whole 10,000-level space, as in 3.1. `--rerun-tasks` is required for the reason given in 3.1: without it a cached `:domain:fuzz` prints no seed count to record. Record the wall-clock time and the seed count actually honoured |
| 1.3 | [ ] No disabled, ignored or suppressed test, and no new baseline, entered the release range without a code-auditor line | [S] | `git diff v<previous>..develop -- '*/src/*' \| grep -cE '^\+.*(@Suppress\|@Ignore\|Disabled\|!!)'` reviewed hit by hit |
| 1.4 | [ ] `THIRD_PARTY.md` exists and **no shipped row lacks a licence** | [S] + [F] for store artwork | Every row in `THIRD_PARTY.md` §§1–5 has a licence; §6 has no row that is both unverified and shipped; §7 has no `PENDING FOUNDER` left; §5b's open item is closed: every family has a row with the research-analyst's verdict (done 2026-10-21), U-6 is closed by item 3.5, and `docs/tech-design.md` §16 #20, the notice duties, is closed, since its deadline is this cut |
| 1.5 | [ ] Every founder question that blocks the cut is answered | [S] reads, [F] answers | `.planning/questions/FOUNDER.md` — no `## Q-…  (open)` whose `Blocks:` line names the release |
| 1.6 | [ ] The changelog source range is known | [S] | `git rev-list v<previous>..develop --count` recorded; `git log --no-merges --grep='^feat' --grep='^fix' --grep='^perf' -E` captured as raw material |
| 1.7 | [ ] *(Blocks Gate 2, not the branch cut; `docs/tech-design.md` §14.1 PU.)* The upload script `scripts/play-upload.sh` is on `develop`, reviewed and merged, **or** the producer has recorded that this release is uploaded by the founder in the Console (operator checklist Step 8 item 1). The script checks no approval itself: the gate is the plugin hook `hooks/publish-gate.sh`, which blocks any command that runs `play-upload.sh` or `fastlane supply` unless `.planning/approvals/publish-$(cat VERSION).approved` exists (plugin `rules/stacks.md` 1.0.0, scaffold contract 9: "it checks no approval itself (`hooks/publish-gate.sh` does)"; `rules/play-store.md`: "the publish hook blocks it") | [S] | `git ls-tree develop scripts/play-upload.sh` prints the file, its code-auditor PASS and tech-lead sign-off exist (`feature/play-upload`, §14.1 PU), and `shellcheck scripts/play-upload.sh` is clean. **Today it does not exist** on `develop` or on any branch (see the note under §4). It is code, so it is never written on the release branch |

**If 1.2 cannot be run green at release settings, stop and say so.** `rules/gitflow.md` makes it a
precondition of the branch, and a release cut over a red or a *weaker-than-required* Check is the
failure this checklist exists to prevent. A Check command that does not yet implement the studio's
own gates (coverage, fuzz) is a *recorded* gap with a named owner, never a silent pass.

## 2. Cutting the branch

| # | Item | Who | Evidence |
|---|---|---|---|
| 2.1 | [ ] `git checkout -b release/<X.Y.Z> develop` | [S] | `git rev-parse --abbrev-ref HEAD` |
| 2.2 | [ ] Root `VERSION` equals `<X.Y.Z>` | [S] | `cat VERSION` |
| 2.3 | [ ] `versionName` equals `VERSION`, and `versionCode` equals `X*10000 + Y*100 + Z` and is **strictly greater** than the highest versionCode ever published | [S] | the two lines in `app/build.gradle.kts`; the published value read from the Console by the founder **[F]** |
| 2.4 | [ ] `CHANGELOG.md` has a `## [<X.Y.Z>] — <date>` section and the date is today | [S] | the file |
| 2.4a | [ ] **Pending-branch clean-up.** For every branch named as pending in `CHANGELOG.md` (*(pending: …)* markers), in `THIRD_PARTY.md` (the branch table at the top, and rows marked "delete this row at checklist item 2.4a") and in `docs/store/README.md` (its two branch tables): if the branch merged into `develop` before the cut, its markers and pending notes are removed; if it did not, its changelog bullets, its `THIRD_PARTY.md` rows and its en-US and tr-TR store lines are deleted | [S] | `git merge-base --is-ancestor <branch> release/<X.Y.Z>` per branch, result recorded; `grep -n 'pending:' CHANGELOG.md` prints nothing; the listing lengths re-counted and the word-rule greps in `docs/store/README.md` re-run and silent |
| 2.5 | [ ] Every claim in the store release notes maps to a changelog bullet | [S] | read side by side; a note promising something the changelog does not carry is a listing that promises a feature that is not there |
| 2.5a | [ ] *(Optional, not a blocker.)* **Hint copy, raised after R1.** The listing, the store release notes and `CHANGELOG.md` are written to the "Today" column of `.planning/reports/phase-14/game-designer-hint-timeout.md` §4: they claim that a shown hint is a real winning move, and never that "no winning move" is proven. If `bugfix/hint-undecided` (R1, the Undecided hint outcome) has merged into `develop` before Gate 2, the copy **may** be raised to that table's "After R1 merges" column, en-US and tr-TR alike: the listing line becomes "Hint shows a real winning move. When it says there is no winning move, there is none from that position, and when it can't work a position out in time, it tells you so."; the release note may become "Hints you can trust: 'no winning move' appears only when it is proven"; the `CHANGELOG.md` hint bullet may add "and it never shows 'no winning move' unless that is proven". "Hints that tell the truth on every board" stays out in both columns, and no coverage count or "always"/"instant" claim is used before the R5 census | [S] | `git merge-base --is-ancestor bugfix/hint-undecided develop` exits 0 before any line is raised; the store lengths re-counted (`docs/store/README.md`) and 2.5 re-read. If the branch has not merged, nothing changes and this item is recorded as not taken |
| 2.6 | [ ] `CLAUDE.md`'s Build command names the artifact actually uploaded | [S] | `grep 'Build command' CLAUDE.md` |
| 2.7 | [ ] One commit: `chore(release): <X.Y.Z>` | [S] | `git log --oneline -1` |

Only `VERSION`, `versionCode`/`versionName`, `CHANGELOG.md`, this checklist, `THIRD_PARTY.md`,
`docs/store/`, `docs/` and `CLAUDE.md`'s Build command line (item 2.6) change on the release branch. **A release branch is not where a feature
gets finished.** A documentation commit (for example `docs/tech-design.md`) may land here; a
behaviour change may not — it goes back to `develop` through a `feature/*` branch, and the branch
is re-cut.

## 3. Gates that must be green on the release branch

| # | Gate | Who | Command / evidence |
|---|---|---|---|
| 3.1 | [ ] Check at release settings, on the release branch itself | [S] | `mkdir -p build; FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks --console=plain > build/release-check.log 2>&1; echo "exit $?"` → `exit 0` (`./scripts/check.sh` is the Check command, `CLAUDE.md` line 6; `docs/tech-design.md` §16 item 9, R-7: the script holds the one-build-per-worktree lock and runs `check`, passing both flags through; on a fresh clone the root `build/` does not exist until `mkdir` makes it). The log, git-ignored under `**/build/`, is where the evidence for 3.1, 3.3 and 3.4 is read. **`--console=plain` and the file are required:** on a terminal, Gradle's rich console keeps no line per executed task, so 3.4 could not be read from it (`.planning/reports/phase-14/qa-engineer-check-gates.md` open issue 5). **`--rerun-tasks` is required.** `:domain:fuzz` and every other task keep Gradle's up-to-date and build-cache semantics (`org.gradle.caching=true`), so a release-branch tree that CI or a developer already built comes back `FROM-CACHE` or `UP-TO-DATE` and prints none of the evidence lines below (`.planning/reports/phase-14/code-auditor-check-gates.md` finding 2). Forcing execution makes this run the release's own execution of every gate, so the fuzz lines, the 3.2 file check, the 3.3 coverage figure and the 3.4 task list all come from this run. If the log shows `> Task :domain:fuzz FROM-CACHE` or `UP-TO-DATE`, the item is not ticked: run it again with the flag. **100,000 is exhaustive, not a sample:** the seed is the level number and the level space is finite, `MAX_CHALLENGE_LEVEL = 10_000` (`GeneratorConstants.kt`), so 10,000 is the whole space and a larger value clamps to it. `docs/tech-design.md` §7.3 item 6: "`FUZZ_SEEDS=100000` is accepted and means 'the entire finite level space plus the determinism pass' … Recorded here so nobody reads R-7's run as 90,000 seeds short"; `.planning/reports/phase-14/tech-lead-r2-gate-brief.md`: "`FUZZ_SEEDS=10000` is the whole space and `FUZZ_SEEDS=100000` cannot be more than that". Evidence: the log carries "fuzz: requested 100000 exceeds the level space 10000; running the whole space (exhaustive)", then "fuzz: examined 10000 levels (requested 100000), …" (`FuzzHarnessTest`); record both lines |
| 3.2 | [ ] Fuzz harness at the release tier, failing seeds file **absent** | [S] | `test ! -e domain/build/fuzz-failures.txt` after 3.1's run: the failure file is `domain/build/fuzz-failures.txt` (`:domain:fuzz` deletes it at the start of a run and writes one line per failing level; `docs/tech-design.md` §7.3 item 5, §16 item 9). **This check is sound only because 3.1 forces `--rerun-tasks`.** The file is written by the test body and is not a declared task output, so a `FROM-CACHE` green `:domain:fuzz` restores a green result without deleting a file left by an earlier red run (`.planning/reports/phase-14/qa-engineer-check-gates.md` open issue 2). Read 3.2 only after a 3.1 log that shows `> Task :domain:fuzz` executed; if 3.1 ran without the flag, 3.2 is not ticked. Any failing seed blocks the release (`rules/puzzle-design.md` invariant 8) |
| 3.3 | [ ] Coverage gate green at its recorded threshold | [S] | `:domain:koverVerify` inside `check` is the gate (kover 0.9.11, `minBound(90)` on `:domain` line coverage, `domain/build.gradle.kts`): the 3.1 log shows `> Task :domain:koverVerify` executed. **The figure comes from one more command, run after 3.1:** `./gradlew :domain:koverXmlReport --offline` (about 1 s; `:domain:test` stays up to date from 3.1). Then record the measured figure, not just "passed", from the root LINE counter of `domain/build/reports/kover/report.xml`, and check that the file's mtime is later than 3.1's run. `koverVerify` writes only `domain/build/reports/kover/verify.err`, not `report.xml`. Without this step the XML is stale from an earlier run, or absent on a fresh clone (`.planning/reports/phase-14/qa-engineer-check-gates.md` open issue 1). Recorded at the gate's introduction (`feature/phase-14-check-gates`, 2026-10-23, `.planning/reports/phase-14/android-game-dev-check-gates.md` C6): **`:domain` line coverage 99.0 % (896 of 905 lines) at bound 90**. A figure below 99.0 % at the cut is reported, not just passed |
| 3.4 | [ ] The four literal gates ran — ad-ID, Play-Games-ID, audio-focus, dp/sp | [S] | they are wired into `check`. In 3.1's plain-console log, `grep -E '^> Task :(app\|domain):(adIdLiteralCheck\|gamesIdLiteralCheck\|audioFocusLiteralCheck\|dpSpLiteralCheck)' build/release-check.log` must print all eight lines (`:app` and `:domain` × four), none of them `FROM-CACHE` or `UP-TO-DATE`. Record the count |
| 3.5 | [ ] Every shipped dependency coordinate falls under a `THIRD_PARTY.md` row | [S] | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` — every coordinate printed maps to a row or a family row, the families in §5b's open item and `com.google.android.play:hsdp` included; **this is the run that closes `THIRD_PARTY.md` §5b and U-6**. Any coordinate not among the 192 of the 2026-09-15 report (`.planning/reports/phase-14/research-analyst-release-docs-licences.md`) gets a research-analyst verdict before the release, and the listing must show no native `cronet-embedded` (rows H and §5b open item) |
| 3.5a | [ ] **NOTICE scan.** Every artifact 3.5 prints was searched for a NOTICE file, and each one found travels with the app | [S] | for each coordinate, `unzip -l` of its jar (for an AAR, the AAR and its `classes.jar`) filtered for `NOTICE`, including the 51 coordinates the 2026-10-18 scan could not reach (`THIRD_PARTY.md` §2, Apache-2.0 text row); record the count scanned and every hit. Once 4.3 has built the bundle, each hit is either in it (`unzip -l <aab> \| grep -i notice`) or bundled under `assets/licenses/`. **checker-qual:** unless the Licenses screen (`feature/licences-screen`, `docs/tech-design.md` §3.6 and §14.1 LN) credits "Checker Framework qualifiers" (`THIRD_PARTY.md` §5b row I), `grep -c '^org\.checkerframework\.' app/build/outputs/mapping/release/mapping.txt` after 4.3 must print 0; a kept class brings its MIT notice duty with it |
| 3.5b | [ ] The Google SDKs' licences are read from their own POMs, not inferred: `play-services-games-v2`, `ads-mobile-sdk`, `user-messaging-platform` and `hsdp` | [S] | the `<licenses>` block of each `<artifact>-<version>.pom` under `https://dl.google.com/android/maven2/`; the local Gradle cache holds only the AARs of the first three (`.planning/reports/phase-14/code-auditor-release-docs.md` I-1). The research-analyst read all four on 2026-10-21 (U-1 and I-1 closed; `THIRD_PARTY.md` §4 rows B to D and §5b row A). At the cut, confirm the versions 3.5 prints are still 22.0.0, 1.4.0, 4.0.0 and 2.0.1; a changed version is re-read and recorded in `THIRD_PARTY.md` §4 or §5b |
| 3.6 | [ ] No secret, path, account identifier or credential entered a tracked file in the release range | [S] | `git diff v<previous>..HEAD \| grep -nE 'BEGIN [A-Z ]*PRIVATE KEY\|\.jks\|/Users/\|password'` reviewed hit by hit |
| 3.7 | [ ] `git status --porcelain` clean, no keystore anywhere in the working tree | [S] | plus `find . -name '*.jks' -o -name '*.keystore' -o -name 'keystore.properties'` → empty |

## 4. The artifact

| # | Item | Who | Evidence |
|---|---|---|---|
| 4.1 | [ ] Real ad-unit and Play Games identifiers are in place | **[F]** | operator checklist Step 6 — `./gradlew :app:verifyReleaseAdIds :app:verifyReleaseGamesIds` exits 0. **Test identifiers are not a substitute**; an artifact built with them is not a release candidate and must not be uploaded |
| 4.1a | [ ] The three ad IDs in the built artifact are the **right** ones — read out of the artifact and compared, character by character, against the app's ad units in the AdMob Console | [S] reads them out of the bundle, **[F]** supplies the Console comparand | `docs/ad-testing-safety.md` §6. This is the one ad check that a test-mode pass cannot cover: test mode serves happily for a valid-but-wrong unit. Done by reading, never by clicking. No ID value is written into this file, a report, or any tracked file |
| 4.2 | [ ] The upload keystore is reachable through the four `PLAY_UPLOAD_*` environment variables, and nowhere else | **[F]** | operator checklist Step 7. No value of any of the four appears in any file, in this checklist, or in any report |
| 4.3 | [ ] The release bundle builds | [S] | the Build command in `CLAUDE.md`; record the exit code |
| 4.4 | [ ] The bundle is signed with the **upload** certificate, not the debug key | [S] verifies, **[F]** supplies the comparand | `jarsigner -verify -verbose:summary -certs <aab>`; the SHA-1 must equal the upload key certificate in the Console (read by the founder) |
| 4.5 | [ ] R8 evidence | [S] | `app/build/outputs/mapping/release/missing_rules.txt` **absent**; the Play Games classes present in `mapping.txt` (kept, not stripped) |
| 4.6 | [ ] Artifact identity recorded | [S] | `shasum -a 256 <aab>`, and the versionName read back out of the bundle's own manifest |
| 4.6a | [ ] The bundle keeps the five bundled fonts and their two OFL texts (`.planning/reports/15-founder-feedback/tech-lead-fonts.md` AC-F5, its release half) | [S] | `bundletool dump resources --bundle <aab> \| grep 'font/'` lists `fredoka_medium`, `fredoka_semibold`, `nunito_regular`, `nunito_semibold` and `nunito_bold`; `unzip -l <aab> \| grep 'base/assets/licenses/'` lists `Fredoka-OFL.txt` and `Nunito-OFL.txt` |
| 4.7 | [ ] No release artifact left on disk from a failed or throwaway-signed build | [S] | `ls app/build/outputs/bundle/release/ app/build/outputs/apk/release/` — a signed artifact sitting there after a failed build is exactly what gets uploaded by accident |
| 4.8 | [ ] The one-screen founder summary is written: version, changelog, checklist state, store-asset state, and the **exact** upload command | [S] | `.planning/reports/release-<X.Y.Z>/release-manager.md` |

**The studio stops here.** Upload requires `.planning/approvals/publish-<X.Y.Z>.approved`, which
the founder creates at Gate 2 and **the studio must never create**. The publish hook enforces it.
The approval file is merged into `develop`, not into `main` or the tag, so the upload is run from a
checkout of `feature/publish-<X.Y.Z>-done`, which the producer cuts from `develop` after the
approval merge; the hook blocks the command on `main` and on the tag.

**The gated upload script does not exist yet.** `rules/play-store.md` and
`.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` item 2 name `scripts/play-upload.sh`
(`fastlane supply` with `PLAY_SERVICE_ACCOUNT_JSON`, track `internal`), but there is no `scripts/`
directory on `develop` (0582e3f) or on any branch. The script itself checks no approval; the plugin
hook `hooks/publish-gate.sh` refuses the command without the approval file (plugin `rules/stacks.md`
1.0.0, scaffold contract 9). It cannot go on the release branch, because it is code and the release
branch takes no code (§2); the release plan's item 2 says the same since 2026-10-21. It is owed on
`feature/play-upload` off `develop` (`docs/tech-design.md` §14.1 PU): the tech-lead assigns it
to the android-game-dev, the code-auditor reviews it and the tech-lead signs it off, routed by the
producer, before Gate 2 (item 1.7). It blocks Gate 2, not the branch cut. Until it merges, the only upload route is the founder's own
Console upload, operator checklist Step 8 item 1.

## 5. Verified from a real track download — not from this machine

The point of the whole exercise. A locally built artifact carries the upload certificate; the one a
tester downloads carries the **app signing** certificate, and only the second is what a real
player's Play Games sign-in is checked against. Everything below is void unless 5.1 passes.

This is the **only** step in the whole release that can reach the founder's live AdMob account, and
5.0 is what stops it from doing so. Ads are verified in **test mode on the real ad unit IDs**, which
verifies more than live ads do — reasoning, policy text and sources in `docs/ad-testing-safety.md`
§6. Live ads are never used for verification.

| # | Item | Who |
|---|---|---|
| 5.0 | [ ] **Gate, before the build is installed:** the test phone's advertising ID is registered under AdMob → Settings → Test devices, registered **≥ 24 h** ago, and the first ad seen in the app carries the **"Test Ad"** label. Evidence: `14-OPERATOR-CHECKLIST.md` Step 7a (items A and B tick this row); reasoning in `docs/ad-testing-safety.md` §3. No label ⇒ not in test mode ⇒ **stop, tap nothing, back out.** 5.6–5.8 must not be attempted until the label is observed. Leave the device registered afterwards — removal also takes up to 24 h and resumes live ads | **[F]** registers; qa-engineer confirms the label |
| 5.1 | [ ] **Gate:** the installed build is the store copy — installer is the Play Store package, and the versionCode and versionName are this release's | [S] runs, **[F]** provides the installed build |
| 5.2 | [ ] Play Games sign-in works on the app signing certificate | qa-engineer |
| 5.3 | [ ] Leaderboard submissions appear on the server-side ladders, not merely "no error in app" | qa-engineer |
| 5.4 | [ ] An achievement unlocks and its popup renders; one incremental tier ticks | qa-engineer |
| 5.5 | [ ] Cloud save produces the union across two devices, with the device's own commit as the oracle | qa-engineer |
| 5.6 | [ ] The consent form appears before any ad request, on a real ad configuration | qa-engineer |
| 5.7 | [ ] The **real** rewarded unit — in test mode, per 5.0 — loads, grants on completion, grants nothing on early dismissal, and the fail-open path still hands over the assist on a no-fill | qa-engineer |
| 5.8 | [ ] The **real** interstitial unit — in test mode, per 5.0 — appears only after Continue, never on loss or restart, with the clock paused | qa-engineer |
| 5.8a | [ ] Ad inspector was opened on the test device and shows both real units requesting and filling — it launches only on a registered test device, so 5.0 is its prerequisite (`ad-testing-safety.md` §6) | qa-engineer |
| 5.8b | [ ] No ad was tapped that did not carry the "Test Ad" label; if one was, `docs/ad-testing-safety.md` §5 was followed and the incident is recorded as a `result:` line in the UAT sheet | qa-engineer, **[F]** for his own device |
| 5.9 | [ ] Sound, hints from the opening book, resume, and the bundled Fredoka and Nunito fonts (the first screen drawn in them, with the device offline) all survive the shrink **on the store copy** | qa-engineer |
| 5.10 | [ ] The account-swap wipe: keep-local preserves, replace-with-cloud replaces, and every walk-away route keeps | qa-engineer, needs **[F]** second account |

Results are recorded with an observed `result:` line per test in the phase's UAT sheet. Never a
pre-ticked box.

## 6. Merge, tag, back-merge

Only after §§1–5 and the founder's Gate 2 answer.

```sh
git checkout main && git merge --no-ff release/<X.Y.Z>
git tag v<X.Y.Z>
git checkout develop && git merge --no-ff main
git branch -d release/<X.Y.Z>
git tag --merged develop | grep -x v<X.Y.Z>     # must print the tag
```

| # | Item | Evidence |
|---|---|---|
| 6.1 | [ ] `main` carries the release merge, and nothing was committed directly on `main` or `develop` | `git log --first-parent main -1`; the branch-guard hook |
| 6.2 | [ ] The tag exists and is **reachable from `develop`** | the `grep -x` above prints the tag |
| 6.3 | [ ] The release branch is deleted | `git branch --list 'release/*'` empty |
| 6.4 | [ ] `.planning/MILESTONES.md` records the milestone → product-version mapping | the file |

**The tag is what the release is; `main` is only where it lives.**

## 7. Rollback — the position if the release has to be pulled

Decide this *before* uploading, not while a bad build is live. Three separate things can be rolled
back and they are not the same:

**7a. The rollout.** The fastest and the most useful. In the Console, **halt the staged rollout**
of the release. Players who have not yet received it never do; players who already have it **keep
it** — Play has no "un-install an update" action. So halting is containment, not repair.

**7b. The build.** There is **no downgrade path**. `versionCode` can never decrease for a published
app, so "going back to the previous build" means *shipping the previous build's code under a new,
higher versionCode* — a fresh release from a `hotfix/*` branch:

```sh
git checkout -b hotfix/<X.Y.Z+1> main        # from the tag's merge on main, never from develop
# revert the offending change, bump VERSION and versionCode, changelog entry
git checkout main && git merge --no-ff hotfix/<X.Y.Z+1>
git tag v<X.Y.Z+1>
git checkout develop && git merge --no-ff main
```

Patch bump, same gates, same approval file (a new
`.planning/approvals/publish-<X.Y.Z+1>.approved` — the old one does not carry over).

**7c. The repository.** `main` and the tag stay. A published tag is history and is not rewritten or
deleted; the record that a bad version existed is part of what makes the next one trustworthy. The
correction is a new commit on a `hotfix/*` branch, never a force-push.

**What cannot be rolled back at all, and therefore needs the most care before upload:**

- **A `versionCode` that is too high.** It is a one-way door — every future release must exceed it.
- **Published achievement definitions.** Incremental step totals are immutable after publication; a
  Console/app mismatch shows up only at runtime.
- **A cloud save written by a newer schema onto an account.** A build that is behind cannot always
  read it. Any `SCHEMA_VERSION` bump is gated on the constraints in `docs/tech-design.md` §4.
- **A lost upload keystore, before Play App Signing enrolment.** After enrolment Play offers a key
  reset; before it, there is no recovery. **[F]**

**Pre-mortem question to answer in the release report, every time:** *if this build is wrong, what
is the fastest true statement we can make to a player, and which of 7a/7b do we reach for first?*

## 8. After the release

| # | Item | Who |
|---|---|---|
| 8.1 | [ ] The release report is written and committed | [S] |
| 8.2 | [ ] `.planning/reports/STATUS.md` carries the release's Done line | producer |
| 8.3 | [ ] QA artifacts under `.planning/qa/<task>/` are deleted | producer |
| 8.4 | [ ] A retrospective is run for the closed version | producer |
| 8.5 | [ ] Every item deferred out of this release has a named owner and a home in the backlog | [S] |
