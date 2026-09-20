package com.app.pegsolitaire.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Scoped constants for the two Play Games opt-in Preferences keys (QUAL-03 — no inline
 * magic strings), mirroring [OnboardingPreferences].
 *
 * Both live in the shared `app_prefs` store (see [AppPreferences]) rather than a store of
 * their own — the D-17 answer is one more launch-time preference alongside
 * `onboarding_seen`, not a new persistence concern.
 */
internal object PlayGamesPreferences {
    /** Whether the one-time opt-in prompt has been answered either way (D-15/D-17). */
    val PROMPT_ANSWERED_KEY = booleanPreferencesKey("play_games_prompt_answered")

    /** Fail-open default (D-17): a missing/corrupt read is treated as "never asked". */
    const val DEFAULT_PROMPT_ANSWERED = false

    /** Whether the player opted in to Play Games — D-01's launch-time gate. */
    val OPTED_IN_KEY = booleanPreferencesKey("play_games_opted_in")

    /** Fail-open default (D-17): a missing/corrupt read never fabricates an opt-in. */
    const val DEFAULT_OPTED_IN = false

    /**
     * Whether a Play Games submission is outstanding — D-10's "submissions pending"
     * marker, read and written by [SubmissionSyncStore] and nothing else.
     *
     * Set whenever a solve is recorded while
     * [com.app.pegsolitaire.platform.games.GamesService.session] is not
     * `SignedIn` — which includes `Unknown`, the state before the first check answers —
     * and whenever ANY submission in a pass reports `false` or throws. Cleared only when
     * a pass that actually issued calls saw every one of them succeed.
     *
     * The key string lives here and nowhere else (QUAL-03, CLAUDE.md's no-magic-strings
     * rule), beside the two opt-in flags, so every `play_games_*` name in the shared
     * `app_prefs` store is declared in one scoped constants object.
     */
    val SUBMISSIONS_PENDING_KEY = booleanPreferencesKey("play_games_submissions_pending")

    /**
     * Fail-open default: a missing key (fresh install) or a corrupt/IO read means
     * "nothing pending".
     *
     * **The tradeoff is real and the direction is deliberate.** `true` would be *more*
     * recoverable — every submission is idempotent (absolute steps, D-12) and an empty
     * database issues no SDK calls at all, so a spurious pass costs nothing — but it
     * contradicts D-10's "clean launches make no calls", which is the property that keeps
     * a player who has never signed in away from the Play Games code path entirely. `false`
     * is taken instead, and its worst case is bounded: at most one solve's delay before the
     * next write re-sets the flag and the backfill resumes.
     */
    const val DEFAULT_SUBMISSIONS_PENDING = false

    /**
     * Whether a cloud-save push is outstanding — D-01's retry marker, read and written by
     * [CloudSyncStore] and nothing else.
     *
     * Set when a push reports `false` or throws, and whenever a solve is recorded while
     * [com.app.pegsolitaire.platform.games.GamesService.session] is not `SignedIn` —
     * which includes `Unknown`, the state before the first check answers. Cleared only on
     * a push that actually succeeded.
     *
     * **This is deliberately not the same flag as [SUBMISSIONS_PENDING_KEY].** One pass
     * can succeed at the leaderboard and achievement submissions and still fail at the
     * Saved Games snapshot — they are different backends and they fail independently.
     * Collapsing them into one boolean would make either outcome unrecoverable on its own:
     * clearing it because the leaderboards landed would lose the snapshot retry, and
     * retaining it because the snapshot failed would re-issue submissions that already
     * succeeded. Two markers, two lifetimes.
     */
    val CLOUD_PUSH_PENDING_KEY = booleanPreferencesKey("play_games_cloud_push_pending")

    /**
     * Fail-open default: a missing key (fresh install) or a corrupt/IO read means "no push
     * outstanding".
     *
     * **The tradeoff is real and the direction is deliberate**, and it is the same one
     * [DEFAULT_SUBMISSIONS_PENDING] makes. `true` would be *more* recoverable — the push
     * is a monotonic union merge (CLOUD-02), so a spurious push can only ever re-send
     * progress the cloud already holds — but it contradicts the "clean launches make no
     * calls" property D-01 inherits, which is what keeps a player who has never signed in
     * away from the Play Games code path entirely. `false` is taken instead, and its worst
     * case is bounded: at most one solve's delay before the next recorded result re-sets
     * the marker and the push is retried on that pass.
     */
    const val DEFAULT_CLOUD_PUSH_PENDING = false

    /**
     * The stage the most recent **attempted** cloud push ended at — a
     * [com.app.pegsolitaire.platform.games.save.CloudSaveStage] name, written in the same
     * edit as [CLOUD_PUSH_PENDING_KEY] so the two can never describe different pushes.
     *
     * A device diagnostic and nothing else. **Nothing in the app reads it**: it changes no
     * behaviour, reaches no UI, and the retry bookkeeping still runs off the boolean alone
     * (CLOUD-05, D-03). It exists because a marker stuck at `true` on a signed-in, online
     * device said *that* pushes were refused and nothing about *where*, and logging — the
     * other way to find out — is what T-11-04's grep gate forbids in this package. It is
     * read off a debuggable build through `run-as`; on a release build it is app-private
     * and unreadable.
     *
     * Not player data: one of seven names this game defines. Overwritten on every push.
     */
    val CLOUD_LAST_PUSH_STAGE_KEY = stringPreferencesKey("play_games_cloud_last_push_stage")

    /**
     * The Play services status integer the most recent attempted push failed with.
     * **Absent**, not zero, whenever that push carried none — a success, a missing
     * Activity, a non-SDK failure — because a stale code beside a newer stage would be read
     * as belonging to it.
     *
     * The integer only. The exception it came from, and above all its message, are never
     * stored (T-11-04 / T-13-40).
     */
    val CLOUD_LAST_PUSH_STATUS_KEY = intPreferencesKey("play_games_cloud_last_push_status")

    /**
     * Whether the most recent cloud **pull** got as far as an opened snapshot.
     *
     * The first of four `play_games_cloud_last_pull_*` keys, written together in one edit by
     * [CloudSyncStore.recordPullProbe] from a
     * [com.app.pegsolitaire.platform.games.save.CloudPullProbe]. They are the read path's
     * counterpart to [CLOUD_LAST_PUSH_STAGE_KEY] and carry the same contract: a device
     * diagnostic, **read by nothing in the app**, changing no behaviour and reaching no UI
     * (CLOUD-05, D-03), readable through `run-as` on a debuggable build only.
     *
     * `false` means the other three are **absent**, not zero — the pull failed before any
     * snapshot opened, so there was nothing to read them from. Why it failed is the push
     * diagnostic's job: `open` is shared.
     *
     * None of the four is player data: a boolean, a boolean, a count and a timestamp. No SDK
     * string — no device name, snapshot id, description or payload — is ever stored.
     */
    val CLOUD_LAST_PULL_OPENED_KEY = booleanPreferencesKey("play_games_cloud_last_pull_opened")

    /**
     * `SnapshotMetadata.hasChangePending()` as the last pull read it: whether the snapshot still
     * held changes that had **not been uploaded**. The datum a locally-`COMMITTED` push cannot
     * supply — `true` here, on a device that has been online for minutes, is a commit that never
     * left it.
     */
    val CLOUD_LAST_PULL_CHANGE_PENDING_KEY = booleanPreferencesKey("play_games_cloud_last_pull_change_pending")

    /** How many conflicts the last pull resolved before its snapshot opened; `0` is the quiet case. */
    val CLOUD_LAST_PULL_CONFLICTS_RESOLVED_KEY = intPreferencesKey("play_games_cloud_last_pull_conflicts_resolved")

    /**
     * The opened snapshot's last-modified time, epoch millis, as the SDK reported it. Set against
     * the time of this device's own last push it answers "has anyone else written since?" — the
     * question a device-name comparison was meant to answer and could not reliably.
     */
    val CLOUD_LAST_PULL_LAST_MODIFIED_AT_KEY = longPreferencesKey("play_games_cloud_last_pull_last_modified_at")

    /**
     * The Play Games player id this device's local progress was last merged with — D-12's
     * account-switch gate, read and written by [CloudProfileStore] and nothing else.
     *
     * The value stored is
     * [com.app.pegsolitaire.platform.games.GamesSignInState.SignedIn.playerId], compared
     * against the signed-in id at the start of every submitter pass, *before* any pull or
     * push; a mismatch raises the one-time Home profile-swap dialog (D-12/D-15) instead of
     * merging anything.
     *
     * **There is deliberately no `DEFAULT_*` constant for this key.** Absence is modelled
     * as Kotlin `null`, which is the whole point: a missing key (fresh install, first-ever
     * sign-in) and a corrupt/IO read both read as `null`, and `null` means **merge
     * silently per D-02** — never "show the profile-swap dialog". The direction matters
     * because that dialog owns the only destructive path in the app (D-13/D-14's
     * "Replace" wipes local results, sessions, ladder and both resume snapshots). Backwards,
     * a damaged preferences file could offer a player the option to wipe their own device;
     * this way round the worst case is one silent, lossless union merge. A default constant
     * could only be some sentinel id, and any sentinel is a value that might accidentally
     * equal — or accidentally differ from — a real player id.
     *
     * The id is never logged (T-11-04): it identifies the player, and this app-private
     * store is the only place it is written.
     */
    val MERGED_PLAYER_ID_KEY = stringPreferencesKey("play_games_merged_player_id")
}
