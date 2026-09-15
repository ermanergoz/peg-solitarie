---
phase: 13-cloud-save
type: operator-checklist
status: pending
blocks: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05]
generated_by: 13-03-PLAN.md
---

# Phase 13 Operator Checklist — Enable Saved Games and stage the two-emulator run

## Why this exists

Saved Games is a **per-project feature toggle** in Play Games Services, and it is
**off by default**. Google's own documentation is blunt about it: *"You must Enable saved games support for
your game in Google Play Console before implementation."* Until that toggle is on and the Play
Games Services configuration is published, every `SnapshotsClient` call this phase makes fails.

The expensive part is *how* it fails. It does not throw a distinctive error, it does not log a
"feature not enabled" message, and it does not surface anything to the player. It fails into
exactly the same `null`/`false` the offline path produces — which is by design, because CLOUD-05
requires a cloud failure to degrade silently to local-only play with no error wall and no snackbar.
So a project with the toggle off presents as **"cloud save does nothing, no error anywhere"**,
which is byte-for-byte indistinguishable from a code bug. A developer who does not know about this
document will spend hours in Kotlin looking for a defect that is not there.

This single checkbox blocks
`.planning/phases/11-play-games-session/11-HUMAN-UAT.md` tests **10 through 13** in their entirety —
that is 100% of this phase's device verification, and with it four of the five CLOUD requirements'
only observable proof. Assumption **A6** in `13-RESEARCH.md` records the toggle's current state as
**unverified**: it may already be on, in which case Step 0 is a no-op confirmation that costs a
minute. Confirm it rather than assume it.

Unlike Phase 12, this phase creates **no new identifiers**. There is nothing to paste into
`local.properties` and nothing to record anywhere. The toggle *is* the deliverable.

Work top to bottom.

## Step 0 — Enable Saved Games

1. [ ] In **Play Console → Play Games Services → this game → Configuration** (the
       Features / Saved games section), **turn on saved games**.
2. [ ] **Publish the Play Games Services configuration afterwards.** Publishing is a separate
       action from configuring, and the Console will happily leave a configured-but-unpublished
       change sitting there indefinitely. This is the same trap Step 3 of
       `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` documents for the
       OAuth consent screen, and it produces the same class of failure: everything looks right in
       the Console and nothing works on the device.
3. [ ] Confirm that **no new ID was generated** and that nothing needs to go into
       `local.properties`. If you find yourself copying a value out of the Console for this phase,
       you are on the wrong screen — Phase 12's 27 identifiers are the only Play Console values
       this project stores, and this phase adds none.

No manifest change accompanies this. The app already declares
`com.google.android.gms.games.APP_ID`, and the `play-services-games-v2:22.0.0` AAR declares nothing
snapshot-specific — so there is no code-side counterpart to this step to go looking for.

## Step 1 — Confirm the prerequisite chain is still intact

Nothing here is new work; it is a re-confirmation that Phase 12's foundation is still standing,
because Saved Games hangs off exactly the same project, credentials and tester list. Cross-reference
`.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` Step 0 rather than
re-deriving any of it.

1. [ ] The **Play Games Services project** is still linked to package `com.erman.pegsolitarie` and
       still published (12-OPERATOR-CHECKLIST Step 0, items 2 and 8).
2. [ ] **Both** OAuth Android credentials still exist — the **debug-keystore SHA-1** and the
       **Play App Signing SHA-1**, both created through Play Console (12-OPERATOR-CHECKLIST Step 0,
       item 4). The debug credential is what makes a locally built debug APK able to sign in at all,
       and therefore what makes tests 10 through 13 runnable without a store upload.
3. [ ] The **tester account** used for Phase 12's tests 4 through 9 is still on the tester list
       (12-OPERATOR-CHECKLIST Step 0, item 6). Phase 13's device run uses that same one account on
       both emulators — see Step 2.

## Step 2 — Two emulators, one tester account (D-16)

D-16 verifies CLOUD-01, CLOUD-02 and CLOUD-05 with two emulators signed into the **one existing
tester account**, solving different levels on each and asserting the union on both. Stage both
before the test run rather than during it.

1. [ ] AVD **`Pixel_9a`** is installed and boots. Launch command, recorded here so the UAT run does
       not have to rediscover it:
       `~/Library/Android/sdk/emulator/emulator -avd Pixel_9a`
2. [ ] AVD **`Pixel_9`** is installed and boots:
       `~/Library/Android/sdk/emulator/emulator -avd Pixel_9`
3. [ ] **Both** emulators have the **Play Games app** present and the **same tester Google account**
       signed in. Two devices, one account — the union is only meaningful if both sides are the same
       player.
4. [ ] `adb devices` lists both, and you can tell which serial is which (the first booted is
       normally `emulator-5554`, the second `emulator-5556`). Test 13 kills the network on
       `Pixel_9` specifically, so knowing which serial is which is not optional.

## Step 3 — Known gap, not a task

The **profile-swap dialog** (D-12 through D-15 — the one dialog this milestone reserves, shown when
`session` resolves to a *different* player id than the one local progress was last merged with)
cannot be exercised on this setup. It needs a **second Google account that has never authorised this
game**. One does not exist.

1. [ ] Confirm this is recorded as a gap in
       `.planning/phases/11-play-games-session/11-HUMAN-UAT.md`, beside Phase 11 test 2 — **same
       root cause, same wording**: one account, already authorised, so the alternate-account path
       cannot be produced on the device.
2. [ ] Confirm D-12 through D-15 are covered instead by JVM tests over a fake session
       (`FakeGamesService.emitSession(SignedIn(playerId = "other"))`) in plans 13-08 and 13-10.

Do **not** treat this step as something to go and fix by creating a throwaway Google account. It is
recorded deliberately, with compensating coverage named, so that the gap survives as a known
limitation instead of being quietly dropped or quietly papered over.

## Before calling Phase 13 done

- [ ] **Saved Games enabled** in Play Console **and** the Play Games Services configuration
      **published** (Step 0).
- [ ] `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` tests **10 through 13** executed on
      the two emulators, with their `result:` lines filled in from what was actually observed.
- [ ] Test 12's measurement recorded while you are in the Play Games saves UI: what
      `getMaxCoverImageSize()` returns and the PNG-compressed size of the 512×512 cover, so
      13-RESEARCH assumption A1 is measured once rather than assumed forever.
- [ ] The **profile-swap gap** is still recorded in `11-HUMAN-UAT.md` — still present, still
      honest, not quietly removed because the phase otherwise passed.

Every checkbox above starts unticked, and this document is executed by a human. Tick a box only
after you have done the thing; a pre-ticked box in an operator checklist is a false claim about the
state of a system nobody else can see.
