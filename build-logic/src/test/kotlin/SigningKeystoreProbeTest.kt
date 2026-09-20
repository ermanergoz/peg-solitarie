import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator

/**
 * Proves the second half of the release-signing contract: the four variables being *set* is
 * not the same as the keystore being usable. Each failure below would otherwise surface
 * either as AGP's own opaque "Failed to read key … from store …" at packaging time or — the
 * case this exists to prevent — as a build that gets far enough to emit something.
 *
 * The fixture keystore is built **in-process** from JDK APIs only (a `JCEKS` store holding one
 * `SecretKeyEntry`): no `keytool` subprocess, no committed binary, nothing to clean up outside
 * the JUnit temporary folder. `KeyStore.getInstance(File, char[])` probes the format, so the
 * production code under test is format-agnostic and a JCEKS fixture exercises the same path a
 * real JKS/PKCS12 upload keystore takes.
 */
class SigningKeystoreProbeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun acceptsAKeystoreWhoseAliasOpensWithTheGivenPasswords() {
        val result = SigningKeystoreProbe.probe(credentials(keystore = writeKeystore()))

        assertThat(result).isEqualTo(SigningKeystoreProbeResult.Usable)
    }

    @Test
    fun saysTheFileIsMissingWhenTheKeystorePathNamesNothing() {
        val absent = File(temporaryFolder.root, "not-there.jks")

        val result = SigningKeystoreProbe.probe(credentials(keystore = absent))

        val message = (result as SigningKeystoreProbeResult.Failure).message
        assertThat(message).contains(SigningKeys.KEYSTORE)
        assertThat(message).contains("does not exist")
    }

    @Test
    fun saysTheStoreCannotBeOpenedWhenTheStorePasswordIsWrong() {
        val result =
            SigningKeystoreProbe.probe(
                credentials(keystore = writeKeystore(), storePassword = "not-the-store-password"),
            )

        val message = (result as SigningKeystoreProbeResult.Failure).message
        assertThat(message).contains(SigningKeys.STORE_PASSWORD)
        assertThat(message).doesNotContain("does not exist")
    }

    @Test
    fun saysTheAliasIsAbsentWhenTheKeystoreHasNoSuchAlias() {
        val result =
            SigningKeystoreProbe.probe(credentials(keystore = writeKeystore(), keyAlias = "absent-alias"))

        val message = (result as SigningKeystoreProbeResult.Failure).message
        assertThat(message).contains(SigningKeys.KEY_ALIAS)
        assertThat(message).contains("keytool -list")
    }

    @Test
    fun saysTheKeyCannotBeOpenedWhenTheKeyPasswordIsWrong() {
        val result =
            SigningKeystoreProbe.probe(
                credentials(keystore = writeKeystore(), keyPassword = "not-the-key-password"),
            )

        val message = (result as SigningKeystoreProbeResult.Failure).message
        assertThat(message).contains(SigningKeys.KEY_PASSWORD)
        assertThat(message).doesNotContain(SigningKeys.STORE_PASSWORD)
    }

    /** Same rule as the resolver's: a probe failure names variables, never their values. */
    @Test
    fun noProbeFailureMessageEverContainsACredentialValue() {
        val keystore = writeKeystore()
        val messages =
            listOf(
                SigningKeystoreProbe.probe(credentials(File(temporaryFolder.root, "gone.jks"))),
                SigningKeystoreProbe.probe(credentials(keystore, storePassword = "wrong-store")),
                SigningKeystoreProbe.probe(credentials(keystore, keyAlias = "wrong-alias")),
                SigningKeystoreProbe.probe(credentials(keystore, keyPassword = "wrong-key")),
            ).filterIsInstance<SigningKeystoreProbeResult.Failure>().map { it.message }

        assertThat(messages).hasSize(4)
        messages.forEach { message ->
            assertThat(message).doesNotContain(ALIAS)
            assertThat(message).doesNotContain(KEY_PASSWORD)
            assertThat(message).doesNotContain(STORE_PASSWORD)
            assertThat(message).doesNotContain(keystore.absolutePath)
        }
    }

    /** A throwaway keystore in the JUnit temporary folder: one alias, one secret key. */
    private fun writeKeystore(): File {
        val file = File(temporaryFolder.root, "fixture.jceks")
        val store = KeyStore.getInstance("JCEKS")
        store.load(null, null)
        store.setEntry(
            ALIAS,
            KeyStore.SecretKeyEntry(KeyGenerator.getInstance("AES").generateKey()),
            KeyStore.PasswordProtection(KEY_PASSWORD.toCharArray()),
        )
        file.outputStream().use { out -> store.store(out, STORE_PASSWORD.toCharArray()) }
        return file
    }

    private fun credentials(
        keystore: File,
        keyAlias: String = ALIAS,
        keyPassword: String = KEY_PASSWORD,
        storePassword: String = STORE_PASSWORD,
    ) = SigningResolutionResult.Resolved(
        keystorePath = keystore.absolutePath,
        keyAlias = keyAlias,
        keyPassword = keyPassword,
        storePassword = storePassword,
    )

    private companion object {
        // Fixture values only — invented here, never a real credential.
        const val ALIAS = "fixture-alias"
        const val KEY_PASSWORD = "fixture-key-password"
        const val STORE_PASSWORD = "fixture-store-password"
    }
}
