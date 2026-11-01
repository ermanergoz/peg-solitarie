# release-manager report — release-3.0.0 (re-cut)
Date: 2026-11-01
Branch: release/3.0.0 (af323e8..this commit), cut from develop 26b56bf
Skills loaded: ergoz-soft:standards (studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md, lessons.md), gitflow
Ran in parallel with: none
Loops: 0
Lessons applied: none

## Summary
3.0.0 was re-cut from `develop` 26b56bf so that it carries the four founder changes (Turkish
interface, immersive Gameplay/Results, Home stage withdrawn with the live Classic Continue chip,
Challenge star bands with a 30-level shift). The store copy, release notes, THIRD_PARTY.md,
CHANGELOG.md and checklist are updated. The release-tier Check is green, with 0 fuzz failures.
**The founder took over QA and sign-off (2026-11-01) and signed off the release;** no
qa-engineer, code-auditor or tech-lead step ran. The AAB was not built, on the founder's
instruction ("Don't build aab I will do it").

## Re-cut mechanics and reasoning
- The old `v3.0.0` (main a3f2221) was local only, with no remote and no upload, so moving the tag
  breaks nobody. `main` was already back-merged into develop (`git log main --not develop` is
  empty).
- The old `release/3.0.0` (d1bc673) had one commit that was not on develop: the Q-029 runbook and
  the gate-2 prep note. It was cherry-picked (`-x`) onto the new branch as af323e8. The old branch
  was renamed `retired/release-3.0.0-d1bc673` and is deleted after the merge, since its only
  unique commit is now carried.
- The new `release/3.0.0` was cut from develop 26b56bf into the `PegSolitarie-release` worktree.
  It is merged to `main` with `--no-ff`, `v3.0.0` is moved with `git tag -f` to that merge, `main`
  is merged back to develop with `--no-ff`, and the release branch is deleted. Proof: run
  `git rev-parse v3.0.0^2`, which prints this branch's tip, and check that
  `git tag --merged develop` lists v3.0.0. Rewriting `main` was rejected: a second merge on top of
  a3f2221 keeps history append-only, and only the unpublished tag moves.
- The `PegSolitarie-v3.0.0` build checkout (detached, holding the founder's git-ignored
  `local.properties`) is moved to the new tag with `git checkout --detach v3.0.0`.

## Outputs
- docs/store/tr-TR/short_description.txt, full_description.txt: `docs/ux/localisation-tr.md`
  §10.1/§10.2 verbatim, in the sen voice, without "Oyunun arayüzü İngilizcedir." (66 and 2,311 chars).
- docs/store/en-US/changelogs/30000.txt (472 chars), docs/store/tr-TR/changelogs/30000.txt
  (464 chars): add "Turkish interface" / "Türkçe arayüz", the star bands, full-screen play and the
  Home Continue change. The tr-TR file is in the sen voice.
- docs/store/README.md: the "no Turkish strings" paragraph is replaced, and the lengths are
  re-counted.
- THIRD_PARTY.md: the §2 OFL verdict note (old line 99), condition (3) (old 115–117) and the OFL
  §3 summary (old 160) now read Fredoka as an OFL-1.1 Modified Version (no RFN, name kept;
  Nunito unmodified), consistent with the §2 row. §8 build tools checked: fontTools 4.60.1 MIT and
  opentype-sanitizer 9.2.0 BSD-3-Clause, matching `fonts/fredoka-tr/requirements.txt`; build-only,
  nothing shipped. Every row has a licence. ip-policy: PASS.
- CHANGELOG.md [3.0.0]: re-cut note with the founder sign-off, plus four "(re-cut)" bullets
  (Added: Turkish; Changed: full-screen, Home/Continue, star bands; Under the hood: generator v2,
  LEVEL_OFFSET 30, Fredoka Modified Version, system-bar seam).
- docs/release-checklist.md: 1.2, 1.3, 1.5, 1.6, 2.1–2.7, 2.4a, 3.1–3.4, 3.5, 3.6, 3.7 and 4.8
  ticked or re-verified with re-cut evidence. 4.3–4.7, 4.6a and 3.5a's post-bundle half are left
  open for the founder's AAB. [F] items are untouched.
- .planning/release/3.0.0/FOUNDER-RUNBOOK.md: moves the build checkout to the re-cut tag, and adds
  the Fredoka 4.6a hash command.
- Commits: af323e8 (cherry-pick), a11780d `chore(release): 3.0.0`, 8256ecc (checklist and runbook),
  plus this report.

## Checks
- `FUZZ_SEEDS=100000 ./scripts/check.sh --offline` (on release/3.0.0, tree = develop 26b56bf + docs): pass (exit 0, BUILD SUCCESSFUL in 11m 17s; `:domain:fuzz` executed: "fuzz: requested 100000 exceeds the level space 10000; running the whole space (exhaustive)", "fuzz: examined 10000 levels (requested 100000), determinism pass, 0 failures"). The run was without `--rerun-tasks` (50 tasks from cache), but fuzz, koverVerify and all eight literal gates executed. Load average 5.27 at the start and 5.11 at the end.
- `test ! -e domain/build/fuzz-failures.txt`: pass (absent).
- `./gradlew :domain:koverXmlReport --offline`: pass (LINE 99.4 %, 934/940, bound 90).
- `git diff a3f2221..HEAD` suppression grep: 0 suppress/ignore; 2 `!!` in tests only.
- `git diff a3f2221..HEAD` secrets grep outside `.planning/`: pass (0).
- Dependency delta `a3f2221..26b56bf`: none (`libs.versions.toml` and `build-logic` untouched).
- Store word-rule greps (en-US and tr-TR, `docs/store/README.md`): pass (silent).
- Fredoka hash command run on a test zip built from `app/src/main/res/font/`: pass (`fredoka_medium.ttf match`, `fredoka_semibold.ttf match`).
- `git tag --merged develop | grep -x v3.0.0` after the back-merge: shown in the producer handoff.

## Founder build (the studio did not build)
1. In AdMob, confirm the test device and consent message (runbook A1–A2). `local.properties` is
   already in `/Users/ermanergoz/Desktop/PegSolitarie-v3.0.0`.
2. Build: runbook step B, the one-line `! bash -c '…'` command. It checks that the checkout is
   exactly `v3.0.0`, asks for the four `PLAY_UPLOAD_*` values hidden, and runs
   `./gradlew --no-daemon --console=plain :app:verifyReleaseAdIds :app:verifyReleaseGamesIds :app:bundleRelease`.
   The artifact is
   `/Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/app/build/outputs/bundle/release/app-release.aab`.
   (`./gradlew :app:assembleRelease` only makes an APK, which neither upload route uses.)
3. Hash the bundle: `shasum -a 256 <aab>` (4.6/4.7).
4. 4.6a, the Fredoka half (each file's sha256 against `fonts/fredoka-tr/recipe.json` `weights[].output_sha256`):

       ! python3 -I -c "import json,hashlib,zipfile; d='/Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/'; z=zipfile.ZipFile(d+'app/build/outputs/bundle/release/app-release.aab'); r=json.load(open(d+'fonts/fredoka-tr/recipe.json')); [print(w['output'].split('/')[-1], 'match' if hashlib.sha256(z.read('base/res/font/'+w['output'].split('/')[-1])).hexdigest()==w['output_sha256'] else 'DIFFERS') for w in r['weights']]"

   The expected output is `fredoka_medium.ttf match` and `fredoka_semibold.ttf match`
   (`0effcf32…1b157d4f`, `5c764171…4890823c`). The rest of 4.6a (`unzip -l <aab> | grep -E
   'base/res/font/|base/assets/licenses/'`) is listed in the checklist row.

## Draft for the producer (Q-PUBLISH-3.0.0)

    ## Q-PUBLISH-3.0.0 — Approve 3.0.0 (re-cut) for the Play internal track?   (open)
    Date: 2026-11-01    Asked by: release-manager (via the producer)
    Context: 3.0.0 re-cut from develop 26b56bf with Turkish, immersive play, Home Continue and Challenge star
    bands; versionName 3.0.0, versionCode 30000 (> published 2); tag v3.0.0 moved to the new main merge (never
    pushed or uploaded). Artifact: /Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/app/build/outputs/bundle/
    release/app-release.aab, built by you (runbook B); release check green, fuzz 10000/10000, 0 failures.
    Known risks: (1) no code-auditor review of the immersive, star-band and Turkish-font work (your sign-off
    replaces it); (2) the below-API-30 inset source (stable insets read from the root view) is unruled;
    (3) Challenge levels 3, 5, 6, 10 and 13 can be lost with one wrong first jump; (4) skill achievements can
    unlock on an easy 1-star clear (three-star rule deferred to 3.0.1, Q-040); (5) LicenseCatalogTest carries one
    exception, for "Peg Solitaire" in the Fredoka licence line (the app named as modifier).
    Route (Q-029): manual upload in the Play Console, runbook E. Internal testing → Create release → the AAB
    above; before Save, check `shasum -a 256` against the 4.6 hash; release notes from docs/store/*/changelogs/
    30000.txt.
    Options: 1) Approve (create .planning/approvals/publish-3.0.0.approved with /ergoz-soft:approve publish 3.0.0)
    2) Hold for fixes to risks 2–4 in 3.0.1 first 3) Reject
    Recommendation: release-manager — 1, after the 4.4 SHA-1, 4.6a font hashes and 4.1a ad IDs read "match".
    Blocks: the 3.0.0 upload
    Answer:

## Open issues / risks
- The five risks above.
- 3.1 ran without `--rerun-tasks` (the gate's evidence lines executed anyway). Load average was
  slightly above 5.0.
- [F] items: 2.3 published comparand, 4.1, 4.1a, 4.2, 4.4. §5 track checks stay open.
- Not created: `.planning/approvals/publish-3.0.0.approved`. Nothing was uploaded.

## Handoff
Next: producer. Relay the founder build steps (runbook B, C), then queue Q-PUBLISH-3.0.0 from the
draft above once the founder reports the build's exit code and hashes.
