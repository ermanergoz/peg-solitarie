# Audio follow-up: technical design

Owner: tech-lead. Written 2026-09-29 on `feature/audio-review`. It ran on Opus under the founder's
Q-011 exception; the report records the deviation. The file name is kept short. The design also
covers the other developer items from the audio review.

The android-game-dev builds from this file, and the code-auditor and qa-engineer review against
it. It answers the tech items of `.planning/reports/audio-review/sound-designer.md`:

- the Silent/Vibrate gate (P3; founder Q-010, option 2);
- background silencing (P8);
- `VOLUME_JUMP` alongside the jump re-level (P4a);
- the credits test for WAV and in-house recipes (P5);
- the `MAX_STREAMS` KDoc nit;
- one item this design adds: the volume keys (V1, §2.5).

It does not cover P1's visual partner (ux-designer, `docs/ux/invalid-tap-feedback.md`), the assets
(sound-designer) or provenance (research-analyst, release-manager).

Paths are under `app/src/main/java/com/app/pegsolitaire/` for code and
`app/src/test/java/com/app/pegsolitaire/` for tests, unless a path says otherwise. Line numbers refer
to this branch at d1c6e9b (code identical to develop 3f2af90).

**One fact the brief got wrong:** the product's `minSdk` is **24**, not 26
(`build-logic/src/main/kotlin/android-app.gradle.kts:29`; `docs/tech-design.md` §9.1 on
`feature/phase-14-release-docs` keeps the gap to the studio's 26 open). This design covers API 24
to 36. Nothing in it changes if the floor later rises to 26.

---

## 0. Decisions at a glance

| # | Decision | § |
|---|---|---|
| A1 | Game sound mutes when the ringer is **Silent or Vibrate and** the sound would come out of the **phone itself** (built-in speaker or earpiece). Every other output keeps sound: wired, USB and Bluetooth headphones, a Bluetooth speaker, a car. | 1.1 |
| A2 | The gate lives in `SoundPoolPlayer`, next to the `soundOn` gate. The system is read through an injected `SystemAudioState` seam in `audio/`. The `SoundPlayer` interface, every caller and `:domain` are unchanged. | 1.5 |
| A3 | The gate is **event-driven, not checked at play time.** Three events trigger a re-read off the main thread: the ringer broadcast, `ACTION_AUDIO_BECOMING_NOISY` and an `AudioDeviceCallback`. `play()` reads one volatile flag. | 1.4 |
| A4 | Route: on API 33+ the platform is asked where the pool's own attributes route (`getAudioDevicesForAttributes`). On API 24–32 the route is inferred from the connected outputs. | 1.3 |
| A5 | Entering the muted state **stops streams already in flight**, the same standard as the in-app mute (SOUND-05). | 1.4 |
| A6 | The `SoundEnabled` pop still bypasses the in-app mute (D-12). It **honours** the speaker gate and the background gate. | 1.7 |
| A7 | Fail-open, with one collector per input. A reader that fails leaves today's behaviour and can never disable the in-app mute. | 1.9 |
| B1 | **P8 approved.** The process `ON_STOP` stops streams and blocks new cues until `ON_START`. It is read from `ProcessLifecycleOwner`, **not** through `AppForegroundTracker`. | 2.1 |
| B2 | **P4a approved.** `VOLUME_JUMP` goes 0.9 → 1.0 on this branch, before it merges, with three KDoc edits and one test rename. | 2.2 |
| B3 | **P5.** The credits test covers every file in `res/raw`. It reads the `## Files` table by header name and classifies each row: **sourced** needs an https source and an allow-listed licence; **in-house** needs an existing `audio/recipes/<stem>.json`. A new format test parses each WAV. | 2.3 |
| B4 | **Nit approved**, comment only, with the corrected arithmetic. | 2.4 |
| B5 | **Added:** `volumeControlStream = STREAM_MUSIC` in `MainActivity`, so the volume keys can never put the phone into Vibrate from inside the game. | 2.5 |
| C1 | Build on this branch **after `develop` contains phase 15.** No file this design touches is changed by phase 15. | 3 |

---

## 1. Silent and Vibrate on the phone speaker (P3, Q-010 option 2)

### 1.1 Behaviour contract

| Ringer | Output | Ordinary cues | `SoundEnabled` pop | Streams already playing when this state begins |
|---|---|---|---|---|
| Normal | any | play if Sound is on in Settings | plays | untouched |
| Vibrate or Silent | the phone (built-in) | muted | muted | **stopped** |
| Vibrate or Silent | anything external | play if Sound is on | plays | untouched |

- **"The phone" means a built-in output:** `AudioDeviceInfo.TYPE_BUILTIN_SPEAKER`,
  `TYPE_BUILTIN_EARPIECE` and `TYPE_BUILTIN_SPEAKER_SAFE` (API 30). The founder's answer reads
  "mute on the phone speaker only; headphones keep sound". Taken literally, it mutes only the phone
  speaker, so a Bluetooth speaker, a car or an HDMI sink keep sound as headphones do. One rule
  ("mute only what would come out of the phone") is simpler to build, test and explain than a list
  of which external devices count.
- **Do Not Disturb is not read.** It is a separate system state (the `NotificationManager`
  interruption filter), and Q-010 does not name it. Some OEMs report the ringer as Silent while DND
  is on. The gate follows the reported ringer mode, which errs toward quiet.
- **Nothing is written.** The gate never changes `soundOn`, the media volume or any system
  setting. Leaving Silent or unplugging into Normal brings sound back without any action.

### 1.2 Reading the ringer mode

- **Read:** `AudioManager.getRingerMode()` (API 1, no permission). It maps to
  `RINGER_MODE_NORMAL | RINGER_MODE_VIBRATE | RINGER_MODE_SILENT`; any other value maps to Normal
  (fail-open).
- **Change:** `AudioManager.RINGER_MODE_CHANGED_ACTION`, a sticky system broadcast (API 1). It is
  received by a receiver registered at runtime, so Android 8's limits on manifest receivers do not
  apply. Register it with `ContextCompat.registerReceiver(context, receiver, filter,
  ContextCompat.RECEIVER_NOT_EXPORTED)` (androidx.core 1.18.0 is already on the graph):
  - targetSdk 36 means the exported flag is enforced from Android 14;
  - `ContextCompat` drops the flag below API 33;
  - system broadcasts still reach a not-exported receiver.

  Because the broadcast is sticky, registering delivers the current value at once, which does no
  harm (§1.4 de-duplicates).
- **API 24 vs 36:** the ringer code is the same on every level. The only part that depends on the
  API level is the route (§1.3).

### 1.3 Reading the output route

- **API 33+** (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`, checked directly so that
  lint's `NewApi` sees the guard): call `AudioManager.getAudioDevicesForAttributes(attrs)` with
  **the same `AudioAttributes` the pool plays with** (`GameAudioAttributes`, §1.6). It returns the
  devices the platform's routing policy would send those streams to.
  - If **any** returned device is built-in, the route is BuiltIn: sound would come out of the phone,
    and duplicated routes count as the phone.
  - Otherwise the route is External.
  - An empty list falls through to the inference below.
- **API 24–32:** use `AudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)` (API 23). The route
  is External if **any** connected output's type is in the external-media allowlist; otherwise it
  is BuiltIn.
  - **Allowlist:** `TYPE_WIRED_HEADSET`, `TYPE_WIRED_HEADPHONES`, `TYPE_LINE_ANALOG`,
    `TYPE_LINE_DIGITAL`, `TYPE_BLUETOOTH_A2DP`, `TYPE_HDMI`, `TYPE_HDMI_ARC`, `TYPE_USB_DEVICE`,
    `TYPE_USB_ACCESSORY`, `TYPE_DOCK`, `TYPE_AUX_LINE`, `TYPE_USB_HEADSET` (26),
    `TYPE_HEARING_AID` (28), `TYPE_BLE_HEADSET` (31), `TYPE_BLE_SPEAKER` (31).
  - **Deliberately excluded:** `TYPE_BLUETOOTH_SCO` (a headset connected for calls only does not
    carry media), `TYPE_TELEPHONY`, `TYPE_REMOTE_SUBMIX` (screen recording and cast capture), FM,
    tuner, IP, bus.
  - **It is an allowlist, not a denylist,** so an unrecognised type counts as the phone. That is the
    direction that keeps a meeting quiet. The list is complete for API 24–32, because a device on
    those levels cannot report a type added later.
- **Why the platform's answer on 33+ and not presence everywhere:** presence is wrong when a
  Bluetooth device is connected but media has been moved back to the phone. The platform's answer
  uses the same routing policy the pool's streams will get.
- **What is read:** only `AudioDeviceInfo.getType()`. Never `getAddress()` or `getProductName()`, so
  no Bluetooth permission is needed on API 31+, and the manifest does not change.
- **Lint:** the API 26/28/30/31 type constants are compile-time ints. Lint reports `InlinedApi` as a
  *warning*, not an error; the int is inlined and harmless on older devices. Do not add
  `@SuppressLint`. If the code-auditor wants the warnings gone, build the two sets in the platform
  reader behind `SDK_INT` guards instead.

### 1.4 When it re-evaluates

**Choice: event-driven.** The alternative was to re-read at every `play()`.

| | A. Re-read at `play()` | **B. Event-driven cache (chosen)** |
|---|---|---|
| Work per cue | 1–2 binder calls on the main thread, plus a `getDevices` array allocation | one volatile read |
| rules/audio.md "no audio work on the main thread beyond the play call" | breached in spirit | holds |
| A tail already ringing when the state changes (the 2.9 s star pops, headphones pulled mid-celebration) | keeps ringing from the speaker | **stopped** (A5) |
| Code | smallest | about 150 more lines |

A cannot silence in-flight streams, and SOUND-05 already set the house standard that a mute stops
what is playing. B wins on that alone.

**Triggers.** Each trigger causes a **full re-read** of ringer and route. The re-read runs in the
collector's coroutine, on the application scope's `Dispatchers.Default` (`di/DataModule.kt:88-89`).
Receivers and callbacks run on the main looper, where they only `trySend` a trigger.

1. **Collection start.** Register both listeners first, then read. That way a change between the
   read and the registration cannot be lost.
2. **`RINGER_MODE_CHANGED_ACTION`.**
3. **`AudioDeviceCallback.onAudioDevicesAdded` / `onAudioDevicesRemoved`** (API 23), registered with
   an explicit `Handler(Looper.getMainLooper())`.
4. **`AudioManager.ACTION_AUDIO_BECOMING_NOISY`** (API 3), in the same receiver as 2. The platform
   sends it as wired headphones are pulled or a Bluetooth sink drops, *before* it re-routes to the
   speaker. The reading for this trigger **forces `route = BuiltIn`**. The device callback that
   follows re-reads the real state. If a second headset is still connected, the forced reading mutes
   for a moment and then restores, which errs toward quiet.

**Mid-game cases:**

| Case | What happens |
|---|---|
| Headphones pulled out during the star pops, ringer on Vibrate | BECOMING_NOISY → muted → `silenceNow()` stops the tails before or as the platform re-routes; the device callback then confirms |
| Headphones plugged in on Vibrate | device added → External → the **next** cue plays; nothing that was muted is replayed |
| Ringer flipped to Silent mid-celebration on the speaker | broadcast → muted → tails stopped |
| Ringer back to Normal | broadcast → unmuted → next cue plays |
| Bluetooth headphones switched off | BECOMING_NOISY, then device removed |

**Accepted limitation.** A route change with no device change is not seen until the next ringer or
device event. The example is Android's output switcher moving media to the phone while headphones
stay connected. Seeing it would need a route listener, and `SoundPool` does not implement
`AudioRouting`. The case is rare for a game with no media session, so it is recorded here and not
built.

### 1.5 Where the logic lives

| File | Change | Android types |
|---|---|---|
| `audio/SystemAudioState.kt` | **new:** `RingerMode`, `OutputRoute`, `SystemAudioSnapshot` (with `mutesGameSound`), `interface SystemAudioState` | none |
| `audio/AudioRouteClassifier.kt` | **new, pure:** `classify`, `ringerModeOf`, and the two type sets in a private scoped-constants object | int constants only |
| `audio/GameAudioAttributes.kt` | **new:** the one `AudioAttributes` definition (`USAGE_GAME`, `CONTENT_TYPE_SONIFICATION`), moved out of `SoundPoolPlayer.kt:85-90` so the pool and the route query cannot drift | yes |
| `audio/AndroidSystemAudioState.kt` | **new:** the platform reader (§1.6) | yes |
| `audio/AppVisibility.kt` | **new** (P8, §2.1): `interface AppVisibility`, `ProcessAppVisibility` | lifecycle |
| `audio/SoundPoolPlayer.kt` | two new constructor parameters; three gates; two new collectors; pool built from `GameAudioAttributes`; KDoc: "one mute gate" becomes three | — |
| `di/AudioModule.kt` | two `@Binds @Singleton` | — |
| `MainActivity.kt` | V1, one line (§2.5) | — |
| `build-logic/src/main/kotlin/standards.gradle.kts:258` | failure **text** only: "leave AudioManager alone" becomes "and never request audio focus from AudioManager". The marker list (`requestAudioFocus`, `abandonAudioFocusRequest`) does not change. | — |

- **`audio/`, not `platform/`.** `di/AudioModule.kt` already gives the reason: `platform` is the
  Play/AdMob/UMP and foreground-Activity seam that three ArchUnit rules define, and `AudioManager` is
  a framework API with no SDK and no Activity. None of `PlatformBoundaryRules`' three rules matches
  the new classes.
- **Not `:domain`.** `:domain` is game logic. rules/audio.md gives playback to one UI-layer owner and
  keeps it out of game-core. The pure pieces (snapshot, classifier) are JVM-testable where they are,
  and `DomainPurityTest` is untouched.
- **`SoundPlayer` is unchanged, and so is every caller:** the `GameScreen` effects, phase 15's
  `JumpSoundEffect` and tutorial, `ResultsCelebration.StarPopSounds`, `withClickSound`,
  `SettingsViewModel` and the test `FakeSoundPlayer`. The gate stays inside the player for the
  reason `SoundPlayer.kt:18-21` and `JumpSoundEffect`'s KDoc give: only the owner of the stream ids
  can stop what is in flight. Do **not** add a "muted" flag to any `UiState`.

### 1.6 Signatures

Only signatures and the one expression that is the contract; bodies are the developer's.

```kotlin
// audio/SystemAudioState.kt: no android.* import
enum class RingerMode { Normal, Vibrate, Silent }

enum class OutputRoute { BuiltIn, External }

data class SystemAudioSnapshot(val ringer: RingerMode, val route: OutputRoute) {
    /** Q-010 option 2: quiet only on Silent/Vibrate AND when the sound would leave the phone itself. */
    val mutesGameSound: Boolean
        get() = ringer != RingerMode.Normal && route == OutputRoute.BuiltIn

    companion object {
        /** The fail-open reading: today's behaviour. */
        val Unmuted = SystemAudioSnapshot(RingerMode.Normal, OutputRoute.BuiltIn)
    }
}

interface SystemAudioState {
    /**
     * One reading on collection, then one after every ringer or output change, de-duplicated.
     * Cold: each collector registers its own listeners and unregisters them on cancellation.
     * Never completes.
     */
    val snapshots: Flow<SystemAudioSnapshot>
}

// audio/AudioRouteClassifier.kt: pure
internal object AudioRouteClassifier {
    /**
     * @param routedTypes where the platform routes game audio (API 33+), or null below 33.
     * @param connectedOutputTypes every connected output's type (all levels).
     */
    fun classify(routedTypes: List<Int>?, connectedOutputTypes: Set<Int>): OutputRoute

    /** AudioManager.RINGER_MODE_* → RingerMode; unknown → Normal. */
    fun ringerModeOf(platformRingerMode: Int): RingerMode
}

// audio/GameAudioAttributes.kt
internal object GameAudioAttributes {
    fun build(): AudioAttributes // USAGE_GAME + CONTENT_TYPE_SONIFICATION
}

// audio/AndroidSystemAudioState.kt
@Singleton
class AndroidSystemAudioState @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SystemAudioState {
    // callbackFlow<ReadTrigger> { register receiver + device callback; send(Initial); awaitClose { unregister both } }
    //   .map { read(it) }            // getRingerMode + route, on the collector's dispatcher
    //   .distinctUntilChanged()
    override val snapshots: Flow<SystemAudioSnapshot>

    private enum class ReadTrigger { Initial, RingerChanged, DevicesChanged, BecomingNoisy }
}

// audio/SoundPoolPlayer.kt
@Singleton
class SoundPoolPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    settingsRepository: SettingsRepository,
    systemAudio: SystemAudioState,
    appVisibility: AppVisibility,
    @param:ApplicationScope private val scope: CoroutineScope,
) : SoundPlayer {
    @Volatile private var soundOn = true        // today's `enabled`, renamed (D-17 fail-open)
    @Volatile private var speakerMuted = false  // Q-010
    @Volatile private var inForeground = true   // P8
    // play(cue, rate):                if (!soundOn || speakerMuted || !inForeground) return
    // playSoundEnabledConfirmation(): if (speakerMuted || !inForeground) return   (D-12 skips only soundOn)
    // init: three independent collectors on `scope`, each: set its flag; if it now blocks, silenceNow()
}
```

- The `AndroidSystemAudioState` constructor does **no IPC**: `AudioManager` is fetched lazily. The
  player is built during Hilt injection in `App.onCreate` on the main thread (`App.kt:93-103`), so
  startup cost stays the same.
- **The detekt constructor-parameter limit** (default 7) is not reached: 5.
- **One collector per input**, not `combine`. `combine` waits for every input to emit, so a system
  reader that never emitted would also switch off the in-app mute. Separate collectors keep SOUND-05
  independent of the new code.
- **The system collector:** `systemAudio.snapshots.map { it.mutesGameSound }.catch { emit(false) }`.
  Without the `catch`, an exception on the `SupervisorJob` application scope would reach the default
  handler and crash the app.

### 1.7 How it interacts with the other settings

- **In-app Sound switch (`soundOn`, SOUND-05):** the two gates combine as an AND and are
  independent. Neither overrides the other. Leaving Vibrate with Sound off stays silent. The switch
  still reads "on" while the ringer mutes the speaker; see Open item O4 for the UX side.
- **`SoundEnabled` pop (D-12):** it keeps bypassing **only** `soundOn`. That bypass exists because
  the DataStore write has not round-tripped yet (`SoundPlayer.kt:56-67`), not to override the
  system. On Vibrate with the phone speaker, switching Sound on gives no pop, and the switch moving
  is the visual partner. With headphones the pop plays. *This is a tech-lead decision: the pop is a
  game sound, and Q-010 mutes game sounds on the speaker.*
- **Volume:** there is no in-app volume. The gate calls no `setStreamVolume` or
  `adjustStreamVolume`. The pool stays `USAGE_GAME`, which routes to the media stream (SOUND-06,
  first half). V1 (§2.5) makes the volume keys drive that stream inside the app.
- **Audio focus:** none is requested. `AndroidSystemAudioState` gets `AudioManager` for **reads
  only**, `requestAudioFocus` and `abandonAudioFocusRequest` stay absent, and
  `audioFocusLiteralCheck` keeps proving it. `SoundPoolPlayer.kt:71`'s "no AudioManager is obtained
  anywhere in this class" stays true.
- **Haptics:** unchanged. `hapticsOn` and the jump haptic do not depend on the ringer, since Q-010 is
  about sound. On Vibrate with the speaker, the jump still buzzes.
- **Visual partners:** unchanged. rules/audio.md already makes sure sound is never the only
  feedback. P1 closes the one gap (`InvalidTap`).

### 1.8 Threading and lifecycle

- Every platform read (`getRingerMode`, `getDevices`, `getAudioDevicesForAttributes`) runs on
  `Dispatchers.Default`, in the collector. Each event costs about a millisecond of binder time and
  happens only on a user's ringer or device action. There is no polling.
- The registrations last the whole process, like the `soundOn` collector. The `awaitClose`
  unregister runs only in tests.
- `silenceNow()` on a transition runs on `Default`, as it already does for `soundOn`. The `play()`
  path gains one volatile read, with no IPC and no allocation.

### 1.9 Failure posture

- **Before the first reading:** unmuted, like `enabled = true` today (D-17). The first reading
  arrives milliseconds after `App.onCreate`. `SoundPool` has not finished decoding any sample by
  then, so in practice no cue can play ahead of it.
- **Reader throws or cannot register:** `catch` emits unmuted. The failure never silences the game
  and never touches the in-app mute.
- **Reader never emits:** the other two collectors are unaffected (§1.6).
- **Logging:** none. The audio package logs nothing today; keep it that way.

### 1.10 Tests

**Pure JVM, runner-free** (like `SoundParamsTest`):

- `SystemAudioSnapshotTest`, one row per ringer × route:
  `normalRingerOnThePhoneSpeakerIsNotMuted`, `normalRingerOnHeadphonesIsNotMuted`,
  `vibrateOnThePhoneSpeakerIsMuted`, `silentOnThePhoneSpeakerIsMuted`,
  `vibrateOnHeadphonesIsNotMuted`, `silentOnHeadphonesIsNotMuted`.
- `AudioRouteClassifierTest`, table-driven:
  - routed `[speaker]` → BuiltIn; routed `[wired headphones]`, `[A2DP]`, `[BLE headset]`,
    `[USB headset]` → External; routed `[speaker, wired headset]` → BuiltIn (duplication counts as
    the phone);
  - routed empty or null, falling through to connected: `{speaker, earpiece}` → BuiltIn;
    `{speaker, wired headset}` → External; `{speaker, BT SCO}` → BuiltIn (call-only headset);
    `{speaker, remote submix}` → BuiltIn (screen recording); `{speaker, hearing aid}` → External;
    an unknown type → BuiltIn;
  - `ringerModeOf` for the three platform values plus an unknown value → Normal.

**Robolectric, SDK 34.** The only `android-all` image cached offline is Android 14; the others are
the i4 builds of 9 and 4.4. So **do not add `@Config(sdk = [<34])`**: it would need a network fetch
and break `./gradlew --offline check`.

`SoundPoolPlayerTest`: the construction gains `systemAudio = FakeSystemAudioState()` and
`appVisibility = FakeAppVisibility()`, both `MutableStateFlow`-backed fakes in the test source set.
Every existing test stays as it is. Add:

- `onVibrateThroughThePhoneSpeakerNothingPlays`
- `onSilentThroughThePhoneSpeakerNothingPlays`
- `onVibrateThroughHeadphonesCuesStillPlay`
- `switchingToVibrateOnThePhoneSpeakerStopsTheStreamsAlreadyInFlight`
- `unpluggingHeadphonesOnVibrateStopsTheStreamsAlreadyInFlight` (route External → BuiltIn)
- `pluggingHeadphonesInOnVibrateLetsTheNextCuePlay`
- `leavingSilentLetsTheNextCuePlayWithoutReplayingAnything`
- `theEnableConfirmationPopIsMutedOnVibrateThroughThePhoneSpeaker`
- `theEnableConfirmationPopStillBypassesTheInAppMute` (today's `theEnableConfirmationPopBypassesTheGate`, kept)
- `leavingVibrateDoesNotOverrideTheInAppMute`
- `aSystemReaderThatThrowsLeavesSoundPlayingAndTheInAppMuteWorking`
- `aSystemReaderThatNeverEmitsLeavesTheInAppMuteWorking`
- the P8 tests in §2.1.

`AndroidSystemAudioStateTest`. Robolectric 4.16's `ShadowAudioManager` implements `setRingerMode` and has
`addOutputDevice(device, notify)`, `removeOutputDevice`, `setAudioDevicesForAttributes` and
`getDevices`, and `AudioDeviceInfoBuilder` exists; all verified in the cached jar. Add:

- `theFirstReadingReportsTheCurrentRingerAndRoute`
- `aRingerModeBroadcastProducesANewReading` (set the mode, send the broadcast, idle the main looper)
- `anAddedWiredHeadsetMovesTheRouteOffThePhone` (no attribute mapping, so this also exercises the
  presence inference)
- `thePlatformRouteWinsOverConnectedDevicesOnApi33` (A2DP connected, but routed to the speaker →
  BuiltIn)
- `theRouteQueryAsksAboutTheAttributesThePoolPlaysWith` (map devices only for
  `GameAudioAttributes.build()`)
- `becomingNoisyReportsThePhoneAtOnce`
- `cancellingTheCollectionUnregistersBothListeners`

`ProcessAppVisibilityTest`: build the class on a `LifecycleRegistry.createUnsafe(owner)` and check
`startedIsForegroundAndCreatedIsBackground`.

The pre-33 platform branch is a single `if`. It is covered by the classifier tests above and by
device check D9.

**Device checks for the qa-engineer.** These need a **physical phone** with wired (USB-C or 3.5 mm)
and Bluetooth headphones; an emulator cannot plug headphones. Ringer changes use the volume panel.

| # | Setup | Expect |
|---|---|---|
| D1 | Normal, speaker | every cue as today (regression) |
| D2 | Vibrate, speaker | no cue anywhere (board, tutorial jump, Results stars, buttons, Settings enable pop); jump haptic still fires; every visual partner present |
| D3 | Silent, speaker | as D2 |
| D4 | Vibrate + wired headphones | cues play; pull the plug during the star pops → the tail stops and nothing leaves the speaker; plug in → the next cue plays |
| D5 | Vibrate + Bluetooth headphones | cues play; switch Bluetooth off mid-game → silent on the speaker |
| D6 | Normal, speaker, win a level; flip to Silent during the stars | tails stop at once |
| D7 | Any, press Home during the star pops (P8) | sound stops within about 1 s, later pops do not sound; back in the game, the next cue plays |
| D8 | Settings, Sound off → on | Vibrate + speaker: no pop; Vibrate + headphones: pop |
| D9 | An API 24–32 device or emulator | D2 and, if headphones can be attached, D4 (the inference path) |
| D10 | In game, nothing playing, press volume down repeatedly (V1) | the media volume moves, the ringer does not; the phone never drops into Vibrate |

### 1.11 Cost

| Item | Production code | Tests | Developer time |
|---|---|---|---|
| Silent/Vibrate gate | 4 new files (about 250 lines with KDoc); `SoundPoolPlayer` about +35; `AudioModule` +8 | about 25 | about 1 day |
| P8 background | 1 new file (about 40); one collector | 4 | about 0.25 day |
| P4a + nit | 1 constant, KDoc | 1 rename | about 0.1 day |
| P5 | none | credits test rewrite + a new format test (about 200 lines) | about 0.5 day |
| V1 | 1 line + KDoc | device check only | under 0.1 day |

- **Total:** about **2 developer-days** (one plan of about 5 tasks), plus one **2–3 h device
  session** on a physical phone with headphones, plus one review loop.
- **Runtime:** no new dependency, no permission, no manifest entry and no polling. One broadcast
  receiver and one device callback live for the process. Each cue does zero IPC and allocates
  nothing. Each ringer or device event costs about 1 ms of binder time off the main thread. The APK
  grows by a few KB of dex.
- **Against Q-010 option 1** (mute on Silent/Vibrate everywhere): that would drop the route reader,
  BECOMING_NOISY and the device callback, for about 0.4 day and no headphone hardware in QA. The
  headphone exception the founder chose costs about 0.6 day more and needs a physical device.

### 1.12 Alternatives considered and rejected

- **Re-read at play time:** §1.4.
- **Moving the pool to `USAGE_ASSISTANCE_SONIFICATION` or the legacy system stream,** so the
  platform's own ringer muting applies. This takes the game off the media volume (SOUND-06, first
  half, and the media slider the player uses). The platform would also mute it on headphones, which
  is option 1, not option 2.
- **The deprecated `isWiredHeadsetOn()` and `isBluetoothA2dpOn()`:** deprecated, compiler warnings,
  and they miss USB, LE audio and hearing aids.
- **Reusing `AppForegroundTracker` for P8:** §2.1.

---

## 2. Rulings on the sound-designer's other developer items

### 2.1 P8: silence one-shots on background (approved)

- **Trigger:** the *process* going to the background (`ProcessLifecycleOwner` `ON_STOP`), not an
  Activity pause. D-16's reasoning applies unchanged: a same-process interstitial is not a
  background.
- **Behaviour:** on background, `silenceNow()` runs and new cues are **blocked**, the
  `SoundEnabled` pop included, until the process is foregrounded again. On foreground, nothing
  replays.
- **Why blocking, not only stopping:** `StarPopSounds`' coroutine keeps its `delay`s running after
  the app leaves (`ui/results/ResultsCelebration.kt:105-113`), so without the block, star pops would
  sound from a backgrounded app.
- **How:** `ProcessAppVisibility` maps `ProcessLifecycleOwner.get().lifecycle.currentStateFlow`
  (androidx.lifecycle 2.9.4, already on the graph) to `isAtLeast(STARTED)`, then
  `distinctUntilChanged()`. Give it an `internal` primary constructor that takes a `Lifecycle`
  (for the test) and an `@Inject constructor()` that passes the process lifecycle.
- **Startup:** the process lifecycle is `CREATED` until `MainActivity` starts, so the first
  collected value is `false`. The resulting `silenceNow()` on an empty pool is harmless, and no cue
  exists before the Activity starts.
- **Not through `AppForegroundTracker`.** That class is the interstitial session boundary: it lives
  in `platform/`, has no interface, and drives `InterstitialCadenceStore`. Making audio depend on it
  would widen `platform`'s role and couple audio tests to the ad cap. Both classes read the same
  `ProcessLifecycleOwner`, so they agree on what "background" means.
- **Known latency:** `ProcessLifecycleOwner` dispatches `ON_STOP` about 700 ms after the last
  Activity stops, so a tail can ring up to about 0.7 s after leaving. That is acceptable.
- **Tests** (in `SoundPoolPlayerTest`): `goingToTheBackgroundStopsTheStreamsAlreadyInFlight`,
  `nothingPlaysWhileTheAppIsInTheBackground`, `returningToTheForegroundLetsTheNextCuePlay`.
  Device check: D7.

### 2.2 P4a: `VOLUME_JUMP` 0.9 → 1.0 with the jump re-level (approved, with conditions)

The asset half is **already on this branch**: d1c6e9b takes `sfx_jump.wav` from −0.02 to −1.10 dBFS.
Until the constant moves, the jump plays about 1 dB quieter than the founder's tuning.

1. **Land `VOLUME_JUMP = 1.0f` on this branch before it merges to develop.** The two halves must
   never reach develop apart:
   - the constant alone would put the jump + removal mix at about 0 dBFS, at the clipping edge;
   - the asset alone would leave the jump 1 dB down.
2. **KDoc for `VOLUME_JUMP`:** record why it is 1.0 (the peak rule, P4a), and that 1.0 is
   `SoundPool`'s ceiling. The jump now has no code headroom. A louder jump would be an asset change,
   and that asset sits 0.1 dB under the peak rule, so it would take loudness processing, not gain.
3. **KDoc for `VOLUME_INVALID` (`SoundParams.kt:169`):** "0.3 against [VOLUME_JUMP]'s 0.9" names
   the constant, not the literal: "0.3 against [VOLUME_JUMP]".
4. **KDoc for `VOLUME_WIN` (`SoundParams.kt:176`):** "the reference all others sit under" becomes
   "no cue is mixed above it". The jump now ties it at 1.0 and still sits about 8 LU under the win,
   because the level difference lives in the assets (spec, Mix table).
5. **Test:** rename `SoundParamsTest.theWinStingerIsTheLoudestCue` to
   `noCueIsMixedAboveTheWinStinger` and assert `SoundCue.entries.all { it.volume <= SoundCue.Win.volume }`.
   The current assertion passes on a tie, but its name would then claim something the numbers no
   longer show. `theRejectedTapIsQuieterThanTheJump` and `theRemovalSitsUnderTheJump…` pass
   unchanged.

### 2.3 P5: the credits test for WAV and in-house recipes

**Today's test fails on this branch.** `AudioCreditsTest` keeps only files with the extension
`ogg` (`AudioCreditsTest.kt:27,88`). Since af2b554 and d1c6e9b, `res/raw` holds twelve `.wav` files
and no `.ogg`, so `assertThat(soundFiles).isNotEmpty()` fails in both tests. `./gradlew check` is red
here until this item lands, which is one more reason §3 builds on this branch.

**The contract with `docs/audio/CREDITS.md`.** The sound-designer is editing that file, and this is
what the test needs from it:

- the test reads **only the first table under `## Files`**. The history sections name the old
  `.ogg` files and must not count;
- columns are found **by header text**, not by position: `res/raw filename`, `original filename`,
  `source`, `licence`, plus optional `author` and `sha256`. Column order is free, and extra columns
  are allowed.

**Assertions**, all runner-free file scans as today:

| Row kind | Recognised by | Asserted |
|---|---|---|
| every row | — | the first cell is exactly one backticked file name; **every file in `res/raw`, whatever its extension, has exactly one row**, matched by an **exact** first-cell match, not today's `line.contains` (which only works because of row order); every row names a file that exists |
| **sourced** | the `source` cell contains `https://` | the `licence` cell is in an allowlist, today exactly `CC0 1.0`. A new licence enters the list only by a reviewed test edit, checked against rules/ip-policy.md: CC-BY also needs the licences screen, and NC, ND or SA never enter. `original filename` is non-empty. If an `author` column exists, it is non-empty. |
| **in-house** | the `source` cell is a repo path | the path equals `audio/recipes/<stem>.json`, where `<stem>` is the file name without `sfx_` and `.wav` (`sfx_invalid.wav` → `audio/recipes/invalid.json`); the recipe file exists; `licence` is non-empty (the sound-designer's label for studio work); no https requirement |
| neither | — | fails, naming the file |
| recipes | — | every `audio/recipes/*.json` is named by exactly one row (no orphan recipe) |
| `sha256`, if the column exists | — | each committed file's sha256 equals its cell. WAV output is deterministic, so this pins the exact bytes that were reviewed |

**Not asserted, by design:**

- **Byte-identical re-render.** It needs the plugin's `sfx-synth.sh`, which is outside the repo and
  not on CI. It stays in the sound-designer's review: `sfx-synth.sh --check`, then render twice and
  `cmp` against the committed WAV.
- **The research-analyst verdict.** It belongs in `THIRD_PARTY.md` (P6), which is not on develop.
  Add a cross-check when that file lands.

**New `AudioAssetFormatTest`**, runner-free, one test per rule in rules/audio.md that a file can
prove:

- every file in `res/raw` is `.wav`;
- the file is RIFF/WAVE and its `fmt ` chunk says PCM (1), 1 channel, 44,100 Hz, 16 bits. **Walk
  the chunks:** ffmpeg writes a `LIST` chunk before `data` unless told to be bitexact. Today's files
  have clean 44-byte headers, and the test must not depend on that;
- size is at most 200 KB (204,800 bytes). The star pop is 145,574 B;
- the sample peak is at most −1 dBFS: the largest |sample| ≤ `Short.MAX_VALUE × 10^(−1/20)`, about
  29,203, computed in the test from a named `-1.0` constant;
- `res/raw` holds exactly `SoundCue.entries.size` files, so an orphan asset fails.

The in-game mix peak is not a property of any one file and stays in the sound-designer's
measurements.

**KDoc sweep in the same task.** `sfx_deselect.ogg` becomes `.wav` at `SoundCue.kt:44` and
`SoundParams.kt:137,155`. Afterwards, `grep -rn '\.ogg' app/src` must return nothing. The measured
figures in `VOLUME_DESELECT`'s KDoc (the −5.48 dBFS peaks and the RMS pair) change only if the
conversion notes in CREDITS.md record a change. Take the numbers from there; do not re-measure in
the developer step.

### 2.4 The `MAX_STREAMS` KDoc nit (approved, comment only)

- **The sound-designer is right.** `starPopRate` plays the first two pops slowed, at 0.630 and
  0.794, so they last 2.62 s and 2.08 s, not 1.65 s.
- **Corrected timings with animations on** (500 ms stagger): the pops start at 250, 750 and 1250 ms
  and end at about 2870, 2830 and 2900 ms. All three ring together from **1250 to about 2830 ms**,
  not 1250–1900.
- **With animations off** (625 ms fallback): the pops start at 250, 875 and 1500 ms, all three
  overlap from 1500 to about 2870 ms, and the last tail ends at about 3150 ms.
- **The conclusion holds:** the win stinger (0.54 s) can overlap only the first pop, so the peak is
  four voices against eight, and oldest-first eviction is never reached.
- **Suggested replacement for the sentence at `SoundParams.kt:102-104`:** "the pops fire 250 /
  750 / 1250 ms after Results entry; the first two play slowed (rates 0.63 and 0.79,
  [starPopRate]), so they last 2.62 s and 2.08 s rather than 1.65 s, and all three sound together
  from 1250 ms until about 2830 ms, the last tail ending at about 2900 ms (with animations off, the
  625 ms fallback stagger moves that window to 1500–2870 ms and the end to about 3150 ms)."
- The rest of the KDoc stands.

### 2.5 V1: the volume keys drive game sound inside the app (added)

- **The problem.** `MainActivity` sets no `volumeControlStream`. With nothing playing, the hardware
  volume keys adjust the platform's no-playback default stream. That is the **ringer** on Android
  7.0–8.1, and on OEM builds that keep the ringer as that default (for example Samsung's "Use
  Volume keys for media" setting, which is off by default).
- **Why it matters now.** Most of the time no one-shot is playing, so a player who presses volume
  down to quiet the game can step the phone into **Vibrate**. With the §1 gate, the game then goes
  silent on the speaker, which the player would read as a bug.
- **Build:** `volumeControlStream = AudioManager.STREAM_MUSIC` in `MainActivity.onCreate`, after
  `super.onCreate` (`MainActivity.kt:133`). Add one KDoc line saying why. It is not a focus call and
  does not trip `audioFocusLiteralCheck`.
- **Player-visible change:** inside the game the volume keys move the media volume, which is what
  game sound plays on. Outside the app nothing changes.
- **No unit test.** `MainActivity` cannot be built under Robolectric without a Hilt test harness,
  which the project does not have. Device check D10 covers it.
- *Tech-lead decision.* The sound-designer or ux-designer may object in review.

---

## 3. Sequencing against phase 15

`git diff develop...feature/phase-15-ux-refresh --stat -- <every file in §1.5 and §2>` prints
**nothing**. The files covered are `audio/SoundPoolPlayer.kt`, `SoundParams.kt`, `SoundCue.kt`,
`SoundPlayer.kt`, `di/AudioModule.kt`, `App.kt`, `MainActivity.kt`, `app/src/test/.../audio/`,
`build-logic/`, `res/raw/`, `audio/`, `docs/audio/`, `app/build.gradle.kts`, `AndroidManifest.xml`
and `gradle/libs.versions.toml`. **No file this design edits is changed by phase 15**, and the
`SoundPlayer` interface that phase 15's callers use does not change.

Phase 15 does change neighbouring files: `ui/game/GameScreen.kt` (329 lines),
`audio/JumpSoundEffect.kt` (new), the tutorial (`ui/onboarding/*`), `ui/settings/SettingsScreen.kt`
(63 lines), `docs/ux-spec.md`, `.planning/questions/FOUNDER.md` (88 lines) and
`.planning/reports/STATUS.md` (49 lines). None of them is edited here.

**The plan is confirmed: build on `feature/audio-review` only after `develop` contains phase 15.**
First merge develop into this branch, then build. The work could technically go in now without a
conflict, but waiting is still right, for five reasons:

1. **QA has to run against the UI that ships.** The tutorial's jump cues (`JumpSoundEffect`, D-01i)
   and the dock exist only on phase 15. D2 must show them muted on the phone speaker.
2. **The removal layer needs phase 15's fix.** Phase 15's 3400828 fixes the removal layer with
   animations off (P9), so the D1 regression would be measured against a known defect on develop
   today.
3. **P1 overlaps phase 15 heavily.** P1's visual partner lands in `GameScreen.kt`, the board and the
   tutorial, all heavily changed by phase 15. It goes to the same developer, and one base avoids a
   second rebase.
4. **FOUNDER.md conflicts.** Merging develop in after phase 15 **will conflict in
   `.planning/questions/FOUNDER.md`**, where both sides add questions at the top (Q-003–Q-005 there,
   Q-008–Q-011 here). Only the producer writes that file, so the producer resolves it.
5. **A possible Settings line would touch phase 15's file.** If the ux-designer adds the Settings
   line in O4, it goes in `SettingsScreen.kt`, which phase 15 rewrote.

**This branch is red now, so its developer items land before it merges.** Commits af2b554 and
d1c6e9b make `AudioCreditsTest` fail (§2.3) and leave the jump 1 dB down (§2.2). The P5 and P4a
changes therefore land on this branch before it merges to develop.

---

## 4. Dependencies and licences

**No new dependency.**

- The framework: `AudioManager`, `AudioDeviceInfo`, `AudioDeviceCallback`, `BroadcastReceiver`.
- Libraries already on the graph:
  - `androidx.core` 1.18.0 (Apache-2.0), for `ContextCompat.registerReceiver`;
  - `androidx.lifecycle:lifecycle-process` 2.9.4 (Apache-2.0), for `ProcessLifecycleOwner`;
  - kotlinx-coroutines (Apache-2.0).

No copyleft. `THIRD_PARTY.md` does not change.

**Performance budgets:** the cue path is unchanged apart from one volatile read, cold start is
unchanged (no IPC in any constructor), and nothing runs between events.

---

## 5. Build order (one plan, TDD)

Start after develop, with phase 15 merged, has been merged into this branch. The sound-designer's
assets (af2b554, d1c6e9b) and the finished `docs/audio/CREDITS.md` must be committed first.

1. **P5 and the KDoc sweep:** `AudioCreditsTest` rewrite and `AudioAssetFormatTest`. This turns the
   branch green.
2. **P4a and the nit:** `SoundParams`, and the `SoundParamsTest` rename.
3. **The gate:**
   - first the pure pieces (`SystemAudioSnapshot`, `AudioRouteClassifier`), test first;
   - then `GameAudioAttributes` and `AndroidSystemAudioState`, with their Robolectric tests;
   - then `SoundPoolPlayer` and `AudioModule`, with the player tests;
   - then the gate's failure text.
4. **P8:** `AppVisibility`, its collector and tests.
5. **V1:** `MainActivity`.

Then `./gradlew check`, the code-auditor, the qa-engineer's device session (D1–D10), and the
sound-designer's integration review against rules/audio.md.

**Must not:**

- request audio focus;
- change any stream volume;
- poll;
- make a binder call on the `play()` path;
- read a device's address or name;
- change `SoundPlayer`;
- carry the gate in a `UiState`;
- depend on `AppForegroundTracker`;
- add a Robolectric SDK other than 34;
- add `@Suppress`, `@SuppressLint` or a baseline.

---

## 6. Risks and open items

| # | Item | Owner | Blocks |
|---|---|---|---|
| O1 | A route change with no device change (the output switcher) goes unseen until the next event (§1.4). Accepted. | — | nothing |
| O2 | On API 24–32, a Bluetooth sink that is connected while media has been moved back to the phone reads as External, so sound plays on Vibrate. This is the loud direction, but it needs the Android 11–12 output switcher, so it is rare. Accepted. | — | nothing |
| O3 | D4, D5 and D9 need a physical phone with wired and Bluetooth headphones. If none is attached, the qa-engineer records them as not run and the producer decides. | producer, qa-engineer | the audio sign-off |
| O4 | While the ringer mutes the speaker, Settings shows Sound "on" with no explanation. The ux-designer decides whether the switch gets a supporting line (for example "Quiet on Silent or Vibrate; headphones still play"). A static line needs no code; a live one would collect `SystemAudioState` in `SettingsViewModel`. | ux-designer | nothing |
| O5 | The build's review runs on Fable (code-auditor, qa-engineer). Q-011 covered only the four design steps, so if Fable is still exhausted when the build is reviewed, that is a new founder question. | producer | the build's merge |
| O6 | This design was written on Opus. When Fable is back, a fresh Fable tech-lead may re-check it (rules/studio.md, Model and effort). | producer | nothing |
| O7 | The OEM ringer controls tested are only those on the QA device. Alert sliders set the same platform ringer mode, so the gate reads them the same way. | qa-engineer | nothing |
| O8 | `InlinedApi` lint warnings for the newer device-type constants (§1.3). They are warnings, and no suppression is added. | code-auditor | nothing |
