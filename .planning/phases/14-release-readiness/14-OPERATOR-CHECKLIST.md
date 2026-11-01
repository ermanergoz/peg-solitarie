---
phase: 14-release-readiness
type: operator-checklist
status: pending
blocks: [PLAT-08, REL-01, REL-02, REL-03, REL-04]
generated_by: release-manager (phase 14 external track)
---

# Phase 14 Operator Checklist — the release systems only you can reach

## Why this exists

Every other phase of this milestone could be finished by the studio on this machine. This one
cannot. Five of phase 14's six deliverables live inside systems that hold no local credentials:
the Play Console, the AdMob UI, the developer website, the release keystore, and a second Google
account that does not yet exist. The studio can write every document, every script and every
build gate around them — and has — but it cannot execute one of them.

The consequence is blunt and worth stating before the first checkbox: **no signed release
artifact of any kind can be produced on this machine today.** Two fail-fast Gradle gates
(`verifyReleaseAdIds`, `verifyReleaseGamesIds`) throw at configuration time when the real AdMob
ad-unit IDs are absent. A third gate, the release `signingConfig` (on `develop` since 9ca3829),
fails a release build when the four `PLAY_UPLOAD_*` variables are absent. That is not a defect;
it is the design working. But it means
ROADMAP success criterion 1 — *every feature re-verified from a downloaded signed build* — is
gated end to end on Step 6 and Step 7 below, and nothing the studio does in the meantime can
substitute for them.

Two corrections to what you may remember about this list:

- **The developer website already exists and already serves a live privacy policy.** The
  ROADMAP's External Prerequisites table still calls it *"the longest lead item — nothing else
  here can start until it does. Does not exist yet."* That was true when the table was written
  and stopped being true on 2026-08-27 (`06-BUILD-EVIDENCE.md`, external prerequisites table,
  item 3) and demonstrably so on 2026-09-02, when the policy page was opened from the app on a
  device and observed headed *"Privacy Policy — Last updated: September 2, 2026"*
  (`09-DEVICE-EVIDENCE.md` item 7, REL-05 PASS). The milestone's single biggest schedule risk
  has already been retired. What remains on that website is two files' worth of work, not a
  hosting project.
- **The "two different PGS certificates" note in the ROADMAP needs reading against D-21.** You
  hold the original release keystore and this app is **not** enrolled in Play App Signing
  (`06-CONTEXT.md` D-21), so there is no separate Play-App-Signing certificate yet. Enrolment
  happens at this upload, and Step 3 below is written for that reality rather than for the
  general case.

Work top to bottom. The order is lead time, longest first — not importance. Steps 0 through 4
have latency you cannot compress and should be started in one sitting today; Steps 5 through 8
are quick but sequenced behind something. One exception: Step 7a item A (registering the test
phone with AdMob) needs 24 hours to propagate, so do it in that same first sitting.

Every checkbox starts unticked. Tick one only after you have observed the thing yourself: a
pre-ticked box in an operator checklist is a false claim about a system nobody else can see.

---

## Step 0 — `app-ads.txt` on the developer website, and the listing field that points at it

**Blocks:** REL-03 in full, and with it ROADMAP success criterion 2.
**Time:** ~10 minutes of work, then **up to 24 hours of crawl latency**, then the AdMob status
flips. This is the longest-latency item on the list; start it before anything else on this page.
**Why it matters beyond the checkbox:** without a verified `app-ads.txt`, programmatic buyers
that honour the IAB spec treat this app's inventory as unauthorised, which suppresses demand and
therefore revenue on exactly the two ad surfaces phases 9 and 10 built.

1. [ ] Find your AdMob **publisher ID** — AdMob → Settings → Account information. It has the
       form `pub-` followed by 16 digits, and it is the same `pub-…` block that appears in the
       middle of the `ADMOB_APP_ID` value you will paste in Step 6. Do not write it into this
       file or any file under `.planning/`.
2. [ ] Publish a plain-text file at the **root** of the developer website, reachable at
       `https://<your-domain>/app-ads.txt`, containing exactly one line:

           google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0

       `f08c47fec0942fa0` is Google's certification authority ID and is the same for every
       publisher — it is not a secret and is not specific to you. Replace only the `pub-…`
       field. One line, no BOM, `Content-Type: text/plain`.
3. [ ] Confirm the file is served **directly, with a 200 and no redirect**, from the host that
       the Play listing's website field names:

           curl -sS -o /dev/null -w '%{http_code} %{num_redirects} %{url_effective}\n' https://<host>/app-ads.txt

       A `200` with `0` redirects is what you want.
4. [x] **Which host the store listing names — settled 2026-09-21.** *(Corrected 2026-10-17 from
       `.planning/reports/phase-15/release-manager-app-ads-diagnosis.md`; the earlier text of this
       item was wrong on two counts, below.)* AdMob discovers `app-ads.txt` by reading the
       developer-website URL **off the store listing**, so the file has to be reachable from the
       root of *that* host.
       - **The field was empty until 2026-09-21.** The public Play page for
         `com.erman.pegsolitarie`, fetched in three locales that day, showed only a support email
         and a privacy-policy link under **App support**; the word `Website` was not on the page.
         Google's own confirmation test for this field is "check that the Developer Website URL is
         linked in the 'app support' of your Android app page" (AdMob Help 9363762). With no
         hostname there is no crawl, which is why the status never flipped although the file had
         been live and correct on `https://ermanergoz.com/app-ads.txt` since 2026-09-02.
       - **You filled it; it is live as `https://ermanergoz.github.io`** — observed on the public
         page at 2026-09-21T01:29:28Z and re-observed 2026-10-17. That host answers
         `/app-ads.txt` with GitHub Pages' custom-domain `301` to
         `https://ermanergoz.com/app-ads.txt`, which serves the correct record with a `200`.
         AdMob Help 9363762, "Web server redirects", lists "Another domain, including multiple
         subdomains" among valid redirect destinations, so this is expected to verify. **Leave
         the field alone**: every edit restarts the 24-hour crawl clock.
       - **Do not try to serve `app-ads.txt` on `ermanergoz.github.io` itself** — an earlier
         revision of this item recommended serving it on both hosts. That is not possible here:
         the site repo ships a `CNAME` of `ermanergoz.com` in `public/` and `dist/` and deploys
         with `gh-pages -d dist`, and GitHub Pages 301-redirects the whole `*.github.io` host
         once a custom domain is set. There is no per-path exception. The 301 itself was never
         the fault.
       - **Only if item 5 still shows unverified 48 hours after the field went live** (that mark
         was 2026-09-23T01:29Z, long past): change the field to `https://ermanergoz.com` (bare
         apex, https, no path, no `www.`) — it serves the file with a direct 200 and, having no
         subdomain, keeps Google's "probe up to one subdomain level" rule from walking up to the
         shared `github.io` suffix. Then wait 24 hours again. If it still does not flip, confirm
         in AdMob → Apps → this app → App settings that the app entry is **linked to the Play
         listing** (an unlinked, manually added entry has no listing to read the website from),
         and that AdMob → Settings → Account information shows the same `pub-…` as the file.
5. [ ] After the crawl, confirm **AdMob → Apps → this app → App settings → `app-ads.txt`**
       reports the app as **authorised/verified** (the wording differs by UI revision; what you
       need is the positive state, not the "not found" or "unauthorised" banner).
6. [ ] Record the date you completed item 2 and the date the status flipped. If 48 hours pass
       with no flip, the cause is almost always item 4 — the crawler is reading a different host
       than the one you published on.

**How to tell it is done:** AdMob shows the app as authorised **and** the curl in item 3 returns
`200 0` for the exact host in the listing's website field. Either one alone is not done.

---

## Step 1 — Store listing assets that describe the app that is actually shipping

**Blocks:** nothing mechanically, but it is a Play policy exposure and the only open-ended item
here.
**Time:** unknown until you look — minutes if the listing is already current, art turnaround if
it is not.

This upload is a **complete rewrite**. The listing on the store today was written for the
build at versionCode 2 (versionName "2.0"), which is a different application: different UI,
different mode structure, no Challenge ladder, no leaderboards, no sound. Play policy requires
screenshots to represent the app as it actually is.

1. [ ] Open Play Console → Store presence → Store listing and **read the current short
       description, full description and screenshots**. Current content: **unverified** — the
       studio has never seen this listing.
2. [ ] Decide, and record in `docs/store/` (the studio has prepared the text files and the
       character limits there): does the current copy still describe the app? Does any
       screenshot show a screen that no longer exists?
3. [ ] If new screenshots are needed: **at least 4 phone screenshots**, and they must come from
       this build, not from the old app. The studio can capture them from an emulator once
       Step 6 unblocks a build — this item is then yours only to approve, not to produce.
4. [ ] Confirm the **512×512 icon** and the **1024×500 feature graphic** on the listing still
       match the app's current identity (the adaptive launcher icon in this repo is drawn
       entirely in-repo as vector XML — `ic_launcher_foreground.xml` and friends — so if the
       listing icon was exported from the old app, it is now inconsistent with what a player
       sees on their home screen).

**How to tell it is done:** every screenshot on the listing shows a screen that exists in the
build you are about to upload, and `docs/store/` records the four texts you settled on with
their character counts inside the limits.

---

## Step 2 — Revise the privacy policy so it covers phases 12 and 13

**Blocks:** REL-04, and therefore REL-01 (Step 5 is written *from* this text, not beside it).
**Time:** 1–2 hours of authoring, minutes to publish.

The live policy is dated **September 2, 2026** — observed on a device that day
(`09-DEVICE-EVIDENCE.md` item 7). That date is *before* phase 12 (Play Games leaderboards and
achievements, merged 2026-09-13) and *before* phase 13 (cloud save over Play Games Saved Games,
merged 2026-09-20). **A policy written on 2026-09-02 therefore cannot name the data flows those
two phases added, because they did not exist yet.** This is the single most likely REL-04 failure
and it is invisible from the Console: the URL is live, the page loads, the Data-safety form
accepts it, and the text is simply incomplete.

Revise it so it names, explicitly:

1. [ ] **Advertising ID collection.** The ads SDK pulls in
       `com.google.android.gms.permission.AD_ID` (recorded in phase 6's build evidence). The
       policy must say the advertising identifier is collected and what it is used for. "We may
       collect device identifiers" is not enough for the Data-safety cross-check in Step 5,
       because that form asks about the advertising ID by name.
2. [ ] **Every third-party SDK that receives player data**, by name. For this build that is,
       exhaustively:
       - **Google Mobile Ads (GMA Next-Gen)** — ad requests, advertising ID, device and coarse
         location-derived signals.
       - **Google User Messaging Platform (UMP)** — consent state, stored on device and read by
         the ads SDK.
       - **Google Play Games Services v2** — Play Games player ID, and the leaderboard scores
         and achievement progress the app submits (phase 12).
       - **Google Play Games Saved Games** — the cloud save payload (phase 13): per-level star
         and score records, the Challenge ladder position, and the resume snapshot. Stored in
         the player's own Google Drive application-data folder under the `drive.appdata` scope.
         This one is new since the current policy text and is the most substantive omission.
3. [ ] **What is stored on the device and never leaves it** — the local Room database and
       DataStore settings — stated as such, so the Data-safety form's "collected" versus
       "not collected" split (Play's definition of *collected* is "sent off the device") has a
       source of truth to agree with.
4. [ ] **Account deletion / data deletion route.** The app has one: signing into a different
       Play Games account offers a wipe (D-13/D-14), and the Play Games Saved Games data can be
       deleted by the player from Drive → Settings → Manage apps. Say so.
5. [ ] Publish the revision, bump the "Last updated" date, and **confirm the URL compiled into
       the app still resolves to the revised page**:

           curl -sS -o /dev/null -w '%{http_code} %{num_redirects}\n' "$(grep -o 'https://[^"]*' app/src/main/java/com/app/pegsolitaire/ui/settings/PrivacyLinks.kt)"

       The constant is a compile-time `const val` that must not change (it is deliberately
       immutable for an intent-redirection reason documented in its KDoc) — so the **page**
       changes, never the URL.
6. [ ] Confirm the same URL is in Play Console → **App content → Privacy policy**, and in the
       store listing.

**How to tell it is done:** you can point at a sentence in the published policy for each of the
four SDKs in item 2 and for the advertising ID in item 1, and the "Last updated" date is later
than 2026-09-20.

---

## Step 3 — Play App Signing enrolment, and the PGS OAuth credential for each distinct SHA-1

**Blocks:** REL-02 (a signed *track download* cannot exist before this), and Step 8.
**Time:** ~20 minutes of console work, then Play Games Services configuration propagation
(minutes to a couple of hours — the same propagation that bit Step 0 item 6 of the phase 12
checklist).

This is the classic "works on my machine, fails from Play" failure, and the ROADMAP flags it for
that reason. Read it against **D-21**: you hold the **original release keystore** and this app is
**not yet enrolled in Play App Signing**, so today only two certificates exist — the original
release keystore and the debug keystore — and the PGS credentials that exist were created for
those (`12-OPERATOR-CHECKLIST.md` Step 0 item 4; note that checklist's wording says "Play App
Signing SHA-1", which per D-21 was the original-keystore SHA-1 in practice). Enrolment creates
the third possibility.

1. [ ] **Enrol this app in Play App Signing at this upload** (D-21). When the Console offers it,
       upload/adopt the **existing original release key** as the app signing key — do not let
       Google generate a fresh one unless you deliberately want to rotate, because adopting the
       existing key keeps every already-installed copy upgradable and keeps the PGS credential
       you already registered valid.
2. [ ] After enrolling, read **both** values from Play Console → **Test and release → App
       integrity → App signing**:
       - **App signing key certificate → SHA-1.** This is the certificate Google re-signs with,
         and therefore **the one the APK a tester downloads from the internal track is actually
         signed with.** It is the only one a real track download exercises.
       - **Upload key certificate → SHA-1.** This is what your local artifact must be signed
         with for the Console to accept the upload.
3. [ ] **Compare the two SHA-1 values.** If you adopted the existing key in item 1 they may be
       **identical**, in which case one PGS OAuth credential covers both roles — and that is the
       correct outcome, not a mistake. Record which it is:
       - [ ] app signing SHA-1 and upload SHA-1 are **identical** → one credential needed
       - [ ] they **differ** → a PGS OAuth Android credential is needed for **each**
4. [ ] For **every distinct** SHA-1 in item 3, plus the **debug keystore** SHA-1 that already has
       one, confirm a Play-Games-Services Android credential exists **created through Play
       Console, never directly in Google Cloud** (the phase 12 checklist explains why: a
       Cloud-created client is not associated with this game's leaderboards, so sign-in appears
       to work and every submission silently goes nowhere).
       `keytool -list -keystore ~/.android/debug.keystore -v` (alias `androiddebugkey`, password
       `android`) prints the debug one for comparison.
5. [ ] **Publish the Play Games Services configuration again** after any credential change.
       Configuring and publishing are separate actions and the Console will leave a configured-
       but-unpublished change sitting indefinitely — the failure mode documented twice already
       in this project (`12-OPERATOR-CHECKLIST.md` Step 0 item 3, `13-OPERATOR-CHECKLIST.md`
       Step 0 item 2), and it presents as a code bug both times.
6. [ ] Confirm the **tester list** still holds your own account and add the second account from
       Step 4. Propagation takes up to a couple of hours; the account that created the game does
       **not** automatically become a tester.

**How to tell it is done:** Play Console → App integrity shows both certificates; the number of
PGS Android credentials equals the number of *distinct* SHA-1s among {app signing, upload, debug};
and the PGS configuration shows **Published** with no pending draft.

**How you will know it is NOT done, later and expensively:** sign-in works on your `installDebug`
build and fails on the internal-track download. If that happens, come back to item 2 before
opening a single Kotlin file.

---

## Step 4 — A second Google account, on the tester list

**Blocks:** the account-swap / D-14 wipe UAT that the phase 13 tech-lead sign-off requires before
v1.1 ships.
**Time:** 10 minutes to create, up to a couple of hours of tester propagation.

`ProfileSwapDialog.kt` plus `resetAllProgress` / `ResumeDao.clearAll` / `LevelResultDao.deleteAll`
is **the one destructive path in the whole milestone**, and it has never run on a device. Phase 13
recorded that honestly rather than papering over it (`13-OPERATOR-CHECKLIST.md` Step 3), because
producing it needs a second Google account that has never authorised this game — and one did not
exist. The tech-lead's sign-off then made closing it a v1.1 requirement, not a phase-13 one:
*"Required before v1.1: one UAT with a second Play Games account, covering keep-local,
replace-with-cloud and walk-away."*

1. [ ] Create (or nominate) a **second Google account that has never signed into this game**. A
       throwaway is fine; what matters is that Play Games has no player record for it against
       this project.
2. [ ] Add it to the Play Games Services **tester list** (Step 3 item 6) and wait for
       propagation.
3. [ ] Confirm it can sign into Play Games on an emulator at all — one launch of the Play Games
       app is enough. An account that cannot sign in produces the same symptom as the dialog not
       firing.
4. [ ] Tell the studio it is ready. The UAT itself is QA's job, not yours; you are providing the
       account, not running the test.

**How to tell it is done:** the second account is signed in on a second emulator and appears in
the tester list. Note the propagation caveat — "added" and "propagated" are not the same state,
and the difference is the couple of hours that phase 12 warned about.

---

## Step 5 — Data safety, ads, and target audience — written FROM the policy

**Blocks:** REL-01, and with it ROADMAP success criterion 3 and 3a.
**Time:** 45–90 minutes. **Must come after Step 2**, and that ordering is the point of this step.

The ROADMAP is explicit and the reason is worth restating: *"REL-04's policy text and REL-01's
Data-safety answers are a single source of truth split across two systems … Write the Data-safety
answers from the policy, not alongside it."* Answering the form from memory and then editing the
policy to match is how the two drift, and a mismatch between them is one of the most common
reasons a Play release is rejected or later pulled.

Work with the revised policy page open beside the form.

1. [ ] **App content → Data safety.** For each data type you declare, the answer must be
       traceable to a sentence in the policy. At minimum, and consistent with Step 2:
       - **Device or other IDs → Advertising ID**: collected, shared with third parties, used for
         advertising. Not optional — the `AD_ID` permission is in the merged manifest.
       - **App activity / in-app actions or "Other app performance data"** as appropriate for the
         scores, stars and ladder position submitted to Play Games (phase 12) and written to
         Saved Games (phase 13). These *are* "collected" by Play's definition, because they are
         sent off the device.
       - Everything held only in Room and DataStore: **not** collected.
       - Declare **data is encrypted in transit** (all four SDKs use HTTPS) and describe the
         deletion route from Step 2 item 4.
2. [ ] **App content → Ads.** Declare that the app **contains ads**. It contains both rewarded
       and interstitial formats.
3. [ ] **App content → Target audience and content.** This is a puzzle game with ads; if you
       declare any audience under 13, Play's Families policy imposes ad-SDK and ad-content
       constraints this build has not been designed or tested against. Declare a **13+**
       audience unless you have decided otherwise deliberately — and if you have, say so, because
       it changes engineering requirements, not just a form.
4. [ ] **App content → Content rating** questionnaire. Answer it honestly for a peg-solitaire
       game with ads and no user-generated content, no communication features and no purchases.
       Current state of all four sections: **unverified** — they were filled for the old app at
       some point and their answers are not knowable from this repo.
5. [ ] **App content → Advertising ID declaration** (the separate AD_ID permission declaration
       Play requires): declare the app uses the advertising ID, purpose = advertising/marketing.
       This is the third place the same fact has to be stated consistently, after the policy and
       the Data-safety form.
6. [ ] **News app / Government app / Financial features / Health**: all no.
7. [ ] Re-read the Data-safety summary Play generates and check it sentence by sentence against
       the policy page. Any statement in one that is absent from the other is the finding.

**How to tell it is done:** every Data-safety answer can be pointed at a sentence in the policy,
and every SDK named in the policy appears somewhere in the form's answers. The two documents say
the same thing in two registers.

---

## Step 6 — The real AdMob IDs, in your untracked `local.properties`

**Blocks:** *every signed artifact.* Nothing downstream of this exists without it: no AAB, no
internal-track upload, no track download, no REL-02, no release-variant cold-start measurement.
**Time:** 5 minutes.

Phase 13's gate evidence records the exact shape of this blocker: the release build was exercised
with **Google's public test ad-unit IDs** because *"this machine holds no real AdMob ids"*, and
that report says plainly the resulting artifact *"is not a release-candidate artifact and must not
be uploaded; the real signed build is phase 14's job with the founder's own ids"*
(`producer-gate-evidence.md`).

1. [ ] Create the two ad units in AdMob if they do not exist: one **rewarded** (hint/undo/retry)
       and one **interstitial** (between levels).
2. [ ] Paste four values into your own **untracked** `local.properties`, replacing the
       `REPLACE_ME_…` placeholders that `local.properties.template` documents:
       `ADMOB_APP_ID` (the `ca-app-pub-…~…` form, tilde not slash), `ADMOB_REWARDED_AD_UNIT_ID`,
       `ADMOB_INTERSTITIAL_AD_UNIT_ID`, `PGS_PROJECT_ID`.
3. [ ] Confirm the 27 Play Games IDs from phase 12 are **still** in that file (they were recorded
       there on 2026-09-15 and the file is untracked, so a machine reset or a fresh clone loses
       them silently). `verifyReleaseGamesIds` names every missing key if they are gone.
4. [ ] Confirm the **UMP consent message** is still configured in the AdMob UI. UMP renders
       nothing without it, and phase 9's consent flow is upstream of every ad request.
5. [ ] Verify without building anything you could accidentally ship:

           ./gradlew :app:verifyReleaseAdIds :app:verifyReleaseGamesIds

       Both tasks must succeed. They are pure checks — they package nothing.
6. [ ] `git status --short` must show **nothing new**. If `local.properties` appears, stop: it
       should be gitignored and something is wrong with the working tree.

**How to tell it is done:** item 5 exits 0 and item 6 is clean.

**Do not** leave test IDs in place "just to get a build". Phase 6 recorded exactly that risk and
the cleanup it required: *"Leaving them in place would have let a future real release silently ship
test IDs — the one failure mode the presence-based gate cannot detect."*

---

## Step 7 — The release keystore, reachable by the build

**Blocks:** every signed artifact, together with Step 6.
**Time:** 10 minutes.

The `signingConfig` exists on `develop` (merged as `feature/phase-14-signing-config`, 9ca3829).
It reads the four environment variables below and nothing else: no committed keystore and no
debug-keystore fallback (D-16: *"a committed debug-keystore shortcut is exactly the kind of thing
that survives to a store upload"*). A release invocation **fails the build** when any of them is
absent, and `verifyReleaseSigning` repeats the check before packaging. Debug builds, `check`
and IDE sync never need them. The studio cannot supply their values.

1. [ ] Locate the **original release keystore** (D-21). Every upload must be signed with that
       exact keystore, or Play rejects it outright and there is no recovery path except a key
       reset request.
2. [ ] Keep it **outside this repository**. `.gitignore` already blocks `*.jks`, `*.keystore` and
       `keystore.properties`, but "blocked by gitignore" is not the same as "not in the working
       tree" — a keystore inside the repo directory is one `git add -f` or one archive away from
       leaking.
3. [ ] Export four environment variables in the shell that will run the build, and only there:
       `PLAY_UPLOAD_KEYSTORE` (absolute path), `PLAY_UPLOAD_KEY_ALIAS`,
       `PLAY_UPLOAD_KEY_PASSWORD`, `PLAY_UPLOAD_STORE_PASSWORD`.
4. [ ] Confirm the keystore and alias are the right ones **before** the build, by comparing the
       fingerprint against the upload certificate from Step 3 item 2:

           keytool -list -keystore "$PLAY_UPLOAD_KEYSTORE" -alias "$PLAY_UPLOAD_KEY_ALIAS" -v | grep SHA1

       The SHA-1 printed must equal the **upload key certificate** SHA-1 in Play Console.
5. [ ] Back the keystore up somewhere you would still have it after losing this machine. Losing
       it after enrolment is survivable (Play offers a key reset); losing it before is not.

**How to tell it is done:** item 4's SHA-1 matches the Console's upload certificate exactly.

---

## Step 7a — Register the test phone as an AdMob test device, and check the "Test Ad" label

**Blocks:** `docs/release-checklist.md` row 5.0, and therefore 5.6, 5.7, 5.8 and 5.8a: every ad
check on the store copy.
**Time:** 5 minutes of work, then **24 hours of propagation**. Item A has the second-longest lead
time on this page: do it in the same sitting as Steps 0 to 4, and in any case at least 24 hours
before Step 8 item 4 installs the internal-track build on the phone.
**Why:** a release build carries the real ad unit IDs. On a phone that is not a registered test
device it serves **live** ads, and a verification session's worth of live impressions, or one tap
on a live ad, is invalid traffic that can close the AdMob account for good, AdSense with it. The
reasoning, the policy text and the sources are in `docs/ad-testing-safety.md` (§3 for these steps,
§4 for the policy). The one rule: **never tap an ad that does not carry the "Test Ad" label.**

These are the ad steps A to E, lifted from `docs/ad-testing-safety.md` §3a. They are yours because
the AdMob UI is. Do not write the advertising ID or any ad unit ID into this file, a report, a
commit message or any file in the repo: the advertising ID is a personal identifier.

A. [ ] At least 24 h before the REL-02 session, and before installing the internal-track build on
       the phone:
       1. On the phone: Settings → Google → Ads. Copy the advertising ID.
       2. https://admob.google.com → Settings → Test devices → Add test device.
       3. Name it (visible to anyone with account access), platform Android, paste the
          advertising ID, gesture = Shake (that gesture opens ad inspector later). Save.
       4. Note the wall-clock time. Nothing below is valid until 24 h have passed.
B. [ ] After Step 8 installs the build: the first ad in the app must show the **"Test Ad"** label
       at the top.
       - Label present → everything in release-checklist section 5 is safe, including tapping
         the ad.
       - Label absent → **STOP.** Tap nothing. Back out of the ad. The registration has not
         propagated (or the wrong ID was pasted). Re-check item A and wait.
C. [ ] Read the three ad unit IDs out of the installed build and compare them, character by
       character, against the app's ad units in the AdMob Console (release-checklist 4.1a). Do
       not write any of the IDs into a file in the repo, a report, or a commit message.
D. [ ] Leave the device registered after the session. Removing it takes up to 24 h and resumes
       live ads on that phone.
E. [ ] If an ad without the "Test Ad" label is ever tapped, follow §5 of
       `docs/ad-testing-safety.md` and tell the producer. Do not click again, and do not click
       more to "even it out".

**How to tell it is done:** item A was saved at least 24 hours before the session, and item B
observed the "Test Ad" label on the first ad. Those two together tick release-checklist row 5.0
(the qa-engineer confirms the label when QA drives the session). Items C to E hold for the rest of
the session.

---

## Step 8 — The internal testing track, and a download that is not an `adb install`

**Blocks:** REL-02, ROADMAP success criterion 1 — the whole point of the phase.
**Time:** 15 minutes, plus Play's processing (minutes) and tester-link propagation (minutes to
hours).

Success criterion 1 is deliberately worded to exclude the easy path: *"verified working from a
signed internal-testing-track **download** — not an `adb install` debug build."* The reason is
Step 3: a locally built artifact is signed with the upload/debug certificate, while the artifact a
tester downloads is signed with the **app signing** certificate. Only the second exercises the
credential that real players will use.

This step runs **after** the studio has produced the signed artifact and **after**
`.planning/approvals/publish-3.0.0.approved` exists. The upload itself is gated: the studio's
publish hook refuses to run the upload script without that file, and creating it is your decision
at Gate 2, not a step on this list.

1. [ ] Upload the build to **Internal testing**. The producer records the route under
       `docs/release-checklist.md` item 1.7:
       - **Console route:** create an Internal testing release in Play Console and upload the AAB
         the studio hands you (its sha256 is in checklist item 4.6).
       - **Script route:** the studio runs the upload; you do two things. (a) In Play Console,
         create a service account with release rights on `com.erman.pegsolitarie` and download its
         JSON key. (b) **Only after** `.planning/approvals/publish-3.0.0.approved` exists, place that
         key on this Mac as a regular file (not a link) **outside every repository**, for example
         under `~/.config/`, and give the studio its absolute path for `PLAY_SERVICE_ACCOUNT_JSON`.
         The key is a secret: as in Step 7, its contents appear in no file and no report. The
         studio's rules for this route are in the note under §4 of `docs/release-checklist.md`
         (T-1, T-3, T-6, T-7, T-11, TL-1). Then continue at item 2.
2. [ ] Confirm Play accepts the signature without a "wrong signing key" or "versionCode already
       used" error. The versionCode being uploaded is **30000** (see `14-RELEASE-PLAN.md`); the
       published one is **2**.
3. [ ] Add the tester accounts (yours and Step 4's second one) to the internal track's tester
       list — this is a **different list** from the Play Games Services tester list in Step 3
       item 6. Both are needed.
4. [ ] **First confirm Step 7a item A was saved at least 24 hours ago** for this phone; if not,
       wait before installing. Then open the **opt-in URL** on the test device, accept, and
       **install from the Play Store app**. Not from a file, not over `adb`. If the store page offers "Update" rather than
       "Install", uninstall the local build first — a locally signed copy of the same
       `applicationId` blocks the store install with a signature mismatch.
5. [ ] Confirm the installed build is the store copy:

           adb shell pm list packages -i com.erman.pegsolitarie

       The installer must be `com.android.vending`. If it says `null` or a shell package, you are
       still testing the wrong artifact and criterion 1 is not satisfied.
6. [ ] Do Step 7a item B on the first ad before any other ad is touched. Then hand the device
       (or the emulator, signed in as the tester) to the studio's verification
       script in `14-RELEASE-PLAN.md`. Executing that script is QA's job; providing the
       store-installed build is yours.

**How to tell it is done:** item 5 prints `installer=com.android.vending`, and the release shows
**Available to testers** in the Console.

---

## Step 9 — Provenance for the artwork that lives only in the Console

**Status: done, 2026-10-17.** You answered Q-017 (*"you did it"*: the studio produced the art in
its own sessions). The studio wrote own-work rows with each file's sha256 into `THIRD_PARTY.md`
§7 on `feature/phase-14-release-docs`. Nothing is left for you here.

**Blocked:** `THIRD_PARTY.md`. The studio's release rules block a release while any row lacks
a licence.

Everything in the artifact has recorded provenance. The twelve WAV sound files are CC0 or rendered
in-house, with a per-file table (`docs/audio/CREDITS.md`). Fredoka and Nunito are **bundled** as
five static OFL-1.1 font files with their licence texts (`feature/phase-15-ux-refresh`; the
downloadable-font path is gone in 3.0.0). The launcher icon is drawn in this repo as vector XML.
The art that had **no** record was the art that lives only in the Play Console:

1. [x] The **20 achievement icons** (512×512) uploaded in phase 12, plus the 7 leaderboard icons.
       Studio-produced (Q-017).
2. [x] The listing's **512×512 icon** and **1024×500 feature graphic**. Studio-produced (Q-017).
       The icon was exported from the launcher design canvas.
3. [x] **Screenshots:** the studio recaptures them as plain captures of this app after the candy
       backdrop lands (Q-016).
4. [x] Answers recorded in `THIRD_PARTY.md` §7, not in this file.

**How to tell it is done:** `THIRD_PARTY.md` has a licence in every row, including the store-art
rows, and §7 holds no pending row.

---

## Step 10 — One leftover, non-blocking

1. [ ] `Q-002(a)` from phase 13 is still open and still worth two minutes:
       **drive.google.com/drive/settings → Manage apps** on the tester account — is "Peg
       Solitaire" listed, and with a hidden-app-data size? It is the only direct confirmation
       that Saved Games data ever reached Google's side. Cloud save is verified by other means
       (two devices, observed union in both directions), so this blocks nothing; it closes a
       question rather than a requirement.

---

## Before calling phase 14 done

The founder-side gate. Every line here is something only you can observe.

- [ ] **Step 0** — `app-ads.txt` serves a direct 200 from the host in the listing's website
      field, **and** AdMob reports the app authorised. (REL-03)
- [ ] **Step 2** — the published privacy policy names advertising-ID collection and all four
      SDKs, and its "Last updated" date is after 2026-09-20. (REL-04)
- [ ] **Step 5** — Data safety, Ads, Target audience, Content rating and the Advertising-ID
      declaration are all submitted, and every answer traces to a sentence in that policy.
      (REL-01)
- [ ] **Step 3** — Play App Signing enrolled; a PGS OAuth credential exists for every distinct
      SHA-1 among {app signing, upload, debug}; PGS configuration **Published**, no draft.
- [ ] **Step 6 / Step 7** — the release IDs and the keystore are in place, and
      `./gradlew :app:verifyReleaseAdIds :app:verifyReleaseGamesIds` exits 0.
- [ ] **Step 7a** — the test phone was registered as an AdMob test device at least 24 hours
      before the session, and the first ad on the store copy carried the "Test Ad" label.
      (release-checklist 5.0)
- [ ] **Step 8** — a tester has the build with `installer=com.android.vending`, and the studio's
      verification script has been run against it. (REL-02)
- [ ] **Step 4** — the second Google account exists and is propagated, and the account-swap /
      D-14 UAT has been run against it.
- [ ] **Step 9** — `THIRD_PARTY.md` has no row without a license.
- [ ] **Step 1** — the listing describes the app that is shipping.

Two of these — Step 5's four Console sections and Step 8's track download — are **founder-only
and unticked at publish time is an escalation**, not a judgement call the studio can make for
you. The studio will stop and ask rather than proceed.
