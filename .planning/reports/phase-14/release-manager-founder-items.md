# Founder-only items before 3.0.0 can be uploaded — one list

Date: 2026-10-17. Author: release-manager. For: the producer, who puts these to the founder.
Product version **3.0.0** (root `VERSION`), versionCode **30000** (develop 08b1b27, merged 2397732),
milestone v1.1.

This merges the founder-side items from `14-OPERATOR-CHECKLIST.md` (Steps 0–10),
`14-RELEASE-PLAN.md` §7 and its Q-F1..Q-F5, `14-COLD-START.md`, `docs/release-checklist.md` (every
**[F]** and **[S→F]** row), `THIRD_PARTY.md` §7, `docs/ad-testing-safety.md` §3a (via
`release-manager-ad-safety.md`) and STATUS.md "Waiting on founder" (app-ads.txt / REL-03). Items
the studio does itself are left out. Studio blockers that need no founder input are listed once at
the end so the founder is not asked about them.

**How "state" was established.** *Done* only with evidence I observed today (2026-10-17, from this
machine) or that a committed report records. *Not done* when the evidence shows it is missing.
*Unknown* when only the founder can see it (Play Console, AdMob UI, his shell, his phone). No ID,
key, account name or password is written here; where `local.properties` was read, only key names and
the shape of values were checked, never printed.

**Screenshots will change.** A candy-style background for the menu and game screens is about to be
added before release (`feature/candy-backdrop`, no commits yet). Every store screenshot is
therefore captured **after** that work merges, from the release-candidate build. Nothing below asks
the founder to produce or approve screenshots before then.

---

## Order to do them in

Lead time first. Items marked ⏱ have a waiting period the founder cannot shorten, so start them
together in one sitting.

| # | Item | State | Blocks |
|---|---|---|---|
| F-1 ⏱ | Read the AdMob `app-ads.txt` status | unknown | REL-03 (phase 14 done), not the upload |
| F-2 ⏱ | Register the test phone as an AdMob test device, ≥ 24 h before the REL-02 session | unknown | release-checklist 5.0 → 5.6–5.8b |
| F-3 ⏱ | Play App Signing enrolment, and a PGS credential per distinct SHA-1 | unknown | the upload; T-1 sign-in on the downloaded build |
| F-4 | Store-art provenance (achievement icons, listing icon, feature graphic) | not done | `THIRD_PARTY.md` §7 → release-checklist 1.4 → **the branch cut** |
| F-5 | Data safety, Ads, Target audience, Content rating, Advertising-ID declaration | unknown | REL-01; unticked at publish = escalation |
| F-6 | Privacy policy: two small confirmations | done in substance; 2 checks open | REL-04 close-out |
| F-7 | AdMob IDs in `local.properties`, and the UMP message | done in substance; 1 check open | every signed artifact |
| F-8 | The release keystore and the four `PLAY_UPLOAD_*` variables | unknown | every signed artifact |
| F-9 | Store listing: read the current copy; approve copy, notes and screenshots | not done | Play policy exposure; Gate 2 summary |
| F-10 | Gate 2: create `publish-3.0.0.approved` (or reject) | not done (correctly — no artifact yet) | the upload |
| F-11 | Internal-track upload, testers, install from the Play Store | not done | REL-02 / success criterion 1 |
| F-12 | Second Google account on the **internal-track** tester list | account exists; list unknown | release-checklist 5.10 / T-12 on the store copy |
| F-13 | Console comparands the studio checks against | unknown | release-checklist 2.3, 4.1a, 4.4 |
| F-14 | Drive "Manage apps" check (Q-002(a)) | not done | T-5 CLOUD-03 oracle on the store build; otherwise non-blocking |

---

## F-1 — Read the AdMob `app-ads.txt` status (REL-03) ⏱

**What.** AdMob → Apps → Peg Solitaire → App settings → `app-ads.txt`: read whether the app shows as
authorised/verified.

**Why.** Without a verified `app-ads.txt`, buyers that honour the IAB spec treat the inventory as
unauthorised, which suppresses demand on both ad formats. REL-03 and ROADMAP success criterion 2
close only on AdMob's own positive status.

**State: unknown — and overdue to read.** Evidence:
- The file has been correct and live since 2026-09-02. Re-checked 2026-10-17T17:27Z:
  `https://ermanergoz.com/app-ads.txt` → `200`, 0 redirects; `https://ermanergoz.github.io/app-ads.txt`
  → one 301, then `200` on the canonical file.
- The listing's Website field was empty until 2026-09-21. The founder filled it; it has been live on
  the public page as `https://ermanergoz.github.io` since 2026-09-21T01:29:28Z, and it is still
  there today (`aria-label="Website https://ermanergoz.github.io …"` on the en-US page).
- The 24 h checkpoint (2026-09-22) and the 48 h escalation mark (2026-09-23) are long past. Nobody
  has recorded the AdMob status (STATUS.md, "Waiting on founder").

**Steps.**
1. Open AdMob → Apps → Peg Solitaire → App settings. Read the `app-ads.txt` line.
2. **If it shows authorised/verified:** tell the producer the date it shows. Done; nothing else.
3. **If it is still not verified:**
   a. Play Console → Grow → Store presence → Store listing → Contact details → Website: change it
      to exactly `https://ermanergoz.com` (https, no `www.`, no path). Save and publish the listing
      change. Then wait 24 hours, touching nothing.
   b. In the same AdMob page, check that the app entry is **linked to the Play listing**
      (it shows the store link / package `com.erman.pegsolitarie`). An app added "not on a store"
      has no listing to read the website from, and can never verify.
   c. AdMob → Settings → Account information: check that the publisher ID there is the same
      `pub-…` as the one inside `ADMOB_APP_ID` in your `local.properties`.
4. Do not change the website or the file. The file is correct.

**Blocks.** Phase 14's REL-03 and success criterion 2. It does not block the upload command.

## F-2 — Register the test phone as an AdMob test device ⏱

**What.** `docs/ad-testing-safety.md` §3a, Step A: register the phone's advertising ID under
AdMob → Settings → Test devices, at least 24 hours before the REL-02 session.

**Why.** The store build carries the real ad units. On an unregistered phone the interstitial
produces real impressions, and one stray tap is invalid traffic — the account-closure risk. A
registered device gets "production ads in test mode" on the real unit IDs, which is what REL-02
needs.

**State: unknown.** Nothing records a registration.

**Steps.**
1. On the phone: Settings → Google → Ads. Copy the advertising ID.
2. admob.google.com → Settings → Test devices → Add test device. Name it, platform Android, paste
   the ID, gesture = Shake. Save. Note the time.
3. Wait 24 hours before installing the internal-track build on that phone.
4. During the session, the first ad must show the **"Test Ad"** label. No label: stop, tap nothing,
   back out, tell the producer.
5. Leave the device registered afterwards.

**Blocks.** Release-checklist 5.0, and through it 5.6, 5.7, 5.8, 5.8a and 5.8b.

## F-3 — Play App Signing enrolment, and PGS credentials ⏱

**What.** Operator checklist Step 3: enrol the app in Play App Signing at this upload, read both
certificate SHA-1s, and make sure every distinct SHA-1 has a Play Games Services Android credential
created **in Play Console**, then publish the PGS configuration.

**Why.** The build a tester downloads is signed with the **app signing** certificate. If that
SHA-1 has no PGS credential, sign-in works on the studio's builds and fails on every store copy.
That is the failure phase 14 exists to catch.

**State: unknown.** D-21 records that the app is not enrolled yet. Nothing records enrolment.

**Steps.**
1. Enrol when the Console offers it during the first upload. The choice of key is a decision; it is
   drafted below as a question (adopt the existing key is the recommendation).
2. Play Console → Test and release → App integrity → App signing. Read the **App signing key**
   SHA-1 and the **Upload key** SHA-1. Tell the studio whether they are identical. You don't need to
   send the values.
3. For each distinct SHA-1, plus the debug keystore's, make sure a PGS Android credential exists,
   created through Play Console → Play Games Services → Configuration (never in Google Cloud).
4. Publish the PGS configuration. Check that no draft is left.
5. PGS tester list: your account and the second account (F-12) are on it.

**Blocks.** T-1 (sign-in on the downloaded build) and through it T-2..T-5. Part of the upload:
enrolment happens at the first upload.

## F-4 — Provenance of the art that lives only in the Console (Q-F3)

**What.** For the **20 achievement icons** (512×512), the **listing icon** (512×512) and the
**feature graphic** (1024×500): where each came from and under what licence.

**Why.** `rules/ip-policy.md`: a release is blocked when a `THIRD_PARTY.md` row lacks a licence.
These four rows are `PENDING FOUNDER`. Nothing in the repo records them, because they were never in
the repo.

**State: not done.** `THIRD_PARTY.md` §7 (feature/phase-14-release-docs) still has four
`PENDING FOUNDER — Q-F3` rows.

**Steps.** Reply to the producer with one answer per group, or one for a group that shares an
origin:
- (a) "I drew them", or
- (b) "generated with <tool>" — and, if the tool's terms depend on a plan or a date, which plan and
  roughly when, or
- (c) "bought/downloaded from <source> under <licence>", or
- (d) "taken from <URL>" (the studio reads the licence there itself).

If the answer is CC-BY or CC-BY-SA, also give the author's name. Do not paste URLs or account
details into a repo file; the studio writes the rows. Store screenshots captured by the studio from
the build need no answer. A screenshot with an added frame, background or overlay font does need one;
see F-9.

**Blocks.** `THIRD_PARTY.md` completeness → release-checklist 1.4 → cutting `release/3.0.0`.

## F-5 — Play Console App content declarations (REL-01)

**What.** Operator checklist Step 5: Data safety, Ads, Target audience and content, Content rating,
the Advertising-ID declaration, and the News/Government/Financial/Health answers (all no).

**Why.** These answers must agree with the published privacy policy, sentence for sentence. A
mismatch is a common reason a Play release is rejected or pulled. `docs/release-checklist.md`
makes an unticked founder-only item at publish time an escalation.

**State: unknown.** They were filled for the old app (versionCode 2) at some point. Their current
answers are not knowable from the repo. The studio's drafted answers (`docs/store/`) do not exist
yet; that is studio work, listed at the end.

**Steps.** Wait for the studio's draft in `docs/store/` (written from the live policy). Then, with
the policy page open beside the form:
1. Data safety: Advertising ID collected, shared, used for advertising. Play Games scores,
   achievements and the cloud save declared as collected (they leave the device). Room/DataStore
   data not collected. Encrypted in transit. Deletion route as the policy states it (Play Games
   profile settings).
2. Ads: contains ads.
3. Target audience: 13+ only, no under-13 age group. This matches your own policy text ("My
   service does not address anyone under the age of 13"); see Q-F5 below.
4. Content rating questionnaire: a puzzle game with ads, no user-generated content, no
   communication, no purchases.
5. Advertising-ID declaration: uses the advertising ID, purpose advertising.
6. Re-read Play's generated Data-safety summary against the policy, line by line.

**Blocks.** REL-01 and success criteria 3/3a. Publish-time escalation if unticked.

## F-6 — Privacy policy: two confirmations (REL-04)

**What.** Operator checklist Step 2, items 5–6, plus one wording fix.

**Why.** REL-04 needs the published policy to cover phases 12 and 13, and it needs the same URL in
the app, the listing and App content.

**State: done in substance; two checks open.** Evidence: on 2026-10-17 the live page
`https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html` (the URL compiled into
`PrivacyLinks.kt`) answered `200`, 0 redirects, and reads **"Last updated: September 21, 2026"**,
after the 2026-09-20 bar. It names the Advertising ID, the Google Mobile Ads SDK / AdMob, the User
Messaging Platform consent form, Play Games sign-in, leaderboards and achievements, and the saved
game in the Drive application-data area. It says that progress otherwise stays on the device, and it
gives a deletion route. Step 2 items 1–4 are therefore met by observation.

**Steps.**
1. Play Console → App content → Privacy policy: confirm the URL there resolves to this same page.
   The listing currently links the `ermanergoz.github.io/policies/…` form, which redirects there. That
   is fine; the point is that it is not an older page.
2. Wording fix on the page, a 1-minute website edit: the policy says to open Settings → Privacy →
   **"Manage ad consent"**, but the app's row is labelled **"Manage privacy choices"**
   (`settings_privacy_choices_label`). Change the page to the app's wording. Do not change the URL.

**Blocks.** Closing REL-04. Not the upload.

## F-7 — AdMob IDs and the UMP message (Step 6)

**What.** The four release IDs in your untracked `local.properties`, and the consent message in the
AdMob UI.

**Why.** `verifyReleaseAdIds` / `verifyReleaseGamesIds` stop every signed build without them. UMP
renders nothing without a configured message, and consent sits upstream of every ad request.

**State: done in substance; one check open.** Evidence, read today without printing a value:
`ADMOB_APP_ID`, `ADMOB_REWARDED_AD_UNIT_ID`, `ADMOB_INTERSTITIAL_AD_UNIT_ID` and `PGS_PROJECT_ID`
are set in `local.properties` (primary checkout and PegSolitarie-p15). None is a `REPLACE_ME`
placeholder, and none carries Google's test publisher ID. The app ID and the unit IDs have the right
shapes (`~` and `/`). 28 Play Games keys are present (27 IDs + project ID). The studio still runs
`verifyReleaseAdIds verifyReleaseGamesIds` on the release branch. That is studio work.

**Steps.**
1. AdMob → Privacy & messaging: confirm a GDPR consent message is published for this app.
2. Keep `local.properties` as it is. When the release branch is built on another checkout, it needs
   the same file there.

**Blocks.** Every signed artifact (already unblocked by the IDs); T-6 needs the UMP message.

## F-8 — Release keystore and environment variables (Step 7)

**What.** The original release keystore, outside the repo, and `PLAY_UPLOAD_KEYSTORE` (absolute
path), `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD` and `PLAY_UPLOAD_STORE_PASSWORD` exported
in the shell that runs the release build.

**Why.** The signing config on develop (9ca3829) reads only these four variables. It fails the
build when any is missing or wrong, and never falls back to the debug key. Play rejects anything
not signed with the upload key.

**State: unknown.** The signing work was proved with a throwaway keystore only. The founder's
keystore and passwords were never involved (`android-game-dev-signing-config.md`).

**Steps.**
1. Locate the original keystore (D-21). Keep it outside every checkout.
2. Back it up somewhere off this machine. Before enrolment, a lost key cannot be recovered.
3. When the studio is ready to build, export the four variables in that shell only, never in a
   file.
4. `keytool -list -keystore "$PLAY_UPLOAD_KEYSTORE" -alias "$PLAY_UPLOAD_KEY_ALIAS" -v | grep SHA1`
   must equal the Console's **upload key** SHA-1 (F-3 step 2).

**Blocks.** Every signed artifact, so release-checklist §4 and the upload.

## F-9 — Store listing (Step 1; Q-F4)

**What.** Read the current listing. Settle the copy, the 500-character release notes and the
screenshots for the app that is shipping.

**Why.** The live listing was written for the old app (versionCode 2, "2.0"), which had a different
UI and modes and no Challenge ladder, leaderboards or sound. Play policy requires screenshots to
show the app as it is. The release notes and the copy are taste, not engineering.

**State: not done.** The studio has never seen the current copy. `docs/store/` does not exist on any
branch. Screenshots cannot be final until the candy backdrop merges.

**Steps.**
1. Play Console → Store presence → Store listing. Read the short and full descriptions. Tell the
   producer whether they still describe the app (the decision is drafted below).
2. After the candy backdrop is merged and a release-candidate build exists, the studio captures
   ≥ 4 phone screenshots from that build and writes the texts into `docs/store/` within the limits.
   You approve them; you do not produce them.
3. Check that the 512×512 listing icon and the feature graphic still fit the app's identity (the
   launcher icon is the in-repo vector one).
4. Approve the ≤ 500-character release notes at Gate 2. They are in the one-screen summary.

**Blocks.** Not mechanical; a Play policy exposure, and the "store-asset state" line of the
Gate 2 summary.

## F-10 — Gate 2 (Q-PUBLISH-3.0.0)

**What.** Approve or reject the publish of 3.0.0. On approval the studio's approve skill writes
`.planning/approvals/publish-3.0.0.approved`. The studio never creates it on its own.

**Why.** It is the only thing between a signed artifact and an upload. The publish hook and
`scripts/play-upload.sh` both refuse without it.

**State: not done, correctly.** No release branch, artifact or one-screen summary exists yet. The
question is drafted below for later.

**Blocks.** The upload (F-11).

## F-11 — Internal track: upload, testers, install from the Play Store (Step 8)

**What.** After Gate 2: create the Internal testing release with the artifact the studio hands
over, add the testers, and install the build **from the Play Store app** on the test device.

**Why.** Success criterion 1 is "verified from a signed internal-testing-track **download**", and
only the downloaded copy carries the app signing certificate.

**State: not done.** It cannot start before F-3, F-8, F-10 and the studio's artifact.

**Steps.** As in operator checklist Step 8:
1. Upload. Play must accept the signature, and the versionCode must be 30000; the published one is 2.
2. Add your account and the second account to the **internal track** tester list. This is a
   different list from the PGS tester list.
3. Open the opt-in link on the test phone (registered per F-2), and install from the Play Store.
   Uninstall any locally built copy first.
4. Hand the device to QA. `adb shell pm list packages -i com.erman.pegsolitarie` must show
   `installer=com.android.vending`.

**Blocks.** REL-02 / criterion 1, release-checklist §5, and C4 of `14-COLD-START.md` (cold start
on the store build). C4 is the only founder-blocked part of PLAT-08.

## F-12 — The second Google account (Step 4)

**What.** A Google account that had never used this game, signed in on a test device, on the
tester lists.

**Why.** The account-swap / D-14 wipe is the one destructive path in the milestone.

**State: account exists; the internal-track list is unknown.** On 2026-09-21 QA ran the full
profile-swap UAT with two distinct accounts on `Pixel_9b_clean` (debug build, verdict PASS;
`.planning/reports/phase-14/qa-engineer-account-swap.md`, commit 014eba9). Account B got its Play
Games profile then. The remaining founder part is the **store-copy** run (release-checklist 5.10 /
T-12).

**Steps.** Add account B to the internal-track tester list (F-11 step 2), and keep it on the PGS
tester list (F-3 step 5).

**Blocks.** Release-checklist 5.10 on the store copy.

## F-13 — Console values the studio checks against

**What.** Three reads that only the Console can supply. The studio does the comparing, and no value
goes into a file:
- the highest versionCode ever published (expected 2), for release-checklist 2.3;
- the three ad IDs in AdMob, compared against the IDs read out of the built bundle, for 4.1a
  (`ad-testing-safety.md` §6, Step C);
- the upload key SHA-1, for 4.4 (same read as F-3 step 2).

**Why.** Each of them catches a mistake that test mode or a green build cannot: a wrong but
valid ad unit, a wrong key, or a versionCode collision.

**State: unknown.**

**Steps.** Read them when the producer asks at the artifact step and compare them as instructed.
None is sent in writing.

**Blocks.** Release-checklist §4, so the Gate 2 summary.

## F-14 — Drive "Manage apps" check (Q-002(a), Step 10)

**What.** drive.google.com/drive/settings → Manage apps, on the tester account: is "Peg Solitaire"
listed with a hidden-app-data size?

**Why.** It is the only direct confirmation that saved games reached Google. On the store build it
is also T-5's CLOUD-03 oracle, because the debug snapshot picker was compiled out of release (L-3).

**State: not done.** Q-002's answer (2026-09-20) records (a) as not done and still open.

**Steps.** Open that link, find the app, and read the size. It takes two minutes. It can be done
during the F-11 session.

**Blocks.** Only T-5 on the store copy. Otherwise non-blocking.

---

## Q-F1..Q-F5 — which are already settled

| Question | Status | Evidence | Ask the founder? |
|---|---|---|---|
| **Q-F1** — ship v1.1 as 3.0.0, versionCode 30000 | **Settled by merged work.** The decision was recorded 2026-08-27; root `VERSION` reads `3.0.0`; develop 08b1b27 (merged 2397732) sets `versionCode = 30_000`, `versionName = "3.0.0"` | `git show develop:app/build.gradle.kts` lines 280–281 | **No.** The Gate 2 summary states the version |
| **Q-F2** — ship without a fuzz harness, or wait | **Settled by a recorded ruling.** The tech-lead decided R-2 rather than waiving it: kover measured then gated, fuzz test wired at 1,000 / 10,000 / 100,000, ordered **branch cut** (`.planning/reports/phase-14/tech-lead-prerelease-signoff.md` §D, R-2 and the bounding paragraph) | same file | **No.** Note: R-2 is decided but **not built**: `git log --all` has no kover or fuzz commit. It is a studio blocker on the cut, listed below, with no founder input |
| **Q-F3** — store-art provenance | **Open, but not a decision.** It is a fact only the founder knows. The ready-to-send wording is F-4 above (and `THIRD_PARTY.md` §7) | — | **Yes, as an information request**, not a decision question; not drafted below |
| **Q-F4** — listing copy and the 500 characters | **Open, and narrowed.** Screenshots are no longer a question: they must be recaptured after the candy backdrop, by the studio, from the RC build. What remains is the copy | — | **Yes**, drafted below |
| **Q-F5** — target audience | **Settled by the founder's own recorded text.** The live policy (updated 2026-09-21) says: "My service does not address anyone under the age of 13". Declaring 13+ in the Console is consistent with it; under-13 would contradict the policy and reopen phases 9–10 | policy page, fetched 2026-10-17 | **No.** F-5 step 3 says to declare 13+. If the founder ever wants under-13, he raises it as new scope |

None of Q-F1..Q-F5 was ever filed in `.planning/questions/FOUNDER.md` (checked in the primary,
r5, p15 and audio checkouts), so none has a founder answer to contradict.

---

## Drafted founder questions (FOUNDER.md format; ids for the producer to number)

Only genuine decisions of taste, money, scope or a gate.

    ## Q-<NNN> — Play App Signing: keep your existing key or let Google make a new one?   (open)
    Date: 2026-10-17    Asked by: release-manager
    Context: The first upload of 3.0.0 enrols the app in Play App Signing (D-21: not enrolled yet).
    Enrolment is one-way. The key you choose signs every copy players get from now on.
    Options: 1) Upload your existing release key as the app signing key; keep using it as the upload key
             2) Let Google generate a new app signing key; your existing key becomes the upload key only
    Recommendation: release-manager picks 1. It keeps every copy of the old app installed today
    upgradable to 3.0.0, and it keeps the Play Games credential already registered for that key's SHA-1
    valid. Option 2 needs a new PGS credential before T-1 can pass, and gains nothing this release needs.
    Blocks: the first upload (F-3, F-11); nothing before it
    Answer:

    ## Q-<NNN> — Store listing copy: rewrite for the new app, or keep?   (open)
    Date: 2026-10-17    Asked by: release-manager
    Context: The live listing was written for the old app (versionCode 2), which had no Challenge ladder,
    leaderboards, cloud save or sound. Screenshots are already settled: the studio recaptures them after
    the candy backdrop lands. The copy and the ≤ 500-char release notes are taste.
    Options: 1) The studio drafts new short + full descriptions and the release notes into docs/store/;
                you approve them at Gate 2
             2) The current copy still fits; the studio drafts only the release notes
             3) You write the copy yourself; the studio checks the limits and checks it against the changelog
    Recommendation: release-manager picks 1. The rewrite is complete, so old copy almost certainly
    describes features that are gone and misses the ones that are new. Approval at Gate 2 adds no extra
    round trip.
    Blocks: nothing mechanical; the store-asset line of the Gate 2 summary
    Answer:

    ## Q-PUBLISH-3.0.0 — Approve the internal-track upload of Peg Solitaire 3.0.0   (open)
    Date: <filed by the producer when the one-screen summary exists>    Asked by: release-manager
    Context: <from .planning/reports/release-3.0.0/release-manager.md: version 3.0.0 / versionCode 30000,
    changelog, checklist state with every [F] row, store-asset state, the exact upload command>
    Options: 1) Approve — /ergoz-soft:approve publish 3.0.0  2) Reject, with the reason
    Recommendation: <release-manager's pick, written with the summary>
    Blocks: the upload
    Answer:

Not drafted, on purpose: Q-F1, Q-F2 and Q-F5 (settled, above); Q-F3 (information, F-4); the
`app-ads.txt` field change (a conditional fix step inside F-1, not a choice); the licences
surface (a scope call for the producer, raised in the release-manager report, and a founder action
only if the producer picks a website page).

---

## Studio blockers that need no founder input (so they are not asked)

Listed so the founder list is not mistaken for the whole release list.
- **R-2: kover + fuzz harness**, decided, ordered "branch cut", not built. Then R-7: the
  `FUZZ_SEEDS=100000` run on the release branch.
- **Branch merges into develop:** `feature/phase-14-gate-scope` (R-1', code-auditor PASS),
  `feature/phase-14-release-docs` (this file, `THIRD_PARTY.md`, the release checklist),
  `feature/phase-14-release-plan` (the operator checklist), `feature/phase-15-ux-refresh`
  (bundled fonts; reviews in flight), `feature/audio-review` (WAV set; `check` red until the
  `AudioCreditsTest` integration), `feature/candy-backdrop` (not started).
- **`docs/store/`, `scripts/play-upload.sh`, `CHANGELOG.md`**: none exists on any branch yet.
- **`docs/tech-design.md`** (R-6): the tech-lead is writing it now.
- **`THIRD_PARTY.md` U-1 (UMP licence), U-6 (classpath listing), U-7 (candy backdrop)**: studio
  work at the cut.
- **PLAT-08 C1–C3** (debug cold start, `14-COLD-START.md`): needs no founder input.
