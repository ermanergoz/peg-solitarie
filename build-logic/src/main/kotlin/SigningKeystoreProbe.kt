import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

/** Whether the resolved credentials actually open a key, or why they do not. */
sealed interface SigningKeystoreProbeResult {
    /** The file exists, the store opened, the alias is present and its key was recovered. */
    data object Usable : SigningKeystoreProbeResult

    /** [message] says **which** of those four steps failed, naming variables, never values. */
    data class Failure(val message: String) : SigningKeystoreProbeResult
}

/**
 * Opens the upload keystore the way the signer will, before anything is packaged (PLAT-08).
 *
 * [SigningResolution] proves the four variables are *set*; this proves they are *right*.
 * Without it the first thing that touches the keystore is AGP's signer, at packaging time,
 * and its diagnosis is a single opaque line ("Failed to read key … from store …") that does
 * not distinguish a mistyped path from a wrong store password from a wrong alias — the three
 * mistakes an operator actually makes. Worse, whichever way it fails, it fails *after* a
 * release variant has been assembled, which is the state
 * `14-RELEASE-PLAN.md` §4a calls out: "a 'successful' unsigned build is precisely what gets
 * mistaken for a release candidate."
 *
 * `KeyStore.getInstance(File, char[])` probes the format rather than being told it, so JKS,
 * PKCS12 and JCEKS all take this same path — the production keystore's format is not this
 * code's business, and the test fixture can be whichever format a JDK can write unaided.
 *
 * Every message here names environment variables only. The keystore path is a value like any
 * other: it identifies a file on the founder's machine, and it is not this build's job to
 * print it into a log.
 */
object SigningKeystoreProbe {
    fun probe(credentials: SigningResolutionResult.Resolved): SigningKeystoreProbeResult {
        val file = File(credentials.keystorePath)
        if (!file.isFile) {
            return failure(
                "${SigningKeys.KEYSTORE} names a file that does not exist (or is not a file).",
                "Check the exported path; the keystore lives outside this repository.",
            )
        }

        val store =
            try {
                KeyStore.getInstance(file, credentials.storePassword.toCharArray())
            } catch (failed: IOException) {
                return cannotOpen(failed)
            } catch (failed: GeneralSecurityException) {
                return cannotOpen(failed)
            }

        if (!store.containsAlias(credentials.keyAlias)) {
            return failure(
                "${SigningKeys.KEY_ALIAS} is not an alias in this keystore.",
                "List the aliases with: keytool -list -keystore \"\$${SigningKeys.KEYSTORE}\"",
            )
        }

        val key =
            try {
                store.getKey(credentials.keyAlias, credentials.keyPassword.toCharArray())
            } catch (failed: GeneralSecurityException) {
                return failure(
                    "The key under ${SigningKeys.KEY_ALIAS} could not be opened " +
                        "(${failed.javaClass.simpleName}).",
                    "${SigningKeys.KEY_PASSWORD} is the usual cause.",
                )
            }

        return if (key == null) {
            failure(
                "${SigningKeys.KEY_ALIAS} exists but holds no key — it is a certificate-only entry.",
                "Export the alias of the private key that signs uploads.",
            )
        } else {
            SigningKeystoreProbeResult.Usable
        }
    }

    private fun cannotOpen(failed: Exception): SigningKeystoreProbeResult.Failure =
        failure(
            "The keystore could not be opened (${failed.javaClass.simpleName}).",
            "${SigningKeys.STORE_PASSWORD} is the usual cause; a file that is not a keystore " +
                "is the other.",
        )

    /** One shape for every message: what failed, what to do, and why nothing was produced. */
    private fun failure(
        what: String,
        howToFix: String,
    ): SigningKeystoreProbeResult.Failure =
        SigningKeystoreProbeResult.Failure(
            buildString {
                appendLine("Release signing cannot proceed.")
                appendLine(what)
                appendLine("  $howToFix")
                append("No unsigned or partially signed artifact was produced.")
            },
        )
}
