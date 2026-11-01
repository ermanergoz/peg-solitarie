# release-manager report — release-3.0.0: cut and pre-device-pass checks
Date: 2026-11-01
Branch: release/3.0.0 (da886ed..a58eaba; off develop `da886ed`), worktree `/Users/ermanergoz/Desktop/PegSolitarie-release`
Skills loaded: ergoz-soft:standards (role release-manager; game; android-compose-game: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, testing, lessons from the product snapshot imported by CLAUDE.md), gitflow (studio overrides win: conventional commits, `.planning/` committed). Not loaded: gsd-complete-milestone, because nothing is merged to main and no milestone is completed in this step
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort low (the harness's setting for this dispatch; not the agent file's pin)
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- Cut `release/3.0.0` from `develop` `da886ed` in a new worktree. `VERSION` 3.0.0 and versionCode 30000 are confirmed. The CHANGELOG draft is applied, with candy's entries added. The 2.4a pending markers are cleared in CHANGELOG, THIRD_PARTY and the store README.
- **Release Check green:** `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` exits 0. It fuzzed the whole 10,000-level space with 0 failures, ran 1,937 tests and measured coverage at 99.0 %. Host load was 4.31 at start and 4.32 at end.
- §3 checks without a device: 3.1–3.4, 3.5b and 3.7 are done. 3.5 is partly done; the per-coordinate diff goes to the research-analyst. 1.3 and 3.6 were reviewed hit by hit; each leaves an item for another role. `:app:bundleRelease` is founder-blocked (4.1/4.2).
- READINESS is rewritten for the cut. It adds the D-1 preconditions, D-13/D-14 (§16 #30), D-15 and D-16. The Q-PUBLISH draft now carries candy's measured lines.

## Outputs
- `VERSION`, `app/build.gradle.kts` — read only (3.0.0 / 30_000).
- `CHANGELOG.md` — draft applied: markers dropped; TalkBack, RM, R1, home-chip-arrival bullets; Under-the-hood corrected; candy bullet updated (Licenses screens, fade-in) and a backdrop line; preamble counts 1366 / 214.
- `THIRD_PARTY.md`, `docs/store/README.md` — 2.4a clean-up (merged branches, candy/Apache/LN rows).
- `docs/release-checklist.md` — tech-lead docs sign-off Conditions 4–6: the T-11 lapse wording, the quiet-host read (1.2, 3.1), the bare `bash -n <` syntax check. `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` — stray spaces.
- `.planning/release/3.0.0/READINESS.md` — the cut-state table, docs syncs, the device-pass list for QA.
- `.planning/release/3.0.0/Q-PUBLISH-3.0.0-draft.md` — candy measured lines, the Check line, the conditional §16 #30 line.
- Local only (git-ignored): `build/release-check.log`, `.planning/qa/release-3.0.0/deps.txt`, `coords.txt`, `check.log` (the cancelled run).

## Checks
- `git -C …/PegSolitarie-dev worktree add ../PegSolitarie-release -b release/3.0.0 develop` — pass (HEAD `da886ed`).
- `cat VERSION` — `3.0.0`. `app/build.gradle.kts:294-295` — `versionCode = 30_000`, `versionName = "3.0.0"`. That is 3·10000 + 0 + 0, and it exceeds the published 2. The Console read is F-13.
- A first run, `FUZZ_SEEDS=100000 ./gradlew check`, was cancelled by SIGINT to its client: it lacked `--rerun-tasks` and showed `FROM-CACHE` tasks, so it was not valid evidence. No `--stop`.
- `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks --console=plain > build/release-check.log` — **pass**:
  - `exit 0`, BUILD SUCCESSFUL in 11m 38s, 107 tasks, 107 executed.
  - Start 12:29:11, load 4.31; end 12:40:50, load 4.32.
  - "fuzz: requested 100000 exceeds the level space 10000; running the whole space (exhaustive)".
  - "fuzz: examined 10000 levels (requested 100000), determinism pass, 0 failures".
  - Test XML totals: 1,937 tests, 0 failures, 0 errors, 0 skipped.
- `test ! -e domain/build/fuzz-failures.txt` — pass (3.2).
- `./gradlew :domain:koverXmlReport --offline` — exit 0. `report.xml` mtime is 12:41:55, and it shows LINE missed 9, covered 912: **99.0 %** at bound 90 (3.3).
- `grep -E '^> Task :(app|domain):(adIdLiteralCheck|…)'` — 8/8 executed (3.4).
- `./gradlew :app:dependencies --configuration releaseRuntimeClasspath --offline` — exit 0, 191 coordinates. Every non-AndroidX group is named in THIRD_PARTY.md, and AndroidX is covered by the family row. okhttp 4.11.0; games-v2 22.0.0, ads-mobile-sdk 1.4.0, UMP 4.0.0, hsdp 2.0.1; no `cronet-embedded` (3.5 partial, 3.5b pass).
- `git rev-list v1.0..HEAD --count` — 1366; 214 feat; 89 fix/perf (1.6).
- `git diff v1.0..HEAD -- '*/src/*' | grep -cE '^\+.*(@Suppress|@Ignore|Disabled|!!)'` — 44, reviewed. Approval lines are missing for 6 main-source suppressions and the test-side set (READINESS 1.3). Not ticked.
- `git diff v1.0..HEAD | grep -nE 'BEGIN … PRIVATE KEY|\.jks|/Users/|password'` — 481 lines, reviewed. No secret anywhere. User-home paths appear only in `.planning/` reports (3.6 question).
- `git merge-base --is-ancestor <b> HEAD` for audio-review, colour-vision, licences-screen, candy-backdrop, phase-15-ux-refresh — all 0. `grep -n 'pending:' CHANGELOG.md` — silent (2.4a).
- `.planning/questions/FOUNDER.md` — only Q-025 is open, `Blocks: nothing` (1.5).
- `PLAY_UPLOAD_*` — all four unset (names checked only), so `:app:bundleRelease` was not run (4.3 founder-blocked).
- `git status --porcelain` clean after the commit; `find` for keystores is empty (3.7).
- No emulator run. `.planning/log/` was not read.

## Open issues / risks
1. **1.3:** six main-source `@Suppress` without a found code-auditor approval line: `SnapshotMapper`, `GamesIds`, `PlayGamesService`, `GameViewModel`, `ModeGlyphs` ×2. The test-side `UNCHECKED_CAST`/`unused`/`LargeClass` set has none either. The code-auditor reviews them; it is not the release-manager's call.
2. **3.6:** absolute `/Users/ermanergoz/…` paths in committed `.planning/` reports. These are not credentials, but the item's wording ("path, account identifier") reads strictly. The producer rules on it.
3. **3.5:** the per-coordinate diff against the 192 coordinates of the 2026-09-15 report, which also closes §5b and U-6, goes to the research-analyst. 3.5a, the NOTICE scan, is still owed.
4. **The Check ran with this branch's docs edits uncommitted on top.** Only `.md` files changed, nothing the Check reads.
5. 2.4 (date) is owed right before the merge to main. 2.5 is re-read if a TalkBack store line is added at Gate 2.

## Handoff
Next: producer —
- **qa-engineer:** the release device pass per READINESS §3. D-1 waits for the AAB; D-4..D-8 and D-13..D-16 can run now on a debug build on `ergoz_studio`.
- **code-auditor:** item 1.3's list.
- **research-analyst:** 3.5 diff, §5b/U-6.
- **tech-lead:** Conditions 7–9 and candy's D1 docs sync.
- **Founder items before Gate 2:**
  - F-7/4.1 real IDs; F-8/4.2 upload keystore → then 4.3 build, 4.4–4.7;
  - F-13 Console versionCode/certificate comparands;
  - F-2 test-device registration for §5;
  - the 1.7 route (script or Console);
  - Q-025 if still open (D-9).
