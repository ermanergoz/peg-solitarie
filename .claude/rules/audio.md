# Audio rules (games)

Cues: every sound is listed in `docs/audio-spec.md` with the event that fires
it. Nothing plays that the spec does not name; no asset ships that no cue uses.

Assets: in the asset directory of the stack (android-compose-game:
`app/src/main/res/raw/sfx_<cue>.wav`), mono 16-bit 44.1 kHz WAV (OGG for
sourced music), at most 200 KB per sound effect, peak at most -1 dBFS.

In-house: rendered from a committed recipe `audio/recipes/<cue>.json` with
`${CLAUDE_PLUGIN_ROOT}/scripts/sfx-synth.sh`; the WAV is committed beside the
recipe and re-renders byte-identically from it.

Sourced: every sourced asset has a THIRD_PARTY.md row — name, source URL,
license, and the verdict of the research-analyst (rules/ip-policy.md). Files
the founder brings arrive in the local, git-ignored `.planning/audio-inbox/`
and are normalised (format, trim, loudness; `ffmpeg` when it is installed)
before they are committed.

Playback: one class in the UI layer owns it, never `game-core`. It preloads
(SoundPool or the equivalent of the stack), triggers on state changes, honours
the mute and volume settings of the product and the system silent mode, and
does no audio work on the main thread beyond the play call.

Accessibility: sound is never the only feedback for an event; every cue is
paired with a visual or haptic one (docs/ux-spec.md, Feedback).

Review: the sound-designer reviews the integration diff against this file and
the spec; the qa-engineer checks on the device that every cue fires on its
event.
