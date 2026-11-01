# 3.0.0 — founder runbook: from now to "uploaded to the internal track" (Console route)

Date: 2026-11-01. Author: release-manager. Route: **manual upload in the Play Console** (Q-029,
answered 2026-11-01; checklist 1.7). No value (ID, password, path, SHA-1) is ever written in a file,
typed into chat, or printed by any command below.

## A. Before the build (you)

1. **F-2 — start the 24 h clock now.** AdMob → Settings → Test devices: add the test phone's
   advertising ID. Nothing else waits on it until after the upload.
2. **F-7 / 4.1 — GDPR message.** AdMob → Privacy & messaging: confirm a consent message is
   *published* for Peg Solitaire.
3. **F-7 / 4.1 — `local.properties`.** The build runs from a clean checkout of the tag:
   `/Users/ermanergoz/Desktop/PegSolitarie-v3.0.0`, detached at `v3.0.0`. **Re-cut 2026-11-01:** the
   tag moved from `a3f2221` to the new `release/3.0.0` merge on `main` (the hash is in
   `.planning/reports/release-3.0.0/release-manager-recut.md`), and the release-manager moved this
   checkout to it with `git checkout --detach v3.0.0`; step B's guard still checks
   `git describe --exact-match` = `v3.0.0`. It has **no** `local.properties` yet (neither
   does `PegSolitarie-release`). Copy yours in, without opening it in chat:
   `! cp -p /Users/ermanergoz/Desktop/PegSolitarie/local.properties /Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/local.properties`
   (the file is git-ignored there).
4. **F-8 / 4.2 — keystore.** Have the original upload keystore file at an absolute path **outside**
   every checkout, and its alias and two passwords ready in your password manager.

## B. The build (you type one command)

Type this in this Claude Code session (the leading `!` runs it in your shell). It refuses to run
unless the checkout is exactly `v3.0.0` and `local.properties` is there, asks the four values with
hidden input, passes them only to that one Gradle process (`--no-daemon`, so nothing keeps them
afterwards), checks the real ad and Play Games IDs (4.1), builds the bundle (4.3), and prints only
the exit code. Nothing is written to a file or to shell history.

```
! bash -c 'cd /Users/ermanergoz/Desktop/PegSolitarie-v3.0.0 || exit 1; [ "$(git describe --exact-match HEAD 2>/dev/null)" = v3.0.0 ] && [ -s local.properties ] || { echo "STOP: not at v3.0.0 or local.properties missing"; exit 1; }; read -r -s -p "Keystore absolute path: " K; echo; read -r -s -p "Key alias: " A; echo; read -r -s -p "Key password: " KP; echo; read -r -s -p "Store password: " SP; echo; PLAY_UPLOAD_KEYSTORE="$K" PLAY_UPLOAD_KEY_ALIAS="$A" PLAY_UPLOAD_KEY_PASSWORD="$KP" PLAY_UPLOAD_STORE_PASSWORD="$SP" ./gradlew --no-daemon --console=plain :app:verifyReleaseAdIds :app:verifyReleaseGamesIds :app:bundleRelease; echo "exit=$?"'
```

If the session does not let you type at the hidden prompts, open Terminal.app and paste the same
line without the leading `!`. Then tell the producer "built, exit=<n>". Nothing more.

## C. What the studio does with the build (no action from you)

The bundle is `/Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/app/build/outputs/bundle/release/app-release.aab`.
The release-manager records its sha256 (`shasum -a 256 <aab>`) and versionName (4.6), then:
4.4 signer SHA-1 (you compare it in step D), 4.5 R8 evidence, 4.6a fonts and licence texts, 4.7
exactly one artifact with the 4.6 hash, 3.5a post-bundle NOTICE check; the qa-engineer runs D-1
(palette + TalkBack on the AAB) and D-16 (store screenshots from this candidate). Then the
producer writes the one-screen summary and asks you **Q-PUBLISH-3.0.0**.

**4.6a's Fredoka half, if you check it yourself** (the studio does not build the AAB; founder,
2026-11-01). The two shipped Fredoka files must equal the recipe's outputs byte for byte:

```
! python3 -I -c "import json,hashlib,zipfile; d='/Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/'; z=zipfile.ZipFile(d+'app/build/outputs/bundle/release/app-release.aab'); r=json.load(open(d+'fonts/fredoka-tr/recipe.json')); [print(w['output'].split('/')[-1], 'match' if hashlib.sha256(z.read('base/res/font/'+w['output'].split('/')[-1])).hexdigest()==w['output_sha256'] else 'DIFFERS') for w in r['weights']]"
```

Expected: `fredoka_medium.ttf match`, `fredoka_semibold.ttf match` (`0effcf32…1b157d4f`, 50,444 B;
`5c764171…4890823c`, 50,408 B). `DIFFERS`, or a `KeyError` (the file is not in the bundle), stops the upload.

## D. Comparands (you, when the producer asks — F-13)

Play Console → App bundle explorer / App integrity and AdMob → Apps: compare, as the studio
instructs, the highest published versionCode (expected 2; 3.0.0 is 30000), the upload-key SHA-1,
and the three ad-unit IDs. Answer "match" or "differs". Send no values.

## E. After you approve Q-PUBLISH-3.0.0 (you, in the Console)

1. **F-3 — Play App Signing.** Play Console → App integrity: enrol (keep the existing key). Make
   sure every distinct SHA-1 (app-signing, upload, debug) has a Play Games Android credential;
   publish the PGS configuration; both your accounts on the PGS tester list.
2. **Upload.** Play Console → Testing → Internal testing → Create new release → upload the AAB at
   the path in C. Before you press Save, the producer gives you the 4.6 sha256 once more: run
   `! shasum -a 256 /Users/ermanergoz/Desktop/PegSolitarie-v3.0.0/app/build/outputs/bundle/release/app-release.aab`
   and confirm it equals that hash (T-3). Release notes from `CHANGELOG.md` 3.0.0. Roll out to
   internal testing.
3. **F-5 — App content.** Policy → App content: Data safety, Ads, Target audience 13+, Content
   rating, Advertising ID, from `docs/store/`, privacy policy open beside it.
4. **F-2 check.** At least 24 h after step A1, install the internal-track build on the test phone;
   the first ad must say "Test Ad". If not: tap nothing, tell the producer. Then the qa-engineer
   runs §5.

Done = "uploaded to the internal track". Tell the producer the date; the upload record goes on
`feature/publish-3.0.0-done`.

Not applicable on this route (3.0.0): T-1 key placement, T-6 command, T-11 invocation rule — no
service-account key and no `./scripts/play-upload.sh` run.
