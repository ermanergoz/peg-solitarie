# 3.0.0 — what only the founder can do before the Play upload

Date: 2026-11-01. Author: release-manager (draft; the producer asks). Details per item:
`.planning/reports/phase-14/release-manager-founder-items.md` and
`.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md`. No value (ID, password, SHA-1)
is ever written in a file or sent in chat.

Suggested order: F-2 first (it needs 24 h), then F-8 and F-7 (they unblock the build), then F-5,
F-3 and F-13 around the upload.

| Item | What to do | Where | Time | Unblocks |
|---|---|---|---|---|
| **F-2** — register the test phone | Copy the phone's advertising ID (Settings → Google → Ads) and add it as a test device. Wait **at least 24 h** before installing the store build on that phone. First ad must say "Test Ad"; if not, tap nothing and tell the producer | admob.google.com → Settings → Test devices | 5 min + 24 h wait | Post-upload checks 5.0 → 5.6–5.8b (real ads on the store copy, safely) |
| **F-7 / 4.1** — real ad and Play Games IDs | IDs are already in your untracked `local.properties`. Confirm a GDPR consent message is published for the app; make sure the same `local.properties` exists in the release checkout `PegSolitarie-release` | AdMob → Privacy & messaging; your machine | 10 min | The studio's `verifyReleaseAdIds verifyReleaseGamesIds`, so the signed build (4.3) |
| **F-8 / 4.2** — upload keystore | Find the original release keystore (keep it outside every checkout, back it up off this machine). When the studio builds, export `PLAY_UPLOAD_KEYSTORE` (absolute path), `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`, `PLAY_UPLOAD_STORE_PASSWORD` in that one shell — never in a file | Your machine / password manager | 15–30 min | The signed AAB (4.3) and every §4 item after it; the Gate 2 summary |
| **F-13** — Console comparands | When the producer asks at the artifact step, read: the highest published versionCode (expected 2; 3.0.0 is 30000), the upload-key SHA-1, and the three AdMob ad-unit IDs; compare them as the studio instructs. Send no values | Play Console → App bundle explorer / App integrity; AdMob → Apps | 10 min | 2.3, 4.1a, 4.4 → the Gate 2 summary |
| **1.7** — upload route | Decide: the studio runs `./scripts/play-upload.sh` (needs `PLAY_SERVICE_ACCOUNT_JSON`), or you upload the AAB yourself in the Console (operator checklist Step 8 item 1). The producer records the choice | Tell the producer | 1 min | Checklist 1.7, which blocks Gate 2 |
| **F-1** — `app-ads.txt` status | Read whether the app's `app-ads.txt` shows verified; tell the producer the date. If not verified, follow the fix steps in the phase-14 founder-items file (website field `https://ermanergoz.com`, app linked to the Play listing, publisher ID match) | AdMob → Apps → Peg Solitaire → App settings | 5 min (+24 h if a fix is needed) | REL-03 (ad demand); does not block the upload command |
| **F-3** — Play App Signing + Play Games credentials | Enrol in Play App Signing at this upload (recommendation: keep the existing key). Read whether the app-signing and upload SHA-1s are identical; make sure each distinct SHA-1 (and the debug one) has a Play Games Android credential created in Play Console; publish the PGS configuration; put your account and the second account on the PGS tester list | Play Console → App integrity; Play Games Services → Configuration | 20–30 min | Play Games sign-in on the downloaded store build (T-1..T-5) |
| **F-5** — App content declarations | Fill or re-check Data safety, Ads (contains ads), Target audience (13+), Content rating, Advertising-ID declaration, with the live privacy policy open beside it, from the studio's draft in `docs/store/` | Play Console → Policy → App content | 30–45 min | REL-01; unticked at publish time is an escalation |

**Q-025** (wired headset / headphones off for the audio QA) is effectively answered by the founder's
2026-10-31 directive "physical device unavailable: emulator only" — option 2: those halves are
recorded as JVM + emulator evidence in the Q-PUBLISH-3.0.0 summary. The producer records the answer
in `.planning/questions/FOUNDER.md`.
