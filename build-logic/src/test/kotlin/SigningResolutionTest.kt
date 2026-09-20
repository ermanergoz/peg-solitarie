import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the release-signing contract (PLAT-08 / D-16): a release invocation with any of the
 * four upload-keystore environment variables absent fails with a message naming **that**
 * variable, while nothing here can ever be satisfied from a file — unlike the ad and Play
 * Games IDs, these are cryptographic secrets and `local.properties` is not a place for them.
 *
 * **Why this test lives in `build-logic` and not in `:app`.** Same reason as
 * [AdIdResolutionTest]: AGP 9.1.0 creates a unit-test component only for the tested build
 * type, so `testReleaseUnitTest` does not exist and a release-side assertion in `:app` would
 * silently never run.
 */
class SigningResolutionTest {
    @Test
    fun resolvesAllFourCredentialsFromTheEnvironment() {
        val result = SigningResolution.resolve(fullEnv())

        assertThat(result).isInstanceOf(SigningResolutionResult.Resolved::class.java)
        val resolved = result as SigningResolutionResult.Resolved
        assertThat(resolved.keystorePath).isEqualTo(KEYSTORE_PATH)
        assertThat(resolved.keyAlias).isEqualTo(KEY_ALIAS)
        assertThat(resolved.keyPassword).isEqualTo(KEY_PASSWORD)
        assertThat(resolved.storePassword).isEqualTo(STORE_PASSWORD)
    }

    @Test
    fun anEmptyEnvironmentFailsNamingEveryMissingVariable() {
        val result = SigningResolution.resolve(emptyMap())

        val message = (result as SigningResolutionResult.Failure).message
        SigningKeys.ALL.forEach { key -> assertThat(message).contains("Missing: $key") }
    }

    @Test
    fun aFailureNamesOnlyTheVariableThatIsActuallyAbsent() {
        val result = SigningResolution.resolve(fullEnv() - SigningKeys.KEY_ALIAS)

        val message = (result as SigningResolutionResult.Failure).message
        assertThat(message).contains("Missing: ${SigningKeys.KEY_ALIAS}")
        assertThat(message).doesNotContain("Missing: ${SigningKeys.KEYSTORE}")
        assertThat(message).doesNotContain("Missing: ${SigningKeys.KEY_PASSWORD}")
        assertThat(message).doesNotContain("Missing: ${SigningKeys.STORE_PASSWORD}")
    }

    @Test
    fun theFailureMessageSaysHowToSetTheVariableAndThatDebugNeedsNone() {
        val result = SigningResolution.resolve(emptyMap())

        val message = (result as SigningResolutionResult.Failure).message
        assertThat(message).contains("export ${SigningKeys.KEYSTORE}")
        assertThat(message).contains("Debug builds")
    }

    @Test
    fun aBlankValueCountsAsAbsent() {
        val result = SigningResolution.resolve(fullEnv() + (SigningKeys.STORE_PASSWORD to "   "))

        val message = (result as SigningResolutionResult.Failure).message
        assertThat(message).contains("Missing: ${SigningKeys.STORE_PASSWORD}")
    }

    /**
     * A password's leading/trailing whitespace is significant — trimming it the way
     * [AdIdResolution] trims an ad unit ID would turn a correct password into a wrong one and
     * surface as an unrecoverable-key failure nobody could explain.
     */
    @Test
    fun passwordWhitespaceIsPreservedVerbatim() {
        val padded = " pa ss "
        val result = SigningResolution.resolve(fullEnv() + (SigningKeys.KEY_PASSWORD to padded))

        assertThat((result as SigningResolutionResult.Resolved).keyPassword).isEqualTo(padded)
    }

    @Test
    fun aRelativeKeystorePathIsRejectedWithItsOwnMessage() {
        val result = SigningResolution.resolve(fullEnv() + (SigningKeys.KEYSTORE to "upload.jks"))

        val message = (result as SigningResolutionResult.Failure).message
        assertThat(message).contains(SigningKeys.KEYSTORE)
        assertThat(message).contains("absolute")
    }

    /**
     * The whole point of the interface: a Gradle log is pasted into issues, chats and reports.
     * No failure message may carry a credential value — only the variable name that holds it.
     */
    @Test
    fun noFailureMessageEverContainsACredentialValue() {
        val messages =
            listOf(
                SigningResolution.resolve(emptyMap()),
                SigningResolution.resolve(fullEnv() - SigningKeys.KEY_ALIAS),
                SigningResolution.resolve(fullEnv() + (SigningKeys.KEYSTORE to "upload.jks")),
            ).filterIsInstance<SigningResolutionResult.Failure>().map { it.message }

        assertThat(messages).hasSize(3)
        messages.forEach { message ->
            assertThat(message).doesNotContain(KEY_ALIAS)
            assertThat(message).doesNotContain(KEY_PASSWORD)
            assertThat(message).doesNotContain(STORE_PASSWORD)
        }
    }

    /** A data class prints every field; these fields must never reach a log or a stack trace. */
    @Test
    fun theResolvedCredentialsRedactThemselvesWhenPrinted() {
        val resolved = SigningResolution.resolve(fullEnv()) as SigningResolutionResult.Resolved

        val printed = resolved.toString()
        assertThat(printed).doesNotContain(KEY_PASSWORD)
        assertThat(printed).doesNotContain(STORE_PASSWORD)
        assertThat(printed).doesNotContain(KEY_ALIAS)
        assertThat(printed).doesNotContain(KEYSTORE_PATH)
    }

    private fun fullEnv(): Map<String, String> =
        mapOf(
            SigningKeys.KEYSTORE to KEYSTORE_PATH,
            SigningKeys.KEY_ALIAS to KEY_ALIAS,
            SigningKeys.KEY_PASSWORD to KEY_PASSWORD,
            SigningKeys.STORE_PASSWORD to STORE_PASSWORD,
        )

    private companion object {
        // Fixture values only — invented here, never a real credential.
        const val KEYSTORE_PATH = "/fixture/keys/upload.jks"
        const val KEY_ALIAS = "fixture-alias"
        const val KEY_PASSWORD = "fixture-key-password"
        const val STORE_PASSWORD = "fixture-store-password"
    }
}
