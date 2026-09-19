# Google Play release rules (game, android-app, kmp-app)

- Versioning: root `VERSION` = semver `X.Y.Z` (versionName); versionCode =
  `X*10000 + Y*100 + Z`, monotonic.
- Signing: Play App Signing; upload keystore outside the repo via
  `PLAY_UPLOAD_KEYSTORE`, `PLAY_UPLOAD_KEY_ALIAS`, `PLAY_UPLOAD_KEY_PASSWORD`,
  `PLAY_UPLOAD_STORE_PASSWORD`.
- Target API: new apps and updates must target API 36 from 2026-08-31
  (Play Console Help, "Target API level requirements").
- Data safety form and a live privacy policy URL are mandatory even when no
  data is collected ("collected" = sent off the device).
- Tracks: internal → production. The "12 testers for 14 days" rule applies
  only to personal accounts created after 2023-11-13; the studio account is
  older, so it does not apply. If a new account is ever used, add closed
  testing and tester recruitment before production.
- Listing assets in `docs/store/`: title ≤ 30, short description ≤ 80, full
  description, 512px icon, 1024×500 feature graphic, ≥ 4 phone screenshots,
  content rating answers, data safety answers, privacy policy URL.
- Release checklist fully ticked before Gate 2. Upload uses `fastlane supply`
  via `scripts/play-upload.sh` with `PLAY_SERVICE_ACCOUNT_JSON`; the publish
  hook blocks it without `.planning/approvals/publish-<VERSION>.approved`.
