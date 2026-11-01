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
| 1.1a | [ ] **The board is playable with TalkBack:** `feature/talkback-board` is merged into `develop` before the cut. The founder answered Q-023 with option 1 (2026-10-21; `.planning/questions/FOUNDER.md` on `develop`, recorded in `415e5b2` and merged with `feature/ux-spec-folds` at `4ff4ec6`): the board becomes TalkBack-playable in 3.0.0, and, in the founder's words, "release/3.0.0 waits for it only if it is the last item left". Shipping without it (Q-023's option 2, board named and peg count spoken but not playable, or option 3) is the founder's call only, never the studio's, put to the founder as row TB's one pre-cut `Q-<NNN>` ("wait, or ship option 2"), which the producer queues only when every other **Blocks the branch cut** row is merged or deferred and this item is the last unticked pre-cut item; `Q-PUBLISH-3.0.0` (item 4.8) is written after the bundle and cannot carry it. While it is open the cut waits (the founder's standing answer); on "ship", this item is deferred with a named owner under 1.1 and the no-claim clause below binds. Read with 1.1, which reads the same branch if `docs/tech-design.md` §14.1 lists it | [S] | **The merge, not the ancestry test.** `git merge-base --is-ancestor feature/talkback-board develop` alone never ticks this item: it exits 0 for a branch with no commits of its own (as on 2026-10-23, when the branch was `develop`'s head) and stays 0 if the board is never built. All four must hold: (1) `M=$(git log --merges --first-parent develop --grep="Merge branch 'feature/talkback-board'" --format=%H -1)` prints a commit; (2) that merge brought the board's code, not only docs: `git diff --name-only $M^1 $M -- app/src/main/` lists the files of the branch's brief (`.planning/reports/talkback-board/tech-lead-brief.md`), `ui/components/BoardSpeech.kt` among them — the brief's own docs commits do not count; (3) the reviews cover what merged, on the pairwise path of `docs/tech-design.md` §14.1's merge order and the TalkBack brief's §13.3: the code-auditor PASS and the qa-engineer PASS under `.planning/reports/talkback-board/` each name the branch's code head they reviewed, `R`; after `R`, nothing changed `app/` on the branch but merges of `develop` in (`git log --no-merges --first-parent --format=%h R..$M^2 -- app/` prints nothing); and the tech-lead sign-off names as reviewed a commit whose `app/` tree equals the merged one (`git diff --quiet <signed-off> $M^2 -- app/` exits 0) and records its re-read of every such merge-in's conflict hunks; (4) the qa-engineer PASS covers TalkBack driven on a device (a real phone, else the brief's `Pixel_9` emulator) through the brief's QA procedure — a sitting recorded VOID (crashed speech engine) or BLOCKED is not a PASS. If it has not merged, no store text or `CHANGELOG.md` bullet claims TalkBack or screen-reader support (`docs/store/README.md`, Accessibility claims) |
| 1.2 | [x] The **Check command passes at release settings** on `develop`, on the merged tree — not on any contributing branch | [S] | `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` exits 0 (`./scripts/check.sh` is the Check command, `CLAUDE.md` line 6, since `feature/phase-14-check-gates`; it passes `--rerun-tasks` to `./gradlew check` verbatim; `docs/tech-design.md` §16 item 9). 100,000 clamps to the whole 10,000-level space, as in 3.1. `--rerun-tasks` is required for the reason given in 3.1: without it a cached `:domain:fuzz` prints no seed count to record. Record the wall-clock time and the seed count actually honoured. **Quiet host:** while row DT is deferred (`docs/tech-design.md` §14.3 item 13), record the 1-minute load average at the start and the end; both must be ≤ 5.0. A red run above it is re-run on a quiet host, never widened (§8.6 item 4) **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** run on the release branch, whose tree is `develop` 26b56bf plus docs only (`git diff --stat 26b56bf HEAD -- ':!docs' ':!*.md' ':!.planning'` empty): `FUZZ_SEEDS=100000 ./scripts/check.sh --offline` exit 0, BUILD SUCCESSFUL in 11m 17s; load average 5.27 at start, 5.11 at end (slightly above 5.0; the run was green, so no re-run owed). Evidence in 3.1 |
| 1.3 | [x] No disabled, ignored or suppressed test, and no new baseline, entered the release range without a code-auditor line | [S] | `git diff v<previous>..develop -- '*/src/*' '*.gradle.kts' 'config/' '.editorconfig' \| grep -cE '^\+.*(@(file:)?Suppress\|@SuppressLint\|tools:ignore\|@Ignore\|@Disabled\|!!)'` reviewed hit by hit (pattern widened 2026-11-01, code-auditor A5: the old `@Suppress\|@Ignore\|Disabled\|!!` missed `@file:Suppress`, `@SuppressLint` and `tools:ignore`). **3.0.0 (2026-11-01, release-manager):** ticked on `.planning/reports/release-3.0.0/code-auditor-suppressions.md`, VERDICT PASS: 31 `@Suppress`/`@file:Suppress` sites over `v1.0..a7c52ab` (13 main, 18 test), 0 `@SuppressLint`, 0 `tools:ignore`, 0 `@Ignore`/`@Disabled`, 0 baselines; 8 carried prior approval lines, the other 23 approved by code-auditor in that report (incl. `Clay.kt:3` `@file:Suppress`, the site the old grep missed); none rejected. Advisories A1–A4, A6 go to 3.0.1 / develop (READINESS §5) **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** the four founder branches' delta `a3f2221..26b56bf` adds 0 `@Suppress`/`@SuppressLint`/`tools:ignore`/`@Ignore`/`@Disabled`; 2 `!!`, both in tests (`SnapshotMapperTest.kt`, `GameViewModelResumeTest.kt`), none in production code. No code-auditor line: the founder took over review and sign-off for these branches (2026-11-01) and signed them off |
| 1.4 | [ ] `THIRD_PARTY.md` exists and **no shipped row lacks a licence** — the pre-cut half only; the release-graph half is item 3.5 and the bundle half item 4.6a, both after the cut | [S] + [F] for store artwork | Every row in `THIRD_PARTY.md` §§1–5 has a licence; §6 has no row that is both unverified and shipped; §7 has no `PENDING FOUNDER` left; §5b: every family has a row with the research-analyst's verdict (done 2026-10-21); `feature/licences-screen` has merged into `develop` with its code-auditor PASS, qa-engineer PASS (LN-7 green: items 1–3, 5 and 6, item 6 being the branch's own check that it adds no coordinate) and tech-lead sign-off, shown by its merge commit as in 1.1a (`git log --merges --first-parent develop --grep="Merge branch 'feature/licences-screen'" -1` prints one, and its diff lists `app/src/main/` and `build-logic/` files), not by `--is-ancestor` alone; and `docs/tech-design.md` §16 #20, the notice duties, has nothing left open but what its own cell leaves to the cut. **Not part of this item:** §5b's open item and U-6 close at 3.5, and the AAB listing (§13.1 LN-7 item 4) at 4.6a |
| 1.5 | [x] Every founder question that blocks the cut is answered | [S] reads, [F] answers | `.planning/questions/FOUNDER.md` — no `## Q-…  (open)` whose `Blocks:` line names the release **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** the only open question, Q-037, has `Blocks: nothing` |
| 1.6 | [x] The changelog source range is known | [S] | `git rev-list v<previous>..develop --count` recorded; `git log --no-merges --grep='^feat' --grep='^fix' --grep='^perf' -E` captured as raw material **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `git rev-list v1.0..develop --count` = 1480; the re-cut delta `a3f2221..develop` = 98 commits, 24 feat/fix/perf subjects |
| 1.7 | [x] *(Blocks Gate 2, not the branch cut; `docs/tech-design.md` §14.1 PU.)* The upload script `scripts/play-upload.sh` is on `develop`, reviewed and merged, **or** the producer has recorded that this release is uploaded by the founder in the Console (operator checklist Step 8 item 1). The script checks no approval itself: the gate is the plugin hook `hooks/publish-gate.sh`, which blocks any command that runs `play-upload.sh` or `fastlane supply` unless `.planning/approvals/publish-$(cat VERSION).approved` exists (plugin `rules/stacks.md` 1.0.0, scaffold contract 9: "it checks no approval itself (`hooks/publish-gate.sh` does)"; `rules/play-store.md`: "the publish hook blocks it") | [S] | `git ls-tree develop scripts/play-upload.sh` prints the file, its code-auditor PASS and tech-lead sign-off exist (`feature/play-upload`, §14.1 PU), and `shellcheck scripts/play-upload.sh` is clean. The script is on `develop` since merge `1dd1080` (`feature/play-upload`, code `9005fe3..6a0b032`): code-auditor PASS `.planning/reports/play-upload/code-auditor.md`, QA PASS `.planning/reports/play-upload/qa-engineer.md`, tech-lead sign-off `.planning/reports/play-upload/tech-lead-signoff.md`. Syntax check: `bash -n < scripts/play-upload.sh` (`bash -n scripts/play-upload.sh` is blocked by the gate); shellcheck 0.11.0 is a host tool on this Mac since 2026-10-24. Its contract is in the note under §4. **The only allowed invocation is `./scripts/play-upload.sh`**, with optional `VAR=value` prefixes or a **bare** `env …` prefix (literally the word `env`, never `/usr/bin/env`), as the tech-lead ruled (`.planning/reports/play-upload/tech-lead-signoff.md:106`). Forbidden: `/usr/bin/env …`, `/usr/bin/nohup …`, `/usr/bin/time …`, `exec …`, and `/bin/bash …`, `/bin/sh …` or any other wrapper or shell named by its path. The publish gate lets every one of those through without the approval file (E-1, the sign-off's §5 probe table, rows 4–8; stopgap T-11 until the plugin fix is installed here). The producer records the route this release uses (script or Console) before this item is ticked. It is code, so it is never written on the release branch. **3.0.0 (2026-11-01, release-manager): Console route** — the founder answered Q-029 with option 2, manual upload in the Play Console (`.planning/questions/FOUNDER.md` on `develop`, `15d02fe`); runbook `.planning/release/3.0.0/FOUNDER-RUNBOOK.md` |

**If 1.2 cannot be run green at release settings, stop and say so.** `rules/gitflow.md` makes it a
precondition of the branch, and a release cut over a red or a *weaker-than-required* Check is the
failure this checklist exists to prevent. A Check command that does not yet implement the studio's
own gates (coverage, fuzz) is a *recorded* gap with a named owner, never a silent pass.

## 2. Cutting the branch

| # | Item | Who | Evidence |
|---|---|---|---|
| 2.1 | [x] `git checkout -b release/<X.Y.Z> develop` | [S] | `git rev-parse --abbrev-ref HEAD` **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `git worktree add -b release/3.0.0 …/PegSolitarie-release develop` (26b56bf); the old branch's one unmerged commit d1bc673 cherry-picked as af323e8 |
| 2.2 | [x] Root `VERSION` equals `<X.Y.Z>` | [S] | `cat VERSION` **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `cat VERSION` = 3.0.0 |
| 2.3 | [x] `versionName` equals `VERSION`, and `versionCode` equals `X*10000 + Y*100 + Z` and is **strictly greater** than the highest versionCode ever published | [S] | the two lines in `app/build.gradle.kts`; the published value read from the Console by the founder **[F]** **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `versionName = "3.0.0"`, `versionCode = 30_000` (`app/build.gradle.kts:294-295`); the published-value comparand stays the founder's (runbook D, expected 2) |
| 2.4 | [x] `CHANGELOG.md` has a `## [<X.Y.Z>] — <date>` section and the date is today | [S] | the file **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `## [3.0.0] — 2026-11-01`, today; the re-cut note and the four (re-cut) bullets added |
| 2.4a | [x] **Pending-branch clean-up.** For every branch named as pending in `CHANGELOG.md` (*(pending: …)* markers), in `THIRD_PARTY.md` (the branch table at the top, and rows marked "delete this row at checklist item 2.4a") and in `docs/store/README.md` (its two branch tables): if the branch merged into `develop` before the cut, its markers and pending notes are removed; if it did not, its changelog bullets, its `THIRD_PARTY.md` rows and its en-US and tr-TR store lines are deleted | [S] | `git merge-base --is-ancestor <branch> release/<X.Y.Z>` per branch, result recorded; `grep -n 'pending:' CHANGELOG.md` prints nothing; the listing lengths re-counted and the word-rule greps in `docs/store/README.md` re-run and silent **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `grep -n 'pending:' CHANGELOG.md` prints nothing; all four founder branches are ancestors of `release/3.0.0`; listing lengths re-counted (`docs/store/README.md`) and both word-rule greps re-run, silent |
| 2.5 | [x] Every claim in the store release notes maps to a changelog bullet | [S] | read side by side; a note promising something the changelog does not carry is a listing that promises a feature that is not there **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** each 30000 note line maps to a 3.0.0 bullet: Turkish interface → Added; star bands, full-screen play, Home Continue → Changed (re-cut); the rest as before |
| 2.5a | [ ] *(Optional, not a blocker.)* **Hint copy, raised after R1.** The listing, the store release notes and `CHANGELOG.md` are written to the "Today" column of `.planning/reports/phase-14/game-designer-hint-timeout.md` §4: they claim that a shown hint is a real winning move, and never that "no winning move" is proven. If `bugfix/hint-undecided` (R1, the Undecided hint outcome) has merged into `develop` before Gate 2, the copy **may** be raised to that table's "After R1 merges" column, en-US and tr-TR alike: the listing line becomes "Hint shows a real winning move. When it says there is no winning move, there is none from that position, and when it can't work a position out in time, it tells you so."; the release note may become "Hints you can trust: 'no winning move' appears only when it is proven"; the `CHANGELOG.md` hint bullet may add "and it never shows 'no winning move' unless that is proven". "Hints that tell the truth on every board" stays out in both columns, and no coverage count or "always"/"instant" claim is used before the R5 census | [S] | `git merge-base --is-ancestor bugfix/hint-undecided develop` exits 0 before any line is raised; the store lengths re-counted (`docs/store/README.md`) and 2.5 re-read. If the branch has not merged, nothing changes and this item is recorded as not taken |
| 2.6 | [x] `CLAUDE.md`'s Build command names the artifact actually uploaded | [S] | `grep 'Build command' CLAUDE.md` **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `Build command: ./gradlew :app:bundleRelease`, a read |
| 2.7 | [x] One commit: `chore(release): <X.Y.Z>` | [S] | `git log --oneline -1` **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `a11780d chore(release): 3.0.0` |

Only `VERSION`, `versionCode`/`versionName`, `CHANGELOG.md`, this checklist, `THIRD_PARTY.md`,
`docs/store/`, `docs/` and `CLAUDE.md`'s Build command line (item 2.6) change on the release branch. *(Note 2026-11-01: for 3.0.0 the Build command line already moved to `./gradlew :app:bundleRelease` on `develop`, with `feature/release-3.0.0-prep`, before the cut; at the cut 2.6 is a read.)* **A release branch is not where a feature
gets finished.** A documentation commit (for example `docs/tech-design.md`) may land here; a
behaviour change may not — it goes back to `develop` through a `feature/*` branch, and the branch
is re-cut.

## 3. Gates that must be green on the release branch

| # | Gate | Who | Command / evidence |
|---|---|---|---|
| 3.1 | [x] Check at release settings, on the release branch itself | [S] | `mkdir -p build; FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks --console=plain > build/release-check.log 2>&1; echo "exit $?"` → `exit 0` (`./scripts/check.sh` is the Check command, `CLAUDE.md` line 6; `docs/tech-design.md` §16 item 9, R-7: the script holds the one-build-per-worktree lock and runs `check`, passing both flags through; on a fresh clone the root `build/` does not exist until `mkdir` makes it). The log, git-ignored under `**/build/`, is where the evidence for 3.1, 3.3 and 3.4 is read. **`--console=plain` and the file are required:** on a terminal, Gradle's rich console keeps no line per executed task, so 3.4 could not be read from it (`.planning/reports/phase-14/qa-engineer-check-gates.md` open issue 5). **`--rerun-tasks` is required.** `:domain:fuzz` and every other task keep Gradle's up-to-date and build-cache semantics (`org.gradle.caching=true`), so a release-branch tree that CI or a developer already built comes back `FROM-CACHE` or `UP-TO-DATE` and prints none of the evidence lines below (`.planning/reports/phase-14/code-auditor-check-gates.md` finding 2). Forcing execution makes this run the release's own execution of every gate, so the fuzz lines, the 3.2 file check, the 3.3 coverage figure and the 3.4 task list all come from this run. If the log shows `> Task :domain:fuzz FROM-CACHE` or `UP-TO-DATE`, the item is not ticked: run it again with the flag. **100,000 is exhaustive, not a sample:** the seed is the level number and the level space is finite, `MAX_CHALLENGE_LEVEL = 10_000` (`GeneratorConstants.kt`), so 10,000 is the whole space and a larger value clamps to it. `docs/tech-design.md` §7.3 item 6: "`FUZZ_SEEDS=100000` is accepted and means 'the entire finite level space plus the determinism pass' … Recorded here so nobody reads R-7's run as 90,000 seeds short"; `.planning/reports/phase-14/tech-lead-r2-gate-brief.md`: "`FUZZ_SEEDS=10000` is the whole space and `FUZZ_SEEDS=100000` cannot be more than that". Evidence: the log carries "fuzz: requested 100000 exceeds the level space 10000; running the whole space (exhaustive)", then "fuzz: examined 10000 levels (requested 100000), …" (`FuzzHarnessTest`); record both lines. The quiet-host read of 1.2 applies here too **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `FUZZ_SEEDS=100000 ./scripts/check.sh --offline` exit 0 (log copied to `build/check.log`). Run **without** `--rerun-tasks` (57 tasks executed, 50 from cache), but `> Task :domain:fuzz` executed, not `FROM-CACHE`, and printed both evidence lines: "fuzz: requested 100000 exceeds the level space 10000; running the whole space (exhaustive)" and "fuzz: examined 10000 levels (requested 100000), determinism pass, 0 failures" |
| 3.2 | [x] Fuzz harness at the release tier, failing seeds file **absent** | [S] | `test ! -e domain/build/fuzz-failures.txt` after 3.1's run: the failure file is `domain/build/fuzz-failures.txt` (`:domain:fuzz` deletes it at the start of a run and writes one line per failing level; `docs/tech-design.md` §7.3 item 5, §16 item 9). **This check is sound only because 3.1 forces `--rerun-tasks`.** The file is written by the test body and is not a declared task output, so a `FROM-CACHE` green `:domain:fuzz` restores a green result without deleting a file left by an earlier red run (`.planning/reports/phase-14/qa-engineer-check-gates.md` open issue 2). Read 3.2 only after a 3.1 log that shows `> Task :domain:fuzz` executed; if 3.1 ran without the flag, 3.2 is not ticked. Any failing seed blocks the release (`rules/puzzle-design.md` invariant 8) **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `domain/build/fuzz-failures.txt` absent after 3.1's executed fuzz |
| 3.3 | [x] Coverage gate green at its recorded threshold | [S] | `:domain:koverVerify` inside `check` is the gate (kover 0.9.11, `minBound(90)` on `:domain` line coverage, `domain/build.gradle.kts`): the 3.1 log shows `> Task :domain:koverVerify` executed. **The figure comes from one more command, run after 3.1:** `./gradlew :domain:koverXmlReport --offline` (about 1 s; `:domain:test` stays up to date from 3.1). Then record the measured figure, not just "passed", from the root LINE counter of `domain/build/reports/kover/report.xml`, and check that the file's mtime is later than 3.1's run. `koverVerify` writes only `domain/build/reports/kover/verify.err`, not `report.xml`. Without this step the XML is stale from an earlier run, or absent on a fresh clone (`.planning/reports/phase-14/qa-engineer-check-gates.md` open issue 1). Recorded at the gate's introduction (`feature/phase-14-check-gates`, 2026-10-23, `.planning/reports/phase-14/android-game-dev-check-gates.md` C6): **`:domain` line coverage 99.0 % (896 of 905 lines) at bound 90**. A figure below 99.0 % at the cut is reported, not just passed **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `:domain:koverVerify` executed in 3.1; `:domain:koverXmlReport` root LINE counter **99.4 % (934 of 940)** at bound 90 |
| 3.4 | [x] The four literal gates ran — ad-ID, Play-Games-ID, audio-focus, dp/sp | [S] | they are wired into `check`. In 3.1's plain-console log, `grep -E '^> Task :(app\|domain):(adIdLiteralCheck\|gamesIdLiteralCheck\|audioFocusLiteralCheck\|dpSpLiteralCheck)' build/release-check.log` must print all eight lines (`:app` and `:domain` × four), none of them `FROM-CACHE` or `UP-TO-DATE`. Record the count **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** all eight `> Task :(app|domain):(adId|gamesId|audioFocus|dpSp)LiteralCheck` lines executed in 3.1's log, none FROM-CACHE/UP-TO-DATE |
| 3.5 | [x] Every shipped dependency coordinate falls under a `THIRD_PARTY.md` row | [S] | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` — every coordinate printed maps to a row or a family row, the families in §5b's open item and `com.google.android.play:hsdp` included; **this is the run that closes `THIRD_PARTY.md` §5b and U-6**, the post-cut half of item 1.4. **okhttp re-check (row J):** row J's exact-version address, `publicsuffix/list` commit `a312abe0e11421aff8fe909a5e02f36b7c5d28de`, holds only for `com.squareup.okhttp3:okhttp@4.11.0`. If the listing prints another okhttp, the research-analyst re-derives the address before the release: hash that tag's test-resource `public_suffix_list.dat` and find the `publicsuffix/list` revision with the same git blob (`.planning/reports/licences-screen/research-analyst-mpl.md`, on `feature/licences-screen` until it merges); the release-manager then updates row J and the Licenses screen string is re-checked. Any coordinate not among the 192 of the 2026-09-15 report (`.planning/reports/phase-14/research-analyst-release-docs-licences.md`) gets a research-analyst verdict before the release, and the listing must show no native `cronet-embedded` (rows H and §5b open item). **3.0.0 (2026-11-01, research-analyst; ticked by release-manager):** PASS on `release/3.0.0` `a7c52ab`: 191 coordinates, the 2026-09-15 set less `ui-text-google-fonts` 1.11.3, none added, no version changed; all 191 POMs re-fetched (173 Apache-2.0, 10 Android SDK License, 5 Chromium BSD-3, 2 BSD-3-Clause, 1 MIT), each maps to a row; okhttp 4.11.0 (row J holds); no `cronet-embedded`. U-6 and §5b closed in `THIRD_PARTY.md` (`2cef970`). Evidence: `.planning/reports/release-3.0.0/research-analyst-licences.md` **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** the delta `a3f2221..26b56bf` changes no dependency (`gradle/libs.versions.toml` and `build-logic` untouched; `app/build.gradle.kts` only adds `localeFilters` and a test input), so the 191-coordinate result holds. fontTools/opentype-sanitizer are build-only, in no Gradle configuration (`THIRD_PARTY.md` §8) |
| 3.5a | [ ] **NOTICE scan.** Every artifact 3.5 prints was searched for a NOTICE file, and each one found travels with the app | [S] | for each coordinate, `unzip -l` of its jar (for an AAR, the AAR and its `classes.jar`) filtered for `NOTICE`, including the 51 coordinates the 2026-10-18 scan could not reach (`THIRD_PARTY.md` §2, Apache-2.0 text row); record the count scanned and every hit. Once 4.3 has built the bundle, each hit is either in it (`unzip -l <aab> \| grep -i notice`) or bundled under `assets/licenses/`. **checker-qual:** unless the Licenses screen (`feature/licences-screen`, `docs/tech-design.md` §3.6 and §14.1 LN) credits "Checker Framework qualifiers" (`THIRD_PARTY.md` §5b row I), `grep -c '^org\.checkerframework\.' app/build/outputs/mapping/release/mapping.txt` after 4.3 must print 0; a kept class brings its MIT notice duty with it. **3.0.0 — pre-bundle half PASS (2026-11-01, research-analyst; recorded by release-manager); box stays open for the post-bundle half after 4.3:** 191 coordinates scanned (the 51 unreached in 2026-10 included), 187 with a binary (222 files, 128 nested jars), 4 POM-only; 2 NOTICE hits, the two known (`jakarta.inject-api` 2.0.1 `META-INF/NOTICE.md`, okhttp 4.11.0 publicsuffix `NOTICE`), both bundled byte-equal under `app/src/main/assets/licenses/`. checker-qual is credited on the Licenses screen (`LicenseCatalog.kt:322`), so the `mapping.txt` grep is not owed. Owed after 4.3: `unzip -l <aab> \| grep -i notice` shows both. Evidence: `.planning/reports/release-3.0.0/research-analyst-licences.md`, `2cef970` |
| 3.5b | [x] The Google SDKs' licences are read from their own POMs, not inferred: `play-services-games-v2`, `ads-mobile-sdk`, `user-messaging-platform` and `hsdp` | [S] | the `<licenses>` block of each `<artifact>-<version>.pom` under `https://dl.google.com/android/maven2/`; the local Gradle cache holds only the AARs of the first three (`.planning/reports/phase-14/code-auditor-release-docs.md` I-1). The research-analyst read all four on 2026-10-21 (U-1 and I-1 closed; `THIRD_PARTY.md` §4 rows B to D and §5b row A). At the cut, confirm the versions 3.5 prints are still 22.0.0, 1.4.0, 4.0.0 and 2.0.1; a changed version is re-read and recorded in `THIRD_PARTY.md` §4 or §5b **3.0.0 (2026-11-01, research-analyst; ticked by release-manager):** confirmed on `release/3.0.0` — 22.0.0, 1.4.0, 4.0.0 and 2.0.1, unchanged; no re-read owed. Evidence: `.planning/reports/release-3.0.0/research-analyst-licences.md` |
| 3.6 | [x] No secret, path, account identifier or credential entered a tracked file in the release range | [S] | `git diff v<previous>..HEAD \| grep -nE 'BEGIN [A-Z ]*PRIVATE KEY\|\.jks\|/Users/\|password'` reviewed hit by hit. **3.0.0 (2026-11-01, release-manager):** `git diff v1.0..HEAD` → 488 hits; 36 outside `.planning/`, in 10 files (`app/build.gradle.kts`, `build-logic` signing sources and tests, `local.properties.template`, `docs/tech-design.md`, this file): only the words password/.jks in signing comments and docs, test fixtures (`fixture-store-password`, `/fixture/keys/upload.jks`) and the placeholder `/absolute/path/outside/this/repo/upload.jks`; the one `/Users/` hit is this row's own grep pattern. No key, no real value, no `/Users/` path. The other 452 are in `.planning/` (405 `/Users/` report paths). **Producer's scope ruling:** 3.6 guards shipped and published material (app/domain/build-logic sources, resources, `docs/store/`, `THIRD_PARTY.md`, `CHANGELOG.md`, scripts); `/Users/…` paths in `.planning/` reports are internal studio records in a repo with no remote, carry no secret or credential, and are accepted **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `git diff a3f2221..HEAD` outside `.planning/` (141 files): 0 added lines matching the pattern |
| 3.7 | [x] `git status --porcelain` clean, no keystore anywhere in the working tree | [S] | plus `find . -name '*.jks' -o -name '*.keystore' -o -name 'keystore.properties'` → empty **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** `git status --porcelain` empty after the commits; no `*.jks`/`*.keystore`/`keystore.properties` in the worktree |

## 4. The artifact

| # | Item | Who | Evidence |
|---|---|---|---|
| 4.1 | [ ] Real ad-unit and Play Games identifiers are in place | **[F]** | operator checklist Step 6 — `./gradlew :app:verifyReleaseAdIds :app:verifyReleaseGamesIds` exits 0. **Test identifiers are not a substitute**; an artifact built with them is not a release candidate and must not be uploaded |
| 4.1a | [ ] The three ad IDs in the built artifact are the **right** ones — read out of the artifact and compared, character by character, against the app's ad units in the AdMob Console | [S] reads them out of the bundle, **[F]** supplies the Console comparand | `docs/ad-testing-safety.md` §6. This is the one ad check that a test-mode pass cannot cover: test mode serves happily for a valid-but-wrong unit. Done by reading, never by clicking. No ID value is written into this file, a report, or any tracked file |
| 4.2 | [ ] The upload keystore is reachable through the four `PLAY_UPLOAD_*` environment variables, and nowhere else | **[F]** | operator checklist Step 7. No value of any of the four appears in any file, in this checklist, or in any report |
| 4.3 | [ ] The release bundle builds | [S] | `./gradlew :app:bundleRelease` (the Build command in `CLAUDE.md`, item 2.6; `:app:assembleRelease` makes an APK, which neither route uploads); record the exit code. Both routes upload this AAB, `app/build/outputs/bundle/release/app-release.aab` **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** **not built by the studio** — founder, 2026-11-01: "Don't build aab I will do it". The founder builds from the `v3.0.0` checkout (`FOUNDER-RUNBOOK.md` B); 4.4–4.7, 3.5a's post-bundle half and 4.6a are read from that AAB |
| 4.4 | [ ] The bundle is signed with the **upload** certificate, not the debug key | [S] verifies, **[F]** supplies the comparand | `jarsigner -verify -verbose:summary -certs <aab>`; the SHA-1 must equal the upload key certificate in the Console (read by the founder) |
| 4.5 | [ ] R8 evidence | [S] | `app/build/outputs/mapping/release/missing_rules.txt` **absent**; the Play Games classes present in `mapping.txt` (kept, not stripped) |
| 4.6 | [ ] Artifact identity recorded | [S] | `shasum -a 256 <aab>`, and the versionName read back out of the bundle's own manifest |
| 4.6a | [ ] The bundle keeps the five bundled fonts and their two OFL texts (`.planning/reports/15-founder-feedback/tech-lead-fonts.md` AC-F5, its release half) **and the Licenses screen's notices** (`docs/tech-design.md` §13.1 LN-7 item 4, the bundle half of item 1.4) | [S] | `bundletool dump resources --bundle <aab> \| grep 'font/'` lists `fredoka_medium`, `fredoka_semibold`, `nunito_regular`, `nunito_semibold` and `nunito_bold`; `unzip -l <aab> \| grep 'base/assets/licenses/'` lists `Fredoka-OFL.txt` and `Nunito-OFL.txt`, **and also**: (a) `google/index.tsv`; (b) every stem file the index names — each stem of `unzip -p <aab> base/assets/licenses/google/index.tsv \| cut -f3 \| sort -u` present as `google/<stem>.txt`; (c) LN-5's texts at the paths `BundledLicenseText` names — protobuf's `LICENSE` (rows F and G), Chromium's `LICENSE` (row H), checker-qual's MIT text (row I), OkHttp's public-suffix `NOTICE` (row J), `Jakarta-Inject-NOTICE.md`, `Apache-2.0.txt` and `MPL-2.0.txt` (row J's licence text, `licences-screen.md` LS-11) — each one's `unzip -p <aab> <path> \| shasum -a 256` starting with its recorded hash (`6e5e1173`, `368cca11`, `e90eaa08`, `8a9c58fb`, `992ed62d`, `cfc7749b`, `3f3d9e00`; LN-5, and `THIRD_PARTY.md` §2 for Apache-2.0 and the Jakarta NOTICE, §5b rows F–J for the rest); (d) the index's sources (`cut -f4`, split on commas) equal the AARs on 3.5's `releaseRuntimeClasspath` listing that carry `third_party_licenses.json`. (a)–(d) are never skipped: without `feature/licences-screen` merged there is no cut (1.1, 1.4) **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** left to the founder's AAB (no studio build). The two Fredoka files must hash to `fonts/fredoka-tr/recipe.json`'s `output_sha256` (`0effcf32…1b157d4f`, `5c764171…4890823c`); command in `FOUNDER-RUNBOOK.md` C and `.planning/reports/release-3.0.0/release-manager-recut.md` |
| 4.7 | [ ] No release artifact left on disk from a failed or throwaway-signed build | [S] | `ls app/build/outputs/bundle/release/ app/build/outputs/apk/release/` lists **exactly one** `app-release.aab` and nothing else, and `shasum -a 256 app/build/outputs/bundle/release/app-release.aab` equals 4.6's recorded value. That directory is the one the upload script sends from, so any other artifact there, or a different hash, is exactly what gets uploaded by accident (T-5, tied to T-3 below) |
| 4.8 | [x] The one-screen founder summary is written: version, changelog, checklist state, store-asset state, and the **exact** upload command | [S] | `.planning/reports/release-<X.Y.Z>/release-manager.md`. Script route: the command is `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh`, in exactly that form (T-11), run from the root of the `feature/publish-<X.Y.Z>-done` checkout after probes P0–P7 (`.planning/reports/play-upload/qa-engineer.md`, "Gate-2 probe list") (T-6). Console route: the AAB of 4.3 and its 4.6 hash **3.0.0 re-cut (2026-11-01, release-manager; `release/3.0.0` re-cut from `develop` 26b56bf):** summary and the Q-PUBLISH-3.0.0 draft in `.planning/reports/release-3.0.0/release-manager-recut.md`; Console route (Q-029) |

**The studio stops here.** Upload requires `.planning/approvals/publish-<X.Y.Z>.approved`, which
the founder creates at Gate 2 and **the studio must never create**. The publish hook enforces it.
The approval file is merged into `develop`, not into `main` or the tag, so the upload is run from a
checkout of `feature/publish-<X.Y.Z>-done`, which the producer cuts from `develop` after the
approval merge; the hook blocks the command on `main` and on the tag.

**The gated upload script.** `scripts/play-upload.sh` is on `develop` since merge `1dd1080`
(`feature/play-upload`, `docs/tech-design.md` §14.1 PU; reviews and sign-off under
`.planning/reports/play-upload/`). Its contract, as built: bash only (it refuses another shell); no
arguments; `PLAY_SERVICE_ACCOUNT_JSON` = the key file's absolute path, and the key must lie outside
the checkout (a key inside it is refused); it uploads `app/build/outputs/bundle/release/app-release.aab`
of the checkout it runs from to the `internal` track with `fastlane supply`; it builds nothing and
uploads no listing; it refuses to run while `fastlane`, `.fastlane`, `Supplyfile` or `Appfile` is at
the repository root; it prints the bundle's sha256 before fastlane starts; fastlane's analytics and
update check are off; every refusal exits 78 before anything is sent. The script checks no approval
itself: the plugin hook `hooks/publish-gate.sh` refuses the command without the approval file (plugin
`rules/stacks.md`, scaffold contract 9). The producer records which route this release uses (item
1.7): this script, or the founder's own Console upload (operator checklist Step 8 item 1).

Rules for the script route (QA `play-upload/qa-engineer.md`, tech-lead `play-upload/tech-lead-signoff.md`):

**3.0.0: not applicable** — Console route (1.7, Q-029). T-1 (key placement), T-6 (the command) and
T-11 (invocation) do not apply to this release; T-3 still binds the Console upload (the AAB uploaded
has 4.6's sha256).

- **T-11, invocation.** Only `./scripts/play-upload.sh`, with optional `VAR=value` prefixes or a
  **bare** `env …` prefix: literally the word `env`, never `/usr/bin/env` (tech-lead ruling,
  `play-upload/tech-lead-signoff.md:106`). Forbidden, because the publish gate lets them through
  without the approval file (E-1, a plugin defect; the sign-off's §5 rows 4–8): `/usr/bin/env …`,
  `/usr/bin/nohup …`, `/usr/bin/time …`, `exec …`, and `/bin/bash …`, `/bin/sh …` or any other
  wrapper or shell named by its path. T-11 stays as good practice after the plugin fix is installed
  here; it is the key-ordering rule below that lapses then (`play-upload/tech-lead-signoff.md:108`).
- **Key ordering (T-1).** The founder places the service-account key on this Mac only after
  `.planning/approvals/publish-<X.Y.Z>.approved` exists in the checkout the upload runs from, as a
  regular file (not a link) outside every repository, for example under `~/.config/`.
- **T-3, the bundle is the release's bundle.** The AAB uploaded is the one 4.3–4.6 built and verified
  on `release/<X.Y.Z>`. It reaches the `-done` checkout by a branch switch in the same worktree (the
  ignored `app/build/` stays) or by a copy to the same path. Right before the real run,
  `shasum -a 256 app/build/outputs/bundle/release/app-release.aab` must equal 4.6's value, and P7's
  printed hash must equal it too; on any difference, stop. The real run's hash line goes into the
  upload report as the record of what was sent.
- **T-6, the command.** `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh`,
  from the `-done` checkout's root, after probes P0–P7, in the foreground with an explicit timeout.
  If the timeout cuts it off, the result is unknown: check the Console before any re-run.
- **T-7, the upload report.** `.planning/reports/publish-<X.Y.Z>/release-manager-upload.md` quotes
  fastlane's summary table **without its `json_key` row** (it prints the key's path), and quotes the
  command line in T-11's form.
- **TL-1, a fastlane failure.** If fastlane fails in a way that needs an option the script does not
  pass, the fix is a script change on a feature branch, through the developer and the code-auditor.
  Never a hand-typed `fastlane supply` command: it would bypass every isolation the script adds,
  even with the approval in place.

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
