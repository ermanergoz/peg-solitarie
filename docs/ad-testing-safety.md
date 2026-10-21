# Ad testing safety — how to verify this app without getting the AdMob account banned

**Owner:** release-manager. **Audience:** the founder (a real phone, a store download) and
qa-engineer (emulators, local builds). **Status:** binding for every release from v1.1 on.

This is a separate document rather than more rows in `docs/release-checklist.md` on purpose. The
checklist is a tick-list — one line, one piece of evidence — and the question "may I tap this ad"
needs the reasoning, the policy text and the sources attached to it, because the cost of getting it
wrong is not a failed check, it is a **non-appealable account closure that also takes AdSense with
it** (§4). The checklist links here and carries only the boxes.

Every claim below is sourced to Google documentation retrieved on **2026-09-21**; the URLs are in
§7. Nothing here rests on recollection. Where the documentation does not answer a question, this
document says so instead of guessing.

---

## 0. The short version

- **The one rule: never tap an ad that does not say "Test Ad".** That label is the SDK's own
  statement that the impression and the click are excluded from your account. No label, no tap —
  back out of the ad instead.
- QA on emulators with debug builds is **not** a risk at all, twice over: debug builds in this
  repo cannot serve a real ad (§1), and emulators are documented as automatic test devices (§2).
- The founder's own phone, running a **release** build from the internal track, is the only real
  exposure in the plan — and it is removed entirely by registering that phone as a test device in
  the AdMob UI **before** installing the build (§3).
- An interstitial *appearing by itself* is an impression, not a click. One is harmless; a
  verification session's worth from a single device is on Google's own list of invalid traffic
  (§4). This is why registering the device is the fix and "be careful what you tap" is not.
- Real (live) ads are **not** required to satisfy REL-02. Test-mode ads on the real ad unit IDs
  verify strictly more than live ads do, with no risk. See §6 — this is the recommendation.

---

## 1. What this app actually requests, per build type

Before any policy question: know which ad unit a given build asks for. In this repo that is
decided in `build-logic/src/main/kotlin/AdIdResolution.kt`, not at runtime.

| Build | Ad unit IDs used | Reaches the founder's AdMob account? |
|---|---|---|
| `debug` (what QA runs, what any emulator runs) | **Hard-wired** to Google's demo units in `AdIdDefaults` — the configured value in `local.properties` is deliberately ignored (`DEF-06-01`) | **No.** Google's demo units "are not associated with your AdMob account, so there's no risk of your account generating invalid traffic when using these ad units" [1] |
| `release` (the signed AAB, the internal-track download) | The real IDs, from `ADMOB_*` in the untracked `local.properties` or the CI environment; `:app:verifyReleaseAdIds` fails the build if they are missing or still placeholders | **Yes** — unless the device is registered as a test device (§3) |

Two consequences worth stating plainly, because they decide most of the rest of this document:

1. **Nothing QA does with a debug build can touch the account.** Not the interstitial, not the
   rewarded ad, not a click, not a thousand impressions. `AdIdResolution.resolve(..., isRelease =
   false)` returns `AdIdDefaults.TEST_*` unconditionally, and the comment on it says why: honouring
   a configured ID on debug meant "every subsequent debug build on that machine served REAL ads —
   the exact invalid-traffic pattern that gets an AdMob account suspended non-appealably."
2. **The founder's real-ids-in-`local.properties` change did not make QA's emulators dangerous.**
   It made exactly one thing dangerous: a **release** artifact installed on a device that is not a
   registered test device. That is REL-02, and only REL-02.

There is no AdMob Mediation in this app — the GMA Next-Gen SDK supports AdMob-only mediation and
none is configured. That matters because test mode and the "Test Ad" label are documented as
applying to the AdMob network only; mediated third-party ads "do NOT render a Test Ad label" and
must be put into test mode network by network [1][4]. Not our problem today; it becomes our problem
the day a mediation adapter is added, and this paragraph is the warning.

---

## 2. Are Android emulators automatically test devices? Yes — and it is documented for Next-Gen

**Confirmed for the Next-Gen SDK specifically.** This project uses
`com.google.android.libraries.ads.mobile.sdk` (GMA Next-Gen), so the legacy
`play-services-ads` page would not have settled it. It does not have to: the **Next-Gen** Android
page *Enable test ads* carries the same Key Point callout in its own "Enable test devices"
section [1]:

> **Key Point:** Android emulators are automatically configured as test devices.

The identical sentence appears on the legacy page [2], so the behaviour is common to both SDKs
rather than something that was dropped in the rewrite. This is a direct statement on the Next-Gen
documentation, not an inference from the legacy SDK — which is what was asked for.

Three caveats, none of which change the answer but all of which affect how much weight to put on it:

- It is a documentation guarantee about **Google-served AdMob ads**. It says nothing about mediated
  networks (we have none) and it is not a contractual immunity — the invalid-traffic policy is
  still the governing document.
- It is about **emulators**, i.e. a device the SDK detects as an emulator. A physical device gets
  nothing from this sentence, however "internal" the build on it is.
- Google does not publish the detection mechanism, and this project should not depend on a
  behaviour it cannot observe. **Verify instead of assuming:** an ad served in test mode renders a
  "Test Ad" label at the top of the banner, interstitial or rewarded video, and the SDK exposes
  `RequestConfiguration.isTestDevice()` to confirm registration [1]. Both are cheap; the
  emulator-is-automatic sentence is the belt, the label is the braces.

Because of §1, the emulator question is close to moot for this project's QA: an emulator running a
debug build is on Google's demo units anyway, and the automatic-test-device behaviour is a second
independent layer under it. The case where it carries real weight is a **release** build sideloaded
onto an emulator for a spot check — there, and only there, is the emulator guarantee doing the work
alone.

---

## 3. Registering a physical device — the founder's phone, for the internal-track build

Two routes exist [1]. For REL-02 only one of them is usable, and it matters why.

### The right route: the AdMob UI (no rebuild, no code, nothing committed)

The build that the founder installs comes from the Play internal testing track. Its code is frozen;
a code-side test-device list would require a new build and a new upload, which defeats the purpose
of verifying the artifact that was actually uploaded. The AdMob UI route is server-side and applies
to a build that already exists.

Steps, from Google's *Set up a test device* article [3]:

1. **Find the phone's advertising ID.** On Android: *Settings → Google → Ads*; the advertising ID
   is shown on that screen. (If a given Android version files the same screen under *Privacy* or
   *Ads privacy*, it is still the same "Ads" screen and the same ID.) It can also be read
   programmatically, but for one phone, reading it off the screen is the whole job.
2. Sign in to the AdMob account at `https://admob.google.com`.
3. **Settings → Test devices → Add test device.**
4. Enter a device name (note: "The device name will be visible to anyone who has access to your
   AdMob account"), select **Android**, paste the advertising ID, choose an ad-inspector gesture
   (Shake is convenient), **Save**.
5. **Wait.** Google's stated latency: "Ads in test mode can take up to 1 hour to show in apps. In
   some cases, it may take up to 24 hours" [3]; the SDK page says test devices "typically start
   serving test ads in your app within 15 minutes, but it can also take up to 24 hours" [1].
   **Register the device at least 24 hours before the REL-02 session** and treat anything earlier
   as unverified.
6. **Confirm on the device, before touching anything.** The first ad that appears must carry the
   **"Test Ad"** label at the top (for native, the headline is prefixed with "Test Ad"). "Ads with
   this Test Ad label are safe to click. Requests, impressions, and clicks on test ads won't show
   up in your account's reports" [1]. No label → the registration has not propagated → stop, do not
   tap, and wait.

What this gives you is the important part: "When you configure your Android or iOS device as a test
device, the AdMob Network sends **production ads in test mode** to your device **using the ad unit
IDs you've created in your AdMob account**" [3]. The real unit is exercised. The account is not
charged an impression. Google frames this as the supported way to do exactly what REL-02 asks:
"Setting up a test device lets you safely test production ads and verify your implementation code
**without violating AdMob's invalid traffic policy**" [3].

Removal has the same latency and the opposite risk: "Removing a test device means that the AdMob
Network will start serving live ads to the device. Note that clicking on live ads can result in a
policy violation for invalid traffic" [3]. **Leave the founder's phone registered.** There is no
benefit to removing it and there is a 24-hour window of confusion if it is removed.

### The other route: programmatic — and precisely where the ID would live

The SDK route is `RequestConfiguration.Builder().setTestDeviceIds(listOf("<hash>"))` passed to
`MobileAds.setRequestConfiguration(...)`; the hash is printed by the SDK to logcat on the first ad
request, in the form `Use RequestConfiguration.Builder.setTestDeviceIds(Arrays.asList("33BE…E231"))
to get test ads on this device` [1]. Note this hash is *not* the advertising ID — the AdMob UI wants
the advertising ID, the SDK wants the hash it printed.

If this project ever needs that route, **the ID does not go in a `.kt` file.** It follows the same
path the ad unit IDs already take:

- a new key in the **untracked** `local.properties` — e.g. `ADMOB_TEST_DEVICE_IDS=<hash>[,<hash>]` —
  read by the existing `local.properties` reader in `app/build.gradle.kts` into a `BuildConfig`
  field, exactly as `ADMOB_APP_ID` / `ADMOB_REWARDED_AD_UNIT_ID` /
  `ADMOB_INTERSTITIAL_AD_UNIT_ID` are today;
- `local.properties` is git-ignored (`.gitignore:7`), so the value cannot be committed by accident;
- `local.properties.template` gets the **key name and a `REPLACE_ME_…` placeholder only**, never a
  real value — the resolver already treats a `REPLACE_ME` string as absent;
- nothing goes in `docs/`, in this file, in a report, or in a commit message. A device advertising
  ID is a personal identifier as well as an account-linked one.

For REL-02 the UI route is the correct one and the programmatic route should not be used at all: it
cannot be applied to an artifact that has already been uploaded.

### 3a. The founder-facing steps, ready to lift into `14-OPERATOR-CHECKLIST.md`

`.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md` is on `develop` since its merge at
0582e3f, but it does not carry these steps yet: it has no test-device registration and no "Test Ad"
check. Lifting Steps A–E into it, before its Step 8 installs the internal-track build, is owed by the
release-manager in a pass that may edit that file. Until then the founder's own list lacks the step
that checklist row 5.0 gates on, so row 5.0 cannot be ticked from the operator checklist alone. These
are the ad steps, and they are founder-only because the AdMob UI is.

    [ ] Step A — at least 24 h before the REL-02 session, and before installing the internal-track
        build on the phone:
        1. On the phone: Settings → Google → Ads. Copy the advertising ID.
        2. https://admob.google.com → Settings → Test devices → Add test device.
        3. Name it (visible to anyone with account access), platform Android, paste the
           advertising ID, gesture = Shake (that gesture opens ad inspector later). Save.
        4. Note the wall-clock time. Nothing below is valid until 24 h have passed.
        Blocks: release-checklist 5.0, and therefore 5.6, 5.7, 5.8, 5.8a.

    [ ] Step B — first ad in the app must show the "Test Ad" label at the top.
        Label present → everything in section 5 is safe, including tapping the ad.
        Label absent  → STOP. Tap nothing. Back out of the ad. The registration has not
                        propagated (or the wrong ID was pasted). Re-check and wait.

    [ ] Step C — read the three ad unit IDs out of the installed build and compare them, character
        by character, against the app's ad units in the AdMob Console (release-checklist 4.1a).
        Do not write any of the IDs into a file in the repo, a report, or a commit message.

    [ ] Step D — leave the device registered after the session. Removing it takes up to 24 h and
        resumes live ads on that phone.

    [ ] Step E — if an ad without the "Test Ad" label is ever tapped, follow §5 of
        docs/ad-testing-safety.md and tell the producer. Do not click again, and do not click more
        to "even it out".

---

## 4. What actually gets an account flagged or banned

Google's definition, verbatim, from *Invalid traffic* [5]:

> Invalid traffic includes any clicks or impressions that may artificially inflate an advertiser's
> costs or a publisher's earnings. Invalid traffic covers intentionally fraudulent traffic as well
> as **accidental clicks**.
>
> Invalid traffic includes, but is not limited to:
> - Clicks or impressions generated by **publishers clicking on their own live ads**
> - **Repeated ad clicks or impressions generated by one or more users**
> - Publishers **encouraging clicks** on their ads …
> - Automated clicking tools or traffic sources, robots, or other deceptive software

And the consequence, from the same page: "If we observe high levels of invalid traffic on your
account, we may **suspend or disable the account** … ultimately it is **your responsibility as the
publisher** to ensure that the traffic on your ads is valid."

The AdMob *Behavioral policies* page is blunter still [6]:

> Publishers may not click their own ads or use any means to inflate impressions and/or clicks
> artificially, including manual methods. **Testing your own ads by clicking on them is not
> allowed.** Please use test ads … to avoid generating invalid clicks.

and, on the scope of a closure:

> Publishers whose accounts are closed for invalid activity or violating our policies may not be
> allowed any further participation in other Google publisher monetization solutions … if a
> publisher had an AdMob account closed for invalid activity or policy violations, they would not
> be able to use AdSense to monetize, and vice versa. For this reason, these publishers **may not
> open new accounts.** If a publisher opens additional accounts, the accounts will be flagged as
> duplicates and then one or both accounts will be closed.

Google's own prevention advice leads with "**Don't click your own ads, even if you think it's okay
to do so** … Testing your own ads by clicking them is not allowed. Use test ads" [7]. Note also
that Google deliberately will not tell you what it saw: "To maintain the integrity of our invalid
activity detection system … we aren't able to provide details on what activity we noticed in your
account" [7]. There is no "explain yourself afterwards" path worth planning around.

### Applied to the activities that are actually planned

| Planned activity | Verdict | Why |
|---|---|---|
| QA driving the app on emulators, debug builds — tapping the rewarded ad, sitting through interstitials, hundreds of times | **Safe** | Debug is hard-wired to Google's demo units, which "are not associated with your AdMob account" [1]; and emulators are documented automatic test devices [1]. Two independent layers (§1, §2) |
| A **release** build on a **registered test device**, any interaction including tapping | **Safe** | "Ads with this Test Ad label are safe to click. Requests, impressions, and clicks on test ads won't show up in your account's reports" [1]; the UI route is described as testing "without violating AdMob's invalid traffic policy" [3] |
| Founder **tapping** a live rewarded ad on an unregistered phone, once | **The single highest-risk act in the plan** | This is literally bullet 1 of the invalid-traffic list: "publishers clicking on their own live ads" [5], and §6's "Testing your own ads by clicking on them is not allowed" [6]. Never do it |
| A live **interstitial appearing by itself** between levels, once or twice | **Not a violation on its own** | An impression is not a click, and no policy forbids the app's own author seeing his own ad. Nothing to "undo" |
| A **REL-02 session's worth** of live interstitial impressions from one phone — level after level, plus retries | **Risky, and the real reason to register the device** | Bullet 2 is "repeated ad clicks or impressions generated by one or more users" [5]. Our interstitial is automatic and frequency-capped, so the tester cannot opt out of generating them; volume from a single device is the exposure, and it is not avoidable by discipline |
| Asking anyone — testers, friends, family — to "have a look at the ads" | **Forbidden** | "Publishers encouraging clicks" [5]. Testers are told to verify the *game*; ads are never the assignment |
| The rewarded ad's own copy and reward wiring | **Compliant as built, and must stay that way** | Rewarded is a first-party AdMob format, but "Google ads may not be placed on apps that promise payment or incentives to users who click on or view ads" [6]. The safe shape, which is the shape we ship: the button offers the **reward** (a hint / an undo), never instructs anyone to click an ad; the reward is granted on the SDK's **reward callback** (completion), never on a click and never on mere display. Any future copy that says "click the ad to…" is a policy violation, not a wording preference |
| Retry being ad-gated | **Forbidden by product decision, and it is also the safe side of this policy** | Already the rule in `CLAUDE.md`; an ad a frustrated player must sit through to continue is precisely the "high volume of accidental clicks" implementation Google names [5] |

---

## 5. If an ad is clicked by accident

Assume it will happen once. It is recoverable. What makes it unrecoverable is the reflex response.

1. **Stop.** Close the ad, leave the browser or store page it opened, and do not tap anything else
   in it. Do not click a second time "to see what happens", and above all do not attempt to
   balance it out — repeat clicks turn one accident into the pattern in §4.
2. **Stop live-ad testing on that device for the session.** Do not continue the REL-02 pass with
   live ads after an accidental click.
3. **Register the device as a test device** (§3) before any further ad work, and wait out the
   propagation window.
4. **Record it**, in the phase UAT sheet, as an observed `result:` line: date and time, device,
   which ad format and which placement, what the tester was trying to tap, and that testing stopped.
   This is for us — evidence that it was a stray tap during a documented verification session and
   not a pattern.
5. **Do not email support to confess a single click.** There is no such process, Google filters
   accidental clicks automatically as part of invalid-traffic handling, and refunds the advertiser —
   "Google refunds revenue generated from invalid clicks to affected advertisers" [7]; a gap between
   estimated and finalised earnings is the ordinary mechanism for this [5]. Do escalate to the
   founder (via the producer) so the decision to report is his.
6. **Report only if it was more than a stray tap** — a whole session on live units, a tester who was
   told to click, a device left unregistered through repeated passes. Google's channel for this is
   the invalid-traffic report form linked from *Invalid traffic* [5] ("If you suspect invalid click
   traffic by a third party, use this form to report it to our traffic quality team"). Proactively
   reporting a self-inflicted burst is better than it being detected.
7. **One accidental click is not a ban.** The policy language is about volume and pattern —
   "high levels of invalid traffic", "a pattern of use" [5][6]. One tap during a documented
   verification session, never repeated, is not what that describes. Panic is the risk; the rule in
   §0 exists so this step is never needed.

---

## 6. Does REL-02 need live ads? No — and test mode verifies more, not less

**Recommendation: keep the real ad unit IDs in the artifact (they must be there — checklist 4.1),
and register the founder's phone as a test device so those real units serve production ads in test
mode. Do not verify with live ads. Ever.**

REL-02's requirement is that every feature is verified working from a signed track download. Ad
*display* is a feature; live *monetisation* is not a feature, it is a revenue event. Separate them
and the requirement is met with no exposure, because a registered test device serves "production ads
in test mode … using the ad unit IDs you've created in your AdMob account" [3]. Concretely, the
test-mode pass still proves:

- the artifact carries the **real** ad unit IDs (verified independently, without any ad, by
  `:app:verifyReleaseAdIds` at build time and by reading `BuildConfig` values back out of the
  installed build);
- the real units **request and fill** — including a no-fill, which is a real outcome to see;
- the interstitial appears only after Continue, never on loss or restart, with the clock paused
  (checklist 5.8);
- the rewarded ad loads, grants on completion, grants nothing on early dismissal, and the fail-open
  path hands over the assist on no-fill (checklist 5.7);
- the UMP consent form appears before any ad request (checklist 5.6) — consent is gathered on the
  real configuration regardless of test mode;
- frequency capping, the pause/resume behaviour, and every piece of app-side logic under test.

What it does **not** prove is that a paying advertiser's creative renders and that revenue accrues.
Neither is verifiable pre-launch by any means, and the first real impressions after the production
rollout are where that is observed — from the AdMob Console, not by tapping.

Two additions make the test-mode pass strictly stronger than a live pass:

- **Ad inspector.** It is an in-app overlay for real-time analysis of ad requests, it reports the ad
  unit actually requested and the network's response, and — decisive here — "**Ad inspector launches
  only on test devices**" [4]. Registering the phone is what unlocks the best available evidence
  that the real units are configured correctly. A live-ad pass cannot use it.
- **A read-only ID comparison by the founder.** Test mode would still serve for a *wrong but valid*
  ad unit, so the ID correctness check must be done by reading, not by clicking: the founder
  compares the three IDs in the installed build against the ad units in the AdMob Console. This is
  a new checklist row (4.1a) and it is the one thing test mode cannot cover.

The residual risk after all of the above is: an ID that is valid, correct, and belongs to the right
app, but whose Console-side configuration (ad format, mediation groups, eCPM floors) is wrong in a
way that only shows under live serving. That is a monetisation-tuning question, it is observable in
the Console after rollout, and it is not worth one percent of the risk of an account closure.

---

## 7. Sources

All retrieved 2026-09-21. Where a claim in this document is contentious, it carries the bracketed
number of the source that settles it.

| # | Document | URL | What it establishes here |
|---|---|---|---|
| 1 | *Enable test ads* — **GMA Next-Gen SDK for Android** | https://developers.google.com/admob/android/next-gen/test-ads | The Next-Gen-specific answer on emulators ("Key Point: Android emulators are automatically configured as test devices"); demo units are "not associated with your AdMob account"; the "Test Ad" label and that labelled ads "are safe to click"; `setTestDeviceIds` and the logcat hash; `isTestDevice()`; mediated ads render no label |
| 2 | *Enable test ads* — Google Mobile Ads SDK (Legacy) Android | https://developers.google.com/admob/android/test-ads | Carries the identical emulator Key Point — i.e. the behaviour is not legacy-only |
| 3 | *Set up a test device* — AdMob Help | https://support.google.com/admob/answer/9691433 | The AdMob UI registration steps; where to find the advertising ID (*Settings → Google → Ads*); "production ads in test mode … using the ad unit IDs you've created"; "without violating AdMob's invalid traffic policy"; 1-hour/24-hour propagation, both on add and on remove; removal resumes live ads |
| 4 | *Ad inspector overview* — GMA Next-Gen SDK for Android | https://developers.google.com/admob/android/next-gen/ad-inspector | Real-time analysis of ad requests in-app; "Ad inspector launches only on test devices" |
| 5 | *Invalid traffic* — AdMob & AdSense program policies | https://support.google.com/admob/answer/3342054 | The definition and the four bullets; accidental clicks are in scope; "we may suspend or disable the account"; publisher responsibility; the traffic-quality report form |
| 6 | *Behavioral policies* — AdMob | https://support.google.com/admob/answer/2753860 | "Testing your own ads by clicking on them is not allowed"; closure is cross-product with AdSense and new accounts are flagged as duplicates; apps that incentivise clicking or viewing ads |
| 7 | *How you can prevent invalid activity* — AdMob Help | https://support.google.com/admob/answer/3342099 | "Don't click your own ads, even if you think it's okay to do so"; Google refunds advertisers; Google will not disclose what it detected |
| 8 | *AdMob policies and restrictions* (index) | https://support.google.com/admob/answer/6128543 | Entry point to the full policy set, for anything this document does not cover |

A question this document cannot answer — a store or ads policy judgement that is not in the pages
above — is an **escalation to the founder through the producer**, not a studio decision
(`rules/studio.md`).
