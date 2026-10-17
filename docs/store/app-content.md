# Play Console App content — drafted answers for 3.0.0

Drafted by the release-manager on 2026-10-18, for the founder's F-5
(`.planning/reports/phase-14/release-manager-founder-items.md`). **These are drafts.** The founder enters
them in Play Console → Policy → App content, with the privacy policy open beside the form. Each answer
has to agree with the policy, sentence by sentence.

Sources:
- **[P]** the live privacy policy, https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html,
  "Last updated: September 21, 2026", fetched 2026-10-18;
- **[G]** Google's data disclosure for the Mobile Ads SDK,
  https://developers.google.com/admob/android/privacy/play-data-disclosure, read 2026-10-18. The
  page fetched was the legacy SDK's (25.5.0). The app ships GMA Next-Gen 1.4.0, so the founder picks
  "Android" (Next-Gen) on that page's platform selector before filling the form, and re-reads the list.
- `CHANGELOG.md` §3.0.0 and `THIRD_PARTY.md` §4 (the SDKs that ship).

The founder is responsible for the answers Play receives. Where the Console's wording differs from
this draft, the Console wins, and the difference goes back to the release-manager through the producer.

---

## 1. Data safety

**Does the app collect or share any of the required user data types?** Yes. Data that Google's SDKs
send off the device counts as collected by the app (`rules/play-store.md`: "collected" = sent off the
device).

**Is all user data encrypted in transit?** Yes. [G]: "All of the user data collected by Google Mobile
Ads SDK … is encrypted in transit using the Transport Layer Security (TLS) protocol." Play Games
Services talks to Google over TLS as well. The app itself sends nothing [P].

**Do you provide a way for users to request that their data is deleted?** Yes. [P], "Deleting your data":
through the Play Games profile settings, which delete the profile and this game's saved game.
Uninstalling removes the device copy. Ad data is held by Google under its own policy [P], "Retention".

**Account creation.** The app does not let users create an account. Play Games sign-in uses the
player's existing Google account and is optional [P].

**Independent security review:** No.

### Data types

| Play data type | Collected | Shared | Optional? | Purposes | Source |
|---|---|---|---|---|---|
| Location → **Approximate location** | Yes | Yes | Required | Advertising or marketing; Analytics; Fraud prevention, security and compliance | [P] "IP address, which may be used to estimate your approximate location"; [G] IP address |
| App activity → **App interactions** | Yes | Yes | Required | Advertising or marketing; Analytics; Fraud prevention, security and compliance | [P] "Ad interaction and diagnostic data"; [G] "user product interactions … app launch, taps, and video views" |
| App info and performance → **Diagnostics** | Yes | Yes | Required | Analytics; Fraud prevention, security and compliance | [P] "diagnostic data"; [G] "app launch time, hang rate, and energy usage" |
| Device or other IDs | Yes | Yes | Required | Advertising or marketing; Analytics; Fraud prevention, security and compliance | [P] "Your device's Advertising ID (AAID)", "Device information"; [G] "advertising (ad) ID, app set ID" |
| Personal info → **User IDs** (Play Games player profile: gamer name, avatar, player ID) | Yes | No | **Optional** (only when the player signs in) | App functionality | [P] "Google processes your Play Games profile (such as your gamer name and avatar)" |
| App activity → **Other actions** (scores, achievements, the cloud save of progress) | Yes | No | **Optional** (only when signed in) | App functionality | [P] "your scores and achievements, and your saved game data"; "a copy of your progress … in the private application-data area of your own Google Drive" |

**Not collected**, so left unticked: name, email, phone, address and other contact details; precise
location; financial info; health and fitness; messages; photos and videos; audio; files and docs
(Play Games' Drive app-data save is a game save, declared above as Other actions); calendar; contacts;
web browsing; installed apps; search history; crash logs (not on Google's [G] list). Game progress,
settings, scores and saved games on the device are not collected: they "never leave it, except through
the optional Google services" [P].

**Why the Play Games rows say "not shared".** That data goes to Google as the provider of the Play
Games service the player signs in to, so the app does not transfer it to a third party on its own
initiative. If the Console's help text for "shared" is read the other way, tick "Shared" too. Ticking
it is the safe direction, and the policy already names Google for that data.

**Why the ad rows say "required".** The player cannot use the app with ad data switched off. In the EEA,
UK and Switzerland the consent form chooses between personalised and non-personalised ads, and
non-personalised ads still use IP address and coarse location [P]. So the data is collected either way.

**Check after filling.** Read Play's generated Data safety summary against the policy, line by line
(F-5 step 6).

## 2. Ads

**Does your app contain ads?** Yes. Rewarded ads, shown only when the player chooses one, and
interstitials between levels after the player taps Continue [P] "Advertising"; CHANGELOG §3.0.0, Added.

## 3. Advertising ID

**Does your app use an advertising ID?** Yes. Purposes: **Advertising or marketing**, plus **Analytics**
and **Fraud prevention, security and compliance**, as the Ads SDK uses it [G]. The permission
`com.google.android.gms.permission.AD_ID` reaches the app through the Ads SDK's manifest merge. The
app's own manifest declares only `INTERNET`. The founder confirms the permission is present in the
release candidate's merged manifest (release-checklist; the release-manager reads it from the built AAB).

## 4. Target audience and content

- **Target age groups:** **13–15, 16–17 and 18+.** No group under 13. [P] "Children's Privacy": "My
  service does not address anyone under the age of 13." Declaring an under-13 group would contradict
  the policy and reopen the consent and ads work (Q-F5, settled).
- **Could the store listing unintentionally appeal to children?** No. It is an abstract board puzzle
  with no characters, mascots or child-directed themes. The new backdrop is abstract light and colour
  (Q-021 direction B). If Play's review disagrees, the answer is a founder decision; it is not changed
  here.
- **Designed for Families / Teacher Approved:** not applicable (no under-13 group).

## 5. Content rating (IARC questionnaire)

- **Email for IARC:** the founder's developer-account address.
- **Category:** Game (puzzle).
- **Violence, fear, sexuality, nudity, crude humour, language, drugs, alcohol, tobacco, discrimination:**
  None. The game shows pegs, holes, stars, a timer and controls. The only sounds are short interface
  cues.
- **Gambling or simulated gambling:** No. No betting, no currency, no prizes; stars and scores have no
  value.
- **Do users interact or exchange content?** No. There is no chat, no user-generated content and no
  messaging. Leaderboards show Play Games gamer names and scores, which is Play Games' own
  presentation, not an exchange between users.
- **Does the app share the user's current physical location with other users?** No.
- **Does the app allow purchases of digital goods?** No. There are no in-app purchases.
- **Does the app contain ads?** Yes. The questionnaire asks this separately from section 2.
- **Is the app a web browser or search engine?** No.

**Expected result:** Everyone (ESRB), PEGI 3, USK 0, IARC 3+, which is the same as the live listing's
"Everyone". If the questionnaire returns something stricter, stop and send it back to the
release-manager through the producer.

## 6. Other declarations

- **News app:** No. **Government app:** No. **Financial features:** none.
  **Health:** No. **COVID-19 contact tracing or status:** No.
- **Data deletion URL** (if the Console asks for one): the policy's "Deleting your data" points to the
  Play Games profile settings. The app has no developer-side account and no data of its own to delete
  [P] "Retention".
- **App access:** every part of the app is reachable without sign-in, so no test credentials are
  needed. Sign-in is optional and leaves the game fully playable [P].

## 7. Points where the policy is thinner than the SDK (non-blocking)

- [G] lists the **app set ID** next to the advertising ID. The policy names the Advertising ID
  explicitly and covers the rest as "Device information". This is consistent, but the founder may add
  "app set ID" to the policy's list at the next update (F-6).
- [G] lists **diagnostic information** (launch time, hang rate, energy use). The policy's "Ad interaction
  and diagnostic data" covers it.
