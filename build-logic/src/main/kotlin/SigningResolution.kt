import java.io.File

/**
 * The environment-variable names for the four upload-signing credentials (PLAT-08 / D-16,
 * rules/play-store.md, `14-OPERATOR-CHECKLIST.md` Step 7).
 *
 * **Environment only, and that is the whole interface.** [AdIdKeys] and [GamesIdKeys]
 * deliberately give each key two sources — an environment variable *or* an untracked
 * `local.properties` entry — because those IDs ship inside the APK and are not secrets. These
 * four are the opposite: a store password recovered from a file is a compromised upload key,
 * and a file that can be read can be committed, archived or synced. So there is no
 * `local.properties` half here, and `local.properties.template` says so in as many words.
 */
object SigningKeys {
    /** Absolute path to the upload keystore, which lives **outside** this repository. */
    const val KEYSTORE = "PLAY_UPLOAD_KEYSTORE"

    /** The alias of the upload key inside that keystore. */
    const val KEY_ALIAS = "PLAY_UPLOAD_KEY_ALIAS"

    /** The password that opens the key held under [KEY_ALIAS]. */
    const val KEY_PASSWORD = "PLAY_UPLOAD_KEY_PASSWORD"

    /** The password that opens the keystore itself. */
    const val STORE_PASSWORD = "PLAY_UPLOAD_STORE_PASSWORD"

    /** All four, in the order the failure message reports them. */
    val ALL = listOf(KEYSTORE, KEY_ALIAS, KEY_PASSWORD, STORE_PASSWORD)
}

/** The four credentials a release package needs, or the reason it cannot be built. */
sealed interface SigningResolutionResult {
    /**
     * Every variable was set. [toString] is overridden because the generated one would print
     * two passwords, and a Gradle stack trace or a `println` while debugging is exactly how a
     * credential reaches a log file that later gets pasted somewhere.
     */
    data class Resolved(
        val keystorePath: String,
        val keyAlias: String,
        val keyPassword: String,
        val storePassword: String,
    ) : SigningResolutionResult {
        override fun toString(): String = "SigningResolutionResult.Resolved(redacted)"
    }

    /**
     * At least one variable is absent or unusable. [message] names the variable — never its
     * value — and how to set it; the caller wraps it in a `GradleException` (mirroring D-09).
     */
    data class Failure(val message: String) : SigningResolutionResult
}

/**
 * Resolves the release upload-signing credentials from the environment (PLAT-08 / D-16).
 *
 * **Sibling of [AdIdResolution] and [GamesIdResolution], same Gradle-free shape** — `Map` in,
 * result out, no `Project`, no `Provider` — and for the same reason: AGP 9.1.0 gives `:app` no
 * `testReleaseUnitTest`, so `SigningResolutionTest` in this included build is the only place
 * the release-failure contract can be proven at all.
 *
 * Two rules differ from its siblings, both deliberately:
 *  - **No `local.properties` source and no placeholder prefix.** See [SigningKeys].
 *  - **Nothing is trimmed.** [AdIdResolution] trims, because whitespace around an ad unit ID
 *    is always an accident. Whitespace inside a password is a character of the password;
 *    trimming it would turn a correct password into a wrong one and surface later as an
 *    unrecoverable-key error with no explanation. A value counts as absent when it is blank,
 *    and is otherwise passed through byte for byte.
 *
 * There is no debug half at all. Debug builds are signed by AGP's own debug keystore and this
 * resolver is never consulted for them — which is what keeps "a fresh clone builds" true.
 */
object SigningResolution {
    fun resolve(env: Map<String, String>): SigningResolutionResult {
        val configured = SigningKeys.ALL.associateWith { key -> present(env[key]) }

        val missing = SigningKeys.ALL.filter { configured[it] == null }
        if (missing.isNotEmpty()) {
            return SigningResolutionResult.Failure(missingMessage(missing))
        }

        // Every key is non-null here — the `missing` filter above is the proof.
        fun required(key: String): String =
            configured[key] ?: error("unreachable: $key was reported present")

        val keystorePath = required(SigningKeys.KEYSTORE)
        // A relative path would be resolved against `app/`, which is both surprising and
        // silent: it names a file that almost certainly does not exist, and the operator
        // checklist asks for an absolute path precisely so this cannot happen.
        if (!File(keystorePath).isAbsolute) {
            return SigningResolutionResult.Failure(notAbsoluteMessage())
        }

        return SigningResolutionResult.Resolved(
            keystorePath = keystorePath,
            keyAlias = required(SigningKeys.KEY_ALIAS),
            keyPassword = required(SigningKeys.KEY_PASSWORD),
            storePassword = required(SigningKeys.STORE_PASSWORD),
        )
    }

    /** `null` for absent or blank; the value **verbatim** otherwise — see the class KDoc. */
    private fun present(raw: String?): String? = raw?.takeIf { it.isNotBlank() }

    /**
     * The message a release invocation dies with. In the register of [AdIdResolution]'s: it
     * names the exact variable, says how to set it, and says that debug needs none of this —
     * a reader hitting it should not have to open a build file to get unblocked.
     *
     * It carries no value of any kind. A Gradle log gets pasted into issues and reports, and
     * the one thing that must never travel that way is the content of these four variables.
     */
    private fun missingMessage(missing: List<String>): String =
        buildString {
            appendLine("Release build requires the Play upload signing credentials.")
            missing.forEach { key ->
                appendLine("Missing: $key")
                appendLine("  export $key=… in the shell that runs the build.")
            }
            appendLine(
                "These four are cryptographic secrets: environment variables only — never " +
                    "local.properties, never a committed file, never a Gradle property.",
            )
            append("Debug builds are signed with the local debug key and need none of this.")
        }

    private fun notAbsoluteMessage(): String =
        buildString {
            appendLine("${SigningKeys.KEYSTORE} must be an absolute path.")
            appendLine(
                "  A relative path is resolved against the app module directory, which would " +
                    "silently name the wrong file — and the keystore belongs outside this " +
                    "repository entirely.",
            )
            append("Re-export it as an absolute path in the shell that runs the build.")
        }
}
