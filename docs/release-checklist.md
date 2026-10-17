# Release checklist — the studio side of cutting `release/<X.Y.Z>`

**Owner:** release-manager. **Scope:** everything the studio can do on this machine.

This is *not* the founder's list. What only the founder can reach — the Play Console, the AdMob
UI, the developer website, the keystore, a second Google account — lives in
`.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md`, and the two lists meet in exactly
three places, flagged **[F]** below. A `[F]` item that is still unticked when the artifact is ready
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
| 1.1 | [ ] `develop` is clean and every release blocker on the tech-lead's ordered list is merged or explicitly deferred with a named owner | [S] | `git status --porcelain` empty; the blocker table in the latest `.planning/reports/phase-*/tech-lead-*.md` has no open row marked **branch cut** |
| 1.2 | [ ] The **Check command passes at release settings** on `develop`, on the merged tree — not on any contributing branch | [S] | `FUZZ_SEEDS=100000 ./scripts/check.sh` exits 0 (the Check command once `feature/phase-14-check-gates` lands; `docs/tech-design.md` §16 item 9). Record the wall-clock time and the seed count actually honoured |
| 1.3 | [ ] No disabled, ignored or suppressed test, and no new baseline, entered the release range without a code-auditor line | [S] | `git diff v<previous>..develop -- '*/src/*' \| grep -cE '^\+.*(@Suppress\|@Ignore\|Disabled\|!!)'` reviewed hit by hit |
| 1.4 | [ ] `THIRD_PARTY.md` exists and **no shipped row lacks a licence** | [S] + [F] for store artwork | Every row in `THIRD_PARTY.md` §§1–5 has a licence; §6 has no row that is both unverified and shipped; §7 has no `PENDING FOUNDER` left |
| 1.5 | [ ] Every founder question that blocks the cut is answered | [S] reads, [F] answers | `.planning/questions/FOUNDER.md` — no `## Q-…  (open)` whose `Blocks:` line names the release |
| 1.6 | [ ] The changelog source range is known | [S] | `git rev-list v<previous>..develop --count` recorded; `git log --no-merges --grep='^feat' --grep='^fix' --grep='^perf' -E` captured as raw material |

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
| 2.5 | [ ] Every claim in the store release notes maps to a changelog bullet | [S] | read side by side; a note promising something the changelog does not carry is a listing that promises a feature that is not there |
| 2.6 | [ ] `CLAUDE.md`'s Build command names the artifact actually uploaded | [S] | `grep 'Build command' CLAUDE.md` |
| 2.7 | [ ] One commit: `chore(release): <X.Y.Z>` | [S] | `git log --oneline -1` |

Only `VERSION`, `versionCode`/`versionName`, `CHANGELOG.md`, this checklist, `THIRD_PARTY.md`,
`docs/store/` and `docs/` live on the release branch. **A release branch is not where a feature
gets finished.** A documentation commit (for example `docs/tech-design.md`) may land here; a
behaviour change may not — it goes back to `develop` through a `feature/*` branch, and the branch
is re-cut.

## 3. Gates that must be green on the release branch

| # | Gate | Who | Command / evidence |
|---|---|---|---|
| 3.1 | [ ] Check at release settings, on the release branch itself | [S] | `FUZZ_SEEDS=100000 ./scripts/check.sh` → exit 0 (`docs/tech-design.md` §16 item 9, R-7: the script holds the one-build-per-worktree lock and runs `check`; it lands with `feature/phase-14-check-gates`, and until then this item cannot be ticked) |
| 3.2 | [ ] Fuzz harness at the release tier, failing seeds file **absent** | [S] | `domain/build/fuzz-failures.txt` does not exist (`docs/tech-design.md` §7.3, §16 item 9). Any failing seed blocks the release (`rules/puzzle-design.md` invariant 8) |
| 3.3 | [ ] Coverage gate green at its recorded threshold | [S] | `koverVerify` inside `check`; record the measured figure, not just "passed" |
| 3.4 | [ ] The four literal gates ran — ad-ID, Play-Games-ID, audio-focus, dp/sp | [S] | they are wired into `check`; confirm the task list actually invoked them |
| 3.5 | [ ] Every shipped dependency coordinate falls under a `THIRD_PARTY.md` row | [S] | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` — every coordinate printed maps to a row or a family row; **this is the run that closes `THIRD_PARTY.md` §5b** |
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
| 4.7 | [ ] No release artifact left on disk from a failed or throwaway-signed build | [S] | `ls app/build/outputs/bundle/release/ app/build/outputs/apk/release/` — a signed artifact sitting there after a failed build is exactly what gets uploaded by accident |
| 4.8 | [ ] The one-screen founder summary is written: version, changelog, checklist state, store-asset state, and the **exact** upload command | [S] | `.planning/reports/release-<X.Y.Z>/release-manager.md` |

**The studio stops here.** Upload requires `.planning/approvals/publish-<X.Y.Z>.approved`, which
the founder creates at Gate 2 and **the studio must never create**. The publish hook enforces it;
the gated script (`scripts/play-upload.sh`) refuses without it as well, belt and braces. The
approval file lives on `develop`, not on `main` and not on the tag, so the upload is run from a
`develop` checkout after the approval merge — the hook blocks it anywhere else.

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
| 5.0 | [ ] **Gate, before the build is installed:** the test phone's advertising ID is registered under AdMob → Settings → Test devices, registered **≥ 24 h** ago, and the first ad seen in the app carries the **"Test Ad"** label | **[F]** registers; qa-engineer confirms the label | `docs/ad-testing-safety.md` §3. No label ⇒ not in test mode ⇒ **stop, tap nothing, back out.** 5.6–5.8 must not be attempted until the label is observed. Leave the device registered afterwards — removal also takes up to 24 h and resumes live ads |
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
| 5.9 | [ ] Sound, hints from the opening book, resume, and the downloadable fonts all survive the shrink **on the store copy** | qa-engineer |
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
